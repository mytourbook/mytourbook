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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.OtherMessages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourLocation;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.tour.location.TourLocationData;
import net.tourbook.tour.location.TourLocationManager;
import net.tourbook.tour.location.TourLocationManager.ZoomLevel;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Menu;

public class ActionLookupCommonLocation extends SubMenu {

   private Map2View                _map2View;

//   private Long                    _currentHoveredTourId;

   private List<ActionSetLocation> _allSubmenuActions = new ArrayList<>();

   private class ActionSetLocation extends Action {

      private ZoomLevel _zoomLevel;

      public ActionSetLocation(final ZoomLevel zoomLevel) {

         super(TourLocationManager.ZOOM_LEVEL_ITEM.formatted(zoomLevel.zoomlevel, zoomLevel.label), AS_PUSH_BUTTON);

         _zoomLevel = zoomLevel;
      }

      @Override
      public void run() {

         lookupMapLocaction(_zoomLevel.zoomlevel);
      }
   }

   public ActionLookupCommonLocation(final Map2View mapView) {

      super(Messages.Map_Action_LookupCommonLocation, AS_DROP_DOWN_MENU);

      _map2View = mapView;

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.MapLocation));

      /*
       * Create all sub menu actions
       */

      // show building zoomlevel at the top
      final List<ZoomLevel> allZoomLevels = Arrays.asList(TourLocationManager.ALL_ZOOM_LEVEL);
      final List<ZoomLevel> allZoomLevelsReverse = new ArrayList<>(allZoomLevels);
      Collections.reverse(allZoomLevelsReverse);

      for (final ZoomLevel zoomlevel : allZoomLevelsReverse) {
         _allSubmenuActions.add(new ActionSetLocation(zoomlevel));
      }
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      for (final Action action : _allSubmenuActions) {
         addActionToMenu(menu, action);
      }
   }

   private void lookupMapLocaction(final int reqestZoomlevel) {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final GeoPosition mouseMoveGeoPosition = _map2View.getMap().getMouseMove_GeoPosition();

      final double clickedTourPointLatitude = mouseMoveGeoPosition.latitude;
      final double clickedTourPointLongitude = mouseMoveGeoPosition.longitude;

      final double locationLatitude = clickedTourPointLatitude;
      final double locationLongitude = clickedTourPointLongitude;

      // get hovered tour
//      final TourData tourData = TourManager.getTour(_currentHoveredTourId);
//
//      long absoluteTourTime = 0;
//      int closestLatLonIndex = -1;
//
//      if (tourData != null) {
//
//         // a tour is hovered, adjust location to a tour slice
//
//         final double[] latitudeSerie = tourData.latitudeSerie;
//         final double[] longitudeSerie = tourData.longitudeSerie;
//
//         double closestDistance = Double.MAX_VALUE;
//
//         for (int index = 0; index < latitudeSerie.length; ++index) {
//
//            final double currentDistanceToClickedTourPoint = MtMath.distanceVincenty(clickedTourPointLatitude,
//                  clickedTourPointLongitude,
//                  latitudeSerie[index],
//                  longitudeSerie[index]);
//
//            if (currentDistanceToClickedTourPoint < closestDistance) {
//
//               closestDistance = currentDistanceToClickedTourPoint;
//               closestLatLonIndex = index;
//            }
//         }
//
//         if (closestLatLonIndex != -1) {
//
//            final int relativeTourTime = tourData.timeSerie[closestLatLonIndex];
//            absoluteTourTime = tourData.getTourStartTimeMS() + (relativeTourTime * 1000);
//
//            locationLatitude = latitudeSerie[closestLatLonIndex];
//            locationLongitude = longitudeSerie[closestLatLonIndex];
//         }
//      }

      /*
       * Retrieve tour location
       */

      final TourLocationData locationData = TourLocationManager.getLocationData(

            locationLatitude,
            locationLongitude,
            null,
            reqestZoomlevel,
            false);

      if (locationData == null) {

         // Could not retrieve location point with latitude: %.6f - longitude: %.6f
         TourLogManager.log_DEFAULT(OtherMessages.LOG_TOUR_LOCATION_RETRIEVE_LOCATION_POINT.formatted(locationLatitude, locationLongitude));

         return;
      }

      final TourLocation tourLocation = locationData.tourLocation;

//      if (tourData != null && closestLatLonIndex != -1) {
//
//         final int latE6 = (int) (locationLatitude * 1E6);
//         final int lonE6 = (int) (locationLongitude * 1E6);
//
//         final TourLocationPoint tourLocationPoint = new TourLocationPoint(tourData, tourLocation);
//
//         tourLocationPoint.setGeoPosition(latE6, lonE6);
//         tourLocationPoint.setSerieIndex(closestLatLonIndex);
//         tourLocationPoint.setTourTime(absoluteTourTime);
//
//         tourData.getTourLocationPoints().add(tourLocationPoint);
//      }

      _map2View.addCommonLocation(tourLocation);
   }

   public void setCurrentHoveredTourId(final Long hoveredTourId) {

//      _currentHoveredTourId = hoveredTourId;
   }

}
