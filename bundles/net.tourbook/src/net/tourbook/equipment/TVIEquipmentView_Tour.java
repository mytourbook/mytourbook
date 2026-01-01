/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Tour extends TVIEquipmentView_Item {

   public static final String SQL_TOUR_COLUMNS = UI.EMPTY_STRING

         + "TourData.TourID, "                                  //   1  //$NON-NLS-1$

         + "TourData.TourStartTime, "                           //   2  //$NON-NLS-1$
         + "TourData.TimeZoneId, "                              //   3  //$NON-NLS-1$

         + "TourData.TourTitle, "                               //   4  //$NON-NLS-1$
         + "TourData.TourType_TypeId, "                         //   5  //$NON-NLS-1$

         + SQL_SUM_COLUMNS_TOUR                                 //   6
   ;

   long                       tourId;

   long                       tourStartTime;
   ZonedDateTime              tourStartDateTime;

   String                     tourTitle;
   long                       tourTypeId;

   public TVIEquipmentView_Tour(final TVIEquipmentView_Item tviParentItem,
                                final TreeViewer treeViewer) {

      super(treeViewer);

      setParentItem(tviParentItem);
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

   @Override
   public boolean hasChildren() {

      return false;
   }

   public void readColumnValues_Tour(final ResultSet result) throws SQLException {

// SET_FORMATTING_OFF

      tourId                        = result.getLong(1);
      final long tourStartTimeMS    = result.getLong(2);
      final String timeZoneID       = result.getString(3);
      tourTitle                     = result.getString(4);
      final Object resultTourTypeId = result.getObject(5);

// SET_FORMATTING_ON

      tourStartDateTime = TimeTools.getZonedDateTime(tourStartTimeMS, timeZoneID);

      firstColumn = tourStartDateTime.format(TimeTools.Formatter_Date_S);

      tourTypeId = (resultTourTypeId == null ? TourDatabase.ENTITY_IS_NOT_SAVED : (Long) resultTourTypeId);

      if (UI.IS_SCRAMBLE_DATA) {
         tourTitle = UI.scrambleText(tourTitle);
      }

      readColumnValues_Default(result, 6);
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TVIEquipmentView_Tour" + NL //                                 //$NON-NLS-1$

            + " tourId              = " + tourId + NL //                      //$NON-NLS-1$
            + " tourStartDateTime   = " + tourStartDateTime + NL //           //$NON-NLS-1$
            + " tourTitle           = " + tourTitle + NL //                   //$NON-NLS-1$
            + " tourTypeId          = " + tourTypeId + NL //                  //$NON-NLS-1$

      ;
   }
}
