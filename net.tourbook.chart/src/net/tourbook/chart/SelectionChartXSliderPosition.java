/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

	public int				slider1ValueIndex				= IGNORE_SLIDER_POSITION;
	public int				slider2ValueIndex				= IGNORE_SLIDER_POSITION;

	public Chart			chart;

	public SelectionChartXSliderPosition(Chart chart, int valueIndex1, int valueIndex2) {

		this.chart = chart;

		slider1ValueIndex = valueIndex1;
		slider2ValueIndex = valueIndex2;
	}

	public boolean isEmpty() {
		return false;
	}
}
