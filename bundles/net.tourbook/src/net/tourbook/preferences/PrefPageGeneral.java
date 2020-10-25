/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.measurement_system.Distance;
import net.tourbook.common.measurement_system.Elevation;
import net.tourbook.common.measurement_system.MeasurementSystem;
import net.tourbook.common.measurement_system.MeasurementSystem_Manager;
import net.tourbook.common.measurement_system.Temperature;
import net.tourbook.common.measurement_system.Weight;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TimeZoneData;
import net.tourbook.database.TourDatabase;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageGeneral extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public static final String  ID                         = "net.tourbook.preferences.PrefPageGeneralId"; //$NON-NLS-1$

   private static final String STATE_GENERAL_SELECTED_TAB = "STATE_GENERAL_SELECTED_TAB";                 //$NON-NLS-1$

   // tab folder indices
   public static final int              TAB_FOLDER_MEASUREMENT_SYSTEM = 0;
   public static final int              TAB_FOLDER_TIME_ZONE          = 1;
   public static final int              TAB_FOLDER_CALENDAR_WEEK      = 2;

   private IPreferenceStore             _prefStore                    = TourbookPlugin.getPrefStore();
   private IPreferenceStore             _prefStoreCommon              = CommonActivator.getPrefStore();

   private String                       _timeZoneId_1;
   private String                       _timeZoneId_2;
   private String                       _timeZoneId_3;

   private int                          _backupFirstDayOfWeek;
   private int                          _backupMinimalDaysInFirstWeek;
   private int                          _currentFirstDayOfWeek;
   private int                          _currentMinimalDaysInFirstWeek;

   private boolean                      _isInUpdateUI;
   private boolean                      _isShowMeasurementSystemInUI;

   /**
    * Contains cloned profiles of the measurement systems.
    */
   private ArrayList<MeasurementSystem> _allSystemProfiles;

   private SystemDistance               _allSystemDistances[]         = {

         new SystemDistance(Distance.KILOMETER, Messages.Pref_System_Option_Distance_Kilometer),
         new SystemDistance(Distance.MILE, Messages.Pref_System_Option_Distance_Mile),
         new SystemDistance(Distance.NAUTIC_MILE, Messages.Pref_System_Option_Distance_NauticMile),
   };

   private SystemElevation              _allSystemElevations[]        = {

         new SystemElevation(Elevation.METER, Messages.Pref_System_Option_Elevation_Meter),
         new SystemElevation(Elevation.FOOT, Messages.Pref_System_Option_Elevation_Foot),
   };

   private SystemTemperature            _allSystemTemperatures[]      = {

         new SystemTemperature(Temperature.CELCIUS, Messages.Pref_System_Option_Temperature_Celcius),
         new SystemTemperature(Temperature.FAHRENHEIT, Messages.Pref_System_Option_Temperature_Fahrenheit),
   };

   private SystemWeight                 _allSystemWeights[]           = {

         new SystemWeight(Weight.KILOGRAM, Messages.Pref_System_Option_BodyWeight_Kilogram),
         new SystemWeight(Weight.POUND, Messages.Pref_System_Option_BodyWeight_Pound),
   };

   private PixelConverter               _pc;

   /*
    * UI controls
    */
   private TabFolder _tabFolder;

   // timezone
   private Button _chkTimeZone_LiveUpdate;
   private Button _chkTimeZone_UseAnotherTimeZone;

   private Combo  _comboTimeZone_1;
   private Combo  _comboTimeZone_2;
   private Combo  _comboTimeZone_3;

   private Label  _lblTimeZone_Info;

   private Button _rdoTimeZone_1;
   private Button _rdoTimeZone_2;
   private Button _rdoTimeZone_3;

   // measurement system
   private Button _chkSystem_ShowMeasurementInAppToolbar;

   private Combo  _comboSystem_Weight;
   private Combo  _comboSystem_Distance;
   private Combo  _comboSystem_Elevation;
   private Combo  _comboSystem_Profile;
   private Combo  _comboSystem_Temperature;

   // calendar week
   private Combo _comboWeek_FirstDay;
   private Combo _comboWeek_MinDaysInFirstWeek;

   // notes
   private Text _txtNotes;

   private class SystemDistance {

      private String   label;
      private Distance distance;

      public SystemDistance(final Distance distance, final String label) {
         this.distance = distance;
         this.label = label;
      }
   }

   private class SystemElevation {

      String    label;
      Elevation elevation;

      public SystemElevation(final Elevation elevation, final String label) {
         this.elevation = elevation;
         this.label = label;
      }
   }

   private class SystemTemperature {

      String      label;
      Temperature temperature;

      public SystemTemperature(final Temperature temperature, final String label) {
         this.temperature = temperature;
         this.label = label;
      }
   }

   private class SystemWeight {

      String label;
      Weight weight;

      public SystemWeight(final Weight weight, final String label) {
         this.weight = weight;
         this.label = label;
      }
   }

   /**
    * check if the user has changed calendar week and if the tour data are inconsistent
    */
   private void checkCalendarWeek() {

      if ((_backupFirstDayOfWeek != _currentFirstDayOfWeek)
            | (_backupMinimalDaysInFirstWeek != _currentMinimalDaysInFirstWeek)) {

         if (MessageDialog.openQuestion(
               Display.getCurrent().getActiveShell(),
               Messages.Pref_General_Dialog_CalendarWeekIsModified_Title,
               Messages.Pref_General_Dialog_CalendarWeekIsModified_Message)) {

            onComputeCalendarWeek();
         }
      }
   }

   @Override
   protected void createFieldEditors() {

      final Composite parent = getFieldEditorParent();

      initUI(parent);
      createUI(parent);

      restoreState();
      enableControls();
   }

   private void createUI(final Composite parent) {

      GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
      GridLayoutFactory.fillDefaults().applyTo(parent);
      {

         _tabFolder = new TabFolder(parent, SWT.TOP);
         GridDataFactory.fillDefaults()
               .grab(true, true)
               .applyTo(_tabFolder);
         {

            final TabItem tabMeasurementSystem = new TabItem(_tabFolder, SWT.NONE);
            tabMeasurementSystem.setControl(createUI_100_MeasurementSystem(_tabFolder));
            tabMeasurementSystem.setText(Messages.Pref_general_system_measurement);

            final TabItem tabBreakTime = new TabItem(_tabFolder, SWT.NONE);
            tabBreakTime.setControl(createUI_200_TimeZone(_tabFolder));
            tabBreakTime.setText(Messages.Pref_General_Group_TimeZone);

            final TabItem tabElevation = new TabItem(_tabFolder, SWT.NONE);
            tabElevation.setControl(createUI_300_WeekNumber(_tabFolder));
            tabElevation.setText(Messages.Pref_General_CalendarWeek);

            final TabItem tabNotes = new TabItem(_tabFolder, SWT.NONE);
            tabNotes.setControl(createUI_400_Notes(_tabFolder));
            tabNotes.setText(Messages.Pref_General_Notes);
         }
      }
   }

   private Composite createUI_100_MeasurementSystem(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(container);
      {

         createUI_110_MeasurementSystem(container);

         {
            /*
             * Checkbox: Show in app toolbar
             */
            _chkSystem_ShowMeasurementInAppToolbar = new Button(container, SWT.CHECK);
            _chkSystem_ShowMeasurementInAppToolbar.setText(Messages.Pref_general_show_system_in_ui);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(0, _pc.convertVerticalDLUsToPixels(8))
                  .applyTo(_chkSystem_ShowMeasurementInAppToolbar);
         }
      }

      return container;
   }

   private void createUI_110_MeasurementSystem(final Composite parent) {

      final GridDataFactory gridData_Combo = GridDataFactory.fillDefaults().grab(true, false);
      final GridDataFactory gridData_Label = GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER);

      final SelectionAdapter itemListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSystemItem_Select();
         }
      };
      final SelectionAdapter profileListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            onSystemProfile_Select();
         }
      };

      final ModifyListener modifyListener = new ModifyListener() {
         @Override
         public void modifyText(final ModifyEvent e) {
            onSystemProfile_Modify(e);
         }
      };

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.Pref_System_Group_MeasurementSystem);
      GridDataFactory.fillDefaults()
            .grab(true, false)
            .indent(0, 5)
            .applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(1).applyTo(group);
