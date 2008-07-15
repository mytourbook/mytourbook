/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.Iterator;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.TreeViewerItem;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;

public class ActionRemoveComparedTours extends Action {

	private TourCatalogView	fTourView;

	public ActionRemoveComparedTours(final TourCatalogView view) {

		fTourView = view;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete));
		setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__delete_disabled));

		setText(Messages.tourCatalog_view_action_delete_tours);

		setEnabled(false);

	}

	/**
	 * @param selection
	 * @param selectionRemovedComparedTours
	 * @return Returns <code>true</code> when the tours are deleted
	 */
	private boolean deleteComparedTours(final Iterator<TVICatalogReferenceTour> selection,
										final SelectionRemovedComparedTours selectionRemovedComparedTours) {

//		// confirm deletion
//		final String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };
//
//		final MessageDialog dialog = new MessageDialog(fTourView.getSite().getShell(),
//				Messages.tourCatalog_view_dlg_delete_comparedTour_title,
//				null,
//				Messages.tourCatalog_view_dlg_delete_comparedTour_msg,
//				MessageDialog.QUESTION,
//				buttons,
//				1);

		if (MessageDialog.openQuestion(fTourView.getSite().getShell(),
				Messages.tourCatalog_view_dlg_delete_comparedTour_title,
				Messages.tourCatalog_view_dlg_delete_comparedTour_msg)) {

			final TreeViewer tourViewer = fTourView.getTourViewer();

			// loop: selected tours
			for (final Iterator<TVICatalogReferenceTour> selTour = selection; selTour.hasNext();) {

				final Object tourItem = selTour.next();

				if (tourItem instanceof TVICatalogComparedTour) {

					final TVICatalogComparedTour compTourItem = (TVICatalogComparedTour) tourItem;
					final long compId = compTourItem.getCompId();

					if (TourCompareManager.removeComparedTourFromDb(compId)) {

						// remove compared tour from the fDataModel
						compTourItem.remove();

						// remove compared tour from the tree viewer
						tourViewer.remove(compTourItem);

						// update selection
						selectionRemovedComparedTours.removedComparedTours.add(compId);
					}
				}
			}
			return true;
		}

		return false;
	}

	/**
	 * @param selection
	 * @param refCompTourSelection
	 * @return Returns <code>true</code> when the tours are deleted
	 */
	private boolean deleteRefTours(	final Iterator<TVICatalogReferenceTour> selection,
									final SelectionRemovedComparedTours refCompTourSelection) {

		// confirm deletion
		final String[] buttons = new String[] { IDialogConstants.OK_LABEL, IDialogConstants.CANCEL_LABEL };

		final MessageDialog dialog = new MessageDialog(fTourView.getSite().getShell(),
				Messages.tourCatalog_view_dlg_delete_refTour_title,
				null,
				Messages.tourCatalog_view_dlg_delete_refTour_msg,
				MessageDialog.QUESTION,
				buttons,
				1);

		if (dialog.open() != Window.OK) {
			return false;
		}

		final TreeViewer tourViewer = fTourView.getTourViewer();

		for (final Iterator<TVICatalogReferenceTour> selTour = selection; selTour.hasNext();) {

			final TVICatalogReferenceTour refTourItem = selTour.next();

			final ArrayList<TreeViewerItem> unfetchedChildren = refTourItem.getUnfetchedChildren();

			// remove all compared tours from the current ref tour
			if (unfetchedChildren != null) {

				// remove all compared tours for the current ref tour from
				// the database
				for (final Iterator<TreeViewerItem> compTour = unfetchedChildren.iterator(); selTour.hasNext();) {

					final long compId = ((TVICatalogComparedTour) compTour.next()).getCompId();

					TourCompareManager.removeComparedTourFromDb(compId);

					// change selection
					refCompTourSelection.removedComparedTours.add(compId);
				}
			}

			TourData tourData;
			TourReference refTour;

			// get the ref tour from the database
			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			refTour = em.find(TourReference.class, refTourItem.refId);

			if (refTour != null) {

				// remove the reference tour from the tour data and persist the
				// tour data
				tourData = refTour.getTourData();

				if (tourData.getTourReferences().remove(refTour)) {
					TourDatabase.saveTour(tourData);
				}

				// remove the ref tour from the fDataModel
				refTourItem.remove();

				// remove the ref tour from the tree
				tourViewer.remove(refTourItem);
			}

			em.close();
		}

		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {

		final TreeViewer tourViewer = fTourView.getTourViewer();

		final SelectionRemovedComparedTours selectionRemovedTours = new SelectionRemovedComparedTours();

		// get selected reference tours
		final IStructuredSelection selection = (IStructuredSelection) tourViewer.getSelection();

		final Object firstItem = selection.getFirstElement();

		boolean isDeleted = false;
		if (firstItem instanceof TVICatalogReferenceTour) {

			// delete the reference tours and it's children
			isDeleted = deleteRefTours(selection.iterator(), selectionRemovedTours);

		} else if (firstItem instanceof TVICatalogComparedTour) {

			// delete compared tours
			isDeleted = deleteComparedTours(selection.iterator(), selectionRemovedTours);
		}

		if (isDeleted) {
			// update the compare result view
			fTourView.fireSelection(selectionRemovedTours);
		}
	}

}
