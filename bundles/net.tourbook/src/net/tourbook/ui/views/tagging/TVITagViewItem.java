/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tagging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.preference.IPreferenceStore;

public abstract class TVITagViewItem extends TreeViewerItem {

   static final String SQL_SUM_COLUMNS;
   static final String SQL_SUM_COLUMNS_TOUR;
   static {

      SQL_SUM_COLUMNS = UI.EMPTY_STRING

            + "SUM(tourDistance)," //              0   //$NON-NLS-1$
            + "SUM(TourDeviceTime_Elapsed)," //    1   //$NON-NLS-1$
            + "SUM(tourComputedTime_Moving)," //   2   //$NON-NLS-1$
            + "SUM(tourAltUp)," //                 3   //$NON-NLS-1$
            + "SUM(tourAltDown)," //               4   //$NON-NLS-1$

            + "MAX(maxPulse)," //                  5   //$NON-NLS-1$
            + "MAX(maxAltitude)," //               6   //$NON-NLS-1$
            + "MAX(maxSpeed)," //                  7   //$NON-NLS-1$

            + "AVG( CASE WHEN AVGPULSE = 0         THEN NULL ELSE AVGPULSE END)," //                                    8   //$NON-NLS-1$
            + "AVG( CASE WHEN AVGCADENCE = 0       THEN NULL ELSE AVGCADENCE END )," //                                 9   //$NON-NLS-1$
            + "AVG( CASE WHEN weather_Temperature_Average_Device = 0   THEN NULL ELSE DOUBLE(weather_Temperature_Average_Device) / TemperatureScale END )," //  10   //$NON-NLS-1$

            + "SUM(TourDeviceTime_Recorded)," //    11   //$NON-NLS-1$

            // tour counter
            + "SUM(1)" //                          12   //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS_TOUR = UI.EMPTY_STRING

            + "tourDistance," //             0   //$NON-NLS-1$
            + "TourDeviceTime_Elapsed," //   1   //$NON-NLS-1$
            + "tourComputedTime_Moving," //  2   //$NON-NLS-1$
            + "tourAltUp," //                3   //$NON-NLS-1$
            + "tourAltDown," //              4   //$NON-NLS-1$

            + "maxPulse," //                 5   //$NON-NLS-1$
            + "maxAltitude," //              6   //$NON-NLS-1$
            + "maxSpeed," //                 7   //$NON-NLS-1$

            + "avgPulse," //                 8   //$NON-NLS-1$
            + "avgCadence," //               9   //$NON-NLS-1$
            + "(DOUBLE(weather_Temperature_Average_Device) / TemperatureScale)," //         10   //$NON-NLS-1$
            + "TourDeviceTime_Recorded" //   11   //$NON-NLS-1$
      ;
   }

   protected final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /**
    * content which is displayed in the tree column
    */
   String                           treeColumn;

   long                             colDistance;

   long                             colElapsedTime;
   long                             colRecordedTime;
   long                             colMovingTime;
   long                             colPausedTime;

   long                             colAltitudeUp;
   long                             colAltitudeDown;

   float                            colMaxSpeed;
   long                             colMaxPulse;
   long                             colMaxAltitude;

   float                            colAvgSpeed;
   float                            colAvgPace;

   float                            colAvgPulse;
   float                            colAvgCadence;
   float                            colAvgTemperature_Device;

   long                             colTourCounter;

   int                              temperatureDigits;

   /**
    * Read sum totals from the database for the tagItem
    *
    * @param tagItem
    */
   public static void readTagTotals(final TVITagView_Tag tagItem) {

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         /*
          * get tags
          */
         final String sql = UI.EMPTY_STRING
               //
               + ("SELECT " + SQL_SUM_COLUMNS) //$NON-NLS-1$
               + (" FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jtblTagData") //$NON-NLS-1$ //$NON-NLS-2$

               // get data for a tour
               + (" LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData ON ") //$NON-NLS-1$ //$NON-NLS-2$
               + (" jtblTagData.TourData_tourId = TourData.tourId") //$NON-NLS-1$

               + " WHERE jtblTagData.TourTag_TagId = ?" //$NON-NLS-1$
               + sqlFilter.getWhereClause();

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, tagItem.getTagId());
         sqlFilter.setParameters(statement, 2);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {
            tagItem.readSumColumnData(result, 1);
         }

         if (tagItem.colTourCounter == 0) {

            /*
             * to hide the '+' for an item which has no children, an empty list of children will be
             * created
             */
            tagItem.setChildren(new ArrayList<>());
         }

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }
   }

   void readDefaultColumnData(final ResultSet result, final int startIndex) throws SQLException {

      colDistance = result.getLong(startIndex + 0);

      colElapsedTime = result.getLong(startIndex + 1);
      colMovingTime = result.getLong(startIndex + 2);
      colPausedTime = colElapsedTime - colMovingTime;

      colAltitudeUp = result.getLong(startIndex + 3);
      colAltitudeDown = result.getLong(startIndex + 4);

      colMaxPulse = result.getLong(startIndex + 5);
      colMaxAltitude = result.getLong(startIndex + 6);
      colMaxSpeed = result.getFloat(startIndex + 7);

      colAvgPulse = result.getFloat(startIndex + 8);
      colAvgCadence = result.getFloat(startIndex + 9);
      colAvgTemperature_Device = result.getFloat(startIndex + 10);

      colRecordedTime = result.getLong(startIndex + 11);

      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final long time = isPaceAndSpeedFromRecordedTime ? colRecordedTime : colMovingTime;
      // prevent divide by 0
      colAvgSpeed = (time == 0 ? 0 : 3.6f * colDistance / time);
      colAvgPace = colDistance == 0 ? 0 : time * 1000f / colDistance;

      if (UI.IS_SCRAMBLE_DATA) {

         colDistance = UI.scrambleNumbers(colDistance);

         colElapsedTime = UI.scrambleNumbers(colElapsedTime);
         colRecordedTime = UI.scrambleNumbers(colRecordedTime);
         colMovingTime = UI.scrambleNumbers(colMovingTime);
         colPausedTime = UI.scrambleNumbers(colPausedTime);

         colAltitudeUp = UI.scrambleNumbers(colAltitudeUp);
         colAltitudeDown = UI.scrambleNumbers(colAltitudeDown);

         colMaxPulse = UI.scrambleNumbers(colMaxPulse);
         colMaxAltitude = UI.scrambleNumbers(colMaxAltitude);
         colMaxSpeed = UI.scrambleNumbers(colMaxSpeed);

         colAvgPulse = UI.scrambleNumbers(colAvgPulse);
         colAvgCadence = UI.scrambleNumbers(colAvgCadence);
         colAvgTemperature_Device = UI.scrambleNumbers(colAvgTemperature_Device);

         colAvgSpeed = UI.scrambleNumbers(colAvgSpeed);
         colAvgPace = UI.scrambleNumbers(colAvgPace);
      }
   }

   public void readSumColumnData(final ResultSet result, final int startIndex) throws SQLException {

      readDefaultColumnData(result, startIndex);

      colTourCounter = result.getLong(startIndex + 12);
   }

}
