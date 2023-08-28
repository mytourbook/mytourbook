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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;

/**
 * root item for the tag view
 */
public class TVITagView_Root extends TVITagViewItem {

   private int _tagViewStructure;

   public TVITagView_Root(final int tagViewStructure) {
      _tagViewStructure = tagViewStructure;
   }

   @Override
   protected void fetchChildren() {

      /*
       * set the children for the root item, these are year items
       */
      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         PreparedStatement statement;
         ResultSet result;

         String sql;

         if (_tagViewStructure == TaggingView.TAG_VIEW_LAYOUT_HIERARCHICAL) {

            /*
             * Get tag categories
             */
            sql = UI.EMPTY_STRING

                  + "SELECT" + NL //                     //$NON-NLS-1$

                  + " tagCategoryId," + NL //          1 //$NON-NLS-1$
                  + " name" + NL //                    2 //$NON-NLS-1$

                  + " FROM " + TourDatabase.TABLE_TOUR_TAG_CATEGORY + NL //$NON-NLS-1$
                  + " WHERE isRoot = 1" + NL //          //$NON-NLS-1$
                  + " ORDER BY name" + NL //             //$NON-NLS-1$
            ;

            statement = conn.prepareStatement(sql);
            result = statement.executeQuery();

            while (result.next()) {

               final TVITagView_TagCategory treeItem = new TVITagView_TagCategory(this);
               children.add(treeItem);

               treeItem.tagCategoryId = result.getLong(1);
               treeItem.treeColumn = treeItem.name = result.getString(2);

               if (UI.IS_SCRAMBLE_DATA) {
                  treeItem.treeColumn = treeItem.name = UI.scrambleText(treeItem.name);
               }
            }
         }

         /*
          * Get tags
          */
         final String sqlWhere = _tagViewStructure == TaggingView.TAG_VIEW_LAYOUT_FLAT

               ? UI.EMPTY_STRING
               : _tagViewStructure == TaggingView.TAG_VIEW_LAYOUT_HIERARCHICAL

                     ? " WHERE isRoot = 1" + NL //                //$NON-NLS-1$
                     : UI.EMPTY_STRING;

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                 //$NON-NLS-1$

               + " tagId," + NL //                             1  //$NON-NLS-1$
               + " name," + NL //                              2  //$NON-NLS-1$
               + " expandType," + NL //                        3  //$NON-NLS-1$
               + " isRoot" + NL //                             4  //$NON-NLS-1$

               + " FROM " + TourDatabase.TABLE_TOUR_TAG + NL //   //$NON-NLS-1$
               + sqlWhere
               + " ORDER BY name" + NL //                         //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);
         result = statement.executeQuery();

         while (result.next()) {

            final TVITagView_Tag tagItem = new TVITagView_Tag(this);

            children.add(tagItem);

            final long tagId = result.getLong(1);

            tagItem.tagId = tagId;
            tagItem.treeColumn = tagItem.name = result.getString(2);
            tagItem.setExpandType(result.getInt(3));
            tagItem.isRoot = result.getInt(4) == 1;

            if (UI.IS_SCRAMBLE_DATA) {
               tagItem.treeColumn = tagItem.name = UI.scrambleText(tagItem.name);
            }

            readTagTotals(tagItem);
         }

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }
   }

}
