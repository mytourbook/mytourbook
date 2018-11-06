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
import java.util.Locale;
//import java.io.FileNotFoundException;
import java.util.Set;

import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25TileSource.Builder;
import net.tourbook.map25.OkHttpEngineMT.OkHttpFactoryMT;
import net.tourbook.map25.layer.labeling.LabelLayerMT;
import net.tourbook.map25.layer.marker.MapMarker;
import net.tourbook.map25.layer.marker.MarkerConfig;
import net.tourbook.map25.layer.marker.MarkerLayer;
import net.tourbook.map25.layer.marker.MarkerLayer.OnItemGestureListener;
import net.tourbook.map25.layer.marker.MarkerRenderer;
import net.tourbook.map25.layer.tourtrack.SliderLocation_Layer;
import net.tourbook.map25.layer.tourtrack.SliderPath_Layer;
import net.tourbook.map25.layer.tourtrack.TourLayer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Display;
import org.oscim.awt.AwtGraphics;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.CanvasAdapter;
//import org.ocsim.backend
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.event.Event;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GestureHandlerImpl;
import org.oscim.gdx.LwjglGL20;
import org.oscim.gdx.MotionHandler;
import org.oscim.layers.tile.TileManager;
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
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mapfile.MapFileTileSource;


import org.oscim.utils.Parameters;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import okhttp3.Cache;

public class Map25App extends GdxMap implements OnItemGestureListener {

	private static final String		STATE_MAP_POS_X						= "STATE_MAP_POS_X";					//$NON-NLS-1$
	private static final String		STATE_MAP_POS_Y						= "STATE_MAP_POS_Y";					//$NON-NLS-1$
	private static final String		STATE_MAP_POS_ZOOM_LEVEL			= "STATE_MAP_POS_ZOOM_LEVEL";			//$NON-NLS-1$
	private static final String		STATE_MAP_POS_BEARING				= "STATE_MAP_POS_BEARING";				//$NON-NLS-1$
	private static final String		STATE_MAP_POS_SCALE					= "STATE_MAP_POS_SCALE";				//$NON-NLS-1$
	private static final String		STATE_MAP_POS_TILT					= "STATE_MAP_POS_TILT";					//$NON-NLS-1$
	private static final String		STATE_SELECTED_MAP25_PROVIDER_ID	= "STATE_SELECTED_MAP25_PROVIDER_ID";	//$NON-NLS-1$

	private static final String		STATE_SUFFIX_MAP_CURRENT_POSITION	= "MapCurrentPosition";					//$NON-NLS-1$
	static final String				STATE_SUFFIX_MAP_DEFAULT_POSITION	= "MapDefaultPosition";					//$NON-NLS-1$

	private static IDialogSettings	_state;
	
	private static String	_mf_mapFilePath = null;
	private static String	_mf_themeFilePath = null;
	private static String	_mf_theme_styleID = null;

	private static Map25View		_map25View;
	private static LwjglApplication	_lwjglApp;

	private String 					_mf_prefered_language = "en";
	
	private Map25Provider			_selectedMapProvider;
	private TileManager				_tileManager;

	private OsmTileLayerMT			_layer_BaseMap;   //extends extends VectorTileLayer
	private VectorTileLayer 		_layer_mf_VectorTileLayer_S3DB;
//	private OsmTileLayerMT			_layer_mf_VectorTileLayer_S3DB;
	
	private BuildingLayer			_layer_Building;
	private S3DBLayer					_layer_mf_S3DB_Building;
	
	private LabelLayerMT				_layer_Label;
	private MarkerLayer				_layer_Marker;
	private MapScaleBarLayer		_layer_ScaleBar;
	private TileGridLayerMT			_layer_TileInfo;
	private TourLayer					_layer_Tour;
	private SliderLocation_Layer	_layer_SliderLocation;
	private SliderPath_Layer		_layer_SliderPath;

	private OkHttpFactoryMT			_httpFactory;

