/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import java.awt.Canvas;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
//import java.io.FileNotFoundException;
import java.util.Set;
import java.util.UUID;

import net.tourbook.common.UI;

//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.comparator.SizeFileComparator;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25TileSource.Builder;
import net.tourbook.map25.OkHttpEngineMT.OkHttpFactoryMT;
import net.tourbook.map25.layer.labeling.LabelLayerMT;
import net.tourbook.map25.layer.marker.MapMarker;
import net.tourbook.map25.layer.marker.MarkerConfig;
import net.tourbook.map25.layer.marker.MarkerLayer;
import net.tourbook.map25.layer.marker.MarkerLayer.OnItemGestureListener;
import net.tourbook.map25.layer.marker.MarkerRenderer;
import net.tourbook.map25.layer.marker.MarkerToolkit;
import net.tourbook.map25.layer.marker.MarkerToolkit.MarkerMode;
import net.tourbook.map25.layer.tourtrack.SliderLocation_Layer;
import net.tourbook.map25.layer.tourtrack.SliderPath_Layer;
import net.tourbook.map25.layer.tourtrack.TourLayer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Display;
import org.oscim.awt.AwtGraphics;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.DateTime;
import org.oscim.backend.DateTimeAdapter;
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
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;

import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.bitmap.BitmapTileLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.buildings.S3DBLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map.UpdateListener;
import org.oscim.map.ViewController;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
//import org.oscim.theme.StreamRenderTheme;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.oscim.theme.ExternalRenderTheme;
import org.oscim.theme.IRenderTheme;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.ThemeLoader;
import org.oscim.theme.VtmThemes;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.tiling.TileSource;
import org.oscim.tiling.TileSource.OpenResult;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.bitmap.DefaultSources;
import org.oscim.tiling.source.mapfile.MapFileTileSource;
import org.oscim.tiling.source.mapfile.MultiMapFileTileSource;
import org.oscim.utils.Parameters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.GLVersion;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import okhttp3.Cache;

public class Map25App extends GdxMap implements OnItemGestureListener, ItemizedLayer.OnItemGestureListener<MarkerItem> {

	private static final String		STATE_MAP_POS_X						= "STATE_MAP_POS_X";					//$NON-NLS-1$
	private static final String		STATE_MAP_POS_Y						= "STATE_MAP_POS_Y";					//$NON-NLS-1$
	private static final String		STATE_MAP_POS_ZOOM_LEVEL			= "STATE_MAP_POS_ZOOM_LEVEL";			//$NON-NLS-1$
	private static final String		STATE_MAP_POS_BEARING				= "STATE_MAP_POS_BEARING";				//$NON-NLS-1$
	private static final String		STATE_MAP_POS_SCALE					= "STATE_MAP_POS_SCALE";				//$NON-NLS-1$
	private static final String		STATE_MAP_POS_TILT					= "STATE_MAP_POS_TILT";					//$NON-NLS-1$
	private static final String		STATE_SELECTED_MAP25_PROVIDER_ID	= "STATE_SELECTED_MAP25_PROVIDER_ID";	//$NON-NLS-1$

	private static final String		STATE_SUFFIX_MAP_CURRENT_POSITION	= "MapCurrentPosition";					//$NON-NLS-1$
	static final String				STATE_SUFFIX_MAP_DEFAULT_POSITION	= "MapDefaultPosition";					//$NON-NLS-1$

	public static final String THEME_STYLE_ALL = "theme-style-all"; //$NON-NLS-1$

	private static IDialogSettings	_state;

	private static String	_mf_mapFilePath = "";
	private static String	_mf_themeFilePath = null;
	private static String	_mf_theme_styleID = null;
	private Boolean	_mf_IsThemeFromFile = null;
	private Boolean   _firstRun = true;


	private static Map25View		_map25View;
	private static LwjglApplication	_lwjglApp;

	private String 					_mf_prefered_language = "en"; //$NON-NLS-1$

	private Map25Provider			_selectedMapProvider;
	private TileManager				_tileManager;

	/*
	 * if i could replace "_l" against "_layer_BaseMap", everything would be easier...
	 * _l = mMap.setBaseMap(tileSource);  returns VectorTileLayer
	 */
   private OsmTileLayerMT        _layer_BaseMap;   //extends extends VectorTileLayer
   //private VectorTileLayer      _layer_BaseMap;
   //private VectorTileLayer     _l;   	

	private BuildingLayer			_layer_Building;
	private S3DBLayer					_layer_S3DB_Building;
	private TileSource            _hillshadingSource = null;
	private MapFileTileSource		 _tileSourceOffline;
	private MultiMapFileTileSource _tileSourceOfflineMM;
	
	private int							_tileSourceOfflineMapCount = 0;
	
	public enum DebugMode {OFF, ON};
	public DebugMode debugMode = DebugMode.ON;   // before releasing, set this to OFF
	
	/**
	 * The opacity can be set in the layer but not read. This will keep the state of the hillshading opacity.
	 */
   private int                   _layer_HillShading_Opacity;
	
	private LabelLayerMT				_layer_Label;
	private MarkerLayer				_layer_Marker;
	private BitmapTileLayer			_layer_HillShading;
	private MapScaleBarLayer		_layer_ScaleBar;
	private SliderLocation_Layer	_layer_SliderLocation;
	private SliderPath_Layer		_layer_SliderPath;
	private TileGridLayerMT			_layer_TileInfo;
	private TourLayer					_layer_Tour;

	private OkHttpFactoryMT			_httpFactory;

