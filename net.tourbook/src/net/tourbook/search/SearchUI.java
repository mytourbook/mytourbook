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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
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
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;
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
public class SearchUI implements XHRHandler, DisposeListener {

	private static final String				JSON_STATE_SEARCH_TEXT					= "searchText";										//$NON-NLS-1$

	private static final String				IMAGE_ACTION_TOUR_WAY_POINT				= net.tourbook.map2.Messages.Image_Action_TourWayPoint;

	final static IDialogSettings			state									= TourbookPlugin
																							.getState("net.tourbook.search.SearchUI");		//$NON-NLS-1$

	private static final String				STATE_CURRENT_SEARCH_TEXT				= "STATE_CURRENT_SEARCH_TEXT";							//$NON-NLS-1$

	static final String						STATE_IS_SHOW_CONTENT_ALL				= "STATE_IS_SHOW_CONTENT_ALL";							//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_ALL_DEFAULT		= true;
	static final String						STATE_IS_SHOW_CONTENT_MARKER			= "STATE_IS_SHOW_CONTENT_MARKER";						//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_MARKER_DEFAULT	= true;
	static final String						STATE_IS_SHOW_CONTENT_TOUR				= "STATE_IS_SHOW_CONTENT_TOUR";						//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_TOUR_DEFAULT		= true;
	static final String						STATE_IS_SHOW_CONTENT_WAYPOINT			= "STATE_IS_SHOW_CONTENT_WAYPOINT";					//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_CONTENT_WAYPOINT_DEFAULT	= true;

	static final String						STATE_IS_SHOW_DATE_TIME					= "STATE_IS_SHOW_DATE_TIME";							//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_DATE_TIME_DEFAULT			= false;
	//
	static final String						STATE_IS_SHOW_ITEM_NUMBER				= "STATE_IS_SHOW_ITEM_NUMBER";							//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_ITEM_NUMBER_DEFAULT		= false;
	//
	static final String						STATE_IS_SHOW_LUCENE_DOC_ID				= "STATE_IS_SHOW_LUCENE_DOC_ID";						//$NON-NLS-1$
	static final boolean					STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT		= false;
	//
	static final String						STATE_IS_SORT_DATE_ASCENDING			= "STATE_IS_SORT_DATE_ASCENDING";						//$NON-NLS-1$
	static final boolean					STATE_IS_SORT_DATE_ASCENDING_DEFAULT	= false;

	private static final String				REQUEST_HEADER_RANGE					= "Range";												//$NON-NLS-1$

	private static final String				CONTENT_RANGE_ITEMS						= "items %d-%d/%d";									//$NON-NLS-1$
	private static final String				CONTENT_RANGE_ZERO						= "0-0/0";												//$NON-NLS-1$

