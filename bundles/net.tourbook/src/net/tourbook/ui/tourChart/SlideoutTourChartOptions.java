/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
import java.util.Collections;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionOpenPrefDialog;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceTourChart;
import net.tourbook.srtm.IPreferences;
import net.tourbook.srtm.PrefPageSRTMData;
import net.tourbook.ui.ChartOptions_Grid;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * Tour chart properties slideout.
 */
public class SlideoutTourChartOptions extends ToolbarSlideout implements IActionResetToDefault {

   private final IPreferenceStore _prefStore           = TourbookPlugin.getPrefStore();

   private SelectionAdapter       _defaultSelectionListener;
   private MouseWheelListener     _defaultMouseWheelListener;

   private ActionOpenPrefDialog   _actionPrefDialog;
   private ActionResetToDefaults  _actionRestoreDefaults;

   private ChartOptions_Grid      _gridUI;

   private SelectionAdapter       _defaultSelectionAdapter;
   private FocusListener          _keepOpenListener;

   /**
    * Pulse graph values MUST be in sync with pulse graph labels
    */
   private PulseGraph[]           _allPulseGraph_Value = {

         PulseGraph.DEVICE_BPM___2ND_RR_AVERAGE,
         PulseGraph.DEVICE_BPM_ONLY,
         PulseGraph.RR_INTERVALS_ONLY,
         PulseGraph.RR_INTERVALS___2ND_RR_AVERAGE,
         PulseGraph.RR_INTERVALS___2ND_DEVICE_BPM,
         PulseGraph.RR_AVERAGE_ONLY,
         PulseGraph.RR_AVERAGE___2ND_DEVICE_BPM,

   };

   /**
    * Pulse graph labels MUST be in sync with pulse graph values
    */
   private String[]               _allPulseGraph_Label = {

         Messages.TourChart_PulseGraph_DeviceBpm_2nd_RRAverage,
         Messages.TourChart_PulseGraph_DeviceBpm_Only,
         Messages.TourChart_PulseGraph_RRIntervals_Only,
         Messages.TourChart_PulseGraph_RRIntervals_2nd_RRAverage,
         Messages.TourChart_PulseGraph_RRIntervals_2nd_DeviceBpm,
         Messages.TourChart_PulseGraph_RRAverage_Only,
         Messages.TourChart_PulseGraph_RRAverage_2nd_DeviceBpm,
   };

   private ArrayList<PulseGraph>  _possiblePulseGraph_Values;

   /*
    * UI controls
    */
   private TourChart _tourChart;

   private Button    _chkInvertPaceGraph;
   private Button    _chkSelectAllTimeSlices;
   private Button    _chkShowBreaktimeValues;
   private Button    _chkShowNightSections;
   private Button    _chkShowSrtmData;
   private Button    _chkShowStartTimeOnXAxis;
   private Button    _chkShowValuePointTooltip;
   private Button    _chkShowValuePointValue;

   private Label     _lblNightSectionsOpacity;

   private Spinner   _spinnerNightSectionsOpacity;

   private Combo     _comboPulseValueGraph;

   /**
    * @param ownerControl
    * @param toolBar
    * @param tourChart
    * @param gridPrefPrefix
    */
   public SlideoutTourChartOptions(final Control ownerControl,
                                   final ToolBar toolBar,
                                   final TourChart tourChart,
                                   final String gridPrefPrefix) {

      super(ownerControl, toolBar);

      _tourChart = tourChart;

      _gridUI = new ChartOptions_Grid(gridPrefPrefix);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);

      _actionPrefDialog = new ActionOpenPrefDialog(Messages.Tour_Action_EditChartPreferences, PrefPageAppearanceTourChart.ID);
      _actionPrefDialog.closeThisTooltip(this);
      _actionPrefDialog.setShell(_tourChart.getShell());
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
            createUI_20_Options(container);
            createUI_30_Graph(container);

            _gridUI.createUI(container);
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
      label.setText(Messages.Slideout_TourChartOptions_Label_Title);
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
      tbm.add(_actionPrefDialog);

