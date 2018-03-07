/*******************************************************************************
 * Copyright (C) 2011-2018 Matthias Helmling and Contributors
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
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

public class CalendarTourDataProvider {

// SET_FORMATTING_OFF
	
	private static boolean		_isScrambleCalendar	= System.getProperty("scrambleCalendar") != null;	//$NON-NLS-1$
	
// SET_FORMATTING_ON

	private static final char								NL					= UI.NEW_LINE;

	private static CalendarTourDataProvider					_instance;

	private static final AtomicLong							_weekExecuterId		= new AtomicLong();
	private static final LinkedBlockingDeque<WeekLoader>	_weekWaitingQueue	= new LinkedBlockingDeque<>();

	private static ThreadPoolExecutor						_weekLoadingExecutor;
	static {

		final ThreadFactory threadFactoryFolder = new ThreadFactory() {

			@Override
			public Thread newThread(final Runnable r) {

				final Thread thread = new Thread(r, "LoadingCalendarData");//$NON-NLS-1$

				thread.setPriority(Thread.MIN_PRIORITY);
				thread.setDaemon(true);

				return thread;
			}
		};

		_weekLoadingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10, threadFactoryFolder);
	}

	private CalendarGraph								_calendarGraph;

	private HashMap<Integer, CalendarTourData[][][]>	_dayCache	= new HashMap<>();
	private HashMap<Integer, CalendarTourData[]>		_weekCache	= new HashMap<>();

	private LocalDateTime								_firstTourDateTime;
	private Long										_firstTourId;

	private CalendarTourDataProvider() {
		invalidate();
	};

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

			final CalendarTourData[][] loadedMonthData = getCalendarMonthData_FromDb(year, month);

			_dayCache.get(year)[month - 1] = loadedMonthData;

			monthData = loadedMonthData;
		}

		final CalendarTourData[] dayData = monthData[day - 1];

		return dayData;

	}

	/**
	 * Retrieve data for 1 month from the database
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return CalendarTourData
	 */
	private CalendarTourData[][] getCalendarMonthData_FromDb(final int year, final int month) {

//		final long start = System.currentTimeMillis();

		CalendarTourData[][] monthData = null;
		CalendarTourData[] dayData = null;

// disabled because dragged tours are not displayed
//
//		final LocalDate today = LocalDate.now();
//		final LocalDate requestedDate = LocalDate.of(year, month, 1);
//
//		if (
//
//		// requested data are after today -> there should be no data
//		requestedDate.isAfter(today.plusMonths(1))
//
//				// requested data are before first tour
//				|| (_firstTourDateTime != null && requestedDate.isBefore(
//
//						// adjust a month before the last month, otherwise data
//						// for the first month are not all loaded
//						_firstTourDateTime.toLocalDate().minusMonths(1)
//
//				))) {
//
//			/*
//			 * Create dummy data
//			 */
//
//			monthData = new CalendarTourData[31][];
//
//			for (int day = 0; day < 31; day++) {
//				monthData[day] = new CalendarTourData[0];
//			}
//
//			return monthData;
//		}

		final int colorOffset = 1;

		final ArrayList<TourType> tourTypeList = TourDatabase.getAllTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final SQLFilter filter = new SQLFilter(SQLFilter.TAG_FILTER);

		ArrayList<String> dbTourTitle = null;
		ArrayList<String> dbTourDescription = null;

		ArrayList<Integer> dbTourYear = null;
		ArrayList<Integer> dbTourMonth = null;
		ArrayList<Integer> dbTourDay = null;

		ArrayList<Integer> dbTourStartTime = null;
		ArrayList<Integer> dbTourEndTime = null;
		ArrayList<Integer> dbTourStartWeek = null;

		ArrayList<Integer> dbDistance = null;
		ArrayList<Integer> dbAltitude = null;
		ArrayList<Integer> dbTourRecordingTime = null;
		ArrayList<Integer> dbTourDrivingTime = null;

		ArrayList<Integer> dbCalories = null;

		ArrayList<Long> dbTypeIds = null;
		ArrayList<Integer> dbTypeColorIndex = null;

		ArrayList<Long> tourIds = null;

		ArrayList<Boolean> dbIsManualTour = null;

		HashMap<Long, ArrayList<Long>> dbTagIds = null;

		long lastTourId = -1;
		ArrayList<Long> tagIds = null;

		final String select = NL

				+ "SELECT 						" + NL //$NON-NLS-1$

				+ " TourId,						" + NL // 1 //$NON-NLS-1$
				+ " StartYear,					" + NL // 2 //$NON-NLS-1$
				+ " StartMonth,					" + NL // 3 //$NON-NLS-1$
				+ " StartDay,					" + NL // 4 //$NON-NLS-1$
				+ " StartHour,					" + NL // 5 //$NON-NLS-1$
				+ " StartMinute,				" + NL // 6 //$NON-NLS-1$
				+ " TourDistance,				" + NL // 7 //$NON-NLS-1$
				+ " TourAltUp,					" + NL // 8 //$NON-NLS-1$
				+ " TourRecordingTime,			" + NL // 9 //$NON-NLS-1$
				+ " TourDrivingTime,			" + NL // 10 //$NON-NLS-1$
				+ " TourTitle,					" + NL // 11 //$NON-NLS-1$
				+ " TourType_typeId,			" + NL // 12 //$NON-NLS-1$
				+ " TourDescription,			" + NL // 13 //$NON-NLS-1$
				+ " startWeek,					" + NL // 14 //$NON-NLS-1$
				+ " devicePluginId,				" + NL // 15 //$NON-NLS-1$
				+ " calories,					" + NL // 16	//$NON-NLS-1$

				+ " jTdataTtag.TourTag_tagId	" + NL // 17 //$NON-NLS-1$

				+ NL

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + NL) //$NON-NLS-1$

				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON tourID = jTdataTtag.TourData_tourId" + NL) //$NON-NLS-1$

				+ " WHERE StartYear=?			" + NL //$NON-NLS-1$
				+ " AND   StartMonth=?			" + NL //$NON-NLS-1$
				+ " AND   StartDay=?			" + NL //$NON-NLS-1$

				+ filter.getWhereClause()

				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"); //$NON-NLS-1$

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(select);

			statement.setInt(1, year);
			statement.setInt(2, month);
			filter.setParameters(statement, 4);

			monthData = new CalendarTourData[31][];

			long tourId = -1;

			for (int day = 0; day < 31; day++) {

				statement.setInt(3, day + 1);

				final ResultSet result = statement.executeQuery();

				boolean firstTourOfDay = true;
				tourIds = new ArrayList<Long>();

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
						dbAltitude = new ArrayList<>();
						dbTourRecordingTime = new ArrayList<>();
						dbTourDrivingTime = new ArrayList<>();

						dbCalories = new ArrayList<>();

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

						final int recordingTime = result.getInt(9);

						dbTourYear.add(tourYear);
						dbTourMonth.add(tourMonth);
						dbTourDay.add(tourDay);

						dbTourStartTime.add(startTime);
						dbTourEndTime.add((startTime + recordingTime));

						dbDistance.add(result.getInt(7));
						dbAltitude.add(result.getInt(8));

						dbTourRecordingTime.add(recordingTime);
						dbTourDrivingTime.add(result.getInt(10));

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

							tagIds = new ArrayList<Long>();
							tagIds.add((Long) dbTagId);

							dbTagIds.put(tourId, tagIds);
						}

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
					data.altitude = dbAltitude.get(tourIndex);

					data.recordingTime = dbTourRecordingTime.get(tourIndex);
					data.drivingTime = dbTourDrivingTime.get(tourIndex);

					data.calories = dbCalories.get(tourIndex);

					data.tourTitle = dbTourTitle.get(tourIndex);
					data.tourDescription = dbTourDescription.get(tourIndex);

					final LocalDate tourDate = LocalDate.of(year, month, data.day);
					data.tourDate = tourDate;
					data.dayOfWeek = tourDate.getDayOfWeek().getValue();

					data.isManualTour = dbIsManualTour.get(tourIndex);

					dayData[tourIndex] = data;

					if (_isScrambleCalendar) {

						data.tourTitle = UI.scrambleText(data.tourTitle);
						data.tourDescription = UI.scrambleText(data.tourDescription);

						data.distance = UI.scrambleNumbers(data.distance);
						data.altitude = UI.scrambleNumbers(data.altitude);
						data.calories = UI.scrambleNumbers(data.calories);

						data.recordingTime = UI.scrambleNumbers(data.recordingTime);
						data.drivingTime = UI.scrambleNumbers(data.drivingTime);
					}

				} // create data for this day

				monthData[day] = dayData;

			} // for days 0 .. 30

		} catch (final SQLException e) {

			StatusUtil.log(select);
			net.tourbook.ui.UI.showSQLException(e);

		} finally {

			Util.closeSql(conn);
		}

