package com.josh2112.inotesmonitor;

import java.io.File;

import net.snarl.Notification;
import net.snarl.SnarlAction;
import net.snarl.SnarlActionListener;
import net.snarl.SnarlNetworkBridge;

import com.josh2112.javafx.PlatformHelper;


public class SnarlManager {

	private Notification note;
	private Runnable notificationAction;
	private boolean isShown;
	
	public SnarlManager( String appVendor, String appName, String appPrettyName ) {
		SnarlNetworkBridge.setDebug( true );
		SnarlNetworkBridge.snRegisterConfig( String.format( "application/x-vnd-%s.%s",
				appVendor, appName ), appPrettyName );
		
		note = new Notification( null, "title", "text", false, Notification.NO_TIMEOUT );
		
		note.setActionListener( new SnarlActionListener() {
			@Override public void notificationTimedOut() {}
			@Override public void defaultActionPerformed() {}
			@Override public void actionPerformed( SnarlAction action ) {}
			
			@Override public void notificationClosed() {
				PlatformHelper.run( notificationAction );
			}
		} );
	}

	public void setIcon( File iconFile ) {
		note.setIcon( iconFile );
	}
	
	public void updateNotification( String title, String text ) {
		note.setTitle( title );
		note.setText( text );
		SnarlNetworkBridge.snShowMessage( note );
		isShown = true;
	}

	public void hideNotification() {
		if( isShown ) SnarlNetworkBridge.snHideMessage( note );
		isShown = false;
	}

	public void unregister() {
		SnarlNetworkBridge.snRevokeConfig();
	}

	public void setNotificationAction( Runnable runnable ) {
		notificationAction = runnable;
	}
}
