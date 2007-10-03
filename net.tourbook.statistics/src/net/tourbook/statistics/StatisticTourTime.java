/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.Iterator;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.colors.GraphColors;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;

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
	private long						fActiveTypeId;
	private int							fCurrentYear;

	private final Calendar				fCalendar		= GregorianCalendar.getInstance();

	private Chart						fChart;
	private TourDataTime				fTourTimeData;

	private final BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();
	private boolean						fIsSynchScaleEnabled;

	private IPostSelectionProvider		fPostSelectionProvider;

	private Long						fSelectedTourId;
	protected int						fCurrentMonth;

//	private IContextActivation			fContextBarChart;

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
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				long selectedTourId = fTourTimeData.fTourIds[valueIndex];
				ProviderTourTime.getInstance().setSelectedTourId(selectedTourId);
				fPostSelectionProvider.setSelection(new SelectionTourId(selectedTourId));
			}
		});

		/*
		 * open tour with double click on the tour bar
		 */
		fChart.addDoubleClickListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				long selectedTourId = fTourTimeData.fTourIds[valueIndex];
				ProviderTourTime.getInstance().setSelectedTourId(selectedTourId);
				TourManager.getInstance().openTourInEditor(selectedTourId);
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
		refreshStatistic(fActivePerson, fActiveTypeId, fCurrentYear, false);
	}

	public void refreshStatistic(	final TourPerson person,
									final long type,
									final int year,
									final boolean refreshData) {

		// reset the selection in the chart when the data have changed
		final boolean isResetSelection = fActivePerson != person
				|| fActiveTypeId != type
				|| fCurrentYear != year;

		fActivePerson = person;
		fActiveTypeId = type;
		fCurrentYear = year;

		fTourTimeData = ProviderTourTime.getInstance().getTourTimeData(person,
				type,
				year,
				isRefreshDataWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(isResetSelection);
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

		if (isSelected) {
			fChart.setSelectedBars(selectedTours);
		}

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

				/*
				 * get tour type name
				 */
				final long typeId = fTourTimeData.fTypeIds[valueIndex];
				final ArrayList<TourType> tourTypes = TourbookPlugin.getDefault().getTourTypes();

				String tourTypeName = ""; //$NON-NLS-1$
				for (final Iterator<TourType> iter = tourTypes.iterator(); iter.hasNext();) {
					final TourType tourType = iter.next();
					if (tourType.getTypeId() == typeId) {
						tourTypeName = tourType.getName();
					}
				}
				final int[] startValue = fTourTimeData.fTourTimeStartValues;
				final int[] endValue = fTourTimeData.fTourTimeEndValues;
				final int[] durationValue = fTourTimeData.fTourTimeDurationValues;

				final String barInfo = new Formatter().format(Messages.TOURTIMEINFO_DATE_FORMAT
						+ Messages.TOURTIMEINFO_DISTANCE
						+ Messages.TOURTIMEINFO_ALTITUDE
						+ Messages.TOURTIMEINFO_DURATION
						+ Messages.TOURTIMEINFO_TOUR_TYPE,
						fCalendar.get(Calendar.DAY_OF_MONTH),
						fCalendar.get(Calendar.MONTH) + 1,
						fCalendar.get(Calendar.YEAR),
						startValue[valueIndex] / 3600,
						(startValue[valueIndex] % 3600) / 60,
						endValue[valueIndex] / 3600,
						(endValue[valueIndex] % 3600) / 60,
						fTourTimeData.fTourTimeDistanceValues[valueIndex],
						fTourTimeData.fTourTimeAltitudeValues[valueIndex],
						durationValue[valueIndex] / 3600,
						(durationValue[valueIndex] % 3600) / 60,
						tourTypeName).toString();

				return barInfo;
			}
		};

		chartModel.setCustomData(ChartDataModel.BAR_INFO_PROVIDER, chartInfoProvider);

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER,
				new TourContextProvider(fChart, this));
	}

	@Override
	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	private void updateChart(final boolean isResetSelection) {

		final ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		final ChartDataXSerie xData = new ChartDataXSerie(fTourTimeData.fTourDOYValues);

		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_YEAR);
		xData.setVisibleMaxValue(fCurrentYear);
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
		StatisticServices.setTourTypeColors(yData, GraphColors.PREF_GRAPH_TIME);

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
		fChart.updateChart(chartModel, isResetSelection);
	}

	@Override
	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}

}
