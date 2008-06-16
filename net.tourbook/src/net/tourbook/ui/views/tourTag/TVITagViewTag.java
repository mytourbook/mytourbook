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
	public int	expandType;

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

			/*
			 * get tour data for the tag Id of this tree item
			 */
			sb.append("SELECT ");

			sb.append(" TData.TourId,");//	// 1 
			sb.append(" Tdata.StartYear,"); //				// 2 
			sb.append(" Tdata.StartMonth,");//				// 3
			sb.append(" Tdata.StartDay,");//				// 4
			sb.append(" Tdata.TourTitle,");//				// 5
			sb.append(" Tdata.tourType_typeId,");//			// 6

//			sb.append(" Tdata.TourDistance,");//			// 
//			sb.append(" Tdata.TourRecordingTime,");//		// 
//			sb.append(" Tdata.TourDrivingTime,");//			// 
//			sb.append(" Tdata.TourAltUp,");//				// 
//			sb.append(" Tdata.TourAltDown,");//				// 

			sb.append(" jTdataTtag.TourTag_tagId");//		// 7 

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " Tdata ON");
			sb.append(" jTdataTtag.TourData_tourId=Tdata.tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");// + tagId);

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVITagViewTour tourItem = new TVITagViewTour(getTagView());

				tourItem.tourId = result.getLong(1);
				tourItem.tourDate = new DateTime(result.getInt(2), result.getInt(3), result.getInt(4), 0, 0, 0, 0);
				tourItem.tourTitle = result.getString(5);

				final Object tourTypeId = result.getObject(6);
				tourItem.tourTypeId = (tourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) tourTypeId);

				System.out.println(result.getObject(1) + //
						("\t" + result.getObject(7))
				//
				);
				
				children.add(tourItem);
			}

			System.out.println();
			
			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void remove() {}

}
