/*******************************************************************************
 * Copyright (C) 2024 Wolfgang Schramm and Contributors
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
package net.tourbook.map2.action;

import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;

import java.util.Set;

import net.tourbook.Images;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.data.TourData;
import net.tourbook.data.TourLocation;
import net.tourbook.data.TourLocationPoint;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.location.TourLocationData;
import net.tourbook.tour.location.TourLocationLookUpManager;
import net.tourbook.tour.location.TourLocationManager;

import org.eclipse.jface.action.Action;

public class ActionLookupTourLocation extends Action {

   private Map2View _map2View;

   private Long     _currentHoveredTourId;

   public ActionLookupTourLocation(final Map2View mapView) {

      super(Messages.Map_Action_LookUpTourLocation, AS_PUSH_BUTTON);

      _map2View = mapView;

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourLocation));
   }

   @Override
   public void run() {

      final TourData tourData = TourManager.getTour(_currentHoveredTourId);

      if (tourData == null

            // make sure the tour editor does not contain a modified tour
            || TourManager.isTourEditorModified()) {

         return;
      }

      final GeoPosition mouseMoveGeoPosition = _map2View.getMap().getMouseMove_GeoPosition();

      final double clickedTourPointLatitude = mouseMoveGeoPosition.latitude;
      final double clickedTourPointLongitude = mouseMoveGeoPosition.longitude;

      final LatLng clickedTourPoint = new LatLng(clickedTourPointLatitude, clickedTourPointLongitude);

      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      double closestDistance = Double.MAX_VALUE;
      int closestLatLonIndex = -1;

      for (int index = 0; index < latitudeSerie.length; ++index) {

         final LatLng currentLocation = new LatLng(latitudeSerie[index], longitudeSerie[index]);
         final double currentDistanceToClickedTourPoint = LatLngTool.distance(clickedTourPoint, currentLocation, LengthUnit.METER);

         if (currentDistanceToClickedTourPoint < closestDistance) {

            closestDistance = currentDistanceToClickedTourPoint;
            closestLatLonIndex = index;
         }
      }

      if (closestLatLonIndex == -1) {
         return;
      }

      final int relativeTourTime = tourData.timeSerie[closestLatLonIndex];
      final long absoluteTourTime = tourData.getTourStartTimeMS() + (relativeTourTime * 1000);

      final double latitude = latitudeSerie[closestLatLonIndex];
      final double longitude = longitudeSerie[closestLatLonIndex];

      final int latE6 = (int) (latitude * 1E6);
      final int lonE6 = (int) (longitude * 1E6);

      final int latE6_Normalized = latE6 + 90_000_000;
      final int lonE6_Normalized = lonE6 + 180_000_000;

      final int reqestedZoomlevel = 18;

      final Set<TourLocationPoint> allTourLocationPoints = tourData.getTourLocationPoints();

      TourLocation tourLocation = null;
      TourLocationPoint tourLocationPoint = null;

//      /*
//       * Get location point from current tour
//       */
//      for (final TourLocationPoint tourLocationPointFromTourData : allTourLocationPoints) {
//
//         final TourLocation tourLocationFromPoint = tourLocationPointFromTourData.getTourLocation();
//
//         if (tourLocationFromPoint.isInBoundingBox(reqestedZoomlevel, latE6_Normalized, lonE6_Normalized)) {
//
//            tourLocation = tourLocationFromPoint;
//            tourLocationPoint = tourLocationPointFromTourData;
//
//            break;
//         }
//      }
//
//      /*
//       * Get tour location from cache
//       */
//      if (tourLocationPoint == null) {
//
//         final TourLocationCache locationCache = TourLocationManager.getLocationCache();
//
//         tourLocation = locationCache.get(latE6_Normalized, lonE6_Normalized, reqestedZoomlevel);
//      }

      if (tourLocation == null) {

         // retrieve tour location

         final TourLocationData locationData = TourLocationManager.getLocationData(latitude, longitude, null, reqestedZoomlevel);

         if (locationData == null) {

            // Could not retrieve location point with latitude: %.6f - longitude: %.6f
            TourLogManager.log_DEFAULT(OtherMessages.LOG_TOUR_LOCATION_RETRIEVE_LOCATION_POINT.formatted(latitude, longitude));

            return;
         }

         tourLocation = locationData.tourLocation;

         tourLocationPoint = new TourLocationPoint(tourData, tourLocation);

         tourLocationPoint.setGeoPosition(latE6, lonE6);
         tourLocationPoint.setSerieIndex(closestLatLonIndex);
         tourLocationPoint.setTourTime(absoluteTourTime);

         allTourLocationPoints.add(tourLocationPoint);

         TourLocationLookUpManager.getTourLocationLookUps().add(tourLocation);
      }

      _map2View.getTourLocationDialog().updateUI(tourLocationPoint);

   }

   public void setCurrentHoveredTourId(final Long hoveredTourId) {

      _currentHoveredTourId = hoveredTourId;
   }

}
