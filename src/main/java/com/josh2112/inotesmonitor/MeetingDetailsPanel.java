package com.josh2112.inotesmonitor;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.shape.SVGPath;
import javafx.util.Pair;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.josh2112.inotesmonitor.inotesdata.NotesMeetingDetails;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.javafx.LabelUtils;
import com.josh2112.javafx.LoadableContainer;
import com.josh2112.javafx.SVGCache;

public class MeetingDetailsPanel extends LoadableContainer {
	
	private Log log = LogFactory.getLog( MeetingDetailsPanel.class );
	
	private ObjectProperty<NotesMessage> notesMessage = new SimpleObjectProperty<NotesMessage>();
	public ObjectProperty<NotesMessage> notesMessageProperty() { return notesMessage; }
	
	// Create a binding that returns the message's NotesMeetingDetails or null if not a meeting.
	private ObjectBinding<NotesMeetingDetails> meetingDetails = Bindings.select( notesMessage, "meetingDetails" );
	
	private static DateTimeFormatter shortTime = DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT );
    private static DateTimeFormatter shortDate = DateTimeFormatter.ofPattern( "MMM d" );
	
	@FXML private Label meetingTopicText, meetingChairText, meetingLocationText, meetingDateTimeText;
	@FXML private Label meetingTypeLabel;
	@FXML private SVGPath meetingTypeIcon;
	@FXML private FlowPane attendeesPane;
	
	private NotesMessageCardActionListener cardActionListener;
	
	public MeetingDetailsPanel( NotesMessageCardActionListener cardActionListener ) {
		super( "MeetingDetailsPanel" );
		
		this.cardActionListener = cardActionListener;
		
		meetingTopicText.textProperty().bind( Bindings.selectString( meetingDetails, "topic" ) );
		meetingChairText.textProperty().bind( Bindings.selectString( meetingDetails, "chair" ) );
		meetingLocationText.textProperty().bind( Bindings.selectString( meetingDetails, "location" ) );
		
		meetingTypeLabel.textProperty().bind( Bindings.selectString( notesMessage, "messageType", "label" ) );
		meetingTypeIcon.contentProperty().bind( Bindings.createObjectBinding( () -> {
			String path = NotesMessageListCell.iconPathByMessageType.getOrDefault(
					notesMessage.get().getMessageType(), null );
			return path != null ? SVGCache.getInstance().getPath( path ) : "";
		}, meetingDetails ) );
		
		final StringBinding dateTimeStringBinding = Bindings.createStringBinding( () -> {
			if( meetingDetails.get() != null ) {
				LocalDateTime startDate = meetingDetails.get().getStartDate();
				return startDate.format( shortDate ) +
						" at " + startDate.format( shortTime ) +
						" for " + makePrettyDuration( meetingDetails.get().getDuration() );
			}
			else return null;
		}, meetingDetails );
		
		meetingDateTimeText.textProperty().bind( dateTimeStringBinding );
		
		meetingDetails.addListener( (prop, oldDetails, newDetails ) -> {
			if( newDetails != null ) {
				List<String> attendees = new ArrayList<>( newDetails.getAttendeeNames() );
				attendees.add( 0, newDetails.getChair() );
				attendeesPane.getChildren().setAll( LabelUtils.makeLabelList( attendees, "attendee", 6 ) );
			}
		} );
	}
	
	/**
	 * Given a duration, returns a string in the format
	 * "X day[s] X hour[s] X minute[s] X second[s]", leaving out
	 * any unit that is 0.
	 * Example: Duration.ofSeconds( 97245 ) => "1 day 3 hours 45 seconds"
	 * @param duration
	 * @return pretty duration string
	 */
	private String makePrettyDuration( Duration duration ) {
		long totalSeconds = duration.getSeconds();
		
		
		List<Pair<String, Long>> pairs = new ArrayList<>();
		
		pairs.add( new Pair<>( "day", (long)Math.floor( totalSeconds / 86400 ) ) );
		pairs.add( new Pair<>( "hour", (long)Math.floor( totalSeconds / 3600 ) % 24 ) );
		pairs.add( new Pair<>( "minute", (long)Math.floor( totalSeconds / 60 ) % 60 ) );
		pairs.add( new Pair<>( "second", totalSeconds % 60 ) );
		
		return pairs.stream().filter( p -> p.getValue() > 0 )
				.map( p -> String.format( "%d %s%s", p.getValue(),
						p.getKey(), p.getValue() > 1 ? "s" : "" ) )
				.collect( Collectors.joining( " " ) );
	}

	private void addMeetingToCalendar() {
    	cardActionListener.addToGoogleCalendar( notesMessage.get() );
    }
    
    @FXML
    void handleAddToCalendarButton( ActionEvent event ) {
    	addMeetingToCalendar();
    }
}
