/*******************************************************************************
 * Copyright (C) 2005, 2021 Wolfgang Schramm and Contributors
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

import java.io.InputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
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
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageSRTMData extends PreferencePage implements IWorkbenchPreferencePage {

// SET_FORMATTING_OFF

   private static final String HTTPS_NASA_EARTHDATA_LOGIN = "https://urs.earthdata.nasa.gov/home"; //$NON-NLS-1$
   private static final String HTTPS_NASA_TEST_URL        = "https://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL3.003/2000.02.11/N10E012.SRTMGL3.hgt.zip.xml";      //$NON-NLS-1$
// private static final String HTTPS_NASA_TEST_URL        = "https://e4ftl01.cr.usgs.gov/MEASURES/SRTMGL1.003/2000.02.11/S20E120.SRTMGL1.hgt.zip.xml";

   // Old url for SRTM 3 data
   //	http://dds.cr.usgs.gov/srtm/version2_1/SRTM3/Eurasia/N47E008.hgt.zip

// SET_FORMATTING_ON

//   private static HttpClient    _httpClient          = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

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

   private Text      _txtSRTM_Username;
   private Text      _txtSRTM_Password;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);
      createUI(parent);

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
      {
         createUI_10_CacheSettings(_prefContainer);
         createUI_20_SRTM(_prefContainer);
      }
   }

   private void createUI_10_CacheSettings(final Composite parent) {

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
            _useDefaultLocation.setPropertyChangeListener(new IPropertyChangeListener() {
               @Override
               public void propertyChange(final PropertyChangeEvent event) {
                  enableControls();
               }
            });
            new Label(group, SWT.NONE);
         }
         {
            /*
             * SRTM data filepath
             */
            _pathContainer = new Composite(group, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).span(3, 1).applyTo(_pathContainer);
            {
               _dataPathEditor = new DirectoryFieldEditor(
                     IPreferences.SRTM_DATA_FILEPATH,
                     Messages.prefPage_srtm_editor_data_filepath,
                     _pathContainer);
               _dataPathEditor.setPage(this);
               _dataPathEditor.setPreferenceStore(_prefStore);
               _dataPathEditor.setEmptyStringAllowed(false);
               _dataPathEditor.setPropertyChangeListener(new IPropertyChangeListener() {
                  @Override
                  public void propertyChange(final PropertyChangeEvent event) {
                     validateData();
                  }
               });
            }
         }
      }

      // !!! set layout after the editor was created because the editor sets the parents layout
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
   }

   private void createUI_20_SRTM(final Composite parent) {

      final GridDataFactory inputLayout = GridDataFactory.fillDefaults()
            .align(SWT.BEGINNING, SWT.FILL)
            .hint(_pc.convertWidthInCharsToPixels(30), SWT.DEFAULT);

      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.prefPage_srtm_group_label_srtm3);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
//      group.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_YELLOW));
      {
         {
            /*
             * Username
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.PrefPage_SRTM_Label_Username);

            _txtSRTM_Username = new Text(group, SWT.BORDER);
            inputLayout.applyTo(_txtSRTM_Username);
         }
         {
            /*
             * Password
             */
            final Label label = new Label(group, SWT.NONE);
            label.setText(Messages.PrefPage_SRTM_Label_Password);

            _txtSRTM_Password = new Text(group, SWT.BORDER | SWT.PASSWORD);
            inputLayout.applyTo(_txtSRTM_Password);
         }
         {
            /*
             * Link: NASA Earthdata user profile
             */
            final Link link = new Link(group, SWT.NONE);
            link.setText(NLS.bind(Messages.PrefPage_SRTM_Link_EarthdataUserProfile, HTTPS_NASA_EARTHDATA_LOGIN));
            link.setToolTipText(HTTPS_NASA_EARTHDATA_LOGIN);
            link.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  WEB.openUrl(HTTPS_NASA_EARTHDATA_LOGIN);
               }
            });
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .indent(0, 10)
                  .applyTo(link);
         }
         {
            /*
             * Test connection
             */
            final Button btnTestConnection = new Button(group, SWT.NONE);
            btnTestConnection.setText(Messages.prefPage_srtm_button_testConnection);
            btnTestConnection.addSelectionListener(new SelectionAdapter() {
               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onCheckConnection();
               }
            });
            GridDataFactory.swtDefaults()
                  .indent(0, 10)
                  .span(2, 1)
                  .applyTo(btnTestConnection);
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

   private void onCheckConnection() {

      // ensure username and password are saved
      saveState();

      BusyIndicator.showWhile(Display.getCurrent(), () -> {

         /*
          * Set up a cookie handler to maintain session cookies. A custom
          * CookiePolicy could be used to limit cookies to just the resource
          * server and URS.
          */
         CookieHandler.setDefault(new CookieManager(null, CookiePolicy.ACCEPT_ALL));

         final String password = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD);
         final String username = _prefStore.getString(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME);

         try (final InputStream inputStream = new DownloadSRTM3().getResource(HTTPS_NASA_TEST_URL, username, password)) {

            MessageDialog.openInformation(
                  _prefContainer.getShell(),
                  Messages.prefPage_srtm_checkHTTPConnection_title,
                  NLS.bind(Messages.prefPage_srtm_checkHTTPConnectionOK_message, HTTPS_NASA_TEST_URL));

         } catch (final Exception e) {

            MessageDialog.openInformation(

                  _prefContainer.getShell(),
                  Messages.prefPage_srtm_checkHTTPConnection_title,

                  NLS.bind(Messages.prefPage_srtm_checkHTTPConnection_message, HTTPS_NASA_TEST_URL)

                        + UI.NEW_LINE2 + e.getMessage());

            e.printStackTrace();
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

      _prefStore.setValue(IPreferences.NASA_EARTHDATA_LOGIN_PASSWORD, _txtSRTM_Password.getText().trim());
      _prefStore.setValue(IPreferences.NASA_EARTHDATA_LOGIN_USER_NAME, _txtSRTM_Username.getText().trim());
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
