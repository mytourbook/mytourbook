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

import java.time.LocalDateTime;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVIEquipmentView_Item extends TreeViewerItem {

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

   private TreeViewer               _equipmentViewer;

   /**
    * {@link #type} and {@link #date} are the key parts to collated (summarize) tour values
    */
   String                           type;

   /**
    * {@link #type} and {@link #date} are the key parts to collated (summarize) tour values
    */
   LocalDateTime                    date;

   float                            distance;
   long                             movingTime;

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
}
