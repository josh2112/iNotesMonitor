package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;

import com.josh2112.inotesmonitor.inotesdata.NotesMeetingDetails;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.javafx.FXMLLoader;
import com.josh2112.javafx.wizard.WizardContainer;
import com.josh2112.javafx.wizard.WizardPage;
import com.josh2112.javafx.wizard.WizardPageConfiguration;

public class MeetingDetailsPage extends WizardPage {

	@FXML private TextField eventNameLabel;
	@FXML private Label startDateLabel;
	@FXML private Label endDateLabel;
	@FXML private TextField locationTextField;
	@FXML private FlowPane attendeesPane;
	@FXML private TextArea detailsTextArea;
	
	private MeetingConfiguration pageConfig;
	
	private InvalidationListener fieldsChangedListener = (observable) -> {
			canNavigateToNextPageProperty().set( pageConfig.getMeeting() != null &&
					!pageConfig.getMeeting().getMeetingDetails().getTopic().isEmpty() &&
					!pageConfig.getMeeting().getMeetingDetails().getLocation().isEmpty());
		};
	
	public MeetingDetailsPage( WizardContainer container ) {
		super( container, new BorderPane() );
		FXMLLoader.loadFXML( this.getRootNode(), this, "/fxml/MeetingDetailsPage.fxml" );
	}

	@Override
	public void activate() {
		pageConfig.getMeeting().getMeetingDetails().topicProperty().addListener( fieldsChangedListener );
		pageConfig.getMeeting().getMeetingDetails().locationProperty().addListener( fieldsChangedListener );
		
		fieldsChangedListener.invalidated( null );
	}

	@Override
	public void activateWithConfiguration( WizardPageConfiguration configuration ) {
		pageConfig = (MeetingConfiguration)configuration;
		
		NotesMessage meeting = pageConfig.getMeeting();
		NotesMeetingDetails meetingDetails = meeting.getMeetingDetails();
		
		Bindings.bindBidirectional( eventNameLabel.textProperty(), meetingDetails.topicProperty() );
		Bindings.bindBidirectional( locationTextField.textProperty(), meetingDetails.locationProperty() );
		Bindings.bindBidirectional( detailsTextArea.textProperty(), meeting.bodyProperty() );
		startDateLabel.setText( meetingDetails.getStartDate().format( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.MEDIUM ) ) );
		endDateLabel.setText( meetingDetails.getEndDate().format( DateTimeFormatter.ofLocalizedDateTime( FormatStyle.MEDIUM ) ) );
		
		attendeesPane.getChildren().clear();
		
		List<String> attendees = new ArrayList<String>(
				meetingDetails.getAttendeeNames().subList( 0, Math.min( meetingDetails.getAttendeeNames().size(), 4 ) ));
		int remainder = meetingDetails.getAttendeeNames().size() - attendees.size();
		if( remainder > 0 ) attendees.add( "+ " + remainder + " more" );
		attendees.add( 0, meetingDetails.getChair() );
		
		for( String attendee : attendees ) {
			Label attendeeLabel = new Label( attendee );
			attendeeLabel.getStyleClass().add( "attendee" );
			attendeesPane.getChildren().add( attendeeLabel );
		}
		
		activate();
	}

	@Override
	public void deactivate() {
		NotesMeetingDetails meetingDetails = pageConfig.getMeeting().getMeetingDetails();
		meetingDetails.topicProperty().removeListener( fieldsChangedListener );
		meetingDetails.locationProperty().removeListener( fieldsChangedListener );
	}

	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public WizardPageConfiguration getConfiguration() {
		return pageConfig;
	}

}
