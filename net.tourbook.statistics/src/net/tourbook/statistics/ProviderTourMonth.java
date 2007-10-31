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

public class ProviderTourMonth extends DataProvider {

	private static final int			YEAR_MONTHS	= 12;

	private static ProviderTourMonth	fInstance;

	private TourPerson					fActivePerson;
	private int							fCurrentYear;
	private TourDataMonth				fTourMonthData;

	private TourTypeFilter				fActiveTourTypeFilter;

	static int[]						fAllMonths;

	private ProviderTourMonth() {}

	public static ProviderTourMonth getInstance() {
		if (fInstance == null) {

			fInstance = new ProviderTourMonth();

			// create month array
			fAllMonths = new int[YEAR_MONTHS];
			for (int month = 0; month < YEAR_MONTHS; month++) {
				fAllMonths[month] = month;
			}
		}
		return fInstance;
	}

	TourDataMonth getMonthData(	TourPerson person,
								TourTypeFilter tourTypeFilter,
								int year,
								boolean refreshData) {

		// when the data for the year are already loaded, all is done
		if (fActivePerson == person
				&& fActiveTourTypeFilter == tourTypeFilter
				&& year == fCurrentYear
				&& refreshData == false) {
			return fTourMonthData;
		}

		// get the tour types
		ArrayList<TourType> tourTypeList = TourDatabase.getTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		fTourMonthData = new TourDataMonth();

		String sqlString = //
		"SELECT " // //$NON-NLS-1$
				+ "STARTMONTH				, "// 1 //$NON-NLS-1$
				+ "SUM(TOURDISTANCE)		, "// 2 //$NON-NLS-1$
				+ "SUM(TOURALTUP)			, "// 3 //$NON-NLS-1$
				+ "SUM(CASE WHEN tourDrivingTime > 0 THEN tourDrivingTime ELSE tourRecordingTime END)," // 4 //$NON-NLS-1$
				+ "tourType_typeId 			\n"// 4 //$NON-NLS-1$
				//
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE STARTYEAR=" + Integer.toString(year)) //$NON-NLS-1$
				+ getSQLFilter(person, tourTypeFilter)
				+ (" GROUP BY STARTMONTH, tourType_typeId") //$NON-NLS-1$
				+ (" ORDER BY STARTMONTH"); //$NON-NLS-1$

		final int serieLength = tourTypes.length + StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		final int valueLength = fAllMonths.length;

		try {

			final int[][] dbDistance = new int[serieLength][valueLength];
			final int[][] dbAltitude = new int[serieLength][valueLength];
			final int[][] dbTime = new int[serieLength][valueLength];
			final long[][] dbTypeIds = new long[serieLength][valueLength];

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {

				final int month = result.getInt(1) - 1;

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */
				int colorIndex = 0;

				Long dbTypeIdObject = (Long) result.getObject(5);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(5);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = typeIndex + StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
							break;
						}
					}
				}

				dbTypeIds[colorIndex][month] = dbTypeIdObject == null ? -1 : dbTypeIdObject;

				dbDistance[colorIndex][month] = result.getInt(2) / 1000;
				dbAltitude[colorIndex][month] = result.getInt(3);
				dbTime[colorIndex][month] = result.getInt(4);
			}

			conn.close();

			fActivePerson = person;
			fActiveTourTypeFilter = tourTypeFilter;
			fCurrentYear = year;

			fTourMonthData.fTypeIds = dbTypeIds;

			fTourMonthData.fDistanceLow = new int[serieLength][valueLength];
			fTourMonthData.fAltitudeLow = new int[serieLength][valueLength];
			fTourMonthData.fTimeLow = new int[serieLength][valueLength];

			fTourMonthData.fDistanceHigh = dbDistance;
			fTourMonthData.fAltitudeHigh = dbAltitude;
			fTourMonthData.fTimeHigh = dbTime;

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return fTourMonthData;
	}

}
