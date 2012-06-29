/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
import net.tourbook.photo.manager.PhotoLoadManager;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.ISmoothingAlgorithm;
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
		 * system
		 */
		store.setDefault(ITourbookPreferences.TOUR_CACHE_SIZE, 500);

		/*
		 * tour database, default is embedded
		 */
		store.setDefault(ITourbookPreferences.TOUR_DATABASE_IS_DERBY_EMBEDDED, true);

		/*
		 * regional settings
		 */

		// disabled since version 11.7
		store.setDefault(ITourbookPreferences.MEASUREMENT_SYSTEM_SHOW_IN_UI, false);

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

		// HR zone backbround
		store.setDefault(ITourbookPreferences.GRAPH_IS_HR_ZONE_BACKGROUND_VISIBLE, false);
		store.setDefault(ITourbookPreferences.GRAPH_HR_ZONE_STYLE, TourChart.COMMAND_ID_HR_ZONE_STYLE_GRAPH_TOP);

		store.setDefault(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE, 0xFF);
		store.setDefault(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING, 0x80);
		store.setDefault(ITourbookPreferences.GRAPH_ANTIALIASING, true);

		// show breaktime values
		store.setDefault(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE, true);

		// is marker visible
		store.setDefault(ITourbookPreferences.GRAPH_IS_MARKER_VISIBLE, true);

		// define which unit is shown on the x-axis
		store.setDefault(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);

		// mouse modus: zoom tour chart
		store.setDefault(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);

		// move sliders to border when zoomed
		store.setDefault(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED, false);

		// graph grid
		store.setDefault(ITourbookPreferences.GRAPH_GRID_VERTICAL_DISTANCE, 80);
		store.setDefault(ITourbookPreferences.GRAPH_GRID_HORIZONTAL_DISTANCE, 80);
		store.setDefault(ITourbookPreferences.GRAPH_GRID_IS_SHOW_HORIZONTAL_GRIDLINES, false);
		store.setDefault(ITourbookPreferences.GRAPH_GRID_IS_SHOW_VERTICAL_GRIDLINES, false);

		// value point tool tip
		store.setDefault(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, true);

		/*
		 * graph smoothing
		 */
		// algorithm
		store.setDefault(
				ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM,
				ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET);

		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING, 1);
		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_REPEATED_TAU, 1.0);
		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_SYNCH_SMOOTHING, true);

		// altitude
		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_ALTITUDE, false);

		// gradient
		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_GRADIENT_TAU, 10.0);

		// pulse
		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_IS_PULSE, false);
		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_PULSE_TAU, 10.0);

		// speed
		store.setDefault(ITourbookPreferences.GRAPH_JAMET_SMOOTHING_SPEED_TAU, 10.0);

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
		store.setDefault(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU, true);

		// heap is not displayed by default since version 11.7
		store.setDefault(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR, false);

		store.setDefault(ITourbookPreferences.APPEARANCE_IS_TAGGING_AUTO_OPEN, true);
		store.setDefault(ITourbookPreferences.APPEARANCE_IS_TAGGING_ANIMATION, true);
		store.setDefault(ITourbookPreferences.APPEARANCE_TAGGING_AUTO_OPEN_DELAY, 500);

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
		 * break time using metric values
		 */
		store.setDefault(ITourbookPreferences.BREAK_TIME_METHOD2, BreakTimeTool.BREAK_TIME_METHOD_BY_AVG_SLICE_SPEED);

		// minimum speed for a break, default is 1.0 km/h to respect walking in the mountains
		store.setDefault(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED_AS, 1.0f);
		store.setDefault(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED_AS, 1.0f);
		store.setDefault(ITourbookPreferences.BREAK_TIME_MIN_SLICE_TIME_AS, 2);

		// minimum speed for a break, default is 1.0 km/h to respect walking in the mountains
		store.setDefault(ITourbookPreferences.BREAK_TIME_MIN_SLICE_SPEED, 1.0f);
		store.setDefault(ITourbookPreferences.BREAK_TIME_MIN_AVG_SPEED, 1.0f);

		// break time, default is 3.6 km/h (20 m in 20 sec)
		store.setDefault(ITourbookPreferences.BREAK_TIME_SHORTEST_TIME, 20);
		store.setDefault(ITourbookPreferences.BREAK_TIME_MAX_DISTANCE, 20.0f);
		store.setDefault(ITourbookPreferences.BREAK_TIME_SLICE_DIFF, 5); // 5 minutes

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

		/*
		 * view actions
		 */
		store.setDefault(
				ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS,
				PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_QUICK_EDIT);

		/*
		 * photo
		 */
		store.setDefault(ITourbookPreferences.PHOTO_THUMBNAIL_STORE_IS_DEFAULT_LOCATION, true);
		store.setDefault(ITourbookPreferences.PHOTO_THUMBNAIL_STORE_CUSTOM_LOCATION, UI.EMPTY_STRING);

		store.setDefault(ITourbookPreferences.PHOTO_THUMBNAIL_STORE_IS_CLEANUP, false);
		store.setDefault(ITourbookPreferences.PHOTO_THUMBNAIL_STORE_NUMBER_OF_DAYS_TO_KEEP_IMAGES, 90);
		store.setDefault(ITourbookPreferences.PHOTO_THUMBNAIL_STORE_CLEANUP_PERIOD, 30);

		store.setDefault(ITourbookPreferences.PHOTO_THUMBNAIL_IMAGE_CACHE_SIZE, 2000);
		store.setDefault(ITourbookPreferences.PHOTO_ORIGINAL_IMAGE_CACHE_SIZE, 3);

		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_FILE_FOLDER, true);

		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_IS_SHOW_IMAGE_WITH_HIGH_QUALITY, true);
		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_HIGH_QUALITY_IMAGE_MIN_SIZE, 50);
		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_HQ_IMAGE_SIZE, PhotoLoadManager.IMAGE_SIZE_LARGE_DEFAULT);

		PreferenceConverter.setDefault(store, ITourbookPreferences.PHOTO_VIEWER_COLOR_FOREGROUND, //
				new RGB(0xf3, 0xf3, 0xf3));

		PreferenceConverter.setDefault(store, ITourbookPreferences.PHOTO_VIEWER_COLOR_BACKGROUND, //
				new RGB(0x33, 0x33, 0x33));

		PreferenceConverter.setDefault(store, ITourbookPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND, //
				new RGB(0xFF, 0x80, 0x33));

		PreferenceConverter.setDefault(store, ITourbookPreferences.PHOTO_VIEWER_COLOR_FOLDER, //
				new RGB(0xFF, 0x6A, 0x11));

		PreferenceConverter.setDefault(store, ITourbookPreferences.PHOTO_VIEWER_COLOR_FILE, //
				new RGB(0x55, 0xC8, 0xFF));

		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_TEXT_MIN_THUMB_SIZE, 70);
		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_IMAGE_BORDER_SIZE, 4);

		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_FONT, //
				UI.IS_OSX /*
						 * this small font for OSX cannot be selected in the UI, but is smaller than
						 * the fonts which can be selected
						 */
				? "1|sans-serif|9|0|"//$NON-NLS-1$
						: "1|sans-serif|7|0|");//$NON-NLS-1$
