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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.UI;

/**
 * Contains a reference tour
 */
public class TVICatalogRefTourItem extends TVICatalogTourItem {

   String      label;
   long        refId;

   float[]     avgAltimeter_MinMax = new float[] { Float.MIN_VALUE, Float.MAX_VALUE };
   float[]     avgPulse_MinMax     = new float[] { Float.MIN_VALUE, Float.MAX_VALUE };
   float[]     maxPulse_MinMax     = new float[] { Float.MIN_VALUE, Float.MAX_VALUE };
   float[]     avgSpeed_MinMax     = new float[] { Float.MIN_VALUE, Float.MAX_VALUE };

   /**
    * Number of tours
    */
   int         numTours;

   private int _viewLayout;

   public TVICatalogRefTourItem(final TVICatalogRootItem parentItem, final int viewLayout) {

      _viewLayout = viewLayout;

      setParentItem(parentItem);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TVICatalogRefTourItem)) {
         return false;
      }
      final TVICatalogRefTourItem other = (TVICatalogRefTourItem) obj;
      if (refId != other.refId) {
         return false;
      }
      return true;
   }

   @Override
   protected void fetchChildren() {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();
      setChildren(children);

      if (_viewLayout == TourCompareManager.REF_TOUR_VIEW_LAYOUT_WITHOUT_YEAR_CATEGORIES) {

         // fetch compared tour items

         fetchComparedTours(this, children, -1);

      } else {

         // fetch year items

         fetchChildren_WithYearCategories(children);
      }
   }

   private void fetchChildren_WithYearCategories(final ArrayList<TreeViewerItem> children) {

      /**
       * Derby does not support expression in "GROUP BY" statements, this is a workaround found
       * here: http://mail-archives.apache.org/mod_mbox/db-derby-dev/200605.mbox/%3C7415300
       * .1147889647479.JavaMail.jira@brutus%3E <br>
       * <code>
       *    String subSQLString = "(SELECT YEAR(tourDate)\n"
       *       + ("FROM " + TourDatabase.TABLE_TOUR_COMPARED + "\n")
       *       + (" WHERE "
       *             + TourDatabase.TABLE_TOUR_REFERENCE
       *             + "_generatedId="
       *             + refId + "\n")
       *       + ")";
       *
       *    String sqlString = "SELECT years FROM \n"
       *       + subSQLString
       *       + (" REFYEARS(years) GROUP BY years");
       * </code>
       */

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                       //$NON-NLS-1$

            + " StartYear," + NL //                                  //$NON-NLS-1$
            + " SUM(1)" + NL //                                      //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_COMPARED + NL //    //$NON-NLS-1$
            + " WHERE refTourId=?" + NL //                           //$NON-NLS-1$
            + " GROUP BY startYear" + NL //                          //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, refId);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final TVICatalogYearItem yearItem = new TVICatalogYearItem(this);
            children.add(yearItem);

            yearItem.refId = refId;
            yearItem.year = result.getInt(1);
            yearItem.numTours = result.getInt(2);

            numTours = result.getInt(2);
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   /**
    * @param parentItem
    * @param children
    * @param year
    *           Fetch compared tours for this year or for all years when <code>year == -1</code>
    */
   void fetchComparedTours(final TreeViewerItem parentItem,
                           final ArrayList<TreeViewerItem> children,
                           final int year) {

      final boolean isWithYear = year != -1;

      final String sqlYear = isWithYear

            ? " AND TourCompared.startYear=?" //$NON-NLS-1$
            : UI.EMPTY_STRING;

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                   //$NON-NLS-1$

            + " TourCompared.comparedId," + NL //              1 //$NON-NLS-1$
            + " TourCompared.tourId," + NL //                  2 //$NON-NLS-1$
            + " TourCompared.tourDate," + NL //                3 //$NON-NLS-1$
            + " TourCompared.avgAltimeter," + NL //            4 //$NON-NLS-1$
            + " TourCompared.avgPulse," + NL //                5 //$NON-NLS-1$
            + " TourCompared.maxPulse," + NL //                6 //$NON-NLS-1$
            + " TourCompared.tourSpeed," + NL //               7 //$NON-NLS-1$
            + " TourCompared.startIndex," + NL //              8 //$NON-NLS-1$
            + " TourCompared.endIndex," + NL //                9 //$NON-NLS-1$
            + " TourCompared.tourDeviceTime_Elapsed," + NL // 10 //$NON-NLS-1$

            + " TourData.tourTitle," + NL //                  11 //$NON-NLS-1$
            + " TourData.tourType_typeId," + NL //            12 //$NON-NLS-1$

            + " jTdataTtag.TourTag_tagId" + NL //             13 //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_COMPARED + " TourCompared" + NL //                      //$NON-NLS-1$ //$NON-NLS-2$

            // get data for a tour
            + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData " + NL //                  //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourCompared.tourId = TourData.tourId" + NL //                                        //$NON-NLS-1$

            // get tag id's
            + " LEFT OUTER JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //    //$NON-NLS-1$ //$NON-NLS-2$
            + " ON TourData.tourId = jTdataTtag.TourData_tourId" + NL //                                 //$NON-NLS-1$

            + " WHERE TourCompared.refTourId=?" + sqlYear + NL //                                        //$NON-NLS-1$
            + " ORDER BY TourCompared.tourDate" + NL //                                                  //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, refId);

         if (isWithYear) {
            statement.setInt(2, year);
         }

         final ResultSet result = statement.executeQuery();

         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         while (result.next()) {

// SET_FORMATTING_OFF

            final long tourId          = result.getLong(2);
            final Object resultTagId   = result.getObject(13);

// SET_FORMATTING_ON

            if (tourId == lastTourId) {

               // get tags from outer join

               if (resultTagId instanceof Long) {
                  tagIds.add((Long) resultTagId);
               }

            } else {

               // a new tour is in the resultset

               final TVICatalogComparedTour tourItem = new TVICatalogComparedTour(parentItem);
               children.add(tourItem);

// SET_FORMATTING_OFF

               tourItem.refId = refId;

               // from TourCompared
               tourItem.compareId                  = result.getLong(1);
               tourItem.setTourId(tourId);

               final Date tourDate                 = result.getDate(3);

               tourItem.avgAltimeter               = result.getFloat(4);
               tourItem.avgPulse                   = result.getFloat(5);
               tourItem.maxPulse                   = result.getFloat(6);
               tourItem.avgSpeed                   = result.getFloat(7);

               tourItem.startIndex                 = result.getInt(8);
               tourItem.endIndex                   = result.getInt(9);
               tourItem.tourDeviceTime_Elapsed     = result.getInt(10);

               // from TourData
               tourItem.tourTitle                  = result.getString(11);
               final Object tourTypeId             = result.getObject(12);

// SET_FORMATTING_ON

               // tour date
               if (tourDate != null) {

                  final LocalDate localDate = tourDate.toLocalDate();

                  tourItem.tourDate = localDate;
                  tourItem.year = localDate.getYear();
               }

               // tour type
               tourItem.tourTypeId = tourTypeId == null
                     ? TourDatabase.ENTITY_IS_NOT_SAVED
                     : (Long) tourTypeId;

               // tour tags
               if (resultTagId instanceof Long) {

                  if (tourItem.tagIds != null) {
                     tourItem.tagIds.clear();
                  }

                  tourItem.tagIds = tagIds = new ArrayList<>();
                  tagIds.add((Long) resultTagId);
               }
            }

            lastTourId = tourId;
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (refId ^ (refId >>> 32));
      return result;
   }

   void remove() {

      final ArrayList<TreeViewerItem> unfetchedChildren = getUnfetchedChildren();

      // remove all children
      if (unfetchedChildren != null) {
         unfetchedChildren.clear();
      }

      // remove this ref item from the parent item
      final ArrayList<TreeViewerItem> unfetchedParentChildren = getParentItem().getUnfetchedChildren();
      if (unfetchedParentChildren != null) {
         unfetchedParentChildren.remove(this);
      }
   }

   public void resetMinMaxValues() {

      avgAltimeter_MinMax[0] = Float.MIN_VALUE;
      avgAltimeter_MinMax[1] = Float.MAX_VALUE;

      avgPulse_MinMax[0] = Float.MIN_VALUE;
      avgPulse_MinMax[1] = Float.MAX_VALUE;

      maxPulse_MinMax[0] = Float.MIN_VALUE;
      maxPulse_MinMax[1] = Float.MAX_VALUE;

      avgSpeed_MinMax[0] = Float.MIN_VALUE;
      avgSpeed_MinMax[1] = Float.MAX_VALUE;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVICatalogRefTourItem" + NL //                        //$NON-NLS-1$

            + "[" + NL //                                            //$NON-NLS-1$

            + "label             = " + label + NL //                 //$NON-NLS-1$
            + "refId             = " + refId + NL //                 //$NON-NLS-1$
            + "numTours          = " + numTours + NL //              //$NON-NLS-1$

            + "]" + NL //                                            //$NON-NLS-1$

      ;
   }

}
