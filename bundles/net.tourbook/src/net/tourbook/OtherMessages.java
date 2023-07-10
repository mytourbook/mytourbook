/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook;

/**
 * Contains links to Messages which are not in {@link Messages}.
 * <p>
 * This is needed because the externalize string tool do not support different Messages
 * classes in one Java file
 * <p>
 * After many years I found this solution which has the advantage that the 2nd Messages in one Java
 * file must not be set into comments when using the externalized string tool.
 */
public class OtherMessages {

// SET_FORMATTING_OFF

   public static final String APP_ACTION_CLOSE_TOOLTIP                   = net.tourbook.common.Messages.App_Action_Close_Tooltip;
   public static final String APP_ACTION_RESTORE_DEFAULT                 = net.tourbook.common.Messages.App_Action_RestoreDefault;

   public static final String APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP    = net.tourbook.common.Messages.App_Theme_BackgroundColor_Dark_Tooltip;
   public static final String APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP   = net.tourbook.common.Messages.App_Theme_BackgroundColor_Light_Tooltip;
   public static final String APP_THEME_BRIGHT_THEME                     = net.tourbook.common.Messages.App_Theme_BrightTheme;
   public static final String APP_THEME_BRIGHT_THEME_TOOLTIP             = net.tourbook.common.Messages.App_Theme_BrightTheme_Tooltip;
   public static final String APP_THEME_DARK_THEME                       = net.tourbook.common.Messages.App_Theme_DarkTheme;
   public static final String APP_THEME_DARK_THEME_TOOLTIP               = net.tourbook.common.Messages.App_Theme_DarkTheme_Tooltip;
   public static final String APP_THEME_FOREGROUND_COLOR_DARK_TOOLTIP    = net.tourbook.common.Messages.App_Theme_ForegroundColor_Dark_Tooltip;
   public static final String APP_THEME_FOREGROUND_COLOR_LIGHT_TOOLTIP   = net.tourbook.common.Messages.App_Theme_ForegroundColor_Light_Tooltip;
   public static final String APP_THEME_VALUE_FOR_LIGHT_TOOLTIP          = net.tourbook.common.Messages.App_Theme_ValueFor_Light_Tooltip;
   public static final String APP_THEME_VALUE_FOR_DARK_TOOLTIP           = net.tourbook.common.Messages.App_Theme_ValueFor_Dark_Tooltip;

