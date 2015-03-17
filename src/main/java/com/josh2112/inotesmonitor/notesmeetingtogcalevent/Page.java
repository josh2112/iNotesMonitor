package com.josh2112.inotesmonitor.notesmeetingtogcalevent;

import java.lang.reflect.InvocationTargetException;

import com.josh2112.javafx.wizard.WizardContainer;
import com.josh2112.javafx.wizard.WizardPage;

public enum Page {
	MeetingDetails( MeetingDetailsPage.class.getName() ),
	CalendarSelectionPage( CalendarSelectionPage.class.getName() ),
	ConflictCheckPage( ConflictCheckPage.class.getName() ),
	SummaryPage( SummaryPage.class.getName() );
	
	private Class<? extends WizardPage> clazz;
	private WizardPage instance;
	
	Page( String className ) {
		try {
			this.clazz = Class.forName( className ).asSubclass( WizardPage.class );
		}
		catch( ClassNotFoundException e ) {
			e.printStackTrace();
		}
	}
	
	WizardPage getInstance( WizardContainer container ) {
		if( instance == null ) {
			try {
				instance = clazz.getConstructor( WizardContainer.class ).newInstance( container );
			}
			catch( InstantiationException | IllegalAccessException |
					IllegalArgumentException | InvocationTargetException |
					NoSuchMethodException | SecurityException e ) {
				e.printStackTrace();
			}
		}
		return instance;
	}
}