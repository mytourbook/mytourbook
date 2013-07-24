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
	private static HashMap<Integer, ILegendProvider>	_legendProviders;

	/**
	 * Create legend provider for all graphs which can be displayed.
	 */
	private static void createLegendProviders() {

		_legendProviders.put(//
				ILegendProvider.TOUR_COLOR_PULSE,
				new GradientColorProvider(new LegendConfig(), new LegendColor(), ILegendProvider.TOUR_COLOR_PULSE));

		_legendProviders.put(//
				ILegendProvider.TOUR_COLOR_ALTITUDE,
				new GradientColorProvider(new LegendConfig(), new LegendColor(), ILegendProvider.TOUR_COLOR_ALTITUDE));

		_legendProviders.put(//
				ILegendProvider.TOUR_COLOR_SPEED,
				new GradientColorProvider(new LegendConfig(), new LegendColor(), ILegendProvider.TOUR_COLOR_SPEED));

		_legendProviders.put(//
				ILegendProvider.TOUR_COLOR_PACE,
				new GradientColorProvider(new LegendConfig(), new LegendColor(), ILegendProvider.TOUR_COLOR_PACE));

		_legendProviders.put(//
				ILegendProvider.TOUR_COLOR_GRADIENT,
				new GradientColorProvider(new LegendConfig(), new LegendColor(), ILegendProvider.TOUR_COLOR_GRADIENT));

		_legendProviders.put(//
				ILegendProvider.TOUR_COLOR_HR_ZONE,
				new HrZonesColorProvider(ILegendProvider.TOUR_COLOR_HR_ZONE));
	}

	public static ILegendProvider getColorProvider(final int colorId) {

		if (_legendProviders == null) {

			_legendProviders = new HashMap<Integer, ILegendProvider>();

			createLegendProviders();
		}

		return _legendProviders.get(colorId);
	}
}
