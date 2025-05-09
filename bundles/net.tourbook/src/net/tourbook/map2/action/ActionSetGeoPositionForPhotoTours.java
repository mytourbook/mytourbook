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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import net.tourbook.Images;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.map2.Messages;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

public class ActionSetGeoPositionForPhotoTours extends SubMenu {

   private TourData    _tourData;
   private GeoPosition _currentMouseGeoPosition;

   private class ActionSetGeoPosition extends Action {

      private int _geoTime;
      private int _timeIndex;

      public ActionSetGeoPosition(final int timeIndex, final int geoTime, final String photoLabel) {

         super(photoLabel != null

               ? photoLabel
               : Integer.toString(geoTime),

               AS_PUSH_BUTTON);

         _timeIndex = timeIndex;
         _geoTime = geoTime;
      }

      @Override
      public void run() {

         setGeoPosition(_timeIndex, _geoTime);
      }
   }

   private class SubAction_DeletePositions extends Action {

      public SubAction_DeletePositions() {

         super(Messages.Map_Action_GeoPositions_Delete, AS_PUSH_BUTTON);

         setImageDescriptor(TourbookPlugin.getImageDescriptor(Images.App_Delete));
      }

      @Override
      public void run() {
//         setGeoPositions(false, false);
      }
   }

   private class SubAction_SetEndPosition extends Action {

      public SubAction_SetEndPosition() {
         super(Messages.Map_Action_GeoPositions_Set_End, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
//         setGeoPositions(false, true);
      }
   }

   private class SubAction_SetStartEndPosition extends Action {

      public SubAction_SetStartEndPosition() {
         super(Messages.Map_Action_GeoPositions_Set_StartAndEnd, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
//         setGeoPositions(true, true);
      }
   }

   private class SubAction_SetStartPosition extends Action {

      public SubAction_SetStartPosition() {
         super(Messages.Map_Action_GeoPositions_Set_Start, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {
//         setGeoPositions(true, false);
      }
   }

   public ActionSetGeoPositionForPhotoTours() {

      super(UI.EMPTY_STRING, AS_DROP_DOWN_MENU);

      /**
       * <pre>
       *
       * Set geo positions from the current mouse position
       * into the selected tour
       *
       * This feature is only available when a tour
       * do not contain any geo positions or max 2 geo positions
       *
       * </pre>
       */

      setToolTipText(Messages.Map_Action_GeoPositions_Tooltip);
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      final int[] timeSerie = _tourData.timeSerie;

      final Set<TourPhoto> allTourPhotos = _tourData.getTourPhotos();
      final ArrayList<TourPhoto> allSortedPhotos = new ArrayList<>(allTourPhotos);

      Collections.sort(allSortedPhotos, (tourPhoto1, tourPhoto2) -> {

         return Long.compare(tourPhoto1.getImageExifTime(), tourPhoto2.getImageExifTime());
      });

      // prevent too many actions
      final int numTimeSlices = Math.min(50, timeSerie.length);
      final int numPhotos = allSortedPhotos.size();

      for (int timeIndex = 0; timeIndex < numTimeSlices; timeIndex++) {

         String photoLabel = null;

         final int relativeTime = timeSerie[timeIndex];

         if (timeIndex > 0 && (timeIndex - 1) < numPhotos) {

            final TourPhoto tourPhoto = allSortedPhotos.get(timeIndex - 1);

            final String photoTime = TimeTools.getZonedDateTime(tourPhoto.getImageExifTime()).format(TimeTools.Formatter_Time_M);
            final String imageFileName = tourPhoto.getImageFileName();

            photoLabel = photoTime + UI.DASH_WITH_DOUBLE_SPACE + imageFileName;
         }

         final ActionSetGeoPosition action = new ActionSetGeoPosition(timeIndex, relativeTime, photoLabel);

         new ActionContributionItem(action).fill(menu, -1);
      }
   }

   public void setData(final TourData tourData, final GeoPosition currentMouseGeoPosition) {

      _tourData = tourData;
      _currentMouseGeoPosition = currentMouseGeoPosition;
   }

   private void setGeoPosition(final int timeIndex, final int geoTime) {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final int[] timeSerie = _tourData.timeSerie;
      double[] latSerie = _tourData.latitudeSerie;
      double[] lonSerie = _tourData.longitudeSerie;

      final int numTimeSlices = timeSerie.length;

      if (latSerie == null) {

         latSerie = new double[numTimeSlices];
         lonSerie = new double[numTimeSlices];

         _tourData.latitudeSerie = latSerie;
         _tourData.longitudeSerie = lonSerie;
      }

      // update tour values
      latSerie[timeIndex] = _currentMouseGeoPosition.latitude;
      lonSerie[timeIndex] = _currentMouseGeoPosition.longitude;

      TourManager.saveModifiedTour(_tourData);
   }

}