	private long						_lastRenderTime;
	private String						_last_mf_themeFilePath = "uninitialized"; //$NON-NLS-1$
	private String						_last_mf_theme_styleID = "";	 //$NON-NLS-1$
	private Boolean               _last_is_mf_Map = true;
	private String                _last_mf_mapFilePath = "uninitialized"; //$NON-NLS-1$
	private Boolean					_last_mf_IsThemeFromFile;

	private IRenderTheme				_mf_IRenderTheme;
	private float						_mf_TextScale = 0.75f;
	private float						_vtm_TextScale = 0.75f;
   private float                 _mf_UserScale = 2.50f;
   private float                 _vtm_UserScale = 2.0f;	
	
   ItemizedLayer<MarkerItem> _layer_MapBookmark;
   private MarkerToolkit _markertoolkit;
   private MarkerMode _markerMode = MarkerToolkit.MarkerMode.NORMAL; // MarkerToolkit.modeDemo or MarkerToolkit.modeNormal

	/**
	 * Is <code>true</code> when a tour marker is hit.
	 */
	private boolean					_isMapItemHit;

	/**
	 * Is <code>true</code> when maps is a mapsforgemap.
	 */	
	private boolean					_isOfflineMap = true;

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
				//debugPrint("############### Orientation: " +  _map25View.getOrientation());  //$NON-NLS-1$
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
		
      debugPrint("# create Layers: prefered language:           " + _mf_prefered_language);//$NON-NLS-1$
      debugPrint("# create Layers: Map Name:                    " +_selectedMapProvider.name); //$NON-NLS-1$
      debugPrint("# create Layers: Map ONLINE/OFFLINE:          " +_selectedMapProvider.is_mf_Map); //$NON-NLS-1$
      debugPrint("# create Layers: Map mf_MapFilepath:          " +_selectedMapProvider.mf_MapFilepath); //$NON-NLS-1$
      debugPrint("# create Layers: Map mf_ThemeFilepath:        " +_selectedMapProvider.mf_ThemeFilepath); //$NON-NLS-1$
      debugPrint("# create Layers: Map encoding:                " +_selectedMapProvider.tileEncoding.toString()); //$NON-NLS-1$
      debugPrint("# create Layers: prefered language:           " + _mf_prefered_language); //$NON-NLS-1$

