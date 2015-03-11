package com.josh2112.inotesmonitor;

import com.josh2112.javafx.FXMLLoader;

import javafx.scene.layout.Pane;

public abstract class LoadableContainer {

	private Pane container;
	
	public Pane getContainer() { return container; }
	
	protected LoadableContainer( String fxmlName ) {
		container = FXMLLoader.loadFXML( this, String.format( "/fxml/%s.fxml", fxmlName ) );
	}
}
