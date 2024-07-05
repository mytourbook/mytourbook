/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.tour.filter.TourFilterFieldOperator;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class Map2ConfigManager {

   private static final String CONFIG_FILE_NAME     = "map2-config.xml";                      //$NON-NLS-1$
   //
   /**
    * Version number is not yet used.
    */
   private static final int    CONFIG_VERSION       = 1;

   private static final Bundle _bundle              = TourbookPlugin.getDefault().getBundle();
   private static final IPath  _stateLocation       = Platform.getStateLocation(_bundle);

   public static final String  CONFIG_DEFAULT_ID_1  = "#1";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_2  = "#2";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_3  = "#3";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_4  = "#4";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_5  = "#5";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_6  = "#6";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_7  = "#7";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_8  = "#8";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_9  = "#9";                                   //$NON-NLS-1$
   static final String         CONFIG_DEFAULT_ID_10 = "#10";                                  //$NON-NLS-1$
   //
   // common attributes
   private static final String ATTR_ACTIVE_CONFIG_ID = "activeConfigId"; //$NON-NLS-1$

   private static final String ATTR_ID               = "id";             //$NON-NLS-1$
   private static final String ATTR_CONFIG_NAME      = "name";           //$NON-NLS-1$
   //
   /*
    * Root
    */
   private static final String TAG_ROOT            = "Map2Configuration"; //$NON-NLS-1$
   private static final String ATTR_CONFIG_VERSION = "configVersion";     //$NON-NLS-1$
   //
   /*
    * Tour options
    */
   private static final String TAG_OPTIONS = "Options"; //$NON-NLS-1$
   //
   /*
    * Map points
    */
   private static final String TAG_MAP_POINTS                        = "MapPoints";                           //$NON-NLS-1$
   private static final String TAG_MAP_POINT                         = "MapPoint";                            //$NON-NLS-1$
   // common
   private static final String ATTR_IS_LABEL_ANTIALIASED             = "isLabelAntialiased";                  //$NON-NLS-1$
   private static final String ATTR_IS_SYMBOL_ANTIALIASED            = "isSymbolAntialiased";                 //$NON-NLS-1$
   // all labels
   private static final String ATTR_IS_TRUNCATE_LABEL                = "isTruncateLabel";                     //$NON-NLS-1$
   private static final String ATTR_IS_WRAP_LABEL                    = "isWrapLabel";                         //$NON-NLS-1$
   private static final String ATTR_LABEL_LAYOUT                     = "labelLayout";                         //$NON-NLS-1$
   private static final String ATTR_LABEL_DISTRIBUTOR_MAX_LABELS     = "labelDistributorMaxLabels";           //$NON-NLS-1$
   private static final String ATTR_LABEL_DISTRIBUTOR_RADIUS         = "labelDistributorRadius";              //$NON-NLS-1$
   private static final String ATTR_LABEL_TRUNCATE_LENGTH            = "labelTruncateLength";                 //$NON-NLS-1$
   private static final String ATTR_LABEL_WRAP_LENGTH                = "labelWrapLength";                     //$NON-NLS-1$
   // tour + common location
   private static final String ATTR_IS_SHOW_COMMON_LOCATION          = "isShowCommonLocation";                //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_LOCATION_BOUNDING_BOX    = "isShowLocationBoundingBox";           //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_TOUR_LOCATION            = "isShowTourLocation";                  //$NON-NLS-1$
   //
   private static final String TAG_COMMON_LOCATION_FILL_COLOR        = "CommonLocationFillColor";             //$NON-NLS-1$
   private static final String TAG_COMMON_LOCATION_OUTLINE_COLOR     = "CommonLocationOutlineColor";          //$NON-NLS-1$
   private static final String TAG_TOUR_LOCATION_FILL_COLOR          = "TourLocationFillColor";               //$NON-NLS-1$
   private static final String TAG_TOUR_LOCATION_OUTLINE_COLOR       = "TourLocationOutlineColor";            //$NON-NLS-1$
   private static final String TAG_TOUR_LOCATION_START_FILL_COLOR    = "TourLocation_StartFillColor";         //$NON-NLS-1$
   private static final String TAG_TOUR_LOCATION_START_OUTLINE_COLOR = "TourLocation_StartOutlineColor";      //$NON-NLS-1$
   private static final String TAG_TOUR_LOCATION_END_FILL_COLOR      = "TourLocation_EndFillColor";           //$NON-NLS-1$
   private static final String TAG_TOUR_LOCATION_END_OUTLINE_COLOR   = "TourLocation_EndOutlineColor";        //$NON-NLS-1$
   // tour marker
   private static final String ATTR_IS_SHOW_TOUR_MARKER              = "isShowTourMarker";                    //$NON-NLS-1$
   private static final String ATTR_IS_GROUP_DUPLICATED_MARKERS      = "isGroupDuplicatedMarkers";            //$NON-NLS-1$
   private static final String ATTR_GROUPED_MARKERS                  = "groupedLabels";                       //$NON-NLS-1$
   private static final String ATTR_GROUP_GRID_SIZE                  = "groupGridSize";                       //$NON-NLS-1$
   //
   private static final String TAG_TOUR_MARKER_FILL_COLOR            = "TourMarkerFillColor";                 //$NON-NLS-1$
   private static final String TAG_TOUR_MARKER_OUTLINE_COLOR         = "TourMarkerOutlineColor";              //$NON-NLS-1$
   // tour marker cluster
   private static final String ATTR_IS_TOUR_MARKER_CLUSTERED         = "isTourMarkerClustered";               //$NON-NLS-1$
   private static final String ATTR_IS_CLUSTER_FILL_OPACITY          = "isClusterFillOpacity";                //$NON-NLS-1$
   //
   private static final String ATTR_CLUSTER_GRID_SIZE                = "clusterGridSize";                     //$NON-NLS-1$
   private static final String ATTR_CLUSTER_OUTLINE_WIDTH            = "clusterOutlineWidth";                 //$NON-NLS-1$
   private static final String ATTR_CLUSTER_SYMBOL_SIZE              = "clusterSymbolSize";                   //$NON-NLS-1$
   //
   private static final String TAG_CLUSTER_FILL_COLOR                = "ClusterFillColor";                    //$NON-NLS-1$
   private static final String TAG_CLUSTER_OUTLINE_COLOR             = "ClusterOutlineColor";                 //$NON-NLS-1$
   // tour pauses
   private static final String ATTR_IS_SHOW_TOUR_PAUSES              = "isShowTourPauses";                    //$NON-NLS-1$
   private static final String ATTR_IS_FILTER_TOUR_PAUSES            = "isFilterTourPauses";                  //$NON-NLS-1$
   private static final String ATTR_IS_FILTER_PAUSE_DURATION         = "isFilterPauseDuration";               //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_AUTO_PAUSES              = "isShowAutoPauses";                    //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_USER_PAUSES              = "isShowUserPauses";                    //$NON-NLS-1$
   private static final String ATTR_DURATION_FILTER_HOURS            = "tourPause_DurationFilter_Hours";      //$NON-NLS-1$
   private static final String ATTR_DURATION_FILTER_MINUTES          = "tourPause_DurationFilter_Minutes";    //$NON-NLS-1$
   private static final String ATTR_DURATION_FILTER_SECONDS          = "tourPause_DurationFilter_Seconds";    //$NON-NLS-1$
   private static final String ATTR_DURATION_FILTER_SUMMARIZED       = "tourPause_DurationFilter_Summarized"; //$NON-NLS-1$
   private static final String ATTR_DURATION_OPERATOR                = "tourPause_DurationFilter_Operator";   //$NON-NLS-1$
   private static final String ATTR_USE_DURATION_FILTER_HOURS        = "useTourPause_DurationFilter_Hours";   //$NON-NLS-1$
   private static final String ATTR_USE_DURATION_FILTER_MINUTES      = "useTourPause_DurationFilter_Minutes"; //$NON-NLS-1$
   private static final String ATTR_USE_DURATION_FILTER_SECONDS      = "useTourPause_DurationFilter_Seconds"; //$NON-NLS-1$
   //
   private static final String TAG_TOUR_PAUSE_FILL_COLOR             = "TourPauseFillColor";                  //$NON-NLS-1$
   private static final String TAG_TOUR_PAUSE_OUTLINE_COLOR          = "TourPauseOutlineColor";               //$NON-NLS-1$
   //
   /*
    * Defaults, min/max
    */
   // marker
   static final int                            LABEL_GROUP_GRID_SIZE_MIN               = 50;
   static final int                            LABEL_GROUP_GRID_SIZE_MAX               = 1000;
   static final int                            LABEL_GROUP_GRID_SIZE_DEFAULT           = 300;
   public static final MapLabelLayout          LABEL_LAYOUT_DEFAULT                    = MapLabelLayout.RECTANGLE_BOX;
   static final int                            LABEL_DISTRIBUTOR_MAX_LABELS_MIN        = 10;
   static final int                            LABEL_DISTRIBUTOR_MAX_LABELS_MAX        = 2000;
   static final int                            LABEL_DISTRIBUTOR_MAX_LABELS_DEFAULT    = 200;
   static final int                            LABEL_DISTRIBUTOR_RADIUS_MIN            = 10;
   static final int                            LABEL_DISTRIBUTOR_RADIUS_MAX            = 2000;
   static final int                            LABEL_DISTRIBUTOR_RADIUS_DEFAULT        = 100;
   static final int                            LABEL_TRUNCATE_LENGTH_MIN               = 0;
   static final int                            LABEL_TRUNCATE_LENGTH_MAX               = 1000;
   static final int                            LABEL_TRUNCATE_LENGTH_DEFAULT           = 40;
   static final int                            LABEL_WRAP_LENGTH_MIN                   = 1;
   static final int                            LABEL_WRAP_LENGTH_MAX                   = 1000;
   static final int                            LABEL_WRAP_LENGTH_DEFAULT               = 40;
   // cluster
   public static final boolean                 DEFAULT_IS_FILL_CLUSTER_SYMBOL          = true;
   public static final int                     DEFAULT_CLUSTER_GRID_SIZE               = 50;
   public static final int                     DEFAULT_CLUSTER_SYMBOL_SIZE             = 8;
   public static final int                     DEFAULT_CLUSTER_OUTLINE_WIDTH           = 1;
   public static final int                     CLUSTER_GRID_SIZE_MIN                   = 1;
   public static final int                     CLUSTER_GRID_SIZE_MAX                   = 10000;
   public static final int                     CLUSTER_OUTLINE_WIDTH_MIN               = 0;
   public static final int                     CLUSTER_OUTLINE_WIDTH_MAX               = 10;
   public static final int                     CLUSTER_SYMBOL_SIZE_MIN                 = 5;
   public static final int                     CLUSTER_SYMBOL_SIZE_MAX                 = 200;
   // colors
   public static final RGB                     DEFAULT_CLUSTER_FILL_RGB                = new RGB(0, 160, 237);
   public static final RGB                     DEFAULT_CLUSTER_OUTLINE_RGB             = new RGB(255, 255, 255);
   public static final RGB                     DEFAULT_COMMON_LOCATION_FILL_RGB        = new RGB(170, 213, 255);
   public static final RGB                     DEFAULT_COMMON_LOCATION_OUTLINE_RGB     = new RGB(0, 0, 0);
   public static final RGB                     DEFAULT_TOUR_LOCATION_FILL_RGB          = new RGB(145, 255, 194);
   public static final RGB                     DEFAULT_TOUR_LOCATION_OUTLINE_RGB       = new RGB(0, 0, 0);
   public static final RGB                     DEFAULT_TOUR_LOCATION_START_FILL_RGB    = new RGB(255, 168, 170);
   public static final RGB                     DEFAULT_TOUR_LOCATION_START_OUTLINE_RGB = new RGB(0, 0, 0);
   public static final RGB                     DEFAULT_TOUR_LOCATION_END_FILL_RGB      = new RGB(255, 252, 145);
   public static final RGB                     DEFAULT_TOUR_LOCATION_END_OUTLINE_RGB   = new RGB(0, 0, 0);
   public static final RGB                     DEFAULT_TOUR_MARKER_FILL_RGB            = new RGB(210, 255, 74);
   public static final RGB                     DEFAULT_TOUR_MARKER_OUTLINE_RGB         = new RGB(0, 0, 0);
   public static final RGB                     DEFAULT_TOUR_PAUSE_FILL_RGB             = new RGB(255, 211, 130);
   public static final RGB                     DEFAULT_TOUR_PAUSE_OUTLINE_RGB          = new RGB(0, 0, 0);
   //
   public static final boolean                 IS_FILTER_TOUR_PAUSES_DEFAULT           = false;
   public static final boolean                 IS_FILTER_PAUSE_DURATION_DEFAULT        = false;
   public static final boolean                 IS_SHOW_AUTO_PAUSES_DEFAULT             = true;
   public static final boolean                 IS_SHOW_USER_PAUSES_DEFAULT             = true;
   public static final int                     DURATION_FILTER_HOURS_DEFAULT           = 1;
   public static final int                     DURATION_FILTER_MINUTES_DEFAULT         = 1;
   public static final int                     DURATION_FILTER_SECONDS_DEFAULT         = 5;
   public static final TourFilterFieldOperator DURATION_OPERATOR_DEFAULT               = TourFilterFieldOperator.LESS_THAN_OR_EQUAL;
   public static final boolean                 USE_DURATION_FILTER_HOURS_DEFAULT       = false;
   public static final boolean                 USE_DURATION_FILTER_MINUTES_DEFAULT     = false;
   public static final boolean                 USE_DURATION_FILTER_SECONDS_DEFAULT     = true;
   //
   /**
    * Contains all configurations which are loaded from a xml file.
    */
   private static final ArrayList<Map2Config>  _allMapPointConfigs                     = new ArrayList<>();
   private static Map2Config                   _activeMapPointConfig;
   //
   private static String                       _fromXml_ActiveMapPointConfigId;

   //
   private static XMLMemento create_Root() {

      final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

      // date/time
      xmlRoot.putString(Util.ATTR_ROOT_DATETIME, TimeTools.now().toString());

      // plugin version
      final Version version = _bundle.getVersion();
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
      xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
      xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

      // config version
      xmlRoot.putInteger(ATTR_CONFIG_VERSION, CONFIG_VERSION);

      return xmlRoot;
   }

   private static void createDefaults_MapPoints() {

      _allMapPointConfigs.clear();

      // currently only 1 config is supported
      _allMapPointConfigs.add(createDefaults_MapPoints_One(1));

//      // append custom configurations
//      for (int configIndex = 1; configIndex < 11; configIndex++) {
//         _allMapPointConfigs.add(createDefaults_MapPoints_One(configIndex));
//      }
   }

   /**
    * @param configIndex
    *           Index starts with 1.
    *
    * @return
    */
   private static Map2Config createDefaults_MapPoints_One(final int configIndex) {

      final Map2Config config = new Map2Config();

// SET_FORMATTING_OFF

      switch (configIndex) {

      case 1:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_1;
         break;

      case 2:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_2;
         break;

      case 3:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_3;
         break;

      case 4:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_4;
         break;

      case 5:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_5;
         break;

      case 6:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_6;
         break;

      case 7:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_7;
         break;

      case 8:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_8;
         break;

      case 9:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_9;
         break;

      case 10:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_10;
         break;
      }

// SET_FORMATTING_ON

      config.setupColors();

      return config;
   }

   private static void createXml_FromMapPointConfig(final Map2Config config, final IMemento xmlMapPoints) {

// SET_FORMATTING_OFF

      // <MapPoint>
      final IMemento xmlConfig = xmlMapPoints.createChild(TAG_MAP_POINT);
      {
         xmlConfig.putString(ATTR_ID,           config.id);
         xmlConfig.putString(ATTR_CONFIG_NAME,  config.name);

         /*
          * Common
          */
         xmlConfig.putBoolean(      ATTR_IS_LABEL_ANTIALIASED,          config.isLabelAntialiased);
         xmlConfig.putBoolean(      ATTR_IS_SYMBOL_ANTIALIASED,         config.isSymbolAntialiased);
         xmlConfig.putBoolean(      ATTR_IS_TRUNCATE_LABEL,             config.isTruncateLabel);
         xmlConfig.putBoolean(      ATTR_IS_WRAP_LABEL,                 config.isWrapLabel);
         xmlConfig.putInteger(      ATTR_LABEL_DISTRIBUTOR_MAX_LABELS,  config.labelDistributorMaxLabels);
         xmlConfig.putInteger(      ATTR_LABEL_DISTRIBUTOR_RADIUS,      config.labelDistributorRadius);
         xmlConfig.putInteger(      ATTR_LABEL_TRUNCATE_LENGTH,         config.labelTruncateLength);
         xmlConfig.putInteger(      ATTR_LABEL_WRAP_LENGTH,             config.labelWrapLength);

         Util.setXmlEnum(xmlConfig, ATTR_LABEL_LAYOUT,                  config.labelLayout);

         /*
          * Location
          */
         xmlConfig.putBoolean(      ATTR_IS_SHOW_TOUR_LOCATION,                  config.isShowTourLocation);
         xmlConfig.putBoolean(      ATTR_IS_SHOW_LOCATION_BOUNDING_BOX,          config.isShowLocationBoundingBox);
         xmlConfig.putBoolean(      ATTR_IS_SHOW_COMMON_LOCATION,                config.isShowCommonLocation);

         Util.setXmlRgb(xmlConfig,  TAG_COMMON_LOCATION_FILL_COLOR,              config.commonLocationFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_COMMON_LOCATION_OUTLINE_COLOR,           config.commonLocationOutline_RGB);

         Util.setXmlRgb(xmlConfig,  TAG_TOUR_LOCATION_FILL_COLOR,                config.tourLocationFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_TOUR_LOCATION_OUTLINE_COLOR,             config.tourLocationOutline_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_TOUR_LOCATION_START_FILL_COLOR,          config.tourLocation_StartFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_TOUR_LOCATION_START_OUTLINE_COLOR,       config.tourLocation_StartOutline_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_TOUR_LOCATION_END_FILL_COLOR,            config.tourLocation_EndFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_TOUR_LOCATION_END_OUTLINE_COLOR,         config.tourLocation_EndOutline_RGB);

         /*
          * Tour marker
          */
         xmlConfig.putBoolean(      ATTR_IS_SHOW_TOUR_MARKER,              config.isShowTourMarker);

         xmlConfig.putBoolean(      ATTR_IS_GROUP_DUPLICATED_MARKERS,      config.isGroupDuplicatedMarkers);
         xmlConfig.putString(       ATTR_GROUPED_MARKERS,                  config.groupedMarkers);
         xmlConfig.putInteger(      ATTR_GROUP_GRID_SIZE,                  config.groupGridSize);

         Util.setXmlRgb(xmlConfig,  TAG_TOUR_MARKER_FILL_COLOR,            config.tourMarkerFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_TOUR_MARKER_OUTLINE_COLOR,         config.tourMarkerOutline_RGB);

         /*
          * Cluster
          */
         xmlConfig.putBoolean(      ATTR_IS_TOUR_MARKER_CLUSTERED,         config.isTourMarkerClustered);
         xmlConfig.putBoolean(      ATTR_IS_CLUSTER_FILL_OPACITY,          config.isFillClusterSymbol);
         xmlConfig.putInteger(      ATTR_CLUSTER_GRID_SIZE,                config.clusterGridSize);
         xmlConfig.putInteger(      ATTR_CLUSTER_OUTLINE_WIDTH,            config.clusterOutline_Width);
         xmlConfig.putInteger(      ATTR_CLUSTER_SYMBOL_SIZE,              config.clusterSymbol_Size);

         Util.setXmlRgb(xmlConfig,  TAG_CLUSTER_FILL_COLOR,                config.clusterFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_CLUSTER_OUTLINE_COLOR,             config.clusterOutline_RGB);

         /*
          * Tour pauses
          */
         xmlConfig.putBoolean(      ATTR_IS_SHOW_TOUR_PAUSES,              config.isShowTourPauses);

         xmlConfig.putBoolean(      ATTR_IS_FILTER_TOUR_PAUSES,            config.isFilterTourPauses);
         xmlConfig.putBoolean(      ATTR_IS_FILTER_PAUSE_DURATION,         config.isFilterTourPause_Duration);
         xmlConfig.putBoolean(      ATTR_IS_SHOW_AUTO_PAUSES,              config.isShowAutoPauses);
         xmlConfig.putBoolean(      ATTR_IS_SHOW_USER_PAUSES,              config.isShowUserPauses);

         xmlConfig.putBoolean(      ATTR_USE_DURATION_FILTER_HOURS,        config.useTourPause_DurationFilter_Hours);
         xmlConfig.putBoolean(      ATTR_USE_DURATION_FILTER_MINUTES,      config.useTourPause_DurationFilter_Minutes);
         xmlConfig.putBoolean(      ATTR_USE_DURATION_FILTER_SECONDS,      config.useTourPause_DurationFilter_Seconds);

         xmlConfig.putInteger(      ATTR_DURATION_FILTER_HOURS,            config.tourPauseDurationFilter_Hours);
         xmlConfig.putInteger(      ATTR_DURATION_FILTER_MINUTES,          config.tourPauseDurationFilter_Minutes);
         xmlConfig.putInteger(      ATTR_DURATION_FILTER_SECONDS,          config.tourPauseDurationFilter_Seconds);

         Util.setXmlLong(xmlConfig, ATTR_DURATION_FILTER_SUMMARIZED,       config.tourPauseDuration);
         Util.setXmlEnum(xmlConfig, ATTR_DURATION_OPERATOR,                config.tourPauseDurationFilter_Operator);

         Util.setXmlRgb(xmlConfig,  TAG_TOUR_PAUSE_FILL_COLOR,             config.tourPauseFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_TOUR_PAUSE_OUTLINE_COLOR,          config.tourPauseOutline_RGB);
      }

// SET_FORMATTING_ON

   }

   public static Map2Config getActiveConfig() {

      if (_activeMapPointConfig == null) {
         readConfigFromXml();
      }

      return _activeMapPointConfig;
   }

   /**
    * @return Returns the index for the {@link #_activeMapPointConfig}, the index starts with 0.
    */
   public static int getActiveConfigIndex() {

      final Map2Config activeConfig = getActiveConfig();

      for (int configIndex = 0; configIndex < _allMapPointConfigs.size(); configIndex++) {

         final Map2Config config = _allMapPointConfigs.get(configIndex);

         if (config.equals(activeConfig)) {
            return configIndex;
         }
      }

      // this case should not happen but ensure that a correct config is set

      setActiveMapPointConfig(_allMapPointConfigs.get(0));

      return 0;
   }

   public static ArrayList<Map2Config> getAllMapPointConfigs() {

      // ensure configs are loaded
      getActiveConfig();

      return _allMapPointConfigs;
   }

   private static Map2Config getConfig_MapPoint() {

      Map2Config activeConfig = null;

      if (_fromXml_ActiveMapPointConfigId != null) {

         // ensure config id belongs to a config which is available

         for (final Map2Config config : _allMapPointConfigs) {

            if (config.id.equals(_fromXml_ActiveMapPointConfigId)) {

               activeConfig = config;
               break;
            }
         }
      }

      if (activeConfig == null) {

         // this case should not happen, create a config

         StatusUtil.logInfo("Created default config for map points");//$NON-NLS-1$

         createDefaults_MapPoints();

         activeConfig = _allMapPointConfigs.get(0);
      }

      return activeConfig;
   }

   private static File getConfigXmlFile() {

      final File layerFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return layerFile;
   }

   private static void parse_210_Config(final XMLMemento xmlConfig,
                                        final Map2Config config) {

// SET_FORMATTING_OFF

      config.id                           = Util.getXmlString (xmlConfig,     ATTR_ID,                            Long.toString(System.nanoTime()));
      config.name                         = Util.getXmlString (xmlConfig,     ATTR_CONFIG_NAME,                   UI.EMPTY_STRING);

      config.isLabelAntialiased           = Util.getXmlBoolean(xmlConfig,     ATTR_IS_LABEL_ANTIALIASED,          true);
      config.isSymbolAntialiased          = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SYMBOL_ANTIALIASED,         true);

      config.isShowCommonLocation         = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_COMMON_LOCATION,       true);
      config.isShowLocationBoundingBox    = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_LOCATION_BOUNDING_BOX, false);
      config.isShowTourLocation           = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_TOUR_LOCATION,         true);
      config.isShowTourMarker             = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_TOUR_MARKER,           true);

      config.isGroupDuplicatedMarkers     = Util.getXmlBoolean(xmlConfig,     ATTR_IS_GROUP_DUPLICATED_MARKERS,   false);
      config.groupedMarkers               = Util.getXmlString (xmlConfig,     ATTR_GROUPED_MARKERS,               UI.EMPTY_STRING);
      config.groupGridSize                = Util.getXmlInteger(xmlConfig,     ATTR_GROUP_GRID_SIZE,               LABEL_GROUP_GRID_SIZE_DEFAULT,  LABEL_GROUP_GRID_SIZE_MIN,   LABEL_GROUP_GRID_SIZE_MAX);

      config.isTourMarkerClustered        = Util.getXmlBoolean(xmlConfig,     ATTR_IS_TOUR_MARKER_CLUSTERED,      true);
      config.isFillClusterSymbol          = Util.getXmlBoolean(xmlConfig,     ATTR_IS_CLUSTER_FILL_OPACITY,       DEFAULT_IS_FILL_CLUSTER_SYMBOL);
      config.clusterGridSize              = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_GRID_SIZE,             DEFAULT_CLUSTER_GRID_SIZE,          CLUSTER_GRID_SIZE_MIN,     CLUSTER_GRID_SIZE_MAX);
      config.clusterOutline_Width         = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_OUTLINE_WIDTH,         DEFAULT_CLUSTER_OUTLINE_WIDTH,      CLUSTER_OUTLINE_WIDTH_MIN, CLUSTER_OUTLINE_WIDTH_MAX);
      config.clusterSymbol_Size           = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_SYMBOL_SIZE,           DEFAULT_CLUSTER_SYMBOL_SIZE,        CLUSTER_SYMBOL_SIZE_MIN,   CLUSTER_SYMBOL_SIZE_MAX);

      config.isTruncateLabel              = Util.getXmlBoolean(xmlConfig,     ATTR_IS_TRUNCATE_LABEL,             false);
      config.isWrapLabel                  = Util.getXmlBoolean(xmlConfig,     ATTR_IS_WRAP_LABEL,                 false);
      config.labelDistributorMaxLabels    = Util.getXmlInteger(xmlConfig,     ATTR_LABEL_DISTRIBUTOR_MAX_LABELS,  LABEL_DISTRIBUTOR_MAX_LABELS_DEFAULT,  LABEL_DISTRIBUTOR_MAX_LABELS_MIN,   LABEL_DISTRIBUTOR_MAX_LABELS_MAX);
      config.labelDistributorRadius       = Util.getXmlInteger(xmlConfig,     ATTR_LABEL_DISTRIBUTOR_RADIUS,      LABEL_DISTRIBUTOR_RADIUS_DEFAULT,      LABEL_DISTRIBUTOR_RADIUS_MIN,       LABEL_DISTRIBUTOR_RADIUS_MAX);
      config.labelWrapLength              = Util.getXmlInteger(xmlConfig,     ATTR_LABEL_WRAP_LENGTH,             LABEL_WRAP_LENGTH_DEFAULT,             LABEL_WRAP_LENGTH_MIN,              LABEL_WRAP_LENGTH_MAX);
      config.labelTruncateLength          = Util.getXmlInteger(xmlConfig,     ATTR_LABEL_TRUNCATE_LENGTH,         LABEL_WRAP_LENGTH_DEFAULT,             LABEL_TRUNCATE_LENGTH_MIN,          LABEL_TRUNCATE_LENGTH_MAX);
      config.labelLayout = (MapLabelLayout) Util.getXmlEnum(   xmlConfig,     ATTR_LABEL_LAYOUT,                  LABEL_LAYOUT_DEFAULT);

      config.isShowTourPauses                      = Util.getXmlBoolean(xmlConfig,  ATTR_IS_SHOW_TOUR_PAUSES,           false);

      config.isFilterTourPauses                    = Util.getXmlBoolean(xmlConfig,  ATTR_IS_FILTER_TOUR_PAUSES,         IS_FILTER_TOUR_PAUSES_DEFAULT);
      config.isFilterTourPause_Duration            = Util.getXmlBoolean(xmlConfig,  ATTR_IS_FILTER_PAUSE_DURATION,      IS_FILTER_PAUSE_DURATION_DEFAULT);
      config.isShowAutoPauses                      = Util.getXmlBoolean(xmlConfig,  ATTR_IS_SHOW_AUTO_PAUSES,           IS_SHOW_AUTO_PAUSES_DEFAULT);
      config.isShowUserPauses                      = Util.getXmlBoolean(xmlConfig,  ATTR_IS_SHOW_USER_PAUSES,           IS_SHOW_USER_PAUSES_DEFAULT);

      config.useTourPause_DurationFilter_Hours     = Util.getXmlBoolean(xmlConfig,  ATTR_USE_DURATION_FILTER_HOURS,     USE_DURATION_FILTER_HOURS_DEFAULT);
      config.useTourPause_DurationFilter_Minutes   = Util.getXmlBoolean(xmlConfig,  ATTR_USE_DURATION_FILTER_MINUTES,   USE_DURATION_FILTER_MINUTES_DEFAULT);
      config.useTourPause_DurationFilter_Seconds   = Util.getXmlBoolean(xmlConfig,  ATTR_USE_DURATION_FILTER_SECONDS,   USE_DURATION_FILTER_SECONDS_DEFAULT);

      config.tourPauseDurationFilter_Hours         = Util.getXmlInteger(xmlConfig,  ATTR_DURATION_FILTER_HOURS,         DURATION_FILTER_HOURS_DEFAULT);
      config.tourPauseDurationFilter_Minutes       = Util.getXmlInteger(xmlConfig,  ATTR_DURATION_FILTER_MINUTES,       DURATION_FILTER_MINUTES_DEFAULT);
      config.tourPauseDurationFilter_Seconds       = Util.getXmlInteger(xmlConfig,  ATTR_DURATION_FILTER_SECONDS,       DURATION_FILTER_SECONDS_DEFAULT);

      config.tourPauseDuration                     = Util.getXmlLong(xmlConfig, ATTR_DURATION_FILTER_SUMMARIZED,        0L);
      config.tourPauseDurationFilter_Operator      = (TourFilterFieldOperator) Util.getXmlEnum(xmlConfig, ATTR_DURATION_OPERATOR,                 DURATION_OPERATOR_DEFAULT);

