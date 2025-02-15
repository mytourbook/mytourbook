/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.photo;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.data.HistoryData;
import net.tourbook.data.TourData;
import net.tourbook.photo.Photo;

import org.joda.time.Period;
import org.joda.time.PeriodType;

public class TourPhotoLink {

   private static final char       NL                  = UI.NEW_LINE;

   private static final PeriodType _tourPeriodTemplate = PeriodType.yearMonthDayTime()

         // hide these components
         .withMinutesRemoved()
         .withSecondsRemoved()
         .withMillisRemoved();

   boolean                         isHistoryTour;

   /**
    * Contains tour id when it's a real tour, otherwise it contains {@link Long#MIN_VALUE}.
    */
   public long                     tourId              = Long.MIN_VALUE;

   long                            tourTypeId          = -1;

   /**
    * Unique id for this link.
    */
   long                            linkId;

   /**
    * Tour start time in ms
    */
   long                            tourStartTime;

   /**
    * Tour end time in ms.
    */
   long                            tourEndTime         = Long.MIN_VALUE;

   long                            historyStartTime    = Long.MIN_VALUE;
   long                            historyEndTime      = Long.MIN_VALUE;

   /**
    * Tour start date/time with the default time zone
    */
   private ZonedDateTime           _tourStartDateTime_DefaultZone;
   private ZonedDateTime           _tourStartDateTime_WithZoneID;

   private ZonedDateTime           _tourEndDateTime_DefaultZone;
   private ZonedDateTime           _tourEndDateTime_WithZoneID;

   Period                          tourPeriod;

   int                             numGPSPhotos;
   int                             numNoGPSPhotos;

   /**
    * Number of photos which are saved in a real tour.
    */
   int                             numTourPhotos;

   /**
    * Adjusted time in seconds which is saved in the tour
    */
   int                             photoTimeAdjustment;

   /**
    * Contains all photos for this tour
    */
   public ArrayList<Photo>         linkPhotos          = new ArrayList<>();

   private TourData                _historyTourData;

   /**
    * Contains names for all cameras which are used to take pictures for the current tour
    */
   String                          tourCameras         = UI.EMPTY_STRING;

   /**
    * Photo file path from the first photo, other photos are currently ignored -> is simplified
    */
   String                          photoFilePath;

   /**
    * Constructor for a history tour.
    *
    * @param notUsed
    */
   TourPhotoLink(final long tourStartTime) {

      isHistoryTour = true;

      linkId = System.nanoTime();

      setTourStartTime(tourStartTime, null);

      _historyTourData = new TourData();
      _historyTourData.setupHistoryTour();
   }

   /**
    * Constructor for a real tour.
    *
    * @param tourEndTime
    * @param tourStartTime
    * @param tourId
    * @param timeZoneID
    * @param dbPhotoTimeAdjustment
    * @param dbNumberOfPhotos
    */
   TourPhotoLink(final long tourId,
                 final long tourStartTime,
                 final long tourEndTime,
                 final Object timeZoneID,
                 final int numPhotos,
                 final int dbPhotoTimeAdjustment) {

      this.tourId = tourId;

      linkId = tourId;

      setTourStartTime(tourStartTime, timeZoneID);
      setTourEndTime(tourEndTime, timeZoneID);

      numTourPhotos = numPhotos;

      tourPeriod = new Period(tourStartTime, tourEndTime, _tourPeriodTemplate);

      photoTimeAdjustment = dbPhotoTimeAdjustment;
   }

