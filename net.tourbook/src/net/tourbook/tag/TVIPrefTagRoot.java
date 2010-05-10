/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

package net.tourbook.tag;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIPrefTagRoot extends TVIPrefTagItem {

	public TVIPrefTagRoot(final TreeViewer tagViewer) {
		super(tagViewer);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void fetchChildren() {

		ArrayList<TourTag> tourTags = new ArrayList<TourTag>();

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			/*
			 * read tour tags from db
			 */
			Query query = em.createQuery(//
					//
					"SELECT tourTag" //$NON-NLS-1$
							+ (" FROM TourTag AS tourTag ") //$NON-NLS-1$
							+ (" WHERE tourTag.isRoot = 1")); //$NON-NLS-1$

			tourTags = (ArrayList<TourTag>) query.getResultList();

			for (final TourTag tourTag : tourTags) {
				final TVIPrefTag tagItem = new TVIPrefTag(getTagViewer(), tourTag);
				addChild(tagItem);
			}

			/*
			 * read tag categories from db
			 */
			query = em.createQuery(//
					//
					"SELECT tourTagCategory" //$NON-NLS-1$
							+ (" FROM TourTagCategory AS tourTagCategory") //$NON-NLS-1$
							+ (" WHERE tourTagCategory.isRoot = 1")); //$NON-NLS-1$

			final ArrayList<TourTagCategory> tourTagCategories = (ArrayList<TourTagCategory>) query.getResultList();

			for (final TourTagCategory tourTagCategory : tourTagCategories) {
				final TVIPrefTagCategory categoryItem = new TVIPrefTagCategory(getTagViewer(), tourTagCategory);
				addChild(categoryItem);
			}

			em.close();
		}
	}

	@Override
	protected void remove() {}
}
