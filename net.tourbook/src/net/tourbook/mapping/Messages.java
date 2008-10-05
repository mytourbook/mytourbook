/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.mapping;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.mapping.messages";	//$NON-NLS-1$

	public static String		graph_label_gradient_unit;
	public static String		graph_label_heartbeat_unit;

	public static String		image_action_change_tile_factory;

	public static String	image_action_reload_map;
	public static String		image_action_show_tour_in_map;
	public static String		image_action_show_tour_in_map_disabled;
	public static String	image_action_synch_with_slider;

	public static String	image_action_synch_with_slider_disabled;

	public static String		image_action_synch_with_tour;
	public static String		image_action_synch_with_tour_disabled;

	public static String		image_action_tour_color_altitude;
	public static String		image_action_tour_color_altitude_disabled;
	public static String		image_action_tour_color_gradient;
	public static String		image_action_tour_color_gradient_disabled;
	public static String		image_action_tour_color_pace;
	public static String		image_action_tour_color_pace_disabled;
	public static String		image_action_tour_color_pulse;
	public static String		image_action_tour_color_pulse_disabled;
	public static String		image_action_tour_color_speed;
	public static String		image_action_tour_color_speed_disabled;

	public static String		image_action_zoom_centered;
	public static String		image_action_zoom_in;
	public static String		image_action_zoom_in_disabled;
	public static String		image_action_zoom_out;
	public static String		image_action_zoom_out_disabled;
	public static String		image_action_zoom_show_all;
	public static String		image_action_zoom_show_all_disabled;
	public static String		image_action_zoom_show_entire_tour;

	public static String		legend_color_dim_color;
	public static String		legend_color_keep_color;
	public static String		legend_color_lighten_color;

	public static String		legendcolor_dialog_chk_max_value_text;
	public static String		legendcolor_dialog_chk_max_value_tooltip;
	public static String		legendcolor_dialog_chk_min_value_text;
	public static String		legendcolor_dialog_chk_min_value_tooltip;
	public static String		legendcolor_dialog_error_max_greater_min;
	public static String		legendcolor_dialog_error_max_value_is_required;
	public static String		legendcolor_dialog_error_min_value_is_required;
	public static String		legendcolor_dialog_group_minmax_brightness;
	public static String		legendcolor_dialog_group_minmax_value;
	public static String		legendcolor_dialog_max_brightness_label;
	public static String		legendcolor_dialog_max_brightness_tooltip;
	public static String		legendcolor_dialog_min_brightness_label;
	public static String		legendcolor_dialog_min_brightness_tooltip;
	public static String		legendcolor_dialog_title;
	public static String		legendcolor_dialog_title_message;
	public static String		legendcolor_dialog_title_name;
	public static String		legendcolor_dialog_txt_max_value;
	public static String		legendcolor_dialog_txt_min_value;

	public static String		map_action_change_tile_factory_tooltip;

	public static String	map_action_dim_map;

	public static String	map_action_dim_map_0;

	public static String	map_action_dim_map_100;

	public static String	map_action_dim_map_25;

	public static String	map_action_dim_map_50;

	public static String	map_action_dim_map_75;
	public static String		map_action_modify_map_provider;

	public static String	map_action_reload_map;

	public static String	map_action_reset_tile_overlays;
	public static String		map_action_save_default_position;
	public static String		map_action_set_default_position;
	public static String		map_action_show_legend_in_map;
	public static String		map_action_show_slider_in_legend;
	public static String		map_action_show_slider_in_map;
	public static String		map_action_show_start_finish_in_map;
	public static String		map_action_show_tour_in_map;

	public static String		map_action_show_tour_marker;
	public static String	map_action_synch_with_slider;

	public static String		map_action_synch_with_tour;
	public static String		map_action_tour_color_altitude_tooltip;
	public static String		map_action_tour_color_gradient_tooltip;
	public static String		map_action_tour_color_pase_tooltip;
	public static String		map_action_tour_color_pulse_tooltip;
	public static String		map_action_tour_color_speed_tooltip;
	public static String		map_action_zoom_centered;
	public static String		map_action_zoom_in;
	public static String		map_action_zoom_level_centered_tour;
	public static String		map_action_zoom_level_default;
	public static String		map_action_zoom_level_x_value;
	public static String		map_action_zoom_out;
	public static String		map_action_zoom_show_all;
	public static String		map_action_zoom_show_entire_tour;

	public static String		map_properties_show_tile_info;
	public static String		map_properties_show_tile_info_no;
	public static String		map_properties_show_tile_info_yes;

	public static String		modify_mapprovider_btn_down;
	public static String		modify_mapprovider_btn_up;
	public static String		modify_mapprovider_dialog_area_message;
	public static String		modify_mapprovider_dialog_area_title;
	public static String		modify_mapprovider_dialog_title;
	public static String		modify_mapprovider_lbl_toggle_info;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
