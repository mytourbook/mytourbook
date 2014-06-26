/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITagViewMonth extends TVITagViewItem {

	private final TVITagViewYear	_yearItem;

	private final int				_year;
	private final int				_month;

	public TVITagViewMonth(final TVITagViewYear parentItem, final int dbYear, final int dbMonth) {

		setParentItem(parentItem);

		_yearItem = parentItem;
		_year = dbYear;
		_month = dbMonth;
	}

	/**
	 * Compare two instances of {@link TVITagViewMonth}
	 * 
	 * @param otherMonthItem
	 * @return
	 */
	public int compareTo(final TVITagViewMonth otherMonthItem) {

		if (this == otherMonthItem) {
			return 0;
		}

		if (_year < otherMonthItem._year) {

			return -1;

		} else if (_year > otherMonthItem._year) {

			return 1;

		} else {

			// same year, check month

			if (_month == otherMonthItem._month) {
				return 0;
			} else if (_month < otherMonthItem._month) {
				return -1;
			} else {
				return 1;
			}
		}
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TVITagViewMonth other = (TVITagViewMonth) obj;
		if (_month != other._month) {
			return false;
		}
		if (_year != other._year) {
			return false;
		}
		if (_yearItem == null) {
			if (other._yearItem != null) {
				return false;
			}
		} else if (!_yearItem.equals(other._yearItem)) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set tour children for the month
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		try {

			/*
			 * get all tours for the current month
			 */
			final StringBuilder sb = new StringBuilder();
			final SQLFilter sqlFilter = new SQLFilter();

			sb.append("SELECT"); //$NON-NLS-1$

			sb.append(" tourID,"); //						1	//$NON-NLS-1$
			sb.append(" jTdataTtag2.TourTag_tagId,");//		2 //$NON-NLS-1$
			//
			sb.append(TVITagViewTour.SQL_TOUR_COLUMNS); //	3

			sb.append(" FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag"); //$NON-NLS-1$ //$NON-NLS-2$

			// get all tours for current tag and year/month
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON jTdataTtag.TourData_tourId=TourData.tourId "); //$NON-NLS-1$

			// get all tag id's for one tour
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag2"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" ON TourData.tourID = jTdataTtag2.TourData_tourId"); //$NON-NLS-1$

			sb.append(" WHERE jTdataTtag.TourTag_TagId=?"); //$NON-NLS-1$
			sb.append(" AND startYear=?"); //$NON-NLS-1$
			sb.append(" AND startMonth=?"); //$NON-NLS-1$
			sb.append(sqlFilter.getWhereClause());

			sb.append(" ORDER BY startDay, startHour, startMinute"); //$NON-NLS-1$

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, _yearItem.getTagId());
			statement.setInt(2, _year);
			statement.setInt(3, _month);
			sqlFilter.setParameters(statement, 4);

			long lastTourId = -1;
			TVITagViewTour tourItem = null;

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final long tourId = result.getLong(1);
				final Object resultTagId = result.getObject(2);

				if (tourId == lastTourId) {

					// get tags from outer join

					if (resultTagId instanceof Long) {
						tourItem.tagIds.add((Long) resultTagId);
					}

				} else {

					// new tour is in the resultset
					tourItem = new TVITagViewTour(this);

					children.add(tourItem);

					tourItem.tourId = tourId;
					tourItem.getTourColumnData(result, resultTagId, 3);

					tourItem.treeColumn = Integer.toString(tourItem.tourDay);
				}

				lastTourId = tourId;
			}
			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	public int getMonth() {
		return _month;
	}

	public TVITagViewYear getYearItem() {
		return _yearItem;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + _month;
		result = prime * result + _year;
		result = prime * result + ((_yearItem == null) ? 0 : _yearItem.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "TVITagViewMonth " + System.identityHashCode(this)

		+ " [_year=" + _year + ", _month=" + _month + ", _yearItem=" + _yearItem + "]";
	}
}
