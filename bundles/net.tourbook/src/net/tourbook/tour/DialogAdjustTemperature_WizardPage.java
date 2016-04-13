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
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;

class DialogAdjustTemperature_WizardPage extends WizardPage {

	private final IPreferenceStore	_prefStore	= TourbookPlugin.getPrefStore();

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private DateTime				_dtTemperatureAdjustmentDuration;

	private Spinner					_spinnerAvgTemperature;

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
						.hint(_pc.convertWidthInCharsToPixels(60), SWT.DEFAULT)
						.grab(true, false)
						.applyTo(label);
			}

			final Composite innerContainer = new Composite(container, SWT.NONE);
//			innerContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
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
					 * DateTime: Duration
					 */
					_dtTemperatureAdjustmentDuration = new DateTime(innerContainer, SWT.TIME | SWT.MEDIUM | SWT.BORDER);
					GridDataFactory.fillDefaults()//
							.align(SWT.BEGINNING, SWT.FILL)
							.indent(0, verticalIndent)
							.applyTo(_dtTemperatureAdjustmentDuration);

					// label: h
					final Label unitLabel = new Label(innerContainer, SWT.NONE);
					unitLabel.setText(UI.UNIT_LABEL_TIME);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.indent(0, verticalIndent)
							.applyTo(unitLabel);
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
					_spinnerAvgTemperature.setMinimum(0);
					_spinnerAvgTemperature.setMaximum(30);
					_spinnerAvgTemperature.addMouseWheelListener(new MouseWheelListener() {
						@Override
						public void mouseScrolled(final MouseEvent event) {
							Util.adjustSpinnerValueOnMouseScroll(event);
						}
					});
					GridDataFactory.fillDefaults() //
							.align(SWT.END, SWT.FILL)
							.applyTo(_spinnerAvgTemperature);

					// label: °C
					final Label unitLabel = new Label(innerContainer, SWT.NONE);
					unitLabel.setText(UI.SYMBOL_TEMPERATURE);
					GridDataFactory.fillDefaults()//
							.align(SWT.FILL, SWT.CENTER)
							.applyTo(unitLabel);
				}
			}
		}
	}

	private void restoreState() {

		final float avgTemperature = _prefStore.getFloat(ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE);
		final int durationTime = _prefStore.getInt(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME);

		_spinnerAvgTemperature.setSelection((int) avgTemperature);
		_dtTemperatureAdjustmentDuration.setTime(
				durationTime / 3600,
				(durationTime % 3600) / 60,
				(durationTime % 3600) % 60);
	}

	void saveState() {

		final float avgTemperature = _spinnerAvgTemperature.getSelection();

		final int hours = _dtTemperatureAdjustmentDuration.getHours();
		final int minutes = _dtTemperatureAdjustmentDuration.getMinutes();
		final int seconds = _dtTemperatureAdjustmentDuration.getSeconds();

		final int durationTime = hours * 3600 + minutes * 60 + seconds;

		_prefStore.setValue(ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE, avgTemperature);
		_prefStore.setValue(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME, durationTime);
	}
}
