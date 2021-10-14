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
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.database.TourDatabase;

public class SensorDataProvider {

   private static final char  NL                  = UI.NEW_LINE;

   private static final float START_END_MIN_VALUE = 0.00001f;

   /**
    * Retrieve chart data from the database
    *
    * @return
    */
   SensorData getTourTimeData(final long sensorId) {

      String sql = null;

      SensorData sensorData = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         sql = UI.EMPTY_STRING

               + "SELECT" + NL

               + "   DEVICESENSOR_SensorID," + NL //                       1  //$NON-NLS-1$
               + "   TOURDATA_TourID," + NL //                             2  //$NON-NLS-1$
               + "   TourStartTime," + NL //                               3  //$NON-NLS-1$
               + "   BatteryVoltage_Start," + NL //                        4  //$NON-NLS-1$
               + "   BatteryVoltage_End" + NL //                           5  //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_DEVICE_SENSOR_VALUE + NL //     //$NON-NLS-1$

               + "WHERE" + NL //                                              //$NON-NLS-1$

               + "   DEVICESENSOR_SensorID = ?" + NL //                       //$NON-NLS-1$

               + "ORDER BY TourStartTime" + NL //                             //$NON-NLS-1$
         ;

         final TFloatArrayList allBatteryVoltage_Start = new TFloatArrayList();
         final TFloatArrayList allBatteryVoltage_End = new TFloatArrayList();
         final TIntArrayList allXValues_BySequence = new TIntArrayList();
         final TIntArrayList allXValues_ByTime = new TIntArrayList();
         final TLongArrayList allTourIds = new TLongArrayList();
         final TLongArrayList allTourStartTime = new TLongArrayList();
         long firstDateTime = Long.MIN_VALUE;

         int numXValue = 0;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         prepStmt.setLong(1, sensorId);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

// SET_FORMATTING_OFF

            final long dbTourId                 = result.getLong(2);
            final long dbTourStartTime          = result.getLong(3);
            final float dbBatteryVoltage_Start  = result.getFloat(4);
            final float dbBatteryVoltage_End    = result.getFloat(5);

// SET_FORMATTING_ON

            final boolean isStartAvailable = dbBatteryVoltage_Start > 0;
            final boolean isEndAvailable = dbBatteryVoltage_End > 0;

            if (isStartAvailable && isEndAvailable) {

               /*
                * Ensure that start and end values are not the same, otherwise they are not visible
                * and this can happen when only one of them is available or the start end values
                * have not changed
                */
               float startEndMinDiff = 0;
               if (dbBatteryVoltage_Start - dbBatteryVoltage_End < 0.01) {
                  startEndMinDiff = START_END_MIN_VALUE;
               }

               allBatteryVoltage_Start.add(dbBatteryVoltage_Start + startEndMinDiff);
               allBatteryVoltage_End.add(dbBatteryVoltage_End);

            } else if (isStartAvailable) {

               allBatteryVoltage_Start.add(dbBatteryVoltage_Start + START_END_MIN_VALUE);
               allBatteryVoltage_End.add(dbBatteryVoltage_Start);

            } else if (isEndAvailable) {

               allBatteryVoltage_Start.add(dbBatteryVoltage_End + START_END_MIN_VALUE);
               allBatteryVoltage_End.add(dbBatteryVoltage_End);

            } else {

//               allBatteryVoltage_Start.add(0f);
//               allBatteryVoltage_End.add(0f);
            }

            if (isStartAvailable || isEndAvailable) {

               allXValues_BySequence.add(numXValue++);
               allTourIds.add(dbTourId);
               allTourStartTime.add(dbTourStartTime);

               /*
                * Set x-axis time
                */
               if (firstDateTime == Long.MIN_VALUE) {
                  firstDateTime = dbTourStartTime;
               }

               final long timeDiff = dbTourStartTime - firstDateTime;

               allXValues_ByTime.add((int) (timeDiff / 1000));
            }
         }

         /*
          * Create external data
          */
         sensorData = new SensorData();

         sensorData.firstDateTime = firstDateTime;

         sensorData.allXValues_BySequence = allXValues_BySequence.toArray();
         sensorData.allXValues_ByTime = allXValues_ByTime.toArray();

         sensorData.allBatteryVoltage_Start = allBatteryVoltage_Start.toArray();
         sensorData.allBatteryVoltage_End = allBatteryVoltage_End.toArray();

         sensorData.allTourIds = allTourIds.toArray();
         sensorData.allTourStartTime = allTourStartTime.toArray();

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return sensorData;
   }

}
