package com.idega.block.email.event;

import org.springframework.context.ApplicationEvent;

import com.idega.user.data.User;

public class UserMessage extends ApplicationEvent {

	private static final long serialVersionUID = -5866237425882480147L;

	private User receiver, sender;

	private String subject, message;

	private boolean sendLetter;

	public UserMessage(Object context, User receiver, String subject, String message, User sender, boolean sendLetter) {
		super(context);

		this.receiver = receiver;
		this.subject = subject;
		this.message = message;
		this.sender = sender;
		this.sendLetter = sendLetter;
	}

	public User getReceiver() {
		return receiver;
	}

	public void setReceiver(User receiver) {
		this.receiver = receiver;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
		this.sender = sender;
	}

	public boolean isSendLetter() {
		return sendLetter;
	}

	public void setSendLetter(boolean sendLetter) {
		this.sendLetter = sendLetter;
	}

	@Override
	public String toString() {
		return "Message with subject '" + getSubject() + "' to " + getReceiver();
	}

}