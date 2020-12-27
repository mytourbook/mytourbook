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
package net.tourbook.cloud.strava;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.cloud.oauth2.OAuth2BrowserDialog;
import net.tourbook.cloud.oauth2.OAuth2Client;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StringUtils;
import net.tourbook.web.WEB;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageStrava extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public static final String ID         = "net.tourbook.cloud.PrefPageStrava";        //$NON-NLS-1$

   private IPreferenceStore   _prefStore = Activator.getDefault().getPreferenceStore();

   /*
    * UI controls
    */
   private Label  _labelAthleteName;
   private Label  _athleteFullName;
   private Label  _labelAthleteWebPage;
   private Link   _athleteWebPageLink;
   private Label  _labelAccessToken;
   private Label  _accessToken;
   private Label  _labelRefreshToken;
   private Label  _refreshToken;
   private Label  _labelExpiresAt;
   private Label  _labelAccessTokenExpiresAt;

   private String _athleteId;
   private long   _accessTokenExpiresAt;

   private String constructAthleteWebPageLink(final String athleteId) {
      if (StringUtils.hasContent(athleteId)) {
         return "https://www.strava.com/athletes/" + athleteId; //$NON-NLS-1$
      }

      return UI.EMPTY_STRING;
   }

   private String constructAthleteWebPageLinkWithTags(final String athleteId) {
      return UI.LINK_TAG_START + constructAthleteWebPageLink(athleteId) + UI.LINK_TAG_END;
   }

   private String constructLocalExpireAtDateTime(final long expireAt) {
      if (expireAt == 0) {
         return UI.EMPTY_STRING;
      }

      return Instant.ofEpochMilli(expireAt).atZone(TimeTools.UTC).format(DateTimeFormatter.ISO_DATE_TIME);
   }

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();
   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      createUI_10_Connect(parent);
      createUI_20_AccountInformation(parent);
   }

   private void createUI_10_Connect(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().applyTo(container);
      {
         /*
          * Connect button
          */
         // As mentioned here : https://developers.strava.com/guidelines/
         // "All apps must use the Connect with Strava button for OAuth that links to
         // https://www.strava.com/oauth/authorize or https://www.strava.com/oauth/mobile/authorize.
         // No variations or modifications are acceptable."

         final Button buttonConnect = new Button(container, SWT.NONE);
         final Image imageConnect = Activator.getImageDescriptor(Messages.Image__Connect_With_Strava).createImage();
         buttonConnect.setImage(imageConnect);
         buttonConnect.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
               onClickAuthorize();
            }
         });
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(buttonConnect);
      }
   }

   private void createUI_20_AccountInformation(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(Messages.PrefPage_Account_Information_Group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         _labelAthleteName = new Label(group, SWT.NONE);
         _labelAthleteName.setText(Messages.PrefPage_Account_Information_AthleteName_Label);
         GridDataFactory.fillDefaults().applyTo(_labelAthleteName);

         _athleteFullName = new Label(group, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_athleteFullName);

         _labelAthleteWebPage = new Label(group, SWT.NONE);
         _labelAthleteWebPage.setText(Messages.PrefPage_Account_Information_AthleteWebPage_Label);
         GridDataFactory.fillDefaults().applyTo(_labelAthleteWebPage);

         _athleteWebPageLink = new Link(group, SWT.NONE);
         _athleteWebPageLink.setEnabled(true);
         _athleteWebPageLink.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               WEB.openUrl(constructAthleteWebPageLink(_athleteId));
            }
         });
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_athleteWebPageLink);

         _labelAccessToken = new Label(group, SWT.NONE);
         _labelAccessToken.setText(Messages.PrefPage_Account_Information_AccessToken_Label);
         GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

         _accessToken = new Label(group, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_accessToken);

         _labelRefreshToken = new Label(group, SWT.NONE);
         _labelRefreshToken.setText(Messages.PrefPage_Account_Information_RefreshToken_Label);
         GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

         _refreshToken = new Label(group, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_refreshToken);

         _labelExpiresAt = new Label(group, SWT.NONE);
         _labelExpiresAt.setText(Messages.PrefPage_Account_Information_ExpiresAt_Label);
         GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

         _labelAccessTokenExpiresAt = new Label(group, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelAccessTokenExpiresAt);
      }
   }

   @Override
   public void init(final IWorkbench workbench) {}

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Strava app to have access
    * to their Strava account.
    */
   private void onClickAuthorize() {

      final OAuth2Client client = new OAuth2Client();

      client.setAuthorizeUrl(StravaUploader.HerokuAppUrl + "/authorize"); //$NON-NLS-1$
      client.setRedirectUri("http://localhost:5000"); //$NON-NLS-1$

      final OAuth2BrowserDialog oAuth2Browser = new OAuth2BrowserDialog(client, "Strava"); //$NON-NLS-1$
      if (oAuth2Browser.open() != Window.OK) {
         return;
      }

      final String authorizationCode = oAuth2Browser.getAuthorizationCode();

      String dialogMessage;
      if (StringUtils.isNullOrEmpty(authorizationCode)) {
         dialogMessage = NLS.bind(
               Messages.Pref_CloudConnectivity_Strava_AccessToken_NotRetrieved,
               oAuth2Browser.getResponse());
      } else {
         final Tokens newTokens = StravaUploader.getTokens(authorizationCode, false, UI.EMPTY_STRING);

         if (newTokens != null) {

            _accessToken.setText(newTokens.getAccess_token());
            _refreshToken.setText(newTokens.getRefresh_token());
            _accessTokenExpiresAt = newTokens.getExpires_at();
            _labelAccessTokenExpiresAt.setText(constructLocalExpireAtDateTime(_accessTokenExpiresAt));

            final Athlete athlete = newTokens.getAthlete();
            if (athlete != null) {
               _athleteFullName.setText(athlete.getFirstName() + UI.SPACE1 + athlete.getLastName());
               _athleteId = athlete.getId();
               _athleteWebPageLink.setText(constructAthleteWebPageLinkWithTags(_athleteId));
            }
         }

         dialogMessage = Messages.Pref_CloudConnectivity_Strava_AccessToken_Retrieved;
      }

      MessageDialog.openInformation(
            Display.getCurrent().getActiveShell(),
            Messages.Pref_CloudConnectivity_Strava_AccessToken_Retrieval_Title,
            dialogMessage);

      UpdateButtonConnectState();
   }

   @Override
   protected void performDefaults() {

      _accessToken.setText(_prefStore.getDefaultString(IPreferences.STRAVA_ACCESSTOKEN));
      _refreshToken.setText(_prefStore.getDefaultString(IPreferences.STRAVA_REFRESHTOKEN));
      _athleteFullName.setText(_prefStore.getDefaultString(IPreferences.STRAVA_ATHLETEFULLNAME));
      _athleteId = _prefStore.getDefaultString(IPreferences.STRAVA_ATHLETEID);
      _athleteWebPageLink.setText(constructAthleteWebPageLinkWithTags(_athleteId));
      _accessTokenExpiresAt = _prefStore.getDefaultLong(IPreferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
      _labelAccessTokenExpiresAt.setText(constructLocalExpireAtDateTime(_accessTokenExpiresAt));

      UpdateButtonConnectState();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(IPreferences.STRAVA_ACCESSTOKEN, _accessToken.getText());
         _prefStore.setValue(IPreferences.STRAVA_REFRESHTOKEN, _refreshToken.getText());
         _prefStore.setValue(IPreferences.STRAVA_ATHLETEFULLNAME, _athleteFullName.getText());
         _prefStore.setValue(IPreferences.STRAVA_ATHLETEID, _athleteId);
         _prefStore.setValue(IPreferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, _accessTokenExpiresAt);
      }

      return isOK;
   }

   private void restoreState() {

      _accessToken.setText(_prefStore.getString(IPreferences.STRAVA_ACCESSTOKEN));
      _refreshToken.setText(_prefStore.getString(IPreferences.STRAVA_REFRESHTOKEN));
      _athleteFullName.setText(_prefStore.getString(IPreferences.STRAVA_ATHLETEFULLNAME));
      _athleteId = _prefStore.getString(IPreferences.STRAVA_ATHLETEID);
      _athleteWebPageLink.setText(constructAthleteWebPageLinkWithTags(_athleteId));
      _accessTokenExpiresAt = _prefStore.getLong(IPreferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
      _labelAccessTokenExpiresAt.setText(constructLocalExpireAtDateTime(_accessTokenExpiresAt));

      UpdateButtonConnectState();
   }

   private void UpdateButtonConnectState() {

      final boolean isAuthorized = StringUtils.hasContent(_accessToken.getText()) && StringUtils.hasContent(_refreshToken.getText());

      _athleteFullName.setEnabled(isAuthorized);
      _labelAthleteWebPage.setEnabled(isAuthorized);
      _athleteWebPageLink.setEnabled(isAuthorized);
      _labelAthleteName.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
      _labelRefreshToken.setEnabled(isAuthorized);
      _refreshToken.setEnabled(isAuthorized);
      _labelAccessTokenExpiresAt.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
   }
}
