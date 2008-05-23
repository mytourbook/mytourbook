package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
			PreparedStatement statement;
			ResultSet result;

			/*
			 * get tour tag categories
			 */
			sb.append("SELECT ");
			sb.append(" StartYear,");
			sb.append(" StartMonth,");
			sb.append(" StartDay,");
			sb.append(" TourDistance,");
			sb.append(" TourRecordingTime,");
			sb.append(" TourDrivingTime,");
			sb.append(" TourAltUp,");
			sb.append(" TourAltDown");
			sb.append(" FROM " + TourDatabase.TABLE_TOUR_DATA + " AS TourData");
			sb.append(" WHERE isRoot = 1");

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
//				+ "\n" //						//$NON-NLS-1$
//				+ ("FROM " + TourDatabase.TABLE_TOUR_DATA + " \n") //			//$NON-NLS-1$ //$NON-NLS-2$
//				+ (" WHERE STARTYEAR = " + yearItem.fTourYear) //				//$NON-NLS-1$
//				+ (" AND STARTMONTH = " + fTourMonth) //						//$NON-NLS-1$
//				+ sqlTourPersonId()
//				+ sqlTourTypeId()
//				+ " ORDER BY STARTDAY, StartHour, StartMinute"; //$NON-NLS-1$

			statement = conn.prepareStatement(sb.toString());
			result = statement.executeQuery();

			while (result.next()) {

				final TVITagViewTagCategory treeItem = new TVITagViewTagCategory(getTagView());
				treeItem.treeColumn = result.getString(1);

				children.add(treeItem);
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void remove() {

	}

}
