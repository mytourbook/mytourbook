/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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

		String sqlString = "SELECT " //$NON-NLS-1$
				+ "STARTYear, " //1			//$NON-NLS-1$
				+ "STARTMonth, " //2		//$NON-NLS-1$
				+ "STARTDay, " //3			//$NON-NLS-1$
				+ "TOURDISTANCE, " //4		//$NON-NLS-1$
				+ "TOURRECORDINGTIME, " //5	//$NON-NLS-1$
				+ "TOURDRIVINGTIME, " //6	//$NON-NLS-1$
				+ "TOURALTUP, " //7			//$NON-NLS-1$
				+ "TOURALTDOWN, " //8		//$NON-NLS-1$
				+ "startDistance, " //9		//$NON-NLS-1$
				+ "tourID," //10			//$NON-NLS-1$
				+ "tourType_typeId," //11	//$NON-NLS-1$
				+ "tourTitle," //12			//$NON-NLS-1$
				+ "deviceTimeInterval" //13	//$NON-NLS-1$
				+ "\n" //$NON-NLS-1$
				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ (" WHERE STARTYEAR = " + yearItem.fTourYear) //$NON-NLS-1$
				+ (" AND STARTMONTH = " + fTourMonth) //$NON-NLS-1$
				+ sqlTourPersonId()
				+ sqlTourTypeId()
				+ " ORDER BY STARTDAY, StartHour, StartMinute"; //$NON-NLS-1$

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

				tourItem.fTourTitle = result.getString(12);
				tourItem.fColumnTimeInterval = result.getShort(13);

				children.add(tourItem);
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void remove() {}

}
