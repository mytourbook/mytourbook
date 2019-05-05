/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui;

import net.tourbook.common.UI;
import net.tourbook.common.formatter.ValueFormat;
import net.tourbook.common.formatter.ValueFormatSet;
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.TableColumnDefinition;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;

public abstract class TableColumnFactory {

   public static final TableColumnFactory ALTITUDE_ALTITUDE;
   public static final TableColumnFactory ALTITUDE_DIFF_SEGMENT_BORDER;
   public static final TableColumnFactory ALTITUDE_DIFF_SEGMENT_COMPUTED;
   public static final TableColumnFactory ALTITUDE_ELEVATION_DOWN;
   public static final TableColumnFactory ALTITUDE_ELEVATION_UP;
   public static final TableColumnFactory ALTITUDE_ELEVATION_SEGMENT_DOWN;
   public static final TableColumnFactory ALTITUDE_ELEVATION_SEGMENT_UP;
   public static final TableColumnFactory ALTITUDE_GRADIENT;
   public static final String             ALTITUDE_GRADIENT_ID          = "ALTITUDE_GRADIENT";          //$NON-NLS-1$
   public static final TableColumnFactory ALTITUDE_SUMMARIZED_BORDER_DOWN;
   public static final TableColumnFactory ALTITUDE_SUMMARIZED_BORDER_UP;
   public static final TableColumnFactory ALTITUDE_SUMMARIZED_COMPUTED_DOWN;
   public static final TableColumnFactory ALTITUDE_SUMMARIZED_COMPUTED_UP;

   public static final TableColumnFactory BODY_AVG_PULSE;
   public static final String             BODY_AVG_PULSE_ID             = "BODY_AVG_PULSE";             //$NON-NLS-1$
   public static final TableColumnFactory BODY_AVG_PULSE_DIFFERENCE;
   public static final TableColumnFactory BODY_CALORIES;
   public static final TableColumnFactory BODY_PULSE;

   public static final TableColumnFactory DATA_FIRST_COLUMN;
   public static final TableColumnFactory DATA_IMPORT_FILE_PATH;
   public static final TableColumnFactory DATA_IMPORT_FILE_NAME;
   public static final TableColumnFactory DATA_SERIE_START_END_INDEX;
   public static final String             DATA_SERIE_START_END_INDEX_ID = "DATA_SERIE_START_END_INDEX"; //$NON-NLS-1$
   public static final TableColumnFactory DATA_SEQUENCE;
   public static final String             DATA_SEQUENCE_ID              = "DATA_SEQUENCE";              //$NON-NLS-1$
   public static final TableColumnFactory DATA_TIME_INTERVAL;

   public static final TableColumnFactory DEVICE_NAME;
   public static final TableColumnFactory DEVICE_PROFILE;

   public static final TableColumnFactory MARKER_MAP_VISIBLE;
   public static final TableColumnFactory MARKER_SERIE_INDEX;
   public static final TableColumnFactory MARKER_TIME_DELTA;
   public static final TableColumnFactory MARKER_URL;

   public static final TableColumnFactory MOTION_ALTIMETER;
   public static final String             MOTION_ALTIMETER_ID           = "MOTION_ALTIMETER";           //$NON-NLS-1$
   public static final TableColumnFactory MOTION_AVG_PACE;
   public static final String             MOTION_AVG_PACE_ID            = "MOTION_AVG_PACE";            //$NON-NLS-1$
   public static final TableColumnFactory MOTION_AVG_PACE_DIFFERENCE;
   public static final TableColumnFactory MOTION_AVG_SPEED;
   public static final String             MOTION_AVG_SPEED_ID           = "MOTION_AVG_SPEED";           //$NON-NLS-1$
   public static final TableColumnFactory MOTION_DISTANCE;
   public static final String             MOTION_DISTANCE_ID            = "MOTION_DISTANCE";            //$NON-NLS-1$
   public static final TableColumnFactory MOTION_DISTANCE_DELTA;
   public static final TableColumnFactory MOTION_DISTANCE_DIFF;
   public static final TableColumnFactory MOTION_DISTANCE_TOTAL;
   public static final TableColumnFactory MOTION_LATITUDE;
   public static final TableColumnFactory MOTION_LONGITUDE;
   public static final TableColumnFactory MOTION_PACE;
   public static final TableColumnFactory MOTION_SPEED;
   public static final TableColumnFactory MOTION_SPEED_DIFF;

   public static final TableColumnFactory PHOTO_NUMBER_OF_GPS_PHOTOS;
   public static final TableColumnFactory PHOTO_NUMBER_OF_NO_GPS_PHOTOS;
   public static final TableColumnFactory PHOTO_NUMBER_OF_PHOTOS;
   public static final TableColumnFactory PHOTO_TIME_ADJUSTMENT;
   public static final TableColumnFactory PHOTO_TOUR_CAMERA;

   public static final TableColumnFactory POWER;

