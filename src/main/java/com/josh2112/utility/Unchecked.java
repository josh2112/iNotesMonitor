package com.josh2112.utility;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class Unchecked {
	public static <T> Supplier<T> wrap( Callable<T> callable ) {
	     return () -> {
	         try { return callable.call(); }
	         catch( Exception e ) { throw new RuntimeException(e); }
	     };
	}
}
