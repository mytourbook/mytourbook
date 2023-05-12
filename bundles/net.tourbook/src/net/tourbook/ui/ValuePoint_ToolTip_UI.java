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
package net.tourbook.ui;

import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.HoveredValuePointData;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.IPinned_ToolTip;
import net.tourbook.common.tooltip.IPinned_Tooltip_Owner;
import net.tourbook.common.tooltip.Pinned_ToolTip_Shell;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.PulseGraph;
import net.tourbook.ui.tourChart.TourChart;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * This tooltip is displayed when the mouse is hovered over a value point in a line graph and
 * displays value point information.
 */
public class ValuePoint_ToolTip_UI extends Pinned_ToolTip_Shell implements IPinned_ToolTip {

   private final IPreferenceStore         _prefStore     = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener        _prefChangeListener;

   private TourData                       _tourData;

   private ValuePoint_ToolTip_MenuManager _ttMenuMgr;

   private ActionOpenTooltipMenu          _actionOpenTooltipMenu;
   private ActionCloseTooltip             _actionCloseTooltip;

   private String                         _headerTitle;
   private String                         _prefKey_TooltipIsVisible;

   private int                            _devXMouse;
   private int                            _devYMouse;

   /**
    * Global state if the tooltip is visible.
    */
   private boolean                        _isToolTipVisible;
   private int                            _currentValueIndex;
   private int                            _valueUnitDistance;
   private double                         _chartZoomFactor;

   private int[]                          _updateCounter = new int[] { 0 };
   private long                           _lastUpdateUITime;
   private boolean                        _isHorizontalOrientation;

