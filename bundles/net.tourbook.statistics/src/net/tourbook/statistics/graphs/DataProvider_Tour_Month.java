/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.DurationTime;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;

class DataProvider_Tour_Month extends DataProvider {

   private TourStatisticData_Month _tourMonthData;

   TourStatisticData_Month getMonthData(final TourPerson person,
                                        final TourTypeFilter tourTypeFilter,
                                        final int lastYear,
                                        final int numYears,
                                        final boolean refreshData,
                                        final DurationTime durationTime) {

      /*
       * check if the required data are already loaded
       */
      if (statistic_ActivePerson == person
            && statistic_ActiveTourTypeFilter == tourTypeFilter
            && lastYear == statistic_LastYear
            && numYears == statistic_NumberOfYears
            && refreshData == false) {

         return _tourMonthData;
      }

      // reset cached values
      statistic_RawStatisticValues = null;

      String sql = null;
      int numUsedTourTypes = 0;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statistic_ActivePerson = person;
         statistic_ActiveTourTypeFilter = tourTypeFilter;
         statistic_LastYear = lastYear;
         statistic_NumberOfYears = numYears;

         // get the tour types
         final ArrayList<TourType> allTourTypesList = TourDatabase.getActiveTourTypes();
         final TourType[] allTourTypes = allTourTypesList.toArray(new TourType[allTourTypesList.size()]);

         _tourMonthData = new TourStatisticData_Month();

         String fromTourData;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.ANY_APP_FILTERS);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder(true);

         if (TourTagFilterManager.isTourTagFilterEnabled()) {

            // with tag filter

            fromTourData = NL

                  + "FROM (" + NL //                                                            //$NON-NLS-1$

                  + "   SELECT" + NL //                                                         //$NON-NLS-1$

                  // this is necessary otherwise tours can occur multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                            //$NON-NLS-1$

                  + "      StartYear," + NL //                                                  //$NON-NLS-1$
                  + "      StartMonth," + NL //                                                 //$NON-NLS-1$

                  + "      TourType_TypeId," + NL //                                            //$NON-NLS-1$

                  + "      TourDeviceTime_Elapsed," + NL //                                     //$NON-NLS-1$
                  + "      TourDeviceTime_Recorded," + NL //                                    //$NON-NLS-1$
                  + "      TourDeviceTime_Paused," + NL //                                      //$NON-NLS-1$
                  + "      TourComputedTime_Moving," + NL //                                    //$NON-NLS-1$

                  + "      TourDistance," + NL //                                               //$NON-NLS-1$
                  + "      TourAltUp," + NL //                                                   //$NON-NLS-1$
                  + "      TourAltDown," + NL //                                                   //$NON-NLS-1$

                  + "      BodyWeight,         " + NL //       //$NON-NLS-1$
                  + "      BodyFat          " + NL //       //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                           //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //     //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                //$NON-NLS-1$

                  + "   WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlAppFilter.getWhereClause() + NL //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                               //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            fromTourData = NL

                  + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                             //$NON-NLS-1$

                  + " WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //     //$NON-NLS-1$ //$NON-NLS-2$
                  + sqlAppFilter.getWhereClause()

            ;
         }

         sql = NL +

               "SELECT" + NL //                                               //$NON-NLS-1$

               + "   StartYear," + NL //                                   1  //$NON-NLS-1$
               + "   StartMonth," + NL //                                  2  //$NON-NLS-1$

               + "   TourType_TypeId," + NL //                             3  //$NON-NLS-1$

               + "   SUM(TourDeviceTime_Elapsed)," + NL //                 4  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Recorded)," + NL //                5  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Paused)," + NL //                  6  //$NON-NLS-1$
               + "   SUM(TourComputedTime_Moving)," + NL //                7  //$NON-NLS-1$
               + "   " + createSQL_SumDurationTime(durationTime) + NL //   8  //$NON-NLS-1$

