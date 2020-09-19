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

public class DataProvider_Tour_Year extends DataProvider {

   private static DataProvider_Tour_Year _instance;

   private TourData_Year                 _tourDataYear;

   private DataProvider_Tour_Year() {}

   public static DataProvider_Tour_Year getInstance() {

      if (_instance == null) {
         _instance = new DataProvider_Tour_Year();
      }

      return _instance;
   }

   TourData_Year getYearData(final TourPerson person,
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

         return _tourDataYear;
      }

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         _activePerson = person;
         _activeTourTypeFilter = tourTypeFilter;
         _lastYear = lastYear;
         _numberOfYears = numYears;

         // get the tour types
         final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
         final TourType[] allTourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

         _tourDataYear = new TourData_Year();

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
                  + "      TourDistance," + NL //                                               //$NON-NLS-1$
                  + "      TourAltUp," + NL //                                                  //$NON-NLS-1$
                  + "      TourDeviceTime_Elapsed,  " + NL //$NON-NLS-1$
                  + "      TourComputedTime_Moving, " + NL //$NON-NLS-1$

                  + "      TourType_TypeId" + NL //                                             //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                           //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //     //$NON-NLS-1$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                //$NON-NLS-1$

                  + "   WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlAppFilter.getWhereClause()

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                               //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            fromTourData = NL

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                              //$NON-NLS-1$

                  + "WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + sqlAppFilter.getWhereClause()

            ;
         }

         sql = NL +

               "SELECT" + NL //                                               //$NON-NLS-1$

               + "   StartYear," + NL //                                   1  //$NON-NLS-1$

               + "   SUM(TourDistance)," + NL //                           2  //$NON-NLS-1$
               + "   SUM(TourAltUp)," + NL //                              3  //$NON-NLS-1$
               + "   " + createSQL_SumDurationTime(durationTime) + NL //   4  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Elapsed)," + NL //                 5  //$NON-NLS-1$
               + "   SUM(TourComputedTime_Moving)," + NL //                6  //$NON-NLS-1$
               + "   SUM(1)," + NL //                                      7  //$NON-NLS-1$
               + "   TourType_TypeId," + NL //                             8  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Recorded)," + NL //                9  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Paused)" + NL //                   10 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartYear, tourType_typeId " + NL //               //$NON-NLS-1$
               + "ORDER BY StartYear" + NL //                                 //$NON-NLS-1$
         ;

         final boolean isShowNoTourTypes = tourTypeFilter.showUndefinedTourTypes();

         int colorOffset = 0;
         if (isShowNoTourTypes) {
            colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
         }

         int numTourTypes = colorOffset + allTourTypes.length;
         numTourTypes = numTourTypes == 0 ? 1 : numTourTypes; // ensure that at least 1 is available

         final float[][] dbDistance = new float[numTourTypes][numYears];
         final float[][] dbAltitude = new float[numTourTypes][numYears];
         final float[][] dbNumTours = new float[numTourTypes][numYears];

         final int[][] dbDurationTime = new int[numTourTypes][numYears];
         final int[][] dbElapsedTime = new int[numTourTypes][numYears];
         final int[][] dbRecordedTime = new int[numTourTypes][numYears];
         final int[][] dbPausedTime = new int[numTourTypes][numYears];
         final int[][] dbMovingTime = new int[numTourTypes][numYears];
         final int[][] dbBreakTime = new int[numTourTypes][numYears];

         final long[][] dbTypeIds = new long[numTourTypes][numYears];
         final long[] tourTypeSum = new long[numTourTypes];
         final long[] usedTourTypeIds = new long[numTourTypes];
         Arrays.fill(usedTourTypeIds, TourType.TOUR_TYPE_IS_NOT_USED);

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

