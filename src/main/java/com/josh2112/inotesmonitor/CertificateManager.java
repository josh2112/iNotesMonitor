package com.josh2112.inotesmonitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import com.josh2112.utility.Storage;


public class CertificateManager {
	
	private static String keyStoreFileName = "jssecacerts";
	private static File keyStoreFilePath = new File( Storage.getInstance().getAppDataDirectory(), keyStoreFileName );
	
	public static File getKeyStoreFilePath() { return keyStoreFilePath; }
	
	private String host;
	private KeyStore keyStore;
	private char[] passphrase = "changeit".toCharArray();
	
	public CertificateManager( String host ) {
		this.host = host;
		
		File file = keyStoreFilePath;
        if( !file.isFile() ) {
        	String defaultDir = String.join( File.separator, System.getProperty( "java.home" ), "lib", "security" );
        	file = new File( defaultDir, keyStoreFileName );
            if( !file.isFile() ) file = new File( defaultDir, "cacerts" );
        }
        try {
        	InputStream in = new FileInputStream( file );
        	keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        	keyStore.load( in, passphrase );
        	in.close();
        }
        catch( Exception e ) {
        	e.printStackTrace();
        }
	}
	
	public List<X509CertificateWrapper> getServerCertificates() throws Exception {
		List<X509CertificateWrapper> certificates = new ArrayList<>();
        
    	int port = 443;
        
        SSLContext context = SSLContext.getInstance( "TLS" );
        TrustManagerFactory tmf = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
        tmf.init( keyStore );
        X509TrustManager defaultTrustManager = (X509TrustManager)tmf.getTrustManagers()[0];
        SavingTrustManager tm = new SavingTrustManager( defaultTrustManager );
        context.init( null, new TrustManager[]{tm}, null );
        SSLSocketFactory factory = context.getSocketFactory();

        SSLSocket socket = (SSLSocket)factory.createSocket( host, port );
        socket.setSoTimeout( 10000 );
        try {
            System.out.println( "Starting SSL handshake..." );
            socket.startHandshake();
            socket.close();
            System.out.println();
            System.out.println( "No errors, certificate is already trusted" );
        } catch( SSLException e ) {
        }

        X509Certificate[] chain = tm.chain;
        if( chain == null ) {
            System.out.println( "Could not obtain server certificate chain" );
            return certificates;
        }
        
        for( int i=0; i<chain.length; ++i ) {
        	certificates.add( new X509CertificateWrapper( chain[i] ) );
        }
        
        return certificates;
	}
	
	public void acceptServerCertificates( List<X509CertificateWrapper> certificates ) {
		try {
			OutputStream out = new FileOutputStream( keyStoreFilePath );
			int certifNum = 1;
			for( X509CertificateWrapper certifWrapper : certificates ) {
		        String alias = host + "-" + certifNum++;
		        keyStore.setCertificateEntry( alias, certifWrapper.getCertificate() );
		        keyStore.store( out, passphrase );
			}
			out.close();
		}
        catch( Exception e ) {
        	e.printStackTrace();
        }
	}

    private static class SavingTrustManager implements X509TrustManager {

        private final X509TrustManager tm;
        private X509Certificate[] chain;

        SavingTrustManager(X509TrustManager tm) {
            this.tm = tm;
        }

        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            throw new UnsupportedOperationException();
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            this.chain = chain;
            tm.checkServerTrusted(chain, authType);
        }
    }
    
	public static class X509CertificateWrapper {
		private X509Certificate certificate;
		
		public X509CertificateWrapper( X509Certificate certif ) {
			certificate = certif;
		}
		
		public X509Certificate getCertificate() {
			return certificate;
		}
		
		public String toString() {
			StringBuilder sb = new StringBuilder();
		
			sb.append( String.format( "Subject: %s\n", certificate.getSubjectDN()));
			sb.append( String.format( "  Issuer: %s\n", certificate.getIssuerDN()));
			
			MessageDigest sha1 = null, md5 = null;
			try {
				sha1 = MessageDigest.getInstance( "SHA1" );
				md5 = MessageDigest.getInstance( "MD5" );
				sha1.update( certificate.getEncoded());
				md5.update( certificate.getEncoded());
				sb.append( String.format( "  sha1: %s\n", toHexString( sha1.digest())));
				sb.append( String.format( "  md5: %s\n", toHexString( md5.digest())));
			}
			catch( Exception e ) {
			}
			return sb.toString();
		}
		
		private static final char[] HEXDIGITS = "0123456789abcdef".toCharArray();

	    private static String toHexString(byte[] bytes) {
	        StringBuilder sb = new StringBuilder(bytes.length * 3);
	        for (int b : bytes) {
	            b &= 0xff;
	            sb.append(HEXDIGITS[b >> 4]);
	            sb.append(HEXDIGITS[b & 15]);
	            sb.append(' ');
	        }
	        return sb.toString();
	    }
	}
}
