/*******************************************************************************
 * Copyright (C) 2001, 2008  Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourTag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

/**
 * root item for the tag view
 */
public class TVITagViewRoot extends TVITagViewItem {

	private TagView	fTagView;

	public TVITagViewRoot(final TagView tagView) {
		fTagView = tagView;
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
			 * get tag categories
			 */
			sb.append("SELECT");
			sb.append(" tagCategoryId,"); // 	1
			sb.append(" name"); // 				2

			sb.append(" FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY);
			sb.append(" WHERE isRoot = 1");
			sb.append(" ORDER BY name");

			statement = conn.prepareStatement(sb.toString());
			result = statement.executeQuery();

			while (result.next()) {

				final TVITagViewTagCategory treeItem = new TVITagViewTagCategory(this);
				children.add(treeItem);

				treeItem.tagCategoryId = result.getLong(1);
				treeItem.treeColumn = result.getString(2);
			}

			/*
			 * get tags
			 */
			sb.delete(0, sb.length());
			sb.append("SELECT");
			sb.append(" tagId,"); //		1
			sb.append(" name,"); //			2
			sb.append(" expandType"); //	3

			sb.append(" FROM " + TourDatabase.TABLE_TOUR_TAG);
			sb.append(" WHERE isRoot = 1");
			sb.append(" ORDER BY name");

			statement = conn.prepareStatement(sb.toString());
			result = statement.executeQuery();

			while (result.next()) {

				final TVITagViewTag treeItem = new TVITagViewTag(this);

				treeItem.tagId = result.getLong(1);
				treeItem.treeColumn = result.getString(2);
				treeItem.setExpandType(result.getInt(3));

				children.add(treeItem);
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	public TagView getTagView() {
		return fTagView;
	}

	@Override
	protected void remove() {}

}
