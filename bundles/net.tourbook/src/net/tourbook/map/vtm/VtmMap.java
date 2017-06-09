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
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.LwjglGL20;
import org.oscim.layers.GroupLayer;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.OsmTileLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.map.Map;
import org.oscim.renderer.BitmapRenderer;
import org.oscim.renderer.GLViewport;
import org.oscim.scalebar.DefaultMapScaleBar;
import org.oscim.scalebar.ImperialUnitAdapter;
import org.oscim.scalebar.MapScaleBar;
import org.oscim.scalebar.MapScaleBarLayer;
import org.oscim.scalebar.MetricUnitAdapter;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mvt.MapboxTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Timer.Task;

public class VtmMap extends GdxMap {

	private static final String	STATE_MAP_POS_X				= "STATE_MAP_POS_X";					//$NON-NLS-1$
	private static final String	STATE_MAP_POS_Y				= "STATE_MAP_POS_Y";					//$NON-NLS-1$
	private static final String	STATE_MAP_POS_ZOOM_LEVEL	= "STATE_MAP_POS_ZOOM_LEVEL";			//$NON-NLS-1$
	private static final String	STATE_MAP_POS_BEARING		= "STATE_MAP_POS_BEARING";				//$NON-NLS-1$
	private static final String	STATE_MAP_POS_SCALE			= "STATE_MAP_POS_SCALE";				//$NON-NLS-1$
	private static final String	STATE_MAP_POS_TILT			= "STATE_MAP_POS_TILT";					//$NON-NLS-1$

	public static final Logger	log							= LoggerFactory.getLogger(VtmMap.class);

	private IDialogSettings		_state;

	private LwjglApplication	_lwjglApp;

	private boolean				mRenderWait;
	private boolean				mRenderRequest;

	/**
	 * Copied from {@link GdxMap}
	 */
	private class MapAdapter extends Map {

		private final Runnable mRedrawCb = new Runnable() {
			@Override
			public void run() {
				prepareFrame();
				Gdx.graphics.requestRendering();
			}
		};

		@Override
		public void beginFrame() {}

		@Override
		public void doneFrame(final boolean animate) {
			synchronized (mRedrawCb) {
				mRenderRequest = false;
				if (animate || mRenderWait) {
					mRenderWait = false;
					updateMap(true);
				}
			}
		}

		@Override
		public int getHeight() {
			return Gdx.graphics.getHeight();
		}

		@Override
		public int getWidth() {
			return Gdx.graphics.getWidth();
		}

		@Override
		public boolean post(final Runnable runnable) {
			Gdx.app.postRunnable(runnable);
			return true;
		}

		@Override
		public boolean postDelayed(final Runnable action, final long delay) {
			Timer.schedule(new Task() {
				@Override
				public void run() {
					action.run();
				}
			}, delay / 1000f);
			return true;
		}

		@Override
		public void render() {
			synchronized (mRedrawCb) {
				mRenderRequest = true;
				if (mClearMap) {
					updateMap(false);
				} else {
					Gdx.graphics.requestRendering();
				}
			}
		}

		@Override
		public void updateMap(final boolean forceRender) {
			synchronized (mRedrawCb) {
				if (!mRenderRequest) {
					mRenderRequest = true;
					Gdx.app.postRunnable(mRedrawCb);
				} else {
					mRenderWait = true;
				}
			}
		}
	}

	public VtmMap(final IDialogSettings state) {

		_state = state;
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

	/*
	 * Copied and modified from
	 * @see org.oscim.gdx.GdxMap#create()
	 */
//	@Override
//	public void create() {
//
//		mMap = new MapAdapter();
//		mMapRenderer = new MapRenderer(mMap);
//
//		Gdx.graphics.setContinuousRendering(false);
//		Gdx.app.setLogLevel(Application.LOG_DEBUG);
//
//		final int w = Gdx.graphics.getWidth();
//		final int h = Gdx.graphics.getHeight();
//
//		mMap.viewport().setScreenSize(w, h);
//		mMapRenderer.onSurfaceCreated();
//		mMapRenderer.onSurfaceChanged(w, h);
//
//		final InputMultiplexer mux = new InputMultiplexer();
//		if (!Map.NEW_GESTURES) {
//			mGestureDetector = new GestureDetector(new GestureHandlerImpl(mMap));
//			mux.addProcessor(mGestureDetector);
//		}
//		mux.addProcessor(new InputHandler(this));
//		mux.addProcessor(new MotionHandler(mMap));
//
//		Gdx.input.setInputProcessor(mux);
//
//		createLayers();
//	}

	@Override
	public void createLayers() {

		createLayers_DefaultMap();
//		createLayers_Test();
	}

	private void createLayers_DefaultMap() {

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
			tileSource = MapboxTileSource
					.builder()
//					.apiKey("mapzen-xxxxxxx") // Put a proper API key
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

	public void createLayers_Test() {

		final VectorTileLayer l = mMap.setBaseMap(new OSciMap4TileSource());

		final GroupLayer groupLayer = new GroupLayer(mMap);
		groupLayer.layers.add(new BuildingLayer(mMap, l));
		groupLayer.layers.add(new LabelLayer(mMap, l));
		mMap.layers().add(groupLayer);

		final DefaultMapScaleBar mapScaleBar = new DefaultMapScaleBar(mMap);
		mapScaleBar.setScaleBarMode(DefaultMapScaleBar.ScaleBarMode.BOTH);
		mapScaleBar.setDistanceUnitAdapter(MetricUnitAdapter.INSTANCE);
		mapScaleBar.setSecondaryDistanceUnitAdapter(ImperialUnitAdapter.INSTANCE);
		mapScaleBar.setScaleBarPosition(MapScaleBar.ScaleBarPosition.BOTTOM_LEFT);

		final MapScaleBarLayer mapScaleBarLayer = new MapScaleBarLayer(mMap, mapScaleBar);
		final BitmapRenderer renderer = mapScaleBarLayer.getRenderer();
		renderer.setPosition(GLViewport.Position.BOTTOM_LEFT);
		renderer.setOffset(5, 0);
		mMap.layers().add(mapScaleBarLayer);

		mMap.setTheme(VtmThemes.DEFAULT);
		mMap.setMapPosition(53.075, 8.808, 1 << 17);
	}

	@Override
	public void dispose() {

		saveState();

		super.dispose();
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

	public void run(final Canvas canvas) {

		init();

		_lwjglApp = new LwjglApplication(new VtmMap(_state), getConfig(null), canvas);
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

		final VectorTileLayer mapLayer = new OsmTileLayer(mMap);
		mapLayer.setTileSource(tileSource);

		mapLayer.setNumLoaders(10);

		mMap.setBaseMap(mapLayer);

//		final VectorTileLayer mapLayer = mMap.setBaseMap(tileSource);
		final Layers layers = mMap.layers();

		layers.add(new BuildingLayer(mMap, mapLayer));
		layers.add(new LabelLayer(mMap, mapLayer));

		mMap.setTheme(themes);

		// extend default tilt
		mMap.viewport().setMaxTilt(88);

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
	}

	void stop() {

		_lwjglApp.stop();
	}
}
