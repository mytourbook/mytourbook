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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.data.TourPhoto;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map2.Messages;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.photo.TourPhotoManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;

/**
 */
public class ActionSetGeoPositionForGeoMarker extends SubMenu {

   public static final String GEO_MARKER_PREFIX = "#Geo"; //$NON-NLS-1$

   private List<TourMarker>   _allGeoMarker;
   private GeoPosition        _currentMouseGeoPosition;

   private class ActionRemoveAllGeoPositions extends Action {

      public ActionRemoveAllGeoPositions() {

         super(Messages.Map_Action_GeoMarkerPosition_RemoveAll, AS_PUSH_BUTTON);

         setToolTipText(Messages.Map_Action_GeoMarkerPosition_RemoveAll_Tooltip);
      }

      @Override
      public void run() {

         actionRemoveAllGeoPositions();
      }
   }

   private class ActionSetGeoPosition extends Action {

      private TourMarker _geoMarker;

      public ActionSetGeoPosition(final TourMarker geoMarker) {

         super(geoMarker.getLabel(), AS_PUSH_BUTTON);

         _geoMarker = geoMarker;
      }

      @Override
      public void run() {

         actionSetGeoPositionIntoGeoMarker(_geoMarker);
      }
   }

   public ActionSetGeoPositionForGeoMarker() {

      super(Messages.Map_Action_GeoMarkerPosition_SetIntoMarker.formatted(GEO_MARKER_PREFIX), AS_DROP_DOWN_MENU);

      setToolTipText(Messages.Map_Action_GeoMarkerPosition_SetIntoMarker_Tooltip.formatted(GEO_MARKER_PREFIX));
   }

   private void actionRemoveAllGeoPositions() {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      TourData tourData = null;

      // discard all geo marker
      for (final TourMarker tourMarker : _allGeoMarker) {

         tourMarker.setDescription(UI.EMPTY_STRING);

         // get tour data from a marker
         tourData = tourMarker.getTourData();
      }

      if (tourData == null) {
         return;
      }

      final ImportState_Process importState_Process = new ImportState_Process()

            // do not interpolate geo positions during the reimport
            .setIsSkipGeoInterpolation(true);

      final TourData reimportedTourData = RawDataManager.getInstance().reimportTour(tourData, importState_Process);

      if (reimportedTourData == null) {
         return;
      }

      cleanupGeoPositions(reimportedTourData);

      final int[] timeSerie = reimportedTourData.timeSerie;
      final double[] reimportedLatitudeSerie = reimportedTourData.latitudeSerie;
      final double[] reimportedLongitudeSerie = reimportedTourData.longitudeSerie;

      // interpolate lat/lon
      reimportedTourData.createTimeSeries_20_InterpolateMissingValues(reimportedLatitudeSerie, timeSerie, false);
      reimportedTourData.createTimeSeries_20_InterpolateMissingValues(reimportedLongitudeSerie, timeSerie, false);

      // adjust distance/speed values
      TourManager.computeDistanceValuesFromGeoPosition(reimportedTourData);

      tourData.latitudeSerie = reimportedLatitudeSerie;
      tourData.longitudeSerie = reimportedLongitudeSerie;
      tourData.distanceSerie = reimportedTourData.distanceSerie;

      TourManager.saveModifiedTour(tourData);
   }

   private void actionSetGeoPositionIntoGeoMarker(final TourMarker geoMarker) {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final TourData tourData = geoMarker.getTourData();

      final ImportState_Process importState_Process = new ImportState_Process()

            // do not interpolate geo positions during the reimport
            .setIsSkipGeoInterpolation(true);

      final TourData reimportedTourData = RawDataManager.getInstance().reimportTour(tourData, importState_Process);

      if (reimportedTourData == null) {
         return;
      }

      cleanupGeoPositions(reimportedTourData);

      final int[] timeSerie = tourData.timeSerie;
      final double[] reimportedLatitudeSerie = reimportedTourData.latitudeSerie;
      final double[] reimportedLongitudeSerie = reimportedTourData.longitudeSerie;

      final int geoMarkerSerieIndex = geoMarker.getSerieIndex();
      final double geoMarkerLatitude = _currentMouseGeoPosition.latitude;
      final double geoMarkerLongitude = _currentMouseGeoPosition.longitude;

      // insert current geo position with the current geo marker position into the lat/lon values
      reimportedLatitudeSerie[geoMarkerSerieIndex] = geoMarkerLatitude;
      reimportedLongitudeSerie[geoMarkerSerieIndex] = geoMarkerLongitude;

      // set lat/lon for all other geo markers
      createOtherGeoPositions(geoMarker, tourData, reimportedLatitudeSerie, reimportedLongitudeSerie);

      // interpolate lat/lon
      tourData.createTimeSeries_20_InterpolateMissingValues(reimportedLatitudeSerie, timeSerie, false);
      tourData.createTimeSeries_20_InterpolateMissingValues(reimportedLongitudeSerie, timeSerie, false);

      // adjust distance/speed values
      TourManager.computeDistanceValuesFromGeoPosition(reimportedTourData);

      tourData.latitudeSerie = reimportedLatitudeSerie;
      tourData.longitudeSerie = reimportedLongitudeSerie;
      tourData.distanceSerie = reimportedTourData.distanceSerie;

      // keep geo position in the marker
      geoMarker.setGeoPosition(geoMarkerLatitude, geoMarkerLongitude);
      geoMarker.setDescription(getGeoMarkerJSON(_currentMouseGeoPosition));

      setTourGPSIntoPhotos(tourData);

      TourManager.saveModifiedTour(tourData);
   }

