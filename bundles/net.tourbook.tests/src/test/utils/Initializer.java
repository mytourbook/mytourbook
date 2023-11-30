/*******************************************************************************
 * Copyright (C) 2020, 2023 Frédéric Bard
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
package utils;

import com.pgssoft.httpclient.HttpClientMock;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.time.ZonedDateTime;
import java.util.HashMap;

import javax.persistence.Persistence;

import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.device.garmin.fit.FitDataReader;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

public class Initializer {

   public static void authorizeVendor(final int callBackPort, final TokensRetrievalHandler tokensRetrievalHandler) {

      try {
         // create the HttpServer
         HttpServer httpServer;

         httpServer = HttpServer.create(new InetSocketAddress(callBackPort), 0);

         httpServer.createContext("/", tokensRetrievalHandler); //$NON-NLS-1$

         // start the server
         httpServer.start();

         // authorize and retrieve the tokens
         final URL url = new URL("http://localhost:" + callBackPort + "/?code=12345"); //$NON-NLS-1$ //$NON-NLS-2$
         final URLConnection conn = url.openConnection();
         new BufferedReader(new InputStreamReader(conn.getInputStream()));

         // stop the server
         httpServer.stop(0);

      } catch (final IOException e) {
         StatusUtil.log(e);
      }
   }

   public static TourData createManualTour() {

      final TourData manualTour = new TourData();
      final ZonedDateTime zonedDateTime = ZonedDateTime.of(
            2022,
            1,
            3,
            17,
            16,
            0,
            0,
            TimeTools.UTC);
      manualTour.setTourStartTime(zonedDateTime);
      manualTour.setTourDistance(10);
      manualTour.setTourDeviceTime_Elapsed(3600);
      manualTour.setTourTitle("Manual Tour"); //$NON-NLS-1$

      final TourType tourType = new TourType();
      tourType.setName("Running"); //$NON-NLS-1$
      manualTour.setTourType(tourType);

      return manualTour;
   }

   public static Object getInitialHttpClient() {

      Field field = null;
      try {
         field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
         field.setAccessible(true);
         return field.get(null);
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.log(e);
      }

      return field;
   }

   public static TourData importTour() {

      return importTour_GPX(FilesUtils.rootPath + "/utils/files/LongsPeak-Manual.gpx"); //$NON-NLS-1$
   }

   public static TourData importTour_FIT(final String importFilePath) {

      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      final FitDataReader fitDataReader = new FitDataReader();

      fitDataReader.processDeviceData(importFilePath,
            null,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      return Comparison.retrieveImportedTour(newlyImportedTours);
   }

   public static TourData importTour_GPX(final String importFilePath) {

      final HashMap<Long, TourData> newlyImportedTours = new HashMap<>();
      final HashMap<Long, TourData> alreadyImportedTours = new HashMap<>();
      final GPXDeviceDataReader deviceDataReader = new GPXDeviceDataReader();
      final String testFilePath = FilesUtils.getAbsoluteFilePath(importFilePath);

      deviceDataReader.processDeviceData(testFilePath,
            null,
            alreadyImportedTours,
            newlyImportedTours,
            new ImportState_File(),
            new ImportState_Process());

      return Comparison.retrieveImportedTour(newlyImportedTours);
   }

   public static void initializeDatabase() {

      Persistence.createEntityManagerFactory("tourdatabase").createEntityManager(); //$NON-NLS-1$
   }

   public static HttpClientMock initializeHttpClientMock() {

      final HttpClientMock httpClientMock = new HttpClientMock();

      setHttpClient(httpClientMock);

      return httpClientMock;
   }

   public static void setHttpClient(final Object httpClient) {

      Field field;
      try {
         field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
         field.setAccessible(true);
         field.set(null, httpClient);
      } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
         StatusUtil.log(e);
      }
   }
}
