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
package net.tourbook.preferences;

import net.tourbook.Messages;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageImport extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String		ID				= "net.tourbook.preferences.PrefPageImport";	//$NON-NLS-1$

	private final IDialogSettings	_importState	= TourbookPlugin.getState(RawDataView.ID);

	private RawDataManager			_rawDataMgr		= RawDataManager.getInstance();

	private PixelConverter			_pc;

	/*
	 * UI controls
	 */
	private Button					_chkCreateTourIdWithTime;

	private Label					_lblIdInfo;

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
			// label: common info
			{
				final Label label = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults().applyTo(label);
				label.setText(Messages.PrefPage_Import_Label_Info);
			}

			// checkbox: create tour id with time
			{
				_chkCreateTourIdWithTime = new Button(container, SWT.CHECK);
				GridDataFactory.fillDefaults()//
						.indent(0, _pc.convertVerticalDLUsToPixels(4))
						.applyTo(_chkCreateTourIdWithTime);
				_chkCreateTourIdWithTime.setText(Messages.PrefPage_Import_Checkbox_CreateTourIdWithTime);
				_chkCreateTourIdWithTime.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						enableControls();
					}
				});
			}

			// label: id info
			{
				_lblIdInfo = new Label(container, SWT.NONE);
				GridDataFactory.fillDefaults()//
						.indent(_pc.convertWidthInCharsToPixels(3), 0)
						.applyTo(_lblIdInfo);
				_lblIdInfo.setText(Messages.PrefPage_Import_Checkbox_CreateTourIdWithTime_Tooltip);
			}
		}

		return container;
	}

	private void enableControls() {

		final boolean isTourIdWithTime = _chkCreateTourIdWithTime.getSelection();

		_lblIdInfo.setEnabled(isTourIdWithTime);
	}

	public void init(final IWorkbench workbench) {

	}

	private void initUI(final Composite parent) {

		_pc = new PixelConverter(parent);
	}

	@Override
	protected void performDefaults() {

		_chkCreateTourIdWithTime.setSelection(RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT);

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

		final boolean isCreateTourIdWithTime = Util.getStateBoolean(
				_importState,
				RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME,
				RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME_DEFAULT);

		_chkCreateTourIdWithTime.setSelection(isCreateTourIdWithTime);
	}

	private void saveState() {

		final boolean isCreateTourIdWithTime = _chkCreateTourIdWithTime.getSelection();
		_importState.put(RawDataView.STATE_IS_CREATE_TOUR_ID_WITH_TIME, isCreateTourIdWithTime);
		_rawDataMgr.setCreateTourIdWithTime(isCreateTourIdWithTime);
	}

}
