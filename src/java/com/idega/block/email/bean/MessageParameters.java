package com.idega.block.email.bean;

import java.io.File;
import java.util.List;

import org.directwebremoting.annotations.DataTransferObject;
import org.directwebremoting.annotations.RemoteProperty;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.builder.bean.AdvancedProperty;
import com.idega.dwr.business.DWRAnnotationPersistance;

/**
 * Message (email) fields bean
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.2 $
 *
 * Last modified: $Date: 2009/04/22 12:55:16 $ by: $Author: valdas $
 */

@DataTransferObject
@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MessageParameters implements DWRAnnotationPersistance {

	@RemoteProperty
	private String senderName;
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
	@RemoteProperty
	private List<AdvancedProperty> properties;
	
	private File attachment;

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
			.append(", message: ").append(getMessage()).append(", attachments: ").append(getAttachments()).append(", properties: ").append(getProperties())
		.toString();
	}

	public File getAttachment() {
		return attachment;
	}

	public void setAttachment(File attachment) {
		this.attachment = attachment;
	}

	public List<AdvancedProperty> getProperties() {
		return properties;
	}

	public void setProperties(List<AdvancedProperty> properties) {
		this.properties = properties;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	
}
