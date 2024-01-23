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

import net.tourbook.Messages;
import net.tourbook.common.UI;
import net.tourbook.common.util.StringUtils;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class DialogBeverageContainer extends Dialog {

   private static final int DEFAULT_TEXT_WIDTH = 20;

   private String           _name              = UI.EMPTY_STRING;
   private float            _capacity;

   private PixelConverter   _pc;

   /*
    * UI controls
    */

   private Text    _txtName;
   private Text    _txtCapacity;

   private boolean _isInUIInit;

   DialogBeverageContainer(final Shell parentShell) {
      super(parentShell);
   }

   @Override
   public void create() {

      super.create();

      getShell().setText(Messages.Dialog_BeverageContainer_Title);

      validateFields();
   }

   @Override
   protected final void createButtonsForButtonBar(final Composite parent) {

      super.createButtonsForButtonBar(parent);

      // set text for the OK button
      getButton(IDialogConstants.OK_ID).setText(Messages.dialog_export_btn_export);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite container = (Composite) super.createDialogArea(parent);

      _pc = new PixelConverter(container);

      createUI(container);

      return container;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         // Label: container name
         UI.createLabel(container, Messages.Dialog_BeverageContainer_Label_Name);

         // Text: container name
         _txtName = new Text(container, SWT.BORDER);
         _txtName.setText(_name);
         GridDataFactory.fillDefaults()
               .hint(_pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH), SWT.DEFAULT)
               .span(2, 1)
               .align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_txtName);
         _txtName.addModifyListener(e -> {

            validateFields();

            final Text textWidget = (Text) e.getSource();
            final String userText = textWidget.getText();
            _name = userText;
         });

         // Label: container name
         UI.createLabel(container, Messages.Dialog_BeverageContainer_Label_Capacity);

         //todo fb use a spinner instead
         // Text: container capacity in L
         _txtCapacity = new Text(container, SWT.BORDER);
         _txtCapacity.setText(String.valueOf(_capacity));
         GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH), SWT.DEFAULT).align(SWT.END, SWT.CENTER).applyTo(
               _txtCapacity);
         _txtCapacity.addModifyListener(e -> {

            final Text textWidget = (Text) e.getSource();
            final String capacityText = textWidget.getText();
            _capacity = Float.parseFloat(capacityText);
         });

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

   public float get_capacity() {
      return _capacity;
   }

   public String get_name() {
      return _name;
   }

   @Override
   protected void okPressed() {

      _name = _txtName.getText();
      _capacity = Float.parseFloat(_txtCapacity.getText());

      super.okPressed();
   }

   public void set_capacity(final float capacity) {
      _capacity = capacity;
   }

   public void set_name(final String name) {
      _name = name;
   }

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      final boolean isContainerValid = StringUtils.hasContent(_name);//todo fb && StringUtils.hasContent(_capacity);
      enableOK(isContainerValid);

   }
}
