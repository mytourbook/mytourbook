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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.action.ActionResetToDefaults;
import net.tourbook.common.action.IActionResetToDefault;
import net.tourbook.common.color.IColorSelectorListener;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPagePeople;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.ToolBar;

/**
 * Tour chart marker properties slideout.
 */
public class SlideoutGraphMinMax extends ToolbarSlideout implements IColorSelectorListener, IActionResetToDefault {

   private static final String    GRAPH_LABEL_CADENCE_UNIT         = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
   private static final String    GRAPH_LABEL_HEARTBEAT_UNIT       = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
   private static final String    GRAPH_LABEL_POWER_UNIT           = net.tourbook.common.Messages.Graph_Label_Power_Unit;
   private static final String    GRAPH_LABEL_SWIM_STROKES         = net.tourbook.common.Messages.Graph_Label_Swim_Strokes;
   private static final String    GRAPH_LABEL_SWIM_SWOLF           = net.tourbook.common.Messages.Graph_Label_Swim_Swolf;

   private static final int       ALTIMETER_MIN                    = -10000;
   private static final int       ALTIMETER_MAX                    = 10000;
   private static final int       ALTITUDE_MIN                     = -1000;
   private static final int       ALTITUDE_MAX                     = 10000;
   private static final int       CADENCE_MAX                      = 300;
   private static final int       GRADIENT_MIN                     = -100;
   private static final int       GRADIENT_MAX                     = 100;
   private static final int       PACE_MAX                         = 100;
   private static final int       POWER_MAX                        = 1000000;
   private static final int       SPEED_MAX                        = 1000;
   private static final int       TEMPERATURE_MIN                  = -100;
   private static final int       TEMPERATURE_MAX                  = 100;

   private static final int       RUN_DYN_STANCE_TIME_MAX          = 10000;                                                  // ms
   private static final int       RUN_DYN_STANCE_TIME_BALANCED_MAX = 100;                                                    // %
   private static final int       RUN_DYN_STEP_LENGTH_MAX          = 10000;                                                  // mm
   private static final int       RUN_DYN_VERTICAL_OSCILLATION_MAX = 1000;                                                   // mm
   private static final int       RUN_DYN_VERTICAL_RATIO_MAX       = 100;                                                    // %

   private static final int       SWIM_STROKES_MAX                 = 10000;
   private static final int       SWIM_SWOLF_MAX                   = 10000;

   private final IPreferenceStore _prefStore                       = TourbookPlugin.getPrefStore();

   private SelectionListener      _defaultSelectionListener;
   private MouseWheelListener     _defaultMouseWheelListener;

   {
      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> onChangeUI());

