package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;
import net.tourbook.ui.UI;

public class TVITagViewTagCategory extends TVITagViewItem {

	long	tagCategoryId;
	String	name;

	public TVITagViewTagCategory(final TVITagViewItem parentItem) {
		setParentItem(parentItem);
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
			sb.append("SELECT");

			sb.append(" tblCat.tagCategoryId,");//	//1
			sb.append(" tblCat.name");//			//2

			sb.append(" FROM " + jTblCatCat + " jTblCatCat");

			sb.append(" LEFT OUTER JOIN " + tblCat + " tblCat ON ");
			sb.append(" jTblCatCat.TourTagCategory_tagCategoryId2 = tblCat.tagCategoryId ");

			sb.append(" WHERE jTblCatCat.TourTagCategory_tagCategoryId1 = ?");// + tagCategoryId);
			sb.append(" ORDER BY tblCat.name");

			final Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagCategoryId);

			ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVITagViewTagCategory treeItem = new TVITagViewTagCategory(this);
				children.add(treeItem);

				treeItem.tagCategoryId = result.getLong(1);
				treeItem.treeColumn = treeItem.name = result.getString(2);
			}

			/*
			 * get tags
			 */
			sb.setLength(0);
			sb.append("SELECT");

			sb.append(" tblTag.tagId,");//		1
			sb.append(" tblTag.name,");//		2
			sb.append(" tblTag.expandType");//	3

			sb.append(" FROM " + jTblCatTag + " jTblCatTag" + UI.NEW_LINE);

			// get all tags for the category
			sb.append(" LEFT OUTER JOIN " + tblTag + " tblTag ON");
			sb.append(" jTblCatTag.TourTag_TagId = tblTag.tagId" + UI.NEW_LINE);

			sb.append(" WHERE jTblCatTag.TourTagCategory_TagCategoryId = ?" + UI.NEW_LINE);
			sb.append(" ORDER BY tblTag.name" + UI.NEW_LINE);

			final String sql = sb.toString();
			statement = conn.prepareStatement(sql);
			statement.setLong(1, tagCategoryId);

			result = statement.executeQuery();
			while (result.next()) {

				final TVITagViewTag tagItem = new TVITagViewTag(this);
				children.add(tagItem);

				final long tagId = result.getLong(1);
				final int expandType = result.getInt(3);

				tagItem.tagId = tagId;
				tagItem.treeColumn = tagItem.name = result.getString(2);
				tagItem.setExpandType(expandType);

				getTagTotals(tagItem);
			}

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}
	}

	public long getCategoryId() {
		return tagCategoryId;
	}

	public String getName() {
		return name;
	}

	@Override
	protected void remove() {}

}
