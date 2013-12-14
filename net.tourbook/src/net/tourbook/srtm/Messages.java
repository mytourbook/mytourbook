/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.srtm;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.srtm.messages";			//$NON-NLS-1$

	public static String		app_ui_N;
	public static String		app_ui_Y;

	public static String		profileViewer_column_content_resolution_fine;
	public static String		profileViewer_column_content_resolution_rough;
	public static String		profileViewer_column_content_resolution_veryFine;
	public static String		profileViewer_column_content_resolution_veryRough;
	public static String		profileViewer_column_label_color;
	public static String		profileViewer_column_label_color_header;
	public static String		profileViewer_column_label_color_tooltip;
	public static String		profileViewer_column_label_id;
	public static String		profileViewer_column_label_id_header;
	public static String		profileViewer_column_label_id_tooltip;
	public static String		profileViewer_column_label_imagePath;
	public static String		profileViewer_column_label_imagePath_header;
	public static String		profileViewer_column_label_imagePath_tooltip;
	public static String		profileViewer_column_label_isShadow;
	public static String		profileViewer_column_label_isShadow_header;
	public static String		profileViewer_column_label_isShadow_tooltip;
	public static String		profileViewer_column_label_name;
	public static String		profileViewer_column_label_name_header;
	public static String		profileViewer_column_label_name_tooltip;
	public static String		profileViewer_column_label_resolution;
	public static String		profileViewer_column_label_resolution_header;
	public static String		profileViewer_column_label_resolution_tooltip;
	public static String		profileViewer_column_label_shadowValue;
	public static String		profileViewer_column_label_shadowValue_header;
	public static String		profileViewer_column_label_shadowValue_tooltip;

	public static String		prefPage_srtm_btn_adjust_columns;
	public static String		prefPage_srtm_button_testConnection;
	public static String		prefPage_srtm_checkHTTPConnection_message;
	public static String		prefPage_srtm_checkHTTPConnection_title;
	public static String		prefPage_srtm_checkHTTPConnectionFAILED_message;
	public static String		prefPage_srtm_checkHTTPConnectionOK_message;
	public static String		prefPage_srtm_chk_use_default_location;
	public static String		prefPage_srtm_confirm_defaults_message;
	public static String		prefPage_srtm_confirm_defaults_title;
	public static String		prefPage_srtm_default_profile_name;
	public static String		prefPage_srtm_default_profile_path;
	public static String		prefPage_srtm_dlg_delete_profile_msg;
	public static String		prefPage_srtm_dlg_delete_profile_title;
	public static String		prefPage_srtm_editor_data_filepath;
	public static String		prefPage_srtm_group_label_data_location;
	public static String		prefPage_srtm_group_label_srtm3;
	public static String		prefPage_srtm_link_srtmProfiles;
	public static String		prefPage_srtm_msg_invalid_data_path;
	public static String		prefPage_srtm_msg_invalidSrtm3FtpUrl;
	public static String		prefPage_srtm_msg_invalidSrtm3HttpUrl;
	public static String		prefPage_srtm_profile_add;
	public static String		prefPage_srtm_profile_duplicate;
	public static String		prefPage_srtm_profile_edit;
	public static String		prefPage_srtm_profile_option_apply_when_selected;
	public static String		prefPage_srtm_profile_remove;
	public static String		prefPage_srtm_radio_srtm3FtpUrl;
	public static String		prefPage_srtm_radio_srtm3HttpUrl;
	public static String		prefPage_srtm_resolution_title;
	public static String		prefPage_srtm_resolution_very_fine;
	public static String		prefPage_srtm_resolution_fine;
	public static String		prefPage_srtm_resolution_rough;
	public static String		prefPage_srtm_resolution_very_rough;
	public static String		prefPage_srtm_shadow_text;
	public static String		prefPage_srtm_shadow_value_text;

	public static String		srtm_transfer_error_message;
	public static String		srtm_transfer_error_title;

	public static String		dialog_adjust_srtm_colors_button_add;
	public static String		dialog_adjust_srtm_colors_button_add_multiple;
	public static String		dialog_adjust_srtm_colors_button_apply;
	public static String		dialog_adjust_srtm_colors_button_remove;
	public static String		dialog_adjust_srtm_colors_button_sort;
	public static String		dialog_adjust_srtm_colors_button_update;
	public static String		dialog_adjust_srtm_colors_checkbutton_ttt;
	public static String		dialog_adjust_srtm_colors_color_tooltip;
	public static String		dialog_adjust_srtm_colors_dialog_title;
	public static String		dialog_adjust_srtm_colors_dialog_message;
	public static String		dialog_adjust_srtm_colors_error_invalid_shadow_value;
	public static String		dialog_adjust_srtm_colors_error_invalid_tile_path;
	public static String		dialog_adjust_srtm_colors_label_profile_name;
	public static String		dialog_adjust_srtm_colors_label_tile_path;

	public static String		dialog_multipleVertexes_label_eleDiff;
	public static String		dialog_multipleVertexes_label_endElevation;
	public static String		dialog_multipleVertexes_label_startElevation;
	public static String		dialog_multipleVertexes_title;

	public static String		error_message_cannotConnectToServer;
	public static String		error_message_timeoutWhenConnectingToServer;

	public static String		job_name_downloadMonitor;
	public static String		job_name_ftpDownload;
	public static String		job_name_httpDownload;

	public static String		rgv_vertex_class_cast_exception;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
