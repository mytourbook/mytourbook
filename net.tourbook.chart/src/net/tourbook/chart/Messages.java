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
package net.tourbook.chart;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.chart.messages";	//$NON-NLS-1$

	public static String		Action_mouse_mode;
	public static String		Action_mouse_mode_slider;
	public static String		Action_mouse_mode_tooltip;
	public static String		Action_mouse_mode_zoom;

	public static String		Action_move_left_slider_here;
	public static String		Action_move_right_slider_here;
	public static String		Action_move_sliders_to_border;

	public static String		Action_next_month;
	public static String		Action_next_month_tooltip;

	public static String		Action_previous_month;
	public static String		Action_previous_month_tooltip;

	public static String		Action_zoom_fit_to_graph;
	public static String		Action_zoom_fit_to_graph_tooltip;
	public static String		Action_zoom_in;
	public static String		Action_zoom_in_tooltip;
	public static String		Action_zoom_out;
	public static String		Action_zoom_out_tooltip;
	public static String		Action_zoom_to_slider;

	public static String		Error_Message_001_Default;

	public static String		Format_number_float;

	public static String		Format_time_hhmm;
	public static String		Format_time_hhmmss;

	public static String		Image_arrow_left;
	public static String		Image_arrow_left_disabled;
	public static String		Image_arrow_right;
	public static String		Image_arrow_right_disabled;

	public static String		Image_cursor_hand_05x;
	public static String		Image_cursor_hand_10x;
	public static String		Image_cursor_hand_20x;
	public static String		Image_cursor_hand_50x;
	public static String		Image_cursor_mode_slider;
	public static String		Image_cursor_mode_zoom;
	public static String		Image_cursor_mode_zoom_move;
	public static String	Image_Cursor_DragXSlider_ModeSlider;

	public static String	Image_Cursor_DragXSlider_ModeZoom;

	public static String	Image_Cursor_Hover_XSlider;

	public static String		Image_Cursor_Move1x;
	public static String		Image_Cursor_Move2x;
	public static String		Image_Cursor_Move3x;
	public static String		Image_Cursor_Move4x;
	public static String		Image_Cursor_Move5x;
	public static String		Image_Cursor_X_Slider_Left;
	public static String		Image_Cursor_X_Slider_Right;

	public static String		Image_mouse_mode;

	public static String		Image_zoom_fit_to_graph;
	public static String		Image_zoom_fit_to_graph_disabled;
	public static String		Image_zoom_in;
	public static String		Image_zoom_in_disabled;
	public static String		Image_zoom_out;
	public static String		Image_zoom_out_disabled;

	public static String		Month_apr;
	public static String		Month_aug;
	public static String		Month_dec;
	public static String		Month_feb;
	public static String		Month_jan;
	public static String		Month_jul;
	public static String		Month_jun;
	public static String		Month_mai;
	public static String		Month_mar;
	public static String		Month_nov;
	public static String		Month_oct;
	public static String		Month_sep;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
