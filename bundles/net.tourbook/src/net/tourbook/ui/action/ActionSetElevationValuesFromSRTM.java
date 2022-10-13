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
package net.tourbook.ui.action;

import java.util.ArrayList;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.data.TourData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.importdata.RawDataManager.TourValueType;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;

import org.eclipse.jface.action.Action;

public class ActionSetElevationValuesFromSRTM extends Action {

   private final ITourProvider _tourProvider;

   public ActionSetElevationValuesFromSRTM(final ITourProvider tourDataEditor) {

      super(null, AS_PUSH_BUTTON);

      _tourProvider = tourDataEditor;

      setText(Messages.TourEditor_Action_SetAltitudeValuesFromSRTM);
   }

   @Override
   public void run() {

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

      final List<TourValueType> tourValueTypes = new ArrayList<>();
      tourValueTypes.add(TourValueType.TIME_SLICES__ELEVATION);
      // create dummy clone BEFORE oldTourData is modified
      final List<TourData> oldTourDataDummyClone = new ArrayList<>();

      for (final TourData tt : selectedTours) {
         oldTourDataDummyClone.add(RawDataManager.createTourDataDummyClone(tourValueTypes, tt));
      }

      if (TourManager.setElevationValuesFromSRTM(selectedTours)) {

         for (int index = 0; index < selectedTours.size(); ++index) {
            RawDataManager.displayTourModifiedDataDifferences(
                  TourValueType.TIME_SLICES__ELEVATION,
                  oldTourDataDummyClone.get(index),
                  selectedTours.get(index));
         }
         // save all modified tours
         TourManager.saveModifiedTours(selectedTours);
      }
   }
}
