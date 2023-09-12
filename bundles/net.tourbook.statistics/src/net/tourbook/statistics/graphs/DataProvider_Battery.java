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
package net.tourbook.statistics.graphs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;

import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.LongArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.ShortArrayList;

public class DataProvider_Battery extends DataProvider {

   private TourStatisticData_Battery _batteryData;

   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {

      if (_batteryData == null) {
         return null;
      }

      if (statistic_RawStatisticValues != null && isShowSequenceNumbers == statistic_isShowSequenceNumbers) {
         return statistic_RawStatisticValues;
      }

      final StringBuilder sb = new StringBuilder();

      final String headerLine1 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead1() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead1()
            + STAT_VALUE_DATE_MONTH.getHead1()
            + STAT_VALUE_DATE_DAY.getHead1()
            + STAT_VALUE_DATE_WEEK.getHead1()

            + STAT_VALUE_TOUR_TYPE.getHead1()

            + STAT_VALUE_BATTERY_SOC_START.getHead1()
            + STAT_VALUE_BATTERY_SOC_END.getHead1()

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getHead2() : UI.EMPTY_STRING)

            + STAT_VALUE_DATE_YEAR.getHead2()
            + STAT_VALUE_DATE_MONTH.getHead2()
            + STAT_VALUE_DATE_DAY.getHead2()
            + STAT_VALUE_DATE_WEEK.getHead2()

            + STAT_VALUE_TOUR_TYPE.getHead2()

            + STAT_VALUE_BATTERY_SOC_START.getHead2()
            + STAT_VALUE_BATTERY_SOC_END.getHead2()

