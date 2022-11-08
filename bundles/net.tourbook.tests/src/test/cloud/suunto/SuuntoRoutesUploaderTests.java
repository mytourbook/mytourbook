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

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.suunto.SuuntoRoutesUploader;
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

public class SuuntoRoutesUploaderTests {

   private static final String           SUUNTO_FILE_PATH = FilesUtils.rootPath + "cloud/suunto/files/"; //$NON-NLS-1$
   private static final IPreferenceStore _prefStore       = Activator.getDefault().getPreferenceStore();

   static HttpClientMock                 httpClientMock;
   static SuuntoRoutesUploader           suuntoRoutesUploader;

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

      final Field field = SuuntoRoutesUploader.class.getDeclaredField("_httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      suuntoRoutesUploader = new SuuntoRoutesUploader();

      final Field activePersonField = suuntoRoutesUploader.getClass().getDeclaredField("_useActivePerson"); //$NON-NLS-1$
      activePersonField.setAccessible(true);
      activePersonField.set(suuntoRoutesUploader, true);

      Display.getDefault().addFilter(SWT.Activate, event -> {
         // Is this a Shell being activated?

         if (event.widget instanceof Shell) {
            final Shell shell = (Shell) event.widget;

            // Look at the shell title to see if it is the one we want

            if (Messages.Dialog_UploadRoutesToSuunto_Title.equals(shell.getText())) {
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
   void testTourUpload() {

      final String workoutsResponse = Comparison.readFileContent(SUUNTO_FILE_PATH
            + "RouteUpload-Response.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAuth2Utils.createOAuthPasseurUri("/suunto/route/import").toString()) //$NON-NLS-1$
            .doReturn(workoutsResponse)
            .withStatus(HttpURLConnection.HTTP_CREATED);

      final TourData tour = Initializer.importTour();
      suuntoRoutesUploader.uploadTours(Arrays.asList(tour));

      httpClientMock.verify().post(OAuth2Utils.createOAuthPasseurUri("/suunto/route/import").toString()).called(); //$NON-NLS-1$

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(Object::toString).anyMatch(log -> log.contains(
            "7/4/20, 5:00 AM -> Uploaded Route Id: \"634f49bf87e35c5a61e64947\""))); //$NON-NLS-1$
   }
}
