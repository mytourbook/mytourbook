/*******************************************************************************
 * Copyright (C) 2011, 2022 Matthias Helmling and Contributors
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
package net.tourbook.ui.views.calendar;

import gnu.trove.list.array.TFloatArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.tour.filter.TourTagFilterManager;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;

public class CalendarTourDataProvider {

   private static final char                            NL                = UI.NEW_LINE;

   private static CalendarTourDataProvider              _instance;

   private static final AtomicLong                      _weekExecuterId   = new AtomicLong();
   private static final LinkedBlockingDeque<WeekLoader> _weekWaitingQueue = new LinkedBlockingDeque<>();

   private static ThreadPoolExecutor                    _weekLoadingExecutor;
   static {

      final ThreadFactory threadFactoryFolder = runnable -> {

         final Thread thread = new Thread(runnable, "LoadingCalendarData");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      _weekLoadingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10, threadFactoryFolder);
   }

   private CalendarGraph                            _calendarGraph;

   private HashMap<Integer, CalendarTourData[][][]> _dayCache  = new HashMap<>();
   private HashMap<Integer, CalendarTourData[]>     _weekCache = new HashMap<>();

   private LocalDateTime                            _firstTourDateTime;
   private Long                                     _firstTourId;

   private CalendarTourDataProvider() {
      invalidate();
   }

   static CalendarTourDataProvider getInstance() {

      if (_instance == null) {
         _instance = new CalendarTourDataProvider();
      }

      return _instance;
   }

   CalendarTourData[] getCalendarDayData(final LocalDate currentDate) {

      final int year = currentDate.getYear();
      final int month = currentDate.getMonthValue();
      final int day = currentDate.getDayOfMonth();

      if (!_dayCache.containsKey(year)) {

         // create year data
         _dayCache.put(year, new CalendarTourData[12][][]);
      }

      CalendarTourData[][] monthData = _dayCache.get(year)[month - 1];

      if (monthData == null) {

         // load month data

         final CalendarTourData[][] loadedMonthData = loadFromDb_Month(year, month);

         _dayCache.get(year)[month - 1] = loadedMonthData;

         monthData = loadedMonthData;
      }

      final CalendarTourData[] dayData = monthData[day - 1];

      return dayData;

   }

   LocalDateTime getCalendarTourDateTime(final Long tourId) {

      LocalDateTime dt = LocalDateTime.now();

      final String select =

            "SELECT" //                               //$NON-NLS-1$

                  + " StartYear," + NL //          1  //$NON-NLS-1$
                  + " StartMonth," + NL //         2  //$NON-NLS-1$
                  + " StartDay," + NL //           3  //$NON-NLS-1$
                  + " StartHour," + NL //          4  //$NON-NLS-1$
                  + " StartMinute" //              5  //$NON-NLS-1$

                  + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

                  + " WHERE TourId=?" + NL; //        //$NON-NLS-1$

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(select);

         statement.setLong(1, tourId);
         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int year = result.getShort(1);
            final int month = result.getShort(2);
            final int day = result.getShort(3);
            final int hour = result.getShort(4);
            final int minute = result.getShort(5);

            dt = LocalDateTime.of(year, month, day, hour, minute);
         }

      } catch (final SQLException e) {

         StatusUtil.logError(select);
         net.tourbook.ui.UI.showSQLException(e);

      }

      return dt;
   }

   public CalendarTourData getCalendarWeekSummaryData(final LocalDate week1stDay) {

      final WeekFields cw = TimeTools.calendarWeek;

      final int year = week1stDay.get(cw.weekBasedYear());
      final int week = week1stDay.get(cw.weekOfWeekBasedYear());

      if (!_weekCache.containsKey(year)) {

         /*
          * Create year, weeks are from 1..53; we simply leave array index 0 unused (yes, a year
          * can have more than 52 weeks)
          */
         _weekCache.put(year, new CalendarTourData[54]);
      }

      CalendarTourData weekData;

      final CalendarTourData cachedWeekData = _weekCache.get(year)[week];

      if (cachedWeekData == null) {

         weekData = getCalendarWeekSummaryData_FromDb(week1stDay, year, week);

         _weekCache.get(year)[week] = weekData;

      } else if (cachedWeekData.loadingState == LoadingState.NOT_LOADED) {

         // load again

         weekData = getCalendarWeekSummaryData_FromDb(week1stDay, year, week);

         // update cached data otherwise an endless loop occurs !!!
         _weekCache.get(year)[week] = weekData;

      } else {

         weekData = cachedWeekData;
      }

      return weekData;

   }

   private CalendarTourData getCalendarWeekSummaryData_FromDb(final LocalDate week1stDay,
                                                              final int year,
                                                              final int week) {

      final CalendarTourData weekData = new CalendarTourData();

      weekData.loadingState = LoadingState.IS_QUEUED;

      _weekWaitingQueue.add(new WeekLoader(week1stDay, year, week, weekData, _weekExecuterId.get()));

      final Runnable executorTask = () -> {

         // get last added loader item
         final WeekLoader weekLoader = _weekWaitingQueue.pollFirst();

         if (weekLoader == null) {
            return;
         }

         if (loadFromDB_Week(weekLoader)) {
            _calendarGraph.updateUI_AfterDataLoading();
         }
      };

      _weekLoadingExecutor.submit(executorTask);

      return weekData;

   }

   /**
    * @return Returns the date/time of the first available tour
    */
   LocalDateTime getFirstTourDateTime() {

      if (null != _firstTourDateTime) {
         return _firstTourDateTime;
      }

      String select = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         long firstTourStartTime = 0;

         /*
          * Get first tour date/time
          */
         select = "SELECT MIN(TourStartTime)" //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_TOUR_DATA; //$NON-NLS-1$

         PreparedStatement statement = conn.prepareStatement(select);

         ResultSet result = statement.executeQuery();
         while (result.next()) {

            firstTourStartTime = result.getLong(1);

            // this occurs when there are 0 tours
            if (firstTourStartTime != 0) {

               _firstTourDateTime = TimeTools.toLocalDateTime(firstTourStartTime);

               break;
            }
         }

         /*
          * Get first tour id
          */
         if (_firstTourDateTime != null) {

            select = "SELECT TourId" //$NON-NLS-1$

                  + " FROM " + TourDatabase.TABLE_TOUR_DATA //$NON-NLS-1$
                  + " WHERE TourStartTime=" + firstTourStartTime; //$NON-NLS-1$

            statement = conn.prepareStatement(select);

            result = statement.executeQuery();
            while (result.next()) {

               _firstTourId = result.getLong(1);

               break;
            }
         }

      } catch (final SQLException e) {

         StatusUtil.logError(select);
         net.tourbook.ui.UI.showSQLException(e);
      }

      if (_firstTourDateTime == null) {
         _firstTourDateTime = LocalDateTime.now().minusMonths(1);
      }

      return _firstTourDateTime;

   }

   /**
    * @return Returns first tour ID or <code>null</code> when a tour is not available.
    */
   Long getFirstTourId() {
      return _firstTourId;
   }

   /**
    * @return Returns todays tour ID or <code>null</code> when a tour is not available.
    */
   Long getTodaysTourId() {

      Long todayTourId = null;
      String select = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final LocalDate today = LocalDate.now();

         select = NL

               + "SELECT" + NL //                                          //$NON-NLS-1$

               + "   TourId" + NL //                                       //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //            //$NON-NLS-1$

               + "WHERE  StartYear=" + today.getYear() + NL //             //$NON-NLS-1$
               + "   AND StartMonth=" + today.getMonthValue() + NL //      //$NON-NLS-1$
               + "   AND StartDay=" + today.getDayOfMonth() + NL //        //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(select);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            todayTourId = result.getLong(1);

            break;
         }

      } catch (final SQLException e) {

         StatusUtil.logError(select);
         net.tourbook.ui.UI.showSQLException(e);

      }

      return todayTourId;
   }

   synchronized void invalidate() {

      // reset all cached data

      // stop week downloader
      _weekExecuterId.incrementAndGet();

      _dayCache.clear();
      _weekCache.clear();

      _firstTourDateTime = null;
      _firstTourId = null;
   }

   /**
    * Retrieve data for 1 month from the database
    *
    * @param year
    * @param month
    * @param day
    * @return CalendarTourData
    */
   private CalendarTourData[][] loadFromDb_Month(final int year, final int month) {

//      final long start = System.currentTimeMillis();

      CalendarTourData[][] monthData = null;
      CalendarTourData[] dayData = null;

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final int colorOffset = 1;

         final ArrayList<TourType> tourTypeList = TourDatabase.getAllTourTypes();
         final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

         ArrayList<String> dbTourTitle = null;
         ArrayList<String> dbTourDescription = null;

         ArrayList<Integer> dbTourYear = null;
         ArrayList<Integer> dbTourMonth = null;
         ArrayList<Integer> dbTourDay = null;

         ArrayList<Integer> dbTourStartTime = null;
         ArrayList<Integer> dbTourEndTime = null;
         ArrayList<Integer> dbTourStartWeek = null;

         ArrayList<Integer> dbDistance = null;
         ArrayList<Integer> dbElevationGain = null;
         ArrayList<Integer> dbElevationLoss = null;
         ArrayList<Integer> dbTourElapsedTime = null;
         ArrayList<Integer> dbTourDeviceTime_Recorded = null;
         ArrayList<Integer> dbTourMovingTime = null;

         ArrayList<String> dbTourWeatherClouds = null;

         ArrayList<Integer> dbCalories = null;
         TFloatArrayList dbPowerAvg = null;
         TFloatArrayList dbPulseAvg = null;

         ArrayList<Long> dbTypeIds = null;
         ArrayList<Integer> dbTypeColorIndex = null;

         ArrayList<Long> tourIds = null;

         ArrayList<Boolean> dbIsManualTour = null;

         HashMap<Long, ArrayList<Long>> dbTagIds = null;

         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         sql = NL

               + "SELECT" + NL //                                    //$NON-NLS-1$

               + "   TourId," + NL //                             1  //$NON-NLS-1$
               + "   StartYear," + NL //                          2  //$NON-NLS-1$
               + "   StartMonth," + NL //                         3  //$NON-NLS-1$
               + "   StartDay," + NL //                           4  //$NON-NLS-1$
               + "   StartHour," + NL //                          5  //$NON-NLS-1$
               + "   StartMinute," + NL //                        6  //$NON-NLS-1$
               + "   TourDistance," + NL //                       7  //$NON-NLS-1$
               + "   TourAltUp," + NL //                          8  //$NON-NLS-1$
               + "   TourDeviceTime_Elapsed," + NL //             9  //$NON-NLS-1$
               + "   TourComputedTime_Moving," + NL //                    10 //$NON-NLS-1$
               + "   TourTitle," + NL //                          11 //$NON-NLS-1$
               + "   TourType_typeId," + NL //                    12 //$NON-NLS-1$
               + "   TourDescription," + NL //                    13 //$NON-NLS-1$
               + "   StartWeek," + NL //                          14 //$NON-NLS-1$
               + "   DevicePluginId," + NL //                     15 //$NON-NLS-1$
               + "   Calories," + NL //                           16 //$NON-NLS-1$

               + "   jTdataTtag.TourTag_tagId," + NL //           17 //$NON-NLS-1$

               + "   TourAltDown," + NL //                        18 //$NON-NLS-1$
               + "   AvgPulse," + NL //                           19 //$NON-NLS-1$
               + "   Power_Avg," + NL //                          20 //$NON-NLS-1$
               + "   TourDeviceTime_Recorded," + NL //            21 //$NON-NLS-1$
               + "   weather_Clouds" + NL //                      22 //$NON-NLS-1$

               + NL

               + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //      //$NON-NLS-1$

               // get/filter tag's
               + tagFilterSqlJoinBuilder.getSqlTagJoinTable()

               + " AS jTdataTtag" + NL //                            //$NON-NLS-1$
               + " ON tourID = jTdataTtag.TourData_tourId" + NL //   //$NON-NLS-1$

               + "WHERE  StartYear=?" + NL //                        //$NON-NLS-1$
               + "   AND StartMonth=?" + NL //                       //$NON-NLS-1$
               + "   AND StartDay=?" + NL //                         //$NON-NLS-1$

               + sqlAppFilter.getWhereClause()

               + "ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"; //$NON-NLS-1$

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;

         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         // set sql other parameters
         prepStmt.setInt(paramIndex++, year);
         prepStmt.setInt(paramIndex++, month);

         final int dayParamIndex = paramIndex++;

         sqlAppFilter.setParameters(prepStmt, paramIndex++);

         monthData = new CalendarTourData[31][];

         long tourId = -1;

         for (int day = 0; day < 31; day++) {

            prepStmt.setInt(dayParamIndex, day + 1);

            final ResultSet result = prepStmt.executeQuery();

            boolean firstTourOfDay = true;
            tourIds = new ArrayList<>();

            while (result.next()) { // all tours of this day

               if (firstTourOfDay) {

                  dbTourTitle = new ArrayList<>();
                  dbTourDescription = new ArrayList<>();

                  dbTourYear = new ArrayList<>();
                  dbTourMonth = new ArrayList<>();
                  dbTourDay = new ArrayList<>();

                  dbTourStartTime = new ArrayList<>();
                  dbTourEndTime = new ArrayList<>();
                  dbTourStartWeek = new ArrayList<>();

                  dbDistance = new ArrayList<>();
                  dbElevationGain = new ArrayList<>();
                  dbElevationLoss = new ArrayList<>();
                  dbTourElapsedTime = new ArrayList<>();
                  dbTourDeviceTime_Recorded = new ArrayList<>();
                  dbTourMovingTime = new ArrayList<>();

                  dbTourWeatherClouds = new ArrayList<>();

                  dbCalories = new ArrayList<>();
                  dbPowerAvg = new TFloatArrayList();
                  dbPulseAvg = new TFloatArrayList();

                  dbTypeIds = new ArrayList<>();
                  dbTypeColorIndex = new ArrayList<>();

                  dbIsManualTour = new ArrayList<>();
                  dbTagIds = new HashMap<>();

                  firstTourOfDay = false;
               }

               tourId = result.getLong(1);
               final Object dbTagId = result.getObject(17);

               if (tourId == lastTourId) {

                  // get additional tags from outer join
                  if (dbTagId instanceof Long) {
                     tagIds.add((Long) dbTagId);
                  }

               } else {

                  // get first record for a tour
                  tourIds.add(tourId);

                  final int tourYear = result.getShort(2);
                  final int tourMonth = result.getShort(3) - 1;
                  final int tourDay = result.getShort(4);
                  final int startHour = result.getShort(5);
                  final int startMinute = result.getShort(6);
                  final int startTime = startHour * 3600 + startMinute * 60;

                  final int elapsedTime = result.getInt(9);
                  final int recordedTime = result.getInt(21);

                  dbTourYear.add(tourYear);
                  dbTourMonth.add(tourMonth);
                  dbTourDay.add(tourDay);

                  dbTourStartTime.add(startTime);
                  dbTourEndTime.add((startTime + elapsedTime));

                  dbDistance.add(result.getInt(7));
                  dbElevationGain.add(result.getInt(8));

                  dbTourElapsedTime.add(elapsedTime);
                  dbTourDeviceTime_Recorded.add(recordedTime);
                  dbTourMovingTime.add(result.getInt(10));

                  dbTourTitle.add(result.getString(11));

                  final String description = result.getString(13);
                  dbTourDescription.add(description == null ? UI.EMPTY_STRING : description);

                  dbTourStartWeek.add(result.getInt(14));

                  // is manual tour
                  final String devicePluginId = result.getString(15);
                  final boolean isManualTour = TourData.DEVICE_ID_FOR_MANUAL_TOUR.equals(devicePluginId)
                        || TourData.DEVICE_ID_CSV_TOUR_DATA_READER.equals(devicePluginId);
                  dbIsManualTour.add(isManualTour);

                  dbCalories.add(result.getInt(16));

                  if (dbTagId instanceof Long) {

                     tagIds = new ArrayList<>();
                     tagIds.add((Long) dbTagId);

                     dbTagIds.put(tourId, tagIds);
                  }

                  dbElevationLoss.add(result.getInt(18));
                  dbPulseAvg.add(result.getFloat(19));
                  dbPowerAvg.add(result.getFloat(20));

                  dbTourWeatherClouds.add(result.getString(22));

                  /*
                   * convert type id to the type index in the tour type array, this is also
                   * the color index for the tour type
                   */
                  int tourTypeColorIndex = 0;
                  final Long dbTypeIdObject = (Long) result.getObject(12);
                  if (dbTypeIdObject != null) {
                     final long dbTypeId = result.getLong(12);
                     for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
                        if (tourTypes[typeIndex].getTypeId() == dbTypeId) {
                           tourTypeColorIndex = colorOffset + typeIndex;
                           break;
                        }
                     }
                  }

                  dbTypeColorIndex.add(tourTypeColorIndex);
                  dbTypeIds.add(dbTypeIdObject == null ? TourDatabase.ENTITY_IS_NOT_SAVED : dbTypeIdObject);
               }

               lastTourId = tourId;

            } // while result.next() == all tours of this day

            /*
             * create data for this day
             */
            final int numTours = tourIds.size();
            dayData = new CalendarTourData[numTours];

            for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

               final CalendarTourData data = new CalendarTourData();

               tourId = tourIds.get(tourIndex);

               data.tourId = tourId;

               data.typeId = dbTypeIds.get(tourIndex);
               data.typeColorIndex = dbTypeColorIndex.get(tourIndex);

               data.tagIds = dbTagIds.get(tourId);

               data.year = dbTourYear.get(tourIndex);
               data.month = dbTourMonth.get(tourIndex);
               data.day = dbTourDay.get(tourIndex);
               data.week = dbTourStartWeek.get(tourIndex);

               data.startTime = dbTourStartTime.get(tourIndex);
               data.endTime = dbTourEndTime.get(tourIndex);

               data.distance = dbDistance.get(tourIndex);
               data.elevationGain = dbElevationGain.get(tourIndex);
               data.elevationLoss = dbElevationLoss.get(tourIndex);

               data.elapsedTime = dbTourElapsedTime.get(tourIndex);
               data.recordedTime = dbTourDeviceTime_Recorded.get(tourIndex);
               data.movingTime = dbTourMovingTime.get(tourIndex);

               data.calories = dbCalories.get(tourIndex);
               data.power_Avg = dbPowerAvg.get(tourIndex);
               data.pulse_Avg = dbPulseAvg.get(tourIndex);

               data.tourTitle = dbTourTitle.get(tourIndex);
               data.tourDescription = dbTourDescription.get(tourIndex);

               data.weatherClouds = dbTourWeatherClouds.get(tourIndex);

               final LocalDate tourDate = LocalDate.of(year, month, data.day);
               data.tourDate = tourDate;
               data.dayOfWeek = tourDate.getDayOfWeek().getValue();

               data.isManualTour = dbIsManualTour.get(tourIndex);

               dayData[tourIndex] = data;

               if (UI.IS_SCRAMBLE_DATA) {

                  data.tourTitle = UI.scrambleText(data.tourTitle);
                  data.tourDescription = UI.scrambleText(data.tourDescription);

                  data.distance = UI.scrambleNumbers(data.distance);
                  data.elevationGain = UI.scrambleNumbers(data.elevationGain);
                  data.elevationLoss = UI.scrambleNumbers(data.elevationLoss);
                  data.calories = UI.scrambleNumbers(data.calories);

                  data.elapsedTime = UI.scrambleNumbers(data.elapsedTime);
                  data.movingTime = UI.scrambleNumbers(data.movingTime);
                  data.recordedTime = UI.scrambleNumbers(data.recordedTime);
               }

            } // create data for this day

            monthData[day] = dayData;

         } // for days 0 .. 30

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         net.tourbook.ui.UI.showSQLException(e);

      }

