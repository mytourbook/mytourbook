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

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

import org.joda.time.DateTime;

public class DataProvider_Tour_Day extends DataProvider {

	private static DataProvider_Tour_Day	_instance;

	private TourData_Day					_tourDayData;

	private DataProvider_Tour_Day() {}

	public static DataProvider_Tour_Day getInstance() {
		if (_instance == null) {
			_instance = new DataProvider_Tour_Day();
		}
		return _instance;
	}

	TourData_Day getDayData(final TourPerson person,
							final TourTypeFilter tourTypeFilter,
							final int lastYear,
							final int numberOfYears,
							final boolean refreshData) {

		// dont reload data which are already here
		if (person == _activePerson
				&& tourTypeFilter == _activeTourTypeFilter
				&& lastYear == _lastYear
				&& numberOfYears == _numberOfYears
				&& refreshData == false) {
			return _tourDayData;
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

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT " // //$NON-NLS-1$
				//
				+ "TourId, " //					// 1 	//$NON-NLS-1$

				+ "StartYear, " // 				// 2	//$NON-NLS-1$
				+ "StartMonth, " // 			// 3	//$NON-NLS-1$
				+ "StartDay, " // 				// 4	//$NON-NLS-1$
				+ "StartWeek," //				// 5	//$NON-NLS-1$
				+ "TourStartTime, " // 			// 6	//$NON-NLS-1$
				+ "TourDrivingTime, " // 		// 7	//$NON-NLS-1$
				+ "TourRecordingTime, " // 		// 8	//$NON-NLS-1$

				+ "TourDistance, " // 			// 9	//$NON-NLS-1$
				+ "TourAltUp, " // 				// 10	//$NON-NLS-1$
				+ "TourTitle, " //				// 11	//$NON-NLS-1$
				+ "TourDescription, " // 		// 12	//$NON-NLS-1$
				//
				+ "TourType_typeId, " // 		// 13	//$NON-NLS-1$
				+ "jTdataTtag.TourTag_tagId"//	// 14	//$NON-NLS-1$
				//
				+ UI.NEW_LINE
				//
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$
				//
				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON tourID = jTdataTtag.TourData_tourId") //$NON-NLS-1$
				//
				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()
				//
				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute "); //$NON-NLS-1$

		try {

			final TLongArrayList allTourIds = new TLongArrayList();

			final TIntArrayList allYears = new TIntArrayList();
			final TIntArrayList allMonths = new TIntArrayList();
			final TIntArrayList allYearsDOY = new TIntArrayList(); // DOY...Day Of Year

			final TIntArrayList allTourStartTime = new TIntArrayList();
			final TIntArrayList allTourEndTime = new TIntArrayList();
			final TIntArrayList allTourStartWeek = new TIntArrayList();
			final ArrayList<DateTime> allTourStartDateTime = new ArrayList<>();

			final TIntArrayList allTourDuration = new TIntArrayList();
			final TIntArrayList allTourRecordingTime = new TIntArrayList();
			final TIntArrayList allTourDrivingTime = new TIntArrayList();

			final TFloatArrayList allDistance = new TFloatArrayList();
			final TFloatArrayList allAvgSpeed = new TFloatArrayList();
			final TFloatArrayList allAvgPace = new TFloatArrayList();
			final TFloatArrayList allAltitudeUp = new TFloatArrayList();

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
				final Object dbTagId = result.getObject(14);

				if (dbTourId == lastTourId) {

					// get additional tags from outer join

					if (dbTagId instanceof Long) {
						tagIds.add((Long) dbTagId);
					}

				} else {

					// get first record from a tour

					final int dbTourYear = result.getShort(2);
					final int dbTourMonth = result.getShort(3) - 1;
					final int dbTourDay = result.getShort(4);
					final int dbTourStartWeek = result.getInt(5);

					final long dbStartTime = result.getLong(6);
					final int dbDrivingTime = result.getInt(7);
					final int dbRecordingTime = result.getInt(8);

					final float dbDistance = result.getFloat(9);
					final int dbAltitudeUp = result.getInt(10);

					final String dbTourTitle = result.getString(11);
					final String dbDescription = result.getString(12);
					final Object dbTypeIdObject = result.getObject(13);

					// get number of days for the year, start with 0
					_calendar.set(dbTourYear, dbTourMonth, dbTourDay);
					final int tourDOY = _calendar.get(Calendar.DAY_OF_YEAR) - 1;

					final DateTime tourStartDateTime = new DateTime(dbStartTime);
					final int startTime = tourStartDateTime.getMillisOfDay() / 1000;

					allTourIds.add(dbTourId);

					allYears.add(dbTourYear);
					allMonths.add(dbTourMonth);
					allYearsDOY.add(getYearDOYs(dbTourYear) + tourDOY);
					allTourStartWeek.add(dbTourStartWeek);

					allTourStartDateTime.add(tourStartDateTime);
					allTourStartTime.add(startTime);
					allTourEndTime.add((startTime + dbRecordingTime));
					allTourRecordingTime.add(dbRecordingTime);
					allTourDrivingTime.add(dbDrivingTime);

					allTourDuration.add(dbDrivingTime == 0 ? dbRecordingTime : dbDrivingTime);

					// round distance
					final float distance = dbDistance / UI.UNIT_VALUE_DISTANCE;

					allDistance.add(distance);
					allAltitudeUp.add(dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE);

					allAvgPace.add(distance == 0 ? 0 : dbDrivingTime * 1000f / distance / 60.0f);
					allAvgSpeed.add(dbDrivingTime == 0 ? 0 : 3.6f * distance / dbDrivingTime);

					allTourTitle.add(dbTourTitle);
					allTourDescription.add(dbDescription == null ? UI.EMPTY_STRING : dbDescription);

					if (dbTagId instanceof Long) {

						tagIds = new ArrayList<Long>();
						tagIds.add((Long) dbTagId);

						allTagIds.put(dbTourId, tagIds);
					}

					/*
					 * convert type id to the type index in the tour types list which is also the
					 * color index
					 */
					int colorIndex = 0;
					long dbTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

					if (dbTypeIdObject instanceof Long) {

						dbTypeId = (Long) dbTypeIdObject;

						for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
							if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
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

			final int[] tourYear = allYears.toArray();
			final int[] tourAllYearsDOY = allYearsDOY.toArray();

			final int[] durationHigh = allTourDuration.toArray();

			final float[] altitudeHigh = allAltitudeUp.toArray();
			final float[] avgPaceHigh = allAvgPace.toArray();
			final float[] avgSpeedHigh = allAvgSpeed.toArray();
			final float[] distanceHigh = allDistance.toArray();

			final int serieLength = durationHigh.length;
			final int[] durationLow = new int[serieLength];
			final float[] altitudeLow = new float[serieLength];
			final float[] avgPaceLow = new float[serieLength];
			final float[] avgSpeedLow = new float[serieLength];
			final float[] distanceLow = new float[serieLength];

			/*
			 * adjust low/high values when a day has multiple tours
			 */
			int prevTourDOY = -1;

			for (int tourIndex = 0; tourIndex < tourAllYearsDOY.length; tourIndex++) {

				final int tourDOY = tourAllYearsDOY[tourIndex];

				if (prevTourDOY == tourDOY) {

					// current tour is at the same day as the tour before

					durationHigh[tourIndex] += durationLow[tourIndex] = durationHigh[tourIndex - 1];

					altitudeHigh[tourIndex] += altitudeLow[tourIndex] = altitudeHigh[tourIndex - 1];
					avgPaceHigh[tourIndex] += avgPaceLow[tourIndex] = avgPaceHigh[tourIndex - 1];
					avgSpeedHigh[tourIndex] += avgSpeedLow[tourIndex] = avgSpeedHigh[tourIndex - 1];
					distanceHigh[tourIndex] += distanceLow[tourIndex] = distanceHigh[tourIndex - 1];

				} else {

					// current tour is on another day as the tour before

					prevTourDOY = tourDOY;
				}
			}

			// get number of days for all years
			int yearDays = 0;
			for (final int doy : _yearDays) {
				yearDays += doy;
			}

			_tourDayData = new TourData_Day();

			_tourDayData.tourIds = allTourIds.toArray();

			_tourDayData.yearValues = tourYear;
			_tourDayData.monthValues = allMonths.toArray();
			_tourDayData.setDoyValues(tourAllYearsDOY);
			_tourDayData.weekValues = allTourStartWeek.toArray();

			_tourDayData.allDaysInAllYears = yearDays;
			_tourDayData.yearDays = _yearDays;
			_tourDayData.years = _years;

			_tourDayData.typeIds = allTypeIds.toArray();
			_tourDayData.typeColorIndex = allTypeColorIndex.toArray();

			_tourDayData.tagIds = allTagIds;

			_tourDayData.setDurationLow(durationLow);
			_tourDayData.setDurationHigh(durationHigh);

			_tourDayData.distanceLow = distanceLow;
			_tourDayData.distanceHigh = distanceHigh;

			_tourDayData.altitudeLow = altitudeLow;
			_tourDayData.altitudeHigh = altitudeHigh;

			_tourDayData.avgPaceLow = avgPaceLow;
			_tourDayData.avgPaceHigh = avgPaceHigh;

			_tourDayData.avgSpeedLow = avgSpeedLow;
			_tourDayData.avgSpeedHigh = avgSpeedHigh;

			_tourDayData.tourStartValues = allTourStartTime.toArray();
			_tourDayData.tourEndValues = allTourEndTime.toArray();
			_tourDayData.tourStartDateTimes = allTourStartDateTime;

			_tourDayData.tourDistanceValues = allDistance.toArray();
			_tourDayData.tourAltitudeValues = allAltitudeUp.toArray();

			_tourDayData.recordingTime = allTourRecordingTime.toArray();
			_tourDayData.drivingTime = allTourDrivingTime.toArray();

			_tourDayData.tourTitle = allTourTitle;
			_tourDayData.tourDescription = allTourDescription;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourDayData;
	}

}
