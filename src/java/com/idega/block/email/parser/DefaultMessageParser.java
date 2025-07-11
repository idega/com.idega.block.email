package com.idega.block.email.parser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.business.EmailSenderHelper;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.client.business.EmailSubjectPatternFinder;
import com.idega.core.messaging.EmailMessage;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

public abstract class DefaultMessageParser implements EmailParser {

	private static final Logger LOGGER = Logger.getLogger(DefaultMessageParser.class.getName());

	@Autowired
	private EmailSubjectPatternFinder emailsFinder;

	@Autowired
	private EmailSenderHelper emailSenderHelper;

	@Override
	public Map<String, Collection<? extends EmailMessage>> getParsedMessages(Map<String, FoundMessagesInfo> messages, EmailParams params) {
		if (messages == null || messages.isEmpty()) {
			return null;
		}

		Map<String, Collection<? extends EmailMessage>> parsedMessages = new HashMap<String, Collection<? extends EmailMessage>>();

		for (String key: messages.keySet()) {
			Collection<Message> messagesByKey = messages.get(key).getMessages();
			parsedMessages.put(key, getParsedMessages(messagesByKey, params));
		}

		return parsedMessages;
	}

	@Override
	public Collection<? extends EmailMessage> getParsedMessagesCollection(Map<String, FoundMessagesInfo> messages, EmailParams params) {
		Map<String, Collection<? extends EmailMessage>> parsedMessages = getParsedMessages(messages, params);
		if (parsedMessages == null || parsedMessages.isEmpty()) {
			return null;
		}

		Collection<EmailMessage> allParsedMessages = new ArrayList<EmailMessage>();
		for (Collection<? extends EmailMessage> parsedMessagesByCategory: parsedMessages.values()) {
			allParsedMessages.addAll(parsedMessagesByCategory);
		}
		return allParsedMessages;
	}

	private Collection<EmailMessage> getParsedMessages(Collection<Message> messages, EmailParams params) {
		Collection<EmailMessage> emailMessages = new ArrayList<EmailMessage>();
		if (ListUtil.isEmpty(messages)) {
			return emailMessages;
		}

		for (Message message: messages) {
			EmailMessage parsedMessage = null;
			try {
				parsedMessage = getParsedMessage(message, params);
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error parsing message: " + message, e);
			}
			if (parsedMessage != null) {
				emailMessages.add(parsedMessage);
			}
		}

		return emailMessages;
	}

	protected EmailMessage getNewMessage() {
		return new EmailMessage();
	}

	protected boolean isValidEmail(Message message) throws MessagingException {
		if (message == null)
			return false;

		String subject = null;
		try {
			subject = message.getSubject();
		} catch (MessagingException e) {}
		Date sentDate = null;
		try {
			sentDate = message.getSentDate();
		} catch (MessagingException e) {}
		String contentType = null;
		try {
			contentType = message.getContentType();
		} catch (MessagingException e) {}

		//	Checking if mail is auto generated
		if (doExistHeaderFlag(message, SendMail.HEADER_AUTO_SUBMITTED, "auto-generated") &&
				!doExistHeaderFlag(message, SendMail.HEADER_PRECEDENCE, "bulk")) {
			return false;
		}

		//	Checking if mail is auto reply
		if (!StringUtil.isEmpty(subject)) {
			if (subject.toLowerCase().indexOf("[autoreply]") != -1) {
				return false;
			}
		}

		//	Checking if mail is report type
		try {
			if (message.isMimeType(EmailConstants.MESSAGE_MULTIPART_REPORT)) {
				return false;
			}
		} catch (MessagingException e) {
			LOGGER.warning("Error resolving mime type for message with subject: " + subject + ", sent: " + sentDate + ", content type: " + contentType);
		}

		if (subject == null) {
			//	Will check if content is provided
			Object content = null;
			try {
				content = message.getContent();
			} catch (Exception e) {
				LOGGER.warning("Error resolving content for message with subject: " + subject + ", sent: " + sentDate + ", content type: " +
						contentType + ". Marking this message as invalid and skipping");
			}
			if (content == null)
				return false;
		}

		return true;
	}

	private boolean doExistHeaderFlag(Message message, String headerFlag, String headerFlagValue) {
		String[] flags = null;
		try {
			flags = message.getHeader(headerFlag);
		} catch (MessageRemovedException e) {
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting header flag: " + headerFlag, e);
		}
		if (ArrayUtil.isEmpty(flags))
			return false;

		for (String flag: flags) {
			if (headerFlagValue.equals(flag))
				return true;
		}

		return false;
	}

	@Override
	public synchronized EmailMessage getParsedMessage(Message message, EmailParams params) throws Exception {
		EmailMessage parsedMessage = null;
		if (!isValidEmail(message)) {
			return null;
		}

		parsedMessage = getNewMessage();
		try {
			parsedMessage.setSubject(message.getSubject());

			Object[] msgAndAttachments = getEmailSenderHelper().getParsedContent(message, false);
			if (ArrayUtil.isEmpty(msgAndAttachments)) {
				parsedMessage = null;
				return parsedMessage;
			}

			Object body = msgAndAttachments[0];
			if (body == null) {
				body = CoreConstants.EMPTY;
			}
			parsedMessage.setBody(body instanceof String ? (String) body : body.toString());

			String fromAddress = getFromAddress(message);

			Address[] froms = message.getFrom();
			String senderName = null;
			if (!ArrayUtil.isEmpty(froms)) {
				List<Address> tmp = Arrays.asList(froms);
				for (Iterator<Address> addressIter = tmp.iterator(); (StringUtil.isEmpty(senderName) && addressIter.hasNext());) {
					Address address = addressIter.next();
					if (address instanceof InternetAddress) {
						InternetAddress iaddr = (InternetAddress) address;
						senderName = iaddr.getPersonal();
					}
				}
			}
			parsedMessage.setSenderName(senderName);
			parsedMessage.setFromAddress(fromAddress);

			@SuppressWarnings("unchecked")
			Map<String, InputStream> files = (Map<String, InputStream>) msgAndAttachments[1];
			parsedMessage.setAttachments(files);

			return parsedMessage;
		} finally {
			if (parsedMessage != null) {
				getEmailsFinder().moveMessage(message, params);
			}
		}
	}


	public EmailSubjectPatternFinder getEmailsFinder() {
		if (emailsFinder == null) {
			ELUtil.getInstance().autowire(this);
		}
		return emailsFinder;
	}

	public void setEmailsFinder(EmailSubjectPatternFinder emailsFinder) {
		this.emailsFinder = emailsFinder;
	}

	@Override
	public Collection<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent) {
		LOGGER.warning("This method is not implemented!");
		return null;
	}

	@Override
	public String getFromAddress(Message message) throws MessagingException {
		Address[] froms = message.getFrom();
		for (Address address : froms) {
			if (address instanceof InternetAddress) {
				InternetAddress iaddr = (InternetAddress) address;
				return iaddr.getAddress();
			}
		}

		return null;
	}


	public EmailSenderHelper getEmailSenderHelper() {
		if (emailSenderHelper == null) {
			ELUtil.getInstance().autowire(this);
		}
		return emailSenderHelper;
	}

	public void setEmailSenderHelper(EmailSenderHelper emailSenderHelper) {
		this.emailSenderHelper = emailSenderHelper;
	}

}