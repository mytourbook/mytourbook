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

import java.text.NumberFormat;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.ColorCache;
import net.tourbook.chart.HoveredValuePointData;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.color.GraphColorManager;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.tooltip.IPinned_ToolTip;
import net.tourbook.common.tooltip.IPinned_Tooltip_Owner;
import net.tourbook.common.tooltip.Pinned_ToolTip_Shell;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

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
import org.eclipse.swt.widgets.Control;
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

// SET_FORMATTING_OFF

   private static final String   GRAPH_LABEL_ALTIMETER                     = net.tourbook.common.Messages.Graph_Label_Altimeter;
   private static final String   GRAPH_LABEL_ALTITUDE                      = net.tourbook.common.Messages.Graph_Label_Altitude;
   private static final String   GRAPH_LABEL_CADENCE                       = net.tourbook.common.Messages.Graph_Label_Cadence;
   private static final String   GRAPH_LABEL_CADENCE_UNIT_RPM              = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
   private static final String   GRAPH_LABEL_CADENCE_UNIT_SPM              = net.tourbook.common.Messages.Graph_Label_Cadence_Unit_Spm;
   private static final String   GRAPH_LABEL_DISTANCE                      = net.tourbook.common.Messages.Graph_Label_Distance;
   private static final String   GRAPH_LABEL_GEARS                         = net.tourbook.common.Messages.Graph_Label_Gears;
   private static final String   GRAPH_LABEL_GRADIENT                      = net.tourbook.common.Messages.Graph_Label_Gradient;
   private static final String   GRAPH_LABEL_GRADIENT_UNIT                 = net.tourbook.common.Messages.Graph_Label_Gradient_Unit;
   private static final String   GRAPH_LABEL_HEARTBEAT                     = net.tourbook.common.Messages.Graph_Label_Heartbeat;
   private static final String   GRAPH_LABEL_HEARTBEAT_UNIT                = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
   private static final String   GRAPH_LABEL_PACE                          = net.tourbook.common.Messages.Graph_Label_Pace;
   private static final String   GRAPH_LABEL_PACE_SUMMARIZED               = net.tourbook.common.Messages.Graph_Label_Pace_Summarized;
   private static final String   GRAPH_LABEL_POWER                         = net.tourbook.common.Messages.Graph_Label_Power;
   private static final String   GRAPH_LABEL_POWER_UNIT                    = net.tourbook.common.Messages.Graph_Label_Power_Unit;
   private static final String   GRAPH_LABEL_SPEED                         = net.tourbook.common.Messages.Graph_Label_Speed;
   private static final String   GRAPH_LABEL_SPEED_SUMMARIZED              = net.tourbook.common.Messages.Graph_Label_Speed_Summarized;
   private static final String   GRAPH_LABEL_TEMPERATURE                   = net.tourbook.common.Messages.Graph_Label_Temperature;
   private static final String   GRAPH_LABEL_TIME_DURATION                 = net.tourbook.common.Messages.Graph_Label_TimeDuration;
   private static final String   GRAPH_LABEL_TIME_OF_DAY                   = net.tourbook.common.Messages.Graph_Label_TimeOfDay;
   private static final String   GRAPH_LABEL_TOUR_COMPARE                  = net.tourbook.common.Messages.Graph_Label_Tour_Compare;

   private static final String   GRAPH_LABEL_RUN_DYN_STANCE_TIME           = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime;
   private static final String   GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCED  = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalance;
   private static final String   GRAPH_LABEL_RUN_DYN_STEP_LENGTH           = net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength;
   private static final String   GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION  = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation;
   private static final String   GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO        = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio;

