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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.database.TourDatabase;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Tour extends TVIEquipmentView_Item {

   public static final String         SQL_TOUR_COLUMNS = UI.EMPTY_STRING

         + "TourData.TourID," + NL                                      //   1  //$NON-NLS-1$

         + "TourData.TourStartTime," + NL                               //   2  //$NON-NLS-1$
         + "TourData.TimeZoneId," + NL                                  //   3  //$NON-NLS-1$
         + "TourData.TourTitle," + NL                                   //   4  //$NON-NLS-1$

         + "TourData.TourType_TypeId," + NL                             //   5  //$NON-NLS-1$

         + "jTdataTtag.TourTag_TagId," + NL                             //   6  //$NON-NLS-1$
         + "Tmarker.MarkerId," + NL                                     //   7  //$NON-NLS-1$
         + "jTdataEq.Equipment_EquipmentID," + NL                       //   8  //$NON-NLS-1$

         + getSQL_SUM_TOUR_COLUMNS("TourData", 0)                       //   9  //$NON-NLS-1$
   ;

   private TVIEquipmentView_Equipment _equipmentItem;
   private TVIEquipmentView_Part      _partItem;

   public long                        tourId;

   long                               tourStartTime;
   ZonedDateTime                      tourStartDateTime;

   String                             tourTitle;
   public long                        tourTypeId;

   private Set<Long>                  _allEquipmentIDs;
   private Set<Long>                  _allMarkerIDs;
   private Set<Long>                  _allTagIDs;

   private List<Long>                 _allEquipmentIDsList;
   private List<Long>                 _allMarkerIDsList;
   private List<Long>                 _allTagIDsList;

   public TVIEquipmentView_Tour(final TVIEquipmentView_Item parentItem,
                                final TVIEquipmentView_Equipment equipmentItem,
                                final TreeViewer treeViewer,
                                final EquipmentViewerType equipmentType) {

      super(treeViewer, equipmentType);

      setParentItem(parentItem);

      _equipmentItem = equipmentItem;
   }

   public TVIEquipmentView_Tour(final TVIEquipmentView_Item parentItem,
                                final TVIEquipmentView_Part partItem,
                                final TreeViewer treeViewer,
                                final EquipmentViewerType equipmentType) {

      super(treeViewer, equipmentType);

      setParentItem(parentItem);

      _partItem = partItem;
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

   public List<Long> getEquipmentIds() {

      if (_allEquipmentIDsList != null) {
         return _allEquipmentIDsList;
      }

      if (_allEquipmentIDs != null) {
         _allEquipmentIDsList = new ArrayList<>(_allEquipmentIDs);
      }

      return _allEquipmentIDsList;
   }

   public TVIEquipmentView_Equipment getEquipmentItem() {
      return _equipmentItem;
   }

   public List<Long> getMarkerIds() {

      if (_allMarkerIDsList != null) {
         return _allMarkerIDsList;
      }

      if (_allMarkerIDs != null) {
         _allMarkerIDsList = new ArrayList<>(_allMarkerIDs);
      }

      return _allMarkerIDsList;
   }

   public TVIEquipmentView_Part getPartItem() {
      return _partItem;
   }

   public List<Long> getTagIds() {

      if (_allTagIDsList != null) {
         return _allTagIDsList;
      }

      if (_allTagIDs != null) {
         _allTagIDsList = new ArrayList<>(_allTagIDs);
      }

      return _allTagIDsList;
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

      final Object dbTourTypeId     = result.getObject(5);

// SET_FORMATTING_ON

      tourStartDateTime = TimeTools.getZonedDateTime(tourStartTimeMS, timeZoneID);

      firstColumn = tourStartDateTime.format(TimeTools.Formatter_Date_S);

      tourTypeId = dbTourTypeId == null
            ? TourDatabase.ENTITY_IS_NOT_SAVED
            : (Long) dbTourTypeId;

      if (UI.IS_SCRAMBLE_DATA) {
         tourTitle = UI.scrambleText(tourTitle);
      }

      readCommonValues(result, 9);
   }

   void setEquipmentIds(final Set<Long> allEquipmentIDs) {
      _allEquipmentIDs = allEquipmentIDs;
   }

   public void setMarkerIds(final Set<Long> allMarkerIDs) {
      _allMarkerIDs = allMarkerIDs;
   }

   void setTagIds(final Set<Long> allTagIDs) {
      _allTagIDs = allTagIDs;
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
