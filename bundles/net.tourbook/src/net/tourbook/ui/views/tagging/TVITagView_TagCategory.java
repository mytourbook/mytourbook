/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *******************************************************************************/
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

public class TVITagView_TagCategory extends TVITagViewItem {

	long		tagCategoryId;
	String	name;

	public TVITagView_TagCategory(final TVITagViewItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	protected void fetchChildren() {

		// create child items for this tag category item
		final ArrayList<TreeViewerItem> children = new ArrayList<>();
		setChildren(children);

		try {

			/*
			 * get all tags for the current tag category
			 */

			final String tblTag = TourDatabase.TABLE_TOUR_TAG;
			final String tblCat = TourDatabase.TABLE_TOUR_TAG_CATEGORY;
			final String jTblCatTag = TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG;
			final String jTblCatCat = TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY;

			final StringBuilder sb = new StringBuilder();

			/*
			 * get tag categories
			 */
			sb.append("SELECT"); //$NON-NLS-1$

			sb.append(" tblCat.tagCategoryId,");//	//1 //$NON-NLS-1$
			sb.append(" tblCat.name");//			//2 //$NON-NLS-1$

			sb.append(" FROM " + jTblCatCat + " jTblCatCat"); //$NON-NLS-1$ //$NON-NLS-2$

			sb.append(" LEFT OUTER JOIN " + tblCat + " tblCat ON "); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" jTblCatCat.TourTagCategory_tagCategoryId2 = tblCat.tagCategoryId "); //$NON-NLS-1$

			sb.append(" WHERE jTblCatCat.TourTagCategory_tagCategoryId1 = ?");// + tagCategoryId); //$NON-NLS-1$
			sb.append(" ORDER BY tblCat.name"); //$NON-NLS-1$

			final Connection conn = TourDatabase.getInstance().getConnection();
			PreparedStatement statement = conn.prepareStatement(sb.toString());
			statement.setLong(1, tagCategoryId);

			ResultSet result = statement.executeQuery();
			while (result.next()) {

				final TVITagView_TagCategory treeItem = new TVITagView_TagCategory(this);
				children.add(treeItem);

				treeItem.tagCategoryId = result.getLong(1);
				treeItem.treeColumn = treeItem.name = result.getString(2);
			}

			/*
			 * get tags
			 */
			sb.setLength(0);
			sb.append("SELECT"); //$NON-NLS-1$

			sb.append(" tblTag.tagId,");//		1 //$NON-NLS-1$
			sb.append(" tblTag.name,");//		2 //$NON-NLS-1$
			sb.append(" tblTag.expandType");//	3 //$NON-NLS-1$

			sb.append(" FROM " + jTblCatTag + " jTblCatTag" + UI.NEW_LINE); //$NON-NLS-1$ //$NON-NLS-2$

			// get all tags for the category
			sb.append(" LEFT OUTER JOIN " + tblTag + " tblTag ON"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(" jTblCatTag.TourTag_TagId = tblTag.tagId" + UI.NEW_LINE); //$NON-NLS-1$

			sb.append(" WHERE jTblCatTag.TourTagCategory_TagCategoryId = ?" + UI.NEW_LINE); //$NON-NLS-1$
			sb.append(" ORDER BY tblTag.name" + UI.NEW_LINE); //$NON-NLS-1$

			final String sql = sb.toString();
			statement = conn.prepareStatement(sql);
			statement.setLong(1, tagCategoryId);

			result = statement.executeQuery();
			while (result.next()) {

				final TVITagView_Tag tagItem = new TVITagView_Tag(this);
				children.add(tagItem);

				final long tagId = result.getLong(1);
				final int expandType = result.getInt(3);

				tagItem.tagId = tagId;
				tagItem.treeColumn = tagItem.name = result.getString(2);
				tagItem.setExpandType(expandType);

				readTagTotals(tagItem);
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
	public String toString() {
		return "TVITagView_TagCategory " //$NON-NLS-1$
				+ System.identityHashCode(this)
				+ " [tagCategoryId=" //$NON-NLS-1$
				+ tagCategoryId
				+ ", name=" //$NON-NLS-1$
				+ name
				+ "]"; //$NON-NLS-1$
	}
}