// SET_FORMATTING_ON

   private final IPreferenceStore         _prefStore     = TourbookPlugin.getPrefStore();

   private IPropertyChangeListener        _prefChangeListener;

   private TourData                       _tourData;

   private ValuePoint_ToolTip_MenuManager _ttMenuMgr;
   private ActionOpenTooltipMenu          _actionOpenTooltipMenu;

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
   private boolean                        _isHorizontal;

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
   private boolean _isVisible_And_Available_TimeSlice;
   private boolean _isVisible_And_Available_TourCompareResult;

   private boolean _isVisible_And_Available_RunDyn_StanceTime;
   private boolean _isVisible_And_Available_RunDyn_StanceTimeBalance;
   private boolean _isVisible_And_Available_RunDyn_StepLength;
   private boolean _isVisible_And_Available_RunDyn_VerticalOscillation;
   private boolean _isVisible_And_Available_RunDyn_VerticalRatio;

   private boolean _isAvailable_Pulse_BpmFromDevice;
   private boolean _isAvailable_Pulse_RRIntervals;

   /*
    * UI resources
    */
   private Color                    _fgBorder;
   private final ColorCache         _colorCache                   = new ColorCache();
   private final GraphColorManager  _colorManager                 = GraphColorManager.getInstance();

   private final ArrayList<Control> _firstColumnControls          = new ArrayList<>();
   private final ArrayList<Control> _firstColumnContainerControls = new ArrayList<>();

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
   private Label     _lblGears;
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

   private class ActionOpenTooltipMenu extends Action {

      public ActionOpenTooltipMenu() {

         super(null, IAction.AS_PUSH_BUTTON);

         setToolTipText(Messages.Tooltip_ValuePoint_Action_OpenToolTipMenu_ToolTip);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.TourOptions));
      }

      @Override
      public void runWithEvent(final Event event) {
         _ttMenuMgr.openToolTipMenu(event, _tourData, _allVisibleValueIds, _isHorizontal);
      }
   }

   public ValuePoint_ToolTip_UI(final IPinned_Tooltip_Owner tooltipOwner, final IDialogSettings state) {

      super(tooltipOwner, state);

      // get state if the tooltip is visible or hidden
      _isToolTipVisible = _prefStore.getBoolean(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE);

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

      _isHorizontal = ValuePoint_ToolTip_Orientation.valueOf(
            stateOrientation) == ValuePoint_ToolTip_Orientation.Horizontal;

      addPrefListener();
   }

   void actionHideToolTip() {

      _prefStore.setValue(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, false);

      _isToolTipVisible = false;

      hide();
   }

   void actionOrientation(final ValuePoint_ToolTip_Orientation orientation, final boolean isReopenToolTip) {

      _isHorizontal = orientation == ValuePoint_ToolTip_Orientation.Horizontal;

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
         if (property.equals(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE)
         //
         ) {
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

      _valueUnitDistance = _isHorizontal ? 2 : 5;

      _firstColumnControls.clear();
      _firstColumnContainerControls.clear();

      final Composite shell = createUI_010_Shell(parent);

      updateUI(_currentValueIndex);

      if (_isHorizontal == false) {

         // compute width for all controls and equalize column width for the different sections
         _shellContainer.layout(true, true);
         UI.setEqualizeColumWidths(_firstColumnControls);

         _shellContainer.layout(true, true);
         UI.setEqualizeColumWidths(_firstColumnContainerControls);
      }

      return shell;

   }

   private Composite createUI_010_Shell(final Composite parent) {

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      _shellContainer = new Composite(parent, SWT.NONE);
      GridLayoutFactory.fillDefaults()
            .spacing(0, 0)
            .numColumns(2)

            // set margin to draw the border
            .extendedMargins(1, 1, 1, 1)

            .applyTo(_shellContainer);

//      _shellContainer.setForeground(_fgColor);
//      _shellContainer.setBackground(_bgColor);
      _shellContainer.addPaintListener(this::onPaintShellContainer);
//      _shellContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {

         if (_allVisibleAndAvailable_ValueIds > 0) {

            createUI_020_AllValues(_shellContainer);

         } else {

            createUI_999_NoData(_shellContainer);
         }

         // action toolbar in the top right corner
         createUI_030_Actions(_shellContainer);
      }

      return _shellContainer;
   }

   private void createUI_020_AllValues(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.CENTER)
            .grab(true, false)
            .applyTo(container);

      if (_isHorizontal) {
         GridLayoutFactory.fillDefaults()
               .numColumns(_allVisibleAndAvailable_ValueCounter)
               .spacing(5, 0)
               .extendedMargins(3, 2, 0, 0)
               .applyTo(container);
      } else {
         GridLayoutFactory.fillDefaults()
               .spacing(5, 0)
               .applyTo(container);
      }