      tbm.update(true);
   }

   private void createUI_20_Options(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         {
            /*
             * Show break time values
             */
            _chkShowBreaktimeValues = new Button(container, SWT.CHECK);
            _chkShowBreaktimeValues.setText(Messages.Tour_Action_ShowBreaktimeValues);
            _chkShowBreaktimeValues.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show start time on x-axis
             */
            _chkShowStartTimeOnXAxis = new Button(container, SWT.CHECK);
            _chkShowStartTimeOnXAxis.setText(Messages.Tour_Action_show_start_time_on_x_axis);
            _chkShowStartTimeOnXAxis.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show SRTM data
             */
            _chkShowSrtmData = new Button(container, SWT.CHECK);
            _chkShowSrtmData.setText(Messages.tour_action_show_srtm_data);
            _chkShowSrtmData.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onSelectSRTM();
               }
            });
         }
         {
            /*
             * Show night section
             */

            _chkShowNightSections = new Button(container, SWT.CHECK);
            _chkShowNightSections.setText(Messages.Slideout_TourChartOptions_Checkbox_ShowNightSections);
            _chkShowNightSections.addSelectionListener(_defaultSelectionListener);

            final Composite nightContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .indent(16, 0)
                  .applyTo(nightContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(nightContainer);
            {
               // label: night sections opacity
               _lblNightSectionsOpacity = new Label(nightContainer, SWT.CHECK);
               _lblNightSectionsOpacity.setText(Messages.Slideout_TourChartOptions_Label_NightSectionsOpacity);
               _lblNightSectionsOpacity.setToolTipText(Messages.Slideout_TourChartOptions_Label_NightSectionsOpacity_Tooltip);

               // spinner: Night sections opacity
               _spinnerNightSectionsOpacity = new Spinner(nightContainer, SWT.BORDER);
               _spinnerNightSectionsOpacity.setMinimum(0);
               _spinnerNightSectionsOpacity.setMaximum(100);
               _spinnerNightSectionsOpacity.setIncrement(1);
               _spinnerNightSectionsOpacity.setPageIncrement(10);
               _spinnerNightSectionsOpacity.setToolTipText(Messages.Slideout_TourChartOptions_Label_NightSectionsOpacity_Tooltip);
               _spinnerNightSectionsOpacity.addSelectionListener(_defaultSelectionListener);
               _spinnerNightSectionsOpacity.addMouseWheelListener(_defaultMouseWheelListener);
            }
         }
         {
            /*
             * Show value point value
             */
            _chkShowValuePointValue = new Button(container, SWT.CHECK);
            _chkShowValuePointValue.setText(Messages.Tour_Action_ShowValuePointValue);
            _chkShowValuePointValue.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Show value point tooltip
             */
            _chkShowValuePointTooltip = new Button(container, SWT.CHECK);
            _chkShowValuePointTooltip.setText(Messages.Tour_Action_ValuePointToolTip_IsVisible);
            _chkShowValuePointTooltip.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {

                  // set in pref store, tooltip is listening pref store modifications
                  _prefStore.setValue(
                        ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE,
                        _chkShowValuePointTooltip.getSelection());

               }
            });
         }
         {
            /*
             * Options to select all the time slices in between the left and right sliders or only
             * the current slider's one
             */
            _chkSelectAllTimeSlices = new Button(container, SWT.CHECK);
            _chkSelectAllTimeSlices.setText(Messages.Tour_Action_Select_Inbetween_Timeslices);
            _chkSelectAllTimeSlices.setToolTipText(Messages.Tour_Action_Select_Inbetween_Timeslices_Tooltip);
            _chkSelectAllTimeSlices.addSelectionListener(_defaultSelectionListener);
         }
      }
   }

   private void createUI_30_Graph(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_Graphs_Group_Graphs);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .applyTo(group);
      GridLayoutFactory.swtDefaults()
            .numColumns(2)
            .applyTo(group);
      {
         {
            /*
             * Pulse graph
             */

            // label
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.Slideout_TourChartOptions_Label_PulseGraph);

            // combo
            _comboPulseValueGraph = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
            _comboPulseValueGraph.setVisibleItemCount(20);
            _comboPulseValueGraph.addSelectionListener(_defaultSelectionAdapter);
            _comboPulseValueGraph.addFocusListener(_keepOpenListener);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.FILL, SWT.FILL)
                  .applyTo(_comboPulseValueGraph);
         }
         {
            /*
             * Pace graph
             */
            _chkInvertPaceGraph = new Button(group, SWT.CHECK);
            _chkInvertPaceGraph.setText(Messages.Slideout_TourChartOptions_Checkbox_InvertPaceGraph);
            _chkInvertPaceGraph.setToolTipText(Messages.Slideout_TourChartOptions_Checkbox_InvertPaceGraph_Tooltip);
            _chkInvertPaceGraph.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .applyTo(_chkInvertPaceGraph);
         }
      }
   }

   private void enableControls() {

      final boolean isShowNightSections = _chkShowNightSections.getSelection();

      _lblNightSectionsOpacity.setEnabled(isShowNightSections);
      _spinnerNightSectionsOpacity.setEnabled(isShowNightSections);
   }

   private PulseGraph getSelectedPulseGraph() {

      return _allPulseGraph_Value[_comboPulseValueGraph.getSelectionIndex()];
   }

   private void initUI() {

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
      };

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };

      _defaultSelectionAdapter = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onChangeUI();
         }
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
      _tourChart.updateTourChart();
   }

   private void onSelectSRTM() {

      final boolean isSrtmSelected = _chkShowSrtmData.getSelection();
      if (isSrtmSelected) {

         // check if the user has validated the SRTM download

         String srtmAccountErrorMessage = null;
         String focusField = null;

         final String password = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD);
         final String username = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME);
         if (password.trim().length() == 0 || username.trim().length() == 0) {

            srtmAccountErrorMessage = Messages.SRTM_Download_Info_UsernamePasswordIsEmpty;
            focusField = PrefPageSRTMData.FOCUS_USER_NAME;
         }

         final long validationDate = _prefStore.getLong(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE);
         if (srtmAccountErrorMessage == null && validationDate < 0) {

            srtmAccountErrorMessage = Messages.SRTM_Download_Info_NoDownloadValidation;
            focusField = PrefPageSRTMData.FOCUS_VALIDATE_DOWNLOAD;
         }

         if (srtmAccountErrorMessage != null && srtmAccountErrorMessage.length() > 0) {

            // SRTM download is not valid

            /*
             * Close slideout now otherwise on Linux the message dialog is behind the slideout even
             * when it's on top and the slidout is it's shell
             */
            close();

            final Shell shell = Display.getDefault().getActiveShell();

            MessageDialog.openInformation(shell,
                  Messages.SRTM_Download_Dialog_SRTMDownloadValidation_Title,
                  srtmAccountErrorMessage);

            // show SRTM pref page
            PreferencesUtil.createPreferenceDialogOn(
                  shell,
                  PrefPageSRTMData.ID,
                  null,

                  // set focus to a control
                  focusField).open();

            return;
         }
      }

      onChangeUI();
   }

   @Override
   public void resetToDefaults() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      final boolean isSelectInBetweenTimeSlices = _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES);
      final boolean isShowBreaktimeValues = _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE);
      final boolean isShowPaceGraphInverted = _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED);
      final boolean isShowValuePointTooltip = _prefStore.getDefaultBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE);
      final boolean isShowValuePointValue = _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SHOW_VALUE_POINT_VALUE);
      final boolean isSrtmDataVisible = _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);
      final boolean isTourStartTime = _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME);

      final boolean isShowNightSections = _prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_SHOW_NIGHT_SECTIONS);
      final int nightSectionsOpacity = _prefStore.getDefaultInt(ITourbookPreferences.GRAPH_NIGHT_SECTIONS_OPACITY);

      final X_AXIS_START_TIME xAxisStartTime = isTourStartTime
            ? X_AXIS_START_TIME.TOUR_START_TIME
            : X_AXIS_START_TIME.START_WITH_0;

      tcc.isShowBreaktimeValues = isShowBreaktimeValues;
      tcc.isShowNightSections = isShowNightSections;
      tcc.isShowValuePointValue = isShowValuePointValue;
      tcc.isSRTMDataVisible = isSrtmDataVisible;
      tcc.pulseGraph = TourChart.PULSE_GRAPH_DEFAULT;
      tcc.xAxisTime = xAxisStartTime;

      _chkInvertPaceGraph.setSelection(isShowPaceGraphInverted);
      _chkSelectAllTimeSlices.setSelection(isSelectInBetweenTimeSlices);
      _chkShowBreaktimeValues.setSelection(isShowBreaktimeValues);
      _chkShowSrtmData.setSelection(isSrtmDataVisible);
      _chkShowStartTimeOnXAxis.setSelection(isTourStartTime);
      _chkShowValuePointTooltip.setSelection(isShowValuePointTooltip);
      _chkShowValuePointValue.setSelection(isShowValuePointValue);

      _chkShowNightSections.setSelection(isShowNightSections);
      _spinnerNightSectionsOpacity.setSelection(nightSectionsOpacity);

      setSelection_PulseGraph(TourChart.PULSE_GRAPH_DEFAULT,
            tcc.canShowPulseSerie,
            tcc.canShowPulseTimeSerie,
            tcc.isShowTimeOnXAxis);

      // this is not set in saveState()
      _prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, isShowValuePointTooltip);

      _gridUI.resetToDefaults();

      onChangeUI();
   }

   private void restoreState() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      if (tcc == null) {
         // this occur when tour chart is empty
         return;
      }

      final boolean canShowTimeOnXAxis = tcc.isShowTimeOnXAxis;
      final boolean canShowSRTMData = tcc.canShowSRTMData;

      _chkInvertPaceGraph.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED));

      _chkShowNightSections.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_NIGHT_SECTIONS));
      _spinnerNightSectionsOpacity.setSelection(_prefStore.getInt(ITourbookPreferences.GRAPH_NIGHT_SECTIONS_OPACITY));

      _chkShowSrtmData.setEnabled(canShowSRTMData);
      _chkShowSrtmData.setSelection(tcc.isSRTMDataVisible);

      _chkShowStartTimeOnXAxis.setEnabled(canShowTimeOnXAxis);
      _chkShowStartTimeOnXAxis.setSelection(tcc.xAxisTime == X_AXIS_START_TIME.TOUR_START_TIME);

      _chkShowBreaktimeValues.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE));
      _chkShowValuePointTooltip.setSelection(_prefStore.getBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE));
      _chkShowValuePointValue.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_VALUE_POINT_VALUE));

      _chkSelectAllTimeSlices.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES));

      setSelection_PulseGraph(tcc.pulseGraph,
            tcc.canShowPulseSerie,
            tcc.canShowPulseTimeSerie,
            tcc.isShowTimeOnXAxis);

      _gridUI.restoreState();
   }

   private void saveState() {

      final TourChartConfiguration tcc = _tourChart.getTourChartConfig();

      final boolean isSelectInBetweenTimeSlices = _chkSelectAllTimeSlices.getSelection();
      final boolean isShowBreaktimeValues = _chkShowBreaktimeValues.getSelection();
      final boolean isShowPaceGraphInverted = _chkInvertPaceGraph.getSelection();
      final boolean isShowValuePointValue = _chkShowValuePointValue.getSelection();
      final boolean isSrtmDataVisible = _chkShowSrtmData.getSelection();

      final boolean isShowNightSections = _chkShowNightSections.getSelection();
      final boolean isTourStartTime = _chkShowStartTimeOnXAxis.getSelection();

      final int nightSectionsOpacity = _spinnerNightSectionsOpacity.getSelection();

      final X_AXIS_START_TIME xAxisStartTime = isTourStartTime
            ? X_AXIS_START_TIME.TOUR_START_TIME
            : X_AXIS_START_TIME.START_WITH_0;

      final PulseGraph pulseGraph = getSelectedPulseGraph();

      /*
       * Update pref store
       */
      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE, isShowBreaktimeValues);
      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES, isSelectInBetweenTimeSlices);
      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED, isShowPaceGraphInverted);
      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_SHOW_VALUE_POINT_VALUE, isShowValuePointValue);
      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_SRTM_VISIBLE, isSrtmDataVisible);
      _prefStore.setValue(ITourbookPreferences.GRAPH_PULSE_GRAPH_VALUES, pulseGraph.name());
      _prefStore.setValue(ITourbookPreferences.GRAPH_X_AXIS_STARTTIME, isTourStartTime);

      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_SHOW_NIGHT_SECTIONS, isShowNightSections);
      _prefStore.setValue(ITourbookPreferences.GRAPH_NIGHT_SECTIONS_OPACITY, nightSectionsOpacity);

      _gridUI.saveState();

      _tourChart.setupChartConfig();

      /*
       * Update chart config
       */
      tcc.isShowBreaktimeValues = isShowBreaktimeValues;
      tcc.isShowNightSections = isShowNightSections;
      tcc.isShowValuePointValue = isShowValuePointValue;
      tcc.isSRTMDataVisible = isSrtmDataVisible;
      tcc.pulseGraph = pulseGraph;
      tcc.xAxisTime = xAxisStartTime;
   }

   /**
    * @param requestedPulseGraph
    * @param canShowPulseSerie
    * @param canShowPulseTimeSerie
    * @param isShowTimeOnXAxis
    *           When distance is displayed on the x-axis, then it is not supported to show the R-R
    *           intervals --> too complicated
    */
   private void setSelection_PulseGraph(final PulseGraph requestedPulseGraph,
                                        final boolean canShowPulseSerie,
                                        final boolean canShowPulseTimeSerie,
                                        final boolean isShowTimeOnXAxis) {

      _comboPulseValueGraph.removeAll();
      _possiblePulseGraph_Values = new ArrayList<>();

      if (canShowPulseSerie && canShowPulseTimeSerie && isShowTimeOnXAxis == true) {

         // all options can be selected

         // update UI
         for (final String pulseGraphLabel : _allPulseGraph_Label) {
            _comboPulseValueGraph.add(pulseGraphLabel);
         }

         // update model
         Collections.addAll(_possiblePulseGraph_Values, _allPulseGraph_Value);

      } else if (canShowPulseSerie && canShowPulseTimeSerie && isShowTimeOnXAxis == false) {

         // distance is displayed on the x-axis -> rr intervals cannot be displayed

         for (int graphIndex = 0; graphIndex < _allPulseGraph_Value.length; graphIndex++) {

            final PulseGraph pulseGraph = _allPulseGraph_Value[graphIndex];

            if (pulseGraph == PulseGraph.RR_INTERVALS_ONLY
                  || pulseGraph == PulseGraph.RR_INTERVALS___2ND_DEVICE_BPM
                  || pulseGraph == PulseGraph.RR_INTERVALS___2ND_RR_AVERAGE) {

               // skip unsupported features
               continue;
            }

            _comboPulseValueGraph.add(_allPulseGraph_Label[graphIndex]);
            _possiblePulseGraph_Values.add(pulseGraph);
         }

      } else if (canShowPulseSerie) {

         // update UI
         _comboPulseValueGraph.add(Messages.TourChart_PulseGraph_DeviceBpm_Only);

         // update model
         _possiblePulseGraph_Values.add(PulseGraph.DEVICE_BPM_ONLY);

      } else if (canShowPulseTimeSerie) {

         // update UI
         _comboPulseValueGraph.add(Messages.TourChart_PulseGraph_RRAverage_Only);

         if (isShowTimeOnXAxis) {
            _comboPulseValueGraph.add(Messages.TourChart_PulseGraph_RRIntervals_Only);
            _comboPulseValueGraph.add(Messages.TourChart_PulseGraph_RRIntervals_2nd_RRAverage);
            _comboPulseValueGraph.add(Messages.TourChart_PulseGraph_RRIntervals_2nd_DeviceBpm);
         }

         // update model
         _possiblePulseGraph_Values.add(PulseGraph.RR_AVERAGE_ONLY);

         if (isShowTimeOnXAxis) {
            _possiblePulseGraph_Values.add(PulseGraph.RR_INTERVALS_ONLY);
            _possiblePulseGraph_Values.add(PulseGraph.RR_INTERVALS___2ND_RR_AVERAGE);
            _possiblePulseGraph_Values.add(PulseGraph.RR_INTERVALS___2ND_DEVICE_BPM);
         }
      }

      /*
       * Select pulse graph
       */
      final int numComboItems = _possiblePulseGraph_Values.size();

      if (numComboItems == 0) {

         // pulse values are not available

         _comboPulseValueGraph.add(Messages.App_Label_NotAvailable);
         _comboPulseValueGraph.select(0);

      } else {

         // set first item in combobox as default
         int comboIndex = 0;

         // get index of the requested pulse graph
         for (int graphIndex = 0; graphIndex < numComboItems; graphIndex++) {

            if (_possiblePulseGraph_Values.get(graphIndex).equals(requestedPulseGraph)) {
               comboIndex = graphIndex;
               break;
            }
         }

         _comboPulseValueGraph.select(comboIndex);
      }

      // disable combo when only 1 or 0 items can be selected
      _comboPulseValueGraph.setEnabled(numComboItems > 1);
   }
}