   public static final String GRAPH_LABEL_ALTIMETER                      = net.tourbook.common.Messages.Graph_Label_Altimeter;
   public static final String GRAPH_LABEL_ALTITUDE                       = net.tourbook.common.Messages.Graph_Label_Altitude;
   public static final String GRAPH_LABEL_CADENCE                        = net.tourbook.common.Messages.Graph_Label_Cadence;
   public static final String GRAPH_LABEL_CADENCE_UNIT                   = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
   public static final String GRAPH_LABEL_CADENCE_UNIT_RPM               = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
   public static final String GRAPH_LABEL_CADENCE_UNIT_RPM_SPM           = net.tourbook.common.Messages.Graph_Label_Cadence_Unit_RpmSpm;
   public static final String GRAPH_LABEL_CADENCE_UNIT_SPM               = net.tourbook.common.Messages.Graph_Label_Cadence_Unit_Spm;
   public static final String GRAPH_LABEL_DISTANCE                       = net.tourbook.common.Messages.Graph_Label_Distance;
   public static final String GRAPH_LABEL_ELEVATION_GAIN                 = net.tourbook.common.Messages.Graph_Label_ElevationGain;
   public static final String GRAPH_LABEL_GEARS                          = net.tourbook.common.Messages.Graph_Label_Gears;
   public static final String GRAPH_LABEL_GEO_COMPARE_UNIT               = net.tourbook.common.Messages.Graph_Label_Geo_Compare_Unit;
   public static final String GRAPH_LABEL_GRADIENT                       = net.tourbook.common.Messages.Graph_Label_Gradient;
   public static final String GRAPH_LABEL_GRADIENT_UNIT                  = net.tourbook.common.Messages.Graph_Label_Gradient_Unit;
   public static final String GRAPH_LABEL_HEARTBEAT                      = net.tourbook.common.Messages.Graph_Label_Heartbeat;
   public static final String GRAPH_LABEL_HEARTBEAT_AVG                  = net.tourbook.common.Messages.Graph_Label_Heartbeat_Avg;
   public static final String GRAPH_LABEL_HEARTBEAT_AVG_MAX              = net.tourbook.common.Messages.Graph_Label_Heartbeat_AvgMax;
   public static final String GRAPH_LABEL_HEARTBEAT_UNIT                 = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
   public static final String GRAPH_LABEL_HEART_RATE_VARIABILITY         = net.tourbook.common.Messages.Graph_Label_HeartRateVariability;
   public static final String GRAPH_LABEL_HEART_RATE_VARIABILITY_UNIT    = net.tourbook.common.Messages.Graph_Label_HeartRateVariability_Unit;
   public static final String GRAPH_LABEL_HR_ZONE                        = net.tourbook.common.Messages.Graph_Label_HrZone;
   public static final String GRAPH_LABEL_PACE                           = net.tourbook.common.Messages.Graph_Label_Pace;
   public static final String GRAPH_LABEL_PACE_INTERVAL                  = net.tourbook.common.Messages.Graph_Label_Pace_Interval;
   public static final String GRAPH_LABEL_PACE_SUMMARIZED                = net.tourbook.common.Messages.Graph_Label_Pace_Summarized;
   public static final String GRAPH_LABEL_POWER                          = net.tourbook.common.Messages.Graph_Label_Power;
   public static final String GRAPH_LABEL_POWER_UNIT                     = net.tourbook.common.Messages.Graph_Label_Power_Unit;
   public static final String GRAPH_LABEL_PREFIX_RUNNING_DYNAMICS        = net.tourbook.common.Messages.Graph_Label_Prefix_RunningDynamics;
   public static final String GRAPH_LABEL_PREFIX_SWIMMING                = net.tourbook.common.Messages.Graph_Label_Prefix_Swimming;
   public static final String GRAPH_LABEL_PREFIX_TRAINING                = net.tourbook.common.Messages.Graph_Label_Prefix_Training;
   public static final String GRAPH_LABEL_RUN_DYN_STANCE_TIME            = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime;
   public static final String GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCE    = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalance;
   public static final String GRAPH_LABEL_RUN_DYN_STEP_LENGTH            = net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength;
   public static final String GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION   = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation;
   public static final String GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO         = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio;
   public static final String GRAPH_LABEL_SPEED                          = net.tourbook.common.Messages.Graph_Label_Speed;
   public static final String GRAPH_LABEL_SPEED_INTERVAL                 = net.tourbook.common.Messages.Graph_Label_Speed_Interval;
   public static final String GRAPH_LABEL_SPEED_SUMMARIZED               = net.tourbook.common.Messages.Graph_Label_Speed_Summarized;
   public static final String GRAPH_LABEL_SWIM_STROKES                   = net.tourbook.common.Messages.Graph_Label_Swim_Strokes;
   public static final String GRAPH_LABEL_SWIM_SWOLF                     = net.tourbook.common.Messages.Graph_Label_Swim_Swolf;
   public static final String GRAPH_LABEL_TEMPERATURE                    = net.tourbook.common.Messages.Graph_Label_Temperature;
   public static final String GRAPH_LABEL_TIME                           = net.tourbook.common.Messages.Graph_Label_Time;
   public static final String GRAPH_LABEL_TIME_DURATION                  = net.tourbook.common.Messages.Graph_Label_TimeDuration;
   public static final String GRAPH_LABEL_TIME_OF_DAY                    = net.tourbook.common.Messages.Graph_Label_TimeOfDay;
   public static final String GRAPH_LABEL_TIME_START                     = net.tourbook.common.Messages.Graph_Label_TimeStart;
   public static final String GRAPH_LABEL_TIME_END                       = net.tourbook.common.Messages.Graph_Label_TimeEnd;
   public static final String GRAPH_LABEL_TOUR_COMPARE                   = net.tourbook.common.Messages.Graph_Label_Tour_Compare;
   public static final String GRAPH_LABEL_TOUR_COMPARE_REFERENCE_TOUR    = net.tourbook.common.Messages.Graph_Label_Tour_Compare_ReferenceTour;
   public static final String GRAPH_LABEL_TOUR_COMPARE_UNIT              = net.tourbook.common.Messages.Graph_Label_Tour_Compare_Unit;
   public static final String GRAPH_LABEL_TRAINING_EFFECT_AEROB          = net.tourbook.common.Messages.Graph_Label_Training_Effect_Aerob;
   public static final String GRAPH_LABEL_TRAINING_EFFECT_ANAEROB        = net.tourbook.common.Messages.Graph_Label_Training_Effect_Anaerob;
   public static final String GRAPH_LABEL_TRAINING_PERFORMANCE           = net.tourbook.common.Messages.Graph_Label_Training_Performance;

