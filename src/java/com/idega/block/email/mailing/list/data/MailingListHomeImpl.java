package com.idega.block.email.mailing.list.data;

import java.util.Collection;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import com.idega.data.IDOEntity;
import com.idega.data.IDOFactory;
import com.idega.user.data.User;

public class MailingListHomeImpl extends IDOFactory implements MailingListHome {

	private static final long serialVersionUID = 8819592779128428123L;

	@Override
	protected Class<MailingList> getEntityInterfaceClass() {
		return MailingList.class;
	}

	public MailingList create() throws CreateException {
		return (MailingList) super.createIDO();
	}

	public MailingList findByPrimaryKey(Object key) throws FinderException {
		return (MailingList) super.findByPrimaryKeyIDO(key);
	}

	public MailingList findByName(String name) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Integer pk = ((MailingListBMPBean) entity).ejbFindByName(name);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}

	public MailingList findByUniqueId(String uniqueId) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Integer pk = ((MailingListBMPBean) entity).ejbFindByUniqueId(uniqueId);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}

	@SuppressWarnings("unchecked")
	public Collection<MailingList> findAll() throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Collection<Integer> ids = ((MailingListBMPBean) entity).ejbFindAll();
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKeyCollection(ids);
	}

	@SuppressWarnings("unchecked")
	public Collection<MailingList> findAllByUser(User user) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Collection<Integer> ids = ((MailingListBMPBean) entity).ejbFindAllByUser(user);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKeyCollection(ids);
	}

	@SuppressWarnings("unchecked")
	public Collection<MailingList> findAllUserIsWaitingToBeConfirmed(User user) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Collection<Integer> ids = ((MailingListBMPBean) entity).ejbFindAllUserIsWaitingToBeConfirmed(user);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKeyCollection(ids);
	}

	@SuppressWarnings("unchecked")
	public Collection<MailingList> findAllListsUserIsNotInvolved(User user) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Collection<Integer> ids = ((MailingListBMPBean) entity).ejbFindAllListsUserIsNotInvolved(user);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKeyCollection(ids);
	}

	@SuppressWarnings("unchecked")
	public Collection<MailingList> findMailingListsByIds(String[] ids) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Collection<Integer> IDs = ((MailingListBMPBean) entity).ejbFindMailingListsByIds(ids);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKeyCollection(IDs);
	}
	
	
}
