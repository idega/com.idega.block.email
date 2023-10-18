package com.idega.block.email.business;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.mail.Message;

import org.apache.commons.lang.StringUtils;
import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.FoundMessagesInfo;
import com.idega.block.email.bean.MessageParameters;
import com.idega.block.email.bean.MessageParserType;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.business.IBOLookup;
import com.idega.business.IBOLookupException;
import com.idega.business.IBORuntimeException;
import com.idega.core.file.util.MimeTypeUtil;
import com.idega.idegaweb.IWApplicationContext;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.repository.RepositoryService;
import com.idega.user.business.UserBusiness;
import com.idega.user.data.User;
import com.idega.util.ArrayUtil;
import com.idega.util.CoreConstants;
import com.idega.util.EmailValidator;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.StringHandler;
import com.idega.util.StringUtil;
import com.idega.util.expression.ELUtil;

/**
 * Implementation for {@link EmailSenderHelper}. Spring/DWR bean
 *
 * @author <a href="mailto:valdas@idega.com">Valdas Žemaitis</a>
 * @version $Revision: 1.4 $
 *
 * Last modified: $Date: 2009/05/15 07:23:40 $ by: $Author: valdas $
 */

@Service(EmailSenderHelperImpl.BEAN_NAME)
@Scope(BeanDefinition.SCOPE_SINGLETON)
@RemoteProxy(creator=SpringCreator.class, creatorParams={
		@Param(name="beanName", value=EmailSenderHelperImpl.BEAN_NAME),
		@Param(name="javascript", value=EmailSenderHelper.DWR_OBJECT)
	}, name=EmailSenderHelper.DWR_OBJECT)
public class EmailSenderHelperImpl implements EmailSenderHelper {

	static final String BEAN_NAME = "emailSenderBean";
	private static final Logger LOGGER = Logger.getLogger(EmailSenderHelperImpl.class.getName());

	@Autowired
	private RepositoryService repository;

	@Autowired
	private ApplicationContext context;

	@Override
	@RemoteMethod
	public boolean sendMessage(MessageParameters parameters) {
		if (parameters == null) {
			return Boolean.FALSE;
		}

		IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
		String host = settings.getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER);

		File attachedFile = getFileToAttach(parameters.getAttachments(), parameters.getSubject());

		Message mail = null;
		boolean success = false;
		try {
			mail = SendMail.send(
					parameters.getFrom(),
					parameters.getRecipientTo(),
					parameters.getRecipientCc(),
					parameters.getRecipientBcc(),
					parameters.getReplyTo(),
					host,
					parameters.getSubject(),
					parameters.getMessage(),
					MimeTypeUtil.MIME_TYPE_HTML,
					null,
					false,
					false,
					attachedFile
			);
			success = mail != null;

			if (parameters.isSaveMessageIntoDB()) {
				//Saving email message into DB
				saveEmailIntoDB(parameters);
			}

			return success;
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error sending mail: " + parameters, e);
			success = false;
		} finally {
			if (success && mail != null) {
				publishEvent(mail, parameters, attachedFile);
			} else {
				//	Attached file will be deleted by published event's handler
				if (attachedFile != null) {
					attachedFile.delete();
				}
			}
		}

