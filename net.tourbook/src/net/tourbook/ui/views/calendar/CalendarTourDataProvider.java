package net.tourbook.ui.views.calendar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

import org.joda.time.DateTime;

public class CalendarTourDataProvider {

	private static CalendarTourDataProvider	_instance;

	private ArrayList<Long>					_tourIds;

	private HashMap<Integer, CalendarTourData[][][]>	_dayCache;
	private HashMap<Integer, CalendarTourData[]>		_weekCache;
	private DateTime									_firstDateTime;


	private CalendarTourDataProvider() {
		invalidate();
	};

	public static CalendarTourDataProvider getInstance() {
		if (_instance == null) {
			_instance = new CalendarTourDataProvider();
		}
		return _instance;
	}

	CalendarTourData[] getCalendarDayData(final int year, final int month, final int day) {
		
		CalendarTourData[]				data;

		if (!_dayCache.containsKey(year)) {
			_dayCache.put(year, new CalendarTourData[12][31][]);
		}
	
		if (_dayCache.get(year)[month - 1][day - 1] != null) {
			data = _dayCache.get(year)[month - 1][day - 1];
			// System.out.println("Cache Hit");
		} else {
			data = getCalendarDayDataFromDb(year, month, day);
			_dayCache.get(year)[month - 1][day - 1] = data;
			// System.out.println("Cache miss");
		}
		
		return data;

	}
	
	/**
	 * Retrieve data from the database
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @return CalendarTourData
	 */
	CalendarTourData[] getCalendarDayDataFromDb(final int year, final int month, final int day) {

		final int colorOffset = 0;
		
		CalendarTourData[] calendarTourData = null;

		final ArrayList<TourType> tourTypeList = TourDatabase.getAllTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final SQLFilter filter = new SQLFilter();

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
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON tourID = jTdataTtag.TourData_tourId") //$NON-NLS-1$

				+ (" WHERE StartYear=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ (" AND   StartMonth=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ (" AND   StartDay=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ filter.getWhereClause()

				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"); //$NON-NLS-1$

		try {

			final ArrayList<String> dbTourTitle = new ArrayList<String>();
			final ArrayList<String> dbTourDescription = new ArrayList<String>();

			final ArrayList<Integer> dbTourYear = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourMonth = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourDay = new ArrayList<Integer>();

			final ArrayList<Integer> dbTourStartTime = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourEndTime = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourStartWeek = new ArrayList<Integer>();

			final ArrayList<Integer> dbDistance = new ArrayList<Integer>();
			final ArrayList<Integer> dbAltitude = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourRecordingTime = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourDrivingTime = new ArrayList<Integer>();

			final ArrayList<Long> dbTypeIds = new ArrayList<Long>();
			final ArrayList<Integer> dbTypeColorIndex = new ArrayList<Integer>();

			_tourIds = new ArrayList<Long>();

			final HashMap<Long, ArrayList<Long>> dbTagIds = new HashMap<Long, ArrayList<Long>>();

			long lastTourId = -1;
			ArrayList<Long> tagIds = null;

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(select);

			statement.setInt(1, year);
			statement.setInt(2, month);
			statement.setInt(3, day);
			filter.setParameters(statement, 4);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(1);
				final Object dbTagId = result.getObject(15);

				if (tourId == lastTourId) {

					// get additional tags from outer join

					if (dbTagId instanceof Long) {
						tagIds.add((Long) dbTagId);
					}

				} else {

					// get first record for a tour

					_tourIds.add(tourId);

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

					dbDistance.add((int) (result.getInt(7) / UI.UNIT_VALUE_DISTANCE));
					dbAltitude.add((int) (result.getInt(8) / UI.UNIT_VALUE_ALTITUDE));

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
					 * convert type id to the type index in the tour type array, this is also the
					 * color index for the tour type
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
			}

			conn.close();

			/*
			 * create data
			 */
			final int size = _tourIds.size();
			
			calendarTourData = new CalendarTourData[size];
			
			final int dayOfWeek = (new DateTime(year, month, day, 12, 0, 0, 0)).getDayOfWeek();

			for (int i = 0; i < size; i++) {
				
				final CalendarTourData data = new CalendarTourData();

				final long tourId = _tourIds.get(i);

				data.tourId = tourId;

				data.typeId = dbTypeIds.get(i);
				data.typeColorIndex = dbTypeColorIndex.get(i);

				data.tagIds = dbTagIds.get(tourId);

				data.year = dbTourYear.get(i);
				data.month = dbTourMonth.get(i);
				data.day = dbTourDay.get(i);
				data.week = dbTourStartWeek.get(i);

				data.startTime = dbTourStartTime.get(i);
				data.endTime = dbTourEndTime.get(i);

				data.distance = dbDistance.get(i);
				data.altitude = dbAltitude.get(i);

				data.recordingTime = dbTourRecordingTime.get(i);
				data.drivingTime = dbTourDrivingTime.get(i);

				data.tourTitle = dbTourTitle.get(i);
				data.tourDescription = dbTourDescription.get(i);

				data.dayOfWeek = dayOfWeek;

				calendarTourData[i] = data;

			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return calendarTourData;
	}

	public DateTime getCalendarTourDateTime(final Long tourId) {
		
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
			UI.showSQLException(e);
		}

		return dt;
	}
	
	CalendarTourData getCalendarWeekData(final int year, final int week) {
		
		CalendarTourData				data;

		if (!_weekCache.containsKey(year)) {
			_weekCache.put(year, new CalendarTourData[54]); // weeks are from 1..53; we simply leave array index 0 unused (yes, a year can have more than 52 weeks)
		}
		
		if (_weekCache.get(year)[week] != null) {
			data = _weekCache.get(year)[week];
		} else {
			data = getCalendarWeekDataFromDb(year, week);
			_weekCache.get(year)[week] = data;
		}
		
		return data;
			
	}

	CalendarTourData getCalendarWeekDataFromDb(final int year, final int week) {
		
		final CalendarTourData data = new CalendarTourData();
		final SQLFilter filter = new SQLFilter();
		
		final String select = "SELECT " //$NON-NLS-1$
				+ "SUM(TourDistance)," //			1 //$NON-NLS-1$
				+ "SUM(TourAltUp)," //				2 //$NON-NLS-1$
				+ "SUM(TourRecordingTime)," //		3 //$NON-NLS-1$
				+ "SUM(TourDrivingTime)"//			4 //$NON-NLS-1$

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

				// get tag id's
				+ (" WHERE startWeekYear=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ (" AND   startWeek=?" + UI.NEW_LINE) //$NON-NLS-1$
				+ filter.getWhereClause();


		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(select);
			
			statement.setInt(1, year);
			statement.setInt(2, week);
			filter.setParameters(statement, 3);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				data.year = year;
				data.week = week;
				data.distance = result.getInt(1);
				data.altitude = result.getInt(2);

				data.recordingTime = result.getInt(3);
				data.drivingTime = result.getInt(4);

			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return data;

	}

	public DateTime getFirstDateTime() {

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
			result.next();
			final int year = result.getShort(1);
			final int month = result.getShort(2);
			final int day = result.getShort(3);
			_firstDateTime = new DateTime(year, month, day, 12, 0, 0, 0);
			conn.close();
		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		if (null == _firstDateTime) {
			_firstDateTime = (new DateTime()).minusYears(1);
		}

		return _firstDateTime;

	}

	public void invalidate() {
		_dayCache = new HashMap<Integer, CalendarTourData[][][]>();
		_weekCache = new HashMap<Integer, CalendarTourData[]>();
		_firstDateTime = null;
	}

}
