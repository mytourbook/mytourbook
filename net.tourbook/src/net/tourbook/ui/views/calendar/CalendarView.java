/*******************************************************************************
 * Copyright (C) 2011  Matthias Helmling and Contributors
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

import java.util.ArrayList;
import java.util.Formatter;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.UI;
import net.tourbook.ui.views.calendar.CalendarGraph.NavigationStyle;
import net.tourbook.util.SelectionProvider;
import net.tourbook.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.part.ViewPart;
import org.joda.time.DateTime;

public class CalendarView extends ViewPart implements ITourProvider {

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String					ID								= "net.tourbook.views.calendar.CalendarView"; //$NON-NLS-1$

	private final IPreferenceStore				_prefStore						= TourbookPlugin.getDefault() //
																						.getPreferenceStore();

	private final IDialogSettings				_state							= TourbookPlugin.getDefault() //
																						.getDialogSettingsSection(
																								"TourCalendarView");			//$NON-NLS-1$

	private PageBook							_pageBook;

	private CalendarComponents					_calendarComponents;
	private CalendarGraph						_calendarGraph;
	private ISelectionProvider					_selectionProvider;

	private ISelectionListener					_selectionListener;
	private IPartListener2						_partListener;
	private IPropertyChangeListener				_propChangeListener;
	private ITourEventListener					_tourPropertyListener;
	private CalendarYearMonthContributionItem	_cymci;
	
	private String								STATE_SELECTED_TOURS			= "SelectedTours";								// $NON-NLS-1$ //$NON-NLS-1$

	private String								STATE_FIRST_DAY					= "FirstDayDisplayed";							// $NON-NLS-1$ //$NON-NLS-1$
	private String								STATE_NUM_OF_WEEKS_NORMAL				= "NumberOfWeeksDisplayed";					// $NON-NLS-1$ //$NON-NLS-1$
	private String								STATE_NUM_OF_WEEKS_TINY					= "NumberOfWeeksDisplayedTinyView";			// $NON-NLS-1$ //$NON-NLS-1$
	private String								STATE_IS_LINKED					= "Linked";									// $NON-NLS-1$ //$NON-NLS-1$
	private String								STATE_TOUR_SIZE_DYNAMIC			= "TourSizeDynamic";							// $NON-NLS-1$ //$NON-NLS-1$
	private String								STATE_NUMBER_OF_TOURS_PER_DAY	= "NumberOfToursPerDay";						// $NON-NLS-1$ //$NON-NLS-1$
	private String								STATE_TOUR_INFO_FORMATTER_INDEX_		= "TourInfoFormatterIndex";					//$NON-NLS-1$
	private String								STATE_WEEK_SUMMARY_FORMATTER_INDEX_		= "WeekSummaryFormatterIndex";					//$NON-NLS-1$
	private String								STATE_TOUR_INFO_TEXT_COLOR			= "TourInfoUseTextColor";						//$NON-NLS-1$
	private String								STATE_TOUR_INFO_BLACK_TEXT_HIGHLIGHT	= "TourInfoUseBlackTextHightlight";			//$NON-NLS-1$
	private String								STATE_SHOW_DAY_NUMBER_IN_TINY_LAYOUT	= "ShowDayNumberInTinyView";
	private String								STATE_USE_LINE_COLOR_FOR_WEEK_SUMMARY	= "UseLineColorForWeekSummary";

	private Action								_forward, _back;
	private Action								_zoomIn, _zoomOut;
	private Action								_setLinked;
	private Action								_gotoToday;
	private Action								_setNavigationStylePhysical, _setNavigationStyleLogical;
	private Action[]							_setNumberOfToursPerDay;
	// private Action								_setTourSizeDynamic;
	private Action[]							_setTourInfoFormatLine;
	private Action[]							_setSummaryFormatLine;
	private Action[][]							_setTourInfoFormat;
	private Action[][]							_setWeekSummaryFormat;
	private Action								_setTourInfoTextColor;
	private Action								_setTourInfoBlackTextHighlight;
	private Action								_setShowDayNumberInTinyLayout;

	private Action								_setUseLineColorForWeekSummary;
	private boolean								_useLineColorForWeekSummary				= false;

	static final int							numberOfInfoLines						= 3;
	static final int							numberOfSummaryLines					= 5;

	ColorDefinition[]							_colorDefinitiosn						= GraphColorProvider
																								.getInstance()
																								.getGraphColorDefinitions();

	private WeekSummaryFormatter[]			_tourWeekSummaryFormatter				= {
																						// fool stupid auto formatter
			// - Nothing -
			new WeekSummaryFormatter(GraphColorProvider.PREF_GRAPH_TIME) {
				@Override
				public String format(final CalendarTourData data) {
					return UI.EMPTY_STRING;
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_ShowNothing;
				}
			},
			// - Distance -
			new WeekSummaryFormatter(GraphColorProvider.PREF_GRAPH_DISTANCE) {

				@Override
				String format(final CalendarTourData data) {
					if (data.distance > 0) {
						final float distance = (float) (data.distance / 1000.0 / UI.UNIT_VALUE_DISTANCE);
						return new Formatter().format(
								NLS.bind(Messages.Calendar_View_Format_Distance, UI.UNIT_LABEL_DISTANCE),
								distance).toString();
					} else {
						return "-";
					}
				}
			},
			// - Time -
			new WeekSummaryFormatter(GraphColorProvider.PREF_GRAPH_TIME) {

				@Override
				String format(final CalendarTourData data) {
					if (data.recordingTime > 0) {
						return new Formatter().format(
								Messages.Calendar_View_Format_Time,
								data.recordingTime / 3600,
								(data.recordingTime % 3600) / 60).toString();
					} else {
						return "-";
					}
				}
			},
			// - Altitude -
			new WeekSummaryFormatter(GraphColorProvider.PREF_GRAPH_ALTITUDE) {

				@Override
				String format(final CalendarTourData data) {
					if (data.altitude > 0) {
						final long alt = (long) (data.altitude / UI.UNIT_VALUE_ALTITUDE);
						return alt + " " + UI.UNIT_LABEL_ALTITUDE;
					} else {
						return "-";
					}
				}
			},
			// - Speed -
			new WeekSummaryFormatter(GraphColorProvider.PREF_GRAPH_SPEED) {

				@Override
				String format(final CalendarTourData data) {
					if (data.distance > 0) {
						return new Formatter().format(
								NLS.bind(Messages.Calendar_View_Format_Speed, UI.UNIT_LABEL_SPEED),
								data.distance == 0 ? 0 : data.distance / (data.recordingTime / 3.6f)).toString();
					} else {
						return "-";
					}
				}
			},
			// - Pace -
			new WeekSummaryFormatter(GraphColorProvider.PREF_GRAPH_PACE) {

				@Override
				String format(final CalendarTourData data) {
					if (data.recordingTime > 0 && data.distance > 0) {
						final int pace = (int) (data.distance == 0
								? 0
								: (1000 * data.recordingTime / data.distance * UI.UNIT_VALUE_DISTANCE));
						return new Formatter().format(
								NLS.bind(Messages.Calendar_View_Format_Pace, UI.UNIT_LABEL_PACE),
								pace / 60,
								pace % 60).toString();
					} else {
						return "-";
					}
				}
			}
																						};

	private TourInfoFormatter[]					_tourInfoFormatter				= {
																						// fool stupid auto formatter
			/*
			 * title - description
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					return UI.EMPTY_STRING;
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_ShowNothing;
				}
			},

			/*
			 * title - description
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					if (data.tourTitle != null && data.tourTitle.length() > 1) {
						return data.tourTitle;
					} else if (data.tourDescription != null && data.tourDescription.length() > 1) {
						// for now we are only supporting one line descriptions
						return data.tourDescription.replace("\r\n", " ").replace("\n", " ");
					} else {
						return UI.EMPTY_STRING;
					}
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_ShowTitleDescription;
				}
			},

			/*
			 * description - title
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					if (data.tourDescription != null && data.tourDescription.length() > 1) {
						// for now we are only supporting one line descriptions
						return data.tourDescription.replace("\r\n", " ").replace("\n", " ");
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
			},

			/*
			 * distance - time
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					final float distance = (float) (data.distance / 1000.0 / UI.UNIT_VALUE_DISTANCE);
					final int time = data.recordingTime;
					return new Formatter().format(
							NLS.bind(Messages.Calendar_View_Format_DistanceTime, UI.UNIT_LABEL_DISTANCE),
							distance,
							time / 3600,
							(time % 3600) / 60).toString();
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_ShowDistanceTime;
				}
			},

			/*
			 * distance - speed
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					final float distance = data.distance;
					final int time = data.recordingTime;
					return new Formatter().format(
							NLS.bind(
									Messages.Calendar_View_Format_DistanceSpeed,
									UI.UNIT_LABEL_DISTANCE,
									UI.UNIT_LABEL_SPEED),
							distance / 1000.0f / UI.UNIT_VALUE_DISTANCE,
							distance == 0 ? 0 : distance / (time / 3.6f)).toString();
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_ShowDistanceSpeed;
				}
			},

			/*
			 * distance - pace
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					final int pace = (int) (data.distance == 0
							? 0
							: (1000 * data.recordingTime / data.distance * UI.UNIT_VALUE_DISTANCE));
					final float distance = data.distance / 1000.0f / UI.UNIT_VALUE_DISTANCE;
					return new Formatter().format(
							NLS.bind(
									Messages.Calendar_View_Format_DistancePace,
									UI.UNIT_LABEL_DISTANCE,
									UI.UNIT_LABEL_PACE),
							distance,
							pace / 60,
							pace % 60).toString();
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_DistancePace;
				}
			},

			/*
			 * time - distance
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					final int time = data.recordingTime;
					final float distance = data.distance / 1000.0f / UI.UNIT_VALUE_DISTANCE;
					return new Formatter().format(
							NLS.bind(Messages.Calendar_View_Format_TimeDistance, UI.UNIT_LABEL_DISTANCE),
							time / 3600,
							(time % 3600) / 60,
							distance).toString();
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_TimeDistance;
				}
			},

			/*
			 * time - speed
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					final int time = data.recordingTime;
					return new Formatter().format(
							NLS.bind(Messages.Calendar_View_Format_TimeSpeed, UI.UNIT_LABEL_SPEED),
							time / 3600,
							(time % 3600) / 60,
							data.distance == 0 ? 0 : data.distance / time * 3.6f / UI.UNIT_VALUE_DISTANCE).toString();
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_TimeSpeed;
				}
			},

			/*
			 * time - pace
			 */
			new TourInfoFormatter() {
				@Override
				public String format(final CalendarTourData data) {
					final int pace = (int) (data.distance == 0
							? 0
							: (1000 * data.recordingTime / data.distance * UI.UNIT_VALUE_DISTANCE));
					return new Formatter().format(
							NLS.bind(Messages.Calendar_View_Format_TimePace, UI.UNIT_LABEL_PACE),
							data.recordingTime / 3600,
							(data.recordingTime % 3600) / 60,
							pace / 60,
							pace % 60).toString();
				}

				@Override
				public String getText() {
					return Messages.Calendar_View_Action_TimePace;
				}
			}

																				};

	class NumberOfToursPerDayAction extends Action {

		private int	numberOfTours;

		NumberOfToursPerDayAction(final int numberOfTours) {

			super(null, AS_RADIO_BUTTON);

			this.numberOfTours = numberOfTours;
			if (0 == numberOfTours) {
				setText(Messages.Calendar_View_Action_DisplayTours_All);
			} else if (1 == numberOfTours) {
				setText(Messages.Calendar_View_Action_DisplayTours_1ByDay);
			} else {
				setText(NLS.bind(Messages.Calendar_View_Action_DisplayTours_ByDay, numberOfTours));
			}
		}

		@Override
		public void run() {
			_calendarGraph.setNumberOfToursPerDay(numberOfTours);
			for (int j = 0; j < 5; j++) {
				_setNumberOfToursPerDay[j].setChecked((j == numberOfTours));
			}
//			if (null != _setTourSizeDynamic) {
//				_setTourSizeDynamic.setEnabled(numberOfTours != 0);
//			}
		};
	}

	class TourInfoFormatAction extends Action {

		TourInfoFormatter	formatter;
		int					forLine;

		TourInfoFormatAction(final String text, final TourInfoFormatter formatter, final int forLine) {

			super(text, AS_RADIO_BUTTON);
			this.formatter = formatter;
			this.forLine = forLine;
		}

		@Override
		public void run() {
			_calendarGraph.setTourInfoFormatter(forLine, formatter);
			for (int i = 0; i < _tourInfoFormatter.length; i++) {
				_setTourInfoFormat[forLine][i].setChecked(i == formatter.index);
			}
		}
	}

	class TourInfoFormatLineAction extends Action implements IMenuCreator {

		int		line;
		Menu	formatMenu;

		TourInfoFormatLineAction(final String text, final int line) {

			super(text, AS_DROP_DOWN_MENU);
			this.line = line;

			setMenuCreator(this);
		}

		@Override
		public void dispose() {
			if (formatMenu != null) {
				formatMenu.dispose();
				formatMenu = null;
			}
		}

		@Override
		public Menu getMenu(final Control parent) {
			return null;
		}

		@Override
		public Menu getMenu(final Menu parent) {
			formatMenu = new Menu(parent);

			for (int i = 0; i < _tourInfoFormatter.length; i++) {
				final ActionContributionItem item = new ActionContributionItem(_setTourInfoFormat[line][i]);
				item.fill(formatMenu, -1);
			}

			return formatMenu;
		}

		@Override
		public void run() {
			//
		}

	}

	abstract class TourInfoFormatter {
		int	index;

		abstract String format(CalendarTourData data);

		abstract String getText();
	}

	class WeekSummaryFormatAction extends Action {

		WeekSummaryFormatter	formatter;
		int					forLine;

		WeekSummaryFormatAction(final String text, final WeekSummaryFormatter formatter, final int forLine) {

			super(text, AS_RADIO_BUTTON);
			this.formatter = formatter;
			this.forLine = forLine;
		}

		@Override
		public void run() {
			_calendarGraph.setWeekSummaryFormatter(forLine, formatter);
			for (int i = 0; i < _tourWeekSummaryFormatter.length; i++) {
				_setWeekSummaryFormat[forLine][i].setChecked(i == formatter.index);
			}
		}
	}

	class WeekSummaryFormatLineAction extends Action implements IMenuCreator {

		int		line;
		Menu	summaryMenu;

		WeekSummaryFormatLineAction(final String text, final int line) {

			super(text, AS_DROP_DOWN_MENU);
			this.line = line;

			setMenuCreator(this);
		}

		@Override
		public void dispose() {
			if (summaryMenu != null) {
				summaryMenu.dispose();
				summaryMenu = null;
			}
		}

		@Override
		public Menu getMenu(final Control parent) {
			return null;
		}

		@Override
		public Menu getMenu(final Menu parent) {
			summaryMenu = new Menu(parent);

			for (int i = 0; i < _tourWeekSummaryFormatter.length; i++) {
				final ActionContributionItem item = new ActionContributionItem(_setWeekSummaryFormat[line][i]);
				item.fill(summaryMenu, -1);
			}

			return summaryMenu;
		}

		@Override
		public void run() {
			//
		}

	}

	abstract class WeekSummaryFormatter {

		int	index;
		ColorDefinition	cd;

		WeekSummaryFormatter(final String colorName) {
			cd = new GraphColorProvider().getGraphColorDefinition(colorName);
		}

		abstract String format(CalendarTourData data);

		RGB getColor() {
			if (_useLineColorForWeekSummary) {
				return cd.getLineColor();
			} else {
				return cd.getTextColor();
				// return new RGB(64, 64, 64); // 0x404040
			}
		}

		String getText() {
			return cd.getVisibleName();
		}
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
		getViewSite().getPage().addPartListener(_partListener);
	}

	private void addPropListener() {

		_propChangeListener = new IPropertyChangeListener() {

			public void propertyChange(final PropertyChangeEvent event) {

				final String property = event.getProperty();

				if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)) {
					refreshCalendar();
				} else if (property.equals(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK)) {
					refreshCalendar();
				} else if (property.equals(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK)) {
					refreshCalendar();
				} else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

					_calendarGraph.updateTourTypeColors();

					refreshCalendar();
				}
			}

		};

		// add prop listener
		_prefStore.addPropertyChangeListener(_propChangeListener);

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
			public void selectionChanged(final CalendarGraph.Selection selection) {
				if (selection.isTour()) {
					_selectionProvider.setSelection(new SelectionTourId(selection.id));
				}
			}

		});
	}

	private void addTourEventListener() {

		_tourPropertyListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (eventId == TourEventId.TOUR_CHANGED || eventId == TourEventId.UPDATE_UI) {
					/*
					 * it is possible when a tour type was modified, the tour can be hidden or
					 * visible in the viewer because of the tour type filter
					 */
					refreshCalendar();

				} else if (eventId == TourEventId.TAG_STRUCTURE_CHANGED
						|| eventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

					refreshCalendar();
				}
			}
		};
		TourManager.getInstance().addTourEventListener(_tourPropertyListener);
	}

	private void contributeToActionBars() {
		final IActionBars bars = getViewSite().getActionBars();
		final IMenuManager menuManager = bars.getMenuManager();
		menuManager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(final IMenuManager manager) {
				fillLocalPullDown(manager);
			}
		});
		fillLocalPullDown(menuManager);
		menuManager.setRemoveAllWhenShown(true);
		fillLocalToolBar(bars.getToolBarManager());
	}

	@Override
	public void createPartControl(final Composite parent) {

		addPartListener();
		addPropListener();
		addTourEventListener();

		createUI(parent);

		makeActions();
		contributeToActionBars();

		addSelectionListener();
		addSelectionProvider();

		restoreState();

		// restore selection
		onSelectionChanged(getSite().getWorkbenchWindow().getSelectionService().getSelection());

		// final Menu contextMenu = TourContextMenu.getInstance().createContextMenu(this, _calendarGraph);
		final Menu contextMenu = (new TourContextMenu()).createContextMenu(this, _calendarGraph, getLocalActions());

		_calendarGraph.setMenu(contextMenu);

	}

	private void createUI(final Composite parent) {

		_pageBook = new PageBook(parent, SWT.NONE);
		_calendarComponents = new CalendarComponents(_pageBook, SWT.NORMAL);
		_calendarGraph = _calendarComponents.getGraph();
		_pageBook.showPage(_calendarComponents);
	}

	@Override
	public void dispose() {

		TourManager.getInstance().removeTourEventListener(_tourPropertyListener);
		getSite().getPage().removePostSelectionListener(_selectionListener);
		_prefStore.removePropertyChangeListener(_propChangeListener);

		super.dispose();
	}

	private void fillLocalPullDown(final IMenuManager manager) {

		for (final Action element : _setSummaryFormatLine) {
			manager.add(element);
		}
		manager.add(new Separator());
		manager.add(_setUseLineColorForWeekSummary);
		manager.add(new Separator());

		if (_calendarGraph.isTinyLayout()) {

			manager.add(_setShowDayNumberInTinyLayout);

		} else {

			for (final Action element : _setTourInfoFormatLine) {
				manager.add(element);
			}

			manager.add(new Separator());
			for (final Action element : _setNumberOfToursPerDay) {
				manager.add(element);
			}
//			manager.add(new Separator());
//			manager.add(_setTourSizeDynamic);
//			manager.add(new Separator());
//			manager.add(_setNavigationStylePhysical);
//			manager.add(_setNavigationStyleLogical);
//			manager.add(new Separator());
			manager.add(_setTourInfoTextColor);
			manager.add(_setTourInfoBlackTextHighlight);

		}

	}

	private void fillLocalToolBar(final IToolBarManager manager) {
		_cymci = new CalendarYearMonthContributionItem(_calendarGraph);
		_calendarGraph.setYearMonthContributor(_cymci);
		manager.add(_cymci);
		manager.add(new Separator());
		// manager.add(_back);
		// manager.add(_forward);
		manager.add(_gotoToday);
		manager.add(new Separator());
		manager.add(_zoomIn);
		manager.add(_zoomOut);
		manager.add(new Separator());
		manager.add(_setLinked);
	}

	private ArrayList<Action> getLocalActions() {
		final ArrayList<Action> localActions = new ArrayList<Action>();
		localActions.add(_back);
		localActions.add(_gotoToday);
		localActions.add(_forward);
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

	private void makeActions() {

		_back = new Action() {
			@Override
			public void run() {
				_calendarGraph.gotoPrevScreen();
			}
		};
		_back.setId("net.tourbook.calendar.back"); //$NON-NLS-1$
		_back.setText(Messages.Calendar_View_Action_Back);
		_back.setToolTipText(Messages.Calendar_View_Action_Back_Tooltip);
		_back.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowDown));

		_forward = new Action() {
			@Override
			public void run() {
				_calendarGraph.gotoNextScreen();
			}
		};
		_forward.setText(Messages.Calendar_View_Action_Forward);
		_forward.setToolTipText(Messages.Calendar_View_Action_Forward_Tooltip);
		_forward.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ArrowUp));

		_zoomOut = new Action() {
			@Override
			public void run() {
				_calendarGraph.zoomOut();
			}
		};
		_zoomOut.setText(Messages.Calendar_View_Action_ZoomOut);
		_zoomOut.setToolTipText(Messages.Calendar_View_Action_ZoomOut_Tooltip);
		_zoomOut.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZoomOut));

		_zoomIn = new Action() {
			@Override
			public void run() {
				_calendarGraph.zoomIn();
			}
		};
		_zoomIn.setText(Messages.Calendar_View_Action_ZoomIn);
		_zoomIn.setToolTipText(Messages.Calendar_View_Action_ZoomIn_Tooltip);
		_zoomIn.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZoomIn));

		_setLinked = new Action(null, org.eclipse.jface.action.Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_calendarGraph.setLinked(_setLinked.isChecked());
			}
		};
		_setLinked.setText(Messages.Calendar_View_Action_LinkWithOtherViews);
		_setLinked.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__Synced));
		_setLinked.setChecked(true);

		_gotoToday = new Action() {
			@Override
			public void run() {
				_calendarGraph.gotoToday();
			}
		};
		_gotoToday.setText(Messages.Calendar_View_Action_GotoToday);
		_gotoToday.setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__ZoomCentered));

		_setNavigationStylePhysical = new Action(null, org.eclipse.jface.action.Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				_setNavigationStyleLogical.setChecked(false);
				_calendarGraph.setNavigationStyle(NavigationStyle.PHYSICAL);
			}
		};
		_setNavigationStylePhysical.setText(Messages.Calendar_View_Action_PhysicalNavigation);
		_setNavigationStylePhysical.setChecked(true);

		_setNavigationStyleLogical = new Action(null, org.eclipse.jface.action.Action.AS_RADIO_BUTTON) {
			@Override
			public void run() {
				_setNavigationStylePhysical.setChecked(false);
				_calendarGraph.setNavigationStyle(NavigationStyle.LOGICAL);
			}
		};
		_setNavigationStyleLogical.setText(Messages.Calendar_View_Action_LogicalNavigation);
		_setNavigationStyleLogical.setChecked(false);

		_setNumberOfToursPerDay = new Action[5];
		for (int i = 0; i < 5; i++) {
			_setNumberOfToursPerDay[i] = new NumberOfToursPerDayAction(i);
		}

