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
package cloud.suunto;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import de.byteholder.geoclipse.map.UI;

import java.lang.reflect.Field;
import java.nio.file.Paths;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.suunto.SuuntoCloudDownloader;
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

public class SuuntoCloudDownloaderTests {

   private static final String           SUUNTO_FILE_PATH = FilesUtils.rootPath + "cloud/suunto/files/"; //$NON-NLS-1$
   private static final IPreferenceStore _prefStore       = Activator.getDefault().getPreferenceStore();

   static HttpClientMock                 httpClientMock;
   static SuuntoCloudDownloader          suuntoCloudDownloader;

   @BeforeAll
   static void initAll() throws NoSuchFieldException, IllegalAccessException {

      //We set the Suunto account information, otherwise the download can't
      //happen
      _prefStore.setValue(
            Preferences.getSuuntoAccessToken_Active_Person_String(),
            "8888888888888888888888888888888888888888"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoRefreshToken_Active_Person_String(),
            "8888888888888888888888888888888888888888"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoAccessTokenIssueDateTime_Active_Person_String(),
            "4071156189000"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoAccessTokenExpiresIn_Active_Person_String(),
            "12609"); //$NON-NLS-1$
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

      final Field field = SuuntoCloudDownloader.class.getDeclaredField("_httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      suuntoCloudDownloader = new SuuntoCloudDownloader();

      Display.getDefault().addFilter(SWT.Activate, event -> {
         // Is this a Shell being activated?

         if (event.widget instanceof Shell) {
            final Shell shell = (Shell) event.widget;

            // Look at the shell title to see if it is the one we want

            if ("Suunto App Workouts Download Summary".equals(shell.getText())) { //$NON-NLS-1$
               // Close the shell after it has finished initializing

               Display.getDefault().asyncExec(shell::close);
            }
         }
      });
   }

   @AfterEach
   public void cleanUpEach() {
      TourLogManager.clear();
   }

   @Test
   void testTourDownload() {

      final String workoutsResponse = Comparison.readFileContent(SUUNTO_FILE_PATH
            + "Workouts-Response.json"); //$NON-NLS-1$
      httpClientMock.onGet(
            OAuth2Constants.HEROKU_APP_URL + "/suunto/workouts?since=1293840000000&until=1295049600000") //$NON-NLS-1$
            .doReturn(workoutsResponse)
            .withStatus(200);

      final String filename = "2011-01-13.fit"; //$NON-NLS-1$
      httpClientMock.onGet(
            OAuth2Constants.HEROKU_APP_URL + "/suunto/workout/exportFit?workoutKey=601227a563c46e612c20b579") //$NON-NLS-1$
            .doReturn(UI.EMPTY_STRING)
            .withHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"") //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            .withStatus(200);

      suuntoCloudDownloader.downloadTours();

      httpClientMock.verify().get(OAuth2Constants.HEROKU_APP_URL + "/suunto/workouts?since=1293840000000&until=1295049600000").called(); //$NON-NLS-1$
      httpClientMock.verify().get(OAuth2Constants.HEROKU_APP_URL + "/suunto/workout/exportFit?workoutKey=601227a563c46e612c20b579").called(); //$NON-NLS-1$

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "601227a563c46e612c20b579 -> Workout Downloaded to the file:"))); //$NON-NLS-1$

      final String downloadedFilename = "20110112-19h02-2011-01-13-601227a563c46e612c20b579-running.fit"; //$NON-NLS-1$
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            downloadedFilename)));

      net.tourbook.common.util.FilesUtils.deleteIfExists(Paths.get(downloadedFilename));
   }

   /*
    * TODO
    * @Test
    * void testTourUpload() {
    * }
    */
}
