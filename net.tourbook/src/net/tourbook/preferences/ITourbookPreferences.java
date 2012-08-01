/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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

import net.tourbook.ui.UI;

public interface ITourbookPreferences {

	/*
	 * dummy field used by field editors so they don't assert
	 */
	public static final String	DUMMY_FIELD									= UI.EMPTY_STRING;

	public static final String	GENERAL_NOTES								= "GENERAL_NOTES";											//$NON-NLS-1$

	/*
	 * system
	 */
	public static final String	TOUR_CACHE_SIZE								= "TourCacheSize";											//$NON-NLS-1$

	/*
	 * tour database
	 */
	public static final String	TOUR_DATABASE_IS_DERBY_EMBEDDED				= "TOUR_DATABASE_IS_DERBY_EMBEDDED";						//$NON-NLS-1$

	/*
	 * notifications
	 */
	public static final String	HR_ZONES_ARE_MODIFIED						= "HrZonesAreModified";									//$NON-NLS-1$

	/*
	 * statistic provider
	 */
	public static final String	STATISTICS_STATISTIC_PROVIDER_IDS			= "statistics.provider.ids";								//$NON-NLS-1$

	/*
	 * statistic: number frequency - where the units can be set in the preferences
	 */
	public static final String	STAT_DISTANCE_NUMBERS						= "statistic.distance.numbers";							//$NON-NLS-1$
	public static final String	STAT_DISTANCE_LOW_VALUE						= "statistic.distance.lowvalue";							//$NON-NLS-1$
	public static final String	STAT_DISTANCE_INTERVAL						= "statistic.distance.interval";							//$NON-NLS-1$

	public static final String	STAT_ALTITUDE_NUMBERS						= "statistic.altitude.numbers";							//$NON-NLS-1$
	public static final String	STAT_ALTITUDE_LOW_VALUE						= "statistic.altitude.lowvalue";							//$NON-NLS-1$
	public static final String	STAT_ALTITUDE_INTERVAL						= "statistic.altitude.interval";							//$NON-NLS-1$

	public static final String	STAT_DURATION_NUMBERS						= "statistic.duration.numbers";							//$NON-NLS-1$
	public static final String	STAT_DURATION_LOW_VALUE						= "statistic.duration.lowvalue";							//$NON-NLS-1$
	public static final String	STAT_DURATION_INTERVAL						= "statistic.duration.interval";							//$NON-NLS-1$

	/*
	 * colors
	 */
	public static final String	GRAPH_COLORS								= "graph.colors.";											//$NON-NLS-1$
	public static final String	GRAPH_COLORS_HAS_CHANGED					= "graph.colors.has-changed";								//$NON-NLS-1$

	/*
	 * Graphs
	 */
	public static final String	GRAPH_ALL									= "graphs.all";											//$NON-NLS-1$
	public static final String	GRAPH_VISIBLE								= "graphs.visible";										//$NON-NLS-1$
	public static final String	GRAPH_X_AXIS								= "graphs.x-axis";											//$NON-NLS-1$
	public static final String	GRAPH_X_AXIS_STARTTIME						= "graphs.x-axis.starttime";								//$NON-NLS-1$
	public static final String	GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE			= "Graph_HrZone_IsVisible";								//$NON-NLS-1$
	public static final String	GRAPH_HR_ZONE_STYLE							= "Graph_HrZone_GraphType";								//$NON-NLS-1$
	public static final String	GRAPH_TRANSPARENCY_LINE						= "Graph_Transparency_Line";								//$NON-NLS-1$
	public static final String	GRAPH_TRANSPARENCY_FILLING					= "Graph_Transparency_Filling";							//$NON-NLS-1$
	public static final String	GRAPH_ANTIALIASING							= "Graph_Antialiasing";									//$NON-NLS-1$

	public static final String	GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER				= "graphs.zoom.autozoom-to-slider";						//$NON-NLS-1$
	public static final String	GRAPH_MOVE_SLIDERS_WHEN_ZOOMED				= "graphs.move-sliders-when-zoomed";						//$NON-NLS-1$
	public static final String	GRAPH_IS_SRTM_VISIBLE						= "Graph_IsSRTMVisible";									//$NON-NLS-1$
	public static final String	GRAPH_IS_MARKER_VISIBLE						= "Graph_IsMarkerVisible";									//$NON-NLS-1$
	public static final String	GRAPH_IS_BREAKTIME_VALUES_VISIBLE			= "Graph_IsBreaktimeVisible";								//$NON-NLS-1$
	public static final String	GRAPH_IS_PHOTO_VISIBLE						= "GRAPH_IS_PHOTO_VISIBLE";								//$NON-NLS-1$

