/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.ui.views.tourSegmenter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.ui.views.tourBook.TVITourBookItem;
import net.tourbook.ui.views.tourBook.TVITourBookTour;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;

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

   private TourSegmenterView   _tourSegmenterView;

   /**
    * Write selected items into a csv file
    *
    * @param selection
    * @param selectedFilePath
    * @param tourSegmenterView
    * @param isUseSimpleCSVFormat
    *           When <code>true</code> then the CSVTourDataReader can read the exported file,
    *           otherwise all values are exported
    */
   public CSVExport(final ISelection selection,
                    final String selectedFilePath,
                    final TourSegmenterView tourSegmenterView,
                    final boolean isUseSimpleCSVFormat) {

      _tourSegmenterView = tourSegmenterView;

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

      try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            Writer exportWriter = new BufferedWriter(outputStreamWriter)) {

         final StringBuilder stringBuilder = new StringBuilder();

         export_100_Header_Time(stringBuilder);
         export_120_Header_Motion(stringBuilder);
         export_140_Header_Elevation(stringBuilder);
         export_160_Header_Body(stringBuilder);
         export_180_Header_Power(stringBuilder);
         export_200_Header_Powertrain(stringBuilder);
         export_220_Header_RunningDynamics(stringBuilder);
         export_240_Header_Data(stringBuilder);

         // end of line
         stringBuilder.append(NL);

         exportWriter.write(stringBuilder.toString());
         if (isFlatLayout) {

            final StructuredSelection structuredSelection = (StructuredSelection) selection;

            for (final Object element : structuredSelection) {

               if (element instanceof TVITourBookTour) {

                  final TVITourBookItem tviItem = (TVITourBookItem) element;

                  // truncate buffer
                  stringBuilder.setLength(0);

                  final boolean isTour = true;

//                  export_410_Value_DateColumns(stringBuilder, tviItem);
//
//                  export_500_Value_Time(stringBuilder, isTour, tviItem);
//                  export_520_Value_Tour(stringBuilder, isTour, tviItem);
//                  export_540_Value_Motion(stringBuilder, tviItem);
//                  export_560_Value_Elevation(stringBuilder, tviItem);
//                  export_580_Value_Weather(stringBuilder, isTour, tviItem);
//                  export_600_Value_Body(stringBuilder, isTour, tviItem);
//                  export_620_Value_Power(stringBuilder, tviItem);
//                  export_640_Value_Powertrain(stringBuilder, tviItem);
//                  export_660_Value_Training(stringBuilder, tviItem);
//                  export_680_Value_RunningDynamics(stringBuilder, tviItem);
//                  export_700_Value_Surfing(stringBuilder, tviItem);
//                  export_720_Value_Device(stringBuilder, isTour, tviItem);
//                  export_740_Value_Data(stringBuilder, isTour, tviItem);

                  // end of line
                  stringBuilder.append(NL);
                  exportWriter.write(stringBuilder.toString());
               }
            }
         }

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }
   }

   private void csvExport_SimpleFormat(final ISelection selection, final String selectedFilePath) {

      try (FileOutputStream fileOutputStream = new FileOutputStream(selectedFilePath);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8);
            Writer exportWriter = new BufferedWriter(outputStreamWriter)) {

         final StringBuilder stringBuilder = new StringBuilder();

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
         stringBuilder.append(TOUR_CSV_ID_3);
         stringBuilder.append(NL);

         exportWriter.write(stringBuilder.toString());

//         if (selection instanceof ITreeSelection) {
//
//            for (final TreePath treePath : ((ITreeSelection) selection).getPaths()) {
//
//               final int numSegment = treePath.getSegmentCount();
//
//               for (int segmentIndex = 0; segmentIndex < numSegment; segmentIndex++) {
//
//                  final Object segment = treePath.getSegment(segmentIndex);
//
//                  if (segment instanceof TVITourBookTour) {
//
//                     csvExport_SimpleFormat_Tour(exportWriter, (TVITourBookTour) segment);
//                  }
//               }
//            }
//
//         } else if (selection instanceof StructuredSelection) {
//
//            for (final Object element : (StructuredSelection) selection) {
//
//               if (element instanceof TVITourBookTour) {
//
//                  csvExport_SimpleFormat_Tour(exportWriter, (TVITourBookTour) element);
//               }
//            }
//         }

      } catch (final IOException e) {
         StatusUtil.showStatus(e);
      }
   }

