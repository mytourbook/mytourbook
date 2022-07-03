/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.map3;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.map3.messages";                //$NON-NLS-1$

   public static String        Custom_Layer_Status;
   public static String        Custom_Layer_TerrainProfile;
   public static String        Custom_Layer_TourLegend;
   public static String        Custom_Layer_TourMarker;
   public static String        Custom_Layer_TourTrack;
   public static String        Custom_Layer_TrackSlider;
   public static String        Custom_Layer_ViewerController;

   public static String        Default_Layer_Atmosphere;
   public static String        Default_Layer_Compass;
   public static String        Default_Layer_NASABlueMarble;
   public static String        Default_Layer_PlaceNames;
   public static String        Default_Layer_PoliticalBoundaries;
   public static String        Default_Layer_ScaleBar;
   public static String        Default_Layer_Stars;
   public static String        Default_Layer_WMS_BingImagery;
   public static String        Default_Layer_WMS_EarthAtNight;
   public static String        Default_Layer_WMS_i_cubed_Landsat;
   public static String        Default_Layer_WMS_NASABlueMarble2004;
   public static String        Default_Layer_WMS_OpenStreetMap;
   public static String        Default_Layer_WMS_USGS_NAIP_PLUS;
   public static String        Default_Layer_WorldMap;

   public static String        Layer_Category_Info;
   public static String        Layer_Category_Map;
   public static String        Layer_Category_Tools;
   public static String        Layer_Category_Tour;

   public static String        Map3_Action_DirectionArrows;
   public static String        Map3_Action_OpenMap3PropertiesView;
   public static String        Map3_Action_OpenMap3StatisticsView;
   public static String        Map3_Action_SetTrackSliderPositionLeft;
   public static String        Map3_Action_SetTrackSliderPositionRight;
   public static String        Map3_Action_ShowLegend;
   public static String        Map3_Action_ShowMarker;
   public static String        Map3_Action_ShowOpenGLVersion_Tooltip;
   public static String        Map3_Action_ShowTrackSlider;
   public static String        Map3_Action_TrackColor;
   public static String        Map3_Action_TrackColors;

   public static String        Map3_Dialog_OpenGLVersion_Title;

   public static String        Map3Color_Dialog_Action_AddVertex_Tooltip;
   public static String        Map3Color_Dialog_Action_AddVertices_Tooltip;
   public static String        Map3Color_Dialog_Action_RemoveVertex_Tooltip;
   public static String        Map3Color_Dialog_Button_Apply;
   public static String        Map3Color_Dialog_Button_Label_GraphType;
   public static String        Map3Color_Dialog_Button_Label_GraphType_Tooltip;
   public static String        Map3Color_Dialog_Button_Label_ProfileName;
   public static String        Map3Color_Dialog_Button_Save;
   public static String        Map3Color_Dialog_Checkbox_EnableProntoColor;
   public static String        Map3Color_Dialog_Checkbox_EnableProntoColor_Tooltip;
   public static String        Map3Color_Dialog_Checkbox_OverwriteLegendValues;
   public static String        Map3Color_Dialog_Checkbox_OverwriteLegendValues_Tooltip;
   public static String        Map3Color_Dialog_Label_Values;
   public static String        Map3Color_Dialog_Message;
   public static String        Map3Color_Dialog_ProfileColor_Tooltip;
   public static String        Map3Color_Dialog_Radio_AbsoluteValues;
   public static String        Map3Color_Dialog_Radio_AbsoluteValues_Tooltip;
   public static String        Map3Color_Dialog_Radio_ProntoColor_Tooltip;
   public static String        Map3Color_Dialog_Radio_RelativeValues;
   public static String        Map3Color_Dialog_Radio_RelativeValues_Tooltip;
   public static String        Map3Color_Dialog_Spinner_ColorOpacity_Tooltip;
   public static String        Map3Color_Dialog_Spinner_ColorValue_Tooltip;
   public static String        Map3Color_Dialog_Title;

   public static String        Map3Layer_Viewer_Column_Layer;
   public static String        Map3Layer_Viewer_Column_Opacity;
   public static String        Map3Layer_Viewer_Column_Opacity_Tooltip;

   public static String        Map3SelectColor_Dialog_Action_AddColor_Tooltip;
   public static String        Map3SelectColor_Dialog_Action_EditAllColors;
   public static String        Map3SelectColor_Dialog_Action_EditSelectedColors;

   public static String        Map3Vertices_Dialog_Label_EndValue;
   public static String        Map3Vertices_Dialog_Label_NumberOfCreatedValues;
   public static String        Map3Vertices_Dialog_Label_StartValue;
   public static String        Map3Vertices_Dialog_Label_ValueDifference;
   public static String        Map3Vertices_Dialog_Title;

   public static String        Pref_Map3Color_Checkbox_ShowDropDownColorSelector;
   public static String        Pref_Map3Color_Checkbox_ShowDropDownColorSelector_Tooltip;
   public static String        Pref_Map3Color_Column_AbsoluteRelativeValue_Label;
   public static String        Pref_Map3Color_Column_AbsoluteRelativeValue_Tooltip;
   public static String        Pref_Map3Color_Column_Colors;
   public static String        Pref_Map3Color_Column_GraphImage;
   public static String        Pref_Map3Color_Column_Id_Header;
   public static String        Pref_Map3Color_Column_Id_Label;
   public static String        Pref_Map3Color_Column_Id_Tooltip;
   public static String        Pref_Map3Color_Column_Legend_Marker;
   public static String        Pref_Map3Color_Column_MaxValue_Header;
   public static String        Pref_Map3Color_Column_MaxValue_Label;
   public static String        Pref_Map3Color_Column_MinValue_Header;
   public static String        Pref_Map3Color_Column_MinValue_Label;
   public static String        Pref_Map3Color_Column_OverwriteLegendMinMax_Label;
   public static String        Pref_Map3Color_Column_OverwriteLegendMinMax_Label_Tooltip;
   public static String        Pref_Map3Color_Column_ProfileName;
   public static String        Pref_Map3Color_Column_ValueMarker_Absolute;
   public static String        Pref_Map3Color_Column_ValueMarker_Relative;
   public static String        Pref_Map3Color_Dialog_RemoveProfile_Message;
   public static String        Pref_Map3Color_Dialog_RemoveProfile_Title;
   public static String        Pref_Map3Color_Label_NumberOfColors;
   public static String        Pref_Map3Color_Label_NumberOfColors_Tooltip;
   public static String        Pref_Map3Color_Label_Title;

   public static String        Prop_Viewer_Label_AltitudeRange_Tooltip;

   public static String        Terrain_Follow_Cursor;
   public static String        Terrain_Follow_Eye;
   public static String        Terrain_Follow_None;
   public static String        Terrain_Follow_Object;
   public static String        Terrain_Follow_View;

   public static String        Terrain_Profile_Checkbox_KeepProportions;
   public static String        Terrain_Profile_Checkbox_ShowEye;
   public static String        Terrain_Profile_Checkbox_ZeroBased;
   public static String        Terrain_Profile_Dimension_Large;
   public static String        Terrain_Profile_Dimension_Medium;
   public static String        Terrain_Profile_Dimension_Small;
   public static String        Terrain_Profile_Label_Dimension;
   public static String        Terrain_Profile_Label_Follow;
   public static String        Terrain_Profile_Label_ProfileLength;

   public static String        TourTrack_Properties_Button_Default;
   public static String        TourTrack_Properties_Button_Default_Tooltip;
   public static String        TourTrack_Properties_Checkbox_AltitudeOffset;
   public static String        TourTrack_Properties_Checkbox_AltitudeOffset_Tooltip;
   public static String        TourTrack_Properties_Checkbox_AltitudeOffsetRandom;
   public static String        TourTrack_Properties_Checkbox_AltitudeOffsetRandom_Tooltip;
   public static String        TourTrack_Properties_Checkbox_DrawVerticals;
   public static String        TourTrack_Properties_Checkbox_DrawVerticals_Tooltip;
   public static String        TourTrack_Properties_Checkbox_ExtrudePath;
   public static String        TourTrack_Properties_Checkbox_ExtrudePath_Tooltip;
   public static String        TourTrack_Properties_Checkbox_IsFollowTerrain;
   public static String        TourTrack_Properties_Checkbox_IsFollowTerrain_Tooltip;
   public static String        TourTrack_Properties_Checkbox_ShowTrackPositions;
   public static String        TourTrack_Properties_Checkbox_ShowTrackPositions_Tooltip;
   public static String        TourTrack_Properties_Label_Altitude;
   public static String        TourTrack_Properties_Label_Altitude_Tooltip;
   public static String        TourTrack_Properties_Label_ConfigName;
   public static String        TourTrack_Properties_Label_ConfigName_Tooltip;
   public static String        TourTrack_Properties_Label_CurtainColor;
   public static String        TourTrack_Properties_Label_CurtainColor_Tooltip;
   public static String        TourTrack_Properties_Label_CurtainColorHovered;
   public static String        TourTrack_Properties_Label_CurtainColorHovered_Tooltip;
   public static String        TourTrack_Properties_Label_CurtainColorHovSel;
   public static String        TourTrack_Properties_Label_CurtainColorHovSel_Tooltip;
   public static String        TourTrack_Properties_Label_CurtainColorSelected;
   public static String        TourTrack_Properties_Label_CurtainColorSelected_Tooltip;
   public static String        TourTrack_Properties_Label_DirectionArrow;
   public static String        TourTrack_Properties_Label_DirectionArrow_Tooltip;
   public static String        TourTrack_Properties_Label_Name;
   public static String        TourTrack_Properties_Label_Name_Tooltip;
   public static String        TourTrack_Properties_Label_OutlineColorHovered;
   public static String        TourTrack_Properties_Label_OutlineColorHovered_Tooltip;
   public static String        TourTrack_Properties_Label_OutlineColor;
   public static String        TourTrack_Properties_Label_OutlineColor_Tooltip;
   public static String        TourTrack_Properties_Label_OutlineColorHovSel;
   public static String        TourTrack_Properties_Label_OutlineColorHovSel_Tooltip;
   public static String        TourTrack_Properties_Label_OutlineColorSelected;
   public static String        TourTrack_Properties_Label_OutlineColorSelected_Tooltip;
   public static String        TourTrack_Properties_Label_OutlineWidth;
   public static String        TourTrack_Properties_Label_OutlineWidth_Tooltip;
   public static String        TourTrack_Properties_Label_PathResolution;
   public static String        TourTrack_Properties_Label_PathResolution_Tooltip;
   public static String        TourTrack_Properties_Label_TrackColor;
   public static String        TourTrack_Properties_Label_TrackColor_Tooltip;
   public static String        TourTrack_Properties_Label_TrackPositionSize;
   public static String        TourTrack_Properties_Label_TrackPositionSize_Tooltip;
   public static String        TourTrack_Properties_Label_TrackPositionThreshold;
   public static String        TourTrack_Properties_Label_TrackPositionThreshold_Tooltip;
   public static String        TourTrack_Properties_Radio_AltitudeOffsetAbsolute;
   public static String        TourTrack_Properties_Radio_AltitudeOffsetAbsolute_Tooltip;
   public static String        TourTrack_Properties_Radio_AltitudeOffsetRelative;
   public static String        TourTrack_Properties_Radio_AltitudeOffsetRelative_Tooltip;

   public static String        Track_Config_Altitude_Mode_Absolute;
   public static String        Track_Config_Altitude_Mode_ClampToGround;
   public static String        Track_Config_Altitude_Mode_RelativeToGround;
   public static String        Track_Config_ConfigName_CloseBright;
   public static String        Track_Config_ConfigName_CloseDark;
   public static String        Track_Config_ConfigName_Default;
   public static String        Track_Config_ConfigName_Extrem;
   public static String        Track_Config_ConfigName_FarBright;
   public static String        Track_Config_ConfigName_FarDark;
   public static String        Track_Config_ConfigName_MiddleBright;
   public static String        Track_Config_ConfigName_MiddleDark;
   public static String        Track_Config_ConfigName_RelativeBright;
   public static String        Track_Config_ConfigName_RelativeDark;
   public static String        Track_Config_ConfigName_Unknown;
   public static String        Track_Config_Path_Resolution_High;
   public static String        Track_Config_Path_Resolution_Optimized;
   public static String        Track_Config_Path_Resolution_Viewport;
   public static String        Track_Config_TrackColorMode_Solid;
   public static String        Track_Config_TrackColorMode_Value;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
