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
import net.tourbook.common.UI;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourLogManager;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;

public class ExternalDatabase {

   private static final IPreferenceStore _prefStore         = TourbookPlugin.getPrefStore();

   /**
    * Folder where the externl jdbc driver jar is stored. This folder is created when a driver jar
    * is downloaded.
    */
   private static final String           FOLDER_EXTERNAL_DB = "external-db";

   private static Path                   _externalDbPath;

// SET_FORMATTING_OFF

   // Source: https://mvnrepository.com/artifact/org.mariadb.jdbc/mariadb-java-client
   //         implementation("org.mariadb.jdbc:mariadb-java-client:3.5.11")
   private static String _jarDownloadUrl            = "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.11/mariadb-java-client-3.5.11.jar";
   private static String _jdbcDriver                 = "org.mariadb.jdbc.Driver";
   private static String _jdbcUrlWithoutDb           = "jdbc:mariadb://localhost:3306/";
   private static String _jdbcUrlWithDb              = _jdbcUrlWithoutDb + TourDatabase.SQL_DB_TOURBOOK;
   private static String _jdbcUrlAdditionalParameter = UI.EMPTY_STRING;
   private static String _dbUsername                 = "mt-user";
   private static String _dbPassword                 = "password";

// SET_FORMATTING_ON

   static {

      String instanceLocation = Platform.getInstanceLocation().getURL().getPath();

      if (instanceLocation.startsWith("/")) {
         instanceLocation = instanceLocation.substring(1);
      }

      _externalDbPath = Paths.get(instanceLocation, FOLDER_EXTERNAL_DB);
   }

   private static void checkAndCreateDb() throws SQLException {

      final String sql = "CREATE DATABASE IF NOT EXISTS " + TourDatabase.SQL_DB_TOURBOOK;

      try (Connection conn = DriverManager.getConnection(_jdbcUrlWithoutDb, _dbUsername, _dbPassword);
            Statement stmt = conn.createStatement()) {

         stmt.executeUpdate(sql);

         TourLogManager.log_DEFAULT("Database checked and verified/created successfully");
      }
   }

   /**
    * Download external db jar when it do not exist
    *
    * @return Returns the local jar path or <code>null</code> when not available
    *
    * @throws Exception
    */
   private static Path getJar() throws Exception {

      try {

         final URI uri = new URI(_jarDownloadUrl);
         final Path uriPath = Paths.get(uri.getPath());
         final String jarFileName = uriPath.getFileName().toString();

         final Path targetPath = _externalDbPath.resolve(jarFileName);

         // check if the file already exists locally
         if (Files.exists(targetPath)) {

            TourLogManager.log_DEFAULT("File exists locally: " + targetPath.toAbsolutePath());

            return targetPath;
         }

         if (getJar_Download(_jarDownloadUrl, targetPath)) {

            return targetPath;
         }

//      } catch (final IOException e) {
//
//         // Catches network drops, full hard drives, permission denied errors
//         System.err.println("Network or File error: " + e.getMessage());
//         TourLogManager.log_EXCEPTION_WithStacktrace(e);
//
//      } catch (final IllegalArgumentException e) {
//
//         // Catches completely broken URL syntax from URI.create()
//         System.err.println("Invalid URL syntax provided: " + e.getMessage());
//         TourLogManager.log_EXCEPTION_WithStacktrace(e);
//
//      } catch (final Exception e) {
//
//         // Safety net for any other unexpected runtime crashes
//         System.err.println("An unexpected error occurred: " + e.getMessage());
//
//         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      } finally {

      }

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

         TourLogManager.log_ERROR(message);

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

         TourLogManager.log_DEFAULT("Download complete! Saved to: " + targetPath.toAbsolutePath());

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
      final Class<?> driverClass = Class.forName(_jdbcDriver, true, classLoader);

      final Driver targetDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();

      // 3. Register the driver via the Shim wrapper
      DriverManager.registerDriver(new DriverShim(targetDriver));
   }

   static void restoreState() {

// SET_FORMATTING_OFF

      _jarDownloadUrl               = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JAR_URL);
      _jdbcDriver                   = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_JDBC_DRIVER);
      _jdbcUrlWithoutDb             = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_URL);
      _jdbcUrlAdditionalParameter   = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_URL_PARAMETER);
      _dbUsername                   = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_USERNAME);
      _dbPassword                   = _prefStore.getString(ITourbookPreferences.TOUR_DATABASE_EXTERNAL_DATABASE_PASSWORD);

      _jdbcUrlWithDb                = _jdbcUrlWithoutDb + TourDatabase.SQL_DB_TOURBOOK + _jdbcUrlAdditionalParameter;

// SET_FORMATTING_ON
   }

   static void test() {

//      final boolean isExternalDb = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);

      restoreState();

      try {

         final Path externalJar = getJar();

         if (externalJar != null) {

            registerDbDriver(externalJar);
            checkAndCreateDb();
            testDB();
         }

      } catch (final Exception e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      }
   }

   public static void testConnection() {

      restoreState();

      TourLogManager.log_TITLE("Testing connection: %s".formatted(_jdbcUrlWithDb));

      try {

         final Path externalJar = getJar();

         if (externalJar != null) {

            registerDbDriver(externalJar);
            checkAndCreateDb();
            testDB();
         }

      } catch (final Exception e) {

         TourLogManager.log_EXCEPTION_WithStacktrace(e);
      }
   }

   private static void testDB() {

      // Connect to the database and query the version
      try (Connection connection = DriverManager.getConnection(_jdbcUrlWithDb, _dbUsername, _dbPassword);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT VERSION()")) {

         TourLogManager.log_DEFAULT("Connected to the database successfully");

         if (resultSet.next()) {
            final String dbVersion = resultSet.getString(1);

            TourLogManager.log_DEFAULT("MariaDB Server Version: " + dbVersion);
         }

      } catch (final SQLException e) {

         TourLogManager.log_EXCEPTION_WithStacktrace("Database connection or query failed", e);
      }
   }
}
