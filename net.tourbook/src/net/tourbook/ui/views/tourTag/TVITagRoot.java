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

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TVITourTag;
import net.tourbook.tag.TVITourTagCategory;
import net.tourbook.tour.TreeViewerItem;

/**
 * root item for the tag view
 */
public class TVITagRoot extends TVITagItem {

	public TVITagRoot(final TagView tagView) {
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

		sb.append("SELECT name");
		sb.append(" FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY);
		sb.append(" WHERE isRoot = 1");

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
			final PreparedStatement statement = conn.prepareStatement(sb.toString());
			final ResultSet result = statement.executeQuery();

			while (result.next()) {

				final TVITagCategory treeItem = new TVITagCategory(fView, this);

				fCalendar.set(result.getShort(1), 0, 1);
				treeItem.fTourDate = fCalendar.getTimeInMillis();

				treeItem.addSumData(result.getLong(2),
						result.getLong(3),
						result.getLong(4),
						result.getLong(5),
						result.getLong(6),
						result.getLong(7),
						result.getFloat(8),
						result.getLong(9),
						result.getLong(10),
						result.getLong(11),
						result.getLong(12),
						result.getLong(13),
						result.getLong(14),
						result.getLong(15));

				children.add(treeItem);
			}

			conn.close();

		} catch (final SQLException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	protected void fetchChildrenOLD() {

		// set children for the root item
		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		ArrayList<TourTag> tourTags = new ArrayList<TourTag>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			/*
			 * read tour tags from db
			 */
			Query query = em.createQuery("SELECT TourTag " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_TAG + " AS TourTag ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" WHERE TourTag.isRoot = 1"));

			tourTags = (ArrayList<TourTag>) query.getResultList();

			for (final TourTag tourTag : tourTags) {
				children.add(new TVITourTag(tourTag));
			}

			/*
			 * read tag categories from db
			 */
			query = em.createQuery("SELECT TourTagCategory " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY + " AS TourTagCategory ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" WHERE TourTagCategory.isRoot = 1"));

			final ArrayList<TourTagCategory> tourTagCategories = (ArrayList<TourTagCategory>) query.getResultList();

			for (final TourTagCategory tourTagCategory : tourTagCategories) {
				children.add(new TVITourTagCategory(tourTagCategory));
			}

			em.close();
		}
	}

	@Override
	protected void remove() {}

}
