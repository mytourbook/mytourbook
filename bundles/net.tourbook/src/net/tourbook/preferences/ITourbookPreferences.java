/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.color.MapGraphId;
import net.tourbook.map.MapColorProvider;

public interface ITourbookPreferences {

   /*
    * Dummy field used by field editors so they don't assert
    */
   public static final String DUMMY_FIELD                          = UI.EMPTY_STRING;

   public static final String APP_LAST_SELECTED_PREFERENCE_PAGE_ID = "APP_LAST_SELECTED_PREFERENCE_PAGE_ID"; //$NON-NLS-1$

   public static final String GENERAL_NOTES                        = "GENERAL_NOTES";                        //$NON-NLS-1$

   /*
    * System
    */
   public static final String TOUR_CACHE_SIZE = "TourCacheSize"; //$NON-NLS-1$

   /*
    * Tour database
    */
   public static final String TOUR_DATABASE_IS_DERBY_EMBEDDED = "TOUR_DATABASE_IS_DERBY_EMBEDDED"; //$NON-NLS-1$

   /*
    * Notifications
    */
   public static final String HR_ZONES_ARE_MODIFIED = "HrZonesAreModified"; //$NON-NLS-1$

   /*
    * Statistic provider
    */
   public static final String STATISTICS_STATISTIC_PROVIDER_IDS = "statistics.provider.ids"; //$NON-NLS-1$

   /*
    * Statistic: Number frequency - where the units can be set in the preferences
    */
   public static final String STAT_DISTANCE_NUMBERS   = "statistic.distance.numbers";  //$NON-NLS-1$
   public static final String STAT_DISTANCE_LOW_VALUE = "statistic.distance.lowvalue"; //$NON-NLS-1$
   public static final String STAT_DISTANCE_INTERVAL  = "statistic.distance.interval"; //$NON-NLS-1$

   public static final String STAT_ALTITUDE_NUMBERS   = "statistic.altitude.numbers";  //$NON-NLS-1$
   public static final String STAT_ALTITUDE_LOW_VALUE = "statistic.altitude.lowvalue"; //$NON-NLS-1$
   public static final String STAT_ALTITUDE_INTERVAL  = "statistic.altitude.interval"; //$NON-NLS-1$

   public static final String STAT_DURATION_NUMBERS   = "statistic.duration.numbers";  //$NON-NLS-1$
   public static final String STAT_DURATION_LOW_VALUE = "statistic.duration.lowvalue"; //$NON-NLS-1$
   public static final String STAT_DURATION_INTERVAL  = "statistic.duration.interval"; //$NON-NLS-1$

   /*
    * Statistic summary
    */
   public static final String STAT_DAY_DURATION_TIME                           = "STAT_DAY_DURATION_TIME";                           //$NON-NLS-1$
   public static final String STAT_DAY_IS_SHOW_ELEVATION_UP                    = "STAT_DAY_IS_SHOW_ELEVATION_UP";                    //$NON-NLS-1$
   public static final String STAT_DAY_IS_SHOW_ELEVATION_DOWN                  = "STAT_DAY_IS_SHOW_ELEVATION_DOWN";                  //$NON-NLS-1$
   public static final String STAT_DAY_IS_SHOW_AVG_PACE                        = "STAT_DAY_IS_SHOW_AVG_PACE";                        //$NON-NLS-1$
   public static final String STAT_DAY_IS_SHOW_AVG_SPEED                       = "STAT_DAY_IS_SHOW_AVG_SPEED";                       //$NON-NLS-1$
   public static final String STAT_DAY_IS_SHOW_DISTANCE                        = "STAT_DAY_IS_SHOW_DISTANCE";                        //$NON-NLS-1$
   public static final String STAT_DAY_IS_SHOW_DURATION                        = "STAT_DAY_IS_SHOW_DURATION";                        //$NON-NLS-1$
   public static final String STAT_DAY_IS_SHOW_YEAR_SEPARATOR                  = "STAT_DAY_IS_SHOW_YEAR_SEPARATOR";                  //$NON-NLS-1$

   public static final String STAT_WEEK_DURATION_TIME                          = "STAT_WEEK_DURATION_TIME";                          //$NON-NLS-1$
   public static final String STAT_WEEK_CHART_TYPE                             = "STAT_WEEK_CHART_TYPE";                             //$NON-NLS-1$
   public static final String STAT_WEEK_IS_SHOW_ELEVATION_UP                   = "STAT_WEEK_IS_SHOW_ELEVATION_UP";                   //$NON-NLS-1$
   public static final String STAT_WEEK_IS_SHOW_ELEVATION_DOWN                 = "STAT_WEEK_IS_SHOW_ELEVATION_DOWN";                 //$NON-NLS-1$
   public static final String STAT_WEEK_IS_SHOW_DISTANCE                       = "STAT_WEEK_IS_SHOW_DISTANCE";                       //$NON-NLS-1$
   public static final String STAT_WEEK_IS_SHOW_DURATION                       = "STAT_WEEK_IS_SHOW_DURATION";                       //$NON-NLS-1$
   public static final String STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS                = "STAT_WEEK_IS_SHOW_NUMBER_OF_TOURS";                //$NON-NLS-1$
   public static final String STAT_WEEK_IS_SHOW_YEAR_SEPARATOR                 = "STAT_WEEK_IS_SHOW_YEAR_SEPARATOR";                 //$NON-NLS-1$
   public static final String STAT_WEEK_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES      = "STAT_WEEK_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES";      //$NON-NLS-1$
   public static final String STAT_WEEK_TOOLTIP_IS_SHOW_SUMMARY_VALUES         = "STAT_WEEK_TOOLTIP_IS_SHOW_SUMMARY_VALUES";         //$NON-NLS-1$

   public static final String STAT_MONTH_DURATION_TIME                         = "STAT_MONTH_DURATION_TIME";                         //$NON-NLS-1$
   public static final String STAT_MONTH_CHART_TYPE                            = "STAT_MONTH_CHART_TYPE";                            //$NON-NLS-1$
   public static final String STAT_MONTH_IS_SHOW_ELEVATION_UP                  = "STAT_MONTH_IS_SHOW_ELEVATION_UP";                  //$NON-NLS-1$
   public static final String STAT_MONTH_IS_SHOW_ELEVATION_DOWN                = "STAT_MONTH_IS_SHOW_ELEVATION_DOWN";                //$NON-NLS-1$
   public static final String STAT_MONTH_IS_SHOW_DISTANCE                      = "STAT_MONTH_IS_SHOW_DISTANCE";                      //$NON-NLS-1$
   public static final String STAT_MONTH_IS_SHOW_DURATION                      = "STAT_MONTH_IS_SHOW_DURATION";                      //$NON-NLS-1$
   public static final String STAT_MONTH_IS_SHOW_NUMBER_OF_TOURS               = "STAT_MONTH_IS_SHOW_NUMBER_OF_TOURS";               //$NON-NLS-1$
   public static final String STAT_MONTH_IS_SHOW_YEAR_SEPARATOR                = "STAT_MONTH_IS_SHOW_YEAR_SEPARATOR";                //$NON-NLS-1$
   public static final String STAT_MONTH_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES     = "STAT_MONTH_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES";     //$NON-NLS-1$
   public static final String STAT_MONTH_TOOLTIP_IS_SHOW_SUMMARY_VALUES        = "STAT_MONTH_TOOLTIP_IS_SHOW_SUMMARY_VALUES";        //$NON-NLS-1$

