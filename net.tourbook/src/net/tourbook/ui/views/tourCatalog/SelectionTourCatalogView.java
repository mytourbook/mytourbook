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

import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;

import org.eclipse.jface.viewers.ISelection;

/**
 * Selection contains data for a compared tour from the {@link TourCatalogView}
 */
public class SelectionTourCatalogView implements ISelection {

	/**
	 * Unique id for a reference tour in {@link TourReference} entity
	 */
	private Long					fRefId;

	/**
	 * unique id for {@link TourCompared} entity or <code>-1</code> when the compared tour is not
	 * saved in the database
	 */
	private Long					fCompTourId;

	private TVICatalogReferenceTour	fRefItem;
	private TVICatalogYearItem		fYearItem;

	public SelectionTourCatalogView(final Long refId) {
		fRefId = refId;
	}

	public SelectionTourCatalogView(final TVICatalogReferenceTour refItem) {
		fRefId = refItem.refId;
		fRefItem = refItem;
	}

	public SelectionTourCatalogView(final TVICatalogYearItem yearItem) {
		fRefId = yearItem.refId;
		fYearItem = yearItem;
	}

	/**
	 * @return Returns the tour Id of the {@link TourData} for the compared tour or
	 *         <code>null</code> when it's not set
	 */
	public Long getCompTourId() {
		return fCompTourId;
	}

	public Long getRefId() {
		return fRefId;
	}

	/**
	 * @return Returns the item {@link TVICatalogReferenceTour} or <code>null</code> when it's not
	 *         set
	 */
	public TVICatalogReferenceTour getRefItem() {
		return fRefItem;
	}

	/**
	 * @return Returns the item {@link TVICatalogYearItem} or <code>null</code> when it's not set
	 */
	public TVICatalogYearItem getYearItem() {
		return fYearItem;
	}

	public boolean isEmpty() {
		return false;
	}

//	public void setRefItem(final TVICatalogReferenceTour refItem) {
//		fRefItem = refItem;
//	}
//
//	public void setYearData(final TVICatalogYearItem yearItem) {
//		fYearItem = yearItem;
//	}

}
