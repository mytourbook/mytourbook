/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.tour.ITourItem;
import net.tourbook.tour.TreeViewerItem;

public abstract class TourBookTreeViewerItem extends TreeViewerItem implements ITourItem {

	static final String	SQL_SUM_COLUMNS	= "" //
												+ "SUM(TOURDISTANCE), " // 		1	//$NON-NLS-1$
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

	TourBookView		fView;

	int					fFirstColumn;

	protected Calendar	fCalendar		= new GregorianCalendar();

	int					fTourYear;
	/**
	 * month starts with 1 for january
	 */
	int					fTourMonth;
	int					fTourDay;

	long				fTourDate;
	String				fTourTitle;

	long				fColumnDistance;
	long				fColumnRecordingTime;
	long				fColumnDrivingTime;
	long				fColumnAltitudeUp;
	long				fColumnAltitudeDown;
	long				fColumnCounter;
	float				fColumnMaxSpeed;
	float				fColumnAvgSpeed;
	long				fColumnMaxAltitude;
	long				fColumnMaxPulse;
	long				fColumnAvgPulse;
	long				fColumnAvgCadence;
	long				fColumnAvgTemperature;

	TourBookTreeViewerItem(final TourBookView view) {
		fView = view;
	}

	public void addSumData(	final long long1,
							final long long2,
							final long long3,
							final long long4,
							final long long5,
							final long long6,
							final float float7,
							final long long8,
							final long long9,
							final long long10,
							final long long11,
							final long long12,
							final long long13,
							final long long14) {

		fColumnDistance = long1;

		fColumnRecordingTime = long2;
		fColumnDrivingTime = long3;

		fColumnAltitudeUp = long4;
		fColumnAltitudeDown = long5;

		fColumnCounter = long6;

		fColumnMaxSpeed = float7;

		// prevent divide by 0
		// 3.6 * SUM(TOURDISTANCE) / SUM(TOURDRIVINGTIME)	
		fColumnAvgSpeed = (float) (long9 == 0 ? 0 : 3.6 * long8 / long9);

		fColumnMaxAltitude = long10;
		fColumnMaxPulse = long11;

		fColumnAvgPulse = long12;
		fColumnAvgCadence = long13;
		fColumnAvgTemperature = long14;
	}

	public Long getTourId() {
		return null;
	}

//	/**
//	 * @return Returns a sql statement string to select only the data which for the selected person
//	 */
//	String sqlTourPersonId() {
//
//		final StringBuffer sqlString = new StringBuffer();
//
//		final long personId = fView.fActivePerson == null ? -1 : fView.fActivePerson.getPersonId();
//
//		if (personId == -1) {
//			// select all people
//		} else {
//			// select only one person
//			sqlString.append(" AND tourPerson_personId = " + Long.toString(personId)); //$NON-NLS-1$
//		}
//		return sqlString.toString();
//	}
//
//	/**
//	 * @return Returns a sql statement string to select only the data which tour type is defined in
//	 * 	fTourTypeId
//	 */
//	String sqlTourTypeId() {
//
//		return fView.fActiveTourTypeFilter.getSQLString();
//
//	}

}
