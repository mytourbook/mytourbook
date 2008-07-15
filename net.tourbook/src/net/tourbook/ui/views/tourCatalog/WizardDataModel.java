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
/**
 * 
 */
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

/**
 * Data fDataModel for the tour list viewer
 */
public class WizardDataModel {

	private TourCatalogTourItem	rootItem;

	public void fetchChildren(final TourCatalogTourItem parentItem) {

		int childType = 0;

		final String sumColumns = "SUM(TOURDISTANCE), SUM(TOURALTUP), SUM(TOURRECORDINGTIME)"; //$NON-NLS-1$
		String sqlString = ""; //$NON-NLS-1$

		switch (parentItem.getItemType()) {

		case TourCatalogTourItem.ITEM_TYPE_ROOT:

			childType = TourCatalogTourItem.ITEM_TYPE_YEAR;

			sqlString = ("SELECT STARTYEAR, " + sumColumns + " \n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
					+ "GROUP BY STARTYEAR" //$NON-NLS-1$
					+ " ORDER BY STARTYEAR"; //$NON-NLS-1$

			break;

		case TourCatalogTourItem.ITEM_TYPE_YEAR:

			childType = TourCatalogTourItem.ITEM_TYPE_MONTH;

			sqlString = ("SELECT STARTMONTH, " + sumColumns + " \n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("WHERE STARTYEAR=" + parentItem.getDateValue()) //$NON-NLS-1$
					+ " GROUP BY STARTMONTH" //$NON-NLS-1$
					+ " ORDER BY STARTMONTH"; //$NON-NLS-1$
			break;

		case TourCatalogTourItem.ITEM_TYPE_MONTH:

			childType = TourCatalogTourItem.ITEM_TYPE_TOUR;

			sqlString = ("SELECT STARTDAY, TOURDISTANCE, TOURALTUP, TOURRECORDINGTIME, tourID \n") //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //$NON-NLS-1$ //$NON-NLS-2$
					+ ("WHERE STARTMONTH = " //$NON-NLS-1$
							+ parentItem.getDateValue()
							+ " AND STARTYEAR = " + parentItem.getParent().getDateValue()) //$NON-NLS-1$
					+ " ORDER BY STARTDAY"; //$NON-NLS-1$

			break;

		}

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			final int columnCount = result.getMetaData().getColumnCount();

			while (result.next()) {

				final long[] row = new long[columnCount];

				for (int col = 0; col < columnCount; col++) {
					row[col] = result.getLong(col + 1);
				}

				final TourCatalogTourItem childItem = new TourCatalogTourItem(childType, row);

				/*
				 * when a tour item does not have children, it's set to be a leaf
				 */
				if (childType == TourCatalogTourItem.ITEM_TYPE_TOUR) {
					childItem.setLeaf();
				}

				parentItem.addChild(childItem);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

	}

	public Object[] getTopLevelEntries() {

		return rootItem.getChildren();
	}

	public void setRootItem() {

		rootItem = new TourCatalogTourItem(TourCatalogTourItem.ITEM_TYPE_ROOT, new long[0]);

		fetchChildren(rootItem);
	}
}
