/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import java.util.ArrayList;
import java.util.Collection;

import javax.persistence.EntityManager;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

public class ActionRemoveComparedTours extends Action {

   private ReferenceTourView _tourView;

   public ActionRemoveComparedTours(final ReferenceTourView view) {

      _tourView = view;

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));

      setText(Messages.RefTour_Action_DeleteTours);

      setEnabled(false);

   }

   /**
    * @param selection
    * @param selectionRemovedTours
    * @return Returns <code>true</code> when the tours are removed
    */
   private boolean removeComparedTours(final IStructuredSelection selection,
                                       final SelectionRemovedComparedTours selectionRemovedTours) {

      // confirm removal
      if (MessageDialog.openConfirm(_tourView.getSite().getShell(),
            Messages.RefTour_Dialog_DeleteComparedTour_Title,
            Messages.RefTour_Dialog_DeleteComparedTour_Message) == false) {

         return false;
      }

      final TreeViewer tourViewer = _tourView.getTourViewer();
      final ArrayList<ElevationCompareResult> removedComparedTours = selectionRemovedTours.removedComparedTours;

      // loop: selected items
      for (final Object element : selection) {
         if (element instanceof TVIRefTour_ComparedTour) {

            final TVIRefTour_ComparedTour compTourItem = (TVIRefTour_ComparedTour) element;
            final long compId = compTourItem.getCompareId();

            if (ElevationCompareManager.removeComparedTourFromDb(compId)) {

               // update model: remove compared tour
               compTourItem.remove();

               // update viewer: remove item
               tourViewer.remove(compTourItem);

               // update selection
               removedComparedTours.add(new ElevationCompareResult(

                     compId,
                     compTourItem.getTourId(),
                     compTourItem.refId));
            }
         }
      }

      return true;
   }

   /**
    * @param selection
    * @param selectionRemovedTours
    * @return Returns <code>true</code> when the tours are deleted
    */
   private boolean removeRefTours(final IStructuredSelection selection,
                                  final SelectionRemovedComparedTours selectionRemovedTours) {

      // confirm deletion
      if (MessageDialog.openConfirm(Display.getCurrent().getActiveShell(),
            Messages.RefTour_Dialog_DeleteRefTour_Title,
            Messages.RefTour_Dialog_DeleteRefTour_Message) == false) {

         return false;
      }

      final TreeViewer tourViewer = _tourView.getTourViewer();

      final ArrayList<ElevationCompareResult> removedComparedTours = selectionRemovedTours.removedComparedTours;
      final ArrayList<Long> modifiedRefTours = new ArrayList<>();

      for (final Object element : selection) {

         if (element instanceof TVIRefTour_RefTourItem) {

            /*
             * Remove all compared tours from the current reference tour
             */

            final TVIRefTour_RefTourItem refTourItem = (TVIRefTour_RefTourItem) element;
            final Collection<StoredComparedTour> allComparedToursFromDb = ElevationCompareManager.getComparedToursFromDb(refTourItem.refId).values();

            for (final StoredComparedTour comparedTour : allComparedToursFromDb) {

               final long compId = comparedTour.comparedId;

               ElevationCompareManager.removeComparedTourFromDb(compId);

               // change selection
               removedComparedTours.add(new ElevationCompareResult(

                     compId,
                     comparedTour.tourId,
                     comparedTour.refTourId));
            }

            /*
             * Remove the reference tour from the tour and save it
             * -> this will also delete TourReference in the db !
             */
            final EntityManager em = TourDatabase.getInstance().getEntityManager();
            final TourReference refTour = em.find(TourReference.class, refTourItem.refId);

            if (refTour != null) {

               // get the ref tour from the database
               final TourData tourData = refTour.getTourData();

               if (tourData.getTourReferences().remove(refTour)) {

                  TourDatabase.saveTour(tourData, false);

                  modifiedRefTours.add(tourData.getTourId());
               }

               // remove the ref tour from the data model
               refTourItem.remove();

               // remove the ref tour from the UI
               tourViewer.remove(refTourItem);
            }

            em.close();
         }
      }

      if (modifiedRefTours.size() > 0) {
         TourManager.fireEventWithCustomData(TourEventId.UPDATE_UI, new SelectionTourIds(modifiedRefTours), null);
      }

      return true;
   }

   @Override
   public void run() {

      if (TourManager.isTourEditorModified()) {
         return;
      }

      final TreeViewer tourViewer = _tourView.getTourViewer();

      final SelectionRemovedComparedTours removedTours = new SelectionRemovedComparedTours();

      // get selected tours
      final IStructuredSelection selection = (IStructuredSelection) tourViewer.getSelection();

      final Object firstItem = selection.getFirstElement();

      boolean isRemoved = false;
      if (firstItem instanceof TVIRefTour_RefTourItem) {

         // remove the reference tours and it's children
         isRemoved = removeRefTours(selection, removedTours);

      } else if (firstItem instanceof TVIRefTour_ComparedTour) {

         // remove compared tours
         isRemoved = removeComparedTours(selection, removedTours);
      }

      if (isRemoved) {

         // update the compare result view
         _tourView.fireSelection(removedTours);
      }
   }

}
