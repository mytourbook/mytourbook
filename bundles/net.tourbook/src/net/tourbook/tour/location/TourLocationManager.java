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
package net.tourbook.tour.location;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.tourbook.application.ApplicationVersion;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourLogManager;
import net.tourbook.tour.TourManager;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.nebula.widgets.opal.duallist.mt.MT_DLItem;
import org.eclipse.osgi.util.NLS;

/**
 * Source: <a href=
 * "https://nominatim.org/release-docs/develop/api/Reverse/">https://nominatim.org/release-docs/develop/api/Reverse/</a>
 * <p>
 *
 * The main format of the reverse API is
 * <a href=
 * "https://nominatim.openstreetmap.org/reverse?lat=<value>&lon=<value>&<params>">https://nominatim.openstreetmap.org/reverse?lat=<value>&lon=<value>&<params></a>
 * <p>
 *
 * Limits and download policy:
 * <a href=
 * "https://operations.osmfoundation.org/policies/nominatim/">https://operations.osmfoundation.org/policies/nominatim/</a>
 * <p>
 *
 * Requested feature: <a href=
 * "https://github.com/mytourbook/mytourbook/issues/878">https://github.com/mytourbook/mytourbook/issues/878</a>
 * <p>
 */
public class TourLocationManager {

   private static final String SYS_PROP__LOG_ADDRESS_RETRIEVAL = "logAddressRetrieval";                                      //$NON-NLS-1$
   private static boolean      _isLogging_AddressRetrieval     = System.getProperty(SYS_PROP__LOG_ADDRESS_RETRIEVAL) != null;

   static {

      if (_isLogging_AddressRetrieval) {
         Util.logSystemProperty_IsEnabled(TourManager.class, SYS_PROP__LOG_ADDRESS_RETRIEVAL, "OSM address retrieval is logged"); //$NON-NLS-1$
      }
   }

   private static final String     SUB_TASK_MESSAGE         = "%d / %d - waited %d ms";
   private static final String     SUB_TASK_MESSAGE_SKIPPED = "%d / %d";                                             //$NON-NLS-1$

   private static final String     _userAgent               = "MyTourbook/" + ApplicationVersion.getVersionSimple(); //$NON-NLS-1$

   private static final HttpClient _httpClient              = HttpClient

         .newBuilder()
         .connectTimeout(Duration.ofSeconds(10))
         .build();

//   private static final ConcurrentHashMap<String, Image> _imageCache              = new ConcurrentHashMap<>();
//   private static final ConcurrentLinkedQueue<String>    _imageCacheFifo          = new ConcurrentLinkedQueue<>();
//   private static final ReentrantLock                    CACHE_LOCK               = new ReentrantLock();

   private static final StringBuilder             _displayNameBuffer   = new StringBuilder();
   private static final Set<String>               _usedDisplayNames    = new HashSet<>();

   /**
    * Contains all available profiles
    */
   private static final List<TourLocationProfile> _allLocationProfiles = new ArrayList<>();
   private static TourLocationProfile             _selectedProfile;

   /**
    * Zoom address detail
    *
    * 3 country
    * 5 state
    * 8 county
    * 10 city
    * 12 town / borough
    * 13 village / suburb
    * 14 neighbourhood
    * 15 any settlement
    * 16 major streets
    * 17 major and minor streets
    * 18 building
    */
   private static final int                       _zoomLevel           = 18;

   private static long                            _lastRetrievalTimeMS;

   /**
    * Append text to the display name
    *
    * @param text
    */
   private static void appendPart(final String text) {

      // prevent to show duplicated fields, this can happen when the "name" field contains also e.g. the road name
      if (_usedDisplayNames.contains(text)) {
         return;
      }

      if (_displayNameBuffer.length() > 0) {
         _displayNameBuffer.append(UI.SYMBOL_COMMA + UI.SPACE);
      }

      _displayNameBuffer.append(text);

      _usedDisplayNames.add(text);
   }

   public static String createLocationDisplayName(final List<MT_DLItem> allSelectedItems) {

      // reset buffers
      _displayNameBuffer.setLength(0);
      _usedDisplayNames.clear();

      for (final MT_DLItem dlItem : allSelectedItems) {
         appendPart(dlItem.getText());
      }

      return _displayNameBuffer.toString();
   }

