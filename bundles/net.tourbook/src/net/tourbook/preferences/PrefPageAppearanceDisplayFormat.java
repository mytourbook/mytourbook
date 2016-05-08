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
import net.tourbook.common.CommonActivator;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.preferences.ICommonPreferences;
import net.tourbook.common.util.ColumnManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageAppearanceDisplayFormat extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID			= "net.tourbook.preferences.PrefPageAppearanceDisplayFormat";	//$NON-NLS-1$

	private final IPreferenceStore	_prefStore	= CommonActivator.getPrefStore();

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private SelectionAdapter		_defaultSelectionListener;

	private Button					_chkLiveUpdate;

	private Button					_rdoAltitude_1_0;
	private Button					_rdoAltitude_1_1;
	private Button					_rdoCadence_1_0;
	private Button					_rdoCadence_1_1;
	private Button					_rdoCadence_1_2;
	private Button					_rdoDistance_1_0;
	private Button					_rdoDistance_1_1;
	private Button					_rdoDistance_1_2;
	private Button					_rdoDistance_1_3;
	private Button					_rdoPower_1_0;
	private Button					_rdoPower_1_1;
	private Button					_rdoPulse_1_0;
	private Button					_rdoPulse_1_1;
	private Button					_rdoSpeed_1_0;
	private Button					_rdoSpeed_1_1;
	private Button					_rdoSpeed_1_2;

	private Button					_rdoTime_Driving_HH;
	private Button					_rdoTime_Driving_HH_MM;
	private Button					_rdoTime_Driving_HH_MM_SS;
	private Button					_rdoTime_Paused_HH;
	private Button					_rdoTime_Paused_HH_MM;
	private Button					_rdoTime_Paused_HH_MM_SS;
	private Button					_rdoTime_Recording_HH;
	private Button					_rdoTime_Recording_HH_MM;
	private Button					_rdoTime_Recording_HH_MM_SS;

	/*
	 * UI controls
	 */

	public PrefPageAppearanceDisplayFormat() {
//		noDefaultAndApplyButton();
	}

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);

		final Composite container = createUI(parent);

		restoreState();

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().spacing(5, 15).applyTo(container);
		{
			{
				/*
				 * Label: Info
				 */
				final Label label = new Label(container, SWT.NONE);
				label.setText(Messages.Pref_DisplayFormat_Label_Info);
				GridDataFactory.fillDefaults()//
						.span(2, 1)
						.applyTo(label);
			}

			createUI_20_Formats(container);
			createUI_99_LiveUpdate(container);
		}

		return container;
	}

	private void createUI_20_Formats(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(2)
				.spacing(20, 5)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_22_Time(container);
			createUI_24_Other(container);
		}
	}

	private void createUI_22_Time(final Composite parent) {

		final String formatName_HH = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH);
		final String formatName_HH_MM = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH_MM);
		final String formatName_HH_MM_SS = ColumnManager.getValueFormatterName(ValueFormat.TIME_HH_MM_SS);
		{
			/*
			 * Recording time format: hh ... hh:mm:ss
			 */

			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.pref_view_layout_label_recording_time_format);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_rdoTime_Recording_HH = new Button(container, SWT.RADIO);
				_rdoTime_Recording_HH.setText(formatName_HH);
				_rdoTime_Recording_HH.addSelectionListener(_defaultSelectionListener);

				_rdoTime_Recording_HH_MM = new Button(container, SWT.RADIO);
				_rdoTime_Recording_HH_MM.setText(formatName_HH_MM);
				_rdoTime_Recording_HH_MM.addSelectionListener(_defaultSelectionListener);

				_rdoTime_Recording_HH_MM_SS = new Button(container, SWT.RADIO);
				_rdoTime_Recording_HH_MM_SS.setText(formatName_HH_MM_SS);
				_rdoTime_Recording_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
			}
		}

		{
			/*
			 * Driving time format: hh ... hh:mm:ss
			 */

			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.pref_view_layout_label_driving_time_format);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_rdoTime_Driving_HH = new Button(container, SWT.RADIO);
				_rdoTime_Driving_HH.setText(formatName_HH);
				_rdoTime_Driving_HH.addSelectionListener(_defaultSelectionListener);

				_rdoTime_Driving_HH_MM = new Button(container, SWT.RADIO);
				_rdoTime_Driving_HH_MM.setText(formatName_HH_MM);
				_rdoTime_Driving_HH_MM.addSelectionListener(_defaultSelectionListener);

				_rdoTime_Driving_HH_MM_SS = new Button(container, SWT.RADIO);
				_rdoTime_Driving_HH_MM_SS.setText(formatName_HH_MM_SS);
				_rdoTime_Driving_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
			}
		}

		{
			/*
			 * Paused time format: hh ... hh:mm:ss
			 */

			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Pref_DisplayFormat_Label_BreakTime);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_rdoTime_Paused_HH = new Button(container, SWT.RADIO);
				_rdoTime_Paused_HH.setText(formatName_HH);
				_rdoTime_Paused_HH.addSelectionListener(_defaultSelectionListener);

				_rdoTime_Paused_HH_MM = new Button(container, SWT.RADIO);
				_rdoTime_Paused_HH_MM.setText(formatName_HH_MM);
				_rdoTime_Paused_HH_MM.addSelectionListener(_defaultSelectionListener);

				_rdoTime_Paused_HH_MM_SS = new Button(container, SWT.RADIO);
				_rdoTime_Paused_HH_MM_SS.setText(formatName_HH_MM_SS);
				_rdoTime_Paused_HH_MM_SS.addSelectionListener(_defaultSelectionListener);
			}
		}
	}

	private void createUI_24_Other(final Composite parent) {

		final String formatName_1_0 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_0);
		final String formatName_1_1 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_1);
		final String formatName_1_2 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_2);
		final String formatName_1_3 = ColumnManager.getValueFormatterName(ValueFormat.NUMBER_1_3);

		{
			/*
			 * Pulse: m / ft
			 */
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Pref_DisplayFormat_Label_Altitude);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_rdoAltitude_1_0 = new Button(container, SWT.RADIO);
				_rdoAltitude_1_0.setText(formatName_1_0);
				_rdoAltitude_1_0.addSelectionListener(_defaultSelectionListener);

				_rdoAltitude_1_1 = new Button(container, SWT.RADIO);
				_rdoAltitude_1_1.setText(formatName_1_1);
				_rdoAltitude_1_1.addSelectionListener(_defaultSelectionListener);
			}

			// vertical indent
			final int vIndent = _pc.convertVerticalDLUsToPixels(4);
			GridDataFactory.fillDefaults().indent(0, vIndent).applyTo(label);
			GridDataFactory.fillDefaults().indent(0, vIndent).applyTo(container);
		}
		{
			/*
			 * Pulse: bpm
			 */
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Pref_DisplayFormat_Label_Pulse);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_rdoPulse_1_0 = new Button(container, SWT.RADIO);
				_rdoPulse_1_0.setText(formatName_1_0);
				_rdoPulse_1_0.addSelectionListener(_defaultSelectionListener);

				_rdoPulse_1_1 = new Button(container, SWT.RADIO);
				_rdoPulse_1_1.setText(formatName_1_1);
				_rdoPulse_1_1.addSelectionListener(_defaultSelectionListener);
			}
		}

		{
			/*
			 * Power: W
			 */
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Pref_DisplayFormat_Label_Power);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
			{
				_rdoPower_1_0 = new Button(container, SWT.RADIO);
				_rdoPower_1_0.setText(formatName_1_0);
				_rdoPower_1_0.addSelectionListener(_defaultSelectionListener);

				_rdoPower_1_1 = new Button(container, SWT.RADIO);
				_rdoPower_1_1.setText(formatName_1_1);
				_rdoPower_1_1.addSelectionListener(_defaultSelectionListener);
			}
		}

		{
			/*
			 * Cadence: rpm/spm
			 */
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Pref_DisplayFormat_Label_Cadence);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_rdoCadence_1_0 = new Button(container, SWT.RADIO);
				_rdoCadence_1_0.setText(formatName_1_0);
				_rdoCadence_1_0.addSelectionListener(_defaultSelectionListener);

				_rdoCadence_1_1 = new Button(container, SWT.RADIO);
				_rdoCadence_1_1.setText(formatName_1_1);
				_rdoCadence_1_1.addSelectionListener(_defaultSelectionListener);

				_rdoCadence_1_2 = new Button(container, SWT.RADIO);
				_rdoCadence_1_2.setText(formatName_1_2);
				_rdoCadence_1_2.addSelectionListener(_defaultSelectionListener);
			}
		}

		{
			/*
			 * Speed: km/h
			 */
			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Pref_DisplayFormat_Label_Speed);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(container);
			{
				_rdoSpeed_1_0 = new Button(container, SWT.RADIO);
				_rdoSpeed_1_0.setText(formatName_1_0);
				_rdoSpeed_1_0.addSelectionListener(_defaultSelectionListener);

				_rdoSpeed_1_1 = new Button(container, SWT.RADIO);
				_rdoSpeed_1_1.setText(formatName_1_1);
				_rdoSpeed_1_1.addSelectionListener(_defaultSelectionListener);

				_rdoSpeed_1_2 = new Button(container, SWT.RADIO);
				_rdoSpeed_1_2.setText(formatName_1_2);
				_rdoSpeed_1_2.addSelectionListener(_defaultSelectionListener);
			}
		}

		{
			/*
			 * Distance: # ... #.###
			 */

			final Label label = new Label(parent, SWT.NONE);
			label.setText(Messages.Pref_DisplayFormat_Label_Distance);
//			GridDataFactory.fillDefaults().grab(true, false).applyTo(label);

			final Composite container = new Composite(parent, SWT.NONE);
			GridLayoutFactory.fillDefaults().numColumns(4).applyTo(container);
//			GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
			{
				_rdoDistance_1_0 = new Button(container, SWT.RADIO);
				_rdoDistance_1_0.setText(formatName_1_0);
				_rdoDistance_1_0.addSelectionListener(_defaultSelectionListener);

				_rdoDistance_1_1 = new Button(container, SWT.RADIO);
				_rdoDistance_1_1.setText(formatName_1_1);
				_rdoDistance_1_1.addSelectionListener(_defaultSelectionListener);

				_rdoDistance_1_2 = new Button(container, SWT.RADIO);
				_rdoDistance_1_2.setText(formatName_1_2);
				_rdoDistance_1_2.addSelectionListener(_defaultSelectionListener);

				_rdoDistance_1_3 = new Button(container, SWT.RADIO);
				_rdoDistance_1_3.setText(formatName_1_3);
				_rdoDistance_1_3.addSelectionListener(_defaultSelectionListener);
			}
		}
	}

	private void createUI_99_LiveUpdate(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.indent(0, _pc.convertVerticalDLUsToPixels(8))
				.applyTo(container);
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

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

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

		final boolean isLiveUpdate = _prefStore.getDefaultBoolean(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE);

		final String altitude = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE);
		final String cadence = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_CADENCE);
		final String distance = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_DISTANCE);
		final String power = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_POWER);
		final String pulse = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_PULSE);
		final String speed = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_SPEED);

		final String drivingTime = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_DRIVING_TIME);
		final String pausedTime = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME);
		final String recordingTime = _prefStore.getDefaultString(ICommonPreferences.DISPLAY_FORMAT_RECORDING_TIME);

		final boolean isAltitude_1_0 = ValueFormat.NUMBER_1_0.name().equals(altitude);

		final boolean isCadence_1_0 = ValueFormat.NUMBER_1_0.name().equals(cadence);
		final boolean isCadence_1_1 = ValueFormat.NUMBER_1_1.name().equals(cadence);
		final boolean isCadence_1_2 = ValueFormat.NUMBER_1_2.name().equals(cadence);

		final boolean isDistance_1_0 = ValueFormat.NUMBER_1_0.name().equals(distance);
		final boolean isDistance_1_1 = ValueFormat.NUMBER_1_1.name().equals(distance);
		final boolean isDistance_1_2 = ValueFormat.NUMBER_1_2.name().equals(distance);
		final boolean isDistance_1_3 = ValueFormat.NUMBER_1_3.name().equals(distance);

		final boolean isPower_1_0 = ValueFormat.NUMBER_1_0.name().equals(power);
		final boolean isPulse_1_0 = ValueFormat.NUMBER_1_0.name().equals(pulse);

		final boolean isSpeed_1_0 = ValueFormat.NUMBER_1_0.name().equals(speed);
		final boolean isSpeed_1_1 = ValueFormat.NUMBER_1_1.name().equals(speed);
		final boolean isSpeed_1_2 = ValueFormat.NUMBER_1_2.name().equals(speed);

		final boolean isDriving_HH = ValueFormat.TIME_HH.name().equals(drivingTime);
		final boolean isDriving_HH_MM = ValueFormat.TIME_HH_MM.name().equals(drivingTime);
		final boolean isDriving_HH_MM_SS = ValueFormat.TIME_HH_MM_SS.name().equals(drivingTime);

		final boolean isPaused_HH = ValueFormat.TIME_HH.name().equals(pausedTime);
		final boolean isPaused_HH_MM = ValueFormat.TIME_HH_MM.name().equals(pausedTime);
		final boolean isPaused_HH_MM_SS = ValueFormat.TIME_HH_MM_SS.name().equals(pausedTime);

		final boolean isRecording_HH = ValueFormat.TIME_HH.name().equals(recordingTime);
		final boolean isRecording_HH_MM = ValueFormat.TIME_HH_MM.name().equals(recordingTime);
		final boolean isRecording_HH_MM_SS = ValueFormat.TIME_HH_MM_SS.name().equals(recordingTime);

		_rdoAltitude_1_0.setSelection(isAltitude_1_0);
		_rdoAltitude_1_1.setSelection(!isAltitude_1_0);

		_rdoCadence_1_0.setSelection(isCadence_1_0);
		_rdoCadence_1_1.setSelection(isCadence_1_1);
		_rdoCadence_1_2.setSelection(isCadence_1_2);

		_rdoDistance_1_0.setSelection(isDistance_1_0);
		_rdoDistance_1_1.setSelection(isDistance_1_1);
		_rdoDistance_1_2.setSelection(isDistance_1_2);
		_rdoDistance_1_3.setSelection(isDistance_1_3);

		_rdoPower_1_0.setSelection(isPower_1_0);
		_rdoPower_1_1.setSelection(!isPower_1_0);

		_rdoPulse_1_0.setSelection(isPulse_1_0);
		_rdoPulse_1_1.setSelection(!isPulse_1_0);

		_rdoSpeed_1_0.setSelection(isSpeed_1_0);
		_rdoSpeed_1_1.setSelection(isSpeed_1_1);
		_rdoSpeed_1_2.setSelection(isSpeed_1_2);

		_rdoTime_Driving_HH.setSelection(isDriving_HH);
		_rdoTime_Driving_HH_MM.setSelection(isDriving_HH_MM);
		_rdoTime_Driving_HH_MM_SS.setSelection(isDriving_HH_MM_SS);

		_rdoTime_Paused_HH.setSelection(isPaused_HH);
		_rdoTime_Paused_HH_MM.setSelection(isPaused_HH_MM);
		_rdoTime_Paused_HH_MM_SS.setSelection(isPaused_HH_MM_SS);

		_rdoTime_Recording_HH.setSelection(isRecording_HH);
		_rdoTime_Recording_HH_MM.setSelection(isRecording_HH_MM);
		_rdoTime_Recording_HH_MM_SS.setSelection(isRecording_HH_MM_SS);

		_chkLiveUpdate.setSelection(isLiveUpdate);

		super.performDefaults();

		doLiveUpdate();
	}

	@Override
	public boolean performOk() {

		saveState();

		return super.performOk();
	}

	private void restoreState() {

		final boolean isLiveUpdate = _prefStore.getBoolean(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE);

		final String altitude = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE);
		final String cadence = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_CADENCE);
		final String distance = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_DISTANCE);
		final String power = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_POWER);
		final String pulse = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PULSE);
		final String speed = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_SPEED);

		final String drivingTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_DRIVING_TIME);
		final String pausedTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME);
		final String recordingTime = _prefStore.getString(ICommonPreferences.DISPLAY_FORMAT_RECORDING_TIME);

		final boolean isAltitude_1_0 = ValueFormat.NUMBER_1_0.name().equals(altitude);
		final boolean isCadence_1_0 = ValueFormat.NUMBER_1_0.name().equals(cadence);
		final boolean isCadence_1_1 = ValueFormat.NUMBER_1_1.name().equals(cadence);
		final boolean isCadence_1_2 = ValueFormat.NUMBER_1_2.name().equals(cadence);

		final boolean isDistance_1_0 = ValueFormat.NUMBER_1_0.name().equals(distance);
		final boolean isDistance_1_1 = ValueFormat.NUMBER_1_1.name().equals(distance);
		final boolean isDistance_1_2 = ValueFormat.NUMBER_1_2.name().equals(distance);
		final boolean isDistance_1_3 = ValueFormat.NUMBER_1_3.name().equals(distance);

		final boolean isPower_1_0 = ValueFormat.NUMBER_1_0.name().equals(power);
		final boolean isPulse_1_0 = ValueFormat.NUMBER_1_0.name().equals(pulse);

		final boolean isSpeed_1_0 = ValueFormat.NUMBER_1_0.name().equals(speed);
		final boolean isSpeed_1_1 = ValueFormat.NUMBER_1_1.name().equals(speed);
		final boolean isSpeed_1_2 = ValueFormat.NUMBER_1_2.name().equals(speed);

		final boolean isDriving_HH = ValueFormat.TIME_HH.name().equals(drivingTime);
		final boolean isDriving_HH_MM = ValueFormat.TIME_HH_MM.name().equals(drivingTime);
		final boolean isDriving_HH_MM_SS = ValueFormat.TIME_HH_MM_SS.name().equals(drivingTime);

		final boolean isPaused_HH = ValueFormat.TIME_HH.name().equals(pausedTime);
		final boolean isPaused_HH_MM = ValueFormat.TIME_HH_MM.name().equals(pausedTime);
		final boolean isPaused_HH_MM_SS = ValueFormat.TIME_HH_MM_SS.name().equals(pausedTime);

		final boolean isRecording_HH = ValueFormat.TIME_HH.name().equals(recordingTime);
		final boolean isRecording_HH_MM = ValueFormat.TIME_HH_MM.name().equals(recordingTime);
		final boolean isRecording_HH_MM_SS = ValueFormat.TIME_HH_MM_SS.name().equals(recordingTime);

		_rdoAltitude_1_0.setSelection(isAltitude_1_0);
		_rdoAltitude_1_1.setSelection(!isAltitude_1_0);

		_rdoCadence_1_0.setSelection(isCadence_1_0);
		_rdoCadence_1_1.setSelection(isCadence_1_1);
		_rdoCadence_1_2.setSelection(isCadence_1_2);

		_rdoDistance_1_0.setSelection(isDistance_1_0);
		_rdoDistance_1_1.setSelection(isDistance_1_1);
		_rdoDistance_1_2.setSelection(isDistance_1_2);
		_rdoDistance_1_3.setSelection(isDistance_1_3);

		_rdoPower_1_0.setSelection(isPower_1_0);
		_rdoPower_1_1.setSelection(!isPower_1_0);

		_rdoPulse_1_0.setSelection(isPulse_1_0);
		_rdoPulse_1_1.setSelection(!isPulse_1_0);

		_rdoSpeed_1_0.setSelection(isSpeed_1_0);
		_rdoSpeed_1_1.setSelection(isSpeed_1_1);
		_rdoSpeed_1_2.setSelection(isSpeed_1_2);

		_rdoTime_Driving_HH.setSelection(isDriving_HH);
		_rdoTime_Driving_HH_MM.setSelection(isDriving_HH_MM);
		_rdoTime_Driving_HH_MM_SS.setSelection(isDriving_HH_MM_SS);

		_rdoTime_Paused_HH.setSelection(isPaused_HH);
		_rdoTime_Paused_HH_MM.setSelection(isPaused_HH_MM);
		_rdoTime_Paused_HH_MM_SS.setSelection(isPaused_HH_MM_SS);

		_rdoTime_Recording_HH.setSelection(isRecording_HH);
		_rdoTime_Recording_HH_MM.setSelection(isRecording_HH_MM);
		_rdoTime_Recording_HH_MM_SS.setSelection(isRecording_HH_MM_SS);

		_chkLiveUpdate.setSelection(isLiveUpdate);
	}

	private void saveState() {

		final String altitudeFormat = _rdoAltitude_1_0.getSelection() //
				? ValueFormat.NUMBER_1_0.name()
				: ValueFormat.NUMBER_1_1.name();

		final String cadenceFormat = _rdoCadence_1_0.getSelection() //
				? ValueFormat.NUMBER_1_0.name()
				: _rdoCadence_1_1.getSelection() //
						? ValueFormat.NUMBER_1_1.name()
						: ValueFormat.NUMBER_1_2.name();

		final String distanceFormat = _rdoDistance_1_0.getSelection() //
				? ValueFormat.NUMBER_1_0.name()
				: _rdoDistance_1_1.getSelection()//
						? ValueFormat.NUMBER_1_1.name()
						: _rdoDistance_1_2.getSelection() //
								? ValueFormat.NUMBER_1_2.name()
								: ValueFormat.NUMBER_1_3.name();

		final String powerFormat = _rdoPower_1_0.getSelection() //
				? ValueFormat.NUMBER_1_0.name()
				: ValueFormat.NUMBER_1_1.name();

		final String pulseFormat = _rdoPulse_1_0.getSelection() //
				? ValueFormat.NUMBER_1_0.name()
				: ValueFormat.NUMBER_1_1.name();

		final String speedFormat = _rdoSpeed_1_0.getSelection() //
				? ValueFormat.NUMBER_1_0.name()
				: _rdoSpeed_1_1.getSelection() //
						? ValueFormat.NUMBER_1_1.name()
						: ValueFormat.NUMBER_1_2.name();

		final String drivingFormat = _rdoTime_Driving_HH.getSelection() //
				? ValueFormat.TIME_HH.name()
				: _rdoTime_Driving_HH_MM.getSelection() //
						? ValueFormat.TIME_HH_MM.name()
						: ValueFormat.TIME_HH_MM_SS.name();

		final String pausedFormat = _rdoTime_Paused_HH.getSelection() //
				? ValueFormat.TIME_HH.name()
				: _rdoTime_Paused_HH_MM.getSelection() //
						? ValueFormat.TIME_HH_MM.name()
						: ValueFormat.TIME_HH_MM_SS.name();

		final String recordingFormat = _rdoTime_Recording_HH.getSelection() //
				? ValueFormat.TIME_HH.name()
				: _rdoTime_Recording_HH_MM.getSelection() //
						? ValueFormat.TIME_HH_MM.name()
						: ValueFormat.TIME_HH_MM_SS.name();

		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_ALTITUDE, altitudeFormat);
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_CADENCE, cadenceFormat);
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_DISTANCE, distanceFormat);
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_POWER, powerFormat);
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_PULSE, pulseFormat);
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_SPEED, speedFormat);

		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_DRIVING_TIME, drivingFormat);
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_PAUSED_TIME, pausedFormat);
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_RECORDING_TIME, recordingFormat);

		// live update
		_prefStore.setValue(ICommonPreferences.DISPLAY_FORMAT_IS_LIVE_UPDATE, _chkLiveUpdate.getSelection());

		/*
		 * Publish modifications
		 */
		FormatManager.updateDisplayFormats();

		// fire one event for all modifications
		_prefStore.setValue(ITourbookPreferences.VIEW_LAYOUT_CHANGED, Math.random());
	}
}
