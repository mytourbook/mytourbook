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

   public static String        Statistic_Week_Tooltip_ColumnHeaderTitle;
   public static String        Statistic_Week_Tooltip_Title;

   static {
      // initialize resource bundle
      NLS.initializeMessages(BUNDLE_NAME, Messages.class);
   }

   private Messages() {}
}
