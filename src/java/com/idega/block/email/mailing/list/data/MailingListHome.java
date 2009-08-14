package com.idega.block.email.mailing.list.data;

import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import com.idega.data.IDOHome;
import com.idega.user.data.User;

public interface MailingListHome extends IDOHome {

	public MailingList create() throws CreateException;
	
	public MailingList findByPrimaryKey(Object key) throws FinderException;
	
	public MailingList findByName(String name) throws FinderException;
	
	public MailingList findByUniqueId(String uniqueId) throws FinderException;
	
	public Collection<MailingList> findAll() throws FinderException;
	
	public Collection<MailingList> findAllByUser(User user) throws FinderException;
	
	public Collection<MailingList> findAllUserIsWaitingToBeConfirmed(User user) throws FinderException;
	
	public Collection<MailingList> findAllListsUserIsNotInvolved(User user) throws FinderException;
	
	public Collection<MailingList> findMailingListsByIds(String[] ids) throws FinderException;
}
