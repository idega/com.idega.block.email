package com.idega.block.email.mailing.list.business;

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
import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.block.email.parser.EmailParser;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.core.contact.data.Email;
import com.idega.core.messaging.EmailMessage;
import com.idega.core.messaging.MessagingSettings;
import com.idega.idegaweb.IWMainApplication;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
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
		
		for (String uniqueId: messages.keySet()) {
			FoundMessagesInfo info = messages.get(uniqueId);
			if (info.getParserType() != MessageParserType.MAILING_LIST) {
				continue;
			}
			
			MailingList mailingList = mailingListManager.getMailingListByUniqueId(uniqueId);
			if (mailingList == null) {
				LOGGER.warning("Unable to send message to mailing list: " + uniqueId + ", " + messages.get(uniqueId));
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
		String subject = mailingList.getName();
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
			parsedMessage.setMailServer(mailServer);
			parsedMessage.setSubject(subject);
			parsedMessage.setMailType(CoreConstants.MAIL_TEXT_HTML_TYPE);
			
			for (Email email: emails) {
				try {
					parsedMessage.setToAddress(email.getEmailAddress());
					parsedMessage.send();
					LOGGER.info("Sent: " + parsedMessage);
					//	TODO: add to sent messages for mailing list
				} catch(Exception e) {
					LOGGER.log(Level.WARNING, "Error sending message " + parsedMessage, e);
				}
			}
		}
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