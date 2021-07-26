/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import java.util.Objects;

import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;

/**
 * Tree view item with the compare result between the reference and the compared tour
 */
public class TVICompareResultComparedTour extends TVICompareResultItem {

   /**
    * Unique key for the {@link TourCompared} entity, when <code>-1</code> the compared tour is not
    * saved in the database
    */
   long               compareId            = -1;

   public RefTourItem refTour;

   /**
    * Contains the tour ID for the compared tour
    */
   Long               tourId;

   /**
    * Contains the {@link TourData} for the compared tour
    */
   private TourData   comparedTourData;

   /**
    * Contains the minimum value for the altitude differenz
    */
   float              minAltitudeDiff;

   /**
    * Contains the minimum data serie for each compared value
    */
   float[]            altitudeDiffSerie;

   int                computedStartIndex   = -1;
   int                computedEndIndex     = -1;

   int                normalizedStartIndex = -1;
   int                normalizedEndIndex   = -1;

   int                compareMovingTime;
   int                compareElapsedTime;

   float              compareDistance;
   float              compareSpeed;
   int                timeInterval;

   /**
    * Metric or imperial altimeter (VAM)
    */
   float              avgAltimeter;

   /*
    * When a compared tour is stored in the database, the compId is set and the data from the
    * database are stored in the field's db...
    */
   int   dbStartIndex;
   int   dbEndIndex;

   float dbSpeed;
   int   dbElapsedTime;

   /*
    * The moved... fields contain the position of the compared tour when the user moved the
    * position
    */
   float movedSpeed;

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

      final TVICompareResultComparedTour other = (TVICompareResultComparedTour) obj;

      return Objects.equals(refTour, other.refTour) && Objects.equals(tourId, other.tourId);
   }

   @Override
   protected void fetchChildren() {}

   public TourData getComparedTourData() {
      return comparedTourData;
   }

   public Long getTourId() {
      return tourId;
   }

   @Override
   public boolean hasChildren() {

      /*
       * Compare result has no children, hide the expand sign
       */
      return false;
   }

   @Override
   public int hashCode() {

      return Objects.hash(refTour, tourId);
   }

   /**
    * @return Returns <code>true</code> when the compare result is saved in the database
    */
   boolean isSaved() {

      return compareId != -1;
   }

   public void setComparedTourData(final TourData comparedTourData) {

      this.comparedTourData = comparedTourData;
      this.tourId = comparedTourData.getTourId();
   }

}
