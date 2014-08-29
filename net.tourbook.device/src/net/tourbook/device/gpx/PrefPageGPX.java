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
package net.tourbook.device.gpx;

import net.tourbook.device.Activator;
import net.tourbook.device.IPreferences;
import net.tourbook.device.Messages;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageGPX extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String	ID			= "net.tourbook.device.PrefPageGPX";			//$NON-NLS-1$

	private IPreferenceStore	_prefStore	= Activator.getDefault().getPreferenceStore();

	private SelectionListener	_defaultSelectionListener;

	private PixelConverter		_pc;

	/*
	 * UI controls
	 */
	private Button				_rdoDistanceRelative;
	private Button				_rdoDistanceAbsolute;

	/*
	 * UI controls
	 */

	@Override
	protected Control createContents(final Composite parent) {

		initUI(parent);

		final Composite ui = createUI(parent);

		restoreState();
		enableControls();

		return ui;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
		{
			createUI_10_Distance(container);
		}

		return container;
	}

	private void createUI_10_Distance(final Composite parent) {

		// label
		final Label label = new Label(parent, SWT.NONE);
		GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.BEGINNING).applyTo(label);
		label.setText(Messages.PrefPage_GPX_Label_DistanceValues);

		// radio
		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
				.indent(_pc.convertWidthInCharsToPixels(3), 0)
				.applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			_rdoDistanceRelative = new Button(container, SWT.RADIO);
			_rdoDistanceRelative.setText(Messages.PrefPage_GPX_Radio_DistanceRelative);
			_rdoDistanceRelative.setToolTipText(Messages.PrefPage_GPX_Radio_DistanceRelative_Tooltip);
			_rdoDistanceRelative.addSelectionListener(_defaultSelectionListener);

			_rdoDistanceAbsolute = new Button(container, SWT.RADIO);
			_rdoDistanceAbsolute.setText(Messages.PrefPage_GPX_Radio_DistanceAbsolute);
			_rdoDistanceAbsolute.setToolTipText(Messages.PrefPage_GPX_Radio_DistanceAbsolute_Tooltip);
			_rdoDistanceAbsolute.addSelectionListener(_defaultSelectionListener);
		}
	}

	private void enableControls() {

	}

	public void init(final IWorkbench workbench) {

	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);

		_defaultSelectionListener = new SelectionAdapter() {

			@Override
			public void widgetSelected(final SelectionEvent e) {
				onModify();
			}
		};
	}

	@Override
	public boolean okToLeave() {

		return super.okToLeave();
	}

	private void onModify() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void performApply() {

		super.performApply();
	}

	@Override
	protected void performDefaults() {

		final boolean isRelativeDistance = _prefStore.getDefaultBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

		_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
		_rdoDistanceRelative.setSelection(isRelativeDistance);

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

		final boolean isRelativeDistance = _prefStore.getBoolean(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE);

		_rdoDistanceAbsolute.setSelection(isRelativeDistance == false);
		_rdoDistanceRelative.setSelection(isRelativeDistance);
	}

	private void saveState() {

		_prefStore.setValue(IPreferences.GPX_IS_RELATIVE_DISTANCE_VALUE, _rdoDistanceRelative.getSelection());
	}
}
