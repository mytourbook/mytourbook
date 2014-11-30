/**
 * 
 */
"use strict";

require(
[
		"dojo/_base/declare",
		"dojo/dom",
		"dojo/request/xhr",
		"dojo/store/Memory",
		//
		"dijit/form/FilteringSelect",
		//
		"dojo/domReady!"
], //
function(declare,
		dom,
		xhr,
		Memory,
		FilteringSelect,
		zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz) {

	SearchMgr.prototype = {

		XHR_SEARCH_HANDLER : "/xhrSearch",

		XHR_PARAM_NAME_ACTION : "ACTION",
		XHR_ACTION_SEARCH : "Search",
		XHR_ACTION_PROPOSALS : "Proposals",

		createUI : function() {

			var self = this;

			this.filteringSelect = new FilteringSelect({

				id : "searchInput",
				name : "idSearch",
				placeHolder : "Search Tours, Marker and Waypoints",
				required : false,

				store : null,

				onKeyUp : function(value) {

					// load suggestions for the entered value

					var enteredSearchText = this.get('displayedValue');
					self.loadData(enteredSearchText);
				},

				onChange : function onChange(value) {

					// show selected item

					dom.byId("domSelectedItem").innerHTML = "onChange: " + value;
				}

			}, "domSearchInput");

			this.filteringSelect.startup();

		},

		loadData : function loadData(xhrSearchText) {

			if (xhrSearchText) {
				xhrSearchText = xhrSearchText.trim();
			}

			if (!xhrSearchText) {
				return;
			}

			xhr(this.XHR_SEARCH_HANDLER, {

				handleAs : "json",
				preventCache : true,
				timeout : 5000,

				query : {
//					action : this.XHR_ACTION_SEARCH,
					ACTION : this.XHR_ACTION_PROPOSALS,
					searchText : xhrSearchText,
				}

			}).then(function(xhrData) {

				var store = new Memory({
					data : xhrData
				});

				var searchField = dijit.byId("searchInput");
				searchField.set('store', store);

				dom.byId("domResult").innerHTML = "XHR " + (new Date()).getTime() + "<br> " + xhrData;

				console.log('store set');

			}, function(err) {

				// Handle the error condition
				console.log("error: " + err);
			})
		},

	}

	new SearchMgr();

	// Manage MT search feature.
	function SearchMgr() {

		this.createUI();

		this.loadData();

		// set focus to the search field
		dijit.byId('searchInput').focus();
	}

//	SearchMgr.createUI();
//
//	// set focus to the search field
//	dijit.byId('searchInput').focus();
//
//	// Startup searching
//	SearchMgr.createUI = function createUI() {
//
//		var filteringSelect = new FilteringSelect({
//
//			id : "searchInput",
//			name : "idSearch",
//			placeHolder : "Search Tours, Marker and Waypoints",
//
//			store : null,
//
//			onKeyUp : function(value) {
//
//				if (dojo.byId("el_id").value.length > 4 && dijit.byId("el_id").get("store") == testStore) {
//					dijit.byId("el_id").set("store", leStore);
//				}
//
//				if (dojo.byId("el_id").value.length <= 4 && dijit.byId("el_id").get("store") == leStore) {
//					dijit.byId("el_id").set("store", testStore);
//				}
//			},
//
//			onChange : function onChange(val) {
//
//				// show selected item
//
//				dom.byId("domSelectedItem").innerHTML = val;
//			}
//
//		}, "domSearchInput");
//
//		xhr("/xhrSearch", {
//			handleAs : "json",
//			preventCache : true,
//			timeout : 5000,
//
//		}).then(function(xhrData) {
//
//			var store = new Memory({
//				data : xhrData
//			});
//
//			var searchField = dijit.byId("searchInput");
//			searchField.set('store', store);
//			searchField.startup();
//
//			console.log('store set');
//
//		}, function(err) {
//
//			// Handle the error condition
//			console.log("error: " + err);
//		})
//	}
});

//
/////////////////////////////////////////////////////////////////////////////////////////////////////
//
//var a = {
//	"identifier" : "id",
//	"label" : "name",
//	"items" :
//	[
//			{
//				"id" : "9",
//				"name" : "Alberta"
//			},
//			{
//				"id" : "4",
//				"name" : "New Brunswick"
//			},
//			{
//				"id" : "11",
//				"name" : "Northwest Territories"
//			}
//	]
//}
//
/////////////////////////////////////////////////////////////////////////////////////////////////////
//
//var serverStore =
//[
//		{
//			"NAME" : "Andres",
//			"id" : "00000000",
//			"label" : "<h3>00000000<\/h3>\tAndres Vargas"
//		},
//		{
//			"NAME" : "Jose",
//			"id" : "11111111",
//			"label" : "<h3>11111111<\/h3>\tJose Mourinho"
//		},
//		{
//			"NAME" : "Scott",
//			"id" : "22222222",
//			"label" : "<h3>22222222<\/h3>\tScott Bonen"
//		},
//		{
//			"NAME" : "Paulina",
//			"id" : "33333333",
//			"label" : "<h3>33333333<\/h3>\tPaulina Gonzalez"
//		},
//		{
//			"NAME" : "Leticia",
//			"id" : "44444444",
//			"label" : "<h3>44444444<\/h3>\tLeticia Ortiz"
//		},
//		{
//			"NAME" : "Alejandra",
//			"id" : "55555555",
//			"label" : "<h3>55555555<\/h3>\tAlejandra Gonzalez"
//		}
//];
//
//require(
//[
//		"dijit/form/TextBox",
//		"dojo/store/Cache",
//		"dojo/store/Memory",
//		"dijit/form/FilteringSelect",
//		"dojo/store/JsonRest"
//], function(ready,
//		Cache,
//		Memory,
//		FilteringSelect,
//		JsonRest,
//		zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz) {
//
//	var leStore = Cache(JsonRest({
//		target : "/path/*",
//		idProperty : "id"
//	}), Memory());
//
//	var testStore = new Memory({
//		data : []
//	});
//
//	dojo.ready(function() {
//
//		var filt_sel = new FilteringSelect({
//			id : "el_id",
//			name : "el_id",
//			hasDownArrow : false,
//			invalidMessage : "No element found",
//			searchAttr : "NAME",
//			queryExpr : "${0}",
//			store : testStore,
//			pageSize : 10,
//			labelAttr : "label",
//			labelType : "html",
//			onKeyUp : function(value) {
//				if (dojo.byId("el_id").value.length > 4 && dijit.byId("el_id").get("store") == testStore)
//					dijit.byId("el_id").set("store", leStore);
//				if (dojo.byId("el_id").value.length <= 4 && dijit.byId("el_id").get("store") == leStore)
//					dijit.byId("el_id").set("store", testStore);
//			},
//			onChange : function(value) {
//				dojo.byId('search').click();
//			}
//		}, "el_id");
//
//	});
//
//});