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
import java.util.List;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.widgets.Menu;

public class ActionSetGeoPositionForPhotoTours extends SubMenu {

   private Map2View    _map2View;

   private TourData    _tourData;

   private GeoPosition _currentMouseGeoPosition;

   private class ActionSetEndPosition extends Action {

      private int       _serieIndex;
      private TourPhoto _tourPhoto;

      public ActionSetEndPosition(final int serieIndex, final TourPhoto tourPhoto) {

         super(Messages.Map_Action_GeoPositions_Set_End, AS_PUSH_BUTTON);

         _serieIndex = serieIndex;
         _tourPhoto = tourPhoto;
      }

      @Override
      public void run() {

         setGeoPosition(_serieIndex, _tourPhoto);
      }
   }

   private class ActionSetStartPosition extends Action {

      private int       _serieIndex;
      private TourPhoto _tourPhoto;

      public ActionSetStartPosition(final int serieIndex, final TourPhoto tourPhoto) {

         super(Messages.Map_Action_GeoPositions_Set_Start, AS_PUSH_BUTTON);

         _serieIndex = serieIndex;
         _tourPhoto = tourPhoto;
      }

      @Override
      public void run() {

         setGeoPosition(_serieIndex, _tourPhoto);
      }
   }

   public ActionSetGeoPositionForPhotoTours(final Map2View map2View) {

      super(UI.EMPTY_STRING, AS_DROP_DOWN_MENU);

      _map2View = map2View;

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

      _tourData = _map2View.getTourDataWhereGeoPositionsCanBeSet();

      final Set<TourPhoto> allTourPhotos = _tourData.getTourPhotos();

      // sort photos by time
      final List<TourPhoto> allSortedPhotos = new ArrayList<>(allTourPhotos);
      Collections.sort(allSortedPhotos, (tourPhoto1, tourPhoto2) -> {
         return Long.compare(tourPhoto1.getImageExifTime(), tourPhoto2.getImageExifTime());
      });

      final int numPhotos = allSortedPhotos.size();
      final int lastPhotoIndex = numPhotos - 1;

      final TourPhoto tourPhotoStart = allSortedPhotos.get(0);
      final TourPhoto tourPhotoEnd = allSortedPhotos.get(lastPhotoIndex);

      final ActionSetStartPosition actionSetStartPosition = new ActionSetStartPosition(0, tourPhotoStart);
      final ActionSetEndPosition actionSetEndPosition = new ActionSetEndPosition(lastPhotoIndex, tourPhotoEnd);

      new ActionContributionItem(actionSetStartPosition).fill(menu, -1);
      new ActionContributionItem(actionSetEndPosition).fill(menu, -1);
   }

   public void setData(final GeoPosition currentMouseGeoPosition) {

      _currentMouseGeoPosition = currentMouseGeoPosition;
   }

   private void setGeoPosition(final int timeIndex, final TourPhoto tourPhoto) {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final int[] timeSerie = _tourData.timeSerie;
      double[] latSerie = _tourData.latitudeSerie;
      double[] lonSerie = _tourData.longitudeSerie;

      final int numTimeSlices = timeSerie.length;

      if (latSerie == null) {

         // create lat/lon when not available

         latSerie = new double[numTimeSlices];
         lonSerie = new double[numTimeSlices];

         _tourData.latitudeSerie = latSerie;
         _tourData.longitudeSerie = lonSerie;
      }

      /*
       * Update tour values
       */
      final double latitude = _currentMouseGeoPosition.latitude;
      final double longitude = _currentMouseGeoPosition.longitude;

      latSerie[timeIndex] = latitude;
      lonSerie[timeIndex] = longitude;

      if (tourPhoto != null) {

         tourPhoto.setGeoLocation(latitude, longitude);

         // keep state for which photo a geo position was set
         final Set<Long> allTourPhotosWithPositionedGeo = _tourData.getTourPhotosWithPositionedGeo();

         allTourPhotosWithPositionedGeo.add(tourPhoto.getPhotoId());
      }

      // interpolate geo positions
      _tourData.computeGeo_Photos();

      TourManager.saveModifiedTour(_tourData);
   }

}
