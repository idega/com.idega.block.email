package com.idega.block.email.patterns;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.business.EmailSenderHelper;
import com.idega.block.email.client.business.EmailParams;
import com.idega.data.SimpleQuerier;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.ListUtil;
import com.idega.util.StringUtil;
import com.idega.util.datastructures.map.MapUtil;
import com.idega.util.expression.ELUtil;

@Service
@Scope(BeanDefinition.SCOPE_SINGLETON)
public class IdentifierSearcher extends DefaultSubjectPatternFinder {

	private static final long serialVersionUID = -7004965182979660614L;

	private static final String IDENTIFIER_REGULAR_EXPRESSION = "[A-Z]{1,3}-\\d{4}-\\d{2}-\\d{2}-[A-Z0-9]{4,}",
								LOWER_CASE_IDENTIFIER_REGULAR_EXPRESSION = "[a-z]{1,3}-\\d{4}-\\d{2}-\\d{2}-[a-z0-9]{4,}";
	private static final Pattern	IDENTIFIER_PATTERN = Pattern.compile(IDENTIFIER_REGULAR_EXPRESSION),
									LOWER_CASE_IDENTIFIER_PATTERN = Pattern.compile(LOWER_CASE_IDENTIFIER_REGULAR_EXPRESSION);


	@Autowired
	private EmailSenderHelper emailSenderHelper;


	public IdentifierSearcher() {
		super();

		addPattern(IDENTIFIER_PATTERN);
		addPattern(LOWER_CASE_IDENTIFIER_PATTERN);
	}

	@Override
	public Map<String, FoundMessagesInfo> getSearchResultsFormatted(EmailParams params) throws MessagingException {
		Map<String, FoundMessagesInfo> messagesMap = super.getCaseIdentifierSearchResultsFormatted(params);

		if (getApplication().getSettings().getBoolean("email.identifier_searcher.msg_body_and_attachment", true)) {
			getCaseIdentifierSearchResultsFormattedForBodyAndAttachments(params, messagesMap);
		}

		return messagesMap;
	}

	@Override
	public MessageParserType getParserType() {
		return MessageParserType.BPM;
	}

	@Override
	public String getFixedIdentifier(String identifier) {
		if (StringUtil.isEmpty(identifier)) {
			return identifier;
		}

		if (getApplication().getSettings().getBoolean("bpm.email_identifier_fix", true)) {
			String changedIdentifier = identifier.toUpperCase();
			if (!changedIdentifier.equals(identifier)) {
				getLogger().info("Changed original identifier '" + identifier + "' to '" + changedIdentifier + "'");
			}
			return changedIdentifier;
		}

		return identifier;
	}

