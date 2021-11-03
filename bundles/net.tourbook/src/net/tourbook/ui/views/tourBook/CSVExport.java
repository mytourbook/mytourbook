/*******************************************************************************
 * Copyright (C) 2020, 2021 Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourData;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;

public class CSVExport {

   private static char         SEPARATOR;

   private static final String NL                                                     = net.tourbook.ui.UI.SYSTEM_NEW_LINE;

   private static final String CSV_EXPORT_DURATION_HHH_MM_SS                          = "hhh:mm:ss";                                        //$NON-NLS-1$

   private static final String HEADER_BODY_MAX_PULSE                                  = "BODY Pulse max (bpm)";                             //$NON-NLS-1$
   private static final String HEADER_BODY_AVERAGE_PULSE                              = "BODY Pulse avg (bpm)";                             //$NON-NLS-1$
   private static final String HEADER_BODY_CALORIES                                   = "BODY Calories (kcal)";                             //$NON-NLS-1$
   private static final String HEADER_BODY_PERSON                                     = "BODY Person";                                      //$NON-NLS-1$
   private static final String HEADER_BODY_WEIGHT                                     = "BODY Weight (%s)";                                 //$NON-NLS-1$
   private static final String HEADER_BODY_RESTPULSE                                  = "BODY Restpulse (bpm)";                             //$NON-NLS-1$

   private static final String HEADER_DATA_DP_TOLERANCE                               = "DATA Douglas Peuker Tolerance";                    //$NON-NLS-1$
   private static final String HEADER_DATA_IMPORT_FILE_NAME                           = "DATA Import Filename";                             //$NON-NLS-1$
   private static final String HEADER_DATA_IMPORT_FILE_PATH                           = "DATA Import Filepath";                             //$NON-NLS-1$
   private static final String HEADER_DATA_TIME_INTERVAL                              = "DATA Time Interval";                               //$NON-NLS-1$
   private static final String HEADER_DATA_TIME_SLICES                                = "DATA Number of Time Slices";                       //$NON-NLS-1$

   private static final String HEADER_DEVICE_NAME                                     = "DEVICE Name";                                      //$NON-NLS-1$
   private static final String HEADER_DEVICE_START_DISTANCE                           = "DEVICE Start distance";                            //$NON-NLS-1$

   private static final String HEADER_ELEVATION_AVERAGE_CHANGE                        = "ELEVATION Average change (%s)";                    //$NON-NLS-1$
   private static final String HEADER_ELEVATION_UP                                    = "ELEVATION Elevation up (%s)";                      //$NON-NLS-1$
   private static final String HEADER_ELEVATION_DOWN                                  = "ELEVATION Elevation down (%s)";                    //$NON-NLS-1$
   private static final String HEADER_ELEVATION_MAX                                   = "ELEVATION Elevation max (%s)";                     //$NON-NLS-1$

   private static final String HEADER_MOTION_DISTANCE                                 = "MOTION Distance (%s)";                             //$NON-NLS-1$
   private static final String HEADER_MOTION_PACE_AVERAGE                             = "MOTION Pace avg (%s)";                             //$NON-NLS-1$
   private static final String HEADER_MOTION_SPEED_AVERAGE                            = "MOTION Speed avg (%s)";                            //$NON-NLS-1$
   private static final String HEADER_MOTION_SPEED_MAX                                = "MOTION Speed max (%s)";                            //$NON-NLS-1$

   private static final String HEADER_POWER_AVG                                       = "POWER Avg (W)";                                    //$NON-NLS-1$
   private static final String HEADER_POWER_MAX                                       = "POWER Max (W)";                                    //$NON-NLS-1$
   private static final String HEADER_POWER_NORMALIZED                                = "POWER Normalized (W)";                             //$NON-NLS-1$
   private static final String HEADER_POWER_TOTAL_WORK                                = "POWER Total work (MJ)";                            //$NON-NLS-1$

   private static final String HEADER_POWERTRAIN_AVERAGE_CADENCE                      = "POWERTRAIN Cadence - Average (rpm)";               //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS            = "POWERTRAIN Left Pedal Smoothness - LPS (%)";       //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS        = "POWERTRAIN Left Torque Effectiveness - LTE (%)";   //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS           = "POWERTRAIN Right Pedal Smoothness - RPS (%)";      //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS       = "POWERTRAIN Right Torque Effectiveness - RTE (%s)"; //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_CADENCE_MULTIPLIER                   = "POWERTRAIN Cadence Multiplier (rpm/spm)";          //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT               = "POWERTRAIN Front Gear Shifts";                     //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT                = "POWERTRAIN Rear Gear Shifts";                      //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE             = "POWERTRAIN Left Right Balance";                    //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES     = "POWERTRAIN Cadence - Slow vs Fast (rpm)";          //$NON-NLS-1$
   private static final String HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER = "POWERTRAIN Cadence - Zones Delimiter (%)";         //$NON-NLS-1$

   private static final String HEADER_RUN_DYN_STANCE_TIME_MIN                         = "RUNDYN Stance Time - Minimum (ms)";                //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STANCE_TIME_MAX                         = "RUNDYN Stance Time - Maximum (ms)";                //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STANCE_TIME_AVG                         = "RUNDYN Stance Time - Average (ms)";                //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STANCE_TIME_BALANCE_MIN                 = "RUNDYN Stance Time Balance - Minimum (%)";         //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STANCE_TIME_BALANCE_MAX                 = "RUNDYN Stance Time Balance - Maximum (%)";         //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STANCE_TIME_BALANCE_AVG                 = "RUNDYN Stance Time Balance - Average (%)";         //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STEP_LENGTH_MIN                         = "RUNDYN Step Length - Minimum (%s)";                //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STEP_LENGTH_MAX                         = "RUNDYN Step Length - Maximum (%s)";                //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_STEP_LENGTH_AVG                         = "RUNDYN Step Length - Average (%s)";                //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_VERTICAL_OSCILLATION_MIN                = "RUNDYN Vertical Oscillation - Minimum (%s)";       //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_VERTICAL_OSCILLATION_MAX                = "RUNDYN Vertical Oscillation - Maximum (%s)";       //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_VERTICAL_OSCILLATION_AVG                = "RUNDYN Vertical Oscillation - Average (%s)";       //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_VERTICAL_RATIO_MIN                      = "RUNDYN Vertical Ratio - Minimum (%)";              //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_VERTICAL_RATIO_MAX                      = "RUNDYN Vertical Ratio - Maximum (%)";              //$NON-NLS-1$
   private static final String HEADER_RUN_DYN_VERTICAL_RATIO_AVG                      = "RUNDYN Vertical Ratio - Average (%)";              //$NON-NLS-1$

   private static final String HEADER_SURFING_MIN_DISTANCE                            = "SURFING Surfing distance - Minimum (%s)";          //$NON-NLS-1$
   private static final String HEADER_SURFING_MIN_SPEED_START_STOP                    = "SURFING Start/stop surfing speed - Minimum (%s)";  //$NON-NLS-1$
   private static final String HEADER_SURFING_MIN_SPEED_SURFING                       = "SURFING Surfing speed - Minimum (%s)";             //$NON-NLS-1$
   private static final String HEADER_SURFING_MIN_TIME_DURATION                       = "SURFING Surfing duration - Minimum (sec)";         //$NON-NLS-1$
   private static final String HEADER_SURFING_NUMBER_OF_EVENTS                        = "SURFING Number of surfing events";                 //$NON-NLS-1$

   private static final String HEADER_TIME_DEVICE_ELAPSED_TIME                        = "TIME Elapsed time (%s)";                           //$NON-NLS-1$
   private static final String HEADER_TIME_DEVICE_RECORDED_TIME                       = "TIME Recorded time (%s)";                          //$NON-NLS-1$
   private static final String HEADER_TIME_DEVICE_PAUSED_TIME                         = "TIME Paused time (%s)";                            //$NON-NLS-1$
   private static final String HEADER_TIME_COMPUTED_MOVING_TIME                       = "TIME Moving time (%s)";                            //$NON-NLS-1$
   private static final String HEADER_TIME_COMPUTED_BREAK_TIME                        = "TIME Break time (%s)";                             //$NON-NLS-1$
   private static final String HEADER_TIME_COMPUTED_BREAK_TIME_RELATIVE               = "TIME Relative break time (%)";                     //$NON-NLS-1$

   private static final String HEADER_TIME_DAY                                        = "TIME Day";                                         //$NON-NLS-1$
   private static final String HEADER_TIME_ISO_DATE_TIME                              = "TIME ISO8601";                                     //$NON-NLS-1$
   private static final String HEADER_TIME_MONTH                                      = "TIME Month";                                       //$NON-NLS-1$
   private static final String HEADER_TIME_TOUR_START_TIME                            = "TIME Tour start time";                             //$NON-NLS-1$
   private static final String HEADER_TIME_WEEK                                       = "TIME Week";                                        //$NON-NLS-1$
   private static final String HEADER_TIME_WEEK_YEAR                                  = "TIME Week year";                                   //$NON-NLS-1$
   private static final String HEADER_TIME_WEEKDAY                                    = "TIME Weekday";                                     //$NON-NLS-1$
   private static final String HEADER_TIME_YEAR                                       = "TIME Year";                                        //$NON-NLS-1$

   private static final String HEADER_TOUR_LOCATION_START                             = "TOUR Start Location";                              //$NON-NLS-1$
   private static final String HEADER_TOUR_LOCATION_END                               = "TOUR End Location";                                //$NON-NLS-1$
   private static final String HEADER_TOUR_NUMBER_OF_MARKER                           = "TOUR Number of markers";                           //$NON-NLS-1$
   private static final String HEADER_TOUR_NUMBER_OF_PHOTOS                           = "TOUR Number of photos";                            //$NON-NLS-1$
   private static final String HEADER_TOUR_NUMBER_OF_TOURS                            = "TOUR Number of tours";                             //$NON-NLS-1$
   private static final String HEADER_TOUR_TAGS                                       = "TOUR Tags";                                        //$NON-NLS-1$
   private static final String HEADER_TOUR_TITLE                                      = "TOUR Title";                                       //$NON-NLS-1$
   private static final String HEADER_TOUR_TYPE_ID                                    = "TOUR Tour type ID";                                //$NON-NLS-1$
   private static final String HEADER_TOUR_TYPE_NAME                                  = "TOUR Tour type name";                              //$NON-NLS-1$

   private static final String HEADER_TRAINING_FTP                                    = "TRAINING Functional Threshold Power - FTP (W)";    //$NON-NLS-1$
   private static final String HEADER_TRAINING_INTENSITY_FACTOR                       = "TRAINING Intensity Factor - IF";                   //$NON-NLS-1$
   private static final String HEADER_TRAINING_POWER_TO_WEIGHT                        = "TRAINING Power to Weight (W/Kg)";                  //$NON-NLS-1$
   private static final String HEADER_TRAINING_STRESS_SCORE                           = "TRAINING Training Stress Score - TSS";             //$NON-NLS-1$
   private static final String HEADER_TRAINING_TRAINING_EFFECT_AEROB                  = "TRAINING Training effect aerob";                   //$NON-NLS-1$
   private static final String HEADER_TRAINING_TRAINING_EFFECT_ANAEROB                = "TRAINING Training effect anaerob";                 //$NON-NLS-1$
   private static final String HEADER_TRAINING_TRAINING_PERFORMANCE                   = "TRAINING Training performance";                    //$NON-NLS-1$

   private static final String HEADER_WEATHER_CLOUDS                                  = "WEATHER Clouds";                                   //$NON-NLS-1$
   private static final String HEADER_WEATHER_TEMPERATURE_AVERAGE                     = "WEATHER Temperature avg (%s)";                     //$NON-NLS-1$
   private static final String HEADER_WEATHER_TEMPERATURE_MIN                         = "WEATHER Temperature min (%s)";                     //$NON-NLS-1$
   private static final String HEADER_WEATHER_TEMPERATURE_MAX                         = "WEATHER Temperature max (%s)";                     //$NON-NLS-1$
   private static final String HEADER_WEATHER_WIND_DIRECTION                          = "WEATHER Wind direction";                           //$NON-NLS-1$
   private static final String HEADER_WEATHER_WIND_SPEED                              = "WEATHER Wind speed (%s)";                          //$NON-NLS-1$

   private static NumberFormat _nf0;
   private static NumberFormat _nf1;
   private static NumberFormat _nf2;
   private static NumberFormat _nf3;

   static {
      _nf0 = NumberFormat.getNumberInstance();
      _nf0.setMinimumFractionDigits(0);
      _nf0.setMaximumFractionDigits(0);
      _nf0.setGroupingUsed(false);

      _nf1 = NumberFormat.getNumberInstance();
      _nf1.setMinimumFractionDigits(1);
      _nf1.setMaximumFractionDigits(1);
      _nf1.setGroupingUsed(false);

      _nf2 = NumberFormat.getNumberInstance();
      _nf2.setMinimumFractionDigits(2);
      _nf2.setMaximumFractionDigits(2);
      _nf2.setGroupingUsed(false);

      _nf3 = NumberFormat.getNumberInstance();
      _nf3.setMinimumFractionDigits(3);
      _nf3.setMaximumFractionDigits(3);
      _nf3.setGroupingUsed(false);
   }

   /**
    * This is a copy from net.tourbook.device.csv.tours.CSVTourDataReader.TOUR_CSV_ID_3
    */
   private static final String TOUR_CSV_ID_3 = UI.EMPTY_STRING

         + "Date (yyyy-mm-dd); "                              //$NON-NLS-1$
         + "Time (hh-mm); "                                   //$NON-NLS-1$
         + "Duration (sec); "                                 //$NON-NLS-1$
         + "Paused Time (sec); "                              //$NON-NLS-1$
         + "Distance (m); "                                   //$NON-NLS-1$
         + "Title; "                                          //$NON-NLS-1$
         + "Comment; "                                        //$NON-NLS-1$
         + "Tour Type; "                                      //$NON-NLS-1$
         + "Tags; "                                           //$NON-NLS-1$
         + "Altitude Up (m); "                                //$NON-NLS-1$
         + "Altitude Down (m);";                              //$NON-NLS-1$

   private TourBookView        _tourBookView;

   /**
    * Write selected items into a csv file
    *
    * @param selection
    * @param selectedFilePath
    * @param tourBookView
    * @param isUseSimpleCSVFormat
    *           When <code>true</code> then the CSVTourDataReader can read the exported file,
    *           otherwise all values are exported
    */
   CSVExport(final ISelection selection,
             final String selectedFilePath,
             final TourBookView tourBookView,
             final boolean isUseSimpleCSVFormat) {

      _tourBookView = tourBookView;

      if (isUseSimpleCSVFormat) {

         SEPARATOR = UI.SYMBOL_SEMICOLON;

         csvExport_SimpleFormat(selection, selectedFilePath);

      } else {

         SEPARATOR = UI.TAB;

         csvExport_DefaultFormat(selection, selectedFilePath);
      }

   }

   private void csvExport_DefaultFormat(final ISelection selection, final String selectedFilePath) {

      final boolean isFlatLayout = selection instanceof StructuredSelection;
      final boolean isTreeLayout = selection instanceof ITreeSelection;

      try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, UI.UTF_8);
            Writer exportWriter = new BufferedWriter(outputStreamWriter)) {

         final StringBuilder sb = new StringBuilder();

         export_100_Header_Time(sb, isTreeLayout);
         export_120_Header_Tour(sb);
         export_140_Header_Motion(sb);
         export_160_Header_Elevation(sb);
         export_180_Header_Weather(sb);
         export_200_Header_Body(sb);
         export_220_Header_Power(sb);
         export_240_Header_Powertrain(sb);
         export_260_Header_Training(sb);
         export_280_Header_RunningDynamics(sb);
         export_300_Header_Surfing(sb);
         export_320_Header_Device(sb);
         export_340_Header_Data(sb);

         // end of line
         sb.append(NL);

         exportWriter.write(sb.toString());

         if (isTreeLayout) {

            for (final TreePath treePath : ((ITreeSelection) selection).getPaths()) {

               // truncate buffer
               sb.setLength(0);

               final int numSegment = treePath.getSegmentCount();

               for (int segmentIndex = 0; segmentIndex < numSegment; segmentIndex++) {

                  final Object segment = treePath.getSegment(segmentIndex);
                  final boolean isTour = segment instanceof TVITourBookTour;

                  export_400_Value_DateColumns(sb, numSegment, segment, isTour);

                  if (segment instanceof TVITourBookItem) {

                     final TVITourBookItem tviItem = (TVITourBookItem) segment;

                     // output data only for the last segment
                     if (numSegment == 1
                           || (numSegment == 2 && segmentIndex == 1)
                           || (numSegment == 3 && segmentIndex == 2)) {

                        export_500_Value_Time(sb, isTour, tviItem);
                        export_520_Value_Tour(sb, isTour, tviItem);
                        export_540_Value_Motion(sb, tviItem);
                        export_560_Value_Elevation(sb, tviItem);
                        export_580_Value_Weather(sb, isTour, tviItem);
                        export_600_Value_Body(sb, isTour, tviItem);
                        export_620_Value_Power(sb, tviItem);
                        export_640_Value_Powertrain(sb, tviItem);
                        export_660_Value_Training(sb, tviItem);
                        export_680_Value_RunningDynamics(sb, tviItem);
                        export_700_Value_Surfing(sb, tviItem);
                        export_720_Value_Device(sb, isTour, tviItem);
                        export_740_Value_Data(sb, isTour, tviItem);
                     }
                  }
               }

               // end of line
               sb.append(NL);
               exportWriter.write(sb.toString());
            }

         } else if (isFlatLayout) {

            final StructuredSelection structuredSelection = (StructuredSelection) selection;

            for (final Object element : structuredSelection) {

               if (element instanceof TVITourBookTour) {

                  final TVITourBookItem tviItem = (TVITourBookItem) element;

                  // truncate buffer
                  sb.setLength(0);

                  final boolean isTour = true;

                  export_410_Value_DateColumns(sb, tviItem);

                  export_500_Value_Time(sb, isTour, tviItem);
                  export_520_Value_Tour(sb, isTour, tviItem);
                  export_540_Value_Motion(sb, tviItem);
                  export_560_Value_Elevation(sb, tviItem);
                  export_580_Value_Weather(sb, isTour, tviItem);
                  export_600_Value_Body(sb, isTour, tviItem);
                  export_620_Value_Power(sb, tviItem);
                  export_640_Value_Powertrain(sb, tviItem);
                  export_660_Value_Training(sb, tviItem);
                  export_680_Value_RunningDynamics(sb, tviItem);
                  export_700_Value_Surfing(sb, tviItem);
                  export_720_Value_Device(sb, isTour, tviItem);
                  export_740_Value_Data(sb, isTour, tviItem);

                  // end of line
                  sb.append(NL);
                  exportWriter.write(sb.toString());
               }
            }
         }

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }
   }

   private void csvExport_SimpleFormat(final ISelection selection, final String selectedFilePath) {

      try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, UI.UTF_8);
            Writer exportWriter = new BufferedWriter(outputStreamWriter)) {

         final StringBuilder sb = new StringBuilder();

         // Date (yyyy-mm-dd);
         // Time (hh-mm);
         // Duration (sec);
         // Paused Time (sec);
         // Distance (m);
         // Title;
         // Comment;
         // Tour Type;
         // Tags;
         // Altitude Up (m);
         // Altitude Down (m);
         sb.append(TOUR_CSV_ID_3);
         sb.append(NL);

         exportWriter.write(sb.toString());

         if (selection instanceof ITreeSelection) {

            for (final TreePath treePath : ((ITreeSelection) selection).getPaths()) {

               final int numSegment = treePath.getSegmentCount();

               for (int segmentIndex = 0; segmentIndex < numSegment; segmentIndex++) {

                  final Object segment = treePath.getSegment(segmentIndex);

                  if (segment instanceof TVITourBookTour) {

                     csvExport_SimpleFormat_Tour(exportWriter, (TVITourBookTour) segment);
                  }
               }
            }

         } else if (selection instanceof StructuredSelection) {

            for (final Object element : (StructuredSelection) selection) {

               if (element instanceof TVITourBookTour) {

                  csvExport_SimpleFormat_Tour(exportWriter, (TVITourBookTour) element);
               }
            }
         }

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }
   }

   private void csvExport_SimpleFormat_Tour(final Writer exportWriter, final TVITourBookTour tviTour) throws IOException {

      final StringBuilder sb = new StringBuilder();

      final TourDateTime colDateTime = tviTour.colTourDateTime;
      final ZonedDateTime tourZonedDateTime = colDateTime.tourZonedDateTime;

      final long tourTypeId = tviTour.getTourTypeId();
      final String tourTypeLabel = net.tourbook.ui.UI.getTourTypeLabel(tourTypeId);
      final String tagNames = TourDatabase.getTagNames(tviTour.getTagIds());

      sb.append(String.format(UI.EMPTY_STRING

            + "%04d-%02d-%02d;" //     date                    //$NON-NLS-1$
            + "%02d-%02d;" //          time                    //$NON-NLS-1$
            ,

            tourZonedDateTime.getYear(), //                    // Date (yyyy-mm-dd);
            tourZonedDateTime.getMonthValue(),
            tourZonedDateTime.getDayOfMonth(),

            tourZonedDateTime.getHour(), //                    // Time (hh-mm);
            tourZonedDateTime.getMinute() //
      ));

      csvField(sb, tviTour.colTourDeviceTime_Recorded); //     // Duration (sec);
      csvField(sb, tviTour.colTourComputedTime_Break); //      // Paused Time (sec);
      csvField(sb, tviTour.colTourDistance); //                // Distance (m);

      csvField(sb, tviTour.colTourTitle); //                   // Title;
      csvField(sb, UI.EMPTY_STRING); //                        // Comment; !!! THIS IS NOT YET SUPPORTED !!!
      csvField(sb, tourTypeLabel); //                          // Tour Type;
      csvField(sb, tagNames); //                               // Tags;

      csvField(sb, tviTour.colAltitudeUp); //                  // Altitude Up (m);
      csvField(sb, tviTour.colAltitudeDown); //                // Altitude Down (m);

      // end of line
      sb.append(NL);

      exportWriter.write(sb.toString());
   }

   private void csvField(final StringBuilder sb, final long fieldValue) {

      if (fieldValue != 0) {
         sb.append(fieldValue);
      }
      sb.append(SEPARATOR);
   }

   private void csvField(final StringBuilder sb, final String fieldValue) {

      if (fieldValue != null) {
         sb.append(fieldValue);
      }

      sb.append(SEPARATOR);
   }

   private void csvField_Nf0(final StringBuilder sb, final float fieldValue) {

      if (fieldValue != 0) {
         sb.append(_nf0.format(fieldValue));
      }
      sb.append(SEPARATOR);
   }

   private void csvField_Nf1(final StringBuilder sb, final float fieldValue) {

      if (fieldValue != 0) {
         sb.append(_nf1.format(fieldValue));
      }
      sb.append(SEPARATOR);
   }

   private void csvField_Nf2(final StringBuilder sb, final float fieldValue) {

      if (fieldValue != 0) {
         sb.append(_nf2.format(fieldValue));
      }
      sb.append(SEPARATOR);
   }

   private void csvHeader(final StringBuilder sb, final String fieldValue) {

      sb.append(fieldValue);
      sb.append(SEPARATOR);
   }

   private void export_100_Header_Time(final StringBuilder sb, final boolean isTreeLayout) {

// SET_FORMATTING_OFF

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
      csvHeader(sb,                 HEADER_TIME_YEAR);

      // Month / Day / Week
      if (isTreeLayout) {

         // the column sorting is according to the tree layout

         if (isYearSubWeek()) {

            // week / month

            csvHeader(sb,           HEADER_TIME_WEEK);
            csvHeader(sb,           HEADER_TIME_MONTH);

         } else {

            // month / week

            csvHeader(sb,           HEADER_TIME_MONTH);
            csvHeader(sb,           HEADER_TIME_WEEK);
         }

         csvHeader(sb,              HEADER_TIME_DAY);

      } else {

         // flat layout, the preferred columns are year/month/day/week

         csvHeader(sb,              HEADER_TIME_MONTH);
         csvHeader(sb,              HEADER_TIME_DAY);
         csvHeader(sb,              HEADER_TIME_WEEK);
      }

      csvHeader(sb,                 HEADER_TIME_TOUR_START_TIME);
      csvHeader(sb,                 HEADER_TIME_ISO_DATE_TIME);
      csvHeader(sb,                 HEADER_TIME_WEEKDAY);
      csvHeader(sb,                 HEADER_TIME_WEEK_YEAR);

      csvHeader(sb, String.format(  HEADER_TIME_DEVICE_ELAPSED_TIME,             Messages.App_Unit_Seconds_Small));
      csvHeader(sb, String.format(  HEADER_TIME_DEVICE_RECORDED_TIME,            Messages.App_Unit_Seconds_Small));
      csvHeader(sb, String.format(  HEADER_TIME_DEVICE_PAUSED_TIME,              Messages.App_Unit_Seconds_Small));
      csvHeader(sb, String.format(  HEADER_TIME_COMPUTED_MOVING_TIME,            Messages.App_Unit_Seconds_Small));
      csvHeader(sb, String.format(  HEADER_TIME_COMPUTED_BREAK_TIME,             Messages.App_Unit_Seconds_Small));
      csvHeader(sb,                 HEADER_TIME_COMPUTED_BREAK_TIME_RELATIVE);
      csvHeader(sb, String.format(  HEADER_TIME_DEVICE_ELAPSED_TIME,             CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(sb, String.format(  HEADER_TIME_DEVICE_RECORDED_TIME,            CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(sb, String.format(  HEADER_TIME_DEVICE_PAUSED_TIME,              CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(sb, String.format(  HEADER_TIME_COMPUTED_MOVING_TIME,            CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(sb, String.format(  HEADER_TIME_COMPUTED_BREAK_TIME,             CSV_EXPORT_DURATION_HHH_MM_SS));

// SET_FORMATTING_ON

   }

   private void export_120_Header_Tour(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Tour_TypeImage();
//    defineColumn_Tour_TypeText();
//    defineColumn_Tour_Title();
//    defineColumn_Tour_Marker();
//    defineColumn_Tour_Photos();
//    defineColumn_Tour_Tags();
//    defineColumn_Tour_Location_Start();
//    defineColumn_Tour_Location_End();
////  defineColumn_Tour_TagIds();            // for debugging

      csvHeader(sb,                 HEADER_TOUR_NUMBER_OF_TOURS);
      csvHeader(sb,                 HEADER_TOUR_TYPE_ID);
      csvHeader(sb,                 HEADER_TOUR_TYPE_NAME);
      csvHeader(sb,                 HEADER_TOUR_TITLE);
      csvHeader(sb,                 HEADER_TOUR_LOCATION_START);
      csvHeader(sb,                 HEADER_TOUR_LOCATION_END);
      csvHeader(sb,                 HEADER_TOUR_TAGS);
      csvHeader(sb,                 HEADER_TOUR_NUMBER_OF_MARKER);
      csvHeader(sb,                 HEADER_TOUR_NUMBER_OF_PHOTOS);

// SET_FORMATTING_ON

   }

   private void export_140_Header_Motion(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Motion_Distance();
//    defineColumn_Motion_MaxSpeed();
//    defineColumn_Motion_AvgSpeed();
//    defineColumn_Motion_AvgPace();

      csvHeader(sb, String.format(  HEADER_MOTION_DISTANCE,             UI.UNIT_LABEL_DISTANCE));
      csvHeader(sb, String.format(  HEADER_MOTION_SPEED_MAX,            UI.UNIT_LABEL_SPEED));
      csvHeader(sb, String.format(  HEADER_MOTION_SPEED_AVERAGE,        UI.UNIT_LABEL_SPEED));
      csvHeader(sb, String.format(  HEADER_MOTION_PACE_AVERAGE,         UI.UNIT_LABEL_PACE));

// SET_FORMATTING_ON

   }

   private void export_160_Header_Elevation(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Elevation_Up();
//    defineColumn_Elevation_Down();
//    defineColumn_Elevation_Max();
//    defineColumn_Elevation_AvgChange();

      final String avgEle_UnitLabel = UI.SYMBOL_AVERAGE + UI.SPACE + UI.UNIT_LABEL_ELEVATION + "/" + UI.UNIT_LABEL_DISTANCE; //$NON-NLS-1$

      csvHeader(sb, String.format(  HEADER_ELEVATION_UP,                UI.UNIT_LABEL_ELEVATION));
      csvHeader(sb, String.format(  HEADER_ELEVATION_DOWN,              UI.UNIT_LABEL_ELEVATION));
      csvHeader(sb, String.format(  HEADER_ELEVATION_MAX,               UI.UNIT_LABEL_ELEVATION));
      csvHeader(sb, String.format(  HEADER_ELEVATION_AVERAGE_CHANGE,    avgEle_UnitLabel));

// SET_FORMATTING_ON

   }

   private void export_180_Header_Weather(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Weather_Clouds();
//    defineColumn_Weather_Temperature_Avg();
//    defineColumn_Weather_Temperature_Min();
//    defineColumn_Weather_Temperature_Max();
//    defineColumn_Weather_WindSpeed();
//    defineColumn_Weather_WindDirection();

      csvHeader(sb,                 HEADER_WEATHER_CLOUDS);
      csvHeader(sb, String.format(  HEADER_WEATHER_TEMPERATURE_AVERAGE, UI.UNIT_LABEL_TEMPERATURE));
      csvHeader(sb, String.format(  HEADER_WEATHER_TEMPERATURE_MIN,     UI.UNIT_LABEL_TEMPERATURE));
      csvHeader(sb, String.format(  HEADER_WEATHER_TEMPERATURE_MAX,     UI.UNIT_LABEL_TEMPERATURE));
      csvHeader(sb, String.format(  HEADER_WEATHER_WIND_SPEED,          UI.UNIT_LABEL_SPEED));
      csvHeader(sb, String.format(  HEADER_WEATHER_WIND_DIRECTION,      UI.UNIT_LABEL_DIRECTION));

// SET_FORMATTING_ON

   }

   private void export_200_Header_Body(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Body_Calories();
//    defineColumn_Body_RestPulse();
//    defineColumn_Body_MaxPulse();
//    defineColumn_Body_AvgPulse();
//    defineColumn_Body_Weight();
//    defineColumn_Body_Person();

      csvHeader(sb,                 HEADER_BODY_CALORIES);
      csvHeader(sb,                 HEADER_BODY_RESTPULSE);
      csvHeader(sb,                 HEADER_BODY_MAX_PULSE);
      csvHeader(sb,                 HEADER_BODY_AVERAGE_PULSE);
      csvHeader(sb, String.format(  HEADER_BODY_WEIGHT,                 UI.UNIT_LABEL_WEIGHT));
      csvHeader(sb,                 HEADER_BODY_PERSON);

// SET_FORMATTING_ON

   }

   private void export_220_Header_Power(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Power_Avg();
//    defineColumn_Power_Max();
//    defineColumn_Power_Normalized();
//    defineColumn_Power_TotalWork();

      csvHeader(sb,                 HEADER_POWER_AVG);
      csvHeader(sb,                 HEADER_POWER_MAX);
      csvHeader(sb,                 HEADER_POWER_NORMALIZED);
      csvHeader(sb,                 HEADER_POWER_TOTAL_WORK);

// SET_FORMATTING_ON

   }

   private void export_240_Header_Powertrain(final StringBuilder sb) {

// SET_FORMATTING_OFF

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

      csvHeader(sb,                 HEADER_POWERTRAIN_AVERAGE_CADENCE);
      csvHeader(sb,                 HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES);
      csvHeader(sb,                 HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER);
      csvHeader(sb,                 HEADER_POWERTRAIN_CADENCE_MULTIPLIER);
      csvHeader(sb,                 HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT);
      csvHeader(sb,                 HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT);
      csvHeader(sb,                 HEADER_POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS);
      csvHeader(sb,                 HEADER_POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS);
      csvHeader(sb,                 HEADER_POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS);
      csvHeader(sb,                 HEADER_POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS);
      csvHeader(sb,                 HEADER_POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE);

// SET_FORMATTING_ON

   }

   private void export_260_Header_Training(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Training_FTP();
//    defineColumn_Training_PowerToWeightRatio();
//    defineColumn_Training_IntensityFactor();
//    defineColumn_Training_StressScore();
//    defineColumn_Training_TrainingEffect();
//    defineColumn_Training_TrainingEffect_Anaerobic();
//    defineColumn_Training_TrainingPerformance();

      csvHeader(sb,                 HEADER_TRAINING_FTP);
      csvHeader(sb,                 HEADER_TRAINING_POWER_TO_WEIGHT);
      csvHeader(sb,                 HEADER_TRAINING_INTENSITY_FACTOR);
      csvHeader(sb,                 HEADER_TRAINING_STRESS_SCORE);
      csvHeader(sb,                 HEADER_TRAINING_TRAINING_EFFECT_AEROB);
      csvHeader(sb,                 HEADER_TRAINING_TRAINING_EFFECT_ANAEROB);
      csvHeader(sb,                 HEADER_TRAINING_TRAINING_PERFORMANCE);

// SET_FORMATTING_ON

   }

   private void export_280_Header_RunningDynamics(final StringBuilder sb) {

// SET_FORMATTING_OFF

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

      csvHeader(sb,                 HEADER_RUN_DYN_STANCE_TIME_MIN);
      csvHeader(sb,                 HEADER_RUN_DYN_STANCE_TIME_MAX);
      csvHeader(sb,                 HEADER_RUN_DYN_STANCE_TIME_AVG);

      csvHeader(sb,                 HEADER_RUN_DYN_STANCE_TIME_BALANCE_MIN);
      csvHeader(sb,                 HEADER_RUN_DYN_STANCE_TIME_BALANCE_MAX);
      csvHeader(sb,                 HEADER_RUN_DYN_STANCE_TIME_BALANCE_AVG);

      csvHeader(sb, String.format(  HEADER_RUN_DYN_STEP_LENGTH_MIN,              UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(sb, String.format(  HEADER_RUN_DYN_STEP_LENGTH_MAX,              UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(sb, String.format(  HEADER_RUN_DYN_STEP_LENGTH_AVG,              UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));

      csvHeader(sb, String.format(  HEADER_RUN_DYN_VERTICAL_OSCILLATION_MIN,     UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(sb, String.format(  HEADER_RUN_DYN_VERTICAL_OSCILLATION_MAX,     UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(sb, String.format(  HEADER_RUN_DYN_VERTICAL_OSCILLATION_AVG,     UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));

      csvHeader(sb,                 HEADER_RUN_DYN_VERTICAL_RATIO_MIN);
      csvHeader(sb,                 HEADER_RUN_DYN_VERTICAL_RATIO_MAX);
      csvHeader(sb,                 HEADER_RUN_DYN_VERTICAL_RATIO_AVG);

// SET_FORMATTING_ON

   }

   private void export_300_Header_Surfing(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Surfing_NumberOfEvents();
//    defineColumn_Surfing_MinSpeed_StartStop();
//    defineColumn_Surfing_MinSpeed_Surfing();
//    defineColumn_Surfing_MinTimeDuration();
//    defineColumn_Surfing_MinDistance();

      csvHeader(sb,                 HEADER_SURFING_NUMBER_OF_EVENTS);
      csvHeader(sb, String.format(  HEADER_SURFING_MIN_SPEED_START_STOP,   UI.UNIT_LABEL_SPEED));
      csvHeader(sb, String.format(  HEADER_SURFING_MIN_SPEED_SURFING,      UI.UNIT_LABEL_SPEED));
      csvHeader(sb,                 HEADER_SURFING_MIN_TIME_DURATION);
      csvHeader(sb, String.format(  HEADER_SURFING_MIN_DISTANCE,           UI.UNIT_LABEL_DISTANCE_M_OR_YD));

// SET_FORMATTING_ON

   }

   private void export_320_Header_Device(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Device_Name();
//    defineColumn_Device_Distance();

      csvHeader(sb,                 HEADER_DEVICE_NAME);
      csvHeader(sb,                 HEADER_DEVICE_START_DISTANCE);

// SET_FORMATTING_ON

   }

   private void export_340_Header_Data(final StringBuilder sb) {

// SET_FORMATTING_OFF

//    defineColumn_Data_DPTolerance();
//    defineColumn_Data_ImportFilePath();
//    defineColumn_Data_ImportFileName();
//    defineColumn_Data_TimeInterval();
//    defineColumn_Data_NumTimeSlices();

      csvHeader(sb,                 HEADER_DATA_DP_TOLERANCE);
      csvHeader(sb,                 HEADER_DATA_IMPORT_FILE_NAME);
      csvHeader(sb,                 HEADER_DATA_IMPORT_FILE_PATH);
      csvHeader(sb,                 HEADER_DATA_TIME_INTERVAL);
      csvHeader(sb,                 HEADER_DATA_TIME_SLICES);

// SET_FORMATTING_ON

   }

   private void export_400_Value_DateColumns(final StringBuilder sb,
                                             final int segmentCount,
                                             final Object segment,
                                             final boolean isTour) {

      if (segment instanceof TVITourBookYear) {

         final TVITourBookYear tviYear = (TVITourBookYear) segment;

         // year
         csvField(sb, tviYear.tourYear);

         if (segmentCount == 1) {

            for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
               sb.append(SEPARATOR);
            }
         }

      } else if (segment instanceof TVITourBookYearCategorized) {

         final TVITourBookYearCategorized tviYearSub = (TVITourBookYearCategorized) segment;

         // month or week
         csvField(sb, tviYearSub.tourYearSub);

         if (segmentCount == 2) {

            for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
               sb.append(SEPARATOR);
            }
         }

      } else if (isTour) {

         final TVITourBookTour tviTour = (TVITourBookTour) segment;

         if (isYearSubWeek()) {

            // month
            csvField(sb, tviTour.tourMonth);

         } else {

            // week
            csvField(sb, tviTour.colWeekNo);
         }

         // day
         csvField(sb, tviTour.tourDay);
      }
   }

   private void export_410_Value_DateColumns(final StringBuilder sb, final TVITourBookItem tviItem) {

      csvField(sb, tviItem.tourYear); // year
      csvField(sb, tviItem.tourMonth); // month
      csvField(sb, tviItem.tourDay); // day

      csvField(sb, tviItem.colWeekNo); // week
   }

   private void export_500_Value_Time(final StringBuilder sb,
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

      { // HEADER_TIME_TOUR_START_TIME

         if (isTour) {
            sb.append(tourStartDateTime.format(TimeTools.Formatter_Time_M));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TIME_ISO_DATE_TIME

         if (isTour) {
            sb.append(tourStartDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TIME_WEEKDAY

         if (isTour) {
            sb.append(tourDateTime.weekDay);
         }
         sb.append(SEPARATOR);
      }

      csvField(sb, tviItem.colWeekYear); // HEADER_TIME_WEEK_YEAR

      /////////////////////////////////////////////////////////////////////////////////////////////

      csvField(sb, tviItem.colTourDeviceTime_Elapsed); // HEADER_TIME_ELAPSED_TIME
      csvField(sb, tviItem.colTourDeviceTime_Recorded); // HEADER_TIME_RECORDED_TIME
      csvField(sb, tviItem.colTourDeviceTime_Paused); // HEADER_TIME_PAUSED_TIME
      csvField(sb, tviItem.colTourComputedTime_Moving); // HEADER_TIME_MOVING_TIME
      csvField(sb, tviItem.colTourComputedTime_Break); // HEADER_TIME_BREAK_TIME

      { // HEADER_TIME_BREAK_TIME_RELATIVE

         final long colBreakTime = tviItem.colTourComputedTime_Break;
         final long dbPausedTime = colBreakTime;
         final long dbElapsedTime = tviItem.colTourDeviceTime_Elapsed;
         final float relativePausedTime = dbElapsedTime == 0 //
               ? 0
               : (float) dbPausedTime / dbElapsedTime * 100;
         if (relativePausedTime != 0) {
            sb.append(_nf1.format(relativePausedTime));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TIME_ELAPSED_TIME hhh:mm:ss

         final long colElapsedTime = (tviItem).colTourDeviceTime_Elapsed;
         if (colElapsedTime != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colElapsedTime));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TIME_RECORDED_TIME hhh:mm:ss

         final long colTourDeviceTime_Recorded = (tviItem).colTourDeviceTime_Recorded;
         if (colTourDeviceTime_Recorded != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colTourDeviceTime_Recorded));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TIME_PAUSED_TIME hhh:mm:ss

         final long colTourDeviceTime_Paused = (tviItem).colTourDeviceTime_Paused;
         if (colTourDeviceTime_Paused != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colTourDeviceTime_Paused));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TIME_MOVING_TIME hhh:mm:ss

         final long colMovingTime = tviItem.colTourComputedTime_Moving;
         if (colMovingTime != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colMovingTime));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TIME_BREAK_TIME hhh:mm:ss

         final long colBreakTime = tviItem.colTourComputedTime_Break;
         if (colBreakTime != 0) {
            sb.append(net.tourbook.common.UI.format_hh_mm_ss(colBreakTime));
         }
         sb.append(SEPARATOR);
      }
   }

   private void export_520_Value_Tour(final StringBuilder sb,
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
//    csvField_Str(sb,                 HEADER_TOUR_LOCATION_START);
//    csvField_Str(sb,                 HEADER_TOUR_LOCATION_END);
//    csvField_Str(sb,                 HEADER_TOUR_TAGS);
//    csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_MARKER);
//    csvField_Str(sb,                 HEADER_TOUR_NUMBER_OF_PHOTOS);

      { // HEADER_TOUR_NUMBER_OF_TOURS

         if (isTour) {
            sb.append(Long.toString(1));
         } else {
            sb.append(Long.toString(tviItem.colCounter));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TOUR_TYPE_ID

         if (isTour) {
            sb.append(tviTour.getTourTypeId());
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TOUR_TYPE_NAME

         if (isTour) {
            final long tourTypeId = tviTour.getTourTypeId();
            sb.append(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
         }
         sb.append(SEPARATOR);
      }

      csvField(sb, tviItem.colTourTitle); // HEADER_TOUR_TITLE
      csvField(sb, tviItem.colTourLocation_Start); // HEADER_TOUR_LOCATION_START
      csvField(sb, tviItem.colTourLocation_End); // HEADER_TOUR_LOCATION_END

      { // HEADER_TOUR_TAGS

         if (isTour) {
            sb.append(TourDatabase.getTagNames(tviTour.getTagIds()));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TOUR_NUMBER_OF_MARKER

         if (isTour) {
            final ArrayList<Long> markerIds = tviTour.getMarkerIds();
            if (markerIds != null) {
               sb.append(Integer.toString(markerIds.size()));
            }
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_TOUR_NUMBER_OF_PHOTOS

         final long numberOfPhotos = tviItem.colNumberOfPhotos;
         if (numberOfPhotos != 0) {
            sb.append(Long.toString(numberOfPhotos));
         }

         sb.append(SEPARATOR);
      }
   }

   private void export_540_Value_Motion(final StringBuilder sb,
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
            sb.append(_nf1.format(dbDistance / 1000 / UI.UNIT_VALUE_DISTANCE));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_MAX_SPEED

         final float dbMaxSpeed = tviItem.colMaxSpeed;
         if (dbMaxSpeed != 0) {
            sb.append(_nf1.format(dbMaxSpeed / UI.UNIT_VALUE_DISTANCE));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_AVERAGE_SPEED

         final float speed = tviItem.colAvgSpeed / UI.UNIT_VALUE_DISTANCE;
         if (speed != 0) {
            sb.append(_nf1.format(speed));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_AVERAGE_PACE

         final float pace = tviItem.colAvgPace * UI.UNIT_VALUE_DISTANCE;
         if (pace != 0) {
            sb.append(net.tourbook.common.UI.format_mm_ss((long) pace));
         }
         sb.append(SEPARATOR);
      }
   }

   private void export_560_Value_Elevation(final StringBuilder sb,
                                           final TVITourBookItem tviItem) {

//    // Elevation
//    defineColumn_Elevation_Up();
//    defineColumn_Elevation_Down();
//    defineColumn_Elevation_Max();
//    defineColumn_Elevation_AvgChange();
//
//    csvField_Str(sb, String.format(  HEADER_ELEVATION_UP,                UI.UNIT_LABEL_ELEVATION));
//    csvField_Str(sb, String.format(  HEADER_ELEVATION_DOWN,              UI.UNIT_LABEL_ELEVATION));
//    csvField_Str(sb, String.format(  HEADER_ELEVATION_MAX,               UI.UNIT_LABEL_ELEVATION));
//    csvField_Str(sb, String.format(  HEADER_ELEVATION_AVERAGE_CHANGE,    avgEle_UnitLabel));

      { // HEADER_ALTITUDE_UP

         final long dbAltitudeUp = tviItem.colAltitudeUp;
         if (dbAltitudeUp != 0) {
            sb.append(Long.toString((long) (dbAltitudeUp / UI.UNIT_VALUE_ELEVATION)));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_ALTITUDE_DOWN

         final long dbAltitudeDown = tviItem.colAltitudeDown;
         if (dbAltitudeDown != 0) {
            sb.append(Long.toString((long) (-dbAltitudeDown / UI.UNIT_VALUE_ELEVATION)));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_MAX_ALTITUDE

         final long dbMaxAltitude = tviItem.colMaxAltitude;
         if (dbMaxAltitude != 0) {
            sb.append(Long.toString((long) (dbMaxAltitude / UI.UNIT_VALUE_ELEVATION)));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_ELEVATION_AVERAGE_CHANGE

         final double dbValue = UI.convertAverageElevationChangeFromMetric((tviItem).colAltitude_AvgChange);

         if (dbValue != 0) {
            sb.append(_nf0.format(dbValue));
         }
         sb.append(SEPARATOR);
      }
   }

   private void export_580_Value_Weather(final StringBuilder sb,
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
//    csvField_Str(sb, String.format(  HEADER_WEATHER_TEMPERATURE_MIN,     UI.UNIT_LABEL_TEMPERATURE));
//    csvField_Str(sb, String.format(  HEADER_WEATHER_TEMPERATURE_MAX,     UI.UNIT_LABEL_TEMPERATURE));
//    csvField_Str(sb,                 HEADER_WEATHER_WIND_SPEED);
//    csvField_Str(sb,                 HEADER_WEATHER_WIND_DIRECTION);

      { // HEADER_WEATHER_CLOUDS

         if (isTour) {
            final String windClouds = tviTour.colClouds;
            if (windClouds != null) {
               sb.append(windClouds);
            }

         }
         sb.append(SEPARATOR);
      }

      { // HEADER_WEATHER_TEMPERATURE_AVERAGE

         final float dbValue = tviItem.colTemperature_Avg;

         if (dbValue != 0) {
            sb.append(_nf1.format(UI.convertTemperatureFromMetric(dbValue)));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_WEATHER_TEMPERATURE_MIN

         final float dbValue = tviItem.colTemperature_Min;

         if (dbValue != 0) {
            sb.append(_nf1.format(UI.convertTemperatureFromMetric(dbValue)));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_WEATHER_TEMPERATURE_MAX

         final float dbValue = tviItem.colTemperature_Max;

         if (dbValue != 0) {
            sb.append(_nf1.format(UI.convertTemperatureFromMetric(dbValue)));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_WEATHER_WIND_SPEED

         final int windSpeed = (int) (tviItem.colWindSpd / UI.UNIT_VALUE_DISTANCE);
         if (windSpeed != 0) {
            sb.append(Integer.toString(windSpeed));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_WEATHER_WIND_DIRECTION

         if (isTour) {
            final int windDir = tviItem.colWindDir;
            if (windDir != 0) {
               sb.append(Integer.toString(windDir));
            }
         }
         sb.append(SEPARATOR);
      }
   }

   private void export_600_Value_Body(final StringBuilder sb,
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
//    csvField_Str(sb,                 HEADER_BODY_WEIGHT);
//    csvField_Str(sb,                 HEADER_BODY_PERSON);

      { // HEADER_BODY_CALORIES

         final double calories = tviItem.colCalories / 1000.0;
         if (calories != 0) {
            sb.append(_nf3.format(calories));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_BODY_RESTPULSE

         if (isTour) {
            final int restPulse = tviItem.colRestPulse;
            if (restPulse != 0) {
               sb.append(Integer.toString(restPulse));
            }
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_BODY_MAX_PULSE

         if (isTour) {
            final long dbMaxPulse = tviItem.colMaxPulse;
            if (dbMaxPulse != 0) {
               sb.append(Long.toString(dbMaxPulse));
            }
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_BODY_AVERAGE_PULSE

         final float pulse = tviItem.colAvgPulse;
         if (pulse != 0) {
            sb.append(_nf1.format(pulse));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_BODY_WEIGHT

         if (isTour) {
            final double dbValue = UI.convertBodyWeightFromMetric((tviItem).colBodyWeight);
            if (dbValue != 0) {
               sb.append(_nf1.format(dbValue));
            }
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_BODY_PERSON

         if (isTour) {
            final long dbPersonId = tviTour.colPersonId;
            sb.append(PersonManager.getPersonName(dbPersonId));
         }
         sb.append(SEPARATOR);
      }
   }

   private void export_620_Value_Power(final StringBuilder sb,
                                       final TVITourBookItem tviItem) {

//    // Power - Leistung
//    defineColumn_Power_Avg();
//    defineColumn_Power_Max();
//    defineColumn_Power_Normalized();
//    defineColumn_Power_TotalWork();

// SET_FORMATTING_OFF

      csvField_Nf1(sb, tviItem.colPower_Avg);                     // HEADER_POWER_AVG
      csvField_Nf1(sb, tviItem.colPower_Max);                     // HEADER_POWER_MAX
      csvField_Nf1(sb, tviItem.colPower_Normalized);              // HEADER_POWER_NORMALIZED
      csvField_Nf1(sb, tviItem.colPower_TotalWork / 1000_000f);   // HEADER_POWER_TOTAL_WORK

// SET_FORMATTING_ON
   }

   private void export_640_Value_Powertrain(final StringBuilder sb,
                                            final TVITourBookItem tviItem) {

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

// SET_FORMATTING_OFF

      csvField_Nf1(  sb, tviItem.colAvgCadence);                        // HEADER_POWERTRAIN_AVERAGE_CADENCE
      csvField(      sb, tviItem.colSlowVsFastCadence);                 // HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES
      csvField_Nf0(  sb, tviItem.colCadenceZonesDelimiter);             // HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER
      csvField_Nf1(  sb, tviItem.colCadenceMultiplier);                 // HEADER_POWERTRAIN_CADENCE_MULTIPLIER
      csvField(      sb, tviItem.colFrontShiftCount);                   // HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT
      csvField(      sb, tviItem.colRearShiftCount);                    // HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT
      csvField_Nf1(  sb, tviItem.colPower_AvgLeftPedalSmoothness);      // HEADER_POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS
      csvField_Nf1(  sb, tviItem.colPower_AvgLeftTorqueEffectiveness);  // HEADER_POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS
      csvField_Nf1(  sb, tviItem.colPower_AvgRightPedalSmoothness);     // HEADER_POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS
      csvField_Nf1(  sb, tviItem.colPower_AvgRightTorqueEffectiveness); // HEADER_POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS
      csvField(      sb, tviItem.colPower_PedalLeftRightBalance);       // HEADER_POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE

// SET_FORMATTING_ON
   }

   private void export_660_Value_Training(final StringBuilder sb,
                                          final TVITourBookItem tviItem) {

//    // Training - Trainingsanalyse
//    defineColumn_Training_FTP();
//    defineColumn_Training_PowerToWeightRatio();
//    defineColumn_Training_IntensityFactor();
//    defineColumn_Training_StressScore();
//    defineColumn_Training_TrainingEffect();
//    defineColumn_Training_TrainingEffect_Anaerobic();
//    defineColumn_Training_TrainingPerformance();

// SET_FORMATTING_OFF

      csvField(      sb, tviItem.colTraining_FTP);                      // HEADER_TRAINING_FTP
      csvField_Nf2(  sb, tviItem.colTraining_PowerToWeight);            // HEADER_TRAINING_POWER_TO_WEIGHT
      csvField_Nf2(  sb, tviItem.colTraining_IntensityFactor);          // HEADER_TRAINING_INTENSITY_FACTOR
      csvField_Nf1(  sb, tviItem.colTraining_TrainingStressScore);      // HEADER_TRAINING_STRESS_SCORE
      csvField_Nf1(  sb, tviItem.colTraining_TrainingEffect_Aerob);     // HEADER_TRAINING_TRAINING_EFFECT_AEROB
      csvField_Nf1(  sb, tviItem.colTraining_TrainingEffect_Anaerobic); // HEADER_TRAINING_TRAINING_EFFECT_ANAEROB
      csvField_Nf2(  sb, tviItem.colTraining_TrainingPerformance);      // HEADER_TRAINING_TRAINING_PERFORMANCE

// SET_FORMATTING_ON
   }

   private void export_680_Value_RunningDynamics(final StringBuilder sb,
                                                 final TVITourBookItem tviItem) {

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

// SET_FORMATTING_OFF

      csvField_Nf1(sb, tviItem.colRunDyn_StanceTime_Min);            // HEADER_RUN_DYN_STANCE_TIME_MIN
      csvField_Nf1(sb, tviItem.colRunDyn_StanceTime_Max);            // HEADER_RUN_DYN_STANCE_TIME_MAX
      csvField_Nf1(sb, tviItem.colRunDyn_StanceTime_Avg);            // HEADER_RUN_DYN_STANCE_TIME_AVG

      csvField_Nf1(sb, tviItem.colRunDyn_StanceTimeBalance_Min);     // HEADER_RUN_DYN_STANCE_TIME_BALANCE_MIN
      csvField_Nf1(sb, tviItem.colRunDyn_StanceTimeBalance_Max);     // HEADER_RUN_DYN_STANCE_TIME_BALANCE_MAX
      csvField_Nf1(sb, tviItem.colRunDyn_StanceTimeBalance_Avg);     // HEADER_RUN_DYN_STANCE_TIME_BALANCE_AVG

      csvField_Nf1(sb, tviItem.colRunDyn_StepLength_Min);            // HEADER_RUN_DYN_STEP_LENGTH_MIN
      csvField_Nf1(sb, tviItem.colRunDyn_StepLength_Max);            // HEADER_RUN_DYN_STEP_LENGTH_MAX
      csvField_Nf1(sb, tviItem.colRunDyn_StepLength_Avg);            // HEADER_RUN_DYN_STEP_LENGTH_AVG

      csvField_Nf1(sb, tviItem.colRunDyn_VerticalOscillation_Min);   // HEADER_RUN_DYN_VERTICAL_OSCILLATION_MIN
      csvField_Nf1(sb, tviItem.colRunDyn_VerticalOscillation_Max);   // HEADER_RUN_DYN_VERTICAL_OSCILLATION_MAX
      csvField_Nf1(sb, tviItem.colRunDyn_VerticalOscillation_Avg);   // HEADER_RUN_DYN_VERTICAL_OSCILLATION_AVG

      csvField_Nf1(sb, tviItem.colRunDyn_VerticalRatio_Min);         // HEADER_RUN_DYN_VERTICAL_RATIO_MIN
      csvField_Nf1(sb, tviItem.colRunDyn_VerticalRatio_Max);         // HEADER_RUN_DYN_VERTICAL_RATIO_MAX
      csvField_Nf1(sb, tviItem.colRunDyn_VerticalRatio_Avg);         // HEADER_RUN_DYN_VERTICAL_RATIO_AVG

// SET_FORMATTING_ON
   }

   private void export_700_Value_Surfing(final StringBuilder sb,
                                         final TVITourBookItem tviItem) {

//    // Surfing
//    defineColumn_Surfing_NumberOfEvents();
//    defineColumn_Surfing_MinSpeed_StartStop();
//    defineColumn_Surfing_MinSpeed_Surfing();
//    defineColumn_Surfing_MinTimeDuration();
//    defineColumn_Surfing_MinDistance();

//    csvField_Str(sb,                 HEADER_SURFING_NUMBER_OF_EVENTS);
//    csvField_Str(sb,                 HEADER_SURFING_MIN_SPEED_START_STOP);
//    csvField_Str(sb,                 HEADER_SURFING_MIN_SPEED_SURFING);
//    csvField_Str(sb,                 HEADER_SURFING_MIN_TIME_DURATION);
//    csvField_Str(sb,                 HEADER_SURFING_MIN_DISTANCE);

      { // HEADER_SURFING_NUMBER_OF_EVENTS

         final long dbValue = tviItem.col_Surfing_NumberOfEvents;

         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
            sb.append(Long.toString(dbValue));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_SURFING_MIN_SPEED_START_STOP

         final long dbValue = tviItem.col_Surfing_MinSpeed_StartStop;

         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
            sb.append(Long.toString(dbValue));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_SURFING_MIN_SPEED_SURFING

         final long dbValue = tviItem.col_Surfing_MinSpeed_Surfing;

         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
            sb.append(Long.toString(dbValue));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_SURFING_MIN_TIME_DURATION

         final long dbValue = tviItem.col_Surfing_MinTimeDuration;

         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
            sb.append(Long.toString(dbValue));
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_SURFING_MIN_DISTANCE

         final short value = tviItem.col_Surfing_MinDistance;
         final boolean isMinDistance = tviItem.col_Surfing_IsMinDistance;

         if (value != 0 && value != TourData.SURFING_VALUE_IS_NOT_SET && isMinDistance) {

            int minSurfingDistance = value;

            // convert imperial -> metric
            if (UI.UNIT_IS_LENGTH_YARD) {
               minSurfingDistance = (int) (minSurfingDistance / UI.UNIT_YARD + 0.5);
            }

            sb.append(Long.toString(minSurfingDistance));
         }
         sb.append(SEPARATOR);
      }
   }

   private void export_720_Value_Device(final StringBuilder sb,
                                        final boolean isTour,
                                        final TVITourBookItem tviItem) {

      TVITourBookTour tviTour = null;

      if (isTour) {
         tviTour = (TVITourBookTour) tviItem;
      }

//    // Device
//    defineColumn_Device_Name();
//    defineColumn_Device_Distance();

//    csvField_Str(sb,                 HEADER_DEVICE_NAME);
//    csvField_Str(sb,                 HEADER_DEVICE_START_DISTANCE);

      { // HEADER_DEVICE_NAME

         final String dbValue = tviItem.colDeviceName;
         if (dbValue != null) {
            sb.append(dbValue);
         }
         sb.append(SEPARATOR);
      }

      { // HEADER_DEVICE_START_DISTANCE

         if (isTour) {
            final long dbStartDistance = tviTour.colStartDistance;
            if (dbStartDistance != 0) {
               sb.append(Long.toString((long) (dbStartDistance / UI.UNIT_VALUE_DISTANCE)));
            }
         }
         sb.append(SEPARATOR);
      }
   }

   private void export_740_Value_Data(final StringBuilder sb,
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
//    csvField_Str(sb,                 HEADER_DATA_IMPORT_FILE_NAME);
//    csvField_Str(sb,                 HEADER_DATA_IMPORT_FILE_PATH);
//    csvField_Str(sb,                 HEADER_DATA_TIME_INTERVAL);
//    csvField_Str(sb,                 HEADER_DATA_TIME_SLICES);

      { // HEADER_DATA_DP_TOLERANCE

         if (isTour) {
            final int dpTolerance = tviItem.colDPTolerance;
            if (dpTolerance != 0) {
               sb.append(_nf1.format(dpTolerance / 10.0));
            }
         }
         sb.append(SEPARATOR);
      }

      csvField(sb, tviItem.col_ImportFileName); // HEADER_DATA_IMPORT_FILE_NAME
      csvField(sb, tviItem.col_ImportFilePath); // HEADER_DATA_IMPORT_FILE_PATH
      csvField(sb, tviItem.colNumberOfTimeSlices); // HEADER_DATA_TIME_SLICES

      { // HEADER_DATA_TIME_INTERVAL

         if (isTour) {
            final int dbValue = tviTour.colTimeInterval;
            if (dbValue != 0) {
               sb.append(Long.toString(dbValue));
            }
         }
         sb.append(SEPARATOR);
      }
   }

   /**
    * @return Returns <code>true</code> when the year subcategory is week, otherwise it is month.
    */
   private boolean isYearSubWeek() {

      return _tourBookView.getViewLayout() == TourBookViewLayout.CATEGORY_WEEK;
   }
}
