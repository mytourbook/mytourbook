/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

	public static String		dialog_is_tour_editor_modified_message;
	public static String		dialog_is_tour_editor_modified_title;

	public static String		dialog_quick_edit_dialog_area_title;
	public static String		dialog_quick_edit_dialog_title;

	public static String		App_Action_About;
	public static String		App_Action_edit_tour;
	public static String		App_Action_Menu_file;
	public static String		App_Action_Menu_help;
	public static String		App_Action_Menu_tools;
	public static String		App_Action_Menu_view;

	public static String		App_Action_open_perspective;
	public static String		App_Action_open_preferences;
	public static String		App_Action_open_tour_segmenter;
	public static String		App_Action_open_tour_segmenter_tooltip;

	public static String		action_tag_add;
	public static String		action_tag_dlg_rename_message;
	public static String		action_tag_dlg_rename_title;
	public static String		action_tag_open_tagging_structure;
	public static String		action_tag_recently_used;
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
	public static String		action_tourCatalog_open_compare_wizard;
	public static String		action_tourType_modify_tourTypes;

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

	public static String		App_Action_set_tour_type;

	public static String		App_measurement_imperial;
	public static String		App_measurement_metric;
	public static String		App_measurement_tooltip;

	public static String		App_People_item_all;
	public static String		App_People_tooltip;

	public static String		App_Dlg_first_startup_msg;
	public static String		App_Dlg_first_startup_title;

	public static String		App_Title;

	public static String		App_Tour_type_item_all_types;
	public static String		App_Tour_type_item_not_defined;
	public static String		App_Tour_type_tooltip;

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

	public static String		Database_Confirm_update;
	public static String		Database_Confirm_update_title;
	public static String		Database_Monitor_db_service_task;
	public static String		Database_Monitor_persistent_service_task;

	public static String		DataImport_Error_file_does_not_exist_msg;
	public static String		DataImport_Error_file_does_not_exist_title;
	public static String		DataImport_Error_invalid_data_format;

	public static String		DeviceManager_Selection_device_is_not_selected;

	public static String		Dlg_AdjustAltitude_Button_reset_altitudes;
	public static String		Dlg_AdjustAltitude_Button_reset_altitudes_tooltip;
	public static String		Dlg_AdjustAltitude_Button_show_original_values;
	public static String		Dlg_AdjustAltitude_Button_show_original_values_tooltip;
	public static String		Dlg_AdjustAltitude_Checkbox_autoscale_yaxis;
	public static String		Dlg_AdjustAltitude_Checkbox_autoscale_yaxis_tooltip;
	public static String		Dlg_AdjustAltitude_Group_options;
	public static String		Dlg_AdjustAltitude_Label_adjustment_type;
	public static String		Dlg_AdjustAltitude_Label_end_altitude;
	public static String		Dlg_AdjustAltitude_Label_end_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Label_max_altitude;
	public static String		Dlg_AdjustAltitude_Label_max_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Label_original_values;
	public static String		Dlg_AdjustAltitude_Label_start_altitude;
	public static String		Dlg_AdjustAltitude_Label_start_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Message_adjust_end;
	public static String		Dlg_AdjustAltitude_Message_adjust_max;
	public static String		Dlg_AdjustAltitude_Message_adjust_start_and_end;
	public static String		Dlg_AdjustAltitude_Message_adjust_whole_tour;
	public static String		Dlg_AdjustAltitude_Message_select_type;
	public static String		Dlg_AdjustAltitude_Radio_keep_bottom_altitude;
	public static String		Dlg_AdjustAltitude_Radio_keep_bottom_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Radio_keep_start_altitude;
	public static String		Dlg_AdjustAltitude_Radio_keep_start_altitude_tooltip;
	public static String		Dlg_AdjustAltitude_Title_dlg;
	public static String		Dlg_AdjustAltitude_Title_window;
	public static String		Dlg_AdjustAltitude_Type_adjust_end;
	public static String		Dlg_AdjustAltitude_Type_adjust_height;
	public static String		Dlg_AdjustAltitude_Type_adjust_whole_tour;
	public static String		Dlg_AdjustAltitude_Type_Show_original;
	public static String		Dlg_AdjustAltitude_Type_start_and_end;

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

	public static String		Format_hhmm;
	public static String		Format_hhmmss;
	public static String		Format_rawdata_file_yyyy_mm_dd;

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
	public static String		Image__open_folder_add;
	public static String		Image__options;
	public static String		Image__quick_edit;
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
	public static String		Image__synch_graph_byScale;
	public static String		Image__synch_graph_byScale_disabled;
	public static String		Image__synch_graph_bySize;
	public static String		Image__synch_graph_bySize_disabled;
	public static String		Image__synch_statistics;
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

	public static String		image__merge_tours;

	public static String		import_data_action_adjust_imported_year;
	public static String		import_data_action_assignment_is_not_available;
	public static String		import_data_action_assignMergedTour;
	public static String		import_data_action_assignMergedTour_default;
	public static String		import_data_action_clear_view;
	public static String		import_data_action_clear_view_tooltip;
	public static String		import_data_action_disable_hac4_checksum_validation;
	public static String		import_data_action_merge_tracks;
	public static String		import_data_action_reimport_tour;
	public static String		import_data_action_save_tour_for_person;
	public static String		import_data_action_save_tour_with_person;
	public static String		import_data_action_save_tours_for_person;
	public static String		import_data_dlg_save_tour_msg;
	public static String		import_data_dlg_save_tour_title;
	public static String		import_data_label_unknown_device;

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

	public static String		Pref_ChartColors_btn_legend;
	public static String		Pref_ChartColors_Column_color;
	public static String		Pref_ChartColors_unit_high;
	public static String		Pref_ChartColors_unit_low;
	public static String		Pref_ChartColors_unit_max;
	public static String		Pref_ChartColors_unit_mid;
	public static String		Pref_ChartColors_unit_min;

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

	public static String		Pref_Graphs_Button_down;
	public static String		Pref_Graphs_Button_up;
	public static String		Pref_Graphs_Check_autozoom;
	public static String		Pref_Graphs_Check_force_minimum_for_altimeter;
	public static String		Pref_Graphs_Check_force_minimum_for_gradient;

	public static String		Pref_Graphs_Check_force_minmax_for_pace;
	public static String		Pref_Graphs_Check_show_start_time;
	public static String		Pref_Graphs_Error_one_graph_must_be_selected;
	public static String		Pref_Graphs_Error_value_must_be_integer;
	public static String		Pref_Graphs_force_minimum_value;
	public static String		Pref_Graphs_grid_distance;
	public static String		Pref_Graphs_grid_horizontal_distance;
	public static String		Pref_Graphs_grid_vertical_distance;
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
	public static String		Pref_Graphs_Tab_zoom_options;
	public static String		Pref_Graphs_Text_max_value;
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

	public static String		pref_appearance_number_of_recent_tags;
	public static String		pref_appearance_number_of_recent_tags_tooltip;

	public static String		pref_general_confirmation;
	public static String		pref_general_hide_confirmation;
	public static String		pref_general_hide_warning;
	public static String		pref_general_restart_app_message;
	public static String		pref_general_restart_app_title;

	public static String		pref_map_layout_dim_color;
	public static String		pref_map_layout_symbol;
	public static String		pref_map_layout_symbol_dot;
	public static String		pref_map_layout_symbol_line;
	public static String		pref_map_layout_symbol_square;
	public static String		pref_map_layout_symbol_width;

	public static String		pref_regional_decimalSeparator;
	public static String		pref_regional_description;
	public static String		pref_regional_groupSeparator;
	public static String		pref_regional_title;
	public static String		pref_regional_useCustomDecimalFormat;
	public static String		pref_regional_value_example;

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
	public static String		pref_view_layout_group_display_format;
	public static String		pref_view_layout_label_category;
	public static String		pref_view_layout_label_color_group;
	public static String		pref_view_layout_label_driving_time_format;
	public static String		pref_view_layout_label_format_hh_mm;
	public static String		pref_view_layout_label_format_hh_mm_ss;
	public static String		pref_view_layout_label_recording_time_format;
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
	public static String		Pref_TourTypes_Column_Color;
	public static String		Pref_TourTypes_Dlg_delete_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_delete_tour_type_title;
	public static String		Pref_TourTypes_Dlg_new_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_new_tour_type_title;
	public static String		Pref_TourTypes_Dlg_rename_tour_type_msg;
	public static String		Pref_TourTypes_Dlg_rename_tour_type_title;
	public static String		Pref_TourTypes_dnd_hint;
	public static String		Pref_TourTypes_root_title;
	public static String		Pref_TourTypes_Title;

	public static String		PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_down;
	public static String		PrefPageTourTypeFilterList_Pref_TourTypeFilter_button_up;

	public static String		Tour_Action_auto_move_sliders_when_zoomed;
	public static String		Tour_Action_auto_zoom_to_slider_position;
	public static String		Tour_Action_chart_options_tooltip;
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
	public static String		Tour_Action_scroll_zoomed_chart;
	public static String		Tour_Action_show_distance_on_x_axis;
	public static String		Tour_Action_show_distance_on_x_axis_tooltip;
	public static String		Tour_Action_show_start_time_on_x_axis;
	public static String		Tour_Action_show_time_on_x_axis;
	public static String		Tour_Action_show_time_on_x_axis_tooltip;

	public static String		Tour_Book_Action_delete_selected_tours;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_message;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_message_confirm;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_title;
	public static String		Tour_Book_Action_delete_selected_tours_dlg_title_confirm;
	public static String		Tour_Book_Action_delete_selected_tours_task;
	public static String		Tour_Book_Combo_statistic_tooltip;
	public static String		Tour_Book_Combo_year_tooltip;
	public static String		Tour_Book_Label_chart_title;

	public static String		Tour_Database_load_all_tours;
	public static String		Tour_Database_update_computed_values;
	public static String		Tour_Database_update_tour;

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
	public static String		Tour_Segmenter_label_invalid_data;
	public static String		Tour_Segmenter_Label_no_chart;
	public static String		Tour_Segmenter_Label_tolerance;

	public static String		tag_view_action_refresh_view_tooltip;
	public static String		tag_view_title_tag;
	public static String		tag_view_title_tag_category;

	public static String		tour_data_label_device_marker;
	public static String		tour_data_label_feature_since_version_9_01;
	public static String		tour_data_label_manually_created_tour;

	public static String		tour_database_version_info_message;
	public static String		tour_database_version_info_title;

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
	public static String		tour_editor_label_tour_date;
	public static String		tour_editor_label_tour_distance;
	public static String		tour_editor_label_tour_id;
	public static String		tour_editor_label_tour_id_tooltip;
	public static String		tour_editor_label_tour_tag;
	public static String		tour_editor_label_tour_title;
	public static String		tour_editor_label_tour_type;
	public static String		tour_editor_message_person_is_required;
	public static String		tour_editor_message_show_another_tour;
	public static String		tour_editor_section_characteristics;
	public static String		tour_editor_section_date_time;
	public static String		tour_editor_section_info;
	public static String		tour_editor_section_tour;
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
	public static String		tourCatalog_view_action_number_of_years;
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

	public static String		TourChart_Property_button_compute_values;
	public static String		TourChart_Property_button_compute_values_tooltip;
	public static String		TourChart_Property_chart_type_bar;
	public static String		TourChart_Property_chart_type_line;
	public static String		TourChart_Property_check_customize_pace_clipping;
	public static String		TourChart_Property_check_customize_value_clipping;
	public static String		TourChart_Property_check_customize_value_computing;
	public static String		TourChart_Property_dlg_compute_values_message;
	public static String		TourChart_Property_dlg_compute_values_title;
	public static String		TourChart_Property_label_chart_type;
	public static String		TourChart_Property_label_pace_speed;
	public static String		TourChart_Property_label_time_slices;

	public static String		TourData_Label_new_marker;

	public static String		TourDataEditorView_tour_editor_status_tour_contains_ref_tour;

	public static String		UI_Label_no_chart_is_selected;

	public static String		ui_tour_not_defined;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
