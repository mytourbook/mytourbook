/*******************************************************************************
 * Copyright (C) 2023 Frédéric Bard
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

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.suunto.SuuntoWorkoutsUploader;
import net.tourbook.common.UI;
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

public class SuuntoWorkoutUploaderTests {

   private static final String           SUUNTO_FILE_PATH = FilesUtils.rootPath + "cloud/suunto/files/"; //$NON-NLS-1$
   private static final IPreferenceStore _prefStore       = Activator.getDefault().getPreferenceStore();

   static HttpClientMock                 httpClientMock;
   static SuuntoWorkoutsUploader         suuntoWorkoutsUploader;

   @BeforeAll
   static void initAll() throws NoSuchFieldException, IllegalAccessException {

      //We set the Suunto account information, otherwise the upload can't
      //happen
      _prefStore.setValue(
            Preferences.getSuuntoAccessToken_Active_Person_String(),
            "8888888888888888888888888888888888888888"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoAccessTokenIssueDateTime_Active_Person_String(),
            "4071156189000"); //$NON-NLS-1$
      _prefStore.setValue(
            Preferences.getSuuntoAccessTokenExpiresIn_Active_Person_String(),
            "12609"); //$NON-NLS-1$

      httpClientMock = new HttpClientMock();

      final Field field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      suuntoWorkoutsUploader = new SuuntoWorkoutsUploader();

      final Field activePersonField = suuntoWorkoutsUploader.getClass().getDeclaredField("_useActivePerson"); //$NON-NLS-1$
      activePersonField.setAccessible(true);
      activePersonField.set(suuntoWorkoutsUploader, true);

      Display.getDefault().addFilter(SWT.Activate, event -> {
         // Is this a Shell being activated?

         if (event.widget instanceof Shell) {
            final Shell shell = (Shell) event.widget;

            // Look at the shell title to see if it is the one we want

            if (Messages.Dialog_UploadWorkoutsToSuunto_Title.equals(shell.getText())) {
               // Close the shell after it has finished initializing

               Display.getDefault().asyncExec(shell::close);
            }
         }
      });
   }

   @AfterEach
   void tearDown() {
      TourLogManager.clear();
   }

   @Test
   void testTourUpload() {

      final String workoutUploadInitializationResponse = Comparison.readFileContent(SUUNTO_FILE_PATH
            + "WorkoutUploadInitialization-Response.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload").toString()) //$NON-NLS-1$
            .doReturn(workoutUploadInitializationResponse)
            .withStatus(HttpURLConnection.HTTP_OK);
      final String WorkoutUploadStatusCheckResponse = Comparison.readFileContent(SUUNTO_FILE_PATH
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

      final TourData tour = Initializer.importTour();
      suuntoWorkoutsUploader.uploadTours(Arrays.asList(tour));

      httpClientMock.verify().post(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload").toString()).called(); //$NON-NLS-1$
      httpClientMock.verify().put(blobUrl).called();
      httpClientMock.verify().get(OAuth2Utils.createOAuthPasseurUri("/suunto/workout/upload/642c5admtced4c09af1c49e6").toString()).called(); //$NON-NLS-1$

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "7/4/2020, 5:00 AM -> Uploaded Workout Id: \"642c5admtced4c09af1c49e6\""))); //$NON-NLS-1$
   }
}
