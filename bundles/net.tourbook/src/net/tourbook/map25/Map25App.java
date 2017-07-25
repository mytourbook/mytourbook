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
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map25.Map25TileSource.Builder;
import net.tourbook.map25.OkHttpEngineMT.OkHttpFactoryMT;
import net.tourbook.map25.layer.marker.ClusterMarkerRenderer;
import net.tourbook.map25.layer.marker.ItemizedLayer;
import net.tourbook.map25.layer.marker.ItemizedLayer.OnItemGestureListener;
import net.tourbook.map25.layer.marker.MarkerItem;
import net.tourbook.map25.layer.marker.MarkerRendererFactory;
import net.tourbook.map25.layer.marker.MarkerSymbol;
import net.tourbook.map25.layer.marker.ScreenUtils;
import net.tourbook.map25.layer.tourtrack.TourLayer;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.widgets.Display;
import org.oscim.awt.AwtGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.GLAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GestureHandlerImpl;
import org.oscim.gdx.LwjglGL20;
import org.oscim.gdx.MotionHandler;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.map.ViewController;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.UrlTileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import okhttp3.Cache;

public class Map25App extends GdxMap {

	private static final String			STATE_MAP_POS_X						= "STATE_MAP_POS_X";						//$NON-NLS-1$
	private static final String			STATE_MAP_POS_Y						= "STATE_MAP_POS_Y";						//$NON-NLS-1$
	private static final String			STATE_MAP_POS_ZOOM_LEVEL			= "STATE_MAP_POS_ZOOM_LEVEL";				//$NON-NLS-1$
	private static final String			STATE_MAP_POS_BEARING				= "STATE_MAP_POS_BEARING";					//$NON-NLS-1$
	private static final String			STATE_MAP_POS_SCALE					= "STATE_MAP_POS_SCALE";					//$NON-NLS-1$
	private static final String			STATE_MAP_POS_TILT					= "STATE_MAP_POS_TILT";						//$NON-NLS-1$
	private static final String			STATE_SELECTED_MAP25_PROVIDER_ID	= "STATE_SELECTED_MAP25_PROVIDER_ID";		//$NON-NLS-1$

	public static final Logger			log									= LoggerFactory.getLogger(Map25App.class);

	private static IDialogSettings		_state;

	private static Map25View			_map25View;
	private static LwjglApplication		_lwjglApp;

	private Map25Provider				_selectedMapProvider;
	private TileManager					_tileManager;

	private OsmTileLayerMT				_layer_BaseMap;
	private BuildingLayer				_layer_Building;
	private LabelLayer					_layer_Label;
	private ItemizedLayer<MarkerItem>	_layer_Marker;
	private MapScaleBarLayer			_layer_ScaleBar;
	private TileGridLayerMT				_layer_TileInfo;
	private TourLayer					_layer_Tour;

	private OkHttpFactoryMT				_httpFactory;

	private long						_lastRenderTime;

	/**
	 * Is <code>true</code> when a tour marker is hit.
	 */
	private boolean						_isMapItemHit;

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

		/*
		 * Overwrite input handler, using own GdxMap.create() method didn't work :-(
		 */
		final InputMultiplexer mux = new InputMultiplexer();
		if (!Map.NEW_GESTURES) {
			mGestureDetector = new GestureDetector(new GestureHandlerImpl(mMap));
			mux.addProcessor(mGestureDetector);
		}
		mux.addProcessor(new InputHandlerMT(this));
		mux.addProcessor(new MotionHandler(mMap));