   public static final String STAT_YEAR_DURATION_TIME                          = "STAT_YEAR_DURATION_TIME";                          //$NON-NLS-1$
   public static final String STAT_YEAR_CHART_TYPE                             = "STAT_YEAR_CHART_TYPE";                             //$NON-NLS-1$
   public static final String STAT_YEAR_IS_SHOW_ELEVATION_UP                   = "STAT_YEAR_IS_SHOW_ELEVATION_UP";                   //$NON-NLS-1$
   public static final String STAT_YEAR_IS_SHOW_ELEVATION_DOWN                 = "STAT_YEAR_IS_SHOW_ELEVATION_DOWN";                 //$NON-NLS-1$
   public static final String STAT_YEAR_IS_SHOW_DISTANCE                       = "STAT_YEAR_IS_SHOW_DISTANCE";                       //$NON-NLS-1$
   public static final String STAT_YEAR_IS_SHOW_DURATION                       = "STAT_YEAR_IS_SHOW_DURATION";                       //$NON-NLS-1$
   public static final String STAT_YEAR_IS_SHOW_NUMBER_OF_TOURS                = "STAT_YEAR_IS_SHOW_NUMBER_OF_TOURS";                //$NON-NLS-1$
   public static final String STAT_YEAR_IS_SHOW_YEAR_SEPARATOR                 = "STAT_YEAR_IS_SHOW_YEAR_SEPARATOR";                 //$NON-NLS-1$
   public static final String STAT_YEAR_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES      = "STAT_YEAR_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES";      //$NON-NLS-1$
   public static final String STAT_YEAR_TOOLTIP_IS_SHOW_SUMMARY_VALUES         = "STAT_YEAR_TOOLTIP_IS_SHOW_SUMMARY_VALUES";         //$NON-NLS-1$

   public static final String STAT_FREQUENCY_DURATION_TIME                     = "STAT_FREQUENCY_DURATION_TIME";                     //$NON-NLS-1$
   public static final String STAT_FREQUENCY_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES = "STAT_FREQUENCY_TOOLTIP_IS_SHOW_PERCENTAGE_VALUES"; //$NON-NLS-1$
   public static final String STAT_FREQUENCY_TOOLTIP_IS_SHOW_SUMMARY_VALUES    = "STAT_FREQUENCY_TOOLTIP_IS_SHOW_SUMMARY_VALUES";    //$NON-NLS-1$

   /*
    * Statistic: Training
    */
   public static final String STAT_TRAINING_BAR_DURATION_TIME                           = "STAT_TRAINING_BAR_DURATION_TIME";                           //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_AVG_PACE                        = "STAT_TRAINING_BAR_IS_SHOW_AVG_PACE";                        //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED                       = "STAT_TRAINING_BAR_IS_SHOW_AVG_SPEED";                       //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_DISTANCE                        = "STAT_TRAINING_BAR_IS_SHOW_DISTANCE";                        //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_DURATION                        = "STAT_TRAINING_BAR_IS_SHOW_DURATION";                        //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_ELEVATION_UP                    = "STAT_TRAINING_BAR_IS_SHOW_ELEVATION_UP";                    //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_ELEVATION_DOWN                  = "STAT_TRAINING_BAR_IS_SHOW_ELEVATION_DOWN";                  //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT                 = "STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT";                 //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC       = "STAT_TRAINING_BAR_IS_SHOW_TRAINING_EFFECT_ANAEROBIC";       //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE            = "STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE";            //$NON-NLS-1$
   public static final String STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE  = "STAT_TRAINING_BAR_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE";  //$NON-NLS-1$

   public static final String STAT_TRAINING_LINE_DURATION_TIME                          = "STAT_TRAINING_LINE_DURATION_TIME";                          //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_AVG_PACE                       = "STAT_TRAINING_LINE_IS_SHOW_AVG_PACE";                       //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_AVG_SPEED                      = "STAT_TRAINING_LINE_IS_SHOW_AVG_SPEED";                      //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_DISTANCE                       = "STAT_TRAINING_LINE_IS_SHOW_DISTANCE";                       //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_DURATION                       = "STAT_TRAINING_LINE_IS_SHOW_DURATION";                       //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_ELEVATION_UP                   = "STAT_TRAINING_LINE_IS_SHOW_ELEVATION_UP";                   //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_ELEVATION_DOWN                 = "STAT_TRAINING_LINE_IS_SHOW_ELEVATION_DOWN";                 //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT                = "STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT";                //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT_ANAEROBIC      = "STAT_TRAINING_LINE_IS_SHOW_TRAINING_EFFECT_ANAEROBIC";      //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE           = "STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE";           //$NON-NLS-1$
   public static final String STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE = "STAT_TRAINING_LINE_IS_SHOW_TRAINING_PERFORMANCE_AVG_VALUE"; //$NON-NLS-1$

   /*
    * Statistic: Athlete's data
    */
   public static final String STAT_BODYWEIGHT_YAXIS_MIN_VISIBLE_VALUE = "STAT_BODYWEIGHT_YAXIS_MIN_VISIBLE_VALUE"; //$NON-NLS-1$
   public static final String STAT_BODYWEIGHT_YAXIS_MAX_VISIBLE_VALUE = "STAT_BODYWEIGHT_YAXIS_MAX_VISIBLE_VALUE"; //$NON-NLS-1$
   public static final String STAT_BODYFAT_YAXIS_MIN_VISIBLE_VALUE    = "STAT_BODYFAT_YAXIS_MIN_VISIBLE_VALUE";    //$NON-NLS-1$
   public static final String STAT_BODYFAT_YAXIS_MAX_VISIBLE_VALUE    = "STAT_BODYFAT_YAXIS_MAX_VISIBLE_VALUE";    //$NON-NLS-1$

   /*
    * Graphs
    */
   /** All graphs in the visible sequence */
   public static final String GRAPH_ALL                                 = "graphs.all";                                //$NON-NLS-1$

   /** All graphs which are currently displayed in the chart */
   public static final String GRAPH_VISIBLE                             = "graphs.visible";                            //$NON-NLS-1$

   public static final String GRAPH_ANTIALIASING                        = "Graph_Antialiasing";                        //$NON-NLS-1$
   public static final String GRAPH_ARE_PAUSES_VISIBLE                  = "GRAPH_ARE_PAUSES_VISIBLE";                  //$NON-NLS-1$
   public static final String GRAPH_BACKGROUND_SOURCE                   = "GRAPH_BACKGROUND_SOURCE";                   //$NON-NLS-1$
   public static final String GRAPH_BACKGROUND_STYLE                    = "GRAPH_BACKGROUND_STYLE";                    //$NON-NLS-1$
   public static final String GRAPH_IS_BREAKTIME_VALUES_VISIBLE         = "Graph_IsBreaktimeVisible";                  //$NON-NLS-1$
   public static final String GRAPH_IS_GRAPH_OVERLAPPED                 = "GRAPH_IS_GRAPH_OVERLAPPED";                 //$NON-NLS-1$
   public static final String GRAPH_IS_MARKER_VISIBLE                   = "Graph_IsMarkerVisible";                     //$NON-NLS-1$
   public static final String GRAPH_IS_SEGMENT_ALTERNATE_COLOR          = "GRAPH_IS_SEGMENT_ALTERNATE_COLOR";          //$NON-NLS-1$
   public static final String GRAPH_IS_SRTM_VISIBLE                     = "Graph_IsSRTMVisible";                       //$NON-NLS-1$
   public static final String GRAPH_IS_SHOW_SRTM_1_VALUES               = "GRAPH_IS_SHOW_SRTM_1_VALUES";               //$NON-NLS-1$
   public static final String GRAPH_IS_TOUR_PHOTO_TOOLTIP_VISIBLE       = "GRAPH_IS_TOUR_PHOTO_TOOLTIP_VISIBLE";       //$NON-NLS-1$
   public static final String GRAPH_IS_TOUR_PHOTO_VISIBLE               = "GRAPH_IS_TOUR_PHOTO_VISIBLE";               //$NON-NLS-1$
   public static final String GRAPH_SEGMENT_ALTERNATE_COLOR             = "GRAPH_SEGMENT_ALTERNATE_COLOR";             //$NON-NLS-1$
   public static final String GRAPH_SEGMENT_ALTERNATE_COLOR_DARK        = "GRAPH_SEGMENT_ALTERNATE_COLOR_DARK";        //$NON-NLS-1$

