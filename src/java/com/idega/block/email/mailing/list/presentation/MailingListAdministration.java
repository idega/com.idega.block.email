package com.idega.block.email.mailing.list.presentation;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.faces.component.UIComponent;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.client.business.EmailDaemon;
import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.block.web2.presentation.JCaptchaImage;
import com.idega.builder.bean.AdvancedProperty;
import com.idega.business.IBOLookup;
import com.idega.presentation.IWContext;
import com.idega.presentation.Layer;
import com.idega.presentation.Table2;
import com.idega.presentation.TableBodyRowGroup;
import com.idega.presentation.TableHeaderRowGroup;
import com.idega.presentation.TableRow;
import com.idega.presentation.text.Heading2;
import com.idega.presentation.text.Heading3;
import com.idega.presentation.text.Link;
import com.idega.presentation.text.Text;
import com.idega.presentation.ui.BackButton;
import com.idega.presentation.ui.Form;
import com.idega.presentation.ui.GenericButton;
import com.idega.presentation.ui.InterfaceObject;
import com.idega.presentation.ui.Label;
import com.idega.presentation.ui.RadioGroup;
import com.idega.presentation.ui.SubmitButton;
import com.idega.presentation.ui.TextInput;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.user.presentation.user.UsersFilter;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;

public class MailingListAdministration extends BasicMailingList {

	private static final String PARAMETER_ACTION = "mlngLstActn";
	private static final String PARAMETER_SAVE_ACTION = "mlngLstSaveAction";
	private static final String PARAMETER_DISABLE_ACTION = "mlngLstDisableAction";
	private static final String PARAMETER_ENABLE_ACTION = "mlngLstEnableAction";
	private static final String PARAMETER_DELETE_ACTION = "mlngLstDeleteAction";

	private static final String PARAMETER_MAILING_LIST_ID = "mlngLstId";
	private static final String PARAMETER_NAME = "mlngLstName";
	private static final String PARAMETER_PRIVATE_OR_NOT = "mlngLstPrivateOrNot";
	private static final String PARAMETER_MAILING_LIST_SUBSCRIBERS = "mlngLstSubscribers";
	private static final String PARAMETER_MAILING_LIST_VALID_SENDERS = "mlngLstValidSenders";
	private static final String PARAMETER_MAILING_LIST_WAITING_USERS = "mlngLstWaitingUsersConfirmed";
	private static final String PARAMETER_SENDER_EMAIL = "mlngLstSenderEmailAddress";
	private static final String PARAMETER_SENDER_NAME = "mlngLstSenderName";
	private static final String PARAMETER_VALIDATION_IMAGE_VALUE = "mlngLstValidationImageValue";

	private static final int EDIT_ACTION = 1;
	private static final int CREATE_MAILING_LIST_ACTION = 2;

	private int forcedAction;

	private List<String> errorMessages;
	private List<String> successMessages;

	private Form form = null;

	@Override
	public void present(IWContext iwc) throws Exception {
		form = new Form();
		add(form);

		if (!ListUtil.isEmpty(successMessages)) {
			Layer successMessages = new Layer();
			successMessages.setStyleClass("successMessages");
			form.add(successMessages);
			for (String message: this.successMessages) {
				successMessages.add(new Heading2(message));
			}
		}

		if (!ListUtil.isEmpty(errorMessages)) {
			Layer errorMessage = new Layer();
			errorMessage.setStyleClass("errorMessages");
			form.add(errorMessage);
			for (String message: errorMessages) {
				errorMessage.add(new Heading2(message));
			}
		}

		switch(resolveAction(iwc)) {
			case EDIT_ACTION:
				editMailingList(iwc);
				break;
			case CREATE_MAILING_LIST_ACTION:
				createNewMailingList(iwc);
				break;
			default:
				listMailingLists(iwc);
				break;
		}
	}

