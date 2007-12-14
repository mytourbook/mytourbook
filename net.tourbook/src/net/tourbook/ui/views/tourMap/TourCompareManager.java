/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
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
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
import net.tourbook.database.TourDatabase;
import net.tourbook.tour.TourManager;

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

	private static TourCompareManager					fInstance;

	private boolean										isComparing			= false;

	private TourReference[]								refTourContext;
	private TourData[]									refToursData;

	private ArrayList<CompareResultItemComparedTour>	fComparedTourItems	= new ArrayList<CompareResultItemComparedTour>();

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
	 * Saves the {@link CompareResultItemComparedTour} item and updates the item with the saved data
	 * 
	 * @param comparedTourItem
	 * @param em
	 * @param ts
	 */
	public static void saveComparedTourItem(CompareResultItemComparedTour comparedTourItem,
											EntityManager em,
											EntityTransaction ts) {

		TourData tourData = comparedTourItem.comparedTourData;

		TourCompared comparedTour = new TourCompared();

		comparedTour.setStartIndex(comparedTourItem.computedStartIndex);
		comparedTour.setEndIndex(comparedTourItem.computedEndIndex);
		comparedTour.setTourId(tourData.getTourId());
		comparedTour.setRefTourId(comparedTourItem.refTour.getRefId());

		Calendar calendar = GregorianCalendar.getInstance();
		calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());

		comparedTour.setTourDate(calendar.getTimeInMillis());
		comparedTour.setStartYear(tourData.getStartYear());

