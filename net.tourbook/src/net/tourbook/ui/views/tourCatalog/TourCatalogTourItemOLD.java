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


public class TourCatalogTourItemOLD extends Object {

//	static final int					ITEM_TYPE_ROOT			= 0;
//	static final int					ITEM_TYPE_YEAR			= 20;
//	static final int					ITEM_TYPE_MONTH			= 30;
//	static final int					ITEM_TYPE_TOUR			= 40;
//
//	private TourCatalogTourItemOLD				parent					= null;
//	private ArrayList<TourCatalogTourItemOLD>	children				= null;
//
////	long[]								fTourItemData;
//	private long						dateValue;
//	private int							fItemType;
//
//	private static final int			CHILDREN_STATUS_NOT_SET	= 0;
//	private static final int			CHILDREN_STATUS_IS_LEAF	= 1;
//	private static final int			CHILDREN_STATUS_FETCHED	= 2;
//
//	private int							childrenStatus			= CHILDREN_STATUS_NOT_SET;
//
//	/**
//	 * when the type for the tour item is <code>ITEM_TYPE_TOUR</code>, this
//	 * field contains the tourId for the tour
//	 */
//	private long						fTourId;
//
//	public TourCatalogTourItem(int itemType, long[] itemData, Long tourTypeId) {
//
//		fItemType = itemType;
//		fTourItemData = itemData;
//
//		// the date value can be year, month or day
//		if ((itemType == ITEM_TYPE_YEAR || itemType == ITEM_TYPE_MONTH || itemType == ITEM_TYPE_TOUR)
//				&& itemData.length > 0) {
//			this.dateValue = itemData[0];
//		}
//
//		// get the tour id from the last column
//		if (itemType == ITEM_TYPE_TOUR) {
//			fTourId = itemData[itemData.length - 1];
//		}
//
//	}
//
//	public TourCatalogTourItem(int itemType, long[] itemData) {
//		this.fItemType = itemType;
//		this.fTourItemData = itemData;
//
//		// the date value can be year, month or day
//		if ((itemType == ITEM_TYPE_YEAR || itemType == ITEM_TYPE_MONTH || itemType == ITEM_TYPE_TOUR)
//				&& itemData.length > 0) {
//			this.dateValue = itemData[0];
//		}
//
//		// get the tour id from the last column
//		if (itemType == ITEM_TYPE_TOUR) {
//			fTourId = itemData[itemData.length - 1];
//		}
//	}
//
//	public boolean hasChildren() {
//
//		if (childrenStatus == CHILDREN_STATUS_NOT_SET) {
//			/*
//			 * if the children have not yet been retrieved we assume that
//			 * children can be available to make the tree node expandable
//			 */
//			return true;
//
//		} else if (childrenStatus == CHILDREN_STATUS_FETCHED) {
//			return children.size() > 0;
//
//		} else {
//			return false;
//		}
//
//	}
//
//	public TourCatalogTourItem[] getChildren() {
//		if (children == null) {
//			return new TourCatalogTourItem[0];
//		}
//		return children.toArray(new TourCatalogTourItem[children.size()]);
//	}
//
//	public ArrayList<TourCatalogTourItem> getChildrenList() {
//		if (children == null) {
//			return new ArrayList<TourCatalogTourItem>();
//		}
//		return children;
//	}
//
//	public TourCatalogTourItem getParent() {
//		return parent;
//	}
//
//	void setParent(TourCatalogTourItem parent) {
//		this.parent = parent;
//	}
//
//	public boolean hasChildrenBeenFetched() {
//
//		return childrenStatus == CHILDREN_STATUS_FETCHED
//				|| childrenStatus == CHILDREN_STATUS_IS_LEAF;
//	}
//
//	public int getItemType() {
//		return fItemType;
//	}
//
//	public void setItemType(int type) {
//		this.fItemType = type;
//	}
//
//	public void addChild(TourCatalogTourItem child) {
//		if (children == null) {
//			children = new ArrayList<TourCatalogTourItem>();
//			childrenStatus = CHILDREN_STATUS_FETCHED;
//		}
//		children.add(child);
//		child.setParent(this);
//	}
//
//	/**
//	 * @return Returns the childrenStatus.
//	 */
//	public int getChildrenStatus() {
//		return childrenStatus;
//	}
//
//	/**
//	 * @return Returns the dateValue.
//	 */
//	public long getDateValue() {
//		return dateValue;
//	}
//
//	public long getTourId() {
//		return fTourId;
//	}
//
//	/**
//	 * set this touritem to a leaf, this indicates no children are available
//	 */
//	public void setLeaf() {
//		childrenStatus = CHILDREN_STATUS_IS_LEAF;
//	}
}
