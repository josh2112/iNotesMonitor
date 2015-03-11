package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.dialog.Dialogs;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.oauth2.model.Userinfoplus;
import com.josh2112.javafx.FXMLLoader;
import com.josh2112.javafx.wizard.WizardContainer;
import com.josh2112.javafx.wizard.WizardPage;
import com.josh2112.javafx.wizard.WizardPageConfiguration;

public class CalendarSelectionPage extends WizardPage {

	@FXML private Pane authorizingPane, authorizedPane;
	@FXML private Text emailText;
	@FXML private ListView<CalendarListEntry> calendarList;
	
	private Log log = LogFactory.getLog( CalendarSelectionPage.class );
	
	private MeetingConfiguration pageConfig;
		
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	
	private static final String APP_NAME = "NotesMeetingToGCalEvent";
	
	private HttpTransport httpTransport;
	private GoogleAuthenticationService authService;
	
	private static com.google.api.services.calendar.Calendar calendarClient;
	private static com.google.api.services.oauth2.Oauth2 oauth2Client;
	
	static class CalendarCell extends ListCell<CalendarListEntry> {
        @Override
        public void updateItem( CalendarListEntry calendar, boolean empty ) {
            super.updateItem( calendar, empty );
            
            if( calendar != null ) {
            	Rectangle rect = new Rectangle( 20, 20, Color.web( calendar.getBackgroundColor() ) );
            	rect.getStyleClass().add( "calendarIcon" );
            	setGraphic( rect );
            	setText( calendar.getSummary() );
            }
        }
    }
	
	public CalendarSelectionPage( WizardContainer container ) {
		super( container, new BorderPane() );
		FXMLLoader.loadFXML( this.getRootNode(), this, "/fxml/CalendarSelectionPage.fxml" );
		
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	    	authService = new GoogleAuthenticationService( httpTransport, JSON_FACTORY );
		} catch( Exception e ) {
			e.printStackTrace();
			Dialogs.create().owner( getStage() ).title( "Authentication Error" )
				.message( "Error creating the Google Authentication Service." ).showError();
			return;
		}
		
		calendarList.setCellFactory( (list) -> new CalendarCell() );
    	
    	authorizingPane.visibleProperty().bind( authService.valueProperty().isNull() );
    	authorizedPane.visibleProperty().bind( authService.valueProperty().isNotNull() );
    	
    	authService.valueProperty().addListener( (val, oldVal, newVal ) -> refreshClientData( newVal ) );
	}
	
	private void refreshClientData( Credential creds ) {
		log.debug( "Google Calendar authentication complete" );
		if( creds != null ) {
			calendarClient = new com.google.api.services.calendar.Calendar.Builder(
	    			httpTransport, JSON_FACTORY, creds ).setApplicationName(
	    					APP_NAME ).build();
			oauth2Client = new com.google.api.services.oauth2.Oauth2.Builder(
					httpTransport, JSON_FACTORY, creds ).setApplicationName(
	    					APP_NAME ).build();
			
			calendarList.getSelectionModel().selectedItemProperty().addListener(
					(value, oldVal, newVal) -> canNavigateToNextPageProperty().set( newVal != null ) );
			
			try {
				Userinfoplus ui = oauth2Client.userinfo().get().execute();
				emailText.setText( ui.getName());
				
				CalendarList cals = calendarClient.calendarList().list().execute();
				calendarList.setItems( FXCollections.observableList( cals.getItems() ) );
				
			} catch( GoogleJsonResponseException e ) {
				e.printStackTrace();
				Dialogs.create().owner( getStage() ).title( "Data Error" ).message(
						e.getDetails().getMessage() ).showError();
				
			} catch( Exception e ) {
				Dialogs.create().owner( getStage() ).title( "Data Error" ).message(
						"Error retreiving user and calendar data." ).showError();
			}
		}
	}
	
	@FXML
    public void handleCalendarListClick( MouseEvent mouseEvent ) {
        if( mouseEvent.getButton().equals( MouseButton.PRIMARY)){
            if( mouseEvent.getClickCount() == 2 ){
            	getWizardContainer().triggerNextButton();
            }
        }
    }
	
	@Override
	public void activate() {
		log.info( "Starting Google Calendar authentication" );
		authService.restart();
	}

	@Override
	public void activateWithConfiguration( WizardPageConfiguration configuration ) {
		pageConfig = (MeetingConfiguration)configuration;
		activate();
	}

	@Override
	public void deactivate() {
		authService.cancel();
	}

	@Override
	public boolean verify() {
		CalendarListEntry calEntry = (CalendarListEntry)calendarList.getSelectionModel().getSelectedItem();
		pageConfig.setCalendarClient( calendarClient );
		pageConfig.setCalendarId( calEntry.getId() );
		return true;
	}

	@Override
	public WizardPageConfiguration getConfiguration() {
		return pageConfig;
	}

}
