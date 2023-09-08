/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.ui.SQLFilter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVITaggingView_Item extends TreeViewerItem {

   static final String         SQL_SUM_COLUMNS;
   static final String         SQL_SUM_COLUMNS_TOUR;

   private static final String SCRAMBLE_FIELD_PREFIX = "col"; //$NON-NLS-1$

   static {

      SQL_SUM_COLUMNS = UI.EMPTY_STRING

            + "SUM(tourDistance)," + NL //                  0  //$NON-NLS-1$
            + "SUM(TourDeviceTime_Elapsed)," + NL //        1  //$NON-NLS-1$
            + "SUM(tourComputedTime_Moving)," + NL //       2  //$NON-NLS-1$
            + "SUM(tourAltUp)," + NL //                     3  //$NON-NLS-1$
            + "SUM(tourAltDown)," + NL //                   4  //$NON-NLS-1$

            + "MAX(maxPulse)," + NL //                      5  //$NON-NLS-1$
            + "MAX(maxAltitude)," + NL //                   6  //$NON-NLS-1$
            + "MAX(maxSpeed)," + NL //                      7  //$NON-NLS-1$

            + "AVG( CASE WHEN AVGPULSE = 0      THEN NULL ELSE AVGPULSE END)," + NL //                8  //$NON-NLS-1$
            + "AVG( CASE WHEN AVGCADENCE = 0    THEN NULL ELSE AVGCADENCE END )," + NL //             9  //$NON-NLS-1$
            + "AVG( CASE WHEN weather_Temperature_Average_Device = 0 " //                                //$NON-NLS-1$
            + "  THEN NULL" //                                                                           //$NON-NLS-1$
            + "  ELSE DOUBLE(weather_Temperature_Average_Device) / TemperatureScale END )," + NL //   10 //$NON-NLS-1$

            + "SUM(TourDeviceTime_Recorded)" + NL //        11 //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS_TOUR = UI.EMPTY_STRING

            + "tourDistance," + NL //                       0  //$NON-NLS-1$
            + "TourDeviceTime_Elapsed," + NL //             1  //$NON-NLS-1$
            + "tourComputedTime_Moving," + NL //            2  //$NON-NLS-1$
            + "tourAltUp," + NL //                          3  //$NON-NLS-1$
            + "tourAltDown," + NL //                        4  //$NON-NLS-1$

            + "maxPulse," + NL //                           5  //$NON-NLS-1$
            + "maxAltitude," + NL //                        6  //$NON-NLS-1$
            + "maxSpeed," + NL //                           7  //$NON-NLS-1$

            + "avgPulse," + NL //                           8  //$NON-NLS-1$
            + "avgCadence," + NL //                         9  //$NON-NLS-1$
            + "(DOUBLE(weather_Temperature_Average_Device) / TemperatureScale)," + NL //     10 //$NON-NLS-1$
            + "TourDeviceTime_Recorded" + NL //             11 //$NON-NLS-1$
      ;
   }

   protected final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   /**
    * Content which is displayed in the first tree column
    */
   String                           firstColumn;

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

   long                             numTours;
   int                              numTags_NoTours;

   int                              temperatureDigits;

   private TreeViewer               _tagViewer;

   public TVITaggingView_Item(final TreeViewer tagViewer) {

      _tagViewer = tagViewer;
   }

   /**
    * Read sum totals from the database for the tagItem
    *
    * @param tagItem
    */
   public static void readTagTotals(final TVITaggingView_Tag tagItem) {

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final SQLFilter sqlFilter = new SQLFilter();

         /*
          * Get tags
          */
         final String sql = UI.EMPTY_STRING

               + "SELECT " + SQL_SUM_COLUMNS + NL //                                               //$NON-NLS-1$
               + " FROM " + TourDatabase.JOINTABLE__TOURDATA__TOURTAG + " jtblTagData" + NL //     //$NON-NLS-1$ //$NON-NLS-2$

               // get data for a tour
               + " LEFT OUTER JOIN " + TourDatabase.TABLE_TOUR_DATA + " TourData ON " + NL //      //$NON-NLS-1$ //$NON-NLS-2$
               + " jtblTagData.TourData_tourId = TourData.tourId" + NL //                          //$NON-NLS-1$

               + " WHERE jtblTagData.TourTag_TagId = ?" + NL //                                    //$NON-NLS-1$
               + sqlFilter.getWhereClause();

         final PreparedStatement statement = conn.prepareStatement(sql);

         statement.setLong(1, tagItem.getTagId());
         sqlFilter.setParameters(statement, 2);

         final ResultSet result = statement.executeQuery();

         while (result.next()) {
            tagItem.readSumColumnData(result, 1);
         }

         if (tagItem.numTours == 0) {

            /*
             * to hide the '+' for an item which has no children, an empty list of children will be
             * created
             */
//            tagItem.setChildren(new ArrayList<>());
         }

      } catch (final SQLException e) {

         net.tourbook.ui.UI.showSQLException(e);
      }
   }

   public TreeViewer getTagViewer() {

      return _tagViewer;
   }

   void readDefaultColumnData(final ResultSet result, final int startIndex) throws SQLException {

// SET_FORMATTING_OFF

      colDistance                = result.getLong(startIndex + 0);

      colElapsedTime             = result.getLong(startIndex + 1);
      colMovingTime              = result.getLong(startIndex + 2);
      colPausedTime              = colElapsedTime - colMovingTime;

      colAltitudeUp              = result.getLong(startIndex + 3);
      colAltitudeDown            = result.getLong(startIndex + 4);

      colMaxPulse                = result.getLong(startIndex + 5);
      colMaxAltitude             = result.getLong(startIndex + 6);
      colMaxSpeed                = result.getFloat(startIndex + 7);

      colAvgPulse                = result.getFloat(startIndex + 8);
      colAvgCadence              = result.getFloat(startIndex + 9);
      colAvgTemperature_Device   = result.getFloat(startIndex + 10);

      colRecordedTime            = result.getLong(startIndex + 11);

      final boolean isPaceAndSpeedFromRecordedTime = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME);
      final long time = isPaceAndSpeedFromRecordedTime ? colRecordedTime : colMovingTime;

      // prevent divide by 0
      colAvgSpeed    = time        == 0 ? 0 : 3.6f * colDistance / time;
      colAvgPace     = colDistance == 0 ? 0 : time * 1000f / colDistance;

// SET_FORMATTING_ON

      if (UI.IS_SCRAMBLE_DATA) {
         scrambleData();
      }
   }

   public void readSumColumnData(final ResultSet result, final int startIndex) throws SQLException {

      readDefaultColumnData(result, startIndex);
   }

   /**
    * Scramble all fields which fieldname is starting with "col"
    */
   private void scrambleData() {

      try {

         for (final Field field : TVITaggingView_Item.class.getDeclaredFields()) {

            final String fieldName = field.getName();

            if (fieldName.startsWith(SCRAMBLE_FIELD_PREFIX)) {

               final Type fieldType = field.getGenericType();

               if (Integer.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getInt(this)));

               } else if (Long.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getLong(this)));

               } else if (Float.TYPE.equals(fieldType)) {

                  field.set(this, UI.scrambleNumbers(field.getFloat(this)));

               } else if (String.class.equals(fieldType)) {

                  final String fieldValue = (String) field.get(this);
                  final String scrambledText = UI.scrambleText(fieldValue);

                  field.set(this, scrambledText);
               }
            }
         }

      } catch (IllegalArgumentException | IllegalAccessException e) {
         e.printStackTrace();
      }
   }
}
