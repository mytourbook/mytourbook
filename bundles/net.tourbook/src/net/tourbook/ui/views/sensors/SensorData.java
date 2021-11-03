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

public class SensorData {

   public ZonedDateTime firstDateTime;

   public long[]        allTourIds;

   /**
    * Relative time values in seconds, starting from 0
    */
   public int[]         allXValues_ByTime;

   public float[]       allBatteryLevel_Start;
   public float[]       allBatteryLevel_End;

   public float[]       allBatteryStatus_Start;
   public float[]       allBatteryStatus_End;

   public float[]       allBatteryVoltage_Start;
   public float[]       allBatteryVoltage_End;

   public boolean       isAvailable_Level;
   public boolean       isAvailable_Status;
   public boolean       isAvailable_Voltage;
}
