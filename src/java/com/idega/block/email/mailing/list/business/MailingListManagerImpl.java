package com.idega.block.email.mailing.list.business;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.block.email.mailing.list.data.MailingListHome;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.data.IDOAddRelationshipException;
import com.idega.data.IDOLookup;
import com.idega.data.IDOLookupException;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.idegaweb.IWMainApplication;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class MailingListManagerImpl implements MailingListManager {

	private static final Logger LOGGER = Logger.getLogger(MailingListManagerImpl.class.getName());
	
	public MailingList createMailingList(String name) {
		if (StringUtil.isEmpty(name)) {
			LOGGER.warning("Name can not be empty!");
			return null;
		}
		
		MailingListHome mailingListHome = getMailingListHome();
		if (mailingListHome == null) {
			return null;
		}
		
		//	Creating new mailing list
		MailingList mailingList = null;
		try {
			mailingList = mailingListHome.create();
		} catch (CreateException e) {
			LOGGER.log(Level.WARNING, "Error creating mailing list: " + name, e);
		}
		if (mailingList == null) {
			return null;
		}
		
		mailingList.setName(name);
		String nameInLatinLetters = getNameInLatinLetters(name);
		if (StringUtil.isEmpty(nameInLatinLetters)) {
			LOGGER.warning("Unable to create mailing list because name in Latin letters was not generated for original name: " + name);
			return null;
		}
		
		mailingList.setNameInLatinLetters(nameInLatinLetters);
		mailingList.store();
		return mailingList;
	}
	
	private String getNameInLatinLetters(String name) {
		return getNameInLatinLetters(null, name);
	}
	
	private String getNameInLatinLetters(MailingList mailingList, String name) {
		return getNameInLatinLetters(mailingList, name, 0);
	}
	
	private String getNameInLatinLetters(MailingList mailingList, String name, int reTryId) {
		if (mailingList != null) {
			String nameInLatinLetters = mailingList.getNameInLatinLetters();
			if (!StringUtil.isEmpty(nameInLatinLetters)) {
				return nameInLatinLetters;
			}
		}
		
		if (StringUtil.isEmpty(name)) {
			LOGGER.warning("Can not generate name in Latin letters because provided name is empty!");
			return null;
		}
		
		String nameAsSource = name;
		if (reTryId > 0) {
			nameAsSource = nameAsSource + reTryId;
		}
		
		String nameInLatinLetters = StringHandler.stripNonRomanCharacters(nameAsSource, new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
		if (StringUtil.isEmpty(nameInLatinLetters)) {
			return null;
		}
		
		MailingList ml = getMailingListByNameInLatinLetters(nameInLatinLetters);
		if (ml != null) {
			if (mailingList != null && !(ml.getPrimaryKey().toString().equals(ml.getPrimaryKey().toString()))) {
				return getNameInLatinLetters(mailingList, mailingList.getName(), reTryId + 1);
			} else {
				return getNameInLatinLetters(mailingList, name, reTryId + 1);
			}
		}
		
		return nameInLatinLetters;
	}

	public MailingList getMailingListByUniqueId(String uniqueId) {
		if (StringUtil.isEmpty(uniqueId)) {
			LOGGER.warning("Unique ID is undefined!");
			return null;
		}
		
		MailingListHome mailingListHome = getMailingListHome();
		if (mailingListHome == null) {
			return null;
		}
		
		try {
			return mailingListHome.findByUniqueId(uniqueId);
		} catch (FinderException e) {
			LOGGER.warning("Mailing list was not found by unique ID: " + uniqueId);
		}
		
		return null;
	}

	public boolean subscribeToMailingList(String uniqueId, String subscriberEmailAddress) {
		return subscribeToMailingList(uniqueId, getUserByEmailAddress(subscriberEmailAddress));
	}

	public boolean subscribeToMailingList(String uniqueId, User subscriber) {
		if (subscriber == null) {
			return false;
		}
		
		MailingList mailingList = getMailingListByUniqueId(uniqueId);
		if (mailingList == null) {
			return false;
		}
		
		if (mailingList.isPrivate()) {
			Collection<User> waitingToBeConfirmed = mailingList.getWaitingList();
			if (waitingToBeConfirmed == null || !waitingToBeConfirmed.contains(subscriber)) {
				try {
					mailingList.addToWaitingList(subscriber);
					mailingList.store();
					return true;
				} catch (IDOAddRelationshipException e) {
					LOGGER.log(Level.WARNING, "Error adding " + subscriber + " to waiting list for mailing list: " + mailingList.getName(), e);
					return false;
				}
			}
			return true;
		} else {
			Collection<User> subscribers = mailingList.getSubscribers();
			if (subscribers == null || !subscribers.contains(subscriber)) {
				try {
					mailingList.addSubscriber(subscriber);
					mailingList.store();
					return true;
				} catch (IDOAddRelationshipException e) {
					LOGGER.log(Level.WARNING, "Error adding " + subscriber + " to mailing list: " + mailingList.getName(), e);
					return false;
				}
			}
			LOGGER.info("User " + subscriber + " is already subscribed to mailing list: " + mailingList.getName());
			return true;
		}
	}

	public boolean subscribeToMailingLists(Collection<String> uniqueIds, String subscriberEmailAddress) {
		return subscribeToMailingLists(uniqueIds, getUserByEmailAddress(subscriberEmailAddress));
	}

	public boolean subscribeToMailingLists(Collection<String> uniqueIds, User subscriber) {
		if (ListUtil.isEmpty(uniqueIds)) {
			LOGGER.warning("No IDs provided for mailin lists");
			return false;
		}
		
		for (String uniqueId : uniqueIds) {
			if (!subscribeToMailingList(uniqueId, subscriber)) {
				return false;
			}
		}
		
		return true;
	}

	public boolean unsubscribeFromMailingList(String uniqueId, String subscriberEmailAddress) {
		return unsubscribeFromMailingList(uniqueId, getUserByEmailAddress(subscriberEmailAddress));
	}

	public boolean unsubscribeFromMailingList(String uniqueId, User subscriber) {
		if (subscriber == null) {
			return false;
		}
		
		MailingList mailingList = getMailingListByUniqueId(uniqueId);
		if (mailingList == null) {
			return false;
		}
		
		try {
			mailingList.removeSubscriber(subscriber);
			mailingList.store();
			return true;
		} catch (IDORemoveRelationshipException e) {
			LOGGER.log(Level.WARNING, "Error while unsubscribing " + subscriber.getName() + " from mailing list: " + mailingList.getName(), e);
		}
		return false;
	}

	public boolean unsubscribeFromMailingLists(Collection<String> uniqueIds, String subscriberEmailAddress) {
		return unsubscribeFromMailingLists(uniqueIds, getUserByEmailAddress(subscriberEmailAddress));
	}

	public boolean unsubscribeFromMailingLists(Collection<String> uniqueIds, User subscriber) {
		if (subscriber == null) {
			return false;
		}
		
		if (ListUtil.isEmpty(uniqueIds)) {
			LOGGER.warning("No IDs provided for mailing lists");
			return false;
		}
		
		for (String uniqueId: uniqueIds) {
			if (!unsubscribeFromMailingList(uniqueId, subscriber)) {
				return false;
			}
		}
		
		return true;
	}
	
	private User getUserByEmailAddress(String emailAddress) {
		if (StringUtil.isEmpty(emailAddress)) {
			LOGGER.warning("Email address is not defined!");
			return null;
		}
		
		UserBusiness userBusiness = getUserBusiness();
		if (userBusiness == null) {
			return null;
		}
		
		Collection<User> users = userBusiness.getUsersByEmail(emailAddress);
		return ListUtil.isEmpty(users) ? null : users.iterator().next();
	}
	
	private MailingListHome getMailingListHome() {
		try {
			return (MailingListHome) IDOLookup.getHome(MailingList.class);
		} catch (IDOLookupException e) {
			LOGGER.log(Level.SEVERE, "Error getting " + MailingListHome.class, e);
		}
		return null;
	}

	private UserBusiness getUserBusiness() {
		try {
			return IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), UserBusiness.class);
		} catch (IBOLookupException e) {
			LOGGER.log(Level.SEVERE, "Error getting " + UserBusiness.class, e);
		}
		return null;
	}

	public Collection<MailingList> getAllMailingLists() {
		MailingListHome mailingListHome = getMailingListHome();
		if (mailingListHome == null) {
			return null;
		}
		
		try {
			return mailingListHome.findAll();
		} catch (FinderException e) {
			LOGGER.warning("No mailing lists found");
		}
		
		return null;
	}

	public boolean editMailingList(String uniqueId, String name, String senderEmail, String senderName, boolean isPrivate, Collection<User> subscribers,
			Collection<User> confirmedFromWaitingList, Collection<User> senders) {
		return editMailingList(getMailingListByUniqueId(uniqueId), name, senderEmail, senderName, isPrivate, subscribers, confirmedFromWaitingList, senders);
	}
	
	public boolean editMailingList(MailingList mailingList, String name, String senderEmail, String senderName, boolean isPrivate, Collection<User> subscribers,
			Collection<User> confirmedFromWaitingList, Collection<User> senders) {
		if (mailingList == null) {
			return false;
		}
		if (StringUtil.isEmpty(name)) {
			LOGGER.warning("Name must be provided!");
			return false;
		}
		
		mailingList.setName(name);
		String nameInLatinLetters = getNameInLatinLetters(mailingList, name);
		if (StringUtil.isEmpty(nameInLatinLetters)) {
			LOGGER.warning("Unable to modify mailing list because name in Latin letters was not generated for original name: " + name);
			return false;
		}
		
		mailingList.setNameInLatinLetters(nameInLatinLetters);
		mailingList.setSenderAddress(senderEmail);
		mailingList.setSenderName(senderName);
		mailingList.setPrivate(isPrivate);
		
		//	Subscribers
		Collection<User> currentSubscribers = mailingList.getSubscribers();
		Collection<User> newSubscribers = getUsersToAdd(currentSubscribers, subscribers);
		if (!addSubscribers(mailingList, newSubscribers)) {
			return false;
		}
		Collection<User> notSubscribers = getUsersToRemove(currentSubscribers, subscribers);
		if (!ListUtil.isEmpty(notSubscribers)) {
			for (User notSubscriber: notSubscribers) {
				try {
					mailingList.removeSubscriber(notSubscriber);
				} catch (IDORemoveRelationshipException e) {
					LOGGER.log(Level.WARNING, "Error removing subscriber ("+notSubscriber+") from mailing list: " + mailingList.getName(), e);
					return false;
				}
				
			}
		}
		
		//	Confirmed as subscribers
		if (!addSubscribers(mailingList, confirmedFromWaitingList)) {
			return false;
		}
		if (!ListUtil.isEmpty(confirmedFromWaitingList)) {
			for (User notInWaitingList: confirmedFromWaitingList) {
				try {
					mailingList.removeFromWaitingList(notInWaitingList);
				} catch (IDORemoveRelationshipException e) {
					LOGGER.log(Level.WARNING, "Error removing user ("+notInWaitingList+") from waiting list to be confirmed for: " + mailingList.getName(), e);
					return false;
				}
			}
		}
		
		//	Senders
		Collection<User> currentSenders = mailingList.getSenders();
		Collection<User> newSenders = getUsersToAdd(currentSenders, senders);
		if (!addSenders(mailingList, newSenders)) {
			return false;
		}
		Collection<User> notSenders = getUsersToRemove(currentSenders, senders);
		if (!ListUtil.isEmpty(notSenders)) {
			for (User notSender: notSenders) {
				try {
					mailingList.removeSender(notSender);
				} catch (IDORemoveRelationshipException e) {
					LOGGER.log(Level.WARNING, "Error removing sender ("+notSender+") from mailing list: " + mailingList.getName(), e);
					return false;
				}
			}
		}
		
		mailingList.store();
		
		return true;
	}
	
	private boolean addSenders(MailingList mailingList, Collection<User> senders) {
		if (ListUtil.isEmpty(senders)) {
			return true;
		}
		
		for (User sender: senders) {
			try {
				mailingList.addSender(sender);
			} catch (IDOAddRelationshipException e) {
				LOGGER.log(Level.WARNING, "Error adding sebder ("+sender+") to mailing list: " + mailingList.getName(), e);
				return false;
			}
		}
		
		return true;
	}
	
	private boolean addSubscribers(MailingList mailingList, Collection<User> subscribers) {
		if (ListUtil.isEmpty(subscribers)) {
			return true;
		}
		
		for (User newSubscriber: subscribers) {
			try {
				mailingList.addSubscriber(newSubscriber);
			} catch (IDOAddRelationshipException e) {
				LOGGER.log(Level.WARNING, "Error adding new subscriber ("+newSubscriber+") to mailing list: " + mailingList.getName(), e);
				return false;
			}
		}
		
		return true;
	}
	
	private Collection<User> getUsersToAdd(Collection<User> currentUsers, Collection<User> newUsers) {		
		if (ListUtil.isEmpty(currentUsers)) {
			return newUsers;
		}
		
		if (ListUtil.isEmpty(newUsers)) {
			return null;
		}
		
		Collection<User> usersToAdd = new ArrayList<User>();
		for (User newUser: newUsers) {
			if (!currentUsers.contains(newUser)) {
				usersToAdd.add(newUser);
			}
		}
		
		return usersToAdd;
	}
	
	private Collection<User> getUsersToRemove(Collection<User> currentUsers, Collection<User> newUsers) {		
		if (ListUtil.isEmpty(currentUsers)) {
			return null;
		}
		
		if (ListUtil.isEmpty(newUsers)) {
			return currentUsers;
		}
		
		Collection<User> usersToRemove = new ArrayList<User>();
		for (User currentUser: currentUsers) {
			if (!newUsers.contains(currentUser)) {
				usersToRemove.add(currentUser);
			}
		}
		
		return usersToRemove;
	}

	public Collection<MailingList> getAllMailingListsForUser(User user) {
		if (user == null) {
			LOGGER.warning("User is not provided!");
			return null;
		}
		
		MailingListHome mailingListHome = getMailingListHome();
		if (mailingListHome == null) {
			return null;
		}
		
		try {
			return mailingListHome.findAllByUser(user);
		} catch (FinderException e) {}
		
		return null;
	}

	public Collection<MailingList> getAllMailingListsUserIsWaitingToBeConfirmed(User user) {
		if (user == null) {
			LOGGER.warning("User is not provided!");
			return null;
		}
		
		MailingListHome mailingListHome = getMailingListHome();
		if (mailingListHome == null) {
			return null;
		}
		
		try {
			return mailingListHome.findAllUserIsWaitingToBeConfirmed(user);
		} catch (FinderException e) {}
		
		return null;
	}

	public Collection<MailingList> getAllMailingListsUserIsNotInvolved(User user) {
		if (user == null) {
			LOGGER.warning("User is not provided!");
			return null;
		}
		
		MailingListHome mailingListHome = getMailingListHome();
		if (mailingListHome == null) {
			return null;
		}
		
		try {
			return mailingListHome.findAllListsUserIsNotInvolved(user);
		} catch (FinderException e) {
		}
		
		return null;
	}

	public Collection<MailingList> getMailingLists(String[] ids) {
		if (ArrayUtil.isEmpty(ids)) {
			LOGGER.warning("User is not provided!");
			return null;
		}
		
		MailingListHome mailingListHome = getMailingListHome();
		if (mailingListHome == null) {
			return null;
		}
		
		try {
			return mailingListHome.findMailingListsByIds(ids);
		} catch (FinderException e) {}
		
		return null;
	}

	public MailingList getMailingListByNameInLatinLetters(String nameInLatinLetters) {
		if (StringUtil.isEmpty(nameInLatinLetters)) {
			LOGGER.warning("Name in Latin is empty!");
			return null;
		}
		
		try {
			return getMailingListHome().findByNameInLatinLetters(nameInLatinLetters);
		} catch (FinderException e) {
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error looking for mailing list by name in latin letters:" + nameInLatinLetters);
		}
		
		return null;
	}
	
}
