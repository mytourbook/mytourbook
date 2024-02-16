/*******************************************************************************
 * Copyright (C) 2024 Frédéric Bard
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
package net.tourbook.nutrition;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

class DialogBeverageContainer extends Dialog {

   private float  _capacity = 0.25f;
   private String _name     = UI.EMPTY_STRING;
   private String _title    = UI.EMPTY_STRING;

   /*
    * UI controls
    */
   private boolean        _isInUIInit;
   private PixelConverter _pc;

   private Spinner        _spinnerCapacity;
   private Text           _txtName;

   DialogBeverageContainer(final Shell parentShell, final boolean isEdit) {

      super(parentShell);
      _title = isEdit ? Messages.Dialog_BeverageContainer_Title_Edit : Messages.Dialog_BeverageContainer_Title_Create;
   }

   @Override
   public void create() {

      super.create();

      getShell().setText(_title);

      validateFields();
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite container = (Composite) super.createDialogArea(parent);

      _pc = new PixelConverter(container);

      _isInUIInit = true;
      {
         createUI(container);
         updateUI();
      }
      _isInUIInit = false;

      return container;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         // Label: container name
         UI.createLabel(container, Messages.Dialog_BeverageContainer_Label_Name);

         _txtName = new Text(container, SWT.BORDER);
         _txtName.addModifyListener(event -> onModifyName(event));
         GridDataFactory.fillDefaults()
               .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
               .span(2, 1)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_txtName);

         // Label: container capacity
         UI.createLabel(container, Messages.Dialog_BeverageContainer_Label_Capacity);

         // Text: container capacity in L
         _spinnerCapacity = UI.createSpinner(container, 2, 25, 10000, 25, 100);
         _spinnerCapacity.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onCapacityModified()));
         _spinnerCapacity.addModifyListener(event -> validateFields());
         _spinnerCapacity.addMouseWheelListener(mouseEvent -> {

            UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 25);

            onCapacityModified();
         });
         GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT).align(SWT.END, SWT.CENTER).applyTo(
               _spinnerCapacity);

         // Unit: L
         UI.createLabel(container, UI.UNIT_FLUIDS_L);
      }
   }

   private void enableOK(final boolean isEnabled) {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   public float getCapacity() {
      return _capacity;
   }

   public String getName() {
      return _name;
   }

   private void onCapacityModified() {

      if (_isInUIInit) {
         return;
      }

      _capacity = _spinnerCapacity.getSelection() / 100f;

      validateFields();
   }

   private void onModifyName(final ModifyEvent e) {
      final Text textWidget = (Text) e.getSource();
      _name = textWidget.getText().trim();

      validateFields();
   }

   public void setCapacity(final float capacity) {
      _capacity = capacity;
   }

   public void setName(final String name) {
      _name = name;
   }

   private void updateUI() {

      _txtName.setText(_name);
      _spinnerCapacity.setSelection(Math.round(_capacity * 100));
   }

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      final boolean isContainerValid = StringUtils.hasContent(_txtName.getText());
      enableOK(isContainerValid);
   }
}
