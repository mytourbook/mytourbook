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

import de.byteholder.geoclipse.map.Map;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map2.Messages;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;

public class ActionCreateMarker extends Action {

	private Map2View	_mapView;
   private Map      _map;

   public ActionCreateMarker(final Map2View mapView, final Map map) {

      super("TOTO", AS_CHECK_BOX);

		_mapView = mapView;
      _map = map;

		setImageDescriptor(TourbookPlugin.getImageDescriptor(Messages.Image_Action_ShowSliderInMap));
	}

	@Override
	public void run() {
      //TODO was the click made on the track ??
      //  final double lat = _map..getMapPosition().getLatitude();
      //  final double lon = this._mapView.getMapLocation().getMapPosition().getLongitude();

      final TourMarker newTourMarker = new TourMarker();
      //  newTourMarker.setGeoPosition(lat, lon);

      final long tourId = _map.getHoveredTourId();

      if (tourId == Integer.MIN_VALUE) {
         return;
      }

      final TourData tourData = TourManager.getTour(tourId);

      final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, newTourMarker);

      markerDialog.create();
      markerDialog.addTourMarker(newTourMarker);

      if (markerDialog.open() == Window.OK) {
         TourManager.saveModifiedTour(tourData);
      }
	}

}
