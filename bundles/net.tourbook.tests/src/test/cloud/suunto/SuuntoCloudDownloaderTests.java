/*******************************************************************************
 * Copyright (C) 2022, 2023 Frédéric Bard
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
package cloud.suunto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.suunto.SuuntoCloudDownloader;
import net.tourbook.cloud.suunto.SuuntoTokensRetrievalHandler;
import net.tourbook.common.UI;
import net.tourbook.tour.TourLogManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;

public class SuuntoCloudDownloaderTests {

   private static final String           OAUTH_PASSEUR_APP_URL_TOKEN = OAuth2Utils.createOAuthPasseurUri("/suunto/token").toString(); //$NON-NLS-1$
   private static final String           SUUNTO_FILE_PATH            = FilesUtils.rootPath + "cloud/suunto/files/";                   //$NON-NLS-1$
   private static final IPreferenceStore _prefStore                  = Activator.getDefault().getPreferenceStore();

   static HttpClientMock                 httpClientMock;
   static SuuntoCloudDownloader          suuntoCloudDownloader;

   private static final String           _validTokenResponse         = Comparison.readFileContent(SUUNTO_FILE_PATH
         + "Token-Response.json");                                                                                                    //$NON-NLS-1$

   private static void authorize() throws IOException {

      // create the HttpServer
      final HttpServer httpServer = HttpServer.create(new InetSocketAddress(4919), 0);
      final SuuntoTokensRetrievalHandler tokensRetrievalHandler =
            new SuuntoTokensRetrievalHandler(UI.EMPTY_STRING);
      httpServer.createContext("/", tokensRetrievalHandler); //$NON-NLS-1$

      // start the server
      httpServer.start();

      // authorize and retrieve the tokens
      final URL url = new URL("http://localhost:4919/?code=12345"); //$NON-NLS-1$
      final URLConnection conn = url.openConnection();
      new BufferedReader(new InputStreamReader(conn.getInputStream()));

      // stop the server
      httpServer.stop(0);
   }

   @BeforeAll
   static void initAll() throws NoSuchFieldException, IllegalAccessException, IOException {

      //We set the Suunto account information, otherwise the download can't
      //happen
      _prefStore.setValue(
            Preferences.getSuuntoWorkoutDownloadFolder_Active_Person_String(),
            "./"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoWorkoutFilterStartDate_Active_Person_String(),
            "1293840000000"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoWorkoutFilterEndDate_Active_Person_String(),
            "1295049600000"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoUseWorkoutFilterStartDate_Active_Person_String(),
            true);
      _prefStore.setValue(
            Preferences.getSuuntoUseWorkoutFilterEndDate_Active_Person_String(),
            true);
      _prefStore.setValue(Preferences.SUUNTO_FILENAME_COMPONENTS,
            "{YEAR}{MONTH}{DAY}{USER_TEXT:-}{HOUR}{USER_TEXT:h}{MINUTE}{USER_TEXT:-}{SUUNTO_FILE_NAME}{USER_TEXT:-}{WORKOUT_ID}{USER_TEXT:-}{ACTIVITY_TYPE}{FIT_EXTENSION}"); //$NON-NLS-1$

      httpClientMock = new HttpClientMock();

      final Field field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(_validTokenResponse)
            .withStatus(201);

      suuntoCloudDownloader = new SuuntoCloudDownloader();

      authorize();

      Display.getDefault().addFilter(SWT.Activate, event -> {
         // Is this a Shell being activated?

         if (event.widget instanceof final Shell shell) {

            // Look at the shell title to see if it is the one we want

            if (Messages.Dialog_DownloadWorkoutsFromSuunto_Title.equals(shell.getText())) {
               // Close the shell after it has finished initializing

               Display.getDefault().asyncExec(shell::close);
            }
         }
      });
   }

   private void setTokenRetrievalDateInThePast() {
      _prefStore.setValue(
            Preferences.getSuuntoAccessTokenIssueDateTime_Active_Person_String(),
            "973701086000"); //$NON-NLS-1$
   }

   @BeforeEach
   void setUp() {

      httpClientMock.reset();
   }

   @AfterEach
   void tearDown() {

      TourLogManager.clear();
   }

   //We set the access token issue date time in the past to trigger the retrieval
   //of a new token.

   @Test
   void testTourDownload() {

      setTokenRetrievalDateInThePast();

      final String workoutsResponse = Comparison.readFileContent(SUUNTO_FILE_PATH
            + "Workouts-Response.json"); //$NON-NLS-1$
      httpClientMock.onGet(
            OAuth2Utils.createOAuthPasseurUri("/suunto/workouts?since=1293840000000&until=1295049600000").toString()) //$NON-NLS-1$
            .doReturn(workoutsResponse)
            .withStatus(200);

      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(_validTokenResponse)
            .withStatus(201);

      final String filename = "2011-01-13.fit"; //$NON-NLS-1$
      httpClientMock.onGet(
            OAuth2Utils.createOAuthPasseurUri("/suunto/workout/exportFit?workoutKey=601227a563c46e612c20b579").toString()) //$NON-NLS-1$
            .doReturn(UI.EMPTY_STRING)
            .withHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            .withStatus(200);

      suuntoCloudDownloader.downloadTours();

      httpClientMock.verify().post(OAUTH_PASSEUR_APP_URL_TOKEN).called();
      httpClientMock.verify().get(OAuth2Utils.createOAuthPasseurUri("/suunto/workouts?since=1293840000000&until=1295049600000").toString()).called(); //$NON-NLS-1$
      httpClientMock.verify().get(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/exportFit?workoutKey=601227a563c46e612c20b579").toString()) //$NON-NLS-1$
            .called();

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "601227a563c46e612c20b579 -> Workout Downloaded to the file:"))); //$NON-NLS-1$

      final String downloadedFilename = "20110112-19h02-2011-01-13-601227a563c46e612c20b579-running.fit"; //$NON-NLS-1$
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            downloadedFilename)));

      net.tourbook.common.util.FileUtils.deleteIfExists(Paths.get(downloadedFilename));
   }

   @Test
   void tourDownload_TokenRetrieval_NullResponse() {

      setTokenRetrievalDateInThePast();

      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(UI.EMPTY_STRING)
            .withStatus(201);
      suuntoCloudDownloader.downloadTours();

      httpClientMock.verify().post(OAUTH_PASSEUR_APP_URL_TOKEN).called();

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "Action aborted due to invalid tokens"))); //$NON-NLS-1$
   }
}
