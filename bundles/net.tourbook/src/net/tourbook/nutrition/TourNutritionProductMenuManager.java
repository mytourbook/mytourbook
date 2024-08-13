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

import java.util.LinkedList;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;
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
 * Manage recently used tour types and fills the context menu
 * <p>
 * The method {@link #fillMenuRecentTourNutritionProducts} creates the actions and must be called
 * before the
 * actions are enabled/disabled with {@link #enableRecentTourNutritionProductActions}
 */
public class TourNutritionProductMenuManager {

   private static final String                       STATE_ID                     = "TourNutritionProductManager.RecentTourNutritionProducts"; //$NON-NLS-1$
   private static final String                       STATE_TOUR_TYPE_ID           = "TourNutritionProductId";                                  //$NON-NLS-1$

   private static final IPreferenceStore  _prefStore         = TourbookPlugin.getDefault().getPreferenceStore();

   /**
    * Tour type manager state is saved in {@link #STATE_ID}
    */
   private static IDialogSettings         _state             = TourbookPlugin.getDefault()                      //
         .getDialogSettingsSection(STATE_ID);

   /**
    * number of tour types which are displayed in the context menu or saved in the dialog settings,
    * it's max number is 9 to have a unique accelerator key
    */
   private static LinkedList<TourNutritionProduct>   _recentTourNutritionProducts = new LinkedList<>();

   /**
    * Contains actions which are displayed in the menu
    */
   private static RecentTourNutritionProductAction[] _actionsRecentTourNutritionProducts;

   private static int                                _maxTourNutritionProducts    = -1;

   private static IPropertyChangeListener _prefChangeListener;

   private static boolean                 _isInitialized     = false;
   private static boolean                 _isSaveTour;

   private static class RecentTourNutritionProductAction extends Action {

      private TourNutritionProduct __TourNutritionProduct;

      @Override
      public void run() {
         setTourNutritionProductIntoTour(__TourNutritionProduct, _isSaveTour);
      }

      private void setTourNutritionProduct(final TourNutritionProduct TourNutritionProduct) {
         __TourNutritionProduct = TourNutritionProduct;
      }
   }

   private static void addPrefChangeListener() {
      // create pref listener
      _prefChangeListener = propertyChangeEvent -> {
         final String property = propertyChangeEvent.getProperty();

         // check if the number of recent tour types has changed
         if (property.equals(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES)) {
            setActions();
         } else if (property.equals(ITourbookPreferences.TOUR_TYPE_LIST_IS_MODIFIED)) {
            updateTourNutritionProducts();
         }
      };

      // add pref listener
      _prefStore.addPropertyChangeListener(_prefChangeListener);
   }

   /**
    * Adds the {@link TourNutritionProduct} to the list of the recently used tour types
    *
    * @param TourNutritionProduct
    */
   private static void addRecentTourNutritionProduct(final TourNutritionProduct TourNutritionProduct) {
      _recentTourNutritionProducts.remove(TourNutritionProduct);
      _recentTourNutritionProducts.addFirst(TourNutritionProduct);
   }

   /**
    * @param isEnabled
    * @param existingTourNutritionProductId
    */
   public static void enableRecentTourNutritionProductActions(final boolean isEnabled, final long existingTourNutritionProductId) {

      if (_isInitialized == false) {
         initTourNutritionProductManager();
      }

      for (final RecentTourNutritionProductAction actionRecentTourNutritionProduct : _actionsRecentTourNutritionProducts) {

         final TourNutritionProduct TourNutritionProduct = actionRecentTourNutritionProduct.__TourNutritionProduct;
         if (TourNutritionProduct == null) {

            // disable tour type

            actionRecentTourNutritionProduct.setEnabled(false);

            // hide image because it looks ugly (on windows) when it's disabled
            actionRecentTourNutritionProduct.setImageDescriptor(null);

            continue;
         }

         final long TourNutritionProductId = 1L;//TourNutritionProduct.getTypeId();

         if (isEnabled) {

            // enable tour type

            boolean isExistingTourNutritionProductId = false;

            // check if the existing tour type should be enabled
            if (existingTourNutritionProductId != TourDatabase.ENTITY_IS_NOT_SAVED && TourNutritionProductId == existingTourNutritionProductId) {
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
    * Create the menu entries for the recently used tour types
    *
    * @param menuMgr
    * @param tourProvider
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

      // add tour types
      int TourNutritionProductIndex = 0;
      for (final RecentTourNutritionProductAction actionRecentTourNutritionProduct : _actionsRecentTourNutritionProducts) {
         try {

            final TourNutritionProduct recentTourNutritionProduct = _recentTourNutritionProducts.get(TourNutritionProductIndex);

            actionRecentTourNutritionProduct.setTourNutritionProduct(recentTourNutritionProduct);
            actionRecentTourNutritionProduct.setText(UI.SPACE4 + UI.MNEMONIC + (TourNutritionProductIndex + 1) + UI.SPACE2
                  + recentTourNutritionProduct.getName());

            menuMgr.add(actionRecentTourNutritionProduct);

         } catch (final IndexOutOfBoundsException e) {
            // there are no more recent tour types
            break;
         }

         TourNutritionProductIndex++;
      }
   }

   private static synchronized void initTourNutritionProductManager() {

      setActions();

      addPrefChangeListener();

      _isInitialized = true;
   }

   public static void restoreState() {

      final String[] allStateTourNutritionProductIds = _state.getArray(STATE_TOUR_TYPE_ID);
      if (allStateTourNutritionProductIds == null) {
         return;
      }

      /*
       * get all tour types from the database which are saved in the state
       */
//      final ArrayList<TourNutritionProduct> dbTourNutritionProducts = TourDatabase.getAllTourNutritionProducts();
//      for (final String stateTourNutritionProductIdItem : allStateTourNutritionProductIds) {
//         try {
//
//            final long stateTourNutritionProductId = Long.parseLong(stateTourNutritionProductIdItem);
//
//            for (final TourNutritionProduct dbTourNutritionProduct : dbTourNutritionProducts) {
//               if (dbTourNutritionProduct.getTypeId() == stateTourNutritionProductId) {
//                  _recentTourNutritionProducts.add(dbTourNutritionProduct);
//                  break;
//               }
//            }
//         } catch (final NumberFormatException e) {
//            // ignore
//         }
//      }
   }

   public static void saveState() {

      if (_maxTourNutritionProducts < 1) {
         // tour types are not initialized or not visible, do nothing
         return;
      }

      final String[] stateTourNutritionProductIds = new String[Math.min(_maxTourNutritionProducts, _recentTourNutritionProducts.size())];
      final int TourNutritionProductIndex = 0;

//      for (final TourNutritionProduct recentTourNutritionProduct : _recentTourNutritionProducts) {
//         stateTourNutritionProductIds[TourNutritionProductIndex++] = Long.toString(recentTourNutritionProduct.getTypeId());
//
//         if (TourNutritionProductIndex == _maxTourNutritionProducts) {
//            break;
//         }
//      }

      _state.put(STATE_TOUR_TYPE_ID, stateTourNutritionProductIds);
   }

   /**
    * create actions for recenct tour types
    */
   private static void setActions() {

      _maxTourNutritionProducts = TourbookPlugin
            .getDefault()
            .getPreferenceStore()
            .getInt(ITourbookPreferences.APPEARANCE_NUMBER_OF_RECENT_TOUR_TYPES);

      _actionsRecentTourNutritionProducts = new RecentTourNutritionProductAction[_maxTourNutritionProducts];

      for (int actionIndex = 0; actionIndex < _actionsRecentTourNutritionProducts.length; actionIndex++) {
         _actionsRecentTourNutritionProducts[actionIndex] = new RecentTourNutritionProductAction();
      }
   }

   public static void setTourNutritionProductIntoTour(final TourNutritionProduct TourNutritionProduct,
                                          final boolean isSaveTour) {

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

   /**
    * Tour types has changed
    */
   private static void updateTourNutritionProducts() {

//      final ArrayList<TourNutritionProduct> dbTourNutritionProducts = TourDatabase.getAllTourNutritionProducts();
//      final LinkedList<TourNutritionProduct> validTourNutritionProducts = new LinkedList<>();
//
//      // check if the tour types are still available
//      for (final TourNutritionProduct recentTourNutritionProduct : _recentTourNutritionProducts) {
//
//         final long recentTypeId = recentTourNutritionProduct.getTypeId();
//
//         for (final TourNutritionProduct dbTourNutritionProduct : dbTourNutritionProducts) {
//
//            if (recentTypeId == dbTourNutritionProduct.getTypeId()) {
//               validTourNutritionProducts.add(dbTourNutritionProduct);
//               break;
//            }
//         }
//      }

      // set updated list
      _recentTourNutritionProducts.clear();
      // _recentTourNutritionProducts = validTourNutritionProducts;
   }
}
