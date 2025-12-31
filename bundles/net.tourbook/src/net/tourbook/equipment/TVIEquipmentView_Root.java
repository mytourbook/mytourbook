/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.Util;
import net.tourbook.data.Equipment;
import net.tourbook.database.TourDatabase;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.viewers.TreeViewer;

public class TVIEquipmentView_Root extends TVIEquipmentView_Item {

   /**
    * Key is equipmentID, partID and type
    */
   Map<String, SummarizedValues> allSummarizedPartValues;

   /**
    * Key is equipmentID, partID and type
    */
   Map<String, SummarizedValues> allSummarizedServiceValues;

   class SummarizedValues {

      long   equipmentID;
      long   partID;

      String type;

      int    numTours;
      float  distance;
      long   movingTime;
   }

   /**
    * @param equipmentViewer
    */
   public TVIEquipmentView_Root(final TreeViewer equipmentViewer) {

      super(equipmentViewer);

   }

   @Override
   @SuppressWarnings("unchecked")
   protected void fetchChildren() {

      allSummarizedPartValues = loadTourValuesFromParts();
      allSummarizedServiceValues = loadTourValuesFromServices();

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      {
         if (em == null) {
            return;
         }

         final Query query = em.createQuery(UI.EMPTY_STRING

               + "SELECT" + NL //                                                      //$NON-NLS-1$
               + " Equipment" + NL //                                                  //$NON-NLS-1$
               + " FROM " + Equipment.class.getSimpleName() + " AS Equipment" //       //$NON-NLS-1$ //$NON-NLS-2$
         );

         final TreeViewer equipmentViewer = getEquipmentViewer();
         final List<Equipment> allEquipments = query.getResultList();

         /*
          * Create equipment tree items
          */
         for (final Equipment equipment : allEquipments) {

            final TVIEquipmentView_Equipment equipmentItem = new TVIEquipmentView_Equipment(equipmentViewer, equipment);

            addChild(equipmentItem);
         }
      }
      em.close();
   }

   /**
    * Summarizes all tour values for each part and type
    *
    * @return
    */
   private Map<String, SummarizedValues> loadTourValuesFromParts() {

      final Map<String, SummarizedValues> allPartValues = new HashMap<>();

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   j_equip.EQUIPMENTID," + NL //                                              1  //$NON-NLS-1$

               + "   j_part.PARTID," + NL //                                                    2  //$NON-NLS-1$

               + "   parts_Summarized.part_type," + NL //                                       3  //$NON-NLS-1$
               + "   parts_Summarized.num_tours," + NL //                                       4  //$NON-NLS-1$
               + "   parts_Summarized.sum_distance," + NL //                                    5  //$NON-NLS-1$
               + "   parts_Summarized.sum_moving_time" + NL //                                  6  //$NON-NLS-1$

               + "FROM" + NL //                                                                    //$NON-NLS-1$
               + "(" + NL //                                                                       //$NON-NLS-1$

               + "   SELECT" + NL //                                                               //$NON-NLS-1$

               + "      part.equipment_equipmentid            AS part_eq_id," + NL //              //$NON-NLS-1$
               + "      part.partid                           AS part_id," + NL //                 //$NON-NLS-1$
               + "      part.\"TYPE\"                         AS part_type," + NL //               //$NON-NLS-1$

               + "      SUM(TourData.tourdistance)              AS sum_distance," + NL //          //$NON-NLS-1$
               + "      SUM(TourData.tourcomputedtime_moving)   AS sum_moving_time," + NL //       //$NON-NLS-1$

               + "      COUNT(*)                              AS num_tours" + NL //                //$NON-NLS-1$

               + "   FROM equipmentpart AS part" + NL //                                           //$NON-NLS-1$

               + "   JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "        ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //   //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "   JOIN TourData AS TourData" + NL //                                            //$NON-NLS-1$
               + "      ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "      AND TourData.tourstarttime >= part.\"DATE\"" + NL //                       //$NON-NLS-1$
               + "      AND TourData.tourstarttime <  part.dateuntil" + NL //                      //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL

               + "   WHERE part.iscollate = TRUE" + NL //                                          //$NON-NLS-1$

               + "   GROUP BY" + NL //                                                             //$NON-NLS-1$
               + "      part.equipment_equipmentid," + NL //                                       //$NON-NLS-1$
               + "      part.partid," + NL //                                                      //$NON-NLS-1$
               + "      part.\"TYPE\"" + NL //                                                     //$NON-NLS-1$

               + ") AS parts_Summarized" + NL //                                                   //$NON-NLS-1$

               + "LEFT JOIN equipment     j_equip ON j_equip.equipmentid = parts_Summarized.part_eq_id" + NL //   //$NON-NLS-1$
               + "LEFT JOIN equipmentpart j_part  ON j_part.partid       = parts_Summarized.part_id" + NL //      //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         sqlFilter.setParameters(statement, 1);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

// SET_FORMATTING_OFF

            final long equipmentID     = result.getLong(1);
            final long partID          = result.getLong(2);
            final String partType      = result.getString(3);
            final int numTours         = result.getInt(4);
            final float distance       = result.getFloat(5); // m
            final long movingTime      = result.getLong(6); // sec

// SET_FORMATTING_ON

            final String partKey = getTourValuesKey(equipmentID, partID, partType);

            final SummarizedValues eqPartValues = new SummarizedValues();

            eqPartValues.numTours = numTours;
            eqPartValues.distance = distance;
            eqPartValues.movingTime = movingTime;

            allPartValues.put(partKey, eqPartValues);
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }

