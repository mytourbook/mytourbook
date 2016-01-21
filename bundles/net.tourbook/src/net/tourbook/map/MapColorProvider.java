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

import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.Map2ColorProfile;
import net.tourbook.common.color.Map2GradientColorProvider;
import net.tourbook.common.color.Map3GradientColorManager;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.map2.view.HrZonesColorProvider;

/**
 * Color providers are used to paint a tour/legend in a 2D/3D map.
 * <p>
 * It contains all active color providers, the selected in the pref dialog is the active color
 * provider.
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

	static {

		/*
		 * 2D Map
		 */
		_map2ColorProvider = new HashMap<MapGraphId, IMapColorProvider>();

		_map2ColorProvider.put(MapGraphId.Altitude, new Map2GradientColorProvider(MapGraphId.Altitude));
		_map2ColorProvider.put(MapGraphId.Gradient, new Map2GradientColorProvider(MapGraphId.Gradient));
		_map2ColorProvider.put(MapGraphId.Pace, new Map2GradientColorProvider(MapGraphId.Pace));
		_map2ColorProvider.put(MapGraphId.Pulse, new Map2GradientColorProvider(MapGraphId.Pulse));
		_map2ColorProvider.put(MapGraphId.Speed, new Map2GradientColorProvider(MapGraphId.Speed));

		_map2ColorProvider.put(MapGraphId.HrZone, new HrZonesColorProvider(MapGraphId.HrZone));

		/*
		 * 3D Map, gradient color providers are managed by the Map3ColorManager.
		 */
		_map3ColorProvider = new HashMap<MapGraphId, IMapColorProvider>();

		_map3ColorProvider.put(MapGraphId.HrZone, new HrZonesColorProvider(MapGraphId.HrZone));
	}

	private MapColorProvider() {}

	public static IMapColorProvider getActiveMap2ColorProvider(final MapGraphId colorId) {

		IMapColorProvider mapColorProvider = _map2ColorProvider.get(colorId);

		// use default when not available
		if (mapColorProvider == null) {
			mapColorProvider = _map2ColorProvider.get(MapGraphId.Altitude);
		}

		return mapColorProvider;
	}

	/**
	 * @param graphId
	 * @return Returns the active color provider which is actived in the pref store.
	 */
	public static IMapColorProvider getActiveMap3ColorProvider(final MapGraphId graphId) {

		if (graphId == MapGraphId.HrZone) {

			return _map3ColorProvider.get(graphId);

		} else {

			return Map3GradientColorManager.getActiveMap3ColorProvider(graphId);
		}
	}

	/**
	 * Set active color profile (net.tourbook.common) into the active color provider
	 * (net.tourbook.map).
	 */
	public static void updateMap2Colors() {

		for (final IMapColorProvider colorProvider : _map2ColorProvider.values()) {

			if (colorProvider instanceof Map2GradientColorProvider) {

				final Map2GradientColorProvider map2Provider = (Map2GradientColorProvider) colorProvider;

				final MapGraphId graphId = map2Provider.getGraphId();

				final ColorDefinition colorDefinition = GraphColorManager.getInstance().getColorDefinition(graphId);
				final Map2ColorProfile map2Profile = colorDefinition.getMap2Color_New();

				map2Provider.setColorProfile(map2Profile);
			}
		}

	}
}
