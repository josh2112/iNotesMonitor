package com.josh2112.javafx.wizard;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.stage.Stage;

public abstract class WizardPage {
	
	private WizardContainer wizardContainer;
	private Node rootNode;
	
	private BooleanProperty canNavigateToNextPage = new SimpleBooleanProperty();
	
	protected WizardPage( WizardContainer container, Node rootNode ) {
		this.wizardContainer = container;
		this.rootNode = rootNode;
	}
	
	public WizardContainer getWizardContainer() {
		return this.wizardContainer;
	}
	
	public Node getRootNode() { return rootNode; }
	protected void setRootNode( Node node ) { this.rootNode = node; }
	
	public Stage getStage() { return (Stage)rootNode.getScene().getWindow(); }
	
	/***
	 * This property indicates whether this page allows transitioning to the next page.
	 */
	public BooleanProperty canNavigateToNextPageProperty() { return canNavigateToNextPage; }
	
	public boolean getCanNavigateToNextPage() { return canNavigateToNextPage.get(); }
	
	/***
	 * Tells the page that it is being transitioned to.
	 */
	public abstract void activate();
	
	/***
	 * Tells the page that it is being transitioned to, passing it state
	 * information from a previous page.
	 */
	public abstract void activateWithConfiguration( WizardPageConfiguration configuration );
	
	/***
	 * Tells the page that it is being transitioned away from.
	 */
	public abstract void deactivate();
	
	/***
	 * Performs the page action and returns the result. If the result is false,
	 * the page transition will be cancelled. 
	 * @return
	 */
	public abstract boolean verify();
	
	/***
	 * Returns the state of the page in a format that can be understood by the 
	 * next page.
	 * @return
	 */
	public abstract WizardPageConfiguration getConfiguration();
}