   /**
    * Creates the location name from different name parts.
    *
    * @return Returns an empty string when a display name not available
    */
   public static String createLocationDisplayName(final OSMLocation osmLocation) {

      if (osmLocation == null || osmLocation.address == null) {
         return UI.EMPTY_STRING;
      }

      final OSMAddress address = osmLocation.address;

      /**
       * Places are sorted by number of inhabitants, only some or nothing are available
       *
       * place = city,
       * place = town,
       * place = village,
       * place = hamlet
       * place = isolated_dwelling
       *
       * https://wiki.openstreetmap.org/wiki/Key:place
       */

      // reset buffers
      _displayNameBuffer.setLength(0);
      _usedDisplayNames.clear();

      // OSM name
      final String osmName = osmLocation.name;

      if (osmName != null && osmName.length() > 0) {
         appendPart(osmName);
      }

      // city
      final String formattedCity = getCustom_City_Largest(address);
      if (formattedCity != null) {
         appendPart(formattedCity);
      }

      // POIs
      final String adrAmenity = address.amenity;
      final String adrTourism = address.tourism;

      if (adrAmenity != null) {
         appendPart(adrAmenity);
      }

      if (adrTourism != null) {
         appendPart(adrTourism);
      }

      // street
      final String formattedStreet = getCustom_Street(address);
      if (formattedStreet != null) {
         appendPart(formattedStreet);
      }

      // state, country
      final String adrState = address.state;
      final String adrCountry = address.country;

      if (adrState != null) {
         appendPart(adrState);
      }

      if (adrCountry != null) {
         appendPart(adrCountry);
      }

      return _displayNameBuffer.toString();

//    return osmLocation.display_name;
   }

   static String getCustom_City_Largest(final OSMAddress address) {

      // SET_FORMATTING_OFF

      final String adrCity                = address.city;
      final String adrTown                = address.town;
      final String adrVillage             = address.village;
      final String adrHamlet              = address.hamlet;
      final String adrIsolated_dwelling   = address.isolated_dwelling;

      String city = null;

      if (adrCity != null) {                    city = adrCity;               }
      else if (adrTown != null) {               city = adrTown;               }
      else if (adrVillage != null) {            city = adrVillage;            }
      else if (adrHamlet != null) {             city = adrHamlet;             }
      else if (adrIsolated_dwelling != null) {  city = adrIsolated_dwelling;  }

// SET_FORMATTING_ON

      return city;
   }

   static String getCustom_City_Smallest(final OSMAddress address) {

      // SET_FORMATTING_OFF

      final String adrCity                = address.city;
      final String adrTown                = address.town;
      final String adrVillage             = address.village;
      final String adrHamlet              = address.hamlet;
      final String adrIsolated_dwelling   = address.isolated_dwelling;

      String city = null;

      if (adrIsolated_dwelling != null) { city = adrIsolated_dwelling;  }
      else if (adrHamlet != null) {       city = adrHamlet;             }
      else if (adrVillage != null) {      city = adrVillage;            }
      else if (adrTown != null) {         city = adrTown;               }
      else if (adrCity != null) {         city = adrCity;               }

// SET_FORMATTING_ON

      return city;
   }

   static String getCustom_CityWithZip_Largest(final OSMAddress address) {

      final String city = getCustom_City_Largest(address);
      final String adrPostCode = address.postcode;

      if (city == null || adrPostCode == null) {
         return null;
      }

      final String adrCountryCode = address.country_code;

      if ("us".equals(adrCountryCode)) {

         // city + zip code

         return city + UI.SPACE + adrPostCode;

      } else {

         // zip code + city

         return adrPostCode + UI.SPACE + city;
      }
   }

   static String getCustom_CityWithZip_Smallest(final OSMAddress address) {

      final String city = getCustom_City_Smallest(address);
      final String adrPostCode = address.postcode;

      if (city == null || adrPostCode == null) {
         return null;
      }

      final String adrCountryCode = address.country_code;

      if ("us".equals(adrCountryCode)) {

         // city + zip code

         return city + UI.SPACE + adrPostCode;

      } else {

         // zip code + city

         return adrPostCode + UI.SPACE + city;
      }
   }

