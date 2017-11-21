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
import net.tourbook.common.util.SelectionProvider;
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
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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
	

	static final int				numberOfSummaryLines					= 5;

	static final int				DEFAULT_TOUR_TOOLTIP_DELAY				= 100; // ms

	
	private final IPreferenceStore	_prefStore								= TourbookPlugin.getPrefStore();
	private final IDialogSettings	_state									= TourbookPlugin.getState("TourCalendarView");		//$NON-NLS-1$
	
	ColorDefinition[]				_allColorDefinition						= GraphColorManager.getInstance().getGraphColorDefinitions();
	
// SET_FORMATTING_ON

	private ISelectionProvider		_selectionProvider;
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

	TourInfoFormatter[]				tourInfoFormatter				= {

			createFormatter_Tour_Empty(),

			createFormatter_Tour_Title_Description(),
			createFormatter_Tour_Description_Title(),

			createFormatter_Tour_Distance_Time(),
			createFormatter_Tour_Distance_Speed(),
			createFormatter_Tour_Distance_Pace(),

			createFormatter_Tour_Time_Distance(),
			createFormatter_Tour_Time_Speed(),
			createFormatter_Tour_Time_Pace()
	};

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

	private Combo					_comboProfileName;

	private Label					_lblTitle;

	abstract class TourInfoFormatter {

		int index;

		abstract String format(CalendarTourData data);

		abstract String getText();
	}

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

	// create and register our selection provider
	private void addSelectionProvider() {

		getSite().setSelectionProvider(_selectionProvider = new SelectionProvider());

		_calendarGraph.addSelectionProvider(new ICalendarSelectionProvider() {

			@Override
			public void selectionChanged(final CalendarGraph.CalendarSelectItem selection) {
				if (selection.isTour()) {
					_selectionProvider.setSelection(new SelectionTourId(selection.id));
				}
			}

		});
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

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

		_actionBack = new Action() {
			@Override
			public void run() {
				_calendarGraph.scroll_Screen(false);
			}
		};
		_actionBack.setText(Messages.Calendar_View_Action_Back);
		_actionBack.setToolTipText(Messages.Calendar_View_Action_Back_Tooltip);
		_actionBack.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowDown));

		_actionForward = new Action() {
			@Override
			public void run() {
				_calendarGraph.scroll_Screen(true);
			}
		};
		_actionForward.setText(Messages.Calendar_View_Action_Forward);
		_actionForward.setToolTipText(Messages.Calendar_View_Action_Forward_Tooltip);
		_actionForward.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowUp));

		_actionSetLinked = new Action(null, Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_calendarGraph.setLinked(_actionSetLinked.isChecked());
			}
		};
		_actionSetLinked.setText(Messages.Calendar_View_Action_LinkWithOtherViews);
		_actionSetLinked.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__SyncViews));
		_actionSetLinked.setChecked(true);

		_actionGotoToday = new Action() {
			@Override
			public void run() {
				_calendarGraph.gotoDate_Today();
			}
		};
		_actionGotoToday.setText(Messages.Calendar_View_Action_GotoToday);
		_actionGotoToday.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZoomCentered));
	}

	/**
	 * description - title
	 */
	private TourInfoFormatter createFormatter_Tour_Description_Title() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {

				if (data.tourDescription != null && data.tourDescription.length() > 1) {

					// for now we are only supporting one line descriptions
					return data.tourDescription.replace("\r\n", UI.SPACE1).replace("\n", UI.SPACE1); //$NON-NLS-1$ //$NON-NLS-2$

				} else if (data.tourTitle != null && data.tourTitle.length() > 1) {

					return data.tourTitle;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_ShowDescriptionTitle;
			}
		};
	}

	/**
	 * distance - pace
	 */
	private TourInfoFormatter createFormatter_Tour_Distance_Pace() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {
				final int pace = (int) (data.distance == 0
						? 0
						: (1000 * data.recordingTime / data.distance * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
				final float distance = data.distance / 1000.0f / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
				return String
						.format(
								NLS.bind(
										Messages.Calendar_View_Format_DistancePace,
										UI.UNIT_LABEL_DISTANCE,
										UI.UNIT_LABEL_PACE),
								distance,
								pace / 60,
								pace % 60)
						.toString();
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_DistancePace;
			}
		};
	}

	/**
	 * distance - speed
	 */
	private TourInfoFormatter createFormatter_Tour_Distance_Speed() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {
				final float distance = data.distance;
				final int time = data.recordingTime;
				return String
						.format(
								NLS.bind(
										Messages.Calendar_View_Format_DistanceSpeed,
										UI.UNIT_LABEL_DISTANCE,
										UI.UNIT_LABEL_SPEED),
								distance / 1000.0f / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE,
								distance == 0 ? 0 : distance / (time / 3.6f))
						.toString();
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_ShowDistanceSpeed;
			}
		};
	}

	/**
	 * distance - time
	 */
	private TourInfoFormatter createFormatter_Tour_Distance_Time() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {

				final float distance = (float) (data.distance / 1000.0 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);
				final int time = data.recordingTime;

				return String
						.format(
								NLS.bind("%4.0f {0}", UI.UNIT_LABEL_DISTANCE),
								distance
//								,
//								time / 3600,
//								(time % 3600) / 60
				)
						.toString();
//				return String
//						.format(
//								NLS.bind(Messages.Calendar_View_Format_DistanceTime, UI.UNIT_LABEL_DISTANCE),
//								distance,
//								time / 3600,
//								(time % 3600) / 60)
//						.toString();
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_ShowDistanceTime;
			}
		};
	}

	private TourInfoFormatter createFormatter_Tour_Empty() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {
				return UI.EMPTY_STRING;
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_ShowNothing;
			}
		};
	}

	/**
	 * time - distance
	 */
	private TourInfoFormatter createFormatter_Tour_Time_Distance() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {
				final int time = data.recordingTime;
				final float distance = data.distance / 1000.0f / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
				return String
						.format(
								NLS.bind(Messages.Calendar_View_Format_TimeDistance, UI.UNIT_LABEL_DISTANCE),
								time / 3600,
								(time % 3600) / 60,
								distance)
						.toString();
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_TimeDistance;
			}
		};
	}

	/**
	 * time - pace
	 */
	private TourInfoFormatter createFormatter_Tour_Time_Pace() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {

				final int pace = (int) (data.distance == 0
						? 0
						: (1000 * data.recordingTime / data.distance * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));

				return String
						.format(
								NLS.bind(Messages.Calendar_View_Format_TimePace, UI.UNIT_LABEL_PACE),
								data.recordingTime / 3600,
								(data.recordingTime % 3600) / 60,
								pace / 60,
								pace % 60)
						.toString();
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_TimePace;
			}
		};
	}

	private TourInfoFormatter createFormatter_Tour_Time_Speed() {
		return /**
				 * time - speed
				 */
		new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {
				final int time = data.recordingTime;
				return String
						.format(
								NLS.bind(Messages.Calendar_View_Format_TimeSpeed, UI.UNIT_LABEL_SPEED),
								time / 3600,
								(time % 3600) / 60,
								data.distance == 0 ? 0 : data.distance
										/ time
										* 3.6f
										/ net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)
						.toString();
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_TimeSpeed;
			}
		};
	}

	/**
	 * title - description
	 */
	private TourInfoFormatter createFormatter_Tour_Title_Description() {

		return new TourInfoFormatter() {
			@Override
			public String format(final CalendarTourData data) {
				if (data.tourTitle != null && data.tourTitle.length() > 1) {

					return data.tourTitle;

				} else if (data.tourDescription != null && data.tourDescription.length() > 1) {

					// for now we are only supporting one line descriptions
//					return data.tourDescription.replace("\r\n", UI.SPACE1).replace("\n", UI.SPACE1); //$NON-NLS-1$ //$NON-NLS-2$
					return data.tourDescription;

				} else {
					return UI.EMPTY_STRING;
				}
			}

			@Override
			public String getText() {
				return Messages.Calendar_View_Action_ShowTitleDescription;
			}
		};
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
		addSelectionProvider();

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
		{
			{
				/*
				 * Title
				 */
				_lblTitle = new Label(container, SWT.NONE);
				GridDataFactory
						.fillDefaults()//
						.grab(true, false)
						.applyTo(_lblTitle);
				MTFont.setHeaderFont(_lblTitle);
			}
			{
				/*
				 * Combo: Profiles
				 */
				_comboProfileName = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
				_comboProfileName.setVisibleItemCount(30);
				_comboProfileName.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						onSelectProfile();
					}
				});
				GridDataFactory
						.fillDefaults()
						.align(SWT.BEGINNING, SWT.CENTER)
						.hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
						.applyTo(_comboProfileName);
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

	private void fillUI_Profiles() {

		// 1. get the profiles
		final ArrayList<CalendarProfile> allCalendarProfiles = CalendarProfileManager.getAllCalendarProfiles();

		// 2. get active profile
		final CalendarProfile activeProfile = CalendarProfileManager.getActiveCalendarProfile();

		_comboProfileName.removeAll();

		int selectIndex = 0;

		for (int profileIndex = 0; profileIndex < allCalendarProfiles.size(); profileIndex++) {

			final CalendarProfile profile = allCalendarProfiles.get(profileIndex);

			_comboProfileName.add(profile.name);

			// get index for active profile
			if (activeProfile.equals(profile)) {
				selectIndex = profileIndex;
			}
		}

		_comboProfileName.select(selectIndex);
	}

	public CalendarGraph getCalendarGraph() {
		return _calendarGraph;
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

		final int selectedIndex = _comboProfileName.getSelectionIndex();
		final ArrayList<CalendarProfile> allProfiles = CalendarProfileManager.getAllCalendarProfiles();

		final CalendarProfile selectedProfile = allProfiles.get(selectedIndex);
		final CalendarProfile activeProfile = CalendarProfileManager.getActiveCalendarProfile();

		if (selectedProfile.equals(activeProfile)) {

			// profile has not changed
			return;
		}

		// when changing the profile then more/less data are needed
		_calendarGraph.stopDataProvider();

		CalendarProfileManager.setActiveCalendarProfile(selectedProfile, true);
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
			_calendarGraph.gotoDate(LocalDate.now());
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
		final int selectedIndex = _comboProfileName.getSelectionIndex();
		_comboProfileName.setItem(selectedIndex, modifiedProfileName);
	}

	void updateUI_Title(final LocalDate calendarFirstDay, final LocalDate calendarLastDay) {

		if (calendarFirstDay.equals(_titleFirstDay) && calendarLastDay.equals(_titleLastDay)) {
			// these dates are already displayed
			return;
		}

		_titleFirstDay = calendarFirstDay;
		_titleLastDay = calendarLastDay;

		_lblTitle.setText(
				calendarFirstDay.format(TimeTools.Formatter_Date_L)
						+ UI.DASH_WITH_DOUBLE_SPACE
						+ calendarLastDay.format(TimeTools.Formatter_Date_L));
	}

}
