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
package net.tourbook.importdata;

import net.tourbook.application.TourbookPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public class DataTransferWizardDialog extends WizardDialog {

	private String	_windowTitle;

	public DataTransferWizardDialog(final Shell parentShell, final IWizard wizard, final String windowTitle) {

		super(parentShell, wizard);

		setShellStyle(getShellStyle() | SWT.RESIZE);

		_windowTitle = windowTitle;
	}

	@Override
	public void create() {

		super.create();

		getShell().setText(_windowTitle);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection("DataTransferWizardDialog_DialogBounds"); //$NON-NLS-1$
	}
}
