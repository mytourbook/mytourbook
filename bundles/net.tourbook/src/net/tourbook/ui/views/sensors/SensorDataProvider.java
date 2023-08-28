/*******************************************************************************
 * Copyright (C) 2021, 2022 Wolfgang Schramm and Contributors
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
import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.jface.dialogs.IDialogSettings;

public class SensorDataProvider {

   private static final char  NL                  = UI.NEW_LINE;

   private static final float START_END_MIN_VALUE = 0.00001f;

   /**
    * Retrieve chart data from the database
    *
    * @param sensorId
    * @param isUseAppFilter
    * @param state
    * @return
    */
   SensorData getData(final long sensorId, final boolean useTourFilter, final IDialogSettings state) {

// SET_FORMATTING_OFF

      final boolean useAppFilter       = Util.getStateBoolean(state, SlideoutSensorTourFilter.STATE_USE_APP_FILTER,           SlideoutSensorTourFilter.STATE_USE_APP_FILTER_DEFAULT);
      final boolean useDurationFilter  = Util.getStateBoolean(state, SlideoutSensorTourFilter.STATE_USE_DURATION_FILTER,      SlideoutSensorTourFilter.STATE_USE_DURATION_FILTER_DEFAULT);

      final boolean isAppFilter        = useTourFilter && useAppFilter;
      final boolean isDurationFilter   = useTourFilter && useDurationFilter;

// SET_FORMATTING_ON

      final ArrayList<TourType> allTourTypesAsList = TourDatabase.getAllTourTypes();
      final TourType[] allTourTypes = allTourTypesAsList.toArray(new TourType[allTourTypesAsList.size()]);

      String sql = null;

      SensorData sensorData = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         String sqlAppFilter = UI.EMPTY_STRING;
         String sqlDurationFilter = UI.EMPTY_STRING;

         /*
          * Setup app filter
          */
         final SQLFilter appFilter = new SQLFilter(SQLFilter.ONLY_FAST_APP_FILTERS);
         if (isAppFilter) {

            final String sqlTourIds = UI.EMPTY_STRING

                  + "SELECT" + NL //                                          //$NON-NLS-1$

                  + " TourId" + NL //                                         //$NON-NLS-1$
                  + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //           //$NON-NLS-1$

                  + " WHERE 1=1 " //                                          //$NON-NLS-1$
                  + "   " + appFilter.getWhereClause() + NL//                 //$NON-NLS-1$
            ;

            sqlAppFilter = " AND TOURDATA_TourID IN (" + sqlTourIds + ")"; // //$NON-NLS-1$ //$NON-NLS-2$
         }

         /*
          * Setup duration filter
          */
         ZonedDateTime durationFilter_DateTime = null;
         if (isDurationFilter) {

            sqlDurationFilter = " AND DeviceSensorValue.TourStartTime >= ?"; //        //$NON-NLS-1$

            durationFilter_DateTime = getDurationFilter_DateTime(state);
         }

         sql = NL

               + "SELECT" + NL //                                                      //$NON-NLS-1$

               + "   DeviceSensorValue.DEVICESENSOR_SensorID," + NL //              1  //$NON-NLS-1$
               + "   DeviceSensorValue.TOURDATA_TourID," + NL //                    2  //$NON-NLS-1$
               + "   DeviceSensorValue.TourStartTime," + NL //                      3  //$NON-NLS-1$
               + "   DeviceSensorValue.TourEndTime," + NL //                        4  //$NON-NLS-1$
               + "   DeviceSensorValue.BatteryLevel_Start," + NL //                 5  //$NON-NLS-1$
               + "   DeviceSensorValue.BatteryLevel_End," + NL //                   6  //$NON-NLS-1$
               + "   DeviceSensorValue.BatteryStatus_Start," + NL //                7  //$NON-NLS-1$
               + "   DeviceSensorValue.BatteryStatus_End," + NL //                  8  //$NON-NLS-1$
               + "   DeviceSensorValue.BatteryVoltage_Start," + NL //               9  //$NON-NLS-1$
               + "   DeviceSensorValue.BatteryVoltage_End," + NL //                 10 //$NON-NLS-1$

               + "   TourData.tourType_typeId" + NL //                              11 //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_DEVICE_SENSOR_VALUE + NL //              //$NON-NLS-1$

               // get data for a tour
               + "LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + NL //             //$NON-NLS-1$
               + "   ON DeviceSensorValue.TOURDATA_TourID = TourData.tourId" + NL //   //$NON-NLS-1$

               + "WHERE" + NL //                                                       //$NON-NLS-1$

               + "   DEVICESENSOR_SensorID = ?" + NL //                                //$NON-NLS-1$
               + "   " + sqlAppFilter + NL //                                          //$NON-NLS-1$
               + "   " + sqlDurationFilter + NL //                                     //$NON-NLS-1$

               + "ORDER BY DeviceSensorValue.TourStartTime" + NL //                    //$NON-NLS-1$
         ;

         final FloatArrayList allDbBatteryLevel_Start = new FloatArrayList();
         final FloatArrayList allDbBatteryLevel_End = new FloatArrayList();
         final FloatArrayList allDbBatteryStatus_Start = new FloatArrayList();
         final FloatArrayList allDbBatteryStatus_End = new FloatArrayList();
         final FloatArrayList allDbBatteryVoltage_Start = new FloatArrayList();
         final FloatArrayList allDbBatteryVoltage_End = new FloatArrayList();

         final LongArrayList allDbTourIds = new LongArrayList();
         final LongArrayList allDbTourStartTime = new LongArrayList();
         final IntArrayList allDbTypeColorIndices = new IntArrayList();

         boolean isAvailable_Level = false;
         boolean isAvailable_Status = false;
         boolean isAvailable_Voltage = false;

         final IntArrayList allDbXValues_ByTime = new IntArrayList();

         long firstDateTime = Long.MIN_VALUE;

         final PreparedStatement stmt = conn.prepareStatement(sql);

         stmt.setLong(1, sensorId);

         int paramIndex = 2;

         if (isAppFilter) {
            appFilter.setParameters(stmt, paramIndex);
            paramIndex = appFilter.getLastParameterIndex();
         }

         if (isDurationFilter) {
            stmt.setLong(paramIndex++, durationFilter_DateTime.toEpochSecond() * 1000);
         }

         final ResultSet result = stmt.executeQuery();
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

            final Object dbTourTypeIdObject     = result.getObject(11);

            // tour duration in seconds
//            final float tourDuration = (dbTourEndTime - dbTourStartTime) / 1000;

            final boolean isAvailable_Level_Start     = dbBatteryLevel_Start >= 0;
            final boolean isAvailable_Level_End       = dbBatteryLevel_End >= 0;
            final boolean isAvailable_Status_Start    = dbBatteryStatus_Start >= 0;
            final boolean isAvailable_Status_End      = dbBatteryStatus_End >= 0;
            final boolean isAvailable_Voltage_Start   = dbBatteryVoltage_Start >= 0;
            final boolean isAvailable_Voltage_End     = dbBatteryVoltage_End >= 0;

            isAvailable_Level    |= isAvailable_Level_Start    || isAvailable_Level_End;
            isAvailable_Status   |= isAvailable_Status_Start   || isAvailable_Status_End;
            isAvailable_Voltage  |= isAvailable_Voltage_Start  || isAvailable_Voltage_End;

// SET_FORMATTING_ON

            final boolean isAvailable_Start = isAvailable_Level_Start
                  || isAvailable_Status_Start
                  || isAvailable_Voltage_Start;

            final boolean isAvailable_End = isAvailable_Level_End
                  || isAvailable_Status_End
                  || isAvailable_Voltage_End;

            // level
            setStartEndValues(
                  isAvailable_Start,
                  isAvailable_End,
                  isAvailable_Level_Start,
                  isAvailable_Level_End,
                  dbBatteryLevel_Start,
                  dbBatteryLevel_End,
                  allDbBatteryLevel_Start,
                  allDbBatteryLevel_End);

            // status
            setStartEndValues(
                  isAvailable_Start,
                  isAvailable_End,
                  isAvailable_Status_Start,
                  isAvailable_Status_End,
                  dbBatteryStatus_Start,
                  dbBatteryStatus_End,
                  allDbBatteryStatus_Start,
                  allDbBatteryStatus_End);

            // voltage
            setStartEndValues(
                  isAvailable_Start,
                  isAvailable_End,
                  isAvailable_Voltage_Start,
                  isAvailable_Voltage_End,
                  dbBatteryVoltage_Start,
                  dbBatteryVoltage_End,
                  allDbBatteryVoltage_Start,
                  allDbBatteryVoltage_End);

            if (isAvailable_Start || isAvailable_End) {

               allDbTourIds.add(dbTourId);
               allDbTourStartTime.add(dbTourStartTime);

               /*
                * Set x-axis time
                */
               if (firstDateTime == Long.MIN_VALUE) {
                  firstDateTime = dbTourStartTime;
               }

               final long relativeTimeDiffInMS = dbTourStartTime - firstDateTime;
               final long relativeTimeDiffInSec = relativeTimeDiffInMS / 1000;

               allDbXValues_ByTime.add((int) relativeTimeDiffInSec);

               /*
                * Convert tour type id to the type index in the tour type array, this is also the
                * color index for the tour type
                */
               int colorIndex = 0;
               long dbTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

               if (dbTourTypeIdObject instanceof Long) {

                  dbTypeId = (Long) dbTourTypeIdObject;

                  for (int typeIndex = 0; typeIndex < allTourTypes.length; typeIndex++) {
                     if (allTourTypes[typeIndex].getTypeId() == dbTypeId) {
                        colorIndex = typeIndex;
                        break;
                     }
                  }
               }

               // paint graph with tour type color
               allDbTypeColorIndices.add(colorIndex);
            }
         }

         /*
          * Add time margin values
          */
         final int[] allXValues = allDbXValues_ByTime.toArray();
         final float[] allBatteryLevel_Start = allDbBatteryLevel_Start.toArray();
         final float[] allBatteryLevel_End = allDbBatteryLevel_End.toArray();
         final float[] allBatteryStatus_Start = allDbBatteryStatus_Start.toArray();
         final float[] allBatteryStatus_End = allDbBatteryStatus_End.toArray();
         final float[] allBatteryVoltage_Start = allDbBatteryVoltage_Start.toArray();
         final float[] allBatteryVoltage_End = allDbBatteryVoltage_End.toArray();

         final int numValues = allXValues.length;
         int timeOffset = 0;

         if (numValues > 0) {

            final double timeDuration_Seconds = allXValues[numValues - 1];

            /**
             * Add 2% more time that the first/last values do not stick at the y-axis -> better
             * visible
             * <p>
             * It took me a few days to finally implement this "simple" solution with hopefully no
             * undiscovered side effects in the chart
             */
            double timeOffsetNotRounded = timeDuration_Seconds * 0.02;

            // ensure there is a time margin
            if (timeOffsetNotRounded < 1) {
               timeOffsetNotRounded = 1;
            }

            timeOffset = (int) timeOffsetNotRounded;
         }

         /*
          * Create external data
          */
         sensorData = new SensorData();

