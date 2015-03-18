package com.josh2112.inotesmonitor;

import javafx.scene.control.Label;
import javafx.scene.control.ListCell;

public class CategoryListCell extends ListCell<String> {
	
	public CategoryListCell() {
		Label label = new Label();
		label.setStyle( "-fx-text-fill: theme-foregroundDark" );
		label.textProperty().bind( itemProperty() );
		setGraphic( label );
	}
}
