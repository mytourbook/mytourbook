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

import java.util.ArrayList;

import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;

public class TVIMap3Category extends TVIMap3Item {

	private ArrayList<TVIMap3Layer>	_checkStateNotSet	= new ArrayList<TVIMap3Layer>();

	public TVIMap3Category(final String name) {

		this.name = name;
	}

//	private void addCustomLayer() {
//
//		final Map3Layer map3Layer = Map3Manager.getMap3Layer(StatusLayer.ID);
//
//	}
//
//	private void addWWDefaultLayer() {
//
//		final LayerList allLayers = Map3Manager.getWWCanvas().getModel().getLayers();
//
////		Map3Manager.dumpLayer(layers);
//
////		Stars							gov.nasa.worldwind.layers.StarsLayer					true
////		Atmosphere						gov.nasa.worldwind.layers.SkyGradientLayer				true
////		NASA Blue Marble Image			gov.nasa.worldwind.layers.Earth.BMNGOneImage			true
////		Blue Marble (WMS) 2004			gov.nasa.worldwind.wms.WMSTiledImageLayer				true
////		i-cubed Landsat					gov.nasa.worldwind.wms.WMSTiledImageLayer				true
////		USDA NAIP						gov.nasa.worldwind.wms.WMSTiledImageLayer				false
////		USDA NAIP USGS					gov.nasa.worldwind.wms.WMSTiledImageLayer				false
////		MS Virtual Earth Aerial			gov.nasa.worldwind.layers.BasicTiledImageLayer			false
////		Bing Imagery					gov.nasa.worldwind.wms.WMSTiledImageLayer				false
////		USGS Topographic Maps 1:250K	gov.nasa.worldwind.wms.WMSTiledImageLayer				false
////		USGS Topographic Maps 1:100K	gov.nasa.worldwind.wms.WMSTiledImageLayer				false
////		USGS Topographic Maps 1:24K		gov.nasa.worldwind.wms.WMSTiledImageLayer				false
////		USGS Urban Area Ortho			gov.nasa.worldwind.layers.BasicTiledImageLayer			false
////		Political Boundaries			gov.nasa.worldwind.layers.Earth.CountryBoundariesLayer	false
////		Open Street 					Map	gov.nasa.worldwind.wms.WMSTiledImageLayer			false
////		Place Names						gov.nasa.worldwind.layers.Earth.NASAWFSPlaceNameLayer	true
////		World Map						gov.nasa.worldwind.layers.WorldMapLayer					true
////		Scale bar						gov.nasa.worldwind.layers.ScalebarLayer					true
////		Compass							gov.nasa.worldwind.layers.CompassLayer					true
//
//		for (final Layer layer : allLayers) {
//
//			final String defaultLayerKey = layer.getName();
//
//			final Map3Layer map3Layer = Map3Manager.getMap3Layer(defaultLayerKey);
//
//			if (map3Layer == null) {
//				// this should not happen for the default layers
//				continue;
//			}
//
//			// create tree item
//			final TVIMap3Layer map3Item = new TVIMap3Layer(map3Layer);
//
//			map3Item.isEnabled = map3Layer.isEnabled;
//
//			addChild(map3Item);
//
//			// check state must be set later, it do not work when it's done now
//			_checkStateNotSet.add(map3Item);
//		}
//
//	}

	@Override
	protected void fetchChildren() {}

	void setCheckState() {

		if (_checkStateNotSet.size() == 0) {
			// nothing to do
			return;
		}

		ContainerCheckedTreeViewer propViewer = null;

		final Map3PropertiesView propView = Map3Manager.getMap3PropertiesView();
		if (propView != null) {

			propViewer = propView.getPropertiesViewer();

			// set check state in the viewer
			for (final TVIMap3Layer layerItem : _checkStateNotSet) {
				propViewer.setChecked(layerItem, layerItem.isEnabled);
			}

			// reset
			_checkStateNotSet.clear();
		}
	}

}
