package com.idega.block.email.data;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Collection;

import com.idega.core.file.data.ICFile;
import com.idega.data.IDOAddRelationshipException;
import com.idega.data.IDOEntity;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.data.TreeableEntity;
import com.idega.data.UniqueIDCapable;

public interface Message extends IDOEntity, TreeableEntity, UniqueIDCapable {
	
	public String getSubject();
	public void setSubject(String subject);
	
	public InputStream getMessageContent();
	public void setMessageContent(InputStream content);
	
	public Timestamp getReceived();
	public void setReceived(Timestamp received);

	public boolean isDeleted();
	public void setDeleted(boolean deleted);
	
	public String getSenderAdress();
	public void setSenderAddress(String senderAddress);
	
	public Collection<ICFile> getAttachments();
	public void addAttachment(ICFile attachment) throws IDOAddRelationshipException;
	public void removeAttachment(ICFile attachment) throws IDORemoveRelationshipException;
}
