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
package net.tourbook.statistic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.tourbook.Messages;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.part.PageBook;

public class StatisticContainer extends Composite {

	private static final String				MEMENTO_SELECTED_STATISTIC	= "statistic.container.selected_statistic"; //$NON-NLS-1$
	private static final String				MEMENTO_NUMBER_OF_YEARS		= "statistic.container.number_of_years";	//$NON-NLS-1$

	private Calendar						fCalendar					= GregorianCalendar.getInstance();

	private TourbookStatistic				fActiveStatistic;
	private int								fActiveYear					= -1;

	private TourPerson						fActivePerson;
	private TourTypeFilter					fActiveTourTypeFilter;

	private Combo							fComboYear;
	private Combo							fComboStatistics;
	private Combo							fComboNumberOfYears;
	private PageBook						fPageBookStatistic;
	private Composite						fStatContainer;

	private IViewSite						fViewSite;
	private ToolBarManager					fTBM;
	private ToolBar							fToolBar;

	private ArrayList<Integer>				fTourYears;
	private ArrayList<TourbookStatistic>	fStatistics;

	private ActionSynchChartScale			fActionSynchChartScale;
	private boolean							fIsSynchScaleEnabled;

	private long							fSelectedDate				= -1;
	private long							fSelectedMonth				= -1;
	private long							fSelectedTourId				= -1;

//	private int								fLastSelectionType;
	private IPostSelectionProvider			fPostSelectionProvider;

	public StatisticContainer(	final IViewSite viewSite,
								final IPostSelectionProvider selectionProvider,
								final Composite parent,
								final int style) {

		super(parent, style);

		fViewSite = viewSite;
		fPostSelectionProvider = selectionProvider;

		createUI();

		addDisposeListener(new DisposeListener() {
			public void widgetDisposed(final DisposeEvent e) {

				// dispose all statistic resources
				for (final TourbookStatistic statistic : fStatistics) {
					statistic.dispose();
				}
			}
		});
	}

	void actionSynchScale(final boolean isEnabled) {

		fIsSynchScaleEnabled = isEnabled;
		fActiveStatistic.setSynchScale(fIsSynchScaleEnabled);

		if (fActiveStatistic instanceof IYearStatistic) {
			((IYearStatistic) fActiveStatistic).refreshStatistic(fActivePerson,
					fActiveTourTypeFilter,
					fActiveYear,
					getNumberOfYears(),
					false);
		}
	}

	public void activateActions(final IWorkbenchPartSite partSite) {

		if (fActiveStatistic == null) {
			return;
		}

		fActiveStatistic.activateActions(partSite);
	}

	private void createActions(final Composite parent) {

		fActionSynchChartScale = new ActionSynchChartScale(this);

		GridData gd;

		// create the toolbar manager
		fToolBar = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		gd.horizontalAlignment = SWT.END;
		fToolBar.setLayoutData(gd);

		fTBM = new ToolBarManager(fToolBar);
	}

	private void createUI() {

		GridLayout gl;

		gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		setLayout(gl);

		/*
		 * container: statistic combo
		 */
		fStatContainer = new Composite(this, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(fStatContainer);
		GridLayoutFactory.fillDefaults().numColumns(5).extendedMargins(0, 0, 1, 0).applyTo(fStatContainer);

		// combo: year
		fComboYear = new Combo(fStatContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboYear.setVisibleItemCount(10);
		fComboYear.setLayoutData(new GridData());
		fComboYear.setToolTipText(Messages.Tour_Book_Combo_year_tooltip);
		fComboYear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectYear();
			}
		});

