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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

class DialogBeverageContainer extends Dialog {

   private static final int DEFAULT_TEXT_WIDTH = 10;

   private Text             _txtName;
   private Text             _txtCapacity;
   private String           _name              = UI.EMPTY_STRING;
   private float            _capacity;

   private PixelConverter   _pc;

   DialogBeverageContainer(final Shell parentShell) {
      super(parentShell);
   }

   @Override
   protected void configureShell(final Shell shell) {
      super.configureShell(shell);
      shell.setText(Messages.Dialog_BeverageContainer_Title);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite container = (Composite) super.createDialogArea(parent);

      createUI(container);

      return container;
   }

   private void createUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(2).applyTo(container);
      {
         // Label: container name
         UI.createLabel(container, Messages.Dialog_BeverageContainer_Label_Name);

         // Text: container name
         _txtName = new Text(container, SWT.BORDER);
         _txtName.setText(_name);
         GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(DEFAULT_TEXT_WIDTH), SWT.DEFAULT).align(SWT.BEGINNING, SWT.CENTER)
               .applyTo(_txtName);
         _txtName.addModifyListener(e -> {

            final Text textWidget = (Text) e.getSource();
            final String userText = textWidget.getText();
            _name = userText;
         });

         // Label: container name
         UI.createLabel(container, Messages.Dialog_BeverageContainer_Label_Capacity);

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

}
