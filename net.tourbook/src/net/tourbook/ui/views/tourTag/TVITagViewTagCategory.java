package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

public class TVITagViewTagCategory extends TVITagViewItem {

	public long	tagCategoryId;

	public TVITagViewTagCategory(final TagView tagView) {
		super(tagView);
	}

	@Override
	protected void fetchChildren() {

		// create child items for this tag category item
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final StringBuilder sb = new StringBuilder();

		try {

			/*
			 * get all tags for the current tag category
			 */

			final Connection conn = TourDatabase.getInstance().getConnection();

			final String schema = "\"" + TourDatabase.TABLE_SCHEMA + "\".";
			final String jTblCatTag = schema + TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAG;
			final String tblTag = schema + TourDatabase.TABLE_TOUR_TAG;

			sb.append("SELECT ");

			sb.append(" tblTag.tagId,");//	//1
			sb.append(" tblTag.name");//	//2

			sb.append(" FROM " + jTblCatTag + " jTblCatTag");
			sb.append(" LEFT OUTER JOIN " + tblTag + " tblTag ON ");
			sb.append(" jTblCatTag.TourTag_TagId = tblTag.tagId ");

			sb.append(" WHERE jTblCatTag.TourTagCategory_TagCategoryId = " + tagCategoryId);

			final String sqlString = sb.toString();
			System.out.println(sqlString);
			final PreparedStatement statement = conn.prepareStatement(sqlString);
			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				final TVITagViewTag treeItem = new TVITagViewTag(getTagView());

				treeItem.tagId = result.getLong(1);
				treeItem.treeColumn = result.getString(2);

				children.add(treeItem);
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void remove() {}

}
