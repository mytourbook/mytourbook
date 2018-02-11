/*******************************************************************************
 * Copyright (C) 2005, 2018 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.views.tourCatalog.geo;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

public class GeoPartTourComparer {

	private static final int COMPARATOR_THREADS = Runtime.getRuntime().availableProcessors();
//	private static final int COMPARATOR_THREADS = 1;

	static {}

	private static final LinkedBlockingDeque<GeoPartComparerItem>	_waitingQueue	= new LinkedBlockingDeque<>();
	private static ThreadPoolExecutor								_comparerExecutor;

	static GeoPartView												geoPartView;

	static {

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
						"[%s] Comparing tours with %d threads",
						GeoPartTourComparer.class.getSimpleName(),
						COMPARATOR_THREADS)));
// TODO remove SYSTEM.OUT.PRINTLN

		_comparerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(COMPARATOR_THREADS, threadFactory);
	}

	/**
	 * @param loaderItem
	 */
	static void compareGeoTours(final GeoPartLoaderItem loaderItem) {

		_waitingQueue.clear();

		for (final long tourId : loaderItem.tourIds) {

			_waitingQueue.add(new GeoPartComparerItem(tourId, loaderItem));

			_comparerExecutor.submit(new Runnable() {
				@Override
				public void run() {

					// get last added loader item
					final GeoPartComparerItem comparatorItem = _waitingQueue.pollFirst();

					if (comparatorItem == null) {
						return;
					}

					try {

						compareTour(comparatorItem);

					} catch (final Exception e) {

						StatusUtil.log(e);
					}

					geoPartView.compare_40_TourIsCompared(comparatorItem);
				}
			});
		}
	}

	private static boolean compareTour(final GeoPartComparerItem comparatorItem) {

		final GeoPartLoaderItem loaderItem = comparatorItem.loaderItem;

		if (loaderItem.isCanceled) {
			return false;
		}

		/*
		 * Load tour data
		 */
		final long startLoading = System.nanoTime();

		final TourData tourData = TourManager.getInstance().getTourData(comparatorItem.tourId);

		/*
		 * Normalize data
		 */
		final long startConvert = System.nanoTime();

		final NormalizedGeoData normalizedPart = loaderItem.normalizedTourPart;
		final int[] partLatSerie = normalizedPart.normalizedLat;
		final int[] partLonSerie = normalizedPart.normalizedLon;

		final NormalizedGeoData normalizedTour = tourData.getNormalizedLatLon();
		final int[] tourLatSerie = normalizedTour.normalizedLat;
		final int[] tourLonSerie = normalizedTour.normalizedLon;

		final int numPartSlices = partLatSerie.length;
		final int numTourSlices = tourLatSerie.length;

		final long[] tourDiff = new long[numTourSlices];

		/*
		 * Compare
		 */
		final long startComparing = System.nanoTime();

		long minDiffValue = Long.MAX_VALUE;
		int minDiffIndex = -1;

		// loop: all tour slices
		for (int tourIndex = 0; tourIndex < numTourSlices; tourIndex++) {

			long latLonDiff = -1;

			// loop: all part slices
			for (int partIndex = 0; partIndex < numPartSlices; partIndex++) {

				if (loaderItem.isCanceled) {
					return false;
				}

				final int compareIndex = tourIndex + partIndex;

				/*
				 * Make sure the compare index is not larger than the tour index, this happens when
				 * the part slices has exeeded the tour slices
				 */
				if (compareIndex == numTourSlices) {
					latLonDiff = -1;
					break;
				}

				final int latDiff = partLatSerie[partIndex] - tourLatSerie[compareIndex];
				final int lonDiff = partLonSerie[partIndex] - tourLonSerie[compareIndex];

				// optimize Math.abs() !!!
				final int latDiffAbs = latDiff < 0 ? -latDiff : latDiff;
				final int lonDiffAbs = lonDiff >= 0 ? lonDiff : -lonDiff;

				// summarize all diffs for one tour slice
				latLonDiff += (latDiffAbs + lonDiffAbs);
			}

			// keep diff value
			tourDiff[tourIndex] = latLonDiff;

			// keep min diff value/index
			if (latLonDiff < minDiffValue && latLonDiff != -1) {

				minDiffValue = latLonDiff;

				// keep tour index where the min diff occured
				minDiffIndex = tourIndex;
			}

		}

		// a tour is available
		if (minDiffIndex != -1) {

			final int[] normalizedIndices = normalizedTour.normalized2OriginalIndices;

			final int startIndex = normalizedIndices[minDiffIndex];
			final int endIndex = normalizedIndices[minDiffIndex + numPartSlices - 1];

			comparatorItem.avgPulse = tourData.computeAvg_PulseSegment(startIndex, endIndex);
			comparatorItem.speed = TourManager.computeTourSpeed(tourData, startIndex, endIndex);

		}

		comparatorItem.tourLatLonDiff = tourDiff;
		comparatorItem.tourMinDiffIndex = minDiffIndex;

//		System.out.println(
//				String.format(
//						""
//								+ "[%5d]" // thread
//								+ " tourId %-20s"
//								+ "   exec %5d"
//
//								+ "   diff %8d"
//								+ "   # %5d / %5d"
//
//								+ "   cmp %10.4f"
//								+ "   ld %10.4f"
//								+ "   cnvrt %10.4f"
//								+ "   all %10.4f ms",
//
//						Thread.currentThread().getId(),
//						comparatorItem.tourId,
//						loaderItem.executorId,
//
//						minDiffIndex < 0 ? minDiffIndex : tourDiff[minDiffIndex],
//						numTourSlices,
//						numPartSlices,
//
//						(float) (System.nanoTime() - startComparing) / 1000000, // compare
//						(float) (startConvert - startLoading) / 1000000, // load
//						(float) (startComparing - startConvert) / 1000000, // convert
//						(float) (System.nanoTime() - startLoading) / 1000000) // all
//		);
//		// TODO remove SYSTEM.OUT.PRINTLN

		return true;
	}

	/**
	 * Stop comparing of the tours in the waiting queue.
	 */
	static void stopComparing() {

		// invalidate old requests
		synchronized (_waitingQueue) {

			for (final GeoPartComparerItem comparerItem : _waitingQueue) {

				comparerItem.loaderItem.isCanceled = true;
			}

			_waitingQueue.clear();
		}
	}
}
