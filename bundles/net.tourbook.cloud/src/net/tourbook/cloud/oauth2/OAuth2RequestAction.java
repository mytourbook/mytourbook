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

import net.tourbook.common.util.StatusUtil;

import org.eclipse.jface.action.Action;

/**
 * Action to get an OAuth2 access token for a client
 */
public class OAuth2RequestAction extends Action {

   private final OAuth2Client client;

   private String             token;

   private String             response;

   /**
    * Create request for client and scope
    *
    * @param client
    * @param scope
    */
   public OAuth2RequestAction(final OAuth2Client client) {
      this.client = client;
   }

   /**
    * Get token
    *
    * @return token
    */
   public String getAccessToken() {
      return token;
   }

   /**
    * Get response
    *
    * @return response
    */
   public String getResponse() {
      return response;
   }

   @Override
   public void run() {
      final String code = OAuth2BrowserDialog.getCode(client);
      if (code != null) {
         try {
            final AccessTokenClient accessTokenClient = new AccessTokenClient(client);
            token = accessTokenClient.fetch(code);
            response = accessTokenClient.getResponse();
         } catch (final IOException e) {
            StatusUtil.log(e);
         }
      }
   }
}
