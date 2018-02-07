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
package net.tourbook.ui.views.tourCatalog;

import gnu.trove.set.hash.TIntHashSet;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;

import net.tourbook.data.TourData;
import net.tourbook.tour.TourManager;

public class GeoPart_TourComparer {

	private static final AtomicLong									_loaderExecuterId	= new AtomicLong();
	private static final LinkedBlockingDeque<GeoPart_ComparerItem>	_waitingQueue		= new LinkedBlockingDeque<>();

	private static ThreadPoolExecutor								_loadingExecutor;

	static GeoPart_View												geoPartView;

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

		_loadingExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10, threadFactory);
	}

	/**
	 * @param loaderItem
	 */
	static void compareGeoTours(final GeoPart_LoaderItem loaderItem) {

		/*
		 * Invalidate old requests
		 */
		final long executerId = _loaderExecuterId.incrementAndGet();

		synchronized (_waitingQueue) {
			_waitingQueue.removeAll(_waitingQueue);
		}

		for (final long tourId : loaderItem.tourIds) {

			_waitingQueue.add(new GeoPart_ComparerItem(executerId, tourId, loaderItem));

			final Runnable executorTask = new Runnable() {
				@Override
				public void run() {

					// get last added loader item
					final GeoPart_ComparerItem comparatorItem = _waitingQueue.pollFirst();

					if (comparatorItem == null) {
						return;
					}

					if (isComparatorValid(comparatorItem)) {

						if (compareTour(comparatorItem)) {

							if (isComparatorValid(comparatorItem)) {
								geoPartView.updateUI_AfterComparingTours(comparatorItem);
							}
						}
					}
				}
			};

			_loadingExecutor.submit(executorTask);
		}
	}

	private static boolean compareTour(final GeoPart_ComparerItem comparatorItem) {

		final long start = System.currentTimeMillis();

		final TourData tourData = TourManager.getInstance().getTourData(comparatorItem.tourId);

		final GeoPart_LoaderItem loaderItem = comparatorItem.loaderItem;

		final int[] partLatSerie = loaderItem.latPartSerie5;
		final int[] partLonSerie = loaderItem.lonPartSerie5;

		final int[] tourLatSerie = tourData.getLatitudeSerie5();
		final int[] tourLonSerie = tourData.getLongitudeSerie5();

		for (int partIndex = 0; partIndex < partLatSerie.length; partIndex++) {

			final int partLat = partLatSerie[partIndex];
			final int partLon = partLonSerie[partIndex];

			for (int tourIndex = 0; tourIndex < tourLatSerie.length; tourIndex++) {

				final int tourLat = tourLatSerie[tourIndex];
				final int tourLon = tourLonSerie[tourIndex];

			}
		}

		final long timeDiff = System.currentTimeMillis() - start;

//		System.out.println(
//				(UI.timeStampNano() + " [" + GeoPart_TourLoader.class.getSimpleName() + "] ")
//						+ "loadTourGeoPartsFromDB\t" + timeDiff + " ms");
//// TODO remove SYSTEM.OUT.PRINTLN
		return true;
	}

	private static boolean isComparatorValid(final GeoPart_ComparerItem loaderItem) {

		if (loaderItem.executorId < _loaderExecuterId.get()) {

			// current executer was invalidated

//			System.out.println(
//					(UI.timeStampNano() + " [" + GeoPart_TourLoader.class.getSimpleName() + "] ")
//							+ ("\trunning id: " + loaderItem.executorId)
//							+ ("\tcurrent id: " + _loaderExecuterId.get()));

			return false;
		}

		return true;
	}

	/**
	 * Computes geo partitions when geo data are available
	 * 
	 * @param partLatitude
	 * @param partLongitude
	 * @param indexStart
	 * @param indexEnd
	 *            Last index + 1
	 * @return Returns all geo partitions or <code>null</code> when geo data are not available.
	 */
	private TIntHashSet computeGeo_Partitions(	final double[] partLatitude,
												final double[] partLongitude,
												final int indexStart,
												final int indexEnd) {

		if (partLatitude == null || partLongitude == null) {
			return null;
		}

		// ensure the indicies are valid

		int firstIndex = indexStart < indexEnd ? indexStart : indexEnd;
		int lastIndex = indexStart > indexEnd ? indexStart : indexEnd;

		if (firstIndex < 0) {
			firstIndex = 0;
		}

		if (lastIndex > partLatitude.length) {
			lastIndex = partLatitude.length;
		}

		final TIntHashSet allGeoParts = new TIntHashSet();

		for (int serieIndex = firstIndex; serieIndex < lastIndex; serieIndex++) {

//			int latPart = (int) (latitude * 100);
//			int lonPart = (int) (longitude * 100);
//
//			lat		( -90 ... + 90) * 100 =  -9_000 +  9_000 = 18_000
//			lon		(-180 ... +180) * 100 = -18_000 + 18_000 = 36_000
//
//			max		(9_000 + 9_000) * 100_000 = 18_000 * 100_000  = 1_800_000_000
//
//												Integer.MAX_VALUE = 2_147_483_647

			final double latitude = partLatitude[serieIndex];
			final double longitude = partLongitude[serieIndex];

			final int latPart = (int) (latitude * 100);
			final int lonPart = (int) (longitude * 100);

			final int latLonPart = (latPart + 9_000) * 100_000 + (lonPart + 18_000);

			allGeoParts.add(latLonPart);
		}

//		System.out.println();
//		System.out.println();
//		System.out.println();
//
//		for (final int latLonPart : allGeoParts.toArray()) {
//
//			final int lat = (latLonPart / 100_000) - 9_000;
//			final int lon = (latLonPart % 100_000) - 18_000;
//
//			System.out.println(
//					(net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//							+ ("\t: " + latLonPart)
//							+ ("\tlat: " + lat)
//							+ ("\tlon: " + lon));
//		}
//		System.out.println();
//		System.out.println(
//				(net.tourbook.common.UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//						+ ("\tsize: " + allGeoParts.toArray().length));

		return allGeoParts;
	}

}
