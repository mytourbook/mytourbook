/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard
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

import java.util.Date;
import java.util.HashMap;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;

@Entity
public class PerformanceModelingData {

   //public static final int                 DB_LENGTH_GOVSS_ASSOCIATED_TOUR_TYPES = 255;

   public static final int       PERFORMANCE_MODELING_DATA_ID_NOT_DEFINED = -1;

   /**
    * Default rest pulse
    */
   public static final int       DEFAULT_REST_PULSE                       = 60;

   /**
    * manually created person creates a unique id to identify it, saved person is compared with the
    * person id
    */
   private static int            _createCounter                           = 0;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long                  PerformanceModelingDataId;

   /**
    * Training Stress data
    */

   // GOVSS
   private HashMap<Date, long[]> govssEntries;

   private HashMap<Date, long[]> bikeScoreEntries;
//   private HashMap<Date, long[]>           swimScoreEntries;
   //  private HashMap<Date, long[]>           trimpEntries;
   /**
    * Computed data : Fitness and fatigue
    */

   private HashMap<Date, long[]> fitnessValuesSkiba;

   private HashMap<Date, long[]> fatigueValuesSkiba;

   /**
    * default constructor used in ejb
    */
   public PerformanceModelingData() {}

   public HashMap<Date, long[]> getGovssEntries() {
      return govssEntries;
   }

   public long getPerformanceModelingDataId() {
      return PerformanceModelingDataId;
   }

   public boolean persist() {

      boolean isSaved = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {

         if (getPerformanceModelingDataId() == PERFORMANCE_MODELING_DATA_ID_NOT_DEFINED) {
            // entity is new
            ts.begin();
            em.persist(this);
            ts.commit();
         } else {
            // update entity
            ts.begin();
            em.merge(this);
            ts.commit();
         }

      } catch (final Exception e) {
         e.printStackTrace();
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         } else {
            isSaved = true;
         }
         em.close();
      }

      if (isSaved) {
         PersonManager.refreshPeople();
      }

      return isSaved;
   }

   public void setGovss(final long tourPersonId, final long tourStartTime, final int govss) {
      // we add or update the govss entry and update the fitness and fatigue values

   }

   @Override
   public String toString() {
      return "PerformanceModeling [performanceModelingDataId=" + PerformanceModelingDataId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
   }
}