   public static final String GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE      = "GRAPH_PREF_PAGE_IS_COLOR_LIVE_UPDATE";      //$NON-NLS-1$
   public static final String GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE = "GRAPH_PREF_PAGE_IS_TOUR_CHART_LIVE_UPDATE"; //$NON-NLS-1$

   // SET_FORMATTING_OFF
   public static final String GRAPH_MARKER_COLOR_DEFAULT                                  = "GRAPH_MARKER_COLOR_DEFAULT";                                   //$NON-NLS-1$
   public static final String GRAPH_MARKER_COLOR_DEFAULT_DARK                             = "GRAPH_MARKER_COLOR_DEFAULT_DARK";                              //$NON-NLS-1$
   public static final String GRAPH_MARKER_COLOR_DEVICE                                   = "GRAPH_MARKER_COLOR_DEVICE";                                    //$NON-NLS-1$
   public static final String GRAPH_MARKER_COLOR_DEVICE_DARK                              = "GRAPH_MARKER_COLOR_DEVICE_DARK";                               //$NON-NLS-1$
   public static final String GRAPH_MARKER_COLOR_HIDDEN                                   = "GRAPH_MARKER_COLOR_HIDDEN";                                    //$NON-NLS-1$
   public static final String GRAPH_MARKER_COLOR_HIDDEN_DARK                              = "GRAPH_MARKER_COLOR_HIDDEN_DARK";                               //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR                     = "GRAPH_MARKER_IS_DRAW_WITH_DEFAULT_COLOR";                      //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_MODIFIED                                    = "GRAPH_MARKER_IS_MODIFIED";                                     //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_LABEL_ELEVATION                             = "GRAPH_MARKER_IS_LABEL_ELEVATION";                              //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_LABEL_DISTANCE                              = "GRAPH_MARKER_IS_LABEL_DISTANCE";                               //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_LABEL_TIME                                  = "GRAPH_MARKER_IS_LABEL_TIME";                                   //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_LABEL_TIME_DIFFERENCE                       = "GRAPH_MARKER_IS_LABEL_TIME_DIFFERENCE";                        //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_LABEL_DISTANCE_DIFFERENCE                   = "GRAPH_MARKER_IS_LABEL_DISTANCE_DIFFERENCE";                    //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_LABEL_ELEVATIONGAIN_DIFFERENCE              = "GRAPH_MARKER_IS_LABEL_ELEVATIONGAIN_DIFFERENCE";               //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_ABSOLUTE_VALUES                        = "GRAPH_MARKER_IS_SHOW_ABSOLUTE_VALUES";                         //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER                          = "GRAPH_MARKER_IS_SHOW_HIDDEN_MARKER";                           //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION                    = "GRAPH_MARKER_IS_SHOW_LABEL_TEMP_POSITION";                     //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_MARKER_LABEL                           = "GRAPH_MARKER_IS_SHOW_MARKER_LABEL";                            //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_MARKER_POINT                           = "GRAPH_MARKER_IS_SHOW_MARKER_POINT";                            //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP                         = "GRAPH_MARKER_IS_SHOW_MARKER_TOOLTIP";                          //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATION                 = "GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATION";                  //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE                  = "GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE";                   //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION                  = "GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION";                   //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATIONGAIN_DIFFERENCE  = "GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_ELEVATIONGAIN_DIFFERENCE";   //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE_DIFFERENCE       = "GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DISTANCE_DIFFERENCE";        //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION_DIFFERENCE       = "GRAPH_MARKER_IS_SHOW_TOOLTIP_DATA_DURATION_DIFFERENCE";        //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_ONLY_WITH_DESCRIPTION                  = "GRAPH_MARKER_IS_SHOW_ONLY_WITH_DESCRIPTION";                   //$NON-NLS-1$
   public static final String GRAPH_MARKER_IS_SHOW_SIGN_IMAGE                             = "GRAPH_MARKER_IS_SHOW_SIGN_IMAGE";                              //$NON-NLS-1$
   public static final String GRAPH_MARKER_HOVER_SIZE                                     = "GRAPH_MARKER_HOVER_OFFSET";                                    //$NON-NLS-1$
   public static final String GRAPH_MARKER_LABEL_OFFSET                                   = "GRAPH_MARKER_LABEL_OFFSET";                                    //$NON-NLS-1$
   public static final String GRAPH_MARKER_LABEL_TEMP_POSITION                            = "GRAPH_MARKER_LABEL_TEMP_POSITION";                             //$NON-NLS-1$
   public static final String GRAPH_MARKER_POINT_SIZE                                     = "GRAPH_MARKER_POINT_SIZE";                                      //$NON-NLS-1$
   public static final String GRAPH_MARKER_SIGN_IMAGE_SIZE                                = "GRAPH_MARKER_SIGN_IMAGE_SIZE";                                 //$NON-NLS-1$
   public static final String GRAPH_MARKER_TOOLTIP_POSITION                               = "GRAPH_MARKER_TOOLTIP_POSITION";                                //$NON-NLS-1$

   public static final String GRAPH_PAUSES_IS_SHOW_PAUSE_TOOLTIP                          = "GRAPH_PAUSES_IS_SHOW_PAUSE_TOOLTIP";                          //$NON-NLS-1$
   public static final String GRAPH_PAUSES_TOOLTIP_POSITION                               = "GRAPH_PAUSES_TOOLTIP_POSITION";                          //$NON-NLS-1$

   // SET_FORMATTING_ON

   public static final String GRAPH_PULSE_GRAPH_VALUES              = "GRAPH_PULSE_GRAPH_VALUES";              //$NON-NLS-1$

