/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.common.util;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import net.tourbook.common.UI;

/**
 * Abstract class which contains an item for a tree viewer
 */
public abstract class TreeViewerItem {

   protected static final char       NL                    = net.tourbook.common.UI.NEW_LINE;

   private static final String       SCRAMBLE_FIELD_PREFIX = "col";                          //$NON-NLS-1$

   private TreeViewerItem            _parentItem;

   private ArrayList<TreeViewerItem> _children;

   /**
    * Replace tags in a sql string with an indent <code>\\$i_</code> and the db name
    * <code>\\$db_</code> and cache these values.
    * <p>
    * Also adds a dot separator between the db name and the field name, when a db name is defined
    *
    * @param allCachedSql
    * @param sql
    * @param dbPrefix
    * @param indent
    *
    * @return
    */
   protected static String getCachedSQL(final ConcurrentHashMap<String, String> allCachedSql,
                                        final String sql,
                                        final String dbPrefix,
                                        final int indent) {

      final String key = dbPrefix + UI.SYMBOL_UNDERSCORE + Integer.toString(indent);

      final String cachedSqlFields = allCachedSql.get(key);

      if (cachedSqlFields != null) {
         return cachedSqlFields;
      }

      final StringBuilder sbIndent = new StringBuilder();
      for (int i = 0; i < indent; i++) {
         sbIndent.append(UI.SPACE);
      }

      // add a dot separator between db name and field name
      String dbPrefixReplaced = UI.EMPTY_STRING;
      if (dbPrefix.length() > 0) {
         dbPrefixReplaced = dbPrefix + UI.SYMBOL_DOT;
      }

      String sqlReplaced = sql;

      sqlReplaced = sqlReplaced.replaceAll("\\$i_", sbIndent.toString());
      sqlReplaced = sqlReplaced.replaceAll("\\$db_", dbPrefixReplaced);

      allCachedSql.put(key, sqlReplaced);

      return sqlReplaced;
   }

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
    * Adds a new child before an existing child
    *
    * @param oldItem
    *           Item before the new item is inserted.
    * @param newItem
    */
   public void addChildBefore(final TreeViewerItem oldItem, final TreeViewerItem newItem) {

//      System.out.println(UI.timeStampNano()
//            + " ["
//            + getClass().getSimpleName()
//            + "] \told: "
//            + oldItem
//            + "\tnew: "
//            + newItem);
//      // TODO remove SYSTEM.OUT.PRINTLN

      // set parent for the new child item
      newItem.setParentItem(this);

      // ensure children are created
      getFetchedChildren();

      int oldItemIndex = 0;
      for (final TreeViewerItem childItem : _children) {

         if (oldItem == childItem) {

            _children.add(oldItemIndex, newItem);

            return;
         }

         oldItemIndex++;
      }
   }

   /**
    * Clear children that they will be fetched again the next time when they are displayed
    */
   public void clearChildren() {

      if (_children != null) {

         for (final TreeViewerItem treeViewerItem : _children) {

            treeViewerItem.clearChildren();
         }

         _children.clear();

         _children = null;
      }

      _parentItem = null;
   }

   /**
    * Fetches children for this tree item, children can be added to this tree item with
    * {@link #addChild(TreeViewerItem)}.
    */
   protected abstract void fetchChildren();

   private void fetchChildrenInternal() {

      _children = new ArrayList<>();

      fetchChildren();
   }

   /**
    * @return Returns a list with all children for this item, when children have not been fetched,
    *         an empty list will be returned.
    */
   public ArrayList<TreeViewerItem> getChildren() {

      if (_children == null) {
         return new ArrayList<>();
      }

      return _children;
   }

   /**
    * @return Returns a list with all fetched children, when children are not available, an empty
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

      if (_children.isEmpty()) {
         return new Object[0];
      }

      return _children.toArray();
   }

   public TreeViewerItem getParentItem() {
      return _parentItem;
   }

   /**
    * @return Returns a list with all fetched children of this tree item or <code>null</code> when
    *         children are not yet fetched
    */
   public ArrayList<TreeViewerItem> getUnfetchedChildren() {
      return _children;
   }

   /**
    * @return Returns <code>true</code> when this item has children or when children have not yet
    *         been retrieved.
    */
   public boolean hasChildren() {

      if (_children == null) {
         /**
          * when _children have not yet been retrieved we assume that _children can be available
          * to make the tree node expandable
          */
         return true;
      } else {
         return _children.size() > 0;
      }
   }

   /**
    * Removes a child from this tree item
    *
    * @param treeItem
    *
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
    * Scramble all fields which fieldname is starting with "col"
    *
    * @param fields
    */
   protected void scrambleValues(final Field[] allFields) {

      try {

         for (final Field field : allFields) {

            final String fieldName = field.getName();

            if (fieldName.startsWith(SCRAMBLE_FIELD_PREFIX)) {

               final Type fieldType = field.getGenericType();

               if (Integer.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getInt(this)));

               } else if (Long.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getLong(this)));

               } else if (Float.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getFloat(this)));

               } else if (String.class.equals(fieldType)) {

                  final String fieldValue = (String) field.get(this);
                  final String scrambledText = UI.scrambleText(fieldValue);

                  field.set(this, scrambledText);
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {

         StatusUtil.log(e);
      }
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
