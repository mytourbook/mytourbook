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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

/**
 * The manages the comparison between reference and all selected tours.
 */
public class TourCompareManager {

   private static TourCompareManager                     _instance;

   private RefTourItem[]                                 _refTourItems;
   private TourData[]                                    _refToursData;

   private final ArrayList<TVICompareResultComparedTour> _comparedTourItems = new ArrayList<>();

   /**
    * internal constructor
    */
   private TourCompareManager() {}

   /**
    * @param refId
    *           Reference Id
    * @return Returns all compared tours for the reference tour with the <code>refId</code> which
    *         are saved in the database
    */
   public static HashMap<Long, StoredComparedTour> getComparedToursFromDb(final long refId) {

      final HashMap<Long, StoredComparedTour> storedComparedTours = new HashMap<>();

      final StringBuilder sb = new StringBuilder();

      sb.append("SELECT"); //$NON-NLS-1$

      sb.append(" tourId,"); //      1 //$NON-NLS-1$
      sb.append(" comparedId,"); //   2 //$NON-NLS-1$
      sb.append(" startIndex,"); //   3 //$NON-NLS-1$
      sb.append(" endIndex,"); //      4 //$NON-NLS-1$
      sb.append(" tourSpeed"); //      5    //$NON-NLS-1$

      sb.append(" FROM " + TourDatabase.TABLE_TOUR_COMPARED); //$NON-NLS-1$
      sb.append(" WHERE refTourId=?"); //$NON-NLS-1$

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         final PreparedStatement statement = conn.prepareStatement(sb.toString());
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

   public static TourCompareManager getInstance() {
      if (_instance == null) {
         _instance = new TourCompareManager();
      }
      return _instance;
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

   public void clearCompareResult() {

      _comparedTourItems.clear();
   }

   /**
    * @param refTourIndex
    *           Index into refTourContext and refToursData
    * @param compareTourData
    *           Tour data of the tour which will be compared
    * @return returns the start index for the ref tour in the compare tour
    */
   private TVICompareResultComparedTour compareTour(final int refTourIndex, final TourData compareTourData) {

      final TVICompareResultComparedTour compareResultItem = new TVICompareResultComparedTour();

      /*
       * normalize the compare tour
       */
      final TourDataNormalizer compareTourNormalizer = new TourDataNormalizer();
      final float[] compareTourDataDistance = compareTourData.getMetricDistanceSerie();
      final int[] compareTourDataTime = compareTourData.timeSerie;

      if (compareTourDataDistance == null || compareTourDataTime == null) {
         return compareResultItem;
      }

      final int numTourSlices = compareTourDataDistance.length;

      // normalize the tour which will be compared
      compareTourNormalizer.normalizeAltitude(compareTourData, 0, numTourSlices - 1);

      final float[] normCompDistances = compareTourNormalizer.getNormalizedDistance();
      final float[] normCompAltitudes = compareTourNormalizer.getNormalizedAltitude();

      if (normCompAltitudes == null || normCompDistances == null) {
         return compareResultItem;
      }

      final int numCompareSlices = normCompAltitudes.length;

      final float[] normCompAltiDiff = new float[numCompareSlices];

      /*
       * reference tour
       */
      final RefTourItem refTour = _refTourItems[refTourIndex];
      final int refMeasureStartIndex = refTour.startIndex;
      final int refMeasureEndIndex = refTour.endIndex;

      // get the reference tour
      final TourData refTourData = _refToursData[refTourIndex];
      if (refTourData == null) {
         return compareResultItem;
      }

      // normalize the reference tour
      final TourDataNormalizer refTourNormalizer = new TourDataNormalizer();
      refTourNormalizer.normalizeAltitude(refTourData, refMeasureStartIndex, refMeasureEndIndex);

      final float[] normRefAltitudes = refTourNormalizer.getNormalizedAltitude();
      if (normRefAltitudes == null) {
         return compareResultItem;
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
         return compareResultItem;
      }

      // get distance for the reference tour
      final float[] distanceSerie = refTourData.getMetricDistanceSerie();
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
      compareResultItem.altitudeDiffSerie = compAltiDif;

      // create the compare result
      compareResultItem.minAltitudeDiff = minAltiDiff;

      compareResultItem.computedStartIndex = compareStartIndex;
      compareResultItem.computedEndIndex = compareEndIndex;

      final int normIndexDiff = (int) (refDistance / TourDataNormalizer.NORMALIZED_DISTANCE);
      compareResultItem.normalizedStartIndex = normCompareIndexStart;
      compareResultItem.normalizedEndIndex = normCompareIndexStart + normIndexDiff;

      final float compareDistance = compareTourDataDistance[compareEndIndex] - compareTourDataDistance[compareStartIndex];
      final int elapsedTime = compareTourDataTime[compareEndIndex] - compareTourDataTime[compareStartIndex];
      final int movingTime = Math.max(0, elapsedTime - compareTourData.getBreakTime(compareStartIndex, compareEndIndex));

      compareResultItem.compareMovingTime = movingTime;
      compareResultItem.compareElapsedTime = elapsedTime;
      compareResultItem.compareDistance = compareDistance;
      compareResultItem.compareSpeed = compareDistance / movingTime * 3.6f;
      compareResultItem.avgAltimeter = getAvgAltimeter(compareTourData, compareStartIndex, compareEndIndex);

      compareResultItem.timeInterval = compareTourData.getDeviceTimeInterval();

      return compareResultItem;
   }

   /**
    * Compares all reference tours with all compare tours
    *
    * @param refTours
    * @param comparedTours
    */
   public void compareTours(final RefTourItem[] refTours, final Object[] comparedTours) {

      _refTourItems = refTours;
      _refToursData = new TourData[refTours.length];

      final int tours2Compare = comparedTours.length * refTours.length;

      final Job compareJob = new Job(Messages.tourCatalog_view_compare_job_title) {

         private void compareTourJob(final RefTourItem[] refTourItems,
                                     final Object[] comparedTours,
                                     final IProgressMonitor monitor) {

            int tourCounter = 0;
            _comparedTourItems.clear();

            // get all reference tours
            loadRefTours();

            // loop: all compare tours
            for (final Object tour : comparedTours) {

               Long tourId;

               if (tour instanceof TVIWizardCompareTour) {
                  tourId = ((TVIWizardCompareTour) tour).tourId;
               } else if (tour instanceof Long) {
                  tourId = (Long) tour;
               } else {
                  // ignore checked year/month
                  continue;
               }

               // load compared tour from the database
               final TourData compareTourData = TourManager.getInstance().getTourData(tourId);

               if (compareTourData != null
                     && compareTourData.timeSerie != null
                     && compareTourData.timeSerie.length > 0) {

                  // loop: all reference tours
                  for (int refTourIndex = 0; refTourIndex < refTourItems.length; refTourIndex++) {

                     if (monitor.isCanceled()) {
                        showCompareResults();
                        return;
                     }

                     // compare the tour
                     final TVICompareResultComparedTour compareResult = compareTour(
                           refTourIndex,
                           compareTourData);

                     // ignore tours which could not be compared
                     if (compareResult.computedStartIndex != -1) {

                        compareResult.refTour = refTourItems[refTourIndex];
                        compareResult.comparedTourData = compareTourData;

                        _comparedTourItems.add(compareResult);
                     }

                     // update the message in the progress monitor
                     monitor.subTask(NLS.bind(//
                           Messages.tourCatalog_view_compare_job_subtask,
                           ++tourCounter,
                           tours2Compare));

                     monitor.worked(1);
                  }
               }

            }
         }

         @Override
         protected IStatus run(final IProgressMonitor monitor) {

            monitor.beginTask(Messages.tourCatalog_view_compare_job_task, tours2Compare);

            compareTourJob(refTours, comparedTours, monitor);

            monitor.done();

            showCompareResults();

            return Status.OK_STATUS;
         }
      };

      compareJob.setUser(true);
      compareJob.schedule();
   }

   private float getAvgAltimeter(final TourData tourData, final int compareStartIndex, final int compareEndIndex) {

      final float[] altimeterSerie = tourData.getAltimeterSerie();

      return tourData.computeAvg_FromValues(altimeterSerie, compareStartIndex, compareEndIndex);
   }

   /**
    * @return Returns the reference tours which has been compared
    */
   public RefTourItem[] getComparedReferenceTours() {
      return _refTourItems;
   }

   /**
    * @return Returns the compare result with all compared tours
    */
   public TVICompareResultComparedTour[] getComparedTours() {
      return _comparedTourItems.toArray(new TVICompareResultComparedTour[_comparedTourItems.size()]);
   }

   /**
    * Get the tour data for all reference tours
    *
    * @param refTourContext
    */
   private void loadRefTours() {

      for (int tourIndex = 0; tourIndex < _refTourItems.length; tourIndex++) {

         final RefTourItem refTour = _refTourItems[tourIndex];

         _refTourItems[tourIndex] = refTour;
         _refToursData[tourIndex] = TourManager.getInstance().getTourData(refTour.tourId);
      }
   }

   /**
    * @param isNextTour
    *           When <code>true</code> then navigate to the next tour, when <code>false</code>
    *           then navigate to the previous tour.
    * @return Returns the navigated tour or <code>null</code> when there is no next/previous tour.
    */
   Object navigateTour(final boolean isNextTour) {

      Object navigatedTour = null;

      final IWorkbench workbench = PlatformUI.getWorkbench();
      final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
      final IWorkbenchPage activePage = window.getActivePage();

      final IViewPart yearStatView = activePage.findView(YearStatisticView.ID);
      if (yearStatView instanceof YearStatisticView) {
         navigatedTour = ((YearStatisticView) yearStatView).navigateTour(isNextTour);
      }

      final IViewPart comparedTours = activePage.findView(TourCompareResultView.ID);
      if (comparedTours instanceof TourCompareResultView) {
         navigatedTour = ((TourCompareResultView) comparedTours).navigateTour(isNextTour);
      }

      return navigatedTour;
   }

   private void showCompareResults() {

      Display.getDefault().asyncExec(new Runnable() {

         @Override
         public void run() {

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
                     view.reloadViewer();
                  }

               } catch (final PartInitException e) {
                  ErrorDialog.openError(window.getShell(),
                        "Error", //$NON-NLS-1$
                        e.getMessage(),
                        e
                              .getStatus());
                  e.printStackTrace();
               } catch (final WorkbenchException e) {
                  e.printStackTrace();
               }
            }

         }
      });
   }
}
