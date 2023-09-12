/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.application;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.Messages;

import org.eclipse.jface.action.ContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.PlatformUI;

public class ContributionItem_RestartApp extends ContributionItem {

   private ToolItem _actionToolItem;

   private Image    _actionImage;

   public ContributionItem_RestartApp() {

      createActionImage();
   }

   private void createActionImage() {

      _actionImage = CommonActivator.getThemedImageDescriptor(CommonImages.App_ReStart).createImage();
   }

   @Override
   public void fill(final ToolBar toolbar, final int index) {

      if (_actionToolItem == null && toolbar != null) {

         // action is not yet created

         toolbar.addDisposeListener(disposeEvent -> onDispose());

         /*
          * It happened that the action image is disposed when dragging the coolbar -> very cool :-(
          */
         if (_actionImage == null || _actionImage.isDisposed()) {
            createActionImage();
         }

         _actionToolItem = new ToolItem(toolbar, SWT.PUSH);

         _actionToolItem.setToolTipText(Messages.Action_App_RestartApp_Tooltip);
         _actionToolItem.setImage(_actionImage);
         _actionToolItem.addSelectionListener(widgetSelectedAdapter(selectionEvent ->

         Display.getCurrent().asyncExec(() -> PlatformUI.getWorkbench().restart())

         ));
      }
   }

   private void onDispose() {

      _actionImage.dispose();

      _actionToolItem.dispose();
      _actionToolItem = null;
   }
}
