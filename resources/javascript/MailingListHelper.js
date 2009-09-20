var MailingListHelper = {};

MailingListHelper.convertToCapitalLetters = function(inputId) {
	var value = jQuery('#' + inputId).attr('value');
	value = value.toUpperCase();
	jQuery('#' + inputId).attr('value', value);
}