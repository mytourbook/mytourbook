/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.device.garmin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.TourbookDevice;
import net.tourbook.tour.TourLogManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GarminTCX_SAXHandler extends DefaultHandler {

   private static final String    TRAINING_CENTER_DATABASE_V1 = "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1"; //$NON-NLS-1$
   private static final String    TRAINING_CENTER_DATABASE_V2 = "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"; //$NON-NLS-1$

   private static final String    TAG_DATABASE                = "TrainingCenterDatabase";                                     //$NON-NLS-1$

   private static final String    TAG_ACTIVITY                = "Activity";                                                   //$NON-NLS-1$
   private static final String    TAG_COURSE                  = "Course";                                                     //$NON-NLS-1$
   private static final String    TAG_HISTORY                 = "History";                                                    //$NON-NLS-1$

   private static final String    TAG_CREATOR                 = "Creator";                                                    //$NON-NLS-1$
   private static final String    TAG_CREATOR_VERSION_MAJOR   = "VersionMajor";                                               //$NON-NLS-1$
   private static final String    TAG_CREATOR_VERSION_MINOR   = "VersionMinor";                                               //$NON-NLS-1$

   private static final String    TAG_NAME                    = "Name";                                                       //$NON-NLS-1$

   private static final String    TAG_ALTITUDE_METERS         = "AltitudeMeters";                                             //$NON-NLS-1$
   private static final String    TAG_CALORIES                = "Calories";                                                   //$NON-NLS-1$
   private static final String    TAG_CADENCE                 = "Cadence";                                                    //$NON-NLS-1$
   private static final String    TAG_DISTANCE_METERS         = "DistanceMeters";                                             //$NON-NLS-1$
   private static final String    TAG_HEART_RATE_BPM          = "HeartRateBpm";                                               //$NON-NLS-1$
   private static final String    TAG_LAP                     = "Lap";                                                        //$NON-NLS-1$
   private static final String    TAG_LATITUDE_DEGREES        = "LatitudeDegrees";                                            //$NON-NLS-1$
   private static final String    TAG_LONGITUDE_DEGREES       = "LongitudeDegrees";                                           //$NON-NLS-1$
   private static final String    TAG_NOTES                   = "Notes";                                                      //$NON-NLS-1$
   private static final String    TAG_RUN_CADENCE             = "RunCadence";                                                 //$NON-NLS-1$
   private static final String    TAG_SENSOR_STATE            = "SensorState";                                                //$NON-NLS-1$
   private static final String    TAG_TRACK                   = "Track";                                                      //$NON-NLS-1$
   private static final String    TAG_TRACKPOINT              = "Trackpoint";                                                 //$NON-NLS-1$
   private static final String    TAG_TIME                    = "Time";                                                       //$NON-NLS-1$
   private static final String    TAG_VALUE                   = "Value";                                                      //$NON-NLS-1$

   private static final String    TAG_NS2_SPEED               = "ns2:Speed";                                                  //$NON-NLS-1$
   private static final String    TAG_NS2_WATTS               = "ns2:Watts";                                                  //$NON-NLS-1$
   private static final String    TAG_NS3_SPEED               = "ns3:Speed";                                                  //$NON-NLS-1$
   private static final String    TAG_NS3_WATTS               = "ns3:Watts";                                                  //$NON-NLS-1$

   private static final String    TAG_TPX                     = "TPX";                                                        //$NON-NLS-1$
   private static final String    TAG_TPX_AVERAGEWATTS        = "AverageWatts";                                               //$NON-NLS-1$
   private static final String    TAG_TPX_SPEED               = "Speed";                                                      //$NON-NLS-1$
   private static final String    TAG_TPX_WATTS               = "Watts";                                                      //$NON-NLS-1$

   private static final String    SENSOR_STATE_PRESENT        = "Present";                                                    //$NON-NLS-1$
   private static final String    ATTR_VALUE_SPORT            = "Sport";                                                      //$NON-NLS-1$

   private static final int       DEFAULT_YEAR                = 2007;
   private static final int       DEFAULT_MONTH               = 4;
   private static final int       DEFAULT_DAY                 = 1;

   private final long             DEFAULT_TIME;

   private final SimpleDateFormat TIME_FORMAT;
   private final SimpleDateFormat TIME_FORMAT_SSSZ;
   private final SimpleDateFormat TIME_FORMAT_RFC822;
   {

      DEFAULT_TIME = ZonedDateTime
            .of(DEFAULT_YEAR, DEFAULT_MONTH, DEFAULT_DAY, 0, 0, 0, 0, TimeTools.getDefaultTimeZone())
            .toInstant()
            .toEpochMilli();

      final String DateTimePattern = "yyyy-MM-dd'T'HH:mm:ss"; //$NON-NLS-1$

// SET_FORMATTING_OFF

      TIME_FORMAT          = new SimpleDateFormat(DateTimePattern + "'Z'"); //$NON-NLS-1$
      TIME_FORMAT_SSSZ     = new SimpleDateFormat(DateTimePattern + ".SSS'Z'"); //$NON-NLS-1$
      TIME_FORMAT_RFC822   = new SimpleDateFormat(DateTimePattern + "Z"); //$NON-NLS-1$

      TIME_FORMAT          .setTimeZone(TimeZone.getTimeZone(UI.TIME_ZONE_UTC));
      TIME_FORMAT_SSSZ     .setTimeZone(TimeZone.getTimeZone(UI.TIME_ZONE_UTC));
      TIME_FORMAT_RFC822   .setTimeZone(TimeZone.getTimeZone(UI.TIME_ZONE_UTC));

// SET_FORMATTING_ON
   }

   private ImportState_File    _importState_File;
   private boolean             _importState_IsIgnoreSpeedValues;

   private boolean             _isComputeAveragePower;

   private boolean             _isInActivity;
   private boolean             _isInCourse;
   private boolean             _isInLap;

   private boolean             _isInAltitude;
   private boolean             _isInCalories;
   private boolean             _isInCadence;
   private boolean             _isInDistance;
   private boolean             _isInHeartRate;
   private boolean             _isInHeartRateValue;
   private boolean             _isInLatitude;
   private boolean             _isInLongitude;
   private boolean             _isInName;
   private boolean             _isInNotes;
   private boolean             _isInRunCadence;
   private boolean             _isInSensorState;
   private boolean             _isInTime;
   private boolean             _isInTrack;
   private boolean             _isInTrackpoint;

   private boolean             _isInNs2_Speed;
   private boolean             _isInNs2_Watts;
   private boolean             _isInNs3_Speed;
   private boolean             _isInNs3_Watts;

   private boolean             _isInTPX;
   private boolean             _isInTPX_AverageWatts;
   private boolean             _isInTPX_Speed;
   private boolean             _isInTPX_Watts;

   private boolean             _isInCreator;
   private boolean             _isInCreatorName;
   private boolean             _isInCreatorVersionMajor;
   private boolean             _isInCreatorVersionMinor;

   private Map<Long, TourData> _alreadyImportedTours;
   private Map<Long, TourData> _newlyImportedTours;
   private TourbookDevice      _device;
   private String              _importFilePath;

   private boolean             _isPreviousTrackPointAPause;
   private boolean             _isFirstTrackPointInTrack;
   private final List<Long>    _pausedTime_Start = new ArrayList<>();
   private List<Long>          _pausedTime_End   = new ArrayList<>();
   private ArrayList<TimeData> _allTimeData      = new ArrayList<>();

   private TimeData            _timeData;

   private int                 _dataVersion      = -1;
   private int                 _lapCounter;
   private int                 _trackPointCounter;

   private boolean             _isSetLapMarker;
   private boolean             _isSetLapStartTime;
   private ArrayList<Long>     _allLapStart      = new ArrayList<>();

   private String              _activitySport;
   private long                _currentTime;
   private boolean             _isDistanceFromSensor;
   private boolean             _isFromStrideSensor;
   private float               _lapAverageWatts;
   private float               _lapCalories;
   private float               _totalLapAverageWatts;
   private float               _tourCalories;

   private StringBuilder       _characters       = new StringBuilder();

   private Sport               _sport;
   private String              _tourNotes;
   private String              _tourTitle;

   private class Sport {

      private String creatorName;
      private String creatorVersionMajor;
      private String creatorVersionMinor;

   }

   public GarminTCX_SAXHandler(final TourbookDevice deviceDataReader,
                               final String importFileName,
                               final DeviceData deviceData,
                               final Map<Long, TourData> alreadyImportedTours,
                               final Map<Long, TourData> newlyImportedTours,
                               final ImportState_File importState_File) {

      _device = deviceDataReader;
      _importFilePath = importFileName;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;
      _importState_File = importState_File;

      final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

      _importState_IsIgnoreSpeedValues = store.getBoolean(IPreferences.IS_IGNORE_SPEED_VALUES);
   }

   private static void formatDT(final DateTimeFormatter zonedFormatter,
                                final SimpleDateFormat jdkFormatter,
                                final StringBuilder sbJdk,
                                final StringBuilder sbZoned,
                                final ZonedDateTime dt,
                                final Calendar jdkCalendar) {

      final Date dtDate = Date.from(dt.toInstant());

      sbZoned.append(dt.format(zonedFormatter));
      sbZoned.append(" | "); //$NON-NLS-1$

      jdkCalendar.setFirstDayOfWeek(Calendar.MONDAY);
      jdkCalendar.setMinimalDaysInFirstWeek(4);

      jdkCalendar.setTime(dtDate);
      final int weekYear = Util.getYearForWeek(jdkCalendar);

      sbJdk.append(jdkFormatter.format(dtDate));
      sbJdk.append(UI.SPACE1 + weekYear + " | "); //$NON-NLS-1$
   }

// private static void weekCheck() {
//
//    final DateTime dt = new DateTime(//
//          2009, /* year */
//          12, /* monthOfYear */
//          6, /* dayOfMonth */
//          23, /* hourOfDay */
//          0, /* minuteOfHour */
//          0, /* secondOfMinute */
//          0 /* millisOfSecond */
//    );
//
//    final StringBuilder buffer = new StringBuilder()//
//          //
//          .append("Testing date ") //$NON-NLS-1$
//          .append(dt.toString())
//          .append("\n") //$NON-NLS-1$
//          //
//          .append("Joda-Time timezone is ") //$NON-NLS-1$
//          .append(DateTimeZone.getDefault())
//          .append(" yet joda wrongly thinks week is ") //$NON-NLS-1$
//          .append(_jodaWeekFormatter.print(dt))
//          .append("\n") //$NON-NLS-1$
//          //
//          .append("JDK timezone is ") //$NON-NLS-1$
//          .append(TimeZone.getDefault().getID())
//          .append(" yet jdk rightfully thinks week is ") //$NON-NLS-1$
//          .append(_jdkWeekFormatter.format(dt.toDate()))
//          .append(" (jdk got it right ?!?!)"); //$NON-NLS-1$
//
//    System.out.println(buffer.toString());
// }

   public static void main(final String[] args) {

//    final String pattern = "w dd.MM.yyyy";
      final String jodPattern = "ww xx     "; //$NON-NLS-1$
      final String jdkPattern = "ww yy"; //$NON-NLS-1$

      final DateTimeFormatter zonedFormatter = DateTimeFormatter.ofPattern(jodPattern);
      final StringBuilder sbJdk = new StringBuilder();
      final StringBuilder sbJoda = new StringBuilder();

      final Locale[] locales = Locale.getAvailableLocales();
      for (int i = 0; i < locales.length; i++) {

         final Locale locale = locales[i];
         final String language = locale.getLanguage();
         final String country = locale.getCountry();
         final String locale_name = locale.getDisplayName();

         if ((i == 120 || i == 132) == false) {
            continue;
         }

         final SimpleDateFormat jdkFormatter = new SimpleDateFormat(jdkPattern, locale);
         final Calendar calendar = GregorianCalendar.getInstance(locale);

         System.out.println();
         System.out.println(i + ": " + language + ", " + country + ", " + locale_name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

         for (int year = 2005; year < 2011; year++) {

            sbJoda.append(year + ": "); //$NON-NLS-1$
            sbJdk.append(year + ": "); //$NON-NLS-1$

            int days = 0;
            final ZonedDateTime dt = ZonedDateTime.of(year, 12, 22, 8, 0, 0, 0, TimeTools.getDefaultTimeZone());

            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            sbJoda.append(UI.SPACE4);
            sbJdk.append(UI.SPACE4);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);
            formatDT(zonedFormatter, jdkFormatter, sbJdk, sbJoda, dt.plusDays(days++), calendar);

            System.out.println(sbJoda.toString());
            System.out.println(sbJdk.toString());
            System.out.println();

            sbJoda.setLength(0);
            sbJdk.setLength(0);
         }
      }
   }

   /**
    * Check if date time starts with the date 2007-04-01, this can happen when the tcx file is
    * partly corrupt. When tour starts with the date 2007-04-01, move forward in the list until
    * another date occurs and use this as the start date.
    */
   private void adjustTourStart() {

      int validIndex = 0;
      ZonedDateTime checkedTourStart = null;

      for (final TimeData timeData : _allTimeData) {

         checkedTourStart = TimeTools.getZonedDateTime(timeData.absoluteTime);

         if (checkedTourStart.getYear() == DEFAULT_YEAR
               && checkedTourStart.getMonthValue() == DEFAULT_MONTH
               && checkedTourStart.getDayOfMonth() == DEFAULT_DAY) {

            // this is an invalid time slice

            validIndex++;

         } else {

            // this is a valid time slice
            break;
         }
      }

      if (validIndex == 0) {

         // date is not 2007-04-01

         return;

      } else {

         if (validIndex == _allTimeData.size()) {

            // all time slices have the same "invalid" date 2007-04-01 but the date also could be valid

            return;
         }
      }

      /*
       * the date starts with 2007-04-01 but it changes to another date
       */

      final TimeData[] timeSlices = _allTimeData.toArray(new TimeData[_allTimeData.size()]);

      /*
       * get average time slice duration
       */
      long sliceAvgDuration;
      if (validIndex == 1) {

         sliceAvgDuration = 8;

      } else {

         long prevSliceTime = 0;
         long sliceDuration = 0;

         for (int sliceIndex = 0; sliceIndex < validIndex; sliceIndex++) {

            final long currentTime = timeSlices[sliceIndex].absoluteTime / 1000;

            if (sliceIndex > 0) {
               sliceDuration += currentTime - prevSliceTime;
            }

            prevSliceTime = currentTime;
         }

         sliceAvgDuration = sliceDuration / validIndex;
      }

      long validTime = timeSlices[validIndex].absoluteTime / 1000;
      long prevInvalidTime = 0;

      for (int sliceIndex = validIndex - 1; sliceIndex >= 0; sliceIndex--) {

         final TimeData timeSlice = timeSlices[sliceIndex];
         final long currentInvalidTime = timeSlice.absoluteTime / 1000;

         if (sliceIndex == validIndex - 1) {

            /*
             * this is the time slice before the valid time slices, use the average time slice diff
             * to get the time, because this time cannot be evaluated it is estimated
             */

            validTime = validTime - sliceAvgDuration;

         } else {

            final long timeDiff = prevInvalidTime - currentInvalidTime;
            validTime = validTime - timeDiff;
         }

         timeSlice.absoluteTime = validTime * 1000;
         prevInvalidTime = currentInvalidTime;
      }

      //We remove any pauses that happened before the official tour start time
      if (_pausedTime_Start.size() > 0 && _pausedTime_End.size() > 0) {

         final List<Integer> pausesToRemove = new ArrayList<>();
         for (int index = 0; index < _pausedTime_Start.size(); ++index) {
            if (_pausedTime_Start.get(index) < timeSlices[0].absoluteTime) {
               pausesToRemove.add(index);
            }
         }

         for (int index = pausesToRemove.size() - 1; index >= 0; --index) {
            _pausedTime_Start.remove(index);
            _pausedTime_End.remove(index);
         }
      }

      TourLogManager.subLog_INFO(

            String.format(Messages.GarminTCX_SAXHandler_InvalidDate_2007_04_01,
                  _importFilePath,
                  TimeTools.getZonedDateTime(_allTimeData.get(0).absoluteTime)));
   }

   @Override
   public void characters(final char[] chars, final int startIndex, final int length) throws SAXException {

      if (_isInTime
            || _isInCalories
            || _isInLatitude
            || _isInLongitude
            || _isInAltitude
            || _isInDistance
            || _isInCadence
            || _isInName
            || _isInRunCadence
            || _isInSensorState
            || _isInHeartRate
            || _isInHeartRateValue

            || _isInNs2_Speed
            || _isInNs2_Watts
            || _isInNs3_Speed
            || _isInNs3_Watts

            || _isInTPX_AverageWatts
            || _isInTPX_Speed
            || _isInTPX_Watts

            || _isInCreatorName
            || _isInCreatorVersionMajor
            || _isInCreatorVersionMinor

            || _isInNotes

      ) {

         _characters.append(chars, startIndex, length);
      }
   }

   void dispose() {

      _allLapStart.clear();
      _allTimeData.clear();
      _pausedTime_Start.clear();
      _pausedTime_End.clear();
   }

   @Override
   public void endElement(final String uri, final String localName, final String name) throws SAXException {

      //System.out.println("</" + name + ">");

      try {

         if (_isInTrackpoint) {

            getData_TrackPoint_20_End(name);

         } else if (_isInCreator) {

            getData_Creator_20_End(name);
         }

         if (name.equals(TAG_TRACKPOINT)) {

            // keep trackpoint data

            _isInTrackpoint = false;

            finalize_Trackpoint();

         } else if (_isInNotes && name.equals(TAG_NOTES)) {

            _isInNotes = false;

            _tourNotes = _characters.toString();

         } else if (_isInCreator && name.equals(TAG_CREATOR)) {

            _isInCreator = false;

         } else if (name.equals(TAG_LAP)) {

            _isInLap = false;

            if (_trackPointCounter > 0) {
               /*
                * summarize calories when at least one trackpoint is available. This will fix a bug
                * because an invalid TCX file can contain old laps with calories but without
                * trackpoints
                */
               _tourCalories += _lapCalories;

               _totalLapAverageWatts += _lapAverageWatts;
            }

         } else if (name.equals(TAG_CALORIES)) {

            _isInCalories = false;

            /* every lap has a calorie value */
            _lapCalories += Util.parseFloat(_characters.toString());
            _characters.delete(0, _characters.length());

         } else if (name.equals(TAG_TPX_AVERAGEWATTS)) {

            _isInTPX_AverageWatts = false;

            _lapAverageWatts += Util.parseFloat(_characters.toString());
            _characters.delete(0, _characters.length());

            _isComputeAveragePower = false;

         } else if (name.equals(TAG_TRACK)) {

            _isInTrack = false;

         } else if (_isInCourse && _isInName) {

            _isInName = false;

            // "Name" tag occurs multiple times
            if (!_isInCreator) {

               _tourTitle = _characters.toString();
            }

         } else if (name.equals(TAG_ACTIVITY)) {

            /*
             * version 2: activity and tour ends
             */
            _isInActivity = false;

            finalize_Tour();

         } else if (name.equals(TAG_COURSE) || name.equals(TAG_HISTORY)) {

            /*
             * version 1+2: course and tour ends, v1: history ends
             */

            _isInCourse = false;

            finalize_Tour();
         }

      } catch (final NumberFormatException | ParseException e) {
         StatusUtil.showStatus(e);
      }

   }

   private void finalize_Tour() {

      // check if data are available
      if (_allTimeData.isEmpty()) {

         TourLogManager.subLog_INFO(String.format(Messages.GarminTCX_SAXHandler_FileIsEmpty, _importFilePath));

         _importState_File.isImportDone = true;
         _importState_File.isImportLogged = true;

         return;
      }

      validateTimeSeries();

      // create data object for each tour
      final TourData tourData = new TourData();

      tourData.setTourTitle(_tourTitle);

      // set tour notes
      setTourNotes(tourData);

      /*
       * set tour start date/time
       */
      adjustTourStart();
      final ZonedDateTime zonedStartTime = TimeTools.getZonedDateTime(_allTimeData.get(0).absoluteTime);
      tourData.setTourStartTime(zonedStartTime);

      tourData.setIsDistanceFromSensor(_isDistanceFromSensor);
      tourData.setIsStrideSensorPresent(_isFromStrideSensor);
      tourData.setDeviceTimeInterval((short) -1);
      tourData.setImportFilePath(_importFilePath);

      tourData.setDeviceModeName(_activitySport);
      tourData.setCalories(Math.round(_tourCalories * 1000));

      final String deviceName = _sport.creatorName;
      final String majorVersion = _sport.creatorVersionMajor;
      final String minorVersion = _sport.creatorVersionMinor;

      tourData.setDeviceId(_device.deviceId);

      tourData.setDeviceName(_device.visibleName
            + (deviceName == null
                  ? UI.EMPTY_STRING
                  : UI.SPACE + deviceName));

      tourData.setDeviceFirmwareVersion(

            majorVersion == null
                  ? UI.EMPTY_STRING

                  : majorVersion
                        + (minorVersion == null
                              ? UI.EMPTY_STRING
                              : UI.SYMBOL_DOT + minorVersion));

      /*
       * In the case where the power was retrieved from the trackpoint's
       * extension field and the file didn't contain the average power value, we
       * need to compute it ourselves.
       */
      if (_isComputeAveragePower) {

         final float[] powerSerie = tourData.getPowerSerie();
         if (powerSerie != null) {
            tourData.setPower_Avg(tourData.computeAvg_FromValues(powerSerie, 0, powerSerie.length - 1));
         }

      } else if (_totalLapAverageWatts > 0 && _lapCounter > 0) {
         tourData.setPower_Avg(_totalLapAverageWatts / _lapCounter);
      }

      tourData.createTimeSeries(_allTimeData, true);

      // after all data are added, the tour id can be created
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_GARMIN_TCX);
      final Long tourId = tourData.createTourId(uniqueId);

      /*
       * The tour start time timezone is set from lat/lon in createTimeSeries()
       */
      final ZonedDateTime tourStartTime_FromLatLon = tourData.getTourStartTime();

      if (zonedStartTime.equals(tourStartTime_FromLatLon) == false) {

         // time zone is different -> fix tour start components with adjusted time zone
         tourData.setTourStartTime_YYMMDD(tourStartTime_FromLatLon);
      }

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         tourData.finalizeTour_TimerPauses(_pausedTime_Start, _pausedTime_End, null);
         tourData.setTourDeviceTime_Recorded(tourData.getTourDeviceTime_Elapsed() - tourData.getTourDeviceTime_Paused());

         tourData.computeAltitudeUpDown();
         tourData.computeTourMovingTime();
         tourData.computeComputedValues();
      }

      _importState_File.isFileImportedWithValidData = true;
   }

   private void finalize_Trackpoint() {

      if (_timeData != null) {

         // set virtual time if time is not available
         if (_timeData.absoluteTime == Long.MIN_VALUE) {

            _timeData.absoluteTime = DEFAULT_TIME;
         } else {
            //If only the time was provided in the Trackpoint element,
            //we consider that a pause.
            if (_timeData.latitude == Double.MIN_VALUE &&
                  _timeData.longitude == Double.MIN_VALUE &&
                  _timeData.absoluteAltitude == Float.MIN_VALUE &&
                  _timeData.absoluteDistance == Float.MIN_VALUE &&
                  _timeData.pulse == Float.MIN_VALUE &&
                  _timeData.cadence == Float.MIN_VALUE &&
                  _timeData.speed == Float.MIN_VALUE &&
                  _timeData.power == Float.MIN_VALUE) {

               //If the previous and current TrackPoints are pauses, we
               //do not create a new pause event.
               if (!_isPreviousTrackPointAPause) {
                  _pausedTime_Start.add(_timeData.absoluteTime);
                  _isPreviousTrackPointAPause = true;
               }

            } else {

               if (_isPreviousTrackPointAPause) {

                  _pausedTime_End.add(_timeData.absoluteTime);
                  _isPreviousTrackPointAPause = false;

               } else if (_isFirstTrackPointInTrack && !_allTimeData.isEmpty() && !_isSetLapMarker) {

                  final long previousTime = _allTimeData.get(_allTimeData.size() - 1).absoluteTime;

                  if (_timeData.absoluteTime - previousTime > 1000) {
                     _pausedTime_Start.add(previousTime);
                     _pausedTime_End.add(_timeData.absoluteTime);
                  }
               }
            }
         }

         if (_isSetLapMarker) {

            _isSetLapMarker = false;

            _timeData.marker = 1;
            _timeData.markerLabel = Integer.toString(_lapCounter - 1);
         }

         _allTimeData.add(_timeData);

         _timeData = null;

         _trackPointCounter++;
         _isFirstTrackPointInTrack = false;
      }

      if (_isSetLapStartTime) {
         _isSetLapStartTime = false;
         _allLapStart.add(_currentTime);
      }
   }

   private void getData_Creator_10_Start(final String name) {

      if (name.equals(TAG_NAME)) {
         _isInCreatorName = true;
      } else if (name.equals(TAG_CREATOR_VERSION_MAJOR)) {
         _isInCreatorVersionMajor = true;
      } else if (name.equals(TAG_CREATOR_VERSION_MINOR)) {
         _isInCreatorVersionMinor = true;
      } else {
         return;
      }

      _characters.delete(0, _characters.length());
   }

   private void getData_Creator_20_End(final String name) {

      final String charData = _characters.toString();

      if (_isInCreatorName && name.equals(TAG_NAME)) {

         _isInCreatorName = false;
         _sport.creatorName = charData;

      } else if (_isInCreatorVersionMajor && name.equals(TAG_CREATOR_VERSION_MAJOR)) {

         _isInCreatorVersionMajor = false;
         _sport.creatorVersionMajor = charData;

      } else if (_isInCreatorVersionMinor && name.equals(TAG_CREATOR_VERSION_MINOR)) {

         _isInCreatorVersionMinor = false;
         _sport.creatorVersionMinor = charData;
      }
   }

   private void getData_TrackPoint_10_Start(final String name) {

      if (name.equals(TAG_HEART_RATE_BPM)) {

         _isInHeartRate = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_ALTITUDE_METERS)) {

         _isInAltitude = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_DISTANCE_METERS)) {

         _isInDistance = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_CADENCE)) {

         _isInCadence = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_RUN_CADENCE)) {

         _isInRunCadence = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_SENSOR_STATE)) {

         _isInSensorState = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_TIME)) {

         _isInTime = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_LATITUDE_DEGREES)) {

         _isInLatitude = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_LONGITUDE_DEGREES)) {

         _isInLongitude = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_NS2_SPEED)) {

         _isInNs2_Speed = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_NS2_WATTS)) {

         _isInNs2_Watts = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_NS3_SPEED)) {

         _isInNs3_Speed = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_NS3_WATTS)) {

         _isInNs3_Watts = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_TPX)) {

         _isInTPX = true;
      } else if (_isInTPX && name.equals(TAG_TPX_SPEED)) {

         _isInTPX_Speed = true;
         _characters.delete(0, _characters.length());

      } else if (_isInTPX && name.equals(TAG_TPX_WATTS)) {

         _isInTPX_Watts = true;
         _characters.delete(0, _characters.length());

      } else if (_isInHeartRate && name.equals(TAG_VALUE)) {

         _isInHeartRateValue = true;
         _characters.delete(0, _characters.length());
      }
   }

   private void getData_TrackPoint_20_End(final String name) throws ParseException {

      if (_isInHeartRateValue && name.equals(TAG_VALUE)) {

         _isInHeartRateValue = false;

         if (_dataVersion == 2) {
            _timeData.pulse = Util.parseFloat(_characters.toString());
         }

      } else if (name.equals(TAG_HEART_RATE_BPM)) {

         _isInHeartRate = false;

         if (_dataVersion == 1) {
            _timeData.pulse = Util.parseFloat(_characters.toString());
         }

      } else if (name.equals(TAG_ALTITUDE_METERS)) {

         _isInAltitude = false;

         _timeData.absoluteAltitude = Util.parseFloat(_characters.toString());

      } else if (name.equals(TAG_DISTANCE_METERS)) {

         _isInDistance = false;
         _timeData.absoluteDistance = Util.parseFloat(_characters.toString());

      } else if (name.equals(TAG_CADENCE)) {

         _isInCadence = false;

         float cadence = Util.parseFloat(_characters.toString());
         _timeData.cadence = cadence = cadence == Float.MIN_VALUE ? 0 : cadence;

      } else if (name.equals(TAG_RUN_CADENCE)) {

         _isInRunCadence = false;
         _isFromStrideSensor = true;

         float cadence = Util.parseFloat(_characters.toString());
         _timeData.cadence = cadence = cadence == Float.MIN_VALUE ? 0 : cadence;

      } else if (name.equals(TAG_SENSOR_STATE)) {

         _isInSensorState = false;
         _isDistanceFromSensor = SENSOR_STATE_PRESENT.equalsIgnoreCase(_characters.toString());

      } else if (name.equals(TAG_LATITUDE_DEGREES)) {

         _isInLatitude = false;

         _timeData.latitude = Util.parseDouble(_characters.toString());

      } else if (name.equals(TAG_LONGITUDE_DEGREES)) {

         _isInLongitude = false;

         _timeData.longitude = Util.parseDouble(_characters.toString());

      } else if (name.equals(TAG_NS2_SPEED)) {

         _isInNs2_Speed = false;

         if (_importState_IsIgnoreSpeedValues == false) {

            // use speed values from the device

            _timeData.speed = Util.parseFloat(_characters.toString());
         }

      } else if (name.equals(TAG_NS2_WATTS)) {

         _isInNs2_Watts = false;

         _timeData.power = Util.parseFloat(_characters.toString());

      } else if (name.equals(TAG_NS3_SPEED)) {

         _isInNs3_Speed = false;

         if (_importState_IsIgnoreSpeedValues == false) {

            // use speed values from the device

            _timeData.speed = Util.parseFloat(_characters.toString());
         }

      } else if (name.equals(TAG_NS3_WATTS)) {

         _isInNs3_Watts = false;

         _timeData.power = Util.parseFloat(_characters.toString());

      } else if (TAG_TPX.equals(name)) {

         _isInTPX = false;

      } else if (_isInTPX && TAG_TPX_SPEED.equals(name)) {

         _isInTPX_Speed = false;

         if (_importState_IsIgnoreSpeedValues == false) {

            // use speed values from the device

            _timeData.speed = Util.parseFloat(_characters.toString());
         }

      } else if (_isInTPX && TAG_TPX_WATTS.equals(name)) {

         _isInTPX_Watts = false;

         _timeData.power = Util.parseFloat(_characters.toString());

      } else if (name.equals(TAG_TIME)) {

         _isInTime = false;

         final String timeString = _characters.toString();

         try {
            _currentTime = ZonedDateTime.parse(timeString).toInstant().toEpochMilli();
         } catch (final Exception e0) {
            try {
               _currentTime = TIME_FORMAT.parse(timeString).getTime();
            } catch (final ParseException e1) {
               try {
                  _currentTime = TIME_FORMAT_SSSZ.parse(timeString).getTime();
               } catch (final ParseException e2) {
                  try {
                     _currentTime = TIME_FORMAT_RFC822.parse(timeString).getTime();
                  } catch (final ParseException e3) {

                     TourLogManager.log_ERROR(e3.getMessage() + " in " + _importFilePath); //$NON-NLS-1$
                  }
               }
            }
         }

         _timeData.absoluteTime = _currentTime;

      }
   }

   private void initialize_NewLap() {

      _isInLap = true;

      _lapCounter++;
      _lapAverageWatts = 0;
      _lapCalories = 0;
      _trackPointCounter = 0;

      if (_lapCounter > 1) {
         _isSetLapMarker = true;
      }
      _isSetLapStartTime = true;
   }

   private void initialize_NewTour() {

      _lapCounter = 0;
      _isSetLapMarker = false;
      _allLapStart.clear();

      _allTimeData.clear();

      _isFromStrideSensor = false;
      _sport = new Sport();
      _tourNotes = null;
      _tourTitle = UI.EMPTY_STRING;

      _pausedTime_Start.clear();
      _pausedTime_End.clear();

      _isComputeAveragePower = true;
   }

   /**
    * Set the notes into the description and/or title field
    *
    * @param tourData
    */
   private void setTourNotes(final TourData tourData) {

      if (StringUtils.isNullOrEmpty(_tourNotes)) {
         return;
      }

      final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

      final boolean isDescriptionField = store.getBoolean(IPreferences.IS_IMPORT_INTO_DESCRIPTION_FIELD);
      final boolean isTitleField = store.getBoolean(IPreferences.IS_IMPORT_INTO_TITLE_FIELD);

      if (isDescriptionField) {
         tourData.setTourDescription(_tourNotes);
      }

      if (isTitleField) {

         final boolean isImportAll = store.getBoolean(IPreferences.IS_TITLE_IMPORT_ALL);
         final int titleCharacters = store.getInt(IPreferences.NUMBER_OF_TITLE_CHARACTERS);

         if (isImportAll) {
            tourData.setTourTitle(_tourNotes);
         } else {
            final int endIndex = Math.min(_tourNotes.length(), titleCharacters);
            tourData.setTourTitle(_tourNotes.substring(0, endIndex));
         }
      }
   }

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

