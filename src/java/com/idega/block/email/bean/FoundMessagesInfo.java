package com.idega.block.email.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import javax.mail.Message;

public class FoundMessagesInfo implements Serializable {

	private static final long serialVersionUID = 5549581211079663255L;

	private Collection<Message> messages;
	
	private MessageParserType parserType;

	public FoundMessagesInfo(MessageParserType parserType) {
		this.parserType = parserType;
	}
	
	public FoundMessagesInfo(Collection<Message> messages, MessageParserType parserType) {
		this(parserType);
		
		this.messages = messages == null ? null : new ArrayList<Message>(messages);
	}
	
	public Collection<Message> getMessages() {
		return messages;
	}

	public MessageParserType getParserType() {
		return parserType;
	}
	
	public void addMessage(Message message) {
		if (messages == null) {
			messages = new ArrayList<Message>();
		}
		messages.add(message);
	}

	@Override
	public String toString() {
		return "Type: " + parserType + ", messages: " + getMessages();
	}	
	
}
