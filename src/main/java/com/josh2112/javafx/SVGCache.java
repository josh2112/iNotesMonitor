package com.josh2112.javafx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javafx.scene.image.Image;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/***
 * A cache for SVG paths. This is a Singleton instance which
 * stores SVG paths as strings keyed by their URL. To load an
 * image:
 *   String path = SVGCache.getInstance().getPath( "/path/to/svgFile.svg" ); 
 * 
 * @author Joshua Foster
 *
 */
public class SVGCache {
	
	private Log log = LogFactory.getLog( SVGCache.class );
	
	private static SVGCache instance;
	
	/***
	 * Returns the Singleton instance of SVGCache.
	 * @return instance
	 */
	public static SVGCache getInstance() {
		if( instance == null ) instance = new SVGCache();
		return instance;
	}
	
	private Map<String,String> svgPathsByFilePath = new HashMap<>();
	
	protected SVGCache() {}
	
	/***
	 * Returns the cached SVG path for the given file path,
	 * loading and storing it if it's not already cached.
	 * @param filePath the path to the SVG file
	 * @return the SVG path
	 */
	public String getPath( String filePath ) {
		if( filePath == null ) return null;
		if( !svgPathsByFilePath.containsKey( filePath )) {
			InputStream stream = getClass().getResourceAsStream( filePath );
			try( BufferedReader reader = new BufferedReader( new InputStreamReader( stream ) ) ) {
				svgPathsByFilePath.put( filePath, reader.lines().collect( Collectors.joining( "\n" ) ) );
			}
			catch( IOException e ) {
				log.error( "SVGCache: Unable to load file " + filePath, e );
			}
		}
		return svgPathsByFilePath.get( filePath );
	}
}