	private long						_lastRenderTime;
	private String						_last_mf_themeFilePath = "";
	private String						_last_mf_theme_styleID = "";
	
	private IRenderTheme				_mf_IRenderTheme;
	private float						_mf_TextScale = 1.5f;
	
	/**
	 * Is <code>true</code> when a tour marker is hit.
	 */
	private boolean					_isMapItemHit;
	
	/**
	 * Is <code>true</code> when maps is a mapsforgemap.
	 */	
	private boolean					_is_mf_Map = true;

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
				//System.out.println("############### Orientation: " +  _map25View.getOrientation()); //always 0
			}
		});
	}

	/**
	 * Layer: Scale bar
	 */
	private MapScaleBarLayer createLayer_ScaleBar() {

		final DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap);

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

	/**
	 * this is only running once after program was started
	 */
	@Override
	public void createLayers() {
		_selectedMapProvider = restoreState_MapProvider();

		_map25View.updateUI_SelectedMapProvider(_selectedMapProvider);
		
		_mf_prefered_language = Locale.getDefault().getLanguage();  // eg.: "en"

		System.out.println("############# create Layers: Map Name: " +_selectedMapProvider.name);
		System.out.println("############# create Layers: Map URL:  " +_selectedMapProvider.url);
		System.out.println("############# create Layers: Map tilepath:  " +_selectedMapProvider.tilePath);
		System.out.println("############# create Layers: Map encoding: " +_selectedMapProvider.tileEncoding.toString());
		System.out.println("############# create Layers: prefered language: " + _mf_prefered_language);
		
		if (_selectedMapProvider.tileEncoding  != TileEncoding.MF) { // NOT mapsforge
			_is_mf_Map = false;
			_httpFactory = new OkHttpEngineMT.OkHttpFactoryMT();
			final UrlTileSource tileSource = createTileSource(_selectedMapProvider, _httpFactory);
			_layer_mf_VectorTileLayer_S3DB = mMap.setBaseMap(tileSource);
			setupMap(_selectedMapProvider, tileSource);
			System.out.println("############# create Layers: is online map: " + _selectedMapProvider.url);
		} else {  //mapsforge
			_is_mf_Map = true;
			_httpFactory = null;  // offline map, no http.
			_mf_mapFilePath = checkFile(_selectedMapProvider.url);
			if (_mf_mapFilePath == null) {
				throw new IllegalArgumentException("############# create Layers: cannot read mapfile: " + _selectedMapProvider.url);
			} else {
				System.out.println("############# create Layers: Map Path: " + _mf_mapFilePath);
			}
			_mf_themeFilePath = checkFile(_selectedMapProvider.tilePath); //check theme path, null when not found
			_mf_theme_styleID = _selectedMapProvider.apiKey;  // style inside theme eg elv-mtb

			final MapFileTileSource tileSource = new MapFileTileSource();	
			tileSource.setMapFile(_mf_mapFilePath);
			tileSource.setPreferredLanguage(_mf_prefered_language);
			_layer_mf_VectorTileLayer_S3DB = mMap.setBaseMap(tileSource);

			if (_mf_themeFilePath == null) { // no, themefile, using buildin osmarender
				System.out.println("############# create Layers: Theme not found: " + _mf_mapFilePath + " using default OSMARENDER");
				mMap.setTheme(VtmThemes.OSMARENDER);   // ThemeLoader.load(_mf_themeFilePath));
			} else {
				if (!_mf_themeFilePath.equals(_last_mf_themeFilePath) || !_mf_theme_styleID.equals(_last_mf_theme_styleID)) {  //only parsing when different file
					System.out.println("############# create Layers: themeloader started"); 			
					this._mf_IRenderTheme = ThemeLoader.load(_mf_themeFilePath);
					System.out.println("############# create Layers: themeloader done");
					_layer_mf_VectorTileLayer_S3DB.setRenderTheme(_mf_IRenderTheme);
					mMap.setTheme(_mf_IRenderTheme);
					loadTheme(_selectedMapProvider.apiKey);
				} else {
					System.out.println("############# create Layers: mapprovider has the same theme file");
				}
			}
			this._last_mf_themeFilePath = _mf_themeFilePath;
			this._last_mf_theme_styleID = _mf_theme_styleID;

			setupMap(_selectedMapProvider, tileSource);
	
			System.out.println("############# create Layers: is mapsforge map using : " + _mf_mapFilePath);
			System.out.println("############# create Layers: is mapsforge theme : " + _mf_themeFilePath);
			System.out.println("############# create Layers: is mapsforge style : " + _mf_theme_styleID);
		}

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
	}

