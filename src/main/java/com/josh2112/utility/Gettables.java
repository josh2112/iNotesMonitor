package com.josh2112.utility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

/***
 * Gettables is a helper class for getting all the accessible values of this object. Accessible
 * values are defined as those for which "getXXX()" or "isXXX()" methods exist (excluding
 * Object.getClass() which all objects inherit). Reflection is used to get a list of accessor
 * methods, and name of the accessor (method name minus the "get" or "is" part) is added to a
 * map along with the String conversion of the value obtained by calling the method. The values
 * can be returned either as a map or formatted into a string.
 * 
 * @author Joshua Foster
 *
 */
public class Gettables {
	
	/***
	 * Returns a map of all the accessible values of this object keyed by their accessor names.
	 * Reflection is used to get a list of accessor methods ("getXXX()" or "isXXX()") excluding
	 * Object.getClass(). The name of the accessor (method name minus the "get" or "is" part) is
	 * added to a map along with the String conversion of the value obtained by calling the
	 * method.
	 * @param obj the object to retrieve accessors from
	 * @return map of accessor values keyed by name
	 */
	public static HashMap<String, String> toMap( Object obj ) {
		HashMap<String, String> values = new HashMap<>();
		
		Arrays.asList( obj.getClass().getMethods() ).stream().filter( m ->
					(m.getName().startsWith( "get" ) && !m.getName().equals( "getClass" )) ||
					 m.getName().startsWith( "is" )
				).forEach( m -> {
					try {
						values.put( m.getName().replace( "get", "" ), m.invoke( obj ).toString() );
					}
					catch( Exception e ) {
						values.put( m.getName().replace( "get", "" ), "<?>" );
					}
				});
		
		return values;
	}
	
	/***
	 * Returns a String consisting of accessor name/value pairs available in this object, surrounded
	 * by brackets and prefixed by the object's class name.  See {@link #toMap(Object)}.
	 * @param obj the object to retrieve accessors from
	 * @return accessor names and values enclosed in class name.
	 */
	public static String toString( Object obj ) {
		return toString( obj, 0 );
	}
	
	/***
	 * Returns a String consisting of accessor name/value pairs available in this object, surrounded
	 * by brackets and prefixed by the object's class name.  See {@link #toMap(Object)}.
	 * @param obj the object to retrieve accessors from
	 * @param indentLevel the level of indentation (in tabs) to use
	 * @return accessor names and values enclosed in class name.
	 */
	public static String toString( Object obj, int indentLevel ) {
		String outerIndent = Strings.repeat( "\t", indentLevel );
		String innerIndent = Strings.repeat( "\t", indentLevel+1 );
		
		String meat = toMap( obj ).entrySet().stream().map(
				e -> innerIndent + e.getKey() + ": " + e.getValue() ).collect( Collectors.joining( "\n" ) );
		return outerIndent + obj.getClass().getSimpleName() + " {\n" + meat + "\n" + outerIndent + "}\n";
	}
}
