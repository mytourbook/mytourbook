/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringToArrayConverter;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.EasyConfig;
import net.tourbook.statistic.DurationTime;
import net.tourbook.tour.BreakTimeTool;
import net.tourbook.tour.DialogSetTimeZone;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.ChartMarkerToolTip;
import net.tourbook.ui.tourChart.ChartPauseToolTip;
import net.tourbook.ui.tourChart.TourChart;
import net.tourbook.ui.views.ISmoothingAlgorithm;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;

/**
 * Class used to initialize default preference values.
 */
public class PreferenceInitializer extends AbstractPreferenceInitializer {

// SET_FORMATTING_OFF

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
       * Regional settings
       */
      store.setDefault(ITourbookPreferences.REGIONAL_USE_CUSTOM_DECIMAL_FORMAT, false);
      store.setDefault(ITourbookPreferences.REGIONAL_DECIMAL_SEPARATOR, UI.SYMBOL_DOT);
      store.setDefault(ITourbookPreferences.REGIONAL_GROUP_SEPARATOR, "'"); //$NON-NLS-1$

      /*
       * Statistics
       */
      store.setDefault(ITourbookPreferences.STAT_ALTITUDE_LOW_VALUE, 0);
      store.setDefault(ITourbookPreferences.STAT_ALTITUDE_INTERVAL, 250);
      store.setDefault(ITourbookPreferences.STAT_ALTITUDE_NUMBERS, 21);

      store.setDefault(ITourbookPreferences.STAT_DISTANCE_LOW_VALUE, 0);
      store.setDefault(ITourbookPreferences.STAT_DISTANCE_INTERVAL, 10);
      store.setDefault(ITourbookPreferences.STAT_DISTANCE_NUMBERS, 21);

      store.setDefault(ITourbookPreferences.STAT_DURATION_LOW_VALUE, 0);
      store.setDefault(ITourbookPreferences.STAT_DURATION_INTERVAL, 60);
      store.setDefault(ITourbookPreferences.STAT_DURATION_NUMBERS, 12);

      store.setDefault(ITourbookPreferences.STAT_DAY_DURATION_TIME, DurationTime.MOVING.name());
      store.setDefault(ITourbookPreferences.STAT_DAY_IS_SHOW_ALTITUDE, true);
      store.setDefault(ITourbookPreferences.STAT_DAY_IS_SHOW_AVG_PACE, true);
      store.setDefault(ITourbookPreferences.STAT_DAY_IS_SHOW_AVG_SPEED, true);
      store.setDefault(ITourbookPreferences.STAT_DAY_IS_SHOW_DISTANCE, true);
      store.setDefault(ITourbookPreferences.STAT_DAY_IS_SHOW_DURATION, true);
      store.setDefault(ITourbookPreferences.STAT_DAY_IS_SHOW_YEAR_SEPARATOR, true);

      store.setDefault(ITourbookPreferences.STAT_WEEK_CHART_TYPE, ChartDataSerie.CHART_TYPE_BAR_STACKED);
      store.setDefault(ITourbookPreferences.STAT_WEEK_DURATION_TIME, DurationTime.MOVING.name());
      store.setDefault(ITourbookPreferences.STAT_WEEK_IS_SHOW_ALTITUDE, true);
      store.setDefault(ITourbookPreferences.STAT_WEEK_IS_SHOW_DISTANCE, true);
      store.setDefault(ITourbookPreferences.STAT_WEEK_IS_SHOW_DURATION, true);
      store.setDefault(ITourbookPreferences.STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS, true);
      store.setDefault(ITourbookPreferences.STAT_WEEK_IS_SHOW_YEAR_SEPARATOR, true);
      store.setDefault(ITourbookPreferences.STAT_WEEK_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES, true);
      store.setDefault(ITourbookPreferences.STAT_WEEK_TOOLTIP_IS_SHOW_SUMMARY_VALUES, true);

