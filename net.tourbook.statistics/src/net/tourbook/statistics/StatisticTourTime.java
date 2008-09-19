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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.ChartSegments;
import net.tourbook.chart.ChartToolTipInfo;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;

public class StatisticTourTime extends YearStatistic implements IBarSelectionProvider {

	private TourPerson					fActivePerson;
	private TourTypeFilter				fActiveTourTypeFiler;
	private int							fCurrentYear;
	private int							fNumberOfYears;

	private final Calendar				fCalendar		= GregorianCalendar.getInstance();
	private final DateFormat			fDateFormatter	= DateFormat.getDateInstance(DateFormat.FULL);

	private Chart						fChart;
	private TourTimeData				fTourTimeData;

	private final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();
	private boolean						fIsSynchScaleEnabled;

	private IPostSelectionProvider		fPostSelectionProvider;

	private Long						fSelectedTourId;
	private int							fCurrentMonth;

	@Override
	public void activateActions(final IWorkbenchPartSite partSite) {

//		IContextService contextService = (IContextService) partSite.getService(IContextService.class);
//		fContextBarChart = contextService.activateContext(Chart.CONTEXT_ID_BAR_CHART);
//		net.tourbook.chart.context.isTourChart
//		fChart.updateChartActionHandlers();
	}

	@Override
	public boolean canSelectTour() {
		return true;
	}

	/**
	 * create segments for the chart
	 */
	ChartSegments createChartSegments(final TourTimeData tourDataTime) {

		final int segmentStart[] = new int[fNumberOfYears];
		final int segmentEnd[] = new int[fNumberOfYears];
		final String[] segmentTitle = new String[fNumberOfYears];

		final int[] allYearDays = tourDataTime.yearDays;
		final int oldestYear = fCurrentYear - fNumberOfYears + 1;
		int yearDaysSum = 0;

		// create segments for each year
		for (int yearIndex = 0; yearIndex < allYearDays.length; yearIndex++) {

			final int yearDays = allYearDays[yearIndex];

			segmentStart[yearIndex] = yearDaysSum;
			segmentEnd[yearIndex] = yearDaysSum + yearDays - 1;
			segmentTitle[yearIndex] = Integer.toString(oldestYear + yearIndex);

			yearDaysSum += yearDays;
		}

		final ChartSegments chartSegments = new ChartSegments();
		chartSegments.valueStart = segmentStart;
		chartSegments.valueEnd = segmentEnd;
		chartSegments.segmentTitle = segmentTitle;

		chartSegments.years = tourDataTime.years;
		chartSegments.yearDays = tourDataTime.yearDays;
		chartSegments.allValues = tourDataTime.allDaysInAllYears;

		return chartSegments;
	}