   private final NumberFormat             _nf0           = NumberFormat.getNumberInstance();
   private final NumberFormat             _nf1           = NumberFormat.getNumberInstance();
   private final NumberFormat             _nf1min        = NumberFormat.getNumberInstance();
   private final NumberFormat             _nf1NoGroup    = NumberFormat.getNumberInstance();
   private final NumberFormat             _nf2           = NumberFormat.getNumberInstance();
   private final NumberFormat             _nf3           = NumberFormat.getNumberInstance();
   private final NumberFormat             _nf3NoGroup    = NumberFormat.getNumberInstance();
   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf1min.setMinimumFractionDigits(1);
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);

      _nf1NoGroup.setMinimumFractionDigits(1);
      _nf1NoGroup.setMaximumFractionDigits(1);
      _nf1NoGroup.setGroupingUsed(false);

      _nf3NoGroup.setMinimumFractionDigits(3);
      _nf3NoGroup.setMaximumFractionDigits(3);
      _nf3NoGroup.setGroupingUsed(false);
   }

   /**
    * Contains all graph id's which are set to be visible but can be unavailable
    */
   private long    _allVisibleValueIds;
   private long    _allVisibleAndAvailable_ValueIds;
   private int     _allVisibleAndAvailable_ValueCounter;

   private boolean _isVisible_And_Available_Altimeter;
   private boolean _isVisible_And_Available_Cadence;
   private boolean _isVisible_And_Available_ChartZoomFactor;
   private boolean _isVisible_And_Available_Distance;
   private boolean _isVisible_And_Available_Elevation;
   private boolean _isVisible_And_Available_Gears;
   private boolean _isVisible_And_Available_Gradient;
   private boolean _isVisible_And_Available_Pace;
   private boolean _isVisible_And_Available_Pace_Summarized;
   private boolean _isVisible_And_Available_Power;
   private boolean _isVisible_And_Available_Pulse;
   private boolean _isVisible_And_Available_Speed;
   private boolean _isVisible_And_Available_Speed_Summarized;
   private boolean _isVisible_And_Available_Temperature;
   private boolean _isVisible_And_Available_TimeDuration;
   private boolean _isVisible_And_Available_TimeOfDay;
   private boolean _isVisible_And_Available_TimeMoving;
   private boolean _isVisible_And_Available_TimeRecorded;
   private boolean _isVisible_And_Available_TimeSlice;
   private boolean _isVisible_And_Available_TourCompareResult;

   private boolean _isVisible_And_Available_RunDyn_StanceTime;
   private boolean _isVisible_And_Available_RunDyn_StanceTimeBalance;
   private boolean _isVisible_And_Available_RunDyn_StepLength;
   private boolean _isVisible_And_Available_RunDyn_VerticalOscillation;
   private boolean _isVisible_And_Available_RunDyn_VerticalRatio;

   private boolean _isAvailable_Pulse_BpmFromDevice;
   private boolean _isAvailable_Pulse_RRIntervals;

   private boolean _canBeDisplayed_ChartZoomFactor = true;

   /*
    * UI resources
    */
   private Color                   _fgBorder;
   private final ColorCache        _colorCache   = new ColorCache();
   private final GraphColorManager _colorManager = GraphColorManager.getInstance();

   /*
    * UI controls
    */
   private Composite _shellContainer;

   private ToolBar   _toolbarControl;

   private Label     _lblAltimeter;
   private Label     _lblAltimeter_Unit;
   private Label     _lblCadence;
   private Label     _lblCadence_Unit;
   private Label     _lblChartZoomFactor;
   private Label     _lblDataSerieCurrent;
   private Label     _lblDataSerieMax;
   private Label     _lblDistance;
   private Label     _lblDistance_Unit;
   private Label     _lblElevation;
   private Label     _lblElevation_Unit;
   private Label     _lblGearRatio;
   private Label     _lblGearTeeth;
   private Label     _lblGradient;
   private Label     _lblGradient_Unit;
   private Label     _lblPace;
   private Label     _lblPace_Unit;
   private Label     _lblPace_Summarized;
   private Label     _lblPace_Summarized_Unit;
   private Label     _lblPower;
   private Label     _lblPower_Unit;
   private Label     _lblPulse;
   private Label     _lblPulse_Unit;
   private Label     _lblSpeed;
   private Label     _lblSpeed_Unit;
   private Label     _lblSpeed_Summarized;
   private Label     _lblSpeed_Summarized_Unit;
   private Label     _lblTemperature;
   private Label     _lblTemperature_Unit;
   private Label     _lblTimeDuration;
   private Label     _lblTimeDuration_Unit;
   private Label     _lblTimeOfDay;
   private Label     _lblTimeOfDay_Unit;
   private Label     _lblTimeMoving;
   private Label     _lblTimeMoving_Unit;
   private Label     _lblTimeRecorded;
   private Label     _lblTimeRecorded_Unit;
   private Label     _lblTourCompareResult;

   private Label     _lblRunDyn_StanceTime;
   private Label     _lblRunDyn_StanceTime_Unit;
   private Label     _lblRunDyn_StanceTimeBalance;
   private Label     _lblRunDyn_StanceTimeBalance_Unit;
   private Label     _lblRunDyn_StepLength;
   private Label     _lblRunDyn_StepLength_Unit;
   private Label     _lblRunDyn_VerticalOscillation;
   private Label     _lblRunDyn_VerticalOscillation_Unit;
   private Label     _lblRunDyn_VerticalRatio;
   private Label     _lblRunDyn_VerticalRatio_Unit;

   private class ActionCloseTooltip extends Action {

      public ActionCloseTooltip() {

         super(null, IAction.AS_PUSH_BUTTON);

         setToolTipText(OtherMessages.APP_ACTION_CLOSE_TOOLTIP);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Close));
      }

      @Override
      public void run() {
         hide();
      }
   }

   private class ActionOpenTooltipMenu extends Action {

      public ActionOpenTooltipMenu() {

         super(null, IAction.AS_PUSH_BUTTON);

         setToolTipText(Messages.Tooltip_ValuePoint_Action_OpenToolTipMenu_ToolTip);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Options));
      }

      @Override
      public void runWithEvent(final Event event) {
         _ttMenuMgr.openToolTipMenu(event, _tourData, _allVisibleValueIds, _isHorizontalOrientation);
      }
   }

   /**
    * @param tooltipOwner
    * @param title
    *           Header title
    * @param state
    * @param prefKey_TooltipIsVisible
    *           Key in pref store to show/hide value point tooltip
    * @param isVisible_ChartZoomFactor
    */
   public ValuePoint_ToolTip_UI(final IPinned_Tooltip_Owner tooltipOwner,
                                final String title,
                                final IDialogSettings state,
                                final String prefKey_TooltipIsVisible,
                                final boolean isVisible_ChartZoomFactor) {

      super(tooltipOwner, state);

      _headerTitle = title;
      _prefKey_TooltipIsVisible = prefKey_TooltipIsVisible;

      _canBeDisplayed_ChartZoomFactor = isVisible_ChartZoomFactor;

      // get state if the tooltip is visible or hidden
      _isToolTipVisible = _prefStore.getBoolean(_prefKey_TooltipIsVisible);

      _allVisibleValueIds = Util.getStateLong(
            state,
            ValuePoint_ToolTip_MenuManager.STATE_VALUE_POINT_TOOLTIP_VISIBLE_GRAPHS,
            ValuePoint_ToolTip_MenuManager.DEFAULT_GRAPHS);

      /*
       * orientation
       */
      final String stateOrientation = Util.getStateString(
            state,
            ValuePoint_ToolTip_MenuManager.STATE_VALUE_POINT_TOOLTIP_ORIENTATION,
            ValuePoint_ToolTip_MenuManager.DEFAULT_ORIENTATION.name());

      _isHorizontalOrientation = ValuePoint_ToolTip_Orientation.valueOf(
            stateOrientation) == ValuePoint_ToolTip_Orientation.Horizontal;

      addPrefListener();
   }

   void actionHideToolTip() {

      _prefStore.setValue(_prefKey_TooltipIsVisible, false);

      _isToolTipVisible = false;

      hide();
   }

   void actionOrientation(final ValuePoint_ToolTip_Orientation orientation, final boolean isReopenToolTip) {

      _isHorizontalOrientation = orientation == ValuePoint_ToolTip_Orientation.Horizontal;

      if (isReopenToolTip) {
         reopen();
      }
   }

   void actionSetDefaults(final long allVisibleValues, final ValuePoint_ToolTip_Orientation orientation) {

      actionOrientation(orientation, false);
      actionVisibleValues(allVisibleValues);

      state.put(STATE_PINNED_TOOLTIP_PIN_LOCATION, DEFAULT_PIN_LOCATION.name());

      setPinLocation(DEFAULT_PIN_LOCATION);
   }

   ToolItem actionVisibleValues(final long visibleValues) {

      // update value states
      updateStateVisibleValues(visibleValues);

      reopen();

      /**
       * Get item which is opening the value point tooltip
       * <p>
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!<br>
       * This is a hack because the toolbar contains only one item, hopefully this will not change.
       * <br>
       * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
       */
      final ToolItem toolItem = _toolbarControl.getItem(0);

      return toolItem;
   }

   private void addPrefListener() {

      _prefChangeListener = propertyChangeEvent -> {

         final String property = propertyChangeEvent.getProperty();

         /*
          * create a new chart configuration when the preferences has changed
          */
         if (property.equals(_prefKey_TooltipIsVisible)) {

            _isToolTipVisible = (Boolean) propertyChangeEvent.getNewValue();

            if (_isToolTipVisible) {
               show(new Point(_devXMouse, _devYMouse));
            } else {
               hide();
            }

         } else if (property.equals(ITourbookPreferences.GRAPH_COLORS_HAS_CHANGED)) {

            // dispose old colors
            _colorCache.dispose();

            reopen();
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private void createActions() {

      _ttMenuMgr = new ValuePoint_ToolTip_MenuManager(this, state);

      _ttMenuMgr.setCanBeDisplayed_ChartZoomFactor(_canBeDisplayed_ChartZoomFactor);

      _actionCloseTooltip = new ActionCloseTooltip();
      _actionOpenTooltipMenu = new ActionOpenTooltipMenu();
   }

   @Override
   protected Composite createToolTipContentArea(final Event event, final Composite parent) {

      createActions();

      final Composite shell = createUI(parent);

      return shell;
   }

   private Composite createUI(final Composite parent) {

      _fgBorder = UI.IS_DARK_THEME
            ? ThemeUtil.getDefaultBackgroundColor_Shell()
            : _colorCache.getColor(new RGB(0xe5, 0xe5, 0xcb));

      _valueUnitDistance = _isHorizontalOrientation ? 2 : 5;

      final Composite shell = createUI_010_Shell(parent);

      updateUI(_currentValueIndex);

      updateUI_Runnable_Colors();

      return shell;

   }

   private Composite createUI_010_Shell(final Composite parent) {

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      _shellContainer = new Composite(parent, SWT.NONE);
      _shellContainer.addPaintListener(this::onPaintShellContainer);
//      _shellContainer.setBackground(UI.SYS_COLOR_GREEN);
      {
         if (_isHorizontalOrientation) {

            // horizontal orientation

            GridLayoutFactory.fillDefaults()
                  .spacing(0, 0)
                  .numColumns(3)

                  // set margin to draw the border
                  .extendedMargins(1, 1, 1, 1)

                  .applyTo(_shellContainer);

            createUI_030_Title(_shellContainer);
            createUI_050_Content(_shellContainer);
            createUI_040_Actions(_shellContainer);

         } else {

            // vertical orientation

            GridLayoutFactory.fillDefaults()
                  .spacing(0, 0)
                  .numColumns(1)

                  // set margin to draw the border
                  .extendedMargins(1, 1, 1, 1)

                  .applyTo(_shellContainer);

            createUI_020_VerticalHeader(_shellContainer);
            createUI_050_Content(_shellContainer);
         }
      }

      return _shellContainer;
   }

   private void createUI_020_VerticalHeader(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_YELLOW);
      {
         createUI_030_Title(container);
         createUI_040_Actions(container);
      }
   }

   private void createUI_030_Title(final Composite parent) {

      /*
       * Title
       */
      final Label label = new Label(parent, SWT.NONE);
      label.setText(_headerTitle);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .align(SWT.FILL, SWT.CENTER)
            .indent(2, 0)
            .applyTo(label);

      MTFont.setBannerFont(label);
//      MTFont.setHeaderFont(label);
   }

   private void createUI_040_Actions(final Composite parent) {

      /*
       * create toolbar
       */
      _toolbarControl = new ToolBar(parent, SWT.FLAT);
//      _toolbarControl.setBackground(UI.SYS_COLOR_BLUE);
      GridDataFactory.fillDefaults()
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(_toolbarControl);

      final ToolBarManager tbm = new ToolBarManager(_toolbarControl);

      tbm.add(_actionOpenTooltipMenu);
      tbm.add(_actionCloseTooltip);

      tbm.update(true);
   }

   private void createUI_050_Content(final Composite parent) {

      if (_allVisibleAndAvailable_ValueIds > 0) {

         createUI_099_AllValues(parent);

      } else {

         createUI_999_NoData(parent);
      }
   }

   private void createUI_099_AllValues(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .align(SWT.CENTER, SWT.CENTER)
            .grab(true, false)
            .applyTo(container);

      if (_isHorizontalOrientation) {

         // horizontal orientation

         GridLayoutFactory.fillDefaults()
               .numColumns(_allVisibleAndAvailable_ValueCounter)
               .spacing(5, 0)
               .extendedMargins(3, 2, 0, 0)
               .applyTo(container);
      } else {

         // vertical orientation

         GridLayoutFactory.fillDefaults()
               .numColumns(2)
               .spacing(5, 0)
               .applyTo(container);
      }

//      container.setBackground(UI.SYS_COLOR_MAGENTA);
      {
         createUI_100_TimeSlices(container);
         createUI_110_TimeDuration(container);
         createUI_112_TimeMoving(container);
         createUI_114_TimeRecorded(container);
         createUI_120_TimeOfDay(container);
         createUI_200_Distance(container);
         createUI_210_Altitude(container);
         createUI_220_Pulse(container);
         createUI_230_Speed(container);
         createUI_232_Speed_Summarized(container);
         createUI_240_Pace(container);
         createUI_242_Pace_Summarized(container);
         createUI_250_Power(container);
         createUI_260_Temperature(container);
         createUI_270_Gradient(container);
         createUI_280_Altimeter(container);
         createUI_290_Cadence(container);
         createUI_300_Gears(container);
         createUI_310_TourCompareResult(container);
         createUI_400_RunDyn_StanceTime(container);
         createUI_410_RunDyn_StanceTimeBalance(container);
         createUI_420_RunDyn_StepLength(container);
         createUI_430_RunDyn_VerticalOscillation(container);
         createUI_440_RunDyn_VerticalRatio(container);
         createUI_500_ChartZoomFactor(container);
      }
   }

   private void createUI_100_TimeSlices(final Composite parent) {

      if (_isVisible_And_Available_TimeSlice) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            // label: current value
            _lblDataSerieCurrent = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  7,
                  Messages.Tooltip_ValuePoint_Label_SlicesCurrent_Tooltip,
                  null);

            // label: max value
            _lblDataSerieMax = createUI_Label_Value(
                  valueContainer,
                  SWT.LEAD,
                  7,
                  Messages.Tooltip_ValuePoint_Label_SlicesMax_Tooltip,
                  null);
         }
      }
   }

   private void createUI_110_TimeDuration(final Composite parent) {

      if (_isVisible_And_Available_TimeDuration) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblTimeDuration = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  8,
                  OtherMessages.GRAPH_LABEL_TIME_DURATION,
                  GraphColorManager.PREF_GRAPH_TIME);

            _lblTimeDuration_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_TIME,
                  OtherMessages.GRAPH_LABEL_TIME_DURATION,
                  GraphColorManager.PREF_GRAPH_TIME);
         }
      }
   }

   private void createUI_112_TimeMoving(final Composite parent) {

      if (_isVisible_And_Available_TimeMoving) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblTimeMoving = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  8,
                  UI.EMPTY_STRING,
                  GraphColorManager.PREF_GRAPH_TIME);

            _lblTimeMoving_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_TIME,
                  UI.EMPTY_STRING,
                  GraphColorManager.PREF_GRAPH_TIME);
         }
      }
   }

   private void createUI_114_TimeRecorded(final Composite parent) {

      if (_isVisible_And_Available_TimeRecorded) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblTimeRecorded = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  8,
                  UI.EMPTY_STRING,
                  GraphColorManager.PREF_GRAPH_TIME);

            _lblTimeRecorded_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_TIME,
                  UI.EMPTY_STRING,
                  GraphColorManager.PREF_GRAPH_TIME);
         }
      }
   }

   private void createUI_120_TimeOfDay(final Composite parent) {

      if (_isVisible_And_Available_TimeOfDay) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblTimeOfDay = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  8,
                  OtherMessages.GRAPH_LABEL_TIME_OF_DAY,
                  GraphColorManager.PREF_GRAPH_TIME);

            _lblTimeOfDay_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_TIME,
                  OtherMessages.GRAPH_LABEL_TIME_OF_DAY,
                  GraphColorManager.PREF_GRAPH_TIME);
         }
      }
   }

   private void createUI_200_Distance(final Composite parent) {

      if (_isVisible_And_Available_Distance) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblDistance = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  9,
                  OtherMessages.GRAPH_LABEL_DISTANCE,
                  GraphColorManager.PREF_GRAPH_DISTANCE);

            _lblDistance_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_DISTANCE,
                  OtherMessages.GRAPH_LABEL_DISTANCE,
                  GraphColorManager.PREF_GRAPH_DISTANCE);
         }
      }
   }

   private void createUI_210_Altitude(final Composite parent) {

      if (_isVisible_And_Available_Elevation) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblElevation = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  6,
                  OtherMessages.GRAPH_LABEL_ALTITUDE,
                  GraphColorManager.PREF_GRAPH_ALTITUDE);

            _lblElevation_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_ELEVATION,
                  OtherMessages.GRAPH_LABEL_ALTITUDE,
                  GraphColorManager.PREF_GRAPH_ALTITUDE);
         }
      }
   }

   private void createUI_220_Pulse(final Composite parent) {

      if (_isVisible_And_Available_Pulse) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblPulse = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  3,
                  OtherMessages.GRAPH_LABEL_HEARTBEAT,
                  GraphColorManager.PREF_GRAPH_HEARTBEAT);

            _lblPulse_Unit = createUI_Label(
                  valueContainer,
                  OtherMessages.GRAPH_LABEL_HEARTBEAT_UNIT,
                  OtherMessages.GRAPH_LABEL_HEARTBEAT,
                  GraphColorManager.PREF_GRAPH_HEARTBEAT);
         }
      }
   }

   private void createUI_230_Speed(final Composite parent) {

      if (_isVisible_And_Available_Speed) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblSpeed = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  6,
                  OtherMessages.GRAPH_LABEL_SPEED,
                  GraphColorManager.PREF_GRAPH_SPEED);

            _lblSpeed_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_SPEED,
                  OtherMessages.GRAPH_LABEL_SPEED,
                  GraphColorManager.PREF_GRAPH_SPEED);
         }
      }
   }

   private void createUI_232_Speed_Summarized(final Composite parent) {

      if (_isVisible_And_Available_Speed_Summarized) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblSpeed_Summarized = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  6,
                  OtherMessages.GRAPH_LABEL_SPEED_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_SPEED);

            _lblSpeed_Summarized_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_SPEED + UI.SPACE + UI.SYMBOL_SUMMARIZED_AVERAGE,
                  OtherMessages.GRAPH_LABEL_SPEED_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_SPEED);
         }
      }
   }

   private void createUI_240_Pace(final Composite parent) {

      if (_isVisible_And_Available_Pace) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblPace = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  5,
                  OtherMessages.GRAPH_LABEL_PACE,
                  GraphColorManager.PREF_GRAPH_PACE);

            _lblPace_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_PACE,
                  OtherMessages.GRAPH_LABEL_PACE,
                  GraphColorManager.PREF_GRAPH_PACE);
         }
      }
   }

   private void createUI_242_Pace_Summarized(final Composite parent) {

      if (_isVisible_And_Available_Pace_Summarized) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblPace_Summarized = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  5,
                  OtherMessages.GRAPH_LABEL_PACE_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_PACE);

            _lblPace_Summarized_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_PACE + UI.SPACE + UI.SYMBOL_SUMMARIZED_AVERAGE,
                  OtherMessages.GRAPH_LABEL_PACE_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_PACE);
         }
      }
   }

   private void createUI_250_Power(final Composite parent) {

      if (_isVisible_And_Available_Power) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblPower = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  4,
                  OtherMessages.GRAPH_LABEL_POWER,
                  GraphColorManager.PREF_GRAPH_POWER);

            _lblPower_Unit = createUI_Label(
                  valueContainer,
                  OtherMessages.GRAPH_LABEL_POWER_UNIT,
                  OtherMessages.GRAPH_LABEL_POWER,
                  GraphColorManager.PREF_GRAPH_POWER);
         }
      }
   }

   private void createUI_260_Temperature(final Composite parent) {

      if (_isVisible_And_Available_Temperature) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblTemperature = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  4,
                  OtherMessages.GRAPH_LABEL_TEMPERATURE,
                  GraphColorManager.PREF_GRAPH_TEMPTERATURE);

            _lblTemperature_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_TEMPERATURE,
                  OtherMessages.GRAPH_LABEL_TEMPERATURE,
                  GraphColorManager.PREF_GRAPH_TEMPTERATURE);
         }
      }
   }

   private void createUI_270_Gradient(final Composite parent) {

      if (_isVisible_And_Available_Gradient) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblGradient = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  4,
                  OtherMessages.GRAPH_LABEL_GRADIENT,
                  GraphColorManager.PREF_GRAPH_GRADIENT);

            _lblGradient_Unit = createUI_Label(
                  valueContainer,
                  OtherMessages.GRAPH_LABEL_GRADIENT_UNIT,
                  OtherMessages.GRAPH_LABEL_GRADIENT,
                  GraphColorManager.PREF_GRAPH_GRADIENT);
         }
      }
   }

   private void createUI_280_Altimeter(final Composite parent) {

      if (_isVisible_And_Available_Altimeter) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblAltimeter = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  6,
                  OtherMessages.GRAPH_LABEL_ALTIMETER,
                  GraphColorManager.PREF_GRAPH_ALTIMETER);

            _lblAltimeter_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_ALTIMETER,
                  OtherMessages.GRAPH_LABEL_ALTIMETER,
                  GraphColorManager.PREF_GRAPH_ALTIMETER);
         }
      }
   }

   private void createUI_290_Cadence(final Composite parent) {

      if (_isVisible_And_Available_Cadence) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblCadence = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  3,
                  OtherMessages.GRAPH_LABEL_CADENCE,
                  GraphColorManager.PREF_GRAPH_CADENCE);

            _lblCadence_Unit = createUI_Label(
                  valueContainer,
                  OtherMessages.GRAPH_LABEL_CADENCE_UNIT_RPM,
                  OtherMessages.GRAPH_LABEL_CADENCE,
                  GraphColorManager.PREF_GRAPH_CADENCE);
         }
      }
   }

   private void createUI_300_Gears(final Composite parent) {

      if (_isVisible_And_Available_Gears) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblGearTeeth = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  5,
                  OtherMessages.GRAPH_LABEL_GEARS,

                  // this is a bit tricky, use default color because the text color is white
                  null);

            _lblGearRatio = createUI_Label_Value(
                  valueContainer,
                  SWT.LEAD,
                  5,
                  OtherMessages.GRAPH_LABEL_GEARS,

                  // this is a bit tricky, use default color because the text color is white
                  null);
         }

      }
   }

   private void createUI_310_TourCompareResult(final Composite parent) {

      if (_isVisible_And_Available_TourCompareResult) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblTourCompareResult = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  12,
                  OtherMessages.GRAPH_LABEL_TOUR_COMPARE,
                  GraphColorManager.PREF_GRAPH_GRADIENT);

            // no unit
            createUI_Label(
                  valueContainer,
                  UI.EMPTY_STRING,
                  OtherMessages.GRAPH_LABEL_TOUR_COMPARE,
                  GraphColorManager.PREF_GRAPH_TOUR_COMPARE);
         }

      }
   }

   private void createUI_400_RunDyn_StanceTime(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_StanceTime) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblRunDyn_StanceTime = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  3,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_STANCE_TIME,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME);

            _lblRunDyn_StanceTime_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_MS,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_STANCE_TIME,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME);
         }
      }
   }

   private void createUI_410_RunDyn_StanceTimeBalance(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_StanceTimeBalance) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblRunDyn_StanceTimeBalance = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  3,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCE,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED);

            _lblRunDyn_StanceTimeBalance_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_PERCENT,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCE,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
         }
      }
   }

   private void createUI_420_RunDyn_StepLength(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_StepLength) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblRunDyn_StepLength = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  3,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_STEP_LENGTH,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STEP_LENGTH);

            _lblRunDyn_StepLength_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_DISTANCE_MM_OR_INCH,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_STEP_LENGTH,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STEP_LENGTH);
         }
      }
   }

   private void createUI_430_RunDyn_VerticalOscillation(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_VerticalOscillation) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblRunDyn_VerticalOscillation = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  3,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION);

            _lblRunDyn_VerticalOscillation_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_LABEL_DISTANCE_MM_OR_INCH,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
         }
      }
   }

   private void createUI_440_RunDyn_VerticalRatio(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_VerticalRatio) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {
            _lblRunDyn_VerticalRatio = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  3,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_RATIO);

            _lblRunDyn_VerticalRatio_Unit = createUI_Label(
                  valueContainer,
                  UI.UNIT_PERCENT,
                  OtherMessages.GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_RATIO);
         }
      }
   }

   private void createUI_500_ChartZoomFactor(final Composite parent) {

      if (_isVisible_And_Available_ChartZoomFactor) {

         final Composite valueContainer = _isHorizontalOrientation ? createUI_ValueContainer(parent) : parent;
         {

            _lblChartZoomFactor = createUI_Label_Value(
                  valueContainer,
                  SWT.TRAIL,
                  8,
                  Messages.Tooltip_ValuePoint_Label_ChartZoomFactor_Tooltip,
                  null);

            // spacer
            new Label(valueContainer, SWT.NONE);
         }
      }
   }

   private Composite createUI_999_NoData(final Composite parent) {

      /*
       * A shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {

         final Composite container = new Composite(shellContainer, SWT.NONE);
         GridLayoutFactory
               .fillDefaults()
               .extendedMargins(5, 5, 0, 0)
               .applyTo(container);
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Tooltip_ValuePoint_Label_NoData);
            label.setToolTipText(Messages.Tooltip_ValuePoint_Label_NoData_Tooltip);
         }
      }

      return shellContainer;
   }

   /**
    * @param parent
    * @param labelText
    * @param tooltip
    * @param colorId
    * @return Returns created label.
    */
   private Label createUI_Label(final Composite parent,
                                final String labelText,
                                final String tooltip,
                                final String colorId) {

      final Label label = new Label(parent, SWT.NONE);

      if (labelText != null) {
         label.setText(labelText);
      }

      if (tooltip != null) {
         label.setToolTipText(tooltip);
      }

      updateUI_Color(label, colorId);

//      label.setBackground(UI.SYS_COLOR_GREEN);

      return label;
   }

   /**
    * @param parent
    * @param style
    * @param chars
    *           Hint for the width in characters.
    * @param tooltip
    * @param colorId
    *           Can be <code>null</code>.
    * @return
    */
   private Label createUI_Label_Value(final Composite parent,
                                      final int style,
                                      final int chars,
                                      final String tooltip,
                                      final String colorId) {

      final int charsWidth;

      if (chars == SWT.DEFAULT) {

         charsWidth = SWT.DEFAULT;

      } else {

         final StringBuilder sb = new StringBuilder();
         sb.append('.');
         for (int charIndex = 0; charIndex < chars; charIndex++) {
            sb.append('8');
         }

         final GC gc = new GC(parent);
         charsWidth = gc.textExtent(sb.toString()).x;
         gc.dispose();
      }

      final Label label = new Label(parent, style);
      GridDataFactory.fillDefaults()
            .hint(charsWidth, SWT.DEFAULT)
            .applyTo(label);

      if (tooltip != null) {
         label.setToolTipText(tooltip);
      }

      updateUI_Color(label, colorId);

//      label.setBackground(UI.SYS_COLOR_GREEN);

      return label;
   }

   private Composite createUI_ValueContainer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);

      GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(container);

      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .spacing(_valueUnitDistance, 0)
            .applyTo(container);

