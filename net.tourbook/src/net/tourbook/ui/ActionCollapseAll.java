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
package net.tourbook.ui;

import net.tourbook.Messages;
import net.tourbook.plugin.TourbookPlugin;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;

public class ActionCollapseAll extends Action {

	private ITourViewer	fTourViewer;

	public ActionCollapseAll(final ITourViewer tourViewer) {

		super(null, AS_PUSH_BUTTON);

		fTourViewer = tourViewer;

		setText(Messages.app_action_collapse_all_tooltip);
		setToolTipText(Messages.app_action_collapse_all_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__collapse_all));

	}

	@Override
	public void run() {

		if (fTourViewer != null) {

			final TreeViewer treeViewer = fTourViewer.getTreeViewer();

			treeViewer.collapseAll();

			final Object firstElement = ((StructuredSelection) treeViewer.getSelection()).getFirstElement();
			if (firstElement != null) {
				treeViewer.reveal(firstElement);
			}
		}
	}
}
