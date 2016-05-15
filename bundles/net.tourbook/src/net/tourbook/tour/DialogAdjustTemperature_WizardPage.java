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
package net.tourbook.tour;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.importdata.EasyConfig;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.joda.time.Period;
import org.joda.time.PeriodType;

class DialogAdjustTemperature_WizardPage extends WizardPage {

	private final IPreferenceStore	_prefStore			= TourbookPlugin.getPrefStore();

	private static PeriodType		_durationTemplate	= PeriodType.yearMonthDayTime()
//			// hide these components
																.withMillisRemoved();

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Label					_lblDurationUnit;

	private Spinner					_spinnerAvgTemperature;
	private Spinner					_spinnerTemperatureAdjustmentDuration;

	protected DialogAdjustTemperature_WizardPage(final String pageName) {

		super(pageName);

		setTitle(Messages.Dialog_AdjustTemperature_Dialog_Title);
		setMessage(Messages.Dialog_AdjustTemperature_Dialog_Message);
	}

	@Override
	public void createControl(final Composite parent) {

		_pc = new PixelConverter(parent);

		final Composite page = createUI(parent);

		// set wizard page control
		setControl(page);

		restoreState();
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().applyTo(container);
		{
			createUI_10_Controls(container);
		}

		return container;
	}

	private void createUI_10_Controls(final Composite parent) {

		final int infoWidth = _pc.convertWidthInCharsToPixels(60);

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.grab(true, false)
				.applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
		{
			{
				/*
				 * Label: Temperature adjustment info
				 */
				final Label label = new Label(container, SWT.WRAP | SWT.READ_ONLY);
				label.setText(Messages.Dialog_AdjustTemperature_Info_TemperatureAdjustment);
				GridDataFactory.fillDefaults()//
						.hint(infoWidth, SWT.DEFAULT)
						.grab(true, false)
						.applyTo(label);
			}

			final Composite innerContainer = new Composite(container, SWT.NONE);
//			innerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
			GridDataFactory.fillDefaults().grab(true, false).applyTo(innerContainer);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(innerContainer);
			{
				{
					final int verticalIndent = _pc.convertVerticalDLUsToPixels(4);

					/*
					 * Label: Adjustment duration
					 */
					final Label label = new Label(innerContainer, SWT.NONE);
					label.setText(Messages.Dialog_AdjustTemperature_Label_TemperatureAdjustmentDuration);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(0, verticalIndent)
							.applyTo(label);

					/*
					 * Spinner: Duration
					 */
					_spinnerTemperatureAdjustmentDuration = new Spinner(innerContainer, SWT.BORDER);
					_spinnerTemperatureAdjustmentDuration.setMinimum(0);
					_spinnerTemperatureAdjustmentDuration.setMaximum(60 * 60 * 24); // 1 day
					_spinnerTemperatureAdjustmentDuration.setPageIncrement(60); // 60 = 1 minute
					_spinnerTemperatureAdjustmentDuration.addMouseWheelListener(new MouseWheelListener() {
						@Override
						public void mouseScrolled(final MouseEvent event) {
							Util.adjustSpinnerValueOnMouseScroll(event);
							updateUI_TemperatureAdjustmentDuration();
						}
					});
					_spinnerTemperatureAdjustmentDuration.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(final SelectionEvent e) {
							updateUI_TemperatureAdjustmentDuration();
						}
					});
					GridDataFactory.fillDefaults()//
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(0, verticalIndent)
							.applyTo(_spinnerTemperatureAdjustmentDuration);

					// label: h
					_lblDurationUnit = new Label(innerContainer, SWT.NONE);
					_lblDurationUnit.setText(UI.UNIT_LABEL_TIME);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(0, verticalIndent)
							.grab(true, false)
							.applyTo(_lblDurationUnit);
				}

				{
					/*
					 * Avg temperature
					 */
					// label
					final Label label = new Label(innerContainer, SWT.NONE);
					label.setText(Messages.Dialog_AdjustTemperature_Label_AvgTemperature);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(label);

					// spinner
					_spinnerAvgTemperature = new Spinner(innerContainer, SWT.BORDER);
					_spinnerAvgTemperature.setPageIncrement(5);
					_spinnerAvgTemperature.setMinimum(EasyConfig.TEMPERATURE_AVG_TEMPERATURE_MIN);
					_spinnerAvgTemperature.setMaximum(EasyConfig.TEMPERATURE_AVG_TEMPERATURE_MAX);
					_spinnerAvgTemperature.addMouseWheelListener(new MouseWheelListener() {
						@Override
						public void mouseScrolled(final MouseEvent event) {
							Util.adjustSpinnerValueOnMouseScroll(event);
						}
					});
					GridDataFactory.fillDefaults() //
							.align(SWT.END, SWT.FILL)
							.applyTo(_spinnerAvgTemperature);

					// label: °C / °F
					final Label unitLabel = new Label(innerContainer, SWT.NONE);
					unitLabel.setText(UI.UNIT_LABEL_TEMPERATURE);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(unitLabel);
				}
			}

			{
				/*
				 * Label: Hint
				 */
				final Label label = new Label(container, SWT.WRAP | SWT.READ_ONLY);
				label.setText(Messages.Dialog_AdjustTemperature_Info_Hint);
				GridDataFactory.fillDefaults()//
						.hint(infoWidth, SWT.DEFAULT)
						.indent(0, _pc.convertVerticalDLUsToPixels(8))
						.grab(true, false)
						.applyTo(label);
			}
		}
	}

	private void restoreState() {

		final float avgTemperature = _prefStore.getFloat(ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE);
		final int durationTime = _prefStore.getInt(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME);

		_spinnerAvgTemperature.setSelection((int) (UI.convertTemperatureFromMetric(avgTemperature) + 0.5));
		_spinnerTemperatureAdjustmentDuration.setSelection(durationTime);

		updateUI_TemperatureAdjustmentDuration();
	}

	void saveState() {

		final float avgTemperature = UI.convertTemperatureToMetric(_spinnerAvgTemperature.getSelection());

		final int durationTime = _spinnerTemperatureAdjustmentDuration.getSelection();

		_prefStore.setValue(ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE, avgTemperature);
		_prefStore.setValue(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME, durationTime);
	}

	private void updateUI_TemperatureAdjustmentDuration() {

		final long duration = _spinnerTemperatureAdjustmentDuration.getSelection();

		final Period tourPeriod = new Period(0, duration * 1000, _durationTemplate);

		_lblDurationUnit.setText(tourPeriod.toString(UI.DEFAULT_DURATION_FORMATTER));
	}

}
