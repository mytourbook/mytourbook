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
package net.tourbook.ui.action;

import net.tourbook.Messages;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.CommonImages;
import net.tourbook.common.color.ThemeUtil;
import net.tourbook.common.util.ITreeViewer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ActionCollapseAll extends Action {

   private ITreeViewer _treeViewerProvider;

   public ActionCollapseAll(final ITreeViewer treeViewerProvider) {

      super(null, AS_PUSH_BUTTON);

      _treeViewerProvider = treeViewerProvider;

      setText(Messages.App_Action_CollapseAll);
      setToolTipText(Messages.App_Action_CollapseAll_Tooltip);

      setImageDescriptor(CommonActivator.getImageDescriptor(ThemeUtil.getThemedImageName(CommonImages.App_CollapseAll)));
      setDisabledImageDescriptor(CommonActivator.getImageDescriptor(ThemeUtil.getThemedImageName(CommonImages.App_CollapseAll_Disabled)));
   }

   @Override
   public void run() {

      if (_treeViewerProvider == null) {
         return;
      }

      final TreeViewer treeViewer = _treeViewerProvider.getTreeViewer();

      if (treeViewer == null) {
         return;
      }

      final Tree tree = treeViewer.getTree();

      // disable redraw that the UI in not flickering
      tree.setRedraw(false);
      {
         try {
            treeViewer.collapseAll();
         } catch (final Exception e) {
            // this occured
         }
      }
      tree.setRedraw(true);

      try {

         final StructuredSelection selection = (StructuredSelection) treeViewer.getSelection();
         if (selection != null) {
            final Object firstElement = selection.getFirstElement();
            if (firstElement != null) {
               treeViewer.reveal(firstElement);
            }
         }

      } catch (final Exception e) {

         // this occured, ensure something is selected otherwise further NPEs occure

         final TreeItem[] selection = tree.getSelection();

         for (final TreeItem treeItem : selection) {

            final Object itemData = treeItem.getData();

            _treeViewerProvider.getTreeViewer().setSelection(new StructuredSelection(itemData), true);

            break;
         }
      }
   }
}
