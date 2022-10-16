/*******************************************************************************
 * Copyright (C) 2005, 2022 Wolfgang Schramm and Contributors
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
package net.tourbook.srtm;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.io.InputStream;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.time.TimeTools;
import net.tourbook.common.util.StatusUtil;
import net.tourbook.srtm.download.DownloadSRTM3;
import net.tourbook.web.WEB;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageSRTMData extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String ID                      = "net.tourbook.srtm.PrefPageSRTMData";//$NON-NLS-1$

   public static final String FOCUS_USER_NAME         = "focusUsername";                     //$NON-NLS-1$
   public static final String FOCUS_VALIDATE_DOWNLOAD = "focusValidateDownload";             //$NON-NLS-1$

// SET_FORMATTING_OFF

   private static final String HTTPS_NASA_EARTHDATA_LOGIN = "https://urs.earthdata.nasa.gov/home"; //$NON-NLS-1$
   private static final String HTTPS_NASA_TEST_URL        = "https://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL3.003/2000.02.11/N10E012.SRTMGL3.hgt.zip.xml";      //$NON-NLS-1$


   // Old url for SRTM 3 data
   //	http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/Eurasia/N47E008.hgt.zip

// SET_FORMATTING_ON

   private IPreferenceStore     _prefStore           = TourbookPlugin.getPrefStore();

   private final String         _defaultSRTMFilePath = Platform.getInstanceLocation().getURL().getPath();

   private BooleanFieldEditor   _useDefaultLocation;
   private DirectoryFieldEditor _dataPathEditor;

   private PixelConverter       _pc;

   /*
    * UI controls
    */
   private Composite _prefContainer;
   private Composite _pathContainer;

   private Button    _btnResetValidation;
   private Button    _btnValidateDownloadOfSRTMData;

   private Label     _lblSRTMValidation;

   private Text      _txtSRTM_Username;
   private Text      _txtSRTM_Password;

   @Override
   public void applyData(final Object data) {

      // run async otherwise the button do not have the focus !!!
      _prefContainer.getDisplay().asyncExec(() -> {

         if (FOCUS_USER_NAME.equals(data)) {

            // set focus to username
            _txtSRTM_Username.setFocus();

         } else if (FOCUS_VALIDATE_DOWNLOAD.equals(data)) {

            // set focus to validation button
            _btnValidateDownloadOfSRTMData.setFocus();
         }
      });
   }

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);
      createUI(parent);

      updateUI_AccountValidation();
      restoreState();

      enableControls();

      /*
       * hide error messages, it appears when the srtm data path is invalid
       */
      if (_useDefaultLocation.getBooleanValue() == false) {
         setErrorMessage(null);
      }
      return _prefContainer;
   }

   private void createUI(final Composite parent) {

      _prefContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.swtDefaults().grab(true, false).applyTo(_prefContainer);
      GridLayoutFactory.fillDefaults().applyTo(_prefContainer);
      GridDataFactory.swtDefaults().applyTo(_prefContainer);
//      _prefContainer.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         createUI_10_LocalCache(_prefContainer);
         createUI_20_ServerAccount(_prefContainer);
      }
   }

   private void createUI_10_LocalCache(final Composite parent) {

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.prefPage_srtm_group_label_data_location);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      {
         {
            /*
             * Default location
             */
            _useDefaultLocation = new BooleanFieldEditor(
                  IPreferences.SRTM_USE_DEFAULT_DATA_FILEPATH,
                  Messages.prefPage_srtm_chk_use_default_location,
                  group);
            _useDefaultLocation.setPage(this);
            _useDefaultLocation.setPreferenceStore(_prefStore);
            _useDefaultLocation.setPropertyChangeListener(propertyChangeEvent -> enableControls());
            new Label(group, SWT.NONE);
         }
         {
            /*
             * SRTM data filepath
             */
            _pathContainer = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .hint(_pc.convertWidthInCharsToPixels(40), SWT.DEFAULT)
                  .span(3, 1)
                  .applyTo(_pathContainer);
            {
               _dataPathEditor = new DirectoryFieldEditor(
                     IPreferences.SRTM_DATA_FILEPATH,
                     Messages.prefPage_srtm_editor_data_filepath,
                     _pathContainer);
               _dataPathEditor.setPage(this);
               _dataPathEditor.setPreferenceStore(_prefStore);
               _dataPathEditor.setEmptyStringAllowed(false);
               _dataPathEditor.setPropertyChangeListener(propertyChangeEvent -> validateData());
            }
         }
      }

      // !!! set layout after the editor was created because the editor sets the parents layout
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
   }

   private void createUI_20_ServerAccount(final Composite parent) {

      final int defaultCommentWidth = _pc.convertWidthInCharsToPixels(40);

      final GridDataFactory inputFieldLayout = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.FILL)
            .hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.PrefPage_SRTMData_Group_SrtmServerAccount);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//      group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         {
            /*
             * Link/Info: How enable SRTM download
             */
            final Link link = new Link(group, SWT.NONE);
            link.setText(NLS.bind(Messages.PrefPage_SRTMData_Link_AccountInfo, HTTPS_NASA_EARTHDATA_LOGIN));
            link.setToolTipText(HTTPS_NASA_EARTHDATA_LOGIN);
            link.addSelectionListener(widgetSelectedAdapter(selectionEvent -> WEB.openUrl(HTTPS_NASA_EARTHDATA_LOGIN)));
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .hint(defaultCommentWidth, SWT.DEFAULT)
                  .applyTo(link);
         }
         UI.createSpacer_Horizontal(group, 2);
         {
            /*
             * Username
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.PrefPage_SRTMData_Label_Username);

            _txtSRTM_Username = new Text(group, SWT.BORDER);
            _txtSRTM_Username.addModifyListener(modifyEvent -> enableControls());
            inputFieldLayout.applyTo(_txtSRTM_Username);
         }
         {
            /*
             * Password
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.PrefPage_SRTMData_Label_Password);

            _txtSRTM_Password = new Text(group, SWT.BORDER | SWT.PASSWORD);
            _txtSRTM_Password.addModifyListener(modifyEvent -> onModifyPassword());
            inputFieldLayout.applyTo(_txtSRTM_Password);
         }
         {
            final Composite container = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .span(2, 1)
                  .indent(0, 10)
                  .applyTo(container);
            GridLayoutFactory.fillDefaults()
                  .numColumns(3)
                  .applyTo(container);
            {
               {
                  /*
                   * Validate download of SRTM data files
                   */
                  _btnValidateDownloadOfSRTMData = new Button(container, SWT.NONE);
                  _btnValidateDownloadOfSRTMData.setText(Messages.PrefPage_SRTMData_Button_ValidateDownloadOfSrtmData);
                  _btnValidateDownloadOfSRTMData.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_ValidateSrtmDownload()));
               }
               {
                  /*
                   * Reset validation
                   */
                  _btnResetValidation = new Button(container, SWT.NONE);
                  _btnResetValidation.setText(Messages.PrefPage_SRTMData_Button_ResetValidation);
                  _btnResetValidation.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_ResetValidation()));
               }
               {
                  /*
                   * Allow dummy validation, this can be helpful when validation is currently not
                   * working or to use already downloaded SRTM files
                   */
                  final Button btnSrtmDummyValidation = new Button(container, SWT.NONE);
                  btnSrtmDummyValidation.setText(Messages.PrefPage_SRTMData_Button_SrtmDummyValidation);
                  btnSrtmDummyValidation.setToolTipText(Messages.PrefPage_SRTMData_Button_SrtmDummyValidation_Tooltip);
                  btnSrtmDummyValidation.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_DummyValidation()));
               }
            }
         }
         {
            /*
             * Account validation
             */
            _lblSRTMValidation = new Label(group, SWT.WRAP);

            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .grab(true, false)
                  .hint(defaultCommentWidth, SWT.DEFAULT)
                  .applyTo(_lblSRTMValidation);
         }
      }
   }

   @Override
   protected IPreferenceStore doGetPreferenceStore() {
      return _prefStore;
   }

   private void enableControls() {

      final boolean useDefaultLocation = _useDefaultLocation.getBooleanValue();

      if (useDefaultLocation) {
         _dataPathEditor.setEnabled(false, _pathContainer);
         _dataPathEditor.setStringValue(_defaultSRTMFilePath);
      } else {
         _dataPathEditor.setEnabled(true, _pathContainer);
      }

      final String username = _txtSRTM_Username.getText();
      final String password = _txtSRTM_Password.getText();

      final String usernameTrimmed = username.trim();
      final String passwordTrimmed = password.trim();

      final long validationDate = _prefStore.getLong(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE);

      final boolean isValidationDateSet = validationDate != Long.MIN_VALUE;
      final boolean isAccountDataSet = usernameTrimmed.length() > 0 && passwordTrimmed.length() > 0;
      final boolean canResetValidation = username.length() > 0 || password.length() > 0 || isValidationDateSet;

      _btnResetValidation.setEnabled(canResetValidation);
      _btnValidateDownloadOfSRTMData.setEnabled(isAccountDataSet);
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);
   }

   @Override
   public boolean okToLeave() {

      if (validateData() == false) {
         return false;
      }

      return super.okToLeave();
   }

   private void onModifyPassword() {

      final String passwordText = _txtSRTM_Password.getText();

      _txtSRTM_Password.setToolTipText(passwordText.length() == 0
            ? Messages.PrefPage_SRTMData_Info_EmptyPassword
            : passwordText);

      enableControls();
   }

   private void onSelect_DummyValidation() {

      final String password = _txtSRTM_Password.getText().trim();
      final String username = _txtSRTM_Username.getText().trim();

      if (UI.EMPTY_STRING.equals(password)) {
         _txtSRTM_Password.setText(Messages.PrefPage_SRTMData_Info_DummyPassword);
      }

      if (UI.EMPTY_STRING.equals(username)) {
         _txtSRTM_Username.setText(Messages.PrefPage_SRTMData_Info_DummyUsername);
      }

      _prefStore.setValue(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE, TimeTools.nowInMilliseconds());

      updateUI_AccountValidation();
   }

   private void onSelect_ResetValidation() {

      _txtSRTM_Password.setText(UI.EMPTY_STRING);
      _txtSRTM_Username.setText(UI.EMPTY_STRING);

      _prefStore.setValue(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE, Long.MIN_VALUE);

      updateUI_AccountValidation();

      enableControls();
   }

   private void onSelect_ValidateSrtmDownload() {

      BusyIndicator.showWhile(_prefContainer.getDisplay(), () -> {

         final String password = _txtSRTM_Password.getText().trim();
         final String username = _txtSRTM_Username.getText().trim();

         try (final InputStream inputStream = new DownloadSRTM3().getResource(HTTPS_NASA_TEST_URL, username, password)) {

            // set validation time, this is used to check (enable actions) if a user can download SRTM data
            _prefStore.setValue(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE, TimeTools.nowInMilliseconds());

            updateUI_AccountValidation();

            MessageDialog.openInformation(
                  _prefContainer.getShell(),
                  Messages.PrefPage_SRTMData_Dialog_ValidateSrtmDownload_Title,
                  NLS.bind(Messages.PrefPage_SRTMData_Dialog_ValidateSrtmDownload_OK_Message, HTTPS_NASA_TEST_URL));

         } catch (final Exception e) {

            // discard validation
            _prefStore.setValue(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE, Long.MIN_VALUE);

            updateUI_AccountValidation();

            MessageDialog.openInformation(

                  _prefContainer.getShell(),
                  Messages.PrefPage_SRTMData_Dialog_ValidateSrtmDownload_Title,
                  NLS.bind(Messages.PrefPage_SRTMData_Dialog_ValidateSrtmDownload_Error_Message,
                        HTTPS_NASA_TEST_URL,
                        e.getMessage()));

            StatusUtil.log(e);
         }
      });
   }

   @Override
   protected void performDefaults() {

      _useDefaultLocation.loadDefault();

      _txtSRTM_Username.setText(UI.EMPTY_STRING);
      _txtSRTM_Password.setText(UI.EMPTY_STRING);

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      if (_useDefaultLocation == null) {

         // page is not initialized this case happened and created a NPE
         return super.performOk();
      }

      if (validateData() == false) {
         return false;
      }

      saveState();

      return super.performOk();
   }

   private void restoreState() {

      _useDefaultLocation.load();
      _dataPathEditor.load();

      _txtSRTM_Password.setText(_prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD));
      _txtSRTM_Username.setText(_prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME));
   }

   private void saveState() {

      _useDefaultLocation.store();
      _dataPathEditor.store();

      final String password = _txtSRTM_Password.getText().trim();
      final String username = _txtSRTM_Username.getText().trim();

      _prefStore.setValue(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD, password);
      _prefStore.setValue(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME, username);

      if (password.length() == 0 || username.length() == 0) {

         // reset validation
         _prefStore.setValue(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE, Long.MIN_VALUE);

         updateUI_AccountValidation();
      }
   }

   private void updateUI_AccountValidation() {

      if (_prefContainer.isDisposed()) {
         return;
      }

      final long validationDate = _prefStore.getLong(IPreferences.NASA_EARTHDATA_ACCOUNT_VALIDATION_DATE);

      final String validationText = validationDate == Long.MIN_VALUE

            ? Messages.PrefPage_SRTMData_Label_AccountValidation_NO

            : NLS.bind(Messages.PrefPage_SRTMData_Label_AccountValidation_YES,
                  TimeTools.Formatter_DateTime_M.format(TimeTools.getZonedDateTime(validationDate)));

      _lblSRTMValidation.setText(validationText);

      // the validation text can have different heights -> relayout to have no vertical gaps
      _prefContainer.layout(true, true);
   }

   private boolean validateData() {

      boolean isValid = true;

      if (_useDefaultLocation.getBooleanValue() == false
            && (!_dataPathEditor.isValid() || _dataPathEditor.getStringValue().trim().length() == 0)) {

         isValid = false;

         setErrorMessage(Messages.prefPage_srtm_msg_invalid_data_path);

         _dataPathEditor.setFocus();
      }

      if (isValid) {
         setErrorMessage(null);
      }

      setValid(isValid);

      return isValid;
   }

}