//   private void csvExport_SimpleFormat_Tour(final Writer exportWriter, final TVITourBookTour tviTour) throws IOException {
//
//      final StringBuilder stringBuilder = new StringBuilder();
//
//      final TourDateTime colDateTime = tviTour.colTourDateTime;
//      final ZonedDateTime tourZonedDateTime = colDateTime.tourZonedDateTime;
//
//      final long tourTypeId = tviTour.getTourTypeId();
//      final String tourTypeLabel = net.tourbook.ui.UI.getTourTypeLabel(tourTypeId);
//      final String tagNames = TourDatabase.getTagNames(tviTour.getTagIds());
//
//      stringBuilder.append(String.format(UI.EMPTY_STRING
//
//            + "%04d-%02d-%02d;" //     date                    //$NON-NLS-1$
//            + "%02d-%02d;" //          time                    //$NON-NLS-1$
//            ,
//
//            tourZonedDateTime.getYear(), //                    // Date (yyyy-mm-dd);
//            tourZonedDateTime.getMonthValue(),
//            tourZonedDateTime.getDayOfMonth(),
//
//            tourZonedDateTime.getHour(), //                    // Time (hh-mm);
//            tourZonedDateTime.getMinute() //
//      ));
//
//      csvField(stringBuilder, tviTour.colTourDeviceTime_Recorded); //     // Duration (sec);
//      csvField(stringBuilder, tviTour.colTourComputedTime_Break); //      // Paused Time (sec);
//      csvField(stringBuilder, tviTour.colTourDistance); //                // Distance (m);
//
//      csvField(stringBuilder, tviTour.colTourTitle); //                   // Title;
//      csvField(stringBuilder, UI.EMPTY_STRING); //                        // Comment; !!! THIS IS NOT YET SUPPORTED !!!
//      csvField(stringBuilder, tourTypeLabel); //                          // Tour Type;
//      csvField(stringBuilder, tagNames); //                               // Tags;
//
//      csvField(stringBuilder, tviTour.colAltitudeUp); //                  // Altitude Up (m);
//      csvField(stringBuilder, tviTour.colAltitudeDown); //                // Altitude Down (m);
//
//      // end of line
//      stringBuilder.append(NL);
//
//      exportWriter.write(stringBuilder.toString());
//   }

   private void csvField(final StringBuilder stringBuilder, final long fieldValue) {

      if (fieldValue != 0) {
         stringBuilder.append(fieldValue);
      }
      stringBuilder.append(SEPARATOR);
   }

   private void csvField(final StringBuilder stringBuilder, final String fieldValue) {

      if (fieldValue != null) {
         stringBuilder.append(fieldValue);
      }

      stringBuilder.append(SEPARATOR);
   }

   private void csvField_Nf0(final StringBuilder stringBuilder, final float fieldValue) {

      if (fieldValue != 0) {
         stringBuilder.append(_nf0.format(fieldValue));
      }
      stringBuilder.append(SEPARATOR);
   }

   private void csvField_Nf1(final StringBuilder stringBuilder, final float fieldValue) {

      if (fieldValue != 0) {
         stringBuilder.append(_nf1.format(fieldValue));
      }
      stringBuilder.append(SEPARATOR);
   }

   private void csvField_Nf2(final StringBuilder stringBuilder, final float fieldValue) {

      if (fieldValue != 0) {
         stringBuilder.append(_nf2.format(fieldValue));
      }
      stringBuilder.append(SEPARATOR);
   }

   private void csvHeader(final StringBuilder stringBuilder, final String fieldValue) {

      stringBuilder.append(fieldValue);
      stringBuilder.append(SEPARATOR);
   }

   private void export_100_Header_Time(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      // Year
      csvHeader(stringBuilder,                 HEADER_TIME_YEAR);

      // Month / Day / Week


         csvHeader(stringBuilder,              HEADER_TIME_MONTH);
         csvHeader(stringBuilder,              HEADER_TIME_DAY);
         csvHeader(stringBuilder,              HEADER_TIME_WEEK);

      csvHeader(stringBuilder,                 HEADER_TIME_TOUR_START_TIME);
      csvHeader(stringBuilder,                 HEADER_TIME_ISO_DATE_TIME);
      csvHeader(stringBuilder,                 HEADER_TIME_WEEKDAY);
      csvHeader(stringBuilder,                 HEADER_TIME_WEEK_YEAR);

      csvHeader(stringBuilder, String.format(  HEADER_TIME_DEVICE_ELAPSED_TIME,             Messages.App_Unit_Seconds_Small));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_DEVICE_RECORDED_TIME,            Messages.App_Unit_Seconds_Small));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_DEVICE_PAUSED_TIME,              Messages.App_Unit_Seconds_Small));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_COMPUTED_MOVING_TIME,            Messages.App_Unit_Seconds_Small));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_COMPUTED_BREAK_TIME,             Messages.App_Unit_Seconds_Small));
      csvHeader(stringBuilder,                 HEADER_TIME_COMPUTED_BREAK_TIME_RELATIVE);
      csvHeader(stringBuilder, String.format(  HEADER_TIME_DEVICE_ELAPSED_TIME,             CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_DEVICE_RECORDED_TIME,            CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_DEVICE_PAUSED_TIME,              CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_COMPUTED_MOVING_TIME,            CSV_EXPORT_DURATION_HHH_MM_SS));
      csvHeader(stringBuilder, String.format(  HEADER_TIME_COMPUTED_BREAK_TIME,             CSV_EXPORT_DURATION_HHH_MM_SS));

// SET_FORMATTING_ON

   }

   private void export_120_Header_Motion(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      csvHeader(stringBuilder, String.format(  HEADER_MOTION_DISTANCE,             UI.UNIT_LABEL_DISTANCE));
      csvHeader(stringBuilder, String.format(  HEADER_MOTION_SPEED_MAX,            UI.UNIT_LABEL_SPEED));
      csvHeader(stringBuilder, String.format(  HEADER_MOTION_SPEED_AVERAGE,        UI.UNIT_LABEL_SPEED));
      csvHeader(stringBuilder, String.format(  HEADER_MOTION_PACE_AVERAGE,         UI.UNIT_LABEL_PACE));

// SET_FORMATTING_ON

   }

   private void export_140_Header_Elevation(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      final String avgEle_UnitLabel = UI.SYMBOL_AVERAGE + UI.SPACE + UI.UNIT_LABEL_ELEVATION + "/" + UI.UNIT_LABEL_DISTANCE; //$NON-NLS-1$

      csvHeader(stringBuilder, String.format(  HEADER_ELEVATION_UP,                UI.UNIT_LABEL_ELEVATION));
      csvHeader(stringBuilder, String.format(  HEADER_ELEVATION_DOWN,              UI.UNIT_LABEL_ELEVATION));
      csvHeader(stringBuilder, String.format(  HEADER_ELEVATION_MAX,               UI.UNIT_LABEL_ELEVATION));
      csvHeader(stringBuilder, String.format(  HEADER_ELEVATION_AVERAGE_CHANGE,    avgEle_UnitLabel));

// SET_FORMATTING_ON

   }

   private void export_160_Header_Body(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      csvHeader(stringBuilder,                 HEADER_BODY_CALORIES);
      csvHeader(stringBuilder,                 HEADER_BODY_RESTPULSE);
      csvHeader(stringBuilder,                 HEADER_BODY_MAX_PULSE);
      csvHeader(stringBuilder,                 HEADER_BODY_AVERAGE_PULSE);
      csvHeader(stringBuilder, String.format(  HEADER_BODY_WEIGHT,                 UI.UNIT_LABEL_WEIGHT));
      csvHeader(stringBuilder,                 HEADER_BODY_PERSON);

// SET_FORMATTING_ON

   }

   private void export_180_Header_Power(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      csvHeader(stringBuilder,                 HEADER_POWER_AVG);
      csvHeader(stringBuilder,                 HEADER_POWER_MAX);
      csvHeader(stringBuilder,                 HEADER_POWER_NORMALIZED);
      csvHeader(stringBuilder,                 HEADER_POWER_TOTAL_WORK);

// SET_FORMATTING_ON

   }

   private void export_200_Header_Powertrain(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_AVERAGE_CADENCE);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_CADENCE_MULTIPLIER);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS);
      csvHeader(stringBuilder,                 HEADER_POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE);

