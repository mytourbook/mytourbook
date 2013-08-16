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
package net.tourbook.map2.view;

import java.util.HashMap;

import net.tourbook.common.color.GradientColorProvider;
import net.tourbook.common.color.IMapColorProvider;
import net.tourbook.common.color.MapColor;
import net.tourbook.common.color.MapColorConfig;
import net.tourbook.common.color.MapColorId;

public class TourMapColors {

	/**
	 * Key is the color id.
	 */
	private static HashMap<MapColorId, IMapColorProvider>	_colorProviders;

	/**
	 * Create legend provider for all graphs which can be displayed.
	 */
	private static void createColorProviders() {

		_colorProviders.put(//
				MapColorId.Altitude,
				new GradientColorProvider(MapColorId.Altitude, new MapColorConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Gradient,
				new GradientColorProvider(MapColorId.Gradient, new MapColorConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Pace,
				new GradientColorProvider(MapColorId.Pace, new MapColorConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Pulse,
				new GradientColorProvider(MapColorId.Pulse, new MapColorConfig(), new MapColor()));

		_colorProviders.put(//
				MapColorId.Speed,
				new GradientColorProvider(MapColorId.Speed, new MapColorConfig(), new MapColor()));

		//

		_colorProviders.put(//
				MapColorId.HrZone,
				new HrZonesColorProvider(MapColorId.HrZone));
	}

	public static IMapColorProvider getColorProvider(final MapColorId colorId) {

		if (_colorProviders == null) {

			_colorProviders = new HashMap<MapColorId, IMapColorProvider>();

			createColorProviders();
		}

		IMapColorProvider legendProvider = _colorProviders.get(colorId);

		// use default when not available
		if (legendProvider == null) {
			legendProvider = _colorProviders.get(MapColorId.Altitude);
		}

		return legendProvider;
	}
}
