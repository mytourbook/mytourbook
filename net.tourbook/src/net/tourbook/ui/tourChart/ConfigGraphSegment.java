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

import net.tourbook.common.color.GraphColorManager;
import net.tourbook.tour.TourManager;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

public class ConfigGraphSegment {

	float[]				segmentDataSerie;
	IValueLabelProvider	labelProvider;

	/**
	 * Is <code>true</code> when negative values can occure, e.g. gradient.
	 */
	boolean				canHaveNegativeValues;

	/**
	 * Position of the painted segment values which is used to get a hovered segment.
	 */
	Rectangle[]			paintedValues;

	RGB					segmentLineColor;

	/**
	 * @param segmentDataSerie
	 * @param labelProvider
	 * @param canHaveNegativeValues
	 * @param graphColorName
	 */
	public ConfigGraphSegment(	final float[] segmentDataSerie,
								final IValueLabelProvider labelProvider,
								final boolean canHaveNegativeValues,
								final String graphColorName) {

		this.segmentDataSerie = segmentDataSerie;
		this.canHaveNegativeValues = canHaveNegativeValues;
		this.labelProvider = labelProvider;

//		this.segmentLineColor = ColorUtil.getComplimentColor(
//				Display.getDefault(),
//				TourManager.getGraphColor(graphColorName, GraphColorManager.PREF_COLOR_TEXT));

		this.segmentLineColor = TourManager.getGraphColor(graphColorName, GraphColorManager.PREF_COLOR_TEXT);

//		final int bw = 0x80;
//		this.segmentLineColor = new RGB(bw, bw, bw);
	}

}
