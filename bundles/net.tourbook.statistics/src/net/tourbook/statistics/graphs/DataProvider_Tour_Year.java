/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
import java.util.OptionalDouble;

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

public class DataProvider_Tour_Year extends DataProvider {

   private TourStatisticData_Year _tourYearData;

   String getRawStatisticValues(final boolean isShowSequenceNumbers) {

      if (_tourYearData == null) {
         return null;
      }

      if (statistic_RawStatisticValues != null && isShowSequenceNumbers == statistic_isShowSequenceNumbers) {
         return statistic_RawStatisticValues;
      }

      if (_tourYearData.numUsedTourTypes == 0) {

         // there are no real data -> show info

         return Messages.Tour_StatisticValues_Label_NoData;
      }

      final StringBuilder sb = new StringBuilder();

      final String headerLine1 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead1() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead1()

            + STAT_VALUE_TOUR_TYPE.getHead1()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead1()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead1()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead1()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead1()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead1()

            + STAT_VALUE_MOTION_DISTANCE.withUnitLabel(UI.UNIT_LABEL_DISTANCE).getHead1()

            + STAT_VALUE_ELEVATION_UP.withUnitLabel(UI.UNIT_LABEL_ELEVATION).getHead1()
            + STAT_VALUE_ELEVATION_DOWN.withUnitLabel(UI.UNIT_LABEL_ELEVATION).getHead1()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getHead1()

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead2() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead2()

            + STAT_VALUE_TOUR_TYPE.getHead2()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getHead2()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getHead2()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getHead2()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getHead2()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getHead2()

            + STAT_VALUE_MOTION_DISTANCE.getHead2()

            + STAT_VALUE_ELEVATION_UP.getHead2()
            + STAT_VALUE_ELEVATION_DOWN.getHead2()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getHead2()

      ;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getValueFormatting() : VALUE_FORMAT_S)

            + STAT_VALUE_DATE_YEAR.getValueFormatting()

            + STAT_VALUE_TOUR_TYPE.getValueFormatting()

            + STAT_VALUE_TIME_DEVICE_ELAPSED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_RECORDED.getValueFormatting()
            + STAT_VALUE_TIME_DEVICE_PAUSED.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_MOVING.getValueFormatting()
            + STAT_VALUE_TIME_COMPUTED_BREAK.getValueFormatting()

            + STAT_VALUE_MOTION_DISTANCE.getValueFormatting()

            + STAT_VALUE_ELEVATION_UP.getValueFormatting()
            + STAT_VALUE_ELEVATION_DOWN.getValueFormatting()

            + STAT_VALUE_TOUR_NUMBER_OF_TOURS.getValueFormatting()

      ;

      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final float[][] numTours = _tourYearData.numTours_High;
      final int numYears = numTours[0].length;
      final int firstYear = statistic_LastYear - statistic_NumberOfYears + 1;

      final long[][] allTourTypeIds = _tourYearData.typeIds;
      final long[] allUsedTourTypeIds = _tourYearData.usedTourTypeIds;

      int sequenceNumber = 0;

      // loop: all years
      for (int yearIndex = 0; yearIndex < numYears; yearIndex++) {

         final int currentYear = firstYear + yearIndex;

         boolean isYearData = false;

         // loop: all tour types
         for (int tourTypeIndex = 0; tourTypeIndex < numTours.length; tourTypeIndex++) {

            final long currentTourTypeId = allTourTypeIds[tourTypeIndex][yearIndex];

            /*
             * Check if this type is used
             */
            String tourTypeName = UI.EMPTY_STRING;

            boolean isDataForTourType = false;

            for (final long usedTourTypeIdValue : allUsedTourTypeIds) {
               if (usedTourTypeIdValue == currentTourTypeId) {

                  isDataForTourType = usedTourTypeIdValue != TourType.TOUR_TYPE_IS_NOT_USED;

                  tourTypeName = isDataForTourType
                        ? TourDatabase.getTourTypeName(currentTourTypeId)
                        : UI.EMPTY_STRING;

                  break;
               }
            }

            if (isDataForTourType) {

               isYearData = true;

               Object sequenceNumberValue = UI.EMPTY_STRING;
               if (isShowSequenceNumbers) {
                  sequenceNumberValue = ++sequenceNumber;
               }

               sb.append(String.format(valueFormatting,

                     sequenceNumberValue,

                     currentYear,

                     tourTypeName,

                     _tourYearData.elapsedTime[tourTypeIndex][yearIndex],
                     _tourYearData.recordedTime[tourTypeIndex][yearIndex],
                     _tourYearData.pausedTime[tourTypeIndex][yearIndex],

                     _tourYearData.movingTime[tourTypeIndex][yearIndex],
                     _tourYearData.breakTime[tourTypeIndex][yearIndex],

                     _tourYearData.distance_High[tourTypeIndex][yearIndex] / 1000,

                     _tourYearData.elevationUp_High[tourTypeIndex][yearIndex],
                     _tourYearData.elevationDown_High[tourTypeIndex][yearIndex],

                     _tourYearData.numTours_High[tourTypeIndex][yearIndex]

               ));

               sb.append(NL);
            }
         }

         // group values
         if (isYearData) {
            sb.append(NL);
         }
      }

