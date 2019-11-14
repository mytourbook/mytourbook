/*******************************************************************************
 * Copyright (C) 2019 Frédéric Bard and Contributors
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
import net.tourbook.tour.TourManager;

import org.eclipse.jface.preference.IPreferenceStore;

@Entity
public class PerformanceModelingData {

   public static final int                     PERFORMANCE_MODELING_DATA_ID_NOT_DEFINED = 0;

   private static IPreferenceStore             _prefStore                               = TourbookPlugin.getPrefStore();
   private static int                          _fitnessDecayTime                        = _prefStore.getInt(ITourbookPreferences.FITNESS_DECAY);
   // private ITrainingStressDataListener         _trainingStressDataListener;

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
   @Lob
   private HashMap<Date, long[]>       fatigueValuesSkiba;

   /**
    * default constructor used in ejb
    */
   public PerformanceModelingData() {}

   // public void addTrainingStressDataListener(final ITrainingStressDataListener listener) {
   //   _trainingStressDataListener = listener;
   // }

   public void computeFatigueValues() {
      // TODO Auto-generated method stub

   }

   /**
    * Computes the fitness value for a given date and a given previous training data
    * Fitness = g(t) = g(t-i)e^(-i/T1) + w(t)
    *
    * @param numberOfDays
    *           The number of days between the current's day of training and the previous day of
    *           training.
    * @param previousFitnessValue
    * @param totalGovss
    * @return
    */
   private int computeFitnessValue(final int numberOfDays, final int previousFitnessValue, final int totalGovss) {

      final int fitnessValue = computeResponseValue(_fitnessDecayTime, numberOfDays, previousFitnessValue, totalGovss);

      return fitnessValue;
   }

   public void computeFitnessValues() {
      // TODO Auto-generated method stub

   }

   /**
    * Computes the response level value for a given date and a given previous training data
    *
    * @param decayTime
    * @param numberOfDays
    *           The number of days between the current's day of training and the previous day of
    *           training.
    * @param previousResponseValue
    * @param trainingStressValue
    * @return
    */
   private int computeResponseValue(final int decayTime, final int numberOfDays, final int previousResponseValue, final int trainingStressValue) {

      final float exponent = numberOfDays * -1f / decayTime;

      final int responseValue = (int) (previousResponseValue * Math.exp(exponent) + trainingStressValue);

      return responseValue;
   }

   public HashMap<LocalDate, Integer> getFitnessValuesSkiba() {
      return fitnessValuesSkiba;
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
      }
      if (!tourIds.contains(tourId)) {
         tourIds.add(tourId);
         govssEntries.put(tourStartDate, tourIds);
      }

      updateSkibaFitnessValues(tourStartDate);

      // _trainingStressDataListener.trainingStressDataIsModified();

   }

   public void setGovssEntries(final HashMap<LocalDate, ArrayList<Long>> govssEntries) {
      this.govssEntries = govssEntries;
   }

   @Override
   public String toString() {
      return "PerformanceModelingData [performanceModelingDataId=" + PerformanceModelingDataId + "]"; //$NON-NLS-1$ //$NON-NLS-2$
   }

   /**
    * Computes and updates the fitness values for the Skiba Model.
    * The formulas are based on R. Banister's paper
    * "Modeling human performance in running"
    * Source : https://pdfs.semanticscholar.org/c497/9ca5064ce45e0b69da4452d2f637d7fdfd9d.pdf
    *
    * @param tourStartDate
    *           The date at and after which the values need to be computed
    */
   private void updateSkibaFitnessValues(final LocalDate tourStartDate) {

      if (fitnessValuesSkiba == null) {
         fitnessValuesSkiba = new HashMap<>();
      }

      // final int fatigueDecayTime = _prefStore.getInt(ITourbookPreferences.FATIGUE_DECAY);

      // h(t) = h(t-i)e^(-i/T2) + w(t)

      int totalGovss = 0;
      ArrayList<Long> tourIds = govssEntries.get(tourStartDate);
      for (final Long tourId : tourIds) {
         totalGovss += TourManager.getTour(tourId).getGovss();
      }

      int fitnessValue;
      if (fitnessValuesSkiba.size() == 0) {

         fitnessValue = computeFitnessValue(0, 0, totalGovss);
         fitnessValuesSkiba.put(tourStartDate, fitnessValue);
         return;
      }

      final LocalDate govssEntriesMinDate = Collections.min(fitnessValuesSkiba.keySet());
      //We find the previous fitness value
      LocalDate previousDate = tourStartDate;
      int previousFitnessValue = 0;
      while (previousDate.compareTo(govssEntriesMinDate) >= 0) {

         if (fitnessValuesSkiba.containsKey(previousDate)) {

            previousFitnessValue = fitnessValuesSkiba.get(previousDate);
            break;
         }
         previousDate = previousDate.minusDays(1);
      }

      // Computing the number of days separating the given workout and the previous one
      int numberOfDays = (int) ChronoUnit.DAYS.between(previousDate, tourStartDate);

      // Computing the given date's performance value
      fitnessValue = computeFitnessValue(numberOfDays, previousFitnessValue, totalGovss);

      if (fitnessValuesSkiba.containsKey(tourStartDate)) {

         fitnessValuesSkiba.replace(tourStartDate, fitnessValue);
      } else {

         fitnessValuesSkiba.put(tourStartDate, fitnessValue);
      }

      // Updating and creating, if necessary, all the fitness values after the given tour's date
      final LocalDate govssEntriesMaxDate = Collections.max(fitnessValuesSkiba.keySet());
      LocalDate nextDate = tourStartDate.plusDays(1);
      previousFitnessValue = fitnessValue;
      LocalDate previousTrainingDate = tourStartDate;
      while (nextDate.compareTo(govssEntriesMaxDate) <= 0) {

         totalGovss = 0;
         if (govssEntries.containsKey(nextDate)) {

            tourIds = govssEntries.get(nextDate);
            for (final Long tourId : tourIds) {
               totalGovss += TourManager.getTour(tourId).getGovss();
            }

            previousTrainingDate = nextDate;
         }

         numberOfDays = (int) ChronoUnit.DAYS.between(previousTrainingDate, nextDate);
         fitnessValue = computeFitnessValue(numberOfDays, previousFitnessValue, totalGovss);

         if (fitnessValuesSkiba.containsKey(nextDate)) {
            fitnessValuesSkiba.replace(nextDate, fitnessValue);
         } else {
            fitnessValuesSkiba.put(nextDate, fitnessValue);
         }

         previousFitnessValue = fitnessValue;

         nextDate = nextDate.plusDays(1);
      }
   }
}
