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

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

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

public class TourGeoFilterManager {

   private static final Bundle                 _bundle                       = TourbookPlugin.getDefault().getBundle();

   private static final IPath                  _stateLocation                = Platform.getStateLocation(_bundle);
   private final static IPreferenceStore       _prefStore                    = TourbookPlugin.getPrefStore();

   private static final String                 TOUR_FILTER_FILE_NAME         = "tour-geo-filter.xml";                  //$NON-NLS-1$
   private static final int                    TOUR_FILTER_VERSION           = 1;

   private static final String                 TAG_GEO_FILTER                = "GeoFilter";                            //$NON-NLS-1$
   private static final String                 TAG_ROOT                      = "TourGeoFilterItems";                   //$NON-NLS-1$

   private static final String                 ATTR_CREATED                  = "created";                              //$NON-NLS-1$
   private static final String                 ATTR_GEO_PARTS                = "geoParts";                             //$NON-NLS-1$
   private static final String                 ATTR_LATITUDE_1               = "latitude1";                            //$NON-NLS-1$;
   private static final String                 ATTR_LONGITUDE_1              = "longitude1";                           //$NON-NLS-1$;;
   private static final String                 ATTR_LATITUDE_2               = "latitude2";                            //$NON-NLS-1$;;
   private static final String                 ATTR_LONGITUDE_2              = "longitude2";                           //$NON-NLS-1$;;
   private static final String                 ATTR_MAP_GEO_CENTER_LATITUDE  = "mapGeoCenterLatitude";                 //$NON-NLS-1$
   private static final String                 ATTR_MAP_GEO_CENTER_LONGITUDE = "mapGeoCenterLongitude";                //$NON-NLS-1$
   private static final String                 ATTR_MAP_ZOOM_LEVEL           = "mapZoomLevel";                         //$NON-NLS-1$

   private static final String                 ATTR_TOUR_FILTER_VERSION      = "tourFilterVersion";                    //$NON-NLS-1$

   private static ActionTourGeoFilter          _actionTourGeoFilter;

   private static boolean                      _isGeoFilterEnabled;

   private static int[]                        _fireEventCounter             = new int[1];

   private static ArrayList<TourGeoFilterItem> _allTourGeoFilter             = new ArrayList<>();
   private static TourGeoFilterItem            _selectedFilter;

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

   static ArrayList<TourGeoFilterItem> getAllGeoFilter() {
      return _allTourGeoFilter;
   }

