/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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

import gnu.trove.list.array.TIntArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;

public class StatisticView extends ViewPart implements ITourProvider {

	public static final String				ID							= "net.tourbook.statistic.StatisticView";			//$NON-NLS-1$

	private static final String				COMBO_MINIMUM_WIDTH			= "1234567890";										//$NON-NLS-1$
	private static final String				COMBO_MAXIMUM_WIDTH			= "123456789012345678901234567890";					//$NON-NLS-1$

	private static final String				STATE_SELECTED_STATISTIC	= "statistic.container.selected_statistic";			//$NON-NLS-1$
	private static final String				STATE_SELECTED_YEAR			= "statistic.container.selected-year";				//$NON-NLS-1$
	private static final String				STATE_NUMBER_OF_YEARS		= "statistic.container.number_of_years";			//$NON-NLS-1$

	private final IPreferenceStore			_prefStore					= TourbookPlugin.getPrefStore();
	private final IPreferenceStore			_prefStoreCommon			= CommonActivator.getPrefStore();
	private final IDialogSettings			_state						= TourbookPlugin.getState("TourStatisticsView");	//$NON-NLS-1$

	private final boolean					_isOSX						= net.tourbook.common.UI.IS_OSX;
	private final boolean					_isLinux					= net.tourbook.common.UI.IS_LINUX;

	private IPartListener2					_partListener;
	private IPropertyChangeListener			_prefChangeListener;
	private IPropertyChangeListener			_prefChangeListenerCommon;
	private ITourEventListener				_tourEventListener;
	private ISelectionListener				_postSelectionListener;

	private TourPerson						_activePerson;
	private TourTypeFilter					_activeTourTypeFilter;

	private int								_selectedYear				= -1;

	private TourbookStatistic				_activeStatistic;

	/**
	 * Contains all years which have tours for the selected tour type and person.
	 */
	private TIntArrayList					_availableYears;

	/**
	 * contains the statistics in the same sort order as the statistic combo box
	 */
	private ArrayList<TourbookStatistic>	_allStatisticProvider;

	private ActionStatisticOptions			_actionStatisticOptions;
	private ActionSynchChartScale			_actionSynchChartScale;

	private boolean							_isInUpdateUI;
	private boolean							_isSynchScaleEnabled;
	private boolean							_isVerticalOrderDisabled;

	private int								_minimumComboWidth;
	private int								_maximumComboWidth;

	private PixelConverter					_pc;

	/*
	 * UI controls
	 */
	private Combo							_comboYear;
	private Combo							_comboStatistics;
	private Combo							_comboNumberOfYears;
	private Combo							_comboBarVerticalOrder;

	private Composite						_statContainer;

	private PageBook						_pageBookStatistic;

	private SlideoutStatisticOptions		_slideoutStatisticOptions;

	private class ActionStatisticOptions extends ActionToolbarSlideout {

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			_slideoutStatisticOptions = new SlideoutStatisticOptions(_statContainer, toolbar);

