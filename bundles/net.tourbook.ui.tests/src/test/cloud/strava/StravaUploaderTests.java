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
package cloud.strava;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.util.List;

import net.tourbook.Messages;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.tour.TourLogManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotCombo;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotSpinner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class StravaUploaderTests extends UITest {

   private static final String           OAUTH_PASSEUR_APP_URL_TOKEN = OAuth2Utils.createOAuthPasseurUri("/strava/token").toString();   //$NON-NLS-1$

   private static final IPreferenceStore _prefStore                  = Activator.getDefault().getPreferenceStore();
   private static final String           CLOUD_FILES_PATH            = Utils.WORKING_DIRECTORY + "\\src\\test\\cloud\\strava\\files\\"; //$NON-NLS-1$

   static HttpClientMock                 httpClientMock;
   static Object                         initialHttpClient;

   @AfterAll
   static void cleanUp() {

      Utils.setHttpClient(initialHttpClient);
   }

   @BeforeAll
   static void initAll() {

      //We set the Strava account information, otherwise the download can't
      //happen as the context menu will be grayed out.
      _prefStore.setValue(Preferences.STRAVA_SENDWEATHERDATA_IN_DESCRIPTION, true);
      _prefStore.setValue(Preferences.STRAVA_ADDWEATHERICON_IN_TITLE, true);
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN, "8888888888888888888888"); //$NON-NLS-1$
      _prefStore.setValue(Preferences.STRAVA_REFRESHTOKEN, "4444444444444444444444"); //$NON-NLS-1$
      //We set the access token issue date time in the past to trigger the retrieval
      //of a new token.
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, "973701086000"); //$NON-NLS-1$

      initialHttpClient = Utils.getInitialHttpClient();
      httpClientMock = Utils.initializeHttpClientMock();

      final String passeurResponse = Utils.readFileContent(CLOUD_FILES_PATH
            + "PasseurResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(passeurResponse)
            .withStatus(201);
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
   void testManualTourUpload() {

      final String stravaResponse = Utils.readFileContent(CLOUD_FILES_PATH
            + "ManualTour-StravaResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            "https://www.strava.com/api/v3/activities") //$NON-NLS-1$
            .doReturn(stravaResponse)
            .withStatus(201);

      Utils.createManualTour(bot);
      final SWTBot tourEditorViewBot = Utils.showView(bot, Utils.VIEW_NAME_TOUREDITOR).bot();

      tourEditorViewBot.comboBox(0).setText("Manual Tour"); //$NON-NLS-1$

      //final SWTBotText tourWeatherDescription = tourEditorViewBot.text(2);
      //assertNotNull(tourWeatherDescription);
      // tourWeatherDescription.setText("Partly cloudy"); //$NON-NLS-1$
      final SWTBotCombo tourClouds = tourEditorViewBot.comboBoxWithTooltip(Messages.tour_editor_label_clouds_Tooltip);
      assertNotNull(tourClouds);
      tourClouds.setSelection(2);
      final SWTBotSpinner tourAvgTemperature = tourEditorViewBot.spinnerWithTooltip(Messages.Tour_Editor_Label_Temperature_Avg_Tooltip);
      assertNotNull(tourAvgTemperature);
      tourAvgTemperature.setSelection(-10);
      final SWTBotSpinner tourWindChill = tourEditorViewBot.spinnerWithTooltip(Messages.Tour_Editor_Label_Temperature_WindChill_Tooltip);
      assertNotNull(tourWindChill);
      tourWindChill.setSelection(-60);
      final SWTBotSpinner tourHumidity = tourEditorViewBot.spinnerWithTooltip(Messages.Tour_Editor_Label_Humidity_Tooltip);
      assertNotNull(tourHumidity);
      tourHumidity.setSelection(78);
      final SWTBotCombo tourWindDirection = tourEditorViewBot.comboBoxWithTooltip(Messages.tour_editor_label_WindDirectionNESW_Tooltip);
      assertNotNull(tourWindDirection);
      tourWindDirection.setSelection(2);
      final SWTBotSpinner tourWindSpeed = tourEditorViewBot.spinnerWithTooltip(Messages.tour_editor_label_wind_speed_Tooltip);
      assertNotNull(tourWindSpeed);
      tourWindSpeed.setSelection(18);
      final SWTBotSpinner tourPrecipitation = tourEditorViewBot.spinnerWithTooltip(Messages.Tour_Editor_Label_Precipitation_Tooltip);
      assertNotNull(tourPrecipitation);
      tourPrecipitation.setSelection(300);
      final SWTBotSpinner tourSnowfall = tourEditorViewBot.spinnerWithTooltip(Messages.Tour_Editor_Label_Snowfall_Tooltip);
      assertNotNull(tourSnowfall);
      tourSnowfall.setSelection(130);
      //Save the tour
      bot.toolbarButtonWithTooltip(Utils.SAVE_MODIFIED_TOUR).click();

      // Act
      SWTBotTreeItem tour = Utils.selectManualTour(bot);
      tour.contextMenu(Messages.App_Action_Upload_Tour)
            .menu("&Strava").click(); //$NON-NLS-1$

      bot.sleep(5000);

      // Assert
      httpClientMock.verify().post(OAUTH_PASSEUR_APP_URL_TOKEN).called();
      httpClientMock
            .verify()
            .post("https://www.strava.com/api/v3/activities") //$NON-NLS-1$
            .withHeader(
                  "Authorization", //$NON-NLS-1$
                  OAuth2Constants.BEARER + "8888888888888888888888888888888888888888") //$NON-NLS-1$
            .withBody(equalTo(
                  "{\"distance\":0,\"trainer\":\"0\",\"start_date_local\":\"2005-01-01T05:00:00+01:00[Europe/Paris]\",\"name\":\"Manual Tour ⛅\",\"elapsed_time\":0,\"description\":\"⛅, ø -1°C, feels like -6°C, 18km/h from NNE, humidity 78%, precipitation 3.0mm, snowfall 1.3mm\",\"type\":\"Ride\"}")) //$NON-NLS-1$
            .called();
      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "message      = 1/1/2005, 5:00 AM -> Uploaded Activity Link: <br><a href=\"https://www.strava.com/activities/6468063624\">https://www.strava.com/activities/6468063624</a></br>\n"))); //$NON-NLS-1$

      // Cleanup
      tour = Utils.selectManualTour(bot);
      Utils.deleteTour(bot, tour);
   }

   @Test
   void testTourUpload_Strava() {

      // Arrange
      final String stravaResponse = Utils.readFileContent(CLOUD_FILES_PATH
            + "LongsPeak-StravaResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            "https://www.strava.com/api/v3/uploads") //$NON-NLS-1$
            .doReturn(stravaResponse)
            .withStatus(201);

      // Select a tour
      Utils.showTourBookView(bot);
      final SWTBotTreeItem tour = Utils.getTour(bot);

      // Act
      tour.contextMenu(Messages.App_Action_Upload_Tour)
            .menu("&Strava").click(); //$NON-NLS-1$

      bot.sleep(5000);

      // Assert
      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "message      = 1/31/2021, 7:15 AM -> Upload Id: \"6877121234\". Creation Activity Status: \"Your activity is still being processed.\"\n")));//$NON-NLS-1$
   }
}
