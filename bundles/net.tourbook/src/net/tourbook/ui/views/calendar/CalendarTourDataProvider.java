/*******************************************************************************
 * Copyright (C) 2011-2017 Matthias Helmling and Contributors
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.common.UI;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.swt.widgets.Display;
import org.joda.time.DateTime;

public class CalendarTourDataProvider {

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

	private HashMap<Integer, CalendarTourData[][][]>	_dayCache	= new HashMap<Integer, CalendarTourData[][][]>();
	private HashMap<Integer, CalendarTourData[]>		_weekCache	= new HashMap<Integer, CalendarTourData[]>();

	private DateTime									_firstDateTime;

	private CalendarTourDataProvider() {
		invalidate();
	};

	static CalendarTourDataProvider getInstance() {

		if (_instance == null) {

			_instance = new CalendarTourDataProvider();
		}

		return _instance;
	}

	CalendarTourData[] getCalendarDayData(final int year, final int month, final int day) {

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

		final long start = System.currentTimeMillis();

		final int colorOffset = 1;

		CalendarTourData[][] monthData = null;
		CalendarTourData[] dayData = null;

		final ArrayList<TourType> tourTypeList = TourDatabase.getAllTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final SQLFilter filter = new SQLFilter();

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

		ArrayList<Long> dbTypeIds = null;
		ArrayList<Integer> dbTypeColorIndex = null;

		ArrayList<Long> tourIds = null;

		HashMap<Long, ArrayList<Long>> dbTagIds = null;

		long lastTourId = -1;
		ArrayList<Long> tagIds = null;

		final String select = "SELECT " //$NON-NLS-1$
				+ "TourId," //					1 //$NON-NLS-1$
				+ "StartYear," //				2 //$NON-NLS-1$
				+ "StartMonth," //				3 //$NON-NLS-1$
				+ "StartDay," //				4 //$NON-NLS-1$
				+ "StartHour," //				5 //$NON-NLS-1$
				+ "StartMinute," //				6 //$NON-NLS-1$
				+ "TourDistance," //			7 //$NON-NLS-1$
				+ "TourAltUp," //				8 //$NON-NLS-1$
				+ "TourRecordingTime," //		9 //$NON-NLS-1$
				+ "TourDrivingTime,"//			10 //$NON-NLS-1$
				+ "TourTitle," //				11 //$NON-NLS-1$
				+ "TourType_typeId,"//			12 //$NON-NLS-1$
				+ "TourDescription," // 		13 //$NON-NLS-1$
				+ "startWeek," //				14 //$NON-NLS-1$

				+ "jTdataTtag.TourTag_tagId"//	15 //$NON-NLS-1$

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON tourID = jTdataTtag.TourData_tourId") //$NON-NLS-1$

				+ (" WHERE StartYear=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ (" AND   StartMonth=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ (" AND   StartDay=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ filter.getWhereClause()

				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();

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

						dbTourTitle = new ArrayList<String>();
						dbTourDescription = new ArrayList<String>();

						dbTourYear = new ArrayList<Integer>();
						dbTourMonth = new ArrayList<Integer>();
						dbTourDay = new ArrayList<Integer>();

						dbTourStartTime = new ArrayList<Integer>();
						dbTourEndTime = new ArrayList<Integer>();
						dbTourStartWeek = new ArrayList<Integer>();

						dbDistance = new ArrayList<Integer>();
						dbAltitude = new ArrayList<Integer>();
						dbTourRecordingTime = new ArrayList<Integer>();
						dbTourDrivingTime = new ArrayList<Integer>();

						dbTypeIds = new ArrayList<Long>();
						dbTypeColorIndex = new ArrayList<Integer>();

						dbTagIds = new HashMap<Long, ArrayList<Long>>();

						firstTourOfDay = false;
					}

					tourId = result.getLong(1);
					final Object dbTagId = result.getObject(15);

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

					data.tourTitle = dbTourTitle.get(tourIndex);
					data.tourDescription = dbTourDescription.get(tourIndex);

					data.dayOfWeek = (new DateTime(year, month, data.day, 12, 0, 0, 0)).getDayOfWeek();

					dayData[tourIndex] = data;

				} // create data for this day

				monthData[day] = dayData;

			} // for days 0 .. 30

			conn.close();

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		}

		System.out.println("getCalendarMonthData_FromDb\t\t\t" + (System.currentTimeMillis() - start) + " ms");
		// TODO remove SYSTEM.OUT.PRINTLN

		return monthData;
	}

	DateTime getCalendarTourDateTime(final Long tourId) {

		DateTime dt = new DateTime();

		final String select = "SELECT " //$NON-NLS-1$
				+ "StartYear," //				2 //$NON-NLS-1$
				+ "StartMonth," //				3 //$NON-NLS-1$
				+ "StartDay," //				4 //$NON-NLS-1$
				+ "StartHour," //				5 //$NON-NLS-1$
				+ "StartMinute" //				6 //$NON-NLS-1$
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

				+ (" WHERE TourId=?" + UI.NEW_LINE); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(select);

			statement.setLong(1, tourId);
			final ResultSet result = statement.executeQuery();
			while (result.next()) {
				final int year = result.getShort(1);
				final int month = result.getShort(2);
				final int day = result.getShort(3);
				final int hour = result.getShort(4);
				final int minute = result.getShort(5);
				dt = new DateTime(year, month, day, hour, minute, 0, 0);
			}
			conn.close();

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		}

		return dt;
	}

	CalendarTourData getCalendarWeekSummaryData(final int year, final int week, final CalendarView calendarView) {

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

			weekData = getCalendarWeekSummaryData_FromDb(year, week);

			_weekCache.get(year)[week] = weekData;

		} else if (cachedWeekData.loadingState == LoadingState.NOT_LOADED) {

			// load again

			System.out.println(
					(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
							+ ("\treloading week data"));
			// TODO remove SYSTEM.OUT.PRINTLN

			weekData = getCalendarWeekSummaryData_FromDb(year, week);

		} else {

			weekData = cachedWeekData;
		}

		return weekData;

	}

	private CalendarTourData getCalendarWeekSummaryData_FromDb(final int year, final int week) {

		final CalendarTourData weekData = new CalendarTourData();

		weekData.loadingState = LoadingState.IS_QUEUED;

		_weekWaitingQueue.add(new WeekLoader(year, week, weekData, _weekExecuterId.get()));

		final Runnable executorTask = new Runnable() {
			@Override
			public void run() {

				// get last added loader item
				final WeekLoader weekLoader = _weekWaitingQueue.pollFirst();

				if (weekLoader == null) {
					return;
				}

				if (loadWeek_FromDB(weekLoader)) {

					// update UI
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							_calendarGraph.updateUI_Layout();
						}
					});
				}
			}
		};

		_weekLoadingExecutor.submit(executorTask);

		return weekData;

	}

	DateTime getFirstDateTime() {

		if (null != _firstDateTime) {
			return _firstDateTime;
		}

		final String select = "SELECT " //$NON-NLS-1$
				+ "StartYear," //			1 //$NON-NLS-1$
				+ "StartMonth," //			2 //$NON-NLS-1$
				+ "StartDay" //				3 //$NON-NLS-1$
				+ UI.NEW_LINE
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$
				+ (" ORDER BY StartYear, StartMonth"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(select);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int year = result.getShort(1);
				final int month = result.getShort(2);
				final int day = result.getShort(3);

				if (year != 0 && month != 0 && day != 0) {

					// this case happened that year/month/day is 0

					_firstDateTime = new DateTime(year, month, day, 12, 0, 0, 0);

					break;
				}
			}

			conn.close();
		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		}

		if (_firstDateTime == null) {
			_firstDateTime = (new DateTime()).minusYears(1);
		}

		return _firstDateTime;

	}

	synchronized void invalidate() {

		// reset all cached data

		_weekExecuterId.incrementAndGet();

		_dayCache.clear();
		_weekCache.clear();

		_firstDateTime = null;
	}

	private boolean loadWeek_FromDB(final WeekLoader weekLoader) {

//		if (weekLoader.executorId < _weekExecuterId.get()) {
//
//			// reset loading state
//			weekLoader.weekData.loadingState = LoadingState.NOT_LOADED;
//
//			System.out.println(
//					(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//							+ ("executer id " + _weekExecuterId.get() + " >  task id " + weekLoader.executorId));
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//			return false;
//		}

		final int year = weekLoader.year;
		final int week = weekLoader.week;

//		final LocalDate requestedDate = LocalDate.of(year, 1, 1).with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week);
//
////		final TemporalAdjuster adjuster = TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY);
////		LocalDate mondayOfWeekByNumber = weekByNumber.with(adjuster);
//		final LocalDate viewportStartDate = _calendarGraph.getViewportStartDate();
//		final LocalDate viewportEndDate = _calendarGraph.getViewportEndDate();
//
//		if (requestedDate.isBefore(viewportStartDate) || requestedDate.isAfter(viewportEndDate)) {
//
//			// reset loading state
//			weekLoader.weekData.loadingState = LoadingState.NOT_LOADED;
//
//			System.out.println(
//					(UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ") //
//							+ (requestedDate + " < " + viewportStartDate + " OR > " + viewportEndDate));
//			// TODO remove SYSTEM.OUT.PRINTLN
//
//			return false;
//		}

		final long start = System.currentTimeMillis();

		final CalendarTourData weekData = weekLoader.weekData;

		try {

			final SQLFilter filter = new SQLFilter();

			final String select = "SELECT " //			  //$NON-NLS-1$

					+ "SUM(TourDistance)," //			1 //$NON-NLS-1$
					+ "SUM(TourAltUp)," //				2 //$NON-NLS-1$
					+ "SUM(TourRecordingTime)," //		3 //$NON-NLS-1$
					+ "SUM(TourDrivingTime),"//			4 //$NON-NLS-1$
					+ "SUM(1)"//			            5 //$NON-NLS-1$

					+ UI.NEW_LINE

					+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

					+ (" WHERE startWeekYear=?" + UI.NEW_LINE) //$NON-NLS-1$
					+ (" AND   startWeek=?" + UI.NEW_LINE) //$NON-NLS-1$

					+ filter.getWhereClause();

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(select);

			statement.setInt(1, year);
			statement.setInt(2, week);
			filter.setParameters(statement, 3);

			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				weekData.year = year;
				weekData.week = week;

				weekData.distance = result.getInt(1);
				weekData.altitude = result.getInt(2);
				weekData.recordingTime = result.getInt(3);
				weekData.drivingTime = result.getInt(4);
				weekData.numTours = result.getInt(5);
			}

			conn.close();

		} catch (final SQLException e) {

			net.tourbook.ui.UI.showSQLException(e);

		} finally {

			weekData.loadingState = LoadingState.IS_LOADED;
		}

		System.out.println("getCalendarWeekSummaryData_FromDb\t" + (System.currentTimeMillis() - start) + " ms");
		// TODO remove SYSTEM.OUT.PRINTLN

		return true;
	}

	public void setCalendarGraph(final CalendarGraph calendarGraph) {

		_calendarGraph = calendarGraph;
	}

}
