/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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

   private static final String BUNDLE_NAME = "net.tourbook.chart.messages"; //$NON-NLS-1$

   public static String        Action_mouse_mode;
   public static String        Action_mouse_mode_slider;
   public static String        Action_mouse_mode_tooltip;
   public static String        Action_mouse_mode_zoom;

   public static String        Action_Move_LeftSlider_ToStart;
   public static String        Action_Move_RightSlider_ToEnd;
   public static String        Action_move_left_slider_here;
   public static String        Action_move_right_slider_here;
   public static String        Action_move_sliders_to_border;

   public static String        Action_next_month;
   public static String        Action_next_month_tooltip;

   public static String        Action_previous_month;
   public static String        Action_previous_month_tooltip;

   public static String        Action_zoom_fit_to_graph;
   public static String        Action_zoom_fit_to_graph_tooltip;
   public static String        Action_zoom_in;
   public static String        Action_zoom_in_tooltip;
   public static String        Action_zoom_out;
   public static String        Action_zoom_out_tooltip;
   public static String        Action_zoom_to_slider;

   public static String        Error_Message_001_Default;

   public static String        MonthName_Shortcut_04_Apr;
   public static String        MonthName_Shortcut_08_Aug;
   public static String        MonthName_Shortcut_12_Dec;
   public static String        MonthName_Shortcut_02_Feb;
   public static String        MonthName_Shortcut_01_Jan;
   public static String        MonthName_Shortcut_07_Jul;
   public static String        MonthName_Shortcut_06_Jun;
   public static String        MonthName_Shortcut_05_May;
   public static String        MonthName_Shortcut_03_Mar;
   public static String        MonthName_Shortcut_11_Nov;
   public static String        MonthName_Shortcut_10_Oct;
   public static String        MonthName_Shortcut_09_Sep;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