	/*
	 * This will handle all xhr actions, they are also defined in SearchMgr.js
	 */
	private static final String				XHR_SEARCH_INPUT_HANDLER				= "/xhrSearch";										//$NON-NLS-1$
	//
	private static final String				XHR_ACTION_ITEM_ACTION					= "itemAction";										//$NON-NLS-1$
	private static final String				XHR_ACTION_PROPOSALS					= "proposals";											//$NON-NLS-1$
	private static final String				XHR_ACTION_SEARCH						= "search";											//$NON-NLS-1$
	private static final String				XHR_ACTION_SELECT						= "select";											//$NON-NLS-1$
	private static final String				XHR_ACTION_GET_SEARCH_OPTIONS			= "getSearchOptions";									//$NON-NLS-1$
	private static final String				XHR_ACTION_SET_SEARCH_OPTIONS			= "setSearchOptions";									//$NON-NLS-1$
	private static final String				XHR_ACTION_GET_STATE					= "getState";											//$NON-NLS-1$
//	private static final String				XHR_ACTION_SET_STATE					= "setState";											//$NON-NLS-1$
																																			//
	private static final String				XHR_PARAM_ACTION						= "action";											//$NON-NLS-1$
	private static final String				XHR_PARAM_ACTION_URL					= "actionUrl";											//$NON-NLS-1$
	private static final String				XHR_PARAM_SEARCH_OPTIONS				= "searchOptions";										//$NON-NLS-1$
	private static final String				XHR_PARAM_SEARCH_TEXT					= JSON_STATE_SEARCH_TEXT;
	private static final String				XHR_PARAM_SELECTED_ID					= "selectedId";										//$NON-NLS-1$
	private static final String				XHR_PARAM_STATE							= "state";												//$NON-NLS-1$
	//
	/*
	 * JSON response values
	 */
	private static final String				JSON_ID									= "id";												//$NON-NLS-1$
	private static final String				JSON_NAME								= "name";												//$NON-NLS-1$
	private static final String				JSON_HTML_CONTENT						= "htmlContent";										//$NON-NLS-1$
	private static final String				JSON_SELECTED_ID						= "selectedId";										//$NON-NLS-1$
	//
	// search options
	private static final String				JSON_IS_SEARCH_OPTIONS_DEFAULT			= "isSearchOptionsDefault";							//$NON-NLS-1$
	//
	private static final String				JSON_IS_SHOW_CONTENT_ALL				= "isShowContentAll";									//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_CONTENT_MARKER				= "isShowContentMarker";								//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_CONTENT_TOUR				= "isShowContentTour";									//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_CONTENT_WAYPOINT			= "isShowContentWaypoint";								//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_LUCENE_ID					= "isShowLuceneID";									//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_ITEM_NUMBER				= "isShowItemNumber";									//$NON-NLS-1$
	private static final String				JSON_IS_SHOW_DATE_TIME					= "isShowDateTime";									//$NON-NLS-1$
	private static final String				JSON_IS_SORT_BY_DATE_ASCENDING			= "isSortByDateAscending";								//$NON-NLS-1$
	//
	private static final String				SEARCH_FOLDER							= "/tourbook/search/";									//$NON-NLS-1$
	private static final String				SEARCH_PAGE								= "search.mthtml";										//$NON-NLS-1$
	private static final String				SEARCH_SWT_CSS_FILE						= SEARCH_FOLDER + "search-swt.css";					//$NON-NLS-1$
	static String							SEARCH_SWT_CSS_STYLE;
	//
	private static String					_actionUrl_EditImage;
	private static String					_iconUrl_Tour;
	private static String					_iconUrl_Marker;
	private static String					_iconUrl_WayPoint;
	//
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
	static final String						TAG_TD									= "<td>";												//$NON-NLS-1$
	static final String						TAG_TD_END								= "</td>";												//$NON-NLS-1$
	static final String						CSS_ITEM_CONTAINER						= "item-container";									//$NON-NLS-1$

	static final String						PAGE_ABOUT_BLANK						= "about:blank";										//$NON-NLS-1$

	/**
	 * This is necessary otherwise XULrunner in Linux do not fire a location change event.
	 */
	static final String						ACTION_URL								= WebContentServer.SERVER_URL
																							+ "/action?";									//$NON-NLS-1$

	static final String						HREF_TOKEN								= "&";													//$NON-NLS-1$
	static final String						HREF_VALUE_SEP							= "=";													//$NON-NLS-1$

	static final String						PARAM_ACTION							= "action";											//$NON-NLS-1$
	static final String						PARAM_DOC_ID							= "docId";												//$NON-NLS-1$
	static final String						PARAM_MARKER_ID							= "markerId";											//$NON-NLS-1$
	static final String						PARAM_TOUR_ID							= "tourId";											//$NON-NLS-1$

	static final String						ACTION_EDIT_MARKER						= "EditMarker";										//$NON-NLS-1$
	static final String						ACTION_EDIT_TOUR						= "EditTour";											//$NON-NLS-1$
	static final String						ACTION_SELECT_TOUR						= "SelectTour";										//$NON-NLS-1$
	static final String						ACTION_SELECT_MARKER					= "SelectMarker";										//$NON-NLS-1$
	static final String						ACTION_SELECT_WAY_POINT					= "SelectWayPoint";									//$NON-NLS-1$

	static final String						HREF_ACTION_EDIT_MARKER;
	static final String						HREF_ACTION_EDIT_TOUR;
	static final String						HREF_ACTION_SELECT_TOUR;
	static final String						HREF_ACTION_SELECT_MARKER;
	static final String						HREF_ACTION_SELECT_WAY_POINT;

	static final String						HREF_PARAM_DOC_ID;
	static final String						HREF_PARAM_MARKER_ID;
	static final String						HREF_PARAM_TOUR_ID;

	static {

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

		SEARCH_SWT_CSS_STYLE = "<style>" + WEB.getFileContent(SEARCH_SWT_CSS_FILE, false) + "</style>"; //$NON-NLS-1$ //$NON-NLS-2$

		// initialize search options
		setInternalSearchOptions();
	}

	private ViewPart						_view;
	private Browser							_browser;

	private boolean							_isModalDialogOpen						= false;

	private PostSelectionProvider			_postSelectionProvider;

	private boolean							_isWebUI;

	class ItemResponse {

		String	createdHtml;
		String	selectedId;

	}

