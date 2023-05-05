/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart properties slideout.
 */
public class SlideoutSensorTourFilter extends ToolbarSlideout implements IActionResetToDefault {

   static final String            STATE_USE_APP_FILTER                 = "STATE_USE_APP_FILTER";         //$NON-NLS-1$
   static final boolean           STATE_USE_APP_FILTER_DEFAULT         = false;
   static final String            STATE_USE_DURATION_FILTER            = "STATE_USE_DURATION_FILTER";    //$NON-NLS-1$
   static final boolean           STATE_USE_DURATION_FILTER_DEFAULT    = false;
   static final String            STATE_USE_TOUR_FILTER_DAYS           = "STATE_USE_TOUR_FILTER_DAYS";   //$NON-NLS-1$
   static final boolean           STATE_USE_TOUR_FILTER_DAYS_DEFAULT   = false;
   static final String            STATE_USE_TOUR_FILTER_MONTHS         = "STATE_USE_TOUR_FILTER_MONTHS"; //$NON-NLS-1$
   static final boolean           STATE_USE_TOUR_FILTER_MONTHS_DEFAULT = false;
   static final String            STATE_USE_TOUR_FILTER_YEARS          = "STATE_USE_TOUR_FILTER_YEARS";  //$NON-NLS-1$
   static final boolean           STATE_USE_TOUR_FILTER_YEARS_DEFAULT  = true;

   static final String            STATE_TOUR_FILTER_DAYS               = "STATE_TOUR_FILTER_DAYS";       //$NON-NLS-1$
   static final int               STATE_TOUR_FILTER_DAYS_DEFAULT       = 1;
   static final String            STATE_TOUR_FILTER_MONTHS             = "STATE_TOUR_FILTER_MONTHS";     //$NON-NLS-1$
   static final int               STATE_TOUR_FILTER_MONTHS_DEFAULT     = 1;
   static final String            STATE_TOUR_FILTER_YEARS              = "STATE_TOUR_FILTER_YEARS";      //$NON-NLS-1$
   static final int               STATE_TOUR_FILTER_YEARS_DEFAULT      = 1;

   private static IDialogSettings _state;

   private SelectionListener      _defaultSelectionListener;
   private MouseWheelListener     _defaultMouseWheelListener;

   private Action_ResetValue      _actionResetValue_Day;
   private Action_ResetValue      _actionResetValue_Month;
   private Action_ResetValue      _actionResetValue_Year;
   private ActionResetToDefaults  _actionRestoreDefaults;

   private SensorChartView        _sensorChartView;

   /*
    * UI controls
    */
   private Button  _checkboxUseAppFilter;
   private Button  _checkboxUseFilterDuration;
   private Button  _checkboxUseTourFilter_Day;
   private Button  _checkboxUseTourFilter_Month;
   private Button  _checkboxUseTourFilter_Year;

   private Spinner _spinnerDay;
   private Spinner _spinnerMonth;
   private Spinner _spinnerYear;

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

