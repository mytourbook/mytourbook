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
package net.tourbook.map25;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEntry;
import net.tourbook.map25.layer.marker.ClusterAlgorithm;
import net.tourbook.map25.layer.marker.MarkerConfig;
import net.tourbook.map25.layer.tourtrack.Map25TrackConfig;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.oscim.core.BoundingBox;
import org.oscim.core.MapPosition;
import org.oscim.map.Animator;
import org.oscim.map.Map;
import org.oscim.utils.ThreadUtils;
import org.oscim.utils.animation.Easing;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class Map25ConfigManager {

   private static final String CONFIG_FILE_NAME             = "map25-config.xml";                     //$NON-NLS-1$
   //
   /**
    * Version number is not yet used.
    */
   private static final int    CONFIG_VERSION               = 1;

   private static final Bundle _bundle                      = TourbookPlugin.getDefault().getBundle();
   private static final IPath  _stateLocation               = Platform.getStateLocation(_bundle);

   public static final int     SYMBOL_ORIENTATION_BILLBOARD = 0;
   public static final int     SYMBOL_ORIENTATION_GROUND    = 1;

// SET_FORMATTING_OFF

   public static final ComboEntry[]           SYMBOL_ORIENTATION           = {

         new ComboEntry(Messages.Map25_Config_SymbolOrientation_Billboard,    SYMBOL_ORIENTATION_BILLBOARD),
         new ComboEntry(Messages.Map25_Config_SymbolOrientation_Ground,       SYMBOL_ORIENTATION_GROUND),
   };

   public static final ClusterAlgorithmItem[] ALL_CLUSTER_ALGORITHM        = {

         new ClusterAlgorithmItem(Messages.Map25_Config_ClusterAlgorithm_FirstMarker_Distance,  ClusterAlgorithm.FirstMarker_Distance),
         new ClusterAlgorithmItem(Messages.Map25_Config_ClusterAlgorithm_FirstMarker_Grid,      ClusterAlgorithm.FirstMarker_Grid),
         new ClusterAlgorithmItem(Messages.Map25_Config_ClusterAlgorithm_Grid,                  ClusterAlgorithm.Grid_Center),
   };
   
// SET_FORMATTING_ON

   public static final String CONFIG_DEFAULT_ID_1  = "#1";  //$NON-NLS-1$

   static final String        CONFIG_DEFAULT_ID_2  = "#2";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_3  = "#3";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_4  = "#4";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_5  = "#5";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_6  = "#6";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_7  = "#7";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_8  = "#8";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_9  = "#9";  //$NON-NLS-1$
   static final String        CONFIG_DEFAULT_ID_10 = "#10"; //$NON-NLS-1$
   //
   // common attributes
   private static final String ATTR_ACTIVE_CONFIG_ID = "activeConfigId"; //$NON-NLS-1$

   private static final String ATTR_ID               = "id";             //$NON-NLS-1$
   private static final String ATTR_CONFIG_NAME      = "name";           //$NON-NLS-1$
   //
   /*
    * Root
    */
   private static final String TAG_ROOT            = "Map25Configuration"; //$NON-NLS-1$
   private static final String ATTR_CONFIG_VERSION = "configVersion";      //$NON-NLS-1$
   //
   /*
    * Tour options
    */
   private static final String TAG_OPTIONS                     = "Options";                      //$NON-NLS-1$
   private static final String ATTR_ANIMATION_EASING_TYPE      = "animationEasingType";          //$NON-NLS-1$
   private static final String ATTR_ANIMATION_TIME             = "animationTime";                //$NON-NLS-1$
   private static final String ATTR_IS_ANIMATE_LOCATION        = "isAnimateLocation";            //$NON-NLS-1$
   private static final String ATTR_USE_DRAGGED_KEY_NAVIGATION = "useDraggedKeyboardNavigation"; //$NON-NLS-1$
   //
   /*
    * Tour tracks
    */
   private static final String TAG_TOUR_TRACKS = "TourTracks"; //$NON-NLS-1$
   private static final String TAG_TRACK       = "Track";      //$NON-NLS-1$
   //
   // outline
   private static final String TAG_OUTLINE             = "Outline";               //$NON-NLS-1$
   private static final String ATTR_OUTLINE_OPACITY    = "opacity";               //$NON-NLS-1$
   private static final String ATTR_OUTLINE_WIDTH      = "width";                 //$NON-NLS-1$
   //
   public static final RGB     DEFAULT_OUTLINE_COLOR   = new RGB(0x80, 0x0, 0x80);
   public static final int     DEFAULT_OUTLINE_OPACITY = 70;
   public static final float   DEFAULT_OUTLINE_WIDTH   = 2.5f;
   public static final int     OUTLINE_OPACITY_MIN     = 1;
   public static final int     OUTLINE_OPACITY_MAX     = 100;
   public static final float   OUTLINE_WIDTH_MIN       = 0.1f;
   public static final float   OUTLINE_WIDTH_MAX       = 20.0f;
   //
   // slider location/path
   private static final String TAG_SLIDER_PATH                     = "SliderPath";             //$NON-NLS-1$
   private static final String TAG_SLIDER_LOCATION_LEFT            = "SliderLocation_Left";    //$NON-NLS-1$
   private static final String TAG_SLIDER_LOCATION_RIGHT           = "SliderLocation_Right";   //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_SLIDER_LOCATION        = "isShowSliderLocation";   //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_SLIDER_PATH            = "isShowSliderPath";       //$NON-NLS-1$
   private static final String ATTR_SLIDER_LOCATION_OPACITY        = "sliderLocation_Opacity"; //$NON-NLS-1$
   private static final String ATTR_SLIDER_LOCATION_SIZE           = "sliderLocation_Size";    //$NON-NLS-1$
   private static final String ATTR_SLIDER_PATH_LINE_WIDTH         = "sliderPath_LineWidth";   //$NON-NLS-1$
   private static final String ATTR_SLIDER_PATH_OPACITY            = "sliderPath_Opacity";     //$NON-NLS-1$
   //
   public static final boolean DEFAULT_IS_SHOW_SLIDER_PATH         = true;
   public static final int     DEFAULT_SLIDER_LOCATION_OPACITY     = 100;
   public static final int     DEFAULT_SLIDER_LOCATION_SIZE        = 30;
   public static final RGB     DEFAULT_SLIDER_LOCATION_LEFT_COLOR  = new RGB(0xff, 0x0, 0x0);
   public static final RGB     DEFAULT_SLIDER_LOCATION_RIGHT_COLOR = new RGB(0x0, 0xff, 0x0);
   public static final int     SLIDER_LOCATION_OPACITY_MIN         = 1;
   public static final int     SLIDER_LOCATION_OPACITY_MAX         = 100;
   public static final int     SLIDER_LOCATION_SIZE_MIN            = 10;
   public static final int     SLIDER_LOCATION_SIZE_MAX            = 100;
   //
   public static final boolean DEFAULT_IS_SHOW_CHART_SLIDER        = true;
   public static final RGB     DEFAULT_SLIDER_PATH_COLOR           = new RGB(0xff, 0xff, 0x0);
   public static final float   DEFAULT_SLIDER_PATH_LINE_WIDTH      = 20.0f;
   public static final int     DEFAULT_SLIDER_PATH_OPACITY         = 30;
   public static float         SLIDER_PATH_LINE_WIDTH_MIN          = 1.0f;
   public static final float   SLIDER_PATH_LINE_WIDTH_MAX          = 50.0f;
   public static final int     SLIDER_PATH_OPACITY_MIN             = 1;
   public static final int     SLIDER_PATH_OPACITY_MAX             = 100;
   //
   // other properties
   public static final int DEFAULT_ANIMATION_TIME = 2000;
   //
   /*
    * Tour marker, map bookmarks, photomarks
    */
   private static final String TAG_TOUR_MARKERS = "TourMarkers"; //$NON-NLS-1$
   private static final String TAG_MARKER       = "Marker";      //$NON-NLS-1$
   //
   // marker
   private static final String TAG_MARKER_FILL_COLOR       = "MarkerFillColor";      //$NON-NLS-1$
   private static final String TAG_MARKER_OUTLINE_COLOR    = "MarkerOutlineColor";   //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_TOUR_MARKER    = "isShowTourMarker";     //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_MAP_BOOKMARK   = "isShowMapBookmark";    //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_PHOTO          = "isShowPhoto";          //$NON-NLS-1$
   private static final String ATTR_IS_SHOW_PHOTO_TITLE    = "isShowPhotoTitle";     //$NON-NLS-1$
   private static final String ATTR_MARKER_ORIENTATION     = "markerOrientation";    //$NON-NLS-1$
   //
   private static final String ATTR_MARKER_FILL_OPACITY    = "markerFillOpacity";    //$NON-NLS-1$
   private static final String ATTR_MARKER_OUTLINE_OPACITY = "markerOutlineOpacity"; //$NON-NLS-1$
   private static final String ATTR_MARKER_OUTLINE_SIZE    = "markerOutlineSize";    //$NON-NLS-1$
   private static final String ATTR_MARKER_SYMBOL_SIZE     = "markerSymbolSize";     //$NON-NLS-1$
   //
   // cluster
   private static final String TAG_CLUSTER_FILL_COLOR       = "ClusterFillColor";     //$NON-NLS-1$
   private static final String TAG_CLUSTER_OUTLINE_COLOR    = "ClusterOutlineColor";  //$NON-NLS-1$
   private static final String ATTR_CLUSTER_ALGORITHM       = "clusterAlgorithm";     //$NON-NLS-1$
   private static final String ATTR_CLUSTER_ORIENTATION     = "clusterOrientation";   //$NON-NLS-1$
   private static final String ATTR_CLUSTER_GRID_SIZE       = "clusterGridSize";      //$NON-NLS-1$
   private static final String ATTR_IS_MARKER_CLUSTERED     = "isMarkerClustered";    //$NON-NLS-1$
   private static final String ATTR_IS_PHOTO_CLUSTERED      = "isPhotoClustered";    //$NON-NLS-1$
   //
   private static final String ATTR_CLUSTER_FILL_OPACITY    = "clusterFillOpacity";   //$NON-NLS-1$
   private static final String ATTR_CLUSTER_OUTLINE_OPACITY = "cluserOutlineOpacity"; //$NON-NLS-1$
   private static final String ATTR_CLUSTER_OUTLINE_SIZE    = "clusterOutlineSize";   //$NON-NLS-1$
   private static final String ATTR_CLUSTER_SYMBOL_SIZE     = "clusterSymbolSize";    //$NON-NLS-1$
   private static final String ATTR_CLUSTER_SYMBOL_WEIGHT   = "clusterSymbolWeight";  //$NON-NLS-1$
   //
   /*
    * Defaults, min/max
    */
   // symbol
   public static final int   DEFAULT_MARKER_SYMBOL_SIZE  = 20;
   public static final float DEFAULT_MARKER_OUTLINE_SIZE = 2.0f;
   public static final float MARKER_OUTLINE_SIZE_MIN     = 0;
   public static final float MARKER_OUTLINE_SIZE_MAX     = 100;
   public static final int   MARKER_SYMBOL_SIZE_MIN      = 10;
   public static final int   MARKER_SYMBOL_SIZE_MAX      = 200;
   //
   // CLUSTER
   public static final int DEFAULT_CLUSTER_GRID_SIZE     = 60;
   public static final int DEFAULT_CLUSTER_SYMBOL_SIZE   = 10;
   public static final int DEFAULT_CLUSTER_SYMBOL_WEIGHT = 20;
   public static final int CLUSTER_GRID_MIN_SIZE         = 1;
   public static final int CLUSTER_GRID_MAX_SIZE         = 10000;
   public static final int CLUSTER_OUTLINE_SIZE_MIN      = 0;
   public static final int CLUSTER_OUTLINE_SIZE_MAX      = 100;
   public static final int CLUSTER_SYMBOL_SIZE_MIN       = 5;
   public static final int CLUSTER_SYMBOL_SIZE_MAX       = 200;
   public static final int CLUSTER_SYMBOL_WEIGHT_MIN     = 0;
   public static final int CLUSTER_SYMBOL_WEIGHT_MAX     = 100;
   //
   // colors
   public static final RGB   DEFAULT_CLUSTER_FILL_COLOR      = new RGB(0xFC, 0x67, 0x00);
   public static final int   DEFAULT_CLUSTER_FILL_OPACITY    = 80;
   public static final RGB   DEFAULT_CLUSTER_OUTLINE_COLOR   = new RGB(0xff, 0xff, 0xff);
   public static final int   DEFAULT_CLUSTER_OUTLINE_OPACITY = 80;
   public static final float DEFAULT_CLUSTER_OUTLINE_SIZE    = 2.0f;
   public static final RGB   DEFAULT_MARKER_FILL_COLOR       = new RGB(0xFF, 0xFF, 0x00);
   public static final int   DEFAULT_MARKER_FILL_OPACITY     = 80;
   public static final RGB   DEFAULT_MARKER_OUTLINE_COLOR    = new RGB(0, 0, 0);
   public static final int   DEFAULT_MARKER_OUTLINE_OPACITY  = 80;
   //
   // map movement with animation
   private static final Easing.Type ANIMATION_EASING_TYPE_DEFAULT   = Easing.Type.SINE_INOUT;
   private static final boolean     IS_ANIMATE_LOCATION_DEFAULT     = true;
   private static final float       LOCATION_ANIMATION_TIME_DEFAULT = 3.0f;
   public static final float        LOCATION_ANIMATION_TIME_MIN     = 0f;
   public static final float        LOCATION_ANIMATION_TIME_MAX     = 60.0f;
   //
   // options
   private static final boolean USE_DRAGGED_KEY_NAVIGATION_DEFAULT = false;
   //
   public static Easing.Type    animationEasingType                = ANIMATION_EASING_TYPE_DEFAULT;
   public static float          animationTime                      = LOCATION_ANIMATION_TIME_DEFAULT;
   public static boolean        isAnimateLocation                  = IS_ANIMATE_LOCATION_DEFAULT;
   public static boolean        useDraggedKeyboardNavigation       = USE_DRAGGED_KEY_NAVIGATION_DEFAULT;
   //
   // !!! this is a code formatting separator !!!
   static {}
   //
   /**
    * Contains all configurations which are loaded from a xml file.
    */
   private static final ArrayList<Map25TrackConfig> _allTrackConfigs  = new ArrayList<>();
   private static Map25TrackConfig                  _activeTrackConfig;
   private static final ArrayList<MarkerConfig>     _allMarkerConfigs = new ArrayList<>();
   private static MarkerConfig                      _activeMarkerConfig;
   //
   private static String                            _fromXml_ActiveMarkerConfigId;
   private static String                            _fromXml_ActiveTrackConfigId;
   // !!! enable new formatting
   {}

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

   private static void createDefaults_Markers() {

      _allMarkerConfigs.clear();

      // append custom configurations
      for (int configIndex = 1; configIndex < 11; configIndex++) {
         _allMarkerConfigs.add(createDefaults_Markers_One(configIndex));
      }
   }

   /**
    * @param configIndex
    *           Index starts with 1.
    * @return
    */
   private static MarkerConfig createDefaults_Markers_One(final int configIndex) {

      final MarkerConfig config = new MarkerConfig();

      final RGB fgBlack = new RGB(0, 0, 0);
      final RGB fgWhite = new RGB(0xff, 0xff, 0xff);

      final RGB bg1 = new RGB(0x00, 0xA0, 0xED);
      final RGB bg2 = new RGB(0xC6, 0x00, 0xA2);
      final RGB bg3 = new RGB(0x00, 0xC4, 0x2C);
      final RGB bg4 = new RGB(0xFF, 0xC9, 0x00);
      final RGB bg5 = new RGB(0xFF, 0x00, 0x62);

      config.clusterAlgorithm = ClusterAlgorithm.FirstMarker_Distance;
      config.markerOutline_Color = fgBlack;
      config.markerFill_Color = fgWhite;

      switch (configIndex) {

      case 1:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_1;
         config.clusterOutline_Color = fgBlack;
         config.clusterFill_Color = bg1;
         config.markerOutline_Color = fgBlack;
         config.markerFill_Color = bg5;
         break;
      case 2:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_2;
         config.clusterOutline_Color = fgWhite;
         config.clusterFill_Color = bg1;
         break;

      case 3:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_3;
         config.clusterOutline_Color = fgBlack;
         config.clusterFill_Color = bg2;
         break;
      case 4:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_4;
         config.clusterOutline_Color = fgWhite;
         config.clusterFill_Color = bg2;
         break;

      case 5:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_5;
         config.clusterOutline_Color = fgBlack;
         config.clusterFill_Color = bg3;
         break;
      case 6:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_6;
         config.clusterOutline_Color = fgWhite;
         config.clusterFill_Color = bg3;
         break;

      case 7:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_7;
         config.clusterOutline_Color = fgBlack;
         config.clusterFill_Color = bg4;
         break;
      case 8:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_8;
         config.clusterOutline_Color = fgWhite;
         config.clusterFill_Color = bg4;
         break;

      case 9:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_9;
         config.clusterOutline_Color = fgBlack;
         config.clusterFill_Color = bg5;
         break;
      case 10:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_10;
         config.clusterOutline_Color = fgWhite;
         config.clusterFill_Color = bg5;
         break;

      }

      return config;
   }

   private static void createDefaults_Tracks() {

      _allTrackConfigs.clear();

      // append custom configurations
      for (int customIndex = 1; customIndex < 11; customIndex++) {
         _allTrackConfigs.add(createDefaults_Tracks_One(customIndex));
      }
   }

   /**
    * Overwrite default defaults.
    * 
    * @param configIndex
    *           Index starts with 1.
    * @return
    */
   private static Map25TrackConfig createDefaults_Tracks_One(final int configIndex) {

      final Map25TrackConfig config = new Map25TrackConfig();

      switch (configIndex) {

      case 1:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_1;
         config.outlineWidth = DEFAULT_OUTLINE_WIDTH;
         break;

      case 2:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_2;
         config.outlineWidth = 1;
         break;

      case 3:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_3;
         config.outlineWidth = 3;
         break;

      case 4:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_4;
         config.outlineWidth = 4;
         break;

      case 5:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_5;
         config.outlineWidth = 5;
         break;

      case 6:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_6;
         config.outlineWidth = 6;
         break;

      case 7:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_7;
         config.outlineWidth = 7;
         break;

      case 8:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_8;
         config.outlineWidth = 8;
         break;

      case 9:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_9;
         config.outlineWidth = 9;
         break;

      case 10:
         config.name = config.defaultId = CONFIG_DEFAULT_ID_10;
         config.outlineWidth = 10;
         break;

      }

      return config;
   }

// SET_FORMATTING_OFF
   
   private static void createXml_FromMarkerConfig(final MarkerConfig config, final IMemento xmlMarkers) {

      // <Marker>
      final IMemento xmlConfig = xmlMarkers.createChild(TAG_MARKER);
      {
         xmlConfig.putString(ATTR_ID,           config.id);
         xmlConfig.putString(ATTR_CONFIG_NAME,  config.name);

         /*
          * Marker
          */
         xmlConfig.putBoolean(      ATTR_IS_SHOW_TOUR_MARKER,     config.isShowTourMarker);
         xmlConfig.putBoolean(      ATTR_IS_SHOW_MAP_BOOKMARK,    config.isShowMapBookmark);
         xmlConfig.putBoolean(      ATTR_IS_SHOW_PHOTO,           config.isShowPhoto);
         xmlConfig.putBoolean(      ATTR_IS_SHOW_PHOTO_TITLE,     config.isShowPhotoTitle);
         xmlConfig.putInteger(      ATTR_MARKER_ORIENTATION,      config.markerOrientation);

         xmlConfig.putInteger(      ATTR_MARKER_FILL_OPACITY,     config.markerFill_Opacity);
         xmlConfig.putInteger(      ATTR_MARKER_OUTLINE_OPACITY,  config.markerOutline_Opacity);
         xmlConfig.putFloat(        ATTR_MARKER_OUTLINE_SIZE,     config.markerOutline_Size);
         xmlConfig.putInteger(      ATTR_MARKER_SYMBOL_SIZE,      config.markerSymbol_Size);

         Util.setXmlRgb(xmlConfig,  TAG_MARKER_OUTLINE_COLOR,     config.markerOutline_Color);
         Util.setXmlRgb(xmlConfig,  TAG_MARKER_FILL_COLOR,        config.markerFill_Color);

         /*
          * Cluster
          */
         Util.setXmlEnum(xmlConfig, ATTR_CLUSTER_ALGORITHM,       config.clusterAlgorithm);
         xmlConfig.putInteger(      ATTR_CLUSTER_GRID_SIZE,       config.clusterGrid_Size);
         xmlConfig.putInteger(      ATTR_CLUSTER_ORIENTATION,     config.clusterOrientation);
         xmlConfig.putBoolean(      ATTR_IS_MARKER_CLUSTERED,     config.isMarkerClustered);
         xmlConfig.putBoolean(      ATTR_IS_PHOTO_CLUSTERED,      config.isPhotoClustered);

         xmlConfig.putInteger(      ATTR_CLUSTER_FILL_OPACITY,    config.clusterFill_Opacity);
         xmlConfig.putInteger(      ATTR_CLUSTER_OUTLINE_OPACITY, config.clusterOutline_Opacity);
         xmlConfig.putFloat(        ATTR_CLUSTER_OUTLINE_SIZE,    config.clusterOutline_Size);
         xmlConfig.putInteger(      ATTR_CLUSTER_SYMBOL_SIZE,     config.clusterSymbol_Size);
         xmlConfig.putInteger(      ATTR_CLUSTER_SYMBOL_WEIGHT,   config.clusterSymbol_Weight);

         Util.setXmlRgb(xmlConfig,  TAG_CLUSTER_OUTLINE_COLOR,    config.clusterOutline_Color);
         Util.setXmlRgb(xmlConfig,  TAG_CLUSTER_FILL_COLOR,       config.clusterFill_Color);
      }
   }

   private static void createXml_FromTrackConfig(final Map25TrackConfig config, final IMemento xmlTourTracks) {

      // <Track>
      final IMemento xmlConfig = xmlTourTracks.createChild(TAG_TRACK);
      {
         xmlConfig.putString(ATTR_ID,           config.id);
         xmlConfig.putString(ATTR_CONFIG_NAME,  config.name);

//         xmlConfig.putInteger(ATTR_ANIMATION_TIME, config.animationTime);

         // <Outline>
         final IMemento xmlOutline = Util.setXmlRgb(xmlConfig, TAG_OUTLINE, config.outlineColor);
         {
            xmlOutline.putFloat(       ATTR_OUTLINE_WIDTH,           config.outlineWidth);
            xmlOutline.putInteger(     ATTR_OUTLINE_OPACITY,         config.outlineOpacity);
         }

         // <SliderPath>
         final IMemento xmlSliderPath = Util.setXmlRgb(xmlConfig, TAG_SLIDER_PATH, config.sliderPath_Color);
         {
            xmlSliderPath.putBoolean(  ATTR_IS_SHOW_SLIDER_PATH,     config.isShowSliderPath);
            xmlSliderPath.putFloat(    ATTR_SLIDER_PATH_LINE_WIDTH,  config.sliderPath_LineWidth);
            xmlSliderPath.putInteger(  ATTR_SLIDER_PATH_OPACITY,     config.sliderPath_Opacity);
         }

         /*
          * Slider location
          */
         xmlConfig.putBoolean(   ATTR_IS_SHOW_SLIDER_LOCATION,    config.isShowSliderLocation);
         xmlConfig.putInteger(   ATTR_SLIDER_LOCATION_OPACITY,    config.sliderLocation_Opacity);
         xmlConfig.putInteger(   ATTR_SLIDER_LOCATION_SIZE,       config.sliderLocation_Size);
         
         // <SliderLocation_Left>
         Util.setXmlRgb(xmlConfig, TAG_SLIDER_LOCATION_LEFT,      config.sliderLocation_Left_Color);
         
         // <SliderLocation_Right>
         Util.setXmlRgb(xmlConfig, TAG_SLIDER_LOCATION_RIGHT,     config.sliderLocation_Right_Color);
      }
   }
   
// SET_FORMATTING_ON

   public static MarkerConfig getActiveMarkerConfig() {

      if (_activeMarkerConfig == null) {
         readConfigFromXml();
      }

      return _activeMarkerConfig;
   }

   /**
    * @return Returns the index for the {@link #_activeMarkerConfig}, the index starts with 0.
    */
   public static int getActiveMarkerConfigIndex() {

      final MarkerConfig activeConfig = getActiveMarkerConfig();

      for (int configIndex = 0; configIndex < _allMarkerConfigs.size(); configIndex++) {

         final MarkerConfig config = _allMarkerConfigs.get(configIndex);

         if (config.equals(activeConfig)) {
            return configIndex;
         }
      }

      // this case should not happen but ensure that a correct config is set

      _activeMarkerConfig = _allMarkerConfigs.get(0);

      return 0;
   }

   public static Map25TrackConfig getActiveTourTrackConfig() {

      if (_activeTrackConfig == null) {
         readConfigFromXml();
      }

      return _activeTrackConfig;
   }

   /**
    * @return Returns the index of the active config within all configs.
    */
   public static int getActiveTourTrackConfigIndex() {

      final Map25TrackConfig activeConfig = getActiveTourTrackConfig();

      for (int configIndex = 0; configIndex < _allTrackConfigs.size(); configIndex++) {

         final Map25TrackConfig config = _allTrackConfigs.get(configIndex);

         if (config == activeConfig) {
            return configIndex;
         }
      }

      // this case should not happen but ensure that a correct config is set

      _activeTrackConfig = _allTrackConfigs.get(0);

      return 0;
   }

   public static ArrayList<MarkerConfig> getAllMarkerConfigs() {

      // ensure configs are loaded
      getActiveMarkerConfig();

      return _allMarkerConfigs;
   }

   public static ArrayList<Map25TrackConfig> getAllTourTrackConfigs() {

      // ensure configs are loaded
      getActiveTourTrackConfig();

      return _allTrackConfigs;
   }

   private static MarkerConfig getConfig_Marker() {

      MarkerConfig activeConfig = null;

      if (_fromXml_ActiveMarkerConfigId != null) {

         // ensure config id belongs to a config which is available

         for (final MarkerConfig config : _allMarkerConfigs) {

            if (config.id.equals(_fromXml_ActiveMarkerConfigId)) {

               activeConfig = config;
               break;
            }
         }
      }

      if (activeConfig == null) {

         // this case should not happen, create a config

         StatusUtil.log("Created default config for marker properties");//$NON-NLS-1$

         createDefaults_Markers();

         activeConfig = _allMarkerConfigs.get(0);
      }

      return activeConfig;
   }

   private static Map25TrackConfig getConfig_Track() {

      Map25TrackConfig activeConfig = null;

      if (_fromXml_ActiveTrackConfigId != null) {

         // ensure config id belongs to a config which is available

         for (final Map25TrackConfig config : _allTrackConfigs) {

            if (config.id.equals(_fromXml_ActiveTrackConfigId)) {

               activeConfig = config;
               break;
            }
         }
      }

      if (activeConfig == null) {

         // this case should not happen, create a config

         StatusUtil.log("Created default config for tour track properties");//$NON-NLS-1$

         createDefaults_Tracks();

         activeConfig = _allTrackConfigs.get(0);
      }

      return activeConfig;
   }

   private static File getConfigXmlFile() {

      final File layerFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return layerFile;
   }

   private static void parse_050_TrackConfig(final XMLMemento xmlConfig, final Map25TrackConfig config) {

// SET_FORMATTING_OFF
      
      config.id   = Util.getXmlString(xmlConfig, ATTR_ID,            Long.toString(System.nanoTime()));
      config.name = Util.getXmlString(xmlConfig, ATTR_CONFIG_NAME,   UI.EMPTY_STRING);

      config.isShowSliderLocation      = Util.getXmlBoolean(xmlConfig,  ATTR_IS_SHOW_SLIDER_LOCATION,    DEFAULT_IS_SHOW_CHART_SLIDER);
      config.sliderLocation_Opacity    = Util.getXmlInteger(xmlConfig,  ATTR_SLIDER_LOCATION_OPACITY,    DEFAULT_SLIDER_LOCATION_OPACITY,    SLIDER_LOCATION_OPACITY_MIN,   SLIDER_LOCATION_OPACITY_MAX);
      config.sliderLocation_Size       = Util.getXmlInteger(xmlConfig,  ATTR_SLIDER_LOCATION_SIZE,       DEFAULT_SLIDER_LOCATION_SIZE,       SLIDER_LOCATION_SIZE_MIN,      SLIDER_LOCATION_SIZE_MAX);

      for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

         final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;

         switch (xmlConfigChild.getType()) {
            
         case TAG_OUTLINE:

            config.outlineColor        = Util.getXmlRgb(xmlConfigChild,          DEFAULT_OUTLINE_COLOR);
            config.outlineOpacity      = Util.getXmlInteger(xmlConfigChild,      ATTR_OUTLINE_OPACITY,   DEFAULT_OUTLINE_OPACITY,   OUTLINE_OPACITY_MIN, OUTLINE_OPACITY_MAX);
            config.outlineWidth        = Util.getXmlFloatFloat(xmlConfigChild,   ATTR_OUTLINE_WIDTH,     DEFAULT_OUTLINE_WIDTH,     OUTLINE_WIDTH_MIN,   OUTLINE_WIDTH_MAX);
            break;
            
         case TAG_SLIDER_PATH:
            
            config.isShowSliderPath    = Util.getXmlBoolean(xmlConfigChild,      ATTR_IS_SHOW_SLIDER_PATH,       DEFAULT_IS_SHOW_SLIDER_PATH);
            config.sliderPath_Color    = Util.getXmlRgb(xmlConfigChild,          DEFAULT_SLIDER_PATH_COLOR);
            config.sliderPath_LineWidth= Util.getXmlFloatFloat(xmlConfigChild,   ATTR_SLIDER_PATH_LINE_WIDTH,    DEFAULT_SLIDER_PATH_LINE_WIDTH, SLIDER_PATH_LINE_WIDTH_MIN, SLIDER_PATH_LINE_WIDTH_MAX);
            config.sliderPath_Opacity  = Util.getXmlInteger(xmlConfigChild,      ATTR_SLIDER_PATH_OPACITY,       DEFAULT_SLIDER_PATH_OPACITY,    SLIDER_PATH_OPACITY_MIN,    SLIDER_PATH_OPACITY_MAX);
            break;

         case TAG_SLIDER_LOCATION_LEFT:
            
            config.sliderLocation_Left_Color   = Util.getXmlRgb(xmlConfigChild,  DEFAULT_SLIDER_LOCATION_LEFT_COLOR);
            break;
            
         case TAG_SLIDER_LOCATION_RIGHT:
            
            config.sliderLocation_Right_Color   = Util.getXmlRgb(xmlConfigChild, DEFAULT_SLIDER_LOCATION_RIGHT_COLOR);
            break;
         }
         
// SET_FORMATTING_ON
      }
   }

   private static void parse_210_MarkerConfig(final XMLMemento xmlConfig, final MarkerConfig config) {

// SET_FORMATTING_OFF
      
      config.id                     = Util.getXmlString(xmlConfig,      ATTR_ID,                      Long.toString(System.nanoTime()));
      config.name                   = Util.getXmlString(xmlConfig,      ATTR_CONFIG_NAME,             UI.EMPTY_STRING);


      config.isMarkerClustered      = Util.getXmlBoolean(xmlConfig,     ATTR_IS_MARKER_CLUSTERED,     true);
      config.clusterAlgorithm       = Util.getXmlEnum(xmlConfig,        ATTR_CLUSTER_ALGORITHM,       ClusterAlgorithm.FirstMarker_Distance);
      config.clusterGrid_Size       = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_GRID_SIZE,       DEFAULT_CLUSTER_GRID_SIZE,       CLUSTER_GRID_MIN_SIZE,     CLUSTER_GRID_MAX_SIZE);
      config.clusterOrientation     = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_ORIENTATION,     Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD);
      
      config.clusterFill_Opacity    = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_FILL_OPACITY,    DEFAULT_CLUSTER_FILL_OPACITY);
      config.clusterOutline_Opacity = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_OUTLINE_OPACITY, DEFAULT_CLUSTER_OUTLINE_OPACITY);
      config.clusterOutline_Size    = Util.getXmlFloatFloat(xmlConfig,  ATTR_CLUSTER_OUTLINE_SIZE,    DEFAULT_CLUSTER_OUTLINE_SIZE,    CLUSTER_OUTLINE_SIZE_MIN,  CLUSTER_OUTLINE_SIZE_MAX);
      config.clusterSymbol_Size     = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_SYMBOL_SIZE,     DEFAULT_CLUSTER_SYMBOL_SIZE,     CLUSTER_SYMBOL_SIZE_MIN,   CLUSTER_SYMBOL_SIZE_MAX);
      config.clusterSymbol_Weight   = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_SYMBOL_WEIGHT,   DEFAULT_CLUSTER_SYMBOL_WEIGHT,   CLUSTER_SYMBOL_WEIGHT_MIN, CLUSTER_SYMBOL_WEIGHT_MAX);

      config.isShowMapBookmark      = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_MAP_BOOKMARK,    true);
      config.isShowTourMarker       = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_TOUR_MARKER,     true);
      config.isShowPhoto            = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_PHOTO,           true);
      config.isShowPhotoTitle       = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_PHOTO_TITLE,     true);
      config.markerOrientation      = Util.getXmlInteger(xmlConfig,     ATTR_MARKER_ORIENTATION,      Map25ConfigManager.SYMBOL_ORIENTATION_BILLBOARD);
      
      config.markerFill_Opacity     = Util.getXmlInteger(xmlConfig,     ATTR_MARKER_FILL_OPACITY,     Map25ConfigManager.DEFAULT_MARKER_FILL_OPACITY);
      config.markerOutline_Opacity  = Util.getXmlInteger(xmlConfig,     ATTR_MARKER_OUTLINE_OPACITY,  Map25ConfigManager.DEFAULT_MARKER_FILL_OPACITY);
      config.markerOutline_Size     = Util.getXmlFloatFloat(xmlConfig,  ATTR_MARKER_OUTLINE_SIZE,     DEFAULT_MARKER_OUTLINE_SIZE,     MARKER_OUTLINE_SIZE_MIN,   MARKER_OUTLINE_SIZE_MAX);
      config.markerSymbol_Size      = Util.getXmlInteger(xmlConfig,     ATTR_MARKER_SYMBOL_SIZE,      DEFAULT_MARKER_SYMBOL_SIZE,      MARKER_SYMBOL_SIZE_MIN,    MARKER_SYMBOL_SIZE_MAX);

