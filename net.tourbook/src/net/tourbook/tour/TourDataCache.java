/*******************************************************************************
 * Copyright (C) 2005, 2011  Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import net.tourbook.data.TourData;

/**
 * Cache for {@link TourData}
 */
class TourDataCache {

	private final ConcurrentHashMap<Long, TourData>	_tourCache;
	private final Queue<Long>						_fifoQueue;

	private int										_cacheSize;
	private int										_cacheLowMark;

	private static final ReentrantLock				CACHE_LOCK	= new ReentrantLock();

	public TourDataCache(final int cacheSize) {

		_cacheSize = cacheSize;
		_cacheLowMark = Math.max(1, cacheSize - 100);

		_tourCache = new ConcurrentHashMap<Long, TourData>();
		_fifoQueue = new ArrayBlockingQueue<Long>(_cacheSize);
	}

	public void clear() {

		CACHE_LOCK.lock();
		{
			try {
				_tourCache.clear();
				_fifoQueue.clear();
			} finally {
				CACHE_LOCK.unlock();
			}
		}
	}

	public TourData get(final Long tourId) {
		return _tourCache.get(tourId);
	}

	public ConcurrentHashMap<Long, TourData> getCache() {
		return _tourCache;
	}

	public void put(final Long tourId, final TourData tourData) {

		CACHE_LOCK.lock();
		{
			try {

				final int fifoQueueSize = _fifoQueue.size();

				// MUST test with >= otherwise a Queue full exceptions occures
				if (fifoQueueSize >= _cacheSize) {

//					final long start = System.nanoTime();

					for (int adjustedQueueSize = fifoQueueSize; adjustedQueueSize > _cacheLowMark; adjustedQueueSize--) {

						// remove all entries until the cache low mark is reached

						final Long head = _fifoQueue.poll();
						_tourCache.remove(head);
					}

//					final long end = System.nanoTime();
//					System.out.println("TourDataCache: removed "
//							+ (queueSize - MAX_TOUR_CACHE_ENTRIES_LOW)
//							+ " items in "
//							+ ((end - start) / 1000000.0)
//							+ "ms");
//					// TODO remove SYSTEM.OUT.PRINTLN
				}

				// remove entry which has the same tour id
				if (_tourCache.containsKey(tourId)) {
					_tourCache.remove(tourId);
					_fifoQueue.remove(tourId);
				}

				_tourCache.put(tourId, tourData);
				_fifoQueue.add(tourId);

			} finally {
				CACHE_LOCK.unlock();
			}
		}
	}

	public void remove(final Long tourId) {

		if (_tourCache.containsKey(tourId)) {

			CACHE_LOCK.lock();
			{
				try {
					_tourCache.remove(tourId);
					_fifoQueue.remove(tourId);
				} finally {
					CACHE_LOCK.unlock();
				}
			}
		}
	}
}
