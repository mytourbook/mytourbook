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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.Util;
import net.tourbook.nutrition.TourNutritionProductMenuManager;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageNutrition extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String            ID         = "net.tourbook.preferences.PrefPageNutrition"; //$NON-NLS-1$

   private static final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

// SET_FORMATTING_OFF

   public static final String PRODUCT_VIEW_DOUBLE_CLICK_ACTION__EDIT_PRODUCT         = "PRODUCT_VIEW_DOUBLE_CLICK_ACTION__EDIT_PRODUCT"; //$NON-NLS-1$
   public static final String PRODUCT_VIEW_DOUBLE_CLICK_ACTION__OPEN_PRODUCT_WEBSITE = "PRODUCT_VIEW_DOUBLE_CLICK_ACTION__OPEN_PRODUCT_WEBSITE";                                                                                  //$NON-NLS-1$


   private String[][] _allDoubleClickActions = new String[][]
   {
      { "Open Product Website",  PRODUCT_VIEW_DOUBLE_CLICK_ACTION__EDIT_PRODUCT },
      { "Edit Product",          PRODUCT_VIEW_DOUBLE_CLICK_ACTION__OPEN_PRODUCT_WEBSITE },
   };

// SET_FORMATTING_ON

   private PixelConverter     _pc;
   private MouseWheelListener _defaultMouseWheelListener;

   private int                _hintDefaultSpinnerWidth;

   /*
    * UI controls
    */
   private Button  _chkIgnoreFirstHour;

   private Combo   _comboProductDoubleClick;

   private Spinner _spinnerRecentProducts;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      fillUI();

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
//            .spacing(5, 15)
            .numColumns(3)
            .applyTo(container);
      {
         /*
          * Ignore the 1st hour for nutrition averages computation
          */
         {
            _chkIgnoreFirstHour = new Button(container, SWT.CHECK);
            _chkIgnoreFirstHour.setText(Messages.PrefPage_Nutrition_Checkbox_IgnoreFirstHour);
            _chkIgnoreFirstHour.setToolTipText(Messages.PrefPage_Nutrition_Checkbox_IgnoreFirstHour_Tooltip);
            GridDataFactory.fillDefaults().span(3, 1).align(SWT.BEGINNING, SWT.FILL).applyTo(_chkIgnoreFirstHour);
         }

         {
            /*
             * Number of recent products
             */
            final String tooltip = Messages.Pref_Nutrition_Label_NumberOfRecentProducts_Tooltip;

            final Label label = UI.createLabel(container, Messages.Pref_Nutrition_Label_NumberOfRecentProducts);
            label.setToolTipText(tooltip);

            // spinner
            _spinnerRecentProducts = new Spinner(container, SWT.BORDER);
            _spinnerRecentProducts.setToolTipText(tooltip);
            _spinnerRecentProducts.setMinimum(0);
            _spinnerRecentProducts.setMaximum(9);
            _spinnerRecentProducts.addMouseWheelListener(_defaultMouseWheelListener);
            GridDataFactory.fillDefaults()
                  .hint(_hintDefaultSpinnerWidth, SWT.DEFAULT)
                  .align(SWT.BEGINNING, SWT.CENTER)
                  .applyTo(_spinnerRecentProducts);

            // button: Remove recent products
            final Button btnRemoveRecentProducts = new Button(container, SWT.PUSH);
            btnRemoveRecentProducts.setText(Messages.Pref_Nutrition_Button_RemoveRecentProducts);
            btnRemoveRecentProducts.setToolTipText(Messages.Pref_Nutrition_Button_RemoveRecentProducts_Tooltip);
            btnRemoveRecentProducts.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                  selectionEvent -> TourNutritionProductMenuManager.clearRecentProducts()));
         }
         {
            /*
             * Default double click
             */
            {
               final Label lblDefaultCadence = new Label(container, SWT.FILL | SWT.LEFT);
               lblDefaultCadence.setText("Product view &double click action");
               lblDefaultCadence.setToolTipText("Action when a product is double clicked in the product view");

               _comboProductDoubleClick = new Combo(container, SWT.READ_ONLY | SWT.BORDER);
               _comboProductDoubleClick.setVisibleItemCount(10);
               _comboProductDoubleClick.addSelectionListener(SelectionListener.widgetSelectedAdapter(
                     selectionEvent -> onSelect_ProductDoubleClick()));

               GridDataFactory.fillDefaults()
                     .align(SWT.FILL, SWT.CENTER)
                     .span(2, 1)
                     .applyTo(_comboProductDoubleClick);
            }
         }
      }

      return container;
   }

   private void fillUI() {

      // fill action labels
      for (final String[] action : _allDoubleClickActions) {
         _comboProductDoubleClick.add(action[0]);
      }
   }

   @Override
   public void init(final IWorkbench workbench) {}

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _hintDefaultSpinnerWidth = _pc.convertWidthInCharsToPixels(3);

      _defaultMouseWheelListener = mouseEvent -> {

         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
      };
   }

   private void onSelect_ProductDoubleClick() {
      // TODO Auto-generated method stub

   }

   @Override
   protected void performDefaults() {

      _chkIgnoreFirstHour.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR));
      _spinnerRecentProducts.setSelection(_prefStore.getDefaultInt(ITourbookPreferences.NUTRITION_NUMBER_OF_RECENT_PRODUCTS));

      super.performDefaults();
   }

   @Override
   public boolean performOk() {

      final boolean isOK = super.performOk();

      if (isOK) {

         _prefStore.setValue(
               ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR,
               _chkIgnoreFirstHour.getSelection());
         _prefStore.setValue(
               ITourbookPreferences.NUTRITION_NUMBER_OF_RECENT_PRODUCTS,
               _spinnerRecentProducts.getSelection());
      }

      return isOK;
   }

   private void restoreState() {

      _chkIgnoreFirstHour.setSelection(_prefStore.getBoolean(ITourbookPreferences.NUTRITION_IGNORE_FIRST_HOUR));
      _spinnerRecentProducts.setSelection(_prefStore.getInt(ITourbookPreferences.NUTRITION_NUMBER_OF_RECENT_PRODUCTS));
   }

}
