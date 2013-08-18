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
package net.tourbook.map3.layer;

import java.util.HashMap;

import net.tourbook.common.color.GradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapColor;
import net.tourbook.common.color.MapLegendImageConfig;
import net.tourbook.common.color.MapColorId;
import net.tourbook.map2.view.HrZonesColorProvider;

public class Map3Colors {

	/**
	 * Contains color provider which are displayed in the map, key is the color id.
	 */
	private static HashMap<MapColorId, IMapColorProvider>	_colorProviders;

	/**
	 * Create legend provider for all graphs which can be displayed.
	 */
	private static void createColorProviders() {

		_colorProviders.put(//
				MapColorId.Altitude,
				new GradientColorProvider(MapColorId.Altitude, new MapLegendImageConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Gradient,
				new GradientColorProvider(MapColorId.Gradient, new MapLegendImageConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Pace,
				new GradientColorProvider(MapColorId.Pace, new MapLegendImageConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Pulse,
				new GradientColorProvider(MapColorId.Pulse, new MapLegendImageConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Speed,
				new GradientColorProvider(MapColorId.Speed, new MapLegendImageConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.HrZone,
				new HrZonesColorProvider(MapColorId.HrZone));
	}

	public static IMapColorProvider getColorProvider(final MapColorId colorId) {

		if (_colorProviders == null) {

			_colorProviders = new HashMap<MapColorId, IMapColorProvider>();

			createColorProviders();
		}

		return _colorProviders.get(colorId);
	}
}
