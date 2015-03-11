package com.josh2112.javafx;

import java.io.IOException;

public class FXMLLoader {
	
	public static <T> T loadFXML( Object controller, String resourceLocation ) {
		return loadFXML( null, controller, resourceLocation );
    }
	
	public static <T> T loadFXML( Object root, Object controller, String resourceLocation ) {
		javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader( FXMLLoader.class.getResource( resourceLocation ));
		loader.setController( controller );
		if( root != null ) loader.setRoot( root );
		
		try {
			return loader.load();
		}
		catch( IOException e ) {
			// This signifies a coding error, so it's safe to rethrow it as an unchecked exception.
			// To catch unchecked exceptions gracefully while still showing a stack trace, you can
			// set a default uncaught exception handler:
			// Thread.setDefaultUncaughtExceptionHandler( (thread, e) -> {
			// 	Dialogs.create().title( "Exception" ).masthead( APP_NAME +
			// 			" has encountered a problem and needs to close." ).showException( e );
			// });
			throw new RuntimeException( e );
		}
	}
}
