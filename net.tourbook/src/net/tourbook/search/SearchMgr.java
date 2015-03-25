/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.search;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.application.IconRequestMgr;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.DialogQuickEdit;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.web.RequestParameterFilter;
import net.tourbook.web.WEB;
import net.tourbook.web.WebContentServer;
import net.tourbook.web.XHRHandler;

import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

/**
 *
 */
public class SearchMgr implements XHRHandler {

	private static final String				SEARCH_APP_ACTION_EDIT_MARKER			= tourbook.search.nls.Messages.Search_App_Action_EditMarker;
	private static final String				SEARCH_APP_ACTION_EDIT_TOUR				= tourbook.search.nls.Messages.Search_App_Action_EditTour;

	private static final String				IMAGE_ACTION_TOUR_WAY_POINT				= net.tourbook.map2.Messages.Image_Action_TourWayPoint;

	final static IDialogSettings			state									= TourbookPlugin
																							.getState("net.tourbook.search.SearchMgr");			//$NON-NLS-1$

	private static final String				JSON_STATE_SEARCH_TEXT					= "searchText";												//$NON-NLS-1$

	private static final String				STATE_CURRENT_SEARCH_TEXT				= "STATE_CURRENT_SEARCH_TEXT";									//$NON-NLS-1$

	static final String						STATE_IS_EASE_SEARCHING					= "STATE_IS_EASE_SEARCHING";									//$NON-NLS-1$
	static final boolean					STATE_IS_EASE_SEARCHING_DEFAULT			= true;

	static final String						STATE_IS_SHOW_CONTENT_ALL				= "STATE_IS_SHOW_CONTENT_ALL";									//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_ALL_DEFAULT		= true;
	static final String						STATE_IS_SHOW_CONTENT_MARKER			= "STATE_IS_SHOW_CONTENT_MARKER";								//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_MARKER_DEFAULT	= true;
	static final String						STATE_IS_SHOW_CONTENT_TOUR				= "STATE_IS_SHOW_CONTENT_TOUR";								//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_TOUR_DEFAULT		= true;
	static final String						STATE_IS_SHOW_CONTENT_WAYPOINT			= "STATE_IS_SHOW_CONTENT_WAYPOINT";							//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_WAYPOINT_DEFAULT	= true;
	//
	static final String						STATE_IS_SHOW_DATE_TIME					= "STATE_IS_SHOW_DATE_TIME";									//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_DATE_TIME_DEFAULT			= false;
	static final String						STATE_IS_SHOW_ITEM_NUMBER				= "STATE_IS_SHOW_ITEM_NUMBER";									//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_ITEM_NUMBER_DEFAULT		= false;
	static final String						STATE_IS_SHOW_LUCENE_DOC_ID				= "STATE_IS_SHOW_LUCENE_DOC_ID";								//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT		= false;
	//
	static final String						STATE_IS_SORT_DATE_ASCENDING			= "STATE_IS_SORT_DATE_ASCENDING";								//$NON-NLS-1$
	static final boolean					STATE_IS_SORT_DATE_ASCENDING_DEFAULT	= false;

	private static final String				REQUEST_HEADER_RANGE					= "Range";														//$NON-NLS-1$

	private static final String				CONTENT_RANGE_ITEMS						= "items %d-%d/%d";											//$NON-NLS-1$
	private static final String				CONTENT_RANGE_ZERO						= "0-0/0";														//$NON-NLS-1$

