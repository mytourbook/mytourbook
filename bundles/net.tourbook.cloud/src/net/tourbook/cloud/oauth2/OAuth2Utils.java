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
package net.tourbook.cloud.oauth2;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.weather.WeatherUtils;

import org.json.JSONObject;

public class OAuth2Utils {

   public static String computeAccessTokenExpirationDate(final long accessTokenIssueDateTime,
                                                         final long accessTokenExpiresIn) {

      final long expireAt = accessTokenIssueDateTime + accessTokenExpiresIn;

      return (expireAt == 0) ? UI.EMPTY_STRING : TimeTools.getUTCISODateTime(expireAt);
   }

   public static URI createOAuthPasseurUri(final String uriSuffix) {

      return URI.create(WeatherUtils.OAUTH_PASSEUR_APP_URL + uriSuffix);
   }

   public static String getTokens(final HttpClient httpClient,
                                  final String authorizationCode,
                                  final boolean isRefreshToken,
                                  final String refreshToken,
                                  final URI uri) {

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
            .uri(uri)
            .build();

      try {
         final HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

         final String responseBody = response.body();
         if (response.statusCode() == HttpURLConnection.HTTP_CREATED && StringUtils.hasContent(responseBody)) {
            return responseBody;
         } else {
            StatusUtil.logError(responseBody);
         }
      } catch (IOException | InterruptedException e) {
         StatusUtil.log(e);
         Thread.currentThread().interrupt();
      }

      return null;
   }

   /**
    * We consider that an access token is valid (non expired) if there are more
    * than 5 mins remaining until the actual expiration
    *
    * @return
    */
   public static boolean isAccessTokenValid(final long tokenExpirationDate) {

      return tokenExpirationDate - System.currentTimeMillis() - 300000 > 0;
   }
}
