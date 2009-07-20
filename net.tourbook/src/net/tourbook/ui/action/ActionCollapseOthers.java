/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.util.ITourViewer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;

public class ActionCollapseOthers extends Action {

	private ITourViewer	fTourViewer;

	public ActionCollapseOthers(final ITourViewer tourViewer) {

		super(null, AS_PUSH_BUTTON);

		fTourViewer = tourViewer;

		setText(Messages.app_action_collapse_others_tooltip);
		setToolTipText(Messages.app_action_collapse_others_tooltip);
//		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__collapse_all));

	}

	@Override
	public void run() {

		if (fTourViewer != null) {

			final ColumnViewer viewer = fTourViewer.getViewer();
			if (viewer instanceof TreeViewer) {

				final TreeViewer treeViewer = (TreeViewer) viewer;
				final Object firstElement = ((StructuredSelection) treeViewer.getSelection()).getFirstElement();

				if (firstElement != null) {

					final Tree tree = treeViewer.getTree();
					tree.setRedraw(false);
					{
						treeViewer.collapseAll();
						treeViewer.setExpandedElements(new Object[] { firstElement });
						treeViewer.setSelection(new StructuredSelection(firstElement), true);
					}
					tree.setRedraw(true);
				}
			}
		}
	}
}
