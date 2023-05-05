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
package net.tourbook.cloud.strava;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.Tokens;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;

public class StravaTokensRetrievalHandler extends TokensRetrievalHandler {

   private IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();

   @Override
   public Tokens retrieveTokens(final String authorizationCode) {

      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return new StravaTokens();
      }

      return StravaUploader.getTokens(authorizationCode, false, UI.EMPTY_STRING);
   }

   @Override
   public void saveTokensInPreferences(final Tokens tokens) {

      if (!(tokens instanceof StravaTokens) || StringUtils.isNullOrEmpty(tokens.getAccess_token())) {

         final String currentAccessToken = _prefStore.getString(Preferences.STRAVA_ACCESSTOKEN);
         _prefStore.firePropertyChangeEvent(Preferences.STRAVA_ACCESSTOKEN,
               currentAccessToken,
               currentAccessToken);
         return;
      }

      final StravaTokens stravaTokens = (StravaTokens) tokens;

      _prefStore.setValue(Preferences.STRAVA_REFRESHTOKEN, stravaTokens.getRefresh_token());
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, stravaTokens.getExpires_at());

      final Athlete athlete = stravaTokens.getAthlete();
      if (athlete != null) {
         _prefStore.setValue(Preferences.STRAVA_ATHLETEFULLNAME, athlete.getFirstName() + UI.SPACE1 + athlete.getLastName());
         _prefStore.setValue(Preferences.STRAVA_ATHLETEID, athlete.getId());
      }

      //Setting it last so that we trigger the preference change when everything is ready
      _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN, stravaTokens.getAccess_token());
   }
}
