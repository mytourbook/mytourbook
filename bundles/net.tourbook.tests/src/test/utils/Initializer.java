/*******************************************************************************
 * Copyright (C) 2020, 2026 Frédéric Bard
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.device.garmin.fit.FitDataReader;
import net.tourbook.device.gpx.GPXDeviceDataReader;
import net.tourbook.importdata.ImportState_File;
import net.tourbook.importdata.ImportState_Process;

import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

public class Initializer {

   private static boolean _isInitDb;

   public static void authorizeVendor(final int callBackPort, final TokensRetrievalHandler tokensRetrievalHandler) {

      BufferedReader bufferedReader = null;
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
         bufferedReader = new BufferedReader(new InputStreamReader(conn.getInputStream()));

         // stop the server
         httpServer.stop(0);

      } catch (final IOException e) {
         StatusUtil.log(e);
      } finally {
         Util.closeReader(bufferedReader);
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

      if (_isInitDb) {
         return;
      }

      try {

         final HibernatePersistenceConfiguration cfg = TourDatabase.getInstance().getHibernateConfiguration();

         final List<String> managedClassNames = cfg.managedClasses().stream()
               .map(Class::getName)
               .toList();

// SET_FORMATTING_OFF

         final jakarta.persistence.spi.PersistenceUnitInfo pui = new jakarta.persistence.spi.PersistenceUnitInfo() {

            @Override public void addTransformer(final jakarta.persistence.spi.ClassTransformer transformer) {}
            @Override public boolean excludeUnlistedClasses()           { return true; }
            @Override public ClassLoader getClassLoader()               { return TourDatabase.class.getClassLoader(); }
            @Override public List<java.net.URL> getJarFileUrls()        { return Collections.emptyList(); }
            @Override public javax.sql.DataSource getJtaDataSource()    { return null; }
            @Override public List<String> getManagedClassNames()        { return managedClassNames; }
            @Override public List<String> getMappingFileNames()         { return Collections.emptyList(); }
            @Override public ClassLoader getNewTempClassLoader()        { return null; }
            @Override public javax.sql.DataSource getNonJtaDataSource() { return null; }
            @Override public String getPersistenceProviderClassName()   { return HibernatePersistenceProvider.class.getName(); }
            @Override public String getPersistenceUnitName()            { return cfg.name(); }
            @Override public java.net.URL getPersistenceUnitRootUrl()   { return null; }
            @Override public String getPersistenceXMLSchemaVersion()    { return "3.2"; }
            @Override public Properties getProperties() {
               final Properties p = new Properties();
               p.putAll(cfg.properties());
               return p;
            }
            @Override public List<String> getQualifierAnnotationNames() { return Collections.emptyList(); }
            @Override public String getScopeAnnotationName()            { return null; }
            @Override public jakarta.persistence.SharedCacheMode getSharedCacheMode()                    { return jakarta.persistence.SharedCacheMode.UNSPECIFIED; }
            @Override public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() { return jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL; }
            @Override public jakarta.persistence.ValidationMode getValidationMode()                      { return jakarta.persistence.ValidationMode.AUTO; }

         };

// SET_FORMATTING_ON

         final EntityManagerFactory emFactory = new HibernatePersistenceProvider().createContainerEntityManagerFactory(pui, cfg.properties());

         TourDatabase.getInstance().setEntityManagerFactory(emFactory);

         // test entity manager
         try (EntityManager em = TourDatabase.getInstance().getEntityManager()) {

            _isInitDb = true;
         }

      } catch (final Exception e) {

         e.printStackTrace();
      }
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
