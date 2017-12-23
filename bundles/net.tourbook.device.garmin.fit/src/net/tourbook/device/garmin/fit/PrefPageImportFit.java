/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.joda.time.Period;
import org.joda.time.PeriodType;

public class PrefPageImportFit extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID								= "net.tourbook.device.PrefPageFit";			//$NON-NLS-1$

	private static final String	STATE_FIT_IMPORT_SELECTED_TAB	= "STATE_FIT_IMPORT_SELECTED_TAB";				//$NON-NLS-1$

	private static final String	DEGREE_CELCIUS					= "\u0394 \u00b0C";								//$NON-NLS-1$

	private static final float	TEMPERATURE_DIGITS				= 10.0f;

	private static final int	TAB_FOLDER_SPEED				= 0;
	private static final int	TAB_FOLDER_TEMPERATURE			= 1;
	private static final int	TAB_FOLDER_MARKER_FILTER		= 2;
	private static final int	TAB_FOLDER_TIME_SLIZE			= 3;

	private static PeriodType	_tourPeriodTemplate				= PeriodType.yearMonthDayTime()

//			// hide these components
			.withMillisRemoved();

	private IPreferenceStore	_prefStore						= Activator.getDefault().getPreferenceStore();

	private PixelConverter		_pc;

	private SelectionAdapter	_defaultSelectionListener;

	/*
	 * UI controls
	 */
	private Button				_chkIgnoreLastMarker;
	private Button				_chkIgnoreSpeedValues;
	private Button				_chkRemoveExceededDuration;

	private Label				_lblIgnorLastMarker_Info;
	private Label				_lblIgnorLastMarker_TimeSlices;
	private Label				_lblIgnorSpeedValues_Info;
	private Label				_lblSplitTour_Duration;
	private Label				_lblSplitTour_DurationUnit;
	private Label				_lblSplitTour_Info;

	private Spinner				_spinnerIgnorLastMarker_TimeSlices;
	private Spinner				_spinnerExceededDuration;
	private Spinner				_spinnerTemperatureAdjustment;

	private TabFolder			_tabFolder;

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

			_tabFolder = new TabFolder(parent, SWT.TOP);
			GridDataFactory
					.fillDefaults()//
					.grab(true, true)
					.applyTo(_tabFolder);
			{

				final TabItem tabMeasurementSystem = new TabItem(_tabFolder, SWT.NONE);
				tabMeasurementSystem.setControl(createUI_20_Speed(_tabFolder));
				tabMeasurementSystem.setText(Messages.PrefPage_Fit_Group_Speed);

				final TabItem tabBreakTime = new TabItem(_tabFolder, SWT.NONE);
				tabBreakTime.setControl(createUI_30_Temperature(_tabFolder));
				tabBreakTime.setText(Messages.PrefPage_Fit_Group_AdjustTemperature);

				final TabItem tabElevation = new TabItem(_tabFolder, SWT.NONE);
				tabElevation.setControl(createUI_50_IgnoreLastMarker(_tabFolder));
				tabElevation.setText(Messages.PrefPage_Fit_Group_IgnoreLastMarker);

				final TabItem tabNotes = new TabItem(_tabFolder, SWT.NONE);
				tabNotes.setControl(createUI_70_SplitTour(_tabFolder));
				tabNotes.setText(Messages.PrefPage_Fit_Group_ReplaceTimeSlice);
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
				_spinnerTemperatureAdjustment.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
					}
				});
			}

			// label: �C
			{
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(label);
				label.setText(DEGREE_CELCIUS);
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
				_spinnerIgnorLastMarker_TimeSlices.addMouseWheelListener(new MouseWheelListener() {
					@Override
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
					}
				});
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

		_lblIgnorSpeedValues_Info.setEnabled(isIgnoreSpeed);

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

			_chkIgnoreLastMarker.setSelection(//
					_prefStore.getDefaultBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER));
			_spinnerIgnorLastMarker_TimeSlices.setSelection(//
					_prefStore.getDefaultInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES));

		} else if (selectedTab == TAB_FOLDER_TIME_SLIZE) {

			_chkRemoveExceededDuration.setSelection(//
					_prefStore.getDefaultBoolean(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE));
			_spinnerExceededDuration.setSelection(//
					_prefStore.getDefaultInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION));
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

		// tempterature
		final float temperatureAdjustment = _prefStore.getFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);
		_spinnerTemperatureAdjustment.setSelection(//
				(int) (temperatureAdjustment * TEMPERATURE_DIGITS));

		// last marker
		_chkIgnoreLastMarker.setSelection(//
				_prefStore.getBoolean(IPreferences.FIT_IS_IGNORE_LAST_MARKER));
		_spinnerIgnorLastMarker_TimeSlices.setSelection(//
				_prefStore.getInt(IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES));

		// exceeded time slice
		_chkRemoveExceededDuration.setSelection(//
				_prefStore.getBoolean(IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE));
		_spinnerExceededDuration.setSelection(//
				_prefStore.getInt(IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION));

		// folder
		_tabFolder.setSelection(_prefStore.getInt(STATE_FIT_IMPORT_SELECTED_TAB));

		enableControls();
	}

	private void saveState() {

		// speed
		_prefStore.setValue(IPreferences.FIT_IS_IGNORE_SPEED_VALUES, _chkIgnoreSpeedValues.getSelection());

		// tempterature
		final float temperatureAdjustment = _spinnerTemperatureAdjustment.getSelection() / TEMPERATURE_DIGITS;
		_prefStore.setValue(//
				IPreferences.FIT_TEMPERATURE_ADJUSTMENT,
				temperatureAdjustment);

		// last marker
		_prefStore.setValue(//
				IPreferences.FIT_IS_IGNORE_LAST_MARKER,
				_chkIgnoreLastMarker.getSelection());
		_prefStore.setValue(
				IPreferences.FIT_IGNORE_LAST_MARKER_TIME_SLICES,
				_spinnerIgnorLastMarker_TimeSlices.getSelection());

		// exceeded time slice
		_prefStore.setValue(//
				IPreferences.FIT_IS_REPLACE_EXCEEDED_TIME_SLICE,
				_chkRemoveExceededDuration.getSelection());
		_prefStore.setValue(//
				IPreferences.FIT_EXCEEDED_TIME_SLICE_DURATION,
				_spinnerExceededDuration.getSelection());
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
