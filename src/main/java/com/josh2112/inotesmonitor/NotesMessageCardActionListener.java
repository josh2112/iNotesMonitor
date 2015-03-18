package com.josh2112.inotesmonitor;

import com.josh2112.inotesmonitor.inotesdata.NotesMessage;

public interface NotesMessageCardActionListener {
	public void openInBrowser( NotesMessage msg );
	public void markAsRead( NotesMessage item );
	public void addToGoogleCalendar( NotesMessage item );
	public void cancelAddToGoogleCalendar();
	public void acceptMeeting( NotesMessage meeting );
	public void delete( NotesMessage selectedItem );
}