   /**
    * The import state with
    * <p>
    * <code>new ImportState_Process().setIsSkipGeoInterpolation(true);</code>
    * <p>
    * is creating interpolated values for latitude, cleanup these values that they are interpolated
    * correctly with #geo positions
    *
    * @param reimportedTourData
    */
   private void cleanupGeoPositions(final TourData reimportedTourData) {

      final boolean[] allInterpolatedValueSerie = reimportedTourData.interpolatedValueSerie;

      final double[] latitudeSerie = reimportedTourData.latitudeSerie;
      final double[] longitudeSerie = reimportedTourData.longitudeSerie;

      for (int serieIndex = 0; serieIndex < allInterpolatedValueSerie.length; serieIndex++) {

         final boolean isInterpolated = allInterpolatedValueSerie[serieIndex];

         if (isInterpolated) {

            latitudeSerie[serieIndex] = Double.MIN_VALUE;
            longitudeSerie[serieIndex] = Double.MIN_VALUE;
         }
      }
   }

   private void createOtherGeoPositions(final TourMarker currentGeoMarker,
                                        final TourData tourData,
                                        final double[] reimportedLatitudeSerie,
                                        final double[] reimportedLongitudeSerie) {

      final String geoPrefix = GEO_MARKER_PREFIX.toLowerCase();

      final List<TourMarker> allSortedTourMarkers = tourData.getTourMarkersSorted();
      final int numTourMarkers = allSortedTourMarkers.size();

      for (int markerIndex = 0; markerIndex < numTourMarkers; markerIndex++) {

         final TourMarker tourMarker = allSortedTourMarkers.get(markerIndex);

         final int serieIndex = tourMarker.getSerieIndex();

         // skip current geo marker
         if (tourMarker.equals(currentGeoMarker)) {

            continue;
         }

         final String label = tourMarker.getLabel();

         if (label.trim().toLowerCase().startsWith(geoPrefix)) {

            // this is a geo marker with a #geo prefix

            final String markerDescription = tourMarker.getDescription();

            if (markerDescription != null

                  /*
                   * Ensure that the geo position is set in the description, it is possible that a
                   * geo position it set later
                   */
                  && markerDescription.trim().length() > 0) {

               try {

                  // parse JSON
                  final ObjectMapper mapper = new ObjectMapper();

                  final GeoPosition markerGeoPos = mapper.readValue(markerDescription, GeoPosition.class);

                  if (markerGeoPos != null) {

                     reimportedLatitudeSerie[serieIndex] = markerGeoPos.latitude;
                     reimportedLongitudeSerie[serieIndex] = markerGeoPos.longitude;
                  }

               } catch (final JsonProcessingException e) {

                  StatusUtil.log(e);
               }
            }
         }
      }
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      if (_allGeoMarker == null) {

         // this happened during debugging

         return;
      }

      for (final TourMarker tourMarker : _allGeoMarker) {

         final ActionSetGeoPosition action = new ActionSetGeoPosition(tourMarker);

         new ActionContributionItem(action).fill(menu, -1);
      }

      new Separator().fill(menu, -1);

      new ActionContributionItem(new ActionRemoveAllGeoPositions()).fill(menu, -1);
   }

