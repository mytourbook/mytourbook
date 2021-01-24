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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.data.CustomTrackDefinition;
import net.tourbook.data.TourData;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart properties slideout.
 */
public class SlideoutTourChartGraphs extends ToolbarSlideout {

   private static final int     GRID_TOOLBAR_SLIDEOUT_NB_COLUMN                  = 17;
   private static final int     GRID_TOOLBAR_SLIDEOUT_NB_ROW                     = 7;
   private static final int     GRID_TOOLBAR_SLIDEOUT_DEFAULT_HORIZONTAL_SPACING = 5;

   private IDialogSettings      _state;

   private Action               _actionRestoreDefaults;
   private ActionOpenPrefDialog _actionPrefDialog;

   private SelectionAdapter     _defaultSelectionListener;
   //
   /*
    * UI controls
    */
   private TourChart               _tourChart;

   private Button                  _chkShowInChartToolbar_Altimeter;
   private Button                  _chkShowInChartToolbar_Altitude;
   private Button                  _chkShowInChartToolbar_Cadence;
   private Button                  _chkShowInChartToolbar_Gears;
   private Button                  _chkShowInChartToolbar_Gradient;
   private Button                  _chkShowInChartToolbar_Pace;
   private Button                  _chkShowInChartToolbar_Power;
   private Button                  _chkShowInChartToolbar_Pulse;
   private Button                  _chkShowInChartToolbar_Tempterature;
   private Button                  _chkShowInChartToolbar_Speed;

   private Button                  _chkShowInChartToolbar_RunDyn_StanceTime;
   private Button                  _chkShowInChartToolbar_RunDyn_StanceTimeBalance;
   private Button                  _chkShowInChartToolbar_RunDyn_StepLength;
   private Button                  _chkShowInChartToolbar_RunDyn_VerticalOscillation;
   private Button                  _chkShowInChartToolbar_RunDyn_VerticalRatio;

   private Button                  _chkShowInChartToolbar_Swim_Strokes;
   private Button                  _chkShowInChartToolbar_Swim_Swolf;

   private HashMap<String, Button> _chkShowInChartToolbar_Custom_Tracks   = new HashMap<>();
   private HashMap<String, Label>  _labelShowInChartToolbar_Custom_Tracks = new HashMap<>();

   private ScrolledComposite       _scrolledContainer;
   private Composite               _container;

   public SlideoutTourChartGraphs(final Control ownerControl,
                                  final ToolBar toolBar,
                                  final TourChart tourChart,
                                  final IDialogSettings state) {

      super(ownerControl, toolBar);

      _tourChart = tourChart;
      _state = state;
   }

   private int computePreferredHeight(final Composite parent) {
      final Label text = new Label(parent, SWT.BORDER);
      text.setText("X");
      final Point preferredSize = text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
      text.dispose();
      return GRID_TOOLBAR_SLIDEOUT_NB_ROW * (preferredSize.y + GRID_TOOLBAR_SLIDEOUT_DEFAULT_HORIZONTAL_SPACING);
   }

