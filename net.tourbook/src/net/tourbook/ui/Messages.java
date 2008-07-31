/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.ui.messages";		//$NON-NLS-1$

	public static String		Action_configure_columns;
	public static String		Action_configure_columns_tooltip;

	public static String		ColumnFactory_altitude_down_label;
	public static String		ColumnFactory_altitude_down_tooltip;
	public static String		ColumnFactory_altitude_label;
	public static String		ColumnFactory_altitude_tooltip;
	public static String		ColumnFactory_altitude_up_label;
	public static String		ColumnFactory_altitude_up_tooltip;
	public static String		ColumnFactory_avg_cadence;
	public static String		ColumnFactory_avg_cadence_label;
	public static String		ColumnFactory_avg_cadence_tooltip;
	public static String		ColumnFactory_avg_pulse;
	public static String		ColumnFactory_avg_pulse_label;
	public static String		ColumnFactory_avg_pulse_tooltip;

	public static String	ColumnFactory_avg_speed_label;

	public static String	ColumnFactory_avg_speed_tooltip;
	public static String		ColumnFactory_avg_temperature_label;
	public static String		ColumnFactory_avg_temperature_tooltip;
	public static String		ColumnFactory_cadence;
	public static String		ColumnFactory_cadence_label;
	public static String		ColumnFactory_cadence_tooltip;
	public static String		ColumnFactory_date;
	public static String		ColumnFactory_date_label;
	public static String		ColumnFactory_date_tooltip;
	public static String		ColumnFactory_db_status_label;
	public static String		ColumnFactory_db_status_tooltip;
	public static String		ColumnFactory_device;
	public static String		ColumnFactory_device_label;
	public static String		ColumnFactory_device_start_distance_label;
	public static String		ColumnFactory_device_start_distance_tooltip;
	public static String		ColumnFactory_device_tooltip;
	public static String		ColumnFactory_distance_label;
	public static String		ColumnFactory_distance_tooltip;
	public static String		ColumnFactory_driving_time;
	public static String		ColumnFactory_driving_time_label;
	public static String		ColumnFactory_driving_time_tooltip;
	public static String		ColumnFactory_gradient;
	public static String		ColumnFactory_gradient_label;
	public static String		ColumnFactory_gradient_tooltip;
	public static String		ColumnFactory_import_filename;
	public static String		ColumnFactory_import_filename_label;
	public static String		ColumnFactory_import_filename_tooltip;
	public static String		ColumnFactory_import_filepath;
	public static String		ColumnFactory_import_filepath_label;
	public static String		ColumnFactory_import_filepath_tooltip;
	public static String		ColumnFactory_latitude;
	public static String		ColumnFactory_latitude_label;
	public static String		ColumnFactory_latitude_tooltip;
	public static String		ColumnFactory_longitude;
	public static String		ColumnFactory_longitude_label;
	public static String		ColumnFactory_longitude_tooltip;
	public static String		ColumnFactory_max_altitude_label;
	public static String		ColumnFactory_max_altitude_tooltip;
	public static String		ColumnFactory_max_pulse;
	public static String		ColumnFactory_max_pulse_label;
	public static String		ColumnFactory_max_pulse_tooltip;
	public static String		ColumnFactory_max_speed_label;
	public static String		ColumnFactory_max_speed_tooltip;
	public static String		ColumnFactory_pace_label;
	public static String		ColumnFactory_pace_tooltip;
	public static String		ColumnFactory_profile;
	public static String		ColumnFactory_profile_label;
	public static String		ColumnFactory_profile_tooltip;
	public static String		ColumnFactory_pulse;
	public static String		ColumnFactory_pulse_label;
	public static String		ColumnFactory_pulse_tooltip;
	public static String		ColumnFactory_recording_time;
	public static String		ColumnFactory_recording_time_label;
	public static String		ColumnFactory_recording_time_tooltip;
	public static String		ColumnFactory_reference_tour;
	public static String		ColumnFactory_sequence;
	public static String		ColumnFactory_sequence_label;
	public static String		ColumnFactory_speed_label;
	public static String		ColumnFactory_speed_tooltip;
	public static String		ColumnFactory_tag;
	public static String		ColumnFactory_tag_label;
	public static String		ColumnFactory_tag_tooltip;
	public static String		ColumnFactory_temperature_label;
	public static String		ColumnFactory_temperature_tooltip;
	public static String		ColumnFactory_time;
	public static String		ColumnFactory_time_interval;
	public static String		ColumnFactory_time_interval_label;
	public static String		ColumnFactory_time_interval_tooltip;
	public static String		ColumnFactory_time_label;
	public static String		ColumnFactory_time_tooltip;
	public static String		ColumnFactory_tour_numbers;
	public static String		ColumnFactory_tour_numbers_lable;
	public static String		ColumnFactory_tour_numbers_tooltip;
	public static String		ColumnFactory_tour_tag_label;
	public static String		ColumnFactory_tour_tag_tooltip;
	public static String		ColumnFactory_tour_time;
	public static String		ColumnFactory_tour_time_label;
	public static String		ColumnFactory_tour_time_tooltip;
	public static String		ColumnFactory_tour_title;
	public static String		ColumnFactory_tour_title_label;
	public static String		ColumnFactory_tour_title_tooltip;
	public static String		ColumnFactory_tour_type_label;
	public static String		ColumnFactory_tour_type_tooltip;

	public static String	ColumnModifyDialog_Button_default;

	public static String		ColumnModifyDialog_Button_deselect_all;
	public static String		ColumnModifyDialog_Button_move_down;
	public static String		ColumnModifyDialog_Button_move_up;
	public static String		ColumnModifyDialog_Button_select_all;
	public static String		ColumnModifyDialog_Dialog_title;
	public static String		ColumnModifyDialog_Label_hint;
	public static String		ColumnModifyDialog_Label_info;

	public static String		Image_configure_columns;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
