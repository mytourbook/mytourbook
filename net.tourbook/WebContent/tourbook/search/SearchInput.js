// 'use strict';

define(
[
	"dojo/_base/declare",
	"dojo/_base/lang",

	"dojo/dom",
	"dojo/on",
	"dojo/request/xhr",
	"dojo/store/Memory",
	"dojo/window",

	"dijit/form/FilteringSelect",
	'./SearchMgr.js',
], //
function(//
declare, lang, //

dom, //
on, //
xhr, //
Memory, //
winUtils, //

FilteringSelect, //
SearchMgr //
) {

	var SearchUI = declare("tourbook.search.SearchInput",
	[
		FilteringSelect,
	], {

		_currentSearchUrl : null,

		_loadData : function loadData(xhrSearchText) {

			if (xhrSearchText) {
				xhrSearchText = xhrSearchText.trim();
			}

			if (!xhrSearchText) {
				console.info("Search text is empty.");
				return;
			}

//				console.info("_loadData: " + xhrSearchText);

			var t = this;

			var query = {};
			query[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_PROPOSALS;
			query[SearchMgr.XHR_PARAM_SEARCH_TEXT] = encodeURIComponent(xhrSearchText);

			xhr(SearchMgr.XHR_SEARCH_HANDLER, {

				handleAs : "json",
				preventCache : true,
				timeout : 60000,

				query : query

			}).then(function(xhrData) {

				var newStore = new Memory({
					data : xhrData
				});

				t.store = newStore;

//					console.debug("proposal received");

			}, function(err) {

				// Handle the error condition
				console.error("error: " + err);
			})
		},

		_onChange : function onChange(evt) {

			// show selected item

			var newSearchUrl = this.getSearchUrl();

			if (this._currentSearchUrl !== newSearchUrl) {

				console.info("onChange: " + newSearchUrl);

				this._currentSearchUrl = newSearchUrl;

				var grid = this._grid;

				grid.collection.target = newSearchUrl;
				grid.refresh();
			}
		},

		_onKeyPress : function _onKeyPress(event) {

			this._canLoadData = true;
		},

		_onKeyUp : function _onKeyUp(event) {

			// load suggestions for the entered value

			console.info("_onKeyUp");

			var isLoading = false;

			if (this._canLoadData) {

				this._canLoadData = false;

				var searchText = this.getSearchText();

				if (searchText !== this._lastSearchText) {

					// prevent that it is call TWICE
					event.stopPropagation();
					event.preventDefault();

					this._lastSearchText = searchText;

					this._loadData(searchText);

					isLoading = true;
				}
			}

			if (isLoading === false) {

				if (event.defaultPrevented) {
					return; // Should do nothing if the key event was already consumed.
				}

				switch (event.key) {
				case "ArrowDown":
				case "ArrowUp":

					// open popup
					
					this.loadDropDown();
					
					// Consume the event for suppressing "double action".
					event.preventDefault();

					break;
				}
			}
		},

		_onKeyDown : function _onKeyDown(evt) {

			// load suggestions for the entered value

			console.info("_onKeyDown");
		},

		getSearchText : function getSearchText() {
			return this.get('displayedValue');
		},

		getSearchUrl : function getSearchUrl() {

			var searchText = this.getSearchText();

			var actionSearch = SearchMgr.XHR_PARAM_ACTION + "=" + SearchMgr.XHR_ACTION_SEARCH;
			var paramSearchText = "&" + SearchMgr.XHR_PARAM_SEARCH_TEXT + "=" + encodeURIComponent(searchText);

			var url = SearchMgr.XHR_SEARCH_HANDLER + '?' + actionSearch + paramSearchText;

//				console.info("store: " + url);

			return url;
		},

		// hide validation checker
		isValid : function() {
			return true;
		},

		log : function(logText) {

			dom.byId("domLog").innerHTML = logText;
		},

		postCreate : function() {

			this.inherited(arguments);

			on(this.domNode, "change", lang.hitch(this, "_onChange"));
			on(this.domNode, "keypress", lang.hitch(this, "_onKeyPress"));
			on(this.domNode, "keydown", lang.hitch(this, "_onKeyDown"));
			on(this.domNode, "keyup", lang.hitch(this, "_onKeyUp"));
		},

		resize : function() {

			this.inherited(arguments);

			// set max height smaller for the dropdown box that the a scollbar of the body is not displayed
			var viewport = winUtils.getBox(this.ownerDocument);
			this.maxHeight = viewport.h * 0.95;
		},

		setGrid : function(grid) {
			this._grid = grid;
		},
	});

	return SearchUI;
});
