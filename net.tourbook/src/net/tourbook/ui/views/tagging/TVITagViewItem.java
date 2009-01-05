/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;

public abstract class TVITagViewItem extends TreeViewerItem {

	static final String	SQL_SUM_COLUMNS			= ""// //$NON-NLS-1$
														+ "SUM(tourDistance)," // 		0	//$NON-NLS-1$
														+ "SUM(tourRecordingTime)," //	1	//$NON-NLS-1$
														+ "SUM(tourDrivingTime)," //	2	//$NON-NLS-1$
														+ "SUM(tourAltUp)," //			3	//$NON-NLS-1$
														+ "SUM(tourAltDown)," //		4	//$NON-NLS-1$
														+ "MAX(maxPulse)," //			5	//$NON-NLS-1$
														+ "MAX(maxAltitude)," //		6	//$NON-NLS-1$
														+ "MAX(maxSpeed)," //			7	//$NON-NLS-1$
														+ "AVG(avgPulse)," //			8	//$NON-NLS-1$
														+ "AVG(avgCadence)," //			9	//$NON-NLS-1$
														+ "AVG(avgTemperature)," //		10	//$NON-NLS-1$

														+ "SUM(1)";		//				11	//$NON-NLS-1$

	static final String	SQL_SUM_COLUMNS_TOUR	= ""// //$NON-NLS-1$
														+ "tourDistance," // 			0	//$NON-NLS-1$
														+ "tourRecordingTime," //		1	//$NON-NLS-1$
														+ "tourDrivingTime," //			2	//$NON-NLS-1$
														+ "tourAltUp," //				3	//$NON-NLS-1$
														+ "tourAltDown," //				4	//$NON-NLS-1$
														+ "maxPulse," //				5	//$NON-NLS-1$
														+ "maxAltitude," //				6	//$NON-NLS-1$
														+ "maxSpeed," //				7	//$NON-NLS-1$
														+ "avgPulse," //				8	//$NON-NLS-1$
														+ "avgCadence," //				9	//$NON-NLS-1$
														+ "avgTemperature"; //			10	//$NON-NLS-1$

	/**
	 * content which is displayed in the tree column
	 */
	String				treeColumn;

	long				colDistance;

	long				colRecordingTime;
	long				colDrivingTime;
	long				colPausedTime;

	long				colAltitudeUp;
	long				colAltitudeDown;

	float				colMaxSpeed;
	long				colMaxPulse;
	long				colMaxAltitude;

	float				colAvgSpeed;
	float				colAvgPace;

	long				colAvgPulse;
	long				colAvgCadence;
	long				colAvgTemperature;

	long				colItemCounter;

	/**
	 * Read sum totals from the database for the tagItem
	 * 
	 * @param tagItem
	 */
	public static void readTagTotals(final TVITagViewTag tagItem) {

		try {
			final StringBuilder sb = new StringBuilder();
			final SQLFilter sqlFilter = new SQLFilter();

			/*
			 * get tags
			 */
			sb.append("SELECT "); //$NON-NLS-1$
			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jtblTagData"); //$NON-NLS-1$ //$NON-NLS-2$

			// get data for a tour
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData ON "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" jtblTagData.TourData_tourId = TourData.tourId"); //$NON-NLS-1$

			sb.append(" WHERE jtblTagData.TourTag_TagId = ?"); //$NON-NLS-1$
			sb.append(sqlFilter.getWhereClause());

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagItem.getTagId());
			sqlFilter.setParameters(statement, 2);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {
				tagItem.readSumColumnData(result, 1);
			}

			conn.close();

			if (tagItem.colItemCounter == 0) {

				/*
				 * to hide the '+' for an item which has no children, an empty list of children will
				 * be created
				 */
				tagItem.setChildren(new ArrayList<TreeViewerItem>());
			}

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	void readDefaultColumnData(final ResultSet result, final int startIndex) throws SQLException {

		colDistance = result.getLong(startIndex + 0);

		colRecordingTime = result.getLong(startIndex + 1);
		colDrivingTime = result.getLong(startIndex + 2);
		colPausedTime = colRecordingTime - colDrivingTime;

		colAltitudeUp = result.getLong(startIndex + 3);
		colAltitudeDown = result.getLong(startIndex + 4);

		colMaxPulse = result.getLong(startIndex + 5);
		colMaxAltitude = result.getLong(startIndex + 6);
		colMaxSpeed = result.getFloat(startIndex + 7);

		colAvgPulse = result.getLong(startIndex + 8);
		colAvgCadence = result.getLong(startIndex + 9);
		colAvgTemperature = result.getLong(startIndex + 10);

		// prevent divide by 0
		// 3.6 * SUM(TOURDISTANCE) / SUM(TOURDRIVINGTIME)	
		colAvgSpeed = (colDrivingTime == 0 ? 0 : 3.6f * colDistance / colDrivingTime);
		colAvgPace = colDistance == 0 ? 0 : colDrivingTime * 166.66f / colDistance;

	}

	public void readSumColumnData(final ResultSet result, final int startIndex) throws SQLException {

		readDefaultColumnData(result, startIndex);

		colItemCounter = result.getLong(startIndex + 11);
	}

}