      // cache values
      statistic_RawStatisticValues = sb.toString();
      statistic_isShowSequenceNumbers = isShowSequenceNumbers;

      return statistic_RawStatisticValues;
   }

   TourStatisticData_Year getYearData(final TourPerson person,
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

         return _tourYearData;
      }

      // reset cached values
      statistic_RawStatisticValues = null;

      String sql = null;

      try (final Connection conn = TourDatabase.getInstance().getConnection()) {

         statistic_ActivePerson = person;
         statistic_ActiveTourTypeFilter = tourTypeFilter;
         statistic_LastYear = lastYear;
         statistic_NumberOfYears = numYears;

         // get the tour types
         final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
         final TourType[] allTourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

         _tourYearData = new TourStatisticData_Year();

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

                  + "      TourType_TypeId," + NL //                                            //$NON-NLS-1$

                  + "      TourDeviceTime_Elapsed," + NL //                                     //$NON-NLS-1$
                  + "      TourDeviceTime_Recorded," + NL //                                    //$NON-NLS-1$
                  + "      TourDeviceTime_Paused," + NL //                                      //$NON-NLS-1$
                  + "      TourComputedTime_Moving," + NL //                                    //$NON-NLS-1$

                  + "      TourDistance," + NL //                                               //$NON-NLS-1$
                  + "      TourAltUp," + NL //                                                  //$NON-NLS-1$
                  + "      TourAltDown," + NL //                                                //$NON-NLS-1$

                  + "      BodyWeight,         " + NL //       //$NON-NLS-1$
                  + "      BodyFat          " + NL //       //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                           //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //     //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                //$NON-NLS-1$

                  + "   WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                               //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            fromTourData = NL

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                              //$NON-NLS-1$

                  + "WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

            ;
         }

         sql = NL +

               "SELECT" + NL //                                               //$NON-NLS-1$

               + "   StartYear," + NL //                                   1  //$NON-NLS-1$

               + "   TourType_TypeId," + NL //                             2  //$NON-NLS-1$

               + "   SUM(TourDeviceTime_Elapsed)," + NL //                 3  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Recorded)," + NL //                4  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Paused)," + NL //                  5  //$NON-NLS-1$
               + "   SUM(TourComputedTime_Moving)," + NL //                6  //$NON-NLS-1$
               + "   " + createSQL_SumDurationTime(durationTime) + NL //   7  //$NON-NLS-1$

               + "   SUM(TourDistance)," + NL //                           8  //$NON-NLS-1$
               + "   SUM(TourAltUp)," + NL //                              9  //$NON-NLS-1$
               + "   SUM(TourAltDown)," + NL //                            10 //$NON-NLS-1$

               + "   SUM(1)," + NL //                                      11 //$NON-NLS-1$

               + "   AVG( CASE WHEN BodyWeight = 0    THEN NULL ELSE BodyWeight END)," + NL //  12 //$NON-NLS-1$
               + "   AVG( CASE WHEN BodyFat = 0       THEN NULL ELSE BodyFat END)" + NL //      13 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartYear, tourType_typeId " + NL //               //$NON-NLS-1$
               + "ORDER BY StartYear" + NL //                                 //$NON-NLS-1$
         ;

         final boolean isShowMultipleTourTypes = tourTypeFilter.containsMultipleTourTypes();

         int numTourTypes = allTourTypes.length;
         numTourTypes = numTourTypes == 0 ? 1 : numTourTypes; // ensure that at least 1 is available

         final float[][] dbDistance = new float[numTourTypes][numYears];
         final float[][] dbElevationUp = new float[numTourTypes][numYears];
         final float[][] dbElevationDown = new float[numTourTypes][numYears];
         final float[][] dbNumTours = new float[numTourTypes][numYears];

         @SuppressWarnings("unchecked")
         final ArrayList<Float>[] dbBodyWeight = new ArrayList[numYears];
         @SuppressWarnings("unchecked")
         final ArrayList<Float>[] dbBodyFat = new ArrayList[numYears];

         // initializing
         for (int index = 0; index < numYears; index++) {
            dbBodyWeight[index] = new ArrayList<>();
            dbBodyFat[index] = new ArrayList<>();
         }

         final int[][] dbDurationTime = new int[numTourTypes][numYears];
         final int[][] dbElapsedTime = new int[numTourTypes][numYears];
         final int[][] dbRecordedTime = new int[numTourTypes][numYears];
         final int[][] dbPausedTime = new int[numTourTypes][numYears];
         final int[][] dbMovingTime = new int[numTourTypes][numYears];
         final int[][] dbBreakTime = new int[numTourTypes][numYears];

         final long[][] dbTypeIds = new long[numTourTypes][numYears];
         final long[] tourTypeSum = new long[numTourTypes];
         final long[] usedTourTypeIds = new long[numTourTypes];

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

            final int dbValue_ResultYear           = result.getInt(1);

            final Long dbValue_TourTypeIdObject    = (Long) result.getObject(2);

            final int dbValue_ElapsedTime          = result.getInt(3);
            final int dbValue_RecordedTime         = result.getInt(4);
            final int dbValue_PausedTime           = result.getInt(5);
            final int dbValue_MovingTime           = result.getInt(6);
            final int dbValue_Duration             = result.getInt(7);

            final long dbValue_Distance            = (long) (result.getInt(8) / UI.UNIT_VALUE_DISTANCE);

            final long dbValue_ElevationUp         = (long) (result.getInt(9) / UI.UNIT_VALUE_ELEVATION);
            final long dbValue_ElevationDown       = (long) (result.getInt(10) / UI.UNIT_VALUE_ELEVATION);

            final int dbValue_NumTours             = result.getInt(11);
            final float dbValue_BodyWeight         = result.getFloat(12) * UI.UNIT_VALUE_WEIGHT;
            final float dbValue_BodyFat            = result.getFloat(13);

