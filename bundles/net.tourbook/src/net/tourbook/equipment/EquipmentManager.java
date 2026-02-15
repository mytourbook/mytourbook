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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.ImageUtils;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.SQLData;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.data.EquipmentPart;
import net.tourbook.data.TourData;
import net.tourbook.database.ITourDataUpdate_OnlyUpdate;
import net.tourbook.database.MyTourbookException;
import net.tourbook.database.TourDatabase;
import net.tourbook.tag.TagManager;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourLogManager.AutoOpenEvent;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.ui.ITourProviderByID;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.internal.DPIUtil;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

public class EquipmentManager {

   private static final char                 NL                          = UI.NEW_LINE;

   private static final Object               DB_LOCK                     = new Object();

   public static final int                   EXPAND_TYPE_FLAT            = 0;
   public static final int                   EXPAND_TYPE_YEAR_TOUR       = 1;
   public static final int                   EXPAND_TYPE_YEAR_MONTH_TOUR = 2;

   static final String[]                     EXPAND_TYPE_NAMES           = {

         Messages.app_action_expand_type_flat,
         Messages.app_action_expand_type_year_day,
         Messages.app_action_expand_type_year_month_day
   };

   /**
    * The EXPAND_TYPE_... value is the index for these labels
    */
   static final String[]                     EXPAND_TYPE_LABEL           = {

         "Sort By Date",
         "By Year",
         "By Year/Month"
   };

   static final int[]                        EXPAND_TYPES                = {

         EXPAND_TYPE_FLAT,
         EXPAND_TYPE_YEAR_TOUR,
         EXPAND_TYPE_YEAR_MONTH_TOUR
   };

   private static int                        _equipmentImageSize_View;

   /**
    * Key is the image file path
    */
   private static final Cache<String, Image> _imageCache_Content;
   private static final Cache<String, Image> _imageCache_View;

   static {

      restoreEquipmentContentValues();

   }
   static {

      final RemovalListener<String, Image> removalListener = new RemovalListener<>() {

         final ExecutorService executor = Executors.newSingleThreadExecutor();

         @Override
         public void onRemoval(final String fileName,
                               final Image image,
                               final RemovalCause removalCause) {

            executor.submit(new Callable<Void>() {
               @Override
               public Void call() throws IOException {

                  // dispose cached image
                  UI.disposeResource(image);

                  return null;
               }
            });
         }
      };

      _imageCache_Content = Caffeine.newBuilder()
            .maximumSize(20)
            .removalListener(removalListener)
            .build();

      _imageCache_View = Caffeine.newBuilder()
            .maximumSize(100)
            .removalListener(removalListener)
            .build();
   }

   private static volatile Map<Long, Equipment>     _allEquipment_ByID;
   private static volatile List<Equipment>          _allEquipment_ByName;
   private static volatile Map<Long, EquipmentPart> _allParts_ByID;

   private static ConcurrentSkipListSet<String>     _allBrands;
   private static ConcurrentSkipListSet<String>     _allCompanies;
   private static ConcurrentSkipListSet<String>     _allModels;
   private static ConcurrentSkipListSet<String>     _allPriceUnits;
   private static ConcurrentSkipListSet<String>     _allServiceNames;
   private static ConcurrentSkipListSet<String>     _allSizes;
   private static ConcurrentSkipListSet<String>     _allTypes;

   private static final List<EquipmentUIContent>    _allEquipmentUIContainer = new ArrayList<>();

   private static class EquipmentUIContent {

      Composite container;

      Label     label1;
      Label     label2;
   }

   /**
    * Clear all equipment resources and tours within MT and fire a equipment modify event, ensure
    * that {@link TourManager#isTourEditorModified()} <code>== false</code>
    */
   public static void clearAllEquipmentResourcesAndFireModifyEvent() {

      // remove old equipment from cached tours
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

      if (_allParts_ByID != null) {
         _allParts_ByID.clear();
         _allParts_ByID = null;
      }

      if (_allBrands != null) {
         _allBrands.clear();
         _allBrands = null;
      }

      if (_allCompanies != null) {
         _allCompanies.clear();
         _allCompanies = null;
      }

      if (_allModels != null) {
         _allModels.clear();
         _allModels = null;
      }

      if (_allPriceUnits != null) {
         _allPriceUnits.clear();
         _allPriceUnits = null;
      }

      if (_allServiceNames != null) {
         _allServiceNames.clear();
         _allServiceNames = null;
      }

      if (_allSizes != null) {
         _allSizes.clear();
         _allSizes = null;
      }

      if (_allTypes != null) {
         _allTypes.clear();
         _allTypes = null;
      }
   }

