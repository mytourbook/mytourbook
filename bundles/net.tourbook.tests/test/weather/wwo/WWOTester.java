package weather.wwo;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.TourData;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Initializer;

public class WWOTester {

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   @BeforeAll
   static void initAll() {

      _prefStore.setValue(ITourbookPreferences.WEATHER_API_KEY, "API_KEY_VALUE"); //$NON-NLS-1$

   }

   /**
    * Regression test for the weather retrieval from World Weather Online.
    */
   @Test
   void testWeatherRetrieval() {

      final TourData tour = Initializer.importTour();
      TourManager.retrieveWeatherData(tour);

      Assertions.assertTrue(tour.isWeatherDataFromApi());
      Assertions.assertEquals(16, tour.getAvgTemperature());
      Assertions.assertEquals(9, tour.getWeatherWindSpeed());
      Assertions.assertEquals(226, tour.getWeatherWindDir());
      Assertions.assertEquals("Patchy rain possible", tour.getWeather()); //$NON-NLS-1$
      Assertions.assertEquals("<not defined>", tour.getWeatherClouds()); //$NON-NLS-1$
      Assertions.assertEquals(55, tour.getWeather_Humidity());
      Assertions.assertEquals(6.8, Math.round(tour.getWeather_Precipitation() * 10.0) / 10.0);
      Assertions.assertEquals(1018, tour.getWeather_Pressure());
      Assertions.assertEquals(20, tour.getWeather_Temperature_Max());
      Assertions.assertEquals(9, tour.getWeather_Temperature_Min());
      Assertions.assertEquals(15, tour.getWeather_Temperature_WindChill());
   }
}
