/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.tour.TourManager;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class SaveTour_Handler extends AbstractHandler implements IElementUpdater {

   private static final ImageDescriptor _iconSaveTour          = TourbookPlugin.getThemedImageDescriptor(Images.SaveTour);

   @Override
   public Object execute(final ExecutionEvent event) throws ExecutionException {

      final IWorkbenchPart part = HandlerUtil.getActivePart(event);

      if (part instanceof ISaveablePart) {

         // tour data editor

         ((ISaveablePart) part).doSave(null);

      } else if (part instanceof ISaveAndRestorePart) {

         // tour tags editor

         ((ISaveAndRestorePart) part).doSave();
      }

      return null;
   }

   @Override
   public boolean isEnabled() {

      return TourManager.isTourModified();
   }

   @SuppressWarnings("rawtypes")
   @Override
   public void updateElement(final UIElement uiElement, final Map parameters) {

      /**
       * Show command icon depending on the active part.
       * <p>
       * This method will be called from partActivated() with
       * org.eclipse.ui.commands.ICommandService.refreshElements(..)
       * <p>
       * -> Higly complicated
       */

      uiElement.setIcon(_iconSaveTour);
   }

}