//      group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
      {
         final Composite container = new Composite(group, SWT.NONE);
         GridDataFactory.fillDefaults().grab(false, false).applyTo(container);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
         {
            {
               /*
                * Measurement system
                */

               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Pref_System_Label_System);
               gridData_Label.applyTo(label);

               // combo
               _comboSystem_Profile = new Combo(container, SWT.DROP_DOWN);
               _comboSystem_Profile.addSelectionListener(profileListener);
               _comboSystem_Profile.addModifyListener(modifyListener);
               gridData_Combo.applyTo(_comboSystem_Profile);
            }
            {
               /*
                * Distance
                */

               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Pref_System_Label_Distance);
               gridData_Label.applyTo(label);

               // combo
               _comboSystem_Distance = new Combo(container, SWT.READ_ONLY);
               _comboSystem_Distance.addSelectionListener(itemListener);
               gridData_Combo.applyTo(_comboSystem_Distance);
            }
            {
               /*
                * Elevation
                */

               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Pref_System_Label_Elevation);
               gridData_Label.applyTo(label);

               // combo
               _comboSystem_Elevation = new Combo(container, SWT.READ_ONLY);
               _comboSystem_Elevation.addSelectionListener(itemListener);
               gridData_Combo.applyTo(_comboSystem_Elevation);
            }
            {
               /*
                * Temperature
                */

               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Pref_System_Label_Temperature);
               gridData_Label.applyTo(label);

               // combo
               _comboSystem_Temperature = new Combo(container, SWT.READ_ONLY);
               _comboSystem_Temperature.addSelectionListener(itemListener);
               gridData_Combo.applyTo(_comboSystem_Temperature);
            }
            {
               /*
                * Body weight
                */

               // label
               final Label label = new Label(container, SWT.NONE);
               label.setText(Messages.Pref_System_Label_Weight);
               gridData_Label.applyTo(label);

               // combo
               _comboSystem_Weight = new Combo(container, SWT.READ_ONLY);
               _comboSystem_Weight.addSelectionListener(itemListener);
               gridData_Combo.applyTo(_comboSystem_Weight);
            }
         }
      }
   }

   private Composite createUI_200_TimeZone(final Composite parent) {

      final String defaultTimeZoneId = ZoneId.systemDefault().getId();

      final SelectionAdapter timeZoneListener = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {

            updateModel_TimeZone();
            enableControls();

            doTimeZoneLiveUpdate();
         }
      };

      final int columnIndent = 16;
      final int verticalSpacing = 5;
      final int defaultTextWidth = _pc.convertWidthInCharsToPixels(60);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory
            .fillDefaults()//
            .grab(true, true)
            .applyTo(container);
      GridLayoutFactory
            .swtDefaults()//
            .numColumns(2)
            .extendedMargins(5, 5, 10, 5)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {
         {
            /*
             * Label: Default time zone
             */
            final Label label = new Label(container, SWT.WRAP);

            label.setText(NLS.bind(Messages.Pref_General_Label_DefaultLocalTimeZone, defaultTimeZoneId));

            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .grab(true, false)
                  .indent(0, verticalSpacing)
                  .hint(defaultTextWidth, SWT.DEFAULT)
                  .applyTo(label);
         }
         {
            /*
             * Checkbox: Set time zone
             */
            _chkTimeZone_UseAnotherTimeZone = new Button(container, SWT.CHECK);
            _chkTimeZone_UseAnotherTimeZone.setText(NLS.bind(Messages.Pref_General_Checkbox_SetTimeZone, defaultTimeZoneId));
            _chkTimeZone_UseAnotherTimeZone.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .indent(0, 2 * verticalSpacing)
                  .applyTo(_chkTimeZone_UseAnotherTimeZone);
         }

         {
            /*
             * Label: Info
             */
            _lblTimeZone_Info = new Label(container, SWT.WRAP);
            _lblTimeZone_Info.setText(Messages.Pref_General_Label_SetAnotherTimeZone);

            GridDataFactory
                  .fillDefaults()//
                  .grab(true, false)
                  .span(2, 1)
                  .indent(columnIndent, verticalSpacing)
                  .hint(defaultTextWidth, SWT.DEFAULT)
                  .applyTo(_lblTimeZone_Info);
         }

         {
            /*
             * Time zone 1
             */
            // radio
            _rdoTimeZone_1 = new Button(container, SWT.RADIO);
            _rdoTimeZone_1.setText(Messages.Pref_General_Label_LocalTimeZone_1);
            _rdoTimeZone_1.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .indent(columnIndent, 2 * verticalSpacing)
                  .applyTo(_rdoTimeZone_1);

            // combo
            _comboTimeZone_1 = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboTimeZone_1.setVisibleItemCount(50);
            _comboTimeZone_1.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .indent(_pc.convertWidthInCharsToPixels(2), 2 * verticalSpacing)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_comboTimeZone_1);

            // fill combobox
            for (final TimeZoneData timeZone : TimeTools.getAllTimeZones()) {
               _comboTimeZone_1.add(timeZone.label);
            }
         }

         {
            /*
             * Time zone 2
             */
            // radio
            _rdoTimeZone_2 = new Button(container, SWT.RADIO);
            _rdoTimeZone_2.setText(Messages.Pref_General_Label_LocalTimeZone_2);
            _rdoTimeZone_2.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .indent(columnIndent, 0)
                  .applyTo(_rdoTimeZone_2);

            // combo
            _comboTimeZone_2 = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboTimeZone_2.setVisibleItemCount(50);
            _comboTimeZone_2.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .indent(_pc.convertWidthInCharsToPixels(2), 0)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_comboTimeZone_2);

            // fill combobox
            for (final TimeZoneData timeZone : TimeTools.getAllTimeZones()) {
               _comboTimeZone_2.add(timeZone.label);
            }
         }

         {
            /*
             * Time zone 3
             */
            // radio
            _rdoTimeZone_3 = new Button(container, SWT.RADIO);
            _rdoTimeZone_3.setText(Messages.Pref_General_Label_LocalTimeZone_3);
            _rdoTimeZone_3.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .indent(columnIndent, 0)
                  .applyTo(_rdoTimeZone_3);

            // combo
            _comboTimeZone_3 = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
            _comboTimeZone_3.setVisibleItemCount(50);
            _comboTimeZone_3.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .indent(_pc.convertWidthInCharsToPixels(2), 0)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .applyTo(_comboTimeZone_3);

            // fill combobox
            for (final TimeZoneData timeZone : TimeTools.getAllTimeZones()) {
               _comboTimeZone_3.add(timeZone.label);
            }
         }

         {
            /*
             * Checkbox: live update
             */
            _chkTimeZone_LiveUpdate = new Button(container, SWT.CHECK);
            _chkTimeZone_LiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
            _chkTimeZone_LiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
            _chkTimeZone_LiveUpdate.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  doTimeZoneLiveUpdate();
               }
            });
            GridDataFactory
                  .fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.END)
                  .span(2, 1)
                  .indent(0, 2 * verticalSpacing)
                  .applyTo(_chkTimeZone_LiveUpdate);
         }
      }

      return container;
   }

   private Composite createUI_300_WeekNumber(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .swtDefaults()//
            .numColumns(2)
            .extendedMargins(5, 5, 10, 5)
            .spacing(20, 5)
            .applyTo(container);
      {
         /*
          * first day of week
          */
         Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Pref_General_Label_FirstDayOfWeek);
         label.setToolTipText(Messages.Pref_General_Label_FirstDayOfWeek_Tooltip);

         _comboWeek_FirstDay = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboWeek_FirstDay.setVisibleItemCount(10);
         _comboWeek_FirstDay.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectCalendarWeek();
            }
         });

         // fill combo
         final int mondayValue = DayOfWeek.MONDAY.getValue();
         final String[] weekDays = TimeTools.weekDays_Full;

         for (int dayIndex = 0; dayIndex < weekDays.length; dayIndex++) {

            String weekDay = weekDays[dayIndex];

            // add iso marker
            if (dayIndex + 1 == mondayValue) {
               weekDay = weekDay + UI.DASH_WITH_SPACE + Messages.App_Label_ISO8601;
            }

            _comboWeek_FirstDay.add(weekDay);
         }

         /*
          * minimal days in first week
          */
         label = new Label(container, SWT.NONE);
         label.setText(Messages.Pref_General_Label_MinimalDaysInFirstWeek);
         label.setToolTipText(Messages.Pref_General_Label_MinimalDaysInFirstWeek_Tooltip);

         _comboWeek_MinDaysInFirstWeek = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboWeek_MinDaysInFirstWeek.setVisibleItemCount(10);
         _comboWeek_MinDaysInFirstWeek.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectCalendarWeek();
            }
         });

         // fill combo
         for (int dayIndex = 1; dayIndex < 8; dayIndex++) {

            String dayText;
            if (dayIndex == 4) {
               dayText = Integer.toString(dayIndex) + " - " + Messages.App_Label_ISO8601;// + ")"; //$NON-NLS-1$
            } else {
               dayText = Integer.toString(dayIndex);
            }
            _comboWeek_MinDaysInFirstWeek.add(dayText);
         }

         /*
          * apply settings to all tours
          */
         final Button button = new Button(container, SWT.NONE);
         button.setText(Messages.Pref_General_Button_ComputeCalendarWeek);
         button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onComputeCalendarWeek();
            }
         });
         GridDataFactory
               .fillDefaults()//
               .align(SWT.BEGINNING, SWT.FILL)
               .span(2, 1)
               .indent(0, _pc.convertHeightInCharsToPixels(2))
               .applyTo(button);
      }

      return container;
   }

   private Composite createUI_400_Notes(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .swtDefaults()//
            .extendedMargins(5, 5, 10, 5)
            .applyTo(container);
      {
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Pref_General_Notes);
         label.setToolTipText(Messages.Pref_General_Notes_Tooltip);

         _txtNotes = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtNotes);
      }

      return container;
   }

   @Override
   public void dispose() {

      super.dispose();
   }

   private void doTimeZoneLiveUpdate() {

      if (_chkTimeZone_LiveUpdate.getSelection()) {
         performApply();
      }
   }

   private void enableControls() {

      final boolean isUseTimeZone = _chkTimeZone_UseAnotherTimeZone.getSelection();

      _lblTimeZone_Info.setEnabled(isUseTimeZone);
      _rdoTimeZone_1.setEnabled(isUseTimeZone);
      _rdoTimeZone_2.setEnabled(isUseTimeZone);
      _rdoTimeZone_3.setEnabled(isUseTimeZone);
      _comboTimeZone_1.setEnabled(isUseTimeZone);
      _comboTimeZone_2.setEnabled(isUseTimeZone);
      _comboTimeZone_3.setEnabled(isUseTimeZone);
   }

   private void fillSystemControls() {

      final boolean isInUpdateUIBackup = _isInUpdateUI;

      _isInUpdateUI = true;
      {
         _comboSystem_Profile.removeAll();
         for (final MeasurementSystem measurementSystem : _allSystemProfiles) {
            _comboSystem_Profile.add(measurementSystem.getName());
         }

         _comboSystem_Distance.removeAll();
         for (final SystemDistance systemDistance : _allSystemDistances) {
            _comboSystem_Distance.add(systemDistance.label);
         }

         _comboSystem_Elevation.removeAll();
         for (final SystemElevation systemElevation : _allSystemElevations) {
            _comboSystem_Elevation.add(systemElevation.label);
         }

         _comboSystem_Temperature.removeAll();
         for (final SystemTemperature systemTemperature : _allSystemTemperatures) {
            _comboSystem_Temperature.add(systemTemperature.label);
         }

         _comboSystem_Weight.removeAll();
         for (final SystemWeight systemWeight : _allSystemWeights) {
            _comboSystem_Weight.add(systemWeight.label);
         }
      }
      _isInUpdateUI = isInUpdateUIBackup;
   }

   private int getSelectedCustomZoneNumber() {

      if (_rdoTimeZone_1.getSelection()) {
         return 1;
      } else if (_rdoTimeZone_2.getSelection()) {
         return 2;
      } else {
         return 3;
      }
   }

   private String getSelectedTimeZoneId(final boolean isUseSystemTimeZone, final int selectedZone) {

      if (isUseSystemTimeZone) {

         return ZoneId.systemDefault().getId();

      } else {

         if (selectedZone == 1) {
            return _timeZoneId_1;
         } else if (selectedZone == 2) {
            return _timeZoneId_2;
         } else {
            return _timeZoneId_3;
         }
      }
   }

   private int getSystemIndex_Distance(final MeasurementSystem selectedSystemProfile) {

      final Distance selectedSystemDistance = selectedSystemProfile.getDistance();

      for (int systemIndex = 0; systemIndex < _allSystemDistances.length; systemIndex++) {

         if (selectedSystemDistance.equals(_allSystemDistances[systemIndex].distance)) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   private int getSystemIndex_Elevation(final MeasurementSystem selectedSystemProfile) {

      final Elevation selectedSystemElevation = selectedSystemProfile.getElevation();

      for (int systemIndex = 0; systemIndex < _allSystemElevations.length; systemIndex++) {

         if (selectedSystemElevation.equals(_allSystemElevations[systemIndex].elevation)) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   private int getSystemIndex_Temperature(final MeasurementSystem selectedSystemProfile) {

      final Temperature selectedSystemTemperature = selectedSystemProfile.getTemperature();

      for (int systemIndex = 0; systemIndex < _allSystemTemperatures.length; systemIndex++) {

         if (selectedSystemTemperature.equals(_allSystemTemperatures[systemIndex].temperature)) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   private int getSystemIndex_Weight(final MeasurementSystem selectedSystemProfile) {

      final Weight selectedSystemWeight = selectedSystemProfile.getWeight();

      for (int systemIndex = 0; systemIndex < _allSystemWeights.length; systemIndex++) {

         if (selectedSystemWeight.equals(_allSystemWeights[systemIndex].weight)) {
            return systemIndex;
         }
      }

      // return default
      return 0;
   }

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);

      _isShowMeasurementSystemInUI = _prefStore.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI);
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   public boolean okToLeave() {

      saveUIState();
      checkCalendarWeek();

      return super.okToLeave();
   }

   /**
    * compute calendar week for all tours
    */
   private void onComputeCalendarWeek() {

      // set app wide week settings
      saveState();

      _currentFirstDayOfWeek = _backupFirstDayOfWeek =
            _prefStoreCommon.getInt(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

      _currentMinimalDaysInFirstWeek = _backupMinimalDaysInFirstWeek =
            _prefStoreCommon.getInt(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

      final IRunnableWithProgress runnable = new IRunnableWithProgress() {
         @Override
         public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

            try (Connection conn = TourDatabase.getInstance().getConnection()) {

               TourDatabase.updateTourWeek(conn, monitor);

            } catch (final SQLException e) {
               net.tourbook.ui.UI.showSQLException(e);
            }
         }
      };

      try {
         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, runnable);
      } catch (final InvocationTargetException | InterruptedException e) {
         e.printStackTrace();
      }

      // fire modify event to update tour statistics and tour editor
      _prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
   }

   private void onSelectCalendarWeek() {

      _currentFirstDayOfWeek = _comboWeek_FirstDay.getSelectionIndex() + 1;
      _currentMinimalDaysInFirstWeek = _comboWeek_MinDaysInFirstWeek.getSelectionIndex() + 1;
   }

   private void onSystemItem_Select() {

      final int activeSystemProfileIndex = MeasurementSystem_Manager.getActiveSystemProfileIndex();

      // update model
      final MeasurementSystem selectedSystemProfile = _allSystemProfiles.get(activeSystemProfileIndex);

      final Distance distance = _allSystemDistances[_comboSystem_Distance.getSelectionIndex()].distance;
      final Elevation elevation = _allSystemElevations[_comboSystem_Elevation.getSelectionIndex()].elevation;
      final Temperature temperature = _allSystemTemperatures[_comboSystem_Temperature.getSelectionIndex()].temperature;
      final Weight weight = _allSystemWeights[_comboSystem_Weight.getSelectionIndex()].weight;

      selectedSystemProfile.setDistance(distance);
      selectedSystemProfile.setElevation(elevation);
      selectedSystemProfile.setTemperature(temperature);
      selectedSystemProfile.setWeight(weight);
   }

   private void onSystemProfile_Modify(final ModifyEvent event) {

      if (_isInUpdateUI) {
         return;
      }

      final int selectedSystemIndex = _comboSystem_Profile.getSelectionIndex();
      final String newProfileText = _comboSystem_Profile.getText();

      if (selectedSystemIndex != -1) {

         // this occurs when an item is selected -> ignore

         return;
      }

      // selectedSystemIndex == -1 -> the previous selected item is modified -> update previous item

      final int activeSystemProfileIndex = MeasurementSystem_Manager.getActiveSystemProfileIndex();

      // update model
      final MeasurementSystem previousSelectedProfile = _allSystemProfiles.get(activeSystemProfileIndex);
      previousSelectedProfile.setName(newProfileText);

      _comboSystem_Profile.getDisplay().asyncExec(() -> {

         // because the index is -1 -> reselect it

         // update UI
         _comboSystem_Profile.setItem(activeSystemProfileIndex, newProfileText);
         _comboSystem_Profile.select(activeSystemProfileIndex);

         // by default the text is selected -> remove anoying selection
         _comboSystem_Profile.clearSelection();
      });
   }

   private void onSystemProfile_Select() {

      final int activeSystemProfileIndex = _comboSystem_Profile.getSelectionIndex();

      // update model
      MeasurementSystem_Manager.setActiveSystemProfileIndex(activeSystemProfileIndex);

      // update UI
      final MeasurementSystem selectedSystemProfile = _allSystemProfiles.get(activeSystemProfileIndex);
      _comboSystem_Distance.select(getSystemIndex_Distance(selectedSystemProfile));
      _comboSystem_Elevation.select(getSystemIndex_Elevation(selectedSystemProfile));
      _comboSystem_Temperature.select(getSystemIndex_Temperature(selectedSystemProfile));
      _comboSystem_Weight.select(getSystemIndex_Weight(selectedSystemProfile));
   }

   @Override
   public boolean performCancel() {
      saveUIState();
      return super.performCancel();
   }

   @Override
   protected void performDefaults() {

      final int selectedTab = _tabFolder.getSelectionIndex();

      if (selectedTab == TAB_FOLDER_MEASUREMENT_SYSTEM) {

         _allSystemProfiles.clear();

         // clone default profiles
         final ArrayList<MeasurementSystem> allSystemProfiles = MeasurementSystem_Manager.getDefaultProfiles();
         for (final MeasurementSystem measurementSystem : allSystemProfiles) {
            _allSystemProfiles.add(measurementSystem.clone());
         }

         // update profile names
         fillSystemControls();

         // select metric system
         _comboSystem_Profile.select(0);

         onSystemProfile_Select();

      } else if (selectedTab == TAB_FOLDER_TIME_ZONE) {

         // time zone
         final int activeZone = _prefStoreCommon.getDefaultInt(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE);

         _chkTimeZone_LiveUpdate.setSelection(_prefStoreCommon.getDefaultBoolean(ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE));

         _chkTimeZone_UseAnotherTimeZone.setSelection(_prefStoreCommon.getDefaultBoolean(
               ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE) == false);

         _timeZoneId_1 = _prefStoreCommon.getDefaultString(ICommonPreferences.TIME_ZONE_LOCAL_ID_1);
         _timeZoneId_2 = _prefStoreCommon.getDefaultString(ICommonPreferences.TIME_ZONE_LOCAL_ID_2);
         _timeZoneId_3 = _prefStoreCommon.getDefaultString(ICommonPreferences.TIME_ZONE_LOCAL_ID_3);

         _rdoTimeZone_1.setSelection(activeZone != 2 && activeZone != 3);
         _rdoTimeZone_2.setSelection(activeZone == 2);
         _rdoTimeZone_3.setSelection(activeZone == 3);

         validateTimeZoneId();
         doTimeZoneLiveUpdate();

      } else if (selectedTab == TAB_FOLDER_CALENDAR_WEEK) {

         // calendar week
         _backupFirstDayOfWeek =
               _currentFirstDayOfWeek = _prefStoreCommon.getDefaultInt(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

         _backupMinimalDaysInFirstWeek =
               _currentMinimalDaysInFirstWeek = _prefStoreCommon.getDefaultInt(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

         updateUI_CalendarWeek();
      }

      enableControls();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();
      if (isOK) {

         checkCalendarWeek();

         saveState();

         if (_chkSystem_ShowMeasurementInAppToolbar.getSelection() != _isShowMeasurementSystemInUI) {

            // field is modified, ask for restart

            if (MessageDialog.openQuestion(
                  Display.getDefault().getActiveShell(),
                  Messages.pref_general_restart_app_title,
                  Messages.pref_general_restart_app_message)) {

               Display.getCurrent().asyncExec(new Runnable() {
                  @Override
                  public void run() {
                     PlatformUI.getWorkbench().restart();
                  }
               });
            }
         }

      }

      return isOK;
   }

   private void restoreState() {

      _isInUpdateUI = true;
      {
         // measurement system

         _chkSystem_ShowMeasurementInAppToolbar.setSelection(_prefStore.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI));

         _allSystemProfiles = new ArrayList<>();

         // clone profiles
         final ArrayList<MeasurementSystem> allSystemProfiles = MeasurementSystem_Manager.getCurrentProfiles();
         for (final MeasurementSystem measurementSystem : allSystemProfiles) {
            _allSystemProfiles.add(measurementSystem.clone());
         }

         fillSystemControls();

         // select active system
         final int activeSystemIndex = MeasurementSystem_Manager.getActiveSystemProfileIndex();
         _comboSystem_Profile.select(activeSystemIndex);

         onSystemProfile_Select();
      }
      {
         // time zone

         final int activeZone = _prefStoreCommon.getInt(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE);

         _chkTimeZone_LiveUpdate.setSelection(_prefStoreCommon.getBoolean(ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE));
         _chkTimeZone_UseAnotherTimeZone.setSelection(_prefStoreCommon.getBoolean(ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE) == false);

         _timeZoneId_1 = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID_1);
         _timeZoneId_2 = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID_2);
         _timeZoneId_3 = _prefStoreCommon.getString(ICommonPreferences.TIME_ZONE_LOCAL_ID_3);

         _rdoTimeZone_1.setSelection(activeZone != 2 && activeZone != 3);
         _rdoTimeZone_2.setSelection(activeZone == 2);
         _rdoTimeZone_3.setSelection(activeZone == 3);

         validateTimeZoneId();
      }

      {
         // calendar week

         _backupFirstDayOfWeek =
               _currentFirstDayOfWeek = _prefStoreCommon.getInt(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

         _backupMinimalDaysInFirstWeek =
               _currentMinimalDaysInFirstWeek = _prefStoreCommon.getInt(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

         updateUI_CalendarWeek();
      }

      {
         // general

         _txtNotes.setText(_prefStore.getString(ITourbookPreferences.GENERAL_NOTES));
      }

      {
         // folder

         _tabFolder.setSelection(_prefStore.getInt(STATE_GENERAL_SELECTED_TAB));
      }
      _isInUpdateUI = false;
   }

   private void saveState() {

      {
         // measurement system

         MeasurementSystem_Manager.saveState(_allSystemProfiles);

         _prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI, _chkSystem_ShowMeasurementInAppToolbar.getSelection());
      }

      {
         // time zone

         final boolean isUseSystemTimeZone = !_chkTimeZone_UseAnotherTimeZone.getSelection();
         final int selectedZone = getSelectedCustomZoneNumber();
         final String selectedTimeZoneId = getSelectedTimeZoneId(isUseSystemTimeZone, selectedZone);

         // update static field BEFORE an event is fired !!!
         TimeTools.setDefaultTimeZone(selectedTimeZoneId);

         // time zone
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE, _chkTimeZone_LiveUpdate.getSelection());
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE, isUseSystemTimeZone);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE, selectedZone);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID, selectedTimeZoneId);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID_1, _timeZoneId_1);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID_2, _timeZoneId_2);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID_3, _timeZoneId_3);
      }

      {
         // calendar week

         final int firstDayOfWeek = _comboWeek_FirstDay.getSelectionIndex() + 1;
         final int minDays = _comboWeek_MinDaysInFirstWeek.getSelectionIndex() + 1;

         _prefStoreCommon.setValue(ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK, firstDayOfWeek);
         _prefStoreCommon.setValue(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK, minDays);

         TimeTools.setCalendarWeek(firstDayOfWeek, minDays);
      }

      {
         // general

         _prefStore.setValue(ITourbookPreferences.GENERAL_NOTES, _txtNotes.getText());
      }
   }

   private void saveUIState() {

      if (_tabFolder == null || _tabFolder.isDisposed()) {
         return;
      }

      _prefStore.setValue(STATE_GENERAL_SELECTED_TAB, _tabFolder.getSelectionIndex());
   }

   private void updateModel_TimeZone() {

      final TimeZoneData selectedTimeZone_1 = TimeTools.getTimeZone_ByIndex(_comboTimeZone_1.getSelectionIndex());
      final TimeZoneData selectedTimeZone_2 = TimeTools.getTimeZone_ByIndex(_comboTimeZone_2.getSelectionIndex());
      final TimeZoneData selectedTimeZone_3 = TimeTools.getTimeZone_ByIndex(_comboTimeZone_3.getSelectionIndex());

      _timeZoneId_1 = selectedTimeZone_1.zoneId;
      _timeZoneId_2 = selectedTimeZone_2.zoneId;
      _timeZoneId_3 = selectedTimeZone_3.zoneId;
   }

   protected void updateUI_CalendarWeek() {

      _comboWeek_FirstDay.select(_backupFirstDayOfWeek - 1);
      _comboWeek_MinDaysInFirstWeek.select(_backupMinimalDaysInFirstWeek - 1);
   }

   /**
    * Ensure {@link #_timeZoneId} is valid.
    */
   private void validateTimeZoneId() {

      final TimeZoneData firstTimeZone = TimeTools.getAllTimeZones().get(0);

      final int timeZoneIndex_1 = TimeTools.getTimeZoneIndex(_timeZoneId_1);
      final int timeZoneIndex_2 = TimeTools.getTimeZoneIndex(_timeZoneId_2);
      final int timeZoneIndex_3 = TimeTools.getTimeZoneIndex(_timeZoneId_3);

      if (timeZoneIndex_1 == -1) {
         _timeZoneId_1 = firstTimeZone.zoneId;
      }
      if (timeZoneIndex_2 == -1) {
         _timeZoneId_2 = firstTimeZone.zoneId;
      }
      if (timeZoneIndex_3 == -1) {
         _timeZoneId_3 = firstTimeZone.zoneId;
      }

      _comboTimeZone_1.select(timeZoneIndex_1);
      _comboTimeZone_2.select(timeZoneIndex_2);
      _comboTimeZone_3.select(timeZoneIndex_3);
   }
}
