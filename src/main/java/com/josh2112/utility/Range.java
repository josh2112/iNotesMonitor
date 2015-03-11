package com.josh2112.utility;

/***
 * A generic class representing a range, or start and end point.
 * 
 * @author Joshua Foster
 *
 * @param <T> the type of thing we are storing a range for.
 */
public class Range<T> {
	private T start, end;
	
	/***
	 * Creates a new range with start and end values.
	 * @param start value
	 * @param end value
	 */
	public Range( T start, T end ) { this.start = start; this.end = end; }
	
	/***
	 * Returns the start value.
	 * @return start value
	 */
	public T getStart() { return start; }
	
	/***
	 * Returns the end value.
	 * @return end value
	 */
	public T getEnd() { return end; }
}