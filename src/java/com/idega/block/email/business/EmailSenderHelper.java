package com.idega.block.email.business;

import com.idega.block.email.bean.MessageParameters;
import com.idega.dwr.business.DWRAnnotationPersistance;

/**
 * Helper interface for sending e-mails
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2009/04/17 11:22:04 $ by: $Author: valdas $
 */
public interface EmailSenderHelper extends DWRAnnotationPersistance {

	public static final String DWR_OBJECT = "EmailSender";
	
	public boolean sendMessage(MessageParameters parameters);
	
}
