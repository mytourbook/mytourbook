/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

	public static String	ACTION_ZOOM_INTO_MONTH;

	public static String	DLG_SELECT_TOUR_MSG;

	public static String	DLG_SELECT_TOUR_TITLE;

	public static String	FORMAT_HHMM_HHMM;

	public static String	LABEL_DRIVING_TIME;

	public static String	LABEL_GRAPH_ALTITUDE;

	public static String	LABEL_GRAPH_ALTITUDE_UNIT;

	public static String	LABEL_GRAPH_DAYTIME;

	public static String	LABEL_GRAPH_DISTANCE;

	public static String	LABEL_GRAPH_DISTANCE_UNIT;

	public static String	LABEL_GRAPH_TIME;

	public static String	LABEL_GRAPH_TIME_UNIT;

	public static String	NUMBERS_ALTITUDE_BETWEEN;

	public static String	NUMBERS_ALTITUDE_DOWN;

	public static String	NUMBERS_ALTITUDE_UNIT;

	public static String	NUMBERS_ALTITUDE_UP;

	public static String	NUMBERS_DISTANCE_BETWEEN;

	public static String	NUMBERS_DISTANCE_DOWN;

	public static String	NUMBERS_DISTANCE_UNIT;

	public static String	NUMBERS_DISTANCE_UP;

	public static String	NUMBERS_TIME_BETWEEN;

	public static String	NUMBERS_TIME_DOWN;

	public static String	NUMBERS_TIME_UNIT;

	public static String	NUMBERS_TIME_UP;

	public static String	NUMBERS_UNIT;

	public static String	TOURDAYINFO_ALTITUDE;

	public static String	TOURDAYINFO_DISTANCE;

	public static String	TOURDAYINFO_DURATION;

	public static String	TOURDAYINFO_TOUR_DATE_FORMAT;

	public static String	TOURTIMEINFO_ALTITUDE;

	public static String	TOURTIMEINFO_DATE_FORMAT;

	public static String	TOURTIMEINFO_DISTANCE;

	public static String	TOURTIMEINFO_DURATION;

	public static String	TOURTIMEINFO_TOUR_TYPE;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
	private Messages() {}
}
