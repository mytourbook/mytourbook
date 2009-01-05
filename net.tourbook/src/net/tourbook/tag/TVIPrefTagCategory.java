/*******************************************************************************
 * Copyright (C) 2005, 2009  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

public class TVIPrefTagCategory extends TVIPrefTagItem {

	private TourTagCategory	fTourTagCategory;

	public TVIPrefTagCategory(final TreeViewer tagViewer, final TourTagCategory tourTagCategory) {

		super(tagViewer);

		fTourTagCategory = tourTagCategory;
	}

	@Override
	protected void fetchChildren() {

		final EntityManager em = TourDatabase.getInstance().getEntityManager();

		if (em == null) {
			return;
		}

		final TourTagCategory tourTagCategory = em.find(TourTagCategory.class, fTourTagCategory.getCategoryId());

		// create tag items
		final Set<TourTag> lazyTourTags = tourTagCategory.getTourTags();
		for (final TourTag tourTag : lazyTourTags) {
			addChild(new TVIPrefTag(getTagViewer(), tourTag));
		}

		// create category items
		final Set<TourTagCategory> lazyTourTagCategories = tourTagCategory.getTagCategories();
		for (final TourTagCategory tagCategory : lazyTourTagCategories) {
			addChild(new TVIPrefTagCategory(getTagViewer(), tagCategory));
		}

		// update number of categories/tags
		fTourTagCategory.setTagCounter(lazyTourTags.size());
		fTourTagCategory.setCategoryCounter(lazyTourTagCategories.size());

		em.close();

		/*
		 * show number of tags/categories in the viewer, this must be done after the viewer task is
		 * finished
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				getTagViewer().update(TVIPrefTagCategory.this, null);
			}
		});
	}

	/**
	 * @return Returns the tag category for this item
	 */
	public TourTagCategory getTourTagCategory() {
		return fTourTagCategory;
	}

	@Override
	protected void remove() {}

	/**
	 * Set the tag category for this item
	 * 
	 * @param tourTagCategoryEntity
	 */
	public void setTourTagCategory(final TourTagCategory tourTagCategoryEntity) {
		fTourTagCategory = tourTagCategoryEntity;
	}

	@Override
	public String toString() {
		return "TVITourTagCategory: " + fTourTagCategory; //$NON-NLS-1$
	}

}
