/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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

import java.util.Collections;
import java.util.Map;

import net.tourbook.common.UI;

import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IServiceLocator;

/**
 * This is not yet used, could use "activePart" to get the tour editor
 */
public class TourEditor_SourceProvider extends AbstractSourceProvider implements IWindowListener, IPartListener2 {

   private static final String     TOUR_EDITOR_PART_PROVIDER = "net.tourbook.commands.currentTourEditor"; //$NON-NLS-1$

   private IWorkbenchPartReference _activePartRef;

   @Override
   public void dispose() {

      PlatformUI.getWorkbench().removeWindowListener(this);
      _activePartRef = null;
   }

   @SuppressWarnings("rawtypes")
   @Override
   public Map getCurrentState() {

      final String partId = _activePartRef == null ? null : _activePartRef.getId();

      if (partId == null) {
         return Collections.singletonMap(TOUR_EDITOR_PART_PROVIDER, UI.EMPTY_STRING);
      }

      return Collections.singletonMap(TOUR_EDITOR_PART_PROVIDER, partId);
   }

   @Override
   public String[] getProvidedSourceNames() {
      return new String[] { TOUR_EDITOR_PART_PROVIDER };
   }

   @Override
   public void initialize(final IServiceLocator locator) {

      super.initialize(locator);

      PlatformUI.getWorkbench().addWindowListener(this);
   }

   @Override
   public void partActivated(final IWorkbenchPartReference partRef) {
      _activePartRef = partRef;
   }

   @Override
   public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

   @Override
   public void partClosed(final IWorkbenchPartReference partRef) {}

   @Override
   public void partDeactivated(final IWorkbenchPartReference partRef) {
      _activePartRef = null;
   }

   @Override
   public void partHidden(final IWorkbenchPartReference partRef) {}

   @Override
   public void partInputChanged(final IWorkbenchPartReference partRef) {}

   @Override
   public void partOpened(final IWorkbenchPartReference partRef) {}

   @Override
   public void partVisible(final IWorkbenchPartReference partRef) {}

   @Override
   public void windowActivated(final IWorkbenchWindow window) {
      window.getPartService().addPartListener(this);
   }

   @Override
   public void windowClosed(final IWorkbenchWindow window) {}

   @Override
   public void windowDeactivated(final IWorkbenchWindow window) {
      window.getPartService().removePartListener(this);
   }

   @Override
   public void windowOpened(final IWorkbenchWindow window) {}

}
