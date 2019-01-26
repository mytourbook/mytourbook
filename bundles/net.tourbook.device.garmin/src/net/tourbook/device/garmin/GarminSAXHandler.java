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
package net.tourbook.device.garmin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TimeData;
import net.tourbook.data.TourData;
import net.tourbook.importdata.DeviceData;
import net.tourbook.importdata.TourbookDevice;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GarminSAXHandler extends DefaultHandler {

   private static final String           TRAINING_CENTER_DATABASE_V1 = "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1"; //$NON-NLS-1$
   private static final String           TRAINING_CENTER_DATABASE_V2 = "http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2"; //$NON-NLS-1$
   //
   private static final String           TAG_DATABASE                = "TrainingCenterDatabase";                                     //$NON-NLS-1$

   private static final String           TAG_ACTIVITY                = "Activity";                                                   //$NON-NLS-1$
   private static final String           TAG_COURSE                  = "Course";                                                     //$NON-NLS-1$
   private static final String           TAG_HISTORY                 = "History";                                                    //$NON-NLS-1$
   //
   private static final String           TAG_CREATOR                 = "Creator";                                                    //$NON-NLS-1$
   private static final String           TAG_CREATOR_NAME            = "Name";                                                       //$NON-NLS-1$
   private static final String           TAG_CREATOR_VERSION_MAJOR   = "VersionMajor";                                               //$NON-NLS-1$
   private static final String           TAG_CREATOR_VERSION_MINOR   = "VersionMinor";                                               //$NON-NLS-1$
   //
   private static final String           TAG_ALTITUDE_METERS         = "AltitudeMeters";                                             //$NON-NLS-1$
   private static final String           TAG_CALORIES                = "Calories";                                                   //$NON-NLS-1$
   private static final String           TAG_CADENCE                 = "Cadence";                                                    //$NON-NLS-1$
   private static final String           TAG_DISTANCE_METERS         = "DistanceMeters";                                             //$NON-NLS-1$
   private static final String           TAG_HEART_RATE_BPM          = "HeartRateBpm";                                               //$NON-NLS-1$
   private static final String           TAG_LAP                     = "Lap";                                                        //$NON-NLS-1$
   private static final String           TAG_LATITUDE_DEGREES        = "LatitudeDegrees";                                            //$NON-NLS-1$
   private static final String           TAG_LONGITUDE_DEGREES       = "LongitudeDegrees";                                           //$NON-NLS-1$
   private static final String           TAG_NOTES                   = "Notes";                                                      //$NON-NLS-1$
   private static final String           TAG_RUN_CADENCE             = "RunCadence";                                                 //$NON-NLS-1$
   private static final String           TAG_SENSOR_STATE            = "SensorState";                                                //$NON-NLS-1$
   private static final String           TAG_TRACKPOINT              = "Trackpoint";                                                 //$NON-NLS-1$
   private static final String           TAG_TIME                    = "Time";                                                       //$NON-NLS-1$
   private static final String           TAG_VALUE                   = "Value";                                                      //$NON-NLS-1$
   private static final String           TAG_NS2_SPEED               = "ns2:Speed";                                                  //$NON-NLS-1$
   private static final String           TAG_NS2_WATTS               = "ns2:Watts";                                                  //$NON-NLS-1$
   private static final String           TAG_NS3_SPEED               = "ns3:Speed";                                                  //$NON-NLS-1$
   private static final String           TAG_NS3_WATTS               = "ns3:Watts";                                                  //$NON-NLS-1$

   private static final String           SENSOR_STATE_PRESENT        = "Present";                                                    //$NON-NLS-1$
   private static final String           ATTR_VALUE_SPORT            = "Sport";                                                      //$NON-NLS-1$

   private static final SimpleDateFormat TIME_FORMAT;
   private static final SimpleDateFormat TIME_FORMAT_SSSZ;
   private static final SimpleDateFormat TIME_FORMAT_RFC822;

   private static final int              DEFAULT_YEAR                = 2007;
   private static final int              DEFAULT_MONTH               = 4;
   private static final int              DEFAULT_DAY                 = 1;

   private static final long             DEFAULT_TIME;

   static {

      DEFAULT_TIME = ZonedDateTime
            .of(DEFAULT_YEAR, DEFAULT_MONTH, DEFAULT_DAY, 0, 0, 0, 0, TimeTools.getDefaultTimeZone())
            .toInstant()
            .toEpochMilli();

      TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //$NON-NLS-1$
      TIME_FORMAT_SSSZ = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"); //$NON-NLS-1$
      TIME_FORMAT_RFC822 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ"); //$NON-NLS-1$

      TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
      TIME_FORMAT_SSSZ.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
      TIME_FORMAT_RFC822.setTimeZone(TimeZone.getTimeZone("UTC")); //$NON-NLS-1$
   }

   private boolean                 _importState_IsIgnoreSpeedValues;
   private boolean                 _isInActivity;
   private boolean                 _isInCourse;
   private boolean                 _isInLap;
   //
   private boolean                 _isInCalories;
   private boolean                 _isInTrackpoint;
   private boolean                 _isInTime;
   private boolean                 _isInLatitude;
   private boolean                 _isInLongitude;
   private boolean                 _isInAltitude;
   private boolean                 _isInDistance;
   private boolean                 _isInCadence;
   private boolean                 _isInRunCadence;
   private boolean                 _isInSensorState;
   private boolean                 _isInHeartRate;
   private boolean                 _isInHeartRateValue;
   private boolean                 _isInNotes;
   private boolean                 _isInNs2Speed;
   private boolean                 _isInNs2Watts;
   private boolean                 _isInNs3Speed;
   private boolean                 _isInNs3Watts;
   //
   private boolean                 _isInCreator;
   private boolean                 _isInCreatorName;
   private boolean                 _isInCreatorVersionMajor;
   private boolean                 _isInCreatorVersionMinor;
   //
   private HashMap<Long, TourData> _alreadyImportedTours;
   private HashMap<Long, TourData> _newlyImportedTours;
   private TourbookDevice          _device;
   private String                  _importFilePath;
   //
   private ArrayList<TimeData>     _dtList      = new ArrayList<>();

   private TimeData                _timeData;

   private int                     _dataVersion = -1;
   private int                     _lapCounter;
   private int                     _trackPointCounter;

   private boolean                 _isSetLapMarker;
   private boolean                 _isSetLapStartTime;
   private ArrayList<Long>         _lapStart    = new ArrayList<Long>();
   private boolean                 _isImported;

   private long                    _currentTime;
   private String                  _activitySport;
   private int                     _tourCalories;
   private int                     _lapCalories;
   private boolean                 _isDistanceFromSensor;
   private boolean                 _isFromStrideSensor;
   private StringBuilder           _characters  = new StringBuilder();

   private String                  _tourNotes;
   private Sport                   _sport;

   private class Sport {

      private String creatorName;
      private String creatorVersionMajor;
      private String creatorVersionMinor;

   }

   public GarminSAXHandler(final TourbookDevice deviceDataReader,
                           final String importFileName,
                           final DeviceData deviceData,
                           final HashMap<Long, TourData> alreadyImportedTours,
                           final HashMap<Long, TourData> newlyImportedTours) {

      _device = deviceDataReader;
      _importFilePath = importFileName;
      _alreadyImportedTours = alreadyImportedTours;
      _newlyImportedTours = newlyImportedTours;

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
      sbJdk.append(" " + weekYear + " | "); //$NON-NLS-1$ //$NON-NLS-2$
   }

//	private static void weekCheck() {
//
//		final DateTime dt = new DateTime(//
//				2009, /* year */
//				12, /* monthOfYear */
//				6, /* dayOfMonth */
//				23, /* hourOfDay */
//				0, /* minuteOfHour */
//				0, /* secondOfMinute */
//				0 /* millisOfSecond */
//		);
//
//		final StringBuilder buffer = new StringBuilder()//
//				//
//				.append("Testing date ") //$NON-NLS-1$
//				.append(dt.toString())
//				.append("\n") //$NON-NLS-1$
//				//
//				.append("Joda-Time timezone is ") //$NON-NLS-1$
//				.append(DateTimeZone.getDefault())
//				.append(" yet joda wrongly thinks week is ") //$NON-NLS-1$
//				.append(_jodaWeekFormatter.print(dt))
//				.append("\n") //$NON-NLS-1$
//				//
//				.append("JDK timezone is ") //$NON-NLS-1$
//				.append(TimeZone.getDefault().getID())
//				.append(" yet jdk rightfully thinks week is ") //$NON-NLS-1$
//				.append(_jdkWeekFormatter.format(dt.toDate()))
//				.append(" (jdk got it right ?!?!)"); //$NON-NLS-1$
//
//		System.out.println(buffer.toString());
//	}

   public static void main(final String[] args) {

//		final String pattern = "w dd.MM.yyyy";
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
            sbJoda.append("    "); //$NON-NLS-1$
            sbJdk.append("    "); //$NON-NLS-1$
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
    * another date occures and use this as the start date.
    */
   private void adjustTourStart() {

      int validIndex = 0;
      ZonedDateTime checkedTourStart = null;

      for (final TimeData timeData : _dtList) {

         checkedTourStart = TimeTools.getZonedDateTime(timeData.absoluteTime);

         if (checkedTourStart.getYear() == DEFAULT_YEAR
               && checkedTourStart.getMonthValue() == DEFAULT_MONTH
               && checkedTourStart.getDayOfMonth() == DEFAULT_DAY) {

            // this is an invalid time slice

            validIndex++;
            continue;

         } else {

            // this is a valid time slice
            break;
         }
      }

      if (validIndex == 0) {

         // date is not 2007-04-01

         return;

      } else {

         if (validIndex == _dtList.size()) {

            // all time slices have the same "invalid" date 2007-04-01 but the date also could be valid

            return;
         }
      }

      /*
       * the date starts with 2007-04-01 but it changes to another date
       */

      final TimeData[] timeSlices = _dtList.toArray(new TimeData[_dtList.size()]);

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

      StatusUtil.log(//
            NLS.bind(//
                  Messages.Garmin_SAXHandler_InvalidDate_2007_04_01,
                  _importFilePath,
                  TimeTools.getZonedDateTime(_dtList.get(0).absoluteTime)));
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
            || _isInRunCadence
            || _isInSensorState
            || _isInHeartRate
            || _isInHeartRateValue
            //
            || _isInNs2Speed
            || _isInNs2Watts
            || _isInNs3Speed
            || _isInNs3Watts
            //
            || _isInCreatorName
            || _isInCreatorVersionMajor
            || _isInCreatorVersionMinor
            //
            || _isInNotes
      //
      ) {

         _characters.append(chars, startIndex, length);
      }
   }

   public void dispose() {
      _lapStart.clear();
      _dtList.clear();
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

            finalizeTrackpoint();

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
                * because an invalid tcx file can contain old laps with calories but without
                * trackpoints
                */
               _tourCalories += _lapCalories;
            }

         } else if (name.equals(TAG_CALORIES)) {

            _isInCalories = false;

            try {
               /* every lap has a calorie value */
               _lapCalories += Integer.parseInt(_characters.toString());
               _characters.delete(0, _characters.length());

            } catch (final NumberFormatException e) {}

         } else if (name.equals(TAG_ACTIVITY)) {

            /*
             * version 2: activity and tour ends
             */
            _isInActivity = false;

            finalizeTour();

         } else if (name.equals(TAG_COURSE) || name.equals(TAG_HISTORY)) {

            /*
             * version 1+2: course and tour ends, v1: history ends
             */

            _isInCourse = false;

            finalizeTour();
         }

      } catch (final NumberFormatException e) {
         StatusUtil.showStatus(e);
      } catch (final ParseException e) {
         StatusUtil.showStatus(e);
      }

   }

   private void finalizeTour() {

      // check if data are available
      if (_dtList.size() == 0) {
         return;
      }

      validateTimeSeries();

      // create data object for each tour
      final TourData tourData = new TourData();

      // set tour notes
      setTourNotes(tourData);

      /*
       * set tour start date/time
       */
      adjustTourStart();
      tourData.setTourStartTime(TimeTools.getZonedDateTime(_dtList.get(0).absoluteTime));

      tourData.setIsDistanceFromSensor(_isDistanceFromSensor);
      tourData.setIsStrideSensorPresent(_isFromStrideSensor);
      tourData.setDeviceTimeInterval((short) -1);
      tourData.setImportFilePath(_importFilePath);

      tourData.setDeviceModeName(_activitySport);
      tourData.setCalories(_tourCalories);

      final String deviceName = _sport.creatorName;
      final String majorVersion = _sport.creatorVersionMajor;
      final String minorVersion = _sport.creatorVersionMinor;

      tourData.setDeviceId(_device.deviceId);

      tourData.setDeviceName(
            _device.visibleName + (deviceName == null
                  ? UI.EMPTY_STRING
                  : UI.SPACE + deviceName));

      tourData.setDeviceFirmwareVersion(
            majorVersion == null
                  ? UI.EMPTY_STRING
                  : majorVersion + (minorVersion == null //
                        ? UI.EMPTY_STRING
                        : UI.SYMBOL_DOT + minorVersion));

      tourData.createTimeSeries(_dtList, true);

      // after all data are added, the tour id can be created
      final String uniqueId = _device.createUniqueId(tourData, Util.UNIQUE_ID_SUFFIX_GARMIN_TCX);
      final Long tourId = tourData.createTourId(uniqueId);

      // check if the tour is already imported
      if (_alreadyImportedTours.containsKey(tourId) == false) {

         // add new tour to other tours
         _newlyImportedTours.put(tourId, tourData);

         // create additional data
         tourData.computeAltitudeUpDown();
         tourData.computeTourDrivingTime();
         tourData.computeComputedValues();
      }

      _isImported = true;
   }

   private void finalizeTrackpoint() {

      if (_timeData != null) {

         // set virtual time if time is not available
         if (_timeData.absoluteTime == Long.MIN_VALUE) {
            _timeData.absoluteTime = DEFAULT_TIME;
         }

         if (_isSetLapMarker) {
            _isSetLapMarker = false;

            _timeData.marker = 1;
            _timeData.markerLabel = Integer.toString(_lapCounter - 1);
         }

         _dtList.add(_timeData);

         _timeData = null;

         _trackPointCounter++;
      }

      if (_isSetLapStartTime) {
         _isSetLapStartTime = false;
         _lapStart.add(_currentTime);
      }
   }

   private void getData_Creator_10_Start(final String name) {

      if (name.equals(TAG_CREATOR_NAME)) {
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

      if (_isInCreatorName && name.equals(TAG_CREATOR_NAME)) {

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

         _isInNs2Speed = true;
         _characters.delete(0, _characters.length());

      } else if (name.equals(TAG_NS2_WATTS)) {

         _isInNs2Watts = true;
         _characters.delete(0, _characters.length());
         
      } else if (name.equals(TAG_NS3_SPEED)) {
         
         _isInNs3Speed = true;
         _characters.delete(0, _characters.length());
         
      } else if (name.equals(TAG_NS3_WATTS)) {
         
         _isInNs3Watts = true;
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

         _isInNs2Speed = false;

         if (_importState_IsIgnoreSpeedValues == false) {

            // use speed values from the device

            _timeData.speed = Util.parseFloat(_characters.toString());
         }

      } else if (name.equals(TAG_NS2_WATTS)) {

         _isInNs2Watts = false;

         _timeData.power = Util.parseFloat(_characters.toString());
         
      } else if (name.equals(TAG_NS3_SPEED)) {
         
         _isInNs3Speed = false;
         
         if (_importState_IsIgnoreSpeedValues == false) {
            
            // use speed values from the device
            
            _timeData.speed = Util.parseFloat(_characters.toString());
         }
         
      } else if (name.equals(TAG_NS3_WATTS)) {
         
         _isInNs3Watts = false;
         
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

                     Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                           final String message = e3.getMessage();
                           MessageDialog.openError(Display.getCurrent().getActiveShell(), "Error", message); //$NON-NLS-1$
                           System.err.println(message + " in " + _importFilePath); //$NON-NLS-1$
                        }
                     });
                  }
               }
            }
         }

         _timeData.absoluteTime = _currentTime;

      }
   }

   private void initializeNewLap() {

      _isInLap = true;

      _lapCounter++;
      _lapCalories = 0;
      _trackPointCounter = 0;

      if (_lapCounter > 1) {
         _isSetLapMarker = true;
      }
      _isSetLapStartTime = true;
   }

   private void initializeNewTour() {

      _lapCounter = 0;
      _isSetLapMarker = false;
      _lapStart.clear();

      _isFromStrideSensor = false;
      _dtList.clear();
      _tourNotes = null;
      _sport = new Sport();
   }

   /**
    * @return Returns <code>true</code> when a tour was imported
    */
   public boolean isImported() {
      return _isImported;
   }

   /**
    * Set the notes into the description and/or title field
    * 
    * @param tourData
    */
   private void setTourNotes(final TourData tourData) {

      if (_tourNotes == null || _tourNotes.length() == 0) {
         return;
      }

      final IPreferenceStore store = Activator.getDefault().getPreferenceStore();

      final boolean isDescriptionField = store.getBoolean(IPreferences.IS_IMPORT_INTO_DESCRIPTION_FIELD);
      final boolean isTitleField = store.getBoolean(IPreferences.IS_IMPORT_INTO_TITLE_FIELD);

      if (isDescriptionField) {
         tourData.setTourDescription(new String(_tourNotes));
      }

      if (isTitleField) {

         final boolean isImportAll = store.getBoolean(IPreferences.IS_TITLE_IMPORT_ALL);
         final int titleCharacters = store.getInt(IPreferences.NUMBER_OF_TITLE_CHARACTERS);

         if (isImportAll) {
            tourData.setTourTitle(new String(_tourNotes));
         } else {
            final int endIndex = Math.min(_tourNotes.length(), titleCharacters);
            tourData.setTourTitle(_tourNotes.substring(0, endIndex));
         }
      }
   }

   @Override
   public void startElement(final String uri, final String localName, final String name, final Attributes attributes)
         throws SAXException {

//		System.out.print("<" + name + ">\n");

      if (_dataVersion > 0) {

         if (_dataVersion == 1) {

            /*
             * http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v1
             */
            if (_isInCourse) {

               if (_isInTrackpoint) {

                  getData_TrackPoint_10_Start(name);

               } else if (name.equals(TAG_TRACKPOINT)) {

                  _isInTrackpoint = true;

                  // create new time item
                  _timeData = new TimeData();

               } else if (name.equals(TAG_LAP)) {

                  initializeNewLap();
               }

            } else if (name.equals(TAG_COURSE) || name.equals(TAG_HISTORY)) {

               /*
                * a new activity starts
                */

               _isInCourse = true;

               initializeNewTour();
            }

         } else if (_dataVersion == 2) {

            /*
             * http://www.garmin.com/xmlschemas/TrainingCenterDatabase/v2
             */

            if (_isInActivity) {

               if (_isInLap) {

                  if (_isInTrackpoint) {

                     getData_TrackPoint_10_Start(name);

                  } else if (name.equals(TAG_TRACKPOINT)) {

                     _isInTrackpoint = true;

                     // create new time item
                     _timeData = new TimeData();

                  } else if (name.equals(TAG_CALORIES)) {

                     _isInCalories = true;
                     _characters.delete(0, _characters.length());
                  }

               } else if (name.equals(TAG_LAP)) {

                  initializeNewLap();
               }

            } else if (_isInCourse) {

               if (_isInTrackpoint) {

                  getData_TrackPoint_10_Start(name);

               } else if (name.equals(TAG_TRACKPOINT)) {

                  _isInTrackpoint = true;

                  // create new time item
                  _timeData = new TimeData();
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

               initializeNewTour();
            }
         }

         // common tags
         if (_dataVersion == 1 || _dataVersion == 2) {

            if (_isInActivity || _isInCourse) {

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
         }

      } else if (name.equals(TAG_DATABASE)) {

         /*
          * get version of the xml file
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
    * Remove duplicated entries
    * <p>
    * There are cases where the lap end time and the next lap start time have the same time value,
    * so there are duplicated times which causes problems like markers are not displayed because the
    * marker time is twice available.
    */
   private void validateTimeSeries() {

      final ArrayList<TimeData> removeTimeData = new ArrayList<TimeData>();

      TimeData previousTimeData = null;
      TimeData firstMarkerTimeData = null;

      for (final TimeData timeData : _dtList) {

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
         }

         previousTimeData = timeData;
      }

      _dtList.removeAll(removeTimeData);
   }
}
