
package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import javafx.concurrent.Service;
import javafx.concurrent.Task;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.oauth2.Oauth2Scopes;
import com.josh2112.utility.Storage;

public class GoogleAuthenticationService extends Service<Credential> {
	
	private static File credentialsPath = new File( Storage.getInstance().getAppDataDirectory(), "credentials.json" );
	
	private static String clientSecretsResource = "/client_secrets.json";
	
	private static String credentialsUserID = "user";
	
	private AuthorizationCodeInstalledApp authFlow;
	
	public GoogleAuthenticationService( HttpTransport httpTransport, JsonFactory jsonFactory ) throws Exception {
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load( jsonFactory,
				new InputStreamReader( CalendarSelectionPage.class.getResourceAsStream( clientSecretsResource ) ));
		FileDataStoreFactory dataStore = new FileDataStoreFactory( credentialsPath );
		
		List<String> scopes = Arrays.asList( CalendarScopes.CALENDAR, Oauth2Scopes.USERINFO_PROFILE );
	    
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
	    		httpTransport, jsonFactory, clientSecrets, scopes ).setDataStoreFactory( dataStore ).build();
	    
	    authFlow = new AuthorizationCodeInstalledApp( flow, new LocalServerReceiver());
	}
	
	@Override
	protected Task<Credential> createTask() {
		return new Task<Credential>() {
			@Override protected Credential call() throws Exception {
				try {
					return authFlow.authorize( credentialsUserID );
				}
				catch( IOException e ) {
					return null;
				}
			}
		};
	}

	public static void removeCredentials() {
		deleteRecursive( credentialsPath );
	}
	
	private static boolean deleteRecursive(File path) {
	    if( path.isDirectory() ) {
	        for( File file : path.listFiles() ) {
	            if( !deleteRecursive( file ) ) return false;
	        } 
	    }
	    return path.delete();
	}

}
