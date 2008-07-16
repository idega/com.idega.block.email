package com.idega.block.email.client.business;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SearchTerm;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.util.CoreConstants;

/**
 * This JavaBean is used to store mail user information.
 */
@Scope("singleton")
@Service(EmailSubjectPatternFinder.BEAN_IDENTIFIER)
public class EmailSubjectPatternFinder {

	public static final String BEAN_IDENTIFIER = "email_EmailSubjectPatternFinder";
	private static final String DEFAULT_PROTOCOL = "pop3";
	private static final String DEFAULT_FOLDER = "Inbox";
	private static final String MSGS_FOLDER = "ReadMessages";
	private static final String IDNETIFIER_PATTERN = "[A-Z]{1,3}-\\d{4}-\\d{2}-\\d{2}-[A-Z0-9]{4,}";
	private static final Pattern subjectPattern = Pattern.compile(IDNETIFIER_PATTERN);

	public EmailSubjectPatternFinder() {
	}

	/**
	 * Returns the javax.mail.Folder object.
	 */
	protected Pattern getSubjectPattern() {
		return subjectPattern;
	}

	/**
	 * Returns all messages with right pattern. Be carefull - changes messages
	 * status in mail server
	 */
	private Message[] getMessages(EmailParams params) throws MessagingException {

		Message[] messages = params.getFolder().search(new SearchTerm() {

			@Override
			public boolean match(Message message) {

				try {

					String subject = message.getSubject();

					if (subject != null) {
						
						Matcher subjectMatcher = getSubjectPattern().matcher(subject);

						if (subjectMatcher.find()) {
							return true;
						}
					}

					else
						return false;

				} catch (MessagingException e) {
					e.printStackTrace();
				}
				return false;
			}

		});
		
		params.setMessagesFound(messages);
		
		return messages;
	}

	/**
	 * move all messages from folder to new or existing folder
	 * 
	 */
	public void moveMessages(EmailParams params) throws MessagingException {

		Folder msgFolder = params.getStore().getFolder(MSGS_FOLDER);

		if (!msgFolder.exists())
			msgFolder.create(Folder.HOLDS_MESSAGES);

		Message[] msgs = params.getMessagesFound();

		params.getFolder().copyMessages(msgs, msgFolder);

		for (int i = 0; i < msgs.length; i++)
			msgs[i].setFlag(Flags.Flag.DELETED, true);
	}

	/**
	 * Returns message map
	 * 
	 */
	public Map<String, Message> getMessageMap(EmailParams params) throws MessagingException {

		Message[] messages = getMessages(params);
		Map<String, Message> messageMap = new HashMap<String, Message>();

		for (int i = 0, n = messages.length; i < n; i++) {

			Matcher subjectMatcher = getSubjectPattern().matcher(
					messages[i].getSubject());
			subjectMatcher.find();

			String indentifier = messages[i].getSubject().substring(
					subjectMatcher.start(), subjectMatcher.end());

			messageMap.put(indentifier, messages[i]);

		}

		return messageMap;
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
		params.getFolder().close(true);
		params.getStore().close();
	}
}
