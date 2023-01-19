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
package net.tourbook.tour;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.UI;
import net.tourbook.common.font.MTFont;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.IToolTipProvider;
import net.tourbook.common.util.Util;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorValue;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.preferences.PrefPageAppearanceDisplayFormat;
import net.tourbook.statistic.StatisticView;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.Messages;
import net.tourbook.ui.action.ActionTourToolTip_EditQuick;
import net.tourbook.ui.action.ActionTourToolTip_EditTour;
import net.tourbook.ui.action.Action_ToolTip_EditPreferences;
import net.tourbook.ui.views.sensors.BatteryStatus;
import net.tourbook.ui.views.sensors.SelectionRecordingDeviceBattery;
import net.tourbook.ui.views.sensors.SelectionSensor;
import net.tourbook.ui.views.sensors.SensorChartView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IWorkbenchPart;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class TourInfoUI {

   private static final String            APP_ACTION_CLOSE_TOOLTIP = net.tourbook.common.Messages.App_Action_Close_Tooltip;

   private static final int               SHELL_MARGIN             = 5;
   private static final int               MAX_DATA_WIDTH           = 300;

   private static final String            BATTERY_FORMAT           = "... %d %%";                                                                //$NON-NLS-1$
   private static final String            GEAR_SHIFT_FORMAT        = "%d / %d";                                                                  //$NON-NLS-1$

   private static final IPreferenceStore  _prefStoreCommon         = CommonActivator.getPrefStore();

   private static final GridDataFactory   _gridDataHint_Zero       = GridDataFactory.fillDefaults().hint(0, 0);
   private static final GridDataFactory   _gridDataHint_Default    = GridDataFactory.fillDefaults().hint(SWT.DEFAULT, SWT.DEFAULT);

   private static final DateTimeFormatter _dtHistoryFormatter      = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.MEDIUM);

   private static PeriodType              _tourPeriodTemplate      = PeriodType.yearMonthDayTime()

         // hide these components
         // .withMinutesRemoved()

         .withSecondsRemoved()
         .withMillisRemoved()
