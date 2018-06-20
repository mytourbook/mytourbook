/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.TreeColumnDefinition;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;

public abstract class TreeColumnFactory {

// SET_FORMATTING_OFF
	
//	private static final String	GRAPH_LABEL_RUN_DYN_STANCE_TIME					= net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime;
//	private static final String	GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCED		= net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalance;
//	private static final String	GRAPH_LABEL_RUN_DYN_STEP_LENGTH					= net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength;
//	private static final String	GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION		= net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation;
//	private static final String	GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO				= net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio;

//	Graph_Label_RunDyn_StanceTime            = Stance Time
//	Graph_Label_RunDyn_StanceTimeBalance    = Stance Time Balance
//	Graph_Label_RunDyn_StepLength            = Step Length
//	Graph_Label_RunDyn_VerticalOscillation   = Vertical Oscillation
//	Graph_Label_RunDyn_VerticalRatio         = Vertical Ratio
	
// SET_FORMATTING_ON

	public static final TreeColumnFactory	ALTITUDE_DOWN;
	public static final TreeColumnFactory	ALTITUDE_UP;
	public static final TreeColumnFactory	ALTITUDE_MAX;

	public static final TreeColumnFactory	BODY_CALORIES;
	public static final TreeColumnFactory	BODY_PERSON;
	public static final TreeColumnFactory	BODY_PULSE_AVG;
	public static final TreeColumnFactory	BODY_PULSE_MAX;
	public static final TreeColumnFactory	BODY_RESTPULSE;
	public static final TreeColumnFactory	BODY_WEIGHT;

	public static final TreeColumnFactory	DATA_DP_TOLERANCE;
	public static final TreeColumnFactory	DATA_IMPORT_FILE_NAME;
	public static final TreeColumnFactory	DATA_IMPORT_FILE_PATH;
	public static final TreeColumnFactory	DATA_NUM_TIME_SLICES;
	public static final TreeColumnFactory	DATA_NUM_TOURS;
	public static final TreeColumnFactory	DATA_TIME_INTERVAL;

	public static final TreeColumnFactory	DEVICE_DISTANCE;
	public static final TreeColumnFactory	DEVICE_NAME;

	public static final TreeColumnFactory	POWER_AVG;
	public static final TreeColumnFactory	POWER_MAX;
	public static final TreeColumnFactory	POWER_NORMALIZED;
	public static final TreeColumnFactory	POWER_TOTAL_WORK;

	public static final TreeColumnFactory	POWERTRAIN_AVG_CADENCE;
	public static final TreeColumnFactory	POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS;
	public static final TreeColumnFactory	POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS;
	public static final TreeColumnFactory	POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS;
	public static final TreeColumnFactory	POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS;
	public static final TreeColumnFactory	POWERTRAIN_CADENCE_MULTIPLIER;
	public static final TreeColumnFactory	POWERTRAIN_GEAR_FRONT_SHIFT_COUNT;
	public static final TreeColumnFactory	POWERTRAIN_GEAR_REAR_SHIFT_COUNT;
	public static final TreeColumnFactory	POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE;

	public static final TreeColumnFactory	RUN_DYN_STANCE_TIME_MIN;
	public static final TreeColumnFactory	RUN_DYN_STANCE_TIME_MAX;
	public static final TreeColumnFactory	RUN_DYN_STANCE_TIME_AVG;
	public static final TreeColumnFactory	RUN_DYN_STANCE_TIME_BALANCE_MIN;
	public static final TreeColumnFactory	RUN_DYN_STANCE_TIME_BALANCE_MAX;
	public static final TreeColumnFactory	RUN_DYN_STANCE_TIME_BALANCE_AVG;
	public static final TreeColumnFactory	RUN_DYN_STEP_LENGTH_MIN;
	public static final TreeColumnFactory	RUN_DYN_STEP_LENGTH_MAX;
	public static final TreeColumnFactory	RUN_DYN_STEP_LENGTH_AVG;
	public static final TreeColumnFactory	RUN_DYN_VERTICAL_OSCILLATION_MIN;
	public static final TreeColumnFactory	RUN_DYN_VERTICAL_OSCILLATION_MAX;
	public static final TreeColumnFactory	RUN_DYN_VERTICAL_OSCILLATION_AVG;
	public static final TreeColumnFactory	RUN_DYN_VERTICAL_RATIO_MIN;
	public static final TreeColumnFactory	RUN_DYN_VERTICAL_RATIO_MAX;
	public static final TreeColumnFactory	RUN_DYN_VERTICAL_RATIO_AVG;