   public static final String MEASUREMENT_SYSTEM_TOOLTIP                 = net.tourbook.common.Messages.Measurement_System_Tooltip;

   public static final String PREF_SYSTEM_LABEL_DISTANCE                 = net.tourbook.common.Messages.Pref_System_Label_Distance;
   public static final String PREF_SYSTEM_LABEL_DISTANCE_INFO            = net.tourbook.common.Messages.Pref_System_Label_Distance_Info;
   public static final String PREF_SYSTEM_LABEL_ELEVATION                = net.tourbook.common.Messages.Pref_System_Label_Elevation;
   public static final String PREF_SYSTEM_LABEL_ELEVATION_INFO           = net.tourbook.common.Messages.Pref_System_Label_Elevation_Info;
   public static final String PREF_SYSTEM_LABEL_HEIGHT                   = net.tourbook.common.Messages.Pref_System_Label_Height;
   public static final String PREF_SYSTEM_LABEL_HEIGHT_INFO              = net.tourbook.common.Messages.Pref_System_Label_Height_Info;
   public static final String PREF_SYSTEM_LABEL_LENGTH                   = net.tourbook.common.Messages.Pref_System_Label_Length;
   public static final String PREF_SYSTEM_LABEL_LENGTH_INFO              = net.tourbook.common.Messages.Pref_System_Label_Length_Info;
   public static final String PREF_SYSTEM_LABEL_LENGTH_SMALL             = net.tourbook.common.Messages.Pref_System_Label_Length_Small;
   public static final String PREF_SYSTEM_LABEL_LENGTH_SMALL_INFO        = net.tourbook.common.Messages.Pref_System_Label_Length_Small_Info;
   public static final String PREF_SYSTEM_LABEL_PACE                     = net.tourbook.common.Messages.Pref_System_Label_Pace;
   public static final String PREF_SYSTEM_LABEL_PACE_INFO                = net.tourbook.common.Messages.Pref_System_Label_Pace_Info;
   public static final String PREF_SYSTEM_LABEL_PRESSURE_ATMOSPHERE      = net.tourbook.common.Messages.Pref_System_Label_Pressure_Atmosphere;
   public static final String PREF_SYSTEM_LABEL_PRESSURE_ATMOSPHERE_INFO = net.tourbook.common.Messages.Pref_System_Label_Pressure_Atmosphere_Info;
   public static final String PREF_SYSTEM_LABEL_SYSTEM                   = net.tourbook.common.Messages.Pref_System_Label_System;
   public static final String PREF_SYSTEM_LABEL_TEMPERATURE              = net.tourbook.common.Messages.Pref_System_Label_Temperature;
   public static final String PREF_SYSTEM_LABEL_USING_INFO               = net.tourbook.common.Messages.Pref_System_Label_UsingInfo;
   public static final String PREF_SYSTEM_LABEL_USING_INFO_TOOLTIP       = net.tourbook.common.Messages.Pref_System_Label_UsingInfo_Tooltip;
   public static final String PREF_SYSTEM_LABEL_WEIGHT                   = net.tourbook.common.Messages.Pref_System_Label_Weight;
   public static final String PREF_SYSTEM_LABEL_WEIGHT_INFO              = net.tourbook.common.Messages.Pref_System_Label_Weight_Info;