   private static SQLData createSQLEquipmentParameters(final Set<Equipment> allEquipment) {

      // collect all ids
      final List<Object> allEquipmentIDs = new ArrayList<>();

      for (final Equipment equipment : allEquipment) {
         allEquipmentIDs.add(equipment.getEquipmentId());
      }

      final int numIDs = allEquipmentIDs.size();
      final String sqlString = SQL.createParameterList(numIDs);

      return new SQLData(sqlString, allEquipmentIDs);
   }

   private static SQLData createSQLPartParameters(final List<Equipment> allEquipment) {

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

   /**
    * Dispose all images
    */
   public static void disposeAllEquipmentImages() {

      _imageCache_Content.invalidateAll();
      _imageCache_View.invalidateAll();
   }

   public static void disposeEquipmentUIContent() {

      _allEquipmentUIContainer.forEach(tagUIContent -> tagUIContent.container.dispose());

      _allEquipmentUIContainer.clear();
   }

   public static void equipment_Add(final Collection<Equipment> allEquipment,
                                    final ITourProvider tourProvider,
                                    final boolean isSaveTour,
                                    final boolean isCheckTourEditor) {

      // fix https://github.com/mytourbook/mytourbook/issues/1437
      if (isCheckTourEditor) {

         if (TourManager.isTourEditorModified()) {
            return;
         }
      }

      final ITourDataUpdate_OnlyUpdate tourDataUpdater = new ITourDataUpdate_OnlyUpdate() {

         @Override
         public boolean updateTourData(final TourData tourData) {

            final Set<Equipment> allTourEquipment = tourData.getEquipment();

            boolean isAdded = false;

            for (final Equipment equipment : allEquipment) {

               final boolean isAddedOneTour = allTourEquipment.add(equipment);

               if (isAddedOneTour) {
                  isAdded = true;
               }
            }

            return isAdded;
         }
      };

      updateTours(tourProvider, tourDataUpdater, isSaveTour);
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

      final ITourDataUpdate_OnlyUpdate tourDataUpdater = new ITourDataUpdate_OnlyUpdate() {

         @Override
         public boolean updateTourData(final TourData tourData) {

            final Set<Equipment> allEquipment = tourData.getEquipment();

            final boolean isAdded = allEquipment.add(equipment);

            return isAdded;
         }
      };

      updateTours(tourProvider, tourDataUpdater, isSaveTour);
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
   public static boolean equipment_DeleteEquipment(final List<Equipment> allEquipment) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified()) {
         return false;
      }

      final SQLData sqlPartData = createSQLPartParameters(allEquipment);

      String dialogMessage;

      final List<Long> allTourIds = getEquipmentTours(allEquipment);

      if (allEquipment.size() == 1) {

         // remove one equipment

         dialogMessage = "Permanently delete equipment\n\n\"%s\"\n\nits %d parts and services, and remove this equipment from %d tours ?".formatted(
               allEquipment.get(0).getName(),
               sqlPartData.getParameters().size(), // number of parts
               allTourIds.size());

      } else {

         // remove multiple equipment

         dialogMessage = "Permanently delete %d equipment, theirs %d parts\nand services and remove them from %d tours ?".formatted(
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

            if (equipment_DeleteEquipment_SQL(allEquipment, sqlPartData)) {

               clearAllEquipmentResourcesAndFireModifyEvent();

//               updateTourTagFilterProfiles(allEquipment);

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean equipment_DeleteEquipment_SQL(final List<Equipment> allEquipment,
                                                        final SQLData sqlPartData) {

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

            final int partResult = returnValue_EquipmentPart == null ? 0 : returnValue_EquipmentPart[equipmentIndex];

            TourLogManager.log_INFO(
                  "Equipment is deleted from %d tours, %d equipment definition, %d equipment parts and/or services - \"%s\"".formatted(
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

   public static boolean equipment_DeleteParts(final List<EquipmentPart> allParts) {

      // ensure that a tour is NOT modified in the tour editor
      if (TourManager.isTourEditorModified() || allParts.size() != 1) {
         return false;
      }

      final EquipmentPart part = allParts.get(0);

      final Display display = Display.getDefault();

      String dialogMessage = null;
      String dialogTitle = null;
      String okButtonText = null;

      if (part.isItemType_Part()) {

         dialogMessage = "Permanently delete part\n\n\"%s\" ?".formatted(part.getName());

         dialogTitle = "Delete Part";
         okButtonText = "&Delete Part";

      } else if (part.isItemType_Service()) {

         dialogMessage = "Permanently delete service\n\n\"%s\" ?".formatted(part.getName());

         dialogTitle = "Delete Service";
         okButtonText = "&Delete Service";
      }

      if (dialogTitle == null) {
         return false;
      }

      // confirm deletion, show equipment name and number of tours which contain a equipment
      final MessageDialog dialog = new MessageDialog(
            display.getActiveShell(),
            dialogTitle,
            null,
            dialogMessage,
            MessageDialog.QUESTION,
            new String[] {
                  okButtonText,
                  IDialogConstants.CANCEL_LABEL },
            1);

      final boolean[] returnValue = { false };

      if (dialog.open() == Window.OK) {

         BusyIndicator.showWhile(display, () -> {

            if (equipment_DeleteParts_SQL(part)) {

               final Set<String> allTypes = new HashSet<>(Arrays.asList(part.getType()));

               updateUntilDate_Parts(part.getEquipment(), allTypes);

               clearAllEquipmentResourcesAndFireModifyEvent();

               returnValue[0] = true;
            }
         });
      }

      return returnValue[0];
   }

   private static boolean equipment_DeleteParts_SQL(final EquipmentPart part) {

      boolean returnResult = false;

      PreparedStatement prepStmt = null;
      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         // remove part from table "EquipmentPart"

         sql = "DELETE FROM " + TourDatabase.TABLE_EQUIPMENT_PART + " WHERE partID=?" + NL; //$NON-NLS-1$

         final long partID = part.getPartId();

         prepStmt = conn.prepareStatement(sql);
         prepStmt.setLong(1, partID);
         prepStmt.execute();

         // log result
         TourLogManager.showLogView(AutoOpenEvent.DELETE_SOMETHING);
         TourLogManager.log_INFO("Equipment part is deleted \"%s\"".formatted(part.getName()));

         returnResult = true;

      } catch (final SQLException e) {

         StatusUtil.log(sql, e);
         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStmt);
      }

      return returnResult;
   }

   /**
    * Remove one equipment from the selected tours
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

      final ITourDataUpdate_OnlyUpdate tourDataUpdater = new ITourDataUpdate_OnlyUpdate() {

         @Override
         public boolean updateTourData(final TourData tourData) {

            final Set<Equipment> allEquipment = tourData.getEquipment();

            final boolean isRemoved = allEquipment.remove(equipment);

            return isRemoved;
         }
      };

      updateTours(tourProvider, tourDataUpdater, isSaveTour);
   }

   /**
    * Remove all equipment from the selected tours
    *
    * @param tourProvider
    * @param isSaveTour
    * @param isCheckTourEditor
    */
   public static void equipment_RemoveAll(final ITourProvider tourProvider,
                                          final boolean isSaveTour,
                                          final boolean isCheckTourEditor) {

      // fix https://github.com/mytourbook/mytourbook/issues/1437
      if (isCheckTourEditor) {

         if (TourManager.isTourEditorModified()) {
            return;
         }
      }

      final ITourDataUpdate_OnlyUpdate tourDataUpdater = new ITourDataUpdate_OnlyUpdate() {

         @Override
         public boolean updateTourData(final TourData tourData) {

            final Set<Equipment> allEquipment = tourData.getEquipment();

            if (allEquipment.size() == 0) {
               return false;
            }

            allEquipment.clear();

            return true;
         }
      };

      updateTours(tourProvider, tourDataUpdater, isSaveTour);
   }

   /**
    * @param allEquipment
    *
    * @return Returns a map were the key is the equipment ID and the value is the multiline detailed
    *         text
    */
   public static Map<Long, String> fetchEquipmentAccumulatedValues(final Set<Equipment> allEquipment) {

      final SQLData sqlTagData = createSQLEquipmentParameters(allEquipment);

      final String sqlQuery = UI.EMPTY_STRING

            + "--" + NL //                                                                         //$NON-NLS-1$
            + NL
            + "--------------------------------" + NL //                                           //$NON-NLS-1$
            + "-- equipment - tours accumulated" + NL //                                           //$NON-NLS-1$
            + "--------------------------------" + NL //                                           //$NON-NLS-1$
            + NL

            + "SELECT" + NL //                                                                     //$NON-NLS-1$

            + "   jTdEq.Equipment_EquipmentID," + NL //                                         1  //$NON-NLS-1$
            + "   SUM(tourData.TOURDISTANCE)             AS TOTALDISTANCE," + NL //             2  //$NON-NLS-1$
            + "   SUM(tourData.TOURDEVICETIME_RECORDED)  AS TOTALRECORDEDTIME" + NL //          3  //$NON-NLS-1$

            + "FROM " + TourDatabase.JOINTABLE__TOURDATA__EQUIPMENT + " AS jTdEq" + NL //          //$NON-NLS-1$ //$NON-NLS-2$

            + "JOIN " + TourDatabase.TABLE_TOUR_DATA + " AS TourData"//                            //$NON-NLS-1$
            + " ON jTdEq.TourData_TourID = TourData.TOURID" + NL //                                //$NON-NLS-1$

            + "WHERE jTdEq.Equipment_EquipmentID IN (" + sqlTagData.getSqlString() + ")" + NL //   //$NON-NLS-1$

            + "GROUP BY jTdEq.Equipment_EquipmentID" + NL //                                       //$NON-NLS-1$

            + NL
            + "--" + NL //                                                                         //$NON-NLS-1$
      ;

      final Map<Long, String> allAccumulatedValues = new HashMap<>();

      try (Connection connection = TourDatabase.getInstance().getConnection();
            final PreparedStatement preparedStatement = connection.prepareStatement(sqlQuery)) {

         sqlTagData.setParameters(preparedStatement, 1);

         final ResultSet result = preparedStatement.executeQuery();

         while (result.next()) {

            final long tagId = result.getLong(1);
            final float distance = result.getLong(2);
            final long timeRecorded = result.getLong(3);

            final float distanceConverted = distance / 1000 / net.tourbook.common.UI.UNIT_VALUE_DISTANCE;

            final StringBuilder sb = new StringBuilder();

            sb.append(Math.round(timeRecorded / 3600f));
            sb.append(UI.SPACE);
            sb.append(net.tourbook.common.UI.UNIT_LABEL_TIME);

            sb.append(NL);

            sb.append(Math.round(distanceConverted));
            sb.append(UI.SPACE);
            sb.append(net.tourbook.common.UI.UNIT_LABEL_DISTANCE);

            allAccumulatedValues.put(tagId, sb.toString());
         }

      } catch (final SQLException e) {

         SQL.showException(e, sqlQuery);
      }

      return allAccumulatedValues;
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

   /**
    * @return Returns {@link #_allParts_ByID}
    */
   public static Map<Long, EquipmentPart> getAllParts_ByID() {

      if (_allParts_ByID != null) {
         return _allParts_ByID;
      }

      loadEquipment();

      return _allParts_ByID;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllBrands() {

      if (_allBrands == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allBrands == null) {

               _allBrands = TourDatabase.getDistinctValues(

                     "brand", //$NON-NLS-1$

                     TourDatabase.TABLE_EQUIPMENT,
                     TourDatabase.TABLE_EQUIPMENT_PART);
            }
         }
      }

      return _allBrands;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllCompanies() {

      if (_allCompanies == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allCompanies == null) {

               _allCompanies = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT_PART, "company"); //$NON-NLS-1$
            }
         }
      }

      return _allCompanies;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllModels() {

      if (_allModels == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allModels == null) {

               _allModels = TourDatabase.getDistinctValues(

                     "model", //$NON-NLS-1$

                     TourDatabase.TABLE_EQUIPMENT,
                     TourDatabase.TABLE_EQUIPMENT_PART);
            }
         }
      }

      return _allModels;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllPriceUnits() {

      if (_allPriceUnits == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allPriceUnits == null) {

               _allPriceUnits = TourDatabase.getDistinctValues(

                     "priceUnit", //$NON-NLS-1$

                     TourDatabase.TABLE_EQUIPMENT,
                     TourDatabase.TABLE_EQUIPMENT_PART);
            }
         }
      }

      return _allPriceUnits;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllServiceNames() {

      if (_allServiceNames == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allServiceNames == null) {

               _allServiceNames = TourDatabase.getDistinctValues(TourDatabase.TABLE_EQUIPMENT_PART, "name"); //$NON-NLS-1$
            }
         }
      }

      return _allServiceNames;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllSizes() {

      if (_allSizes == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allSizes == null) {

               _allSizes = TourDatabase.getDistinctValues(

                     "size", //$NON-NLS-1$

                     TourDatabase.TABLE_EQUIPMENT,
                     TourDatabase.TABLE_EQUIPMENT_PART);
            }
         }
      }

