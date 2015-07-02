/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class PrefPageImportFit extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID					= "net.tourbook.device.PrefPageFit";			//$NON-NLS-1$

	private static final String	DEGREE_CELCIUS		= "\u0394 °C";									//$NON-NLS-1$

	private static final float	TEMPERATURE_DIGITS	= 10.0f;

	private IPreferenceStore	_prefStore			= Activator.getDefault().getPreferenceStore();

	private PixelConverter		_pc;

	private SelectionAdapter	_defaultSelectionListener;

	private static PeriodType	_tourPeriodTemplate	= PeriodType.yearMonthDayTime()
													// hide these components
//																.withMinutesRemoved()
//																.withSecondsRemoved()
//																.withMillisRemoved()
													;

//	private final PeriodFormatter	_durationFormatter	= new PeriodFormatterBuilder()
//																.appendYears()
//																.appendSuffix("y ", "y ") //$NON-NLS-1$ //$NON-NLS-2$
//																.appendMonths()
//																.appendSuffix("m ", "m ") //$NON-NLS-1$ //$NON-NLS-2$
//																.appendDays()
//																.appendSuffix("d ", "d ") //$NON-NLS-1$ //$NON-NLS-2$
//																.appendHours()
//																.appendSuffix("h ", "h ") //$NON-NLS-1$ //$NON-NLS-2$
//																.toFormatter();
	{}

