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
package net.tourbook.ui.views.tourBook;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.PostSelectionProvider;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogState;
import net.tourbook.tour.TourLogView;
import net.tourbook.tour.TourManager;

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

	private TourBookView	_tourBookView;
	private TreeViewerItem	_nextSelectedTreeItem;

	public ActionDeleteTour(final TourBookView tourBookView) {

		_tourBookView = tourBookView;

		setText(Messages.Tour_Book_Action_delete_selected_tours);

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete_disabled));
	}

	private void deleteTours(	final IStructuredSelection selection,
								final SelectionDeletedTours selectionRemovedTours,
								final IProgressMonitor monitor) {

		TourLogManager.addLog(TourLogState.DEFAULT, TourLogManager.LOG_TOUR_DELETE_TOURS, TourLogView.CSS_LOG_TITLE);

		final int selectionSize = selection.size();
		int tourCounter = 0;

		int firstSelectedTourIndex = -1;
		TreeViewerItem firstSelectedParent = null;

		final ArrayList<ITourItem> removedTours = selectionRemovedTours.removedTours;

		if (monitor != null) {
			monitor.beginTask(Messages.Tour_Book_Action_DeleteSelectedTours_Monitor, selectionSize);
		}
		// loop: selected tours
		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			if (monitor != null) {

				monitor.subTask(NLS.bind(
						Messages.Tour_Book_Action_DeleteSelectedTours_MonitorSubtask,
						++tourCounter,
						selectionSize));

				if (monitor.isCanceled()) {
					break;
				}
			}

			final Object treeItem = iterator.next();
			if (treeItem instanceof TVITourBookTour) {

				final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

				final Long tourId = tourItem.getTourId();
				final TourData tourData = TourManager.getTour(tourId);
				final String tour = UI.DTFormatterShort.print(tourData.getTourStartTimeMS());

				if (TourDatabase.deleteTour(tourId)) {

					// log deletion
					TourLogManager.addSubLog(TourLogState.TOUR_DELETED, tour);

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
		_nextSelectedTreeItem = null;

		if (firstSelectedParent != null) {

			final ArrayList<TreeViewerItem> firstSelectedChildren = firstSelectedParent.getChildren();
			final int remainingChildren = firstSelectedChildren.size();

			if (remainingChildren > 0) {

				// there are children still available

				if (firstSelectedTourIndex < remainingChildren) {
					_nextSelectedTreeItem = firstSelectedChildren.get(firstSelectedTourIndex);
				} else {
					_nextSelectedTreeItem = firstSelectedChildren.get(remainingChildren - 1);
				}

			} else {

				/*
				 * it's possible that the parent does not have any children, then also this parent
				 * must be removed (to be done later)
				 */
//				_nextSelectedTreeItem = firstSelectedParent;
				// for (TreeViewerItem tourParent : tourParents) {
				//
				// }
			}
		}
	}

	@Override
	public void run() {

		if (TourManager.isTourEditorModified()) {
			return;
		}

		// confirm deletion
		if (MessageDialog.openConfirm(
				Display.getCurrent().getActiveShell(),
				Messages.Tour_Book_Action_delete_selected_tours_dlg_title,
				Messages.Tour_Book_Action_delete_selected_tours_dlg_message) == false) {
			return;
		}

		final ColumnViewer treeViewer = _tourBookView.getViewer();

		// get selected tours
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
			if (MessageDialog.openConfirm(
					Display.getCurrent().getActiveShell(),
					Messages.Tour_Book_Action_delete_selected_tours_dlg_title_confirm,
					NLS.bind(Messages.Tour_Book_Action_delete_selected_tours_dlg_message_confirm, selectedTours)) == false) {
				return;
			}
		}

		// log deletions
		TourLogManager.openLogView();

		final SelectionDeletedTours selectionRemovedTours = new SelectionDeletedTours();

		if (selectedTours < 2) {

			final Runnable deleteRunnable = new Runnable() {
				@Override
				public void run() {

					// delete selected tours
					deleteTours(selection, selectionRemovedTours, null);
				}
			};
			BusyIndicator.showWhile(Display.getCurrent(), deleteRunnable);

		} else {

			final IRunnableWithProgress deleteRunnable = new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

					// delete selected tours
					deleteTours(selection, selectionRemovedTours, monitor);
				}
			};

			try {
				new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, deleteRunnable);
			} catch (InvocationTargetException | InterruptedException e) {
				StatusUtil.log(e);
			}
		}

		final PostSelectionProvider postSelectionProvider = _tourBookView.getPostSelectionProvider();

		// fire post selection
		postSelectionProvider.setSelection(selectionRemovedTours);

		// set selection empty
		selectionRemovedTours.removedTours.clear();
		postSelectionProvider.clearSelection();

		// ensure that the deleted item is removed
		_tourBookView.reloadViewer();

		if (_nextSelectedTreeItem != null) {
			treeViewer.setSelection(new StructuredSelection(_nextSelectedTreeItem), true);
		}
	}

}
