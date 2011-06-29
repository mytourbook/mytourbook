/*******************************************************************************
 * Copyright (C) 2005, 201  Wolfgang Schramm and Contributors
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

public class SelectionChartInfo implements ISelection {

	private Chart			_chart;

	public ChartDataModel	chartDataModel;
	public ChartDrawingData	chartDrawingData;

	public int				leftSliderValuesIndex;
	public int				rightSliderValuesIndex;

	/**
	 * contains the value index for the slider which is selected
	 */
	public int				selectedSliderValuesIndex;

	@SuppressWarnings("unused")
	private SelectionChartInfo() {}

	public SelectionChartInfo(final Chart chart) {
		_chart = chart;
	}

	public Chart getChart() {
		return _chart;
	}

	public boolean isEmpty() {
		return false;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[SelectionChartInfo] "); //$NON-NLS-1$
		sb.append("left:"); //$NON-NLS-1$
		sb.append(leftSliderValuesIndex);
		sb.append(" right:"); //$NON-NLS-1$
		sb.append(rightSliderValuesIndex);

		sb.append("\n\t");//$NON-NLS-1$
		sb.append(_chart.getChartDataModel());

		return sb.toString();
	}

}
