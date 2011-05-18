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
package net.tourbook.statistics;

import net.tourbook.chart.Chart;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public class StatisticTourHrZone extends YearStatistic {

	private IPostSelectionProvider	_postSelectionProvider;
	private Chart					_chart;
	private TourPerson				_activePerson;
	private TourTypeFilter			_activeTourTypeFiler;
	private int						_currentYear;
	private int						_numberOfYears;

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		// TODO Auto-generated method stub

	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		_postSelectionProvider = postSelectionProvider;

		// chart widget page
		_chart = new Chart(parent, SWT.BORDER | SWT.FLAT);

	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {
		// TODO Auto-generated method stub

	}

	@Override
	public void prefColorChanged() {
		// TODO Auto-generated method stub

	}

	public void refreshStatistic(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int year,
									final int numberOfYears,
									final boolean refreshData) {

		_activePerson = person;
		_activeTourTypeFiler = tourTypeFilter;
		_currentYear = year;
		_numberOfYears = numberOfYears;

//		/*
//		 * get currently selected tour id
//		 */
//		long selectedTourId = -1;
//		final ISelection selection = _chart.getSelection();
//		if (selection instanceof SelectionBarChart) {
//			final SelectionBarChart barChartSelection = (SelectionBarChart) selection;
//
//			if (barChartSelection.serieIndex != -1) {
//
//				int selectedValueIndex = barChartSelection.valueIndex;
//				final long[] tourIds = _tourTimeData.fTourIds;
//
//				if (tourIds.length > 0) {
//					if (selectedValueIndex >= tourIds.length) {
//						selectedValueIndex = tourIds.length - 1;
//					}
//
//					selectedTourId = tourIds[selectedValueIndex];
//				}
//			}
//		}
//
//		_tourTimeData = DataProviderTourTime.getInstance().getTourTimeData(
//				person,
//				tourTypeFilter,
//				year,
//				numberOfYears,
//				isDataDirtyWithReset() || refreshData);
//
//		// reset min/max values
//		if (_ifIsSynchScaleEnabled == false && refreshData) {
//			_minMaxKeeper.resetMinMax();
//		}
//
//		updateChart(selectedTourId);
	}

	@Override
	public void resetSelection() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSynchScale(final boolean isEnabled) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		// TODO Auto-generated method stub

	}

}
