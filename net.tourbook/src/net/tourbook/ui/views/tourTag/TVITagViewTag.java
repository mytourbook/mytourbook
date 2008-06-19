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
			 * get all tours for the tag Id of this tree item
			 */
			sb.append("SELECT ");

			sb.append(" Tdata.tourId,");//					// 1 
			sb.append(" Tdata.startYear,"); //				// 2 
			sb.append(" Tdata.startMonth,");//				// 3
			sb.append(" Tdata.startDay,");//				// 4
			sb.append(" Tdata.tourTitle,");//				// 5
			sb.append(" Tdata.tourType_typeId,");//			// 6

//			sb.append(" Tdata.TourDistance,");//			// 
//			sb.append(" Tdata.TourRecordingTime,");//		// 
//			sb.append(" Tdata.TourDrivingTime,");//			// 
//			sb.append(" Tdata.TourAltUp,");//				// 
//			sb.append(" Tdata.TourAltDown,");//				// 

			sb.append(" jTdataTtag2.TourTag_tagId");//		// 7 

			sb.append(" FROM " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag");

			// get all tours for current tag
			sb.append(" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " Tdata");
			sb.append(" ON jTdataTtag.TourData_tourId=Tdata.tourId ");

			// get all tag id's for one tour 
			sb.append(" LEFT OUTER JOIN " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + " jTdataTtag2");
			sb.append(" ON Tdata.tourID = jTdataTtag2.TourData_tourId");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");// + tagId);

			long lastTourId = -1;
			ArrayList<Long> tagIds = null;

			final Connection conn = TourDatabase.getInstance().getConnection();

			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagId);

			final ResultSet result = statement.executeQuery();
			while (result.next()) {

				long tourId = result.getLong(1);

				if (tourId == lastTourId) {

					// get tags from outer join

					final Object resultTagId = result.getObject(7);
					if (resultTagId instanceof Long) {
						tagIds.add((Long) resultTagId);
					}

				} else {

					final TVITagViewTour tourItem = new TVITagViewTour(getTagView());
					children.add(tourItem);

					tourItem.tourId = tourId = result.getLong(1);
					tourItem.tourDate = new DateTime(result.getInt(2), result.getInt(3), result.getInt(4), 0, 0, 0, 0);
					tourItem.tourTitle = result.getString(5);

					final Object tourTypeId = result.getObject(6);
					tourItem.tourTypeId = (tourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) tourTypeId);

					final Object resultTagId = result.getObject(7);
					if (resultTagId instanceof Long) {
						tourItem.tagIds = tagIds = new ArrayList<Long>();
						tagIds.add((Long) resultTagId);
					}

				}
				
				lastTourId = tourId;
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void remove() {}

}
