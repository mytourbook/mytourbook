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

package net.tourbook.tag;

import java.util.Set;

import javax.persistence.EntityManager;

import net.tourbook.data.TourTag;
import net.tourbook.data.TourTagCategory;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TreeViewerItem;

public class TVITourTagCategory extends TreeViewerItem {

	private TourTagCategory	fTourTagCategory;

	public TVITourTagCategory(final TourTagCategory tourTagCategory) {
		fTourTagCategory = tourTagCategory;
	}

	@Override
	protected void fetchChildren() {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em != null) {

			final TourTagCategory tourTagCategory = em.find(TourTagCategory.class, fTourTagCategory.getCategoryId());

			// create tree tag items
			final Set<TourTag> lazyTourTags = tourTagCategory.getTourTags();
			for (final TourTag tourTag : lazyTourTags) {
				addChild(new TVITourTag(tourTag));
			}

			// create tree category items
			final Set<TourTagCategory> lazyTourTagCategories = tourTagCategory.getTagCategories();
			for (final TourTagCategory tagCategory : lazyTourTagCategories) {
				addChild(new TVITourTagCategory(tagCategory));
			}

			em.close();
		}
	}

	/**
	 * @return Returns the tag category for this view item
	 */
	public TourTagCategory getTourTagCategory() {
		return fTourTagCategory;
	}

	@Override
	protected void remove() {}

	public void setTourTagCategory(final TourTagCategory tourTagCategoryEntity) {
		fTourTagCategory = tourTagCategoryEntity;
	}

}