	public static final String	GRAPH_MOUSE_MODE							= "graph.toggle-mouse";									//$NON-NLS-1$

	public static final String	GRAPH_PACE_MINMAX_IS_ENABLED				= "graph.pace.is-min-enabled";								//$NON-NLS-1$
	public static final String	GRAPH_PACE_MIN_VALUE						= "graph.pace.min-value";									//$NON-NLS-1$
	public static final String	GRAPH_PACE_MAX_VALUE						= "graph.pace.max-value";									//$NON-NLS-1$
	public static final String	GRAPH_ALTIMETER_MIN_IS_ENABLED				= "graphs.altimeter.is-min-enabled";						//$NON-NLS-1$
	public static final String	GRAPH_ALTIMETER_MIN_VALUE					= "graphs.altimeter.min-value";							//$NON-NLS-1$
	public static final String	GRAPH_GRADIENT_MIN_IS_ENABLED				= "graphs.gradient.is-min-enabled";						//$NON-NLS-1$
	public static final String	GRAPH_GRADIENT_MIN_VALUE					= "graphs.gradient.min-value";								//$NON-NLS-1$

	public static final String	GRAPH_GRID_VERTICAL_DISTANCE				= "graphs.grid.vertical-distance";							//$NON-NLS-1$
	public static final String	GRAPH_GRID_HORIZONTAL_DISTANCE				= "graphs.grid.horizontal-distance";						//$NON-NLS-1$
	public static final String	GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES		= "GraphGrid_IsShowHorizontalGridlines";					//$NON-NLS-1$
	public static final String	GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES		= "GraphGrid_IsShowVerticalGridlines";						//$NON-NLS-1$

	public static final String	GRAPH_SMOOTHING_SMOOTHING_ALGORITHM			= "GraphSmoothing_SmoothingAlgorithm";						//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_IS_SYNCH_SMOOTHING	= "GraphJametSmoothing_IsSynchSmoothing";					//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_IS_ALTITUDE			= "GraphJametSmoothing_IsAltitudeSmoothing";				//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_IS_PULSE				= "GraphJametSmoothing_IsPulseSmoothing";					//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_GRADIENT_TAU			= "GraphJametSmoothing_GradientSmoothingTau";				//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_PULSE_TAU				= "GraphJametSmoothing_PulseSmoothingTau";					//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_SPEED_TAU				= "GraphJametSmoothing_SpeedSmoothingTau";					//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING	= "GraphJametSmoothing_RepeatedSmoothing";					//$NON-NLS-1$
	public static final String	GRAPH_JAMET_SMOOTHING_REPEATED_TAU			= "GraphJametSmoothing_RepeatedTau";						//$NON-NLS-1$

	/*
	 * 
	 */
	public static final String	GRAPH_PROPERTY_CHARTTYPE					= "graph.property.chartType";								//$NON-NLS-1$

	public static final String	GRAPH_PROPERTY_IS_VALUE_CLIPPING			= "graph.property.is.value_clipping";						//$NON-NLS-1$
	public static final String	GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE		= "graph.property.timeslice.value_clipping";				//$NON-NLS-1$

	public static final String	GRAPH_PROPERTY_IS_PACE_CLIPPING				= "graph.property.is.pace_clipping";						//$NON-NLS-1$
	public static final String	GRAPH_PROPERTY_PACE_CLIPPING_VALUE			= "graph.property.is.pace_clipping.value";					//$NON-NLS-1$

	public static final String	DEFAULT_IMPORT_TOUR_TYPE_ID					= "tourtype.import.default";								//$NON-NLS-1$

	public static final String	TOUR_TYPE_LIST_IS_MODIFIED					= "tourtype.list.is-modified";								//$NON-NLS-1$
	public static final String	TOUR_TYPE_FILTER_LIST						= "tourtype.filter.list";									//$NON-NLS-1$

	public static final String	TOUR_BIKE_LIST_IS_MODIFIED					= "tourbike.list.is-modified";								//$NON-NLS-1$
	public static final String	TOUR_PERSON_LIST_IS_MODIFIED				= "tourperson.list.is-modified";							//$NON-NLS-1$

	public static final String	APP_LAST_SELECTED_PERSON_ID					= "application.last-selected-person-id";					//$NON-NLS-1$
	public static final String	APP_LAST_SELECTED_TOUR_TYPE_FILTER			= "application.last-selected-tourtypefilter";				//$NON-NLS-1$

