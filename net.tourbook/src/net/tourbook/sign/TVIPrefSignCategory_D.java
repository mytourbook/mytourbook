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

import net.tourbook.data.TourSign;
import net.tourbook.data.TourSignCategory_D;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

public class TVIPrefSignCategory_D extends TVIPrefSignItem {

	private TourSignCategory_D	_tourSignCategory;

	public TVIPrefSignCategory_D(final TreeViewer signViewer, final TourSignCategory tourSignCategory) {

		super(signViewer);

		_tourSignCategory = tourSignCategory;
	}

	@Override
	protected void fetchChildren() {

		final SignCollection signChildren = SignManager.getSignEntries(_tourSignCategory.getCategoryId());

		final TreeViewer signViewer = getSignViewer();

		// create sign items
		for (final TourSign sign : signChildren.tourSigns) {
			addChild(new TVIPrefSign(this, signViewer, sign));
		}

		// create category items
		for (final TourSignCategory signCategory : signChildren.tourSignCategories) {
			addChild(new TVIPrefSignCategory(signViewer, signCategory));
		}

		// update number of categories/signs
		_tourSignCategory.setSignCounter(signChildren.tourSigns.size());
		_tourSignCategory.setCategoryCounter(signChildren.tourSignCategories.size());

		/*
		 * show number of signs/categories in the viewer, this must be done after the viewer task is
		 * finished
		 */
		Display.getCurrent().asyncExec(new Runnable() {
			public void run() {
				signViewer.update(TVIPrefSignCategory.this, null);
			}
		});
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
