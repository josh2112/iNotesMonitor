package com.josh2112.utils.jooq;

import org.jooq.impl.EnumConverter;

import com.josh2112.inotesmonitor.inotesdata.NotesMessage.MessageType;

public class MessageTypeConverter extends EnumConverter<Integer, MessageType> {
	public MessageTypeConverter() {
		super( Integer.class, MessageType.class );
	}
}
