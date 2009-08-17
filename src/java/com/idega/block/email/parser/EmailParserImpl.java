package com.idega.block.email.parser;

import javax.mail.Message;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.EmailParams;
import com.idega.core.messaging.EmailMessage;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
@Qualifier("defaultEmailsParser")
public class EmailParserImpl extends DefaultMessageParser implements EmailParser {

	@Override
	public EmailMessage getParsedMessage(Message message, EmailParams params) throws Exception {
		return super.getParsedMessage(message, params);
	}

	public MessageParserType getMessageParserType() {
		return MessageParserType.MAILING_LIST;
	}

}
