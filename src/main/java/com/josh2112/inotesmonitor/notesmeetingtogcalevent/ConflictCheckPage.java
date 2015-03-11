package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;

import org.controlsfx.dialog.Dialogs;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Event.Source;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.Events;
import com.josh2112.inotesmonitor.INotesMonitorMain;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage.MessageType;
import com.josh2112.javafx.FXMLLoader;
import com.josh2112.javafx.wizard.CardPaneBehavior;
import com.josh2112.javafx.wizard.WizardContainer;
import com.josh2112.javafx.wizard.WizardPage;
import com.josh2112.javafx.wizard.WizardPageConfiguration;

public class ConflictCheckPage extends WizardPage {

	@FXML private StackPane parentStackPane;
	@FXML private StackPane multipleReschedulePane;
	@FXML private StackPane notFoundReschedulePane;
	@FXML private StackPane okReschedulePane;
	@FXML private StackPane multiplePane;
	@FXML private StackPane updateExistingPane;
	
	@FXML private Text okRescheduleEventName;
	@FXML private Label okRescheduleEventLocation;
	@FXML private Label okRescheduleEventStart;
	@FXML private Label okRescheduleEventEnd;
	
	@FXML private Text updateExistingEventName;
	@FXML private Label updateExistingEventLocation;
	@FXML private Label updateExistingEventStart;
	@FXML private Label updateExistingEventEnd;
	
	private static Source APP_EVENT_SOURCE = null;
	
	private MeetingConfiguration pageConfig;
	
	{
		APP_EVENT_SOURCE = new Source();
		APP_EVENT_SOURCE.setTitle( "NotesMeetingToGCalEvent" );
		APP_EVENT_SOURCE.setUrl( "http://www.josh2112.com/apps/NotesMeetingToGCalEvent" );
	}

	public ConflictCheckPage( WizardContainer container ) {
		super( container, new BorderPane() );
		FXMLLoader.loadFXML( this.getRootNode(), this, "/fxml/ConflictCheckPage.fxml" );
		
		new CardPaneBehavior( parentStackPane );
	}
	
	private List<Event> getEventsWeCreatedBetween( LocalDateTime minTime, LocalDateTime maxTime ) throws IOException {
		String pageToken = null;
		List<Event> eventsWeCreated = null;
		
		do {
		    Events events = pageConfig.getCalendarClient().events().list( pageConfig.getCalendarId()).
		    		setPageToken( pageToken ).
		    		setTimeMin( LotusToGCalTranslator.localDateTimeToGoogleDateTime( minTime )).
		    		setTimeMax( LotusToGCalTranslator.localDateTimeToGoogleDateTime( maxTime )).
		    		execute();
		    
		    eventsWeCreated = events.getItems().stream()
		    		.filter( evt -> evt.getSource() != null && evt.getSource().getTitle().equals( APP_EVENT_SOURCE.getTitle() ) )
		    		.collect( Collectors.toList() );
		    
		    pageToken = events.getNextPageToken();
		}
		while( pageToken != null );
		
		return eventsWeCreated;
	}
	
	@FXML protected void handleCalendarLink( ActionEvent evt ) {
		INotesMonitorMain.getHostService().showDocument( "http://www.google.com/calendar" );
	}

