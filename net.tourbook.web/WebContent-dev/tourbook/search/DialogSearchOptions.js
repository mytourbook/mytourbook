define(
[
	'dojo/_base/declare',
	"dojo/request/xhr",

	'dijit/form/NumberSpinner',
	'dijit/form/RadioButton',
	'dijit/TitlePane',

	'../widget/BaseDialog',
	'./SearchMgr',

	'dojo/text!./DialogSearchOptions.html',
	'dojo/i18n!./nls/Messages'

], function(//
//	
declare, //
xhr, //

// these widgets MUST be preloaded when used in the template
NumberSpinner, //
RadioButton, //
TitlePane,

BaseDialog, //
SearchMgr, //

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

		actionRestoreDefaults : function actionRestoreDefaults() {

			this._setSearchOptions({

				isRestoreDefaults : true
			});
		},

		create : function() {
			return this.inherited(arguments);
		},

		onSelection : function onSelection() {

			this._setSearchOptions({

				isSortByDateAscending : apSortByDateAscending.checked,

				isShowDateTime : apChkShowDateTime.checked,
				isShowItemNumber : apChkShowItemNumber.checked,
				isShowLuceneID : apChkShowLuceneID.checked
			});
		},

		_setSearchOptions : function _setSearchOptions(searchOptions) {

			var self = this;

			var jsonSearchOptions = JSON.stringify(searchOptions);

			var xhrQuery = {};
			xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SET_SEARCH_OPTIONS;
			xhrQuery[SearchMgr.XHR_PARAM_SEARCH_OPTIONS] = encodeURIComponent(jsonSearchOptions);

			xhr(SearchMgr.XHR_SEARCH_HANDLER, {

				handleAs : 'json',
				preventCache : true,
				timeout : 5000,

				query : xhrQuery

			}).then(function(xhrData) {

				if (xhrData.isSearchOptionsDefault) {

					// set defaults in the UI

					self.apSortByDateAscending.set('checked', xhrData.isSortByDateAscending);
					self.apSortByDateDescending.set('checked', !xhrData.isSortByDateAscending);

					self.apChkShowDateTime.set('checked', xhrData.isShowDateTime);
					self.apChkShowItemNumber.set('checked', xhrData.isShowItemNumber);
					self.apChkShowLuceneID.set('checked', xhrData.isShowLuceneID);
				}

				// repeat previous search

			}, function(err) {

				// Handle the error condition
				console.error("error: " + err);
			});
		}

	});

	return dlgSearchOptions;

});