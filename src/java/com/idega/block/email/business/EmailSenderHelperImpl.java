package com.idega.block.email.business;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.directwebremoting.annotations.Param;
import org.directwebremoting.annotations.RemoteMethod;
import org.directwebremoting.annotations.RemoteProxy;
import org.directwebremoting.spring.SpringCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import com.idega.block.email.bean.MessageParameters;
import com.idega.block.email.client.business.ApplicationEmailEvent;
import com.idega.business.IBOLookup;
import com.idega.idegaweb.IWMainApplication;
import com.idega.idegaweb.IWMainApplicationSettings;
import com.idega.slide.business.IWSlideService;
import com.idega.util.CoreConstants;
import com.idega.util.FileUtil;
import com.idega.util.IOUtil;
import com.idega.util.ListUtil;
import com.idega.util.SendMail;
import com.idega.util.StringHandler;

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
	
	private IWSlideService slide;
	
	@Autowired
	private ApplicationContext context;
	
	@RemoteMethod
	public boolean sendMessage(MessageParameters parameters) {
		if (parameters == null) {
			return Boolean.FALSE;
		}
		
		IWMainApplicationSettings settings = IWMainApplication.getDefaultIWMainApplication().getSettings();
		String host = settings.getProperty(CoreConstants.PROP_SYSTEM_SMTP_MAILSERVER);
		
		File attachedFile = getFileToAttach(parameters.getAttachments(), parameters.getSubject());
		
		boolean success = true;
		try {
			SendMail.send(parameters.getFrom(), parameters.getRecipientTo(), parameters.getRecipientCc(), parameters.getRecipientBcc(), parameters.getReplyTo(),
					host, parameters.getSubject(), parameters.getMessage(), attachedFile);
			return Boolean.TRUE;
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error sending mail: " + parameters, e);
			success = false;
		} finally {
			if (success) {
				publishEvent(parameters, attachedFile);
			} else {
				//	Attached file will be deleted by published event's handler
				if (attachedFile != null) {
					attachedFile.delete();
				}
			}
		}
		
		return Boolean.FALSE;
	}
	
	private void publishEvent(MessageParameters parameters, File attachedFile) {
		ApplicationEmailEvent event = new ApplicationEmailEvent(this);
		
		parameters.setAttachment(attachedFile);
		event.setParameters(parameters);
		
		context.publishEvent(event);
	}
	
	private File getFileToAttach(List<String> filesInSlide, String subject) {
		if (ListUtil.isEmpty(filesInSlide)) {
			return null;
		}
		
		File attachment = filesInSlide.size() == 1 ? getResource(filesInSlide.iterator().next()) : getZippedFiles(filesInSlide, subject);
		
		return attachment;
	}
	
	private File getZippedFiles(List<String> filesInSlide, String subject) {
		if (ListUtil.isEmpty(filesInSlide)) {
			return null;
		}
		
		List<File> filesToZip = new ArrayList<File>(filesInSlide.size());
		for (String pathInSlide: filesInSlide) {
			File file = getResource(pathInSlide);
			if (file != null) {
				filesToZip.add(file);
			}
		}
		if (ListUtil.isEmpty(filesToZip)) {
			return null;
		}
		
		try {
			return FileUtil.getZippedFiles(filesToZip, new StringBuilder("Attachment_for_")
				.append(StringHandler.stripNonRomanCharacters(subject, new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}))
				.append(".zip").toString());
		} catch(Exception e) {
			LOGGER.log(Level.WARNING, "Error zipping uploaded files: " + filesInSlide);
		}
		
		return null;
	}
	
	private File getResource(String pathInSlide) {
		if (!pathInSlide.startsWith(CoreConstants.WEBDAV_SERVLET_URI)) {
			pathInSlide = new StringBuilder(CoreConstants.WEBDAV_SERVLET_URI).append(pathInSlide).toString();
		}
		
		IWSlideService slide = getSlideService();
		if (slide == null) {
			return null;
		}
		
		InputStream stream = null;
		try {
			stream = slide.getInputStream(pathInSlide);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error getting InputStream for: " + pathInSlide, e);
		}
		if (stream == null) {
			return null;
		}
		
		String fileName = pathInSlide;
		int index = fileName.lastIndexOf(CoreConstants.SLASH);
		if (index != -1) {
			fileName = pathInSlide.substring(index + 1);
		}
		File file = new File(fileName);
		try {
			FileUtil.streamToFile(stream, file);
		} catch(Exception e) {
			LOGGER.log(Level.SEVERE, "Error streaming from " + pathInSlide + " to file: " + file.getName(), e);
		} finally {
			IOUtil.closeInputStream(stream);
		}
		
		return file;
	}
	
	private IWSlideService getSlideService() {
		if (slide == null) {
			try {
				slide = IBOLookup.getServiceInstance(IWMainApplication.getDefaultIWApplicationContext(), IWSlideService.class);
			} catch(Exception e) {
				LOGGER.log(Level.SEVERE, "Error getting " + IWSlideService.class, e);
			}
		}
		return slide;
	}
}
