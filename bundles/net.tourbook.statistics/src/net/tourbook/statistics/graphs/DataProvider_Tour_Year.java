/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
import java.util.Arrays;

import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Year extends DataProvider {

	private static DataProvider_Tour_Year	_instance;

	private TourData_Year						_tourDataYear;

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
										final int numYears,
										final boolean refreshData) {

		/*
		 * check if the required data are already loaded
		 */
		if (_activePerson == person
				&& _activeTourTypeFilter == tourTypeFilter
				&& lastYear == _lastYear
				&& numYears == _numberOfYears
				&& refreshData == false) {

			return _tourDataYear;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;
		_lastYear = lastYear;
		_numberOfYears = numYears;

		// get the tour types
		final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
		final TourType[] allTourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

		_tourDataYear = new TourData_Year();

		String fromTourData;

		final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);
		if (sqlFilter.isTagFilterActive()) {

			// with tag filter

			fromTourData = NL

					+ "FROM (							" + NL //$NON-NLS-1$

					+ " SELECT							" + NL //$NON-NLS-1$

					+ "  StartYear,					" + NL //$NON-NLS-1$
					+ "  TourDistance,				" + NL //$NON-NLS-1$
					+ "  TourAltUp,					" + NL //$NON-NLS-1$
					+ "  TourRecordingTime,			" + NL //$NON-NLS-1$
					+ "  TourDrivingTime,			" + NL //$NON-NLS-1$

					+ "  TourType_TypeId, 			" + NL //$NON-NLS-1$
					+ "  jTdataTtag.TourTag_tagId	" + NL //$NON-NLS-1$

					+ (" FROM " + TourDatabase.TABLE_TOUR_DATA) + NL//$NON-NLS-1$

					// get tag id's
					+ (" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag") + NL //$NON-NLS-1$ //$NON-NLS-2$
					+ (" ON tourID = jTdataTtag.TourData_tourId") + NL //$NON-NLS-1$

					+ (" WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")") + NL //$NON-NLS-1$ //$NON-NLS-2$
					+ sqlFilter.getWhereClause() + NL

					+ ") td" //$NON-NLS-1$
			;

		} else {

			// without tag filter

			fromTourData = NL

					+ (" FROM " + TourDatabase.TABLE_TOUR_DATA) + NL //$NON-NLS-1$

					+ (" WHERE StartYear IN (" + getYearList(lastYear, numYears) + ")") + NL //$NON-NLS-1$ //$NON-NLS-2$
					+ sqlFilter.getWhereClause()

			;
		}

		final String sqlString = NL +

				"SELECT" + NL //$NON-NLS-1$

				+ " StartYear,						" + NL // 1 //$NON-NLS-1$
				+ " SUM(TourDistance),			" + NL // 2 //$NON-NLS-1$
				+ " SUM(TourAltUp),				" + NL // 3 //$NON-NLS-1$
				+ " SUM(CASE WHEN TourDrivingTime > 0 THEN TourDrivingTime ELSE TourRecordingTime END)," + NL // 4 //$NON-NLS-1$
				+ " SUM(TourRecordingTime),	" + NL // 5 //$NON-NLS-1$
				+ " SUM(TourDrivingTime),		" + NL // 6 //$NON-NLS-1$
				+ " SUM(1), 						" + NL // 7 //$NON-NLS-1$
				+ " TourType_TypeId 				" + NL // 8 //$NON-NLS-1$

				+ fromTourData

				+ (" GROUP BY StartYear, tourType_typeId ") + NL //		//$NON-NLS-1$
				+ (" ORDER BY StartYear") + NL //						//$NON-NLS-1$
		;

		final boolean isShowNoTourTypes = tourTypeFilter.showUndefinedTourTypes();

		int colorOffset = 0;
		if (isShowNoTourTypes) {
			colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
		}

		int numTourTypes = colorOffset + allTourTypes.length;
		numTourTypes = numTourTypes == 0 ? 1 : numTourTypes; // ensure that at least 1 is available

		try {

			final float[][] dbDistance = new float[numTourTypes][numYears];
			final float[][] dbAltitude = new float[numTourTypes][numYears];
			final float[][] dbNumTours = new float[numTourTypes][numYears];

			final int[][] dbDurationTime = new int[numTourTypes][numYears];
			final int[][] dbRecordingTime = new int[numTourTypes][numYears];
			final int[][] dbDrivingTime = new int[numTourTypes][numYears];
			final int[][] dbBreakTime = new int[numTourTypes][numYears];

			final long[][] dbTypeIds = new long[numTourTypes][numYears];
			final long[] tourTypeSum = new long[numTourTypes];
			final long[] usedTourTypeIds = new long[numTourTypes];
			Arrays.fill(usedTourTypeIds, TourType.TOUR_TYPE_IS_NOT_USED);

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbValue_ResultYear = result.getInt(1);
				final int dbValue_Altitude = (int) (result.getInt(3) / UI.UNIT_VALUE_ALTITUDE);
				final int dbValue_Distance = (int) ((result.getInt(2) + 500) / 1000 / UI.UNIT_VALUE_DISTANCE);
				final int dbValue_Duration = result.getInt(4);
				final int dbValue_RecordingTime = result.getInt(5);
				final int dbValue_DrivingTime = result.getInt(6);
				final int dbValue_NumTours = result.getInt(7);
				final Long dbValue_TourTypeIdObject = (Long) result.getObject(8);

				final int yearIndex = numYears - (lastYear - dbValue_ResultYear + 1);

				/*
				 * convert type id to the type index in the tour types list which is also the color
				 * index
				 */

				// set default color index
				int colorIndex = 0;

				// get colorIndex from the type id
				if (dbValue_TourTypeIdObject != null) {

					final long dbTypeId = dbValue_TourTypeIdObject;

					for (int typeIndex = 0; typeIndex < allTourTypes.length; typeIndex++) {
						if (dbTypeId == allTourTypes[typeIndex].getTypeId()) {
							colorIndex = colorOffset + typeIndex;
							break;
						}
					}
				}

				final long noTourTypeId = isShowNoTourTypes
						? TourType.TOUR_TYPE_IS_NOT_DEFINED_IN_TOUR_DATA
						: TourType.TOUR_TYPE_IS_NOT_USED;

				final long typeId = dbValue_TourTypeIdObject == null ? noTourTypeId : dbValue_TourTypeIdObject;

				dbTypeIds[colorIndex][yearIndex] = typeId;

				dbAltitude[colorIndex][yearIndex] = dbValue_Altitude;
				dbDistance[colorIndex][yearIndex] = dbValue_Distance;
				dbDurationTime[colorIndex][yearIndex] = dbValue_Duration;
				dbNumTours[colorIndex][yearIndex] = dbValue_NumTours;

				dbRecordingTime[colorIndex][yearIndex] = dbValue_RecordingTime;
				dbDrivingTime[colorIndex][yearIndex] = dbValue_DrivingTime;
				dbBreakTime[colorIndex][yearIndex] = dbValue_RecordingTime - dbValue_DrivingTime;

				usedTourTypeIds[colorIndex] = typeId;
				tourTypeSum[colorIndex] += dbValue_Distance + dbValue_Altitude + dbValue_RecordingTime;
			}

			conn.close();

			final int[] years = new int[_numberOfYears];
			int yearIndex = 0;
			for (int currentYear = _lastYear - _numberOfYears + 1; currentYear <= _lastYear; currentYear++) {
				years[yearIndex++] = currentYear;
			}
			_tourDataYear.years = years;

			/*
			 * Remove not used tour types
			 */
			final ArrayList<Object> typeIdsWithData = new ArrayList<>();

			final ArrayList<Object> altitudeWithData = new ArrayList<>();
			final ArrayList<Object> distanceWithData = new ArrayList<>();
			final ArrayList<Object> durationWithData = new ArrayList<>();
			final ArrayList<Object> numToursWithData = new ArrayList<>();

			final ArrayList<Object> recordingTimeWithData = new ArrayList<>();
			final ArrayList<Object> drivingTimeWithData = new ArrayList<>();
			final ArrayList<Object> breakTimeWithData = new ArrayList<>();

			for (int tourTypeIndex = 0; tourTypeIndex < tourTypeSum.length; tourTypeIndex++) {

				final long summary = tourTypeSum[tourTypeIndex];

				if (summary > 0) {

					typeIdsWithData.add(dbTypeIds[tourTypeIndex]);

					altitudeWithData.add(dbAltitude[tourTypeIndex]);
					distanceWithData.add(dbDistance[tourTypeIndex]);
					durationWithData.add(dbDurationTime[tourTypeIndex]);
					numToursWithData.add(dbNumTours[tourTypeIndex]);

					recordingTimeWithData.add(dbRecordingTime[tourTypeIndex]);
					drivingTimeWithData.add(dbDrivingTime[tourTypeIndex]);
					breakTimeWithData.add(dbBreakTime[tourTypeIndex]);
				}
			}

			/*
			 * Create statistic data
			 */
			final int numUsedTourTypes = typeIdsWithData.size();

			if (numUsedTourTypes == 0) {

				// there are NO data, create dummy data that the UI do not fail

				_tourDataYear.typeIds = new long[1][1];
				_tourDataYear.usedTourTypeIds = new long[] { TourType.TOUR_TYPE_IS_NOT_USED };

				_tourDataYear.altitudeLow = new float[1][numYears];
				_tourDataYear.altitudeHigh = new float[1][numYears];

				_tourDataYear.distanceLow = new float[1][numYears];
				_tourDataYear.distanceHigh = new float[1][numYears];

				_tourDataYear.setDurationTimeLow(new int[1][numYears]);
				_tourDataYear.setDurationTimeHigh(new int[1][numYears]);

				_tourDataYear.recordingTime = new int[1][numYears];
				_tourDataYear.drivingTime = new int[1][numYears];
				_tourDataYear.breakTime = new int[1][numYears];

				_tourDataYear.numToursLow = new float[1][numYears];
				_tourDataYear.numToursHigh = new float[1][numYears];

			} else {

				final long[][] usedTypeIds = new long[numUsedTourTypes][];

				final float[][] usedAltitude = new float[numUsedTourTypes][];
				final float[][] usedDistance = new float[numUsedTourTypes][];
				final int[][] usedDuration = new int[numUsedTourTypes][];
				final int[][] usedRecordingTime = new int[numUsedTourTypes][];
				final int[][] usedDrivingTime = new int[numUsedTourTypes][];
				final int[][] usedBreakTime = new int[numUsedTourTypes][];
				final float[][] usedNumTours = new float[numUsedTourTypes][];

				for (int index = 0; index < numUsedTourTypes; index++) {

					usedTypeIds[index] = (long[]) typeIdsWithData.get(index);

					usedAltitude[index] = (float[]) altitudeWithData.get(index);
					usedDistance[index] = (float[]) distanceWithData.get(index);

					usedDuration[index] = (int[]) durationWithData.get(index);
					usedRecordingTime[index] = (int[]) recordingTimeWithData.get(index);
					usedDrivingTime[index] = (int[]) drivingTimeWithData.get(index);
					usedBreakTime[index] = (int[]) breakTimeWithData.get(index);

					usedNumTours[index] = (float[]) numToursWithData.get(index);
				}

				_tourDataYear.typeIds = usedTypeIds;
				_tourDataYear.usedTourTypeIds = usedTourTypeIds;

				_tourDataYear.altitudeLow = new float[numUsedTourTypes][numYears];
				_tourDataYear.altitudeHigh = usedAltitude;

				_tourDataYear.distanceLow = new float[numUsedTourTypes][numYears];
				_tourDataYear.distanceHigh = usedDistance;

				_tourDataYear.setDurationTimeLow(new int[numUsedTourTypes][numYears]);
				_tourDataYear.setDurationTimeHigh(usedDuration);

				_tourDataYear.recordingTime = usedRecordingTime;
				_tourDataYear.drivingTime = usedDrivingTime;
				_tourDataYear.breakTime = usedBreakTime;

				_tourDataYear.numToursLow = new float[numUsedTourTypes][numYears];
				_tourDataYear.numToursHigh = usedNumTours;
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _tourDataYear;
	}
}