/////////////////////	  1|DejaVu Sans|6.75|0|WINDOWS|1|-9|0|0|0|400|0|0|0|0|3|2|1|34|DejaVu Sans

		store.setDefault(ITourbookPreferences.PHOTO_VIEWER_IMAGE_FRAMEWORK, //
				UI.IS_OSX //
						? PhotoLoadManager.IMAGE_FRAMEWORK_SWT
						//
						// SWT is terrible when scolling large images on win & linux, osx is smoothly
						//
						: PhotoLoadManager.IMAGE_FRAMEWORK_AWT);

		store.setDefault(ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_PREVIEW, false);
		store.setDefault(ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_LOADING_MESSAGE, true);
		store.setDefault(ITourbookPreferences.PHOTO_FULLSIZE_VIEWER_IS_SHOW_HQ_IMAGE, false);

		store.setDefault(ITourbookPreferences.PHOTO_SYSTEM_IS_ROTATE_IMAGE_AUTOMATICALLY, true);

		/*
		 * external photo viewer
		 */
		if (UI.IS_WIN) {

			store.setDefault(ITourbookPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1, "explorer.exe"); //$NON-NLS-1$

		} else if (UI.IS_OSX) {

			store.setDefault(ITourbookPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1, "Preview.app"); //$NON-NLS-1$
			store.setDefault(ITourbookPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_2, "Finder.app"); //$NON-NLS-1$

		} else if (UI.IS_LINUX) {
			store.setDefault(ITourbookPreferences.PHOTO_EXTERNAL_PHOTO_VIEWER_1, "nautilus"); //$NON-NLS-1$
		}

	}
}