   public static final TableColumnFactory POWERTRAIN_AVG_CADENCE;
   public static final String             POWERTRAIN_AVG_CADENCE_ID     = "POWERTRAIN_AVG_CADENCE";     //$NON-NLS-1$
   public static final TableColumnFactory POWERTRAIN_CADENCE;
   public static final TableColumnFactory POWERTRAIN_GEAR_RATIO;
   public static final TableColumnFactory POWERTRAIN_GEAR_TEETH;

   public static final TableColumnFactory RUN_DYN_STEP_LENGTH_AVG;

   public static final TableColumnFactory STATE_DB_STATUS;
   public static final TableColumnFactory STATE_IMPORT_STATE;

   public static final TableColumnFactory SWIM__SWIM_CADENCE;
   public static final TableColumnFactory SWIM__SWIM_STROKES;
   public static final TableColumnFactory SWIM__SWIM_STROKE_STYLE;

   public static final TableColumnFactory SWIM__TIME_TOUR_TIME_DIFF;
   public static final TableColumnFactory SWIM__TIME_TOUR_TIME_HH_MM_SS;
   public static final TableColumnFactory SWIM__TIME_TOUR_TIME;
   public static final TableColumnFactory SWIM__TIME_TOUR_TIME_OF_DAY_HH_MM_SS;

   public static final TableColumnFactory TIME_BREAK_TIME;
   public static final TableColumnFactory TIME_DRIVING_TIME;
   public static final String             TIME_DRIVING_TIME_ID          = "TIME_DRIVING_TIME_ID";       //$NON-NLS-1$
   public static final TableColumnFactory TIME_PAUSED_TIME;
   public static final TableColumnFactory TIME_RECORDING_TIME;
   public static final String             TIME_RECORDING_TIME_ID        = "TIME_RECORDING_TIME";        //$NON-NLS-1$
   public static final TableColumnFactory TIME_RECORDING_TIME_TOTAL;
   public static final TableColumnFactory TIME_TIME_ZONE;
   public static final TableColumnFactory TIME_TIME_ZONE_DIFFERENCE;
   public static final TableColumnFactory TIME_TOUR_TIME_DIFF;
   public static final TableColumnFactory TIME_TOUR_TIME_HH_MM_SS;
   public static final TableColumnFactory TIME_TOUR_TIME;
   public static final TableColumnFactory TIME_TOUR_TIME_OF_DAY_HH_MM_SS;
   public static final TableColumnFactory TIME_TOUR_DATE;
   public static final TableColumnFactory TIME_TOUR_DURATION_TIME;
   public static final TableColumnFactory TIME_TOUR_START_TIME;
   public static final TableColumnFactory TIME_TOUR_END_TIME;
   public static final TableColumnFactory TIME_TOUR_START_DATE;
   public static final TableColumnFactory TIME_TOUR_END_DATE;

   public static final TableColumnFactory TOUR_TAGS;
   public static final TableColumnFactory TOUR_MARKERS;
   public static final TableColumnFactory TOUR_MARKER;
   public static final TableColumnFactory TOUR_TITLE;
   public static final TableColumnFactory TOUR_TYPE;
   public static final TableColumnFactory TOUR_TYPE_TEXT;

   public static final TableColumnFactory WAYPOINT_ALTITUDE;
   public static final TableColumnFactory WAYPOINT_DATE;
   public static final TableColumnFactory WAYPOINT_CATEGORY;
   public static final TableColumnFactory WAYPOINT_COMMENT;
   public static final TableColumnFactory WAYPOINT_DESCRIPTION;
   public static final TableColumnFactory WAYPOINT_ID;
   public static final TableColumnFactory WAYPOINT_NAME;
   public static final TableColumnFactory WAYPOINT_SYMBOL;
   public static final TableColumnFactory WAYPOINT_TIME;

   public static final TableColumnFactory WEATHER_CLOUDS;
   public static final TableColumnFactory WEATHER_TEMPERATURE;

