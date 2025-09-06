/*******************************************************************************
 * Copyright (C) 2005, 2025 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.data.TourBike;

public class TourBikeManager {

   private static TourBikeManager     _instance;

   private static ArrayList<TourBike>     _bikes;

   private static final Object            LOCK = new Object();

   @SuppressWarnings("unchecked")
   private static void getBikesFromDb() {

      if (_bikes != null) {
         _bikes.clear();
      }

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      if (em != null) {

         final Query emQuery = em.createQuery(//
               //
               "SELECT TourBike" //$NON-NLS-1$
                     //
                     + (" FROM TourBike AS TourBike")); //$NON-NLS-1$

         _bikes = (ArrayList<TourBike>) emQuery.getResultList();

         em.close();
      }
   }

   public static TourBikeManager getInstance() {

      if (_instance != null) {
         return _instance;
      }

      synchronized (LOCK) {
         // check again
         if (_instance == null) {
            _instance = new TourBikeManager();
         }
      }

      return _instance;
   }

   /**
    * @return Returns all tour bikes from the database
    */
   public static ArrayList<TourBike> getTourBikes() {

      if (_bikes != null) {
         return _bikes;
      }

      synchronized (LOCK) {
         // check again
         if (_bikes != null) {
            return _bikes;
         }
         getBikesFromDb();
      }

      return _bikes;
   }

   public static void refreshBikes() {
      getBikesFromDb();
   }

}
