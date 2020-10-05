/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.geoCompare;

import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.ui.IWorkbenchPart;

public class GeoCompareManager {

   private static final int                                      COMPARATOR_THREADS   = Runtime.getRuntime().availableProcessors();

   private static final LinkedBlockingDeque<GeoPartComparerItem> _compareWaitingQueue = new LinkedBlockingDeque<>();

   private static ThreadPoolExecutor                             _comparerExecutor;

   private static boolean                                        LOG_TOUR_COMPARING   = false;

   private static final ListenerList<IGeoCompareListener>        _geoCompareListeners = new ListenerList<>(ListenerList.IDENTITY);

   private static boolean                                        _isGeoComparingOn;

   static {

      /*
       * Setup comparer executer
       */

      final ThreadFactory threadFactory = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, "Comparing geo tours");//$NON-NLS-1$

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      System.out.println(
            (String.format(
                  "[%s] Comparing tours with %d threads", //$NON-NLS-1$
                  GeoCompareManager.class.getSimpleName(),
                  COMPARATOR_THREADS)));
// TODO remove SYSTEM.OUT.PRINTLN

      _comparerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(COMPARATOR_THREADS, threadFactory);
   }

   public static void addGeoCompareEventListener(final IGeoCompareListener listener) {

      _geoCompareListeners.add(listener);
   }

   /**
    * @param loaderItem
    * @param geoPartView
    */
   static void compareGeoTours(final GeoPartItem loaderItem, final GeoCompareView geoPartView) {

      for (final long tourId : loaderItem.tourIds) {

         final GeoPartComparerItem comparerItem = new GeoPartComparerItem(tourId, loaderItem);

         // keep compared tour
         loaderItem.comparedTours.add(comparerItem);

         _compareWaitingQueue.add(comparerItem);

         _comparerExecutor.submit(new Runnable() {
            @Override
            public void run() {

               // get last added loader item
               final GeoPartComparerItem comparatorItem = _compareWaitingQueue.pollFirst();

               if (comparatorItem == null) {
                  return;
               }

               try {

                  compareTour(comparatorItem);

               } catch (final Exception e) {

                  StatusUtil.log(e);
               }

               geoPartView.compare_50_TourIsCompared(comparatorItem);
            }
         });
      }
   }

   private static void compareTour(final GeoPartComparerItem comparerItem) {

      final GeoPartItem geoPartItem = comparerItem.geoPartItem;

      if (geoPartItem.isCanceled) {
         return;
      }

      /*
       * Load tour data
       */
      final long startLoading = System.nanoTime();

      final TourData tourData = TourManager.getInstance().getTourData(comparerItem.tourId);

      /*
       * Normalize data
       */
      final long startConvert = System.nanoTime();

      final NormalizedGeoData normalizedTourPart = geoPartItem.normalizedTourPart;
      final int[] normPartLatSerie = normalizedTourPart.normalizedLat;
      final int[] normPartLonSerie = normalizedTourPart.normalizedLon;

      final NormalizedGeoData normalizedTour = tourData.getNormalizedLatLon(
            normalizedTourPart.geoAccuracy,
            normalizedTourPart.distanceAccuracy);
      final int[] normTourLatSerie = normalizedTour.normalizedLat;
      final int[] normTourLonSerie = normalizedTour.normalizedLon;

      final int numNormPartSlices = normPartLatSerie.length;
      final int numNormTourSlices = normTourLatSerie.length;

      final float[] normLatLonDiff = new float[numNormTourSlices];

      /*
       * Compare
       */
      final long startComparing = System.nanoTime();

      long minDiffValue = Long.MAX_VALUE;
      int normMinDiffIndex = -1;
      int numCompares = 0;

      // loop: all normalized tour slices
      for (int normTourIndex = 0; normTourIndex < numNormTourSlices; normTourIndex++) {

         long latLonDiff = -1;

         // loop: all part slices
         for (int normPartIndex = 0; normPartIndex < numNormPartSlices; normPartIndex++) {

            if (geoPartItem.isCanceled) {
               return;
            }

            numCompares++;

            final int compareIndex = normTourIndex + normPartIndex;

            /*
             * Make sure the compare index is not larger than the tour index, this happens when the
             * part slices has exeeded the tour slices
             */
            if (compareIndex == numNormTourSlices) {
               latLonDiff = -1;
               break;
            }

            final int latDiff = normPartLatSerie[normPartIndex] - normTourLatSerie[compareIndex];
            final int lonDiff = normPartLonSerie[normPartIndex] - normTourLonSerie[compareIndex];

            // optimize Math.abs() !!!
            final int latDiffAbs = latDiff < 0 ? -latDiff : latDiff;
            final int lonDiffAbs = lonDiff >= 0 ? lonDiff : -lonDiff;

            // summarize all diffs for one tour slice
            latLonDiff += (latDiffAbs + lonDiffAbs);
         }

         // keep diff value
         normLatLonDiff[normTourIndex] = latLonDiff;

         // keep min diff value/index
         if (latLonDiff < minDiffValue && latLonDiff != -1) {

            minDiffValue = latLonDiff;

            // keep tour index where the min diff occured
            normMinDiffIndex = normTourIndex;
         }

      }

      final int[] norm2origIndices = normalizedTour.normalized2OriginalIndices;

      // a tour is available and could be compared
      if (normMinDiffIndex > -1) {

         final int origStartIndex = norm2origIndices[normMinDiffIndex];
         final int origEndIndex = norm2origIndices[normMinDiffIndex + numNormPartSlices - 1];

         comparerItem.avgPulse = tourData.computeAvg_PulseSegment(origStartIndex, origEndIndex);
         comparerItem.avgSpeed = TourManager.computeTourSpeed(tourData, origStartIndex, origEndIndex);

         comparerItem.tourFirstIndex = origStartIndex;
         comparerItem.tourLastIndex = origEndIndex;

         comparerItem.tourTitle = tourData.getTourTitle();
         comparerItem.tourType = tourData.getTourType();

         comparerItem.avgAltimeter = tourData.computeAvg_FromValues(tourData.getAltimeterSerie(), origStartIndex, origEndIndex);

         final int elapsedTime = tourData.timeSerie[origEndIndex] - tourData.timeSerie[origStartIndex];
         final int movingTime = Math.max(0, elapsedTime - tourData.getBreakTime(origStartIndex, origEndIndex));
         comparerItem.elapsedTime = elapsedTime;
         comparerItem.movingTime = movingTime;
         comparerItem.distance = tourData.distanceSerie[origEndIndex] - tourData.distanceSerie[origStartIndex];
      }

      final ZonedDateTime tourStartTime = tourData.getTourStartTime();

      comparerItem.tourStartTime = tourStartTime;
      comparerItem.tourStartTimeMS = TimeTools.toEpochMilli(tourStartTime);

      comparerItem.minDiffValue = (long) (normMinDiffIndex < 0 ? -1 : normLatLonDiff[normMinDiffIndex]);

      /*
       * Create data serie for the chart graph from the normalized diff data serie
       */
      final int numTourSlices = tourData.latitudeSerie.length;

      final float[] tourLatLonDiff = new float[numTourSlices];

      int serieIndex = 0;

      // loop: all normalized tour slices
      for (int normIndex = 0; normIndex < numNormTourSlices; normIndex++) {

         final float latLonDiff = normLatLonDiff[normIndex];

         int nextNormIndex = normIndex++;

         if (nextNormIndex >= numNormTourSlices) {
            nextNormIndex = numNormTourSlices - 1;
         }

         final int nextSerieIndex = norm2origIndices[nextNormIndex];

         while (serieIndex < nextSerieIndex && serieIndex < numTourSlices) {

            tourLatLonDiff[serieIndex++] = latLonDiff;
         }
      }

      comparerItem.tourLatLonDiff = tourLatLonDiff;

      if (LOG_TOUR_COMPARING) {

         final float time_Compare = (float) (System.nanoTime() - startComparing) / 1000000;
         final float time_All = (float) (System.nanoTime() - startLoading) / 1000000;
         final float time_Load = (float) (startConvert - startLoading) / 1000000;
         final float time_Convert = (float) (startComparing - startConvert) / 1000000;

         final float cmpAvgTime = numCompares / time_Compare;

         System.out.println(
               String.format(
                     UI.EMPTY_STRING
                           + "[%3d]" // thread //$NON-NLS-1$
                           + " tour %-20s" //$NON-NLS-1$
                           // + "   exec %5d"

                           + "   diff %12d" //$NON-NLS-1$
                           + "   # %5d / %5d" //$NON-NLS-1$

                           + "   cmp %7.0f" //$NON-NLS-1$
                           + "   #cmp %10d" //$NON-NLS-1$
                           + "   #cmpAvg %8.0f" //$NON-NLS-1$

                           + "   all %7.0f ms" //$NON-NLS-1$
                           + "   ld %10.4f" //$NON-NLS-1$
                           + "   cnvrt %10.4f", //$NON-NLS-1$

                     Thread.currentThread().getId(),
                     comparerItem.tourId,
                     //                     loaderItem.executorId,

                     normMinDiffIndex < 0 ? normMinDiffIndex : normLatLonDiff[normMinDiffIndex],
                     numNormTourSlices,
                     numNormPartSlices,

                     time_Compare,
                     numCompares,
                     cmpAvgTime,

                     time_All,
                     time_Load,
                     time_Convert

               ));
         // TODO remove SYSTEM.OUT.PRINTLN
      }
   }

   public static void fireEvent(final GeoCompareEventId eventId, final Object eventData, final IWorkbenchPart part) {

      for (final Object listener : _geoCompareListeners.getListeners()) {
         ((IGeoCompareListener) listener).geoCompareEvent(part, eventId, eventData);
      }
   }

   public static boolean isGeoComparing() {
      return _isGeoComparingOn;
   }

   public static void removeGeoCompareListener(final IGeoCompareListener listener) {

      if (listener != null) {
         _geoCompareListeners.remove(listener);
      }
   }

   /**
    * Sets geo comparing on/off internal state and fires an
    * {@value GeoCompareEventId#SET_COMPARING_ON} or {@value GeoCompareEventId#SET_COMPARING_OFF}
    *
    * @param isGeoComparingOn
    * @param part
    */
   public static void setGeoComparing(final boolean isGeoComparingOn, final IWorkbenchPart part) {

      _isGeoComparingOn = isGeoComparingOn;

      fireEvent(
            isGeoComparingOn
                  ? GeoCompareEventId.SET_COMPARING_ON
                  : GeoCompareEventId.SET_COMPARING_OFF,
            null,
            part);
   }

}
