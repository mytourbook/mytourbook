/*******************************************************************************
 * Copyright (C) 2023, 2025 Wolfgang Schramm and Contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.OtherMessages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.common.widgets.ComboEnumEntry;
import net.tourbook.data.DeviceSensor;
import net.tourbook.data.DeviceSensorType;
import net.tourbook.database.TourDatabase;

/**
 * Manage device sensors
 */
public class SensorManager {

   private static final char               NL = UI.NEW_LINE;

   public static final ComboEnumEntry<?>[] ALL_SENSOR_TYPES;

   static {

// SET_FORMATTING_OFF

      ALL_SENSOR_TYPES = new ComboEnumEntry<?>[] {

            new ComboEnumEntry<>(UI.EMPTY_STRING,                       DeviceSensorType.NONE),
            new ComboEnumEntry<>(Messages.Sensor_Type_RecordingDevice,  DeviceSensorType.RECORDING_DEVICE),

            new ComboEnumEntry<>(OtherMessages.GRAPH_LABEL_CADENCE,     DeviceSensorType.CADENCE),
            new ComboEnumEntry<>(OtherMessages.GRAPH_LABEL_SPEED,       DeviceSensorType.SPEED),
            new ComboEnumEntry<>(OtherMessages.GRAPH_LABEL_PACE,        DeviceSensorType.PACE),
            new ComboEnumEntry<>(OtherMessages.GRAPH_LABEL_HEARTBEAT,   DeviceSensorType.HEARTBEAT),
            new ComboEnumEntry<>(Messages.Sensor_Type_GearShifting,     DeviceSensorType.GEAR_SHIFTING),
            new ComboEnumEntry<>(Messages.Sensor_Type_PowerMeter,       DeviceSensorType.POWER_METER),
            new ComboEnumEntry<>(Messages.Sensor_Type_Wind,             DeviceSensorType.WIND),
            new ComboEnumEntry<>(Messages.Sensor_Type_Radar,            DeviceSensorType.RADAR),

            new ComboEnumEntry<>(Messages.Sensor_Type_LightFront,       DeviceSensorType.LIGHT_FRONT),
            new ComboEnumEntry<>(Messages.Sensor_Type_LightRear,        DeviceSensorType.LIGHT_REAR),

            new ComboEnumEntry<>(Messages.Sensor_Type_Other,            DeviceSensorType.OTHER),
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
    *
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

   /**
    * @param deviceSensor
    *
    * @return Returns a list with all tour id's which are containing the device sensor
    */
   public static ArrayList<Long> getToursWithSensor(final DeviceSensor deviceSensor) {

      final ArrayList<Long> allTourIds = new ArrayList<>();

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                          //$NON-NLS-1$
            + "   DISTINCT TOURDATA_TOURID," + NL //                    //$NON-NLS-1$
            + "   DEVICESENSOR_SENSORID" + NL //                        //$NON-NLS-1$

            + "FROM DEVICESENSORVALUE " + NL //                         //$NON-NLS-1$

            + "WHERE DEVICESENSOR_SENSORID = ?" + NL //                 //$NON-NLS-1$
      ;

      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statement = conn.prepareStatement(sql);

         statement.setLong(1, deviceSensor.getSensorId());

         final ResultSet result = statement.executeQuery();

         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         UI.showSQLException(e);

      } finally {

         Util.closeSql(statement);
      }

      return allTourIds;
   }
}
