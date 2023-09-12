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
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVITaggingView_Year extends TVITaggingView_Item {

   private final int          _year;

   private TVITaggingView_Tag _tagItem;

   /**
    * <code>true</code> when the children of this year item contains month items<br>
    * <code>false</code> when the children of this year item contains tour items
    */
   private boolean            _isMonth;

   public TVITaggingView_Year(final TVITaggingView_Tag parentItem,
                              final int year,
                              final boolean isMonth,
                              final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(parentItem);

      _tagItem = parentItem;
      _year = year;
      _isMonth = isMonth;
   }

   /**
    * Compare two instances of {@link TVITaggingView_Year}
    *
    * @param otherYearItem
    * @return
    */
   public int compareTo(final TVITaggingView_Year otherYearItem) {

      if (this == otherYearItem) {
         return 0;
      }

      if (_year == otherYearItem._year) {
         return 0;
      } else if (_year < otherYearItem._year) {
         return -1;
      } else {
         return 1;
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

      final TVITaggingView_Year other = (TVITaggingView_Year) obj;

      if (_isMonth != other._isMonth) {
         return false;
      }

      if (_year != other._year) {
         return false;
      }

      if (_tagItem == null) {
         if (other._tagItem != null) {
            return false;
         }
      } else if (!_tagItem.equals(other._tagItem)) {
         return false;
      }

      return true;
   }

   @Override
   protected void fetchChildren() {

      if (_isMonth) {
         setChildren(readYearChildren_Months());
      } else {
         setChildren(readYearChildren_Tours());
      }
   }

   public long getTagId() {
      return _tagItem.getTagId();
   }

   public TVITaggingView_Tag getTagItem() {
      return _tagItem;
   }

   public int getYear() {
      return _year;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (_isMonth ? 1231 : 1237);
      result = prime * result + ((_tagItem == null) ? 0 : _tagItem.hashCode());
      result = prime * result + _year;
      return result;
   }

   private ArrayList<TreeViewerItem> readYearChildren_Months() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * Get all tours for the tag ID of this tree item
          */

         final SQLFilter sqlFilter = new SQLFilter();

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //               //$NON-NLS-1$
               + " startYear," + NL //       1  //$NON-NLS-1$
               + " startMonth," + NL //      2  //$NON-NLS-1$

               + SQL_SUM_COLUMNS

               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //   //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag and year
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //       //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jTdataTtag.TourData_tourId=TourData.tourId " + NL //                      //$NON-NLS-1$

               + " WHERE jTdataTtag.TourTag_TagId=?" + NL //                                    //$NON-NLS-1$
               + " AND startYear=?" + NL //                                                     //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL

               + " GROUP BY startYear, startMonth" + NL //                                      //$NON-NLS-1$
               + " ORDER BY startYear" + NL //                                                  //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, _tagItem.getTagId());
         statement.setInt(2, _year);
         sqlFilter.setParameters(statement, 3);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final int dbYear = result.getInt(1);
            final int dbMonth = result.getInt(2);

            final TVITaggingView_Month tourItem = new TVITaggingView_Month(this, dbYear, dbMonth, getTagViewer());
            children.add(tourItem);

            tourItem.firstColumn = LocalDate.of(dbYear, dbMonth, 1).format(TimeTools.Formatter_Month);

            tourItem.readSumColumnData(result, 3);

            if (UI.IS_SCRAMBLE_DATA) {
               tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
            }
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return children;
   }

   private ArrayList<TreeViewerItem> readYearChildren_Tours() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * Get all tours for the tag Id of this tree item
          */

         final SQLFilter sqlFilter = new SQLFilter();

         final String sql = UI.EMPTY_STRING

               + "SELECT" + NL //                              //$NON-NLS-1$

               + " tourID," + NL //                         1  //$NON-NLS-1$
               + " jTdataTtag2.TourTag_tagId," + NL //      2  //$NON-NLS-1$
               + TVITaggingView_Tour.SQL_TOUR_COLUMNS + NL //   3

               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //               //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag and year/month
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //                   //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jTdataTtag.TourData_tourId=TourData.tourId " + NL //                                  //$NON-NLS-1$

               // get all tag id's for one tour
               + " LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag2" + NL //   //$NON-NLS-1$ //$NON-NLS-2$
               + " ON TourData.tourID = jTdataTtag2.TourData_tourId" + NL //                                //$NON-NLS-1$

               + " WHERE jTdataTtag.TourTag_TagId=?" + NL //                                                //$NON-NLS-1$
               + " AND startYear=?" + NL //                                                                 //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL

               + " ORDER BY startMonth, startDay, startHour, startMinute" + NL //                           //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);

         statement.setLong(1, _tagItem.getTagId());
         statement.setInt(2, _year);
         sqlFilter.setParameters(statement, 3);

         long lastTourId = -1;
         TVITaggingView_Tour tourItem = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);
            final Object resultTagId = result.getObject(2);

            if (tourId == lastTourId) {

               // get tags from outer join for the current tour id

               if (resultTagId instanceof Long) {
                  tourItem.tagIds.add((Long) resultTagId);
               }

            } else {

               // resultset contains a new tour

               tourItem = new TVITaggingView_Tour(this, getTagViewer());

               children.add(tourItem);

               tourItem.tourId = tourId;
               tourItem.getTourColumnData(result, resultTagId, 3);

               tourItem.firstColumn = tourItem.tourDate.format(TimeTools.Formatter_Date_S);

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
               }

            }

            lastTourId = tourId;
         }
      } catch (final SQLException e) {

         UI.showSQLException(e);
      }

      return children;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_Year " + System.identityHashCode(this) + NL //   //$NON-NLS-1$

            + " [" + NL //                                                 //$NON-NLS-1$

            + "  _year        = " + _year + NL //                          //$NON-NLS-1$
            + "  _isMonth     = " + _isMonth + NL //                       //$NON-NLS-1$

            + NL
            + "  numTours          = " + numTours + NL //                  //$NON-NLS-1$
            + "  numTags_NoTours   = " + numTags_NoTours + NL //           //$NON-NLS-1$

//          + NL
//          + "_tagItem  = " + _tagItem + NL //                            //$NON-NLS-1$

            + "]" + NL //                                                  //$NON-NLS-1$
      ;
   }
}
