/*******************************************************************************
 * Copyright (C) 2005, 2026 Wolfgang Schramm and Contributors
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.TreeViewerItem;
import net.tourbook.preferences.ITourbookPreferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.TreeViewer;

public abstract class TVITaggingView_Item extends TreeViewerItem {

   static final String                   SQL_SUM_COLUMNS;
   static final String                   SQL_SUM_COLUMNS_TOUR;

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   static {

      SQL_SUM_COLUMNS = UI.EMPTY_STRING

            + "   SUM(tourDistance)," + NL //                  0  //$NON-NLS-1$
            + "   SUM(TourDeviceTime_Elapsed)," + NL //        1  //$NON-NLS-1$
            + "   SUM(tourComputedTime_Moving)," + NL //       2  //$NON-NLS-1$
            + "   SUM(tourAltUp)," + NL //                     3  //$NON-NLS-1$
            + "   SUM(tourAltDown)," + NL //                   4  //$NON-NLS-1$

            + "   MAX(maxPulse)," + NL //                      5  //$NON-NLS-1$
            + "   MAX(maxAltitude)," + NL //                   6  //$NON-NLS-1$
            + "   MAX(maxSpeed)," + NL //                      7  //$NON-NLS-1$

            + "   AVG( CASE WHEN AVGPULSE = 0      THEN NULL ELSE AVGPULSE END)," + NL //                8  //$NON-NLS-1$
            + "   AVG( CASE WHEN AVGCADENCE = 0    THEN NULL ELSE AVGCADENCE END )," + NL //             9  //$NON-NLS-1$
            + "   AVG( CASE WHEN weather_Temperature_Average_Device = 0 " //                                //$NON-NLS-1$
            + "     THEN NULL" //                                                                           //$NON-NLS-1$
            + "     ELSE DOUBLE(weather_Temperature_Average_Device) / TemperatureScale END )," + NL //   10 //$NON-NLS-1$

            + "   SUM(TourDeviceTime_Recorded)" + NL //        11 //$NON-NLS-1$
      ;

      SQL_SUM_COLUMNS_TOUR = UI.EMPTY_STRING

            + "   tourDistance," + NL //                       0  //$NON-NLS-1$
            + "   TourDeviceTime_Elapsed," + NL //             1  //$NON-NLS-1$
            + "   tourComputedTime_Moving," + NL //            2  //$NON-NLS-1$
            + "   tourAltUp," + NL //                          3  //$NON-NLS-1$
            + "   tourAltDown," + NL //                        4  //$NON-NLS-1$

            + "   maxPulse," + NL //                           5  //$NON-NLS-1$
            + "   maxAltitude," + NL //                        6  //$NON-NLS-1$
            + "   maxSpeed," + NL //                           7  //$NON-NLS-1$

            + "   avgPulse," + NL //                           8  //$NON-NLS-1$
            + "   avgCadence," + NL //                         9  //$NON-NLS-1$
            + "   (DOUBLE(weather_Temperature_Average_Device) / TemperatureScale)," + NL //     10 //$NON-NLS-1$

            + "   TourDeviceTime_Recorded" + NL //             11 //$NON-NLS-1$
      ;
   }

   /**
    * Content which is displayed in the first tree column
    */
   String firstColumn;

   /*
    * THESE FIELD MUST BE PUBLIC THAT net.tourbook.common.util.TreeViewerItem.scrambleValues() HAS
    * ACCESS !!!
    */
   public long        colDistance;

   public long        colElapsedTime;
   public long        colRecordedTime;
   public long        colMovingTime;
   public long        colPausedTime;

   public long        colAltitudeUp;
   public long        colAltitudeDown;

   public long        colMaxAltitude;
   public long        colMaxPulse;
   public float       colMaxSpeed;

   public float       colAvgCadence;
   public float       colAvgPace;
   public float       colAvgPulse;
   public float       colAvgSpeed;
   public float       colAvgTemperature_Device;

   public AtomicLong  numTours          = new AtomicLong();

   /**
    * This is needed for the tag filter to identify tag categories which contains tags which do not
    * have tours
    */
   public AtomicLong  numNoTours        = new AtomicLong();

   public AtomicLong  numNotLoadedItems = new AtomicLong();

   private TreeViewer _tagViewer;

   public TVITaggingView_Item(final TreeViewer tagViewer) {

      _tagViewer = tagViewer;
   }

   TreeViewer getTagViewer() {

      return _tagViewer;
   }

   void readSumColumnData(final ResultSet result, final int startIndex) throws SQLException {

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
         scrambleValues(TVITaggingView_Item.class.getDeclaredFields());
      }
   }

   /**
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    * <p>
    * <b>RECURSIVE</b>
    * <p>
    * Add number of loaded items
    * <p>
    * !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    */
   void updateNumLoadedItems_Add(final int numItems) {

      numNotLoadedItems.addAndGet(numItems);

      final TreeViewerItem parentItem = getParentItem();

      if (parentItem instanceof final TVITaggingView_Item taggingItem) {

         taggingItem.updateNumLoadedItems_Add(numItems);
      }
   }

   /**
    * <b>!!! RECURSIVE !!!</b>
    * <p>
    * Decrement number of loaded items
    */
   void updateNumLoadedItems_Decrement() {

      numNotLoadedItems.decrementAndGet();

      final TreeViewerItem parentItem = getParentItem();

      if (parentItem instanceof final TVITaggingView_Item taggingItem) {

         taggingItem.updateNumLoadedItems_Decrement();
      }
   }

   /**
    * <b>!!! RECURSIVE !!!</b>
    * <p>
    * Increment number of loaded items
    */
   void updateNumLoadedItems_Increment() {

      numNotLoadedItems.incrementAndGet();

      final TreeViewerItem parentItem = getParentItem();

      if (parentItem instanceof final TVITaggingView_Item taggingItem) {

         taggingItem.updateNumLoadedItems_Increment();
      }
   }

   void updateParent_NumNoTours(final TVITaggingView_Item taggingItem) {

      final TreeViewerItem parentItem = getParentItem();

      if (parentItem instanceof final TVITaggingView_Item parentTaggingItem) {

         parentTaggingItem.numNoTours.addAndGet(taggingItem.numNoTours.get());
      }
   }

   /**
    * <b>!!! RECURSIVE !!!</b>
    * <p>
    * Update number of tours
    *
    * @param newNumTours
    * @param newNumNoTours
    * @param allUpdatedItems
    */
   void updateParent_NumToursAndNoTours(final int newNumTours,
                                        final int newNumNoTours,
                                        final Set<TVITaggingView_Item> allUpdatedItems) {

      final TreeViewerItem parentItem = getParentItem();

      if (parentItem instanceof final TVITaggingView_Item taggingItem) {

         if (taggingItem instanceof TVITaggingView_Root) {

            // skip root

            return;
         }

         taggingItem.numTours.addAndGet(newNumTours);
         taggingItem.numNoTours.addAndGet(newNumNoTours);

         allUpdatedItems.add(taggingItem);

         taggingItem.updateParent_NumToursAndNoTours(newNumTours, newNumNoTours, allUpdatedItems);

//         String item = UI.EMPTY_STRING;
//         String name = UI.EMPTY_STRING;
//
//         if (taggingItem instanceof final TVITaggingView_Tag tagItem) {
//
//            item = "tag";
//            name = tagItem.getTourTag().getTagName();
//
//         } else if (taggingItem instanceof final TVITaggingView_TagCategory categoryItem) {
//
//            item = "cat";
//            name = categoryItem.getTourTagCategory().getCategoryName();
//         }
//
//         final String text = "%-5s %-30s %5d %5d+  %-15s".formatted(
//
//               item,
//               name,
//
//               newAddedTours,
//               newNumTours,
//
//               Thread.currentThread().getName()
//
//         );
//
//         System.out.println(text);

// TODO remove SYSTEM.OUT.PRINTLN
      }
   }
}
