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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class PrefPageNutrition extends PreferencePage implements IWorkbenchPreferencePage {

   public static final String     ID         = "net.tourbook.preferences.PrefPageNutrition"; //$NON-NLS-1$

   private final IPreferenceStore _prefStore = TourbookPlugin.getPrefStore();

   private PixelConverter         _pc;
   private MouseWheelListener     _defaultMouseWheelListener;

   private int                    _hintDefaultSpinnerWidth;

   /*
    * UI controls
    */
   private Button  _chkIgnoreFirstHour;

   private Spinner _spinnerRecentProducts;

   @Override
   protected Control createContents(final Composite parent) {

      initUI(parent);

      final Composite container = createUI(parent);

      restoreState();

      return container;
   }

   private Composite createUI(final Composite parent) {

      final Composite container = new Composite(parent, SWT.NONE);
      GridDataFactory.fillDefaults().grab(true, false).applyTo(container);
      GridLayoutFactory.fillDefaults()
            .spacing(5, 15)
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
      }

      return container;
   }

   @Override
   public void init(final IWorkbench workbench) {
      // Nothing to do
   }

   private void initUI(final Composite parent) {

      _pc = new PixelConverter(parent);

      _hintDefaultSpinnerWidth = _pc.convertWidthInCharsToPixels(3);

      _defaultMouseWheelListener = mouseEvent -> {

         Util.adjustSpinnerValueOnMouseScroll(mouseEvent);
      };
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
