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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.joda.time.DateTime;

public class TVITourBookRoot extends TVITourBookItem {

	TVITourBookRoot(final TourBookView view) {
		super(view);
	}

	@Override
	protected void fetchChildren() {

		if (tourBookView.getViewType() == ViewType.COLLATE_BY_TOUR_TYPE) {
			fetchChildren_ByTourType();
		} else {
			fetchChildren_ByYear();
		}
	}

	private void fetchChildren_ByTourType() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final String sql = "" //

				+ "SELECT" //						 //$NON-NLS-1$
				+ " tourStartTime," //			1
				+ " tourTitle" //				2

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA + "\n" //$NON-NLS-1$

				+ (" WHERE tourtype_typeid =20") //

				+ " ORDER BY tourStartTime";//			//$NON-NLS-1$

//		select tourType_typeId, tourType.name, tourTitle, tourDescription, tourStartTime
//		from "TOURDATA" inner join "USER"."TOURTYPE" on tourtype_typeid=typeid
//		where tourtype_typeid =19
//		order by tourStartTime

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);

//			final long time = System.currentTimeMillis();
//			System.out.println(System.currentTimeMillis() - time + "ms");

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long dbTourStartTime = result.getLong(1);
				final String dbTourTitle = result.getString(2);

				final TVITourBookTourType typeItem = new TVITourBookTourType(tourBookView, this);
				children.add(typeItem);

				final DateTime eventStart = new DateTime(dbTourStartTime);
				final String eventStartText = UI.DateFormatterShort.format(dbTourStartTime);

				typeItem.treeColumn = dbTourTitle == null ? UI.EMPTY_STRING : dbTourTitle;
				typeItem.eventStart = eventStart;
				typeItem.eventStartText = eventStartText;
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			try {
				conn.close();
			} catch (final SQLException e) {
				StatusUtil.log(e);
			}
		}
	}

	private void fetchChildren_ByYear() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final SQLFilter sqlFilter = new SQLFilter();

		final String sql = ""
		//
				+ "SELECT" //						 //$NON-NLS-1$
				+ " startYear," //
				+ SQL_SUM_COLUMNS

				+ " FROM " + TourDatabase.TABLE_TOUR_DATA + UI.NEW_LINE //$NON-NLS-1$

				+ (" WHERE 1=1" + sqlFilter.getWhereClause()) //

				+ " GROUP BY startYear" //			//$NON-NLS-1$
				+ " ORDER BY startYear";//			//$NON-NLS-1$

		Connection conn = null;

		try {

			conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			sqlFilter.setParameters(statement, 1);

//			final long time = System.currentTimeMillis();
//			System.out.println(System.currentTimeMillis() - time + "ms");

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);

				final TVITourBookYear yearItem = new TVITourBookYear(tourBookView, this);
				children.add(yearItem);

				yearItem.treeColumn = Integer.toString(dbYear);
				yearItem.tourYear = dbYear;

				calendar.set(dbYear, 0, 1);
				yearItem.colTourDate = calendar.getTimeInMillis();

				yearItem.addSumColumns(result, 2);
			}

		} catch (final SQLException e) {
			SQL.showException(e, sql);
		} finally {
			try {
				conn.close();
			} catch (final SQLException e) {
				StatusUtil.log(e);
			}
		}
	}
}