//      container.setBackground(UI.SYS_COLOR_RED);

      return container;
   }

   private String getCadenceUnit(final int valueIndex) {

      float cadenceMultiplier;

      if (_tourData.isMultipleTours()) {

         final int[] multipleTourStartIndex = _tourData.multipleTourStartIndex;
         final int numTours = multipleTourStartIndex.length;

         int tourIndex = numTours - 1;

         // loop backwards, this is less complex
         for (; tourIndex >= 0; tourIndex--) {

            final int tourStart_Index = multipleTourStartIndex[tourIndex];

            if (valueIndex >= tourStart_Index) {
               break;
            }
         }

         cadenceMultiplier = _tourData.multipleTours_CadenceMultiplier[tourIndex];

      } else {

         cadenceMultiplier = _tourData.getCadenceMultiplier();
      }

      final String cadenceUnit = cadenceMultiplier == 2.0
            ? OtherMessages.GRAPH_LABEL_CADENCE_UNIT_SPM
            : OtherMessages.GRAPH_LABEL_CADENCE_UNIT_RPM;

      return cadenceUnit;
   }

   private long getState(final long visibleValues, final long valueId) {
      return (visibleValues & valueId) > 0 ? valueId : 0;
   }

   @Override
   public Shell getToolTipShell() {
      return super.getToolTipShell();
   }

   @Override
   protected boolean isHideTooltipWhenOwnerShellIsInactive() {
      return false;
   }

   public boolean isVisible() {
      return _isToolTipVisible;
   }

   @Override
   protected void onDispose() {

      _prefStore.removePropertyChangeListener(_prefChangeListener);

      _colorCache.dispose();

      if (_ttMenuMgr != null) {
         _ttMenuMgr.dispose();
      }

      super.onDispose();
   }

   @Override
   protected void onEvent_ContextMenu(final Event event) {

      _ttMenuMgr.openToolTipMenu(event, _tourData, _allVisibleValueIds, _isHorizontalOrientation);
   }

   private void onPaintShellContainer(final PaintEvent event) {

      final GC gc = event.gc;
      final Point shellSize = _shellContainer.getSize();

      // draw border
      gc.setForeground(_fgBorder);
      gc.drawRectangle(0, 0, shellSize.x - 1, shellSize.y - 1);

// this is not working correctly because a new paint needs to be done
//      gc.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_BLACK));
//      switch (pinnedLocation) {
//      case TopLeft:
//         gc.drawPoint(0, 0);
//         break;
//      case TopRight:
//         gc.drawPoint(shellSize.x - 1, 0);
//         break;
//      case BottomLeft:
//         gc.drawPoint(0, shellSize.y - 1);
//         break;
//      case BottomRight:
//         gc.drawPoint(shellSize.x - 1, shellSize.y - 1);
//         break;
//      }
   }

   /**
    * Reopens the tooltip at the current position, this will not show the tooltip when it is set to
    * be hidden.
    */
   public void reopen() {

      // hide and recreate it
      hide();
      show(new Point(_devXMouse, _devXMouse));
   }

   @Override
   public void setHoveredData(final int devXMouseMove, final int devYMouseMove, final Object hoveredData) {

      if (_tourData == null || _isToolTipVisible == false) {
         return;
      }

      if (hoveredData instanceof HoveredValuePointData) {

         final HoveredValuePointData hoveredValuePointData = (HoveredValuePointData) hoveredData;

         _devXMouse = devXMouseMove;
         _devYMouse = devYMouseMove;

         _chartZoomFactor = hoveredValuePointData.graphZoomFactor;

         if (_shellContainer == null || _shellContainer.isDisposed()) {

            /*
             * Tool tip is disposed, this happens on a mouse exit, display the tooltip again
             */
            show(new Point(devXMouseMove, devYMouseMove));
         }

         // check again
         if (_shellContainer != null && !_shellContainer.isDisposed()) {

            setTTShellLocation(devXMouseMove, devYMouseMove, hoveredValuePointData.valueDevPosition);

            updateUI(hoveredValuePointData.valueIndex);
         }
      }
   }

   @Override
   public void setSnapBorder(final int marginTop, final int marginBottom) {

      this.snapBorder_Top = marginTop;
      this.snapBorder_Bottom = marginBottom;
   }

   /**
    * @param tourData
    *           When <code>null</code> the tooltip will be hidden.
    */
   public void setTourData(final TourData tourData) {

      if (_tourData != null && tourData != null
            && _tourData.getTourId().equals(tourData.getTourId())) {

         // the same tour is already set

         return;
      }

      _tourData = tourData;
      _currentValueIndex = -1;

      if (tourData == null) {

         hide();

         return;
      }

      /*
       * hide tool tip context menu because new tour data can change the available graphs which can
       * be selected in the context menu
       */
      if (_ttMenuMgr != null) {
         _ttMenuMgr.hideContextMenu();
      }

      final long visibleValuesBackup = _allVisibleAndAvailable_ValueIds;

      updateStateVisibleValues(_allVisibleValueIds);

      // prevent flickering when reopen
      if (visibleValuesBackup != _allVisibleAndAvailable_ValueIds) {

         // reopen when other tour data are set which has other graphs
         reopen();
      }

      updateUI(0);
   }

   @Override
   protected boolean shouldCreateToolTip(final Event event) {

      if (_tourData == null) {
         return false;
      }

      return super.shouldCreateToolTip(event);
   }

   @Override
   public void show(final Point location) {

      if (_isToolTipVisible) {
         super.show(location);
      }
   }

   /**
    * Sets state which graphs can be displayed.
    *
    * @param ttVisibleValues
    */
   private void updateStateVisibleValues(final long ttVisibleValues) {

// SET_FORMATTING_OFF

      final long visibleId_Altimeter                  = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_ALTIMETER);
      final long visibleId_Altitude                   = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_ALTITUDE);
      final long visibleId_Cadence                    = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_CADENCE);
      final long visibleId_ChartZoomFactor            = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_CHART_ZOOM_FACTOR);
      final long visibleId_Distance                   = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_DISTANCE);
      final long visibleId_Gears                      = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_GEARS);
      final long visibleId_Gradient                   = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_GRADIENT);
      final long visibleId_Pace                       = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_PACE);
      final long visibleId_Pace_Summarized            = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_PACE_SUMMARIZED);
      final long visibleId_Power                      = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_POWER);
      final long visibleId_Pulse                      = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_PULSE);
      final long visibleId_RunDyn_StanceTime          = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_RUN_DYN_STANCE_TIME);
      final long visibleId_RunDyn_StanceTimeBalance   = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_RUN_DYN_STANCE_TIME_BALANCED);
      final long visibleId_RunDyn_StepLength          = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_RUN_DYN_STEP_LENGTH);
      final long visibleId_RunDyn_VerticalOscillation = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_RUN_DYN_VERTICAL_OSCILLATION);
      final long visibleId_RunDyn_VerticalRatio       = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_RUN_DYN_VERTICAL_RATIO);
      final long visibleId_Speed                      = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_SPEED);
      final long visibleId_Speed_Summarized           = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_SPEED_SUMMARIZED);
      final long visibleId_Temperature                = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TEMPERATURE);
      final long visibleId_TimeDuration               = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TIME_DURATION);
      final long visibleId_TimeOfDay                  = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TIME_OF_DAY);
      final long visibleId_TimeMoving                 = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TIME_MOVING);
      final long visibleId_TimeRecorded               = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TIME_RECORDED);
      final long visibleId_TimeSlice                  = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TIME_SLICES);
      final long visibleId_TourCompareResult          = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TOUR_COMPARE_RESULT);

      _isAvailable_Pulse_BpmFromDevice                = _tourData.pulseSerie                          != null;
      _isAvailable_Pulse_RRIntervals                  = _tourData.getPulse_RRIntervals()              != null;

      final boolean isAvailable_Altimeter             = _tourData.getAltimeterSerie()                 != null;
      final boolean isAvailable_Altitude              = _tourData.getAltitudeSerie()                  != null;
      final boolean isAvailable_Cadence               = _tourData.getCadenceSerie()                   != null;
      final boolean isAvailable_ChartZoomFactor       = _canBeDisplayed_ChartZoomFactor;
      final boolean isAvailable_Distance              = _tourData.distanceSerie                       != null;
      final boolean isAvailable_Gears                 = _tourData.getGears()                          != null;
      final boolean isAvailable_Gradient              = _tourData.getGradientSerie()                  != null;
      final boolean isAvailable_Pace                  = _tourData.getPaceSerie()                      != null;
      final boolean isAvailable_Pace_Summarized       = _tourData.getPaceSerie_Summarized_Seconds()   != null;
      final boolean isAvailable_Power                 = _tourData.getPowerSerie()                     != null;
      final boolean isAvailable_Pulse                 = _isAvailable_Pulse_BpmFromDevice ||_isAvailable_Pulse_RRIntervals;
      final boolean isAvailable_Speed                 = _tourData.getSpeedSerie()                     != null;
      final boolean isAvailable_Speed_Summarized      = _tourData.getSpeedSerie_Summarized()          != null;
      final boolean isAvailable_Temperature           = _tourData.temperatureSerie                    != null;
      final boolean isAvailable_TimeDuration          = _tourData.timeSerie                           != null;
      final boolean isAvailable_TimeOfDay             = _tourData.timeSerie                           != null;
      final boolean isAvailable_TimeMoving            = _tourData.getMovingTimeSerie()                != null;
      final boolean isAvailable_TimeRecorded          = _tourData.getRecordedTimeSerie()              != null;
      final boolean isAvailable_TimeSlice             = true;
      final boolean isAvailable_TourCompareResult     = _tourData.tourCompareSerie != null && _tourData.tourCompareSerie.length > 0;

      final boolean isAvailable_RunDyn_StanceTime           = _tourData.getRunDyn_StanceTime()           != null;
      final boolean isAvailable_RunDyn_StanceTimeBalance    = _tourData.getRunDyn_StanceTimeBalance()    != null;
      final boolean isAvailable_RunDyn_StepLength           = _tourData.getRunDyn_StepLength()           != null;
      final boolean isAvailable_RunDyn_VerticalOscillation  = _tourData.getRunDyn_VerticalOscillation()  != null;
      final boolean isAvailable_RunDyn_VerticalRatio        = _tourData.getRunDyn_VerticalRatio()        != null;

      _allVisibleValueIds =

              visibleId_Altimeter
            + visibleId_Altitude
            + visibleId_Cadence
            + visibleId_ChartZoomFactor
            + visibleId_Distance
            + visibleId_Gears
            + visibleId_Gradient
            + visibleId_Pace
            + visibleId_Pace_Summarized
            + visibleId_Power
            + visibleId_Pulse
            + visibleId_Speed
            + visibleId_Speed_Summarized
            + visibleId_Temperature
            + visibleId_TimeDuration
            + visibleId_TimeOfDay
            + visibleId_TimeMoving
            + visibleId_TimeRecorded
            + visibleId_TimeSlice
            + visibleId_TourCompareResult
            + visibleId_RunDyn_StanceTime
            + visibleId_RunDyn_StanceTimeBalance
            + visibleId_RunDyn_StepLength
            + visibleId_RunDyn_VerticalOscillation
            + visibleId_RunDyn_VerticalRatio

            ;

      _allVisibleAndAvailable_ValueIds =

              (isAvailable_Altimeter         ? visibleId_Altimeter         : 0)
            + (isAvailable_Altitude          ? visibleId_Altitude          : 0)
            + (isAvailable_Cadence           ? visibleId_Cadence           : 0)
            + (isAvailable_ChartZoomFactor   ? visibleId_ChartZoomFactor   : 0)
            + (isAvailable_Distance          ? visibleId_Distance          : 0)
            + (isAvailable_Gears             ? visibleId_Gears             : 0)
            + (isAvailable_Gradient          ? visibleId_Gradient          : 0)
            + (isAvailable_Pace              ? visibleId_Pace              : 0)
            + (isAvailable_Pace_Summarized   ? visibleId_Pace_Summarized   : 0)
            + (isAvailable_Power             ? visibleId_Power             : 0)
            + (isAvailable_Pulse             ? visibleId_Pulse             : 0)
            + (isAvailable_Speed             ? visibleId_Speed             : 0)
            + (isAvailable_Speed_Summarized  ? visibleId_Speed_Summarized  : 0)
            + (isAvailable_Temperature       ? visibleId_Temperature       : 0)
            + (isAvailable_TimeDuration      ? visibleId_TimeDuration      : 0)
            + (isAvailable_TimeOfDay         ? visibleId_TimeOfDay         : 0)
            + (isAvailable_TimeMoving        ? visibleId_TimeMoving        : 0)
            + (isAvailable_TimeRecorded      ? visibleId_TimeRecorded      : 0)
            + (isAvailable_TimeSlice         ? visibleId_TimeSlice         : 0)
            + (isAvailable_TourCompareResult ? visibleId_TourCompareResult : 0)

            + (isAvailable_RunDyn_StanceTime          ? visibleId_RunDyn_StanceTime          : 0)
            + (isAvailable_RunDyn_StanceTimeBalance   ? visibleId_RunDyn_StanceTimeBalance   : 0)
            + (isAvailable_RunDyn_StepLength          ? visibleId_RunDyn_StepLength          : 0)
            + (isAvailable_RunDyn_VerticalOscillation ? visibleId_RunDyn_VerticalOscillation : 0)
            + (isAvailable_RunDyn_VerticalRatio       ? visibleId_RunDyn_VerticalRatio       : 0)

            ;

      _isVisible_And_Available_Altimeter           = isAvailable_Altimeter          && visibleId_Altimeter > 0;
      _isVisible_And_Available_Elevation           = isAvailable_Altitude           && visibleId_Altitude > 0;
      _isVisible_And_Available_Cadence             = isAvailable_Cadence            && visibleId_Cadence > 0;
      _isVisible_And_Available_ChartZoomFactor     = isAvailable_ChartZoomFactor    && visibleId_ChartZoomFactor > 0;
      _isVisible_And_Available_Distance            = isAvailable_Distance           && visibleId_Distance > 0;
      _isVisible_And_Available_Gears               = isAvailable_Gears              && visibleId_Gears > 0;
      _isVisible_And_Available_Gradient            = isAvailable_Gradient           && visibleId_Gradient > 0;
      _isVisible_And_Available_Pace                = isAvailable_Pace               && visibleId_Pace > 0;
      _isVisible_And_Available_Pace_Summarized     = isAvailable_Pace_Summarized    && visibleId_Pace_Summarized > 0;
      _isVisible_And_Available_Power               = isAvailable_Power              && visibleId_Power > 0;
      _isVisible_And_Available_Pulse               = isAvailable_Pulse              && visibleId_Pulse > 0;
      _isVisible_And_Available_Speed               = isAvailable_Speed              && visibleId_Speed > 0;
      _isVisible_And_Available_Speed_Summarized    = isAvailable_Speed_Summarized   && visibleId_Speed_Summarized > 0;
      _isVisible_And_Available_Temperature         = isAvailable_Temperature        && visibleId_Temperature > 0;
      _isVisible_And_Available_TimeDuration        = isAvailable_TimeDuration       && visibleId_TimeDuration > 0;
      _isVisible_And_Available_TimeOfDay           = isAvailable_TimeOfDay          && visibleId_TimeOfDay > 0;
      _isVisible_And_Available_TimeMoving          = isAvailable_TimeMoving         && visibleId_TimeMoving > 0;
      _isVisible_And_Available_TimeRecorded        = isAvailable_TimeRecorded       && visibleId_TimeRecorded > 0;
      _isVisible_And_Available_TimeSlice           = isAvailable_TimeSlice          && visibleId_TimeSlice > 0;
      _isVisible_And_Available_TourCompareResult   = isAvailable_TourCompareResult  && visibleId_TourCompareResult > 0;

      _isVisible_And_Available_RunDyn_StanceTime            = isAvailable_RunDyn_StanceTime           && visibleId_RunDyn_StanceTime          > 0;
      _isVisible_And_Available_RunDyn_StanceTimeBalance     = isAvailable_RunDyn_StanceTimeBalance    && visibleId_RunDyn_StanceTimeBalance > 0;
      _isVisible_And_Available_RunDyn_StepLength            = isAvailable_RunDyn_StepLength           && visibleId_RunDyn_StepLength          > 0;
      _isVisible_And_Available_RunDyn_VerticalOscillation   = isAvailable_RunDyn_VerticalOscillation  && visibleId_RunDyn_VerticalOscillation > 0;
      _isVisible_And_Available_RunDyn_VerticalRatio         = isAvailable_RunDyn_VerticalRatio        && visibleId_RunDyn_VerticalRatio       > 0;

      _allVisibleAndAvailable_ValueCounter =

              (_isVisible_And_Available_Altimeter                    ? 1 : 0)
            + (_isVisible_And_Available_Elevation                    ? 1 : 0)
            + (_isVisible_And_Available_Cadence                      ? 1 : 0)
            + (_isVisible_And_Available_ChartZoomFactor              ? 1 : 0)
            + (_isVisible_And_Available_Distance                     ? 1 : 0)
            + (_isVisible_And_Available_Gears                        ? 1 : 0)
            + (_isVisible_And_Available_Gradient                     ? 1 : 0)
            + (_isVisible_And_Available_Pace                         ? 1 : 0)
            + (_isVisible_And_Available_Pace_Summarized              ? 1 : 0)
            + (_isVisible_And_Available_Power                        ? 1 : 0)
            + (_isVisible_And_Available_Pulse                        ? 1 : 0)
            + (_isVisible_And_Available_Speed                        ? 1 : 0)
            + (_isVisible_And_Available_Speed_Summarized             ? 1 : 0)
            + (_isVisible_And_Available_Temperature                  ? 1 : 0)
            + (_isVisible_And_Available_TimeDuration                 ? 1 : 0)
            + (_isVisible_And_Available_TimeOfDay                    ? 1 : 0)
            + (_isVisible_And_Available_TimeMoving                   ? 1 : 0)
            + (_isVisible_And_Available_TimeRecorded                 ? 1 : 0)
            + (_isVisible_And_Available_TimeSlice                    ? 1 : 0)
            + (_isVisible_And_Available_TourCompareResult            ? 1 : 0)

            + (_isVisible_And_Available_RunDyn_StanceTime            ? 1 : 0)
            + (_isVisible_And_Available_RunDyn_StanceTimeBalance     ? 1 : 0)
            + (_isVisible_And_Available_RunDyn_StepLength            ? 1 : 0)
            + (_isVisible_And_Available_RunDyn_VerticalOscillation   ? 1 : 0)
            + (_isVisible_And_Available_RunDyn_VerticalRatio         ? 1 : 0)
      ;

