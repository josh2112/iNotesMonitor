package com.josh2112.inotesmonitor.inotesdata;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jregex.Matcher;
import jregex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.josh2112.inotesmonitor.INotesClient.MessageCheckPageResult;
import com.josh2112.inotesmonitor.INotesClient;
import com.josh2112.inotesmonitor.MessageCheckService;
import com.josh2112.inotesmonitor.database.NotesLocalDatabase;
import com.josh2112.inotesmonitor.database.Tables;
import com.josh2112.inotesmonitor.database.tables.records.NotesMessageRecord;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage.MessageType;

public class INotesParser {

	private static Log log = LogFactory.getLog( INotesParser.class );
	
	private static DateTimeFormatter emailDateTimeFormatter = DateTimeFormatter.ofPattern( "MM/dd/yyyy hh:mma" );
	
	public static MessageCheckPageResult parseMessages( String htmlSrc ) throws Exception { 
		Document doc = Jsoup.parse( htmlSrc );
		
		if( !doc.select( "form[action=/names.nsf?Login]" ).isEmpty() ) {
			throw new MessageCheckService.AuthenticationExpiredException();
		}
		
		String pageNonce = doc.select( "input[name=%%Nonce]" ).attr( "value" );
		
		List<NotesMessageRecord> messages = new ArrayList<>();
		
		// Messages are represented as <div> tags with the CSS class .mailRowArea.
	    // Unread messages also have the class 'unread'. There is a checkbox for each message
	    // allowing you to select the message; it has an alphanumeric 'value' which appears
	    // to be a unique identifier for the message message... I hope so because I'm using
	    // it that way.
	    // 
	    // Inside each <div class="mailRowArea"> are divs with classes of .senderArea,
	    // .subjectArea and .dateArea. These contain details about the message.
		//
		Elements messageRows = doc.select( "div.mailRowArea" ); // "div.mailRowArea.unread" for unread messages
		
		for( Element messageRow : messageRows ) {
			NotesMessageRecord record = NotesLocalDatabase.getContext().newRecord( Tables.NotesMessage );
			record.setGUID( messageRow.select( ".check" ).attr( "value" ) );
			
			Element senderArea = messageRow.select( ".senderArea" ).first();
			
			String msgTypeStr = senderArea.select( ".hiddenLabel" ).text();
			if( msgTypeStr.contains( "Message" ) ) record.setMessageType( MessageType.EMAIL );
			else if( msgTypeStr.contains( "New invitation" ) )  record.setMessageType( MessageType.NEW_INVITATION );
			else if( msgTypeStr.contains( "Accepted invitation" ) )  record.setMessageType( MessageType.ACCEPTED_INVITATION );
			else if( msgTypeStr.contains( "Rescheduled" ) )  record.setMessageType( MessageType.RESCHEDULED_INVITATION );
			else if( msgTypeStr.contains( "Cancelled meeting" ) )  record.setMessageType( MessageType.CANCELLED_MEETING );
			else if( msgTypeStr.contains( "Decline invitation" ) )  record.setMessageType( MessageType.DECLINED_INVITATION );
			else if( msgTypeStr.contains( "Information" ) )  record.setMessageType( MessageType.UPDATED_MEETING );
			else throw new Exception( "Unknown message type: " + msgTypeStr );
			
			record.setIsRead( !messageRow.hasClass( "unread" ) );
			
			// Annoyingly the .senderArea also holds message icons, so
			// we need to use ownText() to get just the text content.
			record.setSender( senderArea.ownText() );
			
			record.setSubject( messageRow.select( ".subjectArea" ).text() );
			
			String dateStr = messageRow.select( ".dateArea" ).text();
			LocalDateTime date = LocalDateTime.parse( dateStr, emailDateTimeFormatter );
			record.setDate( date );
			
			messages.add( record );
		}
		
		return new MessageCheckPageResult( messages, pageNonce );
	}
	
	private static Pattern fieldPattern = new Pattern( "(\\w+)\\s*=\\s*('(?:(?:(?:\\\\')|(?:[^']))+)?'|\\d+(?:\\.\\d+)?)" );
	
	// This pattern matches the body-wrapped-in-JS-function we sometimes get, var XXX=Dnu('XXX')
	// This could probably be integrated into the fieldPattern regex, but I can't regex very well.
	private static Pattern wrappedBodyPattern = new Pattern( "(\\w+)\\s*=\\s*Dnu\\(('(?:(?:(?:\\\\')|(?:[^']))+)?'|\\d+(?:\\.\\d+)?)" );
	
	public static HashMap<String,String> getJavascriptFields( String src ) throws IOException {
		HashMap<String,String> fields = new HashMap<>();
		
		Matcher matcher = fieldPattern.matcher( src );
		while( matcher.find()) {
			String value = matcher.group( 2 );
			if( value.startsWith( "'" ) && value.endsWith( "'" ) ) {
				value = value.substring( 1, value.length()-1 );
			}
			fields.put( matcher.group( 1 ).toLowerCase(), value );
		}
		
		if( !fields.containsKey( "bodyhtml" ) ) {
			matcher = wrappedBodyPattern.matcher( src );
			if( matcher.find()) {
				String value = matcher.group( 2 );
				if( value.startsWith( "'" ) && value.endsWith( "'" ) ) {
					value = value.substring( 1, value.length()-1 );
				}
				fields.put( matcher.group( 1 ).toLowerCase(), value );
			}
		}
		
		Document doc = Jsoup.parse( src );
		fields.put( "%%Nonce", doc.select( "input[name=%%Nonce]" ).attr( "value" ) );
		
		return fields;
	}
}
