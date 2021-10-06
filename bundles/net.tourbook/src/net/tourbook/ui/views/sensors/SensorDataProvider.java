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

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.database.TourDatabase;

public class SensorDataProvider {

   private static final char        NL = UI.NEW_LINE;


   /**
    * Retrieve chart data from the database
    *
    * @return
    */
   SensorData getTourTimeData(final long sensorId) {

      String sql = null;

      SensorData sensorData = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final long firstDateTime = TimeTools.toEpochMilli(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
         final long lastDateTime = TimeTools.toEpochMilli(LocalDateTime.of(2100 + 1, 1, 1, 0, 0, 0));

         sql = UI.EMPTY_STRING

               + "SELECT" + NL

               + "   DEVICESENSOR_SENSORID," + NL //                       1  //$NON-NLS-1$
               + "   TourStartTime," + NL //                               2  //$NON-NLS-1$
               + "   BatteryVoltage_Start," + NL //                        3  //$NON-NLS-1$
               + "   BatteryVoltage_End" + NL //                           4  //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_DEVICE_SENSOR_VALUE + NL //     //$NON-NLS-1$

               + "WHERE" + NL //                                              //$NON-NLS-1$

               + "   DEVICESENSOR_SENSORID = ?     AND " + NL //              //$NON-NLS-1$
               + "   TourStartTime >= ?            AND " + NL //              //$NON-NLS-1$
               + "   TourStartTime < ?" + NL //                               //$NON-NLS-1$

               + "ORDER BY TourStartTime" + NL //                             //$NON-NLS-1$
         ;

         final TFloatArrayList allBatteryVoltage = new TFloatArrayList();
         final TIntArrayList allXValues = new TIntArrayList();
         int xValueCounter = 0;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         prepStmt.setLong(1, sensorId);
         prepStmt.setLong(2, firstDateTime);
         prepStmt.setLong(3, lastDateTime);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

// SET_FORMATTING_OFF

            final float dbBatteryVoltage_Start  = result.getFloat(3);
            final float dbBatteryVoltage_End    = result.getFloat(4);

// SET_FORMATTING_ON

            final boolean isStartAvailable = dbBatteryVoltage_Start > 0;
            final boolean isEndAvailable = dbBatteryVoltage_End > 0;

            if (isStartAvailable) {

               allBatteryVoltage.add(dbBatteryVoltage_Start);
               allXValues.add(xValueCounter++);
            }

            if (isEndAvailable) {

               allBatteryVoltage.add(dbBatteryVoltage_End);
               allXValues.add(xValueCounter++);
            }
         }

         // ensure that valid data are set
         if (allXValues.size() == 0) {

            allBatteryVoltage.add(-1);
            allBatteryVoltage.add(1);
            allXValues.add(xValueCounter++);
            allXValues.add(xValueCounter++);
         }

         /*
          * Create external data
          */
         sensorData = new SensorData();

         sensorData.allXValues = allXValues.toArray();
         sensorData.allBatteryVoltage = allBatteryVoltage.toArray();

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return sensorData;
   }

}
