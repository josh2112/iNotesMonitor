package com.josh2112.inotesmonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.josh2112.utility.Storage;

public class CredentialStorage {

	private static File keystoreFile = new File( Storage.getInstance().getAppDataDirectory(), "keystore.dat" );
	private static String keystoreType = "JCEKS";
	private static String secretKeyFactoryType = "PBE";
	private static String usernameKey = "username";
	private static String passwordKey = "pwd";
	
	private static char[] keyStorePwd = "iNotesChecker".toCharArray();
	
	public static class Credentials {
		public char[] username, password;
		
		public void destroy() {
			Arrays.fill( username, 'x' );
			Arrays.fill( password, 'x' );
		}
		
		public Credentials( char[] uname, char[] pwd ) {
			username = uname;
			password = pwd;
		}
	}
	
	/***
	 * This method can throw a shitload of exceptions. Two of them are not really problems,
	 * IOException (file not found) and UnrecoverableKeyException (key(s) don't exist in the
	 * keystore -- these we will silently handle and return no credentials -- all the others
	 * we'll forward on.
	 * @return Credentials, or null if no credentials exist
	 * @throws various exceptions related to keystore and decrypting keys
	 */
	public static Credentials tryGetCredentials() throws Exception {
		PasswordProtection keyStorePP = new PasswordProtection( keyStorePwd );
		
		KeyStore ks = KeyStore.getInstance( keystoreType );
		try {
			ks.load( new FileInputStream( keystoreFile ), keyStorePwd );
		}
		catch( IOException e ) {
			return null;
		}
		
		SecretKeyFactory factory = SecretKeyFactory.getInstance( secretKeyFactoryType );
		PBEKeySpec unameKeySpec, pwdKeySpec;
		
		try {
			SecretKeyEntry ske = (SecretKeyEntry)ks.getEntry( usernameKey, keyStorePP );
			if( ske == null ) return null;
			unameKeySpec = (PBEKeySpec)factory.getKeySpec( ske.getSecretKey(), PBEKeySpec.class );
			
			ske = (SecretKeyEntry)ks.getEntry( passwordKey, keyStorePP );
			if( ske == null ) return null;
			pwdKeySpec = (PBEKeySpec)factory.getKeySpec( ske.getSecretKey(), PBEKeySpec.class );
		}
		catch( UnrecoverableKeyException e ) {
			return null;
		}
		
		Credentials creds = new Credentials( unameKeySpec.getPassword(), pwdKeySpec.getPassword() );
		unameKeySpec.clearPassword(); pwdKeySpec.clearPassword();
		return creds;
	}

	public static void storeCredentials( Credentials creds ) throws Exception {
		PasswordProtection keyStorePP = new PasswordProtection( keyStorePwd );
		
		KeyStore ks = KeyStore.getInstance( keystoreType );
		ks.load( null, keyStorePwd );
		
		SecretKeyFactory factory = SecretKeyFactory.getInstance( secretKeyFactoryType );
		
		SecretKey generatedSecret = factory.generateSecret( new PBEKeySpec( creds.username ) );
		ks.setEntry( usernameKey, new SecretKeyEntry( generatedSecret ), keyStorePP );
		
		generatedSecret = factory.generateSecret( new PBEKeySpec( creds.password ) );
		ks.setEntry( passwordKey, new SecretKeyEntry( generatedSecret ), keyStorePP );
		ks.store( new FileOutputStream( keystoreFile ), keyStorePwd );
	}

	public static void removeCredentials() throws Exception {
		KeyStore ks = KeyStore.getInstance( keystoreType );
		ks.load( null, keyStorePwd );
		ks.deleteEntry( usernameKey );
		ks.deleteEntry( passwordKey );
		ks.store( new FileOutputStream( keystoreFile ), keyStorePwd );
	}
}
