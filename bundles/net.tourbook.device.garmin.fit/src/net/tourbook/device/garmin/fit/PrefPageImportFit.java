/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.common.UI;
import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class PrefPageImportFit extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String    ID                            = "net.tourbook.device.PrefPageFit";          //$NON-NLS-1$

   private static final String   STATE_FIT_IMPORT_SELECTED_TAB = "STATE_FIT_IMPORT_SELECTED_TAB";            //$NON-NLS-1$

   private static final String   DEGREE_CELSIUS                = "\u0394 \u00b0C";                           //$NON-NLS-1$

   private static final float    TEMPERATURE_DIGITS            = 10.0f;

   private static final int      TAB_FOLDER_COMMON             = 0;
   private static final int      TAB_FOLDER_SPEED              = 1;
   private static final int      TAB_FOLDER_TEMPERATURE        = 2;
   private static final int      TAB_FOLDER_MARKER_FILTER      = 3;
   private static final int      TAB_FOLDER_TIME_SLIZE         = 4;
   private static final int      TAB_FOLDER_POWER              = 5;
   private static final int      TAB_FOLDER_TOURTYPE           = 6;

   private static PeriodType     _tourPeriodTemplate           = PeriodType.yearMonthDayTime()

//       // hide these components
         .withMillisRemoved();

   private static final String[] PowerDataSources              = new String[] {
         Messages.PrefPage_Fit_Combo_Power_Data_Source_Stryd,
         Messages.PrefPage_Fit_Combo_Power_Data_Source_Garmin_RD_Pod
   };

   private IPreferenceStore      _prefStore                    = Activator.getDefault().getPreferenceStore();

   private PixelConverter        _pc;

   private SelectionListener     _defaultSelectionListener;

   /*
    * UI controls
    */
   private CTabFolder _tabFolder;

   private Button     _chkFitImportTourType;
   private Button     _chkIgnoreLastMarker;
   private Button     _chkIgnoreSpeedValues;
   private Button     _chkLogSensorData;
   private Button     _chkRemoveExceededDuration;
   private Button     _chkSetTourTitleFromFileName;

   private Combo      _comboPowerDataSource;

   private Label      _lblIgnorLastMarker_Info;
   private Label      _lblIgnorLastMarker_TimeSlices;
   private Label      _lblIgnorSpeedValues_Info;
   private Label      _lblSplitTour_Duration;
   private Label      _lblSplitTour_DurationUnit;
   private Label      _lblSplitTour_Info;
   private Label      _lblPowerDataSource;

   private Button     _rdoTourTypeFrom_Profile;
   private Button     _rdoTourTypeFrom_ProfileElseSport;
   private Button     _rdoTourTypeFrom_SessionSportProfileName;
   private Button     _rdoTourTypeFrom_Sport;
   private Button     _rdoTourTypeFrom_SportAndProfile;
   private Button     _rdoTourTypeFrom_SportAndSubSport;

   private Spinner    _spinnerIgnorLastMarker_TimeSlices;
   private Spinner    _spinnerExceededDuration;
   private Spinner    _spinnerTemperatureAdjustment;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

      GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
      GridLayoutFactory.fillDefaults().applyTo(parent);
      {

         _tabFolder = new CTabFolder(parent, SWT.TOP);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(_tabFolder);
         {

            final CTabItem tabCommon = new CTabItem(_tabFolder, SWT.NONE);
            tabCommon.setControl(createUI_10_Common(_tabFolder));
            tabCommon.setText(Messages.PrefPage_Fit_Group_Common);

            final CTabItem tabSpeed = new CTabItem(_tabFolder, SWT.NONE);
            tabSpeed.setControl(createUI_20_Speed(_tabFolder));
            tabSpeed.setText(Messages.PrefPage_Fit_Group_Speed);

            final CTabItem tabTemperature = new CTabItem(_tabFolder, SWT.NONE);
            tabTemperature.setControl(createUI_30_Temperature(_tabFolder));
            tabTemperature.setText(Messages.PrefPage_Fit_Group_AdjustTemperature);

            final CTabItem tabMarker = new CTabItem(_tabFolder, SWT.NONE);
            tabMarker.setControl(createUI_50_IgnoreLastMarker(_tabFolder));
            tabMarker.setText(Messages.PrefPage_Fit_Group_IgnoreLastMarker);

            final CTabItem tabTimeSlice = new CTabItem(_tabFolder, SWT.NONE);
            tabTimeSlice.setControl(createUI_70_SplitTour(_tabFolder));
            tabTimeSlice.setText(Messages.PrefPage_Fit_Group_ReplaceTimeSlice);

            final CTabItem tabPower = new CTabItem(_tabFolder, SWT.NONE);
            tabPower.setControl(createUI_80_Power(_tabFolder));
            tabPower.setText(Messages.PrefPage_Fit_Group_Power);

            final CTabItem tabTourType = new CTabItem(_tabFolder, SWT.NONE);
            tabTourType.setControl(createUI_90_TourType(_tabFolder));
            tabTourType.setText(Messages.PrefPage_Fit_Group_TourType);
         }
      }

      return _tabFolder;
   }

   private Control createUI_10_Common(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .extendedMargins(5, 5, 15, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         {
            /*
             * Set tour title from file name
             */
            _chkSetTourTitleFromFileName = new Button(container, SWT.CHECK);
            _chkSetTourTitleFromFileName.setText(Messages.PrefPage_Fit_Checkbox_SetTourTitleFromImportFileName);
            _chkSetTourTitleFromFileName.addSelectionListener(_defaultSelectionListener);
         }
         {
            /*
             * Log sensor data
             */
            _chkLogSensorData = new Button(container, SWT.CHECK);
            _chkLogSensorData.setText(Messages.PrefPage_Fit_Checkbox_LogSensorData);
            _chkLogSensorData.setToolTipText(Messages.PrefPage_Fit_Checkbox_LogSensorData_Tooltip);
            _chkLogSensorData.addSelectionListener(_defaultSelectionListener);
         }
      }

      return container;
   }

   private Composite createUI_20_Speed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .extendedMargins(5, 5, 15, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         /*
          * Marker
          */

         // checkbox: ignore last marker
         _chkIgnoreSpeedValues = new Button(container, SWT.CHECK);
         _chkIgnoreSpeedValues.setText(Messages.PrefPage_Fit_Checkbox_IgnoreSpeedValues);
         _chkIgnoreSpeedValues.addSelectionListener(_defaultSelectionListener);
      }
      {
         // label: info
         _lblIgnorSpeedValues_Info = createUI_InfoLabel(container, 3);
         _lblIgnorSpeedValues_Info.setText(Messages.PrefPage_Fit_Label_IgnoreSpeedValues_Info);
      }

      return container;
   }

   private Composite createUI_30_Temperature(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
            .extendedMargins(5, 5, 15, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         /*
          * temperature adjustment
          */

         // label:
         {
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
            label.setText(Messages.PrefPage_Fit_Label_AdjustTemperature);
         }

         // spinner: temperature adjustment
         {
            _spinnerTemperatureAdjustment = new Spinner(container, SWT.BORDER);
            _spinnerTemperatureAdjustment.setDigits(1);
            _spinnerTemperatureAdjustment.setPageIncrement(10);
            _spinnerTemperatureAdjustment.setMinimum(-100); // - 10.0 �C
            _spinnerTemperatureAdjustment.setMaximum(100); // +10.0 �C
            _spinnerTemperatureAdjustment.addMouseWheelListener(mouseEvent -> Util.adjustSpinnerValueOnMouseScroll(mouseEvent));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerTemperatureAdjustment);
         }

         // label: �C
         {
            final Label label = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
            label.setText(DEGREE_CELSIUS);
         }

         // label: info
         {
            final Label lblInfo = createUI_InfoLabel(container, 3);
            lblInfo.setText(Messages.PrefPage_Fit_Label_AdjustTemperature_Info);
         }
      }

      return container;
   }

   private Composite createUI_50_IgnoreLastMarker(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .fillDefaults()//
            .numColumns(3)
            .extendedMargins(5, 5, 15, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         /*
          * Marker
          */

         {
            // checkbox: ignore last marker

            _chkIgnoreLastMarker = new Button(container, SWT.CHECK);
            _chkIgnoreLastMarker.setText(Messages.PrefPage_Fit_Checkbox_IgnoreLastMarker);
            _chkIgnoreLastMarker.addSelectionListener(_defaultSelectionListener);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkIgnoreLastMarker);
         }
         {
            // label: info

            _lblIgnorLastMarker_Info = createUI_InfoLabel(container, 3);
            _lblIgnorLastMarker_Info.setText(Messages.PrefPage_Fit_Label_IgnoreLastMarker_Info);
         }
         {
            // label: ignore time slices

            _lblIgnorLastMarker_TimeSlices = new Label(container, SWT.NONE);
            _lblIgnorLastMarker_TimeSlices.setText(Messages.PrefPage_Fit_Label_IgnoreLastMarker_TimeSlices);
         }
         {
            // spinner

            _spinnerIgnorLastMarker_TimeSlices = new Spinner(container, SWT.BORDER);
            _spinnerIgnorLastMarker_TimeSlices.setMinimum(0);
            _spinnerIgnorLastMarker_TimeSlices.setMaximum(1000);
            _spinnerIgnorLastMarker_TimeSlices.setPageIncrement(10);
            _spinnerIgnorLastMarker_TimeSlices.addMouseWheelListener(mouseEvent -> Util.adjustSpinnerValueOnMouseScroll(mouseEvent));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerIgnorLastMarker_TimeSlices);
         }
      }

      return container;
   }

   private Composite createUI_70_SplitTour(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
            .extendedMargins(5, 5, 15, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         // checkbox
         {
            _chkRemoveExceededDuration = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkRemoveExceededDuration);
            _chkRemoveExceededDuration.setText(Messages.PrefPage_Fit_Checkbox_ReplaceTimeSlice);
            _chkRemoveExceededDuration.addSelectionListener(_defaultSelectionListener);
         }

         // label: info
         {
            _lblSplitTour_Info = createUI_InfoLabel(container, 3);
            _lblSplitTour_Info.setText(Messages.PrefPage_Fit_Label_ReplaceTimeSlice_Info);
         }

         // label: duration
         {
            _lblSplitTour_Duration = new Label(container, SWT.NONE);
            _lblSplitTour_Duration.setText(Messages.PrefPage_Fit_Label_ReplaceTimeSlice_Duration);
         }

         // spinner
         {
            _spinnerExceededDuration = new Spinner(container, SWT.BORDER);
            _spinnerExceededDuration.setMinimum(0);
            _spinnerExceededDuration.setMaximum(Integer.MAX_VALUE);
            _spinnerExceededDuration.setPageIncrement(3600); // 60*60 = 1 hour
            _spinnerExceededDuration.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               updateUI_SplitTour();
            });
            _spinnerExceededDuration.addSelectionListener(widgetSelectedAdapter(selectionEvent -> updateUI_SplitTour()));
            GridDataFactory.fillDefaults()
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerExceededDuration);
         }

         // label: duration in year/months/days/...
         {
            _lblSplitTour_DurationUnit = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(_lblSplitTour_DurationUnit);
         }
      }

      return container;
   }

   private Composite createUI_80_Power(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(3)
            .extendedMargins(5, 5, 15, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         // label: Power data source
         _lblPowerDataSource = new Label(container, SWT.NONE);
         _lblPowerDataSource.setText(Messages.PrefPage_Fit_Label_Preferred_Power_Data_Source);
         /*
          * combo: Power data source
          */
         _comboPowerDataSource = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboPowerDataSource.setVisibleItemCount(2);

         /*
          * Fill-up the power data sources choices
          */
         for (final String powerDataSource : PowerDataSources) {
            _comboPowerDataSource.add(powerDataSource);
         }
      }

      return container;
   }

   private Composite createUI_90_TourType(final Composite parent) {

      final GridDataFactory gd = GridDataFactory.fillDefaults().span(2, 1);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .numColumns(1)
            .extendedMargins(5, 5, 15, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         /*
          * Set Tour Type using FIT file fields
          */

         // label: info
         {
            final Label lblFitImportTourType_Info = createUI_InfoLabel(container, 3);
            lblFitImportTourType_Info.setText(Messages.PrefPage_Fit_Label_FitImportTourType_Info);
         }

         {
            // checkbox: enable/disable setting Tour Type
            _chkFitImportTourType = new Button(container, SWT.CHECK);
            _chkFitImportTourType.setText(Messages.PrefPage_Fit_Checkbox_FitImportTourType);
            _chkFitImportTourType.addSelectionListener(widgetSelectedAdapter(selectionEvent -> enableControls()));
         }

         // container: Tour Type import options
         final Composite containerTourTypeMode = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().span(2, 1).grab(true, false).indent(15, 0).applyTo(containerTourTypeMode);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTourTypeMode);
         {
            // radio: from sport name
            _rdoTourTypeFrom_Sport = new Button(containerTourTypeMode, SWT.RADIO);
            _rdoTourTypeFrom_Sport.setText(Messages.PrefPage_Fit_Radio_TourTypeFromSport);
            _rdoTourTypeFrom_Sport.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_rdoTourTypeFrom_Sport);

            // radio: from profile name
            _rdoTourTypeFrom_Profile = new Button(containerTourTypeMode, SWT.RADIO);
            _rdoTourTypeFrom_Profile.setText(Messages.PrefPage_Fit_Radio_TourTypeFromProfile);
            _rdoTourTypeFrom_Profile.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_rdoTourTypeFrom_Profile);

            // radio: profile name if possible, otherwise sport name
            _rdoTourTypeFrom_ProfileElseSport = new Button(containerTourTypeMode, SWT.RADIO);
            _rdoTourTypeFrom_ProfileElseSport.setText(Messages.PrefPage_Fit_Radio_TourTypeFromProfileElseSport);
            _rdoTourTypeFrom_ProfileElseSport.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_rdoTourTypeFrom_ProfileElseSport);

            // radio: sport name and profile name
            _rdoTourTypeFrom_SportAndProfile = new Button(containerTourTypeMode, SWT.RADIO);
            _rdoTourTypeFrom_SportAndProfile.setText(Messages.PrefPage_Fit_Radio_TourTypeFromSportAndProfile);
            _rdoTourTypeFrom_SportAndProfile.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_rdoTourTypeFrom_SportAndProfile);

            // radio: Session Message: SportProfileName
            _rdoTourTypeFrom_SessionSportProfileName = new Button(containerTourTypeMode, SWT.RADIO);
            _rdoTourTypeFrom_SessionSportProfileName.setText(Messages.PrefPage_Fit_Radio_ProfileNameFromSession);
            _rdoTourTypeFrom_SessionSportProfileName.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_rdoTourTypeFrom_SessionSportProfileName);

            // radio: from sport name + sub sport name
            _rdoTourTypeFrom_SportAndSubSport = new Button(containerTourTypeMode, SWT.RADIO);
            _rdoTourTypeFrom_SportAndSubSport.setText(Messages.PrefPage_Fit_Radio_TourType_From_SportAndSubSport);
            _rdoTourTypeFrom_SportAndSubSport.setToolTipText(Messages.PrefPage_Fit_Radio_TourType_From_SportAndSubSport_Tooltip);
            _rdoTourTypeFrom_SportAndSubSport.addSelectionListener(_defaultSelectionListener);
            gd.applyTo(_rdoTourTypeFrom_SportAndSubSport);
         }
      }

      return container;
   }

   private Label createUI_InfoLabel(final Composite parent, final int horizontalSpan) {

      final Label lblInfo = new Label(parent, SWT.WRAP);
      GridDataFactory.fillDefaults()
            .span(horizontalSpan, 1)
            .grab(true, false)
            .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
            .indent(0, _pc.convertVerticalDLUsToPixels(2))
            .applyTo(lblInfo);

      return lblInfo;
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final boolean isSplitTour              = _chkRemoveExceededDuration.getSelection();
      final boolean isIgnoreSpeed            = _chkIgnoreSpeedValues.getSelection();
      final boolean isIgnorLastMarker        = _chkIgnoreLastMarker.getSelection();
      final boolean isFitImportTourType      = _chkFitImportTourType.getSelection();

      _lblIgnorSpeedValues_Info              .setEnabled(isIgnoreSpeed);

      _lblIgnorLastMarker_Info               .setEnabled(isIgnorLastMarker);
      _lblIgnorLastMarker_TimeSlices         .setEnabled(isIgnorLastMarker);
      _spinnerIgnorLastMarker_TimeSlices     .setEnabled(isIgnorLastMarker);

      _lblSplitTour_DurationUnit             .setEnabled(isSplitTour);
      _lblSplitTour_Duration                 .setEnabled(isSplitTour);
      _lblSplitTour_Info                     .setEnabled(isSplitTour);
      _spinnerExceededDuration               .setEnabled(isSplitTour);

      _rdoTourTypeFrom_Profile                  .setEnabled(isFitImportTourType);
      _rdoTourTypeFrom_ProfileElseSport         .setEnabled(isFitImportTourType);
      _rdoTourTypeFrom_SessionSportProfileName  .setEnabled(isFitImportTourType);
      _rdoTourTypeFrom_Sport                    .setEnabled(isFitImportTourType);
      _rdoTourTypeFrom_SportAndProfile          .setEnabled(isFitImportTourType);
      _rdoTourTypeFrom_SportAndSubSport         .setEnabled(isFitImportTourType);

// SET_FORMATTING_ON

      updateUI_SplitTour();
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = widgetSelectedAdapter(selectionEvent -> enableControls());
   }

   @Override
   public boolean okToLeave() {

      saveUIState();

      return super.okToLeave();
   }

   @Override
   public boolean performCancel() {

      saveUIState();

      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      final int selectedTab = _tabFolder.getSelectionIndex();

      if (selectedTab == TAB_FOLDER_COMMON) {

         _chkLogSensorData.setSelection(_prefStore.getDefaultBoolean(IPreferences.FIT_IS_LOG_SENSOR_VALUES));
         _chkSetTourTitleFromFileName.setSelection(_prefStore.getDefaultBoolean(IPreferences.FIT_IS_SET_TOUR_TITLE_FROM_FILE_NAME));

      } else if (selectedTab == TAB_FOLDER_SPEED) {

         _chkIgnoreSpeedValues.setSelection(_prefStore.getDefaultBoolean(IPreferences.FIT_IS_IGNORE_SPEED_VALUES));

      } else if (selectedTab == TAB_FOLDER_TEMPERATURE) {

         final float temperatureAdjustment = _prefStore.getDefaultFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);
         _spinnerTemperatureAdjustment.setSelection((int) (temperatureAdjustment * TEMPERATURE_DIGITS));

      } else if (selectedTab == TAB_FOLDER_MARKER_FILTER) {

         _chkIgnoreLastMarker.setSelection(_prefStore.getDefaultBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER));
         _spinnerIgnorLastMarker_TimeSlices.setSelection(_prefStore.getDefaultInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES));

      } else if (selectedTab == TAB_FOLDER_TIME_SLIZE) {

         _chkRemoveExceededDuration.setSelection(_prefStore.getDefaultBoolean(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE));
         _spinnerExceededDuration.setSelection(_prefStore.getDefaultInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION));

      } else if (selectedTab == TAB_FOLDER_POWER) {

         _comboPowerDataSource.select(_prefStore.getDefaultInt(IPreferences.FIT_PREFERRED_POWER_DATA_SOURCE));

      } else if (selectedTab == TAB_FOLDER_TOURTYPE) {

// SET_FORMATTING_OFF

         // Set Tour Type during FIT import
         _chkFitImportTourType                     .setSelection(_prefStore.getDefaultBoolean(IPreferences.FIT_IS_SET_TOURTYPE_DURING_IMPORT));

         // Mode for Tour Type during FIT import
         final String tourTypeMode = _prefStore.getDefaultString(IPreferences.FIT_IMPORT_TOURTYPE_MODE);
         _rdoTourTypeFrom_Profile                  .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_PROFILE));
         _rdoTourTypeFrom_ProfileElseSport         .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_TRY_PROFILE));
         _rdoTourTypeFrom_SessionSportProfileName  .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SESSION_SPORT_PROFILE_NAME));
         _rdoTourTypeFrom_Sport                    .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT));
         _rdoTourTypeFrom_SportAndProfile          .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT_AND_PROFILE));
         _rdoTourTypeFrom_SportAndSubSport         .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_LOOKUP_SPORT_AND_SUB_SPORT));
      }