// SET_FORMATTING_OFF

         sensorData.firstDateTime            = TimeTools.getZonedDateTime(firstDateTime).minusSeconds(timeOffset);

         sensorData.allXValues_ByTime        = getXData_TimeMarginValues(allXValues, timeOffset);

         sensorData.allBatteryLevel_Start    = getYData_WithTimeMarginValues(allBatteryLevel_Start);
         sensorData.allBatteryLevel_End      = getYData_WithTimeMarginValues(allBatteryLevel_End);
         sensorData.allBatteryStatus_Start   = getYData_WithTimeMarginValues(allBatteryStatus_Start);
         sensorData.allBatteryStatus_End     = getYData_WithTimeMarginValues(allBatteryStatus_End);
         sensorData.allBatteryVoltage_Start  = getYData_WithTimeMarginValues(allBatteryVoltage_Start);
         sensorData.allBatteryVoltage_End    = getYData_WithTimeMarginValues(allBatteryVoltage_End);


         sensorData.isAvailable_Level        = isAvailable_Level;
         sensorData.isAvailable_Status       = isAvailable_Status;
         sensorData.isAvailable_Voltage      = isAvailable_Voltage;

         sensorData.allTourIds               = getTourIDs_WithTimeMarginValues(allDbTourIds.toArray());

         sensorData.allTypeColorIndices      = getYData_WithTimeMarginValues(allDbTypeColorIndices.toArray());

