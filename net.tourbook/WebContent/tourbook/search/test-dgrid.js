var filter;

require(
[
	'dojo/_base/declare',
	'dojo/_base/lang',

	'dgrid/OnDemandList',
	'dstore/QueryResults',
	'dstore/RequestMemory',

	'put-selector/put',

	'./SearchMgr.js',

], function(//
declare, //
lang, //

OnDemandList, //
QueryResults, //
RequestMemory, //

put,

SearchMgr

) {

	var getSearchUrl = function() {

//		var searchText = this.getSearchText();
		var searchText = "a";

		var actionSearch = SearchMgr.XHR_PARAM_ACTION + "=" + SearchMgr.XHR_ACTION_SEARCH;
		var paramSearchText = "&" + SearchMgr.XHR_PARAM_SEARCH_TEXT + "=" + encodeURIComponent(searchText);

		var url = SearchMgr.XHR_SEARCH_HANDLER + '?' + actionSearch + paramSearchText;

//		console.info("store: " + url);

		return url;
	}

//	var store = new RequestMemory({
//		target : getSearchUrl(),
//		useRangeHeaders : true,
//	});

	// copied from http://dgrid.io/tutorials/0.4/grids_and_stores/demo/OnDemandGrid-comparison.html
	// ??? WHEN fetchRange IS NOT DEFINED, DATA WILL NOT BE RETRIEVED ???
	var store = new (declare("MyStore", RequestMemory, {

		fetchRange : function(kwArgs) {

			var start = kwArgs.start, //
			end = kwArgs.end, //

			requestArgs = {};
			requestArgs.headers = this._renderRangeHeaders(start, end);

			var results = this._request(requestArgs);

			return new QueryResults(//
			results.data, //
			{
				totalLength : results.total,
				response : results.response
			});
		},
	}))({

		target : getSearchUrl(),
		useRangeHeaders : true,
	});

	var grid = new (declare("MyOnDemandList", OnDemandList, {

		renderRow : function(value) {
			// summary:
			//		Responsible for returning the DOM for a single row in the grid.
			// value: Mixed
			//		Value to render
			// options: Object?
			//		Optional object with additional options

			var row = '<b>' + value.id + '</b><br>' + value.name;

			var div=put('div', '');
			div.innerHTML=row;
			
			return div;
		},
	}))({

		columns : {
			id : 'id',
			name : 'name'
		},

		collection : store

	}, 'domGrid');

//	var grid = new OnDemandList({
//		columns : {
//			id : 'id',
//			name : 'name'
//		},
//		collection : store
//	}, 'domGrid');

});
