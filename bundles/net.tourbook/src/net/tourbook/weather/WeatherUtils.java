package net.tourbook.weather;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
import com.javadocmd.simplelatlng.util.LengthUnit;

import net.tourbook.data.TourData;

public class WeatherUtils {

   /**
    * Determines the geographic area covered by a GPS track. The goal is to
    * encompass most of the track to search a weather station as close as possible
    * to the overall course and not just to a specific point.
    */
   public static LatLng determineWeatherSearchAreaCenter(final TourData tour) {

      final double[] latitudeSerie = tour.latitudeSerie;
      final double[] longitudeSerie = tour.longitudeSerie;

      // Looking for the farthest point of the track
      LatLng furthestPoint = null;
      double maxDistance = Double.MIN_VALUE;
      final LatLng startPoint = new LatLng(latitudeSerie[0], longitudeSerie[0]);

      for (int index = 1; index < latitudeSerie.length && index < longitudeSerie.length; ++index) {

         final LatLng currentPoint =
               new LatLng(latitudeSerie[index], longitudeSerie[index]);

         final double distanceFromStart =
               LatLngTool.distance(startPoint, currentPoint, LengthUnit.METER);

         if (distanceFromStart > maxDistance) {
            maxDistance = distanceFromStart;
            furthestPoint = currentPoint;
         }
      }

      final double distanceFromStart =
            LatLngTool.distance(startPoint, furthestPoint, LengthUnit.METER);
      final double bearingBetweenPoint =
            LatLngTool.initialBearing(startPoint, furthestPoint);

      // We find the center of the circle formed by the starting point and the farthest point
      final LatLng searchAreaCenter =
            LatLngTool.travel(startPoint, bearingBetweenPoint, distanceFromStart / 2, LengthUnit.METER);

      return searchAreaCenter;
   }

}
