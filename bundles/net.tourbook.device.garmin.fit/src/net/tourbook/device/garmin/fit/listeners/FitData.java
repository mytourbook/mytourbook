/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin.fit.listeners;

import com.garmin.fit.DateTime;
import com.garmin.fit.EventMesg;
import com.garmin.fit.HrMesg;
import com.garmin.fit.LengthMesg;
import com.garmin.fit.LengthType;
import com.garmin.fit.SwimStroke;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.data.GearData;
import net.tourbook.data.SwimData;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.device.garmin.fit.FitDataReader2;
import net.tourbook.tour.TourLogManager;
import net.tourbook.ui.tourChart.ChartLabel;

/**
 * Collects all data from a fit file
 */
public class FitData {

   private FitDataReader2          fitDataReader2;
   private String                  importFilePath;

   private HashMap<Long, TourData> alreadyImportedTours;
   private HashMap<Long, TourData> newlyImportedTours;

   TourData                        tourData = new TourData();

   // ========== FitContext ==========

   private boolean                 _isHeartRateSensorPresent;
   private boolean                 _isPowerSensorPresent;
   private boolean                 _isSpeedSensorPresent;
   private boolean                 _isStrideSensorPresent;

   private String                  _deviceId;
   private String                  _manufacturer;
   private String                  _garminProduct;
   private String                  _softwareVersion;

   private String                  _sessionIndex;
   private ZonedDateTime           _sessionTime;

   private float                   _lapDistance;
   private int                     _lapTime;

   private Map<Long, TourData>     _alreadyImportedTours;
   private HashMap<Long, TourData> _newlyImportedTours;

   private boolean                 _isIgnoreLastMarker;
   private boolean                 _isSetLastMarker;
   private int                     _lastMarkerTimeSlices;

   // ========== FitContextData ==========

   private final List<GearData>   _allGearData   = new ArrayList<>();
   private final List<SwimData>   _allSwimData   = new ArrayList<>();
   private final List<TimeData>   _allTimeData   = new ArrayList<>();
   private final List<TourMarker> _allTourMarker = new ArrayList<>();

   private TimeData               _current_TimeData;
   private TimeData               _previous_TimeData;

   private List<TimeData>         _current_AllTimeData;
   private List<TimeData>         _previous_AllTimeData;

   private TourMarker             _current_TourMarker;

   private long                   _timeDiffMS;

   public FitData(final FitDataReader2 fitDataReader2,
                  final String importFilePath,
                  final HashMap<Long, TourData> alreadyImportedTours,
                  final HashMap<Long, TourData> newlyImportedTours) {

      this.fitDataReader2 = fitDataReader2;
      this.importFilePath = importFilePath;
      this.alreadyImportedTours = alreadyImportedTours;
      this.newlyImportedTours = newlyImportedTours;

   }

   /**
    * Gear data are available in the common {@link EventMesg}.
    *
    * @param mesg
    */
   public void onMesg_Event(final EventMesg mesg) {

      // ensure a tour is setup
      setupSession_Tour_10_Initialize();

      final Long gearChangeData = mesg.getGearChangeData();

      // check if gear data are available, it can be null
      if (gearChangeData != null) {

         // get gear list for current tour
         List<GearData> tourGears = _allGearData.get(_current_TourContext);

         if (tourGears == null) {
            tourGears = new ArrayList<>();
            _allGearData.put(_current_TourContext, tourGears);
         }

         // create gear data for the current time
         final GearData gearData = new GearData();

         final com.garmin.fit.DateTime garminTime = mesg.getTimestamp();

         // convert garmin time into java time
         final long garminTimeS = garminTime.getTimestamp();
         final long garminTimeMS = garminTimeS * 1000;
         final long javaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

         gearData.absoluteTime = javaTime;
         gearData.gears = gearChangeData;

         tourGears.add(gearData);
      }
   }

