package com.josh2112.javafx;

import org.controlsfx.dialog.Dialog;

import javafx.scene.web.WebView;

public class WebViewDialog extends Dialog {

	private final WebView htmlViewer = new WebView();
	
	public WebViewDialog( Object owner, String title, String html ) {
		super( owner, title );
		
		htmlViewer.getEngine().loadContent( html );
		
		htmlViewer.getEngine().setJavaScriptEnabled( true );
		
	    setIconifiable( false );
	    setContent( htmlViewer );
	    getActions().addAll( Dialog.Actions.OK );
	}
}
