/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

public class TVITourBookYear extends TourBookTreeViewerItem {

	public TVITourBookYear(TourBookView view, TourBookTreeViewerItem parentItem, int year) {

		super(view);

		setParentItem(parentItem);
		fTourYear = fFirstColumn = year;
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are month items
		 */
		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		String sqlString = "SELECT " //$NON-NLS-1$
				+ "STARTYear, " //$NON-NLS-1$
				+ "STARTMonth, " //$NON-NLS-1$
				+ SQL_SUM_COLUMNS
				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
				+ ("WHERE STARTYEAR=" + fTourYear) //$NON-NLS-1$
				+ sqlTourPersonId()
				+ sqlTourTypeId()
				+ " GROUP BY StartYear, STARTMONTH" //$NON-NLS-1$
				+ " ORDER BY STARTMONTH"; //$NON-NLS-1$

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			while (result.next()) {
				TourBookTreeViewerItem tourItem = new TVITourBookMonth(fView,
						this,
						result.getInt(1),
						result.getInt(2));

				fCalendar.set(result.getShort(1), result.getShort(2) - 1, 1);
				tourItem.fTourDate = fCalendar.getTimeInMillis();

				tourItem.addSumData(result.getLong(3),
						result.getLong(4),
						result.getLong(5),
						result.getLong(6),
						result.getLong(7),
						result.getLong(8),
						result.getFloat(9),
						result.getLong(10),
						result.getLong(11),
						result.getLong(12),
						result.getLong(13),
						result.getLong(14),
						result.getLong(15),
						result.getLong(16));

				children.add(tourItem);
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void remove() {}

}
