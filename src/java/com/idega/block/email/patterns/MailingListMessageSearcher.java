package com.idega.block.email.patterns;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.mailing.list.business.MailingListManager;
import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.util.ArrayUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class MailingListMessageSearcher extends DefaultSubjectPatternFinder {

	private static final long serialVersionUID = -339350300052921900L;
	
	public static final String MAILING_LIST_MESSAGE_SUBJECT_IDENTIFIER = "_ml_";
	private static final String MAILING_LIST_REGULAR_EXPRESSION = "." + MAILING_LIST_MESSAGE_SUBJECT_IDENTIFIER +
		"[a-z0-9]{8}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{4}-[a-z0-9]{12}";
	private static final Pattern MAILING_LIST_REGULAR_EXPRESSION_PATTERN = Pattern.compile(MAILING_LIST_REGULAR_EXPRESSION);
	
	@Autowired
	private MailingListManager mailingListManager;
	
	public MailingListMessageSearcher() {
		super();
		
		setSubjectPattern(MAILING_LIST_REGULAR_EXPRESSION_PATTERN);
	}
	
	public Map<String, FoundMessagesInfo> getSearchResultsFormatted(EmailParams params) throws MessagingException {
		Message[] messages = getMessages(params);
		
		Map<String, FoundMessagesInfo> mailingListsMessages = new HashMap<String, FoundMessagesInfo>();
		
		for (Message message: messages) {
			String[] parts = message.getSubject().split(MAILING_LIST_MESSAGE_SUBJECT_IDENTIFIER);
			if (ArrayUtil.isEmpty(parts)) {
				continue;
			}
			
			String uniqueId = parts[parts.length - 1];
			MailingList mailingList = mailingListManager.getMailingListByUniqueId(uniqueId);
			if (mailingList == null) {
				continue;
			}
			
			FoundMessagesInfo messagesForMailingList = mailingListsMessages.get(uniqueId);
			if (messagesForMailingList == null) {
				messagesForMailingList = new FoundMessagesInfo(getParserType());
				messagesForMailingList.addMessage(message);
				mailingListsMessages.put(uniqueId, messagesForMailingList);
			} else {
				messagesForMailingList.addMessage(message);
			}
		}
		
		return mailingListsMessages;
	}

	public MessageParserType getParserType() {
		return MessageParserType.MAILING_LIST;
	}

}
