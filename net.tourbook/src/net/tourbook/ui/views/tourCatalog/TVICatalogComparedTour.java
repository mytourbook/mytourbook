/*******************************************************************************
 * Copyright (C) 2005, 2015 Wolfgang Schramm and Contributors
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

import java.sql.Date;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourCompared;

/**
 * Represents a compared tour (tree item) in the tour map viewer
 */
public class TVICatalogComparedTour extends TVICatalogTourItem {

	/**
	 * unique id for the {@link TourCompared} entity
	 */
	long			compareId;

	/**
	 * 
	 */
	long			refId		= -1;

	int				startIndex	= -1;
	int				endIndex	= -1;

	/*
	 * fields from TourData
	 */
	long			tourTypeId;
	String			tourTitle;
	Date			tourDate;

	float			tourSpeed;
	float			avgPulse;

	ArrayList<Long>	tagIds;

	public TVICatalogComparedTour(final TVICatalogYearItem parentItem) {
		setParentItem(parentItem);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof TVICatalogComparedTour)) {
			return false;
		}
		final TVICatalogComparedTour other = (TVICatalogComparedTour) obj;
		if (compareId != other.compareId) {
			return false;
		}
		if (refId != other.refId) {
			return false;
		}
		return true;
	}

	@Override
	protected void fetchChildren() {}

	public float getAvgPulse() {
		return avgPulse;
	}

	/**
	 * @return Returns the Id for {@link TourCompared} entity
	 */
	public long getCompId() {
		return compareId;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public long getRefId() {
		return refId;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public Date getTourDate() {
		return tourDate;
	}

	public float getTourSpeed() {
		return tourSpeed;
	}

	@Override
	public boolean hasChildren() {
		/*
		 * compared tours do not have children
		 */
		return false;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (compareId ^ (compareId >>> 32));
		result = prime * result + (int) (refId ^ (refId >>> 32));
		return result;
	}

	void remove() {

		// remove this tour item from the parent
		final ArrayList<TreeViewerItem> unfetchedChildren = getParentItem().getUnfetchedChildren();
		if (unfetchedChildren != null) {
			unfetchedChildren.remove(this);
		}
	}

	public void setAvgPulse(final float avgPulse) {
		this.avgPulse = avgPulse;
	}

	void setEndIndex(final int endIndex) {
		this.endIndex = endIndex;
	}

	void setStartIndex(final int startIndex) {
		this.startIndex = startIndex;
	}

	void setTourSpeed(final float tourSpeed) {
		this.tourSpeed = tourSpeed;
	}
}