   /**
    * Indicator to either select all time slices in between left and right sliders or just the left
    * and right sliders time slices.
    */
   public static final String GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES = "GRAPH_IS_SELECT_INBETWEEN_TIME_SLICES"; //$NON-NLS-1$
   public static final String GRAPH_IS_SHOW_PACE_GRAPH_INVERTED     = "GRAPH_IS_SHOW_PACE_GRAPH_INVERTED";     //$NON-NLS-1$
   public static final String GRAPH_IS_SHOW_VALUE_POINT_VALUE       = "GRAPH_IS_SHOW_VALUE_POINT_VALUE";       //$NON-NLS-1$
   public static final String GRAPH_MOUSE_MODE                      = "graph.toggle-mouse";                    //$NON-NLS-1$
   public static final String GRAPH_MOVE_SLIDERS_WHEN_ZOOMED        = "graphs.move-sliders-when-zoomed";       //$NON-NLS-1$
   public static final String GRAPH_SPEED_PACE_DISTANCE_INTERVAL    = "GRAPH_SPEED_PACE_DISTANCE_INTERVAL";    //$NON-NLS-1$
   public static final String GRAPH_TRANSPARENCY_FILLING            = "Graph_Transparency_Filling";            //$NON-NLS-1$
   public static final String GRAPH_TRANSPARENCY_FILLING_DARK       = "GRAPH_TRANSPARENCY_FILLING_DARK";       //$NON-NLS-1$
   public static final String GRAPH_TRANSPARENCY_LINE               = "Graph_Transparency_Line";               //$NON-NLS-1$
   public static final String GRAPH_X_AXIS                          = "graphs.x-axis";                         //$NON-NLS-1$
   public static final String GRAPH_X_AXIS_STARTTIME                = "graphs.x-axis.starttime";               //$NON-NLS-1$
   public static final String GRAPH_ZOOM_AUTO_ZOOM_TO_SLIDER        = "graphs.zoom.autozoom-to-slider";        //$NON-NLS-1$

   public static final String GRAPH_IS_SHOW_NIGHT_SECTIONS          = "GRAPH_IS_SHOW_NIGHT_SECTIONS";          //$NON-NLS-1$
   public static final String GRAPH_NIGHT_SECTIONS_OPACITY          = "GRAPH_NIGHT_SECTIONS_OPACITY";          //$NON-NLS-1$

   /*
    * Tour info
    */
   public static final String GRAPH_TOUR_INFO_IS_VISIBLE                = "GRAPH_IS_TOUR_INFO_VISIBLE";                //$NON-NLS-1$
   public static final String GRAPH_TOUR_INFO_IS_TITLE_VISIBLE          = "GRAPH_TOUR_INFO_IS_TITLE_VISIBLE";          //$NON-NLS-1$
   public static final String GRAPH_TOUR_INFO_IS_TOOLTIP_VISIBLE        = "GRAPH_TOUR_INFO_IS_TOOLTIP_VISIBLE";        //$NON-NLS-1$
   public static final String GRAPH_TOUR_INFO_IS_TOUR_SEPARATOR_VISIBLE = "GRAPH_TOUR_INFO_IS_TOUR_SEPARATOR_VISIBLE"; //$NON-NLS-1$
   public static final String GRAPH_TOUR_INFO_TOOLTIP_DELAY             = "GRAPH_TOUR_INFO_TOOLTIP_DELAY";             //$NON-NLS-1$

   /*
    * Min/Max values
    */
   public static final String GRAPH_IS_MIN_MAX_ENABLED                          = "GRAPH_IS_MIN_MAX_ENABLED";                          //$NON-NLS-1$

   public static final String GRAPH_ALTIMETER_IS_MIN_ENABLED                    = "GRAPH_ALTIMETER_IS_MIN_ENABLED";                    //$NON-NLS-1$
   public static final String GRAPH_ALTIMETER_IS_MAX_ENABLED                    = "GRAPH_ALTIMETER_IS_MAX_ENABLED";                    //$NON-NLS-1$
   public static final String GRAPH_ALTIMETER_MIN_VALUE                         = "GRAPH_ALTIMETER_MIN_VALUE";                         //$NON-NLS-1$
   public static final String GRAPH_ALTIMETER_MAX_VALUE                         = "GRAPH_ALTIMETER_MAX_VALUE";                         //$NON-NLS-1$

   public static final String GRAPH_ALTITUDE_IS_MIN_ENABLED                     = "GRAPH_ALTITUDE_IS_MIN_ENABLED";                     //$NON-NLS-1$
   public static final String GRAPH_ALTITUDE_IS_MAX_ENABLED                     = "GRAPH_ALTITUDE_IS_MAX_ENABLED";                     //$NON-NLS-1$
   public static final String GRAPH_ALTITUDE_MIN_VALUE                          = "GRAPH_ALTITUDE_MIN_VALUE";                          //$NON-NLS-1$
   public static final String GRAPH_ALTITUDE_MAX_VALUE                          = "GRAPH_ALTITUDE_MAX_VALUE";                          //$NON-NLS-1$

   public static final String GRAPH_CADENCE_IS_MIN_ENABLED                      = "GRAPH_CADENCE_IS_MIN_ENABLED";                      //$NON-NLS-1$
   public static final String GRAPH_CADENCE_IS_MAX_ENABLED                      = "GRAPH_CADENCE_IS_MAX_ENABLED";                      //$NON-NLS-1$
   public static final String GRAPH_CADENCE_MIN_VALUE                           = "GRAPH_CADENCE_MIN_VALUE";                           //$NON-NLS-1$
   public static final String GRAPH_CADENCE_MAX_VALUE                           = "GRAPH_CADENCE_MAX_VALUE";                           //$NON-NLS-1$

   public static final String GRAPH_GRADIENT_IS_MIN_ENABLED                     = "GRAPH_GRADIENT_IS_MIN_ENABLED";                     //$NON-NLS-1$
   public static final String GRAPH_GRADIENT_IS_MAX_ENABLED                     = "GRAPH_GRADIENT_IS_MAX_ENABLED";                     //$NON-NLS-1$
   public static final String GRAPH_GRADIENT_MIN_VALUE                          = "GRAPH_GRADIENT_MIN_VALUE";                          //$NON-NLS-1$
   public static final String GRAPH_GRADIENT_MAX_VALUE                          = "GRAPH_GRADIENT_MAX_VALUE";                          //$NON-NLS-1$

   public static final String GRAPH_PACE_IS_MIN_ENABLED                         = "GRAPH_PACE_IS_MIN_ENABLED";                         //$NON-NLS-1$
   public static final String GRAPH_PACE_IS_MAX_ENABLED                         = "GRAPH_PACE_IS_MAX_ENABLED";                         //$NON-NLS-1$
   public static final String GRAPH_PACE_MIN_VALUE                              = "GRAPH_PACE_MIN_VALUE";                              //$NON-NLS-1$
   public static final String GRAPH_PACE_MAX_VALUE                              = "GRAPH_PACE_MAX_VALUE";                              //$NON-NLS-1$

   public static final String GRAPH_POWER_IS_MIN_ENABLED                        = "GRAPH_POWER_IS_MIN_ENABLED";                        //$NON-NLS-1$
   public static final String GRAPH_POWER_IS_MAX_ENABLED                        = "GRAPH_POWER_IS_MAX_ENABLED";                        //$NON-NLS-1$
   public static final String GRAPH_POWER_MIN_VALUE                             = "GRAPH_POWER_MIN_VALUE";                             //$NON-NLS-1$
   public static final String GRAPH_POWER_MAX_VALUE                             = "GRAPH_POWER_MAX_VALUE";                             //$NON-NLS-1$

   public static final String GRAPH_PULSE_IS_MIN_ENABLED                        = "GRAPH_PULSE_IS_MIN_ENABLED";                        //$NON-NLS-1$
   public static final String GRAPH_PULSE_IS_MAX_ENABLED                        = "GRAPH_PULSE_IS_MAX_ENABLED";                        //$NON-NLS-1$
   public static final String GRAPH_PULSE_MIN_VALUE                             = "GRAPH_PULSE_MIN_VALUE";                             //$NON-NLS-1$
   public static final String GRAPH_PULSE_MAX_VALUE                             = "GRAPH_PULSE_MAX_VALUE";                             //$NON-NLS-1$

