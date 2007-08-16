/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.GregorianCalendar;
import java.util.List;

import net.tourbook.chart.BarChartMinMaxKeeper;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.IBarSelectionListener;
import net.tourbook.chart.IChartInfoProvider;
import net.tourbook.chart.SelectionBarChart;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourChartViewer;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.dialogs.ListDialog;

public abstract class StatisticDay extends YearStatistic implements IBarSelectionProvider {

	static final String		DISTANCE_DATA	= "distance";						//$NON-NLS-1$
	static final String		ALTITUDE_DATA	= "altitude";						//$NON-NLS-1$
	static final String		DURATION_DATA	= "duration";						//$NON-NLS-1$

	private long			fActiveTypeId;
	private TourPerson		fActivePerson;

	Long					fSelectedTourId;

	int						fCurrentYear;
	int						fCurrentMonth;
	Calendar				fCalendar		= GregorianCalendar.getInstance();

	IPostSelectionProvider	fPostSelectionProvider;

	Chart					fChart;
	BarChartMinMaxKeeper	fMinMaxKeeper	= new BarChartMinMaxKeeper();

	private TourDataTour	fTourDataTour;

	boolean					fIsSynchScaleEnabled;

	private class Tour {

		long	tourId;
		String	time;

		public Tour(final long tourId, final String time) {
			this.tourId = tourId;
			this.time = time;
		}
	}

	public boolean canTourBeVisible() {
		return true;
	}

	public void activateActions(IWorkbenchPartSite partSite) {
		fChart.updateChartActionHandlers();
	}

	@Override
	public void deactivateActions(IWorkbenchPartSite partSite) {}

