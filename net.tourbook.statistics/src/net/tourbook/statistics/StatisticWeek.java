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

package net.tourbook.statistics;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartSegments;
import net.tourbook.data.TourPerson;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticWeek extends YearStatistic {

	Chart					fChart;
	BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	private TourPerson		fActivePerson;
	int						fCurrentYear;
	int						fNumberOfYears;
	TourTypeFilter			fActiveTourTypeFilter;

	private final Calendar	fCalendar		= GregorianCalendar.getInstance();
	boolean					fIsSynchScaleEnabled;

	public boolean canTourBeVisible() {
		return false;
	}

	@Override
	public void activateActions(IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	@Override
	public void deactivateActions(IWorkbenchPartSite partSite) {}

	@Override
	public void createControl(Composite parent, IViewSite viewSite, final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		// create chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, fNumberOfYears, false);
	}

	public void refreshStatistic(	TourPerson person,
									TourTypeFilter typeId,
									int currentYear,
									int numberOfYears,
									boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = typeId;
		fCurrentYear = currentYear;
		fNumberOfYears = numberOfYears;

		TourDataWeek tourWeekData = ProviderTourWeek.getInstance().getWeekData(person,
				typeId,
				currentYear,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(tourWeekData);
	}

	@Override
	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	@Override
	public boolean selectDay(Long date) {
		fCalendar.setTimeInMillis(date);
		int selectedWeek = fCalendar.get(Calendar.WEEK_OF_YEAR) - 0;

		boolean selectedItems[] = new boolean[ProviderTourWeek.YEAR_WEEKS];
		selectedItems[selectedWeek] = true;

		fChart.setSelectedBars(selectedItems);

		return true;
	}

	@Override
	public boolean selectMonth(Long date) {

		fCalendar.setTimeInMillis(date);
		int selectedMonth = fCalendar.get(Calendar.MONTH);

		boolean selectedItems[] = new boolean[ProviderTourWeek.YEAR_WEEKS];
		boolean isSelected = false;

		// select all weeks in the selected month
		for (int weekIndex = 0; weekIndex < selectedItems.length; weekIndex++) {
			fCalendar.set(Calendar.WEEK_OF_YEAR, weekIndex + 0);

			boolean isMonthSelected = fCalendar.get(Calendar.MONTH) == selectedMonth ? true : false;
			if (isMonthSelected) {
				isSelected = true;
			}
			selectedItems[weekIndex] = isMonthSelected;
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedItems);
		}

		return isSelected;
	}

	@Override
	public void setSynchScale(boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract void updateChart(TourDataWeek tourWeekData);

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}

	protected int[] createWeekData(TourDataWeek tourWeekData) {
		int weekCounter = tourWeekData.fAltitudeHigh[0].length;
		int allWeeks[] = new int[weekCounter];
		for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {
			allWeeks[weekIndex] = weekIndex;
		}
		return allWeeks;
	}

	protected ChartSegments createChartSegments(TourDataWeek tourWeekData) {
		/*
		 * create segments for each year
		 */
		int yearWeeks = ProviderTourWeek.YEAR_WEEKS;
		int segmentStart[] = new int[fNumberOfYears];
		int segmentEnd[] = new int[fNumberOfYears];
		String[] segmentTitle = new String[fNumberOfYears];

		int weekCounter = tourWeekData.fAltitudeHigh[0].length;

		int oldestYear = fCurrentYear - fNumberOfYears + 1;

		// get start/end and title for each segment
		for (int weekIndex = 0; weekIndex < weekCounter; weekIndex++) {

			int currentYearIndex = weekIndex / yearWeeks;

			if (weekIndex % yearWeeks == 0) {

				// first week in a year

				segmentStart[currentYearIndex] = weekIndex;
				segmentTitle[currentYearIndex] = Integer.toString(oldestYear + currentYearIndex);

			} else if (weekIndex % yearWeeks == yearWeeks - 1) {

				// last week in a year

				segmentEnd[currentYearIndex] = weekIndex;
			}
		}

		ChartSegments weekSegments = new ChartSegments();
		weekSegments.valueStart = segmentStart;
		weekSegments.valueEnd = segmentEnd;
		weekSegments.segmentTitle = segmentTitle;

		return weekSegments;
	}
}
