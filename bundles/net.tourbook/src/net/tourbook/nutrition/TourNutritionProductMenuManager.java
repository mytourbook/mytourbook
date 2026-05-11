/*******************************************************************************
  * Copyright (C) 2026 Frédéric Bard
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.LRUMap;
import net.tourbook.common.util.StringUtils;
import net.tourbook.data.TourData;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.nutrition.openfoodfacts.Product;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;

/**
 * Manage recently used tour nutrition products and fills the context menu
 * <p>
 * The method {@link #fillMenuWithRecentTourNutritionProducts} creates the actions and must be
 * called
 * before the
 * actions are enabled/disabled with {@link #enableRecentTourNutritionProductActions}
 */
public class TourNutritionProductMenuManager {

   private static final String                                STATE_ID                        =
         "TourNutritionProductManager.RecentTourNutritionProducts";                                                                              //$NON-NLS-1$
   private static final String                                STATE_TOUR_NUTRITION_PRODUCT_ID = "TourNutritionProductId";                        //$NON-NLS-1$

   private static final IPreferenceStore                      _prefStore                      = TourbookPlugin.getDefault().getPreferenceStore();

   /**
    * Tour nutrition product manager state is saved in {@link #STATE_ID}
    */
   private static IDialogSettings                             _state                          = TourbookPlugin.getDefault()                      //
         .getDialogSettingsSection(STATE_ID);

   /**
    * Number of tour nutrition products that are displayed in the context menu or
    * saved in the dialog settings, its max number is 9 to have a unique accelerator key
    */
   private static int                                         _maxTourNutritionProducts;

   private static LinkedHashMap<String, TourNutritionProduct> _recentTourNutritionProducts    = new LRUMap<>(10);

   private static TourNutritionProductMenuManager             _currentInstance;

   /**
    * Contains actions which are displayed in the menu
    */
   private static RecentTourNutritionProductAction[]          _actionsRecentTourNutritionProducts;

   private static IPropertyChangeListener                     _prefChangeListener;

   private static boolean                                     _isInitialized                  = false;

   private TourData                                           _tourData;

   public static class RecentTourNutritionProductAction extends Action {

      private Map.Entry<String, TourNutritionProduct> __recentTourNutritionProduct;

      @Override
      public void run() {

         _currentInstance.setTourNutritionProductIntoTour(__recentTourNutritionProduct);
      }

      private void setRecentTourNutritionProduct(final Map.Entry<String, TourNutritionProduct> recentTourNutritionProduct) {
         __recentTourNutritionProduct = recentTourNutritionProduct;
      }
   }

