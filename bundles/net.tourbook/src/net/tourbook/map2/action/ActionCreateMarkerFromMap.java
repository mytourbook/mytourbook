/*******************************************************************************
 * Copyright (C) 2005, 2019  Wolfgang Schramm and Contributors
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

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.ChartLabel;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionCreateMarkerFromMap extends Action {

   private Map2View _mapView;
   private long     _currentHoverTourId;

   public ActionCreateMarkerFromMap(final Map2View mapView) {

      super(Messages.Map_Action_CreateMarkerFromMap, AS_PUSH_BUTTON);

      _mapView = mapView;

      setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_Action_CreateMarkerFromMap));
   }

   @Override
   public void run() {

      final TourData tourData = TourManager.getTour(_currentHoverTourId);

      if (tourData == null) {
         return;
      }

      final double clickedTourPointLatitude = this._mapView.getMapLocation().getMapPosition().getLatitude();
      final double clickedTourPointLongitude = this._mapView.getMapLocation().getMapPosition().getLongitude();

      final LatLng clickedTourPoint = new LatLng(clickedTourPointLatitude, clickedTourPointLongitude);
      double closestDistance = Double.MAX_VALUE;
      int closestLatLongIndex = -1;
      for (int index = 0; index < tourData.latitudeSerie.length; ++index) {
         final LatLng currentLocation = new LatLng(tourData.latitudeSerie[index],
               tourData.longitudeSerie[index]);
         final double currentDistanceToClickedTourPoint = LatLngTool.distance(clickedTourPoint, currentLocation, LengthUnit.METER);
         if (currentDistanceToClickedTourPoint < closestDistance) {
            closestDistance = currentDistanceToClickedTourPoint;
            closestLatLongIndex = index;
         }
      }

      if (closestLatLongIndex == -1) {
         return;
      }

      final int relativeTourTime = tourData.timeSerie[closestLatLongIndex];
      final float[] altitudeSerie = tourData.altitudeSerie;
      final float[] distSerie = tourData.getMetricDistanceSerie();
      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      // create a new marker
      final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_CUSTOM);
      tourMarker.setSerieIndex(closestLatLongIndex);
      tourMarker.setTime(relativeTourTime, tourData.getTourStartTimeMS() + (relativeTourTime * 1000));

      if (altitudeSerie != null) {
         tourMarker.setAltitude(altitudeSerie[closestLatLongIndex]);
      }

      if (distSerie != null) {
         tourMarker.setDistance(distSerie[closestLatLongIndex]);
      }

      if (latitudeSerie != null) {
         tourMarker.setGeoPosition(latitudeSerie[closestLatLongIndex], longitudeSerie[closestLatLongIndex]);
      }

      final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, tourMarker);

      markerDialog.create();
      markerDialog.addTourMarker(tourMarker);

      if (markerDialog.open() == Window.OK) {
         TourManager.saveModifiedTour(tourData);
      }
   }

   public void setCurrentHoverTourId(final long tourId) {
      _currentHoverTourId = tourId;
   }

}
