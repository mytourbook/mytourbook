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

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.LocalHostServer;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.web.WEB;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageDropbox extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   //SET_FORMATTING_OFF
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN  = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_AccessToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_BUTTON_AUTHORIZE   = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Button_Authorize;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Group_CloudAccount;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT    = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_ExpiresAt;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_RefreshToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE      = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_WebPage;
   //SET_FORMATTING_ON

   public static final String      ID            = "net.tourbook.cloud.PrefPageDropbox";       //$NON-NLS-1$

   public static final String      ClientId      = "vye6ci8xzzsuiao";                          //$NON-NLS-1$

   public static final int         CALLBACK_PORT = 4917;

   private IPreferenceStore        _prefStore    = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;
   private LocalHostServer         _server;
   /*
    * UI controls
    */
   private Group                   _group;
   private Label                   _labelAccessToken;
   private Label                   _labelAccessToken_Value;
   private Label                   _labelExpiresAt;
   private Label                   _labelExpiresAt_Value;
   private Label                   _labelRefreshToken;
   private Label                   _labelRefreshToken_Value;

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();

      _prefChangeListener = event -> {

         if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {

            Display.getDefault().syncExec(() -> {

               if (!event.getOldValue().equals(event.getNewValue())) {

                  _labelAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
                  _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
                        _prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME),
                        _prefStore.getInt(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN)));
                  _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

                  _group.redraw();

                  updateTokensInformationGroup();
               }

               if (_server != null) {
                  _server.stopCallBackServer();
               }
            });
         }
      };
   }

   private Composite createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      createUI_10_Authorize(parent);
      createUI_20_TokensInformation(parent);

      return parent;
   }

   private void createUI_10_Authorize(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         /*
          * Authorize button
          */
         final Button btnAuthorizeConnection = new Button(container, SWT.NONE);
         setButtonLayoutData(btnAuthorizeConnection);
         btnAuthorizeConnection.setText(PREFPAGE_CLOUDCONNECTIVITY_BUTTON_AUTHORIZE);
         btnAuthorizeConnection.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onClickAuthorize()));
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(btnAuthorizeConnection);
      }
   }

   private void createUI_20_TokensInformation(final Composite parent) {

      final int textWidth = new PixelConverter(parent).convertWidthInCharsToPixels(60);

      _group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_group);
      _group.setText(PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_group);
      {
         {
            final Label labelWebPage = new Label(_group, SWT.NONE);
            labelWebPage.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE);
            GridDataFactory.fillDefaults().applyTo(labelWebPage);

            final Link linkWebPage = new Link(_group, SWT.NONE);
            linkWebPage.setText(UI.LINK_TAG_START + Messages.PrefPage_CloudConnectivity_Dropbox_WebPage_Link + UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
                  Messages.PrefPage_CloudConnectivity_Dropbox_WebPage_Link)));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
         }
         {
            _labelAccessToken = new Label(_group, SWT.NONE);
            _labelAccessToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN);
            _labelAccessToken.setToolTipText(Messages.PrefPage_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _labelAccessToken_Value = new Label(_group, SWT.WRAP);
            _labelAccessToken_Value.setToolTipText(Messages.PrefPage_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().hint(textWidth, SWT.DEFAULT).applyTo(_labelAccessToken_Value);
         }
         {
            _labelRefreshToken = new Label(_group, SWT.NONE);
            _labelRefreshToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _labelRefreshToken_Value = new Label(_group, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(textWidth, SWT.DEFAULT).applyTo(_labelRefreshToken_Value);
         }
         {
            _labelExpiresAt = new Label(_group, SWT.NONE);
            _labelExpiresAt.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(_group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
      }
   }

   private String generateCodeChallenge(final String codeVerifier) {

      byte[] digest = null;
      try {
         final byte[] bytes = codeVerifier.getBytes(StandardCharsets.US_ASCII);
         final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256"); //$NON-NLS-1$
         messageDigest.update(bytes, 0, bytes.length);
         digest = messageDigest.digest();
      } catch (final NoSuchAlgorithmException e) {
         e.printStackTrace();
      }

      return digest == null ? null : Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
   }

   private String generateCodeVerifier() {

      final SecureRandom secureRandom = new SecureRandom();
      final byte[] codeVerifier = new byte[32];
      secureRandom.nextBytes(codeVerifier);
      return Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifier);
   }

   @Override
   public void init(final IWorkbench workbench) {
      //Not needed
   }

   @Override
   public boolean okToLeave() {

      if (_server != null) {
         _server.stopCallBackServer();
      }

      return super.okToLeave();
   }

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Dropbox app to have access
    * to their Dropbox account.
    */
   private void onClickAuthorize() {

      if (_server != null) {
         _server.stopCallBackServer();
      }

      final String codeVerifier = generateCodeVerifier();
      final String codeChallenge = generateCodeChallenge(codeVerifier);

      final DropboxTokensRetrievalHandler tokensRetrievalHandler = new DropboxTokensRetrievalHandler(codeVerifier);
      _server = new LocalHostServer(CALLBACK_PORT, "Dropbox", _prefChangeListener); //$NON-NLS-1$
      final boolean isServerCreated = _server.createCallBackServer(tokensRetrievalHandler);

      if (!isServerCreated) {
         return;
      }

      final URIBuilder authorizeUrlBuilder = new URIBuilder();
      authorizeUrlBuilder.setScheme("https"); //$NON-NLS-1$
      authorizeUrlBuilder.setHost("www.dropbox.com"); //$NON-NLS-1$
      authorizeUrlBuilder.setPath("/oauth2/authorize"); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_RESPONSE_TYPE, OAuth2Constants.PARAM_CODE);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_CLIENT_ID, ClientId);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_REDIRECT_URI, DropboxClient.DropboxCallbackUrl);
      authorizeUrlBuilder.addParameter("code_challenge", codeChallenge); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter("code_challenge_method", "S256"); //$NON-NLS-1$ //$NON-NLS-2$
      authorizeUrlBuilder.addParameter("token_access_type", "offline"); //$NON-NLS-1$ //$NON-NLS-2$
      try {
         final String authorizeUrl = authorizeUrlBuilder.build().toString();

         Display.getDefault().syncExec(() -> WEB.openUrl(authorizeUrl));
      } catch (final URISyntaxException e) {
         StatusUtil.log(e);
      }
   }

   @Override
   public boolean performCancel() {

      final boolean isCancel = super.performCancel();

      if (isCancel && _server != null) {
         _server.stopCallBackServer();
      }

      return isCancel;
   }

   @Override
   protected void performDefaults() {

      _labelAccessToken_Value.setText(_prefStore.getDefaultString(Preferences.DROPBOX_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(UI.EMPTY_STRING);
      _labelRefreshToken_Value.setText(_prefStore.getDefaultString(Preferences.DROPBOX_REFRESHTOKEN));

      updateTokensInformationGroup();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN, _labelAccessToken_Value.getText());
         _prefStore.setValue(Preferences.DROPBOX_REFRESHTOKEN, _labelRefreshToken_Value.getText());

         if (StringUtils.isNullOrEmpty(_labelExpiresAt_Value.getText())) {

            _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME, UI.EMPTY_STRING);
            _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN, UI.EMPTY_STRING);
         }

         if (_server != null) {
            _server.stopCallBackServer();
         }
      }

      return isOK;
   }

   private void restoreState() {

      _labelAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
            _prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME),
            _prefStore.getInt(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN)));
      _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

      updateTokensInformationGroup();
   }

   private void updateTokensInformationGroup() {

      final boolean isAuthorized = StringUtils.hasContent(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
   }

}
