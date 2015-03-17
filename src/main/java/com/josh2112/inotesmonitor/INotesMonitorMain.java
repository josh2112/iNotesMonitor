package com.josh2112.inotesmonitor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.LogManager;
import java.util.stream.Collectors;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Worker.State;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.Dialog.Actions;
import org.controlsfx.dialog.Dialogs;
import org.jooq.Result;

import com.josh2112.inotesmonitor.Configuration.Settings;
import com.josh2112.inotesmonitor.database.NotesLocalDatabase;
import com.josh2112.inotesmonitor.database.Tables;
import com.josh2112.inotesmonitor.database.tables.records.NotesMessageRecord;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.inotesmonitor.notesmeetingtogcalevent.GoogleAuthenticationService;
import com.josh2112.inotesmonitor.notesmeetingtogcalevent.NotesMeetingToGCalEventWizard;
import com.josh2112.javafx.FXMLLoader;
import com.josh2112.javafx.LabelUtils;
import com.josh2112.javafx.LoadableContainer;
import com.josh2112.javafx.OneTimeChangeListener;
import com.josh2112.utility.Storage;

public class INotesMonitorMain extends Application {
	
	private Log log = LogFactory.getLog( INotesMonitorMain.class );
	
	public static final String APP_NAME_PRETTY = "iNotes Monitor";
	public static final String APP_NAME = "iNotesMonitor";
	
	private static String SERVER = "webmail.cem.com";
	
	private static String defaultMessageCss;
	
	private static HostServices hostService = null;
	private static void setHostService( HostServices hs ) { hostService = hs; }
	public static HostServices getHostService() { return hostService; }
	
	private static Stage parentWindow = null;
	private static void setParentWindow( Stage pw ) { parentWindow = pw; }
	public static Stage getParentWindow() { return parentWindow; }
	
	private INotesClient client = new INotesClient( SERVER );
	private Timeline periodicUpdater;
	
	private SnarlManager snarlManager;
	
	private MessageCheckService messageCheckService = new MessageCheckService( client );
	
	@FXML private StackPane container;
	
	@FXML private ListView<String> categoryList;
	@FXML private ListView<NotesMessage> messageList;
	@FXML private VBox messageDetailsPane;
	@FXML private Label senderLabel, subjectLabel;
	@FXML private FlowPane recipientsPane;
	@FXML private WebView htmlViewer;
	@FXML private HBox messageToolbar;
	@FXML private Label usernameLabel;
	@FXML private Button refreshButton;
	@FXML private ProgressBar progressBar;
	@FXML private ProgressIndicator spinner;
	@FXML private TextField searchTextBox;
	@FXML private CheckBox showEmailsCheckBox, showMeetingsCheckBox;
	
	@FXML private Label placeholder_meetingDetailsPanel;
	@FXML private MeetingDetailsPanel meetingDetailsPanel;
	
	@FXML private Label placeholder_statusPanel;
	@FXML private StatusPanel statusPanel;
	
	private LoginPanel loginPanel;
	private BooleanBinding initializationCompleteBinding;
	
	private Stage debugWindow;
	
	private ReadOnlyObjectProperty<NotesMessage> selectedMessageProperty() {
		return messageList.getSelectionModel().selectedItemProperty();
	}
	
	private ObservableList<NotesMessage> messages = FXCollections.<NotesMessage>observableArrayList();
	
	private NotesMessageCardActionListener notesMessageCardActionListener = new NotesMessageCardActionListener() {
		@Override
		public void openInBrowser( NotesMessage msg ) {
			messageCheckService.addMessageUpdateRequest( msg );
			client.openMessageInBrowser( msg );
			snarlManager.hideNotification();
		}

		@Override
		public void markAsRead( NotesMessage item ) {
			try { client.setMessageIsRead( item, true ); }
			catch( Exception e ) {
				Dialogs.create().title( "Mark as Read Failed" ).showException( e );
			}
			snarlManager.hideNotification();
		}
		
		@Override
		public void addToGoogleCalendar( NotesMessage meeting ) {
			new NotesMeetingToGCalEventWizard( meeting, notesMessageCardActionListener );
			snarlManager.hideNotification();
		}

		@Override
		public void acceptMeeting( NotesMessage meeting ) {
			try {
				client.acceptMeeting( meeting );
			} catch( Exception e ) {
				Dialogs.create().title( "Accept Meeting Failed" ).showException( e );
			}
		}

		@Override
		public void delete( NotesMessage msg ) {
			try {
				client.deleteMessage( msg );
			}
			catch( Exception e ) {
				if( Dialogs.create().title( "Delete Message Failed" ).masthead( "Failed to delete this message on the server." )
					.message( "Do you want to remove it from the list anyway?" ).showConfirm() == Actions.YES ) {
					messages.remove( msg );
					msg.delete();
				}
			}
		}
	};
	
