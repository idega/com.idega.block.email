if (EmailSenderHelper == null) var EmailSenderHelper = {};

EmailSenderHelper.localizations = {
	sending:	'Sending...',
	error:		'Ooops... Some error occurred while sending email...',
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

EmailSenderHelper.init = function(){
	tinyMCE.init({
		// General options
		mode : "textareas",
		theme : "advanced",
		plugins : "autolink,lists,pagebreak,style,layer,table,save,advhr,advimage,advlink,emotions,iespell,insertdatetime,preview,media,searchreplace,print,contextmenu,paste,directionality,fullscreen,noneditable,visualchars,nonbreaking,xhtmlxtras,template,inlinepopups,autosave",

		theme_advanced_buttons1 : "save,newdocument,|,bold,italic,underline,strikethrough,|,justifyleft,justifycenter,justifyright,justifyfull,|,styleselect,formatselect,fontselect,fontsizeselect,cut,copy,paste,pastetext,pasteword,|,search,replace",
		theme_advanced_buttons2 :"bullist,numlist,|,outdent,indent,blockquote,|,undo,redo,|,link,unlink,anchor,image,cleanup,help,code,|,insertdate, inserttime,preview,|,forecolor,backcolor,tablecontrols,|",
		theme_advanced_buttons3 : "hr,removeformat,visualaid,|,sub,sup,|,charmap,emotions,iespell,media,advhr,|,print,|,ltr,rtl,|,fullscreen,insertlayer,moveforward,movebackward,absolute,|,styleprops,|,cite,abbr,acronym,del,ins,attribs,|,visualchars,nonbreaking,template,pagebreak,restoredraft",
		theme_advanced_toolbar_location : "top",
		theme_advanced_toolbar_align : "left",
		theme_advanced_statusbar_location : "bottom",
		width : "100%",
		height : "200px",
		//file_browser_callback : 'EmailSenderHelper.openFileBrowser',
		relative_urls: false

	});
	
}

function onLoad() { 
	var textAreaValue = EmailSenderHelper.getValueFromInput('input[type=\'hidden\'][name=\'textAreaValue\']', jQuery('#emailSenderFormId'));
	tinyMCE.get('emailSenderMessage').setContent(textAreaValue);
}

//EmailSenderHelper.openFileBrowser = function(field_name, url, type, win) {
//    tinyMCE.activeEditor.windowManager.open({
//        file : EmailSenderHelper.URI_TO_FILE_BROWSER,
//        title : 'My File Browser',
//        width : "300px",//document.body.offsetWidth * 0.8,  // Your dimensions may differ - toy around with them!
//        height : "150px",//document.body.offsetHeight * 0.8,
//        resizable : "yes",
//        inline : "yes",  // This parameter only has an effect if you use the inlinepopups plugin!
//        close_previous : "no"
//    }, {
//        window : win,
//        input : field_name
//    });
//    return false;
// }


EmailSenderHelper.setProperties = function(properties) {
	EmailSenderHelper.properties = properties;
	
	var container = jQuery('#emailSenderFormId');
	var keepEnabledFromField = EmailSenderHelper.getValueFromInput('input[type=\'hidden\'][name=\'allowChangeRecipientAddress\']', container);
	if (keepEnabledFromField != 'true' && EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderFrom\']', container) != null) {
		jQuery('input[type=\'text\'][name=\'emailSenderFrom\']').attr('disabled', 'disabled');
	}
}

EmailSenderHelper.proceedValidator = function() {
	jQuery.validator.addMethod('emails',
		function(value, element, params) {
			var tempValidator = this;
			
			value = value.replace(' ', '');
			var emails = value.split(',');
			if (emails == null || emails.length == 0) {
				return true;
			}
			
			var result = true;
			for (var i = 0; (i < emails.length && result); i++) {
				result = jQuery.validator.methods['email'].call( this, emails[i], element);
			}
			return result; 
		},
	EmailSenderHelper.localizations.enterValidEmail);
	
	var validator = jQuery('#emailSenderFormId').validate({
		rules: {
			emailSenderFrom: {
				required: true,
				email: true
			},
			emailSenderReplyTo: {
				emails: true
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
			}
		},
		messages: {
			emailSenderFrom: EmailSenderHelper.localizations.enterSenderEmail,
			emailSenderReplyTo: EmailSenderHelper.localizations.enterValidEmail,
			emailSenderTo: EmailSenderHelper.localizations.enterRecipientEmail,
			emailSenderCc: EmailSenderHelper.localizations.enterValidEmail,
			emailSenderBcc: EmailSenderHelper.localizations.enterValidEmail
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
	
	var replyTo = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderReplyTo\']', container);
	
	var recipientTo = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderTo\']', container);;
	var recipientCc = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderCc\']', container);;
	var recipientBcc = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderBcc\']', container);;
	
	var subject = EmailSenderHelper.getValueFromInput('input[type=\'text\'][name=\'emailSenderSubject\']', container);
	
	var message = EmailSenderHelper.getValueFromInput('textarea[name=\'emailSenderMessage\']', container);
	if (message == null) {
		message = tinyMCE.get('emailSenderMessage').getContent();
	}
	
	var attachment = null;
	
	showLoadingMessage(EmailSenderHelper.localizations.sending);
	EmailSender.sendMessage(EmailSenderHelper.getMessageParametersObject(from, replyTo, recipientTo, recipientCc, recipientBcc, subject, message, attachment), {
		callback: function(result) {
			closeAllLoadingMessages();
			
			result += '';
			if (result == 'false') {
				humanMsg.displayMsg(EmailSenderHelper.localizations.error);
				return;
			}
			
			if (typeof FileUploadHelper != 'undefined') {
				FileUploadHelper.removeAllUploadedFiles();
			}
			
			humanMsg.displayMsg(EmailSenderHelper.localizations.success, {
				callback: function() {
					try {
						window.parent.jQuery.fancybox.close();
					} catch(e) {}
				},
				timeout: 1500
			});
			
			jQuery('input[type=\'text\']', container).attr('value', '');
			jQuery('textarea', container).attr('value', '');
		}, errorHandler: function() {
			closeAllLoadingMessages();
			humanMsg.displayMsg(EmailSenderHelper.localizations.error);
			return;
		}
	});
}

EmailSenderHelper.getMessageParametersObject = function(from, replyTo, recipientTo, recipientCc, recipientBcc, subject, message, attachment) {
	var parameters = {
		senderName: EmailSenderHelper.getValueFromInput('input[type=\'hidden\'][name=\'emailSenderFullName\']', jQuery('#emailSenderFormId')),
		from: from || null,
	
		replyTo: replyTo || null,
	
		recipientTo: recipientTo || null,
		recipientCc: recipientCc || null,
		recipientBcc: recipientBcc || null,
	
		subject: subject || null,
		message: message || null,
	
		attachments: FileUploadHelper.allUploadedFiles || null,
		properties: EmailSenderHelper.properties,
		
		saveMessageIntoDB: EmailSenderHelper.getValueFromInput('input[type=\'hidden\'][name=\'saveMessageIntoDB\']', jQuery('#emailSenderFormId'))
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