		if (_selectedMapProvider.tileEncoding  != TileEncoding.MF) { // NOT mapsforge
			_isOfflineMap = false;
			final UrlTileSource tileSource = createTileSource(_selectedMapProvider, _httpFactory);
			debugPrint("# create Layers OSCI: step 1: " + mMap.layers().toString() + " size: " + mMap.layers().size());  // result 1
			//tileSource.getDataSource().dispose();
			//_l = mMap.setBaseMap(tileSource);

			//loadTheme(null);
			setupMap(_selectedMapProvider, tileSource);
         debugPrint("# create Layers OSCI: step 2: " + mMap.layers().toString() + " size: " + mMap.layers().size());  // result 3

			debugPrint("############# create Layers: is online map with theme: " + _selectedMapProvider.theme.name()); //$NON-NLS-1$
		} else {  //offline maps
			_isOfflineMap = true;
			//_httpFactory = null;
			//_mf_mapFilePath = checkFile(_selectedMapProvider.mf_MapFilepath);
			_mf_mapFilePath = _selectedMapProvider.mf_MapFilepath;
			if (!checkMapFile(new File(_mf_mapFilePath))) {
				throw new IllegalArgumentException("cannot read mapfile: " + _mf_mapFilePath); //$NON-NLS-1$
			}
			
			final MultiMapFileTileSource tileSource = getMapFile(_mf_mapFilePath);
			if (_tileSourceOfflineMapCount == 0) {
				throw new IllegalArgumentException("cannot read (any) mapfile: " + _selectedMapProvider.mf_MapFilepath);
			}
			//_l = mMap.setBaseMap(tileSource);
			
			
         debugPrint("# create Layers: mf step 1: " + mMap.layers().toString() + " size: " + mMap.layers().size());  // result 1
         // here we have only one layer, that we need for mapsource switching
			
         // the next block was active for single mapsource
			//final MapFileTileSource tileSource = new MapFileTileSource();
			//tileSource.setMapFile(_mf_mapFilePath);
			//tileSource.setPreferredLanguage(_mf_prefered_language);
         
         debugPrint("# create Layers: mf step 2: " + mMap.layers().toString() + " size: " + mMap.layers().size());  // result 1
			//_l = mMap.setBaseMap(tileSource);
		
         debugPrint("# create Layers: mf step 3: " + mMap.layers().toString() + " size: " + mMap.layers().size());  // result ?
			
			
			_mf_IsThemeFromFile = _selectedMapProvider.mf_IsThemeFromFile;
			_mf_themeFilePath = checkFile(_selectedMapProvider.mf_ThemeFilepath); //check theme path, null when not found
			_mf_theme_styleID = _selectedMapProvider.mf_ThemeStyle;

			debugPrint("# create Layers: is mapsforge map using : " + _mf_mapFilePath); //$NON-NLS-1$
			debugPrint("# create Layers: is mapsforge theme : " + _mf_themeFilePath); //$NON-NLS-1$
			debugPrint("# create Layers: is mapsforge style : " + _mf_theme_styleID); //$NON-NLS-1$
					
			//setupMap(_selectedMapProvider, tileSource); //single map file
			setupMap(_selectedMapProvider, tileSource); //multi map file
			
			loadTheme(_mf_theme_styleID);
			
         debugPrint("# create Layers: mf step 4: " + mMap.layers().toString() + " size: " + mMap.layers().size());  // result ?
			
			debugPrint("# create Layers: leaving"); //$NON-NLS-1$
		}  // end mf_ maps

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
	}  // end createLayers()

	protected void loadTheme(final String styleId) {
		debugPrint("####### loadtheme: entering styleID: " + styleId); //$NON-NLS-1$
		
		if (!_isOfflineMap) { // NOT mapsforge
			debugPrint("####### loadtheme: is online map setting textscale " +   _vtm_TextScale); //$NON-NLS-1$
			//CanvasAdapter.textScale = _vtm_TextScale;
			// if problems with switching themes via keyboard, maybe this block is the problem
			/*if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
				debugPrint("############# setMapProvider: onlinemap using internal theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
				mMap.setTheme((ThemeFile) _selectedMapProvider.theme);			
			} else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
				debugPrint("############# setMapProvider: onlinemap using internal default theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
				mMap.setTheme(VtmThemes.DEFAULT);
			}*/
			mMap.clearMap();
			mMap.updateMap(true);
		}
		
		else {  //is mapsforge map

			//debugPrint("####### loadtheme: is offline map setting textscale " +   _mf_TextScale); //$NON-NLS-1$
			//debugPrint("####### loadtheme: is offline map IsThemeFileFromFile " +  _mf_IsThemeFromFile); //$NON-NLS-1$
			
			if (_mf_IsThemeFromFile) { //external theme
			   debugPrint("####### loadtheme: using external theme"); //$NON-NLS-1$
				mMap.setTheme(new ExternalRenderTheme(_mf_themeFilePath, new XmlRenderThemeMenuCallback() {
					@Override
					public Set<String> getCategories(XmlRenderThemeStyleMenu renderThemeStyleMenu) {
						String style = styleId != null ? styleId : renderThemeStyleMenu.getDefaultValue();
						XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
						if(THEME_STYLE_ALL.equals(styleId)) {
							return null;
						} else if (renderThemeStyleLayer == null) {
							System.err.println("####### loadtheme:  Invalid style \"" + style + "\" so i show all styles"); //$NON-NLS-1$ //$NON-NLS-2$
							return null;
						} else 
						   ;
							debugPrint("####### loadtheme:  selected Style: " + renderThemeStyleLayer.getTitle(_mf_prefered_language)); //$NON-NLS-1$
						Set<String> categories = renderThemeStyleLayer.getCategories();
						int n = 0;
						int overlaycount = renderThemeStyleLayer.getOverlays().size();
						for (XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
							if (overlay.isEnabled())
								categories.addAll(overlay.getCategories());
						}
						debugPrint("####### loadtheme: leaving"); //$NON-NLS-1$
						return categories;
					}
				}));
			} else { //internal theme
				if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
					debugPrint("####### loadtheme: using internal theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
					mMap.setTheme((ThemeFile) _selectedMapProvider.theme);				
				} else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
					debugPrint("####### loadtheme: using internal default theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
					mMap.setTheme(VtmThemes.DEFAULT);
				}
				_mf_IsThemeFromFile = false;
			}
			//mMap.clearMap();
			mMap.updateMap(true);

		}

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
	


	public OsmTileLayerMT getLayer_BaseMap() {
		return _layer_BaseMap;
	}

	public BuildingLayer getLayer_Building() {
		return _layer_Building;
	}

	public S3DBLayer getLayer_S3DB() {
		return _layer_S3DB_Building;
	}
	
   public ItemizedLayer<MarkerItem> getLayer_MapBookmark() {
      return _layer_MapBookmark;
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

	public MarkerLayer getLayer_TourMarker() {
		return _layer_Marker;
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

	Map25View getMap25View() {
		return _map25View;
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
			return (ThemeFile) mapProvider.theme;
		}
	}





	public void onModifyMarkerConfig() {

		updateUI_MarkerLayer();

		updateUI_MapBookmarkLayer();
		
		mMap.render();
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

		_state.put(STATE_MAP_POS_X + stateSuffixName, mapPosition.x);
		_state.put(STATE_MAP_POS_Y + stateSuffixName, mapPosition.y);
		_state.put(STATE_MAP_POS_BEARING + stateSuffixName, mapPosition.bearing);
		_state.put(STATE_MAP_POS_SCALE + stateSuffixName, mapPosition.scale);
		_state.put(STATE_MAP_POS_TILT + stateSuffixName, mapPosition.tilt);
		_state.put(STATE_MAP_POS_ZOOM_LEVEL + stateSuffixName, mapPosition.zoomLevel);
	}

	public void setLayer_HillShading_Opacity(int layer_HillShading_Opacity) {
      _layer_HillShading_Opacity = layer_HillShading_Opacity;
   }  

	public void setMapProvider(final Map25Provider mapProvider) {
	   //saveState();  //doesnt help
	   Boolean onlineOfflineStatusHasChanged = false;
      _mf_mapFilePath =  mapProvider.mf_MapFilepath;
      debugPrint("############# setMapProvider entering setMapProvider: _mf_mapFilePath:" + _mf_mapFilePath + " _last_mf_mapFilePath: " + _last_mf_mapFilePath); //$NON-NLS-1$
      //debugPrint("############# setMapProvider layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$


      debugPrint("############# setMapProvider layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$
	   debugPrint("############# setMapProvider entering setMapProvider"); //$NON-NLS-1$
		debugPrint("############# setMapProvider layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size()); //$NON-NLS-1$ //$NON-NLS-2$
		/*boolean label_layer_was_enabled = getLayer_Label().isEnabled();
		boolean building_layer_was_enabled = getLayer_Building().isEnabled();
		mMap.layers().remove(_layer_Label);
		mMap.layers().remove(_layer_Building);*/
		
		//if NOT mapsforge map
      if (!_mf_mapFilePath.equals(_last_mf_mapFilePath) || mapProvider.is_mf_Map !=  _last_is_mf_Map  ) {  //only reloading layers when neccercary 


         debugPrint("############# setMapProvider switchin offline/one to: " + mapProvider.is_mf_Map); //$NON-NLS-1$
         onlineOfflineStatusHasChanged = true;
         removeLayers();
      }
      
      //if NOT mapsforge map      
		//debugPrint("############# setMapProvider MapProviderENCODING: " + mapProvider.tileEncoding); //$NON-NLS-1$
		if (mapProvider.tileEncoding  != TileEncoding.MF) { // NOT mapsforge

			this._isOfflineMap = false;

			CanvasAdapter.textScale = _vtm_TextScale;
			CanvasAdapter.userScale = _vtm_UserScale;
			debugPrint("############# setMapProvider: setMapProvider NOT mf Map"); //$NON-NLS-1$
			//debugPrint("############# setMapProvider: using internal default theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
			final UrlTileSource tileSource = createTileSource(mapProvider, _httpFactory);
			_layer_BaseMap.setTileSource(tileSource);
			//_l.setRenderTheme(ThemeLoader.load(VtmThemes.DEFAULT));  //if active, key 1-5 nor working, if not active "ERROR VectorTileLoader - no theme is set"
			//debugPrint("############# setMapProvider: set theme to-> " + mapProvider.name); //$NON-NLS-1$

         if (onlineOfflineStatusHasChanged) {
            setupMap(mapProvider, tileSource);
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
         
			if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
				//debugPrint("############# setMapProvider: onlinemap using internal theme: " + mapProvider.theme); //$NON-NLS-1$
				mMap.setTheme((ThemeFile) mapProvider.theme);			
			} else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
				//debugPrint("############# setMapProvider: onlinemap using internal default theme: " + mapProvider.theme); //$NON-NLS-1$
				mMap.setTheme(VtmThemes.DEFAULT);
			}
			//_layer_Building = new BuildingLayer(mMap, _layer_BaseMap);
			
	//		mMap.clearMap();
	//		mMap.updateMap(true);

			_mf_themeFilePath = ""; // so if mf is next themefile is parsed //$NON-NLS-1$
		} else { //it mapsforge map
			this._isOfflineMap = true;
			CanvasAdapter.textScale = _mf_TextScale;
			CanvasAdapter.userScale = _mf_UserScale;
			debugPrint("############# setMapProvider: setMapProvider its mf Map"); //$NON-NLS-1$
			//final MapFileTileSource tileSource = new MapFileTileSource();
			//final MultiMapFileTileSource tileSource = getMapFile(mapProvider.mf_MapFilepath);
			
			debugPrint("############# setMapProvider: setMap   to      " + mapProvider.mf_MapFilepath); //$NON-NLS-1$
			//debugPrint("############# setMapProvider: setTheme to      " + mapProvider.mf_ThemeFilepath); //$NON-NLS-1$
			//debugPrint("############# setMapProvider: setStyle to      " + mapProvider.mf_ThemeStyle); //$NON-NLS-1$
			debugPrint("############# setMapProvider: is_mf_Map     " + mapProvider.is_mf_Map); //$NON-NLS-1$
			debugPrint("############# setMapProvider: isThemeFromFile  " + mapProvider.mf_IsThemeFromFile); //$NON-NLS-1$
			//debugPrint("############# setMapProvider: name             " + mapProvider.name); //$NON-NLS-1$

			_mf_mapFilePath =  mapProvider.mf_MapFilepath;
			
			if (!checkMapFile(new File(_mf_mapFilePath))) {
				StatusUtil.showStatus(String.format(
						"Cannot read map file \"%s\" in map provider \"%s\"",  //$NON-NLS-1$
						_mf_mapFilePath, 
						mapProvider.name));

				throw new IllegalArgumentException("############# setMapProvider: cannot read mapfile: " + _mf_mapFilePath); //$NON-NLS-1$
			} else {
			   ;
				debugPrint("############# setMapProvider: Map Path: " + _mf_mapFilePath); //$NON-NLS-1$
			}

			/* When switching subthemes buildings disapear and
			 * apears again when switching also the mapprovider
			 * codeblock mustbe outside of the if statement otherwise all themes are allways on
			 */
			
			//singlefile mapsource
			/*final MapFileTileSource tileSource = new MapFileTileSource();
			tileSource.setMapFile(_mf_mapFilePath);
			tileSource.setPreferredLanguage(_mf_prefered_language);
			_layer_BaseMap.setTileSource(tileSource);*/
			_mf_mapFilePath = checkFile(mapProvider.mf_MapFilepath);
			
         final MultiMapFileTileSource tileSource = getMapFile(_mf_mapFilePath);
         if (_tileSourceOfflineMapCount == 0) {
            throw new IllegalArgumentException("cannot read (any) mapfile: " + _selectedMapProvider.mf_MapFilepath);
         }
         //_l = mMap.setBaseMap(tileSource);
         
         tileSource.setPreferredLanguage(_mf_prefered_language);
         _layer_BaseMap.setTileSource(tileSource);
         
         if (onlineOfflineStatusHasChanged) {
            // when this code inside this if staements, subthemes are not working and always on
            /*final MapFileTileSource tileSource = new MapFileTileSource();
            tileSource.setMapFile(_mf_mapFilePath);
            tileSource.setPreferredLanguage(_mf_prefered_language);
            _layer_BaseMap.setTileSource(tileSource);*/
            setupMap(mapProvider, tileSource);
            setupMap_Layers();
            //restoreState();
            
            updateUI_MapBookmarkLayer();
            
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
					debugPrint("############# setMapProvider: Theme not found: " + _mf_mapFilePath + " using default DEFAULT"); //$NON-NLS-1$ //$NON-NLS-2$
					mMap.setTheme(VtmThemes.DEFAULT);   // ThemeLoader.load(_mf_themeFilePath));
				} else {
					if (!_mf_themeFilePath.equals(_last_mf_themeFilePath) || !_mf_theme_styleID.equals(_last_mf_theme_styleID) || _mf_IsThemeFromFile != _last_mf_IsThemeFromFile ) {  //only parsing when different file	
					   debugPrint("############# setMapProvider: Theme loader started"); //$NON-NLS-1$
						this._mf_IRenderTheme = ThemeLoader.load(_mf_themeFilePath);
						debugPrint("############# setMapProvider: Theme loader done, now activating..."); //$NON-NLS-1$
						_layer_BaseMap.setRenderTheme(_mf_IRenderTheme);
						////mMap.setTheme(_mf_IRenderTheme);
						loadTheme(mapProvider.mf_ThemeStyle);  //whene starting with onlinemaps and switching to mf, osmarender is used ??? when uncommented it ok
						debugPrint("############# setMapProvider: ...activaded"); //$NON-NLS-1$
						//_mf_IsThemeFromFile = true;
					} else {
					   ;
						debugPrint("############# setMapProvider: mapprovider has the same theme file and style"); //$NON-NLS-1$
					}
				}
			} else { //internal theme
				if (_selectedMapProvider.theme != null && _selectedMapProvider.theme != VtmThemes.MAPZEN && _selectedMapProvider.theme != VtmThemes.OPENMAPTILES) {
					debugPrint("############# setMapProvider: using internal theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
					mMap.setTheme((ThemeFile) _selectedMapProvider.theme);				
				} else { //when null or when not working MAPZEN or OPENMAPTILES is selected, using DEFAULT theme instead
					debugPrint("############# setMapProvider: using internal default theme: " + _selectedMapProvider.theme); //$NON-NLS-1$
					mMap.setTheme(VtmThemes.DEFAULT);
				}
				_mf_IsThemeFromFile = false;
			}
		}
		
      
      mMap.clearMap();
      mMap.updateMap(true);	

		//debugPrint("############# setMapProvider: set language : " + _mf_prefered_language); //$NON-NLS-1$
      this._last_mf_themeFilePath = _mf_themeFilePath;
		this._last_mf_theme_styleID = _mf_theme_styleID;
		this._last_mf_mapFilePath = _mf_mapFilePath;
		this._last_mf_IsThemeFromFile = _mf_IsThemeFromFile;
		this._last_is_mf_Map = _isOfflineMap;
		_selectedMapProvider = mapProvider;
		debugPrint("############# setMapProvider leaving: layers now: " + mMap.layers().toString() + " size: " + mMap.layers().size());
	}

	  /**
    * when switching from offline to online or Vice versa all layers must be removed first
    * this is done here, after that all layers must be added again. but that is a different story.
    */
   private void removeLayers() {
      debugPrint("### removeLayers: layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size());
      //saveState();
      for( int n = mMap.layers().size() - 1; n > 0 ;n--) { 
         debugPrint("### removeLayers: layer " + n + "/" + mMap.layers().size()+ " " + mMap.layers().get(n).toString());
         mMap.layers().remove(n);
      }
      debugPrint("### removeLayers: layers after: " + mMap.layers().toString() + " size: " + mMap.layers().size());
   }
	
	/**
	 * setupMap for online maps
	 * @param mapProvider
	 * @param tileSource
	 */
	private void setupMap(final Map25Provider mapProvider, final UrlTileSource tileSource) {
	   debugPrint("############# setupMap:  online entering"); //$NON-NLS-1$
		
      CanvasAdapter.textScale = _vtm_TextScale;
      CanvasAdapter.userScale = _vtm_UserScale;
		
      //_l = mMap.setBaseMap(tileSource);
      
		_layer_BaseMap = new OsmTileLayerMT(mMap);

		_tileManager = _layer_BaseMap.getManager();

		_layer_BaseMap.setTileSource(tileSource);
		
		//_l.setRenderTheme(ThemeLoader.load(VtmThemes.DEFAULT));  //if active, key 1-5 nor working, if not active "ERROR VectorTileLoader - no theme is set"

// THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);

		mMap.setBaseMap(_layer_BaseMap);

		//setupMap_Layers();  // this was prior to s3db

		//debugPrint("############# setupMap:  mMap.setTheme(getTheme(mapProvider))" + getTheme(mapProvider)); //$NON-NLS-1$
		//debugPrint("############# setupMap:  Map25ProviderManager.getDefaultTheme(TileEncoding.VTM)" + Map25ProviderManager.getDefaultTheme(TileEncoding.VTM));  //$NON-NLS-1$
		mMap.setTheme(getTheme(mapProvider));
		//mMap.setTheme((ThemeFile) Map25ProviderManager.getDefaultTheme(TileEncoding.VTM));

		debugPrint("############# setupMap:  leaving"); //$NON-NLS-1$
	}


	/**
	 * setupMap for mapsforge
	 * @param mapProvider
	 * @param tileSource
	 */
	//private void setupMap(final Map25Provider mapProvider, final MapFileTileSource tileSource) {
	private void setupMap(final Map25Provider mapProvider, final MultiMapFileTileSource tileSource) {	
	   debugPrint("############# setupMap:  mapsforge entering"); //$NON-NLS-1$
      debugPrint("############# setupMap: layers before: " + mMap.layers().toString() + " size: " + mMap.layers().size());		
      CanvasAdapter.textScale = _mf_TextScale;
      CanvasAdapter.userScale = _mf_UserScale;
		
      _mf_IsThemeFromFile = mapProvider.mf_IsThemeFromFile;
      _mf_themeFilePath = checkFile(mapProvider.mf_ThemeFilepath); //check theme path, null when not found
      _mf_theme_styleID = mapProvider.mf_ThemeStyle;    
      _mf_mapFilePath = mapProvider.mf_MapFilepath;
   
      debugPrint("############# setupMap: is mapsforge map using : " + _mf_mapFilePath); //$NON-NLS-1$
      debugPrint("############# setupMap: is mapsforge theme : " + _mf_themeFilePath); //$NON-NLS-1$
      debugPrint("############# setupMap: is mapsforge style : " + _mf_theme_styleID); //$NON-NLS-1$
      debugPrint("############# setupMap: is mapsforge mapfilepath : " + _mf_mapFilePath); //$NON-NLS-1$
      
      
		_layer_BaseMap = new OsmTileLayerMT(mMap);

		_tileManager = _layer_BaseMap.getManager();

		//_l = mMap.setBaseMap(tileSource);
		
		_layer_BaseMap.setTileSource(tileSource);
		
		_layer_BaseMap.setRenderTheme(ThemeLoader.load(VtmThemes.DEFAULT));  //to avoid errors

		// THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);		

		mMap.setBaseMap(_layer_BaseMap);

		//_mf_mapFilePath = checkFile(_selectedMapProvider.mf_MapFilepath);
		//_mf_mapFilePath = _selectedMapProvider.mf_MapFilepath;
		
		if (!checkMapFile(new File(_mf_mapFilePath))) {
			throw new IllegalArgumentException("cannot read mapfile: " + _mf_mapFilePath); //$NON-NLS-1$
		} else {
		   ;
			debugPrint("############# setupMap: Map Path: " + _mf_mapFilePath); //$NON-NLS-1$
		}
		
		/*if (_tileSourceOfflineMapCount == 0) {
			;
			//throw new IllegalArgumentException("cannot read mapfile: " + _selectedMapProvider.mf_MapFilepath); //$NON-NLS-1$
		} else {
			//debugPrint("############# setupMap: Map Path: " + _mf_mapFilePath); //$NON-NLS-1$
		}*/

		//_mf_themeFilePath = checkFile(_selectedMapProvider.mf_ThemeFilepath);

		if (_mf_themeFilePath == null) {
			debugPrint("############# setupMap:  Theme not found: " + _mf_themeFilePath + " using default OSMARENDER"); //$NON-NLS-1$ //$NON-NLS-2$
			//mMap.setTheme(VtmThemes.OSMARENDER);   // ThemeLoader.load(_mf_themeFilePath));
		} else {
			;
			//_l.setRenderTheme(ThemeLoader.load(VtmThemes.DEFAULT));  //to avoid errors
			this._mf_IRenderTheme = ThemeLoader.load(_mf_themeFilePath);  // because of changes in loadtheme
			_layer_BaseMap.setRenderTheme(_mf_IRenderTheme);
			mMap.setTheme(ThemeLoader.load(_mf_themeFilePath)); //neccercary?seem so
			////loadTheme(mapProvider.mf_ThemeStyle); //neccercary?
		}

		//setupMap_Layers();	  // this was prior to s3db

		debugPrint("############# setupMap:  leaving"); //$NON-NLS-1$
	}

	private void setupMap_Layers() {
	   debugPrint("################ setupMap_Layers:  entering"); //$NON-NLS-1$
	   final Layers layers = mMap.layers();
	   final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
	   
	   // hillshading with 1MB RAM Cache, using existing _httpfactory with diskcache
	   _hillshadingSource =  DefaultSources.HIKEBIKE_HILLSHADE
	         .httpFactory(_httpFactory)
	         .zoomMin(1)
	         .zoomMax(16)
	         .build();  
	   _layer_HillShading = new BitmapTileLayer(mMap, _hillshadingSource, 1 << 20);
	   _layer_HillShading.setEnabled(false);
	   mMap.layers().add(_layer_HillShading);

	   // tour
	   _layer_Tour = new TourLayer(mMap);
	   _layer_Tour.setEnabled(false);
	   layers.add(_layer_Tour);

	   // slider path
	   _layer_SliderPath = new SliderPath_Layer(mMap);
	   _layer_SliderPath.setEnabled(false);
	   layers.add(_layer_SliderPath);


      //buildings
      /**
       * here i have to investigate
       * with this code i got always good S3DB, but online buildings did not look good with: "new S3DBLayer(mMap,_layer_BaseMap, true)"
       * 
       */
//    // Buildings or S3DB  Block I
      _layer_S3DB_Building = new S3DBLayer(mMap,_layer_BaseMap, true);  //this is working with subtheme  switching, but no online buildings anymore
      //_layer_S3DB_Building = new S3DBLayer(mMap,_l, true);  //is working online and offline, but S3DB only until subtheme switching
      _layer_Building = new BuildingLayer(mMap, _layer_BaseMap, false, true);
      
		if(_isOfflineMap) {
//			// S3DB
			_layer_S3DB_Building.setEnabled(true);
			_layer_S3DB_Building.setColored(true);
			debugPrint("################ setupMap_Layers: adding S3DBlayer "); //$NON-NLS-1$
//			//_l.setRenderTheme(_mf_IRenderTheme); //again??
			//layers.remove(_layer_Building);
			layers.add(_layer_S3DB_Building);
		} else {
//			// building
			_layer_Building.setEnabled(true);
			debugPrint("################ setupMap_Layers:Building Layer "); //$NON-NLS-1$
			//layers.remove(_layer_S3DB_Building);
			layers.add(_layer_Building);
		}

	   // building Block II
	   //_layer_Building = new BuildingLayer(mMap, _layer_BaseMap, true, true);
/*	   _layer_Building = new BuildingLayer(mMap, _layer_BaseMap, false, true);
	   _layer_Building.setEnabled(false);
	   layers.add(_layer_Building); */ //prior to s3db


	   // label
	   _layer_Label = new LabelLayerMT(mMap, _layer_BaseMap);
	   _layer_Label.setEnabled(false);
	   layers.add(_layer_Label);

      // MapBookmarks
      //debugPrint("################ setupMap_Layers: calling constructor"); //$NON-NLS-1$
      _markertoolkit = new MarkerToolkit(MarkerToolkit.MarkerShape.STAR);
      if (config.isMarkerClustered) {
         _layer_MapBookmark = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), _markertoolkit._markerRendererFactory, this);
      } else {
         _layer_MapBookmark = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), _markertoolkit._symbol, this);
      }
      List<MarkerItem> pts = _markertoolkit.createMarkerItemList(_markerMode);
      _layer_MapBookmark.addItems(pts);
      _layer_MapBookmark.setEnabled(false);
      layers.add(_layer_MapBookmark);	   
	   
	   // marker
	   _layer_Marker = new MarkerLayer(mMap, this);
	   _layer_Marker.setEnabled(false);
	   layers.add(_layer_Marker);

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

      debugPrint("################ setupMap_Layers: " + mMap.layers().toString() + " size: " + mMap.layers().size());
	   debugPrint("################ setupMap_Layers:  leaving"); //$NON-NLS-1$

	}

	void stop() {

		_lwjglApp.stop();
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

	public void updateUI_MapBookmarkLayer() {
	   final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();
	   final Layers layers = mMap.layers();
	   final int layer_index_MapBookmark = layers.indexOf(_layer_MapBookmark);
	   final boolean isShowMapBookmark = config.isShowMapBookmark;
	   debugPrint("# updateUI_MapBookmarkLayer(): entering"); //$NON-NLS-1$
	   if (config.isMarkerClustered != _markertoolkit._isMarkerClusteredLast) { // only recreate MapBookmarkLayer when changed in UI
	      //debugPrint("# updateUI_MapBookmarkLayer(): index was before: " + layer_index_MapBookmark); //$NON-NLS-1$
	      layers.remove(_layer_MapBookmark); 
	      if (config.isMarkerClustered) {
	         _layer_MapBookmark = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), _markertoolkit._markerRendererFactory, this);
	      } else {
	         _layer_MapBookmark = new ItemizedLayer<>(mMap, new ArrayList<MarkerItem>(), _markertoolkit._symbol, this);
	      }
	      layers.add(layer_index_MapBookmark, _layer_MapBookmark);
	      //debugPrint("# updateUI_MapBookmarkLayer(): index is now: " + layer_index_MapBookmark); //$NON-NLS-1$
	   } else {
	      _layer_MapBookmark.removeAllItems();
	   }  
	   //_layer_Bookmark.removeAllItems();
	   List<MarkerItem> pts = _markertoolkit.createMarkerItemList(_markerMode);
	   _layer_MapBookmark.addItems(pts);
	   _layer_MapBookmark.setEnabled(isShowMapBookmark);
	   _markertoolkit._isMarkerClusteredLast = config.isMarkerClustered;
	   //
	}

	
	/**
	 * gget a sorted list with mapsforgemap files
	 * @param <MultiMapDataStore>
	 * @param filename
	 * @return files[]
	 * {@link http://www.avajava.com/tutorials/lessons/how-do-i-sort-an-array-of-files-according-to-their-sizes.html}
	 * 
	 */
	public MultiMapFileTileSource getMapFile(String filename) {
		
		File file = new File(filename);
		File directory = new File(file.getParent());
		File[] files = directory.listFiles(new FilenameFilter() {
		    public boolean accept(File directory, String name) {
		        return name.toLowerCase().endsWith(".map"); //$NON-NLS-1$
		    }
		});
		
		debugPrint("#### getMapFile: basepath: " + directory);	 //$NON-NLS-1$
		
		//Arrays.sort(files, SizeFileComparator.SIZE_COMPARATOR); // sort mapsfiles size
		
		MultiMapFileTileSource mMFileTileSource = new MultiMapFileTileSource ();   //DataPolicy.RETURN_ALL);
		MapFileTileSource tileSourceOfflinePrimary = new MapFileTileSource();
		
		if(checkMapFile(file)){
			tileSourceOfflinePrimary.setMapFile(file.getAbsolutePath());
			tileSourceOfflinePrimary.setPreferredLanguage(_mf_prefered_language);
			mMFileTileSource.add(tileSourceOfflinePrimary);   // adding primary map first
			_tileSourceOfflineMapCount += 1;
			debugPrint("#### getMapFile: Adding primary map: " + file + " size: " +  file.length() + " bytes)"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
		   ;
			debugPrint("#### getMapFile: primary file missing: " + file.getAbsolutePath()); //$NON-NLS-1$
		}

		//debugPrint("Adding: " + file + " size: " +  	FileUtils.byteCountToDisplaySize(file.length()) + "(" + file.length() + " bytes)");
		
		for (File f : files)
      {
			if(checkMapFile(f)) {
				if(!f.getAbsolutePath().equalsIgnoreCase(filename)) { //add all mapfiles except the primary map, which is already added
					MapFileTileSource tileSourceOffline = new MapFileTileSource();
					tileSourceOffline.setMapFile(f.getAbsolutePath());
					tileSourceOffline.setPreferredLanguage(_mf_prefered_language);
					mMFileTileSource.add(tileSourceOffline);
					_tileSourceOfflineMapCount += 1;
					//long size = FileUtils.sizeOf(f);
					//debugPrint("Adding: " + f + " size: " +  	FileUtils.byteCountToDisplaySize(size) + "(" + size + " bytes)");
					debugPrint("#### getMapFile: Adding secondary map: " + f + " size: " +  f.length() + " bytes), Total Maps: " + _tileSourceOfflineMapCount); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
      }	
		return mMFileTileSource;
	}	
	
	public void debugPrint(String debugText) {
	   if(debugMode == DebugMode.ON) {
	      System.out.println(UI.timeStamp() + " map25: " + debugText);//$NON-NLS-1$
	   }
	}
	
	
	/**
	 * Checks if a given file is a valid mapsforge file
	 * @param file2check
	 * @return true, when file is ok
	 */
	public Boolean checkMapFile(File file2check) {
		Boolean result = false;
		MapFileTileSource mapFileSource = new MapFileTileSource();
		mapFileSource.setMapFile(file2check.getAbsolutePath());
		OpenResult mOpenResult = mapFileSource.open();
		mapFileSource.close();
		result = mOpenResult.isSuccess();
		if (!mOpenResult.isSuccess()) {
		   ;
		 debugPrint("### checkMapFile: not adding: " + file2check.getAbsolutePath() + " " + mOpenResult.getErrorMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return result;
	}
	
/**
 * checks if a file is a file
 * @param FilePath
 * @return absolut file path as string
 */
	public String checkFile(String FilePath) {  	

		File file = new File(FilePath);
		if (!file.exists()) {
		  		   debugPrint("## checkFile: file not exist: " +  file.getAbsolutePath()); //$NON-NLS-1$
			return null;
			//throw new IllegalArgumentException("file does not exist: " + file);
		} else if (!file.isFile()) {
		   debugPrint("## checkFile: is not a file: " +  file.getAbsolutePath()); //$NON-NLS-1$
			return null;
			//throw new IllegalArgumentException("not a file: " + file);
		} else if (!file.canRead()) {
		   debugPrint("## checkFile: can not read file: " +  file.getAbsolutePath()); //$NON-NLS-1$
			return null;
			//throw new IllegalArgumentException("cannot read file: " + file);
		}
		//debugPrint("############ check file:  file_path: " +  file.getAbsolutePath()); //$NON-NLS-1$
		//return file;
		return file.getAbsolutePath();
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

	@Override
	public boolean onItemSingleTapUp(int index, MarkerItem item) {
	   if (item.getMarker() == null)
	      ;
	   // item.setMarker(symbol);
	   else
	      ;
	   // item.setMarker(null);

	   //debugPrint("Marker tap " + item.getTitle()); //$NON-NLS-1$
	   return true;
	}

	@Override
	public boolean onItemLongPress(int index, MarkerItem item) {
	   if (item.getMarker() == null)
	      ;
	   // item.setMarker(symbol);
	   else
	      ;
	   // item.setMarker(null);

	   //debugPrint("Marker long press " + item.getTitle()); //$NON-NLS-1$
	   return true;
	}

	@Override
	public boolean onItemLongPress(final int index, final MapMarker item) {

// System.out.println(
//       (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//       + ("\tonItemLongPress")
//       + ("\tindex:" + index)
//       + ("\t_isMapItemHit:" + _isMapItemHit + " -> true")
//       //
//       );
// // TODO remove SYSTEM.OUT.PRINTLN
//
// _isMapItemHit = true;
//
// return true;

	   return false;
	}

	@Override
	public boolean onItemSingleTapUp(final int index, final MapMarker item) {

// System.out.println(
//       (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//       + ("\tonItemSingleTapUp")//
//       + ("\tindex:" + index)
//       + ("\t_isMapItemHit:" + _isMapItemHit + " -> true")
//       //Pref_Map25_Encoding_Mapsforge
//       );
// // TODO remove SYSTEM.OUT.PRINTLN
//
// _isMapItemHit = true;
//
// return true;
	   return false;
	}

	@Override
	public void dispose() {
	   // stop loading tiles
	   _tileManager.clearJobs();  
	   saveState();
	   super.dispose();
	}

	@Override
	protected void initGLAdapter(GLVersion arg0) {
	   // TODO Auto-generated method stub

	}

}