			return _slideoutStatisticOptions;
		}

	}

	void actionSynchScale(final boolean isEnabled) {

		_isSynchScaleEnabled = isEnabled;

		_activeStatistic.setSynchScale(_isSynchScaleEnabled);

		_activeStatistic.updateStatistic(new StatisticContext(//
				_activePerson,
				_activeTourTypeFilter,
				_selectedYear,
				getNumberOfYears()));
	}

	private void addPartListener() {

		// set the part listener
		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {

//				if (partRef.getPart(false) == TourStatisticsView.this) {
//
//					int a = 0;
//					a++;
//				}
			}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == StatisticView.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};

		// register the part listener
		getSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				/*
				 * set a new chart configuration when the preferences has changed
				 */

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)
						|| property.equals(ITourbookPreferences.TOUR_PERSON_LIST_IS_MODIFIED)) {

					_activePerson = TourbookPlugin.getActivePerson();
					_activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

					updateStatistic();

				} else if (property.equals(ITourbookPreferences.STATISTICS_STATISTIC_PROVIDER_IDS)) {

					refreshStatisticProvider();

				} else if (property.equals(ITourbookPreferences.MEASUREMENT_SYSTEM)) {

					// measurement system has changed

					UI.updateUnits();

					updateStatistic();
				}
			}
		};

		// register the listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);

		/*
		 * Common preferences
		 */
		_prefChangeListenerCommon = new IPropertyChangeListener() {
			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ICommonPreferences.TIME_ZONE_LOCAL_ID)) {

					updateStatistic();
				}
			}
		};

		// register the listener
		_prefStoreCommon.addPropertyChangeListener(_prefChangeListenerCommon);
	}

	private void addSelectionListener() {

		// this view part is a selection listener
		_postSelectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				if (part == StatisticView.this) {
					return;
				}

				if (selection instanceof SelectionDeletedTours) {
					updateStatistic();
				}
			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object propertyData) {

				if (eventId == TourEventId.TOUR_CHANGED && propertyData instanceof TourEvent) {

					if (part == StatisticView.this) {
						return;
					}

					if (((TourEvent) propertyData).isTourModified) {
						/*
						 * ignore edit changes because the statistics show data only from saved data
						 */
						return;
					}

					_isInUpdateUI = true;

					// update statistics
					updateStatistic();

					_isInUpdateUI = false;

				} else if (eventId == TourEventId.UPDATE_UI || //
				eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					updateStatistic();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	public boolean canFireEvents() {

		return _isInUpdateUI == false;
	}

	private void createActions() {

		_actionStatisticOptions = new ActionStatisticOptions();
		_actionSynchChartScale = new ActionSynchChartScale(this);
	}

	@Override
	public void createPartControl(final Composite parent) {

		initUI(parent);

		createUI(parent);

		createActions();
		updateUI();

		addPartListener();
		addPrefListener();
		addSelectionListener();
		addTourEventListener();

		/*
		 * Start async that the workspace is fully initialized with all data filters
		 */
		parent.getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {

				_activePerson = TourbookPlugin.getActivePerson();
				_activeTourTypeFilter = TourbookPlugin.getActiveTourTypeFilter();

				restoreState();
			}
		});
	}

	private void createUI(final Composite parent) {

//		GridLayoutFactory.fillDefaults().applyTo(parent);

		_statContainer = new Composite(parent, SWT.NONE);

		GridDataFactory.fillDefaults().grab(true, true).applyTo(_statContainer);
		GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(_statContainer);
		{
			createUI_10_Toolbar(_statContainer);

			// pagebook: statistics
			_pageBookStatistic = new PageBook(_statContainer, SWT.NONE);
			_pageBookStatistic.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		}
	}

	private void createUI_10_Toolbar(final Composite parent) {

		final int widgetSpacing = 15;

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory
				.fillDefaults()//
				.grab(true, false)
				.align(SWT.BEGINNING, SWT.FILL)
				.applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(6)
				.margins(3, 3)
				.applyTo(container);
		{
			{
				/*
				 * combo: statistics
				 */

				_comboStatistics = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboStatistics.setToolTipText(Messages.Tour_Book_Combo_statistic_tooltip);
				_comboStatistics.setVisibleItemCount(50);

				_comboStatistics.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectStatistic();
					}
				});
			}

			{
				/*
				 * combo: year
				 */

				_comboYear = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboYear.setToolTipText(Messages.Tour_Book_Combo_year_tooltip);
				_comboYear.setVisibleItemCount(50);

				GridDataFactory
						.fillDefaults()//
						.indent(widgetSpacing, 0)
						.hint(_pc.convertWidthInCharsToPixels(_isOSX ? 12 : _isLinux ? 12 : 5), SWT.DEFAULT)
						.applyTo(_comboYear);

				_comboYear.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectYear();
					}
				});
			}

			{
				/*
				 * combo: year numbers
				 */

				_comboNumberOfYears = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboNumberOfYears.setToolTipText(Messages.tour_statistic_number_of_years);
				_comboNumberOfYears.setVisibleItemCount(50);

				GridDataFactory
						.fillDefaults()//
						.indent(2, 0)
						.hint(_pc.convertWidthInCharsToPixels(_isOSX ? 8 : _isLinux ? 8 : 4), SWT.DEFAULT)
						.applyTo(_comboNumberOfYears);

				_comboNumberOfYears.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectYear();
					}
				});
			}

			{
				/*
				 * combo: sequence for stacked charts
				 */

				_comboBarVerticalOrder = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
				_comboBarVerticalOrder.setToolTipText(Messages.Tour_Statistic_Combo_BarVOrder_Tooltip);
				_comboBarVerticalOrder.setVisibleItemCount(50);
				_comboBarVerticalOrder.setVisible(false);

				GridDataFactory
						.fillDefaults()//
						.indent(widgetSpacing, 0)
//						.hint(defaultTextSize.x, SWT.DEFAULT)
						.applyTo(_comboBarVerticalOrder);

				_comboBarVerticalOrder.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectBarVerticalOrder();
					}
				});
			}
		}
	}

	@Override
	public void dispose() {

		// dispose all statistic resources
		for (final TourbookStatistic statistic : getAvailableStatistics()) {
			statistic.dispose();
		}

		getViewSite().getPage().removePartListener(_partListener);
		getSite().getPage().removePostSelectionListener(_postSelectionListener);
		TourManager.getInstance().removeTourEventListener(_tourEventListener);

		_prefStore.removePropertyChangeListener(_prefChangeListener);
		_prefStoreCommon.removePropertyChangeListener(_prefChangeListenerCommon);

		super.dispose();
	}

	/**
	 * @param defaultYear
	 * @return Returns the index for the active year or <code>-1</code> when there are no years
	 *         available
	 */
	private int getActiveYearComboboxIndex(final int defaultYear) {

		int selectedYearIndex = -1;

		if (_availableYears == null) {
			return selectedYearIndex;
		}

		/*
		 * try to get the year index for the default year
		 */
		if (defaultYear != -1) {

			int yearIndex = 0;
			for (final int year : _availableYears.toArray()) {

				if (year == defaultYear) {

					_selectedYear = defaultYear;

					return yearIndex;
				}
				yearIndex++;
			}
		}

		/*
		 * try to get year index of the selected year
		 */
		int yearIndex = 0;
		for (final int year : _availableYears.toArray()) {
			if (year == _selectedYear) {
				selectedYearIndex = yearIndex;
				break;
			}
			yearIndex++;
		}

		return selectedYearIndex;
	}

	/**
	 * @return Returns all statistic plugins which are displayed in the statistic combo box
	 */
	private ArrayList<TourbookStatistic> getAvailableStatistics() {

		if (_allStatisticProvider == null) {
			_allStatisticProvider = StatisticManager.getStatisticProviders();
		}

		return _allStatisticProvider;
	}

	/**
	 * @return Returns number of years which are selected in the combobox
	 */
	private int getNumberOfYears() {

		int numberOfYears = 1;
		final int selectedIndex = _comboNumberOfYears.getSelectionIndex();

		if (selectedIndex != -1) {
			numberOfYears = selectedIndex + 1;
		}

		return numberOfYears;
	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		if (_activeStatistic == null) {
			return null;
		}

		final Long selectedTourId = _activeStatistic.getSelectedTour();
		if (selectedTourId == null) {
			return null;
		}

		final TourData selectedTourData = TourManager.getInstance().getTourData(selectedTourId);
		if (selectedTourData == null) {
			return null;
		} else {
			final ArrayList<TourData> selectedTours = new ArrayList<TourData>();
			selectedTours.add(selectedTourData);
			return selectedTours;
		}
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		final GC gc = new GC(parent);
		{
			_minimumComboWidth = gc.textExtent(COMBO_MINIMUM_WIDTH).x;
			_maximumComboWidth = gc.textExtent(COMBO_MAXIMUM_WIDTH).x;
		}
		gc.dispose();
	}

	private void onSelectBarVerticalOrder() {

		if (_activeStatistic == null) {
			return;
		}

		_activeStatistic.setBarVerticalOrder(_comboBarVerticalOrder.getSelectionIndex());
	}

	private void onSelectStatistic() {

		if (setActiveStatistic() == false) {
			return;
		}

		updateStatistic_10_NoReload();
	}

	private void onSelectYear() {

		final int selectedItem = _comboYear.getSelectionIndex();
		if (selectedItem != -1) {

			_selectedYear = Integer.parseInt(_comboYear.getItem(selectedItem));

			updateStatistic_10_NoReload();
		}
	}

	void refreshStatisticProvider() {

		if (setActiveStatistic() == false) {
			return;
		}

		_allStatisticProvider = StatisticManager.getStatisticProviders();

		_comboStatistics.removeAll();
		int indexCounter = 0;
		int selectedIndex = 0;

		// fill combobox with statistic names
		for (final TourbookStatistic statistic : getAvailableStatistics()) {

			_comboStatistics.add(statistic.plugin_VisibleName);

			if (_activeStatistic != null && _activeStatistic.plugin_StatisticId.equals(statistic.plugin_StatisticId)) {
				selectedIndex = indexCounter;
			}

			indexCounter++;
		}

		// reselect stat
		_comboStatistics.select(selectedIndex);
		onSelectStatistic();
	}

	/**
	 * create the year list for all tours and fill the year combobox with the available years
	 */
	private void refreshYearCombobox() {

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "\n\n" //								//$NON-NLS-1$
				+ "SELECT" // 											//$NON-NLS-1$
				+ " startYear " //										//$NON-NLS-1$
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA) //			//$NON-NLS-1$
				+ (" WHERE 1=1 " + sqlFilter.getWhereClause()) //		//$NON-NLS-1$
				+ " GROUP BY STARTYEAR ORDER BY STARTYEAR" //			//$NON-NLS-1$
				+ "\n\n"; //											//$NON-NLS-1$

		_availableYears = new TIntArrayList();

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				_availableYears.add(result.getInt(1));
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		_comboYear.removeAll();

		/*
		 * add all years of the tours and the current year
		 */
		final int thisYear = LocalDate.now().getYear();

		boolean isThisYearSet = false;

		for (final int year : _availableYears.toArray()) {

			if (year == thisYear) {
				isThisYearSet = true;
			}

			_comboYear.add(Integer.toString(year));
		}

		// add currenty year if not set
		if (isThisYearSet == false) {
			_availableYears.add(thisYear);
			_comboYear.add(Integer.toString(thisYear));
		}
	}

	/**
	 * Restore selected statistic
	 */
	void restoreState() {

		final ArrayList<TourbookStatistic> allAvailableStatistics = getAvailableStatistics();
		if (allAvailableStatistics.size() == 0) {
			return;
		}

		// select number of years
		final int numberOfYearsIndex = Util.getStateInt(_state, STATE_NUMBER_OF_YEARS, 0);
		_comboNumberOfYears.select(numberOfYearsIndex);

		// select year
		final int defaultYear = Util.getStateInt(_state, STATE_SELECTED_YEAR, -1);
		refreshYearCombobox();
		selectYear(defaultYear);

		// select statistic
		int prevStatIndex = 0;
		final String mementoStatisticId = _state.get(STATE_SELECTED_STATISTIC);
		if (mementoStatisticId != null) {
			int statIndex = 0;
			for (final TourbookStatistic statistic : allAvailableStatistics) {
				if (mementoStatisticId.equalsIgnoreCase(statistic.plugin_StatisticId)) {
					prevStatIndex = statIndex;
					break;
				}
				statIndex++;
			}
		}

		// select statistic item
		_comboStatistics.select(prevStatIndex);
		onSelectStatistic();

		// restore statistic state, e.g. reselect previous selection
		if (_state != null) {
			allAvailableStatistics.get(prevStatIndex).restoreState(_state);
		}
	}

	public void saveState() {

		final ArrayList<TourbookStatistic> allAvailableStatistics = getAvailableStatistics();
		if (allAvailableStatistics.size() == 0) {
			return;
		}

		// keep statistic id for the selected statistic
		final int selectionIndex = _comboStatistics.getSelectionIndex();
		if (selectionIndex != -1) {
			_state.put(STATE_SELECTED_STATISTIC, allAvailableStatistics.get(selectionIndex).plugin_StatisticId);
		}

		for (final TourbookStatistic tourbookStatistic : allAvailableStatistics) {
			tourbookStatistic.saveState(_state);
		}

		_state.put(STATE_NUMBER_OF_YEARS, _comboNumberOfYears.getSelectionIndex());
		_state.put(STATE_SELECTED_YEAR, _selectedYear);
	}

	private void selectYear(final int defaultYear) {

		int selectedYearIndex = getActiveYearComboboxIndex(defaultYear);
		if (selectedYearIndex == -1) {

			/*
			 * the active year was not found in the combo box, it's possible that the combo box
			 * needs to be update
			 */

			refreshYearCombobox();
			selectedYearIndex = getActiveYearComboboxIndex(defaultYear);

			if (selectedYearIndex == -1) {

				// year is still not selected
				final int yearCount = _comboYear.getItemCount();

				// reselect the youngest year if years are available
				if (yearCount > 0) {
					selectedYearIndex = yearCount - 1;
					_selectedYear = Integer.parseInt(_comboYear.getItem(yearCount - 1));
				}
			}
		}

		_comboYear.select(selectedYearIndex);
	}

	/**
	 * @return Returns <code>true</code> when a statistic is selected and {@link #_activeStatistic}
	 *         is valid.
	 */
	private boolean setActiveStatistic() {

		// get selected statistic
		final int selectedIndex = _comboStatistics.getSelectionIndex();
		if (selectedIndex == -1) {
			_activeStatistic = null;
			return false;
		}

		final ArrayList<TourbookStatistic> allAvailableStatistics = getAvailableStatistics();
		if (allAvailableStatistics.size() == 0) {
			return false;
		}

		final TourbookStatistic tourbookStatistic = allAvailableStatistics.get(selectedIndex);

		Composite statisticUI = tourbookStatistic.getUIControl();

		if (statisticUI == null) {

			// create statistic UI in the pagebook for the selected statistic

			statisticUI = new Composite(_pageBookStatistic, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(statisticUI);
			GridLayoutFactory.fillDefaults().applyTo(statisticUI);
			{
				tourbookStatistic.createUI(statisticUI, getViewSite());
				tourbookStatistic.restoreStateEarly(_state);
			}
		}

		_activeStatistic = tourbookStatistic;

		return true;
	}

	@Override
	public void setFocus() {

		_comboStatistics.setFocus();
	}

	/**
	 * Update all statistics which have been created because person or tour type could be changed
	 * and reload data.
	 * 
	 * @param person
	 * @param tourTypeFilter
	 */
	private void updateStatistic() {

		if (setActiveStatistic() == false) {
			return;
		}

		refreshYearCombobox();
		selectYear(-1);

		// tell all existing statistics the data have changed
		for (final TourbookStatistic statistic : getAvailableStatistics()) {

			if (statistic.getUIControl() != null) {

				statistic.setSynchScale(_isSynchScaleEnabled);
				statistic.setDataDirty();
			}
		}

		// refresh current statistic
		final StatisticContext statContext = new StatisticContext(
				_activePerson,
				_activeTourTypeFilter,
				_selectedYear,
				getNumberOfYears());

		statContext.isRefreshData = true;
		statContext.eventManager = this;

		_activeStatistic.updateStatistic(statContext);

		updateStatistic_20_PostRefresh(statContext);
	}

	/**
	 */
	private void updateStatistic_10_NoReload() {

		// keep current year
		if (_selectedYear == -1) {
			return;
		}

		if (setActiveStatistic() == false) {
			// statistic is not available
			return;
		}

		// display selected statistic
		_pageBookStatistic.showPage(_activeStatistic.getUIControl());

		selectYear(-1);

		_activeStatistic.setSynchScale(_isSynchScaleEnabled);

		final StatisticContext statContext = new StatisticContext(
				_activePerson,
				_activeTourTypeFilter,
				_selectedYear,
				getNumberOfYears());

		_activeStatistic.updateStatistic(statContext);

		updateStatistic_20_PostRefresh(statContext);
		updateUI_Toolbar();
	}

	private void updateStatistic_20_PostRefresh(final StatisticContext statContext) {

		if (statContext.outIsBarReorderingSupported) {

			updateStatistic_30_BarOrdering(statContext);

			// vertical order feature is used
			_isVerticalOrderDisabled = false;

			_comboBarVerticalOrder.setVisible(true);

		} else {

			if (_isVerticalOrderDisabled == false) {

				// disable vertical order feature

				_comboBarVerticalOrder.setVisible(false);

				_isVerticalOrderDisabled = true;
			}
		}
	}

	private void updateStatistic_30_BarOrdering(final StatisticContext statContext) {

		// check if enabled
		if (!statContext.outIsUpdateBarNames) {
			return;
		}

		final String[] stackedNames = statContext.outBarNames;

		// hide combo when bars are not available
		if (stackedNames == null) {
			_comboBarVerticalOrder.setVisible(false);
			_isVerticalOrderDisabled = true;

			return;
		}

		_comboBarVerticalOrder.removeAll();

		for (final String name : stackedNames) {
			_comboBarVerticalOrder.add(name);
		}

		final int selectedIndex = statContext.outVerticalBarIndex;
		final int checkedIndex = selectedIndex >= _comboBarVerticalOrder.getItemCount() ? 0 : selectedIndex;

		_comboBarVerticalOrder.select(checkedIndex);
		_comboBarVerticalOrder.setEnabled(true);

		/*
		 * Adjust the combo width
		 */
		final int preferredWidth = _comboBarVerticalOrder.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x;

		final GridData gd = (GridData) _comboBarVerticalOrder.getLayoutData();
		gd.widthHint = preferredWidth > _maximumComboWidth //
				? _maximumComboWidth
				: preferredWidth < _minimumComboWidth //
						? _minimumComboWidth
						: preferredWidth;

		_statContainer.layout(true, true);
	}

	private void updateUI() {

		// fill combobox with number of years
		for (int years = 1; years <= 100; years++) {
			_comboNumberOfYears.add(Integer.toString(years));
		}

		// fill combobox with statistic names
		for (final TourbookStatistic statistic : getAvailableStatistics()) {
			_comboStatistics.add(statistic.plugin_VisibleName);
		}
	}

	/**
	 * Each statistic has it's own toolbar
	 */
	private void updateUI_Toolbar() {

		// update view toolbar
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();
		tbm.removeAll();

		tbm.add(_actionSynchChartScale);
		tbm.add(_actionStatisticOptions);

		// add actions from the statistic
		_activeStatistic.updateToolBar();

		// update toolbar to show added items
		tbm.update(true);

		// use slideout AFTER the toolbar is created/updated/filled, this creates it
		_activeStatistic.setupStatisticSlideout(_slideoutStatisticOptions);
		_slideoutStatisticOptions.setupGrid(//
				_activeStatistic.getGridPrefPrefix(),
				_activeStatistic.getEnabledGridOptions());
	}

}
