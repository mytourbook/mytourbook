package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

import org.joda.time.DateTime;

public class TVITagViewTag extends TVITagViewItem {

	public long	tagId;

	public TVITagViewTag(final TagView tagView) {
		super(tagView);
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

			final Connection conn = TourDatabase.getInstance().getConnection();

			/*
			 * get tour data for the tag Id of this tree item
			 */
			sb.append("SELECT ");

			sb.append(" jTdataTtag.TourData_tourId,");//	// 1 
			sb.append(" Tdata.StartYear,"); //				// 2 
			sb.append(" Tdata.StartMonth,");//				// 3
			sb.append(" Tdata.StartDay,");//				// 4
			sb.append(" Tdata.TourDistance,");//			// 5
			sb.append(" Tdata.TourRecordingTime,");//		// 6
			sb.append(" Tdata.TourDrivingTime,");//			// 7
			sb.append(" Tdata.TourAltUp,");//				// 8
			sb.append(" Tdata.TourAltDown");//				// 9

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " Tdata ON ");
			sb.append(" jTdataTtag.TourData_tourId=Tdata.tourId ");
//			sb.append(" Tdata.tourId=jTdataTtag.TourData_tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = " + tagId);

			final String sqlString = sb.toString();

			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

//			final long time = System.currentTimeMillis();
//			System.out.println(System.currentTimeMillis() - time + "ms\t" + sqlString);

			while (result.next()) {

				final TVITagViewTour treeItem = new TVITagViewTour(getTagView());

				treeItem.treeColumn = result.getString(1);

				treeItem.tourDate = new DateTime(result.getInt(2), result.getInt(3), result.getInt(4), 0, 0, 0, 0);

				children.add(treeItem);
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

//	@Override
//	public boolean hasChildren() {
//		return false;
//	}

	@Override
	protected void remove() {}

}
