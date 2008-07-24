/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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

import org.eclipse.jface.viewers.ISelection;

/**
 * This selection contains data for a compared tour from the view
 * {@link TourCatalogViewComparedTour}
 */
public class SelectionCompareResultView implements ISelection {

//	/**
//	 * Id of the reference tour
//	 */
//	private Long	fRefId;
//
//	/**
//	 * unique id for {@link TourCompared} entity or <code>-1</code> when the compared tour is not
//	 * saved in the database
//	 */
//	private Long	fCompareId;
//	private Long	fCompTourId;
//
//	private int		fCompareStartIndex;
//	private int		fCompareEndIndex;
//
//	/*
//	 * all fDb... fields contains the data when a compared Tour is saved in the database
//	 */
//	private int		fDbStartIndex;
//	private int		fDbEndIndex;
//	private float	fDbSpeed;
//
//	public SelectionCompareResultView(Long refId) {
//		fRefId = refId;
//	}
//
//	public int getCompareEndIndex() {
//		return fCompareEndIndex;
//	}
//
//	/**
//	 * @return Returns the key for the {@link TourCompared} instance or <code>null</code> when
//	 *         it's not set
//	 */
//	public Long getCompareId() {
//		return fCompareId;
//	}
//
//	public int getCompareStartIndex() {
//		return fCompareStartIndex;
//	}
//
//	/**
//	 * @return Returns the tour Id of the {@link TourData} for the compared tour or
//	 *         <code>null</code> when it's not set
//	 */
//	public Long getCompTourId() {
//		return fCompTourId;
//	}
//
//	int getDbEndIndex() {
//		return fDbEndIndex;
//	}
//
//	float getDbSpeed() {
//		return fDbSpeed;
//	}
//
//	int getDbStartIndex() {
//		return fDbStartIndex;
//	}
//
//	public Long getRefId() {
//		return fRefId;
//	}
//
	public boolean isEmpty() {
		return false;
	}
//
//	/**
//	 * Set data for the compared tour
//	 * 
//	 * @param compareId
//	 *        database Id for the compared tour
//	 * @param compTourId
//	 *        database Id for the compared tour data
//	 * @param compStartIndex
//	 *        start index of the x-marker
//	 * @param compEndIndex
//	 *        end index of the x-marker
//	 */
//	public void setTourCompareData(	long compareId,
//									long compTourId,
//									int compStartIndex,
//									int compEndIndex) {
//
//		fCompareId = compareId;
//		fCompTourId = compTourId;
//
//		fCompareStartIndex = compStartIndex;
//		fCompareEndIndex = compEndIndex;
//	}
//
//	public void setTourCompareDbData(int dbStartIndex, int dbEndIndex, float dbSpeed) {
//		fDbStartIndex = dbStartIndex;
//		fDbEndIndex = dbEndIndex;
//		fDbSpeed = dbSpeed;
//	}

}
