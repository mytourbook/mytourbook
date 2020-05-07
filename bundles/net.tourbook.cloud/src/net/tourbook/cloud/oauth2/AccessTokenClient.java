/******************************************************************************
 *  Copyright (c) 2011 GitHub Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Kevin Sawicki (GitHub Inc.) - initial API and implementation
 *    https://github.com/kevinsawicki/eclipse-oauth2
 *****************************************************************************/
/*
 * Modified for MyTourbook by Frédéric Bard
 */
package net.tourbook.cloud.oauth2;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

/**
 *
 */
public class AccessTokenClient {

   private final OAuth2Client client;

   private String             response;

   /**
    * Create access token client
    *
    * @param client
    */
   public AccessTokenClient(final OAuth2Client client) {
      this.client = client;
   }

   /**
    * Execute request and return response
    *
    * @param target
    * @param request
    * @return response
    * @throws IOException
    */
   private String execute(final HttpHost target, final HttpRequest request)
         throws IOException {
      try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
         final HttpResponse response = httpClient.execute(target, request);
         return getToken(response.getEntity());
      }
   }

   /**
    * Get access token using given code
    *
    * @param code
    * @return fetched token
    * @throws IOException
    */
   public String fetch(final String code) throws IOException {
      final URI uri = URI.create(client.getAccessTokenUrl());
      final HttpHost target = URIUtils.extractHost(uri);
      final List<NameValuePair> params = getParams(code);
      final HttpPost request = new HttpPost(uri.getRawPath());
      if (params != null && !params.isEmpty()) {
         request.setEntity(new UrlEncodedFormEntity(params));
      }
      return execute(target, request);
   }

   /**
    * Get parameters for access token request
    * See {link https://www.dropbox.com/developers/documentation/http/documentation#oauth2-token}
    *
    * @param code
    * @return list of parameters
    */
   private List<NameValuePair> getParams(final String code) {
      final List<NameValuePair> params = new ArrayList<>();
      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_CLIENT_ID,
            client.getId()));
      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_CLIENT_SECRET,
            client.getSecret()));
      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_CODE, code));
      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_GRANT_TYPE, "authorization_code")); //$NON-NLS-1$
      params.add(new BasicNameValuePair(IOAuth2Constants.PARAM_REDIRECT_URI, client.getRedirectUri()));
      return params;
   }

   public String getResponse() {
      return response;
   }

   /**
    * Get token from response entity
    *
    * @param entity
    * @return token or null if not present in given entity
    * @throws IOException
    */
   private String getToken(final HttpEntity entity) throws IOException {
      response = EntityUtils.toString(entity);
      if (response == null || response.length() == 0) {
         return null;
      }

      final JSONObject currentSampleJson = new JSONObject(response);
      String token = null;
      try {
         token = currentSampleJson.get(IOAuth2Constants.PARAM_ACCESS_TOKEN).toString();
      } catch (final Exception e) {}

      return token;
   }
}
