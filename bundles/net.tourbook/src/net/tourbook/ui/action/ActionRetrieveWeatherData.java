/*******************************************************************************
 * Copyright (C) 2005, 2019 Wolfgang Schramm and Contributors
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

import com.javadocmd.simplelatlng.LatLng;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.ITourProvider2;
import net.tourbook.weather.HistoricalWeatherRetriever;
import net.tourbook.weather.WeatherData;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class ActionRetrieveWeatherData extends Action {
   private final ITourProvider2   _tourProvider;
   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   public ActionRetrieveWeatherData(final ITourProvider2 tourProvider) {

      super(null, AS_PUSH_BUTTON);

      _tourProvider = tourProvider;

      setText(Messages.Tour_Action_RetrieveWeatherData);
   }

   @Override
   public void run() {

      // check if the tour editor contains a modified tour
      if (TourManager.isTourEditorModified()) {
         return;
      }

      final ArrayList<TourData> selectedTours = _tourProvider.getSelectedTours();

      final Shell shell = Display.getCurrent().getActiveShell();
      if (selectedTours == null || selectedTours.size() < 1) {

         // a tour is not selected

         MessageDialog.openInformation(
               shell,
               Messages.Dialog_RetrieveWeather_Dialog_Title,
               Messages.UI_Label_TourIsNotSelected);

         return;
      }

      final LatLng startPoint = new LatLng(selectedTours.get(0).latitudeSerie[0], selectedTours.get(0).longitudeSerie[0]);

      final HistoricalWeatherRetriever historicalWeatherRetriever = HistoricalWeatherRetriever
            .where(startPoint)
            .when(DateTimeFormatter.ofPattern("yyyy-MM-dd").format(selectedTours.get(0).getTourStartTime()),
                  selectedTours.get(0).getStartTimeOfDay() / 3600)
            .forUser(_prefStore.getString(ITourbookPreferences.API_KEY))
            .retrieve();

      final WeatherData historicalWeatherData = historicalWeatherRetriever.getHistoricalWeatherData();
      if (historicalWeatherData == null) {
         MessageDialog.openInformation(
               shell,
               Messages.Dialog_RetrieveWeather_Dialog_Title,
               Messages.UI_Label_TourIsNotSelected);

         return;
      }

      // For the request, get the half-point of the route just like in CG
      for (final TourData tour : selectedTours) {
         // TODO WHich avg temperature ? the one of the day ? the one of the hour ?
         tour.setAvgTemperature(historicalWeatherData.getTemperatureAverage());
         tour.setWeatherWindSpeed(historicalWeatherData.getWindSpeed());
         tour.setWeatherWindDir(historicalWeatherData.getWindDirection());
         tour.setWeather(historicalWeatherData.getWeatherDescription());
         tour.setWeatherClouds(historicalWeatherData.getWeatherType());
      }

      TourManager.saveModifiedTours(selectedTours);
   }
}
