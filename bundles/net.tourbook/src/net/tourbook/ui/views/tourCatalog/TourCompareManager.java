/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

/**
 * The manages the comparison between reference and all selected tours.
 */
public class TourCompareManager {

   private static final String                                  NL                 = UI.NEW_LINE;

   private static final String                                  NUMBER_FORMAT_1F   = "%.1f";                                             //$NON-NLS-1$

   private static ArrayList<RefTourItem>                        _allRefTourItems;

   private final static ArrayList<TVICompareResultComparedTour> _comparedTourItems = new ArrayList<>();

   private static ThreadPoolExecutor                            _compareTour_Executor;
   private static ArrayBlockingQueue<Long>                      _compareTour_Queue = new ArrayBlockingQueue<>(Util.NUMBER_OF_PROCESSORS);

   static {

      final ThreadFactory threadFactory = new ThreadFactory() {

         @Override
         public Thread newThread(final Runnable r) {

            final Thread thread = new Thread(r, "Comparing tours by elevation");//$NON-NLS-1$

            thread.setPriority(Thread.MIN_PRIORITY);
            thread.setDaemon(true);

            return thread;
         }
      };

      _compareTour_Executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Util.NUMBER_OF_PROCESSORS, threadFactory);
   }

   public static void clearCompareResult() {

      _comparedTourItems.clear();
   }

   /**
    * Compares all reference tours with all compare tours
    *
    * @param selectedRefTourItems
    * @param allComparedItems
    */
   public static void compareTours(final ArrayList<RefTourItem> selectedRefTourItems, final Object[] allComparedItems) {

      final int numComparedTours = allComparedItems.length;

      _allRefTourItems = selectedRefTourItems;

      try {

         final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {

               final long startTime = System.currentTimeMillis();
               long lastUpdateTime = startTime;

               monitor.beginTask(String.format("Comparing tours"), numComparedTours);

               _comparedTourItems.clear();

               // load all reference tour data
               final ArrayList<TourData> allRefTourData = new ArrayList<>();
               for (final RefTourItem refTourItem : _allRefTourItems) {
                  allRefTourData.add(TourManager.getInstance().getTourData(refTourItem.tourId));
               }

               int tourCounter = 1;
               int lastUpdateNumItems = 1;
               int sumComparedTours = 0;

               // loop: all compared tours
               for (final Object comparedItem : allComparedItems) {

                  final long currentTime = System.currentTimeMillis();
                  final long timeDiff = currentTime - lastUpdateTime;

                  Long tourId;

                  if (comparedItem instanceof TVIWizardCompareTour) {

                     tourId = ((TVIWizardCompareTour) comparedItem).tourId;

                  } else if (comparedItem instanceof Long) {

                     tourId = (Long) comparedItem;

                  } else {

                     // ignore checked year/month
                     continue;
                  }

                  // reduce logging
                  if (timeDiff > 200

                        // update UI for the last tour otherwise it looks like that not all data are converted
                        || tourCounter == numComparedTours) {

                     lastUpdateTime = currentTime;

                     final long numTourDiff = tourCounter - lastUpdateNumItems;
                     lastUpdateNumItems = tourCounter;
                     sumComparedTours += numTourDiff;

                     final String percentValue = String.format(NUMBER_FORMAT_1F, (float) tourCounter / numComparedTours * 100.0);

                     // Compared tours: {0} of {1} - {2} % - {3} Δ
                     monitor.subTask(NLS.bind("Compared tours: {0} of {1} - {2} % - {3} Δ",
                           new Object[] {
                                 sumComparedTours,
                                 numComparedTours,
                                 percentValue,
                                 numTourDiff,
                           }));
                  }

                  tourCounter++;

                  final boolean isLastCompare = tourCounter > numComparedTours;

                  compareTours_Concurrent(tourId, allRefTourData, monitor, isLastCompare);
               }
            }

         };

         new ProgressMonitorDialog(TourbookPlugin.getAppShell()).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {

         StatusUtil.showStatus(e);
         Thread.currentThread().interrupt();
      }
   }

   private static void compareTours_Concurrent(final Long tourId,
                                               final ArrayList<TourData> allRefTourData,
                                               final IProgressMonitor monitor,
                                               final boolean isLastCompare) {

      // put tour ID (queue item) into the queue AND wait when it is full

      try {

         _compareTour_Queue.put(tourId);

      } catch (final InterruptedException e) {

         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      _compareTour_Executor.submit(() -> {

         // get last added item
         final Long queueItem_TourId = _compareTour_Queue.poll();

         if (queueItem_TourId == null) {
//            showCompareResults();
            return;
         }

         // load compared tour from the database
         final TourData compareTourData = TourManager.getInstance().getTourData(tourId);

         if (compareTourData != null
               && compareTourData.timeSerie != null
               && compareTourData.timeSerie.length > 0) {

            // loop: all reference tours
            for (int refTourIndex = 0; refTourIndex < _allRefTourItems.size(); refTourIndex++) {

               if (monitor.isCanceled()) {
                  showCompareResults();
                  return;
               }

               final RefTourItem refTourItem = _allRefTourItems.get(refTourIndex);

               // compare the tour
               final TVICompareResultComparedTour compareResult = compareTours_OneTour(
                     refTourItem,
                     allRefTourData.get(refTourIndex),
                     compareTourData);

               // ignore tours which could not be compared
               if (compareResult.computedStartIndex != -1) {

                  compareResult.refTour = refTourItem;
                  compareResult.comparedTourData = compareTourData;

                  _comparedTourItems.add(compareResult);
               }
            }

            monitor.worked(1);
         }

         if (isLastCompare) {
            showCompareResults();
         }

      });
   }

   /**
    * @param refTourIndex
    *           Index into refTourContext and refToursData
    * @param compareTourData
    *           Tour data of the tour which will be compared
    * @param compareTourData2
    * @return returns the start index for the ref tour in the compare tour
    */
   private static TVICompareResultComparedTour compareTours_OneTour(final RefTourItem refTour_Item,
                                                                    final TourData refTour_Data,
                                                                    final TourData compareTourData) {

      final TVICompareResultComparedTour compareResult = new TVICompareResultComparedTour();

      /*
       * normalize the compare tour
       */
      final TourDataNormalizer compareTourNormalizer = new TourDataNormalizer();
      final float[] compareTourDataDistance = compareTourData.getMetricDistanceSerie();
      final int[] compareTourDataTime = compareTourData.timeSerie;

      if (compareTourDataDistance == null || compareTourDataTime == null) {
         return compareResult;
      }

      final int numTourSlices = compareTourDataDistance.length;

      // normalize the tour which will be compared
      compareTourNormalizer.normalizeAltitude(compareTourData, 0, numTourSlices - 1);

      final float[] normCompDistances = compareTourNormalizer.getNormalizedDistance();
      final float[] normCompAltitudes = compareTourNormalizer.getNormalizedAltitude();

      if (normCompAltitudes == null || normCompDistances == null) {
         return compareResult;
      }

      final int numCompareSlices = normCompAltitudes.length;

      final float[] normCompAltiDiff = new float[numCompareSlices];

      /*
       * Reference tour item
       */
      final int refMeasureStartIndex = refTour_Item.startIndex;
      final int refMeasureEndIndex = refTour_Item.endIndex;

      // get the reference tour
      if (refTour_Data == null) {
         return compareResult;
      }

      // normalize the reference tour
      final TourDataNormalizer refTourNormalizer = new TourDataNormalizer();
      refTourNormalizer.normalizeAltitude(refTour_Data, refMeasureStartIndex, refMeasureEndIndex);

      final float[] normRefAltitudes = refTourNormalizer.getNormalizedAltitude();
      if (normRefAltitudes == null) {
         return compareResult;
      }

      float minAltiDiff = Float.MAX_VALUE;

      // start index of the reference tour in the compare tour
      int normCompareIndexStart = -1;

      final int compareLastIndex = numCompareSlices;

      for (int normCompareIndex = 0; normCompareIndex < numCompareSlices; normCompareIndex++) {

         float altitudeDiff = -1;

         // loop: all data in the reference tour
         for (int normRefIndex = 0; normRefIndex < normRefAltitudes.length; normRefIndex++) {

            final int compareRefIndex = normCompareIndex + normRefIndex;

            /*
             * make sure the ref index is not bigger than the compare index, this can happen
             * when the reference data exeed the compare data
             */
            if (compareRefIndex == compareLastIndex) {
               altitudeDiff = -1;
               break;
            }

            // get the altitude difference between the reference and the compared value
            altitudeDiff += Math.abs(normRefAltitudes[normRefIndex] - normCompAltitudes[compareRefIndex]);
         }

         // keep altitude difference
         normCompAltiDiff[normCompareIndex] = altitudeDiff;

         /*
          * find the lowest altitude difference, this will be the start point of the reference
          * tour in the compared tour
          */
         if (altitudeDiff < minAltiDiff && altitudeDiff != -1) {
            minAltiDiff = altitudeDiff;
            normCompareIndexStart = normCompareIndex;
         }
      }

      // exit if tour was not found
      if (normCompareIndexStart == -1) {
         return compareResult;
      }

      // get distance for the reference tour
      final float[] distanceSerie = refTour_Data.getMetricDistanceSerie();
      final float refDistance = distanceSerie[refMeasureEndIndex] - distanceSerie[refMeasureStartIndex];

      // get the start/end point in the compared tour
      final float compDistanceStart = normCompDistances[normCompareIndexStart];
      final float compDistanceEnd = compDistanceStart + refDistance;

      /*
       * get the start point in the compare tour
       */
      int compareStartIndex = 0;
      for (; compareStartIndex < numTourSlices; compareStartIndex++) {
         if (compareTourDataDistance[compareStartIndex] >= compDistanceStart) {
            break;
         }
      }

      /*
       * get the end point in the compare tour
       */
      int compareEndIndex = compareStartIndex;
      float oldDistance = compareTourDataDistance[compareEndIndex];
      for (; compareEndIndex < numTourSlices; compareEndIndex++) {
         if (compareTourDataDistance[compareEndIndex] >= compDistanceEnd) {
            break;
         }

         final float newDistance = compareTourDataDistance[compareEndIndex];

         if (oldDistance == newDistance) {} else {
            oldDistance = newDistance;
         }
      }
      compareEndIndex = Math.min(compareEndIndex, numTourSlices - 1);

      /*
       * create data serie for altitude difference
       */
      final float[] normDistanceSerie = compareTourNormalizer.getNormalizedDistance();
      final float[] compAltiDif = new float[numTourSlices];

      final int maxNormIndex = normDistanceSerie.length - 1;
      int normIndex = 0;

      for (int compIndex = 0; compIndex < numTourSlices; compIndex++) {

         final float compDistance = compareTourDataDistance[compIndex];
         float normDistance = normDistanceSerie[normIndex];

         while (compDistance > normDistance && normIndex < maxNormIndex) {
            normDistance = normDistanceSerie[++normIndex];
         }

         compAltiDif[compIndex] = normCompAltiDiff[normIndex];
      }
      compareResult.altitudeDiffSerie = compAltiDif;

      // create the compare result
      compareResult.minAltitudeDiff = minAltiDiff;

      compareResult.computedStartIndex = compareStartIndex;
      compareResult.computedEndIndex = compareEndIndex;

      final int normIndexDiff = (int) (refDistance / TourDataNormalizer.NORMALIZED_DISTANCE);
      compareResult.normalizedStartIndex = normCompareIndexStart;
      compareResult.normalizedEndIndex = normCompareIndexStart + normIndexDiff;

      final float compareDistance = compareTourDataDistance[compareEndIndex] - compareTourDataDistance[compareStartIndex];
      final int elapsedTime = compareTourDataTime[compareEndIndex] - compareTourDataTime[compareStartIndex];
      final int movingTime = Math.max(0, elapsedTime - compareTourData.getBreakTime(compareStartIndex, compareEndIndex));

      compareResult.compareMovingTime = movingTime;
      compareResult.compareElapsedTime = elapsedTime;
      compareResult.compareDistance = compareDistance;
      compareResult.compareSpeed = compareDistance / movingTime * 3.6f;
      compareResult.avgAltimeter = getAvgAltimeter(compareTourData, compareStartIndex, compareEndIndex);

      compareResult.timeInterval = compareTourData.getDeviceTimeInterval();

      return compareResult;
   }

   /**
    * @param modifiedTours
    * @return Returns an expression to select tour id's in the WHERE clause
    */
   private static String createRefId_IN(final ArrayList<Long> allSelectedRefTourIds) {

      if (allSelectedRefTourIds.isEmpty()) {
         return UI.EMPTY_STRING;
      }

      final StringBuilder sb = new StringBuilder();
      boolean isFirst = true;

      for (final Long refTourId : allSelectedRefTourIds) {

         if (isFirst) {
            isFirst = false;
         } else {
            sb.append(',');
         }

         sb.append(Long.toString(refTourId));
      }

      return sb.toString();
   }

   /**
    * Create {@link RefTourItem}'s for all provided ref tour id's
    *
    * @param allRefTourIds
    * @return
    */
   static ArrayList<RefTourItem> createRefTourItems(final ArrayList<Long> allRefTourIds) {

      final ArrayList<RefTourItem> allSelectedRefTourItems = new ArrayList<>();

      final String refId_IN = createRefId_IN(allRefTourIds);

      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //                                       //$NON-NLS-1$

            + " refId," + NL //                                   1  //$NON-NLS-1$
            + " TourData_tourId," + NL //                         2  //$NON-NLS-1$

            + " label," + NL //                                   3  //$NON-NLS-1$
            + " startIndex," + NL //                              4  //$NON-NLS-1$
            + " endIndex" + NL //                                 5  //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_REFERENCE + NL //   //$NON-NLS-1$

            + " WHERE refId IN (" + refId_IN + " )" + NL //          //$NON-NLS-1$

            + " ORDER BY label" + NL //                              //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         final ResultSet result = statement.executeQuery();

         while (result.next()) {

            final RefTourItem refItem = new RefTourItem();

            refItem.refId = result.getLong(1);
            refItem.tourId = result.getLong(2);

            refItem.label = result.getString(3);
            refItem.startIndex = result.getInt(4);
            refItem.endIndex = result.getInt(5);

            allSelectedRefTourItems.add(refItem);
         }

      } catch (final SQLException e) {
         net.tourbook.ui.UI.showSQLException(e);
      }

      return allSelectedRefTourItems;
   }

   private static float getAvgAltimeter(final TourData tourData, final int compareStartIndex, final int compareEndIndex) {

      final float[] altimeterSerie = tourData.getAltimeterSerie();

      return tourData.computeAvg_FromValues(altimeterSerie, compareStartIndex, compareEndIndex);
   }

   /**
    * @return Returns the reference tours which has been compared
    */
   public static ArrayList<RefTourItem> getComparedReferenceTours() {

      return _allRefTourItems;
   }

   /**
    * @return Returns the compare result with all compared tours
    */
   public static TVICompareResultComparedTour[] getComparedTours() {

      return _comparedTourItems.toArray(new TVICompareResultComparedTour[_comparedTourItems.size()]);
   }

   /**
    * @param refId
    *           Reference Id
    * @return Returns all compared tours for the reference tour with the <code>refId</code> which
    *         are saved in the database
    */
   public static HashMap<Long, StoredComparedTour> getComparedToursFromDb(final long refId) {

      final HashMap<Long, StoredComparedTour> storedComparedTours = new HashMap<>();
      final String sql = UI.EMPTY_STRING

            + "SELECT" + NL //               //$NON-NLS-1$

            + " tourId," + NL //          1  //$NON-NLS-1$
            + " comparedId," + NL //      2  //$NON-NLS-1$
            + " startIndex," + NL //      3  //$NON-NLS-1$
            + " endIndex," + NL //        4  //$NON-NLS-1$
            + " tourSpeed" + NL //        5  //$NON-NLS-1$

            + " FROM " + TourDatabase.TABLE_TOUR_COMPARED + NL //  //$NON-NLS-1$
            + " WHERE refTourId=?" + NL //   //$NON-NLS-1$
      ;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sql);
         statement.setLong(1, refId);

         final ResultSet result = statement.executeQuery();
         while (result.next()) {

            final StoredComparedTour storedComparedTour = new StoredComparedTour();

            final long dbTourId = result.getLong(1);
            storedComparedTour.comparedId = result.getLong(2);
            storedComparedTour.startIndex = result.getInt(3);
            storedComparedTour.endIndex = result.getInt(4);
            storedComparedTour.tourSpeed = result.getFloat(5);

            storedComparedTours.put(dbTourId, storedComparedTour);
         }

      } catch (final SQLException e) {
         UI.showSQLException(e);
      }

      return storedComparedTours;
   }

   /**
    * @param isNextTour
    *           When <code>true</code> then navigate to the next tour, when <code>false</code>
    *           then navigate to the previous tour.
    * @return Returns the navigated tour or <code>null</code> when there is no next/previous tour.
    */
   static Object navigateTour(final boolean isNextTour) {

      Object navigatedTour = null;

      final IWorkbench workbench = PlatformUI.getWorkbench();
      final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
      final IWorkbenchPage activePage = window.getActivePage();

      final IViewPart yearStatView = activePage.findView(RefTour_YearStatistic_View.ID);
      if (yearStatView instanceof RefTour_YearStatistic_View) {
         navigatedTour = ((RefTour_YearStatistic_View) yearStatView).navigateTour(isNextTour);
      }

      final IViewPart comparedTours = activePage.findView(TourCompareResultView.ID);
      if (comparedTours instanceof TourCompareResultView) {
         navigatedTour = ((TourCompareResultView) comparedTours).navigateTour(isNextTour);
      }

      return navigatedTour;
   }

   /**
    * Removes a compared tour from the database
    *
    * @param compTourItem
    */
   static boolean removeComparedTourFromDb(final Long compId) {

      boolean returnResult = false;

      final EntityManager em = TourDatabase.getInstance().getEntityManager();
      final EntityTransaction ts = em.getTransaction();

      try {
         final TourCompared compTour = em.find(TourCompared.class, compId);

         if (compTour != null) {
            ts.begin();
            em.remove(compTour);
            ts.commit();
         }

      } catch (final Exception e) {
         e.printStackTrace();
      } finally {
         if (ts.isActive()) {
            ts.rollback();
         } else {
            returnResult = true;
         }
         em.close();
      }

      return returnResult;
   }

   /**
    * Saves the {@link TVICompareResultComparedTour} item and updates the item with the saved data
    *
    * @param comparedTourItem
    * @param em
    * @param ts
    */
   public static void saveComparedTourItem(final TVICompareResultComparedTour comparedTourItem,
                                           final EntityManager em,
                                           final EntityTransaction ts) {

      final TourData tourData = comparedTourItem.comparedTourData;

      final float avgPulse = tourData.computeAvg_PulseSegment(
            comparedTourItem.computedStartIndex,
            comparedTourItem.computedEndIndex);

      final float speed = TourManager.computeTourSpeed(
            tourData,
            comparedTourItem.computedStartIndex,
            comparedTourItem.computedEndIndex);

      final int tourDeviceTime_Elapsed = TourManager.computeTourDeviceTime_Elapsed(
            tourData,
            comparedTourItem.computedStartIndex,
            comparedTourItem.computedEndIndex);

      final TourCompared comparedTour = new TourCompared();

      comparedTour.setStartIndex(comparedTourItem.computedStartIndex);
      comparedTour.setEndIndex(comparedTourItem.computedEndIndex);
      comparedTour.setTourId(tourData.getTourId());
      comparedTour.setRefTourId(comparedTourItem.refTour.refId);

      comparedTour.setTourDate(tourData.getTourStartTimeMS());
      comparedTour.setStartYear(tourData.getTourStartTime().getYear());

      comparedTour.setAvgPulse(avgPulse);
      comparedTour.setTourSpeed(speed);
      comparedTour.setTourDeviceTime_Elapsed(tourDeviceTime_Elapsed);

      // persist entity
      ts.begin();
      em.persist(comparedTour);
      ts.commit();

      // updata saved data
      comparedTourItem.compId = comparedTour.getComparedId();
      comparedTourItem.dbStartIndex = comparedTourItem.computedStartIndex;
      comparedTourItem.dbEndIndex = comparedTourItem.computedEndIndex;

      comparedTourItem.dbSpeed = speed;
      comparedTourItem.dbElapsedTime = tourDeviceTime_Elapsed;
   }

   private static void showCompareResults() {

      Display.getDefault().asyncExec(() -> {

         final IWorkbench workbench = PlatformUI.getWorkbench();
         final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

         if (window != null) {

            try {

               // show compare result perspective
               workbench.showPerspective(PerspectiveFactoryCompareTours.PERSPECTIVE_ID, window);

               final TourCompareResultView view = (TourCompareResultView) Util.showView(
                     TourCompareResultView.ID,
                     true);

               if (view != null) {
                  view.updateViewer();
               }

            } catch (final WorkbenchException e) {
               StatusUtil.log(e);
            }
         }
      });
   }
}
