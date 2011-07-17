/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.statistics;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.statistics.messages";	//$NON-NLS-1$

	public static String		LABEL_GRAPH_ALTITUDE;
	public static String		LABEL_GRAPH_DAYTIME;
	public static String		LABEL_GRAPH_DISTANCE;
	public static String		LABEL_GRAPH_TIME;
	public static String		LABEL_GRAPH_TIME_UNIT;

	public static String		numbers_info_altitude_between;
	public static String		numbers_info_altitude_down;
	public static String		numbers_info_altitude_total;
	public static String		numbers_info_altitude_up;
	public static String		numbers_info_distance_between;
	public static String		numbers_info_distance_down;
	public static String		numbers_info_distance_total;
	public static String		numbers_info_distance_up;
	public static String		numbers_info_time_between;
	public static String		numbers_info_time_down;
	public static String		numbers_info_time_total;
	public static String		numbers_info_time_up;

	public static String		NUMBERS_UNIT;

	public static String	Statistic_HrZone_Error_NoHrZoneInPerson;

	public static String	Statistic_HrZone_ErrorNoPerson;

	public static String	Statistic_Label_Invers;

	public static String		tourtime_info_altitude;
	public static String		tourtime_info_avg_pace;
	public static String		tourtime_info_avg_speed;
	public static String		tourtime_info_break_time;
	public static String		tourtime_info_break_time_tour;
	public static String		tourtime_info_date_month;
	public static String		tourtime_info_date_week;
	public static String		tourtime_info_date_year;
	public static String		TourTime_Info_DateDay;
	public static String		tourtime_info_description;
	public static String		tourtime_info_description_text;
	public static String		tourtime_info_distance;
	public static String		tourtime_info_distance_tour;
	public static String		tourtime_info_driving_time;
	public static String		tourtime_info_driving_time_tour;
	public static String		tourtime_info_recording_time;
	public static String		tourtime_info_recording_time_tour;
	public static String		tourtime_info_tags;
	public static String		tourtime_info_time;
	public static String		tourtime_info_tour_type;
	public static String		tourtime_info_week;

	public static String	UI_Label_PersonIsNotSelected;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
