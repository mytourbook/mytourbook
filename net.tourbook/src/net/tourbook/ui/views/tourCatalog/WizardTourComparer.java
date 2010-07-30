/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourReference;
import net.tourbook.ui.IReferenceTourProvider;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.wizard.Wizard;

public class WizardTourComparer extends Wizard {

	public static final String		DIALOG_SETTINGS_SECTION	= "WizardTourComparer"; //$NON-NLS-1$

	private WizardPageCompareTour	_pageCompareTour;
	private WizardPageReferenceTour	_pageReferenceTour;

	private IReferenceTourProvider	_refTourProvider;

	public WizardTourComparer() {
		setDialogSettings();
		setWindowTitle(Messages.tourCatalog_wizard_Wizard_title);
	}

	public WizardTourComparer(final IReferenceTourProvider refTourProvider) {
		this();
		_refTourProvider = refTourProvider;
	}

	@Override
	public void addPages() {

		addPage(_pageCompareTour = new WizardPageCompareTour());
		addPage(_pageReferenceTour = new WizardPageReferenceTour(_refTourProvider));
	}

	@Override
	public boolean performCancel() {
		persistDialogSettings();
		return true;
	}

	@Override
	public boolean performFinish() {

		persistDialogSettings();

		final TourReference[] refTours = _pageReferenceTour.getReferenceTours();
		final Object[] comparedTours = _pageCompareTour.getComparedTours();

		TourCompareManager.getInstance().compareTours(refTours, comparedTours);

		return true;
	}

	private void persistDialogSettings() {
		_pageReferenceTour.persistDialogSettings();
		_pageCompareTour.persistDialogSettings();
	}

	private void setDialogSettings() {

		final IDialogSettings pluginSettings = TourbookPlugin.getDefault().getDialogSettings();
		IDialogSettings wizardSettings = pluginSettings.getSection(DIALOG_SETTINGS_SECTION);

		if (wizardSettings == null) {
			wizardSettings = pluginSettings.addNewSection(DIALOG_SETTINGS_SECTION);
		}

		super.setDialogSettings(wizardSettings);
	}

}