//      container.setBackground(_bgColor);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         createUI_100_TimeSlices(container);
         createUI_110_TimeDuration(container);
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

   private void createUI_030_Actions(final Composite parent) {

      /*
       * create toolbar
       */
      _toolbarControl = new ToolBar(parent, SWT.FLAT);
      GridDataFactory.fillDefaults()
//            .align(SWT.END, SWT.FILL)
            .align(SWT.END, SWT.BEGINNING)
            .applyTo(_toolbarControl);
//      _toolbarControl.setForeground(_fgColor);
//      _toolbarControl.setBackground(_bgColor);

//      _toolbarControl.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
//      _toolbarControl.setBackground(_fgToolbar);

      final ToolBarManager tbm = new ToolBarManager(_toolbarControl);

      tbm.add(_actionOpenTooltipMenu);

      tbm.update(true);
   }

   private void createUI_100_TimeSlices(final Composite parent) {

      if (_isVisible_And_Available_TimeSlice) {

         final Composite container = new Composite(parent, SWT.NONE);
//         container.setForeground(_fgColor);
//         container.setBackground(_bgColor);
         GridDataFactory.fillDefaults()
               .align(SWT.CENTER, SWT.FILL)
               .grab(true, false)
               .applyTo(container);
         GridLayoutFactory.fillDefaults()
               .numColumns(3)
               .spacing(2, 0)
               .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {

            // label: current value
            _lblDataSerieCurrent = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  7,
                  Messages.Tooltip_ValuePoint_Label_SlicesCurrent_Tooltip,
                  null);

            // label: separator
            createUILabel(container, UI.SYMBOL_COLON, null, null);

            // label: max value
            _lblDataSerieMax = createUI_Label_Value(
                  container,
                  SWT.LEAD,
                  7,
                  Messages.Tooltip_ValuePoint_Label_SlicesMax_Tooltip,
                  null);
         }
      }
   }

   private void createUI_110_TimeDuration(final Composite parent) {

      if (_isVisible_And_Available_TimeDuration) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblTimeDuration = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  8,
                  GRAPH_LABEL_TIME_DURATION,
                  GraphColorManager.PREF_GRAPH_TIME);

            _lblTimeDuration_Unit = createUILabel(
                  container,
                  UI.UNIT_LABEL_TIME,
                  GRAPH_LABEL_TIME_DURATION,
                  GraphColorManager.PREF_GRAPH_TIME);
         }

         _firstColumnControls.add(_lblTimeDuration);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_120_TimeOfDay(final Composite parent) {

      if (_isVisible_And_Available_TimeOfDay) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblTimeOfDay = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  8,
                  GRAPH_LABEL_TIME_OF_DAY,
                  GraphColorManager.PREF_GRAPH_TIME);

            _lblTimeOfDay_Unit = createUILabel(
                  container,
                  UI.UNIT_LABEL_TIME,
                  GRAPH_LABEL_TIME_OF_DAY,
                  GraphColorManager.PREF_GRAPH_TIME);
         }

         _firstColumnControls.add(_lblTimeOfDay);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_200_Distance(final Composite parent) {

      if (_isVisible_And_Available_Distance) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblDistance = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  9,
                  GRAPH_LABEL_DISTANCE,
                  GraphColorManager.PREF_GRAPH_DISTANCE);

            _lblDistance_Unit = createUI_Label_ValueUnit(
                  container,
                  SWT.LEAD,
                  GRAPH_LABEL_DISTANCE,
                  GraphColorManager.PREF_GRAPH_DISTANCE);

            _lblDistance_Unit.setText(UI.UNIT_LABEL_DISTANCE);
         }
         _firstColumnControls.add(_lblDistance);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_210_Altitude(final Composite parent) {

      if (_isVisible_And_Available_Elevation) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblElevation = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  6,
                  GRAPH_LABEL_ALTITUDE,
                  GraphColorManager.PREF_GRAPH_ALTITUDE);

            _lblElevation_Unit = createUI_Label_ValueUnit(
                  container,
                  SWT.LEAD,
                  GRAPH_LABEL_ALTITUDE,
                  GraphColorManager.PREF_GRAPH_ALTITUDE);

            _lblElevation_Unit.setText(UI.UNIT_LABEL_ELEVATION);
         }
         _firstColumnControls.add(_lblElevation);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_220_Pulse(final Composite parent) {

      if (_isVisible_And_Available_Pulse) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblPulse = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  3,
                  GRAPH_LABEL_HEARTBEAT,
                  GraphColorManager.PREF_GRAPH_HEARTBEAT);

            _lblPulse_Unit = createUILabel(
                  container,
                  GRAPH_LABEL_HEARTBEAT_UNIT,
                  GRAPH_LABEL_HEARTBEAT,
                  GraphColorManager.PREF_GRAPH_HEARTBEAT);
         }
         _firstColumnControls.add(_lblPulse);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_230_Speed(final Composite parent) {

      if (_isVisible_And_Available_Speed) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblSpeed = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  4,
                  GRAPH_LABEL_SPEED,
                  GraphColorManager.PREF_GRAPH_SPEED);

            _lblSpeed_Unit = createUI_Label_Value(
                  container,
                  SWT.LEAD,
                  11, // km/h needs more space
                  GRAPH_LABEL_SPEED,
                  GraphColorManager.PREF_GRAPH_SPEED);

            _lblSpeed_Unit.setText(UI.UNIT_LABEL_SPEED);
         }
         _firstColumnControls.add(_lblSpeed);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_232_Speed_Summarized(final Composite parent) {

      if (_isVisible_And_Available_Speed_Summarized) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblSpeed_Summarized = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  4,
                  GRAPH_LABEL_SPEED_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_SPEED);

            _lblSpeed_Summarized_Unit = createUI_Label_Value(
                  container,
                  SWT.LEAD,
                  14, // km/h ∑ Ø needs more space
                  GRAPH_LABEL_SPEED_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_SPEED);

            _lblSpeed_Summarized_Unit.setText(UI.UNIT_LABEL_SPEED + UI.SPACE + UI.SYMBOL_SUMMARIZED_AVERAGE);
         }
         _firstColumnControls.add(_lblSpeed_Summarized);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_240_Pace(final Composite parent) {

      if (_isVisible_And_Available_Pace) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblPace = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  5,
                  GRAPH_LABEL_PACE,
                  GraphColorManager.PREF_GRAPH_PACE);

            _lblPace_Unit = createUI_Label_Value(
                  container,
                  SWT.LEAD,
                  12, // min/km needs more space
                  GRAPH_LABEL_PACE,
                  GraphColorManager.PREF_GRAPH_PACE);

            _lblPace_Unit.setText(UI.UNIT_LABEL_PACE);
         }
         _firstColumnControls.add(_lblPace);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_242_Pace_Summarized(final Composite parent) {

      if (_isVisible_And_Available_Pace_Summarized) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblPace_Summarized = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  5,
                  GRAPH_LABEL_PACE_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_PACE);

            _lblPace_Summarized_Unit = createUI_Label_Value(
                  container,
                  SWT.LEAD,
                  15, // min/km ∑ Ø needs more space
                  GRAPH_LABEL_PACE_SUMMARIZED,
                  GraphColorManager.PREF_GRAPH_PACE);

            _lblPace_Summarized_Unit.setText(UI.UNIT_LABEL_PACE + UI.SPACE + UI.SYMBOL_SUMMARIZED_AVERAGE);
         }
         _firstColumnControls.add(_lblPace_Summarized);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_250_Power(final Composite parent) {

      if (_isVisible_And_Available_Power) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblPower = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  4,
                  GRAPH_LABEL_POWER,
                  GraphColorManager.PREF_GRAPH_POWER);

            _lblPower_Unit = createUILabel(
                  container,
                  GRAPH_LABEL_POWER_UNIT,
                  GRAPH_LABEL_POWER,
                  GraphColorManager.PREF_GRAPH_POWER);
         }
         _firstColumnControls.add(_lblPower);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_260_Temperature(final Composite parent) {

      if (_isVisible_And_Available_Temperature) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblTemperature = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  4,
                  GRAPH_LABEL_TEMPERATURE,
                  GraphColorManager.PREF_GRAPH_TEMPTERATURE);

            _lblTemperature_Unit = createUI_Label_ValueUnit(
                  container,
                  SWT.LEAD,
                  GRAPH_LABEL_TEMPERATURE,
                  GraphColorManager.PREF_GRAPH_TEMPTERATURE);

            _lblTemperature_Unit.setText(UI.UNIT_LABEL_TEMPERATURE);
         }
         _firstColumnControls.add(_lblTemperature);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_270_Gradient(final Composite parent) {

      if (_isVisible_And_Available_Gradient) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblGradient = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  4,
                  GRAPH_LABEL_GRADIENT,
                  GraphColorManager.PREF_GRAPH_GRADIENT);

            _lblGradient_Unit = createUILabel(
                  container,
                  GRAPH_LABEL_GRADIENT_UNIT,
                  GRAPH_LABEL_GRADIENT,
                  GraphColorManager.PREF_GRAPH_GRADIENT);
         }
         _firstColumnControls.add(_lblGradient);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_280_Altimeter(final Composite parent) {

      if (_isVisible_And_Available_Altimeter) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblAltimeter = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  6,
                  GRAPH_LABEL_ALTIMETER,
                  GraphColorManager.PREF_GRAPH_ALTIMETER);

            _lblAltimeter_Unit = createUI_Label_ValueUnit(
                  container,
                  SWT.LEAD,
                  GRAPH_LABEL_ALTIMETER,
                  GraphColorManager.PREF_GRAPH_ALTIMETER);

            _lblAltimeter_Unit.setText(UI.UNIT_LABEL_ALTIMETER);
         }
         _firstColumnControls.add(_lblAltimeter);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_290_Cadence(final Composite parent) {

      if (_isVisible_And_Available_Cadence) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblCadence = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  3,
                  GRAPH_LABEL_CADENCE,
                  GraphColorManager.PREF_GRAPH_CADENCE);

            _lblCadence_Unit = createUILabel(
                  container,
                  GRAPH_LABEL_CADENCE_UNIT_RPM,
                  GRAPH_LABEL_CADENCE,
                  GraphColorManager.PREF_GRAPH_CADENCE);
         }
         _firstColumnControls.add(_lblCadence);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_300_Gears(final Composite parent) {

      if (_isVisible_And_Available_Gears) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblGears = createUI_Label_Value(//
                  container,
                  SWT.TRAIL,
                  10,
                  GRAPH_LABEL_GEARS,

                  // this is a bit tricky, use default color because the text color is white
                  null
//                  GraphColorManager.PREF_GRAPH_GEAR
            );

            // no unit
            createUILabel(container, UI.EMPTY_STRING, GRAPH_LABEL_GEARS, GraphColorManager.PREF_GRAPH_GEAR);
         }
         _firstColumnControls.add(_lblGears);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_310_TourCompareResult(final Composite parent) {

      if (_isVisible_And_Available_TourCompareResult) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblTourCompareResult = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  12,
                  GRAPH_LABEL_TOUR_COMPARE,
                  GraphColorManager.PREF_GRAPH_GRADIENT);

            // no unit
            createUILabel(
                  container,
                  UI.EMPTY_STRING,
                  GRAPH_LABEL_TOUR_COMPARE,
                  GraphColorManager.PREF_GRAPH_TOUR_COMPARE);
         }

         _firstColumnControls.add(_lblTourCompareResult);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_400_RunDyn_StanceTime(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_StanceTime) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblRunDyn_StanceTime = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  3,
                  GRAPH_LABEL_RUN_DYN_STANCE_TIME,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME);

            _lblRunDyn_StanceTime_Unit = createUILabel(
                  container,
                  UI.UNIT_MS,
                  GRAPH_LABEL_RUN_DYN_STANCE_TIME,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME);
         }
         _firstColumnControls.add(_lblRunDyn_StanceTime);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_410_RunDyn_StanceTimeBalance(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_StanceTimeBalance) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblRunDyn_StanceTimeBalance = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  3,
                  GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCED,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED);

            _lblRunDyn_StanceTimeBalance_Unit = createUILabel(
                  container,
                  UI.UNIT_PERCENT,
                  GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCED,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STANCE_TIME_BALANCED);
         }
         _firstColumnControls.add(_lblRunDyn_StanceTimeBalance);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_420_RunDyn_StepLength(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_StepLength) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblRunDyn_StepLength = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  3,
                  GRAPH_LABEL_RUN_DYN_STEP_LENGTH,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STEP_LENGTH);

            _lblRunDyn_StepLength_Unit = createUILabel(
                  container,
                  UI.UNIT_LABEL_DISTANCE_MM_OR_INCH,
                  GRAPH_LABEL_RUN_DYN_STEP_LENGTH,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_STEP_LENGTH);
         }
         _firstColumnControls.add(_lblRunDyn_StepLength);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_430_RunDyn_VerticalOscillation(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_VerticalOscillation) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblRunDyn_VerticalOscillation = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  3,
                  GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION);

            _lblRunDyn_VerticalOscillation_Unit = createUILabel(
                  container,
                  UI.UNIT_LABEL_DISTANCE_MM_OR_INCH,
                  GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_OSCILLATION);
         }
         _firstColumnControls.add(_lblRunDyn_VerticalOscillation);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_440_RunDyn_VerticalRatio(final Composite parent) {

      if (_isVisible_And_Available_RunDyn_VerticalRatio) {

         final Composite container = createUIValueContainer(parent);
         {
            _lblRunDyn_VerticalRatio = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  3,
                  GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_RATIO);

            _lblRunDyn_VerticalRatio_Unit = createUILabel(
                  container,
                  UI.UNIT_PERCENT,
                  GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO,
                  GraphColorManager.PREF_GRAPH_RUN_DYN_VERTICAL_RATIO);
         }
         _firstColumnControls.add(_lblRunDyn_VerticalRatio);
         _firstColumnContainerControls.add(container);
      }
   }

   private void createUI_500_ChartZoomFactor(final Composite parent) {

      if (_isVisible_And_Available_ChartZoomFactor) {

         final Composite container = createUIValueContainer(parent);
         {

            _lblChartZoomFactor = createUI_Label_Value(
                  container,
                  SWT.TRAIL,
                  8,
                  Messages.Tooltip_ValuePoint_Label_ChartZoomFactor_Tooltip,
                  null);

            // spacer
            new Label(container, SWT.NONE);
         }

         _firstColumnControls.add(_lblChartZoomFactor);
      }
   }

   private Composite createUI_999_NoData(final Composite parent) {

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
//      shellContainer.setForeground(_fgColor);
//      shellContainer.setBackground(_bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {

         final Composite container = new Composite(shellContainer, SWT.NONE);
//         container.setForeground(_fgColor);
//         container.setBackground(_bgColor);
         GridLayoutFactory
               .fillDefaults()//
               .extendedMargins(5, 5, 0, 0)
               .applyTo(container);
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Tooltip_ValuePoint_Label_NoData);
            label.setToolTipText(Messages.Tooltip_ValuePoint_Label_NoData_Tooltip);
