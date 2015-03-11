package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import com.google.api.services.calendar.model.Event;
import com.josh2112.inotesmonitor.NotesMessageCardActionListener;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.javafx.wizard.WizardPageConfiguration;

public class MeetingConfiguration extends WizardPageConfiguration {
	
	private NotesMessage meeting;
	private NotesMessageCardActionListener actionListener;
	private com.google.api.services.calendar.Calendar calendarClient;
	private String calendarId;
	
	private Event eventToReplace;
	private Event addedEvent;
	
	public MeetingConfiguration( NotesMessage meeting, NotesMessageCardActionListener actionListener  ) {
		this.meeting = meeting;
		this.actionListener = actionListener;
	}
	
	public NotesMessage getMeeting() {
		return meeting;
	}
	
	public NotesMessageCardActionListener getActionListener() {
		return actionListener;
	}

	public void setCalendarClient( com.google.api.services.calendar.Calendar calendarClient ) {
		this.calendarClient = calendarClient;
	}
	
	public com.google.api.services.calendar.Calendar getCalendarClient() {
		return calendarClient;
	}

	public void setCalendarId( String id ) {
		this.calendarId = id;
	}
	
	public String getCalendarId() {
		return calendarId;
	}

	public void setEventToReplace( Event event ) {
		this.eventToReplace = event;
	}

	public Event getEventToReplace() {
		return eventToReplace;
	}

	public void setAddedEvent( Event addedEvent ) {
		this.addedEvent = addedEvent;
	}
	
	public Event getAddedEvent() {
		return addedEvent;
	}
}
