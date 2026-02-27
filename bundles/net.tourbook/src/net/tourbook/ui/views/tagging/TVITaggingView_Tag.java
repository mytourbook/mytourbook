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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourData;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.AppFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVITaggingView_Tag extends TVITaggingView_Item {

   private TourTag _tourTag;

   private long    _tagId;

   public TVITaggingView_Tag(final TourTag tourTag,
                             final TVITaggingView_Item parentItem,
                             final TreeViewer treeViewer) {

      super(treeViewer);

      _tourTag = tourTag;
      _tagId = _tourTag.getTagId();

      setParentItem(parentItem);

      firstColumn = tourTag.getTagName();

      if (UI.IS_SCRAMBLE_DATA) {
         firstColumn = UI.scrambleText(firstColumn);
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
      final TVITaggingView_Tag other = (TVITaggingView_Tag) obj;
      if (_tagId != other._tagId) {
         return false;
      }
      return true;
   }

   @Override
   protected void fetchChildren() {

      switch (_tourTag.getExpandType()) {

      // 0
      case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:
         setChildren(loadTagChildren_Years(true, UI.EMPTY_STRING));
         break;

      // 1
      case TourTag.EXPAND_TYPE_FLAT:
         setChildren(loadTagChildren_Tours(UI.EMPTY_STRING));
         break;

      // 2
      case TourTag.EXPAND_TYPE_YEAR_DAY:
         setChildren(loadTagChildren_Years(false, UI.EMPTY_STRING));
         break;

      default:
         break;
      }

   }

   public int getExpandType() {
      return _tourTag.getExpandType();
   }

   public long getTagId() {
      return _tagId;
   }

   /**
    * @param modifiedTours
    *
    * @return Returns an expression to select tour id's in the WHERE clause
    */
   private String getTourIdWhereClause(final ArrayList<TourData> modifiedTours) {

      if (modifiedTours.isEmpty()) {
         return UI.EMPTY_STRING;
      }

      final StringBuilder sb = new StringBuilder();
      boolean isFirst = true;

      sb.append(" AND TourData.tourId IN ("); //$NON-NLS-1$

      for (final TourData tourData : modifiedTours) {

         if (isFirst) {
            isFirst = false;
         } else {
            sb.append(',');
         }

         sb.append(Long.toString(tourData.getTourId()));
      }

      sb.append(')');

      return sb.toString();
   }

   public TourTag getTourTag() {
      return _tourTag;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (_tagId ^ (_tagId >>> 32));
      return result;
   }

   /**
    * get all tours for the tag Id of this tree item
    */
   private ArrayList<TreeViewerItem> loadTagChildren_Tours(final String whereClause) {

      final ArrayList<TreeViewerItem> children = new ArrayList<>();

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = new AppFilter();

         sql = UI.EMPTY_STRING

               + "--" + NL //                                                                      //$NON-NLS-1$
               + NL
               + "--------------" + NL //                                                          //$NON-NLS-1$
               + "-- tag - tours" + NL //                                                                //$NON-NLS-1$
               + "--------------" + NL //                                                          //$NON-NLS-1$
               + NL

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   TourData.tourId," + NL //                                                  1  //$NON-NLS-1$
               + "   jTdataTtag.TourTag_tagId," + NL //                                         2  //$NON-NLS-1$

               + TVITaggingView_Tour.SQL_TOUR_COLUMNS + NL //                                   3

               + "FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag" + NL //    //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData" //                   //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON jTdataTtag.TourData_tourId = TourData.tourId " + NL //                      //$NON-NLS-1$

               // get all equipment ids
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdataEq" //    //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.TOURID = jTdataEq.TOURDATA_TOURID" + NL //                         //$NON-NLS-1$

               // get all tag ids for one tour
               + "LEFT JOIN " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " AS jTdataTtag_2" //  //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourID = jTdataTtag_2.TourData_tourId" + NL //                     //$NON-NLS-1$

               // get marker ids
               + "LEFT JOIN " + TourDatabase.TABLE_TOUR_MARKER + " AS Tmarker" //                  //$NON-NLS-1$ //$NON-NLS-2$
               + "  ON TourData.tourId = Tmarker.TourData_tourId" + NL //                          //$NON-NLS-1$

               + "WHERE jTdataTtag.TourTag_TagId = ?" + NL //                                      //$NON-NLS-1$
               + whereClause + NL
               + appFilter.getWhereClause() + NL

               + "ORDER BY TourStartTime" + NL //                                                  //$NON-NLS-1$

               + NL;

         long previousTourId = -1;
         TVITaggingView_Tour tourItem = null;

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, _tagId);
         appFilter.setParameters(statement, 2);

         Set<Long> allTagIDs = null;
         Set<Long> allEquipmentIDs = null;

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final long tourId = result.getLong(1);

            final Object dbTagID = result.getObject(11);
            final Object dbEquipmentID = result.getObject(12);

            if (tourId == previousTourId) {

               // get tags from left join
               if (dbTagID instanceof Long) {
                  tourItem.allTagIDs.add((Long) dbTagID);
               }

               // get equipment from left join
               if (dbEquipmentID instanceof final Long equipmentID) {
                  allEquipmentIDs.add(equipmentID);
               }

            } else {

               tourItem = new TVITaggingView_Tour(this, getTagViewer());
               children.add(tourItem);

               tourItem.tourId = tourId;

               tourItem.readTourColumnValues(result, 3);

               tourItem.firstColumn = tourItem.tourDate.format(TimeTools.Formatter_Date_S);

               // get first tag id
               if (dbTagID instanceof Long) {

                  allTagIDs = new HashSet<>();
                  allTagIDs.add((Long) dbTagID);

                  tourItem.allTagIDs = allTagIDs;
               }

               // get first equipment id
               if (dbEquipmentID instanceof final Long equipmentID) {

                  allEquipmentIDs = new HashSet<>();
                  allEquipmentIDs.add(equipmentID);

                  tourItem.allEquipmentIDs = allEquipmentIDs;
               }

               if (UI.IS_SCRAMBLE_DATA) {
                  tourItem.firstColumn = UI.scrambleText(tourItem.firstColumn);
               }
            }

            previousTourId = tourId;
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }
      return children;
   }

   private ArrayList<TreeViewerItem> loadTagChildren_Years(final boolean isMonth, final String whereClause) {

      /*
       * get the children for the tag item
       */
      final ArrayList<TreeViewerItem> children = new ArrayList<>();

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         /*
          * get all tours for the tag Id of this tree item
          */
         final AppFilter sqlFilter = new AppFilter();

         sql = UI.EMPTY_STRING

               + "SELECT " + NL //                 //$NON-NLS-1$

               + " startYear," + NL //          1  //$NON-NLS-1$
               + SQL_SUM_COLUMNS + NL

               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jTdataTtag" + NL //      //$NON-NLS-1$ //$NON-NLS-2$

               // get all tours for current tag
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //          //$NON-NLS-1$ //$NON-NLS-2$
               + " ON jTdataTtag.TourData_tourId = TourData.tourId " + NL //                       //$NON-NLS-1$

               + " WHERE jTdataTtag.TourTag_TagId = ?" + NL //             //$NON-NLS-1$
               + whereClause + NL //
               + sqlFilter.getWhereClause() + NL

               + " GROUP BY startYear" + NL //                             //$NON-NLS-1$
               + " ORDER BY startYear" + NL //                             //$NON-NLS-1$
         ;

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, _tagId);
         sqlFilter.setParameters(statement, 2);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final int dbYear = result.getInt(1);

            final TVITaggingView_Year yearItem = new TVITaggingView_Year(this, dbYear, isMonth, getTagViewer());
            children.add(yearItem);

            yearItem.firstColumn = Integer.toString(dbYear);
            yearItem.readSumColumnData(result, 2);

            if (UI.IS_SCRAMBLE_DATA) {
               yearItem.firstColumn = UI.scrambleText(yearItem.firstColumn);
            }
         }

      } catch (final SQLException e) {

         SQL.showException(e, sql);
      }

      return children;
   }

   /**
    * This tag was added or removed from tours. According to the expand type, the structure of the
    * tag will be modified for the added or removed tours
    *
    * @param tagViewer
    * @param modifiedTours
    * @param isAddMode
    */
   public void refresh(final TreeViewer tagViewer,
                       final ArrayList<TourData> modifiedTours,
                       final boolean isAddMode) {

      switch (_tourTag.getExpandType()) {

      case TourTag.EXPAND_TYPE_FLAT:

         refreshFlatTours(tagViewer, modifiedTours, isAddMode);
         break;

      case TourTag.EXPAND_TYPE_YEAR_MONTH_DAY:

         refreshYearItems(tagViewer, true);
         break;

      case TourTag.EXPAND_TYPE_YEAR_DAY:

         refreshYearItems(tagViewer, false);
         break;

      default:
         break;
      }
   }

   private void refreshFlatTours(final TreeViewer tagViewer,
                                 final ArrayList<TourData> modifiedTours,
                                 final boolean isAddMode) {

      final ArrayList<TreeViewerItem> unfetchedChildren = getUnfetchedChildren();
      if (unfetchedChildren == null) {
         // children are not fetched
         return;
      }

      if (isAddMode) {

         // this tag was added to tours

         final ArrayList<TreeViewerItem> tagChildren = loadTagChildren_Tours(getTourIdWhereClause(modifiedTours));

         // update model
         unfetchedChildren.addAll(tagChildren);

         // update viewer
         tagViewer.add(this, tagChildren.toArray());

      } else {

         // this tag was remove from tours

         final HashMap<Long, TVITaggingView_Tour> removedTours = new HashMap<>();

         // loop all tour items
         for (final TreeViewerItem treeItem : unfetchedChildren) {

            if (treeItem instanceof TVITaggingView_Tour) {

               final TVITaggingView_Tour tourItem = (TVITaggingView_Tour) treeItem;
               final long itemTourId = tourItem.getTourId();

               // find tour item in the modified tours
               for (final TourData tourData : modifiedTours) {
                  if (tourData.getTourId().longValue() == itemTourId) {

                     // tree tour item was found in the modified tours

                     // remove the item outside of the for loop
                     removedTours.put(itemTourId, tourItem);

                     break;
                  }
               }
            }
         }

         final Collection<TVITaggingView_Tour> removedTourItems = removedTours.values();

         // update model
         unfetchedChildren.removeAll(removedTours.values());

         // update viewer
         tagViewer.remove(removedTourItems.toArray());
      }
   }

   /**
    * Read the year totals, this will read all year items for the tag and removes the years which do
    * not have any tour items
    *
    * @param tagViewer
    * @param isMonth
    */
   private void refreshYearItems(final TreeViewer tagViewer, final boolean isMonth) {

      final ArrayList<TreeViewerItem> allYearItems = loadTagChildren_Years(isMonth, UI.EMPTY_STRING);

      // update model
      setChildren(allYearItems);

      // update viewer
      tagViewer.update(allYearItems.toArray(), null);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVITagView_Tag " + System.identityHashCode(this) + NL //       //$NON-NLS-1$

            + "[" + NL //                                                     //$NON-NLS-1$

            + _tourTag

            + NL
            + "  numTours          = " + numTours + NL //                     //$NON-NLS-1$
            + "  numTags_NoTours   = " + numTags_NoTours + NL //              //$NON-NLS-1$

            + "]" + NL //                                                     //$NON-NLS-1$
      ;
   }

}