//		_setTourSizeDynamic = new Action(null, org.eclipse.jface.action.Action.AS_CHECK_BOX) {
//			@Override
//			public void run() {
//				_calendarGraph.setTourFieldSizeDynamic(this.isChecked());
//			}
//		};
//		_setTourSizeDynamic.setText(Messages.Calendar_View_Action_ResizeTours);

		// the tour info line popup menu opener
		_setTourInfoFormatLine = new Action[numberOfInfoLines];
		for (int i = 0; i < numberOfInfoLines; i++) {
			_setTourInfoFormatLine[i] = new TourInfoFormatLineAction(NLS.bind(
					Messages.Calendar_View_Action_LineInfo,
					i + 1), i);
		}
		
		// the formatter actions used for all tour info lines
		_setTourInfoFormat = new Action[numberOfInfoLines][_tourInfoFormatter.length];
		for (int i = 0; i < numberOfInfoLines; i++) {
			for (int j = 0; j < _tourInfoFormatter.length; j++) {
				_tourInfoFormatter[j].index = j;
				if (null != _tourInfoFormatter[j]) {
					_setTourInfoFormat[i][j] = new TourInfoFormatAction(
							_tourInfoFormatter[j].getText(),
							_tourInfoFormatter[j],
							i);
				}
			}
		}

		// the week info line popup menu opener
		_setSummaryFormatLine = new Action[numberOfSummaryLines];
		for (int i = 0; i < numberOfSummaryLines; i++) {
			_setSummaryFormatLine[i] = new WeekSummaryFormatLineAction(NLS.bind(
					Messages.Calendar_View_Action_SummaryInfo,
					i + 1), i);
		}

		// the formatter actions used for the week summaries
		_setWeekSummaryFormat = new Action[numberOfSummaryLines][_tourWeekSummaryFormatter.length];
		for (int i = 0; i < numberOfSummaryLines; i++) {
			for (int j = 0; j < _tourWeekSummaryFormatter.length; j++) {
				_tourWeekSummaryFormatter[j].index = j;
				if (null != _tourWeekSummaryFormatter[j]) {
					_setWeekSummaryFormat[i][j] = new WeekSummaryFormatAction(
							_tourWeekSummaryFormatter[j].getText(),
							_tourWeekSummaryFormatter[j],
							i);
				}
			}
		}

		_setTourInfoTextColor = new Action(null, org.eclipse.jface.action.Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_calendarGraph.setTourInfoUseLineColor(this.isChecked());
			}
		};
		_setTourInfoTextColor.setText(Messages.Calendar_View_Action_TextColor);

		_setTourInfoBlackTextHighlight = new Action(null, org.eclipse.jface.action.Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_calendarGraph.setTourInfoUseHighlightTextBlack(this.isChecked());
			}
		};
		_setTourInfoBlackTextHighlight.setText(Messages.Calendar_View_Action_BlackHighlightText);

		_setShowDayNumberInTinyLayout = new Action(null, org.eclipse.jface.action.Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_calendarGraph.setShowDayNumberInTinyView(this.isChecked());
			}
		};
		_setShowDayNumberInTinyLayout.setText(Messages.Calendar_View_Action_ShowDayNumberInTinyView);

		_setUseLineColorForWeekSummary = new Action(null, org.eclipse.jface.action.Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				_useLineColorForWeekSummary = this.isChecked();
				_calendarGraph.draw();
			}
		};
		_setUseLineColorForWeekSummary.setText(Messages.Calendar_View_Action_UseLineColorForSummary);
	}

	private void onSelectionChanged(final ISelection selection) {

		// show and select the selected tour
		if (selection instanceof SelectionTourId) {
			final Long newTourId = ((SelectionTourId) selection).getTourId();
			final Long oldTourId = _calendarGraph.getSelectedTourId();
			if (newTourId != oldTourId) {
				if (_setLinked.isChecked()) {
					_calendarGraph.gotoTourId(newTourId);
				} else {
					_calendarGraph.removeSelection();
				}
			}
		} else if (selection instanceof SelectionDeletedTours) {
			_calendarGraph.refreshCalendar();
		}
	}

	private void refreshCalendar() {
		if (null != _calendarGraph) {
			_calendarGraph.refreshCalendar();
		}
	}

	private void restoreState() {

		_calendarGraph.setNumWeeksNormalLayout(Util.getStateInt(_state, STATE_NUM_OF_WEEKS_NORMAL, 5));

		_calendarGraph.setNumWeeksTinyLayout(Util.getStateInt(_state, STATE_NUM_OF_WEEKS_TINY, 15));

		final Long dateTimeMillis = Util.getStateLong(_state, STATE_FIRST_DAY, (new DateTime()).getMillis());
		final DateTime firstDate = new DateTime(dateTimeMillis);
		_calendarGraph.setFirstDay(firstDate);

		final Long selectedTourId = Util.getStateLong(_state, STATE_SELECTED_TOURS, new Long(-1));
		_calendarGraph.setSelectionTourId(selectedTourId);

//		final String[] selectedTourIds = _state.getArray(STATE_SELECTED_TOURS);
//		_selectedTourIds.clear();
//
//		if (selectedTourIds != null) {
//			for (final String tourId : selectedTourIds) {
//				try {
//					_selectedTourIds.add(Long.valueOf(tourId));
//				} catch (final NumberFormatException e) {
//					// ignore
//				}
//			}
//		}

		_setLinked.setChecked(Util.getStateBoolean(_state, STATE_IS_LINKED, false));

		// _setTourSizeDynamic.setChecked(Util.getStateBoolean(_state, STATE_TOUR_SIZE_DYNAMIC, true));

		final int numberOfTours = Util.getStateInt(_state, STATE_NUMBER_OF_TOURS_PER_DAY, 3);
		if (numberOfTours < _setNumberOfToursPerDay.length) {
			_setNumberOfToursPerDay[numberOfTours].run();
		}

		for (int i = 0; i < numberOfInfoLines; i++) {
			final int tourInfoFormatterIndex = Util.getStateInt(_state, STATE_TOUR_INFO_FORMATTER_INDEX_ + i, i + 1); // the 0. line has the 1. entry selected, the 1. line the 2. ...
			_setTourInfoFormat[i][tourInfoFormatterIndex].run();
		}

		for (int i = 0; i < numberOfSummaryLines; i++) {
			final int weekSummaryFormatterIndex = Util.getStateInt(
					_state,
					STATE_WEEK_SUMMARY_FORMATTER_INDEX_ + i,
					i + 1);
			_setWeekSummaryFormat[i][weekSummaryFormatterIndex].run();
		}

		final boolean useTextColorForTourInfo = Util.getStateBoolean(_state, STATE_TOUR_INFO_TEXT_COLOR, false);
		_setTourInfoTextColor.setChecked(useTextColorForTourInfo);
		_setTourInfoTextColor.run();

		final boolean useBlackForTextHightlight = Util.getStateBoolean(
				_state,
				STATE_TOUR_INFO_BLACK_TEXT_HIGHLIGHT,
				false);
		_setTourInfoBlackTextHighlight.setChecked(useBlackForTextHightlight);
		_setTourInfoBlackTextHighlight.run();

		final boolean showDayNumberInTinyView = Util.getStateBoolean(
				_state,
				STATE_SHOW_DAY_NUMBER_IN_TINY_LAYOUT,
				false);
		_setShowDayNumberInTinyLayout.setChecked(showDayNumberInTinyView);
		_setShowDayNumberInTinyLayout.run();

		_useLineColorForWeekSummary = Util.getStateBoolean(_state, STATE_USE_LINE_COLOR_FOR_WEEK_SUMMARY, false);
		_setUseLineColorForWeekSummary.setChecked(_useLineColorForWeekSummary);

	}

	private void saveState() {

		// save current date displayed
		_state.put(STATE_FIRST_DAY, _calendarGraph.getFirstDay().getMillis());

		// save number of weeks displayed
		_state.put(STATE_NUM_OF_WEEKS_NORMAL, _calendarGraph.getNumWeeksNormalLayout());
		_state.put(STATE_NUM_OF_WEEKS_TINY, _calendarGraph.getNumWeeksTinyLayout());

		// convert tour id's into string
		// final ArrayList<String> selectedTourIds = new ArrayList<String>();
		// for (final Long tourId : _selectedTourIds) {
		// 	selectedTourIds.add(tourId.toString());
		// }
		// until now we only implement single tour selection
		_state.put(STATE_SELECTED_TOURS, _calendarGraph.getSelectedTourId());

		_state.put(STATE_IS_LINKED, _setLinked.isChecked());
		// _state.put(STATE_TOUR_SIZE_DYNAMIC, _setTourSizeDynamic.isChecked());

		_state.put(STATE_NUMBER_OF_TOURS_PER_DAY, _calendarGraph.getNumberOfToursPerDay());

		for (int i = 0; i < numberOfInfoLines; i++) {
			_state.put(STATE_TOUR_INFO_FORMATTER_INDEX_ + i, _calendarGraph.getTourInfoFormatterIndex(i));
		}

		for (int i = 0; i < numberOfSummaryLines; i++) {
			_state.put(STATE_WEEK_SUMMARY_FORMATTER_INDEX_ + i, _calendarGraph.getWeekSummaryFormatter(i));
		}

		_state.put(STATE_TOUR_INFO_TEXT_COLOR, _calendarGraph.getTourInfoUseTextColor());
		_state.put(STATE_TOUR_INFO_BLACK_TEXT_HIGHLIGHT, _calendarGraph.getTourInfoUseHighlightTextBlack());
		_state.put(STATE_SHOW_DAY_NUMBER_IN_TINY_LAYOUT, _calendarGraph.getShowDayNumberInTinyView());

		_state.put(STATE_USE_LINE_COLOR_FOR_WEEK_SUMMARY, _useLineColorForWeekSummary);
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus() {
		_calendarComponents.setFocus();
	}

//	private void showMessage(final String message) {
//		MessageDialog.openInformation(_pageBook.getShell(), "%view_name_Calendar", message);
//	}

}
