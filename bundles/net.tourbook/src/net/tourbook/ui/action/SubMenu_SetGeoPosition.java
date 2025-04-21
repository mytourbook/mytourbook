/*******************************************************************************
 * Copyright (C) 2025 Wolfgang Schramm and Contributors
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
package net.tourbook.ui.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import net.tourbook.common.UI;
import net.tourbook.common.map.GeoPosition;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.database.TourDatabase;
import net.tourbook.importdata.ImportState_Process;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map2.Messages;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.swt.widgets.Menu;

/**
 */
public class SubMenu_SetGeoPosition extends SubMenu {

   public static final String GEO_MARKER_PREFIX = "#Geo"; //$NON-NLS-1$

   private List<TourMarker>   _allGeoMarker;
   private GeoPosition        _currentMouseGeoPosition;

   private class ActionRemoveAllGeoPositions extends Action {

      public ActionRemoveAllGeoPositions() {

         super(Messages.Map_Action_GeoMarkerPosition_RemoveAll, AS_PUSH_BUTTON);

         setToolTipText(Messages.Map_Action_GeoMarkerPosition_RemoveAll_Tooltip);
      }

      @Override
      public void run() {

         actionRemoveAllGeoPositions();
      }
   }

   private class ActionSetGeoPosition extends Action {

      private TourMarker _geoMarker;

      public ActionSetGeoPosition(final TourMarker geoMarker) {

         super(geoMarker.getLabel(), AS_PUSH_BUTTON);

         _geoMarker = geoMarker;
      }

      @Override
      public void run() {

         actionSetGeoPositionIntoGeoMarker(_geoMarker);
      }
   }

   public SubMenu_SetGeoPosition() {

      super(Messages.Map_Action_GeoMarkerPosition_SetIntoMarker.formatted(GEO_MARKER_PREFIX), AS_DROP_DOWN_MENU);

      setToolTipText(Messages.Map_Action_GeoMarkerPosition_SetIntoMarker_Tooltip.formatted(GEO_MARKER_PREFIX));
   }

   private void actionRemoveAllGeoPositions() {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      TourData tourData = null;

      // discard all geo marker
      for (final TourMarker tourMarker : _allGeoMarker) {

         tourMarker.setDescription(UI.EMPTY_STRING);

         // get tour data from a marker
         tourData = tourMarker.getTourData();
      }

      if (tourData == null) {
         return;
      }

      final ImportState_Process importState_Process = new ImportState_Process()

            // do not interpolate geo positions during the reimport
            .setIsSkipGeoInterpolation(true);

      final TourData reimportedTourData = RawDataManager.getInstance().reimportTour(tourData, importState_Process);

      if (reimportedTourData == null) {
         return;
      }

      final int[] timeSerie = reimportedTourData.timeSerie;
      final double[] reimportedLatitudeSerie = reimportedTourData.latitudeSerie;
      final double[] reimportedlongitudeSerie = reimportedTourData.longitudeSerie;

      // interpolate lat/lon
      reimportedTourData.createTimeSeries_20_InterpolateMissingValues(reimportedLatitudeSerie, timeSerie, false);
      reimportedTourData.createTimeSeries_20_InterpolateMissingValues(reimportedlongitudeSerie, timeSerie, false);

      // adjust distance/speed values
      TourManager.computeDistanceValuesFromGeoPosition(reimportedTourData);

      tourData.latitudeSerie = reimportedLatitudeSerie;
      tourData.longitudeSerie = reimportedlongitudeSerie;
      tourData.distanceSerie = reimportedTourData.distanceSerie;

      TourManager.saveModifiedTour(tourData);
   }

