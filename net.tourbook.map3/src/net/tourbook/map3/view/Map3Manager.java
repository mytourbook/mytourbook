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
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map3.Activator;

import org.eclipse.jface.dialogs.IDialogSettings;

public class Map3Manager {

	private static final String					REG_EX_NONE_WORD_CHARACTERS	= "\\W";

	private static final String					STATE_LAYER_IS_ENABLED_		= "STATE_LAYER_IS_ENABLED_";			//$NON-NLS-1$

	static final int							WW_DEFAULT_LAYER_ID			= 1;

	private static final IDialogSettings		_state						= Activator
																					.getDefault()
																					.getDialogSettingsSection(
																							"PhotoDirectoryView");	//$NON-NLS-1$

	private static ArrayList<TVIMap3Category>	_rootCategories;

	private static final WorldWindowGLCanvas	_wwCanvas					= new WorldWindowGLCanvas();

	/**
	 * Instance of {@link Map3View} or <code>null</code> when view is not created.
	 */
	private static Map3View						_map3View;

	/**
	 * Instance of {@link Map3PropertiesView} or <code>null</code> when view is not created.
	 */
	private static Map3PropertiesView			_map3PropertiesView;

	private static HashMap<String, Map3Layer>	_map3Layer					= new HashMap<String, Map3Layer>();

	static {
		initWorldWindLayerModel();
	}

	static void dumpLayer(final LayerList layers) {

		for (final Layer layer : layers) {

			final String name = layer.getName();

			final double minActiveAltitude = layer.getMinActiveAltitude();
			final double maxActiveAltitude = layer.getMaxActiveAltitude();

			System.out.println();
			System.out.println(" layer: " + layer.getClass().getName() + "\t" + name);
			System.out.println(" \tMinMax altitude:\t"
					+ UI.FormatDoubleMinMax(minActiveAltitude)
					+ ("\t" + UI.FormatDoubleMinMax(maxActiveAltitude)));
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
	 * Create a layer key from the layer name by removing none-word characters.
	 * 
	 * @param layerName
	 * @return
	 */
	private static String getLayerEnabledKey(final String layerName) {
		return STATE_LAYER_IS_ENABLED_ + getLayerKey(layerName);
	}

	/**
	 * Create a layer key from the layer name by removing none-word characters.
	 * 
	 * @param layerName
	 * @return
	 */
	private static String getLayerKey(final String layerName) {
		return layerName.replaceAll(REG_EX_NONE_WORD_CHARACTERS, UI.EMPTY_STRING);
	}

	/**
	 * @param layer
	 * @return Returns a layer or <code>null</code> when layer is not defined.
	 */
	static Map3Layer getMap3Layer(final Layer layer) {

		final String layerKey = getLayerKey(layer.getName());

		return _map3Layer.get(layerKey);
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

	static ArrayList<TVIMap3Category> getRootCategories(final TVIMap3Root rootItem) {

		if (_rootCategories != null) {
			return _rootCategories;
		}

		final ArrayList<TVIMap3Category> _rootCategories = new ArrayList<TVIMap3Category>();

		_rootCategories.add(new TVIMap3Category(rootItem, WW_DEFAULT_LAYER_ID, "World Wind Layer"));

		return _rootCategories;
	}

	static WorldWindowGLCanvas getWWCanvas() {
		return _wwCanvas;
	}

	/*
	 * Initialize WW model with default layers
	 */
	static void initWorldWindLayerModel() {

		// create default model
		final Model model = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);

		// restore map3 state
		restoreState(model);

		model.setShowWireframeExterior(false);
		model.setShowWireframeInterior(false);
		model.setShowTessellationBoundingVolumes(false);

		_wwCanvas.setModel(model);
	}

	private static void restoreState(final Model model) {

		/*
		 * create map3layer for all ww default layers with the visibility from the state
		 */
		for (final Layer layer : model.getLayers()) {

			final boolean defaultIsEnabled = layer.isEnabled();
			final String layerName = layer.getName();

			final boolean isEnabled = Util.getStateBoolean(_state, getLayerEnabledKey(layerName), defaultIsEnabled);

			final Map3Layer map3Layer = new Map3Layer(layerName);

			// update model
			map3Layer.isEnabled = isEnabled;

			// update UI
			layer.setEnabled(isEnabled);

			_map3Layer.put(getLayerKey(layerName), map3Layer);
		}

//		Stars							gov.nasa.worldwind.layers.StarsLayer					true
//		Atmosphere						gov.nasa.worldwind.layers.SkyGradientLayer				true
//		NASA Blue Marble Image			gov.nasa.worldwind.layers.Earth.BMNGOneImage			true
//		Blue Marble (WMS) 2004			gov.nasa.worldwind.wms.WMSTiledImageLayer				true
//		i-cubed Landsat					gov.nasa.worldwind.wms.WMSTiledImageLayer				true
//		USDA NAIP						gov.nasa.worldwind.wms.WMSTiledImageLayer				false
//		USDA NAIP USGS					gov.nasa.worldwind.wms.WMSTiledImageLayer				false
//		MS Virtual Earth Aerial			gov.nasa.worldwind.layers.BasicTiledImageLayer			false
//		Bing Imagery					gov.nasa.worldwind.wms.WMSTiledImageLayer				false
//		USGS Topographic Maps 1:250K	gov.nasa.worldwind.wms.WMSTiledImageLayer				false
//		USGS Topographic Maps 1:100K	gov.nasa.worldwind.wms.WMSTiledImageLayer				false
//		USGS Topographic Maps 1:24K		gov.nasa.worldwind.wms.WMSTiledImageLayer				false
//		USGS Urban Area Ortho			gov.nasa.worldwind.layers.BasicTiledImageLayer			false
//		Political Boundaries			gov.nasa.worldwind.layers.Earth.CountryBoundariesLayer	false
//		Open Street 					Map	gov.nasa.worldwind.wms.WMSTiledImageLayer			false
//		Place Names						gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer	true
//		World Map						gov.nasa.worldwind.layers.WorldMapLayer					true
//		Scale bar						gov.nasa.worldwind.layers.ScalebarLayer					true
//		Compass							gov.nasa.worldwind.layers.CompassLayer					true

	}

	public static void saveState() {

		final Model model = _wwCanvas.getModel();
		if (model == null) {
			return;
		}

		// save layer visibility
		for (final Layer layer : model.getLayers()) {
			_state.put(getLayerEnabledKey(layer.getName()), layer.isEnabled());
		}
	}

	static void setMap3PropertiesView(final Map3PropertiesView map3PropertiesView) {
		_map3PropertiesView = map3PropertiesView;
	}

	static void setMap3View(final Map3View map3View) {
		_map3View = map3View;
	}

}
