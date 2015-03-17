package com.josh2112.inotesmonitor.inotesdata;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import com.josh2112.inotesmonitor.database.Keys;
import com.josh2112.inotesmonitor.database.NotesLocalDatabase;
import com.josh2112.inotesmonitor.database.Tables;
import com.josh2112.inotesmonitor.database.tables.records.NotesMeetingAttendeeRecord;
import com.josh2112.inotesmonitor.database.tables.records.NotesMeetingDetailsRecord;
import com.josh2112.utility.Gettables;

public class NotesMeetingDetails {

	private static DateTimeFormatter jsDateTimeFormatter = DateTimeFormatter.ofPattern( "yyyyMMdd'T'HHmmss','x'Z'" );
	
	private StringProperty topic, location;
	
	protected NotesMeetingDetailsRecord meetingDetailsRecord;
	protected List<NotesMeetingAttendeeRecord> meetingAttendeeRecords;
	
	// Topic

	public StringProperty topicProperty() {
		if( topic == null ) {
			topic = new SimpleStringProperty( meetingDetailsRecord.getTopic() );
			topic.addListener( (prop, oldTopic, newTopic ) -> meetingDetailsRecord.setTopic( newTopic ) );
		}
		return topic;
	}
	
	public String getTopic() { return topicProperty().get(); }
	public void setTopic( String topic ) { topicProperty().set( topic ); }
	
	public String getChair() { return meetingDetailsRecord.getChair(); }
	
	// Location
	
	public StringProperty locationProperty() {
		if( location == null ) {
			location = new SimpleStringProperty( meetingDetailsRecord.getLocation() );
			location.addListener( (prop, oldLoc, newLoc ) -> meetingDetailsRecord.setLocation( newLoc ) );
		}
		return location;
	}
	
	public String getLocation() { return locationProperty().get(); }
	public void setLocation( String location ) { locationProperty().set( location ); }
	
	public LocalDateTime getStartDate() { return meetingDetailsRecord.getStartDate(); }
	public LocalDateTime getEndDate() { return meetingDetailsRecord.getEndDate(); }
	
	public Duration getDuration() {
		if( getStartDate() != null && getEndDate() != null ) {
			return Duration.between(
					getStartDate().toInstant( ZoneOffset.UTC ),
					getEndDate().toInstant( ZoneOffset.UTC ) );
		}
		else return Duration.ZERO;
	}
	
	public List<String> getAttendeeNames() {
		return meetingAttendeeRecords.stream().map( NotesMeetingAttendeeRecord::getName ).collect( Collectors.toList() );
	}	
	
	@Override
	public String toString() {
		return Gettables.toString( this );
	}
	
	private static LocalDateTime parseJavascriptDateTime( String str ) {
		// If the timezone has no sign, add a '+'.
		int commaIdx = str.indexOf( "," );
		if( Character.isDigit( str.charAt( commaIdx + 1 ) ) ) str = str.replace( ",", ",+" );
		
		ZonedDateTime zonedResult = ZonedDateTime.parse( str, jsDateTimeFormatter )
				.withZoneSameInstant( ZoneId.systemDefault() );
		return zonedResult.toLocalDateTime();
	}
	
	/////////////////////////
	
	public NotesMeetingDetails( NotesMeetingDetailsRecord record ) {
		this.meetingDetailsRecord = record;
	}
	
	public static NotesMeetingDetails fromCode( NotesMeetingDetailsRecord record, List<NotesMeetingAttendeeRecord> attendees )  {
		NotesMeetingDetails md = new NotesMeetingDetails( record );
		md.meetingAttendeeRecords = attendees;
		
		return md;
	}
		
	public static NotesMeetingDetails fromHtml( String guid, Map<String, String> fields ) {
		NotesMeetingDetailsRecord record = NotesLocalDatabase.getContext().newRecord( Tables.NotesMeetingDetails );
		
		// For a rescheduled meeting, "apptunid" is the GUID of the original meeting.
		record.setMeetingId( fields.get( "apptunid" ) );
		
		record.setTopic( fields.get( "topic" ) );
		record.setLocation( NotesMessage.fixLotusNotesIdentifier( fields.get( "room" ) ) );
		
		record.setChair( NotesMessage.fixLotusNotesIdentifier( fields.get( "chair" ) ) );
		
		record.setStartDate( parseJavascriptDateTime( fields.get( "startdatetime" ) ) );
		record.setEndDate( parseJavascriptDateTime( fields.get( "enddatetime" ) ) );
		
		NotesMeetingDetails md = new NotesMeetingDetails( record );
		md.meetingDetailsRecord.setGUID( guid );
		
		md.meetingAttendeeRecords = Arrays.stream( fields.get( "requiredattendees" ).split( ", " ) ).map( n -> {
			NotesMeetingAttendeeRecord attendee = NotesLocalDatabase.getContext().newRecord( Tables.NotesMeetingAttendee );
			attendee.setName( NotesMessage.fixLotusNotesIdentifier( n ) );
			return attendee;
		} ).collect( Collectors.toList() );
		
		return md;
	}
	
	public void load() {
		meetingAttendeeRecords = meetingDetailsRecord.fetchChildren( Keys.fk_NotesMeetingAttendee_NotesMeetingDetails_1 );
	}
	
	public void setStartEnd( LocalDateTime start, LocalDateTime end ) {
		meetingDetailsRecord.setStartDate( start );
		meetingDetailsRecord.setEndDate( end );
	}

	public void store() {
		if( meetingDetailsRecord.update() == 0 ) meetingDetailsRecord.store();
		
		for( NotesMeetingAttendeeRecord attendeeRecord : meetingAttendeeRecords ) {
			attendeeRecord.setMeetingDetailsId( meetingDetailsRecord.getID() );
			attendeeRecord.store();
		}
	}

	public void delete() {
		meetingDetailsRecord.delete();
		meetingAttendeeRecords.stream().forEach( r -> r.delete() );
	}
}
