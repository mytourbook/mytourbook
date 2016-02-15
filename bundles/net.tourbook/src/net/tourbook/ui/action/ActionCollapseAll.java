/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.ITourViewer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ActionCollapseAll extends Action {

	private ITourViewer	_tourViewer;

	public ActionCollapseAll(final ITourViewer tourViewer) {

		super(null, AS_PUSH_BUTTON);

		_tourViewer = tourViewer;

		setText(Messages.app_action_collapse_all_tooltip);
		setToolTipText(Messages.app_action_collapse_all_tooltip);
		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__collapse_all));
	}

	@Override
	public void run() {

		if (_tourViewer != null) {

			final ColumnViewer viewer = _tourViewer.getViewer();

			if (viewer instanceof TreeViewer) {

				final TreeViewer treeViewer = (TreeViewer) viewer;

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

						_tourViewer.getViewer().setSelection(new StructuredSelection(itemData), true);

						break;
					}
				}
			}
		}
	}
}
