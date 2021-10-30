/*******************************************************************************
 * Copyright (C) 2021 Frédéric Bard
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
package net.tourbook.cloud.suunto;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.Activator;
import net.tourbook.cloud.Messages;
import net.tourbook.cloud.PreferenceInitializer;
import net.tourbook.cloud.Preferences;
import net.tourbook.cloud.oauth2.LocalHostServer;
import net.tourbook.cloud.oauth2.OAuth2Constants;
import net.tourbook.cloud.oauth2.OAuth2Utils;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourPerson;
import net.tourbook.database.PersonManager;
import net.tourbook.importdata.DialogEasyImportConfig;
import net.tourbook.web.WEB;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageSuunto extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   //SET_FORMATTING_OFF
   private static final String PREFPAGE_CLOUDCONNECTIVITY_BUTTON_AUTHORIZE                         = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Button_Authorize;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT                       = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Group_CloudAccount;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_GROUP_TOURDOWNLOAD                       = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Group_TourDownload;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN                        = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_AccessToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT                          = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_ExpiresAt;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_PERSONLINKEDTOCLOUDACCOUNT         = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_PersonLinkedToCloudAccount;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_PERSONLINKEDTOCLOUDACCOUNT_TOOLTIP = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_PersonLinkedToCloudAccount_Tooltip;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN                       = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_RefreshToken;
   private static final String PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE                            = net.tourbook.cloud.Messages.PrefPage_CloudConnectivity_Label_WebPage;
   //SET_FORMATTING_ON

   private static final String     APP_BTN_BROWSE                   = net.tourbook.Messages.app_btn_browse;
   private static final String     DIALOG_EXPORT_DIR_DIALOG_MESSAGE = net.tourbook.Messages.dialog_export_dir_dialog_message;
   private static final String     DIALOG_EXPORT_DIR_DIALOG_TEXT    = net.tourbook.Messages.dialog_export_dir_dialog_text;

   public static final String      ID                               = "net.tourbook.cloud.PrefPageSuunto";                   //$NON-NLS-1$

   public static final String      ClientId                         = "d8f3e53f-6c20-4d17-9a4e-a4930c8667e8";                //$NON-NLS-1$

   public static final int         CALLBACK_PORT                    = 4919;

   private IPreferenceStore        _prefStore                       = Activator.getDefault().getPreferenceStore();

   private final IDialogSettings   _state                           = TourbookPlugin.getState(DialogEasyImportConfig.ID);
   private IPropertyChangeListener _prefChangeListener;
   private LocalHostServer         _server;
   private List<Long>              _personIds;

   /*
    * UI controls
    */
   private Group    _groupCloudAccount;
   private Label    _labelAccessToken;
   private Label    _labelAccessToken_Value;
   private Label    _labelExpiresAt;
   private Label    _labelExpiresAt_Value;
   private Label    _labelRefreshToken;
   private Label    _labelRefreshToken_Value;
   private Label    _labelDownloadFolder;
   private Combo    _comboDownloadFolderPath;
   private Button   _btnSelectFolder;
   private Button   _chkUseDateFilter;
   private DateTime _dtFilterSince;
   private Combo    _comboPeopleList;

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();

      enableControls();

      _prefChangeListener = event -> {

         Display.getDefault().syncExec(() -> {

            final String selectedPersonId = getSelectedPersonId();

            if (!event.getProperty().equals(Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId))) {
               return;
            }

            if (!event.getOldValue().equals(event.getNewValue())) {

               _labelAccessToken_Value.setText(_prefStore.getString(Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId)));
               _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
                     _prefStore.getLong(Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(selectedPersonId)),
                     _prefStore.getLong(Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(selectedPersonId)) * 1000));
               _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.getPerson_SuuntoRefreshToken_String(selectedPersonId)));

               _groupCloudAccount.redraw();

               enableControls();
            }

            if (_server != null) {
               _server.stopCallBackServer();
            }
         });
      };
   }

   private Composite createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      createUI_10_Authorize(parent);
      createUI_20_TokensInformation(parent);
      createUI_30_TourDownload(parent);

      return parent;
   }

   private void createUI_10_Authorize(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         final Label labelPerson = new Label(container, SWT.NONE);
         labelPerson.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_PERSONLINKEDTOCLOUDACCOUNT);
         labelPerson.setToolTipText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_PERSONLINKEDTOCLOUDACCOUNT_TOOLTIP);
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(labelPerson);

         /*
          * Drop down menu to select a user
          */
         _comboPeopleList = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
         _comboPeopleList.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectPerson()));
         GridDataFactory.fillDefaults().applyTo(_comboPeopleList);

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

      final PixelConverter pc = new PixelConverter(parent);

      _groupCloudAccount = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupCloudAccount);
      _groupCloudAccount.setText(PREFPAGE_CLOUDCONNECTIVITY_GROUP_CLOUDACCOUNT);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupCloudAccount);
      {
         {
            final Label labelWebPage = new Label(_groupCloudAccount, SWT.NONE);
            labelWebPage.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_WEBPAGE);
            GridDataFactory.fillDefaults().applyTo(labelWebPage);

            final Link linkWebPage = new Link(_groupCloudAccount, SWT.NONE);
            linkWebPage.setText(UI.LINK_TAG_START + Messages.PrefPage_AccountInformation_Link_SuuntoApp_WebPage + UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
                  Messages.PrefPage_AccountInformation_Link_SuuntoApp_WebPage)));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
         }
         {
            _labelAccessToken = new Label(_groupCloudAccount, SWT.NONE);
            _labelAccessToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_ACCESSTOKEN);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _labelAccessToken_Value = new Label(_groupCloudAccount, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelAccessToken_Value);
         }
         {
            _labelRefreshToken = new Label(_groupCloudAccount, SWT.NONE);
            _labelRefreshToken.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_REFRESHTOKEN);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _labelRefreshToken_Value = new Label(_groupCloudAccount, SWT.WRAP);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_labelRefreshToken_Value);
         }
         {
            _labelExpiresAt = new Label(_groupCloudAccount, SWT.NONE);
            _labelExpiresAt.setText(PREFPAGE_CLOUDCONNECTIVITY_LABEL_EXPIRESAT);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(_groupCloudAccount, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
      }
   }

   private void createUI_30_TourDownload(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(PREFPAGE_CLOUDCONNECTIVITY_GROUP_TOURDOWNLOAD);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {
         {
            _labelDownloadFolder = new Label(group, SWT.NONE);
            _labelDownloadFolder.setText(Messages.PrefPage_SuuntoWorkouts_Label_FolderPath);
            _labelDownloadFolder.setToolTipText(Messages.PrefPage_SuuntoWorkouts_FolderPath_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(_labelDownloadFolder);

            /*
             * combo: path
             */
            _comboDownloadFolderPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboDownloadFolderPath);
            _comboDownloadFolderPath.setToolTipText(Messages.PrefPage_SuuntoWorkouts_FolderPath_Tooltip);
            _comboDownloadFolderPath.setEnabled(false);

            _btnSelectFolder = new Button(group, SWT.PUSH);
            _btnSelectFolder.setText(APP_BTN_BROWSE);
            _btnSelectFolder.setToolTipText(Messages.PrefPage_SuuntoWorkouts_FolderPath_Tooltip);
            _btnSelectFolder.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectBrowseDirectory()));
            setButtonLayoutData(_btnSelectFolder);
         }

         {
            /*
             * Checkbox: Use a "since" date filter
             */
            _chkUseDateFilter = new Button(group, SWT.CHECK);
            _chkUseDateFilter.setText(Messages.PrefPage_SuuntoWorkouts_Checkbox_SinceDateFilter);
            _chkUseDateFilter.setToolTipText(Messages.PrefPage_SuuntoWorkouts_SinceDateFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(_chkUseDateFilter);
            _chkUseDateFilter.addSelectionListener(widgetSelectedAdapter(selectionEvent -> _dtFilterSince.setEnabled(_chkUseDateFilter
                  .getSelection())));

            _dtFilterSince = new DateTime(group, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
            _dtFilterSince.setToolTipText(Messages.PrefPage_SuuntoWorkouts_SinceDateFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(_dtFilterSince);
         }
      }
   }

   private void enableControls() {

      final boolean isAuthorized = StringUtils.hasContent(_labelAccessToken_Value.getText())
            && StringUtils.hasContent(_labelRefreshToken_Value.getText());

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
      _labelDownloadFolder.setEnabled(isAuthorized);
      _comboDownloadFolderPath.setEnabled(isAuthorized);
      _btnSelectFolder.setEnabled(isAuthorized);
      _chkUseDateFilter.setEnabled(isAuthorized);
      _dtFilterSince.setEnabled(isAuthorized && _chkUseDateFilter.getSelection());
   }

   private long getFilterSinceDate() {

      final int year = _dtFilterSince.getYear();
      final int month = _dtFilterSince.getMonth() + 1;
      final int day = _dtFilterSince.getDay();
      return ZonedDateTime.of(
            year,
            month,
            day,
            0,
            0,
            0,
            0,
            ZoneId.of("Etc/GMT")).toEpochSecond() * 1000; //$NON-NLS-1$
   }

   private String getSelectedPersonId() {

      final int selectedPersonIndex = _comboPeopleList.getSelectionIndex();

      final String personId = selectedPersonIndex == 0 ? UI.EMPTY_STRING : String.valueOf(_personIds.get(
            selectedPersonIndex));

      return personId;
   }

   @Override
   public void init(final IWorkbench workbench) {}

   @Override
   public boolean okToLeave() {

      if (_server != null) {
         _server.stopCallBackServer();
      }

      return super.okToLeave();
   }

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Suunto app to have access
    * to their Suunto account.
    */
   private void onClickAuthorize() {

      if (_server != null) {
         _server.stopCallBackServer();
      }

      final SuuntoTokensRetrievalHandler tokensRetrievalHandler = new SuuntoTokensRetrievalHandler(getSelectedPersonId());
      _server = new LocalHostServer(CALLBACK_PORT, "Suunto", _prefChangeListener); //$NON-NLS-1$
      final boolean isServerCreated = _server.createCallBackServer(tokensRetrievalHandler);

      if (!isServerCreated) {
         return;
      }

      final URIBuilder authorizeUrlBuilder = new URIBuilder();
      authorizeUrlBuilder.setScheme("https"); //$NON-NLS-1$
      authorizeUrlBuilder.setHost("cloudapi-oauth.suunto.com"); //$NON-NLS-1$
      authorizeUrlBuilder.setPath("/oauth/authorize"); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_RESPONSE_TYPE, OAuth2Constants.PARAM_CODE);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_CLIENT_ID, ClientId);
      authorizeUrlBuilder.addParameter(OAuth2Constants.PARAM_REDIRECT_URI, "http://localhost:" + CALLBACK_PORT); //$NON-NLS-1$
      try {
         final String authorizeUrl = authorizeUrlBuilder.build().toString();

         Display.getDefault().syncExec(() -> WEB.openUrl(authorizeUrl));
      } catch (final URISyntaxException e) {
         StatusUtil.log(e);
      }
   }

   private void onSelectBrowseDirectory() {

      final DirectoryDialog dialog = new DirectoryDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
      dialog.setText(DIALOG_EXPORT_DIR_DIALOG_TEXT);
      dialog.setMessage(DIALOG_EXPORT_DIR_DIALOG_MESSAGE);

      final String selectedDirectoryName = dialog.open();

      if (selectedDirectoryName != null) {

         setErrorMessage(null);
         _comboDownloadFolderPath.setText(selectedDirectoryName);
      }
   }

   private void onSelectPerson() {

      final String personId = getSelectedPersonId();
      restoreAccountInformation(personId);

      enableControls();
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

      // Restore the default values for the "All People" UI
      _comboPeopleList.select(_prefStore.getDefaultInt(Preferences.SUUNTO_SELECTED_PERSON_INDEX));

      final String selectedPersonId = _prefStore.getDefaultString(Preferences.SUUNTO_SELECTED_PERSON_ID);

      _labelAccessToken_Value.setText(_prefStore.getDefaultString(Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId)));
      _labelExpiresAt_Value.setText(UI.EMPTY_STRING);
      _labelRefreshToken_Value.setText(_prefStore.getDefaultString(Preferences.getPerson_SuuntoRefreshToken_String(selectedPersonId)));

      _comboDownloadFolderPath.setText(_prefStore.getDefaultString(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(selectedPersonId)));

      _chkUseDateFilter.setSelection(_prefStore.getDefaultBoolean(Preferences.getPerson_SuuntoUseWorkoutFilterSinceDate_String(selectedPersonId)));
      setFilterSinceDate(_prefStore.getDefaultLong(Preferences.getPerson_SuuntoWorkoutFilterSinceDate_String(selectedPersonId)));

      enableControls();

      // Restore the default values in the preferences for each person. Otherwise,
      // those values will reappear when selecting another person
      final List<TourPerson> tourPeopleList = PersonManager.getTourPeople();
      final List<String> tourPersonIds = new ArrayList<>();

      // This empty string represents "All people"
      tourPersonIds.add(UI.EMPTY_STRING);
      tourPeopleList.forEach(
            tourPerson -> tourPersonIds.add(
                  String.valueOf(tourPerson.getPersonId())));

      for (final String tourPersonId : tourPersonIds) {

         _prefStore.setValue(Preferences.getPerson_SuuntoAccessToken_String(tourPersonId), UI.EMPTY_STRING);
         _prefStore.setValue(Preferences.getPerson_SuuntoRefreshToken_String(tourPersonId), UI.EMPTY_STRING);
         _prefStore.setValue(Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(tourPersonId), 0L);
         _prefStore.setValue(Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(tourPersonId), 0L);
         _prefStore.setValue(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(tourPersonId), UI.EMPTY_STRING);
         _prefStore.setValue(Preferences.getPerson_SuuntoUseWorkoutFilterSinceDate_String(tourPersonId), false);
         _prefStore.setValue(Preferences.getPerson_SuuntoWorkoutFilterSinceDate_String(tourPersonId), PreferenceInitializer.SUUNTO_FILTER_SINCE_DATE);
      }

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {

         final String personId = getSelectedPersonId();

         _prefStore.setValue(Preferences.getPerson_SuuntoAccessToken_String(personId), _labelAccessToken_Value.getText());
         _prefStore.setValue(Preferences.getPerson_SuuntoRefreshToken_String(personId), _labelRefreshToken_Value.getText());

         if (StringUtils.isNullOrEmpty(_labelExpiresAt_Value.getText())) {
            _prefStore.setValue(Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(personId), 0L);
            _prefStore.setValue(Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(personId), 0L);
         }

         if (_server != null) {
            _server.stopCallBackServer();
         }

         final String downloadFolder = _comboDownloadFolderPath.getText();
         _prefStore.setValue(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(personId), downloadFolder);
         if (StringUtils.hasContent(downloadFolder)) {

            final String[] currentDeviceFolderHistoryItems = _state.getArray(
                  DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS);
            final List<String> stateDeviceFolderHistoryItems = currentDeviceFolderHistoryItems != null ? new ArrayList<>(Arrays.asList(
                  currentDeviceFolderHistoryItems))
                  : new ArrayList<>();

            if (!stateDeviceFolderHistoryItems.contains(downloadFolder)) {
               stateDeviceFolderHistoryItems.add(downloadFolder);
               _state.put(DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS,
                     stateDeviceFolderHistoryItems.toArray(new String[stateDeviceFolderHistoryItems.size()]));
            }
         }

         _prefStore.setValue(Preferences.getPerson_SuuntoUseWorkoutFilterSinceDate_String(personId), _chkUseDateFilter.getSelection());
         _prefStore.setValue(Preferences.getPerson_SuuntoWorkoutFilterSinceDate_String(personId), getFilterSinceDate());

         final int selectedPersonIndex = _comboPeopleList.getSelectionIndex();
         _prefStore.setValue(Preferences.SUUNTO_SELECTED_PERSON_INDEX, selectedPersonIndex);
         _prefStore.setValue(Preferences.SUUNTO_SELECTED_PERSON_ID, personId);
      }

      return isOK;
   }

   private void restoreAccountInformation(final String selectedPersonId) {

      _labelAccessToken_Value.setText(_prefStore.getString(Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId)));
      _labelExpiresAt_Value.setText(OAuth2Utils.computeAccessTokenExpirationDate(
            _prefStore.getLong(Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(selectedPersonId)),
            _prefStore.getLong(Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(selectedPersonId)) * 1000));
      _labelRefreshToken_Value.setText(_prefStore.getString(Preferences.getPerson_SuuntoRefreshToken_String(selectedPersonId)));

      _comboDownloadFolderPath.setText(_prefStore.getString(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(selectedPersonId)));

      _chkUseDateFilter.setSelection(_prefStore.getBoolean(Preferences.getPerson_SuuntoUseWorkoutFilterSinceDate_String(selectedPersonId)));
      setFilterSinceDate(_prefStore.getLong(Preferences.getPerson_SuuntoWorkoutFilterSinceDate_String(selectedPersonId)));
   }

   private void restoreState() {

      final String selectedPersonId = _prefStore.getString(Preferences.SUUNTO_SELECTED_PERSON_ID);
      restoreAccountInformation(selectedPersonId);

      final ArrayList<TourPerson> tourPeople = PersonManager.getTourPeople();
      _comboPeopleList.add(net.tourbook.Messages.App_People_item_all);

      _personIds = new ArrayList<>();
      //Adding the "All People" Id -> null
      _personIds.add(null);
      for (final TourPerson tourPerson : tourPeople) {

         _comboPeopleList.add(tourPerson.getName());
         _personIds.add(tourPerson.getPersonId());
      }

      _comboPeopleList.select(_prefStore.getInt(Preferences.SUUNTO_SELECTED_PERSON_INDEX));
   }

   private void setFilterSinceDate(final long filterSinceDate) {

      final LocalDate suuntoFileDownloadSinceDate = TimeTools.toLocalDate(filterSinceDate);

      _dtFilterSince.setDate(suuntoFileDownloadSinceDate.getYear(),
            suuntoFileDownloadSinceDate.getMonthValue() - 1,
            suuntoFileDownloadSinceDate.getDayOfMonth());
   }

}