               + "   SUM(TourDistance)," + NL //                           9  //$NON-NLS-1$
               + "   SUM(TourAltUp)," + NL //                              10 //$NON-NLS-1$
               + "   SUM(TourAltDown)," + NL //                            11 //$NON-NLS-1$

               + "   SUM(1)," + NL //                                      12 //$NON-NLS-1$

               + "   AVG( CASE WHEN BodyWeight = 0    THEN NULL ELSE BodyWeight END)," + NL //  13 //$NON-NLS-1$
               + "   AVG( CASE WHEN BodyFat = 0       THEN NULL ELSE BodyFat END)" + NL //      14 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartYear, StartMonth, tourType_typeId" + NL //   //$NON-NLS-1$
               + "ORDER BY StartYear, StartMonth" + NL //                    //$NON-NLS-1$
         ;

         final boolean isShowMultipleTourTypes = tourTypeFilter.containsMultipleTourTypes();

         int numTourTypes = allTourTypes.length;
         numTourTypes = numTourTypes == 0 ? 1 : numTourTypes; // ensure that at least 1 is available

         final int numMonths = 12 * numYears;

         final float[][] dbDistance = new float[numTourTypes][numMonths];
         final float[][] dbNumTours = new float[numTourTypes][numMonths];

         final float[][] dbElevationUp = new float[numTourTypes][numMonths];
         final float[][] dbElevationDown = new float[numTourTypes][numMonths];

         final int[][] dbDurationTime = new int[numTourTypes][numMonths];
         final int[][] dbElapsedTime = new int[numTourTypes][numMonths];
         final int[][] dbRecordedTime = new int[numTourTypes][numMonths];
         final int[][] dbPausedTime = new int[numTourTypes][numMonths];
         final int[][] dbMovingTime = new int[numTourTypes][numMonths];
         final int[][] dbBreakTime = new int[numTourTypes][numMonths];

         final long[][] dbTypeIds = new long[numTourTypes][numMonths];
         final long[] tourTypeSum = new long[numTourTypes];
         final long[] usedTourTypeIds = new long[numTourTypes];

         final float[] allDbBodyWeight = new float[numMonths];
         final float[] allDbBodyFat = new float[numMonths];

         /*
          * Initialize tour types, when there are 0 tours for some years/months, a tour
          * type 0 could be a valid tour type which is the default values for native arrays
          * -> wrong tour type
          */
         Arrays.fill(usedTourTypeIds, TourType.TOUR_TYPE_IS_NOT_USED);
         for (final long[] allTypeIds : dbTypeIds) {
            Arrays.fill(allTypeIds, TourType.TOUR_TYPE_IS_NOT_USED);
         }

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

// SET_FORMATTING_OFF

            final int dbValue_Year                 = result.getInt(1);
            final int dbValue_Month                = result.getInt(2);

            final Long dbValue_TourTypeIdObject    = (Long) result.getObject(3);

            final int dbValue_ElapsedTime          = result.getInt(4);
            final int dbValue_RecordedTime         = result.getInt(5);
            final int dbValue_PausedTime           = result.getInt(6);
            final int dbValue_MovingTime           = result.getInt(7);
            final int dbValue_Duration             = result.getInt(8);

            final long dbValue_Distance            = (long) (result.getInt(9) / UI.UNIT_VALUE_DISTANCE);

            final long dbValue_ElevationUp         = (long) (result.getInt(10) / UI.UNIT_VALUE_ELEVATION);
            final long dbValue_ElevationDown       = (long) (result.getInt(11) / UI.UNIT_VALUE_ELEVATION);

            final int dbValue_NumTours             = result.getInt(12);

            final float dbValue_BodyWeight         = result.getFloat(13) * UI.UNIT_VALUE_WEIGHT;
            final float dbValue_BodyFat            = result.getFloat(14);

// SET_FORMATTING_ON

            final int yearIndex = numYears - (lastYear - dbValue_Year + 1);
            final int monthIndex = (dbValue_Month - 1) + yearIndex * 12;

            /*
             * Convert type id to the type index in the tour types list which is also the color
             * index
             */
            int colorIndex = 0;

