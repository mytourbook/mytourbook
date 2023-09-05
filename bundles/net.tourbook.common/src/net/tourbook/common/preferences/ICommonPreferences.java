/*******************************************************************************
 * Copyright (C) 2012, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.common.preferences;

public interface ICommonPreferences {

   /*
    * Appearance, is mainly used for debugging purposes
    */
   public static final String APPEARANCE_IS_SHOW_MEMORY_MONITOR_IN_APP       = "APPEARANCE_IS_SHOW_MEMORY_MONITOR_IN_APP";       //$NON-NLS-1$
   public static final String APPEARANCE_IS_SHOW_RESTART_APP_ACTION_IN_APP   = "APPEARANCE_IS_SHOW_RESTART_APP_ACTION_IN_APP";   //$NON-NLS-1$
   public static final String APPEARANCE_IS_SHOW_SCRAMBLE_DATA_ACTION_IN_APP = "APPEARANCE_IS_SHOW_SCRAMBLE_DATA_ACTION_IN_APP"; //$NON-NLS-1$

   /*
    * Theme
    */
   public static final String THEME_IS_THEME_MODIFIED             = "THEME_IS_THEME_MODIFIED";             //$NON-NLS-1$
   public static final String THEME_IS_SHOW_THEME_SELECTOR_IN_APP = "THEME_IS_SHOW_THEME_SELECTOR_IN_APP"; //$NON-NLS-1$

   /*
    * Measurement system
    */
   public static final String MEASUREMENT_SYSTEM            = "MEASUREMENT_SYSTEM";            //$NON-NLS-1$
   public static final String MEASUREMENT_SYSTEM_SHOW_IN_UI = "MEASUREMENT_SYSTEM_SHOW_IN_UI"; //$NON-NLS-1$

   /*
    * Colors
    */
   public static final String GRAPH_COLORS                         = "graph.colors.";                        //$NON-NLS-1$

   public static final String DISPLAY_FORMAT_ALTITUDE              = "DISPLAY_FORMAT_ALTITUDE";              //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_CADENCE               = "DISPLAY_FORMAT_CADENCE";               //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_DISTANCE              = "DISPLAY_FORMAT_DISTANCE";              //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_POWER                 = "DISPLAY_FORMAT_POWER";                 //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_PULSE                 = "DISPLAY_FORMAT_PULSE";                 //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_SPEED                 = "DISPLAY_FORMAT_SPEED";                 //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_TEMPERATURE           = "DISPLAY_FORMAT_TEMPERATURE";           //$NON-NLS-1$

   public static final String DISPLAY_FORMAT_ELAPSED_TIME          = "DISPLAY_FORMAT_ELAPSED_TIME";          //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_RECORDED_TIME         = "DISPLAY_FORMAT_RECORDED_TIME";         //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_PAUSED_TIME           = "DISPLAY_FORMAT_PAUSED_TIME";           //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_MOVING_TIME           = "DISPLAY_FORMAT_MOVING_TIME";           //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_BREAK_TIME            = "DISPLAY_FORMAT_BREAK_TIME";            //$NON-NLS-1$

   public static final String DISPLAY_FORMAT_ALTITUDE_SUMMARY      = "DISPLAY_FORMAT_ALTITUDE_SUMMARY";      //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_CADENCE_SUMMARY       = "DISPLAY_FORMAT_CADENCE_SUMMARY";       //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_DISTANCE_SUMMARY      = "DISPLAY_FORMAT_DISTANCE_SUMMARY";      //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_POWER_SUMMARY         = "DISPLAY_FORMAT_POWER_SUMMARY";         //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_PULSE_SUMMARY         = "DISPLAY_FORMAT_PULSE_SUMMARY";         //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_SPEED_SUMMARY         = "DISPLAY_FORMAT_SPEED_SUMMARY";         //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_TEMPERATURE_SUMMARY   = "DISPLAY_FORMAT_TEMPERATURE_SUMMARY";   //$NON-NLS-1$

   public static final String DISPLAY_FORMAT_ELAPSED_TIME_SUMMARY  = "DISPLAY_FORMAT_ELAPSED_TIME_SUMMARY";  //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_RECORDED_TIME_SUMMARY = "DISPLAY_FORMAT_RECORDED_TIME_SUMMARY"; //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_PAUSED_TIME_SUMMARY   = "DISPLAY_FORMAT_PAUSED_TIME_SUMMARY";   //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_MOVING_TIME_SUMMARY   = "DISPLAY_FORMAT_MOVING_TIME_SUMMARY";   //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_BREAK_TIME_SUMMARY    = "DISPLAY_FORMAT_BREAK_TIME_SUMMARY";    //$NON-NLS-1$

   public static final String DISPLAY_FORMAT_IS_LIVE_UPDATE        = "DISPLAY_FORMAT_IS_LIVE_UPDATE";        //$NON-NLS-1$
   public static final String DISPLAY_FORMAT_SELECTED_TAB          = "DISPLAY_FORMAT_SELECTED_TAB";          //$NON-NLS-1$

   /*
    * Timezone
    */
   public static final String TIME_ZONE_IS_LIVE_UPDATE          = "TIME_ZONE_IS_LIVE_UPDATE";          //$NON-NLS-1$
   public static final String TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE = "TIME_ZONE_IS_USE_SYSTEM_TIME_ZONE"; //$NON-NLS-1$
   public static final String TIME_ZONE_LOCAL_ID                = "TIME_ZONE_LOCAL_ID";                //$NON-NLS-1$
   public static final String TIME_ZONE_LOCAL_ID_1              = "TIME_ZONE_LOCAL_ID_1";              //$NON-NLS-1$
   public static final String TIME_ZONE_LOCAL_ID_2              = "TIME_ZONE_LOCAL_ID_2";              //$NON-NLS-1$
   public static final String TIME_ZONE_LOCAL_ID_3              = "TIME_ZONE_LOCAL_ID_3";              //$NON-NLS-1$
   public static final String TIME_ZONE_SELECTED_CUSTOM_ZONE    = "TIME_ZONE_SELECTED_CUSTOM_ZONE";    //$NON-NLS-1$

   /*
    * Calendar week
    */
   /** MO=1 .. SO=7 */
   public static final String CALENDAR_WEEK_FIRST_DAY_OF_WEEK      = "CALENDAR_WEEK_FIRST_DAY_OF_WEEK";      //$NON-NLS-1$
   public static final String CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK = "CALENDAR_WEEK_MIN_DAYS_IN_FIRST_WEEK"; //$NON-NLS-1$

   /*
    * Transform values
    */
   public static final String TRANSFORM_VALUE_OPACITY_MAX = "TRANSFORM_VALUE_OPACITY_MAX"; //$NON-NLS-1$

}
