/*******************************************************************************
 * Copyright (C) 2001, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourTag;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.tourbook.tour.TreeViewerItem;

public abstract class TVITagViewItem extends TreeViewerItem {

	static final String	SQL_SUM_COLUMNS	= "SUM(TOURDISTANCE), " // 				1	//$NON-NLS-1$
												+ "SUM(TOURRECORDINGTIME), " //	2	//$NON-NLS-1$
												+ "SUM(TOURDRIVINGTIME), " //	3	//$NON-NLS-1$
												+ "SUM(TOURALTUP), " //			4	//$NON-NLS-1$
												+ "SUM(TOURALTDOWN)," //		5	//$NON-NLS-1$
												+ "SUM(1)," //					6	//$NON-NLS-1$
												+ "MAX(MAXSPEED)," //			7	//$NON-NLS-1$
												+ "SUM(TOURDISTANCE)," //		8	//$NON-NLS-1$
												+ "SUM(TOURDRIVINGTIME)," //	9	//$NON-NLS-1$
												+ "MAX(MAXALTITUDE)," //		10	//$NON-NLS-1$
												+ "MAX(MAXPULSE)," //			11	//$NON-NLS-1$
												+ "AVG(AVGPULSE)," //			12	//$NON-NLS-1$
												+ "AVG(AVGCADENCE)," //			13	//$NON-NLS-1$
												+ "AVG(AVGTEMPERATURE)";	//	14	//$NON-NLS-1$

	/**
	 * content which is displayed in the tree column
	 */
	String				treeColumn;

	long				colDistance;

	long				colRecordingTime;
	long				colDrivingTime;

	long				colAltitudeUp;
	long				colAltitudeDown;

	long				colCounter;

	float				colMaxSpeed;
	float				colAvgSpeed;

	long				colMaxAltitude;
	long				colMaxPulse;

	long				colAvgPulse;
	long				colAvgCadence;
	long				colAvgTemperature;

	public void addSumData(final ResultSet result, final int startIndex) throws SQLException {

		colDistance = result.getLong(startIndex + 1);

		colRecordingTime = result.getLong(startIndex + 2);
		colDrivingTime = result.getLong(startIndex + 3);

		colAltitudeUp = result.getLong(startIndex + 4);
		colAltitudeDown = result.getLong(startIndex + 5);

		colCounter = result.getLong(startIndex + 6);

		colMaxSpeed = result.getFloat(startIndex + 7);

		// prevent divide by 0
		// 3.6 * SUM(TOURDISTANCE) / SUM(TOURDRIVINGTIME)	
		final float dbSpeed = result.getFloat(startIndex + 9);
		colAvgSpeed = (float) (dbSpeed == 0 ? 0 : 3.6 * result.getLong(startIndex + 8) / dbSpeed);

		colMaxAltitude = result.getLong(startIndex + 10);
		colMaxPulse = result.getLong(startIndex + 11);

		colAvgPulse = result.getLong(startIndex + 12);
		colAvgCadence = result.getLong(startIndex + 13);
		colAvgTemperature = result.getLong(startIndex + 14);
	}

}
