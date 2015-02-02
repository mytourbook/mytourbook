define(
[
	'dojo/_base/declare',

	'dijit/form/NumberSpinner',
	'dijit/form/RadioButton',
	'dijit/TitlePane',

	'../widget/BaseDialog',
	'dojo/text!./DialogSearchOptions.html',
	'dojo/i18n!./nls/Messages'

], function(//
//	
declare, //

// must be preloaded when used in the template
NumberSpinner, //
RadioButton, //
TitlePane,

BaseDialog, //
template, //
Messages //
//
) {

	var dlgSearchOptions = declare('tourbook.search.DialogSearchOptions',
	[ BaseDialog
	], {

		templateString : template,

		// create field which is needed that the messages can be accessed
		messages : Messages,

		create : function() {
			return this.inherited(arguments);
		},

		onSelection : function onSelection() {

			alert("onSelection()");
		}

//		actionRegister : function() {
//
//			var form = dijit.byId('dialogRegisterForm');
//			if (form.validate()) {
////				Auth.register(form.get('value'));
//			}
//		},

//		actionCancel : function() {
//			this.hideDialog();
//		}
	});

	return dlgSearchOptions;

});