            if (dbValue_TourTypeIdObject != null) {
               final long dbTypeId = dbValue_TourTypeIdObject;
               for (int typeIndex = 0; typeIndex < numTourTypes; typeIndex++) {
                  if (dbTypeId == allTourTypes[typeIndex].getTypeId()) {
                     colorIndex = typeIndex;
                     break;
                  }
               }
            }

            final long noTourTypeId = isShowMultipleTourTypes
                  ? TourType.TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA
                  : TourType.TOUR_TYPE_IS_NOT_USED;

            final long typeId = dbValue_TourTypeIdObject == null
                  ? noTourTypeId
                  : dbValue_TourTypeIdObject;

            dbTypeIds[colorIndex][monthIndex] = typeId;
            usedTourTypeIds[colorIndex] = typeId;

            dbDistance[colorIndex][monthIndex] = dbValue_Distance;
            dbDurationTime[colorIndex][monthIndex] = dbValue_Duration;

            dbElevationUp[colorIndex][monthIndex] = dbValue_ElevationUp;
            dbElevationDown[colorIndex][monthIndex] = dbValue_ElevationDown;

            dbElapsedTime[colorIndex][monthIndex] = dbValue_ElapsedTime;
            dbRecordedTime[colorIndex][monthIndex] = dbValue_RecordedTime;
            dbPausedTime[colorIndex][monthIndex] = dbValue_PausedTime;
            dbMovingTime[colorIndex][monthIndex] = dbValue_MovingTime;
            dbBreakTime[colorIndex][monthIndex] = dbValue_ElapsedTime - dbValue_MovingTime;

            dbNumTours[colorIndex][monthIndex] = dbValue_NumTours;

            if (dbValue_BodyWeight > 0) {
               allDbBodyWeight[monthIndex] = dbValue_BodyWeight;
            }
            if (dbValue_BodyFat > 0) {
               allDbBodyFat[monthIndex] = dbValue_BodyFat;
            }

            tourTypeSum[colorIndex] += dbValue_Distance + dbValue_ElevationUp + dbValue_ElapsedTime;

