package com.josh2112.inotesmonitor;

import java.util.List;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;

import com.josh2112.inotesmonitor.CertificateManager.X509CertificateWrapper;
import com.josh2112.inotesmonitor.Configuration.Settings;
import com.josh2112.inotesmonitor.CredentialStorage.Credentials;
import com.josh2112.inotesmonitor.INotesClient.AuthenticationResult;
import com.josh2112.javafx.FXMLLoader;

public class LoginPanel {

	private Log log = LogFactory.getLog( LoginPanel.class );
	
	private Pane container;
	@FXML private BorderPane innerPanel;
	@FXML private TextField usernameTextField;
	@FXML private PasswordField passwordTextField;
	@FXML private CheckBox rememberMeCheckBox;
	@FXML private Label loginFailedLabel;
	@FXML private ProgressIndicator spinner;
	@FXML private Button okButton;
	
	private AuthenticationService authenticationService;
	
	private BooleanProperty isLoggedIn = new SimpleBooleanProperty();
	public BooleanProperty isLoggedInProperty() { return isLoggedIn; }
	
	private Timeline fadeInTimeline, fadeOutTimeline;
	
	public LoginPanel( INotesClient client ) {
		container = FXMLLoader.loadFXML( this, "/fxml/LoginPanel.fxml" );
		
		fadeInTimeline =  new Timeline(
					new KeyFrame( Duration.ZERO,
							new KeyValue( container.opacityProperty(), 0.0f ),
							new KeyValue( innerPanel.scaleXProperty(), 0.9f ),
							new KeyValue( innerPanel.scaleYProperty(), 0.9f )),
					new KeyFrame( Duration.seconds( 0.3 ),
							new KeyValue( container.opacityProperty(), 1.0f, Interpolator.EASE_OUT ),
							new KeyValue( innerPanel.scaleXProperty(), 1.0f, Interpolator.EASE_OUT ),
							new KeyValue( innerPanel.scaleYProperty(), 1.0f, Interpolator.EASE_OUT )) );

		// Fade out the container over 0.3 seconds, then remove it from its parent.
		fadeOutTimeline = new Timeline(
					new KeyFrame( Duration.seconds( 0.3 ), (finishedEvent) -> {
							((Pane)container.getParent()).getChildren().remove( container );
						},
						new KeyValue( container.opacityProperty(), 0.0f, Interpolator.EASE_IN ),
						new KeyValue( innerPanel.scaleXProperty(), 0.9f, Interpolator.EASE_IN ),
						new KeyValue( innerPanel.scaleYProperty(), 0.9f, Interpolator.EASE_IN )) );
		
		Credentials creds = null;
		
		try { creds = CredentialStorage.tryGetCredentials(); }
		catch( Exception e ) {
			Dialogs.create().owner( INotesMonitorMain.getParentWindow() ).message( "Unable to open the keystore." ).showError();
		}

		authenticationService = new AuthenticationService( client, creds );
		
		// If the authentication result is "need certificate", show the certificate dialog. If the
		// user accepts the certifs, retry the authentication.
		authenticationService.valueProperty().addListener( (value, oldVal, newVal) -> {
			log.info( "Authentication result: " + newVal );
			if( newVal == AuthenticationResult.NEED_CERTIFICATE ) {
				if( askToAcceptCertificates( client.getHost()) ) {
					authenticationService.restart();
				}
			}
		} );
		
		rememberMeCheckBox.setSelected( Configuration.getInstance().getBool( Settings.SAVE_CREDENTIALS ) );
		
		// Show the "login failed" label if the result of the authentication is failed
		loginFailedLabel.visibleProperty().bind( authenticationService.valueProperty().isEqualTo( AuthenticationResult.FAILED ) );
		
		// Show the spinner and disable the text fields and OK button when the authentication service is running
		spinner.visibleProperty().bind( authenticationService.runningProperty() );
		usernameTextField.disableProperty().bind( authenticationService.runningProperty() );
		passwordTextField.disableProperty().bind( authenticationService.runningProperty() );
		okButton.disableProperty().bind( authenticationService.runningProperty() );
		
		// Set isLoggedIn to true when authentication succeeds
		isLoggedIn.bind( authenticationService.valueProperty().isEqualTo( AuthenticationResult.SUCCESS ) );
		
		// Once we are logged in, save the credentials if the user has requested it
		isLoggedIn.addListener( (value, oldVal, newVal ) -> {
			if( newVal && rememberMeCheckBox.isSelected() ) {
				try { CredentialStorage.storeCredentials( authenticationService.creds ); }
				catch( Exception e ) {
					Dialogs.create().owner( INotesMonitorMain.getParentWindow() ).title( "Error" ).masthead( null )
						.message( "Unable to save your login information." ).showError();
				}
			}
			Configuration.getInstance().setBool( Settings.SAVE_CREDENTIALS, rememberMeCheckBox.isSelected() );
		} );
		
		// Finally, if we have credentials, fill them in and start the login.
		if( creds != null ) {
			usernameTextField.setText( String.valueOf( creds.username ) );
			passwordTextField.setText( String.valueOf( creds.password ) );
			authenticationService.start();
		}
		else {
			Platform.runLater( () -> usernameTextField.requestFocus() );
		}
	}
	
	public Pane getContainer() { return container; }
	
	protected boolean askToAcceptCertificates( String host ) {
		CertificateManager certifMgr = new CertificateManager( host );
		List<X509CertificateWrapper> certifs = null;
		
		try { certifs = certifMgr.getServerCertificates(); }
		catch( Exception e ) {
			Dialogs.create().owner( INotesMonitorMain.getParentWindow() ).message( "Error retreiving server certificates." ).showError();
			Platform.exit();
			return false;
		}
		
		Action result = Dialogs.create().owner( INotesMonitorMain.getParentWindow() ).title( "Accept Server Certificates" )
				.masthead( "Accept server certificates" ).message( "To sign in, you must accept the server's " +
						"self-signed certificates. Do you want to continue?" ).showConfirm();
		
		if( result == Dialog.Actions.YES) {
			certifMgr.acceptServerCertificates( certifs );
			return true;
		}
		
		return false;
	}

	@FXML protected void handleOkButton( ActionEvent evt ) {
		authenticationService.creds = new Credentials(
				usernameTextField.getText().toCharArray(),
				passwordTextField.getText().toCharArray() );
		authenticationService.restart();
	}
	
	@FXML protected void handleCancelButton( ActionEvent evt ) {
		if( authenticationService.isRunning()) authenticationService.cancel();
		else Platform.exit();
	}
	
	public class AuthenticationService extends Service<AuthenticationResult> {
		private INotesClient client;
		private CredentialStorage.Credentials creds;
		
		public AuthenticationService( INotesClient client, CredentialStorage.Credentials creds ) {
			this.client = client;
			setCredentials( creds );
		}
		
		public void setCredentials( CredentialStorage.Credentials creds ) {
			this.creds = creds;
		}
		
		@Override protected Task<AuthenticationResult> createTask() {
			return new Task<AuthenticationResult>() {
				@Override protected AuthenticationResult call() throws Exception {
					log.info( "Starting authentication service" );
					return client.authenticate( creds, CertificateManager.getKeyStoreFilePath() );
				}
			};
		}
	}
	
	public void attachAnimated( Pane parent ) {
		parent.getChildren().add( container );
		Platform.runLater( () -> fadeInTimeline.play() );
	}

	public void detachAnimated() {
		Platform.runLater( () -> fadeOutTimeline.play() );
	}
}