	SearchUI(	final ViewPart view,
				final Browser browser,
				final PostSelectionProvider postSelectionProvider,
				final boolean isWebUI) {

		_view = view;
		_postSelectionProvider = postSelectionProvider;
		_isWebUI = isWebUI;

		// ensure web server is started
		WebContentServer.start();

		WebContentServer.addXHRHandler(XHR_SEARCH_INPUT_HANDLER, this);

		_browser = browser;
		_browser.addDisposeListener(this);

		/*
		 * set image urls
		 */
		_iconUrl_Tour = getIconUrl(Messages.Image__TourChart);
		_iconUrl_Marker = getIconUrl(Messages.Image__TourMarker);
		_iconUrl_WayPoint = getIconUrl(IMAGE_ACTION_TOUR_WAY_POINT);

		_actionUrl_EditImage = getIconUrl(Messages.Image__quick_edit);

		if (_isWebUI) {

			_browser.addLocationListener(new LocationAdapter() {
				@Override
				public void changing(final LocationEvent event) {
					onBrowserLocation(event);
				}
			});

// !!! This will crash the whole app on Linux !!!
//			final String searchUrl = WEB.SERVER_URL + SEARCH_FOLDER + SEARCH_PAGE;
//			_browser.setUrl(searchUrl);
//
// This worked partly, Dojo could not load all web resources
//			final String html = WEB.getFileContent(SEARCH_FOLDER + SEARCH_PAGE, true);
//			_browser.setText(html);

			final String searchUrl = WEB.SERVER_URL + SEARCH_FOLDER + SEARCH_PAGE;
			_browser.setUrl(searchUrl);
		}
	}

	/**
	 * Set defaults for the search options in the state.
	 */
	static void saveDefaultSearchOption() {

		state.put(STATE_IS_SHOW_CONTENT_ALL, STATE_IS_SHOW_CONTENT_ALL_DEFAULT);
		state.put(STATE_IS_SHOW_CONTENT_MARKER, STATE_IS_SHOW_CONTENT_MARKER_DEFAULT);
		state.put(STATE_IS_SHOW_CONTENT_TOUR, STATE_IS_SHOW_CONTENT_TOUR_DEFAULT);
		state.put(STATE_IS_SHOW_CONTENT_WAYPOINT, STATE_IS_SHOW_CONTENT_WAYPOINT_DEFAULT);

		state.put(STATE_IS_SHOW_DATE_TIME, STATE_IS_SHOW_DATE_TIME_DEFAULT);
		state.put(STATE_IS_SHOW_ITEM_NUMBER, STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);
		state.put(STATE_IS_SHOW_LUCENE_DOC_ID, STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);

		state.put(STATE_IS_SORT_DATE_ASCENDING, STATE_IS_SORT_DATE_ASCENDING_DEFAULT);
	}

	/**
	 * Set internal search options from the state. These internal options are used when creating the
	 * search result.
	 */
	static void setInternalSearchOptions() {

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

	ItemResponse createHTML_10_Item(final SearchResultItem resultItem, final int itemNumber) {

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
			hoverMessage = Messages.Search_View_Action_EditTour_Tooltip;

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
			hoverMessage = Messages.Search_View_Action_EditMarker_Tooltip;

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
			hoverMessage = Messages.Search_View_Action_EditMarker_Tooltip;
		}

		if (itemTitleText == null) {
			itemTitleText = UI.EMPTY_STRING;
		}

		String itemTitle = itemTitleText;
		if (itemTitle.length() == 0) {
			// show new line that the icon is not overwritten
			itemTitle = "</br>";
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

		if (_isWebUI == false) {

			// web UI is using the Dojo list which do not need an <a> tag because a selection event is fired
			sb.append("<a class='item'" //
					+ (" href='" + hrefSelectItem + "'") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" title='" + itemTitle + "'") //$NON-NLS-1$ //$NON-NLS-2$
					+ ">"); // //$NON-NLS-1$
		}

		{
			sb.append("<table><tbody><tr>");
			{
				/*
				 * Item image
				 */
				sb.append("<td class='item-image'>");
				sb.append("<img src='" + iconUrl + "'></img>");
				sb.append(TAG_TD_END);

				/*
				 * Item content
				 */
				sb.append("<td style='width:100%;'>");
				{
					// title
					if (isDescription) {
						sb.append("<span class='item-title'>" + itemTitle + "</span>");
					} else {
						sb.append("<span class='item-title-no-description'>" + itemTitle + "</span>");
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
						sb.append("<table><tbody><tr>");
						{
							if (_isUI_ShowDateTime) {

								final long tourStartTime = resultItem.tourStartTime;
								if (tourStartTime != 0) {

									final DateTime dt = new DateTime(tourStartTime);

									sb.append(TAG_TD
											+ String.format("%s", _dateFormatter.print(dt.getMillis()))
											+ TAG_TD_END);

								}
							}

							if (_isUI_ShowItemNumber) {
								sb.append(TAG_TD + Integer.toString(itemNumber) + TAG_TD_END);
							}

							if (_isUI_ShowLuceneDocId) {
								sb.append(TAG_TD + String.format("%d", docId) + TAG_TD_END);
							}
						}
						sb.append("</tr></tbody></table>");
						sb.append("</div>\n"); //$NON-NLS-1$
					}
				}
				sb.append(TAG_TD_END);
			}
			sb.append("</tr></tbody></table>");
		}

		if (_isWebUI == false) {
			sb.append("</a>");
		}

		final ItemResponse itemResponse = new ItemResponse();
		itemResponse.createdHtml = sb.toString();
		itemResponse.selectedId = hrefSelectItem;

		return itemResponse;
	}

