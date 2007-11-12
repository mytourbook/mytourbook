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
package net.tourbook.preferences;

public interface ITourbookPreferences {

	/*
	 * dummy field used by field editors so they don't assert
	 */
	public static final String	DUMMY_FIELD								= "";											//$NON-NLS-1$

	/*
	 * general page
	 */
	public static final String	MEASUREMENT_SYSTEM						= "system.of.measurement";						//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_METRIC				= "metric";									//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_IMPERIAL				= "imperial";									//$NON-NLS-1$

	/*
	 * statistics where the units can be set in the preferences
	 */
	public static final String	STAT_DISTANCE_NUMBERS					= "statistic.distance.numbers";				//$NON-NLS-1$
	public static final String	STAT_DISTANCE_LOW_VALUE					= "statistic.distance.lowvalue";				//$NON-NLS-1$
	public static final String	STAT_DISTANCE_INTERVAL					= "statistic.distance.interval";				//$NON-NLS-1$

	public static final String	STAT_ALTITUDE_NUMBERS					= "statistic.altitude.numbers";				//$NON-NLS-1$
	public static final String	STAT_ALTITUDE_LOW_VALUE					= "statistic.altitude.lowvalue";				//$NON-NLS-1$
	public static final String	STAT_ALTITUDE_INTERVAL					= "statistic.altitude.interval";				//$NON-NLS-1$

	public static final String	STAT_DURATION_NUMBERS					= "statistic.duration.numbers";				//$NON-NLS-1$
	public static final String	STAT_DURATION_LOW_VALUE					= "statistic.duration.lowvalue";				//$NON-NLS-1$
	public static final String	STAT_DURATION_INTERVAL					= "statistic.duration.interval";				//$NON-NLS-1$

	/*
	 * colors
	 */
	public static final String	GRAPH_COLORS							= "graph.colors.";								//$NON-NLS-1$
	public static final String	GRAPH_COLORS_HAS_CHANGED				= "graph.colors.has-changed";					//$NON-NLS-1$

	/*
	 * Graphs
	 */
	public static final String	GRAPH_ALL								= "graphs.all";								//$NON-NLS-1$
	public static final String	GRAPH_VISIBLE							= "graphs.visible";							//$NON-NLS-1$
	public static final String	GRAPH_X_AXIS							= "graphs.x-axis";								//$NON-NLS-1$
	public static final String	GRAPH_X_AXIS_STARTTIME					= "graphs.x-axis.starttime";					//$NON-NLS-1$

	public static final String	GRAPH_ZOOM_SCROLL_ZOOMED_GRAPH			= "graphs.zoom.scroll-zoomed-graph";			//$NON-NLS-1$
	public static final String	GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER			= "graphs.zoom.autozoom-to-slider";			//$NON-NLS-1$

	public static final String	GRAPH_ALTIMETER_MIN_ENABLED				= "graphs.altimeter.is-min-enabled";			//$NON-NLS-1$
	public static final String	GRAPH_ALTIMETER_MIN_VALUE				= "graphs.altimeter.min-value";				//$NON-NLS-1$

	public static final String	GRAPH_GRADIENT_MIN_ENABLED				= "graphs.gradient.is-min-enabled";			//$NON-NLS-1$
	public static final String	GRAPH_GRADIENT_MIN_VALUE				= "graphs.gradient.min-value";					//$NON-NLS-1$

	public static final String	DEFAULT_IMPORT_TOUR_TYPE_ID				= "tourtype.import.default";					//$NON-NLS-1$

	public static final String	TOUR_TYPE_LIST_IS_MODIFIED				= "tourtype.list.is-modified";					//$NON-NLS-1$
	public static final String	TOUR_TYPE_FILTER_LIST					= "tourtype.filter.list";						//$NON-NLS-1$

	public static final String	TOUR_BIKE_LIST_IS_MODIFIED				= "tourbike.list.is-modified";					//$NON-NLS-1$
	public static final String	TOUR_PERSON_LIST_IS_MODIFIED			= "tourperson.list.is-modified";				//$NON-NLS-1$

	public static final String	APP_LAST_SELECTED_PERSON_ID				= "application.last-selected-person-id";		//$NON-NLS-1$
	public static final String	APP_LAST_SELECTED_TOUR_TYPE_FILTER		= "application.last-selected-tourtypefilter";	//$NON-NLS-1$

	/**
	 * event is fired when a person or a tour type is modified
	 */
	public static final String	APP_DATA_FILTER_IS_MODIFIED				= "application.data-filter-is-modified";		//$NON-NLS-1$

	public static final String	GRAPH_PROPERTY_CHARTTYPE				= "graph.property.chartType";					//$NON-NLS-1$

	public static final String	GRAPH_PROPERTY_IS_COMPUTE_VALUE			= "graph.property.is.compute-value";			//$NON-NLS-1$
	public static final String	GRAPH_PROPERTY_TIMESLICE_COMPUTE_VALUE	= "graph.property.timeslice.compute-value";	//$NON-NLS-1$

	public static final String	GRAPH_PROPERTY_IS_CLIP_VALUE			= "graph.property.is.clip-value";				//$NON-NLS-1$
	public static final String	GRAPH_PROPERTY_TIMESLICE_CLIP_VALUE		= "graph.property.timeslice.clip-value";		//$NON-NLS-1$

}
