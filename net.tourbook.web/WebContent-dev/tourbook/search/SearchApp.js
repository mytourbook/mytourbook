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
	"dijit/popup",
	'dijit/registry',
	'dijit/TooltipDialog',

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
popup, //
registry, //
TooltipDialog, //

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

			this._createUI_Actions();

			/**
			 * Dialog: Search options
			 */
			this._dlgSearchOptions = new DialogSearchOptions(//
			{
				searchApp : this
			});

			/**
			 * Field: Search input
			 */
			this._searchInput = new SearchInput(//
			{
				id : 'searchInput',
				name : 'idSearch',

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

		_createUI_Actions : function _createUI_Actions() {

			var app = this;

			/**
			 * Action: Show search options.
			 */
			this._actionSearchOptions = new Button(//
			{
				// label is displayed as tooltip
				label : Messages.searchOptions_Tooltip,

				showLabel : false,
				iconClass : 'actionOptionsIcon',

				// show dialog when button is pressed with the keyboard
				onClick : function() {
					this.showDialog();
				},

				// show dialog when action button is hovered
				onMouseOver : function() {
					this.showDialog();
				},

				showDialog : function showDialog() {

					var dialogProperties = //
					{
						title : Messages.searchOptions_Title,

						// set flag that the dialog is positioned below this button
						layoutParent : this,
					};

					app._dlgSearchOptions.showDialog(dialogProperties);
				}

			}, 'domActionSearchOptions');
			this._actionSearchOptions.startup();
		},

		createUI_Grid : function createUI_Grid() {

			var app = this;

			// copied from http://dgrid.io/tutorials/0.4/grids_and_stores/demo/OnDemandGrid-comparison.html
			// ??? WHEN fetchRange IS NOT DEFINED, DATA WILL NOT BE RETRIEVED ???
			var collection = new (declare('tourbook.search.ResultStore', RequestMemory,//
			{
				/**
				 * Overwrite fetchRange in dstore.Cache
				 */
				fetchRange : function(args) {

					var //
					start = args.start, //
					end = args.end, //

					requestArgs = {};
					requestArgs.headers = this._renderRangeHeaders(start, end);

					var response = this._request(requestArgs);

					// keep data from the reponse which contain additional data, e.g. status, error
					app.responseData = response.response

					/*
					 * Create query result
					 */
					var queryResult = new QueryResults(//

					// data
					response.data, //

					// options
					{
						totalLength : response.total,
						response : response.response
					});

					/*
					 * Update status
					 */
					response.response.then(function(args) {

						var //

						/*
						 * The data must be parsed a second time but didn't find a solution to do it better after 2 days of
						 * try and error.
						 */
						requestData = JSON.parse(args.data), //

						searchText = app._searchInput.getSearchText(), //
						searchTime = requestData.searchTime, //
						searchTotal = requestData.totalHits, //

						statusText = "" + searchTotal + " - " + searchTime;

						// update status
						dom.byId('domAppStatus').innerHTML = statusText;
					});

					return queryResult;
				}
			}))(//
			{

				// a valid url is necessary
				// target will be set for each search request
				//			target : 'about:blank',
				target : app._searchInput.createSearchUrl(),

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

			// fire an event when tour, marker or waypoint is selected to select it in the UI
			grid.on('dgrid-select', function(event) {

				var row = event.rows[0];
				var selectedId = row.data[SearchMgr.XHR_PARAM_SELECTED_ID];

				var xhrQuery = {};
				xhrQuery[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SELECT;
				xhrQuery[SearchMgr.XHR_PARAM_SELECTED_ID] = encodeURIComponent(selectedId);

				xhr(SearchMgr.XHR_SEARCH_HANDLER, {

					handleAs : 'json',
					preventCache : true,
					timeout : SearchMgr.XHR_TIMEOUT,

					query : xhrQuery
				});
			});

			return grid;
		},

		_createUI_Status : function() {

			this.apStatus.innerHTML = "test";
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
