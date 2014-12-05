// require(
// [
// "dojo/dom",
// "dojo/store/Memory",
// "dijit/form/FilteringSelect",
// "../tourbook/SearchUI.js",
// "dojo/domReady!"
// ], function(dom,
// Memory,
// SearchUI,
// FilteringSelect,
// zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz) {
//
// var fs = new FilteringSelect({
// id : "domSearchInput",
// name : "xyz",
// value : 3,
// store : dojoStore,
// searchAttr : "name",
// labelAttr : "label",
// labelType : "html"
// }, dom.byId("domSearchInput")).startup();
// });

"use strict";

require(
[
		"dojo/_base/declare",
		"../tourbook/SearchUI.js",
		"dojo/domReady!"
], //
function(declare,
		SearchUI,
		zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz) {

	var searchUI = new SearchUI({

		id : "searchInput",
		name : "idSearch",

		placeHolder : "Search Tours, Marker and Waypoints",

		hasDownArrow : false,
//		highlightMatch:"none",

		searchAttr : "id",
		labelAttr : "name",
		labelType : "html",

//		lowercase : false,
//		uppercase:false,
//		propercase:true,

//		queryExpr : "*${0}*",
//		${0}* means "starts with", 
//		*${0}* means "contains", 
//		${0} means "is"

		store : null,

	}, "domSearchInput");

	searchUI.startup();
});
