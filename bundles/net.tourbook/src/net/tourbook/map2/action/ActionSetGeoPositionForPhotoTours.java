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
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;

public class ActionSetGeoPositionForPhotoTours extends SubMenu {

   private static final String LAT_LON = "%8.4f %8.4f"; //$NON-NLS-1$

   private Map2View            _map2View;

   private TourData            _tourData;

   private GeoPosition         _currentMouseGeoPosition;

   private List<TourPhoto>     _allSortedPhotos;

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

   /**
    * This is an action for each photo
    */
   private class ActionSetGeoPosition extends Action {

      private int       _serieIndex;

      /**
       * Can be <code>null</code> when this action/time slice has not a photo
       */
      private TourPhoto _tourPhoto;

      public ActionSetGeoPosition(final String photoLabel,
                                  final int serieIndex,
                                  final TourPhoto tourPhoto) {

         super(photoLabel, AS_PUSH_BUTTON);

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
      _allSortedPhotos = new ArrayList<>(allTourPhotos);
      Collections.sort(_allSortedPhotos, (tourPhoto1, tourPhoto2) -> {

         return Long.compare(tourPhoto1.getImageExifTime(), tourPhoto2.getImageExifTime());
      });

      final int[] timeSerie = _tourData.timeSerie;
      final int numTimeSlices = timeSerie.length;

      final TourPhoto tourPhotoStart = _allSortedPhotos.get(0);
      final TourPhoto tourPhotoEnd = _allSortedPhotos.get(numTimeSlices - 1);

      final ActionSetStartPosition actionSetStartPosition = new ActionSetStartPosition(0, tourPhotoStart);
      final ActionSetEndPosition actionSetEndPosition = new ActionSetEndPosition(numTimeSlices - 1, tourPhotoEnd);

      new ActionContributionItem(actionSetStartPosition).fill(menu, -1);
      new ActionContributionItem(actionSetEndPosition).fill(menu, -1);

      new Separator().fill(menu, -1);

      fillMenu_ForEachPhoto(menu);
   }

   private void fillMenu_ForEachPhoto(final Menu menu) {

      final int[] timeSerie = _tourData.timeSerie;

      final Set<Long> allTourPhotosWithPositionedGeo = _tourData.getTourPhotosWithPositionedGeo();

      // prevent too many actions
      final int numTimeSlices = Math.min(100, timeSerie.length);

      for (int timeIndex = 0; timeIndex < numTimeSlices; timeIndex++) {

         String photoGeoInfo = UI.EMPTY_STRING;
         String photoLabel = null;
         TourPhoto tourPhoto = null;

         tourPhoto = _allSortedPhotos.get(timeIndex);

         final long photoExifTime = tourPhoto.getImageExifTime();
         final String photoTime = TimeTools.getZonedDateTime(photoExifTime).format(TimeTools.Formatter_Time_M);
         final String imageFileName = tourPhoto.getImageFileName();
         final long tourPhotoId = tourPhoto.getPhotoId();

         final boolean isPhotoWithGeoPosition = allTourPhotosWithPositionedGeo.contains(tourPhotoId);

         if (isPhotoWithGeoPosition) {

            photoGeoInfo = UI.DASH_WITH_DOUBLE_SPACE + LAT_LON.formatted(

                  tourPhoto.getLatitude(),
                  tourPhoto.getLongitude());
         }

         photoLabel = photoTime
               + UI.DASH_WITH_DOUBLE_SPACE + imageFileName
               + photoGeoInfo;

         final ActionSetGeoPosition action = new ActionSetGeoPosition(photoLabel, timeIndex, tourPhoto);

         new ActionContributionItem(action).fill(menu, -1);
      }
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

      final Set<Long> allTourPhotosWithPositionedGeo = _tourData.getTourPhotosWithPositionedGeo();

      if (tourPhoto != null) {

         tourPhoto.setGeoLocation(latitude, longitude);

         // keep state for which photo a geo position was set
         allTourPhotosWithPositionedGeo.add(tourPhoto.getPhotoId());
      }

      // interpolate geo positions
      _tourData.computeGeo_Photos();

      TourManager.saveModifiedTour(_tourData);
   }

}
