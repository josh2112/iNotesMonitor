package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.layout.BorderPane;

import com.josh2112.inotesmonitor.INotesMonitorMain;
import com.josh2112.javafx.FXMLLoader;
import com.josh2112.javafx.wizard.WizardContainer;
import com.josh2112.javafx.wizard.WizardPage;
import com.josh2112.javafx.wizard.WizardPageConfiguration;

public class SummaryPage extends WizardPage {

	@FXML private Hyperlink eventLink;
	
	private MeetingConfiguration pageConfig;

	public SummaryPage( WizardContainer container ) {
		super( container, new BorderPane() );
		FXMLLoader.loadFXML( this.getRootNode(),  this, "/fxml/SummaryPage.fxml" );
	}
	
	@FXML protected void handleEventLink( ActionEvent e ) {
		INotesMonitorMain.getHostService().showDocument( pageConfig.getAddedEvent().getHtmlLink());
	}

	@Override
	public void activate() {
		eventLink.setText( pageConfig.getMeeting().getMeetingDetails().getTopic() );
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
		return false;
	}

	@Override
	public WizardPageConfiguration getConfiguration() {
		return null;
	}

}