	@Override
	public void createControl(	final Composite parent,
								final IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		fPostSelectionProvider = postSelectionProvider;

		// chart widget page
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
//		fChart.setShowPartNavigation(true);
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

					fSelectedTourId = tourIds[valueIndex];

					DataProviderTourTime.getInstance().setSelectedTourId(fSelectedTourId);
					fPostSelectionProvider.setSelection(new SelectionTourId(fSelectedTourId));
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
					fSelectedTourId = tourIds[valueIndex];
					DataProviderTourTime.getInstance().setSelectedTourId(fSelectedTourId);
					TourManager.getInstance().openTourInEditor(fSelectedTourId);
				}
			}
		});

		/*
		 * open tour with Enter key
		 */
		fChart.addTraverseListener(new TraverseListener() {
			public void keyTraversed(final TraverseEvent event) {

				if (event.detail == SWT.TRAVERSE_RETURN) {
					final ISelection selection = fChart.getSelection();
					if (selection instanceof SelectionBarChart) {
						final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

						if (barChartSelection.serieIndex != -1) {

							fSelectedTourId = fTourTimeData.fTourIds[barChartSelection.valueIndex];
							TourManager.getInstance().openTourInEditor(fSelectedTourId);
						}
					}
				}
			}
		});

	}

	@Override
	public void deactivateActions(final IWorkbenchPartSite partSite) {

//		IContextService contextService = (IContextService) partSite.getService(IContextService.class);
//		contextService.deactivateContext(fContextBarChart);
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
									final int numberOfYears,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFiler = tourTypeFilter;
		fCurrentYear = year;
		fNumberOfYears = numberOfYears;

		/*
		 * get currently selected tour id
		 */
		long selectedTourId = -1;
		final ISelection selection = fChart.getSelection();
		if (selection instanceof SelectionBarChart) {
			final SelectionBarChart barChartSelection = (SelectionBarChart) selection;

			if (barChartSelection.serieIndex != -1) {

				int selectedValueIndex = barChartSelection.valueIndex;
				final long[] tourIds = fTourTimeData.fTourIds;

				if (tourIds.length > 0) {
					if (selectedValueIndex >= tourIds.length) {
						selectedValueIndex = tourIds.length - 1;
					}

					selectedTourId = tourIds[selectedValueIndex];
				}
			}
		}

		fTourTimeData = DataProviderTourTime.getInstance().getTourTimeData(person,
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
	public void restoreState(final IMemento memento) {

		final String mementoTourId = memento.getString(MEMENTO_SELECTED_TOUR_ID);
		if (mementoTourId != null) {
			try {
				final long tourId = Long.parseLong(mementoTourId);
				selectTour(tourId);
			} catch (final Exception e) {
				// ignore
			}
		}
	}

	@Override
	public void saveState(final IMemento memento) {

		if (fChart == null || fChart.isDisposed()) {
			return;
		}

		final ISelection selection = fChart.getSelection();
		if (fTourTimeData != null && selection instanceof SelectionBarChart) {

			final Long selectedTourId = fTourTimeData.fTourIds[((SelectionBarChart) selection).valueIndex];

			memento.putString(MEMENTO_SELECTED_TOUR_ID, Long.toString(selectedTourId));
		}
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

		if (tourIds.length == 0) {
			fSelectedTourId = null;
			return false;
		}

		final boolean selectedTours[] = new boolean[tourIds.length];

		boolean isSelected = false;

		// find the tour which has the same tourId as the selected tour
		for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
			if ((tourIds[tourIndex] == tourId)) {
				selectedTours[tourIndex] = true;
				isSelected = true;
				fSelectedTourId = tourId;
				break;
			}
		}

		if (isSelected == false) {
			// select first tour
			selectedTours[0] = true;
			fSelectedTourId = tourIds[0];
		}

		fChart.setSelectedBars(selectedTours);

		return isSelected;
	}

	private void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		final IChartInfoProvider chartInfoProvider = new IChartInfoProvider() {

			public ChartToolTipInfo getToolTipInfo(final int serieIndex, int valueIndex) {

				final int[] tourDOYValues = fTourTimeData.fTourDOYValues;

				if (valueIndex >= tourDOYValues.length) {
					valueIndex -= tourDOYValues.length;
				}

				if (tourDOYValues == null || valueIndex >= tourDOYValues.length) {
					return null;
				}

				/*
				 * set calendar day/month/year
				 */
				final int oldestYear = fCurrentYear - fNumberOfYears + 1;
				final int tourDOY = tourDOYValues[valueIndex];
				fCalendar.set(oldestYear, 0, 1);
				fCalendar.set(Calendar.DAY_OF_YEAR, tourDOY + 1);
				final String beginDate = fDateFormatter.format(fCalendar.getTime());

				fCurrentMonth = fCalendar.get(Calendar.MONTH) + 1;
				fSelectedTourId = fTourTimeData.fTourIds[valueIndex];

				final String tourTypeName = TourDatabase.getTourTypeName(fTourTimeData.fTypeIds[valueIndex]);
				final String tourTags = TourDatabase.getTagNames(fTourTimeData.fTagIds.get(fSelectedTourId));
				final String tourDescription = fTourTimeData.tourDescription.get(valueIndex)
						.replace(UI.SYSTEM_NEW_LINE, UI.NEW_LINE);

				final int[] startValue = fTourTimeData.fTourTimeStartValues;
				final int[] endValue = fTourTimeData.fTourTimeEndValues;

				final Integer recordingTime = fTourTimeData.fTourRecordingTimeValues.get(valueIndex);
				final Integer drivingTime = fTourTimeData.fTourDrivingTimeValues.get(valueIndex);
				final int breakTime = recordingTime - drivingTime;

				final StringBuilder toolTipFormat = new StringBuilder();
				toolTipFormat.append(Messages.tourtime_info_date_day);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_distance);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_altitude);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_time);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_recording_time);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_driving_time);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_break_time);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_tour_type);
				toolTipFormat.append(NEW_LINE);
				toolTipFormat.append(Messages.tourtime_info_tags);

				if (tourDescription.length() > 0) {
					toolTipFormat.append(NEW_LINE);
					toolTipFormat.append(NEW_LINE);
					toolTipFormat.append(Messages.tourtime_info_description);
					toolTipFormat.append(NEW_LINE);
					toolTipFormat.append(Messages.tourtime_info_description_text);
				}

				final String toolTipLabel = new Formatter().format(toolTipFormat.toString(),
				//
						beginDate,
						//
						fTourTimeData.fTourDistanceValues[valueIndex],
						UI.UNIT_LABEL_DISTANCE,
						//
						fTourTimeData.fTourAltitudeValues[valueIndex],
						UI.UNIT_LABEL_ALTITUDE,
						//
						startValue[valueIndex] / 3600,
						(startValue[valueIndex] % 3600) / 60,
						endValue[valueIndex] / 3600,
						(endValue[valueIndex] % 3600) / 60,
						//
						recordingTime / 3600,
						(recordingTime % 3600) / 60,
						//
						drivingTime / 3600,
						(drivingTime % 3600) / 60,
						//
						breakTime / 3600,
						(breakTime % 3600) / 60,
						//					
						tourTypeName,
						tourTags,
						//
						tourDescription
//
				)
						.toString();

				/*
				 * create tool tip info
				 */
				String tourTitle = fTourTimeData.fTourTitle.get(valueIndex);
				if (tourTitle == null || tourTitle.trim().length() == 0) {
					tourTitle = tourTypeName;
				}

				final ChartToolTipInfo toolTipInfo = new ChartToolTipInfo();
				toolTipInfo.setTitle(tourTitle);
				toolTipInfo.setLabel(toolTipLabel);
//				toolTipInfo.setLabel(toolTipFormat.toString());

				return toolTipInfo;
			}
		};

		chartModel.setCustomData(ChartDataModel.BAR_TOOLTIP_INFO_PROVIDER, chartInfoProvider);

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER, new TourContextProvider(fChart, this));
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void updateChart(final long selectedTourId) {

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		final ChartDataXSerie xData = new ChartDataXSerie(fTourTimeData.fTourDOYValues);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_DAY);
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
		StatisticServices.setTourTypeColors(yData, GraphColorProvider.PREF_GRAPH_TIME, fActiveTourTypeFiler);
		StatisticServices.setDefaultColors(yData, GraphColorProvider.PREF_GRAPH_TIME);

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

		// set grid size
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		fChart.setGridDistance(prefStore.getInt(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE),
				prefStore.getInt(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE));

		// show the data in the chart
		fChart.updateChart(chartModel, false);

		// try to select the previous selected tour
		selectTour(selectedTourId);
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}

}
