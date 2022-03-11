/*******************************************************************************
 * Copyright (C) 2021, 2022 Frédéric Bard
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.part.PageBook;

public class PrefPageSuunto extends PreferencePage implements IWorkbenchPreferencePage {

   private static final String     APP_BTN_BROWSE                   = net.tourbook.Messages.app_btn_browse;
   private static final String     DIALOG_EXPORT_DIR_DIALOG_MESSAGE = net.tourbook.Messages.dialog_export_dir_dialog_message;
   private static final String     DIALOG_EXPORT_DIR_DIALOG_TEXT    = net.tourbook.Messages.dialog_export_dir_dialog_text;
   private static final String     PARAMETER_TRAILING_CHAR          = "}";                                                   //$NON-NLS-1$
   private static final String     PARAMETER_LEADING_CHAR           = "{";                                                   //$NON-NLS-1$
   private static final String     _suuntoApp_WebPage_Link          = "https://www.suunto.com/suunto-app/suunto-app/";       //$NON-NLS-1$
   public static final String      ID                               = "net.tourbook.cloud.PrefPageSuunto";                   //$NON-NLS-1$

   public static final String      ClientId                         = "d8f3e53f-6c20-4d17-9a4e-a4930c8667e8";                //$NON-NLS-1$

   public static final int         CALLBACK_PORT                    = 4919;

   private static final String     STATE_SUUNTO_CLOUD_SELECTED_TAB  = "suuntoCloud.selectedTab";                             //$NON-NLS-1$

   private IPreferenceStore        _prefStore                       = Activator.getDefault().getPreferenceStore();
   private final IDialogSettings   _state                           = TourbookPlugin.getState(DialogEasyImportConfig.ID);
   private IPropertyChangeListener _prefChangeListener;
   private LocalHostServer         _server;
   private List<Long>              _personIds;
   private String                  _internalFileNameComponents;

   /*
    * UI controls
    */
   private CTabFolder                  _tabFolder;
   private Composite                   _partContainer;

   private Button                      _btnCleanup;
   private Button                      _btnSelectFolder;
   private Button                      _chkShowHideTokens;
   private Button                      _chkUseStartDateFilter;
   private Button                      _chkUseEndDateFilter;
   private Combo                       _comboDownloadFolderPath;
   private Combo                       _comboPeopleList;
   private DateTime                    _dtFilterStart;
   private DateTime                    _dtFilterEnd;
   private Group                       _groupCloudAccount;
   private Label                       _labelAccessToken;
   private Label                       _labelExpiresAt;
   private Label                       _labelExpiresAt_Value;
   private Label                       _labelRefreshToken;
   private Label                       _labelDownloadFolder;
   private Text                        _txtAccessToken_Value;
   private Text                        _txtCustomFileName;
   private Text                        _txtRefreshToken_Value;

   /**
    * Contains all rows with file name parts that are displayed in the UI
    */
   private final ArrayList<PartRow>    PART_ROWS  = new ArrayList<>();

   /**
    * File name parts that can be selected in the combobox for each parameter
    */
   private final ArrayList<PartUIItem> PART_ITEMS = new ArrayList<>();

   {
      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.NONE,
            WIDGET_KEY.PAGE_NONE,
            UI.EMPTY_STRING,
            UI.EMPTY_STRING));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.SUUNTO_FILE_NAME,
            WIDGET_KEY.PAGE_SUUNTO_FILE_NAME,
            Messages.Filename_Component_SuuntoName,
            Messages.Filename_Component_SuuntoName_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.FIT_EXTENSION,
            WIDGET_KEY.PAGE_FIT_EXTENSION,
            Messages.Filename_Component_FitExtension,
            Messages.Filename_Component_FitExtension_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.WORKOUT_ID,
            WIDGET_KEY.PAGE_WORKOUT_ID,
            Messages.Filename_Component_ActivityId,
            Messages.Filename_Component_ActivityId_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.ACTIVITY_TYPE,
            WIDGET_KEY.PAGE_ACTIVITY_TYPE,
            Messages.Filename_Component_ActivityType,
            Messages.Filename_Component_ActivityType_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.YEAR,
            WIDGET_KEY.PAGE_YEAR,
            Messages.Filename_Component_Year,
            Messages.Filename_Component_Year_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.MONTH,
            WIDGET_KEY.PAGE_MONTH,
            Messages.Filename_Component_Month,
            Messages.Filename_Component_Month_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.DAY,
            WIDGET_KEY.PAGE_DAY,
            Messages.Filename_Component_Day,
            Messages.Filename_Component_Day_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.HOUR,
            WIDGET_KEY.PAGE_HOUR,
            Messages.Filename_Component_Hour,
            Messages.Filename_Component_Hour_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.MINUTE,
            WIDGET_KEY.PAGE_MINUTE,
            Messages.Filename_Component_Minute,
            Messages.Filename_Component_Minute_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.USER_NAME,
            WIDGET_KEY.PAGE_USER_NAME,
            Messages.Filename_Component_UserName,
            Messages.Filename_Component_UserName_Abbr));

      PART_ITEMS.add(new PartUIItem(
            PART_TYPE.USER_TEXT,
            WIDGET_KEY.PAGE_USER_TEXT,
            Messages.Filename_Component_UserText,
            Messages.Filename_Component_UserText_Abbr));
   }

   private String buildComponentKey(final PART_TYPE partType) {
      return PARAMETER_LEADING_CHAR + partType.toString() + PARAMETER_TRAILING_CHAR;
   }

   private String buildEnhancedComponentKey(final PART_TYPE partType, final String additionalString) {
      String componentKey = PARAMETER_LEADING_CHAR + partType.toString();
      if (partType == PART_TYPE.USER_TEXT) {

         componentKey += UI.SYMBOL_COLON + StringUtils.sanitizeFileName(additionalString).trim();
      }

      componentKey += PARAMETER_TRAILING_CHAR;

      return componentKey;
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI();

      final Composite container = createUI(parent);

      restoreState();

      initializeUIFromModel();

      enableControls();

      _prefChangeListener = event -> {

         Display.getDefault().syncExec(() -> {

            final String selectedPersonId = getSelectedPersonId();

            if (!event.getProperty().equals(Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId))) {
               return;
            }

            if (!event.getOldValue().equals(event.getNewValue())) {

               _txtAccessToken_Value.setText(
                     _prefStore.getString(
                           Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId)));

               _labelExpiresAt_Value.setText(
                     OAuth2Utils.computeAccessTokenExpirationDate(
                           _prefStore.getLong(
                                 Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(selectedPersonId)),
                           _prefStore.getLong(
                                 Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(selectedPersonId)) * 1000));

               _txtRefreshToken_Value.setText(
                     _prefStore.getString(
                           Preferences.getPerson_SuuntoRefreshToken_String(selectedPersonId)));

               _groupCloudAccount.redraw();

               enableControls();
            }

            if (_server != null) {
               _server.stopCallBackServer();
            }
         });
      };

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(1).spacing(0, 15).applyTo(container);
      {
         _tabFolder = new CTabFolder(container, SWT.TOP);
         GridDataFactory.fillDefaults().grab(true, true).applyTo(_tabFolder);
         {
            final CTabItem tabCloudAccount = new CTabItem(_tabFolder, SWT.NONE);
            tabCloudAccount.setControl(createUI_100_AccountInformation(_tabFolder));
            tabCloudAccount.setText(Messages.SuuntoCloud_Group_AccountInformation);

            final CTabItem tabFileNameCustomization = new CTabItem(_tabFolder, SWT.NONE);
            tabFileNameCustomization.setControl(createUI_200_FileNameCustomization(_tabFolder));
            tabFileNameCustomization.setText(Messages.SuuntoCloud_Group_FileNameCustomization);
         }
      }

      return _tabFolder;
   }

   private Control createUI_100_AccountInformation(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      createUI_101_Authorize(container);
      createUI_102_TokensInformation(container);
      createUI_103_TourDownload(container);
      createUI_104_AccountCleanup(container);

      return container;
   }

   private void createUI_101_Authorize(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         final Label labelPerson = new Label(container, SWT.NONE);
         labelPerson.setText(Messages.PrefPage_CloudConnectivity_Label_PersonLinkedToCloudAccount);
         labelPerson.setToolTipText(Messages.PrefPage_CloudConnectivity_Label_PersonLinkedToCloudAccount_Tooltip);
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
         btnAuthorizeConnection.setText(Messages.PrefPage_CloudConnectivity_Button_Authorize);
         btnAuthorizeConnection.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onClickAuthorize()));
         GridDataFactory.fillDefaults().align(SWT.CENTER, SWT.FILL).grab(true, true).applyTo(btnAuthorizeConnection);
      }
   }

   private void createUI_102_TokensInformation(final Composite parent) {

      final PixelConverter pc = new PixelConverter(parent);

      _groupCloudAccount = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(_groupCloudAccount);
      _groupCloudAccount.setText(Messages.PrefPage_CloudConnectivity_Group_CloudAccount);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_groupCloudAccount);
      {
         {
            final Label labelWebPage = new Label(_groupCloudAccount, SWT.NONE);
            labelWebPage.setText(Messages.PrefPage_CloudConnectivity_Label_WebPage);
            GridDataFactory.fillDefaults().applyTo(labelWebPage);

            final Link linkWebPage = new Link(_groupCloudAccount, SWT.NONE);
            linkWebPage.setText(UI.LINK_TAG_START + _suuntoApp_WebPage_Link + UI.LINK_TAG_END);
            linkWebPage.setEnabled(true);
            linkWebPage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(
                  _suuntoApp_WebPage_Link)));
            GridDataFactory.fillDefaults().grab(true, false).applyTo(linkWebPage);
         }
         {
            _labelAccessToken = new Label(_groupCloudAccount, SWT.NONE);
            _labelAccessToken.setText(Messages.PrefPage_CloudConnectivity_Label_AccessToken);
            GridDataFactory.fillDefaults().applyTo(_labelAccessToken);

            _txtAccessToken_Value = new Text(_groupCloudAccount, SWT.READ_ONLY | SWT.PASSWORD);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_txtAccessToken_Value);
         }
         {
            _labelRefreshToken = new Label(_groupCloudAccount, SWT.NONE);
            _labelRefreshToken.setText(Messages.PrefPage_CloudConnectivity_Label_RefreshToken);
            GridDataFactory.fillDefaults().applyTo(_labelRefreshToken);

            _txtRefreshToken_Value = new Text(_groupCloudAccount, SWT.READ_ONLY | SWT.PASSWORD);
            GridDataFactory.fillDefaults().hint(pc.convertWidthInCharsToPixels(60), SWT.DEFAULT).applyTo(_txtRefreshToken_Value);
         }
         {
            _labelExpiresAt = new Label(_groupCloudAccount, SWT.NONE);
            _labelExpiresAt.setText(Messages.PrefPage_CloudConnectivity_Label_ExpiresAt);
            GridDataFactory.fillDefaults().applyTo(_labelExpiresAt);

            _labelExpiresAt_Value = new Label(_groupCloudAccount, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_labelExpiresAt_Value);
         }
         {
            _chkShowHideTokens = new Button(_groupCloudAccount, SWT.CHECK);
            _chkShowHideTokens.setText(Messages.PrefPage_CloudConnectivity_Checkbox_ShowOrHideTokens);
            _chkShowHideTokens.setToolTipText(Messages.PrefPage_CloudConnectivity_Checkbox_ShowOrHideTokens_Tooltip);
            _chkShowHideTokens.addSelectionListener(widgetSelectedAdapter(selectionEvent -> showOrHideAllPasswords(_chkShowHideTokens
                  .getSelection())));
            GridDataFactory.fillDefaults().applyTo(_chkShowHideTokens);
         }
      }
   }

   private void createUI_103_TourDownload(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      group.setText(Messages.PrefPage_CloudConnectivity_Group_TourDownload);
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
             * Checkbox: Use a start (or "since") date filter
             */
            _chkUseStartDateFilter = new Button(group, SWT.CHECK);
            _chkUseStartDateFilter.setText(Messages.PrefPage_SuuntoWorkouts_Checkbox_StartDateFilter);
            _chkUseStartDateFilter.setToolTipText(Messages.PrefPage_SuuntoWorkouts_DatesFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(_chkUseStartDateFilter);
            _chkUseStartDateFilter.addSelectionListener(widgetSelectedAdapter(selectionEvent -> _dtFilterStart.setEnabled(_chkUseStartDateFilter
                  .getSelection())));

            _dtFilterStart = new DateTime(group, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
            _dtFilterStart.setToolTipText(Messages.PrefPage_SuuntoWorkouts_DatesFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(_dtFilterStart);
         }

         {
            /*
             * Checkbox: Use an end date filter
             */
            _chkUseEndDateFilter = new Button(group, SWT.CHECK);
            _chkUseEndDateFilter.setText(Messages.PrefPage_SuuntoWorkouts_Checkbox_EndDateFilter);
            _chkUseEndDateFilter.setToolTipText(Messages.PrefPage_SuuntoWorkouts_DatesFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).applyTo(_chkUseEndDateFilter);
            _chkUseEndDateFilter.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> _dtFilterEnd.setEnabled(_chkUseEndDateFilter.getSelection())));

            _dtFilterEnd = new DateTime(group, SWT.DATE | SWT.MEDIUM | SWT.DROP_DOWN | SWT.BORDER);
            _dtFilterEnd.setToolTipText(Messages.PrefPage_SuuntoWorkouts_DatesFilter_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.LEFT, SWT.CENTER).span(2, 1).applyTo(_dtFilterEnd);
         }

      }
   }

   private void createUI_104_AccountCleanup(final Composite parent) {

      GridDataFactory.fillDefaults().grab(true, false).applyTo(parent);
      GridLayoutFactory.fillDefaults().applyTo(parent);
      {
         /*
          * Clean-up button
          */
         _btnCleanup = new Button(parent, SWT.NONE);
         _btnCleanup.setText(Messages.PrefPage_CloudConnectivity_Label_Cleanup);
         _btnCleanup.setToolTipText(Messages.PrefPage_CloudConnectivity_Label_Cleanup_Tooltip);
         _btnCleanup.addSelectionListener(widgetSelectedAdapter(selectionEvent -> performDefaults()));
         GridDataFactory.fillDefaults().align(SWT.END, SWT.FILL).grab(true, false).applyTo(_btnCleanup);
      }
   }

   private Composite createUI_200_FileNameCustomization(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().applyTo(container);
      {
         // Label: custom file name
         final Label label = new Label(container, SWT.NONE);
         label.setText(Messages.Dialog_DownloadWorkoutsFromSuunto_Label_CustomFilename);
         label.setToolTipText(Messages.Dialog_DownloadWorkoutsFromSuunto_Label_CustomFilename_Tooltip);
         GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

         // Text: custom file name
         _txtCustomFileName = new Text(container, SWT.BORDER | SWT.READ_ONLY);
         _txtCustomFileName.setToolTipText(Messages.Dialog_DownloadWorkoutsFromSuunto_Label_CustomFilename_Tooltip);
         _txtCustomFileName.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtCustomFileName);

         // File name parts
         _partContainer = new Composite(container, SWT.NONE);
         GridDataFactory.fillDefaults().grab(true, true).indent(0, 10).applyTo(_partContainer);
         GridLayoutFactory.fillDefaults().numColumns(2).applyTo(_partContainer);
         {
            PART_ROWS.add(createUI210PartRow(_partContainer, 0));
            PART_ROWS.add(createUI210PartRow(_partContainer, 1));
            PART_ROWS.add(createUI210PartRow(_partContainer, 2));
            PART_ROWS.add(createUI210PartRow(_partContainer, 3));
            PART_ROWS.add(createUI210PartRow(_partContainer, 4));
            PART_ROWS.add(createUI210PartRow(_partContainer, 5));
            PART_ROWS.add(createUI210PartRow(_partContainer, 6));
            PART_ROWS.add(createUI210PartRow(_partContainer, 7));
            PART_ROWS.add(createUI210PartRow(_partContainer, 8));
            PART_ROWS.add(createUI210PartRow(_partContainer, 9));
            PART_ROWS.add(createUI210PartRow(_partContainer, 10));
            PART_ROWS.add(createUI210PartRow(_partContainer, 11));
            PART_ROWS.add(createUI210PartRow(_partContainer, 12));
            PART_ROWS.add(createUI210PartRow(_partContainer, 13));
            PART_ROWS.add(createUI210PartRow(_partContainer, 14));
            PART_ROWS.add(createUI210PartRow(_partContainer, 15));
         }
      }
      return container;
   }

   private PartRow createUI210PartRow(final Composite container, final int row) {

      // combo: parameter item type
      final Combo combo = new Combo(container, SWT.READ_ONLY);
      combo.setVisibleItemCount(20);
      combo.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

         final Combo widgetCombo = (Combo) selectionEvent.widget;

         /*
          * show page according to the selected item in the combobox
          */
         final Map<WIDGET_KEY, Widget> rowWidgets = PART_ROWS.get(row).getRowWidgets();

         onSelectPart(widgetCombo, rowWidgets);
      }));

      // fill combo
      PART_ITEMS.forEach(paraItem -> combo.add(paraItem.text));

      // select default
      combo.select(0);

      /*
       * Pagebook: parameter widgets
       */
      final EnumMap<WIDGET_KEY, Widget> paraWidgets = createUI212ParaWidgets(container);

      return new PartRow(combo, paraWidgets);
   }

   private EnumMap<WIDGET_KEY, Widget> createUI212ParaWidgets(final Composite parent) {

      final EnumMap<WIDGET_KEY, Widget> paraWidgets = new EnumMap<>(WIDGET_KEY.class);

      final ModifyListener modifyListener = event -> updateCustomFileName();

      final PageBook bookParameter = new PageBook(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(bookParameter);
      paraWidgets.put(WIDGET_KEY.PAGEBOOK, bookParameter);
      {
         // Page: none
         Label label = new Label(bookParameter, SWT.NONE);
         paraWidgets.put(WIDGET_KEY.PAGE_NONE, label);

         /*
          * Page: Suunto's file name
          */
         final Composite suuntoFilenameContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(suuntoFilenameContainer);
         {
            label = new Label(suuntoFilenameContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.SUUNTO_FILE_NAME));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_SUUNTO_FILE_NAME, suuntoFilenameContainer);

/*
 * Page: FIT Extension
 */
         final Composite fitExtensionContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(fitExtensionContainer);
         {
            label = new Label(fitExtensionContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.FIT_EXTENSION));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_FIT_EXTENSION, fitExtensionContainer);

         /*
          * Page: Workout ID
          */
         final Composite workoutIdContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(workoutIdContainer);
         {
            label = new Label(workoutIdContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.WORKOUT_ID));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_WORKOUT_ID, workoutIdContainer);

         /*
          * Page: Activity Type
          */
         final Composite activityTypeContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(activityTypeContainer);
         {
            label = new Label(activityTypeContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.ACTIVITY_TYPE));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_ACTIVITY_TYPE, activityTypeContainer);

         /*
          * Page: year
          */
         final Composite yearContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(yearContainer);
         {
            label = new Label(yearContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.YEAR));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_YEAR, yearContainer);

         /*
          * Page: month
          */
         final Composite monthContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(monthContainer);
         {
            label = new Label(monthContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.MONTH));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_MONTH, monthContainer);

         /*
          * Page: day
          */
         final Composite dayContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(dayContainer);
         {
            label = new Label(dayContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.DAY));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_DAY, dayContainer);

         /*
          * Page: hour
          */
         final Composite hourContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(hourContainer);
         {
            label = new Label(hourContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.HOUR));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_HOUR, hourContainer);

         /*
          * Page: minute
          */
         final Composite minuteContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(minuteContainer);
         {
            label = new Label(minuteContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.MINUTE));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_MINUTE, minuteContainer);

         /*
          * Page: user name
          */
         final Composite userNameContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(userNameContainer);
         {
            label = new Label(userNameContainer, SWT.NONE);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(label);
            label.setText(createUI214Parameter(PART_TYPE.USER_NAME));
         }
         paraWidgets.put(WIDGET_KEY.PAGE_USER_NAME, userNameContainer);

         /*
          * Page: User text
          */
         final Composite textContainer = new Composite(bookParameter, SWT.NONE);
         GridLayoutFactory.fillDefaults().applyTo(textContainer);
         {
            final Text text = new Text(textContainer, SWT.BORDER);
            GridDataFactory.fillDefaults()//
                  .grab(true, true)
                  .align(SWT.FILL, SWT.CENTER)
                  .applyTo(text);
            text.addModifyListener(modifyListener);

            paraWidgets.put(WIDGET_KEY.TEXT_USER_TEXT, text);
         }
         paraWidgets.put(WIDGET_KEY.PAGE_USER_TEXT, textContainer);
      }

      // show hide page
      bookParameter.showPage((Control) paraWidgets.get(WIDGET_KEY.PAGE_NONE));

      return paraWidgets;
   }

   private String createUI214Parameter(final PART_TYPE itemKey) {

      for (final PartUIItem paraItem : PART_ITEMS) {
         if (paraItem.partKey == itemKey) {
            return PARAMETER_LEADING_CHAR + paraItem.abbreviation + PARAMETER_TRAILING_CHAR;
         }
      }

      StatusUtil.showStatus("invalid itemKey '" + itemKey + "'", new Exception()); //$NON-NLS-1$ //$NON-NLS-2$

      return UI.EMPTY_STRING;
   }

   private void enableControls() {

      final boolean isAuthorized = StringUtils.hasContent(_txtAccessToken_Value.getText())
            && StringUtils.hasContent(_txtRefreshToken_Value.getText());

      _labelRefreshToken.setEnabled(isAuthorized);
      _labelExpiresAt.setEnabled(isAuthorized);
      _labelAccessToken.setEnabled(isAuthorized);
      _chkShowHideTokens.setEnabled(isAuthorized);
      _labelDownloadFolder.setEnabled(isAuthorized);
      _comboDownloadFolderPath.setEnabled(isAuthorized);
      _btnSelectFolder.setEnabled(isAuthorized);
      _chkUseStartDateFilter.setEnabled(isAuthorized);
      _chkUseEndDateFilter.setEnabled(isAuthorized);
      _dtFilterStart.setEnabled(isAuthorized && _chkUseStartDateFilter.getSelection());
      _dtFilterEnd.setEnabled(isAuthorized && _chkUseEndDateFilter.getSelection());
      _btnCleanup.setEnabled(isAuthorized);
   }

   private long getFilterDate(final DateTime filterDate) {

      final int year = filterDate.getYear();
      final int month = filterDate.getMonth() + 1;
      final int day = filterDate.getDay();
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

   private long getFilterEndDate() {

      return getFilterDate(_dtFilterEnd);
   }

   private long getFilterStartDate() {

      return getFilterDate(_dtFilterStart);
   }

   private String getSelectedPersonId() {

      final int selectedPersonIndex = _comboPeopleList.getSelectionIndex();

      final String personId = selectedPersonIndex == 0
            ? UI.EMPTY_STRING
            : String.valueOf(_personIds.get(selectedPersonIndex));

      return personId;
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initializeUIFromModel() {

      final String fileNameComponent = _prefStore.getString(Preferences.SUUNTO_FILENAME_COMPONENTS);
      final List<String> fileNameComponents = CustomFileNameBuilder.extractFileNameComponents(fileNameComponent);
      updateUIFilenameComponents(fileNameComponents);
      updateCustomFileName();
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
    * so that the user can allow the MyTourbook Suunto app to have access
    * to their Suunto account.
    */
   private void onClickAuthorize() {

      if (_server != null) {
         _server.stopCallBackServer();
      }

      final SuuntoTokensRetrievalHandler tokensRetrievalHandler =
            new SuuntoTokensRetrievalHandler(getSelectedPersonId());
      _server = new LocalHostServer(CALLBACK_PORT, "Suunto", _prefChangeListener); //$NON-NLS-1$
      final boolean isServerCreated = _server.createCallBackServer(tokensRetrievalHandler);

      if (!isServerCreated) {
         return;
      }

      final URIBuilder authorizeUrlBuilder = new URIBuilder();
      authorizeUrlBuilder.setScheme("https"); //$NON-NLS-1$
      authorizeUrlBuilder.setHost("cloudapi-oauth.suunto.com"); //$NON-NLS-1$
      authorizeUrlBuilder.setPath("/oauth/authorize"); //$NON-NLS-1$
      authorizeUrlBuilder.addParameter(
            OAuth2Constants.PARAM_RESPONSE_TYPE,
            OAuth2Constants.PARAM_CODE);
      authorizeUrlBuilder.addParameter(
            OAuth2Constants.PARAM_CLIENT_ID,
            ClientId);
      authorizeUrlBuilder.addParameter(
            OAuth2Constants.PARAM_REDIRECT_URI,
            "http://localhost:" + CALLBACK_PORT); //$NON-NLS-1$
      try {
         final String authorizeUrl = authorizeUrlBuilder.build().toString();

         Display.getDefault().syncExec(() -> WEB.openUrl(authorizeUrl));
      } catch (final URISyntaxException e) {
         StatusUtil.log(e);
      }
   }

   private void onSelectBrowseDirectory() {

      final DirectoryDialog dialog = new DirectoryDialog(
            Display.getCurrent().getActiveShell(),
            SWT.SAVE);
      dialog.setText(DIALOG_EXPORT_DIR_DIALOG_TEXT);
      dialog.setMessage(DIALOG_EXPORT_DIR_DIALOG_MESSAGE);

      final String selectedDirectoryName = dialog.open();

      if (selectedDirectoryName != null) {

         setErrorMessage(null);
         _comboDownloadFolderPath.setText(selectedDirectoryName);
      }
   }

   /**
    * Shows the part page which is selected in the combo
    *
    * @param combo
    * @param rowWidgets
    */
   private void onSelectPart(final Combo combo, final Map<WIDGET_KEY, Widget> rowWidgets) {

      final PartUIItem selectedPartItem = PART_ITEMS.get(combo.getSelectionIndex());

      final PageBook pagebook = (PageBook) rowWidgets.get(WIDGET_KEY.PAGEBOOK);
      final Widget page = rowWidgets.get(selectedPartItem.widgetKey);

      pagebook.showPage((Control) page);

      _partContainer.layout();

      updateCustomFileName();
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
      _comboPeopleList.select(
            _prefStore.getDefaultInt(Preferences.SUUNTO_SELECTED_PERSON_INDEX));

      final String selectedPersonId =
            _prefStore.getDefaultString(Preferences.SUUNTO_SELECTED_PERSON_ID);

      _txtAccessToken_Value.setText(
            _prefStore.getDefaultString(
                  Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId)));
      _labelExpiresAt_Value.setText(UI.EMPTY_STRING);
      _txtRefreshToken_Value.setText(
            _prefStore.getDefaultString(
                  Preferences.getPerson_SuuntoRefreshToken_String(selectedPersonId)));

      _comboDownloadFolderPath.setText(
            _prefStore.getDefaultString(
                  Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(selectedPersonId)));

      _chkUseStartDateFilter.setSelection(
            _prefStore.getDefaultBoolean(
                  Preferences.getPerson_SuuntoUseWorkoutFilterStartDate_String(selectedPersonId)));
      setFilterSinceDate(_prefStore.getDefaultLong(
            Preferences.getPerson_SuuntoWorkoutFilterStartDate_String(selectedPersonId)));
      _chkUseEndDateFilter.setSelection(
            _prefStore.getDefaultBoolean(
                  Preferences.getPerson_SuuntoUseWorkoutFilterEndDate_String(selectedPersonId)));
      setFilterEndDate(_prefStore.getDefaultLong(
            Preferences.getPerson_SuuntoWorkoutFilterEndDate_String(selectedPersonId)));

      final String suuntoFilenameComponents = _prefStore.getDefaultString(Preferences.SUUNTO_FILENAME_COMPONENTS);
      _txtCustomFileName.setText(suuntoFilenameComponents);
      _internalFileNameComponents = suuntoFilenameComponents;
      initializeUIFromModel();

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

      tourPersonIds.forEach(tourPersonId -> {
         _prefStore.setValue(
               Preferences.getPerson_SuuntoAccessToken_String(tourPersonId),
               UI.EMPTY_STRING);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoRefreshToken_String(tourPersonId),
               UI.EMPTY_STRING);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(tourPersonId),
               0L);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(tourPersonId),
               0L);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(tourPersonId),
               UI.EMPTY_STRING);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoUseWorkoutFilterStartDate_String(tourPersonId),
               false);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoWorkoutFilterStartDate_String(tourPersonId),
               PreferenceInitializer.SUUNTO_FILTER_SINCE_DATE);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoUseWorkoutFilterEndDate_String(tourPersonId),
               false);
         _prefStore.setValue(
               Preferences.getPerson_SuuntoWorkoutFilterEndDate_String(tourPersonId),
               PreferenceInitializer.SUUNTO_FILTER_END_DATE);
      });

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {

         final String personId = getSelectedPersonId();

         _prefStore.setValue(
               Preferences.getPerson_SuuntoAccessToken_String(personId),
               _txtAccessToken_Value.getText());
         _prefStore.setValue(
               Preferences.getPerson_SuuntoRefreshToken_String(personId),
               _txtRefreshToken_Value.getText());

         if (StringUtils.isNullOrEmpty(_labelExpiresAt_Value.getText())) {
            _prefStore.setValue(
                  Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(personId),
                  0L);
            _prefStore.setValue(
                  Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(personId),
                  0L);
         }

         if (_server != null) {
            _server.stopCallBackServer();
         }

         final String downloadFolder = _comboDownloadFolderPath.getText();
         _prefStore.setValue(Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(personId), downloadFolder);
         if (StringUtils.hasContent(downloadFolder)) {

            final String[] currentDeviceFolderHistoryItems = _state.getArray(
                  DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS);
            final List<String> stateDeviceFolderHistoryItems =
                  currentDeviceFolderHistoryItems != null
                        ? new ArrayList<>(Arrays.asList(currentDeviceFolderHistoryItems))
                        : new ArrayList<>();

            if (!stateDeviceFolderHistoryItems.contains(downloadFolder)) {
               stateDeviceFolderHistoryItems.add(downloadFolder);
               _state.put(DialogEasyImportConfig.STATE_DEVICE_FOLDER_HISTORY_ITEMS,
                     stateDeviceFolderHistoryItems.toArray(new String[stateDeviceFolderHistoryItems.size()]));
            }
         }

         _prefStore.setValue(
               Preferences.getPerson_SuuntoUseWorkoutFilterStartDate_String(personId),
               _chkUseStartDateFilter.getSelection());
         _prefStore.setValue(
               Preferences.getPerson_SuuntoWorkoutFilterStartDate_String(personId),
               getFilterStartDate());
         _prefStore.setValue(
               Preferences.getPerson_SuuntoUseWorkoutFilterEndDate_String(personId),
               _chkUseEndDateFilter.getSelection());
         _prefStore.setValue(
               Preferences.getPerson_SuuntoWorkoutFilterEndDate_String(personId),
               getFilterEndDate());

         final int selectedPersonIndex = _comboPeopleList.getSelectionIndex();
         _prefStore.setValue(Preferences.SUUNTO_SELECTED_PERSON_INDEX, selectedPersonIndex);
         _prefStore.setValue(Preferences.SUUNTO_SELECTED_PERSON_ID, personId);

         _prefStore.setValue(STATE_SUUNTO_CLOUD_SELECTED_TAB, _tabFolder.getSelectionIndex());

         _prefStore.setValue(Preferences.SUUNTO_FILENAME_COMPONENTS, _internalFileNameComponents);
      }

      return isOK;
   }

   private void restoreAccountInformation(final String selectedPersonId) {

      _txtAccessToken_Value.setText(
            _prefStore.getString(
                  Preferences.getPerson_SuuntoAccessToken_String(selectedPersonId)));
      _labelExpiresAt_Value.setText(
            OAuth2Utils.computeAccessTokenExpirationDate(
                  _prefStore.getLong(
                        Preferences.getPerson_SuuntoAccessTokenIssueDateTime_String(
                              selectedPersonId)),
                  _prefStore.getLong(
                        Preferences.getPerson_SuuntoAccessTokenExpiresIn_String(
                              selectedPersonId)) * 1000));
      _txtRefreshToken_Value.setText(
            _prefStore.getString(
                  Preferences.getPerson_SuuntoRefreshToken_String(selectedPersonId)));

      _comboDownloadFolderPath.setText(
            _prefStore.getString(
                  Preferences.getPerson_SuuntoWorkoutDownloadFolder_String(selectedPersonId)));

      _chkUseStartDateFilter.setSelection(
            _prefStore.getBoolean(
                  Preferences.getPerson_SuuntoUseWorkoutFilterStartDate_String(selectedPersonId)));
      setFilterSinceDate(
            _prefStore.getLong(
                  Preferences.getPerson_SuuntoWorkoutFilterStartDate_String(selectedPersonId)));
      _chkUseEndDateFilter.setSelection(
            _prefStore.getBoolean(
                  Preferences.getPerson_SuuntoUseWorkoutFilterEndDate_String(selectedPersonId)));
      setFilterEndDate(
            _prefStore.getLong(
                  Preferences.getPerson_SuuntoWorkoutFilterEndDate_String(selectedPersonId)));
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

      _tabFolder.setSelection(_prefStore.getInt(STATE_SUUNTO_CLOUD_SELECTED_TAB));
   }

   /**
    * select part type in the row combo
    */
   private int selectPartType(final PartRow partRow, final PART_TYPE partType) {

      int partTypeIndex = 0;
      for (final PartUIItem partItem : PART_ITEMS) {
         if (partItem.partKey == partType) {
            break;
         }
         partTypeIndex++;
      }

      final Combo rowCombo = partRow.getRowCombo();
      rowCombo.select(partTypeIndex);

      onSelectPart(rowCombo, partRow.getRowWidgets());

      return partTypeIndex;
   }

   private void setFilterEndDate(final long filterEndDate) {

      final LocalDate suuntoFileDownloadEndDate = TimeTools.toLocalDate(filterEndDate);

      _dtFilterEnd.setDate(suuntoFileDownloadEndDate.getYear(),
            suuntoFileDownloadEndDate.getMonthValue() - 1,
            suuntoFileDownloadEndDate.getDayOfMonth());
   }

   private void setFilterSinceDate(final long filterSinceDate) {

      final LocalDate suuntoFileDownloadSinceDate = TimeTools.toLocalDate(filterSinceDate);

      _dtFilterStart.setDate(suuntoFileDownloadSinceDate.getYear(),
            suuntoFileDownloadSinceDate.getMonthValue() - 1,
            suuntoFileDownloadSinceDate.getDayOfMonth());
   }

   private void showOrHideAllPasswords(final boolean showPasswords) {

      final List<Text> texts = new ArrayList<>();
      texts.add(_txtAccessToken_Value);
      texts.add(_txtRefreshToken_Value);

      Preferences.showOrHidePasswords(texts, showPasswords);
   }

   /**
    * Update the custom file name
    */
   private void updateCustomFileName() {

      final StringBuilder stringBuilder = new StringBuilder();
      final StringBuilder internalFileNameComponentsBuilder = new StringBuilder();

      for (final PartRow row : PART_ROWS) {

         final Map<WIDGET_KEY, Widget> rowWidgets = row.getRowWidgets();
         final PartUIItem selectedParaItem = PART_ITEMS.get(row.getRowCombo().getSelectionIndex());

         switch (selectedParaItem.partKey) {

         case SUUNTO_FILE_NAME:
            stringBuilder.append(createUI214Parameter(PART_TYPE.SUUNTO_FILE_NAME));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.SUUNTO_FILE_NAME));
            break;

         case FIT_EXTENSION:
            stringBuilder.append(UI.SYMBOL_DOT + createUI214Parameter(PART_TYPE.FIT_EXTENSION));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.FIT_EXTENSION));
            break;

         case WORKOUT_ID:
            stringBuilder.append(createUI214Parameter(PART_TYPE.WORKOUT_ID));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.WORKOUT_ID));
            break;

         case ACTIVITY_TYPE:
            stringBuilder.append(createUI214Parameter(PART_TYPE.ACTIVITY_TYPE));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.ACTIVITY_TYPE));
            break;

         case YEAR:
            stringBuilder.append(createUI214Parameter(PART_TYPE.YEAR));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.YEAR));
            break;

         case MONTH:
            stringBuilder.append(createUI214Parameter(PART_TYPE.MONTH));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.MONTH));
            break;

         case DAY:
            stringBuilder.append(createUI214Parameter(PART_TYPE.DAY));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.DAY));
            break;

         case HOUR:
            stringBuilder.append(createUI214Parameter(PART_TYPE.HOUR));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.HOUR));
            break;

         case MINUTE:
            stringBuilder.append(createUI214Parameter(PART_TYPE.MINUTE));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.MINUTE));
            break;

         case USER_NAME:
            stringBuilder.append(createUI214Parameter(PART_TYPE.USER_NAME));
            internalFileNameComponentsBuilder.append(buildComponentKey(PART_TYPE.USER_NAME));
            break;

         case USER_TEXT:
            final Text txtHtml = (Text) rowWidgets.get(WIDGET_KEY.TEXT_USER_TEXT);
            stringBuilder.append(txtHtml.getText());
            internalFileNameComponentsBuilder.append(buildEnhancedComponentKey(PART_TYPE.USER_TEXT, txtHtml.getText()));
            break;

         default:
            break;
         }
      }

      _internalFileNameComponents = internalFileNameComponentsBuilder.toString();
      _txtCustomFileName.setText(stringBuilder.toString());
   }

   private void updateUIFilenameComponents(final List<String> fileNameComponents) {

      int rowIndex = 0;
      for (final String fileNameComponent : fileNameComponents) {

         // check bounds
         if (rowIndex >= PART_ROWS.size()) {
            StatusUtil.log("there are too few part rows", new Exception()); //$NON-NLS-1$
            break;
         }

         final PartRow partRow = PART_ROWS.get(rowIndex++);
         final PART_TYPE partType = CustomFileNameBuilder.getPartTypeFromComponent(fileNameComponent);

         // display part widget (page/input widget)
         selectPartType(partRow, partType);

         if (partType == PART_TYPE.USER_TEXT) {
            final Text txtHtml = (Text) partRow.getRowWidgets().get(WIDGET_KEY.TEXT_USER_TEXT);
            txtHtml.setText(fileNameComponent.substring(
                  fileNameComponent.indexOf(UI.SYMBOL_COLON) + 1));
         }
      }

      // hide part rows which are not used
      rowIndex = rowIndex < 0 ? 0 : rowIndex;
      for (int partRowIndex = rowIndex; partRowIndex < PART_ROWS.size(); partRowIndex++) {
         final PartRow partRow = PART_ROWS.get(partRowIndex);
         selectPartType(partRow, PART_TYPE.NONE);
      }

   }
}