   public SlideoutSensorTourFilter(final Composite ownerControl,
                                   final ToolBar toolbar,
                                   final SensorChartView sensorChartView,
                                   final IDialogSettings state) {

      super(ownerControl, toolbar);

      _sensorChartView = sensorChartView;

      _state = state;
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      restoreState();
      enableControls();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            final Composite titleContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(titleContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(titleContainer);
            {
               createUI_10_Title(titleContainer);
               createUI_12_Actions(titleContainer);
            }
            createUI_20_Filter(container);
         }
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_SensorTourFilter_Label_Title);
      label.setFont(JFaceResources.getBannerFont());

      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);

      tbm.update(true);
   }

   private void createUI_20_Filter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Use app filter
             */

            // checkbox
            _checkboxUseAppFilter = new Button(container, SWT.CHECK);
            _checkboxUseAppFilter.setText(Messages.Slideout_SensorTourFilter_Checkbox_IsUseAppFilter);
            _checkboxUseAppFilter.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Filter by duration
             */

            // checkbox
            _checkboxUseFilterDuration = new Button(container, SWT.CHECK);
            _checkboxUseFilterDuration.setText(Messages.Slideout_SensorTourFilter_Checkbox_IsFilterDuration);
            _checkboxUseFilterDuration.addSelectionListener(_defaultSelectionListener);

            createUI_22_DurationFilter(container);
         }
      }
   }

   private void createUI_22_DurationFilter(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(16, 0)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
      {
         {
            /*
             * Year
             */

            // radio
            _checkboxUseTourFilter_Year = new Button(container, SWT.CHECK);
            _checkboxUseTourFilter_Year.setText(Messages.Slideout_SensorTourFilter_Radio_Year);
            _checkboxUseTourFilter_Year.setToolTipText(Messages.Slideout_SensorTourFilter_Radio_Year_Tooltip);
            _checkboxUseTourFilter_Year.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerYear = new Spinner(container, SWT.BORDER);
            _spinnerYear.setMinimum(0);
            _spinnerYear.setMaximum(999);
            _spinnerYear.addSelectionListener(_defaultSelectionListener);
            _spinnerYear.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Year = createUI_Action_ResetValue(container, _spinnerYear);
         }
         {
            /*
             * Month
             */

            // radio
            _checkboxUseTourFilter_Month = new Button(container, SWT.CHECK);
            _checkboxUseTourFilter_Month.setText(Messages.Slideout_SensorTourFilter_Radio_Month);
            _checkboxUseTourFilter_Month.setToolTipText(Messages.Slideout_SensorTourFilter_Radio_Month_Tooltip);
            _checkboxUseTourFilter_Month.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerMonth = new Spinner(container, SWT.BORDER);
            _spinnerMonth.setMinimum(0);
            _spinnerMonth.setMaximum(999);
            _spinnerMonth.addSelectionListener(_defaultSelectionListener);
            _spinnerMonth.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Month = createUI_Action_ResetValue(container, _spinnerMonth);
         }
         {
            /*
             * Day
             */

            // radio
            _checkboxUseTourFilter_Day = new Button(container, SWT.CHECK);
            _checkboxUseTourFilter_Day.setText(Messages.Slideout_SensorTourFilter_Radio_Day);
            _checkboxUseTourFilter_Day.setToolTipText(Messages.Slideout_SensorTourFilter_Radio_Day_Tooltip);
            _checkboxUseTourFilter_Day.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerDay = new Spinner(container, SWT.BORDER);
            _spinnerDay.setMinimum(0);
            _spinnerDay.setMaximum(999);
            _spinnerDay.addSelectionListener(_defaultSelectionListener);
            _spinnerDay.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Day = createUI_Action_ResetValue(container, _spinnerDay);
         }
      }
   }

   private Action_ResetValue createUI_Action_ResetValue(final Composite parent, final Spinner spinner) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      final Action_ResetValue action = new Action_ResetValue(spinner);

      tbm.add(action);

      tbm.update(true);

      return action;
   }

   private void enableControls() {

      final boolean isFilterDuration = _checkboxUseFilterDuration.getSelection();

      final boolean isDay = _checkboxUseTourFilter_Day.getSelection() && isFilterDuration;
      final boolean isMonth = _checkboxUseTourFilter_Month.getSelection() && isFilterDuration;
      final boolean isYear = _checkboxUseTourFilter_Year.getSelection() && isFilterDuration;

      final int durationDays = _spinnerDay.getSelection();
      final int durationMonths = _spinnerMonth.getSelection();
      final int durationYears = _spinnerYear.getSelection();

      _actionResetValue_Day.setEnabled(isDay && durationDays > 0);
      _actionResetValue_Month.setEnabled(isMonth && durationMonths > 0);
      _actionResetValue_Year.setEnabled(isYear && durationYears > 0);

      _checkboxUseTourFilter_Day.setEnabled(isFilterDuration);
      _checkboxUseTourFilter_Month.setEnabled(isFilterDuration);
      _checkboxUseTourFilter_Year.setEnabled(isFilterDuration);

      _spinnerDay.setEnabled(isDay);
      _spinnerMonth.setEnabled(isMonth);
      _spinnerYear.setEnabled(isYear);
   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      // update chart with new settings
      _sensorChartView.updateChart();
   }

   private void onResetValue(final Spinner spinner) {

      spinner.setSelection(0);

      onChangeUI();
   }

   @Override
   public void resetToDefaults() {

      _checkboxUseAppFilter.setSelection(STATE_USE_APP_FILTER_DEFAULT);
      _checkboxUseFilterDuration.setSelection(STATE_USE_DURATION_FILTER_DEFAULT);

      _checkboxUseTourFilter_Day.setSelection(STATE_USE_TOUR_FILTER_DAYS_DEFAULT);
      _checkboxUseTourFilter_Month.setSelection(STATE_USE_TOUR_FILTER_MONTHS_DEFAULT);
      _checkboxUseTourFilter_Year.setSelection(STATE_USE_TOUR_FILTER_YEARS_DEFAULT);

      _spinnerDay.setSelection(STATE_TOUR_FILTER_DAYS_DEFAULT);
      _spinnerMonth.setSelection(STATE_TOUR_FILTER_MONTHS_DEFAULT);
      _spinnerYear.setSelection(STATE_TOUR_FILTER_YEARS_DEFAULT);

      onChangeUI();
   }

   private void restoreState() {

      _checkboxUseAppFilter.setSelection(Util.getStateBoolean(_state, STATE_USE_APP_FILTER, STATE_USE_APP_FILTER_DEFAULT));
      _checkboxUseFilterDuration.setSelection(Util.getStateBoolean(_state, STATE_USE_DURATION_FILTER, STATE_USE_DURATION_FILTER_DEFAULT));

      _checkboxUseTourFilter_Day.setSelection(Util.getStateBoolean(_state, STATE_USE_TOUR_FILTER_DAYS, STATE_USE_TOUR_FILTER_DAYS_DEFAULT));
      _checkboxUseTourFilter_Month.setSelection(Util.getStateBoolean(_state, STATE_USE_TOUR_FILTER_MONTHS, STATE_USE_TOUR_FILTER_MONTHS_DEFAULT));
      _checkboxUseTourFilter_Year.setSelection(Util.getStateBoolean(_state, STATE_USE_TOUR_FILTER_YEARS, STATE_USE_TOUR_FILTER_YEARS_DEFAULT));

      _spinnerDay.setSelection(Util.getStateInt(_state, STATE_TOUR_FILTER_DAYS, STATE_TOUR_FILTER_DAYS_DEFAULT));
      _spinnerMonth.setSelection(Util.getStateInt(_state, STATE_TOUR_FILTER_MONTHS, STATE_TOUR_FILTER_MONTHS_DEFAULT));
      _spinnerYear.setSelection(Util.getStateInt(_state, STATE_TOUR_FILTER_YEARS, STATE_TOUR_FILTER_YEARS_DEFAULT));
   }

   private void saveState() {

      _state.put(STATE_USE_APP_FILTER, _checkboxUseAppFilter.getSelection());
      _state.put(STATE_USE_DURATION_FILTER, _checkboxUseFilterDuration.getSelection());

      _state.put(STATE_USE_TOUR_FILTER_DAYS, _checkboxUseTourFilter_Day.getSelection());
      _state.put(STATE_USE_TOUR_FILTER_MONTHS, _checkboxUseTourFilter_Month.getSelection());
      _state.put(STATE_USE_TOUR_FILTER_YEARS, _checkboxUseTourFilter_Year.getSelection());

      _state.put(STATE_TOUR_FILTER_DAYS, _spinnerDay.getSelection());
      _state.put(STATE_TOUR_FILTER_MONTHS, _spinnerMonth.getSelection());
      _state.put(STATE_TOUR_FILTER_YEARS, _spinnerYear.getSelection());
   }

}
