/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
import java.util.Calendar;
import java.util.HashMap;

import net.tourbook.common.util.ArrayListToArray;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Time extends DataProvider {

	private static DataProvider_Tour_Time	_instance;

	private ArrayList<Long>				_tourIds;

	private Long						_selectedTourId;

	private TourData_Time				_tourDataTime;

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
	 * @return
	 */
	TourData_Time getTourTimeData(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int lastYear,
									final int numberOfYears,
									final boolean refreshData) {

		// dont reload data which are already here
		if (_activePerson == person
				&& _activeTourTypeFilter == tourTypeFilter
				&& _lastYear == lastYear
				&& _numberOfYears == numberOfYears
				&& refreshData == false) {
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

				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()

				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"); //$NON-NLS-1$

		try {

			final ArrayList<String> dbTourTitle = new ArrayList<String>();
			final ArrayList<String> dbTourDescription = new ArrayList<String>();

			final ArrayList<Integer> dbTourYear = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourMonths = new ArrayList<Integer>();
			final ArrayList<Integer> dbAllYearsDOY = new ArrayList<Integer>(); // DOY...Day Of Year for all years

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

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

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
					final int startHour = result.getShort(5);
					final int startMinute = result.getShort(6);
					final int startTime = startHour * 3600 + startMinute * 60;

					final int recordingTime = result.getInt(9);
					_calendar.set(tourYear, tourMonth, result.getShort(4), startHour, startMinute);
					final int tourDOY = _calendar.get(Calendar.DAY_OF_YEAR) - 1;

					dbTourYear.add(tourYear);
					dbTourMonths.add(tourMonth);
					dbAllYearsDOY.add(getYearDOYs(tourYear) + tourDOY);

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

			// get number of days for all years
			int yearDays = 0;
			for (final int doy : _yearDays) {
				yearDays += doy;
			}

			/*
			 * create data
			 */
			_tourDataTime = new TourData_Time();

			_tourDataTime.tourIds = ArrayListToArray.toLong(_tourIds);

			_tourDataTime.typeIds = ArrayListToArray.toLong(dbTypeIds);
			_tourDataTime.typeColorIndex = ArrayListToArray.toInt(dbTypeColorIndex);

			_tourDataTime.tagIds = dbTagIds;

			_tourDataTime.allDaysInAllYears = yearDays;
			_tourDataTime.yearDays = _yearDays;
			_tourDataTime.years = _years;

			_tourDataTime.tourYearValues = ArrayListToArray.toInt(dbTourYear);
			_tourDataTime.tourMonthValues = ArrayListToArray.toInt(dbTourMonths);
			_tourDataTime.tourDOYValues = ArrayListToArray.toInt(dbAllYearsDOY);
			_tourDataTime.weekValues = ArrayListToArray.toInt(dbTourStartWeek);

			_tourDataTime.tourTimeStartValues = ArrayListToArray.toInt(dbTourStartTime);
			_tourDataTime.tourTimeEndValues = ArrayListToArray.toInt(dbTourEndTime);

			_tourDataTime.tourDistanceValues = ArrayListToArray.toInt(dbDistance);
			_tourDataTime.tourAltitudeValues = ArrayListToArray.toInt(dbAltitude);

			_tourDataTime.tourRecordingTimeValues = dbTourRecordingTime;
			_tourDataTime.tourDrivingTimeValues = dbTourDrivingTime;

			_tourDataTime.tourTitle = dbTourTitle;
			_tourDataTime.tourDescription = dbTourDescription;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourDataTime;
	}

	void setSelectedTourId(final Long selectedTourId) {
		_selectedTourId = selectedTourId;
	}

}
