/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.commands;

import net.tourbook.ui.views.tagging.TourTags_View;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class RestoreTour_Handler extends AbstractHandler {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      final IWorkbenchPart part = HandlerUtil.getActivePart(event);

      if (part instanceof ISaveAndRestorePart) {

         ((ISaveAndRestorePart) part).doRestore();
      }

      return null;
   }

   @Override
   public boolean isEnabled() {

      final IWorkbenchWindow wbWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
      if (wbWindow == null) {
         return false;
      }

      final IWorkbenchPart activePart = wbWindow.getActivePage().getActivePart();

      if (activePart instanceof TourTags_View) {

         /*
          * Save/restore actions are always enabled in the tour tags view, this is necessary,
          * otherwise tags must be modified that the save/restore actions are enabled
          */

         return true;
      }

      if (activePart instanceof ISaveablePart) {
         return ((ISaveablePart) activePart).isDirty();
      }

      return false;
   }

}
