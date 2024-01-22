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
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class DialogTourNutritionProduct extends Dialog {
   //todo fb disable the add button if the field validation is not correct
   // todo fb use balloon tooltip to validate the container capacity
   private Text _txtName;
   //Text number of servings
   private Text _txtCalories_Serving;
   private Text _txtSodium_Serving;
   //checkbox isbeverage
   //Text beverage quantity
   private String name = UI.EMPTY_STRING;
   private int    beverageQuantity;

   public DialogTourNutritionProduct(final Shell parentShell) {
      super(parentShell);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite container = (Composite) super.createDialogArea(parent);
      final GridLayout layout = new GridLayout(2, false);
      layout.marginRight = 5;
      layout.marginLeft = 10;
      container.setLayout(layout);

      UI.createLabel(container, "User");

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

      _txtCalories_Serving = new Text(container, SWT.BORDER);
      _txtCalories_Serving.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
      _txtCalories_Serving.addModifyListener(e -> {
         final Text textWidget = (Text) e.getSource();
         //todo fb verify that it's a valid int
         //enableControls
      });
      return container;
   }

   public float getBeverageQuantity() {
      return beverageQuantity;
   }

   public String getName() {
      return name;
   }

   public TourNutritionProduct getTourNutritionProduct(final TourData tourData) {

      final TourNutritionProduct product = new TourNutritionProduct(tourData, true);
      product.setName(name.trim());

      return product;
   }

   @Override
   protected void okPressed() {

      name = _txtName.getText();
      beverageQuantity = Integer.parseInt(_txtCalories_Serving.getText());
      super.okPressed();
   }

}