   static {

      /*
       * Altitude
       */

      ALTITUDE_ALTITUDE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_ALTITUDE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_label);
            colDef.setColumnHeaderText(UI.UNIT_LABEL_ALTITUDE);
            colDef.setColumnUnit(UI.UNIT_LABEL_ALTITUDE);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      ALTITUDE_DIFF_SEGMENT_BORDER = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String unitLabel = UI.SYMBOL_DIFFERENCE_WITH_SPACE
                  + UI.UNIT_LABEL_ALTITUDE
                  + UI.SYMBOL_DOUBLE_VERTICAL;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_DIFF_SEGMENT_BORDER", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_difference_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_difference_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_0,
                  columnManager);

            return colDef;
         }
      };

      ALTITUDE_DIFF_SEGMENT_COMPUTED = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String unitLabel = UI.SYMBOL_DIFFERENCE_WITH_SPACE
                  + UI.UNIT_LABEL_ALTITUDE
                  + UI.SYMBOL_DOUBLE_HORIZONTAL;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_DIFF_SEGMENT_COMPUTED", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_difference_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_computed_difference_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_0,
                  columnManager);

            return colDef;
         }
      };

      ALTITUDE_ELEVATION_DOWN = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_ELEVATION_DOWN", //$NON-NLS-1$
                  SWT.TRAIL);

            final String unitLabel = UI.UNIT_LABEL_ALTITUDE
                  + Messages.ColumnFactory_hour
                  + UI.SPACE
                  + UI.SYMBOL_ARROW_DOWN;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_h_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_down_h_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      ALTITUDE_ELEVATION_UP = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_ELEVATION_UP", //$NON-NLS-1$
                  SWT.TRAIL);

            final String unitLabel = UI.UNIT_LABEL_ALTITUDE
                  + Messages.ColumnFactory_hour
                  + UI.SPACE
                  + UI.SYMBOL_ARROW_UP;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_h_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_up_h_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      ALTITUDE_ELEVATION_SEGMENT_DOWN = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_ELEVATION_SEGMENT_DOWN", //$NON-NLS-1$
                  SWT.TRAIL);

            final String unitLabel = UI.UNIT_LABEL_ALTITUDE + UI.SPACE + UI.SYMBOL_ARROW_DOWN;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_Segment_Descent_Label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Segment_Descent_Tooltip);
            colDef.setColumnUnit(unitLabel);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      ALTITUDE_ELEVATION_SEGMENT_UP = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_ELEVATION_SEGMENT_UP", //$NON-NLS-1$
                  SWT.TRAIL);

            final String unitLabel = UI.UNIT_LABEL_ALTITUDE + UI.SPACE + UI.SYMBOL_ARROW_UP;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_Segment_Ascent_Label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Segment_Ascent_Tooltip);
            colDef.setColumnUnit(unitLabel);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      ALTITUDE_GRADIENT = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  ALTITUDE_GRADIENT_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_gradient_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_gradient);
            colDef.setColumnUnit(Messages.ColumnFactory_gradient);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_gradient_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_1,
                  columnManager);

            return colDef;
         }
      };

      ALTITUDE_SUMMARIZED_BORDER_DOWN = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_SUMMARIZED_BORDER_DOWN", //$NON-NLS-1$
                  SWT.TRAIL);

            final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE
                  + UI.UNIT_LABEL_ALTITUDE
                  + UI.SPACE
                  + UI.SYMBOL_ARROW_DOWN;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_down_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      ALTITUDE_SUMMARIZED_BORDER_UP = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_SUMMARIZED_BORDER_UP", //$NON-NLS-1$
                  SWT.TRAIL);

            final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE
                  + UI.UNIT_LABEL_ALTITUDE
                  + UI.SPACE
                  + UI.SYMBOL_ARROW_UP;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_up_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      ALTITUDE_SUMMARIZED_COMPUTED_DOWN = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE
                  + UI.UNIT_LABEL_ALTITUDE
                  + UI.SYMBOL_DOUBLE_HORIZONTAL
                  + UI.SPACE
                  + UI.SYMBOL_ARROW_DOWN;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_SUMMARIZED_COMPUTED_DOWN", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_computed_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_down_computed_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      ALTITUDE_SUMMARIZED_COMPUTED_UP = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE
                  + UI.UNIT_LABEL_ALTITUDE
                  + UI.SYMBOL_DOUBLE_HORIZONTAL
                  + UI.SPACE
                  + UI.SYMBOL_ARROW_UP;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, //
                  "ALTITUDE_SUMMARIZED_COMPUTED_UP", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_computed_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_up_computed_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      /*
       * Body
       */

      BODY_AVG_PULSE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  BODY_AVG_PULSE_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
            colDef.setColumnLabel(Messages.ColumnFactory_avg_pulse_label);
            colDef.setColumnHeaderText(UI.SYMBOL_AVERAGE_WITH_SPACE + Messages.ColumnFactory_pulse);
            colDef.setColumnUnit(UI.SYMBOL_AVERAGE_WITH_SPACE + Messages.ColumnFactory_pulse);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pulse_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_1,
                  columnManager);

            return colDef;
         }
      };

      BODY_AVG_PULSE_DIFFERENCE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "BODY_AVG_PULSE_DIFFERENCE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
            colDef.setColumnLabel(Messages.ColumnFactory_avg_pulse_difference_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_pulse_difference);
            colDef.setColumnUnit(Messages.ColumnFactory_pulse_difference);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pulse_difference_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      BODY_CALORIES = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "BODY_CALORIES", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
            colDef.setColumnLabel(Messages.ColumnFactory_calories_label);
            colDef.setColumnHeaderText(Messages.Value_Unit_KCalories);
            colDef.setColumnUnit(Messages.Value_Unit_KCalories);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_calories_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      BODY_PULSE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "BODY_PULSE", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
            colDef.setColumnLabel(Messages.ColumnFactory_pulse_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_pulse);
            colDef.setColumnUnit(Messages.ColumnFactory_pulse);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_pulse_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_1,
                  columnManager);

            return colDef;
         }
      };

      /*
       * Data
       */

      DATA_FIRST_COLUMN = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "DATA_FIRST_COLUMN", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);

            colDef.setDefaultColumnWidth(0);

            return colDef;
         }
      };

      DATA_IMPORT_FILE_NAME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "DATA_IMPORT_FILE_NAME", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
            colDef.setColumnLabel(Messages.ColumnFactory_import_filename_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_import_filename);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_import_filename_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

            return colDef;
         }
      };

      DATA_IMPORT_FILE_PATH = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "DATA_IMPORT_FILE_PATH", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
            colDef.setColumnLabel(Messages.ColumnFactory_import_filepath_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_import_filepath);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_import_filepath_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

            return colDef;
         }
      };

      DATA_SERIE_START_END_INDEX = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  DATA_SERIE_START_END_INDEX_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
            colDef.setColumnLabel(Messages.ColumnFactory_SerieStartEndIndex_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_SerieStartEndIndex);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_SerieStartEndIndex_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

            return colDef;
         }
      };

      DATA_SEQUENCE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  DATA_SEQUENCE_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
            colDef.setColumnLabel(Messages.ColumnFactory_sequence_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_sequence);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      DATA_TIME_INTERVAL = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "DATA_TIME_INTERVAL", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
            colDef.setColumnLabel(Messages.ColumnFactory_time_interval_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_time_interval);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_time_interval_tooltip);
            colDef.setColumnUnit(Messages.ColumnFactory_time_interval);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      /*
       * Device
       */

      DEVICE_NAME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "DEVICE_NAME", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Device);
            colDef.setColumnLabel(Messages.ColumnFactory_device_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_device);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_device_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      DEVICE_PROFILE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "DEVICE_PROFILE", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Device);
            colDef.setColumnLabel(Messages.ColumnFactory_profile_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_profile);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_profile_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      /*
       * Marker
       */

      MARKER_MAP_VISIBLE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(8);

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "MARKER_MAP_VISIBLE", //$NON-NLS-1$
                  SWT.CENTER);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Marker);
            colDef.setColumnLabel(Messages.Tour_Marker_Column_IsVisible);
            colDef.setColumnHeaderText(Messages.Tour_Marker_Column_IsVisible);
            colDef.setColumnHeaderToolTipText(Messages.Tour_Marker_Column_IsVisibleNoEdit_Tooltip);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      MARKER_SERIE_INDEX = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "MARKER_SERIE_INDEX", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
            colDef.setColumnLabel(Messages.ColumnFactory_SerieIndex_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_SerieIndex);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_SerieIndex_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      MARKER_TIME_DELTA = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "MARKER_TIME_DELTA", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TimeDelta_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TimeDelta_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TimeDelta_Tooltip);
            colDef.setColumnUnit(UI.UNIT_LABEL_TIME);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

            return colDef;
         }
      };

      MARKER_URL = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "MARKER_URL", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Marker);
            colDef.setColumnLabel(Messages.ColumnFactory_Url_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Url_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Url_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(25));

            return colDef;
         }
      };

      /*
       * Motion
       */

      MOTION_ALTIMETER = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, MOTION_ALTIMETER_ID, SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnHeaderText(UI.UNIT_LABEL_ALTIMETER);
            colDef.setColumnUnit(UI.UNIT_LABEL_ALTIMETER);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Motion_Altimeter_Tooltip);
            colDef.setColumnLabel(Messages.ColumnFactory_Motion_Altimeter);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_0,
                  columnManager);

            return colDef;
         }
      };

      MOTION_AVG_PACE = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  MOTION_AVG_PACE_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_avg_pace_label);
            colDef.setColumnHeaderText(UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_PACE);
            colDef.setColumnUnit(UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_PACE);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pace_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

            return colDef;
         }
      };

      MOTION_AVG_PACE_DIFFERENCE = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "MOTION_AVG_PACE_DIFFERENCE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_avg_pace_difference_label);
            colDef.setColumnHeaderText(UI.SYMBOL_DIFFERENCE_WITH_SPACE + UI.UNIT_LABEL_PACE);
            colDef.setColumnUnit(UI.SYMBOL_DIFFERENCE_WITH_SPACE + UI.UNIT_LABEL_PACE);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pace_difference_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

            return colDef;
         }
      };

      MOTION_AVG_SPEED = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  MOTION_AVG_SPEED_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_avg_speed_label);
            colDef.setColumnHeaderText(UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_SPEED);
            colDef.setColumnUnit(UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_SPEED);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_speed_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_1,
                  columnManager);

            return colDef;
         }
      };

      MOTION_DISTANCE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  MOTION_DISTANCE_ID,
                  SWT.TRAIL);
            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(11);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_distance_label);
            colDef.setColumnHeaderText(UI.UNIT_LABEL_DISTANCE);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_distance_tooltip);
            colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_3,
                  columnManager);

            return colDef;
         }
      };

      MOTION_DISTANCE_DELTA = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String deltaDistance = UI.SYMBOL_DIFFERENCE_WITH_SPACE
                  + net.tourbook.common.UI.UNIT_LABEL_DISTANCE;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "MOTION_DISTANCE_DELTA", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_DistanceDelta_Label);
            colDef.setColumnHeaderText(deltaDistance);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_DistanceDelta_Tooltip);
            colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(11));

            return colDef;
         }
      };

      MOTION_DISTANCE_DIFF = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String header = Messages.ColumnFactory_Diff_Header + UI.SPACE + UI.UNIT_LABEL_DISTANCE;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "MOTION_DISTANCE_DIFF", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_TourDistanceDiff_Label);
            colDef.setColumnHeaderText(header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourDistanceDiff_Tooltip);
            colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      MOTION_DISTANCE_TOTAL = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "MOTION_DISTANCE_TOTAL", //$NON-NLS-1$
                  SWT.TRAIL);
            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(11);
            final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + UI.UNIT_LABEL_DISTANCE;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_distanceTotal_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_distanceTotal_tooltip);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_3,
                  columnManager);

            return colDef;
         }
      };

      MOTION_LATITUDE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "MOTION_LATITUDE", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_latitude_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_latitude);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_latitude_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

            return colDef;
         }
      };

      MOTION_LONGITUDE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "MOTION_LONGITUDE", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_longitude_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_longitude);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_longitude_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

            return colDef;
         }
      };

      MOTION_PACE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "MOTION_PACE", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_pace_label);
            colDef.setColumnHeaderText(UI.UNIT_LABEL_PACE);
            colDef.setColumnUnit(UI.UNIT_LABEL_PACE);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_pace_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      MOTION_SPEED = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "MOTION_SPEED", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_speed_label);
            colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
            colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_speed_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
            colDef.setValueFormats(ValueFormatSet.Number, ValueFormat.NUMBER_1_1, columnManager);

            return colDef;
         }
      };

      MOTION_SPEED_DIFF = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String header = Messages.ColumnFactory_Diff_Header + UI.SPACE + UI.UNIT_LABEL_SPEED;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "MOTION_SPEED_DIFF", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
            colDef.setColumnLabel(Messages.ColumnFactory_SpeedDiff_Label);
            colDef.setColumnHeaderText(header);
            colDef.setColumnUnit(header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_SpeedDiff_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      /*
       * Photo
       */

      PHOTO_NUMBER_OF_GPS_PHOTOS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "PHOTO_NUMBER_OF_GPS_PHOTOS", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Photo);
            colDef.setColumnLabel(Messages.ColumnFactory_NumberOfGPSPhotos_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfGPSPhotos_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfGPSPhotos_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      PHOTO_NUMBER_OF_NO_GPS_PHOTOS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "PHOTO_NUMBER_OF_NO_GPS_PHOTOS", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Photo);
            colDef.setColumnLabel(Messages.ColumnFactory_NumberOfNoGPSPhotos_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfNoGPSPhotos_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfNoGPSPhotos_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      PHOTO_NUMBER_OF_PHOTOS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "PHOTO_NUMBER_OF_PHOTOS", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Photo);
            colDef.setColumnLabel(Messages.ColumnFactory_NumberOfTourPhotos_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfTourPhotos_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfTourPhotos_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      PHOTO_TIME_ADJUSTMENT = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "PHOTO_TIME_ADJUSTMENT", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Photo);
            colDef.setColumnLabel(Messages.ColumnFactory_PhotoTimeAdjustment_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_PhotoTimeAdjustment_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_PhotoTimeAdjustment_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(11)); // 9 ... 54

            return colDef;
         }
      };

      PHOTO_TOUR_CAMERA = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "PHOTO_TOUR_CAMERA", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Photo);
            colDef.setColumnLabel(Messages.ColumnFactory_TourCamera_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourCamera_Label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourCamera_Label_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

            return colDef;
         }
      };

      /*
       * Power
       */

      POWER = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "POWER", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Power);
            colDef.setColumnLabel(Messages.ColumnFactory_power_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_power);
            colDef.setColumnUnit(Messages.ColumnFactory_power);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_power_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      /*
       * Powertrain
       */

      POWERTRAIN_AVG_CADENCE = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  POWERTRAIN_AVG_CADENCE_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
            colDef.setColumnLabel(Messages.ColumnFactory_avg_cadence_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_avg_cadence);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_cadence_tooltip);
            colDef.setColumnUnit(Messages.ColumnFactory_avg_cadence);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
            colDef.setValueFormats(//
                  ValueFormatSet.Number,
                  ValueFormat.NUMBER_1_1,
                  columnManager);

            return colDef;
         }
      };

      POWERTRAIN_CADENCE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "POWERTRAIN_CADENCE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
            colDef.setColumnLabel(Messages.ColumnFactory_cadence_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_cadence);
            colDef.setColumnUnit(Messages.ColumnFactory_cadence);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_cadence_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      POWERTRAIN_GEAR_RATIO = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "POWERTRAIN_GEAR_RATIO", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
            colDef.setColumnLabel(Messages.ColumnFactory_GearRatio_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_GearRatio_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_GearRatio_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

            return colDef;
         }
      };

      POWERTRAIN_GEAR_TEETH = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "POWERTRAIN_GEAR_TEETH", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
            colDef.setColumnLabel(Messages.ColumnFactory_GearTeeth_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_GearTeeth_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_GearTeeth_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      /*
       * Running dynamics
       */
      RUN_DYN_STEP_LENGTH_AVG = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final String headerLabel = UI.EMPTY_STRING
                  + UI.SYMBOL_AVERAGE
                  + UI.SPACE
                  + UI.SYMBOL_ARROW_LEFT_RIGHT
                  + UI.SPACE
                  + UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "RUN_DYN_STEP_LENGTH_AVG", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

            colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StepLength_Avg);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StepLength_Avg);
            colDef.setColumnHeaderText(headerLabel);

            colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE_MM_OR_INCH);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(11));

            if (UI.UNIT_IS_METRIC == false) {

               // imperial has 1 more digit

               colDef.setValueFormats(
                     ValueFormatSet.Number,
                     null,
                     ValueFormat.NUMBER_1_1,
                     columnManager);
            }

            return colDef;
         }
      };

      /*
       * State
       */

      STATE_DB_STATUS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "STATE_DB_STATUS", //$NON-NLS-1$
                  SWT.CENTER);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_State);
            colDef.setColumnLabel(Messages.ColumnFactory_db_status_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_db_status_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_db_status_tooltip);

            colDef.setDefaultColumnWidth(20);

            return colDef;
         }
      };

      STATE_IMPORT_STATE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "STATE_IMPORT_STATE", //$NON-NLS-1$
                  SWT.CENTER);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_State);
            colDef.setColumnLabel(Messages.ColumnFactory_ImportStatus_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_ImportStatus_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_ImportStatus_Tooltip);

            colDef.setDefaultColumnWidth(20);

            return colDef;
         }
      };

      /*
       * Swimming
       */
      SWIM__SWIM_CADENCE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "SWIM__SWIM_CADENCE", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Swimming);

            colDef.setColumnLabel(Messages.ColumnFactory_Swim_Cadence_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Swim_Cadence_Label);