      store.setDefault(ITourbookPreferences.STAT_MONTH_CHART_TYPE, ChartDataSerie.CHART_TYPE_BAR_STACKED);
      store.setDefault(ITourbookPreferences.STAT_MONTH_DURATION_TIME, DurationTime.MOVING.name());
      store.setDefault(ITourbookPreferences.STAT_MONTH_IS_SHOW_ALTITUDE, true);
      store.setDefault(ITourbookPreferences.STAT_MONTH_IS_SHOW_DISTANCE, true);
      store.setDefault(ITourbookPreferences.STAT_MONTH_IS_SHOW_DURATION, true);
      store.setDefault(ITourbookPreferences.STAT_MONTH_IS_SHOW_NUMBER_OF_TOURS, true);
      store.setDefault(ITourbookPreferences.STAT_MONTH_IS_SHOW_YEAR_SEPARATOR, true);
      store.setDefault(ITourbookPreferences.STAT_MONTH_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES, true);
      store.setDefault(ITourbookPreferences.STAT_MONTH_TOOLTIP_IS_SHOW_SUMMARY_VALUES, true);

      store.setDefault(ITourbookPreferences.STAT_YEAR_CHART_TYPE, ChartDataSerie.CHART_TYPE_BAR_STACKED);
      store.setDefault(ITourbookPreferences.STAT_YEAR_DURATION_TIME, DurationTime.MOVING.name());
      store.setDefault(ITourbookPreferences.STAT_YEAR_IS_SHOW_ALTITUDE, true);
      store.setDefault(ITourbookPreferences.STAT_YEAR_IS_SHOW_DISTANCE, true);
      store.setDefault(ITourbookPreferences.STAT_YEAR_IS_SHOW_DURATION, true);
      store.setDefault(ITourbookPreferences.STAT_YEAR_IS_SHOW_NUMBER_OF_TOURS, true);
      store.setDefault(ITourbookPreferences.STAT_YEAR_IS_SHOW_YEAR_SEPARATOR, true);
      store.setDefault(ITourbookPreferences.STAT_YEAR_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES, true);
      store.setDefault(ITourbookPreferences.STAT_YEAR_TOOLTIP_IS_SHOW_SUMMARY_VALUES, true);

      store.setDefault(ITourbookPreferences.STAT_FREQUENCY_DURATION_TIME, DurationTime.MOVING.name());
      store.setDefault(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES, true);
      store.setDefault(ITourbookPreferences.STAT_FREQUENCY_TOOLTIP_IS_SHOW_SUMMARY_VALUES, true);

      store.setDefault(ITourbookPreferences.STAT_TRAINING_BAR_DURATION_TIME, DurationTime.MOVING.name());
      store.setDefault(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT, true);
      store.setDefault(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC, true);
      store.setDefault(ITourbookPreferences.STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE, true);

      store.setDefault(ITourbookPreferences.STAT_TRAINING_LINE_DURATION_TIME, DurationTime.MOVING.name());
      store.setDefault(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT, true);
      store.setDefault(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT_ANAEROBIC, true);
      store.setDefault(ITourbookPreferences.STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE, true);

      store.setDefault(ITourbookPreferences.STAT_BODYWEIGHT_YAXIS_MIN_VISIBLE_VALUE, 0.0);
      store.setDefault(ITourbookPreferences.STAT_BODYWEIGHT_YAXIS_MAX_VISIBLE_VALUE, 200.0);
      store.setDefault(ITourbookPreferences.STAT_BODYFAT_YAXIS_MIN_VISIBLE_VALUE, 0.0);
      store.setDefault(ITourbookPreferences.STAT_BODYFAT_YAXIS_MAX_VISIBLE_VALUE, 100.0);

      /*
       * Graph preferences
       */
      store.setDefault(ITourbookPreferences.GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE, true);
      store.setDefault(ITourbookPreferences.GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE, true);
      store.setDefault(ITourbookPreferences.GRAPH_VISIBLE, Integer.toString(TourManager.GRAPH_ALTITUDE));
      store.setDefault(ITourbookPreferences.GRAPH_IS_GRAPH_OVERLAPPED, false);

      final String separator = StringToArrayConverter.STRING_SEPARATOR;

