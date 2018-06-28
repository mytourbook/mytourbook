/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import net.tourbook.chart.ISliderLabelProvider;
import net.tourbook.common.UI;

public class SliderLabelProvider_Gear implements ISliderLabelProvider {

	private float[][] _gearSerie;

	public SliderLabelProvider_Gear(final float[][] gearSerie) {
		_gearSerie = gearSerie;
	}

	@Override
	public String getLabel(final int sliderValueIndex) {

		final float value0 = _gearSerie[0][sliderValueIndex];
		final float value1 = _gearSerie[1][sliderValueIndex];
		final float value2 = _gearSerie[2][sliderValueIndex];

		if (Float.isNaN(value0)) {
			return UI.DASH_WITH_SPACE;
		}

		return String.format(

				TourManager.GEAR_VALUE_FORMAT,
				(int) value1,
				(int) value2,
				value0

		);
	}

}
