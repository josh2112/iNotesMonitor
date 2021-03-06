package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.josh2112.inotesmonitor.NotesMessageCardActionListener;
import com.josh2112.inotesmonitor.inotesdata.NotesMessage;
import com.josh2112.javafx.LoadableContainer;
import com.josh2112.javafx.wizard.WizardContainer;
import com.josh2112.javafx.wizard.WizardPage;

public class NotesMeetingToGCalEventWizard extends LoadableContainer implements WizardContainer {

	@FXML Button previousButton, nextButton;

	private Log log = LogFactory.getLog( NotesMeetingToGCalEventWizard.class );
	
	private NotesMessageCardActionListener actionListener;
	
	// currentPage property
	private ObjectProperty<Page> currentPage = new SimpleObjectProperty<Page>();
	public ObjectProperty<Page> currentPageProperty() { return currentPage; }
	private Page getCurrentPage() { return currentPage.get(); }
	
	public NotesMeetingToGCalEventWizard( NotesMessage meeting, NotesMessageCardActionListener actionListener ) {
		super( "NotesMeetingToGCalEventWizard" );
		
		this.actionListener = actionListener;
		
		log.info( "NotesMeetingToGCalEventWizard started" );
		
		currentPage.set( Page.MeetingDetails );
		
		WizardPage newPageInstance = getCurrentPage().getInstance( this );
		((BorderPane)getContainer()).setCenter( newPageInstance.getRootNode() );
		newPageInstance.activateWithConfiguration( new MeetingConfiguration( meeting, actionListener ) );
		
		newPageInstance.canNavigateToNextPageProperty().addListener( new InvalidationListener() {
			@Override public void invalidated( Observable arg0 ) {
				reevaluateNextButton();
			}
		} );
		
		previousButton.setDisable( true );
		reevaluateNextButton();
	}
	
	private void setCurrentPage( Page newPage ) {
		WizardPage currentPageInstance = getCurrentPage().getInstance( this );
		WizardPage newPageInstance = newPage.getInstance( this );
		
		boolean isAdvancing = newPage.ordinal() > getCurrentPage().ordinal();
		
		currentPageInstance.canNavigateToNextPageProperty().unbind();
		currentPageInstance.deactivate();
		
		if( isAdvancing ) newPageInstance.activateWithConfiguration( currentPageInstance.getConfiguration());
		else newPageInstance.activate();
		
		newPageInstance.canNavigateToNextPageProperty().addListener( new InvalidationListener() {
			@Override public void invalidated( Observable arg0 ) {
				reevaluateNextButton();
			}
		} );
		
		// TODO: Fancy transition
		((BorderPane)getContainer()).setCenter( newPageInstance.getRootNode() );
		
		currentPage.set( newPage );
		
		previousButton.setDisable( newPage.ordinal() == 0 );
		reevaluateNextButton();
	}
	
	// onAction handlers
	
	@FXML
	public void handlePreviousButton( ActionEvent event ) {
        setCurrentPage( Page.values()[getCurrentPage().ordinal()-1] );
    }
	
	@FXML
	public void handleNextButton( ActionEvent event ) {
		if( getCurrentPage().getInstance( this ).verify()) {
			setCurrentPage( Page.values()[getCurrentPage().ordinal()+1] );
		}
    }

	@FXML
	public void handleCancelButton( ActionEvent event ) {
		actionListener.cancelAddToGoogleCalendar();
    }
	
	// WizardContainer implementation

	/**
	 * Enables or disables the Next button. To be enabled, there has to 
	 * be a next page and the current page's canNavigateToNextPage() has to
	 * be true.
	 */
	@Override
	public void reevaluateNextButton() {
		nextButton.setDisable( !getCurrentPage().getInstance( this ).getCanNavigateToNextPage() ||
				getCurrentPage().ordinal() >= Page.values().length - 1 );
	}
	
	@Override
	public void triggerNextButton() {
		reevaluateNextButton();
		if( !nextButton.isDisabled()) {
			Platform.runLater( new Runnable() {
				@Override public void run() {
					nextButton.fire();
				}
			});
		}
	}
}
