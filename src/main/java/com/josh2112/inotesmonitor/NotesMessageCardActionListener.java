package com.josh2112.inotesmonitor;

import com.josh2112.inotesmonitor.inotesdata.NotesMessage;

public interface NotesMessageCardActionListener {
	public void openInBrowser( NotesMessage msg );
	public void remove( NotesMessage item );
	public void addToGoogleCalendar( NotesMessage item );
	public void acceptMeeting( NotesMessage meeting );
}
