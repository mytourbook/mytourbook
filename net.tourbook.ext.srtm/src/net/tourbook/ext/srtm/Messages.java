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
package net.tourbook.ext.srtm;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String	BUNDLE_NAME	= "net.tourbook.ext.srtm.messages"; //$NON-NLS-1$

	public static String		profileViewer_column_label_checkbox;
	public static String		profileViewer_column_label_color;
	public static String		profileViewer_column_label_color_header;
	public static String		profileViewer_column_label_color_tooltip;
	public static String		profileViewer_column_label_imagePath;
	public static String		profileViewer_column_label_imagePath_header;
	public static String		profileViewer_column_label_imagePath_tooltip;
	public static String		profileViewer_column_label_name;
	public static String		profileViewer_column_label_name_header;
	public static String		profileViewer_column_label_name_tooltip;

	public static String		prefPage_srtm_btn_adjust_columns;
	public static String		prefPage_srtm_chk_use_default_location;
	public static String		prefPage_srtm_confirm_defaults_message;
	public static String		prefPage_srtm_confirm_defaults_title;
	public static String		prefPage_srtm_default_profile_name;
	public static String		prefPage_srtm_default_profile_path;
	public static String		prefPage_srtm_dlg_delete_profile_msg;
	public static String		prefPage_srtm_dlg_delete_profile_title;
	public static String		prefPage_srtm_editor_data_filepath;
	public static String		prefPage_srtm_group_label_data_location;
	public static String		prefPage_srtm_group_label_colors;
	public static String		prefPage_srtm_msg_invalid_data_path;
	public static String		prefPage_srtm_profile_title;
	public static String		prefPage_srtm_profile_add;
	public static String		prefPage_srtm_profile_remove;
	public static String		prefPage_srtm_resolution_title;
	public static String		prefPage_srtm_resolution_very_fine;
	public static String		prefPage_srtm_resolution_fine;
	public static String		prefPage_srtm_resolution_rough;
	public static String		prefPage_srtm_resolution_very_rough;
	public static String		prefPage_srtm_shadow_text;

	public static String		srtm_transfer_error_message;
	public static String		srtm_transfer_error_title;
	public static String		srtm_transfer_initialize;
	public static String		srtm_transfer_retrieve_file;
	public static String		srtm_transfer_task;

	public static String		Image__adjust_srtm_colors;

	public static String		dialog_adjust_srtm_colors_button_add;
	public static String		dialog_adjust_srtm_colors_button_remove;
	public static String		dialog_adjust_srtm_colors_button_sort;
	public static String		dialog_adjust_srtm_colors_button_update;
	public static String		dialog_adjust_srtm_colors_checkbutton_ttt;
	public static String		dialog_adjust_srtm_colors_dialog_title;
	public static String		dialog_adjust_srtm_colors_dialog_area_title;

	public static String	dialog_adjust_srtm_colors_label_profile_name;

	public static String	dialog_adjust_srtm_colors_label_tile_path;

	public static String		color_chooser_choosed_color;
	public static String		color_chooser_hexagon;
	public static String		color_chooser_hexagon_move;
	public static String		color_chooser_rgb;
	public static String		color_chooser_hsb;
	public static String		color_chooser_red;
	public static String		color_chooser_green;
	public static String		color_chooser_blue;
	public static String		color_chooser_hue;
	public static String		color_chooser_saturation;
	public static String		color_chooser_brightness;

	public static String		rgv_vertex_class_cast_exception;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
