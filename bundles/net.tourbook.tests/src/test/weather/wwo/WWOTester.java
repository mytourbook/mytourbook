/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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
package weather.wwo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Initializer;

public class WWOTester {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private static String                 API_KEY;

   @BeforeAll
   static void initAll() {

      API_KEY = Initializer.getCredential("WorldWeatherOnline"); //$NON-NLS-1$
      _prefStore.setValue(ITourbookPreferences.WEATHER_API_KEY, API_KEY);

   }

   /**
    * Regression test for the weather retrieval from World Weather Online.
    */
   @Test
   void testWeatherRetrieval() {

      //If the API key is not found, we just consider the unit tests successful for now
      //A possible more elegant solution
      //https://keyholesoftware.com/2018/02/12/disabling-filtering-tests-junit-5/
      if (StringUtils.isNullOrEmpty(API_KEY)) {
         assertTrue(true);
         return;
      }
      final TourData tour = Initializer.importTour();
      TourManager.retrieveWeatherData(tour);

      assertTrue(tour.isWeatherDataFromApi());
      assertEquals(16, tour.getAvgTemperature());
      assertEquals(9, tour.getWeatherWindSpeed());
      assertEquals(136, tour.getWeatherWindDir());
      assertEquals("Partly cloudy", tour.getWeather()); //$NON-NLS-1$
      assertEquals("weather-cloudy", tour.getWeatherClouds()); //$NON-NLS-1$
      assertEquals(50, tour.getWeather_Humidity());
      assertEquals(1.6, Math.round(tour.getWeather_Precipitation() * 10.0) / 10.0);
      assertEquals(1017, tour.getWeather_Pressure());
      assertEquals(19, tour.getWeather_Temperature_Max());
      assertEquals(8, tour.getWeather_Temperature_Min());
      assertEquals(16, tour.getWeather_Temperature_WindChill());
   }
}