	@Override
	protected void doBusiness(IWContext iwc) {
		if (iwc.isParameterSet(PARAMETER_SAVE_ACTION)) {
			boolean editingNewMailingList = iwc.isParameterSet(PARAMETER_MAILING_LIST_ID);

			String validationText = iwc.getParameter(PARAMETER_VALIDATION_IMAGE_VALUE);
			if (!web2.validateJCaptcha(iwc.getSessionId(), validationText)) {
				addErrorMessage(editingNewMailingList ?
						iwrb.getLocalizedString("ml.editing_ml_validation_failed", "Mailing list was not updated - validation failed!") :
						iwrb.getLocalizedString("ml.new_ml_validation_failed", "Mailing list was not created - validation failed!"));
				forcedAction = EDIT_ACTION;
				return;
			}

			String name = iwc.getParameter(PARAMETER_NAME);
			String senderEmail = iwc.getParameter(PARAMETER_SENDER_EMAIL);
			String senderName = iwc.getParameter(PARAMETER_SENDER_NAME);
			boolean isPrivate = iwc.isParameterSet(PARAMETER_PRIVATE_OR_NOT) ? Boolean.valueOf(iwc.getParameter(PARAMETER_PRIVATE_OR_NOT)) : false;
			Collection<User> subscribers = getUsers(iwc.getParameterValues(PARAMETER_MAILING_LIST_SUBSCRIBERS));
			Collection<User> confirmedFromWaitingList = getUsers(iwc.getParameterValues(PARAMETER_MAILING_LIST_WAITING_USERS));
			Collection<User> senders = getUsers(iwc.getParameterValues(PARAMETER_MAILING_LIST_VALID_SENDERS));

			if (editingNewMailingList) {
				//	Editing
				MailingList mailingList = mailingListManager.getMailingListByUniqueId(iwc.getParameter(PARAMETER_MAILING_LIST_ID));
				if (mailingList == null) {
					addErrorMessage(iwrb.getLocalizedString("ml.mailing_list_was_not_found", "Mailing list was not found"));
				} else {
					if (mailingListManager.editMailingList(mailingList, name, senderEmail, senderName, isPrivate, subscribers, confirmedFromWaitingList,
							senders)) {
						addSuccessMessage(iwrb.getLocalizedString("ml.success_editing_mailing_list", "Mailing list was successfully updated"));
					} else {
						addErrorMessage(iwrb.getLocalizedString("ml.error_editing_mailing_list", "Some error occurred editing mailing list"));
					}
				}
			} else {
				//	Creating new
				MailingList newMailingList = mailingListManager.createMailingList(name, senderName, senderEmail, isPrivate, subscribers, senders);
				if (newMailingList == null) {
					addErrorMessage(iwrb.getLocalizedString("ml.error_creating", "Mailing list was not created - some error occurred"));
				} else {
					addSuccessMessage(iwrb.getLocalizedString("ml.success_creating", "New mailing list was created"));
				}
			}
		} else if (iwc.isParameterSet(PARAMETER_DISABLE_ACTION)) {
			MailingList mailingList = mailingListManager.getMailingListByUniqueId(iwc.getParameter(PARAMETER_MAILING_LIST_ID));
			if (mailingList == null) {
				addErrorMessage(iwrb.getLocalizedString("ml.error_disabling", "Mailing list was not disabled - some error occurred"));
			} else {
				mailingList.setDeleted(Boolean.TRUE);
				mailingList.store();

				addSuccessMessage(iwrb.getLocalizedString("ml.success_disabling", "Mailing list was successfully disabled"));
			}
		} else if (iwc.isParameterSet(PARAMETER_ENABLE_ACTION)) {
			MailingList mailingList = mailingListManager.getMailingListByUniqueId(iwc.getParameter(PARAMETER_MAILING_LIST_ID));
			if (mailingList == null) {
				addErrorMessage(iwrb.getLocalizedString("ml.error_enabling", "Mailing list was not enabled - some error occurred"));
			} else {
				mailingList.setDeleted(Boolean.FALSE);
				mailingList.store();

				addSuccessMessage(iwrb.getLocalizedString("ml.success_enabling", "Mailing list was successfully enabled"));
			}
		} else if (iwc.isParameterSet(PARAMETER_DELETE_ACTION)) {
			MailingList mailingList = mailingListManager.getMailingListByUniqueId(iwc.getParameter(PARAMETER_MAILING_LIST_ID));
			if (mailingList == null || !mailingListManager.deleteMailingList(mailingList)) {
				addErrorMessage(iwrb.getLocalizedString("ml.error_deleting", "Mailing list was not deleted - some error occurred"));
			} else {
				addSuccessMessage(iwrb.getLocalizedString("ml.success_deleting", "Mailing list was deleted"));
			}
		}
	}

