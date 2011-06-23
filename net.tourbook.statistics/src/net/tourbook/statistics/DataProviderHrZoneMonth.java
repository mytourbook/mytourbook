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

import net.tourbook.data.TourPerson;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProviderHrZoneMonth extends DataProvider {

	private static DataProviderHrZoneMonth	_instance;

	private TourDataMonthHrZones			_monthData;

	private DataProviderHrZoneMonth() {}

	public static DataProviderHrZoneMonth getInstance() {
		if (_instance == null) {
			_instance = new DataProviderHrZoneMonth();
		}
		return _instance;
	}

	TourDataMonthHrZones getMonthData(	final TourPerson person,
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
			return _monthData;
		}

		_activePerson = person;
		_activeTourTypeFilter = tourTypeFilter;
		_lastYear = lastYear;
		_numberOfYears = numberOfYears;

		_monthData = new TourDataMonthHrZones();

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = "SELECT " // //$NON-NLS-1$

				+ "startYear," // 											1 //$NON-NLS-1$
				+ "startMonth," // 											2 //$NON-NLS-1$

				+ "SUM(CASE WHEN hrZone0 > 0 THEN hrZone0 ELSE 0 END), \n" //  3 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone1 > 0 THEN hrZone1 ELSE 0 END), \n" //  4 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone2 > 0 THEN hrZone2 ELSE 0 END), \n" //  5 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone3 > 0 THEN hrZone3 ELSE 0 END), \n" //  6 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone4 > 0 THEN hrZone4 ELSE 0 END), \n" //  7 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone5 > 0 THEN hrZone5 ELSE 0 END), \n" //  8 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone6 > 0 THEN hrZone6 ELSE 0 END), \n" //  9 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone7 > 0 THEN hrZone7 ELSE 0 END), \n" //  10 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone8 > 0 THEN hrZone8 ELSE 0 END), \n" //  11 //$NON-NLS-1$
				+ "SUM(CASE WHEN hrZone9 > 0 THEN hrZone9 ELSE 0 END)  \n" //  12 //$NON-NLS-1$

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$

				+ (" WHERE startYear IN (" + getYearList(lastYear, numberOfYears) + ") \n") //$NON-NLS-1$ //$NON-NLS-2$
//				+ (" AND NumberOfHrZones > 0  \n") //$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ (" GROUP BY startYear, startMonth \n") //$NON-NLS-1$

				+ (" ORDER BY startYear, startMonth \n"); //$NON-NLS-1$

		try {

			final int maxZones = 10; // hr zones: 0...9
			final int serieLength = maxZones;
			final int valueLength = 12 * numberOfYears;

			final int[][] dbHrZones = new int[serieLength][valueLength];

			final Connection conn = TourDatabase.getInstance().getConnection();
			{
				final PreparedStatement statement = conn.prepareStatement(sqlString);
				sqlFilter.setParameters(statement, 1);

				final ResultSet result = statement.executeQuery();
				while (result.next()) {

					final int dbYear = result.getInt(1);
					final int dbMonth = result.getInt(2);

					final int yearIndex = numberOfYears - (lastYear - dbYear + 1);
					final int monthIndex = (dbMonth - 1) + yearIndex * 12;

					dbHrZones[0][monthIndex] = result.getInt(3);
					dbHrZones[1][monthIndex] = result.getInt(4);
					dbHrZones[2][monthIndex] = result.getInt(5);
					dbHrZones[3][monthIndex] = result.getInt(6);
					dbHrZones[4][monthIndex] = result.getInt(7);
					dbHrZones[5][monthIndex] = result.getInt(8);
					dbHrZones[6][monthIndex] = result.getInt(9);
					dbHrZones[7][monthIndex] = result.getInt(10);
					dbHrZones[8][monthIndex] = result.getInt(11);
					dbHrZones[9][monthIndex] = result.getInt(12);
				}
			}
			conn.close();

			_monthData.hrZones = dbHrZones;

//			for (int zoneIndex = 0; zoneIndex < 10; zoneIndex++) {
//				for (int monthIndex = 0; monthIndex < 12; monthIndex++) {
//					final int hrZone = dbHrZones[zoneIndex][monthIndex];
//					if (hrZone > 0) {
//						System.out.println("z:" + zoneIndex + "\tm:" + monthIndex + "\thr:" + hrZone);
//						// TODO remove SYSTEM.OUT.PRINTLN
//					}
//				}
//				System.out.println("\t");
//				// TODO remove SYSTEM.OUT.PRINTLN
//			}
//
//			System.out.println("\t");
//			System.out.println("\t");
//// TODO remove SYSTEM.OUT.PRINTLN

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return _monthData;
	}

}
