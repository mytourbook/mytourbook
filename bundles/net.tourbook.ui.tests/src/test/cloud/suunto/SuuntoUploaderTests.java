/*******************************************************************************
 * Copyright (C) 2022, 2025 Frédéric Bard
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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.net.HttpURLConnection;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.tour.TourLogManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class SuuntoUploaderTests extends UITest {

   private static final String           CLOUD_FILES_PATH = Utils.WORKING_DIRECTORY + "\\src\\test\\cloud\\suunto\\files\\"; //$NON-NLS-1$
   private static final IPreferenceStore _prefStore       = Activator.getDefault().getPreferenceStore();
   static Object                         initialHttpClient;

   private static HttpClientMock         httpClientMock;
   private static final String           UPLOAD_PATH      = "/suunto/workout/upload";                                        //$NON-NLS-1$

   @AfterAll
   static void cleanUp() {

      Utils.setHttpClient(initialHttpClient);
   }

   @BeforeAll
   static void initAll() {

      //We set the Suunto account information, otherwise the upload can't
      //happen as the context menu will be grayed out.
      _prefStore.setValue(
            Preferences.getSuuntoAccessToken_Active_Person_String(),
            "8888888888888888888888888888888888888888"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoAccessTokenIssueDateTime_Active_Person_String(),
            "4071156189000"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoAccessTokenExpiresIn_Active_Person_String(),
            "12609"); //$NON-NLS-1$
      _prefStore.setValue(Preferences.getPerson_SuuntoAccessToken_String("0"), "access_token"); //$NON-NLS-1$ //$NON-NLS-2$
      _prefStore.setValue(Preferences.getPerson_SuuntoRefreshToken_String("0"), "refresh_token"); //$NON-NLS-1$ //$NON-NLS-2$

      initialHttpClient = Utils.getInitialHttpClient();
      httpClientMock = Utils.initializeHttpClientMock();
   }

   @BeforeEach
   void setUp() {
      Utils.clearTourLogView(bot);
   }

   @AfterEach
   void tearDown() {
      Utils.clearTourLogView(bot);
   }

   @Test
   @Tag("ExternalConnection")
   void testRouteUpload() {

      // Arrange
      final String workoutsResponse = Utils.readFileContent(CLOUD_FILES_PATH
            + "RouteUpload-Response.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAuth2Utils.createOAuthPasseurUri("/suunto/route/import").toString()) //$NON-NLS-1$
            .doReturn(workoutsResponse)
            .withStatus(HttpURLConnection.HTTP_CREATED);

      // Act
      // Select a tour with GPS coordinates
      Utils.showTourBookView(bot);
      SWTBotTreeItem tour = Utils.getTour(bot);
      tour.contextMenu(Messages.App_Action_Upload_Tour)
            .menu("S&uunto App (Routes)").click(); //$NON-NLS-1$

      bot.sleep(5000);

      // Assert
      httpClientMock.verify().post(OAuth2Utils.createOAuthPasseurUri("/suunto/route/import").toString()).called(); //$NON-NLS-1$

      List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "1/31/2021, 7:15 AM -> Uploaded Route Id: \"634f49bf87e35c5a61e64947\""))); //$NON-NLS-1$

      // Act
      // Select a tour without GPS coordinates
      Utils.showTourBookView(bot);
      tour = bot.tree().getTreeItem("2022   1").expand() //$NON-NLS-1$
            .getNode("Feb   1").expand().select().getNode("3").select(); //$NON-NLS-1$ //$NON-NLS-2$
      assertNotNull(tour);
      tour.contextMenu(Messages.App_Action_Upload_Tour)
            .menu("S&uunto App (Routes)").click(); //$NON-NLS-1$

      bot.sleep(5000);

      // Assert
      logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "2/3/2022, 1:51 AM -> GPS coordinates are missing"))); //$NON-NLS-1$
   }

   @Test
   void testWorkoutUpload() {

      // Arrange
      httpClientMock.onPost(
            OAuth2Utils.createOAuthPasseurUri(UPLOAD_PATH).toString())
            .doReturn(UI.EMPTY_STRING)
            .withStatus(HttpURLConnection.HTTP_BAD_REQUEST);

      // Act
      // Select a tour
      Utils.showTourBookView(bot);
      SWTBotTreeItem tour = Utils.getTour(bot);
      tour.contextMenu(Messages.App_Action_Upload_Tour)
            .menu("S&uunto App (Workouts)").click(); //$NON-NLS-1$

      bot.sleep(5000);

      // Assert
      httpClientMock.verify().post(OAuth2Utils.createOAuthPasseurUri(UPLOAD_PATH).toString()).called();

      List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "1/31/2021, 7:15 AM -> Error while uploading the workout of Id"))); //$NON-NLS-1$

      // Arrange
      final String workoutUploadInitializationResponse = Utils.readFileContent(CLOUD_FILES_PATH
            + "WorkoutUploadInitialization-Response.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAuth2Utils.createOAuthPasseurUri(UPLOAD_PATH).toString())
            .doReturn(workoutUploadInitializationResponse)
            .withStatus(HttpURLConnection.HTTP_OK);
      final String WorkoutUploadStatusCheckResponse = Utils.readFileContent(CLOUD_FILES_PATH
            + "WorkoutUploadStatusCheck-Response.json"); //$NON-NLS-1$
      httpClientMock.onGet(
            OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload/642c5admtced4c09af1c49e6").toString()) //$NON-NLS-1$
            .doReturn(WorkoutUploadStatusCheckResponse)
            .withStatus(HttpURLConnection.HTTP_OK);
      final String blobUrl =
            "https://askoworkout001.blob.core.windows.net/fit6/642c5admtced4c09af1c49e6?sv=2019-02-02&se=2023-04-05T05%3A14%3A03Z&sr=b&sp=racwd&sig=lLSJzHaMa6EEN%2FYdFQJyCDEBzO0LuM%2BTyWVt4Bf%2BmoU%3D"; //$NON-NLS-1$
      httpClientMock.onPut(
            blobUrl)
            .doReturn(UI.EMPTY_STRING)
            .withStatus(HttpURLConnection.HTTP_CREATED);

      // Act
      // Select a tour
      Utils.showTourBookView(bot);
      tour = Utils.getTour(bot);
      tour.contextMenu(Messages.App_Action_Upload_Tour)
            .menu("S&uunto App (Workouts)").click(); //$NON-NLS-1$

      bot.sleep(5000);

      // Assert
      httpClientMock.verify().post(OAuth2Utils.createOAuthPasseurUri(UPLOAD_PATH).toString()).called(2);
      httpClientMock.verify().put(blobUrl).called();
      httpClientMock.verify().get(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload/642c5admtced4c09af1c49e6").toString()).called(); //$NON-NLS-1$

      logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "1/31/2021, 7:15 AM -> Uploaded Workout Id: \"642c5admtced4c09af1c49e6\""))); //$NON-NLS-1$
   }
}
