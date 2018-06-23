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
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalField;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

public class TVITourBookYear extends TVITourBookItem {

	private static final String	YEAR_WEEK_FORMAT	= "[%02d] %s";	//$NON-NLS-1$

	private YearSubCategory		_subCategory;

	boolean						isRowSummary;

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

		String sumYearField = UI.EMPTY_STRING;
		String sumYearFieldSub = UI.EMPTY_STRING;

		if (isWeekDisplayed) {

			// show weeks

			sumYearField = "StartWeekYear"; //$NON-NLS-1$
			sumYearFieldSub = "StartWeek"; //$NON-NLS-1$

		} else {

			// show months

			sumYearField = "StartYear"; //$NON-NLS-1$
			sumYearFieldSub = "StartMonth"; //$NON-NLS-1$
		}

		final SQLFilter sqlFilter = new SQLFilter(SQLFilter.TAG_FILTER);
		String fromTourData;

		if (sqlFilter.isTagFilterActive()) {

			// with tag filter

			fromTourData = NL

					+ "FROM (				" + NL //$NON-NLS-1$

					+ " SELECT				" + NL //$NON-NLS-1$

					+ sumYearField + ",			" + NL //$NON-NLS-1$
					+ sumYearFieldSub + ",		" + NL //$NON-NLS-1$
					+ SQL_SUM_FIELDS + NL

					+ "  FROM " + TourDatabase.TABLE_TOUR_DATA + NL//$NON-NLS-1$

					// get tag id's
					+ "  LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //$NON-NLS-1$ //$NON-NLS-2$
					+ "  ON tourID = jTdataTtag.TourData_tourId	" + NL //$NON-NLS-1$

					+ "  WHERE " + sumYearField + "=?" + NL //$NON-NLS-1$ //$NON-NLS-2$
					+ sqlFilter.getWhereClause()

					+ ") td				" + NL//$NON-NLS-1$
			;

		} else {

			// without tag filter

			fromTourData = NL

					+ " FROM " + TourDatabase.TABLE_TOUR_DATA + NL //$NON-NLS-1$

					+ " WHERE " + sumYearField + "=?" + NL //$NON-NLS-1$ //$NON-NLS-2$
					+ sqlFilter.getWhereClause() + NL;
		}

		final String sql = NL +

				"SELECT					" + NL //$NON-NLS-1$

				+ sumYearField + ",			" + NL //$NON-NLS-1$
				+ sumYearFieldSub + ",		" + NL //$NON-NLS-1$
				+ SQL_SUM_COLUMNS

				+ fromTourData

				+ " GROUP BY " + sumYearField + "," + sumYearFieldSub + NL //		//$NON-NLS-1$ //$NON-NLS-2$
				+ " ORDER BY " + sumYearFieldSub + NL //						//$NON-NLS-1$
		;
		try {

			final ZonedDateTime tourWeek = calendar8.with(//
					TimeTools.calendarWeek.dayOfWeek(),
					TimeTools.calendarWeek.getFirstDayOfWeek().getValue());

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sql);

			// set sql parameters
			statement.setInt(1, tourYear);
			sqlFilter.setParameters(statement, 2);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVITourBookItem tourItem = new TVITourBookYearSub(tourBookView, this, _subCategory);

				children.add(tourItem);

				final int dbYear = result.getInt(1);
				final int dbYearSub = result.getInt(2);

				String columnText;

				/*
				 * Fixed "java.time.LocalDate cannot be cast to java.time.ZonedDateTime" exception
				 * with this special date/time contruction
				 */
				LocalDate categoryDate;
				final LocalDate tourWeekLocal = tourWeek.toLocalDate();

				if (isWeekDisplayed) {

					// week

					final TemporalField weekBasedYear = TimeTools.calendarWeek.weekBasedYear();
					final TemporalField weekOfYear = TimeTools.calendarWeek.weekOfYear();
//					final TemporalField weekOfYear = TimeTools.calendarWeek.weekOfWeekBasedYear();

					categoryDate = tourWeekLocal//
							.with(weekBasedYear, dbYear)
							.with(weekOfYear, dbYearSub);

					columnText = String.format(
							YEAR_WEEK_FORMAT,
							dbYearSub,
							categoryDate.format(TimeTools.Formatter_Week_Month));

				} else {

					// month

					categoryDate = tourWeekLocal//
							.withYear(dbYear)
							.withMonth(dbYearSub);

					columnText = categoryDate.format(TimeTools.Formatter_Month);
				}

				final ZonedDateTime zonedWeek = ZonedDateTime.from(tourWeek);
				final ZonedDateTime zonedWeekDate = zonedWeek.with(categoryDate);

				tourItem.treeColumn = columnText;

				tourItem.tourYear = dbYear;
				tourItem.tourYearSub = dbYearSub;
				tourItem.colTourDateTime = new TourDateTime(zonedWeekDate);

				tourItem.addSumColumns(result, 3);
			}

			conn.close();

		} catch (final SQLException e) {
			net.tourbook.ui.UI.showSQLException(e);
		}
	}

	@Override
	public boolean hasChildren() {

		if (isRowSummary) {

			// row summary has no children

			return false;
		}

		return super.hasChildren();
	}

}
