package com.josh2112.utility;

public class StringUtils {

	public static boolean isNullOrEmpty( String str ) {
		return str == null || str.isEmpty();
	}
	
	public static boolean isNullOrWhitespace( String str ) {
		return str == null || str.trim().isEmpty();
	}
}
