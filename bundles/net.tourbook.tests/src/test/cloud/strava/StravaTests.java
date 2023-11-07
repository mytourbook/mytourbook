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

import com.pgssoft.httpclient.HttpClientMock;

import java.lang.reflect.Field;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.strava.StravaUploader;
import net.tourbook.common.formatter.FormatManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.BeforeAll;

import utils.Comparison;
import utils.FilesUtils;

public class StravaTests {

   private static final String           OAUTH_PASSEUR_APP_URL_TOKEN = OAuth2Utils.createOAuthPasseurUri("/strava/token").toString(); //$NON-NLS-1$
   private static final IPreferenceStore _prefStore                  = Activator.getDefault().getPreferenceStore();

   private static final String           STRAVA_FILE_PATH            = FilesUtils.rootPath + "cloud/strava/files/";                   //$NON-NLS-1$

   static HttpClientMock                 httpClientMock;
   static StravaUploader                 stravaUploader;

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

      final Field field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);

      stravaUploader = new StravaUploader();

      Display.getDefault().addFilter(SWT.Activate, event -> {
         // Is this a Shell being activated?

         if (event.widget instanceof final Shell shell) {

            // Look at the shell title to see if it is the one we want

            if (Messages.Dialog_UploadToursToStrava_Title.equals(shell.getText())) {
               // Close the shell after it has finished initializing

               Display.getDefault().asyncExec(() -> shell.close());
            }
         }
      });

      FormatManager.updateDisplayFormats();
   }

}
