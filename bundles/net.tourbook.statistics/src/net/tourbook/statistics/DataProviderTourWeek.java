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
package net.tourbook.statistics;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProviderTourWeek extends DataProvider {

	private static DataProviderTourWeek	_instance;

	private TourDataWeek				_tourWeekData;

	private DataProviderTourWeek() {}

	public static DataProviderTourWeek getInstance() {
		if (_instance == null) {
			_instance = new DataProviderTourWeek();
		}
		return _instance;
	}

	TourDataWeek getWeekData(	final TourPerson person,
								final TourTypeFilter tourTypeFilter,
								final int lastYear,
								final int numberOfYears,
								final boolean refreshData) {

		// when the data for the year are already loaded, all is done
		if (_activePerson == person
				&& _activeTourTypeFilter == tourTypeFilter
				&& lastYear == _lastYear
				&& numberOfYears == _numberOfYears
				&& refreshData == false) {
			return _tourWeekData;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;

		_lastYear = lastYear;
		_numberOfYears = numberOfYears;

		initYearNumbers();

		_tourWeekData = new TourDataWeek();

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		int weekCounter = 0;
		for (final int weeks : _yearWeeks) {
			weekCounter += weeks;
		}

		int colorOffset = 0;
		if (tourTypeFilter.showUndefinedTourTypes()) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		int serieLength = colorOffset + tourTypes.length;
		serieLength = serieLength == 0 ? 1 : serieLength;

		final int valueLength = weekCounter;
		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT \n" //$NON-NLS-1$
				+ " StartWeekYear,			\n" // 1 //$NON-NLS-1$
				+ " StartWeek,				\n" // 2 //$NON-NLS-1$
				+ " SUM(TourDistance),		\n" // 3 //$NON-NLS-1$
				+ " SUM(TourAltUp),			\n" // 4 //$NON-NLS-1$
				+ " SUM(CASE WHEN TourDrivingTime > 0 THEN TourDrivingTime ELSE TourRecordingTime END),\n" // 5 //$NON-NLS-1$
				+ " SUM(TourRecordingTime),	\n" // 6 //$NON-NLS-1$
				+ " SUM(TourDrivingTime),	\n" // 7 //$NON-NLS-1$
				+ " TourType_TypeId 		\n" // 8 //$NON-NLS-1$
				//
				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$

				+ (" WHERE StartWeekYear IN (" + getYearList(lastYear, numberOfYears) + ")\n") //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()

				+ (" GROUP BY StartWeekYear, StartWeek, tourType_typeId \n") //$NON-NLS-1$
				+ (" ORDER BY StartWeekYear, StartWeek"); //$NON-NLS-1$

		try {

			final float[][] dbDistance = new float[serieLength][valueLength];
			final float[][] dbAltitude = new float[serieLength][valueLength];

			final int[][] dbDurationTime = new int[serieLength][valueLength];
			final int[][] dbRecordingTime = new int[serieLength][valueLength];
			final int[][] dbDrivingTime = new int[serieLength][valueLength];
			final int[][] dbBreakTime = new int[serieLength][valueLength];

			final long[][] dbTypeIds = new long[serieLength][valueLength];

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);
				final int dbWeek = result.getInt(2);

				// get number of weeks for the current year in the db
				final int dbYearIndex = numberOfYears - (lastYear - dbYear + 1);
				int allWeeks = 0;
				for (int yearIndex = 0; yearIndex <= dbYearIndex; yearIndex++) {
					if (yearIndex > 0) {
						allWeeks += _yearWeeks[yearIndex - 1];
					}
				}

				final int weekIndex = allWeeks + dbWeek - 1;

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */
				int colorIndex = 0;
				final Long dbTypeIdObject = (Long) result.getObject(8);
				if (dbTypeIdObject != null) {
					final long dbTypeId = result.getLong(8);
					for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
						if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
							colorIndex = colorOffset + typeIndex;
							break;
						}
					}
				}

				final long dbTypeId = dbTypeIdObject == null ? TourDatabase.ENTITY_IS_NOT_SAVED : dbTypeIdObject;

				dbTypeIds[colorIndex][weekIndex] = dbTypeId;

				dbDistance[colorIndex][weekIndex] = (int) (result.getInt(3) / UI.UNIT_VALUE_DISTANCE);
				dbAltitude[colorIndex][weekIndex] = (int) (result.getInt(4) / UI.UNIT_VALUE_ALTITUDE);
				dbDurationTime[colorIndex][weekIndex] = result.getInt(5);

				final int recordingTime = result.getInt(6);
				final int drivingTime = result.getInt(7);

				dbRecordingTime[colorIndex][weekIndex] = recordingTime;
				dbDrivingTime[colorIndex][weekIndex] = drivingTime;
				dbBreakTime[colorIndex][weekIndex] = recordingTime - drivingTime;
			}

			conn.close();

			_tourWeekData.typeIds = dbTypeIds;

			_tourWeekData.years = _years;
			_tourWeekData.yearWeeks = _yearWeeks;
			_tourWeekData.yearDays = _yearDays;

			_tourWeekData.setTimeLow(new int[serieLength][valueLength]);
			_tourWeekData.setTimeHigh(dbDurationTime);

			_tourWeekData.distanceLow = new float[serieLength][valueLength];
			_tourWeekData.distanceHigh = dbDistance;

			_tourWeekData.altitudeLow = new float[serieLength][valueLength];
			_tourWeekData.altitudeHigh = dbAltitude;

			_tourWeekData.recordingTime = dbRecordingTime;
			_tourWeekData.drivingTime = dbDrivingTime;
			_tourWeekData.breakTime = dbBreakTime;

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourWeekData;
	}
}
