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
package net.tourbook.mapping;

import java.util.HashMap;

import net.tourbook.common.color.ILegendProvider;
import net.tourbook.common.color.LegendColor;
import net.tourbook.common.color.LegendConfig;

public class TourMapColors {

	/*
	 * color ids
	 */
	public static final int								TOUR_COLOR_DEFAULT	= 0;
	public static final int								TOUR_COLOR_ALTITUDE	= 10;
	public static final int								TOUR_COLOR_GRADIENT	= 20;
	public static final int								TOUR_COLOR_PULSE	= 30;
	public static final int								TOUR_COLOR_SPEED	= 40;
	public static final int								TOUR_COLOR_PACE		= 50;
	public static final int								TOUR_COLOR_HR_ZONE	= 60;

	/**
	 * Key is color id.
	 */
	private static HashMap<Integer, ILegendProvider>	_legendProviders;

	/**
	 * Create legend provider for all graphs which can be displayed.
	 */
	private static void createLegendProviders() {

		_legendProviders.put(//
				TOUR_COLOR_PULSE, //
				new LegendProviderGradientColors(new LegendConfig(), new LegendColor(), TOUR_COLOR_PULSE));

		_legendProviders.put(//
				TOUR_COLOR_ALTITUDE, //
				new LegendProviderGradientColors(new LegendConfig(), new LegendColor(), TOUR_COLOR_ALTITUDE));

		_legendProviders.put(//
				TOUR_COLOR_SPEED, //
				new LegendProviderGradientColors(new LegendConfig(), new LegendColor(), TOUR_COLOR_SPEED));

		_legendProviders.put(//
				TOUR_COLOR_PACE, //
				new LegendProviderGradientColors(new LegendConfig(), new LegendColor(), TOUR_COLOR_PACE));

		_legendProviders.put(//
				TOUR_COLOR_GRADIENT, //
				new LegendProviderGradientColors(new LegendConfig(), new LegendColor(), TOUR_COLOR_GRADIENT));

		_legendProviders.put(//
				TOUR_COLOR_HR_ZONE, //
				new LegendProviderHrZones(TOUR_COLOR_HR_ZONE));
	}

	public static ILegendProvider getColorProvider(final int colorId) {

		if (_legendProviders == null) {

			_legendProviders = new HashMap<Integer, ILegendProvider>();

			createLegendProviders();
		}

		return _legendProviders.get(colorId);
	}
}
