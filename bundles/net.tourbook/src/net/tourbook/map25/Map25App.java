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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.tourbook.common.Bool;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25TileSource.Builder;
import net.tourbook.map25.OkHttpEngineMT.OkHttpFactoryMT;
import net.tourbook.map25.layer.labeling.LabelLayerMT;
import net.tourbook.map25.layer.marker.MapMarker;
import net.tourbook.map25.layer.marker.MarkerConfig;
import net.tourbook.map25.layer.marker.MarkerLayerMT;
import net.tourbook.map25.layer.marker.MarkerLayerMT.OnItemGestureListener;
import net.tourbook.map25.layer.marker.MarkerMode;
import net.tourbook.map25.layer.marker.MarkerRendererMT;
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
import org.oscim.event.Event;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GestureHandlerImpl;
import org.oscim.gdx.LwjglGL20;
import org.oscim.gdx.MotionHandler;
import org.oscim.layers.GenericLayer;
import org.oscim.layers.Layer;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerInterface;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.S3DBLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map.UpdateListener;
import org.oscim.map.ViewController;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.ExtrusionRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.renderer.LayerRenderer;
import org.oscim.renderer.light.Sun;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.RenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ThemeLoader;
import org.oscim.theme.VtmThemes;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.oscim.theme.rule.Rule;
import org.oscim.theme.rule.Rule.RuleVisitor;
import org.oscim.theme.styles.ExtrusionStyle;
import org.oscim.theme.styles.RenderStyle;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.TileSource.OpenResult;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.bitmap.BitmapTileSource;
import org.oscim.tiling.source.bitmap.DefaultSources;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.tiling.source.mvt.MapilionMvtTileSource;
import org.oscim.utils.FastMath;
import org.oscim.utils.Parameters;

import okhttp3.Cache;

public class Map25App extends GdxMap implements OnItemGestureListener, ItemizedLayer.OnItemGestureListener<MarkerInterface> {

   /**
    * When <code>true</code> then <b>net.tourbook.ext.vtm</b> plugin is used, when
    * <code>false</code> then <b>vtm-parent</b> plugin for map25 development is used.
    * <p>
    * <b>Before releasing, set this to <code>true</code></b>
    * <p>
    */
   private static final boolean IS_USING_VTM_PRODUCTION_PLUGIN = true;

   //
   private static final String     STATE_LAYER_BUILDING_IS_SHOW_SHADOW    = "STATE_LAYER_BUILDING_IS_SHOW_SHADOW";    //$NON-NLS-1$
   private static final String     STATE_LAYER_BUILDING_IS_VISIBLE        = "STATE_LAYER_BUILDING_IS_VISIBLE";        //$NON-NLS-1$
   private static final String     STATE_LAYER_BUILDING_MIN_ZOOM_LEVEL    = "STATE_LAYER_BUILDING_MIN_ZOOM_LEVEL";    //$NON-NLS-1$
   private static final String     STATE_LAYER_BUILDING_SUN_DAY_TIME      = "STATE_LAYER_BUILDING_SUN_DAY_TIME";      //$NON-NLS-1$
   private static final String     STATE_LAYER_BUILDING_SUN_RISE_SET_TIME = "STATE_LAYER_BUILDING_SUN_RISE_SET_TIME"; //$NON-NLS-1$
   private static final String     STATE_LAYER_LABEL_IS_VISIBLE           = "STATE_LAYER_LABEL_IS_VISIBLE";           //$NON-NLS-1$
   private static final String     STATE_LAYER_LABEL_IS_BEFORE_BUILDING   = "STATE_LAYER_LABEL_IS_BEFORE_BUILDING";   //$NON-NLS-1$
   private static final String     STATE_MAP_POS_X                        = "STATE_MAP_POS_X";                        //$NON-NLS-1$

