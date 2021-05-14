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

import java.util.Map;

import net.tourbook.Images;
import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.common.util.Util;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class ActionHandler_TourCompareWizard extends AbstractHandler implements IElementUpdater {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      if (TourManager.isTourEditorModified()) {
         return null;
      }

      final Wizard wizard = new WizardTourComparer();

      final WizardDialog dialog = new PositionedWizardDialog(HandlerUtil.getActiveShellChecked(event),
            wizard,
            WizardTourComparer.DIALOG_SETTINGS_SECTION,
            800,
            600);

      BusyIndicator.showWhile(null, new Runnable() {
         @Override
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
                  Util.showView(TourCompareResultView.ID, true);

               } catch (final WorkbenchException e) {
                  e.printStackTrace();
               }
            }
         }
      });

      return null;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public void updateElement(final UIElement uiElement, final Map parameters) {

      UI.setThemedIcon(uiElement, Images.TourCatalog_CompareWizard);
   }
}
