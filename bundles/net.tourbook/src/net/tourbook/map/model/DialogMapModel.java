/*******************************************************************************
 * Copyright (C) 2023 Wolfgang Schramm and Contributors
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
package net.tourbook.map.model;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.nio.file.Path;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.NIO;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

/**
 * Add map model in the map model slideout
 */
public class DialogMapModel extends TitleAreaDialog {

   private static final String   ID     = "net.tourbook.map.model.DialogMapModel"; //$NON-NLS-1$

   private final IDialogSettings _state = TourbookPlugin.getState(ID);

   /**
    * When not <code>null</code> then this model is edited, otherwise a new model is created
    */
   private MapModel              _mapModel_Editing;

   /**
    * Contains a newly create map model
    */
   private MapModel              _mapModel_New;

   /*
    * UI controls
    */
   private Button  _btnBrowseModelFilepath;

   private Spinner _spinnerForwardAngle;
   private Spinner _spinnerModelLengthFactor;

   private Text    _txtDescription;
   private Text    _txtModelFilepath;
   private Text    _txtModelName;

   public DialogMapModel(final Shell parentShell) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      shell.setText(Messages.Dialog_MapModel_Title);
   }

   @Override
   public void create() {

      super.create();

      if (_mapModel_Editing == null) {

         setTitle(Messages.Dialog_MapModel_Title_Add);
         setMessage(Messages.Dialog_MapModel_Title_Message_Add);

      } else {

         setTitle(Messages.Dialog_MapModel_Title_Edit);
         setMessage(Messages.Dialog_MapModel_Title_Message_Edit);
      }
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      if (_mapModel_Editing == null) {
         getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Add);
      } else {
         getButton(IDialogConstants.OK_ID).setText(Messages.App_Action_Save);
      }
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      restoreState();

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).spacing(10, 8).applyTo(container);
//      dlgContainer(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
      {

         {
            /*
             * Model name
             */
            UI.createLabel(container, Messages.Dialog_MapModel_Label_Name);

            _txtModelName = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtModelName);
         }
         {
            /*
             * Model file path
             */
            final Label label = UI.createLabel(container, Messages.Dialog_MapModel_Label_ModelFilepath);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            final Composite containerModelFile = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerModelFile);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerModelFile);
            {
               {
                  _txtModelFilepath = new Text(containerModelFile, SWT.BORDER);
                  _txtModelFilepath.addModifyListener(modifyEvent -> onModelFile_Modify());
                  GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtModelFilepath);
               }

               {
                  /*
                   * Button: Browse...
                   */
                  _btnBrowseModelFilepath = new Button(containerModelFile, SWT.PUSH);
                  _btnBrowseModelFilepath.setText(Messages.app_btn_browse);
                  _btnBrowseModelFilepath.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onModelFile_Select()));
                  GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_btnBrowseModelFilepath);
                  setButtonLayoutData(_btnBrowseModelFilepath);
               }
            }
         }
         {
            /*
             * Forward angle
             */
            final Label label = UI.createLabel(container, Messages.Dialog_MapModel_Label_ForwardAngle);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _spinnerForwardAngle = new Spinner(container, SWT.BORDER);
            _spinnerForwardAngle.setMinimum(-360);
            _spinnerForwardAngle.setMaximum(360);
            _spinnerForwardAngle.setIncrement(1);
            _spinnerForwardAngle.setPageIncrement(10);
            _spinnerForwardAngle.setToolTipText(Messages.Dialog_MapModel_Label_ForwardAngle_Tooltip);
            _spinnerForwardAngle.addMouseWheelListener(mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent));
         }
         {
            /*
             * Model length factor
             */
            final Label label = UI.createLabel(container, Messages.Dialog_MapModel_Label_ModelLengthFactor);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _spinnerModelLengthFactor = new Spinner(container, SWT.BORDER);
            _spinnerModelLengthFactor.setDigits(1);
            _spinnerModelLengthFactor.setMinimum(-1000);
            _spinnerModelLengthFactor.setMaximum(1000);
            _spinnerModelLengthFactor.setIncrement(1);
            _spinnerModelLengthFactor.setPageIncrement(10);
            _spinnerModelLengthFactor.setToolTipText(Messages.Dialog_MapModel_Label_ModelLengthFactor_Tooltip);
            _spinnerModelLengthFactor.addMouseWheelListener(mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 10));
         }
         {
            /*
             * Description
             */
            final Label label = UI.createLabel(container, Messages.Dialog_MapModel_Label_Description);
            GridDataFactory.fillDefaults()
                  .grab(false, false)
                  .align(SWT.FILL, SWT.BEGINNING)
                  .applyTo(label);

            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtDescription);
         }
      }
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;

      // for debugging
//    return null;
   }

   public MapModel getNewMapModel() {
      return _mapModel_New;
   }

   @Override
   protected void okPressed() {

      saveState();

      super.okPressed();
   }

   private void onModelFile_Modify() {

   }

   private void onModelFile_Select() {

      String mapFile_Foldername = null;

      final String userPathname = _txtModelFilepath.getText();
      final Path mapFilepath = NIO.getPath(userPathname);

      if (mapFilepath != null) {

         final Path mapFile_Folder = mapFilepath.getParent();
         if (mapFile_Folder != null) {

            mapFile_Foldername = mapFile_Folder.toString();
         }
      }

      final FileDialog dialog = new FileDialog(getShell(), SWT.SAVE);

      dialog.setText(Messages.Dialog_MapModel_Dialog_ModelFilepath_Title);
      dialog.setFilterPath(mapFile_Foldername);

      if (UI.IS_WIN) {

         // with Linux the file select dialog is empty when using these filters !!!

         dialog.setFileName("*." + MapModelManager.MAP_MODEL_FILE_EXTENTION);//$NON-NLS-1$
         dialog.setFilterExtensions(new String[] { MapModelManager.MAP_MODEL_FILE_EXTENTION });
      }

      final String selectedFilepath = dialog.open();

      if (selectedFilepath != null) {

         setErrorMessage(null);

         // update UI
         _txtModelFilepath.setText(selectedFilepath);
      }
   }

   private void restoreState() {

      if (_mapModel_Editing == null) {
         return;
      }

// SET_FORMATTING_OFF

      _txtModelName              .setText(_mapModel_Editing.name);
      _txtDescription            .setText(_mapModel_Editing.description);
      _txtModelFilepath          .setText(_mapModel_Editing.filepath);

      _spinnerForwardAngle       .setSelection(_mapModel_Editing.modelForwardAngle);
      _spinnerModelLengthFactor  .setSelection((int) (_mapModel_Editing.modelCenterToForwardFactor*10));

// SET_FORMATTING_ON
   }

   private void saveState() {

      MapModel mapModel;

      if (_mapModel_Editing != null) {

         // edit existing model

         mapModel = _mapModel_Editing;

      } else {

         // create map model

         mapModel = new MapModel();

         _mapModel_New = mapModel;
      }

// SET_FORMATTING_OFF

      mapModel.name                       = _txtModelName               .getText().strip();
      mapModel.description                = _txtDescription             .getText().strip();
      mapModel.filepath                   = _txtModelFilepath           .getText().strip();

      mapModel.modelForwardAngle          = _spinnerForwardAngle        .getSelection();
      mapModel.modelCenterToForwardFactor = _spinnerModelLengthFactor   .getSelection() / 10.0f;

// SET_FORMATTING_ON

   }

   /**
    * Set model which should be edited
    *
    * @param mapModel
    */
   public void setMapModel(final MapModel mapModel) {

      _mapModel_Editing = mapModel;
   }
}
