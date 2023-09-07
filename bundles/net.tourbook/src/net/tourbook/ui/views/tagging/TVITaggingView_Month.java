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
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVITaggingView_Month extends TVITaggingView_Item {

   private final TVITaggingView_Year _yearItem;

   private final int                 _year;
   private final int                 _month;

   public TVITaggingView_Month(final TVITaggingView_Year parentItem,
                               final int dbYear,
                               final int dbMonth,
                               final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(parentItem);

      _yearItem = parentItem;
      _year = dbYear;
      _month = dbMonth;
   }

   /**
    * Compare two instances of {@link TVITaggingView_Month}
    *
    * @param otherMonthItem
    * @return
    */
   public int compareTo(final TVITaggingView_Month otherMonthItem) {

      if (this == otherMonthItem) {
         return 0;
      }

      if (_year < otherMonthItem._year) {

         return -1;

      } else if (_year > otherMonthItem._year) {

         return 1;

      } else {

         // same year, check month

         if (_month == otherMonthItem._month) {
            return 0;
         } else if (_month < otherMonthItem._month) {
            return -1;
         } else {
            return 1;
         }
      }
   }

   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (getClass() != obj.getClass()) {
         return false;
      }

      final TVITaggingView_Month other = (TVITaggingView_Month) obj;

      if (_month != other._month) {
         return false;
      }

      if (_year != other._year) {
         return false;
      }

      if (_yearItem == null) {

         if (other._yearItem != null) {
            return false;
         }

      } else if (!_yearItem.equals(other._yearItem)) {

         return false;
      }

      return true;
   }

   @Override
   protected void fetchChildren() {

      /*
       * Set tour children for the month
       */
      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * Get all tours for the current month
          */
         final SQLFilter sqlFilter = new SQLFilter();

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                 //$NON-NLS-1$

               + " tourID," + NL //                //				1	//$NON-NLS-1$
               + " jTdataTtag2.TourTag_tagId," + NL //         2  //$NON-NLS-1$

               + TVITaggingView_Tour.SQL_TOUR_COLUMNS + NL //  3

               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //                  //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag and year/month
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //                      //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jTdataTtag.TourData_tourId=TourData.tourId " + NL //                                     //$NON-NLS-1$

               // get all tag id's for one tour
               + " LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag2" + NL //      //$NON-NLS-1$ //$NON-NLS-2$
               + " ON TourData.tourID = jTdataTtag2.TourData_tourId" + NL //                                   //$NON-NLS-1$

               + " WHERE jTdataTtag.TourTag_TagId=?" + NL //                                                   //$NON-NLS-1$
               + " AND startYear=?" + NL //                                                                    //$NON-NLS-1$
               + " AND startMonth=?" + NL //                                                                   //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL

               + " ORDER BY startDay, startHour, startMinute" + NL //                                          //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, _yearItem.getTagId());
         statement.setInt(2, _year);
         statement.setInt(3, _month);
         sqlFilter.setParameters(statement, 4);

         long lastTourId = -1;
         TVITaggingView_Tour tourItem = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);
            final Object resultTagId = result.getObject(2);

            if (tourId == lastTourId) {

               // get tags from outer join

               if (resultTagId instanceof Long) {
                  tourItem.tagIds.add((Long) resultTagId);
               }

            } else {

               // new tour is in the resultset
               tourItem = new TVITaggingView_Tour(this, getTagViewer());

               children.add(tourItem);

               tourItem.tourId = tourId;
               tourItem.getTourColumnData(result, resultTagId, 3);

               tourItem.firstColumn = Integer.toString(tourItem.tourDay);

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
               }

            }

            lastTourId = tourId;
         }
      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   public int getMonth() {
      return _month;
   }

   public TVITaggingView_Year getYearItem() {
      return _yearItem;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + _month;
      result = prime * result + _year;
      result = prime * result + ((_yearItem == null) ? 0 : _yearItem.hashCode());
      return result;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_Month " + System.identityHashCode(this) + NL //     //$NON-NLS-1$

            + "[" + NL //                                                     //$NON-NLS-1$

            + "  _year         = " + _year + NL //                            //$NON-NLS-1$
            + "  _month        = " + _month + NL //                           //$NON-NLS-1$
            + "  _yearItem     = " + _yearItem + NL //                        //$NON-NLS-1$

            + "  numTours          = " + numTours + NL //                     //$NON-NLS-1$
            + "  numTags_NoTours   = " + numTags_NoTours + NL //              //$NON-NLS-1$

            + "]" + NL //                                                     //$NON-NLS-1$
      ;
   }
}
