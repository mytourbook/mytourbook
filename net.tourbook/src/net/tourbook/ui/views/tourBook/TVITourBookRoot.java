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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.TourTypeSQL;
import net.tourbook.ui.UI;

public class TVITourBookRoot extends TourBookTreeViewerItem {

	TVITourBookRoot(final TourBookView view) {
		super(view);
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final TourTypeSQL sqlTourTypes = UI.sqlTourTypes();

		final String sqlString = "SELECT " + //		$NON-NLS-1$

				" StartYear, " //		1				$NON-NLS-1$
				+ SQL_SUM_COLUMNS //	2

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE) //$NON-NLS-1$ //$NON-NLS-2$

				+ " WHERE 1=1 " //					$NON-NLS-1$
				+ UI.sqlTourPersonId()
				+ sqlTourTypes.getWhereClause()

				+ " GROUP BY StartYear" //			$NON-NLS-1$
				+ " ORDER BY StartYear"; //			$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			sqlTourTypes.setSQLParameters(statement, 1);

			final ResultSet result = statement.executeQuery();

			while (result.next()) {
				final TVITourBookYear tourItem = new TVITourBookYear(fView, this, result.getInt(1));

				fCalendar.set(result.getShort(1), 0, 1);
				tourItem.fTourDate = fCalendar.getTimeInMillis();

				tourItem.addSumData(result.getLong(2),
						result.getLong(3),
						result.getLong(4),
						result.getLong(5),
						result.getLong(6),
						result.getLong(7),
						result.getFloat(8),
						result.getLong(9),
						result.getLong(10),
						result.getLong(11),
						result.getLong(12),
						result.getLong(13),
						result.getLong(14),
						result.getLong(15));

				children.add(tourItem);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	@Override
	protected void remove() {}
}
