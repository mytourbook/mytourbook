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
package net.tourbook.map.vtm;

import java.awt.Canvas;

import net.tourbook.common.util.Util;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.oscim.awt.AwtGraphics;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.core.MercatorProjection;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.GestureHandlerImpl;
import org.oscim.gdx.LwjglGL20;
import org.oscim.gdx.MotionHandler;
import org.oscim.layers.PathLayer;
import org.oscim.layers.tile.TileManager;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.map.ViewController;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mvt.MapboxTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
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

public class VtmMap extends GdxMap {

	private static final String		STATE_MAP_POS_X				= "STATE_MAP_POS_X";					//$NON-NLS-1$
	private static final String		STATE_MAP_POS_Y				= "STATE_MAP_POS_Y";					//$NON-NLS-1$
	private static final String		STATE_MAP_POS_ZOOM_LEVEL	= "STATE_MAP_POS_ZOOM_LEVEL";			//$NON-NLS-1$
	private static final String		STATE_MAP_POS_BEARING		= "STATE_MAP_POS_BEARING";				//$NON-NLS-1$
	private static final String		STATE_MAP_POS_SCALE			= "STATE_MAP_POS_SCALE";				//$NON-NLS-1$
	private static final String		STATE_MAP_POS_TILT			= "STATE_MAP_POS_TILT";					//$NON-NLS-1$

	public static final Logger		log							= LoggerFactory.getLogger(VtmMap.class);

	private static IDialogSettings	_state;

	private static LwjglApplication	_lwjglApp;

	private TileGridLayerMT			_gridLayer;
	private TileManager				_tileManager;

	private long					_lastRenderTime;
	private PathLayer				_tourLayer;

	public VtmMap(final IDialogSettings state) {

		_state = state;
	}

	public static VtmMap createMap(final IDialogSettings state, final Canvas canvas) {

		init();

		_state = state;

		final VtmMap mapApp = new VtmMap(state);
		
		_lwjglApp = new LwjglApplication(mapApp, getConfig(null), canvas);
		
		return mapApp;
	}

