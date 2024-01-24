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
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class DialogCustomTourNutritionProduct extends Dialog {

   private int    _calories;
   private int    _sodium;
   private String _name = UI.EMPTY_STRING;
   private int    _numServings;
   private float  beverageQuantity;

   //todo fb add the possibility to add custom products (i.e: water...)
// => Manually with a dialog that asks the name, calories, sodium, is beverage (if yes, ungray the beverage qtty)

   /*
    * UI controls
    */
   private boolean        _isInUIInit;
   private PixelConverter _pc;

   //todo fb disable the add button if the field validation is not correct
   private Button  _checkIsBeverage;
   private Spinner _spinnerNumServings;
   private Spinner _spinnerBeverageQuantity;
   private Text    _txtCalories;
   private Text    _txtName;
   private Text    _txtSodium;

   public DialogCustomTourNutritionProduct(final Shell parentShell) {
      super(parentShell);
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite container = (Composite) super.createDialogArea(parent);

      _pc = new PixelConverter(container);

      _isInUIInit = true;
      {
         createUI(container);
      }
      _isInUIInit = false;

      return container;
   }

   private void createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
      GridLayoutFactory.swtDefaults().numColumns(3).applyTo(container);
      {
         {
            // Label: product name
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_Name);

            _txtName = new Text(container, SWT.BORDER);
            _txtName.setText(_name);
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .span(2, 1)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtName);
            _txtName.addModifyListener(e -> {

               // validateFields();

               final Text textWidget = (Text) e.getSource();
               final String userText = textWidget.getText();
               _name = userText;
            });
         }
         {
            // Label: number of servings
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_NumberServings);

            // Spinner: number of servings
            _spinnerNumServings = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT).span(2, 1).align(SWT.BEGINNING, SWT.CENTER).applyTo(
                  _spinnerNumServings);
            _spinnerNumServings.setMinimum(25);
            _spinnerNumServings.setIncrement(25);
            _spinnerNumServings.setMaximum(10000);
            _spinnerNumServings.setDigits(2);
            _spinnerNumServings.addMouseWheelListener(mouseEvent -> Util.adjustSpinnerValueOnMouseScroll(mouseEvent));
            _spinnerNumServings.setSelection(Math.round(_numServings * 100));
            _spinnerNumServings.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {

               if (_isInUIInit) {
                  return;
               }

//            validateFields();
//            onCapacityModified();
            }));
            _spinnerNumServings.addModifyListener(event -> {

               if (_isInUIInit) {
                  return;
               }

//            validateFields();
//            onCapacityModified();
            });
            _spinnerNumServings.addMouseWheelListener(mouseEvent -> {

               Util.adjustSpinnerValueOnMouseScroll(mouseEvent);

//            validateFields();
//            onCapacityModified();
            });
         }
         {
            // Label: calories
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_Calories);

            _txtCalories = new Text(container, SWT.BORDER);
            _txtCalories.setText(String.valueOf(_calories));
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtCalories);
            _txtCalories.addModifyListener(e -> {

               // validateFields();

               final Text textWidget = (Text) e.getSource();
               final String userText = textWidget.getText();
               if (UI.verifyIntegerValue(userText)) {

                  _calories = Integer.parseInt(userText);
               }
            });

            // Unit: kcal
            UI.createLabel(container, UI.UNIT_FLUIDS_L);
         }
         {
            // Label: sodium
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_Sodium);

            _txtSodium = new Text(container, SWT.BORDER);
            _txtSodium.setText(String.valueOf(_sodium));
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtSodium);
            _txtSodium.addModifyListener(e -> {

               // validateFields();

               final Text textWidget = (Text) e.getSource();
               final String userText = textWidget.getText();
               if (UI.verifyIntegerValue(userText)) {

                  _sodium = Integer.parseInt(userText);
               }
            });

            // Unit: mg
            UI.createLabel(container, UI.UNIT_WEIGHT_MG);
         }
         {
            // Label: Is Beverage
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_IsBeverage);

            _checkIsBeverage = new Button(container, SWT.CHECK);
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .span(2, 1)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_checkIsBeverage);
         }
         {
            // Label: Beverage quantity
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_BeverageQuantity);

            _spinnerBeverageQuantity = new Spinner(container, SWT.BORDER);
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerBeverageQuantity);

            // Unit: L
            UI.createLabel(container, UI.UNIT_FLUIDS_L);
         }
      }
   }

   public float getBeverageQuantity() {
      return beverageQuantity;
   }

   public String getName() {
      return _name;
   }

   public TourNutritionProduct getTourNutritionProduct(final TourData tourData) {

      final TourNutritionProduct product = new TourNutritionProduct(tourData, true);
      product.setName(_name.trim());

      return product;
   }

   @Override
   protected void okPressed() {

      _name = _txtName.getText();
      beverageQuantity = Float.parseFloat(_txtCalories.getText());
      super.okPressed();
   }

}
