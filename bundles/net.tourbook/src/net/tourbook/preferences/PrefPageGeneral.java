/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;

import net.tourbook.Messages;
import net.tourbook.application.MeasurementSystemContributionItem;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.preferences.BooleanFieldEditor2;
import net.tourbook.common.time.TimeZone;
import net.tourbook.common.time.TimeZoneUtils;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageGeneral extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID			= "net.tourbook.preferences.PrefPageGeneralId"; //$NON-NLS-1$

	private IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private String				_timeZoneId;
	private int					_timeZoneOffset;

	private int					_backupFirstDayOfWeek;
	private int					_backupMinimalDaysInFirstWeek;
	private int					_currentFirstDayOfWeek;
	private int					_currentMinimalDaysInFirstWeek;

	private boolean				_showMeasurementSystemInUI;

	private PixelConverter		_pc;

	/*
	 * UI controls
	 */
	// timezone
	private Combo				_comboTimeZone;
	private Label				_lblTimeZoneOffset;

	private Combo				_comboSystem;
	private Label				_lblSystemAltitude;
	private Label				_lblSystemDistance;
	private Label				_lblSystemTemperature;
	private Button				_rdoAltitudeMeter;
	private Button				_rdoAltitudeFoot;
	private Button				_rdoDistanceKm;
	private Button				_rdoDistanceMi;
	private Button				_rdoTemperatureCelcius;
	private Button				_rdoTemperatureFahrenheit;

	private BooleanFieldEditor2	_editShowMeasurementInUI;
	private Combo				_comboFirstDay;
	private Combo				_comboMinDaysInFirstWeek;

	private Text				_txtNotes;

	// live update
	private Button				_chkLiveUpdate;

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
			createUI_10_TimeZone(parent);
			createUI_20_MeasurementSystem(parent);
			createUI_30_WeekNumber(parent);
			createUI_40_Notes(parent);
		}
	}

	private void createUI_10_TimeZone(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_General_Group_TimeZone);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults()//
				.numColumns(3)
				.applyTo(group);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_MAGENTA));
		{
			/*
			 * Time zone
			 */
			{
				// label
				final Label label = new Label(group, NONE);
				label.setText(Messages.Pref_General_Label_LocalTimeZone);
			}

			{
				// combo
				_comboTimeZone = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
				_comboTimeZone.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						updateUI_TimeZone();
						doTimeZoneLiveUpdate();
					}
				});
				GridDataFactory.fillDefaults()//
						.indent(_pc.convertWidthInCharsToPixels(2), 0)
						.applyTo(_comboTimeZone);

				// fill combobox
				for (final TimeZone timeZone : TimeZoneUtils.getAllTimeZones()) {
					_comboTimeZone.add(timeZone.zoneId);
				}
			}

			{
				// label: offset
				_lblTimeZoneOffset = new Label(group, NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.indent(_pc.convertWidthInCharsToPixels(0), 0)
						.applyTo(_lblTimeZoneOffset);
			}

			createUI_99_LiveUpdate(group);
		}
	}

	private void createUI_20_MeasurementSystem(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_general_system_measurement);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			/*
			 * measurement system
			 */
			// label
			final Label label = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
			label.setText(Messages.Pref_General_Label_MeasurementSystem);

			// combo
			_comboSystem = new Combo(group, SWT.DROP_DOWN | SWT.READ_ONLY);
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
			_lblSystemAltitude = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().indent(20, 0).applyTo(_lblSystemAltitude);
			_lblSystemAltitude.setText(Messages.Pref_general_system_altitude);

			// radio
			final Composite containerAltitude = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerAltitude);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerAltitude);
			{
				_rdoAltitudeMeter = new Button(containerAltitude, SWT.RADIO);
				_rdoAltitudeMeter.setText(Messages.Pref_general_metric_unit_m);

				_rdoAltitudeFoot = new Button(containerAltitude, SWT.RADIO);
				_rdoAltitudeFoot.setText(Messages.Pref_general_imperial_unit_feet);
			}

			/*
			 * radio: distance
			 */

			// label
			_lblSystemDistance = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().indent(20, 0).applyTo(_lblSystemDistance);
			_lblSystemDistance.setText(Messages.Pref_general_system_distance);

			// radio
			final Composite containerDistance = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerDistance);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerDistance);
			{
				_rdoDistanceKm = new Button(containerDistance, SWT.RADIO);
				_rdoDistanceKm.setText(Messages.Pref_general_metric_unit_km);

				_rdoDistanceMi = new Button(containerDistance, SWT.RADIO);
				_rdoDistanceMi.setText(Messages.Pref_general_imperial_unit_mi);
			}

			/*
			 * radio: temperature
			 */

			// label
			_lblSystemTemperature = new Label(group, SWT.NONE);
			GridDataFactory.fillDefaults().indent(20, 0).applyTo(_lblSystemTemperature);
			_lblSystemTemperature.setText(Messages.Pref_general_system_temperature);

			// radio
			final Composite containerTemperature = new Composite(group, SWT.NONE);
			GridDataFactory.fillDefaults().grab(true, false).applyTo(containerTemperature);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerTemperature);
			{
				_rdoTemperatureCelcius = new Button(containerTemperature, SWT.RADIO);
				_rdoTemperatureCelcius.setText(Messages.Pref_general_metric_unit_celcius);

				_rdoTemperatureFahrenheit = new Button(containerTemperature, SWT.RADIO);
				_rdoTemperatureFahrenheit.setText(Messages.Pref_general_imperial_unit_fahrenheit);
			}

			/*
			 * this is currently disabled because computing the power according to
			 * http://www.rennradtraining.de/www.kreuzotter.de/deutsch/speed.htm takes a lot of time
			 * to implement it correctly
			 */
