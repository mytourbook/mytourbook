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
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.strava.StravaTokensRetrievalHandler;

import org.eclipse.jface.preference.IPreferenceStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import utils.Comparison;
import utils.FilesUtils;

public class StravaTests {

   private static final String     OAUTH_PASSEUR_APP_URL_TOKEN = OAuth2Utils.createOAuthPasseurUri("/strava/token").toString(); //$NON-NLS-1$
   private static IPreferenceStore _prefStore                  = Activator.getDefault().getPreferenceStore();

   private static final String     STRAVA_FILE_PATH            = FilesUtils.rootPath + "cloud/strava/files/";                   //$NON-NLS-1$
   static HttpClientMock           httpClientMock;

   private static void authorize() throws IOException {

      // create the HttpServer
      final HttpServer httpServer = HttpServer.create(new InetSocketAddress(4918), 0);
      final StravaTokensRetrievalHandler tokensRetrievalHandler =
            new StravaTokensRetrievalHandler();
      httpServer.createContext("/", tokensRetrievalHandler); //$NON-NLS-1$

      // start the server
      httpServer.start();

      // authorize and retrieve the tokens
      final URL url = new URL("http://localhost:4918/?code=12345"); //$NON-NLS-1$
      final URLConnection conn = url.openConnection();
      new BufferedReader(new InputStreamReader(conn.getInputStream()));

      // stop the server
      httpServer.stop(0);
   }

   @BeforeAll
   static void initAll() throws NoSuchFieldException, IllegalAccessException {

      httpClientMock = new HttpClientMock();

      final Field field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, httpClientMock);
   }

   @Test
   void testTokenRetrieval() throws IOException {

      final String passeurResponse = Comparison.readFileContent(STRAVA_FILE_PATH
            + "PasseurResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            OAUTH_PASSEUR_APP_URL_TOKEN)
            .doReturn(passeurResponse)
            .withStatus(201);

      authorize();

      assertEquals("4444444444444444444", _prefStore.getString(Preferences.STRAVA_REFRESHTOKEN)); //$NON-NLS-1$
      assertEquals("1699400247000", //$NON-NLS-1$
            _prefStore.getString(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT));
      assertEquals("John Doe", _prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME)); //$NON-NLS-1$
      assertEquals("1234", _prefStore.getString(Preferences.STRAVA_ATHLETEID)); //$NON-NLS-1$
      assertEquals("4444444444444444444", _prefStore.getString(Preferences.STRAVA_ACCESSTOKEN)); //$NON-NLS-1$
   }
}
