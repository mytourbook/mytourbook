/*******************************************************************************
 * Copyright (C) 2011, 2023 Matthias Helmling and Contributors
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

import static org.eclipse.swt.events.ControlListener.controlResizedAdapter;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
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

import org.eclipse.e4.ui.di.PersistState;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;

public class CalendarView extends ViewPart implements ITourProvider, ICalendarProfileListener {

   /**
    * The ID of the view as specified by the extension.
    */
   public static final String      ID                              = "net.tourbook.views.calendar.CalendarView";  //$NON-NLS-1$

   private static final String     STATE_IS_LINKED                 = "STATE_IS_LINKED";                           //$NON-NLS-1$
   private static final String     STATE_IS_SHOW_TOUR_INFO         = "STATE_IS_SHOW_TOUR_INFO";                   //$NON-NLS-1$
   static final String             STATE_TOUR_TOOLTIP_DELAY        = "STATE_TOUR_TOOLTIP_DELAY";                  //$NON-NLS-1$

   private static final String     STATE_FIRST_DISPLAYED_EPOCH_DAY = "STATE_FIRST_DISPLAYED_EPOCH_DAY";           //$NON-NLS-1$
   private static final String     STATE_SELECTED_TOURS            = "STATE_SELECTED_TOURS";                      //$NON-NLS-1$

   static final int                DEFAULT_TOUR_TOOLTIP_DELAY      = 100;                                         // ms

   private final IPreferenceStore  _prefStore                      = TourbookPlugin.getPrefStore();
   private final IPreferenceStore  _prefStore_Common               = CommonActivator.getPrefStore();
   private final IDialogSettings   _state                          = TourbookPlugin.getState("TourCalendarView"); //$NON-NLS-1$

   private boolean                 _stateIsLinked;

   private ISelectionListener      _selectionListener;
   private IPartListener2          _partListener;
   private IPropertyChangeListener _prefChangeListener;
   private IPropertyChangeListener _prefChangeListener_Common;
   private ITourEventListener      _tourEventListener;

   private ActionCalendarOptions   _actionCalendarOptions;

   private Action                  _actionBack;
   private Action                  _actionForward;
   private Action                  _actionSetLinked;
   private Action                  _actionGotoToday;
   private ActionTourInfo          _actionTourInfo;

   private LocalDate               _titleFirstDay;
   private LocalDate               _titleLastDay;

   private PixelConverter          _pc;

   private CalendarTourInfoToolTip _tourInfoToolTip;
   private OpenDialogManager       _openDlgMgr                     = new OpenDialogManager();

   private ArrayList<Integer>      _allYearValues;

   /*
    * UI controls
    */
   private CalendarGraph _calendarGraph;

   private Composite     _parent;
   private Composite     _calendarContainer;
   private Composite     _headerContainer;

   private Combo         _comboMonth;
   private Combo         _comboYear;
   private Combo         _comboProfiles;

   private Label         _lblTitle;

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
               CalendarProfileManager.removeProfileListener(CalendarView.this);
            }
         }

         @Override
         public void partDeactivated(final IWorkbenchPartReference partRef) {}

         @Override
         public void partHidden(final IWorkbenchPartReference partRef) {

            if (partRef.getPart(false) == CalendarView.this) {
               onPartHidden();
            }
         }

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

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ITourbookPreferences.APP_DATA_FILTER_IS_MODIFIED)
               || property.equals(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK)
               || property.equals(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK)) {

            refreshCalendar();

         } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {

            _calendarGraph.updateTourTypeColors();

            refreshCalendar();
         }
      };

      _prefChangeListener_Common = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         if (property.equals(ICommonPreferences.MEASUREMENT_SYSTEM)) {

            refreshCalendar();
         }
      };

      // add prop listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
      _prefStore_Common.addPropertyChangeListener(_prefChangeListener_Common);
   }

   // create and register our selection listener
   private void addSelectionListener() {

      _selectionListener = (part, selection) -> {

         // prevent to listen to a selection which is originated by this year chart
         if (part == CalendarView.this) {
            return;
         }

         onSelectionChanged(selection);

      };

      // register selection listener in the page
      getSite().getPage().addPostSelectionListener(_selectionListener);
   }

   private void addTourEventListener() {

      _tourEventListener = (part, tourEventId, eventData) -> {

         if (CalendarView.this == part) {
            // skip own events
            return;
         }

         if (tourEventId == TourEventId.TOUR_CHANGED || tourEventId == TourEventId.UPDATE_UI) {
            /*
             * it is possible when a tour type was modified, the tour can be hidden or visible in
             * the viewer because of the tour type filter
             */
            refreshCalendar();

         } else if ((tourEventId == TourEventId.TOUR_SELECTION //
               || tourEventId == TourEventId.SLIDER_POSITION_CHANGED)

               && eventData instanceof ISelection) {

            onSelectionChanged((ISelection) eventData);

         } else if (tourEventId == TourEventId.TAG_STRUCTURE_CHANGED
               || tourEventId == TourEventId.ALL_TOURS_ARE_MODIFIED) {

            refreshCalendar();
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

      {
         /*
          * Back
          */
         _actionBack = new Action() {
            @Override
            public void run() {
               _calendarGraph.scroll_WithWheel_Screen(1);
            }
         };
         _actionBack.setText(Messages.Calendar_View_Action_Back);
         _actionBack.setToolTipText(Messages.Calendar_View_Action_Back_Tooltip);

         _actionBack.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.ArrowUp));
      }
      {
         /*
          * Forward
          */
         _actionForward = new Action() {
            @Override
            public void run() {
               _calendarGraph.scroll_WithWheel_Screen(-1);
            }
         };
         _actionForward.setText(Messages.Calendar_View_Action_Forward);
         _actionForward.setToolTipText(Messages.Calendar_View_Action_Forward_Tooltip);

         _actionForward.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.ArrowDown));
      }
      {
         /*
          * Link with other views
          */
         _actionSetLinked = new Action(null, Action.AS_CHECK_BOX) {
            @Override
            public void run() {
               _calendarGraph.setLinked(_stateIsLinked);
            }
         };
         _actionSetLinked.setText(Messages.Calendar_View_Action_LinkWithOtherViews);

         _actionSetLinked.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.SyncViews));
         _actionSetLinked.setChecked(true);
      }
      {
         /*
          * Go to today
          */
         _actionGotoToday = new Action() {
            @Override
            public void run() {
               _calendarGraph.gotoDate_Today();
            }
         };
         _actionGotoToday.setText(Messages.Calendar_View_Action_GotoToday);
         _actionGotoToday.setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.App_Today));
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

      fillMonthComboBox();
      fillYearComboBox();

      createActions();
      fillActionBars();

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

      parent.addControlListener(controlResizedAdapter(controlEvent -> updateUI_Title(null, null)));

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 0).applyTo(container);
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

      _headerContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_headerContainer);
      GridLayoutFactory.swtDefaults().numColumns(4).applyTo(_headerContainer);