   private static final String     STATE_MAP_POS_Y                        = "STATE_MAP_POS_Y";                        //$NON-NLS-1$
   private static final String     STATE_MAP_POS_ZOOM_LEVEL               = "STATE_MAP_POS_ZOOM_LEVEL";               //$NON-NLS-1$
   private static final String     STATE_MAP_POS_BEARING                  = "STATE_MAP_POS_BEARING";                  //$NON-NLS-1$
   private static final String     STATE_MAP_POS_SCALE                    = "STATE_MAP_POS_SCALE";                    //$NON-NLS-1$
   private static final String     STATE_MAP_POS_TILT                     = "STATE_MAP_POS_TILT";                     //$NON-NLS-1$
   private static final String     STATE_SELECTED_MAP25_PROVIDER_ID       = "STATE_SELECTED_MAP25_PROVIDER_ID";       //$NON-NLS-1$
   private static final String     STATE_SUFFIX_MAP_CURRENT_POSITION      = "MapCurrentPosition";                     //$NON-NLS-1$
   static final String             STATE_SUFFIX_MAP_DEFAULT_POSITION      = "MapDefaultPosition";                     //$NON-NLS-1$
   //
   public static final String      THEME_STYLE_ALL                        = "theme-style-all";                        //$NON-NLS-1$
   //
   public static final float       SUN_TIME_RANGE                         = 10;
   public static final float       SUN_TIME_DETAIL_RANGE                  = 50;
   //
   private static IDialogSettings  _state;
   //
   private static Map25View        _map25View;
   private static LwjglApplication _lwjglApp;
   //
   private Map25Provider           _selectedMapProvider;
   //
   private String                  _mapDefaultLanguage                    = Locale.getDefault().toString();
   private BitmapTileSource        _hillshadingSource;
   private BitmapTileSource        _satelliteSource;
   //
   private int                     _numOfflineMapFiles                    = 0;
   //
   private String                  _mp_key                                = "80d7bc63-94fe-416f-a63f-7173f81a484c";   //$NON-NLS-1$
   //
   /**
    * The opacity can be set in the layer but not read. This will keep the state of the hillshading
    * opacity.
    */
   private int                     _layer_HillShading_Opacity;
   //
   private Bool                    _building_IsShowShadow;
   private boolean                 _building_IsVisible;
   private int                     _building_MinZoomLevel;
   private SunDayTime              _building_SunDaytime;
   //
   /**
    * Relative time <code>0...1</code> between sunset and sunrise
    */
   private float                   _building_Sunrise_Sunset_Time;
   //
   private Bool                    _currentBuilding_IsShowShadow;
   private int                     _currentBuilding_MinZoomLevel;
   private IRenderTheme            _currentBuilding_RenderTheme;
   private SunDayTime              _currentBuilding_SunDayTime;
   private float                   _currentBuilding_Sunrise_Sunset_Time;
   //
   private boolean                 _layer_Label_IsVisible;
   private boolean                 _layer_Label_IsBeforeBuilding;
   //
   private OsmTileLayerMT          _layer_BaseMap;
   private S3DBLayer               _layer_Building_S3DB;
   private GenericLayer            _layer_Building_S3DB_SunUpdate;
   private Layer                   _layer_HillShading_AFTER;
   private BitmapTileLayer         _layer_HillShading_TILE_LOADING;
   private LabelLayerMT            _layer_Label;
   private ItemizedLayer           _layer_MapBookmark_VARYING;
   private ItemizedLayer           _layer_MapBookmark_Clustered;
   private ItemizedLayer           _layer_MapBookmark_NotClustered;
   private ItemizedLayer           _layer_Photo_VARYING;
   private ItemizedLayer           _layer_Photo_Clustered;
   private ItemizedLayer           _layer_Photo_NotCluster;
   private Layer                   _layer_Satellite_AFTER;
   private BitmapTileLayer         _layer_Satellite_TILE_LOADING;
   private MapScaleBarLayer        _layer_ScaleBar;
   private SliderLocation_Layer    _layer_SliderLocation;
   private SliderPath_Layer        _layer_SliderPath;
   private TileGridLayerMT         _layer_TileInfo;
   private TourLayer               _layer_Tour;
   private MarkerLayerMT           _layer_TourMarker;
   //
   private OkHttpFactoryMT         _httpFactory;
   //
   private long                    _lastRenderTime;
   //
   private float                   _offline_TextScale                     = 0.75f;
   private float                   _offline_UserScale                     = 2.50f;
   private float                   _online_TextScale                      = 0.50f;
   private float                   _online_UserScale                      = 2.0f;

   private OffOnline               _currentOffOnline;
   private TileSource              _currentOnline_TileSource;
   private TileEncoding            _currentOnline_TileSource_Encoding;
   private ThemeFile               _currentOnline_MapProviderTheme;
   private TileSource              _currentOffline_TileSource;
   private String                  _currentOffline_TileSource_FilePath;
   //
   //
   private MarkerToolkit _mapBookmarkToolkit = new MarkerToolkit(MarkerShape.STAR);
   // MarkerToolkit.modeDemo or MarkerToolkit.modeNormal
   private MarkerMode    _tourMarkerMode     = MarkerMode.NORMAL;
   //
   /*
    * Photos
    */
   private PhotoToolkit _photoToolkit     = new PhotoToolkit(this);
   //
   private boolean      _isShowPhoto      = true;
   private boolean      _isShowPhotoTitle = true;
   private boolean      _isPhotoScaled    = false;
   //
   private int          _photoSize;
   /**
    * Is <code>true</code> when a tour marker is hit.
    */
   private boolean      _isMapItemHit;

   private static class MinZoomRuleVisitor extends RuleVisitor {

      private int _minZoomMask;

      public MinZoomRuleVisitor(final int minZoomLevel) {

         _minZoomMask = 1 << minZoomLevel;
      }

      @Override
      public void apply(final Rule rule) {

         for (final RenderStyle<?> style : rule.styles) {

            if (style instanceof ExtrusionStyle) {

               /*
                * Using reflection because the zoom field is final and the 2.5D author do not want
                * to modify it
                * https://github.com/mapsforge/vtm/discussions/927#discussioncomment-2735903
                */
               try {

                  final Field zoomField = rule.getClass().getField("zoom"); //$NON-NLS-1$

                  zoomField.setAccessible(true);
                  zoomField.setInt(rule, _minZoomMask);

               } catch (NoSuchFieldException
                     | SecurityException
                     | IllegalArgumentException
                     | IllegalAccessException e) {

                  e.printStackTrace();
               }
            }
         }

         super.apply(rule);
      }
   }

   private static enum OffOnline {
      IS_ONLINE, IS_OFFLINE
   }

   public enum SunDayTime {

      CURRENT_TIME, //
      DAY_TIME, //
      NIGHT_TIME, //
   }

   /**
    * There is no easy way to update the sun position when current time is NOT selected, this will
    * "overwrite" {@link org.oscim.renderer.ExtrusionRenderer.update(GLViewport)}
    */
   private class SunUpdateRenderer extends LayerRenderer {

      @Override
      public void render(final GLViewport viewport) {}

