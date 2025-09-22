/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import net.tourbook.common.UI;

/**
 * Is contained in {@link DeviceSensorImport} to keep the battery values
 */
public class DeviceSensorValueImport {

   private static final char NL                   = UI.NEW_LINE;

   /**
    * Tour start time
    */
   public long               tourStartTime;
   public long               tourEndTime;

   /**
    * Battery level: 0...100%
    */
   public short              batteryLevel_Start   = -1;
   public short              batteryLevel_End     = -1;

   /**
    * Defined in {@link com.garmin.fit.BatteryStatus}
    */
   public short              batteryStatus_Start  = -1;
   public short              batteryStatus_End    = -1;

   public float              batteryVoltage_Start = -1;
   public float              batteryVoltage_End   = -1;

   /**
    * Set battery level start and/or end
    *
    * @param batteryLevel
    */
   public void setBattery_Level(final Short batteryLevel) {

      if (batteryLevel == null) {
         return;
      }

      if (batteryLevel_Start == -1) {

         // first set the start value

         batteryLevel_Start = batteryLevel;

      } else {

         batteryLevel_End = batteryLevel;
      }

      /*
       * Ensure that the start value is larger than the end value
       */
      if (batteryLevel_Start != -1 && batteryLevel_End != -1

            && batteryLevel_End > batteryLevel_Start) {

         final short swapValue = batteryLevel_Start;

         batteryLevel_Start = batteryLevel_End;
         batteryLevel_End = swapValue;
      }
   }

   /**
    * Set battery status start and/or end
    *
    * @param batteryStatus
    */
   public void setBattery_Status(final Short batteryStatus) {

      if (batteryStatus == null) {
         return;
      }

      if (batteryStatus_Start == -1) {

         // first set the start value

         batteryStatus_Start = batteryStatus;

      } else {

         batteryStatus_End = batteryStatus;
      }
   }

   /**
    * Set battery voltage start and/or end
    *
    * @param batteryVoltage
    */
   public void setBattery_Voltage(final Float batteryVoltage) {

      if (batteryVoltage == null || batteryVoltage == 0.0) {
         return;
      }

      if (batteryVoltage_Start == -1) {

         // first set the start value

         batteryVoltage_Start = batteryVoltage;

      } else {

         batteryVoltage_End = batteryVoltage;
      }

      /*
       * It happened, that the end value is larger than the start value -> this cannot be possible
       * when riding a tour -> the sensor chart could hide bars because of the wrong min/max values
       */
      if (batteryVoltage_Start != -1 && batteryVoltage_End != -1

            && batteryVoltage_End > batteryVoltage_Start) {

         final float swapValue = batteryVoltage_Start;

         batteryVoltage_Start = batteryVoltage_End;
         batteryVoltage_End = swapValue;
      }
   }

   /**
    * This method is called in the MT UI in the "Tour Data" view
    */
   @Override
   public String toString() {

      return "DeviceSensorValueImport" + NL //                                      //$NON-NLS-1$

            + "   batteryLevel_Start   = " + batteryLevel_Start + NL //             //$NON-NLS-1$
            + "   batteryLevel_End     = " + batteryLevel_End + NL //               //$NON-NLS-1$

            + "   batteryStatus_Start  = " + batteryStatus_Start + NL //            //$NON-NLS-1$
            + "   batteryStatus_End    = " + batteryStatus_End + NL //              //$NON-NLS-1$

            + "   batteryVoltage_Start = " + batteryVoltage_Start + NL //           //$NON-NLS-1$
            + "   batteryVoltage_End   = " + batteryVoltage_End + NL //             //$NON-NLS-1$
      ;
   }
}
