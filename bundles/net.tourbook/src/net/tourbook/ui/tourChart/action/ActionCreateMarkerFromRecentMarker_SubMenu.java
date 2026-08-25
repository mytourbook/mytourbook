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
package net.tourbook.ui.tourChart.action;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartXSlider;
import net.tourbook.chart.IChartContextProvider;
import net.tourbook.common.UI;
import net.tourbook.common.ui.SubMenu;
import net.tourbook.data.TourData;
import net.tourbook.data.TourMarker;
import net.tourbook.tour.DialogMarker;
import net.tourbook.tour.TourManager;
import net.tourbook.tourMarker.RecentMarker;
import net.tourbook.tourMarker.TourMarkerManager;
import net.tourbook.ui.tourChart.ChartLabelMarker;
import net.tourbook.ui.tourChart.TourChartContextProvider;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;

/**
 * Create a {@link TourMarker} from a recently used tour marker
 */
public class ActionCreateMarkerFromRecentMarker_SubMenu extends SubMenu {

   private List<ActionRecentMarker>    _allRecentMarkerActions = new ArrayList<>();

   private final IChartContextProvider _chartContextProvider;
   private boolean                     _isLeftSlider;

   private IMarkerReceiver             _markerReceiver;

   private class ActionRecentMarker extends Action {

      private RecentMarker __recentMarker;

      public ActionRecentMarker() {

         super(UI.EMPTY_STRING, AS_PUSH_BUTTON);
      }

      @Override
      public void run() {

         actionCreateMarker(__recentMarker);
      }
   }

   public ActionCreateMarkerFromRecentMarker_SubMenu(final TourChartContextProvider tourChartContextProvider) {

      super(Messages.Action_TourMarker_CreateFromRecentMarker, AS_DROP_DOWN_MENU);

      setImageDescriptor(TourbookPlugin.getThemedImageDescriptor(Images.TourMarker_New));

      _chartContextProvider = tourChartContextProvider;

      for (int actionIndex = 0; actionIndex < TourMarkerManager.MAX_NUMBER_OF_RECENT_MARKERS; actionIndex++) {

         _allRecentMarkerActions.add(new ActionRecentMarker());
      }
   }

   private void actionCreateMarker(final RecentMarker recentMarker) {

      final Chart chart = _chartContextProvider.getChart();

      TourData tourData = null;
      final Object tourId = chart.getChartDataModel().getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
      if (tourId instanceof Long) {
         tourData = TourManager.getInstance().getTourData((Long) tourId);
      }

      if (tourData == null) {
         return;
      }

      final TourMarker newTourMarker = createTourMarker(tourData);
      if (newTourMarker == null) {
         return;
      }

      // set data from the recent marker
      newTourMarker.setLabel(recentMarker.label);

      if (_markerReceiver != null) {

         _markerReceiver.addTourMarker(newTourMarker);

         // the marker dialog will not be opened
         return;
      }

      final DialogMarker markerDialog = new DialogMarker(Display.getCurrent().getActiveShell(), tourData, null);

      markerDialog.create();
      markerDialog.addTourMarker(newTourMarker);

      if (markerDialog.open() == Window.OK) {

         TourManager.saveModifiedTour(tourData);

         // set created marker to the top of the recent markers
         TourMarkerManager.addRecentMarker(newTourMarker.getLabel());
      }
   }

   /**
    * Creates a new marker
    *
    * @param tourData
    *
    * @return
    */
   private TourMarker createTourMarker(final TourData tourData) {

      final ChartXSlider leftSlider = _chartContextProvider.getLeftSlider();
      final ChartXSlider rightSlider = _chartContextProvider.getRightSlider();

      final ChartXSlider slider = rightSlider == null
            ? leftSlider
            : _isLeftSlider
                  ? leftSlider
                  : rightSlider;

      if (slider == null || tourData.timeSerie == null) {
         return null;
      }

      final int serieIndex = slider.getValuesIndex();
      final int relativeTourTime = tourData.timeSerie[serieIndex];
      final float[] altitudeSerie = tourData.altitudeSerie;
      final float[] distSerie = tourData.getMetricDistanceSerie();
      final double[] latitudeSerie = tourData.latitudeSerie;
      final double[] longitudeSerie = tourData.longitudeSerie;

      // create a new marker
      final TourMarker tourMarker = new TourMarker(tourData, ChartLabelMarker.MARKER_TYPE_CUSTOM);
      tourMarker.setSerieIndex(serieIndex);
      tourMarker.setLabel(Messages.TourData_Label_new_marker);
      tourMarker.setTime(relativeTourTime, tourData.getTourStartTimeMS() + (relativeTourTime * 1000));

      if (altitudeSerie != null) {
         tourMarker.setAltitude(altitudeSerie[serieIndex]);
         //tourMarker.setDescription("#alti: " + (int)altitudeSerie[serieIndex] + " m");
      }

      if (distSerie != null) {
         tourMarker.setDistance(distSerie[serieIndex]);
      }

      if (latitudeSerie != null) {
         tourMarker.setGeoPosition(latitudeSerie[serieIndex], longitudeSerie[serieIndex]);
      }

      return tourMarker;
   }

   @Override
   public void enableActions() {

   }

   @Override
   public void fillMenu(final Menu menu) {

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
   }

   public void setMarkerReceiver(final IMarkerReceiver markerReceiver) {
      _markerReceiver = markerReceiver;
   }

}
