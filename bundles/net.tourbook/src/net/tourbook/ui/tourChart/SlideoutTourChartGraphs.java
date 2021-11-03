/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.tourChart;

import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Slideout to select visible graphs
 */
public class SlideoutTourChartGraphs extends ToolbarSlideout implements IActionResetToDefault {

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();
   private IDialogSettings        _state;

   private ActionOpenPrefDialog   _actionPrefDialog;
   private ActionResetToDefaults  _actionRestoreDefaults;

   private SelectionAdapter       _defaultSelectionListener;

   /*
    * UI controls
    */
   private TourChart _tourChart;

   private Button    _chkShowInChartToolbar_Altimeter;
   private Button    _chkShowInChartToolbar_Altimeter_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Cadence;
   private Button    _chkShowInChartToolbar_Cadence_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Elevation;
   private Button    _chkShowInChartToolbar_Elevation_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Gears;
   private Button    _chkShowInChartToolbar_Gears_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Gradient;
   private Button    _chkShowInChartToolbar_Gradient_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Pace;
   private Button    _chkShowInChartToolbar_Pace_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Power;
   private Button    _chkShowInChartToolbar_Power_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Pulse;
   private Button    _chkShowInChartToolbar_Pulse_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Tempterature;
   private Button    _chkShowInChartToolbar_Tempterature_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Speed;
   private Button    _chkShowInChartToolbar_Speed_DefaultWhenOpened;

   private Button    _chkShowInChartToolbar_RunDyn_StanceTime;
   private Button    _chkShowInChartToolbar_RunDyn_StanceTime_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_RunDyn_StanceTimeBalance;
   private Button    _chkShowInChartToolbar_RunDyn_StanceTimeBalance_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_RunDyn_StepLength;
   private Button    _chkShowInChartToolbar_RunDyn_StepLength_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_RunDyn_VerticalOscillation;
   private Button    _chkShowInChartToolbar_RunDyn_VerticalOscillation_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_RunDyn_VerticalRatio;
   private Button    _chkShowInChartToolbar_RunDyn_VerticalRatio_DefaultWhenOpened;

   private Button    _chkShowInChartToolbar_Swim_Strokes;
   private Button    _chkShowInChartToolbar_Swim_Strokes_DefaultWhenOpened;
   private Button    _chkShowInChartToolbar_Swim_Swolf;
   private Button    _chkShowInChartToolbar_Swim_Swolf_DefaultWhenOpened;

