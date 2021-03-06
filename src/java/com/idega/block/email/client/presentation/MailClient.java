package com.idega.block.email.client.presentation;

import java.util.Iterator;
import java.util.Map;

import javax.faces.component.UIComponent;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.business.EmailAccount;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.client.business.EmailSubjectPatternFinder;
import com.idega.block.email.client.business.MessageFinder;
import com.idega.block.email.client.business.MessageInfo;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.Block;
import com.idega.presentation.IWContext;
import com.idega.presentation.Table;
import com.idega.presentation.text.Link;
import com.idega.presentation.ui.Form;
import com.idega.presentation.ui.SubmitButton;
import com.idega.presentation.ui.TextInput;
import com.idega.util.expression.ELUtil;

/**
 * Title: Description: Copyright: Copyright (c) 2001 Company:
 *
 * @author <br>
 *         <a href="mailto:aron@idega.is">Aron Birkir</a><br>
 * @version 1.0
 */
public class MailClient extends Block {

	private final static String prmMsgNum = "em_cl_msg_num";
	private final static String prmSessionParams = "em_email_params";
	private final static String prmSessionUserMsgs = "em_email_user_msgs";
	private final static String prmAction = "em_client_action";

	// /private EmailSubjectPatternFinder mailuser;
	private EmailParams emailParams;
	private Map<Integer, MessageInfo> messagesMap;
	private EmailAccount mailaccount;

	private IWResourceBundle iwrb;

	public MailClient() {
	}

	@Override
	public String getBundleIdentifier() {
		return EmailConstants.IW_BUNDLE_IDENTIFIER;
	}

	@Override
	public void main(IWContext iwc) throws Exception {
		debugParameters(iwc);
		this.iwrb = getResourceBundle(iwc);
		// process forms
		processForm(iwc);
		// initialize accounts
		initAccount(iwc);
		Table T = new Table();

		if (emailParams != null) {
			if (iwc.isParameterSet(prmMsgNum)) {
				T.add(getMessage(iwc));
			} else {
				T.add(getListMessages(iwc));
			}
		} else {
			T.add(getLogin(iwc));
		}
		Form f = new Form();
		f.add(T);
		add(f);
	}

	@SuppressWarnings("unchecked")
	public void initAccount(IWContext iwc) throws Exception {
		if (iwc.getSessionAttribute(prmSessionParams) != null) {
			emailParams = (EmailParams) iwc.getSessionAttribute(prmSessionParams);
			this.messagesMap = (Map<Integer, MessageInfo>) iwc.getSessionAttribute(prmSessionUserMsgs);
		} else {
			if (this.mailaccount != null) {

				emailParams = new EmailParams();
				//this.mailuser = new EmailSubjectPatternFinder();
				emailParams.setHostname(mailaccount.getHost());
				emailParams.setPassword(mailaccount.getPassword());
				emailParams.setProtocol(mailaccount.getProtocolName());
				emailParams.setUsername(mailaccount.getUser());

			} else if ("login".equals(iwc.getParameter(prmAction))) {
				//this.mailuser = new EmailSubjectPatternFinder();
				String host = iwc.getParameter("host");
				String pass = iwc.getParameter("pass");
				String prot = iwc.getParameter("prot");
				String user = iwc.getParameter("user");

//				this.mailuser.setHostname(host);
//				this.mailuser.setPassword(pass);
//				this.mailuser.setProtocol(prot);
//				this.mailuser.setUsername(user);

				emailParams = new EmailParams();
				//this.mailuser = new EmailSubjectPatternFinder();
				emailParams.setHostname(host);
				emailParams.setPassword(pass);
				emailParams.setProtocol(prot);
				emailParams.setUsername(user);
			}

			if (emailParams != null) {

				EmailSubjectPatternFinder emailFinder = ELUtil.getInstance().getBean(EmailSubjectPatternFinder.BEAN_IDENTIFIER);
				emailFinder.login(emailParams);
				this.messagesMap = MessageFinder
						.getMappedMessagesInfo(emailParams);
				iwc.setSessionAttribute(prmSessionParams, emailParams);
				iwc.setSessionAttribute(prmSessionUserMsgs, this.messagesMap);
			}
		}
	}

	public void processForm(IWContext iwc) {

	}

	public UIComponent getLogin(IWContext iwc) {
		Table T = new Table();

		T.add(this.iwrb.getLocalizedString("client.user", "User"), 1, 1);
		T.add(new TextInput("user"), 2, 1);
		T
				.add(this.iwrb
						.getLocalizedString("client.password", "Password"), 1,
						2);
		T.add(new TextInput("pass"), 2, 2);
		T.add(this.iwrb.getLocalizedString("client.host", "Host"), 1, 3);
		T.add(new TextInput("host"), 2, 3);
		T
				.add(this.iwrb
						.getLocalizedString("client.protocol", "Protocol"), 1,
						4);
		T.add(new TextInput("prot", "pop3"), 2, 4);

		T.add(new SubmitButton(this.iwrb.getLocalizedImageButton(
				"client.login", "Login"), prmAction, "login"), 2, 5);
		return T;
	}

	public UIComponent getListMessages(IWContext iwc) {
		Table T = new Table();
		int row = 1;
		T.add(this.iwrb.getLocalizedString("client.from", "From"), 1, row);
		T
				.add(this.iwrb.getLocalizedString("client.subject", "Subject"),
						2, row);
		T.add(this.iwrb.getLocalizedString("client.date", "date"), 3, row);
		row++;
		try {
			if (this.messagesMap != null && this.messagesMap.size() > 0) {
				Iterator<MessageInfo> iter = this.messagesMap.values().iterator();
				MessageInfo m;
				while (iter.hasNext()) {
					m = iter.next();
					Link l = new Link(m.getSubject());
					l.addParameter(prmMsgNum, m.getNum());
					T.add(m.getFrom(), 1, row);
					T.add(l, 2, row);
					T.add(m.getDate(), 3, row);
					T.add(m.getNum(), 4, row);
					row++;
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return T;
	}

	public UIComponent getMessage(IWContext iwc) throws Exception {
		Table T = new Table(2, 5);
		String num = iwc.getParameter(prmMsgNum);
		if (num != null) {
			MessageInfo m = this.messagesMap
					.get(new Integer(num));
			if (m != null) {
				T
						.add(this.iwrb
								.getLocalizedString("client.from", "From"), 1,
								1);
				T.add(m.getFrom(), 2, 1);
				T
						.add(this.iwrb
								.getLocalizedString("client.date", "Date"), 1,
								2);
				T.add(m.getDate(), 2, 2);
				T.add(this.iwrb.getLocalizedString("client.to", "To"), 1, 3);
				T.add(m.getTo(), 2, 3);
				T.add(
						this.iwrb.getLocalizedString("client.subject",
								"Subject"), 1, 4);
				T.add(m.getSubject(), 2, 4);
				T.mergeCells(1, 5, 2, 5);
				T.add(m.getBody(), 1, 5);

			}
		}
		return T;
	}

}
