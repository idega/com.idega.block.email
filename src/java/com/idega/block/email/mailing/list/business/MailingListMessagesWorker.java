package com.idega.block.email.mailing.list.business;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.data.MessageHome;
import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.block.email.parser.EmailParser;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.contact.data.Email;
import com.idega.core.file.data.ICFile;
import com.idega.core.file.data.ICFileHome;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.core.messaging.EmailMessage;
import com.idega.core.messaging.MessagingSettings;
import com.idega.data.IDOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

@Service
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class MailingListMessagesWorker implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(MailingListMessagesWorker.class.getName());
	
	@Autowired
	private MailingListManager mailingListManager;
	
	@Autowired()
	@Qualifier("defaultEmailsParser")
	private EmailParser emailParser;
	
	private Map<String, FoundMessagesInfo> messages;
	private EmailParams params;
	
	public MailingListMessagesWorker(Map<String, FoundMessagesInfo> messages, EmailParams params) {
		ELUtil.getInstance().autowire(this);
		
		this.messages = messages;
		this.params = params;
	}
	
	public void run() {
		sendAllMessages();
	}
	
	private void sendAllMessages() {
		if (messages == null || messages.isEmpty()) {
			return;
		}
		
		for (String nameInLatinLetters: messages.keySet()) {
			FoundMessagesInfo info = messages.get(nameInLatinLetters);
			if (info.getParserType() != MessageParserType.MAILING_LIST) {
				continue;
			}
			
			MailingList mailingList = mailingListManager.getMailingListByNameInLatinLetters(nameInLatinLetters);
			if (mailingList == null) {
				LOGGER.warning("Unable to send message to mailing list: " + nameInLatinLetters + ", " + messages.get(nameInLatinLetters));
				continue;
			}
			
			sendMessagesToMailingList(mailingList, info);
		}
	}
	
	private void sendMessagesToMailingList(MailingList mailingList, FoundMessagesInfo messagesInfo) {
		if (messagesInfo == null || messagesInfo.getParserType() != MessageParserType.MAILING_LIST || ListUtil.isEmpty(messagesInfo.getMessages())) {
			LOGGER.warning("Messages is not for mailing list: " + messagesInfo);
			return;
		}
		
		String senderAddress = mailingList.getSenderAddress();
		if (StringUtil.isEmpty(senderAddress)) {
			LOGGER.warning("Mailing list '"+mailingList.getName()+"' doesn't have sender e-mail's address!");
			return;
		}
		
		String mailServer = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(MessagingSettings.PROP_SYSTEM_SMTP_MAILSERVER);
		if (StringUtil.isEmpty(mailServer)) {
			LOGGER.warning("There is no mail server defined to send emails thru");
			return;
		}
		
		Collection<User> subscribers = mailingList.getSubscribers();
		if (ListUtil.isEmpty(subscribers)) {
			LOGGER.warning("Mailing list " + mailingList.getName() + " doesn't have subscribers! Messages were not sent: " + messagesInfo.getMessages());
			return;
		}
		
		Collection<Email> emails = getEmailAddresses(subscribers);
		if (ListUtil.isEmpty(emails)) {
			LOGGER.warning("No emails were found for: " + subscribers);
			return;
		}

		IWMainApplication.getDefaultIWMainApplication().getMessagingSettings().setEmailingEnabled(Boolean.TRUE);
		
		String senderName = mailingList.getSenderName();
		if (StringUtil.isEmpty(senderName)) {
			senderName = senderAddress;
		}
		for (Message message: messagesInfo.getMessages()) {
			EmailMessage parsedMessage = null;
			try {
				parsedMessage = emailParser.getParsedMessage(message, params);
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error parsing message: " + message, e);
			}
			if (parsedMessage == null) {
				continue;
			}
			
			parsedMessage.setFromAddress(senderAddress);
			parsedMessage.setSenderName(senderName);
			parsedMessage.setSubject(StringUtil.isEmpty(messagesInfo.getIdentifier()) ?
					parsedMessage.getSubject() : StringHandler.replace(parsedMessage.getSubject(), messagesInfo.getIdentifier(), CoreConstants.EMPTY).trim());
			parsedMessage.setMailServer(mailServer);
			parsedMessage.setMailType(CoreConstants.MAIL_TEXT_HTML_TYPE);
			
			for (Email email: emails) {
				try {
					parsedMessage.setToAddress(email.getEmailAddress());
					
					addMessage(mailingList, parsedMessage);
					
					parsedMessage.send();
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Error sending message " + parsedMessage, e);
				}
			}
		}
	}
	
	private void addMessage(MailingList mailingList, EmailMessage emailMessage) throws Exception {
		MessageHome messageHome = (MessageHome) IDOLookup.getHome(com.idega.block.email.data.Message.class);
		com.idega.block.email.data.Message message = messageHome.create();
		message.setSubject(emailMessage.getSubject());
		message.setSenderAddress(emailMessage.getFromAddress());
		message.setMessageContent(StringHandler.getStreamFromString(emailMessage.getBody()));
		message.setReceived(new Timestamp(System.currentTimeMillis()));
		message.store();
		
		Collection<File> attachments = emailMessage.getAttachedFiles();
		if (!ListUtil.isEmpty(attachments)) {
			ICFileHome fileHome = (ICFileHome) IDOLookup.getHome(ICFile.class);
			for (File attachment: attachments) {
				ICFile attachmentInDB = fileHome.create();
				attachmentInDB.setName(attachment.getName());
				attachmentInDB.setFileValue(new FileInputStream(attachment));
				MimeTypeUtil.resolveMimeTypeFromFileName(attachment.getName());
				attachmentInDB.setMimeType(MimeTypeUtil.resolveMimeTypeFromFileName(attachment.getName()));
				attachmentInDB.store();
				message.addAttachment(attachmentInDB);
			}
			message.store();
		}
		
		mailingList.addMessage(message);
		mailingList.store();
	}
	
	private Collection<Email> getEmailAddresses(Collection<User> users) {
		UserBusiness userBusiness = null;
		try {
			userBusiness = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.WARNING, "Error getting " + UserBusiness.class, e);
		}
		if (userBusiness == null) {
			return null;
		}
		
		try {
			return userBusiness.getEmailHome().findMainEmailsForUsers(users);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting emails for: " + users, e);
		}
		
		return null;
	}

}