package com.idega.block.email.patterns;

import java.io.Serializable;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.EmailParams;

public interface EmailSubjectSearchable extends Serializable {

	public abstract Message[] getMessages(EmailParams params) throws MessagingException;

	public abstract Map<String, FoundMessagesInfo> getSearchResultsFormatted(EmailParams params) throws MessagingException;

	public abstract MessageParserType getParserType();

	public String getFixedIdentifier(String identifier);

}