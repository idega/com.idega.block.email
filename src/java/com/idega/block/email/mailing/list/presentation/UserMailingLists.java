package com.idega.block.email.mailing.list.presentation;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;

import javax.faces.component.UIComponent;

import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableBodyRowGroup;
import com.idega.presentation.TableHeaderRowGroup;
import com.idega.presentation.TableRow;
import com.idega.presentation.text.Heading3;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.CheckBox;
import com.idega.presentation.ui.Form;
import com.idega.presentation.ui.SubmitButton;
import com.idega.user.data.User;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public class UserMailingLists extends BasicMailingList {
	
	private static final String PARAMETER_SAVE_SETTINGS = "saveMyMailingListsSettings";
	private static final String PARAMETER_UN_SUBSCRIBE_FROM_MAILING_LIST = "unSubscribeFromMailingList";
	private static final String PARAMETER_TAKE_BACK_SUBSCRIBTION_FROM_MAILING_LIST = "takeBackSubscribtionFromMailingList";
	private static final String PARAMETER_SUBSCRIBE_TO_MAILING_LIST = "subscribeToMailingList";
	
	private Form form;
	
	@Override
	protected void doBusiness(IWContext iwc) {
		if (!iwc.isParameterSet(PARAMETER_SAVE_SETTINGS)) {
			return;
		}
	
		User currentUser = iwc.isLoggedOn() ? iwc.getCurrentUser() : null;
		if (currentUser == null) {
			return;
		}
		
		if (iwc.isParameterSet(PARAMETER_SUBSCRIBE_TO_MAILING_LIST)) {
			/*Collection<MailingList> listsToSubscribe = mailingListManager.getMailingLists();
			if (!ListUtil.isEmpty(listsToSubscribe)) {
				for (MailingList listToSubscribe: listsToSubscribe) {
					if (listToSubscribe.isPrivate()) {
						try {
							listToSubscribe.addToWaitingList(currentUser);
						} catch (IDOAddRelationshipException e) {
							e.printStackTrace();
						}
					} else {
						try {
							listToSubscribe.addSubscriber(currentUser);
						} catch (IDOAddRelationshipException e) {
							e.printStackTrace();
						}
					}
					listToSubscribe.store();
				}
			}*/
			mailingListManager.subscribeToMailingLists(Arrays.asList(iwc.getParameterValues(PARAMETER_SUBSCRIBE_TO_MAILING_LIST)), currentUser);
		}
		
		if (iwc.isParameterSet(PARAMETER_UN_SUBSCRIBE_FROM_MAILING_LIST)) {
			Collection<MailingList> listsToUnsubscribe = mailingListManager.getMailingLists(iwc.getParameterValues(PARAMETER_UN_SUBSCRIBE_FROM_MAILING_LIST));
			if (!ListUtil.isEmpty(listsToUnsubscribe)) {
				for (MailingList listToUnsubscribe: listsToUnsubscribe) {
					try {
						listToUnsubscribe.removeSubscriber(currentUser);
						listToUnsubscribe.store();
					} catch (IDORemoveRelationshipException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		if (iwc.isParameterSet(PARAMETER_TAKE_BACK_SUBSCRIBTION_FROM_MAILING_LIST)) {
			Collection<MailingList> listsToTakeBackSubscribtions = mailingListManager.getMailingLists(
					iwc.getParameterValues(PARAMETER_TAKE_BACK_SUBSCRIBTION_FROM_MAILING_LIST));
			if (!ListUtil.isEmpty(listsToTakeBackSubscribtions)) {
				for (MailingList listToTakeBackSubscribtion: listsToTakeBackSubscribtions) {
					try {
						listToTakeBackSubscribtion.removeFromWaitingList(currentUser);
						listToTakeBackSubscribtion.store();
					} catch (IDORemoveRelationshipException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public void present(IWContext iwc) throws Exception {
		form = new Form();
		add(form);
		
		User currentUser = iwc.isLoggedOn() ? iwc.getCurrentUser() : null;
		if (currentUser == null) {
			Logger.getLogger(UserMailingLists.class.getName()).warning("User must be logged in order to see and manage mailing lists");
			return;
		}
		
		//	Mailing lists user is subscribed to
		Collection<MailingList> userMailingLists = mailingListManager.getAllMailingListsForUser(currentUser);
		addLists(iwc, userMailingLists, iwrb.getLocalizedString("ml.my_lists", "My lists"), iwrb.getLocalizedString("ml.unsubscribe", "Un-subscribe"),
				iwrb.getLocalizedString("ml.unsubscribe_by_checking_box", "Check this box if want to un-subscribe from this mailing list"),
				PARAMETER_UN_SUBSCRIBE_FROM_MAILING_LIST);
	
		//	Mailing lists user is waiting to be confirmed to
		Collection<MailingList> waitingLists = mailingListManager.getAllMailingListsUserIsWaitingToBeConfirmed(currentUser);
		addLists(iwc, waitingLists, iwrb.getLocalizedString("ml.my_waiting_lists", "Lists I must be confirmed to"),
				iwrb.getLocalizedString("ml.take_back_subscribtion", "Take back"),
				iwrb.getLocalizedString("ml.take_back_subscribtion_by_checking_box", "Check this box if want to take back subscribtion"),
				PARAMETER_TAKE_BACK_SUBSCRIBTION_FROM_MAILING_LIST);
		
		//	Mailing lists user is not involved into
		Collection<MailingList> listsUserIsNotInvolved = mailingListManager.getAllMailingListsUserIsNotInvolved(currentUser);
		addLists(iwc, listsUserIsNotInvolved, iwrb.getLocalizedString("ml.other_mailing_lists", "Other mailing lists"),
				iwrb.getLocalizedString("ml.subscribe", "Subscribe"),
				iwrb.getLocalizedString("ml.subscribe_by_checking_box", "Check this box if want to subscribe to this mailing list"),
				PARAMETER_SUBSCRIBE_TO_MAILING_LIST);
		
		if (!ListUtil.isEmpty(userMailingLists) || !ListUtil.isEmpty(waitingLists) || !ListUtil.isEmpty(listsUserIsNotInvolved)) {
			Layer buttons = new Layer();
			form.add(buttons);
			SubmitButton save = new SubmitButton(iwrb.getLocalizedString("ml.save", "Save"), PARAMETER_SAVE_SETTINGS, Boolean.TRUE.toString());
			buttons.add(save);
		}
	}
	
	private void addLists(IWContext iwc, Collection<MailingList> lists, String label, String checkBoxLabel, String checkBoxTitle, String checkBoxParameter) {
		if (ListUtil.isEmpty(lists)) {
			return;
		}
		
		Layer container = new Layer();
		form.add(container);
		
		Layer header = new Layer();
		container.add(header);
		header.add(new Heading3(label));
		
		Layer listsContainer = new Layer();
		container.add(listsContainer);
		
		Table2 table = new Table2();
		listsContainer.add(table);
		TableHeaderRowGroup headerRows = table.createHeaderRowGroup();
		TableRow headerRow = headerRows.createRow();
		addCell(headerRow, checkBoxLabel);
		addCell(headerRow, iwrb.getLocalizedString("ml.name", "Name"));
				
		TableBodyRowGroup bodyRows = table.createBodyRowGroup();
		for (MailingList mailingList: lists) {
			TableRow bodyRow = bodyRows.createRow();
			String uniqueId = mailingList.getUniqueId();
			if (StringUtil.isEmpty(uniqueId)) {
				continue;
			}
			
			CheckBox select = new CheckBox(checkBoxParameter, uniqueId);
			select.setTitle(checkBoxTitle);
			addCell(bodyRow, select);
			
			UIComponent component = null;
			if (PARAMETER_UN_SUBSCRIBE_FROM_MAILING_LIST.equals(checkBoxParameter)) {
				component = getLink(mailingList.getName(), getUriToMailingListViewer(iwc), CoreConstants.EMPTY,
						new AdvancedProperty(MailingListViewer.PARAMETER_MAILING_LIST_UNIQUE_ID, uniqueId)
				);
			} else {
				component = new Text(mailingList.getName());
			}
			addCell(bodyRow, component);
		}
	}
}
