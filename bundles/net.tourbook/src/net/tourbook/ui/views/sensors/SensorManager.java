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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.widgets.ComboEnumEntry;
import net.tourbook.data.DeviceSensorType;

/**
 * Manage device sensors
 */
public class SensorManager {

   private static final String             GRAPH_LABEL_CADENCE   = net.tourbook.common.Messages.Graph_Label_Cadence;
   private static final String             GRAPH_LABEL_HEARTBEAT = net.tourbook.common.Messages.Graph_Label_Heartbeat;
   private static final String             GRAPH_LABEL_PACE      = net.tourbook.common.Messages.Graph_Label_Pace;
   private static final String             GRAPH_LABEL_SPEED     = net.tourbook.common.Messages.Graph_Label_Speed;

   public static final ComboEnumEntry<?>[] ALL_SENSOR_TYPES;

   static {

// SET_FORMATTING_OFF

      ALL_SENSOR_TYPES = new ComboEnumEntry<?>[] {

            new ComboEnumEntry<>(UI.EMPTY_STRING,                    DeviceSensorType.NONE),
            new ComboEnumEntry<>(Messages.Sensor_Type_Other,         DeviceSensorType.OTHER),
            new ComboEnumEntry<>(GRAPH_LABEL_CADENCE,                DeviceSensorType.CADENCE),
            new ComboEnumEntry<>(GRAPH_LABEL_SPEED,                  DeviceSensorType.SPEED),
            new ComboEnumEntry<>(GRAPH_LABEL_PACE,                   DeviceSensorType.PACE),
            new ComboEnumEntry<>(GRAPH_LABEL_HEARTBEAT,              DeviceSensorType.HEARTBEAT),
            new ComboEnumEntry<>(Messages.Sensor_Type_GearShifting,  DeviceSensorType.GEAR_SHIFTING),
      };

// SET_FORMATTING_ON
   }

   private SensorManager() {}

   public static int getSensorTypeIndex(final Enum<DeviceSensorType> requestedSensorType) {

      if (requestedSensorType == null) {

         // this case should not happen

         return 0;
      }

      for (int itemIndex = 0; itemIndex < ALL_SENSOR_TYPES.length; itemIndex++) {

         if (ALL_SENSOR_TYPES[itemIndex].value.equals(requestedSensorType)) {
            return itemIndex;
         }
      }

      return 0;
   }

   /**
    * @param requestedSensorType
    * @return Returns a UI name of the sensor type
    */
   public static String getSensorTypeName(final Enum<DeviceSensorType> requestedSensorType) {

      if (requestedSensorType == null) {

         // this case should not happen

         return Messages.App_Label_NotAvailable;
      }

      for (final ComboEnumEntry<?> enumItem : ALL_SENSOR_TYPES) {

         if (enumItem.value.equals(requestedSensorType)) {
            return enumItem.label;
         }
      }

      return Messages.App_Label_NotAvailable;
   }
}
