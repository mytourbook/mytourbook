define(
[
	'dojo/_base/declare',
	"dojo/_base/lang",
	'dojo/on',
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
lang, //
on, //
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

		// create messages field which is needed that messages can be accessed in the template
		messages : Messages,

		postCreate : function() {

			this.inherited(arguments);

			// hide dialog when mouse has leaved it
			on(this.domNode, "mouseleave", lang.hitch(this, "hideDialog"));
		},

		/**
		 * Overwrite BaseDialog.showDialog and restore the state of the UI.
		 */
		showDialog : function showDialog() {

			this._searchApp = arguments[0].searchApp;

			this.inherited(arguments);

			this._restoreState();
		},

		apActionRestoreDefaults : function apActionRestoreDefaults() {

			this._setSearchOptions({

				isRestoreDefaults : true
			});
		},

		/**
		 * Selection is from an attach point.
		 */
		apSelection : function apSelection() {

			var searchOptions = //
			{
				isSortByDateAscending : apSortByDateAscending.checked,

				isShowDateTime : apChkShowDateTime.checked,
				isShowItemNumber : apChkShowItemNumber.checked,
				isShowLuceneID : apChkShowLuceneID.checked
			};

			this._setSearchOptions(searchOptions);
		},

		/**
		 * Set search options in the backend and reload current search with new search options.
		 */
		_setSearchOptions : function _setSearchOptions(searchOptions) {

			var _this = this;

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
					_this._updateUIFromState(_this, xhrData);
				}

				// repeat previous search

				_this._searchApp._searchInput.loadSearchResults(true);
			});
		},

		/**
		 * 
		 */
		_restoreState : function _restoreState(callBack) {

			var _this = this;

			var xhrQuery = {};
			xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_GET_SEARCH_OPTIONS;

			xhr(SearchMgr.XHR_SEARCH_HANDLER, {

				handleAs : 'json',
				preventCache : true,
				timeout : 5000,

				query : xhrQuery

			}).then(function(xhrData) {

				_this._updateUIFromState(_this, xhrData);
			});
		},

		_updateUIFromState : function _updateUIFromState(dialog, xhrData) {

			dialog.apSortByDateAscending.set('checked', xhrData.isSortByDateAscending);
			dialog.apSortByDateDescending.set('checked', !xhrData.isSortByDateAscending);

			dialog.apChkShowDateTime.set('checked', xhrData.isShowDateTime);
			dialog.apChkShowItemNumber.set('checked', xhrData.isShowItemNumber);
			dialog.apChkShowLuceneID.set('checked', xhrData.isShowLuceneID);
		}

	});

	return dlgSearchOptions;

});