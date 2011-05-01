/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.training;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class DialogHRZones extends TitleAreaDialog {

	private final IDialogSettings	_state	= TourbookPlugin.getDefault() //
													.getDialogSettingsSection(getClass().getName());
	private TourPerson				_person;

	public DialogHRZones(final Shell parentShell, final TourPerson tourPerson) {

		super(parentShell);

		_person = tourPerson;

		// make dialog resizable
//		setShellStyle(getShellStyle() | SWT.RESIZE);
	}

	private void actionAddZone() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void configureShell(final Shell shell) {

		super.configureShell(shell);

		shell.setText(Messages.Dialog_HRZone_DialogTitle);
	}

	@Override
	public void create() {

		super.create();

		setTitle(Messages.Dialog_HRZone_DialogTitle);
		setMessage(Messages.Dialog_HRZone_DialogMessage);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		Button button;

		/*
		 * button: add vertex
		 */
		button = createButton(parent, IDialogConstants.CLIENT_ID + 1, Messages.Dialog_HRZone_Button_AddZone, false);

		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				actionAddZone();
			}
		});

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
//		fBtnOK = getButton(IDialogConstants.OK_ID);
//		fBtnOK.setText(Messages.dialog_adjust_srtm_colors_button_update);
	}

	@Override
	protected Control createDialogArea(final Composite parent) {

		final Composite ui = (Composite) super.createDialogArea(parent);

		// create ui
		createUI(ui);

//		updateUIFromModel();
//		enableControls();

		return ui;
	}

	private void createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		{
			createUI10HRZoneTable(container);
		}
	}

	private void createUI10HRZoneTable(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
		{
			/*
			 * header label: zone
			 */
			Label label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(Messages.Dialog_HRZone_Label_Header_Zone);

			/*
			 * header label: pulse
			 */
			label = new Label(container, SWT.NONE);
			GridDataFactory.fillDefaults().applyTo(label);
			label.setText(Messages.Dialog_HRZone_Label_Header_Pulse);

		}
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		// keep window size and position
		return _state;
	}
}