      /**
       * Set sun position to the current viewport location
       *
       * @param viewport
       */
      private void setSunPosition(final GLViewport viewport) {

         if (_building_IsVisible == false || _building_IsShowShadow != Bool.TRUE) {
            return;
         }

         final Sun sun = _layer_Building_S3DB.getExtrusionRenderer().getSun();

         /*
          * Set sun coordinates, copied from org.oscim.renderer.ExtrusionRenderer.update(GLViewport)
          */
         final MapPosition pos = viewport.pos;
         final float lat = (float) pos.getLatitude();
         final float lon = (float) pos.getLongitude();

         if (FastMath.abs(sun.getLatitude() - lat) > 0.2f
               || Math.abs(sun.getLongitude() - lon) > 0.2f) {

            // location is only updated if necessary (not every frame)

            sun.setCoordinates(lat, lon);

            setLayer_Building_SunPosition();
         }
      }

      @Override
      public void update(final GLViewport viewport) {

         setSunPosition(viewport);
      }
   }

   public Map25App(final IDialogSettings state) {

      _state = state;

      restoreState();
   }

   public static Map25App createMap(final Map25View map25View, final IDialogSettings state, final Canvas canvas) {

      init();

      _map25View = map25View;
      _state = state;

      final Map25App mapApp = new Map25App(state);

      _lwjglApp = new LwjglApplication(mapApp, getConfig(), canvas);

      return mapApp;
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
    * @return Returns the absolut file path or <code>null</code> when not available
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
    * @return Returns <code>true</code> when file is OK
    */
   public Boolean checkMapFile(final File file2check) {

      final MapFileTileSource mapFileSource = new MapFileTileSource();
      mapFileSource.setMapFile(file2check.getAbsolutePath());

      final OpenResult openResult = mapFileSource.open();
      mapFileSource.close();

      final boolean isSuccess = openResult.isSuccess();

      if (isSuccess == false) {
         StatusUtil.logError(String.format("[2.5D Map] Cannot open '%s': %s", //$NON-NLS-1$
               file2check.getAbsolutePath(),
               openResult.getErrorMessage()));
      }

      return isSuccess;
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
//    mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH);

      mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
//    mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);

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

         setMapProvider_01_Online(_selectedMapProvider);

      } else {

         // offline map

         setMapProvider_02_Offline(_selectedMapProvider);
      }

      createLayers_SetupLayers();

      /**
       * Map Viewport
       */
      final ViewController mapViewport = mMap.viewport();

      // extend default tilt
      mapViewport.setMaxTilt(180.0f);
      mapViewport.setMinScale(2);

      updateLayer_TourMarkers();

      restoreState_MapPosition();

      // update actions in UI thread, run this AFTER the layers are created
      Display.getDefault().asyncExec(() -> {

         // enable/disable layers
         _map25View.restoreState();

         restoreMapLayers();
      });
   }

   /**
    * {@link #_layer_BaseMap} must be set before calling this method
    */
   private void createLayers_SetupLayers() {

      // needs long copyright hint...
      _hillshadingSource = DefaultSources.MAPILION_HILLSHADE_2
            .httpFactory(_httpFactory)
            .apiKey(_mp_key)
            .build();

      // hillshading with 1MB RAM Cache, using existing _httpfactory with diskcache
      _layer_HillShading_TILE_LOADING = new BitmapTileLayer(mMap, _hillshadingSource, 1 << 19);
      _layer_HillShading_TILE_LOADING.setEnabled(false);

      /*
       * Satellite maps like google earth
       */
      _satelliteSource = BitmapTileSource.builder()
            .httpFactory(_httpFactory)
            .url("http://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/tile") //$NON-NLS-1$
            .tilePath("/{Z}/{Y}/{X}.png") //$NON-NLS-1$
            .zoomMin(1)
            .zoomMax(18)
            .build();

      _layer_Satellite_TILE_LOADING = new BitmapTileLayer(mMap, _satelliteSource, 1 << 19);
      _layer_Satellite_TILE_LOADING.setEnabled(false);

      // tour
      _layer_Tour = new TourLayer(mMap);
      _layer_Tour.setEnabled(false);

      // slider path
      _layer_SliderPath = new SliderPath_Layer(mMap);
      _layer_SliderPath.setEnabled(false);

      /*
       * Buildings, create a default layer to keep it's position, it can be replaced later on
       */
      final boolean isShowShadow = _building_IsShowShadow == Bool.TRUE;
      final int minZoom = _building_MinZoomLevel;
      final int maxZoom = mMap.viewport().getMaxZoomLevel();

      _layer_Building_S3DB_SunUpdate = new GenericLayer(mMap, new SunUpdateRenderer());
      _layer_Building_S3DB_SunUpdate.setEnabled(true);

      _layer_Building_S3DB = new S3DBLayer(mMap, _layer_BaseMap, minZoom, maxZoom, isShowShadow);
      _layer_Building_S3DB.setEnabled(true);
      setLayer_Building_SunPosition();

      // keep current building states
      _currentBuilding_IsShowShadow = _building_IsShowShadow;
      _currentBuilding_SunDayTime = _building_SunDaytime;
      _currentBuilding_Sunrise_Sunset_Time = _building_Sunrise_Sunset_Time;
      _currentBuilding_MinZoomLevel = minZoom;

      /*
       * Label
       */
      _layer_Label = new LabelLayerMT(mMap, _layer_BaseMap);
      _layer_Label.setEnabled(false);

      /*
       * Map bookmarks
       */
      final MarkerConfig markerConfig = Map25ConfigManager.getActiveMarkerConfig();

      _layer_MapBookmark_Clustered = new ItemizedLayer(mMap,
            new ArrayList<MarkerInterface>(),
            _mapBookmarkToolkit.getMarkerRendererFactory(),
            _mapBookmarkToolkit);

      _layer_MapBookmark_NotClustered = new ItemizedLayer(
            mMap,
            new ArrayList<MarkerInterface>(),
            _mapBookmarkToolkit.getMarkerSymbol(),
            _mapBookmarkToolkit);

      if (markerConfig.isMarkerClustered) {
         _layer_MapBookmark_VARYING = _layer_MapBookmark_Clustered;
      } else {
         _layer_MapBookmark_VARYING = _layer_MapBookmark_NotClustered;
      }

      final List<MarkerInterface> allMarkerItems = _mapBookmarkToolkit.createBookmarksAsMapMarker(_tourMarkerMode);
      _layer_MapBookmark_VARYING.addItems(allMarkerItems);
      _layer_MapBookmark_VARYING.setEnabled(false);

      /*
       * Tour marker
       */
      _layer_TourMarker = new MarkerLayerMT(mMap, this);
      _layer_TourMarker.setEnabled(false);

      /*
       * Photos
       */
      _layer_Photo_Clustered = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _photoToolkit.getMarkerRendererFactory(), _photoToolkit);
      _layer_Photo_NotCluster = new ItemizedLayer(mMap, new ArrayList<MarkerInterface>(), _photoToolkit.getSymbol(), _photoToolkit);
      if (markerConfig.isMarkerClustered) {
         //sharing same setting as MapBookmarks, later photolayer should get its own configuration
         _layer_Photo_VARYING = _layer_Photo_Clustered;
      } else {
         _layer_Photo_VARYING = _layer_Photo_NotCluster;
      }
      //_layer_Photo.addItems(_phototoolkit._photo_pts);  //must not be done at startup, no tour is loaded yet
      _layer_Photo_VARYING.setEnabled(false);

