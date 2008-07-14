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
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITourBookYear extends TVITourBookItem {

	public TVITourBookYear(final TourBookView view, final TVITourBookItem parentItem) {

		super(view);

		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are month items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final SQLFilter sqlFilter = new SQLFilter();

		final String sqlString = ""//
				+ "SELECT" //$NON-NLS-1$
				+ " startYear, " //$NON-NLS-1$
				+ " startMonth, " //$NON-NLS-1$

				+ SQL_SUM_COLUMNS

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$

				+ (" WHERE startYear=?") //$NON-NLS-1$
				+ sqlFilter.getWhereClause()

				+ " GROUP BY StartYear, StartMonth" //$NON-NLS-1$
				+ " ORDER BY StartMonth"; //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			statement.setInt(1, fTourYear);
			sqlFilter.setParameters(statement, 2);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVITourBookItem tourItem = new TVITourBookMonth(fView, this);
				children.add(tourItem);

				final int dbYear = result.getInt(1);
				final int dbMonth = result.getInt(2);
				fCalendar.set(dbYear, dbMonth - 1, 1);

				tourItem.treeColumn = fMonthFormatter.format(fCalendar.getTime());

				tourItem.fTourYear = dbYear;
				tourItem.fTourMonth = dbMonth;
				tourItem.fTourDate = fCalendar.getTimeInMillis();

				tourItem.addSumColumns(result, 3);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	protected void remove() {}

}