	private Predicate<NotesMessage> messageFilter = (msg) -> {
		if( msg.isMeeting() && !showMeetingsCheckBox.isSelected() ) return false;
		else if( !msg.isMeeting() && !showEmailsCheckBox.isSelected() ) return false;
		
		if( searchTextBox.getText().isEmpty() ) return true;
		else
		{
			String searchText = searchTextBox.getText();
			return StringUtils.containsIgnoreCase( msg.getSubject(), searchText ) ||
					StringUtils.containsIgnoreCase( msg.getBody(), searchText ) ||
					StringUtils.containsIgnoreCase( msg.getSender(), searchText );
		}
	};
	
	private Timeline textFilterUpdater;
	
	private void startUpdates() {
		periodicUpdater = new Timeline( new KeyFrame( Duration.minutes( 
				Configuration.getInstance().getInt( Settings.UPDATE_FREQUENCY_MINUTES )  ),
				e -> messageCheckService.restart() ) );
		
		periodicUpdater.setCycleCount( Timeline.INDEFINITE );
		periodicUpdater.play();
		
		messageCheckService.restart();
	}
	
	@FXML
    void handleMenuButton( ActionEvent event ) {
		Control control = (Control)event.getSource();
		control.getContextMenu().show( control, Side.BOTTOM, 0, 0 );
    }
	
	@FXML
    void handleDebugMenuItem( ActionEvent event ) {
		if( debugWindow == null ) {
			debugWindow = new Stage();
			debugWindow.setTitle( "Debug - iNotesMonitor" );
			debugWindow.setScene( new Scene( new DebugPanel( client ).getContainer() ) );
		}
		debugWindow.show();
    }
	
	@FXML
    void handleRefreshButton( ActionEvent event ) {
		periodicUpdater.playFromStart();
		messageCheckService.restart();
    }
	
	@FXML
    void clearSearchText( ActionEvent event ) {
		searchTextBox.setText( "" );
    }
	
	@FXML
    void handleForgetMeMenuItem( ActionEvent event ) {
		periodicUpdater.stop();
		messages.clear();
		
		try {
			CredentialStorage.removeCredentials();
			GoogleAuthenticationService.removeCredentials();
		}
		catch( Exception e ) {}
		Configuration.getInstance().setBool( Settings.SAVE_CREDENTIALS, false );
		
		client = new INotesClient( SERVER );
		messageCheckService.setClient( client );
		
		container.getChildren().add( loginPanel.getContainer() );
    }
	
	@FXML
    void handleOpenInBrowserButtonClick( ActionEvent event ) {
		notesMessageCardActionListener.openInBrowser( 
				messageList.getSelectionModel().getSelectedItem() );
	}
	
	@FXML
    void handleDeleteButtonClick( ActionEvent event ) {
		notesMessageCardActionListener.delete( 
				messageList.getSelectionModel().getSelectedItem() );
	}
    
    /*************************************************************************/
	
