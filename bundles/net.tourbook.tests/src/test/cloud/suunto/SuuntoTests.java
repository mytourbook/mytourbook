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

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pgssoft.httpclient.HttpClientMock;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.suunto.SuuntoTokensRetrievalHandler;
import net.tourbook.common.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;
import utils.Initializer;

public class SuuntoTests {

   private static final String     OAUTH_PASSEUR_APP_URL_TOKEN = OAuth2Utils.createOAuthPasseurUri("/suunto/token").toString(); //$NON-NLS-1$
   private static final String     SUUNTO_FILE_PATH            = FilesUtils.rootPath + "cloud/suunto/files/";                   //$NON-NLS-1$
   private static IPreferenceStore _prefStore                  = Activator.getDefault().getPreferenceStore();
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

      final String tokenResponse = Comparison.readFileContent(SUUNTO_FILE_PATH
            + "Token-Response.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(tokenResponse)
            .withStatus(201);

      Initializer.authorizeVendor(4919, new SuuntoTokensRetrievalHandler(UI.EMPTY_STRING));

      assertEquals("8888888888888888888888888888888888888888", _prefStore.getString(Preferences.getPerson_SuuntoAccessToken_String(UI.EMPTY_STRING))); //$NON-NLS-1$
      assertEquals("8888888888888888888888888888888888888888", //$NON-NLS-1$
            _prefStore.getString(Preferences.getPerson_SuuntoRefreshToken_String(UI.EMPTY_STRING)));
      assertEquals("12609", _prefStore.getString(Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(UI.EMPTY_STRING))); //$NON-NLS-1$
   }

}