//    container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
      {
         {
            /*
             * Title
             */
            _lblTitle = new Label(_headerContainer, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblTitle);
            MTFont.setHeaderFont(_lblTitle);
//          _lblTitle.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
         }
         {
            /*
             * Month
             */

            // combo
            _comboMonth = new Combo(_headerContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboMonth.setToolTipText(Messages.Calendar_View_Combo_Month_Tooltip);
            _comboMonth.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectDate()));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_comboMonth);
         }
         {
            /*
             * Year
             */

            // combo
            _comboYear = new Combo(_headerContainer, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboYear.setToolTipText(Messages.Calendar_View_Combo_Year_Tooltip);
            _comboYear.setVisibleItemCount(50);
            _comboYear.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectDate()));
            _comboYear.addTraverseListener(traverseEvent -> {
               if (traverseEvent.detail == SWT.TRAVERSE_RETURN) {
                  onSelectDate();
               }
            });
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(UI.IS_OSX ? 12 : UI.IS_LINUX ? 12 : 5), SWT.DEFAULT)
                  .applyTo(_comboYear);
         }
         {
            /*
             * Combo: Profiles
             */
            _comboProfiles = new Combo(_headerContainer, SWT.READ_ONLY | SWT.BORDER);
            _comboProfiles.setVisibleItemCount(30);
            _comboProfiles.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectProfile()));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .applyTo(_comboProfiles);
         }
      }
   }

   @Override
   public void dispose() {

      TourManager.getInstance().removeTourEventListener(_tourEventListener);
      getViewSite().getPage().removePartListener(_partListener);
      getSite().getPage().removePostSelectionListener(_selectionListener);

      _prefStore.removePropertyChangeListener(_prefChangeListener);
      _prefStore_Common.removePropertyChangeListener(_prefChangeListener_Common);

      super.dispose();
   }

   private void fillActionBars() {

      final IToolBarManager toolbarMrg = getViewSite().getActionBars().getToolBarManager();

      /*
       * Toolbar
       */
      toolbarMrg.add(_actionGotoToday);
      toolbarMrg.add(_actionSetLinked);
      toolbarMrg.add(_actionTourInfo);
      toolbarMrg.add(_actionCalendarOptions);
   }

   private void fillMonthComboBox() {

      LocalDate date = LocalDate.now();
      final int thisMonth = date.getMonthValue();
      date = date.withMonth(1);

      for (int monthIndex = 0; monthIndex < 12; monthIndex++) {

         _comboMonth.add(TimeTools.Formatter_Month.format(date));

         date = date.plusMonths(1);
      }

      // select this month
      _comboMonth.select(thisMonth - 1);

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

         _comboProfiles.add(CalendarProfileManager.getProfileName(profile, profile.profileName));

         // get index for active profile
         if (activeProfile.equals(profile)) {
            selectIndex = profileIndex;
         }
      }

      _comboProfiles.select(selectIndex);
   }

   private void fillYearComboBox() {

      final int thisYear = LocalDate.now().getYear();

      _allYearValues = new ArrayList<>();

      final LocalDateTime firstTourDateTime = CalendarTourDataProvider.getInstance().getFirstTourDateTime();
      final int firstYear = firstTourDateTime.getYear();

      for (int year = thisYear; year >= firstYear; year--) {

         _comboYear.add(Integer.toString(year));
         _allYearValues.add(year);
      }

      // select first year
      _comboYear.select(0);
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

      final ArrayList<Action> localActions = new ArrayList<>();

      localActions.add(_actionBack);
      localActions.add(_actionGotoToday);
      localActions.add(_actionForward);

      return localActions;

   }

   @Override
   public ArrayList<TourData> getSelectedTours() {

      final ArrayList<TourData> selectedTourData = new ArrayList<>();
      final ArrayList<Long> tourIdSet = new ArrayList<>();
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

   private void onPartHidden() {

      // hide config slideout
      final SlideoutCalendarOptions slideout = getConfigSlideout();

      slideout.close();
   }

   private void onSelectDate() {

      int yearIndex = _comboYear.getSelectionIndex();
      if (yearIndex < 0) {
         yearIndex = 0;
      }

      final int selectedYear = _allYearValues.get(yearIndex);
      final int selectedMonth = _comboMonth.getSelectionIndex() + 1;

      _calendarGraph.gotoDate(LocalDate.of(selectedYear, selectedMonth, 1), false);
   }

   private void onSelectionChanged(final ISelection selection) {

      // show and select the selected tour
      if (selection instanceof SelectionTourId) {

         final Long newTourId = ((SelectionTourId) selection).getTourId();
         final Long oldTourId = _calendarGraph.getSelectedTourId();

         if (newTourId != oldTourId) {

            _stateIsLinked = _actionSetLinked.isChecked();

            if (_stateIsLinked) {
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
//    _calendarGraph.stopDataProvider();

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

         BusyIndicator.showWhile(Display.getCurrent(), _calendarGraph::refreshCalendar);
      }
   }

   private void restoreState() {

      _stateIsLinked = Util.getStateBoolean(_state, STATE_IS_LINKED, false);
      final boolean stateIsShowTourInfo = Util.getStateBoolean(_state, STATE_IS_SHOW_TOUR_INFO, true);

      _actionSetLinked.setChecked(_stateIsLinked);
      _actionTourInfo.setSelected(stateIsShowTourInfo);

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

      final Long selectedTourId = Util.getStateLong(_state, STATE_SELECTED_TOURS, -1);
      _calendarGraph.setSelectionTourId(selectedTourId);

      // tooltip
      _tourInfoToolTip.setPopupDelay(Util.getStateInt(_state, STATE_TOUR_TOOLTIP_DELAY, DEFAULT_TOUR_TOOLTIP_DELAY));
   }

   @PersistState
   private void saveState() {

      _state.put(STATE_IS_LINKED, _stateIsLinked);
      _state.put(STATE_IS_SHOW_TOUR_INFO, _actionTourInfo.isChecked());

      // save current date displayed
      _state.put(STATE_FIRST_DISPLAYED_EPOCH_DAY, _calendarGraph.getFirstDay().toLocalDate().toEpochDay());

      // until now we only implement single tour selection
      _state.put(STATE_SELECTED_TOURS, _calendarGraph.getSelectedTourId());

      CalendarProfileManager.saveState();
   }

   void setDate(final LocalDate requestedDate, final CalendarProfile calendarProfile) {

      // disable month when year columns are used
      _comboMonth.setEnabled(
            !(calendarProfile.isShowYearColumns
                  && calendarProfile.yearColumnsStart != ColumnStart.CONTINUOUSLY));

      final int requestedYear = requestedDate.getYear();

      if (requestedYear < _allYearValues.get(_allYearValues.size() - 1)) {

         // year is before available years

         // select first date
         _comboMonth.select(0);
         _comboYear.select(0);

      } else if (requestedYear > _allYearValues.get(0)) {

         // year is after the available years

         // select last date
         _comboMonth.select(11);
         _comboYear.select(0);

      } else {

         // year is available

         for (int yearIndex = 0; yearIndex < _allYearValues.size(); yearIndex++) {

            final int currentYear = _allYearValues.get(yearIndex);

            if (currentYear == requestedYear) {

               final int requestedMonth = requestedDate.getMonthValue();

               _comboMonth.select(requestedMonth - 1);
               _comboYear.select(yearIndex);

               break;
            }
         }
      }

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

      Display.getDefault().asyncExec(() -> _calendarGraph.updateUI_Layout(true));
   }

   void updateUI_ProfileName(final CalendarProfile selectedProfile, final String modifiedProfileName) {

      // update text in the combo
      final int selectedIndex = _comboProfiles.getSelectionIndex();
      _comboProfiles.setItem(
            selectedIndex,
            CalendarProfileManager.getProfileName(selectedProfile, modifiedProfileName));
   }

   void updateUI_Title(final LocalDate calendarFirstDay, final LocalDate calendarLastDay) {

      if (calendarFirstDay != null && calendarLastDay != null) {

         if (calendarFirstDay.equals(_titleFirstDay) && calendarLastDay.equals(_titleLastDay)) {

            // these dates are already displayed
            return;
         }

         _titleFirstDay = calendarFirstDay;
         _titleLastDay = calendarLastDay;
      }

      // re-layout to get the available width
      _headerContainer.layout(true, true);

      _headerContainer.getDisplay().asyncExec(() -> {

         if (_headerContainer.isDisposed()) {
            return;
         }

         if (_titleFirstDay == null) {
            return;
         }

         // reset to default header font
         MTFont.setHeaderFont(_lblTitle);

         /*
          * Get title text
          */
         String titleText = UI.EMPTY_STRING;
         GC gc = new GC(_lblTitle);
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
                        + UI.DASH_WITH_SPACE
                        + _titleLastDay.format(TimeTools.Formatter_Date_S);

                  titleWidth = gc.stringExtent(titleText).x;

                  if (titleWidth > availableWidth) {

                     // set default font
                     _lblTitle.setFont(null);

                     gc.dispose();
                     gc = new GC(_lblTitle);

                     titleWidth = gc.stringExtent(titleText).x;

                     if (titleWidth > availableWidth) {

                        /*
                         * Force that the title is displayed the next time. There was a problem when
                         * resizing the canvas and the title was empty.
                         */
                        _titleFirstDay = null;
                     }
                  }
               }
            }
         }
         gc.dispose();

         _lblTitle.setText(titleText);

         // re-layout to center vertically the text
         _headerContainer.layout(true, true);
      });

   }

}
