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
 * OAuth2 constants
 */
public interface IOAuth2Constants {

   String PARAM_AUTHORIZATION_CODE = "authorization_code"; //$NON-NLS-1$

   String PARAM_ACCESS_TOKEN       = "access_token";       //$NON-NLS-1$

   String PARAM_REFRESH_TOKEN      = "refresh_token";      //$NON-NLS-1$

   String PARAM_CLIENT_ID          = "client_id";          //$NON-NLS-1$

   String PARAM_CLIENT_SECRET      = "client_secret";      //$NON-NLS-1$

   String PARAM_CODE               = "code";               //$NON-NLS-1$

   String PARAM_TOKEN              = "token";              //$NON-NLS-1$

   String PARAM_GRANT_TYPE         = "grant_type";         //$NON-NLS-1$

   String PARAM_REDIRECT_URI       = "redirect_uri";       //$NON-NLS-1$

   String PARAM_SCOPE              = "scope";              //$NON-NLS-1$

   String RESPONSE_TYPE            = "response_type";      //$NON-NLS-1$
}
