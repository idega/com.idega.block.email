if (EmailSenderHelper == null) var EmailSenderHelper = {};

EmailSenderHelper.localizations = {
	sending:	'Sending...',
	error:		'Ooops... Some error occured while sending email...',
	success:	'E-mail was successfully sent',
	enterSenderEmail:		'Please enter a valid sender email address',
	enterRecipientEmail:	'Please enter a valid recipient email address',
	enterValidEmail:		'Please enter a valid email address',
	enterSubject:			'Please enter subject',
	enterMessage:			'Please enter some message'
}

EmailSenderHelper.properties = null;

EmailSenderHelper.setLocalizations = function(localizations) {
	EmailSenderHelper.localizations = localizations;
}

EmailSenderHelper.setProperties = function(properties) {
	EmailSenderHelper.properties = properties;
}

EmailSenderHelper.proceedValidator = function() {
	var validator = jQuery('#emailSenderFormId').validate({
		rules: {
			emailSenderFrom: {
				required: true,
				email: true
			},
			emailSenderTo: {
				required: true,
				email: true
			},
			emailSenderCc: {
				email: true
			},
			emailSenderBcc: {
				email: true
			},
			emailSenderSubject: "required",
			emailSenderMessage: "required"
		},
		messages: {
			emailSenderFrom: EmailSenderHelper.localizations.enterSenderEmail,
			emailSenderTo: EmailSenderHelper.localizations.enterRecipientEmail,
			emailSenderCc: EmailSenderHelper.localizations.enterValidEmail,
			emailSenderBcc: EmailSenderHelper.localizations.enterValidEmail,
			emailSenderSubject: EmailSenderHelper.localizations.enterSubject,
			emailSenderMessage: EmailSenderHelper.localizations.enterMessage
		}
	});
	
	var isValid = validator.form();
	if (isValid == 1 || isValid == 'true') {
		EmailSenderHelper.proceedSendingMessage();
	} else {
		return false;
	}
}

EmailSenderHelper.sendMessage = function(event) {
	if (event == null) {
		humanMsg.displayMsg(EmailSenderHelper.localizations.error);
		return;
	}
	
	EmailSenderHelper.proceedValidator();
}

EmailSenderHelper.proceedSendingMessage = function() {
	var container = jQuery('#emailSenderFormId');
	
	var from = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderFrom\']', container);
	
	var recipientTo = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderTo\']', container);;
	var recipientCc = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderCc\']', container);;
	var recipientBcc = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderBcc\']', container);;
	
	var subject = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderSubject\']', container);
	var message = EmailSenderHelper.getValueFromInput('textarea[name=\'emailSenderMessage\']', container);
	
	var attachment = null;
	
	showLoadingMessage(EmailSenderHelper.localizations.sending);
	EmailSender.sendMessage(EmailSenderHelper.getMessageParametersObject(from, recipientTo, recipientCc, recipientBcc, subject, message, attachment), {
		callback: function(result) {
			closeAllLoadingMessages();
			if (result == null || result == 'false') {
				humanMsg.displayMsg(EmailSenderHelper.localizations.error);
				return;
			}
			
			humanMsg.displayMsg(EmailSenderHelper.localizations.success);
			
			jQuery('input[type=\'text\']', container).attr('value', '');
			jQuery('textarea', container).attr('value', '');
			
			if (typeof FileUploadHelper != 'undefined') {
				FileUploadHelper.removeAllUploadedFiles();
			}
		}, errorHandler: function() {
			closeAllLoadingMessages();
			humanMsg.displayMsg(EmailSenderHelper.localizations.error);
			return;
		}
	});
}

EmailSenderHelper.getMessageParametersObject = function(from, recipientTo, recipientCc, recipientBcc, subject, message, attachment) {
	var parameters = {
		senderName: EmailSenderHelper.getValueFromInput('input[type=\'hidden\'][name=\'emailSenderFullName\']', jQuery('#emailSenderFormId')),
		from: from || null,
	
		recipientTo: recipientTo || null,
		recipientCc: recipientCc || null,
		recipientBcc: recipientBcc || null,
	
		subject: subject || null,
		message: message || null,
	
		attachments: FileUploadHelper.allUploadedFiles || null,
		properties: EmailSenderHelper.properties
	}
	return parameters;
}

EmailSenderHelper.getValueFromInput = function(filter, container) {
	var objects = jQuery(filter, container);
	if (objects == null || objects.length == 0) {
		return null;
	}
	
	var value = jQuery(objects[objects.length -1]).attr('value');
	return value == null ? null : value == '' ? null : value;
}