/*******************************************************************************
 * Copyright (C) 2017 Matthias Helmling and Contributors
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
package net.tourbook.ui.views.calendar;

import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.color.ColorDefinition;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.views.calendar.CalendarProfileManager.ICalendarProfileListener;

import org.eclipse.jface.action.Action;
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
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class CalendarView extends ViewPart implements ITourProvider, ICalendarProfileListener {

// SET_FORMATTING_OFF
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String		ID										= "net.tourbook.views.calendar.CalendarView";		//$NON-NLS-1$
	
	private static final String		STATE_IS_LINKED							= "STATE_IS_LINKED"; 								//$NON-NLS-1$
	private static final String		STATE_IS_SHOW_TOUR_INFO					= "STATE_IS_SHOW_TOUR_INFO";	 					//$NON-NLS-1$
	static final String 			STATE_TOUR_TOOLTIP_DELAY	 			= "STATE_TOUR_TOOLTIP_DELAY"; 						//$NON-NLS-1$

	private static final String		STATE_FIRST_DISPLAYED_EPOCH_DAY			= "STATE_FIRST_DISPLAYED_EPOCH_DAY";				//$NON-NLS-1$
	private static final String		STATE_SELECTED_TOURS					= "STATE_SELECTED_TOURS";							//$NON-NLS-1$
	
	static final int				DEFAULT_TOUR_TOOLTIP_DELAY				= 100; // ms
	
	private final IPreferenceStore	_prefStore								= TourbookPlugin.getPrefStore();
	private final IDialogSettings	_state									= TourbookPlugin.getState("TourCalendarView");		//$NON-NLS-1$
	
	ColorDefinition[]				_allColorDefinition						= GraphColorManager.getInstance().getGraphColorDefinitions();
	
// SET_FORMATTING_ON

	//
	private ISelectionListener		_selectionListener;
	private IPartListener2			_partListener;
	private IPropertyChangeListener	_prefChangeListener;
	private ITourEventListener		_tourEventListener;

	private ActionCalendarOptions	_actionCalendarOptions;

	private Action					_actionBack;
	private Action					_actionForward;
	private Action					_actionSetLinked;
	private Action					_actionGotoToday;
	private ActionTourInfo			_actionTourInfo;
	//
	private LocalDate				_titleFirstDay;
	private LocalDate				_titleLastDay;

	private PixelConverter			_pc;

	private CalendarTourInfoToolTip	_tourInfoToolTip;
	private OpenDialogManager		_openDlgMgr						= new OpenDialogManager();

	/*
	 * UI controls
	 */
	private CalendarGraph			_calendarGraph;

	private Composite				_parent;
	private Composite				_calendarContainer;

	private Combo					_comboProfiles;

	private Label					_lblTitle;

	public CalendarView() {}

	private void addPartListener() {

		_partListener = new IPartListener2() {
			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == CalendarView.this) {
					saveState();
					CalendarProfileManager.removeProfileListener(CalendarView.this);
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {

				if (partRef.getPart(false) == CalendarView.this) {
					CalendarProfileManager.addProfileListener(CalendarView.this);
				}
			}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPrefListener() {

		_prefChangeListener = new IPropertyChangeListener() {

			@Override
			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
						|| property.equals(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK)
						|| property.equals(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK)) {

					refreshCalendar();

				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					_calendarGraph.updateTourTypeColors();

					refreshCalendar();
				}
			}

		};

		// add prop listener
		_prefStore.addPropertyChangeListener(_prefChangeListener);

	}

	// create and register our selection listener
	private void addSelectionListener() {

		_selectionListener = new ISelectionListener() {

			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {

				// prevent to listen to a selection which is originated by this year chart
				if (part == CalendarView.this) {
					return;
				}

				onSelectionChanged(selection);

			}
		};

		// register selection listener in the page
		getSite().getPage().addPostSelectionListener(_selectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (CalendarView.this == part) {
					// skip own events
					return;
				}

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {
					/*
					 * it is possible when a tour type was modified, the tour can be hidden or
					 * visible in the viewer because of the tour type filter
					 */
					refreshCalendar();

				} else if ((eventId == TourEventId.TOUR_SELECTION //
						|| eventId == TourEventId.SLIDER_POSITION_CHANGED)

						&& eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
						|| eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					refreshCalendar();
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	/**
	 * Close all opened dialogs except the opening dialog.
	 * 
	 * @param openingDialog
	 */
	void closeOpenedDialogs(final IOpeningDialog openingDialog) {

		_openDlgMgr.closeOpenedDialogs(openingDialog);
	}

	private void createActions() {

		_actionCalendarOptions = new ActionCalendarOptions(this);
		_actionTourInfo = new ActionTourInfo(this, _parent);

		/*
		 * Back
		 */
		{
			_actionBack = new Action() {
				@Override
				public void run() {
					_calendarGraph.scroll_WithWheel_Screen(-1);
				}
			};
			_actionBack.setText(Messages.Calendar_View_Action_Back);
			_actionBack.setToolTipText(Messages.Calendar_View_Action_Back_Tooltip);
			_actionBack.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowUp));
		}

		/*
		 * Forward
		 */
		{
			_actionForward = new Action() {
				@Override
				public void run() {
					_calendarGraph.scroll_WithWheel_Screen(1);
				}
			};
			_actionForward.setText(Messages.Calendar_View_Action_Forward);
			_actionForward.setToolTipText(Messages.Calendar_View_Action_Forward_Tooltip);
			_actionForward.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowDown));
		}

		/*
		 * Link with other views
		 */
		{
			_actionSetLinked = new Action(null, Action.AS_CHECK_BOX) {
				@Override
				public void run() {
					_calendarGraph.setLinked(_actionSetLinked.isChecked());
				}
			};
			_actionSetLinked.setText(Messages.Calendar_View_Action_LinkWithOtherViews);
			_actionSetLinked.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SyncViews));
			_actionSetLinked.setChecked(true);
		}

		/*
		 * Today
		 */
		{
			_actionGotoToday = new Action() {
				@Override
				public void run() {
					_calendarGraph.gotoDate_Today();
				}
			};
			_actionGotoToday.setText(Messages.Calendar_View_Action_GotoToday);
			_actionGotoToday.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZoomCentered));
		}
	}

	@Override
	public void createPartControl(final Composite parent) {

		_parent = parent;

		addPartListener();
		addPrefListener();
		addTourEventListener();

		initUI(parent);

		createUI(parent);

		createActions();
		fillActions();

		addSelectionListener();

		// set context menu
		final Menu contextMenu = (new TourContextMenu()).createContextMenu(this, _calendarGraph, getLocalActions());
		_calendarGraph.setMenu(contextMenu);

		// set tour tooltip
		_tourInfoToolTip = new CalendarTourInfoToolTip(this);

		restoreState();

		profileIsModified();

		// restore selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());
	}

	private void createUI(final Composite parent) {

		parent.addControlListener(new ControlAdapter() {

			@Override
			public void controlResized(final ControlEvent e) {
				updateUI_Title(null, null);
			}
		});

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory
				.fillDefaults()//
				.numColumns(1)
				.spacing(0, 0)
				.applyTo(container);
		{
			createUI_10_Header(container);

			// create composite with vertical scrollbars
			_calendarContainer = new Composite(container, SWT.NO_BACKGROUND | SWT.V_SCROLL);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_calendarContainer);
			GridLayoutFactory.fillDefaults().spacing(0, 0).margins(0, 0).numColumns(1).applyTo(_calendarContainer);
			{
				_calendarGraph = new CalendarGraph(_calendarContainer, SWT.NO_BACKGROUND, this);
				GridDataFactory.fillDefaults().grab(true, true).applyTo(_calendarGraph);
			}
		}
	}

	private void createUI_10_Header(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		{
			{
				/*
				 * Title
				 */
				_lblTitle = new Label(container, SWT.NONE);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.BEGINNING)
						.applyTo(_lblTitle);
				MTFont.setHeaderFont(_lblTitle);
//				_lblTitle.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
			}
			{
				/*
				 * Combo: Profiles
				 */
				_comboProfiles = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				_comboProfiles.setVisibleItemCount(30);
				_comboProfiles.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectProfile();
					}
				});
				GridDataFactory
						.fillDefaults()
						.align(SWT.BEGINNING, SWT.CENTER)
						.hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
						.applyTo(_comboProfiles);
			}
		}
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourEventListener);
		getSite().getPage().removePostSelectionListener(_selectionListener);
		_prefStore.removePropertyChangeListener(_prefChangeListener);

		super.dispose();
	}

	private void fillActions() {

		final IActionBars bars = getViewSite().getActionBars();

		final IToolBarManager toolbarMrg = bars.getToolBarManager();

		/*
		 * Toolbar
		 */
		final CalendarYearMonthContributionItem yearMonthItem = new CalendarYearMonthContributionItem(_calendarGraph);
		_calendarGraph.setYearMonthContributor(yearMonthItem);

		toolbarMrg.add(yearMonthItem);

		toolbarMrg.add(_actionSetLinked);
		toolbarMrg.add(_actionTourInfo);
		toolbarMrg.add(_actionCalendarOptions);
	}

	void fillUI_Profiles() {

		// 1. get the profiles
		final ArrayList<CalendarProfile> allCalendarProfiles = CalendarProfileManager.getAllCalendarProfiles();

		// 2. get active profile
		final CalendarProfile activeProfile = CalendarProfileManager.getActiveCalendarProfile();

		_comboProfiles.removeAll();

		int selectIndex = 0;

		for (int profileIndex = 0; profileIndex < allCalendarProfiles.size(); profileIndex++) {

			final CalendarProfile profile = allCalendarProfiles.get(profileIndex);

			_comboProfiles.add(profile.name);

			// get index for active profile
			if (activeProfile.equals(profile)) {
				selectIndex = profileIndex;
			}
		}

		_comboProfiles.select(selectIndex);
	}

	void fireSelection(final long tourId) {

		TourManager.fireEventWithCustomData(TourEventId.TOUR_SELECTION, new SelectionTourId(tourId), this);
	}

	public CalendarGraph getCalendarGraph() {
		return _calendarGraph;
	}

	SlideoutCalendarOptions getConfigSlideout() {
		return _actionCalendarOptions.getSlideout();
	}

	private ArrayList<Action> getLocalActions() {

		final ArrayList<Action> localActions = new ArrayList<Action>();

		localActions.add(_actionBack);
		localActions.add(_actionGotoToday);
		localActions.add(_actionForward);

		return localActions;

	}

	@Override
	public ArrayList<TourData> getSelectedTours() {

		final ArrayList<TourData> selectedTourData = new ArrayList<TourData>();
		final ArrayList<Long> tourIdSet = new ArrayList<Long>();
		tourIdSet.add(_calendarGraph.getSelectedTourId());
		for (final Long tourId : tourIdSet) {
			if (tourId > 0) { // < 0 means not selected
				selectedTourData.add(TourManager.getInstance().getTourData(tourId));
			}
		}
		return selectedTourData;
	}

	IDialogSettings getState() {
		return _state;
	}

	CalendarTourInfoToolTip getTourInfoTooltip() {
		return _tourInfoToolTip;
	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	boolean isShowTourTooltip() {

		return _actionTourInfo.isChecked();
	}

	private void onSelectionChanged(final ISelection selection) {

		// show and select the selected tour
		if (selection instanceof SelectionTourId) {

			final Long newTourId = ((SelectionTourId) selection).getTourId();
			final Long oldTourId = _calendarGraph.getSelectedTourId();

			if (newTourId != oldTourId) {

				if (_actionSetLinked.isChecked()) {
					_calendarGraph.gotoTour_Id(newTourId);
				} else {
					_calendarGraph.removeSelection();
				}
			}

		} else if (selection instanceof SelectionDeletedTours) {

			_calendarGraph.refreshCalendar();
		}
	}

	private void onSelectProfile() {

		final int selectedIndex = _comboProfiles.getSelectionIndex();
		final ArrayList<CalendarProfile> allProfiles = CalendarProfileManager.getAllCalendarProfiles();

		final CalendarProfile selectedProfile = allProfiles.get(selectedIndex);
		final CalendarProfile activeProfile = CalendarProfileManager.getActiveCalendarProfile();

		if (selectedProfile.equals(activeProfile)) {

			// profile has not changed
			return;
		}

		// when changing the profile then more/less data are needed
//		_calendarGraph.stopDataProvider();

		CalendarProfileManager.setActiveCalendarProfile(selectedProfile, true);
		CalendarProfileManager.getActiveCalendarProfile().dump();
	}

	@Override
	public void profileIsModified() {

		fillUI_Profiles();

		updateUI_Graph();
	}

	private void refreshCalendar() {

		if (null != _calendarGraph) {

			BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
				@Override
				public void run() {
					_calendarGraph.refreshCalendar();
				}
			});
		}
	}

	private void restoreState() {

		_actionSetLinked.setChecked(Util.getStateBoolean(_state, STATE_IS_LINKED, false));
		_actionTourInfo.setSelected(Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_INFO, true));

		final long epochDay = Util.getStateLong(_state, STATE_FIRST_DISPLAYED_EPOCH_DAY, Long.MIN_VALUE);
		if (epochDay == Long.MIN_VALUE) {
			_calendarGraph.gotoDate(
					LocalDate
							.now()

							// adjust that not the week after today are displayed -> this is empty :-(
							.minusWeeks(4),
					true);
		} else {
			_calendarGraph.setFirstDay(LocalDate.ofEpochDay(epochDay));
		}

		final Long selectedTourId = Util.getStateLong(_state, STATE_SELECTED_TOURS, new Long(-1));
		_calendarGraph.setSelectionTourId(selectedTourId);

		// tooltip
		_tourInfoToolTip.setPopupDelay(Util.getStateInt(_state, STATE_TOUR_TOOLTIP_DELAY, DEFAULT_TOUR_TOOLTIP_DELAY));
	}

	private void saveState() {

		_state.put(STATE_IS_LINKED, _actionSetLinked.isChecked());
		_state.put(STATE_IS_SHOW_TOUR_INFO, _actionTourInfo.isChecked());

		// save current date displayed
		_state.put(STATE_FIRST_DISPLAYED_EPOCH_DAY, _calendarGraph.getFirstDay().toLocalDate().toEpochDay());

		// until now we only implement single tour selection
		_state.put(STATE_SELECTED_TOURS, _calendarGraph.getSelectedTourId());

		CalendarProfileManager.saveState();
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		_calendarContainer.setFocus();
	}

	void updateUI_Graph() {

		// run async that the calling UI (slideout) is updated immediately

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				_calendarGraph.updateUI_Layout(true);
			}
		});
	}

	void updateUI_ProfileName(final String modifiedProfileName) {

		// update text in the combo
		final int selectedIndex = _comboProfiles.getSelectionIndex();
		_comboProfiles.setItem(selectedIndex, modifiedProfileName);
	}

	void updateUI_Title(final LocalDate calendarFirstDay, final LocalDate calendarLastDay) {

		if (calendarFirstDay != null) {

			if (calendarFirstDay.equals(_titleFirstDay) && calendarLastDay.equals(_titleLastDay)) {
				// these dates are already displayed
				return;
			}

			_titleFirstDay = calendarFirstDay;
			_titleLastDay = calendarLastDay;
		}

		if (_titleFirstDay == null) {
			// this can occure when resized and not setup
			return;
		}

		/*
		 * Get title text
		 */
		String titleText = UI.EMPTY_STRING;
		final GC gc = new GC(_lblTitle);
		{
			final int availableWidth = _lblTitle.getSize().x;

			titleText = _titleFirstDay.format(TimeTools.Formatter_Date_L)
					+ UI.DASH_WITH_DOUBLE_SPACE
					+ _titleLastDay.format(TimeTools.Formatter_Date_L);

			int titleWidth = gc.stringExtent(titleText).x;

			if (titleWidth > availableWidth) {

				titleText = _titleFirstDay.format(TimeTools.Formatter_Date_M)
						+ UI.DASH_WITH_DOUBLE_SPACE
						+ _titleLastDay.format(TimeTools.Formatter_Date_M);

				titleWidth = gc.stringExtent(titleText).x;

				if (titleWidth > availableWidth) {

					titleText = _titleFirstDay.format(TimeTools.Formatter_Date_S)
							+ UI.DASH_WITH_DOUBLE_SPACE
							+ _titleLastDay.format(TimeTools.Formatter_Date_S);

					titleWidth = gc.stringExtent(titleText).x;

					if (titleWidth > availableWidth) {

						titleText = UI.EMPTY_STRING;
					}
				}
			}
		}
		gc.dispose();

		_lblTitle.setText(titleText);
	}

}
