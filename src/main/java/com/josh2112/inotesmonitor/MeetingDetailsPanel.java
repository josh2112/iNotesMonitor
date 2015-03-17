package com.josh2112.inotesmonitor;

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
import javafx.scene.control.LabelBuilder;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;






import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;






import com.josh2112.inotesmonitor.inotesdata.NotesMeetingDetails;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.javafx.LabelUtils;
import com.josh2112.javafx.LoadableContainer;

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
	@FXML private FlowPane attendeesPane;
	
	private NotesMessageCardActionListener cardActionListener;
	
	public MeetingDetailsPanel( NotesMessageCardActionListener cardActionListener ) {
		super( "MeetingDetailsPanel" );
		
		this.cardActionListener = cardActionListener;
		
		meetingTopicText.textProperty().bind( Bindings.selectString( meetingDetails, "topic" ) );
		meetingChairText.textProperty().bind( Bindings.selectString( meetingDetails, "chair" ) );
		meetingLocationText.textProperty().bind( Bindings.selectString( meetingDetails, "location" ) );
		
		meetingTypeLabel.textProperty().bind( Bindings.selectString( notesMessage, "messageType", "label" ) );
		
		final StringBinding dateTimeStringBinding = Bindings.createStringBinding( () -> {
			if( meetingDetails.get() != null ) {
				LocalDateTime startDate = meetingDetails.get().getStartDate();
				return startDate.format( shortDate ) +
						" at " + startDate.format( shortTime ) +
						" for " + meetingDetails.get().getDuration().toMinutes() + " minutes";
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
	
	private void addMeetingToCalendar() {
    	cardActionListener.addToGoogleCalendar( notesMessage.get() );
    }
    
    @FXML
    void handleAddToCalendarButton( ActionEvent event ) {
    	addMeetingToCalendar();
    }
}
