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

import net.tourbook.cloud.Activator;
import net.tourbook.cloud.IPreferences;
import net.tourbook.cloud.oauth2.OAuth2Client;
import net.tourbook.cloud.oauth2.OAuth2RequestAction;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageDropbox extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

   public static final String ID         = "net.tourbook.cloud.PrefPageDropbox";       //$NON-NLS-1$

   private IPreferenceStore   _prefStore = Activator.getDefault().getPreferenceStore();
   /*
    * UI controls
    */
   private Button             _btnAuthorizeConnection;
   private Text               _textAccessToken;
   private Button             _btnChooseFolder;
   private Text               _textFolderPath;

   @Override
   protected void createFieldEditors() {

      createUI();

      restoreState();
   }

   private void createUI() {

      final Composite parent = getFieldEditorParent();
      GridLayoutFactory.fillDefaults().applyTo(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults().numColumns(2).applyTo(container);
      {
         {
            /*
             * Authorize button
             */
            _btnAuthorizeConnection = new Button(container, SWT.NONE);
            setButtonLayoutData(_btnAuthorizeConnection);
            _btnAuthorizeConnection.setText(Messages.Pref_CloudConnectivity_Dropbox_Button_Authorize);
            _btnAuthorizeConnection.addSelectionListener(new SelectionAdapter() {

               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onClickAuthorize();
               }
            });

            /*
             * Access Token
             */
            _textAccessToken = new Text(container, SWT.BORDER);
            _textAccessToken.setEditable(false);
            _textAccessToken.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Tooltip);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_textAccessToken);
         }
         {
            /*
             * Choose Dropbox folder
             */
            _btnChooseFolder = new Button(container, SWT.NONE);
            setButtonLayoutData(_btnChooseFolder);
            _btnChooseFolder.setEnabled(false);
            _btnChooseFolder.setText(Messages.Pref_CloudConnectivity_Dropbox_Button_ChooseFolder);
            _btnChooseFolder.addSelectionListener(new SelectionAdapter() {

               @Override
               public void widgetSelected(final SelectionEvent e) {
                  onClickChooseFolder();
               }
            });

            /*
             * Dropbox folder path
             */
            _textFolderPath = new Text(container, SWT.BORDER);
            _textFolderPath.setEditable(false);
            _textFolderPath.setToolTipText(Messages.Pref_CloudConnectivity_Dropbox_FolderPath_Tooltip);
            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .applyTo(_textFolderPath);
         }
      }
   }

   private void enableControls() {

      _btnChooseFolder.setEnabled(!StringUtils.isNullOrEmpty(_textAccessToken.getText()));
   }

   @Override
   public void init(final IWorkbench workbench) {}

   /**
    * When the user clicks on the "Authorize" button, a browser is opened
    * so that the user can allow the MyTourbook Dropbox app to have access
    * to their Dropbox account.
    */
   private void onClickAuthorize() {

      final OAuth2Client client = new OAuth2Client();
      client.setId("client_id"); //$NON-NLS-1$
      client.setSecret("secret_id"); //$NON-NLS-1$
      client.setAccessTokenUrl("https://api.dropboxapi.com/oauth2/token"); //$NON-NLS-1$
      client.setAuthorizeUrl("https://www.dropbox.com/oauth2/authorize"); //$NON-NLS-1$
      client.setRedirectUri("https://sourceforge.net/projects/mytourbook"); //$NON-NLS-1$

      final OAuth2RequestAction request = new OAuth2RequestAction(client);
      //Opens the dialog
      request.run();
      final String token = request.getAccessToken();
      final String dialogMessage = StringUtils.isNullOrEmpty(token) ? NLS.bind(Messages.Pref_CloudConnectivity_Dropbox_AccessToken_NotRetrieved,
            request.getResponse()) : Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Retrieved;

      if (!StringUtils.isNullOrEmpty(token)) {
         _textAccessToken.setText(token);

         // Once we retrieve a valid token, by default, we set the user folder to the root folder of
         // their Dropbox account.
         _textFolderPath.setText("/");//$NON-NLS-1$
      }

      MessageDialog.openInformation(
            Display.getCurrent().getActiveShell(),
            Messages.Pref_CloudConnectivity_Dropbox_AccessToken_Retrieval_Title,
            dialogMessage);

      enableControls();
   }

   /**
    * When the user clicks on the "Choose Folder" button, a dialog is opened
    * so that the user can choose which folder will be used when using their Dropbox
    * account as a device to watch.
    */
   protected void onClickChooseFolder() {

      final DropboxFolderBrowser dropboxFolderChooser[] = new DropboxFolderBrowser[1];
      final int folderChooserResult[] = new int[1];
      BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
         @Override
         public void run() {
            dropboxFolderChooser[0] = new DropboxFolderBrowser(Display.getCurrent().getActiveShell(),
                  _textAccessToken.getText());
            folderChooserResult[0] = dropboxFolderChooser[0].open();
         }
      });

      if (folderChooserResult[0] == Window.OK) {

         final String selectedFolder = dropboxFolderChooser[0].getSelectedFolder();
         if (!StringUtils.isNullOrEmpty(selectedFolder)) {
            _textFolderPath.setText(selectedFolder);
         }
      }
   }

   @Override
   protected void performDefaults() {

      _textAccessToken.setText(_prefStore.getDefaultString(IPreferences.DROPBOX_ACCESSTOKEN));
      _textFolderPath.setText(_prefStore.getDefaultString(IPreferences.DROPBOX_FOLDER));

      enableControls();

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {

         _prefStore.setValue(IPreferences.DROPBOX_ACCESSTOKEN, _textAccessToken.getText());
         _prefStore.setValue(IPreferences.DROPBOX_FOLDER, _textFolderPath.getText());
      }

      return isOK;
   }

   private void restoreState() {
      _textAccessToken.setText(_prefStore.getString(IPreferences.DROPBOX_ACCESSTOKEN));
      _textFolderPath.setText(_prefStore.getString(IPreferences.DROPBOX_FOLDER));

      enableControls();
   }
}
