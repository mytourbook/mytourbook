/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.cloud.suunto;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.oauth2.Tokens;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;
import org.json.JSONObject;

public class SuuntoTokensRetrievalHandler extends TokensRetrievalHandler {

   private static HttpClient       _httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(5)).build();

   private static IPreferenceStore _prefStore  = Activator.getDefault().getPreferenceStore();

   private String                  _selectedPersonId;

   protected SuuntoTokensRetrievalHandler(final String selectedPersonId) {

      _selectedPersonId = selectedPersonId;
   }

   public static String getAccessToken_ActivePerson() {

      return _prefStore.getString(Preferences.getSuuntoAccessToken_Active_Person_String());
   }

   public static String getAccessToken_AllPeople() {

      return _prefStore.getString(Preferences.getPerson_SuuntoAccessToken_String(UI.EMPTY_STRING));
   }

   public static String getDownloadFolder_ActivePerson() {

      return _prefStore.getString(Preferences.getSuuntoWorkoutDownloadFolder_Active_Person_String());
   }

   public static String getDownloadFolder_AllPeople() {

      return _prefStore.getString(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(UI.EMPTY_STRING));
   }

   public static String getRefreshToken_ActivePerson() {

      return _prefStore.getString(Preferences.getSuuntoRefreshToken_Active_Person_String());
   }

   public static String getRefreshToken_AllPeople() {

      return _prefStore.getString(Preferences.getPerson_SuuntoRefreshToken_String(UI.EMPTY_STRING));
   }

   public static SuuntoTokens getTokens(final String authorizationCode, final boolean isRefreshToken, final String refreshToken) {

      final JSONObject body = new JSONObject();
      String grantType;
      if (isRefreshToken) {
         body.put(OAuth2Constants.PARAM_REFRESH_TOKEN, refreshToken);
         grantType = OAuth2Constants.PARAM_REFRESH_TOKEN;
      } else {
         body.put(OAuth2Constants.PARAM_CODE, authorizationCode);
         grantType = OAuth2Constants.PARAM_AUTHORIZATION_CODE;
      }

      body.put(OAuth2Constants.PARAM_GRANT_TYPE, grantType);
      final HttpRequest request = HttpRequest.newBuilder()
            .header(OAuth2Constants.CONTENT_TYPE, "application/json") //$NON-NLS-1$
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .uri(URI.create(OAuth2Constants.HEROKU_APP_URL + "/suunto/token"))//$NON-NLS-1$
            .build();

      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(response.body())) {
            final SuuntoTokens token = new ObjectMapper().readValue(response.body(), SuuntoTokens.class);

            return token;
         } else {
            StatusUtil.logError(response.body());
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return null;
   }

   public static boolean getValidTokens(final boolean useActivePerson, final boolean useAllPeople) {

      if (!useActivePerson && !useAllPeople) {
         return false;
      }

      String suuntoAccessTokenIssueDateTime = Preferences.getSuuntoAccessTokenIssueDateTime_Active_Person_String();
      String suuntoAccessTokenExpiresIn = Preferences.getSuuntoAccessTokenExpiresIn_Active_Person_String();
      String suuntoRefreshToken = Preferences.getSuuntoRefreshToken_Active_Person_String();
      String suuntoAccessToken = Preferences.getSuuntoAccessToken_Active_Person_String();

      if (useAllPeople) {
         suuntoAccessTokenIssueDateTime = Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(UI.EMPTY_STRING);
         suuntoAccessTokenExpiresIn = Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(UI.EMPTY_STRING);
         suuntoRefreshToken = Preferences.getPerson_SuuntoRefreshToken_String(UI.EMPTY_STRING);
         suuntoAccessToken = Preferences.getPerson_SuuntoAccessToken_String(UI.EMPTY_STRING);
      }

      //if active person has no tokens and all people has, take the tokens from all people

      if (OAuth2Utils.isAccessTokenValid(
            _prefStore.getLong(suuntoAccessTokenIssueDateTime) +
                  _prefStore.getLong(suuntoAccessTokenExpiresIn) * 1000)) {
         return true;
      }

      final SuuntoTokens newTokens = getTokens(UI.EMPTY_STRING, true, _prefStore.getString(suuntoRefreshToken));

      boolean isTokenValid = false;
      if (newTokens != null) {

         _prefStore.setValue(suuntoAccessTokenExpiresIn, newTokens.getExpires_in());
         _prefStore.setValue(suuntoRefreshToken, newTokens.getRefresh_token());
         _prefStore.setValue(suuntoAccessTokenIssueDateTime, System.currentTimeMillis());
         _prefStore.setValue(suuntoAccessToken, newTokens.getAccess_token());
         isTokenValid = true;
      }

      return isTokenValid;
   }

   public static boolean isReady_ActivePerson() {

      return StringUtils.hasContent(getAccessToken_ActivePerson()) &&
            StringUtils.hasContent(getRefreshToken_ActivePerson()) &&
            StringUtils.hasContent(getDownloadFolder_ActivePerson());
   }

   public static boolean isReady_AllPeople() {

      return StringUtils.hasContent(getAccessToken_AllPeople()) &&
            StringUtils.hasContent(getRefreshToken_AllPeople()) &&
            StringUtils.hasContent(getDownloadFolder_AllPeople());
   }

   @Override
   public Tokens retrieveTokens(final String authorizationCode) {

      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return new SuuntoTokens();
      }

      return getTokens(authorizationCode, false, UI.EMPTY_STRING);
   }

   @Override
   public void saveTokensInPreferences(final Tokens tokens) {

      if (!(tokens instanceof SuuntoTokens) || StringUtils.isNullOrEmpty(tokens.getAccess_token())) {

         final String currentAccessToken = _prefStore.getString(Preferences.getPerson_SuuntoAccessToken_String(_selectedPersonId));
         _prefStore.firePropertyChangeEvent(Preferences.getPerson_SuuntoAccessToken_String(_selectedPersonId),
               currentAccessToken,
               currentAccessToken);
         return;
      }

      final SuuntoTokens suuntoTokens = (SuuntoTokens) tokens;

      _prefStore.setValue(Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(_selectedPersonId), suuntoTokens.getExpires_in());
      _prefStore.setValue(Preferences.getPerson_SuuntoRefreshToken_String(_selectedPersonId), suuntoTokens.getRefresh_token());
      _prefStore.setValue(Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(_selectedPersonId), System.currentTimeMillis());

      //Setting it last so that we trigger the preference change when everything is ready
      _prefStore.setValue(Preferences.getPerson_SuuntoAccessToken_String(_selectedPersonId), suuntoTokens.getAccess_token());
   }
}
