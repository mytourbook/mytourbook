/*******************************************************************************
 * Copyright (C) 2018, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.referenceTour;

import java.time.LocalDate;
import java.util.ArrayList;

import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.data.TourCompared;
import net.tourbook.ui.views.geoCompare.GeoComparedTour;

/**
 * Represents a compared tour (tree item) in the reference tour viewer or in the geo compare view
 */
public class TVIRefTour_ComparedTour extends TVIRefTour_TourItem {

   /**
    * Unique id for the {@link TourCompared} entity
    */
   long compareId;

   /**
    *
    */
   long refId      = -1;

   int  startIndex = -1;
   int  endIndex   = -1;

   /*
    * Fields from TourData
    */
   long            tourTypeId;
   String          tourTitle;

   LocalDate       tourDate;
   int             year;
   int             tourDeviceTime_Elapsed;

   float           avgAltimeter;
   float           avgPulse;
   float           maxPulse;

   float           avgSpeed;
   float           avgPace;

   ArrayList<Long> tagIds;

   GeoComparedTour geoCompareTour;

   public TVIRefTour_ComparedTour(final TreeViewerItem parentItem) {

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
      if (!(obj instanceof TVIRefTour_ComparedTour)) {
         return false;
      }
      final TVIRefTour_ComparedTour other = (TVIRefTour_ComparedTour) obj;
      if (compareId != other.compareId) {
         return false;
      }
      if (refId != other.refId) {
         return false;
      }
      return true;
   }

   @Override
   protected void fetchChildren() {}

   public float getAvgAltimeter() {
      return avgAltimeter;
   }

   public float getAvgPulse() {
      return avgPulse;
   }

   /**
    * @return Returns the Id for {@link TourCompared} entity
    */
   public long getCompareId() {
      return compareId;
   }

   public int getEndIndex() {
      return endIndex;
   }

   public GeoComparedTour getGeoCompareTour() {
      return geoCompareTour;
   }

   public float getMaxPulse() {
      return maxPulse;
   }

   public long getRefId() {
      return refId;
   }

   public int getStartIndex() {
      return startIndex;
   }

   public float getTourPace() {
      return avgPace;
   }

   public float getTourSpeed() {
      return avgSpeed;
   }

   @Override
   public boolean hasChildren() {
      /*
       * compared tours do not have children
       */
      return false;
   }

   @Override
   public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (compareId ^ (compareId >>> 32));
      result = prime * result + (int) (refId ^ (refId >>> 32));
      return result;
   }

   void remove() {

      // remove this tour item from the parent
      final ArrayList<TreeViewerItem> unfetchedChildren = getParentItem().getUnfetchedChildren();
      if (unfetchedChildren != null) {
         unfetchedChildren.remove(this);
      }
   }

   public void setAvgAltimeter(final float avgAltimeter) {
      this.avgAltimeter = avgAltimeter;
   }

   public void setAvgPulse(final float avgPulse) {
      this.avgPulse = avgPulse;
   }

   void setEndIndex(final int endIndex) {
      this.endIndex = endIndex;
   }

   public void setMaxPulse(final float maxPulse) {
      this.maxPulse = maxPulse;
   }

   void setStartIndex(final int startIndex) {
      this.startIndex = startIndex;
   }

   public void setTourDeviceTime_Elapsed(final int tourDeviceTime_Elapsed) {
      this.tourDeviceTime_Elapsed = tourDeviceTime_Elapsed;
   }

   void setTourPace(final float tourPace) {
      this.avgPace = tourPace;
   }

   void setTourSpeed(final float tourSpeed) {
      this.avgSpeed = tourSpeed;
   }

   @Override
   public String toString() {

      return NL

            + "TVIRefTour_ComparedTour" + NL //$NON-NLS-1$

            + "[" + NL //$NON-NLS-1$

//          + " compareId = " + compareId + NL //$NON-NLS-1$
//          + " refId     = " + refId + NL //$NON-NLS-1$
            + " tourDate  = " + tourDate + NL //$NON-NLS-1$
            + " tourTitle = " + tourTitle + NL //$NON-NLS-1$

            + "]" + NL //$NON-NLS-1$
      ;
   }
}
