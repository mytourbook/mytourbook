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

package net.tourbook.statistics;

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
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProviderTourDay extends DataProvider {

	private static DataProviderTourDay	_instance;

	private TourDataDay					_tourDayData;

	private DataProviderTourDay() {}

	public static DataProviderTourDay getInstance() {
		if (_instance == null) {
			_instance = new DataProviderTourDay();
		}
		return _instance;
	}

	TourDataDay getDayData(	final TourPerson person,
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

		_tourDayData = new TourDataDay();
		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT " // //$NON-NLS-1$
				//
				+ "TourId, " //					// 1 	//$NON-NLS-1$
				+ "StartYear, " // 				// 2	//$NON-NLS-1$
				+ "StartMonth, " // 			// 3	//$NON-NLS-1$
				+ "StartDay, " // 				// 4	//$NON-NLS-1$
				+ "StartHour, " // 				// 5	//$NON-NLS-1$
				+ "StartMinute, " // 			// 6	//$NON-NLS-1$
				+ "TourDistance, " // 			// 7	//$NON-NLS-1$
				+ "TourAltUp, " // 				// 8	//$NON-NLS-1$
				+ "TourDrivingTime, " // 		// 9	//$NON-NLS-1$
				+ "TourRecordingTime, " // 		// 10	//$NON-NLS-1$
				+ "TourTitle, " //				// 11	//$NON-NLS-1$
				+ "TourType_typeId, " // 		// 12	//$NON-NLS-1$
				+ "TourDescription, " // 		// 13	//$NON-NLS-1$
				+ "startWeek," //				// 14	//$NON-NLS-1$
				//
				+ "jTdataTtag.TourTag_tagId"//	// 15	//$NON-NLS-1$
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

			final TLongArrayList dbTourIds = new TLongArrayList();

			final TIntArrayList dbYear = new TIntArrayList();
			final TIntArrayList dbMonths = new TIntArrayList();
			final TIntArrayList dbAllYearsDOY = new TIntArrayList(); // DOY...Day Of Year

			final TIntArrayList dbTourStartTime = new TIntArrayList();
			final TIntArrayList dbTourEndTime = new TIntArrayList();
			final TIntArrayList dbTourStartWeek = new TIntArrayList();

			final TFloatArrayList dbDistance = new TFloatArrayList();
			final TFloatArrayList dbAvgSpeed = new TFloatArrayList();
			final TFloatArrayList dbAvgPace = new TFloatArrayList();
			final TFloatArrayList dbAltitudeUp = new TFloatArrayList();
			final TIntArrayList dbTourDuration = new TIntArrayList();
			final TIntArrayList dbTourRecordingTime = new TIntArrayList();
			final TIntArrayList dbTourDrivingTime = new TIntArrayList();
			final ArrayList<String> dbTourTitle = new ArrayList<String>();
			final ArrayList<String> dbTourDescription = new ArrayList<String>();

			final TLongArrayList dbTypeIds = new TLongArrayList();
			final TIntArrayList dbTypeColorIndex = new TIntArrayList();

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

					final int tourYear = result.getShort(2);
					final int tourMonth = result.getShort(3) - 1;

					final int startHour = result.getShort(5);
					final int startMinute = result.getShort(6);
					final int startTime = startHour * 3600 + startMinute * 60;

					final int drivingTime = result.getInt(9);
					final int recordingTime = result.getInt(10);

					// get number of days for the year, start with 0
					_calendar.set(tourYear, tourMonth, result.getShort(4));
					final int tourDOY = _calendar.get(Calendar.DAY_OF_YEAR) - 1;

					dbTourIds.add(tourId);
					dbYear.add(tourYear);
					dbMonths.add(tourMonth);
					dbAllYearsDOY.add(getYearDOYs(tourYear) + tourDOY);

					// round distance
					final float distance = result.getFloat(7) / UI.UNIT_VALUE_DISTANCE;
					dbDistance.add(distance);

					dbAltitudeUp.add(result.getInt(8) / UI.UNIT_VALUE_ALTITUDE);

					dbAvgPace.add(distance == 0 ? 0 : drivingTime * 1000f / distance / 60.0f);
					dbAvgSpeed.add(drivingTime == 0 ? 0 : 3.6f * distance / drivingTime);

					dbTourStartTime.add(startTime);
					dbTourEndTime.add((startTime + recordingTime));
					dbTourDuration.add(drivingTime == 0 ? recordingTime : drivingTime);
					dbTourRecordingTime.add(recordingTime);
					dbTourDrivingTime.add(drivingTime);

					dbTourTitle.add(result.getString(11));
					dbTourStartWeek.add(result.getInt(14));

					final String description = result.getString(13);
					dbTourDescription.add(description == null ? UI.EMPTY_STRING : description);

					if (dbTagId instanceof Long) {
						tagIds = new ArrayList<Long>();
						tagIds.add((Long) dbTagId);

						dbTagIds.put(tourId, tagIds);
					}

					/*
					 * convert type id to the type index in the tour types list which is also the
					 * color index
					 */
					int colorIndex = 0;
					final Object dbTypeIdObject = result.getObject(12);
					if (dbTypeIdObject != null) {
						final long dbTypeId = result.getLong(12);
						for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
							if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
								colorIndex = colorOffset + typeIndex;
								break;
							}
						}
					}

					dbTypeColorIndex.add(colorIndex);
					dbTypeIds.add((Long) dbTypeIdObject);
				}

				lastTourId = tourId;
			}

			conn.close();

			final int[] tourYear = dbYear.toArray();
			final int[] tourAllYearsDOY = dbAllYearsDOY.toArray();

			final int[] durationHigh = dbTourDuration.toArray();

			final float[] altitudeHigh = dbAltitudeUp.toArray();
			final float[] avgPaceHigh = dbAvgPace.toArray();
			final float[] avgSpeedHigh = dbAvgSpeed.toArray();
			final float[] distanceHigh = dbDistance.toArray();

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

			_tourDayData.tourIds = dbTourIds.toArray();

			_tourDayData.yearValues = tourYear;
			_tourDayData.monthValues = dbMonths.toArray();
			_tourDayData.setDoyValues(tourAllYearsDOY);
			_tourDayData.weekValues = dbTourStartWeek.toArray();

			_tourDayData.allDaysInAllYears = yearDays;
			_tourDayData.yearDays = _yearDays;
			_tourDayData.years = _years;

			_tourDayData.typeIds = dbTypeIds.toArray();
			_tourDayData.typeColorIndex = dbTypeColorIndex.toArray();

			_tourDayData.tagIds = dbTagIds;

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

			_tourDayData.tourStartValues = dbTourStartTime.toArray();
			_tourDayData.tourEndValues = dbTourEndTime.toArray();

			_tourDayData.tourDistanceValues = dbDistance.toArray();
			_tourDayData.tourAltitudeValues = dbAltitudeUp.toArray();

			_tourDayData.recordingTime = dbTourRecordingTime.toArray();
			_tourDayData.drivingTime = dbTourDrivingTime.toArray();

			_tourDayData.tourTitle = dbTourTitle;
			_tourDayData.tourDescription = dbTourDescription;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourDayData;
	}

}
