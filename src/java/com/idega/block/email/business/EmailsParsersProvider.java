package com.idega.block.email.business;

import java.util.List;

import com.idega.business.SpringBeanName;

/**
 * Interface for e-mails' parsers providers
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $ Last modified: $Date: 2009/04/22 12:55:16 $ by $Author: valdas $
 */

@SpringBeanName("emailsParsersProviderBean")
public interface EmailsParsersProvider {
	
	public abstract List<EmailParser> getAllParsers();
	
}