	/**
	 * event is fired when a person or a tour type is modified
	 */
	public static final String	APP_DATA_FILTER_IS_MODIFIED					= "application.data-filter-is-modified";					//$NON-NLS-1$

	public static final String	APP_DATA_SPEED_MIN_TIMESLICE_VALUE			= "application.data-speed-min-timeslice-value";			//$NON-NLS-1$

	/**
	 * initially this was an int value, with 2 it's a string
	 */
	public static final String	BREAK_TIME_METHOD2							= "BreakTime_Method2";										//$NON-NLS-1$

	public static final String	BREAK_TIME_IS_MODIFIED						= "BreakTime_IsModified";									//$NON-NLS-1$
	public static final String	BREAK_TIME_MIN_AVG_SPEED_AS					= "BreakTime_MinAvgSpeedAS";								//$NON-NLS-1$
	public static final String	BREAK_TIME_MIN_SLICE_SPEED_AS				= "BreakTime_MinSliceSpeedAS";								//$NON-NLS-1$
	public static final String	BREAK_TIME_MIN_SLICE_TIME_AS				= "BreakTime_MinSliceTimeAS";								//$NON-NLS-1$
	public static final String	BREAK_TIME_MIN_AVG_SPEED					= "BreakTime_MinAvgSpeed";									//$NON-NLS-1$
	public static final String	BREAK_TIME_MIN_SLICE_SPEED					= "BreakTime_MinSliceSpeed";								//$NON-NLS-1$
	public static final String	BREAK_TIME_SHORTEST_TIME					= "BreakTime_ShortestTime";								//$NON-NLS-1$
	public static final String	BREAK_TIME_MAX_DISTANCE						= "BreakTime_MaxDistance";									//$NON-NLS-1$
	public static final String	BREAK_TIME_SLICE_DIFF						= "BreakTime_SliceDiff";									//$NON-NLS-1$

	/*
	 * measurement system
	 */
	public static final String	MEASUREMENT_SYSTEM							= "system.of.measurement";									//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_SHOW_IN_UI				= "system.of.measurement.show.in.ui";						//$NON-NLS-1$

	public static final String	MEASUREMENT_SYSTEM_DISTANCE					= "system.of.measurement.distance";						//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_DISTANCE_KM				= "metric.km";												//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_DISTANCE_MI				= "imperial.mi";											//$NON-NLS-1$

	public static final String	MEASUREMENT_SYSTEM_ALTITUDE					= "system.of.measurement.altitude";						//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_ALTITUDE_M				= "metric.m";												//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_ALTITUDE_FOOT			= "imperial.foot";											//$NON-NLS-1$

	public static final String	MEASUREMENT_SYSTEM_TEMPERATURE				= "system.of.measurement.temperature";						//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_TEMPERATURE_C			= "metric.celcius";										//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_TEMPTERATURE_F			= "metric.fahrenheit";										//$NON-NLS-1$

	public static final String	MEASUREMENT_SYSTEM_ENERGY					= "system.of.energy";										//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_ENERGY_JOULE				= "energy.joule";											//$NON-NLS-1$
	public static final String	MEASUREMENT_SYSTEM_ENERGY_CALORIE			= "energy.calorie";										//$NON-NLS-1$

	/*
	 * map settings
	 */
	public static final String	MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING		= "map.view.confirmation.show-dim-warning";				//$NON-NLS-1$

	/*
	 * regional settings
	 */
	public static final String	REGIONAL_USE_CUSTOM_DECIMAL_FORMAT			= "regional_use.custom.decimal.format";					//$NON-NLS-1$
	public static final String	REGIONAL_DECIMAL_SEPARATOR					= "regional_decimal.separator";							//$NON-NLS-1$
	public static final String	REGIONAL_GROUP_SEPARATOR					= "regional_group.separator";								//$NON-NLS-1$

	/**
	 * layout for the views have been changed
	 */
	public static final String	VIEW_LAYOUT_CHANGED							= "view.layout.changed";									//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_DISPLAY_LINES					= "view.layout.display.lines";								//$NON-NLS-1$

