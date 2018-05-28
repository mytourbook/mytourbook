/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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

import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

public class TVITourBookRoot extends TVITourBookItem {

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

		final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);
		String fromTourData;

		if (sqlFilter.isTagFilterActive()) {

			// with tag filter

			fromTourData = NL

					+ "FROM (			" + NL //$NON-NLS-1$

					+ " SELECT			" + NL //$NON-NLS-1$

					+ "  StartYear,		" + NL //$NON-NLS-1$
					+ SQL_SUM_FIELDS + NL

					+ "  FROM " + TourDatabase.TABLE_TOUR_DATA + NL//$NON-NLS-1$

					// get tag id's
					+ "  LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //$NON-NLS-1$ //$NON-NLS-2$
					+ "  ON tourID = jTdataTtag.TourData_tourId	" + NL //$NON-NLS-1$

					+ "  WHERE 1=1		" + NL //$NON-NLS-1$
					+ sqlFilter.getWhereClause()

					+ ") td				" + NL//$NON-NLS-1$
			;

		} else {

			// without tag filter

			fromTourData = NL

					+ " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

					+ " WHERE 1=1			" + NL //$NON-NLS-1$
					+ sqlFilter.getWhereClause() + NL;
		}

		final String sql = NL +

				"SELECT						" + NL //$NON-NLS-1$

				+ " StartYear,				" + NL //$NON-NLS-1$
				+ SQL_SUM_COLUMNS

				+ fromTourData

				+ " GROUP BY StartYear		" + NL //$NON-NLS-1$
				+ " ORDER BY StartYear		" + NL//			//$NON-NLS-1$
		;

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			sqlFilter.setParameters(statement, 1);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);

				final TVITourBookYear yearItem = new TVITourBookYear(tourBookView, this);
				children.add(yearItem);

				yearItem.treeColumn = Integer.toString(dbYear);
				yearItem.tourYear = dbYear;

				yearItem.colTourDateTime = new TourDateTime(calendar8.withYear(dbYear));

				yearItem.addSumColumns(result, 2);
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			Util.closeSql(conn);
		}
	}

}