	private Collection<User> getUsers(String[] ids) {
		if (ArrayUtil.isEmpty(ids)) {
			return null;
		}

		UserBusiness userBusiness = null;
		try {
			userBusiness = IBOLookup.getServiceInstance(getIWApplicationContext(), UserBusiness.class);
		} catch(Exception e) {
			e.printStackTrace();
		}
		if (userBusiness == null) {
			return null;
		}

		Collection<User> users = null;
		try {
			users = userBusiness.getUsers(ids);
		} catch (EJBException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		return users;
	}

	private void addErrorMessage(String message) {
		if (errorMessages == null) {
			errorMessages = new ArrayList<String>();
		}
		errorMessages.add(message);
	}

	private void addSuccessMessage(String message) {
		if (successMessages == null) {
			successMessages = new ArrayList<String>();
		}
		successMessages.add(message);
	}

	private void listMailingLists(IWContext iwc) {
		Collection<MailingList> lists = mailingListManager.getAllMailingLists();

		Layer container = new Layer();
		form.add(container);

		if (ListUtil.isEmpty(lists)) {
			container.add(new Heading3(iwrb.getLocalizedString("ml.there_are_no_mailing_lists", "There are no mailing lists yet")));
		} else {
			String uriToMailingListViewerPage = getUriToMailingListViewer(iwc);

			String confirmMessage = iwrb.getLocalizedString("ml.are_you_sure", "Are you sure?");

			Table2 table = new Table2();
			container.add(table);
			TableHeaderRowGroup headerRows = table.createHeaderRowGroup();
			TableRow headerRow = headerRows.createRow();
			addCell(headerRow, iwrb.getLocalizedString("nr", "Nr."));
			addCell(headerRow, iwrb.getLocalizedString("ml.name", "Name"));
			addCell(headerRow, iwrb.getLocalizedString("ml.private", "Private"));
			addCell(headerRow, iwrb.getLocalizedString("ml.edit", "Edit"));
			addCell(headerRow, iwrb.getLocalizedString("ml.disable_enable", "Disable/enable"));
			addCell(headerRow, iwrb.getLocalizedString("ml.delete", "Delete"));
			addCell(headerRow, iwrb.getLocalizedString("ml.create_new_message", "Create new message"));

			int index = 1;
			TableBodyRowGroup bodyRows = table.createBodyRowGroup();
			for (MailingList mailingList: lists) {
				TableRow bodyRow = bodyRows.createRow();
				String uniqueId = mailingList.getUniqueId();
				if (StringUtil.isEmpty(uniqueId)) {
					continue;
				}

				addCell(bodyRow, String.valueOf(index));
				addCell(bodyRow, StringUtil.isEmpty(uriToMailingListViewerPage) ?
						getLink(mailingList.getName(),
								new AdvancedProperty(PARAMETER_ACTION, String.valueOf(EDIT_ACTION)),
								new AdvancedProperty(PARAMETER_MAILING_LIST_ID, uniqueId)
						) :
						getLink(mailingList.getName(), uriToMailingListViewerPage, CoreConstants.EMPTY,
								new AdvancedProperty(MailingListViewer.PARAMETER_MAILING_LIST_UNIQUE_ID, uniqueId)
				));
				addCell(bodyRow, mailingList.isPrivate() ? iwrb.getLocalizedString("yes", "Yes") : iwrb.getLocalizedString("no", "No"));
				addCell(bodyRow, getLink(iwrb.getLocalizedString("edit", "Edit"), bundle.getVirtualPathWithFileNameString("images/edit.png"),
						new AdvancedProperty(PARAMETER_ACTION, String.valueOf(EDIT_ACTION)),
						new AdvancedProperty(PARAMETER_MAILING_LIST_ID, uniqueId)
				));

				boolean deleted = mailingList.isDeleted();
				if (deleted) {
					addCell(bodyRow, getLink(iwrb.getLocalizedString("ml.enable", "Enable"),
							bundle.getVirtualPathWithFileNameString("images/enable.png"),
							new AdvancedProperty(PARAMETER_ENABLE_ACTION, Boolean.TRUE.toString()),
							new AdvancedProperty(PARAMETER_MAILING_LIST_ID, uniqueId)
					));
				} else {
					addCell(bodyRow, getLink(iwrb.getLocalizedString("disable", "Disable"), bundle.getVirtualPathWithFileNameString("images/disable.png"),
							new AdvancedProperty(PARAMETER_DISABLE_ACTION, Boolean.TRUE.toString()),
							new AdvancedProperty(PARAMETER_MAILING_LIST_ID, uniqueId)
					));

				}

				Link removeMailingList = getLink(iwrb.getLocalizedString("ml.delete", "Delete"), bundle.getVirtualPathWithFileNameString("images/delete.png"),
						new AdvancedProperty(PARAMETER_DELETE_ACTION, Boolean.TRUE.toString()),
						new AdvancedProperty(PARAMETER_MAILING_LIST_ID, uniqueId)
				);
				String uri = removeMailingList.getFinalUrl(iwc);
				removeMailingList.setURL("javascript:void(0);");
				removeMailingList.setOnClick(new StringBuilder(300).append("MailingListHelper.confirmMailingListToBeDeleted('").append(confirmMessage)
						.append("', '").append(uri).append("');").toString());
				addCell(bodyRow, removeMailingList);

				if (deleted) {
					addCell(bodyRow, iwrb.getLocalizedString("ml.unable_to_create_new_message_for_disabled_mailing_list", "Impossible"));
				} else {
					UIComponent newMessageUI = getNewMessageButton(iwc, mailingList, Link.class, iwrb.getLocalizedString("ml.new_message", "New message"));
					newMessageUI = newMessageUI == null ?
							new Text(iwrb.getLocalizedString("ml.unable_to_create_new_message_for_mailing_list", "Impossible")) : newMessageUI;
					addCell(bodyRow, newMessageUI);
				}

				index++;
			}
		}

		Layer buttons = new Layer();
		form.add(buttons);
		SubmitButton createNew = new SubmitButton(iwrb.getLocalizedString("ml.create_new_mailing_list", "Create new mailing list"), PARAMETER_ACTION,
				String.valueOf(CREATE_MAILING_LIST_ACTION));
		buttons.add(createNew);
	}

	private Layer getFormItem(String label, String inputName, String value) {
		return getFormItem(label, inputName, value, false);
	}

	private Layer getFormItem(String label, String inputName, String value, boolean disabled) {
		TextInput input = new TextInput(inputName);
		if (!StringUtil.isEmpty(value)) {
			input.setContent(value);
		}
		if (disabled) {
			input.setDisabled(Boolean.TRUE);
		}
		return getFormItem(label, input);
	}

	private Layer getFormItem(String label, InterfaceObject uiObject) {
		return getFormItem(label, (UIComponent) uiObject);
	}

	private Layer getFormItem(String label, UIComponent uiComponent) {
		Layer formItem = new Layer();
		formItem.setStyleClass("formItem");

		Label labelUI = new Label();
		labelUI.setLabel(label);
		labelUI.setFor(uiComponent.getId());

		formItem.add(labelUI);
		formItem.add(uiComponent);
		if (uiComponent instanceof JCaptchaImage) {
			Layer textContainer = new Layer();
			formItem.add(textContainer);
			textContainer.setStyleClass("validationTextInputContainer");
			TextInput validationInput = new TextInput(PARAMETER_VALIDATION_IMAGE_VALUE);
			validationInput.setStyleClass("validationTextInput");
			validationInput.setOnKeyUp(new StringBuilder(100).append("MailingListHelper.convertToCapitalLetters('").append(validationInput.getId())
					.append("');").toString());
			textContainer.add(validationInput);
			labelUI.setFor(validationInput.getId());
		}

		return formItem;
	}

	private Layer getBasicMailingListForm(IWContext iwc, MailingList mailingList) {
		Layer container = new Layer();
		container.setStyleClass("formSection");

		//	Name
		container.add(getFormItem(iwrb.getLocalizedString("ml.name", "Name"), PARAMETER_NAME, mailingList == null ? CoreConstants.EMPTY : mailingList.getName()));

		String nameInLatinLetters = mailingList == null ? null : mailingList.getNameInLatinLetters();
		if (!StringUtil.isEmpty(nameInLatinLetters)) {
			container.add(getFormItem(iwrb.getLocalizedString("ml.name_in_latin_letters", "Name in Latin letters"), "mlngLstNameInLatin", nameInLatinLetters,
					Boolean.TRUE));
		}

		//	Sender e-mail's address
		container.add(getFormItem(iwrb.getLocalizedString("ml.sender_email", "Sender e-mail's address"), PARAMETER_SENDER_EMAIL,
				mailingList == null ? CoreConstants.EMPTY : mailingList.getSenderAddress()));

		//	Sender's name
		container.add(getFormItem(iwrb.getLocalizedString("ml.sender_name", "Sender's name"), PARAMETER_SENDER_NAME,
				mailingList == null ? CoreConstants.EMPTY : mailingList.getSenderName()));

		//	Private or not
		RadioGroup privateOrNot = new RadioGroup(PARAMETER_PRIVATE_OR_NOT);
		privateOrNot.addRadioButton(Boolean.FALSE.toString(), new Text(iwrb.getLocalizedString("no", "No")),
				mailingList == null ? false : !mailingList.isPrivate());
		privateOrNot.addRadioButton(Boolean.TRUE.toString(), new Text(iwrb.getLocalizedString("yes", "Yes")),
				mailingList == null ? false : mailingList.isPrivate());
		container.add(getFormItem(iwrb.getLocalizedString("ml.private", "Private"), privateOrNot));

		//	Confirmation image
		JCaptchaImage validationImage = new JCaptchaImage();
		validationImage.setStyleClass("validationImage");
		container.add(getFormItem(iwrb.getLocalizedString("ml.validation_text", "Validation text"), validationImage));

		return container;
	}

	private void createNewMailingList(IWContext iwc) {
		Layer formSection = getBasicMailingListForm(iwc, null);
		form.add(formSection);

		formSection.add(getUsersFilterContainer(null, PARAMETER_MAILING_LIST_VALID_SENDERS,
				new StringBuilder(iwrb.getLocalizedString("ml.set_senders", "Set senders")).append(CoreConstants.SPACE)
				.append(iwrb.getLocalizedString("ml_senders_warning", "WARNING: if no senders are set when ANYBODY can send mails to mailing list!"))
				.toString(), true));
		formSection.add(getUsersFilterContainer(null, PARAMETER_MAILING_LIST_SUBSCRIBERS, iwrb.getLocalizedString("ml.set_subscribers", "Set subscribers"), true));

		Layer buttons = new Layer();
		form.add(buttons);
		BackButton back = new BackButton(iwrb.getLocalizedString("back", "Back"));
		buttons.add(back);
		SubmitButton save = new SubmitButton(iwrb.getLocalizedString("save", "Save"), PARAMETER_SAVE_ACTION, Boolean.TRUE.toString());
		buttons.add(save);
	}

	private List<String> getSubscribersIds(Collection<User> subscribers) {
		if (ListUtil.isEmpty(subscribers)) {
			return null;
		}

		List<String> ids = new ArrayList<String>(subscribers.size());
		for (User subscriber: subscribers) {
			ids.add(subscriber.getId());
		}

		return ids;
	}

	private void editMailingList(IWContext iwc) {
		MailingList mailingList = mailingListManager.getMailingListByUniqueId(iwc.getParameter(PARAMETER_MAILING_LIST_ID));
		if (mailingList == null) {
			form.add(new Heading2(iwrb.getLocalizedString("ml.mailing_list_was_not_found", "Mailing list was not found")));
			return;
		}

		form.addParameter(PARAMETER_MAILING_LIST_ID, "-1");

		Layer container = getBasicMailingListForm(iwc, mailingList);
		form.add(container);

		//	Senders
		container.add(getUsersFilterContainer(mailingList.getSenders(), PARAMETER_MAILING_LIST_VALID_SENDERS,
				new StringBuilder(iwrb.getLocalizedString("ml.set_senders", "Set senders")).append(CoreConstants.SPACE)
				.append(iwrb.getLocalizedString("ml_senders_warning", "WARNING: if no senders are set when ANYBODY can send mails to mailing list!")).toString(),
				true));

		//	Subscribers
		container.add(getUsersFilterContainer(mailingList.getSubscribers(), PARAMETER_MAILING_LIST_SUBSCRIBERS,
				iwrb.getLocalizedString("ml.set_subscribers", "Set subscribers"), true));

		//	Waiting list
		if (mailingList.isPrivate()) {
			container.add(getUsersFilterContainer(mailingList.getWaitingList(), PARAMETER_MAILING_LIST_WAITING_USERS,
					iwrb.getLocalizedString("ml.confirm_waiting_users", "Confirm users from waiting list as subscribers"), false));
		}

		Layer buttons = new Layer();
		form.add(buttons);
		BackButton back = new BackButton(iwrb.getLocalizedString("back", "Back"));
		buttons.add(back);
		SubmitButton save = new SubmitButton(iwrb.getLocalizedString("save", "Save"), PARAMETER_SAVE_ACTION, Boolean.TRUE.toString());
		buttons.add(save);
		save.setValueOnClick(PARAMETER_MAILING_LIST_ID, mailingList.getUniqueId());
		UIComponent newMessage = getNewMessageButton(iwc, mailingList, GenericButton.class, iwrb.getLocalizedString("ml.new_message", "New message"));
		if (newMessage != null) {
			buttons.add(newMessage);
		}
	}

	private UIComponent getNewMessageButton(IWContext iwc, MailingList mailingList, Class<? extends UIComponent> componentType, String label) {
		UIComponent component = null;
		try {
			component = (UIComponent) Class.forName(componentType.getName()).newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if (component == null) {
			return null;
		}

		String mailToMailingList = iwc.getApplicationSettings().getProperty(EmailConstants.MAILING_LIST_MESSAGE_RECEIVER);
		if (StringUtil.isEmpty(mailToMailingList)) {
			Logger.getLogger(MailingListAdministration.class.getName()).warning("There is no mailing list message receiver (e-mail address) defined! Check " +
					EmailDaemon.class + " what settings are needed");
			return null;
		}
		String nameInLatinLetters = mailingList.getNameInLatinLetters();
		if (StringUtil.isEmpty(nameInLatinLetters)) {
			Logger.getLogger(MailingListAdministration.class.getName()).warning("Mailing list '" + mailingList.getName() +
					"' doesn't have name in Latin letters. Please update mailing list!");
			return null;
		}

		String mailTo = new StringBuilder("mailto:").append(mailToMailingList).append("?subject=[").append(nameInLatinLetters)
			.append(EmailConstants.IW_MAILING_LIST).append("]").toString();

		if (component instanceof GenericButton) {
			((GenericButton) component).setContent(label);
			((GenericButton) component).setOnClick(new StringBuilder("window.location.href='").append(mailTo).append("';").toString());
		} else if (component instanceof Link) {
			((Link) component).setText(label);
			((Link) component).setURL(mailTo);
		}

		return component;
	}

	private Layer getUsersFilterContainer(Collection<User> users, String inputName, String label, boolean showGroupChooser) {
		Layer usersFilterContainer = new Layer();
		usersFilterContainer.setStyleClass("formItem");

		UsersFilter usersFilter = new UsersFilter();
		usersFilter.setAddLabel(false);
		usersFilter.setShowGroupChooser(showGroupChooser);
		usersFilter.setSelectedUsers(getSubscribersIds(users));
		usersFilter.setSelectedUserInputName(inputName);
		Label usersFilterLabel = new Label(label, usersFilter);
		usersFilterContainer.add(usersFilterLabel);
		usersFilterContainer.add(usersFilter);

		return usersFilterContainer;
	}

	private int resolveAction(IWContext iwc) {
		if (iwc.isParameterSet(PARAMETER_ACTION)) {
			try {
				return Integer.valueOf(iwc.getParameter(PARAMETER_ACTION));
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
		}

		return forcedAction > 0 ? forcedAction : 0;
	}

	@Override
	public String getBundleIdentifier() {
		return EmailConstants.IW_BUNDLE_IDENTIFIER;
	}
}
