// 'use strict';

define(
[
	"dojo/_base/declare",
	"dojo/_base/lang",

	"dojo/dom",
	"dojo/keys", // keys.DOWN_ARROW keys.ENTER keys.ESCAPE
	"dojo/on",
	"dojo/request/xhr",
	"dojo/store/Memory",
	"dojo/window",

	"dijit/form/FilteringSelect",

	'./SearchMgr',
	'dojo/i18n!./nls/Messages'
], //
function(
//
declare, //
lang, //

dom, //
keys, //
on, //
xhr, //
Memory, //
winUtils, //

FilteringSelect, //

SearchMgr, //
Messages //
) {

	var SearchUI = declare("tourbook.search.SearchInput",
	[ FilteringSelect
	], {

		_loadProposals : function _loadProposals(xhrSearchText) {

			if (xhrSearchText) {
				xhrSearchText = xhrSearchText.trim();
			}

			if (!xhrSearchText) {
				console.info("Search text is empty.");
				return;
			}

			var self = this;

			var query = {};
			query[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_PROPOSALS;
			query[SearchMgr.XHR_PARAM_SEARCH_TEXT] = encodeURIComponent(xhrSearchText);

			xhr(SearchMgr.XHR_SEARCH_HANDLER, {

				query : query,
				handleAs : "json",
				preventCache : true,
				timeout : SearchMgr.XHR_TIMEOUT

			}).then(function(xhrData) {

				var newStore = new Memory({
					data : xhrData
				});

				// overwrite store in dijit/form/ComboBoxMixin
				self.store = newStore;

//				console.debug("proposal received");

			}, function(err) {

				// Handle the error condition
				console.error("error: " + err);
			});
		},

		/**
		 * Get search results for the current search text.
		 */
		startSearch : function startSearch(isForceRefresh) {

			// show selected item

			var newSearchUrl = this.createSearchUrl();

			console.warn("startSearch '" + newSearchUrl + "'");

			// check if loading is needed
			if (isForceRefresh || this._currentSearchUrl !== newSearchUrl) {

				// keep current search
				this._currentSearchUrl = newSearchUrl;

				var grid = this._grid;
				
				// set no data message when a search is started manually
				grid.noDataMessage = Messages.searchGrid_Label_NoDataMessage;

				// overwrite store url
				grid.collection.target = newSearchUrl;

				grid.refresh();
			}
		},

		_onKeyUp : function _onKeyUp(event) {

//			console.info("_onKeyUp '" + this.getSearchText() + "'");

			var searchText = this.getSearchText();

			// load suggestions for the entered value
			if (searchText !== this._lastSearchText) {

				// prevent that it is call TWICE
				event.stopPropagation();
				event.preventDefault();

				this._lastSearchText = searchText;

				this._loadProposals(searchText);
			}

			// load results only with the <Enter> key
			if (event.keyCode == keys.ENTER) {
				this.startSearch();
			}
		},

		/**
		 * Returns text which is entered for searching.
		 */
		getSearchText : function getSearchText() {
			return this.get('displayedValue').trim();
		},

		/**
		 * Set text into the search field.
		 */
		setSearchText : function setSearchText(value) {
			this.set('displayedValue', value);
		},

		createSearchUrl : function createSearchUrl() {

			var searchText = this.getSearchText();

			var actionSearch = SearchMgr.XHR_PARAM_ACTION + "=" + SearchMgr.XHR_ACTION_SEARCH;
			var paramSearchText = "&" + SearchMgr.XHR_PARAM_SEARCH_TEXT + "=" + encodeURIComponent(searchText);

			var url = SearchMgr.XHR_SEARCH_HANDLER + '?' + actionSearch + paramSearchText;

			return url;
		},

		// hide validation checker
		isValid : function() {
			return true;
		},

		log : function(logText) {

			dom.byId("domLog").innerHTML = logText;
		},

		onChange : function(value) {
			/*
			 * THIS IS NOT WORKING PROPERLY :-(
			 */
//			this._onChange(value);
			console.warn("onChange '" + this.getSearchText() + "'");
		},

		postCreate : function() {

			this.inherited(arguments);

			on(this.domNode, "keyup", lang.hitch(this, "_onKeyUp"));
		},

		resize : function() {

			this.inherited(arguments);

			// overwrite dijit._HasDropDown
			// set max height smaller for the dropdown box that the a scollbar of the body is not displayed
			var viewport = winUtils.getBox(this.ownerDocument);
			this.maxHeight = viewport.h * 0.90;

			// overwrite dijit._HasDropDown
			// force with to the max proposal with
			this.autoWidth = false;
		},

		/**
		 * Select all text in the input field.
		 */
		selectAll : function selectAll() {

			this.textbox.select();
		},

		setGrid : function(grid) {
			this._grid = grid;
		}
	});

	return SearchUI;
});
