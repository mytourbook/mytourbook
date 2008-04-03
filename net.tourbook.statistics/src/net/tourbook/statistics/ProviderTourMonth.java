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

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class ProviderTourMonth extends DataProvider {

	private static ProviderTourMonth	fInstance;

	private TourDataMonth				fTourMonthData;

	private ProviderTourMonth() {}

	public static ProviderTourMonth getInstance() {
		if (fInstance == null) {
			fInstance = new ProviderTourMonth();
		}
		return fInstance;
	}

	TourDataMonth getMonthData(	final TourPerson person,
								final TourTypeFilter tourTypeFilter,
								final int lastYear,
								final int numberOfYears,
								final boolean refreshData) {

		/*
		 * check if the required data are already loaded
		 */
		if (fActivePerson == person
				&& fActiveTourTypeFilter == tourTypeFilter
				&& lastYear == fLastYear
				&& numberOfYears == fNumberOfYears
				&& refreshData == false) {
			return fTourMonthData;
		}

		fActivePerson = person;
		fActiveTourTypeFilter = tourTypeFilter;
		fLastYear = lastYear;
		fNumberOfYears = numberOfYears;

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		fTourMonthData = new TourDataMonth();

		final String sqlString = //
		"SELECT " // //$NON-NLS-1$
				+ "StartYear				, " // 1 //$NON-NLS-1$
				+ "STARTMONTH				, " // 2 //$NON-NLS-1$
				+ "SUM(TOURDISTANCE)		, " // 3 //$NON-NLS-1$
				+ "SUM(TOURALTUP)			, " // 4 //$NON-NLS-1$
				+ "SUM(CASE WHEN tourDrivingTime > 0 THEN tourDrivingTime ELSE tourRecordingTime END)," // 5 //$NON-NLS-1$
				+ "tourType_typeId 			\n" // 6 //$NON-NLS-1$
				//
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE STARTYEAR IN (" + getYearList(lastYear, numberOfYears) + ")") //$NON-NLS-1$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" GROUP BY STARTYEAR, STARTMONTH, tourType_typeId") //$NON-NLS-1$
				+ (" ORDER BY STARTYEAR, STARTMONTH"); //$NON-NLS-1$

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		final int tourTypeSerieLength = colorOffset + tourTypes.length;
		final int valueLength = 12 * numberOfYears;

		try {

			final int[][] dbDistance = new int[tourTypeSerieLength][valueLength];
			final int[][] dbAltitude = new int[tourTypeSerieLength][valueLength];
			final int[][] dbTime = new int[tourTypeSerieLength][valueLength];
			final long[][] dbTypeIds = new long[tourTypeSerieLength][valueLength];

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				final int resultYear = result.getInt(1);
				final int resultMonth = result.getInt(2);

				final int yearIndex = numberOfYears - (lastYear - resultYear + 1);
				final int monthIndex = (resultMonth - 1) + yearIndex * 12;

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

				dbTypeIds[colorIndex][monthIndex] = dbTypeIdObject == null ? -1 : dbTypeIdObject;
				dbDistance[colorIndex][monthIndex] = (int) (result.getInt(3) / 1000 / UI.UNIT_VALUE_DISTANCE);
				dbAltitude[colorIndex][monthIndex] = (int) (result.getInt(4) / UI.UNIT_VALUE_ALTITUDE);
				dbTime[colorIndex][monthIndex] = result.getInt(5);
			}

			conn.close();

			fTourMonthData.fTypeIds = dbTypeIds;

			fTourMonthData.fDistanceLow = new int[tourTypeSerieLength][valueLength];
			fTourMonthData.fAltitudeLow = new int[tourTypeSerieLength][valueLength];
			fTourMonthData.fTimeLow = new int[tourTypeSerieLength][valueLength];

			fTourMonthData.fDistanceHigh = dbDistance;
			fTourMonthData.fAltitudeHigh = dbAltitude;
			fTourMonthData.fTimeHigh = dbTime;

		} catch (final SQLException e) {
			e.printStackTrace();
		}

		return fTourMonthData;
	}

}
