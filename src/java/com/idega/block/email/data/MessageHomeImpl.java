package com.idega.block.email.data;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import com.idega.data.IDOEntity;
import com.idega.data.IDOFactory;

public class MessageHomeImpl extends IDOFactory implements MessageHome {

	private static final long serialVersionUID = -52023393359204171L;

	@Override
	protected Class<Message> getEntityInterfaceClass() {
		return Message.class;
	}

	public Message create() throws CreateException {
		return (Message) super.createIDO();
	}

	public Message findByPrimaryKey(Object key) throws FinderException {
		return (Message) super.findByPrimaryKeyIDO(key);
	}

	public Message findByUniqueId(String uniqueId) throws FinderException {
		IDOEntity entity = this.idoCheckOutPooledEntity();
		Integer pk = ((MessageBMPBean) entity).ejbFindByUniqueId(uniqueId);
		this.idoCheckInPooledEntity(entity);
		return this.findByPrimaryKey(pk);
	}

}
