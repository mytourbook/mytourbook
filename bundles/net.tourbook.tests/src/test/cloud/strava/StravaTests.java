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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pgssoft.httpclient.HttpClientMock;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.strava.StravaTokensRetrievalHandler;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class StravaTests {

   private static final String     OAUTH_PASSEUR_APP_URL_TOKEN = OAuth2Utils.createOAuthPasseurUri("/strava/token").toString(); //$NON-NLS-1$
   private static IPreferenceStore _prefStore                  = Activator.getDefault().getPreferenceStore();

   private static final String     STRAVA_FILE_PATH            = FilesUtils.rootPath + "cloud/strava/files/";                   //$NON-NLS-1$
   private static Object           initialHttpClient;
   private static HttpClientMock   httpClientMock;

   @AfterAll
   static void cleanUp() {

      Initializer.setHttpClient(initialHttpClient);
   }

   @BeforeAll
   static void initAll() {

      initialHttpClient = Initializer.getInitialHttpClient();
      httpClientMock = Initializer.initializeHttpClientMock();
   }

   @Test
   void testTokenRetrieval() {

      final String passeurResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "PasseurResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(passeurResponse)
            .withStatus(201);

      Initializer.authorizeVendor(4918, new StravaTokensRetrievalHandler());

      assertEquals("4444444444444444444444444444444444444444", _prefStore.getString(Preferences.STRAVA_REFRESHTOKEN)); //$NON-NLS-1$
      assertEquals("4071156189000", //$NON-NLS-1$
            _prefStore.getString(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT));
      assertEquals("First Name", _prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME)); //$NON-NLS-1$
      assertEquals("12345678", _prefStore.getString(Preferences.STRAVA_ATHLETEID)); //$NON-NLS-1$
      assertEquals("8888888888888888888888888888888888888888", _prefStore.getString(Preferences.STRAVA_ACCESSTOKEN)); //$NON-NLS-1$
   }
}
