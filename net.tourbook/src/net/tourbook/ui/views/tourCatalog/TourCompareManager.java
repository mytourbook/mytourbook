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
package net.tourbook.ui.views.tourCatalog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import net.tourbook.Messages;
import net.tourbook.application.PerspectiveFactoryCompareTours;
import net.tourbook.data.TourCompared;
import net.tourbook.data.TourData;
import net.tourbook.data.TourReference;
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

	private static TourCompareManager				fInstance;

	private boolean									isComparing			= false;

	private TourReference[]							refTourContext;
	private TourData[]								refToursData;

	private ArrayList<TVICompareResultComparedTour>	fComparedTourItems	= new ArrayList<TVICompareResultComparedTour>();

	/**
	 * @param refId
	 *            Reference Id
	 * @return Returns all compared tours for the reference tour with the <code>refId</code> which
	 *         are saved in the database
	 */
	public static HashMap<Long, StoredComparedTour> getComparedToursFromDb(final long refId) {

		final HashMap<Long, StoredComparedTour> storedComparedTours = new HashMap<Long, StoredComparedTour>();

		final StringBuilder sb = new StringBuilder();

		sb.append("SELECT"); //$NON-NLS-1$

		sb.append(" tourId,"); //		1 //$NON-NLS-1$
		sb.append(" comparedId,"); //	2 //$NON-NLS-1$
		sb.append(" startIndex,"); //	3 //$NON-NLS-1$
		sb.append(" endIndex,"); //		4 //$NON-NLS-1$
		sb.append(" tourSpeed"); //		5	 //$NON-NLS-1$

		sb.append(" FROM " + TourDatabase.TABLE_TOUR_COMPARED); //$NON-NLS-1$
		sb.append(" WHERE refTourId=?"); //$NON-NLS-1$

		try {

			final Connection conn = TourDatabase.getInstance().getConnection();
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

			conn.close();

		} catch (final SQLException e) {
			UI.showSQLException(e);
		}

		return storedComparedTours;
	}

	public static TourCompareManager getInstance() {
		if (fInstance == null) {
			fInstance = new TourCompareManager();
		}
		return fInstance;
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

		final TourCompared comparedTour = new TourCompared();

		comparedTour.setStartIndex(comparedTourItem.computedStartIndex);
		comparedTour.setEndIndex(comparedTourItem.computedEndIndex);
		comparedTour.setTourId(tourData.getTourId());
		comparedTour.setRefTourId(comparedTourItem.refTour.getRefId());

		final Calendar calendar = GregorianCalendar.getInstance();
		calendar.set(tourData.getStartYear(), tourData.getStartMonth() - 1, tourData.getStartDay());

		comparedTour.setTourDate(calendar.getTimeInMillis());
		comparedTour.setStartYear(tourData.getStartYear());

		final float speed = TourManager.computeTourSpeed(tourData,
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
	 * internal constructor
	 */
	private TourCompareManager() {}

	/**
	 * @param refTourIndex
	 *            index into refTourContext and refToursData
	 * @param compareTourData
	 *            tour data of the tour which will be compared
	 * @return returns the start index for the ref tour in the compare tour
	 */
	private TVICompareResultComparedTour compareTour(final int refTourIndex, final TourData compareTourData) {

		final TVICompareResultComparedTour compareResultItem = new TVICompareResultComparedTour();

		/*
		 * normalize the compare tour
		 */
		final TourDataNormalizer compareTourNormalizer = new TourDataNormalizer();
		final int[] compareTourDataDistance = compareTourData.getMetricDistanceSerie();
		final int[] compareTourDataTime = compareTourData.timeSerie;

		if (compareTourDataDistance == null) {
			return compareResultItem;
		}
		// normalize the tour which will be compared
		compareTourNormalizer.normalizeAltitude(compareTourData, 0, compareTourDataDistance.length - 1);

		final int[] normCompDistances = compareTourNormalizer.getNormalizedDistance();
		final int[] normCompAltitudes = compareTourNormalizer.getNormalizedAltitude();

		if (normCompAltitudes == null || normCompDistances == null) {
			return compareResultItem;
		}

		final int[] normCompAltiDiff = new int[normCompAltitudes.length];

		/*
		 * reference tour
		 */
		final TourReference refTour = refTourContext[refTourIndex];
		final int refMeasureStartIndex = refTour.getStartValueIndex();
		final int refMeasureEndIndex = refTour.getEndValueIndex();

		// get the reference tour
		final TourData refTourData = refToursData[refTourIndex];
		if (refTourData == null) {
			return compareResultItem;
		}

		// normalize the reference tour
		final TourDataNormalizer refTourNormalizer = new TourDataNormalizer();
		refTourNormalizer.normalizeAltitude(refTourData, refMeasureStartIndex, refMeasureEndIndex);

		final int[] normRefAltitudes = refTourNormalizer.getNormalizedAltitude();
		if (normRefAltitudes == null) {
			return compareResultItem;
		}

		int minAltiDiff = Integer.MAX_VALUE;

		// start index of the reference tour in the compare tour
		int normCompareIndexStart = -1;

		final int compareLastIndex = normCompAltitudes.length;

		for (int normCompareIndex = 0; normCompareIndex < normCompAltitudes.length; normCompareIndex++) {

			int altitudeDiff = -1;

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
		final int[] distanceSerie = refTourData.getMetricDistanceSerie();
		final int refDistance = distanceSerie[refMeasureEndIndex] - distanceSerie[refMeasureStartIndex];

		// get the start/end point in the compared tour
		final int compDistanceStart = normCompDistances[normCompareIndexStart];
		final int compDistanceEnd = compDistanceStart + refDistance;

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

			final int newDistance = compareTourDataDistance[compareEndIndex];

			if (oldDistance == newDistance) {} else {
				oldDistance = newDistance;
			}
		}
		compareEndIndex = Math.min(compareEndIndex, compareTourDataDistance.length - 1);

		/*
		 * create data serie for altitude difference
		 */
		final int[] normDistanceSerie = compareTourNormalizer.getNormalizedDistance();
		final int[] compAltiDif = new int[compareTourDataDistance.length];

		final int maxNormIndex = normDistanceSerie.length - 1;
		int normIndex = 0;

		for (int compIndex = 0; compIndex < compareTourDataDistance.length; compIndex++) {

			final int compDistance = compareTourDataDistance[compIndex];
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

		final int compareDistance = compareTourDataDistance[compareEndIndex]
				- compareTourDataDistance[compareStartIndex];
		final int recordingTime = compareTourDataTime[compareEndIndex] - compareTourDataTime[compareStartIndex];
		final int drivingTime = Math.max(0, recordingTime
				- compareTourData.getBreakTime(compareStartIndex, compareEndIndex));

		compareResultItem.compareDrivingTime = drivingTime;
		compareResultItem.compareRecordingTime = recordingTime;
		compareResultItem.compareDistance = compareDistance;
		compareResultItem.compareSpeed = ((float) compareDistance) / drivingTime * 3.6f;

		compareResultItem.timeIntervall = compareTourData.getDeviceTimeInterval();

		return compareResultItem;
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

		final Job compareJob = new Job(Messages.tourCatalog_view_compare_job_title) {

			private void compareTourJob(final TourReference[] refTourContext,
										final Object[] comparedTours,
										final IProgressMonitor monitor) {

				int tourCounter = 0;
				fComparedTourItems.clear();

				// get all reference tours
				getRefToursData();

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

					if (compareTourData != null && compareTourData.timeSerie.length > 0) {

						// loop: all reference tours
						for (int refTourIndex = 0; refTourIndex < refTourContext.length; refTourIndex++) {

							if (monitor.isCanceled()) {
								showCompareResults();
								return;
							}

							// compare the tour
							final TVICompareResultComparedTour compareResult = compareTour(refTourIndex,
									compareTourData);

							// ignore tours which could not be compared
							if (compareResult.computedStartIndex != -1) {

								compareResult.refTour = refTourContext[refTourIndex];
								compareResult.comparedTourData = compareTourData;

								fComparedTourItems.add(compareResult);
							}

							// update the message in the progress monitor
							monitor.subTask(NLS.bind(Messages.tourCatalog_view_compare_job_subtask,
									Integer.toString(++tourCounter)));

							monitor.worked(1);
						}
					}

				}
			}

			@Override
			protected IStatus run(final IProgressMonitor monitor) {

				final int tours2Compare = comparedTours.length * refTourContext.length;

				monitor.beginTask(Messages.tourCatalog_view_compare_job_task, tours2Compare);

				compareTourJob(refTourContext, comparedTours, monitor);

				monitor.done();

				showCompareResults();

				return Status.OK_STATUS;
			}
		};

		compareJob.setUser(true);
		compareJob.schedule();
	}

	/**
	 * @return Returns the reference tours which has been compared
	 */
	public TourReference[] getComparedReferenceTours() {
		return refTourContext;
	}

	/**
	 * @return Returns the compare result with all compared tours
	 */
	public TVICompareResultComparedTour[] getComparedTours() {
		return fComparedTourItems.toArray(new TVICompareResultComparedTour[fComparedTourItems.size()]);
	}

	/**
	 * Get the tour data for all reference tours
	 * 
	 * @param refTourContext
	 */
	private void getRefToursData() {

		for (int tourIndex = 0; tourIndex < refTourContext.length; tourIndex++) {

			final TourReference refTour = refTourContext[tourIndex];

			refTourContext[tourIndex] = refTour;

			// load tour from database
			refToursData[tourIndex] = refTour.getTourData();
		}
	}

	/**
	 * @return Returns true when currently tours are compared
	 */
	public boolean isComparing() {
		return isComparing;
	}

	private void showCompareResults() {

		Display.getDefault().asyncExec(new Runnable() {

			public void run() {

				final IWorkbench workbench = PlatformUI.getWorkbench();
				final IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

				if (window != null) {
					try {

						// show compare result perspective
						workbench.showPerspective(PerspectiveFactoryCompareTours.PERSPECTIVE_ID, window);

						final TourCompareResultView view = (TourCompareResultView) window.getActivePage()
								.showView(TourCompareResultView.ID, null, IWorkbenchPage.VIEW_ACTIVATE);

						if (view != null) {
							view.reloadViewer();
						}

					} catch (final PartInitException e) {
						ErrorDialog.openError(window.getShell(), "Error", e.getMessage(), e //$NON-NLS-1$
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