	/*
	 * This will handle all xhr actions, they are also defined in SearchMgr.js
	 */
	private static final String				XHR_SEARCH_INPUT_HANDLER				= "/xhrSearch";												//$NON-NLS-1$
	//
	private static final String				XHR_ACTION_ITEM_ACTION					= "itemAction";												//$NON-NLS-1$
	private static final String				XHR_ACTION_PROPOSALS					= "proposals";													//$NON-NLS-1$
	private static final String				XHR_ACTION_SEARCH						= "search";													//$NON-NLS-1$
	private static final String				XHR_ACTION_SELECT						= "select";													//$NON-NLS-1$
	private static final String				XHR_ACTION_GET_SEARCH_OPTIONS			= "getSearchOptions";											//$NON-NLS-1$
	private static final String				XHR_ACTION_SET_SEARCH_OPTIONS			= "setSearchOptions";											//$NON-NLS-1$
	private static final String				XHR_ACTION_GET_STATE					= "getState";													//$NON-NLS-1$
//	private static final String				XHR_ACTION_SET_STATE					= "setState";											//$NON-NLS-1$
																																					//
	private static final String				XHR_PARAM_ACTION						= "action";													//$NON-NLS-1$
	private static final String				XHR_PARAM_ACTION_URL					= "actionUrl";													//$NON-NLS-1$
	private static final String				XHR_PARAM_SEARCH_OPTIONS				= "searchOptions";												//$NON-NLS-1$
	private static final String				XHR_PARAM_SEARCH_TEXT					= JSON_STATE_SEARCH_TEXT;
	private static final String				XHR_PARAM_SELECTED_ID					= "selectedId";												//$NON-NLS-1$
//	private static final String				XHR_PARAM_STATE							= "state";												//$NON-NLS-1$
																																					//
	/*
	 * JSON response values
	 */
	private static final String				JSON_ID									= "id";														//$NON-NLS-1$
	private static final String				JSON_NAME								= "name";														//$NON-NLS-1$
	private static final String				JSON_HTML_CONTENT						= "htmlContent";												//$NON-NLS-1$
	private static final String				JSON_ITEM_ACTION_URL_EDIT_ITEM			= "actionUrl_EditItem";										//$NON-NLS-1$
	private static final String				JSON_ITEM_IS_MARKER						= "isMarker";													//$NON-NLS-1$
	private static final String				JSON_ITEM_IS_TOUR						= "isTour";													//$NON-NLS-1$
	private static final String				JSON_ITEM_IS_WAYPOINT					= "isWaypoint";												//$NON-NLS-1$
	private static final String				JSON_SELECTED_ID						= "selectedId";												//$NON-NLS-1$
	//
	// search options
	private static final String				JSON_IS_SEARCH_OPTIONS_DEFAULT			= "isSearchOptionsDefault";									//$NON-NLS-1$
	//
	private static final String				JSON_IS_EASE_SEARCHING					= "isEaseSearching";											//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_CONTENT_ALL				= "isShowContentAll";											//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_CONTENT_MARKER				= "isShowContentMarker";										//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_CONTENT_TOUR				= "isShowContentTour";											//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_CONTENT_WAYPOINT			= "isShowContentWaypoint";										//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_LUCENE_ID					= "isShowLuceneID";											//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_ITEM_NUMBER				= "isShowItemNumber";											//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_DATE_TIME					= "isShowDateTime";											//$NON-NLS-1$
	private static final String				JSON_IS_SORT_BY_DATE_ASCENDING			= "isSortByDateAscending";										//$NON-NLS-1$
	//
	private static final String				SEARCH_FOLDER							= "/tourbook/search/";											//$NON-NLS-1$
	private static final String				SEARCH_PAGE								= "search.mthtml";												//$NON-NLS-1$
	//
	/**
	 * This is necessary otherwise XULrunner in Linux do not fire a location change event.
	 */
	static String							ACTION_URL;
	static String							SEARCH_URL;
	//
	private static String					_actionUrl_EditImage;
	private static String					_iconUrl_Tour;
	private static String					_iconUrl_Marker;
	private static String					_iconUrl_WayPoint;
	//
	private static boolean					_isUI_EaseSearching;
	private static boolean					_isUI_ShowContentAll;
	private static boolean					_isUI_ShowContentMarker;
	private static boolean					_isUI_ShowContentTour;
	private static boolean					_isUI_ShowContentWaypoint;
	private static boolean					_isUI_ShowDateTime;
	private static boolean					_isUI_ShowItemNumber;
	private static boolean					_isUI_ShowLuceneDocId;
	private static boolean					_isUI_SortDateAscending;

	private static final DateTimeFormatter	_dateFormatter							= DateTimeFormat.mediumDate();
	//
	static final String						TAG_TD									= "<td>";														//$NON-NLS-1$
	static final String						TAG_TD_END								= "</td>";														//$NON-NLS-1$
	static final String						CSS_ITEM_CONTAINER						= "item-container";											//$NON-NLS-1$

	static final String						PAGE_ABOUT_BLANK						= "about:blank";												//$NON-NLS-1$

	static final String						HREF_TOKEN								= "&";															//$NON-NLS-1$
	static final String						HREF_VALUE_SEP							= "=";															//$NON-NLS-1$

	static final String						PARAM_ACTION							= "action";													//$NON-NLS-1$
	static final String						PARAM_DOC_ID							= "docId";														//$NON-NLS-1$
	static final String						PARAM_MARKER_ID							= "markerId";													//$NON-NLS-1$
	static final String						PARAM_TOUR_ID							= "tourId";													//$NON-NLS-1$

	static final String						ACTION_EDIT_MARKER						= "EditMarker";												//$NON-NLS-1$
	static final String						ACTION_EDIT_TOUR						= "EditTour";													//$NON-NLS-1$
	static final String						ACTION_SELECT_TOUR						= "SelectTour";												//$NON-NLS-1$
	static final String						ACTION_SELECT_MARKER					= "SelectMarker";												//$NON-NLS-1$
	static final String						ACTION_SELECT_WAY_POINT					= "SelectWayPoint";											//$NON-NLS-1$

	static final String						HREF_ACTION_EDIT_MARKER;
	static final String						HREF_ACTION_EDIT_TOUR;
	static final String						HREF_ACTION_SELECT_TOUR;
	static final String						HREF_ACTION_SELECT_MARKER;
	static final String						HREF_ACTION_SELECT_WAY_POINT;

	static final String						HREF_PARAM_DOC_ID;
	static final String						HREF_PARAM_MARKER_ID;
	static final String						HREF_PARAM_TOUR_ID;

	static {

		ACTION_URL = WebContentServer.SERVER_URL + "/action?"; //$NON-NLS-1$
		SEARCH_URL = WebContentServer.SERVER_URL + SEARCH_FOLDER + SEARCH_PAGE;

		// e.g. ...&action=EditMarker...

		final String HREF_ACTION = HREF_TOKEN + PARAM_ACTION + HREF_VALUE_SEP;

		HREF_ACTION_EDIT_MARKER = HREF_ACTION + ACTION_EDIT_MARKER;
		HREF_ACTION_EDIT_TOUR = HREF_ACTION + ACTION_EDIT_TOUR;
		HREF_ACTION_SELECT_TOUR = HREF_ACTION + ACTION_SELECT_TOUR;
		HREF_ACTION_SELECT_MARKER = HREF_ACTION + ACTION_SELECT_MARKER;
		HREF_ACTION_SELECT_WAY_POINT = HREF_ACTION + ACTION_SELECT_WAY_POINT;

		HREF_PARAM_DOC_ID = HREF_TOKEN + PARAM_DOC_ID + HREF_VALUE_SEP;
		HREF_PARAM_MARKER_ID = HREF_TOKEN + PARAM_MARKER_ID + HREF_VALUE_SEP;
		HREF_PARAM_TOUR_ID = HREF_TOKEN + PARAM_TOUR_ID + HREF_VALUE_SEP;

		/*
		 * set image urls
		 */
		_iconUrl_Tour = getIconUrl(Messages.Image__TourChart);
		_iconUrl_Marker = getIconUrl(Messages.Image__TourMarker);
		_iconUrl_WayPoint = getIconUrl(IMAGE_ACTION_TOUR_WAY_POINT);

		_actionUrl_EditImage = getIconUrl(Messages.Image__quick_edit);

		// initialize search options
		setInternalSearchOptions();
	}