//		float speed = TourManager.computeTourSpeed(tourData.getMetricDistanceSerie(),
//				tourData.timeSerie,
//				comparedTourItem.computedStartIndex,
//				comparedTourItem.computedEndIndex);
		float speed = TourManager.computeTourSpeed(tourData,
				comparedTourItem.computedStartIndex,
				comparedTourItem.computedEndIndex);

		comparedTour.setTourSpeed(speed);

		// persist entity
		ts.begin();
		em.persist(comparedTour);
		ts.commit();

		// updata saved data
		comparedTourItem.compId = comparedTour.getComparedId();
		comparedTourItem.dbStartIndex = comparedTourItem.computedStartIndex;
		comparedTourItem.dbEndIndex = comparedTourItem.computedEndIndex;
		comparedTourItem.dbSpeed = speed;
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
	 * @param comparedTours
	 */
	public void compareTours(final TourReference[] refTourContext, final Object[] comparedTours) {

		this.refTourContext = refTourContext;
		refToursData = new TourData[refTourContext.length];

		Job compareJob = new Job(Messages.Tour_Map_Compare_job_title) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {

				final int tours2Compare = comparedTours.length * refTourContext.length;

				monitor.beginTask(Messages.Tour_Map_Compare_job_task, tours2Compare);

				compareTourJob(refTourContext, comparedTours, monitor);

				monitor.done();

				showCompareResults();

				return Status.OK_STATUS;
			}

			private void compareTourJob(final TourReference[] refTourContext,
										final Object[] compareTourIDs,
										IProgressMonitor monitor) {

				int tourCounter = 0;
				fComparedTourItems = new ArrayList<CompareResultItemComparedTour>();

				// get all reference tours
				getRefToursData();

				// loop: all compare tours
				for (int compareIndex = 0; compareIndex < compareTourIDs.length; compareIndex++) {

					Long tourId;

					Object tour = compareTourIDs[compareIndex];

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
							CompareResultItemComparedTour compareResult = compareTour(refTourIndex, compareTourData);

							// ignore tours which could not be compared
							if (compareResult.computedStartIndex != -1) {

								compareResult.refTour = refTourContext[refTourIndex];
								compareResult.comparedTourData = compareTourData;

								fComparedTourItems.add(compareResult);
							}

							// update the message in the progress monitor
							tourCounter++;
							monitor.subTask(NLS.bind(Messages.Tour_Map_Compare_job_subtask,
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
						workbench.showPerspective(PerspectiveFactoryCompareTours.PERSPECTIVE_ID, window);

						CompareResultView view = (CompareResultView) window.getActivePage()
								.showView(CompareResultView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

						if (view != null) {
							view.updateViewer();
						}

					}
					catch (PartInitException e) {
						ErrorDialog.openError(window.getShell(), "Error", e.getMessage(), e //$NON-NLS-1$
						.getStatus());
						e.printStackTrace();
					}
					catch (WorkbenchException e) {
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
	private CompareResultItemComparedTour compareTour(int refTourIndex, TourData compareTourData) {

		final CompareResultItemComparedTour compareResultItem = new CompareResultItemComparedTour();

		/*
		 * normalize the compare tour
		 */
		TourDataNormalizer compareTourNormalizer = new TourDataNormalizer();
		final int[] compareTourDataDistance = compareTourData.getMetricDistanceSerie();
		final int[] compareTourDataTime = compareTourData.timeSerie;

		// normalize the tour which will be compared
		compareTourNormalizer.normalizeAltitude(compareTourData, 0, compareTourDataDistance.length - 1);

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
			return compareResultItem;
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

			// loop: all data in the reference tour
			for (int normRefIndex = 0; normRefIndex < normRefAltitudes.length; normRefIndex++) {

				int compareRefIndex = normCompareIndex + normRefIndex;

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
		final int[] distanceSerie = refTourData.getMetricDistanceSerie();
		int refDistance = distanceSerie[refMeasureEndIndex] - distanceSerie[refMeasureStartIndex];

		// get the start/end point in the compared tour
		int compDistanceStart = normCompDistances[normCompareIndexStart];
		int compDistanceEnd = compDistanceStart + refDistance;

		/*
		 * get the start point in the compare tour
		 */
		int compareStartIndex = 0;
		for (; compareStartIndex < compareTourDataDistance.length; compareStartIndex++) {
			if (compareTourDataDistance[compareStartIndex] >= compDistanceStart) {
				break;
			}
		}

		/*
		 * get the end point in the compare tour
		 */
		int compareEndIndex = compareStartIndex;
		int oldDistance = compareTourDataDistance[compareEndIndex];
		for (; compareEndIndex < compareTourDataDistance.length; compareEndIndex++) {
			if (compareTourDataDistance[compareEndIndex] >= compDistanceEnd) {
				break;
			}

			int newDistance = compareTourDataDistance[compareEndIndex];

			if (oldDistance == newDistance) {} else {
				oldDistance = newDistance;
			}
		}
		compareEndIndex = Math.min(compareEndIndex, compareTourDataDistance.length - 1);

		/*
		 * create data serie for altitude difference
		 */
		int[] normDistanceSerie = compareTourNormalizer.getNormalizedDistance();
		int[] compAltiDif = new int[compareTourDataDistance.length];

		final int maxNormIndex = normDistanceSerie.length - 1;
		int normIndex = 0;

		for (int compIndex = 0; compIndex < compareTourDataDistance.length; compIndex++) {

			int compDistance = compareTourDataDistance[compIndex];
			int normDistance = normDistanceSerie[normIndex];

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

		final int normIndexDiff = refDistance / TourDataNormalizer.NORMALIZED_DISTANCE;
		compareResultItem.normalizedStartIndex = normCompareIndexStart;
		compareResultItem.normalizedEndIndex = normCompareIndexStart + normIndexDiff;

		int compareDistance = compareTourDataDistance[compareEndIndex] - compareTourDataDistance[compareStartIndex];
		int recordingTime = compareTourDataTime[compareEndIndex] - compareTourDataTime[compareStartIndex];
		int drivingTime = recordingTime - compareTourData.getBreakTime(compareStartIndex, compareEndIndex);
		compareResultItem.compareDrivingTime = drivingTime;
		compareResultItem.compareRecordingTime = recordingTime;
		compareResultItem.compareDistance = compareDistance;
		compareResultItem.compareSpeed = ((float) compareDistance) / drivingTime * 3.6f;

		compareResultItem.timeIntervall = compareTourData.getDeviceTimeInterval();

		return compareResultItem;
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
	public CompareResultItemComparedTour[] getComparedTours() {
		return fComparedTourItems.toArray(new CompareResultItemComparedTour[fComparedTourItems.size()]);
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

		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
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
