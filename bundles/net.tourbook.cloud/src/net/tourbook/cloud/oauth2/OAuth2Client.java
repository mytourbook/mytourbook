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

/**
 * OAuth2 client class.
 */
public class OAuth2Client {

   private String accessTokenUrl;

   private String authorizeUrl;

   private String id;

   private String redirectUri;

   private String secret;

   /**
    * @return accessTokenUrl
    */
   public String getAccessTokenUrl() {
      return accessTokenUrl;
   }

   /**
    * @return authorizeUrl
    */
   public String getAuthorizeUrl() {
      return authorizeUrl;
   }

   /**
    * @return id
    */
   public String getId() {
      return id;
   }

   /**
    * @return redirectUri
    */
   public String getRedirectUri() {
      return redirectUri;
   }

   /**
    * @return secret
    */
   public String getSecret() {
      return secret;
   }

   /**
    * @param accessTokenUrl
    */
   public void setAccessTokenUrl(final String accessTokenUrl) {
      this.accessTokenUrl = accessTokenUrl;
   }

   /**
    * @param authorizeUrl
    */
   public void setAuthorizeUrl(final String authorizeUrl) {
      this.authorizeUrl = authorizeUrl;
   }

   /**
    * @param id
    * @return this client
    */
   public OAuth2Client setId(final String id) {
      this.id = id;
      return this;
   }

   /**
    * @param redirectUri
    * @return this client
    */
   public OAuth2Client setRedirectUri(final String redirectUri) {
      this.redirectUri = redirectUri;
      return this;
   }

   /**
    * @param secret
    * @return this client
    */
   public OAuth2Client setSecret(final String secret) {
      this.secret = secret;
      return this;
   }
}
