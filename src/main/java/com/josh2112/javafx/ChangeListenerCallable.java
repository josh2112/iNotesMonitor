package com.josh2112.javafx;

import javafx.beans.value.ObservableValue;

@FunctionalInterface
public interface ChangeListenerCallable<T> {
	
	void apply( ObservableValue<? extends T> observable, T oldValue, T newValue );
}
