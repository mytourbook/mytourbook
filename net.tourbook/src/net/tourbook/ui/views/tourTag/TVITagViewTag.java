package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

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
			Statement statement;
			ResultSet result;

			/*
			 * get tour data for the tag Id of this tree item
			 */
			sb.append("SELECT ");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".StartYear,");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".StartMonth,");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".StartDay,");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".TourDistance,");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".TourRecordingTime,");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".TourDrivingTime,");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".TourAltUp,");
			sb.append(TourDatabase.TABLE_TOUR_DATA + ".TourAltDown");

			sb.append(" FROM " + TourDatabase.TABLE_TOUR_DATA + " " + TourDatabase.TABLE_TOUR_DATA);

			sb.append(" LEFT OUTER JOIN "
					+ TourDatabase.JOINTABLE_TOURDATA__TOURTAG
					+ " "
					+ TourDatabase.JOINTABLE_TOURDATA__TOURTAG
					+ " ON ");

			sb.append((TourDatabase.JOINTABLE_TOURDATA__TOURTAG + ".TourData_tourId = ")
					+ (TourDatabase.TABLE_TOUR_DATA + ".tourId"));

			sb.append(" WHERE " + TourDatabase.JOINTABLE_TOURDATA__TOURTAG + ".TourTag_TagId = " + tagId);

			//			String sqlString = "SELECT " //			//$NON-NLS-1$
//				+ "STARTYear, " //1				//$NON-NLS-1$
//				+ "STARTMonth, " //2			//$NON-NLS-1$
//				+ "STARTDay, " //3				//$NON-NLS-1$
//				+ "TOURDISTANCE, " //4			//$NON-NLS-1$
//				+ "TOURRECORDINGTIME, " //5		//$NON-NLS-1$
//				+ "TOURDRIVINGTIME, " //6		//$NON-NLS-1$
//				+ "TOURALTUP, " //7				//$NON-NLS-1$
//				+ "TOURALTDOWN, " //8			//$NON-NLS-1$
//				+ "startDistance, " //9			//$NON-NLS-1$
//				+ "tourID," //10				//$NON-NLS-1$
//				+ "tourType_typeId," //11		//$NON-NLS-1$
//				+ "tourTitle," //12				//$NON-NLS-1$
//				+ "deviceTimeInterval," //13	//$NON-NLS-1$
//				+ "maxSpeed," //14				//$NON-NLS-1$
//				+ "maxAltitude," //15			//$NON-NLS-1$
//				+ "maxPulse," //16				//$NON-NLS-1$
//				+ "avgPulse," //17				//$NON-NLS-1$
//				+ "avgCadence," //18			//$NON-NLS-1$
//				+ "avgTemperature" //19			//$NON-NLS-1$

//			statement = conn.prepareStatement(sb.toString());
			statement = conn.createStatement();

			result = statement.executeQuery(sb.toString());

			while (result.next()) {

				final TVITagViewTour treeItem = new TVITagViewTour(getTagView());
				treeItem.treeColumn = result.getString(1);

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
