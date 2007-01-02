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
/**
 * 
 */
package net.tourbook.views.tourMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.tourbook.database.TourDatabase;

/**
 * Data fDataModel for the tour list viewer
 */
public class WizardDataModel {

	private TourMapTourItem	rootItem;

	public void setRootItem() {

		rootItem = new TourMapTourItem(TourMapTourItem.ITEM_TYPE_ROOT, new long[0]);

		fetchChildren(rootItem);
	}

	public Object[] getTopLevelEntries() {

		return rootItem.getChildren();
	}

	public void fetchChildren(TourMapTourItem parentItem) {

		int childType = 0;

		String sumColumns = "SUM(TOURDISTANCE), SUM(TOURALTUP), SUM(TOURRECORDINGTIME)";
		String sqlString = "";

		switch (parentItem.getItemType()) {

		case TourMapTourItem.ITEM_TYPE_ROOT:

			childType = TourMapTourItem.ITEM_TYPE_YEAR;

			sqlString = ("SELECT STARTYEAR, " + sumColumns + " \n")
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n")
					+ "GROUP BY STARTYEAR"
					+ " ORDER BY STARTYEAR";

			break;

		case TourMapTourItem.ITEM_TYPE_YEAR:

			childType = TourMapTourItem.ITEM_TYPE_MONTH;

			sqlString = ("SELECT STARTMONTH, " + sumColumns + " \n")
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n")
					+ ("WHERE STARTYEAR=" + parentItem.getDateValue())
					+ " GROUP BY STARTMONTH"
					+ " ORDER BY STARTMONTH";
			break;

		case TourMapTourItem.ITEM_TYPE_MONTH:

			childType = TourMapTourItem.ITEM_TYPE_TOUR;

			sqlString = ("SELECT STARTDAY, TOURDISTANCE, TOURALTUP, TOURRECORDINGTIME, tourID \n")
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n")
					+ ("WHERE STARTMONTH = "
							+ parentItem.getDateValue()
							+ " AND STARTYEAR = " + parentItem.getParent().getDateValue())
					+ " ORDER BY STARTDAY";

			break;

		}

		try {

			Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sqlString);
			ResultSet result = statement.executeQuery();

			int columnCount = result.getMetaData().getColumnCount();

			while (result.next()) {

				long[] row = new long[columnCount];

				for (int col = 0; col < columnCount; col++) {
					row[col] = result.getLong(col + 1);
				}

				TourMapTourItem childItem = new TourMapTourItem(childType, row);

				/*
				 * when a tour item does not have children, it's set to be a
				 * leaf
				 */
				if (childType == TourMapTourItem.ITEM_TYPE_TOUR) {
					childItem.setLeaf();
				}

				parentItem.addChild(childItem);
			}

			conn.close();

		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
