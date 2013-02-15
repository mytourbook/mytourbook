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

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

public class TVICategory extends TVIMapItem {

	protected int	id;

	public TVICategory(final TVIRoot rootItem, final int id, final String name) {

		super(rootItem);

		this.id = id;
		this.name = name;
	}

	@Override
	protected void fetchChildren() {

		final LayerList layers = Map3Manager.getWWCanvas().getModel().getLayers();

//		// dump layer
//		System.out.println(UI.timeStampNano() + " Layer");
//		System.out.println(UI.timeStampNano() + " \t");
//		// TODO remove SYSTEM.OUT.PRINTLN
//		for (final Layer layer : layers) {
//			System.out.println(layer.getName() + "\t" + layer.getClass().getCanonicalName() + "\t" + layer.isEnabled());
//			// TODO remove SYSTEM.OUT.PRINTLN
//		}

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

		if (id == Map3Manager.LAYER_ID) {

			for (final Layer layer : layers) {
				addChild(new TVILayer(rootItem, layer));
			}

		} else if (id == Map3Manager.CONTROLS_ID) {

		}
	}

}
