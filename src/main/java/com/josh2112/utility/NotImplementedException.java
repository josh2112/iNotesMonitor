package com.josh2112.utility;

/***
 * An exception signifying that the method throwing it has not been
 * implemented yet. Used for rapid prototyping where you need to fill
 * out a bunch of method definitions to make the compiler happy, but
 * you want to remember to implement them at a later date.
 * 
 * @author Joshua Foster
 */
public class NotImplementedException extends RuntimeException {
	
	private static final long serialVersionUID = 1L;
	
	/***
	 * Creates an empty NotImplementedException.
	 */
	public NotImplementedException() { super(); }
	
	/***
	 * Creates a NotImplementedException with a message.
	 * @param message the message, accessible via {@link #getMessage()}.
	 */
	public NotImplementedException( String message ) { super( message ); }
}
