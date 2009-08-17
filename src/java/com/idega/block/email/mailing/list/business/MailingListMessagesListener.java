package com.idega.block.email.mailing.list.business;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.client.business.ApplicationEmailEvent;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class MailingListMessagesListener implements ApplicationListener {

	public void onApplicationEvent(ApplicationEvent event) {
		if (!(event instanceof ApplicationEmailEvent)) {
			return;
		}

		ApplicationEmailEvent emailEvent = (ApplicationEmailEvent) event;
		MailingListMessagesWorker mailingListMessagesWorker = new MailingListMessagesWorker(emailEvent.getMessages(), emailEvent.getEmailParams());
		Thread worker = new Thread(mailingListMessagesWorker);
		worker.start();
	}

}
