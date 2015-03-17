package com.josh2112.inotesmonitor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.josh2112.inotesmonitor.INotesClient.MessageCheckPageResult;
import com.josh2112.inotesmonitor.database.NotesLocalDatabase;
import com.josh2112.inotesmonitor.database.Tables;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;

public class MessageCheckService extends Service<List<NotesMessage>> {
	
	@SuppressWarnings( "serial" )
	public static class AuthenticationExpiredException extends RuntimeException {
		public AuthenticationExpiredException() {
			super( "Authentication tokens expired" );
		}
	}
	
	private Log log = LogFactory.getLog( MessageCheckService.class );
	
	private INotesClient client;
	
	private List<NotesMessage> messageUpdateRequests = new ArrayList<NotesMessage>();
	
	private ReadOnlyObjectWrapper<ObservableList<NotesMessage>> addedUpdatedMessageList =
            new ReadOnlyObjectWrapper<>( FXCollections.<NotesMessage>observableArrayList() );
    public final ObservableList<NotesMessage> getAddedUpdatedMessageList() { return addedUpdatedMessageList.get(); }
    
    private ReadOnlyObjectWrapper<ObservableList<String>> removedMessageGuidList =
            new ReadOnlyObjectWrapper<>( FXCollections.<String>observableArrayList() );
    public final ObservableList<String> getRemovedMessageGuidList() { return removedMessageGuidList.get(); }
    
    public void addMessageUpdateRequest( NotesMessage msg ) {
    	messageUpdateRequests.add( msg );
    }

	@Override protected Task<List<NotesMessage>> createTask() {
		return new Task<List<NotesMessage>>() {
			
			@Override protected void updateMessage( String message ) {
				if( !message.isEmpty() ) log.info( "MessageCheckService message: " + message );
				super.updateMessage( message );
			}
			
			@Override protected void updateProgress( double workDone, double max ) {
				log.info( String.format( "MessageCheckService progress: %f/%f (%f%%)", workDone, max, workDone/max*100 ) );
				// Show indeterminate until we are at least 1% through
				if( workDone/max >= 0.01 ) super.updateProgress( workDone, max );
				else super.updateProgress( -1.0, 0.0 );
			}
			
			@Override protected List<NotesMessage> call() throws Exception {
				updateMessage( "Checking for new messages" );
				updateProgress( -1, 0 );
				
				Platform.runLater( () -> {
					addedUpdatedMessageList.get().clear();
					removedMessageGuidList.get().clear();
				} );
				
				LocalDateTime stopDate = calculateStopDate();
				
				// Parse ONLY messages that aren't in the database, messages we know about
				// that are marked unread (to see if they have been read outside our app),
				// and other messages that we have been specifically requested to update.
				//
				// We do this by getting a list the GUIDs of all read messages in the database,
				// minus the ones we've been requested to update, then skipping a message if
				// its GUID is in this list.
				Set<String> guidsToIgnore = new HashSet<String>( NotesLocalDatabase.getContext()
						.select( Tables.NotesMessage.GUID )
						.from( Tables.NotesMessage )
						.where( Tables.NotesMessage.IsRead.equal( true ) )
						.fetch( Tables.NotesMessage.GUID ) );
				
				guidsToIgnore.removeAll( messageUpdateRequests.stream()
						.map( req -> req.getGuid() ).collect( Collectors.toList() ) );
				messageUpdateRequests.clear();
				
				int startMessageNum = 1, messageCount = 40;
				MessageCheckPageResult page;
				
				long totalMinutesIntoPast = stopDate.until( LocalDateTime.now(), ChronoUnit.MINUTES );
				
				LocalDateTime pageStartDate = LocalDateTime.now();
				 
				do {
					log.debug( String.format( "Retrieving %d messages (%d to %d)...", messageCount,
							startMessageNum, startMessageNum + messageCount ) );
					page = client.getMessages( startMessageNum, messageCount );
					
					if( page.messages.isEmpty() ) {
						log.debug( "Got empty message list page, assuming we've read all messages." );
						break;
					}
					
					log.debug( String.format( "%d messages, oldest on page dated %s",
							page.messages.size(), page.getOldestMessageDate() ) );
					startMessageNum += messageCount;
					
					// How far back into the past (in minutes) have we reached?
					long currentMinutesIntoPast = page.getOldestMessageDate().until( LocalDateTime.now(), ChronoUnit.MINUTES );
					updateProgress( currentMinutesIntoPast, totalMinutesIntoPast );
					
					// Make a list of the GUIDs we know that should be on this page.
					List<String> knownGuids = NotesLocalDatabase.getContext()
						.select( Tables.NotesMessage.GUID )
						.from( Tables.NotesMessage )
						.where( Tables.NotesMessage.Date.between( page.getOldestMessageDate(), pageStartDate ) )
						.fetch( Tables.NotesMessage.GUID );
					
					List<String> discoveredGuids = page.messages.stream().map( record -> record.getGUID() ).sorted().collect( Collectors.toList() );
					Set<String> deletedGuids = new HashSet<String>( knownGuids );
					deletedGuids.removeAll( discoveredGuids );
					Platform.runLater( () -> removedMessageGuidList.get().addAll( deletedGuids ) );
					
					pageStartDate = page.getOldestMessageDate();
					
					// Keep the messages we don't already know about
					List<NotesMessage> messagesOnPage = page.messages.stream()
							.filter( record -> !guidsToIgnore.contains( record.getGUID() ) )
							.map( record -> {
								try { return client.getMessageContent( record ); }
								catch( Exception e ) { throw new RuntimeException( e ); }
							} )
							.collect( Collectors.toList() );
					
					Platform.runLater( () -> addedUpdatedMessageList.get().addAll( messagesOnPage ) );
					
					messagesOnPage.stream().forEach( msg -> msg.store() );
				}
				while( page.getOldestMessageDate().isAfter( stopDate ) );
				
				///////////////////////

				return getAddedUpdatedMessageList();
			}
			
			/**
			 * Calculates how far back we should check for messages. If we have checked for messages
			 * before, only check back to the date of the newest message we've seen. Otherwise return
			 * a lower boundary date (LocalDateTime.MIN) to make sure we get everything.
			 * @return LocalDateTime to stop at when checking messages
			 */
			private LocalDateTime calculateStopDate() {
				LocalDateTime stopDate = NotesLocalDatabase.getContext().select(
						Tables.NotesMessage.Date ).from( Tables.NotesMessage ).orderBy(
								Tables.NotesMessage.Date.desc() ).limit( 1 ).fetchOne( Tables.NotesMessage.Date );
				
				return stopDate != null ? stopDate : LocalDateTime.MIN;
			}

			@Override protected void failed() {
				super.failed();
				updateMessage( "Error checking messages: " + this.getException().getMessage() );
				log.error( "Error checking messages", this.getException() );
			}
		};
	}
	
	public MessageCheckService( INotesClient client ) {
		setClient( client );
	}
	
	public void setClient( INotesClient client ) {
		this.client = client;
	}
}
