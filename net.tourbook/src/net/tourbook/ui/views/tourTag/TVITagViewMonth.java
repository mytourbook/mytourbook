package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.TourTypeSQL;
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

	@Override
	protected void fetchChildren() {

		/*
		 * set the children for the root item, these are year items
		 */
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		try {

			/*
			 * get all tours for the tag Id of this tree item
			 */
			final StringBuilder sb = new StringBuilder();
			final TourTypeSQL sqlTourTypes = UI.sqlTourTypes();

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
			sb.append(UI.sqlTourPersonId());
			sb.append(sqlTourTypes.getWhereClause());

			sb.append(" ORDER BY startDay, startHour, startMinute");

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, fYearItem.getTagId());
			statement.setInt(2, fYear);
			statement.setInt(3, fMonth);
			sqlTourTypes.setSQLParameters(statement, 4);

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
	protected void remove() {}

}
