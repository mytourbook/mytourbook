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
import java.io.File;

import net.tourbook.common.util.Util;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.oscim.awt.AwtGraphics;
import org.oscim.backend.GLAdapter;
import org.oscim.core.MapPosition;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.LwjglGL20;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.map.Layers;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mvt.MapboxTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;
import okhttp3.Cache;

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

	@Override
	public void createLayers() {

		final Cache cache = new Cache(new File(getCacheDir()), Integer.MAX_VALUE);

		final OkHttpEngine.OkHttpFactory httpFactory = new OkHttpEngine.OkHttpFactory(cache);

//		final TileSourceProvider tileSourceProvider = TileSourceProvider.OpenScienceMap;
		final TileSourceProvider tileSourceProvider = TileSourceProvider.TileMaker;

//		final TileSourceProvider tileSourceProvider = TileSourceProvider.Mapzen;

		if (tileSourceProvider.equals(TileSourceProvider.TileMaker)) {

			createTileSource_TileMaker(httpFactory);

		} else if (tileSourceProvider.equals(TileSourceProvider.Mapzen)) {

			createTileSource_Mapzen(httpFactory);

		} else {

			// Default

			createTileSource_OSci(httpFactory);
		}

		mMap.setMapPosition(0, 0, 1 /* 1 << 2 */);

		restoreState();
	}

	private void createTileSource_Mapzen(final OkHttpFactory httpFactory) {

		final UrlTileSource tileSource = MapboxTileSource

				.builder()
//				.apiKey("mapzen-xxxxxxx") // Put a proper API key
				.httpFactory(httpFactory)
				.build();

		final VectorTileLayer mapLayer = mMap.setBaseMap(tileSource);

		setupMap(mapLayer, VtmThemes.MAPZEN);
	}

	private void createTileSource_OSci(final OkHttpEngine.OkHttpFactory httpFactory) {

		final OSciMap4TileSource tileSource = OSciMap4TileSource
				//
				.builder()
				.httpFactory(httpFactory)
				.build();

		final VectorTileLayer mapLayer = mMap.setBaseMap(tileSource);

		setupMap(mapLayer, VtmThemes.DEFAULT);
	}

	private void createTileSource_TileMaker(final OkHttpEngine.OkHttpFactory httpFactory) {

		final TileMakerTileSource tileSource = TileMakerTileSource
				//
				.builder()
				.httpFactory(httpFactory)
				.build();

		final VectorTileLayer mapLayer = mMap.setBaseMap(tileSource);

		setupMap(mapLayer, VtmThemes.DEFAULT);
	}

	@Override
	public void dispose() {

		saveState();

		super.dispose();
	}

	private String getCacheDir() {

		final String workingDirectory = Platform.getInstanceLocation().getURL().getPath();

		final IPath tileCachePath = new Path(workingDirectory).append("vtm-tile-cache");

		if (tileCachePath.toFile().exists() == false) {
			tileCachePath.toFile().mkdirs();
		}

		return tileCachePath.toOSString();
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

	private void setupMap(final VectorTileLayer mapLayer, final VtmThemes themes) {

		mMap.setTheme(themes);

		final Layers layers = mMap.layers();

		layers.add(new BuildingLayer(mMap, mapLayer));
		layers.add(new LabelLayer(mMap, mapLayer));

//		layers.add(new TileGridLayer(mMap, 1));
	}

	void stop() {

		_lwjglApp.stop();
	}
}
