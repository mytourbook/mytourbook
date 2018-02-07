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

import net.tourbook.data.NormalizedGeoData;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

public class GeoPartTourComparer {

	private static final int										COMPARATOR_THREADS	= 8;

	private static final LinkedBlockingDeque<GeoPartComparerItem>	_waitingQueue		= new LinkedBlockingDeque<>();

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

		_comparerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(COMPARATOR_THREADS, threadFactory);
	}

	/**
	 * @param loaderItem
	 */
	static void compareGeoTours(final GeoPartLoaderItem loaderItem) {

		synchronized (_waitingQueue) {
			_waitingQueue.removeAll(_waitingQueue);
		}

		for (final long tourId : loaderItem.tourIds) {

			_waitingQueue.add(new GeoPartComparerItem(tourId, loaderItem));

			final Runnable executorTask = new Runnable() {
				@Override
				public void run() {

					// get last added loader item
					final GeoPartComparerItem comparatorItem = _waitingQueue.pollFirst();

					if (comparatorItem == null) {
						return;
					}

					final long executorId = comparatorItem.loaderItem.executorId;

					if (isComparatorValid(executorId)) {

						compareTour(comparatorItem);

						if (isComparatorValid(executorId)) {
							geoPartView.compare_40_TourIsCompared(comparatorItem);
						}
					}
				}
			};

			_comparerExecutor.submit(executorTask);
		}
	}

	private static void compareTour(final GeoPartComparerItem comparatorItem) {

		final long startLoading = System.nanoTime();

		final TourData tourData = TourManager.getInstance().getTourData(comparatorItem.tourId);

		final long startConvert = System.nanoTime();

		final GeoPartLoaderItem loaderItem = comparatorItem.loaderItem;
		final int[] partLatSerie = loaderItem.normalizedTourPart.normalizedLat;
		final int[] partLonSerie = loaderItem.normalizedTourPart.normalizedLon;

		final NormalizedGeoData normalizedLatLon = tourData.getNormalizedLatLon();
		final int[] tourLatSerie = normalizedLatLon.normalizedLat;
		final int[] tourLonSerie = normalizedLatLon.normalizedLon;

		final long startComparing = System.nanoTime();

		for (int partIndex = 0; partIndex < partLatSerie.length; partIndex++) {

			final int partLat = partLatSerie[partIndex];
			final int partLon = partLonSerie[partIndex];

			for (int tourIndex = 0; tourIndex < tourLatSerie.length; tourIndex++) {

				final int tourLat = tourLatSerie[tourIndex];
				final int tourLon = tourLonSerie[tourIndex];

			}
		}

		System.out.println(
				String.format(
						"tourId %-20s   id %5d   # %5d / %5d   load %10.4f   convert %10.4f   compare %10.4f   all %10.4f ms",
						comparatorItem.tourId,
						loaderItem.executorId,
						tourLatSerie.length,
						partLatSerie.length,
						(float) (startConvert - startLoading) / 1000000, // load
						(float) (startComparing - startConvert) / 1000000, // convert
						(float) (System.nanoTime() - startComparing) / 1000000, // compare
						(float) (System.nanoTime() - startLoading) / 1000000) // all
		);
		// TODO remove SYSTEM.OUT.PRINTLN
	}

	private static boolean isComparatorValid(final long executorId) {

		return GeoPartTourLoader.isLoaderValid(executorId);
	}

}