//			// radio: energy
//			addField(new RadioGroupFieldEditor(
//					ITourbookPreferences.MEASUREMENT_SYSTEM_ENERGY,
//					Messages.Pref_General_Energy,
//					2,
//					new String[][] {
//							new String[] {
//									Messages.Pref_General_Energy_Joule,
//									ITourbookPreferences.MEASUREMENT_SYSTEM_ENERGY_JOULE },
//							new String[] {
//									Messages.Pref_General_Energy_Calorie,
//									ITourbookPreferences.MEASUREMENT_SYSTEM_ENERGY_CALORIE }, },
//					group,
//					false));

			/*
			 * checkbox: show in UI
			 */
			addField(_editShowMeasurementInUI = new BooleanFieldEditor2(
					ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI,
					Messages.Pref_general_show_system_in_ui,
					group));
		}

		// set layout AFTER the fields are set !!!
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);

		final Button showSystemInUI = _editShowMeasurementInUI.getChangeControl(group);
		GridDataFactory.fillDefaults().span(2, 1).indent(0, 5).applyTo(showSystemInUI);
	}

	private void createUI_30_WeekNumber(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.Pref_General_CalendarWeek);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			/*
			 * first day of week
			 */
			Label label = new Label(group, SWT.NONE);
			label.setText(Messages.Pref_General_Label_FirstDayOfWeek);
			label.setToolTipText(Messages.Pref_General_Label_FirstDayOfWeek_Tooltip);

			_comboFirstDay = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
			_comboFirstDay.setVisibleItemCount(10);
			_comboFirstDay.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onSelectCalendarWeek();
				}
			});

			// fill combo, first week day entry is ignore
			final String[] weekDays = DateFormatSymbols.getInstance().getWeekdays();
			for (int dayIndex = 1; dayIndex < weekDays.length; dayIndex++) {

				String weekDay = weekDays[dayIndex];

				if (dayIndex == Calendar.MONDAY) {
					// add iso marker
					weekDay = weekDay + " - " + Messages.App_Label_ISO8601;// + ")"; //$NON-NLS-1$
				}
				_comboFirstDay.add(weekDay);
			}

			/*
			 * minimal days in first week
			 */
			label = new Label(group, SWT.NONE);
			label.setText(Messages.Pref_General_Label_MinimalDaysInFirstWeek);
			label.setToolTipText(Messages.Pref_General_Label_MinimalDaysInFirstWeek_Tooltip);

			_comboMinDaysInFirstWeek = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
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
			final Button button = new Button(group, SWT.NONE);
			GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.FILL).span(2, 1).applyTo(button);
			button.setText(Messages.Pref_General_Button_ComputeCalendarWeek);
			button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					onComputeCalendarWeek();
				}
			});
		}
	}

	private void createUI_40_Notes(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(group);
		group.setText(Messages.Pref_General_Notes);
		group.setToolTipText(Messages.Pref_General_Notes_Tooltip);
		GridLayoutFactory.swtDefaults().applyTo(group);
		{
			_txtNotes = new Text(group, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
			GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtNotes);
		}
	}

	private void createUI_99_LiveUpdate(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.indent(0, _pc.convertVerticalDLUsToPixels(8))
				.span(3, 1)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			/*
			 * Checkbox: live update
			 */
			_chkLiveUpdate = new Button(container, SWT.CHECK);
			_chkLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
			_chkLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
			_chkLiveUpdate.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					doTimeZoneLiveUpdate();
				}
			});
		}
	}

	private void doTimeZoneLiveUpdate() {

		if (_chkLiveUpdate.getSelection()) {
			performApply();
		}
	}

	private void enableControls() {

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

		checkCalendarWeek();

		return super.okToLeave();
	}

	/**
	 * compute calendar week for all tours
	 */
	private void onComputeCalendarWeek() {

		saveState();

		_currentFirstDayOfWeek = _backupFirstDayOfWeek = _prefStore
				.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		_currentMinimalDaysInFirstWeek = _backupMinimalDaysInFirstWeek = _prefStore
				.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
			@Override
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

				Connection conn = null;
				try {
					conn = TourDatabase.getInstance().getConnection();
					TourDatabase.updateTourWeek(conn, monitor, _backupFirstDayOfWeek, _backupMinimalDaysInFirstWeek);
				} catch (final SQLException e) {
					UI.showSQLException(e);
				} finally {
					if (conn != null) {
						try {
							conn.close();
						} catch (final SQLException e) {
							UI.showSQLException(e);
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

	/*
	 * this seems not to work correctly and is disabled since version 10.3
	 */
//	private void createUI20Confirmations(final Composite parent) {
//
//		final Group group = new Group(parent, SWT.NONE);
//		group.setText(Messages.pref_general_confirmation);
//		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
//		{
//			// checkbox: confirm undo in tour editor
//			addField(new BooleanFieldEditor(
//					ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR,
//					Messages.pref_general_hide_confirmation + Messages.tour_editor_dlg_revert_tour_message,
//					group));
//
//			// checkbox: confirm undo in tour editor
//			addField(new BooleanFieldEditor(
//					ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING,
//					Messages.pref_general_hide_warning
//					/*
//					 * the externalize string wizard has problems when the messages are from 2
//					 * different
//					 * packages, Eclipse 3.4
//					 */
//					+ Messages.map_dlg_dim_warning_message//
//					//The map is dimmed, this can be the reason when the map is not visible.
//					,
//					group));
//		}
//
//		// set margins after the editors are added
//		GridLayoutFactory.swtDefaults().applyTo(group);
//	}

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
	protected void performDefaults() {

		// live update
		_chkLiveUpdate.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TIME_ZONE_IS_LIVE_UPDATE));

		// timezone
		_timeZoneId = _prefStore.getDefaultString(ITourbookPreferences.TIME_ZONE_LOCAL_ID);
		_timeZoneOffset = _prefStore.getDefaultInt(ITourbookPreferences.TIME_ZONE_LOCAL_OFFSET);

		validateTimeZoneId();
		doTimeZoneLiveUpdate();

		// calendar week
		_backupFirstDayOfWeek = //
		_currentFirstDayOfWeek = _prefStore.getDefaultInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

		_backupMinimalDaysInFirstWeek = //
		_currentMinimalDaysInFirstWeek = _prefStore
				.getDefaultInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		updateUI_CalendarWeek();
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK) {

			checkCalendarWeek();

			saveState();

			if (_editShowMeasurementInUI.getBooleanValue() != _showMeasurementSystemInUI) {

				// field was modified, ask for restart

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

		// timezone
		_chkLiveUpdate.setSelection(_prefStore.getBoolean(ITourbookPreferences.TIME_ZONE_IS_LIVE_UPDATE));
		_timeZoneId = _prefStore.getString(ITourbookPreferences.TIME_ZONE_LOCAL_ID);
		_timeZoneOffset = _prefStore.getInt(ITourbookPreferences.TIME_ZONE_LOCAL_OFFSET);
		validateTimeZoneId();

		// calendar week
		_backupFirstDayOfWeek = //
		_currentFirstDayOfWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

		_backupMinimalDaysInFirstWeek = //
		_currentMinimalDaysInFirstWeek = _prefStore.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		updateUI_CalendarWeek();

		// general
		_txtNotes.setText(_prefStore.getString(ITourbookPreferences.GENERAL_NOTES));

		// measurement system
		MeasurementSystemContributionItem.selectSystemFromPrefStore(_comboSystem);
		onSelectSystem();
	}

	private void saveState() {

		// timezone
		_prefStore.setValue(ITourbookPreferences.TIME_ZONE_IS_LIVE_UPDATE, _chkLiveUpdate.getSelection());
		_prefStore.setValue(ITourbookPreferences.TIME_ZONE_LOCAL_ID, _timeZoneId);
		_prefStore.setValue(ITourbookPreferences.TIME_ZONE_LOCAL_OFFSET, _timeZoneOffset);

		// calendar week
		final int firstDayOfWeek = _comboFirstDay.getSelectionIndex() + 1;
		final int minDays = _comboMinDaysInFirstWeek.getSelectionIndex() + 1;

		_prefStore.setValue(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK, firstDayOfWeek);
		_prefStore.setValue(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK, minDays);

		// general
		_prefStore.setValue(ITourbookPreferences.GENERAL_NOTES, _txtNotes.getText());

		// measurement system
		int selectedIndex = _comboSystem.getSelectionIndex();
		if (selectedIndex == -1) {
			selectedIndex = 0;
		}

		MeasurementSystemContributionItem.selectSystemInPrefStore(selectedIndex);
	}

	protected void updateUI_CalendarWeek() {

		_comboFirstDay.select(_backupFirstDayOfWeek - 1);
		_comboMinDaysInFirstWeek.select(_backupMinimalDaysInFirstWeek - 1);
	}

	private void updateUI_TimeZone() {

		final int selectedTimeZoneIndex = _comboTimeZone.getSelectionIndex();

		if (selectedTimeZoneIndex == -1) {
			return;
		}

		final ArrayList<TimeZone> allTimeZone = TimeZoneUtils.getAllTimeZones();
		final TimeZone selectedTimeZone = allTimeZone.get(selectedTimeZoneIndex);

		// update model
		_timeZoneId = selectedTimeZone.zoneId;
		_timeZoneOffset = selectedTimeZone.zoneOffset;

		updateUI_TimeZone_Offset();
	}

	private void updateUI_TimeZone_Offset() {

		_lblTimeZoneOffset.setText(TimeZoneUtils.printOffset(_timeZoneOffset));
	}

	/**
	 * Ensure {@link #_timeZoneId} is valid.
	 */
	private void validateTimeZoneId() {

		final int timeZoneIndex = TimeZoneUtils.getTimeZoneIndex(_timeZoneId);

		if (timeZoneIndex == -1) {

			final TimeZone firstTimeZone = TimeZoneUtils.getAllTimeZones().get(0);

			_timeZoneId = firstTimeZone.zoneId;
			_timeZoneOffset = firstTimeZone.zoneOffset;
		}

		_comboTimeZone.select(timeZoneIndex);

		updateUI_TimeZone_Offset();
	}
}