	public static final String	VIEW_LAYOUT_COLOR_CATEGORY					= "view.layout.color.category";							//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_COLOR_TITLE						= "view.layout.color.title";								//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_COLOR_SUB						= "view.layout.color.sub";									//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_COLOR_SUB_SUB					= "view.layout.color.sub-sub";								//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_COLOR_TOUR						= "view.layout.color.tour";								//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_COLOR_BG_SEGMENTER_UP			= "view.layout.colorBg.segmenterUp";						//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_COLOR_BG_SEGMENTER_DOWN			= "view.layout.colorBg.segmenterDown";						//$NON-NLS-1$

	public static final String	VIEW_LAYOUT_RECORDING_TIME_FORMAT			= "view.layout.recording-time-format";						//$NON-NLS-1$
	public static final String	VIEW_LAYOUT_DRIVING_TIME_FORMAT				= "view.layout.driving-time-format";						//$NON-NLS-1$

	/*
	 * layout for maps
	 */
	public static final String	MAP_LAYOUT_SYMBOL							= "map.layout.symbol";										//$NON-NLS-1$
	public static final String	MAP_LAYOUT_SYMBOL_WIDTH						= "map.layout.symbol-width";								//$NON-NLS-1$
	public static final String	MAP_LAYOUT_DIM_COLOR						= "map.layout.dim-color";									//$NON-NLS-1$
	public static final String	MAP_LAYOUT_PAINT_WITH_BORDER				= "map.layout.paintWithBorder";							//$NON-NLS-1$
	public static final String	MAP_LAYOUT_BORDER_WIDTH						= "map.layout.borderWidth";								//$NON-NLS-1$
	public static final String	MAP_LAYOUT_TOUR_PAINT_METHOD				= "map.layout.tour-paint-algorithm";						//$NON-NLS-1$

	/*
	 * id's for preference pages
	 */
	public static final String	PREF_PAGE_TAGS								= "net.tourbook.preferences.PrefPageTags";					//$NON-NLS-1$
	public static final String	PREF_PAGE_TOUR_TYPE							= "net.tourbook.preferences.PrefPageTourTypeDefinition";	//$NON-NLS-1$
	public static final String	PREF_PAGE_TOUR_TYPE_FILTER					= "net.tourbook.preferences.PrefPageTourTypeFilter";		//$NON-NLS-1$

	/*
	 * tour data editor
	 */
	public static final String	TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR	= "tourdata.editor.confirmation.revert-tour";				//$NON-NLS-1$
	public static final String	TOUR_EDITOR_DESCRIPTION_HEIGHT				= "tourdata.editor.description-height";					//$NON-NLS-1$

	/*
	 * common appearance
	 */
	public static final String	APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES		= "appearance.NumberOfRecentTourTypes";					//$NON-NLS-1$
	public static final String	APPEARANCE_NUMBER_OF_RECENT_TAGS			= "appearance.number-of-recent-tags";						//$NON-NLS-1$
	public static final String	APPEARANCE_SHOW_MEMORY_MONITOR				= "appearance.show-memory-monitor";						//$NON-NLS-1$
	public static final String	APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU		= "appearance.ShowTourTypeContextMenuOnMouseHovering";		//$NON-NLS-1$

	public static final String	APPEARANCE_IS_TAGGING_AUTO_OPEN				= "Appearance.IsAutoOpenTagging";							//$NON-NLS-1$
	public static final String	APPEARANCE_IS_TAGGING_ANIMATION				= "Appearance.IsTaggingAnimation";							//$NON-NLS-1$
	public static final String	APPEARANCE_TAGGING_AUTO_OPEN_DELAY			= "Appearance.AutoOpenTaggingDelay";						//$NON-NLS-1$

	/*
	 * merge tour dialog
	 */
	public static final String	MERGE_TOUR_GRAPH_X_AXIS						= "merge.tour.chart-x-axis";								//$NON-NLS-1$

	public static final String	MERGE_TOUR_PREVIEW_CHART					= "merge.tour.preview-chart";								//$NON-NLS-1$
	public static final String	MERGE_TOUR_ALTITUDE_DIFF_SCALING			= "merge.tour.altitude-diff-scaling";						//$NON-NLS-1$

	public static final String	MERGE_TOUR_SYNC_START_TIME					= "merge.tour.synch-start-time";							//$NON-NLS-1$
	public static final String	MERGE_TOUR_ADJUST_START_ALTITUDE			= "merge.tour.adjust-start-altitude";						//$NON-NLS-1$

	public static final String	MERGE_TOUR_SET_TOUR_TYPE					= "merge.tour.set-tour-type-for-merge-from-tour";			//$NON-NLS-1$
	public static final String	MERGE_TOUR_SET_TOUR_TYPE_ID					= "merge.tour.set-tour-type-id";							//$NON-NLS-1$

