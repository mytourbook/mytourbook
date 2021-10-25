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
   SensorData getData(final long sensorId) {

      String sql = null;

      SensorData sensorData = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         sql = UI.EMPTY_STRING

               + "SELECT" + NL

               + "   DEVICESENSOR_SensorID," + NL //                       1  //$NON-NLS-1$
               + "   TOURDATA_TourID," + NL //                             2  //$NON-NLS-1$
               + "   TourStartTime," + NL //                               3  //$NON-NLS-1$
               + "   TourEndTime," + NL //                                 4  //$NON-NLS-1$
               + "   BatteryLevel_Start," + NL //                          5  //$NON-NLS-1$
               + "   BatteryLevel_End," + NL //                            6  //$NON-NLS-1$
               + "   BatteryStatus_Start," + NL //                         7  //$NON-NLS-1$
               + "   BatteryStatus_End," + NL //                           8  //$NON-NLS-1$
               + "   BatteryVoltage_Start," + NL //                        9  //$NON-NLS-1$
               + "   BatteryVoltage_End" + NL //                           10 //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_DEVICE_SENSOR_VALUE + NL //     //$NON-NLS-1$

               + "WHERE" + NL //                                              //$NON-NLS-1$

               + "   DEVICESENSOR_SensorID = ?" + NL //                       //$NON-NLS-1$

               + "ORDER BY TourStartTime" + NL //                             //$NON-NLS-1$
         ;

         final TFloatArrayList allBatteryLevel_Start = new TFloatArrayList();
         final TFloatArrayList allBatteryLevel_End = new TFloatArrayList();
         final TFloatArrayList allBatteryStatus_Start = new TFloatArrayList();
         final TFloatArrayList allBatteryStatus_End = new TFloatArrayList();
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
//          final long dbTourEndTime            = result.getLong(4);
            final float dbBatteryLevel_Start    = result.getShort(5);
            final float dbBatteryLevel_End      = result.getShort(6);
            final float dbBatteryStatus_Start   = result.getShort(7);
            final float dbBatteryStatus_End     = result.getShort(8);
            final float dbBatteryVoltage_Start  = result.getFloat(9);
            final float dbBatteryVoltage_End    = result.getFloat(10);


            // tour duration in seconds
//          final float tourDuration = (dbTourEndTime - dbTourStartTime) / 1000;

            final boolean isAvailable_Level_Start     = dbBatteryLevel_Start >= 0;
            final boolean isAvailable_Level_End       = dbBatteryLevel_End >= 0;
            final boolean isAvailable_Status_Start    = dbBatteryStatus_Start >= 0;
            final boolean isAvailable_Status_End      = dbBatteryStatus_End >= 0;
            final boolean isAvailable_Voltage_Start   = dbBatteryVoltage_Start >= 0;
            final boolean isAvailable_Voltage_End     = dbBatteryVoltage_End >= 0;

// SET_FORMATTING_ON

            final boolean isAvailable_Start = isAvailable_Level_Start
                  || isAvailable_Status_Start
                  || isAvailable_Voltage_Start;

            final boolean isAvailable_End = isAvailable_Level_End
                  || isAvailable_Status_End
                  || isAvailable_Voltage_End;

            setStartEndValues(
                  isAvailable_Start,
                  isAvailable_End,
                  isAvailable_Level_Start,
                  isAvailable_Level_End,
                  dbBatteryLevel_Start,
                  dbBatteryLevel_End,
                  allBatteryLevel_Start,
                  allBatteryLevel_End);

            setStartEndValues(
                  isAvailable_Start,
                  isAvailable_End,
                  isAvailable_Status_Start,
                  isAvailable_Status_End,
                  dbBatteryStatus_Start,
                  dbBatteryStatus_End,
                  allBatteryStatus_Start,
                  allBatteryStatus_End);

            setStartEndValues(
                  isAvailable_Start,
                  isAvailable_End,
                  isAvailable_Voltage_Start,
                  isAvailable_Voltage_End,
                  dbBatteryVoltage_Start,
                  dbBatteryVoltage_End,
                  allBatteryVoltage_Start,
                  allBatteryVoltage_End);

            if (isAvailable_Start || isAvailable_End) {

               allXValues_BySequence.add(numXValue++);
               allTourIds.add(dbTourId);
               allTourStartTime.add(dbTourStartTime);

               /*
                * Set x-axis time
                */
               if (firstDateTime == Long.MIN_VALUE) {
                  firstDateTime = dbTourStartTime;
               }

               final long relativeTimeDiffInMS = dbTourStartTime - firstDateTime;

               allXValues_ByTime.add((int) (relativeTimeDiffInMS / 1000));
            }
         }

         /*
          * Create external data
          */
         sensorData = new SensorData();

         sensorData.firstDateTime = firstDateTime;

         sensorData.allXValues_BySequence = allXValues_BySequence.toArray();
         sensorData.allXValues_ByTime = allXValues_ByTime.toArray();

         sensorData.allBatteryLevel_Start = allBatteryLevel_Start.toArray();
         sensorData.allBatteryLevel_End = allBatteryLevel_End.toArray();
         sensorData.allBatteryStatus_Start = allBatteryStatus_Start.toArray();
         sensorData.allBatteryStatus_End = allBatteryStatus_End.toArray();
         sensorData.allBatteryVoltage_Start = allBatteryVoltage_Start.toArray();
         sensorData.allBatteryVoltage_End = allBatteryVoltage_End.toArray();

         sensorData.allTourIds = allTourIds.toArray();
         sensorData.allTourStartTime = allTourStartTime.toArray();

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return sensorData;
   }

   private void setStartEndValues(final boolean isAvailableValue_Start,
                                  final boolean isAvailableValue_End,
                                  final boolean isAvailable_Start,
                                  final boolean isAvailable_End,
                                  final float batteryValue_Start,
                                  final float batteryValue_End,
                                  final TFloatArrayList allBatteryValue_Start,
                                  final TFloatArrayList allBatteryValue_End) {

      if (isAvailableValue_Start && isAvailableValue_End) {

         /*
          * Ensure that start and end values are not the same, otherwise they are not visible
          * and this can happen when only one of them is available or the start end values
          * have not changed
          */
         float startEndMinDiff = 0;
         final float startEndDiff = batteryValue_Start - batteryValue_End;
         if (startEndDiff < 0.01) {
            startEndMinDiff = START_END_MIN_VALUE;
         }

         allBatteryValue_Start.add(batteryValue_Start + startEndMinDiff);
         allBatteryValue_End.add(batteryValue_End);

      } else if (isAvailableValue_Start) {

         allBatteryValue_Start.add(batteryValue_Start + START_END_MIN_VALUE);
         allBatteryValue_End.add(batteryValue_Start);

      } else if (isAvailableValue_End) {

         allBatteryValue_Start.add(batteryValue_End + START_END_MIN_VALUE);
         allBatteryValue_End.add(batteryValue_End);

      } else {

         if (isAvailable_Start || isAvailable_End) {

            // add dummy data, that all y-data have the same number of items

            allBatteryValue_Start.add(-1);
            allBatteryValue_End.add(-1);
         }

      }
   }

}
