package com.josh2112.utility;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.josh2112.inotesmonitor.INotesMonitorMain;

public class Storage {

	private static Log log = LogFactory.getLog( Storage.class );
	
	private static Storage applicationStorage = null;
	
	public static Storage initApplicationStorage( String appName ) {
		return applicationStorage = new Storage( appName );
	}

	public static Storage getInstance() {
		if( applicationStorage != null ) return applicationStorage;
		else throw new IllegalArgumentException( "You must call initApplicationStorage( appName ) first!" );
	}
	
	public enum OperatingSystemFamily {
		WINDOWS,
		MAC,
		UNIX
	}
	
	private String appName;
	private OperatingSystemFamily osFamily;
	
	private Storage( String appName ) {
		this.appName = appName;
		String osName = System.getProperty( "os.name" );
		if( osName.contains( "Windows" ) ) osFamily = OperatingSystemFamily.WINDOWS;
		else if( osName.contains( "Mac" ) ) osFamily = OperatingSystemFamily.MAC;
		else osFamily = OperatingSystemFamily.UNIX;
	}
	
	public File getAppDirectory() {
		switch( osFamily ) {
			case WINDOWS: return new File( String.join( File.separator,
					System.getProperty( "user.home" ), "AppData", "Local", appName ) );
				
			case MAC: return new File( String.join( File.separator,
					System.getProperty( "user.home" ), "Library", "Application Support", appName ) );
							
			default: return new File( String.join( File.separator,
					System.getProperty( "user.home" ), ".local", appName ) );
		}
	}
	
	private File maybeCreateDirectory( File dir ) {
		if( !dir.isDirectory() ) dir.mkdirs();
		return dir;
	}
	
	public File getAppDataDirectory() {
		return maybeCreateDirectory( new File( getAppDirectory(), "data" ) );
	}
	
	public File getAppResourcesDirectory() {
		return maybeCreateDirectory( new File( getAppDirectory(), "resources" ) );
	}
	
	public Path resourcePathToFileSystemPath( String resourceUrl ) {
		Path resourcePath = Paths.get( ".", resourceUrl ).normalize();
		return getAppResourcesDirectory().toPath().resolve( resourcePath );
	}

	/**
	 * Extract the resources given by the URLs from the jar file and save them in the
	 * app resources directory if they don't already exist.
	 * @param resourceURLs
	 */
	public void maybeUnpackResources( String[] resourceURLs ) {
		for( String url : resourceURLs ) {
			Path extractPath = resourcePathToFileSystemPath( url );
			maybeCreateDirectory( extractPath.getParent().toFile() );
			File outFilePath = extractPath.toFile();
			if( !outFilePath.isFile() ) {
				InputStream in = this.getClass().getResourceAsStream( url );
				OutputStream out;
			    int readBytes;
			    byte[] buffer = new byte[4096];
			    try {
			    	out = new FileOutputStream( outFilePath );
			        while(( readBytes = in.read( buffer )) > 0 ) out.write( buffer, 0, readBytes );
			        out.close();
			        in.close();
			        log.info( String.format( "Extracted resource '%s' to path '%s'", url, extractPath ) );
			    }
			    catch( IOException e ) {
			    	log.fatal( String.format( "Failed to extract resource '%s'", url ), e );
			    }
			    
			}
		}
	}
}
