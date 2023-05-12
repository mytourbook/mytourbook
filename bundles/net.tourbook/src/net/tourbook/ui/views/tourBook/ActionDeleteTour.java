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
package net.tourbook.ui.views.tourBook;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
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

   private TourBookView   _tourBookView;

   private TreeViewerItem _nextSelectedTreeItem;

   private final class TourItem implements ITourItem {

      private Long _tourId;

      public TourItem(final Long tourId) {
         _tourId = tourId;
      }

      @Override
      public Long getTourId() {
         return _tourId;
      }
   }

   public ActionDeleteTour(final TourBookView tourBookView) {

      _tourBookView = tourBookView;

      tourBookView.setActionDeleteTour(this);

      setText(Messages.Tour_Book_Action_delete_selected_tours);

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));
   }

   private void deleteTours(final ArrayList<Long> selectedTourIDs,
                            final IStructuredSelection treeSelection,
                            final SelectionDeletedTours selectionRemovedTours,
                            final IProgressMonitor monitor) {

      TourLogManager.addLog(TourLogState.DEFAULT, Messages.Log_Tour_DeleteTours, TourLogView.CSS_LOG_TITLE);

      if (_tourBookView.isLayoutNatTable()) {

         deleteTours_NatTable(selectedTourIDs, selectionRemovedTours, monitor);

      } else {

         deleteTours_Tree(treeSelection, selectionRemovedTours, monitor);
      }
   }

   private void deleteTours_NatTable(final ArrayList<Long> selectedTourIDs,
                                     final SelectionDeletedTours selectionRemovedTours,
                                     final IProgressMonitor monitor) {

      final int numSelectedTours = selectedTourIDs.size();
      int tourCounter = 0;

      final ArrayList<ITourItem> removedTours = selectionRemovedTours.removedTours;

      if (monitor != null) {
         monitor.beginTask(Messages.Tour_Book_Action_DeleteSelectedTours_Monitor, numSelectedTours);
      }

      // loop: selected tours
      for (final Long tourId : selectedTourIDs) {

         if (monitor != null) {

            monitor.subTask(NLS.bind(
                  Messages.Tour_Book_Action_DeleteSelectedTours_MonitorSubtask,
                  ++tourCounter,
                  numSelectedTours));

            if (monitor.isCanceled()) {
               break;
            }
         }

         final TourData tourData = TourManager.getTour(tourId);
         final String tourDate = TourManager.getTourDateTimeShort(tourData);

         if (TourDatabase.deleteTour(tourId)) {

            // log deletion
            TourLogManager.addSubLog(TourLogState.TOUR_DELETED, tourDate);

            // keep removed tour id
            removedTours.add(new TourItem(tourId));
         }

         if (monitor != null) {
            monitor.worked(1);
         }
      }
   }

   private void deleteTours_Tree(final IStructuredSelection selection,
                                 final SelectionDeletedTours selectionRemovedTours,
                                 final IProgressMonitor monitor) {

      final int selectionSize = selection.size();
      int tourCounter = 0;

      int firstSelectedTourIndex = -1;
      TreeViewerItem firstSelectedParent = null;

      final ArrayList<ITourItem> removedTours = selectionRemovedTours.removedTours;

      if (monitor != null) {
         monitor.beginTask(Messages.Tour_Book_Action_DeleteSelectedTours_Monitor, selectionSize);
      }

      // loop: selected tours
      for (final Object treeItem : selection) {

         if (monitor != null) {

            monitor.subTask(NLS.bind(
                  Messages.Tour_Book_Action_DeleteSelectedTours_MonitorSubtask,
                  ++tourCounter,
                  selectionSize));

            if (monitor.isCanceled()) {
               break;
            }
         }

         if (treeItem instanceof TVITourBookTour) {

            final TVITourBookTour tourItem = (TVITourBookTour) treeItem;

            final Long tourId = tourItem.getTourId();
            final TourData tourData = TourManager.getTour(tourId);
            final String tour = TourManager.getTourDateTimeShort(tourData);

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
             * it's possible that the parent does not have any children, then also this parent must
             * be removed (to be done later)
             */
//          _nextSelectedTreeItem = firstSelectedParent;
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

      final ArrayList<Long> selectedTourIDs = new ArrayList<>(_tourBookView.getSelectedTourIDs());
      final int numSelectedTours = selectedTourIDs.size();
      final int[] firstDeletePosition = { 0 };
      final IStructuredSelection[] treeSelection = new StructuredSelection[] { new StructuredSelection() };
      ColumnViewer treeViewer = null;

      final boolean isLayoutNatTable = _tourBookView.isLayoutNatTable();
      if (isLayoutNatTable) {

         final ArrayList<Long> firstTourId = new ArrayList<>();
         firstTourId.add(selectedTourIDs.get(0));

         final CompletableFuture<Void> rowIndexFuture = _tourBookView.getNatTable_DataLoader().getRowIndexFromTourId(firstTourId)

               .thenAccept(allRowPositions -> {

                  // keep row position for the first deleted tour
                  final int firstRowPosition = allRowPositions[0];

                  firstDeletePosition[0] = firstRowPosition;
               });

         // wait until row index is set
         rowIndexFuture.join();

      } else {

         treeViewer = _tourBookView.getViewer();

         // get selected tours
         treeSelection[0] = (IStructuredSelection) treeViewer.getSelection();
      }

      /*
       * confirm a second time
       */
      if (numSelectedTours > 0) {
         if (MessageDialog.openConfirm(
               Display.getCurrent().getActiveShell(),
               Messages.Tour_Book_Action_delete_selected_tours_dlg_title_confirm,
               NLS.bind(Messages.Tour_Book_Action_delete_selected_tours_dlg_message_confirm, numSelectedTours)) == false) {
            return;
         }
      }

      // log deletions
      TourLogManager.showLogView();

      final SelectionDeletedTours selectionForDeletedTours = new SelectionDeletedTours();

      if (numSelectedTours < 2) {

         // delete selected tours
         final Runnable deleteRunnable = () -> deleteTours(selectedTourIDs, treeSelection[0], selectionForDeletedTours, null);
         BusyIndicator.showWhile(Display.getCurrent(), deleteRunnable);

      } else {

         // delete selected tours
         final IRunnableWithProgress deleteRunnable = monitor -> deleteTours(selectedTourIDs,
               treeSelection[0],
               selectionForDeletedTours,
               monitor);

         try {
            new ProgressMonitorDialog(Display.getCurrent().getActiveShell()).run(true, true, deleteRunnable);
         } catch (InvocationTargetException | InterruptedException e) {
            StatusUtil.log(e);
         }
      }

      final PostSelectionProvider postSelectionProvider = _tourBookView.getPostSelectionProvider();

      // fire post selection
      postSelectionProvider.setSelection(selectionForDeletedTours);

      // set selection empty
      selectionForDeletedTours.removedTours.clear();
      postSelectionProvider.clearSelection();

      // ensure that the deleted item is removed
      _tourBookView.reloadViewer();

      // select next tour item
      if (isLayoutNatTable) {

         // select tour at the same position as the first deleted tour
         _tourBookView.selectTours_NatTable(firstDeletePosition, true, true, true);

      } else {

         if (_nextSelectedTreeItem != null) {
            treeViewer.setSelection(new StructuredSelection(_nextSelectedTreeItem), true);
         }
      }
   }

}
