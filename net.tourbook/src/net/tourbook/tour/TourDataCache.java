/*******************************************************************************
 * Copyright (C) 2005, 2008  Wolfgang Schramm and Contributors
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

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import net.tourbook.data.TourData;

/**
 * Cache for {@link TourData}
 */
class TourDataCache {

	private static final int			MAX_TOUR_CACHE_ENTRIES	= 100;

	private final Map<Long, TourData>	tourCache				= new HashMap<Long, TourData>();
	private final Queue<Long>			fifoQueue				= new ArrayBlockingQueue<Long>(MAX_TOUR_CACHE_ENTRIES);

	public synchronized void clear() {
		tourCache.clear();
		fifoQueue.clear();
	}

	public TourData get(final Long tourId) {

		if (tourCache.containsKey(tourId)) {
			return tourCache.get(tourId);
		}

		return null;
	}

	public synchronized void put(final Long tourId, final TourData tourData) {

		if (fifoQueue.size() >= MAX_TOUR_CACHE_ENTRIES) {
			// remove oldest entry
			final Long head = fifoQueue.poll();
			tourCache.remove(head);
		}

		// must the same tour data be removed to prevent memory leaks??
		if (tourCache.containsKey(tourId)) {
			tourCache.remove(tourId);
			fifoQueue.remove(tourId);
		}

		tourCache.put(tourId, tourData);
		fifoQueue.add(tourId);
	}

	public synchronized void remove(final Long tourId) {

		if (tourCache.containsKey(tourId)) {
			tourCache.remove(tourId);
			fifoQueue.remove(tourId);
		}
	}
}
