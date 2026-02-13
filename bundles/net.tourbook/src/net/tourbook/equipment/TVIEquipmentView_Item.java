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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.SQLData;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.AppFilter;
import net.tourbook.ui.AppFilterType;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVIEquipmentView_Item extends TreeViewerItem {

   private static final String                      SQL_SUM_FIELDS;
   private static final String                      SQL_SUM_COLUMNS;
   private static final String                      SQL_SUM_TOUR_FIELDS;

   private static final IPreferenceStore            _prefStore                     = TourbookPlugin.getPrefStore();

   private static ConcurrentHashMap<String, String> _allCached_SqlAllSumColumns    = new ConcurrentHashMap<>();
   private static ConcurrentHashMap<String, String> _allCached_SqlAllSumFields     = new ConcurrentHashMap<>();
   private static ConcurrentHashMap<String, String> _allCached_SqlAllSumTourFields = new ConcurrentHashMap<>();

   static {

// SET_FORMATTING_OFF

      SQL_SUM_TOUR_FIELDS = UI.EMPTY_STRING

            + "$i_$db_TourDistance," + NL //                            0  //$NON-NLS-1$
            + "$i_$db_TourDeviceTime_Elapsed," + NL //                  1  //$NON-NLS-1$
            + "$i_$db_TourDeviceTime_Recorded," + NL //                 2  //$NON-NLS-1$
            + "$i_$db_TourComputedTime_Moving," + NL //                 3  //$NON-NLS-1$
            + "$i_$db_TourAltUp," + NL //                               4  //$NON-NLS-1$
            + "$i_$db_TourAltDown," + NL //                             5  //$NON-NLS-1$

            + "$i_$db_MaxPulse," + NL //                                6  //$NON-NLS-1$
            + "$i_$db_MaxAltitude," + NL //                             7  //$NON-NLS-1$
            + "$i_$db_MaxSpeed," + NL //                                8  //$NON-NLS-1$

            + "$i_$db_AvgPulse," + NL //                                9  //$NON-NLS-1$
            + "$i_$db_AvgCadence," + NL //                             10  //$NON-NLS-1$

            + "$i_$db_Weather_Temperature_Average_Device," + NL //     11  //$NON-NLS-1$
            + "$i_$db_TemperatureScale" + NL //                        12  //$NON-NLS-1$
            ;

      SQL_SUM_FIELDS = UI.EMPTY_STRING

            + "$i_$db_Sum_TourDistance," + NL //                        0  //$NON-NLS-1$
            + "$i_$db_Sum_TourDeviceTime_Elapsed," + NL //              1  //$NON-NLS-1$
            + "$i_$db_Sum_TourDeviceTime_Recorded," + NL //             2  //$NON-NLS-1$
            + "$i_$db_Sum_TourComputedTime_Moving," + NL //             3  //$NON-NLS-1$
            + "$i_$db_Sum_TourAltUp," + NL //                           4  //$NON-NLS-1$
            + "$i_$db_Sum_TourAltDown," + NL //                         5  //$NON-NLS-1$

            + "$i_$db_Max_MaxPulse," + NL //                            6  //$NON-NLS-1$
            + "$i_$db_Max_MaxAltitude," + NL //                         7  //$NON-NLS-1$
            + "$i_$db_Max_MaxSpeed," + NL //                            8  //$NON-NLS-1$

            + "$i_$db_Avg_AvgPulse," + NL //                            9  //$NON-NLS-1$
            + "$i_$db_Avg_AvgCadence," + NL //                         10  //$NON-NLS-1$
            + "$i_$db_Avg_Weather_Temperature_Average_Device" + NL //  11  //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS = UI.EMPTY_STRING

            + "$i_SUM($db_TourDistance)                AS Sum_TourDistance," + NL //                   0  //$NON-NLS-1$
            + "$i_SUM($db_TourDeviceTime_Elapsed)      AS Sum_TourDeviceTime_Elapsed," + NL //         1  //$NON-NLS-1$
            + "$i_SUM($db_TourDeviceTime_Recorded)     AS Sum_TourDeviceTime_Recorded," + NL //        2  //$NON-NLS-1$
            + "$i_SUM($db_TourComputedTime_Moving)     AS Sum_TourComputedTime_Moving," + NL //        3  //$NON-NLS-1$
            + "$i_SUM($db_TourAltUp)                   AS Sum_TourAltUp," + NL //                      4  //$NON-NLS-1$
            + "$i_SUM($db_TourAltDown)                 AS Sum_TourAltDown," + NL //                    5  //$NON-NLS-1$

            + "$i_MAX($db_MaxPulse)                    AS Max_MaxPulse," + NL //                       6  //$NON-NLS-1$
            + "$i_MAX($db_MaxAltitude)                 AS Max_MaxAltitude," + NL //                    7  //$NON-NLS-1$
            + "$i_MAX($db_MaxSpeed)                    AS Max_MaxSpeed," + NL //                       8  //$NON-NLS-1$

            + "$i_AVG( CASE WHEN $db_AVGPULSE = 0      THEN NULL ELSE $db_AVGPULSE END)     AS Avg_AvgPulse," + NL //              9  //$NON-NLS-1$
            + "$i_AVG( CASE WHEN $db_AVGCADENCE = 0    THEN NULL ELSE $db_AVGCADENCE END)   AS Avg_AvgCadence," + NL //           10  //$NON-NLS-1$

            + "$i_AVG( CASE WHEN $db_weather_Temperature_Average_Device = 0" //                                                                          //$NON-NLS-1$
            + "  THEN NULL" //                                                                                                                           //$NON-NLS-1$
            + "  ELSE DOUBLE($db_Weather_Temperature_Average_Device) / $db_TemperatureScale END) AS Avg_Weather_Temperature_Average_Device" + NL //  11  //$NON-NLS-1$
      ;

// SET_FORMATTING_ON
   }

   private TreeViewer          _equipmentViewer;

   /**
    * Type where the viewer items are used
    */
   private EquipmentViewerType _viewerType;

   public long                 numTours_All;
   public long                 numTours_IsCollated;

   /**
    * Content which is displayed in the first tree column
    */
   public String               firstColumn;

   float                       colDistance;

   long                        colElapsedTime;
   long                        colRecordedTime;
   long                        colMovingTime;
   long                        colPausedTime;

   long                        colAltitudeUp;
   long                        colAltitudeDown;

   float                       colMaxSpeed;
   long                        colMaxPulse;
   long                        colMaxAltitude;

   float                       colAvgSpeed;
   float                       colAvgPace;

   float                       colAvgPulse;
   float                       colAvgCadence;
   float                       colAvgTemperature_Device;

   /**
    * {@link #type} and {@link #dateFrom} are the key parts to collated (summarize) tour values
    */
   String                      type;

   /**
    * {@link #type} and {@link #dateFrom} are the key parts to collated (summarize) tour values
    */
   LocalDateTime               dateFrom;

   /*
    * These are common values for equipment, part and service
    */
   float  price;
   String priceUnit;

   /**
    * Usage duration in ms
    */
   long   usageDuration;

   /**
    * Text which identifies the last collated item
    */
   String usageDurationLast;

   public TVIEquipmentView_Item(final TreeViewer equipmentViewer, final EquipmentViewerType equipmentType) {

      _equipmentViewer = equipmentViewer;

      _viewerType = equipmentType;
   }

   /**
    * Prepend a db prefix to the fields and indent it
    *
    * @param dbPrefix
    * @param indent
    *
    * @return
    */
   public static String getSQL_SUM_COLUMNS(final String dbPrefix, final int indent) {

      return getCachedSQL(_allCached_SqlAllSumFields, SQL_SUM_COLUMNS, dbPrefix, indent);
   }

   /**
    * Prepend a db prefix to the fields and indent it
    *
    * @param dbPrefix
    * @param indent
    *
    * @return
    */
   public static String getSQL_SUM_FIELDS(final String dbPrefix, final int indent) {

      return getCachedSQL(_allCached_SqlAllSumColumns, SQL_SUM_FIELDS, dbPrefix, indent);
   }

   /**
    * Prepend a db prefix to the fields and indent it
    *
    * @param dbPrefix
    * @param indent
    *
    * @return
    */
   public static String getSQL_SUM_TOUR_COLUMNS(final String dbPrefix, final int indent) {

      return getCachedSQL(_allCached_SqlAllSumTourFields, SQL_SUM_TOUR_FIELDS, dbPrefix, indent);
   }

   AppFilter createAppFilter() {

      if (_viewerType == EquipmentViewerType.IS_EQUIPMENT_FILTER) {

         // the equipment filter viewer should display ALL available equipment for the current person

         return new AppFilter(AppFilterType.Person);

      } else {

         return new AppFilter(AppFilter.ANY_APP_FILTERS);
      }
   }

   /**
    * @return Each equipment viewer item has access to its viewer
    */
   public TreeViewer getEquipmentViewer() {

      return _equipmentViewer;
   }

   /**
    * @return {@link #_viewerType}
    */
   public EquipmentViewerType getViewerType() {

      return _viewerType;
   }

   void loadSummarizedValues_Equipment(final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      loadSummarizedValues_Equipment_WithCollate(allEquipmentItems);
      loadSummarizedValues_Equipment_IgnoreCollate(allEquipmentItems);
   }

   private void loadSummarizedValues_Equipment_IgnoreCollate(final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = createAppFilter();
         final SQLData partFilter = new EquipmentPartFilter().getSqlData();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                            //$NON-NLS-1$

               + "   j_Td_Eq.EQUIPMENT_EQUIPMENTID," + NL //                              1  //$NON-NLS-1$
               + "   COUNT(*) AS numTours" + NL //                                        2  //$NON-NLS-1$

               + "FROM EQUIPMENT" + NL //                                                    //$NON-NLS-1$

               + "JOIN TOURDATA_EQUIPMENT AS j_Td_Eq" + NL //                                //$NON-NLS-1$
               + "   ON j_Td_Eq.EQUIPMENT_EQUIPMENTID = EQUIPMENT.EQUIPMENTID" + NL //       //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "JOIN TourData AS TourData" + NL //                                         //$NON-NLS-1$
               + "   ON TourData.tourid = j_Td_Eq.tourdata_tourid" + NL //                   //$NON-NLS-1$

               + appFilter.getWhereClause()
               + partFilter.getSqlString()

               + "GROUP BY " + NL //                                                         //$NON-NLS-1$
               + "   j_Td_Eq.EQUIPMENT_EQUIPMENTID" + NL //                                  //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = partFilter.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long equipmentID = result.getLong(1);
            final int numTours = result.getInt(2);

            final TVIEquipmentView_Equipment equipmentItem = allEquipmentItems.get(equipmentID);

            if (equipmentItem != null) {

               equipmentItem.numTours_All = numTours;
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }

   private void loadSummarizedValues_Equipment_WithCollate(final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      // clone map
      final Map<Long, TVIEquipmentView_Equipment> allEquipmentItemsWithoutTours = new HashMap<>(allEquipmentItems);

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = createAppFilter();
         final SQLData partFilter = new EquipmentPartFilter().getSqlData();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                            //$NON-NLS-1$
               + "   equipment.EQUIPMENTID," + NL //                                      1  //$NON-NLS-1$
               + "   COUNT(*) AS num_Tours," + NL //                                      2  //$NON-NLS-1$

               + getSQL_SUM_COLUMNS("TourData", 3) //                                     3  //$NON-NLS-1$

               + "FROM equipment AS equipment" + NL //                                       //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_Td_Eq" + NL //                                //$NON-NLS-1$
               + "  ON j_Td_Eq.equipment_equipmentid = equipment.EQUIPMENTID" + NL //        //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "JOIN tourdata AS TourData" + NL //                                         //$NON-NLS-1$
               + "  ON TourData.tourid = j_Td_Eq.tourdata_tourid" + NL //                    //$NON-NLS-1$
               + "  AND TourData.tourstarttime >= equipment.dateFrom" + NL //                //$NON-NLS-1$
               + "  AND TourData.tourstarttime <  equipment.dateUntil" + NL //               //$NON-NLS-1$

               + appFilter.getWhereClause()
               + partFilter.getSqlString()

               + "WHERE equipment.iscollate = true" + NL //                                  //$NON-NLS-1$

               + "GROUP BY equipment.EQUIPMENTID" + NL //                                    //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = partFilter.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long equipmentID = result.getLong(1);
            final int numTours = result.getInt(2);

            final TVIEquipmentView_Equipment equipmentItem = allEquipmentItems.get(equipmentID);

            if (equipmentItem != null) {

               equipmentItem.numTours_IsCollated = numTours;

               equipmentItem.readCommonValues(result, 3);

               // this equipment has a tour -> remove from list
               allEquipmentItemsWithoutTours.remove(equipmentID);
            }
         }

         for (final TVIEquipmentView_Equipment equipmentItem : allEquipmentItemsWithoutTours.values()) {

            if (equipmentItem.getEquipment().isCollate()) {

               // set 0 children that the expand icon in the view is not displayed
               equipmentItem.setChildren(new ArrayList<>());
            }
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }

   /**
    * Summarizes all tour values for each part and type
    *
    * @return
    */
   void loadSummarizedValues_Part(final TVIEquipmentView_Part partItem) {

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = createAppFilter();
         final SQLData partFilter = new EquipmentPartFilter().getSqlData();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   Summarized.num_tours," + NL //                                             1  //$NON-NLS-1$

               + getSQL_SUM_FIELDS("Summarized", 3) //                                          2

               + "FROM" + NL //                                                                    //$NON-NLS-1$
               + "(" + NL //                                                                       //$NON-NLS-1$

               + "   SELECT" + NL //                                                               //$NON-NLS-1$

               + "      part.equipment_equipmentid    AS part_eq_id," + NL //                      //$NON-NLS-1$
               + "      part.partid                   AS part_id," + NL //                         //$NON-NLS-1$
               + "      part.\"TYPE\"                 AS part_type," + NL //                       //$NON-NLS-1$

               + "      COUNT(*)                      AS num_tours," + NL //                       //$NON-NLS-1$

               + getSQL_SUM_COLUMNS("TourData", 6) //                                              //$NON-NLS-1$

               + "   FROM EquipmentPart AS part" + NL //                                           //$NON-NLS-1$

               + "   JOIN tourdata_equipment AS j_Td_Eq" + NL //                                   //$NON-NLS-1$
               + "      ON j_Td_Eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "   JOIN TourData AS TourData" + NL //                                            //$NON-NLS-1$
               + "      ON TourData.tourid = j_Td_Eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "      AND TourData.tourstarttime >= part.dateFrom" + NL //                       //$NON-NLS-1$
               + "      AND TourData.tourstarttime <  part.dateUntil" + NL //                      //$NON-NLS-1$

               + appFilter.getWhereClause()
               + partFilter.getSqlString()

               + "   WHERE part.isCollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "      AND part.partID = ?" + NL //                                               //$NON-NLS-1$

               + "   GROUP BY" + NL //                                                             //$NON-NLS-1$
               + "      part.equipment_equipmentid," + NL //                                       //$NON-NLS-1$
               + "      part.partid," + NL //                                                      //$NON-NLS-1$
               + "      part.\"TYPE\"" + NL //                                                     //$NON-NLS-1$

               + ") AS Summarized" + NL //                                                         //$NON-NLS-1$

               + "LEFT JOIN equipment     AS j_equip ON j_equip.equipmentid = Summarized.part_eq_id" + NL //   //$NON-NLS-1$
               + "LEFT JOIN equipmentpart AS j_part  ON j_part.partid       = Summarized.part_id" + NL //      //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);
         nextIndex = partFilter.setParameters(statement, nextIndex);

         statement.setLong(nextIndex++, partItem.getPartID());

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final int numTours = result.getInt(1);

            partItem.numTours_IsCollated = numTours;

            partItem.readCommonValues(result, 2);

            // there should be only one part
            break;
         }

         final long numTours = partItem.numTours_IsCollated;

         if (numTours == 0) {

            // hide expand UI icon when there are no children

            partItem.setChildren(new ArrayList<>());
         }

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      } finally {
         Util.closeSql(statement);
      }
   }

   /**
    * Read SQL column values into common "col..." fields which can be sum or tour values
    *
    * @param result
    * @param startIndex
    *           SQL column start index
    *
    * @throws SQLException
    */
   void readCommonValues(final ResultSet result, final int startIndex) throws SQLException {

   // SET_FORMATTING_OFF

         colDistance                = result.getFloat(startIndex  + 0);

         colElapsedTime             = result.getLong(startIndex   + 1);
         colRecordedTime            = result.getLong(startIndex   + 2);
         colMovingTime              = result.getLong(startIndex   + 3);
         colPausedTime              = colElapsedTime - colMovingTime;

         colAltitudeUp              = result.getLong(startIndex   + 4);
         colAltitudeDown            = result.getLong(startIndex   + 5);

         colMaxPulse                = result.getLong(startIndex   + 6);
         colMaxAltitude             = result.getLong(startIndex   + 7);
         colMaxSpeed                = result.getFloat(startIndex  + 8);

         colAvgPulse                = result.getFloat(startIndex  + 9);
         colAvgCadence              = result.getFloat(startIndex  + 10);
         colAvgTemperature_Device   = result.getFloat(startIndex  + 11);


         final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
         final long time = isPaceAndSpeedFromRecordedTime ? colRecordedTime : colMovingTime;

         // prevent divide by 0
         colAvgSpeed    = time        == 0 ? 0 : 3.6f * colDistance / time;
         colAvgPace     = colDistance == 0 ? 0 : time * 1000f / colDistance;

   // SET_FORMATTING_ON

      if (UI.IS_SCRAMBLE_DATA) {
         scrambleValues(TVIEquipmentView_Item.class.getDeclaredFields());
      }
   }
}