// SET_FORMATTING_ON

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return sensorData;
   }

   /**
    * @param state
    * @return Returns the date/time after which the tours should be retrieved
    */
   private ZonedDateTime getDurationFilter_DateTime(final IDialogSettings state) {

// SET_FORMATTING_OFF

      final boolean useTourFilter_Days    = Util.getStateBoolean(state, SlideoutSensorTourFilter.STATE_USE_TOUR_FILTER_DAYS, SlideoutSensorTourFilter.STATE_USE_TOUR_FILTER_DAYS_DEFAULT);
      final boolean useTourFilter_Months  = Util.getStateBoolean(state, SlideoutSensorTourFilter.STATE_USE_TOUR_FILTER_MONTHS, SlideoutSensorTourFilter.STATE_USE_TOUR_FILTER_MONTHS_DEFAULT);
      final boolean useTourFilter_Years   = Util.getStateBoolean(state, SlideoutSensorTourFilter.STATE_USE_TOUR_FILTER_YEARS, SlideoutSensorTourFilter.STATE_USE_TOUR_FILTER_YEARS_DEFAULT);

// SET_FORMATTING_ON

      ZonedDateTime firstTourStartTime = TimeTools.now();

      if (useTourFilter_Days) {

         final int tourFilterDays = Util.getStateInt(state,
               SlideoutSensorTourFilter.STATE_TOUR_FILTER_DAYS,
               SlideoutSensorTourFilter.STATE_TOUR_FILTER_DAYS_DEFAULT);

         firstTourStartTime = firstTourStartTime.minusDays(tourFilterDays);

      }

      if (useTourFilter_Months) {

         final int tourFilterMonths = Util.getStateInt(state,
               SlideoutSensorTourFilter.STATE_TOUR_FILTER_MONTHS,
               SlideoutSensorTourFilter.STATE_TOUR_FILTER_MONTHS_DEFAULT);

         firstTourStartTime = firstTourStartTime.minusMonths(tourFilterMonths);

      }

      if (useTourFilter_Years) {

         final int tourFilterYears = Util.getStateInt(state,
               SlideoutSensorTourFilter.STATE_TOUR_FILTER_YEARS,
               SlideoutSensorTourFilter.STATE_TOUR_FILTER_YEARS_DEFAULT);

         firstTourStartTime = firstTourStartTime.minusYears(tourFilterYears);
      }

      return firstTourStartTime;
   }

   private long[] getTourIDs_WithTimeMarginValues(final long[] allTourIDs) {

      final int numValues_NoMargin = allTourIDs.length;

      if (numValues_NoMargin == 0) {
         return allTourIDs;
      }

      // add time margin values
      final int numValues_WithMargin = numValues_NoMargin + 2;

      final long[] allValues_WithMargin = new long[numValues_WithMargin];

      // copy db tour id's
      System.arraycopy(allTourIDs, 0, allValues_WithMargin, 1, numValues_NoMargin);

      // create dummy tour id's
      allValues_WithMargin[0] = TourDatabase.ENTITY_IS_NOT_SAVED;
      allValues_WithMargin[numValues_WithMargin - 1] = TourDatabase.ENTITY_IS_NOT_SAVED;

      return allValues_WithMargin;
   }

   private int[] getXData_TimeMarginValues(final int[] allXValues_NoMargin, final int timeMargin) {

      final int numValues_NoMargin = allXValues_NoMargin.length;

      if (numValues_NoMargin == 0) {
         return allXValues_NoMargin;
      }

      // add time margin values
      final int numValues_WithMargin = numValues_NoMargin + 2;

      final int[] allValues_WithTimeMargins = new int[numValues_WithMargin];

      // add time margin to each value
      for (int valueIndex = 0; valueIndex < allXValues_NoMargin.length; valueIndex++) {

         allValues_WithTimeMargins[valueIndex + 1] = allXValues_NoMargin[valueIndex] + timeMargin;
      }

      // set last value, the first value is 0
      allValues_WithTimeMargins[numValues_WithMargin - 1] = allValues_WithTimeMargins[numValues_WithMargin - 2] + timeMargin;

      return allValues_WithTimeMargins;
   }

   private float[] getYData_WithTimeMarginValues(final float[] allValues_NoMargin) {

      final int numValues_NoMargin = allValues_NoMargin.length;

      if (numValues_NoMargin == 0) {
         return allValues_NoMargin;
      }

      // add time margin values
      final int numValues_WithMargin = numValues_NoMargin + 2;

      final float[] allValues_WithMargin = new float[numValues_WithMargin];

      System.arraycopy(allValues_NoMargin, 0, allValues_WithMargin, 1, numValues_NoMargin);

      // set dummy values, these values are skipped when navigated in the bar chart
      allValues_WithMargin[0] = Float.MIN_VALUE;
      allValues_WithMargin[numValues_WithMargin - 1] = Float.MIN_VALUE;

      return allValues_WithMargin;
   }

   private int[] getYData_WithTimeMarginValues(final int[] allValues_NoMargin) {

      final int numValues_NoMargin = allValues_NoMargin.length;

      if (numValues_NoMargin == 0) {
         return allValues_NoMargin;
      }

      // add time margin values
      final int numValues_WithMargin = numValues_NoMargin + 2;

      final int[] allValues_WithMargin = new int[numValues_WithMargin];

      System.arraycopy(allValues_NoMargin, 0, allValues_WithMargin, 1, numValues_NoMargin);

      // set dummy values, these values are skipped when navigated in the bar chart
      allValues_WithMargin[0] = 0;
      allValues_WithMargin[numValues_WithMargin - 1] = 0;

      return allValues_WithMargin;
   }

   private void setStartEndValues(final boolean isAvailable_Start,
                                  final boolean isAvailable_End,

                                  final boolean isAvailableValue_Start,
                                  final boolean isAvailableValue_End,

                                  final float value_Start,
                                  final float value_End,

                                  final FloatArrayList allBatteryValue_Start,
                                  final FloatArrayList allBatteryValue_End) {

      float value_1;
      float value_2;

      if (isAvailableValue_Start && isAvailableValue_End) {

         float startEndMinDiff = 0;
         float startEndDiff = value_Start - value_End;

         // ensure a positive diff
         if (startEndDiff < 0) {
            startEndDiff = -startEndDiff;
         }

         /*
          * Ensure that start and end values are not the same, otherwise they are not visible
          * and this can happen when only one of them is available or the start end values
          * have not changed
          */
         if (startEndDiff < 0.01) {
            startEndMinDiff = START_END_MIN_VALUE;
         }

         value_1 = value_Start + startEndMinDiff;
         value_2 = value_End;

         // ensure start > end, otherwise the chart bar is painted wrong also the min/max values are wrong
         if (value_2 > value_1) {

            final float value_1_Backup = value_1;

            value_1 = value_2;
            value_2 = value_1_Backup;
         }

         allBatteryValue_Start.add(value_1);
         allBatteryValue_End.add(value_2);

      } else if (isAvailableValue_Start) {

         value_1 = value_Start + START_END_MIN_VALUE;
         value_2 = value_Start;

         allBatteryValue_Start.add(value_1);
         allBatteryValue_End.add(value_2);

      } else if (isAvailableValue_End) {

         value_1 = value_End + START_END_MIN_VALUE;
         value_2 = value_End;

         allBatteryValue_Start.add(value_1);
         allBatteryValue_End.add(value_2);

      } else {

         if (isAvailable_Start || isAvailable_End) {

            // add dummy data, that all y-data have the same number of items

            allBatteryValue_Start.add(0);
            allBatteryValue_End.add(0);
         }

      }
   }

}
