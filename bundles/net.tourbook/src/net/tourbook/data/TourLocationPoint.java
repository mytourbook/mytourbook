/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;

/**
 * Tour location points are wrapping {@link TourLocation}s with additional data
 */
@Entity
public class TourLocationPoint implements Serializable {

   private static final long          serialVersionUID = 1L;

   private static final char          NL               = UI.NEW_LINE;

   /**
    * Manually created entities create a unique id to identify them, saved entities are compared
    * with the ID
    */
   private static final AtomicInteger _createCounter   = new AtomicInteger();

   /**
    * Contains the entity id
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                       locationPointID  = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData                   tourData;

   @ManyToOne(optional = false)
   private TourLocation               tourLocation;

   /**
    * Absolute time of the tour location in milliseconds since 1970-01-01T00:00:00Z.
    */
   private long                       tourTime;

   /**
    * Position of this tour location in the data serie
    */
   private int                        serieIndex;

   /**
    * Optional latitude position
    */
   private int                        latitudeE6;

   /**
    * Optional longitude position
    */
   private int                        longitudeE6;

   /**
    * Unique ID for manually created entities because the {@link #locationPointIDId} is 0 when the
    * entity is not yet persisted
    */
   @Transient
   private long                       _createId        = 0;

   /**
    * Default constructor used in ejb
    */
   public TourLocationPoint() {}

   public TourLocationPoint(final TourData tourData, final TourLocation tourLocation) {

      this.tourData = tourData;
      this.tourLocation = tourLocation;

      _createId = _createCounter.incrementAndGet();
   }

   /**
    * TourLocationPoint is compared with the {@link TourLocationPoint#locationPointID} or
    * {@link TourLocationPoint#_createId}
    * <p>
    * <b> {@link #serieIndex} is not used for equals or hashcode because this is modified when
    * location points are deleted</b>
    *
    * @see java.lang.Object#equals(java.lang.Object)
    */
   @Override
   public boolean equals(final Object obj) {

      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (!(obj instanceof TourLocationPoint)) {
         return false;
      }

      final TourLocationPoint otherPoint = (TourLocationPoint) obj;

      if (locationPointID == TourDatabase.ENTITY_IS_NOT_SAVED) {

         // location point was create

         if (_createId == otherPoint._createId) {
            return true;
         }

      } else {

         // location point is from the database

         if (locationPointID == otherPoint.locationPointID) {
            return true;
         }
      }

      return false;
   }

   public int getLatitude() {
      return latitudeE6;
   }

   public int getLongitude() {
      return longitudeE6;
   }

   public int getSerieIndex() {
      return serieIndex;
   }

   public TourLocation getTourLocation() {
      return tourLocation;
   }

   public long getTourTime() {
      return tourTime;
   }

   @Override
   public int hashCode() {

      return Objects.hash(locationPointID);
   }

   public void setGeoPosition(final int latE6, final int lonE6) {

      latitudeE6 = latE6;
      longitudeE6 = lonE6;
   }

   public void setSerieIndex(final int serieIndex) {
      this.serieIndex = serieIndex;
   }

   public void setTourTime(final long tourTime) {
      this.tourTime = tourTime;
   }

   @Override
   public String toString() {

      return UI.EMPTY_STRING

            + "TourLocationPoint" + NL //                            //$NON-NLS-1$

            + " locationPointID  = " + locationPointID + NL //       //$NON-NLS-1$
            + " _createId        = " + _createId + NL //             //$NON-NLS-1$

            + " tourTime         = " + tourTime + NL //              //$NON-NLS-1$
            + " serieIndex       = " + serieIndex + NL //            //$NON-NLS-1$
            + " latitudeE6       = " + latitudeE6 + NL //            //$NON-NLS-1$
            + " longitudeE6      = " + longitudeE6 + NL //           //$NON-NLS-1$
            + NL
            + " tourData         = " + tourData + NL //              //$NON-NLS-1$
            + NL
            + " tourLocation     = " + tourLocation + NL //          //$NON-NLS-1$

      ;
   }
}
