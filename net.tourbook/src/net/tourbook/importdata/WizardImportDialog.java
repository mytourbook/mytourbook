/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

public class WizardImportDialog extends WizardDialog {

	private String	fWindowTitle;

	public WizardImportDialog(Shell parentShell, IWizard newWizard, String windowTitle) {

		super(parentShell, newWizard);

		setShellStyle(getShellStyle() | SWT.RESIZE);

		fWindowTitle = windowTitle;
	}

	public void create() {
		
		super.create();

		getShell().setText(fWindowTitle);
	}

	protected IDialogSettings getDialogBoundsSettings() {
		return TourbookPlugin.getDefault().getDialogSettingsSection(
				getClass().getName() + "_DialogBounds"); //$NON-NLS-1$
	}
}
