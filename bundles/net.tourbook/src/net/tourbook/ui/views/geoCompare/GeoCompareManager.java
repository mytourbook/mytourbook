/*******************************************************************************
 * Copyright (C) 2018, 2023 Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.ElevationGainLoss;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.referenceTour.ReferenceTimelineView;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

public class GeoCompareManager {

   private static final IDialogSettings _state = TourbookPlugin.getState("net.tourbook.ui.views.geoCompare.GeoCompareManager"); //$NON-NLS-1$
   //
   //
   private static final String                               STATE_IS_GEO_COMPARE_ON = "STATE_IS_GEO_COMPARE_ON";                 //$NON-NLS-1$

   private static final int                                  COMPARATOR_THREADS      = Runtime.getRuntime().availableProcessors();
   private static ThreadPoolExecutor                         _comparerExecutor;

   private static final LinkedBlockingDeque<GeoComparedTour> _compareWaitingQueue    = new LinkedBlockingDeque<>();
   private static final ListenerList<IGeoCompareListener>    _geoCompareListeners    = new ListenerList<>(ListenerList.IDENTITY);

   private static boolean                                    _isGeoComparingOn;
   private static boolean                                    IS_LOG_TOUR_COMPARING   = false;

   static {

      /*
       * Setup comparer executer
       */

      final ThreadFactory threadFactory = runnable -> {

         final Thread thread = new Thread(runnable, "Comparing geo tours");//$NON-NLS-1$

         thread.setPriority(Thread.MIN_PRIORITY);
         thread.setDaemon(true);

         return thread;
      };

