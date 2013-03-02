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

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.CompassLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.placename.PlaceNameLayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Activator;
import net.tourbook.map3.layer.DefaultLayer;
import net.tourbook.map3.layer.MapDefaultLayer;
import net.tourbook.map3.layer.StatusLayer;

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

	private static final String						MAP3_LAYER_STRUCTURE_FILE_NAME	= "map3-layer.xml";					//$NON-NLS-1$

	private static final String						TAG_ROOT						= "Map3LayerStructure";				//$NON-NLS-1$
	private static final String						TAG_CATEGORY					= "category";							//$NON-NLS-1$
	private static final String						TAG_LAYER						= "layer";								//$NON-NLS-1$

	private static final String						ATTR_NAME						= "name";								//$NON-NLS-1$
	private static final String						ATTR_ID							= "id";								//$NON-NLS-1$
	private static final String						ATTR_IS_DEFAULT_LAYER			= "isDefaultLayer";					//$NON-NLS-1$
	private static final String						ATTR_IS_ENABLED					= "isEnabled";							//$NON-NLS-1$
	private static final String						ATTR_IS_EXPANDED				= "isExpanded";						//$NON-NLS-1$

	/**
	 * _bundle must be set here otherwise an exception occures in saveState()
	 */
	private static final Bundle						_bundle							= Activator
																							.getDefault()
																							.getBundle();
	private static final IPath						_stateLocation					= Platform
																							.getStateLocation(_bundle);
	private static final IDialogSettings			_state							= Activator.getDefault()//
																							.getDialogSettingsSection(
																									"Map3Manager");		//$NON-NLS-1$

	/**
	 * Root item for the layer tree viewer. This contains the UI model.
	 */
	private static TVIMap3Root						_uiRootItem;

	private static final WorldWindowGLCanvas		_wwCanvas						= new WorldWindowGLCanvas();

	/**
	 * Instance of {@link Map3View} or <code>null</code> when view is not created.
	 */
	private static Map3View							_map3View;

	/**
	 * Instance of {@link Map3PropertiesView} or <code>null</code> when view is not created.
	 */
	private static Map3PropertiesView				_map3PropertiesView;

	private static LayerList						_wwDefaultLayers;
	private static HashMap<String, TVIMap3Layer>	_customLayers					= new HashMap<String, TVIMap3Layer>();

	private static Object[]							_uiEnabledLayers;
	private static Object[]							_uiExpandedCategories;
	private static ArrayList<TVIMap3Layer>			_uiEnabledLayersFromXml			= new ArrayList<TVIMap3Layer>();
	private static ArrayList<TVIMap3Category>		_uiExpandedCategoriesFromXml	= new ArrayList<TVIMap3Category>();

	private static final DateTimeFormatter			_dtFormatter					= ISODateTimeFormat
																							.basicDateTimeNoMillis();

	private static final int						INSERT_BEFORE_COMPASS			= 1;

	static {

		_initWorldWindLayerModel();
	}

	/**
	 * Initialize WW model with default layers.
	 */
	private static void _initWorldWindLayerModel() {

		// create default model
		final Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		model.setShowWireframeExterior(false);
		model.setShowWireframeInterior(false);
		model.setShowTessellationBoundingVolumes(false);

		// get default layer
		_wwDefaultLayers = model.getLayers();

		// create custom layer
		createMapStatusLayer();

		// restore layer from xml file
		_uiRootItem = readLayerXml_10();

		// 3.
		updateCustomLayer();

		_wwCanvas.setModel(model);
	}

	/**
	 * These layers are defined as default in WorldWind 1.5
	 * 
	 * <pre>
	 * 
	 * 		Stars							gov.nasa.worldwind.layers.StarsLayer					true
	 * 		Atmosphere						gov.nasa.worldwind.layers.SkyGradientLayer				true
	 * 		NASA Blue Marble Image			gov.nasa.worldwind.layers.Earth.BMNGOneImage			true
	 * 		Blue Marble (WMS) 2004			gov.nasa.worldwind.wms.WMSTiledImageLayer				true
	 * 		i-cubed Landsat					gov.nasa.worldwind.wms.WMSTiledImageLayer				true
	 * 		USDA NAIP						gov.nasa.worldwind.wms.WMSTiledImageLayer				false
	 * 		USDA NAIP USGS					gov.nasa.worldwind.wms.WMSTiledImageLayer				false
	 * 		MS Virtual Earth Aerial			gov.nasa.worldwind.layers.BasicTiledImageLayer			false
	 * 		Bing Imagery					gov.nasa.worldwind.wms.WMSTiledImageLayer				false
	 * 		USGS Topographic Maps 1:250K	gov.nasa.worldwind.wms.WMSTiledImageLayer				false
	 * 		USGS Topographic Maps 1:100K	gov.nasa.worldwind.wms.WMSTiledImageLayer				false
	 * 		USGS Topographic Maps 1:24K		gov.nasa.worldwind.wms.WMSTiledImageLayer				false
	 * 		USGS Urban Area Ortho			gov.nasa.worldwind.layers.BasicTiledImageLayer			false
	 * 		Political Boundaries			gov.nasa.worldwind.layers.Earth.CountryBoundariesLayer	false
	 * 		Open Street 					Map	gov.nasa.worldwind.wms.WMSTiledImageLayer			false
	 * 		Place Names						gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer	true
	 * 		World Map						gov.nasa.worldwind.layers.WorldMapLayer					true
	 * 		Scale bar						gov.nasa.worldwind.layers.ScalebarLayer					true
	 * 		Compass							gov.nasa.worldwind.layers.CompassLayer					true
	 * 
	 * </pre>
	 * 
	 * @return
	 */
	private static XMLMemento createLayerXml_0_DefaultTreeItems() {

		XMLMemento xmlRoot;

		try {

			xmlRoot = createLayerXml_10_WriteRoot();

			IMemento xmlWWCategory = xmlRoot.createChild(TAG_CATEGORY);
			xmlWWCategory.putString(ATTR_NAME, Messages.Default_Category_Name_Map);
			xmlWWCategory.putBoolean(ATTR_IS_EXPANDED, true);

			{
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_STARS, true);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_ATMOSPHERE, true);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_NASA_BLUE_MARBLE_IMAGE, true);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_BLUE_MARBLE_WMS_2004, true);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_I_CUBED_LANDSAT, true);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_USDA_NAIP, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_USDA_NAIP_USGS, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_MS_VIRTUAL_EARTH_AERIAL, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_BING_IMAGERY, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_250K, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_100K, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_USGS_TOPOGRAPHIC_MAPS_1_24K, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_USGS_URBAN_AREA_ORTHO, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_POLITICAL_BOUNDARIES, false);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_OPEN_STREET_MAP, false);
			}

			xmlWWCategory = xmlRoot.createChild(TAG_CATEGORY);
			xmlWWCategory.putString(ATTR_NAME, Messages.Default_Category_Name_Features);
			xmlWWCategory.putBoolean(ATTR_IS_EXPANDED, true);
			{
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_PLACE_NAMES, true);
			}

			xmlWWCategory = xmlRoot.createChild(TAG_CATEGORY);
			xmlWWCategory.putString(ATTR_NAME, Messages.Default_Category_Name_Controls);
			xmlWWCategory.putBoolean(ATTR_IS_EXPANDED, true);
			{
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_WORLD_MAP, true);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_SCALE_BAR, true);
				createLayerXml_20_DefaultLayer(xmlWWCategory, MapDefaultLayer.ID_COMPASS, true);
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

		return xmlRoot;
	}

	private static void createLayerXml_20_DefaultLayer(	final IMemento xmlCategory,
														final String defaultLayerId,
														final boolean isEnabled) {

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
				xmlLayer.putBoolean(ATTR_IS_ENABLED, tviLayer.isEnabled);
				xmlLayer.putBoolean(ATTR_IS_DEFAULT_LAYER, tviLayer.isDefaultLayer);
			}
		}
	}

	private static void createMapStatusLayer() {

		/*
		 * create WW layer
		 */
		final StatusLayer statusLayer = new StatusLayer();
		//this.layer = new StatusMGRSLayer();
		//this.layer = new StatusUTMLayer();

		statusLayer.setEventSource(_wwCanvas);
		statusLayer.setCoordDecimalPlaces(2); // default is 4
		//layer.setElevationUnits(StatusLayer.UNIT_IMPERIAL);

		/*
		 * create UI model layer
		 */
		final String layerId = StatusLayer.class.getCanonicalName();

		final TVIMap3Layer tviLayer = new TVIMap3Layer(statusLayer, statusLayer.getName());

		tviLayer.id = layerId;
		tviLayer.isEnabled = true;
		tviLayer.defaultPosition = INSERT_BEFORE_COMPASS;

		_customLayers.put(layerId, tviLayer);
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

	private static File getLayerXmlFile() {

		final File layerFile = _stateLocation.append(MAP3_LAYER_STRUCTURE_FILE_NAME).toFile();

		return layerFile;
	}

	/**
	 * @return Returns instance of {@link Map3PropertiesView} or null when view is not created.
	 */
	static Map3PropertiesView getMap3PropertiesView() {
		return _map3PropertiesView;
	}

	/**
	 * @return Returns instance of {@link Map3View} or null when view is not created.
	 */
	static Map3View getMap3View() {
		return _map3View;
	}

	static TVIMap3Root getRootItem() {
		return _uiRootItem;
	}

	static Object[] getUIEnabledLayers() {
		return _uiEnabledLayers;
	}

	static Object[] getUIExpandedCategories() {
		return _uiExpandedCategories;
	}

	static WorldWindowGLCanvas getWWCanvas() {
		return _wwCanvas;
	}

	/**
	 * Insert the layer into the layer list just after the placenames.
	 * 
	 * @param wwd
	 * @param layer
	 */
	public static void insertAfterPlacenames(final WorldWindow wwd, final Layer layer) {

		int compassPosition = 0;
		final LayerList layers = wwd.getModel().getLayers();
		for (final Layer l : layers) {
			if (l instanceof PlaceNameLayer) {
				compassPosition = layers.indexOf(l);
			}
		}
		layers.add(compassPosition + 1, layer);
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
		final LayerList layers = wwd.getModel().getLayers();
		for (final Layer l : layers) {
			if (l instanceof CompassLayer) {
				compassPosition = layers.indexOf(l);
			}
		}

		final Layer newWWLayer = newUILayer.wwLayer;

		layers.add(compassPosition, newWWLayer);

		/*
		 * update UI model
		 */
		final TVIMap3Layer insertedUILayer = insertBeforeCompassInUIModel(_uiRootItem, newWWLayer);

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
	 * @return
	 */
	private static TVIMap3Layer insertBeforeCompassInUIModel(final TVIMap3Item tviParent, final Layer newWWLayer) {

//		for (final TreeViewerItem tviItem : tviParent.getFetchedChildren()) {
//			if (tviItem instanceof TVIMap3Layer) {
//				final TVIMap3Layer tviLayer = (TVIMap3Layer) tviItem;
//				if (tviLayer.wwLayer instanceof CompassLayer) {
//
//					// compass layer found in ui model
//
//					tviParent.addChildBefore(tviLayer, newTVILayer);
//
//					return newTVILayer;
//				}
//
//			} else if (tviItem instanceof TVIMap3Category) {
//
//				final TVIMap3Layer insertedLayer = insertBeforeCompassInUIModel((TVIMap3Category) tviItem, newWWLayer);
//
//				if (insertedLayer != null) {
//					// new layer is inserted
//					return insertedLayer;
//				}
//			}
//		}

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

		int targetPosition = 0;
		final LayerList layers = wwd.getModel().getLayers();
		for (final Layer l : layers) {
			if (l.getName().indexOf(targetName) != -1) {
				targetPosition = layers.indexOf(l);
				break;
			}
		}
		layers.add(targetPosition, layer);
	}

	/**
	 * Insert the layer into the layer list just before the placenames.
	 * 
	 * @param wwd
	 * @param layer
	 */
	public static void insertBeforePlacenames(final WorldWindow wwd, final Layer layer) {

		int compassPosition = 0;
		final LayerList layers = wwd.getModel().getLayers();
		for (final Layer l : layers) {
			if (l instanceof PlaceNameLayer) {
				compassPosition = layers.indexOf(l);
			}
		}
		layers.add(compassPosition, layer);
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
	 * Read map provider list from a xml file
	 * 
	 * @return Returns a list with all map providers from a xml file including wrapped plugin map
	 *         provider
	 */
	private static TVIMap3Root readLayerXml_10() {

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

			if (xmlRoot == null) {
				// get default layer tree
				xmlRoot = createLayerXml_0_DefaultTreeItems();
			}

			readLayerXml_20_Children(xmlRoot, tviRoot);

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
	private static void readLayerXml_20_Children(final XMLMemento xmlParent, final TVIMap3Item tviParent) {

		for (final IMemento mementoChild : xmlParent.getChildren()) {

			if (mementoChild instanceof XMLMemento) {

				final XMLMemento xmlChild = (XMLMemento) mementoChild;

				try {

					final String xmlName = xmlChild.getString(ATTR_NAME);
					final String xmlType = xmlChild.getType();

					if (xmlType.equals(TAG_LAYER)) {

						final boolean isDefaultLayer = xmlChild.getBoolean(ATTR_IS_DEFAULT_LAYER) == null
								? false
								: true;

						final String xmlLayerId = xmlChild.getString(ATTR_ID);
						final boolean isEnabled = xmlChild.getBoolean(ATTR_IS_ENABLED);

						TVIMap3Layer tviLayer;

						if (isDefaultLayer) {

							// default layer

							final Layer wwLayer = _wwDefaultLayers.getLayerByName(xmlLayerId);

							// check if xml layer is a default ww layer
							if (wwLayer == null) {
								StatusUtil.log(NLS.bind("NTMVMM001 layer \"{0}\" is not a ww default layer.", xmlName));//$NON-NLS-1$
								continue;
							}

							final DefaultLayer mapDefaultLayer = MapDefaultLayer.getLayer(xmlLayerId);
							String layerName;

							// use untranslated name, this should not occure
							if (mapDefaultLayer == null) {
								StatusUtil.log(NLS.bind(
										"NTMVMM002 layer \"{0}\" is not defined as map default layer.", xmlName));//$NON-NLS-1$
								layerName = xmlName;
							} else {
								layerName = mapDefaultLayer.layerName;
							}

							tviLayer = new TVIMap3Layer(wwLayer, layerName);

							tviLayer.id = xmlLayerId;
							tviLayer.isDefaultLayer = true;

							tviParent.addChild(tviLayer);

						} else {

							// NO default layer

							tviLayer = _customLayers.get(xmlLayerId);

							if (tviLayer == null) {
								StatusUtil.log(NLS.bind("NTMVMM003 layer \"{0}\" is not available.", xmlName));//$NON-NLS-1$
								continue;
							}

							_uiEnabledLayersFromXml.add(tviLayer);
						}

						/*
						 * set layer/UI enable state
						 */
						tviLayer.isEnabled = isEnabled;
						tviLayer.wwLayer.setEnabled(isEnabled);
						if (isEnabled) {
							_uiEnabledLayersFromXml.add(tviLayer);
						}

					} else if (xmlType.equals(TAG_CATEGORY)) {

						final TVIMap3Category tviCategory = new TVIMap3Category(xmlName);

						tviParent.addChild(tviCategory);

						// set expanded state
						final Boolean isExpanded = xmlChild.getBoolean(ATTR_IS_EXPANDED);
						if (isExpanded != null && isExpanded) {
							_uiExpandedCategoriesFromXml.add(tviCategory);
						}

						readLayerXml_20_Children(xmlChild, tviCategory);
					}
				} catch (final Exception e) {
					StatusUtil.log(Util.dumpMemento(xmlChild), e);
				}
			}
		}
	}

	public static void saveState() {

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
	 * @param map3PropertiesView
	 */
	static void setMap3PropertiesView(final Map3PropertiesView map3PropertiesView) {
		_map3PropertiesView = map3PropertiesView;
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

	static void setMap3View(final Map3View map3View) {
		_map3View = map3View;
	}

	private static void updateCustomLayer() {

		for (final TVIMap3Layer tviLayer : _customLayers.values()) {

			// update ui/ww model
			final TVIMap3Layer insertedUILayer = insertBeforeCompass(_wwCanvas, tviLayer);

			if (_map3PropertiesView != null) {
				_map3PropertiesView.updateUINewLayer(insertedUILayer);
			}
		}
	}

}