//
   ;

   private final IPreferenceStore         _prefStore               = TourbookPlugin.getPrefStore();

   private final NumberFormat             _nf0                     = NumberFormat.getNumberInstance();
   private final NumberFormat             _nf1                     = NumberFormat.getInstance();
   private final NumberFormat             _nf2                     = NumberFormat.getInstance();
   private final NumberFormat             _nf3                     = NumberFormat.getInstance();

   {
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);

      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);

      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);

      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
   }

   private boolean        _hasRecordingDeviceBattery;
   private boolean        _hasDescription;
   private boolean        _hasGears;
   private boolean        _hasRunDyn;
   private boolean        _hasTags;
   private boolean        _hasTourType;
   private boolean        _hasWeather;

   private int            _defaultTextWidth;

   private int            _descriptionLineCount;
   private int            _descriptionScroll_Lines = 15;
   private int            _descriptionScroll_Height;

   /**
    * Part which fired an event
    */
   private IWorkbenchPart _part;

   /*
    * Actions
    */
   private ActionCloseTooltip             _actionCloseTooltip;
   private ActionTourToolTip_EditTour     _actionEditTour;
   private ActionTourToolTip_EditQuick    _actionEditQuick;
   private Action_ToolTip_EditPreferences _actionPrefDialog;

   private boolean                        _isActionsVisible = false;

   /**
    * When <code>true</code> then the tour info is embedded in a view and do not need the toolbar to
    * close the tooltip.
    */
   private boolean                        _isUIEmbedded;

   /**
    * Tour which is displayed in the tool tip
    */
   private TourData                       _tourData;

   private ArrayList<DeviceSensorValue>   _allSensorValuesWithData;

   private String                         _noTourTooltip    = Messages.Tour_Tooltip_Label_NoTour;

   /*
    * fields which are optionally displayed when they are not null
    */
   private ZonedDateTime    _uiDtCreated;
   private ZonedDateTime    _uiDtModified;
   private String           _uiTourTypeName;

   private IToolTipProvider _tourToolTipProvider;
   private ITourProvider    _tourProvider;

   /*
    * UI resources
    */
   private Color _bgColor;
   private Color _fgColor;

   private Font  _boldFont = JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT);

   /*
    * UI controls
    */
   private Composite        _ttContainer;
   private Composite        _lowerPartContainer;

   private Text             _txtDescription;
   private Text             _txtWeather;

   private CLabel           _lblClouds;
   private CLabel           _lblTourType_Image;

   private Label            _lblAltitudeUp;
   private Label            _lblAltitudeUpUnit;
   private Label            _lblAltitudeDown;
   private Label            _lblAltitudeDownUnit;
   private Label            _lblAvgElevationChange;
   private Label            _lblAvgElevationChangeUnit;
   private Label            _lblAvgSpeed;
   private Label            _lblAvgSpeedUnit;
   private Label            _lblAvgPace;
   private Label            _lblAvgPaceUnit;
   private Label            _lblAvgPulse;
   private Label            _lblAvgPulseUnit;
   private Label            _lblAvgCadence;
   private Label            _lblAvgCadenceUnit;
   private Label            _lblAvg_Power;
   private Label            _lblAvg_PowerUnit;
   private Label            _lblBattery_Spacer;
   private Label            _lblBattery_Start;
   private Label            _lblBattery_End;
   private Label            _lblBodyWeight;
   private Label            _lblBreakTime;
   private Label            _lblBreakTime_Unit;
   private Label            _lblCalories;
   private Label            _lblCloudsUnit;
   private Label            _lblDate;
   private Label            _lblDateTimeCreatedValue;
   private Label            _lblDateTimeModifiedValue;
   private Label            _lblDateTimeModified;
   private Label            _lblDescription;
   private Label            _lblDistance;
   private Label            _lblDistanceUnit;
   private Label            _lblGear;
   private Label            _lblGear_Spacer;
   private Label            _lblGear_GearShifts;
   private Label            _lblGear_GearShifts_Spacer;
   private Label            _lblMaxAltitude;
   private Label            _lblMaxAltitudeUnit;
   private Label            _lblMaxPace;
   private Label            _lblMaxPaceUnit;
   private Label            _lblMaxPulse;
   private Label            _lblMaxPulseUnit;
   private Label            _lblMaxSpeed;
   private Label            _lblMaxSpeedUnit;
   private Label            _lblMovingTime;
   private Label            _lblMovingTime_Unit;
   private Label            _lblElapsedTime;
   private Label            _lblElapsedTime_Unit;
   private Label            _lblPausedTime;
   private Label            _lblPausedTime_Unit;
   private Label            _lblRecordedTime;
   private Label            _lblRecordedTime_Unit;
   private Label            _lblRestPulse;
   private Label            _lblTemperature_Part1;
   private Label            _lblTemperature_Part2;
   private Label            _lblTimeZone_Value;
   private Label            _lblTimeZoneDifference;
   private Label            _lblTimeZoneDifference_Value;
   private Label            _lblTitle;
   private Label            _lblTourTags;
   private Label            _lblTourTags_Value;
   private Label            _lblTourType;
   private Label            _lblTourType_Value;
   private Label            _lblWeather;
   private Label            _lblWindSpeed;
   private Label            _lblWindSpeedUnit;
   private Label            _lblWindDirection;
   private Label            _lblWindDirectionUnit;

   private Label            _lblRunDyn_StanceTime_Min;
   private Label            _lblRunDyn_StanceTime_Min_Unit;
   private Label            _lblRunDyn_StanceTime_Max;
   private Label            _lblRunDyn_StanceTime_Max_Unit;
   private Label            _lblRunDyn_StanceTime_Avg;
   private Label            _lblRunDyn_StanceTime_Avg_Unit;
   private Label            _lblRunDyn_StanceTimeBalance_Min;
   private Label            _lblRunDyn_StanceTimeBalance_Min_Unit;
   private Label            _lblRunDyn_StanceTimeBalance_Max;
   private Label            _lblRunDyn_StanceTimeBalance_Max_Unit;
   private Label            _lblRunDyn_StanceTimeBalance_Avg;
   private Label            _lblRunDyn_StanceTimeBalance_Avg_Unit;
   private Label            _lblRunDyn_StepLength_Min;
   private Label            _lblRunDyn_StepLength_Min_Unit;
   private Label            _lblRunDyn_StepLength_Max;
   private Label            _lblRunDyn_StepLength_Max_Unit;
   private Label            _lblRunDyn_StepLength_Avg;
   private Label            _lblRunDyn_StepLength_Avg_Unit;
   private Label            _lblRunDyn_VerticalOscillation_Min;
   private Label            _lblRunDyn_VerticalOscillation_Min_Unit;
   private Label            _lblRunDyn_VerticalOscillation_Max;
   private Label            _lblRunDyn_VerticalOscillation_Max_Unit;
   private Label            _lblRunDyn_VerticalOscillation_Avg;
   private Label            _lblRunDyn_VerticalOscillation_Avg_Unit;
   private Label            _lblRunDyn_VerticalRatio_Min;
   private Label            _lblRunDyn_VerticalRatio_Min_Unit;
   private Label            _lblRunDyn_VerticalRatio_Max;
   private Label            _lblRunDyn_VerticalRatio_Max_Unit;
   private Label            _lblRunDyn_VerticalRatio_Avg;
   private Label            _lblRunDyn_VerticalRatio_Avg_Unit;

   private Link             _linkBattery;
   private ArrayList<Link>  _allSensorValue_Link;

   private ArrayList<Label> _allSensorValue_Level;
   private ArrayList<Label> _allSensorValue_Status;
   private ArrayList<Label> _allSensorValue_Voltage;

   private class ActionCloseTooltip extends Action {

      public ActionCloseTooltip() {

         super(null, IAction.AS_PUSH_BUTTON);

         setToolTipText(APP_ACTION_CLOSE_TOOLTIP);
         setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_Close));
      }

      @Override
      public void run() {
         _tourToolTipProvider.hideToolTip();
      }
   }

   /**
    * Run tour action quick edit.
    */
   public void actionQuickEditTour() {

      _actionEditQuick.run();
   }

   /**
    * @param parent
    * @param tourData
    * @param tourToolTipProvider
    * @param tourProvider
    * @return Returns the content area control
    */
   public Composite createContentArea(final Composite parent,
                                      final TourData tourData,
                                      final IToolTipProvider tourToolTipProvider,
                                      final ITourProvider tourProvider) {

      _tourData = tourData;
      _tourToolTipProvider = tourToolTipProvider;
      _tourProvider = tourProvider;

      final Display display = parent.getDisplay();

      _bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      _fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      final Set<TourTag> tourTags = _tourData.getTourTags();
      final String tourDescription = _tourData.getTourDescription();

      // date/time created/modified
      _uiDtCreated = _tourData.getDateTimeCreated();
      _uiDtModified = _tourData.getDateTimeModified();

      final TourType tourType = _tourData.getTourType();
      _uiTourTypeName = tourType == null
            ? null
            : TourDatabase.getTourTypeName(tourType.getTypeId());

      _hasDescription = tourDescription != null && tourDescription.length() > 0;
      _hasGears = _tourData.getFrontShiftCount() > 0 || _tourData.getRearShiftCount() > 0;
      _hasRecordingDeviceBattery = tourData.getBattery_Percentage_Start() != -1;
      _hasRunDyn = _tourData.isRunDynAvailable();
      _hasTags = tourTags != null && tourTags.size() > 0;
      _hasTourType = tourType != null;
      _hasWeather = _tourData.getWeather().length() > 0;

      final Composite container = createUI(parent);

// this do not help to remove flickering, first an empty tooltip window is displayed then also it's content
//      _ttContainer.setRedraw(false);

      initUI(parent);

      updateUI();
      updateUI_Layout();

      enableControls();

//      _ttContainer.setRedraw(true);

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Point defaultSpacing = LayoutConstants.getSpacing();
      final int columnSpacing = defaultSpacing.x + 30;

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
      shellContainer.setForeground(_fgColor);
      shellContainer.setBackground(_bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {
         _ttContainer = new Composite(shellContainer, SWT.NONE);
         _ttContainer.setForeground(_fgColor);
         _ttContainer.setBackground(_bgColor);
         GridLayoutFactory.fillDefaults()
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(_ttContainer);
//         _ttContainer.setBackground(UI.SYS_COLOR_GREEN);
         {
            createUI_10_UpperPart(_ttContainer);

            final Composite container = new Composite(_ttContainer, SWT.NONE);
            container.setBackground(_bgColor);
            GridDataFactory.fillDefaults().applyTo(container);

            if (_hasRunDyn) {

               GridLayoutFactory.fillDefaults()
                     .numColumns(3)
                     .spacing(columnSpacing, defaultSpacing.y)
                     .applyTo(container);
               {
                  createUI_30_Column_1(container);
                  createUI_40_Column_2(container);
                  createUI_50_Column_3(container);
               }

            } else {

               GridLayoutFactory.fillDefaults()
                     .numColumns(2)
                     .spacing(columnSpacing, defaultSpacing.y)
                     .applyTo(container);
               {
                  createUI_30_Column_1(container);
                  createUI_40_Column_2(container);
               }
            }

            createUI_90_LowerPart(_ttContainer);
            createUI_99_CreateModifyTime(_ttContainer);
         }
      }

      return shellContainer;
   }

   private void createUI_10_UpperPart(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
            .applyTo(container);
      {
         {
            /*
             * Tour type
             */
            if (_uiTourTypeName != null) {

               _lblTourType_Image = new CLabel(container, SWT.NONE);
               _lblTourType_Image.setForeground(_fgColor);
               _lblTourType_Image.setBackground(_bgColor);
               GridDataFactory.swtDefaults()
                     .align(SWT.BEGINNING, SWT.BEGINNING)
                     .applyTo(_lblTourType_Image);
            }
         }
         {
            /*
             * Title
             */
            _lblTitle = new Label(container, SWT.LEAD | SWT.WRAP);
            _lblTitle.setForeground(_fgColor);
            _lblTitle.setBackground(_bgColor);
            GridDataFactory.fillDefaults()
                  .hint(MAX_DATA_WIDTH, SWT.DEFAULT)
                  .grab(true, false)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblTitle);
            MTFont.setBannerFont(_lblTitle);
         }
         {
            /*
             * Action toolbar in the top right corner
             */
            createUI_12_Toolbar(container);
         }
         {
            /*
             * Date
             */
            _lblDate = createUI_LabelValue(container, SWT.LEAD);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_lblDate);
         }
      }
   }

   private void createUI_12_Toolbar(final Composite container) {

      if (_isUIEmbedded) {

         // spacer
         new Label(container, SWT.NONE);

      } else {

         /*
          * Create toolbar
          */
         final ToolBar toolbar = new ToolBar(container, SWT.FLAT);
         toolbar.setForeground(_fgColor);
         toolbar.setBackground(_bgColor);
         GridDataFactory.fillDefaults().applyTo(toolbar);

         final ToolBarManager tbm = new ToolBarManager(toolbar);

         /*
          * Fill toolbar
          */
         if (_isActionsVisible) {

            _actionEditTour = new ActionTourToolTip_EditTour(_tourToolTipProvider, _tourProvider);
            _actionEditQuick = new ActionTourToolTip_EditQuick(_tourToolTipProvider, _tourProvider);

            final Integer selectedTabFolder = Integer.valueOf(0);

            _actionPrefDialog = new Action_ToolTip_EditPreferences(_tourToolTipProvider,
                  Messages.Tour_Tooltip_Action_EditFormatPreferences,
                  PrefPageAppearanceDisplayFormat.ID,
                  selectedTabFolder);

            tbm.add(_actionEditTour);
            tbm.add(_actionEditQuick);
            tbm.add(_actionPrefDialog);
         }

         /**
          * The close action is ALWAYS visible, sometimes there is a bug that the tooltip do not
          * automatically close when hovering out.
          */
         _actionCloseTooltip = new ActionCloseTooltip();
         tbm.add(_actionCloseTooltip);

         tbm.update(true);
      }
   }

   private void createUI_30_Column_1(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
      {
         createUI_32_Time(container);
         createUI_34_DistanceAltitude(container);
         createUI_36_Misc(container);

         // gear data
         _lblGear_Spacer = createUI_Spacer(container);
         createUI_38_Gears(container);
      }
   }

   private void createUI_32_Time(final Composite container) {

      {
         /*
          * Elapsed time
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_ElapsedTime);

         _lblElapsedTime = createUI_LabelValue(container, SWT.TRAIL);
         _lblElapsedTime_Unit = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);

         // force this column to take the rest of the space
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_lblElapsedTime_Unit);
      }

      {
         /*
          * Recorded time
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_RecordedTime);

         _lblRecordedTime = createUI_LabelValue(container, SWT.TRAIL);
         _lblRecordedTime_Unit = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
      }

      {
         /*
          * Paused time
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_PausedTime);

         _lblPausedTime = createUI_LabelValue(container, SWT.TRAIL);
         _lblPausedTime_Unit = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
      }

      {
         /*
          * Moving time
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_MovingTime);

         _lblMovingTime = createUI_LabelValue(container, SWT.TRAIL);
         _lblMovingTime_Unit = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
      }

      {
         /*
          * Break time
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_BreakTime);

         _lblBreakTime = createUI_LabelValue(container, SWT.TRAIL);
         _lblBreakTime_Unit = createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
      }

      if (isSimpleTour()) {

         createUI_Spacer(container);

         {
            /*
             * Timezone difference
             */

            _lblTimeZoneDifference = createUI_Label(container, Messages.Tour_Tooltip_Label_TimeZoneDifference);

            // set layout that the decoration is correctly layouted
//            GridDataFactory.fillDefaults().applyTo(_lblTimeZoneDifference);

            _lblTimeZoneDifference_Value = createUI_LabelValue(container, SWT.TRAIL);

            // hour
            createUI_Label(container, Messages.Tour_Tooltip_Label_Hour);
         }
         {
            /*
             * Timezone
             */
            createUI_Label(container, Messages.Tour_Tooltip_Label_TimeZone);

            _lblTimeZone_Value = createUI_LabelValue(container, SWT.TRAIL);

            // spacer
            createUI_LabelValue(container, SWT.TRAIL);
         }
      }
   }

   private void createUI_34_DistanceAltitude(final Composite container) {

      createUI_Spacer(container);

      /*
       * distance
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_Distance);

      _lblDistance = createUI_LabelValue(container, SWT.TRAIL);
      _lblDistanceUnit = createUI_LabelValue(container, SWT.LEAD);

      /*
       * altitude up
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_AltitudeUp);

      _lblAltitudeUp = createUI_LabelValue(container, SWT.TRAIL);
      _lblAltitudeUpUnit = createUI_LabelValue(container, SWT.LEAD);

      /*
       * altitude up
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_AltitudeDown);

      _lblAltitudeDown = createUI_LabelValue(container, SWT.TRAIL);
      _lblAltitudeDownUnit = createUI_LabelValue(container, SWT.LEAD);

      /*
       * Average elevation change
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_AvgElevationChange);

      _lblAvgElevationChange = createUI_LabelValue(container, SWT.TRAIL);
      _lblAvgElevationChangeUnit = createUI_LabelValue(container, SWT.LEAD);

      createUI_Spacer(container);
   }

   private void createUI_36_Misc(final Composite container) {

      {
         /*
          * calories
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_Calories);

         _lblCalories = createUI_LabelValue(container, SWT.TRAIL);

         createUI_Label(container, Messages.Value_Unit_KCalories);
      }

      {
         /*
          * rest pulse
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_RestPulse);

         _lblRestPulse = createUI_LabelValue(container, SWT.TRAIL);

         createUI_Label(container, Messages.Value_Unit_Pulse);
      }
      {
         /*
          * Body weight
          */
         createUI_Label(container, Messages.Tour_Tooltip_Label_BodyWeight);

         _lblBodyWeight = createUI_LabelValue(container, SWT.TRAIL);

         createUI_Label(container, UI.UNIT_LABEL_WEIGHT);
      }
   }

   private void createUI_38_Gears(final Composite parent) {

      /*
       * Front/rear gear shifts
       */
      _lblGear = createUI_Label(parent, Messages.Tour_Tooltip_Label_GearShifts);

      _lblGear_GearShifts = createUI_LabelValue(parent, SWT.TRAIL);
      _lblGear_GearShifts_Spacer = createUI_LabelValue(parent, SWT.LEAD);
   }

   private void createUI_40_Column_2(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_42_Avg(container);

         createUI_Spacer(container);
         createUI_43_Max(container);

         createUI_Spacer(container);
         createUI_44_Weather(container);

         _lblBattery_Spacer = createUI_Spacer(container);
         createUI_45_Battery(container);
      }
   }

   private void createUI_42_Avg(final Composite parent) {

      /*
       * avg pulse
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPulse);

      _lblAvgPulse = createUI_LabelValue(parent, SWT.TRAIL);
      _lblAvgPulseUnit = createUI_LabelValue(parent, SWT.LEAD);

      /*
       * avg speed
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgSpeed);

      _lblAvgSpeed = createUI_LabelValue(parent, SWT.TRAIL);
      _lblAvgSpeedUnit = createUI_LabelValue(parent, SWT.LEAD);

      /*
       * avg pace
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPace);

      _lblAvgPace = createUI_LabelValue(parent, SWT.TRAIL);
      _lblAvgPaceUnit = createUI_LabelValue(parent, SWT.LEAD);

      /*
       * avg cadence
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgCadence);

      _lblAvgCadence = createUI_LabelValue(parent, SWT.TRAIL);
      _lblAvgCadenceUnit = createUI_LabelValue(parent, SWT.LEAD);

      /*
       * avg power
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_AvgPower);

      _lblAvg_Power = createUI_LabelValue(parent, SWT.TRAIL);
      _lblAvg_PowerUnit = createUI_LabelValue(parent, SWT.LEAD);
   }

   private void createUI_43_Max(final Composite container) {

      /*
       * max pulse
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_MaxPulse);

      _lblMaxPulse = createUI_LabelValue(container, SWT.TRAIL);
      _lblMaxPulseUnit = createUI_LabelValue(container, SWT.LEAD);

      /*
       * max speed
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_MaxSpeed);

      _lblMaxSpeed = createUI_LabelValue(container, SWT.TRAIL);
      _lblMaxSpeedUnit = createUI_LabelValue(container, SWT.LEAD);

      /*
       * max pace
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_MaxPace);

      _lblMaxPace = createUI_LabelValue(container, SWT.TRAIL);
      _lblMaxPaceUnit = createUI_LabelValue(container, SWT.LEAD);

      /*
       * max altitude
       */
      createUI_Label(container, Messages.Tour_Tooltip_Label_MaxAltitude);

      _lblMaxAltitude = createUI_LabelValue(container, SWT.TRAIL);
      _lblMaxAltitudeUnit = createUI_LabelValue(container, SWT.LEAD);
   }

   private void createUI_44_Weather(final Composite parent) {

      /*
       * Clouds
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_Clouds);

      // Icon: clouds
      _lblClouds = new CLabel(parent, SWT.TRAIL);
      _lblClouds.setForeground(_fgColor);
      _lblClouds.setBackground(_bgColor);
      GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).applyTo(_lblClouds);

      // text: clouds
      _lblCloudsUnit = createUI_LabelValue(parent, SWT.LEAD);
      GridDataFactory.swtDefaults().applyTo(_lblCloudsUnit);

      /*
       * Temperature
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_Temperature);

      _lblTemperature_Part1 = createUI_LabelValue(parent, SWT.TRAIL);
      _lblTemperature_Part2 = createUI_LabelValue(parent, SWT.LEAD);

      /*
       * Wind speed
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_WindSpeed);

      _lblWindSpeed = createUI_LabelValue(parent, SWT.TRAIL);
      _lblWindSpeedUnit = createUI_LabelValue(parent, SWT.LEAD);

      /*
       * Wind direction
       */
      createUI_Label(parent, Messages.Tour_Tooltip_Label_WindDirection);

      _lblWindDirection = createUI_LabelValue(parent, SWT.TRAIL);
      _lblWindDirectionUnit = createUI_LabelValue(parent, SWT.LEAD);
   }

   private void createUI_45_Battery(final Composite parent) {

      {
         /*
          * Device battery, e.g. 88...56 %
          */
         _linkBattery = createUI_Link(parent, Messages.Tour_Tooltip_Label_Battery);
         _linkBattery.setToolTipText(Messages.Tour_Tooltip_Label_Battery_Tooltip);
         _linkBattery.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_Battery()));

         _lblBattery_Start = createUI_LabelValue(parent, SWT.TRAIL);
         _lblBattery_Start.setToolTipText(Messages.Tour_Tooltip_Label_Battery_Tooltip);

         _lblBattery_End = createUI_LabelValue(parent, SWT.LEAD);
         _lblBattery_End.setToolTipText(Messages.Tour_Tooltip_Label_Battery_Tooltip);
      }
   }

   private void createUI_50_Column_3(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(3).spacing(5, 0).applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         createUI_52_RunDyn(container);
      }
   }

   private void createUI_52_RunDyn(final Composite parent) {

      {
         /*
          * Stance time
          */

         {
            /*
             * Min
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTime_Min);

            _lblRunDyn_StanceTime_Min = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StanceTime_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Max
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTime_Max);

            _lblRunDyn_StanceTime_Max = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StanceTime_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Avg
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTime_Avg);

            _lblRunDyn_StanceTime_Avg = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StanceTime_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
      }

      createUI_Spacer(parent);

      {
         /*
          * Stance Time Balance
          */

         {
            /*
             * Min
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTimeBalance_Min);

            _lblRunDyn_StanceTimeBalance_Min = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StanceTimeBalance_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Max
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTimeBalance_Max);

            _lblRunDyn_StanceTimeBalance_Max = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StanceTimeBalance_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Avg
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StanceTimeBalance_Avg);

            _lblRunDyn_StanceTimeBalance_Avg = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StanceTimeBalance_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
      }

      createUI_Spacer(parent);

      {
         /*
          * Step Length
          */

         {
            /*
             * Min
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StepLength_Min);

            _lblRunDyn_StepLength_Min = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StepLength_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Max
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StepLength_Max);

            _lblRunDyn_StepLength_Max = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StepLength_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Avg
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_StepLength_Avg);

            _lblRunDyn_StepLength_Avg = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_StepLength_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
      }

      createUI_Spacer(parent);

      {
         /*
          * Vertical Oscillation
          */

         {
            /*
             * Min
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalOscillation_Min);

            _lblRunDyn_VerticalOscillation_Min = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_VerticalOscillation_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Max
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalOscillation_Max);

            _lblRunDyn_VerticalOscillation_Max = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_VerticalOscillation_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Avg
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalOscillation_Avg);

            _lblRunDyn_VerticalOscillation_Avg = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_VerticalOscillation_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
      }

      createUI_Spacer(parent);

      {
         /*
          * Vertical Ratio
          */

         {
            /*
             * Min
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalRatio_Min);

            _lblRunDyn_VerticalRatio_Min = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_VerticalRatio_Min_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Max
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalRatio_Max);

            _lblRunDyn_VerticalRatio_Max = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_VerticalRatio_Max_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
         {
            /*
             * Avg
             */
            createUI_Label(parent, Messages.Tour_Tooltip_Label_RunDyn_VerticalRatio_Avg);

            _lblRunDyn_VerticalRatio_Avg = createUI_LabelValue(parent, SWT.TRAIL);
            _lblRunDyn_VerticalRatio_Avg_Unit = createUI_LabelValue(parent, SWT.LEAD);
         }
      }
   }

   private void createUI_90_LowerPart(final Composite parent) {

      final int numColumns = 4;

      _lowerPartContainer = new Composite(parent, SWT.NONE);
      _lowerPartContainer.setForeground(_fgColor);
      _lowerPartContainer.setBackground(_bgColor);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_lowerPartContainer);
      GridLayoutFactory.fillDefaults().numColumns(numColumns).spacing(16, 0).applyTo(_lowerPartContainer);
//      _lowerPartContainer.setBackground(UI.SYS_COLOR_CYAN);
      {

         createUI_92_SensorValues(_lowerPartContainer);

         {
            /*
             * Tour type
             */
            _lblTourType = createUI_Label(_lowerPartContainer, Messages.Tour_Tooltip_Label_TourType);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING)
                  .indent(0, 5)
                  .applyTo(_lblTourType);

            _lblTourType_Value = createUI_LabelValue(_lowerPartContainer, SWT.LEAD | SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .span(numColumns - 1, 1)
//                  .grab(true, false)
//                  .hint(MAX_DATA_WIDTH, SWT.DEFAULT)
                  .indent(0, 5)
                  .applyTo(_lblTourType_Value);
         }
         {
            /*
             * Tags
             */
            _lblTourTags = createUI_Label(_lowerPartContainer, Messages.Tour_Tooltip_Label_Tags);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(_lblTourTags);

            _lblTourTags_Value = createUI_LabelValue(_lowerPartContainer, SWT.LEAD | SWT.WRAP);
            GridDataFactory.fillDefaults()
                  .span(numColumns - 1, 1)
//                  .grab(true, false)
//                  .hint(MAX_DATA_WIDTH, SWT.DEFAULT)
                  .applyTo(_lblTourTags_Value);
         }
         {
            /*
             * Weather
             */
            _lblWeather = createUI_Label(_lowerPartContainer, Messages.Tour_Tooltip_Label_Weather);
            _lblWeather.setFont(_boldFont);
            GridDataFactory.fillDefaults()
                  .span(numColumns, 1)
                  .indent(0, 10)
                  .applyTo(_lblWeather);

            _txtWeather = new Text(_lowerPartContainer, SWT.WRAP | SWT.MULTI | SWT.READ_ONLY
//                  | SWT.BORDER
            );
            GridDataFactory.fillDefaults()
                  .span(numColumns, 1)
//                  .indent(-5, 0)
//                  .grab(true, false)
//                  .hint(_defaultTextWidth, SWT.DEFAULT)
                  .applyTo(_txtWeather);

            _txtWeather.setForeground(_fgColor);
            _txtWeather.setBackground(_bgColor);
         }
         {
            /*
             * Description
             */

            // label
            _lblDescription = createUI_Label(_lowerPartContainer, Messages.Tour_Tooltip_Label_Description);
            _lblDescription.setFont(_boldFont);
            GridDataFactory.fillDefaults()
                  .span(numColumns, 1)
                  .indent(0, 10)
                  .applyTo(_lblDescription);

            // text field
            int style = SWT.WRAP | SWT.MULTI | SWT.READ_ONLY
//                  | SWT.BORDER
            ;
            _descriptionLineCount = Util.countCharacter(_tourData.getTourDescription(), '\n');

            if (_descriptionLineCount > _descriptionScroll_Lines) {
               style |= SWT.V_SCROLL;
            }

            _txtDescription = new Text(_lowerPartContainer, style);
            GridDataFactory.fillDefaults()
                  .span(numColumns, 1)
//                  .indent(-5, 0)
//                  .grab(true, false)
//                  .hint(_defaultTextWidth, SWT.DEFAULT)
                  .applyTo(_txtDescription);

            if (_descriptionLineCount > _descriptionScroll_Lines) {
               final GridData gd = (GridData) _txtDescription.getLayoutData();
               gd.heightHint = _descriptionScroll_Height;
            }

            _txtDescription.setForeground(_fgColor);
            _txtDescription.setBackground(_bgColor);
         }
      }
   }

   private void createUI_92_SensorValues(final Composite parent) {

      /*
       * Setup sensor value data BEFORE returning, otherwise old data could cause widget dispose
       * exceptions because this instance is reused
       */
      _allSensorValuesWithData = new ArrayList<>();
      _allSensorValue_Link = new ArrayList<>();

      _allSensorValue_Level = new ArrayList<>();
      _allSensorValue_Status = new ArrayList<>();
      _allSensorValue_Voltage = new ArrayList<>();

      final Set<DeviceSensorValue> allSensorValues = _tourData.getDeviceSensorValues();
      if (allSensorValues.isEmpty()) {
         return;
      }

      // sort by sensor label
      final ArrayList<DeviceSensorValue> allSortedSensorValues = new ArrayList<>(allSensorValues);
      Collections.sort(allSortedSensorValues, (sensorValue1, sensorValue2) -> {

         return sensorValue1.getDeviceSensor().getLabel().compareTo(
               sensorValue2.getDeviceSensor().getLabel());
      });

      for (final DeviceSensorValue sensorValue : allSortedSensorValues) {

         if (sensorValue.isDataAvailable() == false) {
            continue;
         }

         final DeviceSensor sensor = sensorValue.getDeviceSensor();

         _allSensorValuesWithData.add(sensorValue);

         // sensor label/link
         final Link link = createUI_Link(parent, sensor.getLabel());
         link.setData(sensor);
         link.addSelectionListener(widgetSelectedAdapter(this::onSelect_Sensor));
         _allSensorValue_Link.add(link);

         _allSensorValue_Level.add(createUI_LabelValue(parent, SWT.LEAD));
         _allSensorValue_Voltage.add(createUI_LabelValue(parent, SWT.LEAD));
         _allSensorValue_Status.add(createUI_LabelValue(parent, SWT.LEAD));
      }
   }

   private void createUI_99_CreateModifyTime(final Composite parent) {

      if (_uiDtCreated == null && _uiDtModified == null) {
         return;
      }

      final Composite container = new Composite(parent, SWT.NONE);
      container.setForeground(_fgColor);
      container.setBackground(_bgColor);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(2)
            .equalWidth(true)
            .spacing(20, 5)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         final Composite containerCreated = new Composite(container, SWT.NONE);
         containerCreated.setForeground(_fgColor);
         containerCreated.setBackground(_bgColor);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerCreated);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerCreated);
         {
            /*
             * date/time created
             */
            createUI_Label(containerCreated, Messages.Tour_Tooltip_Label_DateTimeCreated);

            _lblDateTimeCreatedValue = createUI_LabelValue(containerCreated, SWT.LEAD);
            GridDataFactory.fillDefaults().applyTo(_lblDateTimeCreatedValue);
         }

         final Composite containerModified = new Composite(container, SWT.NONE);
         containerModified.setForeground(_fgColor);
         containerModified.setBackground(_bgColor);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerModified);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerModified);
         {
            /*
             * date/time modified
             */
            _lblDateTimeModified = createUI_Label(containerModified, Messages.Tour_Tooltip_Label_DateTimeModified);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_lblDateTimeModified);

            _lblDateTimeModifiedValue = createUI_LabelValue(containerModified, SWT.TRAIL);
            GridDataFactory.fillDefaults()
                  .align(SWT.END, SWT.FILL)
                  .applyTo(_lblDateTimeModifiedValue);
         }
      }
   }

   private Label createUI_Label(final Composite parent, final String labelText) {

      final Label label = new Label(parent, SWT.NONE);
      label.setForeground(_fgColor);
      label.setBackground(_bgColor);

      if (labelText != null) {
         label.setText(labelText);
      }

      return label;
   }

   private Label createUI_LabelValue(final Composite parent, final int style) {

      final Label label = new Label(parent, style);
      label.setForeground(_fgColor);
      label.setBackground(_bgColor);
      GridDataFactory.fillDefaults().applyTo(label);

      return label;
   }

   private Link createUI_Link(final Composite parent, final String linkText) {

      final Link link = new Link(parent, SWT.NONE);
      link.setText(UI.LINK_TAG_START + linkText + UI.LINK_TAG_END);
      link.setForeground(_fgColor);
      link.setBackground(_bgColor);

      return link;
   }

   public Composite createUI_NoData(final Composite parent) {

      final Display display = parent.getDisplay();

      final Color bgColor = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND);
      final Color fgColor = display.getSystemColor(SWT.COLOR_INFO_FOREGROUND);

      /*
       * shell container is necessary because the margins of the inner container will hide the
       * tooltip when the mouse is hovered, which is not as it should be.
       */
      final Composite shellContainer = new Composite(parent, SWT.NONE);
      shellContainer.setForeground(fgColor);
      shellContainer.setBackground(bgColor);
      GridLayoutFactory.fillDefaults().applyTo(shellContainer);
      {

         final Composite container = new Composite(shellContainer, SWT.NONE);
         container.setForeground(fgColor);
         container.setBackground(bgColor);
         GridLayoutFactory.fillDefaults()
               .margins(SHELL_MARGIN, SHELL_MARGIN)
               .applyTo(container);
         {
            final Label label = new Label(container, SWT.NONE);
            label.setText(_noTourTooltip);
            label.setForeground(fgColor);
            label.setBackground(bgColor);
         }
      }

      return shellContainer;
   }

   private Label createUI_Spacer(final Composite container) {

      // spacer
      final Label label = createUI_Label(container, null);
      GridDataFactory.fillDefaults().span(3, 1).applyTo(label);

      return label;
   }

   public void dispose() {

   }

   private void enableControls() {

      if (_isActionsVisible == false) {
         return;
      }

      final boolean isTourSaved = _tourData.isTourSaved();

      _actionEditQuick.setEnabled(isTourSaved);
      _actionEditTour.setEnabled(true);
   }

   private int getWindSpeedTextIndex(final int speed) {

      final int[] unitValueWindSpeed = IWeather.getAllWindSpeeds();

      // set speed to max index value
      int speedValueIndex = unitValueWindSpeed.length - 1;

      for (int speedIndex = 0; speedIndex < unitValueWindSpeed.length; speedIndex++) {

         final int speedMaxValue = unitValueWindSpeed[speedIndex];

         if (speed <= speedMaxValue) {
            speedValueIndex = speedIndex;
            break;
         }
      }

      return speedValueIndex;
   }

   private void initUI(final Composite parent) {

      final PixelConverter pc = new PixelConverter(parent);

      /*
       * !!! It is important that the width value is not too large otherwise empty lines (because of
       * the default width) are added below the text control when there is a lot of content
       */
      _defaultTextWidth = pc.convertWidthInCharsToPixels(75);
      _descriptionScroll_Height = pc.convertHeightInCharsToPixels(_descriptionScroll_Lines);
   }

   private boolean isSimpleTour() {

      final long elapsedTime = _tourData.getTourDeviceTime_Elapsed();

      final boolean isShortTour = elapsedTime < UI.DAY_IN_SECONDS;
      final boolean isSingleTour = !_tourData.isMultipleTours();

      return isShortTour || isSingleTour;
   }

   /**
    * Show tour in battery SoC statistic
    */
   private void onSelect_Battery() {

      Util.showView(StatisticView.ID, false);

      TourManager.fireEventWithCustomData(

            TourEventId.SELECTION_RECORDING_DEVICE_BATTERY,
            new SelectionRecordingDeviceBattery(_tourData.getTourId(), _tourData.getStartYear()),
            null);
   }

   /**
    * Show sensor in the sensor chart, e.g. to visualize the voltage or level over time
    *
    * @param selectionEvent
    */
   private void onSelect_Sensor(final SelectionEvent selectionEvent) {

      final Object linkData = selectionEvent.widget.getData();
      if (linkData instanceof DeviceSensor) {

         Util.showView(SensorChartView.ID, false);

         TourManager.fireEventWithCustomData(

               TourEventId.SELECTION_SENSOR,
               new SelectionSensor((DeviceSensor) linkData, _tourData.getTourId()),
               _part);
      }
   }

   /**
    * Enable/disable tour edit actions, actions are disabled by default
    *
    * @param isEnabled
    */
   public void setActionsEnabled(final boolean isEnabled) {
      _isActionsVisible = isEnabled;
   }

   /**
    * @param isUIEmbedded
    *           When <code>true</code> then the tour info is embedded in a view and do not need the
    *           toolbar
    *           to close the tooltip.
    */
   public void setIsUIEmbedded(final boolean isUIEmbedded) {
      _isUIEmbedded = isUIEmbedded;
   }

   /**
    * Set text for the tooltip which is displayed when a tour is not hovered.
    *
    * @param noTourTooltip
    */
   public void setNoTourTooltip(final String noTourTooltip) {
      _noTourTooltip = noTourTooltip;
   }

   public void setPart(final IWorkbenchPart part) {
      _part = part;
   }

   /**
    * Set control visible or hidden
    *
    * @param control
    * @param isVisible
    */
   private void showHideControl(final Control control, final boolean isVisible) {

      showHideControl(control, isVisible, SWT.DEFAULT, SWT.DEFAULT);
   }

   /**
    * Set control visible or hidden
    *
    * @param control
    * @param isVisible
    * @param defaultWidth
    */
   private void showHideControl(final Control control, final boolean isVisible, final int defaultWidth) {

      showHideControl(control, isVisible, defaultWidth, SWT.DEFAULT);
   }

   /**
    * Set control visible or hidden
    *
    * @param control
    * @param isVisible
    * @param defaultWidth
    * @param defaultHeight
    */
   private void showHideControl(final Control control, final boolean isVisible, final int defaultWidth, final int defaultHeight) {

      if (isVisible) {

         if (control.getLayoutData() instanceof GridData) {

            final GridData gridData = (GridData) control.getLayoutData();

            gridData.widthHint = defaultWidth;
            gridData.heightHint = defaultHeight;

         } else {

            _gridDataHint_Default.applyTo(control);
         }

         // allow tab access
         control.setVisible(true);

      } else {

         if (control.getLayoutData() instanceof GridData) {

            final GridData gridData = (GridData) control.getLayoutData();

            gridData.widthHint = 0;
            gridData.heightHint = 0;

         } else {

            _gridDataHint_Zero.applyTo(control);
         }

         // deny tab access
         control.setVisible(false);
      }
   }

   private void updateUI() {

      /*
       * Upper/lower part
       */
      if (_lblTourType_Image != null && _lblTourType_Image.isDisposed() == false) {
         _lblTourType_Image.setToolTipText(_uiTourTypeName);
         net.tourbook.ui.UI.updateUI_TourType(_tourData, _lblTourType_Image, false);
      }

      String tourTitle = _tourData.getTourTitle();
      if (tourTitle == null || tourTitle.trim().length() == 0) {

         if (_uiTourTypeName == null) {
            tourTitle = Messages.Tour_Tooltip_Label_DefaultTitle;
         } else {
            tourTitle = _uiTourTypeName;
         }
      }
      _lblTitle.setText(tourTitle);

      /*
       * Lower part container contains weather, tour type, tags and description
       */
      showHideControl(_lowerPartContainer, _hasWeather || _hasTourType || _hasTags || _hasDescription);

      /*
       * Weather
       */
      if (_hasWeather) {
         _txtWeather.setText(_tourData.getWeather());
      }
      showHideControl(_lblWeather, _hasWeather);
      showHideControl(_txtWeather, _hasWeather, _defaultTextWidth);

      /*
       * Tour type
       */
      if (_hasTourType) {
         _lblTourType_Value.setText(_tourData.getTourType().getName());
      }
      showHideControl(_lblTourType, _hasTourType);
      showHideControl(_lblTourType_Value, _hasTourType);

      /*
       * Tags
       */
      if (_hasTags) {
         net.tourbook.ui.UI.updateUI_Tags(_tourData, _lblTourTags_Value, true);
      }
      showHideControl(_lblTourTags, _hasTags);
      showHideControl(_lblTourTags_Value, _hasTags);

      /*
       * Description
       */
      if (_hasDescription) {
         _txtDescription.setText(_tourData.getTourDescription());
      }
      showHideControl(_lblDescription, _hasDescription);

      if (_descriptionLineCount > _descriptionScroll_Lines) {
         // show with vertical scrollbar
         showHideControl(_txtDescription, _hasDescription, _defaultTextWidth, _descriptionScroll_Height);
      } else {
         // vertical scrollbar is not necessary
         showHideControl(_txtDescription, _hasDescription, _defaultTextWidth);
      }

      /*
       * Column: left
       */
      final long elapsedTime = _tourData.getTourDeviceTime_Elapsed();
      final long recordedTime = _tourData.getTourDeviceTime_Recorded();
      final long pausedTime = _tourData.getTourDeviceTime_Paused();
      final long movingTime = _tourData.getTourComputedTime_Moving();
      final long breakTime = elapsedTime - movingTime;

      final ZonedDateTime zdtTourStart = _tourData.getTourStartTime();
      final ZonedDateTime zdtTourEnd = zdtTourStart.plusSeconds(elapsedTime);

      if (isSimpleTour()) {

         // < 1 day

         _lblDate.setText(String.format(
               Messages.Tour_Tooltip_Format_DateWeekTime,
               zdtTourStart.format(TimeTools.Formatter_Date_F),
               zdtTourStart.format(TimeTools.Formatter_Time_M),
               zdtTourEnd.format(TimeTools.Formatter_Time_M),
               zdtTourStart.get(TimeTools.calendarWeek.weekOfWeekBasedYear())

         ));

         // show units only when data are available
         _lblElapsedTime_Unit.setVisible(elapsedTime > 0);
         _lblRecordedTime_Unit.setVisible(recordedTime > 0);
         _lblPausedTime_Unit.setVisible(pausedTime > 0);
         _lblMovingTime_Unit.setVisible(movingTime > 0);
         _lblBreakTime_Unit.setVisible(breakTime > 0);

         _lblElapsedTime.setText(FormatManager.formatElapsedTime(elapsedTime));
         _lblRecordedTime.setText(FormatManager.formatMovingTime(recordedTime));
         _lblPausedTime.setText(FormatManager.formatPausedTime(pausedTime));
         _lblMovingTime.setText(FormatManager.formatMovingTime(movingTime));
         _lblBreakTime.setText(FormatManager.formatPausedTime(breakTime));

         /*
          * Time zone
          */
         final String tourTimeZoneId = _tourData.getTimeZoneId();
         final TourDateTime tourDateTime = _tourData.getTourDateTime();
         _lblTimeZone_Value.setText(tourTimeZoneId == null ? UI.EMPTY_STRING : tourTimeZoneId);
         _lblTimeZoneDifference_Value.setText(tourDateTime.timeZoneOffsetLabel);

         // set tooltip text
         final String defaultTimeZoneId = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID);
         final String timeZoneTooltip = NLS.bind(
               Messages.ColumnFactory_TimeZoneDifference_Tooltip,
               defaultTimeZoneId);

         _lblTimeZoneDifference.setToolTipText(timeZoneTooltip);
         _lblTimeZoneDifference_Value.setToolTipText(timeZoneTooltip);

      } else {

         // > 1 day

         _lblDate.setText(String.format(
               Messages.Tour_Tooltip_Format_HistoryDateTime,
               zdtTourStart.format(_dtHistoryFormatter),
               zdtTourEnd.format(_dtHistoryFormatter)));

         // hide labels, they are displayed with the period values
         _lblElapsedTime_Unit.setVisible(false);
         _lblRecordedTime_Unit.setVisible(false);
         _lblPausedTime_Unit.setVisible(false);
         _lblMovingTime_Unit.setVisible(false);
         _lblBreakTime_Unit.setVisible(false);

         final Period elapsedPeriod = new Period(
               _tourData.getTourStartTimeMS(),
               _tourData.getTourEndTimeMS(),
               _tourPeriodTemplate);
         final Period recordedPeriod = new Period(0, recordedTime * 1000, _tourPeriodTemplate);
         final Period pausedPeriod = new Period(0, pausedTime * 1000, _tourPeriodTemplate);
         final Period movingPeriod = new Period(0, movingTime * 1000, _tourPeriodTemplate);
         final Period breakPeriod = new Period(0, breakTime * 1000, _tourPeriodTemplate);

         _lblElapsedTime.setText(elapsedPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
         _lblRecordedTime.setText(recordedPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
         _lblPausedTime.setText(pausedPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
         _lblMovingTime.setText(movingPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
         _lblBreakTime.setText(breakPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
      }

      int windSpeed = _tourData.getWeather_Wind_Speed();
      windSpeed = (int) (windSpeed / UI.UNIT_VALUE_DISTANCE);

      _lblWindSpeed.setText(Integer.toString(windSpeed));
      _lblWindSpeedUnit.setText(
            String.format(
                  Messages.Tour_Tooltip_Format_WindSpeedUnit,
                  UI.UNIT_LABEL_SPEED,
                  IWeather.windSpeedTextShort[getWindSpeedTextIndex(windSpeed)]));

      // wind direction
      final int weatherWindDirectionDegree = _tourData.getWeather_Wind_Direction();
      if (weatherWindDirectionDegree != -1) {
         _lblWindDirection.setText(Integer.toString(weatherWindDirectionDegree));
         _lblWindDirectionUnit.setText(String.format(
               Messages.Tour_Tooltip_Format_WindDirectionUnit,
               UI.getCardinalDirectionText(weatherWindDirectionDegree)));
      }

      // Average temperature
      final float temperature_NoDevice = _tourData.getWeather_Temperature_Average();
      final float temperature_FromDevice = _tourData.getWeather_Temperature_Average_Device();

      final float convertedTemperature_NoDevice = UI.convertTemperatureFromMetric(temperature_NoDevice);
      final float convertedTemperature_FromDevice = UI.convertTemperatureFromMetric(temperature_FromDevice);

      final String formattedTemperature_NoDevice = _tourData.isMultipleTours()
            ? FormatManager.formatTemperature_Summary(convertedTemperature_NoDevice)
            : FormatManager.formatTemperature(convertedTemperature_NoDevice);
      final String formattedTemperature_FromDevice = _tourData.isMultipleTours()
            ? FormatManager.formatTemperature_Summary(convertedTemperature_FromDevice)
            : FormatManager.formatTemperature(convertedTemperature_FromDevice);

      final boolean isTemperature_NoDevice = temperature_NoDevice > 0 || _tourData.isWeatherDataFromProvider();
      final boolean isTemperature_FromDevice = _tourData.temperatureSerie != null && _tourData.temperatureSerie.length > 0;

      String part1Text = UI.EMPTY_STRING;
      String part2Text = UI.EMPTY_STRING;
      String part1Tooltip = UI.EMPTY_STRING;
      String part2Tooltip = UI.EMPTY_STRING;

      if (isTemperature_NoDevice && isTemperature_FromDevice) {

         // both values are available

         part1Text = formattedTemperature_NoDevice + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE;
         part2Text = formattedTemperature_FromDevice + UI.SPACE + UI.UNIT_LABEL_TEMPERATURE;

         part1Tooltip = Messages.Tour_Tooltip_Label_AvgTemperature_NoDevice;
         part2Tooltip = Messages.Tour_Tooltip_Label_AvgTemperature_FromDevice;

      } else if (isTemperature_NoDevice) {

         // values only from provider or manual

         part1Text = formattedTemperature_NoDevice;
         part2Text = UI.UNIT_LABEL_TEMPERATURE;

         part1Tooltip = Messages.Tour_Tooltip_Label_AvgTemperature_NoDevice;
         part2Tooltip = Messages.Tour_Tooltip_Label_AvgTemperature_NoDevice;

      } else if (isTemperature_FromDevice) {

         // values only from device

         part1Text = formattedTemperature_FromDevice;
         part2Text = UI.UNIT_LABEL_TEMPERATURE;

         part1Tooltip = Messages.Tour_Tooltip_Label_AvgTemperature_FromDevice;
         part2Tooltip = Messages.Tour_Tooltip_Label_AvgTemperature_FromDevice;
      }

      _lblTemperature_Part1.setText(part1Text);
      _lblTemperature_Part1.setToolTipText(part1Tooltip);

      _lblTemperature_Part2.setText(part2Text);
      _lblTemperature_Part2.setToolTipText(part2Tooltip);

      // weather clouds
      final int weatherIndex = _tourData.getWeatherIndex();
      final String cloudText = IWeather.cloudText[weatherIndex];
      final String cloudImageName = IWeather.cloudIcon[weatherIndex];

      _lblClouds.setImage(UI.IMAGE_REGISTRY.get(cloudImageName));
      _lblCloudsUnit.setText(cloudText.equals(IWeather.cloudIsNotDefined) ? UI.EMPTY_STRING : cloudText);

      /*
       * column: right
       */
      final float distance = _tourData.getTourDistance() / UI.UNIT_VALUE_DISTANCE;

      _lblDistance.setText(FormatManager.formatDistance(distance / 1000.0));
      _lblDistanceUnit.setText(UI.UNIT_LABEL_DISTANCE);

      _lblAltitudeUp.setText(Integer.toString((int) (_tourData.getTourAltUp() / UI.UNIT_VALUE_ELEVATION)));
      _lblAltitudeUpUnit.setText(UI.UNIT_LABEL_ELEVATION);

      _lblAltitudeDown.setText(Integer.toString((int) (_tourData.getTourAltDown() / UI.UNIT_VALUE_ELEVATION)));
      _lblAltitudeDownUnit.setText(UI.UNIT_LABEL_ELEVATION);

      final int averageElevationChange = Math.round(UI.convertAverageElevationChangeFromMetric(_tourData.getAvgAltitudeChange()));
      _lblAvgElevationChange.setText(Integer.toString(averageElevationChange));
      _lblAvgElevationChangeUnit.setText(UI.UNIT_LABEL_ELEVATION + "/" + UI.UNIT_LABEL_DISTANCE); //$NON-NLS-1$

      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final long time = isPaceAndSpeedFromRecordedTime ? recordedTime : movingTime;
      final float avgSpeed = time == 0 ? 0 : 3.6f * distance / time;
      _lblAvgSpeed.setText(FormatManager.formatSpeed(avgSpeed));
      _lblAvgSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

      final int pace = (int) (distance == 0 ? 0 : (time * 1000 / distance));
      _lblAvgPace.setText(String.format(Messages.Tour_Tooltip_Format_Pace,
            pace / 60,
            pace % 60));
      _lblAvgPaceUnit.setText(UI.UNIT_LABEL_PACE);

      // avg pulse
      final double avgPulse = _tourData.getAvgPulse();
      _lblAvgPulse.setText(FormatManager.formatPulse(avgPulse));
      _lblAvgPulseUnit.setText(Messages.Value_Unit_Pulse);

      // avg cadence
      final double avgCadence = _tourData.getAvgCadence() * _tourData.getCadenceMultiplier();
      _lblAvgCadence.setText(FormatManager.formatCadence(avgCadence));
      _lblAvgCadenceUnit.setText(
            _tourData.isCadenceSpm()
                  ? Messages.Value_Unit_Cadence_Spm
                  : Messages.Value_Unit_Cadence);

      // avg power
      final double avgPower = _tourData.getPower_Avg();
      _lblAvg_Power.setText(FormatManager.formatPower(avgPower));
      _lblAvg_PowerUnit.setText(UI.UNIT_POWER);

      // calories
      final double calories = _tourData.getCalories();
      _lblCalories.setText(FormatManager.formatNumber_0(calories / 1000));

      // body
      _lblRestPulse.setText(Integer.toString(_tourData.getRestPulse()));
      final float bodyWeight = UI.convertBodyWeightFromMetric(_tourData.getBodyWeight());
      _lblBodyWeight.setText(_nf1.format(bodyWeight));

      /*
       * Max values
       */
      _lblMaxAltitude.setText(Integer.toString((int) (_tourData.getMaxAltitude() / UI.UNIT_VALUE_ELEVATION)));
      _lblMaxAltitudeUnit.setText(UI.UNIT_LABEL_ELEVATION);

      _lblMaxPulse.setText(FormatManager.formatPulse(_tourData.getMaxPulse()));
      _lblMaxPulseUnit.setText(Messages.Value_Unit_Pulse);

      _lblMaxPace.setText(UI.format_mm_ss((long) (_tourData.getMaxPace() * UI.UNIT_VALUE_DISTANCE)));
      _lblMaxPaceUnit.setText(UI.UNIT_LABEL_PACE);

      _lblMaxSpeed.setText(FormatManager.formatSpeed(_tourData.getMaxSpeed() / UI.UNIT_VALUE_DISTANCE));
      _lblMaxSpeedUnit.setText(UI.UNIT_LABEL_SPEED);

      /*
       * Gears
       */
      if (_hasGears) {

         _lblGear_GearShifts.setText(String.format(GEAR_SHIFT_FORMAT,
               _tourData.getFrontShiftCount(),
               _tourData.getRearShiftCount()));
      }

      showHideControl(_lblGear_Spacer, _hasGears);

      showHideControl(_lblGear, _hasGears);
      showHideControl(_lblGear_GearShifts, _hasGears);
      showHideControl(_lblGear_GearShifts_Spacer, _hasGears);

      /*
       * Battery
       */
      if (_hasRecordingDeviceBattery) {
         _lblBattery_Start.setText(Short.toString(_tourData.getBattery_Percentage_Start()));
         _lblBattery_End.setText(String.format(BATTERY_FORMAT, _tourData.getBattery_Percentage_End()));
      }
      showHideControl(_linkBattery, _hasRecordingDeviceBattery);
      showHideControl(_lblBattery_Spacer, _hasRecordingDeviceBattery);
      showHideControl(_lblBattery_Start, _hasRecordingDeviceBattery);
      showHideControl(_lblBattery_End, _hasRecordingDeviceBattery);

      updateUI_SensorValues();

      /*
       * Date/time
       */

      // date/time created
      if (_uiDtCreated != null) {

         _lblDateTimeCreatedValue.setText(_uiDtCreated == null
               ? UI.EMPTY_STRING
               : _uiDtCreated.format(TimeTools.Formatter_DateTime_M));
      }

      // date/time modified
      if (_uiDtModified != null) {

         _lblDateTimeModifiedValue.setText(_uiDtModified == null
               ? UI.EMPTY_STRING
               : _uiDtModified.format(TimeTools.Formatter_DateTime_M));
      }

      /*
       * Running Dynamics
       */
      if (_hasRunDyn) {

         final float mmOrInch = UI.UNIT_VALUE_DISTANCE_MM_OR_INCH;

         _lblRunDyn_StanceTime_Min.setText(Integer.toString(_tourData.getRunDyn_StanceTime_Min()));
         _lblRunDyn_StanceTime_Min_Unit.setText(UI.UNIT_MS);
         _lblRunDyn_StanceTime_Max.setText(Integer.toString(_tourData.getRunDyn_StanceTime_Max()));
         _lblRunDyn_StanceTime_Max_Unit.setText(UI.UNIT_MS);
         _lblRunDyn_StanceTime_Avg.setText(_nf0.format(_tourData.getRunDyn_StanceTime_Avg()));
         _lblRunDyn_StanceTime_Avg_Unit.setText(UI.UNIT_MS);

         _lblRunDyn_StanceTimeBalance_Min.setText(_nf1.format(_tourData.getRunDyn_StanceTimeBalance_Min()));
         _lblRunDyn_StanceTimeBalance_Min_Unit.setText(UI.SYMBOL_PERCENTAGE);
         _lblRunDyn_StanceTimeBalance_Max.setText(_nf1.format(_tourData.getRunDyn_StanceTimeBalance_Max()));
         _lblRunDyn_StanceTimeBalance_Max_Unit.setText(UI.SYMBOL_PERCENTAGE);
         _lblRunDyn_StanceTimeBalance_Avg.setText(_nf1.format(_tourData.getRunDyn_StanceTimeBalance_Avg()));
         _lblRunDyn_StanceTimeBalance_Avg_Unit.setText(UI.SYMBOL_PERCENTAGE);

         if (UI.UNIT_IS_DISTANCE_KILOMETER) {

            _lblRunDyn_StepLength_Min.setText(_nf0.format(_tourData.getRunDyn_StepLength_Min() * mmOrInch));
            _lblRunDyn_StepLength_Max.setText(_nf0.format(_tourData.getRunDyn_StepLength_Max() * mmOrInch));
            _lblRunDyn_StepLength_Avg.setText(_nf0.format(_tourData.getRunDyn_StepLength_Avg() * mmOrInch));

            _lblRunDyn_VerticalOscillation_Min.setText(_nf0.format(_tourData.getRunDyn_VerticalOscillation_Min() * mmOrInch));
            _lblRunDyn_VerticalOscillation_Max.setText(_nf0.format(_tourData.getRunDyn_VerticalOscillation_Max() * mmOrInch));
            _lblRunDyn_VerticalOscillation_Avg.setText(_nf0.format(_tourData.getRunDyn_VerticalOscillation_Avg() * mmOrInch));

         } else {

            // imperial has 1 more digit

            _lblRunDyn_StepLength_Min.setText(_nf1.format(_tourData.getRunDyn_StepLength_Min() * mmOrInch));
            _lblRunDyn_StepLength_Max.setText(_nf1.format(_tourData.getRunDyn_StepLength_Max() * mmOrInch));
            _lblRunDyn_StepLength_Avg.setText(_nf1.format(_tourData.getRunDyn_StepLength_Avg() * mmOrInch));

            _lblRunDyn_VerticalOscillation_Min.setText(_nf1.format(_tourData.getRunDyn_VerticalOscillation_Min() * mmOrInch));
            _lblRunDyn_VerticalOscillation_Max.setText(_nf1.format(_tourData.getRunDyn_VerticalOscillation_Max() * mmOrInch));
            _lblRunDyn_VerticalOscillation_Avg.setText(_nf1.format(_tourData.getRunDyn_VerticalOscillation_Avg() * mmOrInch));
         }

         _lblRunDyn_StepLength_Min_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
         _lblRunDyn_StepLength_Max_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
         _lblRunDyn_StepLength_Avg_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);

         _lblRunDyn_VerticalOscillation_Min_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
         _lblRunDyn_VerticalOscillation_Max_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);
         _lblRunDyn_VerticalOscillation_Avg_Unit.setText(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);

         _lblRunDyn_VerticalRatio_Min.setText(_nf1.format(_tourData.getRunDyn_VerticalRatio_Min()));
         _lblRunDyn_VerticalRatio_Min_Unit.setText(UI.SYMBOL_PERCENTAGE);
         _lblRunDyn_VerticalRatio_Max.setText(_nf1.format(_tourData.getRunDyn_VerticalRatio_Max()));
         _lblRunDyn_VerticalRatio_Max_Unit.setText(UI.SYMBOL_PERCENTAGE);
         _lblRunDyn_VerticalRatio_Avg.setText(_nf1.format(_tourData.getRunDyn_VerticalRatio_Avg()));
         _lblRunDyn_VerticalRatio_Avg_Unit.setText(UI.SYMBOL_PERCENTAGE);
      }
   }

   private void updateUI_Layout() {

      // compute width for all controls and equalize column width for the different sections

      _ttContainer.layout(true, true);
   }

   private void updateUI_SensorValues() {

      if (_allSensorValuesWithData == null) {
         return;
      }

      for (int sensorValueIndex = 0; sensorValueIndex < _allSensorValuesWithData.size(); sensorValueIndex++) {

         final DeviceSensorValue sensorValue = _allSensorValuesWithData.get(sensorValueIndex);

         final Label lblLevel = _allSensorValue_Level.get(sensorValueIndex);
         final Label lblStatus = _allSensorValue_Status.get(sensorValueIndex);
         final Label lblVoltage = _allSensorValue_Voltage.get(sensorValueIndex);

         final float batteryLevel_Start = sensorValue.getBatteryLevel_Start();
         final float batteryLevel_End = sensorValue.getBatteryLevel_End();
         final float batteryStatus_Start = sensorValue.getBatteryStatus_Start();
         final float batteryStatus_End = sensorValue.getBatteryStatus_End();
         final float batteryVoltage_Start = sensorValue.getBatteryVoltage_Start();
         final float batteryVoltage_End = sensorValue.getBatteryVoltage_End();

         final boolean isBatteryLevel = batteryLevel_Start != -1 || batteryLevel_End != -1;
         final boolean isBatteryStatus = batteryStatus_Start != -1 || batteryStatus_End != -1;
         final boolean isBatteryVoltage = batteryVoltage_Start != -1 || batteryVoltage_End != -1;

         if (isBatteryLevel) {

            // 77 ... 51 %

            String batteryLevel = batteryLevel_Start == batteryLevel_End

                  // don't repeat the same level
                  ? _nf0.format(batteryLevel_Start)

                  : _nf0.format(batteryLevel_Start) + UI.ELLIPSIS_WITH_SPACE + _nf0.format(batteryLevel_End);

            // add unit
            batteryLevel += UI.SPACE + UI.SYMBOL_PERCENTAGE;

            lblLevel.setText(batteryLevel);
            lblLevel.setToolTipText(Messages.Tour_Tooltip_Label_BatteryLevel_Tooltip);
         }

         if (isBatteryStatus) {

            final String statusStart_Name = BatteryStatus.getLabelFromValue((short) batteryStatus_Start);

            final String batteryStatus = batteryStatus_Start == batteryStatus_End

                  // don't repeat the same status
                  ? statusStart_Name

                  : statusStart_Name + UI.ELLIPSIS_WITH_SPACE + BatteryStatus.getLabelFromValue((short) batteryStatus_End);

            lblStatus.setText(batteryStatus);
            lblStatus.setToolTipText(Messages.Tour_Tooltip_Label_BatteryStatus_Tooltip);
         }

         if (isBatteryVoltage) {

            String batteryVoltage = batteryVoltage_Start == batteryVoltage_End

                  // don't repeat the same level
                  ? _nf2.format(batteryVoltage_Start)

                  : _nf2.format(batteryVoltage_Start) + UI.ELLIPSIS_WITH_SPACE + _nf2.format(batteryVoltage_End);

            // add unit
            batteryVoltage += UI.SPACE + UI.UNIT_VOLT;

            lblVoltage.setText(batteryVoltage);
            lblVoltage.setToolTipText(Messages.Tour_Tooltip_Label_BatteryVoltage_Tooltip);
         }

      }
   }

}