   /**
    * @return Returns sql data for the selected tour filter profile or <code>null</code> when not
    *         available.
    */
   public static SQLFilterData getSQL() {

      if (_isGeoFilterEnabled == false || _selectedFilter == null) {

         // tour filter is not enabled or not selected

         return null;
      }

      final StringBuilder sqlWhere = new StringBuilder();
      final ArrayList<Object> sqlParameters = new ArrayList<>();

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
      // y: latitude

      final int normalizedLat1 = _selectedFilter.topLeftE2.y + TourData.NORMALIZED_LATITUDE_OFFSET_E2;
      final int normalizedLat2 = _selectedFilter.bottomRightE2.y + TourData.NORMALIZED_LATITUDE_OFFSET_E2;

      final int normalizedLon1 = _selectedFilter.topLeftE2.x + TourData.NORMALIZED_LONGITUDE_OFFSET_E2;
      final int normalizedLon2 = _selectedFilter.bottomRightE2.x + TourData.NORMALIZED_LONGITUDE_OFFSET_E2;

      final double gridSize_E2 = 1; // 0.01°

      for (int normalizedLon = normalizedLon1; normalizedLon < normalizedLon2; normalizedLon += gridSize_E2) {

         for (int normalizedLat = normalizedLat2; normalizedLat < normalizedLat1; normalizedLat += gridSize_E2) {

            final int latitudeE2 = normalizedLat - TourData.NORMALIZED_LATITUDE_OFFSET_E2;
            final int longitudeE2 = normalizedLon - TourData.NORMALIZED_LONGITUDE_OFFSET_E2;

            final int latLonPart = (latitudeE2 + 9_000) * 100_000 + (longitudeE2 + 18_000);

            sqlParameters.add(latLonPart);

//            System.out.println(String.format("lon(x) %d  lat(y) %d  %s",
//
//                  longitudeE2,
//                  latitudeE2,
//
//                  Integer.toString(latLonPart)
//
//            ));
//// TODO remove SYSTEM.OUT.PRINTLN
         }
      }

      final int numGeoParts = sqlParameters.size();

      /*
       * Create sql parameters
       */
      final StringBuilder sqlInParameters = new StringBuilder();
      for (int partIndex = 0; partIndex < numGeoParts; partIndex++) {
         if (partIndex == 0) {
            sqlInParameters.append(" ?"); //$NON-NLS-1$
         } else {
            sqlInParameters.append(", ?"); //$NON-NLS-1$
         }
      }

      final char NL = UI.NEW_LINE;

      final String selectGeoPart = "SELECT" + NL //                       //$NON-NLS-1$

            + " DISTINCT TourId " + NL //                           //$NON-NLS-1$

            + (" FROM " + TourDatabase.TABLE_TOUR_GEO_PARTS + NL) //      //$NON-NLS-1$
            + (" WHERE GeoPart IN (" + sqlInParameters + ")") + NL //      //$NON-NLS-1$ //$NON-NLS-2$
      ;

      sqlWhere.append(" AND TourId IN (" + selectGeoPart + ") ");

      final SQLFilterData tourFilterSQLData = new SQLFilterData(sqlWhere.toString(), sqlParameters);

      return tourFilterSQLData;
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

   public static void setAction_TourGeoFilter(final ActionTourGeoFilter actionTourGeoFilter) {

      _actionTourGeoFilter = actionTourGeoFilter;
   }

   public static void setFilter(final Point topLeftE2, final Point bottomRightE2, final int mapZoomLevel, final GeoPosition mapGeoCenter) {

      _selectedFilter = new TourGeoFilterItem(topLeftE2, bottomRightE2, mapZoomLevel, mapGeoCenter);

      _allTourGeoFilter.add(_selectedFilter);

      // ensure the action is selected
      _actionTourGeoFilter.setSelection(true);

      // show the slideout with the new geo filter
      _actionTourGeoFilter.showSlideout();

      // set selection state
      _isGeoFilterEnabled = true;

      fireTourFilterModifyEvent();
   }

   /**
    * Sets the state if the tour filter is active or not.
    *
    * @param isEnabled
    */
   public static void setFilterEnabled(final boolean isEnabled) {

      _isGeoFilterEnabled = isEnabled;

      // show/hide geo grid in map
      TourManager.fireEventWithCustomData(TourEventId.MAP_SHOW_LAST_GEO_GRID, _isGeoFilterEnabled, null);

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
            for (final IMemento mementoChild : xmlRoot.getChildren()) {

               final XMLMemento xmlGeoFilter = (XMLMemento) mementoChild;
               if (TAG_GEO_FILTER.equals(xmlGeoFilter.getType())) {

                  final TourGeoFilterItem geoFilter = new TourGeoFilterItem();

                  geoFilter.created = Util.getXmlDateTime(xmlGeoFilter, ATTR_CREATED, TimeTools.now());
                  geoFilter.createdMS = TimeTools.toEpochMilli(geoFilter.created);

                  geoFilter.numGeoParts = Util.getXmlInteger(xmlGeoFilter, ATTR_GEO_PARTS, 0);

                  geoFilter.latitude1 = Util.getXmlDouble(xmlGeoFilter, ATTR_LATITUDE_1, 0);
                  geoFilter.longitude1 = Util.getXmlDouble(xmlGeoFilter, ATTR_LONGITUDE_1, 0);
                  geoFilter.latitude2 = Util.getXmlDouble(xmlGeoFilter, ATTR_LATITUDE_2, 0);
                  geoFilter.longitude2 = Util.getXmlDouble(xmlGeoFilter, ATTR_LONGITUDE_2, 0);

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

         // loop: all geo filter
         for (final TourGeoFilterItem geoFilter : _allTourGeoFilter) {

            final IMemento xmlFilter = xmlRoot.createChild(TAG_GEO_FILTER);

            xmlFilter.putString(ATTR_CREATED, geoFilter.created.toString());
            xmlFilter.putInteger(ATTR_GEO_PARTS, geoFilter.numGeoParts);

            xmlFilter.putFloat(ATTR_LATITUDE_1, (float) geoFilter.latitude1);
            xmlFilter.putFloat(ATTR_LONGITUDE_1, (float) geoFilter.longitude1);
            xmlFilter.putFloat(ATTR_LATITUDE_2, (float) geoFilter.latitude2);
            xmlFilter.putFloat(ATTR_LONGITUDE_2, (float) geoFilter.longitude2);

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
