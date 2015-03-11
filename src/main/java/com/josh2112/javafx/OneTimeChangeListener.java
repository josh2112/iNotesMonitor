package com.josh2112.javafx;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

/***
 * An implementation of ChangeListener<T> that unregisters itself
 * from the observable after firing once.
 * 
 * @author jf334
 *
 * @param <T>
 */
public class OneTimeChangeListener<T> implements ChangeListener<T> {

	private ChangeListenerCallable<T> changedCallback;
	
	public OneTimeChangeListener( ChangeListenerCallable<T> changedCallback ) {
		this.changedCallback = changedCallback;
	}
	
	@Override
	public void changed( ObservableValue<? extends T> observable, T oldValue, T newValue ) {
		observable.removeListener( this );
		changedCallback.apply( observable, oldValue, newValue );
	}
}