	private static SearchMgr				_searchMgr;

	private static ISearchView				_searchView;

	private static boolean					_isModalDialogOpen						= false;

	class ItemResponse {

		String	actionUrl_EditItem;

		String	createdHtml;
		String	selectedId;

		boolean	isMarker;
		boolean	isTour;
		boolean	isWayPoint;
	}

	/**
	 * !!! With an active shell the dialog positions are maintained !!!
	 * 
	 * @return
	 */
	private static Shell getActiveShell() {

		final Display display = Display.getDefault();
		final Shell[] allShells = display.getShells();

		final Shell activeShell = allShells[allShells.length - 1];

		activeShell.forceActive();
		activeShell.forceFocus();

		return activeShell;
	}

	private static String getIconUrl(final String iconImage) {

		final String iconUrl = net.tourbook.ui.UI.getIconUrl(iconImage);

		return WebContentServer.SERVER_URL + '/' + iconUrl;
	}

	private static SearchMgr getInstance() {

		if (_searchMgr == null) {
			_searchMgr = new SearchMgr();
		}

		return _searchMgr;
	}

	/**
	 * @param location
	 * @return Returns <code>true</code> when a action is performed.
	 */
	private static boolean hrefAction(final String location) {

		if (_searchView == null) {
			return false;
		}

		String action = null;

		long tourId = -1;
		long markerId = -1;

		final String[] urlParameter = location.split(HREF_TOKEN);

		// loop all url parameter
		for (final String part : urlParameter) {

			final int valueSepPos = part.indexOf(HREF_VALUE_SEP);

			String key;
			String value = null;

			if (valueSepPos == -1) {
				key = part;
			} else {
				key = part.substring(0, valueSepPos);
				value = part.substring(valueSepPos + 1, part.length());
			}

			if (key == null) {
				// this should not happen
				return false;
			}

			switch (key) {
			case PARAM_ACTION:
				action = value;
				break;

			case PARAM_MARKER_ID:
				markerId = Long.parseLong(value);
				break;

			case PARAM_TOUR_ID:
				tourId = Long.parseLong(value);
				break;

			default:
				break;
			}
		}

		if (action == null) {

			final boolean isAppUrl = location.startsWith(WebContentServer.SERVER_URL);

			// check if an external links is called
			if (location.startsWith(WEB.PROTOCOL_HTTP) && !isAppUrl) {

				// identified external link

				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						WEB.openUrl(location);
					}
				});