   static String getCustom_Street(final OSMAddress address) {

      final String adrRoad = address.road;
      final String adrHouseNumber = address.house_number;

      if (address.road == null || adrHouseNumber == null) {
         return null;
      }

      final String countryCode = address.country_code;

      String formattedStreet = UI.EMPTY_STRING;

      if ("us".equals(countryCode)) {

         if (adrHouseNumber != null) {
            formattedStreet += adrHouseNumber + UI.SPACE;
         }

         formattedStreet += adrRoad;

      } else {

         formattedStreet += adrRoad;

         if (adrHouseNumber != null) {
            formattedStreet += UI.SPACE + adrHouseNumber;
         }
      }

      if (formattedStreet.length() == 0) {
         return null;
      }

      return formattedStreet;
   }

   /**
    * Combine old and new location name
    *
    * @param oldLocation
    * @param newLocation
    *
    * @return
    */
   private static String getJoinedLocationNames(final String oldLocation, final String newLocation) {

      final boolean isOldLocation = oldLocation.length() > 0;
      final boolean isNewLocation = newLocation.length() > 0;

      if (isOldLocation && isNewLocation) {
         return oldLocation + UI.DASH_WITH_DOUBLE_SPACE + newLocation;
      }

      if (isNewLocation) {
         return newLocation;
      }

      if (isOldLocation) {
         return oldLocation;
      }

      return UI.EMPTY_STRING;
   }

   /**
    * Retrieve location data
    *
    * @param latitude
    * @param longitude
    *
    * @return
    */
   public static TourLocationData getLocationData(final double latitude,
                                                  final double longitude) {

      return getLocationData(latitude, longitude, null);
   }

   /**
    * Retrieve location data when not yet available
    *
    * @param latitude
    * @param longitude
    * @param existingLocationData
    *
    * @return
    */
   public static TourLocationData getLocationData(final double latitude,
                                                  final double longitude,
                                                  final TourLocationData existingLocationData) {

      if (existingLocationData != null) {
         return existingLocationData;
      }

      final TourLocationData tourLocationData = getName_10_RetrieveData(latitude, longitude, _zoomLevel);

      final OSMLocation osmLocation = tourLocationData.osmLocation = getName_20_DeserializeData(tourLocationData.downloadedData);

      if (_isLogging_AddressRetrieval && osmLocation != null) {

         System.out.println("Default name      " + osmLocation.display_name);

         if (osmLocation.name != null && osmLocation.name.length() > 0) {
            System.out.println("name              " + osmLocation.name);
         }

         System.out.println(" Waiting time     %d ms".formatted(tourLocationData.waitingTime));
         System.out.println(" Download time    %d ms".formatted(tourLocationData.downloadTime));

         if (osmLocation.address != null) {
            System.out.println(osmLocation.address.logAddress());
         }
      }

      return tourLocationData;
   }

   /**
    * @param latitude
    * @param longitudeSerie
    * @param zoomLevel
    *
    * @return Returns <code>null</code> or
    *
    *         <ol>
    *         <li>Location</li>
    *         <li>Request waiting time in ms</li>
    *         </ol>
    */
   private static TourLocationData getName_10_RetrieveData(final double latitude,
                                                           final double longitudeSerie,
                                                           final int zoomLevel) {

      final long now = System.currentTimeMillis();
      long waitingTime = now - _lastRetrievalTimeMS;

      if (waitingTime < 1000) {

         /*
          * Max requests are limited to 1 per second, we have to wait
          * https://operations.osmfoundation.org/policies/nominatim/
          */

         waitingTime = 1000 - waitingTime;

         try {

            Thread.sleep(waitingTime);

         } catch (final InterruptedException e) {
            StatusUtil.showStatus(e);
            Thread.currentThread().interrupt();
         }

      } else {

         // waiting time >= 1000 ms -> adjust value for log message

         waitingTime = 0;
      }

      final long retrievalStartTime = System.currentTimeMillis();
      _lastRetrievalTimeMS = retrievalStartTime;

//      System.out.println(UI.timeStamp() + " DOWNLOAD start - waiting time: " + waitingTime);
// TODO remove SYSTEM.OUT.PRINTLN

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

      String downloadedData = UI.EMPTY_STRING;

      try {

         final HttpRequest request = HttpRequest
               .newBuilder(URI.create(requestUrl))
               .header(WEB.HTTP_HEADER_USER_AGENT, _userAgent)
               .GET()
               .build();

         final HttpResponse<String> response = _httpClient.send(request, BodyHandlers.ofString());

         downloadedData = response.body();

         if (response.statusCode() != HttpURLConnection.HTTP_OK) {

            logError(downloadedData);

            return null;
         }

      } catch (final Exception ex) {

         logError(ex.getMessage());
         Thread.currentThread().interrupt();

         return null;
      }

      final long retrievalEndTime = System.currentTimeMillis();

      final long retrievalDuration = retrievalEndTime - retrievalStartTime;

      return new TourLocationData(downloadedData, retrievalDuration, waitingTime);
   }

