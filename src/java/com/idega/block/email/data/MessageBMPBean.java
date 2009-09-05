package com.idega.block.email.data;

import java.io.InputStream;
import java.sql.Timestamp;
import java.util.Collection;

import javax.ejb.FinderException;

import com.idega.core.file.data.ICFile;
import com.idega.data.BlobWrapper;
import com.idega.data.IDOAddRelationshipException;
import com.idega.data.IDORelationshipException;
import com.idega.data.IDORemoveRelationshipException;
import com.idega.data.IDOStoreException;
import com.idega.data.TreeableEntityBMPBean;
import com.idega.data.query.Column;
import com.idega.data.query.MatchCriteria;
import com.idega.data.query.SelectQuery;
import com.idega.data.query.Table;

@SuppressWarnings("unchecked")
public class MessageBMPBean extends TreeableEntityBMPBean implements Message {

	private static final long serialVersionUID = -3377437264415778974L;

	public static final String TABLE_NAME = "MAIL_MESSAGE";
	
	private static final String COLUMN_CONTENT = "CONTENT";
	private static final String COLUMN_RECEIVED = "RECEIVED";
	private static final String COLUMN_DELETED = "DELETED";
	private static final String COLUMN_SENDER = "SENDER";
	private static final String COLUMN_SUBJECT = "SUBJECT";
	
	private static final String MESSAGE_ATTACHMENTS = TABLE_NAME + "_ATTACHMENTS";
	
	@Override
	public String getEntityName() {
		return TABLE_NAME;
	}

	@Override
	public void initializeAttributes() {
		addAttribute(getIDColumnName());

		addAttribute(COLUMN_CONTENT, "Content", true, true, BlobWrapper.class);
		addAttribute(COLUMN_RECEIVED, "Received", true, true, Timestamp.class);
		addAttribute(COLUMN_DELETED, "Deleted", true, true, Boolean.class);
		addAttribute(COLUMN_SENDER, "From", true, true, String.class);
		addAttribute(COLUMN_SUBJECT, "Subject", true, true, String.class);
		
		addUniqueIDColumn();
		addIndex(getUniqueIdColumnName());
		
		addManyToManyRelationShip(ICFile.class, MESSAGE_ATTACHMENTS);
	}

	public InputStream getMessageContent() {
		return getInputStreamColumnValue(COLUMN_CONTENT);
	}
	
	public BlobWrapper getBlobWrapperFileValue() {
		return (BlobWrapper) getColumnValue(COLUMN_CONTENT);
	}
	
	public void setBlobWrapperFileValue(BlobWrapper fileValue) {
		setColumn(COLUMN_CONTENT, fileValue);
	}

	public void setMessageContent(InputStream content) {
		setColumn(COLUMN_CONTENT, content);		
	}

	public Timestamp getReceived() {
		return (Timestamp) getColumnValue(COLUMN_RECEIVED);
	}

	public void setReceived(Timestamp received) {
		setColumn(COLUMN_RECEIVED, received);
	}

	public boolean isDeleted() {
		return getBooleanColumnValue(COLUMN_DELETED);
	}

	public void setDeleted(boolean deleted) {
		setColumn(COLUMN_DELETED, deleted);
	}

	public Integer ejbFindByUniqueId(String uniqueId) throws FinderException {
		Table table = new Table(this);
		SelectQuery query = new SelectQuery(table);
    	query.addColumn(new Column(table, getIDColumnName()));
    	
    	query.addCriteria(new MatchCriteria(new Column(table, getUniqueIdColumnName()), MatchCriteria.EQUALS, uniqueId));
    	
    	return (Integer) idoFindOnePKByQuery(query);
	}

	public String getSenderAdress() {
		return (String) getColumnValue(COLUMN_SENDER);
	}

	public void setSenderAddress(String from) {
		setColumn(COLUMN_SENDER, from);
	}

	public String getSubject() {
		return (String) getColumnValue(COLUMN_SUBJECT);
	}

	public void setSubject(String subject) {
		setColumn(COLUMN_SUBJECT, subject);
	}
	
	@Override
	public void store() throws IDOStoreException {
		super.store();
		BlobWrapper wrapper = getBlobColumnValue(COLUMN_CONTENT);
		wrapper.setInputStreamForBlobWrite(null);
	}

	public void addAttachment(ICFile attachment) throws IDOAddRelationshipException {
		this.idoAddTo(attachment);
	}

	public Collection<ICFile> getAttachments() {
		try {
			return this.idoGetRelatedEntities(ICFile.class);
		} catch (IDORelationshipException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void removeAttachment(ICFile attachment) throws IDORemoveRelationshipException {
		this.idoRemoveFrom(attachment);
	}

}
