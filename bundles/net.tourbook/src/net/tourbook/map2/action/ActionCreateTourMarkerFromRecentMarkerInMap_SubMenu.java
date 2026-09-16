/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.map2.view.Map2View;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.tourMarker.ActionClearRecentMarkers;
import net.tourbook.tourMarker.ActionCreateAndSave;
import net.tourbook.tourMarker.ActionHeader_AllRecentMarkers;
import net.tourbook.tourMarker.ActionHeader_CustomizeRecentMarkers;
import net.tourbook.tourMarker.ActionSortRecentMarkers;
import net.tourbook.tourMarker.RecentMarker;
import net.tourbook.tourMarker.TourMarkerManager;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.views.tourDataEditor.TourDataEditorView;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;

/**
 * Create a {@link TourMarker} from a recently used tour marker
 */
public class ActionCreateTourMarkerFromRecentMarkerInMap_SubMenu extends SubMenu {

   private Map2View                            _mapView;
   private Long                                _currentHoveredTourId;

   private ActionHeader_AllRecentMarkers       _actionHeader_AllRecentMarkers;
   private ActionHeader_CustomizeRecentMarkers _actionHeader_CustomizeRecentMarkers;
   private ActionClearRecentMarkers            _actionClearRecentMarkers;
   private ActionCreateAndSave                 _actionCreateAndSave;
   private ActionSortRecentMarkers             _actionSortRecentMarkers;

   private List<ActionRecentMarker>            _allRecentMarkerActions = new ArrayList<>();

   private class ActionRecentMarker extends Action {

      private RecentMarker __recentMarker;

      public ActionRecentMarker() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);

         setToolTipText(Messages.Action_TourMarker_RecentMarker_Tooltip);
      }

      @Override
      public void runWithEvent(final Event event) {

         actionCreateMarker(__recentMarker, event);
      }
   }

   public ActionCreateTourMarkerFromRecentMarkerInMap_SubMenu(final Map2View mapView) {

      super(Messages.Action_TourMarker_CreateFromRecentMarker, AS_DROP_DOWN_MENU);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_New));

      _mapView = mapView;

      createActions();
   }

   private void actionCreateMarker(final RecentMarker recentMarker, final Event event) {

      if (UI.isCtrlKey(event)) {

         // remove this marker

         TourMarkerManager.removeRecentMarker(recentMarker);

         return;
      }

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final TourData tourData = TourManager.getTour(_currentHoveredTourId);
      if (tourData == null) {
         return;
      }

      final double[] latSerie = tourData.latitudeSerie;
      final double[] lonSerie = tourData.longitudeSerie;

      final GeoPosition mouseGeoPosition = _mapView.getMap().getMouseMove_GeoPosition();

      final double clickedTourPointLatitude = mouseGeoPosition.latitude;
      final double clickedTourPointLongitude = mouseGeoPosition.longitude;

      final LatLng clickedTourPoint = new LatLng(clickedTourPointLatitude, clickedTourPointLongitude);

      double closestDistance = Double.MAX_VALUE;
      int closestLatLonIndex = -1;

      for (int index = 0; index < latSerie.length; ++index) {

         final LatLng currentLocation = new LatLng(latSerie[index], lonSerie[index]);
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
      final float[] altitudeSerie = tourData.altitudeSerie;
      final float[] distSerie = tourData.getMetricDistanceSerie();

      final String newMarkerLabel = recentMarker.label;

      // create a new marker
      final TourMarker newTourMarker = new TourMarker(tourData, ChartLabelMarker.MARKER_TYPE_CUSTOM);

      newTourMarker.setSerieIndex(closestLatLonIndex);
      newTourMarker.setTime(relativeTourTime, tourData.getTourStartTimeMS() + (relativeTourTime * 1000));
      newTourMarker.setLabel(newMarkerLabel);

      if (altitudeSerie != null) {
         newTourMarker.setAltitude(altitudeSerie[closestLatLonIndex]);
      }

      if (distSerie != null) {
         newTourMarker.setDistance(distSerie[closestLatLonIndex]);
      }

      newTourMarker.setGeoPosition(latSerie[closestLatLonIndex], lonSerie[closestLatLonIndex]);

      if (TourMarkerManager.isCreateAndSave()) {

         // update model
         tourData.getTourMarkers().add(newTourMarker);

         TourManager.saveModifiedTour(tourData);

         // set created marker to the top of the recent markers
         TourMarkerManager.addRecentMarker(newMarkerLabel);

         return;
      }

      final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, null);

      markerDialog.create();
      markerDialog.addTourMarker(newTourMarker);

      //We save instantly the marker so that it is displayed on the map while the user renames the marker name.
      //I found that otherwise, it's easy for the user to forget where the click was made.
      saveModifiedTour(tourData);

      markerDialog.open();

      //We save the tour again to take into account the action of the user (renamed the marker, cancelled the dialog...)
      saveModifiedTour(tourData);

      // set last used marker to the top of the list
      TourMarkerManager.addRecentMarker(newTourMarker.getLabel());
   }

   private void createActions() {

      for (int actionIndex = 0; actionIndex < TourMarkerManager.MAX_NUMBER_OF_RECENT_MARKERS; actionIndex++) {

         _allRecentMarkerActions.add(new ActionRecentMarker());
      }

// SET_FORMATTING_OFF

      _actionHeader_AllRecentMarkers         = new ActionHeader_AllRecentMarkers();
      _actionHeader_CustomizeRecentMarkers   = new ActionHeader_CustomizeRecentMarkers();
      _actionClearRecentMarkers              = new ActionClearRecentMarkers();
      _actionCreateAndSave                   = new ActionCreateAndSave();
      _actionSortRecentMarkers               = new ActionSortRecentMarkers();

// SET_FORMATTING_ON
   }

   @Override
   public void enableActions() {

      _actionCreateAndSave.setChecked(TourMarkerManager.isCreateAndSave());
   }

   @Override
   public void fillMenu(final Menu menu) {

      addActionToMenu(_actionHeader_AllRecentMarkers);

      final LinkedList<RecentMarker> allRecentMarkers = TourMarkerManager.getRecentMarkers();
      final int numRecentMarkers = allRecentMarkers.size();

      for (int markerIndex = 0; markerIndex < numRecentMarkers; markerIndex++) {

         final RecentMarker recentMarker = allRecentMarkers.get(markerIndex);

         if (recentMarker == null) {
            break;
         }

         // update recycled marker action
         final ActionRecentMarker actionRecentMarker = _allRecentMarkerActions.get(markerIndex);

         actionRecentMarker.setText(recentMarker.label);
         actionRecentMarker.__recentMarker = recentMarker;

         addActionToMenu(actionRecentMarker);
      }

      addSeparatorToMenu();
      addActionToMenu(_actionHeader_CustomizeRecentMarkers);
      addActionToMenu(_actionCreateAndSave);
      addActionToMenu(_actionSortRecentMarkers);
      addActionToMenu(_actionClearRecentMarkers);
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

   public void setCurrentHoveredTourId(final Long hoveredTourId) {
      _currentHoveredTourId = hoveredTourId;
   }
}