// SET_FORMATTING_ON

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();
      if (isOK) {

         saveState();
      }

      return isOK;
   }

   private void restoreState() {

      // common
      _chkLogSensorData.setSelection(_prefStore.getBoolean(IPreferences.FIT_IS_LOG_SENSOR_VALUES));
      _chkSetTourTitleFromFileName.setSelection(_prefStore.getBoolean(IPreferences.FIT_IS_SET_TOUR_TITLE_FROM_FILE_NAME));

      // speed
      _chkIgnoreSpeedValues.setSelection(_prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_SPEED_VALUES));

      // temperature
      final float temperatureAdjustment = _prefStore.getFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);
      _spinnerTemperatureAdjustment.setSelection((int) (temperatureAdjustment * TEMPERATURE_DIGITS));

      // last marker
      _chkIgnoreLastMarker.setSelection(_prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER));
      _spinnerIgnorLastMarker_TimeSlices.setSelection(_prefStore.getInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES));

      // exceeded time slice
      _chkRemoveExceededDuration.setSelection(_prefStore.getBoolean(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE));
      _spinnerExceededDuration.setSelection(_prefStore.getInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION));

      // folder
      _tabFolder.setSelection(_prefStore.getInt(STATE_FIT_IMPORT_SELECTED_TAB));

      // Preferred power data source
      _comboPowerDataSource.select(_prefStore.getInt(IPreferences.FIT_PREFERRED_POWER_DATA_SOURCE));

      // Set Tour Type during FIT import
      _chkFitImportTourType.setSelection(_prefStore.getBoolean(IPreferences.FIT_IS_SET_TOURTYPE_DURING_IMPORT));

