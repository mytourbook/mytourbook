/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm
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
package net.tourbook.ui.views.tourMap;

import java.util.ArrayList;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.TreeViewerItem;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;

/**
 * The TourCompareManager manages the comparision between tours
 */
public class TourCompareManager {

	private static TourCompareManager	fInstance;

	private boolean						isComparing		= false;

	private TourReference[]				refTourContext;
	private TourData[]					refToursData;

	protected CompareResultView			tourComparerView;

	private ArrayList<TVICompareResult>	comparedTours	= new ArrayList<TVICompareResult>();

	/**
	 * internal constructor
	 */
	private TourCompareManager() {}

	public static TourCompareManager getInstance() {
		if (fInstance == null) {
			fInstance = new TourCompareManager();
		}
		return fInstance;
	}

	/**
	 * Find the compared tours in the tour map tree viewer<br>
	 * !!! Recursive !!!
	 * 
	 * @param comparedTours
	 * @param parentItem
	 * @param findCompIds
	 *        comp id's which should be found
	 */
	static void getComparedTours(	ArrayList<TVTITourMapComparedTour> comparedTours,
									final TreeViewerItem parentItem,
									final ArrayList<Long> findCompIds) {

		final ArrayList<TreeViewerItem> unfetchedChildren = parentItem.getUnfetchedChildren();

		if (unfetchedChildren != null) {

			// children are available

			for (final TreeViewerItem tourTreeItem : unfetchedChildren) {

				if (tourTreeItem instanceof TVTITourMapComparedTour) {

					final TVTITourMapComparedTour ttiCompResult = (TVTITourMapComparedTour) tourTreeItem;
					final long ttiCompId = ttiCompResult.getCompId();

					for (final Long compId : findCompIds) {
						if (ttiCompId == compId) {
							comparedTours.add(ttiCompResult);
						}
					}

				} else {
					// this is a child which can be the parent for other childs
					getComparedTours(comparedTours, tourTreeItem, findCompIds);
				}
			}
		}
	}

	/**
	 * @return Returns true when currently tours are compared
	 */
	public boolean isComparing() {
		return isComparing;
	}

	/**
	 * Compares all reference tours with all compare tours
	 * 
	 * @param refTourContext
	 * @param compareTours
	 */
	public void compareTours(final TourReference[] refTourContext, final Object[] compareTours) {

		this.refTourContext = refTourContext;
		refToursData = new TourData[refTourContext.length];

		Job compareJob = new Job(Messages.TourMap_Compare_job_title) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				final int tours2Compare = compareTours.length * refTourContext.length;

				monitor.beginTask(Messages.TourMap_Compare_job_task, tours2Compare);

				compareTourJob(refTourContext, compareTours, monitor);

				monitor.done();

				showCompareResults();

				return Status.OK_STATUS;
			}

