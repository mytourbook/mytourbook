/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.ui.views.tourMap;

import java.util.ArrayList;

import net.tourbook.data.TourReference;
import net.tourbook.tour.TreeViewerItem;

/**
 * Rootitem for compare results, the children are reference tours
 */
public class TVICompareResultRoot extends TreeViewerItem {

	protected void fetchChildren() {

		ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		TourReference[] refTours = TourCompareManager
				.getInstance()
				.getComparedReferenceTours();

		if (refTours != null) {
			for (TourReference refTour : refTours) {
				children.add(new TVICompareResultReference(
						this,
						refTour.getLabel(),
						refTour,
						refTour.getTourData().getTourId()));
			}
		}
	}

	protected void remove() {}
}
