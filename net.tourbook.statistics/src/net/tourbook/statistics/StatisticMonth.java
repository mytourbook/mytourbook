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

package net.tourbook.statistics;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.GregorianCalendar;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticMonth extends YearStatistic {

	private TourPerson					fActivePerson;
	private TourTypeFilter				fActiveTourTypeFilter;

	private int							fCurrentYear;
	private int							fNumberOfYears;

	private Chart						fChart;
	private final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	private boolean						fIsSynchScaleEnabled;

	private final Calendar				fCalendar		= GregorianCalendar.getInstance();
	private DateFormat					fDateFormatter	= DateFormat.getDateInstance(DateFormat.FULL);

	private TourDataMonth				fTourMonthData;

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return false;
	}

	ChartSegments createChartSegments(final TourDataMonth tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.fAltitudeHigh[0].length;
		final int segmentStart[] = new int[fNumberOfYears];
		final int segmentEnd[] = new int[fNumberOfYears];
		final String[] segmentTitle = new String[fNumberOfYears];

		final int oldestYear = fCurrentYear - fNumberOfYears + 1;

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {

			final int yearIndex = monthIndex / 12;

			if (monthIndex % 12 == 0) {

				// first month in a year
				segmentStart[yearIndex] = monthIndex;
				segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			} else if (monthIndex % 12 == 11) {

				// last month in a year
				segmentEnd[yearIndex] = monthIndex;
			}
		}

		final ChartSegments monthSegments = new ChartSegments();
		monthSegments.valueStart = segmentStart;
		monthSegments.valueEnd = segmentEnd;
		monthSegments.segmentTitle = segmentTitle;

		return monthSegments;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		// create chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);
	}

	int[] createMonthData(final TourDataMonth tourMonthData) {

		/*
		 * create segments for each year
		 */
		final int monthCounter = tourMonthData.fAltitudeHigh[0].length;
		final int allMonths[] = new int[monthCounter];

		// get start/end and title for each segment
		for (int monthIndex = 0; monthIndex < monthCounter; monthIndex++) {
			allMonths[monthIndex] = monthIndex;
		}

		return allMonths;
	}

	private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

		final int oldestYear = fCurrentYear - fNumberOfYears + 1;

		final Calendar calendar = GregorianCalendar.getInstance();

		calendar.set(oldestYear, 0, 1);
		calendar.add(Calendar.MONTH, valueIndex);

		//
		final StringBuffer monthStringBuffer = new StringBuffer();
		final FieldPosition monthPosition = new FieldPosition(DateFormat.MONTH_FIELD);

		final Date date = new Date();
		date.setTime(calendar.getTimeInMillis());
		fDateFormatter.format(date, monthStringBuffer, monthPosition);

		final Integer recordingTime = fTourMonthData.fRecordingTime[serieIndex][valueIndex];
		final Integer drivingTime = fTourMonthData.fDrivingTime[serieIndex][valueIndex];
		final int breakTime = recordingTime - drivingTime;

		/*
		 * tool tip: title
		 */
		final StringBuilder titleString = new StringBuilder();

		final String tourTypeName = getTourTypeName(serieIndex, fActiveTourTypeFilter);
		if (tourTypeName != null && tourTypeName.length() > 0) {
			titleString.append(tourTypeName);
		}

		final String toolTipTitle = new Formatter().format(Messages.tourtime_info_date_month, //
				titleString.toString(),
				monthStringBuffer.substring(monthPosition.getBeginIndex(), monthPosition.getEndIndex()),
				calendar.get(Calendar.YEAR)
		//
		)
				.toString();

		/*
		 * tool tip: label
		 */
		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourtime_info_distance_tour);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_altitude);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_recording_time);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_driving_time);
		toolTipFormat.append(NEW_LINE);
		toolTipFormat.append(Messages.tourtime_info_break_time);

		final String toolTipLabel = new Formatter().format(toolTipFormat.toString(), //
				//
				(float) fTourMonthData.fDistanceHigh[serieIndex][valueIndex] / 1000,
				UI.UNIT_LABEL_DISTANCE,
				//
				fTourMonthData.fAltitudeHigh[serieIndex][valueIndex],
				UI.UNIT_LABEL_ALTITUDE,
				//
				recordingTime / 3600,
				(recordingTime % 3600) / 60,
				//
				drivingTime / 3600,
				(drivingTime % 3600) / 60,
				//
				breakTime / 3600,
				(breakTime % 3600) / 60
		//
		)
				.toString();

		/*
		 * create tool tip info
		 */

		final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
		toolTipInfo.setTitle(toolTipTitle);
		toolTipInfo.setLabel(toolTipLabel);
//		toolTipInfo.setLabel(toolTipFormat.toString());

		return toolTipInfo;
	}

	void createXDataMonths(final ChartDataModel chartDataModel) {
		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createMonthData(fTourMonthData));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_MONTH);
		xData.setChartSegments(createChartSegments(fTourMonthData));

		chartDataModel.setXData(xData);
	}

	void createYDataAltitude(final ChartDataModel chartDataModel) {
		// altitude
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourMonthData.fAltitudeLow,
				fTourMonthData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourMonthData.fTypeIds, fActiveTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	void createYDataDistance(final ChartDataModel chartDataModel) {
		// distance
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourMonthData.fDistanceLow,
				fTourMonthData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setValueDivisor(1000);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourMonthData.fTypeIds, fActiveTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	void createYDataTourTime(final ChartDataModel chartDataModel) {
		// duration
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				fTourMonthData.fTimeLow,
				fTourMonthData.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourMonthData.fTypeIds, fActiveTourTypeFilter);

		chartDataModel.addYData(yData);
	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fCurrentYear, fNumberOfYears, false);
	}

	public void refreshStatistic(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int currentYear,
									final int numberOfYears,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;
		fCurrentYear = currentYear;
		fNumberOfYears = numberOfYears;

		fTourMonthData = DataProviderTourMonth.getInstance().getMonthData(person,
				tourTypeFilter,
				currentYear,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		final ChartDataModel chartDataModel = updateChart();

		setChartProviders(chartDataModel);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartDataModel);
		}

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		fChart.setGridDistance(prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the fDataModel in the chart
		fChart.updateChart(chartDataModel, true);
	}

	@Override
	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	@Override
	public boolean selectMonth(final Long date) {

		fCalendar.setTimeInMillis(date);
		final int selectedMonth = fCalendar.get(Calendar.MONTH);

		final boolean selectedItems[] = new boolean[12];
		selectedItems[selectedMonth] = true;

		fChart.setSelectedBars(selectedItems);

		return true;
	}

	private void setChartProviders(final ChartDataModel chartModel) {

		// set tool tip info
		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, new IChartInfoProvider() {
			public ChartToolTipInfo getToolTipInfo(final int serieIndex, final int valueIndex) {
				return createToolTipInfo(serieIndex, valueIndex);
			}
		});
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract ChartDataModel updateChart();

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