//				colDef.setColumnUnit(Messages.ColumnFactory_Swim_Cadence_Label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Swim_Cadence_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      SWIM__SWIM_STROKES = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "SWIM__SWIM_STROKES", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Swimming);

            colDef.setColumnLabel(Messages.ColumnFactory_Swim_Strokes_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Swim_Strokes_Label);
//				colDef.setColumnUnit(Messages.ColumnFactory_Swim_Strokes_Label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Swim_Strokes_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      SWIM__SWIM_STROKE_STYLE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "SWIM__SWIM_STROKE_STYLE", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Swimming);

            colDef.setColumnLabel(Messages.ColumnFactory_Swim_StrokeStyle_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Swim_StrokeStyle_Label);
//				colDef.setColumnUnit(Messages.ColumnFactory_Swim_StrokeStyle_Label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Swim_StrokeStyle_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

            return colDef;
         }
      };

      SWIM__TIME_TOUR_TIME_DIFF = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "SWIM__TIME_TOUR_TIME_DIFF", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);

            colDef.setColumnLabel(Messages.ColumnFactory_TourTimeDiff_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourTimeDiff_Header);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourTimeDiff_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      SWIM__TIME_TOUR_TIME_HH_MM_SS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(12);

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "SWIM__TIME_TOUR_TIME_HH_MM_SS", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);

            colDef.setColumnLabel(Messages.ColumnFactory_tour_time_label_hhmmss);
            colDef.setColumnHeaderText(Messages.ColumnFactory_tour_time_label_hhmmss);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time_hhmmss);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_time_tooltip_hhmmss);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      SWIM__TIME_TOUR_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "SWIM__TIME_TOUR_TIME", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);

            colDef.setColumnLabel(Messages.ColumnFactory_tour_time_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_tour_time);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_time_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      SWIM__TIME_TOUR_TIME_OF_DAY_HH_MM_SS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(12);
            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "SWIM__TIME_TOUR_TIME_OF_DAY_HH_MM_SS", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);

            colDef.setColumnLabel(Messages.ColumnFactory_Tour_DayTime);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Tour_DayTime);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time_hhmmss);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Tour_DayTime_Tooltip);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      /*
       * Time
       */

      TIME_BREAK_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_BREAK_TIME", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_BreakTime_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_BreakTime_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_BreakTime_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(3));

            return colDef;
         }
      };

      TIME_TIME_ZONE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "TIME_TIME_ZONE", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TimeZone_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TimeZone_Header);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(25));

            return colDef;
         }
      };

      TIME_TIME_ZONE_DIFFERENCE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_TIME_ZONE_DIFFERENCE", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TimeZoneDifference_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TimeZoneDifference_Header);

