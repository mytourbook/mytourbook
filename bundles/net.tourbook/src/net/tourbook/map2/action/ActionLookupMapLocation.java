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
import net.tourbook.tour.location.MapLocationManager;
import net.tourbook.tour.location.TourLocationData;
import net.tourbook.tour.location.TourLocationManager;

import org.eclipse.jface.action.Action;

public class ActionLookupMapLocation extends Action {

   private Map2View _map2View;

   private Long     _currentHoveredTourId;

   public ActionLookupMapLocation(final Map2View mapView) {

      super(Messages.Map_Action_LookUpMapLocation, AS_PUSH_BUTTON);

      _map2View = mapView;

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.MapLocation));
   }

   @Override
   public void run() {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final GeoPosition mouseMoveGeoPosition = _map2View.getMap().getMouseMove_GeoPosition();

      final double clickedTourPointLatitude = mouseMoveGeoPosition.latitude;
      final double clickedTourPointLongitude = mouseMoveGeoPosition.longitude;

      double locationLatitude = clickedTourPointLatitude;
      double locationLongitude = clickedTourPointLongitude;

      // get hovered tour
      final TourData tourData = TourManager.getTour(_currentHoveredTourId);

      long absoluteTourTime = 0;
      int closestLatLonIndex = -1;

      if (tourData != null) {

         // a tour is hovered, adjust location to a tour slice

         final LatLng clickedTourPoint = new LatLng(clickedTourPointLatitude, clickedTourPointLongitude);

         final double[] latitudeSerie = tourData.latitudeSerie;
         final double[] longitudeSerie = tourData.longitudeSerie;

         double closestDistance = Double.MAX_VALUE;

         for (int index = 0; index < latitudeSerie.length; ++index) {

            final LatLng currentLocation = new LatLng(latitudeSerie[index], longitudeSerie[index]);
            final double currentDistanceToClickedTourPoint = LatLngTool.distance(clickedTourPoint, currentLocation, LengthUnit.METER);

            if (currentDistanceToClickedTourPoint < closestDistance) {

               closestDistance = currentDistanceToClickedTourPoint;
               closestLatLonIndex = index;
            }
         }

         if (closestLatLonIndex != -1) {

            final int relativeTourTime = tourData.timeSerie[closestLatLonIndex];
            absoluteTourTime = tourData.getTourStartTimeMS() + (relativeTourTime * 1000);

            locationLatitude = latitudeSerie[closestLatLonIndex];
            locationLongitude = longitudeSerie[closestLatLonIndex];
         }

      }

      final int reqestedZoomlevel = 18;

      /*
       * Retrieve tour location
       */

      final TourLocationData locationData = TourLocationManager.getLocationData(locationLatitude, locationLongitude, null, reqestedZoomlevel);

      if (locationData == null) {

         // Could not retrieve location point with latitude: %.6f - longitude: %.6f
         TourLogManager.log_DEFAULT(OtherMessages.LOG_TOUR_LOCATION_RETRIEVE_LOCATION_POINT.formatted(locationLatitude, locationLongitude));

         return;
      }

      final TourLocation tourLocation = locationData.tourLocation;

      if (tourData != null && closestLatLonIndex != -1) {

         final int latE6 = (int) (locationLatitude * 1E6);
         final int lonE6 = (int) (locationLongitude * 1E6);

         final TourLocationPoint tourLocationPoint = new TourLocationPoint(tourData, tourLocation);

         tourLocationPoint.setGeoPosition(latE6, lonE6);
         tourLocationPoint.setSerieIndex(closestLatLonIndex);
         tourLocationPoint.setTourTime(absoluteTourTime);

         tourData.getTourLocationPoints().add(tourLocationPoint);
      }

      MapLocationManager.addLocation(tourLocation);
   }

   public void setCurrentHoveredTourId(final Long hoveredTourId) {

      _currentHoveredTourId = hoveredTourId;
   }

}
