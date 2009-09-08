package com.idega.block.email.mailing.list.business;

import java.util.Collection;

import com.idega.block.email.mailing.list.data.MailingList;
import com.idega.user.data.User;

public interface MailingListManager {

	public MailingList createMailingList(String name);
	
	public boolean editMailingList(String uniqueId, String name, String senderEmail, String senderName, boolean isPrivate, Collection<User> subscribers,
			Collection<User> confirmedFromWaitingList, Collection<User> senders);
	public boolean editMailingList(MailingList mailingList, String name, String senderAddress, String senderName, boolean isPrivate, Collection<User> subscribers,
			Collection<User> confirmedFromWaitingList, Collection<User> senders);
	
	public MailingList getMailingListByUniqueId(String uniqueId);
	public MailingList getMailingListByNameInLatinLetters(String nameInLatinLetters);
	
	public boolean subscribeToMailingList(String uniqueId, String subscriberEmailAddress);
	public boolean subscribeToMailingLists(Collection<String> uniqueIds, String subscriberEmailAddress);
	public boolean subscribeToMailingList(String uniqueId, User subscriber);
	public boolean subscribeToMailingLists(Collection<String> uniqueIds, User subscriber);
	
	public boolean unsubscribeFromMailingList(String uniqueId, String subscriberEmailAddress);
	public boolean unsubscribeFromMailingLists(Collection<String> uniqueIds, String subscriberEmailAddress);
	public boolean unsubscribeFromMailingList(String uniqueId, User subscriber);
	public boolean unsubscribeFromMailingLists(Collection<String> uniqueIds, User subscriber);
	
	public Collection<MailingList> getAllMailingLists();
	
	public Collection<MailingList> getAllMailingListsForUser(User user);
	
	public Collection<MailingList> getAllMailingListsUserIsWaitingToBeConfirmed(User user);
	
	public Collection<MailingList> getAllMailingListsUserIsNotInvolved(User user);
	
	public Collection<MailingList> getMailingLists(String[] ids);
}
