package com.josh2112.javafx.wizard;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class CardPaneBehavior {
	
	private final Pane parent;
	
	private Node childBeingModified;
	
	public CardPaneBehavior( Pane parent ) {
		this.parent = parent;
		
		for( final Node child : parent.getChildren()) {
			child.visibleProperty().addListener( new ChangeListener<Boolean>() {
				@Override
				public void changed( ObservableValue<? extends Boolean> observable,
						Boolean oldVal, Boolean newVal ) {
					if( newVal == true && childBeingModified == null ) {
						childBeingModified = child;

						for( Node child2 : CardPaneBehavior.this.parent.getChildren()) {
							if( child2 != child ) child2.setVisible( false );
						}
						
						childBeingModified = null;
					}
				}
			} );
		}
	}
}
