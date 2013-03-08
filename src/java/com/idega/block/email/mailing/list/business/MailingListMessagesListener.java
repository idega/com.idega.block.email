package com.idega.block.email.mailing.list.business;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.client.business.ApplicationEmailEvent;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class MailingListMessagesListener implements ApplicationListener<ApplicationEmailEvent> {

	@Override
	public void onApplicationEvent(ApplicationEmailEvent event) {
		MailingListMessagesWorker mailingListMessagesWorker = new MailingListMessagesWorker(event.getMessages(), event.getEmailParams());
		Thread worker = new Thread(mailingListMessagesWorker);
		worker.start();
	}

}
