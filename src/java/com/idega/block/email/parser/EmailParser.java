package com.idega.block.email.parser;

import java.util.Collection;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.core.messaging.EmailMessage;

/**
 * Interface for e-mails' parser
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/22 12:55:16 $ by $Author: valdas $
 */
public interface EmailParser {
	
	public abstract Collection<? extends EmailMessage> getParsedMessagesCollection(Map<String, FoundMessagesInfo> messages, EmailParams params);
	
	public abstract Collection<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent);
	
	public abstract Map<String, Collection<? extends EmailMessage>> getParsedMessages(Map<String, FoundMessagesInfo> messages, EmailParams params);
	
	public abstract EmailMessage getParsedMessage(Message message, EmailParams params) throws Exception;
	
	public abstract MessageParserType getMessageParserType();
	
	public abstract String getFromAddress(Message  message) throws MessagingException;
}