   private void actionSetGeoPositionIntoGeoMarker(final TourMarker geoMarker) {

      // make sure the tour editor does not contain a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final TourData tourData = geoMarker.getTourData();

      final ImportState_Process importState_Process = new ImportState_Process()

            // do not interpolate geo positions during the reimport
            .setIsSkipGeoInterpolation(true);

      final TourData reimportedTourData = RawDataManager.getInstance().reimportTour(tourData, importState_Process);

      if (reimportedTourData == null) {
         return;
      }

      final int[] timeSerie = tourData.timeSerie;
      final double[] reimportedLatitudeSerie = reimportedTourData.latitudeSerie;
      final double[] reimportedlongitudeSerie = reimportedTourData.longitudeSerie;

      final int geoMarkerSerieIndex = geoMarker.getSerieIndex();
      final double geoMarkerLatitude = _currentMouseGeoPosition.latitude;
      final double geoMarkerLongitude = _currentMouseGeoPosition.longitude;

      // insert current geo position with the current geo marker position into the lat/lon values
      reimportedLatitudeSerie[geoMarkerSerieIndex] = geoMarkerLatitude;
      reimportedlongitudeSerie[geoMarkerSerieIndex] = geoMarkerLongitude;

      // set lat/lon for all other geo markers
      createOtherGeoPositions(geoMarker, tourData, reimportedLatitudeSerie, reimportedlongitudeSerie);

      // interpolate lat/lon
      tourData.createTimeSeries_20_InterpolateMissingValues(reimportedLatitudeSerie, timeSerie, false);
      tourData.createTimeSeries_20_InterpolateMissingValues(reimportedlongitudeSerie, timeSerie, false);

      // adjust distance/speed values
      TourManager.computeDistanceValuesFromGeoPosition(reimportedTourData);

      tourData.latitudeSerie = reimportedLatitudeSerie;
      tourData.longitudeSerie = reimportedlongitudeSerie;
      tourData.distanceSerie = reimportedTourData.distanceSerie;

      // keep geo position in the marker
      geoMarker.setGeoPosition(geoMarkerLatitude, geoMarkerLongitude);
      geoMarker.setDescription(getGeoMarkerJSON(_currentMouseGeoPosition));

      TourManager.saveModifiedTour(tourData);
   }

   private void createOtherGeoPositions(final TourMarker currentGeoMarker,
                                        final TourData tourData,
                                        final double[] latitudeSerie,
                                        final double[] longitudeSerie) {

      final String geoPrefix = GEO_MARKER_PREFIX.toLowerCase();

      for (final TourMarker tourMarker : tourData.getTourMarkers()) {

         // skip current geo marker
         if (tourMarker.equals(currentGeoMarker)) {
            continue;
         }

         final String label = tourMarker.getLabel();

         if (label.trim().toLowerCase().startsWith(geoPrefix)) {

            // this is a geo marker

            final String markerDescription = tourMarker.getDescription();

            if (markerDescription != null) {

               try {

                  // parse JSON
                  final ObjectMapper mapper = new ObjectMapper();

                  final GeoPosition markerGeoPos = mapper.readValue(markerDescription, GeoPosition.class);

                  if (markerGeoPos != null) {

                     final int serieIndex = tourMarker.getSerieIndex();

                     latitudeSerie[serieIndex] = markerGeoPos.latitude;
                     longitudeSerie[serieIndex] = markerGeoPos.longitude;
                  }

               } catch (final JsonProcessingException e) {

                  StatusUtil.log(e);
               }
            }
         }
      }
   }

   @Override
   public void enableActions() {}

   @Override
   public void fillMenu(final Menu menu) {

      for (final TourMarker tourMarker : _allGeoMarker) {

         final ActionSetGeoPosition action = new ActionSetGeoPosition(tourMarker);

         new ActionContributionItem(action).fill(menu, -1);
      }

      new Separator().fill(menu, -1);

      new ActionContributionItem(new ActionRemoveAllGeoPositions()).fill(menu, -1);
   }

   private String getGeoMarkerJSON(final GeoPosition geoPosition) {

      try {

         final ObjectMapper mapper = new ObjectMapper();

         final String geoPosJSON = mapper.writeValueAsString(geoPosition);

         if (geoPosJSON.length() >= TourDatabase.VARCHAR_MAX_LENGTH) {

            StatusUtil.logError("Cannot save photoAdjustmentsJSON because it is > %d".formatted(TourDatabase.VARCHAR_MAX_LENGTH)); //$NON-NLS-1$

            return null;
         }

         return geoPosJSON;

      } catch (final JsonProcessingException e) {

         StatusUtil.log(e);
      }

      return null;
   }

   /**
    * These context data are set before the submenu is opened
    *
    * @param allGeoMarker
    * @param currentMouseGeoPosition
    */
   public void setContextData(final List<TourMarker> allGeoMarker, final GeoPosition currentMouseGeoPosition) {

      _allGeoMarker = allGeoMarker;
      _currentMouseGeoPosition = currentMouseGeoPosition;
   }

}
