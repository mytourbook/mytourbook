/*******************************************************************************
 * Copyright (C) 2006, 2007  Wolfgang Schramm
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
package net.tourbook.views.tourMap;

import java.sql.Date;

import net.tourbook.tour.TreeViewerTourItem;

/**
 * Represents a compared tour (tree item) in the tour map viewer
 */
public class TVTITourMapComparedTour extends TreeViewerTourItem {

	/**
	 * id for the TourCompared entity
	 */
	private long	compId;

	/**
	 * 
	 */
	private long	refId		= -1;

	private int		startIndex	= -1;
	private int		endIndex	= -1;

	private Date	tourDate;
	private float	tourSpeed;

	/**
	 * @param parentItem
	 * @param tourDate
	 * @param tourSpeed
	 * @param compId
	 * @param compId
	 * @param startIndex
	 * @param endIndex
	 * @param refId
	 */
	public TVTITourMapComparedTour(TVITourMapYear parentItem, Date tourDate,
			float tourSpeed, long compId, long compTourId, int startIndex, int endIndex,
			long refId) {

		this.setParentItem(parentItem);
		this.refId = refId;

		this.tourDate = tourDate;
		this.tourSpeed = tourSpeed;
		this.compId = compId;
		this.startIndex = startIndex;
		this.endIndex = endIndex;

		setTourId(compTourId);
	}

	public boolean hasChildren() {
		/*
		 * compared tours do not have children
		 */
		return false;
	}

	public void remove() {

		// remove this tour item from the parent
		getParentItem().getUnfetchedChildren().remove(this);
	}

	public long getCompId() {
		return compId;
	}

	public Date getTourDate() {
		return tourDate;
	}

	public float getTourSpeed() {
		return tourSpeed;
	}

	public int getEndIndex() {
		return endIndex;
	}

	public int getStartIndex() {
		return startIndex;
	}

	protected void fetchChildren() {}

	public long getRefId() {
		return refId;
	}

	void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	void setTourSpeed(float tourSpeed) {
		this.tourSpeed = tourSpeed;
	}
}
