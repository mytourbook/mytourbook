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
import java.util.Map;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.SQL;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.common.util.Util;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.AppFilter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVIEquipmentView_Item extends TreeViewerItem {

   static final String                   SQL_SUM_COLUMNS;
   static final String                   SQL_SUM_COLUMNS_SUMMARIZED;
   static final String                   SQL_SUM_COLUMNS_TOUR;

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   static {

      SQL_SUM_COLUMNS = UI.EMPTY_STRING

            + "   Summarized.Sum_TourDistance," + NL //                       0  //$NON-NLS-1$
            + "   Summarized.Sum_TourDeviceTime_Elapsed," + NL //             1  //$NON-NLS-1$
            + "   Summarized.Sum_TourDeviceTime_Recorded," + NL //            2  //$NON-NLS-1$
            + "   Summarized.Sum_TourComputedTime_Moving," + NL //            3  //$NON-NLS-1$
            + "   Summarized.Sum_TourAltUp," + NL //                          4  //$NON-NLS-1$
            + "   Summarized.Sum_TourAltDown," + NL //                        5  //$NON-NLS-1$

            + "   Summarized.Max_MaxPulse," + NL //                           6  //$NON-NLS-1$
            + "   Summarized.Max_MaxAltitude," + NL //                        7  //$NON-NLS-1$
            + "   Summarized.Max_MaxSpeed," + NL //                           8  //$NON-NLS-1$

            + "   Summarized.Avg_AvgPulse," + NL //                           9  //$NON-NLS-1$
            + "   Summarized.Avg_AvgCadence," + NL //                        10  //$NON-NLS-1$
            + "   Summarized.Avg_Weather_Temperature_Average_Device" + NL // 11  //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS_SUMMARIZED = UI.EMPTY_STRING

            + "   SUM(TourData.TourDistance)                AS Sum_TourDistance," + NL //                   0  //$NON-NLS-1$
            + "   SUM(TourData.TourDeviceTime_Elapsed)      AS Sum_TourDeviceTime_Elapsed," + NL //         1  //$NON-NLS-1$
            + "   SUM(TourData.TourDeviceTime_Recorded)     AS Sum_TourDeviceTime_Recorded," + NL //        2  //$NON-NLS-1$
            + "   SUM(TourData.TourComputedTime_Moving)     AS Sum_TourComputedTime_Moving," + NL //        3  //$NON-NLS-1$
            + "   SUM(TourData.TourAltUp)                   AS Sum_TourAltUp," + NL //                      4  //$NON-NLS-1$
            + "   SUM(TourData.TourAltDown)                 AS Sum_TourAltDown," + NL //                    5  //$NON-NLS-1$

            + "   MAX(TourData.MaxPulse)                    AS Max_MaxPulse," + NL //                       6  //$NON-NLS-1$
            + "   MAX(TourData.MaxAltitude)                 AS Max_MaxAltitude," + NL //                    7  //$NON-NLS-1$
            + "   MAX(TourData.MaxSpeed)                    AS Max_MaxSpeed," + NL //                       8  //$NON-NLS-1$

            + "   AVG( CASE WHEN TourData.AVGPULSE = 0      THEN NULL ELSE TourData.AVGPULSE END)     AS Avg_AvgPulse," + NL //              9  //$NON-NLS-1$
            + "   AVG( CASE WHEN TourData.AVGCADENCE = 0    THEN NULL ELSE TourData.AVGCADENCE END)   AS Avg_AvgCadence," + NL //           10  //$NON-NLS-1$
            + "   AVG( CASE WHEN TourData.weather_Temperature_Average_Device = 0" //                                                            //$NON-NLS-1$
            + "     THEN NULL" //                                                                                                               //$NON-NLS-1$
            + "     ELSE DOUBLE(TourData.Weather_Temperature_Average_Device) / TourData.TemperatureScale END )" + NL //                         //$NON-NLS-1$
            + "     AS Avg_Weather_Temperature_Average_Device" + NL //                                                                      11  //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS_TOUR = UI.EMPTY_STRING

            + "   TourData.TourDistance," + NL //                       0  //$NON-NLS-1$
            + "   TourData.TourDeviceTime_Elapsed," + NL //             1  //$NON-NLS-1$
            + "   TourData.TourDeviceTime_Recorded," + NL //            2  //$NON-NLS-1$
            + "   TourData.TourComputedTime_Moving," + NL //            3  //$NON-NLS-1$
            + "   TourData.TourAltUp," + NL //                          4  //$NON-NLS-1$
            + "   TourData.TourAltDown," + NL //                        5  //$NON-NLS-1$

            + "   TourData.MaxPulse," + NL //                           6  //$NON-NLS-1$
            + "   TourData.MaxAltitude," + NL //                        7  //$NON-NLS-1$
            + "   TourData.MaxSpeed," + NL //                           8  //$NON-NLS-1$

            + "   TourData.AvgPulse," + NL //                           9  //$NON-NLS-1$
            + "   TourData.AvgCadence," + NL //                        10  //$NON-NLS-1$
            + "   (DOUBLE(TourData.weather_Temperature_Average_Device) / TourData.TemperatureScale)" + NL //      11 //$NON-NLS-1$
      ;
   }

   private TreeViewer _equipmentViewer;

   /**
    * Is <code>true</code> when tours, years and months should be displayed in the viewer, otherwise
    * <code>false</code> which is used in the tour equipment filter where only the equipment
    * structure is displayed
    */
   private boolean    _isShowTours;

   public long        numTours_All;
   public long        numTours_IsCollated;

   /**
    * Content which is displayed in the first tree column
    */
   public String      firstColumn;

   float              colDistance;

   long               colElapsedTime;
   long               colRecordedTime;
   long               colMovingTime;
   long               colPausedTime;

   long               colAltitudeUp;
   long               colAltitudeDown;

   float              colMaxSpeed;
   long               colMaxPulse;
   long               colMaxAltitude;

   float              colAvgSpeed;
   float              colAvgPace;

   float              colAvgPulse;
   float              colAvgCadence;
   float              colAvgTemperature_Device;

   /**
    * {@link #type} and {@link #dateFrom} are the key parts to collated (summarize) tour values
    */
   String             type;

   /**
    * {@link #type} and {@link #dateFrom} are the key parts to collated (summarize) tour values
    */
   LocalDateTime      dateFrom;

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

   public TVIEquipmentView_Item(final TreeViewer equipmentViewer, final boolean isShowTours) {

      _equipmentViewer = equipmentViewer;

      _isShowTours = isShowTours;
   }

   /**
    * @return Each equipment viewer item has access to its viewer
    */
   public TreeViewer getEquipmentViewer() {

      return _equipmentViewer;
   }

   /**
    * @return {@link #_isShowTours}
    */
   public boolean isShowTours() {

      return _isShowTours;
   }

   void loadSummarizedValues_Equipment(final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      loadSummarizedValues_Equipment_WithCollate(allEquipmentItems);
      loadSummarizedValues_Equipment_IgnoreCollate(allEquipmentItems);
   }

   private void loadSummarizedValues_Equipment_IgnoreCollate(final Map<Long, TVIEquipmentView_Equipment> allEquipmentItems) {

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = new AppFilter(AppFilter.ANY_APP_FILTERS);

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                            //$NON-NLS-1$

               + "   j_TD_EQ.EQUIPMENT_EQUIPMENTID," + NL //                              1  //$NON-NLS-1$
               + "   COUNT(*) AS numTours" + NL //                                        2  //$NON-NLS-1$

               + "FROM EQUIPMENT" + NL //                                                    //$NON-NLS-1$

               + "JOIN TOURDATA_EQUIPMENT AS j_TD_EQ" + NL //                                //$NON-NLS-1$
               + "   ON j_TD_EQ.EQUIPMENT_EQUIPMENTID = EQUIPMENT.EQUIPMENTID" + NL //       //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "JOIN TourData AS TourData" + NL //                                         //$NON-NLS-1$
               + "   ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                   //$NON-NLS-1$

               + appFilter.getWhereClause()

               + "GROUP BY " + NL //                                                         //$NON-NLS-1$
               + "   j_TD_EQ.EQUIPMENT_EQUIPMENTID" + NL //                                  //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);

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

      String sql = null;
      PreparedStatement statement = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final AppFilter appFilter = new AppFilter(AppFilter.ANY_APP_FILTERS);

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                            //$NON-NLS-1$
               + "   equipment.EQUIPMENTID," + NL //                                      1  //$NON-NLS-1$
               + "   COUNT(*) AS num_Tours," + NL //                                      2  //$NON-NLS-1$

               + SQL_SUM_COLUMNS_SUMMARIZED //                                            3

               + "FROM equipment AS equipment" + NL //                                       //$NON-NLS-1$

               + "JOIN tourdata_equipment AS j_td_eq" + NL //                                //$NON-NLS-1$
               + "  ON j_td_eq.equipment_equipmentid = equipment.EQUIPMENTID" + NL //        //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "JOIN tourdata AS TourData" + NL //                                         //$NON-NLS-1$
               + "  ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                    //$NON-NLS-1$
               + "  AND TourData.tourstarttime >= equipment.dateFrom" + NL //                //$NON-NLS-1$
               + "  AND TourData.tourstarttime <  equipment.dateUntil" + NL //               //$NON-NLS-1$

               + appFilter.getWhereClause()

               + "WHERE equipment.iscollate = true" + NL //                                  //$NON-NLS-1$

               + "GROUP BY equipment.EQUIPMENTID" + NL //                                    //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final long equipmentID = result.getLong(1);
            final int numTours = result.getInt(2);

            final TVIEquipmentView_Equipment equipmentItem = allEquipmentItems.get(equipmentID);

            if (equipmentItem != null) {

               equipmentItem.numTours_IsCollated = numTours;

               equipmentItem.readCommonValues(result, 3);
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

         final AppFilter appFilter = new AppFilter(AppFilter.ANY_APP_FILTERS);

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                                                  //$NON-NLS-1$

               + "   Summarized.num_tours," + NL //                                             1  //$NON-NLS-1$

               + SQL_SUM_COLUMNS //                                                             2

               + "FROM" + NL //                                                                    //$NON-NLS-1$
               + "(" + NL //                                                                       //$NON-NLS-1$

               + "   SELECT" + NL //                                                               //$NON-NLS-1$

               + "      part.equipment_equipmentid    AS part_eq_id," + NL //                      //$NON-NLS-1$
               + "      part.partid                   AS part_id," + NL //                         //$NON-NLS-1$
               + "      part.\"TYPE\"                 AS part_type," + NL //                       //$NON-NLS-1$

               + "      COUNT(*)                      AS num_tours," + NL //                       //$NON-NLS-1$

               + SQL_SUM_COLUMNS_SUMMARIZED

               + "   FROM EquipmentPart AS part" + NL //                                           //$NON-NLS-1$

               + "   JOIN tourdata_equipment AS j_td_eq" + NL //                                   //$NON-NLS-1$
               + "      ON j_td_eq.equipment_equipmentid = part.equipment_equipmentid" + NL //     //$NON-NLS-1$

               // the alias "TourData" is needed that the app filter is working
               + "   JOIN TourData AS TourData" + NL //                                            //$NON-NLS-1$
               + "      ON TourData.tourid = j_td_eq.tourdata_tourid" + NL //                      //$NON-NLS-1$
               + "      AND TourData.tourstarttime >= part.dateFrom" + NL //                       //$NON-NLS-1$
               + "      AND TourData.tourstarttime <  part.dateUntil" + NL //                      //$NON-NLS-1$

               + appFilter.getWhereClause()

               + "   WHERE part.iscollate = TRUE" + NL //                                          //$NON-NLS-1$
               + "      AND part.partID = ?" + NL //                                               //$NON-NLS-1$

               + "   GROUP BY" + NL //                                                             //$NON-NLS-1$
               + "      part.equipment_equipmentid," + NL //                                       //$NON-NLS-1$
               + "      part.partid," + NL //                                                      //$NON-NLS-1$
               + "      part.\"TYPE\"" + NL //                                                     //$NON-NLS-1$

               + ") AS Summarized" + NL //                                                         //$NON-NLS-1$

               + "LEFT JOIN equipment     j_equip ON j_equip.equipmentid = Summarized.part_eq_id" + NL //   //$NON-NLS-1$
               + "LEFT JOIN equipmentpart j_part  ON j_part.partid       = Summarized.part_id" + NL //      //$NON-NLS-1$
         ;

         statement = conn.prepareStatement(sql);

         int nextIndex = 1;

         nextIndex = appFilter.setParameters(statement, nextIndex);

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
