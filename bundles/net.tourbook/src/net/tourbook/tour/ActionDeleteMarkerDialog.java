/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
import java.util.stream.Collectors;

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

   private ITourProvider         _tourProvider;
   private ArrayList<TourMarker> _tourMarkers;

   public ActionDeleteMarkerDialog(final ITourProvider tourProvider) {

      _tourProvider = tourProvider;

      setText(Messages.App_Action_DeleteTourMarker);
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete_Disabled));

      setEnabled(false);
   }

   private void doAction() {

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

      // check if one tour is selected
      if (selectedTours == null || selectedTours.size() != 1 || selectedTours.get(0) == null ||
            _tourMarkers == null || _tourMarkers.isEmpty() || _tourMarkers.get(0) == null) {
         return;
      }

      final TourData tourData = selectedTours.get(0);

      if (tourData.isManualTour()) {
         // a manually created tour does not have time slices -> no markers
         return;
      }

      String dialogTitle = Messages.Dlg_TourMarker_MsgBox_delete_marker_title;
      String dialogMessage = NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_marker_message, (_tourMarkers.get(0)).getLabel());

      if (_tourMarkers.size() > 1) {
         dialogTitle = Messages.Dlg_TourMarker_MsgBox_delete_markers_title;

         final StringBuilder markersNames = new StringBuilder(UI.NEW_LINE);
         for (final TourMarker tourMarker : _tourMarkers) {

            if (StringUtils.hasContent(markersNames.toString())) {
               markersNames.append(UI.COMMA_SPACE);
            }
            markersNames.append("\"" + tourMarker.getLabel() + "\""); //$NON-NLS-1$ //$NON-NLS-2$
         }
         dialogMessage = NLS.bind(Messages.Dlg_TourMarker_MsgBox_delete_markers_message, markersNames.toString());
      }

      if (MessageDialog.openQuestion(
            Display.getDefault().getActiveShell(),
            dialogTitle,
            dialogMessage) == false) {
         return;
      }

      final List<TourMarker> _originalTourMarkers = tourData.getTourMarkers().stream().collect(Collectors.toList());

      for (final TourMarker selectedTourMarker : _tourMarkers) {
         _originalTourMarkers.removeIf(m -> m.getMarkerId() == selectedTourMarker.getMarkerId());
      }

      final Set<TourMarker> _newTourMarkers = new HashSet<>();

      for (final TourMarker tourMarker : _originalTourMarkers) {
         _newTourMarkers.add(tourMarker.clone());
      }

      tourData.setTourMarkers(_newTourMarkers);

      TourManager.saveModifiedTours(selectedTours);

   }

   @Override
   public void run() {
      BusyIndicator.showWhile(Display.getCurrent(), () -> doAction());

   }

   public void setTourMarkers(final Object[] tourMarkers) {

      _tourMarkers = new ArrayList<>();

      Arrays.stream(tourMarkers).forEach(tourMarker -> _tourMarkers.add((TourMarker) tourMarker));
   }
}
