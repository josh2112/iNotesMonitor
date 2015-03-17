package com.josh2112.javafx;

import org.controlsfx.dialog.Dialog;

import javafx.scene.web.WebView;

public class WebViewDialog extends Dialog {

	private final WebView htmlViewer = new WebView();
	
	public WebViewDialog( Object owner, String title, String html ) {
		super( owner, title );
		
		htmlViewer.getEngine().loadContent( html );
		
		htmlViewer.getEngine().setJavaScriptEnabled( true );
	       
	    // create the dialog with a custom graphic and the gridpane above as the
	    // main content region
	    //setResizable(false);
	    setIconifiable( false );
	    //setGraphic(new ImageView(HelloDialog.class.getResource("login.png").toString()));
	    setContent( htmlViewer );
	    getActions().addAll( Dialog.Actions.OK );
	}
}