   public static final String WEATHER_CLOUDS_SUNNY                       = net.tourbook.common.Messages.Weather_Clouds_Sunny;

   public static final String SLIDEOUT_MAP_TRACK_COLORS_LABEL_TITLE                            = net.tourbook.common.Messages.Slideout_Map_TrackColors_Label_Title;
   public static final String SLIDEOUT_MAP_TRACK_COLORS_LABEL_VISIBLE_COLOR_PROFILES_TOOLTIP   = net.tourbook.common.Messages.Slideout_Map_TrackColors_Label_VisibleColorProfiles_Tooltip;
   public static final String SLIDEOUT_MAP_TRACK_COLORS_LABEL_VISIBLE_COLOR_PROFILES           = net.tourbook.common.Messages.Slideout_Map_TrackColors_Label_VisibleColorProfiles;

   public static final String COLUMN_FACTORY_CATEGORY_BODY                = net.tourbook.ui.Messages.ColumnFactory_Category_Body;
   public static final String COLUMN_FACTORY_CATEGORY_DATA                = net.tourbook.ui.Messages.ColumnFactory_Category_Data;
   public static final String COLUMN_FACTORY_CATEGORY_DEVICE              = net.tourbook.ui.Messages.ColumnFactory_Category_Device;
   public static final String COLUMN_FACTORY_CATEGORY_ELEVATION           = net.tourbook.ui.Messages.ColumnFactory_Category_Altitude;
   public static final String COLUMN_FACTORY_CATEGORY_MARKER              = net.tourbook.ui.Messages.ColumnFactory_Category_Marker;
   public static final String COLUMN_FACTORY_CATEGORY_MOTION              = net.tourbook.ui.Messages.ColumnFactory_Category_Motion;
   public static final String COLUMN_FACTORY_CATEGORY_PHOTO               = net.tourbook.ui.Messages.ColumnFactory_Category_Photo;
   public static final String COLUMN_FACTORY_CATEGORY_POWER               = net.tourbook.ui.Messages.ColumnFactory_Category_Power;
   public static final String COLUMN_FACTORY_CATEGORY_POWERTRAIN          = net.tourbook.ui.Messages.ColumnFactory_Category_Powertrain;
   public static final String COLUMN_FACTORY_CATEGORY_STATE               = net.tourbook.ui.Messages.ColumnFactory_Category_State;
   public static final String COLUMN_FACTORY_CATEGORY_TIME                = net.tourbook.ui.Messages.ColumnFactory_Category_Time;
   public static final String COLUMN_FACTORY_CATEGORY_TOUR                = net.tourbook.ui.Messages.ColumnFactory_Category_Tour;
   public static final String COLUMN_FACTORY_CATEGORY_TRAINING            = net.tourbook.ui.Messages.ColumnFactory_Category_Training;
   public static final String COLUMN_FACTORY_CATEGORY_WAYPOINT            = net.tourbook.ui.Messages.ColumnFactory_Category_Waypoint;
   public static final String COLUMN_FACTORY_CATEGORY_WEATHER             = net.tourbook.ui.Messages.ColumnFactory_Category_Weather;
   public static final String COLUMN_FACTORY_GEAR_REAR_SHIFT_COUNT_LABEL  = net.tourbook.ui.Messages.ColumnFactory_GearRearShiftCount_Label;
   public static final String COLUMN_FACTORY_GEAR_FRONT_SHIFT_COUNT_LABEL = net.tourbook.ui.Messages.ColumnFactory_GearFrontShiftCount_Label;
   public static final String COLUMN_FACTORY_MOTION_ALTIMETER             = net.tourbook.ui.Messages.ColumnFactory_Motion_Altimeter;
   public static final String COLUMN_FACTORY_MOTION_ALTIMETER_TOOLTIP     = net.tourbook.ui.Messages.ColumnFactory_Motion_Altimeter_Tooltip;
   public static final String COLUMN_FACTORY_POWER_AVG                    = net.tourbook.ui.Messages.ColumnFactory_Power_Avg_Tooltip;
   public static final String COLUMN_FACTORY_POWER_MAX                    = net.tourbook.ui.Messages.ColumnFactory_Power_Max_Tooltip;
   public static final String COLUMN_FACTORY_POWER_NORMALIZED             = net.tourbook.ui.Messages.ColumnFactory_Power_Normalized_Tooltip;
   public static final String COLUMN_FACTORY_POWER_TOTAL_WORK             = net.tourbook.ui.Messages.ColumnFactory_Power_TotalWork;
   public static final String COLUMN_FACTORY_POWER_FTP                    = net.tourbook.ui.Messages.ColumnFactory_Power_FTP_Label;
   public static final String COLUMN_FACTORY_POWERTRAIN_AVG_CADENCE       = net.tourbook.ui.Messages.ColumnFactory_avg_cadence_label;
   public static final String COLUMN_FACTORY_POWERTRAIN_AVG_CADENCE_UNIT  = net.tourbook.ui.Messages.ColumnFactory_avg_cadence;
   public static final String COLUMN_FACTORY_POWERTRAIN_GEAR_FRONT_SHIFT  = net.tourbook.ui.Messages.ColumnFactory_GearFrontShiftCount_Label;
   public static final String COLUMN_FACTORY_POWERTRAIN_GEAR_REAR_SHIFT   = net.tourbook.ui.Messages.ColumnFactory_GearRearShiftCount_Label;
   public static final String COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP       = net.tourbook.ui.Messages.ColumnFactory_TimeZoneDifference_Tooltip;

