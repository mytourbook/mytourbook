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

import net.tourbook.Messages;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.DurationTime;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Month extends DataProvider {

   private static DataProvider_Tour_Month _instance;

   private TourData_Month                 _tourMonthData;

   private DataProvider_Tour_Month() {}

   public static DataProvider_Tour_Month getInstance() {

      if (_instance == null) {
         _instance = new DataProvider_Tour_Month();
      }

      return _instance;
   }

   TourData_Month getMonthData(final TourPerson person,
                               final TourTypeFilter tourTypeFilter,
                               final int lastYear,
                               final int numYears,
                               final boolean refreshData,
                               final DurationTime durationTime) {

      /*
       * check if the required data are already loaded
       */
      if (_activePerson == person
            && _activeTourTypeFilter == tourTypeFilter
            && lastYear == _lastYear
            && numYears == _numberOfYears
            && refreshData == false) {

         return _tourMonthData;
      }

      String sql = null;
      int numUsedTourTypes = 0;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         _activePerson = person;
         _activeTourTypeFilter = tourTypeFilter;
         _lastYear = lastYear;
         _numberOfYears = numYears;

         // get the tour types
         final ArrayList<TourType> allTourTypesList = TourDatabase.getActiveTourTypes();
         final TourType[] allTourTypes = allTourTypesList.toArray(new TourType[allTourTypesList.size()]);

         _tourMonthData = new TourData_Month();

         String fromTourData;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

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
                  + "      TourAltUp" + NL //                                                   //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                           //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //     //$NON-NLS-1$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                //$NON-NLS-1$

                  + "   WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlAppFilter.getWhereClause() + NL

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

               + "   SUM(1)" + NL //                                       11 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartYear, StartMonth, tourType_typeId" + NL //   //$NON-NLS-1$
               + "ORDER BY StartYear, StartMonth" + NL //                    //$NON-NLS-1$
         ;

         final boolean isShowNoTourTypes = tourTypeFilter.showUndefinedTourTypes();

         int colorOffset = 0;
         if (isShowNoTourTypes) {
            colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
         }

         int numTourTypes = colorOffset + allTourTypes.length;
         numTourTypes = numTourTypes == 0 ? 1 : numTourTypes; // ensure that at least 1 is available

         final int numMonths = 12 * numYears;

         final float[][] dbAltitude = new float[numTourTypes][numMonths];
         final float[][] dbDistance = new float[numTourTypes][numMonths];
         final float[][] dbNumTours = new float[numTourTypes][numMonths];

         final int[][] dbDurationTime = new int[numTourTypes][numMonths];
         final int[][] dbElapsedTime = new int[numTourTypes][numMonths];
         final int[][] dbRecordedTime = new int[numTourTypes][numMonths];
         final int[][] dbPausedTime = new int[numTourTypes][numMonths];
         final int[][] dbMovingTime = new int[numTourTypes][numMonths];
         final int[][] dbBreakTime = new int[numTourTypes][numMonths];

         final long[][] dbTypeIds = new long[numTourTypes][numMonths];
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

            final int dbValue_Year                 = result.getInt(1);
            final int dbValue_Month                = result.getInt(2);

            final Long dbValue_TourTypeIdObject    = (Long) result.getObject(3);

            final int dbValue_ElapsedTime          = result.getInt(4);
            final int dbValue_RecordedTime         = result.getInt(5);
            final int dbValue_PausedTime           = result.getInt(6);
            final int dbValue_MovingTime           = result.getInt(7);
            final int dbValue_Duration             = result.getInt(8);

            final int dbValue_Distance             = (int) (result.getInt(9) / UI.UNIT_VALUE_DISTANCE);
            final int dbValue_Altitude             = (int) (result.getInt(10) / UI.UNIT_VALUE_ALTITUDE);

            final int dbValue_NumTours             = result.getInt(11);

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
               for (int typeIndex = 0; typeIndex < allTourTypes.length; typeIndex++) {
                  if (dbTypeId == allTourTypes[typeIndex].getTypeId()) {
                     colorIndex = colorOffset + typeIndex;
                     break;
                  }
               }
            }

            final long noTourTypeId = isShowNoTourTypes
                  ? TourType.TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA
                  : TourType.TOUR_TYPE_IS_NOT_USED;

            final long typeId = dbValue_TourTypeIdObject == null
                  ? noTourTypeId
                  : dbValue_TourTypeIdObject;

            dbTypeIds[colorIndex][monthIndex] = typeId;
            usedTourTypeIds[colorIndex] = typeId;

            dbAltitude[colorIndex][monthIndex] = dbValue_Altitude;
            dbDistance[colorIndex][monthIndex] = dbValue_Distance;
            dbDurationTime[colorIndex][monthIndex] = dbValue_Duration;

            dbElapsedTime[colorIndex][monthIndex] = dbValue_ElapsedTime;
            dbRecordedTime[colorIndex][monthIndex] = dbValue_RecordedTime;
            dbPausedTime[colorIndex][monthIndex] = dbValue_PausedTime;
            dbMovingTime[colorIndex][monthIndex] = dbValue_MovingTime;
            dbBreakTime[colorIndex][monthIndex] = dbValue_ElapsedTime - dbValue_MovingTime;

            dbNumTours[colorIndex][monthIndex] = dbValue_NumTours;

            tourTypeSum[colorIndex] += dbValue_Distance + dbValue_Altitude + dbValue_ElapsedTime;

         }

         /*
          * Remove not used tour types
          */
         final ArrayList<Object> typeIdsWithData = new ArrayList<>();

         final ArrayList<Object> altitudeWithData = new ArrayList<>();
         final ArrayList<Object> distanceWithData = new ArrayList<>();
         final ArrayList<Object> durationWithData = new ArrayList<>();
         final ArrayList<Object> numToursWithData = new ArrayList<>();

         final ArrayList<Object> elapsedTimeWithData = new ArrayList<>();
         final ArrayList<Object> recordedTimeWithData = new ArrayList<>();
         final ArrayList<Object> pausedTimeWithData = new ArrayList<>();
         final ArrayList<Object> movingTimeWithData = new ArrayList<>();
         final ArrayList<Object> breakTimeWithData = new ArrayList<>();

         for (int tourTypeIndex = 0; tourTypeIndex < tourTypeSum.length; tourTypeIndex++) {

            final long summary = tourTypeSum[tourTypeIndex];

            if (summary > 0) {

               typeIdsWithData.add(dbTypeIds[tourTypeIndex]);

               altitudeWithData.add(dbAltitude[tourTypeIndex]);
               distanceWithData.add(dbDistance[tourTypeIndex]);
               durationWithData.add(dbDurationTime[tourTypeIndex]);
               numToursWithData.add(dbNumTours[tourTypeIndex]);

               elapsedTimeWithData.add(dbElapsedTime[tourTypeIndex]);
               recordedTimeWithData.add(dbRecordedTime[tourTypeIndex]);
               pausedTimeWithData.add(dbPausedTime[tourTypeIndex]);
               movingTimeWithData.add(dbMovingTime[tourTypeIndex]);
               breakTimeWithData.add(dbBreakTime[tourTypeIndex]);
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

            _tourMonthData.altitudeLow = new float[1][numMonths];
            _tourMonthData.altitudeHigh = new float[1][numMonths];

            _tourMonthData.distanceLow = new float[1][numMonths];
            _tourMonthData.distanceHigh = new float[1][numMonths];

            _tourMonthData.setDurationTimeLow(new int[1][numMonths]);
            _tourMonthData.setDurationTimeHigh(new int[1][numMonths]);

            _tourMonthData.elapsedTime = new int[1][numMonths];
            _tourMonthData.recordedTime = new int[1][numMonths];
            _tourMonthData.pausedTime = new int[1][numMonths];
            _tourMonthData.movingTime = new int[1][numMonths];
            _tourMonthData.breakTime = new int[1][numMonths];

            _tourMonthData.numToursLow = new float[1][numMonths];
            _tourMonthData.numToursHigh = new float[1][numMonths];

         } else {

            final long[][] usedTypeIds = new long[numUsedTourTypes][];

            final float[][] usedAltitude = new float[numUsedTourTypes][];
            final float[][] usedDistance = new float[numUsedTourTypes][];

            final float[][] usedNumTours = new float[numUsedTourTypes][];

            final int[][] usedDuration = new int[numUsedTourTypes][];
            final int[][] usedElapsedTime = new int[numUsedTourTypes][];
            final int[][] usedRecordedTime = new int[numUsedTourTypes][];
            final int[][] usedPausedTime = new int[numUsedTourTypes][];
            final int[][] usedMovingTime = new int[numUsedTourTypes][];
            final int[][] usedBreakTime = new int[numUsedTourTypes][];

            for (int index = 0; index < numUsedTourTypes; index++) {

               usedTypeIds[index] = (long[]) typeIdsWithData.get(index);

               usedAltitude[index] = (float[]) altitudeWithData.get(index);
               usedDistance[index] = (float[]) distanceWithData.get(index);

               usedDuration[index] = (int[]) durationWithData.get(index);
               usedElapsedTime[index] = (int[]) elapsedTimeWithData.get(index);
               usedRecordedTime[index] = (int[]) recordedTimeWithData.get(index);
               usedPausedTime[index] = (int[]) pausedTimeWithData.get(index);
               usedMovingTime[index] = (int[]) movingTimeWithData.get(index);
               usedBreakTime[index] = (int[]) breakTimeWithData.get(index);

               usedNumTours[index] = (float[]) numToursWithData.get(index);
            }

            _tourMonthData.typeIds = usedTypeIds;
            _tourMonthData.usedTourTypeIds = usedTourTypeIds;

            _tourMonthData.altitudeLow = new float[numUsedTourTypes][numMonths];
            _tourMonthData.altitudeHigh = usedAltitude;

            _tourMonthData.distanceLow = new float[numUsedTourTypes][numMonths];
            _tourMonthData.distanceHigh = usedDistance;

            _tourMonthData.setDurationTimeLow(new int[numUsedTourTypes][numMonths]);
            _tourMonthData.setDurationTimeHigh(usedDuration);

            _tourMonthData.elapsedTime = usedElapsedTime;
            _tourMonthData.recordedTime = usedRecordedTime;
            _tourMonthData.pausedTime = usedPausedTime;
            _tourMonthData.movingTime = usedMovingTime;
            _tourMonthData.breakTime = usedBreakTime;

            _tourMonthData.numToursLow = new float[numUsedTourTypes][numMonths];
            _tourMonthData.numToursHigh = usedNumTours;
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      setStatisticValues(numUsedTourTypes);

      return _tourMonthData;
   }

   private void setStatisticValues(final int numUsedTourTypes) {

      if (numUsedTourTypes == 0) {

         // there are no real data -> show info

         _tourMonthData.statisticValuesRaw = Messages.Tour_StatisticValues_Label_NoData;

         return;
      }

      final long[][] allTourTypeIds = _tourMonthData.typeIds;
      final long[] allUsedTourTypeIds = _tourMonthData.usedTourTypeIds;

      final StringBuilder sb = new StringBuilder();

      final String headerLine1 = "Year, Month,  Tour Type,            Elapsed, Recorded, Paused, Moving,  Break, Elevation,    Distance, Tours"; //$NON-NLS-1$
      final String headerLine2 = "    ,      ,  ,                         (s),      (s),    (s),    (s),    (s),       (m),         (m),   (#), "; //$NON-NLS-1$

      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final String valueFormatting = UI.EMPTY_STRING

            // date
            + "%4d,   %3d," //$NON-NLS-1$

            // tour type
            + "  %-20s,"//$NON-NLS-1$

            // device time
            + "  %6d,   %6d, %6d," //$NON-NLS-1$

            // computed time
            + " %6d, %6d," //$NON-NLS-1$

            // elevation/distance
            + "    %6.0f,   %9.0f," //$NON-NLS-1$

            // #tours
            + "  %4.0f" //$NON-NLS-1$

            + NL;

      final float[][] numTours = _tourMonthData.numToursHigh;
      final int numMonths = numTours[0].length;
      final int firstYear = _lastYear - _numberOfYears + 1;

      // loop: all months + years
      for (int monthIndex = 0; monthIndex < numMonths; monthIndex++) {

         final int yearIndex = monthIndex / 12;
         final int year = firstYear + yearIndex;

         final int month = (monthIndex % 12) + 1;

         boolean isDataInTourType = false;

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

               isDataInTourType = true;

               sb.append(String.format(valueFormatting,

                     year,
                     month,

                     tourTypeName,

                     _tourMonthData.elapsedTime[tourTypeIndex][monthIndex],
                     _tourMonthData.recordedTime[tourTypeIndex][monthIndex],
                     _tourMonthData.pausedTime[tourTypeIndex][monthIndex],

                     _tourMonthData.movingTime[tourTypeIndex][monthIndex],
                     _tourMonthData.breakTime[tourTypeIndex][monthIndex],

                     _tourMonthData.altitudeHigh[tourTypeIndex][monthIndex],
                     _tourMonthData.distanceHigh[tourTypeIndex][monthIndex],

                     _tourMonthData.numToursHigh[tourTypeIndex][monthIndex]

               ));
            }
         }

         // group values
         if (isDataInTourType) {
            sb.append(NL);
         }
      }

      _tourMonthData.statisticValuesRaw = sb.toString();
   }
}