//      System.out.println(String.format(
//            "[%s] Comparing tours with %d threads", //$NON-NLS-1$
//            GeoCompareManager.class.getSimpleName(),
//            COMPARATOR_THREADS));

      _comparerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(COMPARATOR_THREADS, threadFactory);
   }

   protected final static IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   public static void addGeoCompareEventListener(final IGeoCompareListener listener) {

      _geoCompareListeners.add(listener);
   }

   /**
    * @param geoCompareData
    * @param geoPartView
    */
   static void compareGeoTours(final GeoCompareData geoCompareData, final GeoCompareView geoPartView) {

      for (final long tourId : geoCompareData.tourIds) {

         final GeoComparedTour geoComparedTour_QueueItem = new GeoComparedTour(tourId, geoCompareData);

         // keep compared tour
         geoCompareData.allGeoComparedTours.add(geoComparedTour_QueueItem);

         _compareWaitingQueue.add(geoComparedTour_QueueItem);

         _comparerExecutor.submit(new Runnable() {
            @Override
            public void run() {

               // get last added queue item
               final GeoComparedTour geoComparedTour_FromQueue = _compareWaitingQueue.pollFirst();

               if (geoComparedTour_FromQueue == null) {
                  return;
               }

               // check if this comparison is canceled
               final GeoCompareData geoCompareData = geoComparedTour_FromQueue.geoCompareData;
               if (geoCompareData.isCanceled) {
                  return;
               }

               try {

                  compareGeoTours_OneTour(geoComparedTour_FromQueue);

               } catch (final Exception e) {

                  StatusUtil.log(e);
               }

               geoPartView.compare_50_OneTourIsCompared(geoCompareData);
            }
         });
      }
   }

   private static void compareGeoTours_OneTour(final GeoComparedTour geoComparedTour) {

      /*
       * Load tour data
       */
      final long startLoading = System.nanoTime();

      final TourData tourData = TourManager.getInstance().getTourData(geoComparedTour.tourId);

      /*
       * Normalize data
       */
      final long startConvert = System.nanoTime();

      final GeoCompareData geoCompareData = geoComparedTour.geoCompareData;
      final NormalizedGeoData normalizedTourPart = geoCompareData.normalizedTourPart;
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

            if (geoCompareData.isCanceled) {
               return;
            }

            numCompares++;

            final int compareIndex = normTourIndex + normPartIndex;

            /*
             * Make sure the compare index is not larger than the tour index, this happens when the
             * part slices has exceeded the tour slices
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

            // keep tour index where the min diff occurred
            normMinDiffIndex = normTourIndex;
         }
      }

      final ZonedDateTime tourStartTime = tourData.getTourStartTime();
      geoComparedTour.tourStartTime = tourStartTime;
      geoComparedTour.tourYear = tourStartTime.getYear();
      geoComparedTour.tourStartTimeMS = TimeTools.toEpochMilli(tourStartTime);

      final int[] norm2OrigIndices = normalizedTour.normalized2OriginalIndices;

      // a tour is available and could be compared
      if (normMinDiffIndex > -1) {

         final int origStartIndex = norm2OrigIndices[normMinDiffIndex];
         final int origEndIndex = norm2OrigIndices[normMinDiffIndex + numNormPartSlices - 1];

         geoComparedTour.avgPulse = tourData.computeAvg_PulseSegment(origStartIndex, origEndIndex);
         geoComparedTour.maxPulse = tourData.computeMax_FromValues(tourData.getPulse_SmoothedSerie(), origStartIndex, origEndIndex);

         geoComparedTour.avgSpeed = TourManager.computeTourSpeed(tourData, origStartIndex, origEndIndex);

         geoComparedTour.tourFirstIndex = origStartIndex;
         geoComparedTour.tourLastIndex = origEndIndex;

         geoComparedTour.tourTitle = tourData.getTourTitle();
         geoComparedTour.tourType = tourData.getTourType();

         geoComparedTour.avgAltimeter = tourData.computeAvg_FromValues(tourData.getAltimeterSerie(), origStartIndex, origEndIndex);

         final ElevationGainLoss elevationGainLoss = tourData.computeAltitudeUpDown(origStartIndex, origEndIndex);
         if (elevationGainLoss != null) {
            geoComparedTour.elevationGainAbsolute = elevationGainLoss.getElevationGain() / UI.UNIT_VALUE_ELEVATION;
            geoComparedTour.elevationLossAbsolute = elevationGainLoss.getElevationLoss() / UI.UNIT_VALUE_ELEVATION;
         }

         final int elapsedTime = tourData.timeSerie[origEndIndex] - tourData.timeSerie[origStartIndex];
         final int movingTime = Math.max(0, elapsedTime - tourData.getBreakTime(origStartIndex, origEndIndex));
         final int recordedTime = Math.max(0, elapsedTime - tourData.getPausedTime(origStartIndex, origEndIndex));
         geoComparedTour.elapsedTime = elapsedTime;
         geoComparedTour.movingTime = movingTime;
         geoComparedTour.recordedTime = recordedTime;

         final float distance = tourData.distanceSerie[origEndIndex] - tourData.distanceSerie[origStartIndex];
         geoComparedTour.distance = distance;

         final long time = _prefStore.getBoolean(ITourbookPreferences.APPEARANCE_IS_PACEANDSPEED_FROM_RECORDED_TIME)
               ? recordedTime
               : movingTime;
         geoComparedTour.avgPace = distance == 0
               ? 0
               : time * 1000 / distance;
      }

      geoComparedTour.minDiffValue = (long) (normMinDiffIndex < 0 ? -1 : normLatLonDiff[normMinDiffIndex]);

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

         final int nextSerieIndex = norm2OrigIndices[nextNormIndex];

         while (serieIndex < nextSerieIndex && serieIndex < numTourSlices) {

            tourLatLonDiff[serieIndex++] = latLonDiff;
         }
      }

      geoComparedTour.tourLatLonDiff = tourLatLonDiff;

      if (IS_LOG_TOUR_COMPARING) {

         final float time_Compare = (float) (System.nanoTime() - startComparing) / 1000000;
         final float time_All = (float) (System.nanoTime() - startLoading) / 1000000;
         final float time_Load = (float) (startConvert - startLoading) / 1000000;
         final float time_Convert = (float) (startComparing - startConvert) / 1000000;

         final float cmpAvgTime = numCompares / time_Compare;

         System.out.println(String.format(

               UI.EMPTY_STRING
                     + "[%3d]" // thread           //$NON-NLS-1$
                     + " tour %-20s" //            //$NON-NLS-1$
                     // + "   exec %5d"

                     + "   diff %12d" //           //$NON-NLS-1$
                     + "   # %5d / %5d" //         //$NON-NLS-1$

                     + "   cmp %7.0f" //           //$NON-NLS-1$
                     + "   #cmp %10d" //           //$NON-NLS-1$
                     + "   #cmpAvg %8.0f" //       //$NON-NLS-1$

                     + "   all %7.0f ms" //        //$NON-NLS-1$
                     + "   ld %10.4f" //           //$NON-NLS-1$
                     + "   cnvrt %10.4f", //       //$NON-NLS-1$

               Thread.currentThread().getId(),
               geoComparedTour.tourId,
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
      }

      // set flag that this comparison is done and can be displayed in the UI (year statistic view)
      geoComparedTour.isGeoCompareDone = true;
   }

   public static void fireEvent(final GeoCompareEventId eventId, final Object eventData, final IWorkbenchPart part) {

      for (final Object listener : _geoCompareListeners.getListeners()) {
         ((IGeoCompareListener) listener).geoCompareEvent(part, eventId, eventData);
      }
   }

   /**
    * @return Returns <code>true</code> when geo comparing is enabled
    */
   public static boolean isGeoComparingOn() {

      return _isGeoComparingOn;
   }

   /**
    * @param isNextTour
    *           When <code>true</code> then navigate to the next tour, when <code>false</code>
    *           then navigate to the previous tour.
    * @return Returns the navigated tour or <code>null</code> when there is no next/previous tour.
    */
   public static Object navigateTour(final boolean isNextTour) {

      Object navigatedTour = null;

      final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

      /*
       * Firstly navigate in the compare result view when view is available
       */
      final IViewPart comparedTours = activePage.findView(GeoCompareView.ID);

      if (comparedTours instanceof GeoCompareView) {

         navigatedTour = ((GeoCompareView) comparedTours).navigateTour(isNextTour);
      }

      /*
       * Secondly navigate in the year statistic view when view is available
       */
      if (navigatedTour == null) {

         final IViewPart yearStatView = activePage.findView(ReferenceTimelineView.ID);

         if (yearStatView instanceof ReferenceTimelineView) {

            navigatedTour = ((ReferenceTimelineView) yearStatView).navigateTour(isNextTour);
         }
      }

      return navigatedTour;
   }

   public static void removeGeoCompareListener(final IGeoCompareListener listener) {

      if (listener != null) {
         _geoCompareListeners.remove(listener);
      }
   }

   public static void restoreState() {

      _isGeoComparingOn = Util.getStateBoolean(_state, STATE_IS_GEO_COMPARE_ON, true);
   }

   public static void saveState() {

      _state.put(STATE_IS_GEO_COMPARE_ON, _isGeoComparingOn);
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
