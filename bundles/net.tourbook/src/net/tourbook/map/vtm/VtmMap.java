/*
 * Copyright 2013 Hannes Janetzek
 * Copyright 2016 devemux86
 *
 * This file is part of the OpenScienceMap project (http://www.opensciencemap.org).
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.tourbook.map.vtm;

import java.awt.Canvas;

import org.oscim.awt.AwtGraphics;
import org.oscim.backend.GLAdapter;
import org.oscim.gdx.GdxAssets;
import org.oscim.gdx.GdxMap;
import org.oscim.gdx.LwjglGL20;
import org.oscim.layers.tile.buildings.BuildingLayer;
import org.oscim.layers.tile.vector.VectorTileLayer;
import org.oscim.layers.tile.vector.labeling.LabelLayer;
import org.oscim.theme.VtmThemes;
import org.oscim.tiling.source.OkHttpEngine;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mvt.MapboxTileSource;
import org.oscim.tiling.source.oscimap4.OSciMap4TileSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.utils.SharedLibraryLoader;

public class VtmMap extends GdxMap {


	public static final Logger log = LoggerFactory.getLogger(VtmMap.class);

	protected static LwjglApplicationConfiguration getConfig(final String title) {

		LwjglApplicationConfiguration.disableAudio = true;
		final LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

		cfg.title = title != null ? title : "vtm-gdx";
		cfg.width = 1200; //800;
		cfg.height = 1000; //600;
		cfg.stencil = 8;
		cfg.samples = 2;
		cfg.foregroundFPS = 30;
		cfg.backgroundFPS = 10;

		return cfg;
	}

	public static void init() {

		// load native library
		new SharedLibraryLoader().load("vtm-jni");

		// init globals
		AwtGraphics.init();

		GdxAssets.init("assets/");

		GLAdapter.init(new LwjglGL20());
		GLAdapter.GDX_DESKTOP_QUIRKS = true;
	}

	@Override
	public void createLayers() {

//		final TileSource tileSource = new OSciMap4TileSource();
//
//		initDefaultLayers(tileSource, false, true, true);
//
//		mMap.setMapPosition(0, 0, 1 << 2);

		/////////////////////////////////////////////////////////////////////////////

		final OkHttpEngine.OkHttpFactory httpFactory = new OkHttpEngine.OkHttpFactory();

		final OSciMap4TileSource tileSource = OSciMap4TileSource//
				.builder()
				.httpFactory(httpFactory)
				.build();

		initDefaultLayers(tileSource, false, true, true);

		mMap.setMapPosition(0, 0, 1 << 2);
	}

	public void createLayers2() {

		final UrlTileSource tileSource = MapboxTileSource
				.builder()
				.apiKey("mapzen-xxxxxxx") // Put a proper API key
				.httpFactory(new OkHttpEngine.OkHttpFactory())
				//.locale("en")
				.build();

		final VectorTileLayer l = mMap.setBaseMap(tileSource);
		mMap.setTheme(VtmThemes.MAPZEN);

		mMap.layers().add(new BuildingLayer(mMap, l));
		mMap.layers().add(new LabelLayer(mMap, l));
	}

	@Override
	public void dispose() {

//		Probably related to how initialize / free the GL resources at start / end of view.
//		There is Map.destroy and LWJGL could have life cycle methods too to check.

//		Exception in thread "LWJGL Application" java.lang.RuntimeException: No OpenGL context found in the current thread.
//		at org.lwjgl.opengl.GLContext.getCapabilities(GLContext.java:124)
//		at org.lwjgl.opengl.GL11.glGetError(GL11.java:1299)
//		at org.lwjgl.opengl.Util.checkGLError(Util.java:57)
//		at org.lwjgl.opengl.WindowsContextImplementation.setSwapInterval(WindowsContextImplementation.java:113)
//		at org.lwjgl.opengl.ContextGL.setSwapInterval(ContextGL.java:232)
//		at org.lwjgl.opengl.DrawableGL.setSwapInterval(DrawableGL.java:86)
//		at org.lwjgl.opengl.Display.setSwapInterval(Display.java:1129)
//		at org.lwjgl.opengl.Display.setVSyncEnabled(Display.java:1142)
//		at com.badlogic.gdx.backends.lwjgl.LwjglGraphics.setVSync(LwjglGraphics.java:558)
//		at com.badlogic.gdx.backends.lwjgl.LwjglApplication$1.run(LwjglApplication.java:124)

		super.dispose();
	}

	public void run(final Canvas canvas) {

		init();

		new LwjglApplication(new VtmMap(), getConfig(null), canvas);
	}
}
