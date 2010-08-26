/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

import java.util.Calendar;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.colors.ColorDefinition;
import net.tourbook.colors.GraphColorProvider;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.util.StringToArrayConverter;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.RGB;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();

		/*
		 * regional settings
		 */
		store.setDefault(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI, true);
		store.setDefault(
				ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE,
				ITourbookPreferences.MEASUREMENT_SYSTEM_DISTANCE_KM);

		store.setDefault(
				ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE,
				ITourbookPreferences.MEASUREMENT_SYSTEM_ALTITUDE_M);

		store.setDefault(
				ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE,
				ITourbookPreferences.MEASUREMENT_SYSTEM_TEMPERATURE_C);

		store.setDefault(
				ITourbookPreferences.MEASUREMENT_SYSTEM_ENERGY,
				ITourbookPreferences.MEASUREMENT_SYSTEM_ENERGY_JOULE);

		store.setDefault(ITourbookPreferences.REGIONAL_USE_CUSTOM_DECIMAL_FORMAT, false);
		store.setDefault(ITourbookPreferences.REGIONAL_DECIMAL_SEPARATOR, "."); //$NON-NLS-1$
		store.setDefault(ITourbookPreferences.REGIONAL_GROUP_SEPARATOR, "'"); //$NON-NLS-1$

		/*
		 * statistics
		 */
		store.setDefault(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE, 0);
		store.setDefault(ITourbookPreferences.STAT_ALTITUDE_INTERVAL, 250);
		store.setDefault(ITourbookPreferences.STAT_ALTITUDE_NUMBERS, 10);

		store.setDefault(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE, 0);
		store.setDefault(ITourbookPreferences.STAT_DISTANCE_INTERVAL, 10);
		store.setDefault(ITourbookPreferences.STAT_DISTANCE_NUMBERS, 10);

		store.setDefault(ITourbookPreferences.STAT_DURATION_LOW_VALUE, 0);
		store.setDefault(ITourbookPreferences.STAT_DURATION_INTERVAL, 60);
		store.setDefault(ITourbookPreferences.STAT_DURATION_NUMBERS, 10);

		/*
		 * graph color preferences
		 */
		for (final ColorDefinition graphDefinition : GraphColorProvider.getInstance().getGraphColorDefinitions()) {

			final String graphPrefName = graphDefinition.getGraphPrefName();

			PreferenceConverter.setDefault(
					store,
					graphPrefName + GraphColorProvider.PREF_COLOR_BRIGHT,
					graphDefinition.getDefaultGradientBright());

			PreferenceConverter.setDefault(
					store,
					graphPrefName + GraphColorProvider.PREF_COLOR_DARK,
					graphDefinition.getDefaultGradientDark());

			PreferenceConverter.setDefault(
					store,
					graphPrefName + GraphColorProvider.PREF_COLOR_LINE,
					graphDefinition.getDefaultLineColor());

		}

		/*
		 * graph preferences
		 */
		store.setDefault(ITourbookPreferences.GRAPH_VISIBLE, Integer.toString(TourManager.GRAPH_ALTITUDE));

		final String separator = StringToArrayConverter.STRING_SEPARATOR;

		store.setDefault(ITourbookPreferences.GRAPH_ALL, (Integer.toString(TourManager.GRAPH_ALTITUDE) + separator)
				+ (Integer.toString(TourManager.GRAPH_PULSE) + separator)
				+ (Integer.toString(TourManager.GRAPH_SPEED) + separator)
				+ (Integer.toString(TourManager.GRAPH_PACE) + separator)
				+ (Integer.toString(TourManager.GRAPH_TEMPERATURE) + separator)
				+ (Integer.toString(TourManager.GRAPH_CADENCE) + separator)
				+ (Integer.toString(TourManager.GRAPH_ALTIMETER) + separator)
				+ (Integer.toString(TourManager.GRAPH_GRADIENT) + separator)
				+ Integer.toString(TourManager.GRAPH_POWER));

		// define which unit is shown on the x-axis
		store.setDefault(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);

		// mouse modus: zoom tour chart
		store.setDefault(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);

		// move sliders to border when zoomed
		store.setDefault(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED, false);

		// graph grid distance
		store.setDefault(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE, 30);
		store.setDefault(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE, 80);

		/*
		 * view colors
		 */
		PreferenceConverter.setDefault(store, //
				ITourbookPreferences.VIEW_LAYOUT_COLOR_CATEGORY,
				new RGB(0xF2, 0x5B, 0x00));

		PreferenceConverter.setDefault(store, //
				ITourbookPreferences.VIEW_LAYOUT_COLOR_TITLE,
				new RGB(0x00, 0x6F, 0xDD));

		PreferenceConverter.setDefault(store, //
				ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB,
				new RGB(0xF2, 0x5B, 0x00));

		PreferenceConverter.setDefault(store, //
				ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB_SUB,
				new RGB(0x00, 0x6F, 0xDD));

		PreferenceConverter.setDefault(store, //
				ITourbookPreferences.VIEW_LAYOUT_COLOR_TOUR,
				new RGB(0x00, 0x00, 0x00));

		PreferenceConverter.setDefault(store, //
				ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_UP,
				new RGB(0xFF, 0x5E, 0x62));

		PreferenceConverter.setDefault(store, //
				ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_SEGMENTER_DOWN,
				new RGB(0x3B, 0xFF, 0x36));

		/*
		 * view formats
		 */
		store.setDefault(
				ITourbookPreferences.VIEW_LAYOUT_RECORDING_TIME_FORMAT,
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM);
		store.setDefault(
				ITourbookPreferences.VIEW_LAYOUT_DRIVING_TIME_FORMAT,
				PrefPageAppearanceView.VIEW_TIME_LAYOUT_HH_MM);

		store.setDefault(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES, false);

		/*
		 * map
		 */
		// appearance of the painted tour in map
		store.setDefault(ITourbookPreferences.MAP_LAYOUT_SYMBOL, PrefPageAppearanceMap.MAP_TOUR_SYMBOL_LINE);
		store.setDefault(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH, 6);
		store.setDefault(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER, true);
		store.setDefault(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH, 1);
		store.setDefault(
				ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD,
				PrefPageAppearanceMap.TOUR_PAINT_METHOD_SIMPLE);

		PreferenceConverter.setDefault(store, ITourbookPreferences.MAP_LAYOUT_DIM_COLOR, new RGB(0x00, 0x00, 0x00));

		/*
		 * tour data editor
		 */
		store.setDefault(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT, 3);

		/*
		 * general appearance
		 */
		store.setDefault(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES, 3);
		store.setDefault(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS, 3);
		store.setDefault(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR, true);

		/*
		 * merge tour dialog
		 */
		store.setDefault(ITourbookPreferences.MERGE_TOUR_GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_PREVIEW_CHART, false);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_ALTITUDE_DIFF_SCALING, false);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_SYNC_START_TIME, true);

		// save actions
		store.setDefault(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_ALTITUDE, false);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_PULSE, false);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_TEMPERATURE, false);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_MERGE_GRAPH_CADENCE, false);

		store.setDefault(ITourbookPreferences.MERGE_TOUR_ADJUST_START_ALTITUDE, false);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_LINEAR_INTERPOLATION, false);

		store.setDefault(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE, false);
		store.setDefault(ITourbookPreferences.MERGE_TOUR_SET_TOUR_TYPE_ID, TourDatabase.ENTITY_IS_NOT_SAVED);

		/*
		 * computed values
		 */
		// minimum altitude difference
		store.setDefault(
				PrefPageComputedValues.STATE_COMPUTED_VALUE_MIN_ALTITUDE,
				PrefPageComputedValues.DEFAULT_MIN_ALTITUDE);

		// speed minimum time slice value in seconds
		store.setDefault(ITourbookPreferences.APP_DATA_SPEED_MIN_TIMESLICE_VALUE, 10);

		/*
		 * calendar week
		 */
		store.setDefault(ITourbookPreferences.CALENDAR_WEEK_FIRST_DAY_OF_WEEK, Calendar.MONDAY);
		store.setDefault(ITourbookPreferences.CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK, 4);

		/*
		 * view column tooltip
		 */
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_DATE, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TIME, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TITLE, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_TAGS, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURBOOK_WEEKDAY, true);

		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAG, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TITLE, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TAGGING_TAGS, true);

		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_REFTOUR, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TITLE, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURCATALOG_TAGS, true);

		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_DATE, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TIME, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TITLE, true);
		store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURIMPORT_TAGS, true);

	}
}
