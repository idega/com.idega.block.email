package com.idega.block.email.bean;

import java.util.List;

import org.directwebremoting.annotations.DataTransferObject;
import org.directwebremoting.annotations.RemoteProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.dwr.business.DWRAnnotationPersistance;

/**
 * Message (email) fields bean
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2009/04/17 11:22:05 $ by: $Author: valdas $
 */

@DataTransferObject
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MessageParameters implements DWRAnnotationPersistance {

	@RemoteProperty
	private String from;
	
	@RemoteProperty
	private String recipientTo;
	@RemoteProperty
	private String recipientCc;
	@RemoteProperty
	private String recipientBcc;
	
	@RemoteProperty
	private String subject;
	@RemoteProperty
	private String message;
	
	@RemoteProperty
	private List<String> attachments;

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getRecipientTo() {
		return recipientTo;
	}

	public void setRecipientTo(String recipientTo) {
		this.recipientTo = recipientTo;
	}

	public String getRecipientCc() {
		return recipientCc;
	}

	public void setRecipientCc(String recipientCc) {
		this.recipientCc = recipientCc;
	}

	public String getRecipientBcc() {
		return recipientBcc;
	}

	public void setRecipientBcc(String recipientBcc) {
		this.recipientBcc = recipientBcc;
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
	
	public List<String> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<String> attachments) {
		this.attachments = attachments;
	}

	@Override
	public String toString() {
		return new StringBuilder("From: ").append(getFrom()).append(", to: ").append(getRecipientTo()).append(", subject: ").append(getSubject())
			.append(", message: ").append(getMessage()).toString();
	}
}
