/*******************************************************************************
 * Copyright (C) 2020, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

public class CSVExport {

   private static final String GRAPH_LABEL_HEARTBEAT_UNIT               = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;

   private static final String CSV_EXPORT_DURATION_HHH_MM_SS            = "hhh:mm:ss";                                            //$NON-NLS-1$

   private static final String HEADER_BODY_MAX_PULSE                    = "BODY Pulse max";                                       //$NON-NLS-1$
   private static final String HEADER_BODY_AVERAGE_PULSE                = "BODY Pulse avg (%s)";                                  //$NON-NLS-1$
   private static final String HEADER_BODY_CALORIES                     = "BODY Calories (kcal)";                                 //$NON-NLS-1$
   private static final String HEADER_BODY_PERSON                       = "BODY Person";                                          //$NON-NLS-1$
   private static final String HEADER_BODY_RESTPULSE                    = "BODY Restpulse";                                       //$NON-NLS-1$

   private static final String HEADER_DATA_DP_TOLERANCE                 = "DATA DP Tolerance";                                    //$NON-NLS-1$
   private static final String HEADER_DATA_TIME_INTERVAL                = "DATA Time interval";                                   //$NON-NLS-1$
   private static final String HEADER_DATA_TIME_SLICES                  = "DATA Number of time slices";                           //$NON-NLS-1$

   private static final String HEADER_DEVICE_START_DISTANCE             = "DEVICE Start distance";                                //$NON-NLS-1$

   private static final String HEADER_ELEVATION_UP                      = "ELEVATION Elevation up (%s)";                          //$NON-NLS-1$
   private static final String HEADER_ELEVATION_DOWN                    = "ELEVATION Elevation down (%s)";                        //$NON-NLS-1$
   private static final String HEADER_ELEVATION_MAX                     = "ELEVATION Elevation max (%s)";                         //$NON-NLS-1$

   private static final String HEADER_MOTION_DISTANCE                   = "MOTION Distance (%s)";                                 //$NON-NLS-1$
   private static final String HEADER_MOTION_PACE_AVERAGE               = "MOTION Pace avg (%s)";                                 //$NON-NLS-1$
   private static final String HEADER_MOTION_SPEED_AVERAGE              = "MOTION Speed avg (%s)";                                //$NON-NLS-1$
   private static final String HEADER_MOTION_SPEED_MAX                  = "MOTION Speed max (%s)";                                //$NON-NLS-1$

   private static final String HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT = "POWERTRAIN Number of front shifts";                    //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT  = "POWERTRAIN Number of rear shifts";                     //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_AVERAGE_CADENCE        = "POWERTRAIN Cadence avg";                               //$NON-NLS-1$

   private static final String HEADER_TIME_DAY                          = "TIME Day";                                             //$NON-NLS-1$
   private static final String HEADER_TIME_MONTH                        = "TIME Month";                                           //$NON-NLS-1$
   private static final String HEADER_TIME_MOVING_TIME                  = "TIME Moving time (%s)";                                //$NON-NLS-1$
   private static final String HEADER_TIME_ISO_DATE_TIME                = "TIME ISO8601";                                         //$NON-NLS-1$
   private static final String HEADER_TIME_PAUSED_TIME                  = "TIME Paused time (%s)";                                //$NON-NLS-1$
   private static final String HEADER_TIME_PAUSED_TIME_RELATIVE         = "TIME Relative paused time (%)";                        //$NON-NLS-1$
   private static final String HEADER_TIME_RECORDING_TIME               = "TIME Recording time (%s)";                             //$NON-NLS-1$
   private static final String HEADER_TIME_TOUR_START_TIME              = "TIME Tour start time";                                 //$NON-NLS-1$
   private static final String HEADER_TIME_WEEK                         = "TIME Week";                                            //$NON-NLS-1$
   private static final String HEADER_TIME_WEEK_YEAR                    = "TIME Week year";                                       //$NON-NLS-1$
   private static final String HEADER_TIME_WEEKDAY                      = "TIME Weekday";                                         //$NON-NLS-1$
   private static final String HEADER_TIME_YEAR                         = "TIME Year";                                            //$NON-NLS-1$

   private static final String HEADER_TOUR_NUMBER_OF_MARKER             = "TOUR Number of markers";                               //$NON-NLS-1$
   private static final String HEADER_TOUR_NUMBER_OF_PHOTOS             = "TOUR Number of photos";                                //$NON-NLS-1$
   private static final String HEADER_TOUR_NUMBER_OF_TOURS              = "TOUR Number of tours";                                 //$NON-NLS-1$
   private static final String HEADER_TOUR_TAGS                         = "TOUR Tags";                                            //$NON-NLS-1$
   private static final String HEADER_TOUR_TITLE                        = "TOUR Title";                                           //$NON-NLS-1$
   private static final String HEADER_TOUR_TYPE_ID                      = "TOUR Tour type ID";                                    //$NON-NLS-1$
   private static final String HEADER_TOUR_TYPE_NAME                    = "TOUR Tour type name";                                  //$NON-NLS-1$

   private static final String HEADER_WEATHER_CLOUDS                    = "WEATHER Clouds";                                       //$NON-NLS-1$
   private static final String HEADER_WEATHER_TEMPERATURE_AVERAGE       = "WEATHER Temperature avg (%s)";                         //$NON-NLS-1$
   private static final String HEADER_WEATHER_WIND_DIRECTION            = "WEATHER Wind direction";                               //$NON-NLS-1$
   private static final String HEADER_WEATHER_WIND_SPEED                = "WEATHER Wind speed";                                   //$NON-NLS-1$

   private final NumberFormat  _nf1;
   private final NumberFormat  _nf3;
   {
      _nf1 = NumberFormat.getNumberInstance();
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf1.setGroupingUsed(false);

      _nf3 = NumberFormat.getNumberInstance();
      _nf3.setMinimumFractionDigits(2);
      _nf3.setMaximumFractionDigits(2);
      _nf3.setGroupingUsed(false);
   }

   private TourBookView _tourBookView;

   CSVExport(final ITreeSelection selection, final String selectedFilePath, final TourBookView tourBookView) {

      _tourBookView = tourBookView;

      /*
       * Write selected items into a csv file.
       */
      try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, UI.UTF_8);
            Writer exportWriter = new BufferedWriter(outputStreamWriter)) {

         final StringBuilder sb = new StringBuilder();

         exportCSV_010_Header(exportWriter, sb);

         for (final TreePath treePath : selection.getPaths()) {

            // truncate buffer
            sb.setLength(0);

            final int segmentCount = treePath.getSegmentCount();

            for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {

               final Object segment = treePath.getSegment(segmentIndex);
               final boolean isTour = segment instanceof TVITourBookTour;

               exportCSV_020_DateColumns(sb, segmentCount, segment, isTour);

               if (segment instanceof TVITourBookItem) {

                  final TVITourBookItem tviItem = (TVITourBookItem) segment;

                  // output data only for the last segment
                  if (segmentCount == 1
                        || (segmentCount == 2 && segmentIndex == 1)
                        || (segmentCount == 3 && segmentIndex == 2)) {

                     export_100_Time(sb, isTour, tviItem);
                     export_120_Tour(sb, isTour, tviItem);
                     export_140_Motion(sb, isTour, tviItem);
                     export_160_Elevation(sb, isTour, tviItem);
                     export_180_Weather(sb, isTour, tviItem);
                     export_200_Body(sb, isTour, tviItem);
                     export_220_Power(sb, isTour, tviItem);
                     export_240_Powertrain(sb, isTour, tviItem);
                     export_260_Training(sb, isTour, tviItem);
                     export_280_RunningDynamics(sb, isTour, tviItem);
                     export_300_Surfing(sb, isTour, tviItem);
                     export_320_Device(sb, isTour, tviItem);
                     export_340_Data(sb, isTour, tviItem);
                  }
               }
            }

            // end of line
            sb.append(net.tourbook.ui.UI.SYSTEM_NEW_LINE);
            exportWriter.write(sb.toString());
         }

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }
   }

   private void csvField_Int(final StringBuilder sb, final int fieldValue) {

      sb.append(fieldValue);
      sb.append(UI.TAB);
   }

   private void csvField_Str(final StringBuilder sb, final String fieldValue) {

      sb.append(fieldValue);
      sb.append(UI.TAB);
   }

   private void export_100_Time(final StringBuilder sb,
                                final boolean isTour,
                                final TVITourBookItem tviItem) {

      TourDateTime tourDateTime = null;
      ZonedDateTime tourStartDateTime = null;

      if (isTour) {

         tourDateTime = tviItem.colTourDateTime;
         tourStartDateTime = tourDateTime.tourZonedDateTime;
      }

//    // Time
//    defineColumn_1stColumn_Date();
//    defineColumn_Time_WeekDay();
//    defineColumn_Time_TourStartTime();
//    defineColumn_Time_TimeZoneDifference();
//    defineColumn_Time_TimeZone();
//    defineColumn_Time_MovingTime();
//    defineColumn_Time_RecordingTime();
//    defineColumn_Time_PausedTime();
//    defineColumn_Time_PausedTime_Relative();
//    defineColumn_Time_WeekNo();
//    defineColumn_Time_WeekYear();

//    csvField_Str(sb,                 HEADER_TIME_TOUR_START_TIME);
//    csvField_Str(sb,                 HEADER_TIME_ISO_DATE_TIME);
//    csvField_Str(sb,                 HEADER_TIME_WEEKDAY);
//    csvField_Str(sb,                 HEADER_TIME_WEEK_YEAR);
//
//    csvField_Str(sb, String.format(  HEADER_TIME_RECORDING_TIME,        Messages.App_Unit_Seconds_Small));
//    csvField_Str(sb, String.format(  HEADER_TIME_MOVING_TIME,           Messages.App_Unit_Seconds_Small));
//    csvField_Str(sb, String.format(  HEADER_TIME_PAUSED_TIME,           Messages.App_Unit_Seconds_Small));
//    csvField_Str(sb,                 HEADER_TIME_PAUSED_TIME_RELATIVE);
//    csvField_Str(sb, String.format(  HEADER_TIME_RECORDING_TIME,        CSV_EXPORT_DURATION_HHH_MM_SS));
//    csvField_Str(sb, String.format(  HEADER_TIME_MOVING_TIME,           CSV_EXPORT_DURATION_HHH_MM_SS));
//    csvField_Str(sb, String.format(  HEADER_TIME_PAUSED_TIME,           CSV_EXPORT_DURATION_HHH_MM_SS));

{// HEADER_TIME_TOUR_START_TIME

         if (isTour) {

            sb.append(tourStartDateTime.format(TimeTools.Formatter_Time_M));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TIME_ISO_DATE_TIME

         if (isTour) {
            sb.append(tourStartDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TIME_WEEKDAY

         if (isTour) {
            sb.append(tourDateTime.weekDay);
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TIME_WEEK_YEAR

         final long colValue = (tviItem).colWeekYear;
         if (colValue != 0) {
            sb.append(Long.toString(colValue));
         }
         sb.append(UI.TAB);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////

      { // HEADER_RECORDING_TIME

         final long colRecordingTime = (tviItem).colTourRecordingTime;
         if (colRecordingTime != 0) {
            sb.append(Long.toString(colRecordingTime));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_MOVING_TIME

         final long colDrivingTime = tviItem.colTourDrivingTime;
         if (colDrivingTime != 0) {
            sb.append(Long.toString(colDrivingTime));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TIME_PAUSED_TIME

         final long colPausedTime = tviItem.colPausedTime;
         if (colPausedTime != 0) {
            sb.append(Long.toString(colPausedTime));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TIME_PAUSED_TIME_RELATIVE

         final long colPausedTime = tviItem.colPausedTime;
         final long dbPausedTime = colPausedTime;
         final long dbRecordingTime = tviItem.colTourRecordingTime;
         final float relativePausedTime = dbRecordingTime == 0 //
               ? 0
               : (float) dbPausedTime / dbRecordingTime * 100;
         if (relativePausedTime != 0) {
            sb.append(_nf1.format(relativePausedTime));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_RECORDING_TIME hhh:mm:ss

         final long colRecordingTime = (tviItem).colTourRecordingTime;
         if (colRecordingTime != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colRecordingTime));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_MOVING_TIME hhh:mm:ss

         final long colDrivingTime = tviItem.colTourDrivingTime;
         if (colDrivingTime != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colDrivingTime));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TIME_PAUSED_TIME hhh:mm:ss

         final long colPausedTime = tviItem.colPausedTime;
         if (colPausedTime != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colPausedTime));
         }
         sb.append(UI.TAB);
      }
   }

   private void export_120_Tour(final StringBuilder sb,
                                final boolean isTour,
                                final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    defineColumn_Tour_TypeImage();
//    defineColumn_Tour_TypeText();
//    defineColumn_Tour_Title();
//    defineColumn_Tour_Marker();
//    defineColumn_Tour_Photos();
//    defineColumn_Tour_Tags();
//    defineColumn_Tour_Location_Start();
//    defineColumn_Tour_Location_End();
////  defineColumn_Tour_TagIds();            // for debugging

//    csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_TOURS);
//    csvField_Str(sb,                 HEADER_TOUR_TYPE_ID);
//    csvField_Str(sb,                 HEADER_TOUR_TYPE_NAME);
//    csvField_Str(sb,                 HEADER_TOUR_TITLE);
//    csvField_Str(sb,                 HEADER_TOUR_TAGS);
//    csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_MARKER);
//    csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_PHOTOS);

      { // HEADER_NUMBER_OF_TOURS

         if (isTour) {
            sb.append(Long.toString(1));
         } else {
            sb.append(Long.toString(tviItem.colCounter));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TOUR_TYPE_ID

         if (isTour) {
            sb.append(tviTour.getTourTypeId());
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TOUR_TYPE_NAME

         if (isTour) {
            final long tourTypeId = tviTour.getTourTypeId();
            sb.append(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TITLE

         final String dbTourTitle = tviItem.colTourTitle;
         if (dbTourTitle != null) {
            sb.append(dbTourTitle);
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TOUR_TAGS

         if (isTour) {
            sb.append(TourDatabase.getTagNames(tviTour.getTagIds()));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TOUR_NUMBER_OF_MARKER

         if (isTour) {
            final ArrayList<Long> markerIds = tviTour.getMarkerIds();
            if (markerIds != null) {
               sb.append(Integer.toString(markerIds.size()));
            }
         }
         sb.append(UI.TAB);
      }

      { // HEADER_TOUR_NUMBER_OF_PHOTOS

         final long numberOfPhotos = tviItem.colNumberOfPhotos;
         if (numberOfPhotos != 0) {
            sb.append(Long.toString(numberOfPhotos));
         }

         sb.append(UI.TAB);
      }
   }

   private void export_140_Motion(final StringBuilder sb,
                                  final boolean isTour,
                                  final TVITourBookItem tviItem) {

//    // Motion / Bewegung
//    defineColumn_Motion_Distance();
//    defineColumn_Motion_MaxSpeed();
//    defineColumn_Motion_AvgSpeed();
//    defineColumn_Motion_AvgPace();

//    csvField_Str(sb, String.format(  HEADER_MOTION_DISTANCE,            UI.UNIT_LABEL_DISTANCE));
//    csvField_Str(sb, String.format(  HEADER_MOTION_MAX_SPEED,           UI.UNIT_LABEL_SPEED));
//    csvField_Str(sb, String.format(  HEADER_MOTION_AVERAGE_SPEED,       UI.UNIT_LABEL_SPEED));
//    csvField_Str(sb, String.format(  HEADER_MOTION_AVERAGE_PACE,        UI.UNIT_LABEL_PACE));

      { // HEADER_DISTANCE

         final float dbDistance = tviItem.colTourDistance;
         if (dbDistance != 0) {
            sb.append(_nf1.format(dbDistance / 1000 / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_MAX_SPEED

         final float dbMaxSpeed = tviItem.colMaxSpeed;
         if (dbMaxSpeed != 0) {
            sb.append(_nf1.format(dbMaxSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_AVERAGE_SPEED

         final float speed = tviItem.colAvgSpeed / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
         if (speed != 0) {
            sb.append(_nf1.format(speed));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_AVERAGE_PACE

         final float pace = tviItem.colAvgPace * net.tourbook.ui.UI.UNIT_VALUE_DISTANCE;
         if (pace != 0) {
            sb.append(net.tourbook.common.UI.format_mm_ss((long) pace));
         }
         sb.append(UI.TAB);
      }
   }

   private void export_160_Elevation(final StringBuilder sb,
                                     final boolean isTour,
                                     final TVITourBookItem tviItem) {

//    // Elevation
//    defineColumn_Elevation_Up();
//    defineColumn_Elevation_Down();
//    defineColumn_Elevation_Max();
//    defineColumn_Elevation_AvgChange();
//
//    csvField_Str(sb, String.format(  HEADER_ELEVATION_UP,               UI.UNIT_LABEL_ALTITUDE));
//    csvField_Str(sb, String.format(  HEADER_ELEVATION_DOWN,             UI.UNIT_LABEL_ALTITUDE));
//    csvField_Str(sb, String.format(  HEADER_ELEVATION_MAX,              UI.UNIT_LABEL_ALTITUDE));

      { // HEADER_ALTITUDE_UP

         final long dbAltitudeUp = tviItem.colAltitudeUp;
         if (dbAltitudeUp != 0) {
            sb.append(Long.toString((long) (dbAltitudeUp / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_ALTITUDE_DOWN

         final long dbAltitudeDown = tviItem.colAltitudeDown;
         if (dbAltitudeDown != 0) {
            sb.append(Long.toString((long) (-dbAltitudeDown / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_MAX_ALTITUDE

         final long dbMaxAltitude = tviItem.colMaxAltitude;
         if (dbMaxAltitude != 0) {
            sb.append(Long.toString((long) (dbMaxAltitude / net.tourbook.ui.UI.UNIT_VALUE_ALTITUDE)));
         }
         sb.append(UI.TAB);
      }
   }

   private void export_180_Weather(final StringBuilder sb,
                                   final boolean isTour,
                                   final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    // Weather
//    defineColumn_Weather_Clouds();
//    defineColumn_Weather_Temperature_Avg();
//    defineColumn_Weather_Temperature_Min();
//    defineColumn_Weather_Temperature_Max();
//    defineColumn_Weather_WindSpeed();
//    defineColumn_Weather_WindDirection();

//    csvField_Str(sb,                 HEADER_WEATHER_CLOUDS);
//    csvField_Str(sb, String.format(  HEADER_WEATHER_TEMPERATURE_AVERAGE, UI.UNIT_LABEL_TEMPERATURE));
//    csvField_Str(sb,                 HEADER_WEATHER_WIND_SPEED);
//    csvField_Str(sb,                 HEADER_WEATHER_WIND_DIRECTION);

      { // HEADER_WEATHER_CLOUDS

         if (isTour) {
            final String windClouds = tviTour.colClouds;
            if (windClouds != null) {
               sb.append(windClouds);
            }

         }
         sb.append(UI.TAB);
      }

      { // HEADER_WEATHER_TEMPERATURE_AVERAGE

         float temperature = tviItem.colTemperature_Avg;

         if (temperature != 0) {
            if (net.tourbook.ui.UI.UNIT_VALUE_TEMPERATURE != 1) {
               temperature = temperature
                     * net.tourbook.ui.UI.UNIT_FAHRENHEIT_MULTI
                     + net.tourbook.ui.UI.UNIT_FAHRENHEIT_ADD;
            }
            sb.append(_nf1.format(temperature));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_WEATHER_WIND_SPEED

         final int windSpeed = (int) (tviItem.colWindSpd / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE);
         if (windSpeed != 0) {
            sb.append(Integer.toString(windSpeed));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_WEATHER_WIND_DIRECTION

         if (isTour) {
            final int windDir = tviItem.colWindDir;
            if (windDir != 0) {
               sb.append(Integer.toString(windDir));
            }
         }
         sb.append(UI.TAB);
      }
   }

   private void export_200_Body(final StringBuilder sb,
                                final boolean isTour,
                                final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    // Body
//    defineColumn_Body_Calories();
//    defineColumn_Body_RestPulse();
//    defineColumn_Body_MaxPulse();
//    defineColumn_Body_AvgPulse();
//    defineColumn_Body_Weight();
//    defineColumn_Body_Person();

//    csvField_Str(sb,                 HEADER_BODY_CALORIES);
//    csvField_Str(sb,                 HEADER_BODY_RESTPULSE);
//    csvField_Str(sb,                 HEADER_BODY_MAX_PULSE);
//    csvField_Str(sb,  String.format( HEADER_BODY_AVERAGE_PULSE, GRAPH_LABEL_HEARTBEAT_UNIT));
//    csvField_Str(sb,                 HEADER_BODY_PERSON);

      { // HEADER_BODY_CALORIES

         final double calories = tviItem.colCalories / 1000;
         if (calories != 0) {
            sb.append(_nf3.format(calories));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_BODY_RESTPULSE

         if (isTour) {
            final int restPulse = tviItem.colRestPulse;
            if (restPulse != 0) {
               sb.append(Integer.toString(restPulse));
            }
         }
         sb.append(UI.TAB);
      }

      { // HEADER_BODY_MAX_PULSE

         if (isTour) {
            final long dbMaxPulse = tviItem.colMaxPulse;
            if (dbMaxPulse != 0) {
               sb.append(Long.toString(dbMaxPulse));
            }
         }
         sb.append(UI.TAB);
      }

      { // HEADER_BODY_AVERAGE_PULSE

         final float pulse = tviItem.colAvgPulse;
         if (pulse != 0) {
            sb.append(_nf1.format(pulse));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_BODY_PERSON

         if (isTour) {
            final long dbPersonId = tviTour.colPersonId;
            sb.append(PersonManager.getPersonName(dbPersonId));
         }
         sb.append(UI.TAB);
      }
   }

   private void export_220_Power(final StringBuilder sb,
                                 final boolean isTour,
                                 final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

      // Power - Leistung
//    defineColumn_Power_Avg();
//    defineColumn_Power_Max();
//    defineColumn_Power_Normalized();
//    defineColumn_Power_TotalWork();

   }

   private void export_240_Powertrain(final StringBuilder sb,
                                      final boolean isTour,
                                      final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    // Powertrain - Antrieb/Pedal
//    defineColumn_Powertrain_AvgCadence();
//    defineColumn_Powertrain_SlowVsFastCadencePercentage();
//    defineColumn_Powertrain_SlowVsFastCadenceZonesDelimiter();
//    defineColumn_Powertrain_CadenceMultiplier();
//    defineColumn_Powertrain_Gear_FrontShiftCount();
//    defineColumn_Powertrain_Gear_RearShiftCount();
//    defineColumn_Powertrain_AvgLeftPedalSmoothness();
//    defineColumn_Powertrain_AvgLeftTorqueEffectiveness();
//    defineColumn_Powertrain_AvgRightPedalSmoothness();
//    defineColumn_Powertrain_AvgRightTorqueEffectiveness();
//    defineColumn_Powertrain_PedalLeftRightBalance();

//    csvField_Str(sb, HEADER_POWERTRAIN_AVERAGE_CADENCE);
//    csvField_Str(sb, HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT);
//    csvField_Str(sb, HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT);

      { // HEADER_POWERTRAIN_AVERAGE_CADENCE

         final float dbAvgCadence = tviItem.colAvgCadence;
         if (dbAvgCadence != 0) {
            sb.append(_nf1.format(dbAvgCadence));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT

         final long shiftCount = tviItem.colFrontShiftCount;
         sb.append(Long.toString(shiftCount));

         sb.append(UI.TAB);
      }

      { // HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT

         final long shiftCount = tviItem.colRearShiftCount;
         sb.append(Long.toString(shiftCount));

         sb.append(UI.TAB);
      }
   }

   private void export_260_Training(final StringBuilder sb,
                                    final boolean isTour,
                                    final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    // Training - Trainingsanalyse
//    defineColumn_Training_FTP();
//    defineColumn_Training_PowerToWeightRatio();
//    defineColumn_Training_IntensityFactor();
//    defineColumn_Training_StressScore();
//    defineColumn_Training_TrainingEffect();
//    defineColumn_Training_TrainingEffect_Anaerobic();
//    defineColumn_Training_TrainingPerformance();

   }

   private void export_280_RunningDynamics(final StringBuilder sb,
                                           final boolean isTour,
                                           final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//  // Running dynamics
//  defineColumn_RunDyn_StanceTime_Min();
//  defineColumn_RunDyn_StanceTime_Max();
//  defineColumn_RunDyn_StanceTime_Avg();

//  defineColumn_RunDyn_StanceTimeBalance_Min();
//  defineColumn_RunDyn_StanceTimeBalance_Max();
//  defineColumn_RunDyn_StanceTimeBalance_Avg();

//  defineColumn_RunDyn_StepLength_Min();
//  defineColumn_RunDyn_StepLength_Max();
//  defineColumn_RunDyn_StepLength_Avg();

//  defineColumn_RunDyn_VerticalOscillation_Min();
//  defineColumn_RunDyn_VerticalOscillation_Max();
//  defineColumn_RunDyn_VerticalOscillation_Avg();

//  defineColumn_RunDyn_VerticalRatio_Min();
//  defineColumn_RunDyn_VerticalRatio_Max();
//  defineColumn_RunDyn_VerticalRatio_Avg();

   }

   private void export_300_Surfing(final StringBuilder sb,
                                   final boolean isTour,
                                   final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    // Surfing
//    defineColumn_Surfing_NumberOfEvents();
//    defineColumn_Surfing_MinSpeed_StartStop();
//    defineColumn_Surfing_MinSpeed_Surfing();
//    defineColumn_Surfing_MinTimeDuration();
//    defineColumn_Surfing_MinDistance();

   }

   private void export_320_Device(final StringBuilder sb,
                                  final boolean isTour,
                                  final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    // Device
//    defineColumn_Device_Name();
//    defineColumn_Device_Distance();

//    csvField_Str(sb,                 HEADER_DEVICE_START_DISTANCE);

      { // HEADER_DEVICE_START_DISTANCE

         if (isTour) {
            final long dbStartDistance = tviTour.getColumnStartDistance();
            if (dbStartDistance != 0) {
               sb.append(Long.toString((long) (dbStartDistance / net.tourbook.ui.UI.UNIT_VALUE_DISTANCE)));
            }
         }
         sb.append(UI.TAB);
      }
   }

   private void export_340_Data(final StringBuilder sb,
                                final boolean isTour,
                                final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {

         tviTour = (TVITourBookTour) tviItem;
      }

//    // Data
//    defineColumn_Data_DPTolerance();
//    defineColumn_Data_ImportFilePath();
//    defineColumn_Data_ImportFileName();
//    defineColumn_Data_TimeInterval();
//    defineColumn_Data_NumTimeSlices();

//    csvField_Str(sb,                 HEADER_DATA_DP_TOLERANCE);
//    csvField_Str(sb,                 HEADER_DATA_TIME_SLICES);
//    csvField_Str(sb,                 HEADER_DATA_TIME_INTERVAL);

      { // HEADER_DATA_DP_TOLERANCE

         if (isTour) {
            final int dpTolerance = tviItem.colDPTolerance;
            if (dpTolerance != 0) {
               sb.append(_nf1.format(dpTolerance / 10.0));
            }
         }
         sb.append(UI.TAB);
      }

      { // HEADER_DATA_TIME_SLICES

         final long numberOfTimeSlices = tviItem.colNumberOfTimeSlices;
         if (numberOfTimeSlices != 0) {
            sb.append(Long.toString(numberOfTimeSlices));
         }
         sb.append(UI.TAB);
      }

      { // HEADER_DATA_TIME_INTERVAL

         if (isTour) {
            final short dbTimeInterval = tviTour.getColumnTimeInterval();
            if (dbTimeInterval != 0) {
               sb.append(Long.toString(dbTimeInterval));
            }
         }
         sb.append(UI.TAB);
      }

   }

   private void exportCSV_010_Header(final Writer exportWriter, final StringBuilder sb) throws IOException {

// SET_FORMATTING_OFF

//    // Time
//    defineColumn_1stColumn_Date();
//    defineColumn_Time_WeekDay();
//    defineColumn_Time_TourStartTime();
//    defineColumn_Time_TimeZoneDifference();
//    defineColumn_Time_TimeZone();
//    defineColumn_Time_MovingTime();
//    defineColumn_Time_RecordingTime();
//    defineColumn_Time_PausedTime();
//    defineColumn_Time_PausedTime_Relative();
//    defineColumn_Time_WeekNo();
//    defineColumn_Time_WeekYear();

      // Year
      csvField_Str(sb, HEADER_TIME_YEAR);

      // Month or Week
      if (isYearSubWeek()) {

         // week / month

         csvField_Str(sb,              HEADER_TIME_WEEK);
         csvField_Str(sb,              HEADER_TIME_MONTH);

      } else {

         // month / week

         csvField_Str(sb,              HEADER_TIME_MONTH);
         csvField_Str(sb,              HEADER_TIME_WEEK);
      }

      csvField_Str(sb,                 HEADER_TIME_DAY);
      csvField_Str(sb,                 HEADER_TIME_TOUR_START_TIME);
      csvField_Str(sb,                 HEADER_TIME_ISO_DATE_TIME);
      csvField_Str(sb,                 HEADER_TIME_WEEKDAY);
      csvField_Str(sb,                 HEADER_TIME_WEEK_YEAR);

      csvField_Str(sb, String.format(  HEADER_TIME_RECORDING_TIME,        Messages.App_Unit_Seconds_Small));
      csvField_Str(sb, String.format(  HEADER_TIME_MOVING_TIME,           Messages.App_Unit_Seconds_Small));
      csvField_Str(sb, String.format(  HEADER_TIME_PAUSED_TIME,           Messages.App_Unit_Seconds_Small));
      csvField_Str(sb,                 HEADER_TIME_PAUSED_TIME_RELATIVE);
      csvField_Str(sb, String.format(  HEADER_TIME_RECORDING_TIME,        CSV_EXPORT_DURATION_HHH_MM_SS));
      csvField_Str(sb, String.format(  HEADER_TIME_MOVING_TIME,           CSV_EXPORT_DURATION_HHH_MM_SS));
      csvField_Str(sb, String.format(  HEADER_TIME_PAUSED_TIME,           CSV_EXPORT_DURATION_HHH_MM_SS));

//    // Tour
//    defineColumn_Tour_TypeImage();
//    defineColumn_Tour_TypeText();
//    defineColumn_Tour_Title();
//    defineColumn_Tour_Marker();
//    defineColumn_Tour_Photos();
//    defineColumn_Tour_Tags();
//    defineColumn_Tour_Location_Start();
//    defineColumn_Tour_Location_End();
////  defineColumn_Tour_TagIds();            // for debugging

      csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_TOURS);
      csvField_Str(sb,                 HEADER_TOUR_TYPE_ID);
      csvField_Str(sb,                 HEADER_TOUR_TYPE_NAME);
      csvField_Str(sb,                 HEADER_TOUR_TITLE);
      csvField_Str(sb,                 HEADER_TOUR_TAGS);
      csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_MARKER);
      csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_PHOTOS);

//    // Motion / Bewegung
//    defineColumn_Motion_Distance();
//    defineColumn_Motion_MaxSpeed();
//    defineColumn_Motion_AvgSpeed();
//    defineColumn_Motion_AvgPace();

      csvField_Str(sb, String.format(  HEADER_MOTION_DISTANCE,            UI.UNIT_LABEL_DISTANCE));
      csvField_Str(sb, String.format(  HEADER_MOTION_SPEED_MAX,           UI.UNIT_LABEL_SPEED));
      csvField_Str(sb, String.format(  HEADER_MOTION_SPEED_AVERAGE,       UI.UNIT_LABEL_SPEED));
      csvField_Str(sb, String.format(  HEADER_MOTION_PACE_AVERAGE,        UI.UNIT_LABEL_PACE));

//    // Elevation
//    defineColumn_Elevation_Up();
//    defineColumn_Elevation_Down();
//    defineColumn_Elevation_Max();
//    defineColumn_Elevation_AvgChange();

      csvField_Str(sb, String.format(  HEADER_ELEVATION_UP,               UI.UNIT_LABEL_ALTITUDE));
      csvField_Str(sb, String.format(  HEADER_ELEVATION_DOWN,             UI.UNIT_LABEL_ALTITUDE));
      csvField_Str(sb, String.format(  HEADER_ELEVATION_MAX,              UI.UNIT_LABEL_ALTITUDE));

//    // Weather
//    defineColumn_Weather_Clouds();
//    defineColumn_Weather_Temperature_Avg();
//    defineColumn_Weather_Temperature_Min();
//    defineColumn_Weather_Temperature_Max();
//    defineColumn_Weather_WindSpeed();
//    defineColumn_Weather_WindDirection();

      csvField_Str(sb,                 HEADER_WEATHER_CLOUDS);
      csvField_Str(sb, String.format(  HEADER_WEATHER_TEMPERATURE_AVERAGE,UI.UNIT_LABEL_TEMPERATURE));
      csvField_Str(sb,                 HEADER_WEATHER_WIND_SPEED);
      csvField_Str(sb,                 HEADER_WEATHER_WIND_DIRECTION);

//    // Body
//    defineColumn_Body_Calories();
//    defineColumn_Body_RestPulse();
//    defineColumn_Body_MaxPulse();
//    defineColumn_Body_AvgPulse();
//    defineColumn_Body_Weight();
//    defineColumn_Body_Person();

      csvField_Str(sb,                 HEADER_BODY_CALORIES);
      csvField_Str(sb,                 HEADER_BODY_RESTPULSE);
      csvField_Str(sb,                 HEADER_BODY_MAX_PULSE);
      csvField_Str(sb,  String.format( HEADER_BODY_AVERAGE_PULSE,         GRAPH_LABEL_HEARTBEAT_UNIT));
      csvField_Str(sb,                 HEADER_BODY_PERSON);

//    // Power - Leistung
//    defineColumn_Power_Avg();
//    defineColumn_Power_Max();
//    defineColumn_Power_Normalized();
//    defineColumn_Power_TotalWork();

//    // Powertrain - Antrieb/Pedal
//    defineColumn_Powertrain_AvgCadence();
//    defineColumn_Powertrain_SlowVsFastCadencePercentage();
//    defineColumn_Powertrain_SlowVsFastCadenceZonesDelimiter();
//    defineColumn_Powertrain_CadenceMultiplier();
//    defineColumn_Powertrain_Gear_FrontShiftCount();
//    defineColumn_Powertrain_Gear_RearShiftCount();
//    defineColumn_Powertrain_AvgLeftPedalSmoothness();
//    defineColumn_Powertrain_AvgLeftTorqueEffectiveness();
//    defineColumn_Powertrain_AvgRightPedalSmoothness();
//    defineColumn_Powertrain_AvgRightTorqueEffectiveness();
//    defineColumn_Powertrain_PedalLeftRightBalance();

      csvField_Str(sb,                 HEADER_POWERTRAIN_AVERAGE_CADENCE);
      csvField_Str(sb,                 HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT);
      csvField_Str(sb,                 HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT);

//    // Training - Trainingsanalyse
//    defineColumn_Training_FTP();
//    defineColumn_Training_PowerToWeightRatio();
//    defineColumn_Training_IntensityFactor();
//    defineColumn_Training_StressScore();
//    defineColumn_Training_TrainingEffect();
//    defineColumn_Training_TrainingEffect_Anaerobic();
//    defineColumn_Training_TrainingPerformance();

//    // Running dynamics
//    defineColumn_RunDyn_StanceTime_Min();
//    defineColumn_RunDyn_StanceTime_Max();
//    defineColumn_RunDyn_StanceTime_Avg();

//    defineColumn_RunDyn_StanceTimeBalance_Min();
//    defineColumn_RunDyn_StanceTimeBalance_Max();
//    defineColumn_RunDyn_StanceTimeBalance_Avg();

//    defineColumn_RunDyn_StepLength_Min();
//    defineColumn_RunDyn_StepLength_Max();
//    defineColumn_RunDyn_StepLength_Avg();

//    defineColumn_RunDyn_VerticalOscillation_Min();
//    defineColumn_RunDyn_VerticalOscillation_Max();
//    defineColumn_RunDyn_VerticalOscillation_Avg();

//    defineColumn_RunDyn_VerticalRatio_Min();
//    defineColumn_RunDyn_VerticalRatio_Max();
//    defineColumn_RunDyn_VerticalRatio_Avg();

//    // Surfing
//    defineColumn_Surfing_NumberOfEvents();
//    defineColumn_Surfing_MinSpeed_StartStop();
//    defineColumn_Surfing_MinSpeed_Surfing();
//    defineColumn_Surfing_MinTimeDuration();
//    defineColumn_Surfing_MinDistance();

//    // Device
//    defineColumn_Device_Name();
//    defineColumn_Device_Distance();

      csvField_Str(sb,                 HEADER_DEVICE_START_DISTANCE);

//    // Data
//    defineColumn_Data_DPTolerance();
//    defineColumn_Data_ImportFilePath();
//    defineColumn_Data_ImportFileName();
//    defineColumn_Data_TimeInterval();
//    defineColumn_Data_NumTimeSlices();

      csvField_Str(sb,                 HEADER_DATA_DP_TOLERANCE);
      csvField_Str(sb,                 HEADER_DATA_TIME_SLICES);
      csvField_Str(sb,                 HEADER_DATA_TIME_INTERVAL);

// SET_FORMATTING_ON

      // end of line
      sb.append(net.tourbook.ui.UI.SYSTEM_NEW_LINE);

      exportWriter.write(sb.toString());
   }

   private void exportCSV_020_DateColumns(final StringBuilder sb,
                                          final int segmentCount,
                                          final Object segment,
                                          final boolean isTour) {

      if (segment instanceof TVITourBookYear) {

         final TVITourBookYear tviYear = (TVITourBookYear) segment;

         // year
         csvField_Int(sb, tviYear.tourYear);

         if (segmentCount == 1) {

            for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
               sb.append(UI.TAB);
            }
         }

      } else if (segment instanceof TVITourBookYearSub) {

         final TVITourBookYearSub tviYearSub = (TVITourBookYearSub) segment;

         // month or week
         csvField_Int(sb, tviYearSub.tourYearSub);

         if (segmentCount == 2) {

            for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
               sb.append(UI.TAB);
            }
         }

      } else if (isTour) {

         final TVITourBookTour tviTour = (TVITourBookTour) segment;

         if (isYearSubWeek()) {

            // month
            csvField_Int(sb, tviTour.tourMonth);

         } else {

            // week
            csvField_Int(sb, tviTour.tourWeek);
         }

         // day
         csvField_Int(sb, tviTour.tourDay);
      }
   }

   /**
    * @return Returns <code>true</code> when the year subcategory is week, otherwise it is month.
    */
   private boolean isYearSubWeek() {

      return _tourBookView.getYearSubCategory() == YearSubCategory.WEEK;
   }
}
