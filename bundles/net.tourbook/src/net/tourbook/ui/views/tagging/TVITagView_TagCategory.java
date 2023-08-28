/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;

public class TVITagView_TagCategory extends TVITagViewItem {

   long   tagCategoryId;
   String name;

   public TVITagView_TagCategory(final TVITagViewItem parentItem) {
      setParentItem(parentItem);
   }

   @Override
   protected void fetchChildren() {

      // create child items for this tag category item
      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * get all tags for the current tag category
          */

         final String tblTag = TourDatabase.TABLE_TOUR_TAG;
         final String tblCat = TourDatabase.TABLE_TOUR_TAG_CATEGORY;
         final String jTblCatTag = TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAG;
         final String jTblCatCat = TourDatabase.JOINTABLE__TOURTAGCATEGORY_TOURTAGCATEGORY;

         /*
          * Get tag categories
          */
         String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                 //$NON-NLS-1$

               + " tblCat.tagCategoryId," + NL //              1  //$NON-NLS-1$
               + " tblCat.name" + NL //                        2  //$NON-NLS-1$

               + " FROM " + jTblCatCat + " jTblCatCat" + NL //    //$NON-NLS-1$ //$NON-NLS-2$

               + " LEFT OUTER JOIN " + tblCat + " tblCat ON " + NL //                              //$NON-NLS-1$ //$NON-NLS-2$
               + " jTblCatCat.TourTagCategory_tagCategoryId2 = tblCat.tagCategoryId " + NL //      //$NON-NLS-1$

               + " WHERE jTblCatCat.TourTagCategory_tagCategoryId1 = ?" + NL //                    //$NON-NLS-1$
               + " ORDER BY tblCat.name" + NL //                                                   //$NON-NLS-1$
         ;

         PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, tagCategoryId);

         ResultSet result = statement.executeQuery();
         while (result.next()) {

            final TVITagView_TagCategory treeItem = new TVITagView_TagCategory(this);
            children.add(treeItem);

            treeItem.tagCategoryId = result.getLong(1);
            treeItem.treeColumn = treeItem.name = result.getString(2);

            if (UI.IS_SCRAMBLE_DATA) {
               treeItem.treeColumn = treeItem.name = UI.scrambleText(treeItem.name);
            }
         }

         /*
          * Get tags
          */
         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                        //$NON-NLS-1$

               + " tblTag.tagId," + NL //             1  //$NON-NLS-1$
               + " tblTag.name," + NL //              2  //$NON-NLS-1$
               + " tblTag.expandType" + NL //         3  //$NON-NLS-1$

               + " FROM " + jTblCatTag + " jTblCatTag" + NL //                   //$NON-NLS-1$ //$NON-NLS-2$

               // get all tags for the category
               + " LEFT OUTER JOIN " + tblTag + " tblTag ON" + NL //             //$NON-NLS-1$ //$NON-NLS-2$
               + " jTblCatTag.TourTag_TagId = tblTag.tagId" + NL //              //$NON-NLS-1$

               + " WHERE jTblCatTag.TourTagCategory_TagCategoryId = ?" + NL //   //$NON-NLS-1$
               + " ORDER BY tblTag.name" + NL //                                 //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);
         statement.setLong(1, tagCategoryId);

         result = statement.executeQuery();
         while (result.next()) {

            final TVITagView_Tag tagItem = new TVITagView_Tag(this);
            children.add(tagItem);

            final long tagId = result.getLong(1);
            final int expandType = result.getInt(3);

            tagItem.tagId = tagId;
            tagItem.treeColumn = tagItem.name = result.getString(2);
            tagItem.setExpandType(expandType);

            if (UI.IS_SCRAMBLE_DATA) {
               tagItem.treeColumn = tagItem.name = UI.scrambleText(tagItem.name);
            }

            readTagTotals(tagItem);
         }

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }
   }

   public long getCategoryId() {
      return tagCategoryId;
   }

   public String getName() {
      return name;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_TagCategory " + System.identityHashCode(this) + NL //       //$NON-NLS-1$

            + "[" + NL //                                     //$NON-NLS-1$
            + "tagCategoryId = " + tagCategoryId + NL //      //$NON-NLS-1$
            + "name          = " + name + NL //               //$NON-NLS-1$

            + "]" + NL //                                     //$NON-NLS-1$
      ;
   }
}
