/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.action;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.map2.Messages;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

public class ActionSetGeoPosition extends SubMenu {

   private SubAction_DeletePositions     _actionDeleteStartAndEndPositions;
   private SubAction_SetStartPosition    _actionSetStartPosition;
   private SubAction_SetEndPosition      _actionSetEndPosition;
   private SubAction_SetStartEndPosition _actionSetStartAndEndPositions;

   private TourData                      _tourData;
   private GeoPosition                   _geoPosition;

   private class SubAction_DeletePositions extends Action {

      public SubAction_DeletePositions() {

         super(Messages.Map_Action_GeoPositions_Delete, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {
         setGeoPositions(false, false);
      }
   }

   private class SubAction_SetEndPosition extends Action {

      public SubAction_SetEndPosition() {
         super(Messages.Map_Action_GeoPositions_Set_End, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         setGeoPositions(false, true);
      }
   }

   private class SubAction_SetStartEndPosition extends Action {

      public SubAction_SetStartEndPosition() {
         super(Messages.Map_Action_GeoPositions_Set_StartAndEnd, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         setGeoPositions(true, true);
      }
   }

   private class SubAction_SetStartPosition extends Action {

      public SubAction_SetStartPosition() {
         super(Messages.Map_Action_GeoPositions_Set_Start, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
         setGeoPositions(true, false);
      }
   }

   public ActionSetGeoPosition() {

      super(UI.EMPTY_STRING, AS_DROP_DOWN_MENU);

      setToolTipText(
            Messages.Map_Action_GeoPositions_Tooltip);

      _actionSetStartPosition = new SubAction_SetStartPosition();
      _actionSetEndPosition = new SubAction_SetEndPosition();
      _actionSetStartAndEndPositions = new SubAction_SetStartEndPosition();
      _actionDeleteStartAndEndPositions = new SubAction_DeletePositions();
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      new ActionContributionItem(_actionSetStartPosition).fill(menu, -1);
      new ActionContributionItem(_actionSetEndPosition).fill(menu, -1);
      new ActionContributionItem(_actionSetStartAndEndPositions).fill(menu, -1);
      new ActionContributionItem(_actionDeleteStartAndEndPositions).fill(menu, -1);
   }

   public void setData(final TourData tourData, final GeoPosition geoPosition) {

      _tourData = tourData;
      _geoPosition = geoPosition;
   }

   private void setGeoPositions(final boolean isSetStartPosition, final boolean isSetEndPosition) {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      int[] timeSerie = null;

      double[] latSerie = null;
      double[] lonSerie = null;

      if (isSetStartPosition == false && isSetEndPosition == false) {

         // reset geo positions

      } else {

         timeSerie = _tourData.timeSerie;

         latSerie = _tourData.latitudeSerie;
         lonSerie = _tourData.longitudeSerie;

         if (timeSerie == null) {

            timeSerie = new int[2];

         } else if (timeSerie.length == 1) {

            // preserve old values

            final int timeValue_0 = timeSerie[0];

            timeSerie = new int[2];

            timeSerie[0] = timeValue_0;
         }

         if (latSerie == null) {

            latSerie = new double[2];
            lonSerie = new double[2];

         } else if (latSerie.length == 1) {

            // preserve old values
            final double latValue_0 = latSerie[0];
            final double lonValue_0 = lonSerie[0];

            latSerie = new double[2];
            lonSerie = new double[2];

            latSerie[0] = latValue_0;
            lonSerie[0] = lonValue_0;
         }

         final int tourElapsedTime = (int) _tourData.getTourDeviceTime_Elapsed();
         final double latitude = _geoPosition.latitude;
         final double longitude = _geoPosition.longitude;

         if (isSetStartPosition && isSetEndPosition) {

            timeSerie[0] = 0;
            timeSerie[1] = tourElapsedTime;

            latSerie[0] = latitude;
            latSerie[1] = latitude;

            lonSerie[0] = longitude;
            lonSerie[1] = longitude;

         } else if (isSetStartPosition) {

            timeSerie[0] = 0;

            latSerie[0] = latitude;
            lonSerie[0] = longitude;

         } else if (isSetEndPosition) {

            timeSerie[1] = tourElapsedTime;

            latSerie[1] = latitude;
            lonSerie[1] = longitude;
         }

// SET_FORMATTING_OFF

         // prevent 0 values

         if (timeSerie[1] == 0)  { timeSerie[1] = tourElapsedTime; }

         if (latSerie[0] == 0)   { latSerie[0]  = latitude; }
         if (latSerie[1] == 0)   { latSerie[1]  = latitude; }

         if (lonSerie[0] == 0)   { lonSerie[0]  = longitude; }
         if (lonSerie[1] == 0)   { lonSerie[1]  = longitude; }

// SET_FORMATTING_ON
      }

      // update tour values
      _tourData.timeSerie = timeSerie;
      _tourData.latitudeSerie = latSerie;
      _tourData.longitudeSerie = lonSerie;

      TourManager.saveModifiedTour(_tourData);
   }
}
