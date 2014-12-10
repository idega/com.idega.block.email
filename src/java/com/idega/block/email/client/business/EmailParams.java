package com.idega.block.email.client.business;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;


/**
 * @author <a href="mailto:civilis@idega.com">Vytautas Čivilis</a>
 * @version $Revision: 1.1 $
 *
 * Last modified: $Date: 2008/07/16 11:39:53 $ by $Author: civilis $
 */
public class EmailParams {

	private Folder folder;
	private String hostname;
	private String username;
	private String password;
	private Session session;
	private Store store;
	private String protocol;
	private Message[] messagesFound;

	private boolean loggedOut;

	public Message[] getMessagesFound() {
		return messagesFound;
	}
	public void setMessagesFound(Message[] messagesFound) {
		this.messagesFound = messagesFound;
	}
	public Folder getFolder() {
		return folder;
	}
	public void setFolder(Folder folder) {
		this.folder = folder;
	}
	public String getHostname() {
		return hostname;
	}
	public void setHostname(String hostname) {
		this.hostname = hostname;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public Session getSession() {
		return session;
	}
	public void setSession(Session session) {
		this.session = session;
	}
	public Store getStore() {
		return store;
	}
	public void setStore(Store store) {
		this.store = store;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public boolean isLoggedOut() {
		return loggedOut;
	}
	public void setLoggedOut(boolean loggedOut) {
		this.loggedOut = loggedOut;
	}

	@Override
	public String toString() {
		return "Hostname: " + getHostname() + ", protocol: " + getProtocol() + ", user name: " + getUsername() + ", password: " +
				getPassword() + ", session: " + getSession() + ", store: " + getStore() + ", folder: " + getFolder();
	}
}