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

public class DataProvider_Tour_Week extends DataProvider {

   private static DataProvider_Tour_Week _instance;

   private TourData_Week                 _tourWeekData;

   private DataProvider_Tour_Week() {}

   public static DataProvider_Tour_Week getInstance() {

      if (_instance == null) {
         _instance = new DataProvider_Tour_Week();
      }

      return _instance;
   }

   TourData_Week getWeekData(final TourPerson person,
                             final TourTypeFilter tourTypeFilter,
                             final int lastYear,
                             final int numberOfYears,
                             final boolean refreshData,
                             final DurationTime durationTime) {

      // when the data for the year are already loaded, all is done
      if (_activePerson == person
            && _activeTourTypeFilter == tourTypeFilter
            && lastYear == _lastYear
            && numberOfYears == _numberOfYears
            && refreshData == false) {

         return _tourWeekData;
      }

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         _activePerson = person;
         _activeTourTypeFilter = tourTypeFilter;

         _lastYear = lastYear;
         _numberOfYears = numberOfYears;

         initYearNumbers();

         _tourWeekData = new TourData_Week();

         // get the tour types
         final ArrayList<TourType> allActiveTourTypesList = TourDatabase.getActiveTourTypes();
         final TourType[] allActiveTourTypes = allActiveTourTypesList.toArray(new TourType[allActiveTourTypesList.size()]);

         int numWeeks = 0;
         for (final int weeks : _yearWeeks) {
            numWeeks += weeks;
         }

         int colorOffset = 0;
         if (tourTypeFilter.showUndefinedTourTypes()) {
            colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
         }

         int numTourTypes = colorOffset + allActiveTourTypes.length;
         numTourTypes = numTourTypes == 0 ? 1 : numTourTypes; // ensure that at least 1 is available

         String fromTourData;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder(true);

         if (TourTagFilterManager.isTourTagFilterEnabled()) {

            // with tag filter

            fromTourData = NL

                  + "FROM (" + NL //                                                                     //$NON-NLS-1$

                  + "   SELECT" + NL //                                                                  //$NON-NLS-1$

                  // this is necessary otherwise tours can occur multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                                     //$NON-NLS-1$

                  + "      StartWeekYear," + NL //                                                       //$NON-NLS-1$
                  + "      StartWeek," + NL //                                                           //$NON-NLS-1$

                  + "      TourDeviceTime_Elapsed," + NL //                                              //$NON-NLS-1$
                  + "      TourDeviceTime_Recorded," + NL //                                             //$NON-NLS-1$
                  + "      TourDeviceTime_Paused," + NL //                                               //$NON-NLS-1$
                  + "      TourComputedTime_Moving," + NL //                                             //$NON-NLS-1$

                  + "      TourDistance," + NL //                                                        //$NON-NLS-1$
                  + "      TourAltUp," + NL //                                                           //$NON-NLS-1$

                  + "      TourType_TypeId" + NL //                                                      //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                                    //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //              //$NON-NLS-1$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                         //$NON-NLS-1$

                  + "   WHERE StartWeekYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
                  + "      " + sqlAppFilter.getWhereClause() + NL //                                     //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                                        //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            fromTourData = NL

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                                       //$NON-NLS-1$

                  + "WHERE StartWeekYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
                  + "   " + sqlAppFilter.getWhereClause()

            ;
         }

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                //$NON-NLS-1$

               + "   StartWeekYear," + NL //                                  1  //$NON-NLS-1$
               + "   StartWeek," + NL //                                      2  //$NON-NLS-1$

               + "   TourType_TypeId," + NL //                                3  //$NON-NLS-1$

               + "   SUM(TourDeviceTime_Elapsed)," + NL //                    4  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Recorded)," + NL //                   5  //$NON-NLS-1$
               + "   SUM(TourDeviceTime_Paused)," + NL //                     6  //$NON-NLS-1$
               + "   SUM(TourComputedTime_Moving)," + NL //                   7  //$NON-NLS-1$
               + "   " + createSQL_SumDurationTime(durationTime) + NL //      8  //$NON-NLS-1$

               + "   SUM(TourDistance)," + NL //                              9  //$NON-NLS-1$
               + "   SUM(TourAltUp)," + NL //                                 10 //$NON-NLS-1$

               + "   SUM(1)" + NL //                                          11 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartWeekYear, StartWeek, tourType_typeId" + NL //    //$NON-NLS-1$
               + "ORDER BY StartWeekYear, StartWeek" + NL //                     //$NON-NLS-1$
         ;

         final long[][] allDbTypeIds = new long[numTourTypes][numWeeks];

         final int[][] allDbDurationTime = new int[numTourTypes][numWeeks];
         final int[][] allDbElapsedTime = new int[numTourTypes][numWeeks];
         final int[][] allDbRecordedTime = new int[numTourTypes][numWeeks];
         final int[][] allDbPausedTime = new int[numTourTypes][numWeeks];
         final int[][] allDbMovingTime = new int[numTourTypes][numWeeks];
         final int[][] allDbBreakTime = new int[numTourTypes][numWeeks];

         final float[][] allDbDistance = new float[numTourTypes][numWeeks];
         final float[][] allDbElevation = new float[numTourTypes][numWeeks];

         final float[][] allDbNumTours = new float[numTourTypes][numWeeks];

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final int dbYear = result.getInt(1);
            final int dbWeek = result.getInt(2);

            // get number of weeks for the current year in the db
            final int dbYearIndex = numberOfYears - (lastYear - dbYear + 1);
            int allWeeks = 0;
            for (int yearIndex = 0; yearIndex <= dbYearIndex; yearIndex++) {
               if (yearIndex > 0) {
                  allWeeks += _yearWeeks[yearIndex - 1];
               }
            }

            final int weekIndex = allWeeks + dbWeek - 1;

            if (weekIndex < 0) {

               /**
                * This can occur when dbWeek == 0, tour is in the previous year and not displayed
                * in the week stats
                */

               continue;
            }

            if (weekIndex >= numWeeks) {

               /**
                * This problem occurred but is not yet fully fixed, it needs more investigation.
                * <p>
                * Problem with this configuration</br>
                * Statistic: Week summary</br>
                * Tour type: Velo (3 bars)</br>
                * Displayed years: 2013 + 2014
                * <p>
                * Problem occurred when selecting year 2015
                */
               continue;
            }

            /*
             * Convert type id to the type index in the tour types list which is also the color
             * index
             */
            final Long dbTypeIdObject = (Long) result.getObject(3);
            int colorIndex = 0;
            if (dbTypeIdObject != null) {
               final long dbTypeId = dbTypeIdObject;
               for (int typeIndex = 0; typeIndex < allActiveTourTypes.length; typeIndex++) {
                  if (dbTypeId == allActiveTourTypes[typeIndex].getTypeId()) {
                     colorIndex = colorOffset + typeIndex;
                     break;
                  }
               }
            }
            final long dbTypeId = dbTypeIdObject == null ? TourDatabase.ENTITY_IS_NOT_SAVED : dbTypeIdObject;

            final int dbElapsedTime = result.getInt(4);
            final int dbRecordedTime = result.getInt(5);
            final int dbPausedTime = result.getInt(6);
            final int dbMovingTime = result.getInt(7);

            final int dbDurationTime = result.getInt(8);

            final int dbDistance = (int) (result.getInt(9) / UI.UNIT_VALUE_DISTANCE);
            final int dbElevation = (int) (result.getInt(10) / UI.UNIT_VALUE_ALTITUDE);

            final int numTours = result.getInt(11);

            allDbTypeIds[colorIndex][weekIndex] = dbTypeId;

            allDbElapsedTime[colorIndex][weekIndex] = dbElapsedTime;
            allDbRecordedTime[colorIndex][weekIndex] = dbRecordedTime;
            allDbPausedTime[colorIndex][weekIndex] = dbPausedTime;
            allDbMovingTime[colorIndex][weekIndex] = dbMovingTime;
            allDbBreakTime[colorIndex][weekIndex] = dbElapsedTime - dbMovingTime;
            allDbDurationTime[colorIndex][weekIndex] = dbDurationTime;

            allDbDistance[colorIndex][weekIndex] = dbDistance;
            allDbElevation[colorIndex][weekIndex] = dbElevation;

            allDbNumTours[colorIndex][weekIndex] = numTours;
         }


         _tourWeekData.years = _years;
         _tourWeekData.yearWeeks = _yearWeeks;
         _tourWeekData.yearDays = _yearDays;

         _tourWeekData.typeIds = allDbTypeIds;

         _tourWeekData.elapsedTime = allDbElapsedTime;
         _tourWeekData.recordedTime = allDbRecordedTime;
         _tourWeekData.pausedTime = allDbPausedTime;
         _tourWeekData.movingTime = allDbMovingTime;
         _tourWeekData.breakTime = allDbBreakTime;

         _tourWeekData.setDurationTimeLow(new int[numTourTypes][numWeeks]);
         _tourWeekData.setDurationTimeHigh(allDbDurationTime);

         _tourWeekData.distanceLow = new float[numTourTypes][numWeeks];
         _tourWeekData.distanceHigh = allDbDistance;

         _tourWeekData.altitudeLow = new float[numTourTypes][numWeeks];
         _tourWeekData.altitudeHigh = allDbElevation;

         _tourWeekData.numToursLow = new float[numTourTypes][numWeeks];
         _tourWeekData.numToursHigh = allDbNumTours;

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourWeekData;
   }
}