			private void compareTourJob(final TourReference[] refTourContext,
										final Object[] compareTours,
										IProgressMonitor monitor) {

				int tourCounter = 0;
				comparedTours = new ArrayList<TVICompareResult>();

				// get all reference tours
				getRefToursData();

				// loop: all compare tours
				for (int compareIndex = 0; compareIndex < compareTours.length; compareIndex++) {

					Long tourId;

					Object tour = compareTours[compareIndex];

					if (tour instanceof TourMapTourItem) {
						TourMapTourItem tourItem = (TourMapTourItem) tour;

						// ignore none tour items
						if (tourItem.getItemType() == TourMapTourItem.ITEM_TYPE_TOUR) {

							// load compare tour from database
							tourId = tourItem.getTourId();
						} else {
							continue;
						}

					} else if (tour instanceof Long) {
						tourId = (Long) tour;

					} else {
						continue;
					}

					// load compare tour from the database
					TourData compareTourData = TourManager.getInstance().getTourData(tourId);

					if (compareTourData.timeSerie.length > 0) {

						// loop: all reference tours
						for (int refTourIndex = 0; refTourIndex < refTourContext.length; refTourIndex++) {

							if (monitor.isCanceled()) {
								showCompareResults();
								return;
							}

							// compare the tour
							TVICompareResult compareResult = compareTour(refTourIndex,
									compareTourData);

							// ignore tours which could not be compared
							if (compareResult.compareIndexStart != -1) {

								compareResult.refTour = refTourContext[refTourIndex];
								compareResult.compTour = compareTourData;

								comparedTours.add(compareResult);
							}

							// update the message in the progress monitor
							tourCounter++;
							monitor.subTask(NLS.bind(Messages.TourMap_Compare_job_subtask,
									Integer.toString(tourCounter)));

							monitor.worked(1);
						}
					}

				}
			}
		};

		compareJob.setUser(true);
		compareJob.schedule();
	}

	private void showCompareResults() {

		Display.getDefault().asyncExec(new Runnable() {

			public void run() {

				final IWorkbench workbench = PlatformUI.getWorkbench();
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

				if (window != null) {
					try {

						// show compare result perspective
						workbench.showPerspective(PerspectiveFactoryCompareTours.PERSPECTIVE_ID,
								window);

						tourComparerView = (CompareResultView) window.getActivePage()
								.showView(CompareResultView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

						tourComparerView.updateViewer();

					} catch (PartInitException e) {
						ErrorDialog.openError(window.getShell(), "Error", e.getMessage(), e //$NON-NLS-1$
						.getStatus());
						e.printStackTrace();
					} catch (WorkbenchException e) {
						e.printStackTrace();
					}
				}

			}
		});
	}

	/**
	 * Get the tour data for all reference tours
	 * 
	 * @param refTourContext
	 */
	private void getRefToursData() {

		for (int tourIndex = 0; tourIndex < refTourContext.length; tourIndex++) {

			TourReference refTour = refTourContext[tourIndex];

			refTourContext[tourIndex] = refTour;

			// load tour from database
			refToursData[tourIndex] = refTour.getTourData();
		}
	}

	/**
	 * @param refTourIndex
	 *        index into refTourContext and refToursData
	 * @param compareTourData
	 *        tour data of the tour which will be compared
	 * @return returns the start index for the ref tour in the compare tour
	 */
	private TVICompareResult compareTour(int refTourIndex, TourData compareTourData) {

		final TVICompareResult compareResult = new TVICompareResult();

		/*
		 * normalize the compare tour
		 */
		TourDataNormalizer compareTourNormalizer = new TourDataNormalizer();
		final int[] compareTourDataDistance = compareTourData.distanceSerie;
		final int[] compareTourDataTime = compareTourData.timeSerie;

		// normalize the tour which will be compared
		compareTourNormalizer.normalizeAltitude(compareTourData,
				0,
				compareTourDataDistance.length - 1);
		int[] normCompDistances = compareTourNormalizer.getNormalizedDistance();
		int[] normCompAltitudes = compareTourNormalizer.getNormalizedAltitude();
		int[] normCompAltiDiff = new int[normCompAltitudes.length];

		/*
		 * reference tour
		 */
		TourReference refTour = refTourContext[refTourIndex];
		int refMeasureStartIndex = refTour.getStartValueIndex();
		int refMeasureEndIndex = refTour.getEndValueIndex();

		// get the reference tour
		TourData refTourData = refToursData[refTourIndex];
		if (refTourData == null) {
			return compareResult;
		}

		// normalize the reference tour
		TourDataNormalizer refTourNormalizer = new TourDataNormalizer();
		refTourNormalizer.normalizeAltitude(refTourData, refMeasureStartIndex, refMeasureEndIndex);
		int[] normRefAltitudes = refTourNormalizer.getNormalizedAltitude();

		int minAltiDiff = Integer.MAX_VALUE;

		// start index of the reference tour in the compare tour
		int normCompareIndexStart = -1;

		final int compareLastIndex = normCompAltitudes.length;

		for (int normCompareIndex = 0; normCompareIndex < normCompAltitudes.length; normCompareIndex++) {

			int altitudeDiff = -1;

//			int startAltDiff = Math.abs(normCompAltitudes[normCompareIndex] - normRefAltitudes[0]);

			// loop: all data in the reference tour
			for (int normRefIndex = 0; normRefIndex < normRefAltitudes.length; normRefIndex++) {

				int compareRefIndex = normCompareIndex + normRefIndex;

				// make sure the ref index is not bigger than the compare index,
				// this can happen when the reference data exeed the compare
				// data
				if (compareRefIndex == compareLastIndex) {
					altitudeDiff = -1;
					break;
				}

				// get the altitude difference between the reference and the
				// compared value
				altitudeDiff += Math.abs(normRefAltitudes[normRefIndex]
						- normCompAltitudes[compareRefIndex]);
				// - startAltDiff);
			}

			// save the altitude difference
			normCompAltiDiff[normCompareIndex] = altitudeDiff;

			// find the lowest altitude, this will be the start point for the
			// reference tour
			if (altitudeDiff < minAltiDiff && altitudeDiff != -1) {
				minAltiDiff = altitudeDiff;
				normCompareIndexStart = normCompareIndex;
			}
		}

		// exit if tour was not found
		if (normCompareIndexStart == -1) {
			return compareResult;
		}

		// distance for the reference tour
		int refDistance = refTourData.distanceSerie[refMeasureEndIndex]
				- refTourData.distanceSerie[refMeasureStartIndex];

		// get the start/end point in the compared tour
		int compDistanceStart = normCompDistances[normCompareIndexStart];
		int compDistanceEnd = compDistanceStart + refDistance;

		// get the start point in the compare tour
		int compareIndexStart = 0;
		for (; compareIndexStart < compareTourDataDistance.length; compareIndexStart++) {
			if (compareTourDataDistance[compareIndexStart] >= compDistanceStart) {
				break;
			}
		}

		// get the end point in the compare tour
		int compareIndexEnd = compareIndexStart;
		int oldDistance = compareTourDataDistance[compareIndexEnd];
		for (; compareIndexEnd < compareTourDataDistance.length; compareIndexEnd++) {
			if (compareTourDataDistance[compareIndexEnd] >= compDistanceEnd) {
				break;
			}

			int newDistance = compareTourDataDistance[compareIndexEnd];

			if (oldDistance == newDistance) {} else {
				oldDistance = newDistance;
			}
		}
		compareIndexEnd = Math.min(compareIndexEnd, compareTourDataDistance.length - 1);

		int distance = compareTourDataDistance[compareIndexEnd]
				- compareTourDataDistance[compareIndexStart];

		int time = compareTourDataTime[compareIndexEnd] - compareTourDataTime[compareIndexStart];

		// remove the breaks from the time
		int timeInterval = compareTourDataTime[1] - compareTourDataTime[0];
		int ignoreTimeSlices = TourManager.getIgnoreTimeSlices(compareTourDataTime,
				compareIndexStart,
				compareIndexEnd,
				10 / timeInterval);
		time = time - (ignoreTimeSlices * timeInterval);

		// // overwrite the changed data series
		// compareTourData.distanceSerie =
		// compareTourNormalizer.getNormalizedDistance();
		// compareTourData.altitudeSerie =
		// compareTourNormalizer.getNormalizedAltitude();
		// compareTourData.pulseSerie = normCompAltiDiff;
		//
		// // set the same array size for each data serie
		// compareTourData.timeSerie =
		// compareTourNormalizer.getNormalizedTime();
		// compareTourData.speedSerie =
		// compareTourNormalizer.getNormalizedTime();
		// compareTourData.cadenceSerie =
		// compareTourNormalizer.getNormalizedTime();
		// compareTourData.temperatureSerie =
		// compareTourNormalizer.getNormalizedTime();

		// create the compare result
		compareResult.altitudeDiff = minAltiDiff;

		compareResult.compareIndexStart = compareIndexStart;
		compareResult.compareIndexEnd = compareIndexEnd;

		final int normIndexDiff = refDistance / TourDataNormalizer.NORMALIZED_DISTANCE;
		compareResult.normIndexStart = normCompareIndexStart;
		compareResult.normIndexEnd = normCompareIndexStart + normIndexDiff;

		compareResult.compareTime = time;
		compareResult.compareDistance = distance;
		compareResult.timeIntervall = compareTourData.getDeviceTimeInterval();

		return compareResult;
	}

	/**
	 * @return Returns the reference tours which has been compared
	 */
	public TourReference[] getComparedReferenceTours() {
		return refTourContext;
	}

	/**
	 * @return Returns the comparedTours.
	 */
	public TVICompareResult[] getComparedTours() {
		return comparedTours.toArray(new TVICompareResult[comparedTours.size()]);
	}

	/**
	 * Remove compared tour from the database
	 * 
	 * @param compTourItem
	 */
	static boolean removeComparedTourFromDb(Long compId) {

		boolean returnResult = false;

		EntityManager em = TourDatabase.getInstance().getEntityManager();
		EntityTransaction ts = em.getTransaction();

		try {
			TourCompared compTour = em.find(TourCompared.class, compId);

			if (compTour != null) {
				ts.begin();
				em.remove(compTour);
				ts.commit();
			}

		} catch (Exception e) {
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
}