      store.setDefault(ITourbookPreferences.GRAPH_ALL,
              (Integer.toString(TourManager.GRAPH_ALTITUDE)    + separator)
            + (Integer.toString(TourManager.GRAPH_PULSE)       + separator)
            + (Integer.toString(TourManager.GRAPH_SPEED)       + separator)
            + (Integer.toString(TourManager.GRAPH_PACE)        + separator)
            + (Integer.toString(TourManager.GRAPH_TEMPERATURE) + separator)
            + (Integer.toString(TourManager.GRAPH_CADENCE)     + separator)
            + (Integer.toString(TourManager.GRAPH_GEARS)       + separator)
            + (Integer.toString(TourManager.GRAPH_ALTIMETER)   + separator)
            + (Integer.toString(TourManager.GRAPH_GRADIENT)    + separator)
            + Integer.toString(TourManager.GRAPH_POWER));

      store.setDefault(ITourbookPreferences.GRAPH_ANTIALIASING,               true);
      store.setDefault(ITourbookPreferences.GRAPH_TRANSPARENCY_LINE,          100);
      store.setDefault(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING,       70);
      store.setDefault(ITourbookPreferences.GRAPH_TRANSPARENCY_FILLING_DARK,  20);

      // chart/tour segment alternate color
      store.setDefault(ITourbookPreferences.GRAPH_IS_SEGMENT_ALTERNATE_COLOR, false);
      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR,       new RGB(0xf5, 0xf5, 0xf5));
      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_SEGMENT_ALTERNATE_COLOR_DARK,  new RGB(0x40, 0x40, 0x40));

      // tour segmenter chart value font
      final FontData[] segmenterDefaultFontData = Display.getDefault().getSystemFont().getFontData();
      segmenterDefaultFontData[0].setHeight(8);
      PreferenceConverter.setDefault(store, ITourbookPreferences.TOUR_SEGMENTER_CHART_VALUE_FONT, segmenterDefaultFontData);

      // show breaktime values
      store.setDefault(ITourbookPreferences.GRAPH_IS_BREAKTIME_VALUES_VISIBLE, true);

      // tour marker
      store.setDefault(ITourbookPreferences.GRAPH_IS_MARKER_VISIBLE,          true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_HOVER_SIZE,          4);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_LABEL_OFFSET,        2);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_LABEL_TEMP_POSITION, TourMarker.LABEL_POS_HORIZONTAL_ABOVE_GRAPH_CENTERED);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_POINT_SIZE,          2);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_SIGN_IMAGE_SIZE,     50); // in DLU
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_TOOLTIP_POSITION,    ChartMarkerToolTip.DEFAULT_TOOLTIP_POSITION);

      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR,      true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ABSOLUTE_VALUES,         false);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER,           false);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_ONLY_WITH_DESCRIPTION,   false);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION,     false);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_LABEL,            true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_POINT,            true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP,          true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_SIGN_IMAGE,              true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATION,                 true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE,                  true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION,                  true);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATIONGAIN_DIFFERENCE,  false);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE_DIFFERENCE,       false);
      store.setDefault(ITourbookPreferences.GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION_DIFFERENCE,       false);

      // pulse values
      store.setDefault(ITourbookPreferences.GRAPH_PULSE_GRAPH_VALUES,      TourChart.PULSE_GRAPH_DEFAULT.name());

      //tour pauses
      store.setDefault(ITourbookPreferences.GRAPH_ARE_PAUSES_VISIBLE,      true);
      store.setDefault(ITourbookPreferences.GRAPH_PAUSES_IS_SHOW_PAUSE_TOOLTIP,      true);
      store.setDefault(ITourbookPreferences.GRAPH_PAUSES_TOOLTIP_POSITION,    ChartPauseToolTip.DEFAULT_TOOLTIP_POSITION);

      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT,       new RGB(0x60, 0x60, 0x60));
      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_MARKER_COLOR_DEFAULT_DARK,  new RGB(0xd0, 0xd0, 0xd0));
      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE,        new RGB(0xff, 0x0,  0x80));
      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_MARKER_COLOR_DEVICE_DARK,   new RGB(255,  193,  79));
      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN,        new RGB(0x24, 0x9C, 0xFF));
      PreferenceConverter.setDefault(store, ITourbookPreferences.GRAPH_MARKER_COLOR_HIDDEN_DARK,   new RGB(0x6F, 0xBE, 0xFF));

      // tour info
      store.setDefault(ITourbookPreferences.GRAPH_TOUR_INFO_IS_VISIBLE, true);
      store.setDefault(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TITLE_VISIBLE, true);
      store.setDefault(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOOLTIP_VISIBLE, true);
      store.setDefault(ITourbookPreferences.GRAPH_TOUR_INFO_IS_TOUR_SEPARATOR_VISIBLE, true);
      store.setDefault(ITourbookPreferences.GRAPH_TOUR_INFO_TOOLTIP_DELAY, 200);

      store.setDefault(ITourbookPreferences.GRAPH_IS_SHOW_VALUE_POINT_VALUE, true);

      // is photo visible
      store.setDefault(ITourbookPreferences.GRAPH_IS_TOUR_PHOTO_VISIBLE, true);
      store.setDefault(ITourbookPreferences.GRAPH_IS_TOUR_PHOTO_TOOLTIP_VISIBLE, true);

      // define which unit is shown on the x-axis
      store.setDefault(ITourbookPreferences.GRAPH_X_AXIS, TourManager.X_AXIS_DISTANCE);

      // mouse modus: zoom tour chart
      store.setDefault(ITourbookPreferences.GRAPH_MOUSE_MODE, Chart.MOUSE_MODE_ZOOM);

      // move sliders to border when zoomed
      store.setDefault(ITourbookPreferences.GRAPH_MOVE_SLIDERS_WHEN_ZOOMED, false);

      // Show the pace chart inverted: High speeds/low paces at the top of the graph and
      // the low speeds/high paces at the bottom of the graph
      store.setDefault(ITourbookPreferences.GRAPH_IS_SHOW_PACE_GRAPH_INVERTED, true);

      // night section
      store.setDefault(ITourbookPreferences.GRAPH_IS_SHOW_NIGHT_SECTIONS, true);
      store.setDefault(ITourbookPreferences.GRAPH_OPACITY_NIGHT_SECTIONS, 50);

      // graph grid
      store.setDefault(ITourbookPreferences.CHART_GRID_VERTICAL_DISTANCE, 80);
      store.setDefault(ITourbookPreferences.CHART_GRID_HORIZONTAL_DISTANCE, 80);
      store.setDefault(ITourbookPreferences.CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES, false);
      store.setDefault(ITourbookPreferences.CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES, false);

      /*
       * Min/Max values
       */
      store.setDefault(ITourbookPreferences.GRAPH_IS_MIN_MAX_ENABLED, true);

      store.setDefault(ITourbookPreferences.GRAPH_ALTIMETER_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_ALTIMETER_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_ALTIMETER_MIN_VALUE, 0);
      store.setDefault(ITourbookPreferences.GRAPH_ALTIMETER_MAX_VALUE, 1500);

      store.setDefault(ITourbookPreferences.GRAPH_ALTITUDE_IS_MIN_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_ALTITUDE_IS_MAX_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_ALTITUDE_MIN_VALUE, 0);
      store.setDefault(ITourbookPreferences.GRAPH_ALTITUDE_MAX_VALUE, 3000);

      store.setDefault(ITourbookPreferences.GRAPH_CADENCE_IS_MIN_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_CADENCE_IS_MAX_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_CADENCE_MIN_VALUE, 50);
      store.setDefault(ITourbookPreferences.GRAPH_CADENCE_MAX_VALUE, 100);

      store.setDefault(ITourbookPreferences.GRAPH_GRADIENT_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_GRADIENT_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_GRADIENT_MIN_VALUE, -20);
      store.setDefault(ITourbookPreferences.GRAPH_GRADIENT_MAX_VALUE, 20);

      store.setDefault(ITourbookPreferences.GRAPH_PACE_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_PACE_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_PACE_MIN_VALUE, 1);
      store.setDefault(ITourbookPreferences.GRAPH_PACE_MAX_VALUE, 6);

      store.setDefault(ITourbookPreferences.GRAPH_SPEED_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_SPEED_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_SPEED_MIN_VALUE, 5);
      store.setDefault(ITourbookPreferences.GRAPH_SPEED_MAX_VALUE, 150);

      store.setDefault(ITourbookPreferences.GRAPH_POWER_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_POWER_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_POWER_MIN_VALUE, 100);
      store.setDefault(ITourbookPreferences.GRAPH_POWER_MAX_VALUE, 400);

      store.setDefault(ITourbookPreferences.GRAPH_PULSE_IS_MIN_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_PULSE_IS_MAX_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_PULSE_MIN_VALUE, 50);
      store.setDefault(ITourbookPreferences.GRAPH_PULSE_MAX_VALUE, 200);

      store.setDefault(ITourbookPreferences.GRAPH_TEMPERATURE_IS_MIN_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_TEMPERATURE_IS_MAX_ENABLED, false);
      store.setDefault(ITourbookPreferences.GRAPH_TEMPERATURE_MIN_VALUE, -50);
      store.setDefault(ITourbookPreferences.GRAPH_TEMPERATURE_MAX_VALUE, 50);

      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MIN_VALUE, 220);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_MAX_VALUE, 300);

      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MIN_VALUE, 47);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MAX_VALUE, 53);

      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MIN_VALUE, 800);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_STEP_LENGTH_MAX_VALUE, 1800);

      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MIN_VALUE, 80);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MAX_VALUE, 140);

      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MIN_VALUE, 5);
      store.setDefault(ITourbookPreferences.GRAPH_RUN_DYN_VERTICAL_RATIO_MAX_VALUE, 15);

      store.setDefault(ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_SWIM_STROKES_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_SWIM_STROKES_MIN_VALUE, 5);
      store.setDefault(ITourbookPreferences.GRAPH_SWIM_STROKES_MAX_VALUE, 20);

      store.setDefault(ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MIN_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_SWIM_SWOLF_IS_MAX_ENABLED, true);
      store.setDefault(ITourbookPreferences.GRAPH_SWIM_SWOLF_MIN_VALUE, 20);
      store.setDefault(ITourbookPreferences.GRAPH_SWIM_SWOLF_MAX_VALUE, 80);

      // value point tool tip
      store.setDefault(ITourbookPreferences.VALUE_POINT_TOOL_TIP_IS_VISIBLE, true);

      /*
       * graph smoothing
       */
      // algorithm
      store.setDefault(ITourbookPreferences.GRAPH_SMOOTHING_SMOOTHING_ALGORITHM, ISmoothingAlgorithm.SMOOTHING_ALGORITHM_JAMET);

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
      PreferenceConverter.setDefault(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_CATEGORY,          new RGB(0xF2, 0x5B, 0x00));
      PreferenceConverter.setDefault(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_TITLE,             new RGB(0x00, 0x6F, 0xDD));
      PreferenceConverter.setDefault(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB,               new RGB(0xF2, 0x5B, 0x00));
      PreferenceConverter.setDefault(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_SUB_SUB,           new RGB(0x00, 0x6F, 0xDD));
      PreferenceConverter.setDefault(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_TOUR,              new RGB(0x00, 0x00, 0x00));
      PreferenceConverter.setDefault(store, ITourbookPreferences.VIEW_LAYOUT_COLOR_BG_HISTORY_TOUR,   new RGB(0xFC, 0xFF, 0xE3));

      /*
       * Display formats
       */

      store.setDefault(ITourbookPreferences.VIEW_LAYOUT_DISPLAY_LINES, false);

      /*
       * map
       */
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_PLOT_TYPE,                PrefPage_Map2_Appearance.DEFAULT_PLOT_TYPE);
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_SYMBOL_WIDTH,             6);
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_PAINT_WITH_BORDER,        true);
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_BORDER_WIDTH,             1);
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_BORDER_DIMM_VALUE,        80); // 0...100

      PreferenceConverter.setDefault(store, ITourbookPreferences.MAP_LAYOUT_BORDER_COLOR,    new RGB(0x50, 0x50, 0x50));
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD,           PrefPage_Map2_Appearance.TOUR_PAINT_METHOD_SIMPLE);
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_TOUR_PAINT_METHOD_WARNING,   true);
      store.setDefault(ITourbookPreferences.MAP_LAYOUT_LIVE_UPDATE, true);

      store.setDefault(ITourbookPreferences.MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY,      false);
      store.setDefault(ITourbookPreferences.MAP2_LAYOUT_TOUR_TRACK_OPACITY,         70);     // opacity in %


      /*
       * Geo compare
       */
      store.setDefault(ITourbookPreferences.GEO_COMPARE_REF_TOUR_LINE_WIDTH,                          15);
      PreferenceConverter.setDefault(store, ITourbookPreferences.GEO_COMPARE_REF_TOUR_RGB,            new RGB(0xff, 0xff, 0xff));
      PreferenceConverter.setDefault(store, ITourbookPreferences.GEO_COMPARE_COMPARED_TOUR_PART_RGB,  new RGB(0xff, 0x00, 0x80));

      /*
       * general appearance
       */
      store.setDefault(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES, 3);
      store.setDefault(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TAGS, 3);
      store.setDefault(ITourbookPreferences.APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU, true);

      // heap is not displayed by default since version 11.7
      store.setDefault(ITourbookPreferences.APPEARANCE_SHOW_MEMORY_MONITOR, false);

      store.setDefault(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME, false);
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

      // DP tolerance when computing altitude up/down
      store.setDefault(ITourbookPreferences.COMPUTED_ALTITUDE_DP_TOLERANCE, 7.0f);

      // Cadence zones delimiter
      store.setDefault(ITourbookPreferences.CADENCE_ZONES_DELIMITER, 70);

      /*
       * view column tooltip
       */
      store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_COLLATION, true);
      store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TIME, true);
      store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TITLE, true);
      store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_TAGS, true);
      store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_COLLATED_WEEKDAY, true);

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

      store.setDefault(ITourbookPreferences.VIEW_TOOLTIP_TOURCOMPARERESULT_TIME, true);

      /*
       * view actions
       */
      store.setDefault(ITourbookPreferences.VIEW_DOUBLE_CLICK_ACTIONS, PrefPageViews.VIEW_DOUBLE_CLICK_ACTION_QUICK_EDIT);

      /*
       * Map 2.5D
       */
      store.setDefault(ITourbookPreferences.MAP25_OFFLINE_MAP_IS_DEFAULT_LOCATION, true);

      /*
       * Pref page: Map3 color
       */
      store.setDefault(ITourbookPreferences.MAP3_IS_COLOR_SELECTOR_DISPLAYED, true);
      store.setDefault(ITourbookPreferences.MAP3_NUMBER_OF_COLOR_SELECTORS, 10);

      /*
       * Tour import
       */
      store.setDefault(ITourbookPreferences.IMPORT_IS_NEW_UI, false);

      /*
       * Adjust temperature
       */
      store.setDefault(ITourbookPreferences.ADJUST_TEMPERATURE_AVG_TEMPERATURE,  EasyConfig.TEMPERATURE_AVG_TEMPERATURE_DEFAULT);
      store.setDefault(ITourbookPreferences.ADJUST_TEMPERATURE_DURATION_TIME,    EasyConfig.TEMPERATURE_ADJUSTMENT_DURATION_DEFAULT);

      /*
       * Heart rate variability
       */
      store.setDefault(ITourbookPreferences.HRV_OPTIONS_2X_ERROR_TOLERANCE, 20); // milliseconds
      store.setDefault(ITourbookPreferences.HRV_OPTIONS_IS_FIX_2X_ERROR, false);

      /*
       * Time zone
       */
      store.setDefault(ITourbookPreferences.DIALOG_SET_TIME_ZONE_ACTION, DialogSetTimeZone.TIME_ZONE_ACTION_SET_FROM_LIST);
      store.setDefault(ITourbookPreferences.DIALOG_SET_TIME_ZONE_SELECTED_ZONE_ID, TimeTools.getDefaultTimeZoneId());

      /*
       * Weather
       */
      store.setDefault(ITourbookPreferences.WEATHER_USE_WEATHER_RETRIEVAL,false);
      store.setDefault(ITourbookPreferences.WEATHER_API_KEY, UI.EMPTY_STRING);

// SET_FORMATTING_ON
   }

}
