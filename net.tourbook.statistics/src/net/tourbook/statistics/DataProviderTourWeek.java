/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProviderTourWeek extends DataProvider {

	private static DataProviderTourWeek	fInstance;

	private TourDataWeek				fTourWeekData;

	private DataProviderTourWeek() {}

	public static DataProviderTourWeek getInstance() {
		if (fInstance == null) {
			fInstance = new DataProviderTourWeek();
		}
		return fInstance;
	}

	TourDataWeek getWeekData(	TourPerson person,
								TourTypeFilter tourTypeFilter,
								int lastYear,
								int numberOfYears,
								boolean refreshData) {

		// when the data for the year are already loaded, all is done
		if (fActivePerson == person
				&& fActiveTourTypeFilter == tourTypeFilter
				&& lastYear == fLastYear
				&& numberOfYears == fNumberOfYears
				&& refreshData == false) {
			return fTourWeekData;
		}

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;

		fLastYear = lastYear;
		fNumberOfYears = numberOfYears;

		initYearNumbers();

		fTourWeekData = new TourDataWeek();

		// get the tour types
		ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		int weekCounter = 0;
		for (int weeks : fYearWeeks) {
			weekCounter += weeks;
		}

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		final int serieLength = colorOffset + tourTypes.length;
		final int valueLength = weekCounter;

		String sqlString = "SELECT " //$NON-NLS-1$
				+ "StartYear			, " // 1 //$NON-NLS-1$
				+ "StartWeek			, " // 2 //$NON-NLS-1$
				+ "SUM(TourDistance)	, " // 3 //$NON-NLS-1$
				+ "SUM(TourAltUp)		, " // 4 //$NON-NLS-1$
				+ "SUM(CASE WHEN TourDrivingTime > 0 THEN TourDrivingTime ELSE TourRecordingTime END)," // 5 //$NON-NLS-1$
				+ "TourType_TypeId 		\n" // 6 //$NON-NLS-1$
				//
				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")") //$NON-NLS-1$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" GROUP BY StartYear, StartWeek, tourType_typeId") //$NON-NLS-1$
				+ (" ORDER BY StartYear, StartWeek"); //$NON-NLS-1$

		try {

			final int[][] dbDistance = new int[serieLength][valueLength];
			final int[][] dbAltitude = new int[serieLength][valueLength];
			final int[][] dbTime = new int[serieLength][valueLength];
			final long[][] dbTypeIds = new long[serieLength][valueLength];

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {

				final int dbYear = result.getInt(1);
				final int dbWeek = result.getInt(2);

				// get number of weeks for the current year in the db
				final int dbYearIndex = numberOfYears - (lastYear - dbYear + 1);
				int allWeeks = 0;
				for (int yearIndex = 0; yearIndex <= dbYearIndex; yearIndex++) {
					if (yearIndex > 0) {
						allWeeks += fYearWeeks[yearIndex - 1];
					}
				}

				final int weekIndex = allWeeks + dbWeek - 1;

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */
				int colorIndex = 0;

				final Long dbTypeIdObject = (Long) result.getObject(6);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(6);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = colorOffset + typeIndex;
							break;
						}
					}
				}

				dbTypeIds[colorIndex][weekIndex] = dbTypeIdObject == null
						? TourType.TOUR_TYPE_ID_NOT_DEFINED
						: dbTypeIdObject;

				dbDistance[colorIndex][weekIndex] = (int) (result.getInt(3) / 1000 / UI.UNIT_VALUE_DISTANCE);
				dbAltitude[colorIndex][weekIndex] = (int) (result.getInt(4) / UI.UNIT_VALUE_ALTITUDE);
				dbTime[colorIndex][weekIndex] = result.getInt(5);
			}

			conn.close();

			fTourWeekData.fTypeIds = dbTypeIds;

			fTourWeekData.fYears = fYears;
			fTourWeekData.fYearWeeks = fYearWeeks;
			fTourWeekData.fYearDays = fYearDays;

			fTourWeekData.fTimeLow = new int[serieLength][valueLength];
			fTourWeekData.fTimeHigh = dbTime;

			fTourWeekData.fDistanceLow = new int[serieLength][valueLength];
			fTourWeekData.fDistanceHigh = dbDistance;

			fTourWeekData.fAltitudeLow = new int[serieLength][valueLength];
			fTourWeekData.fAltitudeHigh = dbAltitude;

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return fTourWeekData;
	}

}
