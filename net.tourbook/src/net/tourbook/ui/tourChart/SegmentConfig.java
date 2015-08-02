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
package net.tourbook.ui.tourChart;

public class SegmentConfig {

	/**
	 * Is <code>true</code> when negative values can occure, e.g. gradient.
	 */
	boolean						canHaveNegativeValues;
	float[]						segmentDataSerie;

	public IValueLabelProvider	labelProvider;

	public SegmentConfig(	final float[] segmentDataSerie,
							final boolean canHaveNegativeValues,
							final IValueLabelProvider labelProvider) {

		this.segmentDataSerie = segmentDataSerie;
		this.canHaveNegativeValues = canHaveNegativeValues;
		this.labelProvider = labelProvider;
	}

}
