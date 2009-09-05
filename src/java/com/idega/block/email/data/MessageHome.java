package com.idega.block.email.data;

import javax.ejb.CreateException;
import javax.ejb.FinderException;

import com.idega.data.IDOHome;

public interface MessageHome extends IDOHome {
	
	public Message create() throws CreateException;
	
	public Message findByPrimaryKey(Object key) throws FinderException;
		
	public Message findByUniqueId(String uniqueId) throws FinderException;
	
}
