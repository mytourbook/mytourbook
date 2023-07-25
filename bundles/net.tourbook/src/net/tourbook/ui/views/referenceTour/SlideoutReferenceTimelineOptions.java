/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_ALTIMETER_AVG;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_ALTIMETER_AVG_DEFAULT;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_PACE_AVG;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_PACE_AVG_DEFAULT;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_PULSE_AVG;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_PULSE_AVG_DEFAULT;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_PULSE_AVG_MAX;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_PULSE_AVG_MAX_DEFAULT;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_SPEED_AVG;
import static net.tourbook.ui.views.referenceTour.ReferenceTimelineView.STATE_SHOW_SPEED_AVG_DEFAULT;
import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
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
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

public class SlideoutReferenceTimelineOptions extends ToolbarSlideout implements IActionResetToDefault {

   private ReferenceTimelineView _refTour_StatisticView;

   private ActionResetToDefaults _actionRestoreDefaults;

   private ChartOptions_Grid     _gridUI;

   private IDialogSettings       _state;

   private MouseWheelListener    _defaultMouseWheelListener;
   private SelectionListener     _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button  _chkShowAltimeter_Avg;
   private Button  _chkShowPulse_Avg;
   private Button  _chkShowPulse_AvgMax;
   private Button  _chkShowPace_Avg;
   private Button  _chkShowSpeed_Avg;

   private Spinner _spinnerBarHeight;

   public SlideoutReferenceTimelineOptions(final ReferenceTimelineView refTour_YearStatistic_View,
                                           final Control ownerControl,
                                           final ToolBar toolBar,
                                           final String prefStoreGridPrefix,
                                           final IDialogSettings state) {

      super(ownerControl, toolBar);

      _refTour_StatisticView = refTour_YearStatistic_View;
      _state = state;

      _gridUI = new ChartOptions_Grid(prefStoreGridPrefix);
   }