//            label.setForeground(_fgColor);
//            label.setBackground(_bgColor);
         }
      }

      return shellContainer;
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

//      label.setBackground(_bgColor);

      if (tooltip != null) {
         label.setToolTipText(tooltip);
      }

      updateUI_Color(label, colorId);

      return label;
   }

   private Label createUI_Label_ValueUnit(final Composite parent,
                                          final int style,
                                          final String tooltip,
                                          final String colorId) {

      return createUI_Label_Value(parent, style, SWT.DEFAULT, tooltip, colorId);
   }

   /**
    * @param parent
    * @param labelText
    * @param tooltip
    * @param colorId
    * @return Returns created label.
    */
   private Label createUILabel(final Composite parent,
                               final String labelText,
                               final String tooltip,
                               final String colorId) {

      final Label label = new Label(parent, SWT.NONE);
//      label.setForeground(_fgColor);
//      label.setBackground(_bgColor);

      if (labelText != null) {
         label.setText(labelText);
      }

      if (tooltip != null) {
         label.setToolTipText(tooltip);
      }

      if (colorId != null) {

         final Color fgColor = _colorCache.getColor(
               colorId,
               _colorManager.getGraphColorDefinition(colorId).getTextColor_Active_Themed());

         label.setForeground(fgColor);
      }

      return label;
   }

   private Composite createUIValueContainer(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).spacing(_valueUnitDistance, 0).applyTo(container);
