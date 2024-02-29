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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import net.tourbook.common.UI;
import net.tourbook.database.TourDatabase;

/**
 * Tour location points are wrapping {@link TourLocation}s with additional data
 */
@Entity
public class TourLocationPoint implements Serializable {

   private static final long serialVersionUID = 1L;

   private static final char NL               = UI.NEW_LINE;

   /**
    * Contains the entity id
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long              locationPointID  = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData          tourData;

   @ManyToOne(optional = false)
   private TourLocation      tourLocation;

   /**
    * Absolute time of the tour location in milliseconds since 1970-01-01T00:00:00Z.
    */
   private long              tourTime;

   /**
    * Position of this tour location in the data serie
    */
   private int               serieIndex;

   /**
    * Optional latitude position
    */
   private int               latitudeE6;

   /**
    * Optional longitude position
    */
   private int               longitudeE6;

   /**
    * Default constructor used also in ejb
    */
   public TourLocationPoint() {}

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

      final TourLocationPoint other = (TourLocationPoint) obj;

      return locationPointID == other.locationPointID;
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

   public long getTourTime() {
      return tourTime;
   }

   @Override
   public int hashCode() {

      return Objects.hash(locationPointID);
   }

   public void setLatitude(final int latitudeE6) {
      this.latitudeE6 = latitudeE6;
   }

   public void setLongitude(final int longitudeE6) {
      this.longitudeE6 = longitudeE6;
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

            + "TourLocationPoint" + NL //                                       //$NON-NLS-1$

            + " locationID          = " + locationPointID + NL //               //$NON-NLS-1$

      ;
   }
}
