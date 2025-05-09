/*******************************************************************************
 * Copyright (C) 2023, 2025 Wolfgang Schramm and Contributors
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

   //
   // de.byteholder.geoclipse.preferences.Messages
   //

   public static final String THEME_FONT_LOGGING_PREVIEW_TEXT              = de.byteholder.geoclipse.preferences.Messages.Theme_Font_Logging_PREVIEW_TEXT;
   public static final String THEME_FONT_LOGGING                           = de.byteholder.geoclipse.preferences.Messages.Theme_Font_Logging;


   //
   // net.tourbook.Messages
   //
   public static final String ACTION_PHOTOS_AND_TOURS_REMOVE_PHOTO         = net.tourbook.Messages.Action_PhotosAndTours_RemovePhoto;

   public static final String APP_ACTION_SAVE                              = net.tourbook.Messages.App_Action_Save;
   public static final String APP_BTN_BROWSE                               = net.tourbook.Messages.app_btn_browse;
   public static final String APP_TRUE                                     = net.tourbook.Messages.App__True;
   public static final String APP_UNIT_HHMMSS                              = net.tourbook.Messages.App_Unit_HHMMSS;
   public static final String APP_UNIT_SECONDS_SMALL                       = net.tourbook.Messages.App_Unit_Seconds_Small;

   public static final String DIALOG_EXPORT_CHK_OVERWRITEFILES             = net.tourbook.Messages.dialog_export_chk_overwriteFiles;
   public static final String DIALOG_EXPORT_CHK_OVERWRITEFILES_TOOLTIP     = net.tourbook.Messages.dialog_export_chk_overwriteFiles_tooltip;
   public static final String DIALOG_EXPORT_DIR_DIALOG_MESSAGE             = net.tourbook.Messages.dialog_export_dir_dialog_message;
   public static final String DIALOG_EXPORT_DIR_DIALOG_TEXT                = net.tourbook.Messages.dialog_export_dir_dialog_text;
   public static final String DIALOG_EXPORT_GROUP_EXPORTFILENAME           = net.tourbook.Messages.dialog_export_group_exportFileName;
   public static final String DIALOG_EXPORT_LABEL_EXPORTFILEPATH           = net.tourbook.Messages.dialog_export_label_exportFilePath;
   public static final String DIALOG_EXPORT_LABEL_FILENAME                 = net.tourbook.Messages.dialog_export_label_fileName;
   public static final String DIALOG_EXPORT_LABEL_FILEPATH                 = net.tourbook.Messages.dialog_export_label_filePath;
   public static final String DIALOG_EXPORT_MSG_FILEALREADYEXISTS          = net.tourbook.Messages.dialog_export_msg_fileAlreadyExists;
   public static final String DIALOG_EXPORT_MSG_FILENAMEISINVALID          = net.tourbook.Messages.dialog_export_msg_fileNameIsInvalid;
   public static final String DIALOG_EXPORT_MSG_PATHISNOTAVAILABLE         = net.tourbook.Messages.dialog_export_msg_pathIsNotAvailable;
   public static final String DIALOG_EXPORT_TXT_FILEPATH_TOOLTIP           = net.tourbook.Messages.dialog_export_txt_filePath_tooltip;

   public static final String LOG_TOUR_LOCATION_RETRIEVE_LOCATION_POINT    = net.tourbook.Messages.Log_TourLocation_Retrieve_LocationPoint;

   public static final String SLIDEOUT_ACTION_COLLAPSE_SLIDEOUT_TOOLTIP    = net.tourbook.Messages.Slideout_Action_CollapseSlideout_Tooltip;
   public static final String SLIDEOUT_ACTION_EXPAND_SLIDEOUT_TOOLTIP      = net.tourbook.Messages.Slideout_Action_ExpandSlideout_Tooltip;

   public static final String TOUR_SEGMENTER_LABEL_VERTICAL_SPEED_DESCENT  = net.tourbook.Messages.Tour_Segmenter_Label_VerticalSpeed_Descent;
   public static final String TOUR_SEGMENTER_LABEL_VERTICAL_SPEED_ASCENT   = net.tourbook.Messages.Tour_Segmenter_Label_VerticalSpeed_Ascent;
   public static final String TOUR_SEGMENTER_LABEL_VERTICAL_SPEED_FLAT     = net.tourbook.Messages.Tour_Segmenter_Label_VerticalSpeed_Flat;

   //
   // net.tourbook.common.Messages
   //

   public static final String APP_ACTION_ADD_WITH_CONFIRM                  = net.tourbook.common.Messages.App_Action_Add_WithConfirm;
   public static final String APP_ACTION_APPLY_AND_CLOSE                   = net.tourbook.common.Messages.App_Action_ApplyAndClose;
   public static final String APP_ACTION_CLOSE_TOOLTIP                     = net.tourbook.common.Messages.App_Action_Close_Tooltip;
   public static final String APP_ACTION_RESTORE_DEFAULT                   = net.tourbook.common.Messages.App_Action_RestoreDefault;

   public static final String APP_SIZE_LARGE_LABEL                         = net.tourbook.common.Messages.App_Size_Large_Label;
   public static final String APP_SIZE_LARGE_SHORTCUT                      = net.tourbook.common.Messages.App_Size_Large_Shortcut;
   public static final String APP_SIZE_LARGE_TEXT                          = net.tourbook.common.Messages.App_Size_Large_Text;
   public static final String APP_SIZE_MEDIUM_LABEL                        = net.tourbook.common.Messages.App_Size_Medium_Label;
   public static final String APP_SIZE_MEDIUM_SHORTCUT                     = net.tourbook.common.Messages.App_Size_Medium_Shortcut;
   public static final String APP_SIZE_MEDIUM_TEXT                         = net.tourbook.common.Messages.App_Size_Medium_Text;
   public static final String APP_SIZE_SMALL_LABEL                         = net.tourbook.common.Messages.App_Size_Small_Label;
   public static final String APP_SIZE_SMALL_SHORTCUT                      = net.tourbook.common.Messages.App_Size_Small_Shortcut;
   public static final String APP_SIZE_SMALL_TEXT                          = net.tourbook.common.Messages.App_Size_Small_Text;
   public static final String APP_SIZE_TINY_LABEL                          = net.tourbook.common.Messages.App_Size_Tiny_Label;
   public static final String APP_SIZE_TINY_SHORTCUT                       = net.tourbook.common.Messages.App_Size_Tiny_Shortcut;
   public static final String APP_SIZE_TINY_TEXT                           = net.tourbook.common.Messages.App_Size_Tiny_Text;

   public static final String APP_THEME_BACKGROUND_COLOR_DARK_TOOLTIP      = net.tourbook.common.Messages.App_Theme_BackgroundColor_Dark_Tooltip;
   public static final String APP_THEME_BACKGROUND_COLOR_LIGHT_TOOLTIP     = net.tourbook.common.Messages.App_Theme_BackgroundColor_Light_Tooltip;
   public static final String APP_THEME_BRIGHT_THEME                       = net.tourbook.common.Messages.App_Theme_BrightTheme;
   public static final String APP_THEME_BRIGHT_THEME_TOOLTIP               = net.tourbook.common.Messages.App_Theme_BrightTheme_Tooltip;
   public static final String APP_THEME_DARK_THEME                         = net.tourbook.common.Messages.App_Theme_DarkTheme;
   public static final String APP_THEME_DARK_THEME_TOOLTIP                 = net.tourbook.common.Messages.App_Theme_DarkTheme_Tooltip;
   public static final String APP_THEME_FOREGROUND_COLOR_DARK_TOOLTIP      = net.tourbook.common.Messages.App_Theme_ForegroundColor_Dark_Tooltip;
   public static final String APP_THEME_FOREGROUND_COLOR_LIGHT_TOOLTIP     = net.tourbook.common.Messages.App_Theme_ForegroundColor_Light_Tooltip;
   public static final String APP_THEME_VALUE_FOR_LIGHT_TOOLTIP            = net.tourbook.common.Messages.App_Theme_ValueFor_Light_Tooltip;
   public static final String APP_THEME_VALUE_FOR_DARK_TOOLTIP             = net.tourbook.common.Messages.App_Theme_ValueFor_Dark_Tooltip;

   public static final String GRAPH_LABEL_ALTIMETER                        = net.tourbook.common.Messages.Graph_Label_Altimeter;
   public static final String GRAPH_LABEL_ALTITUDE                         = net.tourbook.common.Messages.Graph_Label_Altitude;
   public static final String GRAPH_LABEL_CADENCE                          = net.tourbook.common.Messages.Graph_Label_Cadence;
   public static final String GRAPH_LABEL_CADENCE_UNIT                     = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
   public static final String GRAPH_LABEL_CADENCE_UNIT_RPM                 = net.tourbook.common.Messages.Graph_Label_Cadence_Unit;
   public static final String GRAPH_LABEL_CADENCE_UNIT_RPM_SPM             = net.tourbook.common.Messages.Graph_Label_Cadence_Unit_RpmSpm;
   public static final String GRAPH_LABEL_CADENCE_UNIT_SPM                 = net.tourbook.common.Messages.Graph_Label_Cadence_Unit_Spm;
   public static final String GRAPH_LABEL_DISTANCE                         = net.tourbook.common.Messages.Graph_Label_Distance;
   public static final String GRAPH_LABEL_ELEVATION_GAIN                   = net.tourbook.common.Messages.Graph_Label_ElevationGain;
   public static final String GRAPH_LABEL_GEARS                            = net.tourbook.common.Messages.Graph_Label_Gears;
   public static final String GRAPH_LABEL_GEO_COMPARE_UNIT                 = net.tourbook.common.Messages.Graph_Label_Geo_Compare_Unit;
   public static final String GRAPH_LABEL_GRADIENT                         = net.tourbook.common.Messages.Graph_Label_Gradient;
   public static final String GRAPH_LABEL_GRADIENT_UNIT                    = net.tourbook.common.Messages.Graph_Label_Gradient_Unit;
   public static final String GRAPH_LABEL_HEARTBEAT                        = net.tourbook.common.Messages.Graph_Label_Heartbeat;
   public static final String GRAPH_LABEL_HEARTBEAT_AVG                    = net.tourbook.common.Messages.Graph_Label_Heartbeat_Avg;
   public static final String GRAPH_LABEL_HEARTBEAT_AVG_MAX                = net.tourbook.common.Messages.Graph_Label_Heartbeat_AvgMax;
   public static final String GRAPH_LABEL_HEARTBEAT_UNIT                   = net.tourbook.common.Messages.Graph_Label_Heartbeat_Unit;
   public static final String GRAPH_LABEL_HEART_RATE_VARIABILITY           = net.tourbook.common.Messages.Graph_Label_HeartRateVariability;
   public static final String GRAPH_LABEL_HEART_RATE_VARIABILITY_UNIT      = net.tourbook.common.Messages.Graph_Label_HeartRateVariability_Unit;
   public static final String GRAPH_LABEL_HR_ZONE                          = net.tourbook.common.Messages.Graph_Label_HrZone;
   public static final String GRAPH_LABEL_PACE                             = net.tourbook.common.Messages.Graph_Label_Pace;
   public static final String GRAPH_LABEL_PACE_INTERVAL                    = net.tourbook.common.Messages.Graph_Label_Pace_Interval;
   public static final String GRAPH_LABEL_PACE_SUMMARIZED                  = net.tourbook.common.Messages.Graph_Label_Pace_Summarized;
   public static final String GRAPH_LABEL_POWER                            = net.tourbook.common.Messages.Graph_Label_Power;
   public static final String GRAPH_LABEL_POWER_UNIT                       = net.tourbook.common.Messages.Graph_Label_Power_Unit;
   public static final String GRAPH_LABEL_PREFIX_RUNNING_DYNAMICS          = net.tourbook.common.Messages.Graph_Label_Prefix_RunningDynamics;
   public static final String GRAPH_LABEL_PREFIX_SWIMMING                  = net.tourbook.common.Messages.Graph_Label_Prefix_Swimming;
   public static final String GRAPH_LABEL_PREFIX_TRAINING                  = net.tourbook.common.Messages.Graph_Label_Prefix_Training;
   public static final String GRAPH_LABEL_RUN_DYN_STANCE_TIME              = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTime;
   public static final String GRAPH_LABEL_RUN_DYN_STANCE_TIME_BALANCE      = net.tourbook.common.Messages.Graph_Label_RunDyn_StanceTimeBalance;
   public static final String GRAPH_LABEL_RUN_DYN_STEP_LENGTH              = net.tourbook.common.Messages.Graph_Label_RunDyn_StepLength;
   public static final String GRAPH_LABEL_RUN_DYN_VERTICAL_OSCILLATION     = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalOscillation;
   public static final String GRAPH_LABEL_RUN_DYN_VERTICAL_RATIO           = net.tourbook.common.Messages.Graph_Label_RunDyn_VerticalRatio;
   public static final String GRAPH_LABEL_SPEED                            = net.tourbook.common.Messages.Graph_Label_Speed;
   public static final String GRAPH_LABEL_SPEED_INTERVAL                   = net.tourbook.common.Messages.Graph_Label_Speed_Interval;
   public static final String GRAPH_LABEL_SPEED_SUMMARIZED                 = net.tourbook.common.Messages.Graph_Label_Speed_Summarized;
   public static final String GRAPH_LABEL_SWIM_STROKES                     = net.tourbook.common.Messages.Graph_Label_Swim_Strokes;
   public static final String GRAPH_LABEL_SWIM_SWOLF                       = net.tourbook.common.Messages.Graph_Label_Swim_Swolf;
   public static final String GRAPH_LABEL_TEMPERATURE                      = net.tourbook.common.Messages.Graph_Label_Temperature;
   public static final String GRAPH_LABEL_TIME                             = net.tourbook.common.Messages.Graph_Label_Time;
   public static final String GRAPH_LABEL_TIME_DURATION                    = net.tourbook.common.Messages.Graph_Label_TimeDuration;
   public static final String GRAPH_LABEL_TIME_OF_DAY                      = net.tourbook.common.Messages.Graph_Label_TimeOfDay;
   public static final String GRAPH_LABEL_TIME_START                       = net.tourbook.common.Messages.Graph_Label_TimeStart;
   public static final String GRAPH_LABEL_TIME_END                         = net.tourbook.common.Messages.Graph_Label_TimeEnd;
   public static final String GRAPH_LABEL_TOUR_COMPARE                     = net.tourbook.common.Messages.Graph_Label_Tour_Compare;
   public static final String GRAPH_LABEL_TOUR_COMPARE_REFERENCE_TOUR      = net.tourbook.common.Messages.Graph_Label_Tour_Compare_ReferenceTour;
   public static final String GRAPH_LABEL_TOUR_COMPARE_UNIT                = net.tourbook.common.Messages.Graph_Label_Tour_Compare_Unit;
   public static final String GRAPH_LABEL_TRAINING_EFFECT_AEROB            = net.tourbook.common.Messages.Graph_Label_Training_Effect_Aerob;
   public static final String GRAPH_LABEL_TRAINING_EFFECT_ANAEROB          = net.tourbook.common.Messages.Graph_Label_Training_Effect_Anaerob;
   public static final String GRAPH_LABEL_TRAINING_PERFORMANCE             = net.tourbook.common.Messages.Graph_Label_Training_Performance;

   public static final String MEASUREMENT_SYSTEM_TOOLTIP                   = net.tourbook.common.Messages.Measurement_System_Tooltip;

   public static final String PREF_SYSTEM_LABEL_DISTANCE                   = net.tourbook.common.Messages.Pref_System_Label_Distance;
   public static final String PREF_SYSTEM_LABEL_DISTANCE_INFO              = net.tourbook.common.Messages.Pref_System_Label_Distance_Info;
   public static final String PREF_SYSTEM_LABEL_ELEVATION                  = net.tourbook.common.Messages.Pref_System_Label_Elevation;
   public static final String PREF_SYSTEM_LABEL_ELEVATION_INFO             = net.tourbook.common.Messages.Pref_System_Label_Elevation_Info;
   public static final String PREF_SYSTEM_LABEL_HEIGHT                     = net.tourbook.common.Messages.Pref_System_Label_Height;
   public static final String PREF_SYSTEM_LABEL_HEIGHT_INFO                = net.tourbook.common.Messages.Pref_System_Label_Height_Info;
   public static final String PREF_SYSTEM_LABEL_LENGTH                     = net.tourbook.common.Messages.Pref_System_Label_Length;
   public static final String PREF_SYSTEM_LABEL_LENGTH_INFO                = net.tourbook.common.Messages.Pref_System_Label_Length_Info;
   public static final String PREF_SYSTEM_LABEL_LENGTH_SMALL               = net.tourbook.common.Messages.Pref_System_Label_Length_Small;
   public static final String PREF_SYSTEM_LABEL_LENGTH_SMALL_INFO          = net.tourbook.common.Messages.Pref_System_Label_Length_Small_Info;
   public static final String PREF_SYSTEM_LABEL_PACE                       = net.tourbook.common.Messages.Pref_System_Label_Pace;
   public static final String PREF_SYSTEM_LABEL_PACE_INFO                  = net.tourbook.common.Messages.Pref_System_Label_Pace_Info;
   public static final String PREF_SYSTEM_LABEL_PRESSURE_ATMOSPHERE        = net.tourbook.common.Messages.Pref_System_Label_Pressure_Atmosphere;
   public static final String PREF_SYSTEM_LABEL_PRESSURE_ATMOSPHERE_INFO   = net.tourbook.common.Messages.Pref_System_Label_Pressure_Atmosphere_Info;
   public static final String PREF_SYSTEM_LABEL_SYSTEM                     = net.tourbook.common.Messages.Pref_System_Label_System;
   public static final String PREF_SYSTEM_LABEL_TEMPERATURE                = net.tourbook.common.Messages.Pref_System_Label_Temperature;
   public static final String PREF_SYSTEM_LABEL_USING_INFO                 = net.tourbook.common.Messages.Pref_System_Label_UsingInfo;
   public static final String PREF_SYSTEM_LABEL_USING_INFO_TOOLTIP         = net.tourbook.common.Messages.Pref_System_Label_UsingInfo_Tooltip;
   public static final String PREF_SYSTEM_LABEL_WEIGHT                     = net.tourbook.common.Messages.Pref_System_Label_Weight;
   public static final String PREF_SYSTEM_LABEL_WEIGHT_INFO                = net.tourbook.common.Messages.Pref_System_Label_Weight_Info;

   public static final String SLIDEOUT_MAP_TRACK_COLORS_LABEL_TITLE                            = net.tourbook.common.Messages.Slideout_Map_TrackColors_Label_Title;
   public static final String SLIDEOUT_MAP_TRACK_COLORS_LABEL_VISIBLE_COLOR_PROFILES_TOOLTIP   = net.tourbook.common.Messages.Slideout_Map_TrackColors_Label_VisibleColorProfiles_Tooltip;
   public static final String SLIDEOUT_MAP_TRACK_COLORS_LABEL_VISIBLE_COLOR_PROFILES           = net.tourbook.common.Messages.Slideout_Map_TrackColors_Label_VisibleColorProfiles;

   public static final String WEATHER_AIRQUAlITY_0_IS_NOT_DEFINED          = net.tourbook.common.Messages.Weather_AirQuality_0_IsNotDefined;
   public static final String WEATHER_AIRQUAlITY_1_GOOD                    = net.tourbook.common.Messages.Weather_AirQuality_1_Good;
   public static final String WEATHER_AIRQUAlITY_2_FAIR                    = net.tourbook.common.Messages.Weather_AirQuality_2_Fair;
   public static final String WEATHER_AIRQUAlITY_3_MODERATE                = net.tourbook.common.Messages.Weather_AirQuality_3_Moderate;
   public static final String WEATHER_AIRQUAlITY_4_POOR                    = net.tourbook.common.Messages.Weather_AirQuality_4_Poor;
   public static final String WEATHER_AIRQUAlITY_5_VERYPOOR                = net.tourbook.common.Messages.Weather_AirQuality_5_VeryPoor;
   public static final String WEATHER_CLOUDS_SUNNY                         = net.tourbook.common.Messages.Weather_Clouds_Sunny;

   //
   // net.tourbook.map2.Messages
   //

   public static final String MAP_ACTION_EXTERNAL_APP_DOUBLE_CLICK_START   = net.tourbook.map2.Messages.Map_Action_ExternalApp_DoubleClickStart;
   public static final String MAP_ACTION_EXTERNAL_APP_OPEN_PHOTO_IMAGE     = net.tourbook.map2.Messages.Map_Action_ExternalApp_OpenPhotoImage;
   public static final String MAP_ACTION_EXTERNAL_APP_SETUP                = net.tourbook.map2.Messages.Map_Action_ExternalApp_Setup;
   public static final String MAP_ACTION_SHOW_TOUR_IN_MAP                  = net.tourbook.map2.Messages.map_action_show_tour_in_map;
   public static final String MAP_ACTION_TOUR_COLOR_ALTITUDE_TOOLTIP       = net.tourbook.map2.Messages.map_action_tour_color_altitude_tooltip;
   public static final String MAP_ACTION_TOUR_COLOR_GRADIENT_TOOLTIP       = net.tourbook.map2.Messages.map_action_tour_color_gradient_tooltip;
   public static final String MAP_ACTION_TOUR_COLOR_PACE_TOOLTIP           = net.tourbook.map2.Messages.map_action_tour_color_pace_tooltip;
   public static final String MAP_ACTION_TOUR_COLOR_PULSE_TOOLTIP          = net.tourbook.map2.Messages.map_action_tour_color_pulse_tooltip;
   public static final String MAP_ACTION_TOUR_COLOR_SPEED_TOOLTIP          = net.tourbook.map2.Messages.map_action_tour_color_speed_tooltip;
   public static final String MAP_ACTION_SYNCH_WITH_SLIDER                 = net.tourbook.map2.Messages.Map_Action_SynchWith_TourPosition;
   public static final String MAP_ACTION_SYNCH_WITH_SLIDER_CENTERED        = net.tourbook.map2.Messages.Map_Action_SynchWithSlider_Centered;
   public static final String TOUR_ACTION_SHOW_HR_ZONES_TOOLTIP            = net.tourbook.map2.Messages.Tour_Action_ShowHrZones_Tooltip;

   //
   // net.tourbook.map3.Messages
   //

   public static final String TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT_TOOLTIP = net.tourbook.map3.Messages.TourTrack_Properties_Button_Default_Tooltip;
   public static final String TOUR_TRACK_PROPERTIES_BUTTON_DEFAULT         = net.tourbook.map3.Messages.TourTrack_Properties_Button_Default;

   //
   // net.tourbook.ui.Messages
   //

   public static final String COLUMN_FACTORY_CATEGORY_BODY                 = net.tourbook.ui.Messages.ColumnFactory_Category_Body;
   public static final String COLUMN_FACTORY_CATEGORY_DATA                 = net.tourbook.ui.Messages.ColumnFactory_Category_Data;
   public static final String COLUMN_FACTORY_CATEGORY_DEVICE               = net.tourbook.ui.Messages.ColumnFactory_Category_Device;
   public static final String COLUMN_FACTORY_CATEGORY_ELEVATION            = net.tourbook.ui.Messages.ColumnFactory_Category_Altitude;
   public static final String COLUMN_FACTORY_CATEGORY_MARKER               = net.tourbook.ui.Messages.ColumnFactory_Category_Marker;
   public static final String COLUMN_FACTORY_CATEGORY_MOTION               = net.tourbook.ui.Messages.ColumnFactory_Category_Motion;
   public static final String COLUMN_FACTORY_CATEGORY_PHOTO                = net.tourbook.ui.Messages.ColumnFactory_Category_Photo;
   public static final String COLUMN_FACTORY_CATEGORY_POWER                = net.tourbook.ui.Messages.ColumnFactory_Category_Power;
   public static final String COLUMN_FACTORY_CATEGORY_POWERTRAIN           = net.tourbook.ui.Messages.ColumnFactory_Category_Powertrain;
   public static final String COLUMN_FACTORY_CATEGORY_STATE                = net.tourbook.ui.Messages.ColumnFactory_Category_State;
   public static final String COLUMN_FACTORY_CATEGORY_TIME                 = net.tourbook.ui.Messages.ColumnFactory_Category_Time;
   public static final String COLUMN_FACTORY_CATEGORY_TOUR                 = net.tourbook.ui.Messages.ColumnFactory_Category_Tour;
   public static final String COLUMN_FACTORY_CATEGORY_TRAINING             = net.tourbook.ui.Messages.ColumnFactory_Category_Training;
   public static final String COLUMN_FACTORY_CATEGORY_WAYPOINT             = net.tourbook.ui.Messages.ColumnFactory_Category_Waypoint;
   public static final String COLUMN_FACTORY_CATEGORY_WEATHER              = net.tourbook.ui.Messages.ColumnFactory_Category_Weather;
   public static final String COLUMN_FACTORY_GEAR_REAR_SHIFT_COUNT_LABEL   = net.tourbook.ui.Messages.ColumnFactory_GearRearShiftCount_Label;
   public static final String COLUMN_FACTORY_GEAR_FRONT_SHIFT_COUNT_LABEL  = net.tourbook.ui.Messages.ColumnFactory_GearFrontShiftCount_Label;
   public static final String COLUMN_FACTORY_MOTION_ALTIMETER              = net.tourbook.ui.Messages.ColumnFactory_Motion_Altimeter;
   public static final String COLUMN_FACTORY_MOTION_ALTIMETER_TOOLTIP      = net.tourbook.ui.Messages.ColumnFactory_Motion_Altimeter_Tooltip;
   public static final String COLUMN_FACTORY_POWER_AVG                     = net.tourbook.ui.Messages.ColumnFactory_Power_Avg_Tooltip;
   public static final String COLUMN_FACTORY_POWER_MAX                     = net.tourbook.ui.Messages.ColumnFactory_Power_Max_Tooltip;
   public static final String COLUMN_FACTORY_POWER_NORMALIZED              = net.tourbook.ui.Messages.ColumnFactory_Power_Normalized_Tooltip;
   public static final String COLUMN_FACTORY_POWER_TOTAL_WORK              = net.tourbook.ui.Messages.ColumnFactory_Power_TotalWork;
   public static final String COLUMN_FACTORY_POWER_FTP                     = net.tourbook.ui.Messages.ColumnFactory_Power_FTP_Label;
   public static final String COLUMN_FACTORY_POWERTRAIN_AVG_CADENCE        = net.tourbook.ui.Messages.ColumnFactory_avg_cadence_label;
   public static final String COLUMN_FACTORY_POWERTRAIN_AVG_CADENCE_UNIT   = net.tourbook.ui.Messages.ColumnFactory_avg_cadence;
   public static final String COLUMN_FACTORY_POWERTRAIN_GEAR_FRONT_SHIFT   = net.tourbook.ui.Messages.ColumnFactory_GearFrontShiftCount_Label;
   public static final String COLUMN_FACTORY_POWERTRAIN_GEAR_REAR_SHIFT    = net.tourbook.ui.Messages.ColumnFactory_GearRearShiftCount_Label;
   public static final String COLUMN_FACTORY_TIME_ZONE_DIFF_TOOLTIP        = net.tourbook.ui.Messages.ColumnFactory_TimeZoneDifference_Tooltip;

   public static final String TOUR_MARKER_COLUMN_IS_VISIBLE                = net.tourbook.ui.Messages.Tour_Marker_Column_IsVisible;
   public static final String TOUR_MARKER_COLUMN_IS_VISIBLE_TOOLTIP        = net.tourbook.ui.Messages.Tour_Marker_Column_IsVisible_Tooltip;

   public static final String TOUR_TOOLTIP_ACTION_EDIT_FORMAT_PREFERENCES  = net.tourbook.ui.Messages.Tour_Tooltip_Action_EditFormatPreferences;
   public static final String TOUR_TOOLTIP_FORMAT_DATE_WEEK_TIME           = net.tourbook.ui.Messages.Tour_Tooltip_Format_DateWeekTime;
   public static final String TOUR_TOOLTIP_LABEL_DISTANCE                  = net.tourbook.ui.Messages.Tour_Tooltip_Label_Distance;
   public static final String TOUR_TOOLTIP_LABEL_ELEVATION_UP              = net.tourbook.ui.Messages.Tour_Tooltip_Label_AltitudeUp;
   public static final String TOUR_TOOLTIP_LABEL_LOCATION_START            = net.tourbook.ui.Messages.Tour_Tooltip_Label_TourLocation_Start;
   public static final String TOUR_TOOLTIP_LABEL_LOCATION_END              = net.tourbook.ui.Messages.Tour_Tooltip_Label_TourLocation_End;
   public static final String TOUR_TOOLTIP_LABEL_MOVING_TIME               = net.tourbook.ui.Messages.Tour_Tooltip_Label_MovingTime;
   public static final String TOUR_TOOLTIP_LABEL_NO_GEO_TOUR               = net.tourbook.ui.Messages.Tour_Tooltip_Label_NoGeoTour;
   public static final String TOUR_TOOLTIP_LABEL_RECORDED_TIME             = net.tourbook.ui.Messages.Tour_Tooltip_Label_RecordedTime;

   public static final String VALUE_UNIT_CALORIES                          = net.tourbook.ui.Messages.Value_Unit_Calories;
   public static final String VALUE_UNIT_CADENCE                           = net.tourbook.ui.Messages.Value_Unit_Cadence;
   public static final String VALUE_UNIT_CADENCE_SPM                       = net.tourbook.ui.Messages.Value_Unit_Cadence_Spm;
   public static final String VALUE_UNIT_K_CALORIES                        = net.tourbook.ui.Messages.Value_Unit_KCalories;
   public static final String VALUE_UNIT_PULSE                             = net.tourbook.ui.Messages.Value_Unit_Pulse;

   //
   // net.tourbook.web.Messages
   //

   public static final String APP_WEB_LABEL_DEFAULT_FONT_SIZE              = net.tourbook.web.Messages.App_Web_Label_ContentFontSize;
   public static final String APP_WEB_LABEL_DEFAULT_FONT_SIZE_TOOLTIP      = net.tourbook.web.Messages.App_Web_Label_ContentFontSize_Tooltip;

   //
   // tourbook.search.nls.Messages
   //

   public static final String SEARCH_APP_ACTION_EDIT_MARKER                = tourbook.search.nls.Messages.Search_App_Action_EditMarker;
   public static final String SEARCH_APP_ACTION_EDIT_TOUR                  = tourbook.search.nls.Messages.Search_App_Action_EditTour;

// SET_FORMATTING_ON
}