      ;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? STAT_VALUE_SEQUENCE_NUMBER.getValueFormatting() : "%s") //$NON-NLS-1$

            + STAT_VALUE_DATE_YEAR.getValueFormatting()
            + STAT_VALUE_DATE_MONTH.getValueFormatting()
            + STAT_VALUE_DATE_DAY.getValueFormatting()
            + STAT_VALUE_DATE_WEEK.getValueFormatting()

            + STAT_VALUE_TOUR_TYPE.getValueFormatting()

            + STAT_VALUE_BATTERY_SOC_START.getValueFormatting()
            + STAT_VALUE_BATTERY_SOC_END.getValueFormatting()

      ;

      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final int numDataItems = _batteryData.allTourIds.length;

      // set initial value
      int prevMonth = numDataItems > 0 ? _batteryData.allTourMonths[0] : 0;

      int sequenceNumber = 0;

      for (int dataIndex = 0; dataIndex < numDataItems; dataIndex++) {

         final int month = _batteryData.allTourMonths[dataIndex];

         // group by month
         if (month != prevMonth) {
            prevMonth = month;
            sb.append(NL);
         }

         Object sequenceNumberValue = UI.EMPTY_STRING;
         if (isShowSequenceNumbers) {
            sequenceNumberValue = ++sequenceNumber;
         }

         sb.append(String.format(valueFormatting,

               sequenceNumberValue,

               _batteryData.allTourYears[dataIndex],
               month,
               _batteryData.allTourDays[dataIndex],
               _batteryData.allWeeks[dataIndex],

               TourDatabase.getTourTypeName(_batteryData.allTypeIds[dataIndex]),

               _batteryData.allBatteryPercentage_Start[dataIndex],
               _batteryData.allBatteryPercentage_End[dataIndex]

         ));

         sb.append(NL);
      }

      // cache values
      statistic_RawStatisticValues = sb.toString();
      statistic_isShowSequenceNumbers = isShowSequenceNumbers;

      return statistic_RawStatisticValues;
   }

   /**
    * Retrieve chart data from the database
    *
    * @param person
    * @param tourTypeFilter
    * @param lastYear
    * @param numYears
    * @param isForceUpdate
    * @return
    */
   TourStatisticData_Battery getTourTimeData(final TourPerson person,
                                             final TourTypeFilter tourTypeFilter,
                                             final int lastYear,
                                             final int numYears,
                                             final boolean isForceUpdate) {

      // don't reload data which are already here
      if (statistic_ActivePerson == person
            && statistic_ActiveTourTypeFilter == tourTypeFilter
            && statistic_LastYear == lastYear
            && statistic_NumberOfYears == numYears
            && isForceUpdate == false) {

         return _batteryData;
      }

      // reset cached values
      statistic_RawStatisticValues = null;

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statistic_ActivePerson = person;
         statistic_ActiveTourTypeFilter = tourTypeFilter;

         statistic_LastYear = lastYear;
         statistic_NumberOfYears = numYears;

         setupYearNumbers();

         final ArrayList<TourType> allActiveTourTypes = TourDatabase.getActiveTourTypes();
         final TourType[] allTourTypes = allActiveTourTypes.toArray(new TourType[allActiveTourTypes.size()]);

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.ANY_APP_FILTERS);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         final String sqlYears = getYearList(lastYear, numYears);

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                   //$NON-NLS-1$

               + "   TourId," + NL //                                            1  //$NON-NLS-1$

               + "   StartYear," + NL //                                         2  //$NON-NLS-1$
               + "   StartMonth," + NL //                                        3  //$NON-NLS-1$
               + "   StartWeek," + NL //                                         4  //$NON-NLS-1$
               + "   TourStartTime," + NL //                                     5  //$NON-NLS-1$
               + "   TimeZoneId," + NL //                                        6  //$NON-NLS-1$
               + "   TourDeviceTime_Elapsed," + NL //                            7  //$NON-NLS-1$

               + "   TourType_typeId," + NL //                                   8  //$NON-NLS-1$

               + "   Battery_Percentage_Start," + NL //                          9  //$NON-NLS-1$
               + "   Battery_Percentage_End," + NL //                            10 //$NON-NLS-1$

               + "   jTdataTtag.TourTag_tagId" + NL //                           11 //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                     //$NON-NLS-1$

               // set tag filter id's
               + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //    //$NON-NLS-1$
               + " ON TourId = jTdataTtag.TourData_tourId" + NL //                  //$NON-NLS-1$

               + "WHERE" + NL //                                                    //$NON-NLS-1$

               // ignore tours without battery values
               + "   Battery_Percentage_Start > -1 AND " + NL //                    //$NON-NLS-1$

               + "   StartYear IN (" + sqlYears + ")" + NL //                       //$NON-NLS-1$ //$NON-NLS-2$
               + sqlAppFilter.getWhereClause() + NL //

               + "ORDER BY TourStartTime" + NL //                                   //$NON-NLS-1$
         ;

         final LongArrayList allTourIds = new LongArrayList();

         final IntArrayList allTourYear = new IntArrayList();
         final IntArrayList allTourMonths = new IntArrayList();
         final IntArrayList allTourDays = new IntArrayList();
         final IntArrayList allYearsDOY = new IntArrayList(); // DOY...Day Of Year for all years

         final IntArrayList allTourStartWeek = new IntArrayList();

         final ShortArrayList allBatteryPercentage_Start = new ShortArrayList();
         final ShortArrayList allBatteryPercentage_End = new ShortArrayList();

         final LongArrayList allTypeIds = new LongArrayList();
         final IntArrayList allTypeColorIndex = new IntArrayList();

         final ArrayList<ZonedDateTime> allTourStartDateTime = new ArrayList<>();
         final ArrayList<String> allTourTimeOffset = new ArrayList<>();

         final HashMap<Long, ArrayList<Long>> allTagIds = new HashMap<>();

