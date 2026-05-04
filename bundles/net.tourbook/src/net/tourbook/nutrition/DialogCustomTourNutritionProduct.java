/*******************************************************************************
 * Copyright (C) 2024, 2026 Frédéric Bard
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
import net.tourbook.OtherMessages;
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
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Text;

public class DialogCustomTourNutritionProduct extends Dialog {

   private static final int   HINT_TEXT_COLUMN_WIDTH = UI.IS_OSX ? 100 : 50;
   private int                _calories;
   private int                _carbohydrates;
   private boolean            _isBeverage;
   private int                _sodium;
   private String             _name                  = UI.EMPTY_STRING;
   private int                _numServings           = 1;
   private int                _beverageQuantity;

   private SelectionListener  _defaultSelectionListener;
   private MouseWheelListener _defaultMouseWheelListener;

   /*
    * UI controls
    */
   private boolean              _isInUIInit;
   private boolean              _isEditMode;

   private PixelConverter       _pc;

   private Button               _checkIsBeverage;

   private TourNutritionProduct _product;

   private Spinner              _spinnerNumServings;
   private Spinner              _spinnerBeverageQuantity;
   private Spinner              _spinnerCalories;
   private Spinner              _spinnerCarbohydrates;
   private Spinner              _spinnerSodium;

   private Text                 _txtName;

   public DialogCustomTourNutritionProduct(final Shell parentShell,
                                           final boolean isEditMode,
                                           final TourNutritionProduct product) {

      super(parentShell);
      _isEditMode = isEditMode;
      _product = product;
   }

   @Override
   public void create() {

      super.create();

      getShell().setText(_isEditMode
            ? Messages.Dialog_EditCustomTourNutritionProduct_Title
            : Messages.Dialog_CustomTourNutritionProduct_Title);

      validateFields();
   }

   @Override
   protected Control createDialogArea(final Composite parent) {

      final Composite container = (Composite) super.createDialogArea(parent);

      _defaultSelectionListener = SelectionListener.widgetSelectedAdapter(
            selectionEvent -> enableControls());

      _defaultMouseWheelListener = mouseEvent -> UI.adjustSpinnerValueOnMouseScroll(
            mouseEvent);

      _pc = new PixelConverter(container);

      _isInUIInit = true;
      {
         createUI(container);
         fillUI();
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
            UI.createLabel(
                  container,
                  Messages.Dialog_CustomTourNutritionProduct_Label_NumberServings);

            // Spinner: number of servings
            _spinnerNumServings = UI.createSpinner(container, 2, 100, 10000, 25, 100);
            _spinnerNumServings.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onCapacityModified()));
            _spinnerNumServings.addModifyListener(event -> onCapacityModified());
            _spinnerNumServings.addMouseWheelListener(mouseEvent -> {

               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 25);

               onCapacityModified();
            });
            GridDataFactory.fillDefaults().hint(
                  _pc.convertWidthInCharsToPixels(5),
                  SWT.DEFAULT).span(2, 1)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(
                        _spinnerNumServings);
         }
         {
            // Label: calories
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_Calories);
            _spinnerCalories = new Spinner(container, SWT.BORDER);
            _spinnerCalories.setMinimum(0);
            _spinnerCalories.setMaximum(5000);
            _spinnerCalories.addSelectionListener(_defaultSelectionListener);
            _spinnerCalories.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerCalories);

            // Unit: kcal
            UI.createLabel(container, OtherMessages.VALUE_UNIT_K_CALORIES);
         }
         {
            // Label: carbohydrates
            UI.createLabel(
                  container,
                  Messages.Dialog_CustomTourNutritionProduct_Label_Carbohydrates);
            _spinnerCarbohydrates = new Spinner(container, SWT.BORDER);
            _spinnerCarbohydrates.setMinimum(0);
            _spinnerCarbohydrates.setMaximum(5000);
            _spinnerCarbohydrates.addSelectionListener(_defaultSelectionListener);
            _spinnerCarbohydrates.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerCarbohydrates);

            // Unit: g
            UI.createLabel(container, UI.UNIT_WEIGHT_G);
         }
         {
            // Label: sodium
            UI.createLabel(container, Messages.Dialog_CustomTourNutritionProduct_Label_Sodium);

            _spinnerSodium = new Spinner(container, SWT.BORDER);
            _spinnerSodium.setMinimum(0);
            _spinnerSodium.setMaximum(5000);
            _spinnerSodium.addSelectionListener(_defaultSelectionListener);
            _spinnerSodium.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .hint(HINT_TEXT_COLUMN_WIDTH, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerSodium);

            // Unit: mg
            UI.createLabel(container, UI.UNIT_WEIGHT_MG);
         }
         {
            // Checkbox: Is Beverage
            _checkIsBeverage = new Button(container, SWT.CHECK);
            _checkIsBeverage.setText(Messages.Dialog_CustomTourNutritionProduct_Label_IsBeverage);
            _checkIsBeverage.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> enableControls()));
            GridDataFactory.fillDefaults()
                  .span(3, 1)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_checkIsBeverage);
         }
         {
            // Label: Beverage quantity
            UI.createLabel(
                  container,
                  Messages.Dialog_CustomTourNutritionProduct_Label_BeverageQuantity);

            // Create a spinner with a default of 1L (1,000mL), a maximum of 10L, an
            // increment of 25cL and a page increment of 10cL
            _spinnerBeverageQuantity = UI.createSpinner(container, 3, 0, 10000, 250, 100);
            _spinnerBeverageQuantity.addSelectionListener(widgetSelectedAdapter(
                  selectionEvent -> onBeverageQuantityModified()));
            _spinnerBeverageQuantity.addModifyListener(event -> onBeverageQuantityModified());
            _spinnerBeverageQuantity.addMouseWheelListener(mouseEvent -> {

               UI.adjustSpinnerValueOnMouseScroll(mouseEvent, 25);

               onBeverageQuantityModified();
            });
            _spinnerBeverageQuantity.setSelection(1000);
            _spinnerBeverageQuantity.setEnabled(false);
            GridDataFactory.fillDefaults()
                  .hint(_pc.convertWidthInCharsToPixels(5), SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerBeverageQuantity);

            // Unit: L
            UI.createLabel(container, UI.UNIT_FLUIDS_L);
         }
      }
   }

   private void enableControls() {

      _spinnerBeverageQuantity.setEnabled(_checkIsBeverage.getSelection());
   }

   private void enableOK(final boolean isEnabled) {

      final Button okButton = getButton(IDialogConstants.OK_ID);
      if (okButton != null) {
         okButton.setEnabled(isEnabled);
      }
   }

   private void fillUI() {

      if (!_isEditMode) {
         return;
      }

      _txtName.setText(_product.getName());

      if (_product.getCalories_Serving() != 0) {

         final int numServings = Math.floorDiv(
               _product.getCalories(),
               _product.getCalories_Serving());
         _spinnerNumServings.setSelection(numServings * 100);
      }

      _spinnerCarbohydrates.setSelection(_product.getCarbohydrates());
      _spinnerCalories.setSelection(_product.getCalories());
      _spinnerSodium.setSelection(_product.getSodium());
      _spinnerBeverageQuantity.setSelection(_product.getBeverageQuantity());
      _spinnerBeverageQuantity.setEnabled(_product.isBeverage());
      _checkIsBeverage.setSelection(_product.isBeverage());
   }

   public TourNutritionProduct getTourNutritionProduct(final TourData tourData,
                                                       final TourNutritionProduct originalProduct) {

      final TourNutritionProduct product = new TourNutritionProduct(tourData, true);
      product.setName(_name.trim());
      final QuantityType quantityType = _numServings == 1
            ? QuantityType.Servings
            : QuantityType.Products;

      product.setQuantityType(quantityType);

      product.setCalories(_calories);
      product.setCalories_Serving((int) Math.round(_calories * 1.0 / _numServings));

      product.setCarbohydrates(_carbohydrates);
      product.setCarbohydrates_Serving((int) Math.round(_carbohydrates * 1.0 / _numServings));

      product.setSodium(_sodium);
      product.setSodium_Serving((int) Math.round(_sodium * 1.0 / _numServings));

      product.setIsBeverage(_isBeverage);
      if (_isBeverage) {

         product.setBeverageQuantity(_beverageQuantity);
         product.setBeverageQuantity_Serving((int) Math.round(_beverageQuantity * 1.0 / _numServings));
      }

      if (originalProduct != null) {

         product.setTourBeverageContainer(originalProduct.getTourBeverageContainer());
         product.setContainersConsumed(originalProduct.getContainersConsumed());
      }

      return product;
   }

   @Override
   protected void okPressed() {

      _name = _txtName.getText();
      _numServings = (int) Math.round(_spinnerNumServings.getSelection() / 100.0);
      _calories = _spinnerCalories.getSelection();
      _carbohydrates = _spinnerCarbohydrates.getSelection();
      _sodium = _spinnerSodium.getSelection();
      _isBeverage = _checkIsBeverage.getSelection();
      _beverageQuantity = _isBeverage
            ? _spinnerBeverageQuantity.getSelection()
            : 0;

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

   private void onModifyName(final ModifyEvent event) {

      final Text textWidget = (Text) event.getSource();
      final String userText = textWidget.getText();
      _name = userText;

      validateFields();
   }

   private void validateFields() {

      if (_isInUIInit) {
         return;
      }

      final boolean isCustomTourNutritionProductValid =
            StringUtils.hasContent(_txtName.getText());
      enableOK(isCustomTourNutritionProductValid);
   }
}
