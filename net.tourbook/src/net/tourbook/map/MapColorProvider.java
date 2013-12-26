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
import net.tourbook.common.color.MapGraphId;
import net.tourbook.map2.view.HrZonesColorProvider;

/**
 * Contains the color provider which are set and selected (3D) in the preferences and which are used
 * to paint a tour/legend in a 2D/3D map.
 */
public class MapColorProvider {

	/**
	 * Key is the color id.
	 */
	private static HashMap<MapGraphId, IMapColorProvider>	_map2ColorProvider;

	/**
	 * Key is the color id.
	 */
	private static HashMap<MapGraphId, IMapColorProvider>	_map3ColorProvider;

	private static void checkColorProvider() {

		if (_map2ColorProvider == null) {

			_map2ColorProvider = new HashMap<MapGraphId, IMapColorProvider>();
			_map3ColorProvider = new HashMap<MapGraphId, IMapColorProvider>();

			createColorProviders();
		}
	}

	/**
	 * Create color provider for all graphs which can be displayed.
	 */
	private static void createColorProviders() {

		/*
		 * 2D Map
		 */
		_map2ColorProvider.put(MapGraphId.Altitude, new Map2GradientColorProvider(MapGraphId.Altitude));
		_map2ColorProvider.put(MapGraphId.Gradient, new Map2GradientColorProvider(MapGraphId.Gradient));
		_map2ColorProvider.put(MapGraphId.Pace, new Map2GradientColorProvider(MapGraphId.Pace));
		_map2ColorProvider.put(MapGraphId.Pulse, new Map2GradientColorProvider(MapGraphId.Pulse));
		_map2ColorProvider.put(MapGraphId.Speed, new Map2GradientColorProvider(MapGraphId.Speed));

		_map2ColorProvider.put(MapGraphId.HrZone, new HrZonesColorProvider(MapGraphId.HrZone));

		/*
		 * 3D Map
		 */
		_map3ColorProvider.put(MapGraphId.Altitude, new Map3GradientColorProvider(MapGraphId.Altitude));
		_map3ColorProvider.put(MapGraphId.Pace, new Map3GradientColorProvider(MapGraphId.Pace));
		_map3ColorProvider.put(MapGraphId.Gradient, new Map3GradientColorProvider(MapGraphId.Gradient));
		_map3ColorProvider.put(MapGraphId.Pulse, new Map3GradientColorProvider(MapGraphId.Pulse));
		_map3ColorProvider.put(MapGraphId.Speed, new Map3GradientColorProvider(MapGraphId.Speed));

		_map3ColorProvider.put(MapGraphId.HrZone, new HrZonesColorProvider(MapGraphId.HrZone));
	}

	public static IMapColorProvider getMap2ColorProvider(final MapGraphId colorId) {

		checkColorProvider();

		IMapColorProvider mapColorProvider = _map2ColorProvider.get(colorId);

		// use default when not available
		if (mapColorProvider == null) {
			mapColorProvider = _map2ColorProvider.get(MapGraphId.Altitude);
		}

		return mapColorProvider;
	}

	public static IMapColorProvider getMap3ColorProvider(final MapGraphId colorId) {

		checkColorProvider();

		IMapColorProvider mapColorProvider = _map3ColorProvider.get(colorId);

		// use default when not available
		if (mapColorProvider == null) {
			mapColorProvider = _map3ColorProvider.get(MapGraphId.Altitude);
		}

		return mapColorProvider;
	}
}
