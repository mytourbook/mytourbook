/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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

package de.byteholder.geoclipse.mapprovider;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class which contains an item for a tree viewer
 */
public abstract class TreeViewerItem {

   private TreeViewerItem            _parentItem = null;

   private List<TreeViewerItem> _children   = null;

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

      _children = new ArrayList<>();

      fetchChildren();
   }

   /**
    * @return Returns a list with all childrens for this item, when children have not been fetched,
    *         an empty list will be returned.
    */
   public List<TreeViewerItem> getChildren() {
      if (_children == null) {
         return new ArrayList<>();
      }
      return _children;
   }

   /**
    * @return Returns a list with all fetched children, when children are not yet fetched they will
    *         be fetched now, when childrens are not available, an empty list will be returned.
    */
   public List<TreeViewerItem> getFetchedChildren() {

      if (_children != null) {
         return _children;
      }

      fetchChildrenInternal();

      if (_children == null) {
         _children = new ArrayList<>();
      }

      return _children;
   }

   /**
    * @return Returns an array with all fetched children
    */
   public Object[] getFetchedChildrenAsArray() {

      if (_children == null) {
         fetchChildrenInternal();
      }

      if (_children == null || _children.isEmpty()) {
         return new Object[0];
      }

      return _children.toArray();
   }

   public TreeViewerItem getParentItem() {
      return _parentItem;
   }

   /**
    * @return Returns a list with all fetched childrens of this tree item, returns
    *         <code>null</code> when childrens are not yet fetched
    */
   public List<TreeViewerItem> getUnfetchedChildren() {
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
         return !_children.isEmpty();
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
   public void setChildren(final List<TreeViewerItem> children) {
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
