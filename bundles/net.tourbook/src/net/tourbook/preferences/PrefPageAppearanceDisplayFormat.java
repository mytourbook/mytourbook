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

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.FormatManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceDisplayFormat extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID						= "net.tourbook.preferences.PrefPageAppearanceDisplayFormat";	//$NON-NLS-1$

	public static final String		DISPLAY_FORMAT_1		= "1";															//$NON-NLS-1$
	public static final String		DISPLAY_FORMAT_1_1		= "1.1";														//$NON-NLS-1$
	public static final String		DISPLAY_FORMAT_1_2		= "1.2";														//$NON-NLS-1$
	public static final String		DISPLAY_FORMAT_CAL		= "cal";														//$NON-NLS-1$
	public static final String		DISPLAY_FORMAT_KCAL		= "kcal";														//$NON-NLS-1$
	public static final String		DISPLAY_FORMAT_HH_MM	= "hh_mm";														//$NON-NLS-1$
	public static final String		DISPLAY_FORMAT_HH_MM_SS	= "hh_mm_ss";													//$NON-NLS-1$

	private final IPreferenceStore	_prefStore				= TourbookPlugin.getPrefStore();

	/*
	 * UI tools
	 */
	private SelectionAdapter		_defaultSelectionListener;

	private Button					_chkLiveUpdate;

	private Button					_rdoAvg_Cadence_1;
	private Button					_rdoAvg_Cadence_1_1;
	private Button					_rdoAvg_Cadence_1_2;
	private Button					_rdoAvg_Power_1;
	private Button					_rdoAvg_Power_1_1;
	private Button					_rdoAvg_Pulse_1;
	private Button					_rdoAvg_Pulse_1_1;

	private Button					_rdoTime_Driving_hh_mm;
	private Button					_rdoTime_Driving_hh_mm_ss;
	private Button					_rdoTime_Paused_hh_mm;
	private Button					_rdoTime_Paused_hh_mm_ss;
	private Button					_rdoTime_Recording_hh_mm;
	private Button					_rdoTime_Recording_hh_mm_ss;

	private Button					_rdoValue_Calories_Cal;
	private Button					_rdoValue_Calories_Kcal;

	/*
	 * UI controls
	 */

	public PrefPageAppearanceDisplayFormat() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUITools(parent);

		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 15).applyTo(container);
		{
			createUI_20_Formats(container);
			createUI_99_LiveUpdate(container);
		}

		return container;
	}

	private void createUI_20_Formats(final Composite parent) {

		/*
		 * group: Formats
		 */
		final Group group = new Group(parent, SWT.NONE);
		group.setText(Messages.Pref_DisplayFormat_Group_Format);
//		group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		GridLayoutFactory.swtDefaults()//
				.numColumns(2)
				.spacing(20, 5)
				.applyTo(group);
		{
			{
				/*
				 * Pulse: bpm
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_DisplayFormat_Label_Pulse);

				final Composite container = new Composite(group, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
				{
					_rdoAvg_Pulse_1 = new Button(container, SWT.RADIO);
					_rdoAvg_Pulse_1.setText(Messages.Pref_DisplayFormat_Radio_Format_1);
					_rdoAvg_Pulse_1.addSelectionListener(_defaultSelectionListener);

					_rdoAvg_Pulse_1_1 = new Button(container, SWT.RADIO);
					_rdoAvg_Pulse_1_1.setText(Messages.Pref_DisplayFormat_Radio_Format_1_1);
					_rdoAvg_Pulse_1_1.addSelectionListener(_defaultSelectionListener);
				}
			}

			{
				/*
				 * Power: W
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_DisplayFormat_Label_Power);

				final Composite container = new Composite(group, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
				{
					_rdoAvg_Power_1 = new Button(container, SWT.RADIO);
					_rdoAvg_Power_1.setText(Messages.Pref_DisplayFormat_Radio_Format_1);
					_rdoAvg_Power_1.addSelectionListener(_defaultSelectionListener);

					_rdoAvg_Power_1_1 = new Button(container, SWT.RADIO);
					_rdoAvg_Power_1_1.setText(Messages.Pref_DisplayFormat_Radio_Format_1_1);
					_rdoAvg_Power_1_1.addSelectionListener(_defaultSelectionListener);
				}
			}

			{
				/*
				 * Cadence: rpm/spm
				 */
				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_DisplayFormat_Label_Cadence);

				final Composite container = new Composite(group, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
				{
					_rdoAvg_Cadence_1 = new Button(container, SWT.RADIO);
					_rdoAvg_Cadence_1.setText(Messages.Pref_DisplayFormat_Radio_Format_1);
					_rdoAvg_Cadence_1.addSelectionListener(_defaultSelectionListener);

					_rdoAvg_Cadence_1_1 = new Button(container, SWT.RADIO);
					_rdoAvg_Cadence_1_1.setText(Messages.Pref_DisplayFormat_Radio_Format_1_1);
					_rdoAvg_Cadence_1_1.addSelectionListener(_defaultSelectionListener);

					_rdoAvg_Cadence_1_2 = new Button(container, SWT.RADIO);
					_rdoAvg_Cadence_1_2.setText(Messages.Pref_DisplayFormat_Radio_Format_1_2);
					_rdoAvg_Cadence_1_2.addSelectionListener(_defaultSelectionListener);
				}
			}

			{
				/*
				 * Calories: cal/kcal
				 */

				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_DisplayFormat_Label_Calories);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

				final Composite container = new Composite(group, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
				GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
				{
					_rdoValue_Calories_Kcal = new Button(container, SWT.RADIO);
					_rdoValue_Calories_Kcal.setText(Messages.Pref_DisplayFormat_Radio_Calories_Format_Kcal);
					_rdoValue_Calories_Kcal.addSelectionListener(_defaultSelectionListener);

					_rdoValue_Calories_Cal = new Button(container, SWT.RADIO);
					_rdoValue_Calories_Cal.setText(Messages.Pref_DisplayFormat_Radio_Calories_Format_Cal);
					_rdoValue_Calories_Cal.addSelectionListener(_defaultSelectionListener);
				}
			}

			{
				/*
				 * Recording time format: hh:mm
				 */

				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.pref_view_layout_label_recording_time_format);

				final Composite container = new Composite(group, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
				{
					_rdoTime_Recording_hh_mm = new Button(container, SWT.RADIO);
					_rdoTime_Recording_hh_mm.setText(Messages.pref_view_layout_label_format_hh_mm);
					_rdoTime_Recording_hh_mm.addSelectionListener(_defaultSelectionListener);

					_rdoTime_Recording_hh_mm_ss = new Button(container, SWT.RADIO);
					_rdoTime_Recording_hh_mm_ss.setText(Messages.pref_view_layout_label_format_hh_mm_ss);
					_rdoTime_Recording_hh_mm_ss.addSelectionListener(_defaultSelectionListener);
				}
			}

			{
				/*
				 * Driving time format: hh:mm
				 */

				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.pref_view_layout_label_driving_time_format);

				final Composite container = new Composite(group, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
				{
					_rdoTime_Driving_hh_mm = new Button(container, SWT.RADIO);
					_rdoTime_Driving_hh_mm.setText(Messages.pref_view_layout_label_format_hh_mm);
					_rdoTime_Driving_hh_mm.addSelectionListener(_defaultSelectionListener);

					_rdoTime_Driving_hh_mm_ss = new Button(container, SWT.RADIO);
					_rdoTime_Driving_hh_mm_ss.setText(Messages.pref_view_layout_label_format_hh_mm_ss);
					_rdoTime_Driving_hh_mm_ss.addSelectionListener(_defaultSelectionListener);
				}
			}

			{
				/*
				 * Paused time format: hh:mm
				 */

				final Label label = new Label(group, SWT.NONE);
				label.setText(Messages.Pref_DisplayFormat_Label_PausedTime);

				final Composite container = new Composite(group, SWT.NONE);
				GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
				{
					_rdoTime_Paused_hh_mm = new Button(container, SWT.RADIO);
					_rdoTime_Paused_hh_mm.setText(Messages.pref_view_layout_label_format_hh_mm);
					_rdoTime_Paused_hh_mm.addSelectionListener(_defaultSelectionListener);

					_rdoTime_Paused_hh_mm_ss = new Button(container, SWT.RADIO);
					_rdoTime_Paused_hh_mm_ss.setText(Messages.pref_view_layout_label_format_hh_mm_ss);
					_rdoTime_Paused_hh_mm_ss.addSelectionListener(_defaultSelectionListener);
				}
			}
		}
	}

	private void createUI_99_LiveUpdate(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			/*
			 * Checkbox: live update
			 */
			_chkLiveUpdate = new Button(container, SWT.CHECK);
			_chkLiveUpdate.setText(Messages.Pref_LiveUpdate_Checkbox);
			_chkLiveUpdate.setToolTipText(Messages.Pref_LiveUpdate_Checkbox_Tooltip);
			_chkLiveUpdate.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void doLiveUpdate() {

		if (_chkLiveUpdate.getSelection()) {
			performApply();
		}
	}

	@Override
	public void init(final IWorkbench workbench) {
		setPreferenceStore(_prefStore);
	}

	private void initUITools(final Composite parent) {

		_defaultSelectionListener = new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				onSelection();
			}
		};
	}

	private void onSelection() {

		doLiveUpdate();
	}

	@Override
	protected void performApply() {

		saveState();

		super.performApply();
	}

	@Override
	protected void performDefaults() {

		final boolean isDefaultLiveUpdate = _prefStore.getDefaultBoolean(//
				ITourbookPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE);

		final String defaultCadence = _prefStore.getDefaultString(ITourbookPreferences.DISPLAY_FORMAT_AVG_CADENCE);
		final String defaultCalories = _prefStore.getDefaultString(ITourbookPreferences.DISPLAY_FORMAT_CALORIES);
		final String defaultPower = _prefStore.getDefaultString(ITourbookPreferences.DISPLAY_FORMAT_AVG_POWER);
		final String defaultPulse = _prefStore.getDefaultString(ITourbookPreferences.DISPLAY_FORMAT_AVG_PULSE);

		final String defaultDrivingTime = _prefStore.getDefaultString(ITourbookPreferences.DISPLAY_FORMAT_DRIVING_TIME);
		final String defaultPausedTime = _prefStore.getDefaultString(ITourbookPreferences.DISPLAY_FORMAT_PAUSED_TIME);
		final String defaultRecordingTime = _prefStore.getDefaultString(//
				ITourbookPreferences.DISPLAY_FORMAT_RECORDING_TIME);

		final boolean isCadence_1 = DISPLAY_FORMAT_1.equals(defaultCadence);
		final boolean isCadence_1_1 = DISPLAY_FORMAT_1_1.equals(defaultCadence);
		final boolean isCadence_1_2 = DISPLAY_FORMAT_1_2.equals(defaultCadence);
		final boolean isCalories_Kcal = DISPLAY_FORMAT_KCAL.equals(defaultCalories);
		final boolean isDriving_hh_mm = DISPLAY_FORMAT_HH_MM.equals(defaultDrivingTime);
		final boolean isPaused_hh_mm = DISPLAY_FORMAT_HH_MM.equals(defaultPausedTime);
		final boolean isRecording_hh_mm = DISPLAY_FORMAT_HH_MM.equals(defaultRecordingTime);
		final boolean isPower_1 = DISPLAY_FORMAT_1.equals(defaultPower);
		final boolean isPulse_1 = DISPLAY_FORMAT_1.equals(defaultPulse);

		_rdoAvg_Cadence_1.setSelection(isCadence_1);
		_rdoAvg_Cadence_1_1.setSelection(isCadence_1_1);
		_rdoAvg_Cadence_1_2.setSelection(isCadence_1_2);

		_rdoAvg_Power_1.setSelection(isPower_1);
		_rdoAvg_Power_1_1.setSelection(!isPower_1);

		_rdoAvg_Pulse_1.setSelection(isPulse_1);
		_rdoAvg_Pulse_1_1.setSelection(!isPulse_1);

		_rdoValue_Calories_Kcal.setSelection(isCalories_Kcal);
		_rdoValue_Calories_Cal.setSelection(!isCalories_Kcal);

		_rdoTime_Driving_hh_mm.setSelection(isDriving_hh_mm);
		_rdoTime_Driving_hh_mm_ss.setSelection(!isDriving_hh_mm);

		_rdoTime_Paused_hh_mm.setSelection(isPaused_hh_mm);
		_rdoTime_Paused_hh_mm_ss.setSelection(!isPaused_hh_mm);

		_rdoTime_Recording_hh_mm.setSelection(isRecording_hh_mm);
		_rdoTime_Recording_hh_mm_ss.setSelection(!isRecording_hh_mm);

		_chkLiveUpdate.setSelection(isDefaultLiveUpdate);

		super.performDefaults();

		doLiveUpdate();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		final boolean isLiveUpdate = _prefStore.getBoolean(ITourbookPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE);

		final String cadence = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_CADENCE);
		final String calories = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_CALORIES);
		final String power = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_POWER);
		final String pulse = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_AVG_PULSE);

		final String drivingTime = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_DRIVING_TIME);
		final String pausedTime = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_PAUSED_TIME);
		final String recordingTime = _prefStore.getString(ITourbookPreferences.DISPLAY_FORMAT_RECORDING_TIME);

		final boolean isCadence_1 = DISPLAY_FORMAT_1.equals(cadence);
		final boolean isCadence_1_1 = DISPLAY_FORMAT_1_1.equals(cadence);
		final boolean isCadence_1_2 = DISPLAY_FORMAT_1_2.equals(cadence);
		final boolean isPower_1 = DISPLAY_FORMAT_1.equals(power);
		final boolean isPulse_1 = DISPLAY_FORMAT_1.equals(pulse);

		final boolean isCalories_Kcal = DISPLAY_FORMAT_KCAL.equals(calories);
		final boolean isDriving_hh_mm = DISPLAY_FORMAT_HH_MM.equals(drivingTime);
		final boolean isPaused_hh_mm = DISPLAY_FORMAT_HH_MM.equals(pausedTime);
		final boolean isRecording_hh_mm = DISPLAY_FORMAT_HH_MM.equals(recordingTime);

		_rdoAvg_Cadence_1.setSelection(isCadence_1);
		_rdoAvg_Cadence_1_1.setSelection(isCadence_1_1);
		_rdoAvg_Cadence_1_2.setSelection(isCadence_1_2);

		_rdoAvg_Power_1.setSelection(isPower_1);
		_rdoAvg_Power_1_1.setSelection(!isPower_1);

		_rdoAvg_Pulse_1.setSelection(isPulse_1);
		_rdoAvg_Pulse_1_1.setSelection(!isPulse_1);

		_rdoValue_Calories_Kcal.setSelection(isCalories_Kcal);
		_rdoValue_Calories_Cal.setSelection(!isCalories_Kcal);

		_rdoTime_Driving_hh_mm.setSelection(isDriving_hh_mm);
		_rdoTime_Driving_hh_mm_ss.setSelection(!isDriving_hh_mm);

		_rdoTime_Paused_hh_mm.setSelection(isPaused_hh_mm);
		_rdoTime_Paused_hh_mm_ss.setSelection(!isPaused_hh_mm);

		_rdoTime_Recording_hh_mm.setSelection(isRecording_hh_mm);
		_rdoTime_Recording_hh_mm_ss.setSelection(!isRecording_hh_mm);

		_chkLiveUpdate.setSelection(isLiveUpdate);
	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_AVG_POWER,//
				_rdoAvg_Power_1.getSelection() //
						? DISPLAY_FORMAT_1
						: DISPLAY_FORMAT_1_1);

		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_AVG_PULSE,//
				_rdoAvg_Pulse_1.getSelection() //
						? DISPLAY_FORMAT_1
						: DISPLAY_FORMAT_1_1);

		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_AVG_CADENCE,//
				_rdoAvg_Cadence_1.getSelection() //
						? DISPLAY_FORMAT_1
						: _rdoAvg_Cadence_1_1.getSelection() //
								? DISPLAY_FORMAT_1_1
								: DISPLAY_FORMAT_1_2);

		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_CALORIES,//
				_rdoValue_Calories_Kcal.getSelection() //
						? DISPLAY_FORMAT_KCAL
						: DISPLAY_FORMAT_CAL);

		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_DRIVING_TIME,//
				_rdoTime_Driving_hh_mm.getSelection() //
						? DISPLAY_FORMAT_HH_MM
						: DISPLAY_FORMAT_HH_MM_SS);

		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_PAUSED_TIME,//
				_rdoTime_Paused_hh_mm.getSelection() //
						? DISPLAY_FORMAT_HH_MM
						: DISPLAY_FORMAT_HH_MM_SS);

		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_RECORDING_TIME,//
				_rdoTime_Recording_hh_mm.getSelection() //
						? DISPLAY_FORMAT_HH_MM
						: DISPLAY_FORMAT_HH_MM_SS);

		// live update
		_prefStore.setValue(ITourbookPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE,//
				_chkLiveUpdate.getSelection());

		/*
		 * Publish modifications
		 */
		FormatManager.updateDisplayFormats();

		// fire one event for all modifications
		_prefStore.setValue(ITourbookPreferences.VIEW_LAYOUT_CHANGED, Math.random());
	}
}
