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
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;

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

public class DialogCustomTourNutritionProduct extends Dialog {

   private static final int HINT_TEXT_COLUMN_WIDTH = UI.IS_OSX ? 100 : 50;
   private int              _calories;
   private boolean          _isBeverage;
   private int              _sodium;
   private String           _name                  = UI.EMPTY_STRING;
   private int              _numServings           = 1;
   private int              _beverageQuantity;

   /*
    * UI controls
    */
   private boolean        _isInUIInit;
   private PixelConverter _pc;

   private Button         _checkIsBeverage;

   private Spinner        _spinnerNumServings;
   private Spinner        _spinnerBeverageQuantity;

   private Text           _txtCalories;
   private Text           _txtName;
   private Text           _txtSodium;

   public DialogCustomTourNutritionProduct(final Shell parentShell) {
      super(parentShell);
   }

   @Override
   public void create() {

      super.create();

      getShell().setText(Messages.Dialog_CustomTourNutritionProduct_Title);

      validateFields();
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
            _txtName.addModifyListener(event -> onModifyName(event));
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(20), SWT.DEFAULT)
                  .span(2, 1)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtName);
         }
         {
            // Label: number of servings
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_NumberServings);

            // Spinner: number of servings
            _spinnerNumServings = UI.createSpinner(container, 2, 25, 10000, 25, 100);
            _spinnerNumServings.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onCapacityModified()));
            _spinnerNumServings.addModifyListener(event -> onCapacityModified());
            _spinnerNumServings.addMouseWheelListener(mouseEvent -> {

               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 25);

               onCapacityModified();
            });
            GridDataFactory.fillDefaults().hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT).span(2, 1).align(SWT.BEGINNING, SWT.CENTER).applyTo(
                  _spinnerNumServings);
         }
         {
            // Label: calories
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_Calories);
            _txtCalories = new Text(container, SWT.BORDER);
            _txtCalories.addModifyListener(event -> onModifyCalories(event));
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtCalories);

            // Unit: kcal
            UI.createLabel(container, net.tourbook.ui.Messages.Value_Unit_KCalories);
         }
         {
            // Label: sodium
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_Sodium);

            _txtSodium = new Text(container, SWT.BORDER);
            _txtSodium.addModifyListener(event -> onModifySodium(event));
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_txtSodium);

            // Unit: mg
            UI.createLabel(container, UI.UNIT_WEIGHT_MG);
         }
         {
            // Checkbox: Is Beverage
            _checkIsBeverage = new Button(container, SWT.CHECK);
            _checkIsBeverage.setText(Messages.Dialog_CustomTourNutritionProduct_Label_IsBeverage);
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_checkIsBeverage);
         }
         {
            // Label: Beverage quantity
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_BeverageQuantity);

            _spinnerBeverageQuantity = UI.createSpinner(container, 2, 0, 10000, 25, 100);
            _spinnerBeverageQuantity.addSelectionListener(widgetSelectedAdapter(selectionEvent -> onBeverageQuantityModified()));
            _spinnerBeverageQuantity.addModifyListener(event -> onBeverageQuantityModified());
            _spinnerBeverageQuantity.addMouseWheelListener(mouseEvent -> {

               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 25);

               onBeverageQuantityModified();
            });
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerBeverageQuantity);

            // Unit: L
            UI.createLabel(container, UI.UNIT_FLUIDS_L);
         }
      }
   }

   private void enableOK(final boolean isEnabled) {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   public TourNutritionProduct getTourNutritionProduct(final TourData tourData) {

      final TourNutritionProduct product = new TourNutritionProduct(tourData, true);
      product.setName(_name.trim());
      final QuantityType quantityType = _numServings == 1
            ? QuantityType.Servings
            : QuantityType.Products;

      product.setQuantityType(quantityType);

      product.setCalories(_calories);
      product.setCalories_Serving((int) Math.round(_calories * 1.0 / _numServings));

      product.setSodium(_sodium);
      product.setSodium_Serving((int) Math.round(_sodium * 1.0 / _numServings));

      product.setIsBeverage(_isBeverage);
      if (_isBeverage) {
         product.setBeverageQuantity(_beverageQuantity);
         product.setBeverageQuantity_Serving((int) Math.round(_beverageQuantity * 1.0 / _numServings));
      }

      return product;
   }

   @Override
   protected void okPressed() {

      _name = _txtName.getText();
      _numServings = (int) Math.round(_spinnerNumServings.getSelection() / 100.0);
      _calories = UI.verifyIntegerValue(_txtCalories.getText()) ? Integer.valueOf(_txtCalories.getText()) : 0;
      _sodium = UI.verifyIntegerValue(_txtSodium.getText()) ? Integer.valueOf(_txtSodium.getText()) : 0;
      _isBeverage = _checkIsBeverage.getSelection();
      _beverageQuantity = _spinnerBeverageQuantity.getSelection();

      super.okPressed();
   }

   private void onBeverageQuantityModified() {

      if (_isInUIInit) {
         return;
      }

      _beverageQuantity = _spinnerBeverageQuantity.getSelection();

      validateFields();
   }

   private void onCapacityModified() {

      if (_isInUIInit) {
         return;
      }

      _numServings = _spinnerNumServings.getSelection();

      validateFields();
   }

   private void onModifyCalories(final ModifyEvent event) {

      final Text textWidget = (Text) event.getSource();
      final String userText = textWidget.getText();
      if (UI.verifyIntegerValue(userText)) {

         _calories = Integer.parseInt(userText);
      }

      validateFields();
   }

   private void onModifyName(final ModifyEvent event) {

      final Text textWidget = (Text) event.getSource();
      final String userText = textWidget.getText();
      _name = userText;

      validateFields();
   }

   private void onModifySodium(final ModifyEvent event) {

      final Text textWidget = (Text) event.getSource();
      final String userText = textWidget.getText();
      if (UI.verifyIntegerValue(userText)) {

         _sodium = Integer.parseInt(userText);
      }

      validateFields();
   }

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }
      final boolean isCustomTourNutritionProductValid = StringUtils.hasContent(_txtName.getText());
      enableOK(isCustomTourNutritionProductValid);
   }
}
