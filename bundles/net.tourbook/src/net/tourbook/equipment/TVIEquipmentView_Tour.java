/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Tour extends TVIEquipmentView_Item {

   public static final String SQL_TOUR_COLUMNS = UI.EMPTY_STRING

         + "startYear,"                                                //   0 //$NON-NLS-1$
         + "startMonth,"                                               //   1 //$NON-NLS-1$
         + "startDay,"                                                 //   2 //$NON-NLS-1$

         + "tourTitle,"                                                //   3 //$NON-NLS-1$
         + "tourType_typeId,"                                          //   4 //$NON-NLS-1$
         + "deviceTimeInterval,"                                       //   5 //$NON-NLS-1$
         + "startDistance"                                             //   6 //$NON-NLS-1$

//         + SQL_SUM_COLUMNS_TOUR                                //   7
   ;

   long                       tourId;

   ZonedDateTime              tourDate;
   int                        tourYear;
   int                        tourMonth;
   int                        tourDay;

   String                     tourTitle;
   long                       tourTypeId;

   List<Long>                 allEquipmentIDs;

   public long                deviceStartDistance;
   public short               deviceTimeInterval;

   public TVIEquipmentView_Tour(final TVIEquipmentView_Equipment tviEquipmentView_Equipment,
                                final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(tviEquipmentView_Equipment);
   }

   @Override
   public boolean equals(final Object obj) {

      if (obj == this) {
         return true;
      }

      return false;
   }

   @Override
   protected void fetchChildren() {

      // there are no children for a tour
   }

   public void getTourColumnData(final ResultSet result,
                                 final Object resultEquipmentId,
                                 final int startIndex)
         throws SQLException {

      tourYear = result.getInt(startIndex + 0);
      tourMonth = result.getInt(startIndex + 1);
      tourDay = result.getInt(startIndex + 2);

      tourDate = ZonedDateTime.of(tourYear, tourMonth, tourDay, 0, 0, 0, 0, TimeTools.getDefaultTimeZone());

      tourTitle = result.getString(startIndex + 3);

      final Object resultTourTypeId = result.getObject(startIndex + 4);
      tourTypeId = (resultTourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) resultTourTypeId);

      deviceTimeInterval = result.getShort(startIndex + 5);
      deviceStartDistance = result.getLong(startIndex + 6);

      if (UI.IS_SCRAMBLE_DATA) {
         tourTitle = UI.scrambleText(tourTitle);
      }

//      readDefaultColumnData(result, startIndex + 7);

      if (resultEquipmentId instanceof Long) {
         allEquipmentIDs = new ArrayList<>();
         allEquipmentIDs.add((Long) resultEquipmentId);
      }
   }

   @Override
   public boolean hasChildren() {

      return false;
   }

   @Override
   public String toString() {

      final int maxLen = 5;

      final List<Long> allEquipmentIDsText = allEquipmentIDs != null //
            ? allEquipmentIDs.subList(0, Math.min(allEquipmentIDs.size(), maxLen))
            : null;

      return UI.EMPTY_STRING

            + "TVIEquipmentView_Tour" + NL //                        //$NON-NLS-1$

            + " tourId     = " + tourId + NL //                      //$NON-NLS-1$
            + " tourDate   = " + tourDate + NL //                    //$NON-NLS-1$
            + " tourTitle  = " + tourTitle + NL //                   //$NON-NLS-1$
            + " tourTypeId = " + tourTypeId + NL //                  //$NON-NLS-1$

            + " allEquipmentIDs = " + allEquipmentIDsText + NL //    //$NON-NLS-1$
      ;
   }
}