      _defaultMouseWheelListener = mouseEvent -> {
         UI.adjustSpinnerValueOnMouseScroll(mouseEvent);
         onChangeUI();
      };
   }

   private PixelConverter        _pc;

   private ActionResetToDefaults _actionRestoreDefaults;

   private int                   _columnSpacing;

   /*
    * UI controls
    */
   private Composite _parent;

   private Button    _chkEnableMinMax;
   private Button    _chkMin_Altimeter;
   private Button    _chkMax_Altimeter;
   private Button    _chkMin_Altitude;
   private Button    _chkMax_Altitude;
   private Button    _chkMin_Cadence;
   private Button    _chkMax_Cadence;
   private Button    _chkMin_Gradient;
   private Button    _chkMax_Gradient;
   private Button    _chkMin_Pace;
   private Button    _chkMax_Pace;
   private Button    _chkMin_Power;
   private Button    _chkMax_Power;
   private Button    _chkMin_Pulse;
   private Button    _chkMax_Pulse;
   private Button    _chkMax_Speed;
   private Button    _chkMin_Speed;
   private Button    _chkMin_Temperature;
   private Button    _chkMax_Temperature;

   private Button    _chkMin_RunDyn_StanceTime;
   private Button    _chkMax_RunDyn_StanceTime;
   private Button    _chkMin_RunDyn_StanceTimeBalance;
   private Button    _chkMax_RunDyn_StanceTimeBalance;
   private Button    _chkMin_RunDyn_StepLength;
   private Button    _chkMax_RunDyn_StepLength;
   private Button    _chkMin_RunDyn_VerticalOscillation;
   private Button    _chkMax_RunDyn_VerticalOscillation;
   private Button    _chkMin_RunDyn_VerticalRatio;
   private Button    _chkMax_RunDyn_VerticalRatio;

   private Button    _chkMin_Swim_Strokes;
   private Button    _chkMax_Swim_Strokes;
   private Button    _chkMin_Swim_Swolf;
   private Button    _chkMax_Swim_Swolf;

   private Label     _lblMaxValue;
   private Label     _lblMinValue;
   private Label     _lblMinMax_Altimeter;
   private Label     _lblMinMax_Altimeter_Unit;
   private Label     _lblMinMax_Altitude;
   private Label     _lblMinMax_Altitude_Unit;
   private Label     _lblMinMax_Cadence;
   private Label     _lblMinMax_Cadence_Unit;
   private Label     _lblMinMax_Gradient;
   private Label     _lblMinMax_Gradient_Unit;
   private Label     _lblMinMax_Pulse;
   private Label     _lblMinMax_Pulse_Unit;
   private Label     _lblMinMax_Pace;
   private Label     _lblMinMax_Pace_Unit;
   private Label     _lblMinMax_Power;
   private Label     _lblMinMax_Power_Unit;
   private Label     _lblMinMax_Speed;
   private Label     _lblMinMax_Speed_Unit;
   private Label     _lblMinMax_Temperature;
   private Label     _lblMinMax_Temperature_Unit;

   private Label     _lblMinMax_RunDyn_StanceTime;
   private Label     _lblMinMax_RunDyn_StanceTime_Unit;
   private Label     _lblMinMax_RunDyn_StanceTimeBalance;
   private Label     _lblMinMax_RunDyn_StanceTimeBalance_Unit;
   private Label     _lblMinMax_RunDyn_StepLength;
   private Label     _lblMinMax_RunDyn_StepLength_Unit;
   private Label     _lblMinMax_RunDyn_VerticalOscillation;
   private Label     _lblMinMax_RunDyn_VerticalOscillation_Unit;
   private Label     _lblMinMax_RunDyn_VerticalRatio;
   private Label     _lblMinMax_RunDyn_VerticalRatio_Unit;

   private Label     _lblMinMax_Swim_Strokes;
   private Label     _lblMinMax_Swim_Strokes_Unit;
   private Label     _lblMinMax_Swim_Swolf;
   private Label     _lblMinMax_Swim_Swolf_Unit;

   private Spinner   _spinnerMin_Altimeter;
   private Spinner   _spinnerMax_Altimeter;
   private Spinner   _spinnerMin_Altitude;
   private Spinner   _spinnerMax_Altitude;
   private Spinner   _spinnerMin_Cadence;
   private Spinner   _spinnerMax_Cadence;
   private Spinner   _spinnerMin_Gradient;
   private Spinner   _spinnerMax_Gradient;
   private Spinner   _spinnerMin_Pace;
   private Spinner   _spinnerMax_Pace;
   private Spinner   _spinnerMin_Power;
   private Spinner   _spinnerMax_Power;
   private Spinner   _spinnerMin_Pulse;
   private Spinner   _spinnerMax_Pulse;
   private Spinner   _spinnerMin_Speed;
   private Spinner   _spinnerMax_Speed;
   private Spinner   _spinnerMin_Temperature;
   private Spinner   _spinnerMax_Temperature;

   private Spinner   _spinnerMin_RunDyn_StanceTime;
   private Spinner   _spinnerMax_RunDyn_StanceTime;
   private Spinner   _spinnerMin_RunDyn_StanceTimeBalance;
   private Spinner   _spinnerMax_RunDyn_StanceTimeBalance;
   private Spinner   _spinnerMin_RunDyn_StepLength;
   private Spinner   _spinnerMax_RunDyn_StepLength;
   private Spinner   _spinnerMin_RunDyn_VerticalOscillation;
   private Spinner   _spinnerMax_RunDyn_VerticalOscillation;
   private Spinner   _spinnerMin_RunDyn_VerticalRatio;
   private Spinner   _spinnerMax_RunDyn_VerticalRatio;

   private Spinner   _spinnerMin_Swim_Strokes;
   private Spinner   _spinnerMax_Swim_Strokes;
   private Spinner   _spinnerMin_Swim_Swolf;
   private Spinner   _spinnerMax_Swim_Swolf;

   private Image     _imageAltimeter;
   private Image     _imageAltitude;
   private Image     _imageCadence;
   private Image     _imageGradient;
   private Image     _imagePace;
   private Image     _imagePower;
   private Image     _imagePulse;
   private Image     _imageSpeed;
   private Image     _imageTemperature;

   private Image     _imageAltimeterDisabled;
   private Image     _imageAltitudeDisabled;
   private Image     _imageCadenceDisabled;
   private Image     _imageGradientDisabled;
   private Image     _imagePaceDisabled;
   private Image     _imagePowerDisabled;
   private Image     _imagePulseDisabled;
   private Image     _imageSpeedDisabled;
   private Image     _imageTemperatureDisabled;

   private Image     _imageRunDyn_StanceTime;
   private Image     _imageRunDyn_StanceTime_Disabled;
   private Image     _imageRunDyn_StanceTimeBalance;
   private Image     _imageRunDyn_StanceTimeBalance_Disabled;
   private Image     _imageRunDyn_StepLength;
   private Image     _imageRunDyn_StepLength_Disabled;
   private Image     _imageRunDyn_VerticalOscillation;
   private Image     _imageRunDyn_VerticalOscillation_Disabled;
   private Image     _imageRunDyn_VerticalRatio;
   private Image     _imageRunDyn_VerticalRatio_Disabled;

   private Image     _imageSwim_Strokes;
   private Image     _imageSwim_Strokes_Disabled;
   private Image     _imageSwim_Swolf;
   private Image     _imageSwim_Swolf_Disabled;

   private CLabel    _iconAltimeter;
   private CLabel    _iconAltitude;
   private CLabel    _iconCadence;
   private CLabel    _iconGradient;
   private CLabel    _iconPace;
   private CLabel    _iconPower;
   private CLabel    _iconPulse;
   private CLabel    _iconSpeed;
   private CLabel    _iconTemperature;

   private CLabel    _iconRunDyn_StanceTime;
   private CLabel    _iconRunDyn_StanceTimeBalance;
   private CLabel    _iconRunDyn_StepLength;
   private CLabel    _iconRunDyn_VerticalOscillation;
   private CLabel    _iconRunDyn_VerticalRatio;

   private CLabel    _iconSwim_Strokes;
   private CLabel    _iconSwim_Swolf;

   private boolean   _isShowPaceChartInverted;

   public SlideoutGraphMinMax(final Control ownerControl, final ToolBar toolBar) {

      super(ownerControl, toolBar);
   }

   @Override
   protected boolean canShowToolTip() {
      return true;
   }

   @Override
   protected boolean closeShellAfterHidden() {

      /*
       * Close the tooltip that the state is saved.
       */

      return true;
   }

   @Override
   public void colorDialogOpened(final boolean isAnotherDialogOpened) {

      setIsAnotherDialogOpened(isAnotherDialogOpened);
   }

   private void createActions() {

      _actionRestoreDefaults = new ActionResetToDefaults(this);
   }

   @Override
   protected Composite createToolTipContentArea(final Composite parent) {

      _parent = parent;

      initUI(parent);
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
         GridLayoutFactory.fillDefaults()
               .numColumns(2)
               .applyTo(container);
//			container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
         {
            createUI_10_Title(container);
            createUI_12_Actions(container);
         }

         createUI_50_MinMaxValues(shellContainer);
      }

      return shellContainer;
   }

   private void createUI_10_Title(final Composite parent) {

      /*
       * Label: Slideout title
       */
      final Label label = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(label);
      label.setText(Messages.Slideout_GraphMinMax_Label_Title);
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

   private Control createUI_50_MinMaxValues(final Composite parent) {

      _columnSpacing = _pc.convertWidthInCharsToPixels(4);

      final Composite container = new Composite(parent, SWT.NONE);
      GridLayoutFactory.swtDefaults().applyTo(container);
      GridLayoutFactory.swtDefaults()
            .numColumns(7)
            .spacing(_pc.convertHorizontalDLUsToPixels(4), _pc.convertVerticalDLUsToPixels(4))
            .applyTo(container);
      {
         createUI_52_MinMax_Enable(container);
         createUI_54_MinMax_Header(container);

         createUI_61_MinMax_Elevation(container);
         createUI_64_MinMax_Pulse(container);
         createUI_65_MinMax_Speed(container);
         createUI_66_MinMax_Pace(container);
         createUI_68_MinMax_Power(container);
         createUI_69_MinMax_Temperature(container);
         createUI_63_MinMax_Gradient(container);
         createUI_62_MinMax_Altimeter(container);
         createUI_67_MinMax_Cadence(container);

         createUI_80_MinMax_RunDyn_StanceTime(container);
         createUI_81_MinMax_RunDyn_StanceTimeBalance(container);
         createUI_82_MinMax_RunDyn_StepLength(container);
         createUI_83_MinMax_RunDyn_VerticalOscillation(container);
         createUI_84_MinMax_RunDyn_VerticalRatio(container);

         createUI_84_MinMax_Swim_Strokes(container);
         createUI_86_MinMax_Swim_Swolf(container);
      }

      return container;
   }

   private void createUI_52_MinMax_Enable(final Composite container) {

      // checkbox: enable min/max
      _chkEnableMinMax = new Button(container, SWT.CHECK);
      GridDataFactory.fillDefaults()
            .span(7, 1)
            .applyTo(_chkEnableMinMax);
      _chkEnableMinMax.setText(Messages.Pref_Graphs_Checkbox_EnableMinMaxValues);
      _chkEnableMinMax.addSelectionListener(_defaultSelectionListener);
   }

   private void createUI_54_MinMax_Header(final Composite parent) {

      // label: spacer
      new Label(parent, SWT.NONE);
      new Label(parent, SWT.NONE);

      // label: min value
      _lblMinValue = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .indent(_columnSpacing, 0)
            .applyTo(_lblMinValue);
      _lblMinValue.setText(Messages.Pref_Graphs_Label_MinValue);

      // label: max value
      _lblMaxValue = new Label(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
            .span(2, 1)
            .indent(_columnSpacing, 0)
            .applyTo(_lblMaxValue);
      _lblMaxValue.setText(Messages.Pref_Graphs_Label_MaxValue);

      // label: spacer
      new Label(parent, SWT.NONE);
   }

   private void createUI_61_MinMax_Elevation(final Composite parent) {

      _iconAltitude = createUI_Icon(parent, _imageAltitude);

      _lblMinMax_Altitude = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Altitude);

      _chkMin_Altitude = createUI_Checkbox(parent);
      _spinnerMin_Altitude = createUI_Spinner(parent, //
            ALTITUDE_MIN,
            ALTITUDE_MAX);

      _chkMax_Altitude = createUI_Checkbox(parent);
      _spinnerMax_Altitude = createUI_Spinner(parent, //
            ALTITUDE_MIN,
            ALTITUDE_MAX);

      _lblMinMax_Altitude_Unit = createUI_Label(parent, UI.UNIT_LABEL_ELEVATION);
   }

   private void createUI_62_MinMax_Altimeter(final Composite parent) {

      _iconAltimeter = createUI_Icon(parent, _imageAltimeter);

      _lblMinMax_Altimeter = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceAltimeterValue);

      _chkMin_Altimeter = createUI_Checkbox(parent);
      _spinnerMin_Altimeter = createUI_Spinner(parent, //
            ALTIMETER_MIN,
            ALTIMETER_MAX);

      _chkMax_Altimeter = createUI_Checkbox(parent);
      _spinnerMax_Altimeter = createUI_Spinner(parent, //
            ALTIMETER_MIN,
            ALTIMETER_MAX);

      _lblMinMax_Altimeter_Unit = createUI_Label(parent, UI.UNIT_LABEL_ALTIMETER);
   }

   private void createUI_63_MinMax_Gradient(final Composite parent) {

      _iconGradient = createUI_Icon(parent, _imageGradient);

      _lblMinMax_Gradient = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceGradientValue);

      _chkMin_Gradient = createUI_Checkbox(parent);
      _spinnerMin_Gradient = createUI_Spinner(parent, //
            GRADIENT_MIN,
            GRADIENT_MAX);

      _chkMax_Gradient = createUI_Checkbox(parent);
      _spinnerMax_Gradient = createUI_Spinner(parent, //
            GRADIENT_MIN,
            GRADIENT_MAX);

      _lblMinMax_Gradient_Unit = createUI_Label(parent, UI.SYMBOL_PERCENTAGE);
   }

   private void createUI_64_MinMax_Pulse(final Composite parent) {

      _iconPulse = createUI_Icon(parent, _imagePulse);

      _lblMinMax_Pulse = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForcePulseValue);

      _chkMin_Pulse = createUI_Checkbox(parent);
      _spinnerMin_Pulse = createUI_Spinner(parent, 0, PrefPagePeople.HEART_BEAT_MAX);

      _chkMax_Pulse = createUI_Checkbox(parent);
      _spinnerMax_Pulse = createUI_Spinner(parent, 0, PrefPagePeople.HEART_BEAT_MAX);

      _lblMinMax_Pulse_Unit = createUI_Label(parent, GRAPH_LABEL_HEARTBEAT_UNIT);
   }

   private void createUI_65_MinMax_Speed(final Composite parent) {

      _iconSpeed = createUI_Icon(parent, _imageSpeed);

      _lblMinMax_Speed = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Speed);

      _chkMin_Speed = createUI_Checkbox(parent);
      _spinnerMin_Speed = createUI_Spinner(parent, 0, SPEED_MAX);

      _chkMax_Speed = createUI_Checkbox(parent);
      _spinnerMax_Speed = createUI_Spinner(parent, 0, SPEED_MAX);

      _lblMinMax_Speed_Unit = createUI_Label(parent, UI.UNIT_LABEL_SPEED);
   }

   private void createUI_66_MinMax_Pace(final Composite parent) {

      _iconPace = createUI_Icon(parent, _imagePace);

      _lblMinMax_Pace = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForcePaceValue);

      _chkMin_Pace = createUI_Checkbox(parent);
      _spinnerMin_Pace = createUI_Spinner(parent, 0, PACE_MAX);

      _chkMax_Pace = createUI_Checkbox(parent);
      _spinnerMax_Pace = createUI_Spinner(parent, 0, PACE_MAX);

      _lblMinMax_Pace_Unit = createUI_Label(parent, UI.UNIT_LABEL_PACE);
   }

   private void createUI_67_MinMax_Cadence(final Composite parent) {

      _iconCadence = createUI_Icon(parent, _imageCadence);

      _lblMinMax_Cadence = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Cadence);

      _chkMin_Cadence = createUI_Checkbox(parent);
      _spinnerMin_Cadence = createUI_Spinner(parent, 0, CADENCE_MAX);

      _chkMax_Cadence = createUI_Checkbox(parent);
      _spinnerMax_Cadence = createUI_Spinner(parent, 0, CADENCE_MAX);

      _lblMinMax_Cadence_Unit = createUI_Label(parent, GRAPH_LABEL_CADENCE_UNIT);
   }

   private void createUI_68_MinMax_Power(final Composite parent) {

      _iconPower = createUI_Icon(parent, _imagePower);

      _lblMinMax_Power = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Power);

      _chkMin_Power = createUI_Checkbox(parent);
      _spinnerMin_Power = createUI_Spinner(parent, 0, POWER_MAX);

      _chkMax_Power = createUI_Checkbox(parent);
      _spinnerMax_Power = createUI_Spinner(parent, 0, POWER_MAX);

      _lblMinMax_Power_Unit = createUI_Label(parent, GRAPH_LABEL_POWER_UNIT);
   }

   private void createUI_69_MinMax_Temperature(final Composite parent) {

      _iconTemperature = createUI_Icon(parent, _imageTemperature);

      _lblMinMax_Temperature = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Temperature);

      _chkMin_Temperature = createUI_Checkbox(parent);
      _spinnerMin_Temperature = createUI_Spinner(parent, TEMPERATURE_MIN, TEMPERATURE_MAX);

      _chkMax_Temperature = createUI_Checkbox(parent);
      _spinnerMax_Temperature = createUI_Spinner(parent, TEMPERATURE_MIN, TEMPERATURE_MAX);

      _lblMinMax_Temperature_Unit = createUI_Label(parent, UI.UNIT_LABEL_TEMPERATURE);
   }

   private void createUI_80_MinMax_RunDyn_StanceTime(final Composite parent) {

      _iconRunDyn_StanceTime = createUI_Icon(parent, _imageRunDyn_StanceTime);

      _lblMinMax_RunDyn_StanceTime = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_RunDyn_StanceTime);

      _chkMin_RunDyn_StanceTime = createUI_Checkbox(parent);
      _spinnerMin_RunDyn_StanceTime = createUI_Spinner(parent, 0, RUN_DYN_STANCE_TIME_MAX);

      _chkMax_RunDyn_StanceTime = createUI_Checkbox(parent);
      _spinnerMax_RunDyn_StanceTime = createUI_Spinner(parent, 0, RUN_DYN_STANCE_TIME_MAX);

      _lblMinMax_RunDyn_StanceTime_Unit = createUI_Label(parent, UI.UNIT_MS);
   }

   private void createUI_81_MinMax_RunDyn_StanceTimeBalance(final Composite parent) {

      _iconRunDyn_StanceTimeBalance = createUI_Icon(parent, _imageRunDyn_StanceTimeBalance);

      _lblMinMax_RunDyn_StanceTimeBalance = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_RunDyn_StanceTimeBalance);

      _chkMin_RunDyn_StanceTimeBalance = createUI_Checkbox(parent);
      _spinnerMin_RunDyn_StanceTimeBalance = createUI_Spinner(parent, 0, RUN_DYN_STANCE_TIME_BALANCED_MAX);

      _chkMax_RunDyn_StanceTimeBalance = createUI_Checkbox(parent);
      _spinnerMax_RunDyn_StanceTimeBalance = createUI_Spinner(parent, 0, RUN_DYN_STANCE_TIME_BALANCED_MAX);

      _lblMinMax_RunDyn_StanceTimeBalance_Unit = createUI_Label(parent, UI.UNIT_PERCENT);
   }

   private void createUI_82_MinMax_RunDyn_StepLength(final Composite parent) {

      final int maxStepLength = (int) (RUN_DYN_STEP_LENGTH_MAX * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH);

      _iconRunDyn_StepLength = createUI_Icon(parent, _imageRunDyn_StepLength);

      _lblMinMax_RunDyn_StepLength = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_RunDyn_StepLength);

      _chkMin_RunDyn_StepLength = createUI_Checkbox(parent);
      _spinnerMin_RunDyn_StepLength = createUI_Spinner(parent, 0, maxStepLength);

      _chkMax_RunDyn_StepLength = createUI_Checkbox(parent);
      _spinnerMax_RunDyn_StepLength = createUI_Spinner(parent, 0, maxStepLength);

      _lblMinMax_RunDyn_StepLength_Unit = createUI_Label(parent, UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
   }

   private void createUI_83_MinMax_RunDyn_VerticalOscillation(final Composite parent) {

      final int maxVertOscillation = (int) (RUN_DYN_VERTICAL_OSCILLATION_MAX * UI.UNIT_VALUE_DISTANCE_MM_OR_INCH);

      _iconRunDyn_VerticalOscillation = createUI_Icon(parent, _imageRunDyn_VerticalOscillation);

      _lblMinMax_RunDyn_VerticalOscillation = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_RunDyn_VerticalOscillation);

      _chkMin_RunDyn_VerticalOscillation = createUI_Checkbox(parent);
      _spinnerMin_RunDyn_VerticalOscillation = createUI_Spinner(parent, 0, maxVertOscillation);

      _chkMax_RunDyn_VerticalOscillation = createUI_Checkbox(parent);
      _spinnerMax_RunDyn_VerticalOscillation = createUI_Spinner(parent, 0, maxVertOscillation);

      _lblMinMax_RunDyn_VerticalOscillation_Unit = createUI_Label(parent, UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
   }

   private void createUI_84_MinMax_RunDyn_VerticalRatio(final Composite parent) {

      _iconRunDyn_VerticalRatio = createUI_Icon(parent, _imageRunDyn_VerticalRatio);

      _lblMinMax_RunDyn_VerticalRatio = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_RunDyn_VerticalRatio);

      _chkMin_RunDyn_VerticalRatio = createUI_Checkbox(parent);
      _spinnerMin_RunDyn_VerticalRatio = createUI_Spinner(parent, 0, RUN_DYN_VERTICAL_RATIO_MAX);

      _chkMax_RunDyn_VerticalRatio = createUI_Checkbox(parent);
      _spinnerMax_RunDyn_VerticalRatio = createUI_Spinner(parent, 0, RUN_DYN_VERTICAL_RATIO_MAX);

      _lblMinMax_RunDyn_VerticalRatio_Unit = createUI_Label(parent, UI.UNIT_PERCENT);
   }

   private void createUI_84_MinMax_Swim_Strokes(final Composite parent) {

      _iconSwim_Strokes = createUI_Icon(parent, _imageSwim_Strokes);

      _lblMinMax_Swim_Strokes = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Swim_Strokes);

      _chkMin_Swim_Strokes = createUI_Checkbox(parent);
      _spinnerMin_Swim_Strokes = createUI_Spinner(parent, 0, SWIM_STROKES_MAX);

      _chkMax_Swim_Strokes = createUI_Checkbox(parent);
      _spinnerMax_Swim_Strokes = createUI_Spinner(parent, 0, SWIM_STROKES_MAX);

      _lblMinMax_Swim_Strokes_Unit = createUI_Label(parent, GRAPH_LABEL_SWIM_STROKES);
   }

   private void createUI_86_MinMax_Swim_Swolf(final Composite parent) {

      _iconSwim_Swolf = createUI_Icon(parent, _imageSwim_Swolf);

      _lblMinMax_Swim_Swolf = createUI_Label(parent, Messages.Pref_Graphs_Checkbox_ForceValue_Swim_Swolf);

      _chkMin_Swim_Swolf = createUI_Checkbox(parent);
      _spinnerMin_Swim_Swolf = createUI_Spinner(parent, 0, SWIM_SWOLF_MAX);

      _chkMax_Swim_Swolf = createUI_Checkbox(parent);
      _spinnerMax_Swim_Swolf = createUI_Spinner(parent, 0, SWIM_SWOLF_MAX);

      _lblMinMax_Swim_Swolf_Unit = createUI_Label(parent, GRAPH_LABEL_SWIM_SWOLF);
   }

   private Button createUI_Checkbox(final Composite parent) {

      // checkbox
      final Button checkbox = new Button(parent, SWT.CHECK);
      GridDataFactory.fillDefaults()
            .indent(_columnSpacing, 0)
            .align(SWT.FILL, SWT.CENTER)
            .applyTo(checkbox);
      checkbox.addSelectionListener(_defaultSelectionListener);

      return checkbox;
   }

   private CLabel createUI_Icon(final Composite parent, final Image image) {

      final CLabel icon = new CLabel(parent, SWT.NONE);
      GridDataFactory.fillDefaults()
//				.indent(16, 0)
            .applyTo(icon);
      icon.setImage(image);

      return icon;
   }

   private Label createUI_Label(final Composite parent, final String text) {

      // label
      final Label label = new Label(parent, SWT.NONE);
      label.setText(text);

      return label;
   }

   private Spinner createUI_Spinner(final Composite parent, final int minValue, final int maxValue) {

      final Spinner spinner = new Spinner(parent, SWT.BORDER);
      GridDataFactory.fillDefaults()
            .align(SWT.FILL, SWT.FILL)
//            .grab(true, false)
            .applyTo(spinner);

      spinner.setMinimum(minValue);
      spinner.setMaximum(maxValue);

      spinner.addMouseWheelListener(_defaultMouseWheelListener);
      spinner.addSelectionListener(_defaultSelectionListener);

      return spinner;
   }

   private void enableControls() {

      _parent.setRedraw(false);

      final boolean isMinMaxEnabled = _chkEnableMinMax.getSelection();

      _lblMinValue.setEnabled(isMinMaxEnabled);
      _lblMaxValue.setEnabled(isMinMaxEnabled);

      _lblMinMax_Altimeter.setEnabled(isMinMaxEnabled);
      _lblMinMax_Altimeter_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Altitude.setEnabled(isMinMaxEnabled);
      _lblMinMax_Altitude_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Cadence.setEnabled(isMinMaxEnabled);
      _lblMinMax_Cadence_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Gradient.setEnabled(isMinMaxEnabled);
      _lblMinMax_Gradient_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Pace.setEnabled(isMinMaxEnabled);
      _lblMinMax_Pace_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Power.setEnabled(isMinMaxEnabled);
      _lblMinMax_Power_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Pulse.setEnabled(isMinMaxEnabled);
      _lblMinMax_Pulse_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Speed.setEnabled(isMinMaxEnabled);
      _lblMinMax_Speed_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Temperature.setEnabled(isMinMaxEnabled);
      _lblMinMax_Temperature_Unit.setEnabled(isMinMaxEnabled);

      _lblMinMax_RunDyn_StanceTime.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_StanceTime_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_StanceTimeBalance.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_StanceTimeBalance_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_StepLength.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_StepLength_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_VerticalOscillation.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_VerticalOscillation_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_VerticalRatio.setEnabled(isMinMaxEnabled);
      _lblMinMax_RunDyn_VerticalRatio_Unit.setEnabled(isMinMaxEnabled);

      _lblMinMax_Swim_Strokes.setEnabled(isMinMaxEnabled);
      _lblMinMax_Swim_Strokes_Unit.setEnabled(isMinMaxEnabled);
      _lblMinMax_Swim_Swolf.setEnabled(isMinMaxEnabled);
      _lblMinMax_Swim_Swolf_Unit.setEnabled(isMinMaxEnabled);

      _chkMin_Altimeter.setEnabled(isMinMaxEnabled);
      _chkMax_Altimeter.setEnabled(isMinMaxEnabled);
      _chkMin_Altitude.setEnabled(isMinMaxEnabled);
      _chkMax_Altitude.setEnabled(isMinMaxEnabled);
      _chkMin_Cadence.setEnabled(isMinMaxEnabled);
      _chkMax_Cadence.setEnabled(isMinMaxEnabled);
      _chkMin_Gradient.setEnabled(isMinMaxEnabled);
      _chkMax_Gradient.setEnabled(isMinMaxEnabled);
      _chkMin_Pace.setEnabled(isMinMaxEnabled);
      _chkMax_Pace.setEnabled(isMinMaxEnabled);
      _chkMin_Power.setEnabled(isMinMaxEnabled);
      _chkMax_Power.setEnabled(isMinMaxEnabled);
      _chkMin_Pulse.setEnabled(isMinMaxEnabled);
      _chkMax_Pulse.setEnabled(isMinMaxEnabled);
      _chkMin_Speed.setEnabled(isMinMaxEnabled);
      _chkMax_Speed.setEnabled(isMinMaxEnabled);
      _chkMin_Temperature.setEnabled(isMinMaxEnabled);
      _chkMax_Temperature.setEnabled(isMinMaxEnabled);

      _chkMin_RunDyn_StanceTime.setEnabled(isMinMaxEnabled);
      _chkMax_RunDyn_StanceTime.setEnabled(isMinMaxEnabled);
      _chkMin_RunDyn_StanceTimeBalance.setEnabled(isMinMaxEnabled);
      _chkMax_RunDyn_StanceTimeBalance.setEnabled(isMinMaxEnabled);
      _chkMin_RunDyn_StepLength.setEnabled(isMinMaxEnabled);
      _chkMax_RunDyn_StepLength.setEnabled(isMinMaxEnabled);
      _chkMin_RunDyn_VerticalOscillation.setEnabled(isMinMaxEnabled);
      _chkMax_RunDyn_VerticalOscillation.setEnabled(isMinMaxEnabled);
      _chkMin_RunDyn_VerticalRatio.setEnabled(isMinMaxEnabled);
      _chkMax_RunDyn_VerticalRatio.setEnabled(isMinMaxEnabled);

      _chkMin_Swim_Strokes.setEnabled(isMinMaxEnabled);
      _chkMax_Swim_Strokes.setEnabled(isMinMaxEnabled);
      _chkMin_Swim_Swolf.setEnabled(isMinMaxEnabled);
      _chkMax_Swim_Swolf.setEnabled(isMinMaxEnabled);

// SET_FORMATTING_OFF

		_iconAltimeter.setImage(						isMinMaxEnabled ? _imageAltimeter 	: _imageAltimeterDisabled);
		_iconAltitude.setImage(							isMinMaxEnabled ? _imageAltitude 	: _imageAltitudeDisabled);
		_iconCadence.setImage(							isMinMaxEnabled ? _imageCadence 		: _imageCadenceDisabled);
		_iconGradient.setImage(							isMinMaxEnabled ? _imageGradient 	: _imageGradientDisabled);
		_iconPace.setImage(								isMinMaxEnabled ? _imagePace 			: _imagePaceDisabled);
		_iconPower.setImage(								isMinMaxEnabled ? _imagePower 		: _imagePowerDisabled);
		_iconPulse.setImage(								isMinMaxEnabled ? _imagePulse 		: _imagePulseDisabled);
		_iconSpeed.setImage(								isMinMaxEnabled ? _imageSpeed 		: _imageSpeedDisabled);
		_iconTemperature.setImage(						isMinMaxEnabled ? _imageTemperature 					: _imageTemperatureDisabled);
		_iconRunDyn_StanceTime.setImage(				isMinMaxEnabled ? _imageRunDyn_StanceTime 			: _imageRunDyn_StanceTime_Disabled);
		_iconRunDyn_StanceTimeBalance.setImage(	isMinMaxEnabled ? _imageRunDyn_StanceTimeBalance 	: _imageRunDyn_StanceTimeBalance_Disabled);
		_iconRunDyn_StepLength.setImage(				isMinMaxEnabled ? _imageRunDyn_StepLength 			: _imageRunDyn_StepLength_Disabled);
		_iconRunDyn_VerticalOscillation.setImage(	isMinMaxEnabled ? _imageRunDyn_VerticalOscillation : _imageRunDyn_VerticalOscillation_Disabled);
		_iconRunDyn_VerticalRatio.setImage(			isMinMaxEnabled ? _imageRunDyn_VerticalRatio 		: _imageRunDyn_VerticalRatio_Disabled);
		_iconSwim_Strokes.setImage(					isMinMaxEnabled ? _imageSwim_Strokes					: _imageSwim_Strokes_Disabled);
		_iconSwim_Swolf.setImage(						isMinMaxEnabled ? _imageSwim_Swolf						: _imageSwim_Swolf_Disabled);

		_spinnerMin_Altimeter.setEnabled(			isMinMaxEnabled && _chkMin_Altimeter.getSelection());
		_spinnerMax_Altimeter.setEnabled(			isMinMaxEnabled && _chkMax_Altimeter.getSelection());
		_spinnerMin_Altitude.setEnabled(				isMinMaxEnabled && _chkMin_Altitude.getSelection());
		_spinnerMax_Altitude.setEnabled(				isMinMaxEnabled && _chkMax_Altitude.getSelection());
		_spinnerMin_Cadence.setEnabled(				isMinMaxEnabled && _chkMin_Cadence.getSelection());
		_spinnerMax_Cadence.setEnabled(				isMinMaxEnabled && _chkMax_Cadence.getSelection());
		_spinnerMin_Gradient.setEnabled(				isMinMaxEnabled && _chkMin_Gradient.getSelection());
		_spinnerMax_Gradient.setEnabled(				isMinMaxEnabled && _chkMax_Gradient.getSelection());
		_spinnerMin_Pace.setEnabled(					isMinMaxEnabled && _chkMin_Pace.getSelection());
		_spinnerMax_Pace.setEnabled(					isMinMaxEnabled && _chkMax_Pace.getSelection());
		_spinnerMin_Power.setEnabled(					isMinMaxEnabled && _chkMin_Power.getSelection());
		_spinnerMax_Power.setEnabled(					isMinMaxEnabled && _chkMax_Power.getSelection());
		_spinnerMin_Pulse.setEnabled(					isMinMaxEnabled && _chkMin_Pulse.getSelection());
		_spinnerMax_Pulse.setEnabled(					isMinMaxEnabled && _chkMax_Pulse.getSelection());
		_spinnerMin_Speed.setEnabled(					isMinMaxEnabled && _chkMin_Speed.getSelection());
		_spinnerMax_Speed.setEnabled(					isMinMaxEnabled && _chkMax_Speed.getSelection());
		_spinnerMin_Temperature.setEnabled(			isMinMaxEnabled && _chkMin_Temperature.getSelection());
		_spinnerMax_Temperature.setEnabled(			isMinMaxEnabled && _chkMax_Temperature.getSelection());

		_spinnerMin_RunDyn_StanceTime.setEnabled(				isMinMaxEnabled && _chkMin_RunDyn_StanceTime.getSelection());
		_spinnerMax_RunDyn_StanceTime.setEnabled(				isMinMaxEnabled && _chkMax_RunDyn_StanceTime.getSelection());
		_spinnerMin_RunDyn_StanceTimeBalance.setEnabled(	isMinMaxEnabled && _chkMin_RunDyn_StanceTimeBalance.getSelection());
		_spinnerMax_RunDyn_StanceTimeBalance.setEnabled(	isMinMaxEnabled && _chkMax_RunDyn_StanceTimeBalance.getSelection());
		_spinnerMin_RunDyn_StepLength.setEnabled(				isMinMaxEnabled && _chkMin_RunDyn_StepLength.getSelection());
		_spinnerMax_RunDyn_StepLength.setEnabled(				isMinMaxEnabled && _chkMax_RunDyn_StepLength.getSelection());
		_spinnerMin_RunDyn_VerticalOscillation.setEnabled(	isMinMaxEnabled && _chkMin_RunDyn_VerticalOscillation.getSelection());
		_spinnerMax_RunDyn_VerticalOscillation.setEnabled(	isMinMaxEnabled && _chkMax_RunDyn_VerticalOscillation.getSelection());
		_spinnerMin_RunDyn_VerticalRatio.setEnabled(			isMinMaxEnabled && _chkMin_RunDyn_VerticalRatio.getSelection());
		_spinnerMax_RunDyn_VerticalRatio.setEnabled(			isMinMaxEnabled && _chkMax_RunDyn_VerticalRatio.getSelection());

		_spinnerMin_Swim_Strokes.setEnabled(					isMinMaxEnabled && _chkMin_Swim_Strokes.getSelection());
		_spinnerMax_Swim_Strokes.setEnabled(					isMinMaxEnabled && _chkMax_Swim_Strokes.getSelection());
		_spinnerMin_Swim_Swolf.setEnabled(						isMinMaxEnabled && _chkMin_Swim_Swolf.getSelection());
		_spinnerMax_Swim_Swolf.setEnabled(						isMinMaxEnabled && _chkMax_Swim_Swolf.getSelection());

// SET_FORMATTING_ON

      _parent.setRedraw(true);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

// SET_FORMATTING_OFF

		_imageAltimeter 									= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Altimeter).createImage();
		_imageAltimeterDisabled 						= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Altimeter_Disabled).createImage();
		_imageAltitude 									= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Elevation).createImage();
		_imageAltitudeDisabled 							= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Elevation_Disabled).createImage();
		_imageCadence 										= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Cadence).createImage();
		_imageCadenceDisabled 							= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Cadence_Disabled).createImage();
		_imageGradient 									= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Gradient).createImage();
		_imageGradientDisabled 							= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Gradient_Disabled).createImage();
		_imagePace 											= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Pace).createImage();
		_imagePaceDisabled 								= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Pace_Disabled).createImage();
		_imagePower 										= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Power).createImage();
		_imagePowerDisabled 								= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Power_Disabled).createImage();
		_imagePulse 										= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Heartbeat).createImage();
		_imagePulseDisabled 								= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Heartbeat_Disabled).createImage();
		_imageSpeed 										= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Speed).createImage();
		_imageSpeedDisabled 								= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Speed_Disabled).createImage();
		_imageTemperature 								= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Temperature).createImage();
		_imageTemperatureDisabled 						= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Temperature_Disabled).createImage();

		_imageRunDyn_StanceTime							= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_StanceTime).createImage();
		_imageRunDyn_StanceTime_Disabled				= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_StanceTime_Disabled).createImage();
		_imageRunDyn_StanceTimeBalance				= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_StanceTimeBalance).createImage();
		_imageRunDyn_StanceTimeBalance_Disabled	= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_StanceTimeBalance_Disabled).createImage();
		_imageRunDyn_StepLength							= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_StepLength).createImage();
		_imageRunDyn_StepLength_Disabled				= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_StepLength_Disabled).createImage();
		_imageRunDyn_VerticalOscillation				= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_VerticalOscillation).createImage();
		_imageRunDyn_VerticalOscillation_Disabled	= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_VerticalOscillation_Disabled).createImage();
		_imageRunDyn_VerticalRatio						= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_VerticalRatio).createImage();
		_imageRunDyn_VerticalRatio_Disabled			= TourbookPlugin.getThemedImageDescriptor(Images.Graph_RunDyn_VerticalRatio_Disabled).createImage();

		_imageSwim_Strokes								= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Swim_Strokes).createImage();
		_imageSwim_Strokes_Disabled					= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Swim_Strokes_Disabled).createImage();
		_imageSwim_Swolf									= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Swim_Swolf).createImage();
		_imageSwim_Swolf_Disabled						= TourbookPlugin.getThemedImageDescriptor(Images.Graph_Swim_Swolf_Disabled).createImage();

