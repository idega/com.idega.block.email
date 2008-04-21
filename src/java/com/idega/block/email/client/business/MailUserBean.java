package com.idega.block.email.client.business;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.search.SearchTerm;

import com.idega.util.CoreConstants;

/**
 * This JavaBean is used to store mail user information.
 */
public class MailUserBean {
    private Folder folder;
    private String hostname;
    private String username;
    private String password;
    private Session session;
    private Store store;
    private String protocol;
    private static final String DEFAULT_PROTOCOL = "pop3";
    private static final String DEFAULT_FOLDER  = "INBOX";
    private static final String IDNETIFIER_PATTERN = "[A-Z]{1,3}-\\d{4}-\\d{2}-\\d{2}-[A-Z0-9]{4,}";
    private static Pattern subjectPattern = Pattern.compile(IDNETIFIER_PATTERN);
    public Map<String, Message> messageMap;
    public Message[] messages;
    private Matcher subjectMatcher;
    
   
    public MailUserBean(){}

    /**
     * Returns the javax.mail.Folder object.
     */
    public Folder getFolder() {
        return this.folder;
    }

    /**
     * Returns all messages with right pattern.
     * Be carefull - changes messages status in mail server
     */
    private synchronized Message[] getMessages() throws MessagingException {
	
	this.messages = this.folder.search(new SearchTerm() {      

	    @Override
	    public boolean match(Message message) {	
		try {
		    subjectMatcher = subjectPattern.matcher(message.getSubject());
		    if((subjectMatcher.find())&&(!message.isSet(Flags.Flag.ANSWERED))){
			    message.setFlag(Flags.Flag.ANSWERED, true);  
			    return true;
			}
		} catch (MessagingException e) {
		    e.printStackTrace();
		}
		return false;
	    }
        
	 });
	return this.messages;
    }
    
    /**
     * Returns message map 
     *
     */
     public Map<String, Message> getMessageMap() throws MessagingException{
	 
	this.messages = getMessages();
	messageMap = new HashMap<String, Message>();

	for (int i = 0, n = this.messages.length; i <  n; i++) {
	    
	    this.subjectMatcher = subjectPattern.matcher(this.messages[i].getSubject());
	    subjectMatcher.find();
	    
	    String indentifier = this.messages[i].getSubject().substring(this.subjectMatcher.start(), this.subjectMatcher.end());
	    
	    this.messageMap.put(indentifier, this.messages[i]);
	                                         
	}
	
	return messageMap;
    }

    /**
     * Returns the number of messages in the folder.
     */
    public int getMessageCount() throws MessagingException {
        return this.folder.getMessageCount();
    }

    /**
     * hostname getter method.
     */
    public String getHostname() {
	
        return this.hostname;
        
    }

    /**
     * hostname setter method.
     */
    public void setHostname(String hostname) {
	
        this.hostname = hostname;
        
    }

    /**
     * username getter method.
     */
    public String getUsername() {
	
        return this.username;
        
    }

    /**
     * username setter method.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * password getter method.
     */
    public String getPassword() {
	
        return this.password;
        
    }

    /**
     * password setter method.
     */
    public void setProtocol(String protocol) {
	
        this.protocol = protocol;
        
    }

     /**
     * password getter method.
     */
    public String getProtocol() {
	
        return this.protocol;
        
    }

    /**
     * password setter method.
     */
    public void setPassword(String password) {
	
        this.password = password;
        
    }


    /**
     * session getter method.
     */
    public Session getSession() {
	
        return this.session;
        
    }

    /**
     * session setter method.
     */
    public void setSession(Session s) {
	
        this.session = s;
        
    }

    /**
     * store getter method.
     */
    public Store getStore() {
	
        return this.store;
        
    }

    /**
     * store setter method.
     */
    public void setStore(Store store) {
	
        this.store = store;
        
    }

    /**
     * Method for checking if the user is logged in.
     */
    public boolean isLoggedIn() {
	
        return this.store.isConnected();
        
    }

    /**
     * Method used to login to the mail inbox.
     */
    public void login() throws Exception {
	
	Properties props = new Properties();
	this.session = Session.getDefaultInstance(props, null);
	
	if (CoreConstants.EMPTY.equals(this.protocol)) {
	    this.store = this.session.getStore(DEFAULT_PROTOCOL);
	}else {
	    this.store = this.session.getStore(this.protocol);
	}
	
        this.store.connect(this.hostname,this.username,this.password);
        this.folder = store.getFolder(DEFAULT_FOLDER);

        this.folder.open(Folder.READ_WRITE);

        
    }

    /**
     * Method used to login to the mail inbox.
     */
    public void login(String hostname, String username, String password,String protocol)
        throws Exception {

        this.protocol = protocol;
        this.hostname = hostname;
        this.username = username;
        this.password = password;

        login();
    }

    /**
     * Method used to logout from the mail host.
     */
    public void logout() throws MessagingException {
        this.folder.close(true);
        this.store.close();
        this.store = null;
        this.session = null;
    }
}

