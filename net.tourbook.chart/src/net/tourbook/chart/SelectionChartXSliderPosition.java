/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.viewers.ISelection;

/**
 * contains the value index for the sliders
 */
public class SelectionChartXSliderPosition implements ISelection {

	public static final int	IGNORE_SLIDER_POSITION			= -1;
	public static final int	SLIDER_POSITION_AT_CHART_BORDER	= -2;

	private int				fSlider1ValueIndex				= IGNORE_SLIDER_POSITION;
	private int				fSlider2ValueIndex				= IGNORE_SLIDER_POSITION;
	
	private boolean			fCenterSliderPosition			= false;

	private Chart			fChart;

	public SelectionChartXSliderPosition(final Chart chart, final int valueIndex1, final int valueIndex2) {

		fChart = chart;

		fSlider1ValueIndex = valueIndex1;
		fSlider2ValueIndex = valueIndex2;
	}

	public SelectionChartXSliderPosition(	final Chart chart,
											final int serieIndex1,
											final int serieIndex2,
											final boolean centerSliderPosition) {

		this(chart, serieIndex1, serieIndex2);

		this.fCenterSliderPosition = centerSliderPosition;
	}

	public Chart getChart() {
		return fChart;
	}

	public int getSlider1ValueIndex() {
		return fSlider1ValueIndex;
	}

	public int getSlider2ValueIndex() {
		return fSlider2ValueIndex;
	}

	public boolean isCenterSliderPosition() {
		return fCenterSliderPosition;
	}

	public boolean isEmpty() {
		return false;
	}
}
