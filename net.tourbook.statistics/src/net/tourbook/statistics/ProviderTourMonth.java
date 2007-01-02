/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.plugin.TourbookPlugin;

public class ProviderTourMonth extends DataProvider {

	private static final int			YEAR_MONTHS	= 12;

	private static ProviderTourMonth	fInstance;

	private TourPerson					fActivePerson;
	private int							fCurrentYear;
	private TourDataMonth				fTourMonthData;

	private long						fActiveTypeId;

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

	TourDataMonth getMonthData(TourPerson person, long typeId, int year, boolean refreshData) {

		// when the data for the year are already loaded, all is done
		if (fActivePerson == person
				&& fActiveTypeId == typeId
				&& year == fCurrentYear
				&& refreshData == false) {
			return fTourMonthData;
		}

		// get the tour types
		TourType[] tourTypes = TourbookPlugin.getDefault().getTourTypesArray();

		fTourMonthData = new TourDataMonth();

		String sqlString = //
		"SELECT " //
				+ "STARTMONTH				, "// 1
				+ "SUM(TOURDISTANCE)		, "// 2
				+ "SUM(TOURALTUP)			, "// 3
				+ "SUM(CASE WHEN tourDrivingTime > 0 THEN tourDrivingTime ELSE tourRecordingTime END)," // 4
				+ "tourType_typeId 			\n"// 4
				//
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n")
				+ (" WHERE STARTYEAR=" + Integer.toString(year))
				+ getSQLFilter(person, typeId)
				+ (" GROUP BY STARTMONTH, tourType_typeId")
				+ (" ORDER BY STARTMONTH");

		final int serieLength = tourTypes.length;
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
				 * convert type id to the type index in the tour types list
				 * which is also the color index
				 */
				int colorIndex = 0;

				Long dbTypeIdObject = (Long) result.getObject(5);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(5);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = typeIndex;
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
			fActiveTypeId = typeId;
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
