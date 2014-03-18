package com.idega.block.email.patterns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.SearchTerm;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.client.business.EmailParams;
import com.idega.core.business.DefaultSpringBean;
import com.idega.util.ArrayUtil;
import com.idega.util.ListUtil;

public abstract class DefaultSubjectPatternFinder extends DefaultSpringBean implements EmailSubjectSearchable {

	private static final long serialVersionUID = -6607788445744465098L;

	private List<Pattern> patterns = new ArrayList<Pattern>();

	/**
	 * Returns all messages with right pattern. Be careful - changes messages status in mail server
	 */
	@Override
	public Message[] getMessages(EmailParams params) throws MessagingException {
		if (ListUtil.isEmpty(patterns)) {
			Logger.getLogger(DefaultSubjectPatternFinder.class.getName()).warning("Patterns are not defined!");
			return new Message[] {};
		}

		Message[] messages = params.getFolder().search(
			new SearchTerm() {
				private static final long serialVersionUID = 5298994639594655420L;

				@Override
				public boolean match(Message message) {
					try {
						String subject = message.getSubject();
						if (subject == null) {
							return false;
						}

						for (Pattern pattern: patterns) {
							Matcher matcher = pattern.matcher(subject);
							if (matcher.find()) {
								return true;
							}
						}
					} catch (MessagingException e) {
						e.printStackTrace();
					}
					return false;
				}
		});

		return messages;
	}

	protected Map<String, FoundMessagesInfo> getCaseIdentifierSearchResultsFormatted(EmailParams params) throws MessagingException {
		Map<String, FoundMessagesInfo> messagesMap = new HashMap<String, FoundMessagesInfo>();

		Message[] messages = getMessages(params);
		if (ArrayUtil.isEmpty(messages)) {
			return messagesMap;
		}

		for (Message message: messages) {
			Matcher matcher = null;
			for (Pattern pattern: patterns) {
				try {
					matcher = pattern.matcher(message.getSubject());
					if (!matcher.find()) {
						matcher = null;
					}
				} catch (Exception e) {
					matcher = null;
				}
			}

			if (matcher == null) {
				continue;
			}

			String indentifier = message.getSubject().substring(matcher.start(), matcher.end());
			if (messagesMap.get(indentifier) == null) {
				FoundMessagesInfo messagesInfo = new FoundMessagesInfo(getParserType());
				messagesInfo.addMessage(message);
				messagesMap.put(indentifier, messagesInfo);
			} else {
				messagesMap.get(indentifier).addMessage(message);
			}
		}

		return messagesMap;
	}

	public void addPattern(Pattern pattern) {
		if (!patterns.contains(pattern)) {
			patterns.add(pattern);
		}
	}

	public List<Pattern> getPatterns() {
		return patterns;
	}

	@Override
	public String getFixedIdentifier(String identifier) {
		return identifier;
	}

}
