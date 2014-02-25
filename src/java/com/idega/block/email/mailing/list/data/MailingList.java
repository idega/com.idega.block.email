package com.idega.block.email.mailing.list.data;

import java.util.Collection;

import com.idega.block.email.data.Message;
import com.idega.data.IDOAddRelationshipException;
import com.idega.data.IDOEntity;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.data.MetaDataCapable;
import com.idega.data.TreeableEntity;
import com.idega.data.UniqueIDCapable;
import com.idega.user.data.User;

public interface MailingList extends IDOEntity, TreeableEntity<MailingList>, UniqueIDCapable, MetaDataCapable {

	public Collection<User> getSenders();
	public void addSender(User sender) throws IDOAddRelationshipException;
	public void removeSender(User sender) throws IDORemoveRelationshipException;

	public Collection<User> getSubscribers();
	public void addSubscriber(User subscriber) throws IDOAddRelationshipException;
	public void removeSubscriber(User subscriber) throws IDORemoveRelationshipException;

	public Collection<User> getWaitingList();
	public void addToWaitingList(User subscriber) throws IDOAddRelationshipException;
	public void removeFromWaitingList(User subscriber) throws IDORemoveRelationshipException;

	public String getName();
	public void setName(String name);

	public String getNameInLatinLetters();
	public void setNameInLatinLetters(String nameInLatinLetters);

	public boolean isPrivate();
	public void setPrivate(boolean privateMailingList);

	public boolean isDeleted();
	public void setDeleted(boolean deleted);

	public String getSenderAddress();
	public void setSenderAddress(String senderAddress);

	public String getSenderName();
	public void setSenderName(String senderName);

	public void addMessage(Message message) throws IDOAddRelationshipException;
	public void removeMessage(Message message) throws IDORemoveRelationshipException;
	public Collection<Message> getMessages();
}
