/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.IconRequestMgr;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourWayPoint;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.DialogQuickEdit;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
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
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 */
public class SearchManager implements XHRHandler {

   private static final String          SEARCH_APP_ACTION_EDIT_MARKER               = tourbook.search.nls.Messages.Search_App_Action_EditMarker;
   private static final String          SEARCH_APP_ACTION_EDIT_TOUR                 = tourbook.search.nls.Messages.Search_App_Action_EditTour;

   private static final String          NL                                          = UI.NEW_LINE1;

   private final static IDialogSettings _state                                      = TourbookPlugin.getState("net.tourbook.search.SearchMgr"); //$NON-NLS-1$

   private static final String          JSON_STATE_SEARCH_TEXT                      = "searchText";                                             //$NON-NLS-1$

   private static final String          STATE_CURRENT_SEARCH_TEXT                   = "STATE_CURRENT_SEARCH_TEXT";                              //$NON-NLS-1$

   static final String                  STATE_IS_EASE_SEARCHING                     = "STATE_IS_EASE_SEARCHING";                                //$NON-NLS-1$
   static final boolean                 STATE_IS_EASE_SEARCHING_DEFAULT             = true;

   static final String                  STATE_IS_SEARCH_ALL                         = "STATE_IS_SEARCH_ALL";                                    //$NON-NLS-1$
   static final boolean                 STATE_IS_SEARCH_ALL_DEFAULT                 = true;
   static final String                  STATE_IS_SEARCH_MARKER                      = "STATE_IS_SEARCH_MARKER";                                 //$NON-NLS-1$
   static final boolean                 STATE_IS_SEARCH_MARKER_DEFAULT              = true;
   static final String                  STATE_IS_SEARCH_TOUR                        = "STATE_IS_SEARCH_TOUR";                                   //$NON-NLS-1$
   static final boolean                 STATE_IS_SEARCH_TOUR_DEFAULT                = true;
   static final String                  STATE_IS_SEARCH_TOUR_LOCATION_START         = "STATE_IS_SEARCH_TOUR_LOCATION_START";                    //$NON-NLS-1$
   static final boolean                 STATE_IS_SEARCH_TOUR_LOCATION_START_DEFAULT = true;
   static final String                  STATE_IS_SEARCH_TOUR_LOCATION_END           = "STATE_IS_SEARCH_TOUR_LOCATION_END";                      //$NON-NLS-1$
   static final boolean                 STATE_IS_SEARCH_TOUR_LOCATION_END_DEFAULT   = true;
   static final String                  STATE_IS_SEARCH_TOUR_WEATHER                = "STATE_IS_SEARCH_TOUR_WEATHER";                           //$NON-NLS-1$
   static final boolean                 STATE_IS_SEARCH_TOUR_WEATHER_DEFAULT        = true;
   static final String                  STATE_IS_SEARCH_WAYPOINT                    = "STATE_IS_SEARCH_WAYPOINT";                               //$NON-NLS-1$
   static final boolean                 STATE_IS_SEARCH_WAYPOINT_DEFAULT            = true;
   //
   static final String                  STATE_IS_SHOW_DATE                          = "STATE_IS_SHOW_DATE";                                     //$NON-NLS-1$
   static final boolean                 STATE_IS_SHOW_DATE_DEFAULT                  = false;
   static final String                  STATE_IS_SHOW_TIME                          = "STATE_IS_SHOW_TIME";                                     //$NON-NLS-1$
   static final boolean                 STATE_IS_SHOW_TIME_DEFAULT                  = false;
   static final String                  STATE_IS_SHOW_DESCRIPTION                   = "STATE_IS_SHOW_DESCRIPTION";                              //$NON-NLS-1$
   static final boolean                 STATE_IS_SHOW_DESCRIPTION_DEFAULT           = true;
   static final String                  STATE_IS_SHOW_ITEM_NUMBER                   = "STATE_IS_SHOW_ITEM_NUMBER";                              //$NON-NLS-1$
   static final boolean                 STATE_IS_SHOW_ITEM_NUMBER_DEFAULT           = false;
   static final String                  STATE_IS_SHOW_LUCENE_DOC_ID                 = "STATE_IS_SHOW_LUCENE_DOC_ID";                            //$NON-NLS-1$
   static final boolean                 STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT         = false;
   //
   static final String                  STATE_IS_SORT_DATE_ASCENDING                = "STATE_IS_SORT_DATE_ASCENDING";                           //$NON-NLS-1$
   static final boolean                 STATE_IS_SORT_DATE_ASCENDING_DEFAULT        = false;

   private static final String          REQUEST_HEADER_RANGE                        = "Range";                                                  //$NON-NLS-1$

   private static final String          CONTENT_RANGE_ITEMS                         = "items %d-%d/%d";                                         //$NON-NLS-1$
   private static final String          CONTENT_RANGE_ZERO                          = "0-0/0";                                                  //$NON-NLS-1$

   /*
    * This will handle all xhr actions, they are also defined in SearchMgr.js
    */
   private static final String XHR_SEARCH_INPUT_HANDLER      = "/xhrSearch";          //$NON-NLS-1$
   //
   private static final String XHR_ACTION_ITEM_ACTION        = "itemAction";          //$NON-NLS-1$
   private static final String XHR_ACTION_PROPOSALS          = "proposals";           //$NON-NLS-1$
   private static final String XHR_ACTION_SEARCH             = "search";              //$NON-NLS-1$
   private static final String XHR_ACTION_SELECT             = "select";              //$NON-NLS-1$
   private static final String XHR_ACTION_GET_SEARCH_OPTIONS = "getSearchOptions";    //$NON-NLS-1$
   private static final String XHR_ACTION_SET_SEARCH_OPTIONS = "setSearchOptions";    //$NON-NLS-1$
   private static final String XHR_ACTION_GET_STATE          = "getState";            //$NON-NLS-1$
   //
   private static final String XHR_PARAM_ACTION              = "action";              //$NON-NLS-1$
   private static final String XHR_PARAM_ACTION_URL          = "actionUrl";           //$NON-NLS-1$
   private static final String XHR_PARAM_SEARCH_OPTIONS      = "searchOptions";       //$NON-NLS-1$
   private static final String XHR_PARAM_SEARCH_TEXT         = JSON_STATE_SEARCH_TEXT;
   private static final String XHR_PARAM_SELECTED_ITEMS      = "selectedItems";       //$NON-NLS-1$

