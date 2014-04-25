/*******************************************************************************
 * Copyright (C) 2005, 2014  Wolfgang Schramm and Contributors
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
package net.tourbook.sign;

import net.tourbook.data.TourSignCategory;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIPrefSignCategory extends TVIPrefSignItem {

	private TourSignCategory	_tourSignCategory;

	public TVIPrefSignCategory(final TreeViewer signViewer, final TourSignCategory tourSignCategory) {

		super(signViewer);

		_tourSignCategory = tourSignCategory;
	}

	@Override
	protected void fetchChildren() {

//		final EntityManager em = TourDatabase.getInstance().getEntityManager();
//
//		if (em == null) {
//			return;
//		}
//
//		final TourSignCategory tourSignCategory = em.find(TourSignCategory.class, _tourSignCategory.getCategoryId());
//
//		// create sign items
//		final Set<TourSign> lazyTourSigns = tourSignCategory.getTourSigns();
//		for (final TourSign tourSign : lazyTourSigns) {
//			addChild(new TVIPrefSign(getSignViewer(), tourSign));
//		}
//
//		// create category items
//		final Set<TourSignCategory> lazyTourSignCategories = tourSignCategory.getSignCategories();
//		for (final TourSignCategory signCategory : lazyTourSignCategories) {
//			addChild(new TVIPrefSignCategory(getSignViewer(), signCategory));
//		}
//
//		// update number of categories/signs
//		_tourSignCategory.setSignCounter(lazyTourSigns.size());
//		_tourSignCategory.setCategoryCounter(lazyTourSignCategories.size());
//
//		em.close();
//
//		/*
//		 * show number of signs/categories in the viewer, this must be done after the viewer task is
//		 * finished
//		 */
//		Display.getCurrent().asyncExec(new Runnable() {
//			public void run() {
//				getSignViewer().update(TVIPrefSignCategory.this, null);
//			}
//		});
	}

	/**
	 * @return Returns the sign category for this item
	 */
	public TourSignCategory getTourSignCategory() {
		return _tourSignCategory;
	}

	/**
	 * Set the sign category for this item
	 * 
	 * @param tourSignCategoryEntity
	 */
	public void setTourSignCategory(final TourSignCategory tourSignCategoryEntity) {
		_tourSignCategory = tourSignCategoryEntity;
	}

	@Override
	public String toString() {
		return "TVITourSignCategory: " + _tourSignCategory; //$NON-NLS-1$
	}

}
