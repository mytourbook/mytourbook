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

                  // this is necessary otherwise tours can occure multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                                     //$NON-NLS-1$

                  + "      StartWeekYear," + NL //                                                       //$NON-NLS-1$
                  + "      StartWeek," + NL //                                                           //$NON-NLS-1$
                  + "      TourDistance," + NL //                                                        //$NON-NLS-1$
                  + "      TourAltUp," + NL //                                                           //$NON-NLS-1$
                  + "      TourRecordingTime," + NL //                                                   //$NON-NLS-1$
                  + "      TourDrivingTime," + NL //                                                     //$NON-NLS-1$

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

         String sqlDurationTime = null;

         switch (durationTime) {
         case BREAK:

            sqlDurationTime = "SUM(TourRecordingTime - TourDrivingTime),"; //$NON-NLS-1$
            break;

         case RECORDING:

            sqlDurationTime = "SUM(TourRecordingTime),"; //$NON-NLS-1$
            break;

         case MOVING:
         default:
            // this is also the old implementation for the duration values
            sqlDurationTime = "SUM(CASE WHEN TourDrivingTime > 0 THEN TourDrivingTime ELSE TourRecordingTime END),"; //$NON-NLS-1$
            break;
         }

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                //$NON-NLS-1$

               + "   StartWeekYear," + NL //                                  1  //$NON-NLS-1$
               + "   StartWeek," + NL //                                      2  //$NON-NLS-1$

               + "   SUM(TourDistance)," + NL //                              3  //$NON-NLS-1$
               + "   SUM(TourAltUp)," + NL //                                 4  //$NON-NLS-1$
               + "   " + sqlDurationTime + NL //                              5  //$NON-NLS-1$
               + "   SUM(TourRecordingTime)," + NL //                         6  //$NON-NLS-1$
               + "   SUM(TourDrivingTime)," + NL //                           7  //$NON-NLS-1$
               + "   SUM(1)," + NL //                                         8  //$NON-NLS-1$

               + "   TourType_TypeId" + NL //                                 9 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartWeekYear, StartWeek, tourType_typeId" + NL // //$NON-NLS-1$
               + "ORDER BY StartWeekYear, StartWeek" + NL //                  //$NON-NLS-1$
         ;

         final float[][] dbDistance = new float[numTourTypes][numWeeks];
         final float[][] dbAltitude = new float[numTourTypes][numWeeks];
         final float[][] dbNumTours = new float[numTourTypes][numWeeks];

         final int[][] dbDurationTime = new int[numTourTypes][numWeeks];
         final int[][] dbRecordingTime = new int[numTourTypes][numWeeks];
         final int[][] dbDrivingTime = new int[numTourTypes][numWeeks];
         final int[][] dbBreakTime = new int[numTourTypes][numWeeks];

         final long[][] dbTypeIds = new long[numTourTypes][numWeeks];

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
                * This can occure when dbWeek == 0, tour is in the previous year and not displayed
                * in the week stats
                */

               continue;
            }

            if (weekIndex >= numWeeks) {

               /**
                * This problem occured but is not yet fully fixed, it needs more investigation.
                * <p>
                * Problem with this configuration</br>
                * Statistic: Week summary</br>
                * Tour type: Velo (3 bars)</br>
                * Displayed years: 2013 + 2014
                * <p>
                * Problem occured when selecting year 2015
                */
               continue;
            }

            /*
             * Convert type id to the type index in the tour types list which is also the color
             * index
             */
            int colorIndex = 0;
            final Long dbTypeIdObject = (Long) result.getObject(9);
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

            dbTypeIds[colorIndex][weekIndex] = dbTypeId;

            dbDistance[colorIndex][weekIndex] = (int) (result.getInt(3) / UI.UNIT_VALUE_DISTANCE);
            dbAltitude[colorIndex][weekIndex] = (int) (result.getInt(4) / UI.UNIT_VALUE_ALTITUDE);
            dbDurationTime[colorIndex][weekIndex] = result.getInt(5);

            final int recordingTime = result.getInt(6);
            final int drivingTime = result.getInt(7);
            final int numTours = result.getInt(8);

            dbNumTours[colorIndex][weekIndex] = numTours;

            dbRecordingTime[colorIndex][weekIndex] = recordingTime;
            dbDrivingTime[colorIndex][weekIndex] = drivingTime;
            dbBreakTime[colorIndex][weekIndex] = recordingTime - drivingTime;
         }

         _tourWeekData.typeIds = dbTypeIds;

         _tourWeekData.years = _years;
         _tourWeekData.yearWeeks = _yearWeeks;
         _tourWeekData.yearDays = _yearDays;

         _tourWeekData.setDurationTimeLow(new int[numTourTypes][numWeeks]);
         _tourWeekData.setDurationTimeHigh(dbDurationTime);

         _tourWeekData.distanceLow = new float[numTourTypes][numWeeks];
         _tourWeekData.distanceHigh = dbDistance;

         _tourWeekData.altitudeLow = new float[numTourTypes][numWeeks];
         _tourWeekData.altitudeHigh = dbAltitude;

         _tourWeekData.recordingTime = dbRecordingTime;
         _tourWeekData.drivingTime = dbDrivingTime;
         _tourWeekData.breakTime = dbBreakTime;

         _tourWeekData.numToursLow = new float[numTourTypes][numWeeks];
         _tourWeekData.numToursHigh = dbNumTours;

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourWeekData;
   }
}