   /*
    * JSON response values
    */
   private static final String JSON_ID                        = "id";                 //$NON-NLS-1$
   private static final String JSON_NAME                      = "name";               //$NON-NLS-1$
   private static final String JSON_HTML_CONTENT              = "htmlContent";        //$NON-NLS-1$
   private static final String JSON_ITEM_ACTION_URL_EDIT_ITEM = "actionUrl_EditItem"; //$NON-NLS-1$
   private static final String JSON_ITEM_IS_MARKER            = "item_IsMarker";      //$NON-NLS-1$
   private static final String JSON_ITEM_IS_TOUR              = "item_IsTour";        //$NON-NLS-1$
   private static final String JSON_ITEM_IS_WAYPOINT          = "item_IsWaypoint";    //$NON-NLS-1$
   private static final String JSON_ITEM_ID_TOUR_ID           = "itemId_TourId";      //$NON-NLS-1$
   private static final String JSON_ITEM_ID_MARKER_ID         = "itemId_MarkerId";    //$NON-NLS-1$
   //
   // search options
   private static final String JSON_IS_SEARCH_OPTIONS_DEFAULT     = "isSearchOptionsDefault";      //$NON-NLS-1$
   //
   private static final String JSON_IS_EASE_SEARCHING             = "isEaseSearching";             //$NON-NLS-1$
   private static final String JSON_IS_SEARCH_ALL                 = "isSearch_All";                //$NON-NLS-1$
   private static final String JSON_IS_SEARCH_MARKER              = "isSearch_Marker";             //$NON-NLS-1$
   private static final String JSON_IS_SEARCH_WAYPOINT            = "isSearch_Waypoint";           //$NON-NLS-1$
   private static final String JSON_IS_SEARCH_TOUR                = "isSearch_Tour";               //$NON-NLS-1$
   private static final String JSON_IS_SEARCH_TOUR_LOCATION_START = "isSearch_Tour_LocationStart"; //$NON-NLS-1$
   private static final String JSON_IS_SEARCH_TOUR_LOCATION_END   = "isSearch_Tour_LocationEnd";   //$NON-NLS-1$
   private static final String JSON_IS_SEARCH_TOUR_WEATHER        = "isSearch_Tour_Weather";       //$NON-NLS-1$
   private static final String JSON_IS_SHOW_DATE                  = "isShowDate";                  //$NON-NLS-1$
   private static final String JSON_IS_SHOW_TIME                  = "isShowTime";                  //$NON-NLS-1$
   private static final String JSON_IS_SHOW_DESCRIPTION           = "isShowDescription";           //$NON-NLS-1$
   private static final String JSON_IS_SHOW_ITEM_NUMBER           = "isShowItemNumber";            //$NON-NLS-1$
   private static final String JSON_IS_SHOW_LUCENE_ID             = "isShowLuceneID";              //$NON-NLS-1$
   private static final String JSON_IS_SORT_BY_DATE_ASCENDING     = "isSortByDateAscending";       //$NON-NLS-1$
   //
   private static final String SEARCH_FOLDER                      = "/tourbook/search/";           //$NON-NLS-1$
   private static final String SEARCH_PAGE                        = "search.mthtml";               //$NON-NLS-1$
   //
   /**
    * This is necessary otherwise XULrunner in Linux do not fire a location change event.
    */
   static String               ACTION_URL;
   static String               SEARCH_URL;
   //
   private static String       _actionUrl_EditImage;
   private static String       _iconUrl_Tour;
   private static String       _iconUrl_Marker;
   private static String       _iconUrl_WayPoint;
   //
   private static boolean      _isUI_EaseSearching;
   private static boolean      _isUI_Search_All;
   private static boolean      _isUI_Search_Marker;
   private static boolean      _isUI_Search_Tour;
   private static boolean      _isUI_Search_Tour_LocationStart;
   private static boolean      _isUI_Search_Tour_LocationEnd;
   private static boolean      _isUI_Search_Tour_Weather;
   private static boolean      _isUI_Search_Waypoint;
   private static boolean      _isUI_Show_Date;
   private static boolean      _isUI_Show_Time;
   private static boolean      _isUI_Show_Description;
   private static boolean      _isUI_Show_ItemNumber;
   private static boolean      _isUI_Show_LuceneDocId;
   private static boolean      _isUI_Sort_DateAscending;
   //
   private static final String TAG_DIV_END                        = "</div>" + NL;                 //$NON-NLS-1$
   private static final String TAG_TABLE_TBODY                    = "<table><tbody>";              //$NON-NLS-1$
   private static final String TAG_TABLE_TBODY_END                = "</tbody></table>" + NL;       //$NON-NLS-1$
   private static final String TAG_TD                             = "<td>";                        //$NON-NLS-1$
   private static final String TAG_TD_END                         = "</td>" + NL;                  //$NON-NLS-1$
   private static final String TAG_TR                             = "<tr>";                        //$NON-NLS-1$
   private static final String TAG_TR_END                         = "</tr>" + NL;                  //$NON-NLS-1$

   private static final String CSS_ITEM_CONTAINER                 = "item-container";              //$NON-NLS-1$