// SET_FORMATTING_ON

      /*
       * Each color has a seaparate tag
       */
      for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

         final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
         final String configTag = xmlConfigChild.getType();

         switch (configTag) {

         case TAG_CLUSTER_OUTLINE_COLOR:
            config.clusterOutline_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_OUTLINE_COLOR);
            break;
         case TAG_CLUSTER_FILL_COLOR:
            config.clusterFill_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_FILL_COLOR);
            break;

         case TAG_MARKER_OUTLINE_COLOR:
            config.markerOutline_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_MARKER_OUTLINE_COLOR);
            break;
         case TAG_MARKER_FILL_COLOR:
            config.markerFill_Color = Util.getXmlRgb(xmlConfigChild, DEFAULT_MARKER_FILL_COLOR);
            break;
         }
      }
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
         restoreState_20_Tracks(xmlRoot, _allTrackConfigs);
         restoreState_30_Markers(xmlRoot, _allMarkerConfigs);

         // ensure config is created
         if (_allTrackConfigs.size() == 0) {
            createDefaults_Tracks();
         }

         if (_allMarkerConfigs.size() == 0) {
            createDefaults_Markers();
         }

         _activeTrackConfig = getConfig_Track();
         _activeMarkerConfig = getConfig_Marker();

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   public static void resetActiveMarkerConfiguration() {

      // do not replace the name
      final String oldName = _activeMarkerConfig.name;

      final int activeMarkerConfigIndex = getActiveMarkerConfigIndex();

      // remove old config
      _allMarkerConfigs.remove(_activeMarkerConfig);

      // create new config
      final int configIndex = activeMarkerConfigIndex + 1;
      final MarkerConfig newConfig = createDefaults_Markers_One(configIndex);
      newConfig.name = oldName;

      // update model
      _activeMarkerConfig = newConfig;
      _allMarkerConfigs.add(activeMarkerConfigIndex, newConfig);
   }

   public static void resetAllMarkerConfigurations() {

      createDefaults_Markers();

      _activeMarkerConfig = _allMarkerConfigs.get(0);
   }

   private static void restoreState_10_Options(final XMLMemento xmlRoot) {

      if (xmlRoot == null) {
         return;
      }

      final XMLMemento xmlOptions = (XMLMemento) xmlRoot.getChild(TAG_OPTIONS);

      if (xmlOptions == null) {
         return;
      }

      isAnimateLocation = Util.getXmlBoolean(xmlOptions, ATTR_IS_ANIMATE_LOCATION, IS_ANIMATE_LOCATION_DEFAULT);

      animationEasingType = (Easing.Type) Util.getXmlEnum(
            xmlOptions,
            ATTR_ANIMATION_EASING_TYPE,
            ANIMATION_EASING_TYPE_DEFAULT);

      animationTime = Util.getXmlFloatFloat(
            xmlOptions,
            ATTR_ANIMATION_TIME,
            LOCATION_ANIMATION_TIME_DEFAULT,
            LOCATION_ANIMATION_TIME_MIN,
            LOCATION_ANIMATION_TIME_MAX);

      // other
      useDraggedKeyboardNavigation = Util.getXmlBoolean(
            xmlOptions,
            ATTR_USE_DRAGGED_KEY_NAVIGATION,
            USE_DRAGGED_KEY_NAVIGATION_DEFAULT);
   }

   /**
    * Parse configuration xml.
    * 
    * @param xmlRoot
    * @param allTourTrackConfig
    */
   private static void restoreState_20_Tracks(final XMLMemento xmlRoot,
                                              final ArrayList<Map25TrackConfig> allTourTrackConfig) {

      if (xmlRoot == null) {
         return;
      }

      final XMLMemento xmlTourTracks = (XMLMemento) xmlRoot.getChild(TAG_TOUR_TRACKS);

      if (xmlTourTracks == null) {
         return;
      }

      _fromXml_ActiveTrackConfigId = Util.getXmlString(xmlTourTracks, ATTR_ACTIVE_CONFIG_ID, null);

      for (final IMemento mementoConfig : xmlTourTracks.getChildren()) {

         final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

         try {

            final String xmlConfigType = xmlConfig.getType();

            if (xmlConfigType.equals(TAG_TRACK)) {

               // <Track>

               final Map25TrackConfig trackConfig = new Map25TrackConfig();

               parse_050_TrackConfig(xmlConfig, trackConfig);

               allTourTrackConfig.add(trackConfig);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlConfig), e);
         }
      }
   }

   private static void restoreState_30_Markers(final XMLMemento xmlRoot,
                                               final ArrayList<MarkerConfig> allMarkerConfigs) {

      if (xmlRoot == null) {
         return;
      }

      final XMLMemento xmlMarkers = (XMLMemento) xmlRoot.getChild(TAG_TOUR_MARKERS);

      if (xmlMarkers == null) {
         return;
      }

      _fromXml_ActiveMarkerConfigId = Util.getXmlString(xmlMarkers, ATTR_ACTIVE_CONFIG_ID, null);

      for (final IMemento mementoConfig : xmlMarkers.getChildren()) {

         final XMLMemento xmlConfig = (XMLMemento) mementoConfig;

         try {

            final String xmlConfigType = xmlConfig.getType();

            if (xmlConfigType.equals(TAG_MARKER)) {

               // <Track>

               final MarkerConfig markerConfig = new MarkerConfig();

               parse_210_MarkerConfig(xmlConfig, markerConfig);

               allMarkerConfigs.add(markerConfig);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlConfig), e);
         }
      }
   }

   public static void saveState() {

      if (_activeTrackConfig == null) {

         // this can happen when not yet used

         return;
      }

      final XMLMemento xmlRoot = create_Root();

      saveState_Tracks(xmlRoot);
      saveState_Markers(xmlRoot);
      saveState_Options(xmlRoot);

      Util.writeXml(xmlRoot, getConfigXmlFile());
   }

   /**
    * Markers
    */
   private static void saveState_Markers(final XMLMemento xmlRoot) {

      final IMemento xmlMarkers = xmlRoot.createChild(TAG_TOUR_MARKERS);
      {
         xmlMarkers.putString(ATTR_ACTIVE_CONFIG_ID, _activeMarkerConfig.id);

         for (final MarkerConfig config : _allMarkerConfigs) {
            createXml_FromMarkerConfig(config, xmlMarkers);
         }
      }
   }

   private static void saveState_Options(final XMLMemento xmlRoot) {

      final IMemento xmlOptions = xmlRoot.createChild(TAG_OPTIONS);
      {
         xmlOptions.putBoolean(ATTR_IS_ANIMATE_LOCATION, isAnimateLocation);
         xmlOptions.putFloat(ATTR_ANIMATION_TIME, animationTime);

         Util.setXmlEnum(xmlOptions, ATTR_ANIMATION_EASING_TYPE, animationEasingType);

         xmlOptions.putBoolean(ATTR_USE_DRAGGED_KEY_NAVIGATION, useDraggedKeyboardNavigation);
      }
   }

   /**
    * Tracks
    */
   private static void saveState_Tracks(final XMLMemento xmlRoot) {

      final IMemento xmlTourTracks = xmlRoot.createChild(TAG_TOUR_TRACKS);
      {
         xmlTourTracks.putString(ATTR_ACTIVE_CONFIG_ID, _activeTrackConfig.id);

         for (final Map25TrackConfig config : _allTrackConfigs) {
            createXml_FromTrackConfig(config, xmlTourTracks);
         }
      }
   }

   public static void setActiveMarkerConfig(final MarkerConfig newConfig) {

      _activeMarkerConfig = newConfig;
   }

   public static void setActiveTrackConfig(final Map25TrackConfig newConfig) {

      _activeTrackConfig = newConfig;
   }

   /**
    * Set map location with or without animation
    * 
    * @param map
    * @param boundingBox
    * @param locationAnimationTime
    */
   public static void setMapLocation(final Map map, final BoundingBox boundingBox, int locationAnimationTime) {

      final Animator animator = map.animator();

      // zero will not move the map, set 1 ms
      if (locationAnimationTime == 0 || isAnimateLocation == false) {
         locationAnimationTime = 1;
      }

      animator.cancel();
      animator.animateTo(//
            locationAnimationTime,
            boundingBox,
            Easing.Type.SINE_INOUT,
            Animator.ANIM_MOVE | Animator.ANIM_SCALE);
   }

   public static void setMapLocation(final Map map, final MapPosition mapPosition) {

      if (ThreadUtils.isMainThread()) {

         setMapLocation_InMapThread(map, mapPosition);

      } else {

         map.post(new Runnable() {
            @Override
            public void run() {

               setMapLocation_InMapThread(map, mapPosition);
            }
         });
      }

   }

   private static void setMapLocation_InMapThread(final Map map, final MapPosition mapPosition) {

//      final boolean isAnimation = animationTime != 0 && isAnimateLocation;
//
//      if (isAnimation) {
//
//         final Animator animator = map.animator();
//
//         animator.cancel();
//         animator.animateTo(
//               (long) (animationTime * 1000),
//               mapPosition,
//               animationEasingType);
//      } else {
//
//      map.setMapPosition(mapPosition);
      map.setMapPosition(mapPosition);
//      }

      map.updateMap(true);
   }
}
