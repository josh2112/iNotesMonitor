package com.josh2112.inotesmonitor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Stopwatch;
import com.josh2112.inotesmonitor.INotesClient.MessageCheckPageResult;
import com.josh2112.inotesmonitor.database.NotesLocalDatabase;
import com.josh2112.inotesmonitor.database.Tables;
import com.josh2112.javafx.LoadableContainer;

public class DebugPanel extends LoadableContainer {

	private Log log = LogFactory.getLog( DebugPanel.class );

	private INotesClient client;
	
	protected DebugPanel( INotesClient client ) {
		super( "DebugPanel" );
		this.client = client;
	}
	
	@FXML
	void handleButton1( ActionEvent event ) {
		try { checkForDeletedMessages(); }
		catch( Exception e ) { e.printStackTrace(); }
	}

	private void checkForDeletedMessages() throws Exception {
		
		Stopwatch timer = Stopwatch.createStarted();
		
		LocalDateTime stopDate = LocalDateTime.MIN;
		
		List<String> knownGuids = NotesLocalDatabase.getContext()
				.select( Tables.NotesMessage.GUID )
				.from( Tables.NotesMessage )
				.fetch( Tables.NotesMessage.GUID );
		
		List<String> retreivedGuids = new ArrayList<String>();
		
		int startMessageNum = 1, messageCount = 1 << 10;
		MessageCheckPageResult page;
		
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
			
			retreivedGuids.addAll( page.messages.stream().map( record -> record.getGUID() ).collect( Collectors.toList() ) );
		}
		while( page.getOldestMessageDate().isAfter( stopDate ) );
		
		log.info( "Grabbing ALL GUIDs took " + timer );
		
		// Remove all server-side GUIDs from the ones in our database... the leftovers are
		// messages that have been deleted.
		Set<String> deletedGuids = new HashSet<String>( knownGuids );
		deletedGuids.removeAll( retreivedGuids );
		
		log.info( "FOUND " + deletedGuids.size() + " DELETED MSGS: " + deletedGuids );
		
		
	}
}
