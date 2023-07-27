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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
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

   private static void doAction(final ITourProvider tourProvider) {

      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

      // check if one tour is selected
      if (selectedTours == null || selectedTours.size() != 1 || selectedTours.get(0) == null) {
         return;
      }

      final TourData tourData = selectedTours.get(0);
      final long[] pausedTime_Data = tourData.getPausedTime_Data();

      if (tourData.isManualTour()) {
         // a manually created tour do not have time slices -> no markers
         return;
      }

      final String dialogTitle = Messages.Dlg_TourMarker_MsgBox_delete_marker_title;
      final String dialogMessage = "NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (selectedTourMarkers.get(0)).getLabel())";

//      if (_tourPausesViewSelectedIndices > 1) {
//         dialogTitle = Messages.Dlg_TourMarker_MsgBox_delete_markers_title;
//
//         final StringBuilder markersNames = new StringBuilder(UI.NEW_LINE);
//         for (final TourMarker tourMarker : selectedTourMarkers) {
//            if (markersNames.toString().isEmpty() == false) {
//               markersNames.append(UI.COMMA_SPACE);
//            }
//            markersNames.append("\"" + tourMarker.getLabel() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
//         }
//         dialogMessage = NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_markers_message, markersNames.toString());
//      }

      if (MessageDialog.openQuestion(
            Display.getDefault().getActiveShell(),
            dialogTitle,
            dialogMessage) == false) {
         return;
      }

      final List<TourMarker> _originalTourMarkers = tourData.getTourMarkers().stream().collect(Collectors.toList());

//      for (final TourMarker selectedTourMarker : selectedTourMarkers) {
//         _originalTourMarkers.removeIf(m -> m.getMarkerId() == selectedTourMarker.getMarkerId());
//      }

      final Set<TourMarker> _newTourMarkers = new HashSet<>();

      for (final TourMarker tourMarker : _originalTourMarkers) {
         _newTourMarkers.add(tourMarker.clone());
      }

      tourData.setTourMarkers(_newTourMarkers);

      TourManager.saveModifiedTours(selectedTours);

   }

   @Override
   public void run() {
      BusyIndicator.showWhile(Display.getCurrent(), () -> doAction(_tourProvider));

   }
}
