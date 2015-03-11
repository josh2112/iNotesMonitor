package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import com.google.api.client.repackaged.com.google.common.base.Joiner;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;

public class LotusToGCalTranslator {
	
	public static DateTime localDateTimeToGoogleDateTime( LocalDateTime ldt ) {
		return new DateTime( Date.from( ldt.atZone( ZoneId.systemDefault() ).toInstant() ) );
	}

	public static EventDateTime localDateTimeToGoogleEventDateTime( LocalDateTime ldt ) {
		EventDateTime edt = new EventDateTime();
		edt.setDateTime( localDateTimeToGoogleDateTime( ldt ));
		return edt;
	}
	
	public static Date eventDateTimeToDate( EventDateTime edt ) {
		return new Date( edt.getDateTime().getValue() );
	}
	
	public static EventAttendee eventAttendeeFromName( String name ) {
		return eventAttendeeFromName( name, false );
	}
	
	public static EventAttendee eventAttendeeFromName( String name, boolean isOrganizer ) {
		EventAttendee attendee = new EventAttendee();
		attendee.setDisplayName( name );
		attendee.setEmail( Joiner.on( '.' ).join( name.split( "\\s+" ) ) + "@cem.com" );
		attendee.setOrganizer( isOrganizer );
		return attendee;
	}
}
