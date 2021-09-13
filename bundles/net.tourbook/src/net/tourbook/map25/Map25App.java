/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
//import java.io.FileNotFoundException;
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
//import org.ocsim.backend
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
import org.oscim.layers.tile.TileManager;
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
//import org.oscim.theme.StreamRenderTheme;
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

   private static final String     STATE_MAP_POS_X                   = "STATE_MAP_POS_X";                  //$NON-NLS-1$
   private static final String     STATE_MAP_POS_Y                   = "STATE_MAP_POS_Y";                  //$NON-NLS-1$
   private static final String     STATE_MAP_POS_ZOOM_LEVEL          = "STATE_MAP_POS_ZOOM_LEVEL";         //$NON-NLS-1$
   private static final String     STATE_MAP_POS_BEARING             = "STATE_MAP_POS_BEARING";            //$NON-NLS-1$
   private static final String     STATE_MAP_POS_SCALE               = "STATE_MAP_POS_SCALE";              //$NON-NLS-1$
   private static final String     STATE_MAP_POS_TILT                = "STATE_MAP_POS_TILT";               //$NON-NLS-1$
   private static final String     STATE_SELECTED_MAP25_PROVIDER_ID  = "STATE_SELECTED_MAP25_PROVIDER_ID"; //$NON-NLS-1$

   private static final String     STATE_SUFFIX_MAP_CURRENT_POSITION = "MapCurrentPosition";               //$NON-NLS-1$
   static final String             STATE_SUFFIX_MAP_DEFAULT_POSITION = "MapDefaultPosition";               //$NON-NLS-1$

   public static final String      THEME_STYLE_ALL                   = "theme-style-all";                  //$NON-NLS-1$

   private static IDialogSettings  _state;

   private static String           _mf_mapFilePath                   = UI.EMPTY_STRING;
   private static String           _mf_themeFilePath                 = null;
   private static String           _mf_theme_styleID                 = null;
   private static Map25View        _map25View;
   private static LwjglApplication _lwjglApp;

   public static DebugMode         debugMode                         = DebugMode.OFF;                      // before releasing, set this to OFF
   private Boolean                 _mf_IsThemeFromFile               = null;

   private Boolean                 _firstRun                         = true;

   private String                  _mf_prefered_language             = "en";                               //$NON-NLS-1$
   private Map25Provider           _selectedMapProvider;

   private TileManager             _tileManager;

   /*
    * if i could replace "_l" against "_layer_BaseMap", everything would be easier...
    * _l = mMap.setBaseMap(tileSource); returns VectorTileLayer
    */
   private OsmTileLayerMT _layer_BaseMap; //extends extends VectorTileLayer
   //private VectorTileLayer      _layer_BaseMap;
   //private VectorTileLayer     _l;
   private BuildingLayer             _layer_Building;
   private S3DBLayer                 _layer_S3DB_Building;
   private TileSource                _hillshadingSource         = null;
   private TileSource                _satelliteSource           = null;
   private MapFileTileSource         _tileSourceOffline;

   private MultiMapFileTileSource    _tileSourceOfflineMM;
   private int                       _tileSourceOfflineMapCount = 0;

   private String                    _mp_key                    = "80d7bc63-94fe-416f-a63f-7173f81a484c"; //$NON-NLS-1$

   /**
    * The opacity can be set in the layer but not read. This will keep the state of the hillshading
    * opacity.
    */
   private int                       _layer_HillShading_Opacity;
   private int                       _layer_Photo_Size;

   private LabelLayerMT              _layer_Label;
   private MarkerLayer               _layer_Marker;
   private BitmapTileLayer           _layer_HillShading;
   private ItemizedLayer             _layer_Photo;
   private BitmapTileLayer           _layer_Satellite;
   private MapScaleBarLayer          _layer_ScaleBar;
   private SliderLocation_Layer      _layer_SliderLocation;
   private SliderPath_Layer          _layer_SliderPath;
   private TileGridLayerMT           _layer_TileInfo;
   private TourLayer                 _layer_Tour;

   private OkHttpFactoryMT           _httpFactory;

   private long                      _lastRenderTime;
   private String                    _last_mf_themeFilePath     = "uninitialized";                        //$NON-NLS-1$
   private String                    _last_mf_theme_styleID     = UI.EMPTY_STRING;
   private Boolean                   _last_is_mf_Map            = true;
   private String                    _last_mf_mapFilePath       = "uninitialized";                        //$NON-NLS-1$
   private Boolean                   _last_mf_IsThemeFromFile;

   private IRenderTheme              _mf_IRenderTheme;
   private float                     _mf_TextScale              = 0.75f;
   private float                     _online_TextScale          = 0.50f;
   private float                     _mf_UserScale              = 2.50f;
   private float                     _online_UserScale          = 2.0f;

   private ItemizedLayer             _layer_MapBookmark;
   private MarkerToolkit             _markertoolkit;
   private MarkerMode                _markerMode                = MarkerMode.NORMAL;                      // MarkerToolkit.modeDemo or MarkerToolkit.modeNormal

   private boolean                   _isPhotoClustered          = true;
   private boolean                   _isPhotoShowTitle          = true;

   private boolean                   _isPhotoShowScaled         = false;
   public PhotoToolkit               _phototoolkit;

   public List<MarkerInterface>      _selectedPhotosPts;

   /**
    * Is <code>true</code> when a tour marker is hit.
    */
   private boolean                   _isMapItemHit;

   /**
    * Is <code>true</code> when maps is a mapsforgemap.
    */
   private boolean                   _isOfflineMap              = true;

   protected XmlRenderThemeStyleMenu _renderThemeStyleMenu;

   public Map25App(final IDialogSettings state) {

      _state = state;
   }

   public static Map25App createMap(final Map25View map25View, final IDialogSettings state, final Canvas canvas) {

      init();

      _map25View = map25View;
      _state = state;

      final Map25App mapApp = new Map25App(state);

      _lwjglApp = new LwjglApplication(mapApp, getConfig(null), canvas);

      return mapApp;
   }

   public static void debugPrint(final String debugText) {

      if (debugMode == DebugMode.ON) {
         System.out.println(UI.timeStamp() + debugText);
         //System.out.println(UI.timeStamp() + " map25: " + debugText);//$NON-NLS-1$
      }
   }

   protected static LwjglApplicationConfiguration getConfig(final String title) {

      LwjglApplicationConfiguration.disableAudio = true;
      final LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

      cfg.title = title != null ? title : "vtm-gdx"; //$NON-NLS-1$
      cfg.width = 1200;
      cfg.height = 1000;
      cfg.stencil = 8;
      cfg.samples = 2;
      cfg.foregroundFPS = 30;
      cfg.backgroundFPS = 10;

      cfg.forceExit = false;

      return cfg;
   }

   public static void init() {

      // load native library
      new SharedLibraryLoader().load("vtm-jni"); //$NON-NLS-1$

      // init canvas
      AwtGraphics.init();

      GdxAssets.init("assets/"); //$NON-NLS-1$

      GLAdapter.init(new LwjglGL20());

      GLAdapter.GDX_DESKTOP_QUIRKS = true;

      DateTimeAdapter.init(new DateTime());
   }

   /**
    * checks if a file is a file
    *
    * @param FilePath
    * @return absolut file path as string
    */
   public String checkFile(final String FilePath) {

      final File file = new File(FilePath);
      if (!file.exists()) {
         debugPrint(" map25: " + "## checkFile: file not exist: " + file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
         return null;
         //throw new IllegalArgumentException("file does not exist: " + file);
      } else if (!file.isFile()) {
         debugPrint(" map25: " + "## checkFile: is not a file: " + file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
         return null;
         //throw new IllegalArgumentException("not a file: " + file);
      } else if (!file.canRead()) {
         debugPrint(" map25: " + "## checkFile: can not read file: " + file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
         return null;
         //throw new IllegalArgumentException("cannot read file: " + file);
      }
      //debugPrint(" map25: " + "############ check file:  file_path: " +  file.getAbsolutePath()); //$NON-NLS-1$
      //return file;
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

         debugPrint(" map25: " + "### checkMapFile: not adding: " + file2check.getAbsolutePath() + " " + mOpenResult.getErrorMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
            //debugPrint(" map25: " + "############### Orientation: " +  _map25View.getOrientation());  //$NON-NLS-1$
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
      _mf_prefered_language = Locale.getDefault().toString();
      _httpFactory = new OkHttpEngineMT.OkHttpFactoryMT();

      debugPrint(" map25: " + "# create Layers: prefered language:           " + _mf_prefered_language);//$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "# create Layers: Map Name:                    " + _selectedMapProvider.name); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "# create Layers: Map if mf Map     :          " + _selectedMapProvider.is_mf_Map); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "# create Layers: Map mf_MapFilepath:          " + _selectedMapProvider.mf_MapFilepath); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "# create Layers: Map mf_ThemeFilepath:        " + _selectedMapProvider.mf_ThemeFilepath); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "# create Layers: Map encoding:                " + _selectedMapProvider.tileEncoding.toString()); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "# create Layers: prefered language:           " + _mf_prefered_language); //$NON-NLS-1$ //$NON-NLS-2$

      if (_selectedMapProvider.tileEncoding != TileEncoding.MF) { // NOT mapsforge
         _isOfflineMap = false;
         if (_selectedMapProvider.tileEncoding == TileEncoding.VTM) {
            final UrlTileSource tileSource = createTileSource(_selectedMapProvider, _httpFactory);
            setupMap(_selectedMapProvider, tileSource);
         } else {
            final MapilionMvtTileSource tileSource = createMaplilionMvtTileSource(_selectedMapProvider, _httpFactory);
            setupMap(_selectedMapProvider, tileSource);
         }

         debugPrint(" map25: " + "# create Layers OSCI: step 1: " + mMap.layers().toString() + " size: " + mMap.layers().size()); // result 1 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         //tileSource.getDataSource().dispose();
         //_l = mMap.setBaseMap(tileSource);

         //loadTheme(null);
         //setupMap(_selectedMapProvider, tileSource);
         debugPrint(" map25: " + "# create Layers OSCI: step 2: " + mMap.layers().toString() + " size: " + mMap.layers().size()); // result 3 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

         debugPrint(" map25: " + "############# create Layers: is online map with theme: " + _selectedMapProvider.theme.name()); //$NON-NLS-1$ //$NON-NLS-2$
      } else { //offline maps
         _isOfflineMap = true;
         //_httpFactory = null;
         //_mf_mapFilePath = checkFile(_selectedMapProvider.mf_MapFilepath);
         _mf_mapFilePath = _selectedMapProvider.mf_MapFilepath;
         if (!checkMapFile(new File(_mf_mapFilePath))) {
            throw new IllegalArgumentException("cannot read mapfile: " + _mf_mapFilePath); //$NON-NLS-1$
         }

         final MultiMapFileTileSource tileSource = getMapFile(_mf_mapFilePath);
         if (_tileSourceOfflineMapCount == 0) {
            throw new IllegalArgumentException("cannot read (any) mapfile: " + _selectedMapProvider.mf_MapFilepath); //$NON-NLS-1$
         }
         //_l = mMap.setBaseMap(tileSource);

         debugPrint(" map25: " + "# create Layers: mf step 1: " + mMap.layers().toString() + " size: " + mMap.layers().size()); // result 1 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         // here we have only one layer, that we need for mapsource switching

         // the next block was active for single mapsource
         //final MapFileTileSource tileSource = new MapFileTileSource();
         //tileSource.setMapFile(_mf_mapFilePath);
         //tileSource.setPreferredLanguage(_mf_prefered_language);

         debugPrint(" map25: " + "# create Layers: mf step 2: " + mMap.layers().toString() + " size: " + mMap.layers().size()); // result 1 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         //_l = mMap.setBaseMap(tileSource);

         debugPrint(" map25: " + "# create Layers: mf step 3: " + mMap.layers().toString() + " size: " + mMap.layers().size()); // result ? //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

         _mf_IsThemeFromFile = _selectedMapProvider.mf_IsThemeFromFile;
         _mf_themeFilePath = checkFile(_selectedMapProvider.mf_ThemeFilepath); //check theme path, null when not found
         _mf_theme_styleID = _selectedMapProvider.mf_ThemeStyle;

         debugPrint(" map25: " + "# create Layers: is mapsforge map using : " + _mf_mapFilePath); //$NON-NLS-1$ //$NON-NLS-2$
         debugPrint(" map25: " + "# create Layers: is mapsforge theme : " + _mf_themeFilePath); //$NON-NLS-1$ //$NON-NLS-2$
         debugPrint(" map25: " + "# create Layers: is mapsforge style : " + _mf_theme_styleID); //$NON-NLS-1$ //$NON-NLS-2$

         //setupMap(_selectedMapProvider, tileSource); //single map file
         setupMap(_selectedMapProvider, tileSource); //multi map file

         loadTheme(_mf_theme_styleID);

         debugPrint(" map25: " + "# create Layers: mf step 4: " + mMap.layers().toString() + " size: " + mMap.layers().size()); // result ? //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

         debugPrint(" map25: " + "# create Layers: leaving"); //$NON-NLS-1$ //$NON-NLS-2$
      } // end mf_ maps

      setupMap_Layers();

      this._last_is_mf_Map = _isOfflineMap;
      this._last_mf_themeFilePath = _mf_themeFilePath;
      this._last_mf_theme_styleID = _mf_theme_styleID;
      this._last_mf_mapFilePath = _mf_mapFilePath;
      this._last_mf_IsThemeFromFile = _mf_IsThemeFromFile;

      /**
       * Map Viewport
       */
      final ViewController mapViewport = mMap.viewport();
      // extend default tilt
      mapViewport.setMaxTilt((float) MercatorProjection.LATITUDE_MAX);
      mapViewport.setMinScale(2);

      //setupMap(_selectedMapProvider, tileSource);
      updateUI_MarkerLayer();

      restoreState();

      // update actions in UI thread, run this AFTER the layers are created
      Display.getDefault().asyncExec(new Runnable() {
         @Override
         public void run() {
            _map25View.restoreState();
         }
      });
   } // end createLayers()

   private MapilionMvtTileSource createMaplilionMvtTileSource(final Map25Provider mapProvider, final OkHttpFactoryMT httpFactory) {
      MapilionMvtTileSource tileSource;
      if (mapProvider.online_ApiKey == null || mapProvider.online_ApiKey.trim().length() == 0) {
         //debugPrint(" map25: " + "####### createMaplilionMvtTileSource API Key is empty using internal key: " + _mp_key); //$NON-NLS-1$ //$NON-NLS-2$
         tileSource = MapilionMvtTileSource.builder()
               .apiKey(_mp_key)
               .httpFactory(httpFactory)
               .build();
      } else {
         //debugPrint(" map25: " + "####### createMaplilionMvtTileSource API Key is not empty using it: " + mapProvider.online_ApiKey.trim()); //$NON-NLS-1$ //$NON-NLS-2$
         tileSource = MapilionMvtTileSource.builder()
               .apiKey(mapProvider.online_ApiKey.trim())
               .httpFactory(httpFactory)
               .build();
      }
      return tileSource;
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

   @Override
   public void dispose() {
      // stop loading tiles
      _tileManager.clearJobs();
      saveState();
      super.dispose();
   }

   public boolean getAndReset_IsMapItemHit() {
//    System.out.println(
//          (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//                + ("\tgetAndReset_IsMapItemHit:" + _isMapItemHit));
//    // TODO remove SYSTEM.OUT.PRINTLN
//
//    final boolean isMapItemHit = _isMapItemHit;
//
//    _isMapItemHit = false;
//
//    return isMapItemHit;
      return false;
   }

   public boolean getIsPhotoClustered() {
      return _isPhotoClustered;
   }

   public boolean getIsPhotoShowScaled() {
      return _isPhotoShowScaled;
   }

   public boolean getIsPhotoShowTitle() {
      return _isPhotoShowTitle;
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

   public int getLayer_Photo_Size() {
      return _layer_Photo_Size;
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

   /**
    * get a sorted list with mapsforgemap files
    *
    * @param <MultiMapDataStore>
    * @param filename
    * @return files[]
    *         {@link http://www.avajava.com/tutorials/lessons/how-do-i-sort-an-array-of-files-according-to-their-sizes.html}
    */
   public MultiMapFileTileSource getMapFile(final String filename) {

      final File file = new File(filename);
      final File directory = new File(file.getParent());
      final File[] files = directory.listFiles(new FilenameFilter() {
         @Override
         public boolean accept(final File directory, final String name) {
            return name.toLowerCase().endsWith(".map"); //$NON-NLS-1$
         }
      });

      debugPrint(" map25: " + "#### getMapFile: basepath: " + directory); //$NON-NLS-1$ //$NON-NLS-2$

      //Arrays.sort(files, SizeFileComparator.SIZE_COMPARATOR); // sort mapsfiles size

      final MultiMapFileTileSource mMFileTileSource = new MultiMapFileTileSource(); //DataPolicy.RETURN_ALL);
      final MapFileTileSource tileSourceOfflinePrimary = new MapFileTileSource();

      if (checkMapFile(file)) {
         tileSourceOfflinePrimary.setMapFile(file.getAbsolutePath());
         tileSourceOfflinePrimary.setPreferredLanguage(_mf_prefered_language);
         mMFileTileSource.add(tileSourceOfflinePrimary); // adding primary map first
         _tileSourceOfflineMapCount += 1;
         debugPrint(" map25: " + "#### getMapFile: Adding primary map: " + file + " size: " + file.length() + " bytes)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      } else {

         debugPrint(" map25: " + "#### getMapFile: primary file missing: " + file.getAbsolutePath()); //$NON-NLS-1$ //$NON-NLS-2$
      }

      //debugPrint(" map25: " + "Adding: " + file + " size: " +  	FileUtils.byteCountToDisplaySize(file.length()) + "(" + file.length() + " bytes)");

      for (final File f : files) {
         if (checkMapFile(f)) {
            if (!f.getAbsolutePath().equalsIgnoreCase(filename)) { //add all mapfiles except the primary map, which is already added
               final MapFileTileSource tileSourceOffline = new MapFileTileSource();
               tileSourceOffline.setMapFile(f.getAbsolutePath());
               tileSourceOffline.setPreferredLanguage(_mf_prefered_language);
               mMFileTileSource.add(tileSourceOffline);
               _tileSourceOfflineMapCount += 1;
               //long size = FileUtils.sizeOf(f);
               //debugPrint(" map25: " + "Adding: " + f + " size: " +  	FileUtils.byteCountToDisplaySize(size) + "(" + size + " bytes)");
               debugPrint(" map25: " + "#### getMapFile: Adding secondary map: " + f + " size: " + f.length() + " bytes), Total Maps: " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
                     + _tileSourceOfflineMapCount);
            }
         }
      }
      return mMFileTileSource;
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
         return mapProvider.theme;
      }
   }

   @Override
   protected void initGLAdapter(final GLVersion arg0) {
      // TODO Auto-generated method stub

   }

   protected void loadTheme(final String styleId) {
      debugPrint(" map25: " + "####### loadtheme: entering styleID: " + styleId); //$NON-NLS-1$ //$NON-NLS-2$

      if (!_isOfflineMap) { // NOT mapsforge
         debugPrint(" map25: " + "####### loadtheme: is online map setting textscale " + _online_TextScale); //$NON-NLS-1$ //$NON-NLS-2$
         //CanvasAdapter.textScale = _vtm_TextScale;
         // if problems with switching themes via keyboard, maybe this block is the problem
         /*
          * if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN
          * && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
          * debugPrint(" map25: " + "############# setMapProvider: onlinemap using internal theme: "
          * + _selectedMapProvider.theme); //$NON-NLS-1$
          * mMap.setTheme((ThemeFile) _selectedMapProvider.theme);
          * } else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using
          * DEFAULT theme instead
          * debugPrint(" map25: " + "############# setMapProvider: onlinemap using internal default
          * theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
          * mMap.setTheme(VtmThemes.DEFAULT);
          * }
          */
         mMap.clearMap();
         mMap.updateMap(true);
      }

      else { //is mapsforge map

         //debugPrint(" map25: " + "####### loadtheme: is offline map setting textscale " +   _mf_TextScale); //$NON-NLS-1$
         //debugPrint(" map25: " + "####### loadtheme: is offline map IsThemeFileFromFile " +  _mf_IsThemeFromFile); //$NON-NLS-1$

         if (_mf_IsThemeFromFile) { //external theme
            debugPrint(" map25: " + "####### loadtheme: using external theme"); //$NON-NLS-1$ //$NON-NLS-2$
            mMap.setTheme(new ExternalRenderTheme(_mf_themeFilePath, new XmlRenderThemeMenuCallback() {
               @Override
               public Set<String> getCategories(final XmlRenderThemeStyleMenu renderThemeStyleMenu) {
                  final String style = styleId != null ? styleId : renderThemeStyleMenu.getDefaultValue();
                  final XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
                  if (THEME_STYLE_ALL.equals(styleId)) {
                     return null;
                  } else if (renderThemeStyleLayer == null) {
                     System.err.println("####### loadtheme:  Invalid style \"" + style + "\" so i show all styles"); //$NON-NLS-1$ //$NON-NLS-2$
                     return null;
                  } else {

                  }
                  debugPrint(" map25: " + "####### loadtheme:  selected Style: " + renderThemeStyleLayer.getTitle(_mf_prefered_language)); //$NON-NLS-1$ //$NON-NLS-2$
                  final Set<String> categories = renderThemeStyleLayer.getCategories();
                  final int n = 0;
                  final int overlaycount = renderThemeStyleLayer.getOverlays().size();
                  for (final XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
                     if (overlay.isEnabled()) {
                        categories.addAll(overlay.getCategories());
                     }
                  }
                  debugPrint(" map25: " + "####### loadtheme: leaving"); //$NON-NLS-1$ //$NON-NLS-2$
                  return categories;
               }
            }));
         } else { //internal theme
            if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN
                  && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
               debugPrint(" map25: " + "####### loadtheme: using internal theme: " + _selectedMapProvider.theme); //$NON-NLS-1$ //$NON-NLS-2$
               mMap.setTheme(_selectedMapProvider.theme);
            } else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
               debugPrint(" map25: " + "####### loadtheme: using internal default theme: " + _selectedMapProvider.theme); //$NON-NLS-1$ //$NON-NLS-2$
               mMap.setTheme(VtmThemes.DEFAULT);
            }
            _mf_IsThemeFromFile = false;
         }
         //mMap.clearMap();
         mMap.updateMap(true);

      }
      debugPrint(" map25: " + "####### loadtheme: leaving styleID: " + styleId); //$NON-NLS-1$ //$NON-NLS-2$
   }




   @Override
   public boolean onItemLongPress(final int index, final MapMarker item) {
      // TODO Auto-generated method stub
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
      // TODO remove SYSTEM.OUT.PRINTLN
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

      debugPrint(" map25: " + //$NON-NLS-1$
            (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //$NON-NLS-1$ //$NON-NLS-2$
            + ("\tonItemSingleTapUp") //$NON-NLS-1$
            + ("\tTourmarker") //$NON-NLS-1$
            + ("\tTitle:" + item.title) //$NON-NLS-1$
            + ("\tindex:" + index) //$NON-NLS-1$
            + ("\t_isMapItemHit:" + _isMapItemHit + " -> true") //$NON-NLS-1$ //$NON-NLS-2$
      //Pref_Map25_Encoding_Mapsforge
      );

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
      // TODO Auto-generated method stub
      return false;
   }

   /**
    * is called from SlideOutMap25_MarkerOptions.java
    * updating the components
    */
   public void onModifyMarkerConfig() {

      debugPrint("MapApp25: onModifyMarkerConfig size current: " + _selectedPhotosPts.size()); //$NON-NLS-1$

      updateUI_MarkerLayer();

      updateUI_MapBookmarkLayer();

      updateUI_PhotoLayer();

      mMap.render();
   }

   /**
    * when switching from offline to online or Vice versa all layers must be removed first
    * this is done here, after that all layers must be added again. but that is a different story.
    */
   private void removeLayers() {
      debugPrint(" map25: " + "### removeLayers: layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      //saveState();
      for (int n = mMap.layers().size() - 1; n > 0; n--) {
         debugPrint(" map25: " + "### removeLayers: layer " + n + "/" + mMap.layers().size() + " " + mMap.layers().get(n).toString()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
         mMap.layers().remove(n);
      }
      debugPrint(" map25: " + "### removeLayers: layers after: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
      //final MapPosition_with_MarkerPosition mapPosition2 = (MapPosition_with_MarkerPosition) mMap.getMapPosition();

      _state.put(STATE_MAP_POS_X + stateSuffixName, mapPosition.x);
      _state.put(STATE_MAP_POS_Y + stateSuffixName, mapPosition.y);
      _state.put(STATE_MAP_POS_BEARING + stateSuffixName, mapPosition.bearing);
      _state.put(STATE_MAP_POS_SCALE + stateSuffixName, mapPosition.scale);
      _state.put(STATE_MAP_POS_TILT + stateSuffixName, mapPosition.tilt);
      _state.put(STATE_MAP_POS_ZOOM_LEVEL + stateSuffixName, mapPosition.zoomLevel);
   }

   public void setIsPhotoClustered(final boolean state) {
      this._isPhotoClustered = state;
   }

   public void setIsPhotoShowScaled(final boolean state) {
      this._isPhotoShowScaled = state;
      debugPrint(" map25: " + "############# setIsPhotoShowScaled: " + state); //$NON-NLS-1$ //$NON-NLS-2$
   }

   public void setIsPhotoShowTitle(final boolean state) {
      this._isPhotoShowTitle = state;
      debugPrint(" map25: " + "############# setIsPhotoShowTitle: " + state); //$NON-NLS-1$ //$NON-NLS-2$
   }

   public void setLayer_HillShading_Opacity(final int layer_HillShading_Opacity) {
      _layer_HillShading_Opacity = layer_HillShading_Opacity;
   }

   public void setLayer_Photo_Size(final int layer_Photo_Size) {
      _layer_Photo_Size = layer_Photo_Size;
      debugPrint(" map25: " + "############# setLayer_PhotoSize to: " + layer_Photo_Size); //$NON-NLS-1$ //$NON-NLS-2$
   }

   public void setMapProvider(final Map25Provider mapProvider) {
      //saveState();  //doesnt help
      Boolean onlineOfflineStatusHasChanged = false;
      _mf_mapFilePath = mapProvider.mf_MapFilepath;
      debugPrint(" map25: " + "############# setMapProvider entering setMapProvider: _mf_mapFilePath:" + _mf_mapFilePath + " _last_mf_mapFilePath: " //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
            + _last_mf_mapFilePath);
      debugPrint(" map25: " + "############# setMapProvider layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      debugPrint(" map25: " + "############# setMapProvider entering setMapProvider"); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "############# setMapProvider layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      //if NOT mapsforge map
      if (!_mf_mapFilePath.equals(_last_mf_mapFilePath) || mapProvider.is_mf_Map != _last_is_mf_Map) { //only reloading layers when neccercary
         debugPrint(" map25: " + "############# setMapProvider switchin offline/one to: " + mapProvider.is_mf_Map); //$NON-NLS-1$ //$NON-NLS-2$
         onlineOfflineStatusHasChanged = true;
         removeLayers();
      }

      //if NOT mapsforge map
      //debugPrint(" map25: " + "############# setMapProvider MapProviderENCODING: " + mapProvider.tileEncoding); //$NON-NLS-1$
      if (mapProvider.tileEncoding != TileEncoding.MF) { // NOT mapsforge
         this._isOfflineMap = false;
         CanvasAdapter.textScale = _online_TextScale;
         CanvasAdapter.userScale = _online_UserScale;
         debugPrint(" map25: " + "############# setMapProvider: setMapProvider NOT mf Map"); //$NON-NLS-1$ //$NON-NLS-2$
         debugPrint(" map25: " + "############# setMapProvider: tileEncoding: " + mapProvider.tileEncoding); //$NON-NLS-1$ //$NON-NLS-2$
         debugPrint(" map25: " + "############# setMapProvider: API: " + mapProvider.online_ApiKey); //$NON-NLS-1$ //$NON-NLS-2$
         if (mapProvider.tileEncoding == TileEncoding.VTM) {
            final UrlTileSource tileSource = createTileSource(mapProvider, _httpFactory);
            _layer_BaseMap.setTileSource(tileSource);
            _layer_BaseMap.setTheme(ThemeLoader.load(VtmThemes.DEFAULT)); //if active, key 1-5 nor working, if not active "ERROR VectorTileLoader - no theme is set"
            if (onlineOfflineStatusHasChanged) {
               setupMap(mapProvider, tileSource);
            }
            //debugPrint(" map25: " + "############# setMapProvider: set theme to-> " + mapProvider.name); //$NON-NLS-1$
         } else {
            final MapilionMvtTileSource tileSource = createMaplilionMvtTileSource(mapProvider, _httpFactory);
            _layer_BaseMap.setTileSource(tileSource);
            _layer_BaseMap.setTheme(ThemeLoader.load(VtmThemes.OPENMAPTILES));
            if (onlineOfflineStatusHasChanged) {
               setupMap(mapProvider, tileSource);
            }
         }

//			final UrlTileSource tileSource = createTileSource(mapProvider, _httpFactory);
//			_layer_BaseMap.setTileSource(tileSource);
//			_layer_BaseMap.setTheme(ThemeLoader.load(VtmThemes.DEFAULT));  //if active, key 1-5 nor working, if not active "ERROR VectorTileLoader - no theme is set"

         if (onlineOfflineStatusHasChanged) {
            //setupMap(mapProvider, tileSource);
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
            Display.getDefault().asyncExec(new Runnable() {
               @Override
               public void run() {
                  _map25View.restoreState();
               }
            });
         }

         if (mapProvider.theme != null && mapProvider.theme != VtmThemes.MAPZEN) {
            //if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
            debugPrint(" map25: " + "############# setMapProvider: onlinemap using internal theme: " + mapProvider.theme); //$NON-NLS-1$ //$NON-NLS-2$
            mMap.setTheme(mapProvider.theme);
         } else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
            debugPrint(" map25: " + "############# setMapProvider: onlinemap MAPZEN or OPENMAPTILES is selected, using internal default theme"); //$NON-NLS-1$ //$NON-NLS-2$
            mMap.setTheme(VtmThemes.DEFAULT);
         }

//		mMap.clearMap();
//		mMap.updateMap(true);

         _mf_themeFilePath = UI.EMPTY_STRING; // so if mf is next themefile is parsed
      } else { //it mapsforge map
         this._isOfflineMap = true;
         CanvasAdapter.textScale = _mf_TextScale;
         CanvasAdapter.userScale = _mf_UserScale;
         debugPrint(" map25: " + "############# setMapProvider: setMapProvider its mf Map"); //$NON-NLS-1$ //$NON-NLS-2$

         debugPrint(" map25: " + "############# setMapProvider: setMap   to      " + mapProvider.mf_MapFilepath); //$NON-NLS-1$ //$NON-NLS-2$
         //debugPrint(" map25: " + "############# setMapProvider: setTheme to      " + mapProvider.mf_ThemeFilepath); //$NON-NLS-1$
         //debugPrint(" map25: " + "############# setMapProvider: setStyle to      " + mapProvider.mf_ThemeStyle); //$NON-NLS-1$
         debugPrint(" map25: " + "############# setMapProvider: is_mf_Map     " + mapProvider.is_mf_Map); //$NON-NLS-1$ //$NON-NLS-2$
         debugPrint(" map25: " + "############# setMapProvider: isThemeFromFile  " + mapProvider.mf_IsThemeFromFile); //$NON-NLS-1$ //$NON-NLS-2$
         //debugPrint(" map25: " + "############# setMapProvider: name             " + mapProvider.name); //$NON-NLS-1$

         _mf_mapFilePath = mapProvider.mf_MapFilepath;

         if (!checkMapFile(new File(_mf_mapFilePath))) {
            StatusUtil.showStatus(String.format(
                  "Cannot read map file \"%s\" in map provider \"%s\"", //$NON-NLS-1$
                  _mf_mapFilePath,
                  mapProvider.name));

            throw new IllegalArgumentException("############# setMapProvider: cannot read mapfile: " + _mf_mapFilePath); //$NON-NLS-1$
         } else {

            debugPrint(" map25: " + "############# setMapProvider: Map Path: " + _mf_mapFilePath); //$NON-NLS-1$ //$NON-NLS-2$
         }

         /**
          * When switching subthemes buildings disapear and
          * apears again when switching also the mapprovider
          * codeblock mustbe outside of the if statement otherwise all themes are allways on
          */

         _mf_mapFilePath = checkFile(mapProvider.mf_MapFilepath);

         final MultiMapFileTileSource tileSource = getMapFile(_mf_mapFilePath);
         if (_tileSourceOfflineMapCount == 0) {
            throw new IllegalArgumentException("cannot read (any) mapfile: " + _selectedMapProvider.mf_MapFilepath); //$NON-NLS-1$
         }
         //_l = mMap.setBaseMap(tileSource);

         tileSource.setPreferredLanguage(_mf_prefered_language);
         _layer_BaseMap.setTileSource(tileSource);

         if (onlineOfflineStatusHasChanged) {
            // when this code inside this if staements, subthemes are not working and always on
            /**
             * final MapFileTileSource tileSource = new MapFileTileSource();
             * tileSource.setMapFile(_mf_mapFilePath);
             * tileSource.setPreferredLanguage(_mf_prefered_language);
             * _layer_BaseMap.setTileSource(tileSource);
             */
            setupMap(mapProvider, tileSource);
            setupMap_Layers();
            //restoreState();

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

         //updateUI_MarkerLayer();
         //updateUI_MapBookmarkLayer();

         // update actions in UI thread, run this AFTER the layers are created
         Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
               _map25View.restoreState();
            }
         });

         //restoreState();

         _mf_IsThemeFromFile = _selectedMapProvider.mf_IsThemeFromFile;

//			_mf_themeFilePath = checkFile(_selectedMapProvider.mf_ThemeFilepath);
//			__theme_styleID = mapProvider.mf_ThemeStyle;

         // i wish i could use loadTheme instead of this Block:
         if (mapProvider.mf_IsThemeFromFile) { //external theme

            _mf_themeFilePath = checkFile(mapProvider.mf_ThemeFilepath);
            _mf_theme_styleID = mapProvider.mf_ThemeStyle;
            this._mf_IsThemeFromFile = true;

            if (_mf_themeFilePath == null) {
               debugPrint(" map25: " + "############# setMapProvider: Theme not found: " + _mf_mapFilePath + " using default DEFAULT"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
               mMap.setTheme(VtmThemes.DEFAULT); // ThemeLoader.load(_mf_themeFilePath));
            } else {
               if (!_mf_themeFilePath.equals(_last_mf_themeFilePath) || !_mf_theme_styleID.equals(_last_mf_theme_styleID)
                     || _mf_IsThemeFromFile != _last_mf_IsThemeFromFile) { //only parsing when different file
                  debugPrint(" map25: " + "############# setMapProvider: Theme loader started"); //$NON-NLS-1$ //$NON-NLS-2$
                  this._mf_IRenderTheme = ThemeLoader.load(_mf_themeFilePath);
                  debugPrint(" map25: " + "############# setMapProvider: Theme loader done, now activating..."); //$NON-NLS-1$ //$NON-NLS-2$
                  _layer_BaseMap.setTheme(_mf_IRenderTheme);
                  ////mMap.setTheme(_mf_IRenderTheme);
                  loadTheme(mapProvider.mf_ThemeStyle); //whene starting with onlinemaps and switching to mf, osmarender is used ??? when uncommented it ok
                  debugPrint(" map25: " + "############# setMapProvider: ...activaded"); //$NON-NLS-1$ //$NON-NLS-2$
                  //_mf_IsThemeFromFile = true;
               } else {

                  debugPrint(" map25: " + "############# setMapProvider: mapprovider has the same theme file and style"); //$NON-NLS-1$ //$NON-NLS-2$
               }
            }
         } else { //internal theme
            if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN
                  && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
               debugPrint(" map25: " + "############# setMapProvider: using internal theme: " + _selectedMapProvider.theme); //$NON-NLS-1$ //$NON-NLS-2$
               mMap.setTheme(_selectedMapProvider.theme);
            } else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
               debugPrint(" map25: " + "############# setMapProvider: using internal default theme: " + _selectedMapProvider.theme); //$NON-NLS-1$ //$NON-NLS-2$
               mMap.setTheme(VtmThemes.DEFAULT);
            }
            _mf_IsThemeFromFile = false;
         }

         //loadTheme(null);
      }

      mMap.clearMap();
      mMap.updateMap(true);

      //debugPrint(" map25: " + "############# setMapProvider: set language : " + _mf_prefered_language); //$NON-NLS-1$
      this._last_mf_themeFilePath = _mf_themeFilePath;
      this._last_mf_theme_styleID = _mf_theme_styleID;
      this._last_mf_mapFilePath = _mf_mapFilePath;
      this._last_mf_IsThemeFromFile = _mf_IsThemeFromFile;
      this._last_is_mf_Map = _isOfflineMap;
      _selectedMapProvider = mapProvider;
      debugPrint(" map25: " + "############# setMapProvider leaving: layers now: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
   }

   public void setPhotoSelection(final List<MarkerInterface> _photoItems) {
      debugPrint("MapApp25: setPhotoSelection size input: " + _photoItems.size()); //$NON-NLS-1$
      this._selectedPhotosPts = _photoItems;
   }

   /**
    * setupMap for Mapilion online maps
    *
    * @param mapProvider
    * @param tileSource
    */
   private void setupMap(final Map25Provider mapProvider, final MapilionMvtTileSource tileSource) {
      debugPrint(" map25: " + "############# setupMap:  Mapilion online entering"); //$NON-NLS-1$ //$NON-NLS-2$

      CanvasAdapter.textScale = _online_TextScale;
      CanvasAdapter.userScale = _online_UserScale;

      _layer_BaseMap = new OsmTileLayerMT(mMap);

      _tileManager = _layer_BaseMap.getManager();

      _layer_BaseMap.setTileSource(tileSource);

      mMap.setBaseMap(_layer_BaseMap);

      debugPrint(" map25: " + "############# setupMap Mapilion :  mMap.setTheme(getTheme(mapProvider)): " + getTheme(mapProvider)); //$NON-NLS-1$ //$NON-NLS-2$
      mMap.setTheme(getTheme(mapProvider));
      debugPrint(" map25: " + "############# setupMap mAPILION:  leaving"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * setupMap for mapsforge
    *
    * @param mapProvider
    * @param tileSource
    */
   //private void setupMap(final Map25Provider mapProvider, final MapFileTileSource tileSource) {
   private void setupMap(final Map25Provider mapProvider, final MultiMapFileTileSource tileSource) {
      debugPrint(" map25: " + "############# setupMap:  mapsforge entering"); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "############# setupMap: layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      CanvasAdapter.textScale = _mf_TextScale;
      CanvasAdapter.userScale = _mf_UserScale;

      _mf_IsThemeFromFile = mapProvider.mf_IsThemeFromFile;
      _mf_themeFilePath = checkFile(mapProvider.mf_ThemeFilepath); //check theme path, null when not found
      _mf_theme_styleID = mapProvider.mf_ThemeStyle;
      _mf_mapFilePath = mapProvider.mf_MapFilepath;

      debugPrint(" map25: " + "############# setupMap: is mapsforge map using : " + _mf_mapFilePath); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "############# setupMap: is mapsforge theme : " + _mf_themeFilePath); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "############# setupMap: is mapsforge style : " + _mf_theme_styleID); //$NON-NLS-1$ //$NON-NLS-2$
      debugPrint(" map25: " + "############# setupMap: is mapsforge mapfilepath : " + _mf_mapFilePath); //$NON-NLS-1$ //$NON-NLS-2$

      _layer_BaseMap = new OsmTileLayerMT(mMap);

      _tileManager = _layer_BaseMap.getManager();

      //_l = mMap.setBaseMap(tileSource);

      _layer_BaseMap.setTileSource(tileSource);

      _layer_BaseMap.setTheme(ThemeLoader.load(VtmThemes.DEFAULT)); //to avoid errors

      // THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);

      mMap.setBaseMap(_layer_BaseMap);

      //_mf_mapFilePath = checkFile(_selectedMapProvider.mf_MapFilepath);
      //_mf_mapFilePath = _selectedMapProvider.mf_MapFilepath;

      if (!checkMapFile(new File(_mf_mapFilePath))) {
         throw new IllegalArgumentException("cannot read mapfile: " + _mf_mapFilePath); //$NON-NLS-1$
      } else {

         debugPrint(" map25: " + "############# setupMap: Map Path: " + _mf_mapFilePath); //$NON-NLS-1$ //$NON-NLS-2$
      }

      /*
       * if (_tileSourceOfflineMapCount == 0) {
       * ;
       * //throw new IllegalArgumentException("cannot read mapfile: " +
       * _selectedMapProvider.mf_MapFilepath); //$NON-NLS-1$
       * } else {
       * //debugPrint(" map25: " + "############# setupMap: Map Path: " + _mf_mapFilePath);
       * //$NON-NLS-1$
       * }
       */

      //_mf_themeFilePath = checkFile(_selectedMapProvider.mf_ThemeFilepath);

      if (_mf_themeFilePath == null) {
         debugPrint(" map25: " + "############# setupMap:  Theme not found: " + _mf_themeFilePath + " using default OSMARENDER"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         mMap.setTheme(VtmThemes.OSMARENDER); // ThemeLoader.load(_mf_themeFilePath));
      } else {

         //_l.setTheme(ThemeLoader.load(VtmThemes.DEFAULT));  //to avoid errors
         this._mf_IRenderTheme = ThemeLoader.load(_mf_themeFilePath); // because of changes in loadtheme
         _layer_BaseMap.setTheme(_mf_IRenderTheme);
         mMap.setTheme(ThemeLoader.load(_mf_themeFilePath)); //neccercary?seem so
         ////loadTheme(mapProvider.mf_ThemeStyle); //neccercary?
      }

      debugPrint(" map25: " + "############# setupMap:  leaving"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * setupMap for online maps
    *
    * @param mapProvider
    * @param tileSource
    */
   private void setupMap(final Map25Provider mapProvider, final UrlTileSource tileSource) {
      debugPrint(" map25: " + "############# setupMap:  online entering"); //$NON-NLS-1$ //$NON-NLS-2$

      CanvasAdapter.textScale = _online_TextScale;
      CanvasAdapter.userScale = _online_UserScale;

      //_l = mMap.setBaseMap(tileSource);

      _layer_BaseMap = new OsmTileLayerMT(mMap);

      _tileManager = _layer_BaseMap.getManager();

      _layer_BaseMap.setTileSource(tileSource);

      //_l.setTheme(ThemeLoader.load(VtmThemes.DEFAULT));  //if active, key 1-5 nor working, if not active "ERROR VectorTileLoader - no theme is set"

// THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);

      mMap.setBaseMap(_layer_BaseMap);

      debugPrint(" map25: " + "############# setupMap:  mMap.setTheme(getTheme(mapProvider)): " + getTheme(mapProvider)); //$NON-NLS-1$ //$NON-NLS-2$
      //debugPrint(" map25: " + "############# setupMap:  Map25ProviderManager.getDefaultTheme(TileEncoding.VTM)" + Map25ProviderManager.getDefaultTheme(TileEncoding.VTM));  //$NON-NLS-1$
      mMap.setTheme(getTheme(mapProvider));
      //mMap.setTheme((ThemeFile) Map25ProviderManager.getDefaultTheme(TileEncoding.VTM));

      debugPrint(" map25: " + "############# setupMap:  leaving"); //$NON-NLS-1$ //$NON-NLS-2$
   }

   private void setupMap_Layers() {
      debugPrint(" map25: " + "################ setupMap_Layers:  entering"); //$NON-NLS-1$ //$NON-NLS-2$
      final Layers layers = mMap.layers();
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      _hillshadingSource = DefaultSources.HIKEBIKE_HILLSHADE
            .httpFactory(_httpFactory)
            .zoomMin(1)
            .zoomMax(16)
            .build();

      /* needs long copyright hint... */
//	   _hillshadingSource =  DefaultSources.MAPILION_HILLSHADE_2
//	         .httpFactory(_httpFactory)
//	         .apiKey(_mp_key)
//	         .build();

      // hillshading with 1MB RAM Cache, using existing _httpfactory with diskcache
      _layer_HillShading = new BitmapTileLayer(mMap, _hillshadingSource, 1 << 19);
      _layer_HillShading.setEnabled(false);
      mMap.layers().add(_layer_HillShading);

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
      mMap.layers().add(_layer_Satellite);

      // tour
      _layer_Tour = new TourLayer(mMap);
      _layer_Tour.setEnabled(false);
      layers.add(_layer_Tour);

      // slider path
      _layer_SliderPath = new SliderPath_Layer(mMap);
      _layer_SliderPath.setEnabled(false);
      layers.add(_layer_SliderPath);

      //buildings
      /*
       * here i have to investigate
       * with this code i got always good S3DB, but online buildings did not look good with:
       * "new S3DBLayer(mMap,_layer_BaseMap, true)"
       */
//    // Buildings or S3DB  Block I
      _layer_S3DB_Building = new S3DBLayer(mMap, _layer_BaseMap, true); //this is working with subtheme  switching, but no online buildings anymore
      _layer_Building = new BuildingLayer(mMap, _layer_BaseMap, true, true); // building is not working with online maps, so deactvated also the shadow

      if (_isOfflineMap) {
//			// S3DB

         _layer_S3DB_Building.setEnabled(true);
         _layer_S3DB_Building.setColored(true);
         debugPrint(" map25: " + "################ setupMap_Layers: adding S3DBlayer "); //$NON-NLS-1$ //$NON-NLS-2$
         //_layer_BaseMap.setTheme(_mf_IRenderTheme); //again??
         layers.remove(_layer_Building);
         layers.add(_layer_S3DB_Building);
      } else {
         // building

         _layer_Building.setEnabled(true);
         debugPrint(" map25: " + "################ setupMap_Layers:Building Layer "); //$NON-NLS-1$ //$NON-NLS-2$
         layers.remove(_layer_S3DB_Building);
         layers.add(_layer_Building);
      }

      // label
      _layer_Label = new LabelLayerMT(mMap, _layer_BaseMap);
      _layer_Label.setEnabled(false);
      layers.add(_layer_Label);

      // MapBookmarks
      //debugPrint(" map25: " + "################ setupMap_Layers: calling constructor"); //$NON-NLS-1$
      _markertoolkit = new MarkerToolkit(MarkerShape.STAR);
      if (config.isMarkerClustered) {
         //_layer_MapBookmark = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), _markertoolkit._markerRendererFactory, this);
         _layer_MapBookmark = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _markertoolkit._markerRendererFactory, _markertoolkit);
      } else {
         _layer_MapBookmark = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _markertoolkit._symbol, _markertoolkit);
      }
      final List<MarkerInterface> pts = _markertoolkit.createMarkerItemList(_markerMode);
      _layer_MapBookmark.addItems(pts);
      _layer_MapBookmark.setEnabled(false);
      layers.add(_layer_MapBookmark);

      // marker
      _layer_Marker = new MarkerLayer(mMap, this);
      _layer_Marker.setEnabled(false);
      layers.add(_layer_Marker);

      //Photos
      _phototoolkit = new PhotoToolkit();
      if (config.isMarkerClustered) { //sharing same setting as MapBookmarks, later photolayer should get its own configuration
         _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _phototoolkit._markerRendererFactory, _phototoolkit);
      } else {
         _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _phototoolkit._symbol, _phototoolkit);
      }
      //_layer_Photo.addItems(_phototoolkit._photo_pts);  //must not be done at startup, no tour is loaded yet
      _layer_Photo.setEnabled(false);
      layers.add(_layer_Photo);

      // slider location
      _layer_SliderLocation = new SliderLocation_Layer(mMap);
      _layer_SliderLocation.setEnabled(false);
      layers.add(_layer_SliderLocation);

      // scale bar
      _layer_ScaleBar = createLayer_ScaleBar();
      layers.add(_layer_ScaleBar);

      // layercheck
      layers.toString();

      // tile info
      _layer_TileInfo = new TileGridLayerMT(mMap);
      _layer_TileInfo.setEnabled(false);
      layers.add(_layer_TileInfo);

      debugPrint(" map25: " + "################ setupMap_Layers: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      debugPrint(" map25: " + "################ setupMap_Layers:  leaving"); //$NON-NLS-1$ //$NON-NLS-2$

   }

   void stop() {

      _lwjglApp.stop();
   }

   /**
    * updates the mapbookmarklayer, switching between clustered and not clustered
    * settings are from MarkerConfig
    * replacing the mapbookmarkitems
    */
   public void updateUI_MapBookmarkLayer() {
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final Layers layers = mMap.layers();
      final int layer_index_MapBookmarkLayer = layers.indexOf(_layer_MapBookmark);
      final boolean isShowMapBookmarkLayer = config.isShowMapBookmark;
      //debugPrint(" map25: # updateUI_MapBookmarkLayer(): entering"); //$NON-NLS-1$
      if (config.isMarkerClustered != _markertoolkit._isMarkerClusteredLast) { // only recreate MapBookmarkLayer when changed in UI

         layers.remove(_layer_MapBookmark);
         if (config.isMarkerClustered) {
            _layer_MapBookmark = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _markertoolkit._markerRendererFactory, _markertoolkit);
         } else {
            _layer_MapBookmark = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _markertoolkit._symbol, _markertoolkit);
         }
         layers.add(layer_index_MapBookmarkLayer, _layer_MapBookmark);
         //debugPrint(" map25: " + "# updateUI_MapBookmarkLayer(): index is now: " + layer_index_MapBookmark); //$NON-NLS-1$
      } else {
         _layer_MapBookmark.removeAllItems();
      }
      //_layer_Bookmark.removeAllItems();
      final List<MarkerInterface> pts = _markertoolkit.createMarkerItemList(_markerMode);
      //debugPrint(" map25: " + "# updateUI_MapBookmarkLayer(): #MapBookmartks: " + pts.size()); //$NON-NLS-1$
      _layer_MapBookmark.addItems(pts);
      _layer_MapBookmark.setEnabled(isShowMapBookmarkLayer);
      _markertoolkit._isMarkerClusteredLast = config.isMarkerClustered;
      //
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
      debugPrint(" map25: " + "# updateUI_PhotoLayer():  entering"); //$NON-NLS-1$ //$NON-NLS-2$
      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final Layers layers = mMap.layers();
      final int layer_index_PhotoLayer = layers.indexOf(_layer_Photo);
      final boolean isShowPhotoLayer = config.isShowPhoto;
      // using settings from MapBookmarks must be changed later with own config
      //"STATE_IS_LAYER_PHOTO_VISIBLE"
      //debugPrint(" map25: " + "# updateUI_PhotoLayer(): #photos: " + _selectedPhotosPts.size()); //$NON-NLS-1$
      if (config.isMarkerClustered != _phototoolkit._isMarkerClusteredLast) { // only recreate PhotoLayer when changed in UI.
         //debugPrint(" map25: " + "# updateUI_PhotoLayer(): index was before: " + layer_index_PhotoLayer); //$NON-NLS-1$
         layers.remove(_layer_Photo);
         //if (config.isPhotoClustered) {
         if (config.isMarkerClustered) {
            _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _phototoolkit._markerRendererFactory, _phototoolkit);
         } else {
            _layer_Photo = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _phototoolkit._symbol, _phototoolkit);
         }
         layers.add(layer_index_PhotoLayer, _layer_Photo);
      } else {
         _layer_Photo.removeAllItems();
      }

      _selectedPhotosPts = _phototoolkit.createPhotoItemList(_map25View.get_allPhotos(), getIsPhotoShowTitle(), getIsPhotoShowScaled()); //hopefully done in map25view "paintToursAndUpdate"
      debugPrint(" map25: " + "# updateUI_PhotoLayer(): #photos: " + _selectedPhotosPts.size() + " enabled: " + "isShowPhotoLayer"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      _layer_Photo.addItems(_selectedPhotosPts); //hopefully done in map25view "paintToursAndUpdate"
      _layer_Photo.setEnabled(isShowPhotoLayer);

      //_phototoolkit._isMarkerClusteredLast = config.isPhotoClustered;
      _phototoolkit._isMarkerClusteredLast = config.isMarkerClustered;// using settings from MapBookmarks must be changed later with own config
   }

}
