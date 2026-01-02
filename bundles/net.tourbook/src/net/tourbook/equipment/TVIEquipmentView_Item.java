/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVIEquipmentView_Item extends TreeViewerItem {

   static final String                   SQL_SUM_COLUMNS;
   static final String                   SQL_SUM_COLUMNS_SUMMARIZED;
   static final String                   SQL_SUM_COLUMNS_TOUR;

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   static {

      SQL_SUM_COLUMNS = UI.EMPTY_STRING

            + "Summarized.Sum_TourDistance," + NL //                       0  //$NON-NLS-1$
            + "Summarized.Sum_TourDeviceTime_Elapsed," + NL //             1  //$NON-NLS-1$
            + "Summarized.Sum_TourDeviceTime_Recorded," + NL //            2  //$NON-NLS-1$
            + "Summarized.Sum_TourComputedTime_Moving," + NL //            3  //$NON-NLS-1$
            + "Summarized.Sum_TourAltUp," + NL //                          4  //$NON-NLS-1$
            + "Summarized.Sum_TourAltDown," + NL //                        5  //$NON-NLS-1$

            + "Summarized.Max_MaxPulse," + NL //                           6  //$NON-NLS-1$
            + "Summarized.Max_MaxAltitude," + NL //                        7  //$NON-NLS-1$
            + "Summarized.Max_MaxSpeed," + NL //                           8  //$NON-NLS-1$

            + "Summarized.Avg_AvgPulse," + NL //                           9  //$NON-NLS-1$
            + "Summarized.Avg_AvgCadence," + NL //                        10  //$NON-NLS-1$
            + "Summarized.Avg_Weather_Temperature_Average_Device" + NL // 11  //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS_SUMMARIZED = UI.EMPTY_STRING

            + "SUM(TourData.TourDistance)                AS Sum_TourDistance," + NL //                   0  //$NON-NLS-1$
            + "SUM(TourData.TourDeviceTime_Elapsed)      AS Sum_TourDeviceTime_Elapsed," + NL //         1  //$NON-NLS-1$
            + "SUM(TourData.TourDeviceTime_Recorded)     AS Sum_TourDeviceTime_Recorded," + NL //        2  //$NON-NLS-1$
            + "SUM(TourData.TourComputedTime_Moving)     AS Sum_TourComputedTime_Moving," + NL //        3  //$NON-NLS-1$
            + "SUM(TourData.TourAltUp)                   AS Sum_TourAltUp," + NL //                      4  //$NON-NLS-1$
            + "SUM(TourData.TourAltDown)                 AS Sum_TourAltDown," + NL //                    5  //$NON-NLS-1$

            + "MAX(TourData.MaxPulse)                    AS Max_MaxPulse," + NL //                       6  //$NON-NLS-1$
            + "MAX(TourData.MaxAltitude)                 AS Max_MaxAltitude," + NL //                    7  //$NON-NLS-1$
            + "MAX(TourData.MaxSpeed)                    AS Max_MaxSpeed," + NL //                       8  //$NON-NLS-1$

            + "AVG( CASE WHEN TourData.AVGPULSE = 0      THEN NULL ELSE TourData.AVGPULSE END)     AS Avg_AvgPulse," + NL //              9  //$NON-NLS-1$
            + "AVG( CASE WHEN TourData.AVGCADENCE = 0    THEN NULL ELSE TourData.AVGCADENCE END)   AS Avg_AvgCadence," + NL //           10  //$NON-NLS-1$
            + "AVG( CASE WHEN TourData.weather_Temperature_Average_Device = 0" //                                                            //$NON-NLS-1$
            + "  THEN NULL" //                                                                                                               //$NON-NLS-1$
            + "  ELSE DOUBLE(TourData.Weather_Temperature_Average_Device) / TourData.TemperatureScale END )" + NL //                         //$NON-NLS-1$
            + "  AS Avg_Weather_Temperature_Average_Device" + NL //                                                                      11  //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS_TOUR = UI.EMPTY_STRING

            + "TourData.TourDistance," + NL //                       0  //$NON-NLS-1$
            + "TourData.TourDeviceTime_Elapsed," + NL //             1  //$NON-NLS-1$
            + "TourData.TourDeviceTime_Recorded," + NL //            2  //$NON-NLS-1$
            + "TourData.TourComputedTime_Moving," + NL //            3  //$NON-NLS-1$
            + "TourData.TourAltUp," + NL //                          4  //$NON-NLS-1$
            + "TourData.TourAltDown," + NL //                        5  //$NON-NLS-1$

            + "TourData.MaxPulse," + NL //                           6  //$NON-NLS-1$
            + "TourData.MaxAltitude," + NL //                        7  //$NON-NLS-1$
            + "TourData.MaxSpeed," + NL //                           8  //$NON-NLS-1$

            + "TourData.AvgPulse," + NL //                           9  //$NON-NLS-1$
            + "TourData.AvgCadence," + NL //                        10  //$NON-NLS-1$
            + "(DOUBLE(TourData.weather_Temperature_Average_Device) / TourData.TemperatureScale)" + NL //      11 //$NON-NLS-1$
      ;
   }

   private TreeViewer _equipmentViewer;

   /**
    * Content which is displayed in the first tree column
    */
   String             firstColumn;

   long               numTours;

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
    * {@link #type} and {@link #date} are the key parts to collated (summarize) tour values
    */
   String             type;

   /**
    * {@link #type} and {@link #date} are the key parts to collated (summarize) tour values
    */
   LocalDateTime      date;

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

   public TVIEquipmentView_Item(final TreeViewer equipmentViewer) {

      _equipmentViewer = equipmentViewer;
   }

   /**
    * @return Each equipment viewer item has access to its viewer
    */
   public TreeViewer getEquipmentViewer() {

      return _equipmentViewer;
   }

   String getTourValuesKey(final long equipmentID, final long partID, final String partType) {

      return equipmentID + UI.DASH + partID + UI.DASH + partType;
   }

   /**
    * Read SQL column values into common "col..." fields which can be sum or tour values
    *
    * @param result
    * @param startIndex
    *
    * @throws SQLException
    */
   void readColumnValues_Default(final ResultSet result, final int startIndex) throws SQLException {

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