	@Override
	public void start( Stage stage ) {
		log.info( APP_NAME_PRETTY + " started" );
		
		Storage.initApplicationStorage( APP_NAME );
		
		String iconResourcePath = "/package/linux/NotesMeetingToGCalEvent.png";
		stage.getIcons().add( new Image( this.getClass().getResourceAsStream( iconResourcePath )));
		
		// Pull these resources out of the .jar file into the local 'resources' directory.
		Storage.getInstance().maybeUnpackResources( new String[] { iconResourcePath, "/defaultMessageStyle.css" } );
		
		try {
			defaultMessageCss = String.join( System.lineSeparator(), Files.readAllLines(
					Paths.get( Storage.getInstance().getAppResourcesDirectory().getAbsolutePath(), "defaultMessageStyle.css" ) ) );
		}
		catch( IOException e ) {
			log.error( e );
		}
		
		snarlManager = new SnarlManager( "josh2112", APP_NAME, APP_NAME_PRETTY );
		snarlManager.setIcon( new File( Storage.getInstance().getAppResourcesDirectory(),
	    			new File( iconResourcePath ).getName() ) );
		snarlManager.setNotificationAction( () -> {
			stage.show();
			stage.toFront();
		} );
		
		Thread.setDefaultUncaughtExceptionHandler( (thread, e) -> {
			if( periodicUpdater != null ) periodicUpdater.stop();
			Dialogs.create().title( "Exception" ).masthead(
					APP_NAME_PRETTY + " has encountered a problem and needs to close." ).showException( e );
		});
		
		INotesMonitorMain.setHostService( this.getHostServices() );
		INotesMonitorMain.setParentWindow( stage );
		
		FXMLLoader.loadFXML( this, "/fxml/Main.fxml" );
		
		stage.setScene( new Scene( container ));
		stage.minHeightProperty().bind( container.minHeightProperty().add( 100 ) );
		stage.minWidthProperty().bind( container.minWidthProperty().add( 50 ) );
		stage.setTitle( APP_NAME_PRETTY );
		
		usernameLabel.textProperty().bind( client.usernameProperty() );
		
		spinner.visibleProperty().bind( messageCheckService.runningProperty() );
		
		progressBar.visibleProperty().bind( messageCheckService.runningProperty() );
		progressBar.progressProperty().bind( messageCheckService.progressProperty() );
		
		refreshButton.visibleProperty().bind( spinner.visibleProperty().not() );
		
		messageList.setCellFactory( (list) -> new NotesMessageListCell() );
		
		FilteredList<NotesMessage> filteredMessages = messages.filtered( messageFilter );

		messageList.setItems( filteredMessages.sorted( (m1, m2) -> m2.getDate().compareTo( m1.getDate() ) ) );
		
		final Runnable updateMessageFilter = () -> {
			filteredMessages.setPredicate( msg -> false );
			filteredMessages.setPredicate( messageFilter );	
		};
		
		textFilterUpdater = new Timeline( new KeyFrame( Duration.seconds( 0.5 ), e -> updateMessageFilter.run() ) );
		
		searchTextBox.textProperty().addListener( (prop, oldText, newText) -> {
			textFilterUpdater.playFromStart();
		});
		
		showEmailsCheckBox.setSelected( Configuration.getInstance().getBool( Settings.SHOW_EMAILS ) );
		showMeetingsCheckBox.setSelected( Configuration.getInstance().getBool( Settings.SHOW_MEETINGS ) );
		
		showEmailsCheckBox.selectedProperty().addListener( (prop, wasSelected, isSelected) -> {
			Configuration.getInstance().setBool( Settings.SHOW_EMAILS, isSelected );
			Configuration.getInstance().saveSettings();
			updateMessageFilter.run();
		} );
		
		showMeetingsCheckBox.selectedProperty().addListener( (prop, wasSelected, isSelected) -> {
			Configuration.getInstance().setBool( Settings.SHOW_MEETINGS, isSelected );
			Configuration.getInstance().saveSettings();
			updateMessageFilter.run();
		} );
		
		selectedMessageProperty().addListener( (prop, oldSelectedItem, newSelectedItem ) -> {
			if( newSelectedItem != null ) {
				htmlViewer.getEngine().loadContent( "<html><head><style type=\"text/css\">" +
						defaultMessageCss + "</style></head><body>" + newSelectedItem.getBody() + "</body></html>" );

				newSelectedItem.load();
				recipientsPane.getChildren().setAll( LabelUtils.makeLabelList(
						newSelectedItem.getRecipients().stream().map( r -> r.getName() )
						.collect( Collectors.toList() ), "attendee", 6 ) );
			}
			else htmlViewer.getEngine().loadContent( "" );
		} );
		
		meetingDetailsPanel = insertCustomComponent( new MeetingDetailsPanel( notesMessageCardActionListener ), placeholder_meetingDetailsPanel );
		meetingDetailsPanel.getContainer().managedProperty().bind( meetingDetailsPanel.getContainer().visibleProperty());
		meetingDetailsPanel.getContainer().visibleProperty().bind( Bindings.selectBoolean( selectedMessageProperty(), "meeting" ) );
		meetingDetailsPanel.notesMessageProperty().bind( selectedMessageProperty() );
		
		statusPanel = insertCustomComponent( new StatusPanel(), placeholder_statusPanel );
		StackPane.setAlignment( statusPanel.getContainer(), Pos.BOTTOM_LEFT );
		
		senderLabel.textProperty().bind( Bindings.selectString( selectedMessageProperty(), "sender" ) );
		subjectLabel.textProperty().bind( Bindings.selectString( selectedMessageProperty(), "subject" ) );
		
		categoryList.getItems().add( "Inbox" );
		categoryList.getSelectionModel().select( 0 );
		
		messageDetailsPane.visibleProperty().bind( selectedMessageProperty().isNotNull() );
		
		// These allows the Message Check Service to do incremental updates. Whenever something is added
		// to this list we'll merge it into the master list.
		messageCheckService.getAddedUpdatedMessageList().addListener(
			(ListChangeListener.Change<? extends NotesMessage> change) -> {
				while( change.next() ) {
					if( change.wasAdded() ) mergeMessages( change.getAddedSubList() );
				}
		});
		messageCheckService.getRemovedMessageGuidList().addListener( 
			(ListChangeListener.Change<? extends String> change) -> {
				while( change.next() ) {
					if( change.wasAdded() ) {
						log.info( String.format( "Detected %d message(s) deleted (%s)",
								change.getAddedSubList().size(), change.getAddedSubList() ) );
						List<NotesMessage> msgsToRemove = messages.stream().filter( m ->
							change.getAddedSubList().contains( m.getGuid() ) ).collect( Collectors.toList() );
						msgsToRemove.stream().forEach( m -> m.delete() );
						messages.removeAll( msgsToRemove );
				}
			}
		});
		
		messageCheckService.setOnRunning( e -> {
			statusPanel.statusTextProperty().bind( messageCheckService.messageProperty() );
		} );
		
		messageCheckService.setOnSucceeded( e -> {
			statusPanel.statusTextProperty().unbind();
			
			List<NotesMessage> unreadMessages = messageCheckService.getValue().stream()
					.filter( m -> !m.isRead() ).collect( Collectors.toList() );
			if( unreadMessages.size() > 0 ) {
				String newMsgsText = String.format( "%d new message%s", unreadMessages.size(), unreadMessages.size() != 1 ? "s" : "" );
				statusPanel.setStatusText( newMsgsText );
				snarlManager.updateNotification( newMsgsText,
					String.format( "%s%s", unreadMessages.stream().limit( 2 )
							.map( m -> String.format( "%s - %s", m.getSender(), m.getSubject() ) )
							.collect( Collectors.joining( "\n" ) ), unreadMessages.size() > 2 ? "\n..." : "" )
				);
			}
			else {
				snarlManager.hideNotification();
				statusPanel.setStatusText( "No new messages" );
			}
		} );
		
		messageCheckService.setOnFailed( e -> {
			statusPanel.statusTextProperty().unbind();
			
			if( messageCheckService.getException() instanceof MessageCheckService.AuthenticationExpiredException ) {
				if( periodicUpdater != null ) periodicUpdater.stop();
				// Authentication tokens have expired, need to re-login by showing login panel again.
				loginPanel.isLoggedInProperty().addListener( new OneTimeChangeListener<Boolean>(
						(observable, wasLoggedIn, isLoggedIn) -> {
							if( isLoggedIn ) handleRefreshButton( null );
						} ) );
				
				loginPanel.attachAnimated( container );
			}
			else {
				Dialogs.create().title( "Exception" ).masthead(
						APP_NAME_PRETTY + " has encountered a problem and needs to close." )
						.showException( messageCheckService.getException() );
			}
		} );
		
		NotesLocalDatabase.initializationStateProperty().addListener( (prop, oldInitState, newInitState) -> {
			if( newInitState == State.SUCCEEDED ) {
				//reloadMessages();
				Result<NotesMessageRecord> allMsgs = NotesLocalDatabase.getContext()
						.selectFrom( Tables.NotesMessage )
						.orderBy( Tables.NotesMessage.Date.desc() )
						.fetch();
				mergeMessages( allMsgs.stream().map( r -> new NotesMessage( r ) ).collect( Collectors.toList() ) );
				
			}
			else if( newInitState == State.FAILED ) {
				Dialogs.create().title( "Exception" ).masthead(
						APP_NAME_PRETTY + " has encountered a problem and needs to close." )
						.showException( NotesLocalDatabase.getInitializationException() );
			}		
		});
		
		loginPanel = new LoginPanel( client );
		loginPanel.attachAnimated( container );
		
		loginPanel.isLoggedInProperty().addListener( (prop, wasLoggedIn, isLoggedIn) -> {
			if( isLoggedIn ) loginPanel.detachAnimated(); 
		} );
		
		initializationCompleteBinding = Bindings.and( loginPanel.isLoggedInProperty(),
				NotesLocalDatabase.initializationStateProperty().isEqualTo( State.SUCCEEDED ) );
		
		initializationCompleteBinding.addListener( (prop, wasInitComplete, isInitComplete) -> {
			if( isInitComplete ) {
				log.info( "Login and database initialization both complete. Starting message check service." );
				startUpdates();
			}
		} );
		
		stage.show();
	}
	