		return Boolean.FALSE;
	}

	private void publishEvent(Message mail, MessageParameters parameters, File attachedFile) {
		ApplicationEmailEvent event = new ApplicationEmailEvent(this);

		if (mail != null) {
			Map<String, FoundMessagesInfo> messages = new HashMap<String, FoundMessagesInfo>();
			messages.put(mail.toString(), new FoundMessagesInfo(Arrays.asList(mail), MessageParserType.MANUAL));
			event.setMessages(messages);
		}
		parameters.setAttachment(attachedFile);
		event.setParameters(parameters);

		context.publishEvent(event);
	}

	@Override
	public File getFileToAttach(List<String> filesInRepository, String fileName) {
		if (ListUtil.isEmpty(filesInRepository)) {
			return null;
		}

		File attachment = filesInRepository.size() == 1 ? getResource(filesInRepository.iterator().next()) : getZippedFiles(filesInRepository, fileName);

		return attachment;
	}

	private File getZippedFiles(List<String> filesInRepository, String name) {
		if (ListUtil.isEmpty(filesInRepository)) {
			return null;
		}

		List<File> filesToZip = new ArrayList<File>(filesInRepository.size());
		for (String pathInRepository: filesInRepository) {
			File file = getResource(pathInRepository);
			if (file != null) {
				filesToZip.add(file);
			}
		}
		if (ListUtil.isEmpty(filesToZip)) {
			return null;
		}

		String fileName = StringUtil.isEmpty(name) ? "Attachments" : "Attachment_for_".concat(
				StringHandler.stripNonRomanCharacters(name, new char[] {'-', '_', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}));
		fileName = fileName.concat(".zip");
		try {
			File zippedFile = FileUtil.getZippedFiles(filesToZip, fileName);
			LOGGER.info("Zipped files " + filesToZip + " into " + fileName + ": " + zippedFile);
			return zippedFile;
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error zipping uploaded files: " + filesInRepository, e);
		}

		return null;
	}

	private File getResource(String pathInRepository) {
		if (StringUtil.isEmpty(pathInRepository)) {
			return null;
		}
		if (!pathInRepository.startsWith(CoreConstants.WEBDAV_SERVLET_URI)) {
			pathInRepository = new StringBuilder(CoreConstants.WEBDAV_SERVLET_URI).append(pathInRepository).toString();
		}

		InputStream stream = null;
		try {
			stream = repository.getInputStreamAsRoot(pathInRepository);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting InputStream for: " + pathInRepository, e);
		}
		if (stream == null) {
			return null;
		}

		String fileName = pathInRepository;
		int index = fileName.lastIndexOf(CoreConstants.SLASH);
		if (index != -1) {
			fileName = pathInRepository.substring(index + 1);
		}
		File file = new File(fileName);
		try {
			FileUtil.streamToFile(stream, file);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error streaming from " + pathInRepository + " to file: " + file.getName(), e);
		} finally {
			IOUtil.closeInputStream(stream);
		}

		return file;
	}

	/**
	 * Saving emails into DB
	 * @param parameters
	 */
	private void saveEmailIntoDB(MessageParameters parameters) {
		IWApplicationContext iwac = IWMainApplication.getDefaultIWApplicationContext();
		if (iwac != null) {
			//Getting the sender user
			UserBusiness userBusiness = getUserBusiness(iwac);
			User sender = getUserByEmail(userBusiness, parameters.getFrom());
			//Getting the receivers and saving the emails
			if (sender != null) {
				List<String> receiversList = new ArrayList<String>();
				//Adding to the receivers list recipients to
				addRecipients(receiversList, parameters.getRecipientTo());
				//Adding to the receivers list  recipients cc
				addRecipients(receiversList, parameters.getRecipientCc());
				//Adding to the receivers list  recipients bcc
				addRecipients(receiversList, parameters.getRecipientBcc());
				//Saving the emails into DB
				if (!receiversList.isEmpty()) {
					for (String receiversEmail : receiversList) {
						User receiver = getUserByEmail(userBusiness, receiversEmail);
						if (receiver != null) {
							try {
								ELUtil.getInstance().publishEvent(new com.idega.block.email.event.UserMessage(this, receiver, parameters.getSubject(), parameters.getMessage(), sender, false));
							} catch (Exception e) {
								LOGGER.log(Level.WARNING, "Could not save the email message: " + e.getLocalizedMessage(), e);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Get user by the email
	 * @param userBusiness UserBusiness
	 * @param email Email
	 * @return User
	 */
	private User getUserByEmail(UserBusiness userBusiness, String email) {
		User user = null;
		try {
			if (userBusiness != null && !StringUtil.isEmpty(email)) {
				Collection<User> senderUsersCol = userBusiness.getUsersByEmail(email);
				if (senderUsersCol != null && !senderUsersCol.isEmpty()) {
					user = senderUsersCol.iterator().next();
				}
			}
		} catch (Exception e) {
			LOGGER.warning("Could not get user by email: " + email + ". Error occured: " + e.getLocalizedMessage());
			e.printStackTrace();
		}
		return user;
	}

	/**
	 * Adding recipients into the list
	 * @param addressesList List of recipients
	 * @param addresses Addresses to be added
	 */
	private void addRecipients(List<String> addressesList, String addresses) {
		if (!StringUtil.isEmpty(addresses)) {
			String fixedAddresses = StringHandler.replace(addresses, CoreConstants.SEMICOLON, CoreConstants.COMMA).trim();
			if (doValidateAddresses(fixedAddresses)) {
				addressesList.addAll(Arrays.asList(StringUtils.split(fixedAddresses, CoreConstants.COMMA)));
			}
		}
	}

	/**
	 * Validate email addresses
	 * @param addresses
	 * @return
	 */
	private static boolean doValidateAddresses(String addresses) {
		if (StringUtil.isEmpty(addresses))
			return false;

		String[] emails = addresses.split(CoreConstants.COMMA);
		if (ArrayUtil.isEmpty(emails))
			return false;

		for (String email: emails) {
			if (!EmailValidator.getInstance().validateEmail(email))
				return false;
		}

		return true;
	}

	private UserBusiness getUserBusiness(IWApplicationContext iwac) {
		try {
			return IBOLookup.getServiceInstance(iwac, UserBusiness.class);
		}
		catch (IBOLookupException ile) {
			throw new IBORuntimeException(ile);
		}
	}
}
