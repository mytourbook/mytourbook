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

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.services.IServiceLocator;

public class SaveTour_Handler extends AbstractHandler implements IElementUpdater {

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      final IWorkbenchPart part = HandlerUtil.getActivePart(event);

      if (part instanceof ISaveablePart) {

         ((ISaveablePart) part).doSave(null);
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

      if (activePart instanceof ISaveablePart) {
         return ((ISaveablePart) activePart).isDirty();
      }

      return false;
   }

   @Override
   public void updateElement(final UIElement uiElement, final Map parameters) {
      // TODO Auto-generated method stub

//      for parameters see
//      https://www.eclipse.org/forums/index.php/t/161643/

//      icon           ="icons/save-tour.png"
//      disabledIcon   ="icons/save-tour-disabled.png"

      final Class<? extends UIElement> clazz = uiElement.getClass();

      final IServiceLocator serviceLocator = uiElement.getServiceLocator();

      int a = 0;
      a++;

   }

}
