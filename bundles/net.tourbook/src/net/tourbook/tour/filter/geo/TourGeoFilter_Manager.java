/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.filter.geo;

import de.byteholder.geoclipse.map.MapGridData;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.ActionTourGeoFilter;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.filter.SQLFilterData;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class TourGeoFilter_Manager {

   private static final char               NL                                             = UI.NEW_LINE;

   private static final Bundle             _bundle                                        = TourbookPlugin.getDefault().getBundle();

   private final static IPreferenceStore   _prefStore                                     = TourbookPlugin.getPrefStore();
   private static final IDialogSettings    _state                                         = TourbookPlugin.getState("TourGeoFilter"); //$NON-NLS-1$
   private static final IPath              _stateLocation                                 = Platform.getStateLocation(_bundle);

   public static final String              STATE_GRID_BOX_SIZE                            = "STATE_GRID_BOX_SIZE";                    //$NON-NLS-1$
   public static final int                 STATE_GRID_BOX_SIZE_DEFAULT                    = 1;
   static final int                        STATE_GRID_BOX_SIZE_MIN                        = 1;
   static final int                        STATE_GRID_BOX_SIZE_MAX                        = 10;

   static final String                     STATE_SELECTED_GEO_FILTER_ID                   = "STATE_SELECTED_GEO_FILTER_ID";           //$NON-NLS-1$
   static final String                     STATE_SORT_COLUMN_DIRECTION                    = "STATE_SORT_COLUMN_DIRECTION";            //$NON-NLS-1$
   static final String                     STATE_SORT_COLUMN_ID                           = "STATE_SORT_COLUMN_ID";                   //$NON-NLS-1$

   public static final String              STATE_FAST_MAP_PAINTING_SKIPPED_VALUES         = "STATE_FAST_MAP_PAINTING_SKIPPED_VALUES"; //$NON-NLS-1$
   public static final int                 STATE_FAST_MAP_PAINTING_SKIPPED_VALUES_DEFAULT = 10;
   public static final String              STATE_IS_AUTO_OPEN_SLIDEOUT                    = "STATE_IS_AUTO_OPEN_SLIDEOUT";            //$NON-NLS-1$
   public static final boolean             STATE_IS_AUTO_OPEN_SLIDEOUT_DEFAULT            = true;
   public static final String              STATE_IS_FAST_MAP_PAINTING                     = "STATE_IS_FAST_MAP_PAINTING";             //$NON-NLS-1$
   public static final boolean             STATE_IS_FAST_MAP_PAINTING_DEFAULT             = true;
   static final String                     STATE_IS_INCLUDE_GEO_PARTS                     = "STATE_IS_INCLUDE_GEO_PARTS";             //$NON-NLS-1$
   static final boolean                    STATE_IS_INCLUDE_GEO_PARTS_DEFAULT             = true;
   public static final String              STATE_IS_SYNC_MAP_POSITION                     = "STATE_IS_SYNC_MAP_POSITION";             //$NON-NLS-1$
   public static final boolean             STATE_IS_SYNC_MAP_POSITION_DEFAULT             = true;
   public static final String              STATE_IS_USE_APP_FILTERS                       = "STATE_IS_USE_APP_FILTERS";               //$NON-NLS-1$
   public static final boolean             STATE_IS_USE_APP_FILTERS_DEFAULT               = true;

   public static final String              STATE_RGB_GEO_PARTS_HOVER                      = "STATE_RGB_GEO_PARTS_HOVER";              //$NON-NLS-1$
   public static final String              STATE_RGB_GEO_PARTS_SELECTED                   = "STATE_RGB_GEO_PARTS_SELECTED";           //$NON-NLS-1$

   public static final RGB                 STATE_RGB_GEO_PARTS_HOVER_DEFAULT              = new RGB(255, 0, 128);
   public static final RGB                 STATE_RGB_GEO_PARTS_SELECTED_DEFAULT           = new RGB(180, 255, 0);

   private static final String             TOUR_FILTER_FILE_NAME                          = "tour-geo-filter.xml";                    //$NON-NLS-1$
   private static final int                TOUR_FILTER_VERSION                            = 1;

   private static final String             TAG_GEO_FILTER                                 = "GeoFilter";                              //$NON-NLS-1$
   private static final String             TAG_ROOT                                       = "TourGeoFilterItems";                     //$NON-NLS-1$

   private static final String             ATTR_ACTIVE_GEO_FILTER_ID                      = "activeGeoFilterId";                      //$NON-NLS-1$
   private static final String             ATTR_CREATED                                   = "created";                                //$NON-NLS-1$
   private static final String             ATTR_GEO_FILTER_ID                             = "geoFilterId";                            //$NON-NLS-1$
   private static final String             ATTR_MAP_GEO_CENTER_LATITUDE                   = "mapGeoCenterLatitude";                   //$NON-NLS-1$
   private static final String             ATTR_MAP_GEO_CENTER_LONGITUDE                  = "mapGeoCenterLongitude";                  //$NON-NLS-1$
   private static final String             ATTR_MAP_ZOOM_LEVEL                            = "mapZoomLevel";                           //$NON-NLS-1$
   private static final String             ATTR_NUM_GEO_PARTS                             = "numGeoParts";                            //$NON-NLS-1$

   private static final String             ATTR_GEO_LOCATION_TOP_LEFT_X_E2                = "geoLocation_TopLeft_X_E2";               //$NON-NLS-1$
   private static final String             ATTR_GEO_LOCATION_TOP_LEFT_Y_E2                = "geoLocation_TopLeft_Y_E2";               //$NON-NLS-1$
   private static final String             ATTR_GEO_LOCATION_BOTTOM_RIGHT_X_E2            = "geoLocation_BottomRight_X_E2";           //$NON-NLS-1$
   private static final String             ATTR_GEO_LOCATION_BOTTOM_RIGHT_Y_E2            = "geoLocation_BottomRight_Y_E2";           //$NON-NLS-1$

   private static final String             ATTR_GEO_PARTS_TOP_LEFT_X_E2                   = "geoParts_TopLeft_X_E2";                  //$NON-NLS-1$
   private static final String             ATTR_GEO_PARTS_TOP_LEFT_Y_E2                   = "geoParts_TopLeft_Y_E2";                  //$NON-NLS-1$
   private static final String             ATTR_GEO_PARTS_BOTTOM_RIGHT_X_E2               = "geoParts_BottomRight_X_E2";              //$NON-NLS-1$
   private static final String             ATTR_GEO_PARTS_BOTTOM_RIGHT_Y_E2               = "geoParts_BottomRight_Y_E2";              //$NON-NLS-1$

   private static final String             ATTR_TOUR_FILTER_VERSION                       = "tourFilterVersion";                      //$NON-NLS-1$

   private static ActionTourGeoFilter      _actionTourGeoFilter;

   private static boolean                  _isGeoFilterEnabled;

   private static int[]                    _fireEventCounter                              = new int[1];

   private static ArrayList<TourGeoFilter> _allTourGeoFilter                              = new ArrayList<>();
   private static TourGeoFilter            _selectedFilter;

   private static String                   _fromXml_ActiveGeoFilterId;

   /**
    * Create a {@link TourGeoFilter} and set it as the currently selected geo filter.
    *
    * @param geo_TopLeft_E2
    * @param geo_BbottomRight_E2
    * @param map_ZoomLevel
    * @param map_GeoCenter
    * @param mapGridData
    */
   public static void createAndSetGeoFilter(final int map_ZoomLevel,
                                            final GeoPosition map_GeoCenter,
                                            final MapGridData mapGridData) {

      _selectedFilter = new TourGeoFilter(map_ZoomLevel, map_GeoCenter, mapGridData);

      _allTourGeoFilter.add(_selectedFilter);

      // ensure the action is selected
      _actionTourGeoFilter.setSelection(true);

      // show the slideout with the new geo filter
      _actionTourGeoFilter.showSlideout(_selectedFilter);

      // set selection state
      _isGeoFilterEnabled = true;

      fireTourFilterModifyEvent();
   }

   /**
    * @param geoLoaderData
    * @param allLatLonParts
    *           Contains lat/lon geo parts for the geo top/left to bottom/right area
    * @return Return SELECT statement for the lat/lon geo parts or <code>null</code> when geo parts
    *         are not available.
    *         <p>
    *         The returned SELECT contains tour id's which are within the geo parts
    */
   static String createSelectStmtForGeoParts(final Point geoParts_TopLeft_E2,
                                             final Point geoParts_BottomRight_E2,
                                             final ArrayList<Integer> allLatLonParts) {

      //         int latPart = (int) (latitude * 100);
      //         int lonPart = (int) (longitude * 100);
      //
      //         lat      ( -90 ... + 90) * 100 =  -9_000 +  9_000 = 18_000
      //         lon      (-180 ... +180) * 100 = -18_000 + 18_000 = 36_000
      //
      //         max      (9_000 + 9_000) * 100_000 = 18_000 * 100_000  = 1_800_000_000
      //
      //                                    Integer.MAX_VALUE = 2_147_483_647

      // x: longitude
      final int normalizedLon1 = geoParts_TopLeft_E2.x + TourData.NORMALIZED_LONGITUDE_OFFSET_E2;
      final int normalizedLon2 = geoParts_BottomRight_E2.x + TourData.NORMALIZED_LONGITUDE_OFFSET_E2;

      // y: latitude
      final int normalizedLat1 = geoParts_TopLeft_E2.y + TourData.NORMALIZED_LATITUDE_OFFSET_E2;
      final int normalizedLat2 = geoParts_BottomRight_E2.y + TourData.NORMALIZED_LATITUDE_OFFSET_E2;

      final int partWidth = geoParts_BottomRight_E2.x - geoParts_TopLeft_E2.x;
//    final int partHeight = geoTopLeftE2.y - geoBottomRightE2.y;

      String sqlWhere;
      final int gridSize_E2 = 1; // 0.01°

      /**
       * Optimize sql time by using different strategies depending on the number of parts AND part
       * width
       */
      final boolean isSmallWidth = partWidth < 4;
      if (isSmallWidth) {

         /**
          * Use sql: GeoPart In (...)
          */

         for (int normalizedLon = normalizedLon1; normalizedLon < normalizedLon2; normalizedLon += gridSize_E2) {
            for (int normalizedLat = normalizedLat2; normalizedLat < normalizedLat1; normalizedLat += gridSize_E2) {

               // create geo part number from lat/lon

               final int latLonPart = (normalizedLat * 100_000) + normalizedLon;

               allLatLonParts.add(latLonPart);
            }
         }

         final StringBuilder sqlGeoPartParameters = new StringBuilder();

         for (int partIndex = 0; partIndex < allLatLonParts.size(); partIndex++) {
            if (partIndex == 0) {
               sqlGeoPartParameters.append(" ?"); //                             //$NON-NLS-1$
            } else {
               sqlGeoPartParameters.append(", ?"); //                            //$NON-NLS-1$
            }
         }

         sqlWhere = "GeoPart IN (" + sqlGeoPartParameters + ")"; //$NON-NLS-1$ //$NON-NLS-2$

      } else {

         /**
          * Use sql: GeoPart >= latlon1 AND GeoPart < latlon2
          */

         final int normalizedLon2_Last = normalizedLon2 - gridSize_E2;

         for (int normalizedLat = normalizedLat2; normalizedLat < normalizedLat1; normalizedLat += gridSize_E2) {

            final int latLonPart1 = (normalizedLat * 100_000) + normalizedLon1;
            final int latLonPart2 = (normalizedLat * 100_000) + normalizedLon2_Last;

            allLatLonParts.add(latLonPart1);
            allLatLonParts.add(latLonPart2);
         }

         final StringBuilder sb = new StringBuilder();

         for (int partIndex = 0; partIndex < allLatLonParts.size(); partIndex += 2) {

            if (partIndex > 0) {

               sb.append(" OR "); //$NON-NLS-1$
            }

            sb.append(" (GeoPart >= ? AND GeoPart <= ?) " + NL); //$NON-NLS-1$
         }

         sqlWhere = sb.toString();
      }

      if (allLatLonParts.size() == 0) {

         // prevent invalid sql
         return null;
      }

      final String sqlSelectWithAllTourIdsFromGeoParts = "" //$NON-NLS-1$

            + "SELECT" + NL //                                                //$NON-NLS-1$

            + " DISTINCT TourId " + NL //                                     //$NON-NLS-1$

            + (" FROM " + TourDatabase.TABLE_TOUR_GEO_PARTS + NL) //          //$NON-NLS-1$
            + (" WHERE " + sqlWhere) + NL //    //$NON-NLS-1$
      ;

      return sqlSelectWithAllTourIdsFromGeoParts;
   }

   /**
    * Fire event that the tour filter has changed.
    */
   static void fireTourFilterModifyEvent() {

      _fireEventCounter[0]++;

      Display.getDefault().asyncExec(new Runnable() {

         final int __runnableCounter = _fireEventCounter[0];

         @Override
         public void run() {

            // skip all events which has not yet been executed
            if (__runnableCounter != _fireEventCounter[0]) {

               // a new event occured
               return;
            }

            _prefStore.setValue(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED, Math.random());
         }
      });

   }

   static ArrayList<TourGeoFilter> getAllGeoFilter() {
      return _allTourGeoFilter;
   }

   /**
    * @return Returns current active geo filter or <code>null</code> when a filter is not selected
    */
   private static TourGeoFilter getGeoFilter() {

      if (_selectedFilter != null) {
         return _selectedFilter;
      }

      if (_fromXml_ActiveGeoFilterId == null) {
         return null;
      }

      for (final TourGeoFilter tourGeoFilterItem : _allTourGeoFilter) {

         if (_fromXml_ActiveGeoFilterId.equals(tourGeoFilterItem.id)) {

            _selectedFilter = tourGeoFilterItem;

            return tourGeoFilterItem;
         }
      }

      return null;
   }

   /**
    * @return Returns sql data for the selected tour filter profile or <code>null</code> when not
    *         available.
    */
   public static SQLFilterData getSQL() {

      if (_isGeoFilterEnabled == false) {

         // tour filter is not enabled

         return null;
      }

      final TourGeoFilter geoFilter = getGeoFilter();

      if (geoFilter == null) {

         // tour filter is not selected

         return null;
      }

      final boolean isIncludeGeoParts = Util.getStateBoolean(_state,
            TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS,
            TourGeoFilter_Manager.STATE_IS_INCLUDE_GEO_PARTS_DEFAULT);

      final ArrayList<Object> sqlParameters = new ArrayList<>();

      final ArrayList<Integer> allLatLonParts = new ArrayList<>();

      final String sqlSelect_WithAllTourIds_FromGeoParts = createSelectStmtForGeoParts(
            geoFilter.geoParts_TopLeft_E2,
            geoFilter.geoParts_BottomRight_E2,
            allLatLonParts);

      if (sqlSelect_WithAllTourIds_FromGeoParts == null) {

         // this can occure when there are no geo parts, this would cause a sql exception

         return null;
      }

      sqlParameters.addAll(allLatLonParts);

      // include or exclude geo parts
      final String sqlIncludeExcludeGeoParts = isIncludeGeoParts ? UI.EMPTY_STRING : "NOT"; //$NON-NLS-1$

      final String sqlWhere = "" //$NON-NLS-1$

            + " AND HasGeoData" + NL //$NON-NLS-1$
            + " AND TourId " + sqlIncludeExcludeGeoParts + " IN (" + sqlSelect_WithAllTourIds_FromGeoParts + ") "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      final SQLFilterData tourFilterSQLData = new SQLFilterData(sqlWhere, sqlParameters);

      return tourFilterSQLData;
   }

   public static IDialogSettings getState() {
      return _state;
   }

   private static File getXmlFile() {

      final File layerFile = _stateLocation.append(TOUR_FILTER_FILE_NAME).toFile();

      return layerFile;
   }

   public static void restoreState() {

      _isGeoFilterEnabled = _prefStore.getBoolean(ITourbookPreferences.APP_TOUR_GEO_FILTER_IS_SELECTED);

      _actionTourGeoFilter.setSelection(_isGeoFilterEnabled);

      xmlReadGeoFilter();
   }

   public static void saveState() {

      _prefStore.setValue(ITourbookPreferences.APP_TOUR_GEO_FILTER_IS_SELECTED, _actionTourGeoFilter.getSelection());

      final XMLMemento xmlRoot = xmlWriteGeoFilter();
      final File xmlFile = getXmlFile();

      Util.writeXml(xmlRoot, xmlFile);
   }

   /**
    * Filter is selected in the geo filter slideout
    *
    * @param selectedFilter
    */
   public static void selectFilter(final TourGeoFilter selectedFilter) {

      if (_selectedFilter == selectedFilter) {

         // prevent reselecting

         return;
      }

      _selectedFilter = selectedFilter;

      fireTourFilterModifyEvent();
   }

   public static void setAction_TourGeoFilter(final ActionTourGeoFilter actionTourGeoFilter) {

      _actionTourGeoFilter = actionTourGeoFilter;
   }

   public static void setAndOpenGeoFilterSlideout(final boolean isOpen) {

      _actionTourGeoFilter.setAndOpenGeoFilterSlideout(isOpen);
   }

   /**
    * Sets the state if the tour filter is active or not.
    *
    * @param isEnabled
    */
   public static void setFilterEnabled(final boolean isEnabled) {

      _isGeoFilterEnabled = isEnabled;

      // show/hide geo grid in map
      TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_GEO_GRID,
            isEnabled
                  ? getGeoFilter()
                  : null,
            null);

      fireTourFilterModifyEvent();
   }

   /**
    * Read filter profile xml file.
    *
    * @return
    */
   private static void xmlReadGeoFilter() {

      final File xmlFile = getXmlFile();

      if (xmlFile.exists()) {

         try (InputStreamReader reader = new InputStreamReader(new FileInputStream(xmlFile), UI.UTF_8)) {

            final XMLMemento xmlRoot = XMLMemento.createReadRoot(reader);

            _fromXml_ActiveGeoFilterId = Util.getXmlString(xmlRoot, ATTR_ACTIVE_GEO_FILTER_ID, null);

            for (final IMemento mementoChild : xmlRoot.getChildren()) {

               final XMLMemento xmlGeoFilter = (XMLMemento) mementoChild;
               if (TAG_GEO_FILTER.equals(xmlGeoFilter.getType())) {

                  final TourGeoFilter geoFilter = new TourGeoFilter();

                  // id
                  final String filterId = Util.getXmlString(xmlGeoFilter, ATTR_GEO_FILTER_ID, null);
                  if (filterId != null) {
                     geoFilter.id = filterId;
                  }

                  geoFilter.created = Util.getXmlDateTime(xmlGeoFilter, ATTR_CREATED, TimeTools.now());
                  geoFilter.createdMS = TimeTools.toEpochMilli(geoFilter.created);

                  geoFilter.numGeoParts = Util.getXmlInteger(xmlGeoFilter, ATTR_NUM_GEO_PARTS, 0);

                  geoFilter.geoLocation_TopLeft_E2 = new Point(
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_LOCATION_TOP_LEFT_X_E2, 0),
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_LOCATION_TOP_LEFT_Y_E2, 0));

                  geoFilter.geoLocation_BottomRight_E2 = new Point(
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_LOCATION_BOTTOM_RIGHT_X_E2, 0),
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_LOCATION_BOTTOM_RIGHT_Y_E2, 0));

                  geoFilter.geoParts_TopLeft_E2 = new Point(
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_PARTS_TOP_LEFT_X_E2, 0),
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_PARTS_TOP_LEFT_Y_E2, 0));

                  geoFilter.geoParts_BottomRight_E2 = new Point(
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_PARTS_BOTTOM_RIGHT_X_E2, 0),
                        Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_PARTS_BOTTOM_RIGHT_Y_E2, 0));

                  // x:long  y:lat
                  geoFilter.geoLocation_TopLeft = new GeoPosition(
                        geoFilter.geoLocation_TopLeft_E2.y / 100.0d,
                        geoFilter.geoLocation_TopLeft_E2.x / 100.0d);
                  geoFilter.geoLocation_BottomRight = new GeoPosition(
                        geoFilter.geoLocation_BottomRight_E2.y / 100.0d,
                        geoFilter.geoLocation_BottomRight_E2.x / 100.0d);

                  geoFilter.geoParts_TopLeft = new GeoPosition(
                        geoFilter.geoParts_TopLeft_E2.y / 100.0d,
                        geoFilter.geoParts_TopLeft_E2.x / 100.0d);
                  geoFilter.geoParts_BottomRight = new GeoPosition(
                        geoFilter.geoParts_BottomRight_E2.y / 100.0d,
                        geoFilter.geoParts_BottomRight_E2.x / 100.0d);

                  geoFilter.mapZoomLevel = Util.getXmlInteger(xmlGeoFilter, ATTR_MAP_ZOOM_LEVEL, 6);
                  geoFilter.mapGeoCenter = new GeoPosition(
                        Util.getXmlDouble(xmlGeoFilter, ATTR_MAP_GEO_CENTER_LATITUDE, 0),
                        Util.getXmlDouble(xmlGeoFilter, ATTR_MAP_GEO_CENTER_LONGITUDE, 0));

                  _allTourGeoFilter.add(geoFilter);
               }
            }

         } catch (final Exception e) {
            StatusUtil.log(e);
         }
      }
   }

   /**
    * @return
    */
   private static XMLMemento xmlWriteGeoFilter() {

      XMLMemento xmlRoot = null;

      try {

         xmlRoot = xmlWriteGeoFilter_10_Root();

// This is currently disabled because loaded filters do not yet work

         if (_selectedFilter != null) {
            xmlRoot.putString(ATTR_ACTIVE_GEO_FILTER_ID, _selectedFilter.id);
         }

         // loop: all geo filter
         for (final TourGeoFilter geoFilter : _allTourGeoFilter) {

            final IMemento xmlFilter = xmlRoot.createChild(TAG_GEO_FILTER);

            xmlFilter.putString(ATTR_GEO_FILTER_ID, geoFilter.id);

            xmlFilter.putString(ATTR_CREATED, geoFilter.created.toString());
            xmlFilter.putInteger(ATTR_NUM_GEO_PARTS, geoFilter.numGeoParts);

            xmlFilter.putInteger(ATTR_GEO_LOCATION_TOP_LEFT_X_E2, geoFilter.geoLocation_TopLeft_E2.x);
            xmlFilter.putInteger(ATTR_GEO_LOCATION_TOP_LEFT_Y_E2, geoFilter.geoLocation_TopLeft_E2.y);
            xmlFilter.putInteger(ATTR_GEO_LOCATION_BOTTOM_RIGHT_X_E2, geoFilter.geoLocation_BottomRight_E2.x);
            xmlFilter.putInteger(ATTR_GEO_LOCATION_BOTTOM_RIGHT_Y_E2, geoFilter.geoLocation_BottomRight_E2.y);

            xmlFilter.putInteger(ATTR_GEO_PARTS_TOP_LEFT_X_E2, geoFilter.geoParts_TopLeft_E2.x);
            xmlFilter.putInteger(ATTR_GEO_PARTS_TOP_LEFT_Y_E2, geoFilter.geoParts_TopLeft_E2.y);
            xmlFilter.putInteger(ATTR_GEO_PARTS_BOTTOM_RIGHT_X_E2, geoFilter.geoParts_BottomRight_E2.x);
            xmlFilter.putInteger(ATTR_GEO_PARTS_BOTTOM_RIGHT_Y_E2, geoFilter.geoParts_BottomRight_E2.y);

            xmlFilter.putInteger(ATTR_MAP_ZOOM_LEVEL, geoFilter.mapZoomLevel);
            Util.setXmlDouble(xmlFilter, ATTR_MAP_GEO_CENTER_LATITUDE, geoFilter.mapGeoCenter.latitude);
            Util.setXmlDouble(xmlFilter, ATTR_MAP_GEO_CENTER_LONGITUDE, geoFilter.mapGeoCenter.longitude);
         }

      } catch (final Exception e) {
         StatusUtil.log(e);
      }

      return xmlRoot;
   }

   private static XMLMemento xmlWriteGeoFilter_10_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // layer structure version
      xmlRoot.putInteger(ATTR_TOUR_FILTER_VERSION, TOUR_FILTER_VERSION);

      return xmlRoot;
   }

}
