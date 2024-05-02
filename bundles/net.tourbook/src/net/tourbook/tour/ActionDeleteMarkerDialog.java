/*******************************************************************************
 * Copyright (C) 2005, 2024 Wolfgang Schramm and Contributors
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class ActionDeleteMarkerDialog extends Action {

   private ITourProvider    _tourProvider;
   private List<TourMarker> _tourMarkers;

   public ActionDeleteMarkerDialog(final ITourProvider tourProvider) {

      _tourProvider = tourProvider;

      setText(Messages.App_Action_DeleteTourMarker);
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));

      setEnabled(false);
   }

   private boolean askUserConfirmation() {

      final String dialogTitle = _tourMarkers.size() == 1
            ? Messages.Dlg_TourMarker_MsgBox_delete_marker_title
            : Messages.Dlg_TourMarker_MsgBox_delete_markers_title;

      String dialogMessage;
      if (_tourMarkers.size() == 1) {

         dialogMessage = NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (_tourMarkers.get(0)).getLabel());
      } else {

         final StringBuilder markersNames = new StringBuilder(UI.NEW_LINE);
         for (final TourMarker tourMarker : _tourMarkers) {

            if (StringUtils.hasContent(markersNames.toString())) {
               markersNames.append(UI.COMMA_SPACE);
            }
            markersNames.append("\"" + tourMarker.getLabel() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
         }
         dialogMessage = NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_markers_message, markersNames.toString());
      }

      return MessageDialog.openQuestion(
            Display.getDefault().getActiveShell(),
            dialogTitle,
            dialogMessage);
   }

   private void doAction() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

      if (selectedTours == null || selectedTours.isEmpty() ||
            _tourMarkers == null || _tourMarkers.isEmpty()) {
         return;
      }

      final boolean isUserProceeding = askUserConfirmation();
      if (!isUserProceeding) {
         return;
      }

      // a manually created tour does not have time slices -> no markers
      selectedTours.removeIf(tour -> tour.isManualTour());

      for (final TourData tourData : selectedTours) {

         final Set<TourMarker> originalTourMarkers = tourData.getTourMarkers();

         for (final TourMarker selectedTourMarker : _tourMarkers) {
            originalTourMarkers.removeIf(m -> m.getMarkerId() == selectedTourMarker.getMarkerId());
         }

         final Set<TourMarker> newTourMarkers = new HashSet<>();

         for (final TourMarker tourMarker : originalTourMarkers) {
            newTourMarkers.add(tourMarker.clone());
         }

         tourData.setTourMarkers(newTourMarkers);
      }

      TourManager.saveModifiedTours(selectedTours);
   }

   @Override
   public void run() {
      BusyIndicator.showWhile(Display.getCurrent(), () -> doAction());
   }

   public void setTourMarkers(final List<TourMarker> tourMarkers) {

      _tourMarkers = tourMarkers;
   }

   public void setTourMarkers(final Object[] tourMarkers) {

      _tourMarkers = new ArrayList<>();

      Arrays.stream(tourMarkers).forEach(tourMarker -> _tourMarkers.add((TourMarker) tourMarker));
   }
}