//		System.out.println(
//				(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") +
//						"getCalendarMonthData_FromDb\t\t\t" + (System.currentTimeMillis() - start) + " ms - "
//						+ "\t" + year + " " + month);
//		// TODO remove SYSTEM.OUT.PRINTLN

		return monthData;
	}

	LocalDateTime getCalendarTourDateTime(final Long tourId) {

		LocalDateTime dt = LocalDateTime.now();

		final String select =

				"SELECT" //$NON-NLS-1$

						+ " StartYear," + NL //			1 //$NON-NLS-1$
						+ " StartMonth," + NL //		2 //$NON-NLS-1$
						+ " StartDay," + NL //			3 //$NON-NLS-1$
						+ " StartHour," + NL //			4 //$NON-NLS-1$
						+ " StartMinute" //				5 //$NON-NLS-1$

						+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + NL) //$NON-NLS-1$

						+ (" WHERE TourId=?" + NL); //$NON-NLS-1$

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();
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

			StatusUtil.log(select);
			net.tourbook.ui.UI.showSQLException(e);

		} finally {

			Util.closeSql(conn);
		}

		return dt;
	}

	public CalendarTourData getCalendarWeekSummaryData(final LocalDate week1stDay, final CalendarView calendarView) {

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

			// update cached data otherwise an endless loop occures !!!
			_weekCache.get(year)[week] = weekData;

		} else {

			weekData = cachedWeekData;
		}

		return weekData;

	}

	private CalendarTourData getCalendarWeekSummaryData_FromDb(	final LocalDate week1stDay,
																final int year,
																final int week) {

		final CalendarTourData weekData = new CalendarTourData();

		weekData.loadingState = LoadingState.IS_QUEUED;

		_weekWaitingQueue.add(new WeekLoader(week1stDay, year, week, weekData, _weekExecuterId.get()));

		final Runnable executorTask = new Runnable() {
			@Override
			public void run() {

				// get last added loader item
				final WeekLoader weekLoader = _weekWaitingQueue.pollFirst();

				if (weekLoader == null) {
					return;
				}

				if (loadWeek_FromDB(weekLoader)) {
					_calendarGraph.updateUI_AfterDataLoading();
				}
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

		Connection conn = null;
		String select = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

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

				// this occures when there are 0 tours
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

			StatusUtil.log(select);
			net.tourbook.ui.UI.showSQLException(e);

		} finally {

			Util.closeSql(conn);
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
		Connection conn = null;
		String select = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final LocalDate today = LocalDate.now();

			select = NL

					+ "SELECT	" + NL //$NON-NLS-1$

					+ " TourId	" + NL //$NON-NLS-1$

					+ " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

					+ " WHERE StartYear=" + today.getYear() + NL//$NON-NLS-1$
					+ " AND   StartMonth=" + today.getMonthValue() + NL//$NON-NLS-1$
					+ " AND   StartDay=" + today.getDayOfMonth() + NL//$NON-NLS-1$

			;

			final PreparedStatement statement = conn.prepareStatement(select);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				todayTourId = result.getLong(1);

				break;
			}

		} catch (final SQLException e) {

			StatusUtil.log(select);
			net.tourbook.ui.UI.showSQLException(e);

		} finally {

			Util.closeSql(conn);
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

	private boolean loadWeek_FromDB(final WeekLoader weekLoader) {

		if (weekLoader.executorId < _weekExecuterId.get()) {

			// current executer was invalidated

			// reset loading state
			weekLoader.weekData.loadingState = LoadingState.NOT_LOADED;

			return false;
		}

//		final long start = System.currentTimeMillis();

		final int year = weekLoader.year;
		final int week = weekLoader.week;
		final CalendarTourData weekData = weekLoader.weekData;

		Connection conn = null;
		String select = null;

		try {

			final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);

			String fromTourData;
			if (sqlFilter.isTagFilterActive()) {

				// filter by tag

				fromTourData = NL

						+ "FROM (						" + NL //$NON-NLS-1$

						+ " SELECT						" + NL //$NON-NLS-1$

						+ "  TourDistance,				" + NL //$NON-NLS-1$
						+ "  TourAltUp,					" + NL //$NON-NLS-1$
						+ "  TourRecordingTime,			" + NL //$NON-NLS-1$
						+ "  TourDrivingTime,			" + NL //$NON-NLS-1$
						+ "  calories,					" + NL //$NON-NLS-1$
						+ "  jTdataTtag.TourTag_tagId	" + NL //$NON-NLS-1$

						+ (" FROM " + TourDatabase.TABLE_TOUR_DATA) + NL//$NON-NLS-1$

						// get tag id's
						+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") + NL //$NON-NLS-1$ //$NON-NLS-2$
						+ (" ON tourID = jTdataTtag.TourData_tourId") + NL //$NON-NLS-1$

						+ " WHERE startWeekYear=?		" + NL //$NON-NLS-1$
						+ " AND   startWeek=?			" + NL //$NON-NLS-1$
						+ sqlFilter.getWhereClause() + NL

						+ ") td" //$NON-NLS-1$

				;

			} else {

				fromTourData = NL

						+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + NL) //$NON-NLS-1$

						+ (" WHERE startWeekYear=?" + NL) //$NON-NLS-1$
						+ (" AND   startWeek=?" + NL) //$NON-NLS-1$

						+ sqlFilter.getWhereClause();
			}

			select = "SELECT" + NL //			  //$NON-NLS-1$

					+ " SUM(TourDistance),			" + NL //	1 //$NON-NLS-1$
					+ " SUM(TourAltUp),				" + NL //	2 //$NON-NLS-1$
					+ " SUM(TourRecordingTime),		" + NL //	3 //$NON-NLS-1$
					+ " SUM(TourDrivingTime),		" + NL //	4 //$NON-NLS-1$
					+ " SUM(calories),				" + NL //	5 //$NON-NLS-1$

					+ " SUM(1)						" + NL //	6 //$NON-NLS-1$

					+ fromTourData;

			conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(select);

			statement.setInt(1, year);
			statement.setInt(2, week);
			sqlFilter.setParameters(statement, 3);

			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				weekData.year = year;
				weekData.week = week;

				weekData.distance = result.getInt(1);
				weekData.altitude = result.getInt(2);
				weekData.recordingTime = result.getInt(3);
				weekData.drivingTime = result.getInt(4);
				weekData.calories = result.getInt(5);

				weekData.numTours = result.getInt(6);

				if (_isScrambleCalendar) {

					weekData.altitude = UI.scrambleNumbers(weekData.altitude);
					weekData.distance = UI.scrambleNumbers(weekData.distance);

					weekData.recordingTime = UI.scrambleNumbers(weekData.recordingTime);
					weekData.drivingTime = UI.scrambleNumbers(weekData.drivingTime);

					weekData.calories = UI.scrambleNumbers(weekData.calories);
				}
			}

		} catch (final SQLException e) {

			StatusUtil.log(select);
			net.tourbook.ui.UI.showSQLException(e);

		} finally {

			Util.closeSql(conn);

			weekData.loadingState = LoadingState.IS_LOADED;
		}

//		System.out.println("getCalendarWeekSummaryData_FromDb\t" + (System.currentTimeMillis() - start) + " ms");
//		// TODO remove SYSTEM.OUT.PRINTLN

		return true;
	}

	public void setCalendarGraph(final CalendarGraph calendarGraph) {

		_calendarGraph = calendarGraph;
	}

}