	protected static LwjglApplicationConfiguration getConfig(final String title) {

		LwjglApplicationConfiguration.disableAudio = true;
		final LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

		cfg.title = title != null ? title : "vtm-gdx";
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
		new SharedLibraryLoader().load("vtm-jni");

		// init canvas
		AwtGraphics.init();

		GdxAssets.init("assets/");

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

	@Override
	public void createLayers() {

		final OkHttpEngine.OkHttpFactory httpFactory = new OkHttpEngineMT.OkHttpFactoryMT();

		final TileSourceProvider tileSourceProvider = TileSourceProvider.CustomTileProvider;
//		final TileSourceProvider tileSourceProvider = TileSourceProvider.OpenScienceMap;
//		final TileSourceProvider tileSourceProvider = TileSourceProvider.Mapzen;

		VtmThemes theme;
		UrlTileSource tileSource;

		switch (tileSourceProvider) {
		case CustomTileProvider:

			theme = VtmThemes.MAPZEN;
			tileSource = CustomTileSource
					//
					.builder()
					.httpFactory(httpFactory)
					.build();
			break;

		case Mapzen:

			theme = VtmThemes.MAPZEN;

			// Mapzen requires an API key that the tiles can be loaded
			final String apiKey = System.getProperty("MapzenApiKey", "mapzen-xxxxxxx");

			tileSource = MapboxTileSource
					.builder()
					.apiKey(apiKey) // Put a proper API key
					.httpFactory(httpFactory)
					.build();
			break;

		default:

			theme = VtmThemes.DEFAULT;
			tileSource = OSciMap4TileSource
					//
					.builder()
					.httpFactory(httpFactory)
					.build();
			break;
		}

		setupMap(tileSource, theme);

		restoreState();
	}

	@Override
	public void dispose() {

		// stop loading tiles
		_tileManager.clearJobs();

		saveState();

		super.dispose();
	}

	PathLayer getTourLayer() {

		return _tourLayer;
	}

	@Override
	protected boolean onKeyDown(final int keycode) {

		MapPosition mapPosition;

		switch (keycode) {

		case Input.Keys.T:

			// show/hide tour layer

			if (_tourLayer.isEnabled()) {

				_tourLayer.setEnabled(false);
				mMap.layers().remove(_tourLayer);

			} else {

				_tourLayer.setEnabled(true);
				mMap.layers().add(_tourLayer);
			}

			mMap.render();

			return true;

		case Input.Keys.G:

			// show/hide grid layer

			if (_gridLayer == null) {
				_gridLayer = new TileGridLayerMT(mMap);
				_gridLayer.setEnabled(true);
				mMap.layers().add(_gridLayer);
			} else {
				if (_gridLayer.isEnabled()) {
					_gridLayer.setEnabled(false);
					mMap.layers().remove(_gridLayer);
				} else {
					_gridLayer.setEnabled(true);
					mMap.layers().add(_gridLayer);
				}
			}

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

	@Override
	public void render() {

		final long renderTime = System.currentTimeMillis();
		if (renderTime > _lastRenderTime + 1000) {

			_lastRenderTime = renderTime;

			final MapVtmDebugView vtmDebugView = MapVtmManager.getMapVtmDebugView();

			if (vtmDebugView != null) {

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

		final MapPosition mapPosition = new MapPosition();

		mapPosition.x = Util.getStateDouble(_state, STATE_MAP_POS_X, 0.5);
		mapPosition.y = Util.getStateDouble(_state, STATE_MAP_POS_Y, 0.5);

		mapPosition.bearing = Util.getStateFloat(_state, STATE_MAP_POS_BEARING, 0);
		mapPosition.tilt = Util.getStateFloat(_state, STATE_MAP_POS_TILT, 0);

		mapPosition.scale = Util.getStateDouble(_state, STATE_MAP_POS_SCALE, 1);
		mapPosition.zoomLevel = Util.getStateInt(_state, STATE_MAP_POS_ZOOM_LEVEL, 1);

		mMap.setMapPosition(mapPosition);

	}

	private void saveState() {

		final MapPosition mapPosition = mMap.getMapPosition();

		_state.put(STATE_MAP_POS_X, mapPosition.x);
		_state.put(STATE_MAP_POS_Y, mapPosition.y);
		_state.put(STATE_MAP_POS_BEARING, mapPosition.bearing);
		_state.put(STATE_MAP_POS_SCALE, mapPosition.scale);
		_state.put(STATE_MAP_POS_TILT, mapPosition.tilt);
		_state.put(STATE_MAP_POS_ZOOM_LEVEL, mapPosition.zoomLevel);
	}

	private void setupMap(final UrlTileSource tileSource, final VtmThemes themes) {

		final VectorTileLayer mapLayer = new OsmTileLayerMT(mMap);

		_tileManager = mapLayer.getManager();

		mapLayer.setTileSource(tileSource);

// THIS IS NOT YET WORKING
//		mapLayer.setNumLoaders(10);

		mMap.setBaseMap(mapLayer);

		final Layers layers = mMap.layers();

		layers.add(new BuildingLayer(mMap, mapLayer));
		layers.add(new LabelLayer(mMap, mapLayer));

		/*
		 * Tour layer
		 */
		final int lineColor = 0xffff0000;
		final float lineWidth = 25f;

		_tourLayer = new PathLayer(mMap, lineColor, lineWidth);
		_tourLayer.setEnabled(true);
		layers.add(_tourLayer);

		mMap.setTheme(themes);

//		/*
//		 * Map Scale
//		 */
//		final DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap);
//
//		mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.SINGLE);
////		mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH);
//
//		mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
////		mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
//
//		mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT);
//
//		final MapScaleBarLayer mapScaleBarLayer = new MapScaleBarLayer(mMap, mapScaleBar);
//		final BitmapRenderer renderer = mapScaleBarLayer.getRenderer();
//		renderer.setPosition(GLViewport.Position.BOTTOM_RIGHT);
//		renderer.setOffset(5, 0);
//		layers.add(mapScaleBarLayer);

		/*
		 * Map Viewport
		 */
		final ViewController mapViewport = mMap.viewport();

		// extend default tilt
		mapViewport.setMaxTilt((float) MercatorProjection.LATITUDE_MAX);
//		mapViewport.setMaxTilt(77.0f);

		mapViewport.setMinScale(2);
	}

	void stop() {

		_lwjglApp.stop();
	}

}
