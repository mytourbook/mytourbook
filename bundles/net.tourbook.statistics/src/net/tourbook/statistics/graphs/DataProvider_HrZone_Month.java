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

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;

public class DataProvider_HrZone_Month extends DataProvider {

   private static DataProvider_HrZone_Month _instance;

   private TourData_MonthHrZones            _monthData;

   private DataProvider_HrZone_Month() {}

   public static DataProvider_HrZone_Month getInstance() {
      if (_instance == null) {
         _instance = new DataProvider_HrZone_Month();
      }
      return _instance;
   }

   TourData_MonthHrZones getMonthData(final TourPerson person,
                                      final TourTypeFilter tourTypeFilter,
                                      final int lastYear,
                                      final int numYears,
                                      final boolean refreshData) {

      /*
       * check if the required data are already loaded
       */
      if (_activePerson == person
            && _activeTourTypeFilter == tourTypeFilter
            && lastYear == _lastYear
            && numYears == _numberOfYears
            && refreshData == false) {
         return _monthData;
      }

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         _activePerson = person;
         _activeTourTypeFilter = tourTypeFilter;

         _lastYear = lastYear;
         _numberOfYears = numYears;

         _monthData = new TourData_MonthHrZones();

         String fromTourData;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder(true);

         if (TourTagFilterManager.isTourTagFilterEnabled()) {

            // with tag filter

            fromTourData = UI.EMPTY_STRING

                  + "FROM (" + NL //                                                            //$NON-NLS-1$

                  + "   SELECT" + NL //                                                         //$NON-NLS-1$

                  + "      StartYear," + NL //                                                  //$NON-NLS-1$
                  + "      StartMonth," + NL //                                                 //$NON-NLS-1$

                  + "      HrZone0," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone1," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone2," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone3," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone4," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone5," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone6," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone7," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone8," + NL //                                                    //$NON-NLS-1$
                  + "      HrZone9" + NL //                                                     //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                           //$NON-NLS-1$

                  // get/filter tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //     //$NON-NLS-1$ //$NON-NLS-2$
                  + "   ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                //$NON-NLS-1$

                  + "   WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
                  + "      AND NumberOfHrZones > 0" + NL //                                     //$NON-NLS-1$
                  + "      " + sqlAppFilter.getWhereClause() + NL //                            //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" + NL //                               //$NON-NLS-1$
            ;

         } else {

            // without tag filter

            fromTourData = UI.EMPTY_STRING

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                              //$NON-NLS-1$

                  + "WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
                  + "   AND NumberOfHrZones > 0" + NL //                                        //$NON-NLS-1$
                  + "   " + sqlAppFilter.getWhereClause() + NL //                               //$NON-NLS-1$

            ;
         }

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                      //$NON-NLS-1$

               + "   StartYear," + NL //                                            1  //$NON-NLS-1$
               + "   StartMonth," + NL //                                           2  //$NON-NLS-1$

               + "   SUM(CASE WHEN hrZone0 > 0 THEN hrZone0 ELSE 0 END)," + NL //   3  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone1 > 0 THEN hrZone1 ELSE 0 END)," + NL //   4  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone2 > 0 THEN hrZone2 ELSE 0 END)," + NL //   5  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone3 > 0 THEN hrZone3 ELSE 0 END)," + NL //   6  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone4 > 0 THEN hrZone4 ELSE 0 END)," + NL //   7  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone5 > 0 THEN hrZone5 ELSE 0 END)," + NL //   8  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone6 > 0 THEN hrZone6 ELSE 0 END)," + NL //   9  //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone7 > 0 THEN hrZone7 ELSE 0 END)," + NL //   10 //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone8 > 0 THEN hrZone8 ELSE 0 END)," + NL //   11 //$NON-NLS-1$
               + "   SUM(CASE WHEN hrZone9 > 0 THEN hrZone9 ELSE 0 END)" + NL //    12 //$NON-NLS-1$

               + fromTourData

               + "GROUP BY StartYear, StartMonth" + NL //                              //$NON-NLS-1$
               + "ORDER BY StartYear, StartMonth" + NL //                              //$NON-NLS-1$
         ;

         final int maxZones = 10; // hr zones: 0...9
         final int serieLength = maxZones;
         final int valueLength = 12 * numYears;

         final int[][] dbHrZones = new int[serieLength][valueLength];

         {
            final PreparedStatement prepStmt = conn.prepareStatement(sql);

            int paramIndex = 1;
            paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

            sqlAppFilter.setParameters(prepStmt, paramIndex);

            final ResultSet result = prepStmt.executeQuery();
            while (result.next()) {

               final int dbYear = result.getInt(1);
               final int dbMonth = result.getInt(2);

               final int yearIndex = numYears - (lastYear - dbYear + 1);
               final int monthIndex = (dbMonth - 1) + yearIndex * 12;

               dbHrZones[0][monthIndex] = result.getInt(3);
               dbHrZones[1][monthIndex] = result.getInt(4);
               dbHrZones[2][monthIndex] = result.getInt(5);
               dbHrZones[3][monthIndex] = result.getInt(6);
               dbHrZones[4][monthIndex] = result.getInt(7);
               dbHrZones[5][monthIndex] = result.getInt(8);
               dbHrZones[6][monthIndex] = result.getInt(9);
               dbHrZones[7][monthIndex] = result.getInt(10);
               dbHrZones[8][monthIndex] = result.getInt(11);
               dbHrZones[9][monthIndex] = result.getInt(12);
            }
         }

         _monthData.hrZoneValues = dbHrZones;

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      setStatisticValues();

      return _monthData;
   }

