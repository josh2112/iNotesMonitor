package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import javafx.scene.layout.BorderPane;

import com.josh2112.javafx.FXMLLoader;
import com.josh2112.javafx.wizard.WizardContainer;
import com.josh2112.javafx.wizard.WizardPage;
import com.josh2112.javafx.wizard.WizardPageConfiguration;

public class NotesDataInputPage extends WizardPage {
	
	private MeetingConfiguration configuration;
	
	public NotesDataInputPage( WizardContainer container ) {
		super( container, new BorderPane());
		FXMLLoader.loadFXML( this.getRootNode(), this, "/fxml/NotesDataInputPage.fxml" );
	}
	
	@Override
	public void activate() {
	}
	
	@Override
	public void activateWithConfiguration( WizardPageConfiguration configuration ) {
		activate();
		this.configuration = (MeetingConfiguration)configuration;
		canNavigateToNextPageProperty().set( true );
		getWizardContainer().triggerNextButton();
	}
	
	@Override
	public void deactivate() {
	}
	
	@Override
	public boolean verify() {
		return true;
	}

	@Override
	public WizardPageConfiguration getConfiguration() {
		return configuration;
	}
}
