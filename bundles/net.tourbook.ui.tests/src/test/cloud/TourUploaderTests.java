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
package cloud;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pgssoft.httpclient.HttpClientMock;

import java.lang.reflect.Field;
import java.util.List;

import net.tourbook.Messages;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.tour.TourLogManager;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotTreeItem;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import utils.UITest;
import utils.Utils;

public class TourUploaderTests extends UITest {

   private static final IPreferenceStore _prefStore       = Activator.getDefault().getPreferenceStore();
   private static final String           CLOUD_FILES_PATH = Utils.WORKING_DIRECTORY + "\\src\\test\\cloud\\files\\"; //$NON-NLS-1$

   static HttpClientMock                 httpClientMock;

   static Object                         initialHttpClient;

   @AfterAll
   static void cleanUp() throws NoSuchFieldException, IllegalAccessException {

      final Field field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);
      field.set(null, initialHttpClient);
   }

   @BeforeAll
   static void initAll() throws NoSuchFieldException, IllegalAccessException {

      _prefStore.setValue(Preferences.STRAVA_SENDWEATHERDATA_IN_DESCRIPTION, true);
      _prefStore.setValue(Preferences.STRAVA_ADDWEATHERICON_IN_TITLE, true);
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN, "8888888888888888888888");
      _prefStore.setValue(Preferences.STRAVA_REFRESHTOKEN, "4444444444444444444444");
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, "4071156189000");

      httpClientMock = new HttpClientMock();

      final Field field = OAuth2Utils.class.getDeclaredField("httpClient"); //$NON-NLS-1$
      field.setAccessible(true);

      initialHttpClient = field.get(null);
      field.set(null, httpClientMock);
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
   void testTourUpload_Strava() {

      final String stravaResponse = Utils.readFileContent(CLOUD_FILES_PATH
            + "LongsPeak-StravaResponse.json"); //$NON-NLS-1$
      httpClientMock.onPost(
            "https://www.strava.com/api/v3/uploads") //$NON-NLS-1$
            .doReturn(stravaResponse)
            .withStatus(201);

      // Select a tour
      Utils.showTourBookView(bot);
      final SWTBotTreeItem tour = Utils.getTour(bot);
      tour.contextMenu(Messages.App_Action_Upload_Tour)
            .menu("&Strava").click();

      bot.sleep(5000);

      final List<?> logs = TourLogManager.getLogs();
      assertTrue(logs.stream().map(log -> log.toString()).anyMatch(log -> log.contains(
            "message      = 1/31/2021, 7:15 AM -> Upload Id: \"6877121234\". Creation Activity Status: \"Your activity is still being processed.\"\n")));//$NON-NLS-1$
   }
}
