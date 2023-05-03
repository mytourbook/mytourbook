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
package net.tourbook.data;

import java.sql.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * This entity contains the data for a tour {@link TourData} which is compared with a reference tour
 * {@link TourReference}
 */
@Entity
public class TourCompared {

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long  comparedId;

   /**
    * Ref tour id which is compared with the tour contained in the tourId
    */
   private long  refTourId;

   /**
    * TourId which is compared with the refTourId
    */
   private long  tourId;

   /**
    * Start index for the reference tour in the compared tour
    */
   private int   startIndex = -1;

   /**
    * End index for the reference tour in the compared tour
    */
   private int   endIndex   = -1;

   private Date  tourDate;
   private int   startYear;

   private float tourSpeed;

   /**
    * This field is read with sql statements
    */
   @SuppressWarnings("unused")
   private int   tourDeviceTime_Elapsed;

   /**
    * @since Db version 28
    */
   private float avgPulse;

   /**
    * @since Db version 50
    */
   private float maxPulse;

   /**
    * @since Db version 50
    */
   private float avgAltimeter;

   public float getAvgAltimeter() {
      return avgAltimeter;
   }

   public float getAvgPulse() {
      return avgPulse;
   }

   public long getComparedId() {
      return comparedId;
   }

   public int getEndIndex() {
      return endIndex;
   }

   public float getMaxPulse() {
      return maxPulse;
   }

   public long getRefTourId() {
      return refTourId;
   }

   public int getStartIndex() {
      return startIndex;
   }

   public int getStartYear() {
      return startYear;
   }

   public Date getTourDate() {
      return tourDate;
   }

   public long getTourId() {
      return tourId;
   }

   public float getTourSpeed() {
      return tourSpeed;
   }

   public void setAvgAltimeter(final float avgAltimeter) {
      this.avgAltimeter = avgAltimeter;
   }

   public void setAvgPulse(final float avgPulse) {
      this.avgPulse = avgPulse;
   }

   public void setEndIndex(final int endIndex) {
      this.endIndex = endIndex;
   }

   public void setMaxPulse(final float maxPulse) {
      this.maxPulse = maxPulse;
   }

   public void setRefTourId(final long refTourId) {
      this.refTourId = refTourId;
   }

   public void setStartIndex(final int startIndex) {
      this.startIndex = startIndex;
   }

   public void setStartYear(final int startYear) {
      this.startYear = startYear;
   }

   public void setTourDate(final long timeInMillis) {
      tourDate = new Date(timeInMillis);
   }

   public void setTourDeviceTime_Elapsed(final int tourDeviceTime_Elapsed) {
      this.tourDeviceTime_Elapsed = tourDeviceTime_Elapsed;
   }

   public void setTourId(final long tourId) {
      this.tourId = tourId;
   }

   public void setTourSpeed(final float speed) {
      this.tourSpeed = speed;
   }

}