   /**
    * This is called AFTER the session is closed. Swimming is producing the hr event separately in
    * it's own device.
    *
    * @param mesg
    */
   public void onMesg_Hr(final HrMesg mesg) {

      // ensure tour is setup
      setupSession_Tour_10_Initialize();

      final DateTime hrTime = mesg.getTimestamp();

//    System.out.println(String.format(""
//
//          + "[%s]"
//
//          + " NumEventTimestamp %-3d"
//          + " NumFilteredBpm %-3d"
//
//          + " Timestamp %-29s"
//          + " timestamp %-15s"
//          + " FractionalTimestamp: %-7.5f"
////           + (" Time256: " + mesg.getTime256())
//
//          + " EventTimestamp %-90s "
//          + " FilteredBpm %-45s "
//
//          + (" NumEventTimestamp12 %-5d")
//          + (" EventTimestamp12 " + Arrays.toString(mesg.getEventTimestamp12())),
//
//          getClass().getSimpleName(),
//
//          mesg.getNumEventTimestamp(),
//          mesg.getNumFilteredBpm(),
//
//          hrTime,
//          hrTime == null ? "" : hrTime.getTimestamp(),
//          mesg.getFractionalTimestamp() == null ? null : mesg.getFractionalTimestamp(),
//
//          Arrays.toString(mesg.getEventTimestamp()),
//          Arrays.toString(mesg.getFilteredBpm()),
//
//          mesg.getNumEventTimestamp12())
////TODO remove SYSTEM.OUT.PRINTLN
//    );

      boolean isTimeAvailable = false;

      final int numEventTimestamp = mesg.getNumEventTimestamp();
      if (numEventTimestamp < 1) {
         return;
      }

      final Float[] allEventTime = mesg.getEventTimestamp();
      final Short[] allFilteredBpm = mesg.getFilteredBpm();

      if (allFilteredBpm.length != allEventTime.length) {

         TourLogManager.logError(String.format("Fit file has different filtered data: EventTimestamp: %d - FilteredBpm: %d", //$NON-NLS-1$
               allEventTime.length,
               allFilteredBpm.length));

         return;
      }

      /*
       * Get time diff between tour and hr recording. It is complicated because it also contains the
       * garmin time offset. After several time and error processes this algorithm seems to be now
       * correct.
       */
      if (hrTime != null && _timeDiffMS == Long.MIN_VALUE && allEventTime.length > 0) {

         final long firstTourTimeMS = _previous_AllTimeData.get(0).absoluteTime;
         final long firstHrTimestampMS = hrTime.getDate().getTime();

         final long hr2TourTimeDiffMS = firstTourTimeMS - firstHrTimestampMS;

         final long firstHrTimeSec = Float.valueOf(allEventTime[0]).longValue();
         final long firstHrTimeMS = firstHrTimeSec * 1000;

         _timeDiffMS = firstTourTimeMS - firstHrTimeMS - hr2TourTimeDiffMS;
      }

      for (int timeStampIndex = 0; timeStampIndex < allEventTime.length; timeStampIndex++) {

         final Float eventTimeStamp = allEventTime[timeStampIndex];
         final Short filteredBpm = allFilteredBpm[timeStampIndex];

         final double sliceGarminTimeS = eventTimeStamp;
         final long sliceGarminTimeMS = (long) (sliceGarminTimeS * 1000);
         final long sliceJavaTime = sliceGarminTimeMS + _timeDiffMS;

         // merge HR data into an already existing time data
         for (final TimeData timeData : _previous_AllTimeData) {

            if (timeData.absoluteTime == sliceJavaTime) {

               timeData.pulse = filteredBpm;
               isTimeAvailable = true;

//             System.out.println(String.format(""
//
//                   + "[%s]"
//
//                   + (" eventTimeStamp %-8.2f   ")
//                   + (" sliceJavaTime %d   ")
//                   + (" localDT %s   ")
//                   + (" bpm %d"),
//
//                   getClass().getSimpleName(),
//
//                   eventTimeStamp,
//                   sliceJavaTime,
//                   new LocalDateTime(sliceJavaTime),
//                   filteredBpm
//
//// TODO remove SYSTEM.OUT.PRINTLN
//             ));

               break;
            }
         }

         if (isTimeAvailable == false) {

            // timeslice is not yet created for this heartrate

//          System.out.println(String.format(""
//
//                + "[%s]"
//
//                + (" eventTimeStamp %-8.2f   ")
//                + (" sliceJavaTime %d   ")
//                + (" localDT %s   ")
//                + (" bpm %d - no timeslice"),
//
//                getClass().getSimpleName(),
//
//                eventTimeStamp,
//                sliceJavaTime,
//                new LocalDateTime(sliceJavaTime),
//                filteredBpm
//
//// TODO remove SYSTEM.OUT.PRINTLN
//          ));
         }
      }
   }

