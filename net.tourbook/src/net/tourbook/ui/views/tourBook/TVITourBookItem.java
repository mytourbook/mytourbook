/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourBook;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.tour.ITourItem;
import net.tourbook.ui.TreeViewerItem;
import net.tourbook.ui.UI;

public abstract class TVITourBookItem extends TreeViewerItem implements ITourItem {

	static final Calendar	calendar		= GregorianCalendar.getInstance();

	static final String		SQL_SUM_COLUMNS	= UI.EMPTY_STRING
											//
													+ "SUM(TOURDISTANCE)," // 		1	//$NON-NLS-1$
													+ "SUM(TOURRECORDINGTIME)," //	2	//$NON-NLS-1$
													+ "SUM(TOURDRIVINGTIME)," //	3	//$NON-NLS-1$
													+ "SUM(TOURALTUP)," //			4	//$NON-NLS-1$
													+ "SUM(TOURALTDOWN)," //		5	//$NON-NLS-1$
													+ "SUM(1)," //					6	//$NON-NLS-1$
													//
													+ "MAX(MAXSPEED)," //			7	//$NON-NLS-1$
													+ "SUM(TOURDISTANCE)," //		8	//$NON-NLS-1$
													+ "SUM(TOURDRIVINGTIME)," //	9	//$NON-NLS-1$
													+ "MAX(MAXALTITUDE)," //		10	//$NON-NLS-1$
													+ "MAX(MAXPULSE)," //			11	//$NON-NLS-1$
													//
													+ "AVG(AVGPULSE)," //			12	//$NON-NLS-1$
													+ "AVG(AVGCADENCE)," //			13	//$NON-NLS-1$
													+ "AVG(DOUBLE(AvgTemperature) / TemperatureScale)," //		14	//$NON-NLS-1$

													+ "AVG(WEATHERWINDDIR)," //		15	//$NON-NLS-1$
													+ "AVG(WEATHERWINDSPD)," //		16	//$NON-NLS-1$
													+ "AVG(RESTPULSE)," //			17	//$NON-NLS-1$

													+ "SUM(CALORIES)";			//	18	//$NON-NLS-1$

	TourBookView			tourBookView;

	String					treeColumn;

	int						tourYear;

	/**
	 * month starts with 1 for january
	 */
	int						tourMonth;
	int						tourDay;

	long					colTourDate;
	String					colTourTitle;
	long					colPersonId;										// tourPerson_personId

	long					colCounter;
	long					colCalories;
	long					colDistance;

	long					colRecordingTime;
	long					colDrivingTime;
	long					colPausedTime;

	long					colAltitudeUp;
	long					colAltitudeDown;

	float					colMaxSpeed;
	long					colMaxAltitude;
	long					colMaxPulse;

	float					colAvgSpeed;
	float					colAvgPace;
	long					colAvgPulse;
	long					colAvgCadence;
	float					colAvgTemperature;

	int						colWindSpd;
	int						colWindDir;
	String					colClouds;
	int						colRestPulse;

	int						colWeekNo;
	int						colWeekDay;
	int						colWeekYear;

	TVITourBookItem(final TourBookView view) {
		tourBookView = view;
	}

	public void addSumColumns(final ResultSet result, final int startIndex) throws SQLException {

		colDistance = result.getLong(startIndex + 0);

		colRecordingTime = result.getLong(startIndex + 1);
		colDrivingTime = result.getLong(startIndex + 2);

		colAltitudeUp = result.getLong(startIndex + 3);
		colAltitudeDown = result.getLong(startIndex + 4);

		colCounter = result.getLong(startIndex + 5);

		colMaxSpeed = result.getFloat(startIndex + 6);

		// compute average speed/pace, prevent divide by 0
		final long dbDistance = result.getLong(startIndex + 7);
		final long dbDrivingTime = result.getLong(startIndex + 8);

		colAvgSpeed = dbDrivingTime == 0 ? 0 : 3.6f * dbDistance / dbDrivingTime;
		colAvgPace = dbDistance == 0 ? 0 : dbDrivingTime * 1000f / dbDistance;

		colMaxAltitude = result.getLong(startIndex + 9);
		colMaxPulse = result.getLong(startIndex + 10);

		colAvgPulse = result.getLong(startIndex + 11);
		colAvgCadence = result.getLong(startIndex + 12);
		colAvgTemperature = result.getFloat(startIndex + 13);

		colWindDir = result.getInt(startIndex + 14);
		colWindSpd = result.getInt(startIndex + 15);
		colRestPulse = result.getInt(startIndex + 16);

		colCalories = result.getLong(startIndex + 17);

		colPausedTime = colRecordingTime - colDrivingTime;
	}

	public Long getTourId() {
		return null;
	}

}
