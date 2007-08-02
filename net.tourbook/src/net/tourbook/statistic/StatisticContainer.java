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
package net.tourbook.statistic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.part.PageBook;

public class StatisticContainer extends Composite {

	private static final String				MEMENTO_SELECTED_STATISTIC	= "tourbookview.selected.statistic";	//$NON-NLS-1$

	private static final int				SELECTION_TYPE_MONTH		= 1;
	private static final int				SELECTION_TYPE_DAY			= 2;
	private static final int				SELECTION_TYPE_TOUR			= 3;

	private TourbookStatistic				fActiveStatistic;
	private int								fActiveYear					= -1;

	private TourPerson						fActivePerson;
	private long							fActiveTypeId;

	private Combo							fComboYear;
	private Combo							fComboStatistics;
	private PageBook						fPageBookStatistic;
	private Composite						fStatContainer;

//	private ToolBarManager					fTBM;
//	private ToolBar							fToolBar;

	private ArrayList<Integer>				fTourYears;
	private ArrayList<TourbookStatistic>	fStatistics;

//	private ActionSynchChartScale			fActionZoomFitGraph;
	private boolean							fIsSynchScaleEnabled;

	private long							fSelectedDate;
	private long							fSelectedMonth;
	private long							fSelectedTourId;

	private int								fLastSelectionType;
	private IPostSelectionProvider			fPostSelectionProvider;

	private IActionBars						fActionBars;

	public StatisticContainer(IActionBars actionBars, IPostSelectionProvider selectionProvider,
			Composite parent, int style) {

		super(parent, style);

		fPostSelectionProvider = selectionProvider;
		fActionBars = actionBars;

		createControl();
	}

	void actionSynchScale(boolean isEnabled) {

		fIsSynchScaleEnabled = isEnabled;
		fActiveStatistic.setSynchScale(fIsSynchScaleEnabled);

		if (fActiveStatistic instanceof IYearStatistic) {
			((IYearStatistic) fActiveStatistic).refreshStatistic(fActivePerson,
					fActiveTypeId,
					fActiveYear,
					false);
		}
	}

//	private void createActions(Composite parent) {
//
//		GridData gd;
//
////		fActionZoomFitGraph = new ActionSynchChartScale(this);
//
//		// create the toolbar manager
//		fToolBar = new ToolBar(parent, SWT.FLAT | SWT.WRAP | SWT.RIGHT);
//		gd = new GridData(SWT.FILL, SWT.NONE, true, false);
//		gd.horizontalAlignment = SWT.END;
//		fToolBar.setLayoutData(gd);
//
//		fTBM = new ToolBarManager(fToolBar);
//	}

	private void createControl() {

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
		GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
		fStatContainer.setLayoutData(gd);

		// fStatContainer.setBackground(Display
		// .getCurrent()
		// .getSystemColor(SWT.COLOR_YELLOW));

		gl = new GridLayout(4, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.marginTop = 1;
		gl.marginBottom = 1;
		gl.verticalSpacing = 0;
		fStatContainer.setLayout(gl);

		// combo: year
		fComboYear = new Combo(fStatContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboYear.setVisibleItemCount(10);
		fComboYear.setLayoutData(new GridData());
		fComboYear.setToolTipText(Messages.TourBook_Combo_year_tooltip);
		fComboYear.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onSelectYear();
			}
		});

		// combo: statistics
		fComboStatistics = new Combo(fStatContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
		fComboStatistics.setVisibleItemCount(20);
		fComboStatistics.setToolTipText(Messages.TourBook_Combo_statistic_tooltip);
		fComboStatistics.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				onSelectStatistic();
			}
		});

		// fill combobox with statistic names
		for (TourbookStatistic statistic : getStatistics()) {
			fComboStatistics.add(statistic.fVisibleName);
		}

		// refreshYears();
//		createActions(fStatContainer);

		// pagebook: statistics
		fPageBookStatistic = new PageBook(this, SWT.NONE);
		fPageBookStatistic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