   private void createActions() {

      /*
       * Action: Restore default
       */
      _actionRestoreDefaults = new Action() {
         @Override
         public void run() {
            resetToDefaults();
         }
      };

      _actionRestoreDefaults.setImageDescriptor(//
            TourbookPlugin.getImageDescriptor(Messages.Image__App_RestoreDefault));
      _actionRestoreDefaults.setToolTipText(Messages.App_Action_RestoreDefault_Tooltip);

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

      final TourData tourData = TourManager.getInstance().getActiveTourChart().getTourData();
      if (tourData != null && tourData.getCustomTracks() != null && tourData.getCustomTracks().size() > 0) {
         /*
          * TOUR contains CUSTOM TRACKS
          */

         _scrolledContainer = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);

         _container = new Composite(_scrolledContainer, SWT.NONE);

         GridDataFactory.fillDefaults().grab(true, false).applyTo(_container);

         GridLayoutFactory.fillDefaults().numColumns(12).applyTo(_container);

         createUI_GraphAction(_container, TourManager.GRAPH_ALTITUDE);
         _chkShowInChartToolbar_Altitude = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Altitude);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Altitude);

         createUI_GraphAction(_container, TourManager.GRAPH_PULSE);
         _chkShowInChartToolbar_Pulse = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Heartbeat);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Heartbeat);

         createUI_GraphAction(_container, TourManager.GRAPH_SPEED);
         _chkShowInChartToolbar_Speed = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Speed);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Speed);

         createUI_GraphAction(_container, TourManager.GRAPH_PACE);
         _chkShowInChartToolbar_Pace = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Pace);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Pace);

         createUI_GraphAction(_container, TourManager.GRAPH_POWER);
         _chkShowInChartToolbar_Power = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Power);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Power);

         createUI_GraphAction(_container, TourManager.GRAPH_TEMPERATURE);
         _chkShowInChartToolbar_Tempterature = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Temperature);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_Temperature);

         createUI_GraphAction(_container, TourManager.GRAPH_GRADIENT);
         _chkShowInChartToolbar_Gradient = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Gradient);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Gradient);

         createUI_GraphAction(_container, TourManager.GRAPH_ALTIMETER);
         _chkShowInChartToolbar_Altimeter = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Altimeter);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_Altimeter);

         createUI_GraphAction(_container, TourManager.GRAPH_CADENCE);
         _chkShowInChartToolbar_Cadence = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Cadence);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Cadence);

         createUI_GraphAction(_container, TourManager.GRAPH_GEARS);
         _chkShowInChartToolbar_Gears = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Gears);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container, net.tourbook.common.Messages.Graph_Label_Gears);

         createUI_GraphAction(_container, TourManager.GRAPH_RUN_DYN_STANCE_TIME);
         _chkShowInChartToolbar_RunDyn_StanceTime = createUI_GraphCheckbox_Custom_Tracks(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime);

         createUI_GraphAction(_container, TourManager.GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
         _chkShowInChartToolbar_RunDyn_StanceTimeBalance = createUI_GraphCheckbox_Custom_Tracks(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalance);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalance);

         createUI_GraphAction(_container, TourManager.GRAPH_RUN_DYN_STEP_LENGTH);
         _chkShowInChartToolbar_RunDyn_StepLength = createUI_GraphCheckbox_Custom_Tracks(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength);

         createUI_GraphAction(_container, TourManager.GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
         _chkShowInChartToolbar_RunDyn_VerticalOscillation = createUI_GraphCheckbox_Custom_Tracks(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation);

         createUI_GraphAction(_container, TourManager.GRAPH_RUN_DYN_VERTICAL_RATIO);
         _chkShowInChartToolbar_RunDyn_VerticalRatio = createUI_GraphCheckbox_Custom_Tracks(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio);

         createUI_GraphAction(_container, TourManager.GRAPH_SWIM_STROKES);
         _chkShowInChartToolbar_Swim_Strokes = createUI_GraphCheckbox_Custom_Tracks(_container,
               net.tourbook.common.Messages.Graph_Label_Swim_Strokes);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_Swim_Strokes);

         createUI_GraphAction(_container, TourManager.GRAPH_SWIM_SWOLF);
         _chkShowInChartToolbar_Swim_Swolf = createUI_GraphCheckbox_Custom_Tracks(_container, net.tourbook.common.Messages.Graph_Label_Swim_Swolf);
         createUI_GraphCheckbox_Custom_Tracks_Label(_container,
               net.tourbook.common.Messages.Graph_Label_Swim_Swolf);

         {
            //CUSTOM TRACKS toolbarChart entries
            for (final Map.Entry<String, Button> entry : _chkShowInChartToolbar_Custom_Tracks.entrySet()) {
               if (entry.getValue() != null && !entry.getValue().isDisposed()) {
                  entry.getValue().dispose();
               }
            }
            for (final Map.Entry<String, Label> entry : _labelShowInChartToolbar_Custom_Tracks.entrySet()) {
               if (entry.getValue() != null && !entry.getValue().isDisposed()) {
                  entry.getValue().dispose();
               }
            }
            _labelShowInChartToolbar_Custom_Tracks.clear();
            _chkShowInChartToolbar_Custom_Tracks.clear();

            if (tourData != null && tourData.getCustomTracks() != null && tourData.getCustomTracks().size() > 0) {
               int cnt = 0;
               final HashMap<String, float[]> custTrk = tourData.getCustomTracks();
               final ArrayList<String> listKey = new ArrayList<>(custTrk.keySet());
               java.util.Collections.sort(listKey);
               final HashMap<String, CustomTrackDefinition> custTrkDefinitions =
                     tourData.getCustomTracksDefinition();
               for (int idx = 0; idx < listKey.size(); idx++) {
                  if (TourManager.MAX_VISIBLE_CUSTOM_TRACKS_DEBUG) {
                     if (cnt >= TourManager.MAX_VISIBLE_CUSTOM_TRACKS) {
                        break;
                     }
                  }
                  createUI_GraphAction(_container, TourManager.GRAPH_CUSTOM_TRACKS + idx);
                  final String key = listKey.get(idx);
                  final CustomTrackDefinition custTrkDefinition = custTrkDefinitions.get(key);
                  final String toolTip = custTrkDefinition.getName();

                  final Button chkShowInChartToolbar_Cust_Track = createUI_GraphCheckbox_Custom_Tracks(_container, toolTip);
                  _chkShowInChartToolbar_Custom_Tracks.put(key, chkShowInChartToolbar_Cust_Track);
                  final Label _labelShowInChartToolbar_Custom_Track = createUI_GraphCheckbox_Custom_Tracks_Label(_container, toolTip);
                  _labelShowInChartToolbar_Custom_Tracks.put(key, _labelShowInChartToolbar_Custom_Track);
                  cnt++;
               }
            }
         }

         _scrolledContainer.setContent(_container);
         _scrolledContainer.setExpandVertical(true);
         _scrolledContainer.setExpandHorizontal(true);

         _scrolledContainer.addListener(SWT.Resize, event -> {
            if (_scrolledContainer != null && !_scrolledContainer.isDisposed()
                  && _container != null && !_container.isDisposed()) {
               final int width = _scrolledContainer.getClientArea().width;
               _scrolledContainer.setMinSize(_container.computeSize(width, SWT.DEFAULT));
            }
         });

         final GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
         gridData.heightHint = computePreferredHeight(_container);
         _scrolledContainer.setLayoutData(gridData);

      } else {
         /*
          * Tour doesn't contain Custom Tracks
          */
         final Composite container = new Composite(parent, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(GRID_TOOLBAR_SLIDEOUT_NB_COLUMN).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {

            {
               /*
                * Actions: chart graphs
                */
               {
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
                  /*
                   * Checkbox: show in chart toolbar
                   */
                  _chkShowInChartToolbar_Altitude = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Pulse = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Speed = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Pace = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Power = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Tempterature = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Gradient = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Altimeter = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Cadence = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Gears = createUI_GraphCheckbox(container);

                  _chkShowInChartToolbar_RunDyn_StanceTime = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_RunDyn_StanceTimeBalance = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_RunDyn_StepLength = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_RunDyn_VerticalOscillation = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_RunDyn_VerticalRatio = createUI_GraphCheckbox(container);

                  _chkShowInChartToolbar_Swim_Strokes = createUI_GraphCheckbox(container);
                  _chkShowInChartToolbar_Swim_Swolf = createUI_GraphCheckbox(container);
               }

            }
         }
      }
   }

   private void createUI_GraphAction(final Composite parent, final int graphId) {

      final ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
      final ToolBarManager tbm = new ToolBarManager(toolbar);

      tbm.add(_tourChart.getGraphAction(graphId));

      tbm.update(true);
   }

   private Button createUI_GraphCheckbox(final Composite parent) {

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

   private Button createUI_GraphCheckbox_Custom_Tracks(final Composite parent, final String toolTip) {

      final Button checkbox = new Button(parent, SWT.CHECK);

      checkbox.setToolTipText(toolTip);
      checkbox.addSelectionListener(_defaultSelectionListener);

      GridDataFactory
            .fillDefaults()
            .grab(true, false)
            .align(SWT.CENTER, SWT.FILL)
            .applyTo(checkbox);

      return checkbox;
   }

   private Label createUI_GraphCheckbox_Custom_Tracks_Label(final Composite parent, final String text) {
      final Label labelbox = new Label(parent, SWT.NONE);
      labelbox.setText(text);
      GridDataFactory
            .fillDefaults()
            .grab(true, false)
            .align(SWT.LEFT, SWT.FILL)
            .applyTo(labelbox);

      return labelbox;
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

   private void resetToDefaults() {

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

		_chkShowInChartToolbar_Altitude.setSelection(							Util.getStateBoolean(_state, TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE,								TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE_DEFAULT));
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

		final TourData tourData = TourManager.getInstance().getActiveTourChart().getTourData();
		int cnt = 0;
      final HashMap<String, float[]> customTracksMap = tourData.getCustomTracks();
      final ArrayList<String> listKey = new ArrayList<>(customTracksMap.keySet());
      java.util.Collections.sort(listKey);
      //final HashMap<String, CustomTrackDefinition> custTrkDef = tourData.getCustomTracksDefinition();
      final HashMap<String, Boolean> state_CustomTracks = TourManager.getInstance().getActiveTourChart().get_state_CustomTracksToolBarChart();
      for (final String key : listKey) {
         if (TourManager.MAX_VISIBLE_CUSTOM_TRACKS_DEBUG) {
            if (cnt >= TourManager.MAX_VISIBLE_CUSTOM_TRACKS) {
               break;
            }
         }
         final Button chkShowInChartToolbar_Cust_Track = _chkShowInChartToolbar_Custom_Tracks.get(key);

         if(chkShowInChartToolbar_Cust_Track != null && state_CustomTracks!= null) {
            final Boolean state = state_CustomTracks.getOrDefault(key, false);
            chkShowInChartToolbar_Cust_Track.setSelection(state);
         }

         cnt++;
      }
// SET_FORMATTING_ON
   }

   private void saveState() {

// SET_FORMATTING_OFF

		_state.put(TourChart.STATE_IS_SHOW_IN_CHART_TOOLBAR_ALTITUDE, 								_chkShowInChartToolbar_Altitude.getSelection());
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

		final TourData tourData = TourManager.getInstance().getActiveTourChart().getTourData();
      int cnt = 0;
      final HashMap<String, float[]> customTracksMap = tourData.getCustomTracks();
      final ArrayList<String> listKey = new ArrayList<>(customTracksMap.keySet());
      java.util.Collections.sort(listKey);
      final LinkedHashMap<String, Boolean> state_CustomTracks = TourManager.getInstance().getActiveTourChart().get_state_CustomTracksToolBarChart();

      for (final String key : listKey) {
         if (TourManager.MAX_VISIBLE_CUSTOM_TRACKS_DEBUG) {
            if (cnt >= TourManager.MAX_VISIBLE_CUSTOM_TRACKS) {
               break;
            }
         }
         final Button chkShowInChartToolbar_Cust_Track = _chkShowInChartToolbar_Custom_Tracks.get(key);
         if(chkShowInChartToolbar_Cust_Track != null && state_CustomTracks != null ) {
            state_CustomTracks.put(key, chkShowInChartToolbar_Cust_Track.getSelection());
         }

         cnt++;
      }
// SET_FORMATTING_ON
   }
}
