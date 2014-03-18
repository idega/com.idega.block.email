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
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.datastructures.map.MapUtil;

/**
 * This JavaBean is used to store mail user information.
 */
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Service(EmailSubjectPatternFinder.BEAN_IDENTIFIER)
public class EmailSubjectPatternFinder extends DefaultSpringBean {

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
			moveMessage(msg, inbox, msgFolder, params, true);
		}
	}

	public void moveMessage(Message message, EmailParams params) throws MessagingException {
		moveMessage(message, params.getFolder(), params.getStore().getFolder(MSGS_FOLDER), params, true);
	}

	public void moveMessage(Message message, EmailParams params, String destinationFolderName) throws MessagingException {
		Folder inbox = params.getFolder();
		Folder destinationFolder = params.getStore().getFolder(destinationFolderName);

		moveMessage(message, inbox, destinationFolder, params, false);
	}

	private synchronized void moveMessage(Message message, Folder sourceFolder, Folder destinationFolder, EmailParams params, boolean logout) throws MessagingException {
		Collection<Message> foundMessages = new ArrayList<Message>(Arrays.asList(params.getMessagesFound()));
		foundMessages.remove(message);
		params.setMessagesFound(ArrayUtil.convertListToArray(foundMessages));

		if (!destinationFolder.exists())
			destinationFolder.create(Folder.HOLDS_MESSAGES);

		sourceFolder.copyMessages(new Message[] {message}, destinationFolder);
		message.setFlag(Flags.Flag.DELETED, true);

		if (logout && ArrayUtil.isEmpty(params.getMessagesFound())) {
			logout(params);
		}
	}

	/**
	 * Returns message map
	 */
	public Map<String, FoundMessagesInfo> getMessageMap(EmailParams params) throws MessagingException {
		Collection<Message> foundMessages = new ArrayList<Message>();
		Map<String, FoundMessagesInfo> allMessages = new HashMap<String, FoundMessagesInfo>();

		Collection<EmailSubjectSearchable> emailsSearchers = getEmailSubjectSearchers();
		if (ListUtil.isEmpty(emailsSearchers)) {
			getLogger().warning("No emails searcher are loaded");
			return allMessages;
		}

		for (EmailSubjectSearchable emailSearcher: emailsSearchers) {
			Map<String, FoundMessagesInfo> messages = emailSearcher.getSearchResultsFormatted(params);
			if (MapUtil.isEmpty(messages)) {
				continue;
			}

			for (String identifier: messages.keySet()) {
				FoundMessagesInfo messagesByIdentifier = messages.get(identifier);
				if (messagesByIdentifier == null || ListUtil.isEmpty(messagesByIdentifier.getMessages())) {
					getLogger().warning("No messages found by identifer: " + identifier + ". Emails searcher: " + emailSearcher.getClass().getName() + ". All messages:\n" + messages.keySet());
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

		store.connect(params.getHostname(), params.getUsername(), params.getPassword());

		String folderName = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty("mail_inbox_folder", DEFAULT_FOLDER);
		Folder folder = store.getFolder(folderName);
		if (folder == null || !folder.exists()) {
			getLogger().warning("Folder with name '" + folderName + "' does not exist!");
		}
		params.setFolder(folder);
		folder.open(Folder.READ_WRITE);
	}

	/**
	 * Method used to login to the mail inbox.
	 */
	public EmailParams login(String hostname, String username, String password,  String protocol) throws Exception {

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