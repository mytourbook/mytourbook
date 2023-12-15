/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour.location;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.tourbook.data.TourLocation;

/**
 * Cache for {@link TourLocation}
 */
public class TourLocationCache {

   private final int                                     _maxLocations;

   private final ConcurrentHashMap<String, TourLocation> _locationCache     = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<String>           _locationCacheFifo = new ConcurrentLinkedQueue<>();

   public TourLocationCache(final int maxLocations) {

      _maxLocations = maxLocations;
   }

   public void add(final String locationKey, final TourLocation location) {

      // check if space is available in the cache
      final int cacheSize = _locationCacheFifo.size();

      if (cacheSize > _maxLocations) {

         // remove cached locations
         for (int cacheIndex = _maxLocations; cacheIndex < cacheSize; cacheIndex++) {
            removeLocation(_locationCacheFifo.poll());
         }
      }

      _locationCache.put(locationKey, location);
      _locationCacheFifo.add(locationKey);
   }

   public TourLocation get(final String locationKey) {

      return _locationCache.get(locationKey);
   }

   public void remove(final String locationKey) {

      _locationCacheFifo.remove(locationKey);

      removeLocation(locationKey);
   }

   /**
    * Removes all locations
    */
   public synchronized void removeAll() {

      _locationCache.clear();
      _locationCacheFifo.clear();
   }

   private void removeLocation(final String locationKey) {

      _locationCache.remove(locationKey);
   }
}
