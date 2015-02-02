define(
[
	'dojo/_base/declare',
	'dijit/_TemplatedMixin',
	'dijit/_WidgetBase',
	'dijit/_WidgetsInTemplateMixin',
	'dijit/Dialog'
], function(//
//
declare, //
_TemplatedMixin, //
_WidgetBase, //
_WidgetsInTemplateMixin, //
Dialog //
//
) {

	return declare('tourbook.widget.BaseDialog',
	[
		_WidgetBase, // super class
		_TemplatedMixin,
		_WidgetsInTemplateMixin
	], {

		_dialog : null,

		createDialog : function createDialog(args) {
	
			if (this._dialog == null)
				
				this._dialog = new Dialog(dojo.mixin({
					content : this
				}, args));
			
			return this._dialog;
		},

		showDialog : function showDialog(args) {

			this.createDialog(args);

			this._dialog.show().then(function() {

				//dialog is created with empty content, that makes dialog has wrong size
//				this._dialog.resize();
			});
		},

		hideDialog : function() {
			if (this._dialog != null)
				this._dialog.hide();
		},

		destroyDialog : function() {
			if (this._dialog != null)
				this._dialog.destroy();
		}

	});

});