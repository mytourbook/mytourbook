define(
[
	'dojo/_base/declare',
	"dojo/_base/lang",
	'dojo/dom',
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
dom, //
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

		constructor : function(args) {
			this._searchApp = args.searchApp;
		},

		postCreate : function() {

			this.inherited(arguments);

			// hide dialog when mouse has leaved it
			on(this.domNode, "mouseleave", lang.hitch(this, "hideDialog"));

			/*
			 * Tooltips with html tags must be defined in the js code, otherwise the tags do not work.
			 */
			this.apChk_EaseSearching_Tooltip.label = Messages.Search_Options_Checkbox_EaseSearching_Tooltip;
		},

		/**
		 * Overwrite BaseDialog.showDialog and restore the state of the UI.
		 */
		showDialog : function showDialog(args) {

			this.inherited(arguments);

			this._restoreState();
		},

		apActionRestoreDefaults : function apActionRestoreDefaults() {

			this._setSearchOptions(//
			{
				isRestoreDefaults : true
			});
		},

		/**
		 * Search all checkbox
		 */
		apSearchAll : function apSearchAll() {

			this._enableControls();

			// fire selection
			this.apSelection();
		},

		/**
		 * Selection is from an attach point.
		 */
		apSelection : function apSelection() {

			if (this._isValid()) {

				var searchOptions = //
				{
					isEaseSearching : this.apChkEaseSearching.get('checked'),

					isShowContentAll : this.apChkShowContentAll.get('checked'),
					isShowContentTour : this.apChkShowContentTour.get('checked'),
					isShowContentMarker : this.apChkShowContentMarker.get('checked'),
					isShowContentWaypoint : this.apChkShowContentWaypoint.get('checked'),

					isSortByDateAscending : this.apSortByDateAscending.get('checked'),

					isShowDateTime : this.apChkShowDateTime.get('checked'),
					isShowItemNumber : this.apChkShowItemNumber.get('checked'),
					isShowLuceneID : this.apChkShowLuceneID.get('checked')
				};

				this._setSearchOptions(searchOptions);
			}
		},

		_isValid : function _isValid() {

			var //
			statusText = '', //
			isValid = true, //

			isShowContentAll = this.apChkShowContentAll.get('checked'), //
			isShowContentTour = this.apChkShowContentTour.get('checked'), //
			isShowContentMarker = this.apChkShowContentMarker.get('checked'), //
			isShowContentWaypoint = this.apChkShowContentWaypoint.get('checked');

			if (isShowContentAll) {

				// content is valid

			} else {

				// at least one content must be checked

				if (isShowContentTour == false //
					&& isShowContentMarker == false //
					&& isShowContentWaypoint == false) {

					statusText = Messages.Search_Validation_SearchFilter;
					isValid = false;
				}
			}

			// update status
			dom.byId('domSearchStatus').innerHTML = statusText;

			return isValid;
		},

		_enableControls : function _enableControls() {

			var isShowContentAll = this.apChkShowContentAll.get('checked');

			this.apChkShowContentTour.set('disabled', isShowContentAll);
			this.apChkShowContentMarker.set('disabled', isShowContentAll);
			this.apChkShowContentWaypoint.set('disabled', isShowContentAll);
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
				timeout : SearchMgr.XHR_TIMEOUT,

				query : xhrQuery

			}).then(function(xhrData) {

				_this._updateUI_FromState(_this, xhrData);
			});
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
				timeout : SearchMgr.XHR_TIMEOUT,

				query : xhrQuery

			}).then(function(xhrData) {

				if (xhrData.isSearchOptionsDefault) {

					// set defaults in the UI
					_this._updateUI_FromState(_this, xhrData);
				}

				// repeat previous search

				_this._searchApp._searchInput.startSearch(true);
			});
		},

		_updateUI_FromState : function _updateUI_FromState(dialog, xhrData) {

			dialog.apChkEaseSearching.set('checked', xhrData.isEaseSearching);

			dialog.apChkShowContentAll.set('checked', xhrData.isShowContentAll);
			dialog.apChkShowContentTour.set('checked', xhrData.isShowContentTour);
			dialog.apChkShowContentMarker.set('checked', xhrData.isShowContentMarker);
			dialog.apChkShowContentWaypoint.set('checked', xhrData.isShowContentWaypoint);

			dialog.apSortByDateAscending.set('checked', xhrData.isSortByDateAscending);
			dialog.apSortByDateDescending.set('checked', !xhrData.isSortByDateAscending);

			dialog.apChkShowDateTime.set('checked', xhrData.isShowDateTime);
			dialog.apChkShowItemNumber.set('checked', xhrData.isShowItemNumber);
			dialog.apChkShowLuceneID.set('checked', xhrData.isShowLuceneID);

			dialog._enableControls();
			dialog._isValid();
		}

	});

	return dlgSearchOptions;

});