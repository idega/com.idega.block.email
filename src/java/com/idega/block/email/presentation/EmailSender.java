package com.idega.block.email.presentation;

import java.util.Arrays;

import javax.faces.context.FacesContext;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.business.EmailSenderHelper;
import com.idega.block.email.business.EmailSenderStateBean;
import com.idega.block.web2.business.JQuery;
import com.idega.block.web2.business.Web2Business;
import com.idega.facelets.ui.FaceletComponent;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.IWBaseComponent;
import com.idega.presentation.IWContext;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.expression.ELUtil;

/**
 * Simple e-mail form
 * 
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2009/04/17 11:22:04 $ by: $Author: valdas $
 */
public class EmailSender extends IWBaseComponent {

	public static final String FROM_PARAMETER = "from";
	public static final String RECIPIENT_TO_PARAMETER = "recipientTo";
	public static final String RECIPIENT_CC_PARAMETER = "recipientCc";
	public static final String RECIPIENT_BCC_PARAMETER = "recipientBcc";
	public static final String SUBJECT_PARAMETER = "subject";
	public static final String MESSAGE_PARAMETER = "message";
	
	@Autowired
	private EmailSenderStateBean emailSender;
	
	@Autowired
	private JQuery jQuery;
	
	@Autowired
	private Web2Business web2;
	
	private String from;
	private String recipientTo;
	private String recipientCc;
	private String recipientBcc;
	private String subject;
	private String message;
	
	@Override
	protected void initializeComponent(FacesContext context) {
		ELUtil.getInstance().autowire(this);
		
		IWContext iwc = IWContext.getIWContext(context);
		FaceletComponent facelet = (FaceletComponent) context.getApplication().createComponent(FaceletComponent.COMPONENT_TYPE);
		facelet.setFaceletURI(iwc.getIWMainApplication().getBundle(EmailConstants.IW_BUNDLE_IDENTIFIER).getFaceletURI("emailSender.xhtml"));
		getChildren().add(facelet);
		
		IWBundle coreBundle = CoreUtil.getCoreBundle();
		IWBundle bundle = getBundle(context, EmailConstants.IW_BUNDLE_IDENTIFIER);
		IWResourceBundle iwrb = bundle.getResourceBundle(iwc);
		PresentationUtil.addStyleSheetsToHeader(iwc, Arrays.asList(
				bundle.getVirtualPathWithFileNameString("style/email.css"),
				web2.getBundleUriToHumanizedMessagesStyleSheet(),
				coreBundle.getVirtualPathWithFileNameString("style/iw_core.css")
		));
		PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, Arrays.asList(
				coreBundle.getVirtualPathWithFileNameString("iw_core.js"),
				bundle.getVirtualPathWithFileNameString("javascript/EmailSenderHelper.js"),
				CoreConstants.DWR_UTIL_SCRIPT,
				CoreConstants.DWR_ENGINE_SCRIPT,
				new StringBuilder("/dwr/interface/").append(EmailSenderHelper.DWR_OBJECT).append(".js").toString(),
				
				jQuery.getBundleURIToJQueryLib(),
				web2.getBundleUriToHumanizedMessagesScript()
		));
		PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, jQuery.getBundleURISToValidation());
		
		String initAction = new StringBuilder("EmailSenderHelper.setLocalizations({sending: '")
			.append(iwrb.getLocalizedString("email_sender.sending", "Sending...")).append("', error: '")
			.append(iwrb.getLocalizedString("email_sender.error", "Ooops... Some error occured while sending email...")).append("', success: '")
			.append(iwrb.getLocalizedString("email_sender.success", "E-mail was successfully sent")).append("', enterSenderEmail: '")
			.append(iwrb.getLocalizedString("email_sender.enter_sender_email", "Please enter a valid sender email address"))
			.append("', enterRecipientEmail: '")
			.append(iwrb.getLocalizedString("email_sender.enter_recipient_email", "Please enter a valid recipient email address"))
			.append("', enterValidEmail: '").append(iwrb.getLocalizedString("email_sender.enter_valid_email", "Please enter a valid email address"))
			.append("', enterSubject: '").append(iwrb.getLocalizedString("email_sender.enter_subject", "Please enter subject"))
			.append("', enterMessage: '").append(iwrb.getLocalizedString("email_sender.enter_message", "Please enter some message"))
		.append("'});").toString();
		if (!CoreUtil.isSingleComponentRenderingProcess(iwc)) {
			initAction = new StringBuilder("jQuery(window).load(function() {").append(initAction).append("});").toString();
		}
		PresentationUtil.addJavaScriptActionToBody(iwc, initAction);
		
		if (iwc.isParameterSet(FROM_PARAMETER)) {
			setFrom(iwc.getParameter(FROM_PARAMETER));
		}
		if (iwc.isParameterSet(RECIPIENT_TO_PARAMETER)) {
			setRecipientTo(iwc.getParameter(RECIPIENT_TO_PARAMETER));
		}
		if (iwc.isParameterSet(RECIPIENT_CC_PARAMETER)) {
			setRecipientCc(iwc.getParameter(RECIPIENT_CC_PARAMETER));
		}
		if (iwc.isParameterSet(RECIPIENT_BCC_PARAMETER)) {
			setRecipientBcc(iwc.getParameter(RECIPIENT_BCC_PARAMETER));
		}
		if (iwc.isParameterSet(SUBJECT_PARAMETER)) {
			setSubject(iwc.getParameter(SUBJECT_PARAMETER));
		}
		if (iwc.isParameterSet(MESSAGE_PARAMETER)) {
			setMessage(iwc.getParameter(MESSAGE_PARAMETER));
		}
		
		getEmailSender().setFrom(getFrom());
		getEmailSender().setRecipientTo(getRecipientTo());
		getEmailSender().setRecipientCc(getRecipientCc());
		getEmailSender().setRecipientBcc(getRecipientBcc());
		getEmailSender().setSubject(getSubject());
		getEmailSender().setMessage(getMessage());
	}

	@Override
	public void restoreState(FacesContext ctx, Object state) {
		super.restoreState(ctx, state);
	}

	@Override
	public Object saveState(FacesContext ctx) {
		return super.saveState(ctx);
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

	public EmailSenderStateBean getEmailSender() {
		return emailSender;
	}

	public void setEmailSender(EmailSenderStateBean emailSender) {
		this.emailSender = emailSender;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public JQuery getJQuery() {
		return jQuery;
	}

	public void setJQuery(JQuery query) {
		jQuery = query;
	}

	public Web2Business getWeb2() {
		return web2;
	}

	public void setWeb2(Web2Business web2) {
		this.web2 = web2;
	}
	
}
