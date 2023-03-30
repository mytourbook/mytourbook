/*******************************************************************************
 * Copyright (C) 2021, 2023 Frédéric Bard
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.cloud.oauth2.Tokens;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;

public class SuuntoTokensRetrievalHandler extends TokensRetrievalHandler {

   private static IPreferenceStore _prefStore  = Activator.getDefault().getPreferenceStore();

   private String                  _selectedPersonId;

   public SuuntoTokensRetrievalHandler(final String selectedPersonId) {

      _selectedPersonId = selectedPersonId;
   }

   static String getAccessToken_ActivePerson() {

      return _prefStore.getString(Preferences.getSuuntoAccessToken_Active_Person_String());
   }

   static String getAccessToken_AllPeople() {

      return _prefStore.getString(Preferences.getPerson_SuuntoAccessToken_String(UI.EMPTY_STRING));
   }

   static String getDownloadFolder_ActivePerson() {

      return _prefStore.getString(Preferences.getSuuntoWorkoutDownloadFolder_Active_Person_String());
   }

   static String getDownloadFolder_AllPeople() {

      return _prefStore.getString(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(UI.EMPTY_STRING));
   }

   static String getRefreshToken_ActivePerson() {

      return _prefStore.getString(Preferences.getSuuntoRefreshToken_Active_Person_String());
   }

   static String getRefreshToken_AllPeople() {

      return _prefStore.getString(Preferences.getPerson_SuuntoRefreshToken_String(UI.EMPTY_STRING));
   }

   private static SuuntoTokens getTokens(final String authorizationCode, final boolean isRefreshToken, final String refreshToken) {

      final String responseBody = OAuth2Utils.getTokens(
            authorizationCode,
            isRefreshToken,
            refreshToken,
            OAuth2Utils.createOAuthPasseurUri("/suunto/token")); //$NON-NLS-1$

      SuuntoTokens suuntoTokens = null;
      try {
         suuntoTokens = new ObjectMapper().readValue(responseBody, SuuntoTokens.class);
      } catch (final IllegalArgumentException | JsonProcessingException e) {
         StatusUtil.log(e);
      }

      return suuntoTokens;
   }

   static boolean getValidTokens(final boolean useActivePerson, final boolean useAllPeople) {

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

   static boolean isDownloadReady_ActivePerson() {

      return isReady_ActivePerson() &&
            StringUtils.hasContent(getDownloadFolder_ActivePerson());
   }

   static boolean isDownloadReady_AllPeople() {

      return isReady_AllPeople() &&
            StringUtils.hasContent(getDownloadFolder_AllPeople());
   }

   static boolean isReady_ActivePerson() {

      return StringUtils.hasContent(getAccessToken_ActivePerson()) &&
            StringUtils.hasContent(getRefreshToken_ActivePerson());
   }

   static boolean isReady_AllPeople() {

      return StringUtils.hasContent(getAccessToken_AllPeople()) &&
            StringUtils.hasContent(getRefreshToken_AllPeople());
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
