package com.idega.block.email.parser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessageRemovedException;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.block.email.EmailConstants;
import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.block.email.client.business.EmailParams;
import com.idega.block.email.client.business.EmailSubjectPatternFinder;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.core.messaging.EmailMessage;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.CoreUtil;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;
import com.sun.mail.imap.IMAPNestedMessage;

public abstract class DefaultMessageParser implements EmailParser {

	private static final Logger LOGGER = Logger.getLogger(DefaultMessageParser.class.getName());

	@Autowired
	private EmailSubjectPatternFinder emailsFinder;

	@Override
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

	@Override
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

	protected boolean isValidEmail(Message message) throws MessagingException {
		if (message == null)
			return false;

		String subject = null;
		try {
			subject = message.getSubject();
		} catch (MessagingException e) {}
		Date sentDate = null;
		try {
			sentDate = message.getSentDate();
		} catch (MessagingException e) {}
		String contentType = null;
		try {
			contentType = message.getContentType();
		} catch (MessagingException e) {}

		//	Checking if mail is auto generated
		if (doExistHeaderFlag(message, SendMail.HEADER_AUTO_SUBMITTED, "auto-generated") &&
				!doExistHeaderFlag(message, SendMail.HEADER_PRECEDENCE, "bulk")) {
			LOGGER.warning("Message (subject: " + subject + ", sent: " + sentDate + ", content type: " + contentType +
					") is auto generated, skipping it");
			return false;
		}

		//	Checking if mail is auto reply
		if (!StringUtil.isEmpty(subject)) {
			if (subject.toLowerCase().indexOf("[autoreply]") != -1) {
				LOGGER.warning("Message (subject: " + subject + ", sent: " + sentDate + ", content type: " + contentType +
						") is a result of auto reply, skipping it");
				return false;
			}
		}

		//	Checking if mail is report type
		try {
			if (message.isMimeType(EmailConstants.MESSAGE_MULTIPART_REPORT)) {
				LOGGER.warning("Message (subject: " + subject + ", sent: " + sentDate + ", content type: " + contentType +
						") is a report, skipping it");
				return false;
			}
		} catch (MessagingException e) {
			LOGGER.warning("Error resolving mime type for message with subject: " + subject + ", sent: " + sentDate + ", content type: " + contentType);
		}

		if (subject == null) {
			//	Will check if content is provided
			Object content = null;
			try {
				content = message.getContent();
			} catch (Exception e) {
				LOGGER.warning("Error resolving content for message with subject: " + subject + ", sent: " + sentDate + ", content type: " +
						contentType + ". Marking this message as invalid and skipping");
			}
			if (content == null)
				return false;
		}

		return true;
	}

	private boolean doExistHeaderFlag(Message message, String headerFlag, String headerFlagValue) {
		String[] flags = null;
		try {
			flags = message.getHeader(headerFlag);
		} catch (MessageRemovedException e) {
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error getting header flag: " + headerFlag, e);
		}
		if (ArrayUtil.isEmpty(flags))
			return false;

		for (String flag: flags) {
			if (headerFlagValue.equals(flag))
				return true;
		}

		return false;
	}

