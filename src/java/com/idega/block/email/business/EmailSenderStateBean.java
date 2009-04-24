package com.idega.block.email.business;

import java.util.Collection;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.presentation.EmailSender;
import com.idega.core.contact.data.Email;
import com.idega.core.contact.data.EmailType;
import com.idega.core.contact.data.EmailTypeBMPBean;
import com.idega.presentation.IWContext;
import com.idega.user.data.User;
import com.idega.util.CoreUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

/**
 * State bean for {@link EmailSender}
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2009/04/24 12:11:45 $ by: $Author: valdas $
 */

@Scope("request")
@Service(EmailSenderStateBean.BEAN_IDENTIFIER)
public class EmailSenderStateBean {

	public static final String BEAN_IDENTIFIER = "emailSenderStateBean";
	
	private String from;
	private String replyTo;
	private String recipientTo;
	private String recipientCc;
	private String recipientBcc;
	private String subject;
	private String message;
	
	private User currentUser;
	private String currentUserEmail;
	
	private List<String> namesForExternalParameters;
	private List<String> externalParameters;
	
	private boolean allowChangeRecipientAddress = true;
	
	public String getFrom() {
		if (from == null) {
			from = getCurrentUserEmail();
		}
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getRecipientTo() {
		return recipientTo;
	}
	public void setRecipientTo(String recipientTo) {
		this.recipientTo = recipientTo;
	}
	public String getRecipientCc() {
		return recipientCc;
	}
	public void setRecipientCc(String recipientCc) {
		this.recipientCc = recipientCc;
	}
	public String getRecipientBcc() {
		return recipientBcc;
	}
	public void setRecipientBcc(String recipientBcc) {
		this.recipientBcc = recipientBcc;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	@SuppressWarnings("unchecked")
	private String getCurrentUserEmail() {
		if (currentUserEmail == null) {
			User currentUser = getCurrentUser();
			if (currentUser == null) {
				IWContext iwc = CoreUtil.getIWContext();
				if (!iwc.isLoggedOn()) {
					return null;
				}
				
				currentUser = iwc.getCurrentUser();
			}
			if (currentUser == null) {
				return null;
			}
			
			Collection<Email> emails = currentUser.getEmails();
			if (ListUtil.isEmpty(emails)) {
				return null;
			}
			
			EmailType mailType = null;
			for (Email mail: emails) {
				mailType = mail.getEmailType();
				if (mailType != null && EmailTypeBMPBean.MAIN_EMAIL.equals(mailType.getUniqueName())) {
					currentUserEmail = mail.getEmailAddress();
				}
			}
			
			if (StringUtil.isEmpty(currentUserEmail)) {
				currentUserEmail = emails.iterator().next().getEmailAddress();
			}
		}
		return currentUserEmail;
	}
	
	public List<String> getExternalParameters() {
		return externalParameters;
	}
	public void setExternalParameters(List<String> externalParameters) {
		this.externalParameters = externalParameters;
	}
	
	public User getCurrentUser() {
		return currentUser;
	}
	public void setCurrentUser(User currentUser) {
		this.currentUser = currentUser;
	}
	
	public List<String> getNamesForExternalParameters() {
		return namesForExternalParameters;
	}
	public void setNamesForExternalParameters(
			List<String> namesForExternalParameters) {
		this.namesForExternalParameters = namesForExternalParameters;
	}
	public String getReplyTo() {
		return replyTo;
	}
	public void setReplyTo(String replyTo) {
		this.replyTo = replyTo;
	}
	
	public boolean isAllowChangeRecipientAddress() {
		return allowChangeRecipientAddress;
	}
	public void setAllowChangeRecipientAddress(boolean allowChangeRecipientAddress) {
		this.allowChangeRecipientAddress = allowChangeRecipientAddress;
	}
	
}
