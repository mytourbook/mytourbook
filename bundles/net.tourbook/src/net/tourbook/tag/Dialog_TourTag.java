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

import net.tourbook.Images;
import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourTag;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to modify a {@link TourTag}
 */
public class Dialog_TourTag extends TitleAreaDialog {

   private static final String           ID                       = "net.tourbook.tag.Dialog_TourTag";      //$NON-NLS-1$

   private static final IPreferenceStore _prefStore               = TourbookPlugin.getPrefStore();
   private static final String           IMAGE_LAST_SELECTED_PATH = "Dialog_TourTag_ImageLastSelectedPath"; //$NON-NLS-1$

   private final IDialogSettings         _state                   = TourbookPlugin.getState(ID);

   private String                        _dlgMessage;

   private String                        _imageFilePath;

   private TourTag                       _tourTag_Original;
   private TourTag                       _tourTag_Clone;

   /*
    * UI resources
    */
   private Image _imageCamera;
   private Image _imageTrash;

   /*
    * UI controls
    */
   private Button _btnDeleteImage;

   private Label  _canvasTagImage;

   private Text   _txtNotes;
   private Text   _txtName;

   public Dialog_TourTag(final Shell parentShell,
                         final String dlgMessage,
                         final TourTag tourTag) {

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

      shell.addDisposeListener(disposeEvent -> dispose());
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

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      _imageCamera = TourbookPlugin.getImageDescriptor(Images.Camera).createImage();
      _imageTrash = TourbookPlugin.getImageDescriptor(Images.App_Trash_Themed).createImage();

      createUI(dlgContainer);

      restoreState();

      _txtName.selectAll();
      _txtName.setFocus();

      enableControls();

      return dlgContainer;
   }

   /**
    * create the drop down menus, this must be created after the parent control is created
    */
   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(4).applyTo(container);
//    container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            /*
             * Text: Name
             */

            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_TourTag_Label_TagName);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _txtName = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().span(3, 1).grab(true, false).applyTo(_txtName);
         }
         {
            /*
             * Label: Tag image
             */
            final int tagImageSize = TagManager.getTagImageSize();

            final Label label = UI.createLabel(container, UI.EMPTY_STRING);
            label.setText(Messages.Dialog_TourTag_Label_Image);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _canvasTagImage = new Label(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .hint(tagImageSize, tagImageSize)
                  .applyTo(_canvasTagImage);

            final Composite imageContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults()
                  .span(2, 1)
                  .align(SWT.RIGHT, SWT.TOP)
                  .applyTo(imageContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(imageContainer);
            {
               final Button btnSelectImage = new Button(imageContainer, SWT.PUSH);
               btnSelectImage.setText(Messages.app_btn_browse);
               btnSelectImage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelectImage()));
               GridDataFactory.fillDefaults().applyTo(btnSelectImage);

               _btnDeleteImage = new Button(imageContainer, SWT.PUSH);
               _btnDeleteImage.setImage(_imageTrash);
               _btnDeleteImage.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onDeleteImage()));
               GridDataFactory.fillDefaults().applyTo(_btnDeleteImage);
            }
         }
         {
            /*
             * Text: Notes
             */
            final Label label = new Label(container, SWT.NONE);
            label.setText(Messages.Dialog_TourTag_Label_Notes);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtNotes = new Text(container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .grab(true, true)
                  .hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(20))
                  .applyTo(_txtNotes);
         }
      }
   }

   private void dispose() {

      disposeCanvasTagImage();

      UI.disposeResource(_imageCamera);
      UI.disposeResource(_imageTrash);
   }

   private void disposeCanvasTagImage() {

      if (_canvasTagImage == null ||
            _canvasTagImage.getImage() == _imageCamera) {
         return;
      }
      UI.disposeResource(_canvasTagImage.getImage());
   }

   private void enableControls() {

      _btnDeleteImage.setEnabled(StringUtils.hasContent(_imageFilePath));
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

   private void onDeleteImage() {

      _imageFilePath = null;

      disposeCanvasTagImage();

      setTagImage(null);

      enableControls();
   }

   private void onSelectImage() {

      final FileDialog fileDialog = new FileDialog(getShell(), SWT.OPEN);

      fileDialog.setText(Messages.Dialog_TourTag_ImportImage_Title);
      fileDialog.setFilterPath(_prefStore.getString(IMAGE_LAST_SELECTED_PATH));
      fileDialog.setFilterNames(new String[] { Messages.Dialog_TourTag_FileDialog_ImageFiles });
      fileDialog.setFilterExtensions(new String[] { "*.bmp;*.gif;*.png;*.jpg" });//$NON-NLS-1$

      // open file dialog
      final String imageFilePath = fileDialog.open();

      // check if user canceled the dialog
      if (imageFilePath == null) {
         return;
      }

      setTagImage(imageFilePath);

      final String filePathFolder = Paths.get(imageFilePath).getParent().toString();

      // keep last selected path
      _prefStore.putValue(IMAGE_LAST_SELECTED_PATH, filePathFolder);

      enableControls();
   }

   private void restoreState() {

      _txtName.setText(_tourTag_Clone.getTagName());
      _txtNotes.setText(_tourTag_Clone.getNotes());
      _imageFilePath = _tourTag_Clone.getImageFilePath();

      setTagImage(_imageFilePath);
   }

   private void saveState() {

      _tourTag_Clone.setNotes(_txtNotes.getText());
      _tourTag_Clone.setTagName(_txtName.getText());
      _tourTag_Clone.setImageFilePath(_imageFilePath);
   }

   private void setTagImage(final String imageFilePath) {

      setErrorMessage(null);

      final Image[] image = new Image[] { _imageCamera };

      if (StringUtils.hasContent(imageFilePath)) {

         if (!Files.exists(Paths.get(imageFilePath))) {

            setErrorMessage(NLS.bind(
                  Messages.Dialog_TourTag_Label_ImageNotFound,
                  imageFilePath));

         } else {

            _imageFilePath = imageFilePath;

            BusyIndicator.showWhile(Display.getCurrent(),
                  () -> image[0] = TagManager.prepareTagImage(_imageFilePath));
         }
      }

      //Before setting a new image, we make sure that the previous one was disposed
      disposeCanvasTagImage();

      _canvasTagImage.setImage(image[0]);
   }
}
