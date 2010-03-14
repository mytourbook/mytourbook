/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import java.util.Calendar;

import net.tourbook.Messages;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.ui.BooleanFieldEditor2;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

public class PrefPageGeneral extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID	= "net.tourbook.preferences.PrefPageGeneralId"; //$NON-NLS-1$

	private boolean				_showMeasurementSystemInUI;
	private BooleanFieldEditor2	_editShowMeasurementInUI;

	private Combo				_comboFirstDay;
	private Combo				_comboMinDaysInFirstWeek;

	private int					_backupFirstDayOfWeek;
	private int					_backupMinimalDaysInFirstWeek;
	private int					_currentFirstDayOfWeek;
	private int					_currentMinimalDaysInFirstWeek;

	/**
	 * check if the user has changed calendar week and if the tour data are inconsistent
	 */
	private void checkCalendarWeek() {

		if (_backupFirstDayOfWeek != _currentFirstDayOfWeek
				| _backupMinimalDaysInFirstWeek != _currentMinimalDaysInFirstWeek) {

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
		GridLayoutFactory.fillDefaults().applyTo(parent);

		createUI10MeasurementSystem(parent);
		createUI20Confirmations(parent);
		createUI30WeekNumber(parent);

		restoreState();
	}

	private void createUI10MeasurementSystem(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_general_system_measurement);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			// radio: altitude
			addField(new RadioGroupFieldEditor(
					ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
					Messages.Pref_general_system_altitude,
					2,
					new String[][] {
							new String[] {
									Messages.Pref_general_metric_unit_m,
									ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_M },
							new String[] {
									Messages.Pref_general_imperial_unit_feet,
									ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_FOOT }, },
					group,
					false));

			// radio: distance
			addField(new RadioGroupFieldEditor(
					ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
					Messages.Pref_general_system_distance,
					2,
					new String[][] {
							new String[] {
									Messages.Pref_general_metric_unit_km,
									ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM },
							new String[] {
									Messages.Pref_general_imperial_unit_mi,
									ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_MI }, },
					group,
					false));

			// radio: temperature
			addField(new RadioGroupFieldEditor(
					ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
					Messages.Pref_general_system_temperature,
					2,
					new String[][] {
							new String[] {
									Messages.Pref_general_metric_unit_celcius,
									ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE_C },
							new String[] {
									Messages.Pref_general_imperial_unit_fahrenheit,
									ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPTERATURE_F }, },
					group,
					false));

			// checkbox: show in UI
			addField(_editShowMeasurementInUI = new BooleanFieldEditor2(
					ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI,
					Messages.Pref_general_show_system_in_ui,
					group));

			final GridData gd = (GridData) _editShowMeasurementInUI.getChangeControl(group).getLayoutData();
			gd.horizontalSpan = 2;

		}

		// force layout after the fields are set !!!
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
	}

	private void createUI20Confirmations(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.pref_general_confirmation);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		{
			// checkbox: confirm undo in tour editor
			addField(new BooleanFieldEditor(
					ITourbookPreferences.TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR,
					Messages.pref_general_hide_confirmation + Messages.tour_editor_dlg_revert_tour_message,
					group));

			// checkbox: confirm undo in tour editor
			addField(new BooleanFieldEditor(
					ITourbookPreferences.MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING,
					Messages.pref_general_hide_warning
					/*
					 * the externalize string wizard has problems when the messages are from 2
					 * different
					 * packages, Eclipse 3.4
					 */
					+ Messages.map_dlg_dim_warning_message//
					//The map is dimmed, this can be the reason when the map is not visible.
					,
					group));
		}

		// set margins after the editors are added
		GridLayoutFactory.swtDefaults().applyTo(group);
	}

	private void createUI30WeekNumber(final Composite parent) {

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
					weekDay = weekDay + " - " + Messages.App_Label_ISO8601;// + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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
					dayText = Integer.toString(dayIndex) + " - " + Messages.App_Label_ISO8601;// + ")"; //$NON-NLS-1$ //$NON-NLS-2$
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

	public void init(final IWorkbench workbench) {
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(prefStore);

		_showMeasurementSystemInUI = prefStore.getBoolean(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI);
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

		final IPreferenceStore prefStore = getPreferenceStore();

		_currentFirstDayOfWeek = _backupFirstDayOfWeek = prefStore
				.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);
		_currentMinimalDaysInFirstWeek = _backupMinimalDaysInFirstWeek = prefStore
				.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		final IRunnableWithProgress runnable = new IRunnableWithProgress() {
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
		prefStore.setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());
	}

	private void onSelectCalendarWeek() {

		_currentFirstDayOfWeek = _comboFirstDay.getSelectionIndex() + 1;
		_currentMinimalDaysInFirstWeek = _comboMinDaysInFirstWeek.getSelectionIndex() + 1;
	}

	@Override
	public boolean performOk() {

		final boolean isOK = super.performOk();
		if (isOK) {

			checkCalendarWeek();

			saveState();

			// fire one event for all modified measurement values
			getPreferenceStore().setValue(ITourbookPreferences.MEASUREMENT_SYSTEM, Math.random());

			if (_editShowMeasurementInUI.getBooleanValue() != _showMeasurementSystemInUI) {

				// field was modified, ask for restart

				if (MessageDialog.openQuestion(
						Display.getDefault().getActiveShell(),
						Messages.pref_general_restart_app_title,
						Messages.pref_general_restart_app_message)) {

					Display.getCurrent().asyncExec(new Runnable() {
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

		final IPreferenceStore prefStore = getPreferenceStore();

		_backupFirstDayOfWeek = _currentFirstDayOfWeek = prefStore
				.getInt(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK);

		_backupMinimalDaysInFirstWeek = _currentMinimalDaysInFirstWeek = prefStore
				.getInt(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK);

		_comboFirstDay.select(_backupFirstDayOfWeek - 1);
		_comboMinDaysInFirstWeek.select(_backupMinimalDaysInFirstWeek - 1);
	}

	private void saveState() {

		final IPreferenceStore prefStore = getPreferenceStore();

		final int firstDayOfWeek = _comboFirstDay.getSelectionIndex() + 1;
		final int minDays = _comboMinDaysInFirstWeek.getSelectionIndex() + 1;

		prefStore.setValue(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK, firstDayOfWeek);
		prefStore.setValue(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK, minDays);
	}
}