// SET_FORMATTING_OFF

            final int dbValue_ResultYear           = result.getInt(1);
            final int dbValue_Altitude             = (int) (result.getInt(3) / UI.UNIT_VALUE_ALTITUDE);
            final int dbValue_Distance             = (int) ((result.getInt(2) + 500) / 1000 / UI.UNIT_VALUE_DISTANCE);
            final int dbValue_Duration             = result.getInt(4);
            final int dbValue_ElapsedTime          = result.getInt(5);
            final int dbValue_MovingTime           = result.getInt(6);
            final int dbValue_NumTours             = result.getInt(7);
            final Long dbValue_TourTypeIdObject    = (Long) result.getObject(8);
            final int dbValue_RecordedTime         = result.getInt(9);
            final int dbValue_PausedTime           = result.getInt(10);

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
                     colorIndex = colorOffset + typeIndex;
                     break;
                  }
               }
            }

            final long noTourTypeId = isShowNoTourTypes
                  ? TourType.TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA
                  : TourType.TOUR_TYPE_IS_NOT_USED;

            final long typeId = dbValue_TourTypeIdObject == null ? noTourTypeId : dbValue_TourTypeIdObject;

            dbTypeIds[colorIndex][yearIndex] = typeId;

            dbAltitude[colorIndex][yearIndex] = dbValue_Altitude;
            dbDistance[colorIndex][yearIndex] = dbValue_Distance;
            dbDurationTime[colorIndex][yearIndex] = dbValue_Duration;
            dbNumTours[colorIndex][yearIndex] = dbValue_NumTours;

            dbElapsedTime[colorIndex][yearIndex] = dbValue_ElapsedTime;
            dbRecordedTime[colorIndex][yearIndex] = dbValue_RecordedTime;
            dbPausedTime[colorIndex][yearIndex] = dbValue_PausedTime;
            dbMovingTime[colorIndex][yearIndex] = dbValue_MovingTime;
            dbBreakTime[colorIndex][yearIndex] = dbValue_ElapsedTime - dbValue_MovingTime;

            usedTourTypeIds[colorIndex] = typeId;
            tourTypeSum[colorIndex] += dbValue_Distance + dbValue_Altitude + dbValue_ElapsedTime;
         }

         final int[] years = new int[_numberOfYears];
         int yearIndex = 0;
         for (int currentYear = _lastYear - _numberOfYears + 1; currentYear <= _lastYear; currentYear++) {
            years[yearIndex++] = currentYear;
         }
         _tourDataYear.years = years;

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
         final int numUsedTourTypes = typeIdsWithData.size();

         if (numUsedTourTypes == 0) {

            // there are NO data, create dummy data that the UI do not fail

            _tourDataYear.typeIds = new long[1][1];
            _tourDataYear.usedTourTypeIds = new long[] { TourType.TOUR_TYPE_IS_NOT_USED };

            _tourDataYear.altitudeLow = new float[1][numYears];
            _tourDataYear.altitudeHigh = new float[1][numYears];

            _tourDataYear.distanceLow = new float[1][numYears];
            _tourDataYear.distanceHigh = new float[1][numYears];

            _tourDataYear.setDurationTimeLow(new int[1][numYears]);
            _tourDataYear.setDurationTimeHigh(new int[1][numYears]);

            _tourDataYear.elapsedTime = new int[1][numYears];
            _tourDataYear.recordedTime = new int[1][numYears];
            _tourDataYear.pausedTime = new int[1][numYears];
            _tourDataYear.movingTime = new int[1][numYears];
            _tourDataYear.breakTime = new int[1][numYears];

            _tourDataYear.numToursLow = new float[1][numYears];
            _tourDataYear.numToursHigh = new float[1][numYears];

         } else {

            final long[][] usedTypeIds = new long[numUsedTourTypes][];

            final float[][] usedAltitude = new float[numUsedTourTypes][];
            final float[][] usedDistance = new float[numUsedTourTypes][];
            final int[][] usedDuration = new int[numUsedTourTypes][];
            final int[][] usedElapsedTime = new int[numUsedTourTypes][];
            final int[][] usedRecordedTime = new int[numUsedTourTypes][];
            final int[][] usedPausedTime = new int[numUsedTourTypes][];
            final int[][] usedMovingTime = new int[numUsedTourTypes][];
            final int[][] usedBreakTime = new int[numUsedTourTypes][];
            final float[][] usedNumTours = new float[numUsedTourTypes][];

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

            _tourDataYear.typeIds = usedTypeIds;
            _tourDataYear.usedTourTypeIds = usedTourTypeIds;

            _tourDataYear.altitudeLow = new float[numUsedTourTypes][numYears];
            _tourDataYear.altitudeHigh = usedAltitude;

            _tourDataYear.distanceLow = new float[numUsedTourTypes][numYears];
            _tourDataYear.distanceHigh = usedDistance;

            _tourDataYear.setDurationTimeLow(new int[numUsedTourTypes][numYears]);
            _tourDataYear.setDurationTimeHigh(usedDuration);

            _tourDataYear.elapsedTime = usedElapsedTime;
            _tourDataYear.recordedTime = usedRecordedTime;
            _tourDataYear.pausedTime = usedPausedTime;
            _tourDataYear.movingTime = usedMovingTime;
            _tourDataYear.breakTime = usedBreakTime;

            _tourDataYear.numToursLow = new float[numUsedTourTypes][numYears];
            _tourDataYear.numToursHigh = usedNumTours;
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourDataYear;
   }
}
