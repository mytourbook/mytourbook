require(
[
	'dojo/_base/declare',
	"dojo/_base/fx",
	'dojo/_base/lang',

	'dojo/aspect',
	"dojo/dom",
	"dojo/dom-style",
	"dojo/parser",

	"dijit/registry",

	'dgrid/Keyboard',
	'dgrid/OnDemandList',
	"dgrid/Selection",
	'dstore/QueryResults',
	'dstore/RequestMemory',

	'put-selector/put',

	'./SearchInput.js',
	'./SearchMgr.js',

	'dojo/domReady!'

], function(
//
declare, //
fx, //
lang, //

aspect, //
dom, //
domStyle, //
parser, //

registry, //

Keyboard, //
OnDemandList, //
Selection, //
QueryResults, //
RequestMemory, //

put, //

SearchInput, //
SearchMgr //

) {

	function App() {
	}

	App.prototype = {

		createUI : function() {

			this.searchInput = new SearchInput({

				id : 'searchInput',
				name : 'idSearch',

				placeHolder : 'Search Tours, Marker and Waypoints',

				hasDownArrow : false,
				maxHeight : 300,

				searchAttr : 'id',
				labelAttr : 'name',
				labelType : 'html',

			}, 'domSearchInput');

			var grid = this.createUI_Grid();

			this.searchInput.setGrid(grid);

			// set focus to the search field
			this.searchInput.focus();
		},

		createUI_Grid : function() {

			// copied from http://dgrid.io/tutorials/0.4/grids_and_stores/demo/OnDemandGrid-comparison.html
			// ??? WHEN fetchRange IS NOT DEFINED, DATA WILL NOT BE RETRIEVED ???
			var store = new (declare("tourbook.search.MyStore", RequestMemory, {

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

				target : this.getSearchUrl(),
				useRangeHeaders : true,
			});

			var grid = new (declare("tourbook.search.MyOnDemandList",
			[
				OnDemandList,
				Keyboard,
				Selection
			], {

				selectionMode : "single",

				renderRow : function(value) {
					// summary:
					//		Responsible for returning the DOM for a single row in the grid.
					// value: Mixed
					//		Value to render
					// options: Object?
					//		Optional object with additional options

					var row = '<b>' + value.id + '</b><br>' + value.name;

					var div = put('div', '');
					div.innerHTML = row;

					return div;
				},
			}))({

				columns : {
					id : 'id',
					name : 'name'
				},

				collection : store

			}, 'domGrid');

//			grid.on("dgrid-select", function(event) {
//
//				// Get the rows that were just selected
//				var rows = event.rows;
//				// ...
//
//				// Iterate through all currently-selected items
//				for ( var id in grid.selection) {
//					if (grid.selection[id]) {
//						// ...
//					}
//				}
//			});
//
//			grid.on("dgrid-deselect", function(event) {
//				// Get the rows that were just deselected
//				var rows = event.rows;
//				// ...
//			});

			return grid;
		},

		getSearchUrl : function getSearchUrl() {

			var searchText = this.searchInput.getSearchText();

			var actionSearch = SearchMgr.XHR_PARAM_ACTION + "=" + SearchMgr.XHR_ACTION_SEARCH;
			var paramSearchText = "&" + SearchMgr.XHR_PARAM_SEARCH_TEXT + "=" + encodeURIComponent(searchText);

			var url = SearchMgr.XHR_SEARCH_HANDLER + '?' + actionSearch + paramSearchText;

			return url;
		},

		run : function() {

			this.createUI();

			// resize UI, otherwise not everthing is correctly rearranged
			var domContainer = registry.byId('domContainer');
			domContainer.resize();

			// fade out loading message
			fx.fadeOut({
				node : "domLoading",
				duration : 200,

				// hide loading layer
				onEnd : function(node) {
					domStyle.set(node, 'display', 'none');
				}
			}).play();
		}
	}

	parser.parse().then(function() {
		new App().run();
	});

});
