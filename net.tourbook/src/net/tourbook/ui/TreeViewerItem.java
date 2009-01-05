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

package net.tourbook.ui;

import java.util.ArrayList;

import net.tourbook.data.TourPerson;
import net.tourbook.plugin.TourbookPlugin;

/**
 * Abstract class which contains an item for a tree viewer
 */
public abstract class TreeViewerItem {

	private TreeViewerItem				fParentItem	= null;
	private ArrayList<TreeViewerItem>	fChildren	= null;

	/**
	 * Adds a new child to this tree item
	 * 
	 * @param newTreeItem
	 */
	public void addChild(final TreeViewerItem newTreeItem) {

		// set parent for the new child item
		newTreeItem.setParentItem(this);

		getFetchedChildren().add(newTreeItem);
	}

	/**
	 * clear children so they will be fetched again the next time when they are displayed
	 */
	public void clearChildren() {
		if (fChildren != null) {
			fChildren.clear();
			fChildren = null;
		}
	}

	/**
	 * fetches the children for this tree item, childs can be added to this tree item with
	 * {@link #addChild(TreeViewerItem)}
	 */
	protected abstract void fetchChildren();

	private void fetchChildrenInternal() {

		fChildren = new ArrayList<TreeViewerItem>();

		fetchChildren();
	}

	/**
	 * @return Returns a list with all childrens for this item, when children have not been fetched,
	 *         an empty list will be returned.
	 */
	public ArrayList<TreeViewerItem> getChildren() {
		if (fChildren == null) {
			return new ArrayList<TreeViewerItem>();
		}
		return fChildren;
	}

	/**
	 * @return Returns a list with all fetched children, when childrens are not available, an empty
	 *         list will be returned.
	 */
	public ArrayList<TreeViewerItem> getFetchedChildren() {

		if (fChildren != null) {
			return fChildren;
		}

		fetchChildrenInternal();

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
			fetchChildrenInternal();
		}

		if (fChildren == null || fChildren.size() == 0) {
			return new Object[0];
		}

		return fChildren.toArray();
	}

	public TreeViewerItem getParentItem() {
		return fParentItem;
	}

	/**
	 * @return Returns a sql string for the WHERE clause to select data only for the selected person
	 */
	public String getSQlTourPersonId() {

		final TourPerson fActivePerson = TourbookPlugin.getDefault().getActivePerson();
		final StringBuilder sqlString = new StringBuilder();

		final long personId = fActivePerson == null ? -1 : fActivePerson.getPersonId();
		if (personId == -1) {
			// select all people
		} else {
			// select only one person
			sqlString.append(" AND tourPerson_personId = " + Long.toString(personId)); //$NON-NLS-1$
		}
		return sqlString.toString();
	}

	/**
	 * @return Returns a list with all fetched childrens of this tree item, it will return
	 *         <code>null</code> when childrens are not yet fetched
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

	/**
	 * Removes a child from this tree item
	 * 
	 * @param treeItem
	 * @return Returns <code>true</code> when the child was removed
	 */
	public boolean removeChild(final TreeViewerItem treeItem) {

		final boolean isRemoved = getFetchedChildren().remove(treeItem);

		if (isRemoved) {
			// remove parent from the child
			treeItem.setParentItem(null);
		}

		return isRemoved;
	}

	/**
	 * Set the children for this tree item
	 * 
	 * @param children
	 */
	public void setChildren(final ArrayList<TreeViewerItem> children) {
		if (fChildren != null) {
			fChildren.clear();
		}
		fChildren = children;
	}

	/**
	 * Set the parent for this tree item
	 * 
	 * @param parentItem
	 */
	public void setParentItem(final TreeViewerItem parentItem) {
		fParentItem = parentItem;
	}
}
