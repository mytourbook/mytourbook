/*******************************************************************************
 * Copyright (C) 2020, 2021 Frédéric Bard
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

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;

public class DropboxClient {

   private static HttpClient     _httpClient        = HttpClient.newBuilder().connectTimeout(Duration.ofMinutes(1)).build();
   private static DbxClientV2    _dropboxClient;

   private static final String   DropboxApiBaseUrl  = "https://api.dropboxapi.com";                                         //$NON-NLS-1$
   public static final String    DropboxCallbackUrl = "http://localhost:" + PrefPageDropbox.CALLBACK_PORT + "/";            //$NON-NLS-1$ //$NON-NLS-2$

   static final IPreferenceStore _prefStore         = Activator.getDefault().getPreferenceStore();

   static {
      final IPropertyChangeListener prefChangeListenerCommon = event -> {

         if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {

            // Re create the Dropbox client
            createDefaultDropboxClient();
         }
      };

      // register the listener
      _prefStore.addPropertyChangeListener(prefChangeListenerCommon);

      createDefaultDropboxClient();
   }

   /**
    * Downloads a remote Dropbox file to a local temporary location
    *
    * @param dropboxFilePath
    *           The Dropbox path of the file
    * @return The local path of the downloaded file
    */
   public static final Path CopyLocally(String dropboxFilePath) {

      if (StringUtils.isNullOrEmpty(dropboxFilePath)) {
         return null;
      }

      Path dropboxTemporaryDirectoryPath = null;

      //Creating a "Dropbox" folder in the system's temporary directory
      try {
         dropboxTemporaryDirectoryPath = Paths.get(FileUtils.getTempDirectoryPath(), "Dropbox"); //$NON-NLS-1$
         FileUtils.forceMkdir(dropboxTemporaryDirectoryPath.toFile());
      } catch (final IOException e) {
         StatusUtil.log(e);
         return null;
      }

      dropboxFilePath = dropboxFilePath.replace(UI.SYMBOL_BACKSLASH, "/"); //$NON-NLS-1$

      final String fileName = Paths.get(dropboxFilePath).getFileName().toString();
      final Path filePath = Paths.get(dropboxTemporaryDirectoryPath.toString(), fileName);

      //Downloading the file from Dropbox to the local disk
      try (OutputStream outputStream = new FileOutputStream(filePath.toString())) {

         _dropboxClient.files().download(dropboxFilePath).download(outputStream);

         return filePath;
      } catch (final DbxException | IOException e) {
         StatusUtil.log(e);
      }

      return null;
   }

   /**
    * Creates a Dropbox client with the access token from the preferences
    */
   private static void createDefaultDropboxClient() {

      final String accessToken = getValidTokens();

      _dropboxClient = createDropboxClient(accessToken);
   }

   /**
    * Creates a Dropbox client with a given access token.
    * This will happen, for example, when a user has just retrieved an access token
    * but has not saved it yet into the preferences but wants to access the Dropbox account already.
    *
    * @param accessToken
    * @return
    */
   private static final DbxClientV2 createDropboxClient(final String accessToken) {

      //Getting the current version of MyTourbook
      final Version version = FrameworkUtil.getBundle(DropboxClient.class).getVersion();

      final DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("mytourbook/" + version.toString().replace(".qualifier", UI.EMPTY_STRING)) //$NON-NLS-1$//$NON-NLS-2$
            .build();

      return new DbxClientV2(requestConfig, accessToken);
   }

   /**
    * Gets the default Dropbox client if an access token was not provided.
    * Otherwise, creates a temporary Dropbox client.
    *
    * @param accessToken
    * @return
    */
   public static final DbxClientV2 getDefault(final String accessToken) {

      if (StringUtils.isNullOrEmpty(accessToken)) {
         return _dropboxClient;
      }

      return createDropboxClient(accessToken);
   }

   public static final DropboxTokens getTokens(final String authorizationCode,
                                               final boolean isRefreshToken,
                                               final String refreshToken,
                                               final String codeVerifier) {

      final Map<String, String> data = new HashMap<>();
      data.put(OAuth2Constants.PARAM_CLIENT_ID, PrefPageDropbox.ClientId);

      String grantType;
      if (isRefreshToken) {
         data.put(OAuth2Constants.PARAM_REFRESH_TOKEN, refreshToken);
         grantType = OAuth2Constants.PARAM_REFRESH_TOKEN;
      } else {
         data.put("code_verifier", codeVerifier); //$NON-NLS-1$
         data.put(OAuth2Constants.PARAM_CODE, authorizationCode);
         grantType = OAuth2Constants.PARAM_AUTHORIZATION_CODE;
         data.put(OAuth2Constants.PARAM_REDIRECT_URI, DropboxCallbackUrl);
      }

      data.put(OAuth2Constants.PARAM_GRANT_TYPE, grantType);

      final HttpRequest request = HttpRequest.newBuilder()
            .header("Content-Type", "application/x-www-form-urlencoded") //$NON-NLS-1$ //$NON-NLS-2$
            .POST(ofFormData(data))
            .uri(URI.create(DropboxApiBaseUrl + "/oauth2/token"))//$NON-NLS-1$
            .build();

      DropboxTokens token = new DropboxTokens();
      try {
         final HttpResponse<String> response = _httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         if (response.statusCode() == HttpURLConnection.HTTP_OK && StringUtils.hasContent(response.body())) {
            token = new ObjectMapper().readValue(response.body(), DropboxTokens.class);

            return token;
         } else {
            StatusUtil.logError(response.body());
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return token;
   }

   public static final String getValidTokens() {

      if (StringUtils.isNullOrEmpty(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN))) {
         return UI.EMPTY_STRING;
      }

      if (!OAuth2Utils.isAccessTokenExpired(_prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME) + _prefStore.getInt(
            Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN) * 1000)) {
         return _prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN);
      }

      final DropboxTokens newTokens = getTokens(UI.EMPTY_STRING, true, _prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN), UI.EMPTY_STRING);

      if (StringUtils.hasContent(newTokens.getAccess_token())) {

         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN, newTokens.getExpires_in());
         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME, System.currentTimeMillis());
         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN, newTokens.getAccess_token());
         return newTokens.getAccess_token();
      }

      return UI.EMPTY_STRING;
   }

   private static final BodyPublisher ofFormData(final Map<String, String> parameters) {

      final StringBuilder result = new StringBuilder();

      for (final Map.Entry<String, String> parameter : parameters.entrySet()) {

         if (StringUtils.hasContent(result.toString())) {
            result.append(UI.SYMBOL_MNEMONIC);
         }

         final String encodedName = URLEncoder.encode(parameter.getKey(), StandardCharsets.UTF_8);
         final String encodedValue = URLEncoder.encode(parameter.getValue(), StandardCharsets.UTF_8);
         result.append(encodedName);
         if (StringUtils.hasContent(encodedValue)) {
            result.append(UI.SYMBOL_EQUAL);
            result.append(encodedValue);
         }
      }
      return HttpRequest.BodyPublishers.ofString(result.toString());
   }
}