// !!! THIS MUST BE SET IN THE VIEW TO SET THE CORRECT DEFAULT TIME ZONE !!!
//				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TimeZone_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      TIME_TOUR_TIME_DIFF = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "TIME_TOUR_TIME_DIFF", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TourTimeDiff_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourTimeDiff_Header);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourTimeDiff_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      TIME_TOUR_TIME_HH_MM_SS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(12);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_TOUR_TIME_HH_MM_SS", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_tour_time_label_hhmmss);
            colDef.setColumnHeaderText(Messages.ColumnFactory_tour_time_label_hhmmss);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time_hhmmss);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_time_tooltip_hhmmss);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      TIME_TOUR_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_TOUR_TIME", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_tour_time_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_tour_time);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_time_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      TIME_TOUR_TIME_OF_DAY_HH_MM_SS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(12);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_TOUR_TIME_OF_DAY_HH_MM_SS", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_Tour_DayTime);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Tour_DayTime);
            colDef.setColumnUnit(Messages.ColumnFactory_tour_time_hhmmss);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Tour_DayTime_Tooltip);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      TIME_DRIVING_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_DRIVING_TIME", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_driving_time_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_driving_time);
            colDef.setColumnUnit(Messages.ColumnFactory_driving_time);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_driving_time_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
            colDef.setValueFormats(//
                  ValueFormatSet.Time,
                  ValueFormat.TIME_HH_MM,
                  columnManager);

            return colDef;
         }
      };

      TIME_PAUSED_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_PAUSED_TIME", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_paused_time_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_paused_time);
            colDef.setColumnUnit(Messages.ColumnFactory_paused_time);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_paused_time_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
            colDef.setValueFormats(//
                  ValueFormatSet.Time,
                  ValueFormat.TIME_HH_MM,
                  columnManager);

            return colDef;
         }
      };

      TIME_RECORDING_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  TIME_RECORDING_TIME_ID,
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_recording_time_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_recording_time);
            colDef.setColumnUnit(Messages.ColumnFactory_recording_time);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_recording_time_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
            colDef.setValueFormats(//
                  ValueFormatSet.Time,
                  ValueFormat.TIME_HH_MM,
                  columnManager);

            return colDef;
         }
      };

      TIME_RECORDING_TIME_TOTAL = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_RECORDING_TIME_TOTAL", //$NON-NLS-1$
                  SWT.TRAIL);

            final String unitLabel = UI.SYMBOL_SUM_WITH_SPACE + Messages.ColumnFactory_recording_time;

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_recording_timeTotal_label);
            colDef.setColumnHeaderText(unitLabel);
            colDef.setColumnUnit(unitLabel);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_recording_timeTotal_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
            colDef.setValueFormats(//
                  ValueFormatSet.Time,
                  ValueFormat.TIME_HH_MM,
                  columnManager);

            return colDef;
         }
      };

      TIME_TOUR_DATE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_TOUR_DATE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnHeaderText(Messages.ColumnFactory_date);
            colDef.setColumnLabel(Messages.ColumnFactory_date_label);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

            return colDef;
         }
      };

      TIME_TOUR_DURATION_TIME = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "TIME_TOUR_DURATION_TIME", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TourDurationTime_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourDurationTime_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourDurationTime_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(15));

            return colDef;
         }
      };

      TIME_TOUR_START_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "TIME_TOUR_START_TIME", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TourStartTime_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourStartTime_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourStartTime_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

            colDef.setValueFormats(
                  ValueFormatSet.Time_mmss,
                  ValueFormat.TIME_HH_MM,
                  columnManager);

            return colDef;
         }
      };

      TIME_TOUR_END_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "TIME_TOUR_END_TIME", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TourEndTime_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourEndTime_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourEndTime_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

            return colDef;
         }
      };

      TIME_TOUR_START_DATE = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "TIME_TOUR_START_DATE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TourStartDate_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourStartDate_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourStartDate_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

            return colDef;
         }
      };

      TIME_TOUR_END_DATE = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "TIME_TOUR_END_DATE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_TourEndDate_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourEndDate_Header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourEndDate_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

            return colDef;
         }
      };

      /*
       * Tour
       */

      TOUR_MARKERS = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "TOUR_MARKERS", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
            colDef.setColumnLabel(Messages.ColumnFactory_tour_marker_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_tour_marker_header);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_marker_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

            return colDef;
         }
      };

      TOUR_MARKER = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "TOUR_MARKER", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
            colDef.setColumnLabel(Messages.ColumnFactory_marker_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_marker_label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_marker_label_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));
            colDef.setColumnWeightData(new ColumnWeightData(100, true));

            return colDef;
         }
      };

      TOUR_TAGS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "TOUR_TAGS", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
            colDef.setColumnLabel(Messages.ColumnFactory_tour_tag_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_tour_tag_label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_tag_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

            return colDef;
         }
      };

      TOUR_TITLE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "TOUR_TITLE", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
            colDef.setColumnLabel(Messages.ColumnFactory_tour_title_label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_tour_title);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_title_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(25));

            return colDef;
         }
      };

      TOUR_TYPE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "TOUR_TYPE", SWT.TRAIL); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
            colDef.setColumnLabel(Messages.ColumnFactory_tour_type_label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_type_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

            return colDef;
         }
      };

      TOUR_TYPE_TEXT = new TableColumnFactory() {
         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "TOUR_TYPE_TEXT", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
            colDef.setColumnHeaderText(Messages.ColumnFactory_TourTypeText_Header);
            colDef.setColumnLabel(Messages.ColumnFactory_TourTypeText_Label);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(18));

            return colDef;
         }
      };

      /*
       * Waypoint
       */

      WAYPOINT_ALTITUDE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(10);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "WAYPOINT_ALTITUDE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Altitude_Label);
            colDef.setColumnHeaderText(UI.UNIT_LABEL_ALTITUDE);
            colDef.setColumnUnit(UI.UNIT_LABEL_ALTITUDE);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Waypoint_Altitude_Label);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      WAYPOINT_CATEGORY = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "WAYPOINT_CATEGORY", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Waypoint);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Category);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Waypoint_Category);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      WAYPOINT_COMMENT = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "WAYPOINT_COMMENT", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Waypoint);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Comment);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Waypoint_Comment);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      WAYPOINT_DATE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(15);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "WAYPOINT_DATE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Date);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Waypoint_Date);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Waypoint_Date_Tooltip);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      WAYPOINT_DESCRIPTION = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "WAYPOINT_DESCRIPTION", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Waypoint);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Description);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Waypoint_Description);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      WAYPOINT_ID = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "WAYPOINT_ID", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Waypoint);
            colDef.setColumnLabel(Messages.ColumnFactory_Id_Label);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Id_Label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Id_Tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

            return colDef;
         }
      };

      WAYPOINT_NAME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager, "WAYPOINT_NAME", SWT.LEAD); //$NON-NLS-1$

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Waypoint);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Name);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Waypoint_Name);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      WAYPOINT_SYMBOL = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(30);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "WAYPOINT_SYMBOL", //$NON-NLS-1$
                  SWT.LEAD);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Waypoint);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Symbol);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Waypoint_Symbol);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      WAYPOINT_TIME = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final int pixelWidth = pixelConverter.convertWidthInCharsToPixels(15);
            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "WAYPOINT_TIME", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
            colDef.setColumnLabel(Messages.ColumnFactory_Waypoint_Time);
            colDef.setColumnHeaderText(Messages.ColumnFactory_Waypoint_Time);
            colDef.setColumnUnit(Messages.ColumnFactory_Waypoint_Time_Unit);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Waypoint_Time_Tooltip);

            colDef.setDefaultColumnWidth(pixelWidth);
            colDef.setColumnWeightData(new ColumnPixelData(pixelWidth, true));

            return colDef;
         }
      };

      /*
       * Weather
       */

      WEATHER_CLOUDS = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(
                  columnManager,
                  "WEATHER_CLOUDS", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Weather);
            colDef.setColumnLabel(Messages.ColumnFactory_clouds_label);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_clouds_tooltip);

            colDef.setDefaultColumnWidth(18);

            return colDef;
         }
      };

      WEATHER_TEMPERATURE = new TableColumnFactory() {

         @Override
         public TableColumnDefinition createColumn(final ColumnManager columnManager,
                                                   final PixelConverter pixelConverter) {

            final TableColumnDefinition colDef = new TableColumnDefinition(columnManager,
                  "WEATHER_TEMPERATURE", //$NON-NLS-1$
                  SWT.TRAIL);

            colDef.setColumnCategory(Messages.ColumnFactory_Category_Weather);
            colDef.setColumnLabel(Messages.ColumnFactory_temperature_label);
            colDef.setColumnHeaderText(UI.UNIT_LABEL_TEMPERATURE);
            colDef.setColumnUnit(UI.UNIT_LABEL_TEMPERATURE);
            colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_temperature_tooltip);

            colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
            colDef.setValueFormats(ValueFormatSet.Number, ValueFormat.NUMBER_1_1, columnManager);

            return colDef;
         }
      };
   }

   /**
    * @param columnManager
    * @param pixelConverter
    * @return Returns a {@link TableColumnDefinition}
    */
   public abstract TableColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter);
}
