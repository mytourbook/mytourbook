/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

   private static final String BUNDLE_NAME = "net.tourbook.statistics.messages"; //$NON-NLS-1$

   public static String        LABEL_GRAPH_ALTITUDE;
   public static String        LABEL_GRAPH_BATTERY;
   public static String        LABEL_GRAPH_BODY_FAT;
   public static String        LABEL_GRAPH_BODY_WEIGHT;
   public static String        LABEL_GRAPH_DAYTIME;
   public static String        LABEL_GRAPH_DISTANCE;
   public static String        LABEL_GRAPH_NUMBER_OF_TOURS;
   public static String        LABEL_GRAPH_PACE;
   public static String        LABEL_GRAPH_SPEED;
   public static String        LABEL_GRAPH_TIME;
   public static String        LABEL_GRAPH_TIME_UNIT;
   public static String        LABEL_GRAPH_TRAINING_EFFECT;
   public static String        LABEL_GRAPH_TRAINING_EFFECT_ANAEROBIC;
   public static String        LABEL_GRAPH_TRAINING_PERFORMANCE;

   public static String        NUMBERS_UNIT;

   public static String        Statistic_HrZone_Error_NoHrZoneInPerson;
   public static String        Statistic_HrZone_ErrorNoPerson;

   public static String        Statistic_Label_Invers;

   public static String        Statistic_Value_BatterySoC_End;
   public static String        Statistic_Value_BatterySoC_Start;
   public static String        Statistic_Value_Date_Day_Header1;
   public static String        Statistic_Value_Date_FirstDay_Header1;
   public static String        Statistic_Value_Date_FirstDay_Header2;
   public static String        Statistic_Value_Date_Month_Header1;
   public static String        Statistic_Value_Date_Week_Header1;
   public static String        Statistic_Value_Date_Year_Header1;
   public static String        Statistic_Value_Elevation_ElevationUp_Header1;
   public static String        Statistic_Value_HR_Summary_Header1;
   public static String        Statistic_Value_HR_Zone_1_Header1;
   public static String        Statistic_Value_HR_Zone_10_Header1;
   public static String        Statistic_Value_HR_Zone_2_Header1;
   public static String        Statistic_Value_HR_Zone_3_Header1;
   public static String        Statistic_Value_HR_Zone_4_Header1;
   public static String        Statistic_Value_HR_Zone_5_Header1;
   public static String        Statistic_Value_HR_Zone_6_Header1;
   public static String        Statistic_Value_HR_Zone_7_Header1;
   public static String        Statistic_Value_HR_Zone_8_Header1;
   public static String        Statistic_Value_HR_Zone_9_Header1;
   public static String        Statistic_Value_Motion_Distance_Header1;
   public static String        Statistic_Value_Motion_Pace_Header1;
   public static String        Statistic_Value_Motion_Speed_Header1;
   public static String        Statistic_Value_Time_Computed_Break_Header1;
   public static String        Statistic_Value_Time_Computed_Moving_Header1;
   public static String        Statistic_Value_Time_Device_Elapsed_Header1;
   public static String        Statistic_Value_Time_Device_Paused_Header1;
   public static String        Statistic_Value_Time_Device_Recorded_Header1;
   public static String        Statistic_Value_Tour_NumberOfTours_Header1;
   public static String        Statistic_Value_Tour_Title_Header1;
   public static String        Statistic_Value_Tour_TourType_Header1;
   public static String        Statistic_Value_Training_Aerob_Header2;
   public static String        Statistic_Value_Training_Anaerob_Header2;
   public static String        Statistic_Value_Training_Header1;
   public static String        Statistic_Value_Training_Performance_Header2;

   public static String        Statistic_Week_Tooltip_ColumnHeaderTitle;
   public static String        Statistic_Week_Tooltip_Title;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
