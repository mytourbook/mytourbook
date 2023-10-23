/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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
package database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.tourbook.common.UI;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.database.TourDataUpdate_051_to_052;

import org.junit.jupiter.api.Test;

import utils.Initializer;

public class TourDataUpdate_051_to_052Tests {

   @Test
   void testUpdateTourData_AirQuality() {

      final TourDataUpdate_051_to_052 tourDataUpdate_051_to_052 = new TourDataUpdate_051_to_052();

      final TourData tourData = Initializer.createManualTour();
      tourData.setWeather_AirQuality("Schön"); //$NON-NLS-1$
      boolean isUpdated = tourDataUpdate_051_to_052.updateTourData(tourData);

      assertTrue(isUpdated);
      assertEquals(IWeather.AIRQUALITY_ID_FAIR, tourData.getWeather_AirQuality());

      tourData.setWeather_AirQuality(null);
      isUpdated = tourDataUpdate_051_to_052.updateTourData(tourData);

      assertFalse(isUpdated);
      assertEquals(null, tourData.getWeather_AirQuality());

      tourData.setWeather_AirQuality("Bon"); //$NON-NLS-1$
      isUpdated = tourDataUpdate_051_to_052.updateTourData(tourData);

      assertTrue(isUpdated);
      assertEquals(IWeather.AIRQUALITY_ID_GOOD, tourData.getWeather_AirQuality());

      tourData.setWeather_AirQuality("Moderat"); //$NON-NLS-1$
      isUpdated = tourDataUpdate_051_to_052.updateTourData(tourData);

      assertTrue(isUpdated);
      assertEquals(IWeather.AIRQUALITY_ID_MODERATE, tourData.getWeather_AirQuality());

      tourData.setWeather_AirQuality("Äusserst Ungesund"); //$NON-NLS-1$
      isUpdated = tourDataUpdate_051_to_052.updateTourData(tourData);

      assertTrue(isUpdated);
      assertEquals(IWeather.AIRQUALITY_ID_POOR, tourData.getWeather_AirQuality());

      tourData.setWeather_AirQuality("Très mauvais"); //$NON-NLS-1$
      isUpdated = tourDataUpdate_051_to_052.updateTourData(tourData);

      assertTrue(isUpdated);
      assertEquals(IWeather.AIRQUALITY_ID_VERYPOOR, tourData.getWeather_AirQuality());

      tourData.setWeather_AirQuality(UI.ISO_8859_1);
      isUpdated = tourDataUpdate_051_to_052.updateTourData(tourData);

      assertFalse(isUpdated);
      assertEquals(UI.ISO_8859_1, tourData.getWeather_AirQuality());
   }
}