// SET_FORMATTING_OFF

      // Mode for Tour Type during FIT import
      final String tourTypeMode = _prefStore.getString(IPreferences.FIT_IMPORT_TOURTYPE_MODE);

      _rdoTourTypeFrom_Profile                  .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_PROFILE));
      _rdoTourTypeFrom_ProfileElseSport         .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_TRY_PROFILE));
      _rdoTourTypeFrom_SessionSportProfileName  .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SESSION_SPORT_PROFILE_NAME));
      _rdoTourTypeFrom_Sport                    .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT));
      _rdoTourTypeFrom_SportAndProfile          .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT_AND_PROFILE));
      _rdoTourTypeFrom_SportAndSubSport         .setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_LOOKUP_SPORT_AND_SUB_SPORT));

// SET_FORMATTING_ON

      enableControls();
   }

   private void saveState() {

      // common
      _prefStore.setValue(IPreferences.FIT_IS_LOG_SENSOR_VALUES, _chkLogSensorData.getSelection());
      _prefStore.setValue(IPreferences.FIT_IS_SET_TOUR_TITLE_FROM_FILE_NAME, _chkSetTourTitleFromFileName.getSelection());

      // speed
      _prefStore.setValue(IPreferences.FIT_IS_IGNORE_SPEED_VALUES, _chkIgnoreSpeedValues.getSelection());

      // temperature
      final float temperatureAdjustment = _spinnerTemperatureAdjustment.getSelection() / TEMPERATURE_DIGITS;
      _prefStore.setValue(IPreferences.FIT_TEMPERATURE_ADJUSTMENT, temperatureAdjustment);

      // last marker
      _prefStore.setValue(IPreferences.FIT_IS_IGNORE_LAST_MARKER, _chkIgnoreLastMarker.getSelection());
      _prefStore.setValue(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES, _spinnerIgnorLastMarker_TimeSlices.getSelection());

      // exceeded time slice
      _prefStore.setValue(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE, _chkRemoveExceededDuration.getSelection());
      _prefStore.setValue(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION, _spinnerExceededDuration.getSelection());

      // Preferred power data source
      _prefStore.setValue(IPreferences.FIT_PREFERRED_POWER_DATA_SOURCE, _comboPowerDataSource.getSelectionIndex());

      // Set Tour Type during FIT import
      _prefStore.setValue(IPreferences.FIT_IS_SET_TOURTYPE_DURING_IMPORT, _chkFitImportTourType.getSelection());

      // Mode for Tour Type during FIT import
      String tourTypeMode = _prefStore.getDefaultString(IPreferences.FIT_IMPORT_TOURTYPE_MODE);

      if (_rdoTourTypeFrom_Sport.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT;

      } else if (_rdoTourTypeFrom_Profile.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_PROFILE;

      } else if (_rdoTourTypeFrom_ProfileElseSport.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_TRY_PROFILE;

      } else if (_rdoTourTypeFrom_SportAndProfile.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT_AND_PROFILE;

      } else if (_rdoTourTypeFrom_SessionSportProfileName.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_SESSION_SPORT_PROFILE_NAME;

      } else if (_rdoTourTypeFrom_SportAndSubSport.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_LOOKUP_SPORT_AND_SUB_SPORT;
      }

      _prefStore.setValue(IPreferences.FIT_IMPORT_TOURTYPE_MODE, tourTypeMode);
   }

   private void saveUIState() {

      if (_tabFolder == null || _tabFolder.isDisposed()) {
         return;
      }

      _prefStore.setValue(STATE_FIT_IMPORT_SELECTED_TAB, _tabFolder.getSelectionIndex());
   }

   private void updateUI_SplitTour() {

      final long duration = _spinnerExceededDuration.getSelection();

      final Period tourPeriod = new Period(0, duration * 1000, _tourPeriodTemplate);

      _lblSplitTour_DurationUnit.setText(tourPeriod.toString(UI.DEFAULT_DURATION_FORMATTER_SHORT));
   }
}
