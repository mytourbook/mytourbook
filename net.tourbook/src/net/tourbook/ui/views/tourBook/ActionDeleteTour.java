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
package net.tourbook.ui.views.tourBook;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TreeViewerItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

public class ActionDeleteTour extends Action {

	private TourBookView	tourView;
	private TreeViewerItem	fNextSelectedTreeItem;

	public ActionDeleteTour(final TourBookView tourBookView) {

		this.tourView = tourBookView;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete_disabled));

		setText(Messages.Tour_Book_Action_delete_selected_tours);

		setEnabled(false);
	}

	private void deleteTours(final Iterator<?> selectedTreeItems, final SelectionDeletedTours selectionRemovedTours) {

		final ArrayList<ITourItem> removedTours = selectionRemovedTours.removedTours;

		final HashSet<TreeViewerItem> tourParents = new HashSet<TreeViewerItem>();

		int firstSelectedTourIndex = -1;
		TreeViewerItem firstSelectedParent = null;

		// loop: selected tours
		for (final Iterator<?> treeItem = selectedTreeItems; treeItem.hasNext();) {

			final Object treeTourItem = treeItem.next();

			if (treeTourItem instanceof TVITourBookTour) {

				final TVITourBookTour tourItem = (TVITourBookTour) treeTourItem;

				if (TourDatabase.removeTour(tourItem.getTourId())) {

					final TreeViewerItem tourParent = tourItem.getParentItem();

					tourParents.add(tourParent);

					// get the index for the first selected tour item
					if (firstSelectedTourIndex == -1) {
						final ArrayList<TreeViewerItem> parentTourItems = tourParent.getChildren();
						for (final TreeViewerItem firstTourItem : parentTourItems) {
							firstSelectedTourIndex++;
							if (firstTourItem == tourItem) {
								firstSelectedParent = tourParent;
								break;
							}
						}
					}

					// remove the tour from the data model
					tourParent.getChildren().remove(tourItem);

					// add to removed tour list
					removedTours.add(tourItem);
				}
			}
		}

		// refresh the tree viewer
		tourView.getTreeViewer().remove(removedTours.toArray(new TVITourBookTour[removedTours.size()]));

		/*
		 * select the item which is before the removed items, this is not yet finished because there
		 * are multiple possibilities
		 */

		fNextSelectedTreeItem = null;

		if (firstSelectedParent != null) {

			final ArrayList<TreeViewerItem> firstSelectedChildren = firstSelectedParent.getChildren();
			final int remainingChildren = firstSelectedChildren.size();

			if (remainingChildren > 0) {

				// there are children still available

				if (firstSelectedTourIndex < remainingChildren) {
					fNextSelectedTreeItem = firstSelectedChildren.get(firstSelectedTourIndex);
				} else {
					fNextSelectedTreeItem = firstSelectedChildren.get(remainingChildren - 1);
				}

			} else {

				/*
				 * it's possible that the parent does not have any children, then also this parent
				 * must be removed (to be done later)
				 */
				fNextSelectedTreeItem = firstSelectedParent;
				// for (TreeViewerItem tourParent : tourParents) {
				//					
				// }
			}
		}
	}

	@Override
	public void run() {

		if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				"Delete Tours",
				"Are you sure to delete the selected tours?") == false) {
			return;
		}

		final SelectionDeletedTours selectionRemovedTours = new SelectionDeletedTours();
		final TreeViewer treeViewer = tourView.getTreeViewer();

		// get selected reference tours
		final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();

		// delete selected tours
		deleteTours(selection.iterator(), selectionRemovedTours);

		// fire post selection
		tourView.firePostSelection(selectionRemovedTours);

		// set selection empty
		selectionRemovedTours.removedTours.clear();

		if (fNextSelectedTreeItem != null) {
			treeViewer.setSelection(new StructuredSelection(fNextSelectedTreeItem), true);
		}
	}

}
