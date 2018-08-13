// 'use strict';

/* eslint-env amd */
define(
		[ //
		'dojo/_base/declare' ],

		function(declare) {
			//
			var SearchMgr = declare('tourbook.search.SearchMgr', [], {});

			/*
			 * Long url's are truncated in the Messages editor.
			 */
			SearchMgr.Search_App_Tooltip_Url = 'http://lucene.apache.org/core/4_10_3/queryparser/org/apache/lucene/queryparser/classic/package-summary.html#Wildcard_Searches';

			/*
			 * These XHR_... definitions are also defined in SearchUI.java
			 */
			SearchMgr.XHR_SEARCH_HANDLER = '/xhrSearch';

			SearchMgr.XHR_ACTION_ITEM_ACTION = 'itemAction';
			SearchMgr.XHR_ACTION_PROPOSALS = 'proposals';
			SearchMgr.XHR_ACTION_SEARCH = 'search';
			SearchMgr.XHR_ACTION_SELECT = 'select';

			SearchMgr.XHR_ACTION_GET_SEARCH_OPTIONS = 'getSearchOptions';
			SearchMgr.XHR_ACTION_SET_SEARCH_OPTIONS = 'setSearchOptions';

			SearchMgr.XHR_ACTION_GET_STATE = 'getState';
			SearchMgr.XHR_ACTION_SET_STATE = 'setState';

			SearchMgr.XHR_PARAM_ACTION = 'action';
			SearchMgr.XHR_PARAM_ACTION_URL = 'actionUrl';
			SearchMgr.XHR_PARAM_SEARCH_OPTIONS = 'searchOptions';
			SearchMgr.XHR_PARAM_SEARCH_TEXT = 'searchText';
			SearchMgr.XHR_PARAM_SELECTED_ITEMS = 'selectedItems';

			/**
			 * Timeout in ms for xhr requests.
			 */
			SearchMgr.XHR_TIMEOUT = 10000;

			return SearchMgr;
		});