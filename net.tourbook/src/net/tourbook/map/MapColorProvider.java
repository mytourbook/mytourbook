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
package net.tourbook.map;

import java.util.HashMap;

import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.Map2GradientColorProvider;
import net.tourbook.common.color.Map3GradientColorProvider;
import net.tourbook.common.color.MapColorId;
import net.tourbook.map2.view.HrZonesColorProvider;

/**
 * Contains the color provider which are set and selected (3D) in the preferences and which are used
 * to paint a tour/legend in a 2D/3D map.
 */
public class MapColorProvider {

	/**
	 * Key is the color id.
	 */
	private static HashMap<MapColorId, IMapColorProvider>	_map2ColorProvider;

	/**
	 * Key is the color id.
	 */
	private static HashMap<MapColorId, IMapColorProvider>	_map3ColorProvider;

	private static void checkColorProvider() {

		if (_map2ColorProvider == null) {

			_map2ColorProvider = new HashMap<MapColorId, IMapColorProvider>();
			_map3ColorProvider = new HashMap<MapColorId, IMapColorProvider>();

			createColorProviders();
		}
	}

	/**
	 * Create legend provider for all graphs which can be displayed.
	 */
	private static void createColorProviders() {

		/*
		 * 2D Map
		 */
		_map2ColorProvider.put(MapColorId.Altitude, new Map2GradientColorProvider(MapColorId.Altitude));
		_map2ColorProvider.put(MapColorId.Gradient, new Map2GradientColorProvider(MapColorId.Gradient));
		_map2ColorProvider.put(MapColorId.Pace, new Map2GradientColorProvider(MapColorId.Pace));
		_map2ColorProvider.put(MapColorId.Pulse, new Map2GradientColorProvider(MapColorId.Pulse));
		_map2ColorProvider.put(MapColorId.Speed, new Map2GradientColorProvider(MapColorId.Speed));

		_map2ColorProvider.put(MapColorId.HrZone, new HrZonesColorProvider(MapColorId.HrZone));

		/*
		 * 3D Map
		 */
		_map3ColorProvider.put(MapColorId.Altitude, new Map3GradientColorProvider(MapColorId.Altitude));
		_map3ColorProvider.put(MapColorId.Pace, new Map3GradientColorProvider(MapColorId.Pace));
		_map3ColorProvider.put(MapColorId.Gradient, new Map3GradientColorProvider(MapColorId.Gradient));
		_map3ColorProvider.put(MapColorId.Pulse, new Map3GradientColorProvider(MapColorId.Pulse));
		_map3ColorProvider.put(MapColorId.Speed, new Map3GradientColorProvider(MapColorId.Speed));

		_map3ColorProvider.put(MapColorId.HrZone, new HrZonesColorProvider(MapColorId.HrZone));
	}

	public static IMapColorProvider getMap2ColorProvider(final MapColorId colorId) {

		checkColorProvider();

		IMapColorProvider mapColorProvider = _map2ColorProvider.get(colorId);

		// use default when not available
		if (mapColorProvider == null) {
			mapColorProvider = _map2ColorProvider.get(MapColorId.Altitude);
		}

		return mapColorProvider;
	}

	public static IMapColorProvider getMap3ColorProvider(final MapColorId colorId) {

		checkColorProvider();

		IMapColorProvider mapColorProvider = _map3ColorProvider.get(colorId);

		// use default when not available
		if (mapColorProvider == null) {
			mapColorProvider = _map3ColorProvider.get(MapColorId.Altitude);
		}

		return mapColorProvider;
	}
}