	private String createHTML_20_Action(final String actionUrl, final String hoverMessage, final String backgroundImage) {

		String url;

		if (_isWebUI) {

			/*
			 * Action is fired with an xhr request to the server
			 */

			url = " href='#anchor-without-scrolling'"
					+ " onclick="
					+ "'"
					+ (" tourbook.search.SearchApp.action(\"" + actionUrl + "\");")
					+ " return false;"
					+ "'";
		} else {
			url = " href='" + actionUrl + "'";
		}

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

	private String getIconUrl(final String iconImage) {

		final String iconUrl = net.tourbook.ui.UI.getIconUrl(iconImage);

		if (_isWebUI) {
			return WEB.SERVER_URL + '/' + iconUrl;
		}

		return iconUrl;
	}

	@Override
	public void handleXHREvent(final HttpExchange httpExchange, final StringBuilder log) throws IOException {

		if (_browser.isDisposed()) {

			// this happened when the request is from an external browser.

			return;
		}

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

	void hrefActionEditTour(final Long tourId) {

		// ensure this dialog is modal (only one dialog can be opened)
		if (_isModalDialogOpen) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
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
					Display.getCurrent().getActiveShell(),
					tourData);

			if (dialogQuickEdit.open() == Window.OK) {

				saveModifiedTour(tourData);
			}

		} finally {
			_isModalDialogOpen = false;
		}
	}

	void hrefActionMarker(final String action, final long tourId, final long markerId) {

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
		case SearchUI.ACTION_EDIT_MARKER:
			hrefActionMarker_Edit(tourData, selectedTourMarker);
			break;

		case SearchUI.ACTION_SELECT_MARKER:
			hrefActionMarker_Select(tourData, selectedTourMarker);
			break;
		}
	}

	private void hrefActionMarker_Edit(final TourData tourData, final TourMarker tourMarker) {

		// ensure this dialog is modal (only one dialog can be opened)
		if (_isModalDialogOpen) {

			MessageDialog.openInformation(
					Display.getCurrent().getActiveShell(),
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
					_browser.getShell(),
					tourData,
					tourMarker);

			if (dialogMarker.open() == Window.OK) {
				saveModifiedTour(tourData);
			}

		} finally {

			_isModalDialogOpen = false;
		}
	}

	private void hrefActionMarker_Select(final TourData tourData, final TourMarker selectedTourMarker) {

		final ArrayList<TourMarker> selectedTourMarkers = new ArrayList<TourMarker>();
		selectedTourMarkers.add(selectedTourMarker);

		final SelectionTourMarker markerSelection = new SelectionTourMarker(tourData, selectedTourMarkers);

		// ensure that the selection provider contain the correct data
		_postSelectionProvider.setSelectionNoFireEvent(markerSelection);

		TourManager.fireEvent(//
				TourEventId.MARKER_SELECTION,
				markerSelection,
				_view.getSite().getPart());
	}

	void hrefActionWayPoint(final String action, final long tourId, final long markerId) {

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
		_postSelectionProvider.setSelection(selection);
	}

	private void onBrowserLocation(final LocationEvent event) {

		final String location = event.location;

		if (performAction(location)) {

			// keep current page when an action is performed, OTHERWISE the current page will disappear :-(
			event.doit = false;
		}
	}

	/**
	 * @param location
	 * @return Returns <code>true</code> when a action is performed.
	 */
	private boolean performAction(final String location) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] performAction()")