//         final float lastBatteryPerformance = 0;
         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final long dbTourId = result.getLong(1);
            final Object dbTagId = result.getObject(11);

            if (dbTourId == lastTourId) {

               // get additional tags from outer join

               if (dbTagId instanceof Long) {
                  tagIds.add((Long) dbTagId);
               }

            } else {

               // get first record for a tour

               allTourIds.add(dbTourId);

// SET_FORMATTING_OFF

               final int dbTourYear                   = result.getShort(2);
               final int dbTourMonth                  = result.getShort(3);
               final int dbTourStartWeek              = result.getInt(4);
               final long dbStartTimeMilli            = result.getLong(5);
               final String dbTimeZoneId              = result.getString(6);
//               final int dbElapsedTime                = result.getInt(7);

               final Object dbTourTypeIdObject        = result.getObject(8);

               final short dbBatteryPercentage_Start  = result.getShort(9);
               final short dbBatteryPercentage_End    = result.getShort(10);

// SET_FORMATTING_ON

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbStartTimeMilli, dbTimeZoneId);
               final ZonedDateTime zonedStartDateTime = tourDateTime.tourZonedDateTime;

               // get number of days for the year, start with 0
               final int tourDOY = zonedStartDateTime.get(ChronoField.DAY_OF_YEAR) - 1;

               zonedStartDateTime.getDayOfMonth();

               allTourYear.add(dbTourYear);
               allTourMonths.add(dbTourMonth);
               allTourDays.add(zonedStartDateTime.getDayOfMonth());

               allYearsDOY.add(getYearDOYs(dbTourYear) + tourDOY);
               allTourStartWeek.add(dbTourStartWeek);

               // tour start date/time
               allTourStartDateTime.add(zonedStartDateTime);
               allTourTimeOffset.add(tourDateTime.timeZoneOffsetLabel);

               allBatteryPercentage_Start.add(dbBatteryPercentage_Start);
               allBatteryPercentage_End.add(dbBatteryPercentage_End);

//               final int batteryDiff = dbBatteryPercentage_Start - dbBatteryPercentage_End;
//               final float batteryPerformance = batteryDiff < 20
//
//                     // ignore values which are too small because these values have a higher error
//                     ? lastBatteryPerformance
//
//                     : dbElapsedTime / batteryDiff;
//
//               lastBatteryPerformance = batteryPerformance;
//
//               allBatteryPercentage_Start.add((short) batteryPerformance);

               if (dbTagId instanceof Long) {
                  tagIds = new ArrayList<>();
                  tagIds.add((Long) dbTagId);

                  allTagIds.put(dbTourId, tagIds);
               }

               /*
                * Convert type id to the type index in the tour type array, this is also the
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
               allTypeColorIndex.add(colorIndex);

               // paint graph with 1st tour type color
//             allTypeColorIndex.add(0);

               allTypeIds.add(dbTypeId);
            }

            lastTourId = dbTourId;
         }

         // get number of days for all years
         int numDaysInAllYears = 0;
         for (final int doy : allYear_NumDays) {
            numDaysInAllYears += doy;
         }

         /*
          * create data
          */
         _batteryData = new TourStatisticData_Battery();

         _batteryData.allTourIds = allTourIds.toArray();

         _batteryData.allTypeIds = allTypeIds.toArray();
         _batteryData.allTypeColorIndices = allTypeColorIndex.toArray();

         _batteryData.allTagIds = allTagIds;

         _batteryData.numDaysInAllYears = numDaysInAllYears;
         _batteryData.allYear_NumDays = allYear_NumDays;
         _batteryData.allYear_Numbers = allYear_Numbers;

         _batteryData.allTourYears = allTourYear.toArray();
         _batteryData.allTourMonths = allTourMonths.toArray();
         _batteryData.allTourDays = allTourDays.toArray();

         _batteryData.allTourDOYs = allYearsDOY.toArray();
         _batteryData.allWeeks = allTourStartWeek.toArray();

         _batteryData.allTourTimeZoneOffsets = allTourTimeOffset;
         _batteryData.allTourStartDateTimes = allTourStartDateTime;

         _batteryData.allBatteryPercentage_Start = allBatteryPercentage_Start.toArray();
         _batteryData.allBatteryPercentage_End = allBatteryPercentage_End.toArray();

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _batteryData;
   }

}