   private void createActions() {

      /*
       * Action: Restore default
       */
      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI();

      createActions();

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(shellContainer);
      {
         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults()
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);

            createUI_20_Graphs(container);

            _gridUI.createUI(container);

            _gridUI.enableGridOptions(ChartOptions_Grid.GRID_VERTICAL_DISTANCE
                  | ChartOptions_Grid.GRID_IS_SHOW_HORIZONTAL_LINE
                  | ChartOptions_Grid.GRID_IS_SHOW_VERTICAL_LINE);
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
      label.setText(Messages.Slideout_RefTour_Label_ReferenceTimelineOptions);
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

   private void createUI_20_Graphs(final Composite parent) {

      final GridDataFactory gd = GridDataFactory.fillDefaults().grab(true, false).span(2, 1);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Slideout_RefTour_Group_Graphs);
      gd.applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//    group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         {
            /*
             * Show avg speed
             */
            _chkShowSpeed_Avg = new Button(group, SWT.CHECK);
            _chkShowSpeed_Avg.setText(Messages.Slideout_RefTour_Checkbox_Speed_Avg);
            _chkShowSpeed_Avg.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_chkShowSpeed_Avg);

         }
         {
            /*
             * Show avg pace
             */
            _chkShowPace_Avg = new Button(group, SWT.CHECK);
            _chkShowPace_Avg.setText(Messages.Slideout_RefTour_Checkbox_Pace_Avg);
            _chkShowPace_Avg.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_chkShowPace_Avg);
         }
         {
            /*
             * Show avg altimeter (VAM)
             */
            _chkShowAltimeter_Avg = new Button(group, SWT.CHECK);
            _chkShowAltimeter_Avg.setText(Messages.Slideout_RefTour_Checkbox_Altimeter_Avg);
            _chkShowAltimeter_Avg.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_chkShowAltimeter_Avg);
         }
         {
            /*
             * Show avg pulse
             */
            _chkShowPulse_Avg = new Button(group, SWT.CHECK);
            _chkShowPulse_Avg.setText(Messages.Slideout_RefTour_Checkbox_Pulse_Avg);
            _chkShowPulse_Avg.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_chkShowPulse_Avg);
         }
         {
            /*
             * Show avg/max pulse
             */
            _chkShowPulse_AvgMax = new Button(group, SWT.CHECK);
            _chkShowPulse_AvgMax.setText(Messages.Slideout_RefTour_Checkbox_Pulse_AvgMax);
            _chkShowPulse_AvgMax.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_chkShowPulse_AvgMax);
         }
         {
            /*
             * Bar size
             */
            final String tooltip = Messages.Slideout_RefTour_Spinner_BarSize_Tooltip;

            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Slideout_RefTour_Label_BarSize);
            label.setToolTipText(tooltip);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);

            _spinnerBarHeight = new Spinner(group, SWT.BORDER);
            _spinnerBarHeight.setMinimum(1);
            _spinnerBarHeight.setMaximum(100);
            _spinnerBarHeight.setIncrement(1);
            _spinnerBarHeight.setPageIncrement(10);
            _spinnerBarHeight.setToolTipText(tooltip);
            _spinnerBarHeight.addMouseWheelListener(_defaultMouseWheelListener);
            _spinnerBarHeight.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerBarHeight);
         }
      }
   }

   private void initUI() {

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {

         UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10);
         onChangeUI();
      };
   }

   private void onChangeUI() {

      // update chart async that the UI is updated immediately

      Display.getCurrent().asyncExec(() -> {

         saveState();

         _refTour_StatisticView.updateUI_YearChart_WithCurrentGeoData();
      });
   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

      _gridUI.resetToDefaults();
      _gridUI.saveState();

      _chkShowAltimeter_Avg   .setSelection(STATE_SHOW_ALTIMETER_AVG_DEFAULT);
      _chkShowPace_Avg        .setSelection(STATE_SHOW_PACE_AVG_DEFAULT);
      _chkShowPulse_Avg       .setSelection(STATE_SHOW_PULSE_AVG_DEFAULT);
      _chkShowPulse_AvgMax    .setSelection(STATE_SHOW_PULSE_AVG_MAX_DEFAULT);
      _chkShowSpeed_Avg       .setSelection(STATE_SHOW_SPEED_AVG_DEFAULT);

      _spinnerBarHeight       .setSelection(ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT_DEFAULT);

// SET_FORMATTING_ON

      onChangeUI();
   }

   private void restoreState() {

      _gridUI.restoreState();

// SET_FORMATTING_OFF

      _chkShowAltimeter_Avg   .setSelection(Util.getStateBoolean(_state, STATE_SHOW_ALTIMETER_AVG, STATE_SHOW_ALTIMETER_AVG_DEFAULT));
      _chkShowPace_Avg        .setSelection(Util.getStateBoolean(_state, STATE_SHOW_PACE_AVG,      STATE_SHOW_PACE_AVG_DEFAULT));
      _chkShowPulse_Avg       .setSelection(Util.getStateBoolean(_state, STATE_SHOW_PULSE_AVG,     STATE_SHOW_PULSE_AVG_DEFAULT));
      _chkShowPulse_AvgMax    .setSelection(Util.getStateBoolean(_state, STATE_SHOW_PULSE_AVG_MAX, STATE_SHOW_PULSE_AVG_MAX_DEFAULT));
      _chkShowSpeed_Avg       .setSelection(Util.getStateBoolean(_state, STATE_SHOW_SPEED_AVG,     STATE_SHOW_SPEED_AVG_DEFAULT));

// SET_FORMATTING_ON

      _spinnerBarHeight.setSelection(Util.getStateInt(_state,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT_DEFAULT,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT_MIN,
            ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT_MAX));
   }

   private void saveState() {

      _gridUI.saveState();

// SET_FORMATTING_OFF

      _state.put(STATE_SHOW_ALTIMETER_AVG,   _chkShowAltimeter_Avg   .getSelection());
      _state.put(STATE_SHOW_PACE_AVG,        _chkShowPace_Avg        .getSelection());
      _state.put(STATE_SHOW_PULSE_AVG,       _chkShowPulse_Avg       .getSelection());
      _state.put(STATE_SHOW_PULSE_AVG_MAX,   _chkShowPulse_AvgMax    .getSelection());
      _state.put(STATE_SHOW_SPEED_AVG,       _chkShowSpeed_Avg       .getSelection());

      _state.put(ReferenceTimelineView.STATE_RELATIVE_BAR_HEIGHT,  _spinnerBarHeight       .getSelection());

// SET_FORMATTING_ON
   }

}
