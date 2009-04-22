package com.idega.block.email.client.business;

import java.util.List;
import java.util.Map;

import javax.mail.Message;

import org.springframework.context.ApplicationEvent;

import com.idega.block.email.bean.MessageParameters;
/**
 * @author <a href="mailto:arunas@idega.com">Arūnas Vasmanas</a>
 * @version $Revision: 1.3 $
 * 
 * Last modified: $Date: 2009/04/22 12:55:16 $ by $Author: valdas $
 */
public class ApplicationEmailEvent extends ApplicationEvent {
    
	private static final long serialVersionUID = -1382336725088284318L;
   
	private Map <String, List<Message>> messages;
	private MessageParameters parameters;
    
    public ApplicationEmailEvent(Object source) {
    	super(source);
    }
    
    public Map<String, List<Message>> getMessages() {
        return messages;
    }
    
    public void setMessages(Map<String, List<Message>> messages) {
        this.messages = messages;
    }

	public MessageParameters getParameters() {
		return parameters;
	}

	public void setParameters(MessageParameters parameters) {
		this.parameters = parameters;
	}

}
