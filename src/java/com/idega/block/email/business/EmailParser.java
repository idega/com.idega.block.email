package com.idega.block.email.business;

import java.util.List;

import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.core.messaging.EmailMessage;

/**
 * Interface for e-mails' parser
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/22 12:55:16 $ by $Author: valdas $
 */
public interface EmailParser {

	public abstract List<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent);
	
}
