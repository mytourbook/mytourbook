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
import net.tourbook.application.TourbookPlugin;
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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
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
public class SlideoutSensorFilter extends ToolbarSlideout implements IActionResetToDefault {

   private static final String           STATE_SELECTED_TOUR_FILTER = "STATE_SELECTED_TOUR_FILTER"; //$NON-NLS-1$
   private static final String           STATE_TOUR_FILTER_DAYS     = "STATE_TOUR_FILTER_DAYS";     //$NON-NLS-1$
   private static final String           STATE_TOUR_FILTER_MONTHS   = "STATE_TOUR_FILTER_MONTHS";   //$NON-NLS-1$
   private static final String           STATE_TOUR_FILTER_YEARS    = "STATE_TOUR_FILTER_YEARS";    //$NON-NLS-1$

   private static final String           RESET_VALUE                = " X ";                        //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                 = TourbookPlugin.getPrefStore();

   private static IDialogSettings        _state;

   private SelectionListener             _defaultSelectionListener;
   private MouseWheelListener            _defaultMouseWheelListener;
   private FocusListener                 _keepOpenListener;

   private Action_ResetValue             _actionResetValue_Day;
   private Action_ResetValue             _actionResetValue_Month;
   private Action_ResetValue             _actionResetValue_Year;
   private ActionResetToDefaults         _actionRestoreDefaults;

   private SensorChartView               _sensorChartView;

   /*
    * UI controls
    */
   private Button  _radioDay;
   private Button  _radioMonth;
   private Button  _radioYear;

   private Spinner _spinnerDay;
   private Spinner _spinnerMonth;
   private Spinner _spinnerYear;

   /**
    * Reset spinner value
    */
   private class Action_ResetValue extends Action {

      private Spinner _spinner;

      public Action_ResetValue(final Spinner spinner) {

         super(RESET_VALUE, AS_PUSH_BUTTON);

         setToolTipText(Messages.Slideout_SensorTourFilter_Action_ResetValue_Tooltip);

         _spinner = spinner;
      }

      @Override
      public void run() {

         onResetValue(_spinner);
      }
   }

   private enum SensorTourFilter {

      DAY, MONTH, YEAR
   }

   public SlideoutSensorFilter(final Composite ownerControl,
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
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Slideout_SensorTourFilter_Label_Duration);
         GridDataFactory.fillDefaults()
//               .align(SWT.FILL, SWT.CENTER)
               .indent(0, 3)
               .applyTo(label);

      }

      final Composite durationContainer = new Composite(container, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(durationContainer);
      GridLayoutFactory.fillDefaults().numColumns(3).applyTo(durationContainer);
      {
         {
            /*
             * Year
             */

            // radio
            _radioYear = new Button(durationContainer, SWT.RADIO);
            _radioYear.setText(Messages.Slideout_SensorTourFilter_Radio_Year);
            _radioYear.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerYear = new Spinner(durationContainer, SWT.BORDER);
            _spinnerYear.setMinimum(1);
            _spinnerYear.addSelectionListener(_defaultSelectionListener);
            _spinnerYear.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Year = createUI_Action_ResetValue(durationContainer, _spinnerYear);
         }
         {
            /*
             * Month
             */

            // radio
            _radioMonth = new Button(durationContainer, SWT.RADIO);
            _radioMonth.setText(Messages.Slideout_SensorTourFilter_Radio_Month);
            _radioMonth.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerMonth = new Spinner(durationContainer, SWT.BORDER);
            _spinnerMonth.setMinimum(1);
            _spinnerMonth.addSelectionListener(_defaultSelectionListener);
            _spinnerMonth.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Month = createUI_Action_ResetValue(durationContainer, _spinnerMonth);
         }
         {
            /*
             * Day
             */

            // radio
            _radioDay = new Button(durationContainer, SWT.RADIO);
            _radioDay.setText(Messages.Slideout_SensorTourFilter_Radio_Day);
            _radioDay.addSelectionListener(_defaultSelectionListener);

            // spinner
            _spinnerDay = new Spinner(durationContainer, SWT.BORDER);
            _spinnerDay.setMinimum(1);
            _spinnerDay.addSelectionListener(_defaultSelectionListener);
            _spinnerDay.addMouseWheelListener(_defaultMouseWheelListener);

            // action: Reset value
            _actionResetValue_Day = createUI_Action_ResetValue(durationContainer, _spinnerDay);
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

      final boolean isDay = _radioDay.getSelection();
      final boolean isMonth = _radioMonth.getSelection();
      final boolean isYear = _radioYear.getSelection();

      _actionResetValue_Day.setEnabled(isDay);
      _actionResetValue_Month.setEnabled(isMonth);
      _actionResetValue_Year.setEnabled(isYear);

      _spinnerDay.setEnabled(isDay);
      _spinnerMonth.setEnabled(isMonth);
      _spinnerYear.setEnabled(isYear);
   }

   private SensorTourFilter getSelectedTourFilter() {

      if (_radioDay.getSelection()) {

         return SensorTourFilter.DAY;

      } else if (_radioMonth.getSelection()) {

         return SensorTourFilter.MONTH;

      } else {

         return SensorTourFilter.YEAR;
      }

   }

   private void initUI() {

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
            setIsAnotherDialogOpened(true);
         }

         @Override
         public void focusLost(final FocusEvent e) {
            setIsAnotherDialogOpened(false);
         }
      };
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      // update chart with new settings
//      _tourChart.updateTourChart();
   }

   private void onResetValue(final Spinner spinner) {

      spinner.setSelection(1);

      onChangeUI();
   }

   @Override
   public void resetToDefaults() {

      onChangeUI();
   }

   private void restoreState() {

      _spinnerDay.setSelection(Util.getStateInt(_state, STATE_TOUR_FILTER_DAYS, 1));
      _spinnerMonth.setSelection(Util.getStateInt(_state, STATE_TOUR_FILTER_MONTHS, 1));
      _spinnerYear.setSelection(Util.getStateInt(_state, STATE_TOUR_FILTER_YEARS, 1));

      final Enum<SensorTourFilter> seletedTourFilter = Util.getStateEnum(_state, STATE_SELECTED_TOUR_FILTER, SensorTourFilter.YEAR);
      selectTourFilter(seletedTourFilter);
   }

   private void saveState() {

      _state.put(STATE_TOUR_FILTER_DAYS, _spinnerDay.getSelection());
      _state.put(STATE_TOUR_FILTER_MONTHS, _spinnerMonth.getSelection());
      _state.put(STATE_TOUR_FILTER_YEARS, _spinnerYear.getSelection());

      Util.setStateEnum(_state, STATE_SELECTED_TOUR_FILTER, getSelectedTourFilter());
   }

   private void selectTourFilter(final Enum<SensorTourFilter> seletedTourFilter) {

      if (seletedTourFilter == SensorTourFilter.DAY) {

         _radioDay.setSelection(true);

      } else if (seletedTourFilter == SensorTourFilter.MONTH) {

         _radioMonth.setSelection(true);

      } else {

         _radioYear.setSelection(true);
      }
   }

}
