package com.josh2112.inotesmonitor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

import org.controlsfx.dialog.Dialogs;

import com.josh2112.javafx.PlatformHelper;
import com.josh2112.utility.Storage;

public class Configuration {
	
	public enum Settings {
		// If true, username/password will be saved in an encrypted key file and automatically filled in
		SAVE_CREDENTIALS( "false" ),		
		// Number of minutes between new message checks
		UPDATE_FREQUENCY_MINUTES( "5" ),
		// Whether or not to show the tutorial at startup.
		SHOW_TUTORIAL( "true" );
		
		private String defaultValue;
		public String getDefaultValue() { return defaultValue; }
		
		Settings( String defaultValue ) {
			this.defaultValue = defaultValue;
		}
	}

	/** Singleton implementation **/
	
	private static Configuration instance;
	
	public static Configuration getInstance() {
		if( instance == null ) instance = new Configuration();
		return instance;
	}
	
	/*****************************/
	
	private static File propertiesFilePath = new File(
			Storage.getInstance().getAppDataDirectory(), "properties.cfg" );
	private Properties properties = new Properties();
	private boolean needsSave;
	private Timeline periodicSaver;
	
	private Configuration() {
		try { properties.load( new FileReader( propertiesFilePath ) ); }
		catch( Exception e ) {}
		
		periodicSaver = new Timeline( new KeyFrame( Duration.seconds( 60 ), new EventHandler<ActionEvent>() {
			@Override public void handle( ActionEvent event ) { maybeSaveSettings(); }
		}));
		periodicSaver.setCycleCount( Timeline.INDEFINITE );
		periodicSaver.play();
	}
	
	public void setString( Settings setting, String value ) {
		properties.setProperty( setting.toString(), value );
		needsSave = true;
	}
	
	public void setBool( Settings setting, boolean value ) {
		setString( setting, String.valueOf( value ) );
	}
	
	public void setInt( Settings setting, int value ) {
		setString( setting, String.valueOf( value ) );
	}
	
	public void setFloat( Settings setting, float value ) {
		setString( setting, String.valueOf( value ) );
	}
	
	public String getString( Settings setting ) {
		String value = properties.getProperty( setting.toString() );
		return value != null ? value : setting.getDefaultValue();
	}
	
	public boolean getBool( Settings setting ) {
		return Boolean.parseBoolean( getString( setting ) );
	}
	
	public int getInt( Settings setting ) {
		return Integer.parseInt( getString( setting ) );
	}
	
	public float getFloat( Settings setting ) {
		return Float.parseFloat( getString( setting ) );
	}
	
	public void saveSettings() {
		try {
			properties.store( new FileWriter( propertiesFilePath ), "Properties for the iNotesMonitor application" );
		}
		catch( Exception e ) {
			PlatformHelper.run( () -> Dialogs.create().title( "Error" ).masthead( null ).message( "Error saving settings." ) );
		}
	}
	
	private void maybeSaveSettings() {
		if( needsSave ) saveSettings();
		needsSave = false;
	}
}
