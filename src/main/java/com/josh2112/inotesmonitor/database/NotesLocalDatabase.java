package com.josh2112.inotesmonitor.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;

import com.josh2112.inotesmonitor.INotesMonitorMain;
import com.josh2112.utility.Storage;

public class NotesLocalDatabase {
	
	private static Log log = LogFactory.getLog( NotesLocalDatabase.class );
	
	// Won't get initialized until getInstance() is called
	private static NotesLocalDatabase instance = new NotesLocalDatabase();
	
	private static State taskState;
	
	public static ReadOnlyObjectProperty<State> initializationStateProperty() { return instance.dbInitTask.stateProperty(); }
	public static Throwable getInitializationException() { return instance.dbInitTask.getException(); }
	
	public static NotesLocalDatabase getInstance() {
		if( taskState != State.SUCCEEDED ) {
			System.err.println( "Please wait until database initialization completes before calling NotesLocalDatabase.getInstance()!");
			return null;
		}
		return instance;
	}
	
	public static DSLContext getContext() {
		if( taskState != State.SUCCEEDED ) {
			System.err.println( "Please wait until database initialization completes before calling NotesLocalDatabase.getInstance()!");
			return null;
		}
		return instance.context;
	}
	
	private Task<Void> dbInitTask = new DatabaseInitializationTask();

	
	private DSLContext context;
	private Connection conn;

	private NotesLocalDatabase() {
		log.debug( "Starting database initialization..." );
		
		dbInitTask.stateProperty().addListener( (prop, oldState, newState) -> taskState = newState );
		
		new Thread( dbInitTask ).start();
	}
	
	private class DatabaseInitializationTask extends Task<Void> {
		
		@Override protected void failed() {
			log.error( "Database initialization failed", getException() );
		};
		
		@Override protected void succeeded() {
			log.debug( "Database initialization succeeded" );
		}
		
		@Override protected Void call() throws Exception {
			DriverManager.registerDriver( new org.sqlite.JDBC() );
			
			conn = tryConnect();
			context = DSL.using( conn, SQLDialect.SQLITE );
			
			new SqlScriptRunner( NotesLocalDatabase.class.getResource( "/scripts/schema.sql" ) ).run( context );
			
			return null;
		}
	}
	
	private static Connection tryConnect() throws Exception {
		File appDataDir = new File( Storage.getInstance().getAppDataDirectory(), INotesMonitorMain.APP_NAME + ".sqlite3" );
		String relativePath = appDataDir.getAbsolutePath().replace( '\\', '/' );
		
		return DriverManager.getConnection( "jdbc:sqlite:" + relativePath );	
	}
	
	public void close() {
		if( conn != null ) try { conn.close(); }
    	catch( SQLException e ) { /* welp, */ }
    }
}
