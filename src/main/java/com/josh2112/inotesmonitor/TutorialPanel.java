package com.josh2112.inotesmonitor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import com.josh2112.inotesmonitor.database.tables.records.NotesMeetingAttendeeRecord;
import com.josh2112.inotesmonitor.database.tables.records.NotesMeetingDetailsRecord;
import com.josh2112.inotesmonitor.database.tables.records.NotesMessageRecipientRecord;
import com.josh2112.inotesmonitor.database.tables.records.NotesMessageRecord;
import com.josh2112.inotesmonitor.inotesdata.NotesMeetingDetails;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage.MessageType;
import com.josh2112.javafx.FXMLLoader;

public class TutorialPanel {
	
	private Pane container;
	@FXML private VBox panel1;
	@FXML private VBox panel2;
	@FXML private VBox panel3;
    @FXML private ListView<NotesMessage> msgListClick;
    @FXML private ListView<NotesMessage> msgListGCal;
    @FXML private ListView<NotesMessage> msgListSwipe;
	
	private BooleanProperty isDone = new SimpleBooleanProperty();
	public BooleanProperty isDoneProperty() { return isDone; }
	public boolean isDone() { return isDone.get(); }
	
	NotesMessage meeting;
	
	public TutorialPanel() {
		container = FXMLLoader.loadFXML( this, "/fxml/TutorialPanel.fxml" );
		
		panel2.setVisible( false );
		panel3.setVisible( false );
		
		String[] names = { "Walter White", "Skylar White", "Jesse Pinkman", "Tuco" };
		List<NotesMessageRecipientRecord> recipients = Arrays.stream( names ).map( n -> {
			NotesMessageRecipientRecord r = new NotesMessageRecipientRecord();
			r.setName( null );
			return r;
		} ).collect( Collectors.toList() );
		
		List<NotesMeetingAttendeeRecord> attendees = Arrays.stream( names ).map( n -> {
			NotesMeetingAttendeeRecord a = new NotesMeetingAttendeeRecord();
			a.setName( null );
			return a;
		} ).collect( Collectors.toList() );
		
		NotesMessageRecord record1 = new NotesMessageRecord();
		record1.setMessageType( MessageType.EMAIL );
		record1.setGUID( "12345678901234567890123456789012" );
		record1.setDate( LocalDateTime.now() );
		record1.setSender( "Walter White" );
		record1.setSubject( "i am the danger" );
		NotesMessage sampleMsg = NotesMessage.fromCode( record1, recipients, null, null );
		
		NotesMessageRecord record2 = new NotesMessageRecord();
		record2.setMessageType( MessageType.NEW_INVITATION );
		record2.setGUID( "12345678901234567890123456789013" );
		record2.setDate( LocalDateTime.now() );
		record2.setSender( "Walter White" );
		record2.setSubject( "i am the danger" );
		NotesMeetingDetailsRecord mtgDetails = new NotesMeetingDetailsRecord();
		mtgDetails.setChair( "Gus Fring" );
		mtgDetails.setLocation( "Los Pollos Hermanos" );
		mtgDetails.setStartDate( LocalDateTime.now() );
		mtgDetails.setEndDate( LocalDateTime.now().plusMinutes( 90 ) );
		NotesMessage sampleMeeting = NotesMessage.fromCode( record2, recipients, mtgDetails, attendees );
		
		NotesMessageCardActionListener msgListClickListener = new NotesMessageCardActionListener() {
			@Override public void remove( NotesMessage item ) {}
			@Override public void addToGoogleCalendar( NotesMessage meeting ) {}
			@Override public void acceptMeeting( NotesMessage meeting ) {}
			
			@Override public void openInBrowser( NotesMessage msg ) {
				nextPanel();
			}
		};
		
		msgListClick.setCellFactory( (list) -> new NotesMessageListCell( ) );
		msgListClick.setItems( FXCollections.observableArrayList( sampleMsg ) );
		
		NotesMessageCardActionListener msgListGCalListener = new NotesMessageCardActionListener() {
			@Override public void openInBrowser( NotesMessage msg ) {}
			@Override public void remove( NotesMessage item ) {}
			@Override public void acceptMeeting( NotesMessage meeting ) {}
			
			@Override public void addToGoogleCalendar( NotesMessage meeting ) {
				nextPanel();
			}
		};
		
		msgListGCal.setCellFactory( (list) -> new NotesMessageListCell( ) );
		msgListGCal.setItems( FXCollections.observableArrayList( sampleMeeting ) );
				
		NotesMessageCardActionListener msgListSwipeListener = new NotesMessageCardActionListener() {
			@Override public void openInBrowser( NotesMessage msg ) {}
			@Override public void acceptMeeting( NotesMessage meeting ) {}
			@Override public void addToGoogleCalendar( NotesMessage meeting ) {}
			
			@Override public void remove( NotesMessage item ) {
				nextPanel();
			}
		};
		
		msgListSwipe.setCellFactory( (list) -> new NotesMessageListCell( ) );
		msgListSwipe.setItems( FXCollections.observableArrayList( sampleMeeting ) );
	}
	
	protected void nextPanel() {
		meeting.setRead( false );
		if( panel1.isVisible() ) {
			animatePanelTransition( panel1, panel2 );
		}
		else if( panel2.isVisible() ) {
			animatePanelTransition( panel2, panel3 );
		}
		else if( panel3.isVisible() ) {
			isDone.set( true );
		}
		
	}
	
	private void animatePanelTransition( Pane fromPanel, Pane toPanel ) {
		Platform.runLater( () -> new Timeline(
				new KeyFrame( Duration.ZERO, (event) -> toPanel.setVisible( true ),
						new KeyValue( toPanel.opacityProperty(), 0.0f, Interpolator.EASE_BOTH ),
						new KeyValue( toPanel.translateXProperty(), toPanel.getWidth(), Interpolator.EASE_BOTH )),
				new KeyFrame( Duration.seconds( 0.15 ), (event) -> fromPanel.setVisible( false ),
						new KeyValue( toPanel.opacityProperty(), 1.0f, Interpolator.EASE_BOTH ),
						new KeyValue( toPanel.translateXProperty(), 0, Interpolator.EASE_BOTH ),
						new KeyValue( fromPanel.opacityProperty(), 0.0f, Interpolator.EASE_BOTH ),
						new KeyValue( fromPanel.translateXProperty(), -fromPanel.getWidth(), Interpolator.EASE_BOTH )
						) ).play() );
	}
	
	public Pane getContainer() { return container; }

	public void detach() {
		// Fade out the container over 0.3 seconds, then remove it from its parent.
		Platform.runLater( () -> new Timeline(
				new KeyFrame( Duration.seconds( 0.3 ), (event) -> {
						((Pane)container.getParent()).getChildren().remove( container );
					},
					new KeyValue( container.opacityProperty(), 0.0f, Interpolator.EASE_IN ) ) ).play() );
	}
}