   private static void addPrefChangeListener() {

      // create pref listener
      _prefChangeListener = propertyChangeEvent -> {
         final String property = propertyChangeEvent.getProperty();

         // check if the number of recent tour nutrition products has changed
         if (property.equals(ITourbookPreferences.NUTRITION_NUMBER_OF_RECENT_PRODUCTS)) {

            setActions();
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   public static void clearRecentProducts() {

      _recentTourNutritionProducts.clear();
   }

   private static String getProductCodeFromTourNutritionProductId(final String tourNutritionProductId) {

      try {
         return tourNutritionProductId.substring(
               0,
               tourNutritionProductId.indexOf(UI.SYMBOL_MINUS)).trim();

      } catch (final StringIndexOutOfBoundsException e) {
         // ignore
         return null;
      }
   }

   private static String getProductNameFromTourNutritionProductId(final String tourNutritionProductId) {

      try {
         return tourNutritionProductId.substring(
               tourNutritionProductId.indexOf(UI.SYMBOL_MINUS) + 1).trim();

      } catch (final StringIndexOutOfBoundsException e) {
         // ignore
         return null;
      }
   }

   private static String getTourNutritionProductId(final TourNutritionProduct tourNutritionProduct) {

      return tourNutritionProduct.getProductCode() + UI.SYMBOL_MINUS + tourNutritionProduct.getName();
   }

   private static synchronized void initTourNutritionProductManager() {

      setActions();

      addPrefChangeListener();

      _isInitialized = true;
   }

   public static void restoreState() {

      final String[] allStateTourNutritionProductIds = _state.getArray(STATE_TOUR_NUTRITION_PRODUCT_ID);

      if (allStateTourNutritionProductIds == null) {
         return;
      }

      for (final String tourNutritionProductId : allStateTourNutritionProductIds) {

         _recentTourNutritionProducts.put(tourNutritionProductId, null);
      }
   }

   public static void saveState() {

      if (_maxTourNutritionProducts < 1) {
         // tour nutrition products are not initialized or not visible, do nothing
         return;
      }

      final String[] stateTourNutritionProductIds = new String[Math.min(
            _maxTourNutritionProducts,
            _recentTourNutritionProducts.size())];
      int tourNutritionProductIndex = 0;

      for (final Map.Entry<String, TourNutritionProduct> recentTourNutritionProduct : _recentTourNutritionProducts.entrySet()) {

         stateTourNutritionProductIds[tourNutritionProductIndex++] =
               recentTourNutritionProduct.getKey();

         if (tourNutritionProductIndex == _maxTourNutritionProducts) {
            break;
         }
      }

      _state.put(STATE_TOUR_NUTRITION_PRODUCT_ID, stateTourNutritionProductIds);
   }

   /**
    * create actions for recent tour nutrition products
    */
   private static void setActions() {

      _maxTourNutritionProducts = TourbookPlugin
            .getDefault()
            .getPreferenceStore()
            .getInt(ITourbookPreferences.NUTRITION_NUMBER_OF_RECENT_PRODUCTS);

      _actionsRecentTourNutritionProducts = new RecentTourNutritionProductAction[_maxTourNutritionProducts];

      for (int actionIndex = 0; actionIndex < _actionsRecentTourNutritionProducts.length; actionIndex++) {
         _actionsRecentTourNutritionProducts[actionIndex] = new RecentTourNutritionProductAction();
      }
   }

   private void enableRecentTourNutritionProductActions() {

      if (_isInitialized == false) {
         initTourNutritionProductManager();
      }

      for (final RecentTourNutritionProductAction actionRecentTourNutritionProduct : _actionsRecentTourNutritionProducts) {

         final Map.Entry<String, TourNutritionProduct> tourNutritionProduct =
               actionRecentTourNutritionProduct.__recentTourNutritionProduct;

         boolean isEnabled = false;
         if (tourNutritionProduct != null) {

            final String recentTourNutritionProductCode = getProductCodeFromTourNutritionProductId(
                  tourNutritionProduct.getKey());

            // If the recent tour nutrition product code is in the list of tour
            // nutrition products, we disable it
            final boolean tourNutritionProductAlreadyExist =
                  NutritionUtils.isProductAlreadyPresent(
                        recentTourNutritionProductCode,
                        _tourData.getTourNutritionProducts());

            isEnabled = tourNutritionProductAlreadyExist == false;
         }

         actionRecentTourNutritionProduct.setEnabled(isEnabled);
      }
   }

   /**
    * Create the menu entries for the recently used tour nutrition products
    */
   public void fillMenuWithRecentTourNutritionProducts(final IMenuManager menuMgr,
                                                       final TourData tourData) {

      _currentInstance = this;

      if (_isInitialized == false) {
         initTourNutritionProductManager();
      }

      if (_recentTourNutritionProducts.isEmpty()) {
         return;
      }

      if (_maxTourNutritionProducts < 1) {
         return;
      }

      _tourData = tourData;

      int tourNutritionProductIndex = 0;
      //iterate over the recent tour nutrition products and add them to the menu
      for (final Map.Entry<String, TourNutritionProduct> recentTourNutritionProduct : _recentTourNutritionProducts.entrySet()) {

         try {

            final RecentTourNutritionProductAction actionRecentTourNutritionProduct =
                  _actionsRecentTourNutritionProducts[tourNutritionProductIndex];
            actionRecentTourNutritionProduct.setRecentTourNutritionProduct(
                  recentTourNutritionProduct);

            final String productCode =
                  getProductCodeFromTourNutritionProductId(recentTourNutritionProduct.getKey());
            final String productName =
                  getProductNameFromTourNutritionProductId(recentTourNutritionProduct.getKey());
            actionRecentTourNutritionProduct.setText(
                  UI.SPACE4 +
                        UI.MNEMONIC +
                        (tourNutritionProductIndex + 1) +
                        UI.SPACE2 +
                        UI.SYMBOL_STAR.repeat(4) +
                        productCode.substring(productCode.length() - 4) +
                        UI.SPACE +
                        UI.SYMBOL_MINUS +
                        UI.SPACE +
                        productName);

            menuMgr.add(actionRecentTourNutritionProduct);

         } catch (final IndexOutOfBoundsException e) {
            // there are no more recent tour nutrition products
            break;
         }

         tourNutritionProductIndex++;
      }

      enableRecentTourNutritionProductActions();
   }

   private void setTourNutritionProductIntoTour(final Map.Entry<String, TourNutritionProduct> recentTourNutritionProduct) {

      final Runnable runnable = () -> {

         TourNutritionProduct tourNutritionProduct = recentTourNutritionProduct.getValue();

         if (tourNutritionProduct == null) {
            // the tour nutrition product is not loaded yet, we need to load it from the database

            final String productCode = getProductCodeFromTourNutritionProductId(
                  recentTourNutritionProduct.getKey());
            final List<Product> searchProductResults = NutritionUtils.searchProduct(
                  productCode,
                  ProductSearchType.ByCode);

            tourNutritionProduct = new TourNutritionProduct(
                  _tourData,
                  searchProductResults.get(0));
         }

         tourNutritionProduct.setTourData(_tourData);

         updateRecentTourNutritionProducts(tourNutritionProduct);

         _tourData.addNutritionProduct(tourNutritionProduct);

         TourManager.saveModifiedTour(_tourData);

      };
      BusyIndicator.showWhile(Display.getCurrent(), runnable);
   }

   public void updateRecentTourNutritionProducts(final TourNutritionProduct tourNutritionProduct) {

      _currentInstance = this;

      final String tourNutritionProductId = getTourNutritionProductId(tourNutritionProduct);

      // If the tour nutrition product Id is not in the list
      if (_recentTourNutritionProducts.containsKey(tourNutritionProductId) == false) {

         _recentTourNutritionProducts.putFirst(
               tourNutritionProductId,
               tourNutritionProduct);

         return;
      }

      // iterate over the recent tour nutrition products and check if the tour nutrition
      // product is already in the list
      for (final Map.Entry<String, TourNutritionProduct> recentTourNutritionProduct : _recentTourNutritionProducts.entrySet()) {

         final String recentTourNutritionProductCode =
               getProductCodeFromTourNutritionProductId(recentTourNutritionProduct.getKey());

         // If the tour nutrition product Id is already in the list and it doesn't have
         // a TourNutritionProduct object, or that object is tied to a beverage container,
         // we insert the TourNutritionProduct object into the list
         if (!StringUtils.isNullOrEmpty(recentTourNutritionProductCode) &&
               recentTourNutritionProductCode.equals(tourNutritionProduct.getProductCode()) &&
               recentTourNutritionProduct.getValue() == null) {

            // update the tour nutrition product in the list
            recentTourNutritionProduct.setValue(tourNutritionProduct);

            return;
         }
      }

      _recentTourNutritionProducts.remove(tourNutritionProductId);
      _recentTourNutritionProducts.putFirst(
            tourNutritionProductId,
            tourNutritionProduct);
   }
}
