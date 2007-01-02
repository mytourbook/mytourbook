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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

public class TVITourBookMonth extends TourBookTreeViewerItem {

	public TVITourBookMonth(TourBookView view, TourBookTreeViewerItem parentItem, int year,
			int month) {

		super(view);

		setParentItem(parentItem);

		fTourYear = year;
		fTourMonth = fFirstColumn = month;
	}

	protected void fetchChildren() {

		/*
		 * set the children for the month item, these are tour items
		 */
		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		TVITourBookYear yearItem = (TVITourBookYear) (getParentItem());

		String sqlString = "SELECT "
				+ "STARTYear, "
				+ "STARTMonth, "
				+ "STARTDay, "
				+ "TOURDISTANCE, "
				+ "TOURRECORDINGTIME, "
				+ "TOURDRIVINGTIME, "
				+ "TOURALTUP, "
				+ "TOURALTDOWN, "
				+ "startDistance, "
				+ "tourID,"
				+ "tourType_typeId"
				+ "\n"
				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n")
				+ (" WHERE STARTYEAR = " + yearItem.fTourYear)
				+ (" AND STARTMONTH = " + fTourMonth)
				+ sqlTourPersonId()
				+ sqlTourTypeId()
				+ " ORDER BY STARTDAY, StartHour, StartMinute";

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {

				TVITourBookTour tourItem = new TVITourBookTour(fView, this);

				tourItem.fTourYear = result.getInt(1);
				tourItem.fTourMonth = result.getInt(2);
				tourItem.fTourDay = tourItem.fFirstColumn = result.getInt(3);

				fCalendar.set(tourItem.fTourYear, tourItem.fTourMonth - 1, tourItem.fTourDay);
				tourItem.fTourDate = fCalendar.getTimeInMillis();

				tourItem.fColumnDistance = result.getLong(4);
				tourItem.fColumnRecordingTime = result.getLong(5);
				tourItem.fColumnDrivingTime = result.getLong(6);
				tourItem.fColumnAltitudeUp = result.getLong(7);
				tourItem.fColumnAltitudeDown = result.getLong(8);

				tourItem.fColumnStartDistance = result.getLong(9);
				tourItem.fTourId = result.getLong(10);
				Object tourTypeId = result.getObject(11);
				tourItem.fTourTypeId = (tourTypeId == null
						? TourType.TOUR_TYPE_ID_NOT_DEFINED
						: (Long) tourTypeId);

				children.add(tourItem);
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	protected void remove() {}

}
