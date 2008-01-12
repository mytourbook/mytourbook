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

	public static String		image_action_synch_with_tour;
	public static String		image_action_synch_with_tour_disabled;
	public static String		image_action_zoom_centered;
	public static String		image_action_zoom_in;
	public static String		image_action_zoom_in_disabled;
	public static String		image_action_zoom_out;
	public static String		image_action_zoom_out_disabled;
	public static String		image_action_zoom_show_all;
	public static String		image_action_zoom_show_all_disabled;

	public static String	image_action_zoom_show_entire_tour;

	public static String		map_action_synch_with_tour;
	public static String		map_action_zoom_centered;
	public static String		map_action_zoom_in;

	public static String	map_action_zoom_level_centered_tour;

	public static String	map_action_zoom_level_default;

	public static String	map_action_zoom_level_x_value;
	public static String		map_action_zoom_out;
	public static String		map_action_zoom_show_all;

	public static String	map_action_zoom_show_entire_tour;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
