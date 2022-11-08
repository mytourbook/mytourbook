/*******************************************************************************
 * Copyright (C) 2022 Frédéric Bard
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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.strava.StravaUploader;
import net.tourbook.common.formatter.FormatManager;
import net.tourbook.common.weather.IWeather;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourLogManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class StravaUploaderTests {

   private static final String           OAUTH_PASSEUR_APP_URL_TOKEN = OAuth2Utils.createOAuthPasseurUri("/strava/token").toString(); //$NON-NLS-1$
   private static final IPreferenceStore _prefStore                  = Activator.getDefault().getPreferenceStore();

   private static final String           STRAVA_FILE_PATH            = FilesUtils.rootPath + "cloud/strava/files/";                   //$NON-NLS-1$

   static HttpClientMock                 httpClientMock;
   static StravaUploader                 stravaUploader;

   private List<TourData>                selectedTours               = new ArrayList<>();

   @BeforeAll
   static void initAll() throws NoSuchFieldException, IllegalAccessException {

      _prefStore.setValue(Preferences.STRAVA_SENDWEATHERDATA_IN_DESCRIPTION, true);
      _prefStore.setValue(Preferences.STRAVA_ADDWEATHERICON_IN_TITLE, true);

      httpClientMock = new HttpClientMock();
      final String passeurResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "PasseurResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(passeurResponse)
            .withStatus(201);

      final Field field = StravaUploader.class.getDeclaredField("_httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      stravaUploader = new StravaUploader();

      Display.getDefault().addFilter(SWT.Activate, event -> {
         // Is this a Shell being activated?

         if (event.widget instanceof Shell) {
            final Shell shell = (Shell) event.widget;

            // Look at the shell title to see if it is the one we want

            if (Messages.Dialog_UploadToursToStrava_Title.equals(shell.getText())) {
               // Close the shell after it has finished initializing

               Display.getDefault().asyncExec(shell::close);
            }
         }
      });

      FormatManager.updateDisplayFormats();
   }

   @AfterEach
   public void cleanUpEach() {
      TourLogManager.clear();
      selectedTours.clear();
   }

   @Test
   void testManualTourUpload() {

      final String stravaResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "ManualTour-StravaResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            "https://www.strava.com/api/v3/activities") //$NON-NLS-1$
            .doReturn(stravaResponse)
            .withStatus(201);

      final TourData tour = Initializer.createManualTour();
      tour.setWeather_Clouds(IWeather.WEATHER_ID_PART_CLOUDS);
      tour.setWeather("Partly cloudy"); //$NON-NLS-1$
      tour.setWeather_Temperature_Average(-1);
      tour.setWeather_Temperature_WindChill(-6);
      tour.setWeather_Humidity((short) 78);
      tour.setWeather_Wind_Speed(18);
      tour.setWeather_Wind_Direction(267);
      tour.setWeather_Precipitation(3);
      tour.setWeather_Snowfall(1.3f);

      selectedTours.add(tour);
      stravaUploader.uploadTours(selectedTours);

      httpClientMock.verify().post(OAUTH_PASSEUR_APP_URL_TOKEN).called();
      httpClientMock
            .verify()
            .post("https://www.strava.com/api/v3/activities") //$NON-NLS-1$
            .withHeader(
                  "Authorization", //$NON-NLS-1$
                  OAuth2Constants.BEARER + "8888888888888888888888888888888888888888") //$NON-NLS-1$
            .withBody(equalTo(
                  "{\"distance\":10,\"trainer\":\"0\",\"start_date_local\":\"2022-01-03T17:16:00Z[UTC]\",\"name\":\"Manual Tour ⛅\",\"elapsed_time\":3600,\"description\":\"⛅ Partly cloudy, -1°C, feels like -6°C, 18km/h from W, 78% humidity, precipitation 3.0mm, snowfall 1.3mm\",\"type\":\"Run\"}")) //$NON-NLS-1$
            .called();

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "message      = 1/3/22, 5:16 PM -> Uploaded Activity Link: <br><a href=\"https://www.strava.com/activities/6468063624\">https://www.strava.com/activities/6468063624</a></br>\n"))); //$NON-NLS-1$
   }

   @Test
   void testTourUpload() {

      final String stravaResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "LongsPeak-StravaResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            "https://www.strava.com/api/v3/uploads") //$NON-NLS-1$
            .doReturn(stravaResponse)
            .withStatus(201);

      final TourData tour = Initializer.importTour();

      selectedTours.add(tour);
      stravaUploader.uploadTours(selectedTours);

      httpClientMock.verify().post(OAUTH_PASSEUR_APP_URL_TOKEN).called();
      httpClientMock
            .verify()
            .post("https://www.strava.com/api/v3/uploads") //$NON-NLS-1$
            .withHeader(
                  "Authorization", //$NON-NLS-1$
                  OAuth2Constants.BEARER + "8888888888888888888888888888888888888888") //$NON-NLS-1$
            .called();

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "message      = 7/4/20, 5:00 AM -> Upload Id: \"6877121234\". Creation Activity Status: \"Your activity is still being processed.\"\n")));//$NON-NLS-1$
   }
}
