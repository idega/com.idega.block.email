package com.idega.block.email.client.business;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

import javax.mail.Message;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.core.business.DefaultSpringBean;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.idegaweb.IWMainApplicationShutdownEvent;
import com.idega.idegaweb.IWMainApplicationStartedEvent;
import com.idega.util.CoreConstants;
import com.idega.util.EventTimer;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.13 $
 *
 * Last modified: $Date: 2009/01/28 12:19:01 $ by $Author: juozas $
 */

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class EmailDaemon extends DefaultSpringBean implements ApplicationContextAware, ApplicationListener<ApplicationEvent>, ActionListener {

	public static final String THREAD_NAME = "email_daemon";

	private EventTimer emailTimer;
	private final ReentrantLock lock = new ReentrantLock();

	@Autowired
	private EmailSubjectPatternFinder emailFinder;
	private ApplicationContext ctx;

	public static final String PROP_MAIL_HOST = "mail_host";
	private static final String PROP_SYSTEM_PROTOCOL = "mail_protocol";
	private static final String PROP_SYSTEM_PASSWORD = "mail_password";
	private static final String PROP_SYSTEM_PORT = "mail_port";

	private static final String PROP_CLIENT_ID = "mail_client_id";
	private static final String PROP_CLIENT_SECRET = "mail_client_secret";
	private static final String PROP_TENANT_ID = "mail_tenant_id";
	private static final String PROP_USER_EMAIL = "mail_user_email";

	public void start() {

		try {
			long defaultCheckInterval = EventTimer.THREAD_SLEEP_5_MINUTES;
			String checkIntervalStr = IWMainApplication.getDefaultIWMainApplication().getSettings()
				.getProperty("email_daemon_check_interval", String.valueOf(defaultCheckInterval));

			long checkInterval;

			if (CoreConstants.EMPTY.equals(checkIntervalStr))
				checkInterval = defaultCheckInterval;
			else
				checkInterval = new Long(checkIntervalStr);

			emailTimer = new EventTimer(checkInterval, THREAD_NAME);
			emailTimer.addActionListener(this);
			emailTimer.start(checkInterval);

		} catch (Exception e) {
			getLogger().log(Level.SEVERE, "Exception while starting up email daemon", e);
		}
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		String accountName = null;
		EmailParams allParams = new EmailParams();
		try {
			if (event.getActionCommand().equalsIgnoreCase(THREAD_NAME)) {
				if (!lock.isLocked()) {
					//	Locking for long running checks in the inbox (lots of messages). skipping processing, if it's already under processing (locked)
					lock.lock();

					try {
						EmailSubjectPatternFinder emailFinder = getEmailFinder();

						IWMainApplicationSettings settings = getSettings();
						if (settings == null) {
							getLogger().warning(IWMainApplicationSettings.class.getSimpleName() + " not available!");
							return;
						}

						Map<String, FoundMessagesInfo> allMessages = new HashMap<>();
						Map<String, FoundMessagesInfo> simpleMailMessages = new HashMap<>();
						Map<String, FoundMessagesInfo> microsoftMessages = new HashMap<>();

						//*** SIMPLE MAIL MESSAGES ***
						if (settings.getBoolean("mail.fetch_emails_from_simple_mailbox", true)) {
							String host = settings.getProperty(PROP_MAIL_HOST, CoreConstants.EMPTY);
							accountName = settings.getProperty(CoreConstants.PROP_SYSTEM_ACCOUNT, CoreConstants.EMPTY);
							String protocol = settings.getProperty(PROP_SYSTEM_PROTOCOL, CoreConstants.EMPTY);
							String password = settings.getProperty(PROP_SYSTEM_PASSWORD, CoreConstants.EMPTY);
							Integer port = settings.getInt(PROP_SYSTEM_PORT, -1);

							boolean proceed = true;
							if (StringUtil.isEmpty(host) || StringUtil.isEmpty(accountName) || StringUtil.isEmpty(protocol) || StringUtil.isEmpty(password)) {
								getLogger().warning("Mail properties are empty: either account name (" + accountName + ") or email protocol (" + protocol +
										") or password for the mailbox ("+password+") or host " + host + ") are not known!");
								proceed = false;
							}

							if (proceed) {
								EmailParams params = emailFinder.login(host, accountName, password, protocol, port);

								// Getting message map
								simpleMailMessages = emailFinder.getMessageMap(params);

								//Logout if no messages
								if (MapUtil.isEmpty(simpleMailMessages)) {
									emailFinder.logout(params);
								}

								allParams.setFolder(params.getFolder());
								allParams.setHostname(params.getHostname());
								allParams.setPassword(params.getPassword());
								allParams.setPort(params.getPort());
								allParams.setProtocol(params.getProtocol());
								allParams.setSession(params.getSession());
								allParams.setUsername(params.getUsername());
								allParams.setStore(params.getStore());
								allParams.setLoggedOut(params.isLoggedOut());
								allParams.setMessagesFound(params.getMessagesFound());
							}

							if (!MapUtil.isEmpty(simpleMailMessages)) {
								allMessages.putAll(simpleMailMessages);
							}
						}

						//*** MICROSOFT MAIL MESSAGES ***
						if (settings.getBoolean("mail.fetch_emails_from_microsoft_mailbox", false)) {
							String clientId = settings.getProperty(PROP_CLIENT_ID, CoreConstants.EMPTY);
							String clientSecret = settings.getProperty(PROP_CLIENT_SECRET, CoreConstants.EMPTY);
							String tenantId = settings.getProperty(PROP_TENANT_ID, CoreConstants.EMPTY);
							String userEmail = settings.getProperty(PROP_USER_EMAIL, CoreConstants.EMPTY);

							boolean proceed = true;
							if (StringUtil.isEmpty(clientId) || StringUtil.isEmpty(clientSecret) || StringUtil.isEmpty(tenantId) || StringUtil.isEmpty(userEmail)) {
								getLogger().warning("Microsoft mail properties are empty: either client id (" + clientId + ") or client secret (" + clientSecret +
										") or tenant id (" + tenantId + ") or user email " + userEmail + ") are not known!");
								proceed = false;
							}

							if (proceed) {
								EmailParams params = emailFinder.loginToMSOffice(
										clientId,
										clientSecret,
										tenantId,
										userEmail
								);

								// Getting message map
								microsoftMessages = emailFinder.getMessageMap(params);

								if (!MapUtil.isEmpty(microsoftMessages)) {
									if (MapUtil.isEmpty(allMessages)) {
										allMessages.putAll(microsoftMessages);
									} else {
										for (Map.Entry<String, FoundMessagesInfo> microsoftMessagesEntry : microsoftMessages.entrySet()) {
											String mapKey = microsoftMessagesEntry.getKey();
											FoundMessagesInfo microsoftMessagesMapValue = microsoftMessagesEntry.getValue();
											if (
													StringUtil.isEmpty(mapKey)
													|| microsoftMessagesMapValue == null
													|| ListUtil.isEmpty(microsoftMessagesMapValue.getMessages())
											) {
												continue;
											}
											if (allMessages.containsKey(mapKey)) {
												Collection<Message> microsoftMessagesForKey = microsoftMessagesMapValue.getMessages();
												FoundMessagesInfo allMessagesMapValue = allMessages.get(mapKey);
												if (
														allMessagesMapValue == null
														|| ListUtil.isEmpty(allMessagesMapValue.getMessages())
												) {
													allMessages.put(mapKey, microsoftMessagesMapValue);
												} else {
													allMessagesMapValue.getMessages().addAll(microsoftMessagesForKey);
													allMessages.put(mapKey, allMessagesMapValue);
												}
											} else {
												allMessages.put(mapKey, microsoftMessagesMapValue);
											}
										}
									}
								}

								allParams.setToken(params.getToken());
								allParams.setUserEmail(params.getUserEmail());
							}
						}


						if (!MapUtil.isEmpty(allMessages)) {
							getLogger().info("Found " + allMessages.size() + " new emails at " + accountName + ". Keys: " + allMessages.keySet());
							ApplicationEmailEvent eventEmail = new ApplicationEmailEvent(this);
							eventEmail.setMessages(allMessages);
							eventEmail.setEmailParams(allParams);
							ctx.publishEvent(eventEmail);
						}
					} catch (Exception e) {
						getLogger().log(Level.WARNING, "Error scanning " + accountName + " for new emails. Parameters: " + allParams, e);
					} finally {
						lock.unlock();
					}
				}
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Exception while processing emails found in " + accountName + ". Parameters: " + allParams, e);
		}

	}

	public void stop() {

		if (this.emailTimer != null) {
			this.emailTimer.stop();
			this.emailTimer = null;
		}

	}

	@Override
	public void onApplicationEvent(ApplicationEvent applicationevent) {

		if (applicationevent instanceof IWMainApplicationStartedEvent) {
			start();

		} else if (applicationevent instanceof IWMainApplicationShutdownEvent) {
			stop();
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationcontext)
			throws BeansException {
		ctx = applicationcontext;
	}

	public EmailSubjectPatternFinder getEmailFinder() {
		return emailFinder;
	}
}