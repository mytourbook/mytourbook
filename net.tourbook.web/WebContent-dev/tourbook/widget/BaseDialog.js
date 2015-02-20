define(
[
	'dojo/_base/declare',
	"dojo/dom-class",
	"dojo/dom-geometry",
	"dojo/dom-style",
	"dojo/window",

	'dijit/_TemplatedMixin',
	'dijit/_WidgetBase',
	'dijit/_WidgetsInTemplateMixin',
	'dijit/Dialog'

], function(//
//
declare, //
domClass, //
domGeometry, //
domStyle, //
winUtils, //

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

		/**
		 * Counter how many tooltips are opened, this dialog will not be closed when >0.
		 */
		openedTooltips : 0,

		createDialog : function createDialog(args) {

			this.openedTooltips = 0;

			if (this._dialog == null) {

				var layoutParent = args.layoutParent;

				this._dialog = new Dialog(dojo.mixin({

					content : this,

					/**
					 * Overwrite _position in dijit._DialogBase to pin the dialog to the layoutParent node.
					 */
					_position : function _position() {
						// summary:
						//		Position the dialog in the viewport.  If no relative offset
						//		in the viewport has been determined (by dragging, for instance),
						//		center the dialog.  Otherwise, use the Dialog's stored relative offset,
						//		adjusted by the viewport's scroll.

						if (layoutParent) {

							// pin this dialog to the bottom of the layoutParent

							if (!domClass.contains(this.ownerDocumentBody, "dojoMove")) { // don't do anything if called during auto-scroll

								var domDialog = this.domNode, //

								viewport = winUtils.getBox(this.ownerDocument), //

								dialogBounds = domGeometry.position(domDialog), //
								parentBounds = domGeometry.position(layoutParent.domNode);

								l = Math.floor(viewport.w - dialogBounds.w - 1), //
								t = Math.floor(parentBounds.y + parentBounds.h);

								domStyle.set(domDialog, //
								{
									left : l + "px",
									top : t + "px"
								});
							}

						} else {

							// do the original _position implementation

							this.inherited(arguments);
						}
					}

				}, args));
			}

			return this._dialog;
		},

		showDialog : function showDialog(args) {

			this.createDialog(args);

			this._dialog.show();
		},

		hideDialog : function(event) {

			if (this._dialog != null) {

//				console.log("this.openedTooltips: " + this.openedTooltips);

				/*
				 * THIS IS ABSOLUTELY NOT WORKING PROPERLY, but sometimes, therefore it is enabled, with the close button
				 * the dialog can be hidden in all cases.
				 */
				if (this.openedTooltips > 0) {
					return;
				}

				this._dialog.hide();
			}
		},

		destroyDialog : function() {
			if (this._dialog != null) {
				this._dialog.destroy();
			}
		}

	});

});