//	private void fillToolbar() {
//
//		// update the toolbar
//		fTBM.removeAll();
//
//		fTBM.add(fActionZoomFitGraph);
//		fActiveStatistic.updateToolBar(false);
//
//		fTBM.update(false);
//		fStatContainer.layout();
//	}

	/**
	 * Get the selected statistic in the combo box and set it as active statistic
	 */
	private void getActiveStatistic() {

		// get selected statistic
		int selectedIndex = fComboStatistics.getSelectionIndex();
		if (selectedIndex == -1) {
			fActiveStatistic = null;
			return;
		}

		TourbookStatistic statistic = fStatistics.get(selectedIndex);

		// get statistic container
		Composite statControlContainer = statistic.getControl();
		if (statControlContainer == null) {

			// create statistic control
			statControlContainer = new Composite(fPageBookStatistic, SWT.NONE);
			GridLayout gl = new GridLayout();
			gl.marginHeight = 0;
			gl.marginWidth = 0;
			statControlContainer.setLayout(gl);
			statControlContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			statistic.createControl(statControlContainer, fActionBars, fPostSelectionProvider);
			statistic.setContainer(statControlContainer);

			Composite statControl = statistic.getControl();
			statControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}

		fActiveStatistic = statistic;
	}

	public int getActiveYear() {
		return fActiveYear;
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
		for (Integer year : fTourYears) {
			if (year == fActiveYear) {
				selectedYearIndex = yearIndex;
				break;
			}
			yearIndex++;
		}
		return selectedYearIndex;
	}

	private String getSQLFilter(TourPerson person, long typeId) {

		String sqlPerson = person == null ? "" : " AND tourPerson_personId = " //$NON-NLS-1$ //$NON-NLS-2$
				+ Long.toString(person.getPersonId());

		String sqlType = typeId == TourType.TOUR_TYPE_ID_ALL ? "" //$NON-NLS-1$
				: typeId == TourType.TOUR_TYPE_ID_NOT_DEFINED ? " AND tourType_typeId is null" //$NON-NLS-1$
						: " AND tourType_typeId =" + Long.toString(typeId); //$NON-NLS-1$

		return sqlPerson + sqlType;
	}

	private ArrayList<TourbookStatistic> getStatistics() {
		if (fStatistics == null) {
			readStatistics();
		}
		return fStatistics;
	}

	private void onSelectStatistic() {

		getActiveStatistic();
		if (fActiveStatistic == null) {
			return;
		}

		refreshStatistic(fActivePerson, fActiveTypeId, fActiveYear, false);

		// reselect data
		switch (fLastSelectionType) {
		case SELECTION_TYPE_MONTH:
			selectMonth(fSelectedMonth);
			break;

		case SELECTION_TYPE_DAY:
			selectDay(fSelectedDate);
			break;

		case SELECTION_TYPE_TOUR:
			if (selectTour(fSelectedTourId)) {
//						fTourChartViewer.showTourChart(true);
			} else {
				// a tour was not selected, hide the tour chart
//						fTourChartViewer.showTourChart(-1);
			}
			break;
		}
	}

	private void onSelectYear() {

		final int selectedItem = fComboYear.getSelectionIndex();

		if (selectedItem != -1) {

			fActiveYear = Integer.parseInt(fComboYear.getItem(selectedItem));

			refreshStatistic(fActivePerson, fActiveTypeId, fActiveYear, false);
		}
	}

	/**
	 * read statistics from the extension registry
	 */
	private void readStatistics() {

		fStatistics = new ArrayList<TourbookStatistic>();

		IExtensionPoint extPoint = Platform.getExtensionRegistry()
				.getExtensionPoint(TourbookPlugin.PLUGIN_ID,
						TourbookPlugin.EXT_POINT_STATISTIC_YEAR);

		if (extPoint != null) {

			for (IExtension extension : extPoint.getExtensions()) {

				for (IConfigurationElement configElement : extension.getConfigurationElements()) {

					if (configElement.getName().equalsIgnoreCase("statistic")) { //$NON-NLS-1$

						Object object;
						try {
							object = configElement.createExecutableExtension("class"); //$NON-NLS-1$
							if (object instanceof TourbookStatistic) {

								TourbookStatistic yearStatistic = (TourbookStatistic) object;

								yearStatistic.fVisibleName = configElement.getAttribute("name"); //$NON-NLS-1$
								yearStatistic.fStatisticId = configElement.getAttribute("id"); //$NON-NLS-1$

								fStatistics.add(yearStatistic);
							}
						} catch (CoreException e) {
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
	public void refreshStatistic(TourPerson person, long typeId) {

		getActiveStatistic();

		if (fActiveStatistic == null) {
			return;
		}

		fActivePerson = person;
		fActiveTypeId = typeId;

		refreshYearCombobox();
		selectActiveYear();

		// tell all existing statistics the data have changed
		for (Iterator<TourbookStatistic> iter = fStatistics.iterator(); iter.hasNext();) {

			TourbookStatistic statistic = (TourbookStatistic) iter.next();

			if (statistic.getControl() != null) {
				if (statistic instanceof IYearStatistic) {

					statistic.setSynchScale(fIsSynchScaleEnabled);
					statistic.setRefreshData(true);
				}
			}
		}

		// refresh current statistic
		((IYearStatistic) fActiveStatistic).refreshStatistic(fActivePerson,
				fActiveTypeId,
				fActiveYear,
				true);

		resetSelection();
	}

	public void refreshStatistic(TourPerson person, long typeId, int year, boolean refreshData) {

		fActivePerson = person;
		fActiveTypeId = typeId;

		// keep current year
		if (year == -1) {
			return;
		}
		fActiveYear = year;

		getActiveStatistic();
		selectActiveYear();

		if (fActiveStatistic == null) {
			return;
		}

		fActiveStatistic.setSynchScale(fIsSynchScaleEnabled);

		if (fActiveStatistic instanceof IYearStatistic) {
			((IYearStatistic) fActiveStatistic).refreshStatistic(fActivePerson,
					fActiveTypeId,
					year,
					refreshData);
		}
//		fillToolbar();
		fPageBookStatistic.showPage(fActiveStatistic.getControl());
	}

	/**
	 * create the year list for all tours and fill the year combobox with the available years
	 */
	private void refreshYearCombobox() {

		String sqlString = "SELECT STARTYEAR \n" //$NON-NLS-1$
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE 1=1 " + getSQLFilter(fActivePerson, fActiveTypeId)) //$NON-NLS-1$
				+ (" GROUP BY STARTYEAR ORDER BY STARTYEAR"); //$NON-NLS-1$

		fTourYears = new ArrayList<Integer>();

		try {
			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				fTourYears.add(result.getInt(1));
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

		fComboYear.removeAll();
		for (Integer year : fTourYears) {
			fComboYear.add(year.toString());
		}
	}

	private void resetSelection() {

		if (fActiveStatistic == null) {
			return;
		}

		// reset selection
		fSelectedDate = -1;
		fSelectedMonth = -1;
		fSelectedTourId = -1;

		fActiveStatistic.resetSelection();
	}

	/**
	 * Restore selected statistic
	 * 
	 * @param memento
	 * @param activeTourTypeId
	 * @param activePerson
	 */
	public void restoreStatistics(IMemento memento, TourPerson activePerson, long activeTourTypeId) {

		fActivePerson = activePerson;
		fActiveTypeId = activeTourTypeId;

		int previousStatistic = 0;

		if (memento != null) {

			/*
			 * get previous statistic
			 */

			final String mementoStatisticId = memento.getString(MEMENTO_SELECTED_STATISTIC);
			if (mementoStatisticId != null) {
				int statisticIndex = 0;
				for (TourbookStatistic statistic : getStatistics()) {
					if (mementoStatisticId.equalsIgnoreCase(statistic.fStatisticId)) {
						previousStatistic = statisticIndex;
						break;
					}
					statisticIndex++;
				}
			}
		}

		// select year
		refreshYearCombobox();
		selectActiveYear();

		// select statistic item
		fComboStatistics.select(previousStatistic);

		onSelectStatistic();
	}

	/**
	 * save statistic
	 */
	public void saveState(IMemento memento) {

		// keep statistic id for the selected statistic
		int selectionIndex = fComboStatistics.getSelectionIndex();
		if (selectionIndex != -1) {
			memento.putString(MEMENTO_SELECTED_STATISTIC,
					getStatistics().get(selectionIndex).fStatisticId);
		}

	}

	private void selectActiveYear() {

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
				int yearCount = fComboYear.getItemCount();

				// reselect the youngest year if years are available
				if (yearCount > 0) {
					selectedYearIndex = yearCount - 1;
					fActiveYear = Integer.parseInt(fComboYear.getItem(yearCount - 1));
				}
			}
		}

		fComboYear.select(selectedYearIndex);
	}

	/**
	 * @param date
	 */
	public void selectDay(long date) {

		fLastSelectionType = SELECTION_TYPE_DAY;

		fSelectedDate = date;
		fActiveStatistic.selectDay(date);
	}

	/**
	 * @param date
	 *        contains the date value in milliseconds
	 */
	public void selectMonth(long date) {

		fLastSelectionType = SELECTION_TYPE_MONTH;

		fSelectedMonth = date;
		fActiveStatistic.selectMonth(date);
	}

	public boolean selectTour(Long tourId) {

		boolean isTourSelected = fActiveStatistic.selectTour(tourId);

		if (isTourSelected) {
			fLastSelectionType = SELECTION_TYPE_TOUR;
			fSelectedTourId = tourId;
		}

		return isTourSelected;
	}

	public boolean setFocus() {
		return super.setFocus();
	}
}
