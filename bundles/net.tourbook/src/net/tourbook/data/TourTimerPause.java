/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard and Contributors
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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.xml.bind.annotation.XmlElement;

import net.tourbook.database.TourDatabase;

@Entity
public class TourTimerPause implements Serializable {

   private static final long serialVersionUID = 1L;

   /**
    * Unique id for the {@link TourTimerPause} entity
    */
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long              timerPauseId     = TourDatabase.ENTITY_IS_NOT_SAVED;

   @ManyToOne(optional = false)
   private TourData          tourData;

   @XmlElement
   private long              startTime;

   @XmlElement
   private long              endTime;

   // constructor is required for hibernate
   public TourTimerPause() {}

   public TourTimerPause(final TourData tourData, final long startTime, final long endTime) {
      this.tourData = tourData;
      this.startTime = startTime;
      this.endTime = endTime;
   }

   public long getEndTime() {
      return endTime;
   }

   public long getPauseDuration() {
      return endTime - startTime;
   }

   public long getStartTime() {
      return startTime;
   }

   /**
    * Sets the timer pause end time (in milliseconds)
    *
    * @param endTime
    */
   public void setEndTime(final long endTime) {
      this.endTime = endTime;
   }

   /**
    * Sets the timer pause start time (in milliseconds)
    *
    * @param startTime
    */
   public void setStartTime(final long startTime) {
      this.startTime = startTime;
   }

   public void setTourData(final TourData tourData) {
      this.tourData = tourData;
   }
}
