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

import java.util.Formatter;

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
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public abstract class StatisticYear extends YearStatistic {

	private static final String			STRING_SEPARATOR	= " - "; //$NON-NLS-1$
	private TourPerson					fActivePerson;
	private TourTypeFilter				fActiveTourTypeFilter;

	private int							fCurrentYear;
	private int							fNumberOfYears;

	private Chart						fChart;

	private final BarChartMinMaxKeeper	fMinMaxKeeper		= new BarChartMinMaxKeeper();
	private TourDataYear				fTourYearData;

	private boolean						fIsSynchScaleEnabled;

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	public boolean canTourBeVisible() {
		return false;
	}

	ChartSegments createChartSegments(final TourDataYear tourDataYear) {

		final int yearCounter = tourDataYear.fAltitudeHigh[0].length;

		final int segmentStart[] = new int[fNumberOfYears];
		final int segmentEnd[] = new int[fNumberOfYears];
		final String[] segmentTitle = new String[fNumberOfYears];

		final int oldestYear = fCurrentYear - fNumberOfYears + 1;

		for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {

			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			segmentStart[yearIndex] = yearIndex;
			segmentEnd[yearIndex] = yearIndex;
		}

		final ChartSegments yearSegments = new ChartSegments();
		yearSegments.valueStart = segmentStart;
		yearSegments.valueEnd = segmentEnd;
		yearSegments.segmentTitle = segmentTitle;
		yearSegments.years = tourDataYear.years;

		return yearSegments;
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

	private ChartToolTipInfo createToolTipInfo(final int serieIndex, final int valueIndex) {

		final int oldestYear = fCurrentYear - fNumberOfYears + 1;

		final Integer recordingTime = fTourYearData.fRecordingTime[serieIndex][valueIndex];
		final Integer drivingTime = fTourYearData.fDrivingTime[serieIndex][valueIndex];
		final int breakTime = recordingTime - drivingTime;

		/*
		 * tool tip: title
		 */
		final StringBuilder titleString = new StringBuilder();

		final String tourTypeName = getTourTypeName(serieIndex, fActiveTourTypeFilter);
		final boolean isTourType = tourTypeName != null && tourTypeName.length() > 0;

		if (isTourType) {
			titleString.append(tourTypeName);
			titleString.append(STRING_SEPARATOR);
		}
		titleString.append(Messages.tourtime_info_date_year);
		titleString.append(NEW_LINE);

		final String toolTipTitle = new Formatter().format(titleString.toString(), oldestYear + valueIndex).toString();

		/*
		 * tool tip: label
		 */
		final StringBuilder toolTipFormat = new StringBuilder();
		toolTipFormat.append(Messages.tourtime_info_distance);
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
				//
				fTourYearData.fDistanceHigh[serieIndex][valueIndex],
				UI.UNIT_LABEL_DISTANCE,
				//
				fTourYearData.fAltitudeHigh[serieIndex][valueIndex],
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

	void createXDataYear(final ChartDataModel chartDataModel) {
		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(createYearData(fTourYearData));
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		xData.setChartSegments(createChartSegments(fTourYearData));
		chartDataModel.setXData(xData);
	}

	/**
	 * altitude
	 * 
	 * @param chartDataModel
	 */
	void createYDataAltitude(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_BESIDE,
				fTourYearData.fAltitudeLow,
				fTourYearData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourYearData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_ALTITUDE);

		chartDataModel.addYData(yData);
	}

	/**
	 * distance
	 * 
	 * @param chartDataModel
	 */
	void createYDataDistance(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_BESIDE,
				fTourYearData.fDistanceLow,
				fTourYearData.fDistanceHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_DISTANCE);
		yData.setUnitLabel(UI.UNIT_LABEL_DISTANCE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourYearData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_DISTANCE);

		chartDataModel.addYData(yData);
	}

	/**
	 * duration
	 * 
	 * @param chartDataModel
	 */
	void createYDataDuration(final ChartDataModel chartDataModel) {

		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_BESIDE,
				fTourYearData.fTimeLow,
				fTourYearData.fTimeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_TIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_HOUR_MINUTE);
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, fActiveTourTypeFilter);
		StatisticServices.setTourTypeColorIndex(yData, fTourYearData.fTypeIds, fActiveTourTypeFilter);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		chartDataModel.addYData(yData);
	}

	int[] createYearData(final TourDataYear tourDataYear) {

		final int yearCounter = tourDataYear.fAltitudeHigh[0].length;
		final int allYears[] = new int[yearCounter];

		for (int yearIndex = 0; yearIndex < yearCounter; yearIndex++) {
			allYears[yearIndex] = yearIndex;
		}

		return allYears;
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

		fTourYearData = DataProviderTourYear.getInstance().getYearData(person,
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
