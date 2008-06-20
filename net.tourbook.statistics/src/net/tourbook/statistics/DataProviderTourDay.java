/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
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

		final String sqlString = //
		"SELECT " // //$NON-NLS-1$
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
				+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag")
				+ (" ON tourID = jTdataTtag.TourData_tourId")

				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$
				+ getSQLFilter(person, tourTypeFilter)

				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute "); //$NON-NLS-1$

		try {
			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

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

			while (result.next()) {

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

				dbTourIds.add(result.getLong(1));
				dbYear.add(tourYear);
				dbMonths.add(tourMonth);
				dbAllYearsDOY.add(getYearDOYs(tourYear) + tourDOY);

				dbDistance.add((int) (result.getInt(7) / 1000 / UI.UNIT_VALUE_DISTANCE));
				dbAltitude.add((int) (result.getInt(8) / UI.UNIT_VALUE_ALTITUDE));

				dbTourStartTime.add(startTime);
				dbTourEndTime.add((startTime + recordingTime));
				dbTourDuration.add(drivingTime == 0 ? recordingTime : drivingTime);
				dbTourRecordingTime.add(recordingTime);
				dbTourDrivingTime.add(drivingTime);

				dbTourTitle.add(result.getString(11));

				final String description = result.getString(13);
				dbTourDescription.add(description == null ? UI.EMPTY_STRING : description);

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
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

			int lastDOY = -1;
			int lastTime = 0;
			int lastDistance = 0;
			int lastAltitude = 0;

			/*
			 * set the low/high values when different tours have the same day
			 */
			int tourIndex = 0;
			for (; tourIndex < tourAllYearsDOY.length; tourIndex++) {

				if (lastDOY == tourAllYearsDOY[tourIndex]) {

					// current tour is at the same day as the tour before

					timeLow[tourIndex] = lastTime;
					distanceLow[tourIndex] = lastDistance;
					altitudeLow[tourIndex] = lastAltitude;

					lastTime = timeHigh[tourIndex - 1] += lastTime;
					lastDistance = distanceHigh[tourIndex - 1] += lastDistance;
					lastAltitude = altitudeHigh[tourIndex - 1] += lastAltitude;

				} else {

					// current tour is on another day as the tour before

					updateLastTour(tourIndex,
							lastTime,
							lastDistance,
							lastAltitude,
							timeLow,
							distanceLow,
							altitudeLow,
							timeHigh,
							distanceHigh,
							altitudeHigh);

					lastTime = 0;
					lastDistance = 0;
					lastAltitude = 0;

					lastDOY = tourAllYearsDOY[tourIndex];
				}
			}

			updateLastTour(tourIndex,
					lastTime,
					lastDistance,
					lastAltitude,
					timeLow,
					distanceLow,
					altitudeLow,
					timeHigh,
					distanceHigh,
					altitudeHigh);

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

//ArrayList<Long>[] dbTagIds;
//fTourDayData.fTagIds = dbTagIds;

			fTourDayData.fTimeLow = timeLow;
			fTourDayData.fDistanceLow = distanceLow;
			fTourDayData.fAltitudeLow = altitudeLow;

			fTourDayData.fTimeHigh = timeHigh;
			fTourDayData.fDistanceHigh = distanceHigh;
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
			e.printStackTrace();
		}

		return fTourDayData;
	}

	private final void updateLastTour(	final int tourIndex,
										final int lastTime,
										final int lastDistance,
										final int lastAltitude,
										final int[] timeLow,
										final int[] distanceLow,
										final int[] altitudeLow,
										final int[] timeHigh,
										final int[] distanceHigh,
										final int[] altitudeHigh) {
		if (lastTime > 0) {

			// update last time
			final int lastTourIndex = tourIndex - 1;
			timeLow[lastTourIndex] = lastTime;
			distanceLow[lastTourIndex] = lastDistance;
			altitudeLow[lastTourIndex] = lastAltitude;

			timeHigh[lastTourIndex] += lastTime;
			distanceHigh[lastTourIndex] += lastDistance;
			altitudeHigh[lastTourIndex] += lastAltitude;
		}
	}
}
