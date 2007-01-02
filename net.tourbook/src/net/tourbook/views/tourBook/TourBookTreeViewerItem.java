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
package net.tourbook.views.tourBook;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.tourbook.data.TourType;
import net.tourbook.tour.ITourItem;
import net.tourbook.tour.TreeViewerItem;

public abstract class TourBookTreeViewerItem extends TreeViewerItem implements ITourItem {

	static final String	SQL_SUM_COLUMNS	= "SUM(TOURDISTANCE), "
												+ "SUM(TOURRECORDINGTIME), "
												+ "SUM(TOURDRIVINGTIME), "
												+ "SUM(TOURALTUP), "
												+ "SUM(TOURALTDOWN),"
												+ "SUM(1)";

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

	long				fColumnDistance;
	long				fColumnRecordingTime;
	long				fColumnDrivingTime;
	long				fColumnAltitudeUp;
	long				fColumnAltitudeDown;
	long				fColumnCounter;

	TourBookTreeViewerItem(TourBookView view) {
		fView = view;
	}

	public void addSumData(	long long1,
							long long2,
							long long3,
							long long4,
							long long5,
							long long6) {

		fColumnDistance = long1;

		fColumnRecordingTime = long2;
		fColumnDrivingTime = long3;

		fColumnAltitudeUp = long4;
		fColumnAltitudeDown = long5;

		fColumnCounter = long6;
	}

	public Long getTourId() {
		return null;
	}

	/**
	 * @return Returns a sql statement string to select only the data which tour
	 *         type is defined in fTourTypeId
	 */
	String sqlTourPersonId() {

		StringBuffer sqlString = new StringBuffer();

		long personId = fView.fActivePerson == null ? -1 : fView.fActivePerson
				.getPersonId();

		if (personId == -1) {
			// select all people
		} else {
			// select only one person
			sqlString.append(" AND tourPerson_personId = " + Long.toString(personId));
		}
		return sqlString.toString();
	}
	/**
	 * @return Returns a sql statement string to select only the data which tour
	 *         type is defined in fTourTypeId
	 */
	String sqlTourTypeId() {

		StringBuffer sqlString = new StringBuffer();
		long tourTypeId = fView.fActiveTourTypeId;

		if (tourTypeId == TourType.TOUR_TYPE_ID_ALL) {
			// select all tour types
		} else {
			// select only one tour type
			sqlString.append(" AND tourType_typeId "
					+ (tourTypeId == TourType.TOUR_TYPE_ID_NOT_DEFINED
							? "is null"
							: ("=" + Long.toString(tourTypeId))));
		}
		return sqlString.toString();
	}

}
