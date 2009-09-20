var MailingListHelper = {};

MailingListHelper.convertToCapitalLetters = function(inputId) {
	var value = jQuery('#' + inputId).attr('value');
	value = value.toUpperCase();
	jQuery('#' + inputId).attr('value', value);
}

MailingListHelper.confirmMailingListToBeDeleted = function(message, uri) {
	if (window.confirm(message)) {
		window.location.href = uri;
	} else {
		return false;
	}
}