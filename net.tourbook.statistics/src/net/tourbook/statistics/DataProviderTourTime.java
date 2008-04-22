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

public class DataProviderTourTime extends DataProvider {

	private static DataProviderTourTime	fInstance;

	private ArrayList<Long>				fTourIds;

	private Long						fSelectedTourId;

	private TourTimeData				fTourDataTime;

	private DataProviderTourTime() {}

	public static DataProviderTourTime getInstance() {
		if (fInstance == null) {
			fInstance = new DataProviderTourTime();
		}
		return fInstance;
	}

	public Long getSelectedTourId() {
		return fSelectedTourId;
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
	TourTimeData getTourTimeData(	final TourPerson person,
									final TourTypeFilter tourTypeFilter,
									final int lastYear,
									int numberOfYears,
									final boolean refreshData) {

		// dont reload data which are already here
		if (fActivePerson == person
				&& fActiveTourTypeFilter == tourTypeFilter
				&& fLastYear == lastYear
				&& fNumberOfYears == numberOfYears
				&& refreshData == false) {
			return fTourDataTime;
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

		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final String sqlString = "SELECT " //$NON-NLS-1$
				+ "TourId, " //				1 //$NON-NLS-1$
				+ "StartYear, " //			2 //$NON-NLS-1$
				+ "StartMonth, " //			3 //$NON-NLS-1$
				+ "StartDay, " //			4 //$NON-NLS-1$
				+ "StartHour, " //			5 //$NON-NLS-1$
				+ "StartMinute, " //		6 //$NON-NLS-1$
				+ "TourDistance, " //		7 //$NON-NLS-1$
				+ "TourAltUp, " //			8 //$NON-NLS-1$
				+ "TourRecordingTime, " //	9 //$NON-NLS-1$
				+ "TourDrivingTime, "//		10 //$NON-NLS-1$
				+ "TourTitle, " //			11 //$NON-NLS-1$
				+ "TourType_typeId, "//		12 //$NON-NLS-1$
				+ "TourDescription " // 	13 //$NON-NLS-1$
				+ "\n" //$NON-NLS-1$

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" ORDER BY StartYear, StartMonth, StartDay, StartHour, StartMinute"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			final ArrayList<String> dbTourTitle = new ArrayList<String>();
			final ArrayList<String> dbTourDescription = new ArrayList<String>();

			final ArrayList<Integer> dbTourYear = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourMonths = new ArrayList<Integer>();
			final ArrayList<Integer> dbAllYearsDOY = new ArrayList<Integer>(); // DOY...Day Of Year for all years

			final ArrayList<Integer> dbTourStartTime = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourEndTime = new ArrayList<Integer>();

			final ArrayList<Integer> dbDistance = new ArrayList<Integer>();
			final ArrayList<Integer> dbAltitude = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourRecordingTime = new ArrayList<Integer>();
			final ArrayList<Integer> dbTourDrivingTime = new ArrayList<Integer>();

			final ArrayList<Long> dbTypeIds = new ArrayList<Long>();
			final ArrayList<Integer> dbTypeColorIndex = new ArrayList<Integer>();

			fTourIds = new ArrayList<Long>();

			while (result.next()) {

				fTourIds.add(result.getLong(1));

				final int tourYear = result.getShort(2);
				final int tourMonth = result.getShort(3) - 1;
				final int startHour = result.getShort(5);
				final int startMinute = result.getShort(6);
				final int startTime = startHour * 3600 + startMinute * 60;

				final int recordingTime = result.getInt(9);
				fCalendar.set(tourYear, tourMonth, result.getShort(4), startHour, startMinute);
				final int tourDOY = fCalendar.get(Calendar.DAY_OF_YEAR) - 1;

				dbTourYear.add(tourYear);
				dbTourMonths.add(tourMonth);
				dbAllYearsDOY.add(getYearDOYs(tourYear) + tourDOY);

				dbTourStartTime.add(startTime);
				dbTourEndTime.add((startTime + recordingTime));

				dbDistance.add((int) (result.getInt(7) / 1000 / UI.UNIT_VALUE_DISTANCE));
				dbAltitude.add((int) (result.getInt(8) / UI.UNIT_VALUE_ALTITUDE));

				dbTourRecordingTime.add(recordingTime);
				dbTourDrivingTime.add(result.getInt(10));

				dbTourTitle.add(result.getString(11));

				final String description = result.getString(13);
				dbTourDescription.add(description == null ? UI.EMPTY_STRING : description);

				/*
				 * convert type id to the type index in the tour type array, this is also the color
				 * index for the tour type
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
				dbTypeIds.add(dbTypeIdObject == null ? TourType.TOUR_TYPE_ID_NOT_DEFINED : dbTypeIdObject);
			}

			conn.close();

			// get number of days for all years
			int yearDays = 0;
			for (int doy : fYearDays) {
				yearDays += doy;
			}

			/*
			 * create data
			 */
			fTourDataTime = new TourTimeData();

			fTourDataTime.fTourIds = ArrayListToArray.toLong(fTourIds);

			fTourDataTime.fTypeIds = ArrayListToArray.toLong(dbTypeIds);
			fTourDataTime.fTypeColorIndex = ArrayListToArray.toInt(dbTypeColorIndex);

			fTourDataTime.allDaysInAllYears = yearDays;
			fTourDataTime.yearDays = fYearDays;
			fTourDataTime.years = fYears;

			fTourDataTime.fTourYearValues = ArrayListToArray.toInt(dbTourYear);
			fTourDataTime.fTourMonthValues = ArrayListToArray.toInt(dbTourMonths);
			fTourDataTime.fTourDOYValues = ArrayListToArray.toInt(dbAllYearsDOY);

			fTourDataTime.fTourTimeStartValues = ArrayListToArray.toInt(dbTourStartTime);
			fTourDataTime.fTourTimeEndValues = ArrayListToArray.toInt(dbTourEndTime);

			fTourDataTime.fTourDistanceValues = ArrayListToArray.toInt(dbDistance);
			fTourDataTime.fTourAltitudeValues = ArrayListToArray.toInt(dbAltitude);

			fTourDataTime.fTourRecordingTimeValues = dbTourRecordingTime;
			fTourDataTime.fTourDrivingTimeValues = dbTourDrivingTime;

			fTourDataTime.fTourTitle = dbTourTitle;
			fTourDataTime.tourDescription = dbTourDescription;

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return fTourDataTime;
	}

	void setSelectedTourId(Long selectedTourId) {
		fSelectedTourId = selectedTourId;
	}

}
