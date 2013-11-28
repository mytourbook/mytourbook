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
package net.tourbook.tour;

import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.data.TourData;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.viewers.ISelection;

/**
 * Selection is fired when a tour was selected.
 */
public class SelectionTourData implements ISelection {

	private TourChart	_tourChart;
	private TourData	_tourData;

	private int			_leftSliderValueIndex	= SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
	private int			_rightSliderValueIndex	= SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;

	public SelectionTourData(final TourChart tourChart, final TourData tourData) {
		_tourChart = tourChart;
		_tourData = tourData;
	}

	public SelectionTourData(final TourData tourData) {
		_tourData = tourData;
	}

	public int getLeftSliderValueIndex() {
		return _leftSliderValueIndex;
	}

	public int getRightSliderValueIndex() {
		return _rightSliderValueIndex;
	}

	/**
	 * @return Returns the tour chart for the tour data or <code>null</code> when a tour chart is
	 *         not available
	 */
	public TourChart getTourChart() {
		return _tourChart;
	}

	public TourData getTourData() {
		return _tourData;
	}

	public boolean isEmpty() {
		return false;
	}

	/**
	 * @return Returns <code>true</code> when left or right slider value is set in this selection.
	 */
	public boolean isSliderValueIndexAvailable() {

		return _leftSliderValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
				|| _rightSliderValueIndex != SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION;
	}

	public void setSliderValueIndex(final int leftSliderValueIndex, final int rightSliderValueIndex) {

		_leftSliderValueIndex = leftSliderValueIndex;
		_rightSliderValueIndex = rightSliderValueIndex;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[SelectionTourData]\n");//$NON-NLS-1$

		sb.append("\t_tourData:");//$NON-NLS-1$
		if (_tourData == null) {
			sb.append(_tourData);
		} else {
			sb.append(_tourData.toString());
		}

		sb.append(UI.NEW_LINE);

		sb.append("\t_tourChart:");//$NON-NLS-1$
		if (_tourChart == null) {
			sb.append(_tourChart);
		} else {
			sb.append(_tourChart.toString());
		}

		sb.append(UI.NEW_LINE);

		return sb.toString();
	}

}