      return allPartValues;
   }

   private Map<String, SummarizedValues> loadTourValuesFromServices() {

      final Map<String, SummarizedValues> allServiceValues = new HashMap<>();

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   j_equip.EQUIPMENTID," + NL //                                              1  //$NON-NLS-1$

               + "   j_service.SERVICEID," + NL //                                              2  //$NON-NLS-1$

               + "   services_Summarized.service_Type," + NL //                                 3  //$NON-NLS-1$
               + "   services_Summarized.num_tours," + NL //                                    4  //$NON-NLS-1$
               + "   services_Summarized.sum_distance," + NL //                                 5  //$NON-NLS-1$
               + "   services_Summarized.sum_moving_time AS sum_Time" + NL //                   6  //$NON-NLS-1$

               + "FROM" + NL //                                                                    //$NON-NLS-1$
               + "(" + NL //                                                                       //$NON-NLS-1$

               + "   SELECT" + NL //                                                               //$NON-NLS-1$
               + "      service.EQUIPMENT_EQUIPMENTID          AS service_EQ_ID," + NL //          //$NON-NLS-1$
               + "      service.SERVICEID                      AS service_ID," + NL //             //$NON-NLS-1$
               + "      service.\"TYPE\"                       AS service_Type," + NL //           //$NON-NLS-1$

               + "      SUM(TourData.tourdistance)             AS sum_distance," + NL //           //$NON-NLS-1$
               + "      SUM(TourData.tourcomputedtime_moving)  AS sum_moving_time," + NL //        //$NON-NLS-1$

               + "      COUNT(*)                               AS num_tours" + NL //               //$NON-NLS-1$

               + "   FROM EQUIPMENTSERVICE AS service" + NL //                                     //$NON-NLS-1$

               + "   JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "      ON j_td_eq.equipment_equipmentid = service.equipment_equipmentid" + NL //  //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "   JOIN tourdata AS TourData" + NL //                                            //$NON-NLS-1$
               + "      ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "      AND TourData.tourstarttime >= service.\"DATE\"" + NL //                    //$NON-NLS-1$
               + "      AND TourData.tourstarttime <  service.dateuntil" + NL //                   //$NON-NLS-1$
               + sqlFilter.getWhereClause() + NL

               + "   WHERE service.isCollate = TRUE" + NL //                                       //$NON-NLS-1$

               + "   GROUP BY" + NL //                                                             //$NON-NLS-1$
               + "      service.EQUIPMENT_EquipmentID," + NL //                                    //$NON-NLS-1$
               + "      service.serviceID," + NL //                                                //$NON-NLS-1$
               + "      service.\"TYPE\"" + NL //                                                  //$NON-NLS-1$

               + ") AS services_Summarized" + NL //                                                //$NON-NLS-1$

               + "LEFT JOIN equipment           j_equip     ON j_equip.equipmentid  = services_Summarized.service_EQ_ID" + NL //    //$NON-NLS-1$
               + "LEFT JOIN EQUIPMENTSERVICE    j_service   ON j_service.SERVICEID  = services_Summarized.service_ID" + NL //       //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);
         sqlFilter.setParameters(statement, 1);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

// SET_FORMATTING_OFF

            final long equipmentID     = result.getLong(1);
            final long serviceID       = result.getLong(2);
            final String partType      = result.getString(3);
            final int numTours         = result.getInt(4);
            final float distance       = result.getFloat(5); // m
            final long movingTime      = result.getLong(6); // sec

// SET_FORMATTING_ON

            final String serviceKey = getTourValuesKey(equipmentID, serviceID, partType);

            final SummarizedValues eqPartValues = new SummarizedValues();

            eqPartValues.numTours = numTours;
            eqPartValues.distance = distance;
            eqPartValues.movingTime = movingTime;

            allServiceValues.put(serviceKey, eqPartValues);
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }

      return allServiceValues;
   }

}