	private <T extends LoadableContainer> T insertCustomComponent( T component, javafx.scene.Node placeholder ) {
		Pane parent = (Pane)placeholder.getParent();
		int indexInParent = parent.getChildrenUnmodifiable().indexOf( placeholder );
		parent.getChildren().remove( indexInParent );
		parent.getChildren().add( indexInParent, component.getContainer() );
		return component;
	}
	
	/*
	private void reloadMessages() {
		messages.clear();
		Task<Void> loadMessagesTask = new Task<Void>() {
			@Override protected Void call() throws Exception {
				Result<NotesMessageRecord> allMsgs = NotesLocalDatabase.getContext()
						.selectFrom( Tables.NotesMessage )
						.orderBy( Tables.NotesMessage.Date.desc() )
						.fetch();
				
				for( int i=0; i<allMsgs.size(); i += 100 ) {
					final List<NotesMessage> loadedMessages = allMsgs.subList( i, Math.min( i + 100, allMsgs.size() ) ).stream()
							.map( record -> NotesMessage.fromDatabase( record ) )
							.collect( Collectors.toList() );
					Platform.runLater( () -> mergeMessages( loadedMessages ) );
				}
				
				return null;
			}
			
		};
		
		ForkJoinPool.commonPool().submit( loadMessagesTask );
	}
	*/
	
