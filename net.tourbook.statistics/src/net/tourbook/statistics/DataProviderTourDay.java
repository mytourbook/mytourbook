/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
import net.tourbook.util.ArrayListToArray;

public class DataProviderTourDay extends DataProvider {

	private static DataProviderTourDay	fInstance;

	private TourDayData					fTourDayData;

	public static DataProviderTourDay getInstance() {
		if (fInstance == null) {
			fInstance = new DataProviderTourDay();
		}
		return fInstance;
	}

	private DataProviderTourDay() {}

	TourDayData getDayData(	final TourPerson person,
							final TourTypeFilter tourTypeFilter,
							final int lastYear,
							final int numberOfYears,
							final boolean refreshData) {

		// dont reload data which are already here
		if (person == fActivePerson
				&& tourTypeFilter == fActiveTourTypeFilter
				&& lastYear == fLastYear
				&& numberOfYears == fNumberOfYears
				&& refreshData == false) {
			return fTourDayData;
		}

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;

		fLastYear = lastYear;
		fNumberOfYears = numberOfYears;

		initYearNumbers();

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		fTourDayData = new TourDayData();
		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT " // //$NON-NLS-1$
				+ "TourId, " //					// 1 //$NON-NLS-1$
				+ "StartYear, " // 				// 2 //$NON-NLS-1$
				+ "StartMonth, " // 			// 3 //$NON-NLS-1$
				+ "StartDay, " // 				// 4 //$NON-NLS-1$
				+ "StartHour, " // 				// 5 //$NON-NLS-1$
				+ "StartMinute, " // 			// 6 //$NON-NLS-1$
				+ "TourDistance, " // 			// 7 //$NON-NLS-1$
				+ "TourAltUp, " // 				// 8 //$NON-NLS-1$
				+ "TourDrivingTime, " // 		// 9 //$NON-NLS-1$
				+ "TourRecordingTime, " // 		// 10 //$NON-NLS-1$
				+ "TourTitle, " //				// 11 //$NON-NLS-1$
				+ "TourType_typeId, " // 		// 12 //$NON-NLS-1$
				+ "TourDescription, " // 		// 13 //$NON-NLS-1$

				+ "jTdataTtag.TourTag_tagId"//	// 14 //$NON-NLS-1$ 

				+ UI.NEW_LINE

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$

				// get tag id's
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ON tourID = jTdataTtag.TourData_tourId") //$NON-NLS-1$

				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()

				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute "); //$NON-NLS-1$

		try {

			final ArrayList<Long> dbTourIds = new ArrayList<Long>();

			final ArrayList<Integer> dbYear = new ArrayList<Integer>();
			final ArrayList<Integer> dbMonths = new ArrayList<Integer>();
			final ArrayList<Integer> dbAllYearsDOY = new ArrayList<Integer>(); // DOY...Day Of Year

			final ArrayList<Integer> dbTourStartTime = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourEndTime = new ArrayList<Integer>();

			final ArrayList<Integer> dbDistance = new ArrayList<Integer>();
			final ArrayList<Integer> dbAltitude = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourDuration = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourRecordingTime = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourDrivingTime = new ArrayList<Integer>();
			final ArrayList<String> dbTourTitle = new ArrayList<String>();
			final ArrayList<String> dbTourDescription = new ArrayList<String>();

			final ArrayList<Long> dbTypeIds = new ArrayList<Long>();
			final ArrayList<Integer> dbTypeColorIndex = new ArrayList<Integer>();

			final HashMap<Long, ArrayList<Long>> dbTagIds = new HashMap<Long, ArrayList<Long>>();

			long lastTourId = -1;
			ArrayList<Long> tagIds = null;

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(1);
				final Object dbTagId = result.getObject(14);

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
					fCalendar.set(tourYear, tourMonth, result.getShort(4));
					final int tourDOY = fCalendar.get(Calendar.DAY_OF_YEAR) - 1;

					dbTourIds.add(tourId);
					dbYear.add(tourYear);
					dbMonths.add(tourMonth);
					dbAllYearsDOY.add(getYearDOYs(tourYear) + tourDOY);

					// round distance
					final int distance = result.getInt(7);
					dbDistance.add((int) (distance / UI.UNIT_VALUE_DISTANCE));
					dbAltitude.add((int) (result.getInt(8) / UI.UNIT_VALUE_ALTITUDE));

					dbTourStartTime.add(startTime);
					dbTourEndTime.add((startTime + recordingTime));
					dbTourDuration.add(drivingTime == 0 ? recordingTime : drivingTime);
					dbTourRecordingTime.add(recordingTime);
					dbTourDrivingTime.add(drivingTime);

					dbTourTitle.add(result.getString(11));

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

			final int[] tourYear = ArrayListToArray.toInt(dbYear);
			final int[] tourAllYearsDOY = ArrayListToArray.toInt(dbAllYearsDOY);

			final int[] timeHigh = ArrayListToArray.toInt(dbTourDuration);
			final int[] distanceHigh = ArrayListToArray.toInt(dbDistance);
			final int[] altitudeHigh = ArrayListToArray.toInt(dbAltitude);

			final int serieLength = timeHigh.length;
			final int[] timeLow = new int[serieLength];
			final int[] distanceLow = new int[serieLength];
			final int[] altitudeLow = new int[serieLength];

			/*
			 * adjust low/high values when a day has multiple tours
			 */
			int prevTourDOY = -1;
			for (int tourIndex = 0; tourIndex < tourAllYearsDOY.length; tourIndex++) {

				final int tourDOY = tourAllYearsDOY[tourIndex];

				if (prevTourDOY == tourDOY) {

					// current tour is at the same day as the tour before

					timeHigh[tourIndex] += timeLow[tourIndex] = timeHigh[tourIndex - 1];
					distanceHigh[tourIndex] += distanceLow[tourIndex] = distanceHigh[tourIndex - 1];
					altitudeHigh[tourIndex] += altitudeLow[tourIndex] = altitudeHigh[tourIndex - 1];

				} else {

					// current tour is on another day as the tour before

					prevTourDOY = tourDOY;
				}
			}

			// get number of days for all years
			int yearDays = 0;
			for (final int doy : fYearDays) {
				yearDays += doy;
			}

			fTourDayData.fTourIds = ArrayListToArray.toLong(dbTourIds);

			fTourDayData.fYearValues = tourYear;
			fTourDayData.fMonthValues = ArrayListToArray.toInt(dbMonths);
			fTourDayData.fDOYValues = tourAllYearsDOY;

			fTourDayData.allDaysInAllYears = yearDays;
			fTourDayData.yearDays = fYearDays;
			fTourDayData.years = fYears;

			fTourDayData.fTypeIds = ArrayListToArray.toLong(dbTypeIds);
			fTourDayData.fTypeColorIndex = ArrayListToArray.toInt(dbTypeColorIndex);

			fTourDayData.fTagIds = dbTagIds;

			fTourDayData.fTimeLow = timeLow;
			fTourDayData.fTimeHigh = timeHigh;

			fTourDayData.fDistanceLow = distanceLow;
			fTourDayData.fDistanceHigh = distanceHigh;

			fTourDayData.fAltitudeLow = altitudeLow;
			fTourDayData.fAltitudeHigh = altitudeHigh;

			fTourDayData.fTourStartValues = ArrayListToArray.toInt(dbTourStartTime);
			fTourDayData.fTourEndValues = ArrayListToArray.toInt(dbTourEndTime);

			fTourDayData.fTourDistanceValues = ArrayListToArray.toInt(dbDistance);
			fTourDayData.fTourAltitudeValues = ArrayListToArray.toInt(dbAltitude);

			fTourDayData.fTourRecordingTimeValues = dbTourRecordingTime;
			fTourDayData.fTourDrivingTimeValues = dbTourDrivingTime;

			fTourDayData.tourTitle = dbTourTitle;
			fTourDayData.tourDescription = dbTourDescription;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return fTourDayData;
	}

}