   private void addTimeSlice(final ArrayList<HistoryData> historyList, final long timeSliceTime) {

      final HistoryData historyData = new HistoryData();

      historyData.absoluteTime = timeSliceTime / 1000 * 1000;

      historyList.add(historyData);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TourPhotoLink)) {
         return false;
      }
      final TourPhotoLink other = (TourPhotoLink) obj;
      if (linkId != other.linkId) {
         return false;
      }
      return true;
   }

   private void finalizeHistoryTour() {

      /*
       * create time data serie
       */
      final int timeSerieLength = linkPhotos.size();
      final long[] historyTimeSerie = new long[timeSerieLength];

      for (int photoIndex = 0; photoIndex < timeSerieLength; photoIndex++) {
         historyTimeSerie[photoIndex] = linkPhotos.get(photoIndex).adjustedTime_Camera;
      }

      final long tourStart = historyTimeSerie[0];
      final long tourEnd = historyTimeSerie[timeSerieLength - 1];

      historyStartTime = tourStart;
      historyEndTime = tourEnd;

      if (timeSerieLength == 1) {

         // only 1 point is visible

         tourStartTime = tourStart - 1000;
         tourEndTime = tourStart + 1000;

      } else {

         // add additional 3% tour time that the tour do not start/end at the chart border

         final double timeDiff = tourEnd - tourStart;

         /**
          * very important: round to 0 ms
          */
         long timeOffset = ((long) (timeDiff * 0.03) / 1000) * 1000;

         // ensure there is a time difference of 1 second
         if (timeOffset == 0) {
            timeOffset = 1000;
         }

         tourStartTime = tourStart - timeOffset;
         tourEndTime = tourEnd + timeOffset;
      }

      // update adjusted start
      _tourStartDateTime_DefaultZone = TimeTools.getZonedDateTime(tourStartTime);
      _tourEndDateTime_DefaultZone = TimeTools.getZonedDateTime(tourEndTime);

      /*
       * adjust start and end that the dummy tour do not start at the chart border
       */

      final ArrayList<HistoryData> historySlices = new ArrayList<>();

      /*
       * Set tour start time line before first time slice
       */
      addTimeSlice(historySlices, tourStartTime);

      /*
       * Create time data list for all time slices which contains photos
       */
      long prevTimeSliceTime = Long.MIN_VALUE;
      for (final long timeSliceTime : historyTimeSerie) {

         // skip duplicates
         if (timeSliceTime == prevTimeSliceTime) {
            continue;
         }

         addTimeSlice(historySlices, timeSliceTime);

         prevTimeSliceTime = timeSliceTime;
      }

      /*
       * Set tour end time after the last time slice
       */
      addTimeSlice(historySlices, tourEndTime);

      _historyTourData.setTourStartTime(_tourStartDateTime_DefaultZone);
      _historyTourData.createHistoryTimeSerie(historySlices);
   }

   public TourData getHistoryTourData() {
      return _historyTourData;
   }

   public ZonedDateTime getTourEndDateTime_DefaultZone() {
      return _tourEndDateTime_DefaultZone;
   }

   public ZonedDateTime getTourEndDateTime_WithZoneID() {
      return _tourEndDateTime_WithZoneID;
   }

   /**
    * @return Can be <code>null</code> when zone ID is not set
    */
   public ZonedDateTime getTourStartDateTime_WithZoneID() {

      return _tourStartDateTime_WithZoneID;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (linkId ^ (linkId >>> 32));
      return result;
   }

   public boolean isHistoryTour() {
      return isHistoryTour;
   }

   void setTourEndTime(final long endTime, final Object timeZoneID) {

      if (isHistoryTour) {

         final int photosSize = linkPhotos.size();

         if (photosSize == 0) {

            // there are no photos in this history tour, THIS SHOULD NOT HAPPEN FOR A HISTORY TOUR
            tourEndTime = tourStartTime;

         } else {

            // get time from last photo
            tourEndTime = linkPhotos.get(photosSize - 1).adjustedTime_Camera;
         }

         finalizeHistoryTour();

      } else {

         tourEndTime = endTime;

         _tourEndDateTime_DefaultZone = TimeTools.getZonedDateTime(tourEndTime);

         if (timeZoneID instanceof final String rawTimeZoneID) {

            final ZoneId zoneId = ZoneId.of(rawTimeZoneID);

            _tourEndDateTime_WithZoneID = TimeTools.getZonedDateTime(tourEndTime, zoneId);
         }
      }

      // set tour period AFTER history tour is finalized
      tourPeriod = new Period(tourStartTime, tourEndTime, _tourPeriodTemplate);
   }

   private void setTourStartTime(final long time, final Object timeZoneID) {

      // remove milliseconds
      tourStartTime = time / 1000 * 1000;

      _tourStartDateTime_DefaultZone = TimeTools.getZonedDateTime(tourStartTime);

      if (timeZoneID instanceof final String rawTimeZoneID) {

         final ZoneId zoneId = ZoneId.of(rawTimeZoneID);

         _tourStartDateTime_WithZoneID = TimeTools.getZonedDateTime(tourStartTime, zoneId);
      }
   }

   @Override
   public String toString() {

      final String tourStart = TimeTools.getZonedDateTime(tourStartTime).format(TimeTools.Formatter_DateTime_ML);

      final String tourStartZoned = _tourStartDateTime_WithZoneID == null
            ? UI.EMPTY_STRING
            : _tourStartDateTime_WithZoneID.format(TimeTools.Formatter_DateTime_ML);

      final String historyStart = historyStartTime == Long.MIN_VALUE
            ? UI.EMPTY_STRING
            : TimeTools.getZonedDateTime(historyStartTime).format(TimeTools.Formatter_DateTime_ML);

      return UI.EMPTY_STRING

            + "TourPhotoLink" + NL //                             //$NON-NLS-1$
            + " isHistory        =" + isHistoryTour + NL //       //$NON-NLS-1$
            + " tourStart        =" + tourStart + NL //           //$NON-NLS-1$
            + " tourStartZoned   =" + tourStartZoned + NL //      //$NON-NLS-1$
            + " historyStartTime =" + historyStart + NL //        //$NON-NLS-1$
            + " linkId           =" + linkId + NL //              //$NON-NLS-1$
      ;
   }

}