   public static final String GRAPH_SPEED_IS_MIN_ENABLED                        = "GRAPH_SPEED_IS_MIN_ENABLED";                        //$NON-NLS-1$
   public static final String GRAPH_SPEED_IS_MAX_ENABLED                        = "GRAPH_SPEED_IS_MAX_ENABLED";                        //$NON-NLS-1$
   public static final String GRAPH_SPEED_MIN_VALUE                             = "GRAPH_SPEED_MIN_VALUE";                             //$NON-NLS-1$
   public static final String GRAPH_SPEED_MAX_VALUE                             = "GRAPH_SPEED_MAX_VALUE";                             //$NON-NLS-1$

   public static final String GRAPH_TEMPERATURE_IS_MIN_ENABLED                  = "GRAPH_TEMPERATURE_IS_MIN_ENABLED";                  //$NON-NLS-1$
   public static final String GRAPH_TEMPERATURE_IS_MAX_ENABLED                  = "GRAPH_TEMPERATURE_IS_MAX_ENABLED";                  //$NON-NLS-1$
   public static final String GRAPH_TEMPERATURE_MIN_VALUE                       = "GRAPH_TEMPERATURE_MIN_VALUE";                       //$NON-NLS-1$
   public static final String GRAPH_TEMPERATURE_MAX_VALUE                       = "GRAPH_TEMPERATURE_MAX_VALUE";                       //$NON-NLS-1$

   public static final String GRAPH_RUN_DYN_STANCE_TIME_IS_MIN_ENABLED          = "GRAPH_RUN_DYN_STANCE_TIME_IS_MIN_ENABLED";          //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STANCE_TIME_IS_MAX_ENABLED          = "GRAPH_RUN_DYN_STANCE_TIME_IS_MAX_ENABLED";          //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STANCE_TIME_MIN_VALUE               = "GRAPH_RUN_DYN_STANCE_TIME_MIN_VALUE";               //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STANCE_TIME_MAX_VALUE               = "GRAPH_RUN_DYN_STANCE_TIME_MAX_VALUE";               //$NON-NLS-1$

   public static final String GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MIN_ENABLED  = "GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MIN_ENABLED";  //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MAX_ENABLED  = "GRAPH_RUN_DYN_STANCE_TIME_BALANCE_IS_MAX_ENABLED";  //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MIN_VALUE       = "GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MIN_VALUE";       //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MAX_VALUE       = "GRAPH_RUN_DYN_STANCE_TIME_BALANCE_MAX_VALUE";       //$NON-NLS-1$

   public static final String GRAPH_RUN_DYN_STEP_LENGTH_IS_MIN_ENABLED          = "GRAPH_RUN_DYN_STEP_LENGTH_IS_MIN_ENABLED";          //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STEP_LENGTH_IS_MAX_ENABLED          = "GRAPH_RUN_DYN_STEP_LENGTH_IS_MAX_ENABLED";          //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STEP_LENGTH_MIN_VALUE               = "GRAPH_RUN_DYN_STEP_LENGTH_MIN_VALUE";               //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_STEP_LENGTH_MAX_VALUE               = "GRAPH_RUN_DYN_STEP_LENGTH_MAX_VALUE";               //$NON-NLS-1$

   public static final String GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MIN_ENABLED = "GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MIN_ENABLED"; //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MAX_ENABLED = "GRAPH_RUN_DYN_VERTICAL_OSCILLATION_IS_MAX_ENABLED"; //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MIN_VALUE      = "GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MIN_VALUE";      //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MAX_VALUE      = "GRAPH_RUN_DYN_VERTICAL_OSCILLATION_MAX_VALUE";      //$NON-NLS-1$

   public static final String GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MIN_ENABLED       = "GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MIN_ENABLED";       //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MAX_ENABLED       = "GRAPH_RUN_DYN_VERTICAL_RATIO_IS_MAX_ENABLED";       //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_VERTICAL_RATIO_MIN_VALUE            = "GRAPH_RUN_DYN_VERTICAL_RATIO_MIN_VALUE";            //$NON-NLS-1$
   public static final String GRAPH_RUN_DYN_VERTICAL_RATIO_MAX_VALUE            = "GRAPH_RUN_DYN_VERTICAL_RATIO_MAX_VALUE";            //$NON-NLS-1$

   public static final String GRAPH_SWIM_STROKES_IS_MIN_ENABLED                 = "GRAPH_SWIM_STROKES_IS_MIN_ENABLED";                 //$NON-NLS-1$
   public static final String GRAPH_SWIM_STROKES_IS_MAX_ENABLED                 = "GRAPH_SWIM_STROKES_IS_MAX_ENABLED";                 //$NON-NLS-1$
   public static final String GRAPH_SWIM_STROKES_MIN_VALUE                      = "GRAPH_SWIM_STROKES_MIN_VALUE";                      //$NON-NLS-1$
   public static final String GRAPH_SWIM_STROKES_MAX_VALUE                      = "GRAPH_SWIM_STROKES_MAX_VALUE";                      //$NON-NLS-1$

   public static final String GRAPH_SWIM_SWOLF_IS_MIN_ENABLED                   = "GRAPH_SWIM_SWOLF_IS_MIN_ENABLED";                   //$NON-NLS-1$
   public static final String GRAPH_SWIM_SWOLF_IS_MAX_ENABLED                   = "GRAPH_SWIM_SWOLF_IS_MAX_ENABLED";                   //$NON-NLS-1$
   public static final String GRAPH_SWIM_SWOLF_MIN_VALUE                        = "GRAPH_SWIM_SWOLF_MIN_VALUE";                        //$NON-NLS-1$
   public static final String GRAPH_SWIM_SWOLF_MAX_VALUE                        = "GRAPH_SWIM_SWOLF_MAX_VALUE";                        //$NON-NLS-1$

   public static final String GRAPH_SMOOTHING_SMOOTHING_ALGORITHM               = "GraphSmoothing_SmoothingAlgorithm";                 //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_IS_SYNCH_SMOOTHING          = "GraphJametSmoothing_IsSynchSmoothing";              //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_IS_ALTITUDE                 = "GraphJametSmoothing_IsAltitudeSmoothing";           //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_IS_PULSE                    = "GraphJametSmoothing_IsPulseSmoothing";              //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_GRADIENT_TAU                = "GraphJametSmoothing_GradientSmoothingTau";          //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_PULSE_TAU                   = "GraphJametSmoothing_PulseSmoothingTau";             //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_SPEED_TAU                   = "GraphJametSmoothing_SpeedSmoothingTau";             //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_REPEATED_SMOOTHING          = "GraphJametSmoothing_RepeatedSmoothing";             //$NON-NLS-1$
   public static final String GRAPH_JAMET_SMOOTHING_REPEATED_TAU                = "GraphJametSmoothing_RepeatedTau";                   //$NON-NLS-1$

   /**
    * Graph color change flag.
    * <p>
    * Graph colors are set in the {@link CommonActivator} pref store, but the change flag is hosted
    * in the {@link TourbookPlugin} pref store.
    */
   public static final String GRAPH_COLORS_HAS_CHANGED                          = "graph.colors.has-changed";                          //$NON-NLS-1$

   public static final String GRAPH_PROPERTY_CHARTTYPE                          = "graph.property.chartType";                          //$NON-NLS-1$

