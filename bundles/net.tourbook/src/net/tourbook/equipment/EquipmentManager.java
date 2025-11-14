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
import java.util.concurrent.ConcurrentSkipListSet;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourTag;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;

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

   public static void deleteEquipments(final List<Equipment> allSelectedEquipments) {

   }

   /**
    * Deletes a tour tag from all contained tours and in the tag structure. This event
    * {@link TourEventId#TAG_STRUCTURE_CHANGED} is fired when done.
    *
    * @param allTags
    *
    * @return Returns <code>true</code> when deletion was successful
    */
   public static boolean deleteTourTag(final List<TourTag> allTags) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified(false)) {
         return false;
      }

      final String dialogMessage;
      final String actionDeleteTags;

//      final ArrayList<Long> allTourIds = getTaggedTours(allTags);
//
//      if (allTags.size() == 1) {
//
//         // remove one tag
//
//         dialogMessage = NLS.bind(Messages.Tag_Manager_Dialog_DeleteTag_Message, allTags.get(0).getTagName(), allTourIds.size());
//         actionDeleteTags = Messages.Tag_Manager_Action_DeleteTag;
//
//      } else {
//
//         // remove multiple tags
//
//         dialogMessage = NLS.bind(Messages.Tag_Manager_Dialog_DeleteTag_Multiple_Message, allTags.size(), allTourIds.size());
//         actionDeleteTags = Messages.Tag_Manager_Action_DeleteTags;
//      }
//
//      final Display display = Display.getDefault();
//
//      // confirm deletion, show tag name and number of tours which contain a tag
//      final MessageDialog dialog = new MessageDialog(
//            display.getActiveShell(),
//            Messages.Tag_Manager_Dialog_DeleteTag_Title,
//            null,
//            dialogMessage,
//            MessageDialog.QUESTION,
//            new String[] {
//                  actionDeleteTags,
//                  IDialogConstants.CANCEL_LABEL },
//            1);

      final boolean[] returnValue = { false };

//      if (dialog.open() == Window.OK) {
//
//         BusyIndicator.showWhile(display, () -> {
//
//            if (deleteTourTag_10(allTags)) {
//
//               clearAllTagResourcesAndFireModifyEvent();
//
//               updateTourTagFilterProfiles(allTags);
//
//               returnValue[0] = true;
//            }
//         });
//      }

      return returnValue[0];
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
    * @return Returns a map with all equipments sorted by name
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

}
