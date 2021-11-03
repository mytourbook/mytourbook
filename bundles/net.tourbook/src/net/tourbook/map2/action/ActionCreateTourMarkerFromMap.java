/*******************************************************************************
 * Copyright (C) 2019, 2021 Frédéric Bard
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
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;

public class ActionCreateTourMarkerFromMap extends Action {

   private Map2View _mapView;
   private long     _currentHoverTourId;

   public ActionCreateTourMarkerFromMap(final Map2View mapView) {

      super(Messages.Map_Action_CreateTourMarkerFromMap, AS_PUSH_BUTTON);

      _mapView = mapView;

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_New));
   }

   @Override
   public void run() {

      final TourData tourData = TourManager.getTour(_currentHoverTourId);

      if (tourData == null ||
      // make sure the tour editor does not contain a modified tour
            TourManager.isTourEditorModified()) {
         return;
      }

      final double clickedTourPointLatitude = this._mapView.getMap().get_mouseMove_GeoPosition().latitude;
      final double clickedTourPointLongitude = this._mapView.getMap().get_mouseMove_GeoPosition().longitude;

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
      final TourMarker tourMarker = new TourMarker(tourData, ChartLabelMarker.MARKER_TYPE_CUSTOM);
      tourMarker.setSerieIndex(closestLatLongIndex);
      tourMarker.setTime(relativeTourTime, tourData.getTourStartTimeMS() + (relativeTourTime * 1000));
      tourMarker.setLabel(Messages.Default_Label_NewTourMarker);

      if (altitudeSerie != null) {
         tourMarker.setAltitude(altitudeSerie[closestLatLongIndex]);
      }

      if (distSerie != null) {
         tourMarker.setDistance(distSerie[closestLatLongIndex]);
      }

      if (latitudeSerie != null) {
         tourMarker.setGeoPosition(latitudeSerie[closestLatLongIndex], longitudeSerie[closestLatLongIndex]);
      }

      final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, null);

      markerDialog.create();
      markerDialog.addTourMarker(tourMarker);

      //We save instantly the marker so that it is displayed on the map while the user renames the marker name.
      //I found that otherwise, it's easy for the user to forget where the click was made.
      saveModifiedTour(tourData);

      markerDialog.open();

      //We save the tour again to take into account the action of the user (renamed the marker, cancelled the dialog...)
      saveModifiedTour(tourData);
   }

   /**
    * Saves a modified tour. In this case, a marker was modified.
    * Additionally, we update the tour data in the tour data editor as, otherwise, it
    * can raise a DB out of sync error message.
    * As an example after analysis and comparing the tour here and the one in the data editor,
    * I found that the power series could be computed and if it was not already, the compared
    * tours will be viewed as different.
    *
    * @param tourData
    *           The tour to be saved
    */
   private void saveModifiedTour(final TourData tourData) {

      final TourDataEditorView tourDataEditor = TourManager.getTourDataEditor();
      if (tourDataEditor != null) {
         tourDataEditor.updateUI(tourData);
      }

      TourManager.saveModifiedTour(tourData);
   }

   public void setCurrentHoverTourId(final long tourId) {
      _currentHoverTourId = tourId;
   }

}
