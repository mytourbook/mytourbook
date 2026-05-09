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
import java.util.Map;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
import net.tourbook.common.util.LRUMap;
import net.tourbook.data.TourNutritionProduct;
import net.tourbook.database.TourDatabase;
import net.tourbook.preferences.ITourbookPreferences;

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
   private static LinkedHashMap<String, TourNutritionProduct> _recentTourNutritionProducts    = new LRUMap<>(10);

   /**
    * Contains actions which are displayed in the menu
    */
   private static RecentTourNutritionProductAction[]          _actionsRecentTourNutritionProducts;

   private static int                                         _maxTourNutritionProducts       = -1;

   private static IPropertyChangeListener                     _prefChangeListener;

   private static boolean                                     _isInitialized                  = false;
   private static boolean                                     _isSaveTour;

   private static class RecentTourNutritionProductAction extends Action {

      private Map.Entry<String, TourNutritionProduct> __recentTourNutritionProduct;

      @Override
      public void run() {
         setTourNutritionProductIntoTour(__recentTourNutritionProduct, _isSaveTour);
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

   /**
    * @param isEnabled
    * @param existingTourNutritionProductId
    */
   public static void enableRecentTourNutritionProductActions(final boolean isEnabled,
                                                              final long existingTourNutritionProductId) {

      if (_isInitialized == false) {
         initTourNutritionProductManager();
      }

      for (final RecentTourNutritionProductAction actionRecentTourNutritionProduct : _actionsRecentTourNutritionProducts) {

         final Map.Entry<String, TourNutritionProduct> tourNutritionProduct =
               actionRecentTourNutritionProduct.__recentTourNutritionProduct;
         if (tourNutritionProduct == null) {

            // disable tour type

            actionRecentTourNutritionProduct.setEnabled(false);

            // hide image because it looks ugly (on windows) when it's disabled
            actionRecentTourNutritionProduct.setImageDescriptor(null);

            continue;
         }

         //todo fb
         final long tourNutritionProductId = 1L;//TourNutritionProduct.getTypeId();

         if (isEnabled) {

            // enable tour type

            boolean isExistingTourNutritionProductId = false;

            // check if the existing tour type should be enabled
            if (existingTourNutritionProductId != TourDatabase.ENTITY_IS_NOT_SAVED &&
                  tourNutritionProductId == existingTourNutritionProductId) {
               isExistingTourNutritionProductId = true;
            }

            actionRecentTourNutritionProduct.setEnabled(isExistingTourNutritionProductId == false);

            if (isExistingTourNutritionProductId) {

               // hide image because it looks ugly (on windows) when it's disabled
               actionRecentTourNutritionProduct.setImageDescriptor(null);

            } else {

               // set tour type image
//					final Image TourNutritionProductImage = UI.getInstance().getTourNutritionProductImage(TourNutritionProductId);
//					actionRecentTourNutritionProduct.setImageDescriptor(ImageDescriptor.createFromImage(TourNutritionProductImage));
//
//               actionRecentTourNutritionProduct.setImageDescriptor(TourNutritionProductImage.getTourNutritionProductImageDescriptor(
//                     TourNutritionProductId));
            }

         } else {

            // disable tour type

            actionRecentTourNutritionProduct.setEnabled(false);

            // hide image because it looks ugly (on windows) when it's disabled
            actionRecentTourNutritionProduct.setImageDescriptor(null);
         }
      }
   }

   /**
    * Create the menu entries for the recently used tour nutrition products
    *
    * @param menuMgr
    * @param isSaveTour
    */
   public static void fillMenuWithRecentTourNutritionProducts(final IMenuManager menuMgr,
                                                              final boolean isSaveTour) {

      if (_isInitialized == false) {
         initTourNutritionProductManager();
      }

      if (_recentTourNutritionProducts.isEmpty()) {
         return;
      }

      if (_maxTourNutritionProducts < 1) {
         return;
      }

      _isSaveTour = isSaveTour;

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
            // there are no more recent tour types
            break;
         }

         tourNutritionProductIndex++;
      }
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

   public static void setTourNutritionProductIntoTour(final Map.Entry<String, TourNutritionProduct> recentTourNutritionProduct,
                                                      final boolean isSaveTour) {

      //TODO FB what to do when the product is already in the list ?
      // the best would be to gray out the action in the contextual menu
      final Runnable runnable = () -> {

         // set tour type in all tours (without tours which are opened in an editor)
//         for (final TourData tourData : selectedTours) {
//            tourData.setTourNutritionProduct(TourNutritionProduct);
//         }
//
//         // keep tour type for the recent menu
//         addRecentTourNutritionProduct(TourNutritionProduct);
//
//         if (isSaveTour) {
//
//            // save all tours with the modified tour type
//            TourManager.saveModifiedTours(selectedTours);
//
//         } else {
//
//            // tours are not saved but the tour provider must be notified
//
//            if (tourProvider instanceof ITourProvider2) {
//               ((ITourProvider2) tourProvider).toursAreModified(selectedTours);
//            } else {
//               TourManager.fireEvent(TourEventId.TOUR_CHANGED, new TourEvent(selectedTours));
//            }
//         }

      };
      BusyIndicator.showWhile(Display.getCurrent(), runnable);
   }

   private String getTourNutritionProductId(final TourNutritionProduct tourNutritionProduct) {

      return tourNutritionProduct.getProductCode() + UI.SYMBOL_MINUS + tourNutritionProduct.getName();
   }

   public void updateRecentTourNutritionProducts(final TourNutritionProduct tourNutritionProduct) {

      // If the tour nutrition product Id is already in the list and it doesn't have
      // a TourNutritionProduct object, we insert the TourNutritionProduct object into
      // the list

      //iterate over the recent tour nutrition products and check if the tour nutrition product is already in the list
      for (final Map.Entry<String, TourNutritionProduct> recentTourNutritionProduct : _recentTourNutritionProducts.entrySet()) {

         final String recentTourNutritionProductCode =
               getProductCodeFromTourNutritionProductId(recentTourNutritionProduct.getKey());

         if (recentTourNutritionProductCode != null &&
               recentTourNutritionProductCode.equals(tourNutritionProduct.getProductCode())) {

            if (recentTourNutritionProduct.getValue() == null) {

               // update the tour nutrition product in the list
               recentTourNutritionProduct.setValue(tourNutritionProduct);

               return;
            }
         }
      }

      final String tourNutritionProductId = getTourNutritionProductId(tourNutritionProduct);
      _recentTourNutritionProducts.putFirst(
            tourNutritionProductId,
            tourNutritionProduct);
   }
}
