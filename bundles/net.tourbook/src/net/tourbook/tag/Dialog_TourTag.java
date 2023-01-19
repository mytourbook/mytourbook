/*******************************************************************************
 * Copyright (C) 2005, 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.tag;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.nio.file.Files;
import java.nio.file.Paths;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.widgets.ImageCanvas;
import net.tourbook.data.TourTag;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to modify a {@link TourTag}
 */
public class Dialog_TourTag extends TitleAreaDialog {

   private static final String           ID                = "net.tourbook.tag.Dialog_TourTag"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore        = TourbookPlugin.getPrefStore();
   private static final String           IMPORT_IMAGE_PATH = "Dialog_TourTag_ImportImagePath";  //$NON-NLS-1$

   private final IDialogSettings         _state            = TourbookPlugin.getState(ID);

   private String                        _dlgMessage;
   private TourTag                       _tourTag_Original;

   private TourTag                       _tourTag_Clone;

   /*
    * UI resources
    */
   private PixelConverter _pc;

   /*
    * UI controls
    */
   private Button      _btnSelectImage;
   private ImageCanvas _canvasTagImage;
   private Text        _txtNotes;
   private Text        _txtName;

   private String      _imageFilePath;

   public Dialog_TourTag(final Shell parentShell, final String dlgMessage, final TourTag tourTag) {

      super(parentShell);

      _dlgMessage = dlgMessage;

      _tourTag_Original = tourTag;
      _tourTag_Clone = tourTag.clone();

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_TourTag_Title);

      shell.addDisposeListener(disposeEvent -> onDispose());
   }

   @Override
   public void create() {

      super.create();

      setTitle(Messages.Dialog_TourTag_EditTag_Title);
      setMessage(_dlgMessage);
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Save
      getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      _pc = new PixelConverter(parent);

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      restoreState();

      _txtName.selectAll();
      _txtName.setFocus();

      return dlgContainer;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         {
            // Text: Name

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_TourTag_Label_TagName);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _txtName = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().span(2, 1).grab(true, false).applyTo(_txtName);
         }
         {
            // Text: Image File Path
            final Label label = UI.createLabel(container, UI.EMPTY_STRING);
            label.setText(Messages.Dialog_TourTag_Label_Image);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _canvasTagImage = new ImageCanvas(container, SWT.DOUBLE_BUFFERED);
            GridDataFactory.fillDefaults()//
                  .hint(_pc.convertWidthInCharsToPixels(10), SWT.DEFAULT)
                  .applyTo(_canvasTagImage);

            _btnSelectImage = new Button(container, SWT.PUSH);
            _btnSelectImage.setText(Messages.app_btn_browse);
            _btnSelectImage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectImage()));
            GridDataFactory.fillDefaults()
                  .align(SWT.LEFT, SWT.CENTER)
                  .applyTo(_btnSelectImage);

         }
         {
            // Text: Notes

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_TourTag_Label_Notes);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtNotes = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .grab(true, true)
                  .hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(20))
                  .applyTo(_txtNotes);
         }
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   @Override
   protected void okPressed() {

      // set model from UI
      saveState();

      if (_tourTag_Clone.isValidForSave() == false) {
         return;
      }

      // update original model
      _tourTag_Original.updateFromModified(_tourTag_Clone);

      super.okPressed();
   }

   private void onDispose() {
      _canvasTagImage.dispose();
   }

   private void onSelectImage() {

      final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);

      fileDialog.setText(Messages.Dialog_TourTag_ImportImage_Title);
      fileDialog.setFilterPath(_prefStore.getString(IMPORT_IMAGE_PATH));
      fileDialog.setFilterNames(new String[] { Messages.Dialog_TourTag_FileDialog_ImageFiles });
      fileDialog.setFilterExtensions(new String[] { "*.bmp;*.gif;*.png;*.jpg" });//$NON-NLS-1$

      // open file dialog
      final String imageFilePath = fileDialog.open();

      setTagImage(imageFilePath);
   }

   private void restoreState() {

      _txtName.setText(_tourTag_Clone.getTagName());
      _txtNotes.setText(_tourTag_Clone.getNotes());
      setTagImage(_tourTag_Clone.getImageFilePath());
   }

   private void saveState() {

      _tourTag_Clone.setNotes(_txtNotes.getText());
      _tourTag_Clone.setTagName(_txtName.getText());
      _tourTag_Clone.setImageFilePath(_imageFilePath);
   }

   private void setTagImage(final String imageFilePath) {

      if (StringUtils.isNullOrEmpty(imageFilePath) || !Files.exists(Paths.get(imageFilePath))) {
         return;
      }

      _imageFilePath = imageFilePath;

      final Image image = net.tourbook.ui.UI.prepareTagImage(_imageFilePath);
      _canvasTagImage.setImage(image);
   }
}
