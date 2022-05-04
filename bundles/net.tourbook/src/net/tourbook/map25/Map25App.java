/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
 * Copyright (C) 2018, 2021 Thomas Theussing
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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.SharedLibraryLoader;

import java.awt.Canvas;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25TileSource.Builder;
import net.tourbook.map25.OkHttpEngineMT.OkHttpFactoryMT;
import net.tourbook.map25.layer.labeling.LabelLayerMT;
import net.tourbook.map25.layer.marker.MapMarker;
import net.tourbook.map25.layer.marker.MarkerConfig;
import net.tourbook.map25.layer.marker.MarkerLayer;
import net.tourbook.map25.layer.marker.MarkerLayer.OnItemGestureListener;
import net.tourbook.map25.layer.marker.MarkerMode;
import net.tourbook.map25.layer.marker.MarkerRenderer;
import net.tourbook.map25.layer.marker.MarkerShape;
import net.tourbook.map25.layer.marker.MarkerToolkit;
import net.tourbook.map25.layer.marker.PhotoToolkit;
import net.tourbook.map25.layer.tourtrack.SliderLocation_Layer;
import net.tourbook.map25.layer.tourtrack.SliderPath_Layer;
import net.tourbook.map25.layer.tourtrack.TourLayer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Display;
import org.oscim.awt.AwtGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.DateTime;
import org.oscim.backend.DateTimeAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.event.Event;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GestureHandlerImpl;
import org.oscim.gdx.LwjglGL20;
import org.oscim.gdx.MotionHandler;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.buildings.S3DBLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map.UpdateListener;
import org.oscim.map.ViewController;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ThemeLoader;
import org.oscim.theme.VtmThemes;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.TileSource.OpenResult;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.bitmap.BitmapTileSource;
import org.oscim.tiling.source.bitmap.DefaultSources;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.tiling.source.mvt.MapilionMvtTileSource;
import org.oscim.utils.Parameters;

import okhttp3.Cache;

public class Map25App extends GdxMap implements OnItemGestureListener, ItemizedLayer.OnItemGestureListener<MarkerInterface> {

   public static DebugMode         debugMode                         = DebugMode.OFF;                          // before releasing, set this to OFF

   /**
    * When <code>true</code> then <b>net.tourbook.ext.vtm</b> plugin is used, when
    * <code>false</code> then <b>vtm-parent</b> plugin for map25 development is used.
    */
   private static final boolean    IS_USING_VTM_PRODUCTION_PLUGIN    = true;                                   // before releasing, set this TRUE

   private static final String     STATE_MAP_POS_X                   = "STATE_MAP_POS_X";                      //$NON-NLS-1$
   private static final String     STATE_MAP_POS_Y                   = "STATE_MAP_POS_Y";                      //$NON-NLS-1$
   private static final String     STATE_MAP_POS_ZOOM_LEVEL          = "STATE_MAP_POS_ZOOM_LEVEL";             //$NON-NLS-1$
   private static final String     STATE_MAP_POS_BEARING             = "STATE_MAP_POS_BEARING";                //$NON-NLS-1$
   private static final String     STATE_MAP_POS_SCALE               = "STATE_MAP_POS_SCALE";                  //$NON-NLS-1$
   private static final String     STATE_MAP_POS_TILT                = "STATE_MAP_POS_TILT";                   //$NON-NLS-1$
   private static final String     STATE_SELECTED_MAP25_PROVIDER_ID  = "STATE_SELECTED_MAP25_PROVIDER_ID";     //$NON-NLS-1$

   private static final String     STATE_SUFFIX_MAP_CURRENT_POSITION = "MapCurrentPosition";                   //$NON-NLS-1$
   static final String             STATE_SUFFIX_MAP_DEFAULT_POSITION = "MapDefaultPosition";                   //$NON-NLS-1$

   public static final String      THEME_STYLE_ALL                   = "theme-style-all";                      //$NON-NLS-1$

   private static IDialogSettings  _state;

   private static Map25View        _map25View;
   private static LwjglApplication _lwjglApp;

   private Map25Provider           _selectedMapProvider;
   private String                  _mapDefaultLanguage               = Locale.getDefault().toString();

   private BitmapTileSource        _hillshadingSource;
   private BitmapTileSource        _satelliteSource;

   /**
    * Offline map provider (MP) file path
    */
   private String                  _offlineMP_MapFilePath            = UI.EMPTY_STRING;
   private String                  _offlineMP_Theme_FilePath;
   private String                  _offlineMP_Theme_StyleID;
   private int                     _numOfflineMapFiles               = 0;

   private String                  _mp_key                           = "80d7bc63-94fe-416f-a63f-7173f81a484c"; //$NON-NLS-1$

   /**
    * The opacity can be set in the layer but not read. This will keep the state of the hillshading
    * opacity.
    */
   private int                     _layer_HillShading_Opacity;

   private S3DBLayer               _layer_S3DB_Building;                                                       // S3DB = Simple 3D Building
   private OsmTileLayerMT          _layer_BaseMap;
   private BuildingLayer           _layer_Building;
   private BitmapTileLayer         _layer_HillShading;
   private LabelLayerMT            _layer_Label;
   private MarkerLayer             _layer_Marker;
   private ItemizedLayer           _layer_Photo;
   private BitmapTileLayer         _layer_Satellite;
   private MapScaleBarLayer        _layer_ScaleBar;
   private SliderLocation_Layer    _layer_SliderLocation;
   private SliderPath_Layer        _layer_SliderPath;
   private TileGridLayerMT         _layer_TileInfo;
   private TourLayer               _layer_Tour;

   private OkHttpFactoryMT         _httpFactory;

   private long                    _lastRenderTime;
   private String                  _last_mf_themeFilePath            = "uninitialized";                        //$NON-NLS-1$
   private String                  _last_mf_theme_styleID            = UI.EMPTY_STRING;
   private Boolean                 _last_isOfflineMap                = true;
   private String                  _last_mf_mapFilePath              = "uninitialized";                        //$NON-NLS-1$
   private Boolean                 _last_mf_IsThemeFromFile;