		// combo: statistics
		fComboStatistics = new Combo(fStatContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboStatistics.setVisibleItemCount(20);
		fComboStatistics.setToolTipText(Messages.Tour_Book_Combo_statistic_tooltip);
		fComboStatistics.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectStatistic();
			}
		});

		// fill combobox with statistic names
		for (final TourbookStatistic statistic : getAllStatistics()) {
			fComboStatistics.add(statistic.fVisibleName);
		}

		/*
		 * number of years
		 */
		final Label label = new Label(fStatContainer, SWT.NONE);
		label.setText(Messages.tour_statistic_label_years);

		// combo: year numbers
		fComboNumberOfYears = new Combo(fStatContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboNumberOfYears.setToolTipText(Messages.tour_statistic_number_of_years);
		fComboNumberOfYears.setVisibleItemCount(20);
		fComboNumberOfYears.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelectYear();
			}
		});

		// fill combobox with number of years
		for (int years = 1; years < 21; years++) {
			fComboNumberOfYears.add(Integer.toString(years));
		}

		// refreshYears();
		createActions(fStatContainer);

		// pagebook: statistics
		fPageBookStatistic = new PageBook(this, SWT.NONE);
		fPageBookStatistic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	public void deactivateActions(final IWorkbenchPartSite partSite) {
		if (fActiveStatistic != null) {
			fActiveStatistic.deactivateActions(partSite);
		}
	}

	private void fillToolbar() {

		final IToolBarManager tbm = fViewSite.getActionBars().getToolBarManager();
//		// update the toolbar
		tbm.removeAll();

		tbm.add(fActionSynchChartScale);

		fActiveStatistic.updateToolBar(true);

		fTBM.update(false);
		fStatContainer.layout();
	}

	/**
	 * @return Returns the index for the active year or <code>-1</code> when there are no years
	 *         available
	 */
	private int getActiveYearComboboxIndex() {

		int selectedYearIndex = -1;

		if (fTourYears == null) {
			return selectedYearIndex;
		}

		int yearIndex = 0;
		for (final Integer year : fTourYears) {
			if (year == fActiveYear) {
				selectedYearIndex = yearIndex;
				break;
			}
			yearIndex++;
		}

		return selectedYearIndex;
	}

	/**
	 * @return Returns all statistic plugins
	 */
	private ArrayList<TourbookStatistic> getAllStatistics() {
		if (fStatistics == null) {
			readStatistics();
		}
		return fStatistics;
	}

	private int getNumberOfYears() {
		// get number of years
		int numberOfYears = 1;
		final int selectedIndex = fComboNumberOfYears.getSelectionIndex();
		if (selectedIndex != -1) {
			numberOfYears = selectedIndex + 1;
		}
		return numberOfYears;
	}

	/**
	 * @return Returns the selected statistic in the combo box or <code>null</code> when a statistic
	 *         is not available
	 */
	private TourbookStatistic getSelectedStatistic() {

		// get selected statistic
		final int selectedIndex = fComboStatistics.getSelectionIndex();
		if (selectedIndex == -1) {
			return null;
		}

		final TourbookStatistic statistic = fStatistics.get(selectedIndex);

		// get statistic container
		Composite statControlContainer = statistic.getControl();
		if (statControlContainer == null) {

			// create statistic control
			statControlContainer = new Composite(fPageBookStatistic, SWT.NONE);
			final GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			statControlContainer.setLayout(gl);
			statControlContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			statistic.createControl(statControlContainer, fViewSite, fPostSelectionProvider);
			statistic.setContainer(statControlContainer);

			final Composite statControl = statistic.getControl();
			statControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

		return statistic;
	}

	private void onSelectStatistic() {

		fActiveStatistic = getSelectedStatistic();
		if (fActiveStatistic == null) {
			return;
		}

		refreshStatistic(fActivePerson, fActiveTourTypeFilter, fActiveYear, false);

		if (fActiveStatistic.canSelectTour()) {
//			selectTour(fSelectedTourId);
		}

//		// reselect data
//		switch (fLastSelectionType) {
//		case SELECTION_TYPE_MONTH:
//			selectMonth(fSelectedMonth);
//			break;
//
//		case SELECTION_TYPE_DAY:
//			selectDay(fSelectedDate);
//			break;
//
//		case SELECTION_TYPE_TOUR:
//			if (selectTour(fSelectedTourId)) {
////						fTourChartViewer.showTourChart(true);
//			} else {
//				// a tour was not selected, hide the tour chart
////						fTourChartViewer.showTourChart(-1);
//			}
//			break;
//		}
	}

	private void onSelectYear() {

		final int selectedItem = fComboYear.getSelectionIndex();

		if (selectedItem != -1) {

			fActiveYear = Integer.parseInt(fComboYear.getItem(selectedItem));

			refreshStatistic(fActivePerson, fActiveTourTypeFilter, fActiveYear, false);
		}
	}

	/**
	 * read statistics from the extension registry
	 */
	private void readStatistics() {

		fStatistics = new ArrayList<TourbookStatistic>();

		final IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(TourbookPlugin.PLUGIN_ID,
				TourbookPlugin.EXT_POINT_STATISTIC_YEAR);

		if (extPoint != null) {

			for (final IExtension extension : extPoint.getExtensions()) {

				for (final IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("statistic")) { //$NON-NLS-1$

						Object object;
						try {
							object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof TourbookStatistic) {

								final TourbookStatistic yearStatistic = (TourbookStatistic) object;

								yearStatistic.fVisibleName = configElement.getAttribute("name"); //$NON-NLS-1$
								yearStatistic.fStatisticId = configElement.getAttribute("id"); //$NON-NLS-1$

								fStatistics.add(yearStatistic);
							}
						} catch (final CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * update all statistics which have been created because person or tour type can be changed
	 * 
	 * @param person
	 */
	public void refreshStatistic(final TourPerson person, final TourTypeFilter tourTypeFilter) {

		fActiveStatistic = getSelectedStatistic();
		if (fActiveStatistic == null) {
			return;
		}

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;

		refreshYearCombobox();
		selectYear();

		// tell all existing statistics the data have changed
		for (final TourbookStatistic statistic : fStatistics) {

			if (statistic.getControl() != null) {
				if (statistic instanceof IYearStatistic) {

					statistic.setSynchScale(fIsSynchScaleEnabled);
					statistic.setDataDirty();
				}
			}
		}

		// refresh current statistic
		((IYearStatistic) fActiveStatistic).refreshStatistic(fActivePerson,
				fActiveTourTypeFilter,
				fActiveYear,
				getNumberOfYears(),
				true);

//		resetSelection();
	}

	private void refreshStatistic(	final TourPerson person,
									final TourTypeFilter activeTourTypeFilter,
									final int selectedYear,
									final boolean refreshData) {

		fActivePerson = person;
		fActiveTourTypeFilter = activeTourTypeFilter;

		// keep current year
		if (selectedYear == -1) {
			return;
		}
		fActiveYear = selectedYear;

		fActiveStatistic = getSelectedStatistic();
		if (fActiveStatistic == null) {
			return;
		}

		selectYear();
		fActiveStatistic.setSynchScale(fIsSynchScaleEnabled);

		if (fActiveStatistic instanceof IYearStatistic) {
			((IYearStatistic) fActiveStatistic).refreshStatistic(fActivePerson,
					fActiveTourTypeFilter,
					selectedYear,
					getNumberOfYears(),
					refreshData);
		}
		fillToolbar();
		fPageBookStatistic.showPage(fActiveStatistic.getControl());
	}

	/**
	 * create the year list for all tours and fill the year combobox with the available years
	 */
	private void refreshYearCombobox() {

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT " // //$NON-NLS-1$
				+ "startYear " //$NON-NLS-1$

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA //$NON-NLS-1$ 

				+ " WHERE 1=1 " //$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ " GROUP BY STARTYEAR ORDER BY STARTYEAR"; //$NON-NLS-1$

		fTourYears = new ArrayList<Integer>();

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				fTourYears.add(result.getInt(1));
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		fComboYear.removeAll();

		/*
		 * add all years of the tours and the current year
		 */
		fCalendar.setTime(new Date());
		final int thisYear = fCalendar.get(Calendar.YEAR);

		boolean isThisYearSet = false;

		for (final Integer year : fTourYears) {

			if (year.intValue() == thisYear) {
				isThisYearSet = true;
			}

			fComboYear.add(year.toString());
		}

		// add currenty year if not set
		if (isThisYearSet == false) {
			fTourYears.add(thisYear);
			fComboYear.add(Integer.toString(thisYear));
		}
	}

//	private void resetSelection() {
//
//		if (fActiveStatistic == null) {
//			return;
//		}
//
//		// reset selection
//		fSelectedDate = -1;
//		fSelectedMonth = -1;
//		fSelectedTourId = -1;
//
//		fActiveStatistic.resetSelection();
//	}

	/**
	 * Restore selected statistic
	 * 
	 * @param memento
	 * @param activeTourTypeFilter
	 * @param activePerson
	 */
	public void restoreStatistics(	final IMemento memento,
									final TourPerson activePerson,
									final TourTypeFilter activeTourTypeFilter) {

		fActivePerson = activePerson;
		fActiveTourTypeFilter = activeTourTypeFilter;

		int prevStatIndex = 0;

		if (memento != null) {

			/*
			 * select statistic
			 */

			final String mementoStatisticId = memento.getString(MEMENTO_SELECTED_STATISTIC);
			if (mementoStatisticId != null) {
				int statIndex = 0;
				for (final TourbookStatistic statistic : getAllStatistics()) {
					if (mementoStatisticId.equalsIgnoreCase(statistic.fStatisticId)) {
						prevStatIndex = statIndex;
						break;
					}
					statIndex++;
				}
			}

			/*
			 * number of years
			 */
			final Integer numberOfYears = memento.getInteger(MEMENTO_NUMBER_OF_YEARS);
			if (numberOfYears != null) {
				fComboNumberOfYears.select(numberOfYears);
			} else {
				fComboNumberOfYears.select(0);
			}

		} else {
			fComboNumberOfYears.select(0);
		}

		// select year
		refreshYearCombobox();
		selectYear();

		// select statistic item
		fComboStatistics.select(prevStatIndex);
		onSelectStatistic();

		// restore statistic state (e.g. reselect previous selection)
		if (memento != null) {
			getAllStatistics().get(prevStatIndex).restoreState(memento);
		}
	}

	/**
	 * save statistic
	 */
	public void saveState(final IMemento memento) {

		// keep statistic id for the selected statistic
		final int selectionIndex = fComboStatistics.getSelectionIndex();
		if (selectionIndex != -1) {

			final TourbookStatistic tourbookStatistic = getAllStatistics().get(selectionIndex);

			memento.putString(MEMENTO_SELECTED_STATISTIC, tourbookStatistic.fStatisticId);

			tourbookStatistic.saveState(memento);
		}

		memento.putInteger(MEMENTO_NUMBER_OF_YEARS, fComboNumberOfYears.getSelectionIndex());
	}

	/**
	 * @param date
	 */
	private void selectDay(final long date) {

//		fLastSelectionType = SELECTION_TYPE_DAY;

		fSelectedDate = date;
		fActiveStatistic.selectDay(date);
	}

	/**
	 * @param date
	 *            contains the date value in milliseconds
	 */
	private void selectMonth(final long date) {

		fSelectedMonth = date;
		fActiveStatistic.selectMonth(date);
	}

	private boolean selectTour(final Long tourId) {

		final boolean isTourSelected = fActiveStatistic.selectTour(tourId);

		if (isTourSelected) {
			fSelectedTourId = tourId;
		}

		return isTourSelected;
	}

	private void selectYear() {

		int selectedYearIndex = getActiveYearComboboxIndex();

		if (selectedYearIndex == -1) {
			/*
			 * the active year was not found in the combo box, it's possible that the combo box
			 * needs to be update
			 */
			refreshYearCombobox();
			selectedYearIndex = getActiveYearComboboxIndex();

			if (selectedYearIndex == -1) {

				// year is still not selected
				final int yearCount = fComboYear.getItemCount();

				// reselect the youngest year if years are available
				if (yearCount > 0) {
					selectedYearIndex = yearCount - 1;
					fActiveYear = Integer.parseInt(fComboYear.getItem(yearCount - 1));
				}
			}
		}

		fComboYear.select(selectedYearIndex);
	}

	@Override
	public boolean setFocus() {
		return super.setFocus();
	}
}
