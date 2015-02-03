define(
[
	'dojo/_base/declare',
	'dojo/_base/fx',
	'dojo/_base/lang',
	'dojo/aspect',
	'dojo/dom',
	'dojo/dom-class',
	'dojo/dom-style',
	'dojo/mouse',
	'dojo/on',
	'dojo/parser',
	'dojo/request/xhr',

	'dijit/form/Button',
	'dijit/registry',

	'dgrid/Keyboard',
	'dgrid/OnDemandList',
	'dgrid/Selection',
	'dstore/QueryResults',
	'dstore/RequestMemory',

	'put-selector/put',

	'./DialogSearchOptions',
	'./SearchInput',
	'./SearchMgr',
	'dojo/i18n!./nls/Messages',

	'dojo/domReady!'
], function(
//
declare, //
fx, //
lang, //

aspect, //
dom, //
domClass, //
domStyle, //
mouse, //
on, //
parser, //
xhr, //

Button, //
registry, //

Keyboard, //
OnDemandList, //
Selection, //
QueryResults, //
RequestMemory, //

put, //

DialogSearchOptions, //
SearchInput, //
SearchMgr, //
Messages //
) {

	var SearchApp = declare('tourbook.search.SearchApp', [], {

		createUI : function createUI() {

			var self = this;

			// set 'search' class to overwrite css theme properties
//			var searchInputContainer=registry.byId('domSearchInputContainer');
//			domClass.add(searchInputContainer, 'search');

			/**
			 * Action: Search Options
			 */
			this._actionOptions = new Button({

				// label is displayed as tooltip
				label : Messages.searchOptions_Tooltip,
				showLabel : false,
				iconClass : 'actionOptionsIcon',

				onClick : function() {

					var dlgSearchOptions = self._dlgSearchOptions;

					dlgSearchOptions.showDialog({
						title : Messages.searchOptions_Title
					});
				}

			}, 'domActionSearchOptions');
			this._actionOptions.startup();

			/*
			 * Dialog: Search options
			 */
			this._dlgSearchOptions = new DialogSearchOptions({
//						fitContentArea : false,
				resizable : true
			});

			/**
			 * Field: Search input
			 */
			this._searchInput = new SearchInput({

				id : 'searchInput',
				name : 'idSearch',

				// this will overwrite all dijit classes for this dom node
//				baseClass : 'domSearchInput',

				placeHolder : Messages.searchInput_PlaceHolder,

				hasDownArrow : false,

				searchAttr : 'id',
				labelAttr : 'name',
				labelType : 'html'

			}, 'domSearchInput');

			domClass.add(this._searchInput.domNode, 'domSearchInput');

			var grid = this.createUI_Grid();

			this._searchInput.setGrid(grid);

			// set focus to the search field
			this._searchInput.focus();
		},

		createUI_Grid : function createUI_Grid() {

			var self = this;

			// copied from http://dgrid.io/tutorials/0.4/grids_and_stores/demo/OnDemandGrid-comparison.html
			// ??? WHEN fetchRange IS NOT DEFINED, DATA WILL NOT BE RETRIEVED ???
			var collection = new (declare('tourbook.search.ResultStore', RequestMemory, {

				fetchRange : function(kwArgs) {

					var start = kwArgs.start, //
					end = kwArgs.end, //

					requestArgs = {};
					requestArgs.headers = this._renderRangeHeaders(start, end);

					var results = this._request(requestArgs);

					// keep data from the reponse which contain additional data, e.g. status, error
					self.responseData = results.response

					var queryResult = new QueryResults(//
					results.data, //
					{
						totalLength : results.total,
						response : results.response
					});

					return queryResult;
				}
			}))({

				// a valid url is necessary
				// target will be set for each search request
				//			target : 'about:blank',
				target : self._searchInput.getSearchUrl(),

				useRangeHeaders : true
			});

			var grid = new (declare('tourbook.search.ResultUIList',
			[
				OnDemandList,
				Keyboard,
				Selection
			], {

				selectionMode : 'single',

				renderRow : function(value) {

					var div = put('div', '');
					div.innerHTML = value.htmlContent;

					return div;
				}
			}))({

				columns : {
					id : 'id',
					name : 'name'
				},

				collection : collection

			}, 'domGrid');

			grid.on('dgrid-select', function(event) {

				// tour, marker or waypoint is selected -> fire an event, select it in the UI

				var row = event.rows[0];
				var selectedId = row.data[SearchMgr.XHR_PARAM_SELECTED_ID];

				var xhrQuery = {};
				xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SELECT;
				xhrQuery[SearchMgr.XHR_PARAM_SELECTED_ID] = encodeURIComponent(selectedId);

				xhr(SearchMgr.XHR_SEARCH_HANDLER, {

					handleAs : 'json',
					preventCache : true,
					timeout : 60000,

					query : xhrQuery
				});
			});

			return grid;
		},

		startApp : function startApp() {

			this.createUI();

			// resize UI, otherwise not everthing is correctly rearranged
			registry.byId('domContainer').resize();

			// fade out loading message
			fx.fadeOut({
				node : 'domLoading',
				duration : 200,

				// hide loading layer
				onEnd : function(node) {
					domStyle.set(node, 'display', 'none');
				}
			}).play();
		}
	});

	parser.parse().then(function() {
		new SearchApp().startApp();
	});

	return SearchApp;
});
