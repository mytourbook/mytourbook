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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;

import net.tourbook.data.TourReference;
import net.tourbook.ui.TreeViewerItem;

/**
 * Rootitem for compare results, the children are reference tours
 */
public class TVICompareResultRootItem extends TVICompareResultItem {

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final TourReference[] refTours = TourCompareManager.getInstance().getComparedReferenceTours();

		if (refTours != null) {
			for (final TourReference refTour : refTours) {
				children.add(new TVICompareResultReferenceTour(this, refTour.getLabel(), refTour, refTour.getTourData()
						.getTourId()));
			}
		}
	}

	@Override
	protected void remove() {}
}
