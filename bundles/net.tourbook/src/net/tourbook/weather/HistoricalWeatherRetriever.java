/*******************************************************************************
 * Copyright (C) 2019, 2022 Frédéric Bard
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

import com.javadocmd.simplelatlng.LatLng;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

public abstract class HistoricalWeatherRetriever {

   public static HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
   public TourData          tour;
   public LatLng            searchAreaCenter;
   public long              tourEndTime;
   public long              tourMiddleTime;
   public long              tourStartTime;

   protected HistoricalWeatherRetriever(final TourData tourData) {
      tour = tourData;

      searchAreaCenter = WeatherUtils.determineWeatherSearchAreaCenter(tour);

      tourStartTime = tour.getTourStartTimeMS() / 1000;
      tourEndTime = tour.getTourEndTimeMS() / 1000;
      tourMiddleTime = tourStartTime + ((tourEndTime - tourStartTime) / 2);
   }

   /**
    * This method ensures the connection to the API can be made successfully.
    */
   public static void checkVendorConnection(final String vendorUrl, final String vendorName) {

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         try {

            final HttpRequest request = HttpRequest
                  .newBuilder(URI.create(vendorUrl.trim()))
                  .GET()
                  .build();

            final HttpResponse<String> response = httpClient.send(
                  request,
                  BodyHandlers.ofString());

            final int statusCode = response.statusCode();
            final String responseMessage = response.body();

            final String message = statusCode == HttpURLConnection.HTTP_OK
                  ? NLS.bind(
                        Messages.Pref_Weather_CheckHTTPConnection_OK_Message,
                        vendorName)
                  : NLS.bind(
                        Messages.Pref_Weather_CheckHTTPConnection_FAILED_Message,
                        new Object[] {
                              vendorUrl,
                              statusCode,
                              responseMessage });

            MessageDialog.openInformation(
                  Display.getCurrent().getActiveShell(),
                  Messages.Pref_Weather_CheckHTTPConnection_Message,
                  message);

         } catch (final IOException | InterruptedException e) {
            StatusUtil.log(e);
            Thread.currentThread().interrupt();
         }
      });
   }

   /**
    * Returns the fully detailed weather log as a human readable string
    * Example: 14h (Temperature 14C, feels like 12C, humidity 54% etc....)
    *
    * @param isCompressed
    *           When true, displays the weather data in the most compressed way
    *           in order to reduce its size as much as possible (example: hiding
    *           empty values, replacing new lines by ';' etc...
    */
   protected abstract String buildDetailedWeatherLog(final boolean isCompressed);

   private void logVendorError(final String exceptionMessage) {

      TourLogManager.subLog_ERROR(NLS.bind(
            Messages.Log_HistoricalWeatherRetriever_002_RetrievalError,
            TourManager.getTourDateTimeShort(tour),
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
