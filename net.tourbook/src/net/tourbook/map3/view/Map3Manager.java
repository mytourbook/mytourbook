/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Messages;
import net.tourbook.map3.layer.DefaultLayer;
import net.tourbook.map3.layer.MapDefaultLayer;
import net.tourbook.map3.layer.StatusLayer;
import net.tourbook.map3.layer.tourtrack.TourTrackLayerWithPaths;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

public class Map3Manager {

	static final int								WW_DEFAULT_LAYER_ID				= 1;

	public static final String						PROPERTY_NAME_ENABLED			= "Enabled";							//$NON-NLS-1$

	private static final String						MAP3_LAYER_STRUCTURE_FILE_NAME	= "map3-layers.xml";					//$NON-NLS-1$

	/**
	 * This version number is incremented, when structural changes (e.g. new category) are done.
	 * When this happens, the <b>default</b> structure is created.
	 */
	private static final int						MAP3_LAYER_STRUCTURE_VERSION	= 3;

	private static final String						ATTR_MAP3_LAYER_VERSION			= "map3LayerVersion";					//$NON-NLS-1$

	private static final String						TAG_ROOT						= "Map3LayerStructure";				//$NON-NLS-1$
	private static final String						TAG_CATEGORY					= "category";							//$NON-NLS-1$
	private static final String						TAG_LAYER						= "layer";								//$NON-NLS-1$

	private static final String						ATTR_NAME						= "name";								//$NON-NLS-1$

	private static final String						ATTR_ID							= "id";								//$NON-NLS-1$
	private static final String						ATTR_IS_DEFAULT_LAYER			= "isDefaultLayer";					//$NON-NLS-1$
	private static final String						ATTR_IS_ENABLED					= "isEnabled";							//$NON-NLS-1$
	private static final String						ATTR_IS_EXPANDED				= "isExpanded";						//$NON-NLS-1$

	private static final int						INSERT_BEFORE_COMPASS			= 1;
	private static final int						INSERT_BEFORE_PLACE_NAMES		= 2;

	/**
	 * _bundle must be set here otherwise an exception occures in saveState()
	 */
	private static final Bundle						_bundle							= TourbookPlugin.getDefault()//
																							.getBundle();
	private static final IDialogSettings			_state							= TourbookPlugin
																							.getStateSection(Map3Manager.class
																									.getCanonicalName());

	private static final IPath						_stateLocation					= Platform
																							.getStateLocation(_bundle);
	/**
	 * Root item for the layer tree viewer. This contains the UI model.
	 */
	private static TVIMap3Root						_uiRootItem;

	private static final WorldWindowGLCanvas		_ww;

	/**
	 * Instance of {@link Map3View} or <code>null</code> when view is not created.
	 */
	private static Map3View							_map3View;

	/**
	 * Instance of {@link Map3LayerView} or <code>null</code> when view is not created or disposed.
	 */
	private static Map3LayerView					_map3LayerView;

	private static LayerList						_wwDefaultLayers;

	/**
	 * Contains custom (none default) layers, key is layerId.
	 */
	private static HashMap<String, TVIMap3Layer>	_customLayers					= new HashMap<String, TVIMap3Layer>();

	private static TourTrackLayerWithPaths			_tourTrackLayer;

	private static Object[]							_uiEnabledLayers;

	private static Object[]							_uiExpandedCategories;
	private static ArrayList<TVIMap3Layer>			_uiEnabledLayersFromXml			= new ArrayList<TVIMap3Layer>();
	private static ArrayList<TVIMap3Category>		_uiExpandedCategoriesFromXml	= new ArrayList<TVIMap3Category>();
	private static ArrayList<Layer>					_xmlLayers						= new ArrayList<Layer>();

	private static final DateTimeFormatter			_dtFormatter					= ISODateTimeFormat
																							.basicDateTimeNoMillis();