   private String getGeoMarkerJSON(final GeoPosition geoPosition) {

      try {

         final ObjectMapper mapper = new ObjectMapper();

         final String geoPosJSON = mapper.writeValueAsString(geoPosition);

         if (geoPosJSON.length() >= TourDatabase.VARCHAR_MAX_LENGTH) {

            StatusUtil.logError("Cannot save photoAdjustmentsJSON because it is > %d".formatted(TourDatabase.VARCHAR_MAX_LENGTH)); //$NON-NLS-1$

            return null;
         }

         return geoPosJSON;

      } catch (final JsonProcessingException e) {

         StatusUtil.log(e);
      }

      return null;
   }

   /**
    * These context data are set before the submenu is opened
    *
    * @param allGeoMarker
    * @param currentMouseGeoPosition
    */
   public void setContextData(final List<TourMarker> allGeoMarker, final GeoPosition currentMouseGeoPosition) {

      if (allGeoMarker == null) {

         // this happened during app startup

         return;
      }

      // sort geo marker by name
      Collections.sort(
            allGeoMarker,
            (tourMarker1, tourMarker2) -> tourMarker1.getLabel().compareTo(tourMarker2.getLabel()));

      _allGeoMarker = allGeoMarker;
      _currentMouseGeoPosition = currentMouseGeoPosition;
   }

   /**
    * This is partly copied from
    * {@link TourPhotoManager#setTourGPSIntoPhotos_10(net.tourbook.tour.photo.TourPhotoLink)}
    *
    * @param tourData
    */
   private void setTourGPSIntoPhotos(final TourData tourData) {

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      if (latitudeSerie == null) {
         // no geo positions
         return;
      }

      final Set<TourPhoto> allTourPhotos = tourData.getTourPhotos();

      final int numPhotos = allTourPhotos.size();
      if (numPhotos == 0) {
         // no photos are available for this tour
         return;
      }

      // sort photos by time
      final List<TourPhoto> allSortedPhotos = new ArrayList<>(allTourPhotos);
      Collections.sort(allSortedPhotos, (tourPhoto1, tourPhoto2) -> {
         return Long.compare(tourPhoto1.getImageExifTime(), tourPhoto2.getImageExifTime());
      });

      final int[] timeSerie = tourData.timeSerie;
      final int numTimeSlices = timeSerie.length;

      final long tourStartSec = tourData.getTourStartTime().toInstant().getEpochSecond();

      long timeSliceEndSec;

      if (numTimeSlices > 1) {
         timeSliceEndSec = tourStartSec + (long) (timeSerie[1] / 2.0);
      } else {
         // tour contains only 1 time slice
         timeSliceEndSec = tourStartSec;
      }

      int timeIndex = 0;
      int photoIndex = 0;

      // get first photo
      TourPhoto tourPhoto = allSortedPhotos.get(photoIndex);

      // loop: time serie
      while (true) {

         // loop: photo serie, check if a photo is in the current time slice
         while (true) {

            final long imageAdjustedTime = tourPhoto.getAdjustedTime();
            long imageTime = 0;

            if (imageAdjustedTime != Long.MIN_VALUE) {
               imageTime = imageAdjustedTime;
            } else {
               imageTime = tourPhoto.getImageExifTime();
            }

            final long photoTimeSec = imageTime / 1000;

            if (photoTimeSec <= timeSliceEndSec) {

               // photo is contained within the current time slice

               final double tourLatitude = latitudeSerie[timeIndex];
               final double tourLongitude = longitudeSerie[timeIndex];

               tourPhoto.setGeoLocation(tourLatitude, tourLongitude);

               photoIndex++;

            } else {

               // advance to the next time slice

               break;
            }

            if (photoIndex < numPhotos) {
               tourPhoto = allSortedPhotos.get(photoIndex);
            } else {
               break;
            }
         }

         if (photoIndex >= numPhotos) {
            // no more photos
            break;
         }

         /*
          * Photos are still available
          */

         // advance to the next time slice on the x-axis
         timeIndex++;

         if (timeIndex >= numTimeSlices - 1) {

            /*
             * end of tour is reached but there are still photos available, set remaining photos
             * at the end of the tour
             */

            while (true) {

               final double tourLatitude = latitudeSerie[timeIndex];
               final double tourLongitude = longitudeSerie[timeIndex];

               tourPhoto.setGeoLocation(tourLatitude, tourLongitude);

               photoIndex++;

               if (photoIndex < numPhotos) {
                  tourPhoto = allSortedPhotos.get(photoIndex);
               } else {
                  break;
               }
            }

         } else {

            final long valuePointTime = timeSerie[timeIndex];
            final long sliceDuration = timeSerie[timeIndex + 1] - valuePointTime;

            timeSliceEndSec = tourStartSec + valuePointTime + (sliceDuration / 2);
         }
      }
   }
}
