/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.text.NumberFormat;

import net.tourbook.chart.ISliderLabelProvider;

public class SliderLabelProvider_Cadence implements ISliderLabelProvider {

	private final NumberFormat	_nf_1_0	= NumberFormat.getNumberInstance();
	private final NumberFormat	_nf_1_1	= NumberFormat.getNumberInstance();
	{
		_nf_1_0.setMinimumFractionDigits(1);
		_nf_1_0.setMaximumFractionDigits(0);

		_nf_1_1.setMinimumFractionDigits(1);
		_nf_1_1.setMaximumFractionDigits(1);
	}

	private float[]				_cadenceSerie;

	public SliderLabelProvider_Cadence(final float[] cadenceSerie) {
		_cadenceSerie = cadenceSerie;
	}

	@Override
	public String getLabel(final int sliderValueIndex) {

		final float cadence = _cadenceSerie[sliderValueIndex];
		final float cadDigits = cadence - (int) cadence;

		/*
		 * Show digits only when it's large enough
		 */
		if (cadDigits > 0.1) {
			return _nf_1_1.format(cadence);
		} else {
			return _nf_1_0.format(cadence);
		}
	}

}
