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
package net.tourbook.device.garmin.fit;

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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

   private static final int      TAB_FOLDER_SPEED              = 0;
   private static final int      TAB_FOLDER_TEMPERATURE        = 1;
   private static final int      TAB_FOLDER_MARKER_FILTER      = 2;
   private static final int      TAB_FOLDER_TIME_SLIZE         = 3;
   private static final int      TAB_FOLDER_POWER              = 4;
   private static final int      TAB_FOLDER_TOURTYPE           = 5;

   private static PeriodType     _tourPeriodTemplate           = PeriodType.yearMonthDayTime()

//       // hide these components
         .withMillisRemoved();

   private static final String[] PowerDataSources              = new String[] {
         Messages.PrefPage_Fit_Combo_Power_Data_Source_Stryd,
         Messages.PrefPage_Fit_Combo_Power_Data_Source_Garmin_RD_Pod
   };

   private IPreferenceStore      _prefStore                    = Activator.getDefault().getPreferenceStore();

   private PixelConverter        _pc;

   private SelectionAdapter      _defaultSelectionListener;

   /*
    * UI controls
    */
   private Button     _chkIgnoreLastMarker;
   private Button     _chkIgnoreSpeedValues;
   private Button     _chkRemoveExceededDuration;
   private Button     _chkFitImportTourType;

   private Combo      _comboPowerDataSource;

   private Label      _lblIgnorLastMarker_Info;
   private Label      _lblIgnorLastMarker_TimeSlices;
   private Label      _lblIgnorSpeedValues_Info;
   private Label      _lblSplitTour_Duration;
   private Label      _lblSplitTour_DurationUnit;
   private Label      _lblSplitTour_Info;
   private Label      _lblPowerDataSource;

   private Button     _rdoTourTypeFromSport;
   private Button     _rdoTourTypeFromProfile;
   private Button     _rdoTourTypeFromProfileElseSport;
   private Button     _rdoTourTypeFromSportAndProfile;

   private Spinner    _spinnerIgnorLastMarker_TimeSlices;
   private Spinner    _spinnerExceededDuration;
   private Spinner    _spinnerTemperatureAdjustment;

   private CTabFolder _tabFolder;

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
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .applyTo(_tabFolder);
         {

            final CTabItem tabMeasurementSystem = new CTabItem(_tabFolder, SWT.NONE);
            tabMeasurementSystem.setControl(createUI_20_Speed(_tabFolder));
            tabMeasurementSystem.setText(Messages.PrefPage_Fit_Group_Speed);

            final CTabItem tabBreakTime = new CTabItem(_tabFolder, SWT.NONE);
            tabBreakTime.setControl(createUI_30_Temperature(_tabFolder));
            tabBreakTime.setText(Messages.PrefPage_Fit_Group_AdjustTemperature);

            final CTabItem tabElevation = new CTabItem(_tabFolder, SWT.NONE);
            tabElevation.setControl(createUI_50_IgnoreLastMarker(_tabFolder));
            tabElevation.setText(Messages.PrefPage_Fit_Group_IgnoreLastMarker);

            final CTabItem tabNotes = new CTabItem(_tabFolder, SWT.NONE);
            tabNotes.setControl(createUI_70_SplitTour(_tabFolder));
            tabNotes.setText(Messages.PrefPage_Fit_Group_ReplaceTimeSlice);

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

   private Composite createUI_20_Speed(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .fillDefaults()//
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
      GridLayoutFactory
            .fillDefaults()//
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
            GridDataFactory
                  .fillDefaults() //
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerTemperatureAdjustment);
            _spinnerTemperatureAdjustment.setDigits(1);
            _spinnerTemperatureAdjustment.setPageIncrement(10);
            _spinnerTemperatureAdjustment.setMinimum(-100); // - 10.0 �C
            _spinnerTemperatureAdjustment.setMaximum(100); // +10.0 �C
            _spinnerTemperatureAdjustment.addMouseWheelListener(Util::adjustSpinnerValueOnMouseScroll);
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

         // checkbox: ignore last marker
         {
            _chkIgnoreLastMarker = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkIgnoreLastMarker);
            _chkIgnoreLastMarker.setText(Messages.PrefPage_Fit_Checkbox_IgnoreLastMarker);
            _chkIgnoreLastMarker.addSelectionListener(_defaultSelectionListener);
         }

         // label: info
         {
            _lblIgnorLastMarker_Info = createUI_InfoLabel(container, 3);
            _lblIgnorLastMarker_Info.setText(Messages.PrefPage_Fit_Label_IgnoreLastMarker_Info);
         }

         // label: ignore time slices
         {
            _lblIgnorLastMarker_TimeSlices = new Label(container, SWT.NONE);
            _lblIgnorLastMarker_TimeSlices.setText(Messages.PrefPage_Fit_Label_IgnoreLastMarker_TimeSlices);
         }

         // spinner
         {
            _spinnerIgnorLastMarker_TimeSlices = new Spinner(container, SWT.BORDER);
            GridDataFactory
                  .fillDefaults() //
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerIgnorLastMarker_TimeSlices);
            _spinnerIgnorLastMarker_TimeSlices.setMinimum(0);
            _spinnerIgnorLastMarker_TimeSlices.setMaximum(1000);
            _spinnerIgnorLastMarker_TimeSlices.setPageIncrement(10);
            _spinnerIgnorLastMarker_TimeSlices.addMouseWheelListener(Util::adjustSpinnerValueOnMouseScroll);
         }
      }

      return container;
   }

   private Composite createUI_70_SplitTour(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .fillDefaults()//
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
            GridDataFactory
                  .fillDefaults() //
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_spinnerExceededDuration);
            _spinnerExceededDuration.setMinimum(0);
            _spinnerExceededDuration.setMaximum(Integer.MAX_VALUE);
            _spinnerExceededDuration.setPageIncrement(3600); // 60*60 = 1 hour
            _spinnerExceededDuration.addMouseWheelListener(mouseEvent -> {
               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
               updateUI_SplitTour();
            });
            _spinnerExceededDuration.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  updateUI_SplitTour();
               }
            });
         }

         // label: duration in year/months/days/...
         {
            _lblSplitTour_DurationUnit = new Label(container, SWT.NONE);
            GridDataFactory
                  .fillDefaults()//
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
      GridLayoutFactory
            .fillDefaults()//
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

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .fillDefaults()//
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
            _chkFitImportTourType.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  enableControls();
               }
            });
         }

         // container: Tour Type import options
         final Composite containerTourTypeMode = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().span(2, 1).grab(true, false).indent(15, 0).applyTo(containerTourTypeMode);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTourTypeMode);
         {
            // radio: from sport name
            _rdoTourTypeFromSport = new Button(containerTourTypeMode, SWT.RADIO);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoTourTypeFromSport);
            _rdoTourTypeFromSport.setText(Messages.PrefPage_Fit_Radio_TourTypeFromSport);
            _rdoTourTypeFromSport.addSelectionListener(_defaultSelectionListener);

            // radio: from profile name
            _rdoTourTypeFromProfile = new Button(containerTourTypeMode, SWT.RADIO);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoTourTypeFromProfile);
            _rdoTourTypeFromProfile.setText(Messages.PrefPage_Fit_Radio_TourTypeFromProfile);
            _rdoTourTypeFromProfile.addSelectionListener(_defaultSelectionListener);

            // radio: profile name if possible, otherwise sport name
            _rdoTourTypeFromProfileElseSport = new Button(containerTourTypeMode, SWT.RADIO);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoTourTypeFromProfileElseSport);
            _rdoTourTypeFromProfileElseSport.setText(Messages.PrefPage_Fit_Radio_TourTypeFromProfileElseSport);
            _rdoTourTypeFromProfileElseSport.addSelectionListener(_defaultSelectionListener);

            // radio: sport name and profile name
            _rdoTourTypeFromSportAndProfile = new Button(containerTourTypeMode, SWT.RADIO);
            GridDataFactory.fillDefaults().span(2, 1).applyTo(_rdoTourTypeFromSportAndProfile);
            _rdoTourTypeFromSportAndProfile.setText(Messages.PrefPage_Fit_Radio_TourTypeFromSportAndProfile);
            _rdoTourTypeFromSportAndProfile.addSelectionListener(_defaultSelectionListener);
         }
      }

      return container;
   }

   private Label createUI_InfoLabel(final Composite parent, final int horizontalSpan) {

      final Label lblInfo = new Label(parent, SWT.WRAP);
      GridDataFactory
            .fillDefaults()//
            .span(horizontalSpan, 1)
            .grab(true, false)
            .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
            .indent(0, _pc.convertVerticalDLUsToPixels(2))
            .applyTo(lblInfo);

      return lblInfo;
   }

   private void enableControls() {

      final boolean isSplitTour = _chkRemoveExceededDuration.getSelection();
      final boolean isIgnoreSpeed = _chkIgnoreSpeedValues.getSelection();
      final boolean isIgnorLastMarker = _chkIgnoreLastMarker.getSelection();
      final boolean isFitImportTourType = _chkFitImportTourType.getSelection();

      _lblIgnorSpeedValues_Info.setEnabled(isIgnoreSpeed);

      _lblIgnorLastMarker_Info.setEnabled(isIgnorLastMarker);
      _lblIgnorLastMarker_TimeSlices.setEnabled(isIgnorLastMarker);
      _spinnerIgnorLastMarker_TimeSlices.setEnabled(isIgnorLastMarker);

      _lblSplitTour_DurationUnit.setEnabled(isSplitTour);
      _lblSplitTour_Duration.setEnabled(isSplitTour);
      _lblSplitTour_Info.setEnabled(isSplitTour);
      _spinnerExceededDuration.setEnabled(isSplitTour);

      _rdoTourTypeFromSport.setEnabled(isFitImportTourType);
      _rdoTourTypeFromProfile.setEnabled(isFitImportTourType);
      _rdoTourTypeFromProfileElseSport.setEnabled(isFitImportTourType);
      _rdoTourTypeFromSportAndProfile.setEnabled(isFitImportTourType);

      updateUI_SplitTour();
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _defaultSelectionListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            enableControls();
         }
      };
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

      if (selectedTab == TAB_FOLDER_SPEED) {

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

         // Set Tour Type during FIT import
         _chkFitImportTourType.setSelection(_prefStore.getDefaultBoolean(IPreferences.FIT_IS_IMPORT_TOURTYPE));

         // Mode for Tour Type during FIT import
         final String tourTypeMode = _prefStore.getDefaultString(IPreferences.FIT_IMPORT_TOURTYPE_MODE);
         _rdoTourTypeFromSport.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT));
         _rdoTourTypeFromProfile.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_PROFILE));
         _rdoTourTypeFromProfileElseSport.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_TRYPROFILE));
         _rdoTourTypeFromSportAndProfile.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORTANDPROFILE));

      }

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
      _chkFitImportTourType.setSelection(_prefStore.getBoolean(IPreferences.FIT_IS_IMPORT_TOURTYPE));

      // Mode for Tour Type during FIT import
      final String tourTypeMode = _prefStore.getString(IPreferences.FIT_IMPORT_TOURTYPE_MODE);
      _rdoTourTypeFromSport.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT));
      _rdoTourTypeFromProfile.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_PROFILE));
      _rdoTourTypeFromProfileElseSport.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_TRYPROFILE));
      _rdoTourTypeFromSportAndProfile.setSelection(tourTypeMode.equals(IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORTANDPROFILE));

      enableControls();
   }

   private void saveState() {

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
      _prefStore.setValue(IPreferences.FIT_IS_IMPORT_TOURTYPE, _chkFitImportTourType.getSelection());

      // Mode for Tour Type during FIT import
      String tourTypeMode = _prefStore.getDefaultString(IPreferences.FIT_IMPORT_TOURTYPE_MODE);

      if (_rdoTourTypeFromSport.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORT;

      } else if (_rdoTourTypeFromProfile.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_PROFILE;

      } else if (_rdoTourTypeFromProfileElseSport.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_TRYPROFILE;

      } else if (_rdoTourTypeFromSportAndProfile.getSelection()) {
         tourTypeMode = IPreferences.FIT_IMPORT_TOURTYPE_MODE_SPORTANDPROFILE;
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