   private static OSMLocation getName_20_DeserializeData(final String osmLocationString) {

      OSMLocation osmLocation = null;

      try {

         osmLocation = new ObjectMapper().readValue(osmLocationString, OSMLocation.class);

      } catch (final Exception e) {

         StatusUtil.logError(

               TourLocationManager.class.getSimpleName() + ".deserializeLocationData : " //$NON-NLS-1$
                     + "Error while deserializing the location JSON object : " //$NON-NLS-1$
                     + osmLocationString + UI.NEW_LINE + e.getMessage());
      }

      return osmLocation;
   }

   static List<TourLocationProfile> getProfiles() {

      return _allLocationProfiles;
   }

   /**
    * @return Returns the selected profile or <code>null</code> when a profile is not selected.
    */
   static TourLocationProfile getSelectedProfile() {
      return _selectedProfile;
   }

   private static void logError(final String exceptionMessage) {

      TourLogManager.log_ERROR(NLS.bind(
            "Error while retrieving location data: \"{1}\"", //$NON-NLS-1$
            exceptionMessage));
   }

   public static void setLocationNames(final List<TourData> requestedTours,
                                       final List<TourData> modifiedTours) {

      try {

         final IRunnableWithProgress runnable = new IRunnableWithProgress() {
            @Override
            public void run(final IProgressMonitor monitor)
                  throws InvocationTargetException, InterruptedException {

               final int numTours = requestedTours.size();
               int numWorked = 0;

               monitor.beginTask("Retrieving tour locations", numTours);

               for (final TourData tourData : requestedTours) {

                  if (monitor.isCanceled()) {
                     break;
                  }

                  final double[] latitudeSerie = tourData.latitudeSerie;
                  final double[] longitudeSerie = tourData.longitudeSerie;

                  if (latitudeSerie == null || latitudeSerie.length == 0) {

                     // needed data are not available

                     monitor.subTask(SUB_TASK_MESSAGE_SKIPPED.formatted(++numWorked, numTours));

                     continue;
                  }

                  final int lastIndex = latitudeSerie.length - 1;

                  final int numRequests = numTours * 2;

                  final TourLocationData locationStart = getLocationData(
                        latitudeSerie[0],
                        longitudeSerie[0],
                        tourData.osmLocation_Start);

                  monitor.subTask(SUB_TASK_MESSAGE.formatted(++numWorked, numRequests, locationStart.waitingTime));

                  final TourLocationData locationEnd = getLocationData(
                        latitudeSerie[lastIndex],
                        longitudeSerie[lastIndex],
                        tourData.osmLocation_End);

                  monitor.subTask(SUB_TASK_MESSAGE.formatted(++numWorked, numRequests, locationEnd.waitingTime));

                  final String oldTourStartPlace = tourData.getTourStartPlace();
                  final String oldTourEndPlace = tourData.getTourEndPlace();

                  final String osmStartLocation = createLocationDisplayName(locationStart.osmLocation);
                  final String osmEndLocation = createLocationDisplayName(locationEnd.osmLocation);

                  tourData.setTourStartPlace(getJoinedLocationNames(oldTourStartPlace, osmStartLocation));
                  tourData.setTourEndPlace(getJoinedLocationNames(oldTourEndPlace, osmEndLocation));

                  // keep location values
                  tourData.osmLocation_Start = locationStart;
                  tourData.osmLocation_End = locationEnd;

                  modifiedTours.add(tourData);
               }
            }
         };

         new ProgressMonitorDialog(TourbookPlugin.getAppShell()).run(true, true, runnable);

      } catch (final InvocationTargetException | InterruptedException e) {

         StatusUtil.showStatus(e);
         Thread.currentThread().interrupt();
      }
   }

   static void setSelectedProfile(final TourLocationProfile selectedProfile) {

      _selectedProfile = selectedProfile;
   }

}
