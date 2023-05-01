/*******************************************************************************
 * Copyright (C) 2020, 2023 Frédéric Bard
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.LocalHostServer;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.web.WEB;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.nebula.widgets.opal.switchbutton.SwitchButton;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageDropbox extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   static final String             ID                    = "net.tourbook.cloud.PrefPageDropbox";       //$NON-NLS-1$

   static final String             ClientId              = "vye6ci8xzzsuiao";                          //$NON-NLS-1$

   static final int                CALLBACK_PORT         = 4917;

   private static final String     _dropbox_WebPage_Link = "https://www.dropbox.com";                  //$NON-NLS-1$

   private IPreferenceStore        _prefStore            = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;
   private LocalHostServer         _server;
   /*
    * UI controls
    */
   private Button                  _btnCleanup;
   private Group                   _group;
   private Label                   _labelAccessToken;
   private Label                   _labelExpiresAt;
   private Label                   _labelExpiresAt_Value;
   private Label                   _labelRefreshToken;
   private Link                    _linkRevokeAccess;
   private SwitchButton            _chkShowHideTokens;
   private Text                    _txtAccessToken_Value;
   private Text                    _txtRefreshToken_Value;

   @Override
   protected void createFieldEditors() {

      initUI();

      createUI();

      restoreState();

      _prefChangeListener = event -> {

         if (event.getProperty().equals(Preferences.DROPBOX_ACCESSTOKEN)) {

            Display.getDefault().syncExec(() -> {

               if (!event.getOldValue().equals(event.getNewValue())) {

                  _txtAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
                  _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
                        _prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME),
                        _prefStore.getInt(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN)));
                  _txtRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

                  _group.redraw();

                  enableControls();
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
      createUI_100_AccountCleanup(parent);

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
         btnAuthorizeConnection.setText(Messages.PrefPage_CloudConnectivity_Button_Authorize);
         btnAuthorizeConnection.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onClickAuthorize()));
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(btnAuthorizeConnection);
      }
   }

   private void createUI_100_AccountCleanup(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         /*
          * Clean-up button
          */
         _btnCleanup = new Button(container, SWT.NONE);
         _btnCleanup.setText(Messages.PrefPage_CloudConnectivity_Label_Cleanup);
         _btnCleanup.setToolTipText(Messages.PrefPage_CloudConnectivity_Label_Cleanup_Tooltip);
         _btnCleanup.addSelectionListener(widgetSelectedAdapter(selectionEvent -> performDefaults()));
         GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(true, true).applyTo(_btnCleanup);
      }
   }

   private void createUI_20_TokensInformation(final Composite parent) {

      final int textWidth = new PixelConverter(parent).convertWidthInCharsToPixels(60);

      _group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_group);
      _group.setText(Messages.PrefPage_CloudConnectivity_Group_CloudAccount);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_group);
      {
         {
            final Label labelWebPage = new Label(_group, SWT.NONE);
            labelWebPage.setText(Messages.PrefPage_CloudConnectivity_Label_WebPage);
            GridDataFactory.fillDefaults().applyTo(labelWebPage);

            final Link linkWebPage = new Link(_group, SWT.NONE);
            linkWebPage.setText(UI.LINK_TAG_START +
                  _dropbox_WebPage_Link +
                  UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
                  _dropbox_WebPage_Link)));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
         }
         {
            _labelAccessToken = new Label(_group, SWT.NONE);
            _labelAccessToken.setText(Messages.PrefPage_CloudConnectivity_Label_AccessToken);
            _labelAccessToken.setToolTipText(Messages.PrefPage_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _txtAccessToken_Value = new Text(_group, SWT.READ_ONLY | SWT.PASSWORD);
            _txtAccessToken_Value.setToolTipText(Messages.PrefPage_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults().hint(textWidth, SWT.DEFAULT).applyTo(_txtAccessToken_Value);
         }
         {
            _labelRefreshToken = new Label(_group, SWT.NONE);
            _labelRefreshToken.setText(Messages.PrefPage_CloudConnectivity_Label_RefreshToken);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _txtRefreshToken_Value = new Text(_group, SWT.PASSWORD | SWT.READ_ONLY);
            GridDataFactory.fillDefaults().hint(textWidth, SWT.DEFAULT).applyTo(_txtRefreshToken_Value);
         }
         {
            _labelExpiresAt = new Label(_group, SWT.NONE);
            _labelExpiresAt.setText(Messages.PrefPage_CloudConnectivity_Label_ExpiresAt);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(_group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
         {
            _chkShowHideTokens = new SwitchButton(_group, SWT.NONE);
            _chkShowHideTokens.setText(Messages.PrefPage_CloudConnectivity_Checkbox_ShowOrHideTokens);
            _chkShowHideTokens.setToolTipText(Messages.PrefPage_CloudConnectivity_Checkbox_ShowOrHideTokens_Tooltip);
            _chkShowHideTokens.addSelectionListener(widgetSelectedAdapter(selectionEvent -> showOrHideAllPasswords(!_chkShowHideTokens
                  .getSelection())));
            GridDataFactory.fillDefaults().applyTo(_chkShowHideTokens);
         }
         {
            _linkRevokeAccess = new Link(_group, SWT.NONE);
            _linkRevokeAccess.setText(Messages.PrefPage_CloudConnectivity_Label_RevokeAccess);
            _linkRevokeAccess.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
                  "https://www.dropbox.com/account/connected_apps")));//$NON-NLS-1$
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(0, 16)
                  .applyTo(_linkRevokeAccess);
         }
      }
   }

   private void enableControls() {

      final boolean isAuthorized = StringUtils.hasContent(_txtAccessToken_Value.getText()) &&
            StringUtils.hasContent(_txtRefreshToken_Value.getText());

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
      _chkShowHideTokens.setEnabled(isAuthorized);
      _chkShowHideTokens.setSelection(true);
      _linkRevokeAccess.setEnabled(isAuthorized);
      _btnCleanup.setEnabled(isAuthorized);
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

   private void initUI() {

      noDefaultAndApplyButton();
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

      final StringBuilder authorizeUrl = new StringBuilder(_dropbox_WebPage_Link + "/oauth2/authorize" + UI.SYMBOL_QUESTION_MARK); //$NON-NLS-1$

// SET_FORMATTING_OFF

      authorizeUrl.append(      OAuth2Constants.PARAM_RESPONSE_TYPE + "=" + OAuth2Constants.PARAM_CODE); //$NON-NLS-1$
      authorizeUrl.append("&" + OAuth2Constants.PARAM_CLIENT_ID +     "=" + ClientId); //$NON-NLS-1$ //$NON-NLS-2$
      authorizeUrl.append("&" + OAuth2Constants.PARAM_REDIRECT_URI +  "=" + DropboxClient.DropboxCallbackUrl); //$NON-NLS-1$ //$NON-NLS-2$
      authorizeUrl.append("&" + "code_challenge" +                    "=" + codeChallenge); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      authorizeUrl.append("&" + "code_challenge_method" +             "=" + "S256"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
      authorizeUrl.append("&" + "token_access_type" +                 "=" + "offline"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

// SET_FORMATTING_ON

      Display.getDefault().syncExec(() -> WEB.openUrl(authorizeUrl.toString()));
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

      _txtAccessToken_Value.setText(
            _prefStore.getDefaultString(Preferences.DROPBOX_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(UI.EMPTY_STRING);
      _txtRefreshToken_Value.setText(
            _prefStore.getDefaultString(Preferences.DROPBOX_REFRESHTOKEN));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(Preferences.DROPBOX_ACCESSTOKEN, _txtAccessToken_Value.getText());
         _prefStore.setValue(Preferences.DROPBOX_REFRESHTOKEN, _txtRefreshToken_Value.getText());

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

      _txtAccessToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_ACCESSTOKEN));
      _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
            _prefStore.getLong(Preferences.DROPBOX_ACCESSTOKEN_ISSUE_DATETIME),
            _prefStore.getInt(Preferences.DROPBOX_ACCESSTOKEN_EXPIRES_IN)));
      _txtRefreshToken_Value.setText(_prefStore.getString(Preferences.DROPBOX_REFRESHTOKEN));

      enableControls();
   }

   private void showOrHideAllPasswords(final boolean showPasswords) {

      final List<Text> texts = new ArrayList<>();
      texts.add(_txtAccessToken_Value);
      texts.add(_txtRefreshToken_Value);

      Preferences.showOrHidePasswords(texts, showPasswords);
   }

}