// SET_FORMATTING_ON

      /*
       * Each color is in it's own tag
       */
      for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

         final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
         final String configTag = xmlConfigChild.getType();

         switch (configTag) {

// SET_FORMATTING_OFF

         case TAG_CLUSTER_FILL_COLOR:                    config.clusterFill_RGB                    = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_FILL_RGB);                   break;
         case TAG_CLUSTER_OUTLINE_COLOR:                 config.clusterOutline_RGB                 = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_OUTLINE_RGB);                break;

         case TAG_COMMON_LOCATION_FILL_COLOR:            config.commonLocationFill_RGB             = Util.getXmlRgb(xmlConfigChild, DEFAULT_COMMON_LOCATION_FILL_RGB);           break;
         case TAG_COMMON_LOCATION_OUTLINE_COLOR:         config.commonLocationOutline_RGB          = Util.getXmlRgb(xmlConfigChild, DEFAULT_COMMON_LOCATION_OUTLINE_RGB);        break;

         case TAG_TOUR_LOCATION_FILL_COLOR:              config.tourLocationFill_RGB               = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_LOCATION_FILL_RGB);             break;
         case TAG_TOUR_LOCATION_OUTLINE_COLOR:           config.tourLocationOutline_RGB            = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_LOCATION_OUTLINE_RGB);          break;
         case TAG_TOUR_LOCATION_START_FILL_COLOR:        config.tourLocation_StartFill_RGB         = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_LOCATION_START_FILL_RGB);       break;
         case TAG_TOUR_LOCATION_START_OUTLINE_COLOR:     config.tourLocation_StartOutline_RGB      = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_LOCATION_START_OUTLINE_RGB);    break;
         case TAG_TOUR_LOCATION_END_FILL_COLOR:          config.tourLocation_EndFill_RGB           = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_LOCATION_END_FILL_RGB);         break;
         case TAG_TOUR_LOCATION_END_OUTLINE_COLOR:       config.tourLocation_EndOutline_RGB        = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_LOCATION_END_OUTLINE_RGB);      break;

         case TAG_TOUR_MARKER_FILL_COLOR:                config.tourMarkerFill_RGB                 = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_MARKER_FILL_RGB);               break;
         case TAG_TOUR_MARKER_OUTLINE_COLOR:             config.tourMarkerOutline_RGB              = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_MARKER_OUTLINE_RGB);            break;

         case TAG_TOUR_PAUSE_FILL_COLOR:                 config.tourPauseFill_RGB                  = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_PAUSE_FILL_RGB);                break;
         case TAG_TOUR_PAUSE_OUTLINE_COLOR:              config.tourPauseOutline_RGB               = Util.getXmlRgb(xmlConfigChild, DEFAULT_TOUR_PAUSE_OUTLINE_RGB);             break;

