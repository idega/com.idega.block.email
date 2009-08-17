package com.idega.block.email.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.client.business.EmailSubjectPatternFinder;
import com.idega.core.messaging.EmailMessage;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.sun.mail.imap.IMAPNestedMessage;

public abstract class DefaultMessageParser implements EmailParser {

	private static final Logger LOGGER = Logger.getLogger(DefaultMessageParser.class.getName());
	
	@Autowired
	private EmailSubjectPatternFinder emailsFinder;
	
	public Map<String, Collection<? extends EmailMessage>> getParsedMessages(Map<String, FoundMessagesInfo> messages, EmailParams params) {
		if (messages == null || messages.isEmpty()) {
			return null;
		}
		
		Map<String, Collection<? extends EmailMessage>> parsedMessages = new HashMap<String, Collection<? extends EmailMessage>>();
		
		for (String key: messages.keySet()) {
			Collection<Message> messagesByKey = messages.get(key).getMessages();
			parsedMessages.put(key, getParsedMessages(messagesByKey, params));
		}
		
		return parsedMessages;
	}
	
	public Collection<? extends EmailMessage> getParsedMessagesCollection(Map<String, FoundMessagesInfo> messages, EmailParams params) {
		Map<String, Collection<? extends EmailMessage>> parsedMessages = getParsedMessages(messages, params);
		if (parsedMessages == null || parsedMessages.isEmpty()) {
			return null;
		}
		
		Collection<EmailMessage> allParsedMessages = new ArrayList<EmailMessage>();
		for (Collection<? extends EmailMessage> parsedMessagesByCategory: parsedMessages.values()) {
			allParsedMessages.addAll(parsedMessagesByCategory);
		}
		return allParsedMessages;
	}
	
	private Collection<EmailMessage> getParsedMessages(Collection<Message> messages, EmailParams params) {
		Collection<EmailMessage> emailMessages = new ArrayList<EmailMessage>();
		if (ListUtil.isEmpty(messages)) {
			return emailMessages;
		}
		
		for (Message message: messages) {
			EmailMessage parsedMessage = null;
			try {
				parsedMessage = getParsedMessage(message, params);
			} catch(Exception e) {
				LOGGER.log(Level.WARNING, "Error parsing message: " + message, e);
			}
			if (parsedMessage != null) {
				emailMessages.add(parsedMessage);
			}
		}
		
		return emailMessages;
	}
	
	protected EmailMessage getNewMessage() {
		return new EmailMessage();
	}
	
	public synchronized EmailMessage getParsedMessage(Message message, EmailParams params) throws Exception {
		EmailMessage parsedMessage = getNewMessage();
		try {			
			parsedMessage.setSubject(message.getSubject());
			
			Object[] msgAndAttachments = parseContent(message);
			if (ArrayUtil.isEmpty(msgAndAttachments)) {
				parsedMessage = null;
				return parsedMessage;
			}
			
			String body = (String) msgAndAttachments[0];
			if (body == null)
				body = CoreConstants.EMPTY;
			parsedMessage.setBody(body);
			
			Address[] froms = message.getFrom();
			String senderName = null;
			String fromAddress = null;
			for (Address address : froms) {
				if (address instanceof InternetAddress) {
					InternetAddress iaddr = (InternetAddress) address;
					fromAddress = iaddr.getAddress();
					senderName = iaddr.getPersonal();
					break;
				}
			}
			parsedMessage.setSenderName(senderName);
			parsedMessage.setFromAddress(fromAddress);
			
			@SuppressWarnings("unchecked")
			Map<String, InputStream> files = (Map<String, InputStream>) msgAndAttachments[1];
			parsedMessage.setAttachments(files);
			
			return parsedMessage;
		} finally {
			if (parsedMessage != null) {
				getEmailsFinder().moveMessage(message, params);
			}
		}
	}

