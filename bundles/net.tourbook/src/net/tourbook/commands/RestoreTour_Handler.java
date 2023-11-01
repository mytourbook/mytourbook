/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;

public class RestoreTour_Handler extends AbstractHandler implements IElementUpdater {

   private static final ImageDescriptor _iconRestoreTour          = TourbookPlugin.getThemedImageDescriptor(Images.RestoreTour);
   private static final ImageDescriptor _iconRestoreTour_Disabled = TourbookPlugin.getThemedImageDescriptor(Images.RestoreTour_Disabled);

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

      uiElement.setDisabledIcon(_iconRestoreTour_Disabled);
      uiElement.setIcon(_iconRestoreTour);
   }
}