   private static final String HREF_TOKEN                         = "&";                           //$NON-NLS-1$
   private static final String HREF_VALUE_SEP                     = "=";                           //$NON-NLS-1$

   private static final String PARAM_ACTION                       = "action";                      //$NON-NLS-1$
   private static final String PARAM_DOC_ID                       = "docId";                       //$NON-NLS-1$
   private static final String PARAM_MARKER_ID                    = "markerId";                    //$NON-NLS-1$
   private static final String PARAM_TOUR_ID                      = "tourId";                      //$NON-NLS-1$

   private static final String ACTION_EDIT_MARKER                 = "EditMarker";                  //$NON-NLS-1$
   private static final String ACTION_EDIT_TOUR                   = "EditTour";                    //$NON-NLS-1$
   private static final String ACTION_SELECT_TOUR                 = "SelectTour";                  //$NON-NLS-1$
   private static final String ACTION_SELECT_MARKER               = "SelectMarker";                //$NON-NLS-1$
   private static final String ACTION_SELECT_WAY_POINT            = "SelectWayPoint";              //$NON-NLS-1$

   private static final String HREF_ACTION_EDIT_MARKER;
   private static final String HREF_ACTION_EDIT_TOUR;

   private static final String HREF_PARAM_DOC_ID;
   private static final String HREF_PARAM_MARKER_ID;
   private static final String HREF_PARAM_TOUR_ID;

   static {

      ACTION_URL = WebContentServer.SERVER_URL + "/action?"; //$NON-NLS-1$
      SEARCH_URL = WebContentServer.SERVER_URL + SEARCH_FOLDER + SEARCH_PAGE;

      // e.g. ...&action=EditMarker...

      final String HREF_ACTION = HREF_TOKEN + PARAM_ACTION + HREF_VALUE_SEP;

      HREF_ACTION_EDIT_MARKER = HREF_ACTION + ACTION_EDIT_MARKER;
      HREF_ACTION_EDIT_TOUR = HREF_ACTION + ACTION_EDIT_TOUR;

      HREF_PARAM_DOC_ID = HREF_TOKEN + PARAM_DOC_ID + HREF_VALUE_SEP;
      HREF_PARAM_MARKER_ID = HREF_TOKEN + PARAM_MARKER_ID + HREF_VALUE_SEP;
      HREF_PARAM_TOUR_ID = HREF_TOKEN + PARAM_TOUR_ID + HREF_VALUE_SEP;

      /*
       * set image urls
       */
      _iconUrl_Tour = getIconUrl(ThemeUtil.getThemedImageName(Images.TourChart));
      _iconUrl_Marker = getIconUrl(ThemeUtil.getThemedImageName(Images.TourMarker));
      _iconUrl_WayPoint = getIconUrl(ThemeUtil.getThemedImageName(Images.TourWayPoint));

      _actionUrl_EditImage = getIconUrl(Images.App_Edit);

      // initialize search options
      setInternalSearchOptions();
   }

   private static SearchManager _searchMgr;

   private static ISearchView   _searchView;

   private static boolean       _isModalDialogOpen = false;

   private class ItemResponse {

      String  actionUrl_EditItem;

      String  createdHtml;

      boolean item_IsMarker;
      boolean item_IsTour;
      boolean item_IsWayPoint;

      String  itemId_TourId;
      String  itemId_MarkerId;   // is marker or waypoint
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

