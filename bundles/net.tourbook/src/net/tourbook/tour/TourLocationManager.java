/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tour;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;

import net.tourbook.application.ApplicationVersion;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.web.WEB;

import org.eclipse.osgi.util.NLS;

/**
 * Source: https://nominatim.org/release-docs/develop/api/Reverse/
 *
 * The main format of the reverse API is
 *
 * https://nominatim.openstreetmap.org/reverse?lat=<value>&lon=<value>&<params>
 *
 */
public class TourLocationManager {

   static final String             ID          = "net.tourbook.tour.TourLocationManager";                               //$NON-NLS-1$

   private static final String     _userAgent  = "MyTourbook/" + ApplicationVersion.getVersionSimple();                 //$NON-NLS-1$

   private static final HttpClient _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

   private static OSMLocation deserializeLocationData(final String osmLocationString) {

      OSMLocation osmLocation = null;

      try {

         osmLocation = new ObjectMapper().readValue(osmLocationString, OSMLocation.class);

      } catch (final Exception e) {

         StatusUtil.logError(
               DialogQuickEdit.class.getSimpleName() + ".deserializeLocationData : " //$NON-NLS-1$
                     + "Error while deserializing the location JSON object : " //$NON-NLS-1$
                     + osmLocationString + UI.NEW_LINE + e.getMessage());
      }

// TODO remove SYSTEM.OUT.PRINTLN
      System.out.println(osmLocationString);
      System.out.println(osmLocation);

      return osmLocation;
   }

   static String getLocationName(final double latitude, final double longitude) {

      // zoom  address detail
      //
      // 3     country
      // 5     state
      // 8     county
      // 10    city
      // 12    town / borough
      // 13    village / suburb
      // 14    neighbourhood
      // 15    any settlement
      // 16    major streets
      // 17    major and minor streets
      // 18    building

      int zoomLevel;
      zoomLevel = 3;
//    zoomLevel = 5;
//    zoomLevel = 8;
//    zoomLevel = 10;
//    zoomLevel = 12;
//    zoomLevel = 13;
//    zoomLevel = 14;
//    zoomLevel = 15;
//    zoomLevel = 16;
//    zoomLevel = 17;
      zoomLevel = 18;

      System.out.println("ZOOM: " + zoomLevel);
// TODO remove SYSTEM.OUT.PRINTLN

      final String retrievedLocationData = retrieveLocationData(latitude, longitude, zoomLevel);

      final OSMLocation osmLocation = deserializeLocationData(retrievedLocationData);

      final String locationName = osmLocation.display_name;

      return locationName;
   }

   private static void logError(final String exceptionMessage) {

      TourLogManager.log_ERROR(NLS.bind(
            "Error while retrieving location data: \"{1}\"", //$NON-NLS-1$
            exceptionMessage));
   }

   private static String retrieveLocationData(final double latitude, final double longitudeSerie, final int zoomLevel) {

//      BusyIndicator.showWhile(_parent.getDisplay(), () -> {
//
//      });

      final String requestUrl = UI.EMPTY_STRING

            + "https://nominatim.openstreetmap.org/reverse?format=json" //$NON-NLS-1$

            + "&lat=" + latitude //             //$NON-NLS-1$
            + "&lon=" + longitudeSerie //       //$NON-NLS-1$
            + "&zoom=" + zoomLevel //           //$NON-NLS-1$

            + "&addressdetails=1" //$NON-NLS-1$

//          + "&extratags=1" //$NON-NLS-1$
//          + "&namedetails=1" //$NON-NLS-1$
//          + "&layer=address,poi,railway,natural,manmade" //$NON-NLS-1$

//          + "&accept-language=1" //$NON-NLS-1$
      ;

      String responseData = UI.EMPTY_STRING;

      try {

         final HttpRequest request = HttpRequest
               .newBuilder(URI.create(requestUrl))
               .header(WEB.HTTP_HEADER_USER_AGENT, _userAgent)
               .GET()
               .build();

         final HttpResponse<String> response = _httpClient.send(request, BodyHandlers.ofString());

         responseData = response.body();

         if (response.statusCode() != HttpURLConnection.HTTP_OK) {

            logError(responseData);

            return UI.EMPTY_STRING;
         }

      } catch (final Exception ex) {

         logError(ex.getMessage());
         Thread.currentThread().interrupt();

         return UI.EMPTY_STRING;
      }

      return responseData;
   }
}
