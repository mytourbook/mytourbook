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
import java.util.Formatter;
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public class StatisticTourTime extends YearStatistic implements IBarSelectionProvider {

	private TourPerson					fActivePerson;
	private TourTypeFilter				fActiveTourTypeFiler;
	private int							fCurrentYear;
	private int							fNumberOfYears;

	private final Calendar				fCalendar		= GregorianCalendar.getInstance();

	private Chart						fChart;
	private TourTimeData				fTourTimeData;

	private final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();
	private boolean						fIsSynchScaleEnabled;

	private IPostSelectionProvider		fPostSelectionProvider;

	private Long						fSelectedTourId;
	protected int						fCurrentMonth;

	@Override
	public void activateActions(IWorkbenchPartSite partSite) {

//		IContextService contextService = (IContextService) partSite.getService(IContextService.class);
//		fContextBarChart = contextService.activateContext(Chart.CONTEXT_ID_BAR_CHART);
//		net.tourbook.chart.context.isTourChart
//		fChart.updateChartActionHandlers();
	}

	@Override
	public void deactivateActions(IWorkbenchPartSite partSite) {

//		IContextService contextService = (IContextService) partSite.getService(IContextService.class);
//		contextService.deactivateContext(fContextBarChart);
	}

	@Override
	public void createControl(	final Composite parent,
								IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		fPostSelectionProvider = postSelectionProvider;

		// chart widget page
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowPartNavigation(true);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setDrawBarChartAtBottom(false);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

//		fChart.createChartActionHandlers();

		fChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, int valueIndex) {

				final long[] tourIds = fTourTimeData.fTourIds;

				if (tourIds != null && tourIds.length > 0) {

					if (valueIndex >= tourIds.length) {
						valueIndex = tourIds.length - 1;
					}

					long selectedTourId = tourIds[valueIndex];
					ProviderTourTime.getInstance().setSelectedTourId(selectedTourId);
					fPostSelectionProvider.setSelection(new SelectionTourId(selectedTourId));
				}
			}
		});

		/*
		 * open tour with double click on the tour bar
		 */
		fChart.addDoubleClickListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				final long[] tourIds = fTourTimeData.fTourIds;
				if (tourIds.length > 0) {
					long selectedTourId = tourIds[valueIndex];
					ProviderTourTime.getInstance().setSelectedTourId(selectedTourId);
					TourManager.getInstance().openTourInEditor(selectedTourId);
				}
			}
		});

		/*
		 * open tour with Enter key
		 */
		fChart.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent event) {

				if (event.detail == SWT.TRAVERSE_RETURN) {
					ISelection selection = fChart.getSelection();
					if (selection instanceof SelectionBarChart) {
						SelectionBarChart barChartSelection = (SelectionBarChart) selection;

						if (barChartSelection.serieIndex != -1) {

							long selectedTourId = fTourTimeData.fTourIds[barChartSelection.valueIndex];
							TourManager.getInstance().openTourInEditor(selectedTourId);
						}
					}
				}
			}
		});

	}

	public Integer getSelectedMonth() {
		return fCurrentMonth;
	}

	public Long getSelectedTourId() {
		return fSelectedTourId;
	}

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTourTypeFiler, fCurrentYear, 1, false);
	}

	public void refreshStatistic(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int year,
									int numberOfYears,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFiler = tourTypeFilter;
		fCurrentYear = year;
		fNumberOfYears = numberOfYears;

		/*
		 * get currently selected tour id
		 */
		long selectedTourId = -1;
		ISelection selection = fChart.getSelection();
		if (selection instanceof SelectionBarChart) {
			SelectionBarChart barChartSelection = (SelectionBarChart) selection;

			if (barChartSelection.serieIndex != -1) {

				int selectedValueIndex = barChartSelection.valueIndex;
				final long[] tourIds = fTourTimeData.fTourIds;

				if (selectedValueIndex >= tourIds.length) {
					selectedValueIndex = tourIds.length - 1;
				}

				selectedTourId = tourIds[selectedValueIndex];
			}
		}

//		System.out.println("selectedTourId: " + selectedTourId);
		fTourTimeData = ProviderTourTime.getInstance().getTourTimeData(person,
				tourTypeFilter,
				year,
				numberOfYears,
				isDataDirtyWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(selectedTourId);
	}

	@Override
	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	@Override
	public boolean selectMonth(final Long date) {

		fCalendar.setTimeInMillis(date);
		final int tourMonth = fCalendar.get(Calendar.MONTH);
		final int[] tourMonths = fTourTimeData.fTourMonthValues;

		final boolean selectedItems[] = new boolean[tourMonths.length];
		boolean isSelected = false;

		// find the tours which have the same day as the selected day
		for (int tourIndex = 0; tourIndex < tourMonths.length; tourIndex++) {
			final boolean isMonthSelected = tourMonths[tourIndex] == tourMonth ? true : false;
			if (isMonthSelected) {
				isSelected = true;
			}
			selectedItems[tourIndex] = isMonthSelected;
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedItems);
		}

		return isSelected;
	}

	@Override
	public boolean selectTour(final Long tourId) {

		final long[] tourIds = fTourTimeData.fTourIds;
		final boolean selectedTours[] = new boolean[tourIds.length];

		boolean isSelected = false;

		// find the tour which has the same tourId as the selected tour
		for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
			if ((tourIds[tourIndex] == tourId)) {
				selectedTours[tourIndex] = true;
				isSelected = true;
			}
		}

		if (isSelected == false) {
			// select first tour
			selectedTours[0] = true;
		}

		fChart.setSelectedBars(selectedTours);

		return isSelected;
	}

	private void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		final IChartInfoProvider chartInfoProvider = new IChartInfoProvider() {

			public String getInfo(final int serieIndex, int valueIndex) {

				final int[] tourDateValues = fTourTimeData.fTourDOYValues;

				if (valueIndex >= tourDateValues.length) {
					valueIndex -= tourDateValues.length;
				}

				if (tourDateValues == null || valueIndex >= tourDateValues.length) {
					return ""; //$NON-NLS-1$
				}
				fCalendar.set(fCurrentYear, 0, 1);
				fCalendar.set(Calendar.DAY_OF_YEAR, tourDateValues[valueIndex] + 1);

				fCurrentMonth = fCalendar.get(Calendar.MONTH) + 1;
				fSelectedTourId = fTourTimeData.fTourIds[valueIndex];

				String tourTypeName = TourDatabase.getTourTypeName(fTourTimeData.fTypeIds[valueIndex]);

				final int[] startValue = fTourTimeData.fTourTimeStartValues;
				final int[] endValue = fTourTimeData.fTourTimeEndValues;
				final int[] durationValue = fTourTimeData.fTourTimeDurationValues;

				StringBuilder infoText = new StringBuilder();
				infoText.append(Messages.TOURTIMEINFO_DATE_FORMAT);
				infoText.append(Messages.TOURTIMEINFO_DISTANCE);
				infoText.append(Messages.TOURTIMEINFO_ALTITUDE);
				infoText.append(Messages.TOURTIMEINFO_DURATION);
				infoText.append(Messages.TOURTIMEINFO_TOUR_TYPE);

				final String barInfo = new Formatter().format(infoText.toString(),
						fCalendar.get(Calendar.DAY_OF_MONTH),
						fCalendar.get(Calendar.MONTH) + 1,
						fCalendar.get(Calendar.YEAR),
						startValue[valueIndex] / 3600,
						(startValue[valueIndex] % 3600) / 60,
						endValue[valueIndex] / 3600,
						(endValue[valueIndex] % 3600) / 60,
						fTourTimeData.fTourTimeDistanceValues[valueIndex],
						UI.UNIT_LABEL_DISTANCE,
						fTourTimeData.fTourTimeAltitudeValues[valueIndex],
						UI.UNIT_LABEL_ALTITUDE,
						durationValue[valueIndex] / 3600,
						(durationValue[valueIndex] % 3600) / 60,
						tourTypeName).toString();

				return barInfo;
			}
		};

		chartModel.setCustomData(ChartDataModel.BAR_INFO_PROVIDER, chartInfoProvider);

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourContextProvider(fChart, this));
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void updateChart(long selectedTourId) {

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(fTourTimeData.fTourDOYValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		xData.setVisibleMaxValue(fCurrentYear);
		xData.setChartSegments(createChartSegments(fTourTimeData));
		chartModel.setXData(xData);

		// set the bar low/high data
		final ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				new int[][] { fTourTimeData.fTourTimeStartValues },
				new int[][] { fTourTimeData.fTourTimeEndValues });
		yData.setYTitle(Messages.LABEL_GRAPH_DAYTIME);
		yData.setUnitLabel(Messages.LABEL_GRAPH_TIME_UNIT);
		yData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_HOUR_MINUTE_24H);
		yData.setYAxisDirection(false);

		yData.setColorIndex(new int[][] { fTourTimeData.fTypeColorIndex });
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

		chartModel.addYData(yData);

		/*
		 * set graph minimum width, this is the number of days in the year
		 */
		fCalendar.set(fCurrentYear, 11, 31);
		final int yearDays = fCalendar.get(Calendar.DAY_OF_YEAR);
		chartModel.setChartMinWidth(yearDays);

		setChartProviders(fChart, chartModel);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartModel);
		}

		// show the data in the chart
		fChart.updateChart(chartModel, false);

		// try to select the previous selected tour
		selectTour(selectedTourId);
	}

	/**
	 * create segments for the chart
	 */
	ChartSegments createChartSegments(TourTimeData tourDataTime) {

		int segmentStart[] = new int[fNumberOfYears];
		int segmentEnd[] = new int[fNumberOfYears];
		String[] segmentTitle = new String[fNumberOfYears];

		int[] allYearDays = tourDataTime.yearDays;
		int oldestYear = fCurrentYear - fNumberOfYears + 1;
		int yearDaysSum = 0;

		// create segments for each year
		for (int yearIndex = 0; yearIndex < allYearDays.length; yearIndex++) {

			int yearDays = allYearDays[yearIndex];

			segmentStart[yearIndex] = yearDaysSum;
			segmentEnd[yearIndex] = yearDaysSum + yearDays - 1;
			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			yearDaysSum += yearDays;
		}

		ChartSegments chartSegments = new ChartSegments();
		chartSegments.valueStart = segmentStart;
		chartSegments.valueEnd = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = tourDataTime.years;
		chartSegments.yearDays = tourDataTime.yearDays;
		chartSegments.allValues = tourDataTime.allDaysInAllYears;

		return chartSegments;
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}

}
