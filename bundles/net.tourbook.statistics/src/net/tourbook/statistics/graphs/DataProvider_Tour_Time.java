/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Time extends DataProvider {

	private static DataProvider_Tour_Time	_instance;

	private Long							_selectedTourId;

	private TourData_Time					_tourDataTime;

	private DataProvider_Tour_Time() {}

	public static DataProvider_Tour_Time getInstance() {
		if (_instance == null) {
			_instance = new DataProvider_Tour_Time();
		}
		return _instance;
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
	TourData_Time getTourTimeData(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int lastYear,
									final int numberOfYears,
									final boolean isForceUpdate) {

		// dont reload data which are already here
		if (_activePerson == person
				&& _activeTourTypeFilter == tourTypeFilter
				&& _lastYear == lastYear
				&& _numberOfYears == numberOfYears
				&& isForceUpdate == false) {
			return _tourDataTime;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;

		_lastYear = lastYear;
		_numberOfYears = numberOfYears;

		initYearNumbers();

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT " //$NON-NLS-1$

				+ "TourId," //					1 //$NON-NLS-1$

				+ "StartYear," //				2 //$NON-NLS-1$
				+ "StartMonth," //				3 //$NON-NLS-1$
				+ "StartDay," //				4 //$NON-NLS-1$
				+ "StartWeek," //				5 //$NON-NLS-1$
				+ "TourStartTime," //			6 //$NON-NLS-1$
				+ "TimeZoneId, "//				7 //$NON-NLS-1$
				+ "TourRecordingTime," //		8 //$NON-NLS-1$
				+ "TourDrivingTime,"//			9 //$NON-NLS-1$

				+ "TourDistance," //			10 //$NON-NLS-1$
				+ "TourAltUp," //				11 //$NON-NLS-1$
				+ "TourTitle," //				12 //$NON-NLS-1$
				+ "TourDescription," // 		13 //$NON-NLS-1$

				+ "TourType_typeId,"//			14 //$NON-NLS-1$
				+ "jTdataTtag.TourTag_tagId"//	15 //$NON-NLS-1$

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$

				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON tourID = jTdataTtag.TourData_tourId") //$NON-NLS-1$

				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()

				+ (" ORDER BY TourStartTime"); //$NON-NLS-1$

		try {

			final TLongArrayList allTourIds = new TLongArrayList();

			final TIntArrayList allTourYear = new TIntArrayList();
			final TIntArrayList allTourMonths = new TIntArrayList();
			final TIntArrayList allYearsDOY = new TIntArrayList(); // DOY...Day Of Year for all years

			final TIntArrayList allTourStartTime = new TIntArrayList();
			final TIntArrayList allTourEndTime = new TIntArrayList();
			final TIntArrayList allTourStartWeek = new TIntArrayList();
			final ArrayList<ZonedDateTime> allTourStartDateTime = new ArrayList<>();
			final ArrayList<String> allTourTimeOffset = new ArrayList<>();

			final TIntArrayList allTourRecordingTime = new TIntArrayList();
			final TIntArrayList allTourDrivingTime = new TIntArrayList();

			final TIntArrayList allDistance = new TIntArrayList();
			final TIntArrayList allAltitudeUp = new TIntArrayList();

			final ArrayList<String> allTourTitle = new ArrayList<String>();
			final ArrayList<String> allTourDescription = new ArrayList<String>();

			final TLongArrayList allTypeIds = new TLongArrayList();
			final TIntArrayList allTypeColorIndex = new TIntArrayList();

			final HashMap<Long, ArrayList<Long>> allTagIds = new HashMap<Long, ArrayList<Long>>();

			long lastTourId = -1;
			ArrayList<Long> tagIds = null;

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
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

					final int dbTourYear = result.getShort(2);
					final int dbTourMonth = result.getShort(3) - 1;

					// needs cleanup
					final int dbTourDay = result.getShort(4);
					final int dbTourStartWeek = result.getInt(5);

					final long dbStartTimeMilli = result.getLong(6);
					final String dbTimeZoneId = result.getString(7);
					final int dbRecordingTime = result.getInt(8);
					final int dbDrivingTime = result.getInt(9);

					final float dbDistance = result.getFloat(10);
					final int dbAltitudeUp = result.getInt(11);

					final String dbTourTitle = result.getString(12);
					final String dbDescription = result.getString(13);
					final Object dbTypeIdObject = result.getObject(14);

					final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbStartTimeMilli, dbTimeZoneId);
					final ZonedDateTime zonedStartDateTime = tourDateTime.tourZonedDateTime;

					// get number of days for the year, start with 0
					final int tourDOY = tourDateTime.tourZonedDateTime.get(ChronoField.DAY_OF_YEAR) - 1;

					final int startDayTime = (zonedStartDateTime.getHour() * 3600)
							+ (zonedStartDateTime.getMinute() * 60)
							+ zonedStartDateTime.getSecond();

					allTourYear.add(dbTourYear);
					allTourMonths.add(dbTourMonth);
					allYearsDOY.add(getYearDOYs(dbTourYear) + tourDOY);
					allTourStartWeek.add(dbTourStartWeek);

					allTourStartDateTime.add(zonedStartDateTime);
					allTourTimeOffset.add(tourDateTime.timeZoneOffsetLabel);
					allTourStartTime.add(startDayTime);
					allTourEndTime.add((startDayTime + dbRecordingTime));
					allTourRecordingTime.add(dbRecordingTime);
					allTourDrivingTime.add(dbDrivingTime);

					allDistance.add((int) (dbDistance / UI.UNIT_VALUE_DISTANCE));
					allAltitudeUp.add((int) (dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE));

					allTourTitle.add(dbTourTitle);

					allTourDescription.add(dbDescription == null ? UI.EMPTY_STRING : dbDescription);

					if (dbTagId instanceof Long) {
						tagIds = new ArrayList<Long>();
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

			conn.close();

			// get number of days for all years
			int yearDays = 0;
			for (final int doy : _yearDays) {
				yearDays += doy;
			}

			/*
			 * create data
			 */
			_tourDataTime = new TourData_Time();

			_tourDataTime.tourIds = allTourIds.toArray();

			_tourDataTime.typeIds = allTypeIds.toArray();
			_tourDataTime.typeColorIndex = allTypeColorIndex.toArray();

			_tourDataTime.tagIds = allTagIds;

			_tourDataTime.allDaysInAllYears = yearDays;
			_tourDataTime.yearDays = _yearDays;
			_tourDataTime.years = _years;

			_tourDataTime.tourYearValues = allTourYear.toArray();
			_tourDataTime.tourMonthValues = allTourMonths.toArray();
			_tourDataTime.tourDOYValues = allYearsDOY.toArray();
			_tourDataTime.weekValues = allTourStartWeek.toArray();

			_tourDataTime.tourTimeStartValues = allTourStartTime.toArray();
			_tourDataTime.tourTimeZoneOffset = allTourTimeOffset;
			_tourDataTime.tourTimeEndValues = allTourEndTime.toArray();
			_tourDataTime.tourStartDateTimes = allTourStartDateTime;

			_tourDataTime.tourDistanceValues = allDistance.toArray();
			_tourDataTime.tourAltitudeValues = allAltitudeUp.toArray();

			_tourDataTime.tourRecordingTimeValues = allTourRecordingTime.toArray();
			_tourDataTime.tourDrivingTimeValues = allTourDrivingTime.toArray();

			_tourDataTime.tourTitle = allTourTitle;
			_tourDataTime.tourDescription = allTourDescription;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourDataTime;
	}

	void setSelectedTourId(final Long selectedTourId) {
		_selectedTourId = selectedTourId;
	}

}
