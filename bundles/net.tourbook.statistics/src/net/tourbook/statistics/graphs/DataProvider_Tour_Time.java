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

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Time extends DataProvider {

   private Long          _selectedTourId;

   private TourStatisticData_Time _tourDataTime;

   public String getRawStatisticValues(final boolean isShowSequenceNumbers) {

      if (_tourDataTime == null) {
         return null;
      }

      if (statistic_RawStatisticValues != null && isShowSequenceNumbers == statistic_isShowSequenceNumbers) {
         return statistic_RawStatisticValues;
      }

      final StringBuilder sb = new StringBuilder();

      final String headerLine1 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? HEAD1_DATA_NUMBER : UI.EMPTY_STRING)

            + HEAD1_DATE_YEAR
            + HEAD1_DATE_MONTH
            + HEAD1_DATE_DAY
            + HEAD1_DATE_WEEK

            + HEAD1_TOUR_TYPE

            + HEAD1_DEVICE_TIME_ELAPSED
            + HEAD1_DEVICE_TIME_RECORDED
            + HEAD1_DEVICE_TIME_PAUSED
            + HEAD1_COMPUTED_TIME_MOVING
            + HEAD1_COMPUTED_TIME_BREAK

            + HEAD1_DISTANCE
            + HEAD1_ELEVATION

      ;

      final String headerLine2 = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? HEAD2_DATA_NUMBER : UI.EMPTY_STRING)

            + HEAD2_DATE_YEAR
            + HEAD2_DATE_MONTH
            + HEAD2_DATE_DAY
            + HEAD2_DATE_WEEK

            + HEAD2_TOUR_TYPE

            + HEAD2_DEVICE_TIME_ELAPSED
            + HEAD2_DEVICE_TIME_RECORDED
            + HEAD2_DEVICE_TIME_PAUSED
            + HEAD2_COMPUTED_TIME_MOVING
            + HEAD2_COMPUTED_TIME_BREAK

            + HEAD2_DISTANCE
            + HEAD2_ELEVATION

      ;

      final String valueFormatting = UI.EMPTY_STRING

            + (isShowSequenceNumbers ? VALUE_DATA_NUMBER : "%s") //$NON-NLS-1$

            + VALUE_DATE_YEAR
            + VALUE_DATE_MONTH
            + VALUE_DATE_DAY
            + VALUE_DATE_WEEK

            + VALUE_TOUR_TYPE

            + VALUE_DEVICE_TIME_ELAPSED
            + VALUE_DEVICE_TIME_RECORDED
            + VALUE_DEVICE_TIME_PAUSED
            + VALUE_COMPUTED_TIME_MOVING
            + VALUE_COMPUTED_TIME_BREAK

            + VALUE_DISTANCE
            + VALUE_ELEVATION

      ;

      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final int numDataItems = _tourDataTime.allTourDistances.length;

      // set initial value
      int prevMonth = numDataItems > 0 ? _tourDataTime.allTourMonths[0] : 0;

      int sequenceNumber = 0;

      for (int dataIndex = 0; dataIndex < numDataItems; dataIndex++) {

         final int month = _tourDataTime.allTourMonths[dataIndex];

         // group by month
         if (month != prevMonth) {
            prevMonth = month;
            sb.append(NL);
         }

         final int elapsedTime = _tourDataTime.allTourDeviceTime_Elapsed[dataIndex];
         final int movingTime = _tourDataTime.allTourComputedTime_Moving[dataIndex];
         final int breakTime = elapsedTime - movingTime;

         Object sequenceNumberValue = UI.EMPTY_STRING;
         if (isShowSequenceNumbers) {
            sequenceNumberValue = ++sequenceNumber;
         }

         sb.append(String.format(valueFormatting,

               sequenceNumberValue,

               _tourDataTime.allTourYears[dataIndex],
               month,
               _tourDataTime.allTourDays[dataIndex],
               _tourDataTime.allWeeks[dataIndex],

               TourDatabase.getTourTypeName(_tourDataTime.allTypeIds[dataIndex]),

               elapsedTime,
               _tourDataTime.allTourDeviceTime_Recorded[dataIndex],
               _tourDataTime.allTourDeviceTime_Paused[dataIndex],
               movingTime,
               breakTime,

               _tourDataTime.allTourDistances[dataIndex],
               _tourDataTime.allTourElevations[dataIndex]

         ));

         sb.append(NL);
      }

      // cache values
      statistic_RawStatisticValues = sb.toString();
      statistic_isShowSequenceNumbers = isShowSequenceNumbers;

      return statistic_RawStatisticValues;
   }

   public Long getSelectedTourId() {
      return _selectedTourId;
   }

   /**
    * Retrieve chart data from the database
    *
    * @param person
    * @param tourTypeFilter
    * @param lastYear
    * @param numberOfYears
    * @param isForceUpdate
    * @return
    */
   TourStatisticData_Time getTourTimeData(final TourPerson person,
                                 final TourTypeFilter tourTypeFilter,
                                 final int lastYear,
                                 final int numberOfYears,
                                 final boolean isForceUpdate) {

      // don't reload data which are already here
      if (statistic_ActivePerson == person
            && statistic_ActiveTourTypeFilter == tourTypeFilter
            && statistic_LastYear == lastYear
            && statistic_NumberOfYears == numberOfYears
            && isForceUpdate == false) {

         return _tourDataTime;
      }

      // reset cached values
      statistic_RawStatisticValues = null;

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statistic_ActivePerson = person;
         statistic_ActiveTourTypeFilter = tourTypeFilter;

         statistic_LastYear = lastYear;
         statistic_NumberOfYears = numberOfYears;

         setupYearNumbers();

         int colorOffset = 0;
         if (tourTypeFilter.showUndefinedTourTypes()) {
            colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
         }

         final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
         final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                   //$NON-NLS-1$

               + "   TourId," + NL //                                            1  //$NON-NLS-1$

               + "   StartYear," + NL //                                         2  //$NON-NLS-1$
               + "   StartMonth," + NL //                                        3  //$NON-NLS-1$
               + "   StartWeek," + NL //                                         4  //$NON-NLS-1$

               + "   TourStartTime," + NL //                                     5  //$NON-NLS-1$
               + "   TimeZoneId," + NL //                                        6  //$NON-NLS-1$

               + "   TourDeviceTime_Elapsed," + NL //                            7  //$NON-NLS-1$
               + "   TourDeviceTime_Recorded," + NL //                           8  //$NON-NLS-1$
               + "   TourDeviceTime_Paused," + NL //                             9  //$NON-NLS-1$
               + "   TourComputedTime_Moving," + NL //                           10 //$NON-NLS-1$

               + "   TourDistance," + NL //                                      11 //$NON-NLS-1$
               + "   TourAltUp," + NL //                                         12 //$NON-NLS-1$

               + "   TourTitle," + NL //                                         13 //$NON-NLS-1$
               + "   TourDescription," + NL //                                   14 //$NON-NLS-1$

               + "   TourType_typeId," + NL //                                   15 //$NON-NLS-1$
               + "   jTdataTtag.TourTag_tagId" + NL //                           16 //$NON-NLS-1$

               + " FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE //           //$NON-NLS-1$

               // get/filter tag id's
               + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //    //$NON-NLS-1$
               + " ON TourId = jTdataTtag.TourData_tourId" + NL //                  //$NON-NLS-1$

               + " WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + UI.NEW_LINE //$NON-NLS-1$ //$NON-NLS-2$
               + "   " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

               + " ORDER BY TourStartTime"; //                                      //$NON-NLS-1$

         final TLongArrayList allTourIds = new TLongArrayList();

         final TIntArrayList allTourYear = new TIntArrayList();
         final TIntArrayList allTourMonths = new TIntArrayList();
         final TIntArrayList allTourDays = new TIntArrayList();
         final TIntArrayList allYearsDOY = new TIntArrayList(); // DOY...Day Of Year for all years

         final TIntArrayList allTourStartTime = new TIntArrayList();
         final TIntArrayList allTourEndTime = new TIntArrayList();
         final TIntArrayList allTourStartWeek = new TIntArrayList();
         final ArrayList<ZonedDateTime> allTourStartDateTime = new ArrayList<>();
         final ArrayList<String> allTourTimeOffset = new ArrayList<>();

         final TIntArrayList allTourDeviceTime_Elapsed = new TIntArrayList();
         final TIntArrayList allTourDeviceTime_Recorded = new TIntArrayList();
         final TIntArrayList allTourDeviceTime_Paused = new TIntArrayList();
         final TIntArrayList allTourComputedTime_Moving = new TIntArrayList();

         final TFloatArrayList allDistances = new TFloatArrayList();
         final TFloatArrayList allElevationUp = new TFloatArrayList();

         final ArrayList<String> allTourTitle = new ArrayList<>();
         final ArrayList<String> allTourDescription = new ArrayList<>();

         final TLongArrayList allTypeIds = new TLongArrayList();
         final TIntArrayList allTypeColorIndex = new TIntArrayList();

         final HashMap<Long, ArrayList<Long>> allTagIds = new HashMap<>();

         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();
         while (result.next()) {

            final long dbTourId = result.getLong(1);
            final Object dbTagId = result.getObject(15);

            if (dbTourId == lastTourId) {

               // get additional tags from outer join

               if (dbTagId instanceof Long) {
                  tagIds.add((Long) dbTagId);
               }

            } else {

               // get first record for a tour

               allTourIds.add(dbTourId);

// SET_FORMATTING_OFF

               final int dbTourYear             = result.getShort(2);
               final int dbTourMonth            = result.getShort(3);

               final int dbTourStartWeek        = result.getInt(4);

               final long dbStartTimeMilli      = result.getLong(5);
               final String dbTimeZoneId        = result.getString(6);

               final int dbElapsedTime          = result.getInt(7);
               final int dbRecordedTime         = result.getInt(8);
               final int dbPausedTime           = result.getInt(9);
               final int dbMovingTime           = result.getInt(10);

               final float dbDistance           = result.getFloat(11);
               final int dbAltitudeUp           = result.getInt(12);

               final String dbTourTitle         = result.getString(13);
               final String dbDescription       = result.getString(14);

               final Object dbTypeIdObject      = result.getObject(15);

// SET_FORMATTING_ON

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbStartTimeMilli, dbTimeZoneId);
               final ZonedDateTime zonedStartDateTime = tourDateTime.tourZonedDateTime;

               // get number of days for the year, start with 0
               final int tourDOY = zonedStartDateTime.get(ChronoField.DAY_OF_YEAR) - 1;

               zonedStartDateTime.getDayOfMonth();

               final int startDayTime = (zonedStartDateTime.getHour() * 3600)
                     + (zonedStartDateTime.getMinute() * 60)
                     + zonedStartDateTime.getSecond();

               allTourYear.add(dbTourYear);
               allTourMonths.add(dbTourMonth);
               allTourDays.add(zonedStartDateTime.getDayOfMonth());

               allYearsDOY.add(getYearDOYs(dbTourYear) + tourDOY);
               allTourStartWeek.add(dbTourStartWeek);

               allTourStartDateTime.add(zonedStartDateTime);
               allTourTimeOffset.add(tourDateTime.timeZoneOffsetLabel);
               allTourStartTime.add(startDayTime);
               allTourEndTime.add((startDayTime + dbRecordedTime));

               allTourDeviceTime_Elapsed.add(dbElapsedTime);
               allTourDeviceTime_Recorded.add(dbRecordedTime);
               allTourDeviceTime_Paused.add(dbPausedTime);
               allTourComputedTime_Moving.add(dbMovingTime);

               allDistances.add(dbDistance / UI.UNIT_VALUE_DISTANCE);
               allElevationUp.add(dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE);

               allTourTitle.add(dbTourTitle);

               allTourDescription.add(dbDescription == null ? UI.EMPTY_STRING : dbDescription);

               if (dbTagId instanceof Long) {
                  tagIds = new ArrayList<>();
                  tagIds.add((Long) dbTagId);

                  allTagIds.put(dbTourId, tagIds);
               }

               /*
                * convert type id to the type index in the tour type array, this is also the
                * color index for the tour type
                */
               int colorIndex = 0;
               long dbTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

               if (dbTypeIdObject instanceof Long) {

                  dbTypeId = (Long) dbTypeIdObject;

                  for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
                     if (tourTypes[typeIndex].getTypeId() == dbTypeId) {
                        colorIndex = colorOffset + typeIndex;
                        break;
                     }
                  }
               }

               allTypeColorIndex.add(colorIndex);
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
         _tourDataTime = new TourStatisticData_Time();

         _tourDataTime.allTourIds = allTourIds.toArray();

         _tourDataTime.allTypeIds = allTypeIds.toArray();
         _tourDataTime.allTypeColorIndices = allTypeColorIndex.toArray();

         _tourDataTime.allTagIds = allTagIds;

         _tourDataTime.numDaysInAllYears = numDaysInAllYears;
         _tourDataTime.allYear_NumDays = allYear_NumDays;
         _tourDataTime.allYear_Numbers = allYear_Numbers;

         _tourDataTime.allTourYears = allTourYear.toArray();
         _tourDataTime.allTourMonths = allTourMonths.toArray();
         _tourDataTime.allTourDays = allTourDays.toArray();

         _tourDataTime.allTourDOYs = allYearsDOY.toArray();
         _tourDataTime.allWeeks = allTourStartWeek.toArray();

         _tourDataTime.allTourTimeStart = allTourStartTime.toArray();
         _tourDataTime.allTourTimeEnd = allTourEndTime.toArray();
         _tourDataTime.allTourTimeZoneOffsets = allTourTimeOffset;
         _tourDataTime.allTourStartDateTimes = allTourStartDateTime;

         _tourDataTime.allTourDistances = allDistances.toArray();
         _tourDataTime.allTourElevations = allElevationUp.toArray();

         _tourDataTime.allTourDeviceTime_Elapsed = allTourDeviceTime_Elapsed.toArray();
         _tourDataTime.allTourDeviceTime_Recorded = allTourDeviceTime_Recorded.toArray();
         _tourDataTime.allTourDeviceTime_Paused = allTourDeviceTime_Paused.toArray();
         _tourDataTime.allTourComputedTime_Moving = allTourComputedTime_Moving.toArray();

         _tourDataTime.allTourTitles = allTourTitle;
         _tourDataTime.allTourDescriptions = allTourDescription;

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourDataTime;
   }

   void setSelectedTourId(final Long selectedTourId) {
      _selectedTourId = selectedTourId;
   }
}
