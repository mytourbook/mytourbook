/*******************************************************************************
 * Copyright (C) 2023, 2024 Wolfgang Schramm and Contributors
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
 * Cache for {@link TourLocation}s
 */
public class TourLocationCache {

   private final int                                   _maxLocations;

   private final ConcurrentHashMap<Long, TourLocation> _locationCache     = new ConcurrentHashMap<>();
   private final ConcurrentLinkedQueue<Long>           _locationCacheFifo = new ConcurrentLinkedQueue<>();

   public TourLocationCache(final int maxLocations) {

      _maxLocations = maxLocations;
   }

   public void add(final Long locationKey, final TourLocation tourLocation) {

      // check if space is available in the cache
      final int cacheSize = _locationCacheFifo.size();

      if (cacheSize > _maxLocations) {

         // remove cached locations
         for (int cacheIndex = _maxLocations; cacheIndex < cacheSize; cacheIndex++) {
            removeLocation(_locationCacheFifo.poll());
         }
      }

      _locationCache.put(locationKey, tourLocation);
      _locationCacheFifo.add(locationKey);
   }

   public TourLocation get(final int latitudeE6_Normalized, final int longitudeE6_Normalized, final int zoomlevel) {

      for (final TourLocation tourLocation : _locationCache.values()) {

         if (tourLocation.isInBoundingBox(zoomlevel, latitudeE6_Normalized, longitudeE6_Normalized)) {

            return tourLocation;
         }
      }

      return null;
   }

   public void remove(final Long locationKey) {

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

   private void removeLocation(final Long locationKey) {

      _locationCache.remove(locationKey);
   }
}
