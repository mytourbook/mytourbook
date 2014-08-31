/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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

import net.tourbook.common.util.Util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageImportFit extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID					= "net.tourbook.device.PrefPageFit";			//$NON-NLS-1$

	private static final String	DEGREE_CELCIUS		= "°C";										//$NON-NLS-1$

	private static final float	TEMPERATURE_DIGITS	= 10.0f;

	private IPreferenceStore	_prefStore			= Activator.getDefault().getPreferenceStore();

	private PixelConverter		_pc;

	/*
	 * UI controls
	 */
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
			createUI_10_Distance(container);
		}

		return container;
	}

	private void createUI_10_Distance(final Composite parent) {

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
				final Text txtInfo = new Text(group, SWT.WRAP | SWT.READ_ONLY);
				GridDataFactory.fillDefaults()//
						.span(3, 1)
						.grab(true, false)
						.hint(_pc.convertWidthInCharsToPixels(50), SWT.DEFAULT)
						.indent(-3, 0)
						.applyTo(txtInfo);
				txtInfo.setText(Messages.PrefPage_Fit_Label_AdjustTemperature_Info);
			}
		}
	}

	public void init(final IWorkbench workbench) {}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	@Override
	protected void performDefaults() {

		final float temperatureAdjustment = _prefStore.getDefaultFloat(IPreferences.FIT_TEMPERATURE_ADJUSTMENT);

		_spinnerTemperatureAdjustment.setSelection((int) (temperatureAdjustment * TEMPERATURE_DIGITS));

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

		_spinnerTemperatureAdjustment.setSelection((int) (temperatureAdjustment * TEMPERATURE_DIGITS));
	}

	private void saveState() {

		final float temperatureAdjustment = _spinnerTemperatureAdjustment.getSelection() / TEMPERATURE_DIGITS;

		_prefStore.setValue(IPreferences.FIT_TEMPERATURE_ADJUSTMENT, temperatureAdjustment);
	}
}
