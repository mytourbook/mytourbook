/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.database;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;

/**
 * Interface to compute tour values for {@link TourData}
 */
public abstract class ITourDataUpdateConcurrent {

   private static ThreadPoolExecutor       _dbUpdateExecutor;

   private static ArrayBlockingQueue<Long> _dbUpdateQueue = new ArrayBlockingQueue<>(Util.NUMBER_OF_PROCESSORS);

	/**
	 * @param originalTourData
	 *            {@link TourData} which is not yet modified
	 * @return Returns <code>true</code> when {@link TourData} was modified and the tour needs to be
	 *         saved
	 */
   protected abstract void tourDataUpdate(TourData originalTourData);

//   ,
//   int originalDatabaseVersion,
//   int newDatabaseVersion

   /**
    * Do data updates concurrently with all available processor threads, this is reducing time
    * significantly.
    *
    * @param tourId
    */
   public void updateDb_047_To_048_DataUpdate_Concurrent(final Long tourId) {

      final ThreadFactory updateThreadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "Saving database entities");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

      _dbUpdateExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Util.NUMBER_OF_PROCESSORS, updateThreadFactory);

      try {

         // put tour ID (queue item) into the queue AND wait when it is full

         _dbUpdateQueue.put(tourId);

      } catch (final InterruptedException e) {

         //return boolean ? _isSQLDataUpdateError = true;

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      _dbUpdateExecutor.submit(() -> {

         // get last added item
         final Long queueItem_TourId = _dbUpdateQueue.poll();

         if (queueItem_TourId == null) {
            return;
         }

         final EntityManager entityManager = TourDatabase.getInstance().getEntityManager();

         try {

            // get tour data by tour id
            final TourData tourData = entityManager.find(TourData.class, queueItem_TourId);
            if (tourData == null) {
               return;
            }

            tourDataUpdate(tourData);
//            if (tourData.getWeather_Clouds().equalsIgnoreCase("weather-showers-scatterd")) { //$NON-NLS-1$
//
//               /**
//                * If the weather cloud has the old value (with the typo) for the scattered showers,
//                * it is updated to the new value
//                */
//               tourData.setWeather_Clouds(IWeather.WEATHER_ID_SCATTERED_SHOWERS);
//            }

            boolean isSaved = false;

            final EntityTransaction transaction = entityManager.getTransaction();
            try {

               transaction.begin();
               {
                  entityManager.merge(tourData);
               }
               transaction.commit();

            } catch (final Exception e) {

               // _isSQLDataUpdateError = true;
               StatusUtil.showStatus(e);

            } finally {
               if (transaction.isActive()) {
                  transaction.rollback();
               } else {
                  isSaved = true;
               }
            }

            if (!isSaved) {
               //  showTourSaveError(tourData);
            }

         } finally {

            entityManager.close();
         }
      });
   }
}
