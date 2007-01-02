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

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

public class TVITourBookYear extends TourBookTreeViewerItem {

	public TVITourBookYear(TourBookView view, TourBookTreeViewerItem parentItem, int year) {

		super(view);

		setParentItem(parentItem);
		fTourYear = fFirstColumn = year;
	}

	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are month items
		 */
		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		String sqlString = "SELECT "
				+ "STARTYear, "
				+ "STARTMonth, "
				+ SQL_SUM_COLUMNS
				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n")
				+ ("WHERE STARTYEAR=" + fTourYear)
				+ sqlTourPersonId()
				+ sqlTourTypeId()
				+ " GROUP BY StartYear, STARTMONTH"
				+ " ORDER BY STARTMONTH";

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				TourBookTreeViewerItem tourItem = new TVITourBookMonth(
						fView,
						this,
						result.getInt(1),
						result.getInt(2));

				fCalendar.set(result.getShort(1), result.getShort(2) - 1, 1);
				tourItem.fTourDate = fCalendar.getTimeInMillis();

				tourItem.addSumData(result.getLong(3), result.getLong(4), result
						.getLong(5), result.getLong(6), result.getLong(7), result
						.getLong(8));

				children.add(tourItem);
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	protected void remove() {}

}
