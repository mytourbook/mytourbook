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

import java.sql.Date;

import net.tourbook.data.TourCompared;
import net.tourbook.tour.TreeViewerTourItem;

/**
 * Represents a compared tour (tree item) in the tour map viewer
 */
public class TourMapItemComparedTour extends TreeViewerTourItem {

	/**
	 * unique id for the {@link TourCompared} entity
	 */
	private long	fCompareId;

	/**
	 * 
	 */
	private long	fRefId		= -1;

	private int		fStartIndex	= -1;
	private int		fEndIndex	= -1;

	private Date	fTourDate;
	private float	fTourSpeed;

	/**
	 * @param parentItem
	 * @param tourDate
	 * @param tourSpeed
	 * @param compareId
	 * @param compTourId
	 * @param startIndex
	 * @param endIndex
	 * @param refId
	 */
	public TourMapItemComparedTour(TourMapItemYear parentItem, Date tourDate, float tourSpeed,
			long compareId, long compTourId, int startIndex, int endIndex, long refId) {

		setParentItem(parentItem);

		fRefId = refId;
		fCompareId = compareId;

		fTourDate = tourDate;
		fTourSpeed = tourSpeed;

		fStartIndex = startIndex;
		fEndIndex = endIndex;

		setTourId(compTourId);
	}

	@Override
	public boolean hasChildren() {
		/*
		 * compared tours do not have children
		 */
		return false;
	}

	@Override
	public void remove() {

		// remove this tour item from the parent
		getParentItem().getUnfetchedChildren().remove(this);
	}

	/**
	 * @return Returns the Id for {@link TourCompared} entity
	 */
	public long getCompId() {
		return fCompareId;
	}

	public Date getTourDate() {
		return fTourDate;
	}

	public float getTourSpeed() {
		return fTourSpeed;
	}

	public int getEndIndex() {
		return fEndIndex;
	}

	public int getStartIndex() {
		return fStartIndex;
	}

	@Override
	protected void fetchChildren() {}

	public long getRefId() {
		return fRefId;
	}

	void setEndIndex(int endIndex) {
		this.fEndIndex = endIndex;
	}

	void setStartIndex(int startIndex) {
		this.fStartIndex = startIndex;
	}

	void setTourSpeed(float tourSpeed) {
		this.fTourSpeed = tourSpeed;
	}
}
