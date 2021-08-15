/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

   private static final String BUNDLE_NAME = "tourbook.search.nls.messages";             //$NON-NLS-1$

   public static String        Search_App_Action_EditMarker;
   public static String        Search_App_Action_EditTour;
   public static String        Search_App_Action_SearchOptions_Tooltip;
   public static String        Search_App_Action_StartSearch_Tooltip;
   public static String        Search_App_Label_Status_Tooltip;
   public static String        Search_App_Text_Search_PlaceHolder;
   public static String        Search_App_Tooltip;

   public static String        Search_Grid_Label_NoDataMessage;

   public static String        Search_Options_Action_RestoreDefaults;
   public static String        Search_Options_Checkbox_EaseSearching;
   public static String        Search_Options_Checkbox_EaseSearching_Tooltip;

   public static String        Search_Options_Checkbox_Search_All;
   public static String        Search_Options_Checkbox_Search_Marker;
   public static String        Search_Options_Checkbox_Search_Marker_Tooltip;
   public static String        Search_Options_Checkbox_Search_Tour_LocationEnd;
   public static String        Search_Options_Checkbox_Search_Tour_LocationEnd_Tooltip;
   public static String        Search_Options_Checkbox_Search_Tour_LocationStart;
   public static String        Search_Options_Checkbox_Search_Tour_LocationStart_Tooltip;
   public static String        Search_Options_Checkbox_Search_Tour;
   public static String        Search_Options_Checkbox_Search_Tour_Tooltip;
   public static String        Search_Options_Checkbox_Search_Tour_Weather;
   public static String        Search_Options_Checkbox_Search_Tour_Weather_Tooltip;
   public static String        Search_Options_Checkbox_Search_Waypoint;
   public static String        Search_Options_Checkbox_Search_Waypoint_Tooltip;

   public static String        Search_Options_Checkbox_Show_Date;
   public static String        Search_Options_Checkbox_Show_Time;
   public static String        Search_Options_Checkbox_Show_Description;
   public static String        Search_Options_Checkbox_Show_ItemNumber;
   public static String        Search_Options_Checkbox_Show_LuceneDocId;

   public static String        Search_Options_Dialog_Header;
   public static String        Search_Options_Group_Content;
   public static String        Search_Options_Group_Sorting;
   public static String        Search_Options_Group_Result;
   public static String        Search_Options_Label_Sort_ByDate;
   public static String        Search_Options_Radio_Sort_ByDate_Ascending;
   public static String        Search_Options_Radio_Sort_ByDate_Descending;

   public static String        Search_Validation_SearchFilter;

   static {

      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