	public void createControl(	final Composite parent,
								IViewSite viewSite,
								final IPostSelectionProvider postSelectionProvider) {

		super.createControl(parent);

		fPostSelectionProvider = postSelectionProvider;

		// create statistic chart
		fChart = new Chart(parent, SWT.BORDER | SWT.FLAT);
		fChart.setShowPartNavigation(true);
		fChart.setShowZoomActions(true);
		fChart.setCanScrollZoomedChart(true);
		fChart.setToolBarManager(viewSite.getActionBars().getToolBarManager(), false);

//		fChart.createChartActionHandlers();

		fChart.addBarSelectionListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				fSelectedTourId = fTourDataTour.fTourIds[valueIndex];
				fPostSelectionProvider.setSelection(new SelectionTourId(fSelectedTourId));
			}
		});

		/*
		 * open tour with double click on the tour bar
		 */
		fChart.addDoubleClickListener(new IBarSelectionListener() {
			public void selectionChanged(final int serieIndex, final int valueIndex) {
				fSelectedTourId = fTourDataTour.fTourIds[valueIndex];
				TourManager.getInstance().openTourInEditor(fSelectedTourId);
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

							long selectedTourId = fTourDataTour.fTourIds[barChartSelection.valueIndex];
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

	void OLDopenTour(	final ITourChartViewer tourChartViewer,
						final int valueIndex,
						final int[] dayValues) {

		fCalendar.set(fCurrentYear, 0, 1);
		fCalendar.set(Calendar.DAY_OF_YEAR, dayValues[valueIndex] + 1);

		final int month = fCalendar.get(Calendar.MONTH) + 1;
		final int day = fCalendar.get(Calendar.DAY_OF_MONTH);

		final String sqlString = "SELECT " //$NON-NLS-1$
				+ "TOURID," //$NON-NLS-1$
				+ "STARTHOUR," //$NON-NLS-1$
				+ "STARTMINUTE," //$NON-NLS-1$
				+ "TOURDRIVINGTIME\n" //$NON-NLS-1$
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE STARTYEAR=" + Integer.toString(fCurrentYear)) //$NON-NLS-1$
				+ (" AND STARTMONTH=" + month) //$NON-NLS-1$
				+ (" AND STARTDAY=" + day); //$NON-NLS-1$

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			final ArrayList<Tour> tourList = new ArrayList<Tour>();

			while (result.next()) {

				final short startHour = result.getShort(2);
				final short startMinute = result.getShort(3);
				final int startTime = startHour * 3600 + startMinute * 60;
				final int endTime = startTime + result.getInt(4);

				final String tourTime = new Formatter().format(Messages.FORMAT_HHMM_HHMM,
						startTime / 3600,
						(startTime % 3600) / 60,
						endTime / 3600,
						(endTime % 3600) / 60).toString();

				tourList.add(new Tour(result.getLong(1), tourTime));
			}

			conn.close();

			// get the tour
			Long tourId;
			tourId = -1L;

			if (tourList.size() > 0) {
				if (tourList.size() == 1) {
					// there is only one tour for this day
					tourId = tourList.get(0).tourId;
				} else {

					/*
					 * there are multiple tours are available, open dialog to select the tour
					 */
					final ListDialog dialog = new ListDialog(Display.getCurrent().getActiveShell());

					dialog.setContentProvider(new IStructuredContentProvider() {
						public void dispose() {}

						public Object[] getElements(final Object inputElement) {
							return tourList.toArray();
						}

						public void inputChanged(	final Viewer viewer,
													final Object oldInput,
													final Object newInput) {}
					});

					dialog.setLabelProvider(new LabelProvider() {
						public String getText(final Object element) {
							final Tour tour = (Tour) element;
							return tour.time;
						}
					});

					dialog.setInput(tourList);
					dialog.setTitle(Messages.DLG_SELECT_TOUR_TITLE);
					dialog.setMessage(Messages.DLG_SELECT_TOUR_MSG);

					if (dialog.open() == Window.OK) {
						// get the selected tour
						final List<Object> dlgResult = Arrays.asList(dialog.getResult());
						if (result != null && dlgResult.size() == 1) {
							tourId = ((Tour) dlgResult.get(0)).tourId;
						}
					}
				}
			}

			// open the tour
			if (tourId != -1L) {
				tourChartViewer.showTourChart(tourId);
				// TourManager.getInstance().openTourInEditor(tourId);
			}

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	// public void selectDay(final Long date) {
	//
	// fCalendar.setTimeInMillis(date);
	// final int selectedDay = fCalendar.get(Calendar.DAY_OF_YEAR) - 1;
	//
	// final int[] tourDate = fTourDataDay.fDOYValues;
	// final boolean selectedItems[] = new boolean[tourDate.length];
	//
	// // find the tours which have the same day as the selected day
	// for (int tourIndex = 0; tourIndex < tourDate.length; tourIndex++) {
	// selectedItems[tourIndex] = tourDate[tourIndex] == selectedDay ? true :
	// false;
	// }
	//
	// fChart.setSelectedXData(selectedItems);
	// }

	public void prefColorChanged() {
		refreshStatistic(fActivePerson, fActiveTypeId, fCurrentYear, false);
	}

	public void refreshStatistic(	final TourPerson person,
									final long typeId,
									final int year,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTypeId = typeId;
		fCurrentYear = year;

		fTourDataTour = ProviderTourDay.getInstance().getDayData(person,
				typeId,
				year,
				isRefreshDataWithReset() || refreshData);

		// reset min/max values
		if (fIsSynchScaleEnabled == false && refreshData) {
			fMinMaxKeeper.resetMinMax();
		}

		updateChart(fTourDataTour);
	}

	public void resetSelection() {
		fChart.setSelectedBars(null);
	}

	public boolean selectMonth(final Long date) {

		fCalendar.setTimeInMillis(date);
		final int selectedMonth = fCalendar.get(Calendar.MONTH);

		final int[] tourMonths = fTourDataTour.fMonthValues;
		final boolean selectedItems[] = new boolean[tourMonths.length];

		boolean isSelected = false;
		// find the tours which have the same month as the selected month
		for (int tourIndex = 0; tourIndex < tourMonths.length; tourIndex++) {
			boolean isMonthSelected = tourMonths[tourIndex] == selectedMonth ? true : false;
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

	public boolean selectTour(Long tourId) {

		long[] tourIds = fTourDataTour.fTourIds;
		boolean selectedItems[] = new boolean[tourIds.length];
		boolean isSelected = false;

		// find the tour which has the same tourId as the selected tour
		for (int tourIndex = 0; tourIndex < tourIds.length; tourIndex++) {
			boolean isTourSelected = tourIds[tourIndex] == tourId ? true : false;
			if (isTourSelected) {
				isSelected = true;
			}
			selectedItems[tourIndex] = isTourSelected;
		}

		if (isSelected) {
			fChart.setSelectedBars(selectedItems);
		}

		return isSelected;
	}

	/**
	 * set the context menu provider and the provider for the bar info
	 * 
	 * @param chartWidget
	 * @param chartModel
	 */
	void setChartProviders(final Chart chartWidget, final ChartDataModel chartModel) {

		chartModel.setCustomData(ChartDataModel.BAR_INFO_PROVIDER, new IChartInfoProvider() {
			public String getInfo(final int serieIndex, final int valueIndex) {

				fCalendar.set(fCurrentYear, 0, 1);
				fCalendar.set(Calendar.DAY_OF_YEAR, fTourDataTour.fDOYValues[valueIndex] + 1);

				fCurrentMonth = fCalendar.get(Calendar.MONTH) + 1;
				fSelectedTourId = fTourDataTour.fTourIds[valueIndex];

				final int duration = fTourDataTour.fTimeHigh[valueIndex]
						- fTourDataTour.fTimeLow[valueIndex];

				final String barInfo = new Formatter().format(Messages.TOURDAYINFO_TOUR_DATE_FORMAT
						+ Messages.TOURDAYINFO_DISTANCE
						+ Messages.TOURDAYINFO_ALTITUDE
						+ Messages.TOURDAYINFO_DURATION,
						fCalendar.get(Calendar.DAY_OF_MONTH),
						fCalendar.get(Calendar.MONTH) + 1,
						fCalendar.get(Calendar.YEAR),
						fTourDataTour.fDistanceHigh[valueIndex],
						fTourDataTour.fAltitudeHigh[valueIndex],
						duration / 3600,
						(duration % 3600) / 60).toString();

				return barInfo;
			}
		});

		// set the menu context provider
		chartModel.setCustomData(ChartDataModel.BAR_CONTEXT_PROVIDER,
				new TourContextProvider(fChart, this));

	}

	public void setSynchScale(final boolean isSynchScaleEnabled) {
		fIsSynchScaleEnabled = isSynchScaleEnabled;
	}

	abstract void updateChart(TourDataTour tourDataDay);

	public void updateToolBar(final boolean refreshToolbar) {
		fChart.fillToolbar(refreshToolbar);
	}
}
