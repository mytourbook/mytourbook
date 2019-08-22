/*******************************************************************************
 * Copyright (C) 2005, 2019  Wolfgang Schramm and Contributors
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
import java.util.Iterator;
import java.util.Set;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;

public class ActionDeleteMarkerDialog extends Action {

   private ITourProvider _tourProvider;
   private TourMarker    _tourMarker;
   private boolean       _isSaveTour;

   /**
    * @param tourProvider
    * @param isSaveTour
    *           when <code>true</code> the tour will be saved when the marker dialog is closed
    */
   public ActionDeleteMarkerDialog(final ITourProvider tourProvider, final boolean isSaveTour) {

      _tourProvider = tourProvider;
      _isSaveTour = isSaveTour;

      setText("Delete marker(s)...");//Messages.app_action_edit_tour_marker);
      setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker));
      setDisabledImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image__edit_tour_marker_disabled));

      setEnabled(false);
   }

   public static void doAction(final ITourProvider tourProvider,
                               final boolean isSaveTour,
                               final TourMarker selectedTourMarker) {

      final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();

      // check if one tour is selected
      if (selectedTours == null || selectedTours.size() != 1 || selectedTours.get(0) == null) {
         return;
      }

      //TODO SEVERAL MARKERS CAN BE SELECTED. TAKE CARE OF THAT CASE.

      // Put an (s) inthe text. Supprimer le(s) marqueur(s)
      // or can the translation be changed depending if several markers ????

      final TourData tourData = selectedTours.get(0);

      if (tourData.isManualTour()) {
         // a manually created tour do not have time slices -> no markers
         return;
      }

      final Set<TourMarker> _originalTourMarkers = tourData.getTourMarkers();
      final Iterator<TourMarker> it = _originalTourMarkers.iterator();

      while (it.hasNext()) {
         if (it.next().getMarkerId() == selectedTourMarker.getMarkerId()) { // remove even elements
            it.remove();
         }
      }

      final Set<TourMarker> _newTourMarkers = new HashSet<>();

      for (final TourMarker tourMarker : _originalTourMarkers) {
         _newTourMarkers.add(tourMarker.clone());
      }

      tourData.setTourMarkers(_newTourMarkers);

      if (isSaveTour) {

         TourManager.saveModifiedTours(selectedTours);
      }

   }

   @Override
   public void run() {
      doAction(_tourProvider, _isSaveTour, _tourMarker);
   }

   public void setTourMarker(final TourMarker tourMarker) {
      _tourMarker = tourMarker;
   }

}
