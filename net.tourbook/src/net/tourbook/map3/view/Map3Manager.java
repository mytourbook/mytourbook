/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.map3.view;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.TerrainProfileLayer;
import gov.nasa.worldwind.layers.ViewControlsLayer;
import gov.nasa.worldwind.layers.ViewControlsSelectListener;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;
import gov.nasa.worldwindx.examples.ApplicationTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;
import net.tourbook.map3.layer.DefaultCategory;
import net.tourbook.map3.layer.DefaultLayer;
import net.tourbook.map3.layer.IToolLayer;
import net.tourbook.map3.layer.MapDefaultCategory;
import net.tourbook.map3.layer.MapDefaultLayer;
import net.tourbook.map3.layer.MarkerLayer;
import net.tourbook.map3.layer.StatusLayer;
import net.tourbook.map3.layer.TourInfoLayer;
import net.tourbook.map3.layer.TourLegendLayer;
import net.tourbook.map3.layer.TrackSliderLayer;
import net.tourbook.map3.layer.tourtrack.TourTrackLayer;
import net.tourbook.map3.ui.DialogMap3Layer;
import net.tourbook.map3.ui.DialogTerrainProfileConfig;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class Map3Manager {

	/**
	 * This version number is incremented, when structural changes (e.g. new category) are done.
	 * When this happens, the <b>default</b> structure is created.
	 */
	private static final int							MAP3_LAYER_STRUCTURE_VERSION	= 18;

	public static final String							PROPERTY_NAME_ENABLED			= "Enabled";														//$NON-NLS-1$

	private static final String							MAP3_LAYER_STRUCTURE_FILE_NAME	= "map3-layers.xml";												//$NON-NLS-1$

	private static final String							ATTR_MAP3_LAYER_VERSION			= "map3LayerVersion";												//$NON-NLS-1$

	private static final String							TAG_ROOT						= "Map3LayerStructure";											//$NON-NLS-1$
	private static final String							TAG_CATEGORY					= "category";														//$NON-NLS-1$
	private static final String							TAG_LAYER						= "layer";															//$NON-NLS-1$
	//
	private static final String							ATTR_ID							= "id";															//$NON-NLS-1$
	private static final String							ATTR_IS_DEFAULT_LAYER			= "isDefaultLayer";												//$NON-NLS-1$
	private static final String							ATTR_IS_ENABLED					= "isEnabled";														//$NON-NLS-1$
	private static final String							ATTR_IS_EXPANDED				= "isExpanded";													//$NON-NLS-1$
	//
	private static final int							INSERT_BEFORE_COMPASS			= 1;
	public static final int								INSERT_BEFORE_PLACE_NAMES		= 2;
	//
	private static final String							ERROR_01						= "NTMV_MM_001 Layer \"{0}\" is not a ww default layer.";			//$NON-NLS-1$
	private static final String							ERROR_02						= "NTMV_MM_002 Layer \"{0}\" is not defined as map default layer."; //$NON-NLS-1$
	private static final String							ERROR_03						= "NTMV_MM_003 XML layer \"{0}\" is not available.";				//$NON-NLS-1$
	private static final String							ERROR_04						= "NTMV_MM_004 Category \"{0}\" is not a default category.";		//$NON-NLS-1$

	/**
	 * _bundle must be set here otherwise an exception occures in saveState()
	 */
	private static final Bundle							_bundle							= TourbookPlugin.getDefault()//
																								.getBundle();

	private static final IDialogSettings				_state							= TourbookPlugin
																								.getState(Map3Manager.class
																										.getCanonicalName());
	private static final IPath							_stateLocation					= Platform
																								.getStateLocation(_bundle);

	/**
	 * Root item for the layer tree viewer. This contains the UI model.
	 */
	private static TVIMap3Root							_uiRootItem;
	private static final WorldWindowGLCanvas			_ww;

	/**
	 * Instance of {@link Map3View} or <code>null</code> when view is not created.
	 */
	private static Map3View								_map3View;

	/**
	 * Instance of the map 3 layer viewer dialog or <code>null</code> when dialog is not created or
	 * disposed.
	 */
	private static DialogMap3Layer						_map3LayerDialog;

	/**
	 * Contains default layers with locale layer names which are used as a layer id.
	 */
	private static LayerList							_wwDefaultLocaleLayers;

	/**
	 * Contains custom (none default) layers, key is layerId and sorted by insertion.
	 */
	private static LinkedHashMap<String, TVIMap3Layer>	_uiCustomLayers					= new LinkedHashMap<String, TVIMap3Layer>();

	/**
	 * Contains layers which can not be set visible in the UI but are visible on demand.
	 */
	private static LinkedHashMap<String, IToolLayer>	_toolLayers						= new LinkedHashMap<String, IToolLayer>();
	//
	private static TrackSliderLayer						_wwLayer_TrackSlider;
	private static MarkerLayer							_wwLayer_Marker;
	private static TourInfoLayer						_wwLayer_TourInfo;
	private static TourLegendLayer						_wwLayer_TourLegend;
	private static TourTrackLayer						_wwLayer_TourTrack;

	//
	private static Object[]								_uiVisibleLayers;
	private static Object[]								_uiExpandedCategories;
	private static ArrayList<TVIMap3Layer>				_uiVisibleLayersFromXml			= new ArrayList<TVIMap3Layer>();
	private static ArrayList<TVIMap3Category>			_uiExpandedCategoriesFromXml	= new ArrayList<TVIMap3Category>();
	private static ArrayList<Layer>						_xmlLayers						= new ArrayList<Layer>();

	private static final DateTimeFormatter				_dtFormatter					= ISODateTimeFormat
																								.basicDateTimeNoMillis();

	static {

		// copied from gov.nasa.worldwindx.examples.ApplicationTemplate - 28.7.2013
//        System.setProperty("java.net.useSystemProxies", "true");
		if (Configuration.isMacOS()) {

//            System.setProperty("apple.laf.useScreenMenuBar", "true");
//            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
//            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
//            System.setProperty("apple.awt.brushMetalLook", "true");

//			org.eclipse.swt.SWTError: Not implemented (java.lang.ClassNotFoundException: apple.awt.CEmbeddedFrame)
//			at org.eclipse.swt.SWT.error(SWT.java:4387)
//			at org.eclipse.swt.SWT.error(SWT.java:4276)
//			at org.eclipse.swt.awt.SWT_AWT.new_Frame(SWT_AWT.java:145)
//			at net.tourbook.map3.view.Map3View.createUI(Map3View.java:836)
//			at net.tourbook.map3.view.Map3View.createPartControl(Map3View.java:708)

//			when this is fixed with...

			SWT_AWT.embeddedFrameClass = "sun.lwawt.macosx.CViewEmbeddedFrame"; //$NON-NLS-1$

//			...this error occures

//Exception in thread "AWT-EventQueue-0" javax.media.opengl.GLException: java.lang.NoClassDefFoundError: apple/awt/ComponentModel
//	at com.sun.opengl.impl.Java2D.invokeWithOGLContextCurrent(Java2D.java:296)
//	at javax.media.opengl.Threading.invokeOnOpenGLThread(Threading.java:266)
//	at javax.media.opengl.GLCanvas.maybeDoSingleThreadedWorkaround(GLCanvas.java:410)
//	at javax.media.opengl.GLCanvas.display(GLCanvas.java:244)
//	at javax.media.opengl.GLCanvas.paint(GLCanvas.java:277)
//	at sun.awt.RepaintArea.paintComponent(RepaintArea.java:264)
//	at sun.lwawt.LWRepaintArea.paintComponent(LWRepaintArea.java:54)
//	at sun.awt.RepaintArea.paint(RepaintArea.java:240)
//	at sun.lwawt.LWComponentPeer.handleJavaPaintEvent(LWComponentPeer.java:1267)
//	at sun.lwawt.LWComponentPeer.handleEvent(LWComponentPeer.java:1150)
//	at java.awt.Component.dispatchEventImpl(Component.java:4937)
//	at java.awt.Component.dispatchEvent(Component.java:4687)
//	at java.awt.EventQueue.dispatchEventImpl(EventQueue.java:735)
//	at java.awt.EventQueue.access$200(EventQueue.java:103)
//	at java.awt.EventQueue$3.run(EventQueue.java:694)
//	at java.awt.EventQueue$3.run(EventQueue.java:692)

// 			This error has been fixed by moving from WWJ 1.5 to 2.0

		} else if (Configuration.isWindowsOS()) {
			System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing //$NON-NLS-1$ //$NON-NLS-2$
		}

		/*
		 * This needs to be set, because the jogl selfexpanded native libraries are not found, so
		 * they are packacked into platform specific plugins.
		 */
		System.setProperty("jogamp.gluegen.UseTempJarCache", Boolean.FALSE.toString()); //$NON-NLS-1$
//java.lang.UnsatisfiedLinkError: Can't load library: C:\E\e-3.8.2-32\eclipse\gluegen-rt.dll
//	at java.lang.ClassLoader.loadLibrary(ClassLoader.java:1854)
//	at java.lang.Runtime.load0(Runtime.java:795)
//	at java.lang.System.load(System.java:1062)
//	at com.jogamp.common.jvm.JNILibLoaderBase.loadLibraryInternal(JNILibLoaderBase.java:548)
//	at com.jogamp.common.jvm.JNILibLoaderBase.access$000(JNILibLoaderBase.java:64)
//	at com.jogamp.common.jvm.JNILibLoaderBase$DefaultAction.loadLibrary(JNILibLoaderBase.java:95)
//	at com.jogamp.common.jvm.JNILibLoaderBase.loadLibrary(JNILibLoaderBase.java:412)
//	at com.jogamp.common.os.DynamicLibraryBundle$GlueJNILibLoader.loadLibrary(DynamicLibraryBundle.java:387)
//	at com.jogamp.common.os.Platform$1.run(Platform.java:202)
//	at java.security.AccessController.doPrivileged(Native Method)
//	at com.jogamp.common.os.Platform.<clinit>(Platform.java:173)
//	at javax.media.opengl.GLProfile.<clinit>(GLProfile.java:82)
//	at gov.nasa.worldwind.Configuration.getMaxCompatibleGLProfile(Unknown Source)
//	at gov.nasa.worldwind.awt.WorldWindowGLCanvas.getCaps(Unknown Source)
//	at gov.nasa.worldwind.awt.WorldWindowGLCanvas.<init>(Unknown Source)
//	at net.tourbook.map3.view.Map3Manager.<clinit>(Map3Manager.java:213)
//	at net.tourbook.map3.view.Map3View.<clinit>(Map3View.java:156)

		_ww = new WorldWindowGLCanvas();

		_initializeMap3();

		Map3State.isMapInitialized = true;
	}

	private static final class ViewerControllerCheckStateListener implements ICheckStateListener {

		ViewControlsSelectListener	_viewControlListener;

		/**
		 * This flag keeps track of adding/removing the listener that it is not done more than once.
		 */
		int							__lastAddRemoveAction	= -1;

		private ViewerControllerCheckStateListener(final ViewControlsLayer viewControlsLayer) {

			_viewControlListener = new ViewControlsSelectListener(_ww, viewControlsLayer);
		}

		@Override
		public void onSetCheckState(final TVIMap3Layer tviMap3Layer) {

			if (tviMap3Layer.isLayerVisible) {

				if (__lastAddRemoveAction != 1) {
					__lastAddRemoveAction = 1;
					_ww.addSelectListener(_viewControlListener);
				}

			} else {

				if (__lastAddRemoveAction != 0) {
					__lastAddRemoveAction = 0;
					_ww.removeSelectListener(_viewControlListener);
				}
			}

		}
	}

	/**
	 * Initialize WW model with default layers.
	 */
	private static void _initializeMap3() {

		// create default model
		final Model wwModel = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		wwModel.setShowWireframeExterior(false);
		wwModel.setShowWireframeInterior(false);
		wwModel.setShowTessellationBoundingVolumes(false);

		// get default layer
		_wwDefaultLocaleLayers = wwModel.getLayers();

//		dumpLayer(_wwDefaultLocaleLayers);

		// create custom layer BEFORE state is applied and xml file is read which references these layers
		createLayer_MT_TourTracks();
		createLayer_MT_Marker();
		createLayer_MT_TrackSlider();
		createLayer_MT_TourInfo();
		createLayer_MT_TourLegend();

		createLayer_WW_StatusLine();
		createLayer_WW_MapNavigator();
		createLayer_WW_TerrainProfile();

		// restore layer from xml file
		_uiRootItem = parseLayerXml();

		// set ww layers from xml layers
		final Layer[] layerObject = _xmlLayers.toArray(new Layer[_xmlLayers.size()]);
		final LayerList layers = new LayerList(layerObject);
		wwModel.setLayers(layers);

		// model must be set BEFORE model is updated
		_ww.setModel(wwModel);

		/*
		 * ensure All custom layers are in the model because it can happen, that a layer is created
		 * after the initial load of the layer list and new layers are not contained in the xml
		 * layer file.
		 */
		setCustomLayerInWWModel(wwModel.getLayers());

		setToolLayerInWWModel();
	}

	private static void createLayer_MT_Marker() {

		/*
		 * create WW layer
		 */
		_wwLayer_Marker = new MarkerLayer();

		/*
		 * create UI model layer
		 */
		final String layerId = MarkerLayer.MAP3_LAYER_ID;
		final TVIMap3Layer tviLayer = new TVIMap3Layer(layerId, _wwLayer_Marker, Messages.Custom_Layer_TourMarker);

		final boolean isVisible = true;

		// default is enabled
		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_PLACE_NAMES;

		if (isVisible) {
			_uiVisibleLayersFromXml.add(tviLayer);
		}

		tviLayer.setCheckStateListener(_wwLayer_Marker);

		_uiCustomLayers.put(layerId, tviLayer);
	}

	private static void createLayer_MT_TourInfo() {

		// create WW layer
		_wwLayer_TourInfo = new TourInfoLayer(_state);

		final String layerId = TourInfoLayer.MAP3_LAYER_ID;

		_toolLayers.put(layerId, _wwLayer_TourInfo);
	}

	private static void createLayer_MT_TourLegend() {

		// create WW layer
		_wwLayer_TourLegend = new TourLegendLayer(_state);

		/*
		 * create UI model layer
		 */
		final String layerId = TourLegendLayer.MAP3_LAYER_ID;
		final TVIMap3Layer tviLayer = new TVIMap3Layer(layerId, _wwLayer_TourLegend, Messages.Custom_Layer_TourLegend);

		final boolean isVisible = true;

		// default is enabled
		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_PLACE_NAMES;

		if (isVisible) {
			_uiVisibleLayersFromXml.add(tviLayer);
		}

		_uiCustomLayers.put(layerId, tviLayer);
	}

	private static void createLayer_MT_TourTracks() {

		/*
		 * create WW layer
		 */
		_wwLayer_TourTrack = new TourTrackLayer(_state);

		/*
		 * create UI model layer
		 */
		final String layerId = TourTrackLayer.MAP3_LAYER_ID;
		final TVIMap3Layer tviLayer = new TVIMap3Layer(layerId, _wwLayer_TourTrack, Messages.Custom_Layer_TourTrack);

		final boolean isVisible = true;

		// default is enabled
		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_PLACE_NAMES;

		if (isVisible) {
			_uiVisibleLayersFromXml.add(tviLayer);
		}

		tviLayer.setCheckStateListener(_wwLayer_TourTrack);

		_uiCustomLayers.put(layerId, tviLayer);
	}

	private static void createLayer_MT_TrackSlider() {

		// create WW layer
		_wwLayer_TrackSlider = new TrackSliderLayer(_state);

		/*
		 * create UI model layer
		 */
		final String layerId = TrackSliderLayer.MAP3_LAYER_ID;
		final TVIMap3Layer tviLayer = new TVIMap3Layer(layerId, _wwLayer_TrackSlider, Messages.Custom_Layer_TrackSlider);

		final boolean isVisible = true;

		// default is enabled
		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_PLACE_NAMES;

		if (isVisible) {
			_uiVisibleLayersFromXml.add(tviLayer);
		}

		_uiCustomLayers.put(layerId, tviLayer);
	}

	private static void createLayer_WW_MapNavigator() {

		/*
		 * create WW layer
		 */
		// Create and install the view controls layer and register a controller for it with the World Window.
		final ViewControlsLayer viewControlsLayer = new ViewControlsLayer();

		/*
		 * create UI model layer
		 */
		final String layerId = ViewControlsLayer.class.getCanonicalName();

		final TVIMap3Layer tviLayer = new TVIMap3Layer(
				layerId,
				viewControlsLayer,
				Messages.Custom_Layer_ViewerController);

		tviLayer.defaultPosition = INSERT_BEFORE_COMPASS;
		tviLayer.setCheckStateListener(new ViewerControllerCheckStateListener(viewControlsLayer));

		_uiCustomLayers.put(layerId, tviLayer);
	}

	private static void createLayer_WW_StatusLine() {

		/*
		 * create WW layer
		 */
		final StatusLayer statusLayer = new StatusLayer();
		//this.layer = new StatusMGRSLayer();
		//this.layer = new StatusUTMLayer();

		statusLayer.setEventSource(_ww);
		statusLayer.setCoordDecimalPlaces(4); // default is 4
		//layer.setElevationUnits(StatusLayer.UNIT_IMPERIAL);

		/*
		 * create UI model layer
		 */
		final String layerId = StatusLayer.class.getCanonicalName();

		final TVIMap3Layer tviLayer = new TVIMap3Layer(layerId, statusLayer, Messages.Custom_Layer_Status);

		final boolean isVisible = true;

		// default is enabled
		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_COMPASS;

		if (isVisible) {
			_uiVisibleLayersFromXml.add(tviLayer);
		}

		_uiCustomLayers.put(layerId, tviLayer);
	}

	private static void createLayer_WW_TerrainProfile() {

		/*
		 * create WW layer
		 */
		// Add TerrainProfileLayer
		final TerrainProfileLayer profileLayer = new TerrainProfileLayer();
		profileLayer.setEventSource(_ww);
		profileLayer.setStartLatLon(LatLon.fromDegrees(0, -10));
		profileLayer.setEndLatLon(LatLon.fromDegrees(0, 65));

		/*
		 * create UI model layer
		 */
		final String layerId = TerrainProfileLayer.class.getCanonicalName();

		final TVIMap3Layer tviLayer = new TVIMap3Layer(layerId, profileLayer, Messages.Custom_Layer_TerrainProfile);

		tviLayer.defaultPosition = INSERT_BEFORE_COMPASS;

		final DialogTerrainProfileConfig terrainProfileConfig = new DialogTerrainProfileConfig(
				_ww,
				profileLayer,
				_state);

		tviLayer.toolProvider = terrainProfileConfig.getToolProvider();

		_uiCustomLayers.put(layerId, tviLayer);
	}

	/**
	 * These layers are defined as default in WorldWind 1.5
	 * 
	 * <pre>
	 * 
	 * 		Stars							true		gov.nasa.worldwind.layers.StarsLayer
	 * 		Atmosphere						true        gov.nasa.worldwind.layers.SkyGradientLayer
	 * 		NASA Blue Marble Image			true        gov.nasa.worldwind.layers.Earth.BMNGOneImage
	 * 		Blue Marble (WMS) 2004			true        gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		i-cubed Landsat					true        gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		USDA NAIP						false       gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		USDA NAIP USGS					false       gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		MS Virtual Earth Aerial			false       gov.nasa.worldwind.layers.BasicTiledImageLayer
	 * 		Bing Imagery					false       gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		USGS Topographic Maps 1:250K	false       gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		USGS Topographic Maps 1:100K	false       gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		USGS Topographic Maps 1:24K		false       gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		USGS Urban Area Ortho			false       gov.nasa.worldwind.layers.BasicTiledImageLayer
	 * 		Political Boundaries			false       gov.nasa.worldwind.layers.Earth.CountryBoundariesLayer
	 * 		Open Street Map					false       gov.nasa.worldwind.wms.WMSTiledImageLayer
	 * 		Place Names						true        gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer
	 * 		World Map						true        gov.nasa.worldwind.layers.WorldMapLayer
	 * 		Scale bar						true        gov.nasa.worldwind.layers.ScalebarLayer
	 * 		Compass							true        gov.nasa.worldwind.layers.CompassLayer
	 * 
	 * </pre>
	 * 
	 * @return
	 */
	private static XMLMemento createLayerXml_0_DefaultLayer() {

		XMLMemento xmlRoot;

		try {

			xmlRoot = createLayerXml_100_WriteRoot();

			/*
			 * Category: Map
			 */
			final IMemento xmlCategoryMap = createLayerXml_110_DefaultCategory(xmlRoot, MapDefaultCategory.ID_MAP);
			{
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_STARS);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_ATMOSPHERE);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_NASA_BLUE_MARBLE_IMAGE);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_BLUE_MARBLE_WMS_2004);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_I_CUBED_LANDSAT);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_USDA_NAIP);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_USDA_NAIP_USGS);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_MS_VIRTUAL_EARTH_AERIAL);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_BING_IMAGERY);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_250K);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_100K);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_24K);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_USGS_URBAN_AREA_ORTHO);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, true, MapDefaultLayer.ID_POLITICAL_BOUNDARIES);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_OPEN_STREET_MAP);
				createLayerXml_120_DefaultLayer(xmlCategoryMap, false, MapDefaultLayer.ID_EARTH_AT_NIGHT);
			}

			/*
			 * Category: Tour
			 */
			final IMemento xmlCategoryTour = createLayerXml_110_DefaultCategory(xmlRoot, MapDefaultCategory.ID_TOUR);
			{
				createLayerXml_120_DefaultLayer(xmlCategoryTour, true, MapDefaultLayer.ID_PLACE_NAMES);
			}

			/*
			 * Category: Info
			 */
			final IMemento xmlCategoryInfo = createLayerXml_110_DefaultCategory(xmlRoot, MapDefaultCategory.ID_INFO);
			{
				createLayerXml_120_DefaultLayer(xmlCategoryInfo, true, MapDefaultLayer.ID_WORLD_MAP);
				createLayerXml_120_DefaultLayer(xmlCategoryInfo, true, MapDefaultLayer.ID_SCALE_BAR);
			}

			/*
			 * Category: Tools
			 */
			final IMemento xmlCategoryTools = createLayerXml_110_DefaultCategory(xmlRoot, MapDefaultCategory.ID_TOOL);
			{
				createLayerXml_120_DefaultLayer(xmlCategoryTools, true, MapDefaultLayer.ID_COMPASS);
			}

		} catch (final Exception e) {
			throw new Error(e.getMessage());
		}

		return xmlRoot;
	}

	private static XMLMemento createLayerXml_100_WriteRoot() {

		final XMLMemento xmlRoot = XMLMemento.createWriteRoot(TAG_ROOT);

		// date/time
		xmlRoot.putString(Util.ATTR_ROOT_DATETIME, _dtFormatter.print(new DateTime()));

		// plugin version
		final Version version = _bundle.getVersion();
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MAJOR, version.getMajor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MINOR, version.getMinor());
		xmlRoot.putInteger(Util.ATTR_ROOT_VERSION_MICRO, version.getMicro());
		xmlRoot.putString(Util.ATTR_ROOT_VERSION_QUALIFIER, version.getQualifier());

		// layer structure version
		xmlRoot.putInteger(ATTR_MAP3_LAYER_VERSION, MAP3_LAYER_STRUCTURE_VERSION);

		return xmlRoot;
	}

	private static IMemento createLayerXml_110_DefaultCategory(final IMemento xmlRoot, final String categoryId) {

		final IMemento xmlCategory = xmlRoot.createChild(TAG_CATEGORY);

		xmlCategory.putString(ATTR_ID, categoryId);
		xmlCategory.putBoolean(ATTR_IS_EXPANDED, true);

		return xmlCategory;
	}

	private static void createLayerXml_120_DefaultLayer(final IMemento xmlCategory,
														final boolean isEnabled,
														final String defaultLayerId) {

		final DefaultLayer mapDefaultLayer = MapDefaultLayer.getLayer(defaultLayerId);

		if (mapDefaultLayer == null) {
			return;
		}

		final IMemento xmlLayer = xmlCategory.createChild(TAG_LAYER);

		xmlLayer.putString(ATTR_ID, defaultLayerId);
		xmlLayer.putBoolean(ATTR_IS_ENABLED, isEnabled);
		xmlLayer.putBoolean(ATTR_IS_DEFAULT_LAYER, true);
	}

	/**
	 * @return
	 */
	private static XMLMemento createLayerXml_200_FromTreeItems() {

		XMLMemento xmlRoot = null;

		try {

			xmlRoot = createLayerXml_100_WriteRoot();

			createLayerXml_210_FromTreeChildren(_uiRootItem, xmlRoot);

		} catch (final Exception e) {
			StatusUtil.log(e);
		}

		return xmlRoot;
	}

	/**
	 * * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * <p>
	 * RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE
	 * <p>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * 
	 * @param parentItem
	 * @param xmlParent
	 */
	private static void createLayerXml_210_FromTreeChildren(final TreeViewerItem parentItem, final XMLMemento xmlParent) {

		final ArrayList<TreeViewerItem> tviChildren = parentItem.getUnfetchedChildren();
		if (tviChildren == null) {
			return;
		}

		for (final TreeViewerItem tviItem : tviChildren) {

			if (tviItem instanceof TVIMap3Category) {

				final TVIMap3Category tviCategory = (TVIMap3Category) tviItem;

				final XMLMemento xmlCategory = (XMLMemento) xmlParent.createChild(TAG_CATEGORY);

				xmlCategory.putString(ATTR_ID, tviCategory.getId());
				xmlCategory.putBoolean(ATTR_IS_EXPANDED, isCategoryExpanded(tviCategory));

				createLayerXml_210_FromTreeChildren(tviItem, xmlCategory);

			} else if (tviItem instanceof TVIMap3Layer) {

				final TVIMap3Layer tviLayer = (TVIMap3Layer) tviItem;

				final IMemento xmlLayer = xmlParent.createChild(TAG_LAYER);

				xmlLayer.putString(ATTR_ID, tviLayer.getId());
				xmlLayer.putBoolean(ATTR_IS_ENABLED, tviLayer.isLayerVisible);
				xmlLayer.putBoolean(ATTR_IS_DEFAULT_LAYER, tviLayer.isDefaultLayer);
			}
		}
	}

	static void dumpLayer(final LayerList layers) {

		System.out.println(UI.timeStampNano() + " [" + Map3Manager.class.getSimpleName() + "] WW Layers"); //$NON-NLS-1$ //$NON-NLS-2$
		// TODO remove SYSTEM.OUT.PRINTLN

		for (final Layer layer : layers) {

			final String name = layer.getName();

//			final double minActiveAltitude = layer.getMinActiveAltitude();
//			final double maxActiveAltitude = layer.getMaxActiveAltitude();

//			System.out.println();
			System.out.println(String.format("\t%-60s %s", layer.getClass().getName(), name)); //$NON-NLS-1$
//			System.out.println(" \tMinMax altitude:\t" //$NON-NLS-1$
//					+ UI.FormatDoubleMinMax(minActiveAltitude)
//					+ ("\t" + UI.FormatDoubleMinMax(maxActiveAltitude))); //$NON-NLS-1$
//			// TODO remove SYSTEM.OUT.PRINTLN

//			if (layer instanceof WMSTiledImageLayer) {
//
//				final WMSTiledImageLayer wmsLayer = (WMSTiledImageLayer) layer;
//
//				System.out.println(" \tLevels\t\t\t" + wmsLayer.getLevels());
//				System.out.println(" \tScale\t\t\t" + wmsLayer.getScale());
//			}

		}
	}

	/**
	 * @param isTrackVisible
	 */
	public static void enableMap3Actions() {

		if (_map3View == null) {
			// ignore when view is not created
			return;
		}

		_map3View.updateActionsState();
	}

	public static MarkerLayer getLayer_Marker() {
		return _wwLayer_Marker;
	}

	public static TourInfoLayer getLayer_TourInfo() {
		return _wwLayer_TourInfo;
	}

	public static TourLegendLayer getLayer_TourLegend() {
		return _wwLayer_TourLegend;
	}

	public static TourTrackLayer getLayer_TourTrack() {
		return _wwLayer_TourTrack;
	}

	public static TrackSliderLayer getLayer_TrackSlider() {
		return _wwLayer_TrackSlider;
	}

	private static File getLayerXmlFile() {

		final File layerFile = _stateLocation.append(MAP3_LAYER_STRUCTURE_FILE_NAME).toFile();

		return layerFile;
	}

	/**
	 * @return Returns an instance of {@link DialogMap3Layer} or <code>null</code> when view is not
	 *         created.
	 */
	public static DialogMap3Layer getMap3LayerDialog() {
		return _map3LayerDialog;
	}

	/**
	 * @return Returns instance of {@link Map3View} or <code>null</code> when view is not created.
	 */
	public static Map3View getMap3View() {
		return _map3View;
	}

	public static TVIMap3Root getRootItem() {
		return _uiRootItem;
	}

	public static Object[] getUIExpandedCategories() {
		return _uiExpandedCategories;
	}

	public static Object[] getUIVisibleLayers() {
		return _uiVisibleLayers;
	}

	public static WorldWindowGLCanvas getWWCanvas() {
		return _ww;
	}

	/**
	 * Insert the layer into the layer list just before the compass.
	 * 
	 * @param wwd
	 * @param newWWLayer
	 * @return
	 */
	private static TVIMap3Layer insertBeforeCompass(final WorldWindow wwd, final TVIMap3Layer newUILayer) {

		/*
		 * update WW model
		 */
		int compassPosition = 0;
		final LayerList wwLayers = wwd.getModel().getLayers();
		for (final Layer wwLayer : wwLayers) {
			if (wwLayer instanceof CompassLayer) {
				compassPosition = wwLayers.indexOf(wwLayer);
			}
		}

		final Layer newWWLayer = newUILayer.wwLayer;

		wwLayers.add(compassPosition, newWWLayer);

		// update ww layer visibility
		newWWLayer.setEnabled(newUILayer.isLayerVisible);

		/*
		 * update UI model
		 */
		final TVIMap3Layer insertedUILayer = insertBeforeCompass_10(_uiRootItem, newUILayer);

		return insertedUILayer;
	}

	/**
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * <p>
	 * RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE
	 * <p>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * 
	 * @param tviParent
	 * @param newWWLayer
	 * @param newUILayer
	 * @return
	 */
	private static TVIMap3Layer insertBeforeCompass_10(final TVIMap3Item tviParent, final TVIMap3Layer newUILayer) {

		for (final TreeViewerItem tviChild : tviParent.getFetchedChildren()) {

			if (tviChild instanceof TVIMap3Layer) {

				final TVIMap3Layer tviLayer = (TVIMap3Layer) tviChild;
				if (tviLayer.wwLayer instanceof CompassLayer) {

					// compass layer found in ui model

					tviParent.addChildBefore(tviLayer, newUILayer);

					return newUILayer;
				}

			} else if (tviChild instanceof TVIMap3Category) {

				final TVIMap3Layer insertedLayer = insertBeforeCompass_10((TVIMap3Category) tviChild, newUILayer);

				if (insertedLayer != null) {
					// new layer is inserted
					return insertedLayer;
				}
			}
		}

		return null;
	}

	/**
	 * Insert the layer into the layer list just before the placenames.
	 * 
	 * @param wwd
	 * @param newWWLayer
	 * @return
	 */
	private static TVIMap3Layer insertBeforePlaceNames(final WorldWindow wwd, final TVIMap3Layer newUILayer) {

		/*
		 * update WW model
		 */
		int compassPosition = 0;
		final LayerList wwLayers = wwd.getModel().getLayers();
		for (final Layer wwLayer : wwLayers) {
			if (wwLayer instanceof PlaceNameLayer) {
				compassPosition = wwLayers.indexOf(wwLayer);
			}
		}

		final Layer newWWLayer = newUILayer.wwLayer;

		wwLayers.add(compassPosition, newWWLayer);

		// update ww layer visibility
		newWWLayer.setEnabled(newUILayer.isLayerVisible);

		/*
		 * update UI model
		 */
		final TVIMap3Layer insertedUILayer = insertBeforePlaceNames_10(_uiRootItem, newUILayer);

		return insertedUILayer;
	}

	/**
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * <p>
	 * RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE
	 * <p>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * 
	 * @param tviParent
	 * @param newWWLayer
	 * @param newUILayer
	 * @return
	 */
	private static TVIMap3Layer insertBeforePlaceNames_10(final TVIMap3Item tviParent, final TVIMap3Layer newUILayer) {

		for (final TreeViewerItem tviChild : tviParent.getFetchedChildren()) {

			if (tviChild instanceof TVIMap3Layer) {

				final TVIMap3Layer tviLayer = (TVIMap3Layer) tviChild;
				if (tviLayer.wwLayer instanceof PlaceNameLayer) {

					// requested layer found in ui model

					tviParent.addChildBefore(tviLayer, newUILayer);

					return newUILayer;
				}

			} else if (tviChild instanceof TVIMap3Category) {

				final TVIMap3Layer insertedLayer = insertBeforePlaceNames_10((TVIMap3Category) tviChild, newUILayer);

				if (insertedLayer != null) {
					// new layer is inserted
					return insertedLayer;
				}
			}
		}

		return null;
	}

	private static boolean isCategoryExpanded(final TVIMap3Category tviCategory) {

		if (_uiExpandedCategories == null) {
			return false;
		}

		for (final Object expandedElement : _uiExpandedCategories) {
			if (expandedElement == tviCategory) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Read/Create map layers with it's state from a xml file
	 * 
	 * @return
	 */
	private static TVIMap3Root parseLayerXml() {

		final TVIMap3Root tviRoot = new TVIMap3Root();

		InputStreamReader reader = null;

		try {

			XMLMemento xmlRoot = null;

			// try to get layer structure from saved xml file
			final File layerFile = getLayerXmlFile();
			final String absoluteLayerPath = layerFile.getAbsolutePath();

			final File inputFile = new File(absoluteLayerPath);
			if (inputFile.exists()) {

				try {

					reader = new InputStreamReader(new FileInputStream(inputFile), UI.UTF_8);
					xmlRoot = XMLMemento.createReadRoot(reader);

				} catch (final Exception e) {
					// ignore
				}
			}

			Integer layerVersion = null;

			// get current layer version, when available
			if (xmlRoot != null) {
				layerVersion = xmlRoot.getInteger(ATTR_MAP3_LAYER_VERSION);
			}

			if (xmlRoot == null || layerVersion == null || layerVersion < MAP3_LAYER_STRUCTURE_VERSION) {

				// create default layer tree
				xmlRoot = createLayerXml_0_DefaultLayer();
			}

			parseLayerXml_10_Children(xmlRoot, tviRoot);

			_uiVisibleLayers = _uiVisibleLayersFromXml.toArray();
			_uiExpandedCategories = _uiExpandedCategoriesFromXml.toArray();

		} catch (final Exception e) {
			StatusUtil.log(e);
		} finally {

			if (reader != null) {
				try {
					reader.close();
				} catch (final IOException e) {
					StatusUtil.log(e);
				}
			}
		}

		return tviRoot;
	}

	/**
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * <p>
	 * RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE RECURSIVE
	 * <p>
	 * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
	 * 
	 * @param xmlParent
	 * @param tviParent
	 */
	private static void parseLayerXml_10_Children(final XMLMemento xmlParent, final TVIMap3Item tviParent) {

		for (final IMemento mementoChild : xmlParent.getChildren()) {

			if (mementoChild instanceof XMLMemento) {

				final XMLMemento xmlChild = (XMLMemento) mementoChild;

				try {

					final String xmlType = xmlChild.getType();
					final String xmlChildId = xmlChild.getString(ATTR_ID);

					if (xmlType.equals(TAG_LAYER)) {

						final boolean isEnabled = Boolean.TRUE.equals(xmlChild.getBoolean(ATTR_IS_ENABLED));
						final boolean isDefaultLayer = Boolean.TRUE.equals(xmlChild.getBoolean(ATTR_IS_DEFAULT_LAYER));

						TVIMap3Layer tviLayer;

						if (isDefaultLayer) {

							// this is a default layer

							final String localeLayerId = MapDefaultLayer.getLocaleLayerId(xmlChildId);

							final Layer wwLayer = _wwDefaultLocaleLayers.getLayerByName(localeLayerId);

							// check if xml layer is a default ww layer
							if (wwLayer == null) {
								StatusUtil.log(NLS.bind(ERROR_01, xmlChildId));
								continue;
							}

							final DefaultLayer mapDefaultLayer = MapDefaultLayer.getLayer(xmlChildId);
							// use untranslated name, this should not occure
							if (mapDefaultLayer == null) {
								StatusUtil.log(NLS.bind(ERROR_02, xmlChildId));
								continue;
							}

							final String layerName = mapDefaultLayer.layerName;

							tviLayer = new TVIMap3Layer(xmlChildId, wwLayer, layerName);

							tviLayer.isDefaultLayer = true;

							_xmlLayers.add(wwLayer);

						} else {

							// this is NO default layer

							tviLayer = _uiCustomLayers.get(xmlChildId);

							if (tviLayer == null) {
								StatusUtil.log(NLS.bind(ERROR_03, xmlChildId));
								continue;
							}

							_xmlLayers.add(tviLayer.wwLayer);
						}

						/*
						 * set layer/UI enable state
						 */
						tviLayer.isLayerVisible = isEnabled;
						tviLayer.wwLayer.setEnabled(isEnabled);
						if (isEnabled) {
							_uiVisibleLayersFromXml.add(tviLayer);
						} else {
							// ensure that default enabled layer are hidden (which are created BEFORE xml file is read) !!!
							_uiVisibleLayersFromXml.remove(tviLayer);
						}

						tviParent.addChild(tviLayer);

					} else if (xmlType.equals(TAG_CATEGORY)) {

						final DefaultCategory defaultCategory = MapDefaultCategory.getLayer(xmlChildId);
						if (defaultCategory == null) {
							StatusUtil.log(NLS.bind(ERROR_04, xmlChildId));
							continue;
						}

						final String categoryName = defaultCategory.categoryName;

						final TVIMap3Category tviCategory = new TVIMap3Category(xmlChildId, categoryName);

						tviParent.addChild(tviCategory);

						// set expanded state
						final Boolean isExpanded = Boolean.TRUE.equals(xmlChild.getBoolean(ATTR_IS_EXPANDED));
						if (isExpanded != null && isExpanded) {
							_uiExpandedCategoriesFromXml.add(tviCategory);
						}

						parseLayerXml_10_Children(xmlChild, tviCategory);
					}
				} catch (final Exception e) {
					StatusUtil.log(Util.dumpMemento(xmlChild), e);
				}
			}
		}
	}

	public static void redrawMap() {

		final View view = _ww.getView();

		view.firePropertyChange(AVKey.VIEW, null, view);

//		_ww.redraw();

//		if (_map3View != null) {
//
//			_map3View.redraw();
//		}
	}

	public static void saveState() {

		Map3GradientColorManager.saveColors();

		_wwLayer_TourTrack.saveState(_state);

		/*
		 * save layer structure in xml file
		 */
		final XMLMemento xmlRoot = createLayerXml_200_FromTreeItems();
		final File xmlFile = getLayerXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	/**
	 * <pre>
	 * //		view.set
	 * //
	 * //
	 * //        BasicFlyView view = (BasicFlyView) this.wwd.getView();
	 * //
	 * //        // Stop iterators first
	 * //        view.stopAnimations();
	 * //
	 * //        // Save current eye position
	 * //        final Position pos = view.getEyePosition();
	 * //
	 * //        // Set view heading, pitch and fov
	 * //        view.setHeading(Angle.fromDegrees(this.headingSlider.getValue()));
	 * //        view.setPitch(Angle.fromDegrees(this.pitchSlider.getValue()));
	 * //        view.setFieldOfView(Angle.fromDegrees(this.fovSlider.getValue()));
	 * //        view.setRoll(Angle.fromDegrees(this.rollSlider.getValue()));
	 * //        //view.setZoom(0);
	 * //
	 * //        // Restore eye position
	 * //        view.setEyePosition(pos);
	 * ////        System.out.println(&quot;Eye Position: &quot; + pos.latitude.toString() + &quot; , &quot; + pos.longitude.toString() + &quot;, &quot; + pos.getElevation());
	 * ////        System.out.println(&quot;Orient: &quot; + view.getHeading() + &quot;, &quot; + view.getPitch() + &quot;, &quot; + view.getRoll() );
	 * //
	 * //        // Redraw
	 * //        this.wwd.redraw();
	 * </pre>
	 */

	public static void saveUIState(final Object[] checkedElements, final Object[] expandedElements) {

		/*
		 * remove categories because they are contained in this list even when only a part of the
		 * children are checked
		 */
		final ArrayList<TVIMap3Layer> enabledLayers = new ArrayList<TVIMap3Layer>();
		for (final Object object : checkedElements) {
			if (object instanceof TVIMap3Layer) {
				enabledLayers.add((TVIMap3Layer) object);
			}
		}

		_uiVisibleLayers = enabledLayers.toArray();
		_uiExpandedCategories = expandedElements;
	}

	/**
	 * Ensure All custom layers are set in the ww model.
	 * 
	 * @param wwLayers
	 *            Layers which are already added to the model.
	 */
	private static void setCustomLayerInWWModel(final LayerList wwLayers) {

		final ArrayList<TVIMap3Layer> insertedLayers = new ArrayList<TVIMap3Layer>();

		for (final TVIMap3Layer tviLayer : _uiCustomLayers.values()) {

			final Layer customWWLayer = tviLayer.wwLayer;

			if (wwLayers.contains(customWWLayer)) {

				// layer is already contained in the model
				continue;
			}

			TVIMap3Layer insertedUILayer = null;

			// update ui/ww model
			if (tviLayer.defaultPosition == INSERT_BEFORE_COMPASS) {

				insertedUILayer = insertBeforeCompass(_ww, tviLayer);

			} else if (tviLayer.defaultPosition == INSERT_BEFORE_PLACE_NAMES) {

				insertedUILayer = insertBeforePlaceNames(_ww, tviLayer);

			} else {

				// insert in default position
				insertedUILayer = insertBeforeCompass(_ww, tviLayer);
			}

			insertedLayers.add(insertedUILayer);
		}

		// update UI
		if (_map3LayerDialog != null && insertedLayers.size() > 0) {
			_map3LayerDialog.updateUI_NewLayer(insertedLayers);
		}

	}

	private static void setCustomLayerVisibleInTVIModel(final boolean isVisible, final String customLayerId) {

		final TVIMap3Layer tviLayer = _uiCustomLayers.get(customLayerId);

		// update tvi model
		tviLayer.isLayerVisible = isVisible;

		if (_map3LayerDialog != null) {

			// update viewer
			_map3LayerDialog.setLayerVisible(tviLayer, isVisible);
		}

		// add/remove layer listener
		tviLayer.fireCheckStateListener();
	}

	static void setLayerVisible_Legend(final boolean isLegendVisible) {

		// update ww model
		_wwLayer_TourLegend.setEnabled(isLegendVisible);

		// update UI
		setCustomLayerVisibleInTVIModel(isLegendVisible, TourLegendLayer.MAP3_LAYER_ID);
	}

	static void setLayerVisible_Marker(final boolean isMarkerVisible) {

		// update ww model
		_wwLayer_Marker.setEnabled(isMarkerVisible);

		// update UI
		setCustomLayerVisibleInTVIModel(isMarkerVisible, MarkerLayer.MAP3_LAYER_ID);
	}

	/**
	 * Show/hide tour track layer.
	 * 
	 * @param isTrackVisible
	 */
	static void setLayerVisible_TourTrack(final boolean isTrackVisible) {

		if (_map3LayerDialog == null) {

			// update model, layer viewer is not displayed

			_wwLayer_TourTrack.setEnabled(isTrackVisible);

		} else {

			// update model and UI

			final TVIMap3Layer tviMap3Layer = _uiCustomLayers.get(TourTrackLayer.MAP3_LAYER_ID);

			_map3LayerDialog.setLayerVisible_TourTrack(tviMap3Layer, isTrackVisible);
		}
	}

	static void setLayerVisible_TrackSlider(final boolean isVisible) {

		// update ww model
		_wwLayer_TrackSlider.setEnabled(isVisible);

		// update UI
		setCustomLayerVisibleInTVIModel(isVisible, TrackSliderLayer.MAP3_LAYER_ID);
	}

	public static void setMap3LayerDialog(final DialogMap3Layer dialogMap3LayerViewer) {

		_map3LayerDialog = dialogMap3LayerViewer;
	}

	/**
	 * Keep track if {@link Map3View} is visible.
	 * 
	 * @param map3View
	 */
	static void setMap3View(final Map3View map3View) {
		_map3View = map3View;
	}

	private static void setToolLayerInWWModel() {

		for (final IToolLayer toolLayer : _toolLayers.values()) {

			final int defaultPosition = toolLayer.getDefaultPosition();

			if (defaultPosition == INSERT_BEFORE_COMPASS) {

				ApplicationTemplate.insertBeforeCompass(_ww, toolLayer);

			} else if (defaultPosition == INSERT_BEFORE_PLACE_NAMES) {

				ApplicationTemplate.insertBeforePlacenames(_ww, toolLayer);

			} else {

				// ensure it's displayed

				ApplicationTemplate.insertBeforeCompass(_ww, toolLayer);
			}
		}
	}

}
