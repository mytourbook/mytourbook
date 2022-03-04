/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
package net.tourbook.weather;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;

import org.eclipse.osgi.util.NLS;

public abstract class HistoricalWeatherRetriever {

   public static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
   public TourData          _tour;

   protected HistoricalWeatherRetriever(final TourData tourData) {
      _tour = tourData;
   }

   private void logVendorError(final String exceptionMessage) {

      TourLogManager.subLog_ERROR(NLS.bind(
            Messages.Log_HistoricalWeatherRetriever_001_RetrievalError,
            TourManager.getTourDateTimeShort(_tour),
            exceptionMessage));

   }

   public abstract boolean retrieveHistoricalWeatherData();

   public String sendWeatherApiRequest(final String weatherRequestWithParameters) {

      String weatherHistoryData = UI.EMPTY_STRING;

      try {
         // NOTE :
         // This error below keeps popping up RANDOMLY and as of today, I haven't found a solution:
         // java.lang.NoClassDefFoundError: Could not initialize class sun.security.ssl.SSLContextImpl$CustomizedTLSContext
         // 2019/06/20 : To avoid this issue, we are using the HTTP address of WWO and not the HTTPS.

         final HttpRequest request = HttpRequest.newBuilder(
               URI.create(weatherRequestWithParameters)).GET().build();

         final HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

         weatherHistoryData = response.body();

         if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logVendorError(response.body());
            return UI.EMPTY_STRING;
         }

      } catch (final Exception ex) {

         logVendorError(ex.getMessage());
         Thread.currentThread().interrupt();

         return UI.EMPTY_STRING;
      }

      return weatherHistoryData;
   }
}