	private void getCaseIdentifierSearchResultsFormattedForBodyAndAttachments(EmailParams params, Map<String, FoundMessagesInfo> messagesMap) {
		try {
			Message[] messages = getAllMessages(params);
			if (!ArrayUtil.isEmpty(messages)) {

				List<String> caseIdentifiers = getCaseIndentifiers();
				if (ListUtil.isEmpty(caseIdentifiers)) {
					return;
				}

				for (Message message : messages) {

					try {
						if (message == null) {
							continue;
						}

						try {
							//Skip the message, if it is seen/read already
							if (message.isSet(Flags.Flag.SEEN)) {
								continue;
							}

							//Set message as read
							message.setFlag(Flags.Flag.SEEN, true);
						} catch (Exception eSeen) {
							getLogger().log(Level.WARNING, "Could not check or set the message as SEEN. Message: " + message, eSeen);
						}

						List<String> fileNames = null;
						String messageBody = null;

						//Get the data from the message
						Object[] msgAndAttachments = getEmailSenderHelper().getParsedContent(message);
						if (ArrayUtil.isEmpty(msgAndAttachments)) {
							messageBody = getMessageBody(message);
							String fileName = message.getFileName();
							if (!StringUtil.isEmpty(fileName)) {
								fileNames = new ArrayList<>();
								fileNames.add(fileName);
							}
						} else {
							//Message body
							Object body = msgAndAttachments[0];
							if (body == null) {
								body = CoreConstants.EMPTY;
							}
							messageBody = body instanceof String ? (String) body : body.toString();
							messageBody = StringUtil.isEmpty(messageBody) ? messageBody : messageBody.replaceAll("&#045;", CoreConstants.MINUS);

							//Attachments
							if (msgAndAttachments.length > 1 && msgAndAttachments[1] != null) {
								@SuppressWarnings("unchecked")
								Map<String, InputStream> files = (Map<String, InputStream>) msgAndAttachments[1];
								if (!MapUtil.isEmpty(files)) {
									Set<String> fileNamesAsSet = files.keySet();
									if (!ListUtil.isEmpty(fileNamesAsSet)) {
										fileNames = new ArrayList<String>();
										fileNames.addAll(fileNamesAsSet);
									}
								}
							}
						}

						//Check
						String caseIdentifierFound = null;
						if (!ListUtil.isEmpty(fileNames)) {
							boolean shouldProceedWithFilesCheck = true;
							for (String fileN : fileNames) {
								if (!StringUtil.isEmpty(fileN)) {
									for (String caseIdentifier : caseIdentifiers) {
										if (fileN.contains(caseIdentifier)) {
											caseIdentifierFound = new String(caseIdentifier);
											shouldProceedWithFilesCheck = false;
											break;
										}
									}
								}
								if (!shouldProceedWithFilesCheck) {
									break;
								}
							}
						}

						if (!StringUtil.isEmpty(messageBody) && StringUtil.isEmpty(caseIdentifierFound)) {
							for (String caseIdentifier : caseIdentifiers) {
								if (messageBody.contains(caseIdentifier)) {
									caseIdentifierFound = new String(caseIdentifier);
									break;
								}
							}
						}

						if (!StringUtil.isEmpty(caseIdentifierFound)) {
							FoundMessagesInfo messagesInfo = messagesMap.get(caseIdentifierFound);
							if (messagesInfo == null) {
								messagesInfo = new FoundMessagesInfo(getParserType());
								messagesInfo.addMessage(message);
								messagesMap.put(caseIdentifierFound, messagesInfo);
							} else {
								Collection<Message> messagesIn = messagesInfo.getMessages();
								if (ListUtil.isEmpty(messagesIn)) {
									messagesInfo.addMessage(message);
									messagesMap.put(caseIdentifierFound, messagesInfo);
								} else {
									boolean canAdd = true;
									for (Message msgIn : messagesIn) {
										if (
												msgIn != null
												&& !StringUtil.isEmpty(msgIn.getSubject())
												&& !StringUtil.isEmpty(message.getSubject())
												&& msgIn.getSubject().equalsIgnoreCase(message.getSubject())
												&& msgIn.getSize() == message.getSize()
										) {
											canAdd = false;
											break;
										}
									}
									if (canAdd) {
										messagesInfo.addMessage(message);
										messagesMap.put(caseIdentifierFound, messagesInfo);
									}
								}
							}

						}
					} catch (Exception eIn) {
						getLogger().log(Level.WARNING, "Could not filter the email messages by body and attachments. Failed to check a message: " + message, eIn);
					}

				}
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Could not filter the email messages by body and attachments.", e);
		}
	}

	private Message[] getAllMessages(EmailParams params) throws MessagingException {
		Folder folder = params.getFolder();

		Message[] messages = folder.getMessages();

		return messages;
	}

    public String getMessageBody(Message message) throws MessagingException, java.io.IOException {
        Object content = message.getContent();
        String result = CoreConstants.EMPTY;

        if (content != null) {
            if (message.isMimeType("text/plain")) {
                return (String)content;
            } else if (message.isMimeType("multipart/alternative")) {
    	        Multipart mp = (Multipart) message.getContent();
                int numParts = mp.getCount();
                for (int i = 0; i < numParts; ++i) {
                    if (mp.getBodyPart(i).isMimeType("text/plain")) {
    					return (String)mp.getBodyPart(i).getContent();
    				}
                }
                return CoreConstants.EMPTY;
            } else if (message.isMimeType("multipart/*")) {
    	        Multipart mp = (Multipart)content;
                if (mp.getBodyPart(0).isMimeType("text/plain")) {
    				return (String)mp.getBodyPart(0).getContent();
    			} else {
    				return CoreConstants.EMPTY;
    			}
            } else {
    			return CoreConstants.EMPTY;
    		}
        }
        return result;
    }

	public List<String> getCaseIndentifiers() {
		List<String> caseIdentifiers = new ArrayList<>();
		List<Serializable[]> caseIdentifiersSerializableList = new ArrayList<>();
		try {
			String queryApp = "select distinct c.case_identifier from proc_case c where c.case_identifier is not null";
			try {
				caseIdentifiersSerializableList = SimpleQuerier.executeQuery(queryApp, 1);
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error executing query: " + queryApp, e);
			}

			//Add the unique user ids into the final list
			if (!ListUtil.isEmpty(caseIdentifiersSerializableList)) {
				for (Serializable[] caseIdentifier : caseIdentifiersSerializableList) {
					if (ArrayUtil.isEmpty(caseIdentifier)) {
						continue;
					}

					Serializable caseIdentifierSer = caseIdentifier[0];
					if (caseIdentifierSer != null && caseIdentifierSer instanceof String) {
						String caseIdentifierStr = (String) caseIdentifierSer;
						if (!caseIdentifiers.contains(caseIdentifierStr)) {
							caseIdentifiers.add(caseIdentifierStr);
						}
					}
				}
			}
		} catch (Exception e) {
			getLogger().log(Level.WARNING, "Could not fetch all of case identifiers.", e);
		}

		return caseIdentifiers;
	}

	public EmailSenderHelper getEmailSenderHelper() {
		if (emailSenderHelper == null) {
			ELUtil.getInstance().autowire(this);
		}
		return emailSenderHelper;
	}

	public void setEmailSenderHelper(EmailSenderHelper emailSenderHelper) {
		this.emailSenderHelper = emailSenderHelper;
	}


}