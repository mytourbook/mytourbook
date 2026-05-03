/*******************************************************************************
 * Copyright (C) 2011, 2026 Matthias Helmling and Contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

class CalendarTourDataProvider {

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

      final String sql =

            "SELECT" //                               //$NON-NLS-1$

                  + " StartYear," + NL //          1  //$NON-NLS-1$
                  + " StartMonth," + NL //         2  //$NON-NLS-1$
                  + " StartDay," + NL //           3  //$NON-NLS-1$
                  + " StartHour," + NL //          4  //$NON-NLS-1$
                  + " StartMinute" //              5  //$NON-NLS-1$

                  + " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

                  + " WHERE TourId=?" + NL; //        //$NON-NLS-1$

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);

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

         SQL.showException(e, sql);
      }

      return dt;
   }

   CalendarTourData getCalendarWeekSummaryData(final LocalDate week1stDay) {

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
    *
    * @return CalendarTourData
    */
   private CalendarTourData[][] loadFromDb_Month(final int year, final int month) {

//      final long start = System.currentTimeMillis();

      CalendarTourData[][] monthData = null;
      CalendarTourData[] dayData = null;

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final int colorOffset = 1;

// SET_FORMATTING_OFF

         final ArrayList<TourType> allTourTypeList       = TourDatabase.getAllTourTypes();
         final TourType[] allTourTypes                   = allTourTypeList.toArray(new TourType[allTourTypeList.size()]);

         final List<String> allTourTitle                 = new ArrayList<>();
         final List<String> allTourDescription           = new ArrayList<>();

         final List<Integer> allTourYear                 = new ArrayList<>();
         final List<Integer> allTourMonth                = new ArrayList<>();
         final List<Integer> allTourDay                  = new ArrayList<>();

         final List<Integer> allTourStartTime            = new ArrayList<>();
         final List<Integer> allTourEndTime              = new ArrayList<>();
         final List<Integer> allTourStartWeek            = new ArrayList<>();

         final List<Integer> allDistance                 = new ArrayList<>();
         final List<Integer> allElevationGain            = new ArrayList<>();
         final List<Integer> allElevationLoss            = new ArrayList<>();
         final List<Integer> allTourElapsedTime          = new ArrayList<>();
         final List<Integer> allTourDeviceTime_Recorded  = new ArrayList<>();
         final List<Integer> allTourMovingTime           = new ArrayList<>();

         final List<String> allTourWeatherClouds         = new ArrayList<>();

         final List<Integer> allCalories                 = new ArrayList<>();
         final FloatArrayList allPowerAvg                = new FloatArrayList();
         final FloatArrayList allPulseAvg                = new FloatArrayList();

         final List<Long> allTypeIds                     = new ArrayList<>();
         final List<Integer> allTypeColorIndex           = new ArrayList<>();

         List<Long> allTourIDs                           = new ArrayList<>();

         final ArrayList<Boolean> allIsManualTour        = new ArrayList<>();

// SET_FORMATTING_ON

         final AppFilter appFilter = new AppFilter(AppFilter.ANY_APP_FILTERS);

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
               + "   TourComputedTime_Moving," + NL //            10 //$NON-NLS-1$
               + "   TourTitle," + NL //                          11 //$NON-NLS-1$
               + "   TourType_typeId," + NL //                    12 //$NON-NLS-1$
               + "   TourDescription," + NL //                    13 //$NON-NLS-1$
               + "   StartWeek," + NL //                          14 //$NON-NLS-1$
               + "   DevicePluginId," + NL //                     15 //$NON-NLS-1$
               + "   Calories," + NL //                           16 //$NON-NLS-1$

               + "   TourAltDown," + NL //                        17 //$NON-NLS-1$
               + "   AvgPulse," + NL //                           18 //$NON-NLS-1$
               + "   Power_Avg," + NL //                          19 //$NON-NLS-1$
               + "   TourDeviceTime_Recorded," + NL //            20 //$NON-NLS-1$
               + "   weather_Clouds" + NL //                      21 //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //      //$NON-NLS-1$

               + "WHERE  StartYear  = ?" + NL //                     //$NON-NLS-1$
               + "   AND StartMonth = ?" + NL //                     //$NON-NLS-1$
               + "   AND StartDay   = ?" + NL //                     //$NON-NLS-1$

               + appFilter.getWhereClause()

               + "ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"; //$NON-NLS-1$

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int nextIndex = 1;

         prepStmt.setInt(nextIndex++, year);
         prepStmt.setInt(nextIndex++, month);

         final int dayParamIndex = nextIndex++;

         nextIndex = appFilter.setParameters(prepStmt, nextIndex);

         monthData = new CalendarTourData[31][];

         for (int day = 0; day < 31; day++) {

            prepStmt.setInt(dayParamIndex, day + 1);

            final ResultSet result = prepStmt.executeQuery();

            boolean firstTourOfDay = true;
            allTourIDs = new ArrayList<>();

            // loop: all tours of this day
            while (result.next()) {

               if (firstTourOfDay) {

// SET_FORMATTING_OFF

                  allTourTitle                  .clear();
                  allTourDescription            .clear();

                  allTourYear                   .clear();
                  allTourMonth                  .clear();
                  allTourDay                    .clear();

                  allTourStartTime              .clear();
                  allTourEndTime                .clear();
                  allTourStartWeek              .clear();

                  allDistance                   .clear();
                  allElevationGain              .clear();
                  allElevationLoss              .clear();
                  allTourElapsedTime            .clear();
                  allTourDeviceTime_Recorded    .clear();
                  allTourMovingTime             .clear();

                  allTourWeatherClouds          .clear();

                  allCalories                   .clear();
                  allPowerAvg                   .clear();
                  allPulseAvg                   .clear();

                  allTypeIds                    .clear();
                  allTypeColorIndex             .clear();

                  allIsManualTour               .clear();

// SET_FORMATTING_ON

                  firstTourOfDay = false;
               }

// SET_FORMATTING_OFF

               allTourIDs                 .add(result.getLong(1));

               final int tourYear         = result.getShort(2);
               final int tourMonth        = result.getShort(3) - 1;
               final int tourDay          = result.getShort(4);
               final int startHour        = result.getShort(5);
               final int startMinute      = result.getShort(6);
               final int startTime        = startHour * 3600 + startMinute * 60;

               final int elapsedTime      = result.getInt(9);
               final int recordedTime     = result.getInt(20);

               allTourYear                .add(tourYear);
               allTourMonth               .add(tourMonth);
               allTourDay                 .add(tourDay);

               allTourStartTime           .add(startTime);
               allTourEndTime             .add((startTime + elapsedTime));

               allDistance                .add(result.getInt(7));
               allElevationGain           .add(result.getInt(8));

               allTourElapsedTime         .add(elapsedTime);
               allTourDeviceTime_Recorded .add(recordedTime);
               allTourMovingTime          .add(result.getInt(10));

               allTourTitle               .add(result.getString(11));

               final String description = result.getString(13);
               allTourDescription         .add(description == null ? UI.EMPTY_STRING : description);

               allTourStartWeek           .add(result.getInt(14));

               // is manual tour
               final String devicePluginId = result.getString(15);
               final boolean isManualTour = TourData.DEVICE_ID_FOR_MANUAL_TOUR.equals(devicePluginId)
                     || TourData.DEVICE_ID_CSV_TOUR_DATA_READER.equals(devicePluginId);
               allIsManualTour.add(isManualTour);

               allCalories                .add(result.getInt(16));
               allElevationLoss           .add(result.getInt(17));
               allPulseAvg                .add(result.getFloat(18));
               allPowerAvg                .add(result.getFloat(19));
               allTourWeatherClouds       .add(result.getString(21));

// SET_FORMATTING_ON

               /*
                * Convert type id to the type index in the tour type array, this is also
                * the color index for the tour type
                */
               int tourTypeColorIndex = 0;
               final Long dbTypeIdObject = (Long) result.getObject(12);
               if (dbTypeIdObject != null) {
                  final long dbTypeId = result.getLong(12);
                  for (int typeIndex = 0; typeIndex < allTourTypes.length; typeIndex++) {
                     if (allTourTypes[typeIndex].getTypeId() == dbTypeId) {
                        tourTypeColorIndex = colorOffset + typeIndex;
                        break;
                     }
                  }
               }

               allTypeColorIndex.add(tourTypeColorIndex);
               allTypeIds.add(dbTypeIdObject == null ? TourDatabase.ENTITY_IS_NOT_SAVED : dbTypeIdObject);

            } // while result.next() == all tours of this day

            /*
             * create data for this day
             */
            final int numTours = allTourIDs.size();
            dayData = new CalendarTourData[numTours];

            for (int tourIndex = 0; tourIndex < numTours; tourIndex++) {

               final CalendarTourData data = new CalendarTourData();

// SET_FORMATTING_OFF

               data.tourId          = allTourIDs            .get(tourIndex);

               data.typeId          = allTypeIds            .get(tourIndex);
               data.typeColorIndex  = allTypeColorIndex     .get(tourIndex);

               data.year            = allTourYear           .get(tourIndex);
               data.month           = allTourMonth          .get(tourIndex);
               data.day             = allTourDay            .get(tourIndex);
               data.week            = allTourStartWeek      .get(tourIndex);

               data.startTime       = allTourStartTime      .get(tourIndex);
               data.endTime         = allTourEndTime        .get(tourIndex);

               data.distance        = allDistance           .get(tourIndex);
               data.elevationGain   = allElevationGain      .get(tourIndex);
               data.elevationLoss   = allElevationLoss      .get(tourIndex);

               data.elapsedTime     = allTourElapsedTime    .get(tourIndex);
               data.recordedTime    = allTourDeviceTime_Recorded.get(tourIndex);
               data.movingTime      = allTourMovingTime     .get(tourIndex);

               data.calories        = allCalories           .get(tourIndex);
               data.power_Avg       = allPowerAvg           .get(tourIndex);
               data.pulse_Avg       = allPulseAvg           .get(tourIndex);

               data.tourTitle       = allTourTitle          .get(tourIndex);
               data.tourDescription = allTourDescription    .get(tourIndex);

               data.weatherClouds   = allTourWeatherClouds  .get(tourIndex);

               final LocalDate tourDate = LocalDate.of(year, month, data.day);
               data.tourDate        = tourDate;
               data.dayOfWeek       = tourDate.getDayOfWeek().getValue();

               data.isManualTour    = allIsManualTour       .get(tourIndex);

               dayData[tourIndex] = data;

               if (UI.IS_SCRAMBLE_DATA) {

                  data.tourTitle       = UI.scrambleText(data.tourTitle);
                  data.tourDescription = UI.scrambleText(data.tourDescription);

                  data.distance        = UI.scrambleNumbers(data.distance);
                  data.elevationGain   = UI.scrambleNumbers(data.elevationGain);
                  data.elevationLoss   = UI.scrambleNumbers(data.elevationLoss);
                  data.calories        = UI.scrambleNumbers(data.calories);

                  data.elapsedTime     = UI.scrambleNumbers(data.elapsedTime);
                  data.movingTime      = UI.scrambleNumbers(data.movingTime);
                  data.recordedTime    = UI.scrambleNumbers(data.recordedTime);
               }

// SET_FORMATTING_ON

            } // create data for this day

            monthData[day] = dayData;

         } // for days 0 .. 30

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }

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

         final AppFilter appFilter = new AppFilter(AppFilter.ANY_APP_FILTERS);

         sql = NL

               + "SELECT" + NL //                                       //$NON-NLS-1$

               + " SUM(1)," + NL //                                  1  //$NON-NLS-1$
               + " SUM(TourDistance)," + NL //                       2  //$NON-NLS-1$

               + " SUM(TourDeviceTime_Elapsed)," + NL //             3  //$NON-NLS-1$
               + " SUM(TourComputedTime_Moving)," + NL //            4  //$NON-NLS-1$

               + " SUM(TourAltUp)," + NL //                          5  //$NON-NLS-1$
               + " SUM(TourAltDown)," + NL //                        6  //$NON-NLS-1$

               + " SUM(calories)," + NL //                           7  //$NON-NLS-1$

               + " SUM(cadenceZone_SlowTime)," + NL //               8  //$NON-NLS-1$
               + " SUM(cadenceZone_FastTime)," + NL //               9  //$NON-NLS-1$

               + " SUM(TourDeviceTime_Recorded)," + NL //            10 //$NON-NLS-1$

               + " SUM(power_TrainingStressScore)" + NL //           11 //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //         //$NON-NLS-1$

               + "WHERE StartWeekYear = ?" + NL //                      //$NON-NLS-1$
               + "  AND StartWeek     = ?" + NL //                      //$NON-NLS-1$

               + appFilter.getWhereClause();

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int nextIndex = 1;

         prepStmt.setInt(nextIndex++, year);
         prepStmt.setInt(nextIndex++, week);

         nextIndex = appFilter.setParameters(prepStmt, nextIndex);

         final ResultSet result = prepStmt.executeQuery();

         while (result.next()) {

// SET_FORMATTING_OFF

            weekData.year                 = year;
            weekData.week                 = week;

            weekData.numTours             = result.getInt(1);
            weekData.distance             = result.getInt(2);

            weekData.elapsedTime          = result.getInt(3);
            weekData.movingTime           = result.getInt(4);

            weekData.elevationGain        = result.getInt(5);
            weekData.elevationLoss        = result.getInt(6);

            weekData.calories             = result.getInt(7);

            weekData.cadenceZone_SlowTime = result.getInt(8);
            weekData.cadenceZone_FastTime = result.getInt(9);

            weekData.recordedTime         = result.getInt(10);

            weekData.trainingLoad_Tss     = result.getInt(11);

            if (UI.IS_SCRAMBLE_DATA) {

               weekData.elevationGain = UI.scrambleNumbers(weekData.elevationGain);
               weekData.elevationLoss = UI.scrambleNumbers(weekData.elevationLoss);
               weekData.distance = UI.scrambleNumbers(weekData.distance);

               weekData.elapsedTime = UI.scrambleNumbers(weekData.elapsedTime);
               weekData.movingTime = UI.scrambleNumbers(weekData.movingTime);

               weekData.calories = UI.scrambleNumbers(weekData.calories);
               weekData.recordedTime = UI.scrambleNumbers(weekData.recordedTime);
            }

// SET_FORMATTING_ON
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);

      } finally {

         weekData.loadingState = LoadingState.IS_LOADED;
      }

      return true;
   }

   public void setCalendarGraph(final CalendarGraph calendarGraph) {

      _calendarGraph = calendarGraph;
   }

}
