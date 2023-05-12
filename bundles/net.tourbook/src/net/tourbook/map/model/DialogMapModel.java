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
import net.tourbook.common.util.Util;

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

   private static final String   ID                   = "net.tourbook.map.model.DialogMapModel"; //$NON-NLS-1$

   private static final String   STATE_IS_LIVE_UPDATE = "STATE_IS_LIVE_UPDATE";                  //$NON-NLS-1$

   private final IDialogSettings _state               = TourbookPlugin.getState(ID);

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
   private Button  _chkLiveUpdate;

   private Label   _labelDescription;
   private Label   _labelFilepath;
   private Label   _labelForwardAngle;
   private Label   _labelForwardAngleUnit;
   private Label   _labelHeadPositionFactor;
   private Label   _labelName;

   private Spinner _spinnerForwardAngle;
   private Spinner _spinnerHeadPositionFactor;

   private Text    _txtDescription;
   private Text    _txtFilepath;
   private Text    _txtName;

   public DialogMapModel(final Shell parentShell) {

      super(parentShell);

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }

   @Override
   public boolean close() {

      saveState_LiveUpdate();

      return super.close();
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
         setMessage(_mapModel_Editing.name);
      }
   }

   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      updateUI_DialogButtons();
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      restoreState();

      // enable actions after the UI is created
      parent.getDisplay().asyncExec(() -> enableControls());

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).spacing(10, 8).applyTo(container);
//      container.setBackground(UI.SYS_COLOR_BLUE);
      {
         {
            /*
             * Model name
             */
            _labelName = UI.createLabel(container, Messages.Dialog_MapModel_Label_Name);
            GridDataFactory.fillDefaults().indent(0, 5).applyTo(_labelName);

            _txtName = new Text(container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).indent(0, 5).applyTo(_txtName);
         }
         {
            /*
             * Model file path
             */
            _labelFilepath = UI.createLabel(container, Messages.Dialog_MapModel_Label_ModelFilepath);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_labelFilepath);

            final Composite containerModelFile = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(containerModelFile);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(containerModelFile);
            {
               {
                  _txtFilepath = new Text(containerModelFile, SWT.BORDER);
                  _txtFilepath.addModifyListener(modifyEvent -> onModelFile_Modify());
                  GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtFilepath);
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
            _labelForwardAngle = UI.createLabel(container, Messages.Dialog_MapModel_Label_ForwardAngle);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_labelForwardAngle);

            final Composite angleContainer = new Composite(container, SWT.NONE);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(angleContainer);
            GridLayoutFactory.fillDefaults().numColumns(2).applyTo(angleContainer);
            {
               _spinnerForwardAngle = new Spinner(angleContainer, SWT.BORDER);
               _spinnerForwardAngle.setMinimum(-360);
               _spinnerForwardAngle.setMaximum(360);
               _spinnerForwardAngle.setIncrement(1);
               _spinnerForwardAngle.setPageIncrement(5);
               _spinnerForwardAngle.setToolTipText(Messages.Dialog_MapModel_Label_ForwardAngle_Tooltip);
               _spinnerForwardAngle.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_LiveUpdateControls()));
               _spinnerForwardAngle.addMouseWheelListener(mouseEvent -> {
                  UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 5);
                  onSelect_LiveUpdateControls();
               });

               _labelForwardAngleUnit = UI.createLabel(angleContainer, UI.SYMBOL_DEGREE);
               GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_labelForwardAngleUnit);
            }
         }
         {
            /*
             * Head position factor
             */
            _labelHeadPositionFactor = UI.createLabel(container, Messages.Dialog_MapModel_Label_HeadPositionFactor);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(_labelHeadPositionFactor);

            _spinnerHeadPositionFactor = new Spinner(container, SWT.BORDER);
            _spinnerHeadPositionFactor.setDigits(1);
            _spinnerHeadPositionFactor.setMinimum(-1000);
            _spinnerHeadPositionFactor.setMaximum(1000);
            _spinnerHeadPositionFactor.setIncrement(1);
            _spinnerHeadPositionFactor.setPageIncrement(10);
            _spinnerHeadPositionFactor.setToolTipText(Messages.Dialog_MapModel_Label_HeadPositionFactor_Tooltip);
            _spinnerHeadPositionFactor.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_LiveUpdateControls()));
            _spinnerHeadPositionFactor.addMouseWheelListener(mouseEvent -> {
               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 5);
               onSelect_LiveUpdateControls();
            });
         }
         {
            /*
             * Description
             */
            _labelDescription = UI.createLabel(container, Messages.Dialog_MapModel_Label_Description);
            GridDataFactory.fillDefaults()
                  .align(SWT.FILL, SWT.BEGINNING)
                  .applyTo(_labelDescription);

            _txtDescription = new Text(container, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.H_SCROLL);
            GridDataFactory.fillDefaults().grab(true, true).applyTo(_txtDescription);
         }
         {
            /*
             * Live update
             */
            UI.createSpacer_Horizontal(container, 1);

            _chkLiveUpdate = new Button(container, SWT.CHECK);
            _chkLiveUpdate.setText(Messages.Dialog_MapModel_Checkbox_IsLiveUpdate);
            _chkLiveUpdate.setToolTipText(Messages.Dialog_MapModel_Checkbox_IsLiveUpdate_Tooltip);
            _chkLiveUpdate.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onSelect_LiveUpdate()));

            GridDataFactory.fillDefaults()
                  .grab(true, false)
                  .align(SWT.BEGINNING, SWT.FILL)
                  .indent(0, 10)
                  .applyTo(_chkLiveUpdate);
         }
      }
   }

   private void enableControls() {

// SET_FORMATTING_OFF

      final Button okButton = getButton(IDialogConstants.OK_ID);

      final boolean isExistingModel = _mapModel_Editing != null;
      final boolean isLiveUpdate    = _chkLiveUpdate.getSelection();

      final boolean isDefaultModel  = isExistingModel && _mapModel_Editing.isDefaultModel;
      final boolean isUserModel     = isDefaultModel == false;
      final boolean isNormalEditing = isLiveUpdate == false  && isUserModel;

      _btnBrowseModelFilepath    .setEnabled(isNormalEditing);

      _chkLiveUpdate             .setEnabled(isExistingModel && isUserModel);

      _labelDescription          .setEnabled(true);
      _labelFilepath             .setEnabled(isNormalEditing);
      _labelName                 .setEnabled(isNormalEditing);

      _labelForwardAngle         .setEnabled(isUserModel);
      _labelForwardAngleUnit     .setEnabled(isUserModel);
      _labelHeadPositionFactor   .setEnabled(isUserModel);

      _spinnerForwardAngle       .setEnabled(isUserModel);
      _spinnerHeadPositionFactor .setEnabled(isUserModel);

      _txtDescription            .setEnabled(true);
      _txtFilepath               .setEnabled(isNormalEditing);
      _txtName                   .setEnabled(isNormalEditing);

      okButton                   .setEnabled(isUserModel);

// SET_FORMATTING_ON
   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;

      // for debugging
      // return null;
   }

   public MapModel getNewMapModel() {

      return _mapModel_New;
   }

   private boolean isLiveUpdate() {

      return _chkLiveUpdate != null && _chkLiveUpdate.getSelection();
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

      final String userPathname = _txtFilepath.getText();
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
         _txtFilepath.setText(selectedFilepath);
      }
   }

   private void onSelect_LiveUpdate() {

      // save current settings
      if (_chkLiveUpdate.getSelection() == true) {
         saveState();
      }

      updateUI_DialogButtons();

      enableControls();
   }

   private void onSelect_LiveUpdateControls() {

      if (isLiveUpdate() == false) {
         return;
      }

      // update model
      saveState_LiveUpdateControls(_mapModel_Editing);

      // update UI
      MapModelManager.updateUI();
   }

   private void restoreState() {

      if (_mapModel_Editing == null) {

         // a new model is being added

         return;
      }

// SET_FORMATTING_OFF

      _txtName                   .setText(_mapModel_Editing.name);
      _txtDescription            .setText(_mapModel_Editing.description);
      _txtFilepath               .setText(_mapModel_Editing.filepath);

      _spinnerForwardAngle       .setSelection(_mapModel_Editing.forwardAngle);
      _spinnerHeadPositionFactor .setSelection((int) (_mapModel_Editing.headPositionFactor * 10));

// SET_FORMATTING_ON

      _chkLiveUpdate.setSelection(Util.getStateBoolean(_state, STATE_IS_LIVE_UPDATE, false));
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

      /*
       * Update model
       */
// SET_FORMATTING_OFF

      mapModel.name        = _txtName        .getText().strip();
      mapModel.description = _txtDescription .getText().strip();
      mapModel.filepath    = _txtFilepath    .getText().strip();

// SET_FORMATTING_ON

      saveState_LiveUpdateControls(mapModel);

      // update UI
      MapModelManager.updateUI();
   }

   private void saveState_LiveUpdate() {

      _state.put(STATE_IS_LIVE_UPDATE, _chkLiveUpdate.getSelection());
   }

   private void saveState_LiveUpdateControls(final MapModel mapModel) {

// SET_FORMATTING_OFF

      mapModel.forwardAngle         = _spinnerForwardAngle.getSelection();
      mapModel.headPositionFactor   = _spinnerHeadPositionFactor.getSelection() / 10.0f;

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

   private void updateUI_DialogButtons() {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      final Button cancelButton = getButton(IDialogConstants.CANCEL_ID);

      final boolean isExistingModel = _mapModel_Editing != null;
      final boolean isDefaultModel = isExistingModel && _mapModel_Editing.isDefaultModel;

      if (isLiveUpdate() || isDefaultModel) {

         okButton.setVisible(false);

         cancelButton.setText(Messages.App_Action_Close);

      } else {

         okButton.setVisible(true);

         // set text for the OK button
         if (_mapModel_Editing == null) {

            // a new model is added
            okButton.setText(Messages.App_Action_Add);

         } else {

            // existing model is edited
            okButton.setText(Messages.App_Action_Save);
         }

         cancelButton.setText(Messages.App_Action_Cancel);
      }
   }
}
