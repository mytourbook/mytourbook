/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Year extends DataProvider {

	private static DataProvider_Tour_Year	_instance;

	private TourData_Year				_tourDataYear;

	private DataProvider_Tour_Year() {}

	public static DataProvider_Tour_Year getInstance() {
		if (_instance == null) {
			_instance = new DataProvider_Tour_Year();
		}
		return _instance;
	}

	TourData_Year getYearData(	final TourPerson person,
								final TourTypeFilter tourTypeFilter,
								final int lastYear,
								final int numberOfYears,
								final boolean refreshData) {

		/*
		 * check if the required data are already loaded
		 */
		if (_activePerson == person
				&& _activeTourTypeFilter == tourTypeFilter
				&& lastYear == _lastYear
				&& numberOfYears == _numberOfYears
				&& refreshData == false) {

			return _tourDataYear;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;
		_lastYear = lastYear;
		_numberOfYears = numberOfYears;

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] allTourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		_tourDataYear = new TourData_Year();
		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = //
		"SELECT " // //$NON-NLS-1$
				+ "StartYear				, " // 		1 //$NON-NLS-1$
				+ "SUM(TOURDISTANCE)		, " // 		2 //$NON-NLS-1$
				+ "SUM(TOURALTUP)			, " // 		3 //$NON-NLS-1$
				+ "SUM(CASE WHEN tourDrivingTime > 0 THEN tourDrivingTime ELSE tourRecordingTime END)," // 4 //$NON-NLS-1$
				+ "SUM(TourRecordingTime)	, " //		5 //$NON-NLS-1$
				+ "SUM(TourDrivingTime)		, " //		6 //$NON-NLS-1$
				+ "tourType_typeId 			\n" //		7 //$NON-NLS-1$

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$

				+ (" WHERE STARTYEAR IN (" + getYearList(lastYear, numberOfYears) + ")") //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()

				+ (" GROUP BY STARTYEAR, tourType_typeId") //$NON-NLS-1$
				+ (" ORDER BY STARTYEAR"); //$NON-NLS-1$

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		int serieLength = colorOffset + allTourTypes.length;
		serieLength = serieLength == 0 ? 1 : serieLength;
		final int valueLength = numberOfYears;

		try {

			final float[][] dbDistance = new float[serieLength][valueLength];
			final float[][] dbAltitude = new float[serieLength][valueLength];
			final int[][] dbTime = new int[serieLength][valueLength];

			final int[][] dbRecordingTime = new int[serieLength][valueLength];
			final int[][] dbDrivingTime = new int[serieLength][valueLength];
			final int[][] dbBreakTime = new int[serieLength][valueLength];

			final long[][] dbTourTypeIds = new long[serieLength][valueLength];

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int resultYear = result.getInt(1);
				final int yearIndex = numberOfYears - (lastYear - resultYear + 1);

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */

				// set default color index
				int colorIndex = 0;

				// get colorIndex from the type id
				final Long dbTourTypeIdObject = (Long) result.getObject(7);
				if (dbTourTypeIdObject != null) {
					final long dbTypeId = result.getLong(7);
					for (int typeIndex = 0; typeIndex < allTourTypes.length; typeIndex++) {
						if (dbTypeId == allTourTypes[typeIndex].getTypeId()) {
							colorIndex = colorOffset + typeIndex;
							break;
						}
					}
				}

				dbTourTypeIds[colorIndex][yearIndex] = dbTourTypeIdObject == null ? -1 : dbTourTypeIdObject;
				dbDistance[colorIndex][yearIndex] = (int) ((result.getInt(2) + 500) / 1000 / UI.UNIT_VALUE_DISTANCE);
				dbAltitude[colorIndex][yearIndex] = (int) (result.getInt(3) / UI.UNIT_VALUE_ALTITUDE);
				dbTime[colorIndex][yearIndex] = result.getInt(4);

				final int recordingTime = result.getInt(5);
				final int drivingTime = result.getInt(6);

				dbRecordingTime[colorIndex][yearIndex] = recordingTime;
				dbDrivingTime[colorIndex][yearIndex] = drivingTime;
				dbBreakTime[colorIndex][yearIndex] = recordingTime - drivingTime;

			}

			conn.close();

			final int[] years = new int[_numberOfYears];
			int yearIndex = 0;
			for (int currentYear = _lastYear - _numberOfYears + 1; currentYear <= _lastYear; currentYear++) {
				years[yearIndex++] = currentYear;
			}

			_tourDataYear.typeIds = dbTourTypeIds;
			_tourDataYear.years = years;

			_tourDataYear.distanceLow = new float[serieLength][valueLength];
			_tourDataYear.altitudeLow = new float[serieLength][valueLength];
			_tourDataYear.setTimeLow(new int[serieLength][valueLength]);

			_tourDataYear.distanceHigh = dbDistance;
			_tourDataYear.altitudeHigh = dbAltitude;
			_tourDataYear.setTimeHigh(dbTime);

			_tourDataYear.recordingTime = dbRecordingTime;
			_tourDataYear.drivingTime = dbDrivingTime;
			_tourDataYear.breakTime = dbBreakTime;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourDataYear;
	}
}
