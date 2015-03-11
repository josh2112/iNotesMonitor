package com.josh2112.inotesmonitor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.mortbay.log.Log;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.StringBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.shape.SVGPath;

import com.google.common.collect.ImmutableMap;
import com.josh2112.inotesmonitor.inotesdata.NotesMeetingDetails;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage.MessageType;
import com.josh2112.javafx.FXMLLoader;
import com.josh2112.javafx.SVGCache;

public class NotesMessageListCell extends ListCell<NotesMessage> {

	@FXML private SVGPath messageTypeImage;
    @FXML private Label senderLabel, dateTimeLabel, topicLabel;
    @FXML private GridPane gridPane;
    
    private static final ImmutableMap<MessageType, String> iconPathByMessageType = ImmutableMap.<MessageType,String>builder()
    		.put( MessageType.NEW_INVITATION, "/images/newInvitation.svg" )
    		.put( MessageType.ACCEPTED_INVITATION, "/images/acceptedInvitation.svg" )
			.put( MessageType.RESCHEDULED_INVITATION, "/images/rescheduledMeeting.svg" )
			.put( MessageType.CANCELLED_MEETING, "/images/cancelledMeeting.svg" )
			.put( MessageType.DECLINED_INVITATION, "/images/cancelledMeeting.svg" )
			.put( MessageType.UPDATED_MEETING, "/images/rescheduledMeeting.svg" ).build();
    
    private static DateTimeFormatter shortTime = DateTimeFormatter.ofLocalizedTime( FormatStyle.SHORT );
    private static DateTimeFormatter dayOfWeek = DateTimeFormatter.ofPattern( "EE" );
    private static DateTimeFormatter shortDate = DateTimeFormatter.ofPattern( "MMM d" );
    private static DateTimeFormatter longDate = DateTimeFormatter.ofPattern( "MMM d YYYY" );
     
    public NotesMessageListCell() {
    	
    	setGraphic( FXMLLoader.loadFXML( this, "/fxml/NotesMessageListCell.fxml" ) );
    	
    	// If no message, hide the card panel.
    	gridPane.visibleProperty().bind( itemProperty().isNotNull() );
    	
		senderLabel.textProperty().bind( Bindings.selectString( itemProperty(), "sender" ) );
		topicLabel.textProperty().bind( Bindings.selectString( itemProperty(), "subject" ) );
		
		// If the message is from today, display the time, otherwise display the date.
		final ObjectBinding<LocalDateTime> dateBinding = Bindings.select( itemProperty(), "date" );
		dateTimeLabel.textProperty().bind( Bindings.createStringBinding( () -> makePrettyDate( dateBinding.get() ), dateBinding ) );
		
		// Set the icon from the ImageCache based on the type of the current message.
		final ObjectBinding<MessageType> messageTypeBinding = Bindings.select( itemProperty(), "messageType" );
		messageTypeImage.contentProperty().bind( Bindings.createObjectBinding( () -> {
				String path = iconPathByMessageType.getOrDefault( messageTypeBinding.get(), null );
				return path != null ? SVGCache.getInstance().getPath( path ) : "";
			}, messageTypeBinding ) );
		
		// Dim the card panel when the message is read by making it slightly transparent.
		gridPane.opacityProperty().bind( Bindings.when( Bindings.selectBoolean( itemProperty(), "read" ) )
				.then( 0.8 ).otherwise( 1.0 ) );
    }
    
    /**
     * Makes a nice-looking date string according to the following rules:
     *  - If the date is today, return a short time (ex. "11:35 AM")
     *  - If the date is within the last 7 days, return the day of the week (ex. "TUESDAY" )
     *  - If the date is within the current year, return the short date (ex. "OCTOBER 12" )
     *  - Otherwise, return the long date.
     * @param dateTime LocalDateTime to convert
     * @return string representing "prettified" date/time.
     */
    private String makePrettyDate( LocalDateTime dateTime ) {
    	if( dateTime == null ) return null;
    	
    	LocalDate date = LocalDate.from( dateTime );
		DateTimeFormatter formatter = null;
		if( date.isEqual( LocalDate.now() ) ) {
			formatter = shortTime;
		}
		else if( dateTime.isAfter( LocalDateTime.now().minusDays( 7 ) ) ) {
			formatter = dayOfWeek;
		}
		else if( dateTime.getYear() == LocalDateTime.now().getYear() ) {
			formatter = shortDate;
		}
		else {
			formatter = longDate;
		}
		return dateTime.format( formatter ).toUpperCase();
	}
    
    /*
    // How far an item has to be dragged before it's activated
	private static double SWIPE_ACTIVATION_WIDTH = 150;
	
	
    private double dragStartX, dragStartY;
    
    @FXML
    void handleMousePressed( MouseEvent event ) {
    	if( event.getButton() == MouseButton.PRIMARY ) {
    		dragStartX = event.getSceneX();
    		dragStartY = event.getSceneY();
    	}
    }
    
    @FXML
    void handleMouseDragged( MouseEvent event ) {
    	if( event.getButton() == MouseButton.PRIMARY ) {
    		double xOffset = event.getSceneX() - dragStartX;
    		double yOffset = event.getSceneY() - dragStartY;
    		gridPane.setTranslateX( xOffset );
    		gridPane.setTranslateY( yOffset );
    		getGraphic().setOpacity( 1-Math.abs(xOffset)/gridPane.getWidth() );
    	}
    }
    
    @FXML
    void handleMouseReleased( MouseEvent event ) {
    	if( event.getButton() == MouseButton.PRIMARY ) {
    		double offset = event.getSceneX() - dragStartX; 
    		if( Math.abs( offset ) > SWIPE_ACTIVATION_WIDTH ) {
    			// Continue to animate the card off the screen, then remove the message.
    			double endX = offset > 0 ? gridPane.getWidth() : -gridPane.getWidth();
    			new Timeline(
    					new KeyFrame( Duration.seconds( 0.1 ), e -> {
    								removeMessage();
    								gridPane.setTranslateX( 0 );
    								getGraphic().setOpacity( 1 );
    							},
    							new KeyValue( gridPane.translateXProperty(), endX ),
    							new KeyValue( getGraphic().opacityProperty(), 0 )
    					)
    				).play();
    		}
    		else {
    			// Animate the card back to its starting position.
    			new Timeline(
					new KeyFrame( Duration.seconds( 0.1 ),
							new KeyValue( gridPane.translateXProperty(), 0 ),
							new KeyValue( getGraphic().opacityProperty(), 1 )
					)
				).play();
    		}
    	}
    	
    	private void removeMessage() {
    		cardActionListener.remove( getItem() );
    	}

    
    }
    */
}
