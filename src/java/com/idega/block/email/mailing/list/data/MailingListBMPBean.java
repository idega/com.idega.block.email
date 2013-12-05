package com.idega.block.email.mailing.list.data;

import java.util.Collection;

import javax.ejb.FinderException;

import com.idega.block.email.data.Message;
import com.idega.block.email.data.MessageBMPBean;
import com.idega.data.IDOAddRelationshipException;
import com.idega.data.IDORelationshipException;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.data.TreeableEntityBMPBean;
import com.idega.data.query.Column;
import com.idega.data.query.Criteria;
import com.idega.data.query.InCriteria;
import com.idega.data.query.MatchCriteria;
import com.idega.data.query.OR;
import com.idega.data.query.SelectQuery;
import com.idega.data.query.Table;
import com.idega.user.data.User;

public class MailingListBMPBean extends TreeableEntityBMPBean implements MailingList {

	private static final long serialVersionUID = -5748549253502076077L;

	public static final String TABLE_NAME = "MAILING_LIST";
	public static final String MAILING_LIST_SUBSCRIBERS = TABLE_NAME + "_SUBSCRIBERS";
	public static final String MAILING_LIST_WAITING = TABLE_NAME + "_WAITING";
	public static final String MAILING_LIST_SENDERS = TABLE_NAME + "_SENDERS";
	public static final String MAILING_LIST_MESSAGES = TABLE_NAME + MessageBMPBean.TABLE_NAME;

	private static final String NAME = "name";
	private static final String NAME_IN_LATIN_LETTERS = "name_in_latin_letters";
	private static final String PRIVATE_MAILING_LIST = "private";
	private static final String DELETED = "deleted";
	private static final String SENDER_ADDRESS = "sender_address";
	private static final String SENDER_NAME = "sender_name";

	@Override
	public String getEntityName() {
		return TABLE_NAME;
	}

	@Override
	public void initializeAttributes() {
		addAttribute(getIDColumnName());

		addAttribute(NAME, "Name", true, true, String.class);
		addAttribute(NAME_IN_LATIN_LETTERS, "Name in Lating letter", true, true, String.class);
		addAttribute(SENDER_ADDRESS, "Sender address", true, true, String.class);
		addAttribute(SENDER_NAME, "Sender name", true, true, String.class);
		addAttribute(PRIVATE_MAILING_LIST, "Mailing list is private", true, true, Boolean.class);
		addAttribute(DELETED, "Deleted", true, true, Boolean.class);

		addUniqueIDColumn();
		addIndex(getUniqueIdColumnName());

		addManyToManyRelationShip(User.class, MAILING_LIST_SUBSCRIBERS);
		addManyToManyRelationShip(User.class, MAILING_LIST_WAITING);
		addManyToManyRelationShip(User.class, MAILING_LIST_SENDERS);
		addManyToManyRelationShip(Message.class, MAILING_LIST_MESSAGES);
	}

