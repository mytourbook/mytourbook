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

import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.strava.StravaUploader;
import net.tourbook.data.TourData;
import net.tourbook.tour.TourLogManager;

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

   private static final String HEROKU_APP_URL_TOKEN = OAuth2Constants.HEROKU_APP_URL + "/strava/token"; //$NON-NLS-1$

   private static final String STRAVA_FILE_PATH     =
         FilesUtils.rootPath + "cloud/strava/files/";                                                   //$NON-NLS-1$

   static HttpClientMock       httpClientMock;
   static StravaUploader       stravaUploader;

   private List<TourData>      selectedTours        = new ArrayList<>();

   @BeforeAll
   static void initAll() throws NoSuchFieldException, IllegalAccessException {

      httpClientMock = new HttpClientMock();
      final String passeurResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "PasseurResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            HEROKU_APP_URL_TOKEN)
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

            if ("Strava Tour Upload Summary".equals(shell.getText())) { //$NON-NLS-1$
               // Close the shell after it has finished initializing

               Display.getDefault().asyncExec(shell::close);
            }
         }
      });
   }

   @AfterEach
   public void cleanUpEach() {
      TourLogManager.clear();
      selectedTours.clear();
   }

   @Test
   void testManualTourUpload() {

      final String stravaResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "LongsPeak-StravaResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            "https://www.strava.com/api/v3/uploads") //$NON-NLS-1$
            .doReturn(stravaResponse)
            .withStatus(201);

      final TourData tour = Initializer.importTour();

      selectedTours.add(tour);
      stravaUploader.uploadTours(selectedTours);

      httpClientMock.verify().post(HEROKU_APP_URL_TOKEN).called();
      httpClientMock
            .verify()
            .post("https://www.strava.com/api/v3/uploads")
            .withHeader("Authorization", OAuth2Constants.BEARER + "8888888888888888888888888888888888888888")
            .called();

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "message      = 7/4/20, 5:00 AM -> Upload Id: \"6877121234\". Creation Activity Status: \"Your activity is still being processed.\"\n")));//$NON-NLS-1$
   }

   @Test
   void testTourUpload() {

      final String stravaResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "ManualTour-StravaResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            "https://www.strava.com/api/v3/activities") //$NON-NLS-1$
            .doReturn(stravaResponse)
            .withStatus(201);

      final TourData tour = Initializer.createManualTour();

      selectedTours.add(tour);
      stravaUploader.uploadTours(selectedTours);

      httpClientMock.verify().post(HEROKU_APP_URL_TOKEN).called();
      httpClientMock
            .verify()
            .post("https://www.strava.com/api/v3/activities")
            .withHeader("Authorization", OAuth2Constants.BEARER + "8888888888888888888888888888888888888888")
            .withBody(equalTo(
                  "{\"distance\":10,\"trainer\":\"0\",\"start_date_local\":\"2022-01-03T17:16:00Z[UTC]\",\"name\":\"Manual Tour\",\"elapsed_time\":3600,\"description\":\"\",\"type\":\"Run\"}"))
            .called();

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "message      = 1/3/22, 5:16 PM -> Uploaded Activity Link: <br><a href=\"https://www.strava.com/activities/6468063624\">https://www.strava.com/activities/6468063624</a></br>\n"))); //$NON-NLS-1$
   }
}
