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

		try {

			/*
			 * get all tags for the current tag category
			 */

			final String tblTag = TourDatabase.TABLE_TOUR_TAG;
			final String tblCat = TourDatabase.TABLE_TOUR_TAG_CATEGORY;
			final String jTblCatTag = TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAG;
			final String jTblCatCat = TourDatabase.JOINTABLE_TOURTAGCATEGORY_TOURTAGCATEGORY;

			final StringBuilder sb = new StringBuilder();

			/*
			 * get tag categories
			 */
			sb.append("SELECT ");

			sb.append(" tblCat.tagCategoryId,");//	//1
			sb.append(" tblCat.name");//			//2

			sb.append(" FROM " + jTblCatCat + " jTblCatCat");
			sb.append(" LEFT OUTER JOIN " + tblCat + " tblCat ON ");
			sb.append(" jTblCatCat.TourTagCategory_tagCategoryId2 = tblCat.tagCategoryId ");

			sb.append(" WHERE jTblCatCat.TourTagCategory_tagCategoryId1 = ?");// + tagCategoryId);

			final Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagCategoryId);

			ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVITagViewTagCategory treeItem = new TVITagViewTagCategory(getTagView());

				treeItem.tagCategoryId = result.getLong(1);
				treeItem.treeColumn = result.getString(2);

				children.add(treeItem);
			}

			/*
			 * get tags
			 */
			sb.setLength(0);
			sb.append("SELECT ");

			sb.append(" tblTag.tagId,");//		1
			sb.append(" tblTag.name,");//		2
			sb.append(" tblTag.expandType");//	3

			sb.append(" FROM " + jTblCatTag + " jTblCatTag");
			sb.append(" LEFT OUTER JOIN " + tblTag + " tblTag ON ");
			sb.append(" jTblCatTag.TourTag_TagId = tblTag.tagId ");

			sb.append(" WHERE jTblCatTag.TourTagCategory_TagCategoryId = ?");

			statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagCategoryId);

			result = statement.executeQuery();
			while (result.next()) {

				final TVITagViewTag treeItem = new TVITagViewTag(getTagView());

				treeItem.tagId = result.getLong(1);
				treeItem.treeColumn = result.getString(2);
				treeItem.expandType = result.getInt(3);

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
