/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.ui.SQLFilter;

import org.joda.time.DateTime;

public class TVITourBookTourType extends TVITourBookItem {

	DateTime	eventStart;
	DateTime	eventEnd;
	String		eventStartText;

	public TVITourBookTourType(final TourBookView view, final TVITourBookItem parentItem) {

		super(view);

		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final SQLFilter sqlFilter = new SQLFilter();

//		final StringBuilder sb = new StringBuilder();
//
//
//		sb.append("SELECT "); //$NON-NLS-1$
//
//		sb.append(sumYear + ", "); //$NON-NLS-1$
//		sb.append(sumYearSub + ","); //$NON-NLS-1$
//		sb.append(SQL_SUM_COLUMNS);
//
//		sb.append(" FROM " + TourDatabase.TABLE_TOUR_DATA); //$NON-NLS-1$
//
//		sb.append(" WHERE " + sumYear + "=?"); //$NON-NLS-1$ //$NON-NLS-2$
//		sb.append(sqlFilter.getWhereClause());
//
//		sb.append(" GROUP BY " + sumYear + ", " + sumYearSub); //$NON-NLS-1$ //$NON-NLS-2$
//		sb.append(" ORDER BY " + sumYearSub); //$NON-NLS-1$
//
//		try {
//
//			final Connection conn = TourDatabase.getInstance().getConnection();
//
//			final PreparedStatement statement = conn.prepareStatement(sb.toString());
//			statement.setInt(1, tourYear);
//			sqlFilter.setParameters(statement, 2);
//
//			final ResultSet result = statement.executeQuery();
//			while (result.next()) {
//
//				final TVITourBookItem tourItem = new TVITourBookYearSub(tourBookView, this, _subCategory);
//				;
//				children.add(tourItem);
//
//				final int dbYear = result.getInt(1);
//				final int dbYearSub = result.getInt(2);
//
////				final DateTime tourDate = new DateTime(dbYear, dbMonth, 1, 0, 0, 0, 0);
//				tourItem.treeColumn = formatItemString(dbYear, dbYearSub);
//
//				tourItem.tourYear = dbYear;
//				tourItem.tourYearSub = dbYearSub;
////				tourItem.colTourDate = tourDate;
//				tourItem.colTourDate = calendar.getTimeInMillis();
//
//				tourItem.addSumColumns(result, 3);
//			}
//
//			conn.close();
//
//		} catch (final SQLException e) {
//			UI.showSQLException(e);
//		}
	}

}
