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
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart properties slideout.
 */
public class SlideoutSensorChartOptions extends ToolbarSlideout implements IActionResetToDefault {

   static final String            STATE_IS_SHOW_BATTERY_LEVEL           = "STATE_IS_SHOW_BATTERY_LEVEL";  //$NON-NLS-1$
   static final boolean           STATE_IS_SHOW_BATTERY_LEVEL_DEFAULT   = true;
   static final String            STATE_IS_SHOW_BATTERY_STATUS          = "STATE_IS_SHOW_BATTERY_STATUS"; //$NON-NLS-1$
   static final boolean           STATE_IS_SHOW_BATTERY_STATUS_DEFAULT  = true;
   static final String            STATE_IS_SHOW_BATTERY_VOLTAGE         = "STATE_IS_SHOW_BATTERY_VOLTAGE";//$NON-NLS-1$
   static final boolean           STATE_IS_SHOW_BATTERY_VOLTAGE_DEFAULT = true;

   private static IDialogSettings _state;

   private SelectionListener      _defaultSelectionListener;

   private ActionResetToDefaults  _actionRestoreDefaults;

   private SensorChartView        _sensorChartView;
   private ChartOptions_Grid      _gridUI;

   /*
    * UI controls
    */
   private Button _checkboxBatteryLevel;
   private Button _checkboxBatteryStatus;
   private Button _checkboxBatteryVoltage;

   public SlideoutSensorChartOptions(final Composite ownerControl,
                                     final ToolBar toolbar,
                                     final SensorChartView sensorChartView,
                                     final IDialogSettings state,
                                     final String gridPrefPrefix) {

      super(ownerControl, toolbar);

      _sensorChartView = sensorChartView;
      _state = state;

      _gridUI = new ChartOptions_Grid(gridPrefPrefix);
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

            createUI_20_BatteryCharts(container);
         }

         _gridUI.createUI(container);
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_SensorChartOptions_Label_Title);
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

   private void createUI_20_BatteryCharts(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
      {
         {
            /*
             * Battery level
             */

            _checkboxBatteryLevel = new Button(container, SWT.CHECK);
            _checkboxBatteryLevel.setText(Messages.Slideout_SensorChartOptions_Checkbox_BatteryLevel);
            _checkboxBatteryLevel.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Battery voltage
             */

            _checkboxBatteryVoltage = new Button(container, SWT.CHECK);
            _checkboxBatteryVoltage.setText(Messages.Slideout_SensorChartOptions_Checkbox_BatteryVoltage);
            _checkboxBatteryVoltage.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Battery status
             */

            _checkboxBatteryStatus = new Button(container, SWT.CHECK);
            _checkboxBatteryStatus.setText(Messages.Slideout_SensorChartOptions_Checkbox_BatteryStatus);
            _checkboxBatteryStatus.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void enableControls() {

   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());
   }

   private void onChangeUI() {

      saveState();

      enableControls();

      // update chart with new settings
      _sensorChartView.updateChart();
   }

   @Override
   public void resetToDefaults() {

      _checkboxBatteryLevel.setSelection(STATE_IS_SHOW_BATTERY_LEVEL_DEFAULT);
      _checkboxBatteryStatus.setSelection(STATE_IS_SHOW_BATTERY_STATUS_DEFAULT);
      _checkboxBatteryVoltage.setSelection(STATE_IS_SHOW_BATTERY_VOLTAGE_DEFAULT);

      _gridUI.resetToDefaults();

      onChangeUI();
   }

   private void restoreState() {

      _checkboxBatteryLevel.setSelection(Util.getStateBoolean(_state, STATE_IS_SHOW_BATTERY_LEVEL, STATE_IS_SHOW_BATTERY_LEVEL_DEFAULT));
      _checkboxBatteryStatus.setSelection(Util.getStateBoolean(_state, STATE_IS_SHOW_BATTERY_STATUS, STATE_IS_SHOW_BATTERY_STATUS_DEFAULT));
      _checkboxBatteryVoltage.setSelection(Util.getStateBoolean(_state, STATE_IS_SHOW_BATTERY_VOLTAGE, STATE_IS_SHOW_BATTERY_VOLTAGE_DEFAULT));

      _gridUI.restoreState();
   }

   private void saveState() {

      _state.put(STATE_IS_SHOW_BATTERY_LEVEL, _checkboxBatteryLevel.getSelection());
      _state.put(STATE_IS_SHOW_BATTERY_STATUS, _checkboxBatteryStatus.getSelection());
      _state.put(STATE_IS_SHOW_BATTERY_VOLTAGE, _checkboxBatteryVoltage.getSelection());

      _gridUI.saveState();
   }

}