   public static final String TOUR_MARKER_COLUMN_IS_VISIBLE               = net.tourbook.ui.Messages.Tour_Marker_Column_IsVisible;
   public static final String TOUR_MARKER_COLUMN_IS_VISIBLE_TOOLTIP       = net.tourbook.ui.Messages.Tour_Marker_Column_IsVisible_Tooltip;

   public static final String TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME          = net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime;
   public static final String TOUR_TOOLTIP_LABEL_DISTANCE                 = net.tourbook.ui.Messages.Tour_Tooltip_Label_Distance;
   public static final String TOUR_TOOLTIP_LABEL_ELEVATION_UP             = net.tourbook.ui.Messages.Tour_Tooltip_Label_AltitudeUp;
   public static final String TOUR_TOOLTIP_LABEL_MOVING_TIME              = net.tourbook.ui.Messages.Tour_Tooltip_Label_MovingTime;
   public static final String TOUR_TOOLTIP_LABEL_NO_GEO_TOUR              = net.tourbook.ui.Messages.Tour_Tooltip_Label_NoGeoTour;
   public static final String TOUR_TOOLTIP_LABEL_RECORDED_TIME            = net.tourbook.ui.Messages.Tour_Tooltip_Label_RecordedTime;

   public static final String VALUE_UNIT_CALORIES                         = net.tourbook.ui.Messages.Value_Unit_Calories;
   public static final String VALUE_UNIT_CADENCE                          = net.tourbook.ui.Messages.Value_Unit_Cadence;
   public static final String VALUE_UNIT_CADENCE_SPM                      = net.tourbook.ui.Messages.Value_Unit_Cadence_Spm;
   public static final String VALUE_UNIT_K_CALORIES                       = net.tourbook.ui.Messages.Value_Unit_KCalories;
   public static final String VALUE_UNIT_PULSE                            = net.tourbook.ui.Messages.Value_Unit_Pulse;

   public static final String APP_UNIT_SECONDS_SMALL                      = net.tourbook.Messages.App_Unit_Seconds_Small;

   public static final String THEME_FONT_LOGGING_PREVIEW_TEXT             = de.byteholder.geoclipse.preferences.Messages.Theme_Font_Logging_PREVIEW_TEXT;
   public static final String THEME_FONT_LOGGING                          = de.byteholder.geoclipse.preferences.Messages.Theme_Font_Logging;

// SET_FORMATTING_ON
}