// SET_FORMATTING_ON
   }

   private void updateUI(final int valueIndex) {

      // get time when the redraw is requested
      final long requestedRedrawTime = System.currentTimeMillis();

      if (requestedRedrawTime > _lastUpdateUITime + 300) {

         // force a redraw

         updateUI_Runnable(valueIndex);

      } else {

         _updateCounter[0]++;

         if (_shellContainer.isDisposed()) {
            return;
         }

         _shellContainer.getDisplay().asyncExec(new Runnable() {

            final int __runnableCounter = _updateCounter[0];

            @Override
            public void run() {

               // update UI delayed
               if (__runnableCounter != _updateCounter[0]) {

                  // a new update UI occured

                  return;
               }

               updateUI_Runnable(valueIndex);
            }
         });
      }

   }

   private void updateUI_Color(final Label label, final String colorId) {

      if (colorId == null) {

         label.setForeground(ThemeUtil.getDefaultForegroundColor_Shell());

      } else {

         final Color fgColor = _colorCache.getColor(
               colorId,
               _colorManager.getGraphColorDefinition(colorId).getTextColor_Active_Themed());

         label.setForeground(fgColor);
      }
   }

   private void updateUI_Runnable(int valueIndex) {

      if (_shellContainer == null || _shellContainer.isDisposed()) {
         return;
      }

      final int[] timeSerie = _tourData.timeSerie;

      if (timeSerie == null) {

         // this happened with .fitlog import files

         return;
      }

      if (valueIndex == _currentValueIndex) {

         // UI is updated with the current value index -> nothing more to do

         return;
      }

      // check bounds
      if (valueIndex < 0 || valueIndex >= timeSerie.length) {
         valueIndex = timeSerie.length - 1;
      }

      _currentValueIndex = valueIndex;

      final int durationTime = timeSerie[valueIndex];

      if (_isVisible_And_Available_Altimeter) {
         _lblAltimeter.setText(Integer.toString((int) _tourData.getAltimeterSerie()[valueIndex]));
      }

      if (_isVisible_And_Available_Elevation) {
         _lblElevation.setText(_nf1NoGroup.format(_tourData.getAltitudeSmoothedSerie(false)[valueIndex]));
      }

      if (_isVisible_And_Available_Cadence) {

         _lblCadence.setText(Integer.toString((int) _tourData.getCadenceSerieWithMuliplier()[valueIndex]));
         _lblCadence_Unit.setText(getCadenceUnit(valueIndex));
      }

      if (_isVisible_And_Available_ChartZoomFactor) {
         _lblChartZoomFactor.setText(_nf1.format(_chartZoomFactor));
      }

      if (_isVisible_And_Available_Distance) {

         final float distance = _tourData.distanceSerie[valueIndex] / 1000 / UI.UNIT_VALUE_DISTANCE;

         _lblDistance.setText(_nf3NoGroup.format(distance));
      }

      if (_isVisible_And_Available_Gears) {

         final float[][] gears = _tourData.getGears();

//       _gears[0] = gear ratio
//       _gears[1] = front gear teeth
//       _gears[2] = rear gear teeth
//       _gears[3] = front gear number, starting with 1
//       _gears[4] = rear gear number, starting with 1

         final String gearRatioText = String.format(TourManager.GEAR_RATIO_FORMAT, gears[0][valueIndex]);

         _lblGearTeeth.setText(String.format(
               TourManager.GEAR_TEETH_FORMAT,
               (int) gears[1][valueIndex],
               (int) gears[2][valueIndex]));

         _lblGearRatio.setText(_isHorizontalOrientation

               // it needs more horizontal space
               ? UI.SPACE1 + gearRatioText

               : gearRatioText);
      }

      if (_isVisible_And_Available_Gradient) {
         _lblGradient.setText(_nf1.format(_tourData.getGradientSerie()[valueIndex]));
      }

      if (_isVisible_And_Available_Pace) {

         final long pace = (long) _tourData.getPaceSerieSeconds()[valueIndex];

         _lblPace.setText(String.format(Messages.Tooltip_ValuePoint_Format_Pace,
               pace / 60,
               pace % 60));
      }

      if (_isVisible_And_Available_Pace_Summarized) {

         final long pace = (long) _tourData.getPaceSerie_Summarized_Seconds()[valueIndex];

         _lblPace_Summarized.setText(String.format(Messages.Tooltip_ValuePoint_Format_Pace,
               pace / 60,
               pace % 60));
      }

      if (_isVisible_And_Available_Power) {
         _lblPower.setText(Integer.toString((int) _tourData.getPowerSerie()[valueIndex]));
      }

      if (_isVisible_And_Available_Pulse) {

         final PulseGraph pulseGraph = (PulseGraph) Util.getEnumValue(
               _prefStore.getString(ITourbookPreferences.GRAPH_PULSE_GRAPH_VALUES),
               TourChart.PULSE_GRAPH_DEFAULT);

         // @FJBDev: Do NOT optimize the following if statements, this way it is better readable for me !

         if (_isAvailable_Pulse_BpmFromDevice) {

            if (pulseGraph == PulseGraph.DEVICE_BPM_ONLY || pulseGraph == PulseGraph.DEVICE_BPM___2ND_RR_AVERAGE) {

               _lblPulse.setText(Integer.toString((int) _tourData.pulseSerie[valueIndex]));
            }

         }

         if (_isAvailable_Pulse_RRIntervals) {

            if (pulseGraph == PulseGraph.RR_INTERVALS_ONLY
                  || pulseGraph == PulseGraph.RR_AVERAGE___2ND_DEVICE_BPM
                  || pulseGraph == PulseGraph.RR_AVERAGE_ONLY) {

               _lblPulse.setText(_nf1.format(_tourData.getPulse_AvgBpmFromRRIntervals()[valueIndex]));
            }
         }
      }

      if (_isVisible_And_Available_Speed) {

         _lblSpeed.setText(_nf2.format(_tourData.getSpeedSerie()[valueIndex]));
      }
      if (_isVisible_And_Available_Speed_Summarized) {

         _lblSpeed_Summarized.setText(_nf2.format(_tourData.getSpeedSerie_Summarized()[valueIndex]));
      }

      if (_isVisible_And_Available_Temperature) {

         final float temperature = UI.convertTemperatureFromMetric(_tourData.temperatureSerie[valueIndex]);

         _lblTemperature.setText(_nf1.format(temperature));
      }

      if (_isVisible_And_Available_TimeDuration) {
         _lblTimeDuration.setText(UI.format_hhh_mm_ss(durationTime));
      }

      if (_isVisible_And_Available_TimeMoving) {

         final int movingTime = _tourData.getMovingTimeSerie()[valueIndex];
         final int breakTime = durationTime - movingTime;

         final String tooltip = String.format(
               Messages.Tooltip_ValuePoint_Label_MovingTime_Tooltip,
               UI.format_hhh_mm_ss(breakTime));

         _lblTimeMoving.setText(UI.format_hhh_mm_ss(movingTime));
         _lblTimeMoving.setToolTipText(tooltip);

         _lblTimeMoving_Unit.setToolTipText(tooltip);
      }

      if (_isVisible_And_Available_TimeRecorded) {

         final int recordedTime = _tourData.getRecordedTimeSerie()[valueIndex];
         final int pauseTime = durationTime - recordedTime;

         final String tooltip = String.format(
               Messages.Tooltip_ValuePoint_Label_RecordedTime_Tooltip,
               UI.format_hhh_mm_ss(pauseTime));

         _lblTimeRecorded.setText(UI.format_hhh_mm_ss(recordedTime));
         _lblTimeRecorded.setToolTipText(tooltip);

         _lblTimeRecorded_Unit.setToolTipText(tooltip);
      }

      if (_isVisible_And_Available_TimeOfDay) {
         _lblTimeOfDay.setText(UI.format_hhh_mm_ss((_tourData.getStartTimeOfDay() + durationTime) % 86400));
      }

      if (_isVisible_And_Available_TimeSlice) {

         /*
          * Show the same time slice value which is also selected in the tour editor where time
          * slices are starting with 1, otherwise it could be confusing
          */

         final String maxSlices = (_isHorizontalOrientation

               ? UI.SPACE + UI.SYMBOL_COLON + UI.SPACE1
               : UI.EMPTY_STRING)

               + Integer.toString(timeSerie.length);

         _lblDataSerieCurrent.setText(Integer.toString(_currentValueIndex + 1));
         _lblDataSerieMax.setText(maxSlices);
      }

      if (_isVisible_And_Available_TourCompareResult) {
         _lblTourCompareResult.setText(_nf0.format(_tourData.tourCompareSerie[valueIndex]));
      }

      if (_isVisible_And_Available_RunDyn_StanceTime) {
         _lblRunDyn_StanceTime.setText(_nf0.format(_tourData.getRunDyn_StanceTime()[valueIndex]));
      }
      if (_isVisible_And_Available_RunDyn_StanceTimeBalance) {
         _lblRunDyn_StanceTimeBalance.setText(_nf2.format(_tourData.getRunDyn_StanceTimeBalance()[valueIndex]));
      }
      if (_isVisible_And_Available_RunDyn_StepLength) {
         _lblRunDyn_StepLength.setText(_nf0.format(_tourData.getRunDyn_StepLength()[valueIndex]));
      }
      if (_isVisible_And_Available_RunDyn_VerticalOscillation) {
         _lblRunDyn_VerticalOscillation.setText(_nf1.format(_tourData.getRunDyn_VerticalOscillation()[valueIndex]));
      }
      if (_isVisible_And_Available_RunDyn_VerticalRatio) {
         _lblRunDyn_VerticalRatio.setText(_nf2.format(_tourData.getRunDyn_VerticalRatio()[valueIndex]));
      }

      _lastUpdateUITime = System.currentTimeMillis();

      // update color
      _shellContainer.getDisplay().asyncExec(() -> {

         if (_shellContainer == null || _shellContainer.isDisposed()) {
            return;
         }

         updateUI_Runnable_Colors();
      });
   }

   private void updateUI_Runnable_Colors() {

      UI.setColorForAllChildren(_shellContainer,
            ThemeUtil.getDefaultForegroundColor_Table(),
            ThemeUtil.getDefaultBackgroundColor_Table());

      if (_isVisible_And_Available_Altimeter) {
         updateUI_Color(_lblAltimeter, GraphColorManager.PREF_GRAPH_ALTIMETER);
         updateUI_Color(_lblAltimeter_Unit, GraphColorManager.PREF_GRAPH_ALTIMETER);
      }

      if (_isVisible_And_Available_Cadence) {
         updateUI_Color(_lblCadence, GraphColorManager.PREF_GRAPH_CADENCE);
         updateUI_Color(_lblCadence_Unit, GraphColorManager.PREF_GRAPH_CADENCE);
      }

      if (_isVisible_And_Available_ChartZoomFactor) {
         updateUI_Color(_lblChartZoomFactor, null);
      }

      if (_isVisible_And_Available_Distance) {
         updateUI_Color(_lblDistance, GraphColorManager.PREF_GRAPH_DISTANCE);
         updateUI_Color(_lblDistance_Unit, GraphColorManager.PREF_GRAPH_DISTANCE);
      }

      if (_isVisible_And_Available_Elevation) {
         updateUI_Color(_lblElevation, GraphColorManager.PREF_GRAPH_ALTITUDE);
         updateUI_Color(_lblElevation_Unit, GraphColorManager.PREF_GRAPH_ALTITUDE);
      }

      if (_isVisible_And_Available_Gears) {
         updateUI_Color(_lblGearTeeth, null);
         updateUI_Color(_lblGearRatio, null);
      }

      if (_isVisible_And_Available_Gradient) {
         updateUI_Color(_lblGradient, GraphColorManager.PREF_GRAPH_GRADIENT);
         updateUI_Color(_lblGradient_Unit, GraphColorManager.PREF_GRAPH_GRADIENT);
      }

      if (_isVisible_And_Available_Pace) {
         updateUI_Color(_lblPace, GraphColorManager.PREF_GRAPH_PACE);
         updateUI_Color(_lblPace_Unit, GraphColorManager.PREF_GRAPH_PACE);
      }

      if (_isVisible_And_Available_Pace_Summarized) {
         updateUI_Color(_lblPace_Summarized, GraphColorManager.PREF_GRAPH_PACE);
         updateUI_Color(_lblPace_Summarized_Unit, GraphColorManager.PREF_GRAPH_PACE);
      }

      if (_isVisible_And_Available_Power) {
         updateUI_Color(_lblPower, GraphColorManager.PREF_GRAPH_POWER);
         updateUI_Color(_lblPower_Unit, GraphColorManager.PREF_GRAPH_POWER);
      }

      if (_isVisible_And_Available_Pulse) {
         updateUI_Color(_lblPulse, GraphColorManager.PREF_GRAPH_HEARTBEAT);
         updateUI_Color(_lblPulse_Unit, GraphColorManager.PREF_GRAPH_HEARTBEAT);
      }

      if (_isVisible_And_Available_Speed) {
         updateUI_Color(_lblSpeed, GraphColorManager.PREF_GRAPH_SPEED);
         updateUI_Color(_lblSpeed_Unit, GraphColorManager.PREF_GRAPH_SPEED);
      }

      if (_isVisible_And_Available_Speed_Summarized) {
         updateUI_Color(_lblSpeed_Summarized, GraphColorManager.PREF_GRAPH_SPEED);
         updateUI_Color(_lblSpeed_Summarized_Unit, GraphColorManager.PREF_GRAPH_SPEED);
      }

      if (_isVisible_And_Available_Temperature) {
         updateUI_Color(_lblTemperature, GraphColorManager.PREF_GRAPH_TEMPTERATURE);
         updateUI_Color(_lblTemperature_Unit, GraphColorManager.PREF_GRAPH_TEMPTERATURE);
      }

      if (_isVisible_And_Available_TimeDuration) {
         updateUI_Color(_lblTimeDuration, GraphColorManager.PREF_GRAPH_TIME);
         updateUI_Color(_lblTimeDuration_Unit, GraphColorManager.PREF_GRAPH_TIME);
      }

      if (_isVisible_And_Available_TimeMoving) {
         updateUI_Color(_lblTimeMoving, GraphColorManager.PREF_GRAPH_TIME);
         updateUI_Color(_lblTimeMoving_Unit, GraphColorManager.PREF_GRAPH_TIME);
      }

      if (_isVisible_And_Available_TimeRecorded) {
         updateUI_Color(_lblTimeRecorded, GraphColorManager.PREF_GRAPH_TIME);
         updateUI_Color(_lblTimeRecorded_Unit, GraphColorManager.PREF_GRAPH_TIME);
      }

      if (_isVisible_And_Available_TimeOfDay) {
         updateUI_Color(_lblTimeOfDay, GraphColorManager.PREF_GRAPH_TIME);
         updateUI_Color(_lblTimeOfDay_Unit, GraphColorManager.PREF_GRAPH_TIME);
      }

      if (_isVisible_And_Available_TimeSlice) {
         updateUI_Color(_lblDataSerieCurrent, null);
         updateUI_Color(_lblDataSerieMax, null);
      }

      if (_isVisible_And_Available_TourCompareResult) {
         updateUI_Color(_lblTourCompareResult, GraphColorManager.PREF_GRAPH_TOUR_COMPARE);
      }

      if (_isVisible_And_Available_RunDyn_StanceTime) {
         updateUI_Color(_lblRunDyn_StanceTime, GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME);
         updateUI_Color(_lblRunDyn_StanceTime_Unit, GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME);
      }
      if (_isVisible_And_Available_RunDyn_StanceTimeBalance) {
         updateUI_Color(_lblRunDyn_StanceTimeBalance, GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
         updateUI_Color(_lblRunDyn_StanceTimeBalance_Unit, GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
      }
      if (_isVisible_And_Available_RunDyn_StepLength) {
         updateUI_Color(_lblRunDyn_StepLength, GraphColorManager.PREF_GRAPH_RUN_DYN_STEP_LENGTH);
         updateUI_Color(_lblRunDyn_StepLength_Unit, GraphColorManager.PREF_GRAPH_RUN_DYN_STEP_LENGTH);
      }
      if (_isVisible_And_Available_RunDyn_VerticalOscillation) {
         updateUI_Color(_lblRunDyn_VerticalOscillation, GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
         updateUI_Color(_lblRunDyn_VerticalOscillation_Unit, GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
      }
      if (_isVisible_And_Available_RunDyn_VerticalRatio) {
         updateUI_Color(_lblRunDyn_VerticalRatio, GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_RATIO);
         updateUI_Color(_lblRunDyn_VerticalRatio_Unit, GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_RATIO);
      }
   }

}
