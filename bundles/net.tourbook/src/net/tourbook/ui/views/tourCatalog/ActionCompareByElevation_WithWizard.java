/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.Util;
import net.tourbook.ui.IReferenceTourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

public class ActionCompareByElevation_WithWizard extends Action {

   private final IReferenceTourProvider _refTourProvider;

   public ActionCompareByElevation_WithWizard(final IReferenceTourProvider refTourProvider) {

      _refTourProvider = refTourProvider;

      setText(Messages.action_tourCatalog_open_compare_wizard);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourCatalog_CompareWizard));
   }

   @Override
   public void run() {

      final WizardTourComparer wizard = new WizardTourComparer(_refTourProvider);

      final Display display = Display.getCurrent();

      final WizardDialog dialog = new PositionedWizardDialog(display.getActiveShell(),
            wizard,
            WizardTourComparer.DIALOG_SETTINGS_SECTION,
            800,
            600);

      BusyIndicator.showWhile(display, () -> {

         if (dialog.open() == Window.OK) {

            /*
             * Show the compare tour perspective
             */
            final IWorkbench workbench = PlatformUI.getWorkbench();
            final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();

            try {

               // show tour compare perspective
               workbench.showPerspective(PerspectiveFactoryCompareTours.PERSPECTIVE_ID, window);

               // show tour compare view
               Util.showView(TourCompareResultView.ID, true);

            } catch (final WorkbenchException e) {
               e.printStackTrace();
            }
         }
      });
   }
}
