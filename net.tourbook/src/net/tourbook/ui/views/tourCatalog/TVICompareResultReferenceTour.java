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
import java.util.HashMap;

import net.tourbook.data.TourReference;
import net.tourbook.ui.TreeViewerItem;

/**
 * contains tree viewer items (TVI) for reference tours
 */
public class TVICompareResultReferenceTour extends TVICompareResultItem {

	String										label;

	long										tourId;

	TourReference								refTour;

	/**
	 * keeps the tourId's for all compared tours which have already been stored in the db
	 */
	private HashMap<Long, StoredComparedTour>	fStoredComparedTours;

	public TVICompareResultReferenceTour(	final TVICompareResultRootItem parentItem,
											final String label,
											final TourReference refTour,
											final long tourId) {

		this.setParentItem(parentItem);

		this.label = label;
		this.refTour = refTour;
		this.tourId = tourId;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TVICompareResultReferenceTour)) {
			return false;
		}
		final TVICompareResultReferenceTour other = (TVICompareResultReferenceTour) obj;
		if (refTour == null) {
			if (other.refTour != null) {
				return false;
			}
		} else if (!refTour.equals(other.refTour)) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final long refId = refTour.getRefId();

		if (fStoredComparedTours != null) {
			fStoredComparedTours.clear();
		}
		
		fStoredComparedTours = TourCompareManager.getComparedToursFromDb(refId);

		final TVICompareResultComparedTour[] comparedTours = TourCompareManager.getInstance().getComparedTours();

		// create children for one reference tour
		for (final TVICompareResultComparedTour compTour : comparedTours) {

			if (compTour.refTour.getRefId() == refId) {

				// compared tour belongs to the reference tour

				// keep the ref tour as the parent
				compTour.setParentItem(this);

				/*
				 * set the status if the compared tour is already stored in the database and set the
				 * id for the compared tour
				 */
				final Long comparedTourId = compTour.comparedTourData.getTourId();
				final boolean isStoredForRefTour = fStoredComparedTours.containsKey(comparedTourId);

				if (isStoredForRefTour) {
					final StoredComparedTour storedComparedTour = fStoredComparedTours.get(comparedTourId);
					compTour.compId = storedComparedTour.comparedId;
					compTour.dbStartIndex = storedComparedTour.startIndex;
					compTour.dbEndIndex = storedComparedTour.endIndex;
					compTour.dbSpeed = storedComparedTour.tourSpeed;
				} else {
					compTour.compId = -1;
				}

				children.add(compTour);
			}
		}

	}

	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((refTour == null) ? 0 : refTour.hashCode());
		return result;
	}

	@Override
	public void remove() {}

}
