package com.idega.block.email.client.business;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationStartedEvent;
import com.idega.util.CoreConstants;
import com.idega.util.EventTimer;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.4 $
 * 
 * Last modified: $Date: 2008/04/16 16:19:08 $ by $Author: arunas $
 */

@Scope("singleton")
@Service
public class EmailDaemon implements ApplicationContextAware, ApplicationListener, ActionListener{

    public static final String THREAD_NAME= "email_daemon";
    private EventTimer emailTimer;
    private MailUserBean mailUser;
    private EmailDaemon deamon;
    private ApplicationContext ctx;

    public static final String PROP_MAIL_HOST = "mail_host";
    private static final String PROP_SYSTEM_ACCOUNT = "mail_user_account";
    private static final String PROP_SYSTEM_PROTOCOL = "mail_protocol";
    private static final String PROP_SYSTEM_PASSWORD = "mail_password";
    private String host, account_name, protocol, password;
    
    public void start() {
	mailUser = new MailUserBean();
	
	// checking uploaded files 5 minutes. 
	this.emailTimer = new EventTimer(EventTimer.THREAD_SLEEP_5_MINUTES,THREAD_NAME);
	this.emailTimer.addActionListener(this);
	
	// Starts the thread after 5 mins.
	this.emailTimer.start(EventTimer.THREAD_SLEEP_5_MINUTES);

    }
    
    public void actionPerformed(ActionEvent event) {
	try {
	    if (event.getActionCommand().equalsIgnoreCase(THREAD_NAME)) {
		
		this.host = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(PROP_MAIL_HOST, CoreConstants.EMPTY);
		this.account_name = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(PROP_SYSTEM_ACCOUNT, CoreConstants.EMPTY);
		this.protocol = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(PROP_SYSTEM_PROTOCOL, CoreConstants.EMPTY);
		this.password = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(PROP_SYSTEM_PASSWORD, CoreConstants.EMPTY);
		
		if (!CoreConstants.EMPTY.equals(this.host)) {
		    if ((CoreConstants.EMPTY.equals(this.host)) || (CoreConstants.EMPTY.equals(this.account_name)) || (CoreConstants.EMPTY.equals(this.protocol)) || (CoreConstants.EMPTY.equals(this.password))){    
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Host mail is empty");
		    }else {
			    mailUser.login(this.host,this.account_name, this.password,this.protocol);	
//			    getting message map
			    Map<String, Message> messages = mailUser.getMessageMap();
			 
			    if ((messages != null) && (!messages.isEmpty())){
			     
				ApplicationEmailEvent eventEmail = new ApplicationEmailEvent(this);
				eventEmail.setMessages(messages);
				ctx.publishEvent(eventEmail);
			    }
			 
			    mailUser.logout();
			}
		}
	    }

	} catch (Exception x) {
	    x.printStackTrace();
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
	    
	    this.deamon = new EmailDaemon();
	    this.deamon.setApplicationContext(ctx);
	    this.deamon.start();
	    
	}
	
    }

    public void setApplicationContext(ApplicationContext applicationcontext)
	    throws BeansException {
		ctx = applicationcontext;
		
    }

}
