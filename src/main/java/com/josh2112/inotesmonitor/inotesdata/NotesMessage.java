package com.josh2112.inotesmonitor.inotesdata;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.apache.commons.lang3.StringEscapeUtils;

import com.josh2112.inotesmonitor.database.Keys;
import com.josh2112.inotesmonitor.database.NotesLocalDatabase;
import com.josh2112.inotesmonitor.database.Tables;
import com.josh2112.inotesmonitor.database.tables.records.NotesMeetingAttendeeRecord;
import com.josh2112.inotesmonitor.database.tables.records.NotesMeetingDetailsRecord;
import com.josh2112.inotesmonitor.database.tables.records.NotesMessageRecipientRecord;
import com.josh2112.inotesmonitor.database.tables.records.NotesMessageRecord;
import com.josh2112.utility.Gettables;

public class NotesMessage {
	
	public enum MessageType {
		EMAIL( "Email" ),
		NEW_INVITATION( "New invitation" ),
		ACCEPTED_INVITATION( "Accepted invitation" ),
		RESCHEDULED_INVITATION( "Rescheduled invitation" ),
		CANCELLED_MEETING( "Cancelled meeting" ),
		DECLINED_INVITATION( "Declined invitation" ),
		UPDATED_MEETING( "Updated meeting" );
		
		private String label;
		
		private MessageType( String label ) { this.label = label; }
		
		public String getLabel() { return label; }
		
		private static List<MessageType> typesWithMeetingDetails = Arrays.asList(
			NEW_INVITATION, ACCEPTED_INVITATION, DECLINED_INVITATION
		);
		
		public boolean isMessage() { return this.equals( EMAIL ); }
		public boolean isMeeting() { return !isMessage(); }
		public boolean hasMeetingDetails() { return typesWithMeetingDetails.contains( this ); }
	}
	
	private boolean isLoaded;
	
	protected NotesMessageRecord messageRecord;
	
	private String messageNonce, pageNonce;
	protected ObjectProperty<NotesMeetingDetails> meetingDetails = new SimpleObjectProperty<NotesMeetingDetails>();
	
	private BooleanProperty read;
	private ReadOnlyBooleanWrapper meeting;
	private StringProperty body;
	private ObjectProperty<List<NotesMessageRecipientRecord>> recipients = new SimpleObjectProperty<>();
	
	// The nonce contained in the message content. This is required to invoke actions
	// that originate from the message content page (such as marking a message as unread)
	public String getMessageNonce() { return messageNonce; }
	
	// The nonce contained in the page that listed this message.
	public String getPageNonce() { return pageNonce; }
	public void setPageNonce( String pageNonce ) { this.pageNonce = pageNonce; }
	
	public ObjectProperty<NotesMeetingDetails> meetingDetailsProperty() { return meetingDetails; }
	public NotesMeetingDetails getMeetingDetails() { return meetingDetails.get(); }
	
	public MessageType getMessageType() { return messageRecord.getMessageType(); }
	
	public String getGuid() { return messageRecord.getGUID(); }
	
	public LocalDateTime getDate() { return messageRecord.getDate(); }
	
	public String getSender() { return messageRecord.getSender(); }
	
	public String getSubject() { return messageRecord.getSubject(); }
	
	// Whether or not this message is a meeting.
	public boolean isMeeting() { return getMessageType().isMeeting(); }
	
	// Whether or not the message has been read. Pushes changes to the message record.
	
	public BooleanProperty readProperty() {
		if( read == null ) {
			read = new SimpleBooleanProperty( messageRecord.getIsRead() );
			read.addListener( (prop, wasRead, isRead ) -> messageRecord.setIsRead( isRead ) );
		}
		return read;
	}
	
	public boolean isRead() { return readProperty().get(); }
	public void setRead( boolean s ) { readProperty().set( s ); }
	
	// Body of the message as HTML content. Pushes changes to the message record.
	
	public StringProperty bodyProperty() {
		if( body == null ) {
			body = new SimpleStringProperty( messageRecord.getBody() );
			body.addListener( (prop, oldBody, newBody ) -> messageRecord.setBody( newBody ) );
		}
		return body;
	}
	
	public String getBody() { return bodyProperty().get(); }
	public void setBody( String b ) { bodyProperty().set( b ); }
	
	// Message recipients
	
	public ReadOnlyObjectProperty<List<NotesMessageRecipientRecord>> recipientsProperty() { return recipients; }
	public List<NotesMessageRecipientRecord> getRecipients() { return recipients.get(); }
	
	//////////////////////////////
	
	public NotesMessage( NotesMessageRecord record ) {
		this.messageRecord = record;
	}
	
	public String toString() {
		return Gettables.toString( this );
	}
	
	public static NotesMessage fromCode( NotesMessageRecord record, List<NotesMessageRecipientRecord> recipients,
			NotesMeetingDetailsRecord meetingRecord, List<NotesMeetingAttendeeRecord> attendees ) {
		NotesMessage msg = new NotesMessage( record );
		msg.isLoaded = true;
		msg.recipients.set( recipients );
		if( meetingRecord != null ) msg.meetingDetails.set( NotesMeetingDetails.fromCode( meetingRecord, attendees ) );
		return msg;
	}
	
	public static NotesMessage fromHtml( NotesMessageRecord record, Map<String, String> fields ) {
		NotesMessage msg = new NotesMessage( record );
		msg.isLoaded = true;
		
		/*try( PrintWriter out = new PrintWriter( "test/fields-" + msg.messageRecord.getGUID() + ".txt" ) ) {
			fields.entrySet().stream().forEach( e -> out.println( String.format( "%s = %s", e.getKey(), e.getValue() )) );
		}
		catch( FileNotFoundException e ) {
			System.err.println( "Can't create fields file" );
		}
		*/
		
		msg.recipients.set( Arrays.stream( fields.get( "sendto" ).split( ", " ) ).map( n -> {
			NotesMessageRecipientRecord attendee = NotesLocalDatabase.getContext().newRecord( Tables.NotesMessageRecipient );
			attendee.setGUID( msg.getGuid() );
			attendee.setName( fixLotusNotesIdentifier( n ) );
			return attendee;
		} ).collect( Collectors.toList() ) );
		
		msg.messageNonce = fields.get( "%%Nonce" );
		msg.setBody( StringEscapeUtils.unescapeJava( fields.get( "bodyhtml" ) ) );
		
		if( msg.isMeeting() ) {
			msg.meetingDetails.set( NotesMeetingDetails.fromHtml( msg.messageRecord.getGUID(), fields ) );
		}
		
		return msg;
	}
	
	public void load() {
		if( !isLoaded ) {
			isLoaded = true;
			recipients.set( messageRecord.fetchChildren( Keys.fk_NotesMessageRecipient_NotesMessage_1 ) );
			
			if( isMeeting() ) { 
				NotesMeetingDetails meeting = new NotesMeetingDetails( messageRecord.fetchChild(
						Keys.fk_NotesMeetingDetails_NotesMessage_1 ) );
				meeting.load();
				meetingDetails.set( meeting );
			}
		}
	}

	public void store() {
		if( messageRecord.update() == 0 ) messageRecord.store();
		
		for( NotesMessageRecipientRecord recipient : recipients.get() ) recipient.store();
		if( isMeeting() ) meetingDetails.get().store();
	}
	
	protected static String fixLotusNotesIdentifier( String item ) {
		if( item == null ) return "";
		else return item.replace( "CN=", "" ).replace( "O=", "" ).replace( "@CEM", "" );
	}
}