   private void setStatisticValues() {

      final StringBuilder sb = new StringBuilder();

      sb.append(UI.EMPTY_STRING

            + "Year," //$NON-NLS-1$
            + " Month," //$NON-NLS-1$
            + "     Zone1," //$NON-NLS-1$
            + "      Zone2," //$NON-NLS-1$
            + "      Zone3," //$NON-NLS-1$
            + "      Zone4," //$NON-NLS-1$
            + "      Zone5," //$NON-NLS-1$
            + "      Zone6," //$NON-NLS-1$
            + "      Zone7," //$NON-NLS-1$
            + "      Zone8," //$NON-NLS-1$
            + "      Zone9," //$NON-NLS-1$
            + "     Zone10," //$NON-NLS-1$

            + "      Summary," //$NON-NLS-1$
            + "       Summary" //$NON-NLS-1$

            + NL);

      sb.append(UI.EMPTY_STRING

            + "    ," //$NON-NLS-1$
            + "     ," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$
            + "        (s)," //$NON-NLS-1$

            + "          (s)," //$NON-NLS-1$
            + "    (hh:mm:ss)" //$NON-NLS-1$

            + NL);

      final int[][] hrZoneValues = _monthData.hrZoneValues;
      final int numMonths = hrZoneValues[0].length;
      final int firstYear = _lastYear - _numberOfYears + 1;

      // setup previous year
      int prevYear = firstYear;

      for (int monthIndex = 0; monthIndex < numMonths; monthIndex++) {

         final int yearIndex = monthIndex / 12;
         final int year = firstYear + yearIndex;

         final int month = (monthIndex % 12) + 1;

         int sumSeconds = 0;
         for (final int[] hrZoneValue : hrZoneValues) {
            sumSeconds += hrZoneValue[monthIndex];
         }

         final String sumHHMMSS = net.tourbook.common.UI.format_hhh_mm_ss(sumSeconds);

         // group by year
         if (year != prevYear) {
            prevYear = year;
            sb.append(NL);
         }

         sb.append(String.format(UI.EMPTY_STRING

               // year, month
               + "%4d, %5d," //$NON-NLS-1$

               // zone 1...10
               + "%10d, %10d, %10d, %10d, %10d, %10d, %10d, %10d, %10d, %10d," //$NON-NLS-1$

               // summaries
               + "%13d, %13s" //$NON-NLS-1$

               + NL,

               year,
               month,

               hrZoneValues[0][monthIndex],
               hrZoneValues[1][monthIndex],
               hrZoneValues[2][monthIndex],
               hrZoneValues[3][monthIndex],
               hrZoneValues[4][monthIndex],
               hrZoneValues[5][monthIndex],
               hrZoneValues[6][monthIndex],
               hrZoneValues[7][monthIndex],
               hrZoneValues[8][monthIndex],
               hrZoneValues[9][monthIndex],

               sumSeconds,
               sumHHMMSS

         ));
      }

      _monthData.statisticValuesRaw = sb.toString();
   }

}