	@Override
	public void activate() {
		List<Event> events = null;
		
		try {
			LocalDateTime minTime = pageConfig.getMeeting().getMeetingDetails().getStartDate();
			LocalDateTime maxTime = pageConfig.getMeeting().getMeetingDetails().getEndDate();
			
			if( pageConfig.getMeeting().getMessageType() == MessageType.RESCHEDULED_INVITATION ) {
				// We'll look for the previously-added event 6 days ahead and 6 days behind this event.
				// 1) If we extend the event search to a full week ahead or a full week behind,
				//    we're likely to find another instance of a weekly meeting which we mistake
				//    for this instance.
				// 2) When someone reschedules a meeting, they usually push it back a day or two, not a whole week.
				minTime = minTime.minusDays( 6 );
				maxTime = maxTime.plusDays( 6 );
			}
				
			events = getEventsWeCreatedBetween( minTime, maxTime );
			
		}
		catch( IOException e ) {
			e.printStackTrace();
			Dialogs.create().owner( getStage() ).title( "Data Error" ).message( "Error retreiving calendar data" ).showError();
			return;
		}
		
		List<Event> matchingEvents = events.stream()
				.filter( evt -> evt.getSummary().equals( pageConfig.getMeeting().getMeetingDetails().getTopic() ) )
				.collect( Collectors.toList() );
		
		DateFormat dateFormatter = new SimpleDateFormat( "EEE, MMM d 'at' hh:mm aa" );
		
		if( pageConfig.getMeeting().getMessageType() == MessageType.RESCHEDULED_INVITATION ) {
			// One of these events should be the previously-scheduled one.
			
			if( matchingEvents.size() > 1 ) {
				// Show UI saying we have multiple events and don't know which one this is a
				// duplicate of, we give up.
				multipleReschedulePane.setVisible( true );
				canNavigateToNextPageProperty().set( false );
			}
			else if( matchingEvents.size() == 0 ) {
				// Show UI saying we're unable to find the previously-added event... if
				// user continues we'll add the event as normal.
				notFoundReschedulePane.setVisible( true );
				canNavigateToNextPageProperty().set( true );
			}
			else if( matchingEvents.size() == 1 ) {
				// Show UI saying we'll replace the found event.
				Event event = matchingEvents.get( 0 );
				pageConfig.setEventToReplace( event );
				okReschedulePane.setVisible( true );
				okRescheduleEventName.setText( event.getSummary());
				okRescheduleEventLocation.setText( event.getLocation() );
				okRescheduleEventStart.setText( dateFormatter.format(
						LotusToGCalTranslator.eventDateTimeToDate( event.getStart())));
				okRescheduleEventEnd.setText( dateFormatter.format(
						LotusToGCalTranslator.eventDateTimeToDate( event.getEnd())));
				
				canNavigateToNextPageProperty().set( true );
			}
		}
		else {
			if( matchingEvents.size() > 1 ) {
				// Show UI saying multiple events already match this description
				// and we give up.
				multiplePane.setVisible( true );
				canNavigateToNextPageProperty().set( false );
			}
			else if( matchingEvents.size() == 1 ) {
				Event event = matchingEvents.get( 0 );
				pageConfig.setEventToReplace( event );
				updateExistingPane.setVisible( true );
				updateExistingEventName.setText( event.getSummary());
				updateExistingEventLocation.setText( event.getLocation() );
				updateExistingEventStart.setText( dateFormatter.format(
						LotusToGCalTranslator.eventDateTimeToDate( event.getStart())));
				updateExistingEventEnd.setText( dateFormatter.format(
						LotusToGCalTranslator.eventDateTimeToDate( event.getEnd())));
				
				canNavigateToNextPageProperty().set( true );
			}
			else if( matchingEvents.size() == 0 ) {
				canNavigateToNextPageProperty().set( true );
				this.getWizardContainer().triggerNextButton();
			}
		}
	}

	@Override
	public void activateWithConfiguration( WizardPageConfiguration configuration ) {
		pageConfig = (MeetingConfiguration)configuration;
		activate();
	}

	@Override
	public void deactivate() {
	}

	@Override
	public boolean verify() {
		if( pageConfig.getEventToReplace() != null ) {
			try {
				deleteEvent( pageConfig.getCalendarClient(), pageConfig.getCalendarId(), pageConfig.getEventToReplace() );
			}
			catch( IOException e ) {
				e.printStackTrace();
				Dialogs.create().owner( getStage() ).message( "Error deleting old event." ).title( "Error" ).showError();
				return false;
			}
		}
		
		try {
			Event event = addEvent( pageConfig.getCalendarClient(), pageConfig.getCalendarId(), lotusNotesMeetingToGCalEvent( pageConfig.getMeeting() ) );
			pageConfig.setAddedEvent( event );
			pageConfig.getActionListener().acceptMeeting( pageConfig.getMeeting() );
			return true;
		}
		catch( GoogleJsonResponseException e ) {
			e.printStackTrace();
			Dialogs.create().owner( getStage() ).message( e.getDetails().getMessage() ).title( "Data Error" ).showError();
			
		}
		catch( Exception e ) {
			e.printStackTrace();
			Dialogs.create().owner( getStage() ).message( "Error retreiving user and calendar data" ).title( "Data Error" ).showError();
		}
		
		return false;
	}
	
	private static Event lotusNotesMeetingToGCalEvent( NotesMessage meeting ) {
		Event newEvent = new Event();
		
		List<EventAttendee> attendees = new ArrayList<EventAttendee>();
		attendees.add( LotusToGCalTranslator.eventAttendeeFromName( meeting.getMeetingDetails().getChair(), true ) );
		
		for( String name : meeting.getMeetingDetails().getAttendeeNames()) {
			attendees.add( LotusToGCalTranslator.eventAttendeeFromName( name ) );
		}
		
		newEvent.setSource( APP_EVENT_SOURCE );
		newEvent.setSummary( meeting.getMeetingDetails().getTopic() );
		newEvent.setDescription( meeting.getBody() );
		newEvent.setLocation( meeting.getMeetingDetails().getLocation() );
		newEvent.setAttendees( attendees );
		newEvent.setStart( LotusToGCalTranslator.localDateTimeToGoogleEventDateTime( meeting.getMeetingDetails().getStartDate() ) );
		newEvent.setEnd( LotusToGCalTranslator.localDateTimeToGoogleEventDateTime( meeting.getMeetingDetails().getEndDate() ) );
		
		return newEvent;
	}

	private static void deleteEvent( com.google.api.services.calendar.Calendar calendar, String calendarId,
			Event event ) throws IOException {
		calendar.events().delete( calendarId, event.getId() ).execute();
	}
	
	private static Event addEvent( com.google.api.services.calendar.Calendar calendar, String calendarId, Event event ) throws Exception {
		return calendar.events().insert( calendarId, event ).execute();
	}

	@Override
	public WizardPageConfiguration getConfiguration() {
		return pageConfig;
	}
}
