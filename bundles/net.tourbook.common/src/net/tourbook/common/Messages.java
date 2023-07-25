/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.common;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.common.messages";               //$NON-NLS-1$

   public static String        App_Action_Close_Tooltip;
   public static String        App_Action_New_WithConfirm;
   public static String        App_Action_Remove_NoConfirm;
   public static String        App_Action_Rename_WithConfirm;
   public static String        App_Action_RestoreDefault;
   public static String        App_Action_RestoreDefault_Tooltip;

   public static String        App_Error_NotSupportedValueFormatter;

   public static String        App_Theme_BackgroundColor_Light_Tooltip;
   public static String        App_Theme_BackgroundColor_Dark_Tooltip;
   public static String        App_Theme_BrightTheme;
   public static String        App_Theme_BrightTheme_Tooltip;
   public static String        App_Theme_DarkTheme;
   public static String        App_Theme_DarkTheme_Tooltip;
   public static String        App_Theme_ForegroundColor_Light_Tooltip;
   public static String        App_Theme_ForegroundColor_Dark_Tooltip;
   public static String        App_Theme_ValueFor_Light_Tooltip;
   public static String        App_Theme_ValueFor_Dark_Tooltip;

   public static String        Action_App_CustomizeColumnsAndProfiles;
   public static String        Action_App_RestartApp_Tooltip;
   public static String        Action_App_SizeAllColumnsToFit;

   public static String        Action_ColumnManager_Column_Info;
   public static String        Action_ColumnManager_ColumnActions_Info;
   public static String        Action_ColumnManager_FreezeCurrentColumn;
   public static String        Action_ColumnManager_FreezeCurrentColumn_Tooltip;
   public static String        Action_ColumnManager_HideCurrentColumn;
   public static String        Action_ColumnManager_Profile_Info;
   public static String        Action_ColumnManager_ShowAllColumns;
   public static String        Action_ColumnManager_ShowDefaultColumns;
   public static String        Action_ColumnManager_UnFreezeAllColumns;
   public static String        Action_ColumnManager_ValueFormatter_Category;
   public static String        Action_ColumnManager_ValueFormatter_Detail;
   public static String        Action_ColumnManager_ValueFormatter_Tour;

   public static String        Advanced_Menu_AnimationSymbol;

   public static String        Battery_Status_CHARGING;
   public static String        Battery_Status_CRITICAL;
   public static String        Battery_Status_GOOD;
   public static String        Battery_Status_LOW;
   public static String        Battery_Status_NEW;
   public static String        Battery_Status_OK;
   public static String        Battery_Status_UNKNOWN;

   public static String        Column_Annotation_Formatting;
   public static String        Column_Annotation_Sorting;

   public static String        Column_Profile_Dialog_ShowAllColumns_Message;
   public static String        Column_Profile_Dialog_ShowDefaultColumns_Message;
   public static String        Column_Profile_Dialog_Title;
   public static String        Column_Profile_Name_Default;

   public static String        ColumnModifyDialog_Button_default;
   public static String        ColumnModifyDialog_Button_Default2_Tooltip;
   public static String        ColumnModifyDialog_Button_DefaultWidth;
   public static String        ColumnModifyDialog_Button_DefaultWidth_Tooltip;
   public static String        ColumnModifyDialog_Button_deselect_all;
   public static String        ColumnModifyDialog_Button_move_down;
   public static String        ColumnModifyDialog_Button_move_up;
   public static String        ColumnModifyDialog_Button_select_all;
   public static String        ColumnModifyDialog_Button_ShowCategoryColumn_Tooltip;
   public static String        ColumnModifyDialog_Button_Sort;
   public static String        ColumnModifyDialog_Button_Sort_Tooltip;
   public static String        ColumnModifyDialog_Checkbox_ShowFormatAnnotations;
   public static String        ColumnModifyDialog_Checkbox_ShowSortingAnnotations;
   public static String        ColumnModifyDialog_Column_Category;
   public static String        ColumnModifyDialog_Column_FormatCategory;
   public static String        ColumnModifyDialog_Column_FormatTour;
   public static String        ColumnModifyDialog_Column_HeaderText;
   public static String        ColumnModifyDialog_column_column;
   public static String        ColumnModifyDialog_column_unit;
   public static String        ColumnModifyDialog_column_width;
   public static String        ColumnModifyDialog_Dialog_Profile_Title;
   public static String        ColumnModifyDialog_Dialog_ProfileNew_Message;
   public static String        ColumnModifyDialog_Dialog_ProfileRename_Message;
   public static String        ColumnModifyDialog_Dialog_title;
   public static String        ColumnModifyDialog_Label_Column;
   public static String        ColumnModifyDialog_Label_Hints;
   public static String        ColumnModifyDialog_Label_Profile;

   public static String        color_chooser_hexagon;
   public static String        color_chooser_rgb;
   public static String        color_chooser_red;
   public static String        color_chooser_green;
   public static String        color_chooser_blue;
   public static String        color_chooser_hue;
   public static String        color_chooser_saturation;
   public static String        color_chooser_brightness;

   public static String        Color_Chooser_Action_AddColorsFromProfile;
   public static String        Color_Chooser_Action_ResetImages;
   public static String        Color_Chooser_Action_SetColorsFromProfile;
   public static String        Color_Chooser_Dialog_SetColorsFromProfile_Message;
   public static String        Color_Chooser_Dialog_SetColorsFromProfile_Title;
   public static String        Color_Chooser_Hexagon_Tooltip;
   public static String        Color_Chooser_HoveredColor_Tooltip;
   public static String        Color_Chooser_Label_ColorCustomColors_Tooltip;
   public static String        Color_Chooser_Label_HoveredColor;
   public static String        Color_Chooser_Label_SelectedColor;
   public static String        Color_Chooser_Link_CustomColors;
   public static String        Color_Chooser_Link_CustomColors_Tooltip;
   public static String        Color_Chooser_SelectedColor_Tooltip;

   public static String        Direction_ArrowDesign_MiddleFin;
   public static String        Direction_ArrowDesign_Wings;
   public static String        Direction_ArrowDesign_Wings_MiddleFin;
   public static String        Direction_ArrowDesign_Wings_OuterFins;
   public static String        Direction_ArrowLayout_OuterFins;

   public static String        Font_Editor_Label_FontSize;

   public static String        Format_DateTime_yyyymmdd_hhmmss;

   public static String        Format_TimeDuration_hh;
   public static String        Format_TimeDuration_hhmm;
   public static String        Format_TimeDuration_hhmmss;

   public static String        Graph_Label_Altimeter;
   public static String        Graph_Label_Altitude;
   public static String        Graph_Label_Athlete_Body_Fat;
   public static String        Graph_Label_Athlete_Body_Weight;
   public static String        Graph_Label_Cadence;
   public static String        Graph_Label_Cadence_Unit;
   public static String        Graph_Label_Cadence_Unit_Spm;
   public static String        Graph_Label_Cadence_Unit_RpmSpm;
   public static String        Graph_Label_Distance;
   public static String        Graph_Label_ElevationGain;
   public static String        Graph_Label_Gears;
   public static String        Graph_Label_Gradient;
   public static String        Graph_Label_Gradient_Unit;
   public static String        Graph_Label_Heartbeat;
   public static String        Graph_Label_Heartbeat_Avg;
   public static String        Graph_Label_Heartbeat_AvgMax;
   public static String        Graph_Label_Heartbeat_Unit;
   public static String        Graph_Label_HeartRateVariability;
   public static String        Graph_Label_HeartRateVariability_Unit;
   public static String        Graph_Label_History;
   public static String        Graph_Label_HrZone;
   public static String        Graph_Label_Pace;
   public static String        Graph_Label_Pace_Interval;
   public static String        Graph_Label_Pace_Summarized;
   public static String        Graph_Label_Power;
   public static String        Graph_Label_Power_Unit;
   public static String        Graph_Label_Sensor;
   public static String        Graph_Label_Speed;
   public static String        Graph_Label_Speed_Interval;
   public static String        Graph_Label_Speed_Summarized;
   public static String        Graph_Label_Temperature;
   public static String        Graph_Label_Time;
   public static String        Graph_Label_TimeDuration;
   public static String        Graph_Label_TimeEnd;
   public static String        Graph_Label_TimeOfDay;
   public static String        Graph_Label_TimeStart;
   public static String        Graph_Label_Tour;
   public static String        Graph_Label_Tour_Compare;
   public static String        Graph_Label_Tour_Compare_ReferenceTour;
   public static String        Graph_Label_Tour_Compare_Unit;
   public static String        Graph_Label_Geo_Compare_Unit;
   public static String        Graph_Label_Prefix_AthleteData;
   public static String        Graph_Label_Prefix_RunningDynamics;
   public static String        Graph_Label_Prefix_Swimming;
   public static String        Graph_Label_Prefix_Training;
   public static String        Graph_Label_RunDyn_StanceTime;
   public static String        Graph_Label_RunDyn_StanceTimeBalance;
   public static String        Graph_Label_RunDyn_StepLength;
   public static String        Graph_Label_RunDyn_VerticalRatio;
   public static String        Graph_Label_RunDyn_VerticalOscillation;
   public static String        Graph_Label_Swim_Strokes;
   public static String        Graph_Label_Swim_Swolf;
   public static String        Graph_Label_Training_Effect_Aerob;
   public static String        Graph_Label_Training_Effect_Anaerob;
   public static String        Graph_Label_Training_Performance;

   public static String        Graph_Pref_color_gradient_bright;
   public static String        Graph_Pref_color_gradient_dark;
   public static String        Graph_Pref_color_mapping;
   public static String        Graph_Pref_color_statistic_distance;
   public static String        Graph_Pref_color_statistic_time;
   public static String        Graph_Pref_ColorLine_Theme_Dark;
   public static String        Graph_Pref_ColorLine_Theme_Light;
   public static String        Graph_Pref_ColorText_Theme_Dark;
   public static String        Graph_Pref_ColorText_Theme_Light;

   public static String        Map3_Color_ProfileName_Default;
   public static String        Map3_Color_ProfileName_New;

   public static String        Map3_PropertyTooltip_Action_Close_Tooltip;
   public static String        Map3_PropertyTooltip_Action_MoveToDefaultLocation_Tooltip;

   public static String        Measurement_System_Dialog_Title;
   public static String        Measurement_System_Dialog_Label_SelectSystem;
   public static String        Measurement_System_Profile_Imperial;
   public static String        Measurement_System_Profile_Metric;
   public static String        Measurement_System_Profile_Nautic;
   public static String        Measurement_System_Profile_Other1;
   public static String        Measurement_System_Profile_Other2;
   public static String        Measurement_System_Profile_Other3;
   public static String        Measurement_System_Tooltip;

   public static String        Period_Format_Space;
   public static String        Period_Format_Comma;
   public static String        Period_Format_CommaAndAnd;
   public static String        Period_Format_CommaSpaceAnd;
   public static String        Period_Format_CommaSpace;
   public static String        Period_Format_SpaceAndSpace;
   public static String        Period_Format_Year;
   public static String        Period_Format_Years;
   public static String        Period_Format_Year_Short;
   public static String        Period_Format_Month;
   public static String        Period_Format_Months;
   public static String        Period_Format_Month_Short;
   public static String        Period_Format_Week;
   public static String        Period_Format_Weeks;
   public static String        Period_Format_Week_Short;
   public static String        Period_Format_Day;
   public static String        Period_Format_Days;
   public static String        Period_Format_Day_Short;
   public static String        Period_Format_Hour;
   public static String        Period_Format_Hours;
   public static String        Period_Format_Hour_Short;
   public static String        Period_Format_Minute;
   public static String        Period_Format_Minutes;
   public static String        Period_Format_Minute_Short;
   public static String        Period_Format_Second;
   public static String        Period_Format_Seconds;
   public static String        Period_Format_Second_Short;
   public static String        Period_Format_Millisecond;
   public static String        Period_Format_Milliseconds;
   public static String        Period_Format_Millisecond_Short;

   public static String        Pref_System_Label_Distance;
   public static String        Pref_System_Label_Distance_Info;
   public static String        Pref_System_Label_Elevation;
   public static String        Pref_System_Label_Elevation_Info;
   public static String        Pref_System_Label_Height;
   public static String        Pref_System_Label_Height_Info;
   public static String        Pref_System_Label_Length;
   public static String        Pref_System_Label_Length_Info;
   public static String        Pref_System_Label_Length_Small;
   public static String        Pref_System_Label_Length_Small_Info;
   public static String        Pref_System_Label_Pace;
   public static String        Pref_System_Label_Pace_Info;
   public static String        Pref_System_Label_Pressure_Atmosphere;
   public static String        Pref_System_Label_Pressure_Atmosphere_Info;
   public static String        Pref_System_Label_System;
   public static String        Pref_System_Label_Temperature;
   public static String        Pref_System_Label_UsingInfo;
   public static String        Pref_System_Label_UsingInfo_Tooltip;
   public static String        Pref_System_Label_Weight;
   public static String        Pref_System_Label_Weight_Info;
   public static String        Pref_System_Option_BodyWeight_Kilogram;
   public static String        Pref_System_Option_BodyWeight_Pound;
   public static String        Pref_System_Option_Distance_Kilometer;
   public static String        Pref_System_Option_Distance_Mile;
   public static String        Pref_System_Option_Distance_NauticMile;
   public static String        Pref_System_Option_Elevation_Foot;
   public static String        Pref_System_Option_Elevation_Meter;
   public static String        Pref_System_Option_Height_Inch;
   public static String        Pref_System_Option_Height_Meter;
   public static String        Pref_System_Option_Length_Meter;
   public static String        Pref_System_Option_Length_Yard;
   public static String        Pref_System_Option_Pace_MinutesPerKilometer;
   public static String        Pref_System_Option_Pace_MinutesPerMile;
   public static String        Pref_System_Option_Pressure_Atmosphere_InchOfMercury;
   public static String        Pref_System_Option_Pressure_Atmosphere_Millibar;
   public static String        Pref_System_Option_SmallLength_Inch;
   public static String        Pref_System_Option_SmallLength_Millimeter;
   public static String        Pref_System_Option_Temperature_Celsius;
   public static String        Pref_System_Option_Temperature_Fahrenheit;

   public static String        Weather_AirQuality_Good;
   public static String        Weather_AirQuality_Fair;
   public static String        Weather_AirQuality_IsNotDefined;
   public static String        Weather_AirQuality_Moderate;
   public static String        Weather_AirQuality_Poor;
   public static String        Weather_AirQuality_VeryPoor;

   public static String        Weather_Clouds_Clouds;
   public static String        Weather_Clouds_Cloudy;
   public static String        Weather_Clouds_IsNotDefined;
   public static String        Weather_Clouds_Lightning;
   public static String        Weather_Clouds_Drizzle;
   public static String        Weather_Clouds_Rain;
   public static String        Weather_Clouds_ScatteredShowers;
   public static String        Weather_Clouds_SevereWeatherAlert;
   public static String        Weather_Clouds_Snow;
   public static String        Weather_Clouds_Sunny;

   public static String        Weather_WindDirection_N;
   public static String        Weather_WindDirection_NNE;
   public static String        Weather_WindDirection_NE;
   public static String        Weather_WindDirection_ENE;
   public static String        Weather_WindDirection_E;
   public static String        Weather_WindDirection_ESE;
   public static String        Weather_WindDirection_SE;
   public static String        Weather_WindDirection_SSE;
   public static String        Weather_WindDirection_S;
   public static String        Weather_WindDirection_SSW;
   public static String        Weather_WindDirection_SW;
   public static String        Weather_WindDirection_WSW;
   public static String        Weather_WindDirection_W;
   public static String        Weather_WindDirection_WNW;
   public static String        Weather_WindDirection_NW;
   public static String        Weather_WindDirection_NNW;

   public static String        Weather_WindSpeed_Bft00;
   public static String        Weather_WindSpeed_Bft00_Short;
   public static String        Weather_WindSpeed_Bft01;
   public static String        Weather_WindSpeed_Bft01_Short;
   public static String        Weather_WindSpeed_Bft02;
   public static String        Weather_WindSpeed_Bft02_Short;
   public static String        Weather_WindSpeed_Bft03;
   public static String        Weather_WindSpeed_Bft03_Short;
   public static String        Weather_WindSpeed_Bft04;
   public static String        Weather_WindSpeed_Bft04_Short;
   public static String        Weather_WindSpeed_Bft05;
   public static String        Weather_WindSpeed_Bft05_Short;
   public static String        Weather_WindSpeed_Bft06;
   public static String        Weather_WindSpeed_Bft06_Short;
   public static String        Weather_WindSpeed_Bft07;
   public static String        Weather_WindSpeed_Bft07_Short;
   public static String        Weather_WindSpeed_Bft08;
   public static String        Weather_WindSpeed_Bft08_Short;
   public static String        Weather_WindSpeed_Bft09;
   public static String        Weather_WindSpeed_Bft09_Short;
   public static String        Weather_WindSpeed_Bft10;
   public static String        Weather_WindSpeed_Bft10_Short;
   public static String        Weather_WindSpeed_Bft11;
   public static String        Weather_WindSpeed_Bft11_Short;
   public static String        Weather_WindSpeed_Bft12;
   public static String        Weather_WindSpeed_Bft12_Short;

   public static String        legend_color_dim_color;
   public static String        legend_color_keep_color;
   public static String        legend_color_lighten_color;

   public static String        Legend_UnitLayout_BrightBackground_NoShadow;
   public static String        Legend_UnitLayout_BrightBackground_WithShadow;
   public static String        Legend_UnitLayout_DarkBackground_NoShadow;
   public static String        Legend_UnitLayout_DarkBackground_WithShadow;

   public static String        rgv_vertex_class_cast_exception;

   public static String        Slideout_Dialog_Action_DragSlideout_ToolTip;
   public static String        Slideout_Dialog_Action_KeepSlideoutOpen_Tooltip;
   public static String        Slideout_Dialog_Action_PinSlideoutLocation_Tooltip;

   public static String        Slideout_Map_TrackColors_Label_Title;
   public static String        Slideout_Map_TrackColors_Label_VisibleColorProfiles;
   public static String        Slideout_Map_TrackColors_Label_VisibleColorProfiles_Tooltip;

   public static String        Swim_Stroke_Backstroke;
   public static String        Swim_Stroke_Breaststroke;
   public static String        Swim_Stroke_Butterfly;
   public static String        Swim_Stroke_Drill;
   public static String        Swim_Stroke_Freestyle;
   public static String        Swim_Stroke_IndividualMedley;
   public static String        Swim_Stroke_Invalid;
   public static String        Swim_Stroke_Mixed;

   public static String        Time_Tools_DST_North;
   public static String        Time_Tools_DST_South;

   public static String        Value_Formatter_Number_1_0;
   public static String        Value_Formatter_Number_1_1;
   public static String        Value_Formatter_Number_1_2;
   public static String        Value_Formatter_Number_1_3;
   public static String        Value_Formatter_Pace_MM_SS;
   public static String        Value_Formatter_Time_HH;
   public static String        Value_Formatter_Time_HH_MM;
   public static String        Value_Formatter_Time_HH_MM_SS;
   public static String        Value_Formatter_Time_SSS;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
