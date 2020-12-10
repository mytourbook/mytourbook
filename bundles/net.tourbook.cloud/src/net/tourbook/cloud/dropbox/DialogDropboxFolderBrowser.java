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

import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.cloud.Activator;
import net.tourbook.common.CommonActivator;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;
import net.tourbook.common.util.TableLayoutComposite;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;

public class DialogDropboxFolderBrowser extends TitleAreaDialog {

   private static final String   ROOT_FOLDER    = "/";                                                   //$NON-NLS-1$

   private static String         _accessToken;
   private static String         _workingDirectory;

   final IPreferenceStore        _prefStore     = CommonActivator.getPrefStore();

   private List<Metadata>        _folderList;
   private TableViewer           _contentViewer;
   private String                _selectedFolder;

   private ArrayList<String>     _selectedFiles = new ArrayList<>();

   private boolean               _isInErrorState;

   private final IDialogSettings _state         = TourbookPlugin.getState("DialogDropboxFolderBrowser"); //$NON-NLS-1$
   /*
    * Browser UI controls
    */
   private Text                  _textSelectedAbsolutePath;
   private Button                _buttonParentFolder;
   private Button                _btnOk;
   /*
    * Error Message UI controls
    */
   private Label                 _labelErrorMessage;

   public DialogDropboxFolderBrowser(final Shell parentShell, final String accessToken, final String workingDirectory) {

      super(parentShell);

      setShellStyle(getShellStyle() | SWT.RESIZE);

      _accessToken = accessToken;
      _workingDirectory = workingDirectory;

      setDefaultImage(Activator.getImageDescriptor(Messages.Image__Dropbox_Logo).createImage());
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      String title = UI.EMPTY_STRING;
      title = Messages.Dialog_DropboxFolderChooser_Area_Title;

      shell.setText(title);
   }

   @Override
   public void create() {

      super.create();

      String text = UI.EMPTY_STRING;
      text = Messages.Dialog_DropboxFolderChooser_Area_Text;

      setTitle(text);
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      _btnOk = getButton(IDialogConstants.OK_ID);
      _btnOk.setText(Messages.Dialog_DropboxBrowser_Button_SelectFolder);
      setButtonLayoutData(_btnOk);

      if (_isInErrorState) {
         _btnOk.setEnabled(false);
      }
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      Composite dialogAreaContainer = (Composite) super.createDialogArea(parent);

      createUI(dialogAreaContainer);

      final String dropboxResult = updateViewers();

      if (!StringUtils.isNullOrEmpty(dropboxResult)) {
         _isInErrorState = true;

         dialogAreaContainer.dispose();
         dialogAreaContainer = (Composite) super.createDialogArea(parent);

         createErrorMessageUI(dialogAreaContainer, dropboxResult);
      }

      return dialogAreaContainer;
   }

