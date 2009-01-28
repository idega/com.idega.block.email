package com.idega.block.email.client.business;

import java.util.List;
import java.util.Map;

import javax.mail.Message;

import org.springframework.context.ApplicationEvent;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.2 $
 * 
 * Last modified: $Date: 2009/01/28 12:19:01 $ by $Author: juozas $
 */
public class ApplicationEmailEvent extends ApplicationEvent{
    private static final long serialVersionUID = -1382336725088284318L;
    private Map <String, List<Message>> messages;
    
    public ApplicationEmailEvent(Object source) {
	super(source);
	
    }
    
    public Map<String, List<Message>> getMessages() {
        return messages;
    }
    
    public void setMessages(Map<String, List<Message>> messages) {
        this.messages = messages;
    }

}