	private Object[] parseContent(Message msg) {
		
		String messageTxt = "";
		
		Object[] msgAndAttachments = new Object[2];
		try {
			Object content = msg.getContent();
			Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
			msgAndAttachments[1] = attachemntMap;
			msgAndAttachments[0] = messageTxt;
			if (msg.isMimeType(CoreConstants.MAIL_TEXT_PLAIN_TYPE)) {
				
				if (content instanceof String)
					msgAndAttachments[0] = parsePlainTextMessage((String) content);
				
			} else if (msg.isMimeType(CoreConstants.MAIL_TEXT_HTML_TYPE)) {
				
				if (content instanceof String)
					msgAndAttachments[0] = parseHTMLMessage((String) content);
				
			} else if (msg.isMimeType(EmailConstants.MULTIPART_MIXED_TYPE)) {
				msgAndAttachments = parseMultipartMixed((Multipart) content);
			} else if (msg.isMimeType(EmailConstants.MULTIPART_ALTERNATIVE_TYPE)) {
				msgAndAttachments[0] = parseMultipartAlternative((MimeMultipart) msg.getContent());
			} else if (msg.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) {
				IMAPNestedMessage nestedMessage = (IMAPNestedMessage) msg.getContent();
				msgAndAttachments = parseRFC822(nestedMessage);
			} else if (msg.isMimeType(EmailConstants.MULTIPART_RELATED_TYPE)) {
				msgAndAttachments[0] = parseMultipartRelated((MimeMultipart) msg.getContent());
			} else {
				LOGGER.warning("There is no content parser for MIME type ('" + msg.getContentType() + "') message: " + msg + ", subject: " + msg.getSubject());
				return null;
			}
			
		} catch (MessagingException e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving content text from email msg", e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving content text from email msg", e);
		}
		return msgAndAttachments;
	}
	
	@SuppressWarnings("unchecked")
	private Object[] parseMultipartMixed(Multipart messageMultipart) throws MessagingException, IOException {
		
		String msg = "";
		Object[] msgAndAttachements = new Object[2];
		Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
		msgAndAttachements[1] = attachemntMap;
		for (int i = 0; i < messageMultipart.getCount(); i++) {
			
			Part messagePart = messageMultipart.getBodyPart(i);
			String disposition = messagePart.getDisposition();
			// it is attachment
			if ((disposition != null) && (!messagePart.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) && ((disposition.equals(Part.ATTACHMENT) ||
																											disposition.equals(Part.INLINE)))) {
				//	Copying attachments to memory
				InputStream input = messagePart.getInputStream();
				ByteArrayOutputStream memory = new ByteArrayOutputStream();
				FileUtil.streamToOutputStream(input, memory);
				InputStream streamFromMemory = new ByteArrayInputStream(memory.toByteArray());
				IOUtil.closeInputStream(input);
				IOUtil.closeOutputStream(memory);
				
				String fileName = messagePart.getFileName();
				if (fileName != null) {
					fileName = MimeUtility.decodeText(fileName);
				} else if (messagePart.getContentType().indexOf("name*=") != -1) {
					// When attachments send from evolution mail client,
					// there is errors so we do what we can.
					fileName = messagePart.getContentType().substring(
					    messagePart.getContentType().indexOf("name*=") + 6);
					// maybe we are lucky to decode it, if not, well
					// better something then nothing.
					fileName = MimeUtility.decodeText(fileName);
					
				} else {
					// well not much can be done then can it?:)
					fileName = "UnknownFile";
				}
				attachemntMap.put(fileName, streamFromMemory);
				// It's a message body
			} else if (messagePart.getContent() instanceof String) {
				if (messagePart.isMimeType(CoreConstants.MAIL_TEXT_HTML_TYPE))
					msg += parseHTMLMessage((String) messagePart.getContent());
				else
					// it's plain text
					msg += (String) messagePart.getContent();
				
				// "multipart/Mixed" can have multipart/alternative sub type.
			} else if (messagePart.getContent() instanceof MimeMultipart && messagePart.isMimeType(EmailConstants.MULTIPART_ALTERNATIVE_TYPE)) {
				msg += parseMultipartAlternative((MimeMultipart) messagePart.getContent());
			} else if (messagePart.getContent() instanceof MimeMultipart && messagePart.isMimeType(EmailConstants.MULTIPART_ALTERNATIVE_TYPE)) {
				msg += parseMultipartRelated((MimeMultipart) messagePart.getContent());
			} else if (messagePart.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) {
				IMAPNestedMessage nestedMessage = (IMAPNestedMessage) messagePart.getContent();
				
				Object[] parsedMsg = parseRFC822(nestedMessage);
				
				msg += parsedMsg[0];
				attachemntMap.putAll((Map) parsedMsg[1]);
				
			}
		}
		msgAndAttachements[0] = msg;
		return msgAndAttachements;
	}
	
	@SuppressWarnings("unchecked")
	private Object[] parseRFC822(IMAPNestedMessage part) throws MessagingException, IOException {
		
		String msg = "";
		
		Object[] msgAndAttachements = new Object[2];
		Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
		msgAndAttachements[1] = attachemntMap;
		
		if (part.isMimeType(CoreConstants.MAIL_TEXT_PLAIN_TYPE)) {
			//	Plain text
			if (part.getContent() instanceof String)
				msg += parsePlainTextMessage((String) part.getContent());
			msgAndAttachements[0] = msg;
		} else if (part.isMimeType(CoreConstants.MAIL_TEXT_HTML_TYPE)) {
			//	HTML
			if (part.getContent() instanceof String)
				msg += parseHTMLMessage((String) part.getContent());
			msgAndAttachements[0] = msg;
		} else if (part.isMimeType(EmailConstants.MULTIPART_MIXED_TYPE)) {
			//	Multipart mixed
			msgAndAttachements = parseMultipartMixed((Multipart) part.getContent());
		} else if (part.isMimeType(EmailConstants.MULTIPART_ALTERNATIVE_TYPE)) {
			//	Multipart alternative
			msg += parseMultipartAlternative((MimeMultipart) part.getContent());
			msgAndAttachements[0] = msg;
		} else if (part.isMimeType(EmailConstants.MULTIPART_RELATED_TYPE)) {
			//	Multipart related
			msg += parseMultipartRelated((MimeMultipart) part.getContent());
			msgAndAttachements[0] = msg;
		} else if (part.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) {
			//	RCF822
			IMAPNestedMessage nestedMessage = (IMAPNestedMessage) part.getContent();
			
			Object[] parsedMsg = parseRFC822(nestedMessage);
			msg += parsedMsg[0];
			
			attachemntMap.putAll((Map) parsedMsg[1]);
		}
		
		return msgAndAttachements;
	}
	
	private String parseMultipartAlternative(MimeMultipart multipart) throws MessagingException, IOException {
		
		String returnStr = null;
		for (int i = 0; i < multipart.getCount(); i++) {
			Part part = multipart.getBodyPart(i);
			if (part.isMimeType(CoreConstants.MAIL_TEXT_HTML_TYPE)) {
				return parseHTMLMessage((String) part.getContent());
			} else if (part.isMimeType(CoreConstants.MAIL_TEXT_PLAIN_TYPE)) {
				returnStr = parsePlainTextMessage((String) part.getContent());
			}
		}
		
		return returnStr;
	}
	
	private String parseMultipartRelated(MimeMultipart multipart) throws MessagingException, IOException {
		String content = null;
		StringBuffer allContent = new StringBuffer();
		
		for (int i = 0; i < multipart.getCount(); i++) {
			BodyPart part = multipart.getBodyPart(i);
			if (part.isMimeType(CoreConstants.MAIL_TEXT_HTML_TYPE)) {
				content = parseHTMLMessage((String) part.getContent());
				if (content != null) {
					return content;
				}
			/*} else if (part.isMimeType(CoreConstants.MAIL_TEXT_PLAIN_TYPE)) {
				content = parsePlainTextMessage((String) part.getContent());
				if (content != null) {
					return content;
				}*/
			} else {
				Object contentObject = part.getContent();
				if (contentObject instanceof MimeMultipart) {
					String partContent = parseMultipartRelated((MimeMultipart) contentObject);
					if (partContent != null) {
						allContent.append(partContent);
					}
				} else {
					LOGGER.warning("Unhandled content: " + contentObject);
				}
			}
		}
		
		return content == null ? allContent.toString() : content;
	}
	
	private String parseHTMLMessage(String message) {
		return message;// "<[!CDATA ["+ message+"]]>";
	}
	
	private String parsePlainTextMessage(String message) {
		String msgWithEscapedHTMLChars = StringUtil.escapeHTMLSpecialChars(message);
		// replacing all new line characktes to <br/> so it will
		// be displayed in html as it should
		return msgWithEscapedHTMLChars.replaceAll("\n", "<br/>");
	}

	public EmailSubjectPatternFinder getEmailsFinder() {
		if (emailsFinder == null) {
			ELUtil.getInstance().autowire(this);
		}
		return emailsFinder;
	}

	public void setEmailsFinder(EmailSubjectPatternFinder emailsFinder) {
		this.emailsFinder = emailsFinder;
	}

	public Collection<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent) {
		LOGGER.warning("This method is not implemented!");
		return null;
	}

}
