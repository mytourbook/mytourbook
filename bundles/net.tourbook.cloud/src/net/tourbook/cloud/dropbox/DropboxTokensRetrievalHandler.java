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
package net.tourbook.cloud.dropbox;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.Tokens;
import net.tourbook.cloud.oauth2.TokensRetrievalHandler;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.preference.IPreferenceStore;

public class DropboxTokensRetrievalHandler extends TokensRetrievalHandler {

   private String           _codeVerifier;

   private IPreferenceStore _prefStore = Activator.getDefault().getPreferenceStore();

   public DropboxTokensRetrievalHandler(final String codeVerifier) {
      _codeVerifier = codeVerifier;
   }

   @Override
   public Tokens retrieveTokens(final String authorizationCode) {

      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         return new DropboxTokens();
      }

      return DropboxClient.getTokens(authorizationCode, false, UI.EMPTY_STRING, _codeVerifier);
   }

   @Override
   public void saveTokensInPreferences(final Tokens tokens) {

      if (!(tokens instanceof DropboxTokens) || StringUtils.isNullOrEmpty(tokens.getAccess_token())) {

         final String currentAccessToken = _prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN);
         _prefStore.firePropertyChangeEvent(Preferences.DROPBOX_ACCESSTOKEN,
               currentAccessToken,
               currentAccessToken);
         return;
      }

      final DropboxTokens dropboxTokens = (DropboxTokens) tokens;

      _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN, dropboxTokens.getExpires_in());
      _prefStore.setValue(Preferences.DROPBOX_REFRESHTOKEN, dropboxTokens.getRefresh_token());
      _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME, System.currentTimeMillis());

      //Setting it last so that we trigger the preference change when everything is ready
      _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN, dropboxTokens.getAccess_token());
   }

}