	@Override
	public synchronized EmailMessage getParsedMessage(Message message, EmailParams params) throws Exception {
		EmailMessage parsedMessage = null;
		if (!isValidEmail(message)) {
			return null;
		}

		parsedMessage = getNewMessage();
		try {
			parsedMessage.setSubject(message.getSubject());

			Object[] msgAndAttachments = getParsedContent(message);
			if (ArrayUtil.isEmpty(msgAndAttachments)) {
				parsedMessage = null;
				return parsedMessage;
			}

			Object body = msgAndAttachments[0];
			if (body == null) {
				body = CoreConstants.EMPTY;
			}
			parsedMessage.setBody(body instanceof String ? (String) body : body.toString());

			String fromAddress = getFromAddress(message);

			Address[] froms = message.getFrom();
			String senderName = null;
			if (!ArrayUtil.isEmpty(froms)) {
				List<Address> tmp = Arrays.asList(froms);
				for (Iterator<Address> addressIter = tmp.iterator(); (StringUtil.isEmpty(senderName) && addressIter.hasNext());) {
					Address address = addressIter.next();
					if (address instanceof InternetAddress) {
						InternetAddress iaddr = (InternetAddress) address;
						senderName = iaddr.getPersonal();
					}
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

	private Object[] getParsedContent(Message msg) {
		String messageTxt = CoreConstants.EMPTY;

		Object[] msgAndAttachments = new Object[2];
		try {
			Object content = msg.getContent();
			Map<String, InputStream> attachemntMap = new HashMap<String, InputStream>();
			msgAndAttachments[1] = attachemntMap;
			msgAndAttachments[0] = messageTxt;
			if (msg.isMimeType(MimeTypeUtil.MIME_TYPE_TEXT_PLAIN)) {
				if (content instanceof String) {
					msgAndAttachments[0] = parsePlainTextMessage((String) content);
				}

			} else if (msg.isMimeType(MimeTypeUtil.MIME_TYPE_HTML)) {
				if (content instanceof String) {
					msgAndAttachments[0] = parseHTMLMessage((String) content);
				}

			} else if (msg.isMimeType(EmailConstants.MULTIPART_MIXED_TYPE)) {
				msgAndAttachments = parseMultipartMixed((Multipart) content);

			} else if (msg.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) {
				IMAPNestedMessage nestedMessage = (IMAPNestedMessage) msg.getContent();
				msgAndAttachments = parseRFC822(nestedMessage);

			} else if (msg.isMimeType(EmailConstants.MULTIPART_ALTERNATIVE_TYPE)) {
				msgAndAttachments = parseMultipartAlternative((Multipart) content);

			} else if (msg.isMimeType(EmailConstants.MULTIPART_RELATED_TYPE)) {
				msgAndAttachments[0] = parseMultipartRelated((Multipart) msg.getContent());

			} else if (msg.isMimeType(EmailConstants.MESSAGE_MULTIPART_SIGNED)) {
				LOGGER.warning("Message (subject: " + msg.getSubject() + ", sent: " + msg.getSentDate() + "; type: " + msg.getClass() +	") is signed! Parsing may be incorrect!");
				msgAndAttachments[0] = getParsedMultipart((Multipart) msg.getContent());

			} else if (msg.isMimeType(EmailConstants.MESSAGE_MULTIPART_REPORT)) {
				msgAndAttachments[0] = getParsedMultipart((Multipart) msg.getContent());

			} else {
				String message = "There is no content parser for MIME type ('" + msg.getContentType() + "') message: " + msg + ", subject: " + msg.getSubject();
				LOGGER.warning(message);
				CoreUtil.sendExceptionNotification(message, null);
				return null;
			}
		} catch (MessagingException e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving content text from email msg", e);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Exception while resolving content text from email msg", e);
		} catch (Exception e) {

		}
		return msgAndAttachments;
	}

	private Object[] getParsedMultipart(Multipart mp) throws MessagingException, IOException {
		return parseMultipartMixed(mp);
	}

	@SuppressWarnings("unchecked")
	private Object[] parseMultipartMixed(Multipart messageMultipart) throws MessagingException, IOException {
		String msg = CoreConstants.EMPTY;
		Object[] msgAndAttachements = new Object[2];
		Map<String, InputStream> attachmenstMap = new HashMap<String, InputStream>();
		msgAndAttachements[1] = attachmenstMap;
		for (int i = 0; i < messageMultipart.getCount(); i++) {
			Part messagePart = messageMultipart.getBodyPart(i);
			String contentType = messagePart.getContentType();
			String disposition = messagePart.getDisposition();
			// it is attachment
			if ((disposition != null) && (!messagePart.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) && ((disposition.equalsIgnoreCase(Part.ATTACHMENT) ||
																											disposition.equalsIgnoreCase(Part.INLINE)))) {
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
				} else if (contentType.indexOf("name*=") != -1) {
					// When attachments send from evolution mail client,
					// there is errors so we do what we can.
					fileName = contentType.substring(contentType.indexOf("name*=") + 6);
					// maybe we are lucky to decode it, if not, well
					// better something then nothing.
					fileName = MimeUtility.decodeText(fileName);

				} else {
					// well not much can be done then can it?:)
					fileName = "UnknownFile";
				}
				attachmenstMap.put(fileName, streamFromMemory);

				// It's a message body
			} else if (messagePart.getContent() instanceof String) {
				if (messagePart.isMimeType(MimeTypeUtil.MIME_TYPE_HTML))
					msg += parseHTMLMessage((String) messagePart.getContent());
				else {
					// it's plain text
					msg += (String) messagePart.getContent();
				}

				// "multipart/Mixed" can have multipart/alternative sub type.
			} else if (messagePart.getContent() instanceof Multipart && (messagePart.isMimeType(EmailConstants.MULTIPART_ALTERNATIVE_TYPE) || contentType.toLowerCase().equals(EmailConstants.MULTIPART_ALTERNATIVE_TYPE))) {
				Object[] parsedMsg = parseMultipartMixed((Multipart) messagePart.getContent());
				msg += parsedMsg[0];

				attachmenstMap.putAll((Map<String, InputStream>) parsedMsg[1]);

			} else if (messagePart.getContent() instanceof Multipart && (messagePart.isMimeType(EmailConstants.MULTIPART_RELATED_TYPE) || contentType.toLowerCase().equals(EmailConstants.MULTIPART_RELATED_TYPE))) {
				msg += parseMultipartRelated((Multipart) messagePart.getContent());

			} else if (messagePart.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) {
				IMAPNestedMessage nestedMessage = (IMAPNestedMessage) messagePart.getContent();

				Object[] parsedMsg = parseRFC822(nestedMessage);

				msg += parsedMsg[0];
				attachmenstMap.putAll((Map<String, InputStream>) parsedMsg[1]);

			} else if (messagePart.getContent() instanceof Multipart) {
				Object[] parsedMsg = parseMultipartMixed((Multipart) messagePart.getContent());
				if (!ArrayUtil.isEmpty(parsedMsg)) {
					msg += parsedMsg[0];

					attachmenstMap.putAll((Map<String, InputStream>) parsedMsg[1]);
				}

			} else {
				Object content = messagePart.getContent();
				LOGGER.warning("Do not know how to handle content type " + messagePart.getContentType() + ", content: " + (content == null ? "unknown" : content.getClass().getName()));
			}
		}
		msgAndAttachements[0] = msg;
		return msgAndAttachements;
	}

	@SuppressWarnings("unchecked")
	private Object[] parseRFC822(IMAPNestedMessage part) throws MessagingException, IOException {
		String msg = CoreConstants.EMPTY;

		Object[] msgAndAttachements = new Object[2];
		Map<String, InputStream> attachmentMap = new HashMap<String, InputStream>();
		msgAndAttachements[1] = attachmentMap;

		if (part.isMimeType(MimeTypeUtil.MIME_TYPE_TEXT_PLAIN)) {
			//	Plain text
			if (part.getContent() instanceof String)
				msg += parsePlainTextMessage((String) part.getContent());
			msgAndAttachements[0] = msg;

		} else if (part.isMimeType(MimeTypeUtil.MIME_TYPE_HTML)) {
			//	HTML
			if (part.getContent() instanceof String)
				msg += parseHTMLMessage((String) part.getContent());
			msgAndAttachements[0] = msg;

		} else if (part.isMimeType(EmailConstants.MULTIPART_MIXED_TYPE)) {
			//	Multipart mixed
			msgAndAttachements = parseMultipartMixed((Multipart) part.getContent());

		} else if (part.isMimeType(EmailConstants.MULTIPART_ALTERNATIVE_TYPE)) {
			//	Multipart alternative
			Object[] parsedMsg = parseMultipartMixed((Multipart) part.getContent());
			msg += parsedMsg[0];

			attachmentMap.putAll((Map<String, InputStream>) parsedMsg[1]);

			//msg += parseMultipartAlternative((MimeMultipart) part.getContent());
			msgAndAttachements[0] = msg;

		} else if (part.isMimeType(EmailConstants.MULTIPART_RELATED_TYPE) || part.getContentType().toLowerCase().equals("multipart/related")) {
			//	Multipart related
			msg += parseMultipartRelated((Multipart) part.getContent());
			msgAndAttachements[0] = msg;

		} else if (part.isMimeType(EmailConstants.MESSAGE_RFC822_TYPE)) {
			//	RCF822
			IMAPNestedMessage nestedMessage = (IMAPNestedMessage) part.getContent();

			Object[] parsedMsg = parseRFC822(nestedMessage);
			msg += parsedMsg[0];

			attachmentMap.putAll((Map<String, InputStream>) parsedMsg[1]);

		} else if (part.getContent() instanceof Multipart) {
			Object[] parsedMsg = parseMultipartMixed((Multipart) part.getContent());
			if (!ArrayUtil.isEmpty(parsedMsg)) {
				msg += parsedMsg[0];

				attachmentMap.putAll((Map<String, InputStream>) parsedMsg[1]);
			}

		} else {
			Object content = part.getContent();
			LOGGER.warning("Do not know how to handle content type " + part.getContentType() + ", content: " + (content == null ? "unknown" : content.getClass().getName()));
		}

		return msgAndAttachements;
	}

	@SuppressWarnings("unchecked")
	private Object[] parseMultipartAlternative(Multipart multipart) throws MessagingException, IOException {
		String msg = CoreConstants.EMPTY;

		Object[] msgAndAttachements = new Object[2];
		Map<String, InputStream> attachmentMap = new HashMap<String, InputStream>();
		msgAndAttachements[1] = attachmentMap;

		for (int i = 0; i < multipart.getCount(); i++) {
			Part part = multipart.getBodyPart(i);
			if (part.isMimeType(MimeTypeUtil.MIME_TYPE_HTML)) {
				msg += parseHTMLMessage((String) part.getContent());
				msgAndAttachements[0] = msg;
			} else if (part.isMimeType(MimeTypeUtil.MIME_TYPE_TEXT_PLAIN)) {
				msg += parsePlainTextMessage((String) part.getContent());
				msgAndAttachements[0] = msg;
			} else if (part.getContent() instanceof Multipart && part.isMimeType(EmailConstants.MULTIPART_MIXED_TYPE)) {
				Object[] parsedMsg = parseMultipartMixed((Multipart) part.getContent());
				msg += parsedMsg[0];

				attachmentMap.putAll((Map<String, InputStream>) parsedMsg[1]);
			}
		}

		return msgAndAttachements;
	}

	private String parseMultipartRelated(Multipart multipart) throws MessagingException, IOException {
		String content = null;
		StringBuffer allContent = new StringBuffer();

		for (int i = 0; i < multipart.getCount(); i++) {
			BodyPart part = multipart.getBodyPart(i);
			if (part.isMimeType(MimeTypeUtil.MIME_TYPE_HTML)) {
				content = parseHTMLMessage((String) part.getContent());
				if (content != null) {
					return content;
				}
			/*} else if (part.isMimeType(MimeTypeUtil.MIME_TYPE_TEXT_PLAIN)) {
				content = parsePlainTextMessage((String) part.getContent());
				if (content != null) {
					return content;
				}*/
			} else {
				String contentType = multipart.getContentType();
				Object contentObject = part.getContent();
				if (contentObject instanceof Multipart) {
					String partContent = parseMultipartRelated((Multipart) contentObject);
					if (partContent != null) {
						allContent.append(partContent);
					}
				} else if (contentObject instanceof Message) {
					LOGGER.warning("Do not know how to handle content object (" + Message.class.getName() + ") of " + contentType + ", content object: " + contentObject.getClass());
				} else if (contentObject instanceof String) {
					allContent.append((String) contentObject);
				} else if (contentObject instanceof InputStream) {
					LOGGER.warning("Do not know how to handle content object (" + InputStream.class.getName() + ") of " + contentType + ", content object: " + contentObject.getClass());
				} else {
					LOGGER.warning("Unhandled content: " + contentType + ", content object: " + contentObject.getClass());
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

	@Override
	public Collection<? extends EmailMessage> getParsedMessages(ApplicationEmailEvent emailEvent) {
		LOGGER.warning("This method is not implemented!");
		return null;
	}

	@Override
	public String getFromAddress(Message message) throws MessagingException {
		Address[] froms = message.getFrom();
		for (Address address : froms) {
			if (address instanceof InternetAddress) {
				InternetAddress iaddr = (InternetAddress) address;
				return iaddr.getAddress();
			}
		}

		return null;
	}
}