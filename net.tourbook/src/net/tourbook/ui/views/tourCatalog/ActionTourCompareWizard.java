/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.IReferenceTourProvider;
import net.tourbook.util.Util;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class ActionTourCompareWizard extends Action {

	private final IReferenceTourProvider	fRefTourProvider;

	public ActionTourCompareWizard(final IReferenceTourProvider refTourProvider) {

		fRefTourProvider = refTourProvider;

		setText(Messages.action_tourCatalog_open_compare_wizard);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__view_compare_wizard));
	}

	@Override
	public void run() {

		final WizardTourComparer wizard = new WizardTourComparer(fRefTourProvider);

		final WizardDialog dialog = new PositionedWizardDialog(Display.getCurrent().getActiveShell(),
				wizard,
				WizardTourComparer.DIALOG_SETTINGS_SECTION,
				800,
				600);

		BusyIndicator.showWhile(null, new Runnable() {
			public void run() {

				if (dialog.open() == Window.OK) {

					/*
					 * show the compare tour perspective
					 */
					final IWorkbench workbench = PlatformUI.getWorkbench();
					final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

					try {
						// show tour compare perspective
						workbench.showPerspective(PerspectiveFactoryCompareTours.PERSPECTIVE_ID, window);

						// show tour compare view
						Util.showView(TourCompareResultView.ID);

					} catch (final WorkbenchException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}
}
