//'use strict';

define(
[ //
	"dojo/_base/declare"
],

function(declare) {
//
	var SearchMgr = declare("tourbook.search.SearchMgr", [], {});

	/*
	 * These XHR_... definitions are also defined in SearchUI.java
	 */
	SearchMgr.XHR_SEARCH_HANDLER = "/xhrSearch";

	SearchMgr.XHR_PARAM_ACTION = "action";

	SearchMgr.XHR_ACTION_SEARCH = "search";
	SearchMgr.XHR_ACTION_SELECT = "select";
	SearchMgr.XHR_ACTION_PROPOSALS = "proposals";

	SearchMgr.XHR_PARAM_SEARCH_TEXT = "searchText";
	SearchMgr.XHR_PARAM_SELECTED_ID = "selectedId";

	return SearchMgr;
});