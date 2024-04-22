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
import net.tourbook.map25.Map25ConfigManager;

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
    * Tour marker, map bookmarks, photos
    */
   private static final String TAG_TOUR_MARKERS = "TourMarkers"; //$NON-NLS-1$
   private static final String TAG_MARKER       = "Marker";      //$NON-NLS-1$
   //
   // marker
   private static final String ATTR_IS_SHOW_TOUR_MARKER    = "isShowTourMarker";     //$NON-NLS-1$
   //
   private static final String ATTR_MARKER_FILL_OPACITY    = "markerFillOpacity";    //$NON-NLS-1$
   private static final String ATTR_MARKER_OUTLINE_OPACITY = "markerOutlineOpacity"; //$NON-NLS-1$
   private static final String ATTR_MARKER_OUTLINE_SIZE    = "markerOutlineSize";    //$NON-NLS-1$
   private static final String ATTR_MARKER_SYMBOL_SIZE     = "markerSymbolSize";     //$NON-NLS-1$
   //
   private static final String TAG_MARKER_FILL_COLOR       = "MarkerFillColor";      //$NON-NLS-1$
   private static final String TAG_MARKER_OUTLINE_COLOR    = "MarkerOutlineColor";   //$NON-NLS-1$
   //
   // cluster
   private static final String ATTR_IS_MARKER_CLUSTERED           = "isMarkerClustered";          //$NON-NLS-1$
   private static final String ATTR_IS_CLUSTER_SYMBOL_ANTIALIASED = "isClusterSymbolAntialiased"; //$NON-NLS-1$
   private static final String ATTR_IS_CLUSTER_TEXT_ANTIALIASED   = "isClusterTextAntialiaed";    //$NON-NLS-1$
   private static final String ATTR_IS_CLUSTER_FILL_OPACITY       = "isClusterFillOpacity";       //$NON-NLS-1$
   //
   private static final String ATTR_CLUSTER_GRID_SIZE             = "clusterGridSize";            //$NON-NLS-1$
   private static final String ATTR_CLUSTER_OUTLINE_WIDTH         = "clusterOutlineWidth";        //$NON-NLS-1$
   private static final String ATTR_CLUSTER_SYMBOL_SIZE           = "clusterSymbolSize";          //$NON-NLS-1$
   //
   private static final String TAG_CLUSTER_FILL_COLOR             = "ClusterFillColor";           //$NON-NLS-1$
   private static final String TAG_CLUSTER_OUTLINE_COLOR          = "ClusterOutlineColor";        //$NON-NLS-1$
   //
   /*
    * Defaults, min/max
    */
   // symbol
   public static final int   DEFAULT_MARKER_PHOTO_SIZE   = 160;
   public static final int   DEFAULT_MARKER_SYMBOL_SIZE  = 20;
   public static final float DEFAULT_MARKER_OUTLINE_SIZE = 2.0f;
   public static final float MARKER_OUTLINE_SIZE_MIN     = 0;
   public static final float MARKER_OUTLINE_SIZE_MAX     = 100;
   public static final int   MARKER_SYMBOL_SIZE_MIN      = 10;
   public static final int   MARKER_SYMBOL_SIZE_MAX      = 200;
   //
   // CLUSTER
   public static final boolean DEFAULT_IS_FILL_CLUSTER_SYMBOL = true;
   public static final int     DEFAULT_CLUSTER_GRID_SIZE      = 60;
   public static final int     DEFAULT_CLUSTER_SYMBOL_SIZE    = 10;
   public static final int     DEFAULT_CLUSTER_OUTLINE_WIDTH  = 0;
   public static final int     CLUSTER_GRID_MIN_SIZE          = 1;
   public static final int     CLUSTER_GRID_MAX_SIZE          = 10000;
   public static final int     CLUSTER_OUTLINE_WIDTH_MIN      = 0;
   public static final int     CLUSTER_OUTLINE_WIDTH_MAX      = 10;
   public static final int     CLUSTER_SYMBOL_SIZE_MIN        = 5;
   public static final int     CLUSTER_SYMBOL_SIZE_MAX        = 200;
   //
   // colors
   public static final RGB   DEFAULT_CLUSTER_FILL_RGB       = new RGB(0xFC, 0x67, 0x00);
   public static final RGB   DEFAULT_CLUSTER_OUTLINE_RGB    = new RGB(0xff, 0xff, 0xff);
   public static final float DEFAULT_CLUSTER_OUTLINE_SIZE   = 2.0f;
   public static final RGB   DEFAULT_MARKER_FILL_RGB        = new RGB(0xFF, 0xFF, 0x00);
   public static final int   DEFAULT_MARKER_FILL_OPACITY    = 200;                      // 80%;
   public static final RGB   DEFAULT_MARKER_OUTLINE_RGB     = new RGB(0, 0, 0);
   public static final int   DEFAULT_MARKER_OUTLINE_OPACITY = 200;                      // 80%;
   //
   //
   /**
    * Contains all configurations which are loaded from a xml file.
    */
   private static final ArrayList<Map2MarkerConfig> _allMarkerConfigs = new ArrayList<>();
   private static Map2MarkerConfig                  _activeMarkerConfig;
   //
   private static String                            _fromXml_ActiveMarkerConfigId;

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
    *
    * @return
    */
   private static Map2MarkerConfig createDefaults_Markers_One(final int configIndex) {

      final Map2MarkerConfig config = new Map2MarkerConfig();

      final RGB fgBlack = new RGB(0, 0, 0);
      final RGB fgWhite = new RGB(0xff, 0xff, 0xff);

      final RGB bg1 = new RGB(0x00, 0xA0, 0xED);
      final RGB bg2 = new RGB(0xC6, 0x00, 0xA2);
      final RGB bg3 = new RGB(0x00, 0xC4, 0x2C);
      final RGB bg4 = new RGB(0xFF, 0xC9, 0x00);
      final RGB bg5 = new RGB(0xFF, 0x00, 0x62);

// SET_FORMATTING_OFF

      config.markerOutline_RGB    = fgBlack;
      config.markerFill_RGB       = fgWhite;
      config.isShowTourMarker       = true;

      switch (configIndex) {

      case 1:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_1;
         config.clusterOutline_RGB   = fgBlack;
         config.clusterFill_RGB      = bg1;
         config.markerOutline_RGB    = fgBlack;
         config.markerFill_RGB       = bg5;
         break;

      case 2:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_2;
         config.clusterOutline_RGB   = fgWhite;
         config.clusterFill_RGB      = bg1;
         break;

      case 3:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_3;
         config.clusterOutline_RGB   = fgBlack;
         config.clusterFill_RGB      = bg2;
         break;

      case 4:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_4;
         config.clusterOutline_RGB   = fgWhite;
         config.clusterFill_RGB      = bg2;
         break;

      case 5:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_5;
         config.clusterOutline_RGB   = fgBlack;
         config.clusterFill_RGB      = bg3;
         break;

      case 6:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_6;
         config.clusterOutline_RGB   = fgWhite;
         config.clusterFill_RGB      = bg3;
         break;

      case 7:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_7;
         config.clusterOutline_RGB   = fgBlack;
         config.clusterFill_RGB      = bg4;
         break;

      case 8:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_8;
         config.clusterOutline_RGB   = fgWhite;
         config.clusterFill_RGB      = bg4;
         break;

      case 9:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_9;
         config.clusterOutline_RGB   = fgBlack;
         config.clusterFill_RGB      = bg5;
         break;

      case 10:
         config.name                   = config.defaultId = CONFIG_DEFAULT_ID_10;
         config.clusterOutline_RGB   = fgWhite;
         config.clusterFill_RGB      = bg5;
         break;
      }

// SET_FORMATTING_ON

      config.setupColors();

      return config;
   }

   private static void createXml_FromMarkerConfig(final Map2MarkerConfig config, final IMemento xmlMarkers) {

// SET_FORMATTING_OFF

      // <Marker>
      final IMemento xmlConfig = xmlMarkers.createChild(TAG_MARKER);
      {
         xmlConfig.putString(ATTR_ID,           config.id);
         xmlConfig.putString(ATTR_CONFIG_NAME,  config.name);

         /*
          * Marker
          */
         xmlConfig.putBoolean(      ATTR_IS_SHOW_TOUR_MARKER,           config.isShowTourMarker);

         xmlConfig.putInteger(      ATTR_MARKER_FILL_OPACITY,           config.markerFill_Opacity);
         xmlConfig.putInteger(      ATTR_MARKER_OUTLINE_OPACITY,        config.markerOutline_Opacity);
         xmlConfig.putFloat(        ATTR_MARKER_OUTLINE_SIZE,           config.markerOutline_Size);
         xmlConfig.putInteger(      ATTR_MARKER_SYMBOL_SIZE,            config.markerSymbol_Size);
         Util.setXmlRgb(xmlConfig,  TAG_MARKER_OUTLINE_COLOR,           config.markerOutline_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_MARKER_FILL_COLOR,              config.markerFill_RGB);

         /*
          * Cluster
          */
         xmlConfig.putBoolean(      ATTR_IS_MARKER_CLUSTERED,           config.isMarkerClustered);
         xmlConfig.putBoolean(      ATTR_IS_CLUSTER_SYMBOL_ANTIALIASED, config.isClusterSymbolAntialiased);
         xmlConfig.putBoolean(      ATTR_IS_CLUSTER_TEXT_ANTIALIASED,   config.isClusterTextAntialiased);

         xmlConfig.putBoolean(      ATTR_IS_CLUSTER_FILL_OPACITY,       config.isFillClusterSymbol);
         xmlConfig.putInteger(      ATTR_CLUSTER_GRID_SIZE,             config.clusterGridSize);
         xmlConfig.putInteger(      ATTR_CLUSTER_OUTLINE_WIDTH,         config.clusterOutline_Width);
         xmlConfig.putInteger(      ATTR_CLUSTER_SYMBOL_SIZE,           config.clusterSymbol_Size);
         Util.setXmlRgb(xmlConfig,  TAG_CLUSTER_FILL_COLOR,             config.clusterFill_RGB);
         Util.setXmlRgb(xmlConfig,  TAG_CLUSTER_OUTLINE_COLOR,          config.clusterOutline_RGB);
      }

// SET_FORMATTING_ON

   }

   public static Map2MarkerConfig getActiveMarkerConfig() {

      if (_activeMarkerConfig == null) {
         readConfigFromXml();
      }

      return _activeMarkerConfig;
   }

   /**
    * @return Returns the index for the {@link #_activeMarkerConfig}, the index starts with 0.
    */
   public static int getActiveMarkerConfigIndex() {

      final Map2MarkerConfig activeConfig = getActiveMarkerConfig();

      for (int configIndex = 0; configIndex < _allMarkerConfigs.size(); configIndex++) {

         final Map2MarkerConfig config = _allMarkerConfigs.get(configIndex);

         if (config.equals(activeConfig)) {
            return configIndex;
         }
      }

      // this case should not happen but ensure that a correct config is set

      setActiveMarkerConfig(_allMarkerConfigs.get(0));

      return 0;
   }

   public static ArrayList<Map2MarkerConfig> getAllMarkerConfigs() {

      // ensure configs are loaded
      getActiveMarkerConfig();

      return _allMarkerConfigs;
   }

   private static Map2MarkerConfig getConfig_Marker() {

      Map2MarkerConfig activeConfig = null;

      if (_fromXml_ActiveMarkerConfigId != null) {

         // ensure config id belongs to a config which is available

         for (final Map2MarkerConfig config : _allMarkerConfigs) {

            if (config.id.equals(_fromXml_ActiveMarkerConfigId)) {

               activeConfig = config;
               break;
            }
         }
      }

      if (activeConfig == null) {

         // this case should not happen, create a config

         StatusUtil.logInfo("Created default config for marker properties");//$NON-NLS-1$

         createDefaults_Markers();

         activeConfig = _allMarkerConfigs.get(0);
      }

      return activeConfig;
   }

   private static File getConfigXmlFile() {

      final File layerFile = _stateLocation.append(CONFIG_FILE_NAME).toFile();

      return layerFile;
   }

   private static void parse_210_MarkerConfig(final XMLMemento xmlConfig,
                                              final Map2MarkerConfig config) {

// SET_FORMATTING_OFF

      config.id                           = Util.getXmlString(xmlConfig,      ATTR_ID,                            Long.toString(System.nanoTime()));
      config.name                         = Util.getXmlString(xmlConfig,      ATTR_CONFIG_NAME,                   UI.EMPTY_STRING);


      config.isShowTourMarker             = Util.getXmlBoolean(xmlConfig,     ATTR_IS_SHOW_TOUR_MARKER,           true);
      config.markerFill_Opacity           = Util.getXmlInteger(xmlConfig,     ATTR_MARKER_FILL_OPACITY,           Map25ConfigManager.DEFAULT_MARKER_FILL_OPACITY);
      config.markerOutline_Opacity        = Util.getXmlInteger(xmlConfig,     ATTR_MARKER_OUTLINE_OPACITY,        Map25ConfigManager.DEFAULT_MARKER_FILL_OPACITY);
      config.markerOutline_Size           = Util.getXmlFloatFloat(xmlConfig,  ATTR_MARKER_OUTLINE_SIZE,           DEFAULT_MARKER_OUTLINE_SIZE,     MARKER_OUTLINE_SIZE_MIN,   MARKER_OUTLINE_SIZE_MAX);
      config.markerSymbol_Size            = Util.getXmlInteger(xmlConfig,     ATTR_MARKER_SYMBOL_SIZE,            DEFAULT_MARKER_SYMBOL_SIZE,      MARKER_SYMBOL_SIZE_MIN,    MARKER_SYMBOL_SIZE_MAX);

      config.isMarkerClustered            = Util.getXmlBoolean(xmlConfig,     ATTR_IS_MARKER_CLUSTERED,           true);
      config.isClusterSymbolAntialiased   = Util.getXmlBoolean(xmlConfig,     ATTR_IS_CLUSTER_SYMBOL_ANTIALIASED, true);
      config.isClusterTextAntialiased     = Util.getXmlBoolean(xmlConfig,     ATTR_IS_CLUSTER_TEXT_ANTIALIASED,   true);
      config.isFillClusterSymbol          = Util.getXmlBoolean(xmlConfig,     ATTR_IS_CLUSTER_FILL_OPACITY,       DEFAULT_IS_FILL_CLUSTER_SYMBOL);
      config.clusterGridSize              = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_GRID_SIZE,             DEFAULT_CLUSTER_GRID_SIZE,       CLUSTER_GRID_MIN_SIZE,     CLUSTER_GRID_MAX_SIZE);
      config.clusterOutline_Width         = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_OUTLINE_WIDTH,         DEFAULT_CLUSTER_OUTLINE_WIDTH,   CLUSTER_OUTLINE_WIDTH_MIN, CLUSTER_OUTLINE_WIDTH_MAX);
      config.clusterSymbol_Size           = Util.getXmlInteger(xmlConfig,     ATTR_CLUSTER_SYMBOL_SIZE,           DEFAULT_CLUSTER_SYMBOL_SIZE,     CLUSTER_SYMBOL_SIZE_MIN,   CLUSTER_SYMBOL_SIZE_MAX);

