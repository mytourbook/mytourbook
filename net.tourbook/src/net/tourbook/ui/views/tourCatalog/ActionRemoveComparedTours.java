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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.EntityManager;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TourEventId;
import net.tourbook.ui.UI;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

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
	 * @param removedTours
	 * @return Returns <code>true</code> when the tours are removed
	 */
	private boolean removeComparedTours(final IStructuredSelection selection,
										final SelectionRemovedComparedTours removedTours) {

		// confirm removal
		if (MessageDialog.openConfirm(fTourView.getSite().getShell(),
				Messages.tourCatalog_view_dlg_delete_comparedTour_title,
				Messages.tourCatalog_view_dlg_delete_comparedTour_msg) == false) {

			return false;
		}

		final TreeViewer tourViewer = fTourView.getTourViewer();
		final ArrayList<Long> removedComparedTours = removedTours.removedComparedTours;

		// loop: selected items
		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			final Object element = iterator.next();
			if (element instanceof TVICatalogComparedTour) {

				final TVICatalogComparedTour compTourItem = (TVICatalogComparedTour) element;
				final long compId = compTourItem.getCompId();

				if (TourCompareManager.removeComparedTourFromDb(compId)) {

					// update model: remove compared tour 
					compTourItem.remove();

					// update viewer: remove item
					tourViewer.remove(compTourItem);

					// update selection
					removedComparedTours.add(compId);
				}
			}
		}

		return true;
	}

	/**
	 * @param selection
	 * @param removedTours
	 * @return Returns <code>true</code> when the tours are deleted
	 */
	private boolean removeRefTours(	final IStructuredSelection selection,
									final SelectionRemovedComparedTours removedTours) {

		// confirm deletion
		if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
				Messages.tourCatalog_view_dlg_delete_refTour_title,
				Messages.tourCatalog_view_dlg_delete_refTour_msg) == false) {
			return false;
		}

		final TreeViewer tourViewer = fTourView.getTourViewer();
		final ArrayList<Long> removedComparedTours = removedTours.removedComparedTours;
		final ArrayList<Long> modifiedRefTours = new ArrayList<Long>();

		for (final Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {

			final Object element = iterator.next();
			if (element instanceof TVICatalogRefTourItem) {

				/*
				 * remove all compared tours from the current reference tour
				 */

				final TVICatalogRefTourItem refTourItem = (TVICatalogRefTourItem) element;
				final Collection<StoredComparedTour> storedCompTours = TourCompareManager.getComparedToursFromDb(refTourItem.refId)
						.values();

				for (final Iterator<StoredComparedTour> iteratorCompTours = storedCompTours.iterator(); iteratorCompTours.hasNext();) {

					final long compId = (iteratorCompTours.next()).comparedId;

					TourCompareManager.removeComparedTourFromDb(compId);

					// change selection
					removedComparedTours.add(compId);
				}

				/*
				 * remove the reference tour from the tour and persist it
				 */
				final EntityManager em = TourDatabase.getInstance().getEntityManager();
				final TourReference refTour = em.find(TourReference.class, refTourItem.refId);

				if (refTour != null) {

					// get the ref tour from the database
					final TourData tourData = refTour.getTourData();

					if (tourData.getTourReferences().remove(refTour)) {
						TourDatabase.saveTour(tourData);

						modifiedRefTours.add(tourData.getTourId());
					}

					// remove the ref tour from the fDataModel
					refTourItem.remove();

					// remove the ref tour from the tree
					tourViewer.remove(refTourItem);
				}

				em.close();
			}
		}

		if (modifiedRefTours.size() > 0) {
			TourManager.fireEvent(TourEventId.UPDATE_UI, new SelectionTourIds(modifiedRefTours));
		}

		return true;
	}

	@Override
	public void run() {

		if (UI.isTourEditorModified()) {
			return;
		}

		final TreeViewer tourViewer = fTourView.getTourViewer();

		final SelectionRemovedComparedTours removedTours = new SelectionRemovedComparedTours();

		// get selected reference tours
		final IStructuredSelection selection = (IStructuredSelection) tourViewer.getSelection();

		final Object firstItem = selection.getFirstElement();

		boolean isRemoved = false;
		if (firstItem instanceof TVICatalogRefTourItem) {

			// remove the reference tours and it's children
			isRemoved = removeRefTours(selection, removedTours);

		} else if (firstItem instanceof TVICatalogComparedTour) {

			// remove compared tours
			isRemoved = removeComparedTours(selection, removedTours);
		}

		if (isRemoved) {
			// update the compare result view
			fTourView.fireSelection(removedTours);
		}
	}

}
