/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourData;
import net.tourbook.database.MyTourbookException;
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

   /**
    * @param allEquipmentIDs
    *
    * @return Returns the equipment names separated with a comma.
    */
   public static String getEquipmentNames(final List<Long> allEquipmentIDs) {

      if (allEquipmentIDs == null) {
         return UI.EMPTY_STRING;
      }

      final Map<Long, Equipment> allEquipment = getAllEquipment_ByID();
      final List<String> allEquipmentNames = new ArrayList<>();

      // get equipment name for each equipment id
      for (final Long equipmentID : allEquipmentIDs) {

         final Equipment equipment = allEquipment.get(equipmentID);

         if (equipment != null) {
            allEquipmentNames.add(equipment.getName());
         } else {
            try {
               throw new MyTourbookException("Equipment id '" + equipmentID + "' is not available"); //$NON-NLS-1$ //$NON-NLS-2$
            } catch (final MyTourbookException e) {
               StatusUtil.log(e);
            }
         }
      }

      return getEquipmentNamesText(allEquipmentNames, false);
   }

   /**
    * @param allEquipment
    *
    * @return Returns the equipment names separated with a comma or an empty string when not
    *         available.
    */
   public static String getEquipmentNames(final Set<Equipment> allEquipment) {

      if (allEquipment.isEmpty()) {
         return UI.EMPTY_STRING;
      }

      final List<String> allEquipmentNames = new ArrayList<>();

      // get equipment name for each equipment id
      for (final Equipment equipment : allEquipment) {
         allEquipmentNames.add(equipment.getName());
      }

      return getEquipmentNamesText(allEquipmentNames, false);
   }

   public static String getEquipmentNames(final Set<Equipment> allEquipment, final boolean isVertical) {

      if (allEquipment.isEmpty()) {
         return UI.EMPTY_STRING;
      }

      final List<String> allEquipmentNames = new ArrayList<>();

      // get equipment name for each equipment id
      for (final Equipment equipment : allEquipment) {
         allEquipmentNames.add(equipment.getName());
      }

      return getEquipmentNamesText(allEquipmentNames, isVertical);
   }

   private static String getEquipmentNamesText(final List<String> allEquipmentNames, final boolean isVertical) {

      // sort equipment by name
      Collections.sort(allEquipmentNames);

      final int numEquipment = allEquipmentNames.size();

      // convert list into visible string
      int equipmentIndex = 0;
      final StringBuilder sb = new StringBuilder();

      for (final String equipmentName : allEquipmentNames) {

         if (equipmentIndex++ > 0) {
            if (isVertical) {
               sb.append(NL);
            } else {
               sb.append(UI.COMMA_SPACE);
            }
         }

         if (isVertical && numEquipment > 1) {

            // prefix a bullet but only when multiple equipment are available

            sb.append(net.tourbook.common.UI.SYMBOL_BULLET + UI.SPACE);
         }
         sb.append(equipmentName);
      }

      String equipmentNamesText = sb.toString();

      if (net.tourbook.common.UI.IS_SCRAMBLE_DATA) {

         equipmentNamesText = net.tourbook.common.UI.scrambleText(equipmentNamesText);
      }

      return equipmentNamesText;
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
