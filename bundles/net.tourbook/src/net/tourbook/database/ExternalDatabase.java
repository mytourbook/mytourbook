/*******************************************************************************
 * Copyright (C) 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.database;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourLogManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;

public class ExternalDatabase {

   private static final IPreferenceStore _prefStore         = TourbookPlugin.getPrefStore();

   /**
    * Subfolder in the app instance location where the externl jdbc driver jar is stored. This
    * folder is created when a driver jar is downloaded and do not yet exist.
    */
   private static final String           FOLDER_EXTERNAL_DB = "external-db";

   private static Path                   _externalDbPath;

   private static String                 _configConnectionUrl;
   private static String                 _configConnectionUrl_Parameters;
   private static String                 _configDbName;
   private static String                 _configDbUsername;
   private static String                 _configDbPassword;
   private static String                 _configHibernateDialect;
   private static String                 _configJarDownloadUrl;
   private static String                 _configJdbcDriver;

   /**
    * Url to connect to the tour database
    */
   private static String                 _connectionUrl_Concatenated;

   static {

      String instanceLocation = Platform.getInstanceLocation().getURL().getPath();

      if (instanceLocation.startsWith("/")) {
         instanceLocation = instanceLocation.substring(1);
      }

      _externalDbPath = Paths.get(instanceLocation, FOLDER_EXTERNAL_DB);
   }

   private static void checkAndCreateDb() throws SQLException {

//      final String sql = "CREATE DATABASE IF NOT EXISTS " + TourDatabase.SQL_DB_TOURBOOK;
//
//      try (Connection conn = DriverManager.getConnection(_configConnectionUrl, _configDbUsername, _configDbPassword);
//            Statement stmt = conn.createStatement()) {
//
//         stmt.executeUpdate(sql);
//
//         TourLogManager.log_DEFAULT("Database checked and verified/created successfully");
//      }
   }

   /**
    * @return
    */
   public static boolean checkDatabaseExists() {

      boolean isDbAvailable = false;

      // Connect to the database server base URL (without specifying a specific database if possible)
      try (Connection conn = DriverManager.getConnection(_configConnectionUrl, _configDbUsername, _configDbPassword)) {

         TourLogManager.log_DEFAULT("Connected to sql server with: " + _configConnectionUrl);

         // Get database metadata
         final var metaData = conn.getMetaData();

         // For SQL Server, MySQL, PostgreSQL, "Catalogs" usually represent the databases
         try (ResultSet resultSet = metaData.getCatalogs()) {

            while (resultSet.next()) {

               // The first column contains the database/catalog name
               final String databaseName = resultSet.getString(1);

               if (databaseName.equalsIgnoreCase(_configDbName)) {
                  isDbAvailable = true;
                  break;
               }
            }
         }

         if (isDbAvailable == false) {

            // Fallback for databases where schemas represent separate DB-like boundaries (e.g., Oracle)
            try (ResultSet resultSet = metaData.getSchemas()) {

               while (resultSet.next()) {

                  final String schemaName = resultSet.getString(1);

                  if (schemaName.equalsIgnoreCase(_configDbName)) {
                     isDbAvailable = true;
                     break;
                  }
               }
            }
         }

      } catch (final SQLException e) {

         TourLogManager.log_EXCEPTION_WithStacktrace("Connection failed or metadata unavailable", e);

         return false;
      }

      if (isDbAvailable) {

         TourLogManager.log_DEFAULT("Database is available: " + _configDbName);

         return true;

      } else {

         TourLogManager.log_INFO("Database is not available: " + _configDbName);

         return false;
      }
   }

   /**
    * Download external db jar when it do not exist
    *
    * @return Returns the local jar file path or <code>null</code> when not available
    *
    * @throws Exception
    */
   private static Path getJar() throws Exception {

      try {

         final URI uri = new URI(_configJarDownloadUrl);
         final Path uriPath = Paths.get(uri.getPath());
         final String jarFileName = uriPath.getFileName().toString();

         final Path targetPath = _externalDbPath.resolve(jarFileName);

         // check if the file already exists locally
         if (Files.exists(targetPath)) {

            TourLogManager.log_DEFAULT("Jar file exists locally: " + targetPath.toAbsolutePath());

            return targetPath;
         }

         if (getJar_Download(_configJarDownloadUrl, targetPath)) {

            return targetPath;
         }

      } finally {}

      return null;
   }

   private static boolean getJar_Download(final String urlString,
                                          final Path targetPath) throws Exception {

      // 2. Ensure missing parent folders are created automatically
      final Path parentDir = targetPath.getParent();
      if (parentDir != null && !Files.exists(parentDir)) {
         Files.createDirectories(parentDir);
         TourLogManager.log_DEFAULT("Created missing directories: " + parentDir.toAbsolutePath());
      }

      // 3. Establish the network connection
      final URL url = URI.create(urlString).toURL();
      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      connection.setInstanceFollowRedirects(true);
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(15000);

      final int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {

         final String message = "Server returned HTTP %d: %s - %s".formatted(
               responseCode,
               connection.getResponseMessage(),
               urlString);

         throw new IOException(message);
      }

      // 4. Read content length for tracking progress
      final long fileSize = connection.getContentLengthLong();
      final String fileSizeText = fileSize > 0 ? (fileSize / 1024) + " KB" : "Unknown";
      TourLogManager.log_DEFAULT("Starting download... Total size: %s - %s".formatted(fileSizeText, urlString));

      // 5. Stream the data and calculate progress
      try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
            FileOutputStream out = new FileOutputStream(targetPath.toFile())) {

         final byte[] buffer = new byte[4096];
         int bytesRead;
         long totalBytesRead = 0;
         int lastPercent = -1;

         while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            totalBytesRead += bytesRead;

            if (fileSize > 0) {
               final int percent = (int) ((totalBytesRead * 100) / fileSize);
               if (percent != lastPercent) {
                  System.out.print("\rDownloading: " + percent + "%");
                  lastPercent = percent;
               }
            } else {
               System.out.print("\rDownloaded: " + (totalBytesRead / 1024) + " KB");
            }
         }

         TourLogManager.log_DEFAULT("Download complete, saved to: " + targetPath.toAbsolutePath());

         return true;

      } finally {

         connection.disconnect();
      }
   }

   /**
    * Registers the external db jar as new sql {@link Driver}
    *
    * @param externalJar
    *
    * @throws Exception
    */
   private static void registerDbDriver(final Path externalJar) throws Exception {

      // 1. Create a classloader targeting the downloaded file
      final URL[] allUrls = new URL[] {
            externalJar.toUri().toURL()
      };

      final URLClassLoader classLoader = new URLClassLoader(allUrls, ExternalDatabase.class.getClassLoader());

      // 2. Load the specific vendor driver class name
      final Class<?> driverClass = Class.forName(_configJdbcDriver, true, classLoader);

      final Driver targetDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();

      // 3. Register the driver via the Shim wrapper
      DriverManager.registerDriver(new DriverShim(targetDriver));

      TourLogManager.log_DEFAULT("JDBC driver is registered: " + _configJdbcDriver);
   }

   private static void restoreState() {

// SET_FORMATTING_OFF

      _configJarDownloadUrl            = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JAR_DOWNLOAD_URL);
      _configJdbcDriver                = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JDBC_DRIVER);
      _configConnectionUrl             = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PROTOCOL);
      _configConnectionUrl_Parameters  = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_CONNECTION_URL_PARAMETERS);
      _configDbUsername                = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_USERNAME);
      _configDbPassword                = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_PASSWORD);
      _configDbName                    = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_NAME);
      _configHibernateDialect          = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_HIBERNATE_DIALECT);

      _connectionUrl_Concatenated = _configConnectionUrl + _configDbName + _configConnectionUrl_Parameters;

// SET_FORMATTING_ON
   }

   public static void testConnection() {

      // get current config
      restoreState();

      TourLogManager.log_TITLE("Testing connection: %s".formatted(_connectionUrl_Concatenated));

      try {

         final Path externalJar = getJar();

         if (externalJar != null) {

            registerDbDriver(externalJar);

            if (checkDatabaseExists()) {

               testDB();
            }
         }

      } catch (final Exception e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      }
   }

   private static void testDB() throws SQLException {

      // Connect to the database and query the version
      try (Connection connection = DriverManager.getConnection(_connectionUrl_Concatenated, _configDbUsername, _configDbPassword);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT VERSION()")) {

         TourLogManager.log_DEFAULT("Connected successfully to the database: " + _connectionUrl_Concatenated);

         if (resultSet.next()) {

            final String dbVersion = resultSet.getString(1);

            TourLogManager.log_DEFAULT("Database server version: " + dbVersion);
         }

      } finally {}
   }
}
