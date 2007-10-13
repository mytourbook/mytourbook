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
import net.tourbook.plugin.TourbookPlugin;

public class ProviderTourWeek extends DataProvider {

	static final int				YEAR_WEEKS	= 53;

	private static ProviderTourWeek	fInstance;

	private int						fCurrentYear;
	private TourPerson				fActivePerson;

	private TourDataWeek			fTourWeekData;

	private long					fActiveTypeId;

	static int[]					fAllWeeks;

	private ProviderTourWeek() {}

	public static ProviderTourWeek getInstance() {
		if (fInstance == null) {

			fInstance = new ProviderTourWeek();

			// create month array
			fAllWeeks = new int[YEAR_WEEKS];
			for (int week = 0; week < YEAR_WEEKS; week++) {
				fAllWeeks[week] = week;
			}
		}
		return fInstance;
	}

	TourDataWeek getWeekData(TourPerson person, long typeId, int year, boolean refreshData) {

		// when the data for the year are already loaded, all is done
		if (fActivePerson == person
				&& fActiveTypeId == typeId
				&& year == fCurrentYear
				&& refreshData == false) {
			return fTourWeekData;
		}

		fTourWeekData = new TourDataWeek();

		// get the tour types
		ArrayList<TourType> tourTypeList = TourbookPlugin.getDefault().getAllTourTypes();
		TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		final int serieLength = tourTypes.length;
		final int valueLength = fAllWeeks.length;

		String sqlString = "SELECT " // //$NON-NLS-1$
				//
				+ "StartWeek			, "// 1 //$NON-NLS-1$
				+ "SUM(TOURDISTANCE)	, "// 2 //$NON-NLS-1$
				+ "SUM(TOURALTUP)		, "// 3 //$NON-NLS-1$
				+ "SUM(CASE WHEN tourDrivingTime > 0 THEN tourDrivingTime ELSE tourRecordingTime END)," // 4 //$NON-NLS-1$
				+ "tourType_typeId 		\n"// 5 //$NON-NLS-1$
				//
				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("WHERE StartYear =" + Integer.toString(year)) //$NON-NLS-1$
				+ getSQLFilter(person, typeId)
				+ " GROUP BY StartWeek, tourType_typeId" //$NON-NLS-1$
				+ " ORDER BY StartWeek"; //$NON-NLS-1$

		try {

			final int[][] dbDistance = new int[serieLength][valueLength];
			final int[][] dbAltitude = new int[serieLength][valueLength];
			final int[][] dbTime = new int[serieLength][valueLength];
			final long[][] dbTypeIds = new long[serieLength][valueLength];

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {

				// first week is 0
				final int week = result.getInt(1);

				/*
				 * convert type id to the type index in the tour types list
				 * which is also the color index
				 */
				int colorIndex = 0;

				final Long dbTypeIdObject = (Long) result.getObject(5);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(5);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = typeIndex;
							break;
						}
					}
				}

				dbTypeIds[colorIndex][week] = dbTypeIdObject == null
						? TourType.TOUR_TYPE_ID_NOT_DEFINED
						: dbTypeIdObject;

				dbDistance[colorIndex][week] = result.getInt(2) / 1000;
				dbAltitude[colorIndex][week] = result.getInt(3);
				dbTime[colorIndex][week] = result.getInt(4);
			}

			conn.close();

			fActivePerson = person;
			fActiveTypeId = typeId;
			fCurrentYear = year;

			fTourWeekData.fTypeIds = dbTypeIds;

			fTourWeekData.fDistanceLow = new int[serieLength][valueLength];
			fTourWeekData.fAltitudeLow = new int[serieLength][valueLength];
			fTourWeekData.fTimeLow = new int[serieLength][valueLength];

			fTourWeekData.fDistanceHigh = dbDistance;
			fTourWeekData.fAltitudeHigh = dbAltitude;
			fTourWeekData.fTimeHigh = dbTime;

		} catch (SQLException e) {
			e.printStackTrace();
		}

		return fTourWeekData;
	}

}
