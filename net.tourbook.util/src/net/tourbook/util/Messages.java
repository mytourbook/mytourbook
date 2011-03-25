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
package net.tourbook.util;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "net.tourbook.util.messages"; //$NON-NLS-1$

	public static String		Action_App_ConfigureColumns;

	public static String		Advanced_Menu_AnimationSymbol;

	public static String		ColumnModifyDialog_Button_default;
	public static String		ColumnModifyDialog_Button_deselect_all;
	public static String		ColumnModifyDialog_Button_move_down;
	public static String		ColumnModifyDialog_Button_move_up;
	public static String		ColumnModifyDialog_Button_select_all;
	public static String		ColumnModifyDialog_column_column;
	public static String		ColumnModifyDialog_column_unit;
	public static String		ColumnModifyDialog_column_width;
	public static String		ColumnModifyDialog_Dialog_title;
	public static String		ColumnModifyDialog_Label_hint;
	public static String		ColumnModifyDialog_Label_Hints;
	public static String		ColumnModifyDialog_Label_info;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