	/***
	 * Merges the given message list with the existing message list. Any new messages
	 * (those for which no message with that GUID exists in the existing list) are added,
	 * and any updated messages (those for which a message with that GUID exists in the
	 * existing list) are replaced.
	 * @param newAndUpdatedMessages
	 */
	private void mergeMessages( List<? extends NotesMessage> newAndUpdatedMessages ) {
		
		// First, remove any messages from 'messages' that match those in 'newAndUpdatedMessages' (have same GUID).
		List<String> newMessageGuids = newAndUpdatedMessages.stream().map( NotesMessage::getGuid ).collect( Collectors.toList() );
		messages.removeIf( m -> newMessageGuids.contains( m.getGuid() ) );
		
		// Next, just all all messages from 'newAndUpdatedMessages' to 'messages'.
		messages.addAll( newAndUpdatedMessages );
	}
	
	@Override
	public void stop() {
		Configuration.getInstance().saveSettings();
		snarlManager.unregister();
		
		NotesLocalDatabase.getInstance().close();
		
		log.info( APP_NAME_PRETTY + " stopped" );
	}
	
	public static void main( String[] args ) throws IOException {
		LogManager.getLogManager().readConfiguration( INotesMonitorMain.class.getResourceAsStream( "/logging.properties" ) );
		launch( args );
	}
}
