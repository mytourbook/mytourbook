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
package net.tourbook.map2.view;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.map2.Messages;
import net.tourbook.ui.FileCollisionBehavior;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogMap2ExportViewImage extends TitleAreaDialog {

   private static final List<String> DistanceData                             = List.of("jpg", "png", "bmp");                         //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

   private static final String       APP_BTN_BROWSE                           =
         net.tourbook.Messages.app_btn_browse;
   private static final String       DIALOG_EXPORT_CHK_OVERWRITEFILES         =
         net.tourbook.Messages.dialog_export_chk_overwriteFiles;
   private static final String       DIALOG_EXPORT_CHK_OVERWRITEFILES_TOOLTIP =
         net.tourbook.Messages.dialog_export_chk_overwriteFiles_tooltip;
   private static final String       DIALOG_EXPORT_DIR_DIALOG_MESSAGE         =
         net.tourbook.Messages.dialog_export_dir_dialog_message;
   private static final String       DIALOG_EXPORT_DIR_DIALOG_TEXT            =
         net.tourbook.Messages.dialog_export_dir_dialog_text;
   private static final String       DIALOG_EXPORT_GROUP_EXPORTFILENAME       =
         net.tourbook.Messages.dialog_export_group_exportFileName;
   private static final String       DIALOG_EXPORT_LABEL_EXPORTFILEPATH       =
         net.tourbook.Messages.dialog_export_label_exportFilePath;
   private static final String       DIALOG_EXPORT_LABEL_FILENAME             =
         net.tourbook.Messages.dialog_export_label_fileName;
   private static final String       DIALOG_EXPORT_MSG_FILEALREADYEXISTS      =
         net.tourbook.Messages.dialog_export_msg_fileAlreadyExists;
   private static final String       DIALOG_EXPORT_MSG_FILENAMEISINVALID      =
         net.tourbook.Messages.dialog_export_msg_fileNameIsInvalid;
   private static final String       DIALOG_EXPORT_MSG_PATHISNOTAVAILABLE     =
         net.tourbook.Messages.dialog_export_msg_pathIsNotAvailable;
   private static final String       DIALOG_EXPORT_TXT_FILEPATH_TOOLTIP       =
         net.tourbook.Messages.dialog_export_txt_filePath_tooltip;

   private static final String       STATE_IMAGE_FORMAT                       = "STATE_IMAGE_FORMAT";                                 //$NON-NLS-1$
   private static final String       STATE_IS_OVERWRITE_IMAGE_FILE            = "STATE_IS_OVERWRITE_IMAGE_FILE";                      //$NON-NLS-1$
   private static final String       STATE_EXPORT_IMAGE_FILE_PATH             = "STATE_EXPORT_IMAGE_FILE_PATH";                       //$NON-NLS-1$
   private static final String       STATE_EXPORT_IMAGE_FILE_NAME             = "STATE_EXPORT_IMAGE_FILE_NAME";                       //$NON-NLS-1$

   private static final int          COMBO_HISTORY_LENGTH                     = 20;

   private final IDialogSettings     _state                                   = TourbookPlugin.getState("DialogMap2ExportViewImage"); //$NON-NLS-1$

   private PixelConverter            _pc;

   private Map2View                  _map2View;

   private FileCollisionBehavior     _exportState_FileCollisionBehavior;

   /*
    * UI controls
    */
   private Composite        _dlgContainer;
   private Composite        _inputContainer;

   private Combo            _comboImageFormat;
   private Text             _txtFilePath;
   private Combo            _comboFile;
   private Combo            _comboPath;
   private Button           _chkOverwriteFiles;

   private ModifyListener   _filePathModifyListener;
   private SelectionAdapter _selectionAdapter;

   public DialogMap2ExportViewImage(final Shell parentShell, final Map2View map2View) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);

      _map2View = map2View;
   }

   @Override
   public boolean close() {

      saveState();

      return super.close();
   }

   @Override
   public void create() {

      super.create();

      getShell().setText(Messages.map_dialog_export_view_to_image_title);

      setTitle(Messages.map_dialog_export_view_to_image_title);
      setMessage(Messages.map_dialog_export_view_to_image_message);

      restoreState();
      validateFields();
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(_dlgContainer);

      return _dlgContainer;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _filePathModifyListener = modifyEvent -> validateFields();

      _selectionAdapter = new SelectionAdapter() {
         @Override
         public void widgetSelected(final SelectionEvent e) {
            validateFields();
         }
      };

      _inputContainer = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_inputContainer);
      GridLayoutFactory.swtDefaults().margins(10, 5).applyTo(_inputContainer);
      {
         createUI_10_ImageFormat(_inputContainer);
         createUI_90_ExportImage(_inputContainer);
      }
   }

   private void createUI_10_ImageFormat(final Composite parent) {

      /*
       * group: Image format
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(Messages.map_dialog_export_group_image_format);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(group);
      {
         /*
          * label: Image format
          */
         final Label label = new Label(group, SWT.NONE);
         label.setText(Messages.map_dialog_export_group_image_format_label);

         /*
          * combo: Image format
          */
         _comboImageFormat = new Combo(group, SWT.READ_ONLY | SWT.BORDER);
         _comboImageFormat.setVisibleItemCount(3);
         _comboImageFormat.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               validateFields();
            }
         });
         GridDataFactory
               .fillDefaults()
               .align(SWT.BEGINNING, SWT.CENTER)
               .hint(_pc.convertWidthInCharsToPixels(15), SWT.DEFAULT)
               .applyTo(_comboImageFormat);

      }

   }

   private void createUI_90_ExportImage(final Composite parent) {

      /*
       * group: filename
       */
      final Group group = new Group(parent, SWT.NONE);
      group.setText(DIALOG_EXPORT_GROUP_EXPORTFILENAME);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(group);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(group);
      {

         Label label;
         /*
          * label: filename
          */
         label = new Label(group, SWT.NONE);
         label.setText(DIALOG_EXPORT_LABEL_FILENAME);

         /*
          * combo: path
          */
         _comboFile = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboFile);
         _comboFile.setVisibleItemCount(20);
         _comboFile.addVerifyListener(UI.verifyFilenameInput());
         _comboFile.addModifyListener(_filePathModifyListener);
         _comboFile.addSelectionListener(_selectionAdapter);

         /*
          * button: browse
          */
         final Button btnSelectFile = new Button(group, SWT.PUSH);
         btnSelectFile.setText(APP_BTN_BROWSE);
         btnSelectFile.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectBrowseFile();
               validateFields();
            }
         });
         setButtonLayoutData(btnSelectFile);

         // -----------------------------------------------------------------------------

         /*
          * label: path
          */
         label = new Label(group, SWT.NONE);
         label.setText(DIALOG_EXPORT_LABEL_EXPORTFILEPATH);

         /*
          * combo: path
          */
         _comboPath = new Combo(group, SWT.SINGLE | SWT.BORDER);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_comboPath);
         _comboPath.setVisibleItemCount(20);
         _comboPath.addModifyListener(_filePathModifyListener);
         _comboPath.addSelectionListener(_selectionAdapter);

         /*
          * button: browse
          */
         final Button btnSelectDirectory = new Button(group, SWT.PUSH);
         btnSelectDirectory.setText(APP_BTN_BROWSE);
         btnSelectDirectory.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent e) {
               onSelectBrowseDirectory();
               validateFields();
            }
         });
         setButtonLayoutData(btnSelectDirectory);

         // -----------------------------------------------------------------------------

         /*
          * label: file path
          */
         label = new Label(group, SWT.NONE);
         label.setText(DIALOG_EXPORT_LABEL_EXPORTFILEPATH);

         /*
          * text: filename
          */
         _txtFilePath = new Text(group, SWT.READ_ONLY);
         GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(_txtFilePath);
         _txtFilePath.setToolTipText(DIALOG_EXPORT_TXT_FILEPATH_TOOLTIP);
         _txtFilePath.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

         // -----------------------------------------------------------------------------

         /*
          * checkbox: overwrite files
          */
         _chkOverwriteFiles = new Button(group, SWT.CHECK);
         GridDataFactory.fillDefaults()//
               .align(SWT.BEGINNING, SWT.CENTER)
               .span(3, 1)
               .indent(0, _pc.convertVerticalDLUsToPixels(4))
               .applyTo(_chkOverwriteFiles);
         _chkOverwriteFiles.setText(DIALOG_EXPORT_CHK_OVERWRITEFILES);
         _chkOverwriteFiles.setToolTipText(DIALOG_EXPORT_CHK_OVERWRITEFILES_TOOLTIP);
      }

   }

   private void doExport() {

      final String exportFileName = _txtFilePath.getText();

      _exportState_FileCollisionBehavior = new FileCollisionBehavior();
      boolean isOverwrite = true;
      final File exportFile = new File(exportFileName);
      if (exportFile.exists() && !_chkOverwriteFiles.getSelection()) {
         isOverwrite = net.tourbook.ui.UI.confirmOverwrite(_exportState_FileCollisionBehavior, exportFile);
      }

      if (!isOverwrite) {
         return;
      }

      net.tourbook.ui.UI.disableAllControls(_inputContainer);

      final Image mapViewImage = _map2View.getMapViewImage();

      final ImageLoader saver = new ImageLoader();
      saver.data = new ImageData[] { mapViewImage.getImageData() };
      saver.save(exportFileName, getSwtImageType());
      mapViewImage.dispose();
   }

   private void enableOK(final boolean isEnabled) {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   private String getExportFileName() {

      return _comboFile.getText().trim();
   }

   private String getExportPathName() {

      return _comboPath.getText().trim();
   }

   private String getFileExtension() {

      return _comboImageFormat.getText();
   }

   private int getSwtImageType() {

      switch (_comboImageFormat.getSelectionIndex()) {

      case 1:
         return SWT.IMAGE_PNG;

      case 2:
         return SWT.IMAGE_BMP;

      case 0:
      default:
         return SWT.IMAGE_JPEG;
      }
   }

   @Override
   protected void okPressed() {

      BusyIndicator.showWhile(Display.getCurrent(), this::doExport);

      if (_exportState_FileCollisionBehavior.value == FileCollisionBehavior.DIALOG_IS_CANCELED) {
         return;
      }

      super.okPressed();
   }

   private void onSelectBrowseDirectory() {

      final DirectoryDialog dialog = new DirectoryDialog(_dlgContainer.getShell(), SWT.SAVE);
      dialog.setText(DIALOG_EXPORT_DIR_DIALOG_TEXT);
      dialog.setMessage(DIALOG_EXPORT_DIR_DIALOG_MESSAGE);
      dialog.setFilterPath(getExportPathName());

      final String selectedDirectoryName = dialog.open();

      if (selectedDirectoryName != null) {

         setErrorMessage(null);
         _comboPath.setText(selectedDirectoryName);
      }
   }

   private void onSelectBrowseFile() {

      final String fileExtension = getFileExtension();

      final FileDialog dialog = new FileDialog(_dlgContainer.getShell(), SWT.SAVE);
      dialog.setText(DIALOG_EXPORT_DIR_DIALOG_TEXT);

      dialog.setFilterPath(getExportPathName());
      dialog.setFilterExtensions(new String[] { fileExtension });
      dialog.setFileName("*." + fileExtension);//$NON-NLS-1$

      final String selectedFilePath = dialog.open();

      if (selectedFilePath != null) {
         setErrorMessage(null);
         _comboFile.setText(new Path(selectedFilePath).toFile().getName());
      }
   }

   private void restoreState() {

      //Image format
      DistanceData.forEach(_comboImageFormat::add);

      _comboImageFormat.select(Util.getStateInt(_state, STATE_IMAGE_FORMAT, 0));

      // export file/path
      UI.restoreCombo(_comboFile, _state.getArray(STATE_EXPORT_IMAGE_FILE_NAME));
      UI.restoreCombo(_comboPath, _state.getArray(STATE_EXPORT_IMAGE_FILE_PATH));
      _chkOverwriteFiles.setSelection(_state.getBoolean(STATE_IS_OVERWRITE_IMAGE_FILE));
   }

   private void saveState() {

      _state.put(STATE_IMAGE_FORMAT, _comboImageFormat.getSelectionIndex());

      // export file/path
      if (validateFilePath()) {
         _state.put(
               STATE_EXPORT_IMAGE_FILE_PATH,
               Util.getUniqueItems(_comboPath.getItems(), getExportPathName(), COMBO_HISTORY_LENGTH));
         _state.put(
               STATE_EXPORT_IMAGE_FILE_NAME,
               Util.getUniqueItems(_comboFile.getItems(), getExportFileName(), COMBO_HISTORY_LENGTH));
      }
      _state.put(STATE_IS_OVERWRITE_IMAGE_FILE, _chkOverwriteFiles.getSelection());
   }

   private void setError(final String message) {

      setErrorMessage(message);
      enableOK(false);
   }

   private void validateFields() {

      setErrorMessage(null);

      if (!validateFilePath()) {
         return;
      }

      setErrorMessage(null);
      enableOK(true);
   }

   private boolean validateFilePath() {

      // check path
      IPath filePath = new Path(getExportPathName());
      if (!new File(filePath.toOSString()).exists()) {

         // invalid path
         setError(NLS.bind(DIALOG_EXPORT_MSG_PATHISNOTAVAILABLE, filePath.toOSString()));
         return false;
      }

      boolean returnValue = false;

      String fileName = getExportFileName();

      // remove extensions
      final int extPos = fileName.indexOf('.');
      if (extPos != -1) {
         fileName = fileName.substring(0, extPos);
      }

      // build file path with extension
      filePath = filePath
            .addTrailingSeparator()
            .append(fileName)
            .addFileExtension(getFileExtension());

      final File newFile = new File(filePath.toOSString());

      if ((fileName.length() == 0) || newFile.isDirectory()) {

         // invalid filename

         setError(DIALOG_EXPORT_MSG_FILENAMEISINVALID);

      } else if (newFile.exists()) {

         // file already exists

         setMessage(
               NLS.bind(DIALOG_EXPORT_MSG_FILEALREADYEXISTS, filePath.toOSString()),
               IMessageProvider.WARNING);
         returnValue = true;

      } else {

         setMessage(UI.EMPTY_STRING);

         try {
            final boolean isFileCreated = newFile.createNewFile();

            // name is correct

            if (isFileCreated) {
               // delete file because the file is created for checking validity
               Files.delete(newFile.toPath());
            }
            returnValue = true;

         } catch (final IOException ioe) {
            setError(DIALOG_EXPORT_MSG_FILENAMEISINVALID);
         }

      }

      _txtFilePath.setText(filePath.toOSString());

      return returnValue;
   }
}