	public static final TreeColumnFactory	TIME_DATE;
	public static final TreeColumnFactory	TIME_DRIVING_TIME;
	public static final TreeColumnFactory	TIME_PAUSED_TIME;
	public static final TreeColumnFactory	TIME_PAUSED_TIME_RELATIVE;
	public static final TreeColumnFactory	TIME_RECORDING_TIME;
	public static final TreeColumnFactory	TIME_TIME_ZONE;
	public static final TreeColumnFactory	TIME_TIME_ZONE_DIFFERENCE;
	public static final TreeColumnFactory	TIME_TOUR_START_TIME;
	public static final TreeColumnFactory	TIME_WEEK_DAY;
	public static final TreeColumnFactory	TIME_WEEK_NO;
	public static final TreeColumnFactory	TIME_WEEKYEAR;

	public static final TreeColumnFactory	MOTION_AVG_PACE;
	public static final TreeColumnFactory	MOTION_AVG_SPEED;
	public static final TreeColumnFactory	MOTION_DISTANCE;
	public static final TreeColumnFactory	MOTION_MAX_SPEED;

	public static final TreeColumnFactory	TOUR_REFTOUR_TOUR;

	public static final TreeColumnFactory	TOUR_COLLATE_EVENT;
	public static final TreeColumnFactory	TOUR_COUNTER;
	public static final TreeColumnFactory	TOUR_NUM_MARKERS;
	public static final TreeColumnFactory	TOUR_NUM_PHOTOS;
	public static final TreeColumnFactory	TOUR_TAG_AND_TAGS;
	public static final TreeColumnFactory	TOUR_TAGS;
	public static final TreeColumnFactory	TOUR_TITLE;
	public static final TreeColumnFactory	TOUR_TYPE;
	public static final TreeColumnFactory	TOUR_TYPE_TEXT;

	public static final TreeColumnFactory	TRAINING_INTENSITY_FACTOR;
	public static final TreeColumnFactory	TRAINING_FTP;
	public static final TreeColumnFactory	TRAINING_POWER_TO_WEIGHT;
	public static final TreeColumnFactory	TRAINING_STRESS_SCORE;

	public static final TreeColumnFactory	WEATHER_AVG_TEMPERATURE;
	public static final TreeColumnFactory	WEATHER_CLOUDS;
	public static final TreeColumnFactory	WEATHER_WIND_DIR;
	public static final TreeColumnFactory	WEATHER_WIND_SPEED;

