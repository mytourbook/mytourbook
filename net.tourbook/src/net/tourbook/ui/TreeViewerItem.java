/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourPerson;

/**
 * Abstract class which contains an item for a tree viewer
 */
public abstract class TreeViewerItem {

	private TreeViewerItem				_parentItem;
	private ArrayList<TreeViewerItem>	_children;

	private boolean						_isChildChildrenFetched;

	/**
	 * Adds a new child to this tree item
	 * 
	 * @param childItem
	 */
	public void addChild(final TreeViewerItem childItem) {

		// set parent for the new child item
		childItem.setParentItem(this);

		getFetchedChildren().add(childItem);
	}

	/**
	 * clear children so they will be fetched again the next time when they are displayed
	 */
	public void clearChildren() {
		if (_children != null) {
			_children.clear();
			_children = null;
		}
	}

	/**
	 * fetches the children for this tree item, childs can be added to this tree item with
	 * {@link #addChild(TreeViewerItem)}
	 */
	protected abstract void fetchChildren();

	private void fetchChildrenInternal() {

		_children = new ArrayList<TreeViewerItem>();

		fetchChildren();
	}

	/**
	 * @return Returns a list with all childrens for this item, when children have not been fetched,
	 *         an empty list will be returned.
	 */
	public ArrayList<TreeViewerItem> getChildren() {
		if (_children == null) {
			return new ArrayList<TreeViewerItem>();
		}
		return _children;
	}

	/**
	 * @return Returns a list with all fetched children, when childrens are not available, an empty
	 *         list will be returned.
	 */
	public ArrayList<TreeViewerItem> getFetchedChildren() {

		if (_children != null) {
			return _children;
		}

		fetchChildrenInternal();

		return _children;
	}

	/**
	 * @return Returns an array with all fetched children
	 */
	public Object[] getFetchedChildrenAsArray() {

		if (_children == null) {
			fetchChildrenInternal();
		}

		if (_children.size() == 0) {
			return new Object[0];
		}

		return _children.toArray();
	}

	public Object[] getFetchedChildrenWithChildrenAsArray() {

		if (_children == null) {
			fetchChildrenInternal();
		}

		if (_isChildChildrenFetched == false) {

			for (final TreeViewerItem child : _children) {
				child.getFetchedChildren();
			}

			_isChildChildrenFetched = true;
		}

		if (_children.size() == 0) {
			return new Object[0];
		}

		return _children.toArray();
	}

	public TreeViewerItem getParentItem() {
		return _parentItem;
	}

	/**
	 * @return Returns a sql string for the WHERE clause to select data only for the selected person
	 */
	public String getSQlTourPersonId() {

		final TourPerson activePerson = TourbookPlugin.getActivePerson();
		final StringBuilder sb = new StringBuilder();

		final long personId = activePerson == null ? -1 : activePerson.getPersonId();
		if (personId == -1) {
			// select all people
		} else {
			// select only one person
			sb.append(" AND tourPerson_personId = " + Long.toString(personId)); //$NON-NLS-1$
		}
		return sb.toString();
	}

	/**
	 * @return Returns a list with all fetched childrens of this tree item, it will return
	 *         <code>null</code> when childrens are not yet fetched
	 */
	public ArrayList<TreeViewerItem> getUnfetchedChildren() {
		return _children;
	}

	public boolean hasChildren() {

		if (_children == null) {
			/*
			 * if fChildren have not yet been retrieved we assume that fChildren can be available to
			 * make the tree node expandable
			 */
			return true;
		} else {
			return _children.size() > 0;
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
		if (_children != null) {
			_children.clear();
		}
		_children = children;
	}

	/**
	 * Set the parent for this tree item
	 * 
	 * @param parentItem
	 */
	public void setParentItem(final TreeViewerItem parentItem) {
		_parentItem = parentItem;
	}
}
