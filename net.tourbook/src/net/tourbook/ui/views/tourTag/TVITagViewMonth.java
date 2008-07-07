package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.UI;

public class TVITagViewMonth extends TVITagViewItem {

	private final TVITagViewYear	fYearItem;

	private final int				fYear;
	private final int				fMonth;

	public TVITagViewMonth(final TVITagViewYear parentItem, final int dbYear, final int dbMonth) {

		setParentItem(parentItem);

		fYearItem = parentItem;
		fYear = dbYear;
		fMonth = dbMonth;
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

		if (fYear < otherMonthItem.fYear) {

			return -1;

		} else if (fYear > otherMonthItem.fYear) {

			return 1;

		} else {

			// same year, check month

			if (fMonth == otherMonthItem.fMonth) {
				return 0;
			} else if (fMonth < otherMonthItem.fMonth) {
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
		if (fMonth != other.fMonth) {
			return false;
		}
		if (fYear != other.fYear) {
			return false;
		}
		if (fYearItem == null) {
			if (other.fYearItem != null) {
				return false;
			}
		} else if (!fYearItem.equals(other.fYearItem)) {
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

			sb.append("SELECT");

			sb.append(" tourID,"); //						1	//$NON-NLS-1$
			sb.append(" jTdataTtag2.TourTag_tagId,");//		2
			sb.append(TVITagViewTour.SQL_TOUR_COLUMNS); //	3

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag and year/month
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData");
			sb.append(" ON jTdataTtag.TourData_tourId=TourData.tourId ");

			// get all tag id's for one tour 
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag2");
			sb.append(" ON TourData.tourID = jTdataTtag2.TourData_tourId");

			sb.append(" WHERE jTdataTtag.TourTag_TagId=?");
			sb.append(" AND startYear=?");
			sb.append(" AND startMonth=?");
			sb.append(sqlFilter.getWhereClause());

			sb.append(" ORDER BY startDay, startHour, startMinute");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, fYearItem.getTagId());
			statement.setInt(2, fYear);
			statement.setInt(3, fMonth);
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
		return fMonth;
	}

	public TVITagViewYear getYearItem() {
		return fYearItem;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fMonth;
		result = prime * result + fYear;
		result = prime * result + ((fYearItem == null) ? 0 : fYearItem.hashCode());
		return result;
	}

	@Override
	protected void remove() {}

}
