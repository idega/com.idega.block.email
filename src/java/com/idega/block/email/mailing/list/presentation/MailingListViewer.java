package com.idega.block.email.mailing.list.presentation;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.email.data.Message;
import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.block.web2.business.JQuery;
import com.idega.block.web2.business.Web2Business;
import com.idega.core.file.data.ICFile;
import com.idega.idegaweb.IWResourceBundle;
import com.idega.presentation.CSSSpacer;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.PresentationObject;
import com.idega.presentation.Span;
import com.idega.presentation.text.DownloadLink;
import com.idega.presentation.text.Heading2;
import com.idega.presentation.text.Heading3;
import com.idega.presentation.text.ListItem;
import com.idega.presentation.text.Lists;
import com.idega.presentation.text.Text;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IWTimestamp;
import com.idega.util.ListUtil;
import com.idega.util.PresentationUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.text.TextSoap;

public class MailingListViewer extends BasicMailingList {

	public static final String PARAMETER_MAILING_LIST_UNIQUE_ID = "mailingListUniqueId";
	
	private String mailingListUniqueId;
	
	private MailingList mailingList;
	
	@Autowired
	private JQuery jQuery;
	@Autowired
	private Web2Business web2;
	
	@Override
	protected void doBusiness(IWContext iwc) {
		if (iwc.isParameterSet(PARAMETER_MAILING_LIST_UNIQUE_ID)) {
			mailingList = mailingListManager.getMailingListByUniqueId(iwc.getParameter(PARAMETER_MAILING_LIST_UNIQUE_ID));
		}
	}

	@Override
	protected void present(IWContext iwc) throws Exception {
		PresentationUtil.addJavaScriptSourcesLinesToHeader(iwc, Arrays.asList(
				jQuery.getBundleURIToJQueryLib(),
				web2.getBundleUriToLinkLinksWithFilesScriptFile()
		));
		PresentationUtil.addStyleSheetToHeader(iwc, web2.getBundleUriToLinkLinksWithFilesStyleFile());
		
		IWResourceBundle iwrb = getResourceBundle(iwc);
		
		if (mailingList == null) {
			add(new Heading3(iwrb.getLocalizedString("ml_viewer.unkown_mailing_list", "Mailing list was not found")));
			return;
		}
		
		Layer container = new Layer();
		add(container);
		container.add(new Heading2(mailingList.getName()));
		
		Collection<Message> messages = mailingList.getMessages();
		if (ListUtil.isEmpty(messages)) {
			container.add(new Heading3(iwrb.getLocalizedString("ml_viewer.there_are_no_messages", "There are no messages yet")));
			return;
		}
		
		Locale locale = iwc.getCurrentLocale();
		
		Class<? extends PresentationObject> messagesContainerClass =  messages.size() == 1 ? Layer.class : Lists.class;
		PresentationObject messagesContainer = messagesContainerClass.newInstance();
		container.add(messagesContainer);
		messagesContainer.setStyleClass("mailingListMessages");
		
		Class<? extends PresentationObject> messageContainerClass =  messages.size() == 1 ? Span.class : ListItem.class;
		for (Message message: messages) {
			String content = StringHandler.getContentFromInputStream(message.getMessageContent());
			
			PresentationObject messageContainer = messageContainerClass.newInstance();
			messagesContainer.getChildren().add(messageContainer);
			messageContainer.setStyleClass("mailingListMessage");
			
			Layer header = new Layer();
			messageContainer.getChildren().add(header);
			header.setStyleClass("mailingListMessageHeader");
			Layer subject = new Layer();
			header.add(subject);
			subject.setStyleClass("mailingListMessageSubject");
			subject.add(message.getSubject());
			
			Layer received = new Layer();
			header.add(received);
			received.setStyleClass("mailingListMessageReceived");
			IWTimestamp receivedAt = new IWTimestamp(message.getReceived());
			received.add(receivedAt.getLocaleDateAndTime(locale, DateFormat.FULL, DateFormat.MEDIUM));
			
			messageContainer.getChildren().add(new CSSSpacer());
			
			Layer messageContent = new Layer();
			messageContainer.getChildren().add(messageContent);
			messageContent.setStyleClass("mailingListMessageContent");
			content = StringUtil.isEmpty(content) ? CoreConstants.MINUS : TextSoap.formatText(content);
			messageContent.add(new Span(new Text(content)));
			
			Collection<ICFile> attachments = message.getAttachments();
			if (!ListUtil.isEmpty(attachments)) {
				Layer attachmentsContainer = new Layer();
				messageContainer.getChildren().add(attachmentsContainer);
				attachmentsContainer.setStyleClass("mailingListMessageAttachments");
				Lists attachmentsList = new Lists();
				attachmentsContainer.add(attachmentsList);
				for (ICFile attachment: attachments) {
					DownloadLink link = new DownloadLink(Integer.valueOf(attachment.getId()));
					link.setMarkupAttribute("rel", attachment.getMimeType());
					link.setText(attachment.getName());
					ListItem item = new ListItem();
					item.add(link);
					attachmentsList.add(item);
					
					Integer fileSize = attachment.getFileSize();
					if (fileSize != null) {
						Span fileSizeUI = new Span(new Text(FileUtil.getHumanReadableSize(fileSize)));
						fileSizeUI.setStyleClass("mailingListMessageAttachmentSize");
						item.add(fileSizeUI);
					}
				}
				String action = new StringBuilder("LinksLinker.linkLinks(false, '").append(attachmentsContainer.getId()).append("');").toString();
				if (!CoreUtil.isSingleComponentRenderingProcess(iwc)) {
					action = new StringBuilder("jQuery(window).load(function() {").append(action).append("});").toString();
				}
				PresentationUtil.addJavaScriptActionToBody(iwc, action);
			}
		}
	}

	public String getMailingListUniqueId() {
		return mailingListUniqueId;
	}

	public void setMailingListUniqueId(String mailingListUniqueId) {
		this.mailingListUniqueId = mailingListUniqueId;
	}

	public MailingList getMailingList() {
		return mailingList;
	}

	public void setMailingList(MailingList mailingList) {
		this.mailingList = mailingList;
	}
}