   public static final String GRAPH_PROPERTY_IS_VALUE_CLIPPING                  = "graph.property.is.value_clipping";                  //$NON-NLS-1$
   public static final String GRAPH_PROPERTY_VALUE_CLIPPING_TIMESLICE           = "graph.property.timeslice.value_clipping";           //$NON-NLS-1$

   public static final String GRAPH_PROPERTY_IS_PACE_CLIPPING                   = "graph.property.is.pace_clipping";                   //$NON-NLS-1$
   public static final String GRAPH_PROPERTY_PACE_CLIPPING_VALUE                = "graph.property.is.pace_clipping.value";             //$NON-NLS-1$

   /*
    * Chart grid
    */
   public static final String CHART_GRID_VERTICAL_DISTANCE            = "CHART_GRID_VERTICAL_DISTANCE";            //$NON-NLS-1$
   public static final String CHART_GRID_HORIZONTAL_DISTANCE          = "CHART_GRID_HORIZONTAL_DISTANCE";          //$NON-NLS-1$
   public static final String CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES = "CHART_GRID_IS_SHOW_HORIZONTAL_GRIDLINES"; //$NON-NLS-1$
   public static final String CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES   = "CHART_GRID_IS_SHOW_VERTICAL_GRIDLINES";   //$NON-NLS-1$

   /*
    * Chart layout
    */
   public static final String CHART_Y_AXIS_WIDTH                 = "CHART_Y_AXIS_WIDTH";                         //$NON-NLS-1$

   /**
    * Tour type
    */
   public static final String TOUR_TYPE_IMPORT_DEFAUL_ID         = "TourType_ImportDefaultID";                   //$NON-NLS-1$
   public static final String TOUR_TYPE_LIST_IS_MODIFIED         = "tourtype.list.is-modified";                  //$NON-NLS-1$
   public static final String TOUR_TYPE_FILTER_LIST              = "tourtype.filter.list";                       //$NON-NLS-1$

   public static final String TOUR_BIKE_LIST_IS_MODIFIED         = "tourbike.list.is-modified";                  //$NON-NLS-1$
   public static final String TOUR_PERSON_LIST_IS_MODIFIED       = "tourperson.list.is-modified";                //$NON-NLS-1$

   public static final String APP_LAST_SELECTED_PERSON_ID        = "application.last-selected-person-id";        //$NON-NLS-1$
   public static final String APP_LAST_SELECTED_TOUR_TYPE_FILTER = "application.last-selected-tourtypefilter";   //$NON-NLS-1$

   /**
    * event is fired when a person or a tour type is modified
    */
   public static final String APP_DATA_FILTER_IS_MODIFIED        = "application.data-filter-is-modified";        //$NON-NLS-1$

   public static final String APP_DATA_SPEED_MIN_TIMESLICE_VALUE = "application.data-speed-min-timeslice-value"; //$NON-NLS-1$

   public static final String APP_TOUR_GEO_FILTER_IS_SELECTED    = "APP_TOUR_DATA_FILTER_IS_SELECTED";           //$NON-NLS-1$
   public static final String APP_TOUR_DATA_FILTER_IS_SELECTED   = "APP_TOUR_GEO_FILTER_IS_SELECTED";            //$NON-NLS-1$
   public static final String APP_TOUR_TAG_FILTER_IS_SELECTED    = "APP_TOUR_TAG_FILTER_IS_SELECTED";            //$NON-NLS-1$

   /**
    * initially this was an int value, with 2 it's a string
    */
   public static final String BREAK_TIME_METHOD2                 = "BreakTime_Method2";                          //$NON-NLS-1$

   public static final String BREAK_TIME_IS_MODIFIED             = "BreakTime_IsModified";                       //$NON-NLS-1$
   public static final String BREAK_TIME_MIN_AVG_SPEED_AS        = "BreakTime_MinAvgSpeedAS";                    //$NON-NLS-1$
   public static final String BREAK_TIME_MIN_SLICE_SPEED_AS      = "BreakTime_MinSliceSpeedAS";                  //$NON-NLS-1$
   public static final String BREAK_TIME_MIN_SLICE_TIME_AS       = "BreakTime_MinSliceTimeAS";                   //$NON-NLS-1$
   public static final String BREAK_TIME_MIN_AVG_SPEED           = "BreakTime_MinAvgSpeed";                      //$NON-NLS-1$
   public static final String BREAK_TIME_MIN_SLICE_SPEED         = "BreakTime_MinSliceSpeed";                    //$NON-NLS-1$
   public static final String BREAK_TIME_SHORTEST_TIME           = "BreakTime_ShortestTime";                     //$NON-NLS-1$
   public static final String BREAK_TIME_MAX_DISTANCE            = "BreakTime_MaxDistance";                      //$NON-NLS-1$
   public static final String BREAK_TIME_SLICE_DIFF              = "BreakTime_SliceDiff";                        //$NON-NLS-1$

   /**
    * DP tolerance and gradient when computing flat, gain and loss areas
    */
   public static final String FLAT_GAIN_LOSS_DP_TOLERANCE        = "FlatGainLoss_DPTolerance";                   //$NON-NLS-1$
   public static final String FLAT_GAIN_LOSS_FLAT_GRADIENT       = "FlatGainLoss_FlatGradient";                  //$NON-NLS-1$

   /**
    * DP tolerance when computing elevation gain/loss
    */
   public static final String COMPUTED_ALTITUDE_DP_TOLERANCE     = "COMPUTED_ALTITUDE_DP_TOLERANCE";             //$NON-NLS-1$

   /**
    * Cadence value differentiating slow from fast
    */
   public static final String CADENCE_ZONES_DELIMITER            = "CADENCE_ZONES_DELIMITER";                    //$NON-NLS-1$

   /*
    * map settings
    */
   public static final String MAP_VIEW_CONFIRMATION_SHOW_DIM_WARNING = "map.view.confirmation.show-dim-warning"; //$NON-NLS-1$

   /*
    * regional settings
    */
   public static final String REGIONAL_USE_CUSTOM_DECIMAL_FORMAT    = "regional_use.custom.decimal.format"; //$NON-NLS-1$
   public static final String REGIONAL_DECIMAL_SEPARATOR            = "regional_decimal.separator";         //$NON-NLS-1$
   public static final String REGIONAL_GROUP_SEPARATOR              = "regional_group.separator";           //$NON-NLS-1$

   /**
    * layout for the views have been changed
    */
   public static final String VIEW_LAYOUT_CHANGED                   = "view.layout.changed";                //$NON-NLS-1$
   public static final String VIEW_LAYOUT_DISPLAY_LINES             = "view.layout.display.lines";          //$NON-NLS-1$

   /**
    * Layout for maps
    */
   public static final String MAP_LAYOUT_BORDER_TYPE                = "MAP_LAYOUT_BORDER_TYPE";             //$NON-NLS-1$
   public static final String MAP_LAYOUT_BORDER_COLOR               = "MAP_LAYOUT_BORDER_COLOR";            //$NON-NLS-1$
   public static final String MAP_LAYOUT_BORDER_DIMM_VALUE          = "MAP_LAYOUT_BORDER_DIMM_VALUE";       //$NON-NLS-1$
   public static final String MAP_LAYOUT_BORDER_WIDTH               = "map.layout.borderWidth";             //$NON-NLS-1$
   public static final String MAP_LAYOUT_IS_ANTIALIAS_PAINTING      = "MAP_LAYOUT_IS_ANTIALIAS_PAINTING";   //$NON-NLS-1$
   public static final String MAP_LAYOUT_IS_CUT_OFF_LINES_IN_PAUSES = "MAP_LAYOUT_IS_DRAW_LINES_IN_PAUSES"; //$NON-NLS-1$
   public static final String MAP_LAYOUT_LIVE_UPDATE                = "MAP_LAYOUT_LIVE_UPDATE";             //$NON-NLS-1$
   public static final String MAP_LAYOUT_PAINT_WITH_BORDER          = "map.layout.paintWithBorder";         //$NON-NLS-1$
   public static final String MAP_LAYOUT_PLOT_TYPE                  = "map.layout.symbol";                  //$NON-NLS-1$
   public static final String MAP_LAYOUT_SYMBOL_WIDTH               = "map.layout.symbol-width";            //$NON-NLS-1$