// SET_FORMATTING_ON

      /*
       * Each color is in it's own tag
       */
      for (final IMemento mementoConfigChild : xmlConfig.getChildren()) {

         final XMLMemento xmlConfigChild = (XMLMemento) mementoConfigChild;
         final String configTag = xmlConfigChild.getType();

         switch (configTag) {

         case TAG_CLUSTER_OUTLINE_COLOR:
            config.clusterOutline_RGB = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_OUTLINE_RGB);
            break;

         case TAG_CLUSTER_FILL_COLOR:
            config.clusterFill_RGB = Util.getXmlRgb(xmlConfigChild, DEFAULT_CLUSTER_FILL_RGB);
            break;

         case TAG_MARKER_OUTLINE_COLOR:
            config.markerOutline_RGB = Util.getXmlRgb(xmlConfigChild, DEFAULT_MARKER_OUTLINE_RGB);
            break;

         case TAG_MARKER_FILL_COLOR:
            config.markerFill_RGB = Util.getXmlRgb(xmlConfigChild, DEFAULT_MARKER_FILL_RGB);
            break;
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
         restoreState_30_Markers(xmlRoot, _allMarkerConfigs);

         // ensure config is created
         if (_allMarkerConfigs.isEmpty()) {
            createDefaults_Markers();
         }

         setActiveMarkerConfig(getConfig_Marker());

      } catch (final Exception e) {
         StatusUtil.log(e);
      } finally {
         Util.close(reader);
      }
   }

   public static void resetActiveMarkerConfiguration() {

      // do not replace the name
      final String oldName = _activeMarkerConfig.name;

      final int activeConfigIndex = getActiveMarkerConfigIndex();

      // remove old config
      _allMarkerConfigs.remove(_activeMarkerConfig);

      // create new config
      final int configID = activeConfigIndex + 1;
      final Map2MarkerConfig newConfig = createDefaults_Markers_One(configID);
      newConfig.name = oldName;

      // update model
      setActiveMarkerConfig(newConfig);
      _allMarkerConfigs.add(activeConfigIndex, newConfig);
   }

   public static void resetAllMarkerConfigurations() {

      createDefaults_Markers();

      setActiveMarkerConfig(_allMarkerConfigs.get(0));
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

   private static void restoreState_30_Markers(final XMLMemento xmlRoot,
                                               final ArrayList<Map2MarkerConfig> allMarkerConfigs) {

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

               // <Marker>

               final Map2MarkerConfig markerConfig = new Map2MarkerConfig();

               parse_210_MarkerConfig(xmlConfig, markerConfig);

               allMarkerConfigs.add(markerConfig);
            }

         } catch (final Exception e) {
            StatusUtil.log(Util.dumpMemento(xmlConfig), e);
         }
      }
   }

   public static void saveState() {

      if (_activeMarkerConfig == null) {

         // this can happen when not yet used

         return;
      }

      final XMLMemento xmlRoot = create_Root();

      saveState_Markers(xmlRoot);

      Util.writeXml(xmlRoot, getConfigXmlFile());
   }

   /**
    * Markers
    */
   private static void saveState_Markers(final XMLMemento xmlRoot) {

      final IMemento xmlMarkers = xmlRoot.createChild(TAG_TOUR_MARKERS);
      {
         xmlMarkers.putString(ATTR_ACTIVE_CONFIG_ID, _activeMarkerConfig.id);

         for (final Map2MarkerConfig config : _allMarkerConfigs) {
            createXml_FromMarkerConfig(config, xmlMarkers);
         }
      }
   }

   public static void setActiveMarkerConfig(final Map2MarkerConfig newConfig) {

      _activeMarkerConfig = newConfig;
   }

}
