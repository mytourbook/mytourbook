/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package net.tourbook.tour;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionDeletePausesDialog extends Action {

   private ITourProvider         _tourProvider;
   private int[]         _tourPausesViewSelectedIndices;

   public ActionDeletePausesDialog(final ITourProvider tourProvider) {

      _tourProvider = tourProvider;

      setText(Messages.App_Action_DeleteTourPauses);
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));

      setEnabled(false);
   }

   private void doAction(final ITourProvider tourProvider) {

      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

      // check if one tour is selected
      if (selectedTours == null || selectedTours.size() != 1 || selectedTours.get(0) == null) {
         return;
      }

      final TourData tourData = selectedTours.get(0);
      final List<Long> listPausedTime_Start = Arrays.stream(tourData.getPausedTime_Start()).boxed()
            .collect(Collectors.toList());
      final List<Long> listPausedTime_End = Arrays.stream(tourData.getPausedTime_End()).boxed()
            .collect(Collectors.toList());
      final List<Long> listPausedTime_Data = Arrays.stream(tourData.getPausedTime_Data()).boxed()
            .collect(Collectors.toList());

      if (tourData.isManualTour()) {
         // a manually created tour do not have time slices -> no  pauses
         return;
      }

      String dialogTitle = Messages.Dlg_TourMarker_MsgBox_delete_marker_title;
      String dialogMessage = "NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (selectedTourMarkers.get(0)).getLabel())";

      if (_tourPausesViewSelectedIndices.length > 1) {
         dialogTitle = Messages.Dlg_TourMarker_MsgBox_delete_markers_title;

         dialogMessage = "NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_markers_message, markersNames.toString())";
      }

      if (MessageDialog.openQuestion(
            Display.getDefault().getActiveShell(),
            dialogTitle,
            dialogMessage) == false) {
         return;
      }

      for (int index = 0; index < _tourPausesViewSelectedIndices.length; ++index)
      {
         listPausedTime_Start.remove(index);
         listPausedTime_End.remove(index);

         if (listPausedTime_Data != null && listPausedTime_Data.size() > 0) {
            listPausedTime_Data.remove(index);
         }

      }

      selectedTours.get(0).finalizeTour_TimerPauses(listPausedTime_Start, listPausedTime_End, listPausedTime_Data);
//      for (final TourMarker selectedTourMarker : selectedTourMarkers) {
//         _originalTourMarkers.removeIf(m -> m.getMarkerId() == selectedTourMarker.getMarkerId());
//      }

      TourManager.saveModifiedTours(selectedTours);
   }

   @Override
   public void run() {
      BusyIndicator.showWhile(Display.getCurrent(), () -> doAction(_tourProvider));

   }

   public void setTourPauses(final int[] tourPausesViewSelectedIndices) {

      _tourPausesViewSelectedIndices = tourPausesViewSelectedIndices;
   }
}