            if (UI.IS_SCRAMBLE_DATA) {

// SET_FORMATTING_OFF

               dbDistance[colorIndex][monthIndex]        = UI.scrambleNumbers(dbDistance[colorIndex][monthIndex]);
               dbDurationTime[colorIndex][monthIndex]    = UI.scrambleNumbers(dbDurationTime[colorIndex][monthIndex]);

               dbElevationUp[colorIndex][monthIndex]     = UI.scrambleNumbers(dbElevationUp[colorIndex][monthIndex]);
               dbElevationDown[colorIndex][monthIndex]   = UI.scrambleNumbers(dbElevationDown[colorIndex][monthIndex]);

               dbElapsedTime[colorIndex][monthIndex]     = UI.scrambleNumbers(dbElapsedTime[colorIndex][monthIndex]);
               dbRecordedTime[colorIndex][monthIndex]    = UI.scrambleNumbers(dbRecordedTime[colorIndex][monthIndex]);
               dbPausedTime[colorIndex][monthIndex]      = UI.scrambleNumbers(dbPausedTime[colorIndex][monthIndex]);
               dbMovingTime[colorIndex][monthIndex]      = UI.scrambleNumbers(dbMovingTime[colorIndex][monthIndex]);
               dbBreakTime[colorIndex][monthIndex]       = UI.scrambleNumbers(dbBreakTime[colorIndex][monthIndex]);

               dbNumTours[colorIndex][monthIndex]        = UI.scrambleNumbers(dbNumTours[colorIndex][monthIndex]);

               allDbBodyWeight[monthIndex]               = UI.scrambleNumbers(allDbBodyWeight[monthIndex]);
               allDbBodyFat[monthIndex]                  = UI.scrambleNumbers(allDbBodyFat[monthIndex]);

               tourTypeSum[colorIndex]                  += UI.scrambleNumbers(dbValue_Distance + dbValue_ElevationUp + dbValue_ElapsedTime);

// SET_FORMATTING_ON
            }
         }

         /*
          * Remove not used tour types
          */
         final ArrayList<Object> typeIdsWithData = new ArrayList<>();

         final ArrayList<Object> distance_WithData = new ArrayList<>();
         final ArrayList<Object> duration_WithData = new ArrayList<>();
         final ArrayList<Object> numTours_WithData = new ArrayList<>();

         final ArrayList<Object> elevationUp_WithData = new ArrayList<>();
         final ArrayList<Object> elevationDown_WithData = new ArrayList<>();

         final ArrayList<Object> elapsedTime_WithData = new ArrayList<>();
         final ArrayList<Object> recordedTime_WithData = new ArrayList<>();
         final ArrayList<Object> pausedTime_WithData = new ArrayList<>();
         final ArrayList<Object> movingTime_WithData = new ArrayList<>();
         final ArrayList<Object> breakTime_WithData = new ArrayList<>();

         for (int tourTypeIndex = 0; tourTypeIndex < tourTypeSum.length; tourTypeIndex++) {

            final long summary = tourTypeSum[tourTypeIndex];

            if (summary > 0) {

               typeIdsWithData.add(dbTypeIds[tourTypeIndex]);

               distance_WithData.add(dbDistance[tourTypeIndex]);
               duration_WithData.add(dbDurationTime[tourTypeIndex]);
               numTours_WithData.add(dbNumTours[tourTypeIndex]);

               elevationUp_WithData.add(dbElevationUp[tourTypeIndex]);
               elevationDown_WithData.add(dbElevationDown[tourTypeIndex]);

               elapsedTime_WithData.add(dbElapsedTime[tourTypeIndex]);
               recordedTime_WithData.add(dbRecordedTime[tourTypeIndex]);
               pausedTime_WithData.add(dbPausedTime[tourTypeIndex]);
               movingTime_WithData.add(dbMovingTime[tourTypeIndex]);
               breakTime_WithData.add(dbBreakTime[tourTypeIndex]);
            }
         }

         /*
          * Create statistic data
          */
         numUsedTourTypes = typeIdsWithData.size();

         if (numUsedTourTypes == 0) {

            // there are NO data -> create dummy data that the UI do not fail

            _tourMonthData.typeIds = new long[1][1];
            _tourMonthData.usedTourTypeIds = new long[] { TourType.TOUR_TYPE_IS_NOT_USED };

            _tourMonthData.elevationUp_Low = new float[1][numMonths];
            _tourMonthData.elevationUp_High = new float[1][numMonths];
            _tourMonthData.elevationDown_Low = new float[1][numMonths];
            _tourMonthData.elevationDown_High = new float[1][numMonths];

            _tourMonthData.distance_Low = new float[1][numMonths];
            _tourMonthData.distance_High = new float[1][numMonths];

            _tourMonthData.setDurationTimeLow(new int[1][numMonths]);
            _tourMonthData.setDurationTimeHigh(new int[1][numMonths]);

            _tourMonthData.elapsedTime = new int[1][numMonths];
            _tourMonthData.recordedTime = new int[1][numMonths];
            _tourMonthData.pausedTime = new int[1][numMonths];
            _tourMonthData.movingTime = new int[1][numMonths];
            _tourMonthData.breakTime = new int[1][numMonths];

            _tourMonthData.numTours_Low = new float[1][numMonths];
            _tourMonthData.numTours_High = new float[1][numMonths];

            _tourMonthData.athleteBodyWeight_Low = new float[numMonths];
            _tourMonthData.athleteBodyWeight_High = new float[numMonths];
            _tourMonthData.athleteBodyFat_Low = new float[numMonths];
            _tourMonthData.athleteBodyFat_High = new float[numMonths];

         } else {

            final long[][] usedTypeIds = new long[numUsedTourTypes][];

            final float[][] usedDistance = new float[numUsedTourTypes][];

            final float[][] usedElevationUp = new float[numUsedTourTypes][];
            final float[][] usedElevationDown = new float[numUsedTourTypes][];

            final float[][] usedNumTours = new float[numUsedTourTypes][];

            final int[][] usedDuration = new int[numUsedTourTypes][];
            final int[][] usedElapsedTime = new int[numUsedTourTypes][];
            final int[][] usedRecordedTime = new int[numUsedTourTypes][];
            final int[][] usedPausedTime = new int[numUsedTourTypes][];
            final int[][] usedMovingTime = new int[numUsedTourTypes][];
            final int[][] usedBreakTime = new int[numUsedTourTypes][];

            for (int index = 0; index < numUsedTourTypes; index++) {

               usedTypeIds[index] = (long[]) typeIdsWithData.get(index);

               usedDistance[index] = (float[]) distance_WithData.get(index);

               usedElevationUp[index] = (float[]) elevationUp_WithData.get(index);
               usedElevationDown[index] = (float[]) elevationDown_WithData.get(index);

               usedDuration[index] = (int[]) duration_WithData.get(index);
               usedElapsedTime[index] = (int[]) elapsedTime_WithData.get(index);
               usedRecordedTime[index] = (int[]) recordedTime_WithData.get(index);
               usedPausedTime[index] = (int[]) pausedTime_WithData.get(index);
               usedMovingTime[index] = (int[]) movingTime_WithData.get(index);
               usedBreakTime[index] = (int[]) breakTime_WithData.get(index);

               usedNumTours[index] = (float[]) numTours_WithData.get(index);
            }

            _tourMonthData.typeIds = usedTypeIds;
            _tourMonthData.usedTourTypeIds = usedTourTypeIds;

            _tourMonthData.elevationUp_Low = new float[numUsedTourTypes][numMonths];
            _tourMonthData.elevationUp_High = usedElevationUp;
            _tourMonthData.elevationDown_Low = new float[numUsedTourTypes][numMonths];
            _tourMonthData.elevationDown_High = usedElevationDown;

            _tourMonthData.distance_Low = new float[numUsedTourTypes][numMonths];
            _tourMonthData.distance_High = usedDistance;

            _tourMonthData.setDurationTimeLow(new int[numUsedTourTypes][numMonths]);
            _tourMonthData.setDurationTimeHigh(usedDuration);

            _tourMonthData.elapsedTime = usedElapsedTime;
            _tourMonthData.recordedTime = usedRecordedTime;
            _tourMonthData.pausedTime = usedPausedTime;
            _tourMonthData.movingTime = usedMovingTime;
            _tourMonthData.breakTime = usedBreakTime;

            _tourMonthData.numTours_Low = new float[numUsedTourTypes][numMonths];
            _tourMonthData.numTours_High = usedNumTours;

            _tourMonthData.athleteBodyWeight_Low = new float[numMonths];
            _tourMonthData.athleteBodyWeight_High = allDbBodyWeight;
            _tourMonthData.athleteBodyFat_Low = new float[numMonths];
            _tourMonthData.athleteBodyFat_High = allDbBodyFat;
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      _tourMonthData.numUsedTourTypes = numUsedTourTypes;

      return _tourMonthData;
   }

   String getRawStatisticValues(final boolean isShowSequenceNumbers) {

      if (_tourMonthData == null) {
         return null;
      }

      if (statistic_RawStatisticValues != null && isShowSequenceNumbers == statistic_isShowSequenceNumbers) {
         return statistic_RawStatisticValues;
      }

      if (_tourMonthData.numUsedTourTypes == 0) {

         // there are no real data -> show info

         return Messages.Tour_StatisticValues_Label_NoData;
      }

      final String headerLine1 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead1() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead1()
            + STAT_VALUE_DATE_MONTH.getHead1()

            + STAT_VALUE_TOUR_TYPE.getHead1()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead1()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead1()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead1()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead1()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead1()

            + STAT_VALUE_MOTION_DISTANCE.withUnitLabel(UI.UNIT_LABEL_DISTANCE).getHead1()
            + STAT_VALUE_ELEVATION_UP.withUnitLabel(UI.UNIT_LABEL_ELEVATION).getHead1()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getHead1()

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead2() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead2()
            + STAT_VALUE_DATE_MONTH.getHead2()

            + STAT_VALUE_TOUR_TYPE.getHead2()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead2()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead2()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead2()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead2()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead2()

            + STAT_VALUE_MOTION_DISTANCE.getHead2()
            + STAT_VALUE_ELEVATION_UP.getHead2()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getHead2()

      ;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getValueFormatting() : VALUE_FORMAT_S)

            + STAT_VALUE_DATE_YEAR.getValueFormatting()
            + STAT_VALUE_DATE_MONTH.getValueFormatting()

            + STAT_VALUE_TOUR_TYPE.getValueFormatting()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getValueFormatting()

            + STAT_VALUE_MOTION_DISTANCE.getValueFormatting()
            + STAT_VALUE_ELEVATION_UP.getValueFormatting()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getValueFormatting()

      ;

      final StringBuilder sb = new StringBuilder();
      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final float[][] numTours = _tourMonthData.numTours_High;
      final int numMonths = numTours[0].length;
      final int firstYear = statistic_LastYear - statistic_NumberOfYears + 1;

      final long[][] allTourTypeIds = _tourMonthData.typeIds;
      final long[] allUsedTourTypeIds = _tourMonthData.usedTourTypeIds;

      int sequenceNumber = 0;

      // loop: all months + years
      for (int monthIndex = 0; monthIndex < numMonths; monthIndex++) {

         final int yearIndex = monthIndex / 12;
         final int year = firstYear + yearIndex;

         final int month = (monthIndex % 12) + 1;

         boolean isMonthData = false;

         // loop: all tour types
         for (int tourTypeIndex = 0; tourTypeIndex < numTours.length; tourTypeIndex++) {

            final long tourTypeId = allTourTypeIds[tourTypeIndex][monthIndex];

            /*
             * Check if this type is used
             */
            String tourTypeName = UI.EMPTY_STRING;

            boolean isDataForTourType = false;

            for (final long usedTourTypeIdValue : allUsedTourTypeIds) {
               if (usedTourTypeIdValue == tourTypeId) {

                  isDataForTourType = usedTourTypeIdValue != TourType.TOUR_TYPE_IS_NOT_USED;

                  tourTypeName = isDataForTourType
                        ? TourDatabase.getTourTypeName(tourTypeId)
                        : UI.EMPTY_STRING;

                  break;
               }
            }

            if (isDataForTourType) {

               isMonthData = true;

               Object sequenceNumberValue = UI.EMPTY_STRING;
               if (isShowSequenceNumbers) {
                  sequenceNumberValue = ++sequenceNumber;
               }

               sb.append(String.format(valueFormatting,

                     sequenceNumberValue,

                     year,
                     month,

                     tourTypeName,

                     _tourMonthData.elapsedTime[tourTypeIndex][monthIndex],
                     _tourMonthData.recordedTime[tourTypeIndex][monthIndex],
                     _tourMonthData.pausedTime[tourTypeIndex][monthIndex],

                     _tourMonthData.movingTime[tourTypeIndex][monthIndex],
                     _tourMonthData.breakTime[tourTypeIndex][monthIndex],

                     _tourMonthData.distance_High[tourTypeIndex][monthIndex] / 1000f,

                     _tourMonthData.elevationUp_High[tourTypeIndex][monthIndex],
                     _tourMonthData.elevationDown_High[tourTypeIndex][monthIndex],

                     _tourMonthData.numTours_High[tourTypeIndex][monthIndex]

               ));

               sb.append(NL);
            }
         }

         // group values
         if (isMonthData) {
            sb.append(NL);
         }
      }

      // cache values
      statistic_RawStatisticValues = sb.toString();
      statistic_isShowSequenceNumbers = isShowSequenceNumbers;

      return statistic_RawStatisticValues;
   }

}
