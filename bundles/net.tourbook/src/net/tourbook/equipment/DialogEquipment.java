/*******************************************************************************
 * Copyright (C) 2025, 2026 Wolfgang Schramm and Contributors
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
package net.tourbook.equipment;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.data.Equipment;
import net.tourbook.data.TourTag;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * Dialog to modify a {@link TourTag}
 */
public class DialogEquipment extends TitleAreaDialog {

   private static final String          ID     = "net.tourbook.equipment.DialogEquipment"; //$NON-NLS-1$

   private static final IDialogSettings _state = TourbookPlugin.getState(ID);

   private Equipment                    _equipment_NewOrCloned;
   private Equipment                    _equipment_Original;

   private boolean                      _isOkPressed;
   private boolean                      _isNewEquipment;

   /*
    * UI resources
    */

   /*
    * UI controls
    */
   private Text      _txtDescription;
   private Text      _txtModel;
   private Text      _txtBrand;

   private Composite _container;

   public DialogEquipment(final Shell parentShell, final Equipment equipment) {

      super(parentShell);

      _isNewEquipment = equipment == null;

      if (_isNewEquipment) {

         _equipment_NewOrCloned = new Equipment();

      } else {

         _equipment_NewOrCloned = equipment.clone();
         _equipment_Original = equipment;
      }

      // make dialog resizable
      setShellStyle(getShellStyle() | SWT.RESIZE);
   }


   @Override
   protected void configureShell(final Shell shell) {

      super.configureShell(shell);

      // set window title
      shell.setText(Messages.Dialog_Equipment_Title);

      shell.addDisposeListener(disposeEvent -> dispose());
   }

   @Override
   public void create() {

      super.create();

      final String newTitle = _isNewEquipment
            ? Messages.Dialog_Equipment_Message_Equipment_New
            : Messages.Dialog_Equipment_Message_Equipment_Edit.formatted(_equipment_NewOrCloned.getName());

      setTitle(newTitle);

//      setMessage("message");
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // OK -> Create/Save
      getButton(IDialogConstants.OK_ID).setText(_isNewEquipment
            ? Messages.App_Action_Create
            : Messages.App_Action_Save);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite dlgContainer = (Composite) super.createDialogArea(parent);

      createUI(dlgContainer);

      updateUIFromModel();

//      _txtBrand.selectAll();
      _txtBrand.setFocus();

      enableControls();

      return dlgContainer;
   }

   private void createUI(final Composite parent) {

      _container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(_container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(_container);
//      _container.setBackground(UI.SYS_COLOR_CYAN);
      {
         {
            /*
             * Name/brand
             */

            final Label label = new Label(_container, SWT.NONE);
            label.setText(Messages.Dialog_Equipment_Label_Brand);
            label.setToolTipText(Messages.Dialog_Equipment_Label_Brand_Tooltip);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _txtBrand = new Text(_container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtBrand);
         }
         {
            /*
             * Subname/model
             */

            final Label label = new Label(_container, SWT.NONE);
            label.setText(Messages.Dialog_Equipment_Label_Model);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).applyTo(label);

            _txtModel = new Text(_container, SWT.BORDER);
            GridDataFactory.fillDefaults().grab(true, false).applyTo(_txtModel);
         }
         {
            /*
             * Description
             */
            final Label label = new Label(_container, SWT.NONE);
            label.setText(Messages.Dialog_Equipment_Label_Description);
            GridDataFactory.fillDefaults().align(SWT.FILL, SWT.BEGINNING).applyTo(label);

            _txtDescription = new Text(_container, SWT.BORDER | SWT.WRAP | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
            GridDataFactory.fillDefaults()
                  .grab(true, true)
                  .hint(convertWidthInCharsToPixels(100), convertHeightInCharsToPixels(20))
                  .applyTo(_txtDescription);
         }
      }
   }

   private void dispose() {

   }

   private void enableControls() {

   }

   @Override
   protected IDialogSettings getDialogBoundsSettings() {

      // keep window size and position
      return _state;
   }

   Equipment getEquipment() {

      return _equipment_NewOrCloned;
   }

   @Override
   protected void okPressed() {

      updateModelFromUI();

      if (_equipment_NewOrCloned.isValidForSave() == false) {

         // data are not valid to be saved which is done in the action which opened this dialog

         return;
      }

      // update original model
      _equipment_Original.updateFromModified(_equipment_NewOrCloned);

      super.okPressed();
   }

   private void updateModelFromUI() {

      _equipment_NewOrCloned.setBrand(_txtBrand.getText().trim());
      _equipment_NewOrCloned.setModel(_txtModel.getText().trim());
      _equipment_NewOrCloned.setDescription(_txtDescription.getText().trim());
   }

   private void updateUIFromModel() {

      _txtBrand.setText(_equipment_NewOrCloned.getBrand());
      _txtModel.setText(_equipment_NewOrCloned.getModel());
      _txtDescription.setText(_equipment_NewOrCloned.getDescription());
   }

}