   public void onMesg_Length(final LengthMesg mesg) {

      // ensure tour is setup
//    setupSession_Tour_10_Initialize();

      // get gear list for current tour
      List<SwimData> tourSwimData = _allSwimData.get(_current_TourContext);

      if (tourSwimData == null) {
         tourSwimData = new ArrayList<>();
         _allSwimData.put(_current_TourContext, tourSwimData);
      }

      // create gear data for the current time
      final SwimData swimData = new SwimData();

      tourSwimData.add(swimData);

      final com.garmin.fit.DateTime garminTime = mesg.getTimestamp();

      // convert garmin time into java time
      final long garminTimeS = garminTime.getTimestamp();
      final long garminTimeMS = garminTimeS * 1000;
      final long javaTime = garminTimeMS + com.garmin.fit.DateTime.OFFSET;

      final Short avgSwimmingCadence = mesg.getAvgSwimmingCadence();
      final LengthType lengthType = mesg.getLengthType();
      final SwimStroke swimStrokeStyle = mesg.getSwimStroke();
      final Integer numStrokes = mesg.getTotalStrokes();

      swimData.absoluteTime = javaTime;

      if (lengthType != null) {
         swimData.swim_LengthType = lengthType.getValue();
      }

      if (avgSwimmingCadence != null) {
         swimData.swim_Cadence = avgSwimmingCadence;
      }

      if (numStrokes != null) {
         swimData.swim_Strokes = numStrokes.shortValue();
      }

      if (swimStrokeStyle != null) {
         swimData.swim_StrokeStyle = swimStrokeStyle.getValue();
      }

//    final long timestamp = mesg.getTimestamp().getDate().getTime();
//
//    System.out.println(String.format(""
//
//          + "[%s]"
//
//          + " Timestamp %-23s"
////           + " StartTime %-23s"
////           + " Time Diff %-6d"
//
//          + " LengthType %-10s"
//
//          + " SwimStroke %-15s"
//          + " AvgSwimmingCadence %-6s"
//          + " TotalStrokes %-5s"
//
////           + " NumStrokeCount %-3d"
////           + " StrokeCount %-30s"
//
//          ,
//
//          getClass().getSimpleName(),
//
//          TimeTools.toLocalDateTime(timestamp),
////           TimeTools.toLocalDateTime(mesg.getStartTime().getDate().getTime()),
////           mesg.getTimestamp().getTimestamp() - mesg.getStartTime().getTimestamp(),
//
//          lengthType,
//
//          swimStrokeStyle == null ? "" : swimStrokeStyle.toString(),
//          avgSwimmingCadence == null ? "" : avgSwimmingCadence.toString(),
//          numStrokes == null ? "" : numStrokes.toString()
//
//    ));
////TODO remove SYSTEM.OUT.PRINTLN

//    [FitContextData] Timestamp 2018-09-01T14:51:01     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 12
//    [FitContextData] Timestamp 2018-09-01T14:51:26     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 23     TotalStrokes 11
//    [FitContextData] Timestamp 2018-09-01T14:52        LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 25     TotalStrokes 13
//    [FitContextData] Timestamp 2018-09-01T14:52:30     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 12
//    [FitContextData] Timestamp 2018-09-01T14:53        LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 12
//    [FitContextData] Timestamp 2018-09-01T14:53:19     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 24     TotalStrokes 11
//    [FitContextData] Timestamp 2018-09-01T14:54:34     LengthType IDLE       SwimStroke                 AvgSwimmingCadence        TotalStrokes
//    [FitContextData] Timestamp 2018-09-01T14:55:25     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 17     TotalStrokes 12
//    [FitContextData] Timestamp 2018-09-01T14:56:09     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 19     TotalStrokes 13
//    [FitContextData] Timestamp 2018-09-01T14:56:50     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 19     TotalStrokes 13
//    [FitContextData] Timestamp 2018-09-01T14:57:19     LengthType ACTIVE     SwimStroke BREASTSTROKE    AvgSwimmingCadence 16     TotalStrokes 11
//    [FitContextData] Timestamp 2018-09-01T14:59:05     LengthType IDLE       SwimStroke                 AvgSwimmingCadence        TotalStrokes
//    [FitContextData] Timestamp 2018-09-01T14:59:42     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 23     TotalStrokes 12
//    [FitContextData] Timestamp 2018-09-01T15:00:10     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 26     TotalStrokes 12
//    [FitContextData] Timestamp 2018-09-01T15:00:38     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 25     TotalStrokes 12
//    [FitContextData] Timestamp 2018-09-01T15:01:09     LengthType ACTIVE     SwimStroke FREESTYLE       AvgSwimmingCadence 26     TotalStrokes 12

   }

