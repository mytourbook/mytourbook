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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.time.TimeTools;
import net.tourbook.database.PersonManager;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;

@Entity
public class PerformanceModelingData {

   public static final int                     PERFORMANCE_MODELING_DATA_ID_NOT_DEFINED = 0;

   private static IPreferenceStore             _prefStore                               = TourbookPlugin.getPrefStore();

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long                                PerformanceModelingDataId;

   /**
    * Training Stress data
    */

   // GOVSS
   @Lob
   private HashMap<LocalDate, ArrayList<Long>> govssEntries;

   @Lob
   private HashMap<Date, long[]>               bikeScoreEntries;

   //  private HashMap<Date, long[]>           swimScoreEntries;
   //  private HashMap<Date, long[]>           trimpEntries;

   /**
    * Computed data : Fitness and fatigue
    */
   @Lob
   private HashMap<LocalDate, Integer> fitnessValuesSkiba;
   private LocalDate                   _govssEntriesMinDate;
   @Lob
   private HashMap<Date, long[]>       fatigueValuesSkiba;

   /**
    * default constructor used in ejb
    */
   public PerformanceModelingData() {}

   public void computeFatigueValues() {
      // TODO Auto-generated method stub

   }

   public void computeFitnessValues() {
      // TODO Auto-generated method stub

   }

   public HashMap<LocalDate, ArrayList<Long>> getGovssEntries() {
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

         // entity is new
         ts.begin();
         {
            final PerformanceModelingData performanceModelingDataEntity = em.find(PerformanceModelingData.class, getPerformanceModelingDataId());
            if (performanceModelingDataEntity == null) {
               em.persist(this);
            } else {
               em.merge(this);
            }
         }
         ts.commit();

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

   public void setGovss(final long tourStartTime, final long tourId) {
      // we add or update the govss entry and update the fitness and fatigue values

      if (govssEntries == null) {
         govssEntries = new HashMap<>();
      }

      final LocalDate tourStartDate = TimeTools.toLocalDate(tourStartTime);
      ArrayList<Long> tourIds = govssEntries.get(tourStartDate);
      if (tourIds == null) {
         tourIds = new ArrayList<>();

         tourIds.add(tourId);
         govssEntries.put(tourStartDate, tourIds);
      } else {
         if (tourIds.contains(tourId)) {
            return;
         }

         tourIds.add(tourId);
         govssEntries.replace(tourStartDate, tourIds);

      }

      updateSkibaFitnessAndFatigueValues(tourStartDate);

   }

   public void setGovssEntries(final HashMap<LocalDate, ArrayList<Long>> govssEntries) {
      this.govssEntries = govssEntries;
   }

   @Override
   public String toString() {
      return "PerformanceModelingData [performanceModelingDataId=" + PerformanceModelingDataId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Computes both the fitness and fatigue values for the Skiba Model.
    *
    * @param tourStartDate
    *           The date at and after which the values need to be computed
    */
   private void updateSkibaFitnessAndFatigueValues(final LocalDate tourStartDate) {

      if (fitnessValuesSkiba == null) {
         fitnessValuesSkiba = new HashMap<>();
      }

      // Computing the given date's performance value
      final int fitnessDecayTime = _prefStore.getInt(ITourbookPreferences.FITNESS_DECAY);
      final int fatigueDecayTime = _prefStore.getInt(ITourbookPreferences.FATIGUE_DECAY);

      // p(t) = k1.g(t) - k2.h(t)
      // g(t) = g(t-i)e^(-i/T1) + w(t)
      // h(t) = h(t-i)e^(-i/T2) + w(t)

      //Looking for the previous performance value
      final LocalDate previousDate = tourStartDate.minusDays(1);
      int previousFitnessValue = -1;
      while (previousDate.compareTo(_govssEntriesMinDate) > 0) {
         if (fitnessValuesSkiba.containsKey(previousDate)) {
            previousFitnessValue = fitnessValuesSkiba.get(previousDate);
            break;
         }
      }

      if (previousFitnessValue == -1) {
         _govssEntriesMinDate = tourStartDate;
         fitnessValuesSkiba.put(tourStartDate, 100);
      }

      // Updating the next dates' performance values, if there are any
      if (!fitnessValuesSkiba.containsKey(tourStartDate)) {
         fitnessValuesSkiba.put(tourStartDate, 0);
      } else {
         fitnessValuesSkiba.replace(tourStartDate, 0);

      }
   }
}
