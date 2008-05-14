/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

package net.tourbook.tour;

import java.util.ArrayList;

/**
 * Abstract class which contains an item for a tree viewer
 */
public abstract class TreeViewerItem {

	private TreeViewerItem				fParentItem	= null;
	private ArrayList<TreeViewerItem>	fChildren	= null;

	protected abstract void fetchChildren();

	/**
	 * @return Returns a list with all childrens for this item, when children have not been fetched,
	 * 	an empty list will be returned
	 */
	public ArrayList<TreeViewerItem> getChildren() {
		if (fChildren == null) {
			return new ArrayList<TreeViewerItem>();
		}
		return fChildren;
	}

	/**
	 * @return Returns a list with all fetched children
	 */
	public ArrayList<TreeViewerItem> getFetchedChildren() {

		if (fChildren == null) {
			fetchChildren();
		}

		if (fChildren == null) {
			fChildren = new ArrayList<TreeViewerItem>();
		}

		return fChildren;
	}

	/**
	 * @return Returns an array with all fetched children
	 */
	public Object[] getFetchedChildrenAsArray() {

		if (fChildren == null) {
			fetchChildren();
		}
		if (fChildren == null) {
			return new Object[0];
		}

		return fChildren.toArray();
	}

	public TreeViewerItem getParentItem() {
		return fParentItem;
	}

	/**
	 * @return Returns a list with all childrens of this item, when the childrens have not been
	 * 	fetched, <code>null</code> will be returned
	 */
	public ArrayList<TreeViewerItem> getUnfetchedChildren() {
		return fChildren;
	}

	public boolean hasChildren() {

		if (fChildren == null) {
			/*
			 * if fChildren have not yet been retrieved we assume that fChildren can be available to
			 * make the tree node expandable
			 */
			return true;
		} else {
			return fChildren.size() > 0;
		}
	}

	protected abstract void remove();

	public void setChildren(final ArrayList<TreeViewerItem> children) {
		fChildren = children;
	}

	public void setParentItem(final TreeViewerItem parentItem) {
		fParentItem = parentItem;
	}

}
