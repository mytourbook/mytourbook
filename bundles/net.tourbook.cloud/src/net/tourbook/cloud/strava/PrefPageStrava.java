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
package net.tourbook.cloud.strava;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.net.URISyntaxException;

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.CloudImages;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.LocalHostServer;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.Util;
import net.tourbook.preferences.PrefPageTourTypeFilterList;
import net.tourbook.web.WEB;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferenceLinkArea;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

public class PrefPageStrava extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   //SET_FORMATTING_OFF
   private static final String PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Group_CloudAccount;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_GROUP_TOURUPLOAD   = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Group_TourUpload;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN  = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_AccessToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT    = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_ExpiresAt;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_RefreshToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE      = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_WebPage;
   //SET_FORMATTING_ON

   public static final String      ID                  = "net.tourbook.cloud.PrefPageStrava";                                         //$NON-NLS-1$
   public static final int         CALLBACK_PORT       = 4918;

   public static final String      ClientId            = "55536";                                                                     //$NON-NLS-1$

   private IPreferenceStore        _prefStore          = Activator.getDefault().getPreferenceStore();
   private IPropertyChangeListener _prefChangeListener;
   private LocalHostServer         _server;

   private String                  _athleteId;
   private long                    _accessTokenExpiresAt;

   private Image                   _imageStravaConnect = Activator.getImageDescriptor(CloudImages.Cloud_Strava_Connect).createImage();

   /*
    * UI controls
    */
   private Label              _labelAccessToken;
   private Label              _labelAccessToken_Value;
   private Label              _labelAthleteName;
   private Label              _labelAthleteName_Value;
   private Label              _labelAthleteWebPage;
   private Label              _labelExpiresAt;
   private Label              _labelExpiresAt_Value;
   private Label              _labelRefreshToken;
   private Label              _labelRefreshToken_Value;
   private Button             _chkSendDescription;
   private Button             _chkUseTourTypeMapping;

   private Link               _linkAthleteWebPage;
   private PreferenceLinkArea _linkTourTypeFilters;

   private String constructAthleteWebPageLink(final String athleteId) {
      if (StringUtils.hasContent(athleteId)) {
         return "https://www.strava.com/athletes/" + athleteId; //$NON-NLS-1$
      }

      return UI.EMPTY_STRING;
   }

   private String constructAthleteWebPageLinkWithTags(final String athleteId) {
      return UI.LINK_TAG_START + constructAthleteWebPageLink(athleteId) + UI.LINK_TAG_END;
   }

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();

      enableControls();

      _prefChangeListener = event -> {

         if (event.getProperty().equals(Preferences.STRAVA_ACCESSTOKEN)) {

            Display.getDefault().syncExec(() -> {

               if (!event.getOldValue().equals(event.getNewValue())) {

                  _labelAccessToken_Value.setText(_prefStore.getString(Preferences.STRAVA_ACCESSTOKEN));
                  _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.STRAVA_REFRESHTOKEN));
                  _accessTokenExpiresAt = _prefStore.getLong(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
                  _labelExpiresAt_Value.setText(getLocalExpireAtDateTime());

                  _labelAthleteName_Value.setText(_prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME));
                  _athleteId = _prefStore.getString(Preferences.STRAVA_ATHLETEID);
                  _linkAthleteWebPage.setText(constructAthleteWebPageLinkWithTags(_athleteId));

                  enableControls();
               }
               if (_server != null) {
                  _server.stopCallBackServer();
               }
            });
         }
      };
   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);
      createUI_10_Connect(parent);
      createUI_20_AccountInformation(parent);
      createUI_30_TourUpload(parent);
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
         buttonConnect.setImage(_imageStravaConnect);
         buttonConnect.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onClickAuthorize()));
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(buttonConnect);
      }

   }

   private void createUI_20_AccountInformation(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         {
            final Label labelWebPage = new Label(group, SWT.NONE);
            labelWebPage.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE);
            GridDataFactory.fillDefaults().applyTo(labelWebPage);

            final Link linkWebPage = new Link(group, SWT.NONE);
            linkWebPage.setText(UI.LINK_TAG_START + Messages.PrefPage_AccountInformation_Link_Strava_WebPage + UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
                  Messages.PrefPage_AccountInformation_Link_Strava_WebPage)));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
         }
         {
            _labelAthleteName = new Label(group, SWT.NONE);
            _labelAthleteName.setText(Messages.PrefPage_AccountInformation_Label_AthleteName);
            GridDataFactory.fillDefaults().applyTo(_labelAthleteName);

            _labelAthleteName_Value = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelAthleteName_Value);
         }
         {
            _labelAthleteWebPage = new Label(group, SWT.NONE);
            _labelAthleteWebPage.setText(Messages.PrefPage_AccountInformation_Label_AthleteWebPage);
            GridDataFactory.fillDefaults().applyTo(_labelAthleteWebPage);

            _linkAthleteWebPage = new Link(group, SWT.NONE);
            _linkAthleteWebPage.setEnabled(true);
            _linkAthleteWebPage.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> WEB.openUrl(constructAthleteWebPageLink(_athleteId))));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_linkAthleteWebPage);
         }
         {
            _labelAccessToken = new Label(group, SWT.NONE);
            _labelAccessToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _labelAccessToken_Value = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelAccessToken_Value);
         }
         {
            _labelRefreshToken = new Label(group, SWT.NONE);
            _labelRefreshToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _labelRefreshToken_Value = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelRefreshToken_Value);
         }
         {
            _labelExpiresAt = new Label(group, SWT.NONE);
            _labelExpiresAt.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
      }
   }

   private void createUI_30_TourUpload(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(PREFPAGE_CLOUDCONNECTIVITY_GROUP_TOURUPLOAD);
      GridLayoutFactory.swtDefaults().applyTo(group);
      {
         {
            /*
             * Checkbox: Send the tour description
             */
            _chkSendDescription = new Button(group, SWT.CHECK);
            GridDataFactory.fillDefaults().applyTo(_chkSendDescription);

            _chkSendDescription.setText(Messages.PrefPage_UploadConfiguration_Button_SendDescription);
         }
         {
            /*
             * Checkbox: Use tour type mapping
             */
            _chkUseTourTypeMapping = new Button(group, SWT.CHECK);
            GridDataFactory.fillDefaults().applyTo(_chkUseTourTypeMapping);
            _chkUseTourTypeMapping.setText(Messages.PrefPage_UploadConfiguration_Button_UseTourTypeMapping);
            _chkUseTourTypeMapping.setToolTipText(Messages.PrefPage_UploadConfiguration_Button_UseTourTypeMapping_Tooltip);
            _chkUseTourTypeMapping.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> _linkTourTypeFilters.getControl().setEnabled(_chkUseTourTypeMapping.getSelection())));
         }
         {
            _linkTourTypeFilters = new PreferenceLinkArea(
                  group,
                  SWT.MULTI | SWT.WRAP,
                  PrefPageTourTypeFilterList.ID,
                  Messages.PrefPage_TourTypeFilter_Link_StravaTourTypes,
                  (IWorkbenchPreferenceContainer) getContainer(),
                  new PrefPageTourTypeFilterList());

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_linkTourTypeFilters.getControl());
         }
      }
   }

   @Override
   public void dispose() {

      Util.disposeResource(_imageStravaConnect);

      super.dispose();
   }

   private void enableControls() {

      final boolean isAuthorized = StringUtils.hasContent(_labelAccessToken_Value.getText()) &&
            StringUtils.hasContent(_labelRefreshToken_Value.getText());

      _labelAthleteName_Value.setEnabled(isAuthorized);
      _labelAthleteWebPage.setEnabled(isAuthorized);
      _linkAthleteWebPage.setEnabled(isAuthorized);
      _labelAthleteName.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
      _labelRefreshToken.setEnabled(isAuthorized);
      _labelRefreshToken_Value.setEnabled(isAuthorized);
      _labelExpiresAt_Value.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _chkSendDescription.setEnabled(isAuthorized);
      _chkUseTourTypeMapping.setEnabled(isAuthorized);

      _linkTourTypeFilters.getControl().setEnabled(_chkUseTourTypeMapping.getSelection());
   }

   private String getLocalExpireAtDateTime() {
      return (_accessTokenExpiresAt == 0) ? UI.EMPTY_STRING : TimeTools.getUTCISODateTime(
            _accessTokenExpiresAt);
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
    * so that the user can allow the MyTourbook Strava app to have access
    * to their Strava account.
    */
   private void onClickAuthorize() {

      if (_server != null) {
         _server.stopCallBackServer();
      }

      final StravaTokensRetrievalHandler tokensRetrievalHandler = new StravaTokensRetrievalHandler();
      _server = new LocalHostServer(CALLBACK_PORT, "Strava", _prefChangeListener); //$NON-NLS-1$

      final boolean isServerCreated = _server.createCallBackServer(tokensRetrievalHandler);

      if (!isServerCreated) {
         return;
      }

      final URIBuilder authorizeUrlBuilder = new URIBuilder();
      authorizeUrlBuilder.setScheme("https"); //$NON-NLS-1$
      authorizeUrlBuilder.setHost("www.strava.com"); //$NON-NLS-1$
      authorizeUrlBuilder.setPath("/oauth/authorize"); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_RESPONSE_TYPE, OAuth2Constants.PARAM_CODE);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_CLIENT_ID, ClientId);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_REDIRECT_URI, "http://localhost:" + CALLBACK_PORT); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter("scope", "read,activity:write"); //$NON-NLS-1$ //$NON-NLS-2$
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

      _labelAccessToken_Value.setText(_prefStore.getDefaultString(Preferences.STRAVA_ACCESSTOKEN));
      _labelRefreshToken_Value.setText(_prefStore.getDefaultString(Preferences.STRAVA_REFRESHTOKEN));
      _labelAthleteName_Value.setText(_prefStore.getDefaultString(Preferences.STRAVA_ATHLETEFULLNAME));
      _athleteId = _prefStore.getDefaultString(Preferences.STRAVA_ATHLETEID);
      _linkAthleteWebPage.setText(constructAthleteWebPageLinkWithTags(_athleteId));
      _accessTokenExpiresAt = _prefStore.getDefaultLong(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
      _labelExpiresAt_Value.setText(getLocalExpireAtDateTime());

      _chkSendDescription.setSelection(_prefStore.getDefaultBoolean(Preferences.STRAVA_SENDDESCRIPTION));
      _chkUseTourTypeMapping.setSelection(_prefStore.getDefaultBoolean(Preferences.STRAVA_USETOURTYPEMAPPING));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {
         _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN, _labelAccessToken_Value.getText());
         _prefStore.setValue(Preferences.STRAVA_REFRESHTOKEN, _labelRefreshToken_Value.getText());
         _prefStore.setValue(Preferences.STRAVA_ATHLETEFULLNAME, _labelAthleteName_Value.getText());
         _prefStore.setValue(Preferences.STRAVA_ATHLETEID, _athleteId);
         _prefStore.setValue(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT, _accessTokenExpiresAt);

         _prefStore.setValue(Preferences.STRAVA_SENDDESCRIPTION, _chkSendDescription.getSelection());

         final boolean prefUseTourTypeMapping = _prefStore.getBoolean(Preferences.STRAVA_USETOURTYPEMAPPING);
         final boolean currentUseTourTypeMapping = _chkUseTourTypeMapping.getSelection();
         _prefStore.setValue(Preferences.STRAVA_USETOURTYPEMAPPING, currentUseTourTypeMapping);
         if (prefUseTourTypeMapping != currentUseTourTypeMapping) {

            final Shell activeShell = Display.getDefault().getActiveShell();
            if (currentUseTourTypeMapping) {

               //If the user has just activated the tour type mapping, a restart
               //is needed in order for the Strava tour type filters to be created.
               if (MessageDialog.openQuestion(
                     activeShell,
                     Messages.Dialog_UseTourTypeMappingModified_Title,
                     Messages.Dialog_UseTourTypeMappingActivated_Message)) {

                  Display.getCurrent().asyncExec(() -> PlatformUI.getWorkbench().restart());

               }
            } else {

               //If the user has just deactivated the tour type mapping, we need
               //to let them know that they can safely remove all the Strava tour
               //type filters from the filter list.
               MessageDialog.openInformation(
                     activeShell,
                     Messages.Dialog_UseTourTypeMappingModified_Title,
                     Messages.Dialog_UseTourTypeMappingDeactivated_Message);
            }
         }

         if (_server != null) {
            _server.stopCallBackServer();
         }
      }

      return isOK;
   }

   private void restoreState() {

      _labelAccessToken_Value.setText(_prefStore.getString(Preferences.STRAVA_ACCESSTOKEN));
      _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.STRAVA_REFRESHTOKEN));
      _labelAthleteName_Value.setText(_prefStore.getString(Preferences.STRAVA_ATHLETEFULLNAME));
      _athleteId = _prefStore.getString(Preferences.STRAVA_ATHLETEID);
      _linkAthleteWebPage.setText(constructAthleteWebPageLinkWithTags(_athleteId));
      _accessTokenExpiresAt = _prefStore.getLong(Preferences.STRAVA_ACCESSTOKEN_EXPIRES_AT);
      _labelExpiresAt_Value.setText(getLocalExpireAtDateTime());

      _chkSendDescription.setSelection(_prefStore.getBoolean(Preferences.STRAVA_SENDDESCRIPTION));
      _chkUseTourTypeMapping.setSelection(_prefStore.getBoolean(Preferences.STRAVA_USETOURTYPEMAPPING));
   }
}