   public SlideoutTourChartGraphs(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final TourChart tourChart,
                                  final IDialogSettings state) {

      super(ownerControl, toolBar);

      _tourChart = tourChart;
      _state = state;
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog(
            Messages.Tour_Action_EditChartPreferences,
            PrefPageAppearanceTourChart.ID);

      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_tourChart.getShell());
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      initUI(parent);

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
         GridLayoutFactory
               .fillDefaults()//
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
         }
         createUI_20_Graphs(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_TourChartGraph_Label_Title);
      MTFont.setBannerFont(label);
   }

   private void createUI_12_Actions(final Composite parent) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      GridDataFactory
            .fillDefaults()//
            .grab(true, false)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(toolbar);

      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_actionRestoreDefaults);
      tbm.add(_actionPrefDialog);

      tbm.update(true);
   }

   private void createUI_20_Graphs(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(18).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
// SET_FORMATTING_OFF
            /*
             * Checkbox: Selected graphs will be displayed when a tour is opened
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourChartGraph_Label_DefaultWhenOpened);

            _chkShowInChartToolbar_Elevation_DefaultWhenOpened       = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Pulse_DefaultWhenOpened           = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Speed_DefaultWhenOpened           = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Pace_DefaultWhenOpened            = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Power_DefaultWhenOpened           = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Tempterature_DefaultWhenOpened    = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Gradient_DefaultWhenOpened        = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Altimeter_DefaultWhenOpened       = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Cadence_DefaultWhenOpened         = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Gears_DefaultWhenOpened           = createUI_DefaulWhenOpened(container);

            _chkShowInChartToolbar_RunDyn_StanceTime_DefaultWhenOpened           = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_RunDyn_StanceTimeBalance_DefaultWhenOpened    = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_RunDyn_StepLength_DefaultWhenOpened           = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_RunDyn_VerticalOscillation_DefaultWhenOpened  = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_RunDyn_VerticalRatio_DefaultWhenOpened        = createUI_DefaulWhenOpened(container);

            _chkShowInChartToolbar_Swim_Strokes_DefaultWhenOpened    = createUI_DefaulWhenOpened(container);
            _chkShowInChartToolbar_Swim_Swolf_DefaultWhenOpened      = createUI_DefaulWhenOpened(container);
// SET_FORMATTING_ON
         }
         {
            /*
             * Actions: chart graphs
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourChartGraph_Label_ShowGraph);

            createUI_GraphAction(container, TourManager.GRAPH_ALTITUDE);
            createUI_GraphAction(container, TourManager.GRAPH_PULSE);
            createUI_GraphAction(container, TourManager.GRAPH_SPEED);
            createUI_GraphAction(container, TourManager.GRAPH_PACE);
            createUI_GraphAction(container, TourManager.GRAPH_POWER);
            createUI_GraphAction(container, TourManager.GRAPH_TEMPERATURE);
            createUI_GraphAction(container, TourManager.GRAPH_GRADIENT);
            createUI_GraphAction(container, TourManager.GRAPH_ALTIMETER);
            createUI_GraphAction(container, TourManager.GRAPH_CADENCE);
            createUI_GraphAction(container, TourManager.GRAPH_GEARS);

            createUI_GraphAction(container, TourManager.GRAPH_RUN_DYN_STANCE_TIME);
            createUI_GraphAction(container, TourManager.GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
            createUI_GraphAction(container, TourManager.GRAPH_RUN_DYN_STEP_LENGTH);
            createUI_GraphAction(container, TourManager.GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
            createUI_GraphAction(container, TourManager.GRAPH_RUN_DYN_VERTICAL_RATIO);

            createUI_GraphAction(container, TourManager.GRAPH_SWIM_STROKES);
            createUI_GraphAction(container, TourManager.GRAPH_SWIM_SWOLF);
         }
         {
// SET_FORMATTING_OFF
            /*
             * Checkbox: Show/hide action in the chart toolbar
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Slideout_TourChartGraph_Label_ShowActionInToolbar);

            _chkShowInChartToolbar_Elevation       = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Pulse           = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Speed           = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Pace            = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Power           = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Tempterature    = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Gradient        = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Altimeter       = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Cadence         = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Gears           = createUI_ShowActionInToolbar(container);

            _chkShowInChartToolbar_RunDyn_StanceTime              = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_RunDyn_StanceTimeBalance       = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_RunDyn_StepLength              = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_RunDyn_VerticalOscillation     = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_RunDyn_VerticalRatio           = createUI_ShowActionInToolbar(container);

            _chkShowInChartToolbar_Swim_Strokes    = createUI_ShowActionInToolbar(container);
            _chkShowInChartToolbar_Swim_Swolf      = createUI_ShowActionInToolbar(container);
         }
// SET_FORMATTING_ON
      }
   }

   private Button createUI_DefaulWhenOpened(final Composite parent) {

      final Button checkbox = new Button(parent, SWT.CHECK);

      checkbox.setToolTipText(Messages.Slideout_TourChartGraph_Checkbox_DefaultWhenOpened_Tooltip);
      checkbox.addSelectionListener(_defaultSelectionListener);

      GridDataFactory
            .fillDefaults()
            .grab(true, false)
            .align(SWT.CENTER, SWT.FILL)
            .applyTo(checkbox);

      return checkbox;
   }

   private void createUI_GraphAction(final Composite parent, final int graphId) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_tourChart.getGraphAction(graphId));

      tbm.update(true);
   }

   private Button createUI_ShowActionInToolbar(final Composite parent) {

      final Button checkbox = new Button(parent, SWT.CHECK);

      checkbox.setToolTipText(Messages.Slideout_TourChartGraph_Checkbox_ShowInChartToolbar_Tooltip);
      checkbox.addSelectionListener(_defaultSelectionListener);

      GridDataFactory
            .fillDefaults()
            .grab(true, false)
            .align(SWT.CENTER, SWT.FILL)
            .applyTo(checkbox);

      return checkbox;
   }

   private void initUI(final Composite parent) {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };
   }

   @Override
   protected boolean isCenterHorizontal() {
      return true;
   }

   private void onChangeUI() {

      saveState();

      // update chart toolbar
      _tourChart.updateGraphToolbar();
   }

   @Override
   protected void onDispose() {

   }

   @Override
   public void resetToDefaults() {

// SET_FORMATTING_OFF

		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE, 								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTIMETER, 							TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTIMETER_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_CADENCE, 								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_CADENCE_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GEARS, 									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GEARS_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GRADIENT, 								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GRADIENT_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PACE, 									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PACE_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_POWER, 									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_POWER_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PULSE, 									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PULSE_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SPEED, 									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SPEED_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_TEMPERATURE,							TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_TEMPERATURE_DEFAULT);

		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME, 				TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME_BALANCED, 	TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME_BALANCED_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STEP_LENGTH, 				TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STEP_LENGTH_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_OSCILLATION, 	TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_OSCILLATION_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_RATIO, 			TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_RATIO_DEFAULT);

		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_STROKES,				 			TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_STROKES_DEFAULT);
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_SWOLF,				 			TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_SWOLF_DEFAULT);

// SET_FORMATTING_ON

      // update UI
      restoreState();

      // update chart toolbar
      _tourChart.updateGraphToolbar();
   }

   private void restoreState() {

// SET_FORMATTING_OFF

		_chkShowInChartToolbar_Elevation.setSelection(							Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE,								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE_DEFAULT));
		_chkShowInChartToolbar_Altimeter.setSelection(							Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTIMETER,								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTIMETER_DEFAULT));
		_chkShowInChartToolbar_Cadence.setSelection(								Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_CADENCE,								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_CADENCE_DEFAULT));
		_chkShowInChartToolbar_Gears.setSelection(								Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GEARS,									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GEARS_DEFAULT));
		_chkShowInChartToolbar_Gradient.setSelection(							Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GRADIENT,								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GRADIENT_DEFAULT));
		_chkShowInChartToolbar_Pace.setSelection(									Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PACE,									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PACE_DEFAULT));
		_chkShowInChartToolbar_Power.setSelection(								Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_POWER,									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_POWER_DEFAULT));
		_chkShowInChartToolbar_Pulse.setSelection(								Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PULSE,									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PULSE_DEFAULT));
		_chkShowInChartToolbar_Speed.setSelection(								Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SPEED,									TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SPEED_DEFAULT));
		_chkShowInChartToolbar_Tempterature.setSelection(						Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_TEMPERATURE,							TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_TEMPERATURE_DEFAULT));

		_chkShowInChartToolbar_RunDyn_StanceTime.setSelection(				Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME,				TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME_DEFAULT));
		_chkShowInChartToolbar_RunDyn_StanceTimeBalance.setSelection(		Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME_BALANCED, 	TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME_BALANCED_DEFAULT));
		_chkShowInChartToolbar_RunDyn_StepLength.setSelection(				Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STEP_LENGTH, 				TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STEP_LENGTH_DEFAULT));
		_chkShowInChartToolbar_RunDyn_VerticalOscillation.setSelection(	Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_OSCILLATION, 	TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_OSCILLATION_DEFAULT));
		_chkShowInChartToolbar_RunDyn_VerticalRatio.setSelection(			Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_RATIO, 			TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_RATIO_DEFAULT));

		_chkShowInChartToolbar_Swim_Strokes.setSelection(						Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_STROKES, 						TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_STROKES_DEFAULT));
		_chkShowInChartToolbar_Swim_Swolf.setSelection(							Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_SWOLF, 							TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_SWOLF_DEFAULT));

// SET_FORMATTING_ON

      final String[] allVisibleIds = StringToArrayConverter.convertStringToArray(
            _prefStore.getString(ITourbookPreferences.GRAPH_VISIBLE));

      for (final String graphId : allVisibleIds) {

         final int graphIdValue = Integer.parseInt(graphId);

         if (graphIdValue == TourManager.GRAPH_ALTITUDE) {
            _chkShowInChartToolbar_Elevation_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_ALTIMETER) {
            _chkShowInChartToolbar_Altimeter_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_CADENCE) {
            _chkShowInChartToolbar_Cadence_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_GEARS) {
            _chkShowInChartToolbar_Gears_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_GRADIENT) {
            _chkShowInChartToolbar_Gradient_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_PACE) {
            _chkShowInChartToolbar_Pace_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_POWER) {
            _chkShowInChartToolbar_Power_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_PULSE) {
            _chkShowInChartToolbar_Pulse_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_SPEED) {
            _chkShowInChartToolbar_Speed_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_TEMPERATURE) {
            _chkShowInChartToolbar_Tempterature_DefaultWhenOpened.setSelection(true);
         }

         /*
          * Running Dynamics
          */
         if (graphIdValue == TourManager.GRAPH_RUN_DYN_STANCE_TIME) {
            _chkShowInChartToolbar_RunDyn_StanceTime_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_RUN_DYN_STANCE_TIME_BALANCED) {
            _chkShowInChartToolbar_RunDyn_StanceTimeBalance_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_RUN_DYN_STEP_LENGTH) {
            _chkShowInChartToolbar_RunDyn_StepLength_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_RUN_DYN_VERTICAL_OSCILLATION) {
            _chkShowInChartToolbar_RunDyn_VerticalOscillation_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_RUN_DYN_VERTICAL_RATIO) {
            _chkShowInChartToolbar_RunDyn_VerticalRatio_DefaultWhenOpened.setSelection(true);
         }

         /*
          * Swimming
          */
         if (graphIdValue == TourManager.GRAPH_SWIM_STROKES) {
            _chkShowInChartToolbar_Swim_Strokes_DefaultWhenOpened.setSelection(true);
         }

         if (graphIdValue == TourManager.GRAPH_SWIM_SWOLF) {
            _chkShowInChartToolbar_Swim_Swolf_DefaultWhenOpened.setSelection(true);
         }
      }
   }

   private void saveState() {

// SET_FORMATTING_OFF

		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE, 								_chkShowInChartToolbar_Elevation.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTIMETER, 							_chkShowInChartToolbar_Altimeter.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_CADENCE, 								_chkShowInChartToolbar_Cadence.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GEARS, 									_chkShowInChartToolbar_Gears.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_GRADIENT, 								_chkShowInChartToolbar_Gradient.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PACE, 									_chkShowInChartToolbar_Pace.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_POWER, 									_chkShowInChartToolbar_Power.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_PULSE, 									_chkShowInChartToolbar_Pulse.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SPEED, 									_chkShowInChartToolbar_Speed.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_TEMPERATURE,							_chkShowInChartToolbar_Tempterature.getSelection());

		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME, 				_chkShowInChartToolbar_RunDyn_StanceTime.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STANCE_TIME_BALANCED, 	_chkShowInChartToolbar_RunDyn_StanceTimeBalance.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_STEP_LENGTH, 				_chkShowInChartToolbar_RunDyn_StepLength.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_OSCILLATION, 	_chkShowInChartToolbar_RunDyn_VerticalOscillation.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_RUN_DYN_VERTICAL_RATIO, 			_chkShowInChartToolbar_RunDyn_VerticalRatio.getSelection());

		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_STROKES,				 			_chkShowInChartToolbar_Swim_Strokes.getSelection());
		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_SWIM_SWOLF,				 			_chkShowInChartToolbar_Swim_Swolf.getSelection());

