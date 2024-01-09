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

import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogBeverageContainer extends Dialog {

   private Text   _txtName;
   private Text   _txtCapacity;
   private String name = UI.EMPTY_STRING;
   private float  capacity;

   public DialogBeverageContainer(final Shell parentShell) {
      super(parentShell);
   }

   // override method to use "Login" as label for the OK button
   @Override
   protected void createButtonsForButtonBar(final Composite parent) {

      createButton(parent, IDialogConstants.OK_ID, "Name", true);
      createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite container = (Composite) super.createDialogArea(parent);
      final GridLayout layout = new GridLayout(2, false);
      layout.marginRight = 5;
      layout.marginLeft = 10;
      container.setLayout(layout);

      final Label lblUser = UI.createLabel(container, "User");

      _txtName = new Text(container, SWT.BORDER);
      _txtName.setLayoutData(new GridData(SWT.FILL,
            SWT.CENTER,
            true,
            false,
            1,
            1));
      _txtName.setText(name);
      _txtName.addModifyListener(e -> {
         final Text textWidget = (Text) e.getSource();
         final String userText = textWidget.getText();
         name = userText;
      });

      final Label lblCapacity = UI.createLabel(container, "Capacity");

      final GridData gridDataPasswordLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false);
      gridDataPasswordLabel.horizontalIndent = 1;
      lblCapacity.setLayoutData(gridDataPasswordLabel);

      _txtCapacity = new Text(container, SWT.BORDER);
      _txtCapacity.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      _txtCapacity.addModifyListener(e -> {
         final Text textWidget = (Text) e.getSource();
         final String capacityText = textWidget.getText();
         capacity = Float.parseFloat(capacityText);
      });
      return container;
   }

   public float getCapacity() {
      return capacity;
   }

   public String getName() {
      return name;
   }

   @Override
   protected void okPressed() {
      name = _txtName.getText();
      capacity = Float.parseFloat(_txtCapacity.getText());
      super.okPressed();
   }

   public void setCapacity(final float capacity) {
      this.capacity = capacity;
   }

   public void setName(final String name) {
      this.name = name;
   }

}
