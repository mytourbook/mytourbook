/*******************************************************************************
 * Copyright (C) 2005, 2016 Wolfgang Schramm and Contributors
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
import java.time.ZonedDateTime;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

public class TVITourBookYear extends TVITourBookItem {

	private YearSubCategory	_subCategory;

	public TVITourBookYear(final TourBookView view, final TVITourBookItem parentItem) {

		super(view);

		_subCategory = view.getYearSub();

		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		final boolean isWeekDisplayed = _subCategory == YearSubCategory.WEEK;

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final SQLFilter sqlFilter = new SQLFilter();

		String sumYear = UI.EMPTY_STRING;
		String sumYearSub = UI.EMPTY_STRING;


		if (isWeekDisplayed) {

			// week

			sumYear = "startWeekYear"; //$NON-NLS-1$
			sumYearSub = "startWeek"; //$NON-NLS-1$

		} else {

			// month

			sumYear = "startYear"; //$NON-NLS-1$
			sumYearSub = "startMonth"; //$NON-NLS-1$
		}

		final String sql = "" //$NON-NLS-1$
				//
				+ "SELECT " //$NON-NLS-1$

				+ (sumYear + ", ") //$NON-NLS-1$
				+ (sumYearSub + ",") //$NON-NLS-1$
				+ SQL_SUM_COLUMNS

				+ (" FROM " + TourDatabase.TABLE_TOUR_DATA) //$NON-NLS-1$

				+ (" WHERE " + sumYear + "=?") //$NON-NLS-1$ //$NON-NLS-2$
				+ sqlFilter.getWhereClause()

				+ (" GROUP BY " + sumYear + ", " + sumYearSub) //$NON-NLS-1$ //$NON-NLS-2$
				+ (" ORDER BY " + sumYearSub); //$NON-NLS-1$

		try {

			final ZonedDateTime tourWeek = calendar8.with(//
					TimeTools.calendarWeek.dayOfWeek(),
					TimeTools.calendarWeek.getFirstDayOfWeek().getValue());

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sql);
			statement.setInt(1, tourYear);
			sqlFilter.setParameters(statement, 2);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVITourBookItem tourItem = new TVITourBookYearSub(tourBookView, this, _subCategory);

				children.add(tourItem);

				final int dbYear = result.getInt(1);
				final int dbYearSub = result.getInt(2);

				String columnText;
				ZonedDateTime categoryDateTime;

				if (isWeekDisplayed) {

					// week

					categoryDateTime = tourWeek//
							.with(TimeTools.calendarWeek.weekBasedYear(), dbYear)
							.with(TimeTools.calendarWeek.weekOfYear(), dbYearSub);

					columnText = String.format("[%02d] %s", //
							dbYearSub,
							categoryDateTime.format(UI.WeekMonthFormatter));

				} else {

					// month

					categoryDateTime = tourWeek//
							.withYear(dbYear)
							.withMonth(dbYearSub);

					columnText = categoryDateTime.format(UI.MonthFormatter);
				}

				tourItem.treeColumn = columnText;

				tourItem.tourYear = dbYear;
				tourItem.tourYearSub = dbYearSub;
				tourItem.colTourDateTime = new TourDateTime(categoryDateTime);

				tourItem.addSumColumns(result, 3);
			}

			conn.close();

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		}
	}

}
