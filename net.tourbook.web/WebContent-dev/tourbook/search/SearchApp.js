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
	'dijit/Menu',
	'dijit/MenuItem',
	'dijit/popup',
	'dijit/registry',
	'dijit/Tooltip',
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
// dojo/_base
declare, //
fx, //
lang, //

// dojo
aspect, //
dom, //
domClass, //
domStyle, //
mouse, //
on, //
parser, //
xhr, //

// dijit
Button, //
Menu, //
MenuItem, //
popup, //
registry, //
Tooltip, //
TooltipDialog, //

// dgrid
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

			this.createUI_Actions();

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

				placeHolder : Messages.Search_App_Text_Search_PlaceHolder,

				hasDownArrow : false,

				searchAttr : 'id',
				labelAttr : 'name',
				labelType : 'html'

			}, 'domSearchInput');

			domClass.add(this._searchInput.domNode, 'domSearchInput');

			var grid = this.createUI_Grid();

			this._searchInput.setGrid(grid);

			new Tooltip(
			//
			{
				connectId : 'domInfo',
				position :
				[ 'below'
				],
				showDelay : 100,
				label : "<div style='width:30em;'>" //
					+ ("<b>" + Messages.Search_App_Tooltip_Title + "</b><br>") //
					+ Messages.Search_App_Tooltip_Label_1
					+ ("<a href='" + SearchMgr.Search_App_Tooltip_Url + "'>" + Messages.Search_App_Tooltip_Label_2 + "</a><br><br>") //
					+ Messages.Search_App_Tooltip_Label_3 //
					+ "</div>"
			});

			// set tooltips
			dom.byId('domAppStatus').title = Messages.Search_App_Label_Status_Tooltip;
		},

		createUI_Actions : function createUI_Actions() {

			var app = this;

			/**
			 * Action: Show search options.
			 */
			this._actionSearchOptions = new Button(//
			{
				// label is displayed as tooltip
				label : Messages.Search_App_Action_SearchOptions_Tooltip,

				showLabel : false,
				iconClass : 'actionIcon iconOptions',

				// show dialog when button is pressed with the keyboard
				onClick : function() {
					this._showDialog();
				},

				// show dialog when action button is hovered
				onMouseOver : function() {
					this._showDialog();
				},

				_showDialog : function _showDialog() {

					var dialogProperties = //
					{
						title : Messages.Search_Options_Dialog_Header,

						// set flag that the dialog is positioned below this button
						layoutParent : this
					};

					app._dlgSearchOptions.showDialog(dialogProperties);
				}

			}, 'domAction_SearchOptions');
			this._actionSearchOptions.startup();

			/**
			 * Action: Start search.
			 */
			this._actionStartSearch = new Button(//
			{
				// label is displayed as tooltip
				label : Messages.Search_App_Action_StartSearch_Tooltip,

				showLabel : false,
				iconClass : 'actionIcon iconSearch',

				// show dialog when button is pressed with the keyboard
				onClick : function() {
					app._searchInput.startSearch();
				}

			}, 'domAction_StartSearch');
			this._actionStartSearch.startup();
		},

		createUI_Grid : function createUI_Grid() {

			var app = this;

			// copied from http://dgrid.io/tutorials/0.4/grids_and_stores/demo/OnDemandGrid-comparison.html
			// ??? WHEN fetchRange IS NOT DEFINED, DATA WILL NOT BE RETRIEVED ???
			var collection = new (declare('tourbook.search.ResultStore', //
			RequestMemory,//
			{
				/**
				 * Overwrite fetchRange in dstore.Cache
				 */
				fetchRange : function fetchRange(args) {

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
						 * The data must be parsed a second time, have not found a solution to do it better after 2 days of
						 * try and error.
						 */
						requestData = JSON.parse(args.data), //

						searchText = app._searchInput.getSearchText(), //
						searchTime = requestData.searchTime, //
						searchTotal = requestData.totalHits, //

						statusText = '' + searchTotal + ' - ' + searchTime;

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

			var grid = new (declare('tourbook.search.Grid',
			[
				OnDemandList,
				Keyboard,
				Selection
			], {

				selectionMode : 'single',

				// default is empty that it is not confusing when loading the first time, 
				// this message will be set when a search is started manually
				noDataMessage : '',

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

			/**
			 * Context menu, is defined declaratively. 
			 */
			var contextMenuItemData;

			var actionEditMarker = registry.byId('domAction_EditMarker');
			var actionEditTour = registry.byId('domAction_EditTour');

			/*
			 * Setup context menu action
			 */
			grid.on('.dgrid-row:contextmenu', function(evt) {

				evt.preventDefault();
				var row = grid.row(evt);

				// keep context item
				contextMenuItemData = row && row.data ? row.data : null;

				/*
				 * enable/disable actions
				 */
				var isMarker = contextMenuItemData.isMarker;
				var isTour = contextMenuItemData.isTour;

				actionEditMarker.set('disabled', !isMarker);
				actionEditTour.set('disabled', !isTour);
			});
			
			/*
			 * Run context menu action
			 */
			var runUrlAction = function doUrlAction(event) {
				
				var actionUrl = contextMenuItemData.actionUrl_EditItem;
				SearchApp.action(actionUrl);
			};
			
			actionEditMarker.on('click', runUrlAction);
			actionEditTour.on('click', runUrlAction);

			return grid;
		},

		restoreState : function restoreState() {

			var app = this;

			var query = {};
			query[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_GET_STATE;

			xhr(SearchMgr.XHR_SEARCH_HANDLER, {

				query : query,
				timeout : SearchMgr.XHR_TIMEOUT,
				handleAs : 'json',
				preventCache : true

			}).then(function(state) {

				var searchInput = app._searchInput;

				searchInput.setSearchText(state.searchText);
				searchInput.selectAll();
			});
		},

// saveState() is disabled because an xhr request during page unload is not working !!!
// 		
//		saveState : function saveState() {
//
//			var searchText = this._searchInput.getSearchText();
//
//			var state = //
//			{
//				searchText : encodeURIComponent(searchText)
//			};
//
//			var query = {};
//			query[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_SET_STATE;
//			query[SearchMgr.XHR_PARAM_STATE] = state;
//
//			xhr(SearchMgr.XHR_SEARCH_HANDLER, {
//
//				query : query,
//				timeout : SearchMgr.XHR_TIMEOUT,
//				handleAs : 'json',
//				preventCache : true
//			});
//		},

		startApp : function startApp() {

			this.createUI();

			this.restoreState();

			// set focus to the search field
			this._searchInput.focus();

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

	/**
	 * Run an action in the MT app when the search UI is in the browser and not in the MT app.
	 * <p>
	 * The action is also startet from the Web UI.
	 */
	SearchApp.action = function action(actionUrl) {

		var query = {};
		query[SearchMgr.XHR_PARAM_ACTION] = SearchMgr.XHR_ACTION_ITEM_ACTION;
		query[SearchMgr.XHR_PARAM_ACTION_URL] = encodeURIComponent(actionUrl);

		xhr(SearchMgr.XHR_SEARCH_HANDLER, {

			handleAs : 'json',
			preventCache : true,
			timeout : SearchMgr.XHR_TIMEOUT,

			query : query
		});
	};

	parser.parse().then(function() {
		new SearchApp().startApp();
	});

	return SearchApp;
});
