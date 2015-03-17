package com.josh2112.javafx;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

public class LabelUtils {

	public static List<Node> makeLabelList( List<String> people, String styleClass, int limit ) {
		List<String> truncatedList = people.stream().limit( limit )
				.collect( Collectors.toCollection( ArrayList::new ) );
		List<String> extras = people.stream().skip( limit ).collect( Collectors.toList() );
		
		List<Node> nodeList = truncatedList.stream().map( p -> {
			Label lbl = new Label( p );
			lbl.getStyleClass().add( styleClass );
			return lbl;
		} ).collect( Collectors.toCollection( ArrayList::new ) );
		
		
		if( !extras.isEmpty() ) {
			Label lbl = new Label( "+ " + extras.size() + " more" );
			lbl.getStyleClass().add( styleClass );
			lbl.setTooltip( new Tooltip( extras.stream().collect( Collectors.joining( "\n" ) ) ) );
			nodeList.add( lbl );
		}
		
		return nodeList;
	}
}