	static {

		/*
		 * Altitude
		 */

		ALTITUDE_DOWN = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "ALTITUDE_DOWN", SWT.TRAIL); //$NON-NLS-1$
				final String unitLabel = UI.UNIT_LABEL_ALTITUDE + UI.SPACE + UI.SYMBOL_ARROW_DOWN;

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
				colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_label);
				colDef.setColumnHeaderText(unitLabel);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_down_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

				return colDef;
			};
		};

		ALTITUDE_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "ALTITUDE_MAX", SWT.TRAIL); //$NON-NLS-1$
				final String unitLabel = "^" + UI.UNIT_LABEL_ALTITUDE; //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
				colDef.setColumnLabel(Messages.ColumnFactory_max_altitude_label);
				colDef.setColumnHeaderText(unitLabel);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_max_altitude_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		ALTITUDE_UP = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "ALTITUDE_UP", SWT.TRAIL); //$NON-NLS-1$
				final String unitLabel = UI.UNIT_LABEL_ALTITUDE + UI.SPACE + UI.SYMBOL_ARROW_UP;

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Altitude);
				colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_label);
				colDef.setColumnHeaderText(unitLabel);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_up_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

				return colDef;
			};
		};

		/*
		 * Body
		 */

		BODY_CALORIES = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "BODY_CALORIES", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
				colDef.setColumnLabel(Messages.ColumnFactory_calories_label);
				colDef.setColumnHeaderText(Messages.Value_Unit_KCalories);
				colDef.setColumnUnit(Messages.Value_Unit_KCalories);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_calories_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_0,
						columnManager);

				return colDef;
			};
		};

		BODY_PERSON = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "BODY_PERSON", SWT.LEAD); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
				colDef.setColumnLabel(Messages.ColumnFactory_TourPerson);
				colDef.setColumnHeaderText(Messages.ColumnFactory_TourPerson);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourPerson_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

				return colDef;
			};
		};

		BODY_PULSE_AVG = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "BODY_PULSE_AVG", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
				colDef.setColumnLabel(Messages.ColumnFactory_avg_pulse_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_avg_pulse);
				colDef.setColumnUnit(Messages.ColumnFactory_avg_pulse);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pulse_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		BODY_PULSE_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "BODY_PULSE_MAX", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
				colDef.setColumnLabel(Messages.ColumnFactory_max_pulse_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_max_pulse);
				colDef.setColumnUnit(Messages.ColumnFactory_max_pulse);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_max_pulse_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

				return colDef;
			};
		};

		BODY_RESTPULSE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "BODY_RESTPULSE", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
				colDef.setColumnLabel(Messages.ColumnFactory_restpulse_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_restpulse);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_restpulse_tooltip);
				colDef.setColumnUnit(Messages.ColumnFactory_restpulse);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(7));

				return colDef;
			};
		};

		BODY_WEIGHT = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "BODY_WEIGHT", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Body);
				colDef.setColumnLabel(Messages.ColumnFactory_BodyWeight_Label);
				colDef.setColumnHeaderText(UI.UNIT_WEIGHT_KG);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_BodyWeight_Tooltip);
				colDef.setColumnUnit(UI.UNIT_WEIGHT_KG);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(7));

				return colDef;
			};
		};

		/*
		 * Data
		 */

		DATA_DP_TOLERANCE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"DATA_DP_TOLERANCE", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
				colDef.setColumnLabel(Messages.ColumnFactory_DPTolerance_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_DPTolerance_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_DPTolerance_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(6));

				return colDef;
			};
		};

		DATA_IMPORT_FILE_NAME = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"DATA_IMPORT_FILE_NAME", //$NON-NLS-1$
						SWT.LEAD);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
				colDef.setColumnLabel(Messages.ColumnFactory_import_filename_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_import_filename);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_import_filename_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

				return colDef;
			};
		};

		DATA_IMPORT_FILE_PATH = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"DATA_IMPORT_FILE_PATH", //$NON-NLS-1$
						SWT.LEAD);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
				colDef.setColumnLabel(Messages.ColumnFactory_import_filepath);
				colDef.setColumnHeaderText(Messages.ColumnFactory_import_filepath);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_import_filepath_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

				return colDef;
			};
		};

		DATA_NUM_TIME_SLICES = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"DATA_NUM_TIME_SLICES", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
				colDef.setColumnLabel(Messages.ColumnFactory_NumberOfTimeSlices_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfTimeSlices_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfTimeSlices_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		DATA_NUM_TOURS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "DATA_NUM_TOURS", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
				colDef.setColumnLabel(Messages.ColumnFactory_NumberOfTours_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfTours_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfTours_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		DATA_TIME_INTERVAL = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"DATA_TIME_INTERVAL", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Data);
				colDef.setColumnLabel(Messages.ColumnFactory_time_interval_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_time_interval);
				colDef.setColumnUnit(Messages.ColumnFactory_time_interval);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_time_interval_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		/*
		 * Device
		 */

		DEVICE_DISTANCE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"DEVICE_DISTANCE", //$NON-NLS-1$
						SWT.TRAIL);

				final String unit = UI.UNIT_LABEL_DISTANCE + " * 1000"; //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Device);
				colDef.setColumnLabel(Messages.ColumnFactory_device_start_distance_label);
				colDef.setColumnHeaderText(unit);
				colDef.setColumnUnit(unit);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_device_start_distance_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(13));

				return colDef;
			};
		};

		DEVICE_NAME = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "DEVICE_NAME", SWT.LEAD); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Device);
				colDef.setColumnLabel(Messages.ColumnFactory_device_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_device);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_device_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

				return colDef;
			};
		};

		/*
		 * Motion
		 */

		MOTION_AVG_PACE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"MOTION_AVG_PACE", //$NON-NLS-1$
						SWT.TRAIL);
				final String unitLabel = UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_PACE;

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
				colDef.setColumnLabel(Messages.ColumnFactory_avg_pace_label);
				colDef.setColumnHeaderText(unitLabel);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pace_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

				return colDef;
			};
		};

		MOTION_AVG_SPEED = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"MOTION_AVG_SPEED", //$NON-NLS-1$
						SWT.TRAIL);
				final String unitLabel = UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_SPEED;

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
				colDef.setColumnLabel(Messages.ColumnFactory_avg_speed_label);
				colDef.setColumnHeaderText(unitLabel);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_speed_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		MOTION_DISTANCE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"MOTION_DISTANCE", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
				colDef.setColumnLabel(Messages.ColumnFactory_distance_label);
				colDef.setColumnHeaderText(UI.UNIT_LABEL_DISTANCE);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_distance_tooltip);
				colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_3,
						columnManager);

				return colDef;
			};
		};

		MOTION_MAX_SPEED = new TreeColumnFactory() {

			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"MOTION_MAX_SPEED", //$NON-NLS-1$
						SWT.TRAIL);
				final String unitLabel = "^" + UI.UNIT_LABEL_SPEED; //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Motion);
				colDef.setColumnLabel(Messages.ColumnFactory_max_speed_label);
				colDef.setColumnHeaderText(unitLabel);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_max_speed_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		/*
		 * POWER
		 */
		POWER_AVG = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWER_AVG", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Power);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_Avg_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_Avg_Header);
				colDef.setColumnUnit(Messages.ColumnFactory_power);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_Avg_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		POWER_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWER_MAX", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Power);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_Max_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_Max_Header);
				colDef.setColumnUnit(Messages.ColumnFactory_power);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_Max_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_0,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		POWER_NORMALIZED = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWER_NORMALIZED", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Power);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_Normalized_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_Normalized_Header);
				colDef.setColumnUnit(Messages.ColumnFactory_power);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_Normalized_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

				return colDef;
			};
		};

		POWER_TOTAL_WORK = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWER_TOTAL_WORK", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Power);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_TotalWork_Tooltip);
				colDef.setColumnHeaderText(UI.UNIT_JOULE_MEGA);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_TotalWork_Tooltip);
				colDef.setColumnUnit(UI.UNIT_JOULE_MEGA);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_2,
						ValueFormat.NUMBER_1_3,
						columnManager);

				return colDef;
			};
		};

		/*
		 * Powertrain
		 */

		POWERTRAIN_AVG_CADENCE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"POWERTRAIN_AVG_CADENCE", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_avg_cadence_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_avg_cadence);
				colDef.setColumnUnit(Messages.ColumnFactory_avg_cadence);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_cadence_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWERTRAIN_AVG_LEFT_PEDAL_SMOOTHNESS", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_AvgLeftPedalSmoothness_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_AvgLeftPedalSmoothness_Header);
				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_AvgLeftPedalSmoothness_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(6));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWERTRAIN_AVG_RIGHT_PEDAL_SMOOTHNESS", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_AvgRightPedalSmoothness_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_AvgRightPedalSmoothness_Header);
				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_AvgRightPedalSmoothness_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(6));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWERTRAIN_AVG_LEFT_TORQUE_EFFECTIVENESS", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_AvgLeftTorqueEffectiveness_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_AvgLeftTorqueEffectiveness_Header);
				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_AvgLeftTorqueEffectiveness_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(6));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWERTRAIN_AVG_RIGHT_TORQUE_EFFECTIVENESS", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_AvgRightTorqueEffectiveness_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_AvgRightTorqueEffectiveness_Header);
				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_AvgRightTorqueEffectiveness_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(6));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		POWERTRAIN_CADENCE_MULTIPLIER = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(
						columnManager,
						"POWERTRAIN_CADENCE_MULTIPLIER", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_CadenceMultiplier_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_CadenceMultiplier_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_CadenceMultiplier_Tooltip);
				colDef.setColumnUnit(Messages.ColumnFactory_CadenceMultiplier_Unit);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(4));

				return colDef;
			};
		};

		POWERTRAIN_GEAR_FRONT_SHIFT_COUNT = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(
						columnManager,
						"POWERTRAIN_GEAR_FRONT_SHIFT_COUNT", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_GearFrontShiftCount_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_GearFrontShiftCount_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_GearFrontShiftCount_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		POWERTRAIN_GEAR_REAR_SHIFT_COUNT = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(
						columnManager,
						"POWERTRAIN_GEAR_REAR_SHIFT_COUNT", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_GearRearShiftCount_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_GearRearShiftCount_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_GearRearShiftCount_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"POWERTRAIN_PEDAL_LEFT_RIGHT_BALANCE", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Powertrain);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_LeftRightBalance_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_LeftRightBalance_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_LeftRightBalance_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

				return colDef;
			};
		};

		/*
		 * Running Dynamics
		 */

		RUN_DYN_STANCE_TIME_MIN = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STANCE_TIME_MIN", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StanceTime_Min);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StanceTime_Min);
				colDef.setColumnHeaderText(Messages.ColumnFactory_RunDyn_StanceTime_Min_Header);

				colDef.setColumnUnit(UI.UNIT_MS);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		RUN_DYN_STANCE_TIME_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STANCE_TIME_MAX", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StanceTime_Max);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StanceTime_Max);
				colDef.setColumnHeaderText(Messages.ColumnFactory_RunDyn_StanceTime_Max_Header);

				colDef.setColumnUnit(UI.UNIT_MS);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		RUN_DYN_STANCE_TIME_AVG = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STANCE_TIME_AVG", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StanceTime_Avg);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StanceTime_Avg);
				colDef.setColumnHeaderText(Messages.ColumnFactory_RunDyn_StanceTime_Avg_Header);

				colDef.setColumnUnit(UI.UNIT_MS);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		RUN_DYN_STANCE_TIME_BALANCE_MIN = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STANCE_TIME_BALANCE_MIN", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Min);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Min);
				colDef.setColumnHeaderText(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Min_Header);

				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						null,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		RUN_DYN_STANCE_TIME_BALANCE_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STANCE_TIME_BALANCE_MAX", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Max);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Max);
				colDef.setColumnHeaderText(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Max_Header);

				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						null,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		RUN_DYN_STANCE_TIME_BALANCE_AVG = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STANCE_TIME_BALANCE_AVG", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Avg);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Avg);
				colDef.setColumnHeaderText(Messages.ColumnFactory_RunDyn_StanceTimeBalance_Avg_Header);

				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						null,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		RUN_DYN_STEP_LENGTH_MIN = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_MIN
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_LEFT_RIGHT
						+ UI.SPACE
						+ UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STEP_LENGTH_MIN", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StepLength_Min);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StepLength_Min);
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
			};
		};

		RUN_DYN_STEP_LENGTH_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_MAX
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_LEFT_RIGHT
						+ UI.SPACE
						+ UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_STEP_LENGTH_MAX", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_StepLength_Max);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_StepLength_Max);
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
			};
		};

		RUN_DYN_STEP_LENGTH_AVG = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_AVERAGE
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_LEFT_RIGHT
						+ UI.SPACE
						+ UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
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
			};
		};

		RUN_DYN_VERTICAL_OSCILLATION_MIN = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_MIN
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_UP_DOWN
						+ UI.SPACE
						+ UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_VERTICAL_OSCILLATION_MIN", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_VerticalOscillation_Min);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_VerticalOscillation_Min);
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
			};
		};

		RUN_DYN_VERTICAL_OSCILLATION_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_MAX
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_UP_DOWN
						+ UI.SPACE
						+ UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_VERTICAL_OSCILLATION_MAX", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_VerticalOscillation_Max);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_VerticalOscillation_Max);
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
			};
		};

		RUN_DYN_VERTICAL_OSCILLATION_AVG = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_AVERAGE
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_UP_DOWN
						+ UI.SPACE
						+ UI.UNIT_LABEL_DISTANCE_MM_OR_INCH;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_VERTICAL_OSCILLATION_AVG", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_VerticalOscillation_Avg);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_VerticalOscillation_Avg);
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
			};
		};

		RUN_DYN_VERTICAL_RATIO_MIN = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_MIN
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_UP_DOWN
						+ UI.SPACE
						+ UI.SYMBOL_PERCENTAGE;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_VERTICAL_RATIO_MIN", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_VerticalRatio_Min);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_VerticalRatio_Min);
				colDef.setColumnHeaderText(headerLabel);

				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						null,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		RUN_DYN_VERTICAL_RATIO_MAX = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_MAX
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_UP_DOWN
						+ UI.SPACE
						+ UI.SYMBOL_PERCENTAGE;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_VERTICAL_RATIO_MAX", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_VerticalRatio_Max);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_VerticalRatio_Max);
				colDef.setColumnHeaderText(headerLabel);

				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						null,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		RUN_DYN_VERTICAL_RATIO_AVG = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final String headerLabel = UI.EMPTY_STRING
						+ UI.SYMBOL_AVERAGE
						+ UI.SPACE
						+ UI.SYMBOL_ARROW_UP_DOWN
						+ UI.SPACE
						+ UI.SYMBOL_PERCENTAGE;

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"RUN_DYN_VERTICAL_RATIO_AVG", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_RunDyn);

				colDef.setColumnLabel(Messages.ColumnFactory_RunDyn_VerticalRatio_Avg);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_RunDyn_VerticalRatio_Avg);
				colDef.setColumnHeaderText(headerLabel);

				colDef.setColumnUnit(UI.SYMBOL_PERCENTAGE);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						null,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		/*
		 * Time
		 */

		TIME_DATE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TIME_DATE", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnLabel(Messages.ColumnFactory_date_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_date);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_date_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(18));

				return colDef;
			};
		};

		TIME_DRIVING_TIME = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
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
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						columnManager);

				return colDef;
			};
		};

		TIME_PAUSED_TIME = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
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
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						columnManager);

				return colDef;
			};
		};

		TIME_PAUSED_TIME_RELATIVE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(
						columnManager,
						"TIME_PAUSED_TIME_RELATIVE", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnLabel(Messages.ColumnFactory_paused_time_relative_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_paused_relative_time);
				colDef.setColumnUnit(Messages.ColumnFactory_paused_relative_time);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_paused_time_relative_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

				return colDef;
			};
		};

		TIME_RECORDING_TIME = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"TIME_RECORDING_TIME", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnLabel(Messages.ColumnFactory_recording_time_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_recording_time);
				colDef.setColumnUnit(Messages.ColumnFactory_recording_time);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_recording_time_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));
				colDef.setValueFormats(//
						ValueFormatSet.Time,
						ValueFormat.TIME_HH,
						ValueFormat.TIME_HH_MM,
						columnManager);

				return colDef;
			};
		};

		TIME_TIME_ZONE = new TreeColumnFactory() {

			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"TIME_TIME_ZONE", //$NON-NLS-1$
						SWT.LEAD);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnLabel(Messages.ColumnFactory_TimeZone_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_TimeZone_Header);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(25));

				return colDef;
			};
		};

		TIME_TIME_ZONE_DIFFERENCE = new TreeColumnFactory() {

			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(
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
			};
		};

		TIME_TOUR_START_TIME = new TreeColumnFactory() {

			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"TIME_TOUR_START_TIME", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnLabel(Messages.ColumnFactory_time_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_time);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_time_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

				return colDef;
			};
		};

		TIME_WEEK_DAY = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TIME_WEEK_DAY", SWT.LEAD); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Tour_WeekDay_Header);
				colDef.setColumnLabel(Messages.ColumnFactory_Tour_WeekDay_Label);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Tour_WeekDay_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

				return colDef;
			};
		};

		TIME_WEEK_NO = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TIME_WEEK_NO", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnHeaderText(Messages.ColumnFactory_tour_week_header);
				colDef.setColumnLabel(Messages.ColumnFactory_tour_week_label);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_week_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(7));

				return colDef;
			};
		};

		TIME_WEEKYEAR = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TIME_WEEKYEAR", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Time);
				colDef.setColumnHeaderText(Messages.ColumnFactory_TourWeekYear_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourWeekYear_Tooltip);
				colDef.setColumnLabel(Messages.ColumnFactory_TourWeekYear_Label);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

				return colDef;
			};
		};

		/*
		 * Tour
		 */

		TOUR_COLLATE_EVENT = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"TOUR_COLLATE_EVENT", //$NON-NLS-1$
						SWT.LEAD);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_CollateEvent_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_CollateEvent_Label);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_CollateEvent_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(18));

				return colDef;
			};
		};

		TOUR_COUNTER = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TOUR_COUNTER", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnHeaderText(Messages.ColumnFactory_tour_numbers);
				colDef.setColumnLabel(Messages.ColumnFactory_tour_numbers_lable);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_numbers_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

				return colDef;
			};
		};

		TOUR_NUM_MARKERS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"TOUR_NUM_MARKERS", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_tour_marker_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_tour_marker_header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_marker_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

				return colDef;
			};
		};

		TOUR_NUM_PHOTOS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"TOUR_NUM_PHOTOS", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_NumberOfPhotos_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfPhotos_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfPhotos_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

				return colDef;
			};
		};

		TOUR_REFTOUR_TOUR = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(
						columnManager,
						"TOUR_REFTOUR_TOUR", //$NON-NLS-1$
						SWT.LEAD);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_reference_tour);
				colDef.setColumnHeaderText(Messages.ColumnFactory_reference_tour);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

				return colDef;
			};
		};

		TOUR_TAG_AND_TAGS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"TOUR_TAG_AND_TAGS", //$NON-NLS-1$
						SWT.LEAD);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_tag_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_tag);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tag_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

				return colDef;
			};
		};

		TOUR_TAGS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TOUR_TAGS", SWT.LEAD); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_tour_tag_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_tour_tag_label);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_tag_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

				return colDef;
			};
		};

		TOUR_TITLE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TOUR_TITLE", SWT.LEAD); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_tour_title);
				colDef.setColumnHeaderText(Messages.ColumnFactory_tour_title);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_title_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

				return colDef;
			};
		};

		TOUR_TYPE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TOUR_TYPE", SWT.LEAD); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnLabel(Messages.ColumnFactory_tour_type_label);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_type_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

				return colDef;
			};
		};

		TOUR_TYPE_TEXT = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "TOUR_TYPE_TEXT", SWT.LEAD); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Tour);
				colDef.setColumnHeaderText(Messages.ColumnFactory_TourTypeText_Header);
				colDef.setColumnLabel(Messages.ColumnFactory_TourTypeText_Label);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(18));

				return colDef;
			};
		};

		/*
		 * Training
		 */

		TRAINING_FTP = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"TRAINING_FTP", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Training);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_FTP_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_FTP_Header);
				colDef.setColumnUnit(Messages.ColumnFactory_power);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_FTP_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

				return colDef;
			};
		};

		TRAINING_INTENSITY_FACTOR = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"TRAINING_INTENSITY_FACTOR", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Training);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_IntensityFactor_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_IntensityFactor_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_IntensityFactor_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_2,
						ValueFormat.NUMBER_1_2,
						columnManager);

				return colDef;
			};
		};

		TRAINING_POWER_TO_WEIGHT = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"TRAINING_POWER_TO_WEIGHT", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Training);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_PowerToWeight_Tooltip);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_PowerToWeight_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_PowerToWeight_Tooltip);
				colDef.setColumnUnit(UI.UNIT_POWER_TO_WEIGHT_RATIO);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_2,
						ValueFormat.NUMBER_1_2,
						columnManager);

				return colDef;
			};
		};

		TRAINING_STRESS_SCORE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(//
						columnManager,
						"TRAINING_STRESS_SCORE", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Training);
				colDef.setColumnLabel(Messages.ColumnFactory_Power_TrainingStressScore_Label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_Power_TrainingStressScore_Header);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Power_TrainingStressScore_Tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		/*
		 * Weather
		 */

		WEATHER_AVG_TEMPERATURE = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"WEATHER_AVG_TEMPERATURE", //$NON-NLS-1$
						SWT.TRAIL);

				final String unitLabel = UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_TEMPERATURE;

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Weather);
				colDef.setColumnLabel(Messages.ColumnFactory_avg_temperature_label);
				colDef.setColumnHeaderText(unitLabel);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_temperature_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
				colDef.setValueFormats(
						ValueFormatSet.Number,
						ValueFormat.NUMBER_1_1,
						ValueFormat.NUMBER_1_1,
						columnManager);

				return colDef;
			};
		};

		WEATHER_CLOUDS = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "WEATHER_CLOUDS", SWT.TRAIL); //$NON-NLS-1$

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Weather);
				colDef.setColumnLabel(Messages.ColumnFactory_clouds_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_clouds);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_clouds_tooltip);

				colDef.setDefaultColumnWidth(25);

				return colDef;
			};
		};

		WEATHER_WIND_DIR = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"WEATHER_WIND_DIR", //$NON-NLS-1$
						SWT.TRAIL);

				final String unitLabel = UI.UNIT_LABEL_DIRECTION;

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Weather);
				colDef.setColumnLabel(Messages.ColumnFactory_wind_dir_label);
				colDef.setColumnHeaderText(Messages.ColumnFactory_wind_dir);
				colDef.setColumnUnit(unitLabel);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_wind_dir_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(7));

				return colDef;
			};
		};

		WEATHER_WIND_SPEED = new TreeColumnFactory() {
			@Override
			public TreeColumnDefinition createColumn(	final ColumnManager columnManager,
														final PixelConverter pixelConverter) {

				final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
						"WEATHER_WIND_SPEED", //$NON-NLS-1$
						SWT.TRAIL);

				colDef.setColumnCategory(Messages.ColumnFactory_Category_Weather);
				colDef.setColumnLabel(Messages.ColumnFactory_wind_speed_label);
				colDef.setColumnHeaderText(UI.SYMBOL_WIND_WITH_SPACE + UI.UNIT_LABEL_SPEED);
				colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
				colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_wind_speed_tooltip);

				colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(11));

				return colDef;
			};
		};
	}

	public abstract TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter);
}
