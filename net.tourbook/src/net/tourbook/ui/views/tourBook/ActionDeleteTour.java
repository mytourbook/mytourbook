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
package net.tourbook.ui.views.tourBook;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;
import net.tourbook.util.PostSelectionProvider;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionDeleteTour extends Action {

	private TourBookView	fTourViewer;
	private TreeViewerItem	fNextSelectedTreeItem;

	public ActionDeleteTour(final TourBookView tourBookView) {

		fTourViewer = tourBookView;

		setText(Messages.Tour_Book_Action_delete_selected_tours);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete_disabled));

		setEnabled(false);
	}

	private void deleteTours(	final IStructuredSelection selection,
								final SelectionDeletedTours selectionRemovedTours,
								final IProgressMonitor monitor) {

		int firstSelectedTourIndex = -1;
		TreeViewerItem firstSelectedParent = null;

		final ArrayList<ITourItem> removedTours = selectionRemovedTours.removedTours;

		if (monitor != null) {
			monitor.beginTask(Messages.Tour_Book_Action_delete_selected_tours_task, selection.size());
		}

		// loop: selected tours
		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			final Object treeItem = iterator.next();
			if (treeItem instanceof TVITourBookTour) {

				final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

				if (TourDatabase.removeTour(tourItem.getTourId())) {

					removedTours.add(tourItem);

					final TreeViewerItem tourParent = tourItem.getParentItem();

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
				}
			}

			if (monitor != null) {
				monitor.worked(1);
			}
		}

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

		if (UI.isTourEditorModified()) {
			return;
		}

		// confirm deletion
		if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				Messages.Tour_Book_Action_delete_selected_tours_dlg_title,
				Messages.Tour_Book_Action_delete_selected_tours_dlg_message) == false) {
			return;
		}

		// get selected tours
		final ColumnViewer treeViewer = fTourViewer.getViewer();
		final IStructuredSelection selection = (IStructuredSelection) treeViewer.getSelection();

		int selectedTours = 0;
		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			final Object nextElement = iterator.next();
			if (nextElement instanceof TVITourBookTour) {
				selectedTours++;
			}
		}

		/*
		 * confirm a second time
		 */
		if (selectedTours > 0) {
			if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
					Messages.Tour_Book_Action_delete_selected_tours_dlg_title_confirm,
					NLS.bind(Messages.Tour_Book_Action_delete_selected_tours_dlg_message_confirm, selectedTours)) == false) {
				return;
			}
		}

		final SelectionDeletedTours selectionRemovedTours = new SelectionDeletedTours();

		if (selectedTours < 2) {

			final Runnable deleteRunnable = new Runnable() {
				public void run() {
					// delete selected tours
					deleteTours(selection, selectionRemovedTours, null);
				}
			};
			BusyIndicator.showWhile(Display.getCurrent(), deleteRunnable);

		} else {

			final IRunnableWithProgress deleteRunnable = new IRunnableWithProgress() {
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					// delete selected tours
					deleteTours(selection, selectionRemovedTours, monitor);
				}
			};
			try {
				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, false, deleteRunnable);
			} catch (final InvocationTargetException e) {
				e.printStackTrace();
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}

		final PostSelectionProvider postSelectionProvider = fTourViewer.getPostSelectionProvider();

		// fire post selection
		postSelectionProvider.setSelection(selectionRemovedTours);

		// set selection empty
		selectionRemovedTours.removedTours.clear();
		postSelectionProvider.clearSelection();

		if (fNextSelectedTreeItem != null) {
			fTourViewer.getViewer().setSelection(new StructuredSelection(fNextSelectedTreeItem), true);
		}
	}

}
