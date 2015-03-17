package com.josh2112.inotesmonitor;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.josh2112.javafx.LoadableContainer;
import com.josh2112.utility.StringUtils;

public class StatusPanel extends LoadableContainer {

	private Log log = LogFactory.getLog( MeetingDetailsPanel.class );
	
	private static double fadeOutYPos = 30.0f;
	
	@FXML private Label statusLabel;
	
	private Timeline fadeInOutTimeline;
	
	public StringProperty statusText = new SimpleStringProperty();
	public StringProperty statusTextProperty() { return statusText; }
	public void setStatusText( String str ) { statusText.set( str ); }

	public StatusPanel() {
		super( "StatusPanel" );
		
		fadeInOutTimeline =  new Timeline(
				new KeyFrame( Duration.seconds( 0.2 ), "fadeInDone",
						new KeyValue( getContainer().opacityProperty(), 1.0f, Interpolator.EASE_OUT ),
						new KeyValue( getContainer().translateYProperty(), 0.0f, Interpolator.EASE_OUT )),
				new KeyFrame( Duration.seconds( 5 ), "holdDone",
						new KeyValue( getContainer().opacityProperty(), 1.0f ),
						new KeyValue( getContainer().translateYProperty(), 0.0f )),
				new KeyFrame( Duration.seconds( 5.2 ), "fadeOutDone",
						e -> statusLabel.setText( statusText.get() ),
						new KeyValue( getContainer().opacityProperty(), 0.0f, Interpolator.EASE_OUT ),
						new KeyValue( getContainer().translateYProperty(), fadeOutYPos, Interpolator.EASE_OUT )) );
		
		getContainer().setTranslateY( fadeOutYPos );
		
		statusTextProperty().addListener( (val, oldVal, newVal) -> {
			if( !StringUtils.isNullOrWhitespace( newVal ) ) {
				if( StringUtils.isNullOrWhitespace( oldVal ) ) {
					statusLabel.setText( newVal );
					fadeInOutTimeline.playFromStart();
				}
				else {
					statusLabel.setText( newVal );
					fadeInOutTimeline.playFrom( "fadeInDone" );
				}
			}
		} );
	}

}
