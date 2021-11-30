/*******************************************************************************
 * Copyright (C) 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.sensors;

import java.time.ZonedDateTime;
import java.util.Arrays;

import net.tourbook.common.UI;

public class SensorData {

   private static final char NL = UI.NEW_LINE;

   public ZonedDateTime      firstDateTime;

   public long[]             allTourIds;
   public int[]              allTypeColorIndices;

   /**
    * Relative time values in seconds, starting from 0
    */
   public int[]              allXValues_ByTime;

   public float[]            allBatteryLevel_Start;
   public float[]            allBatteryLevel_End;

   public float[]            allBatteryStatus_Start;
   public float[]            allBatteryStatus_End;

   public float[]            allBatteryVoltage_Start;
   public float[]            allBatteryVoltage_End;

   public boolean            isAvailable_Level;
   public boolean            isAvailable_Status;
   public boolean            isAvailable_Voltage;

   @Override
   public String toString() {

      return "SensorData" + NL //$NON-NLS-1$
            + "isAvailable_Level=" + isAvailable_Level + NL //$NON-NLS-1$
            + "isAvailable_Status=" + isAvailable_Status + NL //$NON-NLS-1$
            + "isAvailable_Voltage=" + isAvailable_Voltage + NL //$NON-NLS-1$
            + "firstDateTime=" + firstDateTime + NL //$NON-NLS-1$
            + "allTourIds=" + Arrays.toString(allTourIds) + NL //$NON-NLS-1$
            + "allXValues_ByTime=" + Arrays.toString(allXValues_ByTime) + NL //$NON-NLS-1$
            + "allBatteryLevel_Start=" + Arrays.toString(allBatteryLevel_Start) + NL //$NON-NLS-1$
            + "allBatteryLevel_End=" + Arrays.toString(allBatteryLevel_End) + NL //$NON-NLS-1$
            + "allBatteryStatus_Start=" + Arrays.toString(allBatteryStatus_Start) + NL //$NON-NLS-1$
            + "allBatteryStatus_End=" + Arrays.toString(allBatteryStatus_End) + NL //$NON-NLS-1$
            + "allBatteryVoltage_Start=" + Arrays.toString(allBatteryVoltage_Start) + NL //$NON-NLS-1$
            + "allBatteryVoltage_End=" + Arrays.toString(allBatteryVoltage_End) + NL //$NON-NLS-1$
            + "]" //$NON-NLS-1$
      ;
   }
}
