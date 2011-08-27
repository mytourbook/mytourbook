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
package net.tourbook;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.messages";							//$NON-NLS-1$

	public static String		Image__ArrowDown;
	public static String		Image__ArrowUp;
	public static String		Image__ZoomOut;
	public static String		Image__ZoomIn;
	public static String		Image__ZoomCentered;
	public static String		Image__Synced;

	public static String		Calendar_View_Action_Back;
	public static String		Calendar_View_Action_Back_Tooltip;
	public static String		Calendar_View_Action_BlackHighlightText;
	public static String		Calendar_View_Action_DisplayTours_1ByDay;
	public static String		Calendar_View_Action_DisplayTours_All;
	public static String		Calendar_View_Action_DisplayTours_ByDay;
	public static String		Calendar_View_Action_DistancePace;
	public static String		Calendar_View_Action_Forward;
	public static String		Calendar_View_Action_Forward_Tooltip;
	public static String		Calendar_View_Action_GotoToday;
	public static String		Calendar_View_Action_LineInfo;
	public static String		Calendar_View_Action_LinkWithOtherViews;
	public static String		Calendar_View_Action_LogicalNavigation;
	public static String		Calendar_View_Action_PhysicalNavigation;
	public static String		Calendar_View_Action_ResizeTours;
	public static String		Calendar_View_Action_ShowDescriptionTitle;
	public static String		Calendar_View_Action_ShowDayNumberInTinyView;
	public static String		Calendar_View_Action_ShowDistanceSpeed;
	public static String		Calendar_View_Action_ShowDistanceTime;
	public static String		Calendar_View_Action_ShowNothing;
	public static String		Calendar_View_Action_ShowTitleDescription;
	public static String		Calendar_View_Action_TextColor;
	public static String		Calendar_View_Action_TimeDistance;
	public static String		Calendar_View_Action_TimePace;
	public static String		Calendar_View_Action_TimeSpeed;
	public static String		Calendar_View_Action_ZoomIn;
	public static String		Calendar_View_Action_ZoomIn_Tooltip;
	public static String		Calendar_View_Action_ZoomOut;
	public static String		Calendar_View_Action_ZoomOut_Tooltip;

	public static String		Calendar_View_Combo_Month_Tooltip;
	public static String		Calendar_View_Combo_Year_Tooltip;

	public static String		Calendar_View_Format_DistancePace;
	public static String		Calendar_View_Format_DistanceSpeed;
	public static String		Calendar_View_Format_DistanceTime;
	public static String		Calendar_View_Format_TimeDistance;
	public static String		Calendar_View_Format_TimePace;
	public static String		Calendar_View_Format_TimeSpeed;
	public static String		Calendar_View_Format_Distance;
	public static String		Calendar_View_Format_Time;
	public static String		Calendar_View_Format_Speed;
	public static String		Calendar_View_Format_Pace;

	public static String		dialog_export_btn_export;
	public static String		dialog_export_chk_camouflageSpeed;
	public static String		dialog_export_chk_camouflageSpeed_tooltip;
	public static String		dialog_export_chk_camouflageSpeedInput_tooltip;
	public static String		dialog_export_chk_exportNotes;
	public static String		dialog_export_chk_exportNotes_tooltip;
	public static String		dialog_export_chk_exportMarkers;
	public static String		dialog_export_chk_exportMarkers_tooltip;
	public static String		dialog_export_chk_mergeAllTours;
	public static String		dialog_export_chk_mergeAllTours_tooltip;
	public static String		dialog_export_chk_overwriteFiles;
	public static String		dialog_export_chk_overwriteFiles_tooltip;
	public static String		dialog_export_chk_tourRangeDisabled;
	public static String		dialog_export_chk_tourRangeWithDistance;
	public static String		dialog_export_chk_tourRangeWithoutDistance;
	public static String		dialog_export_dialog_message;
	public static String		dialog_export_dialog_title;
	public static String		dialog_export_dir_dialog_message;
	public static String		dialog_export_dir_dialog_text;
	public static String		dialog_export_error_camouflageSpeedIsInvalid;
	public static String		dialog_export_file_dialog_text;
	public static String		dialog_export_group_exportFileName;
	public static String		dialog_export_group_options;
	public static String		dialog_export_label_DefaultFileName;
	public static String		dialog_export_label_exportFilePath;
	public static String		dialog_export_label_fileName;
	public static String		dialog_export_label_filePath;
	public static String		dialog_export_lbl_exportFilePath;
	public static String		dialog_export_msg_fileAlreadyExists;
	public static String		dialog_export_msg_fileNameIsInvalid;
	public static String		dialog_export_msg_pathIsNotAvailable;
	public static String		dialog_export_shell_text;
	public static String		dialog_export_txt_filePath_tooltip;

	public static String		Dialog_HRZone_Button_AddZone;
	public static String		Dialog_HRZone_Button_EditHrZones;
	public static String		Dialog_HRZone_Button_RemoveZone;
	public static String		Dialog_HRZone_Button_SortZone;
	public static String		Dialog_HRZone_Button_SortZone_Tooltip;
	public static String		Dialog_HRZone_DialogMessage;
	public static String		Dialog_HRZone_DialogTitle;
	public static String		Dialog_HRZone_Label_Header_Color;
	public static String		Dialog_HRZone_Label_Header_Pulse;
	public static String		Dialog_HRZone_Label_Header_Zone;
	public static String		Dialog_HRZone_Label_Header_ZoneShortcut;
	public static String		Dialog_HRZone_Label_Trash_Tooltip;

	public static String		Dialog_DoubleClickAction_InvalidAction_Message;
	public static String		Dialog_DoubleClickAction_InvalidAction_Title;
	public static String		Dialog_DoubleClickAction_NoAction_Message;
	public static String		Dialog_DoubleClickAction_NoAction_Title;

	public static String		Dialog_ExtractTour_DlgArea_Message;
	public static String		Dialog_ExtractTour_DlgArea_Title;
	public static String		Dialog_ExtractTour_Label_DeviceName;
	public static String		Dialog_ExtractTour_Label_SplitMethod;

	public static String		Dialog_PersonManager_PersonIsNotAvailable_Message;
	public static String		Dialog_PersonManager_PersonIsNotAvailable_Title;

	public static String		dialog_is_tour_editor_modified_message;
	public static String		dialog_is_tour_editor_modified_title;

	public static String		dialog_quick_edit_dialog_area_title;
	public static String		dialog_quick_edit_dialog_title;

	public static String		Action_Tag_Add_AutoOpen;
	public static String		Action_Tag_Add_AutoOpen_ModifiedTags;
	public static String		Action_Tag_Add_AutoOpen_Title;
	public static String		Action_Tag_Add_RecentTags;
	public static String		Action_Tag_AutoOpenCancel;
	public static String		Action_Tag_AutoOpenOK;

	public static String		Action_TourType_ModifyTourTypeFilter;

	public static String		Adjust_Altitude_CreateDummyAltitudeData_Message;
	public static String		Adjust_Altitude_CreateDummyAltitudeData_Title;
	public static String		Adjust_Altitude_Group_GeoPosition;
	public static String		Adjust_Altitude_Label_GeoPosition_Slices;
	public static String		Adjust_Altitude_Type_HorizontalGeoPosition;

	public static String		App_Action_About;
	public static String		App_Action_Cancel;
	public static String		App_Action_edit_tour;
	public static String		App_Action_ExtractTour;
	public static String		App_Action_JoinTours;
	public static String		App_Action_Menu_file;
	public static String		App_Action_Menu_help;
	public static String		App_Action_Menu_tools;
	public static String		App_Action_Menu_view;
	public static String		App_Action_open_perspective;
	public static String		App_Action_open_preferences;
	public static String		App_Action_open_tour_segmenter;
	public static String		App_Action_open_tour_segmenter_tooltip;
	public static String		App_Action_OpenOtherViews;

	public static String		action_export_tour;
	public static String		action_print_tour;
	public static String		action_tag_add;
	public static String		action_tag_dlg_rename_message;
	public static String		action_tag_dlg_rename_title;
	public static String		action_tag_open_tagging_structure;
	public static String		action_tag_remove;
	public static String		action_tag_remove_all;
	public static String		action_tag_rename_tag;
	public static String		action_tag_rename_tag_category;
	public static String		action_tag_set_all_confirm_message;
	public static String		action_tag_set_all_confirm_title;
	public static String		action_tag_set_all_tag_structures;
	public static String		action_tag_set_tag_expand_type;
	public static String		action_tagcategory_dlg_rename_message;
	public static String		action_tagcategory_dlg_rename_title;
	public static String		action_tagView_flat_hierarchical;
	public static String		action_tagView_flat_layout;
	public static String		action_tour_editor_delete_time_slices_keep_time;
	public static String		action_tour_editor_delete_time_slices_remove_time;
	public static String		action_tour_editor_delete_tour_marker;
	public static String		action_tourbook_select_year_month_tours;
	public static String		action_tourbook_year_sub;
	public static String		action_tourbook_year_sub_month;
	public static String		action_tourbook_year_sub_week;
	public static String		action_tourCatalog_open_compare_wizard;
	public static String		action_tourType_modify_tourTypes;

	public static String		adjust_altitude_action_create_spline_point;
	public static String		adjust_altitude_btn_reset_altitude;
	public static String		adjust_altitude_btn_reset_altitude_and_points;
	public static String		adjust_altitude_btn_reset_altitude_and_points_tooltip;
	public static String		adjust_altitude_btn_reset_altitude_tooltip;
	public static String		adjust_altitude_btn_save_modified_tour;
	public static String		adjust_altitude_btn_srtm_remove_all_points;
	public static String		adjust_altitude_btn_srtm_remove_all_points_tooltip;
	public static String		adjust_altitude_btn_update_altitude;
	public static String		adjust_altitude_btn_update_altitude_tooltip;
	public static String		adjust_altitude_btn_update_modified_tour;
	public static String		adjust_altitude_dlg_dialog_message;
	public static String		adjust_altitude_dlg_dialog_title;
	public static String		adjust_altitude_dlg_shell_title;
	public static String		adjust_altitude_label_adjustment_type;
	public static String		adjust_altitude_type_adjust_end;
	public static String		adjust_altitude_type_adjust_height;
	public static String		adjust_altitude_type_adjust_whole_tour;
	public static String		adjust_altitude_type_srtm;
	public static String		adjust_altitude_type_srtm_spline;
	public static String		adjust_altitude_type_start_and_end;

	public static String		app_action_button_down;
	public static String		app_action_button_up;
	public static String		app_action_collapse_all_tooltip;
	public static String		app_action_collapse_others_tooltip;
	public static String		app_action_edit_adjust_altitude;
	public static String		app_action_edit_rows_tooltip;
	public static String		app_action_edit_tour_marker;
	public static String		app_action_expand_selection_tooltip;
	public static String		app_action_expand_type_flat;
	public static String		app_action_expand_type_year_day;
	public static String		app_action_expand_type_year_month_day;
	public static String		app_action_merge_tour;
	public static String		app_action_open_tour;
	public static String		app_action_quick_edit;
	public static String		app_action_read_edit_tooltip;
	public static String		app_action_save;
	public static String		app_action_save_tour_tooltip;
	public static String		app_action_undo_modifications;
	public static String		app_action_undo_modifications_tooltip;
	public static String		app_action_update;

	public static String		app_btn_browse;

	public static String		app_db_consistencyCheck_checkFailed;
	public static String		app_db_consistencyCheck_checkIsOK;
	public static String		app_db_consistencyCheck_dlgTitle;

	public static String		app_dlg_confirmFileOverwrite_message;
	public static String		app_dlg_confirmFileOverwrite_title;

	public static String		app_error_title;

	public static String		app_unit_minutes;
	public static String		app_unit_seconds;

	public static String		App_Action_set_tour_type;
	public static String		App_Action_Save;
	public static String		App_Action_SetPerson;
	public static String		App_Action_SplitTour;

	public static String		App_measurement_imperial;
	public static String		App_measurement_metric;
	public static String		App_measurement_tooltip;

	public static String		App_People_item_all;
	public static String		App_People_tooltip;

	public static String		App_Default_PersonFirstName;

	public static String		App_Dialog_FirstStartup_Message;
	public static String		App_Dialog_FirstStartup_Title;
	public static String		App_Dialog_FirstStartupSystem_Label_Info;
	public static String		App_Dialog_FirstStartupSystem_Label_System;
	public static String		App_Dialog_FirstStartupSystem_Radio_Imperial;
	public static String		App_Dialog_FirstStartupSystem_Radio_Metric;
	public static String		App_Dialog_FirstStartupSystem_Title;
	public static String		App_Dialog_FirstStartupTip_Message;
	public static String		App_Dialog_FirstStartupTip_Title;

	public static String		App_Label_H_MM;
	public static String		App_Label_ISO8601;
	public static String		App_Label_max;

	public static String		App_Link_RestoreDefaultValues;

	public static String		App_Title;

	public static String		App_Tour_type_item_all_types;
	public static String		App_Tour_type_item_not_defined;
	public static String		App_TourType_ToolTip;
	public static String		App_TourType_ToolTipTitle;

	public static String		App_Unit_Minute;
	public static String		App_Unit_Seconds_Small;

	public static String		App_Window_Title;

	public static String		Compare_Result_Action_check_selected_tours;
	public static String		Compare_Result_Action_remove_save_result;
	public static String		Compare_Result_Action_save_checked_tours;
	public static String		Compare_Result_Action_save_checked_tours_tooltip;
	public static String		Compare_Result_Action_uncheck_selected_tours;
	public static String		Compare_Result_Column_diff;
	public static String		Compare_Result_Column_diff_label;
	public static String		Compare_Result_Column_diff_tooltip;
	public static String		Compare_Result_Column_kmh_db_label;
	public static String		Compare_Result_Column_kmh_db_tooltip;
	public static String		Compare_Result_Column_kmh_label;
	public static String		Compare_Result_Column_kmh_moved_label;
	public static String		Compare_Result_Column_kmh_moved_tooltip;
	public static String		Compare_Result_Column_kmh_tooltip;
	public static String		Compare_Result_Column_tour;

	public static String		Compute_BreakTime_Button_ComputeAllTours;
	public static String		Compute_BreakTime_Button_ComputeAllTours_Tooltip;
	public static String		Compute_BreakTime_Button_RestoreDefaultValues;
	public static String		Compute_BreakTime_Button_RestoreDefaultValues_Tooltip;
	public static String		Compute_BreakTime_Button_SetDefaultValues;
	public static String		Compute_BreakTime_Button_SetDefaultValues_Tooltip;
	public static String		Compute_BreakTime_Dialog_ComputeForAllTours_Message;
	public static String		Compute_BreakTime_Dialog_ComputeForAllTours_Title;
	public static String		Compute_BreakTime_ForAllTour_Job_Result;
	public static String		Compute_BreakTime_ForAllTour_Job_SubTask;
	public static String		Compute_BreakTime_Group_BreakTime;
	public static String		Compute_BreakTime_Label_ComputeBreakTimeBy;
	public static String		Compute_BreakTime_Label_Description;
	public static String		Compute_BreakTime_Label_Description_ComputeByAvgSliceSpeed;
	public static String		Compute_BreakTime_Label_Description_ComputeByAvgSpeed;
	public static String		Compute_BreakTime_Label_Description_ComputeBySliceSpeed;
	public static String		Compute_BreakTime_Label_Description_ComputeByTime;
	public static String		Compute_BreakTime_Label_Hints;
	public static String		Compute_BreakTime_Label_MinimumAvgSpeed;
	public static String		Compute_BreakTime_Label_MinimumDistance;
	public static String		Compute_BreakTime_Label_MinimumSliceSpeed;
	public static String		Compute_BreakTime_Label_MinimumSliceTime;
	public static String		Compute_BreakTime_Label_MinimumTime;
	public static String		Compute_BreakTime_Label_SliceDiffBreak;
	public static String		Compute_BreakTime_Label_SliceDiffBreak_Tooltip;
	public static String		Compute_BreakTime_Label_Title;
	public static String		Compute_BreakTime_Label_TourBreakTime;
	public static String		Compute_BreakTime_Method_SpeedByAverage;
	public static String		Compute_BreakTime_Method_SpeedByAverageAndSlice;
	public static String		Compute_BreakTime_Method_SpeedBySlice;
	public static String		Compute_BreakTime_Method_TimeDistance;

	public static String		Compute_HrZone_Group;
	public static String		Compute_HrZone_Link;
	public static String		Compute_HrZones_Dialog_ComputeAllTours_Title;
	public static String		Compute_HrZones_Dialog_ComputeAllTours_Title_Message;
	public static String		Compute_HrZones_Job_ComputeAllTours_Result;
	public static String		Compute_HrZones_Job_ComputeAllTours_SubTask;

	public static String		Compute_Smoothing_Button_ForAllTours;
	public static String		Compute_Smoothing_Button_ForAllTours_Tooltip;

	public static String		Compute_TourValueSpeed_Title;

	public static String		Compute_Values_Group_Smoothing;
	public static String		Compute_Values_Label_Info;

	public static String		Conconi_Chart_Chk_LogScaling;
	public static String		Conconi_Chart_Chk_LogScaling_Tooltip;
	public static String		Conconi_Chart_DeflactionPoint;
	public static String		Conconi_Chart_InvalidData;
	public static String		Conconi_Chart_Label_ScalingFactor;
	public static String		Conconi_Chart_Label_Tour;
	public static String		Conconi_Chart_Label_Tour_Tooltip;

	public static String		compute_tourValueElevation_button_computeValues;
	public static String		compute_tourValueElevation_button_computeValues_tooltip;
	public static String		compute_tourValueElevation_dlg_computeValues_message;
	public static String		compute_tourValueElevation_dlg_computeValues_title;
	public static String		compute_tourValueElevation_group_computeTourAltitude;
	public static String		compute_tourValueElevation_label_description;
	public static String		compute_tourValueElevation_label_description_Hints;
	public static String		compute_tourValueElevation_label_minAltiDifference;
	public static String		compute_tourValueElevation_resultText;
	public static String		compute_tourValueElevation_subTaskText;

	public static String		compute_tourValueSpeed_label_description;
	public static String		compute_tourValueSpeed_label_description_Hints;
	public static String		compute_tourValueSpeed_label_speedTimeSlice;

	public static String		Database_Confirm_update;
	public static String		Database_Confirm_update_title;
	public static String		Database_Monitor_CreateDatabase;

	public static String		Database_Monitor_db_service_task;
	public static String		Database_Monitor_persistent_service_task;
	public static String		Database_Monitor_SetupPooledConnection;

	public static String		DataImport_Error_file_does_not_exist_msg;
	public static String		DataImport_Error_file_does_not_exist_title;
	public static String		DataImport_Error_invalid_data_format;
	public static String		DataImport_ConfirmImport_title;

	public static String		Db_Field_TourData_Description;
	public static String		Db_Field_TourData_EndPlace;
	public static String		Db_Field_TourData_StartPlace;
	public static String		Db_Field_TourData_Title;
	public static String		Db_Field_TourData_TourImportFilePath;
	public static String		Db_Field_TourData_Weather;

	public static String		DeviceManager_Selection_device_is_not_selected;

	public static String		Dialog_JoinTours_Checkbox_CreateTourMarker;
	public static String		Dialog_JoinTours_Checkbox_IncludeDescription;
	public static String		Dialog_JoinTours_Checkbox_IncludeMarkerWaypoints;
	public static String		Dialog_JoinTours_ComboText_ConcatenateTime;
	public static String		Dialog_JoinTours_ComboText_KeepTime;
	public static String		Dialog_JoinTours_ComboText_MarkerTourTime;
	public static String		Dialog_JoinTours_ComboText_TourTileCustom;
	public static String		Dialog_JoinTours_ComboText_TourTitleFromTour;
	public static String		Dialog_JoinTours_ComboText_TourTypeCustom;
	public static String		Dialog_JoinTours_ComboText_TourTypeFromTour;
	public static String		Dialog_JoinTours_ComboText_TourTypePrevious;
	public static String		Dialog_JoinTours_DlgArea_Message;
	public static String		Dialog_JoinTours_DlgArea_Title;
	public static String		Dialog_JoinTours_InvalidData_Distance;
	public static String		Dialog_JoinTours_InvalidData_DlgMessage;
	public static String		Dialog_JoinTours_InvalidData_DlgTitle;
	public static String		Dialog_JoinTours_InvalidData_Latitude;
	public static String		Dialog_JoinTours_InvalidData_Power;
	public static String		Dialog_JoinTours_InvalidData;
	public static String		Dialog_JoinTours_InvalidData_InvalidTours;
	public static String		Dialog_JoinTours_InvalidData_RequiredDataSeries;
	public static String		Dialog_JoinTours_InvalidData_Speed;
	public static String		Dialog_JoinTours_InvalidData_Temperature;
	public static String		Dialog_JoinTours_InvalidData_Time;
	public static String		Dialog_JoinTours_Label_DefaultTitle;
	public static String		Dialog_JoinTours_Label_DeviceName;
	public static String		Dialog_JoinTours_Label_JoinMethod;
	public static String		Dialog_JoinTours_Label_Tour;
	public static String		Dialog_JoinTours_Label_TourDate;
	public static String		Dialog_JoinTours_Label_TourMarkerText;
	public static String		Dialog_JoinTours_Label_TourTime;
	public static String		Dialog_JoinTours_Label_TourType;
	public static String		Dialog_JoinTours_Link_TourType;

	public static String		Dialog_SplitTour_Checkbox_IncludeDescription;
	public static String		Dialog_SplitTour_Checkbox_KeepTime;
	public static String		Dialog_SplitTour_ComboText_KeepSlices;
	public static String		Dialog_SplitTour_ComboText_RemoveSlices;
	public static String		Dialog_SplitTour_ComboText_TourTileCustom;
	public static String		Dialog_SplitTour_ComboText_TourTitleFromFirstMarker;
	public static String		Dialog_SplitTour_ComboText_TourTitleFromTour;
	public static String		Dialog_SplitTour_ComboText_TourTypeCustom;
	public static String		Dialog_SplitTour_ComboText_TourTypeFromTour;
	public static String		Dialog_SplitTour_ComboText_TourTypePrevious;
	public static String		Dialog_SplitTour_DlgArea_Message;
	public static String		Dialog_SplitTour_DlgArea_Title;
	public static String		Dialog_SplitTour_Label_DefaultTitle;
	public static String		Dialog_SplitTour_Label_DeviceName;
	public static String		Dialog_SplitTour_Label_Person;
	public static String		Dialog_SplitTour_Label_Person_Tooltip;
	public static String		Dialog_SplitTour_Label_SplitMethod;
	public static String		Dialog_SplitTour_Label_TourStartDateTime;
	public static String		Dialog_SplitTour_Label_TourTitle;
	public static String		Dialog_SplitTour_Label_TourTitle_Tooltip;

	public static String		Dlg_AdjustAltitude_Group_options;
	public static String		Dlg_AdjustAltitude_Label_end_altitude;
	public static String		Dlg_AdjustAltitude_Label_end_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Label_max_altitude;
	public static String		Dlg_AdjustAltitude_Label_max_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Label_original_values;
	public static String		Dlg_AdjustAltitude_Label_start_altitude;
	public static String		Dlg_AdjustAltitude_Label_start_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Radio_keep_bottom_altitude;
	public static String		Dlg_AdjustAltitude_Radio_keep_bottom_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Radio_keep_start_altitude;
	public static String		Dlg_AdjustAltitude_Radio_keep_start_altitude_tooltip;

	public static String		Dlg_TourMarker_Button_delete;
	public static String		Dlg_TourMarker_Button_delete_tooltip;
	public static String		Dlg_TourMarker_Button_reset_offset;
	public static String		Dlg_TourMarker_Button_reset_offset_tooltip;
	public static String		Dlg_TourMarker_Button_undo;
	public static String		Dlg_TourMarker_Button_undo_tooltip;
	public static String		Dlg_TourMarker_Dlg_Message;
	public static String		Dlg_TourMarker_Dlg_title;
	public static String		Dlg_TourMarker_Label_horizontal_offset;
	public static String		Dlg_TourMarker_Label_marker_name;
	public static String		Dlg_TourMarker_Label_markers;
	public static String		Dlg_TourMarker_Label_position;
	public static String		Dlg_TourMarker_Label_vertical_offset;
	public static String		Dlg_TourMarker_MsgBox_delete_marker_message;
	public static String		Dlg_TourMarker_MsgBox_delete_marker_title;

	public static String		External_Link_TourChartSmoothing;

	public static String		Format_hhmm;
	public static String		Format_hhmmss;
	public static String		Format_rawdata_file_yyyy_mm_dd;
	public static String		Format_yyyymmdd_hhmmss;

	public static String		Graph_Label_Altimeter;
	public static String		Graph_Label_Altitude;
	public static String		Graph_Label_Cadence;
	public static String		Graph_Label_Cadence_unit;
	public static String		Graph_Label_Gradiend_unit;
	public static String		Graph_Label_Gradient;
	public static String		Graph_Label_Heartbeat;
	public static String		Graph_Label_Heartbeat_unit;
	public static String		Graph_Label_Pace;
	public static String		Graph_Label_Power;
	public static String		Graph_Label_Power_unit;
	public static String		Graph_Label_ShowHrZoneBackground;
	public static String		Graph_Label_ShowHrZoneBackground_Tooltip;
	public static String		Graph_Label_Speed;
	public static String		Graph_Label_Temperature;
	public static String		Graph_Label_Tour_Compare;
	public static String		Graph_Label_Tour_Compare_unit;

	public static String		Graph_Pref_color_gradient_bright;
	public static String		Graph_Pref_color_gradient_dark;
	public static String		Graph_Pref_color_line;
	public static String		Graph_Pref_color_mapping;
	public static String		Graph_Pref_color_statistic_distance;
	public static String		Graph_Pref_color_statistic_time;

	public static String		Graph_Pref_ColorText;

	public static String		HR_Zone_01_060_Moderate;
	public static String		HR_Zone_01_060_Moderate_Shortcut;
	public static String		HR_Zone_01_070_FatBurning;
	public static String		HR_Zone_01_070_FatBurning_Shortcut;
	public static String		HR_Zone_01_080_Aerobic;
	public static String		HR_Zone_01_080_Aerobic_Shortcut;
	public static String		HR_Zone_01_090_Anaerobic;
	public static String		HR_Zone_01_090_Anaerobic_Shortcut;
	public static String		HR_Zone_01_100_Maximum;
	public static String		HR_Zone_01_100_Maximum_Shortcut;
	public static String		HR_Zone_02_065_KB;
	public static String		HR_Zone_02_065_KB_Shortcut;
	public static String		HR_Zone_02_075_GA1;
	public static String		HR_Zone_02_075_GA1_Shortcut;
	public static String		HR_Zone_02_085_GA2;
	public static String		HR_Zone_02_085_GA2_Shortcut;
	public static String		HR_Zone_02_095_EB;
	public static String		HR_Zone_02_095_EB_Shortcut;
	public static String		HR_Zone_02_095_SB;
	public static String		HR_Zone_02_095_SB_Shortcut;
	public static String		HR_Zone_Template_01_Moderate60Max100;
	public static String		HR_Zone_Template_02_GA1GA2;
	public static String		HR_Zone_Template_Select;

	public static String		HRMax_Label;

	public static String		HRMaxFormula_Name_HRmax_191_5;
	public static String		HRMaxFormula_Name_HRmax_205_8;
	public static String		HRMaxFormula_Name_HRmax_206_9;
	public static String		HRMaxFormula_Name_HRmax_220_age;
	public static String		HRMaxFormula_Name_Manual;

	public static String		Image___Empty16;
	public static String		Image__App_Cancel;
	public static String		Image__App_OK;
	public static String		Image__App_Trash;
	public static String		Image__assignMergedTour;
	public static String		Image__assignMergedTour_disabled;
	public static String		Image__chart_analyzer;
	public static String		Image__collapse_all;
	public static String		Image__database;
	public static String		Image__database_other_person;
	public static String		Image__delete;
	public static String		Image__delete_disabled;
	public static String		Image__edit_adjust_altitude;
	public static String		Image__edit_adjust_altitude_disabled;
	public static String		Image__edit_row;
	public static String		Image__edit_tour;
	public static String		Image__edit_tour_disabled;
	public static String		Image__edit_tour_marker;
	public static String		Image__edit_tour_marker_disabled;
	public static String		Image__edit_tour_marker_new;
	public static String		Image__edit_tour_marker_new_disabled;
	public static String		Image__expand_all;
	public static String		Image__graph_altimeter;
	public static String		Image__graph_altitude;
	public static String		Image__graph_cadence;
	public static String		Image__graph_gradient;
	public static String		Image__graph_heartbeat;
	public static String		Image__graph_pace;
	public static String		Image__graph_power;
	public static String		Image__graph_speed;
	public static String		Image__graph_temperature;
	public static String		Image__graph_altimeter_disabled;
	public static String		Image__graph_altitude_disabled;
	public static String		Image__graph_cadence_disabled;
	public static String		Image__graph_gradient_disabled;
	public static String		Image__graph_heartbeat_disabled;
	public static String		Image__graph_pace_disabled;
	public static String		Image__graph_power_disabled;
	public static String		Image__graph_speed_disabled;
	public static String		Image__graph_temperature_disabled;
	public static String		Image__graph_tour_compare;
	public static String		Image__graph_tour_compare_disabled;
	public static String		Image__icon_placeholder;
	public static String		Image__layout_flat;
	public static String		Image__layout_hierarchical;
	public static String		Image__link;
	public static String		Image__MyTourbook16;
	public static String		Image__open_folder_add;
	public static String		Image__options;
	public static String		Image__PulseZones;
	public static String		Image__PulseZones_Disabled;
	public static String		Image__quick_edit;
	public static String		Image__RawData_Import;
	public static String		Image__RawData_Transfer;
	public static String		Image__RawData_TransferDirect;
	public static String		Image__refresh;
	public static String		Image__remove_all;
	public static String		Image__remove_all_disabled;
	public static String		Image__save;
	public static String		Image__save_disabled;
	public static String		Image__save_tour;
	public static String		Image__save_tour_disabled;
	public static String		Image__show_distance_on_x_axis;
	public static String		Image__show_distance_on_x_axis_disabled;
	public static String		Image__show_time_on_x_axis;
	public static String		Image__spline_point;
	public static String		Image__synch_graph_byScale;
	public static String		Image__synch_graph_byScale_disabled;
	public static String		Image__synch_graph_bySize;
	public static String		Image__synch_graph_bySize_disabled;
	public static String		Image__synch_statistics;
	public static String		Image__synch_statistics_Disabled;
	public static String		Image__tag;
	public static String		Image__tag_category;
	public static String		Image__tag_root;
	public static String		Image__tour_map_ref_tour_new;
	public static String		Image__tour_new;
	public static String		Image__tour_new_disabled;
	public static String		Image__tour_options;
	public static String		Image__tour_segmenter;
	public static String		Image__tour_viewer;
	public static String		Image__undo_edit;
	public static String		Image__undo_edit_disabled;
	public static String		Image__undo_tour_type_filter;
	public static String		Image__undo_tour_type_filter_system;
	public static String		Image__view_compare_wizard;
	public static String		Image__weather_sunny;
	public static String		Image__weather_cloudy;
	public static String		Image__weather_clouds;
	public static String		Image__weather_lightning;
	public static String		Image__weather_rain;
	public static String		Image__weather_snow;
	public static String		Image__Weather_ScatteredShowers;
	public static String		Image__Weather_Severe;
	public static String		Image__ZoomFitGraph;

	public static String		image__merge_tours;

	public static String		import_data_action_adjust_imported_year;
	public static String		import_data_action_assignment_is_not_available;
	public static String		import_data_action_assignMergedTour;
	public static String		import_data_action_assignMergedTour_default;
	public static String		import_data_action_clear_view;
	public static String		import_data_action_clear_view_tooltip;
	public static String		import_data_action_createTourIdWithTime;
	public static String		import_data_action_disable_hac4_checksum_validation;
	public static String		import_data_action_merge_tracks;
	public static String		import_data_action_reimport_tour;
	public static String		import_data_action_reimport_tour_OnlyTimeSlices;
	public static String		import_data_action_save_tour_for_person;
	public static String		import_data_action_save_tour_with_person;
	public static String		import_data_action_save_tours_for_person;
	public static String		import_data_dlg_reimport_invalid_file_message;
	public static String		import_data_dlg_reimport_message;
	public static String		import_data_dlg_reimport_title;
	public static String		import_data_dlg_save_tour_msg;
	public static String		import_data_dlg_save_tour_title;
	public static String		import_data_importTours_subTask;
	public static String		import_data_importTours_task;
	public static String		import_data_updateDataFromDatabase_subTask;
	public static String		import_data_updateDataFromDatabase_task;

	public static String		Import_Data_Action_RemoveTour;
	public static String		Import_Data_Dialog_ConfirmReimport_Message;
	public static String		Import_Data_Dialog_ConfirmReimportTimeSlices_Message;
	public static String		Import_Data_Dialog_ReimportIsInvalid_WrongSliceNumbers_Message;
	public static String		Import_Data_Label_Hint;
	public static String		Import_Data_Label_Info;
	public static String		Import_Data_Link_Import;
	public static String		Import_Data_Link_Transfer;
	public static String		Import_Data_Link_TransferDirectly;

	public static String		Import_Wizard_Control_combo_person_default_settings;
	public static String		Import_Wizard_Control_combo_ports_not_available;
	public static String		Import_Wizard_Dlg_message;
	public static String		Import_Wizard_Dlg_title;
	public static String		Import_Wizard_Error_com_port_is_required;
	public static String		Import_Wizard_Error_path_is_invalid;
	public static String		Import_Wizard_Error_select_a_device;
	public static String		Import_Wizard_Label_auto_save_path;
	public static String		Import_Wizard_Label_device;
	public static String		Import_Wizard_Label_serial_port;
	public static String		Import_Wizard_Label_use_settings;
	public static String		Import_Wizard_Message_replace_existing_file;
	public static String		Import_Wizard_Message_Title;
	public static String		Import_Wizard_Monitor_stop_port;
	public static String		Import_Wizard_Monitor_task_msg;
	public static String		Import_Wizard_Monitor_task_received_bytes;
	public static String		Import_Wizard_Monitor_wait_for_data;
	public static String		Import_Wizard_Thread_name_read_device_data;

	public static String		merge_tour_dlg_invalid_serie_data_message;
	public static String		merge_tour_dlg_invalid_tour_data_message;
	public static String		merge_tour_dlg_invalid_tour_message;
	public static String		merge_tour_dlg_invalid_tour_title;
	public static String		merge_tour_source_graph_altitude;
	public static String		merge_tour_source_graph_altitude_tooltip;
	public static String		merge_tour_source_graph_cadence;
	public static String		merge_tour_source_graph_cadence_tooltip;
	public static String		merge_tour_source_graph_heartbeat;
	public static String		merge_tour_source_graph_heartbeat_tooltip;
	public static String		merge_tour_source_graph_temperature;
	public static String		merge_tour_source_graph_temperature_tooltip;

	public static String		NT001_DialogExtractTour_InvalidTourData;

	public static String		Pref_App_Label_TourEditorIsModified;

	public static String		Pref_Appearance_Check_AutoOpenTagging;
	public static String		Pref_Appearance_Check_TaggingAnimation;
	public static String		Pref_Appearance_Group_Tagging;
	public static String		Pref_Appearance_Label_AutoOpenTagging_Tooltip;
	public static String		Pref_Appearance_Label_AutoOpenTaggingDelay;
	public static String		Pref_Appearance_NumberOfRecent_TourTypes;
	public static String		Pref_Appearance_NumberOfRecent_TourTypes_Tooltip;
	public static String		Pref_Appearance_ShowTourTypeContextMenu;
	public static String		Pref_Appearance_ShowTourTypeContextMenu_Tooltip;

	public static String		Pref_ChartColors_btn_legend;
	public static String		Pref_ChartColors_unit_high;
	public static String		Pref_ChartColors_unit_low;
	public static String		Pref_ChartColors_unit_max;
	public static String		Pref_ChartColors_unit_mid;
	public static String		Pref_ChartColors_unit_min;

	public static String		Pref_DataImport_Label;

	public static String		Pref_general_imperial_unit_fahrenheit;
	public static String		Pref_general_imperial_unit_feet;
	public static String		Pref_general_imperial_unit_mi;
	public static String		Pref_general_metric_unit_celcius;
	public static String		Pref_general_metric_unit_km;
	public static String		Pref_general_metric_unit_m;
	public static String		Pref_general_show_system_in_ui;
	public static String		Pref_general_system_altitude;
	public static String		Pref_general_system_distance;
	public static String		Pref_general_system_measurement;
	public static String		Pref_general_system_temperature;
	public static String		Pref_General_Button_ComputeCalendarWeek;
	public static String		Pref_General_CalendarWeek;
	public static String		Pref_General_Dialog_CalendarWeekIsModified_Message;
	public static String		Pref_General_Dialog_CalendarWeekIsModified_Title;
	public static String		Pref_General_Label_FirstDayOfWeek;
	public static String		Pref_General_Label_FirstDayOfWeek_Tooltip;
	public static String		Pref_General_Label_MeasurementSystem;
	public static String		Pref_General_Label_MinimalDaysInFirstWeek;
	public static String		Pref_General_Label_MinimalDaysInFirstWeek_Tooltip;

	public static String		Pref_Graphs_Button_down;
	public static String		Pref_Graphs_Button_up;
	public static String		Pref_Graphs_Check_autozoom;
	public static String		Pref_Graphs_Check_force_minimum_for_altimeter;
	public static String		Pref_Graphs_Check_force_minimum_for_gradient;
	public static String		Pref_Graphs_Check_force_minmax_for_pace;
	public static String		Pref_Graphs_Check_show_start_time;
	public static String		Pref_Graphs_Checkbox_ShowHorizontalGrid;
	public static String		Pref_Graphs_Checkbox_ShowVerticalGrid;
	public static String		Pref_Graphs_Error_one_graph_must_be_selected;
	public static String		Pref_Graphs_Error_value_must_be_integer;
	public static String		Pref_Graphs_force_minimum_value;
	public static String		Pref_Graphs_grid_distance;
	public static String		Pref_Graphs_grid_horizontal_distance;
	public static String		Pref_Graphs_grid_vertical_distance;
	public static String		Pref_Graphs_Group_Grid;
	public static String		Pref_Graphs_Group_mouse_mode;
	public static String		Pref_Graphs_Group_units_for_xaxis;
	public static String		Pref_Graphs_Group_zoom_options;
	public static String		Pref_Graphs_Label_select_graph;
	public static String		Pref_Graphs_Label_select_graph_tooltip;
	public static String		Pref_Graphs_move_sliders_when_zoomed;
	public static String		Pref_Graphs_Radio_mouse_mode_slider;
	public static String		Pref_Graphs_Radio_mouse_mode_zoom;
	public static String		Pref_Graphs_Radio_show_distance;
	public static String		Pref_Graphs_Radio_show_time;
	public static String		Pref_Graphs_Tab_default_values;
	public static String		Pref_Graphs_Tab_graph_defaults;
	public static String		Pref_Graphs_Tab_Grid;
	public static String		Pref_Graphs_Tab_zoom_options;
	public static String		Pref_Graphs_Text_max_value;
	public static String		Pref_Graphs_Text_min_value;

	public static String		Pref_MapLayout_Dialog_OSX_Warning_Message;
	public static String		Pref_MapLayout_Dialog_OSX_Warning_Title;
	public static String		Pref_MapLayout_Group_TourInMapProperties;
	public static String		Pref_MapLayout_Label_TourPaintMethod;
	public static String		Pref_MapLayout_Label_TourPaintMethod_Complex;
	public static String		Pref_MapLayout_Label_TourPaintMethod_Complex_Tooltip;
	public static String		Pref_MapLayout_Label_TourPaintMethod_Simple;
	public static String		Pref_MapLayout_Label_TourPaintMethod_Simple_Tooltip;

	public static String		Pref_People_Action_add_person;
	public static String		Pref_People_Button_HrZones_ComputeAllTours;
	public static String		Pref_People_Button_HrZones_ComputeAllTours_Tooltip;
	public static String		Pref_People_Column_Birthday;
	public static String		Pref_People_Column_device;
	public static String		Pref_People_Column_first_name;
	public static String		Pref_People_Column_height;
	public static String		Pref_People_Column_last_name;
	public static String		Pref_People_Column_weight;
	public static String		Pref_People_Dialog_ComputeHrZonesForAllTours_Message;
	public static String		Pref_People_Dialog_ComputeHrZonesForAllToursIsCanceled_Message;
	public static String		Pref_People_Dialog_SaveModifiedPerson_Message;
	public static String		Pref_People_Dialog_SaveModifiedPerson_Title;
	public static String		Pref_People_Error_ComputeHrZonesForAllTours;
	public static String		Pref_People_Error_first_name_is_required;
	public static String		Pref_People_Error_path_is_invalid;
	public static String		Pref_People_Label_Age;
	public static String		Pref_People_Label_Birthday;
	public static String		Pref_People_Label_DataTransfer;
	public static String		Pref_People_Label_DefaultDataTransferFilePath;
	public static String		Pref_People_Label_DefaultDataTransferFilePath_Tooltip;
	public static String		Pref_People_Label_device;
	public static String		Pref_People_Label_first_name;
	public static String		Pref_People_Label_Gender;
	public static String		Pref_People_Label_GenderFemale;
	public static String		Pref_People_Label_GenderMale;
	public static String		Pref_People_Label_height;
	public static String		Pref_People_Label_HrZoneInfo;
	public static String		Pref_People_Label_HrZoneTemplate_Tooltip;
	public static String		Pref_People_Label_last_name;
	public static String		Pref_People_Label_MaxHR;
	public static String		Pref_People_Label_RestingHR;
	public static String		Pref_People_Label_weight;
	public static String		Pref_People_Label_Years;
	public static String		Pref_People_Tab_DataTransfer;
	public static String		Pref_People_Tab_HRZone;
	public static String		Pref_People_Tab_Person;
	public static String		Pref_People_Title;

	public static String		pref_appearance_number_of_recent_tags;
	public static String		pref_appearance_number_of_recent_tags_tooltip;
	public static String		pref_appearance_showMemoryMonitor;
	public static String		pref_appearance_showMemoryMonitor_message;
	public static String		pref_appearance_showMemoryMonitor_title;

	public static String		pref_general_restart_app_message;
	public static String		pref_general_restart_app_title;

	public static String		pref_map_layout_BorderWidth;
	public static String		pref_map_layout_dim_color;
	public static String		pref_map_layout_PaintBorder;
	public static String		pref_map_layout_symbol;
	public static String		pref_map_layout_symbol_dot;
	public static String		pref_map_layout_symbol_line;
	public static String		pref_map_layout_symbol_square;
	public static String		pref_map_layout_symbol_width;

	public static String		pref_statistic_lbl_info;

	public static String		pref_tour_editor_description_height;
	public static String		pref_tour_editor_description_height_tooltip;
	public static String		pref_tour_editor_dlg_desc_height_message;
	public static String		pref_tour_editor_dlg_desc_height_title;

	public static String		pref_tourtag_btn_new_tag;
	public static String		pref_tourtag_btn_new_tag_category;
	public static String		pref_tourtag_btn_rename;
	public static String		pref_tourtag_btn_reset;
	public static String		pref_tourtag_dlg_new_tag_category_message;
	public static String		pref_tourtag_dlg_new_tag_category_title;
	public static String		pref_tourtag_dlg_new_tag_message;
	public static String		pref_tourtag_dlg_new_tag_title;
	public static String		pref_tourtag_dlg_rename_message;
	public static String		pref_tourtag_dlg_rename_message_category;
	public static String		pref_tourtag_dlg_rename_title;
	public static String		pref_tourtag_dlg_rename_title_category;
	public static String		pref_tourtag_dlg_reset_message;
	public static String		pref_tourtag_dlg_reset_title;
	public static String		pref_tourtag_hint;
	public static String		pref_tourtag_viewer_title;

	public static String		pref_view_layout_display_lines;
	public static String		pref_view_layout_display_lines_Tooltip;
	public static String		pref_view_layout_group_display_format;
	public static String		pref_view_layout_label_category;
	public static String		pref_view_layout_label_color_group;
	public static String		pref_view_layout_label_driving_time_format;
	public static String		pref_view_layout_label_format_hh_mm;
	public static String		pref_view_layout_label_format_hh_mm_ss;
	public static String		pref_view_layout_label_recording_time_format;
	public static String		pref_view_layout_label_segmenter_down;
	public static String		pref_view_layout_label_segmenter_up;
	public static String		pref_view_layout_label_sub;
	public static String		pref_view_layout_label_sub_sub;
	public static String		pref_view_layout_label_title;

	public static String		Pref_Statistic_Label_altitude;
	public static String		Pref_Statistic_Label_altitude_low_value;
	public static String		Pref_Statistic_Label_altitude_quantity;
	public static String		Pref_Statistic_Label_distance;
	public static String		Pref_Statistic_Label_distance_low_value;
	public static String		Pref_Statistic_Label_distance_quantity;
	public static String		Pref_Statistic_Label_duration;
	public static String		Pref_Statistic_Label_duration_interval;
	public static String		Pref_Statistic_Label_duration_low_value;
	public static String		Pref_Statistic_Label_duration_quantity;
	public static String		Pref_Statistic_Label_h;
	public static String		Pref_Statistic_Label_interval;
	public static String		Pref_Statistic_Label_separator;

	public static String		Pref_Tour_Dialog_TourCacheIsModified_Message;
	public static String		Pref_Tour_Dialog_TourCacheIsModified_Title;
	public static String		Pref_Tour_Group_TourCache;
	public static String		Pref_Tour_Label_TourCacheSize;
	public static String		Pref_Tour_Label_TourCacheSize_Info;

	public static String		Pref_TourTag_Link_AppearanceOptions;

	public static String		Pref_TourTypeFilter_button_new;
	public static String		Pref_TourTypeFilter_button_remove;
	public static String		Pref_TourTypeFilter_button_rename;
	public static String		Pref_TourTypeFilter_dlg_new_message;
	public static String		Pref_TourTypeFilter_dlg_new_title;
	public static String		Pref_TourTypeFilter_dlg_rename_message;
	public static String		Pref_TourTypeFilter_dlg_rename_title;

	public static String		Pref_TourTypes_Button_add;
	public static String		Pref_TourTypes_Button_delete;
	public static String		Pref_TourTypes_Button_rename;
	public static String		Pref_TourTypes_Dlg_delete_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_delete_tour_type_title;
	public static String		Pref_TourTypes_Dlg_new_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_new_tour_type_title;
	public static String		Pref_TourTypes_Dlg_rename_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_rename_tour_type_title;
	public static String		Pref_TourTypes_dnd_hint;
	public static String		Pref_TourTypes_root_title;
	public static String		Pref_TourTypes_Title;

	public static String		PrefPage_ViewActions_Group;
	public static String		PrefPage_ViewActions_Label_DoubleClick;
	public static String		PrefPage_ViewActions_Label_DoubleClick_AdjustAltitude;
	public static String		PrefPage_ViewActions_Label_DoubleClick_EditMarker;
	public static String		PrefPage_ViewActions_Label_DoubleClick_EditTour;
	public static String		PrefPage_ViewActions_Label_DoubleClick_None;
	public static String		PrefPage_ViewActions_Label_DoubleClick_NoneNoWarning;
	public static String		PrefPage_ViewActions_Label_DoubleClick_OpenTour;
	public static String		PrefPage_ViewActions_Label_DoubleClick_QuickEdit;
	public static String		PrefPage_ViewActions_Label_Info;

	public static String		PrefPage_ViewTooltip_Button_DisableAll;
	public static String		PrefPage_ViewTooltip_Button_EnableAll;
	public static String		PrefPage_ViewTooltip_Group;
	public static String		PrefPage_ViewTooltip_Label_Date;
	public static String		PrefPage_ViewTooltip_Label_Day;
	public static String		PrefPage_ViewTooltip_Label_Info;
	public static String		PrefPage_ViewTooltip_Label_RawData;
	public static String		PrefPage_ViewTooltip_Label_ReferenceTour;
	public static String		PrefPage_ViewTooltip_Label_TagFirstColumn;
	public static String		PrefPage_ViewTooltip_Label_TaggedTour;
	public static String		PrefPage_ViewTooltip_Label_Tags;
	public static String		PrefPage_ViewTooltip_Label_Time;
	public static String		PrefPage_ViewTooltip_Label_Title;
	public static String		PrefPage_ViewTooltip_Label_TourBook;
	public static String		PrefPage_ViewTooltip_Label_TourCatalog;
	public static String		PrefPage_ViewTooltip_Label_WeekDay;

	public static String		PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_down;
	public static String		PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_up;

	public static String		Tour_Action_auto_move_sliders_when_zoomed;
	public static String		Tour_Action_auto_zoom_to_slider_position;
	public static String		Tour_Action_chart_options_tooltip;
	public static String		Tour_Action_EditChartPreferences;
	public static String		Tour_Action_graph_altimeter_tooltip;
	public static String		Tour_Action_graph_altitude_tooltip;
	public static String		Tour_Action_graph_analyzer_tooltip;
	public static String		Tour_Action_graph_cadence_tooltip;
	public static String		Tour_Action_graph_gradient_tooltip;
	public static String		Tour_Action_graph_heartbeat_tooltip;
	public static String		Tour_Action_graph_pace_tooltip;
	public static String		Tour_Action_graph_power_tooltip;
	public static String		Tour_Action_graph_speed_tooltip;
	public static String		Tour_Action_graph_temperature_tooltip;
	public static String		Tour_Action_graph_tour_compare_tooltip;
	public static String		Tour_Action_HrZone_Title;
	public static String		Tour_Action_HrZoneGraphType_Default;
	public static String		Tour_Action_HrZoneGraphType_NoGradient;
	public static String		Tour_Action_HrZoneGraphType_WhiteBottom;
	public static String		Tour_Action_HrZoneGraphType_WhiteTop;
	public static String		Tour_Action_scroll_zoomed_chart;
	public static String		Tour_Action_show_distance_on_x_axis;
	public static String		Tour_Action_show_distance_on_x_axis_tooltip;
	public static String		Tour_Action_show_start_time_on_x_axis;
	public static String		Tour_Action_show_time_on_x_axis;
	public static String		Tour_Action_show_time_on_x_axis_tooltip;
	public static String		Tour_Action_ShowBreaktimeValues;
	public static String		Tour_Action_ShowHrZones_Tooltip;
	public static String		Tour_Action_ShowTourMarker;

	public static String		Tour_Book_Action_delete_selected_tours;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_message;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_message_confirm;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_title;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_title_confirm;
	public static String		Tour_Book_Action_delete_selected_tours_menu;
	public static String		Tour_Book_Action_DeleteSelectedTours_Monitor;
	public static String		Tour_Book_Action_DeleteSelectedTours_MonitorSubtask;

	public static String		Tour_Book_Combo_statistic_tooltip;
	public static String		Tour_Book_Combo_year_tooltip;
	public static String		Tour_Book_Label_chart_title;

	public static String		Tour_Data_SaveTour_Monitor;
	public static String		Tour_Data_SaveTour_MonitorSubtask;

	public static String		Tour_Database_CannotConnectToDerbyServer_Message;
	public static String		Tour_Database_CannotConnectToDerbyServer_Title;
	public static String		Tour_Database_Dialog_ValidateFields_Message;
	public static String		Tour_Database_Dialog_ValidateFields_Title;
	public static String		Tour_Database_load_all_tours;
	public static String		Tour_Database_PostUpdate011_SetTourCreateTime;
	public static String		Tour_Database_TourSaveError;
	public static String		Tour_Database_update_tour;
	public static String		Tour_Database_Update;
	public static String		Tour_Database_Update_Subtask;
	public static String		Tour_Database_Update_TourWeek;
	public static String		Tour_Database_Update_TourWeek_Info;
	public static String		Tour_Database_UpdateDone;
	public static String		Tour_Database_UpdateInfo;

	public static String		Tour_Editor_Label_AltitudeDown;
	public static String		Tour_Editor_Label_AltitudeUp;
	public static String		Tour_Editor_Label_DateTimeCreated;
	public static String		Tour_Editor_Label_DateTimeModified;
	public static String		Tour_Editor_Label_DeviceFirmwareVersion;
	public static String		Tour_Editor_Label_DeviceSensor_Tooltip;
	public static String		Tour_Editor_Label_DistanceSensor_Tooltip;
	public static String		Tour_Editor_Label_Hours_Tooltip;
	public static String		Tour_Editor_Label_Minutes_Tooltip;
	public static String		Tour_Editor_Label_PowerSensor;
	public static String		Tour_Editor_Label_PulseSensor;
	public static String		Tour_Editor_Label_Seconds_Tooltip;
	public static String		Tour_Editor_Label_Sensor_No;
	public static String		Tour_Editor_Label_Sensor_Yes;
	public static String		Tour_Editor_Label_Weather;
	public static String		Tour_Editor_Label_WindDirection_Unit;

	public static String		Tour_Marker_Column_horizontal_offset;
	public static String		Tour_Marker_Column_horizontal_offset_tooltip;
	public static String		Tour_Marker_Column_km_tooltip;
	public static String		Tour_Marker_Column_remark;
	public static String		Tour_Marker_Column_time;
	public static String		Tour_Marker_Column_vertical_offset;
	public static String		Tour_Marker_Column_vertical_offset_tooltip;
	public static String		Tour_Marker_Position_horizontal_above_centered;
	public static String		Tour_Marker_Position_horizontal_above_left;
	public static String		Tour_Marker_Position_horizontal_above_right;
	public static String		Tour_Marker_Position_horizontal_below_centered;
	public static String		Tour_Marker_Position_horizontal_below_left;
	public static String		Tour_Marker_Position_horizontal_below_right;
	public static String		Tour_Marker_Position_horizontal_left;
	public static String		Tour_Marker_Position_horizontal_right;
	public static String		Tour_Marker_Position_vertical_above;
	public static String		Tour_Marker_Position_vertical_below;
	public static String		Tour_Marker_Position_vertical_chart_bottom;
	public static String		Tour_Marker_Position_vertical_chart_top;

	public static String		Tour_Segmenter_Label_default_tolerance;
	public static String		Tour_Segmenter_Label_no_chart;
	public static String		Tour_Segmenter_Label_tolerance;
	public static String		Tour_Segmenter_Type_ByBreakTime;

	public static String		Tour_Statistic_Combo_BarVOrder_InfoItem;
	public static String		Tour_Statistic_Combo_BarVOrder_Tooltip;

	public static String		tag_view_action_refresh_view_tooltip;
	public static String		tag_view_title_tag;
	public static String		tag_view_title_tag_category;

	public static String		tour_action_show_srtm_data;

	public static String		tour_data_label_device_marker;
	public static String		tour_data_label_feature_since_version_9_01;
	public static String		tour_data_label_manually_created_tour;

	public static String		tour_database_computeComputedValues_resultMessage;
	public static String		tour_database_computeComputedValues_resultTitle;
	public static String		tour_database_computeComputeValues_mainTask;
	public static String		tour_database_computeComputeValues_subTask;
	public static String		tour_database_version_info_message;
	public static String		tour_database_version_info_title;

	public static String		tour_editor_csvTimeSliceExport;
	public static String		tour_editor_dlg_create_tour_message;
	public static String		tour_editor_dlg_create_tour_title;
	public static String		tour_editor_dlg_delete_marker_message;
	public static String		tour_editor_dlg_delete_marker_title;
	public static String		tour_editor_dlg_delete_rows_message;
	public static String		tour_editor_dlg_delete_rows_mode_message;
	public static String		tour_editor_dlg_delete_rows_mode_toggle_message;
	public static String		tour_editor_dlg_delete_rows_not_successive;
	public static String		tour_editor_dlg_delete_rows_title;
	public static String		tour_editor_dlg_discard_tour_message;
	public static String		tour_editor_dlg_discard_tour_title;
	public static String		tour_editor_dlg_reload_data_message;
	public static String		tour_editor_dlg_reload_data_title;
	public static String		tour_editor_dlg_revert_tour_message;
	public static String		tour_editor_dlg_revert_tour_title;
	public static String		tour_editor_dlg_revert_tour_toggle_message;
	public static String		tour_editor_dlg_save_invalid_tour;
	public static String		tour_editor_dlg_save_tour_message;
	public static String		tour_editor_dlg_save_tour_title;
	public static String		tour_editor_label_datapoints;
	public static String		tour_editor_label_description;
	public static String		tour_editor_label_device_name;
	public static String		tour_editor_label_distance;
	public static String		tour_editor_label_driving_time;
	public static String		tour_editor_label_end_location;
	public static String		tour_editor_label_import_file_path;
	public static String		tour_editor_label_merge_from_tour_id;
	public static String		tour_editor_label_merge_from_tour_id_tooltip;
	public static String		tour_editor_label_merge_into_tour_id;
	public static String		tour_editor_label_merge_into_tour_id_tooltip;
	public static String		tour_editor_label_paused_time;
	public static String		tour_editor_label_person;
	public static String		tour_editor_label_recording_time;
	public static String		tour_editor_label_ref_tour;
	public static String		tour_editor_label_ref_tour_none;
	public static String		tour_editor_label_start_location;
	public static String		tour_editor_label_start_time;
	public static String		tour_editor_label_time;
	public static String		tour_editor_label_time_unit;
	public static String		tour_editor_label_tour_calories;
	public static String		tour_editor_label_tour_calories_unit;
	public static String		tour_editor_label_tour_date;
	public static String		tour_editor_label_tour_distance;
	public static String		tour_editor_label_tour_id;
	public static String		tour_editor_label_tour_id_tooltip;
	public static String		tour_editor_label_tour_tag;
	public static String		tour_editor_label_tour_title;
	public static String		tour_editor_label_tour_type;
	public static String		tour_editor_label_wind_direction;
	public static String		tour_editor_label_wind_direction_Tooltip;
	public static String		tour_editor_label_wind_speed;
	public static String		tour_editor_label_wind_speed_Tooltip;
	public static String		tour_editor_label_clouds;
	public static String		tour_editor_label_clouds_Tooltip;
	public static String		tour_editor_label_DistanceSensor;
	public static String		tour_editor_label_temperature;
	public static String		tour_editor_label_temperature_Tooltip;
	public static String		tour_editor_label_rest_pulse;
	public static String		tour_editor_label_rest_pulse_Tooltip;
	public static String		tour_editor_label_WindDirectionNESW_Tooltip;
	public static String		tour_editor_message_person_is_required;
	public static String		tour_editor_message_show_another_tour;
	public static String		tour_editor_section_characteristics;
	public static String		tour_editor_section_date_time;
	public static String		tour_editor_section_info;
	public static String		tour_editor_section_tour;
	public static String		tour_editor_section_personal;
	public static String		tour_editor_section_weather;
	public static String		tour_editor_tabLabel_info;
	public static String		tour_editor_tabLabel_tour;
	public static String		tour_editor_tabLabel_tour_data;
	public static String		tour_editor_tabLabel_tour_marker;
	public static String		tour_editor_tour_new_tooltip;

	public static String		tour_merger_btn_reset_adjustment;
	public static String		tour_merger_btn_reset_adjustment_tooltip;
	public static String		tour_merger_btn_reset_values;
	public static String		tour_merger_btn_reset_values_tooltip;
	public static String		tour_merger_chk_adjust_altitude_from_source;
	public static String		tour_merger_chk_adjust_altitude_from_source_tooltip;
	public static String		tour_merger_chk_adjust_altitude_linear_interpolition;
	public static String		tour_merger_chk_adjust_altitude_linear_interpolition_tooltip;
	public static String		tour_merger_chk_adjust_start_altitude;
	public static String		tour_merger_chk_adjust_start_altitude_tooltip;
	public static String		tour_merger_chk_alti_diff_scaling;
	public static String		tour_merger_chk_alti_diff_scaling_tooltip;
	public static String		tour_merger_chk_keep_horiz_vert_adjustments;
	public static String		tour_merger_chk_keep_horiz_vert_adjustments_tooltip;
	public static String		tour_merger_chk_preview_graphs;
	public static String		tour_merger_chk_preview_graphs_tooltip;
	public static String		tour_merger_chk_set_tour_type;
	public static String		tour_merger_chk_set_tour_type_tooltip;
	public static String		tour_merger_chk_use_synced_start_time;
	public static String		tour_merger_chk_use_synced_start_time_tooltip;
	public static String		tour_merger_dialog_header_message;
	public static String		tour_merger_dialog_header_title;
	public static String		tour_merger_dialog_title;
	public static String		tour_merger_group_adjust_altitude;
	public static String		tour_merger_group_adjust_time;
	public static String		tour_merger_group_save_actions;
	public static String		tour_merger_group_save_actions_tooltip;
	public static String		tour_merger_label_adjust_minutes;
	public static String		tour_merger_label_adjust_seconds;
	public static String		tour_merger_save_target_tour;

	public static String		tour_segmenter_button_updateAltitude;
	public static String		tour_segmenter_button_updateAltitude_tooltip;
	public static String		tour_segmenter_label_createSegmentsWith;
	public static String		tour_segmenter_label_tourAltitude_tooltip;
	public static String		tour_segmenter_segType_byDistance_defaultDistance;
	public static String		tour_segmenter_segType_byDistance_label;
	public static String		tour_segmenter_segType_byUpDownAlti_label;
	public static String		tour_segmenter_type_byAltitude;
	public static String		tour_segmenter_type_byComputedAltiUpDown;
	public static String		tour_segmenter_type_byDistance;
	public static String		tour_segmenter_type_byMarker;
	public static String		tour_segmenter_type_byPulse;

	public static String		tour_statistic_label_years;
	public static String		tour_statistic_number_of_years;

	public static String		TourAnalyzer_Label_average;
	public static String		TourAnalyzer_Label_difference;
	public static String		TourAnalyzer_Label_left;
	public static String		TourAnalyzer_Label_maximum;
	public static String		TourAnalyzer_Label_minimum;
	public static String		TourAnalyzer_Label_right;
	public static String		TourAnalyzer_Label_value;

	public static String		tourCatalog_view_action_create_left_marker;
	public static String		tourCatalog_view_action_create_marker;
	public static String		tourCatalog_view_action_create_reference_tour;
	public static String		tourCatalog_view_action_create_right_marker;
	public static String		tourCatalog_view_action_delete_tours;
	public static String		tourCatalog_view_action_link;
	public static String		tourCatalog_view_action_rename_reference_tour;
	public static String		tourCatalog_view_action_save_marker;
	public static String		tourCatalog_view_action_synch_chart_years_tooltip;
	public static String		tourCatalog_view_action_synch_charts_byScale_tooltip;
	public static String		tourCatalog_view_action_synch_charts_bySize_tooltip;
	public static String		tourCatalog_view_action_undo_marker_position;
	public static String		tourCatalog_view_compare_job_subtask;
	public static String		tourCatalog_view_compare_job_task;
	public static String		tourCatalog_view_compare_job_title;
	public static String		tourCatalog_view_dlg_add_reference_tour_msg;
	public static String		tourCatalog_view_dlg_add_reference_tour_title;
	public static String		tourCatalog_view_dlg_delete_comparedTour_msg;
	public static String		tourCatalog_view_dlg_delete_comparedTour_title;
	public static String		tourCatalog_view_dlg_delete_refTour_msg;
	public static String		tourCatalog_view_dlg_delete_refTour_title;
	public static String		tourCatalog_view_dlg_rename_reference_tour_msg;
	public static String		tourCatalog_view_dlg_rename_reference_tour_title;
	public static String		tourCatalog_view_dlg_save_compared_tour_message;
	public static String		tourCatalog_view_dlg_save_compared_tour_title;
	public static String		tourCatalog_view_label_chart_title_reference_tour;
	public static String		tourCatalog_view_label_year_chart_title;
	public static String		tourCatalog_view_label_year_not_selected;
	public static String		tourCatalog_view_tooltip_speed;

	public static String		tourCatalog_wizard_Action_deselect_all;
	public static String		tourCatalog_wizard_Action_select_all;
	public static String		tourCatalog_wizard_Action_select_all_tours;
	public static String		tourCatalog_wizard_Column_altitude_up_tooltip;
	public static String		tourCatalog_wizard_Column_distance_tooltip;
	public static String		tourCatalog_wizard_Column_h;
	public static String		tourCatalog_wizard_Column_h_tooltip;
	public static String		tourCatalog_wizard_Column_tour;
	public static String		tourCatalog_wizard_Error_select_reference_tours;
	public static String		tourCatalog_wizard_Error_tour_must_be_selected;
	public static String		tourCatalog_wizard_Group_chart_title;
	public static String		tourCatalog_wizard_Group_selected_tour;
	public static String		tourCatalog_wizard_Group_selected_tour_2;
	public static String		tourCatalog_wizard_Label_a_tour_is_not_selected;
	public static String		tourCatalog_wizard_Label_page_message;
	public static String		tourCatalog_wizard_Label_reference_tour;
	public static String		tourCatalog_wizard_Msg_select_reference_tour;
	public static String		tourCatalog_wizard_Page_compared_tours_title;
	public static String		tourCatalog_wizard_Page_reference_tour_title;
	public static String		tourCatalog_wizard_Wizard_title;

	public static String		TourChart_Property_chart_type_bar;
	public static String		TourChart_Property_chart_type_line;
	public static String		TourChart_Property_check_customize_pace_clipping;
	public static String		TourChart_Property_check_customize_value_clipping;
	public static String		TourChart_Property_label_chart_type;
	public static String		TourChart_Property_label_pace_speed;
	public static String		TourChart_Property_label_time_slices;

	public static String		TourChart_Smoothing_Algorithm_Initial;
	public static String		TourChart_Smoothing_Algorithm_Jamet;
	public static String		TourChart_Smoothing_Checkbox_IsAltitudeSmoothing;
	public static String		TourChart_Smoothing_Checkbox_IsAltitudeSmoothing_Tooltip;
	public static String		TourChart_Smoothing_Checkbox_IsPulseSmoothing;
	public static String		TourChart_Smoothing_Checkbox_IsPulseSmoothing_Tooltip;
	public static String		TourChart_Smoothing_Checkbox_IsSyncSmoothing;
	public static String		TourChart_Smoothing_Checkbox_IsSyncSmoothing_Tooltip;
	public static String		TourChart_Smoothing_Dialog_SmoothAllTours_Message;
	public static String		TourChart_Smoothing_Dialog_SmoothAllTours_Title;
	public static String		TourChart_Smoothing_Label_GradientSmoothing;
	public static String		TourChart_Smoothing_Label_GradientSmoothing_Tooltip;
	public static String		TourChart_Smoothing_Label_RepeatedSmoothing;
	public static String		TourChart_Smoothing_Label_RepeatedSmoothing_Tooltip;
	public static String		TourChart_Smoothing_Label_RepeatedTau;
	public static String		TourChart_Smoothing_Label_RepeatedTau_Tooltip;
	public static String		TourChart_Smoothing_Label_SmoothingAlgorithm;
	public static String		TourChart_Smoothing_Label_SpeedSmoothing;
	public static String		TourChart_Smoothing_Label_SpeedSmoothing_Tooltip;
	public static String		TourChart_Smoothing_Label_TauParameter;
	public static String		TourChart_Smoothing_Link_PrefBreakTime;
	public static String		TourChart_Smoothing_Link_SmoothingOnlineDocumentation;

	public static String		TourData_Label_new_marker;

	public static String		TourDataEditorView_tour_editor_status_tour_contains_ref_tour;

	public static String		TourEditor_Action_ComputeDistanceValuesFromGeoPosition;
	public static String		TourEditor_Action_DeleteDistanceValues;
	public static String		TourEditor_Action_SetAltitudeValuesFromSRTM;
	public static String		TourEditor_Action_SetStartDistanceTo0;

	public static String		TourEditor_Dialog_ComputeDistanceValues_Message;
	public static String		TourEditor_Dialog_ComputeDistanceValues_Title;
	public static String		TourEditor_Dialog_DeleteDistanceValues_Message;
	public static String		TourEditor_Dialog_DeleteDistanceValues_Title;

	public static String		TourEditor_Dialog_SetAltitudeFromSRTM_Message;
	public static String		TourEditor_Dialog_SetAltitudeFromSRTM_Title;

	public static String		Training_Action_EditHrZones;

	public static String		Training_HRZone_Label_Header_Zone;

	public static String		Training_View_Action_ShowAllPulseValues;
	public static String		Training_View_Action_SynchChartScale;
	public static String		Training_View_Label_LeftChartBorder;
	public static String		Training_View_Label_LeftChartBorder_Tooltip;
	public static String		Training_View_Label_NoHrZones;
	public static String		Training_View_Label_NoPulseData;
	public static String		Training_View_Label_RightChartBorder;
	public static String		Training_View_Label_RightChartBorder_Tooltip;
	public static String		Training_View_Link_NoHrZones;

	public static String		UI_Label_no_chart_is_selected;
	public static String		UI_Label_PersonIsRequired;
	public static String		UI_Label_TourIsNotSelected;

	public static String		ui_tour_not_defined;

	public static String		Weather_Clounds_Clouds;
	public static String		Weather_Clounds_Clouny;
	public static String		Weather_Clounds_IsNotDefined;
	public static String		Weather_Clounds_Lightning;
	public static String		Weather_Clounds_Rain;
	public static String		Weather_Clounds_ScatteredShowers;
	public static String		Weather_Clounds_SevereWeatherAlert;
	public static String		Weather_Clounds_Snow;
	public static String		Weather_Clounds_Sunny;

	public static String		Weather_WindDirection_E;
	public static String		Weather_WindDirection_N;
	public static String		Weather_WindDirection_NE;
	public static String		Weather_WindDirection_NW;
	public static String		Weather_WindDirection_S;
	public static String		Weather_WindDirection_SE;
	public static String		Weather_WindDirection_SW;
	public static String		Weather_WindDirection_W;

	public static String		Weather_WindSpeed_Bft00;
	public static String		Weather_WindSpeed_Bft00_Short;
	public static String		Weather_WindSpeed_Bft01;
	public static String		Weather_WindSpeed_Bft01_Short;
	public static String		Weather_WindSpeed_Bft02;
	public static String		Weather_WindSpeed_Bft02_Short;
	public static String		Weather_WindSpeed_Bft03;
	public static String		Weather_WindSpeed_Bft03_Short;
	public static String		Weather_WindSpeed_Bft04;
	public static String		Weather_WindSpeed_Bft04_Short;
	public static String		Weather_WindSpeed_Bft05;
	public static String		Weather_WindSpeed_Bft05_Short;
	public static String		Weather_WindSpeed_Bft06;
	public static String		Weather_WindSpeed_Bft06_Short;
	public static String		Weather_WindSpeed_Bft07;
	public static String		Weather_WindSpeed_Bft07_Short;
	public static String		Weather_WindSpeed_Bft08;
	public static String		Weather_WindSpeed_Bft08_Short;
	public static String		Weather_WindSpeed_Bft09;
	public static String		Weather_WindSpeed_Bft09_Short;
	public static String		Weather_WindSpeed_Bft10;
	public static String		Weather_WindSpeed_Bft10_Short;
	public static String		Weather_WindSpeed_Bft11;
	public static String		Weather_WindSpeed_Bft11_Short;
	public static String		Weather_WindSpeed_Bft12;
	public static String		Weather_WindSpeed_Bft12_Short;

	public static String		Year_Statistic_Combo_LastYears_Tooltip;
	public static String		Year_Statistic_Combo_NumberOfYears_Tooltip;
	public static String		Year_Statistic_Label_NumberOfYears;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
