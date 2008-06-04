/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

public class TVIRootItem extends TVIPrefTagViewer {

	public TVIRootItem(final TreeViewer tagViewer) {
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
			Query query = em.createQuery("SELECT TourTag " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_TAG + " AS TourTag ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" WHERE TourTag.isRoot = 1"));

			tourTags = (ArrayList<TourTag>) query.getResultList();

			for (final TourTag tourTag : tourTags) {
				addChild(new TVITourTag(getTagViewer(), tourTag));
			}

			/*
			 * read tag categories from db
			 */
			query = em.createQuery("SELECT TourTagCategory " //$NON-NLS-1$
					+ ("FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY + " AS TourTagCategory ") //$NON-NLS-1$ //$NON-NLS-2$
					+ (" WHERE TourTagCategory.isRoot = 1"));

			final ArrayList<TourTagCategory> tourTagCategories = (ArrayList<TourTagCategory>) query.getResultList();

			for (final TourTagCategory tourTagCategory : tourTagCategories) {
				addChild(new TVITourTagCategory(getTagViewer(), tourTagCategory));
			}

			em.close();
		}
	}

	@Override
	protected void remove() {}
}