   public static final String MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY     = "MAP2_LAYOUT_IS_TOUR_TRACK_OPACITY";  //$NON-NLS-1$
   public static final String MAP2_LAYOUT_TOUR_TRACK_OPACITY        = "MAP2_LAYOUT_TOUR_TRACK_OPACITY";     //$NON-NLS-1$

   /*
    * Geo compare
    */
   public static final String GEO_COMPARE_COMPARED_TOUR_PART_RGB = "GEO_COMPARE_COMPARED_TOUR_PART_RGB"; //$NON-NLS-1$
   public static final String GEO_COMPARE_REF_TOUR_LINE_WIDTH    = "GEO_COMPARE_REF_TOUR_LINE_WIDTH";    //$NON-NLS-1$
   public static final String GEO_COMPARE_REF_TOUR_RGB           = "GEO_COMPARE_REF_TOUR_RGB";           //$NON-NLS-1$

   /*
    * Tour data editor
    */
   public static final String TOURDATA_EDITOR_CONFIRMATION_REVERT_TOUR = "tourdata.editor.confirmation.revert-tour"; //$NON-NLS-1$

   /*
    * Common appearance
    */
   public static final String APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES        = "appearance.NumberOfRecentTourTypes";                //$NON-NLS-1$
   public static final String APPEARANCE_NUMBER_OF_RECENT_TAGS              = "appearance.number-of-recent-tags";                  //$NON-NLS-1$
   public static final String APPEARANCE_SHOW_TOUR_TYPE_CONTEXT_MENU        = "appearance.ShowTourTypeContextMenuOnMouseHovering"; //$NON-NLS-1$

   public static final String APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME = "Appearance.IsPaceAndSpeedFromRecordedTime";         //$NON-NLS-1$
   public static final String APPEARANCE_IS_TAGGING_AUTO_OPEN               = "Appearance.IsAutoOpenTagging";                      //$NON-NLS-1$
   public static final String APPEARANCE_IS_TAGGING_ANIMATION               = "Appearance.IsTaggingAnimation";                     //$NON-NLS-1$
   public static final String APPEARANCE_TAGGING_AUTO_OPEN_DELAY            = "Appearance.AutoOpenTaggingDelay";                   //$NON-NLS-1$

   /*
    * merge tour dialog
    */
   public static final String MERGE_TOUR_GRAPH_X_AXIS            = "merge.tour.chart-x-axis";                      //$NON-NLS-1$

   public static final String MERGE_TOUR_PREVIEW_CHART           = "merge.tour.preview-chart";                     //$NON-NLS-1$
   public static final String MERGE_TOUR_ALTITUDE_DIFF_SCALING   = "merge.tour.altitude-diff-scaling";             //$NON-NLS-1$

   public static final String MERGE_TOUR_SYNC_START_TIME         = "merge.tour.synch-start-time";                  //$NON-NLS-1$
   public static final String MERGE_TOUR_ADJUST_START_ALTITUDE   = "merge.tour.adjust-start-altitude";             //$NON-NLS-1$

   public static final String MERGE_TOUR_SET_TOUR_TYPE           = "merge.tour.set-tour-type-for-merge-from-tour"; //$NON-NLS-1$
   public static final String MERGE_TOUR_SET_TOUR_TYPE_ID        = "merge.tour.set-tour-type-id";                  //$NON-NLS-1$

   public static final String MERGE_TOUR_LINEAR_INTERPOLATION    = "merge.tour.use-linear-interpolation";          //$NON-NLS-1$

   public static final String MERGE_TOUR_MERGE_GRAPH_ALTITUDE    = "merge.tour.merge-graph-altitude";              //$NON-NLS-1$
   public static final String MERGE_TOUR_MERGE_GRAPH_PULSE       = "merge.tour.merge-graph-pulse";                 //$NON-NLS-1$
   public static final String MERGE_TOUR_MERGE_GRAPH_SPEED       = "merge.tour.merge-graph-speed";                 //$NON-NLS-1$
   public static final String MERGE_TOUR_MERGE_GRAPH_TEMPERATURE = "merge.tour.merge-graph-temperature";           //$NON-NLS-1$
   public static final String MERGE_TOUR_MERGE_GRAPH_CADENCE     = "merge.tour.merge-graph-cadence";               //$NON-NLS-1$

   /*
    * dialog: adjust altitude
    */
   public static final String ADJUST_ALTITUDE_CHART_X_AXIS_UNIT = "adjust.altitude.x-axis-unit"; //$NON-NLS-1$

   /*
    * view tooltip
    */
   public static final String VIEW_TOOLTIP                        = "view.tooltip.";                         //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_IS_MODIFIED            = VIEW_TOOLTIP + "isModified";             //$NON-NLS-1$

   public static final String VIEW_TOOLTIP_COLLATED_COLLATION     = "VIEW_TOOLTIP_COLLATED_COLLATION";       //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_COLLATED_TIME          = "VIEW_TOOLTIP_COLLATED_TIME";            //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_COLLATED_TITLE         = "VIEW_TOOLTIP_COLLATED_TITLE";           //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_COLLATED_TAGS          = "VIEW_TOOLTIP_COLLATED_TAGS";            //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_COLLATED_WEEKDAY       = "VIEW_TOOLTIP_COLLATED_WEEKDAY";         //$NON-NLS-1$

   public static final String VIEW_TOOLTIP_TOURBOOK_DATE          = VIEW_TOOLTIP + "tourbook.date";          //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURBOOK_TIME          = VIEW_TOOLTIP + "tourbook.time";          //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURBOOK_WEEKDAY       = VIEW_TOOLTIP + "tourbook.weekday";       //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURBOOK_TITLE         = VIEW_TOOLTIP + "tourbook.title";         //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURBOOK_TAGS          = VIEW_TOOLTIP + "tourbook.tags";          //$NON-NLS-1$

   public static final String VIEW_TOOLTIP_TAGGING_TAG            = VIEW_TOOLTIP + "tagging.tag";            //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TAGGING_TAGS           = VIEW_TOOLTIP + "tagging.tags";           //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TAGGING_TITLE          = VIEW_TOOLTIP + "tagging.title";          //$NON-NLS-1$

   public static final String VIEW_TOOLTIP_TOURCATALOG_REFTOUR    = VIEW_TOOLTIP + "tourcatalog.reftour";    //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURCATALOG_TAGS       = VIEW_TOOLTIP + "tourcatalog.tags";       //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURCATALOG_TITLE      = VIEW_TOOLTIP + "tourcatalog.title";      //$NON-NLS-1$

