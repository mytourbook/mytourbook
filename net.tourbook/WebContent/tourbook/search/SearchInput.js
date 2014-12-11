// 'use strict';

define(
	[
		"dojo/_base/declare",
		"dojo/_base/lang",
		"dojo/dom",
		"dojo/on",
		"dojo/request/xhr",
		"dojo/store/Memory",
		"dijit/form/FilteringSelect",
		'./SearchMgr.js',
	], //
	function(declare,
		lang,
		dom,
		on,
		xhr,
		Memory,
		FilteringSelect,
		SearchMgr,
		zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzZZZZZZZZz) {

		var SearchUI = declare("tourbook.search.SearchInput",
		[
			FilteringSelect,
		], {

			_loadData : function loadData(xhrSearchText) {

				if (xhrSearchText) {
					xhrSearchText = xhrSearchText.trim();
				}

				if (!xhrSearchText) {
					console.info("Search text is empty.");
					return;
				}

				console.info("_loadData: " + xhrSearchText);

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

					console.debug("proposal received");

				}, function(err) {

					// Handle the error condition
					console.error("error: " + err);
				})
			},

			_onChange : function onChange(event) {

				// show selected item

				console.info("onChange");

				var grid = this._grid;

				grid.store.target = this.getSearchUrl();

				grid.model.clearCache();
				grid.body.refresh()
			},

			_onKeyPress : function _onKeyPress(event) {

				this._canLoadData = true;
			},

			_onKeyUp : function _onKeyUp(evt) {

				// load suggestions for the entered value

				console.info("_onKeyUp");

				if (this._canLoadData) {

					this._canLoadData = false;

					var searchText = this.getSearchText();

					if (searchText !== this._lastSearchText) {

						// prevent that it is call TWICE
						evt.stopPropagation();
						evt.preventDefault();

						this._lastSearchText = searchText;

						this._loadData(searchText);

					} else {

						this._onChange();
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

			getSearchUrl : function() {

				var searchText = this.getSearchText();

				var actionSearch = SearchMgr.XHR_PARAM_ACTION + "=" + SearchMgr.XHR_ACTION_SEARCH;
				var paramSearchText = "&" + SearchMgr.XHR_PARAM_SEARCH_TEXT + "=" + encodeURIComponent(searchText);

				var url = SearchMgr.XHR_SEARCH_HANDLER + '?' + actionSearch + paramSearchText;

				console.info("store: " + url);

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

			setGrid : function(grid) {
				this._grid = grid;
			},
		});

		return SearchUI;
	});
