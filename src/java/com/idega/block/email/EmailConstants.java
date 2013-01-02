package com.idega.block.email;

import com.idega.util.CoreConstants;

/**
 * Constants for com.idega.block.email bundle
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2009/04/17 11:22:05 $ by: $Author: valdas $
 */

public class EmailConstants {

	public static final String IW_BUNDLE_IDENTIFIER = "com.idega.block.email";
	
	public static final String MAILING_LIST_MESSAGE_RECEIVER = CoreConstants.PROP_SYSTEM_ACCOUNT;
	
	public static final String MULTIPART_MIXED_TYPE = "multipart/Mixed";
	public static final String MULTIPART_ALTERNATIVE_TYPE = "multipart/alternative";
	public static final String MULTIPART_RELATED_TYPE = "multipart/related";
	public static final String MESSAGE_RFC822_TYPE = "message/rfc822";
	public static final String MESSAGE_MULTIPART_SIGNED = "multipart/signed";
	public static final String MESSAGE_MULTIPART_REPORT = "multipart/report";
	
	public static final String IW_MAILING_LIST = "-iwlist";
}
