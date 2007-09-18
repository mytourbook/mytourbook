/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

	private static final String	BUNDLE_NAME	= "net.tourbook.messages";						//$NON-NLS-1$

	public static String		Action_About;

	public static String		Action_edit_tour;
	public static String		Action_import_rawdata;
	public static String		Action_import_rawdata_direct;
	public static String		Action_import_rawdata_direct_tooltip;
	public static String		Action_import_rawdata_tooltip;
	public static String		Action_Menu_file;
	public static String		Action_Menu_help;
	public static String		Action_Menu_view;
	public static String		Action_open_compare_wizard;
	public static String		Action_open_compare_wizard_tooltip;
	public static String		Action_open_preferences;
	public static String		Action_openview_compare_result;
	public static String		Action_openview_compare_result_tooltip;
	public static String		Action_openview_rawdata;
	public static String		Action_openview_rawdata_tooltip;
	public static String		Action_openview_tourbook;
	public static String		Action_openview_tourbook_tooltip;
	public static String		Action_openview_tourmap;
	public static String		Action_openview_tourmap_tooltip;

	public static String		App_People_item_all;
	public static String		App_People_tooltip;

	public static String		App_Dlg_first_startup_msg;
	public static String		App_Dlg_first_startup_title;

	public static String		App_Title;

	public static String		App_Tour_type_item_all_types;
	public static String		App_Tour_type_item_not_defined;
	public static String		App_Tour_type_tooltip;

	public static String		CompareResult_Action_check_selected_tours;
	public static String		CompareResult_Action_save_checked_tours;
	public static String		CompareResult_Action_save_checked_tours_tooltip;
	public static String		CompareResult_Action_uncheck_selected_tours;
	public static String		CompareResult_Chart_title_compared_tour;
	public static String		CompareResult_Chart_title_reference_tour;
	public static String		CompareResult_Column_diff;
	public static String		CompareResult_Column_diff_tooltip;
	public static String		CompareResult_Column_km;
	public static String		CompareResult_Column_km_tooltip;
	public static String		CompareResult_Column_kmh;
	public static String		CompareResult_Column_kmh_tooltip;
	public static String		CompareResult_Column_tour;

	public static String		Database_Confirm_update;
	public static String		Database_Confirm_update_title;

	public static String		Database_Monitor_db_service_task;
	public static String		Database_Monitor_persistent_service_task;

	public static String		DataImport_Error_file_does_not_exist_msg;
	public static String		DataImport_Error_file_does_not_exist_title;
	public static String		DataImport_Error_invalid_data_format;

	public static String		DeviceManager_Selection_device_is_not_selected;

	public static String		Dlg_Adjust_Altitude_Button_reset_altitudes;
	public static String		Dlg_Adjust_Altitude_Button_reset_altitudes_tooltip;
	public static String		Dlg_Adjust_Altitude_Button_show_original_values;
	public static String		Dlg_Adjust_Altitude_Button_show_original_values_tooltip;
	public static String		Dlg_Adjust_Altitude_Checkbox_autoscale_yaxis;
	public static String		Dlg_Adjust_Altitude_Checkbox_autoscale_yaxis_tooltip;
	public static String		Dlg_Adjust_Altitude_Dlg_save_tour_message;
	public static String		Dlg_Adjust_Altitude_Dlg_save_tour_title;
	public static String		Dlg_Adjust_Altitude_Group_options;
	public static String		Dlg_Adjust_Altitude_Label_adjustment_type;
	public static String		Dlg_Adjust_Altitude_Label_end_altitude;
	public static String		Dlg_Adjust_Altitude_Label_end_altitude_tooltip;
	public static String		Dlg_Adjust_Altitude_Label_max_altitude;
	public static String		Dlg_Adjust_Altitude_Label_max_altitude_tooltip;
	public static String		Dlg_Adjust_Altitude_Label_original_values;
	public static String		Dlg_Adjust_Altitude_Label_start_altitude;
	public static String		Dlg_Adjust_Altitude_Label_start_altitude_tooltip;
	public static String		Dlg_Adjust_Altitude_Message_adjust_end;
	public static String		Dlg_Adjust_Altitude_Message_adjust_max;
	public static String		Dlg_Adjust_Altitude_Message_adjust_start_and_end;
	public static String		Dlg_Adjust_Altitude_Message_adjust_whole_tour;
	public static String		Dlg_Adjust_Altitude_Message_select_type;
	public static String		Dlg_Adjust_Altitude_Radio_keep_bottom_altitude;
	public static String		Dlg_Adjust_Altitude_Radio_keep_bottom_altitude_tooltip;
	public static String		Dlg_Adjust_Altitude_Radio_keep_start_altitude;
	public static String		Dlg_Adjust_Altitude_Radio_keep_start_altitude_tooltip;
	public static String		Dlg_Adjust_Altitude_Title_dlg;
	public static String		Dlg_Adjust_Altitude_Title_window;
	public static String		Dlg_Adjust_Altitude_Type_adjust_end;
	public static String		Dlg_Adjust_Altitude_Type_adjust_height;
	public static String		Dlg_Adjust_Altitude_Type_adjust_whole_tour;
	public static String		Dlg_Adjust_Altitude_Type_Show_original;
	public static String		Dlg_Adjust_Altitude_Type_start_and_end;

	public static String		Format_hhmm;
	public static String		Format_hhmmss;
	public static String		Format_rawdata_file_yyyy_mm_dd;

	public static String		Graph_Label_Altimeter;
	public static String		Graph_Label_Altimeter_unit;
	public static String		Graph_Label_Altitude;
	public static String		Graph_Label_Altitude_unit;
	public static String		Graph_Label_Cadence;
	public static String		Graph_Label_Cadence_unit;
	public static String		Graph_Label_Gradiend;
	public static String		Graph_Label_Gradiend_unit;
	public static String		Graph_Label_Heartbeat;
	public static String		Graph_Label_Heartbeat_unit;
	public static String		Graph_Label_Power;
	public static String		Graph_Label_Speed;
	public static String		Graph_Label_Speed_unit;
	public static String		Graph_Label_Temperature;
	public static String		Graph_Label_Temperature_unit;
	public static String		Graph_Pref_color_gradient_bright;
	public static String		Graph_Pref_color_gradient_dark;
	public static String		Graph_Pref_color_line;
	public static String		Graph_Pref_color_statistic_distance;
	public static String		Graph_Pref_color_statistic_time;

	public static String		Image_adjust_altitude;
	public static String		Image_chart_analyzer;
	public static String		Image_chart_options;
	public static String		Image_database;
	public static String		Image_database_other_person;
	public static String		Image_database_placeholder;
	public static String		Image_delete;
	public static String		Image_delete_disabled;
	public static String		Image_edit_tour;
	public static String		Image_edit_tour_disabled;
	public static String		Image_fit_to_window;
	public static String		Image_graph_altimeter;
	public static String		Image_graph_altitude;
	public static String		Image_graph_cadence;
	public static String		Image_graph_gradient;
	public static String		Image_graph_heartbeat;
	public static String		Image_graph_speed;
	public static String		Image_graph_temperature;
	public static String		Image_import_rawdata;
	public static String		Image_import_rawdata_direct;
	public static String		Image_open_import_file;
	public static String		Image_open_marker_editor;
	public static String		Image_open_tour_segmenter;
	public static String		Image_remove_all;
	public static String		Image_remove_all_disabled;
	public static String		Image_save_raw_data_to_file;
	public static String		Image_save_raw_data_to_file_disabled;
	public static String		Image_save_tour;
	public static String		Image_save_tour_disabled;
	public static String		Image_show_distance_on_x_axis;
	public static String		Image_show_statistic_and_chart;
	public static String		Image_show_statistics;
	public static String		Image_show_time_on_x_axis;
	public static String		Image_show_tour_viewer;
	public static String		Image_show_view_detail;
	public static String		Image_synch_graph_byScale;
	public static String		Image_synch_graph_byScale_disabled;
	public static String		Image_synch_graph_bySize;
	public static String		Image_synch_graph_bySize_disabled;
	public static String		Image_synch_graph_vertical;
	public static String		Image_undo_edit;
	public static String		Image_undo_edit_disabled;
	public static String		Image_view_compare_result;
	public static String		Image_view_compare_wizard;
	public static String		Image_view_rawdata;
	public static String		Image_view_tourbool;
	public static String		Image_view_tourmap;
	public static String		Image_zoom_fit_to_graph;

	public static String		ImportWizard_Control_combo_person_default_settings;
	public static String		ImportWizard_Control_combo_ports_not_available;
	public static String		ImportWizard_Dlg_message;
	public static String		ImportWizard_Dlg_title;
	public static String		ImportWizard_Error_com_port_is_required;
	public static String		ImportWizard_Error_invalid_data_format;
	public static String		ImportWizard_Error_path_is_invalid;
	public static String		ImportWizard_Error_select_a_device;
	public static String		ImportWizard_Label_auto_save_path;
	public static String		ImportWizard_Label_device;
	public static String		ImportWizard_Label_serial_port;
	public static String		ImportWizard_Label_use_settings;
	public static String		ImportWizard_Message_replace_existing_file;
	public static String		ImportWizard_Monitor_stop_port;
	public static String		ImportWizard_Monitor_task_msg;
	public static String		ImportWizard_Monitor_task_received_bytes;
	public static String		ImportWizard_Monitor_wait_for_data;
	public static String		ImportWizard_Thread_name_read_device_data;

	public static String		MarkerDialog_Button_delete;
	public static String		MarkerDialog_Button_delete_tooltip;
	public static String		MarkerDialog_Button_reset_offset;
	public static String		MarkerDialog_Button_reset_offset_tooltip;
	public static String		MarkerDialog_Button_undo;
	public static String		MarkerDialog_Button_undo_tooltip;
	public static String		MarkerDialog_Dlg_Message;
	public static String		MarkerDialog_Dlg_title;
	public static String		MarkerDialog_Label_horizontal_offset;
	public static String		MarkerDialog_Label_marker_name;
	public static String		MarkerDialog_Label_markers;
	public static String		MarkerDialog_Label_position;
	public static String		MarkerDialog_Label_vertical_offset;
	public static String		MarkerDialog_MsgBox_delete_marker_message;
	public static String		MarkerDialog_MsgBox_delete_marker_title;

	public static String		Pref_ChartColors_Column_color;
	public static String		Pref_ChartColors_Label_title;
	public static String		Pref_Graphs_Button_down;
	public static String		Pref_Graphs_Button_up;
	public static String		Pref_Graphs_Check_autozoom;
	public static String		Pref_Graphs_Check_force_minimum_for_altimeter;
	public static String		Pref_Graphs_Check_force_minimum_for_gradient;
	public static String		Pref_Graphs_Check_scroll_zoomed_chart;
	public static String		Pref_Graphs_Check_show_start_time;
	public static String		Pref_Graphs_Error_one_graph_must_be_selected;
	public static String		Pref_Graphs_Error_value_must_be_integer;
	public static String		Pref_Graphs_Group_units_for_xaxis;
	public static String		Pref_Graphs_Group_zoom_options;
	public static String		Pref_Graphs_Label_select_graph;
	public static String		Pref_Graphs_Radio_show_distance;
	public static String		Pref_Graphs_Radio_show_time;
	public static String		Pref_Graphs_Tab_default_values;
	public static String		Pref_Graphs_Tab_graph_defaults;
	public static String		Pref_Graphs_Text_min_value;

	public static String		Pref_People_Action_add_person;
	public static String		Pref_People_Column_device;
	public static String		Pref_People_Column_first_name;
	public static String		Pref_People_Column_height;
	public static String		Pref_People_Column_last_name;
	public static String		Pref_People_Column_weight;
	public static String		Pref_People_Dlg_del_person_message;
	public static String		Pref_People_Dlg_del_person_title;
	public static String		Pref_People_Error_first_name_is_required;
	public static String		Pref_People_Error_invalid_number;
	public static String		Pref_People_Error_path_is_invalid;
	public static String		Pref_People_Group_person;
	public static String		Pref_People_Label_bike;
	public static String		Pref_People_Label_device;
	public static String		Pref_People_Label_first_name;
	public static String		Pref_People_Label_height;
	public static String		Pref_People_Label_last_name;
	public static String		Pref_People_Label_rawdata_path;
	public static String		Pref_People_Label_weight;
	public static String		Pref_People_Title;

	public static String		Pref_Statistic_Label_altitude;
	public static String		Pref_Statistic_Label_altitude_interval;
	public static String		Pref_Statistic_Label_altitude_low_value;
	public static String		Pref_Statistic_Label_altitude_quantity;
	public static String		Pref_Statistic_Label_distance;
	public static String		Pref_Statistic_Label_distance_interval;
	public static String		Pref_Statistic_Label_distance_low_value;
	public static String		Pref_Statistic_Label_distance_quantity;
	public static String		Pref_Statistic_Label_duration;
	public static String		Pref_Statistic_Label_duration_interval;
	public static String		Pref_Statistic_Label_duration_low_value;
	public static String		Pref_Statistic_Label_duration_quantity;
	public static String		Pref_Statistic_Label_h;
	public static String		Pref_Statistic_Label_km;
	public static String		Pref_Statistic_Label_m;
	public static String		Pref_Statistic_Label_separator;

	public static String		Pref_TourTypes_Button_add;
	public static String		Pref_TourTypes_Button_delete;
	public static String		Pref_TourTypes_Button_rename;
	public static String		Pref_TourTypes_Column_Color;
	public static String		Pref_TourTypes_Dlg_delete_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_delete_tour_type_title;
	public static String		Pref_TourTypes_Dlg_new_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_new_tour_type_title;
	public static String		Pref_TourTypes_Dlg_rename_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_rename_tour_type_title;
	public static String		Pref_TourTypes_Title;

	public static String		RawData_Action_clear_view;
	public static String		RawData_Action_clear_view_tooltip;
	public static String		RawData_Action_open_import_file;
	public static String		RawData_Action_open_import_file_tooltip;
	public static String		RawData_Action_save_tour_for_person;
	public static String		RawData_Action_save_tour_with_person;
	public static String		RawData_Action_save_tours_for_person;
	public static String		RawData_Action_show_tour_chart_tooltip;
	public static String		RawData_Chart_title;
	public static String		RawData_Dlg_save_tour_msg;
	public static String		RawData_Dlg_save_tour_title;
	public static String		RawData_Label_unknown_device;

	public static String		Tour_Action_adjust_tour_altitude;
	public static String		Tour_Action_adjust_tour_altitude_tooltip;
	public static String		Tour_Action_auto_zoom_to_slider_position;
	public static String		Tour_Action_chart_options_tooltip;
	public static String		Tour_Action_graph_altimeter_tooltip;
	public static String		Tour_Action_graph_altitude_tooltip;
	public static String		Tour_Action_graph_analyzer_tooltip;
	public static String		Tour_Action_graph_cadence_tooltip;
	public static String		Tour_Action_graph_gradient_tooltip;
	public static String		Tour_Action_graph_heartbeat_tooltip;
	public static String		Tour_Action_graph_speed_tooltip;
	public static String		Tour_Action_graph_temperature_tooltip;
	public static String		Tour_Action_open_marker_editor;
	public static String		Tour_Action_open_marker_view;
	public static String		Tour_Action_open_tour_segmenter_tooltip;
	public static String		Tour_Action_scroll_zoomed_chart;
	public static String		Tour_Action_show_distance_on_x_axis;
	public static String		Tour_Action_show_distance_on_x_axis_tooltip;
	public static String		Tour_Action_show_start_time_on_x_axis;
	public static String		Tour_Action_show_time_on_x_axis;
	public static String		Tour_Action_show_time_on_x_axis_tooltip;
	public static String		Tour_Action_undo_typing;
	public static String		Tour_Action_undo_typing_tooltip;
	public static String		Tour_Action_zoom_fit_to_window;

	public static String		Tour_Label_datapoints;
	public static String		Tour_Label_description;
	public static String		Tour_Label_distance;
	public static String		Tour_Label_distance_unit;
	public static String		Tour_Label_driving_time;
	public static String		Tour_Label_end_location;
	public static String		Tour_Label_recording_time;
	public static String		Tour_Label_start_location;
	public static String		Tour_Label_start_time;
	public static String		Tour_Label_tab_tour_chart;
	public static String		Tour_Label_tab_tour_data;
	public static String		Tour_Label_time;
	public static String		Tour_Label_time_unit;
	public static String		Tour_Label_tour_date;
	public static String		Tour_Label_tour_title;

	public static String		TourAnalyzer_Label_average;
	public static String		TourAnalyzer_Label_difference;
	public static String		TourAnalyzer_Label_left;
	public static String		TourAnalyzer_Label_maximum;
	public static String		TourAnalyzer_Label_minimum;
	public static String		TourAnalyzer_Label_right;
	public static String		TourAnalyzer_Label_value;

	public static String		TourBook_Action_delete_selected_tours;
	public static String		TourBook_Action_set_tour_type;
	public static String		TourBook_Action_set_tour_type_with_dlg;
	public static String		TourBook_Action_show_statistic_and_chart_tooltip;
	public static String		TourBook_Action_show_statistics_tooltip;
	public static String		TourBook_Action_show_tour_chart_tooltip;
	public static String		TourBook_Action_show_tour_viewer_tooltip;
	public static String		TourBook_Action_show_view_detail_tooltip;
	public static String		TourBook_Combo_statistic_tooltip;
	public static String		TourBook_Combo_year_tooltip;
	public static String		TourBook_Dlg_set_tour_type_msg;
	public static String		TourBook_Dlg_set_tour_type_title;
	public static String		TourBook_Label_chart_title;
	public static String		TourBook_Label_no_tour_is_selected;
	public static String		TourBook_Lable_no_statistic_is_selected;

	public static String		TourData_Label_device_marker;
	public static String		TourData_Label_new_marker;

	public static String		TourMap_Action_create_left_marker;
	public static String		TourMap_Action_create_marker;
	public static String		TourMap_Action_create_reference_tour;
	public static String		TourMap_Action_create_right_marker;
	public static String		TourMap_Action_delete_compared_tour;
	public static String		TourMap_Action_delete_tours;
	public static String		TourMap_Action_rename_reference_tour;
	public static String		TourMap_Action_synch_chart_years_tooltip;
	public static String		TourMap_Action_synch_charts_byScale_tooltip;
	public static String		TourMap_Action_synch_charts_bySize_tooltip;
	public static String		TourMap_Column_kmh;
	public static String		TourMap_Column_tour;
	public static String		TourMap_Compare_job_subtask;
	public static String		TourMap_Compare_job_task;
	public static String		TourMap_Compare_job_title;
	public static String		TourMap_Dlg_add_reference_tour_msg;
	public static String		TourMap_Dlg_add_reference_tour_title;
	public static String		TourMap_Dlg_delete_comparedTour_msg;
	public static String		TourMap_Dlg_delete_comparedTour_title;
	public static String		TourMap_Dlg_delete_refTour_msg;
	public static String		TourMap_Dlg_delete_refTour_title;
	public static String		TourMap_Dlg_rename_reference_tour_msg;
	public static String		TourMap_Dlg_rename_reference_tour_title;
	public static String		TourMap_Label_a_tour_is_not_selected;
	public static String		TourMap_Label_altitude_down;
	public static String		TourMap_Label_altitude_up;
	public static String		TourMap_Label_chart_title_compared_tour;
	public static String		TourMap_Label_chart_title_reference_tour;
	public static String		TourMap_Label_chart_title_year_map;
	public static String		TourMap_Label_distance;
	public static String		TourMap_Label_km;
	public static String		TourMap_Label_m;
	public static String		TourMap_Label_year_chart_title;
	public static String		TourMap_Label_year_chart_unit;

	public static String		TourMapWizard_Action_deselect_all;
	public static String		TourMapWizard_Action_select_all;
	public static String		TourMapWizard_Action_select_all_tours;
	public static String		TourMapWizard_Column_h;
	public static String		TourMapWizard_Column_h_tooltip;
	public static String		TourMapWizard_Column_km;
	public static String		TourMapWizard_Column_km_tooltip;
	public static String		TourMapWizard_Column_m;
	public static String		TourMapWizard_Column_m_tooltip;
	public static String		TourMapWizard_Column_tour;
	public static String		TourMapWizard_Error_select_reference_tours;
	public static String		TourMapWizard_Error_tour_must_be_selected;
	public static String		TourMapWizard_Group_chart_title;
	public static String		TourMapWizard_Group_selected_tour;
	public static String		TourMapWizard_Group_selected_tour_2;
	public static String		TourMapWizard_Label_a_tour_is_not_selected;
	public static String		TourMapWizard_Label_page_message;
	public static String		TourMapWizard_Label_reference_tour;
	public static String		TourMapWizard_Msg_select_reference_tour;
	public static String		TourMapWizard_Page_compared_tours_title;
	public static String		TourMapWizard_Page_reference_tour_title;
	public static String		TourMapWizard_Wizard_title;

	public static String		TourMarker_Action_delete_marker;
	public static String		TourMarker_Action_edit_marker;
	public static String		TourMarker_Column_horizontal_offset;
	public static String		TourMarker_Column_horizontal_offset_tooltip;
	public static String		TourMarker_Column_km;
	public static String		TourMarker_Column_km_tooltip;
	public static String		TourMarker_Column_position;
	public static String		TourMarker_Column_remark;
	public static String		TourMarker_Column_time;
	public static String		TourMarker_Column_vertical_offset;
	public static String		TourMarker_Column_vertical_offset_tooltip;
	public static String		TourMarker_Position_horizontal_above_centered;
	public static String		TourMarker_Position_horizontal_above_left;
	public static String		TourMarker_Position_horizontal_above_right;
	public static String		TourMarker_Position_horizontal_below_centered;
	public static String		TourMarker_Position_horizontal_below_left;
	public static String		TourMarker_Position_horizontal_below_right;
	public static String		TourMarker_Position_horizontal_left;
	public static String		TourMarker_Position_horizontal_right;
	public static String		TourMarker_Position_vertical_above;
	public static String		TourMarker_Position_vertical_below;
	public static String		TourMarker_Position_vertical_chart_bottom;
	public static String		TourMarker_Position_vertical_chart_top;

	public static String		TourSegmenter_Check_show_segments_in_chart;
	public static String		TourSegmenter_Column_altimeter_down;
	public static String		TourSegmenter_Column_altimeter_down_tooltip;
	public static String		TourSegmenter_Column_altimeter_up;
	public static String		TourSegmenter_Column_altimeter_up_tooltip;
	public static String		TourSegmenter_Column_altitude;
	public static String		TourSegmenter_Column_altitude_tooltip;
	public static String		TourSegmenter_Column_distance;
	public static String		TourSegmenter_Column_distance_tooltip;
	public static String		TourSegmenter_Column_gradient;
	public static String		TourSegmenter_Column_gradient_tooltip;
	public static String		TourSegmenter_Column_speed;
	public static String		TourSegmenter_Column_speed_tooltip;
	public static String		TourSegmenter_Column_time;
	public static String		TourSegmenter_Column_time_tooltip;
	public static String		TourSegmenter_Label_default_tolerance;
	public static String		TourSegmenter_Label_tolerance;

	public static String		UI_Label_no_chart_is_selected;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
