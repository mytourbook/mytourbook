/*******************************************************************************
 * Copyright (C) 2005, 2010 Wolfgang Schramm and Contributors
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
import net.tourbook.common.util.ColumnManager;
import net.tourbook.common.util.TreeColumnDefinition;

import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;

public abstract class TreeColumnFactory {

	public static final TreeColumnFactory ALTITUDE_DOWN = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "altitudeDown", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = "\\ " + UI.UNIT_LABEL_ALTITUDE; //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_altitude_down_label);
			colDef.setColumnHeaderText(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_down_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory ALTITUDE_UP = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "altitudeUp", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = "/ " + UI.UNIT_LABEL_ALTITUDE; //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_altitude_up_label);
			colDef.setColumnHeaderText(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_altitude_up_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory AVG_CADENCE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgCadence", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_avg_cadence_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_avg_cadence);
			colDef.setColumnUnit(Messages.ColumnFactory_avg_cadence);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_cadence_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

			return colDef;
		};
	};

	public static final TreeColumnFactory AVG_PULSE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgPulse", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_avg_pulse_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_avg_pulse);
			colDef.setColumnUnit(Messages.ColumnFactory_avg_pulse);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pulse_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory AVG_PACE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgPace", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_PACE;

			colDef.setColumnLabel(Messages.ColumnFactory_avg_pace_label);
			colDef.setColumnHeaderText(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_pace_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

			return colDef;
		};
	};

	public static final TreeColumnFactory AVG_SPEED = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgSpeed", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_SPEED;

			colDef.setColumnLabel(Messages.ColumnFactory_avg_speed_label);
			colDef.setColumnHeaderText(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_speed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory AVG_TEMPERATURE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "avgTemperature", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = UI.SYMBOL_AVERAGE_WITH_SPACE + UI.UNIT_LABEL_TEMPERATURE;

			colDef.setColumnLabel(Messages.ColumnFactory_avg_temperature_label);
			colDef.setColumnHeaderText(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_avg_temperature_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

			return colDef;
		};
	};

	public static final TreeColumnFactory WIND_SPEED = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "weatherWindSpeed", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_wind_speed_label);
			colDef.setColumnHeaderText(UI.SYMBOL_WIND_WITH_SPACE + UI.UNIT_LABEL_SPEED);
			colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_wind_speed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(11));

			return colDef;
		};
	};

	public static final TreeColumnFactory WIND_DIR = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "weatherWindDir", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = UI.UNIT_LABEL_DIRECTION;

			colDef.setColumnLabel(Messages.ColumnFactory_wind_dir_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_wind_dir);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_wind_dir_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(7));

			return colDef;
		};
	};

	public static final TreeColumnFactory CLOUDS = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "weatherCouds", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_clouds_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_clouds);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_clouds_tooltip);
			colDef.setDefaultColumnWidth(25);

			return colDef;
		};
	};

	public static final TreeColumnFactory RESTPULSE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "restPulse", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_restpulse_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_restpulse);
			colDef.setColumnUnit(Messages.ColumnFactory_restpulse);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_restpulse_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(7));

			return colDef;
		};
	};
	
	public static final TreeColumnFactory CALORIES = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "calories", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_calories_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_calories);
			colDef.setColumnUnit(Messages.ColumnFactory_calories);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_calories_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory DATE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "date", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_date_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_date);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_date_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(18));

			return colDef;
		};
	};

	public static final TreeColumnFactory DEVICE_DISTANCE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "deviceDistance", SWT.TRAIL); //$NON-NLS-1$

			final String unit = UI.UNIT_LABEL_DISTANCE +" * 1000"; //$NON-NLS-1$
			colDef.setColumnLabel(Messages.ColumnFactory_device_start_distance_label);
			colDef.setColumnHeaderText(unit);
			colDef.setColumnUnit(unit);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_device_start_distance_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(13));

			return colDef;
		};
	};

	public static final TreeColumnFactory DEVICE_NAME = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "deviceName", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_device_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_device);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_device_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory DISTANCE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "distance", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_distance_label);
			colDef.setColumnHeaderText(UI.UNIT_LABEL_DISTANCE);
			colDef.setColumnUnit(UI.UNIT_LABEL_DISTANCE);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_distance_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

			return colDef;
		};
	};
	
	public static final TreeColumnFactory DP_TOLERANCE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "DPTolerance", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_DPTolerance_Label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_DPTolerance_Header);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_DPTolerance_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(6));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory DRIVING_TIME = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "drivingTime", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_driving_time_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_driving_time);
			colDef.setColumnUnit(Messages.ColumnFactory_driving_time);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_driving_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory MAX_ALTITUDE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "maxAltitude", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = "^" + UI.UNIT_LABEL_ALTITUDE; //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_max_altitude_label);
			colDef.setColumnHeaderText(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_max_altitude_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

			return colDef;
		};
	};

	public static final TreeColumnFactory MAX_PULSE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "maxPulse", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_max_pulse_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_max_pulse);
			colDef.setColumnUnit(Messages.ColumnFactory_max_pulse);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_max_pulse_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

			return colDef;
		};
	};

	public static final TreeColumnFactory MAX_SPEED = new TreeColumnFactory() {

		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "maxSpeed", SWT.TRAIL); //$NON-NLS-1$
			final String unitLabel = "^" + UI.UNIT_LABEL_SPEED; //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_max_speed_label);
			colDef.setColumnHeaderText(unitLabel);
			colDef.setColumnUnit(unitLabel);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_max_speed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory PAUSED_TIME = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "pausedTime", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_paused_time_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_paused_time);
			colDef.setColumnUnit(Messages.ColumnFactory_paused_time);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_paused_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory PAUSED_TIME_RELATIVE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "pausedTimeRelative", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_paused_time_relative_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_paused_relative_time);
			colDef.setColumnUnit(Messages.ColumnFactory_paused_relative_time);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_paused_time_relative_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory PERSON = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourPerson", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_TourPerson);
			colDef.setColumnHeaderText(Messages.ColumnFactory_TourPerson);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourPerson_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

			return colDef;
		};
	};

	public static final TreeColumnFactory RECORDING_TIME = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "recordingTime", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_recording_time_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_recording_time);
			colDef.setColumnUnit(Messages.ColumnFactory_recording_time);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_recording_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(10));

			return colDef;
		};
	};

	public static final TreeColumnFactory	REF_TOUR	= new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "referenceTour", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_reference_tour);
			colDef.setColumnHeaderText(Messages.ColumnFactory_reference_tour);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

			return colDef;
		};
	};

	public static final TreeColumnFactory SPEED = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "speed", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_speed_label);
			colDef.setColumnHeaderText(UI.UNIT_LABEL_SPEED);
			colDef.setColumnUnit(UI.UNIT_LABEL_SPEED);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_speed_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(9));

			return colDef;
		};
	};

	public static final TreeColumnFactory TAG = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager,
																			"tag", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_tag_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_tag);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tag_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

			return colDef;
		};
	};

	public static final TreeColumnFactory TIME_INTERVAL = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "timeInterval", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_time_interval_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_time_interval);
			colDef.setColumnUnit(Messages.ColumnFactory_time_interval);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_time_interval_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));

			return colDef;
 		};
	};

	public static final TreeColumnFactory TIME_SLICES = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "NumberOfTimeSlices", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_NumberOfTimeSlices_Label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfTimeSlices_Header);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfTimeSlices_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			return colDef;
		};
	};
	
	public static final TreeColumnFactory TITLE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourTitle", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_tour_title);
			colDef.setColumnHeaderText(Messages.ColumnFactory_tour_title);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_title_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_COUNTER = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourCounter", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnHeaderText(Messages.ColumnFactory_tour_numbers);
			colDef.setColumnLabel(Messages.ColumnFactory_tour_numbers_lable);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_numbers_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

			return colDef;
		};
	};

	public static final TreeColumnFactory	TOUR_TAGS	= new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourTags", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_tour_tag_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_tour_tag_label);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_tag_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(20));

			return colDef;
		};
	};

	public static final TreeColumnFactory	TOUR_MARKERS	= new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourMarkers", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_tour_marker_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_tour_marker_header);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_marker_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_PHOTOS = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "NumberOfTourPhotos", SWT.TRAIL); //$NON-NLS-1$
			
			colDef.setColumnLabel(Messages.ColumnFactory_NumberOfPhotos_Label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_NumberOfPhotos_Header);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_NumberOfPhotos_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(8));
			
			return colDef;
		};
	};
 
	public static final TreeColumnFactory TOUR_START_TIME = new TreeColumnFactory() {

		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {


			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourStartTime", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_time_label);
			colDef.setColumnHeaderText(Messages.ColumnFactory_time);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_time_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(12));

			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_TYPE = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourType", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnLabel(Messages.ColumnFactory_tour_type_label);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_type_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

			return colDef;
		};
	};

	public static final TreeColumnFactory TOUR_TYPE_TEXT = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {
			
			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourTypeText", SWT.LEAD); //$NON-NLS-1$
			
			colDef.setColumnHeaderText(Messages.ColumnFactory_TourTypeText_Header);
			colDef.setColumnLabel(Messages.ColumnFactory_TourTypeText_Label);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(18));
			
			return colDef;
		};
	};

	public static final TreeColumnFactory WEEK_DAY = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourWeekDay", SWT.LEAD); //$NON-NLS-1$

			colDef.setColumnHeaderText(Messages.ColumnFactory_Tour_WeekDay_Header);
			colDef.setColumnLabel(Messages.ColumnFactory_Tour_WeekDay_Label);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_Tour_WeekDay_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(5));

			return colDef;
		};
	};

	public static final TreeColumnFactory WEEK_NO = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourWeek", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnHeaderText(Messages.ColumnFactory_tour_week_header);
			colDef.setColumnLabel(Messages.ColumnFactory_tour_week_label);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_tour_week_tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(7));

			return colDef;
		};
	};

	public static final TreeColumnFactory WEEKYEAR = new TreeColumnFactory() {
		@Override
		public TreeColumnDefinition createColumn(final ColumnManager columnManager, final PixelConverter pixelConverter) {

			final TreeColumnDefinition colDef = new TreeColumnDefinition(columnManager, "tourWeekYear", SWT.TRAIL); //$NON-NLS-1$

			colDef.setColumnHeaderText(Messages.ColumnFactory_TourWeekYear_Header);
			colDef.setColumnLabel(Messages.ColumnFactory_TourWeekYear_Label);
			colDef.setColumnHeaderToolTipText(Messages.ColumnFactory_TourWeekYear_Tooltip);
			colDef.setDefaultColumnWidth(pixelConverter.convertWidthInCharsToPixels(14));

			return colDef;
		};
	};



	public abstract TreeColumnDefinition createColumn(ColumnManager columnManager, PixelConverter pixelConverter);
}
