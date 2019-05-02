/*******************************************************************************
 * Copyright (C) 2005, 2010  Wolfgang Schramm and Contributors
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

public class ActionCreateMarker extends Action {

	private Map2View	_mapView;
   private long     _currentHoverTourId;

   public ActionCreateMarker(final Map2View mapView) {

      super("TOTO", AS_CHECK_BOX);

		_mapView = mapView;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_Action_ShowSliderInMap));
	}

   @Override
public void run() {
   //TODO was the click made on the track ??
      final double lat = this._mapView.getMapLocation().getMapPosition().getLatitude();
      final double lon = this._mapView.getMapLocation().getMapPosition().getLongitude();





      final TourData tourData = TourManager.getTour(_currentHoverTourId);

      if(tourData == null) {
         return;
      }

      int tourClickedIndex = -1;
      final double closestLatitudeDistance = Double.MAX_VALUE;
      final int closestLatitudeIndex = -1;
      for (int index = 0; index < tourData.latitudeSerie.length; ++index)
      {
         final long currentDistanceDifference = Math.abs(tourData.latitudeSerie[index] - lat);
         if (currentDistanceDifference < closestLatitudeDistance) {
            closestLatitudeDistance = currentDistanceDifference;
            closestLatitudeIndex = index;
         }

         if (tourData.latitudeSerie[index] == lat && tourData.longitudeSerie[index] == lon) {
            tourClickedIndex = index;
            break;
         }
      }

      final TourMarker newTourMarker = new TourMarker();
      newTourMarker.setGeoPosition(lat, lon);

      final int relativeTourTime = tourData.timeSerie[tourClickedIndex];
      final float[] altitudeSerie = tourData.altitudeSerie;
      final float[] distSerie = tourData.getMetricDistanceSerie();
      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      // create a new marker
      final TourMarker tourMarker = new TourMarker(tourData, ChartLabel.MARKER_TYPE_CUSTOM);
      tourMarker.setSerieIndex(tourClickedIndex);
      tourMarker.setLabel("Tdedededed");
      tourMarker.setTime(relativeTourTime, tourData.getTourStartTimeMS() + (relativeTourTime * 1000));

      if (altitudeSerie != null) {
         tourMarker.setAltitude(altitudeSerie[tourClickedIndex]);
         //tourMarker.setDescription("#alti: " + (int)altitudeSerie[serieIndex] + " m");
      }

      if (distSerie != null) {
         tourMarker.setDistance(distSerie[tourClickedIndex]);
      }

      if (latitudeSerie != null) {
         tourMarker.setGeoPosition(latitudeSerie[tourClickedIndex], longitudeSerie[tourClickedIndex]);
      }

   final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, newTourMarker);

   markerDialog.create();
   markerDialog.addTourMarker(newTourMarker);

   if (markerDialog.open() == Window.OK) {
      TourManager.saveModifiedTour(tourData);
   }
}

	public void setCurrentHoverTourId(final long tourId)
     {
        _currentHoverTourId = tourId;
     }

}
