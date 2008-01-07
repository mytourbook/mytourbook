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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.window.Window;

class ActionRenameRefTour extends Action {

	/**
	 * 
	 */
	private final TourCatalogView	fTourCatalogView;

	public ActionRenameRefTour(TourCatalogView tourCatalogView) {

		super(Messages.tourCatalog_view_action_rename_reference_tour);
		fTourCatalogView = tourCatalogView;
	}

	@Override
	public void run() {

		final Object selectedItem = (((ITreeSelection) fTourCatalogView.getTourViewer().getSelection()).getFirstElement());

		if (selectedItem instanceof TourCatalogItemReferenceTour) {

			final TourCatalogItemReferenceTour ttiRefTour = (TourCatalogItemReferenceTour) selectedItem;

			// ask for the reference tour name
			final InputDialog dialog = new InputDialog(this.fTourCatalogView.getSite().getShell(),
					Messages.tourCatalog_view_dlg_rename_reference_tour_title,
					Messages.tourCatalog_view_dlg_rename_reference_tour_msg,
					ttiRefTour.label,
					null);

			if (dialog.open() != Window.OK) {
				return;
			}

			// get the ref tour from the database
			final EntityManager em = TourDatabase.getInstance().getEntityManager();
			final TourReference refTour = em.find(TourReference.class, ttiRefTour.refId);

			if (refTour != null) {

				final EntityTransaction ts = em.getTransaction();

				// persist the changed ref tour
				try {
					// change the label
					refTour.setLabel(dialog.getValue());

					ts.begin();
					em.merge(refTour);
					ts.commit();
				} catch (final Exception e) {
					e.printStackTrace();
				} finally {
					if (ts.isActive()) {
						ts.rollback();
					} else {

						// refresh the tree viewer and resort the ref tours
						fTourCatalogView.fRootItem.fetchChildren();
						fTourCatalogView.getTourViewer().refresh();
					}
					em.close();
				}
			}
		}
	}
}
