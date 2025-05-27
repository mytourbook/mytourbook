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
package net.tourbook.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourPhoto;
import net.tourbook.tour.TourManager;

/**
 *
 */
public class TourDataUpdate_057_to_058 implements ITourDataUpdate {

   private static final char NL = UI.NEW_LINE //
   ;

   @Override
   public int getDatabaseVersion() {

      return 58;
   }

   /**
    * Returns only tour id's which are photo tours
    */
   @Override
   public List<Long> getTourIDs() {

      final List<Long> allTourIds = new ArrayList<>();

      PreparedStatement stmt = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final String sql = UI.EMPTY_STRING

               + "SELECT tourId" + NL //                                                           //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                                   //$NON-NLS-1$
               + " WHERE devicePluginId = '" + TourData.DEVICE_ID_FOR_PHOTO_TOUR + "'" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
               + " ORDER BY TourStartTime" + NL //                                                 //$NON-NLS-1$
         ;

         stmt = conn.prepareStatement(sql);

         final ResultSet result = stmt.executeQuery();

         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      } finally {
         Util.closeSql(stmt);
      }

      return allTourIds;
   }

   /**
    * When a "photo tour" was introduced in 25.3, it contained 3 time slices which could have the
    * same positions or different. The photos where distributed evenly over all time slices.
    * <p>
    * This feature was replaced in 25.? (after 25.4) by creating a photo tour where each photo
    * had its own time slice which geo position could be moved with the mouse.
    * <p>
    * This tour update will convert the 3 time slices into n time slices for each photo. Each photo
    * will have a slightly different geo position that it can be relocated with the mouse.
    */
   @Override
   public boolean updateTourData(final TourData tourData) {

      if (tourData.isPhotoTour() == false) {
         return false;
      }

      final Set<TourPhoto> allTourPhotos = tourData.getTourPhotos();

      if (allTourPhotos.size() == 0) {
         return false;
      }

      // at least one photo is available

      final int[] timeSerie = tourData.timeSerie;
      final int numOriginalTimeSlices = timeSerie.length;
      final int lastOriginalTimeSliceIndex = numOriginalTimeSlices - 1;

      if (numOriginalTimeSlices != 3) {

         StatusUtil.log("Photo tour '%s' do not contain 3 time slices".formatted(TourManager.getTourTitle(tourData)), new Exception()); //$NON-NLS-1$
      }

      final List<TourPhoto> allSortedPhotos = new ArrayList<>(allTourPhotos);

      // sort photos by time
      Collections.sort(allSortedPhotos, (tourPhoto1, tourPhoto2) -> {

         return Long.compare(tourPhoto1.getImageExifTime(), tourPhoto2.getImageExifTime());
      });

      final int numPhotos = allSortedPhotos.size();
      final int lastPhotoIndex = numPhotos - 1;

      final ZoneId timeZoneIdWithDefault = tourData.getTimeZoneIdWithDefault();

      final long tourStartTimeMS = allSortedPhotos.get(0).getAdjustedTime();
      final long tourEndTimeMS = allSortedPhotos.get(lastPhotoIndex).getAdjustedTime();

      final Instant tourStartInstant = Instant.ofEpochMilli(tourStartTimeMS);
      final Instant tourEndInstant = Instant.ofEpochMilli(tourEndTimeMS);

      final ZonedDateTime zonedStartTime = ZonedDateTime.ofInstant(tourStartInstant, timeZoneIdWithDefault);
      final ZonedDateTime zonedEndTime = ZonedDateTime.ofInstant(tourEndInstant, timeZoneIdWithDefault);

      final long elapsedTimeSeconds = ChronoUnit.SECONDS.between(zonedStartTime, zonedEndTime);

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      final double latFirst = latitudeSerie[0];
      final double lonFirst = longitudeSerie[0];
      double latLast = latitudeSerie[lastOriginalTimeSliceIndex];
      double lonLast = longitudeSerie[lastOriginalTimeSliceIndex];

      final double latFirstNorm = latFirst + TourData.NORMALIZED_LATITUDE_OFFSET;
      final double latLastNorm = latLast + TourData.NORMALIZED_LATITUDE_OFFSET;
      final double lonFirstNorm = lonFirst + TourData.NORMALIZED_LONGITUDE_OFFSET;
      final double lonLastNorm = lonLast + TourData.NORMALIZED_LONGITUDE_OFFSET;

      final double latDiffTour = Math.abs(latFirstNorm - latLastNorm);
      final double lonDiffTour = Math.abs(lonFirstNorm - lonLastNorm);

      double latDiffSlice = latDiffTour / numPhotos;
      double lonDiffSlice = lonDiffTour / numPhotos;

      final double minDiff = 0.00001;

      if (latDiffSlice < minDiff) {
         latDiffSlice = minDiff;
      }

      if (lonDiffSlice < minDiff) {
         lonDiffSlice = minDiff;
      }

      final double minLatDiffTour = latDiffSlice * numPhotos;
      final double minLonDiffTour = lonDiffSlice * numPhotos;

      // ensure that there is a min lat/lon difference between each photo
      if (latDiffTour < minLatDiffTour) {
         latLast = latFirst + minLatDiffTour;
      }

      if (lonDiffTour < minLonDiffTour) {
         lonLast = lonFirst + minLonDiffTour;
      }

      final int[] newTimeSerie = new int[numPhotos];
      final double[] newLatSerie = new double[numPhotos];
      final double[] newLonSerie = new double[numPhotos];

      int relativeTimeMS;
      TourPhoto tourPhoto;
      final Set<Long> allTourPhotosWithPositionedGeo = tourData.getTourPhotosWithPositionedGeo();

      /*
       * Set values for the first time slice
       */
      if (numPhotos > 0) {

         newTimeSerie[0] = 0;
         newLatSerie[0] = latFirst;
         newLonSerie[0] = lonFirst;

         tourPhoto = allSortedPhotos.get(0);
         tourPhoto.setGeoLocation(latFirst, lonFirst);

         // keep state for which photo a geo position was set
         allTourPhotosWithPositionedGeo.add(tourPhoto.getPhotoId());
      }

      /*
       * Set values between first and last time slice
       */
      for (int serieIndex = 1; serieIndex < lastPhotoIndex; serieIndex++) {

         tourPhoto = allSortedPhotos.get(serieIndex);

         final long photoTimeMS = tourPhoto.getAdjustedTime();
         relativeTimeMS = (int) (photoTimeMS - tourStartTimeMS);

         newTimeSerie[serieIndex] = relativeTimeMS / 1000;

         /*
          * Lat/lon values are computed in "tourData.computeGeo_Photos();"
          */
      }

      /*
       * Set values for the last time slice
       */
      if (numPhotos > 1) {

         tourPhoto = allSortedPhotos.get(lastPhotoIndex);

         final long photoTimeMS = tourPhoto.getAdjustedTime();
         relativeTimeMS = (int) (photoTimeMS - tourStartTimeMS);

         newTimeSerie[lastPhotoIndex] = relativeTimeMS / 1000;
         newLatSerie[lastPhotoIndex] = latLast;
         newLonSerie[lastPhotoIndex] = lonLast;

         tourPhoto = allSortedPhotos.get(lastPhotoIndex);
         tourPhoto.setGeoLocation(latLast, lonLast);

         // keep state for which photo a geo position was set
         allTourPhotosWithPositionedGeo.add(tourPhoto.getPhotoId());
      }

      /*
       * Update tour data
       */
      tourData.setTourStartTime(zonedStartTime);
      tourData.setTourDeviceTime_Elapsed(elapsedTimeSeconds);

      tourData.timeSerie = newTimeSerie;
      tourData.latitudeSerie = newLatSerie;
      tourData.longitudeSerie = newLonSerie;

      // interpolate geo positions
      tourData.computeGeo_Photos();

      // update modified serie data
      tourData.onPrePersist();

      return true;
   }

}
