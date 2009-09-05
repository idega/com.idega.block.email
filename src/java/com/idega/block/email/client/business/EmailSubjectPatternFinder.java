package com.idega.block.email.client.business;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.patterns.EmailSubjectSearchable;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;

/**
 * This JavaBean is used to store mail user information.
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(EmailSubjectPatternFinder.BEAN_IDENTIFIER)
public class EmailSubjectPatternFinder {
	
	public static final String BEAN_IDENTIFIER = "email_EmailSubjectPatternFinder";
	private static final String DEFAULT_PROTOCOL = "pop3";
	private static final String DEFAULT_FOLDER = "Inbox";
	private static final String MSGS_FOLDER = "ReadMessages";
	
	private Collection<EmailSubjectSearchable> emailSubjectSearchers;
	
	public EmailSubjectPatternFinder() {}
	
	/**
	 * move all messages from folder to new or existing folder
	 */
	public void moveMessages(EmailParams params) throws MessagingException {
		Folder msgFolder = params.getStore().getFolder(MSGS_FOLDER);
		Folder inbox = params.getFolder();
		Message[] msgs = params.getMessagesFound();
		
		for (Message msg: msgs) {
			moveMessage(msg, inbox, msgFolder, params);
		}
	}
	
	public void moveMessage(Message message, EmailParams params) throws MessagingException {
		moveMessage(message, params.getFolder(), params.getStore().getFolder(MSGS_FOLDER), params);
	}
	
	private synchronized void moveMessage(Message message, Folder inbox, Folder msgFolder, EmailParams params) throws MessagingException {
		Collection<Message> foundMessages = new ArrayList<Message>(Arrays.asList(params.getMessagesFound()));
		foundMessages.remove(message);
		params.setMessagesFound(ArrayUtil.convertListToArray(foundMessages));
		
		if (!msgFolder.exists())
			msgFolder.create(Folder.HOLDS_MESSAGES);
		
		inbox.copyMessages(new Message[] {message}, msgFolder);
		message.setFlag(Flags.Flag.DELETED, true);
		
		if (ArrayUtil.isEmpty(params.getMessagesFound())) {
			logout(params);
		}
	}
	
	/**
	 * Returns message map
	 */
	public Map<String, FoundMessagesInfo> getMessageMap(EmailParams params) throws MessagingException {
		
		Collection<Message> foundMessages = new ArrayList<Message>();
		Map<String, FoundMessagesInfo> allMessages = new HashMap<String, FoundMessagesInfo>();
		
		for (EmailSubjectSearchable emailSearcher: getEmailSubjectSearchers()) {
			Map<String, FoundMessagesInfo> messages = emailSearcher.getSearchResultsFormatted(params);
			if (messages == null || messages.isEmpty()) {
				continue;
			}
			
			for (String identifier: messages.keySet()) {
				FoundMessagesInfo messagesByIdentifier = messages.get(identifier);
				if (messagesByIdentifier == null || ListUtil.isEmpty(messagesByIdentifier.getMessages())) {
					continue;
				}
				
				FoundMessagesInfo formattedMessages = allMessages.get(identifier);
				if (formattedMessages == null) {
					formattedMessages = new FoundMessagesInfo(messagesByIdentifier.getMessages(), messagesByIdentifier.getParserType());
					formattedMessages.setIdentifier(messagesByIdentifier.getIdentifier());
					allMessages.put(identifier, formattedMessages);
				} else {
					for (Message messageByIdentifier: messagesByIdentifier.getMessages()) {
						if (!formattedMessages.getMessages().contains(messageByIdentifier)) {
							formattedMessages.addMessage(messageByIdentifier);
						}
					}
				}
			}
		}

		for (FoundMessagesInfo messageInfo: allMessages.values()) {
			foundMessages.addAll(messageInfo.getMessages());
		}
		params.setMessagesFound(ListUtil.isEmpty(foundMessages) ? new Message[] {} : ArrayUtil.convertListToArray(foundMessages));
			
		return allMessages;
	}
	
	/**
	 * Method used to login to the mail inbox.
	 */
	public void login(EmailParams params) throws Exception {
		
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);
		params.setSession(session);
		final Store store;
		
		if (CoreConstants.EMPTY.equals(params.getProtocol())) {
			store = session.getStore(DEFAULT_PROTOCOL);
		} else {
			store = session.getStore(params.getProtocol());
		}
		
		params.setStore(store);
		
		store.connect(params.getHostname(), params.getUsername(), params
		        .getPassword());
		
		Folder folder = store.getFolder(DEFAULT_FOLDER);
		params.setFolder(folder);
		folder.open(Folder.READ_WRITE);
	}
	
	/**
	 * Method used to login to the mail inbox.
	 */
	public EmailParams login(String hostname, String username, String password,
	        String protocol) throws Exception {
		
		EmailParams params = new EmailParams();
		params.setProtocol(protocol);
		params.setHostname(hostname);
		params.setUsername(username);
		params.setPassword(password);
		
		login(params);
		
		return params;
	}
	
	/**
	 * Method used to logout from the mail host.
	 */
	public void logout(EmailParams params) throws MessagingException {
		if (params.isLoggedOut()) {
			return;
		}
		
		params.getFolder().close(true);
		params.getStore().close();
		params.setLoggedOut(true);
	}

	@SuppressWarnings("unchecked")
	public Collection<EmailSubjectSearchable> getEmailSubjectSearchers() {
		if (ListUtil.isEmpty(emailSubjectSearchers)) {
			Map<String, ? extends EmailSubjectSearchable> beans = WebApplicationContextUtils
				.getWebApplicationContext(IWMainApplication.getDefaultIWMainApplication().getServletContext()).getBeansOfType(EmailSubjectSearchable.class);
			if (beans == null || beans.isEmpty()) {
				return null;
			}
			
			emailSubjectSearchers = new ArrayList<EmailSubjectSearchable>(beans.size());
			for (EmailSubjectSearchable searcher: beans.values()) {
				addEmailSubjectSearcher(searcher);
			}
		}
		
		return emailSubjectSearchers;
	}

	public void addEmailSubjectSearcher(EmailSubjectSearchable emailSubjectSearcher) {
		if (emailSubjectSearchers == null) {
			emailSubjectSearchers = new ArrayList<EmailSubjectSearchable>();
		}
		
		if (emailSubjectSearchers.contains(emailSubjectSearcher)) {
			return;
		}
		emailSubjectSearchers.add(emailSubjectSearcher);
	}
	
}