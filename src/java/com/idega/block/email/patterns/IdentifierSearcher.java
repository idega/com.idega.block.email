package com.idega.block.email.patterns;

import java.util.Map;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.EmailParams;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class IdentifierSearcher extends DefaultSubjectPatternFinder {

	private static final long serialVersionUID = -7004965182979660614L;

	private static final String IDENTIFIER_REGULAR_EXPRESSION = "[A-Z]{1,3}-\\d{4}-\\d{2}-\\d{2}-[A-Z0-9]{4,}",
								LOWER_CASE_IDENTIFIER_REGULAR_EXPRESSION = "[a-z]{1,3}-\\d{4}-\\d{2}-\\d{2}-[a-z0-9]{4,}";
	private static final Pattern	IDENTIFIER_PATTERN = Pattern.compile(IDENTIFIER_REGULAR_EXPRESSION),
									LOWER_CASE_IDENTIFIER_PATTERN = Pattern.compile(LOWER_CASE_IDENTIFIER_REGULAR_EXPRESSION);

	public IdentifierSearcher() {
		super();

		addPattern(IDENTIFIER_PATTERN);
		addPattern(LOWER_CASE_IDENTIFIER_PATTERN);
	}

	@Override
	public Map<String, FoundMessagesInfo> getSearchResultsFormatted(EmailParams params) throws MessagingException {
		return super.getCaseIdentifierSearchResultsFormatted(params);
	}

	@Override
	public MessageParserType getParserType() {
		return MessageParserType.BPM;
	}
}