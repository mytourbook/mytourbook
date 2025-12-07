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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.SQLData;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.data.TourData;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class EquipmentManager {

   private static final char                    NL      = UI.NEW_LINE;

   private static final Object                  DB_LOCK = new Object();

   private static volatile Map<Long, Equipment> _allEquipment_ByID;
   private static volatile List<Equipment>      _allEquipment_ByName;

   private static ConcurrentSkipListSet<String> _allEquipment_Brands;
   private static ConcurrentSkipListSet<String> _allEquipment_Models;
   private static ConcurrentSkipListSet<String> _allEquipment_PriceUnits;
   private static ConcurrentSkipListSet<String> _allPart_Brands;
   private static ConcurrentSkipListSet<String> _allPart_Models;
   private static ConcurrentSkipListSet<String> _allPart_PriceUnits;
   private static ConcurrentSkipListSet<String> _allService_Names;
   private static ConcurrentSkipListSet<String> _allService_PriceUnits;

   /**
    * Clear all equipment resources within MT and fire a equipment modify event, ensure that
    * {@link TourManager#isTourEditorModified()} <code>== false</code>
    */
   public static void clearAllEquipmentResourcesAndFireModifyEvent() {

      // remove old equpment from cached tours
      clearCachedValues();

      EquipmentMenuManager.clearRecentEquipment();

      TourManager.getInstance().clearTourDataCache();

      // fire modify event
      TourManager.fireEvent(TourEventId.EQUIPMENT_STRUCTURE_CHANGED);
   }

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

      if (_allEquipment_PriceUnits != null) {
         _allEquipment_PriceUnits.clear();
         _allEquipment_PriceUnits = null;
      }

      if (_allPart_Brands != null) {
         _allPart_Brands.clear();
         _allPart_Brands = null;
      }

      if (_allPart_Models != null) {
         _allPart_Models.clear();
         _allPart_Models = null;
      }

      if (_allPart_PriceUnits != null) {
         _allPart_PriceUnits.clear();
         _allPart_PriceUnits = null;
      }

      if (_allService_Names != null) {
         _allService_Names.clear();
         _allService_Names = null;
      }

      if (_allService_PriceUnits != null) {
         _allService_PriceUnits.clear();
         _allService_PriceUnits = null;
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

   /**
    * Deletes a equipment from all contained tours and in the equipment structure. This event
    * {@link TourEventId#EQUIPMENT_STRUCTURE_CHANGED} is fired when done
    *
    * @param allEquipment
    *
    * @return
    *
    * @return Returns <code>true</code> when deletion was successful
    */
   public static boolean equipment_Delete(final List<Equipment> allEquipment) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return false;
      }

      final SQLData sqlPartData = getSQLData_Parts(allEquipment);

      String dialogMessage;

      final List<Long> allTourIds = getEquipmentTours(allEquipment);

      if (allEquipment.size() == 1) {

         // remove one equipment

         dialogMessage = "Permanently delete equipment\n\n\"%s\"\n\nits %d parts and remove this equipment from %d tours ?".formatted(
               allEquipment.get(0).getName(),
               sqlPartData.getParameters().size(), // number of parts
               allTourIds.size());

      } else {

         // remove multiple equipment

         dialogMessage = "Permanently delete %d equipment, theirs %d parts\nand remove them from %d tours ?".formatted(
               allEquipment.size(),
               sqlPartData.getParameters().size(), // number of parts
               allTourIds.size());
      }

      final Display display = Display.getDefault();

      // confirm deletion, show equipment name and number of tours which contain a equipment
      final MessageDialog dialog = new MessageDialog(
            display.getActiveShell(),
            "Delete Equipment",
            null,
            dialogMessage,
            MessageDialog.QUESTION,
            new String[] {
                  "&Delete Equipment",
                  IDialogConstants.CANCEL_LABEL },
            1);

      final boolean[] returnValue = { false };

      if (dialog.open() == Window.OK) {

         BusyIndicator.showWhile(display, () -> {

            if (equipment_Delete_SQL(allEquipment, sqlPartData)) {

               clearAllEquipmentResourcesAndFireModifyEvent();

//               updateTourTagFilterProfiles(allEquipment);

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean equipment_Delete_SQL(final List<Equipment> allEquipment, final SQLData sqlPartData) {

      final boolean isPartAvailable = sqlPartData.getParameters().size() > 0;

      boolean returnResult = false;

      String sql;

      PreparedStatement prepStmt_TourData = null;
      PreparedStatement prepStmt_Equipment = null;
      PreparedStatement prepStmt_EquipmentPart = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove equipment from "TOURDATA_Equipment"
         sql = "DELETE" + NL //                                                     //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + NL //     //$NON-NLS-1$
               + " WHERE " + TourDatabase.KEY_EQUIPMENT + "=?" + NL //              //$NON-NLS-1$ //$NON-NLS-2$
         ;

         prepStmt_TourData = conn.prepareStatement(sql);

         // remove equipment from table "Equipment"
         sql = "DELETE" + NL//                                                      //$NON-NLS-1$
               + " FROM " + TourDatabase.TABLE_EQUIPMENT + NL //                    //$NON-NLS-1$
               + " WHERE " + TourDatabase.ENTITY_ID_EQUIPMENT + "=?" + NL //        //$NON-NLS-1$ //$NON-NLS-2$
         ;
         prepStmt_Equipment = conn.prepareStatement(sql);

         if (isPartAvailable) {

            // remove parts from table "EquipmentPart"
            sql = "DELETE" + NL//                                                   //$NON-NLS-1$
                  + " FROM " + TourDatabase.TABLE_EQUIPMENT_PART + NL //            //$NON-NLS-1$

                  // "EquipmentPart.partID IN (" + parameterList + ")"
                  + " WHERE " + sqlPartData.getSqlString() + NL //                  //$NON-NLS-1$
            ;
            prepStmt_EquipmentPart = conn.prepareStatement(sql);
         }

         int[] returnValue_TourData;
         int[] returnValue_Equipment;
         int[] returnValue_EquipmentPart = null;

         conn.setAutoCommit(false);
         {
            for (final Equipment equipment : allEquipment) {

               final long equipmentID = equipment.getEquipmentId();

               prepStmt_TourData.setLong(1, equipmentID);
               prepStmt_TourData.addBatch();

               prepStmt_Equipment.setLong(1, equipmentID);
               prepStmt_Equipment.addBatch();

               if (isPartAvailable) {
                  sqlPartData.setParameters(prepStmt_EquipmentPart, 1);
                  prepStmt_EquipmentPart.addBatch();
               }
            }

            returnValue_TourData = prepStmt_TourData.executeBatch();
            returnValue_Equipment = prepStmt_Equipment.executeBatch();

            if (isPartAvailable) {
               returnValue_EquipmentPart = prepStmt_EquipmentPart.executeBatch();
            }
         }
         conn.commit();

         // log result
         TourLogManager.showLogView(AutoOpenEvent.DELETE_SOMETHING);

         final int numEquipment = allEquipment.size();

         for (int equipmentIndex = 0; equipmentIndex < numEquipment; equipmentIndex++) {

            final int partResult = returnValue_EquipmentPart == null
                  ? 0
                  : returnValue_EquipmentPart[equipmentIndex];

            TourLogManager.log_INFO("Equipment is deleted from %d tours, %d equipment definition and %d equipment parts - \"%s\"".formatted(
                  returnValue_TourData[equipmentIndex],
                  returnValue_Equipment[equipmentIndex],
                  partResult,
                  allEquipment.get(equipmentIndex).getName()));
         }

         returnResult = true;

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt_TourData);
         Util.closeSql(prepStmt_Equipment);
         Util.closeSql(prepStmt_EquipmentPart);
      }

      return returnResult;
   }

   /**
    * Remove equipment
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

   public static ConcurrentSkipListSet<String> getCachedFields_AllEquipment_PriceUnits() {

      if (_allEquipment_PriceUnits == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allEquipment_PriceUnits == null) {

               _allEquipment_PriceUnits = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT, "priceUnit"); //$NON-NLS-1$
            }
         }
      }

      return _allEquipment_PriceUnits;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllPart_Brands() {

      if (_allPart_Brands == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allPart_Brands == null) {

               _allPart_Brands = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT_PART, "brand"); //$NON-NLS-1$
            }
         }
      }

      return _allPart_Brands;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllPart_Models() {

      if (_allPart_Models == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allPart_Models == null) {

               _allPart_Models = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT_PART, "model"); //$NON-NLS-1$
            }
         }
      }

      return _allPart_Models;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllPart_PriceUnits() {

      if (_allPart_PriceUnits == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allPart_PriceUnits == null) {

               _allPart_PriceUnits = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT_PART, "priceUnit"); //$NON-NLS-1$
            }
         }
      }

      return _allPart_PriceUnits;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllService_Names() {

      if (_allService_Names == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allService_Names == null) {

               _allService_Names = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT_SERVICE, "name"); //$NON-NLS-1$
            }
         }
      }

      return _allService_Names;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllService_PriceUnits() {

      if (_allService_PriceUnits == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allService_PriceUnits == null) {

               _allService_PriceUnits = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT_SERVICE, "priceUnit"); //$NON-NLS-1$
            }
         }
      }

      return _allService_PriceUnits;
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

   /**
    * Get all tours for a equipment ID
    *
    * @param allEquipment
    *
    * @return Returns a list with all tour id's which contain the tour equipment
    */
   private static List<Long> getEquipmentTours(final List<Equipment> allEquipment) {

      final List<Long> allTourIds = new ArrayList<>();

      final List<Long> sqlParameters = new ArrayList<>();
      final StringBuilder sqlParameterPlaceholder = new StringBuilder();

      boolean isFirst = true;

      for (final Equipment equipment : allEquipment) {

         if (isFirst) {
            isFirst = false;
            sqlParameterPlaceholder.append(TourDatabase.PARAMETER_FIRST);
         } else {
            sqlParameterPlaceholder.append(TourDatabase.PARAMETER_FOLLOWING);
         }

         sqlParameters.add(equipment.getEquipmentId());
      }

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                                                           //$NON-NLS-1$

            + " DISTINCT TourData.tourId" + NL //                                                        //$NON-NLS-1$

            + " FROM " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " JTdataTequipment " + NL //      //$NON-NLS-1$ //$NON-NLS-2$

            // get all tours for current equipment
            + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData" + NL //                   //$NON-NLS-1$ //$NON-NLS-2$
            + " ON JTdataTequipment.TourData_tourId = TourData.tourId " + NL //                          //$NON-NLS-1$

            + " WHERE JTdataTequipment.Equipment_equipmentID IN (" + sqlParameterPlaceholder.toString() + ")" + NL //$NON-NLS-1$ //$NON-NLS-2$

            + " ORDER BY tourId" + NL //                                                                 //$NON-NLS-1$
      ;

      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         statement = conn.prepareStatement(sql);

         // fillup parameter
         for (int parameterIndex = 0; parameterIndex < sqlParameters.size(); parameterIndex++) {
            statement.setLong(parameterIndex + 1, sqlParameters.get(parameterIndex));
         }

         final ResultSet result = statement.executeQuery();
         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         net.tourbook.ui.UI.showSQLException(e);

      } finally {
         Util.closeSql(statement);
      }

      return allTourIds;
   }

   private static SQLData getSQLData_Parts(final List<Equipment> allEquipment) {

      // collect all part IDs
      final List<Object> allPartIDs = new ArrayList<>();

      for (final Equipment equipment : allEquipment) {

         final Set<EquipmentPart> allEquipmentParts = equipment.getParts();

         for (final EquipmentPart equipmentPart : allEquipmentParts) {

            allPartIDs.add(equipmentPart.getPartId());
         }
      }

      final int numIDs = allPartIDs.size();
      final String parameterList = SQL.createParameterList(numIDs);

      final String sqlStatement = "EquipmentPart.partID IN (" + parameterList + ")"; //$NON-NLS-1$ //$NON-NLS-2$

      return new SQLData(sqlStatement, allPartIDs);
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

   /**
    * @param tourData
    * @param equipmentLabel
    * @param isVertical
    *           When <code>true</code> then the equipment are displayed as a list, otherwise
    *           horizontally
    */
   public static void updateUI_Equipment(final TourData tourData, final Label equipmentLabel, final boolean isVertical) {

      final Set<Equipment> allEquipment = tourData.getEquipment();

      if (allEquipment == null || allEquipment.isEmpty()) {

         equipmentLabel.setText(UI.EMPTY_STRING);

      } else {

         final String equipmentLabels = getEquipmentNames(allEquipment, isVertical);

         equipmentLabel.setText(equipmentLabels);
         equipmentLabel.setToolTipText(equipmentLabels);
      }
   }
}
