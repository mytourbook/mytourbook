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
package net.tourbook.commands;

import java.util.Map;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.ui.views.tagging.TourTags_View;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
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

      final IWorkbenchWindow window = uiElement.getServiceLocator().getService(IWorkbenchWindow.class);
      if (window == null) {
         return;
      }
      final IWorkbenchPage page = window.getActivePage();
      if (page == null) {
         return;
      }
      final IWorkbenchPart part = page.getActivePart();
      if (part == null) {
         return;
      }

      /**
       * !!! VERY IMPORTANT !!!
       * <p>
       * The disabled icon must be set first, otherwise the wrong icon is displayed !!!
       */
      if (part instanceof TourDataEditorView) {

         uiElement.setDisabledIcon(_iconRestoreTour_Disabled);
         uiElement.setIcon(_iconRestoreTour);
      }
   }
}
