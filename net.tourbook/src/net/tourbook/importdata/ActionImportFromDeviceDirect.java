/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.util.PositionedWizardDialog;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;

public class ActionImportFromDeviceDirect extends Action {

	private IWorkbenchWindow	fWindow;

	public ActionImportFromDeviceDirect(IWorkbenchWindow window) {

		fWindow = window;

		setText(Messages.Action_import_rawdata_direct);
		setToolTipText(Messages.Action_import_rawdata_direct_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_import_rawdata_direct));
	}

	public void run() {

		final WizardImportData importWizard = new WizardImportData();

		final WizardDialog dialog = new PositionedWizardDialog(
				fWindow.getShell(),
				importWizard,
				WizardImportData.DIALOG_SETTINGS_SECTION);

		// create the dialog that the shell is created which is required in
		// setAutoDownload()
		dialog.create();

		importWizard.setAutoDownload();

		dialog.open();
	}
}