	public static final String	MERGE_TOUR_LINEAR_INTERPOLATION				= "merge.tour.use-linear-interpolation";					//$NON-NLS-1$

	public static final String	MERGE_TOUR_MERGE_GRAPH_ALTITUDE				= "merge.tour.merge-graph-altitude";						//$NON-NLS-1$
	public static final String	MERGE_TOUR_MERGE_GRAPH_PULSE				= "merge.tour.merge-graph-pulse";							//$NON-NLS-1$
	public static final String	MERGE_TOUR_MERGE_GRAPH_TEMPERATURE			= "merge.tour.merge-graph-temperature";					//$NON-NLS-1$
	public static final String	MERGE_TOUR_MERGE_GRAPH_CADENCE				= "merge.tour.merge-graph-cadence";						//$NON-NLS-1$

	/*
	 * dialog: adjust altitude
	 */
	public static final String	ADJUST_ALTITUDE_CHART_X_AXIS_UNIT			= "adjust.altitude.x-axis-unit";							//$NON-NLS-1$

	/*
	 * calendar week
	 */
	public static final String	CALENDAR_WEEK_FIRST_DAY_OF_WEEK				= "calendar.week.first-day-of-week";						//$NON-NLS-1$
	public static final String	CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK		= "calendar.week.minimum-days-in-first-week";				//$NON-NLS-1$

	/*
	 * view tooltip
	 */
	public static final String	VIEW_TOOLTIP								= "view.tooltip.";											//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_IS_MODIFIED					= VIEW_TOOLTIP + "isModified";								//$NON-NLS-1$

	public static final String	VIEW_TOOLTIP_TOURBOOK_DATE					= VIEW_TOOLTIP + "tourbook.date";							//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURBOOK_TIME					= VIEW_TOOLTIP + "tourbook.time";							//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURBOOK_WEEKDAY				= VIEW_TOOLTIP + "tourbook.weekday";						//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURBOOK_TITLE					= VIEW_TOOLTIP + "tourbook.title";							//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURBOOK_TAGS					= VIEW_TOOLTIP + "tourbook.tags";							//$NON-NLS-1$

	public static final String	VIEW_TOOLTIP_TAGGING_TAG					= VIEW_TOOLTIP + "tagging.tag";							//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TAGGING_TAGS					= VIEW_TOOLTIP + "tagging.tags";							//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TAGGING_TITLE					= VIEW_TOOLTIP + "tagging.title";							//$NON-NLS-1$

	public static final String	VIEW_TOOLTIP_TOURCATALOG_REFTOUR			= VIEW_TOOLTIP + "tourcatalog.reftour";					//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURCATALOG_TAGS				= VIEW_TOOLTIP + "tourcatalog.tags";						//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURCATALOG_TITLE				= VIEW_TOOLTIP + "tourcatalog.title";						//$NON-NLS-1$

	public static final String	VIEW_TOOLTIP_TOURIMPORT_DATE				= VIEW_TOOLTIP + "tourimport.date";						//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURIMPORT_TIME				= VIEW_TOOLTIP + "tourimport.time";						//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURIMPORT_TAGS				= VIEW_TOOLTIP + "tourimport.tags";						//$NON-NLS-1$
	public static final String	VIEW_TOOLTIP_TOURIMPORT_TITLE				= VIEW_TOOLTIP + "tourimport.title";						//$NON-NLS-1$

	/*
	 * view actions
	 */
	public static final String	VIEW_DOUBLE_CLICK_ACTIONS					= "ViewDoubleClickActions";								//$NON-NLS-1$

	/*
	 * Dialog toggle states
	 */
	public static final String	TOGGLE_STATE_REIMPORT_TOUR					= "TOGGLE_STATE_REIMPORT_TOUR";							//$NON-NLS-1$
	public static final String	TOGGLE_STATE_REIMPORT_ALL_TIME_SLICES		= "TOGGLE_STATE_REIMPORT_ALL_TIME_SLICES";					//$NON-NLS-1$
	public static final String	TOGGLE_STATE_REIMPORT_ALTITUDE_VALUES		= "TOGGLE_STATE_REIMPORT_ALTITUDE_VALUES";					//$NON-NLS-1$

	/*
	 * Value point tool tip
	 */
	public static final String	VALUE_POINT_TOOL_TIP_IS_VISIBLE				= "VALUE_POINT_TOOL_TIP_IS_VISIBLE";						//$NON-NLS-1$
}
