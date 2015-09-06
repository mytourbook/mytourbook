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
package net.tourbook.chart;

import java.util.ArrayList;

/**
 * Contains data how a chart is painted.
 */
public class ChartDrawingData {

	public ArrayList<GraphDrawingData>	graphDrawingData;

	ChartDataModel						chartDataModel;

	int									devMarginTop;
	int									devXTitelBarHeight;
	int									devSliderBarHeight;
	int									devXAxisHeight;

	public int							devVisibleChartWidth;

	/**
	 * Contains the painted chart titles and their positons.
	 */
	public ArrayList<ChartTitleSegment>	chartTitleSegments	= new ArrayList<ChartTitleSegment>();

	ChartDrawingData(final ArrayList<GraphDrawingData> graphDrawingData) {

		this.graphDrawingData = graphDrawingData;
	}
}
