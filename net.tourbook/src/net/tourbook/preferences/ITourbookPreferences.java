/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.preferences;

public interface ITourbookPreferences {

	/*
	 * dummy field used by field editors so they don't assert
	 */
	public static final String	DUMMY_FIELD						= "";

	// hidden preferences
	public static final String	LAST_RAW_DATA_FILE_PATH			= "data.lastRawDataFilePath";

	/*
	 * statistics where the units can be set in the preferences
	 */
	public static final String	STAT_DISTANCE_NUMBERS			= "statistic.distance.numbers";
	public static final String	STAT_DISTANCE_LOW_VALUE			= "statistic.distance.lowvalue";
	public static final String	STAT_DISTANCE_INTERVAL			= "statistic.distance.interval";

	public static final String	STAT_ALTITUDE_NUMBERS			= "statistic.altitude.numbers";
	public static final String	STAT_ALTITUDE_LOW_VALUE			= "statistic.altitude.lowvalue";
	public static final String	STAT_ALTITUDE_INTERVAL			= "statistic.altitude.interval";

	public static final String	STAT_DURATION_NUMBERS			= "statistic.duration.numbers";
	public static final String	STAT_DURATION_LOW_VALUE			= "statistic.duration.lowvalue";
	public static final String	STAT_DURATION_INTERVAL			= "statistic.duration.interval";

	/*
	 * colors
	 */
	public static final String	GRAPH_COLORS					= "graph.colors.";
	public static final String	GRAPH_COLORS_HAS_CHANGED		= "graph.colors.has-changed";

	/*
	 * Graphs
	 */
	public static final String	GRAPH_ALL						= "graphs.all";
	public static final String	GRAPH_VISIBLE					= "graphs.visible";
	public static final String	GRAPH_X_AXIS					= "graphs.x-axis";
	public static final String	GRAPH_X_AXIS_STARTTIME			= "graphs.x-axis.starttime";

	public static final String	GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH	= "graphs.zoom.scroll-zoomed-graph";
	public static final String	GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER	= "graphs.zoom.autozoom-to-slider";

	public static final String	GRAPH_ALTIMETER_MIN_ENABLED		= "graphs.altimeter.is-min-enabled";
	public static final String	GRAPH_ALTIMETER_MIN_VALUE		= "graphs.altimeter.min-value";

	public static final String	GRAPH_GRADIENT_MIN_ENABLED		= "graphs.gradient.is-min-enabled";
	public static final String	GRAPH_GRADIENT_MIN_VALUE		= "graphs.gradient.min-value";

	public static final String	DEFAULT_IMPORT_TOUR_TYPE_ID		= "tourtype.import.default";

	public static final String	TOUR_TYPE_LIST_IS_MODIFIED		= "tourtype.list.is-modified";
	public static final String	TOUR_BIKE_LIST_IS_MODIFIED		= "tourbike.list.is-modified";
	public static final String	TOUR_PERSON_LIST_IS_MODIFIED	= "tourperson.list.is-modified";

	public static final String	APP_LAST_SELECTED_PERSON_ID		= "application.last-selected-person-id";
	public static final String	APP_LAST_SELECTED_TOUR_TYPE_ID	= "application.last-selected-tourtype-id";
	public static final String	APP_NEW_DATA_FILTER				= "application.new-data-filter";

}
