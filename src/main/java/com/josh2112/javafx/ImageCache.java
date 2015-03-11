package com.josh2112.javafx;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.image.Image;

/***
 * A cache for JavaFX images. This is a Singleton instance which
 * stores{@link Image} instances keyed by their URL. To load an
 * image:
 *   Image img = ImageCache.getInstance().getImage( "/path/to/image.png" ); 
 * 
 * @author Joshua Foster
 *
 */
public class ImageCache {
	
	private static ImageCache instance;
	
	/***
	 * Returns the Singleton instance of ImageCache.
	 * @return instance
	 */
	public static ImageCache getInstance() {
		if( instance == null ) instance = new ImageCache();
		return instance;
	}
	
	private Map<String,Image> images = new HashMap<>();
	
	protected ImageCache() {}
	
	/***
	 * Returns the cached Image for the given URL, loading and
	 * storing it if it's not already cached.
	 * @param url the URL of the Image.
	 * @return the Image
	 */
	public Image getImage( String url ) {
		if( url == null ) return null;
		if( !images.containsKey( url )) images.put( url, new Image( url ) );
		return images.get( url );
	}
}
