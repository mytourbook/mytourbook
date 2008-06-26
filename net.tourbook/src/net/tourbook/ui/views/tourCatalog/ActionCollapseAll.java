/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

public class ActionCollapseAll extends Action {

	private TreeViewer	fTreeViewer;

	public ActionCollapseAll(final TreeViewer treeViewer) {

		super(null, AS_PUSH_BUTTON);

		fTreeViewer = treeViewer;

		setText(Messages.tourCatalog_view_action_collapse_all_tooltip);
		setToolTipText(Messages.tourCatalog_view_action_collapse_all_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__collapse_all));
	}

	@Override
	public void run() {

		if (fTreeViewer != null) {

			fTreeViewer.collapseAll();

			final Object firstElement = ((StructuredSelection) fTreeViewer.getSelection()).getFirstElement();
			if (firstElement != null) {
				fTreeViewer.reveal(firstElement);
			}
		}
	}

}