//      container.setForeground(_fgColor);
//      container.setBackground(_bgColor);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));

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

      final String cadenceUnit = cadenceMultiplier == 2.0 ? GRAPH_LABEL_CADENCE_UNIT_SPM : GRAPH_LABEL_CADENCE_UNIT_RPM;

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

      _firstColumnControls.clear();
      _firstColumnContainerControls.clear();

      super.onDispose();
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

      final HoveredValuePointData hoveredValuePointData = (HoveredValuePointData) hoveredData;

      _devXMouse = devXMouseMove;
      _devYMouse = devYMouseMove;

      _chartZoomFactor = hoveredValuePointData.graphZoomFactor;

      if (_shellContainer == null || _shellContainer.isDisposed()) {

         /*
          * tool tip is disposed, this happens on a mouse exit, display the tooltip again
          */
         show(new Point(devXMouseMove, devYMouseMove));
      }

      // check again
      if (_shellContainer != null && !_shellContainer.isDisposed()) {

         setTTShellLocation(devXMouseMove, devYMouseMove, hoveredValuePointData.valueDevPosition);

         updateUI(hoveredValuePointData.valueIndex);
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
   void setTourData(final TourData tourData) {

      _tourData = tourData;
      _currentValueIndex = 0;

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
      final long visibleId_TimeSlice                  = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TIME_SLICES);
      final long visibleId_TourCompareResult          = getState(ttVisibleValues, ValuePoint_ToolTip_MenuManager.VALUE_ID_TOUR_COMPARE_RESULT);

      _isAvailable_Pulse_BpmFromDevice                = _tourData.pulseSerie                          != null;
      _isAvailable_Pulse_RRIntervals                  = _tourData.getPulse_RRIntervals()              != null;
                                                                                                      
      final boolean isAvailable_Altimeter             = _tourData.getAltimeterSerie()                 != null;
      final boolean isAvailable_Altitude              = _tourData.getAltitudeSerie()                  != null;
      final boolean isAvailable_Cadence               = _tourData.getCadenceSerie()                   != null;
      final boolean isAvailable_ChartZoomFactor       = true;                                         
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
            + (isAvailable_TimeSlice         ? visibleId_TimeSlice         : 0)
            + (isAvailable_TourCompareResult ? visibleId_TourCompareResult : 0)

            + (isAvailable_RunDyn_StanceTime          ? visibleId_RunDyn_StanceTime          : 0)
            + (isAvailable_RunDyn_StanceTimeBalance   ? visibleId_RunDyn_StanceTimeBalance   : 0)
            + (isAvailable_RunDyn_StepLength          ? visibleId_RunDyn_StepLength          : 0)
            + (isAvailable_RunDyn_VerticalOscillation ? visibleId_RunDyn_VerticalOscillation : 0)
            + (isAvailable_RunDyn_VerticalRatio       ? visibleId_RunDyn_VerticalRatio       : 0)

            ;

      _isVisible_And_Available_Altimeter           = isAvailable_Altimeter          && visibleId_Altimeter > 0;
      _isVisible_And_Available_Elevation            = isAvailable_Altitude           && visibleId_Altitude > 0;
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

      if (requestedRedrawTime > _lastUpdateUITime + 100) {

         // force a redraw

         updateUI_Runnable(valueIndex);

      } else {

         _updateCounter[0]++;

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

      // check bounds
      if (valueIndex < 0 || valueIndex >= timeSerie.length) {
         valueIndex = timeSerie.length - 1;
      }

      _currentValueIndex = valueIndex;

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

//         _gears[0] = gear ratio
//         _gears[1] = front gear teeth
//         _gears[2] = rear gear teeth
//         _gears[3] = front gear number, starting with 1
//         _gears[4] = rear gear number, starting with 1

         _lblGears.setText(String.format(
               TourManager.GEAR_VALUE_FORMAT,
               (int) gears[1][valueIndex],
               (int) gears[2][valueIndex],
               gears[0][valueIndex]
         //
         ));
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

         float temperature = _tourData.temperatureSerie[valueIndex];

         if (UI.UNIT_IS_TEMPERATURE_FAHRENHEIT) {

            // get imperial temperature

            temperature = temperature
                  * UI.UNIT_FAHRENHEIT_MULTI
                  + UI.UNIT_FAHRENHEIT_ADD;
         }

         _lblTemperature.setText(_nf1.format(temperature));
      }

      if (_isVisible_And_Available_TimeDuration) {
         _lblTimeDuration.setText(UI.format_hhh_mm_ss(timeSerie[valueIndex]));
      }

      if (_isVisible_And_Available_TimeOfDay) {
         _lblTimeOfDay.setText(UI.format_hhh_mm_ss((_tourData.getStartTimeOfDay() + timeSerie[valueIndex]) % 86400));
      }

      if (_isVisible_And_Available_TimeSlice) {

         /*
          * Show the same time slice value which is also selected in the tour editor where time
          * slices are starting with 1, otherwise it could be confusing
          */

         _lblDataSerieCurrent.setText(Integer.toString(_currentValueIndex + 1));
         _lblDataSerieMax.setText(Integer.toString(timeSerie.length));
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
         updateUI_Color(_lblGears, GraphColorManager.PREF_GRAPH_GEAR);
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