		Gdx.input.setInputProcessor(mux);
	}

	private ItemizedLayer<MarkerItem> createLayer_Marker() {

		final Bitmap awtBitmap = createMarkerImage();

		MarkerSymbol markerSymbol;
//		symbol = new MarkerSymbol(awtBitmap, MarkerSymbol.HotspotPlace.BOTTOM_CENTER);
		markerSymbol = new MarkerSymbol(awtBitmap, MarkerSymbol.HotspotPlace.CENTER, true);

		final MarkerRendererFactory markerRenderFactory = ClusterMarkerRenderer.factory(
				markerSymbol,
				new ClusterMarkerRenderer.ClusterStyle(Color.WHITE, Color.BLUE));

		final OnItemGestureListener<MarkerItem> onItemGestureListener = new OnItemGestureListener<MarkerItem>() {

			@Override
			public boolean onItemLongPress(final int index, final MarkerItem item) {

				System.out.println(
						(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
								+ ("\tonItemLongPress")
								+ ("\tindex:" + index)
								+ ("\t_isMapItemHit:" + _isMapItemHit + " -> true")
				//
				);
				// TODO remove SYSTEM.OUT.PRINTLN

				_isMapItemHit = true;

				return true;
			}

			@Override
			public boolean onItemSingleTapUp(final int index, final MarkerItem item) {

				System.out.println(
						(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
								+ ("\tonItemSingleTapUp")//
								+ ("\tindex:" + index)
								+ ("\t_isMapItemHit:" + _isMapItemHit + " -> true")
				//
				);
				// TODO remove SYSTEM.OUT.PRINTLN

				_isMapItemHit = true;

				return true;
			}
		};

		final ItemizedLayer<MarkerItem> markerLayer = new ItemizedLayer<>(
				mMap,
				new ArrayList<MarkerItem>(),
				markerRenderFactory,
				onItemGestureListener);

		return markerLayer;
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
		layer.setEnabled(false);

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

		final UrlTileSource tileSource = createTileSource(_selectedMapProvider, _httpFactory);

		setupMap(_selectedMapProvider, tileSource);
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

	private Bitmap createMarkerImage() {

		final Paint mPaintCircle = CanvasAdapter.newPaint();
		final Paint mPaintBorder = CanvasAdapter.newPaint();

		final int iconSize = 20;

		final Bitmap bitmap = CanvasAdapter.newBitmap(iconSize, iconSize, 0);
		final org.oscim.backend.canvas.Canvas canvas = CanvasAdapter.newCanvas();
		canvas.setBitmap(bitmap);

		final int iconSize2 = iconSize / 2;
		final int noneClippingRadius = iconSize2 - ScreenUtils.getPixels(2);

		// fill + outline
		canvas.drawCircle(iconSize2, iconSize2, noneClippingRadius, mPaintCircle);
		canvas.drawCircle(iconSize2, iconSize2, noneClippingRadius, mPaintBorder);

		return bitmap;
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

		System.out.println(
				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
						+ ("\tgetAndReset_IsMapItemHit:" + _isMapItemHit));
		// TODO remove SYSTEM.OUT.PRINTLN

		final boolean isMapItemHit = _isMapItemHit;

		_isMapItemHit = false;

		return isMapItemHit;
	}

	public OsmTileLayerMT getLayer_BaseMap() {
		return _layer_BaseMap;
	}

	public BuildingLayer getLayer_Building() {
		return _layer_Building;
	}

	public LabelLayer getLayer_Label() {
		return _layer_Label;
	}

	public ItemizedLayer<MarkerItem> getLayer_Marker() {
		return _layer_Marker;
	}

	public MapScaleBarLayer getLayer_ScaleBar() {
		return _layer_ScaleBar;
	}

	public TileGridLayerMT getLayer_TileInfo() {
		return _layer_TileInfo;
	}

	public TourLayer getLayer_Tour() {
		return _layer_Tour;
	}

	public Map25Provider getSelectedMapProvider() {
		return _selectedMapProvider;
	}

	private ThemeFile getTheme(final Map25Provider mapProvider) {

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
	protected boolean onKeyDown(final int keycode) {

		MapPosition mapPosition;

		switch (keycode) {

		case Input.Keys.T:

			// show/hide tour layer
			_layer_Tour.setEnabled(!_layer_Tour.isEnabled());

			mMap.render();

			// update actions in UI thread
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					_map25View.updateActionsState();
				}
			});

			return true;

		case Input.Keys.G:

			// show/hide grid layer

			_layer_TileInfo.setEnabled(!_layer_TileInfo.isEnabled());

			mMap.render();

			return true;

		case Input.Keys.O:

			// reset bearing

			mapPosition = mMap.getMapPosition();
			mapPosition.bearing = 0;

			mMap.setMapPosition(mapPosition);
			mMap.render();

			return true;

		case Input.Keys.I:

			// reset tilt

			mapPosition = mMap.getMapPosition();
			mapPosition.tilt = 0;

			mMap.setMapPosition(mapPosition);
			mMap.render();

			return true;
		}

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

		/*
		 * Map position
		 */
		final MapPosition mapPosition = new MapPosition();

		mapPosition.x = Util.getStateDouble(_state, STATE_MAP_POS_X, 0.5);
		mapPosition.y = Util.getStateDouble(_state, STATE_MAP_POS_Y, 0.5);

		mapPosition.bearing = Util.getStateFloat(_state, STATE_MAP_POS_BEARING, 0);
		mapPosition.tilt = Util.getStateFloat(_state, STATE_MAP_POS_TILT, 0);

		mapPosition.scale = Util.getStateDouble(_state, STATE_MAP_POS_SCALE, 1);
		mapPosition.zoomLevel = Util.getStateInt(_state, STATE_MAP_POS_ZOOM_LEVEL, 1);

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

		final MapPosition mapPosition = mMap.getMapPosition();

		_state.put(STATE_MAP_POS_X, mapPosition.x);
		_state.put(STATE_MAP_POS_Y, mapPosition.y);
		_state.put(STATE_MAP_POS_BEARING, mapPosition.bearing);
		_state.put(STATE_MAP_POS_SCALE, mapPosition.scale);
		_state.put(STATE_MAP_POS_TILT, mapPosition.tilt);
		_state.put(STATE_MAP_POS_ZOOM_LEVEL, mapPosition.zoomLevel);

		_state.put(STATE_SELECTED_MAP25_PROVIDER_ID, _selectedMapProvider.getId());
	}

	public void setMapProvider(final Map25Provider mapProvider) {

		final UrlTileSource tileSource = createTileSource(mapProvider, _httpFactory);

		_layer_BaseMap.setTileSource(tileSource);
		mMap.setTheme(getTheme(mapProvider));

		_selectedMapProvider = mapProvider;
	}

	private void setupMap(final Map25Provider mapProvider, final UrlTileSource tileSource) {

		_layer_BaseMap = new OsmTileLayerMT(mMap);

		_tileManager = _layer_BaseMap.getManager();

		_layer_BaseMap.setTileSource(tileSource);

// THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);

		mMap.setBaseMap(_layer_BaseMap);

		setupMap_Layers();

		mMap.setTheme(getTheme(mapProvider));

		/*
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

		// building
		_layer_Building = new BuildingLayer(mMap, _layer_BaseMap);
		_layer_Building.setEnabled(false);
		layers.add(_layer_Building);

		// label
		_layer_Label = new LabelLayer(mMap, _layer_BaseMap);
		_layer_Label.setEnabled(false);
		layers.add(_layer_Label);

		// marker
		_layer_Marker = createLayer_Marker();
		_layer_Marker.setEnabled(false);
		layers.add(_layer_Marker);

		// scale bar
		_layer_ScaleBar = createLayer_ScaleBar();
		layers.add(_layer_ScaleBar);

		// tile info
		_layer_TileInfo = new TileGridLayerMT(mMap);
		_layer_TileInfo.setEnabled(false);
		layers.add(_layer_TileInfo);
	}

	void stop() {

		_lwjglApp.stop();
	}

	private void updateUI_MarkerLayer() {

		final ClusterMarkerRenderer markerRenderer = (ClusterMarkerRenderer) _layer_Marker.getRenderer();
		final Map25MarkerConfig markerConfig = Map25ConfigManager.getActiveMarkerConfig();

		markerRenderer.setClusterIconConfig(//
				markerConfig.iconClusterSizeDP,
				markerConfig.clusterColorForeground,
				markerConfig.clusterColorBackground);
	}

}