// SET_FORMATTING_ON

   }

   private void export_220_Header_RunningDynamics(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      csvHeader(stringBuilder,                 HEADER_RUN_DYN_STANCE_TIME_MIN);
      csvHeader(stringBuilder,                 HEADER_RUN_DYN_STANCE_TIME_MAX);
      csvHeader(stringBuilder,                 HEADER_RUN_DYN_STANCE_TIME_AVG);

      csvHeader(stringBuilder,                 HEADER_RUN_DYN_STANCE_TIME_BALANCE_MIN);
      csvHeader(stringBuilder,                 HEADER_RUN_DYN_STANCE_TIME_BALANCE_MAX);
      csvHeader(stringBuilder,                 HEADER_RUN_DYN_STANCE_TIME_BALANCE_AVG);

      csvHeader(stringBuilder, String.format(  HEADER_RUN_DYN_STEP_LENGTH_MIN,              UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(stringBuilder, String.format(  HEADER_RUN_DYN_STEP_LENGTH_MAX,              UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(stringBuilder, String.format(  HEADER_RUN_DYN_STEP_LENGTH_AVG,              UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));

      csvHeader(stringBuilder, String.format(  HEADER_RUN_DYN_VERTICAL_OSCILLATION_MIN,     UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(stringBuilder, String.format(  HEADER_RUN_DYN_VERTICAL_OSCILLATION_MAX,     UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));
      csvHeader(stringBuilder, String.format(  HEADER_RUN_DYN_VERTICAL_OSCILLATION_AVG,     UI.UNIT_LABEL_DISTANCE_MM_OR_INCH));

      csvHeader(stringBuilder,                 HEADER_RUN_DYN_VERTICAL_RATIO_MIN);
      csvHeader(stringBuilder,                 HEADER_RUN_DYN_VERTICAL_RATIO_MAX);
      csvHeader(stringBuilder,                 HEADER_RUN_DYN_VERTICAL_RATIO_AVG);

// SET_FORMATTING_ON

   }

   private void export_240_Header_Data(final StringBuilder stringBuilder) {

// SET_FORMATTING_OFF

      csvHeader(stringBuilder,                 HEADER_DATA_DP_TOLERANCE);
      csvHeader(stringBuilder,                 HEADER_DATA_IMPORT_FILE_NAME);
      csvHeader(stringBuilder,                 HEADER_DATA_IMPORT_FILE_PATH);
      csvHeader(stringBuilder,                 HEADER_DATA_TIME_INTERVAL);
      csvHeader(stringBuilder,                 HEADER_DATA_TIME_SLICES);

// SET_FORMATTING_ON

   }

//   private void export_400_Value_DateColumns(final StringBuilder stringBuilder,
//                                             final int segmentCount,
//                                             final Object segment,
//                                             final boolean isTour) {
//
//      if (segment instanceof TVITourBookYear) {
//
//         final TVITourBookYear tviYear = (TVITourBookYear) segment;
//
//         // year
//         csvField(stringBuilder, tviYear.tourYear);
//
//         if (segmentCount == 1) {
//
//            for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
//               stringBuilder.append(SEPARATOR);
//            }
//         }
//
//      } else if (segment instanceof TVITourBookYearCategorized) {
//
//         final TVITourBookYearCategorized tviYearSub = (TVITourBookYearCategorized) segment;
//
//         // month or week
//         csvField(stringBuilder, tviYearSub.tourYearSub);
//
//         if (segmentCount == 2) {
//
//            for (int spacerIndex = segmentCount; spacerIndex < 4; spacerIndex++) {
//               stringBuilder.append(SEPARATOR);
//            }
//         }
//
//      } else if (isTour) {
//
//         final TVITourBookTour tviTour = (TVITourBookTour) segment;
//
//         if (isYearSubWeek()) {
//
//            // month
//            csvField(stringBuilder, tviTour.tourMonth);
//
//         } else {
//
//            // week
//            csvField(stringBuilder, tviTour.colWeekNo);
//         }
//
//         // day
//         csvField(stringBuilder, tviTour.tourDay);
//      }
//   }
//
//   private void export_410_Value_DateColumns(final StringBuilder stringBuilder, final TVITourBookItem tviItem) {
//
//      csvField(stringBuilder, tviItem.tourYear); // year
//      csvField(stringBuilder, tviItem.tourMonth); // month
//      csvField(stringBuilder, tviItem.tourDay); // day
//
//      csvField(stringBuilder, tviItem.colWeekNo); // week
//   }
//
//   private void export_500_Value_Time(final StringBuilder stringBuilder,
//                                      final boolean isTour,
//                                      final TVITourBookItem tviItem) {
//
//      TourDateTime tourDateTime = null;
//      ZonedDateTime tourStartDateTime = null;
//
//      if (isTour) {
//
//         tourDateTime = tviItem.colTourDateTime;
//         tourStartDateTime = tourDateTime.tourZonedDateTime;
//      }
//
////    // Time
////    defineColumn_1stColumn_Date();
////    defineColumn_Time_WeekDay();
////    defineColumn_Time_TourStartTime();
////    defineColumn_Time_TimeZoneDifference();
////    defineColumn_Time_TimeZone();
////    defineColumn_Time_MovingTime();
////    defineColumn_Time_RecordingTime();
////    defineColumn_Time_PausedTime();
////    defineColumn_Time_PausedTime_Relative();
////    defineColumn_Time_WeekNo();
////    defineColumn_Time_WeekYear();
//
////    csvField_Str(stringBuilder,                 HEADER_TIME_TOUR_START_TIME);
////    csvField_Str(stringBuilder,                 HEADER_TIME_ISO_DATE_TIME);
////    csvField_Str(stringBuilder,                 HEADER_TIME_WEEKDAY);
////    csvField_Str(stringBuilder,                 HEADER_TIME_WEEK_YEAR);
////
////    csvField_Str(stringBuilder, String.format(  HEADER_TIME_RECORDING_TIME,        Messages.App_Unit_Seconds_Small));
////    csvField_Str(stringBuilder, String.format(  HEADER_TIME_MOVING_TIME,           Messages.App_Unit_Seconds_Small));
////    csvField_Str(stringBuilder, String.format(  HEADER_TIME_PAUSED_TIME,           Messages.App_Unit_Seconds_Small));
////    csvField_Str(stringBuilder,                 HEADER_TIME_PAUSED_TIME_RELATIVE);
////    csvField_Str(stringBuilder, String.format(  HEADER_TIME_RECORDING_TIME,        CSV_EXPORT_DURATION_HHH_MM_SS));
////    csvField_Str(stringBuilder, String.format(  HEADER_TIME_MOVING_TIME,           CSV_EXPORT_DURATION_HHH_MM_SS));
////    csvField_Str(stringBuilder, String.format(  HEADER_TIME_PAUSED_TIME,           CSV_EXPORT_DURATION_HHH_MM_SS));
//
//      { // HEADER_TIME_TOUR_START_TIME
//
//         if (isTour) {
//            stringBuilder.append(tourStartDateTime.format(TimeTools.Formatter_Time_M));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TIME_ISO_DATE_TIME
//
//         if (isTour) {
//            stringBuilder.append(tourStartDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TIME_WEEKDAY
//
//         if (isTour) {
//            stringBuilder.append(tourDateTime.weekDay);
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      csvField(stringBuilder, tviItem.colWeekYear); // HEADER_TIME_WEEK_YEAR
//
//      /////////////////////////////////////////////////////////////////////////////////////////////
//
//      csvField(stringBuilder, tviItem.colTourDeviceTime_Elapsed); // HEADER_TIME_ELAPSED_TIME
//      csvField(stringBuilder, tviItem.colTourDeviceTime_Recorded); // HEADER_TIME_RECORDED_TIME
//      csvField(stringBuilder, tviItem.colTourDeviceTime_Paused); // HEADER_TIME_PAUSED_TIME
//      csvField(stringBuilder, tviItem.colTourComputedTime_Moving); // HEADER_TIME_MOVING_TIME
//      csvField(stringBuilder, tviItem.colTourComputedTime_Break); // HEADER_TIME_BREAK_TIME
//
//      { // HEADER_TIME_BREAK_TIME_RELATIVE
//
//         final long colBreakTime = tviItem.colTourComputedTime_Break;
//         final long dbPausedTime = colBreakTime;
//         final long dbElapsedTime = tviItem.colTourDeviceTime_Elapsed;
//         final float relativePausedTime = dbElapsedTime == 0 //
//               ? 0
//               : (float) dbPausedTime / dbElapsedTime * 100;
//         if (relativePausedTime != 0) {
//            stringBuilder.append(_nf1.format(relativePausedTime));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TIME_ELAPSED_TIME hhh:mm:ss
//
//         final long colElapsedTime = (tviItem).colTourDeviceTime_Elapsed;
//         if (colElapsedTime != 0) {
//            stringBuilder.append(net.tourbook.common.UI.format_hh_mm_ss(colElapsedTime));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TIME_RECORDED_TIME hhh:mm:ss
//
//         final long colTourDeviceTime_Recorded = (tviItem).colTourDeviceTime_Recorded;
//         if (colTourDeviceTime_Recorded != 0) {
//            stringBuilder.append(net.tourbook.common.UI.format_hh_mm_ss(colTourDeviceTime_Recorded));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TIME_PAUSED_TIME hhh:mm:ss
//
//         final long colTourDeviceTime_Paused = (tviItem).colTourDeviceTime_Paused;
//         if (colTourDeviceTime_Paused != 0) {
//            stringBuilder.append(net.tourbook.common.UI.format_hh_mm_ss(colTourDeviceTime_Paused));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TIME_MOVING_TIME hhh:mm:ss
//
//         final long colMovingTime = tviItem.colTourComputedTime_Moving;
//         if (colMovingTime != 0) {
//            stringBuilder.append(net.tourbook.common.UI.format_hh_mm_ss(colMovingTime));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TIME_BREAK_TIME hhh:mm:ss
//
//         final long colBreakTime = tviItem.colTourComputedTime_Break;
//         if (colBreakTime != 0) {
//            stringBuilder.append(net.tourbook.common.UI.format_hh_mm_ss(colBreakTime));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_520_Value_Tour(final StringBuilder stringBuilder,
//                                      final boolean isTour,
//                                      final TVITourBookItem tviItem) {
//
//      TVITourBookTour tviTour = null;
//
//      if (isTour) {
//
//         tviTour = (TVITourBookTour) tviItem;
//      }
//
////    defineColumn_Tour_TypeImage();
////    defineColumn_Tour_TypeText();
////    defineColumn_Tour_Title();
////    defineColumn_Tour_Marker();
////    defineColumn_Tour_Photos();
////    defineColumn_Tour_Tags();
////    defineColumn_Tour_Location_Start();
////    defineColumn_Tour_Location_End();
//////  defineColumn_Tour_TagIds();            // for debugging
//
////    csvField_Str(stringBuilder,                 HEADER_TOUR_NUMBER_OF_TOURS);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_TYPE_ID);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_TYPE_NAME);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_TITLE);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_LOCATION_START);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_LOCATION_END);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_TAGS);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_NUMBER_OF_MARKER);
////    csvField_Str(stringBuilder,                 HEADER_TOUR_NUMBER_OF_PHOTOS);
//
//      { // HEADER_TOUR_NUMBER_OF_TOURS
//
//         if (isTour) {
//            stringBuilder.append(Long.toString(1));
//         } else {
//            stringBuilder.append(Long.toString(tviItem.colCounter));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TOUR_TYPE_ID
//
//         if (isTour) {
//            stringBuilder.append(tviTour.getTourTypeId());
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TOUR_TYPE_NAME
//
//         if (isTour) {
//            final long tourTypeId = tviTour.getTourTypeId();
//            stringBuilder.append(net.tourbook.ui.UI.getTourTypeLabel(tourTypeId));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      csvField(stringBuilder, tviItem.colTourTitle); // HEADER_TOUR_TITLE
//      csvField(stringBuilder, tviItem.colTourLocation_Start); // HEADER_TOUR_LOCATION_START
//      csvField(stringBuilder, tviItem.colTourLocation_End); // HEADER_TOUR_LOCATION_END
//
//      { // HEADER_TOUR_TAGS
//
//         if (isTour) {
//            stringBuilder.append(TourDatabase.getTagNames(tviTour.getTagIds()));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TOUR_NUMBER_OF_MARKER
//
//         if (isTour) {
//            final ArrayList<Long> markerIds = tviTour.getMarkerIds();
//            if (markerIds != null) {
//               stringBuilder.append(Integer.toString(markerIds.size()));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_TOUR_NUMBER_OF_PHOTOS
//
//         final long numberOfPhotos = tviItem.colNumberOfPhotos;
//         if (numberOfPhotos != 0) {
//            stringBuilder.append(Long.toString(numberOfPhotos));
//         }
//
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_540_Value_Motion(final StringBuilder stringBuilder,
//                                        final TVITourBookItem tviItem) {
//
////    // Motion / Bewegung
////    defineColumn_Motion_Distance();
////    defineColumn_Motion_MaxSpeed();
////    defineColumn_Motion_AvgSpeed();
////    defineColumn_Motion_AvgPace();
//
////    csvField_Str(stringBuilder, String.format(  HEADER_MOTION_DISTANCE,            UI.UNIT_LABEL_DISTANCE));
////    csvField_Str(stringBuilder, String.format(  HEADER_MOTION_MAX_SPEED,           UI.UNIT_LABEL_SPEED));
////    csvField_Str(stringBuilder, String.format(  HEADER_MOTION_AVERAGE_SPEED,       UI.UNIT_LABEL_SPEED));
////    csvField_Str(stringBuilder, String.format(  HEADER_MOTION_AVERAGE_PACE,        UI.UNIT_LABEL_PACE));
//
//      { // HEADER_DISTANCE
//
//         final float dbDistance = tviItem.colTourDistance;
//         if (dbDistance != 0) {
//            stringBuilder.append(_nf1.format(dbDistance / 1000 / UI.UNIT_VALUE_DISTANCE));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_MAX_SPEED
//
//         final float dbMaxSpeed = tviItem.colMaxSpeed;
//         if (dbMaxSpeed != 0) {
//            stringBuilder.append(_nf1.format(dbMaxSpeed / UI.UNIT_VALUE_DISTANCE));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_AVERAGE_SPEED
//
//         final float speed = tviItem.colAvgSpeed / UI.UNIT_VALUE_DISTANCE;
//         if (speed != 0) {
//            stringBuilder.append(_nf1.format(speed));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_AVERAGE_PACE
//
//         final float pace = tviItem.colAvgPace * UI.UNIT_VALUE_DISTANCE;
//         if (pace != 0) {
//            stringBuilder.append(net.tourbook.common.UI.format_mm_ss((long) pace));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_560_Value_Elevation(final StringBuilder stringBuilder,
//                                           final TVITourBookItem tviItem) {
//
////    // Elevation
////    defineColumn_Elevation_Up();
////    defineColumn_Elevation_Down();
////    defineColumn_Elevation_Max();
////    defineColumn_Elevation_AvgChange();
////
////    csvField_Str(stringBuilder, String.format(  HEADER_ELEVATION_UP,                UI.UNIT_LABEL_ELEVATION));
////    csvField_Str(stringBuilder, String.format(  HEADER_ELEVATION_DOWN,              UI.UNIT_LABEL_ELEVATION));
////    csvField_Str(stringBuilder, String.format(  HEADER_ELEVATION_MAX,               UI.UNIT_LABEL_ELEVATION));
////    csvField_Str(stringBuilder, String.format(  HEADER_ELEVATION_AVERAGE_CHANGE,    avgEle_UnitLabel));
//
//      { // HEADER_ALTITUDE_UP
//
//         final long dbAltitudeUp = tviItem.colAltitudeUp;
//         if (dbAltitudeUp != 0) {
//            stringBuilder.append(Long.toString((long) (dbAltitudeUp / UI.UNIT_VALUE_ELEVATION)));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_ALTITUDE_DOWN
//
//         final long dbAltitudeDown = tviItem.colAltitudeDown;
//         if (dbAltitudeDown != 0) {
//            stringBuilder.append(Long.toString((long) (-dbAltitudeDown / UI.UNIT_VALUE_ELEVATION)));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_MAX_ALTITUDE
//
//         final long dbMaxAltitude = tviItem.colMaxAltitude;
//         if (dbMaxAltitude != 0) {
//            stringBuilder.append(Long.toString((long) (dbMaxAltitude / UI.UNIT_VALUE_ELEVATION)));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_ELEVATION_AVERAGE_CHANGE
//
//         final double dbValue = UI.convertAverageElevationChangeFromMetric((tviItem).colAltitude_AvgChange);
//
//         if (dbValue != 0) {
//            stringBuilder.append(_nf0.format(dbValue));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_580_Value_Weather(final StringBuilder stringBuilder,
//                                         final boolean isTour,
//                                         final TVITourBookItem tviItem) {
//
//      TVITourBookTour tviTour = null;
//
//      if (isTour) {
//         tviTour = (TVITourBookTour) tviItem;
//      }
//
////    // Weather
////    defineColumn_Weather_Clouds();
////    defineColumn_Weather_Temperature_Avg();
////    defineColumn_Weather_Temperature_Min();
////    defineColumn_Weather_Temperature_Max();
////    defineColumn_Weather_WindSpeed();
////    defineColumn_Weather_WindDirection();
//
////    csvField_Str(stringBuilder,                 HEADER_WEATHER_CLOUDS);
////    csvField_Str(stringBuilder, String.format(  HEADER_WEATHER_TEMPERATURE_AVERAGE, UI.UNIT_LABEL_TEMPERATURE));
////    csvField_Str(stringBuilder, String.format(  HEADER_WEATHER_TEMPERATURE_MIN,     UI.UNIT_LABEL_TEMPERATURE));
////    csvField_Str(stringBuilder, String.format(  HEADER_WEATHER_TEMPERATURE_MAX,     UI.UNIT_LABEL_TEMPERATURE));
////    csvField_Str(stringBuilder,                 HEADER_WEATHER_WIND_SPEED);
////    csvField_Str(stringBuilder,                 HEADER_WEATHER_WIND_DIRECTION);
//
//      { // HEADER_WEATHER_CLOUDS
//
//         if (isTour) {
//            final String windClouds = tviTour.colClouds;
//            if (windClouds != null) {
//               stringBuilder.append(windClouds);
//            }
//
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_WEATHER_TEMPERATURE_AVERAGE
//
//         final float dbValue = tviItem.colTemperature_Average_Device;
//
//         if (dbValue != 0) {
//            stringBuilder.append(_nf1.format(UI.convertTemperatureFromMetric(dbValue)));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_WEATHER_TEMPERATURE_MIN
//
//         final float dbValue = tviItem.colTemperature_Min_Device;
//
//         if (dbValue != 0) {
//            stringBuilder.append(_nf1.format(UI.convertTemperatureFromMetric(dbValue)));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_WEATHER_TEMPERATURE_MAX
//
//         final float dbValue = tviItem.colTemperature_Max_Device;
//
//         if (dbValue != 0) {
//            stringBuilder.append(_nf1.format(UI.convertTemperatureFromMetric(dbValue)));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_WEATHER_WIND_SPEED
//
//         final int windSpeed = (int) (tviItem.colWindSpeed / UI.UNIT_VALUE_DISTANCE);
//         if (windSpeed != 0) {
//            stringBuilder.append(Integer.toString(windSpeed));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_WEATHER_WIND_DIRECTION
//
//         if (isTour) {
//            final int windDir = tviItem.colWindDirection;
//            if (windDir != 0) {
//               stringBuilder.append(Integer.toString(windDir));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_600_Value_Body(final StringBuilder stringBuilder,
//                                      final boolean isTour,
//                                      final TVITourBookItem tviItem) {
//
//      TVITourBookTour tviTour = null;
//
//      if (isTour) {
//
//         tviTour = (TVITourBookTour) tviItem;
//      }
//
////    // Body
////    defineColumn_Body_Calories();
////    defineColumn_Body_RestPulse();
////    defineColumn_Body_MaxPulse();
////    defineColumn_Body_AvgPulse();
////    defineColumn_Body_Weight();
////    defineColumn_Body_Person();
//
////    csvField_Str(stringBuilder,                 HEADER_BODY_CALORIES);
////    csvField_Str(stringBuilder,                 HEADER_BODY_RESTPULSE);
////    csvField_Str(stringBuilder,                 HEADER_BODY_MAX_PULSE);
////    csvField_Str(stringBuilder,  String.format( HEADER_BODY_AVERAGE_PULSE, GRAPH_LABEL_HEARTBEAT_UNIT));
////    csvField_Str(stringBuilder,                 HEADER_BODY_WEIGHT);
////    csvField_Str(stringBuilder,                 HEADER_BODY_PERSON);
//
//      { // HEADER_BODY_CALORIES
//
//         final double calories = tviItem.colCalories / 1000.0;
//         if (calories != 0) {
//            stringBuilder.append(_nf3.format(calories));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_BODY_RESTPULSE
//
//         if (isTour) {
//            final int restPulse = tviItem.colRestPulse;
//            if (restPulse != 0) {
//               stringBuilder.append(Integer.toString(restPulse));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_BODY_MAX_PULSE
//
//         if (isTour) {
//            final long dbMaxPulse = tviItem.colMaxPulse;
//            if (dbMaxPulse != 0) {
//               stringBuilder.append(Long.toString(dbMaxPulse));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_BODY_AVERAGE_PULSE
//
//         final float pulse = tviItem.colAvgPulse;
//         if (pulse != 0) {
//            stringBuilder.append(_nf1.format(pulse));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_BODY_WEIGHT
//
//         if (isTour) {
//            final double dbValue = UI.convertBodyWeightFromMetric((tviItem).colBodyWeight);
//            if (dbValue != 0) {
//               stringBuilder.append(_nf1.format(dbValue));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_BODY_PERSON
//
//         if (isTour) {
//            final long dbPersonId = tviTour.colPersonId;
//            stringBuilder.append(PersonManager.getPersonName(dbPersonId));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_620_Value_Power(final StringBuilder stringBuilder,
//                                       final TVITourBookItem tviItem) {
//
////    // Power - Leistung
////    defineColumn_Power_Avg();
////    defineColumn_Power_Max();
////    defineColumn_Power_Normalized();
////    defineColumn_Power_TotalWork();
//
//// SET_FORMATTING_OFF
//
//      csvField_Nf1(stringBuilder, tviItem.colPower_Avg);                     // HEADER_POWER_AVG
//      csvField_Nf1(stringBuilder, tviItem.colPower_Max);                     // HEADER_POWER_MAX
//      csvField_Nf1(stringBuilder, tviItem.colPower_Normalized);              // HEADER_POWER_NORMALIZED
//      csvField_Nf1(stringBuilder, tviItem.colPower_TotalWork / 1000_000f);   // HEADER_POWER_TOTAL_WORK
//
//// SET_FORMATTING_ON
//   }
//
//   private void export_640_Value_Powertrain(final StringBuilder stringBuilder,
//                                            final TVITourBookItem tviItem) {
//
////    // Powertrain - Antrieb/Pedal
////    defineColumn_Powertrain_AvgCadence();
////    defineColumn_Powertrain_SlowVsFastCadencePercentage();
////    defineColumn_Powertrain_SlowVsFastCadenceZonesDelimiter();
////    defineColumn_Powertrain_CadenceMultiplier();
////    defineColumn_Powertrain_Gear_FrontShiftCount();
////    defineColumn_Powertrain_Gear_RearShiftCount();
////    defineColumn_Powertrain_AvgLeftPedalSmoothness();
////    defineColumn_Powertrain_AvgLeftTorqueEffectiveness();
////    defineColumn_Powertrain_AvgRightPedalSmoothness();
////    defineColumn_Powertrain_AvgRightTorqueEffectiveness();
////    defineColumn_Powertrain_PedalLeftRightBalance();
//
//// SET_FORMATTING_OFF
//
//      csvField_Nf1(  stringBuilder, tviItem.colAvgCadence);                        // HEADER_POWERTRAIN_AVERAGE_CADENCE
//      csvField(      stringBuilder, tviItem.colSlowVsFastCadence);                 // HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_PERCENTAGES
//      csvField_Nf0(  stringBuilder, tviItem.colCadenceZonesDelimiter);             // HEADER_POWERTRAIN_SLOW_VS_FAST_CADENCE_ZONES_DELIMITER
//      csvField_Nf1(  stringBuilder, tviItem.colCadenceMultiplier);                 // HEADER_POWERTRAIN_CADENCE_MULTIPLIER
//      csvField(      stringBuilder, tviItem.colFrontShiftCount);                   // HEADER_POWERTRAIN_GEAR_FRONT_SHIFT_COUNT
//      csvField(      stringBuilder, tviItem.colRearShiftCount);                    // HEADER_POWERTRAIN_GEAR_REAR_SHIFT_COUNT
//      csvField_Nf1(  stringBuilder, tviItem.colPower_AvgLeftPedalSmoothness);      // HEADER_POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS
//      csvField_Nf1(  stringBuilder, tviItem.colPower_AvgLeftTorqueEffectiveness);  // HEADER_POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS
//      csvField_Nf1(  stringBuilder, tviItem.colPower_AvgRightPedalSmoothness);     // HEADER_POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS
//      csvField_Nf1(  stringBuilder, tviItem.colPower_AvgRightTorqueEffectiveness); // HEADER_POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS
//      csvField(      stringBuilder, tviItem.colPower_PedalLeftRightBalance);       // HEADER_POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE
//
//// SET_FORMATTING_ON
//   }
//
//   private void export_660_Value_Training(final StringBuilder stringBuilder,
//                                          final TVITourBookItem tviItem) {
//
////    // Training - Trainingsanalyse
////    defineColumn_Training_FTP();
////    defineColumn_Training_PowerToWeightRatio();
////    defineColumn_Training_IntensityFactor();
////    defineColumn_Training_StressScore();
////    defineColumn_Training_TrainingEffect();
////    defineColumn_Training_TrainingEffect_Anaerobic();
////    defineColumn_Training_TrainingPerformance();
//
//// SET_FORMATTING_OFF
//
//      csvField(      stringBuilder, tviItem.colTraining_FTP);                      // HEADER_TRAINING_FTP
//      csvField_Nf2(  stringBuilder, tviItem.colTraining_PowerToWeight);            // HEADER_TRAINING_POWER_TO_WEIGHT
//      csvField_Nf2(  stringBuilder, tviItem.colTraining_IntensityFactor);          // HEADER_TRAINING_INTENSITY_FACTOR
//      csvField_Nf1(  stringBuilder, tviItem.colTraining_TrainingStressScore);      // HEADER_TRAINING_STRESS_SCORE
//      csvField_Nf1(  stringBuilder, tviItem.colTraining_TrainingEffect_Aerob);     // HEADER_TRAINING_TRAINING_EFFECT_AEROB
//      csvField_Nf1(  stringBuilder, tviItem.colTraining_TrainingEffect_Anaerobic); // HEADER_TRAINING_TRAINING_EFFECT_ANAEROB
//      csvField_Nf2(  stringBuilder, tviItem.colTraining_TrainingPerformance);      // HEADER_TRAINING_TRAINING_PERFORMANCE
//
//// SET_FORMATTING_ON
//   }
//
//   private void export_680_Value_RunningDynamics(final StringBuilder stringBuilder,
//                                                 final TVITourBookItem tviItem) {
//
////    // Running dynamics
////    defineColumn_RunDyn_StanceTime_Min();
////    defineColumn_RunDyn_StanceTime_Max();
////    defineColumn_RunDyn_StanceTime_Avg();
//
////    defineColumn_RunDyn_StanceTimeBalance_Min();
////    defineColumn_RunDyn_StanceTimeBalance_Max();
////    defineColumn_RunDyn_StanceTimeBalance_Avg();
//
////    defineColumn_RunDyn_StepLength_Min();
////    defineColumn_RunDyn_StepLength_Max();
////    defineColumn_RunDyn_StepLength_Avg();
//
////    defineColumn_RunDyn_VerticalOscillation_Min();
////    defineColumn_RunDyn_VerticalOscillation_Max();
////    defineColumn_RunDyn_VerticalOscillation_Avg();
//
////    defineColumn_RunDyn_VerticalRatio_Min();
////    defineColumn_RunDyn_VerticalRatio_Max();
////    defineColumn_RunDyn_VerticalRatio_Avg();
//
//// SET_FORMATTING_OFF
//
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StanceTime_Min);            // HEADER_RUN_DYN_STANCE_TIME_MIN
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StanceTime_Max);            // HEADER_RUN_DYN_STANCE_TIME_MAX
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StanceTime_Avg);            // HEADER_RUN_DYN_STANCE_TIME_AVG
//
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StanceTimeBalance_Min);     // HEADER_RUN_DYN_STANCE_TIME_BALANCE_MIN
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StanceTimeBalance_Max);     // HEADER_RUN_DYN_STANCE_TIME_BALANCE_MAX
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StanceTimeBalance_Avg);     // HEADER_RUN_DYN_STANCE_TIME_BALANCE_AVG
//
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StepLength_Min);            // HEADER_RUN_DYN_STEP_LENGTH_MIN
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StepLength_Max);            // HEADER_RUN_DYN_STEP_LENGTH_MAX
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_StepLength_Avg);            // HEADER_RUN_DYN_STEP_LENGTH_AVG
//
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_VerticalOscillation_Min);   // HEADER_RUN_DYN_VERTICAL_OSCILLATION_MIN
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_VerticalOscillation_Max);   // HEADER_RUN_DYN_VERTICAL_OSCILLATION_MAX
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_VerticalOscillation_Avg);   // HEADER_RUN_DYN_VERTICAL_OSCILLATION_AVG
//
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_VerticalRatio_Min);         // HEADER_RUN_DYN_VERTICAL_RATIO_MIN
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_VerticalRatio_Max);         // HEADER_RUN_DYN_VERTICAL_RATIO_MAX
//      csvField_Nf1(stringBuilder, tviItem.colRunDyn_VerticalRatio_Avg);         // HEADER_RUN_DYN_VERTICAL_RATIO_AVG
//
//// SET_FORMATTING_ON
//   }
//
//   private void export_700_Value_Surfing(final StringBuilder stringBuilder,
//                                         final TVITourBookItem tviItem) {
//
////    // Surfing
////    defineColumn_Surfing_NumberOfEvents();
////    defineColumn_Surfing_MinSpeed_StartStop();
////    defineColumn_Surfing_MinSpeed_Surfing();
////    defineColumn_Surfing_MinTimeDuration();
////    defineColumn_Surfing_MinDistance();
//
////    csvField_Str(stringBuilder,                 HEADER_SURFING_NUMBER_OF_EVENTS);
////    csvField_Str(stringBuilder,                 HEADER_SURFING_MIN_SPEED_START_STOP);
////    csvField_Str(stringBuilder,                 HEADER_SURFING_MIN_SPEED_SURFING);
////    csvField_Str(stringBuilder,                 HEADER_SURFING_MIN_TIME_DURATION);
////    csvField_Str(stringBuilder,                 HEADER_SURFING_MIN_DISTANCE);
//
//      { // HEADER_SURFING_NUMBER_OF_EVENTS
//
//         final long dbValue = tviItem.col_Surfing_NumberOfEvents;
//
//         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
//            stringBuilder.append(Long.toString(dbValue));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_SURFING_MIN_SPEED_START_STOP
//
//         final long dbValue = tviItem.col_Surfing_MinSpeed_StartStop;
//
//         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
//            stringBuilder.append(Long.toString(dbValue));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_SURFING_MIN_SPEED_SURFING
//
//         final long dbValue = tviItem.col_Surfing_MinSpeed_Surfing;
//
//         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
//            stringBuilder.append(Long.toString(dbValue));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_SURFING_MIN_TIME_DURATION
//
//         final long dbValue = tviItem.col_Surfing_MinTimeDuration;
//
//         if (dbValue != 0 && dbValue != TourData.SURFING_VALUE_IS_NOT_SET) {
//            stringBuilder.append(Long.toString(dbValue));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_SURFING_MIN_DISTANCE
//
//         final short value = tviItem.col_Surfing_MinDistance;
//         final boolean isMinDistance = tviItem.col_Surfing_IsMinDistance;
//
//         if (value != 0 && value != TourData.SURFING_VALUE_IS_NOT_SET && isMinDistance) {
//
//            int minSurfingDistance = value;
//
//            // convert imperial -> metric
//            if (UI.UNIT_IS_LENGTH_YARD) {
//               minSurfingDistance = (int) (minSurfingDistance / UI.UNIT_YARD + 0.5);
//            }
//
//            stringBuilder.append(Long.toString(minSurfingDistance));
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_720_Value_Device(final StringBuilder stringBuilder,
//                                        final boolean isTour,
//                                        final TVITourBookItem tviItem) {
//
//      TVITourBookTour tviTour = null;
//
//      if (isTour) {
//         tviTour = (TVITourBookTour) tviItem;
//      }
//
////    // Device
////    defineColumn_Device_Name();
////    defineColumn_Device_Distance();
//
////    csvField_Str(stringBuilder,                 HEADER_DEVICE_NAME);
////    csvField_Str(stringBuilder,                 HEADER_DEVICE_START_DISTANCE);
//
//      { // HEADER_DEVICE_NAME
//
//         final String dbValue = tviItem.colDeviceName;
//         if (dbValue != null) {
//            stringBuilder.append(dbValue);
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      { // HEADER_DEVICE_START_DISTANCE
//
//         if (isTour) {
//            final long dbStartDistance = tviTour.colStartDistance;
//            if (dbStartDistance != 0) {
//               stringBuilder.append(Long.toString((long) (dbStartDistance / UI.UNIT_VALUE_DISTANCE)));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }
//
//   private void export_740_Value_Data(final StringBuilder stringBuilder,
//                                      final boolean isTour,
//                                      final TVITourBookItem tviItem) {
//
//      TVITourBookTour tviTour = null;
//
//      if (isTour) {
//
//         tviTour = (TVITourBookTour) tviItem;
//      }
//
////    // Data
////    defineColumn_Data_DPTolerance();
////    defineColumn_Data_ImportFilePath();
////    defineColumn_Data_ImportFileName();
////    defineColumn_Data_TimeInterval();
////    defineColumn_Data_NumTimeSlices();
//
////    csvField_Str(stringBuilder,                 HEADER_DATA_DP_TOLERANCE);
////    csvField_Str(stringBuilder,                 HEADER_DATA_IMPORT_FILE_NAME);
////    csvField_Str(stringBuilder,                 HEADER_DATA_IMPORT_FILE_PATH);
////    csvField_Str(stringBuilder,                 HEADER_DATA_TIME_INTERVAL);
////    csvField_Str(stringBuilder,                 HEADER_DATA_TIME_SLICES);
//
//      { // HEADER_DATA_DP_TOLERANCE
//
//         if (isTour) {
//            final int dpTolerance = tviItem.colDPTolerance;
//            if (dpTolerance != 0) {
//               stringBuilder.append(_nf1.format(dpTolerance / 10.0));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//
//      csvField(stringBuilder, tviItem.col_ImportFileName); // HEADER_DATA_IMPORT_FILE_NAME
//      csvField(stringBuilder, tviItem.col_ImportFilePath); // HEADER_DATA_IMPORT_FILE_PATH
//      csvField(stringBuilder, tviItem.colNumberOfTimeSlices); // HEADER_DATA_TIME_SLICES
//
//      { // HEADER_DATA_TIME_INTERVAL
//
//         if (isTour) {
//            final int dbValue = tviTour.colTimeInterval;
//            if (dbValue != 0) {
//               stringBuilder.append(Long.toString(dbValue));
//            }
//         }
//         stringBuilder.append(SEPARATOR);
//      }
//   }

}
