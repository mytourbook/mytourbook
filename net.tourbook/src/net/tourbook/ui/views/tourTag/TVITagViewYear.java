package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

public class TVITagViewYear extends TVITagViewItem {

	private final int		fYear;
	private TVITagViewTag	fTagItem;

	public TVITagViewYear(final TVITagViewTag parentItem, final int year) {
		setParentItem(parentItem);
		fTagItem = parentItem;
		fYear = year;
	}

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			sb.append("SELECT");
			sb.append(" startYear,"); //		// 1
			sb.append(" startMonth,"); //		// 2

			sb.append(SQL_SUM_COLUMNS);

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag and year
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " Tdata");
			sb.append(" ON jTdataTtag.TourData_tourId=Tdata.tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId=?");
			sb.append(" AND startYear=?");
			sb.append(sqlTourPersonId());
			sb.append(sqlTourTypeId());

			sb.append(" GROUP BY startYear, startMonth");
			sb.append(" ORDER BY startYear");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, fTagItem.tagId);
			statement.setInt(2, fYear);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final int dbYear = result.getInt(1);
				final int dbMonth = result.getInt(2);

				final TVITagViewMonth tourItem = new TVITagViewMonth(this, dbYear, dbMonth);
				children.add(tourItem);

				tourItem.treeColumn = Integer.toString(dbMonth);
				tourItem.addSumData(result, 2);
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	public int getYear() {
		return fYear;
	}

	@Override
	protected void remove() {}

}
