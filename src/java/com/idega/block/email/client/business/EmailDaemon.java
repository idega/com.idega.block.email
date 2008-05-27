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
import com.idega.idegaweb.IWMainApplicationShutdownEvent;
import com.idega.idegaweb.IWMainApplicationStartedEvent;
import com.idega.util.CoreConstants;
import com.idega.util.EventTimer;

/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.11 $
 * 
 * Last modified: $Date: 2008/05/27 19:05:58 $ by $Author: valdas $
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
    private static final String PROP_SYSTEM_PROTOCOL = "mail_protocol";
    private static final String PROP_SYSTEM_PASSWORD = "mail_password";
    private String host, account_name, protocol, password;
    
    public void start() {
    	
    	try {
		
    		mailUser = new MailUserBean();
    		
    		long defaultCheckInterval = EventTimer.THREAD_SLEEP_5_MINUTES;
    		String checkIntervalStr = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty("email_daemon_check_interval", String.valueOf(defaultCheckInterval));
    		
    		long checkInterval;
    		
    		if(CoreConstants.EMPTY.equals(checkIntervalStr))
    			checkInterval = defaultCheckInterval;
    		else
    			checkInterval = new Long(checkIntervalStr);
    		
    		this.emailTimer = new EventTimer(checkInterval, THREAD_NAME);
    		this.emailTimer.addActionListener(this);
    		
    		this.emailTimer.start(checkInterval);
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public void actionPerformed(ActionEvent event) {
	try {
		
	    if (event.getActionCommand().equalsIgnoreCase(THREAD_NAME)) {
	    	
		this.host = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(PROP_MAIL_HOST, CoreConstants.EMPTY);
		this.account_name = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(CoreConstants.PROP_SYSTEM_ACCOUNT, CoreConstants.EMPTY);
		this.protocol = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(PROP_SYSTEM_PROTOCOL, CoreConstants.EMPTY);
		this.password = IWMainApplication.getDefaultIWMainApplication().getSettings().getProperty(PROP_SYSTEM_PASSWORD, CoreConstants.EMPTY);
		
		if (!CoreConstants.EMPTY.equals(this.host)) {
		    if ((CoreConstants.EMPTY.equals(this.account_name)) || (CoreConstants.EMPTY.equals(this.protocol)) || (CoreConstants.EMPTY.equals(this.password))){    
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Mail properties are empty");
		    }else {
			    mailUser.login(this.host,this.account_name, this.password,this.protocol);	
//			    getting message map
			    Map<String, Message> messages = mailUser.getMessageMap();
			    
			    if ((messages != null) && (!messages.isEmpty())){
			     
				ApplicationEmailEvent eventEmail = new ApplicationEmailEvent(this);
				eventEmail.setMessages(messages);
				ctx.publishEvent(eventEmail);
			    }
			    
			    mailUser.moveMessages();
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
		    
		} else if (applicationevent instanceof IWMainApplicationShutdownEvent){
		    this.deamon.stop();
		}
    }

    public void setApplicationContext(ApplicationContext applicationcontext)
	    throws BeansException {
		ctx = applicationcontext;
    }
}