      // slider location
      _layer_SliderLocation = new SliderLocation_Layer(mMap);
      _layer_SliderLocation.setEnabled(false);

      // scale bar
      _layer_ScaleBar = createLayer_ScaleBar();

      // tile info
      _layer_TileInfo = new TileGridLayerMT(mMap);
      _layer_TileInfo.setEnabled(false);

      /*
       * Add all layers
       */
      final Layers allMapLayer = mMap.layers();

      allMapLayer.add(_layer_Satellite_TILE_LOADING);
      allMapLayer.add(_layer_HillShading_TILE_LOADING);
      allMapLayer.add(_layer_Tour);
      allMapLayer.add(_layer_SliderPath);
      allMapLayer.add(_layer_Building_S3DB_SunUpdate);
      allMapLayer.add(_layer_Building_S3DB);
      allMapLayer.add(_layer_Label);
      allMapLayer.add(_layer_MapBookmark_VARYING);
      allMapLayer.add(_layer_TourMarker);
      allMapLayer.add(_layer_Photo_VARYING);
      allMapLayer.add(_layer_SliderLocation);
      allMapLayer.add(_layer_ScaleBar);
      allMapLayer.add(_layer_TileInfo);

      /*
       * Set static layers which are located after the named layer and which will never be removed,
       * this "position" is used to set the correct position for removed/added layers
       */
      _layer_Satellite_AFTER = _layer_Tour;
      _layer_HillShading_AFTER = _layer_Tour;
   }

   private UrlTileSource createTileSource(final Map25Provider mapProvider) {

      final Builder<?> map25Builder = Map25TileSource
            .builder(mapProvider)
            .url(mapProvider.online_url)
            .tilePath(mapProvider.online_TilePath)
            .httpFactory(_httpFactory);

      final String apiKey = mapProvider.online_ApiKey;
      if (apiKey != null && apiKey.trim().length() > 0) {
         map25Builder.apiKey(apiKey);
      }

      return map25Builder.build();
   }

   private MapilionMvtTileSource createTileSource_Maplilion(final Map25Provider mapProvider) {

      MapilionMvtTileSource tileSource;

      if (mapProvider.online_ApiKey == null || mapProvider.online_ApiKey.trim().length() == 0) {

         tileSource = MapilionMvtTileSource.builder()
               .apiKey(_mp_key)
               .httpFactory(_httpFactory)
               .build();

      } else {

         tileSource = MapilionMvtTileSource.builder()
               .apiKey(mapProvider.online_ApiKey.trim())
               .httpFactory(_httpFactory)
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
    * @return Returns all map files which are available in the map file folder and sets the number
    *         of map files into {@link #_numOfflineMapFiles}
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

   public Bool getLayer_Building_IsShadow() {
      return _building_IsShowShadow;
   }

   public int getLayer_Building_MinZoomLevel() {
      return _building_MinZoomLevel;
   }

   public S3DBLayer getLayer_Building_S3DB() {
      return _layer_Building_S3DB;
   }

   public SunDayTime getLayer_Building_SunDayTime() {
      return _building_SunDaytime;
   }

   /**
    * @return Returns relative time <code>0...1</code> between sunset and sunrise
    */
   public float getLayer_Building_Sunrise_Sunset_Time() {
      return _building_Sunrise_Sunset_Time;
   }

   public BitmapTileLayer getLayer_HillShading() {
      return _layer_HillShading_TILE_LOADING;
   }

   public int getLayer_HillShading_Opacity() {
      return _layer_HillShading_Opacity;
   }

   public LabelLayerMT getLayer_Label() {
      return _layer_Label;
   }

   public boolean getLayer_Label_IsBeforeBuilding() {
      return _layer_Label_IsBeforeBuilding;
   }

   public ItemizedLayer getLayer_MapBookmark() {
      return _layer_MapBookmark_VARYING;
   }

   public ItemizedLayer getLayer_Photo() {
      return _layer_Photo_VARYING;
   }

   public BitmapTileLayer getLayer_Satellite() {
      return _layer_Satellite_TILE_LOADING;
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

   public MarkerLayerMT getLayer_TourMarker() {
      return _layer_TourMarker;
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

   private ThemeFile getTheme_Online(final Map25Provider mapProvider) {

      switch (mapProvider.tileEncoding) {
      case MVT: // Mapzen
         return VtmThemes.MAPZEN;

      case VTM: // Open Science Map
      default:
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

   private boolean isUpdateAll(final OffOnline isOffOnline) {

      boolean isUpdateAll = false;

      if (_currentOffOnline != isOffOnline) {
         isUpdateAll = true;
      }

      _currentOffOnline = isOffOnline;

      return isUpdateAll;
   }

   private ExternalRenderTheme loadTheme(final String themeFilePath, final String styleID) {

      final XmlRenderThemeMenuCallback menuCallback = new XmlRenderThemeMenuCallback() {

         @Override
         public Set<String> getCategories(final XmlRenderThemeStyleMenu renderThemeStyleMenu) {

            final String style = styleID == null
                  ? renderThemeStyleMenu.getDefaultValue()
                  : styleID;

            final XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);

            if (THEME_STYLE_ALL.equals(styleID)) {

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
      };

      return new ExternalRenderTheme(themeFilePath, menuCallback);
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

      updateLayer_TourMarkers();
      updateLayer_MapBookmarks();

      // photos can be clustered which is currently set in the marker config
      updateLayer_Photos();

      mMap.render();
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

//        Fix exception
         //
//           Exception in thread "LWJGL Application" java.lang.IllegalArgumentException: top == bottom
//              at org.oscim.renderer.GLMatrix.frustumM(GLMatrix.java:331)
//              at org.oscim.map.ViewController.setScreenSize(ViewController.java:50)
//              at org.oscim.gdx.GdxMap.resize(GdxMap.java:122)
//              at net.tourbook.map.vtm.VtmMap.resize(VtmMap.java:176)

         return;
      }

      super.resize(w, h);
   }

   /**
    * Remove tile layers which are not visible, otherwise they would still download tile images even
    * when they are disabled !!!
    */
   private void restoreMapLayers() {

      updateLayer_Building();
      updateLayer_Label();

      updateLayer_ReorderLayers();

      /*
       * This order must be the same as when these layers were added initially
       */
      setupMapLayers_SetTileLoadingLayer(_layer_Satellite_TILE_LOADING, _layer_Satellite_AFTER);
      setupMapLayers_SetTileLoadingLayer(_layer_HillShading_TILE_LOADING, _layer_HillShading_AFTER);
   }

   private void restoreState() {

// SET_FORMATTING_OFF

      _building_IsVisible              = Util.getStateBoolean( _state, STATE_LAYER_BUILDING_IS_VISIBLE,      true);
      _building_MinZoomLevel           = Util.getStateInt(     _state, STATE_LAYER_BUILDING_MIN_ZOOM_LEVEL,  17);
      _building_Sunrise_Sunset_Time    = Util.getStateFloat(   _state, STATE_LAYER_BUILDING_SUN_RISE_SET_TIME, 0.5f);

      _building_IsShowShadow           = (Bool)       Util.getStateEnum(_state, STATE_LAYER_BUILDING_IS_SHOW_SHADOW,    Bool.TRUE);
      _building_SunDaytime             = (SunDayTime) Util.getStateEnum(_state, STATE_LAYER_BUILDING_SUN_DAY_TIME,      SunDayTime.CURRENT_TIME);

      _layer_Label_IsVisible           = Util.getStateBoolean( _state, STATE_LAYER_LABEL_IS_VISIBLE,         true) ;
      _layer_Label_IsBeforeBuilding    = Util.getStateBoolean( _state, STATE_LAYER_LABEL_IS_BEFORE_BUILDING, true) ;

// SET_FORMATTING_ON
   }

   private void restoreState_MapPosition() {

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

// SET_FORMATTING_OFF

      _state.put(STATE_LAYER_BUILDING_IS_VISIBLE,                    _building_IsVisible);
      _state.put(STATE_LAYER_BUILDING_MIN_ZOOM_LEVEL,                _building_MinZoomLevel);
      _state.put(STATE_LAYER_BUILDING_SUN_RISE_SET_TIME,             _building_Sunrise_Sunset_Time);
      Util.setStateEnum(_state, STATE_LAYER_BUILDING_IS_SHOW_SHADOW, _building_IsShowShadow);
      Util.setStateEnum(_state, STATE_LAYER_BUILDING_SUN_DAY_TIME,   _building_SunDaytime);

      _state.put(STATE_LAYER_LABEL_IS_VISIBLE,                       _layer_Label_IsVisible);
      _state.put(STATE_LAYER_LABEL_IS_BEFORE_BUILDING,               _layer_Label_IsBeforeBuilding);

      _state.put(STATE_SELECTED_MAP25_PROVIDER_ID,                   _selectedMapProvider.getId());

// SET_FORMATTING_ON

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

   /**
    * Adjust building min zoom level
    *
    * @param renderTheme
    */
   private void setLayer_Building_MinZoomLevel(final IRenderTheme renderTheme) {

      _currentBuilding_RenderTheme = renderTheme;

      final RenderTheme modifiedRenderTheme = (RenderTheme) renderTheme;

      modifiedRenderTheme.traverseRules(new MinZoomRuleVisitor(_building_MinZoomLevel));
      modifiedRenderTheme.updateStyles();
   }

   public void setLayer_Building_Options(final boolean isVisible,
                                         final int minZoomLevel,
                                         final Bool isShowShadow,
                                         final SunDayTime sunDayTime,
                                         final float sunRiseSetTime) {

      _building_IsVisible = isVisible;
      _building_MinZoomLevel = minZoomLevel;
      _building_IsShowShadow = isShowShadow;
      _building_SunDaytime = sunDayTime;
      _building_Sunrise_Sunset_Time = sunRiseSetTime;
   }

   public void setLayer_Building_SunOptions(final SunDayTime sunDayTime,
                                            final float sunRiseSetTime) {

      _building_SunDaytime = sunDayTime;
      _building_Sunrise_Sunset_Time = sunRiseSetTime;

      setLayer_Building_SunPosition();
   }

   private void setLayer_Building_SunPosition() {

      final ExtrusionRenderer extrusionRenderer = _layer_Building_S3DB.getExtrusionRenderer();

      if (_building_SunDaytime == SunDayTime.CURRENT_TIME) {

         // show shadow for the current sun day time position

         extrusionRenderer.enableCurrentSunPos(true);

      } else {

         // show shadow for a selected time

         extrusionRenderer.enableCurrentSunPos(false);

         // The progress
         //
         // of the daylight in range 0 (sunrise) to 1 (sunset) and
         // of the night    in range 1 (sunset)  to 2 (sunrise)
         final int nightAdjustment = _building_SunDaytime == SunDayTime.NIGHT_TIME ? 1 : 0;

         final float sunPosition = _building_Sunrise_Sunset_Time + nightAdjustment;

//         System.out.println((System.currentTimeMillis() + " sun: " + sunPosition));
         // TODO remove SYSTEM.OUT.PRINTLN

         final Sun sun = extrusionRenderer.getSun();

         sun.setProgress(sunPosition);
         sun.updatePosition();
         sun.updateColor();
      }
   }

   public void setLayer_HillShading_Options(final int layer_HillShading_Opacity) {

      _layer_HillShading_Opacity = layer_HillShading_Opacity;
   }

   public void setLayer_Label_Options(final boolean isVisible, final boolean isBeforeBuilding) {

      _layer_Label_IsVisible = isVisible;
      _layer_Label_IsBeforeBuilding = isBeforeBuilding;
   }

   /**
    * This is called when the map provider, theme or theme style was modified.
    *
    * @param mapProvider
    */
   public void setMapProvider(final Map25Provider mapProvider) {

      _selectedMapProvider = mapProvider;

      if (mapProvider.tileEncoding == TileEncoding.MF) {

         // offline map

         setMapProvider_02_Offline(mapProvider);

      } else {

         // online map

         setMapProvider_01_Online(mapProvider);
      }

      mMap.clearMap();
   }

   /**
    * Setup online map, e.g. Mapilion
    *
    * @param mapProvider
    */
   private void setMapProvider_01_Online(final Map25Provider mapProvider) {

      // check if off/online has changed
      final boolean isUpdateAll = isUpdateAll(OffOnline.IS_ONLINE);

      CanvasAdapter.textScale = _online_TextScale;
      CanvasAdapter.userScale = _online_UserScale;

      setMapProvider_10_CreateBaseMapLayer();

      /*
       * Set tile source for the map layer when changed
       */
      final TileEncoding tileEncoding = mapProvider.tileEncoding;
      if (isUpdateAll || tileEncoding != _currentOnline_TileSource_Encoding) {

         _currentOnline_TileSource = TileEncoding.VTM.equals(tileEncoding)

               ? createTileSource(mapProvider)
               : createTileSource_Maplilion(mapProvider);

         _currentOnline_TileSource_Encoding = tileEncoding;

         _layer_BaseMap.setTileSource(_currentOnline_TileSource);
      }

      /*
       * Set theme when changed
       */
      final ThemeFile mapProviderTheme = getTheme_Online(mapProvider);

      if (isUpdateAll || mapProviderTheme != _currentOnline_MapProviderTheme) {

         _currentOnline_MapProviderTheme = mapProviderTheme;

         final IRenderTheme loadedRenderTheme = ThemeLoader.load(mapProviderTheme);
         mMap.setTheme(loadedRenderTheme, false);

         setLayer_Building_MinZoomLevel(loadedRenderTheme);
      }
   }

   /**
    * Setup offline map for mapsforge
    *
    * @param mapProvider
    */
   private void setMapProvider_02_Offline(final Map25Provider mapProvider) {

      // check if off/online has changed
      final boolean isUpdateAll = isUpdateAll(OffOnline.IS_OFFLINE);

      CanvasAdapter.textScale = _offline_TextScale;
      CanvasAdapter.userScale = _offline_UserScale;

      /*
       * Create/set _layer_BaseMap
       */
      setMapProvider_10_CreateBaseMapLayer();

      /*
       * Set tile source from offline file
       */
      TileSource tileSource = null;

      final String offlineMapFilePath = mapProvider.offline_MapFilepath;

      if (isUpdateAll == false
            && offlineMapFilePath != null
            && offlineMapFilePath.equals(_currentOffline_TileSource_FilePath)
            && _currentOffline_TileSource != null) {

         // use already loaded tile source

         tileSource = _currentOffline_TileSource;

      } else {

         // get tile source

         if (checkMapFile(new File(offlineMapFilePath))) {
            StatusUtil.logInfo("[2.5D Map] Using map file: " + offlineMapFilePath); //$NON-NLS-1$
         } else {
            final String errorText = "[2.5D Map] Cannot read map file: " + offlineMapFilePath;
            StatusUtil.showStatus(errorText);
            throw new IllegalArgumentException(errorText);
         }

         tileSource = getAllOfflineMapFiles(offlineMapFilePath);
         if (_numOfflineMapFiles == 0) {
            final String errorText = "[2.5D Map] Cannot read multiple map files from: " + offlineMapFilePath;
            StatusUtil.showStatus(errorText);
            throw new IllegalArgumentException(errorText);
         }

         _currentOffline_TileSource = tileSource;
         _currentOffline_TileSource_FilePath = offlineMapFilePath;
      }

      // set map tile source
      if (isUpdateAll || _layer_BaseMap.getTileSource() != tileSource) {
         _layer_BaseMap.setTileSource(tileSource);
      }

      /*
       * Set theme
       */
      boolean isThemeSet = false;

      IRenderTheme iRenderTheme = null;

      if (mapProvider.offline_IsThemeFromFile) {

         // check theme path, is null when not found
         final String themeFilePath = checkFile(mapProvider.offline_ThemeFilepath);

         if (themeFilePath != null) {

            final ExternalRenderTheme externalRenderTheme = loadTheme(themeFilePath, mapProvider.offline_ThemeStyle);

            iRenderTheme = ThemeLoader.load(externalRenderTheme);

            mMap.setTheme(iRenderTheme);

            isThemeSet = true;
         }
      }

      if (isThemeSet == false) {

         // set internal theme

         if (mapProvider.vtmTheme != null
               && mapProvider.vtmTheme != VtmThemes.MAPZEN
//               && mapProvider.vtmTheme != VtmThemes.OPENMAPTILES
         ) {

            iRenderTheme = ThemeLoader.load(mapProvider.vtmTheme);

            mMap.setTheme(iRenderTheme);
            isThemeSet = true;

         } else {

            // when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead

            iRenderTheme = ThemeLoader.load(VtmThemes.DEFAULT);

            mMap.setTheme(iRenderTheme);
            isThemeSet = true;
         }
      }

      /*
       * Adjust building min zoom level
       */
      if (isThemeSet) {

         setLayer_Building_MinZoomLevel(iRenderTheme);
      }
   }

   /**
    * Set map tile layer
    */
   private void setMapProvider_10_CreateBaseMapLayer() {

      if (_layer_BaseMap == null) {

         _layer_BaseMap = new OsmTileLayerMT(mMap);

         mMap.setBaseMap(_layer_BaseMap);
      }
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

   private void setupMapLayers_SetTileLoadingLayer(final BitmapTileLayer tileLoadingLayer, final Layer tileLoading_AFTER) {

      final Layers allMapLayer = mMap.layers();

      final int currentLayerIndex = allMapLayer.indexOf(tileLoadingLayer);

      if (tileLoadingLayer.isEnabled()) {

         // layer should be visible

         if (currentLayerIndex == -1) {

            // layer is hidden -> add this layer

            final int layerIndexAfter = allMapLayer.indexOf(tileLoading_AFTER);

            allMapLayer.add(layerIndexAfter, tileLoadingLayer);
         }

      } else {

         // layer should be hidden

         if (currentLayerIndex != -1) {

            // layer is visible -> remove this layer

            allMapLayer.remove(tileLoadingLayer);
         }
      }
   }

   void stop() {

      _lwjglApp.stop();
   }

   public void updateLayer() {

      restoreMapLayers();

      mMap.updateMap();
   }

   private void updateLayer_Building() {

      _layer_Building_S3DB.setEnabled(_building_IsVisible);
      _layer_Building_S3DB_SunUpdate.setEnabled(_building_IsVisible);

      /*
       * Check if ALL building parameters have changed
       */
      if (_building_MinZoomLevel == _currentBuilding_MinZoomLevel
            && _building_IsShowShadow == _currentBuilding_IsShowShadow
            && _building_SunDaytime == _currentBuilding_SunDayTime
            && _building_Sunrise_Sunset_Time == _currentBuilding_Sunrise_Sunset_Time) {

         return;
      }

      /*
       * Check if only the sun position has changed, this does not need to recreate the building
       * layer -> this is much faster
       */
      if (_building_MinZoomLevel == _currentBuilding_MinZoomLevel
            && _building_IsShowShadow == _currentBuilding_IsShowShadow) {

         setLayer_Building_SunPosition();

         // keep current building layer states to compare it the next time
         _currentBuilding_SunDayTime = _building_SunDaytime;
         _currentBuilding_Sunrise_Sunset_Time = _building_Sunrise_Sunset_Time;

         return;
      }

      /*
       * Recreate building layer
       */
      final boolean isMinZoomLevelModified = _building_MinZoomLevel != _currentBuilding_MinZoomLevel;
      final int minZoom = _building_MinZoomLevel;
      final int maxZoom = mMap.viewport().getMaxZoomLevel();
      final Bool isShowShadow = _building_IsShowShadow;

      final S3DBLayer newBuildingLayer = new S3DBLayer(mMap, _layer_BaseMap, minZoom, maxZoom, isShowShadow == Bool.TRUE);

      /*
       * Replace old building layer
       */
      final Layers allMapLayer = mMap.layers();

      final int currentLayerIndex = allMapLayer.indexOf(_layer_Building_S3DB);

      allMapLayer.remove(currentLayerIndex);
      allMapLayer.add(currentLayerIndex, newBuildingLayer);

      _layer_Building_S3DB = newBuildingLayer;
      _layer_Building_S3DB.setEnabled(_building_IsVisible);
      setLayer_Building_SunPosition();

      // keep current building layer states to compare it the next time
      _currentBuilding_MinZoomLevel = _building_MinZoomLevel;
      _currentBuilding_IsShowShadow = _building_IsShowShadow;
      _currentBuilding_SunDayTime = _building_SunDaytime;
      _currentBuilding_Sunrise_Sunset_Time = _building_Sunrise_Sunset_Time;

      if (isMinZoomLevelModified) {

         setLayer_Building_MinZoomLevel(_currentBuilding_RenderTheme);

         // force update
         _currentOffOnline = null;

         // this is necessarry that the new zoom level is applied, otherwise it do not work
         setMapProvider(_selectedMapProvider);
      }
   }

   private void updateLayer_Label() {

      _layer_Label.setEnabled(_layer_Label_IsVisible);
   }

   /**
    * updates the mapbookmarklayer, switching between clustered and not clustered
    * settings are from MarkerConfig
    * replacing the mapbookmarkitems
    */
   public void updateLayer_MapBookmarks() {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
      final Layers allMapLayer = mMap.layers();

      final int bookmarkLayerPosition = allMapLayer.indexOf(_layer_MapBookmark_VARYING);

      // only recreate MapBookmarkLayer when changed in UI
      if (config.isMarkerClustered != _mapBookmarkToolkit.isMarkerClusteredLast()) {

         allMapLayer.remove(_layer_MapBookmark_VARYING);

         if (config.isMarkerClustered) {

            _layer_MapBookmark_VARYING = _layer_MapBookmark_Clustered;

         } else {

            _layer_MapBookmark_VARYING = _layer_MapBookmark_NotClustered;
         }

         allMapLayer.add(bookmarkLayerPosition, _layer_MapBookmark_VARYING);

      } else {

         _layer_MapBookmark_VARYING.removeAllItems();
      }

      final List<MarkerInterface> allBookmarkMarker = _mapBookmarkToolkit.createBookmarksAsMapMarker(_tourMarkerMode);

      _layer_MapBookmark_VARYING.addItems(allBookmarkMarker);
      _layer_MapBookmark_VARYING.setEnabled(config.isShowMapBookmark);

      _mapBookmarkToolkit.setIsMarkerClusteredLast(config.isMarkerClustered);
   }

   /**
    * updates the photo layer, switchung between clustered to not clustered
    * settings are from MarkerConfig
    * replacing the photo Items
    * currently no GUI for selecting clustering
    */
   public void updateLayer_Photos() {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      // using settings from MapBookmarks must be changed later with own config
      if (config.isMarkerClustered != _photoToolkit.isMarkerClusteredLast()) {

         // only recreate PhotoLayer when changed in UI

         final Layers allMapLayer = mMap.layers();
         final int photoLayerPosition = allMapLayer.indexOf(_layer_Photo_VARYING);

         allMapLayer.remove(_layer_Photo_VARYING);

         //  config.isPhotoClustered
         if (config.isMarkerClustered) {
            _layer_Photo_VARYING = _layer_Photo_Clustered;

         } else {

            _layer_Photo_VARYING = _layer_Photo_NotCluster;
         }

         if (photoLayerPosition == -1) {
            allMapLayer.add(_layer_Photo_VARYING);
         } else {
            allMapLayer.add(photoLayerPosition, _layer_Photo_VARYING);
         }

      } else {

         _layer_Photo_VARYING.removeAllItems();
      }

      final List<MarkerInterface> photoItems = _photoToolkit.createPhotoItems(_map25View.getFilteredPhotos());

      _layer_Photo_VARYING.addItems(photoItems);
      _layer_Photo_VARYING.setEnabled(_isShowPhoto);

      //_phototoolkit._isMarkerClusteredLast = config.isPhotoClustered;
      // using settings from MapBookmarks must be changed later with own config
      _photoToolkit.setIsMarkerClusteredLast(config.isMarkerClustered);
   }

   private void updateLayer_ReorderLayers() {

      final Layers allMapLayer = mMap.layers();

      final int layerLabel_CurrentIndex = allMapLayer.indexOf(_layer_Label);
      int layerBuilding_CurrentIndex = allMapLayer.indexOf(_layer_Building_S3DB);

      final boolean isLabelBeforeBuilding = layerLabel_CurrentIndex > layerBuilding_CurrentIndex;

      if (_building_IsVisible && _layer_Label_IsVisible) {

         // both layers must be visible

         if (_layer_Label_IsBeforeBuilding && isLabelBeforeBuilding == false) {

            // label < building -> label > building

            allMapLayer.remove(_layer_Label);

            layerBuilding_CurrentIndex = allMapLayer.indexOf(_layer_Building_S3DB);

            allMapLayer.add(layerBuilding_CurrentIndex + 1, _layer_Label);

         } else if (_layer_Label_IsBeforeBuilding == false && isLabelBeforeBuilding) {

            // label > building -> label < building

            allMapLayer.remove(_layer_Label);

            layerBuilding_CurrentIndex = allMapLayer.indexOf(_layer_Building_S3DB);

            allMapLayer.add(layerBuilding_CurrentIndex, _layer_Label);
         }
      }
   }

   private void updateLayer_TourMarkers() {

      final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

      final boolean isShowTourMarker = config.isShowTourMarker;

      _layer_TourMarker.setEnabled(isShowTourMarker);

      if (isShowTourMarker) {

         final MarkerRendererMT markerRenderer = (MarkerRendererMT) _layer_TourMarker.getRenderer();

         markerRenderer.configureRenderer();
      }
   }

   /**
    * Update map and render next frame afterwards
    */
   public void updateMap() {

      mMap.updateMap();
   }

}