//				+ ("\tlocation: " + location));
//		// TODO remove SYSTEM.OUT.PRINTLN

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
			return false;
		}

		switch (action) {

		case ACTION_EDIT_TOUR:

			hrefActionEditTour(tourId);

			break;

		case ACTION_SELECT_TOUR:

			_postSelectionProvider.setSelection(new SelectionTourId(tourId));

			break;

		case ACTION_EDIT_MARKER:
		case ACTION_SELECT_MARKER:

			hrefActionMarker(action, tourId, markerId);

			break;

		case ACTION_SELECT_WAY_POINT:

			hrefActionWayPoint(action, tourId, markerId);

			break;
		}

		return true;
	}

	private void saveModifiedTour(final TourData tourData) {

		/*
		 * Run async because a tour save will fire a tour change event.
		 */
		_browser.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				TourManager.saveModifiedTour(tourData);
			}
		});
	}

	void setFocus() {
		_browser.setFocus();
	}

	@Override
	public void widgetDisposed(final DisposeEvent e) {

		WebContentServer.removeXHRHandler(XHR_SEARCH_INPUT_HANDLER);
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
			_browser.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					performAction(xhrAction);
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

		if (proposals.size() == 0) {
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

		jsonResponse.put("items", allItems);

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

			final String[] ranges = range.substring("items=".length()).split("-");

			searchPosFrom = Integer.valueOf(ranges[0]);
			searchPosTo = Integer.valueOf(ranges[1]);
		}

		SearchResult searchResult = null;
		int allItemSize = 0;

		if (xhrSearchText != null) {

			if (WebContentServer.LOG_XHR) {
				log.append("range: " + searchPosFrom + "-" + searchPosTo + "\t" + headers.entrySet());
			}

			String searchText = URLDecoder.decode(xhrSearchText, WEB.UTF_8);

			// keep search text in state
			state.put(STATE_CURRENT_SEARCH_TEXT, searchText);

			// ensure white space is removed
			searchText = searchText.trim();

			if (searchText.endsWith(UI.SYMBOL_STAR) == false) {

				// Append a * otherwise nothing is found
				searchText += UI.SYMBOL_STAR;
			}

			searchResult = FTSearchManager.searchByPosition(searchText, searchPosFrom, searchPosTo);

			/*
			 * create items html
			 */
			final int itemBaseNumber = searchResult.pageNumber * searchResult.hitsPerPage;
			int itemIndex = 0;

			for (final SearchResultItem resultItem : searchResult.items) {

				final StringBuilder sb = new StringBuilder();
				final int itemNumber = itemBaseNumber + (++itemIndex);
				ItemResponse itemResponse = null;

				sb.append("<div" + (" class='" + CSS_ITEM_CONTAINER + "'") + " id='" + resultItem.docId + "'>\n"); //$NON-NLS-1$
				{
					itemResponse = createHTML_10_Item(resultItem, itemNumber);
					sb.append(itemResponse.createdHtml);
				}
				sb.append("</div>\n"); //$NON-NLS-1$

				/*
				 * Create JSON for an item.
				 */
				final JSONObject jsonResponse = new JSONObject();

				jsonResponse.put(JSON_ID, resultItem.docId);
				jsonResponse.put(JSON_HTML_CONTENT, sb.toString());
				jsonResponse.put(JSON_SELECTED_ID, itemResponse.selectedId);

				allItems.put(jsonResponse);
			}

			allItemSize = allItems.length();

		} else {

			// also keep empty search text

			state.put(STATE_CURRENT_SEARCH_TEXT, UI.EMPTY_STRING);
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
			searchTime = String.format("%.2f ms", timeDiff);
		} else {
			searchTime = String.format("%.0f ms", timeDiff);
		}

		final int totalHits = searchResult == null ? 0 : searchResult.totalHits;

		/*
		 * Create JSON response
		 */
		final JSONObject response = new JSONObject();

		response.put("items", allItems);
		response.put("searchTime", searchTime);
		response.put("totalHits", totalHits);

		return response.toString();
	}

	private void xhr_Select(final Map<String, Object> params) throws UnsupportedEncodingException {

		final Object xhrSelectedId = params.get(XHR_PARAM_SELECTED_ID);

		if (xhrSelectedId instanceof String) {

			final String xhrAction = URLDecoder.decode((String) xhrSelectedId, WEB.UTF_8);

			// run in UI thread
			_browser.getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					performAction(xhrAction);
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

		if (jsonSearchOptions.isNull("isRestoreDefaults") == false) {

			// the action 'Restore Default' is selected in the web UI

			saveDefaultSearchOption();
			setInternalSearchOptions();

			// set flag that defaults are returned
			responceObj.put(JSON_IS_SEARCH_OPTIONS_DEFAULT, true);

			// create xhr response with default values
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
