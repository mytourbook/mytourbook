/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.view;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.tooltip.AdvancedSlideout;
import net.tourbook.common.ui.IChangeUIListener;
import net.tourbook.tour.filter.TourFilterFieldOperator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

public class TourPauseUI implements IColorSelectorListener {

   /**
    * Filter operator MUST be in sync with filter labels
    */
   private static TourFilterFieldOperator[] _allDurationOperator_Value = {

         TourFilterFieldOperator.LESS_THAN_OR_EQUAL,
         TourFilterFieldOperator.GREATER_THAN_OR_EQUAL,
         TourFilterFieldOperator.EQUALS,
         TourFilterFieldOperator.NOT_EQUALS,

   };

   /**
    * Filter labels MUST be in sync with filter operator
    */
   private static String[]                  _allDurationOperator_Label = {

         Messages.Tour_Filter_Operator_LessThanOrEqual,
         Messages.Tour_Filter_Operator_GreaterThanOrEqual,
         Messages.Tour_Filter_Operator_Equals,
         Messages.Tour_Filter_Operator_NotEquals,

   };

   private IChangeUIListener                _changeUIListener;
   private SelectionListener                _defaultSelectionListener;
   private MouseWheelListener               _defaultMouseWheelListener;
   private FocusListener                    _keepOpenListener;

   private int                              _firstColumnIndent;
   private GridDataFactory                  _firstColumnLayoutData;
   private GridDataFactory                  _secondColumnLayoutData;

   private Action_ResetValue                _actionResetValue_Hours;
   private Action_ResetValue                _actionResetValue_Minutes;
   private Action_ResetValue                _actionResetValue_Seconds;

   private PixelConverter                   _pc;

   private AdvancedSlideout                 _slideout;

   private boolean                          _isShowTourPauses;

   /*
    * UI controls
    */
   private Button  _chkIsFilter_TourPauses;
   private Button  _chkIsShow_AutoPauses;
   private Button  _chkIsFilter_PauseDuration;
   private Button  _chkIsShow_UserPauses;
   private Button  _chkUseDurationFilter_Hours;
   private Button  _chkUseDurationFilter_Minutes;
   private Button  _chkUseDurationFilter_Seconds;

   private Combo   _comboPauseFilter_Duration;

   private Spinner _spinnerHours;
   private Spinner _spinnerMinutes;
   private Spinner _spinnerSeconds;

   private Label   _lblPauseDuration;

   /**
    * Reset spinner value
    */
   private class Action_ResetValue extends Action {

      private Spinner _spinner;

      public Action_ResetValue(final Spinner spinner) {

         super(UI.RESET_LABEL, AS_PUSH_BUTTON);

         setToolTipText(Messages.Slideout_SensorTourFilter_Action_ResetValue_Tooltip);

         _spinner = spinner;
      }

      @Override
      public void run() {

         onResetValue(_spinner);
      }
   }

   public TourPauseUI(final AdvancedSlideout slideout, final IChangeUIListener changeUIListener) {

      _slideout = slideout;
      _changeUIListener = changeUIListener;
   }

   @Override
   public void colorDialogOpened(final boolean isDialogOpened) {

      _slideout.setIsAnotherDialogOpened(isDialogOpened);
   }

   private void createActions() {

   }

   public Composite createContent(final Composite parent) {

      initUI(parent);

      createActions();

      final Composite ui = createUI(parent);

      setupUI();

      restoreState();
      updateUI();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {

         createUI_10_TourPauseFilter(container);
         createUI_20_DurationFilter(container);
      }

      return container;
   }