//	private final PeriodFormatter	_durationFormatter	= PeriodFormat.getDefault();

	/*
	 * UI controls
	 */
	private Button				_chkIgnoreLastMarker;
	private Button				_chkRemoveExceededDuration;

	private Label				_lblIgnorLastMarker_Info;
	private Label				_lblIgnorLastMarker_TimeSlices;
	private Label				_lblSplitTour_Duration;
	private Label				_lblSplitTour_DurationUnit;
	private Label				_lblSplitTour_Info;

	private Spinner				_spinnerIgnorLastMarker_TimeSlices;
	private Spinner				_spinnerExceededDuration;
	private Spinner				_spinnerTemperatureAdjustment;

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);

		final Composite ui = createUI(parent);

		restoreState();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_10_Temperature(container);
			createUI_20_IgnoreLastMarker(container);
			createUI_30_SplitTour(container);
		}

		return container;
	}

	private void createUI_10_Temperature(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		group.setText(Messages.PrefPage_Fit_Group_AdjustTemperature);
		{
			/*
			 * temperature adjustment
			 */

			// label:
			{
				final Label label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(Messages.PrefPage_Fit_Label_AdjustTemperature);
			}

			// spinner: temperature adjustment
			{
				_spinnerTemperatureAdjustment = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerTemperatureAdjustment);
				_spinnerTemperatureAdjustment.setDigits(1);
				_spinnerTemperatureAdjustment.setPageIncrement(10);
				_spinnerTemperatureAdjustment.setMinimum(-100); // - 10.0 °C
				_spinnerTemperatureAdjustment.setMaximum(100); // +10.0 °C
				_spinnerTemperatureAdjustment.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
					}
				});
			}

			// label: °C
			{
				final Label label = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(DEGREE_CELCIUS);
			}

			// label: info
			{
				final Label lblInfo = createUI_InfoLabel(group, 3);
				lblInfo.setText(Messages.PrefPage_Fit_Label_AdjustTemperature_Info);
			}
		}
	}

	private void createUI_20_IgnoreLastMarker(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		group.setText(Messages.PrefPage_Fit_Group_IgnoreLastMarker);
		{
			/*
			 * Marker
			 */

			// checkbox: ignore last marker
			{
				_chkIgnoreLastMarker = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkIgnoreLastMarker);
				_chkIgnoreLastMarker.setText(Messages.PrefPage_Fit_Checkbox_IgnoreLastMarker);
				_chkIgnoreLastMarker.addSelectionListener(_defaultSelectionListener);
			}

			// label: info
			{
				_lblIgnorLastMarker_Info = createUI_InfoLabel(group, 3);
				_lblIgnorLastMarker_Info.setText(Messages.PrefPage_Fit_Checkbox_IgnoreLastMarker_Info);
			}

			// label: ignore time slices
			{
				_lblIgnorLastMarker_TimeSlices = new Label(group, SWT.NONE);
				_lblIgnorLastMarker_TimeSlices.setText(Messages.PrefPage_Fit_Label_IgnoredTimeSlices);
			}

			// spinner
			{
				_spinnerIgnorLastMarker_TimeSlices = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerIgnorLastMarker_TimeSlices);
				_spinnerIgnorLastMarker_TimeSlices.setMinimum(0);
				_spinnerIgnorLastMarker_TimeSlices.setMaximum(1000);
				_spinnerIgnorLastMarker_TimeSlices.setPageIncrement(10);
				_spinnerIgnorLastMarker_TimeSlices.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
					}
				});
			}
		}
	}

	private void createUI_30_SplitTour(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
		group.setText(Messages.PrefPage_Fit_Group_SplitTour);
		{
			// checkbox
			{
				_chkRemoveExceededDuration = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults().span(3, 1).applyTo(_chkRemoveExceededDuration);
				_chkRemoveExceededDuration.setText(Messages.PrefPage_Fit_Checkbox_SplitTour);
				_chkRemoveExceededDuration.addSelectionListener(_defaultSelectionListener);
			}

			// label: info
			{
				_lblSplitTour_Info = createUI_InfoLabel(group, 3);
				_lblSplitTour_Info.setText(Messages.PrefPage_Fit_Checkbox_SplitTour_Info);
			}

			// label: duration
			{
				_lblSplitTour_Duration = new Label(group, SWT.NONE);
				_lblSplitTour_Duration.setText(Messages.PrefPage_Fit_Label_SplitTour_Duration);
			}

			// spinner
			{
				_spinnerExceededDuration = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerExceededDuration);
				_spinnerExceededDuration.setMinimum(0);
				_spinnerExceededDuration.setMaximum(Integer.MAX_VALUE);
				_spinnerExceededDuration.setPageIncrement(3600); // 60*60 = 1 hour
				_spinnerExceededDuration.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
						updateUI_SplitTour();
					}
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
				_lblSplitTour_DurationUnit = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.grab(true, false)
						.align(SWT.FILL, SWT.CENTER)
						.applyTo(_lblSplitTour_DurationUnit);
			}
		}
	}

	private Label createUI_InfoLabel(final Composite parent, final int horizontalSpan) {

		final Label lblInfo = new Label(parent, SWT.WRAP);
		GridDataFactory.fillDefaults()//
				.span(horizontalSpan, 1)
				.grab(true, false)
				.hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
				.indent(0, _pc.convertVerticalDLUsToPixels(2))
				.applyTo(lblInfo);

		return lblInfo;
	}

	private void enableControls() {

		final boolean isSplitTour = _chkRemoveExceededDuration.getSelection();
		final boolean isIgnorLastMarker = _chkIgnoreLastMarker.getSelection();

		_lblIgnorLastMarker_Info.setEnabled(isIgnorLastMarker);
		_lblIgnorLastMarker_TimeSlices.setEnabled(isIgnorLastMarker);
		_spinnerIgnorLastMarker_TimeSlices.setEnabled(isIgnorLastMarker);

		_lblSplitTour_DurationUnit.setEnabled(isSplitTour);
		_lblSplitTour_Duration.setEnabled(isSplitTour);
		_lblSplitTour_Info.setEnabled(isSplitTour);
		_spinnerExceededDuration.setEnabled(isSplitTour);

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
	protected void performDefaults() {

		final float temperatureAdjustment = _prefStore.getDefaultFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);
		_spinnerTemperatureAdjustment.setSelection((int) (temperatureAdjustment * TEMPERATURE_DIGITS));

		_chkIgnoreLastMarker.setSelection(//
				_prefStore.getDefaultBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER));
		_spinnerIgnorLastMarker_TimeSlices.setSelection(//
				_prefStore.getDefaultInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES));

		_chkRemoveExceededDuration.setSelection(//
				_prefStore.getDefaultBoolean(IPreferences.FIT_IS_REMOVE_EXCEEDED_TIME_SLICE));
		_spinnerExceededDuration.setSelection(//
				_prefStore.getDefaultInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION));

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

		final float temperatureAdjustment = _prefStore.getFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);
		_spinnerTemperatureAdjustment.setSelection(//
				(int) (temperatureAdjustment * TEMPERATURE_DIGITS));

		_chkIgnoreLastMarker.setSelection(//
				_prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER));
		_spinnerIgnorLastMarker_TimeSlices.setSelection(//
				_prefStore.getInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES));

		_chkRemoveExceededDuration.setSelection(//
				_prefStore.getBoolean(IPreferences.FIT_IS_REMOVE_EXCEEDED_TIME_SLICE));
		_spinnerExceededDuration.setSelection(//
				_prefStore.getInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION));

		enableControls();
	}

	private void saveState() {

		final float temperatureAdjustment = _spinnerTemperatureAdjustment.getSelection() / TEMPERATURE_DIGITS;
		_prefStore.setValue(//
				IPreferences.FIT_TEMPERATURE_ADJUSTMENT,
				temperatureAdjustment);

		_prefStore.setValue(//
				IPreferences.FIT_IS_IGNORE_LAST_MARKER,
				_chkIgnoreLastMarker.getSelection());
		_prefStore.setValue(
				IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES,
				_spinnerIgnorLastMarker_TimeSlices.getSelection());

		_prefStore.setValue(//
				IPreferences.FIT_IS_REMOVE_EXCEEDED_TIME_SLICE,
				_chkRemoveExceededDuration.getSelection());
		_prefStore.setValue(//
				IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION,
				_spinnerExceededDuration.getSelection());
	}

	private void updateUI_SplitTour() {

		final long duration = _spinnerExceededDuration.getSelection();

		final Period tourPeriod = new Period(0, duration * 1000, _tourPeriodTemplate);

		_lblSplitTour_DurationUnit.setText(tourPeriod.toString(UI.DEFAULT_DURATION_FORMATTER));
	}
}