//      System.out.println(
//            (UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") +
//                  "getCalendarMonthData_FromDb\t\t\t" + (System.currentTimeMillis() - start) + " ms - "
//                  + "\t" + year + " " + month);
//      // TODO remove SYSTEM.OUT.PRINTLN

      return monthData;
   }

   private boolean loadFromDB_Week(final WeekLoader weekLoader) {

      if (weekLoader.executorId < _weekExecuterId.get()) {

         // current executer was invalidated

         // reset loading state
         weekLoader.weekData.loadingState = LoadingState.NOT_LOADED;

         return false;
      }

//      final long start = System.currentTimeMillis();

      final CalendarTourData weekData = weekLoader.weekData;
      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final int year = weekLoader.year;
         final int week = weekLoader.week;

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         String sqlFromTourData;

         final boolean isTourTagFilterEnabled = TourTagFilterManager.isTourTagFilterEnabled();

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         if (isTourTagFilterEnabled) {

            // filter by tag

            sqlFromTourData = NL

                  + "FROM (" + NL //                                                   //$NON-NLS-1$

                  + "   SELECT" + NL //                                                //$NON-NLS-1$

                  // this is necessary otherwise tours can occur multiple times when a tour contains multiple tags !!!
                  + "      DISTINCT TourId," + NL //                                   //$NON-NLS-1$

                  + "      TourDistance," + NL //                                      //$NON-NLS-1$
                  + "      TourDeviceTime_Elapsed," + NL //                            //$NON-NLS-1$
                  + "      TourComputedTime_Moving," + NL //                           //$NON-NLS-1$
                  + "      TourAltUp," + NL //                                         //$NON-NLS-1$
                  + "      TourAltDown," + NL //                                       //$NON-NLS-1$
                  + "      calories," + NL //                                          //$NON-NLS-1$
                  + "      cadenceZone_SlowTime," + NL //                              //$NON-NLS-1$
                  + "      cadenceZone_FastTime," + NL //                              //$NON-NLS-1$
                  + "      TourDeviceTime_Recorded" + NL //                            //$NON-NLS-1$

                  + "   FROM " + TourDatabase.TABLE_TOUR_DATA + NL //                  //$NON-NLS-1$

                  // get tag id's
                  + "   " + tagFilterSqlJoinBuilder.getSqlTagJoinTable() //$NON-NLS-1$

                  + "   AS jTdataTtag" //                                              //$NON-NLS-1$
                  + "   ON tourId = jTdataTtag.TourData_tourId" + NL //                //$NON-NLS-1$

                  + "   WHERE  startWeekYear=?" + NL //                                //$NON-NLS-1$
                  + "      AND startWeek=?" + NL //                                    //$NON-NLS-1$
                  + "      " + sqlAppFilter.getWhereClause() + NL //$NON-NLS-1$

                  + ") NecessaryNameOtherwiseItDoNotWork" //                           //$NON-NLS-1$

            ;

         } else {

            sqlFromTourData = NL

                  + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

                  + "   WHERE  startWeekYear=?" + NL //                                //$NON-NLS-1$
                  + "      AND startWeek=?" + NL //                                    //$NON-NLS-1$
                  + "      " + sqlAppFilter.getWhereClause(); //$NON-NLS-1$
         }

         sql = "SELECT" + NL //                                                        //$NON-NLS-1$

               + " SUM(1)," + NL //                                                 1  //$NON-NLS-1$
               + " SUM(TourDistance)," + NL //                                      2  //$NON-NLS-1$

               + " SUM(TourDeviceTime_Elapsed)," + NL //                            3  //$NON-NLS-1$
               + " SUM(TourComputedTime_Moving)," + NL //                           4  //$NON-NLS-1$

               + " SUM(TourAltUp)," + NL //                                         5  //$NON-NLS-1$
               + " SUM(TourAltDown)," + NL //                                       6  //$NON-NLS-1$

               + " SUM(calories)," + NL //                                          7  //$NON-NLS-1$

               + " SUM(cadenceZone_SlowTime)," + NL //                              8  //$NON-NLS-1$
               + " SUM(cadenceZone_FastTime)," + NL //                              9  //$NON-NLS-1$

               + " SUM(TourDeviceTime_Recorded)" + NL //                            10 //$NON-NLS-1$

               + sqlFromTourData;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;

         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         prepStmt.setInt(paramIndex++, year);
         prepStmt.setInt(paramIndex++, week);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();

         while (result.next()) {

            weekData.year = year;
            weekData.week = week;

            weekData.numTours = result.getInt(1);
            weekData.distance = result.getInt(2);

            weekData.elapsedTime = result.getInt(3);
            weekData.movingTime = result.getInt(4);

            weekData.elevationGain = result.getInt(5);
            weekData.elevationLoss = result.getInt(6);

            weekData.calories = result.getInt(7);

            weekData.cadenceZone_SlowTime = result.getInt(8);
            weekData.cadenceZone_FastTime = result.getInt(9);

            weekData.recordedTime = result.getInt(10);

            if (UI.IS_SCRAMBLE_DATA) {

               weekData.elevationGain = UI.scrambleNumbers(weekData.elevationGain);
               weekData.elevationLoss = UI.scrambleNumbers(weekData.elevationLoss);
               weekData.distance = UI.scrambleNumbers(weekData.distance);

               weekData.elapsedTime = UI.scrambleNumbers(weekData.elapsedTime);
               weekData.movingTime = UI.scrambleNumbers(weekData.movingTime);

               weekData.calories = UI.scrambleNumbers(weekData.calories);
               weekData.recordedTime = UI.scrambleNumbers(weekData.recordedTime);
            }
         }

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         weekData.loadingState = LoadingState.IS_LOADED;
      }

//      System.out.println("getCalendarWeekSummaryData_FromDb\t" + (System.currentTimeMillis() - start) + " ms");
//      // TODO remove SYSTEM.OUT.PRINTLN

      return true;
   }

   public void setCalendarGraph(final CalendarGraph calendarGraph) {

      _calendarGraph = calendarGraph;
   }

}