	static {

		// copied from gov.nasa.worldwindx.examples.ApplicationTemplate - 28.7.2013
//        System.setProperty("java.net.useSystemProxies", "true");
		if (Configuration.isMacOS()) {
//            System.setProperty("apple.laf.useScreenMenuBar", "true");
//            System.setProperty("com.apple.mrj.application.apple.menu.about.name", "World Wind Application");
//            System.setProperty("com.apple.mrj.application.growbox.intrudes", "false");
//            System.setProperty("apple.awt.brushMetalLook", "true");
		} else if (Configuration.isWindowsOS()) {
			System.setProperty("sun.awt.noerasebackground", "true"); // prevents flashing during window resizing
		}

		_ww = new WorldWindowGLCanvas();

		_initializeMap3();
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
		_wwDefaultLayers = wwModel.getLayers();

		// create custom layer BEFORE state is applied and xml file is read which references these layers
		createCustomLayer_TourTracks();
		createCustomLayer_MapStatus();
		createCustomLayer_ViewerController();
		createCustomLayer_TerrainProfile();

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

//		gov.nasa.worldwindx.examples.kml.KMLViewController

//		this.viewController = new ViewController(this.getWwd());
//		this.viewController.setObjectsToTrack(this.objectsToTrack);

	}

	private static void createCustomLayer_MapStatus() {

		/*
		 * create WW layer
		 */
		final StatusLayer statusLayer = new StatusLayer();
		//this.layer = new StatusMGRSLayer();
		//this.layer = new StatusUTMLayer();

		statusLayer.setEventSource(_ww);
		statusLayer.setCoordDecimalPlaces(2); // default is 4
		//layer.setElevationUnits(StatusLayer.UNIT_IMPERIAL);

		/*
		 * create UI model layer
		 */
		final String layerId = StatusLayer.class.getCanonicalName();

		final TVIMap3Layer tviLayer = new TVIMap3Layer(statusLayer, statusLayer.getName());

		tviLayer.id = layerId;

		final boolean isVisible = true;

		// default is enabled
		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_COMPASS;

		if (isVisible) {
			_uiEnabledLayersFromXml.add(tviLayer);
		}

		_customLayers.put(layerId, tviLayer);
	}

	private static void createCustomLayer_TerrainProfile() {

		/*
		 * create WW layer
		 */
		// Add terrain profile layer
//		final TerrainProfileLayer profileLayer = new TerrainProfileLayer();
//		profileLayer.setEventSource(_wwCanvas);
//		profileLayer.setFollow(TerrainProfileLayer.FOLLOW_PATH);
//		profileLayer.setShowProfileLine(false);

//		final boolean isVisible = false;

		// Add TerrainProfileLayer
		final TerrainProfileLayer profileLayer = new TerrainProfileLayer();
		profileLayer.setEventSource(_ww);
		profileLayer.setStartLatLon(LatLon.fromDegrees(0, -10));
		profileLayer.setEndLatLon(LatLon.fromDegrees(0, 65));

		/*
		 * create UI model layer
		 */
		final String layerId = TerrainProfileLayer.class.getCanonicalName();

		final TVIMap3Layer tviLayer = new TVIMap3Layer(profileLayer, profileLayer.getName());

		tviLayer.id = layerId;

		// default is enabled
//		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_COMPASS;

		final DialogTerrainProfileConfig terrainProfileConfig = new DialogTerrainProfileConfig(
				_ww,
				profileLayer,
				_state);
		tviLayer.toolProvider = terrainProfileConfig.getToolProvider();

//		if (isVisible) {
//			_uiEnabledLayersFromXml.add(tviLayer);
//		}

		_customLayers.put(layerId, tviLayer);
	}

	private static void createCustomLayer_TourTracks() {

		/*
		 * create WW layer
		 */
//		_tourTrackLayer = new TourTrackLayerWithMarkers();
		_tourTrackLayer = new TourTrackLayerWithPaths(_state);
//		_tourTrackLayer = new TourTrackLayerWithFastShape();

		/*
		 * create UI model layer
		 */
		final TVIMap3Layer tviLayer = new TVIMap3Layer(_tourTrackLayer, _tourTrackLayer.getName());

		final String layerId = TourTrackLayerWithPaths.MAP3_LAYER_ID;
		tviLayer.id = layerId;

		final boolean isVisible = true;

		// default is enabled
		tviLayer.isLayerVisible = isVisible;
		tviLayer.defaultPosition = INSERT_BEFORE_PLACE_NAMES;

		if (isVisible) {
			_uiEnabledLayersFromXml.add(tviLayer);
		}

		tviLayer.addCheckStateListener(_tourTrackLayer);

		_customLayers.put(layerId, tviLayer);
	}

