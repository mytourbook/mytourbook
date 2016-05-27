/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
package tourbook.search.nls;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

	private static final String	BUNDLE_NAME	= "tourbook.search.nls.messages";	//$NON-NLS-1$

	public static String		Search_App_Action_EditMarker;
	public static String		Search_App_Action_EditTour;
	public static String		Search_App_Action_SearchOptions_Tooltip;
	public static String		Search_App_Action_StartSearch_Tooltip;
	public static String		Search_App_Label_Status_Tooltip;
	public static String		Search_App_Text_Search_PlaceHolder;
	public static String		Search_App_Tooltip;

	public static String		Search_Grid_Label_NoDataMessage;

	public static String		Search_Options_Action_RestoreDefaults;
	public static String		Search_Options_Checkbox_EaseSearching;
	public static String		Search_Options_Checkbox_EaseSearching_Tooltip;
	public static String		Search_Options_Checkbox_ShowContentAll;
	public static String		Search_Options_Checkbox_ShowContentTour;
	public static String		Search_Options_Checkbox_ShowContentMarker;
	public static String		Search_Options_Checkbox_ShowContentWaypoint;
	public static String		Search_Options_Checkbox_ShowDate;
	public static String		Search_Options_Checkbox_ShowTime;
	public static String		Search_Options_Checkbox_ShowDescription;
	public static String		Search_Options_Checkbox_ShowItemNumber;
	public static String		Search_Options_Checkbox_ShowLuceneDocId;
	public static String		Search_Options_Dialog_Header;
	public static String		Search_Options_Group_Content;
	public static String		Search_Options_Group_Sorting;
	public static String		Search_Options_Group_Result;
	public static String		Search_Options_Label_SortAscending;
	public static String		Search_Options_Radio_SortAscending;
	public static String		Search_Options_Radio_SortDescending;

	public static String		Search_Validation_SearchFilter;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {}
}
