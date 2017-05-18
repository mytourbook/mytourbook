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

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Display;
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

	public static final Logger	log	= LoggerFactory.getLogger(VtmMap.class);

	private LwjglApplication	_lwjglApp;
	private Thread				_lwjglAppThread;

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
	public void create() {

		super.create();

		_lwjglAppThread = Thread.currentThread();

		try {
			Display.makeCurrent();
		} catch (final LWJGLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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

	public void destroy() {

//		_lwjglAppThread.
//
//				Display.destroy();
//		_lwjglApp.stop();
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

//		http //www.badlogicgames.com/forum/viewtopic.php?f=11&t=4802&p=22988&hilit=restart+jvm#p22988

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

	public void run(final Canvas canvas) {

		init();

		_lwjglApp = new LwjglApplication(new VtmMap(), getConfig(null), canvas);
	}
}
