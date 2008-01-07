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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;
import net.tourbook.data.TourReference;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;

public class WizardTourComparer extends Wizard {

	public static final String		DIALOG_SETTINGS_SECTION	= "WizardTourComparer"; //$NON-NLS-1$

	private WizardPageCompareTour	pageCompareTour;
	private WizardPageReferenceTour	pageReferenceTour;

	public WizardTourComparer() {
		setDialogSettings();
		setWindowTitle(Messages.tourCatalog_wizard_Wizard_title);
	}

	@Override
	public boolean performCancel() {
		persistDialogSettings();
		return true;
	}

	@Override
	public boolean performFinish() {

		persistDialogSettings();

		TourReference[] refTours = pageReferenceTour.getReferenceTours();
		Object[] comparesTours = pageCompareTour.getComparedTours();

		TourCompareManager.getInstance().compareTours(refTours, comparesTours);

		return true;
	}

	@Override
	public void addPages() {

		pageCompareTour = new WizardPageCompareTour("compare-tour"); //$NON-NLS-1$
		addPage(pageCompareTour);

		pageReferenceTour = new WizardPageReferenceTour("reference-tour"); //$NON-NLS-1$
		addPage(pageReferenceTour);

	}

	private void setDialogSettings() {

		IDialogSettings pluginSettings = TourbookPlugin.getDefault().getDialogSettings();
		IDialogSettings wizardSettings = pluginSettings.getSection(DIALOG_SETTINGS_SECTION);

		if (wizardSettings == null) {
			wizardSettings = pluginSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		}

		super.setDialogSettings(wizardSettings);
	}

	private void persistDialogSettings() {
		pageReferenceTour.persistDialogSettings();
		pageCompareTour.persistDialogSettings();
	}
}