   public void setupLap_Marker_10_Initialize() {

      setupSession_Tour_10_Initialize();

      List<TourMarker> tourMarkers = _allTourMarker.get(_current_TourContext);

      if (tourMarkers == null) {

         tourMarkers = new ArrayList<>();

         _allTourMarker.put(_current_TourContext, tourMarkers);
      }

      _current_TourMarker = new TourMarker(getCurrent_TourData(), ChartLabel.MARKER_TYPE_DEVICE);
      tourMarkers.add(_current_TourMarker);
   }

   public void setupLap_Marker_20_Finalize() {

      _current_TourMarker = null;
   }

   public void setupRecord_10_Initialize() {

      // ensure tour is setup
      setupSession_Tour_10_Initialize();

      if (_current_AllTimeData == null) {

         _current_AllTimeData = new ArrayList<>();

         _allTimeData.put(_current_TourContext, _current_AllTimeData);
      }

      _current_TimeData = new TimeData();
   }

   public void setupRecord_20_Finalize() {

      if (_current_TimeData == null) {
         // this occured
         return;
      }

      boolean useThisTimeSlice = true;

      if (_previous_TimeData != null) {

         final long prevTime = _previous_TimeData.absoluteTime;
         final long currentTime = _current_TimeData.absoluteTime;

         if (prevTime == currentTime) {

            /*
             * Ignore and merge duplicated records. The device Bryton 210 creates duplicated enries,
             * to have valid data for this device, they must be merged.
             */

            useThisTimeSlice = false;

            if (_previous_TimeData.absoluteAltitude == Float.MIN_VALUE) {
               _previous_TimeData.absoluteAltitude = _current_TimeData.absoluteAltitude;
            }

            if (_previous_TimeData.absoluteDistance == Float.MIN_VALUE) {
               _previous_TimeData.absoluteDistance = _current_TimeData.absoluteDistance;
            }

            if (_previous_TimeData.cadence == Float.MIN_VALUE) {
               _previous_TimeData.cadence = _current_TimeData.cadence;
            }

            if (_previous_TimeData.latitude == Double.MIN_VALUE) {
               _previous_TimeData.latitude = _current_TimeData.latitude;
            }

            if (_previous_TimeData.longitude == Double.MIN_VALUE) {
               _previous_TimeData.longitude = _current_TimeData.longitude;
            }

            if (_previous_TimeData.power == Float.MIN_VALUE) {
               _previous_TimeData.power = _current_TimeData.power;
            }

            if (_previous_TimeData.pulse == Float.MIN_VALUE) {
               _previous_TimeData.pulse = _current_TimeData.pulse;
            }

            if (_previous_TimeData.speed == Float.MIN_VALUE) {
               _previous_TimeData.speed = _current_TimeData.speed;
            }

            if (_previous_TimeData.temperature == Float.MIN_VALUE) {
               _previous_TimeData.temperature = _current_TimeData.temperature;
            }
         }
      }

      if (useThisTimeSlice) {
         _current_AllTimeData.add(_current_TimeData);
      }

      _previous_TimeData = _current_TimeData;
      _current_TimeData = null;
   }

   public void setupSession_Tour_10_Initialize() {

      if (_current_TourContext == null) {

         final TourData currentTourData = new TourData();

         _current_TourContext = new TourContext(currentTourData);

         _allTourContext.add(_current_TourContext);
      }
   }

   public void setupSession_Tour_20_Finalize() {

      setupRecord_20_Finalize();

      _previous_AllTimeData = _current_AllTimeData;
      _timeDiffMS = Long.MIN_VALUE;

      _current_TourContext = null;
      _current_AllTimeData = null;
   }
}
