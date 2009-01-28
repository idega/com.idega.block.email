package com.idega.block.email.client.business;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationShutdownEvent;
import com.idega.idegaweb.IWMainApplicationStartedEvent;
import com.idega.util.CoreConstants;
import com.idega.util.EventTimer;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.13 $
 * 
 * Last modified: $Date: 2009/01/28 12:19:01 $ by $Author: juozas $
 */

@Scope("singleton")
@Service
public class EmailDaemon implements ApplicationContextAware,
		ApplicationListener, ActionListener {

	public static final String THREAD_NAME = "email_daemon";
	private EventTimer emailTimer;
	private final ReentrantLock lock = new ReentrantLock();

	@Autowired
	private EmailSubjectPatternFinder emailFinder;
	private ApplicationContext ctx;

	public static final String PROP_MAIL_HOST = "mail_host";
	private static final String PROP_SYSTEM_PROTOCOL = "mail_protocol";
	private static final String PROP_SYSTEM_PASSWORD = "mail_password";

	public void start() {

		try {
			long defaultCheckInterval = EventTimer.THREAD_SLEEP_5_MINUTES;
			String checkIntervalStr = IWMainApplication
					.getDefaultIWMainApplication().getSettings().getProperty(
							"email_daemon_check_interval",
							String.valueOf(defaultCheckInterval));

			long checkInterval;

			if (CoreConstants.EMPTY.equals(checkIntervalStr))
				checkInterval = defaultCheckInterval;
			else
				checkInterval = new Long(checkIntervalStr);

			emailTimer = new EventTimer(checkInterval, THREAD_NAME);
			emailTimer.addActionListener(this);
			emailTimer.start(checkInterval);

		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Exception while starting up email daemon", e);
		}
	}

	public void actionPerformed(ActionEvent event) {
		try {

			if (event.getActionCommand().equalsIgnoreCase(THREAD_NAME)) {

				if (!lock.isLocked()) {
//					locking for long running checks in the inbox (lots of messages). skipping processing, if it's already under processing (locked)
					lock.lock();

					try {
						String host = IWMainApplication
								.getDefaultIWMainApplication().getSettings()
								.getProperty(PROP_MAIL_HOST,
										CoreConstants.EMPTY);
						String accountName = IWMainApplication
								.getDefaultIWMainApplication().getSettings()
								.getProperty(CoreConstants.PROP_SYSTEM_ACCOUNT,
										CoreConstants.EMPTY);
						String protocol = IWMainApplication
								.getDefaultIWMainApplication().getSettings()
								.getProperty(PROP_SYSTEM_PROTOCOL,
										CoreConstants.EMPTY);
						String password = IWMainApplication
								.getDefaultIWMainApplication().getSettings()
								.getProperty(PROP_SYSTEM_PASSWORD,
										CoreConstants.EMPTY);

						if (!CoreConstants.EMPTY.equals(host)) {
							if ((CoreConstants.EMPTY.equals(accountName))
									|| (CoreConstants.EMPTY.equals(protocol))
									|| (CoreConstants.EMPTY.equals(password))) {
								Logger.getLogger(getClass().getName()).log(
										Level.WARNING,
										"Mail properties are empty");
							} else {

								EmailSubjectPatternFinder emailFinder = getEmailFinder();
								EmailParams params = emailFinder.login(host,
										accountName, password, protocol);
								// getting message map
								Map<String, List<Message>> messages = emailFinder
										.getMessageMap(params);

								if ((messages != null) && (!messages.isEmpty())) {

									ApplicationEmailEvent eventEmail = new ApplicationEmailEvent(
											this);
									eventEmail.setMessages(messages);
									ctx.publishEvent(eventEmail);
								}

								emailFinder.moveMessages(params);
								emailFinder.logout(params);
							}
						}

					} finally {
						lock.unlock();
					}
				}
			}

		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE,
					"Exceptio while processing emails found in the inbox", e);
		}

	}

	public void stop() {

		if (this.emailTimer != null) {
			this.emailTimer.stop();
			this.emailTimer = null;
		}

	}

	public void onApplicationEvent(ApplicationEvent applicationevent) {

		if (applicationevent instanceof IWMainApplicationStartedEvent) {
			start();

		} else if (applicationevent instanceof IWMainApplicationShutdownEvent) {
			stop();
		}
	}

	public void setApplicationContext(ApplicationContext applicationcontext)
			throws BeansException {
		ctx = applicationcontext;
	}

	public EmailSubjectPatternFinder getEmailFinder() {
		return emailFinder;
	}
}