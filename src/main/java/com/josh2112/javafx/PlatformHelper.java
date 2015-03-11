package com.josh2112.javafx;

import javafx.application.Platform;

/***
 * The {@link #run(Runnable)} method in this class allows you to run
 * something on the JavaFX Application Thread without worrying about
 * whether the caller is on that thread or not. If on the application
 * thread, the Runnable is run directly, otherwise it is scheduled to
 * be run on the application thread via {@link Platform.#runLater(Runnable)}.
 * 
 * @author Joshua Foster
 *
 */
public class PlatformHelper {
	
	/***
	 * Runs the {@link Runnable} on the JavaFX application thread.
	 * @param runnable the code to run
	 */
	public static void run( Runnable runnable ) {
        if( Platform.isFxApplicationThread() ) runnable.run();
        else Platform.runLater( runnable );
    }
}