   private void createUI_10_TourPauseFilter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Filter tour pauses
             */
            _chkIsFilter_TourPauses = new Button(container, SWT.CHECK);
            _chkIsFilter_TourPauses.setText(Messages.Tour_Pauses_Checkbox_TourPauseFilter);
            _chkIsFilter_TourPauses.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkIsFilter_TourPauses);
         }
         {
            /*
             * Pauses Filter: Show auto pause
             */
            _chkIsShow_AutoPauses = new Button(container, SWT.CHECK);
            _chkIsShow_AutoPauses.setText(Messages.Tour_Pauses_Checkbox_ShowAutoPauses);
            _chkIsShow_AutoPauses.addSelectionListener(_defaultSelectionListener);
            _firstColumnLayoutData.span(2, 1).applyTo(_chkIsShow_AutoPauses);
         }
         {
            /*
             * Pauses Filter: Show pauses started/stopped by the user
             */
            _chkIsShow_UserPauses = new Button(container, SWT.CHECK);
            _chkIsShow_UserPauses.setText(Messages.Tour_Pauses_Checkbox_ShowUserPauses);
            _chkIsShow_UserPauses.setToolTipText(Messages.Tour_Pauses_Checkbox_ShowUserPauses_Tooltip);
            _chkIsShow_UserPauses.addSelectionListener(_defaultSelectionListener);
            _firstColumnLayoutData.span(2, 1).applyTo(_chkIsShow_UserPauses);
         }
         {
            /*
             * Pauses Filter: Duration
             */
            _chkIsFilter_PauseDuration = new Button(container, SWT.CHECK);
            _chkIsFilter_PauseDuration.setText(Messages.Tour_Pauses_Checkbox_PauseDurationFilter);
            _chkIsFilter_PauseDuration.addSelectionListener(_defaultSelectionListener);
            _firstColumnLayoutData.applyTo(_chkIsFilter_PauseDuration);

            final Composite containerDuration = new Composite(parent, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerDuration);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerDuration);
            {
               // combo
               _comboPauseFilter_Duration = new Combo(containerDuration, SWT.DROP_DOWN | SWT.READ_ONLY);
               _comboPauseFilter_Duration.setVisibleItemCount(5);
               _comboPauseFilter_Duration.addSelectionListener(_defaultSelectionListener);
               _comboPauseFilter_Duration.addFocusListener(_keepOpenListener);
               _secondColumnLayoutData.applyTo(_comboPauseFilter_Duration);

               // label: Show time in hh:mm:ss
               _lblPauseDuration = new Label(containerDuration, SWT.NONE);
               GridDataFactory.fillDefaults()
                     .grab(true, false)
                     .align(SWT.BEGINNING, SWT.CENTER)
                     .applyTo(_lblPauseDuration);
            }
         }
      }
   }

   private void createUI_20_DurationFilter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Seconds
             */

            // radio
            _chkUseDurationFilter_Seconds = new Button(container, SWT.CHECK);
            _chkUseDurationFilter_Seconds.setText(Messages.Tour_Pauses_Checkbox_Duration_Seconds);
            _chkUseDurationFilter_Seconds.addSelectionListener(_defaultSelectionListener);
            _secondColumnLayoutData.applyTo(_chkUseDurationFilter_Seconds);

            // spinner
            _spinnerSeconds = new Spinner(container, SWT.BORDER);
            _spinnerSeconds.setMinimum(0);
            _spinnerSeconds.setMaximum(999);
            _spinnerSeconds.addSelectionListener(_defaultSelectionListener);
            _spinnerSeconds.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Seconds = createUI_Action_ResetValue(container, _spinnerSeconds);
         }
         {
            /*
             * Minutes
             */

            // radio
            _chkUseDurationFilter_Minutes = new Button(container, SWT.CHECK);
            _chkUseDurationFilter_Minutes.setText(Messages.Tour_Pauses_Checkbox_Duration_Minutes);
            _chkUseDurationFilter_Minutes.addSelectionListener(_defaultSelectionListener);
            _secondColumnLayoutData.applyTo(_chkUseDurationFilter_Minutes);

            // spinner
            _spinnerMinutes = new Spinner(container, SWT.BORDER);
            _spinnerMinutes.setMinimum(0);
            _spinnerMinutes.setMaximum(999);
            _spinnerMinutes.addSelectionListener(_defaultSelectionListener);
            _spinnerMinutes.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Minutes = createUI_Action_ResetValue(container, _spinnerMinutes);
         }
         {
            /*
             * Hours
             */

            // radio
            _chkUseDurationFilter_Hours = new Button(container, SWT.CHECK);
            _chkUseDurationFilter_Hours.setText(Messages.Tour_Pauses_Checkbox_Duration_Hours);
            _chkUseDurationFilter_Hours.addSelectionListener(_defaultSelectionListener);
            _secondColumnLayoutData.applyTo(_chkUseDurationFilter_Hours);

            // spinner
            _spinnerHours = new Spinner(container, SWT.BORDER);
            _spinnerHours.setMinimum(0);
            _spinnerHours.setMaximum(999);
            _spinnerHours.addSelectionListener(_defaultSelectionListener);
            _spinnerHours.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Hours = createUI_Action_ResetValue(container, _spinnerHours);
         }
      }
   }

   private Action_ResetValue createUI_Action_ResetValue(final Composite parent, final Spinner spinner) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults().applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      final Action_ResetValue action = new Action_ResetValue(spinner);

      tbm.add(action);

      tbm.update(true);

      return action;
   }

   private void enableControls() {

      enableControls(_isShowTourPauses);
   }

   public void enableControls(final boolean isShowTourPauses) {

      _isShowTourPauses = isShowTourPauses;

// SET_FORMATTING_OFF

      final boolean isFilterTourPauses       = _chkIsFilter_TourPauses     .getSelection();
      final boolean isShowAutoPauses         = _chkIsShow_AutoPauses       .getSelection();
      final boolean isShowUserPauses         = _chkIsShow_UserPauses       .getSelection();
      final boolean isDurationFilter         = _chkIsFilter_PauseDuration  .getSelection();
      final boolean isPausesFilter           = isShowTourPauses && isFilterTourPauses;
      final boolean isPausesDurationFilter   = isShowTourPauses && isFilterTourPauses && isDurationFilter
                                                                 && (isShowAutoPauses || isShowUserPauses);

      final boolean isHours      = _chkUseDurationFilter_Hours.getSelection()   && isPausesDurationFilter;
      final boolean isMinutes    = _chkUseDurationFilter_Minutes.getSelection() && isPausesDurationFilter;
      final boolean isSeconds    = _chkUseDurationFilter_Seconds.getSelection() && isPausesDurationFilter;

      final int durationDays     = _spinnerHours.getSelection();
      final int durationMonths   = _spinnerMinutes.getSelection();
      final int durationYears    = _spinnerSeconds.getSelection();

      _chkIsFilter_TourPauses       .setEnabled(isShowTourPauses);
      _chkIsFilter_PauseDuration    .setEnabled(isPausesFilter && (isShowAutoPauses || isShowUserPauses));
      _chkIsShow_AutoPauses         .setEnabled(isPausesFilter);
      _chkIsShow_UserPauses         .setEnabled(isPausesFilter);

      _comboPauseFilter_Duration    .setEnabled(isPausesFilter && isPausesDurationFilter);

      _lblPauseDuration             .setEnabled(isPausesDurationFilter);

      _chkUseDurationFilter_Hours   .setEnabled(isPausesDurationFilter);
      _chkUseDurationFilter_Minutes .setEnabled(isPausesDurationFilter);
      _chkUseDurationFilter_Seconds .setEnabled(isPausesDurationFilter);

      _spinnerHours                 .setEnabled(isHours);
      _spinnerMinutes               .setEnabled(isMinutes);
      _spinnerSeconds               .setEnabled(isSeconds);

      _actionResetValue_Hours       .setEnabled(isHours && durationDays > 0);
      _actionResetValue_Minutes     .setEnabled(isMinutes && durationMonths > 0);
      _actionResetValue_Seconds     .setEnabled(isSeconds && durationYears > 0);

// SET_FORMATTING_ON
   }

   private TourFilterFieldOperator getSelectedPauseDurationOperator() {

      final int selectedIndex = _comboPauseFilter_Duration.getSelectionIndex();

      if (selectedIndex >= 0) {
         return _allDurationOperator_Value[selectedIndex];
      } else {
         return Map2ConfigManager.DURATION_OPERATOR_DEFAULT;
      }
   }

   private long getSummarizedDuration() {

      int durationValue = 0;

      if (_chkUseDurationFilter_Seconds.getSelection()) {
         durationValue += _spinnerSeconds.getSelection();
      }
      if (_chkUseDurationFilter_Minutes.getSelection()) {
         durationValue += _spinnerMinutes.getSelection() * 60;
      }
      if (_chkUseDurationFilter_Hours.getSelection()) {
         durationValue += _spinnerHours.getSelection() * 3600;
      }

      return durationValue;
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };

      _keepOpenListener = new FocusListener() {

         @Override
         public void focusGained(final FocusEvent e) {

            /*
             * This will fix the problem that when the list of a combobox is displayed, then the
             * slideout will disappear :-(((
             */
            _slideout.setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            _slideout.setIsAnotherDialogOpened(false);
         }
      };

      _firstColumnIndent = _pc.convertWidthInCharsToPixels(3);

      _firstColumnLayoutData = GridDataFactory.fillDefaults()
            .indent(_firstColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);

      _secondColumnLayoutData = GridDataFactory.fillDefaults()
            .indent(2 * _firstColumnIndent, 0)
            .align(SWT.FILL, SWT.CENTER);
   }

   private void onChangeUI() {

      updateUI();

      saveState();

      enableControls();

      _changeUIListener.onChangeUI_External();
   }

   private void onResetValue(final Spinner spinner) {

      spinner.setSelection(0);
      spinner.setFocus();

      onChangeUI();
   }

   void resetToDefaults() {

// SET_FORMATTING_OFF

      _chkIsFilter_TourPauses.setSelection(        Map2ConfigManager.IS_FILTER_TOUR_PAUSES_DEFAULT);
      _chkIsFilter_PauseDuration.setSelection(     Map2ConfigManager.IS_FILTER_PAUSE_DURATION_DEFAULT);

      _chkIsShow_AutoPauses.setSelection(          Map2ConfigManager.IS_SHOW_AUTO_PAUSES_DEFAULT);
      _chkIsShow_UserPauses.setSelection(          Map2ConfigManager.IS_SHOW_USER_PAUSES_DEFAULT);

      _chkUseDurationFilter_Hours.setSelection(    Map2ConfigManager.USE_DURATION_FILTER_HOURS_DEFAULT);
      _chkUseDurationFilter_Minutes.setSelection(  Map2ConfigManager.USE_DURATION_FILTER_MINUTES_DEFAULT);
      _chkUseDurationFilter_Seconds.setSelection(  Map2ConfigManager.USE_DURATION_FILTER_SECONDS_DEFAULT);

      _spinnerHours.setSelection(                  Map2ConfigManager.DURATION_FILTER_HOURS_DEFAULT);
      _spinnerMinutes.setSelection(                Map2ConfigManager.DURATION_FILTER_MINUTES_DEFAULT);
      _spinnerSeconds.setSelection(                Map2ConfigManager.DURATION_FILTER_SECONDS_DEFAULT);

// SET_FORMATTING_ON

      selectPauseDurationOperator(Map2ConfigManager.DURATION_OPERATOR_DEFAULT);

      updateUI();
      enableControls();
   }

   void restoreState() {

      final Map2Config config = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      _chkIsFilter_TourPauses.setSelection(        config.isFilterTourPauses);
      _chkIsFilter_PauseDuration.setSelection(     config.isFilterTourPause_Duration);

      _chkIsShow_AutoPauses.setSelection(          config.isShowAutoPauses);
      _chkIsShow_UserPauses.setSelection(          config.isShowUserPauses);

      _chkUseDurationFilter_Hours.setSelection(    config.useTourPause_DurationFilter_Hours);
      _chkUseDurationFilter_Minutes.setSelection(  config.useTourPause_DurationFilter_Minutes);
      _chkUseDurationFilter_Seconds.setSelection(  config.useTourPause_DurationFilter_Seconds);

      _spinnerHours.setSelection(                  config.tourPauseDurationFilter_Hours);
      _spinnerMinutes.setSelection(                config.tourPauseDurationFilter_Minutes);
      _spinnerSeconds.setSelection(                config.tourPauseDurationFilter_Seconds);

// SET_FORMATTING_ON

      selectPauseDurationOperator(config.tourPauseDurationFilter_Operator);

      updateUI();
      enableControls();
   }

   private void saveState() {

      final Map2Config config = Map2ConfigManager.getActiveConfig();

// SET_FORMATTING_OFF

      config.isFilterTourPauses                    = _chkIsFilter_TourPauses.getSelection();
      config.isFilterTourPause_Duration            = _chkIsFilter_PauseDuration.getSelection();
      config.isShowAutoPauses                      = _chkIsShow_AutoPauses.getSelection();
      config.isShowUserPauses                      = _chkIsShow_UserPauses.getSelection();

      config.useTourPause_DurationFilter_Hours     = _chkUseDurationFilter_Hours.getSelection();
      config.useTourPause_DurationFilter_Minutes   = _chkUseDurationFilter_Minutes.getSelection();
      config.useTourPause_DurationFilter_Seconds   = _chkUseDurationFilter_Seconds.getSelection();

      config.tourPauseDurationFilter_Hours         = _spinnerHours.getSelection();
      config.tourPauseDurationFilter_Minutes       = _spinnerMinutes.getSelection();
      config.tourPauseDurationFilter_Seconds       = _spinnerSeconds.getSelection();

      config.tourPauseDuration                     = getSummarizedDuration();
      config.tourPauseDurationFilter_Operator      = getSelectedPauseDurationOperator();

// SET_FORMATTING_ON

   }

   private void selectPauseDurationOperator(final Enum<TourFilterFieldOperator> filterOperator) {

      int selectionIndex = 0;

      for (int operatorIndex = 0; operatorIndex < _allDurationOperator_Value.length; operatorIndex++) {

         final TourFilterFieldOperator tourFilterFieldOperator = _allDurationOperator_Value[operatorIndex];

         if (tourFilterFieldOperator.equals(filterOperator)) {
            selectionIndex = operatorIndex;
            break;
         }
      }

      _comboPauseFilter_Duration.select(selectionIndex);
   }

   private void setupUI() {

      for (final String label : _allDurationOperator_Label) {
         _comboPauseFilter_Duration.add(label);
      }
   }

   private void updateUI() {

      final boolean isDurationFilter = _chkIsFilter_PauseDuration.getSelection();

      final String durationText = isDurationFilter

            ? UI.format_hh_mm_ss(getSummarizedDuration())
            : UI.EMPTY_STRING;

      _lblPauseDuration.setText(durationText);
      _lblPauseDuration.getParent().layout(true, true);
   }
}
