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

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationAdapter;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog to display authorization page for an OAuth2 request
 */
public class OAuth2BrowserDialog extends Dialog {

   private static final String paramAccessToken = IOAuth2Constants.PARAM_ACCESS_TOKEN;

   private final String        url;

   private final String        redirectUri;

   private String              token;

   private String              response;

   /**
    * @param shell
    * @param url
    * @param redirectUri
    */
   public OAuth2BrowserDialog(final OAuth2Client client) {

      this(PlatformUI.getWorkbench()
            .getDisplay()
            .getActiveShell(), OAuth2Utils.getAuthorizeUrl(client), client.getRedirectUri());
   }

   /**
    * @param shell
    * @param url
    * @param parameterName
    * @param redirectUri
    */
   public OAuth2BrowserDialog(final Shell shell,
                              final String url,
                              final String redirectUri) {
      super(shell);
      this.url = url;
      this.redirectUri = redirectUri;
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {
      createButton(parent,
            IDialogConstants.CANCEL_ID,
            IDialogConstants.CANCEL_LABEL,
            false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {
      final Composite control = (Composite) super.createDialogArea(parent);

      final Composite displayArea = new Composite(control, SWT.NONE);
      GridLayoutFactory.fillDefaults().applyTo(displayArea);
      GridDataFactory.fillDefaults()
            .hint(600, 600)
            .grab(true, true)
            .applyTo(displayArea);

      final Browser browser = new Browser(displayArea, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(browser);
      browser.setUrl(url);
      browser.addLocationListener(new LocationAdapter() {

         @Override
         public void changing(final LocationEvent event) {
            if (!event.location.startsWith(redirectUri)) {
               return;
            }
            URI uri;
            try {
               uri = new URI(event.location);
            } catch (final URISyntaxException ignored) {
               return;
            }
            retrieveTokenFromResponse(uri);

            event.doit = false;
            close();
         }

      });
      getShell().setText(Messages.OAuth2BrowserDialog_Title);
      return control;
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      final String sectionName = getClass().getName() + ".dialogBounds"; //$NON-NLS-1$
      final IDialogSettings settings = OAuth2Plugin.getDefault()
            .getDialogSettings();
      IDialogSettings section = settings.getSection(sectionName);
      if (section == null) {
         section = settings.addNewSection(sectionName);
      }
      return section;
   }

   public String getResponse() {
      return response;
   }

   public String getToken() {
      return token;
   }

   @Override
   protected boolean isResizable() {
      return true;
   }

   private void retrieveTokenFromResponse(final URI uri) {

      final char[] separators = { '#', '&' };

      response = uri.toString();

      final List<NameValuePair> params = URLEncodedUtils.parse(response, StandardCharsets.UTF_8, separators);
      for (final NameValuePair param : params) {
         if (paramAccessToken.equals(param.getName())) {
            token = param.getValue();
            break;
         }
      }
   }
}
