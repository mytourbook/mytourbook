/*******************************************************************************
 * Copyright (C) 2020 Frédéric Bard
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

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.IOAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageDropbox extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String      ID         = "net.tourbook.cloud.PrefPageDropbox";       //$NON-NLS-1$

   public static final String      ClientId   = "vye6ci8xzzsuiao";                          //$NON-NLS-1$

   private IPreferenceStore        _prefStore = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;

   private HttpServer              _server;
   private ThreadPoolExecutor      _threadPoolExecutor;
   /*
    * UI controls
    */
   private Label                   _labelAccessToken;
   private Label                   _labelAccessToken_Value;
   private Label                   _labelExpiresAt;
   private Label                   _labelExpiresAt_Value;
   private Label                   _labelRefreshToken;
   private Label                   _labelRefreshToken_Value;

   private void addPrefListener() {

      _prefChangeListener = new IPropertyChangeListener() {
         @Override
         public void propertyChange(final PropertyChangeEvent event) {

            if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {

               Display.getDefault().syncExec(new Runnable() {
                  @Override
                  public void run() {

                     _labelAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
                     _labelExpiresAt_Value.setText(computeAccessTokenExpirationDate());
                     _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

                     stopCallBackServer();

                     updateTokensInformationGroup();
                  }
               });
            }
         }
      };

      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   private String computeAccessTokenExpirationDate() {

      return OAuth2Utils.constructLocalExpireAtDateTime(_prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME) + _prefStore.getInt(
            Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN));
   }

   private void createCallBackServer(final String codeVerifier) {

      if (_server != null) {
         stopCallBackServer();
      }

      try {
         _server = HttpServer.create(new InetSocketAddress("localhost", 8001), 0); //$NON-NLS-1$
         final TokensRetrievalHandler tokensRetrievalHandler = new TokensRetrievalHandler(codeVerifier);
         _server.createContext("/dropboxAuthorizationCode", tokensRetrievalHandler); //$NON-NLS-1$
         _threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

         _server.setExecutor(_threadPoolExecutor);

         _server.start();

         addPrefListener();

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   @Override
   protected Control createContents(final Composite parent) {

      final Composite ui = createUI(parent);

      restoreState();

      return ui;
   }

   private Composite createUI(final Composite parent) {

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
         btnAuthorizeConnection.setText(Messages.Pref_CloudConnectivity_Dropbox_Button_Authorize);
         btnAuthorizeConnection.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }
         });
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(btnAuthorizeConnection);
      }
   }

   private void createUI_20_TokensInformation(final Composite parent) {

      final PixelConverter pc = new PixelConverter(parent);

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(Messages.Pref_CloudConnectivity_Dropbox_Tokens_Information_Group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         {
            _labelAccessToken = new Label(group, SWT.NONE);
            _labelAccessToken.setText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Label);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _labelAccessToken_Value = new Label(group, SWT.WRAP);
            _labelAccessToken_Value.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelAccessToken_Value);
         }
         {
            _labelExpiresAt = new Label(group, SWT.NONE);
            _labelExpiresAt.setText(Messages.Pref_CloudConnectivity_Dropbox_ExpiresAt_Label);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
         {
            _labelRefreshToken = new Label(group, SWT.NONE);
            _labelRefreshToken.setText(Messages.Pref_CloudConnectivity_Dropbox_RefreshToken_Label);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _labelRefreshToken_Value = new Label(group, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelRefreshToken_Value);
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
   public void init(final IWorkbench workbench) {}

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Dropbox app to have access
    * to their Dropbox account.
    */
   private void onClickAuthorize() {

      final String codeVerifier = generateCodeVerifier();
      final String codeChallenge = generateCodeChallenge(codeVerifier);

      Display.getDefault().syncExec(new Runnable() {
         @Override
         public void run() {

            createCallBackServer(codeVerifier);

            WEB.openUrl(
                  "https://www.dropbox.com/oauth2/authorize?" + //$NON-NLS-1$
                        IOAuth2Constants.PARAM_CLIENT_ID + UI.SYMBOL_EQUAL + ClientId +
                        "&response_type=" + IOAuth2Constants.PARAM_CODE + //$NON-NLS-1$
                        "&" + IOAuth2Constants.PARAM_REDIRECT_URI + UI.SYMBOL_EQUAL + DropboxClient.DropboxCallbackUrl + //$NON-NLS-1$
                        "&code_challenge=" + codeChallenge + //$NON-NLS-1$
                        "&code_challenge_method=S256&token_access_type=offline"); //$NON-NLS-1$
         }
      });
   }

   @Override
   public boolean performCancel() {

      final boolean isCancel = super.performCancel();

      if (isCancel) {
         stopCallBackServer();
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

         stopCallBackServer();
      }

      return isOK;
   }

   private void restoreState() {

      _labelAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(computeAccessTokenExpirationDate());
      _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

      updateTokensInformationGroup();
   }

   private void stopCallBackServer() {

      if (_server != null) {
         _server.stop(0);
         _server = null;

         _prefStore.removePropertyChangeListener(_prefChangeListener);
      }
      if (_threadPoolExecutor != null) {
         _threadPoolExecutor.shutdownNow();
      }
   }

   private void updateTokensInformationGroup() {

      final boolean isAuthorized = StringUtils.hasContent(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
   }
}
