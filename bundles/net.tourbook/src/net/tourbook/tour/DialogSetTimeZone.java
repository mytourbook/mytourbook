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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class DialogSetTimeZone extends WizardDialog {

	public static final int	TIME_ZONE_ACTION_SET_FROM_LIST			= 0;
	public static final int	TIME_ZONE_ACTION_SET_FROM_GEO_POSITION	= 1;
	public static final int	TIME_ZONE_ACTION_REMOVE_TIME_ZONE		= 10;

	private IDialogSettings	_state									= TourbookPlugin
																			.getState("net.tourbook.tour.DialogSetTimeZone");	//$NON-NLS-1$

	public DialogSetTimeZone(final Shell parentShell, final IWizard wizard) {

		super(parentShell, wizard);
	}

	@Override
	protected final void createButtonsForButtonBar(final Composite parent) {

		super.createButtonsForButtonBar(parent);

		// set text for the OK button
		final Button button = getButton(IDialogConstants.FINISH_ID);

		button.setText(Messages.Dialog_SetTimeZone_Button_AdjustTimeZone);

		// ensure the button is wide enough
		final GridData gd = (GridData) button.getLayoutData();
		gd.widthHint = SWT.DEFAULT;
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {

		// use state to keep window position
		return _state;
	}

}
