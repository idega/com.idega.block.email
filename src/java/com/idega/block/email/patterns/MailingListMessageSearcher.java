package com.idega.block.email.patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.mailing.list.business.MailingListManager;
import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.util.CoreConstants;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class MailingListMessageSearcher extends DefaultSubjectPatternFinder {

	private static final long serialVersionUID = -339350300052921900L;

	private static final String MAILING_LIST_REGULAR_EXPRESSION = "\\[.*" + EmailConstants.IW_MAILING_LIST + "\\]";
	private static final Pattern MAILING_LIST_REGULAR_EXPRESSION_PATTERN = Pattern.compile(MAILING_LIST_REGULAR_EXPRESSION);

	@Autowired
	private MailingListManager mailingListManager;

	public MailingListMessageSearcher() {
		super();

		addPattern(MAILING_LIST_REGULAR_EXPRESSION_PATTERN);
	}

	@Override
	public Map<String, FoundMessagesInfo> getSearchResultsFormatted(EmailParams params) throws MessagingException {
		Message[] messages = getMessages(params);

		Map<String, FoundMessagesInfo> mailingListsMessages = new HashMap<String, FoundMessagesInfo>();

		for (Message message: messages) {
			String subject = message.getSubject();
			if (StringUtil.isEmpty(subject)) {
				continue;
			}

			Matcher subjectMatcher = getPatterns().get(0).matcher(subject);
			subjectMatcher.find();
			String mailingListIdentifier = subject.substring(subjectMatcher.start(), subjectMatcher.end());
			if (StringUtil.isEmpty(mailingListIdentifier)) {
				continue;
			}

			String mailingListNameInLatinLetters = mailingListIdentifier.substring(1);
			mailingListNameInLatinLetters = mailingListNameInLatinLetters.replace(EmailConstants.IW_MAILING_LIST, CoreConstants.EMPTY);
			mailingListNameInLatinLetters = mailingListNameInLatinLetters.substring(0, mailingListNameInLatinLetters.length() - 1);
			MailingList mailingList = mailingListManager.getMailingListByNameInLatinLetters(mailingListNameInLatinLetters);
			if (mailingList == null) {
				continue;
			}

			FoundMessagesInfo messagesForMailingList = mailingListsMessages.get(mailingListNameInLatinLetters);
			if (messagesForMailingList == null) {
				messagesForMailingList = new FoundMessagesInfo(mailingListIdentifier, getParserType());
				messagesForMailingList.addMessage(message);
				mailingListsMessages.put(mailingListNameInLatinLetters, messagesForMailingList);
			} else {
				messagesForMailingList.addMessage(message);
			}
		}

		return mailingListsMessages;
	}

	@Override
	public MessageParserType getParserType() {
		return MessageParserType.MAILING_LIST;
	}

}