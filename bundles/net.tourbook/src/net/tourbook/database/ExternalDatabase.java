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

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;

public class ExternalDatabase {

   private static final IPreferenceStore _prefStore        = TourbookPlugin.getPrefStore();

   private static Path                   _externalDbPath;

   private static String                 _jdbcUrlWithoutDb = "jdbc:mariadb://localhost:3306/";
   private static String                 _jdbcUrlWithDb    = _jdbcUrlWithoutDb + TourDatabase.SQL_DB_TOURBOOK;

   private static String                 _dbUsername       = "mt-user";
   private static String                 _dbPassword       = "password";

   static {

      String instanceLocation = Platform.getInstanceLocation().getURL().getPath();

      if (instanceLocation.startsWith("/")) {
         instanceLocation = instanceLocation.substring(1);
      }

      _externalDbPath = Paths.get(instanceLocation, "external-db");
   }

   private static void checkAndCreateDb() {

      final String sql = "CREATE DATABASE IF NOT EXISTS " + TourDatabase.SQL_DB_TOURBOOK;

      try (Connection conn = DriverManager.getConnection(_jdbcUrlWithoutDb, _dbUsername, _dbPassword);
            Statement stmt = conn.createStatement()) {

         stmt.executeUpdate(sql);
         System.out.println("Database checked and verified/created successfully.");

      } catch (final SQLException e) {

         e.printStackTrace();
      }
   }

   /**
    * Download external db jar when it do not exist
    *
    * @return Returns the local jar path or <code>null</code> when not available
    */
   private static Path downloadJar() {

      // Source: https://mvnrepository.com/artifact/org.mariadb.jdbc/mariadb-java-client
      //         implementation("org.mariadb.jdbc:mariadb-java-client:3.5.11")
      final String prefDownloadUrl = "https://repo1.maven.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.11/mariadb-java-client-3.5.11.jar";

      try {

         final URI uri = new URI(prefDownloadUrl);
         final Path uriPath = Paths.get(uri.getPath());
         final String jarFileName = uriPath.getFileName().toString();

         final Path targetPath = _externalDbPath.resolve(jarFileName);

         if (downloadJar_WithProgress(prefDownloadUrl, targetPath)) {

            return targetPath;
         }

      } catch (final IOException e) {

         // Catches network drops, full hard drives, permission denied errors
         System.err.println("Network or File error: " + e.getMessage());

      } catch (final IllegalArgumentException e) {

         // Catches completely broken URL syntax from URI.create()
         System.err.println("Invalid URL syntax provided: " + e.getMessage());

      } catch (final Exception e) {

         // Safety net for any other unexpected runtime crashes
         System.err.println("An unexpected error occurred: " + e.getMessage());
      }

      return null;
   }

   private static boolean downloadJar_WithProgress(final String urlString,
                                                   final Path targetPath) throws IOException {

      // 1. Check if the file already exists locally
      if (Files.exists(targetPath)) {
         System.out.println("File already exists locally: " + targetPath.toAbsolutePath());
         return true;
      }

      // 2. Ensure missing parent folders are created automatically
      final Path parentDir = targetPath.getParent();
      if (parentDir != null && !Files.exists(parentDir)) {
         Files.createDirectories(parentDir);
         System.out.println("Created missing directories: " + parentDir.toAbsolutePath());
      }

      // 3. Establish the network connection using URI to avoid deprecation
      // URI.create() parses the string safely, then converted to URL
      final URL url = URI.create(urlString).toURL();
      final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");

      connection.setInstanceFollowRedirects(true);
      connection.setConnectTimeout(10000);
      connection.setReadTimeout(15000);

      final int responseCode = connection.getResponseCode();
      if (responseCode != HttpURLConnection.HTTP_OK) {
         throw new IOException("Server returned HTTP " + responseCode + ": " + connection.getResponseMessage());
      }

      // 4. Read content length for tracking progress
      final long fileSize = connection.getContentLengthLong();
      final String fileSizeText = fileSize > 0 ? (fileSize / 1024) + " KB" : "Unknown";
      System.out.println("Starting download... Total size: %d - %s".formatted(fileSizeText, urlString));

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
         System.out.println("\nDownload complete! Saved to: " + targetPath.toAbsolutePath());

         return true;

      } finally {
         connection.disconnect();
      }
   }

   private static void registerDbDriver(final Path downloadJar) throws Exception {

      try {

         // 1. Create a classloader targeting the downloaded file
         final URL[] allUrls = new URL[] {
               downloadJar.toUri().toURL()
         };

         final URLClassLoader classLoader = new URLClassLoader(allUrls, ExternalDatabase.class.getClassLoader());

         // 2. Load the specific vendor driver class name
         final Class<?> driverClass = Class.forName("org.mariadb.jdbc.Driver", true, classLoader);

         final Driver targetDriver = (Driver) driverClass.getDeclaredConstructor().newInstance();

         // 3. Register the driver via your Shim wrapper
         DriverManager.registerDriver(new DriverShim(targetDriver));

      } catch (final SQLException e) {

         e.printStackTrace();
      }
   }

   static void test() {

//      final boolean isExternalDb = _prefStore.getBoolean(ITourbookPreferences.TOUR_DATABASE_IS_EXTERNAL);

      final Path externalJar = downloadJar();

      if (externalJar != null) {

         try {

            registerDbDriver(externalJar);
            checkAndCreateDb();
            testDB();

         } catch (final Exception e) {

            e.printStackTrace();
         }
      }
   }

   private static void testDB() {

      // 3. Connect to the database and query the version
      // Using try-with-resources automatically closes resources to prevent leaks
      try (Connection connection = DriverManager.getConnection(_jdbcUrlWithDb, _dbUsername, _dbPassword);
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT VERSION()")) {

         System.out.println("✅ Connected to the database successfully.");

         if (resultSet.next()) {
            final String dbVersion = resultSet.getString(1);
            System.out.println("📊 MariaDB Server Version: " + dbVersion);
         }

      } catch (final SQLException e) {
         System.err.println("❌ Database connection or query failed.");
         e.printStackTrace();
      }
   }
}