// SET_FORMATTING_ON

      final ArrayList<String> allDefaultGraphs = new ArrayList<>();

      /*
       * Add all visible graphs in the chart
       */
      if (_chkShowInChartToolbar_Elevation_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_ALTITUDE));
      }

      if (_chkShowInChartToolbar_Altimeter_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_ALTIMETER));
      }

      if (_chkShowInChartToolbar_Cadence_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_CADENCE));
      }

      if (_chkShowInChartToolbar_Gears_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_GEARS));
      }

      if (_chkShowInChartToolbar_Gradient_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_GRADIENT));
      }

      if (_chkShowInChartToolbar_Pace_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_PACE));
      }

      if (_chkShowInChartToolbar_Power_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_POWER));
      }

      if (_chkShowInChartToolbar_Pulse_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_PULSE));
      }

      if (_chkShowInChartToolbar_Speed_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_SPEED));
      }

      if (_chkShowInChartToolbar_Tempterature_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_TEMPERATURE));
      }

      /*
       * Running Dynamics
       */
      if (_chkShowInChartToolbar_RunDyn_StanceTime_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_RUN_DYN_STANCE_TIME));
      }

      if (_chkShowInChartToolbar_RunDyn_StanceTimeBalance_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_RUN_DYN_STANCE_TIME_BALANCED));
      }

      if (_chkShowInChartToolbar_RunDyn_StepLength_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_RUN_DYN_STEP_LENGTH));
      }

      if (_chkShowInChartToolbar_RunDyn_VerticalOscillation_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_RUN_DYN_VERTICAL_OSCILLATION));
      }

      if (_chkShowInChartToolbar_RunDyn_VerticalRatio_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_RUN_DYN_VERTICAL_RATIO));
      }

      /*
       * Swimming
       */

      if (_chkShowInChartToolbar_Swim_Strokes_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_SWIM_STROKES));
      }
      if (_chkShowInChartToolbar_Swim_Swolf_DefaultWhenOpened.getSelection()) {
         allDefaultGraphs.add(Integer.toString(TourManager.GRAPH_SWIM_SWOLF));
      }

      _prefStore.setValue(
            ITourbookPreferences.GRAPH_VISIBLE,
            StringToArrayConverter.convertArrayToString(allDefaultGraphs.toArray(new String[allDefaultGraphs.size()])));
   }
}