   private float                   _offline_TextScale                = 0.75f;
   private float                   _offline_UserScale                = 2.50f;
   private float                   _online_TextScale                 = 0.50f;
   private float                   _online_UserScale                 = 2.0f;

   private ItemizedLayer           _layer_MapBookmark;
   private MarkerToolkit           _markertoolkit;
   private MarkerMode              _markerMode                       = MarkerMode.NORMAL;                      // MarkerToolkit.modeDemo or MarkerToolkit.modeNormal

   /*
    * Photos
    */
   private PhotoToolkit              _photoToolkit     = new PhotoToolkit(this);

   private boolean                   _isShowPhoto      = true;
   private boolean                   _isShowPhotoTitle = true;
   private boolean                   _isPhotoScaled    = false;
   private int                       _photoSize;

   /**
    * Is <code>true</code> when a tour marker is hit.
    */
   private boolean                   _isMapItemHit;

   /**
    * Is <code>true</code> when maps is a mapsforgemap.
    */
   private boolean                   _isOfflineMap     = true;

   protected XmlRenderThemeStyleMenu _renderThemeStyleMenu;

   public Map25App(final IDialogSettings state) {

      _state = state;
   }

   public static Map25App createMap(final Map25View map25View, final IDialogSettings state, final Canvas canvas) {

      init();

      _map25View = map25View;
      _state = state;

      final Map25App mapApp = new Map25App(state);

      _lwjglApp = new LwjglApplication(mapApp, getConfig(), canvas);

      return mapApp;
   }

   public static void debugPrint(final String debugText) {

      if (debugMode == DebugMode.ON) {
         System.out.println(UI.timeStamp() + debugText);
         //System.out.println(UI.timeStamp() + " map25: " + debugText);//$NON-NLS-1$
      }
   }

   private static LwjglApplicationConfiguration getConfig() {

      LwjglApplicationConfiguration.disableAudio = true;
      final LwjglApplicationConfiguration appConfig = new LwjglApplicationConfiguration();

      appConfig.title = Map25App.class.getSimpleName();
      appConfig.width = 1200;
      appConfig.height = 1000;
      appConfig.stencil = 8;
      appConfig.samples = 2;
      appConfig.foregroundFPS = 30;
      appConfig.backgroundFPS = 10;

      appConfig.forceExit = false;

      // this setting seems not to work for 4k display
//    appConfig.useHDPI = true;

      // reduce CPU cycles
      appConfig.pauseWhenBackground = true;
      appConfig.backgroundFPS = 3;

      return appConfig;
   }

   public static void init() {

      // load native library
      new SharedLibraryLoader().load("vtm-jni"); //$NON-NLS-1$

      // init canvas
      AwtGraphics.init();

      if (IS_USING_VTM_PRODUCTION_PLUGIN) {

         GdxAssets.init("assets/"); //$NON-NLS-1$

      } else {

         /**
          * <p>
          * "Bundle-ClassPath: vtm/, ..." from "MANIFEST.MF" in "vtm-parent" plugin is prepended to
          * the path "resources/assets/", "build.properties" contains
          *
          * <pre>
          * bin.includes = ...
          *                vtm/resources/
          * </pre>
          * <p>
          * It took me a while to fix this path
          */
         GdxAssets.init("resources/assets/"); //$NON-NLS-1$
      }

      GLAdapter.init(new LwjglGL20());

      GLAdapter.GDX_DESKTOP_QUIRKS = true;

      DateTimeAdapter.init(new DateTime());
   }

   /**
    * Checks if a file is available for reading
    *
    * @param FilePath
    * @return Returns the absolut file path as string or <code>null</code> when not available
    */
   public String checkFile(final String FilePath) {

      final File file = new File(FilePath);
      if (!file.exists()) {

         // throw new IllegalArgumentException("file does not exist: " + file);

         return null;

      } else if (!file.isFile()) {

         // throw new IllegalArgumentException("not a file: " + file);

         return null;

      } else if (!file.canRead()) {

         // throw new IllegalArgumentException("cannot read file: " + file);

         return null;
      }

      return file.getAbsolutePath();
   }

   /**
    * Checks if a given file is a valid mapsforge file
    *
    * @param file2check
    * @return true, when file is ok
    */
   public Boolean checkMapFile(final File file2check) {

      Boolean result = false;

      final MapFileTileSource mapFileSource = new MapFileTileSource();
      mapFileSource.setMapFile(file2check.getAbsolutePath());

      final OpenResult mOpenResult = mapFileSource.open();
      mapFileSource.close();

      result = mOpenResult.isSuccess();

      if (!mOpenResult.isSuccess()) {
         StatusUtil.logError(" map25: " + "### checkMapFile: not adding: " + file2check.getAbsolutePath() + " " + mOpenResult.getErrorMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      }

      return result;
   }

   @Override
   public void create() {

      super.create();

      /**
       * Overwrite input handler, using own GdxMap.create() method didn't work :-(
       */
      final InputMultiplexer mux = new InputMultiplexer();

      if (!Parameters.MAP_EVENT_LAYER2) {

         mGestureDetector = new GestureDetector(new GestureHandlerImpl(mMap));
         mux.addProcessor(mGestureDetector);
      }

      mux.addProcessor(new InputHandlerMT(this));
      mux.addProcessor(new MotionHandler(mMap));

      Gdx.input.setInputProcessor(mux);

      mMap.events.bind(new UpdateListener() {
         @Override
         public void onMapEvent(final Event e, final MapPosition mapPosition) {

            _map25View.fireSyncMapEvent(mapPosition, 0);
         }
      });
   }

   /**
    * Layer: Scale bar
    */
   private MapScaleBarLayer createLayer_ScaleBar() {

      final DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap, 1f);

      mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.SINGLE);
//		mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH);

      mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
//		mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);

      mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT);

      final MapScaleBarLayer layer = new MapScaleBarLayer(mMap, mapScaleBar);
      layer.setEnabled(true);

      final BitmapRenderer renderer = layer.getRenderer();
      renderer.setPosition(GLViewport.Position.BOTTOM_RIGHT);
      renderer.setOffset(5, 0);

      return layer;
   }

   @Override
   public void createLayers() {

      _selectedMapProvider = restoreState_MapProvider();
      _map25View.updateUI_SelectedMapProvider(_selectedMapProvider);
      _httpFactory = new OkHttpEngineMT.OkHttpFactoryMT();

      if (_selectedMapProvider.tileEncoding != TileEncoding.MF) {

         // online map

         _isOfflineMap = false;

         final TileSource tileSource = _selectedMapProvider.tileEncoding == TileEncoding.VTM

               ? createTileSource(_selectedMapProvider, _httpFactory)
               : createTileSource_Maplilion(_selectedMapProvider, _httpFactory);

         setupMap_Online(_selectedMapProvider, tileSource);

      } else {

         // offline map

         _isOfflineMap = true;

         _offlineMP_MapFilePath = _selectedMapProvider.offline_MapFilepath;
         if (!checkMapFile(new File(_offlineMP_MapFilePath))) {
            throw new IllegalArgumentException("cannot read mapfile: " + _offlineMP_MapFilePath); //$NON-NLS-1$
         }

         final MultiMapFileTileSource tileSource = getAllOfflineMapFiles(_offlineMP_MapFilePath);
         if (_numOfflineMapFiles == 0) {
            throw new IllegalArgumentException("Cannot read (any) mapfile: " + _offlineMP_MapFilePath); //$NON-NLS-1$
         }

         _offlineMP_Theme_FilePath = checkFile(_selectedMapProvider.offline_ThemeFilepath); //check theme path, null when not found
         _offlineMP_Theme_StyleID = _selectedMapProvider.offline_ThemeStyle;

         setupMap_Offline(_selectedMapProvider, tileSource); //multi map file

         loadTheme(_offlineMP_Theme_StyleID);
      }

      setupMap_Layers();

      _last_isOfflineMap = _isOfflineMap;
      _last_mf_themeFilePath = _offlineMP_Theme_FilePath;
      _last_mf_theme_styleID = _offlineMP_Theme_StyleID;
      _last_mf_mapFilePath = _offlineMP_MapFilePath;
      _last_mf_IsThemeFromFile = _selectedMapProvider.offline_IsThemeFromFile;

      /**
       * Map Viewport
       */
      final ViewController mapViewport = mMap.viewport();

      // extend default tilt
      mapViewport.setMaxTilt((float) MercatorProjection.LATITUDE_MAX);
      mapViewport.setMinScale(2);

      updateUI_MarkerLayer();

      restoreState();

      // update actions in UI thread, run this AFTER the layers are created
      Display.getDefault().asyncExec(() -> _map25View.restoreState());
   }

   private UrlTileSource createTileSource(final Map25Provider mapProvider, final OkHttpFactoryMT httpFactory) {

      final Builder<?> map25Builder = Map25TileSource
            .builder(mapProvider)
            .url(mapProvider.online_url)
            .tilePath(mapProvider.online_TilePath)
            .httpFactory(httpFactory);

      final String apiKey = mapProvider.online_ApiKey;
      if (apiKey != null && apiKey.trim().length() > 0) {
         map25Builder.apiKey(apiKey);
      }

      return map25Builder.build();
   }

   private MapilionMvtTileSource createTileSource_Maplilion(final Map25Provider mapProvider, final OkHttpFactoryMT httpFactory) {

      MapilionMvtTileSource tileSource;

      if (mapProvider.online_ApiKey == null || mapProvider.online_ApiKey.trim().length() == 0) {

         tileSource = MapilionMvtTileSource.builder()
               .apiKey(_mp_key)
               .httpFactory(httpFactory)
               .build();

      } else {

         tileSource = MapilionMvtTileSource.builder()
               .apiKey(mapProvider.online_ApiKey.trim())
               .httpFactory(httpFactory)
               .build();
      }

      return tileSource;
   }

   @Override
   public void dispose() {

      // stop loading tiles
      _layer_BaseMap.getManager().clearJobs();

      saveState();

      super.dispose();
   }

   /**
    * @param requestedMapFilePathName
    * @return Returns all map files which are available in the map file folder
    */
   public MultiMapFileTileSource getAllOfflineMapFiles(final String requestedMapFilePathName) {

      /*
       * Get all map files from the map file folder
       */
      final File requestedMapFile = new File(requestedMapFilePathName);
      final File mapFileFolder = new File(requestedMapFile.getParent());
      final File[] allMapFiles = mapFileFolder.listFiles(new FilenameFilter() {
         @Override
         public boolean accept(final File directory, final String name) {
            return name.toLowerCase().endsWith(".map"); //$NON-NLS-1$
         }
      });

      final MultiMapFileTileSource multiMapFileTileSource = new MultiMapFileTileSource(); //DataPolicy.RETURN_ALL);
      final MapFileTileSource tileSourceOfflinePrimary = new MapFileTileSource();

      _numOfflineMapFiles = 0;

      if (checkMapFile(requestedMapFile)) {

         // adding primary map first

         tileSourceOfflinePrimary.setMapFile(requestedMapFile.getAbsolutePath());
         tileSourceOfflinePrimary.setPreferredLanguage(_mapDefaultLanguage);
         multiMapFileTileSource.add(tileSourceOfflinePrimary);

         _numOfflineMapFiles += 1;
      }

      for (final File mapFile : allMapFiles) {

         if (checkMapFile(mapFile)) {

            //add all mapfiles except the primary map, which is already added
            if (!mapFile.getAbsolutePath().equalsIgnoreCase(requestedMapFilePathName)) {

               final MapFileTileSource tileSourceOffline = new MapFileTileSource();
               tileSourceOffline.setMapFile(mapFile.getAbsolutePath());
               tileSourceOffline.setPreferredLanguage(_mapDefaultLanguage);

               multiMapFileTileSource.add(tileSourceOffline);

               _numOfflineMapFiles += 1;
            }
         }
      }

      return multiMapFileTileSource;
   }

   public boolean getAndReset_IsMapItemHit() {

//    System.out.println(
//          (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//                + ("\tgetAndReset_IsMapItemHit:" + _isMapItemHit));
//
//    final boolean isMapItemHit = _isMapItemHit;
//
//    _isMapItemHit = false;
//
//    return isMapItemHit;

      return false;
   }

   public OsmTileLayerMT getLayer_BaseMap() {
      return _layer_BaseMap;
   }

   public BuildingLayer getLayer_Building() {
      return _layer_Building;
   }

   public BitmapTileLayer getLayer_HillShading() {
      return _layer_HillShading;
   }

   public int getLayer_HillShading_Opacity() {
      return _layer_HillShading_Opacity;
   }

   public LabelLayerMT getLayer_Label() {
      return _layer_Label;
   }

   public ItemizedLayer getLayer_MapBookmark() {
      return _layer_MapBookmark;
   }

   public ItemizedLayer getLayer_Photo() {
      return _layer_Photo;
   }

   public S3DBLayer getLayer_S3DB() {
      return _layer_S3DB_Building;
   }

   public BitmapTileLayer getLayer_Satellite() {
      return _layer_Satellite;
   }

   public MapScaleBarLayer getLayer_ScaleBar() {
      return _layer_ScaleBar;
   }

   public SliderLocation_Layer getLayer_SliderLocation() {
      return _layer_SliderLocation;
   }

   public SliderPath_Layer getLayer_SliderPath() {
      return _layer_SliderPath;
   }

   public TileGridLayerMT getLayer_TileInfo() {
      return _layer_TileInfo;
   }

   public TourLayer getLayer_Tour() {
      return _layer_Tour;
   }

   public MarkerLayer getLayer_TourMarker() {
      return _layer_Marker;
   }

   Map25View getMap25View() {
      return _map25View;
   }

   public int getPhoto_Size() {
      return _photoSize;
   }

   public PhotoToolkit getPhotoToolkit() {
      return _photoToolkit;
   }

   public Map25Provider getSelectedMapProvider() {
      return _selectedMapProvider;
   }

   /**
    * @return Returns map position from the state
    */
   MapPosition getStateMapPosition(final String suffixName) {

      final String stateSuffixName = '_' + suffixName;

      final MapPosition mapPosition = new MapPosition();

      mapPosition.x = Util.getStateDouble(_state, STATE_MAP_POS_X + stateSuffixName, 0.5);
      mapPosition.y = Util.getStateDouble(_state, STATE_MAP_POS_Y + stateSuffixName, 0.5);

      mapPosition.bearing = Util.getStateFloat(_state, STATE_MAP_POS_BEARING + stateSuffixName, 0);
      mapPosition.tilt = Util.getStateFloat(_state, STATE_MAP_POS_TILT + stateSuffixName, 0);

      mapPosition.scale = Util.getStateDouble(_state, STATE_MAP_POS_SCALE + stateSuffixName, 1);
      mapPosition.zoomLevel = Util.getStateInt(_state, STATE_MAP_POS_ZOOM_LEVEL + stateSuffixName, 1);

      return mapPosition;
   }

   private ThemeFile getTheme(final Map25Provider mapProvider) {

      switch (mapProvider.tileEncoding) {
      case MVT:
         return VtmThemes.MAPZEN;

      // Open Science Map
      case VTM:
      default:
         //return VtmThemes.DEFAULT;
         return mapProvider.vtmTheme;
      }
   }

   @Override
   protected void initGLAdapter(final GLVersion arg0) {}

   public boolean isPhoto_Scaled() {
      return _isPhotoScaled;
   }

   public boolean isPhoto_ShowTitle() {
      return _isShowPhotoTitle;
   }

   boolean isPhoto_Visible() {
      return _isShowPhoto;
   }

   private void loadTheme(final String styleId) {

      if (!_isOfflineMap) {

         // NOT mapsforge

         mMap.clearMap();
         mMap.updateMap();

      } else {

         // is mapsforge map

         if (_selectedMapProvider.offline_IsThemeFromFile) {

            // external theme

            mMap.setTheme(new ExternalRenderTheme(_offlineMP_Theme_FilePath, new XmlRenderThemeMenuCallback() {

               @Override
               public Set<String> getCategories(final XmlRenderThemeStyleMenu renderThemeStyleMenu) {

                  final String style = styleId != null ? styleId : renderThemeStyleMenu.getDefaultValue();
                  final XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);

                  if (THEME_STYLE_ALL.equals(styleId)) {

                     return null;

                  } else if (renderThemeStyleLayer == null) {

                     System.err.println("####### loadtheme:  Invalid style \"" + style + "\" so i show all styles"); //$NON-NLS-1$ //$NON-NLS-2$

                     return null;
                  }

                  final Set<String> categories = renderThemeStyleLayer.getCategories();

                  for (final XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
                     if (overlay.isEnabled()) {
                        categories.addAll(overlay.getCategories());
                     }
                  }

                  return categories;
               }
            }));

         } else {

            // internal theme

            if (_selectedMapProvider.vtmTheme != null
                  && _selectedMapProvider.vtmTheme != VtmThemes.MAPZEN
                  && _selectedMapProvider.vtmTheme != VtmThemes.OPENMAPTILES) {

               mMap.setTheme(_selectedMapProvider.vtmTheme);

            } else {

               // when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead

               mMap.setTheme(VtmThemes.DEFAULT);
            }
         }

         mMap.updateMap();
      }
   }

   @Override
   public boolean onItemLongPress(final int index, final MapMarker item) {
      return false;
   }

   /**
    * longpress on a mapbookmark
    * this method is moved to net.tourbook.map25.layer.marker.MarkerToolkit !!
    *
    * @param index
    * @param MarkerItem
    * @return true, when clicked
    */
   @Override
   public boolean onItemLongPress(final int index, final MarkerInterface mi) {

      final MarkerItem markerItem = (MarkerItem) mi;
      System.out.println("Marker tap " + markerItem.getTitle()); //$NON-NLS-1$
      System.out.println(
            (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") // //$NON-NLS-1$ //$NON-NLS-2$
                  + ("\tonItemLongPress (Tourmarker)") //$NON-NLS-1$
                  + ("\tindex:" + index) //$NON-NLS-1$
                  + ("\t_isMapItemHit:" + _isMapItemHit + " -> true") //$NON-NLS-1$ //$NON-NLS-2$
      //
      );
//
      _isMapItemHit = true;
//
// return true;

      return false;
   }

   /**
    * clicking on a tourmarker
    *
    * @param index
    * @param MapMarker
    * @return true, when clicked
    */
   @Override
   public boolean onItemSingleTapUp(final int index, final MapMarker item) {

//      debugPrint(" map25: " + //$NON-NLS-1$
//            (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
//            + ("\tonItemSingleTapUp") //$NON-NLS-1$
//            + ("\tTourmarker") //$NON-NLS-1$
//            + ("\tTitle:" + item.title) //$NON-NLS-1$
//            + ("\tindex:" + index) //$NON-NLS-1$
//            + ("\t_isMapItemHit:" + _isMapItemHit + " -> true") //$NON-NLS-1$ //$NON-NLS-2$
//      //Pref_Map25_Encoding_Mapsforge
//      );

      _isMapItemHit = true;

// return true;
      return false;
   }

   /**
    * clicking on a mapbookmark
    * this method is moved to net.tourbook.map25.layer.marker.MarkerToolkit !!
    *
    * @param index
    * @param MarkerItem
    * @return true, when clicked
    */
   @Override
   public boolean onItemSingleTapUp(final int index, final MarkerInterface mi) {
      return false;
   }

   /**
    * is called from SlideOutMap25_MarkerOptions.java
    * updating the components
    */
   public void onModifyMarkerConfig() {

      updateUI_MarkerLayer();
      updateUI_MapBookmarkLayer();

      // photos can be clustered which is currently set in the marker config
      updateUI_PhotoLayer();

      mMap.render();
   }

   /**
    * when switching from offline to online or Vice versa all layers must be removed first
    * this is done here, after that all layers must be added again. but that is a different story.
    */
   private void removeLayers() {
      //saveState();
      for (int n = mMap.layers().size() - 1; n > 0; n--) {
         mMap.layers().remove(n);
      }
   }

   @Override
   public void render() {

      final long renderTime = System.currentTimeMillis();
      if (renderTime > _lastRenderTime + 1000) {

         final Map25DebugView vtmDebugView = Map25ProviderManager.getMap25DebugView();
         if (vtmDebugView != null) {

            _lastRenderTime = renderTime;

            final Cache httpCache = OkHttpEngineMT.getHttpCache();

            vtmDebugView.updateUI(mMap, httpCache);
         }
      }

      super.render();
   }

   @Override
   public void resize(final int w, final int h) {

      if (h < 1) {

//	       Fix exception
         //
//	          Exception in thread "LWJGL Application" java.lang.IllegalArgumentException: top == bottom
//	             at org.oscim.renderer.GLMatrix.frustumM(GLMatrix.java:331)
//	             at org.oscim.map.ViewController.setScreenSize(ViewController.java:50)
//	             at org.oscim.gdx.GdxMap.resize(GdxMap.java:122)
//	             at net.tourbook.map.vtm.VtmMap.resize(VtmMap.java:176)

         return;
      }

      super.resize(w, h);
   }

   private void restoreState() {

      final MapPosition mapPosition = getStateMapPosition(STATE_SUFFIX_MAP_CURRENT_POSITION);
      mMap.setMapPosition(mapPosition);
   }

   private Map25Provider restoreState_MapProvider() {

      final String mpId = Util.getStateString(
            _state,
            STATE_SELECTED_MAP25_PROVIDER_ID,
            Map25ProviderManager.getDefaultMapProvider().getId());

      return Map25ProviderManager.getMapProvider(mpId);
   }

   private void saveState() {

      _state.put(STATE_SELECTED_MAP25_PROVIDER_ID, _selectedMapProvider.getId());

      saveState_MapPosition(STATE_SUFFIX_MAP_CURRENT_POSITION);
   }

   private void saveState_MapPosition(final String suffixName) {

      final String stateSuffixName = '_' + suffixName;

      final MapPosition mapPosition = mMap.getMapPosition();

// SET_FORMATTING_OFF

      _state.put(STATE_MAP_POS_X          + stateSuffixName, mapPosition.x);
      _state.put(STATE_MAP_POS_Y          + stateSuffixName, mapPosition.y);
      _state.put(STATE_MAP_POS_BEARING    + stateSuffixName, mapPosition.bearing);
      _state.put(STATE_MAP_POS_SCALE      + stateSuffixName, mapPosition.scale);
      _state.put(STATE_MAP_POS_TILT       + stateSuffixName, mapPosition.tilt);
      _state.put(STATE_MAP_POS_ZOOM_LEVEL + stateSuffixName, mapPosition.zoomLevel);

// SET_FORMATTING_ON
   }

   public void setLayer_HillShading_Opacity(final int layer_HillShading_Opacity) {
      _layer_HillShading_Opacity = layer_HillShading_Opacity;
   }

   public void setMapProvider(final Map25Provider mapProvider) {

      _selectedMapProvider = mapProvider;

      boolean isOnlineOfflineStatusHasChanged = false;

      _offlineMP_MapFilePath = mapProvider.offline_MapFilepath;

      //if NOT mapsforge map
      if (!_offlineMP_MapFilePath.equals(_last_mf_mapFilePath)
            || mapProvider.isOfflineMap != _last_isOfflineMap) {

         //only reloading layers when neccercary

         isOnlineOfflineStatusHasChanged = true;

         removeLayers();
      }

      if (mapProvider.tileEncoding != TileEncoding.MF) {

         // is NOT a mapsforge map

         _isOfflineMap = false;

         CanvasAdapter.textScale = _online_TextScale;
         CanvasAdapter.userScale = _online_UserScale;

         if (mapProvider.tileEncoding == TileEncoding.VTM) {

            final UrlTileSource tileSource = createTileSource(mapProvider, _httpFactory);

            _layer_BaseMap.setTileSource(tileSource);
            _layer_BaseMap.setTheme(ThemeLoader.load(VtmThemes.DEFAULT)); //if active, key 1-5 nor working, if not active "ERROR VectorTileLoader - no theme is set"

            if (isOnlineOfflineStatusHasChanged) {
               setupMap_Online(mapProvider, tileSource);
            }

         } else {

            final MapilionMvtTileSource tileSource = createTileSource_Maplilion(mapProvider, _httpFactory);

            _layer_BaseMap.setTileSource(tileSource);
            _layer_BaseMap.setTheme(ThemeLoader.load(VtmThemes.OPENMAPTILES));

            if (isOnlineOfflineStatusHasChanged) {
               setupMap_Online(mapProvider, tileSource);
            }
         }

         if (isOnlineOfflineStatusHasChanged) {

            setupMap_Layers();

            /**
             * Map Viewport
             */
            final ViewController mapViewport = mMap.viewport();
            // extend default tilt
            mapViewport.setMaxTilt((float) MercatorProjection.LATITUDE_MAX);
            mapViewport.setMinScale(2);

            updateUI_MarkerLayer();
            restoreState();

            // update actions in UI thread, run this AFTER the layers are created
            Display.getDefault().asyncExec(() -> _map25View.restoreState());
         }

         if (mapProvider.vtmTheme != null
               && mapProvider.vtmTheme != VtmThemes.MAPZEN) {

            mMap.setTheme(mapProvider.vtmTheme);

         } else {

            // when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
            mMap.setTheme(VtmThemes.DEFAULT);
         }

         _offlineMP_Theme_FilePath = UI.EMPTY_STRING; // so if mf is next themefile is parsed

      } else {

         // it is a mapsforge map

         _isOfflineMap = true;

         CanvasAdapter.textScale = _offline_TextScale;
         CanvasAdapter.userScale = _offline_UserScale;

         _offlineMP_MapFilePath = mapProvider.offline_MapFilepath;

         if (!checkMapFile(new File(_offlineMP_MapFilePath))) {
            StatusUtil.showStatus(String.format(
                  "Cannot read map file \"%s\" in map provider \"%s\"", //$NON-NLS-1$
                  _offlineMP_MapFilePath,
                  mapProvider.name));

            throw new IllegalArgumentException("############# setMapProvider: cannot read mapfile: " + _offlineMP_MapFilePath); //$NON-NLS-1$

         } else {

            StatusUtil.logError(" map25: " + "############# setMapProvider: Map Path: " + _offlineMP_MapFilePath); //$NON-NLS-1$ //$NON-NLS-2$
         }

         /**
          * When switching subthemes buildings disapear and
          * apears again when switching also the mapprovider
          * codeblock mustbe outside of the if statement otherwise all themes are allways on
          */

         _offlineMP_MapFilePath = checkFile(mapProvider.offline_MapFilepath);

         final MultiMapFileTileSource tileSource = getAllOfflineMapFiles(_offlineMP_MapFilePath);
         if (_numOfflineMapFiles == 0) {
            throw new IllegalArgumentException("cannot read (any) mapfile: " + mapProvider.offline_MapFilepath); //$NON-NLS-1$
         }

         tileSource.setPreferredLanguage(_mapDefaultLanguage);
         _layer_BaseMap.setTileSource(tileSource);

         if (isOnlineOfflineStatusHasChanged) {

            // when this code inside this if staements, subthemes are not working and always on

            /**
             * final MapFileTileSource tileSource = new MapFileTileSource();
             * tileSource.setMapFile(_mf_mapFilePath);
             * tileSource.setPreferredLanguage(_mf_prefered_language);
             * _layer_BaseMap.setTileSource(tileSource);
             */
            setupMap_Offline(mapProvider, tileSource);
            setupMap_Layers();

            updateUI_MapBookmarkLayer();
            updateUI_PhotoLayer();

            /**
             * Map Viewport
             */
            final ViewController mapViewport = mMap.viewport();

            // extend default tilt
            mapViewport.setMaxTilt((float) MercatorProjection.LATITUDE_MAX);
            mapViewport.setMinScale(2);
         }

         // update actions in UI thread, run this AFTER the layers are created
         Display.getDefault().asyncExec(() -> _map25View.restoreState());

         // i wish i could use loadTheme instead of this Block:
         if (mapProvider.offline_IsThemeFromFile) {

            // external theme

            _offlineMP_Theme_FilePath = checkFile(mapProvider.offline_ThemeFilepath);
            _offlineMP_Theme_StyleID = mapProvider.offline_ThemeStyle;

            if (_offlineMP_Theme_FilePath == null) {

               mMap.setTheme(VtmThemes.DEFAULT);

            } else {

               if (!_offlineMP_Theme_FilePath.equals(_last_mf_themeFilePath)
                     || !_offlineMP_Theme_StyleID.equals(_last_mf_theme_styleID)

                     //only parsing when different file
                     || mapProvider.offline_IsThemeFromFile != _last_mf_IsThemeFromFile) {

                  final IRenderTheme offline_IRenderTheme = ThemeLoader.load(_offlineMP_Theme_FilePath);
                  _layer_BaseMap.setTheme(offline_IRenderTheme);

                  // when starting with onlinemaps and switching to mf, osmarender is used ??? when uncommented it ok
                  loadTheme(mapProvider.offline_ThemeStyle);

               } else {

                  StatusUtil.logError(" map25: " + "############# setMapProvider: mapprovider has the same theme file and style"); //$NON-NLS-1$ //$NON-NLS-2$
               }
            }

         } else {

            // internal theme

            if (mapProvider.vtmTheme != null
                  && mapProvider.vtmTheme != VtmThemes.MAPZEN
                  && mapProvider.vtmTheme != VtmThemes.OPENMAPTILES) {

               mMap.setTheme(mapProvider.vtmTheme);

            } else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead

               mMap.setTheme(VtmThemes.DEFAULT);
            }
         }

         //loadTheme(null);
      }

      mMap.clearMap();
      mMap.updateMap();

      _last_mf_themeFilePath = _offlineMP_Theme_FilePath;
      _last_mf_theme_styleID = _offlineMP_Theme_StyleID;
      _last_mf_mapFilePath = _offlineMP_MapFilePath;
      _last_mf_IsThemeFromFile = mapProvider.offline_IsThemeFromFile;
      _last_isOfflineMap = _isOfflineMap;
   }

   public void setPhoto_IsScaled(final boolean isPhotoScaled) {
      _isPhotoScaled = isPhotoScaled;
   }

   public void setPhoto_IsShowTitle(final boolean isShowPhotoTitle) {
      _isShowPhotoTitle = isShowPhotoTitle;
   }

   public void setPhoto_IsVisible(final boolean isShowPhoto) {
      _isShowPhoto = isShowPhoto;
   }

   public void setPhoto_Size(final int layer_Photo_Size) {
      _photoSize = layer_Photo_Size;
   }

   private void setupMap_Layers() {

      final Layers allMapLayer = mMap.layers();
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      // needs long copyright hint...
      _hillshadingSource = DefaultSources.MAPILION_HILLSHADE_2
            .httpFactory(_httpFactory)
            .apiKey(_mp_key)
            .build();

      // hillshading with 1MB RAM Cache, using existing _httpfactory with diskcache
      _layer_HillShading = new BitmapTileLayer(mMap, _hillshadingSource, 1 << 19);
      _layer_HillShading.setEnabled(false);

      // satellite maps like google earth

      _satelliteSource = BitmapTileSource.builder()
            .httpFactory(_httpFactory)
            .url("http://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/tile") //$NON-NLS-1$
            .tilePath("/{Z}/{Y}/{X}.png") //$NON-NLS-1$
            .zoomMin(1)
            .zoomMax(18)
            .build();

      _layer_Satellite = new BitmapTileLayer(mMap, _satelliteSource, 1 << 19);
      _layer_Satellite.setEnabled(false);

      // tour
      _layer_Tour = new TourLayer(mMap);
      _layer_Tour.setEnabled(false);

      // slider path
      _layer_SliderPath = new SliderPath_Layer(mMap);
      _layer_SliderPath.setEnabled(false);

      //buildings
      /*
       * here i have to investigate
       * with this code i got always good S3DB, but online buildings did not look good with:
       * "new S3DBLayer(mMap,_layer_BaseMap, true)"
       */
//    // Buildings or S3DB  Block I
      _layer_S3DB_Building = new S3DBLayer(mMap, _layer_BaseMap, true); //this is working with subtheme  switching, but no online buildings anymore
      _layer_Building = new BuildingLayer(mMap, _layer_BaseMap, true, true); // building is not working with online maps, so deactvated also the shadow

      allMapLayer.add(_layer_HillShading);
//      allMapLayer.add(_layer_Satellite);
      allMapLayer.add(_layer_Tour);
      allMapLayer.add(_layer_SliderPath);

      if (_isOfflineMap) {

         // S3DB

         _layer_S3DB_Building.setEnabled(true);
         _layer_S3DB_Building.setColored(true);

         allMapLayer.remove(_layer_Building);
         allMapLayer.add(_layer_S3DB_Building);

      } else {

         // building

         _layer_Building.setEnabled(true);
         allMapLayer.remove(_layer_S3DB_Building);
         allMapLayer.add(_layer_Building);
      }

      // label
      _layer_Label = new LabelLayerMT(mMap, _layer_BaseMap);
      _layer_Label.setEnabled(false);

      // MapBookmarks
      _markertoolkit = new MarkerToolkit(MarkerShape.STAR);
      if (config.isMarkerClustered) {
         //_layer_MapBookmark = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), _markertoolkit._markerRendererFactory, this);
         _layer_MapBookmark = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _markertoolkit.getMarkerRendererFactory(), _markertoolkit);
      } else {
         _layer_MapBookmark = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _markertoolkit.getMarkerSymbol(), _markertoolkit);
      }
      final List<MarkerInterface> pts = _markertoolkit.createBookmarksAsMapMarker(_markerMode);
      _layer_MapBookmark.addItems(pts);
      _layer_MapBookmark.setEnabled(false);

      // marker
      _layer_Marker = new MarkerLayer(mMap, this);
      _layer_Marker.setEnabled(false);

      //Photos
      if (config.isMarkerClustered) { //sharing same setting as MapBookmarks, later photolayer should get its own configuration
         _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _photoToolkit._markerRendererFactory, _photoToolkit);
      } else {
         _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _photoToolkit.getSymbol(), _photoToolkit);
      }
      //_layer_Photo.addItems(_phototoolkit._photo_pts);  //must not be done at startup, no tour is loaded yet
      _layer_Photo.setEnabled(false);

      // slider location
      _layer_SliderLocation = new SliderLocation_Layer(mMap);
      _layer_SliderLocation.setEnabled(false);

      // scale bar
      _layer_ScaleBar = createLayer_ScaleBar();

      // layercheck
      allMapLayer.toString();

      // tile info
      _layer_TileInfo = new TileGridLayerMT(mMap);
      _layer_TileInfo.setEnabled(false);

      allMapLayer.add(_layer_Label);
      allMapLayer.add(_layer_MapBookmark);
      allMapLayer.add(_layer_Marker);
      allMapLayer.add(_layer_Photo);
      allMapLayer.add(_layer_SliderLocation);
      allMapLayer.add(_layer_ScaleBar);
      allMapLayer.add(_layer_TileInfo);
   }

   /**
    * setupMap for mapsforge
    *
    * @param mapProvider
    * @param tileSource
    */
   private void setupMap_Offline(final Map25Provider mapProvider, final MultiMapFileTileSource tileSource) {

      CanvasAdapter.textScale = _offline_TextScale;
      CanvasAdapter.userScale = _offline_UserScale;

      _layer_BaseMap = new OsmTileLayerMT(mMap);
      _layer_BaseMap.setTileSource(tileSource);
      _layer_BaseMap.setTheme(ThemeLoader.load(VtmThemes.DEFAULT)); //to avoid errors

      mMap.setBaseMap(_layer_BaseMap);

      _offlineMP_MapFilePath = mapProvider.offline_MapFilepath;
      _offlineMP_Theme_FilePath = checkFile(mapProvider.offline_ThemeFilepath); //check theme path, null when not found
      _offlineMP_Theme_StyleID = mapProvider.offline_ThemeStyle;

      if (!checkMapFile(new File(_offlineMP_MapFilePath))) {
         throw new IllegalArgumentException("cannot read mapfile: " + _offlineMP_MapFilePath); //$NON-NLS-1$
      } else {

         StatusUtil.logError(" map25: " + "############# setupMap: Map Path: " + _offlineMP_MapFilePath); //$NON-NLS-1$ //$NON-NLS-2$
      }

      if (_offlineMP_Theme_FilePath == null) {

         mMap.setTheme(VtmThemes.OSMARENDER); // ThemeLoader.load(_mf_themeFilePath));

      } else {

         // because of changes in loadtheme
         final IRenderTheme offline_IRenderTheme = ThemeLoader.load(_offlineMP_Theme_FilePath);
         _layer_BaseMap.setTheme(offline_IRenderTheme);

         mMap.setTheme(ThemeLoader.load(_offlineMP_Theme_FilePath)); //neccercary?seem so
      }
   }

   /**
    * Setup map, e.g. for Mapilion online maps
    *
    * @param mapProvider
    * @param tileSource
    */
   private void setupMap_Online(final Map25Provider mapProvider, final TileSource tileSource) {

      CanvasAdapter.textScale = _online_TextScale;
      CanvasAdapter.userScale = _online_UserScale;

      _layer_BaseMap = new OsmTileLayerMT(mMap);
      _layer_BaseMap.setTileSource(tileSource);

      mMap.setBaseMap(_layer_BaseMap);
      mMap.setTheme(getTheme(mapProvider));
   }

   void stop() {

      _lwjglApp.stop();
   }

   /**
    * Update map and render next frame afterwards
    */
   public void updateMap() {

      mMap.updateMap();
   }

   /**
    * updates the mapbookmarklayer, switching between clustered and not clustered
    * settings are from MarkerConfig
    * replacing the mapbookmarkitems
    */
   public void updateUI_MapBookmarkLayer() {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final Layers allMapLayer = mMap.layers();
      final int layer_index_MapBookmarkLayer = allMapLayer.indexOf(_layer_MapBookmark);

      // only recreate MapBookmarkLayer when changed in UI
      if (config.isMarkerClustered != _markertoolkit.isMarkerClusteredLast()) {

         allMapLayer.remove(_layer_MapBookmark);

         if (config.isMarkerClustered) {

            _layer_MapBookmark = new ItemizedLayer(
                  mMap,
                  new ArrayList<MarkerInterface>(),
                  _markertoolkit.getMarkerRendererFactory(),
                  _markertoolkit);

         } else {

            _layer_MapBookmark = new ItemizedLayer(
                  mMap,
                  new ArrayList<MarkerInterface>(),
                  _markertoolkit.getMarkerSymbol(),
                  _markertoolkit);
         }

         allMapLayer.add(layer_index_MapBookmarkLayer, _layer_MapBookmark);

      } else {

         _layer_MapBookmark.removeAllItems();
      }

      final List<MarkerInterface> allBookmarkMarker = _markertoolkit.createBookmarksAsMapMarker(_markerMode);

      _layer_MapBookmark.addItems(allBookmarkMarker);
      _layer_MapBookmark.setEnabled(config.isShowMapBookmark);

      _markertoolkit.setIsMarkerClusteredLast(config.isMarkerClustered);
   }

   /**
    * @wbp.parser.entryPoint
    */
   private void updateUI_MarkerLayer() {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final boolean isShowTourMarker = config.isShowTourMarker;

      _layer_Marker.setEnabled(isShowTourMarker);

      if (isShowTourMarker) {

         final MarkerRenderer markerRenderer = (MarkerRenderer) _layer_Marker.getRenderer();

         markerRenderer.configureRenderer();
      }
   }

   /**
    * updates the photo layer, switchung between clustered to not clustered
    * settings are from MarkerConfig
    * replacing the photo Items
    * currently no GUI for selecting clustering
    */
   public void updateUI_PhotoLayer() {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      // using settings from MapBookmarks must be changed later with own config
      if (config.isMarkerClustered != _photoToolkit._isMarkerClusteredLast) {

         // only recreate PhotoLayer when changed in UI

         final Layers allMapLayer = mMap.layers();
         final int photoLayerPosition = allMapLayer.indexOf(_layer_Photo);

         allMapLayer.remove(_layer_Photo);

         //  config.isPhotoClustered
         if (config.isMarkerClustered) {
            _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _photoToolkit._markerRendererFactory, _photoToolkit);
         } else {
            _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _photoToolkit.getSymbol(), _photoToolkit);
         }

         if (photoLayerPosition == -1) {
            allMapLayer.add(_layer_Photo);
         } else {
            allMapLayer.add(photoLayerPosition, _layer_Photo);
         }

      } else {

         _layer_Photo.removeAllItems();
      }

      final List<MarkerInterface> photoItems = _photoToolkit.createPhotoItems(_map25View.getFilteredPhotos());

      _layer_Photo.addItems(photoItems);
      _layer_Photo.setEnabled(_isShowPhoto);

      //_phototoolkit._isMarkerClusteredLast = config.isPhotoClustered;
      // using settings from MapBookmarks must be changed later with own config
      _photoToolkit._isMarkerClusteredLast = config.isMarkerClustered;
   }

}