   private static SearchManager getInstance() {

      if (_searchMgr == null) {
         _searchMgr = new SearchManager();
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

         hrefAction_Tour_Select(new SelectionTourId(tourId));

         break;

      case ACTION_EDIT_MARKER:
      case ACTION_SELECT_MARKER:

         hrefAction_Marker(action, tourId, markerId);

         break;

      case ACTION_SELECT_WAY_POINT:

         hrefAction_WayPoint(tourId, markerId);

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
      case SearchManager.ACTION_EDIT_MARKER:
         hrefAction_Marker_Edit(tourData, selectedTourMarker);
         break;

      case SearchManager.ACTION_SELECT_MARKER:
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

      final ArrayList<TourMarker> selectedTourMarkers = new ArrayList<>();
      selectedTourMarkers.add(selectedTourMarker);

      final SelectionTourMarker markerSelection = new SelectionTourMarker(tourData, selectedTourMarkers);

      updateSelectionProvider(markerSelection);

      TourManager.fireEventWithCustomData(//
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

   private static void hrefAction_Tour_Select(final ISelection selection) {

      updateSelectionProvider(selection);

      TourManager.fireEventWithCustomData(//
            TourEventId.TOUR_SELECTION,
            selection,
            _searchView.getPart());
   }

   private static void hrefAction_WayPoint(final long tourId, final long markerId) {

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
      final Set<TourWayPoint> tourWayPoints = tourData.getTourWayPoints();
      for (final TourWayPoint wayPoint : tourWayPoints) {
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

// SET_FORMATTING_OFF

      _state.put(STATE_IS_EASE_SEARCHING,             STATE_IS_EASE_SEARCHING_DEFAULT);

      _state.put(STATE_IS_SEARCH_ALL,                 STATE_IS_SEARCH_ALL_DEFAULT);
      _state.put(STATE_IS_SEARCH_MARKER,              STATE_IS_SEARCH_MARKER_DEFAULT);
      _state.put(STATE_IS_SEARCH_TOUR,                STATE_IS_SEARCH_TOUR_DEFAULT);
      _state.put(STATE_IS_SEARCH_TOUR_LOCATION_START, STATE_IS_SEARCH_TOUR_LOCATION_START_DEFAULT);
      _state.put(STATE_IS_SEARCH_TOUR_LOCATION_END,   STATE_IS_SEARCH_TOUR_LOCATION_END_DEFAULT);
      _state.put(STATE_IS_SEARCH_TOUR_WEATHER,        STATE_IS_SEARCH_TOUR_WEATHER_DEFAULT);
      _state.put(STATE_IS_SEARCH_WAYPOINT,            STATE_IS_SEARCH_WAYPOINT_DEFAULT);

      _state.put(STATE_IS_SHOW_DATE,                  STATE_IS_SHOW_DATE_DEFAULT);
      _state.put(STATE_IS_SHOW_TIME,                  STATE_IS_SHOW_TIME_DEFAULT);
      _state.put(STATE_IS_SHOW_ITEM_NUMBER,           STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);
      _state.put(STATE_IS_SHOW_LUCENE_DOC_ID,         STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);

      _state.put(STATE_IS_SORT_DATE_ASCENDING,        STATE_IS_SORT_DATE_ASCENDING_DEFAULT);

// SET_FORMATTING_ON
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

// SET_FORMATTING_OFF

      _isUI_EaseSearching              = Util.getStateBoolean(_state, STATE_IS_EASE_SEARCHING,              STATE_IS_EASE_SEARCHING_DEFAULT);
      _isUI_Search_All                 = Util.getStateBoolean(_state, STATE_IS_SEARCH_ALL,                  STATE_IS_SEARCH_ALL_DEFAULT);
      _isUI_Search_Marker              = Util.getStateBoolean(_state, STATE_IS_SEARCH_MARKER,               STATE_IS_SEARCH_MARKER_DEFAULT);
      _isUI_Search_Tour                = Util.getStateBoolean(_state, STATE_IS_SEARCH_TOUR,                 STATE_IS_SEARCH_TOUR_DEFAULT);
      _isUI_Search_Tour_LocationStart  = Util.getStateBoolean(_state, STATE_IS_SEARCH_TOUR_LOCATION_START,  STATE_IS_SEARCH_TOUR_LOCATION_START_DEFAULT);
      _isUI_Search_Tour_LocationEnd    = Util.getStateBoolean(_state, STATE_IS_SEARCH_TOUR_LOCATION_END,    STATE_IS_SEARCH_TOUR_LOCATION_END_DEFAULT);
      _isUI_Search_Tour_Weather        = Util.getStateBoolean(_state, STATE_IS_SEARCH_TOUR_WEATHER,         STATE_IS_SEARCH_TOUR_WEATHER_DEFAULT);
      _isUI_Search_Waypoint            = Util.getStateBoolean(_state, STATE_IS_SEARCH_WAYPOINT,             STATE_IS_SEARCH_WAYPOINT_DEFAULT);

      _isUI_Show_Date                  = Util.getStateBoolean(_state, STATE_IS_SHOW_DATE,                   STATE_IS_SHOW_DATE_DEFAULT);
      _isUI_Show_Time                  = Util.getStateBoolean(_state, STATE_IS_SHOW_TIME,                   STATE_IS_SHOW_TIME_DEFAULT);
      _isUI_Show_Description           = Util.getStateBoolean(_state, STATE_IS_SHOW_DESCRIPTION,            STATE_IS_SHOW_DESCRIPTION_DEFAULT);
      _isUI_Show_LuceneDocId           = Util.getStateBoolean(_state, STATE_IS_SHOW_LUCENE_DOC_ID,          STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);
      _isUI_Show_ItemNumber            = Util.getStateBoolean(_state, STATE_IS_SHOW_ITEM_NUMBER,            STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);
      _isUI_Sort_DateAscending         = Util.getStateBoolean(_state, STATE_IS_SORT_DATE_ASCENDING,         STATE_IS_SORT_DATE_ASCENDING_DEFAULT);

// SET_FORMATTING_ON

      // update ftsearch manager parameters
      FTSearchManager.setSearchOptions(

            _isUI_Search_All,
            _isUI_Search_Marker,
            _isUI_Search_Tour,
            _isUI_Search_Tour_LocationStart,
            _isUI_Search_Tour_LocationEnd,
            _isUI_Search_Tour_Weather,
            _isUI_Search_Waypoint,
            _isUI_Sort_DateAscending,
            _isUI_Show_Description);
   }

   /**
    * Web content server is available only when the search view is opened.
    *
    * @param searchView
    */
   public static void setSearchView(final ISearchView searchView) {

      if (searchView == null) {

         // shutdown search service

         FTSearchManager.closeIndexReaderSuggester();

         WebContentServer.stop();

         WebContentServer.removeXHRHandler(XHR_SEARCH_INPUT_HANDLER);
         WebContentServer.setIconRequestHandler(null);

      } else {

         // start search service

         WebContentServer.start();

         WebContentServer.addXHRHandler(XHR_SEARCH_INPUT_HANDLER, SearchManager.getInstance());
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

   private String createContentRange(final SearchResult searchResult, final int searchPosFrom, final int numItems) {

      String contentRange;

      if (searchResult == null || numItems == 0) {

         contentRange = CONTENT_RANGE_ZERO;

      } else {

         contentRange = String.format(

               // "items %d-%d/%d"
               CONTENT_RANGE_ITEMS,

               searchPosFrom,
               searchPosFrom + numItems - 1,
               searchResult.totalHits);
      }

      return contentRange;
   }

   /**
    * @param allItems
    * @param searchPosFrom
    * @param searchResult
    * @return
    */
   private void createHTML_10_SearchResults(final JSONArray allItems, final int searchPosFrom, final SearchResult searchResult) {

      int itemIndex = 0;

      for (final SearchResultItem resultItem : searchResult.allItems) {

         final StringBuilder sb = new StringBuilder();
         final int itemNumber = searchPosFrom + (++itemIndex);
         ItemResponse itemResponse = null;

         sb.append("<div" + (" class='" + CSS_ITEM_CONTAINER + "'") + " id='" + resultItem.docId + "'>" + NL); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
         {
            itemResponse = createHTML_20_Item(resultItem, itemNumber);
            sb.append(itemResponse.createdHtml);
         }
         sb.append(TAG_DIV_END);

         /*
          * Create JSON for an item.
          */
         final JSONObject jsonResponse = new JSONObject();

         // set unique grid row id
         jsonResponse.put(JSON_ID, resultItem.docId);

         jsonResponse.put(JSON_HTML_CONTENT, sb.toString());
         jsonResponse.put(JSON_ITEM_ACTION_URL_EDIT_ITEM, itemResponse.actionUrl_EditItem);
         jsonResponse.put(JSON_ITEM_ID_TOUR_ID, itemResponse.itemId_TourId);
         jsonResponse.put(JSON_ITEM_ID_MARKER_ID, itemResponse.itemId_MarkerId);
         jsonResponse.put(JSON_ITEM_IS_MARKER, itemResponse.item_IsMarker);
         jsonResponse.put(JSON_ITEM_IS_TOUR, itemResponse.item_IsTour);
         jsonResponse.put(JSON_ITEM_IS_WAYPOINT, itemResponse.item_IsWayPoint);

         allItems.put(jsonResponse);
      }
   }

   private ItemResponse createHTML_20_Item(final SearchResultItem resultItem, final int itemNumber) {

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
      String itemTitleText = null;

      if (isTour) {

         final String tourTitle = resultItem.title;
         if (tourTitle != null) {
            itemTitleText = tourTitle;
         }

         hrefEditItem = ACTION_URL
               + HREF_ACTION_EDIT_TOUR
               + HREF_PARAM_TOUR_ID + tourId
               + HREF_PARAM_DOC_ID + docId;

         iconUrl = _iconUrl_Tour;
         hoverMessage = SEARCH_APP_ACTION_EDIT_TOUR;

      } else if (isMarker) {

         final String tourMarkerLabel = resultItem.title;
         if (tourMarkerLabel != null) {
            itemTitleText = tourMarkerLabel;
         }

         hrefEditItem = ACTION_URL
               + HREF_ACTION_EDIT_MARKER
               + HREF_PARAM_TOUR_ID + tourId
               + HREF_PARAM_MARKER_ID + markerId
               + HREF_PARAM_DOC_ID + docId;

         iconUrl = _iconUrl_Marker;
         hoverMessage = SEARCH_APP_ACTION_EDIT_MARKER;

      } else if (isWayPoint) {

         final String tourMarkerLabel = resultItem.title;
         if (tourMarkerLabel != null) {
            itemTitleText = tourMarkerLabel;
         }

         iconUrl = _iconUrl_WayPoint;
         hoverMessage = SEARCH_APP_ACTION_EDIT_MARKER;
      }

      if (itemTitleText == null) {
         itemTitleText = UI.EMPTY_STRING;
      }

      String itemTitle = itemTitleText;
      String description = resultItem.description;
      final boolean isAvailable_Description = _isUI_Show_Description && description != null;

      String tour_LocationStart = resultItem.locationStart;
      String tour_LocationEnd = resultItem.locationEnd;
      String tour_Weather = resultItem.weather;

      if (UI.IS_SCRAMBLE_DATA) {

         itemTitle = UI.scrambleText(itemTitle);
         description = UI.scrambleText(description);

         tour_LocationStart = UI.scrambleText(tour_LocationStart);
         tour_LocationEnd = UI.scrambleText(tour_LocationEnd);
         tour_Weather = UI.scrambleText(tour_Weather);
      }

      if (itemTitle.length() == 0) {

         // show new line that the icon is not overwritten
         itemTitle = "</br>"; //$NON-NLS-1$
      }

      final boolean isAvailable_Tour_LocationStart = tour_LocationStart != null && tour_LocationStart.length() > 0;
      final boolean isAvailable_Tour_LocationEnd = tour_LocationEnd != null && tour_LocationEnd.length() > 0;
      final boolean isAvailable_Tour_Weather = tour_Weather != null && tour_Weather.length() > 0;

      final StringBuilder sb = new StringBuilder();

      // hovered actions
      if (hrefEditItem != null) {

         sb.append("<div class='action-container'>" //$NON-NLS-1$
               + TAG_TABLE_TBODY
               + TAG_TR
               + TAG_TD + createHTML_30_Action(hrefEditItem, hoverMessage, _actionUrl_EditImage) + TAG_TD_END
               + TAG_TR_END
               + TAG_TABLE_TBODY_END
               + TAG_DIV_END);
      }

      sb.append(TAG_TABLE_TBODY);
      sb.append(TAG_TR);
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
            if (_isUI_Search_Tour
                  || _isUI_Search_Marker
                  || _isUI_Search_Waypoint

                  || _isUI_Show_Description) {

               /*
                * Title
                */
               if (isAvailable_Description) {
                  sb.append("<span class='item-title'>" + itemTitle + "</span>"); //$NON-NLS-1$ //$NON-NLS-2$
               } else {
                  sb.append("<span class='item-title-no-description'>" + itemTitle + "</span>"); //$NON-NLS-1$ //$NON-NLS-2$
               }

               /*
                * Description
                */
               if (isAvailable_Description) {
                  sb.append("<div class='item-description'>"); //$NON-NLS-1$
                  sb.append(description);
                  sb.append(TAG_DIV_END);
               }
            }

            /*
             * Start/end location
             */
            if (isAvailable_Tour_LocationStart
                  || isAvailable_Tour_LocationEnd) {

               sb.append("<div class='item-location'>"); //$NON-NLS-1$
               sb.append(TAG_TABLE_TBODY);
               sb.append(TAG_TR);
               {
                  if (isAvailable_Tour_LocationStart) {
                     sb.append(TAG_TD + tour_LocationStart + TAG_TD_END);
                  }

                  sb.append(TAG_TD + " ... " + TAG_TD_END); //$NON-NLS-1$

                  if (isAvailable_Tour_LocationEnd) {
                     sb.append(TAG_TD + tour_LocationEnd + TAG_TD_END);
                  }
               }
               sb.append(TAG_TR_END);
               sb.append(TAG_TABLE_TBODY_END);
               sb.append(TAG_DIV_END);
            }

            /*
             * Weather
             */
            if (isAvailable_Tour_Weather) {

               sb.append("<div class='item-weather'>"); //$NON-NLS-1$
               sb.append(tour_Weather);
               sb.append(TAG_DIV_END);
            }

            // info
            if (_isUI_Show_Date || _isUI_Show_Time || _isUI_Show_ItemNumber || _isUI_Show_LuceneDocId) {

               sb.append("<div class='item-info'>"); //$NON-NLS-1$
               sb.append(TAG_TABLE_TBODY);
               sb.append(TAG_TR);
               {
                  if (_isUI_Show_Date || _isUI_Show_Time) {

                     final long tourStartTime = resultItem.tourStartTime;
                     if (tourStartTime != 0) {

                        final ZonedDateTime dt = TimeTools.getZonedDateTime(tourStartTime);

                        String dateTimeText = null;

                        if (_isUI_Show_Date && _isUI_Show_Time) {

                           dateTimeText = dt.format(TimeTools.Formatter_DateTime_MS);

                        } else if (_isUI_Show_Date) {

                           dateTimeText = dt.format(TimeTools.Formatter_Date_M);

                        } else if (_isUI_Show_Time) {

                           dateTimeText = dt.format(TimeTools.Formatter_Time_S);
                        }

                        sb.append(TAG_TD + dateTimeText + TAG_TD_END);
                     }
                  }

                  if (_isUI_Show_ItemNumber) {
                     sb.append(TAG_TD + Integer.toString(itemNumber) + TAG_TD_END);
// for debugging
//                     sb.append(TAG_TD + tourId + TAG_TD_END);
//                     sb.append(TAG_TD + markerId + TAG_TD_END);
                  }

                  if (_isUI_Show_LuceneDocId) {
                     sb.append(TAG_TD + String.format("%d", docId) + TAG_TD_END); //$NON-NLS-1$
                  }
               }
               sb.append(TAG_TR_END);
               sb.append(TAG_TABLE_TBODY_END);
               sb.append(TAG_DIV_END);
            }
         }
         sb.append(TAG_TD_END);
      }
      sb.append(TAG_TR_END);
      sb.append(TAG_TABLE_TBODY_END);

      final ItemResponse itemResponse = new ItemResponse();

      itemResponse.createdHtml = sb.toString();
      itemResponse.actionUrl_EditItem = hrefEditItem;

      itemResponse.item_IsMarker = isMarker;
      itemResponse.item_IsTour = isTour;
      itemResponse.item_IsWayPoint = isWayPoint;

      itemResponse.itemId_MarkerId = markerId;
      itemResponse.itemId_TourId = tourId;

      return itemResponse;
   }

   private String createHTML_30_Action(final String actionUrl, final String hoverMessage, final String backgroundImage) {

      // an action is fired with a xhr request to the server

      final String url = " href='#anchor-without-scrolling'" //                  //$NON-NLS-1$
            + " onclick=" //                                                     //$NON-NLS-1$
            + "'" //                                                             //$NON-NLS-1$
            + " tourbook.search.SearchApp.action(\"" + actionUrl + "\");" //     //$NON-NLS-1$ //$NON-NLS-2$
            + " return false;" //                                                //$NON-NLS-1$
            + "'"; //                                                            //$NON-NLS-1$

      return "<a class='action'" //                                              //$NON-NLS-1$
            + " style='background-image: url(" + backgroundImage + ");'" //      //$NON-NLS-1$ //$NON-NLS-2$
            + url
            + " title='" + hoverMessage + "'" //                                 //$NON-NLS-1$ //$NON-NLS-2$
            + ">" //                                                             //$NON-NLS-1$
            + "</a>"; //                                                         //$NON-NLS-1$
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

      writeResponse(httpExchange, response);
   }

   private void writeResponse(final HttpExchange httpExchange, final String response) {

      OutputStream os = null;

      try {

//			response.setContentType("application/json;charset=UTF-8");
         final byte[] convertedResponse = response.getBytes(UI.UTF_8);

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

// SET_FORMATTING_OFF

      responceObj.put(JSON_IS_EASE_SEARCHING,               _isUI_EaseSearching);

      responceObj.put(JSON_IS_SEARCH_ALL,                   _isUI_Search_All);
      responceObj.put(JSON_IS_SEARCH_TOUR,                  _isUI_Search_Tour);
      responceObj.put(JSON_IS_SEARCH_TOUR_LOCATION_START,   _isUI_Search_Tour_LocationStart);
      responceObj.put(JSON_IS_SEARCH_TOUR_LOCATION_END,     _isUI_Search_Tour_LocationEnd);
      responceObj.put(JSON_IS_SEARCH_TOUR_WEATHER,          _isUI_Search_Tour_Weather);
      responceObj.put(JSON_IS_SEARCH_MARKER,                _isUI_Search_Marker);
      responceObj.put(JSON_IS_SEARCH_WAYPOINT,              _isUI_Search_Waypoint);

      responceObj.put(JSON_IS_SHOW_DATE,                    _isUI_Show_Date);
      responceObj.put(JSON_IS_SHOW_TIME,                    _isUI_Show_Time);
      responceObj.put(JSON_IS_SHOW_DESCRIPTION,             _isUI_Show_Description);
      responceObj.put(JSON_IS_SHOW_ITEM_NUMBER,             _isUI_Show_ItemNumber);
      responceObj.put(JSON_IS_SHOW_LUCENE_ID,               _isUI_Show_LuceneDocId);

      responceObj.put(JSON_IS_SORT_BY_DATE_ASCENDING,       _isUI_Sort_DateAscending);

// SET_FORMATTING_ON

      return responceObj.toString();
   }

   private String xhr_GetState(final Map<String, Object> params) {

      final String searchText = Util.getStateString(_state, STATE_CURRENT_SEARCH_TEXT, UI.EMPTY_STRING);

      final JSONObject responceObj = new JSONObject();

      responceObj.put(JSON_STATE_SEARCH_TEXT, searchText);

      return responceObj.toString();
   }

   private void xhr_ItemAction(final Map<String, Object> params) throws UnsupportedEncodingException {

      final Object xhrParameter = params.get(XHR_PARAM_ACTION_URL);

      if (xhrParameter instanceof String) {

         final String xhrAction = URLDecoder.decode((String) xhrParameter, UI.UTF_8);

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

         final String searchText = URLDecoder.decode((String) xhrSearchText, UI.UTF_8);

         proposals = FTSearchManager.getProposals(searchText);
      }

      if (proposals == null || proposals.isEmpty()) {
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

      final String xhrSearchText = (String) params.get(XHR_PARAM_SEARCH_TEXT);
      final String xhrRange = headers.getFirst(REQUEST_HEADER_RANGE);

      int searchPosFrom = 0;
      int searchPosTo = 0;

      if (xhrRange != null) {

         final String[] ranges = xhrRange.substring("items=".length()).split(UI.DASH); //$NON-NLS-1$

         searchPosFrom = Integer.valueOf(ranges[0]);
         searchPosTo = Integer.valueOf(ranges[1]);
      }

      SearchResult searchResult = null;

      final JSONArray allItems = new JSONArray();

      if (xhrSearchText != null) {

         if (WebContentServer.LOG_XHR) {
            log.append("range: " + searchPosFrom + UI.DASH + searchPosTo + UI.TAB + headers.entrySet()); //$NON-NLS-1$
         }

         String searchText = URLDecoder.decode(xhrSearchText, UI.UTF_8);

         // keep search text in state
         _state.put(STATE_CURRENT_SEARCH_TEXT, searchText);

         // ensure white space is removed
         searchText = searchText.trim();

         if (_isUI_EaseSearching

               // ensure that a * is not yet at the end
               && searchText.endsWith(UI.SYMBOL_STAR) == false) {

            // append a * otherwise nothing is found
            searchText += UI.SYMBOL_STAR;
         }

         searchResult = FTSearchManager.searchByPosition(searchText, searchPosFrom, searchPosTo);

         createHTML_10_SearchResults(allItems, searchPosFrom, searchResult);
      }

      /*
       * Set response header
       */

      // this is very important otherwise nothing is displayed
      final String contentRange = createContentRange(searchResult, searchPosFrom, allItems.length());

      final Headers responseHeaders = httpExchange.getResponseHeaders();
      responseHeaders.set(WEB.RESPONSE_HEADER_CONTENT_TYPE, WEB.CONTENT_TYPE_APPLICATION_JSON);
      responseHeaders.set(WEB.RESPONSE_HEADER_CONTENT_RANGE, contentRange);

      /*
       * Create JSON response
       */
      String searchTime;
      final float timeDiff = (float) (System.nanoTime() - start) / 1000000;
      if (timeDiff < 1.0) {
         searchTime = String.format("%.2f ms", timeDiff); //$NON-NLS-1$
      } else if (timeDiff < 10.0) {
         searchTime = String.format("%.1f ms", timeDiff); //$NON-NLS-1$
      } else {
         searchTime = String.format("%.0f ms", timeDiff); //$NON-NLS-1$
      }

      final long totalHits = searchResult == null ? 0 : searchResult.totalHits;

      final JSONObject response = new JSONObject();

      response.put("items", allItems); //$NON-NLS-1$
      response.put("searchTime", searchTime); //$NON-NLS-1$
      response.put("totalHits", totalHits); //$NON-NLS-1$

      return response.toString();
   }

   private void xhr_Select(final Map<String, Object> params) throws UnsupportedEncodingException {

      final Object selectedItems = params.get(XHR_PARAM_SELECTED_ITEMS);

      if (selectedItems != null) {

         final JSONArray jsonSelectedItems = WEB.parseJSONArray(selectedItems);

         Runnable runnable = null;

         if (jsonSelectedItems.length() == 1) {

            // 1 item is selected

            final Object item = jsonSelectedItems.get(0);

            if (item instanceof JSONObject) {

               final JSONObject itemObject = (JSONObject) item;

               final long markerId = itemObject.optLong(JSON_ITEM_ID_MARKER_ID, Long.MIN_VALUE);
               final long tourId = itemObject.optLong(JSON_ITEM_ID_TOUR_ID, Long.MIN_VALUE);

               runnable = new Runnable() {
                  @Override
                  public void run() {

                     if (itemObject.optBoolean(JSON_ITEM_IS_TOUR)) {

                        hrefAction_Tour_Select(new SelectionTourId(tourId));

                     } else if (itemObject.optBoolean(JSON_ITEM_IS_MARKER)) {

                        hrefAction_Marker(SearchManager.ACTION_SELECT_MARKER, tourId, markerId);

                     } else if (itemObject.optBoolean(JSON_ITEM_IS_WAYPOINT)) {

                        hrefAction_WayPoint(tourId, markerId);
                     }
                  }
               };
            }

         } else {

            // multiple items are selected

            // !!! currently only multiple tours are supported !!!

            final ArrayList<Long> tourIds = new ArrayList<>();

            for (final Object item : jsonSelectedItems) {

               if (item instanceof JSONObject) {

                  final JSONObject itemObject = (JSONObject) item;

                  if (itemObject.optBoolean(JSON_ITEM_IS_TOUR)) {

                     final long tourId = itemObject.optLong(JSON_ITEM_ID_TOUR_ID, Long.MIN_VALUE);

                     tourIds.add(tourId);
                  }
               }
            }

            final int numTours = tourIds.size();

            if (numTours > 0) {

               runnable = new Runnable() {
                  @Override
                  public void run() {

                     final ISelection selection = numTours == 1 //

                           // it is possible that only 1 tour and other items are selected
                           ? new SelectionTourId(tourIds.get(0))

                           : new SelectionTourIds(tourIds);

                     hrefAction_Tour_Select(selection);
                  }
               };
            }
         }

         // run in UI thread
         if (runnable != null) {
            Display.getDefault().asyncExec(runnable);
         }
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
      final JSONObject jsonSearchOptions = WEB.parseJSONObject(xhrSearchOptions);

// SET_FORMATTING_OFF

      if (jsonSearchOptions.isNull("isRestoreDefaults") == false) { //$NON-NLS-1$

         // the action 'Restore Default' is selected in the web UI

         saveDefaultSearchOption();
         setInternalSearchOptions();

         // set flag that defaults are returned
         responceObj.put(JSON_IS_SEARCH_OPTIONS_DEFAULT, true);

         // create xhr response with default values
         responceObj.put(JSON_IS_EASE_SEARCHING,               STATE_IS_EASE_SEARCHING_DEFAULT);

         responceObj.put(JSON_IS_SEARCH_ALL,                   STATE_IS_SEARCH_ALL_DEFAULT);
         responceObj.put(JSON_IS_SEARCH_MARKER,                STATE_IS_SEARCH_MARKER_DEFAULT);
         responceObj.put(JSON_IS_SEARCH_TOUR,                  STATE_IS_SEARCH_TOUR_DEFAULT);
         responceObj.put(JSON_IS_SEARCH_TOUR_LOCATION_START,   STATE_IS_SEARCH_TOUR_LOCATION_START_DEFAULT);
         responceObj.put(JSON_IS_SEARCH_TOUR_LOCATION_END,     STATE_IS_SEARCH_TOUR_LOCATION_END_DEFAULT);
         responceObj.put(JSON_IS_SEARCH_TOUR_WEATHER,          STATE_IS_SEARCH_TOUR_WEATHER_DEFAULT);
         responceObj.put(JSON_IS_SEARCH_WAYPOINT,              STATE_IS_SEARCH_WAYPOINT_DEFAULT);

         responceObj.put(JSON_IS_SHOW_DATE,                    STATE_IS_SHOW_DATE_DEFAULT);
         responceObj.put(JSON_IS_SHOW_TIME,                    STATE_IS_SHOW_TIME_DEFAULT);
         responceObj.put(JSON_IS_SHOW_DESCRIPTION,             STATE_IS_SHOW_DESCRIPTION_DEFAULT);
         responceObj.put(JSON_IS_SHOW_ITEM_NUMBER,             STATE_IS_SHOW_ITEM_NUMBER_DEFAULT);
         responceObj.put(JSON_IS_SHOW_LUCENE_ID,               STATE_IS_SHOW_LUCENE_DOC_ID_DEFAULT);

         responceObj.put(JSON_IS_SORT_BY_DATE_ASCENDING,       STATE_IS_SORT_DATE_ASCENDING_DEFAULT);

      } else {

         // update state

         _state.put(STATE_IS_EASE_SEARCHING,                jsonSearchOptions.getBoolean(JSON_IS_EASE_SEARCHING));

         _state.put(STATE_IS_SEARCH_ALL,                    jsonSearchOptions.getBoolean(JSON_IS_SEARCH_ALL));
         _state.put(STATE_IS_SEARCH_MARKER,                 jsonSearchOptions.getBoolean(JSON_IS_SEARCH_MARKER));
         _state.put(STATE_IS_SEARCH_TOUR,                   jsonSearchOptions.getBoolean(JSON_IS_SEARCH_TOUR));
         _state.put(STATE_IS_SEARCH_TOUR_LOCATION_START,    jsonSearchOptions.getBoolean(JSON_IS_SEARCH_TOUR_LOCATION_START));
         _state.put(STATE_IS_SEARCH_TOUR_LOCATION_END,      jsonSearchOptions.getBoolean(JSON_IS_SEARCH_TOUR_LOCATION_END));
         _state.put(STATE_IS_SEARCH_TOUR_WEATHER,           jsonSearchOptions.getBoolean(JSON_IS_SEARCH_TOUR_WEATHER));
         _state.put(STATE_IS_SEARCH_WAYPOINT,               jsonSearchOptions.getBoolean(JSON_IS_SEARCH_WAYPOINT));

         _state.put(STATE_IS_SHOW_DATE,                     jsonSearchOptions.getBoolean(JSON_IS_SHOW_DATE));
         _state.put(STATE_IS_SHOW_TIME,                     jsonSearchOptions.getBoolean(JSON_IS_SHOW_TIME));
         _state.put(STATE_IS_SHOW_DESCRIPTION,              jsonSearchOptions.getBoolean(JSON_IS_SHOW_DESCRIPTION));
         _state.put(STATE_IS_SHOW_ITEM_NUMBER,              jsonSearchOptions.getBoolean(JSON_IS_SHOW_ITEM_NUMBER));
         _state.put(STATE_IS_SHOW_LUCENE_DOC_ID,            jsonSearchOptions.getBoolean(JSON_IS_SHOW_LUCENE_ID));

         _state.put(STATE_IS_SORT_DATE_ASCENDING,           jsonSearchOptions.getBoolean(JSON_IS_SORT_BY_DATE_ASCENDING));

         setInternalSearchOptions();

         // create xhr response
         responceObj.put(JSON_IS_SEARCH_OPTIONS_DEFAULT, false);
      }

// SET_FORMATTING_ON

      return responceObj.toString();
   }

}