// SET_FORMATTING_ON

            final int yearIndex = numYears - (lastYear - dbValue_ResultYear + 1);

            /*
             * convert type id to the type index in the tour types list which is also the color
             * index
             */

            // set default color index
            int colorIndex = 0;

            // get colorIndex from the type id
            if (dbValue_TourTypeIdObject != null) {

               final long dbTypeId = dbValue_TourTypeIdObject;

               for (int typeIndex = 0; typeIndex < allTourTypes.length; typeIndex++) {
                  if (dbTypeId == allTourTypes[typeIndex].getTypeId()) {
                     colorIndex = typeIndex;
                     break;
                  }
               }
            }

            final long noTourTypeId = isShowMultipleTourTypes
                  ? TourType.TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA
                  : TourType.TOUR_TYPE_IS_NOT_USED;

            final long dbTypeId = dbValue_TourTypeIdObject == null ? noTourTypeId : dbValue_TourTypeIdObject;

            dbTypeIds[colorIndex][yearIndex] = dbTypeId;

            dbDistance[colorIndex][yearIndex] = dbValue_Distance;

            dbElevationUp[colorIndex][yearIndex] = dbValue_ElevationUp;
            dbElevationDown[colorIndex][yearIndex] = dbValue_ElevationDown;

            dbNumTours[colorIndex][yearIndex] = dbValue_NumTours;
            if (dbValue_BodyWeight > 0) {
               dbBodyWeight[yearIndex].add(dbValue_BodyWeight);
            }
            if (dbValue_BodyFat > 0) {
               dbBodyFat[yearIndex].add(dbValue_BodyFat);
            }

            dbDurationTime[colorIndex][yearIndex] = dbValue_Duration;

            dbElapsedTime[colorIndex][yearIndex] = dbValue_ElapsedTime;
            dbRecordedTime[colorIndex][yearIndex] = dbValue_RecordedTime;
            dbPausedTime[colorIndex][yearIndex] = dbValue_PausedTime;
            dbMovingTime[colorIndex][yearIndex] = dbValue_MovingTime;
            dbBreakTime[colorIndex][yearIndex] = dbValue_ElapsedTime - dbValue_MovingTime;

            usedTourTypeIds[colorIndex] = dbTypeId;
            tourTypeSum[colorIndex] += dbValue_Distance + dbValue_ElevationUp + dbValue_ElapsedTime;
         }

         final int[] years = new int[statistic_NumberOfYears];
         int yearIndex = 0;
         for (int currentYear = statistic_LastYear - statistic_NumberOfYears + 1; currentYear <= statistic_LastYear; currentYear++) {
            years[yearIndex++] = currentYear;
         }
         _tourYearData.years = years;

         /*
          * Remove not used tour types
          */
         final ArrayList<Object> allTypeIds_WithData = new ArrayList<>();

         final ArrayList<Object> allElevationUp_WithData = new ArrayList<>();
         final ArrayList<Object> allElevationDown_WithData = new ArrayList<>();
         final ArrayList<Object> allDistance_WithData = new ArrayList<>();
         final ArrayList<Object> allDuration_WithData = new ArrayList<>();
         final ArrayList<Object> allNumTours_WithData = new ArrayList<>();

         final ArrayList<Object> allElapsedTime_WithData = new ArrayList<>();
         final ArrayList<Object> allRecordedTime_WithData = new ArrayList<>();
         final ArrayList<Object> allPausedTime_WithData = new ArrayList<>();
         final ArrayList<Object> allMovingTime_WithData = new ArrayList<>();
         final ArrayList<Object> allBreakTime_WithData = new ArrayList<>();

         for (int tourTypeIndex = 0; tourTypeIndex < tourTypeSum.length; tourTypeIndex++) {

            final long summary = tourTypeSum[tourTypeIndex];

            if (summary > 0) {

               allTypeIds_WithData.add(dbTypeIds[tourTypeIndex]);

               allElevationUp_WithData.add(dbElevationUp[tourTypeIndex]);
               allElevationDown_WithData.add(dbElevationDown[tourTypeIndex]);
               allDistance_WithData.add(dbDistance[tourTypeIndex]);
               allDuration_WithData.add(dbDurationTime[tourTypeIndex]);
               allNumTours_WithData.add(dbNumTours[tourTypeIndex]);

               allElapsedTime_WithData.add(dbElapsedTime[tourTypeIndex]);
               allRecordedTime_WithData.add(dbRecordedTime[tourTypeIndex]);
               allPausedTime_WithData.add(dbPausedTime[tourTypeIndex]);
               allMovingTime_WithData.add(dbMovingTime[tourTypeIndex]);
               allBreakTime_WithData.add(dbBreakTime[tourTypeIndex]);
            }
         }

         /*
          * Create statistic data
          */
         final int numTourTypes_WithData = allTypeIds_WithData.size();

         if (numTourTypes_WithData == 0) {

            // there are NO data, create dummy data that the UI do not fail

            _tourYearData.typeIds = new long[1][1];
            _tourYearData.usedTourTypeIds = new long[] { TourType.TOUR_TYPE_IS_NOT_USED };

            _tourYearData.elevationUp_Low = new float[1][numYears];
            _tourYearData.elevationUp_High = new float[1][numYears];
            _tourYearData.elevationDown_Low = new float[1][numYears];
            _tourYearData.elevationDown_High = new float[1][numYears];

            _tourYearData.distance_Low = new float[1][numYears];
            _tourYearData.distance_High = new float[1][numYears];

            _tourYearData.setDurationTimeLow(new int[1][numYears]);
            _tourYearData.setDurationTimeHigh(new int[1][numYears]);

            _tourYearData.elapsedTime = new int[1][numYears];
            _tourYearData.recordedTime = new int[1][numYears];
            _tourYearData.pausedTime = new int[1][numYears];
            _tourYearData.movingTime = new int[1][numYears];
            _tourYearData.breakTime = new int[1][numYears];

            _tourYearData.numTours_Low = new float[1][numYears];
            _tourYearData.numTours_High = new float[1][numYears];

            _tourYearData.athleteBodyWeight_Low = new float[numYears];
            _tourYearData.athleteBodyWeight_High = new float[numYears];
            _tourYearData.athleteBodyFat_Low = new float[numYears];
            _tourYearData.athleteBodyFat_High = new float[numYears];

         } else {

            final long[][] usedTypeIds = new long[numTourTypes_WithData][];

            final float[][] usedElevationUp = new float[numTourTypes_WithData][];
            final float[][] usedElevationDown = new float[numTourTypes_WithData][];
            final float[][] usedDistance = new float[numTourTypes_WithData][];
            final int[][] usedDuration = new int[numTourTypes_WithData][];
            final int[][] usedElapsedTime = new int[numTourTypes_WithData][];
            final int[][] usedRecordedTime = new int[numTourTypes_WithData][];
            final int[][] usedPausedTime = new int[numTourTypes_WithData][];
            final int[][] usedMovingTime = new int[numTourTypes_WithData][];
            final int[][] usedBreakTime = new int[numTourTypes_WithData][];
            final float[][] usedNumTours = new float[numTourTypes_WithData][];

            for (int index = 0; index < numTourTypes_WithData; index++) {

               usedTypeIds[index] = (long[]) allTypeIds_WithData.get(index);

               usedElevationUp[index] = (float[]) allElevationUp_WithData.get(index);
               usedElevationDown[index] = (float[]) allElevationDown_WithData.get(index);
               usedDistance[index] = (float[]) allDistance_WithData.get(index);

               usedDuration[index] = (int[]) allDuration_WithData.get(index);
               usedElapsedTime[index] = (int[]) allElapsedTime_WithData.get(index);
               usedRecordedTime[index] = (int[]) allRecordedTime_WithData.get(index);
               usedPausedTime[index] = (int[]) allPausedTime_WithData.get(index);
               usedMovingTime[index] = (int[]) allMovingTime_WithData.get(index);
               usedBreakTime[index] = (int[]) allBreakTime_WithData.get(index);

               usedNumTours[index] = (float[]) allNumTours_WithData.get(index);
            }

            _tourYearData.typeIds = usedTypeIds;
            _tourYearData.usedTourTypeIds = usedTourTypeIds;

            _tourYearData.elevationUp_Low = new float[numTourTypes_WithData][numYears];
            _tourYearData.elevationUp_High = usedElevationUp;
            _tourYearData.elevationDown_Low = new float[numTourTypes_WithData][numYears];
            _tourYearData.elevationDown_High = usedElevationDown;

            _tourYearData.distance_Low = new float[numTourTypes_WithData][numYears];
            _tourYearData.distance_High = usedDistance;

            _tourYearData.setDurationTimeLow(new int[numTourTypes_WithData][numYears]);
            _tourYearData.setDurationTimeHigh(usedDuration);

            _tourYearData.elapsedTime = usedElapsedTime;
            _tourYearData.recordedTime = usedRecordedTime;
            _tourYearData.pausedTime = usedPausedTime;
            _tourYearData.movingTime = usedMovingTime;
            _tourYearData.breakTime = usedBreakTime;

            _tourYearData.numTours_Low = new float[numTourTypes_WithData][numYears];
            _tourYearData.numTours_High = usedNumTours;

            _tourYearData.athleteBodyWeight_Low = new float[numYears];

            final float[] weight = new float[numYears];
            for (int index = 0; index < numYears; ++index) {
               final OptionalDouble averageDouble = dbBodyWeight[index].stream().mapToDouble(d -> d).average();

               if (averageDouble.isPresent()) {
                  weight[index] = (float) averageDouble.getAsDouble();
               }
            }
            _tourYearData.athleteBodyWeight_High = weight;

            final float[] fat = new float[numYears];
            for (int index = 0; index < numYears; ++index) {
               final OptionalDouble averageDouble = dbBodyFat[index].stream().mapToDouble(d -> d).average();

               if (averageDouble.isPresent()) {
                  fat[index] = (float) averageDouble.getAsDouble();
               }
            }
            _tourYearData.athleteBodyFat_Low = new float[numYears];
            _tourYearData.athleteBodyFat_High = fat;
         }

         _tourYearData.numUsedTourTypes = numTourTypes_WithData;

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourYearData;
   }
}