	private static void createCustomLayer_ViewerController() {

		/*
		 * create WW layer
		 */
		// Create and install the view controls layer and register a controller for it with the World Window.
		final ViewControlsLayer viewControlsLayer = new ViewControlsLayer();

		/*
		 * create UI model layer
		 */
		final String layerId = ViewControlsLayer.class.getCanonicalName();

		final TVIMap3Layer tviLayer = new TVIMap3Layer(viewControlsLayer, viewControlsLayer.getName());

		tviLayer.id = layerId;

		// default is enabled
//		tviLayer.isLayerVisible = true;
		tviLayer.defaultPosition = INSERT_BEFORE_COMPASS;

		tviLayer.addCheckStateListener(new ViewerControllerCheckStateListener(viewControlsLayer));

//		_uiEnabledLayersFromXml.add(tviLayer);

		_customLayers.put(layerId, tviLayer);
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
	private static XMLMemento createLayerXml_0__DefaultLayer() {

		XMLMemento xmlRoot;

		try {

			xmlRoot = createLayerXml_10_WriteRoot();

			/*
			 * Category: Map
			 */
			final IMemento xmlWWCategoryMap = xmlRoot.createChild(TAG_CATEGORY);
			xmlWWCategoryMap.putString(ATTR_NAME, Messages.Default_Category_Name_Map);
			xmlWWCategoryMap.putBoolean(ATTR_IS_EXPANDED, true);
			{
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_STARS);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_ATMOSPHERE);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_NASA_BLUE_MARBLE_IMAGE);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_BLUE_MARBLE_WMS_2004);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_I_CUBED_LANDSAT);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, false, MapDefaultLayer.ID_USDA_NAIP);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, false, MapDefaultLayer.ID_USDA_NAIP_USGS);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_MS_VIRTUAL_EARTH_AERIAL);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_BING_IMAGERY);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, false, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_250K);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, false, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_100K);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, false, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_24K);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, false, MapDefaultLayer.ID_USGS_URBAN_AREA_ORTHO);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, true, MapDefaultLayer.ID_POLITICAL_BOUNDARIES);
				createLayerXml_20_DefaultLayer(xmlWWCategoryMap, false, MapDefaultLayer.ID_OPEN_STREET_MAP);
			}

			/*
			 * Category: Tour
			 */
			final IMemento xmlWWCategoryTour = xmlRoot.createChild(TAG_CATEGORY);
			xmlWWCategoryTour.putString(ATTR_NAME, Messages.Custom_Category_Name_Tour);
			xmlWWCategoryTour.putBoolean(ATTR_IS_EXPANDED, true);
			{
				createLayerXml_20_DefaultLayer(xmlWWCategoryTour, true, MapDefaultLayer.ID_PLACE_NAMES);
			}

			/*
			 * Category: Info
			 */
			final IMemento xmlWWCategoryInfo = xmlRoot.createChild(TAG_CATEGORY);
			xmlWWCategoryInfo.putString(ATTR_NAME, Messages.Default_Category_Name_Info);
			xmlWWCategoryInfo.putBoolean(ATTR_IS_EXPANDED, true);
			{
				createLayerXml_20_DefaultLayer(xmlWWCategoryInfo, true, MapDefaultLayer.ID_WORLD_MAP);
				createLayerXml_20_DefaultLayer(xmlWWCategoryInfo, true, MapDefaultLayer.ID_SCALE_BAR);
			}

			/*
			 * Category: Tools
			 */
			final IMemento xmlWWCategoryTools = xmlRoot.createChild(TAG_CATEGORY);
			xmlWWCategoryTools.putString(ATTR_NAME, Messages.Default_Category_Name_Tools);
			xmlWWCategoryTools.putBoolean(ATTR_IS_EXPANDED, true);
			{
				createLayerXml_20_DefaultLayer(xmlWWCategoryTools, true, MapDefaultLayer.ID_COMPASS);
			}

		} catch (final Exception e) {
			throw new Error(e.getMessage());
		}

		return xmlRoot;
	}

	private static XMLMemento createLayerXml_10_WriteRoot() {

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

	private static void createLayerXml_20_DefaultLayer(	final IMemento xmlCategory,
														final boolean isEnabled,
														final String defaultLayerId) {

		final DefaultLayer mapDefaultLayer = MapDefaultLayer.getLayer(defaultLayerId);

		if (mapDefaultLayer == null) {
			return;
		}

		final IMemento xmlLayer = xmlCategory.createChild(TAG_LAYER);

		xmlLayer.putString(ATTR_ID, defaultLayerId);
		xmlLayer.putString(ATTR_NAME, mapDefaultLayer.layerName);
		xmlLayer.putBoolean(ATTR_IS_ENABLED, isEnabled);
		xmlLayer.putBoolean(ATTR_IS_DEFAULT_LAYER, true);
	}

	/**
	 * @return
	 */
	private static XMLMemento createLayerXml_50_FromTreeItems() {

		XMLMemento xmlRoot = null;

		try {

			xmlRoot = createLayerXml_10_WriteRoot();

			createLayerXml_60_FromTreeChildren(_uiRootItem, xmlRoot);

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
	private static void createLayerXml_60_FromTreeChildren(final TreeViewerItem parentItem, final XMLMemento xmlParent) {

		final ArrayList<TreeViewerItem> tviChildren = parentItem.getUnfetchedChildren();
		if (tviChildren == null) {
			return;
		}

		for (final TreeViewerItem tviItem : tviChildren) {

			if (tviItem instanceof TVIMap3Category) {

				final TVIMap3Category tviCategory = (TVIMap3Category) tviItem;

				final XMLMemento xmlCategory = (XMLMemento) xmlParent.createChild(TAG_CATEGORY);

				xmlCategory.putString(ATTR_NAME, tviCategory.name);
				xmlCategory.putBoolean(ATTR_IS_EXPANDED, isCategoryExpanded(tviCategory));

				createLayerXml_60_FromTreeChildren(tviItem, xmlCategory);

			} else if (tviItem instanceof TVIMap3Layer) {

				final TVIMap3Layer tviLayer = (TVIMap3Layer) tviItem;

				final IMemento xmlLayer = xmlParent.createChild(TAG_LAYER);

				xmlLayer.putString(ATTR_ID, tviLayer.id);
				xmlLayer.putString(ATTR_NAME, tviLayer.name);
				xmlLayer.putBoolean(ATTR_IS_ENABLED, tviLayer.isLayerVisible);
				xmlLayer.putBoolean(ATTR_IS_DEFAULT_LAYER, tviLayer.isDefaultLayer);
			}
		}
	}

	static void dumpLayer(final LayerList layers) {

		for (final Layer layer : layers) {

			final String name = layer.getName();

			final double minActiveAltitude = layer.getMinActiveAltitude();
			final double maxActiveAltitude = layer.getMaxActiveAltitude();

			System.out.println();
			System.out.println(" layer: " + layer.getClass().getName() + "\t" + name); //$NON-NLS-1$ //$NON-NLS-2$
			System.out.println(" \tMinMax altitude:\t" //$NON-NLS-1$
					+ UI.FormatDoubleMinMax(minActiveAltitude)
					+ ("\t" + UI.FormatDoubleMinMax(maxActiveAltitude))); //$NON-NLS-1$
			// TODO remove SYSTEM.OUT.PRINTLN

//			if (layer instanceof WMSTiledImageLayer) {
//
//				final WMSTiledImageLayer wmsLayer = (WMSTiledImageLayer) layer;
//
//				System.out.println(" \tLevels\t\t\t" + wmsLayer.getLevels());
//				System.out.println(" \tScale\t\t\t" + wmsLayer.getScale());
//			}

//			for (final Entry<String, Object> layerEntry : layer.getEntries()) {
//
//				final Object layerEntryValue = layerEntry.getValue();
//
//				System.out.println(" \t" + layerEntry.getKey() + "\t" + layerEntryValue);
//				// TODO remove SYSTEM.OUT.PRINTLN
//
//				if (layerEntryValue instanceof AVList) {
//
//					final AVList avList = (AVList) layerEntryValue;
//
//					for (final Entry<String, Object> avListEntry : avList.getEntries()) {
//
//						System.out.println(" \t\t"
//								+ String.format("%-60s", avListEntry.getKey())
//								+ "\t"
//								+ avListEntry.getValue());
//						// TODO remove SYSTEM.OUT.PRINTLN
//					}
//				}
//			}
		}
	}

	/**
	 * @param isTrackVisible
	 */
	static void enableMap3Actions() {

		if (_map3View == null) {
			// ignore when view is not created
			return;
		}

		_map3View.enableActions();
	}

	private static File getLayerXmlFile() {

		final File layerFile = _stateLocation.append(MAP3_LAYER_STRUCTURE_FILE_NAME).toFile();

		return layerFile;
	}

	/**
	 * @return Returns instance of {@link Map3LayerView} or null when view is not created.
	 */
	static Map3LayerView getMap3PropertiesView() {
		return _map3LayerView;
	}

	/**
	 * @return Returns instance of {@link Map3View} or null when view is not created.
	 */
	public static Map3View getMap3View() {
		return _map3View;
	}

	static TVIMap3Root getRootItem() {
		return _uiRootItem;
	}

	public static TourTrackLayerWithPaths getTourTrackLayer() {
		return _tourTrackLayer;
	}

	static Object[] getUIEnabledLayers() {
		return _uiEnabledLayers;
	}

	static Object[] getUIExpandedCategories() {
		return _uiExpandedCategories;
	}

	public static WorldWindowGLCanvas getWWCanvas() {
		return _ww;
	}

	/**
	 * Insert the layer into the layer list just after the placenames.
	 * 
	 * @param wwd
	 * @param layer
	 */
	public static void insertAfterPlacenames(final WorldWindow wwd, final Layer layer) {

//		int compassPosition = 0;
//		final LayerList layers = wwd.getModel().getLayers();
//		for (final Layer l : layers) {
//			if (l instanceof PlaceNameLayer) {
//				compassPosition = layers.indexOf(l);
//			}
//		}
//		layers.add(compassPosition + 1, layer);
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
	 * Insert the layer into the layer list just before the target layer.
	 * 
	 * @param wwd
	 * @param layer
	 * @param targetName
	 */
	public static void insertBeforeLayerName(final WorldWindow wwd, final Layer layer, final String targetName) {

//		int targetPosition = 0;
//		final LayerList layers = wwd.getModel().getLayers();
//		for (final Layer l : layers) {
//			if (l.getName().indexOf(targetName) != -1) {
//				targetPosition = layers.indexOf(l);
//				break;
//			}
//		}
//		layers.add(targetPosition, layer);
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

					// compass layer found in ui model

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
				// get default layer tree
				xmlRoot = createLayerXml_0__DefaultLayer();
			}

			parseLayerXml_10_Children(xmlRoot, tviRoot);

			_uiEnabledLayers = _uiEnabledLayersFromXml.toArray();
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

					final String xmlName = xmlChild.getString(ATTR_NAME);
					final String xmlType = xmlChild.getType();

					if (xmlType.equals(TAG_LAYER)) {

						final String xmlLayerId = xmlChild.getString(ATTR_ID);
						final boolean isEnabled = Boolean.TRUE.equals(xmlChild.getBoolean(ATTR_IS_ENABLED));
						final boolean isDefaultLayer = Boolean.TRUE.equals(xmlChild.getBoolean(ATTR_IS_DEFAULT_LAYER));

						TVIMap3Layer tviLayer;

						if (isDefaultLayer) {

							// layer is a default layer

							final Layer wwLayer = _wwDefaultLayers.getLayerByName(xmlLayerId);

							// check if xml layer is a default ww layer
							if (wwLayer == null) {
								StatusUtil.log(NLS.bind(
										"NTMVMM001 layer \"{0}\" is not a ww default layer.", xmlLayerId));//$NON-NLS-1$
								continue;
							}

							String layerName;
							final DefaultLayer mapDefaultLayer = MapDefaultLayer.getLayer(xmlLayerId);

							// use untranslated name, this should not occure
							if (mapDefaultLayer == null) {
								StatusUtil.log(NLS.bind(
										"NTMVMM002 layer \"{0}\" is not defined as map default layer.", xmlLayerId));//$NON-NLS-1$
								layerName = xmlName;
							} else {
								layerName = mapDefaultLayer.layerName;
							}

							tviLayer = new TVIMap3Layer(wwLayer, layerName);

							tviLayer.id = xmlLayerId;
							tviLayer.isDefaultLayer = true;

							_xmlLayers.add(wwLayer);

						} else {

							// NO default layer

							tviLayer = _customLayers.get(xmlLayerId);

							if (tviLayer == null) {
								StatusUtil.log(NLS.bind("NTMVMM003 xml layer \"{0}\" is not available.", xmlLayerId));//$NON-NLS-1$
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
							_uiEnabledLayersFromXml.add(tviLayer);
						} else {
							// ensure that default enabled layer are hidden (which are created BEFORE xml file is read) !!!
							_uiEnabledLayersFromXml.remove(tviLayer);
						}

						tviParent.addChild(tviLayer);

					} else if (xmlType.equals(TAG_CATEGORY)) {

						final TVIMap3Category tviCategory = new TVIMap3Category(xmlName);

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

	public static void redraw() {

		_ww.getView().firePropertyChange(AVKey.VIEW, null, _ww.getView());
//		_ww.redraw();

//		if (_map3View != null) {
//
//			_map3View.redraw();
//		}
	}

	public static void saveState() {

		_tourTrackLayer.saveState();

		/*
		 * save layer structure in xml file
		 */
		final XMLMemento xmlRoot = createLayerXml_50_FromTreeItems();
		final File xmlFile = getLayerXmlFile();

		Util.writeXml(xmlRoot, xmlFile);
	}

	static void saveUIState(final Object[] checkedElements, final Object[] expandedElements) {

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

		_uiEnabledLayers = enabledLayers.toArray();
		_uiExpandedCategories = expandedElements;
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

	/**
	 * Ensure All custom layers are set in the ww model.
	 * 
	 * @param wwLayers
	 *            Layers which are already added to the model.
	 */
	private static void setCustomLayerInWWModel(final LayerList wwLayers) {

		final ArrayList<TVIMap3Layer> insertedLayers = new ArrayList<TVIMap3Layer>();

		for (final TVIMap3Layer tviLayer : _customLayers.values()) {

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
		if (_map3LayerView != null && insertedLayers.size() > 0) {
			_map3LayerView.updateUI_NewLayer(insertedLayers);
		}
	}

	/**
	 * @param map3PropertiesView
	 */
	static void setMap3PropertiesView(final Map3LayerView map3PropertiesView) {
		_map3LayerView = map3PropertiesView;
	}

	/**
	 * Keep track if {@link Map3View} is visible.
	 * 
	 * @param map3View
	 */
	static void setMap3View(final Map3View map3View) {
		_map3View = map3View;
	}

	/**
	 * Show/hide tour track layer.
	 * 
	 * @param isTrackVisible
	 */
	static void setTourTrackVisible(final boolean isTrackVisible) {

		if (_map3LayerView == null) {

			// layer viewer is not displayed, update model

			_tourTrackLayer.setEnabled(isTrackVisible);

		} else {

			// update model and UI

			_map3LayerView.setTourTrackLayerVisibility(
					_customLayers.get(TourTrackLayerWithPaths.MAP3_LAYER_ID),
					isTrackVisible);
		}
	}

}