//    System.out.print("<" + name + ">\n");

      if (_dataVersion > 0) {

         if (_dataVersion == 1) {

            /*
             * xsi:schemaLocation=http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1
             */
            if (_isInCourse) {

               if (_isInTrackpoint) {

                  getData_TrackPoint_10_Start(name);

               } else if (name.equals(TAG_TRACKPOINT)) {

                  _isInTrackpoint = true;

                  // create new time item
                  _timeData = new TimeData();

               } else if (name.equals(TAG_LAP)) {

                  initialize_NewLap();
               }

            } else if (name.equals(TAG_COURSE) || name.equals(TAG_HISTORY)) {

               /*
                * a new activity starts
                */

               _isInCourse = true;

               initialize_NewTour();
            }

         } else if (_dataVersion == 2) {

            /*
             * xsi:schemaLocation=http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2
             */

            if (_isInActivity) {

               if (_isInLap) {

                  if (name.equals(TAG_CALORIES)) {

                     _isInCalories = true;
                     _characters.delete(0, _characters.length());
                  }

                  if (name.equals(TAG_TPX_AVERAGEWATTS)) {

                     _isInTPX_AverageWatts = true;
                     _characters.delete(0, _characters.length());
                  }

                  if (_isInTrack) {

                     if (_isInTrackpoint) {

                        getData_TrackPoint_10_Start(name);

                     } else if (name.equals(TAG_TRACKPOINT)) {

                        _isInTrackpoint = true;

                        // create new time item
                        _timeData = new TimeData();

                     } else if (name.equals(TAG_DISTANCE_METERS)) {

                        _isInDistance = true;
                        _characters.delete(0, _characters.length());

                     }
                  } else if (name.equals(TAG_TRACK)) {

                     _isInTrack = true;
                     _isFirstTrackPointInTrack = true;

                  }

               } else if (name.equals(TAG_LAP)) {

                  initialize_NewLap();
               }

            } else if (_isInCourse) {

               if (_isInTrackpoint) {

                  getData_TrackPoint_10_Start(name);

               } else if (name.equals(TAG_TRACKPOINT)) {

                  _isInTrackpoint = true;

                  // create new time item
                  _timeData = new TimeData();

               } else if (name.equals(TAG_NAME)) {

                  _isInName = true;
                  _characters.delete(0, _characters.length());
               }

            } else if (name.equals(TAG_ACTIVITY) || name.equals(TAG_COURSE)) {

               /*
                * a new activity/course starts
                */

               if (name.equals(TAG_ACTIVITY)) {
                  _isInActivity = true;

                  /* get sport type */

                  _activitySport = attributes.getValue(ATTR_VALUE_SPORT);

               } else if (name.equals(TAG_COURSE)) {
                  _isInCourse = true;
               }

               initialize_NewTour();
            }
         }

         // common tags
         if ((_dataVersion == 1 || _dataVersion == 2) && (_isInActivity || _isInCourse)) {

            if (_isInCreator) {
               getData_Creator_10_Start(name);
            }

            if (name.equals(TAG_NOTES)) {

               _isInNotes = true;
               _characters.delete(0, _characters.length());

            } else if (name.equals(TAG_CREATOR)) {

               _isInCreator = true;
            }
         }

      } else if (name.equals(TAG_DATABASE)) {

         /*
          * get version of the XML file
          */
         for (int attrIndex = 0; attrIndex < attributes.getLength(); attrIndex++) {

            final String value = attributes.getValue(attrIndex);

            if (value.equalsIgnoreCase(TRAINING_CENTER_DATABASE_V1)) {
               _dataVersion = 1;
               return;
            } else if (value.equalsIgnoreCase(TRAINING_CENTER_DATABASE_V2)) {
               _dataVersion = 2;
               return;
            }
         }
      }
   }

   /**
    * Remove duplicated entries and, if necessary, recomputes the absolute distance values.
    * <p>
    * There are cases where the lap end time and the next lap start time have the same time value,
    * so there are duplicated times which causes problems like markers are not displayed because the
    * marker time is twice available.
    * There are cases where the {@link GarminTCX_SAXHandler#TAG_DISTANCE_METERS} element (at the
    * {@link GarminTCX_SAXHandler#TAG_TRACKPOINT} level) is reset within each new lap.
    * In this case, we need to recompute correctly all the {@link TimeData#absoluteDistance} values.
    */
   private void validateTimeSeries() {

      final ArrayList<TimeData> removeTimeData = new ArrayList<>();

      TimeData previousTimeData = null;
      TimeData firstMarkerTimeData = null;

      boolean recomputeAbsoluteDistances = false;
      float previousTrackPointDistance = 0;
      float currentTrackPointDistance = 0;

      for (final TimeData timeData : _allTimeData) {

         if (previousTimeData != null) {

            if (previousTimeData.absoluteTime == timeData.absoluteTime) {

               // current slice has the same time as the previous slice

               if (firstMarkerTimeData == null) {

                  // initialize first item

                  firstMarkerTimeData = previousTimeData;
               }

               // copy marker into the first time data

               if (firstMarkerTimeData.markerLabel == null && timeData.markerLabel != null) {

                  firstMarkerTimeData.marker = timeData.marker;
                  firstMarkerTimeData.markerLabel = timeData.markerLabel;
               }

               // remove obsolete time data
               removeTimeData.add(timeData);

            } else {

               /*
                * current slice time is different than the previous
                */
               firstMarkerTimeData = null;
            }

            // If we have found that we need to recompute the TimeData distances, we don't need to check anymore
            if (recomputeAbsoluteDistances == false) {

               final float distanceDifference = previousTimeData.absoluteDistance - timeData.absoluteDistance;

               // Checking that the difference of distance is more than 5 meters
               // as there were reported cases where the previous distance was greater by about 1 meter
               // see https://sourceforge.net/p/mytourbook/discussion/622811/thread/926e45c3/#2208
               if (timeData.absoluteDistance == 0 && distanceDifference > 5) {

                  recomputeAbsoluteDistances = true;

                  timeData.absoluteDistance = previousTimeData.absoluteDistance;
               }
            } else {
               if (timeData.absoluteDistance > 0) { // We are still within the same lap
                  currentTrackPointDistance = timeData.absoluteDistance;
                  timeData.absoluteDistance = currentTrackPointDistance - previousTrackPointDistance + previousTimeData.absoluteDistance;

                  previousTrackPointDistance = currentTrackPointDistance;
               } else if (timeData.absoluteDistance == 0) { // We are entering a new lap

                  timeData.absoluteDistance = previousTimeData.absoluteDistance;
                  previousTrackPointDistance = 0;
               }
            }
         }

         previousTimeData = timeData;
      }

      _allTimeData.removeAll(removeTimeData);
   }
}
