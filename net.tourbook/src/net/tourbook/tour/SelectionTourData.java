/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourData;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.viewers.ISelection;

/**
 * selection is fired when a tour was selected
 */
public class SelectionTourData implements ISelection {

	private TourChart	_tourChart;
	private TourData	_tourData;

	private boolean		_isForceRedraw	= false;

	public SelectionTourData(final TourChart tourChart, final TourData tourData) {
		_tourChart = tourChart;
		_tourData = tourData;
	}

	/**
	 * @param tourChart
	 * @param tourData
	 * @param forceRedraw
	 *            when <code>true</code> the displayed tour should be redrawn because the
	 *            {@link TourData} has been changed
	 */
	public SelectionTourData(final TourChart tourChart, final TourData tourData, final boolean forceRedraw) {
		_tourChart = tourChart;
		_tourData = tourData;
		_isForceRedraw = forceRedraw;
	}

	public SelectionTourData(final TourData tourData) {
		_tourData = tourData;
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

	public boolean isForceRedraw() {
		return _isForceRedraw;
	}

	public void setForceRedraw(final boolean fForceRedraw) {
		this._isForceRedraw = fForceRedraw;
	}

	@Override
	public String toString() {

		final StringBuilder sb = new StringBuilder();

		sb.append("[SelectionTourData]\n");//$NON-NLS-1$

		sb.append("\tfTourData:");//$NON-NLS-1$
		if (_tourData == null) {
			sb.append(_tourData);
		} else {
			sb.append(_tourData.toString());
		}

		sb.append(UI.NEW_LINE);

		sb.append("\tfTourChart:");//$NON-NLS-1$
		if (_tourChart == null) {
			sb.append(_tourChart);
		} else {
			sb.append(_tourChart.toString());
		}

		sb.append(UI.NEW_LINE);

		return sb.toString();
	}

}
