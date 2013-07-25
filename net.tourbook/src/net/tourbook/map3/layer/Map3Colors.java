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
import net.tourbook.common.color.ILegendProvider;
import net.tourbook.common.color.LegendColor;
import net.tourbook.common.color.LegendConfig;
import net.tourbook.map2.view.HrZonesColorProvider;

public class Map3Colors {


	/**
	 * Key is the color id.
	 */
	private static HashMap<Integer, ILegendProvider>	_colorProviders;

	/**
	 * Create legend provider for all graphs which can be displayed.
	 */
	private static void createColorProviders() {

		_colorProviders.put(//
				ILegendProvider.TOUR_COLOR_PULSE,
				new GradientColorProvider(ILegendProvider.TOUR_COLOR_PULSE, new LegendConfig(), new LegendColor()));

		_colorProviders.put(//
				ILegendProvider.TOUR_COLOR_ALTITUDE,
				new GradientColorProvider(ILegendProvider.TOUR_COLOR_ALTITUDE, new LegendConfig(), new LegendColor()));

		_colorProviders.put(//
				ILegendProvider.TOUR_COLOR_SPEED,
				new GradientColorProvider(ILegendProvider.TOUR_COLOR_SPEED, new LegendConfig(), new LegendColor()));

		_colorProviders.put(//
				ILegendProvider.TOUR_COLOR_PACE,
				new GradientColorProvider(ILegendProvider.TOUR_COLOR_PACE, new LegendConfig(), new LegendColor()));

		_colorProviders.put(//
				ILegendProvider.TOUR_COLOR_GRADIENT,
				new GradientColorProvider(ILegendProvider.TOUR_COLOR_GRADIENT, new LegendConfig(), new LegendColor()));

		_colorProviders.put(//
				ILegendProvider.TOUR_COLOR_HR_ZONE,
				new HrZonesColorProvider(ILegendProvider.TOUR_COLOR_HR_ZONE));
	}

	public static ILegendProvider getColorProvider(final int colorId) {

		if (_colorProviders == null) {

			_colorProviders = new HashMap<Integer, ILegendProvider>();

			createColorProviders();
		}

		return _colorProviders.get(colorId);
	}
}