      return _allSizes;
   }

   public static ConcurrentSkipListSet<String> getCachedFields_AllTypes() {

      if (_allTypes == null) {

         synchronized (DB_LOCK) {

            // recheck again, another thread could have it created
            if (_allTypes == null) {

               _allTypes = TourDatabase.getDistinctValues(

                     "type", //$NON-NLS-1$

                     TourDatabase.TABLE_EQUIPMENT,
                     TourDatabase.TABLE_EQUIPMENT_PART);
            }
         }
      }

      return _allTypes;
   }

   /**
    * For a given image file path, try to retrieve the already created
    * Image resource from the cache.
    * Otherwise, create an image resource, and put it in the cache
    *
    * @param equipment
    *
    * @return Return the equipment image or <code>null</code> when not available
    */
   public static Image getEquipmentImage(final Equipment equipment) {

      final String imageFilePath = equipment.getImageFilePath();

      if (StringUtils.isNullOrEmpty(imageFilePath)) {
         return null;
      }

      Image equipmentImage = _imageCache_Content.getIfPresent(imageFilePath);

      if (equipmentImage == null) {

         try {

            equipmentImage = ImageUtils.createImage(imageFilePath, TagManager.getTagContent_ImageSize());

         } catch (final IOException e) {

            return null;
         }

         if (equipmentImage != null) {
            _imageCache_Content.put(imageFilePath, equipmentImage);
         }
      }

      return equipmentImage;
   }

   /**
    * For a given image file path, try to retrieve the already created
    * Image resource from the cache.
    * Otherwise, create an image resource, and put it in the cache
    *
    * @param imageFilePath
    * @param imageSize
    *
    * @return Return the equipment image or <code>null</code> when not available
    *
    * @throws IOException
    *            This exceptions is thrown when the image could not be loaded
    */
   public static Image getEquipmentImage(final String imageFilePath,
                                         final ImageSize imageSizeType) throws IOException {

      if (StringUtils.isNullOrEmpty(imageFilePath)) {
         return null;
      }

      final int deviceZoom = DPIUtil.getDeviceZoom();
      final float deviceScale = deviceZoom / 100.0f;

      if (imageSizeType == ImageSize.CONTENT) {

         Image equipmentImage = _imageCache_Content.getIfPresent(imageFilePath);

         if (equipmentImage == null) {

            final int imageSizeScaled = (int) (TagManager.getTagContent_ImageSize() * deviceScale);

            equipmentImage = ImageUtils.createImage(imageFilePath, imageSizeScaled);

            if (equipmentImage != null) {
               _imageCache_Content.put(imageFilePath, equipmentImage);
            }
         }

         return equipmentImage;

      } else if (imageSizeType == ImageSize.VIEW) {

         Image equipmentImage = _imageCache_View.getIfPresent(imageFilePath);

         if (equipmentImage == null) {

            final int imageSizeScaled = (int) (_equipmentImageSize_View * deviceScale);

            equipmentImage = ImageUtils.createImage(imageFilePath, imageSizeScaled, true);

            if (equipmentImage != null) {
               _imageCache_View.put(imageFilePath, equipmentImage);
            }
         }

         return equipmentImage;
      }

      return null;
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

      PreparedStatement prepStatement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         prepStatement = conn.prepareStatement(sql);

         // fillup parameter
         for (int parameterIndex = 0; parameterIndex < sqlParameters.size(); parameterIndex++) {
            prepStatement.setLong(parameterIndex + 1, sqlParameters.get(parameterIndex));
         }

         final ResultSet result = prepStatement.executeQuery();
         while (result.next()) {
            allTourIds.add(result.getLong(1));
         }

      } catch (final SQLException e) {

         StatusUtil.logError(sql);
         net.tourbook.ui.UI.showSQLException(e);

      } finally {

         Util.closeSql(prepStatement);
      }

      return allTourIds;
   }

   private static void loadEquipment() {

      synchronized (DB_LOCK) {

         // check again, field must be volatile to work correctly
         if (_allEquipment_ByID != null) {
            return;
         }

         final Map<Long, Equipment> allEquipments_ByID = new HashMap<>();
         final Map<Long, EquipmentPart> allParts_ByID = new HashMap<>();

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

                  for (final EquipmentPart part : equipment.getParts()) {

                     allParts_ByID.put(part.getPartId(), part);
                  }
               }
            }

            em.close();
         }

         _allEquipment_ByID = allEquipments_ByID;
         _allEquipment_ByName = allEquipments_ByName;

         _allParts_ByID = allParts_ByID;
      }
   }

   private static void restoreEquipmentContentValues() {

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

   public static void setEquipmentImageSize_View(final int imageSize) {

      if (imageSize != _equipmentImageSize_View) {

         // the image size was modified -> dispose images with the wrong size
         _imageCache_View.invalidateAll();

         // set new image size
         _equipmentImageSize_View = imageSize;
      }
   }

   public static void updateEquipmentContent() {

      // dispose equipment content
      _imageCache_Content.invalidateAll();
      disposeEquipmentUIContent();
   }

   /**
    * Update and save tours
    *
    * @param tourProvider
    * @param tourDataUpdater
    * @param isSaveTour
    */
   private static void updateTours(final ITourProvider tourProvider,
                                   final ITourDataUpdate_OnlyUpdate tourDataUpdater,
                                   final boolean isSaveTour) {

      if (tourProvider instanceof final ITourProviderByID tourProviderByID
            && isSaveTour) {

         // this is the FAST update method

         final Set<Long> selectedTourIDs = tourProviderByID.getSelectedTourIDs();

         // save all tours with the added equipment
         TourManager.updateTourData_Concurrent(selectedTourIDs, tourDataUpdater);

      } else {

         // this is the SLOW update method

         final ArrayList<TourData> allSelectedTours = tourProvider.getSelectedTours();

         if (allSelectedTours != null && allSelectedTours.size() > 0) {

            for (final TourData tourData : allSelectedTours) {

               tourDataUpdater.updateTourData(tourData);

               saveAndNotify(tourProvider, isSaveTour, allSelectedTours);
            }
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
   public static void updateUI_Equipment(final TourData tourData,
                                         final Label equipmentLabel,
                                         final boolean isVertical) {

      final Set<Equipment> allEquipment = tourData.getEquipment();

      if (allEquipment == null || allEquipment.isEmpty()) {

         equipmentLabel.setText(UI.EMPTY_STRING);

      } else {

         final String equipmentLabels = getEquipmentNames(allEquipment, isVertical);

         equipmentLabel.setText(equipmentLabels);
         equipmentLabel.setToolTipText(equipmentLabels);
      }
   }

   public static void updateUI_EquipmentWithImage(final PixelConverter pc,
                                                  final Set<Equipment> allEquipment,
                                                  final Composite contentContainer) {

      final int numEquipment = allEquipment.size();

      if (numEquipment == 0) {
         return;
      }

      final Equipment[] allEquipmentSorted = allEquipment.toArray(new Equipment[numEquipment]);

      // sort equipment by name
      Arrays.sort(allEquipmentSorted);

      // update number of equipment content columns
      ((GridLayout) contentContainer.getLayout()).numColumns = TagManager.getTagContent_NumContentColumns();

      // create missing equipment UI container
      updateUI_EquipmentWithImages_CreateUIContainer(pc, contentContainer, numEquipment);

      /*
       * Check if any equipment images are available
       */
      boolean isAnyEquipmentImageAvailable = false;

      for (final Equipment equipment : allEquipmentSorted) {

         final Image equipmentImage = getEquipmentImage(equipment);

         if (equipmentImage != null) {

            isAnyEquipmentImageAvailable = true;

            break;
         }
      }

      /*
       * Fill equipment content
       */
      final Map<Long, String> allEquipmentAccumulatedValues = fetchEquipmentAccumulatedValues(allEquipment);
      final List<EquipmentUIContent> allNotNeededEquipment = new ArrayList<>();

      final GridDataFactory gd = GridDataFactory.fillDefaults();

      for (int equipmentIndex = 0; equipmentIndex < _allEquipmentUIContainer.size(); equipmentIndex++) {

         final EquipmentUIContent equipmentUIContent = _allEquipmentUIContainer.get(equipmentIndex);

         if (equipmentIndex < numEquipment) {

            final Equipment equipment = allEquipmentSorted[equipmentIndex];
            final long equipmentId = equipment.getEquipmentId();

            final String equipmentText = equipment.getName() + UI.NEW_LINE

                  + allEquipmentAccumulatedValues.get(equipmentId);

            final Label label1 = equipmentUIContent.label1;
            final Label label2 = equipmentUIContent.label2;

            if (isAnyEquipmentImageAvailable) {

               // 1st label shows the equipment image
               // 2nd label shows the equipment text

               final Image equipmentImage = getEquipmentImage(equipment);

               label1.setText(UI.EMPTY_STRING);

               // !!! IMPORTANT: image must be set AFTER the text, otherwise the image is not displayed !!!
               label1.setImage(equipmentImage);

               label2.setVisible(true);
               label2.setText(equipmentText);

               gd.grab(false, false).hint(TagManager.getTagContent_ImageSize(), SWT.DEFAULT).applyTo(label1);
               gd.grab(true, false).applyTo(label2);

            } else {

               // 1st label shows the equipment text
               // 2nd label is hidden

               label1.setText(equipmentText);
               label1.setImage(null);

               label2.setVisible(false);
               label2.setText(UI.EMPTY_STRING);

               gd.grab(true, false).hint(SWT.DEFAULT, SWT.DEFAULT).applyTo(label1);
               gd.grab(false, false).applyTo(label2);
            }

         } else {

            // there are no more equipment -> dispose remaining UI container

            allNotNeededEquipment.add(equipmentUIContent);
         }
      }

      /*
       * Not used equipment UI container must be disposed and removed otherwise they still occupy UI
       * space
       * :-(
       */
      allNotNeededEquipment.forEach(equipmentUIContent -> {
         equipmentUIContent.container.dispose();
      });

      _allEquipmentUIContainer.removeAll(allNotNeededEquipment);
   }

   /**
    * Create missing equipment UI container
    *
    * @param pc
    * @param equipmentContainer
    * @param numEquipment
    */
   private static void updateUI_EquipmentWithImages_CreateUIContainer(final PixelConverter pc,
                                                                      final Composite equipmentContainer,
                                                                      final int numEquipment) {

      final int numMissingUIContainer = numEquipment - _allEquipmentUIContainer.size();

      if (numMissingUIContainer > 0) {

         final int equipmentContentWidth = 0
               + TagManager.getTagContent_ImageSize()
               + TagManager.getTagContent_TextWidth();

         final Color backgroundColor = equipmentContainer.getBackground();

         final GridDataFactory gdContainer = GridDataFactory.fillDefaults().hint(equipmentContentWidth, SWT.DEFAULT);

         for (int numCreated = 0; numCreated < numMissingUIContainer; numCreated++) {

            Label label1;
            Label label2;

            final EquipmentUIContent equipmentUIContent = new EquipmentUIContent();

            final Composite container = new Composite(equipmentContainer, SWT.NONE);
            gdContainer.applyTo(container);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
            {
               label1 = new Label(container, SWT.WRAP);
               GridDataFactory.fillDefaults().hint(TagManager.getTagContent_ImageSize(), SWT.DEFAULT).applyTo(label1);

               label2 = new Label(container, SWT.WRAP);
               GridDataFactory.fillDefaults().grab(true, false).applyTo(label2);

               equipmentUIContent.container = container;
               equipmentUIContent.label1 = label1;
               equipmentUIContent.label2 = label2;
            }

            container.setBackground(backgroundColor);
            label1.setBackground(backgroundColor);
            label2.setBackground(backgroundColor);

//            container.setBackground(net.tourbook.common.UI.SYS_COLOR_RED);
//            label1.setBackground(net.tourbook.common.UI.SYS_COLOR_GREEN);
//            label2.setBackground(net.tourbook.common.UI.SYS_COLOR_YELLOW);

            _allEquipmentUIContainer.add(equipmentUIContent);
         }
      }
   }

   /**
    * Check and update the until date for all equipment with the modified types and/or dates. This
    * has to be called when at least one collated field was modified.
    * <p>
    * <b> !!! After calling this method, all equipment must be reloaded because they are updated
    * !!! </b>
    *
    * @param allModifiedTypes
    */
   public static void updateUntilDate_Equipment(final Set<String> allModifiedTypes) {

      // force the reload of equipment because one of them was modified
      _allEquipment_ByID = null;

      final Map<Long, Equipment> allEquipment_ByID = getAllEquipment_ByID();

      for (final String modifiedType : allModifiedTypes) {

         final List<Equipment> allFilteredEquipment = new ArrayList<>();

         /*
          * Filter equipment: Get all equipment with the same type and which are collated
          */
         for (final Equipment equipment : allEquipment_ByID.values()) {

            if (equipment.isCollate()
                  && modifiedType.equalsIgnoreCase(equipment.getType())) {

               allFilteredEquipment.add(equipment);
            }
         }

         final int numEquipment = allFilteredEquipment.size();

         if (numEquipment == 0) {
            continue;
         }

         /*
          * Sort equipment by date
          */
         final List<Equipment> allSortedEquipment = new ArrayList<>(allFilteredEquipment);

         Collections.sort(allSortedEquipment, (equipment1, equipment2) -> {
            return Long.compare(equipment1.getDateFrom(), equipment2.getDateFrom());
         });

         final List<Equipment> allModifiedEquipment = new ArrayList<>();

         Equipment equipment = allSortedEquipment.get(0);

         if (numEquipment == 1) {

            // this is the first and only equipment

            final long currentDateUntil = equipment.getDateUntil();
            final long newDateUntil = TimeTools.MAX_TIME_IN_EPOCH_MILLI;

            if (currentDateUntil != newDateUntil) {

               // modify date

               equipment.setDateUntil(newDateUntil);

               allModifiedEquipment.add(equipment);
            }

         } else {

            // these are all other equipment with more than 1 equipment

            for (int equipmentIndex = 0; equipmentIndex < numEquipment; equipmentIndex++) {

               equipment = allSortedEquipment.get(equipmentIndex);

               final long currentDateUntil = equipment.getDateUntil();

               long newDateUntil = currentDateUntil;

               if (equipmentIndex == numEquipment - 1) {

                  // this is the last equipment

                  newDateUntil = TimeTools.MAX_TIME_IN_EPOCH_MILLI;

               } else {

                  // these are all equipment without the last equipment

                  final Equipment nextEquipment = allSortedEquipment.get(equipmentIndex + 1);

                  final long nextDate = nextEquipment.getDateFrom();

                  // this is the until date for the current equipment
                  final long validDateUntil = nextDate - 1;

                  if (currentDateUntil != validDateUntil) {

                     newDateUntil = validDateUntil;
                  }
               }

               if (currentDateUntil != newDateUntil) {

                  // until date is not correct -> modify date

                  equipment.setDateUntil(newDateUntil);

                  allModifiedEquipment.add(equipment);
               }
            }
         }

         if (allModifiedEquipment.size() > 0) {

            for (final Equipment modifiedEquipment : allModifiedEquipment) {

               TourDatabase.saveEntity(modifiedEquipment, modifiedEquipment.getEquipmentId(), Equipment.class);
            }
         }
      }
   }

   /**
    * Check and update the until date for all parts of one equipment.
    * This has to be called when at least one collated field was modified.
    * <p>
    * <b> !!! After calling this method, all equipment must be reloaded because they are updated
    * !!! </b>
    *
    * @param equipment
    * @param allModifiedTypes
    */
   public static void updateUntilDate_Parts(final Equipment equipment,
                                            final Set<String> allModifiedTypes) {

      final Set<EquipmentPart> allParts = equipment.getParts();
      final int numParts = allParts.size();

      if (numParts > 0) {

         for (final String type : allModifiedTypes) {

            updateUntilDate_Parts_One(allParts, type);
         }
      }
   }

   private static void updateUntilDate_Parts_One(final Set<EquipmentPart> allParts, final String modifiedType) {

      /*
       * Filter parts: Get all parts with the same type and which are collated
       */
      final List<EquipmentPart> allFilteredParts = new ArrayList<>();

      for (final EquipmentPart part : allParts) {

         if (part.isCollate()
               && modifiedType.equalsIgnoreCase(part.getType())) {

            allFilteredParts.add(part);
         }
      }

      final int numParts = allFilteredParts.size();

      if (numParts == 0) {
         return;
      }

      /*
       * Sort parts by date
       */
      final List<EquipmentPart> allSortedParts = new ArrayList<>(allFilteredParts);

      Collections.sort(allSortedParts, (part1, part2) -> {
         return Long.compare(part1.getDateFrom(), part2.getDateFrom());
      });

      final List<EquipmentPart> allModifiedParts = new ArrayList<>();

      EquipmentPart part = allSortedParts.get(0);

      if (numParts == 1) {

         // this is the first and only part

         final long currentDateUntil = part.getDateUntil();
         final long newDateUntil = TimeTools.MAX_TIME_IN_EPOCH_MILLI;

         if (currentDateUntil != newDateUntil) {

            // modify date

            part.setDateUntil(newDateUntil);

            allModifiedParts.add(part);
         }

      } else {

         // these are all other parts with more than 1 part

         for (int partIndex = 0; partIndex < numParts; partIndex++) {

            part = allSortedParts.get(partIndex);

            final long currentDateUntil = part.getDateUntil();

            long newDateUntil = currentDateUntil;

            if (partIndex == numParts - 1) {

               // this is the last part

               newDateUntil = TimeTools.MAX_TIME_IN_EPOCH_MILLI;

            } else {

               // these are all parts without the last part

               final EquipmentPart nextPart = allSortedParts.get(partIndex + 1);

               final long nextDate = nextPart.getDateFrom();

               // this is the until date for the current part
               final long validDateUntil = nextDate - 1;

               if (currentDateUntil != validDateUntil) {

                  newDateUntil = validDateUntil;
               }
            }

            if (currentDateUntil != newDateUntil) {

               // until date is not correct -> modify date

               part.setDateUntil(newDateUntil);

               allModifiedParts.add(part);
            }
         }
      }

      if (allModifiedParts.size() > 0) {

         for (final EquipmentPart modifiedPart : allModifiedParts) {

            TourDatabase.saveEntity(modifiedPart, modifiedPart.getPartId(), EquipmentPart.class);
         }
      }
   }
}