				return true;
			}

			return false;
		}

		switch (action) {

		case ACTION_EDIT_TOUR:

			hrefAction_Tour_Edit(tourId);

			break;

		case ACTION_SELECT_TOUR:

			hrefAction_Tour_Select(tourId);

			break;

		case ACTION_EDIT_MARKER:
		case ACTION_SELECT_MARKER:

			hrefAction_Marker(action, tourId, markerId);

			break;

		case ACTION_SELECT_WAY_POINT:

			hrefAction_WayPoint(action, tourId, markerId);

			break;
		}

		return true;
	}

	private static void hrefAction_Marker(final String action, final long tourId, final long markerId) {

		// get tour by id
		final TourData tourData = TourManager.getTour(tourId);
		if (tourData == null) {
			return;
		}

		TourMarker selectedTourMarker = null;

		// get marker by id
		for (final TourMarker tourMarker : tourData.getTourMarkers()) {
			if (tourMarker.getMarkerId() == markerId) {
				selectedTourMarker = tourMarker;
				break;
			}
		}

		if (selectedTourMarker == null) {
			return;
		}

		switch (action) {
		case SearchMgr.ACTION_EDIT_MARKER:
			hrefAction_Marker_Edit(tourData, selectedTourMarker);
			break;

		case SearchMgr.ACTION_SELECT_MARKER:
			hrefAction_Marker_Select(tourData, selectedTourMarker);
			break;
		}
	}

	private static void hrefAction_Marker_Edit(final TourData tourData, final TourMarker tourMarker) {

		final Shell activeShell = getActiveShell();

		// ensure this dialog is modal (only one dialog can be opened)
		if (_isModalDialogOpen) {

			MessageDialog.openInformation(
					activeShell,
					Messages.App_Action_Dialog_ActionIsInProgress_Title,
					Messages.App_Action_Dialog_ActionIsInProgress_Message);

			return;
		}

		if (tourData.isManualTour()) {
			// a manually created tour do not have time slices -> no markers
			return;
		}

		_isModalDialogOpen = true;

		try {

			final DialogMarker dialogMarker = new DialogMarker(//
					activeShell,
					tourData,
					tourMarker);

			if (dialogMarker.open() == Window.OK) {
				saveModifiedTour(tourData);
			}

		} finally {

			_isModalDialogOpen = false;
		}
	}

	private static void hrefAction_Marker_Select(final TourData tourData, final TourMarker selectedTourMarker) {

		if (_searchView == null) {
			return;
		}

		final ArrayList<TourMarker> selectedTourMarkers = new ArrayList<TourMarker>();
		selectedTourMarkers.add(selectedTourMarker);

		final SelectionTourMarker markerSelection = new SelectionTourMarker(tourData, selectedTourMarkers);

		updateSelectionProvider(markerSelection);

		TourManager.fireEvent(//
				TourEventId.MARKER_SELECTION,
				markerSelection,
				_searchView.getPart());
	}

	private static void hrefAction_Tour_Edit(final Long tourId) {

		final Shell activeShell = getActiveShell();

		// ensure this dialog is modal (only one dialog can be opened)
		if (_isModalDialogOpen) {

			MessageDialog.openInformation(
					activeShell,
					Messages.App_Action_Dialog_ActionIsInProgress_Title,
					Messages.App_Action_Dialog_ActionIsInProgress_Message);

			return;
		}

		// get tour by id
		final TourData tourData = TourManager.getTour(tourId);
		if (tourData == null) {
			return;
		}

		_isModalDialogOpen = true;

		try {

			final DialogQuickEdit dialogQuickEdit = new DialogQuickEdit(//
					activeShell,
					tourData);

			if (dialogQuickEdit.open() == Window.OK) {

				saveModifiedTour(tourData);
			}

		} finally {
			_isModalDialogOpen = false;
		}
	}

	private static void hrefAction_Tour_Select(final long tourId) {

		final SelectionTourId selection = new SelectionTourId(tourId);

		updateSelectionProvider(selection);

		TourManager.fireEvent(TourEventId.TOUR_SELECTION, selection, _searchView.getPart());
	}

	private static void hrefAction_WayPoint(final String action, final long tourId, final long markerId) {

		if (_searchView == null) {
			return;
		}

		// get tour by id
		final TourData tourData = TourManager.getTour(tourId);
		if (tourData == null) {
			return;
		}

		TourWayPoint selectedWayPoint = null;

		// get marker by id
		for (final TourWayPoint wayPoint : tourData.getTourWayPoints()) {
			if (wayPoint.getWayPointId() == markerId) {
				selectedWayPoint = wayPoint;
				break;
			}
		}

		if (selectedWayPoint == null) {
			return;
		}

		// fire selection
		final ISelection selection = new StructuredSelection(selectedWayPoint);
		_searchView.getPostSelectionProvider().setSelection(selection);
	}

	static void onBrowserLocation(final LocationEvent event) {

		final String location = event.location;

		if (hrefAction(location)) {

			// keep current page when an action is performed, OTHERWISE the current page will disappear or is replaced :-(
			event.doit = false;
		}
	}

	/**
	 * Set defaults for the search options in the state.
	 */
	static void saveDefaultSearchOption() {

		state.put(STATE_IS_EASE_SEARCHING, STATE_IS_EASE_SEARCHING_DEFAULT);

		state.put(STATE_IS_SHOW_CONTENT_ALL, STATE_IS_SHOW_CONTENT_ALL_DEFAULT);
		state.put(STATE_IS_SHOW_CONTENT_MARKER, STATE_IS_SHOW_CONTENT_MARKER_DEFAULT);
		state.put(STATE_IS_SHOW_CONTENT_TOUR, STATE_IS_SHOW_CONTENT_TOUR_DEFAULT);
		state.put(STATE_IS_SHOW_CONTENT_WAYPOINT, STATE_IS_SHOW_CONTENT_WAYPOINT_DEFAULT);

		state.put(STATE_IS_SHOW_DATE_TIME, STATE_IS_SHOW_DATE_TIME_DEFAULT);
		state.put(STATE_IS_SHOW_ITEM_NUMBER, STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);
		state.put(STATE_IS_SHOW_LUCENE_DOC_ID, STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);

		state.put(STATE_IS_SORT_DATE_ASCENDING, STATE_IS_SORT_DATE_ASCENDING_DEFAULT);
	}

	private static void saveModifiedTour(final TourData tourData) {

		/*
		 * Run async because a tour save will fire a tour change event.
		 */
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				TourManager.saveModifiedTour(tourData);
			}
		});
	}

	/**
	 * Set internal search options from the state. These internal options are used when creating the
	 * search result.
	 */
	static void setInternalSearchOptions() {

		_isUI_EaseSearching = Util.getStateBoolean(state, //
				STATE_IS_EASE_SEARCHING,
				STATE_IS_EASE_SEARCHING_DEFAULT);

		_isUI_ShowContentAll = Util.getStateBoolean(state, //
				STATE_IS_SHOW_CONTENT_ALL,
				STATE_IS_SHOW_CONTENT_ALL_DEFAULT);

		_isUI_ShowContentMarker = Util.getStateBoolean(state, //
				STATE_IS_SHOW_CONTENT_MARKER,
				STATE_IS_SHOW_CONTENT_MARKER_DEFAULT);

		_isUI_ShowContentTour = Util.getStateBoolean(state, //
				STATE_IS_SHOW_CONTENT_TOUR,
				STATE_IS_SHOW_CONTENT_TOUR_DEFAULT);

		_isUI_ShowContentWaypoint = Util.getStateBoolean(state, //
				STATE_IS_SHOW_CONTENT_WAYPOINT,
				STATE_IS_SHOW_CONTENT_WAYPOINT_DEFAULT);

		_isUI_ShowDateTime = Util.getStateBoolean(state, //
				STATE_IS_SHOW_DATE_TIME,
				STATE_IS_SHOW_DATE_TIME_DEFAULT);

		_isUI_ShowLuceneDocId = Util.getStateBoolean(state, //
				STATE_IS_SHOW_LUCENE_DOC_ID,
				STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);

		_isUI_ShowItemNumber = Util.getStateBoolean(state, //
				STATE_IS_SHOW_ITEM_NUMBER,
				STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);

		_isUI_SortDateAscending = Util.getStateBoolean(state,//
				STATE_IS_SORT_DATE_ASCENDING,
				STATE_IS_SORT_DATE_ASCENDING_DEFAULT);

		// set sorting in the search manager
		FTSearchManager.setSearchOptions(
				_isUI_ShowContentAll,
				_isUI_ShowContentMarker,
				_isUI_ShowContentTour,
				_isUI_ShowContentWaypoint,
				_isUI_SortDateAscending);
	}

	/**
	 * Web content server is available only when the search view is opened.
	 * 
	 * @param searchView
	 */
	public static void setSearchView(final ISearchView searchView) {

		if (searchView == null) {

			// shutdown search service

			FTSearchManager.close();

			WebContentServer.stop();

			WebContentServer.removeXHRHandler(XHR_SEARCH_INPUT_HANDLER);
			WebContentServer.setIconRequestHandler(null);

		} else {

			// start search service

			WebContentServer.start();

			WebContentServer.addXHRHandler(XHR_SEARCH_INPUT_HANDLER, SearchMgr.getInstance());
			WebContentServer.setIconRequestHandler(IconRequestMgr.getInstance());
		}

		_searchView = searchView;
	}

	/**
	 * Ensure that the selection provider contains the same data.
	 * 
	 * @param selection
	 */
	private static void updateSelectionProvider(final ISelection selection) {

		_searchView.getPostSelectionProvider().setSelectionNoFireEvent(selection);
	}

	private ItemResponse createHTML_10_Item(final SearchResultItem resultItem, final int itemNumber) {

		final StringBuilder sb = new StringBuilder();

		final int docId = resultItem.docId;
		final int docSource = resultItem.docSource;
		final String markerId = resultItem.markerId;
		final String tourId = resultItem.tourId;

		final boolean isTour = docSource == FTSearchManager.DOC_SOURCE_TOUR;
		final boolean isMarker = docSource == FTSearchManager.DOC_SOURCE_TOUR_MARKER;
		final boolean isWayPoint = docSource == FTSearchManager.DOC_SOURCE_WAY_POINT;

		String iconUrl = null;

		String hoverMessage = null;
		String hrefEditItem = null;
		String hrefSelectItem = null;
		String itemTitleText = null;

		if (isTour) {

			final String tourTitle = resultItem.title;
			if (tourTitle != null) {
				itemTitleText = tourTitle;
			}

			hrefSelectItem = ACTION_URL
					+ HREF_ACTION_SELECT_TOUR
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_DOC_ID + docId);

			hrefEditItem = ACTION_URL
					+ HREF_ACTION_EDIT_TOUR
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_DOC_ID + docId);

			iconUrl = _iconUrl_Tour;
			hoverMessage = SEARCH_APP_ACTION_EDIT_TOUR;

		} else if (isMarker) {

			final String tourMarkerLabel = resultItem.title;
			if (tourMarkerLabel != null) {
				itemTitleText = tourMarkerLabel;
			}

			hrefSelectItem = ACTION_URL
					+ HREF_ACTION_SELECT_MARKER
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_MARKER_ID + markerId)
					+ (HREF_PARAM_DOC_ID + docId);

			hrefEditItem = ACTION_URL
					+ HREF_ACTION_EDIT_MARKER
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_MARKER_ID + markerId)
					+ (HREF_PARAM_DOC_ID + docId);

			iconUrl = _iconUrl_Marker;
			hoverMessage = SEARCH_APP_ACTION_EDIT_MARKER;

		} else if (isWayPoint) {

			final String tourMarkerLabel = resultItem.title;
			if (tourMarkerLabel != null) {
				itemTitleText = tourMarkerLabel;
			}

			hrefSelectItem = ACTION_URL
					+ HREF_ACTION_SELECT_WAY_POINT
					+ (HREF_PARAM_TOUR_ID + tourId)
					+ (HREF_PARAM_MARKER_ID + markerId)
					+ (HREF_PARAM_DOC_ID + docId);

			iconUrl = _iconUrl_WayPoint;
			hoverMessage = SEARCH_APP_ACTION_EDIT_MARKER;
		}

		if (itemTitleText == null) {
			itemTitleText = UI.EMPTY_STRING;
		}

		String itemTitle = itemTitleText;
		if (itemTitle.length() == 0) {
			// show new line that the icon is not overwritten
			itemTitle = "</br>"; //$NON-NLS-1$
		}

		final String description = resultItem.description;
		final boolean isDescription = description != null;

		// hovered actions
		if (hrefEditItem != null) {

			sb.append("<div class='action-container'>" //$NON-NLS-1$
					+ ("<table><tbody><tr>") //$NON-NLS-1$
					+ (TAG_TD + createHTML_20_Action(hrefEditItem, hoverMessage, _actionUrl_EditImage) + TAG_TD_END)
					+ "</tr></tbody></table>" // //$NON-NLS-1$
					+ "</div>\n"); //$NON-NLS-1$
		}

		sb.append("<table><tbody><tr>"); //$NON-NLS-1$
		{
			/*
			 * Item image
			 */
			sb.append("<td class='item-image'>"); //$NON-NLS-1$
			sb.append("<img src='" + iconUrl + "'></img>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(TAG_TD_END);

			/*
			 * Item content
			 */
			sb.append("<td style='width:100%;'>"); //$NON-NLS-1$
			{
				// title
				if (isDescription) {
					sb.append("<span class='item-title'>" + itemTitle + "</span>"); //$NON-NLS-1$ //$NON-NLS-2$
				} else {
					sb.append("<span class='item-title-no-description'>" + itemTitle + "</span>"); //$NON-NLS-1$ //$NON-NLS-2$
				}

				// description
				if (isDescription) {
					sb.append("<div class='item-description'>"); //$NON-NLS-1$
					sb.append(description);
					sb.append("</div>\n"); //$NON-NLS-1$
				}

				// info
				if (_isUI_ShowDateTime || _isUI_ShowItemNumber || _isUI_ShowLuceneDocId) {

					sb.append("<div class='item-info'>"); //$NON-NLS-1$
					sb.append("<table><tbody><tr>"); //$NON-NLS-1$
					{
						if (_isUI_ShowDateTime) {

							final long tourStartTime = resultItem.tourStartTime;
							if (tourStartTime != 0) {

								final DateTime dt = new DateTime(tourStartTime);

								sb.append(TAG_TD + String.format("%s", _dateFormatter.print(dt.getMillis())) //$NON-NLS-1$
										+ TAG_TD_END);

							}
						}

						if (_isUI_ShowItemNumber) {
							sb.append(TAG_TD + Integer.toString(itemNumber) + TAG_TD_END);
						}

						if (_isUI_ShowLuceneDocId) {
							sb.append(TAG_TD + String.format("%d", docId) + TAG_TD_END); //$NON-NLS-1$
						}
					}
					sb.append("</tr></tbody></table>"); //$NON-NLS-1$
					sb.append("</div>\n"); //$NON-NLS-1$
				}
			}
			sb.append(TAG_TD_END);
		}
		sb.append("</tr></tbody></table>"); //$NON-NLS-1$

		final ItemResponse itemResponse = new ItemResponse();
		itemResponse.createdHtml = sb.toString();
		itemResponse.selectedId = hrefSelectItem;
		itemResponse.isMarker = isMarker;
		itemResponse.isTour = isTour;
		itemResponse.isWayPoint = isWayPoint;
		itemResponse.actionUrl_EditItem = hrefEditItem;

		return itemResponse;
	}

	private String createHTML_20_Action(final String actionUrl, final String hoverMessage, final String backgroundImage) {

		String url;

		/*
		 * Action is fired with an xhr request to the server
		 */

		url = " href='#anchor-without-scrolling'" //$NON-NLS-1$
				+ " onclick=" //$NON-NLS-1$
				+ "'" //$NON-NLS-1$
				+ (" tourbook.search.SearchApp.action(\"" + actionUrl + "\");") //$NON-NLS-1$ //$NON-NLS-2$
				+ " return false;" //$NON-NLS-1$
				+ "'"; //$NON-NLS-1$

		return "<a class='action'" // //$NON-NLS-1$
				+ (" style='background-image: url(" + backgroundImage + ");'") //$NON-NLS-1$ //$NON-NLS-2$
				+ url
				+ (" title='" + hoverMessage + "'") //$NON-NLS-1$ //$NON-NLS-2$
				+ ">" // //$NON-NLS-1$
				+ "</a>"; //$NON-NLS-1$
	}

	private String getContentRange(final SearchResult searchResult, final int searchPosFrom, final int allItemSize) {

		String contentRange;

		if (searchResult == null || allItemSize == 0) {

			contentRange = CONTENT_RANGE_ZERO;

		} else {

			contentRange = String.format(
					CONTENT_RANGE_ITEMS,
					searchPosFrom,
					(searchPosFrom + allItemSize - 1),
					searchResult.totalHits);
		}

		return contentRange;
	}

	@Override
	public void handleXHREvent(final HttpExchange httpExchange, final StringBuilder log) throws IOException {

		// get parameters from url query string
		@SuppressWarnings("unchecked")
		final Map<String, Object> params = (Map<String, Object>) httpExchange
				.getAttribute(RequestParameterFilter.ATTRIBUTE_PARAMETERS);

		String response = UI.EMPTY_STRING;
		final Object action = params.get(XHR_PARAM_ACTION);

		if (XHR_ACTION_SEARCH.equals(action)) {

			response = xhr_Search(params, httpExchange, log);

		} else if (XHR_ACTION_ITEM_ACTION.equals(action)) {

			xhr_ItemAction(params);

		} else if (XHR_ACTION_SELECT.equals(action)) {

			xhr_Select(params);

		} else if (XHR_ACTION_GET_SEARCH_OPTIONS.equals(action)) {

			response = xhr_GetSearchOptions(params);

		} else if (XHR_ACTION_GET_STATE.equals(action)) {

			response = xhr_GetState(params);

		} else if (XHR_ACTION_SET_SEARCH_OPTIONS.equals(action)) {

			response = xhr_SetSearchOptions(params);

//		} else if (XHR_ACTION_GET_STATE.equals(action)) {
//
//			xhr_SetState(params);

		} else if (XHR_ACTION_PROPOSALS.equals(action)) {

			response = xhr_Proposals(params);
		}

		writeRespone(httpExchange, response);
	}

	private void writeRespone(final HttpExchange httpExchange, final String response) {

		OutputStream os = null;

		try {

//			response.setContentType("application/json;charset=UTF-8");
			final byte[] convertedResponse = response.getBytes(WEB.UTF_8);

			httpExchange.sendResponseHeaders(200, convertedResponse.length);

			os = httpExchange.getResponseBody();
			os.write(convertedResponse);

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {
			Util.close(os);
		}
	}

	private String xhr_GetSearchOptions(final Map<String, Object> params) {

		// ensure state options are set
		setInternalSearchOptions();

		final JSONObject responceObj = new JSONObject();

		responceObj.put(JSON_IS_EASE_SEARCHING, _isUI_EaseSearching);

		responceObj.put(JSON_IS_SHOW_CONTENT_ALL, _isUI_ShowContentAll);
		responceObj.put(JSON_IS_SHOW_CONTENT_MARKER, _isUI_ShowContentMarker);
		responceObj.put(JSON_IS_SHOW_CONTENT_TOUR, _isUI_ShowContentTour);
		responceObj.put(JSON_IS_SHOW_CONTENT_WAYPOINT, _isUI_ShowContentWaypoint);

		responceObj.put(JSON_IS_SHOW_DATE_TIME, _isUI_ShowDateTime);
		responceObj.put(JSON_IS_SHOW_ITEM_NUMBER, _isUI_ShowItemNumber);
		responceObj.put(JSON_IS_SHOW_LUCENE_ID, _isUI_ShowLuceneDocId);

		responceObj.put(JSON_IS_SORT_BY_DATE_ASCENDING, _isUI_SortDateAscending);

		return responceObj.toString();
	}

	private String xhr_GetState(final Map<String, Object> params) {

		final String searchText = Util.getStateString(state, STATE_CURRENT_SEARCH_TEXT, UI.EMPTY_STRING);

		final JSONObject responceObj = new JSONObject();

		responceObj.put(JSON_STATE_SEARCH_TEXT, searchText);

		return responceObj.toString();
	}

	private void xhr_ItemAction(final Map<String, Object> params) throws UnsupportedEncodingException {

		final Object xhrParameter = params.get(XHR_PARAM_ACTION_URL);

		if (xhrParameter instanceof String) {

			final String xhrAction = URLDecoder.decode((String) xhrParameter, WEB.UTF_8);

			// run in UI thread
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					hrefAction(xhrAction);
				}
			});
		}
	}

	private String xhr_Proposals(final Map<String, Object> params) throws UnsupportedEncodingException {

		final JSONObject jsonResponse = new JSONObject();

		List<LookupResult> proposals = null;
		final Object xhrSearchText = params.get(XHR_PARAM_SEARCH_TEXT);

		if (xhrSearchText instanceof String) {

			final String searchText = URLDecoder.decode((String) xhrSearchText, WEB.UTF_8);

			proposals = FTSearchManager.getProposals(searchText);
		}

		if (proposals == null || proposals.size() == 0) {
			return UI.EMPTY_STRING;
		}

		final JSONArray allItems = new JSONArray();
		for (final LookupResult lookupResult : proposals) {

			final String proposal = lookupResult.key.toString();

			final JSONObject item = new JSONObject();
			item.put(JSON_ID, proposal);
			item.put(JSON_NAME, proposal);

			allItems.put(item);
		}

		jsonResponse.put("items", allItems); //$NON-NLS-1$

		final String response = jsonResponse.toString();

		return response;
	}

	private String xhr_Search(final Map<String, Object> params, final HttpExchange httpExchange, final StringBuilder log)
			throws UnsupportedEncodingException {

		final long start = System.nanoTime();

		final Headers headers = httpExchange.getRequestHeaders();

		final JSONArray allItems = new JSONArray();

		final String xhrSearchText = (String) params.get(XHR_PARAM_SEARCH_TEXT);
		final String range = headers.getFirst(REQUEST_HEADER_RANGE);

		int searchPosFrom = 0;
		int searchPosTo = 0;

		if (range != null) {

			final String[] ranges = range.substring("items=".length()).split("-"); //$NON-NLS-1$ //$NON-NLS-2$

			searchPosFrom = Integer.valueOf(ranges[0]);
			searchPosTo = Integer.valueOf(ranges[1]);
		}

		SearchResult searchResult = null;
		int allItemSize = 0;

		if (xhrSearchText != null) {

			if (WebContentServer.LOG_XHR) {
				log.append("range: " + searchPosFrom + "-" + searchPosTo + "\t" + headers.entrySet()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}

			String searchText = URLDecoder.decode(xhrSearchText, WEB.UTF_8);

			// keep search text in state
			state.put(STATE_CURRENT_SEARCH_TEXT, searchText);

			// ensure white space is removed
			searchText = searchText.trim();

			if (searchText.endsWith(UI.SYMBOL_STAR) == false && _isUI_EaseSearching) {

				// Append a * otherwise nothing is found
				searchText += UI.SYMBOL_STAR;
			}

			searchResult = FTSearchManager.searchByPosition(searchText, searchPosFrom, searchPosTo);

			/*
			 * create items html
			 */
			int itemIndex = 0;

			for (final SearchResultItem resultItem : searchResult.items) {

				final StringBuilder sb = new StringBuilder();
				final int itemNumber = searchPosFrom + (++itemIndex);
				ItemResponse itemResponse = null;

				sb.append("<div" + (" class='" + CSS_ITEM_CONTAINER + "'") + " id='" + resultItem.docId + "'>\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				{
					itemResponse = createHTML_10_Item(resultItem, itemNumber);
					sb.append(itemResponse.createdHtml);
				}
				sb.append("</div>\n"); //$NON-NLS-1$

				/*
				 * Create JSON for an item.
				 */
				final JSONObject jsonResponse = new JSONObject();

				// set unique grid row id
				jsonResponse.put(JSON_ID, resultItem.docId);

				jsonResponse.put(JSON_HTML_CONTENT, sb.toString());
				jsonResponse.put(JSON_SELECTED_ID, itemResponse.selectedId);
				jsonResponse.put(JSON_ITEM_ACTION_URL_EDIT_ITEM, itemResponse.actionUrl_EditItem);
				jsonResponse.put(JSON_ITEM_IS_MARKER, itemResponse.isMarker);
				jsonResponse.put(JSON_ITEM_IS_TOUR, itemResponse.isTour);
				jsonResponse.put(JSON_ITEM_IS_WAYPOINT, itemResponse.isWayPoint);

				allItems.put(jsonResponse);
			}

			allItemSize = allItems.length();

		} else {

			// also keep empty search text

//			state.put(STATE_CURRENT_SEARCH_TEXT, UI.EMPTY_STRING);
		}

		final Headers responseHeaders = httpExchange.getResponseHeaders();
		responseHeaders.set(WEB.RESPONSE_HEADER_CONTENT_TYPE, WEB.CONTENT_TYPE_APPLICATION_JSON);

		// this is very important otherwise nothing is displayed
		responseHeaders.set(
				WEB.RESPONSE_HEADER_CONTENT_RANGE,
				getContentRange(searchResult, searchPosFrom, allItemSize));

		final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
		String searchTime;
		if (timeDiff < 1.0) {
			searchTime = String.format("%.2f ms", timeDiff); //$NON-NLS-1$
		} else if (timeDiff < 10.0) {
			searchTime = String.format("%.1f ms", timeDiff); //$NON-NLS-1$
		} else {
			searchTime = String.format("%.0f ms", timeDiff); //$NON-NLS-1$
		}

		final int totalHits = searchResult == null ? 0 : searchResult.totalHits;

		/*
		 * Create JSON response
		 */
		final JSONObject response = new JSONObject();

		response.put("items", allItems); //$NON-NLS-1$
		response.put("searchTime", searchTime); //$NON-NLS-1$
		response.put("totalHits", totalHits); //$NON-NLS-1$

		return response.toString();
	}

	private void xhr_Select(final Map<String, Object> params) throws UnsupportedEncodingException {

		final Object xhrSelectedId = params.get(XHR_PARAM_SELECTED_ID);

		if (xhrSelectedId instanceof String) {

			final String xhrAction = URLDecoder.decode((String) xhrSelectedId, WEB.UTF_8);

			// run in UI thread
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					hrefAction(xhrAction);
				}
			});
		}
	}

	/**
	 * Set search options from the web UI.
	 * 
	 * @param params
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private String xhr_SetSearchOptions(final Map<String, Object> params) throws UnsupportedEncodingException {

		final JSONObject responceObj = new JSONObject();

		final Object xhrSearchOptions = params.get(XHR_PARAM_SEARCH_OPTIONS);
		final JSONObject jsonSearchOptions = WEB.getJSONObject(xhrSearchOptions);

		if (jsonSearchOptions.isNull("isRestoreDefaults") == false) { //$NON-NLS-1$

			// the action 'Restore Default' is selected in the web UI

			saveDefaultSearchOption();
			setInternalSearchOptions();

			// set flag that defaults are returned
			responceObj.put(JSON_IS_SEARCH_OPTIONS_DEFAULT, true);

			// create xhr response with default values
			responceObj.put(JSON_IS_EASE_SEARCHING, STATE_IS_EASE_SEARCHING_DEFAULT);

			responceObj.put(JSON_IS_SHOW_CONTENT_ALL, STATE_IS_SHOW_CONTENT_ALL_DEFAULT);
			responceObj.put(JSON_IS_SHOW_CONTENT_MARKER, STATE_IS_SHOW_CONTENT_MARKER_DEFAULT);
			responceObj.put(JSON_IS_SHOW_CONTENT_TOUR, STATE_IS_SHOW_CONTENT_TOUR_DEFAULT);
			responceObj.put(JSON_IS_SHOW_CONTENT_WAYPOINT, STATE_IS_SHOW_CONTENT_WAYPOINT_DEFAULT);

			responceObj.put(JSON_IS_SHOW_DATE_TIME, STATE_IS_SHOW_DATE_TIME_DEFAULT);
			responceObj.put(JSON_IS_SHOW_ITEM_NUMBER, STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);
			responceObj.put(JSON_IS_SHOW_LUCENE_ID, STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);

			responceObj.put(JSON_IS_SORT_BY_DATE_ASCENDING, STATE_IS_SORT_DATE_ASCENDING_DEFAULT);

		} else {

			// update state

			state.put(STATE_IS_EASE_SEARCHING, jsonSearchOptions.getBoolean(JSON_IS_EASE_SEARCHING));

			state.put(STATE_IS_SHOW_CONTENT_ALL, jsonSearchOptions.getBoolean(JSON_IS_SHOW_CONTENT_ALL));
			state.put(STATE_IS_SHOW_CONTENT_MARKER, jsonSearchOptions.getBoolean(JSON_IS_SHOW_CONTENT_MARKER));
			state.put(STATE_IS_SHOW_CONTENT_TOUR, jsonSearchOptions.getBoolean(JSON_IS_SHOW_CONTENT_TOUR));
			state.put(STATE_IS_SHOW_CONTENT_WAYPOINT, jsonSearchOptions.getBoolean(JSON_IS_SHOW_CONTENT_WAYPOINT));

			state.put(STATE_IS_SHOW_DATE_TIME, jsonSearchOptions.getBoolean(JSON_IS_SHOW_DATE_TIME));
			state.put(STATE_IS_SHOW_ITEM_NUMBER, jsonSearchOptions.getBoolean(JSON_IS_SHOW_ITEM_NUMBER));
			state.put(STATE_IS_SHOW_LUCENE_DOC_ID, jsonSearchOptions.getBoolean(JSON_IS_SHOW_LUCENE_ID));

			state.put(STATE_IS_SORT_DATE_ASCENDING, jsonSearchOptions.getBoolean(JSON_IS_SORT_BY_DATE_ASCENDING));

			setInternalSearchOptions();

			// create xhr response
			responceObj.put(JSON_IS_SEARCH_OPTIONS_DEFAULT, false);
		}

		return responceObj.toString();
	}

}