// SET_FORMATTING_ON
   }

   private void onChangeUI() {

      validateMinMax();

      enableControls();

      saveState();
   }

   @Override
   public void onDispose() {

      Util.disposeResource(_imageAltimeter);
      Util.disposeResource(_imageAltimeterDisabled);
      Util.disposeResource(_imageAltitude);
      Util.disposeResource(_imageAltitudeDisabled);
      Util.disposeResource(_imageCadence);
      Util.disposeResource(_imageCadenceDisabled);
      Util.disposeResource(_imageGradient);
      Util.disposeResource(_imageGradientDisabled);
      Util.disposeResource(_imagePace);
      Util.disposeResource(_imagePaceDisabled);
      Util.disposeResource(_imagePower);
      Util.disposeResource(_imagePowerDisabled);
      Util.disposeResource(_imagePulse);
      Util.disposeResource(_imagePulseDisabled);
      Util.disposeResource(_imageSpeed);
      Util.disposeResource(_imageSpeedDisabled);
      Util.disposeResource(_imageTemperature);
      Util.disposeResource(_imageTemperatureDisabled);

      Util.disposeResource(_imageRunDyn_StanceTime);
      Util.disposeResource(_imageRunDyn_StanceTime_Disabled);
      Util.disposeResource(_imageRunDyn_StanceTimeBalance);
      Util.disposeResource(_imageRunDyn_StanceTimeBalance_Disabled);
      Util.disposeResource(_imageRunDyn_StepLength);
      Util.disposeResource(_imageRunDyn_StepLength_Disabled);
      Util.disposeResource(_imageRunDyn_VerticalOscillation);
      Util.disposeResource(_imageRunDyn_VerticalOscillation_Disabled);
      Util.disposeResource(_imageRunDyn_VerticalRatio);
      Util.disposeResource(_imageRunDyn_VerticalRatio_Disabled);

      Util.disposeResource(_imageSwim_Strokes);
      Util.disposeResource(_imageSwim_Strokes_Disabled);
      Util.disposeResource(_imageSwim_Swolf);
      Util.disposeResource(_imageSwim_Swolf_Disabled);
   }

   private void prefRestoreDefault(final Button button, final String prefName) {
      button.setSelection(_prefStore.getDefaultBoolean(prefName));
   }

   private void prefRestoreDefault(final Spinner spinner, final String prefName) {
      spinner.setSelection(_prefStore.getDefaultInt(prefName));
   }

   private void prefRestoreDefault(final Spinner spinner, final String prefName, final float measurementFactor) {

      final float prefValue = _prefStore.getDefaultInt(prefName);
      final double prefValueAdjusted = (prefValue * measurementFactor) + 0.5;
      final int uiValue = (int) prefValueAdjusted;

      spinner.setSelection(uiValue);
   }

   private void prefRestoreValue(final Button button, final String prefName) {
      button.setSelection(_prefStore.getBoolean(prefName));
   }

   private void prefRestoreValue(final Spinner spinner, final String prefName) {
      spinner.setSelection(_prefStore.getInt(prefName));
   }

   private void prefRestoreValue(final Spinner spinner, final String prefName, final float measurementFactor) {

      final float prefValue = _prefStore.getInt(prefName);
      final double prefValueAdjusted = (prefValue * measurementFactor) + 0.5;
      final int uiValue = (int) prefValueAdjusted;

      spinner.setSelection(uiValue);
   }

   private void prefSaveValue(final Button button, final String prefName) {
      _prefStore.setValue(prefName, button.getSelection());
   }

   private void prefSaveValue(final Spinner spinner, final String prefName) {
      _prefStore.setValue(prefName, spinner.getSelection());
   }

   private void prefSaveValue(final Spinner spinner, final String prefName, final float measurementFactor) {

      final float uiValue = spinner.getSelection();
      final float uiValueAdjusted = uiValue / measurementFactor;
      final int prefValue = (int) (uiValueAdjusted + 0.5f);

      _prefStore.setValue(prefName, prefValue);
   }

   @Override
   public void resetToDefaults() {

      _parent.setRedraw(false);

      _chkEnableMinMax.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED));

      // min/max altitude
      prefRestoreDefault(_chkMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE);

      // min/max pulse
      prefRestoreDefault(_chkMin_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Pulse, ITourbookPreferences.GRAPH_PULSE_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Pulse, ITourbookPreferences.GRAPH_PULSE_MAX_VALUE);

      // min/max speed
      prefRestoreDefault(_chkMin_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Speed, ITourbookPreferences.GRAPH_SPEED_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Speed, ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);

      // min/max pace
      prefRestoreDefault(_chkMin_Pace, ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Pace, ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED);
      if (_isShowPaceChartInverted) {
         prefRestoreDefault(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
         prefRestoreDefault(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
      } else {
         prefRestoreDefault(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
         prefRestoreDefault(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
      }

      // min/max cadence
      prefRestoreDefault(_chkMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE);

      // min/max gradient
      prefRestoreDefault(_chkMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);

      // min/max altimeter
      prefRestoreDefault(_chkMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

      // min/max power
      prefRestoreDefault(_chkMin_Power, ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Power, ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Power, ITourbookPreferences.GRAPH_POWER_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Power, ITourbookPreferences.GRAPH_POWER_MAX_VALUE);

      // min/max temperature
      prefRestoreDefault(_chkMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED);
      prefRestoreDefault(_chkMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED);
      prefRestoreDefault(_spinnerMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE);
      prefRestoreDefault(_spinnerMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE);

// SET_FORMATTING_OFF

		/*
		 * Running dynamics
		 */
		final float measurementDistanceXS = UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

		prefRestoreDefault(_chkMin_RunDyn_StanceTime, 					ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MIN_ENABLED);
		prefRestoreDefault(_chkMax_RunDyn_StanceTime, 					ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MAX_ENABLED);
		prefRestoreDefault(_spinnerMin_RunDyn_StanceTime, 	 		 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MIN_VALUE);
		prefRestoreDefault(_spinnerMax_RunDyn_StanceTime, 	 		 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MAX_VALUE);

		prefRestoreDefault(_chkMin_RunDyn_StanceTimeBalance, 			ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MIN_ENABLED);
		prefRestoreDefault(_chkMax_RunDyn_StanceTimeBalance, 			ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MAX_ENABLED);
		prefRestoreDefault(_spinnerMin_RunDyn_StanceTimeBalance, 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MIN_VALUE);
		prefRestoreDefault(_spinnerMax_RunDyn_StanceTimeBalance, 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MAX_VALUE);

		prefRestoreDefault(_chkMin_RunDyn_StepLength, 					ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MIN_ENABLED);
		prefRestoreDefault(_chkMax_RunDyn_StepLength, 					ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MAX_ENABLED);
		prefRestoreDefault(_spinnerMin_RunDyn_StepLength, 	 	 		ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MIN_VALUE, 				measurementDistanceXS);
		prefRestoreDefault(_spinnerMax_RunDyn_StepLength, 	 	 		ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MAX_VALUE,				measurementDistanceXS);

		prefRestoreDefault(_chkMin_RunDyn_VerticalOscillation, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MIN_ENABLED);
		prefRestoreDefault(_chkMax_RunDyn_VerticalOscillation, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MAX_ENABLED);
		prefRestoreDefault(_spinnerMin_RunDyn_VerticalOscillation, 	ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MIN_VALUE, 	measurementDistanceXS);
		prefRestoreDefault(_spinnerMax_RunDyn_VerticalOscillation, 	ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MAX_VALUE, 	measurementDistanceXS);

		prefRestoreDefault(_chkMin_RunDyn_VerticalRatio, 				ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MIN_ENABLED);
		prefRestoreDefault(_chkMax_RunDyn_VerticalRatio, 				ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MAX_ENABLED);
		prefRestoreDefault(_spinnerMin_RunDyn_VerticalRatio, 			ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MIN_VALUE);
		prefRestoreDefault(_spinnerMax_RunDyn_VerticalRatio, 			ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MAX_VALUE);

		/*
		 * Swimming
		 */
		prefRestoreDefault(_chkMin_Swim_Strokes, 							ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MIN_ENABLED);
		prefRestoreDefault(_chkMax_Swim_Strokes, 							ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MAX_ENABLED);
		prefRestoreDefault(_spinnerMin_Swim_Strokes, 					ITourbookPreferences.GRAPH_SWIM_STROKES_MIN_VALUE);
		prefRestoreDefault(_spinnerMax_Swim_Strokes, 					ITourbookPreferences.GRAPH_SWIM_STROKES_MAX_VALUE);

		prefRestoreDefault(_chkMin_Swim_Swolf, 							ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MIN_ENABLED);
		prefRestoreDefault(_chkMax_Swim_Swolf, 							ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MAX_ENABLED);
		prefRestoreDefault(_spinnerMin_Swim_Swolf, 						ITourbookPreferences.GRAPH_SWIM_SWOLF_MIN_VALUE);
		prefRestoreDefault(_spinnerMax_Swim_Swolf, 						ITourbookPreferences.GRAPH_SWIM_SWOLF_MAX_VALUE);

// SET_FORMATTING_ON

      _parent.setRedraw(true);

      onChangeUI();
   }

   private void restoreState() {

      _parent.setRedraw(false);

      _isShowPaceChartInverted = _prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED);

      _chkEnableMinMax.setSelection(_prefStore.getBoolean(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED));

      // min/max altitude
      prefRestoreValue(_chkMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE);

      // min/max pulse
      prefRestoreValue(_chkMin_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Pulse, ITourbookPreferences.GRAPH_PULSE_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Pulse, ITourbookPreferences.GRAPH_PULSE_MAX_VALUE);

      // min/max speed
      prefRestoreValue(_chkMin_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Speed, ITourbookPreferences.GRAPH_SPEED_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Speed, ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);

      // min/max pace
      prefRestoreValue(_chkMin_Pace, ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Pace, ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED);
      if (_isShowPaceChartInverted) {
         prefRestoreValue(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
         prefRestoreValue(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
      } else {
         prefRestoreValue(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
         prefRestoreValue(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
      }

      // min/max cadence
      prefRestoreValue(_chkMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE);

      // min/max gradient
      prefRestoreValue(_chkMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);

      // min/max altimeter
      prefRestoreValue(_chkMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

      // min/max power
      prefRestoreValue(_chkMin_Power, ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Power, ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Power, ITourbookPreferences.GRAPH_POWER_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Power, ITourbookPreferences.GRAPH_POWER_MAX_VALUE);

      // min/max temperature
      prefRestoreValue(_chkMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED);
      prefRestoreValue(_chkMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED);
      prefRestoreValue(_spinnerMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE);
      prefRestoreValue(_spinnerMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE);

// SET_FORMATTING_OFF

		/*
		 * Running dynamics
		 */
		final float measurementDistanceXS = UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

		prefRestoreValue(_chkMin_RunDyn_StanceTime, 					ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_RunDyn_StanceTime, 					ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_RunDyn_StanceTime, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MIN_VALUE);
		prefRestoreValue(_spinnerMax_RunDyn_StanceTime, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MAX_VALUE);

		prefRestoreValue(_chkMin_RunDyn_StanceTimeBalance, 		ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_RunDyn_StanceTimeBalance, 		ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_RunDyn_StanceTimeBalance, 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MIN_VALUE);
		prefRestoreValue(_spinnerMax_RunDyn_StanceTimeBalance, 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MAX_VALUE);

		prefRestoreValue(_chkMin_RunDyn_StepLength, 					ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_RunDyn_StepLength, 					ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_RunDyn_StepLength, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MIN_VALUE,				measurementDistanceXS);
		prefRestoreValue(_spinnerMax_RunDyn_StepLength, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MAX_VALUE,				measurementDistanceXS);

		prefRestoreValue(_chkMin_RunDyn_VerticalOscillation, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_RunDyn_VerticalOscillation, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_RunDyn_VerticalOscillation, ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MIN_VALUE,	measurementDistanceXS);
		prefRestoreValue(_spinnerMax_RunDyn_VerticalOscillation, ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MAX_VALUE,	measurementDistanceXS);

		prefRestoreValue(_chkMin_RunDyn_VerticalRatio, 				ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_RunDyn_VerticalRatio, 				ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_RunDyn_VerticalRatio, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MIN_VALUE);
		prefRestoreValue(_spinnerMax_RunDyn_VerticalRatio, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MAX_VALUE);

		/*
		 * Swimming
		 */
		prefRestoreValue(_chkMin_Swim_Strokes, 						ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Swim_Strokes, 						ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Swim_Strokes, 					ITourbookPreferences.GRAPH_SWIM_STROKES_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Swim_Strokes, 					ITourbookPreferences.GRAPH_SWIM_STROKES_MAX_VALUE);

		prefRestoreValue(_chkMin_Swim_Swolf, 							ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MIN_ENABLED);
		prefRestoreValue(_chkMax_Swim_Swolf, 							ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MAX_ENABLED);
		prefRestoreValue(_spinnerMin_Swim_Swolf, 						ITourbookPreferences.GRAPH_SWIM_SWOLF_MIN_VALUE);
		prefRestoreValue(_spinnerMax_Swim_Swolf, 						ITourbookPreferences.GRAPH_SWIM_SWOLF_MAX_VALUE);

// SET_FORMATTING_ON

      _parent.setRedraw(true);
   }

   private void saveState() {

      _prefStore.setValue(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED, _chkEnableMinMax.getSelection());

      // min/max altitude
      prefSaveValue(_chkMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE);
      prefSaveValue(_spinnerMax_Altitude, ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE);

      // min/max pulse
      prefSaveValue(_chkMin_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Pulse, ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Pulse, ITourbookPreferences.GRAPH_PULSE_MIN_VALUE);
      prefSaveValue(_spinnerMax_Pulse, ITourbookPreferences.GRAPH_PULSE_MAX_VALUE);

      // min/max speed
      prefSaveValue(_chkMin_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Speed, ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Speed, ITourbookPreferences.GRAPH_SPEED_MIN_VALUE);
      prefSaveValue(_spinnerMax_Speed, ITourbookPreferences.GRAPH_SPEED_MAX_VALUE);

      // min/max pace
      prefSaveValue(_chkMin_Pace, ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Pace, ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED);
      if (_isShowPaceChartInverted) {
         prefSaveValue(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
         prefSaveValue(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
      } else {
         prefSaveValue(_spinnerMin_Pace, ITourbookPreferences.GRAPH_PACE_MIN_VALUE);
         prefSaveValue(_spinnerMax_Pace, ITourbookPreferences.GRAPH_PACE_MAX_VALUE);
      }

      // min/max cadence
      prefSaveValue(_chkMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Cadence, ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE);
      prefSaveValue(_spinnerMax_Cadence, ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE);

      // min/max gradient
      prefSaveValue(_chkMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE);
      prefSaveValue(_spinnerMax_Gradient, ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE);

      // min/max altimeter
      prefSaveValue(_chkMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE);
      prefSaveValue(_spinnerMax_Altimeter, ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE);

      // min/max power
      prefSaveValue(_chkMin_Power, ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Power, ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Power, ITourbookPreferences.GRAPH_POWER_MIN_VALUE);
      prefSaveValue(_spinnerMax_Power, ITourbookPreferences.GRAPH_POWER_MAX_VALUE);

      // min/max temperature
      prefSaveValue(_chkMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED);
      prefSaveValue(_chkMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED);
      prefSaveValue(_spinnerMin_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE);
      prefSaveValue(_spinnerMax_Temperature, ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE);

// SET_FORMATTING_OFF

		/*
		 * Running dynamics
		 */
		final float measurementDistanceXS = UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

		prefSaveValue(_chkMin_RunDyn_StanceTime, 					ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_RunDyn_StanceTime, 					ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_RunDyn_StanceTime, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MIN_VALUE);
		prefSaveValue(_spinnerMax_RunDyn_StanceTime, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MAX_VALUE);

		prefSaveValue(_chkMin_RunDyn_StanceTimeBalance, 		ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_RunDyn_StanceTimeBalance, 		ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_RunDyn_StanceTimeBalance, 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MIN_VALUE);
		prefSaveValue(_spinnerMax_RunDyn_StanceTimeBalance, 	ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MAX_VALUE);

		prefSaveValue(_chkMin_RunDyn_StepLength, 					ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_RunDyn_StepLength, 					ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_RunDyn_StepLength, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MIN_VALUE,				measurementDistanceXS);
		prefSaveValue(_spinnerMax_RunDyn_StepLength, 	 	 	ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MAX_VALUE,				measurementDistanceXS);

		prefSaveValue(_chkMin_RunDyn_VerticalOscillation, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_RunDyn_VerticalOscillation, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_RunDyn_VerticalOscillation, ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MIN_VALUE,	measurementDistanceXS);
		prefSaveValue(_spinnerMax_RunDyn_VerticalOscillation, ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MAX_VALUE,	measurementDistanceXS);

		prefSaveValue(_chkMin_RunDyn_VerticalRatio, 				ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_RunDyn_VerticalRatio, 				ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_RunDyn_VerticalRatio, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MIN_VALUE);
		prefSaveValue(_spinnerMax_RunDyn_VerticalRatio, 		ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MAX_VALUE);


		/*
		 * Swimming
		 */
		prefSaveValue(_chkMin_Swim_Strokes, 						ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Swim_Strokes, 						ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Swim_Strokes, 					ITourbookPreferences.GRAPH_SWIM_STROKES_MIN_VALUE);
		prefSaveValue(_spinnerMax_Swim_Strokes, 					ITourbookPreferences.GRAPH_SWIM_STROKES_MAX_VALUE);

		prefSaveValue(_chkMin_Swim_Swolf, 							ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MIN_ENABLED);
		prefSaveValue(_chkMax_Swim_Swolf, 							ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MAX_ENABLED);
		prefSaveValue(_spinnerMin_Swim_Swolf, 						ITourbookPreferences.GRAPH_SWIM_SWOLF_MIN_VALUE);
		prefSaveValue(_spinnerMax_Swim_Swolf, 						ITourbookPreferences.GRAPH_SWIM_SWOLF_MAX_VALUE);

// SET_FORMATTING_ON
   }

   private void validateMinMax() {

      if (_chkEnableMinMax.getSelection() == false) {

         // min/max is disabled
         return;
      }

      if (_chkMin_Altimeter.getSelection()) {

         final int min = _spinnerMin_Altimeter.getSelection();
         final int max = _spinnerMax_Altimeter.getSelection();

         if (min >= max) {

            if (max == ALTIMETER_MAX) {
               _spinnerMin_Altimeter.setSelection(max - 1);
            } else {
               _spinnerMax_Altimeter.setSelection(min + 1);
            }
         }
      }

      if (_chkMin_Gradient.getSelection()) {

         final int min = _spinnerMin_Gradient.getSelection();
         final int max = _spinnerMax_Gradient.getSelection();

         if (min >= max) {

            if (max == GRADIENT_MAX) {
               _spinnerMin_Gradient.setSelection(max - 1);
            } else {
               _spinnerMax_Gradient.setSelection(min + 1);
            }
         }
      }

      if (_chkMin_Pace.getSelection()) {

         final int min = _spinnerMin_Pace.getSelection();
         final int max = _spinnerMax_Pace.getSelection();

         if (_isShowPaceChartInverted) {
            if (min <= max) {

               if (max == PACE_MAX) {
                  _spinnerMax_Pace.setSelection(max + 1);
               } else {
                  _spinnerMin_Pace.setSelection(min + 1);
               }
            }
         } else {
            if (min >= max) {

               if (max == PACE_MAX) {
                  _spinnerMin_Pace.setSelection(max - 1);
               } else {
                  _spinnerMax_Pace.setSelection(min + 1);
               }
            }
         }
      }

      if (_chkMin_Pulse.getSelection()) {

         final int min = _spinnerMin_Pulse.getSelection();
         final int max = _spinnerMax_Pulse.getSelection();

         if (min >= max) {

            if (max == PrefPagePeople.HEART_BEAT_MAX) {
               _spinnerMin_Pulse.setSelection(max - 1);
            } else {
               _spinnerMax_Pulse.setSelection(min + 1);
            }
         }
      }
   }

}
