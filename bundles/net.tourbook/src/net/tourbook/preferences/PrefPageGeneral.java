/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
import net.tourbook.application.MeasurementSystemContributionItem;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
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

   /*
    * contains the tab folder index
    */
   public static final int          TAB_FOLDER_MEASUREMENT_SYSTEM = 0;
   public static final int          TAB_FOLDER_TIME_ZONE          = 1;
   public static final int          TAB_FOLDER_CALENDAR_WEEK      = 2;

   private IPreferenceStore         _prefStore                    = TourbookPlugin.getPrefStore();
   private IPreferenceStore         _prefStoreCommon              = CommonActivator.getPrefStore();

   private String                   _timeZoneId_1;
   private String                   _timeZoneId_2;
   private String                   _timeZoneId_3;

   private int                      _backupFirstDayOfWeek;
   private int                      _backupMinimalDaysInFirstWeek;
   private int                      _currentFirstDayOfWeek;
   private int                      _currentMinimalDaysInFirstWeek;

   private boolean                  _showMeasurementSystemInUI;

   private PixelConverter           _pc;

   /**
    * Contains the controls which are displayed in the first column. These controls are used to get
    * the maximum width and set the first column within the differenct section to the same width.
    */
   private final ArrayList<Control> _firstColumnControls          = new ArrayList<>();

   /*
    * UI controls
    */
   private TabFolder _tabFolder;

   // timezone
   private Button _chkTimeZoneLiveUpdate;
   private Button _chkUseAnotherTimeZone;
   private Button _rdoTimeZone_1;
   private Button _rdoTimeZone_2;
   private Button _rdoTimeZone_3;
   private Combo  _comboTimeZone_1;
   private Combo  _comboTimeZone_2;
   private Combo  _comboTimeZone_3;

   // measurement system
   private Button _chkShowMeasurementInAppToolbar;
   private Combo  _comboSystem;

   private Label  _lblSystemAltitude;
   private Label  _lblSystemDistance;
   private Label  _lblSystemTemperature;
   private Label  _lblTimeZoneInfo;

   private Button _rdoAltitudeMeter;
   private Button _rdoAltitudeFoot;
   private Button _rdoDistanceKm;
   private Button _rdoDistanceMi;
   private Button _rdoTemperatureCelcius;
   private Button _rdoTemperatureFahrenheit;

   // calendar week
   private Combo _comboFirstDay;
   private Combo _comboMinDaysInFirstWeek;

   // notes
   private Text _txtNotes;

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
         GridDataFactory
               .fillDefaults()//
               .grab(true, true)
               .applyTo(_tabFolder);
         {

            final TabItem tabMeasurementSystem = new TabItem(_tabFolder, SWT.NONE);
            tabMeasurementSystem.setControl(createUI_10_MeasurementSystem(_tabFolder));
            tabMeasurementSystem.setText(Messages.Pref_general_system_measurement);

            final TabItem tabBreakTime = new TabItem(_tabFolder, SWT.NONE);
            tabBreakTime.setControl(createUI_20_TimeZone(_tabFolder));
            tabBreakTime.setText(Messages.Pref_General_Group_TimeZone);

            final TabItem tabElevation = new TabItem(_tabFolder, SWT.NONE);
            tabElevation.setControl(createUI_30_WeekNumber(_tabFolder));
            tabElevation.setText(Messages.Pref_General_CalendarWeek);

            final TabItem tabNotes = new TabItem(_tabFolder, SWT.NONE);
            tabNotes.setControl(createUI_40_Notes(_tabFolder));
            tabNotes.setText(Messages.Pref_General_Notes);
         }
      }
   }

   private Composite createUI_10_MeasurementSystem(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory
            .swtDefaults()//
            .numColumns(2)
            .extendedMargins(5, 5, 10, 5)
            .spacing(20, 5)
            .applyTo(container);
//      container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         /*
          * measurement system
          */
         // label
         final Label label = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
         label.setText(Messages.Pref_General_Label_MeasurementSystem);

         // combo
         _comboSystem = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
         _comboSystem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectSystem();
            }
         });

         // fill combo box
         _comboSystem.add(Messages.App_measurement_metric); // metric system
         _comboSystem.add(Messages.App_measurement_imperial); // imperial system

         /*
          * radio: altitude
          */

         // label
         _lblSystemAltitude = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults().indent(20, 0).applyTo(_lblSystemAltitude);
         _lblSystemAltitude.setText(Messages.Pref_general_system_altitude);

         // radio
         final Composite containerAltitude = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerAltitude);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerAltitude);
         {
            _rdoAltitudeMeter = new Button(containerAltitude, SWT.RADIO);
            _rdoAltitudeMeter.setText(Messages.Pref_general_metric_unit_m);

            GridDataFactory.fillDefaults().applyTo(_rdoAltitudeMeter);
            _firstColumnControls.add(_rdoAltitudeMeter);

            _rdoAltitudeFoot = new Button(containerAltitude, SWT.RADIO);
            _rdoAltitudeFoot.setText(Messages.Pref_general_imperial_unit_feet);
         }

         /*
          * radio: distance
          */

         // label
         _lblSystemDistance = new Label(container, SWT.NONE);
         GridDataFactory.fillDefaults().indent(20, 0).applyTo(_lblSystemDistance);
         _lblSystemDistance.setText(Messages.Pref_general_system_distance);

         // radio
         final Composite containerDistance = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(containerDistance);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerDistance);
         {
            _rdoDistanceKm = new Button(containerDistance, SWT.RADIO);
            _rdoDistanceKm.setText(Messages.Pref_general_metric_unit_km);

            GridDataFactory.fillDefaults().applyTo(_rdoDistanceKm);
            _firstColumnControls.add(_rdoDistanceKm);

            _rdoDistanceMi = new Button(containerDistance, SWT.RADIO);
            _rdoDistanceMi.setText(Messages.Pref_general_imperial_unit_mi);
         }

         {
            /*
             * radio: temperature
             */

            // label
            _lblSystemTemperature = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults().indent(20, 0).applyTo(_lblSystemTemperature);
            _lblSystemTemperature.setText(Messages.Pref_general_system_temperature);

            // radio
            final Composite containerTemperature = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerTemperature);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTemperature);
            {
               _rdoTemperatureCelcius = new Button(containerTemperature, SWT.RADIO);
               _rdoTemperatureCelcius.setText(Messages.Pref_general_metric_unit_celcius);

               GridDataFactory.fillDefaults().applyTo(_rdoTemperatureCelcius);
               _firstColumnControls.add(_rdoTemperatureCelcius);

               _rdoTemperatureFahrenheit = new Button(containerTemperature, SWT.RADIO);
               _rdoTemperatureFahrenheit.setText(Messages.Pref_general_imperial_unit_fahrenheit);
            }
         }

         {
            /*
             * Checkbox: Show in app toolbar
             */
            _chkShowMeasurementInAppToolbar = new Button(container, SWT.CHECK);
            _chkShowMeasurementInAppToolbar.setText(Messages.Pref_general_show_system_in_ui);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .indent(0, _pc.convertVerticalDLUsToPixels(8))
                  .applyTo(_chkShowMeasurementInAppToolbar);
         }
      }

      container.layout(true, true);
      UI.setEqualizeColumWidths(_firstColumnControls);

      return container;
   }

   private Composite createUI_20_TimeZone(final Composite parent) {

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
            _chkUseAnotherTimeZone = new Button(container, SWT.CHECK);
            _chkUseAnotherTimeZone.setText(NLS.bind(Messages.Pref_General_Checkbox_SetTimeZone, defaultTimeZoneId));
            _chkUseAnotherTimeZone.addSelectionListener(timeZoneListener);
            GridDataFactory
                  .fillDefaults()//
                  .span(2, 1)
                  .indent(0, 2 * verticalSpacing)
                  .applyTo(_chkUseAnotherTimeZone);
         }

         {
            /*
             * Label: Info
             */
            _lblTimeZoneInfo = new Label(container, SWT.WRAP);
            _lblTimeZoneInfo.setText(Messages.Pref_General_Label_SetAnotherTimeZone);

            GridDataFactory
                  .fillDefaults()//
                  .grab(true, false)
                  .span(2, 1)
                  .indent(columnIndent, verticalSpacing)
                  .hint(defaultTextWidth, SWT.DEFAULT)
                  .applyTo(_lblTimeZoneInfo);
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
            _chkTimeZoneLiveUpdate = new Button(container, SWT.CHECK);
            _chkTimeZoneLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
            _chkTimeZoneLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
            _chkTimeZoneLiveUpdate.addSelectionListener(new SelectionAdapter() {
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
                  .applyTo(_chkTimeZoneLiveUpdate);
         }
      }

      return container;
   }

   private Composite createUI_30_WeekNumber(final Composite parent) {

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

         _comboFirstDay = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboFirstDay.setVisibleItemCount(10);
         _comboFirstDay.addSelectionListener(new SelectionAdapter() {
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

            _comboFirstDay.add(weekDay);
         }

         /*
          * minimal days in first week
          */
         label = new Label(container, SWT.NONE);
         label.setText(Messages.Pref_General_Label_MinimalDaysInFirstWeek);
         label.setToolTipText(Messages.Pref_General_Label_MinimalDaysInFirstWeek_Tooltip);

         _comboMinDaysInFirstWeek = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboMinDaysInFirstWeek.setVisibleItemCount(10);
         _comboMinDaysInFirstWeek.addSelectionListener(new SelectionAdapter() {
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
            _comboMinDaysInFirstWeek.add(dayText);
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

   private Composite createUI_40_Notes(final Composite parent) {

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

      _firstColumnControls.clear();

      super.dispose();
   }

   private void doTimeZoneLiveUpdate() {

      if (_chkTimeZoneLiveUpdate.getSelection()) {
         performApply();
      }
   }

   private void enableControls() {

      final boolean isUseTimeZone = _chkUseAnotherTimeZone.getSelection();

      _lblTimeZoneInfo.setEnabled(isUseTimeZone);
      _rdoTimeZone_1.setEnabled(isUseTimeZone);
      _rdoTimeZone_2.setEnabled(isUseTimeZone);
      _rdoTimeZone_3.setEnabled(isUseTimeZone);
      _comboTimeZone_1.setEnabled(isUseTimeZone);
      _comboTimeZone_2.setEnabled(isUseTimeZone);
      _comboTimeZone_3.setEnabled(isUseTimeZone);

      /*
       * disable all individual measurement controls because this is currently not working when
       * individual systems are changed
       */
      _lblSystemAltitude.setEnabled(false);
      _lblSystemDistance.setEnabled(false);
      _lblSystemTemperature.setEnabled(false);

      _rdoAltitudeMeter.setEnabled(false);
      _rdoAltitudeFoot.setEnabled(false);

      _rdoDistanceKm.setEnabled(false);
      _rdoDistanceMi.setEnabled(false);

      _rdoTemperatureCelcius.setEnabled(false);
      _rdoTemperatureFahrenheit.setEnabled(false);
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

   @Override
   public void init(final IWorkbench workbench) {

      setPreferenceStore(_prefStore);

      _showMeasurementSystemInUI = _prefStore.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI);
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

            Connection conn = null;
            try {

               conn = TourDatabase.getInstance().getConnection();
               TourDatabase.updateTourWeek(conn, monitor);

            } catch (final SQLException e) {
               net.tourbook.ui.UI.showSQLException(e);
            } finally {
               if (conn != null) {
                  try {
                     conn.close();
                  } catch (final SQLException e) {
                     net.tourbook.ui.UI.showSQLException(e);
                  }
               }
            }
         }
      };

      try {
         new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, runnable);
      } catch (final InvocationTargetException e) {
         e.printStackTrace();
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }

      // fire modify event to update tour statistics and tour editor
      _prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
   }

   private void onSelectCalendarWeek() {

      _currentFirstDayOfWeek = _comboFirstDay.getSelectionIndex() + 1;
      _currentMinimalDaysInFirstWeek = _comboMinDaysInFirstWeek.getSelectionIndex() + 1;
   }

   private void onSelectSystem() {

      int selectedSystem = _comboSystem.getSelectionIndex();

      if (selectedSystem == -1) {
         _comboSystem.select(0);
         selectedSystem = 0;
      }

      if (selectedSystem == 0) {

         // metric

         _rdoAltitudeMeter.setSelection(true);
         _rdoAltitudeFoot.setSelection(false);

         _rdoDistanceKm.setSelection(true);
         _rdoDistanceMi.setSelection(false);

         _rdoTemperatureCelcius.setSelection(true);
         _rdoTemperatureFahrenheit.setSelection(false);

      } else {

         // imperial

         _rdoAltitudeMeter.setSelection(false);
         _rdoAltitudeFoot.setSelection(true);

         _rdoDistanceKm.setSelection(false);
         _rdoDistanceMi.setSelection(true);

         _rdoTemperatureCelcius.setSelection(false);
         _rdoTemperatureFahrenheit.setSelection(true);
      }
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

      } else if (selectedTab == TAB_FOLDER_TIME_ZONE) {

         // time zone
         final int activeZone = _prefStoreCommon.getDefaultInt(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE);

         _chkTimeZoneLiveUpdate.setSelection(//
               _prefStoreCommon.getDefaultBoolean(ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE));

         _chkUseAnotherTimeZone.setSelection(//
               _prefStoreCommon.getDefaultBoolean(ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE) == false);

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
         _backupFirstDayOfWeek = //
               _currentFirstDayOfWeek = _prefStoreCommon.getDefaultInt(
                     ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

         _backupMinimalDaysInFirstWeek = //
               _currentMinimalDaysInFirstWeek = _prefStoreCommon
                     .getDefaultInt(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

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

         if (_chkShowMeasurementInAppToolbar.getSelection() != _showMeasurementSystemInUI) {

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

      {
         // measurement system

         _chkShowMeasurementInAppToolbar.setSelection(
               _prefStore
                     .getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI));
         MeasurementSystemContributionItem.selectSystemFromPrefStore(_comboSystem);
         onSelectSystem();
      }

      {
         // time zone

         final int activeZone = _prefStoreCommon.getInt(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE);

         _chkTimeZoneLiveUpdate.setSelection(
               _prefStoreCommon
                     .getBoolean(ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE));
         _chkUseAnotherTimeZone.setSelection(//
               _prefStoreCommon.getBoolean(ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE) == false);

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

         _backupFirstDayOfWeek = //
               _currentFirstDayOfWeek = _prefStoreCommon.getInt(
                     ICommonPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

         _backupMinimalDaysInFirstWeek = //
               _currentMinimalDaysInFirstWeek = _prefStoreCommon
                     .getInt(ICommonPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

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
   }

   private void saveState() {

      {
         // measurement system

         int selectedIndex = _comboSystem.getSelectionIndex();
         if (selectedIndex == -1) {
            selectedIndex = 0;
         }
         MeasurementSystemContributionItem.selectSystemInPrefStore(selectedIndex);

			_prefStore.setValue(
					ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI,
					_chkShowMeasurementInAppToolbar.getSelection());
      }

      {
         // time zone

         final boolean isUseSystemTimeZone = !_chkUseAnotherTimeZone.getSelection();
         final int selectedZone = getSelectedCustomZoneNumber();
         final String selectedTimeZoneId = getSelectedTimeZoneId(isUseSystemTimeZone, selectedZone);

         // update static field BEFORE an event is fired !!!
         TimeTools.setDefaultTimeZone(selectedTimeZoneId);

         // time zone
         _prefStoreCommon.setValue(
               ICommonPreferences.TIME_ZONE_IS_LIVE_UPDATE, //
               _chkTimeZoneLiveUpdate.getSelection());
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE, isUseSystemTimeZone);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_SELECTED_CUSTOM_ZONE, selectedZone);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID, selectedTimeZoneId);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID_1, _timeZoneId_1);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID_2, _timeZoneId_2);
         _prefStoreCommon.setValue(ICommonPreferences.TIME_ZONE_LOCAL_ID_3, _timeZoneId_3);
      }

      {
         // calendar week

         final int firstDayOfWeek = _comboFirstDay.getSelectionIndex() + 1;
         final int minDays = _comboMinDaysInFirstWeek.getSelectionIndex() + 1;

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

      _comboFirstDay.select(_backupFirstDayOfWeek - 1);
      _comboMinDaysInFirstWeek.select(_backupMinimalDaysInFirstWeek - 1);
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