   public static final String VIEW_TOOLTIP_TOURIMPORT_DATE        = VIEW_TOOLTIP + "tourimport.date";        //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURIMPORT_TIME        = VIEW_TOOLTIP + "tourimport.time";        //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURIMPORT_TAGS        = VIEW_TOOLTIP + "tourimport.tags";        //$NON-NLS-1$
   public static final String VIEW_TOOLTIP_TOURIMPORT_TITLE       = VIEW_TOOLTIP + "tourimport.title";       //$NON-NLS-1$

   public static final String VIEW_TOOLTIP_TOURCOMPARERESULT_TIME = VIEW_TOOLTIP + "tourcompareresult.time"; //$NON-NLS-1$

   /*
    * view actions
    */
   public static final String VIEW_DOUBLE_CLICK_ACTIONS = "ViewDoubleClickActions"; //$NON-NLS-1$

   /*
    * View: combined temperatures
    */
   public static final String VIEW_PREFERRED_TEMPERATURE_VALUE = "VIEW_PREFERRED_TEMPERATURE_VALUE"; //$NON-NLS-1$

   /*
    * Dialog toggle states
    */
   public static final String TOGGLE_STATE_GEO_FILTER_DELETE_ALL_WITHOUT_NAME = "TOGGLE_STATE_GEO_FILTER_DELETE_ALL_WITHOUT_NAME";//$NON-NLS-1$

   public static final String TOGGLE_STATE_DELETE_TOUR_VALUES                 = "TOGGLE_STATE_DELETE_TOUR_VALUES";                //$NON-NLS-1$
   public static final String TOGGLE_STATE_REIMPORT_TOUR_VALUES               = "TOGGLE_STATE_REIMPORT_TOUR_VALUES";              //$NON-NLS-1$

   public static final String TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING     = "TOGGLE_STATE_SHOW_HISTORY_TOUR_SAVE_WARNING";    //$NON-NLS-1$
   public static final String TOGGLE_STATE_SHOW_STAR_RATING_SAVE_WARNING      = "TOGGLE_STATE_SHOW_STAR_RATING_SAVE_WARNING";     //$NON-NLS-1$

   /*
    * Value point tool tip
    */
   public static final String VALUE_POINT_TOOL_TIP_IS_VISIBLE_MAP2  = "VALUE_POINT_TOOL_TIP_IS_VISIBLE_MAP2";  //$NON-NLS-1$
   public static final String VALUE_POINT_TOOL_TIP_IS_VISIBLE_CHART = "VALUE_POINT_TOOL_TIP_IS_VISIBLE_CHART"; //$NON-NLS-1$

   /*
    * Map 2 D
    */
   public static final String MAP2_OPTIONS_IS_MODIFIED = "MAP2_OPTIONS_IS_MODIFIED"; //$NON-NLS-1$

   /*
    * Map 2.5D
    */
   public static final String MAP25_OFFLINE_MAP_CUSTOM_LOCATION     = "MAP25_OFFLINE_MAP_CUSTOM_LOCATION";     //$NON-NLS-1$
   public static final String MAP25_OFFLINE_MAP_IS_DEFAULT_LOCATION = "MAP25_OFFLINE_MAP_IS_DEFAULT_LOCATION"; //$NON-NLS-1$

   /*
    * Pref page: Map3 color
    */
   /**
    * Colors must be retrieved again from the {@link MapColorProvider}, another instance is used for
    * the same {@link MapGraphId}.
    */
   public static final String MAP3_COLOR_IS_MODIFIED           = "MAP3_COLOR_IS_MODIFIED";           //$NON-NLS-1$

   public static final String MAP3_IS_COLOR_SELECTOR_DISPLAYED = "MAP3_IS_COLOR_SELECTOR_DISPLAYED"; //$NON-NLS-1$

   /*
    * Tour segmenter
    */
   public static final String TOUR_SEGMENTER_CHART_VALUE_FONT = "TOUR_SEGMENTER_CHART_VALUE_FONT"; //$NON-NLS-1$

   /*
    * Adjust temperature
    */
   public static final String ADJUST_TEMPERATURE_AVG_TEMPERATURE = "ADJUST_TEMPERATURE_AVG_TEMPERATURE"; //$NON-NLS-1$
   public static final String ADJUST_TEMPERATURE_DURATION_TIME   = "ADJUST_TEMPERATURE_DURATION_TIME";   //$NON-NLS-1$

   /*
    * Heart rate variability
    */
   public static final String HRV_OPTIONS_2X_ERROR_TOLERANCE = "HRV_OPTIONS_2X_ERROR_TOLERANCE"; //$NON-NLS-1$
   public static final String HRV_OPTIONS_IS_FIX_2X_ERROR    = "HRV_OPTIONS_IS_FIX_2X_ERROR";    //$NON-NLS-1$

   /*
    * Time zone
    */
   public static final String DIALOG_SET_TIME_ZONE_ACTION           = "DIALOG_SET_TIME_ZONE_ACTION";           //$NON-NLS-1$
   public static final String DIALOG_SET_TIME_ZONE_SELECTED_ZONE_ID = "DIALOG_SET_TIME_ZONE_SELECTED_ZONE_ID"; //$NON-NLS-1$

   /*
    * Save tags
    */
   public static final String DIALOG_SAVE_TAGS_ACTION = "DIALOG_SAVE_TAGS_ACTION"; //$NON-NLS-1$

   /*
    * Fonts
    */
   public static final String FONT_LOGGING_IS_MODIFIED = "FONT_LOGGING_IS_MODIFIED"; //$NON-NLS-1$

   /*
    * Weather
    */
   /** This is the api key for world weather online */
   public static final String WEATHER_API_KEY                              = "WEATHER_API_KEY";                              //$NON-NLS-1$
   public static final String WEATHER_DISPLAY_FULL_LOG                     = "WEATHER_DISPLAY_FULL_LOG";                     //$NON-NLS-1$
   public static final String WEATHER_IS_APPEND_WEATHER_DESCRIPTION        = "WEATHER_IS_APPEND_WEATHER_DESCRIPTION";        //$NON-NLS-1$
   public static final String WEATHER_SAVE_LOG_IN_TOUR_WEATHER_DESCRIPTION = "WEATHER_SAVE_LOG_IN_TOUR_WEATHER_DESCRIPTION"; //$NON-NLS-1$
   public static final String WEATHER_WEATHER_PROVIDER_ID                  = "WEATHER_WEATHER_PROVIDER_ID";                  //$NON-NLS-1$

   /*
    * Tour marker view
    */
   public static final String TOURMARKERVIEW_USE_ELAPSED_TIME  = "TOURMARKERVIEW_USE_ELAPSED_TIME";  //$NON-NLS-1$
   public static final String TOURMARKERVIEW_USE_MOVING_TIME   = "TOURMARKERVIEW_USE_MOVING_TIME";   //$NON-NLS-1$
   public static final String TOURMARKERVIEW_USE_RECORDED_TIME = "TOURMARKERVIEW_USE_RECORDED_TIME"; //$NON-NLS-1$

   /*
    * Fulltext index
    */
   public static final String FULLTEXT_INDEX_DELETE = "FULLTEXT_INDEX_DELETE"; //$NON-NLS-1$

   /*
    * Tour nutrition view
    */
   public static final String NUTRITION_BEVERAGECONTAINERS_HAVE_CHANGED = "NUTRITION_BEVERAGECONTAINERS_HAVE_CHANGED"; //$NON-NLS-1$
   public static final String NUTRITION_IGNORE_FIRST_HOUR               = "NUTRITION_IGNORE_FIRST_HOUR";               //$NON-NLS-1$
}