	@Override
	public Collection<User> getSubscribers() {
		try {
			return this.idoGetRelatedEntitiesBySQL(User.class, "select subscribers.ic_user_id from " + MAILING_LIST_SUBSCRIBERS + " subscribers, " +
					TABLE_NAME + " mailing_lists where subscribers." + getIDColumnName() + " = mailing_lists." + getIDColumnName() +
					" and mailing_lists." + getIDColumnName() + " = " + getId());
		} catch (IDORelationshipException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void addSubscriber(User subscriber) throws IDOAddRelationshipException {
		this.idoAddTo(subscriber, MAILING_LIST_SUBSCRIBERS);
	}

	@Override
	public void removeSubscriber(User subscriber) throws IDORemoveRelationshipException {
		this.idoRemoveFrom(subscriber, MAILING_LIST_SUBSCRIBERS);
	}

	@Override
	public void addToWaitingList(User subscriber) throws IDOAddRelationshipException {
		this.idoAddTo(subscriber, MAILING_LIST_WAITING);
	}

	@Override
	public Collection<User> getWaitingList() {
		try {
			return this.idoGetRelatedEntitiesBySQL(User.class, "select waiting.ic_user_id from " + MAILING_LIST_WAITING + " waiting, " +
					TABLE_NAME + " mailing_lists where waiting." + getIDColumnName() + " = mailing_lists." + getIDColumnName() +
					" and mailing_lists." + getIDColumnName() + " = " + getId());
		} catch (IDORelationshipException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void removeFromWaitingList(User subscriber) throws IDORemoveRelationshipException {
		this.idoRemoveFrom(subscriber, MAILING_LIST_WAITING);
	}

	@Override
	public void addSender(User sender) throws IDOAddRelationshipException {
		this.idoAddTo(sender, MAILING_LIST_SENDERS);
	}

	@Override
	public Collection<User> getSenders() {
		try {
			return this.idoGetRelatedEntitiesBySQL(User.class, "select senders.ic_user_id from " + MAILING_LIST_SENDERS + " senders, " +
					TABLE_NAME + " mailing_lists where senders." + getIDColumnName() + " = mailing_lists." + getIDColumnName() +
					" and mailing_lists." + getIDColumnName() + " = " + getId());
		} catch (IDORelationshipException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void removeSender(User sender) throws IDORemoveRelationshipException {
		this.idoRemoveFrom(sender, MAILING_LIST_SENDERS);
	}

	@Override
	public String getName() {
		return getStringColumnValue(NAME);
	}

	@Override
	public void setName(String name) {
		setColumn(NAME, name);
	}

	@Override
	public boolean isPrivate() {
		return getBooleanColumnValue(PRIVATE_MAILING_LIST);
	}

	@Override
	public void setPrivate(boolean privateMailingList) {
		setColumn(PRIVATE_MAILING_LIST, privateMailingList);
	}

	private void addNotDeletedCriteria(Table table, SelectQuery query) {
		Criteria isNull = new MatchCriteria(new Column(table, DELETED), MatchCriteria.IS, MatchCriteria.NULL);
		Criteria notEquals = new MatchCriteria(new Column(table, DELETED), MatchCriteria.NOTEQUALS, true);
		query.addCriteria(new OR(isNull, notEquals));
	}

	public Integer ejbFindByName(String name) throws FinderException {
		Table table = new Table(this);
		SelectQuery query = new SelectQuery(table);
    	query.addColumn(new Column(table, getIDColumnName()));

    	query.addCriteria(new MatchCriteria(new Column(table, NAME), MatchCriteria.EQUALS, name));
    	addNotDeletedCriteria(table, query);

    	return (Integer) idoFindOnePKByQuery(query);
	}

	public Integer ejbFindByNameInLatinLetters(String nameInLatinLetters) throws FinderException {
		Table table = new Table(this);
		SelectQuery query = new SelectQuery(table);
    	query.addColumn(new Column(table, getIDColumnName()));

    	query.addCriteria(new MatchCriteria(new Column(table, NAME_IN_LATIN_LETTERS), MatchCriteria.EQUALS, nameInLatinLetters));
    	addNotDeletedCriteria(table, query);

    	return (Integer) idoFindOnePKByQuery(query);
	}

	public Integer ejbFindByUniqueId(String uniqueId) throws FinderException {
		Table table = new Table(this);
		SelectQuery query = new SelectQuery(table);
    	query.addColumn(new Column(table, getIDColumnName()));

    	query.addCriteria(new MatchCriteria(new Column(table, getUniqueIdColumnName()), MatchCriteria.EQUALS, uniqueId));

    	return (Integer) idoFindOnePKByQuery(query);
	}

	public Collection<Integer> ejbFindAll() throws FinderException {
		Table table = new Table(this);
		SelectQuery query = new SelectQuery(table);
    	query.addColumn(new Column(table, getIDColumnName()));
    	return idoFindPKsByQuery(query);
	}

	public Collection<Integer> ejbFindAllByUser(User user) throws FinderException {
		Table mailingLists = new Table(this);
		Table mailingListsSubscribers = new Table(MAILING_LIST_SUBSCRIBERS);

		SelectQuery query = new SelectQuery(mailingLists);
    	query.addColumn(new Column(mailingLists, getIDColumnName()));

		query.addJoin(mailingListsSubscribers, getIDColumnName(), mailingLists, getIDColumnName());

    	query.addCriteria(new MatchCriteria(mailingListsSubscribers, User.FIELD_USER_ID, MatchCriteria.EQUALS, user.getId()));

    	return idoFindPKsByQuery(query);
	}

	public Collection<Integer> ejbFindAllUserIsWaitingToBeConfirmed(User user) throws FinderException {
		Table mailingLists = new Table(this);
		Table waitingLists = new Table(MAILING_LIST_WAITING);

		SelectQuery query = new SelectQuery(mailingLists);
    	query.addColumn(new Column(mailingLists, getIDColumnName()));

		query.addJoin(waitingLists, getIDColumnName(), mailingLists, getIDColumnName());

    	query.addCriteria(new MatchCriteria(waitingLists, User.FIELD_USER_ID, MatchCriteria.EQUALS, user.getId()));

    	return idoFindPKsByQuery(query);
	}

	public Collection<Integer> ejbFindAllListsUserIsNotInvolved(User user) throws FinderException {
		String query = new StringBuilder("select ").append(getEntityName()).append(".").append(getIDColumnName()).append(" from ").append(getEntityName())
			.append(" where ").append(getEntityName()).append(".").append(getIDColumnName()).append(" not in (select ").append(MAILING_LIST_SUBSCRIBERS)
			.append(".").append(getIDColumnName()).append(" from ").append(MAILING_LIST_SUBSCRIBERS).append(" where ").append(MAILING_LIST_SUBSCRIBERS)
			.append(".").append(User.FIELD_USER_ID).append(" = ").append(user.getId()).append(") and ").append(getEntityName()).append(".")
			.append(getIDColumnName()).append(" not in (select ").append(MAILING_LIST_WAITING).append(".").append(getIDColumnName()).append(" from ")
			.append(MAILING_LIST_WAITING).append(" where ").append(MAILING_LIST_WAITING).append(".").append(User.FIELD_USER_ID).append(" = ")
			.append(user.getId()).append(")")
		.toString();

    	return idoFindPKsBySQL(query);
	}

	public Collection<Integer> ejbFindMailingListsByIds(String[] ids) throws FinderException {
		Table table = new Table(this);
		SelectQuery query = new SelectQuery(table);
    	query.addColumn(new Column(table, getIDColumnName()));

    	query.addCriteria(new InCriteria(new Column(table, getUniqueIdColumnName()), ids));

    	return idoFindPKsByQuery(query);
	}

	@Override
	public boolean isDeleted() {
		return getBooleanColumnValue(DELETED);
	}

	@Override
	public void setDeleted(boolean deleted) {
		setColumn(DELETED, deleted);
	}

	@Override
	public String getSenderAddress() {
		return (String) getColumnValue(SENDER_ADDRESS);
	}

	@Override
	public String getSenderName() {
		return (String) getColumnValue(SENDER_NAME);
	}

	@Override
	public void setSenderAddress(String senderAddress) {
		setColumn(SENDER_ADDRESS, senderAddress);
	}

	@Override
	public void setSenderName(String senderName) {
		setColumn(SENDER_NAME, senderName);
	}

	@Override
	public void addMessage(Message message) throws IDOAddRelationshipException {
		this.idoAddTo(message);
	}

	@Override
	public Collection<Message> getMessages() {
		try {
			return this.idoGetRelatedEntities(Message.class);
		} catch (IDORelationshipException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void removeMessage(Message message) throws IDORemoveRelationshipException {
		this.idoRemoveFrom(message, MAILING_LIST_MESSAGES);
	}

	@Override
	public String getNameInLatinLetters() {
		return (String) getColumnValue(NAME_IN_LATIN_LETTERS);
	}

	@Override
	public void setNameInLatinLetters(String nameInLatinLetters) {
		setColumn(NAME_IN_LATIN_LETTERS, nameInLatinLetters);
	}

}
