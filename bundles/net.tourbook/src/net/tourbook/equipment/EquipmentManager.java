/*******************************************************************************
 * Copyright (C) 2055, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public class EquipmentManager {

   private static final char                    NL      = UI.NEW_LINE;

   private static final Object                  DB_LOCK = new Object();

   private static volatile Map<Long, Equipment> _allEquipment_ByID;
   private static volatile List<Equipment>      _allEquipment_ByName;

   private static ConcurrentSkipListSet<String> _allEquipment_Brands;
   private static ConcurrentSkipListSet<String> _allEquipment_Models;

   public static void clearCachedValues() {

      if (_allEquipment_ByID != null) {

         _allEquipment_ByID.clear();
         _allEquipment_ByID = null;
      }

      if (_allEquipment_ByName != null) {

         _allEquipment_ByName.clear();
         _allEquipment_ByName = null;
      }

      if (_allEquipment_Brands != null) {

         _allEquipment_Brands.clear();
         _allEquipment_Brands = null;
      }

      if (_allEquipment_Models != null) {

         _allEquipment_Models.clear();
         _allEquipment_Models = null;
      }
   }

   /**
    * Add equipment additional to the existing equipment
    *
    * @param equipment
    * @param tourProvider
    * @param isSaveTour
    * @param isCheckTourEditor
    *           When <code>true</code> then the tour editor is check if it is dirty
    */
   public static void equipment_Add(final Equipment equipment,
                                    final ITourProvider tourProvider,
                                    final boolean isSaveTour,
                                    final boolean isCheckTourEditor) {

      // fix https://github.com/mytourbook/mytourbook/issues/1437
      if (isCheckTourEditor) {

         if (TourManager.isTourEditorModified()) {
            return;
         }
      }

      final Runnable runnable = new Runnable() {
         @Override
         public void run() {

            final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
            if (selectedTours == null || selectedTours.isEmpty()) {
               return;
            }

            // add equipment in all tours (without tours which are opened in an editor)
            for (final TourData tourData : selectedTours) {

               final Set<Equipment> allEquipment = tourData.getEquipment();

               allEquipment.add(equipment);
            }

            saveAndNotify(tourProvider, isSaveTour, selectedTours);
         }
      };

      BusyIndicator.showWhile(Display.getCurrent(), runnable);
   }

   public static void equipment_Delete(final List<Equipment> allSelectedEquipments) {
 
   }

   /**
    * Add equipment additional to the existing equipment
    *
    * @param equipment
    * @param tourProvider
    * @param isSaveTour
    * @param isCheckTourEditor
    *           When <code>true</code> then the tour editor is check if it is dirty
    */
   public static void equipment_Remove(final Equipment equipment,
                                       final ITourProvider tourProvider,
                                       final boolean isSaveTour,
                                       final boolean isCheckTourEditor) {

      // fix https://github.com/mytourbook/mytourbook/issues/1437
      if (isCheckTourEditor) {

         if (TourManager.isTourEditorModified()) {
            return;
         }
      }

      final Runnable runnable = new Runnable() {
         @Override
         public void run() {

            final ArrayList<TourData> selectedTours = tourProvider.getSelectedTours();
            if (selectedTours == null || selectedTours.isEmpty()) {
               return;
            }

            // add equipment in all tours (without tours which are opened in an editor)
            for (final TourData tourData : selectedTours) {

               final Set<Equipment> allEquipment = tourData.getEquipment();

               allEquipment.remove(equipment);
            }

            saveAndNotify(tourProvider, isSaveTour, selectedTours);
         }
      };

      BusyIndicator.showWhile(Display.getCurrent(), runnable);
   }

   /**
    * @return Returns a map with all equipments, key is the equipment ID
    */
   public static Map<Long, Equipment> getAllEquipment_ByID() {

      if (_allEquipment_ByID != null) {
         return _allEquipment_ByID;
      }

      loadEquipment();

      return _allEquipment_ByID;
   }

   /**
    * @return Returns a list with all equipments sorted by name
    */
   public static List<Equipment> getAllEquipment_Name() {

      if (_allEquipment_ByName != null) {
         return _allEquipment_ByName;
      }

      loadEquipment();

      return _allEquipment_ByName;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllEquipment_Brands() {

      if (_allEquipment_Brands == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allEquipment_Brands == null) {

               _allEquipment_Brands = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT, "brand"); //$NON-NLS-1$
            }
         }
      }

      return _allEquipment_Brands;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllEquipment_Models() {

      if (_allEquipment_Models == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allEquipment_Models == null) {

               _allEquipment_Models = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT, "model"); //$NON-NLS-1$
            }
         }
      }

      return _allEquipment_Models;
   }

   private static void loadEquipment() {

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allEquipment_ByID != null) {
            return;
         }

         final Map<Long, Equipment> allEquipments_ByID = new HashMap<>();
         final List<Equipment> allEquipments_ByName = new ArrayList<>();

         final EntityManager em = TourDatabase.getInstance().getEntityManager();
         if (em != null) {

            final Query query = em.createQuery(UI.EMPTY_STRING

                  + "SELECT Equipment" + NL //                                               //$NON-NLS-1$
                  + " FROM " + Equipment.class.getSimpleName() + " AS Equipment" + NL //     //$NON-NLS-1$ //$NON-NLS-2$

                  // sort by name
                  + " ORDER BY Equipment.brand, Equipment.model" + NL //                     //$NON-NLS-1$
            );

            final List<?> resultList = query.getResultList();

            for (final Object result : resultList) {

               if (result instanceof final Equipment equipment) {

                  allEquipments_ByID.put(equipment.getEquipmentId(), equipment);
                  allEquipments_ByName.add(equipment);
               }
            }

            em.close();
         }

         _allEquipment_ByID = allEquipments_ByID;
         _allEquipment_ByName = allEquipments_ByName;
      }
   }

   private static void saveAndNotify(final ITourProvider tourProvider,
                                     final boolean isSaveTour,
                                     final ArrayList<TourData> selectedTours) {

      if (isSaveTour) {

         // save all tours with the modified equipment
         TourManager.saveModifiedTours(selectedTours);

      } else {

         // tours are not saved but the tour provider must be notified

         if (tourProvider instanceof final ITourProvider2 tourProvider2) {

            tourProvider2.toursAreModified(selectedTours);

         } else {

            TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(selectedTours));
         }
      }
   }

}