/*	protected void loadTheme(final String styleId) {
		if(_is_mf_Map) {
			mMap.setTheme(ThemeLoader.load(_mf_themeFilePath));//    load(_themeFile));
			mMap.clearMap();
			mMap.updateMap(true);
		} else {
			mMap.setTheme(VtmThemes.OSMARENDER);
		}
	}*/

	protected void loadTheme(final String styleId) {
		System.out.println("##### loadtheme: entering styleID: " + styleId);
		if(_is_mf_Map) {
			System.out.println("##### loadtheme: is mf map");
			mMap.setTheme(new ExternalRenderTheme(_mf_themeFilePath, new XmlRenderThemeMenuCallback() {
				// mMap.setTheme(new StreamRenderTheme("", getClass().getResourceAsStream("/assets/vtm/stylemenu.xml"), new XmlRenderThemeMenuCallback() {
				@Override
				public Set<String> getCategories(XmlRenderThemeStyleMenu renderThemeStyleMenu) {
					// Use the selected style or the default
					String style = styleId != null ? styleId : renderThemeStyleMenu.getDefaultValue();
					// Retrieve the layer from the style id
					XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
					if (renderThemeStyleLayer == null) {
						System.err.println("##### loadtheme:  Invalid style \"" + style + "\" showing all styles");
						return null;
					}
					System.out.println("####### loadtheme:  selected Style: " + renderThemeStyleLayer.getTitle(_mf_prefered_language));
					// First get the selected layer's categories that are enabled together
					Set<String> categories = renderThemeStyleLayer.getCategories();
					// Then add the selected layer's overlays that are enabled individually
					// Here we use the style menu, but users can use their own preferences
					System.out.println("##### loadtheme: getting overlays");
					for (XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
						//System.out.println("overlay id, title: " + overlay.getId() + " , " + overlay.getTitle("de") + " , " + "");
						if (overlay.isEnabled())
							categories.addAll(overlay.getCategories());
					}
					System.out.println("##### loadtheme: leaving");
					// This is the whole categories set to be enabled
					return categories;
				}
			}));

		}  else {
			System.out.println("##### loadtheme: is online map");
			mMap.setTheme(VtmThemes.OSMARENDER);
		}
/*		CanvasAdapter.textScale = _mf_TextScale;
		mMap.clearMap();
		mMap.updateMap(true);*/
	}
	
	
	private UrlTileSource createTileSource(final Map25Provider mapProvider, final OkHttpFactoryMT httpFactory) {

		final Builder<?> map25Builder = Map25TileSource
				.builder(mapProvider)
				.url(mapProvider.url)
				.tilePath(mapProvider.tilePath)
				.httpFactory(httpFactory);

		final String apiKey = mapProvider.apiKey;
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

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//						+ ("\tgetAndReset_IsMapItemHit:" + _isMapItemHit));
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		final boolean isMapItemHit = _isMapItemHit;
//
//		_isMapItemHit = false;
//
//		return isMapItemHit;
		return false;
	}

	public OsmTileLayerMT getLayer_BaseMap() {
		return _layer_BaseMap;
	}

	public BuildingLayer getLayer_Building() {
		return _layer_Building;
	}
	
	public S3DBLayer getLayer_S3DB() {
		return _layer_mf_S3DB_Building;
	}

	public LabelLayerMT getLayer_Label() {
		return _layer_Label;
	}

	public MarkerLayer getLayer_Marker() {
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

	private ThemeFile getTheme(final Map25Provider mapProvider) {  //used only for online maps

		switch (mapProvider.tileEncoding) {
		case MVT:
			return VtmThemes.MAPZEN;

		// Open Science Map
		case VTM:
		default:
			return VtmThemes.DEFAULT;
		}
	}

	@Override
	public boolean onItemLongPress(final int index, final MapMarker item) {

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//				+ ("\tonItemLongPress")
//				+ ("\tindex:" + index)
//				+ ("\t_isMapItemHit:" + _isMapItemHit + " -> true")
//				//
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		_isMapItemHit = true;
//
//		return true;

		return false;
	}

	@Override
	public boolean onItemSingleTapUp(final int index, final MapMarker item) {

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//				+ ("\tonItemSingleTapUp")//
//				+ ("\tindex:" + index)
//				+ ("\t_isMapItemHit:" + _isMapItemHit + " -> true")
//				//Pref_Map25_Encoding_Mapsforge
//				);
//		// TODO remove SYSTEM.OUT.PRINTLN
//
//		_isMapItemHit = true;
//
//		return true;
		return false;
	}

	public void onModifyMarkerConfig() {

		updateUI_MarkerLayer();

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

//			Fix exception
//
//				Exception in thread "LWJGL Application" java.lang.IllegalArgumentException: top == bottom
//					at org.oscim.renderer.GLMatrix.frustumM(GLMatrix.java:331)
//					at org.oscim.map.ViewController.setScreenSize(ViewController.java:50)
//					at org.oscim.gdx.GdxMap.resize(GdxMap.java:122)
//					at net.tourbook.map.vtm.VtmMap.resize(VtmMap.java:176)

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

		_state.put(STATE_MAP_POS_X + stateSuffixName, mapPosition.x);
		_state.put(STATE_MAP_POS_Y + stateSuffixName, mapPosition.y);
		_state.put(STATE_MAP_POS_BEARING + stateSuffixName, mapPosition.bearing);
		_state.put(STATE_MAP_POS_SCALE + stateSuffixName, mapPosition.scale);
		_state.put(STATE_MAP_POS_TILT + stateSuffixName, mapPosition.tilt);
		_state.put(STATE_MAP_POS_ZOOM_LEVEL + stateSuffixName, mapPosition.zoomLevel);
	}

	/**
	 * called when user change the map25provider
	 * @param mapProvider
	 */
	public void setMapProvider(final Map25Provider mapProvider) {
		
		System.out.println("############# setMapProvider entering setMapProvider");
		//if NOT mapsforge map
		System.out.println("############# setMapProvider MapProviderENCODING: " + mapProvider.tileEncoding);
		if (mapProvider.tileEncoding  != TileEncoding.MF) { // NOT mapsforge
			this._is_mf_Map = false;
		//if(!_is_mf_Map) {
			System.out.println("############# setMapProvider setMapProvider NOT mf Map");
			final UrlTileSource tileSource = createTileSource(mapProvider, _httpFactory);
			_layer_BaseMap.setTileSource(tileSource);
			mMap.setTheme(getTheme(mapProvider));
			////mMap.clearMap();
			////mMap.updateMap(true);
				_mf_themeFilePath = "";
		} else { //it mapsforge map
			this._is_mf_Map = true;
			System.out.println("############# setMapProvider setMapProvider its mf Map");
			//_httpFactory = null;
			final MapFileTileSource tileSource = new MapFileTileSource();
			System.out.println("############# setMapProvider setMap to " + mapProvider.url);
			System.out.println("############# setMapProvider setTheme to " + mapProvider.tilePath);
			System.out.println("############# setMapProvider setStyle to " + mapProvider.apiKey);
			tileSource.setMapFile(mapProvider.url);
			tileSource.setPreferredLanguage(_mf_prefered_language);
				
			_layer_BaseMap.setTileSource(tileSource);

			_mf_mapFilePath = checkFile(mapProvider.url);
			if (_mf_mapFilePath == null) {
				throw new IllegalArgumentException("############# setMapProvider cannot read mapfile: " + _mf_mapFilePath);
			} else {
				System.out.println("############# setMapProvider Map Path: " + _mf_mapFilePath);
			}

			_mf_themeFilePath = checkFile(mapProvider.tilePath);
			_mf_theme_styleID = mapProvider.apiKey;

			if (_mf_themeFilePath == null) {
				System.out.println("############# setMapProvider Theme not found: " + _mf_mapFilePath + " using default OSMARENDER");
				//_mf_VectorTileLayer_S3DB.setRenderTheme(ThemeLoader.load(_mf_themeFilePath));

				mMap.setTheme(VtmThemes.OSMARENDER);   // ThemeLoader.load(_mf_themeFilePath));
			} else {
				if (!_mf_themeFilePath.equals(_last_mf_themeFilePath) || !_mf_theme_styleID.equals(_last_mf_theme_styleID)) {  //only parsing when different file
					System.out.println("############# setMapProvider Theme loader started");
					this._mf_IRenderTheme = ThemeLoader.load(_mf_themeFilePath);
					System.out.println("############# setMapProvider Theme loader done, now activating...");
					_layer_mf_VectorTileLayer_S3DB.setRenderTheme(_mf_IRenderTheme);
					mMap.setTheme(_mf_IRenderTheme);
					loadTheme(mapProvider.apiKey);  //whene starting with onlinemaps and switching to mf, osmarender is used ??? when uncommented it ok
					System.out.println("############# setMapProvider ...activaded");
				} else {
					System.out.println("############# setMapProvider mapprovider has the same theme file");
				}
			}
		}
		mMap.clearMap();
		mMap.updateMap(true);
		System.out.println("############# setMapProvider set language : " + _mf_prefered_language);
		this._last_mf_themeFilePath = _mf_themeFilePath;
		this._last_mf_theme_styleID = _mf_theme_styleID;
		_selectedMapProvider = mapProvider;
	}

	/**
	 * setupMap for online maps
	 * @param mapProvider
	 * @param tileSource
	 */
	private void setupMap(final Map25Provider mapProvider, final UrlTileSource tileSource) {

		_layer_BaseMap = new OsmTileLayerMT(mMap);

		_tileManager = _layer_BaseMap.getManager();

		_layer_BaseMap.setTileSource(tileSource);

// THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);

		mMap.setBaseMap(_layer_BaseMap);

		setupMap_Layers();

		mMap.setTheme(getTheme(mapProvider));

		/**
		 * Map Viewport
		 */
		final ViewController mapViewport = mMap.viewport();

		// extend default tilt
		mapViewport.setMaxTilt((float) MercatorProjection.LATITUDE_MAX);
//		mapViewport.setMaxTilt(77.0f);

		mapViewport.setMinScale(2);
	}
	
	
/**
 * setupMap for mapsforge
 * @param mapProvider
 * @param tileSource
 */
	private void setupMap(final Map25Provider mapProvider, final MapFileTileSource tileSource) {

		_layer_BaseMap = new OsmTileLayerMT(mMap);
		
		_tileManager = _layer_BaseMap.getManager();
		
		_layer_BaseMap.setTileSource(tileSource);
		
	// THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);		
		
		mMap.setBaseMap(_layer_BaseMap);

		_mf_mapFilePath = checkFile(_selectedMapProvider.url);
		if (_mf_mapFilePath == null) {
			throw new IllegalArgumentException("cannot read mapfile: " + _selectedMapProvider.url);
		} else {
			System.out.println("########## setupMap: Map Path: " + _mf_mapFilePath);
		}

		
		_mf_themeFilePath = checkFile(_selectedMapProvider.tilePath);

// is double, also in createlayer(). when totaly removed getting errors
		if (_mf_themeFilePath == null) {
			System.out.println("############# setupMap:  Theme not found: " + _mf_mapFilePath + " using default OSMARENDER");
			mMap.setTheme(VtmThemes.OSMARENDER);   // ThemeLoader.load(_mf_themeFilePath));
		} else {
			_layer_mf_VectorTileLayer_S3DB.setRenderTheme(_mf_IRenderTheme);
			mMap.setTheme(ThemeLoader.load(_mf_themeFilePath));
			loadTheme(mapProvider.apiKey);
		}
		
		CanvasAdapter.textScale = _mf_TextScale;
		
		setupMap_Layers();	
		
		/**
		 * Map Viewport
		 */
		final ViewController mapViewport = mMap.viewport();

		// extend default tilt
		mapViewport.setMaxTilt((float) MercatorProjection.LATITUDE_MAX);
//		mapViewport.setMaxTilt(77.0f);

		mapViewport.setMinScale(2);
	}

	private void setupMap_Layers() {

		final Layers layers = mMap.layers();

		// tour
		_layer_Tour = new TourLayer(mMap);
		_layer_Tour.setEnabled(false);
		layers.add(_layer_Tour);

		// slider path
		_layer_SliderPath = new SliderPath_Layer(mMap);
		_layer_SliderPath.setEnabled(false);
		layers.add(_layer_SliderPath);

		// building
		_layer_Building = new BuildingLayer(mMap, _layer_BaseMap);
		//if(!_is_mf_Map) {
			_layer_Building.setEnabled(false);
		//	layers.remove(_layer_mf_S3DB_Building);
			layers.add(_layer_Building);
		//}
		
		// S3DB
		_layer_mf_S3DB_Building = new S3DBLayer(mMap,_layer_mf_VectorTileLayer_S3DB);
		//_layer_mf_S3DB_Building = new S3DBLayer(mMap,_layer_BaseMap);
		if(_is_mf_Map) {
			//_layer_mf_S3DB = new S3DBLayer(mMap,_mf_VectorTileLayer_S3DB);
			_layer_mf_S3DB_Building.setEnabled(true);
			System.out.println("############ setupMaplayer: adding S3DBlayer ");
			//_mf_VectorTileLayer_S3DB.setRenderTheme(_mf_IRenderTheme);
			//layers.remove(_layer_Building);
			layers.add(_layer_mf_S3DB_Building);
		}

		// label
		_layer_Label = new LabelLayerMT(mMap, _layer_BaseMap);
		_layer_Label.setEnabled(false);
		layers.add(_layer_Label);

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
	}

	void stop() {

		_lwjglApp.stop();
	}

	/**
	 * @wbp.parser.entryPoint
	 */
	private void updateUI_MarkerLayer() {

		final MarkerConfig config = Map25ConfigManager.getActiveMarkerConfig();

		final boolean isShowMarkerPoint = config.isShowMarkerPoint;

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\tisShowMarkerPoint:" + isShowMarkerPoint));
		// TODO remove SYSTEM.OUT.PRINTLN

		_layer_Marker.setEnabled(isShowMarkerPoint);

		if (isShowMarkerPoint) {

			final MarkerRenderer markerRenderer = (MarkerRenderer) _layer_Marker.getRenderer();

			markerRenderer.configureRenderer();
		}
	}
	
	public static String checkFile(String FilePath) {  	
		File file = new File(FilePath);
		if (!file.exists()) {
			System.out.println("############# file not exist: " +  file.getAbsolutePath());
			return null;
		} else if (!file.isFile()) {
			System.out.println("############# is not a file: " +  file.getAbsolutePath());
			return null;
		} else if (!file.canRead()) {
			System.out.println("############# can not read file: " +  file.getAbsolutePath());
			return null;
		}
		return file.getAbsolutePath();
	}	
	
	

}
