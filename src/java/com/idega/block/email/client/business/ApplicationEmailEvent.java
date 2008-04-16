package com.idega.block.email.client.business;

import java.util.Map;

import javax.mail.Message;

import org.springframework.context.ApplicationEvent;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.1 $
 * 
 * Last modified: $Date: 2008/04/16 15:43:33 $ by $Author: arunas $
 */
public class ApplicationEmailEvent extends ApplicationEvent{
    private static final long serialVersionUID = -1382336725088284318L;
    private Map <String, Message> messages;
    
    public ApplicationEmailEvent(Object source) {
	super(source);
	
    }
    
    public Map<String, Message> getMessages() {
        return messages;
    }
    
    public void setMessages(Map<String, Message> messages) {
        this.messages = messages;
    }

}
