'use strict';

require(
[
	'dojo/_base/declare',
	'dojo/_base/lang',
	'dojo/dom',

//	'dojo/store/JsonRest',
	'dojox/data/QueryReadStore',

	'gridx/Grid',
	'gridx/core/model/cache/Async',

	'gridx/modules/VirtualVScroller',
//	'gridx/modules/extendedSelect/Row',
//	'gridx/modules/Focus',
//	"gridx/modules/ColumnResizer",

	'./SearchInput.js',
	'dojo/domReady!'
], //
function(//
declare, //
lang, //
dom, //

//JsonRest, //
QueryReadStore, //

GridX, //
Cache, //

VirtualVScroller, //
//SelectRow, //
//Focus, //
//ColumnResizer, //

SearchInput //
) {

	var searchInput = new SearchInput({

		id : 'searchInput',
		name : 'idSearch',

		placeHolder : 'Search Tours, Marker and Waypoints',

		hasDownArrow : false,

		searchAttr : 'id',
		labelAttr : 'name',
		labelType : 'html',

	}, 'domSearchInput');

	searchInput.startup();

	var searchUrl = searchInput.getSearchUrl();

//	var store = new JsonRest({
//		target : searchUrl,
//	});

	var store = new QueryReadStore({
		idAttribute : 'id',
//		url : 'QueryReadStore.php'
		url : searchUrl
	});

	var columns =
	[
		{
			id : 'id',
			field : 'id',
			name : 'ID',
			width : '100px',
		},
		{
			id : 'name',
			field : 'name',
			name : 'Tour',
			width : '100%',
		}
	];

	var grid = new GridX({

		store : store,
		structure : columns,
		cacheClass : Cache,

		pageSize : 10,
		vScrollerLazy : true,

		modules : [
//			VirtualVScroller,
//			SelectRow,
//			Focus,
		]
	});

	grid.placeAt('domGrid');
	grid.startup();

	searchInput.setGrid(grid);

	// set focus to the search field
	searchInput.focus();
});
