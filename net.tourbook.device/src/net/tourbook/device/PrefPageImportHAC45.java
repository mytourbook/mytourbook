/*******************************************************************************
 * Copyright (C) 2005, 2014 Wolfgang Schramm and Contributors
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
package net.tourbook.device;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.ui.views.rawData.RawDataView;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
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
import org.joda.time.DateTime;

public class PrefPageImportHAC45 extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID				= "net.tourbook.device.PrefPageImportHAC45";	//$NON-NLS-1$

	private final IDialogSettings	_importState	= TourbookPlugin.getState(RawDataView.ID);

	private RawDataManager			_rawDataMgr		= RawDataManager.getInstance();

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Button					_chkAdjustImportYear;
	private Button					_chkDisableCheckSumValidation;

	private Label					_lblImportYear;

	private Spinner					_spinnerImportYear;

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
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			createUI_10_DisableChecksumvalidation(container);
			createUI_20_AdjustImportYear(container);
		}

		return container;
	}

	private void createUI_10_DisableChecksumvalidation(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_HAC4_Group);
		GridLayoutFactory.swtDefaults().applyTo(group);
		{
			// checkbox: disable checksum validation
			{
				_chkDisableCheckSumValidation = new Button(group, SWT.CHECK);
				_chkDisableCheckSumValidation.setText(Messages.PrefPage_HAC4_Checkbox_DisableChecksumValidation);
			}
		}
	}

	private void createUI_20_AdjustImportYear(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
		group.setText(Messages.PrefPage_HAC45_Group);
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
		{
			// checkbox: adjust import year
			{
				_chkAdjustImportYear = new Button(group, SWT.CHECK);
				GridDataFactory.fillDefaults().span(2, 1).applyTo(_chkAdjustImportYear);
				_chkAdjustImportYear.setText(Messages.PrefPage_HAC45_Checkbox_AdjustImportYear);
				_chkAdjustImportYear.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						enableControls();
					}
				});
			}

			// label: import year
			{
				_lblImportYear = new Label(group, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.align(SWT.BEGINNING, SWT.CENTER)
						.indent(_pc.convertHorizontalDLUsToPixels(12), 0)
						.applyTo(_lblImportYear);
				_lblImportYear.setText(Messages.PrefPage_HAC45_Label_ImportYear);
			}

			// spinner: import year
			{
				_spinnerImportYear = new Spinner(group, SWT.BORDER);
				GridDataFactory.fillDefaults() //
						.align(SWT.BEGINNING, SWT.FILL)
						.applyTo(_spinnerImportYear);
				_spinnerImportYear.setPageIncrement(1);
				_spinnerImportYear.setMinimum(1970);
				_spinnerImportYear.setMaximum(3000);
				_spinnerImportYear.addMouseWheelListener(new MouseWheelListener() {
					public void mouseScrolled(final MouseEvent event) {
						Util.adjustSpinnerValueOnMouseScroll(event);
					}
				});
			}
		}
	}

	private void enableControls() {

		final boolean isAdjustYear = _chkAdjustImportYear.getSelection();

		_lblImportYear.setEnabled(isAdjustYear);
		_spinnerImportYear.setEnabled(isAdjustYear);
	}

	public void init(final IWorkbench workbench) {}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	@Override
	protected void performDefaults() {

		// merge all tracks into one tour
		_chkDisableCheckSumValidation.setSelection(RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);

		// HAC 4/5: adjust import year, this value is never saved in a state
		_chkAdjustImportYear.setSelection(false);
		_spinnerImportYear.setSelection(new DateTime().getYear());

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

		// HAC4: disable checksum validation
		final boolean isMergeIntoOneTour = Util.getStateBoolean(
				_importState,
				RawDataView.STATE_IS_MERGE_TRACKS,
				RawDataView.STATE_IS_MERGE_TRACKS_DEFAULT);
		_chkDisableCheckSumValidation.setSelection(isMergeIntoOneTour);

		// HAC 4/5: adjust import year, this value is never saved in a state but a temp state is available
		final boolean isDisabled = _rawDataMgr.getImportYear() == RawDataManager.ADJUST_IMPORT_YEAR_IS_DISABLED;
		_chkAdjustImportYear.setSelection(!isDisabled);
		_spinnerImportYear.setSelection(new DateTime().getYear());
	}

	private void saveState() {

		// HAC4: disable checksum validation
		final boolean isValidation = _chkDisableCheckSumValidation.getSelection() == false;

		_importState.put(RawDataView.STATE_IS_CHECKSUM_VALIDATION, isValidation);
		_rawDataMgr.setIsChecksumValidation(isValidation);

		// HAC 4/5: adjust import year
		final boolean isAdjustYear = _chkAdjustImportYear.getSelection();
		if (isAdjustYear) {

			_rawDataMgr.setImportYear(_spinnerImportYear.getSelection());

		} else {

			// disable adjustment for the import year

			_rawDataMgr.setImportYear(RawDataManager.ADJUST_IMPORT_YEAR_IS_DISABLED);
		}
	}
}
