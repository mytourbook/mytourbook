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
package net.tourbook.ui.views.tourCatalog;

import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.util.TreeViewerItem;

/**
 * contains tree viewer items (TVI) for reference tours
 */
public class TVICompareResultReferenceTour extends TVICompareResultItem {

	String										label;

	long										tourId;

	RefTourItem									refTourItem;

	/**
	 * keeps the tourId's for all compared tours which have already been stored in the db
	 */
	private HashMap<Long, StoredComparedTour>	_storedComparedTours;

	public TVICompareResultReferenceTour(	final TVICompareResultRootItem parentItem,
											final String label,
											final RefTourItem refTourItem,
											final long tourId) {

		this.setParentItem(parentItem);

		this.label = label;
		this.refTourItem = refTourItem;
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
		if (refTourItem == null) {
			if (other.refTourItem != null) {
				return false;
			}
		} else if (!refTourItem.equals(other.refTourItem)) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {

		final ArrayList<TreeViewerItem> children = new ArrayList<TreeViewerItem>();
		setChildren(children);

		final long refId = refTourItem.refId;

		if (_storedComparedTours != null) {
			_storedComparedTours.clear();
		}

		_storedComparedTours = TourCompareManager.getComparedToursFromDb(refId);

		final TVICompareResultComparedTour[] comparedTours = TourCompareManager.getInstance().getComparedTours();

		// create children for one reference tour
		for (final TVICompareResultComparedTour compTour : comparedTours) {

			if (compTour.refTour.refId == refId) {

				// compared tour belongs to the reference tour

				// keep the ref tour as the parent
				compTour.setParentItem(this);

				/*
				 * set the status if the compared tour is already stored in the database and set the
				 * id for the compared tour
				 */
				final Long comparedTourId = compTour.comparedTourData.getTourId();
				final boolean isStoredForRefTour = _storedComparedTours.containsKey(comparedTourId);

				if (isStoredForRefTour) {
					final StoredComparedTour storedComparedTour = _storedComparedTours.get(comparedTourId);
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
		result = prime * result + ((refTourItem == null) ? 0 : refTourItem.hashCode());
		return result;
	}

}