   /**
    * Creates a composite for when the connectivity with Dropbox could not be established
    * and we can't display the Dropbox account contents.
    *
    * @param parent
    * @param dropboxMessage
    *           The error message from Dropbox
    * @return
    */
   private Composite createErrorMessageUI(final Composite parent, final String dropboxMessage) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().applyTo(container);
      GridLayoutFactory.fillDefaults().margins(20, 20).numColumns(1).applyTo(container);
      {
         /*
          * Error message obtained when trying to retrieve the Dropbox folder content
          */
         _labelErrorMessage = new Label(container, SWT.LEFT);
         _labelErrorMessage.setText(dropboxMessage);
      }

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.fillDefaults().margins(20, 20).numColumns(2).applyTo(container);
      {
         /*
          * Parent folder button
          */
         _buttonParentFolder = new Button(container, SWT.LEFT);
         _buttonParentFolder.setToolTipText(Messages.Dialog_DropboxBrowser_Button_ParentFolder_Tooltip);
         _buttonParentFolder.setImage(Activator.getImageDescriptor(Messages.Image__Dropbox_Parentfolder).createImage());
         GridDataFactory.fillDefaults().align(SWT.BEGINNING, SWT.CENTER).applyTo(_buttonParentFolder);
         _buttonParentFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(final SelectionEvent event) {
               onClickParentFolder();
            }
         });
         _buttonParentFolder.setEnabled(false);

         /*
          * Dropbox folder path
          */
         _textSelectedAbsolutePath = new Text(container, SWT.BORDER);
         _textSelectedAbsolutePath.setEditable(false);
         _textSelectedAbsolutePath.setToolTipText(Messages.Dialog_DropboxBrowser_Text_AbsolutePath_Tooltip);
         GridDataFactory.fillDefaults().grab(true, false).applyTo(_textSelectedAbsolutePath);
         _textSelectedAbsolutePath.setText(ROOT_FOLDER);

         createUI_10_FolderViewer(container);
      }

      return container;
   }

   private void createUI_10_FolderViewer(final Composite parent) {

      final TableLayoutComposite layouter = new TableLayoutComposite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().span(3, 1).grab(true, true).hint(600, 300).applyTo(layouter);

      final Table table = new Table(layouter, (SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI));
      table.setHeaderVisible(false);
      table.setLinesVisible(false);

      _contentViewer = new TableViewer(table);

      // column: name + image
      final TableViewerColumn tvc = new TableViewerColumn(_contentViewer, SWT.NONE);
      tvc.setLabelProvider(new CellLabelProvider() {
         @Override
         public void update(final ViewerCell cell) {

            final Metadata entry = ((Metadata) cell.getElement());

            String entryName = null;
            Image entryImage = null;

            entryName = entry.getName();

            if (entry instanceof FolderMetadata) {

               entryImage =
                     Activator.getImageDescriptor(Messages.Image__Dropbox_Folder).createImage();
            } else if (entry instanceof FileMetadata) {

               entryImage =
                     Activator.getImageDescriptor(Messages.Image__Dropbox_File).createImage();
            }

            cell.setText(entryName);
            cell.setImage(entryImage);
         }
      });
      layouter.addColumnData(new ColumnWeightData(1));

      _contentViewer.setContentProvider(new IStructuredContentProvider() {
         @Override
         public void dispose() {}

         @Override
         public Object[] getElements(final Object inputElement) {
            final Object[] sortedElements = _folderList.stream().sorted((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName())).collect(Collectors
                  .toList()).toArray();
            return sortedElements;
         }

         @Override
         public void inputChanged(final Viewer viewer, final Object oldInput, final Object newInput) {}
      });

      _contentViewer.addDoubleClickListener(new IDoubleClickListener() {
         @Override
         public void doubleClick(final DoubleClickEvent event) {
            onSelectItem(event.getSelection());
         }
      });
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {
      // keep window size and position
      return _state;
   }

   public ArrayList<String> getSelectedFiles() {
      return _selectedFiles;
   }

   public String getSelectedFolder() {
      return _selectedFolder;
   }

   @Override
   protected void okPressed() {

      final boolean readyToQuit = true;

      if (_isInErrorState) {
         super.okPressed();
         return;
      }

      _selectedFolder = _textSelectedAbsolutePath.getText();

      if (readyToQuit) {
         super.okPressed();
      }
   }

   protected void onClickParentFolder() {

      final String currentFolder = _textSelectedAbsolutePath.getText();

      final int endIndex = currentFolder.lastIndexOf(ROOT_FOLDER);
      if (endIndex != -1) {
         final String parentFolder = currentFolder.substring(0, endIndex);
         selectFolder(parentFolder);
      }
   }

   protected void onSelectItem(final ISelection selectedItem) {
      final StructuredSelection selection = (StructuredSelection) selectedItem;
      final Object[] selectionArray = selection.toArray();
      if (selectionArray.length == 0) {
         return;
      }

      // Double clicking on an item should always return only 1 element.
      final Metadata item = ((Metadata) selectionArray[0]);
      final String itemPath = item.getPathDisplay();

      if (item instanceof FolderMetadata) {

         selectFolder(itemPath);
      }
   }

   private String selectFolder(String folderAbsolutePath) {

      try {

         if (folderAbsolutePath.equals(ROOT_FOLDER)) {
            folderAbsolutePath = UI.EMPTY_STRING;
         }

         final ListFolderResult list = DropboxClient.getDefault(_accessToken).files().listFolder(folderAbsolutePath);
         _folderList = list.getEntries();

         _textSelectedAbsolutePath.setText(
               StringUtils.isNullOrEmpty(folderAbsolutePath) ? ROOT_FOLDER : folderAbsolutePath);

         _buttonParentFolder.setEnabled(_textSelectedAbsolutePath.getText().length() > 1);

         _contentViewer.refresh();
      } catch (final Exception e) {
         return e.getMessage();
      }
      return null;
   }

   private String updateViewers() {

      final String dropboxResult = selectFolder(_workingDirectory);

      if (!StringUtils.isNullOrEmpty(dropboxResult)) {
         return dropboxResult;
      }

      // show contents in the viewer
      _contentViewer.setInput(new Object());

      return null;
   }
}