// SET_FORMATTING_ON
         }
      }

      config.setupColors();
   }

   /**
    * Read or create configuration a xml file
    *
    * @return
    */
   private static void readConfigFromXml() {

      InputStreamReader reader = null;

      try {

         XMLMemento xmlRoot = null;

         // try to get layer structure from saved xml file
         final File layerFile = getConfigXmlFile();
         final String absoluteLayerPath = layerFile.getAbsolutePath();

         final File inputFile = new File(absoluteLayerPath);
         if (inputFile.exists()) {

            try {

               reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
               xmlRoot = XMLMemento.createReadRoot(reader);

            } catch (final Exception e) {
               // ignore
            }
         }

         // parse xml and set states
         restoreState_10_Options(xmlRoot);
         restoreState_30_MapPoints(xmlRoot, _allMapPointConfigs);

         // ensure config is created
         if (_allMapPointConfigs.isEmpty()) {
            createDefaults_MapPoints();
         }

         setActiveMapPointConfig(getConfig_MapPoint());

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   public static void resetActiveMapPointConfiguration() {

      // keep old values
      final String oldName = _activeMapPointConfig.name;
      final String oldGroupedMarkers = _activeMapPointConfig.groupedMarkers;

      final int activeConfigIndex = getActiveConfigIndex();

      // remove old config
      _allMapPointConfigs.remove(_activeMapPointConfig);

      // create new config
      final int configID = activeConfigIndex + 1;

      final Map2Config newConfig = createDefaults_MapPoints_One(configID);

      newConfig.name = oldName;
      newConfig.groupedMarkers = oldGroupedMarkers;

      // update model
      setActiveMapPointConfig(newConfig);
      _allMapPointConfigs.add(activeConfigIndex, newConfig);
   }

   public static void resetAllMapPointConfigurations() {

      createDefaults_MapPoints();

      setActiveMapPointConfig(_allMapPointConfigs.get(0));
   }

   private static void restoreState_10_Options(final XMLMemento xmlRoot) {

      if (xmlRoot == null) {
         return;
      }

      final XMLMemento xmlOptions = (XMLMemento) xmlRoot.getChild(TAG_OPTIONS);

      if (xmlOptions == null) {
         return;
      }
   }

   private static void restoreState_30_MapPoints(final XMLMemento xmlRoot,
                                                 final ArrayList<Map2Config> allConfigs) {

      if (xmlRoot == null) {
         return;
      }

      final XMLMemento xmlMapPoints = (XMLMemento) xmlRoot.getChild(TAG_MAP_POINTS);

      if (xmlMapPoints == null) {
         return;
      }

      _fromXml_ActiveMapPointConfigId = Util.getXmlString(xmlMapPoints, ATTR_ACTIVE_CONFIG_ID, null);

      for (final IMemento mementoConfig : xmlMapPoints.getChildren()) {

         final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

         try {

            final String xmlConfigType = xmlConfig.getType();

            if (xmlConfigType.equals(TAG_MAP_POINT)) {

               // <MapPoint>

               final Map2Config mapConfig = new Map2Config();

               parse_210_Config(xmlConfig, mapConfig);

               allConfigs.add(mapConfig);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlConfig), e);
         }
      }
   }

   public static void saveState() {

      if (_activeMapPointConfig == null) {

         // this can happen when not yet used

         return;
      }

      final XMLMemento xmlRoot = create_Root();

      saveState_MapPoints(xmlRoot);

      Util.writeXml(xmlRoot, getConfigXmlFile());
   }

   /**
    * Map points
    */
   private static void saveState_MapPoints(final XMLMemento xmlRoot) {

      final IMemento xmlMapPoints = xmlRoot.createChild(TAG_MAP_POINTS);
      {
         xmlMapPoints.putString(ATTR_ACTIVE_CONFIG_ID, _activeMapPointConfig.id);

         for (final Map2Config config : _allMapPointConfigs) {
            createXml_FromMapPointConfig(config, xmlMapPoints);
         }
      }
   }

   private static void setActiveMapPointConfig(final Map2Config newConfig) {

      _activeMapPointConfig = newConfig;
   }

}
