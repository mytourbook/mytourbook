package net.tourbook.ui.views.tourTag;

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

			sb.append(" TData.TourId			as tdata_tourid,");//	// 1 
			sb.append(" Tdata.StartYear			as tdata_year,"); //				// 2 
			sb.append(" Tdata.StartMonth		as tdata_month,");//				// 3
			sb.append(" Tdata.StartDay			as tdata_day,");//				// 4
			sb.append(" Tdata.TourTitle			as tdata_title,");//				// 5
			sb.append(" Tdata.tourType_typeId	as tdata_typeid,");//			// 6

//			sb.append(" Tdata.TourDistance,");//			// 
//			sb.append(" Tdata.TourRecordingTime,");//		// 
//			sb.append(" Tdata.TourDrivingTime,");//			// 
//			sb.append(" Tdata.TourAltUp,");//				// 
			sb.append(" Tdata.TourAltDown	as tdata_altdown");//				// 

			sb.append(" FROM \"USER\".\"TOURDATA_TOURTAG\" jTdataTtag");
			sb.append(" LEFT OUTER JOIN \"USER\".\"TOURDATA\" Tdata ON ");
			sb.append(" jTdataTtag.TourData_tourId=Tdata.tourId ");

			sb.append(" WHERE jTdataTtag.TourTag_TagId = ?");// + tagId);

			final String sqlString = sb.toString();

//			final long time = System.currentTimeMillis();

			final PreparedStatement statement = TourDatabase.getInstance().getPreparedStatement(sqlString);
			statement.setLong(1, tagId);

			final ResultSet result = statement.executeQuery();

//			System.out.println(System.currentTimeMillis() - time + "ms\t" + sqlString);

			while (result.next()) {

				final TVITagViewTour tourItem = new TVITagViewTour(getTagView());

				tourItem.tourId = result.getLong(1);
				tourItem.tourDate = new DateTime(result.getInt(2), result.getInt(3), result.getInt(4), 0, 0, 0, 0);
				tourItem.tourTitle = result.getString(5);

				final Object tourTypeId = result.getObject(6);
				tourItem.tourTypeId = (tourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) tourTypeId);

				children.add(tourItem);
			}

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void remove() {}

}
