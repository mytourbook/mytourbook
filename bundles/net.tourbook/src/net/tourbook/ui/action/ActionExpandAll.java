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
import net.tourbook.common.util.ITreeViewer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;

public class ActionExpandAll extends Action {

   private ITreeViewer _treeViewer;

   /**
    * @param treeViewer
    *           {@link TreeViewer} which should be expanded.
    */
   public ActionExpandAll(final ITreeViewer treeViewer) {

      super(null, AS_PUSH_BUTTON);

      _treeViewer = treeViewer;

      setText(Messages.App_Action_Expand_All_Tooltip);
      setToolTipText(Messages.App_Action_Expand_All_Tooltip);

      setImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_ExpandAll));
      setDisabledImageDescriptor(CommonActivator.getThemedImageDescriptor(CommonImages.App_ExpandAll_Disabled));
   }

   @Override
   public void run() {

      if (_treeViewer == null) {
         return;
      }

      final TreeViewer treeViewer = _treeViewer.getTreeViewer();
      if (treeViewer == null) {
         return;
      }

      final Tree tree = treeViewer.getTree();

      // disable redraw that the UI in not flickering
      tree.setRedraw(false);
      {
         treeViewer.expandAll();
      }
      tree.setRedraw(true);
   }
}
